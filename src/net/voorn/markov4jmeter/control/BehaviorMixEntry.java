/***************************************************************************
 * Copyright (c) 2016 the WESSBAS project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/

package net.voorn.markov4jmeter.control;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import net.voorn.markov4jmeter.control.gui.GuiLogger;

import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.property.DoubleProperty;
import org.apache.jmeter.testelement.property.StringProperty;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

/**
 * <p>This class represents a user behavior model within a user behavior mix.</p>
 *
 * <p>It consists of the behavior name, the behavior model as well as the
 * relative frequency of occurence.</p>
 *
 * <p> During test plan creation, an instance of this class doesn't hold the
 * actual model but only a reference to the file containing the behavior model.
 * When a test is executed, an instance of this class instantiates the behavior
 * model with the information fetched from the file.</p>
 *
 * @author Andr&eacute; van Hoorn
 * @author Eike Schulz (esc@informatik.uni-kiel.de)
 */
public class BehaviorMixEntry extends AbstractTestElement implements Serializable {

    /** Default serial version ID. */
    private static final long serialVersionUID = 1L;

    /** Logger for this class */
    private static Logger logger = LoggingManager.getLoggerForClass();

    /** Name used to store the name. */
    public static final String BNAME = "Behavior.name";

    /** Name used to store the relative frequency. */
    public static final String RFREQ = "Behavior.frequency";

    /** Name used to store the filename. */
    public static final String FILENAME = "Behavior.filename";

    private static GuiLogger guiLogger;

    private final static String TOKEN_SEPARATOR = ",";
    private final static String PROBABILITY_TT_SEPARATOR = ";";

    private final static String EXIT_STATE_SYMBOL = "$";

    /** Flag indicating whether any think times to an exit state need to be
     *  defined in a behavior mix CSV-file. */
    private final static boolean THINK_TIMES_TO_EXIT_STATE_OPTIONAL = false;

    private boolean usesThinkTimes = false;

    /**
     * Maps source state id to state transition probabilities (during test
     * execution). Write access to this map must be synchronized since an
     * instance is used by multiple threads.
     */
    private transient Map<Integer,Map<Integer,Double>> probabilityMaps =
            new HashMap<Integer,Map<Integer,Double>>();

    private transient Map<Integer,Map<Integer,ThinkTime>> thinkTimeMaps =
            new HashMap<Integer,Map<Integer,ThinkTime>>();

    /**
     * Whether the behavior mix has been initialized or not.
     */
    private transient boolean initialized = false;

    /**
     * Id of the entry state.
     */
    private transient int entryState = -1;

    /**
     * Create a new behavior without properties.
     */
    public BehaviorMixEntry() {
    }

    public boolean usesThinkTimes () {

        return this.usesThinkTimes;
    }

    public static void setGuiLogger (GuiLogger guiLogger) {

        BehaviorMixEntry.guiLogger = guiLogger;
    }

    /**
     * Create a new behavior with name, relative frequency and filename
     *
     * @param name the name of the destination state.
     * @param rfreq the relative frequency.
     * @param filename the filename.
     */
    public BehaviorMixEntry(String name, double rfreq, String filename) {
        setProperty(new StringProperty(BNAME, name));
        setProperty(new DoubleProperty(RFREQ, rfreq));
        setProperty(new StringProperty(FILENAME, filename));
    }

    /**
     * Set the name of the behavior.
     *
     * @param newName the new name.
     */
    public void setBName(String newName) {
        setProperty(new StringProperty(BNAME, newName));
    }

    /**
     * Returns the name of the behavior.
     *
     * @return the behavior's name.
     */
    public String getBName() {
        return getPropertyAsString(BNAME);
    }

    /**
     * Sets the relative frequency of the behavior.
     *
     * @param newRFreq the relative frequency.
     */
    public void setRFreq(double newRFreq) {
        setProperty(new DoubleProperty(RFREQ, newRFreq));
    }

    /**
     * Returns the relative frequency of the behavior.
     *
     * @return the behavior's relative frequency.
     */
    public double getRFreq() {
        return getPropertyAsDouble(RFREQ);
    }

    /**
     * Sets the filename of the behavior model.
     *
     * @param newFilename the filename.
     */
    public void setFilename(String newFilename) {
        setProperty(new StringProperty(FILENAME, newFilename));
    }

    /**
     * Returns the filename of the behavior model.
     *
     * @return the filename.
     */
    public String getFilename() {
        return getPropertyAsString(FILENAME);
    }

    /**
     * Returns a string representation of the object.
     *
     * @return string representation of the object.
     */
    @Override
    public String toString() {
        return getName() + getRFreq() + getFilename();
    }

    /**
     * Returns the map containing the probabilities (values) for transitions
     * to all states (values) from the given state.
     *
     * @param id  The ID of the source state.
     */
    public Map<Integer,Double> getStateTransitionProbabilitiesMap(int id){

        return this.probabilityMaps.get(id);
    }

    /**
     * Returns the map containing the think times (values) for transitions
     * to all states (values) from the given state.
     *
     * @param id  The ID of the source state.
     */
    public Map<Integer,ThinkTime> getStateTransitionThinkTimesMap(int id){

        return this.thinkTimeMaps.get(id);
    }

    /**
     * Removes copy with leading and trailing quotes being removed.
     *
     * @param the resulting String.
     */
    private String removeQuotes(String str){

        return str.replaceAll("^\"|\"$", "");
        // int startIdx = str.startsWith("\"")?1:0;
        // return str.substring(startIdx,str.endsWith("\"")?str.length()-1:str.length());
    }

    /**
     * Collects all state names and IDs from a given header line of a CSV file.
     *
     * @param headerLine
     *     The header line of a CSV file to be parsed for state information.
     * @param stateNames
     *     The state names to be collected; should be empty by default.
     * @param stateIds
     *     The state IDs to be collected; should be empty by default.
     * @param stateNames2Ids
     *     Map which assigns state IDs to state names.
     * @return
     *     The number of identified states.
     *
     * @throws Exception
     *     in case of any duplicate state occurrences, or if a state is unknown
     *     (not included as key in the <code>stateNames2Ids</code> map), or if
     *     the exit state could not be found.
     */
    private int collectStateNamesAndIDs (
            final String headerLine,
            final List<String> stateNames,
            final List<Integer> stateIds,
            final Map<String,Integer> stateNames2Ids) throws Exception {

        int numberOfStates;  //  to be returned;

        boolean foundExitState = false;  //  '$' denotes the final state;

        // StringTokenizer does not consider space as a token;
        final StringTokenizer tokenizer = new StringTokenizer(
                headerLine,
                BehaviorMixEntry.TOKEN_SEPARATOR);

        // calculate number of times nextToken() can be called; current position
        // (= names column) is not included, since it consists of whitespace
        // -> number of states = remaining tokens;
        numberOfStates = tokenizer.countTokens();

        while( tokenizer.hasMoreTokens() ) {

            final String stateName =
                    this.removeQuotes(tokenizer.nextToken().trim());

            if ( this.isExitState(stateName) ) {

                foundExitState = true;

                // exit state has always ID 0;
                stateIds.add(0);

            } else if ( stateNames2Ids.containsKey(stateName) ) {

                this.warnIfStateIsDuplicateOccurrence(stateName, stateNames);
                stateIds.add( stateNames2Ids.get(stateName) );

            } else {

                // throws an Exception;
                this.error("Unknown state \"%s\".", stateName);
            }

            stateNames.add(stateName);  // also add '$'
        }

        if (!foundExitState) {

            this.error("Could not find exit state.");
        }

        return numberOfStates;
    }

    /**
     * Checks whether the given state name denotes the exit state.
     *
     * @param stateName  State name to be checked.
     *
     * @return
     *     <code>true</code> if and only if the given state name denotes the
     *     exit state.
     */
    private boolean isExitState (final String stateName) {

        return BehaviorMixEntry.EXIT_STATE_SYMBOL.equals(stateName);
    }

    /**
     * Helping function for error handling; throws a BehaviorException for
     * stopping the current process. This method can be easily modified for
     * logging the error messages into a dedicated logging window.
     *
     * @param format     <code>String</code> template for being formatted.
     * @param arguments  Arguments for being inserted into the template.
     *
     * @throws BehaviorException
     *     will be always thrown, includes the error message.
     */
    private void error (final String format, final Object... arguments)
            throws BehaviorException {

        // add filename to the given information;
        final String message =
                "File \"" + this.getFilename() + "\" -- " +
                String.format(format, arguments);

        if (BehaviorMixEntry.guiLogger != null) {

            BehaviorMixEntry.guiLogger.error(message);
        }

        throw new BehaviorException(message);
    }

    /**
     * Helping function for handling warning messages; writes the warning
     * messages into the log. This method can be easily modified for logging
     * the warning messages into a dedicated logging window.
     *
     * @param format     <code>String</code> template for being formatted.
     * @param arguments  Arguments for being inserted into the template.
     */
    private void warn (final String format, final Object... arguments) {

        // add filename to the given information;
        final String message =
                "File \"" + this.getFilename() + "\" -- " +
                String.format(format, arguments);

        if (BehaviorMixEntry.guiLogger != null) {

            BehaviorMixEntry.guiLogger.warn(message);
        }

        BehaviorMixEntry.logger.warn(message);
    }

    /**
     * Helping function for handling general information messages; writes the
     * messages into the log. This method can be easily modified for logging
     * the warning messages into a dedicated logging window.
     *
     * @param format     <code>String</code> template for being formatted.
     * @param arguments  Arguments for being inserted into the template.
     */
    private void info (final String format, final Object... arguments) {

        // add filename to the given information;
        final String message =
                "File \"" + this.getFilename() + "\" -- " +
                String.format(format, arguments);

        if (BehaviorMixEntry.guiLogger != null) {
            BehaviorMixEntry.guiLogger.info(message);
        }

        BehaviorMixEntry.logger.info(message);
    }

    /**
     * Writes a warning message for a duplicate state occurrence into the
     * log, in case the given state is contained in the specified state list.
     *
     * @param stateName   State name to be checked.
     * @param stateNames  List of state names.
     */
    private void warnIfStateIsDuplicateOccurrence (
            final String stateName, final List<String> stateNames) {

        // give warning for any duplicate state name;
        if ( this.isStateNameInList(stateName, stateNames) ) {

            this.warn("Duplicate occurrences for state \"%s\".", stateName);
        }
    }

    /**
     * Checks whether a given state name is contained in the specified list
     * of state names.
     *
     * @param stateName   State name to be checked.
     * @param stateNames  List of state names.
     *
     * @return
     *     <code>true</code> if and only if the given state name is contained
     *     in the specified list.
     */
    private boolean isStateNameInList (
            final String stateName, final List<String> stateNames) {

        for (final String stateNameL : stateNames) {

            if ( stateNameL.equals(stateName) ) {

                return true;
            }
        }

        return false;
    }

    /**
     * Loads behavior model from file.
     *
     * @param stateNames2Ids maps state names to their IDs.
     * @throws BehaviorException when an errors occurs.
     */
    private synchronized void loadFile(Map<String,Integer> stateNames2Ids)
            throws BehaviorException {

        this.info("Loading file for behavior \"%s\".", this.getBName());

        final String filename = this.getFilename();

        BufferedReader reader = null;

        try {

            // throws FileNotFound- or NullPointerException;
            reader = new BufferedReader(new FileReader(new File(filename)));

            // read header; might throw an IOException;
            final String headerLine = reader.readLine();

            final List<String>  stateNames = new ArrayList<String>();
            final List<Integer> stateIds   = new ArrayList<Integer>();

            // might throw an Exception;
            final int numberOfStates = this.collectStateNamesAndIDs(
                    headerLine,
                    stateNames,
                    stateIds,
                    stateNames2Ids);

            if (stateNames.size() != stateNames2Ids.size() + 1) {

                this.error("Invalid number of states in header "
                        + "(found: %d, expected: %d).",
                        stateNames.size() - 1, stateNames2Ids.size());
            }

            // now we know that stateNames contains "$" plus all state names in
            // stateNames2Ids;

            // might throw an Exception;
            this.collectProbabilitiesAndThinkTimes(
                    reader,
                    numberOfStates,
                    stateNames,
                    stateIds,
                    stateNames2Ids);

        } catch (final Exception ex) {

            // intercept exception for logging and converting it;
            BehaviorMixEntry.logger.error("Error while loading behavior file. ", ex);
            throw new BehaviorException(ex.getMessage(), ex);

        } finally {

            if (reader != null){

                try {

                    reader.close();

                } catch (final IOException ex) {

                    // do not throw Exception, since this is the finally-block;
                    // give a warning instead;
                    this.warn("File reader could not be closed.");
                }
            }
        }
    }

    /**
     * Collects all probabilities and think time definitions; the source matrix
     * targeted by the given <code>BufferedReader</code> instance must be
     * consistent, that is either all think times must be defined besides the
     * probabilities, or none must be defined.
     *
     * @param reader
     *     Reader targeting the matrix to be parsed.
     * @param numberOfStates
     *     Number of states which have been already read from header line.
     * @param stateNames
     *     List of states which have been found in the header line.
     * @param stateIds
     *     The state IDs assigned to the (target) states which have been
     *     identified in the header line.
     * @param stateNames2Ids
     *     Map which assigns state IDs to state names.
     * @throws Exception
     *     in the following cases:
     *     <ul>
     *       <li> matrix entries of any row does not match the number of states;
     *       </li>
     *       <li> if a state is unknown (not included as key in the
     *            <code>stateNames2Ids</code> map); </li>
     *       <li> any probability is undefined or any invalid probability
     *            definition occurs; </li>
     *       <li> any invalid think time definition occurs; </li>
     *       <li> matrix definition is inconsistent; </li>
     *       <li> initial state could not be found. </li>
     *     </ul>
     */
    @SuppressWarnings("unused")  // no warnings for flag THINK_TIMES_TO_EXIT_STATE_OPTIONAL;
    private void collectProbabilitiesAndThinkTimes (
            final BufferedReader reader,
            final int numberOfStates,
            final List<String> stateNames,
            final List<Integer> stateIds,
            final Map<String,Integer> stateNames2Ids) throws Exception {

        boolean foundEntryState = false;  // the trailing '*';
        int lineNumber = 2;  // header line has been already read before;

        // register found state names for detecting duplicate occurrences;
        final LinkedList<String> foundStateNames = new LinkedList<String>();

        Map<Integer, Double> probabilityMap;
        Map<Integer, ThinkTime> thinkTimeMap;

        int thinkTimesNum = 0;

        // will be set true, if first think time is detected;
        this.usesThinkTimes = false;

        // readLine() might throw an IOException;
        for (String line = reader.readLine(); line != null; line = reader.readLine(), lineNumber++) {

            probabilityMap = new HashMap<Integer, Double>();
            thinkTimeMap   = new HashMap<Integer, ThinkTime>();

            final StringTokenizer tokenizer = new StringTokenizer(
                    line,
                    BehaviorMixEntry.TOKEN_SEPARATOR);

            // +1 ~ first column (state names);
            if (tokenizer.countTokens() != numberOfStates + 1) {

                this.error("Invalid column count (found: %d, expected: %d) in row %d.",
                        tokenizer.countTokens(),
                        numberOfStates + 1,
                        lineNumber);
            }

            String stateName = this.removeQuotes(tokenizer.nextToken().trim());

            // is the state marked as entry state?
            if ( stateName.endsWith("*") ) {

                stateName = stateName.substring(0, stateName.length() - 1);
                this.entryState = stateNames2Ids.get(stateName);

                if (foundEntryState) {

                    this.warn("Ambiguous initial state definitions; "
                            + "will use \"%s\" as initial state.", stateName);
                }

                foundEntryState = true;
            }

            if ( !stateNames2Ids.containsKey(stateName) ) {

                this.error("Unknown state \"%s\".", stateName);
            }

            this.warnIfStateIsDuplicateOccurrence(stateName, foundStateNames);
            foundStateNames.add(stateName);

            for (int i = 0; tokenizer.hasMoreTokens(); i++) {

                Double probability = 0.0d;
                ThinkTime thinkTime;

                final String token = tokenizer.nextToken();

                final String[] subTokens =
                        token.split(BehaviorMixEntry.PROBABILITY_TT_SEPARATOR);

                if (subTokens.length == 0) {

                    this.error("No probability value defined for state \"%s\" "
                            + "in line %d.", stateName, lineNumber);
                }

                final String probabilityString = subTokens[0];

                try {

                    // might throw a (NullPointer- or) NumberFormatException;
                    probability = Double.parseDouble(probabilityString);

                } catch (final NumberFormatException ex) {

                    this.error(
                            "Invalid probability definition in line %d (\"%s\").",
                            lineNumber,
                            probabilityString);
                }

                // think times to exit state can be ignored;
                final String headerStateName = stateNames.get(i);

                // !this.isExitState(...) ~ ignore think time to exit state;
                if (subTokens.length >= 2 &&
                        (!BehaviorMixEntry.THINK_TIMES_TO_EXIT_STATE_OPTIONAL ||
                                !this.isExitState(headerStateName))) {

                    final String thinkTimeStr = subTokens[1];

                    thinkTime = ThinkTimeParser.parse(thinkTimeStr);

                    if (thinkTime == null) {

                        this.error(
                                "Invalid think time definition in line %d (\"%s\").",
                                lineNumber,
                                thinkTimeStr);
                    }

                    this.usesThinkTimes = true;
                    thinkTimesNum++;
                    thinkTimeMap.put(stateIds.get(i), thinkTime);
                }

                probabilityMap.put(stateIds.get(i), probability);
            }

            // ensure that either none or all think times have been defined;
            final int rows = lineNumber - 1;

            int statesPerRow = numberOfStates;

            if (BehaviorMixEntry.THINK_TIMES_TO_EXIT_STATE_OPTIONAL) {

                statesPerRow--;
            }

            // (numberOfStates - 1) ~ exit state does not need think time;
            if (thinkTimesNum > 0 && thinkTimesNum != rows * statesPerRow) {

                this.error("Inconsistent matrix definition in line %d: " +
                        "think time values partially undefined.",
                        lineNumber);
            }

            this.probabilityMaps.put(stateNames2Ids.get(stateName), probabilityMap);
            this.thinkTimeMaps.put(stateNames2Ids.get(stateName), thinkTimeMap);
        }

        // TODO: ensure that not multiple think time distributions are used;

        this.info(this.usesThinkTimes ?
                "Think time definitions detected; delays will be emulated." :
                "No think time definitions detected; delays will not be emulated.");

        if (!foundEntryState) {

            this.error("No state marked as entry state");
        }
    }

    /**
     * Instantiates the behavior model by reading the model information from
     * file.
     *
     * @param stateNames2Ids maps state names to their IDs.
     * @throws BehaviorException when an errors occurs.
     */
    public synchronized void initializeModel(Map<String,Integer> stateNames2Ids) throws BehaviorException{

        this.probabilityMaps.clear();
        this.thinkTimeMaps.clear();
        this.loadFile(stateNames2Ids);
        this.initialized = true;
    }

    /**
     * Returns the ID of the entry state.
     *
     * @return the ID.
     */
    public int getEntryState() {

        if (!initialized)

            BehaviorMixEntry.logger.fatalError("Behavior mix not yet initialized");

        return this.entryState;
    }

    /**
     * Clears the behavior entry.
     */
    @Override
    public void clear() {

        this.initialized = false;

        if (this.probabilityMaps != null) // required for distributed mode
            this.probabilityMaps.clear();

        if (this.thinkTimeMaps != null) // required for distributed mode
            this.thinkTimeMaps.clear();
    }
}


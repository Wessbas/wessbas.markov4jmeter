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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.voorn.markov4jmeter.control.gui.GuiLogger;

import org.apache.jmeter.testelement.property.CollectionProperty;
import org.apache.jmeter.testelement.property.PropertyIterator;
import org.apache.jmeter.testelement.property.TestElementProperty;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

/**
 * This class represents a so-called behavior mix, i.e. a set of user behaviors
 * each associated with their relative frequency of occurence.
 *
 * For example, a behavior mix may contain the user behaviors "Heavy Buyer" and
 * "Browser" with the associated relative frequencies 0.2 and 0.8.
 *
 * At present, an instance of this class de-facto represents the behavior mix
 * controller which returns a behavior model its method <i>getBehavior</i> is
 * called.
 *
 * @author Andr&eacute; van Hoorn
 */
public class BehaviorMix extends org.apache.jmeter.config.ConfigTestElement implements Serializable {
    /** Logger for this class. */
    private static Logger logger = LoggingManager.getLoggerForClass();

    /** Random number generator to be used within this class. */
    protected static java.util.Random rand = new java.util.Random();

    /** Property name used to store the list of behavior model entries. */
    public static final String BEHAVIORMIX = "UserBehaviorMix.behaviorEntries";

    /**
     * Contains initialized behavior models (during test execution)
     * Write access to this list must be synchronized since during test execution
     * an instance is used by multiple threads!
     */
    private transient List<BehaviorMixEntry> behaviorList = new ArrayList();

    /** Whether instance initialized (i.e. method initialize() has been called) */
    private boolean initialized = false;

    private static GuiLogger guiLogger;


    /**
     * Create a new empty behavior mix.
     */
    public BehaviorMix() {
        setProperty(new CollectionProperty(BEHAVIORMIX, new ArrayList()));
    }

    public static void setGuiLogger (GuiLogger guiLogger) {

        BehaviorMix.guiLogger = guiLogger;
    }

    /**
     * Get the behavior mix.
     *
     * @return the behavior mix.
     */
    public CollectionProperty getBehaviorMix() {
        return (CollectionProperty) getProperty(BEHAVIORMIX);
    }

    /**
     * Clear the behavior mix.
     */
    @Override
    public void clear() {
        super.clear();
        setProperty(new CollectionProperty(BEHAVIORMIX, new ArrayList()));
        this.initialized = false;
        if (this.behaviorList != null) // required for distributed mode
            this.behaviorList.clear();
    }

    /**
     * Set the list of behavior entries. Any existing entries will be lost.
     *
     * @param behaviorEntries the new behavior mix.
     */
    public void setBehaviorMix(List behaviorEntries) {
        setProperty(new CollectionProperty(BEHAVIORMIX, behaviorEntries));
    }

    /**
     * Returns the behavior mix as a List.
     *
     * @return the behavior mix as a list.
     */
    public List<BehaviorMixEntry> getBehaviorMixAsList(){
       return this.behaviorList;
    }

    /**
     * Returns the behavior mix as a Map.
     *
     * Each behavior name is used as the entrie's key.
     *
     * @return the behavior mix map.
     */
    public Map<String,BehaviorMixEntry> getBehaviorMixAsMap() {
        PropertyIterator iter = getBehaviorMix().iterator();
        Map behaviorMixMap = new HashMap();
        while (iter.hasNext()) {
            BehaviorMixEntry behv = (BehaviorMixEntry) iter.next().getObjectValue();
            behaviorMixMap.put(behv.getName(), behv);
        }
        return behaviorMixMap;
    }

    /**
     * Add a new behavior entry.
     *
     * @param behv the new behavior entry.
     */
    public void addBehaviorEntry(BehaviorMixEntry behv) {
        TestElementProperty newBehv = new TestElementProperty(behv.getName(), behv);
        if (isRunningVersion()) {
            this.setTemporary(newBehv);
        }
        getBehaviorMix().addItem(newBehv);
    }

    /**
     * Add a new behavior entry with the given name, relative frequency,
     * and filename.
     *
     * @param name the name of the behavior.
     * @param rfreq the relative frequency of the behavior within the mix.
     * @param filename the filename of the behavior.
     */
    public void addBehaviorEntry(String name, double rfreq, String filename) {
        addBehaviorEntry(new BehaviorMixEntry(name, rfreq, filename));
    }

    /**
     * Get a PropertyIterator of the behavior entries.
     *
     * @return an iterator for the behavior entries
     */
    public PropertyIterator iterator() {
        return getBehaviorMix().iterator();
    }

    /**
     * Create a string representation of the behavior mix.
     *
     * @return the string representation of the behavior mix.
     */
    @Override
    public String toString() {
        StringBuffer str = new StringBuffer();
        PropertyIterator iter = getBehaviorMix().iterator();
        while (iter.hasNext()) {
            BehaviorMixEntry behv = (BehaviorMixEntry) iter.next().getObjectValue();
            str.append(behv.getName() + behv.getRFreq() + behv.getFilename());
            if (iter.hasNext()) {
                str.append("&");
            }
        }
        return str.toString();
    }

    /**
     * Remove the specified behavior entry from the mix.
     *
     * @param row the index of the entry to remove.
     */
    public void removeBehaviorEntry(int row) {
        if (row < getBehaviorMix().size()) {
            getBehaviorMix().remove(row);
        }
    }

    /**
     * Remove the specified behavior entry from the list.
     *
     * @param behv the entry to remove.
     */
    public void removeBehaviorMix(BehaviorMixEntry behv) {
        PropertyIterator iter = getBehaviorMix().iterator();
        while (iter.hasNext()) {
            BehaviorMixEntry item = (BehaviorMixEntry) iter.next().getObjectValue();
            if (behv.equals(item)) {
                iter.remove();
            }
        }
    }

    /**
     * Remove the behavior entry with the specified name.
     *
     * @param behvName the name of the behavior to remove.
     */
    public void removeBehaviorMix(String behvName) {
        PropertyIterator iter = getBehaviorMix().iterator();
        while (iter.hasNext()) {
            BehaviorMixEntry behv = (BehaviorMixEntry) iter.next().getObjectValue();
            if (behv.getName().equals(behvName)) {
                iter.remove();
            }
        }
    }

    /**
     * Remove all behavior entries from the mix.
     */
    public void removeAllBehaviorEntries() {
        getBehaviorMix().clear();
    }

    /**
     * Add a new empty behavior entry to the list. The new entry will have the
     * empty string as its name a relative frequency of 0.0, and the empty
     * String as its filename.
     */
    public void addEmptyBehaviorEntry() {
        addBehaviorEntry(new BehaviorMixEntry("", 0.0, ""));
    }

    /**
     * Get the number of entries in the mix.
     *
     * @return the number of entries
     */
    public int getBehaviorCount() {
        return getBehaviorMix().size();
    }

    /**
     * Get a single behavior entry by index.
     *
     * @param row the index of the entry to return.
     * @return the entry at the specified index, or null if no entry
     *         exists at that index.
     */
    public BehaviorMixEntry getBehaviorEntry(int row) {
        BehaviorMixEntry behv = null;

        if (row < getBehaviorMix().size()) {
            behv = (BehaviorMixEntry) getBehaviorMix().get(row).getObjectValue();
        }

        return behv;
    }

    /**
     * Initializes the behavior models (during test execution).
     *
     * The behavior models are read from file. The map passed as parameter is
     * used to map the state names contained within the file to the
     * ApplicationStates.
     *
     * @param stateNames2Ids map mapping all ApplicationState names to their ID.
     * @throws BehaviorException when an error during initialization occurs.
     */
    public synchronized void initialize(Map stateNames2Ids) throws BehaviorException {

        this.initialized = false;

        this.behaviorList.clear();
        int numBehaviors = this.getBehaviorCount();

        int numberOfThinkTimeBehaviors = 0;
        for (int i=0; i<numBehaviors; i++){
            BehaviorMixEntry behavior =
                    (BehaviorMixEntry) this.getBehaviorEntry(i).clone();

            behavior.initializeModel(stateNames2Ids);
            this.behaviorList.add(behavior);

            if (behavior.usesThinkTimes()) {

                numberOfThinkTimeBehaviors++;
            }
        }

        if (numberOfThinkTimeBehaviors > 0 &&
            numberOfThinkTimeBehaviors < numBehaviors) {

            final String message = "Inconsistent mix of behavior models; some models use think times, some do not.";

            this.error(message);
        }

        this.initialized = true;
    }

    /**
     * Helping function for error handling; throws a BehaviorException for
     * stopping the current process. Furthermore, the error message will be
     * logged in the dedicated logging window of the Markov Controller.
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
                "Behavior Mix problem -- " + String.format(format, arguments);

        if (BehaviorMix.guiLogger != null) {

            BehaviorMix.guiLogger.error(message);
        }

        throw new BehaviorException(message);
    }

    /**
     * Returns a behavior model based on the behavior mix.
     *
     * @return the behavior model and null if no behavior chosen (list of
     *         behaviors empty or all entries have a relative frequency of 0.0).
     * @throws BehaviorException when an error occurs.
     */
    public BehaviorMixEntry getBehavior() throws BehaviorException {
        List<Double> cumProbList = new ArrayList();
        double curCumProb = 0.0;

        for (int i=0; i<this.behaviorList.size(); i++){
            /* add all to have the same number of entries in
             * both lists */
            curCumProb+=this.behaviorList.get(i).getRFreq();
            cumProbList.add(curCumProb);

        }
        if(cumProbList.isEmpty() || (cumProbList.get(cumProbList.size()-1) == 0)){
            logger.fatalError("No behavior matches");
            throw new BehaviorException("no behavior matches");
        }
        //System.out.print("Choosing behavior ... -> cumProbList " + cumProbList.toString() + " ->");
        double rndVal = rand.nextDouble()*(cumProbList.get(cumProbList.size()-1));
        //System.out.print("rnd: " + rndVal + " ");
        BehaviorMixEntry behavior = this.behaviorList.get(0);
        for (int i=0; i<cumProbList.size()-1; i++){
            if(rndVal<cumProbList.get(i))
                break;
            behavior = this.behaviorList.get(i+1);
        }
        //System.out.println("-> Behavior: " + behavior.getBName());

        return behavior;
    }
}


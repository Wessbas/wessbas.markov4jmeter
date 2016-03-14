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
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

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
 */
public class BehaviorMixEntry extends AbstractTestElement implements Serializable {
    /** Logger for this class */
    private static Logger logger = LoggingManager.getLoggerForClass();
    
    /** Name used to store the name. */
    public static final String BNAME = "Behavior.name";
    
    /** Name used to store the relative frequency. */
    public static final String RFREQ = "Behavior.frequency";
    
    /** Name used to store the filename. */
    public static final String FILENAME = "Behavior.filename";
    
    /**
     * Maps source state id to state transition probabilities (during test 
     * execution). Write access to this map must be synchronized since an 
     * instance is used by multiple threads.
     */
    private transient Map<Integer,Map<Integer,Double>> probabilityMaps = new HashMap();

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
    
    /**
     * Create a new behavior with name, relative frequency and filename
     *
     * @param name th ename of the destination state.
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
     */
    public Map<Integer,Double> getStateTransitionMap(int id){
        return this.probabilityMaps.get(id);
    }
    
    /**
     * Removes copy with leading and trailing quotes being removed.
     *
     * @param the resulting String.
     */
    private String removeQuotes(String str){
        int startIdx = str.startsWith("\"")?1:0;
        return str.substring(startIdx,str.endsWith("\"")?str.length()-1:str.length());
    }
    
    /**
     * Loads behavior model from file.
     *
     * TODO: warn when duplicate states found within file.
     *
     * @param stateNames2Ids maps state names to their IDs.
     * @throws BehaviorException when an errors occurs.
     */
    private synchronized void loadFile(Map<String,Integer> stateNames2Ids) throws BehaviorException{
        Map<Integer,Map<Integer,Double>> probabilityMaps = new HashMap();
        System.out.println("Loading behavior file " + this.getFilename() +
                " for behavior " + this.getBName());
        
        String filename = this.getFilename();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(new File(filename)));
            StringTokenizer token;
            String line;
            String stateName;
            boolean foundFinalState = false; // the '$' character
            boolean foundEntryState = false; // the trailing '*'
            
            List<String> stateNames = new ArrayList(); 
            List<Integer> stateIds = new ArrayList();
            int numStates;
            
            /* read header */
            line = reader.readLine();
            token = new StringTokenizer(line,","); // does not consider space as token
            numStates = token.countTokens()-1; 
            while(token.hasMoreTokens()){
                stateName = removeQuotes(token.nextToken().trim());
                if(stateName.equals("$")){
                    foundFinalState = true;
                    stateIds.add(new Integer(0));
                }else if (stateNames2Ids.containsKey(stateName)){
                    stateIds.add(new Integer(stateNames2Ids.get(stateName)));
                }else {
                    throw new Exception("Unknown state: '" + stateName +"'");
                }
                stateNames.add(stateName); // also add '$'
            }
            if(stateNames.size() != stateNames2Ids.size()+1){
                logger.error("Expected: " + stateNames2Ids.toString()+"\n"+
                        "found " + stateNames.toString());
                throw new Exception("Invalid number of states in header (found"+
                        (stateNames.size()-1)+", expected "+stateNames2Ids.size()+")");
            }
            /* now we know that stateNames contains all statenames in 
             * stateNames2Ids as well as "$"
             *

            /* read values */
            int numLines = 0;
            String entryStateName = null;
            Map<Integer,Double> probabilityMap;
            for(line=reader.readLine(); line!=null; line=reader.readLine()){
                probabilityMap = new HashMap();
                numLines++;
                token = new StringTokenizer(line,",");
                if(token.countTokens()!=numStates+2){
                    throw new Exception("Invalid column count ("+
                            token.countTokens()+") in row " + numLines);
                }
                
                stateName = removeQuotes(token.nextToken().trim());
                
                /* is the state marked as entry state? */
                if (stateName.endsWith("*")){
                    stateName = stateName.substring(0,stateName.length()-1);
                    this.entryState = stateNames2Ids.get(stateName);
                    foundEntryState = true;
                }
                
                if(!stateNames2Ids.containsKey(stateName)){
                    throw new Exception("Unknown state: '" + stateName +"'");                    
                }
                
                for(int i=0; token.hasMoreTokens(); i++){
                    Double p = Double.parseDouble(removeQuotes(token.nextToken()));
                    probabilityMap.put(new Integer(stateIds.get(i)),p);
                }
                
                if(!foundEntryState){
                    throw new Exception("No state marked as entry state");
                }
                probabilityMaps.put(new Integer(stateNames2Ids.get(stateName)),probabilityMap);
            }
        } catch (Exception ex) {
            logger.error("Error while loading behavior file",ex);
            throw new BehaviorException(ex.getMessage(), ex);
        } finally{
            if (reader != null){
                try {
                    reader.close();
                } catch (IOException ex) {
                    throw new BehaviorException(ex.getMessage(), ex);
                }
            }
        }
        this.probabilityMaps = probabilityMaps;
    }
    
    /**
     * Instantiates the behavior model by reading the model information from 
     * file. 
     *
     * @param stateNames2Ids maps state names to their IDs.
     * @throws BehaviorException when an errors occurs.
     */
    public synchronized void initializeModel(Map stateNames2Ids) throws BehaviorException{
        this.probabilityMaps.clear();
        this.loadFile(stateNames2Ids);
        this.initialized = true;
    }
    
    /**
     * Returns the ID of the entry state.
     *
     * @return the ID.
     */
    public int getEntryState(){
        if (!initialized)
            logger.fatalError("Behavior mix not yet initialized");
        return this.entryState;
    }

    /**
     * Clears the behaveior entry.
     */
    @Override
    public void clear() {
        this.initialized = false;
        if (this.probabilityMaps != null) // required for distributed mode
            this.probabilityMaps.clear();
    }    
}

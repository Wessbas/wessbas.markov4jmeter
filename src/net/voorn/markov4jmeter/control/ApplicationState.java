/*
 * Copyright 2007 Andre van Hoorn.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package net.voorn.markov4jmeter.control;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.jmeter.testelement.property.IntegerProperty;

import org.apache.jmeter.testelement.property.TestElementProperty;
import org.apache.log.Logger;
import org.apache.jorphan.logging.LoggingManager;

/**
 * <P>This class extends JMeter's GenericController and represents a state within a 
 * Markov Model and must be placed directly underneath a MarkovController.</P>
 *
 * <P>During test run, an ApplicationState's works exactly like JMeter's 
 * SimpleController. But moreover, it contains information
 * about outgoing transitions to all other ApplicationState within the same
 * Markov Model. After the MarkovController executed an ApplicationState it 
 * request the list of all outgoing transitions from this ApplicationState.</P>
 *
 * <P>ApplicationStates belonging to a MarkovController must have a unique 
 * names since the user behavior models's defined outside JMeter are linked to
 * the ApplicationStates by name.</P>
 *
 * @author Andr&eacute; van Hoorn
 */
public class ApplicationState extends org.apache.jmeter.control.GenericController {
    
    /** Logger for this class. */
    private static Logger logger = LoggingManager.getLoggerForClass();
    
    /** Property name used to store the ID. */
    public final static String ID = "ApplicationState.id";
    /** Property name used to store the comment. */
    public final static String COMMENTS = "ApplicationState.comments";
    /** Property name used to store the transitions. */
    public final static String TRANSITIONS = "ApplicationState.transitions";

    /** 
     * Keeps history of transitions which existed during runtime. 
     * This is required for loading existing transitions from file in case 
     * the corresponding destination states aren't created yet.
     */
    private static Map<Integer,Map<Integer,ApplicationStateTransition>> orphanTransitions = Collections.synchronizedMap(new HashMap());
    
    /** Creates a new instance of ApplicationState */
    public ApplicationState() {
        setProperty(new TestElementProperty(TRANSITIONS, new ApplicationStateTransitions()));
    }

    /** 
     * Set the transitions.
     * 
     * @param transitions the transitions to set.     
     */
    public void setTransitions(ApplicationStateTransitions transitions){
        setProperty(new TestElementProperty(TRANSITIONS, transitions));
    }
    
    /**
     * Returns the transitions.
     *
     * @return the transitions.
     *
     */
    public ApplicationStateTransitions getTransitions(){
        ApplicationStateTransitions transitions = (ApplicationStateTransitions) getProperty(TRANSITIONS).getObjectValue();
        if (transitions == null) {
            transitions = new ApplicationStateTransitions();
            setTransitions(transitions);
        }
        return transitions;
    }
    
    /**
     * Forces an update of the state transitions by passing the current
     * list of state ids. 
     *
     * @param stateIds list of destination state IDs.
     */
    public void updateTransitions(List<Integer> stateIds){
        ApplicationStateTransitions transitions = this.getTransitions();
        boolean setOrphanTransitions = false;
        Map<Integer,ApplicationStateTransition> myOrphanTransitions = 
                ApplicationState.orphanTransitions.get(this.getId());
        if (myOrphanTransitions == null) {
            // needn't be synchronized since only one instance for application in GUI
            myOrphanTransitions = new HashMap(); 
            ApplicationState.orphanTransitions.put(this.getId(), myOrphanTransitions);
            setOrphanTransitions = true;
        }
        transitions.updateTransitions(stateIds, myOrphanTransitions, setOrphanTransitions);
        setProperty(new TestElementProperty(TRANSITIONS, transitions));
    }
    
    /**
     * Set the ID of the state.
     * 
     * @param newId the new ID.
     */
    public void setId(int newId) {
        setProperty(new IntegerProperty(ID, newId));       
    }

    /**
     * Return the ID of the state.
     * 
     * @return the ID.
     */
    public int getId() {
        return getPropertyAsInt(ID);
    }
    
    /**
     * Clears the application state
     */
    @Override
    public void clear() {
	super.clear();
        if (ApplicationState.orphanTransitions != null) // required for distributed mode
            ApplicationState.orphanTransitions.remove(this.getId());
    }
}

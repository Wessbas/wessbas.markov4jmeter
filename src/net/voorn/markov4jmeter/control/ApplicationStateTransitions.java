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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.jmeter.testelement.property.CollectionProperty;
import org.apache.jmeter.testelement.property.PropertyIterator;
import org.apache.jmeter.testelement.property.TestElementProperty;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

/**
 * A set of outgoing transitions for one state.
 *
 * @author Andr&eacute; van Hoorn
 */
public class ApplicationStateTransitions extends org.apache.jmeter.config.ConfigTestElement implements Serializable {
    
    /** Logger for this class. */
    private static Logger logger = LoggingManager.getLoggerForClass();
    
    /** Property name used to store list of transitions. */
    public static final String TRANSITIONS = "ApplicationStateTransitions.transitions";
    
    /**
     * Create a new empty transition set.
     */
    public ApplicationStateTransitions() {
        setProperty(new CollectionProperty(TRANSITIONS, new ArrayList()));
    }
    
    /**
     * Get the transitions.
     *
     * @return the transitions.
     */
    public CollectionProperty getTransitions() {
        return (CollectionProperty) getProperty(TRANSITIONS);
    }
    
    /**
     * Clear the transition set.
     */
    @Override
    public void clear() {
        super.clear();
        setProperty(new CollectionProperty(TRANSITIONS, new ArrayList()));
    }
    
    /**
     * Set the set of transitions. 
     *
     * @param transitions the list of transitions.
     */
    public void setTransitions(List transitions) {
        setProperty(new CollectionProperty(TRANSITIONS, transitions));
    }
    
    /**
     * Update the transitions by passing the list of state IDs.
     *
     * The set of transitions will be modified to match the given list of
     * IDs. Existing transitions which have states not in the given list
     * will be removed. New (empty) transitions will be created.
     *
     * The orphan transitions map contains information about transitions which 
     * used to be outgoing transitions of a state but the the IDs of their 
     * destination states were not contained in the list of state IDs the last 
     * time this method was called. Especially, this is necessary to store 
     * guards and actions of transitions with destination states which have not 
     * yet been initialized (and thus are not in the state ID list) when they 
     * are read from file.
     *
     * @param stateIds the list of state IDs.
     * @param orphanTransitions map of orphan transitions.
     * @param setOrphanTransitions whether to update the orphan transitions or 
     *        not.
     */
    public void updateTransitions(List<Integer> stateIds,
            Map<Integer,ApplicationStateTransition> orphanTransitions,
            boolean setOrphanTransitions){
        Map map = this.getTransitionsAsMap();
        
        List list = new ArrayList();
        ListIterator<Integer> it = stateIds.listIterator();
        
        if(setOrphanTransitions)
            orphanTransitions.putAll(map);
        
        //System.out.println("Updating transitions: ..");
        //System.out.println("Before:\n"+this.toString());
        
        while (it.hasNext()){
            Integer cur = it.next();
            if(map.containsKey(cur)) {
                list.add(map.get(cur));
            }else if(orphanTransitions.containsKey(cur)){
                list.add(orphanTransitions.remove(cur));
            }else{
                ApplicationStateTransition newTransition = new ApplicationStateTransition(cur.intValue());
                list.add(newTransition);
            }
        }
        this.setTransitions(list);
        
        //System.out.println("After:\n"+this.toString());
    }
    
    /**
     * Returns the transition set as a Map. The destination state IDs are used 
     * as the entries' keys. 
     *
     * @return the transitions map.
     */
    public Map getTransitionsAsMap() {
        PropertyIterator iter = getTransitions().iterator();
        Map transitionsMap = new HashMap();
        while (iter.hasNext()) {
            ApplicationStateTransition transition = 
                    (ApplicationStateTransition) iter.next().getObjectValue();
            transitionsMap.put(transition.getDstStateId(), transition);
        }
        return transitionsMap;
    }

    /**
     * Returns the transition set as a list.
     *
     * @return the transition list.
     */
    public List<ApplicationStateTransition> getTransitionsAsList(){
        PropertyIterator iter = getTransitions().iterator();
        List<ApplicationStateTransition> transitionsList = new ArrayList();
        while (iter.hasNext()) {
            ApplicationStateTransition transition = (ApplicationStateTransition) iter.next().getObjectValue();
            transitionsList.add(transition);
        }
        return transitionsList;
    }
    
    /**
     * Add a transition to the transition set.
     *
     * @param transition the transition to add.
     */
    public void addTransition(ApplicationStateTransition transition) {
        TestElementProperty newTransition = new TestElementProperty(transition.getDstStateId()+"", transition);
        if (isRunningVersion()) {
            this.setTemporary(newTransition);
        }
        getTransitions().addItem(newTransition);
    }
    
    /**
     * Add a new transition with the given destination state ID, 
     * guard and action.
     *
     * @param dstStateId the destination state.
     * @param guard the guard.
     * @param action the action.
     */
    public void addTransition(int dstStateId, String guard, String action) {
        addTransition(new ApplicationStateTransition(dstStateId, guard, action));
    }
    
    /**
     * Get a PropertyIterator of the arguments.
     *
     * @return an argument iteraor.
     */
    public PropertyIterator iterator() {
        return getTransitions().iterator();
    }
    
    /**
     * Remove the specified transition from the transition set.
     *
     * @param row the index of the transition to remove.
     */
    public void removeTransitionByRow(int row) {
        if (row < getTransitions().size()) {
            getTransitions().remove(row);
        }
    }
    
    /**
     * Remove the specified transition from the transition set. 
     * The transition to be removed is compared by means of the <i>equals</i> 
     * method.
     *
     * @param transition the transition to remove.
     */
    public void removeTransitions(ApplicationStateTransition transition) {
        PropertyIterator iter = getTransitions().iterator();
        while (iter.hasNext()) {
            ApplicationStateTransition item =
                    (ApplicationStateTransition) iter.next().getObjectValue();
            if (transition.equals(item)) {
                iter.remove();
            }
        }
    }
    
    /**
     * Remove the transition with the specified destination state ID.
     *
     * @param dstStateId the destination state ID.
     */
    public void removeTransitionById(int dstStateId) {
        PropertyIterator iter = getTransitions().iterator();
        while (iter.hasNext()) {
            ApplicationStateTransition transition = (ApplicationStateTransition) iter.next().getObjectValue();
            if (transition.getDstStateId() == dstStateId) {
                iter.remove();
            }
        }
    }
    
    /**
     * Remove all transitions from the transition set.
     */
    public void removeAllTransitions() {
        getTransitions().clear();
    }
    
    /**
     * Get the number of transitions in the set.
     *
     * @return the number of transitions.
     */
    public int getTransitionCount() {
        return getTransitions().size();
    }
    
    /**
     * Get a single transition by index.
     *
     * @param row the index of the transition to return.
     * @return the transition at the specified index, or null if no transition
     *         exists at that index.
     */
    public ApplicationStateTransition getTransition(int row) {
        ApplicationStateTransition transition = null;
        
        if (row < getTransitions().size()) {
            transition = (ApplicationStateTransition) getTransitions().get(row).getObjectValue();
        }
        
        return transition;
    }
    
    /**
     * Create a string representation of the transition set.
     *
     * @return the string representation of the transition set
     */
    public String toString() {
        StringBuffer str = new StringBuffer();
        PropertyIterator iter = getTransitions().iterator();
        while (iter.hasNext()) {
            ApplicationStateTransition transition =
                    (ApplicationStateTransition) iter.next().getObjectValue();
            str.append(transition.toString());
            if (iter.hasNext()) {
                str.append("\n");
            }
        }
        return str.toString();
    }
}

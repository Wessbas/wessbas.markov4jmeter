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
import net.voorn.markov4jmeter.control.gui.MarkovControllerTreeListener;

import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.property.BooleanProperty;
import org.apache.jmeter.testelement.property.IntegerProperty;
import org.apache.jmeter.testelement.property.StringProperty;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

/**
 * This class repress a single outgoing transition from one state to a
 * destination state consisting of the destination state name, a guard and an action.
 *
 * @author Andr&eacute; van Hoorn
 */
public class ApplicationStateTransition extends AbstractTestElement implements Serializable {
    
    /** Logger for this class. */
    private static Logger logger = LoggingManager.getLoggerForClass();
    
    /** Property name used to store the destination state. */
    public static final String DST_STATE_ID = "ApplicationState.dstId";
    /** Property name used to store the guard. */
    public static final String GUARD = "ApplicationState.guard";
    /** Property name used to store the action. */
    public static final String ACTION = "ApplicationState.action";
    /** Property name used to store the enable/disable state. */
    public static final String DISABLED = "ApplicationState.disabled";
    
    /**
     * Create a new transition.
     */
    public ApplicationStateTransition() {
    }
    
    /**
     * Create a new transition with destination state. The transition contains 
     * no guard (equivalent to "true") and no action.
     *
     * @param dstStateId ID of the destination state.
     */
    public ApplicationStateTransition(int dstStateId) {
        this(dstStateId, "", "");
    }
    
    /**
     * Create a new transition with destination state, guard and action.
     *
     * @param dstStateId ID of the destination state.
     * @param guard the guard.
     * @param action the action.
     */
    public ApplicationStateTransition(int dstStateId, String guard, String action) {
        setProperty(new IntegerProperty(DST_STATE_ID, dstStateId));
        setProperty(new StringProperty(GUARD, guard));
        setProperty(new StringProperty(ACTION, action));
        setProperty(new BooleanProperty(DISABLED, false));
    }
    
    /**
     * Set the destination state by ID.
     *
     * @param newDstStateId ID of the destination state.
     */
    public void setDstStateId(int newDstStateId) {
        setProperty(new IntegerProperty(DST_STATE_ID, newDstStateId));
    }
    
    /**
     * Returns the ID of the destination state.
     *
     * @return ID of the destination state.
     */
    public int getDstStateId() {
        return getPropertyAsInt(DST_STATE_ID);
    }
    
    /**
     * Returns the name of the destination state by requesting the 
     * MarkovControllerTreeListener. This method fails when being called in 
     * non-GUI mode. 
     *
     * @return the name of the destination state.
     */
    public String getDstStateName(){
        return MarkovControllerTreeListener.getInstance().lookupStateName(this.getDstStateId());
    }
    
    /**
     * Sets the transition's guard.
     *
     * @param newGuard the guard.
     */
    public void setGuard(String newGuard) {
        setProperty(new StringProperty(GUARD, newGuard));
    }
    
    /**
     * Returns the transition's guard.
     *
     * @return the transition's guard.
     */
    public String getGuard() {
        return getPropertyAsString(GUARD);
    }
    
    
    /**
     * Sets the transition's action.
     *
     * @param newAction the new action.
     */
    public void setAction(String newAction) {
        setProperty(new StringProperty(ACTION, newAction));
    }
    
    /**
     * Returns the transition's action.
     *
     * @return the transition's action.
     */
    public String getAction() {
        return getPropertyAsString(ACTION);
    }
    
    /**
     * Used to deactivate a transition which is equivalent to
     * having a guard always evaluating to false.
     *
     * @param disabled iff set, the transition is disabled.
     */
    public void setDisabled(boolean disabled){
        setProperty(new BooleanProperty(DISABLED, disabled));
    }
    
    /**
     * Returns whether the transition is disabled or not.
     * 
     * @return true iff the transition is disabled.
     */
    public boolean isDisabled(){
        return getPropertyAsBoolean(DISABLED);
    }
    
    /**
     * Returns a string representation of the object.
     *
     * @return string representation of the object.
     */
    @Override
    public String toString() {
        return  "dstStateId:" + getDstStateId() + " " +
                (this.isDisabled() ? " (disabled) " : " (enabled) ") +
                "G: '"+ getGuard() + "' " +
                "A: '"+ getAction() + "'";
    }
}

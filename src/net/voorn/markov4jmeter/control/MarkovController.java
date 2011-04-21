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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.apache.jmeter.control.Controller;

import org.apache.jmeter.control.NextIsNullException;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestListener;
import org.apache.jmeter.testelement.ThreadListener;
import org.apache.jmeter.testelement.property.IntegerProperty;
import org.apache.jmeter.testelement.property.BooleanProperty;
import org.apache.jmeter.testelement.property.StringProperty;
import org.apache.jmeter.testelement.property.TestElementProperty;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.jorphan.util.JMeterStopTestException;
import org.apache.jorphan.util.JMeterStopThreadException;
import org.apache.log.Logger;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Scriptable;

/**
 * <p>The class represents a Markov Model and contains the definition 
 * of the behavior mix to use during execution as well as the specification
 * of the number of allowed parallel active instances of the Markov Model
 * (each representing a user session) during test run. It must only contain
 * ApplicationState elements as direct child elements.</p> 
 *
 * <p>An instance of the controller executes as follows:
 * <ul>
 * <li>When initialized by the JMeter engine, a behavior 
 * model is requested from the <i>session arrival controller</i>. The entry state 
 * is set according to the user behavior's entry state.</li>
 * <li>When the first iteration is requested by the engine (i.e. the engine calls the
 * method <i>next()</i>), the controller waits inside the <i>session arrival 
 * controller</i>'s queue to be granted session entrance. As soon as the 
 * controller exits the queue, the ApplicationState representing the entry state 
 * is executed.</li>
 * <li>After an ApplicationState has executed, the MarkovController determines
 * the next state by first requesting the set of outgoing transitions
 * from the current state, determining the set of transitions whose guards 
 * evaluate to true, and randomly choosing a transtion from the set based on the
 * probabilities defined within the user behavior model. The action associated 
 * with the transition being chosen is executed.</li>
 * </ul>
 *
 * @author Andr&eacute; van Hoorn
 */
public class MarkovController extends org.apache.jmeter.control.GenericController  implements ThreadListener, TestListener{
    
    /** Logger for this class */
    private static final Logger logger = LoggingManager.getLoggerForClass();
    
    /** Random number generator to be used within this class. */
    private static java.util.Random rand = new java.util.Random();

    /** Property name used to store the ID. */
    public final static String ID = "MarkovController.id";
    /** Property name used to store the comments. */
    public final static String COMMENTS = "MarkovController.comments";
    /** Property name used to store the behavior mix. */
    public final static String BEHV_MIX = "MarkovController.behaviorMix";
    /** Property name used to store the disable/enable state of the arrival 
     *  controller. */
    public final static String ENABLE_ARRIVAL_CTRL = "MarkovController.arrivalCtrl";
    /** Property name used to store the formula defining the allowed active 
     *  sessions count. */
    public final static String ARRIVAL_CTRL_NUM_SESSIONS = "MarkovController.arrivalNumSessions";
    /** Property name used to store the enable/disable state of the arrival 
     *  controller logging. */
    public final static String ENABLE_ARRIVAL_CTRL_LOGGING = "MarkovController.arrivalCtrlLogging";
    /** Property name used to store the arrival controllers log filename. */
    public final static String ARRIVAL_CTRL_LOGFILE = "MarkovController.arrivalCtrlLogFile";
    
    /*
     * Contains user behavior mixes for all applications (during test execution).
     * This map is cleared after each test execution.
     */
    private static Map<Integer, BehaviorMix>  applicationBehaviorMixes =
            Collections.synchronizedMap(new HashMap());
    
    /** To make sure that exitSession of the session arrival controller is 
      * called only once. */
    private boolean mustReduceNumThreads = false;
    
    /** Whether currently within a session or not. */
    private boolean inSession = false;
    
    /* Needed while creating test plan in GUI (to export template behavior file) */
    private transient List<String> stateNames = new ArrayList();
    
    /* Maps state IDs to state objects (during test execution) */
    private transient Map<Integer,ApplicationState> stateMap = new HashMap();
    /* References application's behavior mix (during test execution)  */
    private transient BehaviorMix behaviorMix = null;
    /* References thread behavior (during test execution) */
    private transient BehaviorMixEntry behavior = null;
    /* References current state (during test execution) */
    private transient ApplicationState currentState = null;
    
    /** The associated sessionArivalController */
    private transient SessionArrivalController sessionArrivalController = null;
    /** Whether the session arrival controller is enabled or not */
    private transient boolean sessionArrivalControllerEnabled = false;
    
    /** The test iteration */
    private int testIteration=-1;
    
    /**
     * Creates a new instance of MarkovController.
     */
    public MarkovController() {
    }

    /**
     * Creates a new instance of MarkovController with a given behavior mix.
     *
     * @param mix the behavior mix.
     */
    public void setBehaviorMix(BehaviorMix mix){
        setProperty(new TestElementProperty(BEHV_MIX, mix));
    }
    
    /**
     * Returns the behavior mix.
     *
     * @return the behavior mix.
     */
    public BehaviorMix getBehaviorMix(){
        BehaviorMix mix = (BehaviorMix) getProperty(BEHV_MIX).getObjectValue();
        if (mix == null) {
            mix = new BehaviorMix();
            setBehaviorMix(mix);
        }
        return mix;
    }
    
    /**
     * Set the controller ID.
     *
     * @param newId the ID.
     */
    public void setId(int newId) {
        setProperty(new IntegerProperty(ID, newId));
    }
    
    /**
     * Returns the controller ID.
     *
     * @return the ID.
     */
    public int getId() {
        return getPropertyAsInt(ID);
    }
    
    /**
     * Sets the use of a session arrival controller.
     *
     * @param enabled true if a session arrival controller shall be used.
     */
    public void setArrivalCtrlEnabled(boolean enabled){
        setProperty(new BooleanProperty(ENABLE_ARRIVAL_CTRL, enabled));
    }
    
    /**
     * Returns whether a session arrival controller is used or not.
     *
     * @return true iff a session arrival controller is used.
     */
    public boolean isArrivalCtrlEnabled(){
        return getPropertyAsBoolean(ENABLE_ARRIVAL_CTRL, false);
    }
    
    /**
     * Set the session arrival formula.
     *
     * @param formula the formula.
     */
    public void setArrivalCtrlNumSessions(String formula){
        setProperty(new StringProperty(ARRIVAL_CTRL_NUM_SESSIONS, formula));
    }
    
    /**
     * Returns the session arrival formula.
     *
     * @return the formula.
     */
    public String getArrivalCtrlNumSessions(){
        return getPropertyAsString(ARRIVAL_CTRL_NUM_SESSIONS);
    }
    
    /**
     * Enables or disables the session arrival logging.
     *
     * @param enabled iff true, logging is enabled.
     */
    public void setArrivalCtrlLoggingEnabled(boolean enabled){
        setProperty(new BooleanProperty(ENABLE_ARRIVAL_CTRL_LOGGING, enabled));
    }
    
    /**
     * Returns whether session arrival logging is enabled or disabled.
     *
     * @return true iff logging is enabled.
     */
    public boolean isArrivalCtrlLoggingEnabled(){
        return getPropertyAsBoolean(ENABLE_ARRIVAL_CTRL_LOGGING, false);
    }
    
    /**
     * Set the session arrival log file.
     *
     * @param logfile the filename.
     */
    public void setArrivalCtrlLogfile(String logfile){
        setProperty(new StringProperty(ARRIVAL_CTRL_LOGFILE, logfile));
    }
    
    /** 
     * Returns the arrival log file.
     *
     * @return the filename.
     */
    public String getArrivalCtrlLogfile(){
        return getPropertyAsString(ARRIVAL_CTRL_LOGFILE);
    }
    
    /**
     * Sets the name of the child application states. Called by the model
     * listener when changes in terms of application state changes occur.
     * We use this list for generating the behavior file templates in GUI mode.
     *
     * @param names the state names.
     */
    public void setStateNames(List<String> names){
        this.stateNames = names;
    }
    
    /**
     * Returns the list of application states associated with this controller.
     *
     * @return the list of state names.
     */
    public List<String> getStateNames() {
        return this.stateNames;
    }
    
    /**
     * Sets the entry state of the user according to its assigned behavior
     * (during test execution).
     */
    private void setEntryState(){
        this.currentState = null;
        if(this.behavior==null){
            logger.fatalError("behavior is null");
            return;
        }
        this.currentState = this.stateMap.get(this.behavior.getEntryState());
    }
    
    /**
     * Returns the state name to state ID map.
     *
     * @return the map.
     */
    private Map<String,Integer> getStateName2IdMap(){
        Map<String,Integer> map = new HashMap(subControllersAndSamplers.size(),1);
        
        for (int i = 0; i < subControllersAndSamplers.size(); i++) {
            TestElement elem = (TestElement) subControllersAndSamplers.get(i);
            if (elem instanceof ApplicationState) {
                ApplicationState state = (ApplicationState) elem;
                map.put(new String(state.getName()),state.getId());
            }
        }
        return map;
    }
    
    /**
     * Initializes the behavior mix including behavior model instantiation
     * (during test execution).
     *
     * @throws BehaviorException when an error occurs.
     */
    private void initializeBehaviorMix() throws BehaviorException{
        /** @TODO lock access to ApplicationController.applicationBehaviorMixes
         *        based on application ids (semaphores) */
        synchronized(this.getClass()){
            /* note: executed once per application */
            this.behaviorMix = MarkovController.applicationBehaviorMixes.get(new Integer(this.getId()));
            if(this.behaviorMix==null){
                /* behavior mix for application id not yet initialized */
                this.behaviorMix = (BehaviorMix) this.getBehaviorMix().clone();
                this.behaviorMix.initialize(this.getStateName2IdMap());
                /* Add to map */
                MarkovController.applicationBehaviorMixes.put(new Integer(this.getId()),this.behaviorMix);
            }
        }
    }
    
    /**
     * Assigns a behavior to be used within a user session.
     *
     * @throws JMeterStopTestException when an error occurs.
     * @throws JMeterStopThreadException when an error occurs.
     */
    private void assignBehavior() throws JMeterStopTestException, JMeterStopThreadException{
        try{
        if(this.behaviorMix == null)
            this.initializeBehaviorMix();
        
        this.behavior = this.behaviorMix.getBehavior();
        if (this.behavior == null){
            logger.fatalError("No behavior assigned");
        }
        }catch(BehaviorException ex){
            logger.error(ex.getMessage(),ex);
            throw new JMeterStopTestException (ex.getMessage());
        }
    }
    
    /**
     * Initialize instance. Contains behavior assignment and model
     * initialization.
     *
     * @throws JMeterStopTestException when an error occurs.
     */
    @Override
    public void initialize() throws JMeterStopTestException {
        super.initialize();
        
        TestElement elem;
        for (int i = 0; i < subControllersAndSamplers.size(); i++) {
            elem = (TestElement) subControllersAndSamplers.get(i);
            if (elem instanceof ApplicationState) {
                ApplicationState state = (ApplicationState) elem;
                this.stateMap.put(state.getId(),state);
            }
        }
        
        this.sessionArrivalControllerEnabled = this.isArrivalCtrlEnabled();
        if (this.sessionArrivalControllerEnabled){
            this.sessionArrivalController = SessionArrivalController.getInstance();
            /* dangerous since several thread access this object but it works */
            this.sessionArrivalController.init(this.isArrivalCtrlLoggingEnabled(), this.getArrivalCtrlLogfile());
        }
        
        this.assignBehavior();
        this.setEntryState();
        this.inSession = false;
    }
    
    /**
     * Reinitalize instance for example after a test iteration. A behavior is
     * chosen from the behavior mix.
     *
     * @throws JMeterStopTestException when an error occurs.
     * @throws JMeterStopThreadException when an error occurs.
     */
    @Override
    protected void reInitialize() throws JMeterStopTestException, JMeterStopThreadException {
        super.reInitialize();
        this.assignBehavior();
        this.setEntryState();
        this.inSession = false;
    }
    
    /**
     * Generate random double in the range from 0.0 to d (exclusive).
     *
     * @param the limit.
     * @return the random number.
     */
    private double nextDouble(double d){
        return rand.nextDouble() * d;
    }
    
    /**
     * Evaluate expression using JavaScript.
     *
     * @param expr the String to evaluate.
     * @return the evaluation result.
     * @throws JavaScriptException when a Javascript error occurs.
     */
    private String evaluateExpression(String expr) throws JavaScriptException {
        String resultStr = "";
        try{
            Context cx = Context.enter();
            Scriptable scope = cx.initStandardObjects(null);
            Object cxResultObject = cx.evaluateString(scope, expr
                    /** * conditionString ** */
                    , "<cmd>", 1, null);
            resultStr = Context.toString(cxResultObject);
        }catch(JavaScriptException ex){
            logger.error(ex.getMessage(), ex);
            throw ex;
        } finally {
            Context.exit();
        }
        
        return resultStr;
    }
    
    /**
     * Evaluate the condition and return boolean evaluation value.
     *
     * @param cond condition String to evaluate.
     * @return evaluation result.
     */
    private boolean evaluateCondition(String cond) {
        logger.debug("    getCondition() : [" + cond + "]");
        
        String resultStr = "";
        boolean result = false;
        
        // now evaluate the condition using JavaScript
        if(cond.equals(""))
            return true;
        
        try{
            resultStr = evaluateExpression(cond);
            
            if (resultStr.equals("false")) {
                result = false;
            } else if (resultStr.equals("true")) {
                result = true;
            } else {
                throw new Exception(" BAD CONDITION :: " + cond);
            }
        } catch (Exception ex){
            logger.error(ex.getMessage(), ex);
        }
               
        return result;
    }
    
    /**
     * Execute the transition action which is a list of variable assignments
     * separated by ';'
     *
     * @param actionStr the action String.
     * @return true iff execution successful.
     */
    private boolean executeAction(String actionStr){
        StringTokenizer actionsTokenizer = new StringTokenizer(actionStr, ";");
        StringTokenizer actionTokenizer;
        boolean result = false;
        JMeterVariables threadVars = this.getThreadContext().getVariables();
        
        while (actionsTokenizer.hasMoreTokens()){
            String curAction = actionsTokenizer.nextToken();
            
            actionTokenizer = new StringTokenizer(curAction, "=");
            /**
             * @ TODO allow
             */
            if (actionTokenizer.countTokens() != 2 && actionTokenizer.countTokens() != 1){
                logger.error("Invalid action: " + curAction);
                return false;
            }
            
            try{
                if (actionTokenizer.countTokens() == 2){
                    /** Assignment var=expr */
                    String leftSide, rightSide;
                    
                    leftSide = actionTokenizer.nextToken().trim();
                    
                    rightSide = evaluateExpression(actionTokenizer.nextToken().trim());
                    
                    threadVars.put(leftSide, rightSide);
                }else{
                    /** Function expression (e.g. a function call) */
                    evaluateExpression(actionTokenizer.nextToken().trim());
                }
            } catch (Exception ex){
                logger.error(ex.getMessage(), ex);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Sets the next state based on the current.
     */
    public void transition(){
        Map<Integer,Double> transitionsProbabilityMap;
        ApplicationStateTransitions transitions = this.currentState.getTransitions();
        
        //System.out.print(Thread.currentThread().getName() + "-" + this.testIteration +
        //        "(" + (this.behavior!=null?this.behavior.getBName():"<NULL>") +")" + "::" +
        //        this.currentState.getName()+" -> ");
        //System.out.println("Transition probabilities:");
        transitionsProbabilityMap = this.behavior.getStateTransitionMap(this.currentState.getId());
        //System.out.println(transitionsProbabilityMap.toString());
        
        {
            List<ApplicationStateTransition> truelist = new ArrayList();
            List<Double> cumProbList = new ArrayList();
            List<ApplicationStateTransition> transitionslist = transitions.getTransitionsAsList();
            Iterator<ApplicationStateTransition> it = transitionslist.listIterator();
            /* add final state 0 to truelist and init curCumProb with its probability*/
            truelist.add(null);
            double curCumProb = transitionsProbabilityMap.get(0);
            cumProbList.add(curCumProb);
            for(int i=0; it.hasNext(); i++){
                ApplicationStateTransition cur = it.next();
                
                double curProb = transitionsProbabilityMap.get(cur.getDstStateId());
                if (!cur.isDisabled() && !(curProb<=0) && evaluateCondition(cur.getGuard())) {
                    truelist.add(cur);
                    curCumProb+=curProb;
                    cumProbList.add(curCumProb);
                }
            }
            
            // cumProbList contains at least one entry!
            double rndVal = nextDouble(cumProbList.get(cumProbList.size()-1));
            
            //System.out.print("rndVal:" + rndVal + "->" /* + "\ncumProbList: " +
            //cumProbList.toString() + "\ntruelist: " + truelist.toString()*/);
            /* pick next state based on rndVal */
            ApplicationStateTransition nextTransition = truelist.get(0);
            for (int i=0; i<truelist.size()-1; i++){
                if(rndVal<cumProbList.get(i))
                    break;
                nextTransition = truelist.get(i+1);
            }
            /**
             * @TODO - execute action
             */
            if (nextTransition != null){
                if (!this.executeAction(nextTransition.getAction())){
                    ;
                }
            }
            
            this.currentState = nextTransition!=null?this.stateMap.get(nextTransition.getDstStateId()):null;
            //System.out.println(this.currentState!=null?this.currentState.getName():new String("<EXIT>"));
        }
    }
    
    /**
     * @see org.apache.jmeter.control.GenericController#next
     */
    @Override
    public Sampler next() throws JMeterStopTestException {
        int numSessions;
        /* If we're not in a session, we must pass here */
        if (this.sessionArrivalControllerEnabled && !this.inSession){
            this.mustReduceNumThreads = true;
            try{
                /**  */
                this.sessionArrivalController.enterSession(Integer.parseInt(this.getArrivalCtrlNumSessions()));
                this.inSession = true;
            }catch (Exception ex){
                this.sessionArrivalController.exitSession();
                logger.error(ex.getMessage(),ex);
                throw new JMeterStopTestException (ex.getMessage());
            }
        }
        
        fireIterEvents();
        
        Sampler returnValue = null;
        try {
            if (this.currentState == null) {
                returnValue = nextIsNull();
            } else {
                returnValue = this.nextIsAController(this.currentState);
            }
        } catch (NextIsNullException e) {
            returnValue = null;
        }
        
        /* exit session. */
        /**
         * @TODO: check why returnValue==null occurs twice
         *        (reason for variable mustReduceNumThreads)
         */
        if(this.sessionArrivalControllerEnabled && returnValue==null){
            if(mustReduceNumThreads){
                mustReduceNumThreads = false;
                this.sessionArrivalController.exitSession();
                this.inSession = false;
            }
        }
        
        return returnValue;
    }
    
    /**
     * @see org.apache.jmeter.control.GenericController#nextIsNull
     */
    @Override
    protected Sampler nextIsNull() throws NextIsNullException {
        this.reInitialize();
        return null;
    }
    
    /**
     * @see org.apache.jmeter.control.GenericController#currentReturnedNull
     */
    @Override
    protected void currentReturnedNull(Controller c) {
        this.transition();
    }
    
    /**
     * Notifies session arrival controller that test started (workaround).
     */
    public void testStarted() {
        /* Initialize whether needed or not  */
        SessionArrivalController.testStarted();
    }
    
    /**
     * Notifies session arrival controller that test ended (workaround).
     */
    public void testEnded() {
        /* Reset behavior mixes for all applications. Forces a reload on each
           test start */
        this.testIteration = -1;
        synchronized (this.getClass()){
            MarkovController.applicationBehaviorMixes.clear();
        }
        
        /* Notify arrival controller about test end */
        SessionArrivalController.testEnded();
    }
    
    /**
     * Notifies session arrival controller that test started (workaround).
     *
     * @param host the host.
     */
    public void testStarted(String host) {
        this.testStarted();
    }
    
    /**
     * Notifies session arrival controller that test ended (workaround).
     *
     * @param host the host.
     */
    public void testEnded(String host) {
        this.testEnded();
    }
    
    /**
     * Increments iteration count.
     *
     * @param event the event.
     */
    public void testIterationStart(LoopIterationEvent event) {
        this.testIteration++;
    }
    
    /**
     * Clears the controller instance.
     */
    @Override
    public void clear() {
        super.clear();
        if (this.stateMap != null) // required for distributed mode
            this.stateMap.clear();
        if (this.stateNames != null) // required for distributed mode
            this.stateNames.clear();
    }

    public void threadStarted() {
    }

    /**
     * Iff in session, the session arrival controller is notified about exiting
     * the session.
     */
    public void threadFinished() {
        if(this.sessionArrivalControllerEnabled && this.inSession){
            /* exit session otherwise other guys waiting sleep forever */
            this.sessionArrivalController.exitSession();
            this.inSession = false;
        }
    }
}

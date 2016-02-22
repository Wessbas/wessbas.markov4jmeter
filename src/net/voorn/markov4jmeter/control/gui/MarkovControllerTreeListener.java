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



package net.voorn.markov4jmeter.control.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import net.voorn.markov4jmeter.control.MarkovController;
import net.voorn.markov4jmeter.control.ApplicationState;

import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.testelement.TestElement;

/**
 * <p>Listens for changing MarkovController nodes within the tree to update
 * the transitions inside the ApplicationStates.</p>
 *
 * <p>This class is used as a singleton class, i.e. instances are not 
 * instantiatable from outside but a reference to the singleton instance is 
 * returned by calling getInstance().</p>
 *
 * @author Andr&eacute; van Hoorn
 */
public class MarkovControllerTreeListener implements TreeModelListener {
    
    private static MarkovControllerTreeListener listener = null;
    
    /** Maps state ids to state names */
    private Map<Integer,String> stateNames = new HashMap();
    
    /** Maps controller IDs to controllers. */
    private Map<Integer,String> controllers = new HashMap();
    
    /** Random number generator to be used within this class. */
    private java.util.Random rnd = new java.util.Random();
    
    /** Not to be instantiated from outside */
    private MarkovControllerTreeListener() {
    }
    
    /**
     * Returns the name for a state with given ID.
     *
     * @param id the ID.
     * @return the state name or null if state does not exist
     */
    public String lookupStateName(int id){
        String name = stateNames.get(new Integer(id));
        if (name == null){
            System.out.println("WARNING: Registered state name for " + id + " is null!");
            return "<null>";
        }
        return new String(name);
    }
    
    /**
     * Debug function dumping the current set of states.
     */
    public void dumpStateNames(){
        Set<Map.Entry<Integer,String>> entries = stateNames.entrySet();
        Iterator<Map.Entry<Integer,String>> it = entries.iterator();        
        Map.Entry<Integer,String> entry;
        
        System.out.println("State entries -------");
        while (it.hasNext()){
            entry = it.next();
            System.out.println("#"+entry.getKey().intValue()+": "+entry.getValue());
        }
        System.out.println("---------------------");
    }

    /**
     * Registers the given controller.
     *
     * @param ctrl the controller.
     */
    public void registerController(MarkovController ctrl){
        int id = rnd.nextInt();
        
        while (id<=0 || controllers.containsKey(new Integer(id)))
            id = rnd.nextInt();
        
        ctrl.setId(id);
        controllers.put(new Integer(id), null);
        //System.out.println("Registering state id '"+id+"' with name '"+state.getName()+"' ");
        
        //this.dumpStateNames();
    }
    
    /**
     * Registers the given state name.
     *
     * @param state the state.
     */
    public void registerState(ApplicationState state){
        int id = rnd.nextInt();
        
        while (id<=0 || stateNames.containsKey(new Integer(id)))
            id = rnd.nextInt();
        
        state.setId(id);
        stateNames.put(new Integer(id), new String(state.getName()));
        //System.out.println("Registering state id '"+id+"' with name '"+state.getName()+"' ");
        
        //this.dumpStateNames();
    }
    
    /**
     * Sets the name of the state with the given id to the given name.
     * If no entry for this state exists it is created.
     *
     * @param state the state.
     */
    public void updateStateName(ApplicationState state){
        int id = state.getId();
        //system.out.println("Updating state '"+id+"' ...");
        String name = (state.getName()!=null)?state.getName():new String("<null>");
        
        String oldName = stateNames.remove(new Integer(id));
        oldName = (oldName!=null)?oldName:new String("<null>");

        //System.out.print("Updating name of state id '"+id+"':");
        //System.out.println(" '"+oldName+"' -> '"+name+"'");        
        stateNames.put(new Integer(id), new String(name));
        
        //this.dumpStateNames();
    }

    /**
     * Returns a list of all application states under the given controller.
     * 
     * @param ac the controller.
     * @return the list of application states.
     */
    public List<ApplicationState> getApplicationStatesForController(MarkovController ac){
        List<ApplicationState> stateList = new ArrayList<ApplicationState>();
        
        JMeterTreeNode node = GuiPackage.getInstance().getNodeOf(ac);
        
        for (int i = 0; i < node.getChildCount(); i++) {
            JMeterTreeNode cur = (JMeterTreeNode) node.getChildAt(i);
            TestElement te = cur.getTestElement();
            if (te instanceof ApplicationState){
                ApplicationState state = (ApplicationState) te;
                stateList.add(state);
            }
        }        
        
        return stateList;
    }
    
    /**
     * Update all application states under the given controller.
     * Also asserts that all states listed in state name map and provides
     * controller with list of state names.
     *
     * @param ac the controller.
     */
    private void updateStates(MarkovController ac){
        JMeterTreeNode node = GuiPackage.getInstance().getNodeOf(ac);
        List<Integer> stateIds = new ArrayList();
        List<String> stateNames = new ArrayList();
        
        /* get all state names and ids */
        for (int i = 0; i < node.getChildCount(); i++) {
            JMeterTreeNode cur = (JMeterTreeNode) node.getChildAt(i);
            TestElement te = cur.getTestElement();
            if (te instanceof ApplicationState){
                ApplicationState state = (ApplicationState) te;
                int id = state.getId();
                if (id == 0){
                    /* this must not happen */
                    System.out.println("WARNING: State "+state.getName()+"has no id yet");
                }
                stateIds.add(id); 
                stateNames.add(state.getName());
                /* Add not existing entries to map. 
                 * Particularly this covers the case when a jmx file is loaded */
                if (!this.stateNames.containsKey(new Integer(id)))
                    this.updateStateName(state);
            }
        }
        
        ac.setStateNames(stateNames);
        
        /* Pass information to all states  */
        for (int i = 0; i < node.getChildCount(); i++) {
            JMeterTreeNode cur = (JMeterTreeNode) node.getChildAt(i);
            TestElement te = cur.getTestElement();
            if (te instanceof ApplicationState){
                ApplicationState state = (ApplicationState) te;
                state.updateTransitions(stateIds);
            }
        }
    }
    
    /**
     * Updates the application states underneath the affected controller.
     *
     * @param ev the event that triggered the call.
     */
    public void treeNodesChanged(TreeModelEvent ev) {
        //System.out.println("Tree nodes changed");
        JMeterTreeNode node = (JMeterTreeNode) ev.getTreePath().getLastPathComponent();
        TestElement te = node.getTestElement();
        if (te instanceof MarkovController){
            MarkovController ac = (MarkovController) te;
            updateStates(ac);
        }
    }
    
    /**
     * Updates the application states underneath the affected controller.
     *
     * @param ev the event that triggered the call.
     */
    public void treeNodesInserted(TreeModelEvent ev) {
        JMeterTreeNode node = (JMeterTreeNode) ev.getTreePath().getLastPathComponent();
        TestElement te = node.getTestElement();
        if (te instanceof MarkovController){
            MarkovController ac = (MarkovController) te;
//            System.out.println("Inserting "+ev.getChildIndices().length+" nodes under "+ac.getName()+"...");
//            for (int i=0; i<ev.getChildIndices().length; i++ ){
//                JMeterTreeNode child = (JMeterTreeNode) node.getChildAt(i);
//                String name = child.getName();
//                TestElement stateTe = child.getTestElement();
//                if (stateTe instanceof ApplicationState){
//                    ApplicationState state = (ApplicationState) stateTe;
//                    MarkovControllerTreeListener.getInstance().registerState(state);
//                }
//            }
            //System.out.println("Inserted "+ev.getChildIndices().length+" nodes under "+ac.getName()+"...");
            
            updateStates(ac);
        }
    }
    
    /**
     * Updates the application states underneath the affected controller.
     *
     * @param ev the event that triggered the call.
     */
    public void treeNodesRemoved(TreeModelEvent ev) {
        JMeterTreeNode node = (JMeterTreeNode) ev.getTreePath().getLastPathComponent();
        TestElement te = node.getTestElement();
        if (te instanceof MarkovController){
            MarkovController ac = (MarkovController) te;
            updateStates(ac);
        }
    }
    
    /**
     * Updates the application states underneath the affected controller.
     *
     * @param ev the event that triggered the call.
     */
    public void treeStructureChanged(TreeModelEvent ev) {
        JMeterTreeNode node = (JMeterTreeNode) ev.getTreePath().getLastPathComponent();
        TestElement te = node.getTestElement();
        if (te instanceof MarkovController){
            MarkovController ac = (MarkovController) te;
            //System.out.println("Node structure changed under " + ac.getName());
            updateStates(ac);
        }
    }
    
    /**
     * Registers an instance (singleton) of the listener within the tree model.
     * If an instance has been registered before, this method returns immediately.
     */
    public static synchronized void registerListener(){
        if (listener != null)
            return;
        
        /* instantiate singleton instance and register it */
        listener = new MarkovControllerTreeListener();
        GuiPackage.getInstance().getTreeModel().addTreeModelListener(listener);
    }
    
    /**
     * Returns listener instance. If not yet registered, this is done as a 
     * side-effect.
     */
    public static synchronized MarkovControllerTreeListener getInstance(){
        if (listener == null)
            listener = new MarkovControllerTreeListener();
        registerListener();
        
        return listener;
    }
}

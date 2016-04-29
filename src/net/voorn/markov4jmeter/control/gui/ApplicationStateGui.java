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

import java.awt.BorderLayout;
import java.awt.Container;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.testelement.TestElement;

import net.voorn.markov4jmeter.control.ApplicationState;
import net.voorn.markov4jmeter.util.Markov4JMeterVersion;
import org.apache.jmeter.gui.util.VerticalPanel;

/**
 * GUI element to create and modify an ApplicationState. 
 *
 * An ApplicationsStateTransitionsPanel is included in order to define the 
 * outgoing transitions for this state including guards and actions.
 *
 * @author Andr&eacute; van Hoorn
 */
public class ApplicationStateGui extends org.apache.jmeter.control.gui.AbstractControllerGui {
    
    /** A panel to contain comments on the application state. */
    private JTextArea commentPanel;    
    
    /** A panel allowing the user to define the application transitions
     *  outgoing from this application state. */
    private ApplicationStateTransitionsPanel transitionsPanel;
    
    /** Panel containing the state ID. */
    private JTextField  idPanel;
    
    /** To notify the treeModel that a node has changed */
    private String oldName;
    
    /**
     * Creates a new instance of ApplicationStateGui
     */
    public ApplicationStateGui() {
        init();
    }
    
    /**
     * @see org.apache.jmeter.control.gui.AbstractControllerGui#getStaticLabel
     */
    @Override
    public String getStaticLabel(){
        return "Markov State";
    }
    
   /**
     * @see org.apache.jmeter.control.gui.AbstractControllerGui#getLabelResource
     */
    public String getLabelResource() {
        return "application_state";
    }
    
   /**
     * @see org.apache.jmeter.control.gui.JMeterGUIComponent#createTestElement
     */
    public ApplicationState createTestElement() {
        ApplicationState state = new ApplicationState();
        super.configureTestElement(state);
        /* We have to register the new state */
        MarkovControllerTreeListener.getInstance().registerState(state);
        return state;
    }
    
   /**
     * @see org.apache.jmeter.control.gui.JMeterGUIComponent#configure
     */
    @Override
    public void configure(TestElement elt) {
        super.configure(elt);
        if (elt instanceof ApplicationState) {
            ApplicationState state = (ApplicationState) elt;
            commentPanel.setText(state.getPropertyAsString(ApplicationState.COMMENTS));
            transitionsPanel.setTransitions(state.getTransitions());
            idPanel.setText(state.getId()+"");
            this.oldName = new String(state.getName());
        }
    }
    
   /**
     * @see org.apache.jmeter.control.gui.JMeterGUIComponent#modifyTestElement
     */
    public void modifyTestElement(TestElement elt) {
        super.configureTestElement(elt);
        if (elt instanceof ApplicationState) {
            ApplicationState state = (ApplicationState) elt;
            state.setProperty(ApplicationState.COMMENTS, commentPanel.getText());
            state.setTransitions(transitionsPanel.getTransitions());
            // never update the id!
            if (this.oldName != null && !this.oldName.equals(state.getName())){
                MarkovControllerTreeListener.getInstance().updateStateName(state);
            }
        }
    }
    
    /**
     * Creates the comment panel.
     */
    private Container createCommentPanel() {
        Container panel = makeTitlePanel();
        commentPanel = new JTextArea();
        JLabel label = new JLabel(JMeterUtils.getResString("testplan_comments"));
        label.setLabelFor(commentPanel);
        // JMeter GUI components already have a text input field by default;
        // leave the lines above as they are, otherwise the comment panel will
        // be packed to minimum size -> components will be hidden;
        //        panel.add(label);
        //        panel.add(commentPanel);
        return panel;
    }
    
    /**
     * Creates the ID panel.
     */
    private Container createIdPanel() {
        Container panel = new VerticalPanel();
        JLabel label = new JLabel("Id:");
        idPanel = new JTextField(15);
        label.setLabelFor(idPanel);
        panel.add(label);
        panel.add(idPanel);
        return panel;
    }
    
    /**
     * Create a panel allowing the user to define transitions for the states.
     *
     * @return the panel.
     */
    private JPanel createTransitionsPanel() {
        transitionsPanel = new ApplicationStateTransitionsPanel("State Transitions");
        
        return transitionsPanel;
    }
    
    /**
     * Initialize the components and layout of this component.
     */
    private void init() {
        setLayout(new BorderLayout(10, 10));
        setBorder(makeBorder());
        
        // JMeter GUI components already have a text input field by default,
        // so just add a default title panel instead;
        //add(makeTitlePanel(), BorderLayout.NORTH);
         add(createCommentPanel(), BorderLayout.NORTH);

        //add(createIdPanel(), BorderLayout.SOUTH);
        createIdPanel();
        add(createTransitionsPanel(), BorderLayout.CENTER);
        add(new JLabel("Markov4JMeter version: " + Markov4JMeterVersion.getVERSION()), BorderLayout.SOUTH);
    }    
}

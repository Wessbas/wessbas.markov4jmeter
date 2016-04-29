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
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.Iterator;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import net.voorn.markov4jmeter.util.LockableJTable;
import org.apache.jmeter.testelement.property.PropertyIterator;
import org.apache.jorphan.gui.ObjectTableModel;
import org.apache.jorphan.reflect.Functor;

import net.voorn.markov4jmeter.control.ApplicationStateTransition;
import net.voorn.markov4jmeter.control.ApplicationStateTransitions;


/**
 * A GUI panel allowing the user to enter the application transitions, i.e.
 * a list of tuples containing the destination state, a guard and an action.
 *
 * @author Andr&eacute; van Hoorn
 */
public class ApplicationStateTransitionsPanel extends JPanel {
    
    /** The title label for this component. */
    private JLabel tableLabel;
    
    /** The table containing the list of behaviors. */
    private transient LockableJTable table;
    
    /** The model for the behavior table. */
    protected transient ObjectTableModel tableModel;
    
    /**
     * Added background support for reporting tool
     */
    private Color background;
    
    //public static final String COL_DST_STATE_ID = "Dst State #:";
    
    /** Label for destination state column */
    public static final String COL_DST_STATE = "Destination State:";

    /** Label for destination state disable/enable column */
    public static final String COL_DISABLED = "Disabled:";
    
    /** Label for destination guard column */
    public static final String COL_GUARD = "Guard:";
    
    /** Label for destination action column */
    private static final String COL_ACTION = "Action:";
    
    /**
     * Create a new ApplicationStateTransitionsPanel as an embedded component, using the specified
     * title.
     * 
     * @param label the title for the component.
     */
    public ApplicationStateTransitionsPanel(String label) {
        tableLabel = new JLabel(label);
        init();
    }
    
    /**
     * Sets the transitions.
     *
     * @param transitions the transitions.
     */
    public void setTransitions(ApplicationStateTransitions transitions) {
        this.clear();
        PropertyIterator iter = transitions.iterator();
        while (iter.hasNext()) {
            ApplicationStateTransition transition = (ApplicationStateTransition) iter.next().getObjectValue();
            tableModel.addRow(transition);
        }
    }
    
    /**
     * Returns the transitions.
     *
     * @return the transitions.
     */
    public ApplicationStateTransitions getTransitions(){
	stopTableEditing();
        Iterator modelData = tableModel.iterator();
        ApplicationStateTransitions transitions = new ApplicationStateTransitions();
        while (modelData.hasNext()) {
            ApplicationStateTransition transition = (ApplicationStateTransition) modelData.next();
            transitions.addTransition(transition);
        }
        return transitions;
    }
    
    /**
     * Get the table used to enter behaviors.
     *
     * @return the table used to enter arguments
     */
    protected JTable getTable() {
        return table;
    }
    
    /**
     * Get the title label for this component.
     *
     * @return the title label displayed with the table
     */
    protected JLabel getTableLabel() {
        return tableLabel;
    }
        
    /**
     * Clear all rows from the table. T.Elanjchezhiyan(chezhiyan@siptech.co.in)
     */
    public void clear() {
        stopTableEditing();
        tableModel.clearData();
    }
    
    /**
     * Stop any editing that is currently being done on the table. This will
     * save any changes that have already been made.
     */
    protected void stopTableEditing() {
        if (table.isEditing()) {
            TableCellEditor cellEditor = table.getCellEditor(table.getEditingRow(), table.getEditingColumn());
            cellEditor.stopCellEditing();
            
        }
    }
    
    /**
     * Initialize the table model used for the behavior mix table.
     */
    protected void initializeTableModel() {
        tableModel = new ObjectTableModel(
                new String[] { /*COL_DST_STATE_ID,*/ COL_DST_STATE, COL_DISABLED, COL_GUARD, COL_ACTION },
                new Functor[] {
            /*new Functor("getDstStateId"),*/ // $NON-NLS-1$
            new Functor("getDstStateName"), // $NON-NLS-1$
            new Functor("isDisabled"), // $NON-NLS-1$
            new Functor("getGuard"), // $NON-NLS-1$
            new Functor("getAction")}, // $NON-NLS-1$
                
                new Functor[] {
            /*new Functor("setDstStateId"),*/ // $NON-NLS-1$
            new Functor("setDstStateName"), // $NON-NLS-1$
            new Functor("setDisabled"), // $NON-NLS-1$
            new Functor("setGuard"),
            new Functor("setAction") }, // $NON-NLS-1$
                new Class[] { /*Integer.class,*/ String.class, Boolean.class, String.class, String.class }
        );
    }
    
    /**
     * Set fix column width.
     */
    private void fixSize(TableColumn column, boolean resizable) {
            column.sizeWidthToFit();
            // column.setMinWidth(column.getWidth());
            column.setMaxWidth((int) (column.getWidth() * 1.5));
            column.setWidth(column.getMaxWidth());
            column.setResizable(resizable);
    }
    
    /**
     * Resize the table columns to appropriate widths.
     *
     * @param table the table.
     */
    protected void sizeColumns(JTable table) {
            int resizeMode = table.getAutoResizeMode();
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            fixSize(table.getColumn(COL_DST_STATE),true);
            fixSize(table.getColumn(COL_DISABLED),false);
            table.setAutoResizeMode(resizeMode);
    }
    
    /**
     * Create a scroll panel that sets it's preferred size to it's minimum size.
     * Explicitly for scroll panes that live inside other scroll panes, or
     * within containers that stretch components to fill the area they exist in.
     * Use this for any component you would put in a scroll pane (such as
     * TextAreas, tables, JLists, etc). It is here for convenience and to avoid
     * duplicate code. JMeter displays best if you follow this custom.
     *
     * @param comp the component which should be placed inside the scroll pane.
     * @return a JScrollPane containing the specified component.
     */
    protected JScrollPane makeScrollPane(Component comp) {
        JScrollPane pane = new JScrollPane(comp);
        pane.setPreferredSize(pane.getMinimumSize());
        return pane;
    }
    
    /**
     * Create the main GUI panel which contains the argument table.
     *
     * @return the main GUI panel.
     */
    private Component makeMainPanel() {
        initializeTableModel();
        table = new LockableJTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setColIsEditable(0,false); // Dst State #
        table.setColIsEditable(1, false); // Dst State
        if (this.background != null) {
            table.setBackground(this.background);
        }
        return makeScrollPane(table);
    }
    
    /**
     * Create a panel containing the title label for the table.
     *
     * @return a panel containing the title label
     */
    protected Component makeLabelPanel() {
        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        labelPanel.add(tableLabel);
        if (this.background != null) {
            labelPanel.setBackground(this.background);
        }
        return labelPanel;
    }
    
    /**
     * Initialize the components and layout of this component.
     */
    private void init() {
        JPanel p = this;
        
        p.setLayout(new BorderLayout());
        
        p.add(makeLabelPanel(), BorderLayout.NORTH);
        p.add(makeMainPanel(), BorderLayout.CENTER);
        // Force a minimum table height of 70 pixels
        p.add(Box.createVerticalStrut(70), BorderLayout.WEST);
        
        table.revalidate();
        sizeColumns(table);
    }
}

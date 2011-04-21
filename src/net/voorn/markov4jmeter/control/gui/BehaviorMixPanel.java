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
package net.voorn.markov4jmeter.control.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import java.util.Map;
import java.util.StringTokenizer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellEditor;

import net.voorn.markov4jmeter.control.ApplicationState;
import net.voorn.markov4jmeter.control.ApplicationStateTransition;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.testelement.property.PropertyIterator;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.gui.ObjectTableModel;
import org.apache.jorphan.reflect.Functor;

import net.voorn.markov4jmeter.control.BehaviorMixEntry;
import net.voorn.markov4jmeter.control.BehaviorMix;
import net.voorn.markov4jmeter.control.MarkovController;
import net.voorn.markov4jmeter.util.Markov4JMeterFileFilter;
import org.apache.jmeter.gui.JMeterFileFilter;

/**
 * A GUI panel allowing the user to enter the user behavior mix.
 *
 * @author Andr&eacute; van Hoorn
 */
public class BehaviorMixPanel extends JPanel implements ActionListener {

    /** The title label for this component. */
    private JLabel tableLabel;
    /** The table containing the list of behaviors. */
    private transient JTable table;
    /** The model for the behavior table. */
    protected transient ObjectTableModel tableModel;
    /** A button for adding new behavior entry to the table. */
    private JButton add;
    /** A button for removing behavior entries from the table. */
    private JButton delete;
    /** A button for generating a template file. */
    private JButton template;
    /** A button for exporting a visualization of the application model. */
    private JButton exportAppModel;
    /** List of state names. Needed for generating template file */
    private List<String> stateNames;
    /**
     * Added background support for reporting tool
     */
    private Color background;
    /** Command for adding a row to the table. */
    private static final String ADD = "add"; // $NON-NLS-1$
    /** Command for removing a row from the table. */
    private static final String DELETE = "delete"; // $NON-NLS-1$
    /** Command for exporting a template file. */
    private static final String GEN_TEMPLATE = "template"; // $NON-NLS-1$
    /** Command for exporting a visualization of the application model. */
    private static final String EXPORT_APPMODEL = "export"; // $NON-NLS-1$
    /** Label for the behavior name column. */
    public static final String COLUMN_NAMES_0 = JMeterUtils.getResString("name"); // $NON-NLS-1$
    /** Label for the relative frequency column. */
    public static final String COLUMN_NAMES_1 = "Relative frequency:";
    /* Name of the filename column. */
    private static final String COLUMN_NAMES_2 = "Filename:";

    /**
     * Create a new BehaviorMixPanel as an embedded component, using the specified
     * title.
     * 
     * @param label the title for the component.
     */
    public BehaviorMixPanel(String label) {
        tableLabel = new JLabel(label);
        init();
    }

    /**
     * A newly created component can be initialized with the contents of a
     * BehaviorMix Element object by calling this method. The component is
     * responsible for querying the Test Element object for the relevant
     * information to display in its GUI.
     * 
     * @param mix the BehaviorMix to configure.
     */
    public void setBehaviorMix(BehaviorMix mix) {
        this.clear();
        PropertyIterator iter = mix.iterator();
        while (iter.hasNext()) {
            BehaviorMixEntry behv =
                    (BehaviorMixEntry) iter.next().getObjectValue();
            tableModel.addRow(behv);
        }
        checkDeleteStatus();
    }

    /**
     * Returns the behavior mix.
     *
     * @return the behavior mix.
     */
    public BehaviorMix getBehaviorMix() {
        stopTableEditing();
        Iterator modelData = tableModel.iterator();
        BehaviorMix mix = new BehaviorMix();
        while (modelData.hasNext()) {
            BehaviorMixEntry entry =
                    (BehaviorMixEntry) modelData.next();
            mix.addBehaviorEntry(entry);
        }
        return mix;
    }

    /**
     * Sets the state names.
     *
     * @param stateNames the list of state names.
     */
    public void setStateNames(List<String> stateNames) {
        this.stateNames = stateNames;
    }

    /**
     * Returns the table used to enter behaviors.
     *
     * @return the table.
     */
    protected JTable getTable() {
        return table;
    }

    /**
     * Get the title label for this component.
     *
     * @return the title label displayed with the table.
     */
    protected JLabel getTableLabel() {
        return tableLabel;
    }

    /**
     * Get the button used to delete rows from the table.
     *
     * @return the button used to delete rows from the table.
     */
    protected JButton getDeleteButton() {
        return delete;
    }

    /**
     * Get the button used to add rows to the table.
     *
     * @return the button used to add rows to the table.
     */
    protected JButton getAddButton() {
        return add;
    }

    /**
     * Get the button used to generate a template file.
     *
     * @return the button used to generate a template file.
     */
    protected JButton getTemplateButton() {
        return template;
    }

    /**
     * Enable or disable the delete button depending on whether or not there is
     * a row to be deleted.
     */
    protected void checkDeleteStatus() {
        // Disable DELETE if there are no rows in the table to delete.
        if (tableModel.getRowCount() == 0) {
            delete.setEnabled(false);
        } else {
            delete.setEnabled(true);
        }
    }

    /**
     * Clear all rows from the table. T.Elanjchezhiyan(chezhiyan@siptech.co.in)
     */
    public void clear() {
        stopTableEditing();
        tableModel.clearData();
    }

    /**
     * Invoked when an action occurs. This implementation supports the add,
     * delete and generate template buttons.
     *
     * @param e the event that has occurred.
     */
    public void actionPerformed(ActionEvent e) {
        String action = e.getActionCommand();
        if (action.equals(DELETE)) {
            deleteArgument();
        } else if (action.equals(ADD)) {
            addBehavior();
        } else if (action.equals(GEN_TEMPLATE)) {
            genTemplate();
        } else if (action.equals(EXPORT_APPMODEL)) {
            exportModels();
        }
    }

    /**
     * Remove the currently selected argument from the table.
     */
    protected void deleteArgument() {
        // If a table cell is being edited, we must cancel the editing before
        // deleting the row
        if (table.isEditing()) {
            TableCellEditor cellEditor = table.getCellEditor(table.getEditingRow(), table.getEditingColumn());
            cellEditor.cancelCellEditing();
        }

        int rowSelected = table.getSelectedRow();
        if (rowSelected >= 0) {
            tableModel.removeRow(rowSelected);
            tableModel.fireTableDataChanged();

            // Disable DELETE if there are no rows in the table to delete.
            if (tableModel.getRowCount() == 0) {
                delete.setEnabled(false);
            } // Table still contains one or more rows, so highlight (select)
            // the appropriate one.
            else {
                int rowToSelect = rowSelected;

                if (rowSelected >= tableModel.getRowCount()) {
                    rowToSelect = rowSelected - 1;
                }

                table.setRowSelectionInterval(rowToSelect, rowToSelect);
            }
        }
    }

    /**
     * Removes copy with leading and trailing quotes being removed.
     *
     * @param the resulting String.
     */
    private String removeQuotes(String str) {
        int startIdx = str.startsWith("\"") ? 1 : 0;
        return str.substring(startIdx, str.endsWith("\"") ? str.length() - 1 : str.length());
    }

    private void exportBehaviorModel(String behaviorfn, String outfn) throws Exception {
        Map<String, Integer> stateName2Id = new HashMap();

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(new File(behaviorfn)));
            StringTokenizer token;
            String line;
            String stateName;
            boolean foundFinalState = false; // the '$' character
            boolean foundEntryState = false; // the trailing '*'

            int curValCol = 0, exitStateCol = -1;

            FileWriter writer = new FileWriter(new File(outfn));
            writer.write("digraph G { \n");
            String labelmainfontsize = "12.0";
            String labelsmallfontsize = "8.0";
            writer.write(" label=<Behavior Model exported by Markov4JMeter<br align=\"left\"/><font point-size=\"" + labelsmallfontsize + "\">" +
                    "image created on " + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(new Date()) + "<br align=\"left\"/></font>>; " +
                    " fontcolor=\"#000000\"; fontname=\"Arial\"; labelfontname=\"Arial\"; fontsize=\"" + labelmainfontsize + "\"; labeljust=\"l\"" +
                    " labelloc=\"t\"; remincross=\"true\";\n");

            /* read header */
            line = reader.readLine();
            token = new StringTokenizer(line, ","); // does not consider space as token
            int numStates = token.countTokens() - 1;
            writer.write("entry " + "[shape=\"point\"];\n");
            for (int valCol = 0; token.hasMoreTokens(); valCol++) {
                stateName = removeQuotes(token.nextToken().trim());
                if (stateName.equals("$")) {
                    foundFinalState = true;
                    exitStateCol = valCol;
                } else {
                    int stateId = curValCol++;
                    stateName2Id.put(stateName, stateId);
                    writer.write("" + stateId + " " + "[label=\"" + stateName + "\", peripheries=1, fontname=\"Arial\" ];\n");
                }
            }

            /* now we know that stateNames contains all statenames in 
             * stateNames2Ids as well as "$"
             *
            /* read values */
            int numLines = 0;
            for (line = reader.readLine(); line != null; line = reader.readLine()) {
                numLines++;
                token = new StringTokenizer(line, ",");
                if (token.countTokens() != numStates + 2) {
                    throw new Exception("Invalid column count (" +
                            token.countTokens() + ") in row " + numLines);
                }

                stateName = removeQuotes(token.nextToken().trim());

                /* is the state marked as entry state? */
                if (stateName.endsWith("*")) {
                    stateName = stateName.substring(0, stateName.length() - 1);
                    // TODO: mark entry state
                    foundEntryState = true;
                    writer.write("entry" + " -> " + stateName2Id.get(stateName) + " [label = \"\", fontname=\"Arial\"];\n");
                }

                for (int i = 0; token.hasMoreTokens(); i++) {
                    Double p = Double.parseDouble(removeQuotes(token.nextToken()));
                    if (p > 0) {
                        if (i == exitStateCol) {
                            writer.write("0" + stateName2Id.get(stateName) + " " + "[label=\"" + "Exit" + "\", peripheries=0, fontname=\"Arial\" ];\n");
                        }
                        writer.write(stateName2Id.get(stateName) + " -> " + (i == exitStateCol ? "0" + stateName2Id.get(stateName) : i) + " [label = \"" + p + "\", fontname=\"Arial\"];\n");
                    }
                }

                if (!foundEntryState) {
                    throw new Exception("No state marked as entry state");
                }
            }
            writer.write("}");
            writer.close();
        } catch (Exception ex) {
            System.out.println("Error while loading behavior file");
            throw new Exception(ex.getMessage(), ex);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    throw new Exception(ex.getMessage(), ex);
                }
            }
        }
    }

    private void exportBehaviorModels(String fnPrefix) throws Exception {
        BehaviorMix bmix = this.getBehaviorMix();
        System.out.println("Bmix has " + bmix.getBehaviorCount() + " entries");

        PropertyIterator iter = getBehaviorMix().iterator();
        while (iter.hasNext()) {
            BehaviorMixEntry b = (BehaviorMixEntry) iter.next().getObjectValue();
            String curBhvName = b.getBName();
            String curFn = b.getFilename();
            System.out.println("Exporting bhv model " + b.getBName());
            this.exportBehaviorModel(curFn, fnPrefix + ".behv-" + curBhvName + ".dot");
        }
    }

    private void exportApplicationModel(String outFn) throws Exception {
        MarkovController mc = (MarkovController) GuiPackage.getInstance().getCurrentElement();
        List<ApplicationState> stateList = MarkovControllerTreeListener.getInstance().getApplicationStatesForController(mc);

        FileWriter writer = new FileWriter(new File(outFn));
        writer.write("digraph G { \n");
        String labelmainfontsize = "12.0";
        String labelsmallfontsize = "8.0";
        writer.write(" label=<Application Model exported by Markov4JMeter<br align=\"left\"/><font point-size=\"" + labelsmallfontsize + "\">" +
                "image created on " + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(new Date()) + "<br align=\"left\"/></font>>; " +
                " fontcolor=\"#000000\"; fontname=\"Arial\"; labelfontname=\"Arial\"; fontsize=\"" + labelmainfontsize + "\"; labeljust=\"l\"" +
                " labelloc=\"t\"; remincross=\"true\";");

        /* Print state names */
        for (ApplicationState curState : stateList) {
            writer.write("" + curState.getId() + " " + "[label=\"" + curState.getName() + "\", peripheries=1, fontname=\"Arial\" ];\n");
        }

        /* Print transitions */
        for (ApplicationState curState : stateList) {
            for (ApplicationStateTransition curTrans : curState.getTransitions().getTransitionsAsList()) {
                if (curTrans.isDisabled()) {
                    continue;
                }
                String guard = curTrans.getGuard().length() > 0 ? "[" + curTrans.getGuard() + "]" : "";
                String action = curTrans.getAction().length() > 0 ? "/" + curTrans.getAction() : "";
                String label = guard + action;
                writer.write(curState.getId() + " -> " + curTrans.getDstStateId() + " [label = \"" + label + "\", fontname=\"Arial\"];\n");
            }
        }
        writer.write("}");
        writer.close();
    }

    /**
     * Export a visualization of the workload model (svg, pdf).
     *
     */
    private void exportModels() {
        String fnPrefix = this.pickBehaviorFile(
                new Markov4JMeterFileFilter("Dot file prefix",
                new String[]{".dot"}, true), // keep true for being able to open dirs
                false);
        if (fnPrefix == null) {
            /* no file selected (most likely cancel button) */
            return;
        }
        if (fnPrefix.endsWith(".dot")){
            fnPrefix=fnPrefix.substring(0,fnPrefix.lastIndexOf(".dot"));
        }

        try {
            this.exportBehaviorModels(fnPrefix);
            this.exportApplicationModel(fnPrefix + ".appModel.dot");
            GuiPackage.showInfoMessage("Exported application and behavior models to .dot-files with prefix\n '" + fnPrefix + "'.", "Export Models");
        } catch (Exception ex) {
            GuiPackage.showInfoMessage("Failed to generate template. See log for details.", "Export Models");
            ex.printStackTrace();
        }
    }

    /**
     * Generate a template file for user behavior.
     *
     * TODO: warn on duplicate state names!
     */
    protected void genTemplate() {
        String filename = this.pickBehaviorFile(new Markov4JMeterFileFilter("Markov4JMeter Behavior Model",
                new String[]{".csv"}, true), // keep true for being able to open dirs
                false);
        if (filename == null) {
            /* no file selected */
            return;
        }
        Iterator<String> it = this.stateNames.iterator();
        int numStates = this.stateNames.size();

        try {
            FileWriter writer = new FileWriter(new File(filename));

            /* Print header */
            writer.write(',');
            for (int i = 0; i < numStates; i++) {
                writer.write(this.stateNames.get(i));
                writer.write(',');
            }
            writer.write("$\n");

            /* Print entries */
            for (int i = 0; i < numStates; i++) {
                writer.write(this.stateNames.get(i) + ((i == 0) ? "*" : "") + ',');
                for (int j = 0; j < numStates; j++) {
                    writer.write("0.0");
                    writer.write(',');
                }
                writer.write("1\n");
            }
            writer.close();

            GuiPackage.showInfoMessage("Generated template file '" + filename + "'.", "Generate Template");
        } catch (IOException ex) {
            GuiPackage.showInfoMessage("Failed to generate template. See log for details.", "Generate Template");
            ex.printStackTrace();
        }


    }

    /**
     * Open file selection dialog and returns chosen filename.
     *
     * @param open if set TRUE, the dialog is labeled as an open dialog;
     *             if set FALSE, the dialog is labeled as a save dialog.
     * @return filename or null if no file was selected.
     */
    protected String pickBehaviorFile(JMeterFileFilter filter, boolean open) {
        JFileChooser chooser = new JFileChooser();
        String start = JMeterUtils.getPropDefault("user.dir", ""); // $NON-NLS-1$ // $NON-NLS-2$
        chooser.setCurrentDirectory(new File(start));
        chooser.setFileFilter(filter);
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setMultiSelectionEnabled(true);
        if (open) {
            chooser.showOpenDialog(GuiPackage.getInstance().getMainFrame());
        } else {
            chooser.showSaveDialog(GuiPackage.getInstance().getMainFrame());
        }
        File[] cfiles = chooser.getSelectedFiles();
        if (cfiles != null && cfiles.length != 0) {
            return cfiles[0].getPath();
        }
        return null;
    }

    /**
     * Add a new argument row to the table.
     */
    protected void addBehavior() {
        String filename;

        // If a table cell is being edited, we should accept the current value
        // and stop the editing before adding a new row.
        stopTableEditing();

        filename = pickBehaviorFile(new Markov4JMeterFileFilter("Markov4JMeter Behavior Model",
                new String[]{".csv"}, true), // keep true for being able to open dirs
                true);
        if (filename == null) // no file has been selected
        {
            return;
        }

        tableModel.addRow(makeNewBehavior(filename));

        // Enable DELETE (which may already be enabled, but it won't hurt)
        delete.setEnabled(true);

        // Highlight (select) the appropriate row.
        int rowToSelect = tableModel.getRowCount() - 1;
        table.setRowSelectionInterval(rowToSelect, rowToSelect);
    }

    /**
     * Create new behavior entry with given filename.
     *
     * @param filename the filename.
     */
    protected Object makeNewBehavior(String filename) {
        return new BehaviorMixEntry("", 0.0, filename); // $NON-NLS-1$ // $NON-NLS-2$
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
                new String[]{COLUMN_NAMES_0, COLUMN_NAMES_1, COLUMN_NAMES_2},
                new Functor[]{
            new Functor("getBName"), // $NON-NLS-1$
            new Functor("getRFreq"), // $NON-NLS-1$
            new Functor("getFilename")
        }, // $NON-NLS-1$

                new Functor[]{
            new Functor("setBName"), // $NON-NLS-1$
            new Functor("setRFreq"), // $NON-NLS-1$
            new Functor("setFilename")
        }, // $NON-NLS-1$
                new Class[]{String.class, Double.class, String.class});
    }

    /**
     * Resize the table columns to appropriate widths.
     *
     * @param table the table to resize columns for
     */
    protected void sizeColumns(JTable table) {
    }

    /**
     * Create a scroll panel that sets it's preferred size to it's minimum size.
     * Explicitly for scroll panes that live inside other scroll panes, or
     * within containers that stretch components to fill the area they exist in.
     * Use this for any component you would put in a scroll pane (such as
     * TextAreas, tables, JLists, etc). It is here for convenience and to avoid
     * duplicate code. JMeter displays best if you follow this custom.
     *
     * @param comp
     *            the component which should be placed inside the scroll pane
     * @return a JScrollPane containing the specified component
     */
    protected JScrollPane makeScrollPane(Component comp) {
        JScrollPane pane = new JScrollPane(comp);
        pane.setPreferredSize(pane.getMinimumSize());
        return pane;
    }

    /**
     * Create the main GUI panel which contains the argument table.
     *
     * @return the main GUI panel
     */
    private Component makeMainPanel() {
        initializeTableModel();
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
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
     * Create a panel containing the add and delete buttons.
     *
     * @return a GUI panel containing the buttons
     */
    private JPanel makeButtonPanel() {
        add = new JButton(JMeterUtils.getResString("add")); // $NON-NLS-1$
        add.setActionCommand(ADD);
        add.setEnabled(true);

        delete = new JButton(JMeterUtils.getResString("delete")); // $NON-NLS-1$
        delete.setActionCommand(DELETE);

        template = new JButton("Generate Template");
        template.setActionCommand(GEN_TEMPLATE);
        template.setEnabled(true);

        exportAppModel = new JButton("Export Models (.dot)");
        exportAppModel.setActionCommand(EXPORT_APPMODEL);
        exportAppModel.setEnabled(true);

        checkDeleteStatus();

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        if (this.background != null) {
            buttonPanel.setBackground(this.background);
        }
        add.addActionListener(this);
        delete.addActionListener(this);
        template.addActionListener(this);
        exportAppModel.addActionListener(this);
        buttonPanel.add(add);
        buttonPanel.add(delete);
        buttonPanel.add(template);
        buttonPanel.add(exportAppModel);
        return buttonPanel;
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
        p.add(makeButtonPanel(), BorderLayout.SOUTH);

        table.revalidate();
        sizeColumns(table);
    }
}

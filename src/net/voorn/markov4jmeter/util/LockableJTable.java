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

package net.voorn.markov4jmeter.util;

import java.util.HashMap;
import java.util.Map;
import javax.swing.JTable;
import javax.swing.table.TableModel;

/**
 * Extends a JTable by the ability to locking cells, i.e. declaring them to be
 * read-only.
 *
 * @author Andr&eacute; van Hoorn
 */
public class LockableJTable extends JTable {
    Map<Integer,Object> lockedCols = new HashMap();
    
    /**
     * Creates a new instance of LockableJTable
     */
    public LockableJTable() {
        super();
    }
    
    public LockableJTable(TableModel m){
        super(m);
    }

    /** @see JTable#isCellEditable */
    @Override
    public boolean isCellEditable(int row, int col) {
        return !lockedCols.containsKey(col);
    }

    /**
     * Sets the edit state of the given col to the given state.
     * 
     * @param col the column number.
     * @param editable iff true the column is set to be editable.
     */
    public void setColIsEditable(int col, boolean editable){
        if(!editable) 
            lockedCols.remove(col);
        else
            lockedCols.put(col, null);
    }
}

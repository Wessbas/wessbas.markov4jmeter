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

import java.util.Arrays;

/**
 * Extends by JMeterFileFilter by setting a filter label on initialization time.
 * This name is then including within the filter's description.
 *
 * @author Andr&eacute; van Hoorn
 */
public class Markov4JMeterFileFilter extends org.apache.jmeter.gui.JMeterFileFilter {
    /**
     * This variable is declared private in super class so we store our own
     * reference. We need this variable within the description text.
     */
    private final String[] exts;
    
    /** The label. */
    private String name = "";
    
    /**
     * Create a new JMeter file filter which allows the specified extensions. If
     * the array of extensions contains no elements, any file will be allowed.
     *
     * This constructor will also return all directories
     *
     * @param extensions
     *            non-null array of allowed file extensions
     */
    public Markov4JMeterFileFilter(String name, String[] extensions) {
        this(name, extensions, true);
    }
    
    /**
     * Create a new JMeter file filter which allows the specified extensions. If
     * the array of extensions contains no elements, any file will be allowed.
     *
     * @param extensions non-null array of allowed file extensions
     * @param allow should directories be returned ?
     */
    public Markov4JMeterFileFilter(String name, String[] extensions, boolean allow) {
        super(extensions, allow);
        this.exts = extensions;
        this.name = new String(name);
    }
    
    /**
     * Get a description for this filter.
     *
     * @return a description for this filter
     */
    @Override
    public String getDescription() {
        return this.name + " " + Arrays.asList(exts).toString();
    }
}

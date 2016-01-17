/***************************************************************************
 * Copyright 2012 by
 *  Christian-Albrechts-University of Kiel, 24098 Kiel, Germany
 *    + Department of Computer Science
 *     + Software Engineering Group
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

package net.voorn.markov4jmeter.control.parser;

/**
 * Exception class for any errors which might occur during the parsing process.
 *
 * @author Eike Schulz (esc@informatik.uni-kiel.de)
 *
 * @version 1.0 (2012-12-15)
 */
public class ParseException extends Exception {

    /**
     * Default serial version ID.
     */
    private static final long serialVersionUID = 1L;


    /**
     * Constructor for a <code>ParseException</code> with a specific message.
     *
     * @param message  further information about the error that occurred.
     */
    public ParseException (final String message) {

        super(message);
    }
}

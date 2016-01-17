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

import java.util.List;

/**
 * This class represents a session with a specific ID and a list of use cases.
 *
 * @author Eike Schulz (esc@informatik.uni-kiel.de)
 *
 * @version 1.0 (2012-12-15)
 */
public class SessionData {

    /** ID of the associated session trace. */
    private final String id;

    /** Use cases of the session. */
    private final List<UseCase> useCases;
    
    private String transactionType;    


    public String getTransactionType() {
		return transactionType;
	}


	public void setTransactionType(String transactionType) {
		this.transactionType = transactionType;
	}


	/**
     * Constructor for a <code>SessionData</code> instance with a specific ID
     * and a list of use cases.
     *
     * @param id        ID of the associated session trace.
     * @param useCases  use cases of the session.
     */
    public SessionData (final String id, final List<UseCase> useCases) {

        this.id       = id;
        this.useCases = useCases;
    }


    /**
     * Returns the ID of the associated session trace.
     *
     * @return ID of the associated session trace.
     */
    public String getId () {

        return this.id;
    }

    /**
     * Returns the use cases of the session.
     *
     * @return use cases of the session.
     */
    public List<UseCase> getUseCases () {

        return this.useCases;
    }

    @Override
    public String toString () {

        final StringBuffer stringBuffer = new StringBuffer();

        for (final UseCase useCase : this.useCases) {

            stringBuffer.append( useCase.toString() ).append(" ");
        }
        return "ID: " + id + " / USE CASES: " + stringBuffer.toString();
    }
}
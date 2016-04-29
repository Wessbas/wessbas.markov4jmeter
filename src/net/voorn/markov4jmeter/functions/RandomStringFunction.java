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

package net.voorn.markov4jmeter.functions;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.jmeter.engine.util.CompoundVariable;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.functions.AbstractFunction;
import org.apache.jmeter.functions.InvalidVariableException;

/**
 * Provides a function which returns a random element from a list of Strings.
 * 
 */
public class RandomStringFunction extends AbstractFunction implements Serializable {

	/**
	 * DefaultId;
	 */
	private static final long serialVersionUID = 1L;

	private static final List<String> desc = new LinkedList<String>();

	private static final String KEY = "__GetRandomString"; //$NON-NLS-1$

	static {
		desc.add("String list"); //$NON-NLS-1$
        desc.add("Delimeter"); //$NON-NLS-1$
	}

	private String[] tokens;
        private static java.util.Random rand = new java.util.Random();
        
	/**
	 * No-arg constructor.
	 */
	public RandomStringFunction() {
	}

	/**
	 *
	 */
    @Override
	public Object clone() {
		return new RandomStringFunction();
	}

	/**
	 * Execute the function.
	 */
	public synchronized String execute(SampleResult previousResult, Sampler currentSampler)
			throws InvalidVariableException {
            int rnd = rand.nextInt(this.tokens.length);
            return this.tokens[rnd];
	}

	/**
	 * Set the parameters for the function.
	 */
	public synchronized void setParameters(Collection<CompoundVariable> parameters) throws InvalidVariableException {
		Object[] values = parameters.toArray();

		if (values.length != 2) {
			throw new InvalidVariableException("Expected 2 got " 
                                + values.length);
		}
		
        String str = ((CompoundVariable) values[0]).execute();
        String delim = ((CompoundVariable) values[1]).execute();
        
        StringTokenizer tokenizer = new StringTokenizer(str, delim);
        this.tokens = new String[tokenizer.countTokens()];
        
        for (int i=0; tokenizer.hasMoreElements(); i++){
            tokens[i] = tokenizer.nextToken().trim();
        }
	}

	/**
	 * Get the invocation key for this function.
	 */
	public String getReferenceKey() {
		return KEY;
	}

	/**
	 * Get the description of this function.
	 */
	public List<String> getArgumentDesc() {
		return desc;
	}

}


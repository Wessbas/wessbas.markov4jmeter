package net.voorn.markov4jmeter.functions;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.jmeter.engine.util.CompoundVariable;
import org.apache.jmeter.functions.AbstractFunction;
import org.apache.jmeter.functions.InvalidVariableException;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.threads.JMeterVariables;

public class RandomStringRemoveFunction  extends AbstractFunction implements Serializable{

	/**
	 * DefaultId;
	 */
	private static final long serialVersionUID = 1L;

	private static final List<String> desc = new LinkedList<String>();

	private static final String KEY = "__GetRandomStringRemove"; //$NON-NLS-1$

	static {
		desc.add("Parameter Name"); //$NON-NLS-1$
        desc.add("Delimiter"); //$NON-NLS-1$
	}

	private String[] tokens;
    private static java.util.Random rand = new java.util.Random();
    private String delimiter;
    private String parameterName;
    private Object[] values;
        
	/**
	 * No-arg constructor.
	 */
	public RandomStringRemoveFunction() {
	}

	
	@Override
	public List<String> getArgumentDesc() {
		return desc;
	}

	@Override
	public String execute(SampleResult arg0, Sampler currentSampler)
			throws InvalidVariableException {
		
	    this.parameterName = ((CompoundVariable) values[0]).execute();
        this.delimiter = ((CompoundVariable) values[1]).execute();        
    
        JMeterVariables jMeterVariables = currentSampler.getThreadContext().getVariables();	
        String parameterValue = jMeterVariables.get(this.parameterName);
        
        StringTokenizer tokenizer = new StringTokenizer(parameterValue, this.delimiter);
        this.tokens = new String[tokenizer.countTokens()];
        for (int i=0; tokenizer.hasMoreElements(); i++){
            tokens[i] = tokenizer.nextToken().trim();
        }			
		
    	String returnString = "";		
		if (this.tokens.length > 0) {						
			// getReturnString
			int rnd = rand.nextInt(this.tokens.length);
			returnString = this.tokens[rnd];
			
			// create new String without returnString
			String newString = "";	
			for (int i = 0; i < this.tokens.length; i++) {
				if (i != rnd) {
					newString += this.tokens[i] + this.delimiter;
				}
			}	
			// set newString 			    	    
		    jMeterVariables.put(this.parameterName, newString);     
		}			
		
        return returnString;
	}

	@Override
	public String getReferenceKey() {
		return KEY;
	}

	@Override
	public void setParameters(Collection<CompoundVariable> parameters)
			throws InvalidVariableException {
		this.values = parameters.toArray();

		if (values.length != 2) {
			throw new InvalidVariableException("Expected 2 got " 
                                + values.length);
		}
		        
	}

}

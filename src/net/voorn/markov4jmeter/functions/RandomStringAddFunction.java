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

public class RandomStringAddFunction extends AbstractFunction implements Serializable {
	
	/**
	 * default serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;

	private static final List<String> desc = new LinkedList<String>();
	
	private static final String KEY = "__GetRandomStringAdd"; //$NON-NLS-1$

	static {
		desc.add("String list"); //$NON-NLS-1$
        desc.add("Name of variable in which the value of returned string is stored"); //$NON-NLS-1$
        desc.add("Delimiter"); //$NON-NLS-1$
	}
	
	private String[] tokens;
	private String parameterName;
	private String delimiter;
    private static java.util.Random rand = new java.util.Random();

	
	/**
	 * No-arg constructor.
	 */
	public RandomStringAddFunction() {
	}
	
	@Override
	public List<String> getArgumentDesc() {
		return desc;
	}

	@Override
	public String execute(SampleResult previousResult, Sampler currentSampler)
			throws InvalidVariableException {
		    int rnd = rand.nextInt(this.tokens.length);
		    String returnString = "";
		    if (this.tokens.length > 0) {
		    	returnString = this.tokens[rnd];
		    }		    
		    JMeterVariables jMeterVariables = currentSampler.getThreadContext().getVariables();		    
		    String newParameterValue = jMeterVariables.get(this.parameterName);		
		    newParameterValue += returnString + this.delimiter;		    
		    jMeterVariables.put(this.parameterName, newParameterValue);          
            return returnString;
	}

	@Override
	public String getReferenceKey() {
		return KEY;
	}

	@Override
	public void setParameters(Collection<CompoundVariable> parameters)
			throws InvalidVariableException {
		Object[] values = parameters.toArray();

		if (values.length != 3) {
			throw new InvalidVariableException("Expected 3 got " 
                                + values.length);
		}
		
        String str = ((CompoundVariable) values[0]).execute();
        this.parameterName = ((CompoundVariable) values[1]).execute();
        this.delimiter = ((CompoundVariable) values[2]).execute();               
        
        StringTokenizer tokenizer = new StringTokenizer(str, this.delimiter);
        this.tokens = new String[tokenizer.countTokens()];
        for (int i=0; tokenizer.hasMoreElements(); i++){
            tokens[i] = tokenizer.nextToken().trim();
        }
		
	}

}

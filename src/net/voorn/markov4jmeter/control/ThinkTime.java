package net.voorn.markov4jmeter.control;

/**
 * This is the <code>abstract</code> base class of all think time types,
 * regarding to a certain distribution type.
 *
 * @author Eike Schulz (esc@informatik.uni-kiel.de)
 * @version 1.0 (2014-01-31)
 */
public abstract class ThinkTime {

    /**
     * Returns a <code>String</code> which indicates the think time
     * distribution type. The descriptor is used as "function name" in CSV-file
     * tokens for specifying the related distribution of think times.
     * @see ThinkTimeParser
     *
     * @return  A valid <code>String</code> instance.
     */
    public abstract String getFunctionDescriptor ();

    /** A <code>String</code> representation of this instance, at least
     *  required for testing purposes. */
    @Override
    public abstract String toString ();

    /** Returns the delay in milliseconds. */
    public abstract long getDelay ();
}
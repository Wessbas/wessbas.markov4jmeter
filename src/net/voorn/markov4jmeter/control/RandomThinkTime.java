package net.voorn.markov4jmeter.control;

import java.util.Random;

/**
 * This class represents a normally distributed think time.
 *
 * @author Eike Schulz (esc@informatik.uni-kiel.de)
 */
public class RandomThinkTime extends ThinkTime {

    /** Function descriptor which indicates the distribution function type.
     *  This must be <code>protected</code> for the {@link ThinkTimeParser},
     *  which needs access to it for identifying think time types. */
    protected final static String FUNCTION_DESCRIPTOR = "rand";

    /** Mean value which is greater or equal 0. */
    private final double mean;

    /** Random number generator. */
    private final Random random = new Random();

    /**
     * Factor to be multiplied with the deviation;
     * in general, the following rules hold:
     * <ul>
     *   <li> factor = 1.0 --> 68.27% of all values will be in deviation range;
     *   <li> factor = 0.5 --> 95.45% of all values will be in deviation range;
     *   <li> factor = 0.3 --> 99.73% of all values will be in deviation range.
     * </ul>
     * Note that any negative think time values will be set to 0, which might
     * influence the given percentage values.
     */
    private final static double DEVIATION_FACTOR = 1.0d;


    /**
     * Constructor for a normally distributed think time with mean and deviation
     * values.
     *
     * @param mean
     *     A <code>double</code> value which is greater or equal 0.
     * @param deviation
     *     A <code>double</code> value which might be even negative.
     */
    public RandomThinkTime (
            final double mean) {

        this.mean = mean;
    }


    /**
     * Returns the mean value.
     *
     * @return A <code>double</code> value which is greater or equal 0.
     */
    public double getMean() {
        return this.mean;
    }

    @Override
    public String getFunctionDescriptor () {
        return RandomThinkTime.FUNCTION_DESCRIPTOR;
    }

    @Override
    public String toString () {
        return RandomThinkTime.FUNCTION_DESCRIPTOR +
                "(mean: " + this.mean + ")";
    }

    @Override
    public long getDelay() {
        // nextGaussian() returns a value between 0 and 1;
        final double value = Math.random() * (this.mean * 2);

        final long delay = (value < 0) ? 0L : (long) Math.round(value);
        /*
        final long id = Thread.currentThread().getId();
        System.out.println("ID: " + id + ", m: " + mean + ", d: " + deviation + ", tt: " + delay);
        */
        return delay;
    }
}
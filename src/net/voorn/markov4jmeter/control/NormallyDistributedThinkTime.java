package net.voorn.markov4jmeter.control;

import java.util.Random;

/**
 * This class represents a normally distributed think time.
 *
 * @author Eike Schulz (esc@informatik.uni-kiel.de)
 */
public class NormallyDistributedThinkTime extends ThinkTime {

    /** Function descriptor which indicates the distribution function type.
     *  This must be <code>protected</code> for the {@link ThinkTimeParser},
     *  which needs access to it for identifying think time types. */
    protected final static String FUNCTION_DESCRIPTOR = "norm";

    /** Mean value which is greater or equal 0. */
    private final double mean;

    /** Deviation value which might be even negative. */
    private final double deviation;

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
    public NormallyDistributedThinkTime (
            final double mean,
            final double deviation) {

        this.mean = mean;
        this.deviation = deviation;
    }


    /**
     * Returns the mean value.
     *
     * @return A <code>double</code> value which is greater or equal 0.
     */
    public double getMean() {

        return this.mean;
    }

    /**
     * Returns the deviation value.
     *
     * @return A <code>double</code> value which might be even negative.
     */
    public double getDeviation() {

        return this.deviation;
    }

    @Override
    public String getFunctionDescriptor () {

        return NormallyDistributedThinkTime.FUNCTION_DESCRIPTOR;
    }

    @Override
    public String toString () {

        return NormallyDistributedThinkTime.FUNCTION_DESCRIPTOR +
                "(mean: " + this.mean + ", deviation: " + this.deviation + ")";
    }

    @Override
    public long getDelay() {

        // nextGaussian() returns a value between 0 and 1;
        final double value =
                this.random.nextGaussian() *
                this.deviation * NormallyDistributedThinkTime.DEVIATION_FACTOR +
                this.mean;

        final long delay = (value < 0) ? 0L : (long) Math.round(value);
        /*
        final long id = Thread.currentThread().getId();
        System.out.println("ID: " + id + ", m: " + mean + ", d: " + deviation + ", tt: " + delay);
        */
        return delay;
    }
}
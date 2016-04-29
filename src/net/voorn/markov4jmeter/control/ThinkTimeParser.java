package net.voorn.markov4jmeter.control;

/**
 * Class for parsing a token of a CSV-file which denotes a user think time.
 * A token has to be formatted as
 * <blockquote>
 *   <i>&#060;functionDescriptor&#062;</i>(<i>&#060;parameter1&#062; &#060;parameter2&#062; ... </i> )
 * </blockquote>
 * whereas <i>&#060;functionDescriptor&#062;</i> indicates the used distribution
 * function. The number of parameters depends on that distribution.
 *
 * @author Eike Schulz (esc@informatik.uni-kiel.de)
 *
 * @version 1.0 (2014-01-31)
 */
public class ThinkTimeParser {

    /** Regular expression specifying the separator for parameters. */
    private final static String PARAMETER_SEPARATOR = "\\s+";

    // TODO: static methods should be made non-static, and an instance of this class needs to be assigned to BehaviorMixEntry, to comply with the thesis documentation;

    /**
     * Parses a given <code>String</code> which specifies a think time according
     * to a certain distribution.
     *
     * @param str
     *     <code>String</code> which provides a distribution function descriptor
     *     and the required parameters as well. The leading function descriptor
     *     indicates the distribution type and therewith the type of
     *     {@link ThinkTime} to be returned. The passed <code>String</code>
     *     might be wrapped into whitespace, which will be removed before
     *     parsing starts.
     *
     * @return
     *     A valid instance of a {@link ThinkTime} sub-class, or
     *     <code>null</code> if parsing fails for any reason.
     */
    public synchronized static ThinkTime parse (String str) {

        if (str != null) {

            str = str.trim();

            // ensure that at least one leading character exists prior the
            // first opening bracket; closing bracket must be at last position;
            if ( str.indexOf('(') > 0 &&
                 str.lastIndexOf(')') == str.length() - 1) {

                final String functionDescriptor =
                        ThinkTimeParser.extractFunctionDescriptor(str);

                final String[] parameters =
                        ThinkTimeParser.extractParameters(str);

                return ThinkTimeParser.createThinkTime(
                        functionDescriptor,
                        parameters);
            }
        }

        return null;  // invalid function String;
    }

    /**
     * Extracts the function descriptor which indicates the distribution type.
     *
     * @param str
     *     <code>String</code> which specifies the distribution type as well
     *     as required parameters; it must contain at least an opening bracket.
     *
     * @return
     *     A valid <code>String</code> instance, or <code>null</code> if no
     *     function descriptor can be found.
     */
    private static String extractFunctionDescriptor (final String str) {

        return str.split("\\(")[0].trim();
    }

    /**
     * Extracts the parameters which are required by the regarding distribution
     * type. The parameters will be returned as (unparsed) <code>String</code>s.
     *
     * @param function
     *     <code>String</code> which specifies the distribution type as well
     *     as required parameters.
     *
     * @return
     *     A valid array of <code>String</code> instances; each entry denotes
     *     an unparsed parameter.
     */
    private static String[] extractParameters (final String function) {

        final String str = function.substring(
                function.indexOf('(') + 1,
                function.length() - 1).trim();

        return str.split(ThinkTimeParser.PARAMETER_SEPARATOR);
    }

    /**
     * Creates an instance of a {@link ThinkTime} sub-class, depending on the
     * given function descriptor.
     *
     * @param functionDescriptor
     *     A <code>String</code> which indicates the think time distribution
     *     type.
     * @param parameters
     *     The (unparsed) function parameters required by the think time
     *     distribution type.
     *
     * @return
     *     A valid instance of a {@link ThinkTime} sub-class; if the given
     *     function descriptor is unknown or parameters are invalid,
     *     <code>null</code> will be returned.
     */
    private static ThinkTime createThinkTime (
            final String functionDescriptor,
            final String[] parameters) {

        if (NormallyDistributedThinkTime.FUNCTION_DESCRIPTOR.
                equalsIgnoreCase(functionDescriptor)) {

            return ThinkTimeParser.createNormalDistributionThinkTime(
                    parameters);
        }

        // more cases might be added for further distribution types;

        return null;  // null ~ unknown function descriptor;
    }

    /**
     * Creates a {@link NormallyDistributedThinkTime} instance, which will be
     * initialized with the parsed values of the given parameters.
     *
     * @param parameters
     *     The function parameters required by the think time distribution type.
     *
     * @return
     *     A valid instance of {@link NormallyDistributedThinkTime}, or
     *     <code>null</code> if any parameter parsing fails.
     */
    private static NormallyDistributedThinkTime
    createNormalDistributionThinkTime (final String[] parameters) {

        NormallyDistributedThinkTime thinkTime = null;

        if (parameters.length == 2) {

            try {

                final double mean      = Double.parseDouble(parameters[0]);
                final double deviation = Double.parseDouble(parameters[1]);

                thinkTime = new NormallyDistributedThinkTime(mean, deviation);

            } catch (final NumberFormatException ex) {

                // keep thinkTime being null for indicating an error;
            }
        }

        return thinkTime;
    }

    /**
     * This has just been added for testing purposes (could be implemented as
     * JUnit test).
     *
     * @param argv  Argument vector; any arguments will be ignored here.
     */
    public static void main (final String[] argv) {

        System.out.println("valid formats:");
        System.out.println( ThinkTimeParser.parse("norm(86 40.5)") );
        System.out.println( ThinkTimeParser.parse("norm(86 40.5f)") );
        System.out.println( ThinkTimeParser.parse("norm(86 40.5d)") );
        System.out.println( ThinkTimeParser.parse("norm(86.5 40)") );
        System.out.println( ThinkTimeParser.parse("norm(86.5f 40)") );
        System.out.println( ThinkTimeParser.parse("norm(86.5d 40)") );
        System.out.println( ThinkTimeParser.parse("norm( 86 40 )") );
        System.out.println( ThinkTimeParser.parse(" norm(86 40.5) ") );
        System.out.println( ThinkTimeParser.parse("norm (86 40.5)") );

        System.out.println("invalid formats:");
        System.out.println( ThinkTimeParser.parse("norm(45 24") );
        System.out.println( ThinkTimeParser.parse("norm 45 24)") );
        System.out.println( ThinkTimeParser.parse("norm(45)") );
        System.out.println( ThinkTimeParser.parse("nor(45 24)") );
        System.out.println( ThinkTimeParser.parse("norm(45x 24)") );
        System.out.println( ThinkTimeParser.parse("norm(45 24x)") );
        System.out.println( ThinkTimeParser.parse(null) );
    }
}
/**
 * 
 */
package net.voorn.markov4jmeter.control;

import java.util.HashMap;

import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

/**
 * @author voegele
 * 
 */
public class ConditionEvaluationStatistics {

	/** Logger for this class */
	private static final Logger logger = LoggingManager.getLoggerForClass();

	// singleton
	private static ConditionEvaluationStatistics instance = null;

	public static ConditionEvaluationStatistics getInstance() {
		if (instance == null) {
			instance = new ConditionEvaluationStatistics();
		}
		return instance;
	}

	private HashMap<String, ConditionEntry> conditionResults = new HashMap<String, ConditionEntry>();

	private int countEvaluations = 0;

	public void addConditionEntry(final String from, final String to,
			final int cntEvaluationTrue) {
		String key = from + " " + to;

		if (conditionResults.get(key) == null) {
			ConditionEntry conditionEntry = new ConditionEntry();
			conditionEntry.setCntEvaluations(1);
			conditionEntry.setCntEvaluationTrue(cntEvaluationTrue);
			conditionResults.put(key, conditionEntry);
		} else {
			ConditionEntry conditionEntry = conditionResults.get(key);
			conditionEntry
					.setCntEvaluations(conditionEntry.getCntEvaluations() + 1);
			conditionEntry.setCntEvaluationTrue(conditionEntry
					.getCntEvaluationTrue() + cntEvaluationTrue);
		}

		if ((countEvaluations % 10000) == 0) {
			printResults();
		}

		countEvaluations++;

	}

	public void printResults() {
		logger.info("---------------");
		for (String key : conditionResults.keySet()) {
			ConditionEntry conditionEntry = conditionResults.get(key);
			logger.info(key + " "
					+ (double) conditionEntry.getCntEvaluationTrue()
					/ (double) conditionEntry.getCntEvaluations());
		}
		logger.info("---------------");
	}

	class ConditionEntry {

		private int cntEvaluations;
		private int cntEvaluationTrue;

		/**
		 * @return the cntEvaluations
		 */
		public final int getCntEvaluations() {
			return cntEvaluations;
		}

		/**
		 * @param cntEvaluations
		 *            the cntEvaluations to set
		 */
		public final void setCntEvaluations(int cntEvaluations) {
			this.cntEvaluations = cntEvaluations;
		}

		/**
		 * @return the cntEvaluationTrue
		 */
		public final int getCntEvaluationTrue() {
			return cntEvaluationTrue;
		}

		/**
		 * @param cntEvaluationTrue
		 *            the cntEvaluationTrue to set
		 */
		public final void setCntEvaluationTrue(int cntEvaluationTrue) {
			this.cntEvaluationTrue = cntEvaluationTrue;
		}

	}

}

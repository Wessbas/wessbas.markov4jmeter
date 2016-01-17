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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Parser for session data files which consist of lines like
 * 
 * <pre>
 * 925668695;"Login":1324283446707872892:1324283446707872892;"Aktenbearbeitung":1324283449513979584:1324284003591760857
 * 1968604756;"Login":1326109113756992122:1326109113756992122;"Aktenbearbeitung":1326109116627369510:1326121328888329705
 * 660493198;"Login":1326354223006079065:1326354223006079065;"Aktenbearbeitung":1326354226173227339:1326371530498646754
 * ...
 * </pre>
 * 
 * whereas each line contains a session-ID as integer, followed by a sequence of
 * use cases (with names in quotes), including their start time and ending time
 * as <code>long</code> values.
 * 
 * <p>
 * This class is implemented as singleton pattern; consequently, there is one
 * unique instance which can be requested by invoking method
 * {@link Parser#getInstance()}.
 * 
 * @author Eike Schulz (esc@informatik.uni-kiel.de)
 * 
 * @version 1.0 (2012-12-15)
 */
public class Parser {

	/** Symbol for separating use cases of a session from each other. */
	private final static String TOKEN_SEPARATOR = ";";

	/** Symbol for separating use cases from each other. */
	private final static String USECASE_TOKEN_SEPARATOR = ":";

	/** (Optional) prefix of a session file line. */
	private final static String SESSION_PREFIX = ""; // for example "#";

	/** Error message for the case that any session prefix is unavailable. */
	private final static String ERROR_NO_SESSION_PREFIX = "line %s has no session prefix ("
			+ SESSION_PREFIX + "): \"%s\"";

	/** Error message for the case that any session ID is invalid. */
	private final static String ERROR_NO_SESSION_ID = "line %s has an invalid session-ID: \"%s\"";

	/**
	 * Error message for the case that insufficient use case information is
	 * available.
	 */
	private final static String ERROR_INSUFFICIENT_USE_CASE_INFORMATION = "line %s has insufficient use case informations: \"%s\"";

	/** Error message for the case that any line is formatted improperly. */
	private final static String ERROR_ILLEGAL_FORMAT = "line %s has illegal format: \"%s\"";

	/**
	 * <code>true</code> if and only if quotes (<code>"..."<code>) shall be
	 *  removed from use case names.
	 */
	private final static boolean REMOVES_QUOTES_FROM_USE_CASE_NAMES = true;

	/**
	 * SingletonHolder for singleton pattern; loaded on the first execution of
	 * {@link Parser#getInstance()}.
	 */
	private static class SingletonHolder {

		public static Parser instance = new Parser();
	}

	/**
	 * Constructor, makes the standard constructor <code>private</code>.
	 */
	private Parser() {
	}

	/**
	 * Returns a unique instance of this class.
	 * 
	 * @return an instance of {@link Parser}.
	 */
	public static Parser getInstance() {

		return Parser.SingletonHolder.instance;
	}

	/**
	 * Returns an iterator for streamwise parsing of the specified input file.
	 * 
	 * @param filePath
	 *            path to the file that shall be parsed.
	 * 
	 * @return an iterator for streamwise parsing of the specified input file.
	 * 
	 * @throws FileNotFoundException
	 *             if the file does not exist, is a directory rather than a
	 *             regular file, or for some other reason cannot be opened for
	 *             reading.
	 * @throws SecurityException
	 *             if a security manager exists and its checkRead method denies
	 *             read access to the file.
	 */
	public Iterator getIterator(final String filePath)
			throws FileNotFoundException {

		// might throw a FileNotFound- or SecurityException;
		final BufferedReader bufferedReader = this.getBufferedReader(filePath);

		return new Iterator(bufferedReader);
	}

	/**
	 * Parses a given file for session information.
	 * 
	 * @param filePath
	 *            path to the file that shall be parsed.
	 * 
	 * @return an array with information data for each session which has been
	 *         parsed from file.
	 * 
	 * @throws ParseException
	 *             in case any syntactical error is detected while parsing.
	 * @throws IOException
	 *             if an I/O error occurs, or (as
	 *             <code>FileNotFoundException</code>) if the file does not
	 *             exist, is a directory rather than a regular file, or for some
	 *             other reason cannot be opened for reading.
	 * @throws SecurityException
	 *             if a security manager exists and its checkRead method denies
	 *             read access to the file.
	 */
	public SessionData[] parseFile(final String filePath,
			final int thresHoldSessionTime) throws ParseException, IOException,
			SecurityException {

		final ArrayList<SessionData> sessions = new ArrayList<SessionData>();

		// might throw a FileNotFound- or SecurityException;
		final BufferedReader bufferedReader = this.getBufferedReader(filePath);

		String line; // temporary variable;

		try {
			// readLine() might throw an IOException;
			for (int lineNumber = 1; (line = bufferedReader.readLine()) != null; lineNumber++) {

				// might throw a ParseException;
				final List<SessionData> sessionDataList = this.parseLine(line,
						lineNumber, thresHoldSessionTime);

				for (SessionData sessionData : sessionDataList) {
					if (sessionData.getUseCases().size() > 0) {
						sessions.add(sessionData);
					}
				}

			}
		} finally {

			bufferedReader.close(); // might throw an IOException;
		}
		return sessions.toArray(new SessionData[] {});
	}

	/**
	 * Creates a <code>BufferedReader</code> instance for a given file.
	 * 
	 * @param filePath
	 *            path to the file for which a <code>BufferedReader</code>
	 *            instance shall be created.
	 * 
	 * @return a <code>BufferedReader</code> instance for the given file.
	 * 
	 * @throws FileNotFoundException
	 *             if the file does not exist, is a directory rather than a
	 *             regular file, or for some other reason cannot be opened for
	 *             reading.
	 * @throws SecurityException
	 *             if a security manager exists and its checkRead method denies
	 *             read access to the file.
	 */
	private BufferedReader getBufferedReader(final String filePath)
			throws FileNotFoundException, SecurityException {

		// FileInputStream constructor might throw a FileNotFound- or
		// SecurityException;
		final FileInputStream fis = new FileInputStream(filePath);
		final InputStreamReader isr = new InputStreamReader(fis);
		final BufferedReader bufferedReader = new BufferedReader(isr);

		return bufferedReader;
	}

	/**
	 * Parses a single line from input source for session information.
	 * 
	 * @param line
	 *            line to be parsed.
	 * @param lineNumber
	 *            number of the line within the source text.
	 * 
	 * @return the session information of the parsed line.
	 * 
	 * @throws ParseException
	 *             in case any syntactical error is detected while parsing.
	 */
	private LinkedList<SessionData> parseLine(final String line,
			final int lineNumber, final int thresHoldSession)
			throws ParseException {

		final LinkedList<SessionData> sessionData = new LinkedList<SessionData>();
		// to be returned;

		if (line.trim().startsWith(Parser.SESSION_PREFIX)) {

			// remove prefix from line;
			final int prefixLength = Parser.SESSION_PREFIX.length();
			final String subLine = line.trim().substring(prefixLength);

			// might throw PatternSyntaxException (should never happen here);
			final String[] tokens = subLine.split(Parser.TOKEN_SEPARATOR);

			if (tokens.length > 0) {

				String id = tokens[0];

				if (id == null) {

					final String message = String.format(
							Parser.ERROR_NO_SESSION_ID, lineNumber, line);

					throw new ParseException(message);
				}

				LinkedList<UseCase> useCases = new LinkedList<UseCase>();
				int newIdentifier = 0;

				for (int i = 1, n = tokens.length; i < n; i++) {

					final String token = tokens[i];

					// use case is null, if informations are insufficient;
					final UseCase useCase = parseUseCase(token);

					if (useCase != null) {

						// TODO: if time between to user actions is above a
						// threshold
						// create new user session.
						if (useCases.size() > 0
								&& thresHoldSession > 0
								&& (useCase.getStartTime()
										- useCases.getLast().getEndTime() > thresHoldSession)) {

							// create new session with old useCases
							sessionData.add(new SessionData(id, useCases));

							// add useCase to new session
							id += "_" + Integer.toString(newIdentifier);
							newIdentifier++;
							useCases = new LinkedList<UseCase>();
							useCases.add(useCase);

						} else {

							useCases.add(useCase);

						}

					} else {

						final String message = String.format(
								Parser.ERROR_INSUFFICIENT_USE_CASE_INFORMATION,
								lineNumber, line);

						throw new ParseException(message);
					}
				}

				sessionData.add(new SessionData(id, useCases));

			} else {
				final String message = String.format(
						Parser.ERROR_ILLEGAL_FORMAT, lineNumber, line);

				throw new ParseException(message);
			}
		} else { // no session prefix (#) in line;

			final String message = String.format(
					Parser.ERROR_NO_SESSION_PREFIX, lineNumber, line);

			throw new ParseException(message);
		}
		return sessionData;
	}

	/**
	 * Parses the given string for a use case.
	 * 
	 * @param str
	 *            the <code>String</code> to be parsed.
	 * 
	 * @return the result data of the parsed use case; if the
	 *         <code>String</code> contains insufficient use case informations,
	 *         <code>null</code> will be returned.
	 */
	private UseCase parseUseCase(final String str) {

		final UseCase useCase; // to be returned;

		// might throw PatternSyntaxException (should never happen here);
		final String[] useCaseTokens = str
				.split(Parser.USECASE_TOKEN_SEPARATOR);

		String name = useCaseTokens[0].trim();
		final long startTime = this.parseTime(useCaseTokens[1]);
		final long endTime = this.parseTime(useCaseTokens[2]);

		if (useCaseTokens.length == 3) {
			if (Parser.REMOVES_QUOTES_FROM_USE_CASE_NAMES) {
				final String plainName = this.removeQuotes(name);
				useCase = new UseCase(plainName, startTime, endTime);
			} else {
				useCase = new UseCase(name, startTime, endTime);
			}
		} else if (useCaseTokens.length == 10) {
			final String uri = useCaseTokens[3].trim();
			final int port = Integer.parseInt(useCaseTokens[4]);
			final String ip = useCaseTokens[5].trim();
			final String protocol = useCaseTokens[6].trim();
			final String methode = useCaseTokens[7].trim();
			final String queryString = useCaseTokens[8].trim();
			final String encoding = useCaseTokens[9].trim();

			if (Parser.REMOVES_QUOTES_FROM_USE_CASE_NAMES) {
				final String plainName = this.removeQuotes(name);
				useCase = new UseCase(plainName, startTime, endTime, uri, port,
						ip, protocol, methode, queryString, encoding);
			} else {
				useCase = new UseCase(name, startTime, endTime, uri, port, ip,
						protocol, methode, queryString, encoding);
			}

		}

		else {
			useCase = null; // insufficient use case informations in token;
		}
		return useCase;
	}

	/**
	 * Removes wrapping quotes of a given <code>String</code>.
	 * 
	 * @param str
	 *            the <code>String</code> with quotes to be removed.
	 * 
	 * @return the given <code>String</code> without wrapping quotes.
	 */
	private String removeQuotes(final String str) {

		return str.replaceAll("(^\")|(\"$)", "");
	}

	/**
	 * Parses a given string for a <code>long</code> value.
	 * 
	 * @param str
	 *            the <code>String</code> to be parsed.
	 * 
	 * @return the resulting <code>long</code> value.
	 * 
	 * @throws NumberFormatException
	 *             if the <code>String</code> does not denote a valid
	 *             <code>long</code> value.
	 */
	private long parseTime(final String str) throws NumberFormatException {

		// might throw a NumberFormatException;
		return Long.parseLong(str);
	}

	/**
	 * Iterator class for streamwise parsing of input lines (= sessions).
	 * 
	 * <p>
	 * Note that a <code>hasNext()</code> method is not available here, since it
	 * would need to read from file as a side effect; instead, the invoking
	 * instance must call nextSession() (generally within a <code>while</code>
	 * -loop), until <code>null</code> is being returned.
	 * 
	 * @author Eike Schulz (esc@informatik.uni-kiel.de)
	 * 
	 * @version 1.0 (2012-12-15)
	 */
	public class Iterator {

		/**
		 * Instance to be used for reading input lines.
		 */
		private final BufferedReader bufferedReader;

		/**
		 * Number of the currently parsed input line.
		 */
		private int lineNumber;

		/**
		 * Constructor for an <code>Iterator</code> with a specific
		 * <code>BufferedReader</code> instance for reading input lines.
		 * 
		 * @param bufferedReader
		 *            instance to be used for reading input lines.
		 */
		private Iterator(final BufferedReader bufferedReader) {

			this.bufferedReader = bufferedReader;
			this.lineNumber = 1;
		}

		/**
		 * Returns the next session from input source or <code>null</code>, if
		 * no more sessions are available.
		 * 
		 * @return the next session from input source, if available; in case no
		 *         more sessions are available, <code>null</code> will be
		 *         returned.
		 * 
		 * @throws IOException
		 *             if an I/O error occurs.
		 * @throws ParseException
		 *             in case any syntactical error is detected while parsing.
		 */
		public List<SessionData> nextSession(final int thresHoldSession)
				throws IOException, ParseException {

			final List<SessionData> sessionData;

			// readLine() might throw an IOException;
			final String line = this.bufferedReader.readLine();

			if (line != null) {

				// might throw a ParseException;
				sessionData = Parser.this.parseLine(line, this.lineNumber,
						thresHoldSession);
				this.lineNumber++;

			} else {

				sessionData = null;
				this.close(); // might throw an IOException;
			}
			return sessionData;
		}

		/**
		 * Closes the <code>BufferedReader</code> instance which is used for
		 * reading input lines.
		 * 
		 * @throws IOException
		 *             if an I/O error occurs.
		 */
		public void close() throws IOException {

			// closing a previously closed stream has no effect;
			this.bufferedReader.close(); // might throw an IOException;
		}
	}

	/**
	 * 
	 * @param sessionInputFilePath
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 * @throws ExtractionException
	 */
	public static ArrayList<SessionData> parseSessionsIntoSessionsRepository(
			final String sessionInputFilePath, final int thresHoldSessionTime)
			throws IOException, ParseException {

		// might throw a FileNotFound- or SecurityException;
		final Parser.Iterator iterator = Parser.getInstance().getIterator(
				sessionInputFilePath);

		final ArrayList<SessionData> sessions = new ArrayList<SessionData>();

		List<SessionData> sessionDataList;

		// nextSession() might throw a Parse- or IOException;
		while ((sessionDataList = iterator.nextSession(thresHoldSessionTime)) != null) {

			for (SessionData sessionData : sessionDataList) {

				sessionData.setTransactionType("noSessionType");

				for (UseCase useCase : sessionData.getUseCases()) {
					if (useCase.getName().contains("login")) {
						if (useCase.getQueryString() != null) {
							if (useCase.getQueryString().contains(
									"doBrowseVehicles-1")) {
								sessionData
										.setTransactionType("doBrowseVehicles-1");
							} else if (useCase.getQueryString().contains(
									"doManageInventory-1")) {
								sessionData
										.setTransactionType("doManageInventory-1");
							} else if (useCase.getQueryString().contains(
									"doPurchaseVehicles-1")) {
								sessionData
										.setTransactionType("doPurchaseVehicles-1");
							}
							break;
						}
					}
				}

				if (sessionData.getUseCases().size() > 0
						&& sessionData.getUseCases()
								.get(sessionData.getUseCases().size() - 1)
								.getName().equals("logout")) {
					sessions.add(sessionData);
				}

			}
		}

		iterator.close(); // closes the input stream;
		return sessions;
	}

}

package net.voorn.markov4jmeter.control;

import java.io.IOException;
import java.util.ArrayList;

import net.voorn.markov4jmeter.control.parser.ParseException;
import net.voorn.markov4jmeter.control.parser.Parser;
import net.voorn.markov4jmeter.control.parser.SessionData;

public class SessionFile {

	// singleton
	private static SessionFile instance = null;

	private static ArrayList<SessionData> sessions;

	private int index = -1;

	private static void init() {
		try {
			sessions = Parser.parseSessionsIntoSessionsRepository(
					"c:/jmetersession/session.dat", 0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Session file sucessfully imported!");
	}

	public static SessionFile getInstance() {
		if (instance == null) {
			instance = new SessionFile();
			init();
		}
		return instance;
	}

	public SessionData getSessionInstance() {
		this.index++;
		if (this.index > sessions.size()) {
			this.index = 0;
		}
		return sessions.get(this.index);
	}

}

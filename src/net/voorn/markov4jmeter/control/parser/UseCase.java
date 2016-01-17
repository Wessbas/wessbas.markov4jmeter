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

/**
 * This class represents a use case with specific start time, ending time and a
 * name.
 *
 * @author Eike Schulz (esc@informatik.uni-kiel.de)
 *
 * @version 1.0 (2012-12-15)
 */
public class UseCase {

    /** Name of the use case. */
    private String name;

    /** Start time of the use case. */
    private final long startTime;

    /** Ending time of the use case. */
    private final long endTime;

    /**
     * Uri.
     */
    private final String uri;
    /**
     * serverport.
     */
    private final int port;
    /**
     * serverip.
     */
    private final String ip;
    /**
     * used protocol
     */
    private final String protocol ;
    /**
     * used method
     */
    private final String methode ;
    /**
     * queryString.
     */
    private final String queryString  ;
    /**
     * used encoding,
     */
    private final String encoding ;

    /**
     * Constructor for a <code>UseCase</code> instance with specific start time,
     * ending time and a name.
     *
     * @param name       name of the use case.
     * @param startTime  start time of the use case.
     * @param endTime    ending time of the use case.
     * @param uri
     * @param port
     * @param ip
     * @param protocol
     * @param methode
     * @param queryString
     * @param encoding
     */
    public UseCase (
            final String name,
            final long startTime,
            final long endTime,
            final String uri,
            final int port,
            final String ip,
            final String protocol,
            final String methode,
            final String queryString,
            final String encoding) {

        this.name      = name;
        this.startTime = startTime;
        this.endTime   = endTime;
        this.uri   = uri;
        this.port   = port;
        this.ip   = ip;
        this.protocol   = protocol;
        this.methode   = methode;
        this.queryString   = queryString;
        this.encoding   = encoding;
    }
    
    /**
     * Constructor for a <code>UseCase</code> instance with specific start time,
     * ending time and a name.
     *
     * @param name       name of the use case.
     * @param startTime  start time of the use case.
     * @param endTime    ending time of the use case.
     * @param uri
     * @param port
     * @param ip
     * @param protocol
     * @param methode
     * @param queryString
     * @param encoding
     */
    public UseCase (
            final String name,
            final long startTime,
            final long endTime) {
        this.name      = name;
        this.startTime = startTime;
        this.endTime   = endTime;
        this.uri   = null;
        this.port   = 0;
        this.ip   = null;
        this.protocol   = null;
        this.methode   = null;
        this.queryString   = null;
        this.encoding   = null;
    }


    /**
     * Returns the name of the use case.
     *
     * @return name of the use case.
     */
    public String getName () {

        return this.name;
    }
    
    /**
     * Returns the name of the use case.
     *
     * @return name of the use case.
     */
    public void setName (String name) {
        this.name = name;
    }

    /**
     * Returns the start time of the use case.
     *
     * @return start time of the use case.
     */
    public long getStartTime () {

        return this.startTime;
    }

    /**
     * Returns the ending time of the use case.
     *
     * @return ending time of the use case.
     */
    public long getEndTime () {

        return this.endTime;
    }


	/**
	 * @return the uri
	 */
	public final String getUri() {
		return uri;
	}


	/**
	 * @return the port
	 */
	public final int getPort() {
		return port;
	}


	/**
	 * @return the ip
	 */
	public final String getIp() {
		return ip;
	}


	/**
	 * @return the protocol
	 */
	public final String getProtocol() {
		return protocol;
	}


	/**
	 * @return the methode
	 */
	public final String getMethode() {
		return methode;
	}


	/**
	 * @return the queryString
	 */
	public final String getQueryString() {
		return queryString;
	}


	/**
	 * @return the encoding
	 */
	public final String getEncoding() {
		return encoding;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "UseCase [name=" + name + ", startTime=" + startTime
				+ ", endTime=" + endTime + ", uri=" + uri + ", port=" + port
				+ ", ip=" + ip + ", protocol=" + protocol + ", methode="
				+ methode + ", queryString=" + queryString + ", encoding="
				+ encoding + "]";
	}    
    
}

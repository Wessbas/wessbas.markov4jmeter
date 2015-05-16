/*
 * Copyright 2007 Andre van Hoorn.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package net.voorn.markov4jmeter.control;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import net.voorn.markov4jmeter.util.Markov4JMeterVersion;
import org.apache.jmeter.util.JMeterUtils;

import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

/**
 * <p>Controls the number of active sessions executed for a Markov Model.</p> 
 * 
 * <p>An instance of this class is mainly used as a session entry and exit 
 * protocol similar to the semaphore-concept:  
 * <ul>
 * <li>When a MarkovController instance wants to enter a session, it calls the
 * method <i>enterSession()</i> within which it might be blocked depending on the current
 * number of active sessions and the number of allowed parallel sessions.</li>
 * <li>When a MarkovController exits a session, i.e. when it reached the exit
 * state, it calls the method <i>exitSession()</i>.</li>
 * </ul>
 * </p> 
 *
 * <p>Note that a MarkovController calling <i>enterSession()</i> provides the 
 * number of active session on method call. Thus, this controller is mainly 
 * responsibly for synchronizing the active session count and for maintaining 
 * the session entrance queue.</p>
 *
 * <p>This class is used in singleton mode, i.e. at any time at most one instance
 * exists and a reference to the instance is returned by calling 
 * <i>getInstance()</i>.</p>
 *
 * <p>When logging is enabled, the current number of active sessions is dumped
 *  to the given logfile.</p>
 *
 * <p>TODO: Change singleton-mode such that a singleton instance exists for 
 * each MarkovController. Therefore, the MarkovController's id should be passed
 * when calling <i>getInstance()</i>.</p>
 *
 * @author Andr&eacute; van Hoorn
 */
public class SessionArrivalController {
    /** Logger for this class */
    private static final Logger logger = LoggingManager.getLoggerForClass();
    
    /** Random number generator to be used within this class. */
    private static java.util.Random rand = new java.util.Random();
    
    /** Whether logging is enabled or not. */
    private boolean loggingEnabled = true;
    
    /** Writer used for for logging. */
    private BufferedWriter writer = null;
    
    /** Whether initialized or not. */
    private boolean initialized = false;
    
    /** The current number of threads is dumped with a resolution of a second. */
    private int lastSecond = -1;
    
    /** The number of active sessions. */
    private int numThreads = 0;
    
    /** The experiment start time in nanoseconds. */
    private static long startTime = 0; 
    
    /** Whether the test has ended or not. */
    private static boolean testEnded = true;
    
    /** The singleton instance. */
    private static SessionArrivalController instance = null;
    
    /** Will be set appropriately on each invocation of enterSession() */
    private static int allowedNum = -1;
    
    /** Creates a new instance of SessionArrivalController */
    private SessionArrivalController() {
    }
    
    /**
     * Returns the singleton instance.
     *
     * @return the instance.
     */
    public static synchronized SessionArrivalController getInstance(){
        if(SessionArrivalController.instance!=null)
            return instance;
        
        SessionArrivalController.instance = new SessionArrivalController();
        //StandardJMeterEngine.register(SessionArrivalController.instance);
        return SessionArrivalController.instance;
    }
    
    /**
     * Can be called at most once per instance. After the first called, this
     * method returns without doing anything.
     *
     * @param loggingEnabled iff true logging is enabled.
     * @param logfile the logfile.
     */
    public synchronized void init (boolean loggingEnabled, String logfile){
        if (this.initialized)
            return;
        
        this.loggingEnabled = loggingEnabled;
        if (this.loggingEnabled)
            try {
                this.openWriter(logfile);
            } catch (IOException ex) {
                logger.error("IO error when opening log file. Disabling logging", ex);
                this.loggingEnabled = false;
            }
        this.initialized = true;
    }
    
    /**
     * Returns the elapsed experiment time in minutes.
     *
     * @return the elapsed time in minutes.
     */
    private static double getExpTimeInMinutes(){
        long curMs = System.currentTimeMillis();
        double expMin = ((double)(curMs-startTime))/(1000*60);
        return expMin;
    }
    
    /**
     * Dumps the current number of active sessions. Must be synchronized from
     * outside.
     *
     * @param expMinute experiment time in minutes.
     * @param the allowed number of active sessions.
     * @param force dump in each case.
     */
    private void dumpNumSessions(double expMinute, int numAllowed, boolean force){
        double second = Math.floor(expMinute*60);
        if (force || (second>this.lastSecond)){
            /**
             * @TODO should restrict this output to a debug mode to be introduced
             */
            System.out.println("second: "+second+" : "+this.numThreads + "(" + numAllowed + ")");
            this.lastSecond = (int)second;
            try {
                this.writer.write((second/60)+","+this.numThreads+"\n");
                this.writer.flush();
            } catch (IOException ex) {
                logger.error("Write error", ex);
            }
        }
    }
    
    /**
     * Must be called by a thread before entering a new session.
     *
     * If allowedNum<=0 all threads can enter immediately,
     *
     * Depending on the current number of active sessions and on the number
     * of allowed sessions, the thread may be blocked.
     *
     * @param allowedNum the maximum number of allowed active sessions.
     * @throws InterruptedException when an error occurs while the thread 
     *         is blocked within the queue.
     */
    public void enterSession(int allowedNum) throws InterruptedException{       
        boolean mustSleep = true;
        
        /* the last value set is the current number of allowed sessions for 
           this session arrival controller. This is necessary since the text
           field containing the allowed sessions formula is not evaluated again
           on subsequent calls.
           Note: the number of allowed sessions is always set by the most recent
                 thread calling this method. */
        this.allowedNum = allowedNum;
        
        do{
            /**
             * Note that this is a dangerous loop! "Cooldown" of the test may take
             * some time since all sleeping threads will finally run since
             * allowedNum is at least 1.
             */
            double curTime = getExpTimeInMinutes();

            /*
             * This was the old way of retrieving the number of allowed threads:
             * int allowedNum = allowedNum(curTime); 
             */
            synchronized (this){
                if (this.allowedNum <=0)
                    this.allowedNum = -1;
                if ((this.allowedNum==-1) || (this.numThreads < this.allowedNum)){
                    mustSleep = false;
                    this.numThreads++;
                    //System.out.println("Must not sleep: allowed:"+this.allowedNum+" current:"+this.numThreads);
                    if (this.loggingEnabled)
                        this.dumpNumSessions(curTime, this.allowedNum, false);
                }else{
                    //System.out.println("Must sleep: allowed:"+this.allowedNum+" current:"+this.numThreads);
                }
            }
            
            if (mustSleep){
                // sleep for a period between 1000 ms and 1999 ms
                Thread.sleep(1000+rand.nextInt(1000));
            }
        } while(mustSleep);
    }
    
    /**
     * Must be called by a thread when exiting a session.
     */
    public void exitSession(){
        double curTime = getExpTimeInMinutes();
        int numThreads;
        synchronized (this){
            numThreads = --this.numThreads;
        }
    }
    
    /**
     * Opens the writer to write into given file.
     *
     * @param filename the filename.
     * @throws IOException when an IO error occurs.
     */
    private void openWriter(String filename) throws IOException {
        this.writer =// do not append
            new BufferedWriter(new FileWriter(filename, false));
    }

    /**
     * Indicate that a new test started.
     */
    public static synchronized void testStarted(){
        if(!testEnded)
            return;
        
        startTime = System.currentTimeMillis();
        testEnded = false;
        /** @TODO remove */
        logger.info("Using JMeter.Markov version " + Markov4JMeterVersion.getVERSION());
        System.out.println("Experiment start time (ms):" + startTime);
        JMeterUtils.setProperty("TEST.START.MS",startTime+"");
    }
    
    /**
     * Indicate that the current test stopped.
     */
    public static synchronized void testEnded() {
        if(testEnded)
            return;
        
        testEnded = true;
        System.out.println("Experiment stop time (ms):" + System.currentTimeMillis());
        if (SessionArrivalController.instance != null){
            SessionArrivalController instance = SessionArrivalController.instance;
            try {
                if (instance.writer!=null){
                    /* run once per test execution */
                    if (instance.loggingEnabled)
                        instance.dumpNumSessions(getExpTimeInMinutes(), 0, true);
                    instance.writer.close();
                    instance.writer = null;
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            SessionArrivalController.instance = null;
        }
    }
}

/*
 * $Id: CmdLine.java 870 2006-09-13 11:27:09 +0200 (mer., 13 sept. 2006) penaulau $
 */
package com.orange.matos.core;

/*
 * #%L
 * Matos
 * %%
 * Copyright (C) 2004 - 2014 Orange SA
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.net.URL;


/** 
 * A step is the description of a content to analyse.
 * It can belong to a campaign (describe by a file or by a directory).
 */
public abstract class Step implements Cloneable {
	
	/** Step status code : none. */
	public static final int NONE = 0;
	
	/** Step status name : none. */
	public static final String NONE_String = "None";
	
	/** Step status code : skipped. */
	public static final int SKIPPED = 1;
	
	/** Step status name : skipped. */
	public static final String SKIPPED_String = "Skipped";
	
	/** Step status code : passed. */
	public static final int PASSED = 2;
	
	/** Step status name : passed. */
	public static final String PASSED_String = "Passed";
	
	/** Step status code : failed. */
	public static final int FAILED = 3;
	
	/** Step status name : failed. */
	public static final String FAILED_String = "Failed";
	
	/** Step status code : processing. */
	public static final int PROCESSING = 4;
	
	/** The Constant PROCESSING_String. */
	public static final String PROCESSING_String = "Processing";
		
	// order in verdicts array is so that verdicts[<V>]==<V>_String
	// ex : verdicts[SKIPPED]==SKIPPED_String
	/** The Constant verdicts. */
	private static final String[] verdicts = {NONE_String, SKIPPED_String, PASSED_String, FAILED_String};
		
	

    /** Name of file to analyze. */
    private String code = null;

    /** Verdict of analysis. */
	private int verdict = NONE;
	
	/** Result file name. */
	private String outFileName = null;	
	
	/** Security profile. */
	private String profileName = null; // spec
	
	/** Css file. */
	private URL cssUrl = null;
	
	/** Log file. */
	private String logFileName = null;
	// public boolean apacheMode = false;
	
	/** Temporary folder. */
	private String temporaryDir = null;
	
	/** Mesage. */
	private String message = null;
	
	/** Elapsed time for analysis. */
	private long time = -1;
	
	/** Score of the analysis. */
	private int score = -1;

	/** The parameters. */
	private final DownloadParameters parameters = new DownloadParameters();
		
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
	    return String.format("profile=%s\ncode=%s\noutFileName=%s\ncss=%s\nlog=%s\ntemp=%s\nmessage=%s\nscore=%d\ntime=%d\nverdict=%d\n",
	                          profileName, code,outFileName, cssUrl== null ? "-" : cssUrl.toExternalForm(),logFileName, temporaryDir, message, score, time, verdict );
	}
	

	/**
	 * Checks for css.
	 *
	 * @return true, if successful
	 */
	public boolean hasCss() { return getCssUrl() == null; }
	
	/**
	 * Checks for log.
	 *
	 * @return true, if successful
	 */
	public boolean hasLog(){ return !isEmpty(getLogFileName()); }
	
	/**
	 * Checks for out.
	 *
	 * @return true, if successful
	 */
	public boolean hasOut() { return !isEmpty(getOutFileName()); }
	
	/**
	 * Checks for profile.
	 *
	 * @return true, if successful
	 */
	public boolean hasProfile() { return !isEmpty(getProfileName()); }
	
	/**
	 * Checks for temporary dir.
	 *
	 * @return true, if successful
	 */
	public boolean hasTemporaryDir(){ return !isEmpty(getTemporaryDir()); }
	
	/**
	 * Checks if is empty.
	 *
	 * @param s the s
	 * @return true, if is empty
	 */
	protected static boolean isEmpty(String s) { return (s==null)||(s.length()==0); }
	
	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) { 
			return null;
		}
	}
		
	/**
	 * Printable string of verdict code.
	 *
	 * @param i the i
	 * @return the string
	 */
	public static String stringOfVerdict(int i){ return verdicts[i]; }

    /**
     * Gets the verdict.
     *
     * @return the verdict
     */
    public int getVerdict() {
        return verdict;
    }

    /**
     * Sets the verdict.
     *
     * @param verdict the new verdict
     */
    public void setVerdict(int verdict) {
        this.verdict = verdict;
    }

    /**
     * Gets the out file name.
     *
     * @return the out file name
     */
    public String getOutFileName() {
        return outFileName;
    }

    /**
     * Sets the out file name.
     *
     * @param outFileName the new out file name
     */
    public void setOutFileName(String outFileName) {
        this.outFileName = outFileName;
    }

    /**
     * Gets the profile name.
     *
     * @return the profile name
     */
    public String getProfileName() {
        return profileName;
    }

    /**
     * Sets the profile name.
     *
     * @param profileName the new profile name
     */
    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    /**
     * Gets the css url.
     *
     * @return the css url
     */
    public URL getCssUrl() {
        return cssUrl;
    }

    /**
     * Sets the css url.
     *
     * @param cssUrl the new css url
     */
    public void setCssUrl(URL cssUrl) {
        this.cssUrl = cssUrl;
    }

    /**
     * Gets the log file name.
     *
     * @return the log file name
     */
    public String getLogFileName() {
        return logFileName;
    }

    /**
     * Sets the log file name.
     *
     * @param logFileName the new log file name
     */
    public void setLogFileName(String logFileName) {
        this.logFileName = logFileName;
    }

    /**
     * Gets the temporary dir.
     *
     * @return the temporary dir
     */
    public String getTemporaryDir() {
        return temporaryDir;
    }

    /**
     * Sets the temporary dir.
     *
     * @param temporaryDir the new temporary dir
     */
    public void setTemporaryDir(String temporaryDir) {
        this.temporaryDir = temporaryDir;
    }

    /**
     * Gets the message.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the message.
     *
     * @param message the new message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Gets the parameters.
     *
     * @return the parameters
     */
    public DownloadParameters getParameters() {
        return parameters;
    }

    /**
     * Gets the code.
     *
     * @return the code
     */
    public String getCode() {
        return code;
    }

    /**
     * Sets the code.
     *
     * @param androidFileName the new code
     */
    public void setCode(String androidFileName) {
        this.code = androidFileName;
    }


    /**
     * Gets the score.
     *
     * @return the score
     */
    public int getScore() {
        return score;
    }


    /**
     * Sets the score.
     *
     * @param score the new score
     */
    public void setScore(int score) {
        this.score = score;
    }


    /**
     * Gets the elapsed time.
     *
     * @return the time
     */
    public long getTime() {
        return time;
    }


    /**
     * Sets the elapsed time.
     *
     * @param time the duration of the analysis
     */
    public void setTime(long time) {
        this.time = time;
    }

}


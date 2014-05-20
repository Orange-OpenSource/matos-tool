/*
 * $Id: DownloadMonitor.java 2279 2013-12-11 14:45:44Z piac6784 $
 */
package com.orange.matos.utils;

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

/** This class is responsible for monitoring the status of a download
 * session. A user will typically create an instance of Downloader as
 * well as an instance of DownloadMonitor. The user will start the
 * Downloader object, as an independant thread, then call the
 * DownloadMonitor.waitForEnd() method, which is blocked until the
 * status is changed to something else than DLOAD_ONGOING. When
 * created, the Downloader thread is passed a pointer to its
 * DownloadMonitor, to keep it informed of the evolution of the
 * session's status. 
 */
public class DownloadMonitor  {
	
	/** The Constant DLOAD_FAILED. */
	final public static int DLOAD_FAILED = 1;
	
	/** The Constant DLOAD_SUCCESS. */
	final public static int DLOAD_SUCCESS = 2;
	
	/** The Constant DLOAD_ONGOING. */
	final public static int DLOAD_ONGOING = 3;
	
	/** The Constant DLOAD_TIMEOUT_C. */
	final public static int DLOAD_TIMEOUT_C = 4; // during connection
	
	/** The Constant DLOAD_TIMEOUT_T. */
	final public static int DLOAD_TIMEOUT_T = 5; // during transfer
	
	/** The Constant DLOAD_FAILED_NO_NEW_ATTEMPT. */
	final public static int DLOAD_FAILED_NO_NEW_ATTEMPT = 6;
	
	/** The status. */
	private int status = DLOAD_ONGOING;
	
	/**
	 * Gets the download status.
	 *
	 * @return the download status
	 */
	public int getDownloadStatus() { return status; };
	
	/**
	 * Sets the download status.
	 *
	 * @param status the new download status
	 */
	public void setDownloadStatus(int status) { this.status = status; }
	
	/**
	 * Reset the session status to DLOAD_ONGOING.
	 */
	public void reset() {
		setDownloadStatus(DLOAD_ONGOING);
	}
	
	/** 
	 * Allows to wait for the end (successful of or not) of the
	 * download session. The method returns only when the session is
	 * over (Active wait). 
	 */ 
	public void waitForEnd() { 
		while (getDownloadStatus() == DLOAD_ONGOING) {}
	};
	
	
}


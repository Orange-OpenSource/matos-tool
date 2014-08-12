/*
 * $Id: Timer.java 2279 2013-12-11 14:45:44Z Pierre Cregut $
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


/**
 * Thread which simulates a timer.
 *
 */
public class Timer extends Thread
{
	/** Rate at which timer is checked */
	protected final static int RATE = 100;
	
	/** Length of timeout */
	private int length;
	
	/** Time elapsed */
	private int elapsed;
	
	/** Timer monitor to report timeout to */
	TimerMonitor monitor;
	
	/** stop condition for this thread */
	private boolean shouldStop = false;
	
	/**
	 * Creates a timer of a specified length
	 * @param	length	Length of time before timeout occurs
	 */
	public Timer ( int length, TimerMonitor monitor ) {
		this.length = length;
		this.monitor = monitor;
		elapsed = 0;
	}
	
	/** Resets the timer back to zero */
	public synchronized void reset()	{
		elapsed = 0;
		shouldStop = false;
	}
	
	/** Stop this thread as soon as possible. */
	public synchronized void stopAsap() {
		shouldStop = true;
	}
	
	/** Performs timer specific code */
	@Override
	public void run()	{
		boolean b;
		synchronized (this){ b	= shouldStop; }
		while (!b) {
			try { Thread.sleep(RATE); }
			catch (InterruptedException ioe) { continue; }
			// Use 'synchronized' to prevent conflicts
			synchronized ( this ) {
				elapsed += RATE;
				if (elapsed > length) {
					monitor.timeout(this);
					//stop();
					shouldStop=true;
				}
				b = shouldStop;
			}
		}
	}
	
}

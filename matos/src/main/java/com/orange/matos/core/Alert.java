/*
 * $Id: Alert.java 2279 2013-12-11 14:45:44Z piac6784 $
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

import java.io.PrintStream;

/**
 * Handles two feeback channel and convey back regular exceptions to Matos level where they should be
 * printed.
 * @author Aurore Penault
 */
public class Alert extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Exception constructor with a message.
	 * @param msg
	 */
	public Alert(String msg) {
		super(msg);
	}
	
	private static PrintStream chan1=null;
	private static PrintStream chan2=null;
	
	private static boolean dumpMessage1=true;
	private static boolean dumpMessage2=true;
	
	private static boolean dumpExceptionTrace1=true;
	private static boolean dumpExceptionTrace2=true;
	
	private static void setOutput(int chanNum, PrintStream stream, boolean dumpMessage, boolean dumpExceptionTrace) {
		if (chanNum==1) {
			chan1 = stream;
			dumpMessage1 = dumpMessage;
			dumpExceptionTrace1 = dumpExceptionTrace;
		} else if (chanNum==2) {
			chan2 = stream;
			dumpMessage2 = dumpMessage;
			dumpExceptionTrace2 = dumpExceptionTrace;
		}
		dumpExceptionTrace1 = !dumpExceptionTrace2;
		dumpMessage1 = !dumpMessage2;
	}

	/**
	 * Change standard output
	 * @param stream
	 */
	public static  void setOutput1(PrintStream stream) {
		setOutput(1, stream, true, true);
	}
	/**
	 * Change error output.
	 * @param stream
	 */
	public static  void setOutput2(PrintStream stream) {
		setOutput(2, stream, true, true);
	}
	
	/**
	 * Change standard output
	 * @param stream
	 * @param dumpMessage
	 * @param dumpExceptionTrace
	 */
	public static  void setOutput1(PrintStream stream, boolean dumpMessage, boolean dumpExceptionTrace) {
		setOutput(1, stream, dumpMessage, dumpExceptionTrace);
	}
	
	/**
	 * Change error output
	 * @param stream
	 * @param dumpMessage
	 * @param dumpExceptionTrace
	 */
	public static  void setOutput2(PrintStream stream, boolean dumpMessage, boolean dumpExceptionTrace) {
		setOutput(2, stream, dumpMessage, dumpExceptionTrace);
	}
	
	/**
	 * Sends back an exception to Matos level. Warning : there is no informative message for the
	 * user
	 * @param e the exception
	 * @throws Alert
	 */
	public static Alert raised(Exception e) {
		return raised(e,"");
	}
	
	/**
	 * Sends back an exception to Matos level for user notification. The exception is only
	 * there for debug and will not be visible on the user level messages.
	 * @param e The exception raised.
	 * @param msg The message printed to the user
	 * @throws Alert always raised
	 */
	public static Alert raised(Exception e, String msg) {
		if (chan1!=null) {
			if (dumpMessage1) chan1.println(msg);
			if ((e!=null)&&(dumpExceptionTrace1)) e.printStackTrace(chan1);
		}	
	
		if (chan2!=null) {
			if (dumpMessage2) chan2.println(msg);
			if ((e!=null)&&(dumpExceptionTrace2)) e.printStackTrace(chan2);
		}	
		
		return new Alert(msg);
	}	
}
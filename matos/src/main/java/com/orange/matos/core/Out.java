/*
 * $Id: Out.java 2279 2013-12-11 14:45:44Z piac6784 $
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

import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Date;

/**
 *  Class carrying the two outputs for messages: main and log.
 *  To be initialized at the very beginning of the program.
 *  All messages should be printed using exclusively Out.main 
 *  or/and Out.log print streams.
 */
public class Out {
		
		private static PrintStream main=System.out, log=System.err;
		
		/**
		 * Set the regular output
		 * @param stream
		 */
		public static void setMain(PrintStream stream) {
			main = stream; 			
		}
		/**
		 * Set the error log output
		 * @param stream
		 */
		public static void setLog(PrintStream stream) {
			log = new LogPrintStream(stream); 
		}
		
		/**
		 * Get regular output
		 * @return
		 */
		public static PrintStream getMain() { return main; }
		/**
		 * Get log output
		 * @return
		 */
		public static PrintStream getLog() { return log; }
		
		/**
		 * @author piac6784
		 * A stream for log
		 */
		public static class LogPrintStream extends PrintStream {
			/**
			 * Constructor
			 * @param out
			 */
			public LogPrintStream(OutputStream out) {
				super(out);
			}

			@Override
			public void println() {
				printTimeStamp();
				super.println();
			}

			@Override
			public void println(boolean x) {
				printTimeStamp();
				super.println(x);
			}

			@Override
			public void println(char x) {
				printTimeStamp();
				super.println(x);
			}

			@Override
			public void println(char[] x) {
				printTimeStamp();
				super.println(x);
			}

			@Override
			public void println(double x) {
				printTimeStamp();
				super.println(x);
			}

			@Override
			public void println(float x) {
				printTimeStamp();
				super.println(x);
			}

			@Override
			public void println(int x) {
				printTimeStamp();
				super.println(x);
			}

			@Override
			public void println(long x) {
				printTimeStamp();
				super.println(x);
			}

			@Override
			public void println(Object x) {
				printTimeStamp();
				super.println(x);
			}

			@Override
			public void println(String x) {
				printTimeStamp();
				super.println(x);
			}
			
			private void printTimeStamp() {
				Date now = new Date();
				DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
//				super.print("["+now.toString()+"] ");
				super.print("["+df.format(now)+"] ");
			}
		}
}
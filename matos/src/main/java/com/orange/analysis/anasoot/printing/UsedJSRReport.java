package com.orange.analysis.anasoot.printing;

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
import java.util.ArrayList;

/**
 * @author piac6784
 * Prints the list of JSR used by the midlet.
 */
public class UsedJSRReport extends JavaReport{
	
	private static String wma11 = "WMA 1.1 (JSR 120)";
	private static String wma20 = "WMA 2.0 (JSR 205)";
	
	ArrayList<String> usedJSRs = new ArrayList<String>();
	private boolean tell;
	
	/**
	 * @param name name of the report.
	 * @param tell print or not.
	 */
	public UsedJSRReport(String name, boolean tell) {
		this.name = name;
		this.tell = tell;
	}
	
	/**
	 * Adds a JSR to the list of used JSR.
	 * @param jsrName
	 */
	public void addJSR(String jsrName) {
		if (!usedJSRs.contains(jsrName)) {
			if (jsrName.equals(wma11) || jsrName.equals(wma20)) {
				if (jsrName.equals(wma11) && !usedJSRs.contains(wma20)) {
					usedJSRs.add(jsrName);
				}else if (jsrName.equals(wma20)) {
					usedJSRs.add(jsrName);
					usedJSRs.remove(wma11);
				}
			} else {
				usedJSRs.add(jsrName);
			}
		}
	}
	
	/**
	 * Printing.
	 * @param outStream Stream for output
	 * @param xmlFormat use an XML format or not
	 */
	public void tell(PrintStream outStream, boolean xmlFormat) {
		if (tell && !usedJSRs.isEmpty()) {
			if (xmlFormat) {
				outStream.println("<usedJSR>");
				for(String jsr: usedJSRs) {
					outStream.println("<jsr name=\"" + jsr + "\"/>");
				}
				outStream.println("</usedJSR>");
			} else {
				outStream.println("<h3>JSRs used by the MIDlet</h3>\n");
				outStream.println("<ul>");

				for(String jsr: usedJSRs) {
					outStream.println("<li>" + jsr + "</li>");
				}
				outStream.println("</ul>");
			}
		}
	}
		
	@Override
	public void tellAll(PrintStream out) {}

}

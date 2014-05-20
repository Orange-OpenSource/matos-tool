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

import com.orange.analysis.anasoot.result.JavaResult;
import com.orange.matos.core.Alert;
import com.orange.matos.core.XMLStream;
import com.orange.matos.utils.HtmlOutput;

/**
 * @author piac6784
 * These are JSR used but that are not part of the security profile.
 * Using them is a risk.
 */
public class UnresolvedReport extends JavaReport {
	
	ArrayList <String> addedJSR = new ArrayList <String>();
	
	/**
	 * Constructor.
	 * @param name name of report
	 */
	public UnresolvedReport(String name){
		this.name = name;
	}
	
	/**
	 * Add a JSR to the list.
	 * @param jsrName
	 */
	public void add(String jsrName){
		if (!addedJSR.contains(jsrName)){
			addedJSR.add(jsrName);
		}
	}
	
	/**
	 * Prints the result of the analysis
	 * @param outStream Printstream
	 * @param xmlFormat as XML or not.
	 * @throws Alert
	 */
	public void tell (PrintStream outStream, boolean xmlFormat) throws Alert{
		if (!addedJSR.isEmpty()){
			if (xmlFormat) {
				XMLStream xmlout = new XMLStream(outStream);
				xmlout.element("jsrtodo");
				for(String jsr : addedJSR) {
					xmlout.element("jsr");
					xmlout.attribute("name", jsr);
					xmlout.endElement();
				}
				xmlout.endElement();
			} else {
			outStream.println(HtmlOutput.warning("The following JSRs are not covered by the security policy but are used by the MIDlet:"));
			outStream.println("<ul>");
			for(String jsr : addedJSR) {
				outStream.println("<li>"+ jsr +"</li>");
			}
			outStream.println("</ul>");
			}
		}
	}
	

	@Override
	public void tellAll(PrintStream out) { }

	@Override
	public void tell(PrintStream out, boolean xmlFormat, JavaResult result, int pos) throws Alert {
	}

}

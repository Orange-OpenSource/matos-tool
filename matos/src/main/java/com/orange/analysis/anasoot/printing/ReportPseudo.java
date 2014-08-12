/*
 * $Id:ReportPseudo.java 917 2006-09-27 10:15:16 +0200 (mer., 27 sept. 2006) penaulau $
 */
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

import com.orange.analysis.anasoot.result.AbsValue;
import com.orange.analysis.anasoot.result.AndValue;
import com.orange.analysis.anasoot.result.JavaResult;
import com.orange.analysis.anasoot.result.OrValue;
import com.orange.analysis.anasoot.result.StringValue;
import com.orange.matos.core.Alert;
import com.orange.matos.core.XMLStream;

/**
 * @author Pierre Cregut
 * A report to print if a given result coresponds to a pseudo constant
 * or is in fact more complex. Pseudo constants are a way to ensure 
 * analysis of string succeeds.
 */
public class ReportPseudo extends JavaReport {
	private String ok, nok;
	private boolean seen, correct;
	/**
	 * Constructor
	 * @param name name of report
	 * @param okReport String if ok
	 * @param badReport String if bad.
	 */
	public ReportPseudo(String name, String okReport, String badReport) {
		this.name = name;
		ok = okReport;
		nok = badReport;
		reset();
	}
	
	@Override
	public void reset() {
		seen = false;
		correct = true;
	}
	
	@Override
	public void tell (PrintStream outStream, boolean xmlFormat, JavaResult result, int position) throws Alert{
		JavaResult javaResult = (JavaResult)result;
		AbsValue argument = javaResult.argument;
		if (argument instanceof AndValue) {
			if (position != -1) {
				argument = ((AndValue) argument).get(position);
			} else {
				outStream.println ("Can only handle simple reports");
				finalVerdict = false;
				return;
			}
		}		
		// For pseudo constant we do not need the pretty version of
		// strings.
		seen = true;		
		if (argument instanceof OrValue) {
			for (AbsValue v : ((OrValue) argument).vals) {
				if (! (v instanceof StringValue)) {
					if (xmlFormat) {
						XMLStream xmlout = new XMLStream(outStream);
						xmlout.element("pseudostring");
						xmlout.attribute("ref", javaResult.getRef());
						xmlout.print("");
						v.xmlOutput(outStream);
						xmlout.close();
					} else {
						print (outStream, nok, v.toString(), javaResult, null);
					}
					hasOut = true;
					finalVerdict = false;
					correct = false;
				}
			}
		} else if ( !(argument instanceof StringValue)) {
			if (xmlFormat) {
				XMLStream xmlout = new XMLStream(outStream);
				xmlout.element("pseudostring");
				xmlout.attribute("ref", javaResult.getRef());
				xmlout.print("");
				argument.xmlOutput(outStream);
				xmlout.endElement();
			} else {
				print (outStream, nok, argument.toString(), javaResult, null);
			}
			hasOut = true;
			finalVerdict = false;
			correct = false;
		}
	}
	
	@Override
	public void tellAll(PrintStream out) {
		if (seen && correct){
			out.println("<p>" + ok + "</p>");
			hasOut = true;
		}
	}
	
}

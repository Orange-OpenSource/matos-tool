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
import java.util.Set;

import soot.SootMethod;

import com.orange.analysis.anasoot.result.ProgramPoint;
import com.orange.matos.core.Alert;
import com.orange.matos.utils.HtmlOutput;

/**
 * Reports the number of time a method is used. This is a weird report as it does not use
 * the tell method, only the infrastructure for naming reports. As a consequence it can be
 * put in a report conjunction but will not work there. It can only be used by a SpyUse.
 *  
 * @author Pierre Cregut
 *
 */
public class ReportUse extends JavaReport {
	
	final private String message;
	final private boolean silent;
	
	/**
	 * Constructor
	 * @param name name of the report
	 * @param message message to print with the usage
	 * @param silent should it contribute to the final verdict as an error.
	 */
	public ReportUse(String name, String message, boolean silent){
		this.name = name;
		this.message = message;
		this.silent = silent;
	}
	
	/**
	 * Gives the string to print in the report
	 * @param method the result given by anasoot
	 */
	public void tell(PrintStream out, SootMethod method, Set <ProgramPoint> ppset) throws Alert {
		print (out,message,method, ppset);
		hasOut = true;
		finalVerdict = silent;
	}

	/**
	 * Gives the string to print in report replacing strings like "%..."
	 * @param message the message to transform
	 * @param method the given result
	 * @return the string to print in report
	 * @throws Alert 
	 */
	private void print(PrintStream out, String message, SootMethod method, Set <ProgramPoint> ppset) throws Alert {
		if (message.equals("-")) return;
		out.print("<p>");
		int i = 0;
    	int j;
    	while((j = message.indexOf('%',i)) >= 0) {
    		if (j > i) out.print(message.substring(i,j));
    		switch (message.charAt(j+1)) {
    		case 'c':
    			out.print(HtmlOutput.escape(method.getDeclaringClass().getName())); break;
    		case 'n':
				char code = message.charAt(j+2);
				j++;
				IntegerCell count = globalCounterTable.get(code);
				if (count == null) {
					count = new IntegerCell(1);
					globalCounterTable.put(code, count);
				}
				out.print(count);
				count.incr();
				break;
    		case 'm':
    			out.print(HtmlOutput.escape(method.getName())); break;
    		case 's':
    			out.print(HtmlOutput.escape(method.getSubSignature())); break;
    		case 'p':
    			out.print("<ul>");
    			for(ProgramPoint pp : ppset) {
    				out.print("<li>" + pp.offset + "@" + HtmlOutput.escape(pp.method.getSignature()) + "</li>");
    			}
    			out.print("</ul>");
    			break;
    		case '%':
    			out.print('%'); break;
    		default: throw Alert.raised(null,"Illegal escaped character in printing rule: " + message);
    		}
    		i = j+2;
    	}
    	out.print(message.substring(i));
		out.print("</p>\n");
	}
	
}

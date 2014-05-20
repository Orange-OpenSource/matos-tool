/*
 * $Id:ReportAnd.java 917 2006-09-27 10:15:16 +0200 (mer., 27 sept. 2006) penaulau $
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
import java.util.ArrayList;

import com.orange.analysis.anasoot.result.AbsValue;
import com.orange.analysis.anasoot.result.AndValue;
import com.orange.analysis.anasoot.result.JavaResult;
import com.orange.matos.core.Alert;

/**
 * @author piac6784
 * Conjunction report. The output of this report is just the concatenation of the output of
 * its member reports.
 */
public class ReportAnd extends JavaReport {
	private final ArrayList <JavaReport> reports;
	private final ArrayList <Integer> positions;
	/**
	 * @param name name of the conjunction report
	 * @param reports Members.
	 * @param positions
	 */
	public ReportAnd(String name, ArrayList<JavaReport> reports, ArrayList <Integer> positions) {
		this.name = name;
		this.reports = reports;
		this.positions = positions;
	}
	
	@Override
	public void tell (PrintStream out, boolean xmlFormat, JavaResult result, int position) throws Alert{
	    if (position != -1) {
	       JavaResult javaResult = (JavaResult)result;
	       AbsValue argument = javaResult.argument;
	       if (argument instanceof AndValue) {
	          AndValue argAnd = (AndValue) argument;
	          if (position < argAnd.size()) {
	              argument = argAnd.get(position);
	              javaResult = javaResult.copy();
	              javaResult.argument = argument;
	              result = javaResult;
	            } else {
	                throw Alert.raised(null,"Can only handle simple reports with right position " + name + " " + position);
	            }
	        }
	    }
		int i = 0;
		for (JavaReport r: reports) {
			r.tell(out, xmlFormat, result, positions.get(i++));
		}
	}
	
	
	@Override
	public void tellAll(PrintStream out) { }
}

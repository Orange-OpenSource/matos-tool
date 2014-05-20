/*
 * $Id: SpyMethodUse.java 2279 2013-12-11 14:45:44Z piac6784 $
 */
package com.orange.analysis.anasoot.spy;

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
import java.util.HashSet;
import java.util.Set;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;

import com.orange.analysis.anasoot.printing.ReportUse;
import com.orange.analysis.anasoot.result.ProgramPoint;
import com.orange.matos.core.Alert;
import com.orange.matos.core.XMLStream;

/**
 * A probe that identifies where a given method is used. 
 * @author piac6784
 *
 */
public class SpyMethodUse implements SpyMethod {
	final SootMethod method;
	final ReportUse report;
	final String name;
	int c = 0;
	Set <ProgramPoint> usePoints;
	
	
	/**
	 * Constructor.
	 * @param name it identifies the query among others. can be provided
	 *   by the security profile or generated on the fly
	 * @param m the method whose usage is analyzed
	 * @param report a way to format the results. 
	 */
	public SpyMethodUse(String name, SootMethod m, ReportUse report) {
		method = m;
		this.name = name;
		this.report = report;
		usePoints = new HashSet<ProgramPoint>();
	}	
	
	@Override
	public SootMethod getMethod() { return method; }
	
	@Override
	public void spy(MethodSpyAnalysis ad, InvokeExpr ie, Unit st) {
		c++;
		usePoints.add(new ProgramPoint(ad.method,(Stmt) st));
	}
	
	@Override
	public boolean useful() { return c > 0; }
	
	@Override
	public void dump (PrintStream out, boolean xmlFormat) throws Alert {
		if (c>0) {
			if (xmlFormat) {
				XMLStream xmlout = new XMLStream(out);
				xmlout.element("usage");
				xmlout.attribute("count", c);
				xmlout.attribute("method", method.getSignature());
				for(ProgramPoint pp : usePoints) pp.out(xmlout);
				xmlout.close();
			} else if (report!=null){
				report.tell(out,method,usePoints);
			} else {
				out.println("---------------------------------");
				out.println(method.getSignature () + " is used " + c + " times.");
				out.println("---------------------------------");
			}
		}
	}

	@Override
	public String getName() {
		return name;
	}
	
}

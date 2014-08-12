/*
 * $Id: SpyReturn.java 2279 2013-12-11 14:45:44Z Pierre Cregut $
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

import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.ReturnStmt;

import com.orange.analysis.anasoot.printing.JavaReport;
import com.orange.analysis.anasoot.result.AbsValue;
import com.orange.analysis.anasoot.result.JavaResult;
import com.orange.analysis.anasoot.result.OrValue;
import com.orange.matos.core.Alert;
import com.orange.matos.core.XMLStream;

/**
 * Performs a points-to analysis on the returned results of a method.
 * @author Pierre Cregut
 *
 */
public class SpyReturn {
	/**
	 * The name of the analysis. 
	 */
	final private String tableName;

	/**
	 * The method whose results are analysed.
	 */
	final private SootMethod method;
	/**
	 * The results as a disjunction.
	 */
	final private OrValue av;

	/**
	 * Report to pretty-print the result.
	 */
	final private JavaReport report;

	/**
	 * Create a new analysis for the result of a method
	 * @param t the name to identify the analysis
	 * @param m the method analysed.
	 */
	public SpyReturn(String t, SootMethod method, JavaReport report) {
		av = new OrValue();
		tableName = t;
		this.method = method;
		this.report = report;
	}	

	/**
	 * Accessor the the value.
	 * @return
	 */
	public AbsValue getAbsValue() { return av; }

	/**
	 * Performs the analysis on a given statement that corresponds to a return statement
	 * of the spied method.
	 * @param ad the analyser with the context of the call
	 * @param st the statement.
	 */
	public void spy(MethodSpyAnalysis ad, ReturnStmt st){
		Value v = st.getOp();
		AbsValue defs =	ad.analyze_expr(v, st, new HashSet<Unit>());
		av.add(defs);
	}

	/**
	 * Display the result of the analysis on a stream in HTML or XML format.
	 * @param out the output stream
	 * @param xmlFormat the format to choose (True for XML, False for HTML).
	 */
	public void dump (PrintStream out, boolean xmlFormat) throws Alert {
		if (report == null) {
			if (xmlFormat) {
				XMLStream xmlout = new XMLStream(out);
				xmlout.element("return");
				xmlout.attribute("id", tableName);
				xmlout.attribute("signature", method.getSignature());
				xmlout.print("");
				for(AbsValue val : av.vals) {
					out.print("  ");
					val.xmlOutput(out);
					out.println();
				}
				xmlout.close();
			} else
				out.println(tableName + "\t" + method + "\t" + av.vals);
		} else {
			JavaResult jr = new JavaResult(av, method, null, -1);
			report.tell(out,xmlFormat,jr, -1);
		}
	}

	/**
	 * Check that the rule has been used and there is something to report.
	 * @return
	 */
	public boolean useful() { return av.vals.size() > 0; }

	/**
	 * Returns the name of the probe.
	 * @return
	 */
	public String getName() { return tableName; }

}

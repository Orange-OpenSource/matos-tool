/*
 * $Id: SpyField.java 2279 2013-12-11 14:45:44Z Pierre Cregut $
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

import soot.Local;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.SootField;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceFieldRef;

import com.orange.analysis.anasoot.printing.JavaReport;
import com.orange.analysis.anasoot.result.AbsValue;
import com.orange.analysis.anasoot.result.JavaResult;
import com.orange.analysis.anasoot.result.OrValue;
import com.orange.matos.core.Alert;
import com.orange.matos.core.XMLStream;

/**
 * @author Pierre Cregut
 * Probe to spy the potential contents of a field
 */
public class SpyField {
	final String tableName;
	final SootField field;
	final JavaReport report;
	final OrValue av;
	boolean done = false;

	/**
	 * Constructor
	 * @param t probe name
	 * @param f field to spy
	 * @param report way to present the result
	 */
	public SpyField(String t,SootField f, JavaReport report) {
		tableName = t;
		av = new OrValue();
		field = f;
		this.report = report;
	}	

	/**
	 * The result of analysis
	 * @return
	 */
	public AbsValue getAbsValue() { return av; }

	/**
	 * Perform the field analysis
	 * @param st the statement containing the field use
	 * @param pag the points-to analysis
	 * @param loc the local context
	 * @param nt
	 * @param m_orig the method containing the statement.
	 */
	public void spy(MethodSpyAnalysis ad, Unit st){
		if (P2SAux.is_simple(field.getType())) {
			AbsValue defs = ad.analyze_single(st, new HashSet<Unit>());
			av.add(defs);
		} else {
			if (done) return;
			done = true;
			Value left = ((AssignStmt) st).getLeftOp();
			if (left instanceof InstanceFieldRef) {
				Value rawBase = ((InstanceFieldRef) left).getBase();
				assert (rawBase instanceof Local) : "JIMPLE invariant one deref at a time"; 
				Local base = (Local) rawBase;
				PointsToSet targetPts = ad.pag.reachingObjects (base, field);
				AbsValue defs = P2SAux.p2sContents(ad.cc.nodeTable, targetPts);
				av.add(defs);
			} else {
				PointsToAnalysis pag = ad.pag;
				
				PointsToSet targetPts = 
					// ((pag instanceof DemandCSPointsTo) ? ((DemandCSPointsTo) pag).getPAG() : pag)
					pag.reachingObjects (field);
					
				AbsValue defs = P2SAux.p2sContents(ad.cc.nodeTable, targetPts);
				av.add(defs);
			}
		}
	}

	/**
	 * Direct unprocessed output of the result
	 * @param out The streeam to print to.
	 * @throws Alert 
	 */
	public void dump (PrintStream out, boolean xmlFormat) throws Alert {
		if (report != null) {
			if (xmlFormat) {
				XMLStream xmlout = new XMLStream(out);
				xmlout.element("field");
				xmlout.attribute("id", tableName);
				xmlout.attribute("field", field);
				xmlout.println();
				out.println("<field id=\"" + tableName + "\" field=\"" + field.toString() + "\">");
				for(AbsValue v : av.vals) {
					out.print("  ");
					v.xmlOutput(out);
					out.println();
				}
				xmlout.close();
			} else {
				JavaResult jr = new JavaResult(av, null, null, -1);
				report.tell(out,xmlFormat,jr, -1);
			}
		}
	}
	
	/**
	 * Check that there is something useful to report.
	 * @return true if a result is present.
	 */
	public boolean useful() { return av.vals.size() > 0; }

	/**
	 * Name of the probe
	 * @return
	 */
	public String getName() { return tableName; }

}

/*
 * $Id:SpyDef.java 917 2006-09-27 10:15:16 +0200 (mer., 27 sept. 2006) penaulau $
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import soot.FastHierarchy;
import soot.Local;
import soot.PointsToSet;
import soot.Scene;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.Constant;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.StringConstant;
import soot.jimple.spark.ondemand.AllocAndContext;
import soot.jimple.spark.ondemand.AllocAndContextSet;
import soot.jimple.spark.pag.PAG;
import soot.jimple.spark.sets.DoublePointsToSet;
import soot.jimple.spark.sets.PointsToSetInternal;
import soot.tagkit.BytecodeOffsetTag;
import soot.toolkits.graph.CompleteUnitGraph;

import com.orange.analysis.anasoot.printing.JavaReport;
import com.orange.analysis.anasoot.result.AbsValue;
import com.orange.analysis.anasoot.result.AndValue;
import com.orange.analysis.anasoot.result.ConstantValue;
import com.orange.analysis.anasoot.result.JavaResult;
import com.orange.analysis.anasoot.result.OrValue;
import com.orange.analysis.anasoot.result.StringValue;
import com.orange.analysis.anasoot.result.UnknownValue;
import com.orange.matos.core.Alert;
import com.orange.matos.core.XMLStream;

/**
 * @author piac6784
 * Probe on the arguments of a method.
 */
public class SpyMethodArgs implements SpyMethod {
	String tableName;
	SootMethod method;
	HashSet<JavaResult> table;
	ArrayList<Integer> argPos;
	OrValue av;
	JavaReport report;
	
	/**
	 * Constructor
	 * @param t table name
	 * @param m method to analyse
	 * @param position position of arguments
	 * @param r report
	 */
	public SpyMethodArgs(String t,SootMethod m, ArrayList<Integer> position, JavaReport r) {
		table = new HashSet<JavaResult> ();
		av = new OrValue();
		tableName = t;
		method = m;
		
		argPos = position;
		report = r;
	}	
	
	@Override
	public SootMethod getMethod() { return method; }
	
	/**
	 * Result of the analysis.
	 * @return
	 */
	public AbsValue getAbsValue() { return av; }
	
	@Override
	public void spy(MethodSpyAnalysis ad, InvokeExpr ie, Unit st){
		SootMethod m = ie.getMethod();
		ArrayList<AbsValue> resultRow = new ArrayList<AbsValue> ();
		for (Integer pos : argPos) {
			AbsValue defs;
			if (pos == 0 && ie instanceof InstanceInvokeExpr) {
				Value v = ((InstanceInvokeExpr) ie).getBase();
				if (v instanceof Local) {
					Local base = (Local) v;
					PointsToSet targetPts = ad.pag.reachingObjects (base);
					defs = P2SAux.p2sContents(ad.cc.nodeTable, targetPts);
					P2SAux.check(targetPts, ad.method, st); // DEBUG
				} else if (v instanceof StringConstant) {
					defs = new StringValue(((StringConstant) v ).value);
				} else defs = new UnknownValue("*");
			} else if (pos==0) {
				defs = new UnknownValue("*");
			} else if (pos == -1) {
				if (st instanceof AssignStmt) {
					// Local lhs =)
					PointsToSet argPts =  getPointstoSet((AssignStmt) st, ad);// ad.pag.reachingObjects (lhs);
					P2SAux.check(argPts, ad.method, st); // DEBUG
					defs = P2SAux.p2sContents(ad.cc.nodeTable, argPts);
				} else defs = new UnknownValue("*");
			} else {
				Value v = ie.getArg(pos - 1);
				if (v instanceof Local) {
					Local arg = (Local) v;
					if (P2SAux.is_simple(arg.getType())) {
						PointsToSet argPts = ad.pag.reachingObjects (arg); // DEBUG
						P2SAux.check(argPts, ad.method, st); // DEBUG
						defs = ad.analyzeLocal(arg, st, new HashSet<Unit>());
					} else {
						PointsToSet argPts = ad.pag.reachingObjects (arg);
						defs = P2SAux.p2sContents(ad.cc.nodeTable, argPts);
						P2SAux.check(argPts, ad.method, st); // DEBUG
					}
				} else if (v instanceof StringConstant) {
					defs = new StringValue(((StringConstant) v ).value);
				} else if (v instanceof Constant)
					defs = new ConstantValue((Constant) v, ((Constant) v).getType());
				else {
					defs = new UnknownValue(v.toString()); // debug
				}
			}
			resultRow.add(defs);
		}
		AbsValue resultValue = (resultRow.size() == 1) ? resultRow.get(0) : new AndValue(resultRow);
		
		BytecodeOffsetTag tag = (BytecodeOffsetTag) st.getTag("BytecodeOffsetTag");
		int offset = (tag == null) ? -1 : tag.getBytecodeOffset(); 
		JavaResult jr = new JavaResult(resultValue, m, ad.method, offset);
		
		table.add(jr);
		av.add(resultValue);
		return;
	}
	
	/**
	 * Gets the associated points to set for an assignement.
	 * @param st
	 * @param ad
	 * @return
	 */
	public static PointsToSet getPointstoSet(AssignStmt st, MethodSpyAnalysis ad) {
		FastHierarchy hier = Scene.v().getFastHierarchy();
		CompleteUnitGraph ug = ad.loc.getUnitGraph();
		Value v = st.getLeftOp();
		assert (v instanceof Local) : "JIMPLE invariant : No combined invoke with field assignment";
		Local lhs = (Local) v;
		List <Unit> l = ug.getUnexceptionalSuccsOf(st);
		PointsToSet argPts = ad.pag.reachingObjects (lhs);
		if (l.size() == 1) {
			Unit st2 = l.get(0);
			if (st2 instanceof AssignStmt) {
				Value rhs2 = ((AssignStmt) st2).getRightOp();
				if (rhs2 instanceof CastExpr && ((CastExpr) rhs2).getOp().equals(lhs)) {
					Type t1 = ((CastExpr) rhs2).getCastType(); 
					if (argPts instanceof AllocAndContextSet) {
						AllocAndContextSet result = new AllocAndContextSet();
						for (AllocAndContext ac : (AllocAndContextSet) argPts) {
							if (hier.canStoreType(ac.alloc.getType(), t1)) result.add(ac);
						}
						return result;
					}
					if (argPts instanceof PointsToSetInternal) { 
						PointsToSetInternal result = new DoublePointsToSet(t1, (PAG) ad.pag);
						result.addAll((PointsToSetInternal) argPts, null);
						return result;
					}
				}
			}
		}
		return argPts;
	}
	
	@Override
	public boolean useful() { return table.size() > 0; }
	
	@Override
	public void dump (PrintStream out, boolean xmlFormat) throws Alert {
		if (table.size() > 0) {
			for(JavaResult jr : table) {
				AbsValue v = jr.argument;
				// jr.normalize(true);
				if (xmlFormat) {
					HashMap<String, String> tags = jr.tags;
					XMLStream xmlout = new XMLStream(out);
					xmlout.element("args");
					xmlout.attribute("kind", tableName);
					xmlout.attribute("orig", jr.method_orig.getName());
					xmlout.attribute("offset", jr.offset_orig);
					xmlout.attribute("target", jr.method.getName());
					for(Entry <String,String> entry : tags.entrySet()) {
						String  tagName = entry.getKey();
						String tagValue = entry.getValue();
						xmlout.attribute(tagName, tagValue);
					}
					xmlout.print("");
					v.xml(xmlout);
					// v.normalize(false).xmlOutput(out);
					
					xmlout.close();
				}
				if (report != null) report.tell(out,xmlFormat,jr, -1); 
			}
		}
	}

	@Override
	public String getName() {
		return tableName;
	}
	
}

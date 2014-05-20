/*
 * $Id: P2SAux.java 2279 2013-12-11 14:45:44Z piac6784 $
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import soot.PointsToSet;
import soot.PrimType;
import soot.Scene;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.jimple.spark.ondemand.AllocAndContext;
import soot.jimple.spark.ondemand.AllocAndContextSet;
import soot.jimple.spark.ondemand.WrappedPointsToSet;
import soot.jimple.spark.pag.AllocNode;
import soot.jimple.spark.pag.Node;
import soot.jimple.spark.pag.StringConstantNode;
import soot.jimple.spark.sets.P2SetVisitor;
import soot.jimple.spark.sets.PointsToSetInternal;
import soot.jimple.toolkits.pointer.FullObjectSet;

import com.orange.analysis.anasoot.arrayanalysis.ArrayAnalysis;
import com.orange.analysis.anasoot.result.AbsValue;
import com.orange.analysis.anasoot.result.NodeTable;
import com.orange.analysis.anasoot.result.NodeValue;
import com.orange.analysis.anasoot.result.OrValue;
import com.orange.analysis.anasoot.result.StringValue;
import com.orange.matos.core.Out;

/**
 * Utility class handling points-to set as given back by Soot.
 * @author piac6784
 *
 */
public class P2SAux {
	static boolean debug = false;
	
	/**
	 * Placeholder 
	 */
	final public static NodeValue dummy_node_index = new NodeValue(-1, null);
	static Type stringType, stringBufferType, stringBuilderType;


	/**
	 * Init the class when the scene is correctly initialized.
	 * @param cc call context
	 * @param baa 
	 */
	public static void init (CallContext cc, ArrayAnalysis baa) { 	
		Scene s = Scene.v(); 
		stringType = s.getRefType("java.lang.String");
		stringBufferType = s.getRefType("java.lang.StringBuffer");
		stringBuilderType = s.getRefType("java.lang.StringBuilder");
		cc.nodeTable.init(baa); 
	}

	static class SpecialVisitor extends P2SetVisitor {
		final NodeTable cc;
		final ArrayList<AbsValue> r;

		SpecialVisitor(NodeTable c, ArrayList<AbsValue> l) {
			super (); cc = c; r = l;
		}
		
		boolean star_added = false;

		@Override
		public void visit (Node node) {
			if(node instanceof AllocNode) {
				int id = cc.add((AllocNode) node);
				NodeValue repr = new NodeValue (id, cc);
				r.add(repr);
			}
			else if (!star_added) {
				r.add(dummy_node_index);
				star_added=true;
			}
		}	
	}
	
	/**
	 * Check if a points to set is non empty.
	 * @param ptsRaw
	 * @param m
	 * @param obj
	 */
	public static void check (PointsToSet ptsRaw, SootMethod m, Unit st) {
		if (!(ptsRaw instanceof PointsToSetInternal)) return;
		PointsToSetInternal pts = (PointsToSetInternal) ptsRaw;
		if ((pts.size() != 0 && pts.getType() != null)) return;
		/*
		CallGraph cg = Scene.v().getCallGraph();
		System.out.println("**********" + st + "***************");
		showAncestor(cg,st, m,1 );
		*/
	}
	

	/*
	private static void showAncestor(CallGraph cg, Unit u, SootMethod m, int i) {
		Iterator <Edge> it = cg.edgesInto(m);
		PointsToAnalysis pag = Scene.v().getPointsToAnalysis();
		if (u instanceof Stmt) {
			Stmt st = (Stmt) u;
			if (st.containsInvokeExpr()) {
				InvokeExpr ie = st.getInvokeExpr();
				if (ie instanceof InstanceInvokeExpr) {
					Value b = ((InstanceInvokeExpr) ie).getBase();
					if (b instanceof Local) {
						Local l  = (Local) b;
						System.out.print(((PointsToSetInternal) pag.reachingObjects(l)).size() + "|");
					} else System.out.print("-|");
				}
				for (Object v : ie.getArgs()) {
					if (v instanceof Local) {
						Local l  = (Local) v;
						System.out.print(((PointsToSetInternal) pag.reachingObjects(l)).size() + ",");
					} else System.out.print("-,");
				}
			}
		}
		for(int j=0;j < i; j++) System.out.print("  ");
		System.out.println(m);
		while(it.hasNext()) {
			Edge e = it.next();
			showAncestor(cg, e.srcUnit(), e.src(), i+1);
		}
	}
	*/


	/**
	 * explore the contens of a points-to set and gives it back as an Anasoot abstract value.
	 * @param nt
	 * @param ptsarg
	 * @return
	 */
	public static AbsValue p2sContents (NodeTable nt, PointsToSet ptsarg) {
		final ArrayList<AbsValue> r = new ArrayList<AbsValue> ();
		if (ptsarg instanceof WrappedPointsToSet) {
			ptsarg = ((WrappedPointsToSet) ptsarg).getWrapped();
		}
		if (ptsarg instanceof PointsToSetInternal) {
			PointsToSetInternal pts = (PointsToSetInternal) ptsarg;
			pts.forall(new SpecialVisitor (nt, r));
			if (debug && pts.size() == 0) Out.getLog().println("Null PTS " + " " + pts.getType());
		} else if (ptsarg instanceof AllocAndContextSet) {
			AllocAndContextSet pts = (AllocAndContextSet) ptsarg;
			for (AllocAndContext ac : pts) nt.addWitness(ac,r);
		} else if (ptsarg instanceof FullObjectSet) {
			Out.getLog().println("FullObjectSet = no info");
		} else throw new RuntimeException("Unknown points-to-set " + ptsarg.getClass());
		return new OrValue(r);
	}

	/**
	 * Check that the type is simple (a primitive type, a string, a buffer
	 * @param t
	 * @return
	 */
	public static boolean is_simple (Type t) {
		return (t instanceof PrimType) 
		|| (t==stringType) 
		|| (t==stringBufferType)
		|| (t==stringBuilderType);
		// || ((t instanceof ArrayType) && is_simple(((ArrayType) t).getElementType()));
	}


	/**
	 * Very simple types are just primitive types or Strings but not buffers. 
	 * @param t
	 * @return
	 */
	public static boolean is_very_simple (Type t) {
		return (t instanceof PrimType) || (t==stringType);
	}

	/**
	 * Computes the possible strings contained in a points-to set. It should also
	 * take into  account non fixed strings contained.
	 * @param p the points-to set given back by soot
	 * @return a set of abstract value representing strings.
	 */
	static public Set<AbsValue> possibleStringConstantsSet(PointsToSet p) {
		final HashSet<AbsValue> ret = new HashSet<AbsValue>();
		if (p instanceof PointsToSetInternal) {
			((PointsToSetInternal) p).forall(new P2SetVisitor() {
				boolean first = true;
	
				@Override
				public final void visit(Node n) {
					if( n instanceof StringConstantNode ) {
						String contents = ((StringConstantNode) n).getString();
						ret.add(new StringValue(contents));
					} else if (first) {
						ret.add(MethodSpyAnalysis.star);
						first = false;
					}
				}});
		} else  if (p instanceof AllocAndContextSet){
			boolean first = true;
			for(AllocAndContext ac : (AllocAndContextSet) p) {
				if (ac.alloc instanceof StringConstantNode) {
					String contents = ((StringConstantNode) ac.alloc).getString();
					ret.add(new StringValue(contents));
				}else if (first) {
					ret.add(MethodSpyAnalysis.star);
					first = false;
				}
			}
		}
		return ret;
	}

}

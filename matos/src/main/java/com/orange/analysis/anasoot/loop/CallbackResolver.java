package com.orange.analysis.anasoot.loop;

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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.Local;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Value;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.spark.sets.EmptyPointsToSet;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.pointer.MemoryEfficientRasUnion;
import soot.jimple.toolkits.pointer.Union;

/**
 * @author Pierre Cregut
 * Gives back what is considered as a callback in the given technology
 * considered.
 */
public class CallbackResolver {
	private HashMap <SootMethod,Translation> table = new HashMap <SootMethod,Translation> ();
	private PointsToAnalysis ptAnalysis;
	private CallGraph callgraph;

	/**
	 * A LinkMethod class represents the fact that a given java method establishes a link between two objects,
	 * the first one being the container and the second being the contents. 
	 * @author Pierre Cregut
	 *
	 */
	public static class LinkMethod {
		final int fromIndex;
		final int toIndex;
		final SootMethod method;

		/** LinkMethod constructor 
		 * @param t the method creating the link.
		 * @param fromArg the position of the arg defining the container object
		 * @param toArg the position of the arg defining the contents object
		 */
		public LinkMethod (SootMethod t, int fromArg, int toArg) {
			fromIndex = fromArg; 
			toIndex = toArg;
			method = t; 
		}
	}

	/**
	 * @author Pierre Cregut
	 *
	 */
	public static class Translation {
		final int argument;
		final String target;
		Translation followup = null;
		final List <LinkMethod> linkThrough;
		final SootMethod caller;
		final String message;

		/** Translation object constructor */
		public Translation (SootMethod c, int a, String t, 
				List <LinkMethod> lt, String m) { 
			caller = c; argument = a; target = t;
			linkThrough = lt; 
			message = m;
		}
		/** Checks if there is a chain of objects linked by through methods */
		boolean isThrough() { return linkThrough != null; }
	}

	/**
	 * Simple constructor
	 */
	public CallbackResolver() {
		Scene scene = Scene.v();
		callgraph =  scene.getCallGraph();
		ptAnalysis = scene.getPointsToAnalysis();
	}

	/** Register a new translation in the global table that is
	 * searched each time a new method call is analysed */

	public void register(Translation translation) {
		SootMethod caller = translation.caller;
		if (table.containsKey(caller)) {
			Translation tr = (Translation) table.get(caller);
			while(tr.followup != null) tr = tr.followup;
			tr.followup = translation;
		} else table.put(caller, translation);
	}

	/** Given an invoke expression and a parameter index (0 for the
	 * base, strictly positive for others), gets the pointstoset
	 * abstracting the potential arguments of this call at the given
	 * parameter position
	 * @param ie the expression containing the invoke
	 * @param index the parameter position
	 * @return the pointstoset abstracting the argument contents. */
	private PointsToSet getPointsTo(InvokeExpr ie, int index) {
		Value arg; 
		if (index==0)
			arg = ((InstanceInvokeExpr) ie).getBase();
		else arg = ie.getArg(index-1);
		if (arg instanceof Local) 
			return ptAnalysis.reachingObjects((Local) arg);
		else return EmptyPointsToSet.v();
	}

	/** Checks whether an edge in the callgraph (a call in the
	 * program) corresponds to a call to a method that should be
	 * translated in the activation of a callback */
	public boolean isActive(Edge edge) {
		// This is a special kind of edge that cannot be taken recursively
		// Taking it would create stupid loops.
		if (edge.isClinit()) return false;
		Stmt stmt = edge.srcStmt();
		InvokeExpr ie = stmt.getInvokeExpr();
		SootMethod m = ie.getMethod();
		return table.containsKey(m);
	}

	/** Given an edge which is a call to a callback activator, gives
	 * back a map whose keys are the methods potentially used as
	 * callbacks and the associated value is an explanation message
	 * (stored in the configuration file) of the kind of callback
	 * resolution
	 *  @param e regularly called edge,
	 *  @return a map from potential methods to explanations.
	 */
	public Map <SootMethod,String> resolve(Edge e) { // [SootMethod] Set
		InvokeExpr ie = e.srcStmt().getInvokeExpr();
		SootMethod m = ie.getMethod();
		Translation trans = (Translation) table.get(m);
		// Ok there is no translation registered, tell to keep this 
		// method call as is.
		if(trans==null) return null;
		HashMap  <SootMethod,String> result = new HashMap  <SootMethod,String> ();
		do {
			PointsToSet ptArg = getPointsTo(ie,trans.argument);
			if (trans.linkThrough != null) {
				for(LinkMethod lm : trans.linkThrough) { 
					ptArg = resolveLinkThrough(ptArg,lm);
				}
			}
			Set <Type> types = ptArg.possibleTypes();
			for(Type t : types) {
				if (! (t instanceof RefType)) continue;
				SootClass c = ((RefType) t).getSootClass();
				SootMethod devirtualized = null;
				try {
					devirtualized = c.getMethod(trans.target);
				} catch (Exception exc) {
					System.out.println("Cannot find [" +  trans.target + "]");
				}
				if (devirtualized != null) 
					result.put(devirtualized, trans.message);
			}

			trans = trans.followup;
		} while (trans != null);
		return result;
	}

	/** 
	 * Auxiliary methods that taken a pointstoset representing a set
     * of objects origins and a method identifying as establishing a
     * link between origin objects and destination objects, gives
     * back an over-approximation of the set of destination methods
     * potentially linked to those origins objects 
	 * @param orig the set of origins abstracted as a pointstoset
	 * @param lm the link method represented by the name of the
	 * 	method and the position of the origin and destination
	 * 	parameters in a call.
	 * @return the destination objects abstracted as a pointstoset
	 */

	private PointsToSet resolveLinkThrough (PointsToSet orig, LinkMethod lm) {
		Iterator <Edge> ite = callgraph.edgesInto(lm.method);
		Union result = new MemoryEfficientRasUnion();
		while(ite.hasNext()) {
			Edge inedge =  ite.next();
			InvokeExpr ie = inedge.srcStmt().getInvokeExpr();
			if (ie != null) {
				PointsToSet ptFrom = getPointsTo(ie,lm.fromIndex);
				if(Union.hasNonEmptyIntersection(orig, ptFrom)) {
					PointsToSet ptTo = getPointsTo(ie,lm.toIndex);
					result.addAll(ptTo);
				}
			}
		}
		return result;
	}
}

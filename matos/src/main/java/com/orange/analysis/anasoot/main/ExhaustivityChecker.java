package com.orange.analysis.anasoot.main;

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

import java.util.HashSet;
import java.util.List;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;

/**
 * This class is a debug class that checks that the application is reasonably
 * covered. The main goal is to point out callbacks that have probably been 
 * forgotten.
 * @author Pierre Cregut
 *
 */

public class ExhaustivityChecker {
	private boolean active = false;
	
	static class MethodCall {
		final SootMethod caller;
		final SootMethod callee;
		MethodCall(SootMethod caller, SootMethod callee){
			this.caller = caller;
			this.callee = callee;
		}
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof MethodCall)) return false;
			MethodCall mc = (MethodCall) o;
			return mc.caller.equals(caller) && mc.callee.equals(callee);
		}
		
		@Override
		public int hashCode() {
			return caller.hashCode() ^ callee.hashCode();
		}
		@Override
		public String toString() {
			return caller + " -> " + callee;
		}
	}
	
	CallGraph cg;
	final HashSet<SootMethod> orphanMethods = new HashSet<SootMethod>();
	final HashSet<MethodCall> unresolved = new HashSet<MethodCall>();
	
	
	
	/**
	 * Check if a method is accessible in the callgraph.
	 * @param m
	 */
	public void checkMethod(SootMethod m) {
	  if (!active) return;
		if (cg == null) {
			Scene scene = Scene.v();
			cg = scene.getCallGraph();
		}
		if (! cg.edgesInto(m).hasNext()) {
			orphanMethods.add(m);
		}
	}
	
	/**
	 * Check if a method call is accessible in the callgraph
	 * @param m caller method
	 * @param u unit containing the invoke
	 */
	public void checkCall(SootMethod m, Unit u) {
	  if (!active) return;
		if (cg == null) {
			Scene scene = Scene.v();
			cg = scene.getCallGraph();
		}

		if (! (u instanceof Stmt)) return;
		if (! ((Stmt) u).containsInvokeExpr()) return;
		if (! orphanMethods.contains(m) && ! cg.edgesOutOf(u).hasNext()) {
			unresolved.add(new MethodCall(m, ((Stmt) u).getInvokeExpr().getMethod()));
		}
	}
	
	/**
	 * Explore a class to find a definition of a method in system code.
	 * @param c the class.
	 * @param name
	 * @param args
	 */
	public void exploreClass(SootClass c, String name, List<?> args ) {
	  if (!active) return;
		for (SootClass itf : c.getInterfaces()) 
			checkInterface(itf, name, args);
		if (! c.isApplicationClass()) {
			// System.out.println("      " + c);
			if (c.declaresMethod(name,args)) System.out.println("  MAY BE " + c);
		}
		if (c.hasSuperclass()) exploreClass(c.getSuperclass(), name, args);		
	}
	
	/**
	 * Explore an interface to find a definition of a method in system code.
	 * @param c
	 * @param name
	 * @param args
	 */
	private void checkInterface(SootClass c, String name, List<?> args) {
	  if (!active) return;
		for (SootClass itf : c.getInterfaces()) 
			checkInterface(itf, name,args);
		if (! c.isApplicationClass()) {
			// System.out.println("      " + c);
			if (c.declaresMethod(name,args)) System.out.println("  MAY BE " + c);
		}
	}

	/**
	 * Dump the results of the analysis
	 */
	public void dump() {
		if (!active) return;
		System.out.println("=== OrphanMethods ===");
		for (SootMethod m : orphanMethods) {
			System.out.println(m);
			SootClass c = m.getDeclaringClass();
			String name = m.getName();
			List<?> args = m.getParameterTypes();
			exploreClass(c,name, args);
		}
		// System.out.println("=== Unresolved Calls ===");
		// for (MethodCall mc : unresolved) System.out.println(mc);
	}
}

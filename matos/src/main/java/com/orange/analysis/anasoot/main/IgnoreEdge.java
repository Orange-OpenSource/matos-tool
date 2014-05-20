/*
 * $Id: IgnoreEdge.java 2279 2013-12-11 14:45:44Z piac6784 $
 */
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
import java.util.Iterator;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.Stmt;

/** 
 * Contains names of classes that should not be part of the analysis. This
 * will simplify the callgraph.
 */
public class IgnoreEdge {
	private static String toIgnore [][] = {
		{"java.lang.StringBuffer","append"},
		{"java.lang.StringBuffer","toString"},
		{"java.io.PrintStream","print"}
	};
	HashSet <SootMethod> ignored;
	
	/**
	 * Computes the set of methods to ignore knowing their name statically tabulated.
	 * @param s the scene describing the analysis context (java classes)
	 */
	public IgnoreEdge (Scene s) {
		ignored = new HashSet <SootMethod>();
		for(int i=0; i < toIgnore.length; i++) {
			try {
				SootClass c = s.getSootClass(toIgnore[i][0]);
				Iterator <SootMethod> i_method = c.methodIterator();
				while (i_method.hasNext()) {
					SootMethod m = i_method.next();
					if (m.getName().equals (toIgnore[i][1])) ignored.add(m);
				}
			} catch(Exception e) {}
		}
	}
	
	/**
	 * Check if the method should be ignored.
	 * @param m a method
	 * @return true if it should be ignored
	 */
	public boolean test(SootMethod m) { return ignored.contains(m); }
	
	/**
	 * Test if we should consider the instruction as a useful method call
	 * @param r the statement to check
	 * @return true if a method call calling a method that should not be ignored
	 */
	public boolean callInstruction(Stmt r) {
		return r.containsInvokeExpr() && !test(r.getInvokeExpr().getMethod());
	}
	
}

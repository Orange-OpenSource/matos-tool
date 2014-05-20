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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import soot.Scene;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

/**
 * @author piac6784
 * 
 * Utility functions on callgraphs
 *
 */
public class CallGraphUtil {

	/**
	 * @author piac6784
	 * Specification of an edge visitor
	 */
	public interface Visitor {
		/**
		 * To visit a caller.
		 * @param e an edge of the callgraph
		 */
		public void visit(Edge e, Deque<SootMethod> callers);
	}
	
	private static void visitAncestors(CallGraph cg, Set<Edge> seen, Deque<SootMethod> stack, Visitor visitor, SootMethod m) {
		Iterator <Edge> it = cg.edgesInto(m);
		stack.push(m);
		while(it.hasNext()) {
			Edge e = it.next();
			if (seen.contains(e)) continue;
			seen.add(e);
			visitor.visit(e,stack);
			visitAncestors(cg, seen, stack, visitor, e.src());
		}
		stack.pop();
	}
	
	/**
	 * Visits all callers of a method. Does not loop if recursive and visit only once an edge.
	 * @param m
	 * @param visitor
	 */
	public static void visitAncestors(SootMethod m, Visitor visitor) {
		Deque<SootMethod> stack = new ArrayDeque<SootMethod> ();
		visitAncestors(Scene.v().getCallGraph(), new HashSet<Edge>(), stack, visitor, m);
	}

}

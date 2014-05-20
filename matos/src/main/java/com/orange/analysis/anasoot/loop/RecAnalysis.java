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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

/**
 * @author piac6784
 * Recursion analysis (part of the loop analysis on recursion)
 */
public class RecAnalysis {

	Tarjan <SootMethod> tarjAnalysis;

	/**
	 * Wrapper for the Soot callgraph for Tarjan.
	 * @author piac6784
	 *
	 */
	public static class CGraph implements Tarjan.Graph <SootMethod> {
		/**
		 * The number of methods in the scene.
		 */
		int size;
		/**
		 * Callgraph computed by Soot
		 */
		CallGraph cg;
		/**
		 * Callback resolver that extends method calls through for example threads.
		 */
		CallbackResolver cba;
		/**
		 * @param size the number of methods.
		 * @param cg the regular callgraph computed by Soot
		 * @param cba The callback resolver to use to cross thread calls for example
		 */
		CGraph(int size, CallGraph cg, CallbackResolver cba) { 
			this.size = size; this.cg = cg; this.cba = cba; 
		}

		@Override
		public Collection <SootMethod> neighbours(SootMethod obj) {
			SootMethod m = (SootMethod) obj;
			Set <SootMethod> result = new HashSet <SootMethod>();
			for (Iterator <Edge> it = cg.edgesOutOf(m); it.hasNext(); ) {
				Edge e = it.next();
				// Do not consider spurious loops enabled by clinit.
				if (e.isClinit()) continue;
				if ((e.tgt().getName().equals("<init>")) && cba.isActive(e)) {
					Map<SootMethod, String> ir = cba.resolve(e);
					if (ir != null) {
						result.addAll(ir.keySet());
					}
				}
				else result.add(e.getTgt().method());
			}

			return result;
		}

		@Override
		public int size() { return size; };
	}

	private final Set <SootMethod> allMethods;
	private final Tarjan.Graph <SootMethod> graph;
	/**
	 * Do a recursion analysis. We even look at recursive thread generations.
	 * @param cba The callback resolver for thread calls.
	 * @param cg The callgraph. 
	 */
	RecAnalysis(CallbackResolver cba, CallGraph cg) {
		int size = 0;
		Scene scene = Scene.v();
		allMethods = new HashSet <SootMethod>();
		
		for (SootClass cl : (Collection <SootClass>) scene.getClasses()) {
			for (SootMethod m : (Collection <SootMethod>) cl.getMethods()) {
					size++;
					allMethods.add(m);
			}
		}
		graph = new CGraph(size,cg,cba);
		tarjAnalysis = new Tarjan <SootMethod>(graph);
		
	}

	/**
	 * Analysis restricted to a method.
	 * @param m
	 */
	public void doAnalysis(SootMethod m) { tarjAnalysis.doAnalysis(m); }
	
	/**
	 * Check if a method is in a recursive loop
	 * @param m The method
	 * @return true if in a loop
	 */
	public boolean isRec(SootMethod m) { return tarjAnalysis.inLoop(m); }
	
	/**
	 * SCC id of a given method
	 * @param m
	 * @return
	 */
	public int component(SootMethod m) { return tarjAnalysis.component(m); }
	
	/**
	 * Other methods in the same SCC
	 * @param m method analysed
	 * @return the set of neighbour methods.
	 */
	public Collection <SootMethod> neighbours (SootMethod m) { return graph.neighbours(m); }

	/**
	 * Perform the analysis 
	 * @return recursive methods found.
	 */
	public Set<SootMethod> doAnalysis() {
		tarjAnalysis.doAnalysis(allMethods.iterator());
		Set <SootMethod>result = new HashSet<SootMethod>();
		for(SootMethod m : allMethods) {
			if (isRec(m)) result.add(m);
		}
		return result;
	}
	
}

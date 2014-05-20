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
import java.util.Iterator;

import soot.Unit;
import soot.jimple.Stmt;
import soot.toolkits.graph.UnitGraph;

/** This class makes an analysis that tags Invoke statements (method calls)
    that are part of a local loop (while or for loop mainly) in the body of
    the method. It uses the traditional dataflow analysis framework provided
    by Soot */

public class LoopAnalysis {
	
	/**
	 * The graph of the units (statements) of the method.
	 */

	static class UGraph implements Tarjan.Graph <Unit> {
		
		/**
		 * Wrap the unit graph to respect the interface.for Tarjan
		 */
		UnitGraph g;
		
		/**
		 * Constructor taking the unit graph to wrap.
		 * @param g
		 */
		UGraph(UnitGraph g) { this.g = g; }
		
		@Override
		public int size() { return g.size(); }
		
		@Override
		public Collection <Unit> neighbours(Unit s) {
			return g.getSuccsOf(s);
		}
	}

	/**
	 * Computes the components of the method and mark the call statements in a loop. 
	 * @param ug The graph of the method to analyse
	 */

	LoopAnalysis(UnitGraph ug) {
		Tarjan <Unit> tarjAnalysis = new Tarjan <Unit> (new UGraph(ug));
		tarjAnalysis.doAnalysis(ug.iterator());
		for (Iterator <Unit> it = ug.iterator(); it.hasNext(); ) {
			Stmt stmt = (Stmt) it.next();
			if (stmt.containsInvokeExpr() && tarjAnalysis.inLoop(stmt))
				stmt.addTag(new LoopTag());
		}
	}

}

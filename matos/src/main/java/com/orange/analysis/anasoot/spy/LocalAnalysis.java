/*
 * $Id: LocalAnalysis.java 2279 2013-12-11 14:45:44Z Pierre Cregut $
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

import java.util.List;

import soot.Local;
import soot.Unit;
import soot.jimple.JimpleBody;
import soot.toolkits.graph.CompleteUnitGraph;
import soot.toolkits.scalar.LocalDefs;
import soot.toolkits.scalar.LocalUses;
import soot.toolkits.scalar.SimpleLocalDefs;
import soot.toolkits.scalar.SimpleLocalUses;
import soot.toolkits.scalar.UnitValueBoxPair;

/**
 * Abstraction around the control flow graph for def use analysis in a 
 * method.
 * @author Pierre Cregut
 *
 */
public class LocalAnalysis {
	private LocalDefs defs;
	private LocalUses uses;
	private CompleteUnitGraph unitGraph;
	
	/**
	 * Constructed for a given method.
	 * @param jb the Jimple body of the method
	 */
	public LocalAnalysis(JimpleBody jb) {
		CompleteUnitGraph ug = new CompleteUnitGraph(jb);
		unitGraph  = ug;
		defs = new SimpleLocalDefs(ug);
		uses = new SimpleLocalUses(ug,defs);
	}
	
	
	/**
	 * Where the result of a statement is used.
	 * @param s statement of interest
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<UnitValueBoxPair> getUsesOf(Unit s) {
		return uses.getUsesOf(s);
	}
	/**
	 * Where a variable in a given statement is defined.
	 * @param l variable of interest.
	 * @param s statement of interest
	 * @return
	 */
	public List<Unit> getDefsOfAt(Local l, Unit s) {
		return defs.getDefsOfAt(l,s);
	}

	/**
	 * Assocated control flow graph
	 * @return
	 */
	public CompleteUnitGraph getUnitGraph() {
		return unitGraph;
	}
}


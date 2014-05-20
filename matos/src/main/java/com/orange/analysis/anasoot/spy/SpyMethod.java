/*
 * $Id: SpyMethod.java 2279 2013-12-11 14:45:44Z piac6784 $
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

import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeExpr;

import com.orange.matos.core.Alert;

/**
 * @author piac6784
 * Abstraction of a probe for method calls.
 */
public interface SpyMethod {
	/**
	 * Perform the analysis on a statement
	 * @param ad Analysis engine for the method
	 * @param ie the invoke expression in statement
	 * @param st statement
	 */
	public void spy(MethodSpyAnalysis ad, InvokeExpr ie, Unit st);
	

	/**
	 * Dumps out the result
	 * @param out output stream 
	 * @param xmlFormat choose between human readable or XML format 
	 * @throws Alert anything that goes wrong
	 */
	public void dump (PrintStream out, boolean xmlFormat) throws Alert;
	
	/**
	 * The method analysed
	 * @return
	 */
	public SootMethod getMethod ();
	
	/**
	 * The name of the rule.
	 * @return
	 */
	public String getName();
	
	/**
	 * Should it be printed in the result or is it intermediate internal.
	 * @return
	 */
	public boolean useful();
}

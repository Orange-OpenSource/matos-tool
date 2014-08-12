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

import soot.SootClass;
import soot.SootMethod;
import soot.Value;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;

/**
 * Description of a method call to analyze.
 * @author Pierre Cregut
 *
 */
public class CallDescription {
	/**
	 * The method called
	 */
	final SootMethod method;
	/**
	 * The class of teh method
	 */
	final SootClass clazz;
	
	/**
	 * Base argument for virtual calls
	 */
	final Value base;
	
	/**
	 * Other arguments 
	 */
	final List <Value> args;
	
	
	CallDescription(InvokeExpr ie) {
		method = ie.getMethod();
		clazz = method.getDeclaringClass();
		base = (ie instanceof InstanceInvokeExpr) ? ((InstanceInvokeExpr) ie).getBase() : null;
		args = getArgs(ie);
	}
	
	private static List <Value>getArgs(InvokeExpr ie) { return ie.getArgs(); }

}

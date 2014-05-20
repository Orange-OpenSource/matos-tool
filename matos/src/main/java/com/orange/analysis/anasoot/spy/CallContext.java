/*
 * $Id: CallContext.java 2279 2013-12-11 14:45:44Z piac6784 $
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

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import com.orange.analysis.anasoot.result.NodeTable;

import soot.SootField;
import soot.SootMethod;

/**
 * @author piac6784
 * Context of a call.
 */
public class CallContext {
	
	/**
	 * tables to register the entries for next iteration.
	 */
	public NodeTable nodeTable = new NodeTable();
	
	/**
	 * Work queue for the next iteration of argument analysis rules 
	 */
	public Hashtable<String, SpyMethod> registered_call = new Hashtable<String, SpyMethod> ();
	/**
	 * Work queue for the next iteration of fields rules
	 */
	public Hashtable<SootField, SpyField> registered_field = new Hashtable<SootField, SpyField> ();
	/**
	 * Work queue for the next iteration of result rules.
	 */
	public Hashtable<SootMethod, SpyReturn> registered_return = new Hashtable<SootMethod, SpyReturn> ();
	
	/**
	 * Table to remember the argument analysis rules already computed. 
	 */
	public Map <String,SpyMethod> doublon = new HashMap<String, SpyMethod> ();
	/**
	 * Table to remember the field rules already computed 
	 */
	public Map <String,SpyField> doublon_field = new HashMap<String, SpyField> ();
	/**
	 * Table to remember the return rules already computed 
	 */
	public Map <String,SpyReturn> doublon_return = new HashMap<String, SpyReturn> ();
	
	/**
	 * Counter used for rule naming.
	 */
	public int count = 0;


	/**
	 * Regiter an argument analysis rule for the next iteration
	 * @param name
	 * @param def
	 */
	public void register(String name, SpyMethod def) {
		registered_call.put(name,def);
	}
	
	/**
	 * Regiter a field rule for the next iteration
	 * @param field field whose contents is analysed
	 * @param def Definition of the rule
	 */
	public void registerField(SootField field, SpyField def) {
		registered_field.put(field,def);
	}
	
	
	/**
	 * Regiter an argument a returned result rule for the next iteration
	 * @param method method whose results are analysed.
	 * @param def Definition of the rule
	 */
	public void registerReturn(SootMethod method, SpyReturn def) {
		registered_return.put(method,def);
	}

}

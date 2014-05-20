package com.orange.analysis.headers;

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

import java.util.regex.Pattern;

import com.orange.matos.core.Alert;

/** 
 * Simple attribute in the manifest. 
 */
public class SimpleAttrChecker extends AttributeChecker {

	String injar,injad;
	String name;
	String regexp = null;
	
	/** 
	 * Generic constructor for building an attribute recognizer
	 * @param name the name of the attribute 
	 */
	public SimpleAttrChecker(String name){
		this.name = name;
		reset();
	}
	
	/** 
	 * Constructor used when we want to check that the value respect a given
	 * pattern
	 * @param name the name of the attribute
	 * @param regexp the regular expression to check the value is well-formed
	 */
	public SimpleAttrChecker(String name, String regexp) {
		this.name = name;
		this.regexp = regexp;
		reset();
	}
	
	/** 
	 * get the name of the attribute 
	 */
	@Override
	public String getName() {
		return name;
	}

	/** 
	 * Function called when the attribute is defined in the JAR. By default
	 * it checks if there is a double definition and if the value is
	 * correct (if a regular expression was registered)
	 * @param key value for the key
	 * @param val the value of the parameter
	 */
	@Override
	public boolean inJar(String key, String val) {
		if (regexp != null && !Pattern.matches(regexp,val)) {
			addProblem("Incorrect value for attribute(s) in JAR manifest:", name);
		}
		injar = val;
		return true;
	}

	/** 
	 * Function called when the attribute is defined in the JAD. By default
	 * it checks if there is a double definition and if the value is
	 * correct (if a regular expression was registered)
	 * @param key value of the key
	 * @param val the value of the parameter
	 */
	@Override
	public boolean inJad(String key, String val) {
		if (regexp != null && !Pattern.matches(regexp,val)) {
			addProblem("Incorrect value for attribute(s) in JAD descriptor:", name);
		}
		injad = val;
		return true;
	}

	/** 
	 * Function called to check that the attribute has been correctly
	 * defined somewhere in the JAR or JAD. By default returns true.
	 */
	@Override
	public void check() throws Alert {}

	@Override
	public void reset() {
		injar = null;
		injad = null;
	}

	@Override
	public void checkTrusted() {
		if ((injad!=null) && (injar!=null)) {
			if (!injad.equals(injar)) {
				addProblem("Values differ from the JAR Manifest to the JAD file:", name);
			}
		}
	}

	@Override
	public String getAttributeName() {
		return "Simple Attribute " + getName();
	}

}

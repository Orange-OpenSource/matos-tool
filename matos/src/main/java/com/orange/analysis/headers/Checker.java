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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.orange.matos.core.Alert;

/**
 * @author piac6784
 * Generic representation of a check on headers of JAD or Manifest file
 */
public abstract class Checker {
	
	/**
	 * Identify the manifest.
	 */
	public final static String JAR_TYPE = "JAR manifest";
	/**
	 * Identify the JAD descriptor.
	 */
	public final static String JAD_TYPE = "JAD";

	/**
	 * List of problem messages to print
	 */
	public HashMap<String, Set<String>> problemMessage = new HashMap<String, Set<String>>();
	/**
	 * List of warning messages to print
	 */
	public HashMap<String, Set<String>> warningMessage = new HashMap<String, Set<String>>();
	/**
	 * List of ok messages to print
	 */
	public HashMap<String, Set<String>> okMessage = new HashMap<String, Set<String>>();
	
	/**
	 * Verdict of checker
	 */
	protected boolean verdict = true;
	
	/**
	 * The verificatio function. Will generate verdict and problems.
	 * @throws Alert
	 */
	public abstract void check() throws Alert;
	
	/**
	 * The name of attribute being checked.
	 * @return
	 */
	public abstract String getAttributeName();
	
	/**
	 * Get the verdict.
	 * @return
	 */
	public boolean getVerdict(){
		return verdict;
	}
	
	/**
	 * Add a new problem to the list
	 * @param title message
	 * @param attributeName name of attribute
	 */
	public void addProblem(String title, String attributeName){
		if (problemMessage.containsKey(title)) {
			Set<String> attributes = problemMessage.get(title);
			if (attributeName != null && attributeName.length() > 0) attributes.add(attributeName);
		} else {
			Set<String> attributes = new HashSet<String>();
			if (attributeName != null && attributeName.length() > 0) attributes.add(attributeName);
			problemMessage.put(title, attributes );
		}
		verdict = false;
	}

	/**
	 * Add a new warning to the list
	 * @param title message
	 * @param attributeName name of attribute
	 */
	public void addWarning(String title, String attributeName){
		if (warningMessage.containsKey(title)) {
			Set<String> attributes = warningMessage.get(title);
			attributes.add(attributeName);
		} else {
			Set<String> attributes = new HashSet<String>();
			attributes.add(attributeName);
			warningMessage.put(title, attributes );
		}
	}

}

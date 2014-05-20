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

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import com.orange.matos.core.Alert;

/**
 * Check whether a property is defined in the JAD file and is not defined in the Jar's Manifest.
 * If a such case occurs, generate a warning. 
 * Exceptions exists (like MIDlet-Jar-URL that MUST be in JAD)
 * It is a kind of RegexpAttributeChecker with a particular regexp that match all of property names,
 * and have it's ownbehavior (inJad(..), inJar(..), check(), checkTrusted() overwritten)
 */
public class OnlyInJADAttrChecker extends RegexpAttributeChecker {

	private File jadFile;
	
	/** List of properties that are allowed to be in JAD and not in JAR*/
	private ArrayList<String> exceptions = new ArrayList<String>(); 
	
	/**
	 * @param jadFile
	 */
	public OnlyInJADAttrChecker(File jadFile) {
		super("[^\\s]+"); // any string having more than 1 char (anything else than space char, once or more)
		this.jadFile = jadFile;
		reset();
		
		// exceptions that MUST be in JAD
		exceptions.add("MIDlet-Jar-URL"); // MUST be in JAD, so can be in it and not in JAR Manifest
		exceptions.add("MIDlet-Jar-Size");// MUST be in JAD, so can be in it and not in JAR Manifest
		// Note : MIDlet-Name, MIDlet-Version, MIDlet-Vendor MUST be in JAD, but are not part of exceptions 
		// 		  since they MUST be duplicated in JAD and JAR Manifest (checked by MandJadJarAttrChecker), 
		//		  so they cannot be only in JAD!


		exceptions.add("MIDlet-Jar-RSA-SHA1");
		
	}

	// Overwritten in order to take group 0 from the match
	@Override
	public boolean inJar(String key, String val) {
		mtc.reset(key);
		if (mtc.matches()) {
			String ext = mtc.group(0);
			jarMap.put(ext,val);
			return true;
		}
		return false;
	}

	// Overwritten in order to take group 0 from the match
	@Override
	public boolean inJad(String key, String val) {
		mtc.reset(key);
		if (mtc.matches()) {
			String ext = mtc.group(0);
			jadMap.put(ext,val);
		}
		return false;
	}

	/** 
	 * Check whether a property is defined in the JAD file and is not defined in the Jar's Manifest.
	 * If a such case occurs, a warning is given.
	 */
	@Override
	public void check() throws Alert{
		if (jadFile!=null) {
			for (Map.Entry <String,String> e : jadMap.entrySet()) {
				String attributeName = (String)e.getKey();
				if ((!exceptions.contains(attributeName))&&(!jarMap.containsKey(attributeName))) { 
					// the property is not part od exceptions, is defined in the JAD and not in the JAR's Manifest
//					addWarning("Warning : attribute " + attributeName + 
//							   " is defined in the JAD, but not in the JAR Manifest.");
					addWarning("attribute(s) defined in the JAD, but not in the JAR Manifest:", attributeName);
				}
			}
		} else {
			// no problem since there is no jad file.
		}
	}

	// Overwritten in order to do nothing...
	@Override
	public void checkTrusted() {
		// Nothing specific to do here if trusted 
	}

	@Override
	public String getAttributeName() {
		return "Only in JAD Attribute " + getName();
	}
}

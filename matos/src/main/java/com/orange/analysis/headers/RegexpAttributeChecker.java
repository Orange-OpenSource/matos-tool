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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.orange.matos.core.Alert;

/** 
 * Checks an attribute whose content can be defined by a regular expression 
 */
public class RegexpAttributeChecker extends AttributeChecker {

	protected HashMap<String, String> jarMap,jadMap;
	protected Matcher mtc;
	private String regexp;
	
	/**
	 * @param regexp regular expression to check.
	 */
	public RegexpAttributeChecker(String regexp) {
		this.regexp = regexp;
		Pattern pat = Pattern.compile(regexp);
		mtc = pat.matcher("");
		reset();
	}
	
	@Override
	public String getName() {
		return regexp;
	}

	@Override
	public boolean inJar(String key, String val) {
		mtc.reset(key);
		if (mtc.matches()) {
			String ext = mtc.group(1);
			if (jarMap.put(ext,val) != null) {
				addProblem("Double definition of attribute(s) in JAR manifest:", key);
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean inJad(String key, String val) {
		mtc.reset(key);
		if (mtc.matches()) {
			String ext = mtc.group(1);
			if (jadMap.put(ext,val) != null) {
				addProblem("Double definition of attribute(s) in JAD decriptor:", key);
			}
			return true;
		}
		return false;
	}

	@Override
	public void check() throws Alert {}

	@Override
	public void reset() {
		jarMap = new HashMap<String, String>();
		jadMap = new HashMap<String, String>();
	}

	@Override
	public void checkTrusted() {
		for (Map.Entry <String,String> e :  jadMap.entrySet()){
			String attributeName = e.getKey();
			String attributeValue = e.getValue();
			if (jarMap.containsKey(attributeName) && !attributeValue.equals(jarMap.get(attributeName))){
				addProblem("Values differ from the JAR Manifest to the JAD file:", attributeName);
			}
		}		
	}

	@Override
	public String getAttributeName() {
		return "Regexp attribute " + getName();
	}
}

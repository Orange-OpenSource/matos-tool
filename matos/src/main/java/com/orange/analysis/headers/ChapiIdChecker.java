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

import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.orange.matos.core.Alert;
import com.orange.matos.core.Configuration;

/**
 * @author Pierre Cregut
 * Check the ids used in attributes describing the use of CHAPI (Content
 * Handler API). The expected format is described by a regular expression
 * specified in the profile (descriptor.chapiIdRegexp) 
 */
public class ChapiIdChecker extends RegexpAttributeChecker {
	
	private final static String KEY_CHAPIID_REGEXP = "descriptor.chapiIdRegexp";
	final private Configuration config;
	
	ChapiIdChecker(Configuration config) { 
		super("MicroEdition-Handler-([0-9][0-9]?)-ID");
		this.config = config;
	}
	
	@Override
	public void check() throws Alert{
		String chapiIdRegexpSpec = config.string(KEY_CHAPIID_REGEXP);
		Pattern chapiIdRegexp = null;
		if (chapiIdRegexpSpec != null && chapiIdRegexpSpec.length() > 0) {
			chapiIdRegexp = Pattern.compile(chapiIdRegexpSpec);
			for(Entry<String,String> e : jadMap.entrySet()) {
				if(!chapiIdRegexp.matcher(e.getValue()).matches()) {
					addProblem("Identifier '" + e.getValue() + "' for content-handler " + e.getKey() 
							   + " in the JAD does not respect the imposed constaints.","");
				}
			}
			for(Entry<String,String> e : jarMap.entrySet()) {
				if(!chapiIdRegexp.matcher(e.getValue()).matches()) {
					addProblem("Identifier '" + e.getValue() + "' for content-handler " + e.getKey() 
							   + " in the JAR manifest does not respect the imposed constaints.","");
				}
			}
		}
	}
}

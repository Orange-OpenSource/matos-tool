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

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import com.orange.matos.core.Alert;
import com.orange.matos.core.Configuration;
import com.orange.matos.java.MidletKind;

/**
 * This checker checks the static declarations of content-handlers as defined by JSR 211.
 * It checks that the content-handler are existing midlets in the JAR.
 * It uses an auxiliary checker ChapiIdChecker.
 * It relies on the following options: 
 * <ul>
 * <li> descriptor.chapiContentTypesRegexp defines an optional constraint on the allowed
 * content-types.
 * <li> descriptor.chapiActionsRegexp defines an optional constraint on the allowed actions.
 * <li> descriptor.chapiSuffixesRegexp defines an optional constraint on the allowed suffixes.
 * <li> descriptor.mandatoryChapiId defines if every handler must have an explicit ID.
 * <li> descriptor.chapiIdRegexp defines an optional constraint on the chapi id used to
 * retrieve the handler.
 * </ul>
 * @author piac6784
 *
 */
public class ChapiChecker extends RegexpAttributeChecker {
	final private Configuration config;
	final private ChapiIdChecker chapiIdChecker;
	
	ChapiChecker(Configuration config, ChapiIdChecker chapiId) { 
		super("MicroEdition-Handler-([0-9][0-9]?)");
		this.config = config;
		this.chapiIdChecker = chapiId;
	}
	
	private static class ChapiDescr {
		final String classname;
		final String [] contentTypes;
		final String [] suffixes;
		final String [] actions;
		@SuppressWarnings("unused")
		final boolean spuriousFields;
		ChapiDescr(String value) {
			String [] components = value.trim().split("[ \t]*,[ \t]*");
			classname = (components.length > 0) ? components[0] : null;
			contentTypes = (components.length > 1) ? components[1].split("[ ]*") : null;
			suffixes = (components.length > 2) ? components[2].split("[ ]*") : null;
			actions = (components.length > 3) ? components[3].split("[ ]*") : null;
			if (contentTypes != null) Arrays.sort(contentTypes);
			if (suffixes != null) Arrays.sort(suffixes);
			if (actions != null) Arrays.sort(actions);
			spuriousFields = (components.length > 4);
		}
	}

	private class ChapiLineChecker {
		final Pattern contentTypesRegexp;
		final Pattern suffixesRegexp;
		final Pattern actionsRegexp;
		ChapiLineChecker() {
			String contentTypesRegexpDesc = config.string("descriptor.chapiContentTypesRegexp");
			String actionsRegexpDesc = config.string("descriptor.chapiActionsRegexp");
			String suffixesRegexpDesc = config.string("descriptor.chapiSuffixesRegexp");
			contentTypesRegexp = (contentTypesRegexpDesc != null) ? Pattern.compile(contentTypesRegexpDesc) : null;
			actionsRegexp = (actionsRegexpDesc != null) ? Pattern.compile(actionsRegexpDesc) : null;
			suffixesRegexp = (suffixesRegexpDesc != null) ? Pattern.compile(suffixesRegexpDesc) : null;	
		}
		
		public void check(ChapiDescr value, String key) {
			if (actionsRegexp != null && value.actions != null) {
				for(String action: value.actions) {
					if(! actionsRegexp.matcher(action).matches()) {
						addProblem("Action '" + action + "' for content-handler " + key + " does not respect imposed constraints.", "");
					}
				}
			}
			if (suffixesRegexp != null && value.suffixes != null) {
				for(String suffix: value.suffixes) {
					if(! suffixesRegexp.matcher(suffix).matches()) {
						addProblem("Suffix '" + suffix + "' for content-handler " + key + " does not respect imposed constraints.", "");
					}
				}
			}
			if (contentTypesRegexp != null && value.contentTypes != null) {
				for(String contentType: value.contentTypes) {
					if(! contentTypesRegexp.matcher(contentType).matches()) {
						addProblem("Content-type description '" + contentType + "' for content-handler " + key + " does not respect imposed constraints.", "");
					}
				}
			}
		}
	}
	
	@Override
	public void check() throws Alert{
		List <Object> midlets = config.getAppInfo("*");
		boolean mandatoryId = config.bool("descriptor.mandatoryChapiId");
		ChapiLineChecker lineChecker = new ChapiLineChecker();
		
		int i = 1;
		String key = String.valueOf(i);
			
		while(jadMap.containsKey(key) || jarMap.containsKey(key)) {
			ChapiDescr jadValue = parse(i, "JAD descriptor", (String) jadMap.get(key), midlets);
			ChapiDescr jarValue = parse(i, "JAR manifest", (String) jarMap.get(key), midlets);
			if (jarValue != null && jadValue != null) {
				checkSameFields(jarValue,jadValue,i);
			}
			ChapiDescr value = (jarValue != null) ? jarValue : jadValue;
			MidletKind.addKind(config, value.classname, "push handler", i);
			lineChecker.check(value, key);
			if (mandatoryId) {
				if (jarValue != null && !chapiIdChecker.jarMap.containsKey(key)) {
					addProblem("No identifier for Content-handler entry " + key + " in the JAR Manifest.","");
				}
				if (jadValue != null && !chapiIdChecker.jadMap.containsKey(key)) {
					addProblem("No identifier for Content-handler entry " + key + " in the JAD.","");
				}
			}
			
			
			jadMap.remove(key);
			jarMap.remove(key);
			key = String.valueOf(++i);
		}
		
		if (jarMap.size() != 0) {
			addProblem("There are gaps in the numbering of chapi handlers in the JAR manifest. The following values are incorrect indices: " +	jarMap.keySet(), "");
		}
		
		if (jadMap.size() != 0) {
			addProblem("There are gaps in the numbering of chapi handlers in the JAD descriptor. The following values are incorrect indices: " + jarMap.keySet(), "");
		}
	}
	
	private void checkSameFields(ChapiDescr jarValue, ChapiDescr jadValue, int i) {
		if (!jarValue.classname.equals(jadValue.classname)) {
			addProblem("Class name for handler " + i + " differs from the JAR Manifest to the JAD file.", "");
		}
		if (!Arrays.equals(jarValue.contentTypes, jadValue.contentTypes)) {
			addProblem("Content-types handled by Chapi Handler " + i + " differ from the JAR Manifest to the JAD file.", "");
		}
		if (!Arrays.equals(jarValue.suffixes, jadValue.suffixes)) {
			addProblem("Suffixes handled by Chapi Handler " + i +	" differ from the JAR Manifest to the JAD file.", "");
		}
		if (!Arrays.equals(jarValue.actions, jadValue.actions)) {
			addProblem("Actions handled by Chapi Handler " + i +	" differ from the JAR Manifest to the JAD file.", "");
		}
	}

	private ChapiDescr parse(int i, String from, String line, List<Object> midlets) {
		if (line == null) return null;
		ChapiDescr cd = new ChapiDescr(line);
		if (cd.classname == null || cd.classname.length()==0) {
			addProblem("Midlet for content-handler " + i + " in " + from + " is not defined.","");
		}
		if (!midlets.contains(cd.classname)) {
			addProblem("Class " + cd.classname + " is registered for content-handler " + i + " but is not defined as a midlet in the suite.","");
		}
		return cd;
	}
}

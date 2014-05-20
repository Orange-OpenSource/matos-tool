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

import java.util.List;
import java.util.regex.Pattern;

import com.orange.matos.core.Alert;
import com.orange.matos.core.Configuration;
import com.orange.matos.java.MidletKind;

/**
 * This checker checks the conformity of static registration of Push handlers.
 * It checks that the classes used are effective midlets in the JAR file and that
 * the numbering of registrations is correct.
 * It relies on the following options:
 * <ul>
 * <li> descriptor.pushURLRegexp optional constraint on the URL accepted for push actions
 * <li> descriptor.restrictionRegexp optional constraint on the restrictions accepted.
 * </ul>
 * @author piac6784
 *
 */
public class PushRegistryChecker extends RegexpAttributeChecker {
	final private Configuration config;

	
	PushRegistryChecker(Configuration config) { 
		super("MIDlet-Push-([0-9][0-9]?)");
		this.config = config;
	}
	
	private static class PushDescr {
		final String url;
		final String restriction;
		final String classname;
		@SuppressWarnings("unused")
		final boolean spuriousFields;
		
		PushDescr(String value) {
			String [] components = value.trim().split("[ \t]*,[ \t]*");
			url = (components.length > 0) ? components[0] : null;
			classname = (components.length > 1) ? components[1] : null;
			restriction= (components.length > 2) ? components[2] : null;
			spuriousFields = (components.length > 3);
		}
	}
	
	@Override
	public void check() throws Alert{
		List <Object> midlets = config.getAppInfo("*");
		String urlRegexpDesc = config.string("descriptor.pushURLRegexp");
		String restrictionRegexpDesc = config.string("descriptor.pushRestrictionRegexp");
		Pattern urlRegexp = null;
		Pattern restrictionRegexp = null;
		if (urlRegexpDesc != null) urlRegexp = Pattern.compile(urlRegexpDesc);
		if (restrictionRegexpDesc != null) restrictionRegexp = Pattern.compile(restrictionRegexpDesc);
		
		int i = 1;
		String key = String.valueOf(i);
		
		while(jadMap.containsKey(key) || jarMap.containsKey(key)) {
			PushDescr jadValue = parse(i, "JAD descriptor", (String) jadMap.get(key), midlets);
			PushDescr jarValue = parse(i, "JAR manifest",  (String) jarMap.get(key), midlets);
			if (jarValue != null && jadValue != null) {
				if (jarValue.classname != null && !jarValue.classname.equals(jadValue.classname)) {
					addProblem("Class name for push-registrar " + i + " differs from the JAR Manifest to the JAD file.", "");
				}
				if (jarValue.url != null && !jarValue.url.equals(jadValue.url)) {
					addProblem("URL for push-registrar " + i + " differ from the JAR Manifest to the JAD file.", "");
				}
				if (jarValue.restriction != null && !jarValue.restriction.equals(jadValue.restriction)) {
					addProblem("Restriction for push-registrar " + i +	" differ from the JAR Manifest to the JAD file.", "");
				}
			}
			PushDescr value = (jarValue != null) ? jarValue : jadValue;
			MidletKind.addKind(config, value.classname, "push registry", i);
			if (restrictionRegexp != null && value.restriction != null) {
				if(! restrictionRegexp.matcher(value.restriction).matches()) {
					addProblem("Restriction '" + value.restriction + "' for push-registrar " + key + " does not respect imposed constraints.", "");
				}
			}
			if (urlRegexp != null && value.url != null) {
				if(! urlRegexp.matcher(value.url).matches()) {
					addProblem("URL description '" + value.url + "' for push-registrar " + key + " does not respect imposed constraints.", "");
				}
			}
			
			jadMap.remove(key);
			jarMap.remove(key);
			key = String.valueOf(++i);
		}
		
		if (jarMap.size() != 0) {
			addProblem("There are gaps in the numbering of push-registrars in the JAR manifest. The following values are incorrect indices: " +	jarMap.keySet(), "");
		}
		
		if (jadMap.size() != 0) {
			addProblem("There are gaps in the numbering of push-registrars in the JAD descriptor. The following values are incorrect indices: " + jarMap.keySet(), "");
		}
	}
	
	private PushDescr parse(int i, String from,  String line, List<Object> midlets) {
		if (line == null) return null;
		PushDescr cd = new PushDescr(line);
		if (cd.classname == null || cd.classname.length()==0) {
			addProblem("Midlet for push-registrar " + i + " in " + from + " is not defined.","");
		}
		if (!midlets.contains(cd.classname)) {
			addProblem("Class " + cd.classname + " is registered for push-registrar " + i + " but is not defined as a midlet in the suite.","");
		}
		if (cd.url == null || cd.url.length()==0) {
			addProblem("URL for push-registrar " + i + " in " + from + " is not defined.","");
		}
		if (cd.restriction == null || cd.restriction.length()==0) {
			addProblem("Restriction for push-registrar " + i + " in " + from + " is not defined (use * for no constraint).","");
		}
		return cd;
	}
}

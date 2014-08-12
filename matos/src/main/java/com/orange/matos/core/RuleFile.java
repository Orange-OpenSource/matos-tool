/*
 * $Id: RuleFile.java 2279 2013-12-11 14:45:44Z Pierre Cregut $
 */
package com.orange.matos.core;

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

import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import org.w3c.dom.Element;


/** 
 * This class describes the contents of an analysis profile and
 * initializes the generic part of the XML parser that parses those files. 
 */
public class RuleFile {
	final static int HTML_OUTPUT = 0;
	final static int XML_OUTPUT = 1;
	final static int DEBUG_OUTPUT = 2;
	
	/**
	 * Name of rule file
	 */
	public String name;
	Properties properties;
	private XMLParser parser;
	
	/**
	 * Creates an XML representation for the rule file and
	 * parses the options
	 * @param file name of the rule file 
	 */	
	public RuleFile(String name, ProfileManager profileManager) throws Alert {
		properties = new Properties ();
		this.name = name;
		URL url = profileManager.getRuleResource(name);
		if (url == null) throw new Alert("Rule resource " + name + " not found."); 
		parser = new XMLParser(url);
		Element optionDefs [] = parser.getOptions();
		
		for(int i = 0; i < optionDefs.length; i++) {
			Element option = optionDefs[i];
			String optionName = option.getAttribute("name");
			String optionValue = option.getAttribute("value");
			properties.setProperty(optionName,optionValue);
		}
	}
	
	/** 
	 * Activation of the rule file. It means that the properties defined
	 * in the rule file as options overides the one defined in the
	 * configuration (warning: does not reset the configuration) 
	 */	
	public void activate(Configuration conf) {
		for (Enumeration <?> en= properties.propertyNames(); en.hasMoreElements();){
			String key = (String) en.nextElement();
			conf.setTransientProperty(key,properties.getProperty(key));
		}
	}

	/**
	 * Retrieves the name of the profile/rule file
	 * @return the profile name
	 */
	public String getProfileName() {
		return parser.getRootAttribute("name");
	}

	/**
	 * Retrieves the version of the profile/rule file
	 * @return the profile version
	 */
	public String getProfileVersion() {
		return parser.getRootAttribute("version");
	}

	/**
	 * Get the underlying parser
	 * @return
	 */
	public XMLParser getParser() {
		return parser;
	}

}

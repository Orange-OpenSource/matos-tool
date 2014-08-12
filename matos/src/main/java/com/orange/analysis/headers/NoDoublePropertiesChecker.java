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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Properties;

import com.orange.matos.core.Alert;

/**
 * Parse Properties File or InputStream, checking for double definition of attrubute.
 * The Propeties can be obtains using the getProperties() method (no double attribute)
 * @author Nicolas Moteau
 *
 */
public class NoDoublePropertiesChecker extends LineAnalyserChecker{
	 
	/** Internal Properties*/
	private Properties props = new Properties();

	/**
	 * Constructor for jar file.
	 * @param jarInputStream
	 */
	public NoDoublePropertiesChecker(InputStream jarInputStream) {
		inputStream = jarInputStream;
		fileType = JAR_TYPE;
	}	

	/**
	 * Constructor for jad file
	 * @param jadFile
	 * @throws FileNotFoundException
	 */
	public NoDoublePropertiesChecker(File jadFile) throws FileNotFoundException {
		fileType = JAD_TYPE;
		inputStream = new FileInputStream(jadFile);
	}

	@Override
	public void check() throws Alert{
		splitLines();
	}

	@Override
	protected boolean parseLine(String aLine, boolean verdict) {
		String line = aLine.trim();
		if (line.length()>0) { // not an empty line
			if ( (line.trim().startsWith("#")) 
				|| (line.trim().startsWith("!")) ) { // this is a comment line -> nothing to do
			} else {
				int index = line.indexOf(':');
				if (index==-1) { index = line.indexOf('='); }
				if (index==-1) { index = line.indexOf(' '); }
				if (index==-1) { index = line.indexOf('\t'); }
				if (index==-1) { index = line.indexOf('\f'); }
				
				if (index==-1) { /*invalid propline*/ 
				} else {
					String key = line.substring(0,index).trim();
					String value = line.substring(index+1,line.length()).trim();
					if (props.setProperty(key, value) != null){
						// key already present !
						addProblem("Double definition of attribute(s) in " + fileType + ":", key);
					}
				}
			}			
		}
		return verdict;
	}

	@Override
	public String getAttributeName() {
		return "Double Definition Checker";
	}

	
}

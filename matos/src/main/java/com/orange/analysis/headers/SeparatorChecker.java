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

import com.orange.matos.core.Alert;

/**
 * @author piac6784
 * Check that separators used in files are the one expected.
 * Depends if it is the jar or the jad. 
 */
public class SeparatorChecker extends LineAnalyserChecker{
	
	private String separator;
	
	
	/**
	 * Constructor for the jad.
	 * @param jadFile
	 * @throws FileNotFoundException
	 */
	public SeparatorChecker(File jadFile) throws FileNotFoundException {
		fileType = JAD_TYPE;
		inputStream = new FileInputStream(jadFile);
		separator = ":\u0020,:\t";
	}

	/**
	 * Constructor for the jar.
	 * @param jarInputStream
	 */
	public SeparatorChecker(InputStream jarInputStream) {
		fileType = JAR_TYPE;
		inputStream = jarInputStream;
		separator = ":";
	}

	@Override
	public void check() throws Alert {
		splitLines();
	}

	@Override
	protected boolean parseLine(String line, boolean verdict) {
		String [] separators = separator.split(",");
		if (line.length()>0) { // not an empty line
			if ((line.startsWith("#")) 
				|| (line.startsWith("!")) // this is a comment line -> nothing to do
				|| (line.startsWith("\u0020"))) { // in the jar Manifest this is the continuation of a line -> nothing to do
			} else {				
				int index = -1;
				for (int i=0; i<separators.length; i++){
					if (index==-1){
						index = line.indexOf(separators[i]);
					}
				}
				if (index==-1) { /*invalid separator*/ 
					if (verdict){
						addProblem("A separator used in "+fileType+" is invalid.", "");
					}
				}
			}			
		}
		return verdict;
	}

	@Override
	public String getAttributeName() {
		return "Separator Character";
	}

	
}

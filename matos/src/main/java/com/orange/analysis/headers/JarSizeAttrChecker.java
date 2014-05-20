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

/**
 * @author piac6784
 * Check that the indicated JAR size is correct.
 */
public class JarSizeAttrChecker extends MandJadAttrChecker {

	private File jarFile;
	
	/**
	 * Constructor. Must look at the real JAR file.
	 * @param jadFile
	 * @param jarFile
	 */
	public JarSizeAttrChecker(File jadFile, File jarFile) { 
		super("MIDlet-Jar-Size","[0-9]*", jadFile);
		this.jarFile = jarFile;
	}
	
	@Override
	public void check(){
		super.check();
		if(jarFile != null && injad != null) {
			String v1 = String.valueOf(jarFile.length());
			String v2 = injad;
			if (!v1.equals(v2)) {
				addProblem("The JAR file size (" + v1 +	") and the value indicated in the JAD file (" + v2 + ") differ.", "");
			}
		}
	}
	
	@Override
	public String getAttributeName() {
		return "JAR Size Attribute";
	}
	
}

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
 * Check attributes that should be present in the manifest OR in the jad 
 * descriptor.
 */
public class MandJadOrJarAttrChecker extends SimpleAttrChecker {

	private File jadFile;
	
	/**
	 * Constructor.
	 * @param name
	 * @param jadFile
	 */
	public MandJadOrJarAttrChecker(String name, File jadFile) {
		super(name);
		this.jadFile = jadFile;
	}
	
	@Override
	public void check(){
		if (jadFile==null) {
			if (injar == null) {
				addProblem("Mandatory attribute(s) is neither defined in the JAR Manifest nor in the JAD:", name);
			}

		} else {
			if (injar == null && injad == null) {
				addProblem("Mandatory attribute(s) is neither defined in the JAR Manifest nor in the JAD:", name);
			}
		}
	}
	
	@Override
	public String getAttributeName() {
		return "Mandatory JAD or JAR Attribute " + getName();
	}
}

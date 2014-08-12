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
 * @author Pierre Cregut
 * Should be present in both JAD and JAR and identical.
 */
public class MandJadJarIdentAttrChecker extends MandJadJarAttrChecker{

	/**
	 * JAD constructor
	 * @param name
	 * @param jadFile
	 */
	public MandJadJarIdentAttrChecker(String name, File jadFile) {
		super(name, jadFile);
	}
	
	/**
	 * JAR Manifest constructor
	 * @param name
	 * @param regexp
	 * @param jadFile
	 */
	public MandJadJarIdentAttrChecker(String name, String regexp, File jadFile){ 
		super(name, regexp, jadFile);
	}
	
	@Override
	public void check(){
		super.check();
		if ((injad!=null) && (injar!=null)) {
			if (!injad.equals(injar)) {
				addProblem("Values differ from the JAR Manifest to the JAD file:", name);
			}
		}
	}

	@Override
	public String getAttributeName() {
		return "Mandatory JAD and JAR Identical Attribute " + getName();
	}
}

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
 * Mandatory JAD checker. Some with a regexp as they can be parameterized.
 */
public class MandJadAttrChecker extends SimpleAttrChecker {
	
	private File jadFile;
	
	/**
	 * Constructor for a simple check.
	 * @param name
	 * @param jadFile
	 */
	public MandJadAttrChecker(String name, File jadFile){ 
		super(name);
		this.jadFile = jadFile;
	}
	
	/**
	 * Constructor for a regexp check
	 * @param name
	 * @param regexp
	 * @param jadFile
	 */
	public MandJadAttrChecker(String name, String regexp, File jadFile){ 
		super(name, regexp); 
		this.jadFile = jadFile;
	}
	
	@Override
	public void check(){
		if (jadFile != null && injad == null){
			addProblem("Mandatory attribute(s) must be defined in the JAD descriptor:", name);
		}
	}

	@Override
	public String getAttributeName() {
		return "Mandatory JAD Attribute " + getName();
	}
}

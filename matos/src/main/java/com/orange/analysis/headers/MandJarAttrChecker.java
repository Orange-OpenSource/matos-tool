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


/**
 * @author Pierre Cregut
 * An attribute mandatory in the manifest.
 */
public class MandJarAttrChecker extends SimpleAttrChecker {

	/**
	 * Constructor for simple attribute
	 * @param name
	 */
	public MandJarAttrChecker(String name){ 
		super(name); 
	}
	
	/**
	 * Constructor for parameterized attribute.
	 * @param name
	 * @param regexp
	 */
	public MandJarAttrChecker(String name, String regexp){ 
		super(name, regexp); 
	}
	
	@Override
	public void check() {
		if (injar == null) {
			addProblem("Mandatory attribute(s) must be defined in the JAR manifest:", name);
		}
	}
	
}

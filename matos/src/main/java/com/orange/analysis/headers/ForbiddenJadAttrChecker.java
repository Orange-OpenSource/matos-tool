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
 * Check that some attributes are not defined in the JAD
 */
public class ForbiddenJadAttrChecker extends SimpleAttrChecker {
	
	private File jadFile;
	
	/**
	 * Constructor.
	 * @param name
	 * @param jadFile
	 */
	public ForbiddenJadAttrChecker(String name, File jadFile){ 
		super(name);
		this.jadFile = jadFile;
	}
	
	@Override
	public void check(){
		if (jadFile != null && injad != null){
			addProblem("Attribute(s) should not be used in Jad file:", name);
		}
	}

}

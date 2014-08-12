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

import java.util.Properties;

/**
 * Common base class for all application descriptions whether they are midlet suite APK files
 * @author Pierre Cregut
 *
 */
public class AppDescription {
	final private Properties facts = new Properties();
	
	/**
	 * Register a set of facts usually the results of an analysis phase.
	 * @param properties
	 */
	public void learn(Properties properties) {
		facts.putAll(properties);
	}
	
	/**
	 * Get back a fact from the set.
	 * @param factName
	 * @return
	 */
	public Object getFact(String factName) {
		return facts.get(factName);
	}

	/**
	 * Get all the facts on the APK
	 * @return
	 */
	public Properties getFacts() {
		return facts;
	}
}

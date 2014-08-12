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

import com.orange.matos.core.Alert;

/**
 * @author Pierre Cregut
 * Check an attribute in the manifest or in the jar.
 */
public abstract class AttributeChecker extends Checker {

	/** 
     * get the name of the attribute 
     */
    public abstract String getName();

    /** 
     * Function called when the attribute is defined in the JAR. By default
     * it checks if there is a double definition and if the value is
     * correct (if a regular expression was registered)
     * @param key value of the key
     * @param val the value of the parameter
     * @return true if value treated
     */
    public abstract boolean inJar(String key, String val);

    /** 
     * Function called when the attribute is defined in the JAD. By default
     * it checks if there is a double definition and if the value is
     * correct (if a regular expression was registered)
     * @param key value of the key
     * @param val the value of the parameter
     * @return true if value treated
     */
    public abstract boolean inJad(String key, String val);
	
    /** 
     * Function called to check that the attribute has been correctly
     * defined somewhere in the JAR or JAD. By default does nothing.
     */
	@Override
	public abstract void check() throws Alert;
	
	/** 
     * Function called between each run to reinitialize the value 
     */    
    public abstract void reset();

	/**
	 * To apply when on a signed midlet.
	 */
	public abstract void checkTrusted();

}

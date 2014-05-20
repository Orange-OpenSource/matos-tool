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

import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

/**
 * Storted Properties 
 */
@SuppressWarnings("serial")
public class SortedProperties extends Properties {
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public synchronized Enumeration keys() {
		Enumeration<Object> keysEnum = super.keys();
		Vector <Comparable<Object>> keyList = new Vector<Comparable<Object>>();
		while(keysEnum.hasMoreElements()){
			keyList.add((Comparable<Object>) keysEnum.nextElement());
		}
		Collections.sort(keyList);
		return keyList.elements();
	}
}

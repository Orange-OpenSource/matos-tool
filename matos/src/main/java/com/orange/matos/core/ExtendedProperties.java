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
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

/**
 * @author Pierre Cregut
 * Properties with a backing origin. Used for differentiating properties from JAD or manifest.
 */
@SuppressWarnings("serial")
public class ExtendedProperties extends Properties {
	final private String origin;
	final private Hashtable<Object, String> fromFile;
	/**
	 * @param o
	 */
	public ExtendedProperties(String o) { 
		origin = o; 
		fromFile = new Hashtable<Object, String>(); 
	}

	@Override
	public Object setProperty(String key, String value) {
		fromFile.put(key,origin);
		return super.setProperty(key,value);
	}

	@Override
	public Object put(Object key, Object value) {
		fromFile.put(key,origin);
		return super.put(key,value);
	}

	@Override
	public void load(InputStream is) throws java.io.IOException {
		Properties local = new Properties();
		local.load(is);
		super.putAll(local);
		Enumeration<?> keys = local.keys();
		while(keys.hasMoreElements())
			fromFile.put(keys.nextElement(),origin);
	}
	@Override
	public void putAll(Map<?,?> t) {
		super.putAll(t);
		if(t instanceof ExtendedProperties)
			fromFile.putAll(((ExtendedProperties) t).fromFile);
	}

	/**
	 * @param key
	 * @return
	 */
	public String getOrigin(String key) {
		return (String) fromFile.get(key);
	}

	@Override
	public boolean equals(Object o) { return super.equals(o); }
	
	@Override
	public int hashCode() { return super.hashCode(); }
}

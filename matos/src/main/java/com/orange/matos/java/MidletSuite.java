package com.orange.matos.java;

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
import java.util.Properties;

import com.orange.matos.core.AppDescription;

/**
 * @author piac6784
 * A record respresenting a midlet suite
 */
public class MidletSuite extends AppDescription {
	/**
	 * JAD URL
	 */
	public String jadURL;
	/**
	 * JAR URL
	 */
	public String jarURL;
	/**
	 * Jar File (local)
	 */
	public File jarFile;
	/**
	 * Jad File (local)
	 */
	public File jadFile;
	/**
	 * Properties defined in the JAD.
	 */
	public Properties properties = new Properties();
}

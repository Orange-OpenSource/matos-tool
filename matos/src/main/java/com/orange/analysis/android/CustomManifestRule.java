package com.orange.analysis.android;

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

import com.orange.matos.android.APKDescr;

/**
 * @author piac6784
 * Interface of a custom checker on the manifest.
 */
public interface CustomManifestRule {
	
	/**
	 * The checker should explore the manifest and register something in the properties.
	 * Properties are a result for the analysis.
	 * @param manifest
	 * @param props
	 */
	void run(APKDescr manifest, Properties props);
	
}

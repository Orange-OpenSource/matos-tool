package com.orange.analysis.anasoot.profile.rules;

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

import java.io.IOException;

import com.orange.analysis.anasoot.AnasootConfig;
import com.orange.matos.core.Alert;
import com.orange.matos.core.RuleFile;

/**
 * @author piac6784
 * Description of analysis rules for an Android profile.
 * 
 */
public class AnaDroidRule extends AnajavaRule {
	/**
	 * Constructor
	 * @param rulefile a representation of the rule file
	 * @param profile name of the profile
	 * @param config configuration for Anasoot.
	 * @throws IOException
	 * @throws Alert
	 */
	public AnaDroidRule(RuleFile rulefile, String profile, AnasootConfig config) throws IOException, Alert{
		super(rulefile,"dalvik",profile, config);
	}

	@Override
	public String getConfiguration(){
		return "Android";
	}
	
	@Override
	public String getProfile() {
		if (profile.equals("android1.5")) return "CupCake (1.5)";
		if (profile.equals("android1.6")) return "Donut	  (1.6)";
		if (profile.equals("android2.1")) return "Eclair  (2.1)";
		if (profile.equals("android2.2")) return "Froyo	  (2.2)";
		if (profile.equals("android2.3")) return "Gingerbread	  (2.3)";
		if (profile.equals("android3.0")) return "HoneyComb	  (3.0)";
		return profile;
	}
}

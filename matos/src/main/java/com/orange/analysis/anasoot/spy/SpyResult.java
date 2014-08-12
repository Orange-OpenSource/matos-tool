package com.orange.analysis.anasoot.spy;

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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.orange.analysis.anasoot.arrayanalysis.ArrayAnalysis;
import com.orange.analysis.anasoot.result.NodeTable;

/**
 * Aggregated result of the analysis. 
 * @author Pierre Cregut
 *
 */
public class SpyResult {
	/**
	 * All the return rules used
	 */
	public final Map<String,SpyReturn> returns = new HashMap<String,SpyReturn>();
	/**
	 * all the call or use rules used 
	 */
	public final Map<String,SpyMethod> callUses = new HashMap<String, SpyMethod>();
	/**
	 * all the field use rules
	 */
	public final Map<String,SpyField> fields = new HashMap<String, SpyField>();
	/**
	 * All the java reports
	 */
	// public final Map<String,JavaReport> reports = new HashMap<String, JavaReport>();
	
	/**
	 * All the nodes in use
	 */
	public final NodeTable nodeTable;
	/**
	 * Array analysis
	 */
	public final ArrayAnalysis arrayAnalysis;
	
	/**
	 * Properties representing results of custom analysis.
	 */
	public final Properties customResults;

	/**
	 * Constructor for the result of the analysis.
	 * @param callUses result of probe uses
	 * @param returns result of probe returns
	 * @param fields result of probe fields
	 * @param reports result of probe method arguments
	 * @param nodeTable abstractions of data (nodes).
	 * @param baa Byte array analysis 
	 */
	public SpyResult(NodeTable nodeTable, ArrayAnalysis baa) {
		this.nodeTable = nodeTable;
		this.arrayAnalysis = baa;
		this.customResults = new Properties();
	}

	/**
	 * Add a checker on method calls if used
	 * @param call
	 */
	public void add(SpyMethod call) {
		String ruleName = call.getName();
		if (call.useful()) { 
			callUses.put(ruleName, call);
		}
	}
	
	/**
	 * Add a checker on fields if used
	 * @param fi
	 */
	public void add(SpyField fi) {
		String ruleName = fi.getName();
		if (fi.useful()) {
			fields.put(ruleName, fi);
		}	
	}
	
	/**
	 * Add a checker on returns if used
	 * @param ret
	 */
	public void add(SpyReturn ret) {
		String ruleName = ret.getName();
		if (ret.useful()) { 
			returns.put(ruleName, ret);
		}
	}
	

	/**
	 * Define a new custom result.
	 * @param key name of the result
	 * @param obj value.
	 * @return
	 */
	public boolean setCustomResult(String key, Object obj) {
		customResults.setProperty(key, obj.toString());
		return false;
	}
}

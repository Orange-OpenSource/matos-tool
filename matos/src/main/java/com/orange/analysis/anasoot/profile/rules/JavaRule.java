/*
 * $Id:Rule.java 917 2006-09-27 10:15:16 +0200 (mer., 27 sept. 2006) penaulau $
 */
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



import com.orange.analysis.anasoot.printing.JavaReport;

/**
 * Description of an analysis rule (generic).
 * @author piac6784
 *
 */
public abstract class JavaRule {
	
	/** The rule name. */
	final String ruleName;
	
	/** The report. */
	final JavaReport report;
	
	/**
	 * Instantiates a new java rule.
	 *
	 * @param ruleName the rule name
	 * @param report the report
	 */
	protected JavaRule(String ruleName, JavaReport report ) { this.ruleName = ruleName; this.report = report; }
	
	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	public String getName() { return ruleName; }
	
	/**
	 * Gets the report.
	 *
	 * @return the report
	 */
	public JavaReport getReport() { return report; }
	
}


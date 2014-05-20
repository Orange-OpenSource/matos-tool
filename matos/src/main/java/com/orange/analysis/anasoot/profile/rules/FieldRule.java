/**
 * 
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
 * The Class FieldRule.
 */
public class FieldRule extends JavaRule {
	
	/** The class name. */
	private final String className; 
	
	/** The field name. */
	private final String fieldName;
	
	/**
	 * Instantiates a new field rule.
	 *
	 * @param ruleName the rule name
	 * @param className the class name
	 * @param fieldName the field name
	 * @param report the report
	 */
	FieldRule(String ruleName, String className, String fieldName, JavaReport report) {
		super(ruleName,report);
		this.className = className;
		this.fieldName = fieldName;
	}

    /**
     * Gets the class name.
     *
     * @return the class name
     */
    public String getClassName() {
        return className;
    }

    /**
     * Gets the field name.
     *
     * @return the field name
     */
    public String getFieldName() {
        return fieldName;
    }
	
}
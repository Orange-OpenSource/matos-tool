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

import java.util.ArrayList;

import com.orange.analysis.anasoot.printing.JavaReport;

/**
 * The Class CallRule.
 *
 * @author Pierre Cregut
 */
public class CallRule extends JavaRule {
	
	/** The class name. */
	private final String className; 
	
	/** The method name. */
	private final String methodName;
	
	/** The position arg. */
	private final ArrayList <Integer> positionArg;
	
	/**
	 * Instantiates a new call rule.
	 *
	 * @param ruleName the rule name
	 * @param className the class name
	 * @param methodName the method name
	 * @param report the report
	 * @param positionArg the position arg
	 */
	CallRule(String ruleName, String className, String methodName, 
			JavaReport report, ArrayList <Integer> positionArg) {
		super(ruleName, report);
		this.className = className;
		this.methodName = methodName;
		this.positionArg = positionArg;
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
     * Gets the method name.
     *
     * @return the method name
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Gets the position arg.
     *
     * @return the position arg
     */
    public ArrayList <Integer> getPositionArg() {
        return positionArg;
    }
}
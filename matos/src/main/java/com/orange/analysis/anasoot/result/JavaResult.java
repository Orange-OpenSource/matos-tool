/*
 * $Id:Result.java 917 2006-09-27 10:15:16 +0200 (mer., 27 sept. 2006) penaulau $
 */
package com.orange.analysis.anasoot.result;

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

import soot.SootMethod;

/**
 * Represents the results of the midlet analysis.
 *
 */
public class JavaResult  implements Cloneable {
	/**
	 * The arguments infered (usually one)
	 */
	public AbsValue argument;
	/**
	 * The method called
	 */
	public final SootMethod method;
	/**
	 * The method that contained the invocation of the target method
	 */
	public final SootMethod method_orig;
	
	/**
	 * The bytecode offset of the call in the origin method.
	 */
	public final int offset_orig;
	/**
	 * A map of analysis specific tags with their value.
	 */
	public final HashMap<String, String> tags;
	
	/**
	 * A special tag used to uniquely identify the result.
	 */
	public static final String REF_TAG = "id";
	
	/**
	 * Hidden counter for generating unique labels.
	 */
	private static int counter = 0;
	
	/**
	 * Create a new java result
	 * @param approx the approximated value
	 * @param target the method targetted by the analysis
	 * @param origin the container method
	 * @param position the position of the invocation in the bytecode
	 */
	public JavaResult(AbsValue approx,  SootMethod target, SootMethod origin, int position) {
		argument = approx;
		method_orig = origin;
		method = target;
		tags = new HashMap<String, String>();
		offset_orig = position;
	}
	
	/**
	 * Simplify the abstract value.
	 * @param b
	 */
	public void normalize(boolean b) {
		argument = argument.normalize(b);
	}
	
	/**
	 * Adds a new tag for the xml printing of the result.
	 * @param key name of the tag
	 * @param v its value.
	 */
	public void addTag(String key, String v) {
		tags.put(key,v);
	}
	
	/**
	 * Gets the unique name of the result. Adds a tag if necessary.
	 * @return the unique ref
	 */
	public String getRef() {
		String tag = tags.get(REF_TAG);
		if (tag == null) {
			tag = "" + counter++;
			tags.put(REF_TAG, tag);
		}
		return tag;
	}

    /**
     * Copy the object.
     * @return
     */
    public JavaResult copy() {
        try { return (JavaResult) clone(); } catch (CloneNotSupportedException e) { throw new RuntimeException(e); } 
    }
}

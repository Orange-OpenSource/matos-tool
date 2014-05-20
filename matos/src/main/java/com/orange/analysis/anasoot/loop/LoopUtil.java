package com.orange.analysis.anasoot.loop;

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
import java.util.List;
import java.util.Map;

import soot.Hierarchy;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;


/**
 * @author piac6784
 * Set of utility functions
 */
public class LoopUtil {
    final static boolean debug = false;

    /**
     * Get the short coded as a tag of a host unit. 
     * @param u the unit
     * @param name the tag name
     * @return
     */
    public static short getTag(Unit u, String name) {
	try {
	    byte key [] = u.getTag(name).getValue();
	    short v = (short) (((int) key[1] & 0xff) +  
			       (((int) key[0] & 0xff) << 8));
	    return v;
	} catch (Exception e) { e.printStackTrace(); return -1;}
    }

    /**
     * Debug output.
     * @param s
     */
    public static void debug(String s) { if (debug) System.err.println(s); }

    /**
     * @param methods
     * @return
     */
    @SuppressWarnings("unchecked")
	public static Map<SootMethod, SootMethod> complete(Map <SootMethod, String> methods) {
	Map<SootMethod, SootMethod> result = new HashMap<SootMethod, SootMethod>();
	Scene sc = Scene.v();
	Hierarchy hierarchy = sc.getActiveHierarchy();
	for (SootMethod meth : methods.keySet()) {
	    if (meth.isConcrete()) result.put(meth,meth);
	    else {
		SootClass itf = meth.getDeclaringClass();
		List<SootClass> potClasses = hierarchy.getImplementersOf(itf);
		List <SootMethod> potMethods = 
		    hierarchy.resolveAbstractDispatch(potClasses,meth);
		for(SootMethod pot : potMethods)
		    result.put (pot, meth);
	    }
	}
	return result;
    }
}

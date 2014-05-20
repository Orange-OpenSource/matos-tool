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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Element;

import soot.Scene;
import soot.SootMethod;

import com.orange.analysis.anasoot.loop.CallbackResolver.LinkMethod;
import com.orange.analysis.anasoot.loop.CallbackResolver.Translation;
import com.orange.matos.core.RuleFile;
import com.orange.matos.core.XMLParser;

class LoopParser {
	private XMLParser parser;
	private Scene scene;
	private Element loopRoot [];
	
	LoopParser(RuleFile rf) {
		parser = rf.getParser();
		scene = Scene.v();
		loopRoot = parser.getKind("loop");
	}
	
	public boolean configured() {
		return (loopRoot != null) && (loopRoot.length > 0);
	}
	
	private static SootMethod getMethod(Scene sc, Element elt) {
		String classname = elt.getAttribute("class");
		String sig = elt.getAttribute("signature");
		return sc.getMethod("<" + classname + ": " + sig + ">");
	}

    Set<Translation> translations() {
	Set<Translation> translations = new HashSet<Translation>();
	for (int k=0; k<loopRoot.length;k++) {
	    Element elt_translation [] = 
		XMLParser.getElements(loopRoot[k], "translation");
	    for(int i=0; i<elt_translation.length; i++) {
		Element e = elt_translation[i];
		Element elt = parser.getElement(e, "caller");
		if (elt == null) continue;
		SootMethod caller = 
		    getMethod(scene, elt);
		elt = parser.getElement(e,"callee");
		if (elt == null) continue;
		String calleeSig = 
		    elt.getAttribute("signature");
		int index = Integer.parseInt(e.getAttribute("index"));
		String description = e.getAttribute("message");
		Element intermediates [] = XMLParser.getElements(e, "through");
		ArrayList <LinkMethod> through = null;
		if ((intermediates != null) && (intermediates.length != 0)) {
		    through = new ArrayList<LinkMethod> ();
		    for (Element eint : intermediates) {
			
			SootMethod methThrough =getMethod(scene,eint);
			int fromIndex = 
			    Integer.parseInt(eint.getAttribute("from"));
			int toIndex = 
			    Integer.parseInt(eint.getAttribute("to"));
			through.add(new LinkMethod(methThrough,fromIndex,toIndex));
		    }
		}
		Translation trans = 
		    new Translation(caller, index, calleeSig,
				    through, description);
		translations.add(trans);
	    }
	}
	return translations;
    }

    Map<SootMethod, String> getMethodsMap(String key) {
	Map<SootMethod, String> methods = new HashMap<SootMethod, String>();
	for (int j=0; j<loopRoot.length;j++) {
	    Element elt_methods [] = XMLParser.getElements(loopRoot[j],key);
	    for(int i=0; i < elt_methods.length; i++)
		methods.put(getMethod(scene,elt_methods[i]),
			    elt_methods[i].getAttribute("message"));
	}
	return methods;
    }

    Map<SootMethod, String> callbacks() { return getMethodsMap("callback"); }
    Map<SootMethod, String> criticals() { return getMethodsMap("critical"); }
}


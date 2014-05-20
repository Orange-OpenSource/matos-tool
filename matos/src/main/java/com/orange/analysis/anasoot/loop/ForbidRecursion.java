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

import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import soot.Scene;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;

import com.orange.matos.core.XMLStream;
import com.orange.matos.utils.HtmlOutput;

/**
 * @author piac6784
 * Implements a checker that issue a message as soon as recursion is used.
 */
public class ForbidRecursion {

	/**
	 * The callgraph of the midlet extracted by soot
	 */
	final CallGraph cg;
	
	/**
	 * The callback resolver that finds callbacks of interest (entry points from the AMS)
	 */
	final CallbackResolver cba;
	
	/**
	 * Analysis of recursion.
	 */
	final RecAnalysis recAnalysis;

	/** 
	 * Creates a new loop instance tailored for just forbidding the use
	 * of recursion
	 * @param cba a resolver of callback invocations. It gives back the list
	 * of potential callbacks (eg. an actual method for Runnable.run) 
	 * invoked when a callback invocation method is called (eg. Thread.start)
	 */
	public ForbidRecursion(CallbackResolver cba) { 
		Scene sc = Scene.v();
		this.cg = sc.getCallGraph();
		this.cba = cba; 
		recAnalysis = new RecAnalysis(cba,cg);
	}

	/**
	 * Perform and dumps the analysis
	 * @param out stream to write to.
	 * @param xmlFormat format of the output
	 */
	public void doAnalysis(PrintStream out, boolean xmlFormat) {
		Set <SootMethod> recursiveMethods = recAnalysis.doAnalysis();
		Map <Integer,Set<SootMethod>> map = new HashMap <Integer,Set<SootMethod>>();
		for (SootMethod m: recursiveMethods) {
			int i = recAnalysis.component(m);
			Set <SootMethod> cell = map.get(i);
			if (cell == null) {
				cell = new HashSet <SootMethod> ();
				map.put(i, cell);
			}
			cell.add(m);
		}
		if (xmlFormat) {
			XMLStream xmlout = new XMLStream(out);
			xmlout.element("recursive");
			for (Set <SootMethod> set : map.values()) {
				xmlout.element("component");
				for (SootMethod m : set) {
					if (!m.getDeclaringClass().isApplicationClass()) continue;
					xmlout.element("method");
					xmlout.attribute("signature", m.getSignature());
					xmlout.endElement();
				}
				xmlout.endElement();
			}
			xmlout.close();
		} else {
			out.println(HtmlOutput.header(1, "Recursive methods"));
			out.println("<ul>");
			int i = 1;
			for (Set <SootMethod> set : map.values()) {
				boolean skip = false;
				for (SootMethod m : set) {
					if (!m.getDeclaringClass().isApplicationClass()) {	skip = true; break;	}
				}
				if (skip) continue;
				out.println(HtmlOutput.header(4,"Component " + i++));
				out.println("<ul>");
				for (SootMethod m : set) {
					if (isRuntimeMethod(m)) continue;
					HashSet <SootMethod> dir = new HashSet<SootMethod> ();
					dir.addAll(set);
					dir.retainAll(recAnalysis.neighbours(m)); 
					out.print("<li>" + HtmlOutput.escape(m.getSignature()) + " =&gt; ");
					for(SootMethod n : dir) out.print(HtmlOutput.escape(n.getSignature()) + " ");
					out.println("</li>");
				}
				out.println("</ul>");
			}
			out.println("</ul>");
		}
	}

	private boolean isRuntimeMethod(SootMethod m) {
		String packageName = m.getDeclaringClass().getPackageName();
		return (packageName.startsWith("java.") || packageName.startsWith("javax") || packageName.startsWith("com.francetelecom.rd.fakemidp."));
	}
}

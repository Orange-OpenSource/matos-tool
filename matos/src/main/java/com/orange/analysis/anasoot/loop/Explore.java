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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import soot.Hierarchy;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.CompleteUnitGraph;

import com.orange.matos.utils.HtmlOutput;

/**
 * This class implements the exploration of the call graph to give arities to method (in fact only 0
 * 1 and many).
 * @author Pierre Cregut
 *
 */
public class Explore
{ 

	/** 
	 * A Step object represents a method used with a given arity. When a path
	 * has been found, there is no reason to look for a similar path 
	 * (but it also means that the method is not able to really count 
	 *  the number of occurencies). Only method loop and callEdge define the equality
	 *  This is also used to go through threads going from calls to start to calls to run. */

	static public class LoopStep {

		/**
		 * The method represented. 
		 */
		final public SootMethod method;
		/**
		 * Are we in a loop.
		 */
		final public boolean loop;
		/**
		 * Calling step
		 */
		final public LoopStep previousStep;
		/**
		 * The call edge with the instruction calling
		 */
		final public Edge callEdge;
		
		/**
		 * How we translate.
		 */
		final public String translationMessage;

		LoopStep(SootMethod m, boolean b, LoopStep s, Edge e, String t) {
			this.method = m; this.loop = b; 
			this.previousStep = s; this.callEdge = e;
			this.translationMessage = t;
		}
		
		@Override
		public boolean equals(Object el) {
			return ((el instanceof LoopStep) && 
					((LoopStep) el).callEdge == callEdge && 
					((LoopStep) el).method == method && 
					((LoopStep) el).loop == loop);
		}
		
		@Override
		public int hashCode() { return method.hashCode(); }

		@Override
		public String toString() {
			return (loop ? ("**" + method + "**") : ("--" + method + "--"));
		}
	}

	/**
	 *  Checks whether the invoke state making the call corresponding to the
	 *  edge is in a local loop (while, for) of the caller method.
	 *  @param e  the  edge
	 *  @return a boolean (true if it is in a loop, false otherwise) 
	 *  */
	public static boolean isLoop(Edge e) {
		return e.srcUnit().hasTag(LoopTag.name);
	}

	/**
	 * The callgraph of the midlet extracted by soot
	 */
	final CallGraph cg;
	
	/**
	 * The callback resolver that finds callbacks of interest (entry points from the AMS)
	 */
	final CallbackResolver cba;
	
	/**
	 * Class hierarchy graph.
	 */
	final Hierarchy hierarchy;
	
	/**
	 * Analysis of recursion.
	 */
	final RecAnalysis recAnalysis;

	/** 
	 * Creates a new loop exploration instance
	 * @param cba a resolver of callback invocations. It gives back the list
	 * of potential callbacks (eg. an actual method for Runnable.run) 
	 * invoked when a callback invocation method is called (eg. Thread.start)
	 */
	public Explore(CallbackResolver cba) { 
		Scene sc = Scene.v();
		this.cg = sc.getCallGraph();
		hierarchy = sc.getActiveHierarchy();
		this.cba = cba; 
		recAnalysis = new RecAnalysis(cba,cg);
	}

	/**
	 * Utility method giving back a readable string describing a method
	 * @param m a soot method
	 * @return the readable string with HTML syntax.
	 */
	private String readableMethodName(SootMethod m) {
		SootClass c = m.getDeclaringClass();
		try { 
			c.getMethodByName(m.getName());
			return HtmlOutput.escape(m.getName () +" of class "+ c.getName()) ;
		} 
		catch (RuntimeException e) {
			return HtmlOutput.escape(m.getSubSignature() + " of class " + 
					c.getName()) ;
		}
	}

	/**
	 * Do the analysis on a given callback. First we must tag the method in a recursion loop.
	 * @param method the callback method
 	 * @return gives back the set of steps.
	 */
	public Set <LoopStep> explore(SootMethod method) {
		Set <LoopStep> set = new HashSet <LoopStep> ();
		// Launch the recursive analysis on that callback
		recAnalysis.doAnalysis(method);
		explore(method, false, set, null, null, null);
		return set;
	}

	/**
	 * @param method the method under scrutiny
	 * @param globLoop is there a loop above (then we are in a loop).
	 * @param worklist what we still have to consider.
	 * @param prevStep previous step above in the callgraph
	 * @param prevEdge the call to this place
	 * @param prevMsg the corresponding message
	 */
	private void explore(SootMethod method, boolean globLoop, Set <LoopStep> worklist,
			LoopStep prevStep, Edge prevEdge, String prevMsg) {

		if(!method.hasTag(LoopTag.name) && method.hasActiveBody()) {
			new LoopAnalysis(new CompleteUnitGraph(method.getActiveBody()));
			// la.doAnalysis();
			method.addTag(new LoopTag());
		}

		boolean recLoop = recAnalysis.isRec(method);
		boolean currentLoop = globLoop || recLoop;
		LoopStep step = new LoopStep(method, currentLoop, prevStep, prevEdge, prevMsg);
		if (worklist.contains(step)) return;
		worklist.add(step);
		if (!method.getDeclaringClass().isApplicationClass()) return;
		for (Iterator <Edge> it = cg.edgesOutOf(method); it.hasNext(); ) {
			Edge edge = it.next();
			boolean localLoop = isLoop(edge);
			boolean nextLoop = globLoop || recLoop || localLoop;
			if ((edge.isClinit()) && cba.isActive(edge)) {
				Map <SootMethod,String> potential = cba.resolve(edge);
				if (potential != null) {
				for (Entry <SootMethod,String> entry : potential.entrySet()) {
					SootMethod called = entry.getKey();
					String transMsg = entry.getValue();
					explore(called, nextLoop, worklist, step, edge,transMsg);
				}
				}
			} else {
				SootMethod called = edge.getTgt().method();
				explore(called,nextLoop, worklist, step,edge, null);
			}
		}
	}

	/**
	 * Do the analysis for a given callback.
	 * @param out The stream to print to (output must be in HTML format)
	 * @param spec this is the specification of the callback we are looking at.
	 * @param criticals map translating an immediate method in top method
	 * @param msg_callbacks message to display for a given callback
	 * @param msg_criticals message to display for a given critical method
	 */
	@SuppressWarnings("unchecked")
	public void doAnalysis(PrintStream out, SootMethod spec, Map <SootMethod,SootMethod>criticals, 
			Map<SootMethod,String> msg_callbacks, Map<SootMethod,String> msg_criticals) {
		SootClass itf = spec.getDeclaringClass();
		List <SootClass> potClasses = hierarchy.getImplementersOf(itf);

		List <SootMethod> potMethods = hierarchy.resolveAbstractDispatch(potClasses,spec);
		// Iterates through all methods of all classes in the scene
		for (SootMethod m : potMethods) {
			Set <LoopStep> calledSet = explore(m);
			for (LoopStep called : calledSet) {
				if (criticals.containsKey(called.method)) {
					SootMethod critical = 
						(SootMethod) criticals.get(called.method);
					String msg_crit = (String) msg_criticals.get(critical);
					String msg_cbck = (String) msg_callbacks.get(spec);
					String mainMsg =
						HtmlOutput.header(3,"Call to " + 
								readableMethodName(critical)) +
								HtmlOutput.bold("Context of the call: ") + 
								readableMethodName(m) +
								"implementing the interface: " + itf +
								HtmlOutput.br() + HtmlOutput.bold("Caller purpose: ") +
								msg_cbck +
								HtmlOutput.br() + HtmlOutput.bold("Callee purpose: ") +
								msg_crit +
								HtmlOutput.br() +
								(called.loop
								 ? HtmlOutput.color("red", "There is a potential loop !")
								 : HtmlOutput.color("green", "Simple call (no loop)")) +
								   HtmlOutput.br() + HtmlOutput.bold("Call trace:");
					List <String> callMsgList = new ArrayList <String> ();
					while(called.previousStep != null) {
						String transMsg = 
							(called.translationMessage ==null) ? "" : (" [" + called.translationMessage + "]");
						boolean isLoop = 
							(called.callEdge!=null) && isLoop(called.callEdge);
						called = called.previousStep;
						boolean isRec = recAnalysis.isRec(called.method);
						String callMsg =
							"called by " + readableMethodName(called.method) + transMsg +
							(isRec? HtmlOutput.color("red"," which is recursive") : "") + 
							(isLoop? HtmlOutput.color("red"," in a local loop")	: "");
						callMsgList.add(callMsg);
					}
					out.println(mainMsg +  
							HtmlOutput.list(callMsgList.toArray()));
				}   
			}
		}
	}		
}

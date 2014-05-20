/*
 * $Id:RelSpy.java 917 2006-09-27 10:15:16 +0200 (mer., 27 sept. 2006) penaulau $
 */
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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import soot.Hierarchy;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.FieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.util.NumberedString;

import com.orange.analysis.anasoot.AnasootConfig;
import com.orange.analysis.anasoot.arrayanalysis.ArrayAnalysis;
import com.orange.analysis.anasoot.printing.JavaReport;
import com.orange.analysis.anasoot.printing.ReportUse;
import com.orange.analysis.anasoot.printing.StructureReport;
import com.orange.analysis.anasoot.profile.rules.AnajavaRule;
import com.orange.analysis.anasoot.profile.rules.CallRule;
import com.orange.analysis.anasoot.profile.rules.FieldRule;
import com.orange.analysis.anasoot.profile.rules.JavaRule;
import com.orange.analysis.anasoot.profile.rules.ReturnRule;
import com.orange.analysis.anasoot.profile.rules.UseRule;
import com.orange.matos.core.Alert;
import com.orange.matos.core.AppDescription;
import com.orange.matos.core.Out;

/**
 * The core of the analysis. It contains live tables representing the rules and calls the identified checker
 * on the elements of the code. 
 * @author piac6784
 *
 */
public class ProgramSpy {
	
	private final static String NEW_INSTANCE_SIGNATURE = "<java.lang.Class: java.lang.Object newInstance()>";
	
	Hashtable <Integer, List<SpyMethod>> spytable;
	
	final Scene scene;
	final Hierarchy hierarchy;
	
	AnajavaRule rulefile;
	
	private SpyResult result;
	
	/**
	 * Current argument rules
	 */
	public Hashtable <String,SpyMethod> call_tables;
	/**
	 * Current field rules
	 */
	public Hashtable <SootField,SpyField> field_tables;
	/**
	 * Current returned results rules.
	 */
	public Hashtable <SootMethod,SpyReturn> return_tables;
	
	
	/**
	 * all the arguments rules cumulated.
	 */
	Hashtable <String,SpyMethod> every_calls;
	/**
	 * all the field rules cumulated
	 */
	Hashtable<SootField, SpyField> every_fields;
	/**
	 * all the returned results rules cumulated 
	 */
	Hashtable <SootMethod,SpyReturn> every_returns;
	
	/**
	 * The argument analysis rules we start from
	 */
	Hashtable <String,SpyMethod> init_calls;
	/**
	 * The field rules we start from
	 */
	Hashtable <SootField,SpyField> init_fields;
	/**
	 * The returned results rules we start from 
	 */
	Hashtable <SootMethod,SpyReturn> init_returns;
	
	CallGraph callgraph;

    private AnasootConfig acf;
	
	
	/**
	 * Initialize the analysis with a given scene and set of rules.
	 * @param scene
	 * @param rulefile
	 * @param acf 
	 */
	public ProgramSpy(Scene scene, AnajavaRule rulefile, AnasootConfig acf) {
	    this.acf = acf;
		spytable = new Hashtable<Integer, List<SpyMethod>> ();
		
		every_calls = new Hashtable<String, SpyMethod> ();
		every_fields = new Hashtable<SootField, SpyField> ();
		every_returns = new Hashtable<SootMethod, SpyReturn> ();
		
		call_tables = new Hashtable<String, SpyMethod> ();
		field_tables = new Hashtable<SootField, SpyField> ();
		return_tables = new Hashtable<SootMethod, SpyReturn> ();
		
		init_calls = call_tables;
		init_fields = field_tables;
		init_returns = return_tables;
		this.scene = scene;
		this.hierarchy = scene.getActiveHierarchy();
		this.rulefile = rulefile;
		callgraph = scene.getCallGraph();
		JavaReport.resetAll();
		for(JavaReport jr: rulefile.getReports()) {
		    jr.reset();
		}
		activateRules(rulefile);
		finish_registration();
	}	
	
	/**
	 * Register the rules in the different tables. 
	 * @param ruleFile
	 */
	public void activateRules(AnajavaRule ruleFile) {
	    for(JavaRule jr : ruleFile.getRules()) {
	        if (jr instanceof FieldRule) {
	            FieldRule fr = (FieldRule) jr;
	            SootField field = getSootField(fr.getClassName(),fr.getFieldName());
	                if (field != null) {
	                    SpyField def = new SpyField(fr.getName(),field,fr.getReport());
	                    field_tables.put(field,def);
	                }
	        } else if (jr instanceof CallRule) {
                CallRule cr = (CallRule) jr;
                try {
                    SootMethod method = 
                        getSootMethod(cr.getClassName(),cr.getMethodName());
                    if (method != null) {
                        SpyMethod def = new SpyMethodArgs(cr.getName(),method,
                                cr.getPositionArg(),cr.getReport());
                        call_tables.put(cr.getName(),def);
                    }
                } catch (Exception e) {
                    Out.getMain().println("Cannot find method " + cr.getMethodName() +
                            " in class " + cr.getClassName());
                }
            } else if(jr instanceof ReturnRule) {
                ReturnRule rr = (ReturnRule) jr;
                SootMethod method = 
                     getSootMethod(rr.getClassName(), rr.getMethodName());
                    if (method != null) {
                        SpyReturn def = new SpyReturn(rr.getName(),method, rr.getReport());
                        return_tables.put(method,def);
                    }
            } else if(jr instanceof UseRule) {
                UseRule ur = (UseRule) jr;
                SootMethod method = 
                     getSootMethod(ur.getClassName(), ur.getMethodName());
                    if (method != null) {
                        SpyMethod def = new SpyMethodUse(ur.getName(), method, (ReportUse) ur.getReport());
                        call_tables.put(ur.getName(),def);
                    }
            }

	    }
	}
	
	/**
	 * Add a spy definition to the table so that it can be found by the method name that triggers
	 * it.
	 */
	private void add_to_spytable(int idx, SpyMethod def) {
		if (spytable.containsKey(idx))
			(spytable.get(idx)).add(def);
		else {
			List <SpyMethod> l = new ArrayList <SpyMethod> ();
			l.add(def); 
			spytable.put(idx,l);
		}
	}
	/**
	 * Finishes the registration: we remember all the rules we are working on and for each method
	 * spied by a call rule, we maintain the list of rules it had.
	 */
	private void finish_registration() {
		every_calls.putAll(call_tables);
		every_returns.putAll(return_tables);
		every_fields.putAll(field_tables);
		
		for (SpyMethod def : call_tables.values()) {
			SootMethod m = def.getMethod();
			int idx = m.getNumber();
			add_to_spytable(idx,def);
			// if (m.isAbstract()) {
				SootClass c = m.getDeclaringClass();
				NumberedString sign = m.getNumberedSubSignature();
				List <SootClass> tgts = (c.isInterface()) ? hierarchy.getImplementersOf(c) : hierarchy.getSubclassesOf(c);
				for (SootClass tc : tgts) {
					if (!tc.declaresMethod(sign)) continue;
					try {
						SootMethod tm = tc.getMethod(sign);
						add_to_spytable(tm.getNumber(), def);
					} catch (RuntimeException e) {
						 e.printStackTrace(Out.getLog());
					}
				}
			// }
		}
	}
	
	/**
	 * Activate the rules for this given iteration. We take the ones in the call context and
	 * initialize a new call context.
	 * @param loc
	 */
	public void activate_registered(CallContext loc) {
		call_tables = loc.registered_call;
		field_tables = loc.registered_field;
		return_tables = loc.registered_return;
		loc.registered_call = new Hashtable<String, SpyMethod>();
		loc.registered_field = new Hashtable<SootField, SpyField>();
		loc.registered_return = new Hashtable<SootMethod, SpyReturn>();
		spytable = new Hashtable<Integer, List<SpyMethod>> ();
		finish_registration();
	}
	
	/**
	 * Check if there is no rule to apply
	 * @return true if finished.
	 */
	public boolean is_empty() {
		return (call_tables.size() == 0 && field_tables.size() == 0 && 
				return_tables.size() == 0);
	}
	
	/**
	 * Finds a class with a given name and a given method subsignature.
	 * @param className the name of the class.
	 * @param methodName a method subsignature in Soot format
	 * @return
	 */
	public SootMethod getSootMethod(String className, String methodName) {
		SootClass classe;
		SootMethod methode = null;
		try { classe = scene.getSootClass(className); }
		catch (Exception e) { 
			// On ne peut afficher la classe ici. Le package peut ne pas
			// etre charge si inutile.
			// Configuration.warn("Class not found : " + className); 
			return null; 
		}
		try { methode = classe.getMethod(methodName); } catch (Exception e) {
			try {
				Hierarchy hier = scene.getActiveHierarchy();
				if (!classe.isInterface()) {
    				Iterator <SootClass> it = hier.getSuperclassesOf(classe).iterator();
    				done : while(it.hasNext()) {
    					SootClass c = (SootClass) it.next();
    					try {
    						methode = c.getMethod(methodName);
    						break done;
    					} catch (Exception e2) {}
    				}
    			} else {
    				Iterator <SootClass> it = hier.getSubinterfacesOf(classe).iterator();
    				done : while(it.hasNext()) {
    					SootClass c = (SootClass) it.next();
    					try {
    						methode = c.getMethod(methodName);
    						break done;
    					} catch (Exception e2) {}
    				}
    			}
			} catch (ConcurrentModificationException e3) {
				scene.setActiveHierarchy(new Hierarchy());
				getSootMethod(className, methodName);
			}
		}
		if (methode==null) {
			// System.out.println("Method not found : <" + className + "> " + methodName);
		}
		return methode;
	}
	
	/**
	 * Look for the implementation of a field knowing its class and name. 
	 * @param className the name of the class
	 * @param fieldName the name of the field
	 * @return the field in soot representation tied to the right level.
	 */
	public SootField getSootField(String className, String fieldName) {
		SootClass classe;
		SootField field = null;
		try { classe = scene.getSootClass(className); }
		catch (Exception e) { return null; }
		try { field = classe.getField(fieldName); }
		catch (Exception e) {
			try {
				Hierarchy hier = scene.getActiveHierarchy();
				Iterator <SootClass> it = hier.getSuperclassesOf(classe).iterator();
				done : while(it.hasNext()) {
					SootClass c = it.next();
					try {
						field = c.getField(fieldName);
						break done;
					} catch (Exception e2) {}
				}
			} catch (ConcurrentModificationException e3) {
				scene.setActiveHierarchy(new Hierarchy());
				getSootField(className, fieldName);
			}
		}
		if (field == null) {
			// Configuration.output("Field not found : " + fieldName + 
			//		       " in " + className); 
		}
		return field;
	}
	
	/**
	 * Analyses the field assignment if it corresponds to a registered rule.
	 * @param ad context for current analysis.
	 * @param st the statement analysed
	 */
	public void spyField(MethodSpyAnalysis ad, AssignStmt st) {
		Value left = st.getLeftOp();
		if (left instanceof FieldRef) {
			SootField field = ((FieldRef) left).getField();
			SpyField sf = field_tables.get(field);
			if (sf != null) sf.spy(ad, st);
		}
	}
	
	/**
	 * gives back the analysis rule for results returned associated to a given method
	 * @param method the method
	 * @return a rule or null
	 */
	public SpyReturn spyReturn(SootMethod method) {
		if (P2SAux.is_simple(method.getReturnType()))
			return (SpyReturn) return_tables.get(method);
		else return null;
	}
	
	/**
	 * Finds all the call rules that can be applied to a given method.
	 * @param tgt the target method.
	 * @param buf the set of call rules (the result).
	 */
	private void solveMethod(SootMethod tgt, Set <SpyMethod> buf) { 
		List <SpyMethod> toadd = spytable.get(tgt.getNumber ());
		if (toadd != null) {
			buf.addAll(toadd);
		}
	}
	
	/**
	 * Launch the call rules on a given invoke expression.
	 * @param ad context of analysis 
	 * @param ie the instruction (expression) under study
	 * @param st the statement that contains the invoke.
	 * @throws Alert if anything goes wrong
	 */
	public void spy(MethodSpyAnalysis ad, InvokeExpr ie,  Unit st) throws Alert  {
		SootMethod m = ie.getMethod();
		try {
			HashSet<SpyMethod> todolist = new HashSet<SpyMethod>();
			
			if (ie instanceof InstanceInvokeExpr) {

				if (ie instanceof InterfaceInvokeExpr ||
					m.getSignature().equals(NEW_INSTANCE_SIGNATURE)) {
					solveMethod(m,todolist);
				} 
				for(Iterator <Edge> it = callgraph.edgesOutOf(st); it.hasNext();) {
					SootMethod tgt = it.next().tgt();
					solveMethod(tgt,todolist);
				}
			} else {

				solveMethod(m,todolist);
			}
			if (todolist.size() > 0) {
				for(SpyMethod def : todolist) {
					def.spy(ad, ie,  st);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(Out.getLog());
			throw Alert.raised(e, "Error in spy: " + e.getMessage() + " in unit " + ie + " in method " + ad.method);
		}
	}
	

	/**
	 * Prints out the result of the analysis
	 * @param cc Call context containing the rules
	 * @param baa 
	 * @param out the stream to print to
	 * @throws Alert
	 */
	public void dump (AnasootConfig acf, PrintStream out) throws Alert {
		StructureReport.dump(acf, rulefile.getGlobal(), result, out);
	}

	/**
	 * Debug hook.
	 * @param string message to print
	 */
	public static void debug(String string) { 
	}

	/**
	 * Build a new result.
	 * @param cc
	 * @param baa
	 */
	public void buildResult(CallContext cc, ArrayAnalysis baa) {
		result = new SpyResult(cc.nodeTable, baa);
		for (SpyMethod r : init_calls.values()) result.add(r);
		for (SpyReturn r: init_returns.values()) result.add(r);
		for (SpyField r: init_fields.values()) result.add(r);	
	}
	/**
	 * Runs the custom rules and builds the result that will be handled to result dumper.
	 * @param ruleFile
	 * @param app 
	 * @throws Alert
	 */
	public void customRules(AnajavaRule ruleFile, AppDescription app) throws Alert {
		result.customResults.putAll(app.getFacts());
		for(String classname : ruleFile.customCheckers) {
			if (classname != null) {
				CustomSemanticRule rule = acf.getCustomClass(classname);
				rule.run(result,app);
			}
		}
	}

}

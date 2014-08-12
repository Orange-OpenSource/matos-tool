/*
 * $Id: MethodSpyAnalysis.java 2279 2013-12-11 14:45:44Z Pierre Cregut $
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.IntType;
import soot.Local;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.CastExpr;
import soot.jimple.ConditionExpr;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InstanceOfExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.NewExpr;
import soot.jimple.ParameterRef;
import soot.jimple.StaticFieldRef;
import soot.jimple.StringConstant;
import soot.jimple.spark.pag.PAG;
import soot.toolkits.scalar.UnitValueBoxPair;

import com.orange.analysis.anasoot.AnasootConfig;
import com.orange.analysis.anasoot.result.AbsValue;
import com.orange.analysis.anasoot.result.AndValue;
import com.orange.analysis.anasoot.result.BinopValue;
import com.orange.analysis.anasoot.result.ConcatValue;
import com.orange.analysis.anasoot.result.ConstantValue;
import com.orange.analysis.anasoot.result.MarkValue;
import com.orange.analysis.anasoot.result.MethValue;
import com.orange.analysis.anasoot.result.OrValue;
import com.orange.analysis.anasoot.result.PropertyValue;
import com.orange.analysis.anasoot.result.StringValue;
import com.orange.analysis.anasoot.result.UnknownValue;
import com.orange.matos.core.Out;

/**
 * COmputes the approximation of arguments of methods and of expressions in general.
 * @author Pierre Cregut
 *
 */
public class MethodSpyAnalysis {

	final private AnasootConfig acf;
	/**
	 * A black hole value.
	 */
	final static AbsValue star =  new UnknownValue ("*");

	/**
	 * Points-to analysis in use.
	 */
	final PointsToAnalysis pag;
	/**
	 * Call-context representing the rules currently active
	 */
	final CallContext cc;
	/**
	 * Local analysis of the method under study.
	 */
	final LocalAnalysis loc;

	/**
	 * The method analysed (it is its local analysis that is given).
	 */
	final SootMethod method;
	/**
	 * The map associating treatments to function names.
	 */
	final Map <String, AnalyzeCallHandler> callHandlers;

	/**
	 * Constructor for an analysis of calls.
	 * @param acf general configuration for the anasoot plugin
	 * @param pag A points-to-analysis supplied by soot
	 * @param cc The context of the call of the analysed method.
	 * @param loc Local analysis on the method
	 * @param m_orig The method under analysis
	 */
	public MethodSpyAnalysis(AnasootConfig acf, PointsToAnalysis pag, CallContext cc, LocalAnalysis loc, SootMethod m_orig) {
		this.pag = pag;
		this.cc = cc;
		this.loc = loc;
		this.method = m_orig;
		this.acf = acf;

		
		callHandlers = new HashMap <String, AnalyzeCallHandler>();
		if (acf.treatConcatenation()) {
			callHandlers.put("java.lang.StringBuffer.append", new HandleAppend());
			callHandlers.put("java.lang.StringBuilder.append", new HandleAppend());
			callHandlers.put("java.lang.String.concat", new HandleConcat());
			callHandlers.put("java.lang.StringBuffer.<init>", new HandleStringBuffer());
			callHandlers.put("java.lang.StringBuilder.<init>", new HandleStringBuffer());
		}
		callHandlers.put("java.lang.String.equals", new HandleStringEqual());
		callHandlers.put("java.lang.String.<init>", new HandleString());
		callHandlers.put("android.content.Context.getString", new HandleGetResource());
		if (acf.treatStringBuffer()) {
			callHandlers.put("java.lang.StringBuffer.toString", new HandleStringOfBuffer());
			callHandlers.put("java.lang.StringBuilder.toString", new HandleStringOfBuffer());
		}
		if (acf.treatValueOf()) callHandlers.put("java.lang.String.valueOf", new HandleValueOf());
		if (acf.treatProperties()) {
			callHandlers.put("javax.microedition.midlet.MIDlet.getAppProperty", new HandleGetAppProperty()); 
		}
		if (acf.treatStringOperation()) {
			callHandlers.put("java.lang.String.trim", new HandleStringOperation("java.lang.String.trim"));
			callHandlers.put("java.lang.String.substring", new HandleStringOperation("java.lang.String.substring"));
			callHandlers.put("java.lang.String.toLowerCase", new HandleStringOperation("java.lang.String.toLowerCase"));
			callHandlers.put("java.lang.String.toUpperCase", new HandleStringOperation("java.lang.String.toUpperCase"));
			callHandlers.put("java.lang.String.replace", new HandleStringOperation("java.lang.String.replace"));
		}
		callHandlers.put("com.francetelecom.rd.fakemidp.ServiceRecordImplem.getConnectionURL", new HandleConnectionURL());
	}

	/**
	 * Computes the possible strings contained in a points-to set. It should also
	 * take into  account non fixed strings contained.
	 * @param stmt The statement to analyze
	 * @param fdefs the points-to set given back by soot
	 * @return returns a single abstract value representing the string and potentially Unknown.
	 */
	static public AbsValue possibleStringConstantsValue (String stmt, PointsToSet fdefs) {
		Set<AbsValue> str_set = P2SAux.possibleStringConstantsSet(fdefs);
		AbsValue r; 
		if (str_set == null || str_set.size() == 0) r = new UnknownValue(stmt);
		else if (str_set.size() == 1) r = str_set.iterator().next();
		else r = new OrValue(str_set);
		return r;
	}

	/**
	 * Analyze a Soot value.
	 * @param arg the value to analyze
	 * @param typ the type of the value
	 * @param stmt the instruction analyzed
	 * @param seen Set of instructions already treated.
	 * @return The abstract value approximating the contents of the value
	 */
	AbsValue treatValue(Value arg, Type typ, Unit stmt,  Set<Unit> seen) {
		if (arg instanceof Local) 
			return analyzeLocal((Local) arg, stmt,seen);
		else if (arg instanceof StringConstant)
			return new StringValue(((StringConstant) arg).value);
		else if (arg instanceof Constant)
			return new ConstantValue((Constant) arg,typ);
		else {
			Out.getLog().println("Weird value to treat" + arg);
			return new UnknownValue (arg.toString());
		}
	}

	class HandleAppend implements AnalyzeCallHandler {
		@Override
		public AbsValue abstractCall(CallDescription cd, Unit stmt,	Set<Unit> seen) {
			Value arg = cd.args.get(0);
			Type t1 = cd.clazz.getType();
			Type t2 = cd.method.getParameterType(0);
			AbsValue av1 = treatValue(cd.base,t1,stmt,seen);
			AbsValue av2 = treatValue(arg,t2,stmt,seen);
			return (new ConcatValue(av1,av2));
		}
	}

	class HandleStringBuffer implements AnalyzeCallHandler {
		@Override
		public AbsValue abstractCall(CallDescription cd, Unit stmt,
				Set<Unit> seen) {
			if (cd.args.size() > 0) {
				Value arg = (Value) cd.args.get(0);
				Type typ = cd.method.getParameterType(0);
				return treatValue(arg,typ,stmt,seen);
			} else return StringValue.NIL_STRING;
		}	
	}

	class HandleConcat implements AnalyzeCallHandler {
		@Override
		public AbsValue abstractCall(CallDescription cd, Unit stmt,	Set<Unit> seen) {
			Value arg = (Value) cd.args.get(0);
			Type t1 = cd.clazz.getType();
			Type t2 = cd.method.getParameterType(0);
			AbsValue av1 = treatValue(cd.base,t1,stmt,seen);
			AbsValue av2 = treatValue(arg,t2,stmt,seen);
			return (new ConcatValue(av1,av2));
		}
	}

	class HandleStringEqual implements AnalyzeCallHandler {
		@Override
		public AbsValue abstractCall(CallDescription cd, Unit stmt,
				Set<Unit> seen) {
			Value arg = (Value) cd.args.get(0);
			Type t1 = cd.clazz.getType();
			Type t2 = cd.method.getParameterType(0);
			AbsValue av1 = treatValue(cd.base,t1,stmt,seen);
			AbsValue av2 = treatValue(arg,t2,stmt,seen);
			return (new BinopValue("equals", av1,av2));
		}
	}

	class HandleString implements AnalyzeCallHandler {
		@Override
		public AbsValue abstractCall(CallDescription cd, Unit stmt,
				Set<Unit> seen) {
			if (cd.args.size() > 0) {
				Value arg = (Value) cd.args.get(0);
				Type typ = cd.method.getParameterType(0);
				return treatValue(arg,typ,stmt,seen);
			} else return StringValue.NIL_STRING;
		}
	}

	class HandleStringOfBuffer implements AnalyzeCallHandler {
		@Override
		public AbsValue abstractCall(CallDescription cd, Unit stmt,
				Set<Unit> seen) {
			Type typ = cd.clazz.getType();
			return treatValue(cd.base,typ,stmt,seen);
		}
	}

	class HandleValueOf implements AnalyzeCallHandler {
		@Override
		public AbsValue abstractCall(CallDescription cd, Unit stmt,
				Set<Unit> seen) {
			Value arg = (Value) cd.args.get(0);
			Type typ = cd.method.getParameterType(0);
			if (cd.args.size() == 1) {
				return treatValue(arg,typ,stmt,seen);
			} else {
				return new MethValue("java.lang.String.valueOf", treatValue(arg,typ,stmt,seen));
			}
		}
	}

	class HandleStringOperation implements AnalyzeCallHandler {
		private final String callname;

		HandleStringOperation(String callname) { this.callname = callname; }

		@Override
		public AbsValue abstractCall(CallDescription cd, Unit stmt,
				Set<Unit> seen) {
			Type typ = cd.clazz.getType();
			AbsValue av = treatValue(cd.base,typ,stmt,seen);
			return new MethValue(callname, av);
		}
	}

	class HandleGetResource implements AnalyzeCallHandler {
		@Override
		public AbsValue abstractCall(CallDescription cd, Unit stmt,
				Set<Unit> seen) {
			Value arg = (Value) cd.args.get(0);
			Type typ = cd.method.getParameterType(0);
			AbsValue av = treatValue(arg,typ,stmt,seen);
			if (av instanceof ConstantValue) ((ConstantValue) av).toHex();
			return new MethValue("android.content.Context.getString", av);
		}
	}

	class HandleGetAppProperty implements AnalyzeCallHandler {
		@Override
		public AbsValue abstractCall(CallDescription cd, Unit stmt,
				Set<Unit> seen) {
			Value arg = (Value) cd.args.get(0);
			Type typ = cd.method.getParameterType(0);
			AbsValue av = treatValue(arg,typ,stmt,seen);
			return new PropertyValue(av);
		}
	}

	static class HandleConnectionURL implements AnalyzeCallHandler {
		@Override
		public AbsValue abstractCall(CallDescription cd, Unit stmt,
				Set<Unit> seen) {
			return  new MethValue("javax.bluetooth.ServiceRecord.getConnectionURL",
					new UnknownValue(stmt.toString()));
		}
	}

	/**
	 * This is the heart of the method arguments analysis.
	 * @param m The called method
	 * @param base the base argument, may be null.
	 * @param args the arguments.
	 * @param stmt the statement.
	 * @param seen set of instructions already treated.
	 * @return Abstract value of the call
	 */

	AbsValue analyzeCall (CallDescription cd, Unit stmt,  Set<Unit> seen) {
		String mn = cd.method.getName();
		String cn = cd.clazz.getName ();
		String callname = cn + "." + mn;
		AnalyzeCallHandler ch = callHandlers.get(callname); 
		if (ch != null) return ch.abstractCall(cd, stmt, seen);
		else if (acf.treatFunctions()) {
			AndValue argsValue = new AndValue();
			if (cd.base != null) argsValue.add(analyzeArg(cd.base));
			for(Value arg : cd.args) {
				argsValue.add(analyzeArg(arg));
			}
			return new MethValue(callname, argsValue);

		} else {
			return new UnknownValue(stmt.toString());
		}
	}

	/**
	 * Given a method m, analyze the possible outputs of its use. The method should not have
	 * type void. The result is wrapped into a mark so that we do not loop.
	 * @param m a method to analyze
	 * @return an abstract value representing the set of potential results
	 */
	AbsValue analyzeReturn(SootMethod m) {
		SootClass c = m.getDeclaringClass ();
		String tailname = c.getName() + "_" + m.getName();
		if (!cc.doublon_return.containsKey(tailname)) {
			String name = "AC" + cc.count++ + "_" + tailname;
			ProgramSpy.debug("Registering return " + name);
			SpyReturn spy = new SpyReturn(name,m, null);
			cc.doublon_return.put(tailname,spy);
			cc.registerReturn (m, spy);
			return new MarkValue(tailname, spy.getAbsValue());
		} else {
			AbsValue av = ((SpyReturn) cc.doublon_return.get(tailname)).getAbsValue();
			return new MarkValue(tailname, av);
		}
	}
	/**
	 * Solve an invoke expr. Checks if the returned result should be analysed.
	 * @param ie the expression to analyse
	 * @param stmt the statement that contains it
	 * @param seen avoids that we loop on a given statement by keeping the knowledge
	 * @return an abstract value.
	 */
	AbsValue analyzeInvoke (InvokeExpr ie, Unit stmt,  Set<Unit> seen) {
		SootMethod m = ie.getMethod();
		SootClass c = m.getDeclaringClass ();

		if (acf.treatSpyReturn() && c.isApplicationClass() && (P2SAux.is_simple(m.getReturnType()))) {
			return analyzeReturn(m);
		} else {
			return analyzeCall(new CallDescription(ie), stmt,seen);
		}
	}

	/**
	 * gives back a representation of the field 
	 * @param fr an instance field
	 * @return the result cl.name
	 */
	static String str(StaticFieldRef fr) {
		SootField sf = fr.getField ();
		return (sf.getDeclaringClass ()).getName () + "." + sf.getName () ;
	}

	/**
	 * gives back a representation of the field 
	 * @param fr an instance field
	 * @return
	 */
	static String str(InstanceFieldRef fr) {
		SootField sf = fr.getField ();
		return (fr.getBase ()).toString () + "." + (sf.getDeclaringClass ()).getName () + "." + sf.getName () ;
	}


	static boolean is_invoke_on(Unit stmt, Value defval) {
		if (stmt instanceof InvokeStmt) {
			InvokeExpr e = ((InvokeStmt) stmt).getInvokeExpr();
			if (e instanceof InstanceInvokeExpr) 
				return ((InstanceInvokeExpr) e).getBase().equivTo(defval);
			else return false;
		} else return false;
	}

	AbsValue solve_init(AbsValue initial, Unit newStmt, Set<Unit> seen) {
		ProgramSpy.debug("*** SOLVE INIT ***");
		seen.add(newStmt);
		List<UnitValueBoxPair> uses = loc.getUsesOf(newStmt);
		AbsValue result = initial;
		for (UnitValueBoxPair uv : uses) {
			Unit use = uv.unit;
			Value defval = uv.valueBox.getValue();
			if (!seen.contains(use) && is_invoke_on(use,defval)) {
				ProgramSpy.debug("*** INVOKE SOLVE ***");
				InvokeExpr e = ((InvokeStmt) use).getInvokeExpr();
				SootMethod m = e.getMethod();
				String name = 
					m.getDeclaringClass().getName() + "." + m.getName();
				if ((acf.treatStringBuffer() &&
						(name.equals("java.lang.StringBuffer.<init>") ||
								name.equals("java.lang.StringBuilder.<init>"))) ||
								name.equals("java.lang.String.<init>")) {
					ProgramSpy.debug("*** STRING[BUFFER INIT] " + use + " ***");
					if (e.getArgCount() == 1) {
						Value arg = e.getArg(0);
						Type typ = m.getParameterType(0);
						if (IntType.v().equals(typ)) {
							result = StringValue.NIL_STRING; // arg is a size not a content.
						} else if (arg instanceof Local) 
							result = analyzeLocal((Local) arg, use,seen);
						else if (arg instanceof StringConstant)
							result = new StringValue(((StringConstant) arg).value);
						else if (arg instanceof Constant) 
							result = new ConstantValue((Constant) arg,typ);
						else 
							result = new UnknownValue(arg.toString());
					} else result = StringValue.NIL_STRING;
					result = solve_init(result,use,seen);
				} else if (acf.treatConcatenation() &&
						(name.equals("java.lang.StringBuffer.append") ||
								name.equals("java.lang.StringBuilder.append")) &&
								e.getArgCount() == 1) {
					ProgramSpy.debug("*** STRINGBUFFER APPEND ***");
					Value arg = e.getArg(0);
					Type typ = m.getParameterType(0);
					AbsValue rhs = 
						arg instanceof Local
						? analyzeLocal( (Local) arg, use,seen)
								: ((arg instanceof StringConstant) 
										? (AbsValue) new StringValue(((StringConstant) arg).value)
								: ((arg instanceof Constant) 
										? (AbsValue) new ConstantValue((Constant) arg,typ)
								: (AbsValue) new UnknownValue(arg.toString())));
						result = new ConcatValue (result,rhs);
						result = solve_init(result,use,seen);
				} else if (acf.treatStringBuffer() && (
						name.equals("java.lang.StringBuffer.toString") ||
						name.equals("java.lang.StringBuilder.toString")) &&
						e.getArgCount() == 1) {
					ProgramSpy.debug("*** STRINGBUFFER TOSTRING ***");
				} else if (acf.treatFunctions()) {
					result = new MethValue("*",result);
				} else
					result = new UnknownValue(result.toString());
			}
		}
		return result;
	}

	static boolean debugtrue(String s) { ProgramSpy.debug(s); return true; }

	private AbsValue analyzeParameterRef(ParameterRef r, Unit u, Set <Unit> seen ) {
		ProgramSpy.debug("************ PARAMETER ********");
		ArrayList<Integer> args = new ArrayList<Integer> ();
		int pos = ((ParameterRef) r).getIndex() + 1;
		args.add (Integer.valueOf(pos));
		String tailname = 
			method.getDeclaringClass().getName() + "_" + 
			method.getName() + "_" + pos;
		if (acf.treatSpyParameter()) {
			if (!cc.doublon.containsKey(tailname)) {
				String name = "AP" + cc.count++ + "_" + tailname;
				ProgramSpy.debug("Registering " + method.getName() + "," + 
						pos + " under " + name);
				SpyMethodArgs spy = new SpyMethodArgs(name,method,args,null);
				cc.doublon.put(tailname,spy);
				cc.register (name, spy);
				return new MarkValue(tailname, spy.getAbsValue());
			} else {
				AbsValue av = ((SpyMethodArgs) cc.doublon.get(tailname)).getAbsValue();
				return new MarkValue(tailname, av);
			}
		} else {
			return new UnknownValue(r.toString());
		}

	}

	private AbsValue analyzeStaticFieldRef(StaticFieldRef r, Unit u, Set <Unit> seen ) {
		ProgramSpy.debug("************ STATIC FIELD REF ********");
		StaticFieldRef fr = (StaticFieldRef) r;
		SootField field = fr.getField();
		PointsToSet fdefs =pag.reachingObjects(fr.getField()); 
		//			((pag instanceof DemandCSPointsTo) ? ((DemandCSPointsTo) pag).getPAG() : pag).reachingObjects(fr.getField());
		if (P2SAux.is_simple(field.getType())) {
			ProgramSpy.debug("Simple field");
			AbsValue sb = possibleStringConstantsValue(str(fr),fdefs);
			String tailname = 
				field.getDeclaringClass().getName() + "_" +
				field.getName();
			// This is bogus but it seems that Soot incorectly ignores some clinit
			if (unresolved(sb)) { 
				if (!cc.doublon_field.containsKey(tailname)) {
					String name = "AF" + cc.count++ + "_" + tailname;
					ProgramSpy.debug("Registering field " + name);
					SpyField spy = new SpyField(name,fr.getField(),null);
					cc.doublon_field.put(tailname,spy);
					cc.registerField (fr.getField(), spy);
					return  new MarkValue(tailname, spy.getAbsValue());
				} else {
					AbsValue av = ((SpyField) cc.doublon_field.get(tailname)).getAbsValue();
					return  new MarkValue(tailname, av);
				}
			} else {
				return sb; 
			}
		} else
			return P2SAux.p2sContents(cc.nodeTable, fdefs);

	}

	// private void debug(String s) { Out.getLog().println(s); }

	private boolean unresolved(AbsValue sb) {
		if (sb instanceof UnknownValue) return true;
		if (sb instanceof StringValue) {
			String s = ((StringValue) sb).value;
			return s.startsWith("*#MATOS#*") || s.startsWith("[stubs:");
		}
		if (! (sb instanceof OrValue)) return false;
		OrValue orv = (OrValue) sb;
		for(AbsValue val: orv.vals) {
			if (val instanceof UnknownValue) return true;
			if (val instanceof StringValue) {
				String s = ((StringValue) val).value;
				if (s.startsWith("*#MATOS#*") || s.startsWith("[stubs:")) return true;
			}
		}
		return false;
	}

	private AbsValue analyzeInstanceFieldRef(InstanceFieldRef r, Unit u, Set <Unit> seen ) {
		ProgramSpy.debug("******** INSTANCE FIELD REF *******");
		InstanceFieldRef fr = (InstanceFieldRef) r;
		SootField field = fr.getField();
		boolean is_simple = P2SAux.is_simple(field.getType());
		Value b = fr.getBase();
		if (b instanceof Local) {
			if (is_simple) {
				AbsValue pot;
				try { 
					PointsToSet fdefs = pag.reachingObjects((Local) b,fr.getField());
					pot = possibleStringConstantsValue(str(fr),fdefs);
				} catch (Exception e) {
					pot = new UnknownValue(str(fr)); 
				}
				String tailname = 
					field.getDeclaringClass().getName() + "_" +
					field.getName();
				if ( /* acf.treatSpyField && */ unresolved(pot)
						&& P2SAux.is_very_simple(field.getType())) {
					if (!cc.doublon_field.containsKey(tailname)) {
						String name = "AF" + cc.count++ + "_" + tailname;
						ProgramSpy.debug("Registering field " + name);
						SpyField spy = new SpyField(name,fr.getField(),null);
						cc.doublon_field.put(tailname,spy);
						cc.registerField (fr.getField(), spy);
						return  new MarkValue(tailname, spy.getAbsValue());
					} else {
						AbsValue av = ((SpyField) cc.doublon_field.get(tailname)).getAbsValue();
						return  new MarkValue(tailname, av);
					}
				} else if (P2SAux.is_very_simple(field.getType())) { 
					return pot;
				} else return new UnknownValue(r.toString());
			} else {
				try { 
					PointsToSet fdefs =
						pag.reachingObjects((Local) b,fr.getField());
					return P2SAux.p2sContents(cc.nodeTable, fdefs);
				} catch(Exception e) {
					return new UnknownValue(r.toString());
				}
			}
		} else return new UnknownValue(r.toString());

	}

	private AbsValue analyzeArrayRef(ArrayRef r, Unit u, Set <Unit> seen ) {
		ProgramSpy.debug("******** ARRAY REF *******");
		ArrayRef fr = (ArrayRef) r;
		Value b = fr.getBase();
		if (b instanceof Local) {
			try {
				PointsToSet ps1 = pag.reachingObjects((Local) b);
				PointsToSet ps2 = ((PAG) pag).reachingObjectsOfArrayElement(ps1); 
				return possibleStringConstantsValue(b.toString(),ps2);
			} catch (Exception e) { 
				return new UnknownValue(r.toString()); 
			}
		} else return new UnknownValue(r.toString());
	}

	private AbsValue analyzeNewExpr(NewExpr r, Unit u, Set<Unit> seen) {
		ProgramSpy.debug("************ NEW ********");
		String typ = (((NewExpr) r).getBaseType ()).toString ();
		if (typ.equals("java.lang.StringBuffer") || typ.equals("java.lang.StringBuilder")) 
			return StringValue.NIL_STRING;
		else return new UnknownValue(r.toString());

	}

	private AbsValue analyzeConditionExpr(ConditionExpr rc, Unit u, Set<Unit> seen) {
		AbsValue av1 = analyze_expr(rc.getOp1(),u,seen);
		AbsValue av2 = analyze_expr(rc.getOp2(),u,seen);
		return  new MethValue(rc.getSymbol().trim(), av1,av2);
	}

	private AbsValue analyzeInstanceOfExpr(InstanceOfExpr rc, Unit u,
			Set<Unit> seen) {
		AbsValue av1 = analyze_expr(rc.getOp(),u,seen);
		AbsValue av2 = new StringValue(rc.getCheckType().toString());
		return  new MethValue("instanceof", av1,av2);
	}

	private AbsValue analyzeArg(Value v) {
		if (v instanceof Local) {
			PointsToSet p2s = pag.reachingObjects((Local) v);
			return P2SAux.p2sContents(cc.nodeTable, p2s);
		} else if (v instanceof StringConstant)
			return new StringValue(((StringConstant) v).value);
		else if (v instanceof Constant) {
			Constant co = (Constant) v;
			return new ConstantValue(co,co.getType());
		} else {
			Out.getLog().println("Weird argument to analyze : " + v);
			return new UnknownValue(v.toString());
		}
	}
	/**
	 * Analyze an expression (a value) and computes an abstract value representing its contents.
	 * @param r the expression to analyse.
	 * @param u The unit that encapsulate the value.
	 * @param seen What has already be seen (avoid loops).
	 * @return
	 */
	public AbsValue analyze_expr(Value r, Unit u,  Set<Unit> seen) {
		AbsValue result;
		if (r instanceof Local) {
			result = analyzeLocal((Local) r, u, seen);
		} else if (r instanceof StringConstant)
			result = new StringValue(((StringConstant) r).value);
		else if (r instanceof Constant) 
			result = new ConstantValue((Constant) r,((Constant) r).getType());
		else if (r instanceof InvokeExpr) {
			result = analyzeInvoke((InvokeExpr) r,u,seen);
		} else if (r instanceof CastExpr) {
			result = analyze_expr(((CastExpr) r).getOp(),u,seen);
		} else if (r instanceof ParameterRef) {
			result = analyzeParameterRef((ParameterRef) r, u, seen);
		} else if (r instanceof ConditionExpr) {
			result = analyzeConditionExpr((ConditionExpr) r, u, seen);
		} else if (r instanceof InstanceOfExpr) {
			result = analyzeInstanceOfExpr((InstanceOfExpr) r, u, seen);
		} else if (r instanceof StaticFieldRef) {
			result = analyzeStaticFieldRef((StaticFieldRef) r,u,seen);
		} else if (r instanceof InstanceFieldRef) {
			result = analyzeInstanceFieldRef((InstanceFieldRef) r,u,seen);
		} else if (r instanceof ArrayRef) {
			result = analyzeArrayRef((ArrayRef) r,u,seen);
		} else if (r instanceof NewExpr) {
			result = analyzeNewExpr((NewExpr) r, u, seen);
		} else {
			result = new UnknownValue(r.toString());
		}
		return solve_init(result,u,seen);
	}

	/**
	 * Analyze a statement and computes an abstract value representing what it produces. (Value
	 * assigned for an assign) and call approximation for an invoke.
	 * @param u
	 * @param seen
	 * @return
	 */
	public AbsValue analyze_single (Unit u, Set<Unit> seen) {
		if (u instanceof InvokeStmt) {
			ProgramSpy.debug("************ INVOKE ********");
			InvokeExpr e = ((InvokeStmt) u).getInvokeExpr();
			return analyzeInvoke(e,u,seen);
		} else if (u instanceof DefinitionStmt) {
			ProgramSpy.debug("************ DEFINITION "+ u + " ********");
			Value r = ((DefinitionStmt)u).getRightOp ();
			return analyze_expr(r,  u, seen);
		} else {
			ProgramSpy.debug("************ OTHER STMT ********");
			return new UnknownValue (u.toString());
		}
	}

	/**
	 * Analyse a local value. 
	 * @param arg
	 * @param stmt
	 * @param seen
	 * @return
	 */
	public AbsValue analyzeLocal(Local arg, Unit stmt, Set<Unit> seen) {
		ProgramSpy.debug("**************** LOCAL ***************");
		seen.add (stmt);
		List<Unit> defs = loc.getDefsOfAt(arg, stmt);
		if (defs.size() == 1) {
			Unit u = (Unit) defs.get(0);
			return analyze_single(u, seen);
		} else {
			ArrayList<AbsValue> r = new ArrayList<AbsValue>();
			for(Unit u : defs) {
				// Attention, il semble que l'instruction peut etre dans
				// sa propre liste de d�finition...
				if (!seen.contains(u)) r.add(analyze_single(u,seen));
			}
			return new OrValue (r);
		}
	}
} 

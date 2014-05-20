/*
 * $Id:DevirtShow.java 917 2006-09-27 10:15:16 +0200 (mer., 27 sept. 2006) penaulau $
 */
package com.orange.analysis.anasoot.main;

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
import java.util.Iterator;
import java.util.Map;

import soot.ByteType;
import soot.PointsToAnalysis;
import soot.RefType;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.FieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.JimpleBody;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;

import com.orange.analysis.anasoot.AnasootConfig;
import com.orange.analysis.anasoot.apiuse.ApiUseReport;
import com.orange.analysis.anasoot.apiuse.ApplicationApiUse;
import com.orange.analysis.anasoot.arrayanalysis.ArrayAnalysis;
import com.orange.analysis.anasoot.loop.CallbackResolver;
import com.orange.analysis.anasoot.loop.ForbidRecursion;
import com.orange.analysis.anasoot.printing.GlobalReport;
import com.orange.analysis.anasoot.printing.UnresolvedReport;
import com.orange.analysis.anasoot.printing.UsedJSRReport;
import com.orange.analysis.anasoot.profile.rules.AnajavaRule;
import com.orange.analysis.anasoot.spy.CallContext;
import com.orange.analysis.anasoot.spy.LocalAnalysis;
import com.orange.analysis.anasoot.spy.MethodSpyAnalysis;
import com.orange.analysis.anasoot.spy.P2SAux;
import com.orange.analysis.anasoot.spy.ProgramSpy;
import com.orange.analysis.anasoot.spy.SpyReturn;
import com.orange.matos.android.APKDescr;
import com.orange.matos.core.Alert;
import com.orange.matos.core.AppDescription;


/** 
 * This class defines the new soot phase that extends the behaviour of
 *  soot to perform the static analysis of the midlet. It is a global phase
 *  for soot (called on the whole program and not on each class).
 *  that must be performed after the points-to analysis. 
 */
public class DevirtShow extends SceneTransformer {

	final static boolean debugCalls = false;
	final ByteType byteType = ByteType.v();
	final RefType stringType = RefType.v("java.lang.String");
	
	/**
	 * Description of the rules to apply
	 */
	AnajavaRule ruleFile;

	final AnasootConfig acf;
	final private boolean isAndroid;

	/**
	 * The stream on which to print the result.
	 */
	PrintStream outStream;
	ExhaustivityChecker exhaust = new ExhaustivityChecker();
	private ApplicationApiUse applicationApiUse;
	private AppDescription app;
    private GlobalReport global;

	/**
	 * Constructor for the analysis.
	 * @param out the stream on which to print the result.
	 * @param ruleFile the set of rules describing the analysis
	 * @param midletSuite the properties of the midlet (JAD + manifest)
	 */
	DevirtShow(PrintStream out, AnasootConfig acf, AnajavaRule ruleFile, AppDescription app) {
		outStream = out;
		this.ruleFile = ruleFile;
		this.acf = acf;
		this.app = app;
		isAndroid = (app instanceof APKDescr);
		if (ruleFile != null) setRuleFile(ruleFile);
		applicationApiUse = null;
	}

	/**
	 * Let define the rule file at a later stage. Useful with Android where there is a collision
	 * with file parsing.
	 * @param ruleFile
	 */
	public void setRuleFile(AnajavaRule ruleFile) {
		this.ruleFile = ruleFile;
		this.global = ruleFile.getGlobal();

		UsedJSRReport report = (UsedJSRReport)global.get("usedJSR");
		report.addJSR(ruleFile.getConfiguration());
		report.addJSR(ruleFile.getProfile());
	}
	/**
	 * Analyse a class once (in a given iteration).
	 * @param c the class analysed
	 * @param callcontext a context of analysis with main soot analysis.
	 * @param relspy The core of the analysis class.
	 * @param pag The points-to analysis.
	 * @param first_iteration is it the first iteration ?
	 */	

	void treatClass(SootClass c, CallContext callcontext, ProgramSpy relspy, ArrayAnalysis baa, PointsToAnalysis pag) 
	throws Alert
	{			
		boolean isApp = c.isApplicationClass();
		boolean isAndroidApp = false;
		if (isAndroid && applicationApiUse != null) {
			isAndroidApp = ! applicationApiUse.isProfileDefined(c);
		}
		@SuppressWarnings("rawtypes")
		Iterator i_method = c.methodIterator ();
		while(i_method.hasNext ()) {
			SootMethod m = (SootMethod) i_method.next();
			SpyReturn spyReturn = relspy.spyReturn(m);

			if (!m.hasActiveBody()) continue;
			if (isApp) exhaust.checkMethod(m);
			JimpleBody jb = (JimpleBody) m.retrieveActiveBody ();
			// Static analysis giving the defs of a variable
			LocalAnalysis locanalysis = new LocalAnalysis(jb);
			MethodSpyAnalysis analysis = new MethodSpyAnalysis(acf, pag, callcontext,locanalysis,m);
			Iterator<Unit> i_units = (jb.getUnits()).iterator();
			while(i_units.hasNext()) {
				Stmt st = (Stmt)i_units.next();
				if (st instanceof AssignStmt) {
					AssignStmt ast = (AssignStmt) st;
					Value left = ast.getLeftOp();
					Value right = ast.getRightOp();
					if (right instanceof NewExpr) {
						SootClass classUsed = ((NewExpr) right).getBaseType().getSootClass();
						if (isAndroidApp) applicationApiUse.register(classUsed, c);
					} else if (right instanceof FieldRef) {
						SootField fieldUse = ((FieldRef) right).getField();
						if (isAndroidApp) applicationApiUse.register(fieldUse, c);
					} else if (right instanceof NewArrayExpr) {
						soot.Type type = ((NewArrayExpr) right).getBaseType();
						if (type.equals(byteType)) {
							baa.treatNewArray(jb, ast, locanalysis, i_units, callcontext);
						} else if (type.equals(stringType))	{
							baa.registerStringArray(callcontext, ast);
						}
					}
					if (left instanceof FieldRef) {
						SootField fieldUse = ((FieldRef) left).getField();
						if (isAndroidApp) applicationApiUse.register(fieldUse, c);
					} else if (left instanceof ArrayRef && left.getType().equals(byteType)) {
						baa.treatByteArrayAssign(ast, locanalysis, callcontext);
					}
					relspy.spyField(analysis,ast);
				} else if ((st instanceof ReturnStmt) && (spyReturn != null)) {
					spyReturn.spy(analysis, (ReturnStmt) st); 
				} 
				if (st.containsInvokeExpr() && isApp) { // ignore.callInstruction(st)
					InvokeExpr ie =  st.getInvokeExpr();
					exhaust.checkCall(m, st);
					SootMethod calledMethod = ie.getMethod();

					if (isAndroidApp) applicationApiUse.register(calledMethod, c);

					String className = calledMethod.getDeclaringClass().getName();
					if (ruleFile.unresolvedJSR.containsKey(className)){
						UnresolvedReport report = (UnresolvedReport)global.get("unresolved");
						report.add((String)ruleFile.unresolvedJSR.get(className));
					}
					if (ruleFile.listOfJSR.containsKey(className)){
						UsedJSRReport report = (UsedJSRReport)global.get("usedJSR");
						report.addJSR((String)ruleFile.listOfJSR.get(className));
					}
					relspy.spy(analysis,ie,st);
				}
			}
		}
	}

	@Override
	protected void internalTransform(String phaseName,@SuppressWarnings("rawtypes") Map options) {
		Scene scene = Scene.v();
		// IgnoreEdge ignore = new IgnoreEdge(scene);
		PointsToAnalysis pag = scene.getPointsToAnalysis();
		ProgramSpy relspy = new ProgramSpy(scene,ruleFile,acf);
		ArrayAnalysis baa = new ArrayAnalysis(pag);
		try {
			if (isAndroid && acf.doApiUse()) {
				applicationApiUse = new ApplicationApiUse(acf.databasePath); 
			}

			CallContext callcontext = new CallContext();
			P2SAux.init(callcontext, baa);
			while (!relspy.is_empty()) {
				@SuppressWarnings("rawtypes")
				Iterator i_classes = scene.getClasses().iterator();
				while(i_classes.hasNext()) {
					SootClass c = (SootClass) i_classes.next();
					treatClass(c,callcontext,relspy, baa, pag);
				}
				relspy.activate_registered(callcontext);
			}
			relspy.buildResult(callcontext, baa);
			relspy.customRules(ruleFile, app);
			if( applicationApiUse!=null ) {
				applicationApiUse.closeDB();
				ApiUseReport report = new ApiUseReport(applicationApiUse);
				report.displayAnalysisReport(acf, outStream);
				applicationApiUse = null;
				report = null;
			}

			exhaust.dump();

			UsedJSRReport report = (UsedJSRReport)global.get("usedJSR");
			UnresolvedReport report2 = (UnresolvedReport)global.get("unresolved");
			report.tell(outStream, acf.xmlFormat());
			report2.tell(outStream, acf.xmlFormat());
			relspy.dump(acf, outStream);
			if (acf.doLoopAnalysis()) {
				ForbidRecursion fra = new ForbidRecursion(new CallbackResolver());
				fra.doAnalysis(outStream, acf.xmlFormat());
			}
		} catch(Alert a) {
			AlertRuntimeException.wrap(a);
		}
	}
}

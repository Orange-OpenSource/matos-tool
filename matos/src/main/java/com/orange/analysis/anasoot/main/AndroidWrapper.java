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

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import soot.ArrayType;
import soot.Local;
import soot.Modifier;
import soot.PatchingChain;
import soot.Printer;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Trap;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.VoidType;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.util.Chain;

import com.orange.matos.android.APKDescr;
import com.orange.matos.core.Configuration;

/**
 * Definition of the Android application wrapper as a direct class in the scene
 * we avoid going through text files to control the loading mechanism.
 * @author piac6784
 *
 */
public class AndroidWrapper {
	private static final String SIMPLE_INIT = "void <init>()";
	private static final String STUB_TOKEN = "<com.francetelecom.rd.stubs.Generator: com.francetelecom.rd.stubs.Token token>";
	private static final String RUNTIME_CLASS = "com.francetelecom.rd.fakeandroid.android.Runtime";
	private static final String ACTIVITY_REGISTRER = "void runActivity(android.app.Activity)";
	private static final String SERVICE_REGISTRER  = "void runService(android.app.Service)";
	private static final String RECEIVER_REGISTRER = "void runReceiver(android.content.BroadcastReceiver)";
	private static final String PROVIDER_REGISTRER = "void runProvider(android.content.ContentProvider)";
	
	private final static String VIEW_INIT_SIMPLE = "void <init>(android.content.Context)";
	private final static String VIEW_INIT_DOUBLE = "void <init>(android.content.Context,android.util.AttributeSet)";
	private final static String FRAGMENT_INIT_SIMPLE = "void <init>()";
	
	private final static String ATTRIBUTE_SET_IMPLEM = "com.francetelecom.rd.fakeandroid.AttributeSetImpl";
	
	private boolean debug=false;

	/**
	 * Package of the wrapper class
	 */
	public final static String WRAPPER_PACKAGE = "com.francetelecom.rd.fakeandroid";
	/**
	 * Name of the wrapper class.
	 */
	public final static String WRAPPER_CLASS = "Wrapper";
	
	/**
	 * Full name of the class
	 */
	public final static String WRAPPER_PATH = WRAPPER_PACKAGE + "." + WRAPPER_CLASS;
	
	private Scene scene;
	private APKDescr apk;
	private SootClass objectClass;
	private RefType stringType;
	private final Jimple jimple = Jimple.v();
	private Configuration config;
	private int count = 0;
	private HashSet<String> notImplemented;
	private HashSet<String> restriction;
	private SootClass exceptionClass;
	
	/**
	 * Initialize the Android wrapper builder.
	 * @param config Configuration of the tool (for keys)
	 * @param apk The APK to analyze
	 * @param restriction A list restraining what we look at
	 */
	public AndroidWrapper(Configuration config, APKDescr apk, HashSet<String> restriction) {
		this.restriction = restriction;
		this.notImplemented = new HashSet<String>();
		this.scene = Scene.v();
		this.config = config;
		this.apk = apk;
		objectClass = scene.getSootClass("java.lang.Object");
		stringType = RefType.v("java.lang.String");
		exceptionClass = scene.getSootClass("java.lang.Exception");
	}
	
	/**
	 * Create the wrapper class.
	 */
	public void createWrapper() {
		SootClass wrapper = new SootClass(WRAPPER_PATH);
		wrapper.setSuperclass(objectClass);
		scene.addClass(wrapper);
		
		wrapper.addMethod(createInit());
		
		wrapper.addMethod(createMain()); 

	}
	
	/**
	 * Create default constructor of the wrapper class.
	 * @return
	 */
	private SootMethod createInit() {
		SootMethod method = new SootMethod("<init>", new ArrayList<Type>(), VoidType.v(), Modifier.PUBLIC);
		JimpleBody body = Jimple.v().newBody(method);
	    method.setActiveBody(body);
	    Chain <Local> locals = body.getLocals();
	    PatchingChain<Unit> units = body.getUnits();
	    RefType thisType = RefType.v(WRAPPER_PATH);
	    Local r0 = jimple.newLocal("r0", thisType);
	    locals.add(r0);
	    
	    units.add(jimple.newIdentityStmt(r0, jimple.newThisRef(thisType)));
	    SootMethod initObject = scene.getMethod("<java.lang.Object: void <init>()>");
	    units.add(jimple.newInvokeStmt
	            (jimple.newSpecialInvokeExpr(r0, initObject.makeRef())));
	    units.add(jimple.newReturnVoidStmt());

		return method;
	}
	
	/**
	 * Extract all implementers from info in the profile.
	 * @param apk
	 * @param config
	 * @param key
	 * @return
	 */
	private static Set <String> getImplementers(APKDescr apk, Configuration config, String key) {
		String allSystemViews = config.string(key, "");
		String [] classes = allSystemViews.split(",");
		return apk.getSubHierarchy(classes);
	}
	
	
	/**
	 * Create the main entry point of the application in the runtime.
	 * @return
	 */
	private SootMethod createMain() {
		Type arg1Type = ArrayType.v(stringType, 1);
		SootMethod method =
			new SootMethod("main",                 
				Arrays.asList(new Type[] {arg1Type}),
		        VoidType.v(), Modifier.PUBLIC | Modifier.STATIC);
		JimpleBody body = Jimple.v().newBody(method);
	    method.setActiveBody(body);
	    Chain <Local> locals = body.getLocals();
	    PatchingChain<Unit> units = body.getUnits();
	    
	    Local arg1 = jimple.newLocal("a1", arg1Type);
	    locals.add(arg1);
	    
	    RefType r1Type = RefType.v("android.content.ContextWrapper");
	    Local r1 = jimple.newLocal("r1", r1Type);
	    locals.add(r1);
	    
	    RefType r2Type = RefType.v("com.francetelecom.rd.stubs.Token");
	    Local r2 = jimple.newLocal("r2", r2Type);
	    locals.add(r2);
	    
	    RefType r3Type = RefType.v(ATTRIBUTE_SET_IMPLEM);
	    Local r3 = jimple.newLocal("r3", r3Type);
	    locals.add(r3);
	    
	    RefType r4Type = exceptionClass.getType();
	    Local r4 = jimple.newLocal("r4", r4Type);
	    locals.add(r4);
		
	    units.add(jimple.newIdentityStmt(arg1, jimple.newParameterRef(arg1Type, 0)));

	    SootField tokenField = scene.getField(STUB_TOKEN);
	    Unit startStmt = jimple.newAssignStmt(r2, jimple.newStaticFieldRef(tokenField.makeRef()));
	    units.add(startStmt);
	    
	    units.add(jimple.newAssignStmt(r1, jimple.newNewExpr(r1Type)));
	    SootMethod initContext = scene.getMethod("<android.content.ContextWrapper: void <init>(com.francetelecom.rd.stubs.Token)>");
	    units.add(jimple.newInvokeStmt
	            (jimple.newSpecialInvokeExpr(r1, initContext.makeRef(), Arrays.asList(new Value []{r2}))));
	    
	    units.add(jimple.newAssignStmt(r3, jimple.newNewExpr(r3Type)));
	    SootMethod initAttributeSet = scene.getMethod("<" + ATTRIBUTE_SET_IMPLEM + ": void <init>(com.francetelecom.rd.stubs.Token)>");
	    units.add(jimple.newInvokeStmt
	            (jimple.newSpecialInvokeExpr(r3, initAttributeSet.makeRef(), Arrays.asList(new Value []{r2}))));
	    
		Set <String> activities = 
			getImplementers(apk,config,"android.hierarchy.Activity");
		Set <String> services = 
			getImplementers(apk,config,"android.hierarchy.Service");
		Set <String> receivers =
			getImplementers(apk,config,"android.hierarchy.BroadcastReceiver");
		Set <String> providers =
			getImplementers(apk,config,"android.hierarchy.ContentProvider");
		
		String [] viewClasses = config.string("android.hierarchy.View", "").split(",");
		Set <String> views = apk.getSubHierarchy(viewClasses);
		for(String view : viewClasses) views.add(view);
		
		String [] fragmentClasses = config.string("android.hierarchy.Fragment", "").split(",");
		Set <String> fragments = apk.getSubHierarchy(fragmentClasses);
		for(String frag : fragmentClasses) fragments.add(frag);
		
		String application = apk.getApplication();

		List<Value> arrayArg0 = Arrays.asList(new Value []{});
		List<Value> arrayArg1 = Arrays.asList(new Value []{r1});
		List<Value> arrayArg2 = Arrays.asList(new Value []{r1,r3});
		
		for (String viewClassName: views) {
			if (viewClassName.equals("")) continue;
			SootClass viewClass = scene.loadClass(viewClassName, SootClass.HIERARCHY);
			if (!viewClass.isAbstract()) {
				if(viewClass.declaresMethod(VIEW_INIT_SIMPLE)) {
					makeViewInit(units, locals, viewClass, VIEW_INIT_SIMPLE, arrayArg1);
				}
				if(viewClass.declaresMethod(VIEW_INIT_DOUBLE)) {
					makeViewInit(units, locals, viewClass, VIEW_INIT_DOUBLE, arrayArg2);
				}
			}
		}
		
		for (String fragmentClassName: fragments) {
			if (fragmentClassName.equals("")) continue;
			SootClass fragClass = scene.loadClass(fragmentClassName, SootClass.HIERARCHY);
			if (!fragClass.isAbstract()) {
				if(fragClass.declaresMethod(FRAGMENT_INIT_SIMPLE)) {
					makeViewInit(units, locals, fragClass, FRAGMENT_INIT_SIMPLE, arrayArg0);
				}
			}
		}

		SootClass runtime = scene.loadClass(RUNTIME_CLASS , SootClass.HIERARCHY);
		SootMethodRef activityRegister = runtime.getMethod(ACTIVITY_REGISTRER).makeRef();
		for(String activity : activities) {
			if (!scene.containsClass(activity)) {
				notImplemented.add(activity);
				continue;
			}
			SootClass activityClass = scene.loadClass(activity,SootClass.HIERARCHY);
			if (!activityClass.isAbstract() && !notImplemented.contains(activity) && (restriction == null || restriction.contains(activity))) {
				makeComponentInit(units, locals, activityClass, activityRegister);
			}
		}
		
		SootMethodRef serviceRegister = runtime.getMethod(SERVICE_REGISTRER).makeRef();
		for(String service : services) {
			if (!scene.containsClass(service)) {
				notImplemented.add(service);
				continue;
			}
			SootClass serviceClass = scene.getSootClass(service);
			if (!serviceClass.isAbstract() && (restriction == null || restriction.contains(service))) {
				makeComponentInit(units, locals, serviceClass, serviceRegister);
			}
		}

		SootMethodRef providerRegister = runtime.getMethod(PROVIDER_REGISTRER).makeRef();
		for(String provider : providers) {
			if (!scene.containsClass(provider)) {
				notImplemented.add(provider);
				continue;
			}
			SootClass providerClass = scene.getSootClass(provider);
			if (!providerClass.isAbstract() && (restriction == null || restriction.contains(provider))) {
				makeComponentInit(units, locals, providerClass, providerRegister);
			}
		}

		SootMethodRef receiverRegister = runtime.getMethod(RECEIVER_REGISTRER).makeRef();
		for(String receiver : receivers) {
			if (!scene.containsClass(receiver)) {
				notImplemented.add(receiver);
				continue;
			}
			SootClass receiverClass = scene.getSootClass(receiver);
			if (!receiverClass.isAbstract() && (restriction == null || restriction.contains(receiver))) {
				makeComponentInit(units, locals, receiverClass, receiverRegister);
			}
		}

		if (application != null) {
			if (!scene.containsClass(application)) {
				notImplemented.add(application);
			} else {
				SootClass applicationClass = scene.getSootClass(application);
				makeComponentInit(units, locals, applicationClass, null);
			}
		}
		Unit returnStmt = jimple.newReturnVoidStmt();
		Unit gotoStmt = jimple.newGotoStmt(returnStmt);
		Unit handlerStmt = jimple.newIdentityStmt(r4, jimple.newCaughtExceptionRef());
		units.add(gotoStmt);
		units.add(handlerStmt);
		units.add(returnStmt);
		Trap trap = jimple.newTrap(exceptionClass, startStmt, gotoStmt, handlerStmt);
		body.getTraps().add(trap);
		
		if (debug) {
		        try {
                    Printer.v().printTo(body, new PrintWriter(new OutputStreamWriter(System.out,"UTF-8"), true));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException("UTF-8 not supported", e);
                }
		}

		return method;
	}

	private void makeViewInit(PatchingChain <Unit> units, Chain<Local> locals, 
			              SootClass viewClass, String initName,
			              List<Value> arrayArg1) {
		RefType type = viewClass.getType();
		Local v = jimple.newLocal("v" + count++, type);
		locals.add(v);
		units.add(jimple.newAssignStmt(v, jimple.newNewExpr(type)));
		SootMethod init = viewClass.getMethod(initName);
	    units.add(jimple.newInvokeStmt
	            (jimple.newSpecialInvokeExpr(v, init.makeRef(), arrayArg1)));
	}

	
	private void makeComponentInit(PatchingChain <Unit> units, Chain<Local> locals, 
			SootClass clazz, SootMethodRef optRegister) {
		if (! clazz.declaresMethod(SIMPLE_INIT)) return;
		RefType type = clazz.getType();
		Local v = jimple.newLocal("v" + count++, type);
		locals.add(v);
		units.add(jimple.newAssignStmt(v, jimple.newNewExpr(type)));
		SootMethod init = clazz.getMethod(SIMPLE_INIT);
		units.add(jimple.newInvokeStmt
				(jimple.newSpecialInvokeExpr(v, init.makeRef())));
		if (optRegister != null) {
			units.add(jimple.newInvokeStmt
					(jimple.newStaticInvokeExpr(optRegister, Arrays.asList(new Value [] {v}))));
		}
	}

	/**
	 * Gives back the list of components referenced in the manifest but not
	 * implemented in the APK. This will probably never happen because the app
	 * cannot be installed.
	 * @return
	 */
	public Set<String> notImplemented () {
		return notImplemented;
	}
}


/*
 * $Id$
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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.Transform;
import soot.options.Options;

import com.orange.analysis.anasoot.AnasootConfig;
import com.orange.analysis.anasoot.loop.LoopTransform;
import com.orange.analysis.anasoot.printing.JavaReport;
import com.orange.analysis.anasoot.printing.ScoreReport;
import com.orange.analysis.anasoot.printing.ScoreReport.PermissionScore;
import com.orange.analysis.anasoot.profile.rules.AnaDroidRule;
import com.orange.matos.android.APKDescr;
import com.orange.matos.core.Alert;
import com.orange.matos.core.AppDescription;
import com.orange.matos.core.Configuration;
import com.orange.matos.core.MatosPhase;
import com.orange.matos.core.Out;
import com.orange.matos.core.RuleFile;
import com.orange.matos.core.XMLStream;
import com.orange.matos.utils.FileUtilities;
import com.orange.matos.utils.HtmlOutput;

/** 
 * Definition of the Android phase 
 */
public class AndroidPhase implements MatosPhase {

	/**
	 * Package of the wrapper class
	 */
	final static String WRAPPER_PACKAGE = "com.francetelecom.rd.fakeandroid";
	/**
	 * Name of the wrapper class.
	 */
	final static String WRAPPER_CLASS = "Wrapper";
	
	final static String WRAPPER_PATH = WRAPPER_PACKAGE + "." + WRAPPER_CLASS;

	/**
	 * Constant identifying an Android activity
	 */
	public final static int ANDROID_ACTIVITY = 1;
	/**
	 * Constant identifying an Android service
	 */
	public final static int ANDROID_SERVICE = 2;
	/**
	 * Constant identifying an Android content provider
	 */
	public final static int ANDROID_CONTENT = 3;
	/**
	 * Constant identifying a broadcast receiver
	 */
	public final static int ANDROID_RECEIVER = 4;

	private Configuration config;

	private String androidOutput;
	private AnasootConfig anasootconfig;
	private ScoreReport scoreReport;
	private int scoreValue = -1;
	
	/**
	 * Empty constructor. No specialisation needed.
	 */
	public AndroidPhase() {
	}

	@Override
	public void init(Configuration config) {
		this.config = config;
		anasootconfig = AnasootConfig.newConfiguration(config, AnasootConfig.ANDROID);
		androidOutput = 
			(!config.bool("anasoot.throwCode"))
			? "output" 
					: new File(config.getTempDir(), "output").getAbsolutePath();
	}

	@Override
	public boolean isGlobal() { 
		return config.bool("anasoot.android.global");
	}

	@Override
	public String getName() { 
		return "android"; 
	}


	/**
	 * Run the analysis on a given SWF application
	 * @param fileName the name of the application.
	 * @param file The corresponding file
	 * @param anaRule a set of rules for the check.
	 * @param outStream the stream to print the result to.
	 * @return 
	 * @throws Alert if a problem is encountered during the analysis.
	 */
	public Map <String, JavaReport> runSoot(APKDescr apk, RuleFile ruleFile, HashSet <String> restriction, HashSet <String> notImplemented, PrintStream outStream) 
	throws Alert
	{
		anasootconfig.readProperties();
		String classpath = config.androidClasspath() + File.pathSeparator + config.getTempDir();
		// Out.getMain().println(classpath);
		
		
		// String ondemand_pointsto = (anasootconfig.ondemandAnalysis) ? "true" : "false";
		String pointsto_options = 
			"enabled:true";  // + enhanced_pointsto;
			// + ",cs-demand:"+ ondemand_pointsto;
		String sootArgs [] = {
				"--app", "-w", "-cp", classpath, // "-j2me", 
				"-main-class", WRAPPER_PATH,
				"-f", "n" , "-keep-offset",
				"-p", "cg.spark",	pointsto_options,
				"-dynamic-class", "java.lang.Object",
				// We explicitly deselect library packages and do not let soot choose for us.
				/*
				"-include-all","-x","javax.","-x","java.","-x","org.w3c.",
				"-x","org.xml.","-x","org.apache.commons.",
				"-x","com.francetelecom.rd.fakeandroid.","-x","android.",
				*/
				"-x","",
				// "-x","com.android.",
				"-d", androidOutput,
				WRAPPER_PATH };
		// Global Soot reset
		G.reset();
		G.v().out = Out.getLog();
		// Lancement des phases
		// Install the new transform.
		DevirtShow analysisPhase = new DevirtShow(outStream,anasootconfig, null, apk);
		Transform t1 = 
			new Transform(AnasootPhase.ANALYSIS_PHASE + "." + AnasootPhase.ANALYSIS_TRANSFORM,
					analysisPhase);
		Transform t2 = 
			new Transform(AnasootPhase.ANALYSIS_PHASE + "." + AnasootPhase.LOOP_TRANSFORM, new LoopTransform(outStream,ruleFile));
		PackManager pm = PackManager.v();
		pm.getPack(AnasootPhase.ANALYSIS_PHASE).add(t1);
		pm.getPack(AnasootPhase.ANALYSIS_PHASE).add(t2);

		// Register all the options in soot engine. 
		Options.v().parse(sootArgs);

		Scene scene = Scene.v();
		scene.setPhantomRefs(true);
		try {
			scene.loadBasicClasses();
			apk.getCode();
			AndroidWrapper wrapper = new AndroidWrapper(config, apk, restriction);
			wrapper.createWrapper();
			scene.loadBasicClasses();
			scene.loadClassAndSupport(WRAPPER_PATH);
			scene.loadDynamicClasses();
			scene.setDoneResolving();
			
		} catch (Exception e) {
			e.printStackTrace(Out.getLog());
			throw Alert.raised(e, "Cannot load the required classes. ");
		}
		
		AnaDroidRule androidRules = null;
		try {
			androidRules = new AnaDroidRule(ruleFile,"android2.1", anasootconfig);
			analysisPhase.setRuleFile(androidRules);
		} catch (IOException e) {
			e.printStackTrace(Out.getLog());
			throw Alert.raised(e, "Problem with rules.");
		}
		scoreReport = androidRules.getGlobal().getScore();
		updatePermissionScore(apk);
		// Initialize the scene with right classes + support for undefined ones.

		try {
			pm.getPack(AnasootPhase.POINTSTO_PHASE).apply();
			pm.getPack(AnasootPhase.ANALYSIS_PHASE).apply();
		} catch (AlertRuntimeException e) {
			AlertRuntimeException.unwrap(e);
		}
		
		return androidRules.getGlobal().getReports();
	}

	private void updatePermissionScore(APKDescr apk) {
		if (scoreReport != null) {
			Set <String> permissions = apk.requestedPermissions();
			for(PermissionScore elt : scoreReport.getScorePermissions()) {
				if (permissions.contains(elt.getPermission())) elt.use();
			}
		}
	}


	@Override
	public boolean run(String entryName, AppDescription descr,
			RuleFile ruleFile, PrintStream outStream) 
	throws IOException, Alert
	{
		boolean xmlFormat = anasootconfig.xmlFormat();
		HashSet <String> restriction;
		if (entryName != null) {
			restriction = new HashSet<String>();
			restriction.add(entryName);
		} else {
			restriction = null;
		}	
		if (! (descr instanceof APKDescr)) throw new RuntimeException("Android app expected.");
		APKDescr apk = (APKDescr) descr;
		
		HashSet <String> notImplemented = new HashSet<String>();
		if (!xmlFormat && entryName == null) {
			outStream.println(HtmlOutput.header(1, "Global content analysis"));
		}
		
		Map <String, JavaReport> reports = 
			runSoot(apk, ruleFile , restriction, notImplemented, outStream);

		boolean verdict = true;
		// Output unresolved classes.

		verdict &= !apk.resolutionErrors(xmlFormat, outStream);
		// Output unresolved components
		if (notImplemented.size() != 0) {
			if (xmlFormat) {
				XMLStream xmlout = new XMLStream(outStream);
				for (String classname : notImplemented) {
					xmlout.element("unknownComponent");
					xmlout.attribute("class", classname);
					xmlout.endElement();
				}
			} else {
				outStream.println(HtmlOutput.header(4, HtmlOutput.color("red", "References in the manifest to unknown components")));
				outStream.println("<ul>");
				for (String classname : notImplemented) {
					outStream.println("<li>" + HtmlOutput.escape(classname) + "</li>" );
				}
				outStream.println("</ul>");
			}
		}

		for(JavaReport report : reports.values()) {
			verdict &= report.getFinalVerdict();
		}
		if (scoreReport != null) {
		    scoreValue = scoreReport.getScore();
			verdict &= scoreValue <= scoreReport.getThreshold();
		}
		File sootDir = new File(androidOutput);
		if (sootDir.exists() && ! FileUtilities.removeDir(sootDir))
			Out.getLog().println("Cannot destroy sootOutput file.");
		
		return verdict;
	}

	@Override
	public String getMessage() {
		if (scoreReport != null) { return "Score: " + scoreReport.getScore() + "/" + scoreReport.getThreshold(); }
		return null;
	}
	
    @Override
    public int getScore() {
        return scoreValue;
    }

	
}

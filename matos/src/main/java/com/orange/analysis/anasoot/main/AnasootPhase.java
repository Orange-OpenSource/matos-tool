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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;

import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.Transform;
import soot.options.Options;

import com.orange.analysis.anasoot.AnasootConfig;
import com.orange.analysis.anasoot.loop.LoopTransform;
import com.orange.analysis.anasoot.printing.JavaReport;
import com.orange.analysis.anasoot.printing.ScoreReport;
import com.orange.analysis.anasoot.profile.rules.AnajavaRule;
import com.orange.matos.core.Alert;
import com.orange.matos.core.AppDescription;
import com.orange.matos.core.Configuration;
import com.orange.matos.core.MatosPhase;
import com.orange.matos.core.Out;
import com.orange.matos.core.RuleFile;
import com.orange.matos.java.MidletSuite;
import com.orange.matos.utils.FileUtilities;
import com.orange.matos.utils.HtmlOutput;

/**
 * Definition of the anasoot phase.
 * @author Pierre Cregut
 * 
 */
public class AnasootPhase implements MatosPhase {

	/**
	 * Soot phase performing the callgraph analysis. 
	 */
	static final String POINTSTO_PHASE = "cg";

	static final String POINTSTO_TRANSFORM = "spark";
	
	/**
	 * The transformation that performs the loop analysis
	 */
	static final String LOOP_TRANSFORM = "tag.loop";

	/**
	 * Message printed when not enough information
	 */
	private static final String MESSAGE_BAD_ARGS = "midlet name must be defined an a rule file must be given.";

	/**
	 * The name of the AMS wrapping the midlet.
	 */
	private static final String WRAPPER_CLASS = "Wrapper";

	/**
	 * Name of the soot phase containing the Anasoot analysis.
	 */
	static final String ANALYSIS_PHASE = "wjtp";

	/**
	 * Name of the transformation for Anasoot.
	 */
	static final String ANALYSIS_TRANSFORM = "devirt";

	/**
	 * The path to the jar file.
	 */
	private File jarFile;

	/**
	 * The name of the midlet.
	 */
	private String midletName;

	/**
	 * Package solver for finding the configuration of the phone.
	 */
	private PackageSolver packageSolver;

	/**
	 * Generic configuration.
	 */
	private Configuration config;

	/**
	 * The configuration associated to the Anasoot session.
	 */
	private AnasootConfig anasootconfig;

	/**
	 * Name of the jimple wrapper
	 */
	private File jwrapper;

	/**
	 * Should we open an output ?
	 */
	private String sootOutput;

	private ScoreReport scoreReport;
    private int scoreValue = -1;


	/**
	 * The constructor register a package solver. 
	 */
	public AnasootPhase(PackageSolver packageSolver) {
		this.packageSolver = packageSolver;
	}

	/**
	 * Returns the list of library used by soot to perform the analysis,
	 * separated by File.separator.
	 * @return soot's libs
	 */
	public String getSootAPIList() {
		StringBuilder apiList = new StringBuilder();
		String classpath = packageSolver.getClasspath();
		String [] libs = classpath.split(File.pathSeparator);
		for( int i=0; i<libs.length; i++) {
			String lib = libs[i];
			apiList.append(lib.substring(lib.lastIndexOf(File.separator)+1));
			if (i!=libs.length-1) apiList.append(", ");
		}

		return apiList.toString();
	}

	/* (non-Javadoc)
	 * @see com.francetelecom.rd.matos.core.MatosPhase#init(com.francetelecom.rd.matos.core.Configuration)
	 */
	@Override
	public void init(Configuration config) {
		this.config = config;
		anasootconfig = AnasootConfig.newConfiguration(config, AnasootConfig.MIDP);
		sootOutput = 
			(!config.bool("anasoot.throwCode"))
			? "output" 
					: new File(config.getTempDir(), "output").getAbsolutePath();
		packageSolver.init(config.midpClasspath());
	}

	@Override
	public boolean isGlobal() { return false; }

	
	@Override
	public String getName() { return "anasoot"; }

	/**
	 * Work-around for the obsolete NT OS (used by Gallery). It still has
	 * the 8 characters restriction and so we copy the jar to a short name
	 * to avoid weird names.
	 * @param file the original file
	 * @return the short file used as a copy.
	 */
	File solveNT4problem(File file) throws Alert {
		String fullpath = file.getAbsolutePath();
		FileInputStream in = null;
		FileOutputStream out = null;
		if (fullpath.indexOf('~') >= 0) {
			try {
				File temp = File.createTempFile("mdl", ".jar", config.getTempDir());
				in = new FileInputStream(file);
				try {
					out = new FileOutputStream(temp);
					try {
						int c;
						while ((c = in.read()) != -1)
							out.write(c);
					} finally {
						out.close();
					}
				} finally {
					in.close();
				}
				return temp;
			} catch (IOException e) {
				throw Alert.raised(e,"Cannot find a work-around for NT4 bogus pathnames.");
			}
		} else return file;
	}

	/**
	 * Launch soot on the midlet.
	 * @param midletName The name of the midlet class.
	 * @param jarFile The path to the jar file containing the midlet suite
	 * @param midletProperties solved properties (between Jad and Manifest)
	 * @param ruleFile a rule file describing the analysis to perform
	 * @param anaFile the rules extracted from the file and corresponding to bytecode analysis.
	 * @param outStream a stream for output (redirected on GUI)
	 * @throws Alert
	 */
	public void runSoot(String midletName, File jarFile,
			MidletSuite midletSuite, RuleFile ruleFile,
			AnajavaRule anaFile, PrintStream outStream) 
	throws Alert
	{
		anasootconfig.readProperties();
		jarFile = solveNT4problem(jarFile);
		this.jarFile = jarFile;
		this.midletName = midletName;
		String classpath = packageSolver.getClasspath()	+ File.pathSeparatorChar + jarFile.getAbsolutePath()
		+ File.pathSeparatorChar + jwrapper.getParent();

		String ondemand_pointsto = (anasootconfig.hasOndemandAnalysis()) ? "true" : "false";
		String pointsto_options = "enabled:true,cs-demand:"+ ondemand_pointsto;

		String sootArgs [] = {
				"--app", "-w", "-cp", classpath, "-j2me", 
				"-main-class", WRAPPER_CLASS,
				"-f", "n" , "-keep-offset",
				"-p", "cg.spark",	pointsto_options,
				"-dynamic-class", "java.lang.Object",
				// We explicitly deselect library packages and do not let soot choose for us.
				"-include-all","-x","javax.","-x","java.","-x","org.w3c.","-x","org.xml.",
				"-x", "com.francetelecom.rd.fakemidp",
				"-d", sootOutput,
				WRAPPER_CLASS };
		// Global Soot reset
		G.reset();
		G.v().out = Out.getLog();
		// Install the new transform.
		Transform t1 = 
			new Transform(ANALYSIS_PHASE + "." + ANALYSIS_TRANSFORM,
					new DevirtShow(outStream,anasootconfig, anaFile,midletSuite));
		Transform t2 = 
			new Transform(ANALYSIS_PHASE + "." + LOOP_TRANSFORM, new LoopTransform(outStream,ruleFile));
		PackManager pm = PackManager.v();
		pm.getPack(ANALYSIS_PHASE).add(t1);
		pm.getPack(ANALYSIS_PHASE).add(t2);

		// Lancement des phases
		Options.v().parse(sootArgs);
		Scene.v().addBasicClass("java.lang.Cloneable",SootClass.HIERARCHY);
		Scene.v().addBasicClass("java.io.Serializable",SootClass.HIERARCHY);
		Scene.v().loadNecessaryClasses();
		Scene.v().setPhantomRefs(true);
		try {
			pm.getPack(POINTSTO_PHASE).apply();
			pm.getPack(ANALYSIS_PHASE).apply();
		} catch (AlertRuntimeException e) {
			AlertRuntimeException.unwrap(e);
		}
	}

	/**
	 * Writes the wrapper calling the midlet (sort of fixed AMS) in Jimple.
	 * It can be read back directly by Soot.
	 * 
	 * @param midletName The name of the midlet class.
	 * @param jarFile
	 */
	public void writeJasminWrapper(String midletName) throws Alert {
		String midletClass = midletName.replace('/','.').replace('\\','.');
		if (jwrapper.exists() && !jwrapper.delete()) 
			Out.getLog().println("Cannot remove the Java wrapper file " + jwrapper);
		try {	
			PrintStream wout = new PrintStream(new FileOutputStream(jwrapper), false, "UTF-8");
			wout.println("class " + WRAPPER_CLASS + "  extends java.lang.Object {");
			wout.println("  void <init>() { ");
			wout.println("          Wrapper r0;");
			wout.println("          r0 := @this:Wrapper;");
			wout.println("          specialinvoke r0.<Wrapper: void <init>()>();");
			wout.println("          return;");
			wout.println("      }");

			wout.println("    public static void main(java.lang.String[])");
			wout.println("    {");
			wout.println("        java.lang.String[] r0;");
			wout.println("        "+ midletClass +" $r1;");
			wout.println("        java.lang.Exception r2, $r3;");

			wout.println("        r0 := @parameter0:java.lang.String [];");

			wout.println("     label0:");
			// packageSolver.writeJasminInit(wout);
			wout.println("        $r1 = new " + midletClass + ";");
			wout.println("        specialinvoke $r1.<" + midletClass + ": void <init>()> ();");
			// wout.println("staticinvoke <com.francetelecom.rd.fakemidp.midp.Runtime: void run(com.francetelecom.rd.fakemidp.midp.MIDletImplem)>($r1);");
			wout.println("     label1:");
			wout.println("        goto label3;");
			wout.println("");
			wout.println("     label2:");
			wout.println("        $r3 := @caughtexception;");
			wout.println("        r2 = $r3;");

			wout.println("     label3:");
			wout.println("        return;");
			wout.println("        catch java.lang.Exception from label0 to label1 with label2;");
			wout.println("    }");
			wout.println("}");
			wout.close(); 
		} catch (IOException e) {
			throw Alert.raised(e,"Cannot write the wrapper class.");
		}
	}

	@Override
	public boolean run(String midletName, AppDescription descr,
			RuleFile ruleFile, PrintStream outStream) 
	throws Alert, IOException
	{
		if (! (descr instanceof MidletSuite)) throw Alert.raised(null, MESSAGE_BAD_ARGS);
		MidletSuite ms = (MidletSuite) descr;
		File jarFile = ms.jarFile; 
		Properties midletProperties = ms.properties;
		if (midletName == null || ruleFile == null) { 
			throw Alert.raised(null, MESSAGE_BAD_ARGS);
		}
		boolean hasOut = false;
		packageSolver.commit(midletProperties);
		AnajavaRule anarule = 
			new AnajavaRule(ruleFile,packageSolver.getConfiguration(), packageSolver.getProfile(), anasootconfig);
		scoreReport = anarule.getGlobal().getScore();
		jwrapper = new File(config.getTempDir(), WRAPPER_CLASS + ".jimple");
		writeJasminWrapper(midletName);
		runSoot(midletName, jarFile, ms, ruleFile ,anarule, outStream);


		boolean verdict = true;
		for(JavaReport report : anarule.getReports()) {
			hasOut |= report.hasOut;
			if (!report.getFinalVerdict()){
				verdict = false;
				break;
			}
		}
	    if (scoreReport != null) {
	         scoreValue = scoreReport.getScore();
	         verdict &= scoreValue <= scoreReport.getThreshold();
	    }
		File sootDir = new File(sootOutput);
		if (sootDir.exists() && ! FileUtilities.removeDir(sootDir)) Out.getLog().println("Cannot destroy sootOutput file.");
		if (!hasOut){
			outStream.println(
					HtmlOutput.paragraph(HtmlOutput.color("green","No problem detected in the analysis of this midlet.")));
		}
		return verdict;
	}

    @Override
    public int getScore() {
        return scoreValue;
    }

	@Override
	public String getMessage() {
		return null;
	}

    /**
     * Gets the midlet name.
     *
     * @return the midlet name
     */
    public String getMidletName() {
        return midletName;
    }


    /**
     * Gets the jar file.
     *
     * @return the jar file
     */
    public File getJarFile() {
        return jarFile;
    }

}

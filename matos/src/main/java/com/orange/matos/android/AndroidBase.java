package com.orange.matos.android;

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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Locale;

import com.orange.d2j.APKFile;
import com.orange.matos.core.Alert;
import com.orange.matos.core.Configuration;
import com.orange.matos.core.Digest;
import com.orange.matos.core.MatosPhase;
import com.orange.matos.core.Out;
import com.orange.matos.core.RuleFile;
import com.orange.matos.utils.HtmlOutput;

/**
 * @author piac6784
 * Base class for Android analysis. Joins te phase and configuration.
 */
public class AndroidBase {

	private final Configuration configuration;
	MatosPhase [] androidPhases;
	private boolean xmlFormat;
    private String message = "";
    private int score = -1;
    private long elapsedTime = -1;
	
	/**
	 * Constructor
	 * @param configuration
	 */
	public AndroidBase(Configuration configuration) {
		this.configuration = configuration;
		androidPhases = configuration.phases("android");
	}

	/** 
	 * Prints the header in the result of the analysis for an android analysis. For files
	 * that were found on a remote site (via HTTP), the header will
	 * display the original location (HTTP URL) of the file rather than the
	 * location of the local copy. 
	 * @param androidFile the local copy of the file
	 * @param androidFileUrl the url of the file
	 * @param outStream the stream where to print the header
	 * @param apacheMode if Matos is invoked by a web application, names of jad and jar file are not displayed in the HTML report.
	 */
	public void printAndroidHeader(RuleFile ruleFile, String androidFile, String androidFileUrl, PrintStream outStream) {
		outStream.println(HtmlOutput.openDiv("header"));
		outStream.println(HtmlOutput.openTable());
		FileInputStream fis = null;


		configuration.printHeader(outStream,xmlFormat,"Android file analyzed", (androidFileUrl==null ? androidFile : androidFileUrl));

		try {
			fis = new FileInputStream(androidFile);
			configuration.printHeader(outStream,xmlFormat,"SHA1 of Android file analyzed", Digest.runSHA1(fis));
			fis.close();
		} catch (FileNotFoundException e) {
			
		} catch (IOException e) {} 
		String date = 
			DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.MEDIUM, 
					Locale.US).format(new GregorianCalendar().getTime ());
		configuration.printHeader(outStream,xmlFormat,"Date", date);
		configuration.printHeader(outStream,xmlFormat,"Rule File", ruleFile.name);
		configuration.printHeader(outStream,xmlFormat,"Analyser version", configuration.string("version"));
		outStream.println(HtmlOutput.closeTable());
		outStream.println(HtmlOutput.closeDiv());
		outStream.println(xmlFormat ? "" : HtmlOutput.hrule());
		outStream.flush();
	}

	/**
	 * @param androidFile
	 * @param androidFileUrl
	 * @param ruleFile
	 * @param outStream
	 * @param apacheMode
	 * @return
	 * @throws Alert
	 */
	public boolean runAndroidAnalysis( File androidFile, String androidFileUrl, RuleFile ruleFile, PrintStream outStream) throws Alert {
		return runAndroidAnalysis( new APKFile(androidFile), androidFileUrl, ruleFile, outStream);
	}
	/**
	 * Run a android analysis.
	 * @param androidFile
	 * @param ruleFile
	 * @param outStream
	 * @param apacheMode
	 * @return a verdict
	 * @throws Alert
	 * Runs the analysis by joining the different phases together.
	 */
	
	public boolean runAndroidAnalysis( APKFile apkFile, String androidFileUrl, RuleFile ruleFile, PrintStream outStream) throws Alert {
	    score = 0;
		boolean verdict = true;
		long time = 0;
		configuration.initAnalysis(ruleFile);
		xmlFormat = configuration.xmlFormat();
		boolean timingEnabled = configuration.timingEnabled();
		if (timingEnabled) {
			time = System.currentTimeMillis();
		}
		if (xmlFormat) {
			outStream.println("<?xml version='1.0' encoding='utf-8'?>");
			outStream.println("<!DOCTYPE matres SYSTEM \"http://rd.francetelecom.com/matres.dtd\" []>");
			outStream.println("<root>");
		}
 
		printAndroidHeader(ruleFile, apkFile.getName(), androidFileUrl, outStream);
		try {
			APKDescr apk = new APKDescr(apkFile);
	
			boolean hasLocalPhases = false;
			for(MatosPhase phase : androidPhases) {
				if (! phase.isGlobal()) { hasLocalPhases = true; continue; }
				if (!xmlFormat) outStream.println(HtmlOutput.openDiv(phase.getName()));
				try {
					verdict &= phase.run(null, apk,  ruleFile, outStream);
					int phaseScore = phase.getScore();
					if (phaseScore > 0) score += phaseScore;
					if (phase.getMessage() != null) {
						if (message.length() == 0) message = phase.getMessage();
						else message = message + " - " + phase.getMessage();
					}
				} catch (Alert e) {
					throw e;
				} catch (Exception e) {
				    e.printStackTrace(Out.getLog());
					String androidFileName = androidFileUrl;
					if (androidFileName==null) androidFileName = apkFile.getName();
					throw Alert.raised(e,"Problem in phase " + phase.getName() + " on " + androidFileName);
				} finally {
					if (!xmlFormat) outStream.println(HtmlOutput.closeDiv());
				}
			}
			if (hasLocalPhases) {
				verdict &= runLocalAnalysis("activity", apk.getActivities(), apk, outStream, ruleFile);
				verdict &= runLocalAnalysis("service", apk.getServices(), apk, outStream, ruleFile);
				verdict &= runLocalAnalysis("content", apk.getContentProviders(), apk, outStream, ruleFile);
				verdict &= runLocalAnalysis("receiver", apk.getBroadcastReceivers(), apk, outStream, ruleFile);
			}
			outStream.println("");
		} catch (IOException e) {
			throw Alert.raised(e, "Cannot parse APK");
		}
		configuration.setInAnalysisMode(false);
		if (timingEnabled) {
			elapsedTime = System.currentTimeMillis() - time;
			System.out.println("Analysis time : " + (elapsedTime / 1000.));
		}
		return verdict;

	}

	private boolean runLocalAnalysis(String kind, Collection<String> entryPoints, APKDescr apk, PrintStream outStream, RuleFile ruleFile) 
	throws Alert {
		boolean verdict = true;
		for(String clazz : entryPoints) {
			if (xmlFormat) {
				outStream.println("<" + kind + " name=\"" + clazz + "\">");
			} else {
				outStream.println(HtmlOutput.br());
				outStream.println(HtmlOutput.openDiv(kind));
				outStream.print(HtmlOutput.header(1, kind + " " + clazz));
			}
			for(MatosPhase phase : androidPhases) {
				try {
					if (phase.isGlobal()) continue;
					boolean verdictPhase = 
						phase.run(clazz, apk,  ruleFile, outStream);
					if (phase.getMessage() != null) { message = message + " - " + phase.getMessage(); }
					verdict = verdict && verdictPhase;
					int phaseScore = phase.getScore();
					if (phaseScore > 0) score += phaseScore;
				} catch (Alert e) {
					throw e;
				} catch (Exception e) {
				    e.printStackTrace(Out.getLog());
					String androidFileName = apk.getName();
					throw Alert.raised(e,"Problem in phase " + phase.getName() + " on " + androidFileName);
				} finally {
					if (!xmlFormat) outStream.println(HtmlOutput.closeDiv());
				}
			}				
		}
		return verdict;
	}
	
	/**
	 * Get back an informative message corresponding to the step.
	 * 
	 * @return
	 */
	public String getMessage() {
		return message;
	}
	
	/**
	 * Gets the global score of the last analysis.
	 *
	 * @return the score
	 */
	public int getScore() { 
	    return score;
	}
	
	/**
	 * Gets the time of the last analysis
	 *
	 * @return the time
	 */
	public long getTime() {
	    return elapsedTime ;
	}
	
}

package com.orange.matos.java;

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
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import soot.util.StringTools;

import com.orange.analysis.anasoot.main.AnasootPhase;
import com.orange.analysis.implemchecker.ImplemCheckerPhase;
import com.orange.matos.core.Alert;
import com.orange.matos.core.Configuration;
import com.orange.matos.core.Digest;
import com.orange.matos.core.ExtendedProperties;
import com.orange.matos.core.MatosPhase;
import com.orange.matos.core.Out;
import com.orange.matos.core.RuleFile;
import com.orange.matos.utils.HtmlOutput;

/**
 * @author Pierre Cregut
 * Main specification class for a MIDP analysis.
 */
public class JavaBase {
	final Configuration configuration;
    private int score = -1;
    private long elapsedTime = -1;

	/**
	 * Constructor
	 * @param configuration
	 */
	public JavaBase(Configuration configuration) {
		this.configuration = configuration;
	}

	/** 
	 * Utility function that loads the properties defined in the manifest of
	 * a midletsuite in another set of properties that usually contains the
	 * properties defined in the JAD file. If the JAR is signed priority
	 * is given to the JAR manifest otherwise priority is to the JAD
	 * @param jarFile the hook to the JAR file
	 * @param jadProperties the properties defined in the JAD if available
	 * @return the augmented set of properties 
	 */	
	static public Properties loadManifest(File jarFile, Properties jadProperties)
			throws Alert {
		Properties jarProperties = new ExtendedProperties("Manifest");

		try {
			ZipFile zf = new ZipFile(jarFile);

			ZipEntry manifest = zf.getEntry("META-INF/MANIFEST.MF");
			try {
				if (manifest == null) {
					zf.close();
					return jadProperties;
				}
				InputStream is = zf.getInputStream(manifest);
				try {
					/*
					 * Old version (Problem with property that exceeds 70
					 * characters): jarProperties.load(is); is.close();
					 */
					Manifest mf = new Manifest(is);
					Map<String, Attributes> map = mf.getEntries();
					Attributes mainAttributes = mf.getMainAttributes();
					Set<Object> mainSetAttributes = mainAttributes.keySet();
					for (Object raw : mainSetAttributes) {
						Name attributeName = (Name) raw;
						String attributeValue = mainAttributes
								.getValue(attributeName);
						jarProperties.put(attributeName.toString(),
								attributeValue);
					}
					Set<String> set = map.keySet();
					for (String propertyName : set) {
						Attributes attributes = (Attributes) map
								.get(propertyName);
						Set<Object> setAttributes = attributes.keySet();
						for (Object raw : setAttributes) {
							Name attributeName = (Name) raw;
							String attributeValue = attributes
									.getValue(attributeName);
							jarProperties.put(attributeName.toString(),
									attributeValue);
						}
					}

					// Priority of Jar over Jad if the midlet is signed.
					// No consistency checking : let to header checking phase.
					if (jadProperties.containsKey("MIDlet-Jar-RSA-SHA1")) {
						jadProperties.putAll(jarProperties);
						return jadProperties;
					} else {
						jarProperties.putAll(jadProperties);
						return jarProperties;
					}
				} finally {
					is.close();
				}
			} finally {
				zf.close();
			}
		} catch (ZipException ze) {
			Out.getLog().println(ze.getMessage());
			throw new Alert("A ZIP error has occured while reading the midlet.");
		} catch (IOException ioe) {
			ioe.printStackTrace(Out.getLog());
			throw Alert.raised(ioe,
					"An I/O exception has occured while reading the manifest");

		}

	}


	/** 
	 * Prints the header in the result of the analysis. For files
	 * that were found on a remote site (via HTTP), the header will
	 * display the original location (HTTP URL) of the file rather than the
	 * location of the local copy. Parameters jarFileUrl and
	 * jadFileUrl are used for this if different from null;
	 * otherwise the jarFile and jadFile absolute paths is used.
	 * 
	 * @param jarFile : hook to the jar file
	 * @param jadFile : hook to the jad file (may be null if non
	 * existent)
	 * @param jarFileUrl : location where the JAR was found originally
	 * @param jadFileUrl : location where the JAD was found originally
	 * @param jad : the set of properties
	 * @param outStream : the stream where to print the header 
	 * @param apacheMode : if Matos is invoked by a web application, names of jad and jar file are not displayed in the HTML report.
	 */	
	public void printJavaHeader(RuleFile ruleFile, MidletSuite ms,
			PrintStream outStream) {
		boolean xmlFormat = configuration.xmlFormat();
		String name = ms.properties.getProperty("MIDlet-Name");
		String vendor = ms.properties.getProperty("MIDlet-Vendor");
		String version = ms.properties.getProperty("MIDlet-Version");
		if (!xmlFormat) {
			outStream.println(HtmlOutput.openDiv("header"));
			outStream.println(HtmlOutput.openTable());
		}
		// boolean validJar = true;


			configuration.printHeader(outStream, xmlFormat,
					"JAR file analyzed",
					(ms.jarURL == null ? ms.jarFile.getAbsolutePath()
							: ms.jarURL));
		
		try {
			FileInputStream fis = new FileInputStream(ms.jarFile);
			try {
				configuration.printHeader(outStream, xmlFormat,
							"SHA1 of JAR file analyzed", Digest.runSHA1(fis));
			} finally {
				fis.close();
			}
		} catch (IOException e) {
			e.printStackTrace(Out.getLog());
		}
		

			if (ms.jadFile == null) {
				configuration.printHeader(outStream,xmlFormat,"JAD file analyzed", "None");
			} else {
				configuration.printHeader(outStream,xmlFormat,"JAD file analyzed", 
						(ms.jadURL==null ? ms.jadFile.getAbsolutePath() : ms.jadURL));
			}

		configuration.printHeader(outStream,xmlFormat,"Midlet suite name",
				(name==null ? "&lt;unknown&gt;" 
						:  StringTools.getQuotedStringOf(name)));
		configuration.printHeader(outStream,xmlFormat,"Vendor",
				(vendor==null ? "&lt;unknown&gt;"
						: StringTools.getQuotedStringOf(vendor)));
		configuration.printHeader(outStream,xmlFormat,"Version", 
				(version==null ? "&lt;unknown&gt;"
						: StringTools.getQuotedStringOf(version)));
		String date = 
			DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.MEDIUM, 
					Locale.US).format(new GregorianCalendar().getTime ());
		configuration.printHeader(outStream,xmlFormat,"Date", date);
		configuration.printHeader(outStream,xmlFormat,"Rule File", ruleFile.name);
		configuration.printHeader(outStream,xmlFormat,"Analyser version", configuration.string("version"));
		if (!xmlFormat) {
			outStream.println(HtmlOutput.closeTable());
			outStream.println(HtmlOutput.closeDiv());
			outStream.println(HtmlOutput.hrule());
		}
		outStream.flush();
	}

	/**
	 * Run a java analysis. 
	 * @param midletNames all the midlets to analyse.
	 * @param jarFile the analysed code
	 * @param jadFile the analysed jad
	 * @param jarFileUrl the url of the code
	 * @param jadFileUrl the url of the descriptor
	 * @param properties properties of the midlet
	 * @param ruleFile the rules
	 * @param outStream where to print the results
	 * @param apacheMode
	 * @return a verdict
	 * @throws Alert
	 */
	public boolean runJavaAnalysis(Object [] midletNames,
			MidletSuite ms,
			RuleFile ruleFile, PrintStream outStream) 
	throws Alert 
	{
	    long time = System.currentTimeMillis();
		configuration.initAnalysis(ruleFile);
		boolean xmlFormat = configuration.xmlFormat();
		boolean verdict = true;

		// if (bool("header.enabled")) // always true because header.enabled propertiy does not exists in config file, and not used anywhere else 
		printJavaHeader(ruleFile, ms, outStream);

		MatosPhase [] javaAuxiliaryPhases = (MatosPhase []) configuration.phases("java");
		configuration.resetAppInfo();
		

		for(int i = 0; i < javaAuxiliaryPhases.length; i++) {
			MatosPhase phase = javaAuxiliaryPhases[i];
			if (!configuration.globalphase(phase)) continue;
			try {
				if (!xmlFormat) outStream.println(HtmlOutput.openDiv(phase.getName()));
				boolean verdictPhase = phase.run(null, ms, ruleFile, outStream);	
				verdict = verdict && verdictPhase;
                int phaseScore = phase.getScore();
                if (phaseScore > 0) score += phaseScore;
			} catch (Alert e) {
				throw e;
			} catch (Exception e) {
				e.printStackTrace();
				String jarName = ms.jarURL;
				if (jarName==null) jarName = ms.jarFile.getName();
				throw Alert.raised(e,"Problem in phase " + phase.getName() + " on " + jarName);
			} finally {
				if (!xmlFormat) outStream.println(HtmlOutput.closeDiv());
			}
		}
		boolean hasLocal = false;
		for(int j = 0; !hasLocal && j < javaAuxiliaryPhases.length; j++) {
			if (configuration.localphase(javaAuxiliaryPhases[j])) hasLocal=true;
		}
		if (hasLocal) {
			if (configuration.bool(ImplemCheckerPhase.MATOS_CHECK_ALL_MIDLETS_KEY, false)) {
				List <Object> allMidlets = configuration.getAppInfo("*");
				midletNames = allMidlets.toArray(new String[0]);
			}
			Arrays.sort(midletNames);
			for(int i=0;i<midletNames.length;i++) {
				String midletName = (String) midletNames[i];
				outStream.println(xmlFormat ? "": HtmlOutput.br());
				if (xmlFormat) {
					outStream.println("<midlet name=\"" + midletName + "\">");
					MidletKind.dump(outStream,configuration, midletName);
				} else {
					outStream.println(HtmlOutput.openDiv("midlet"));
					outStream.print("<h1> Midlet "+ midletName + " " );
					MidletKind.dump(outStream,configuration, midletName);
					outStream.println("</h1>");
				}
				MatosPhase phase = null;
				try {
					for(int j = 0; j < javaAuxiliaryPhases.length; j++) {
						phase = javaAuxiliaryPhases[j];
						if (configuration.localphase(phase)) {
							boolean verdictPhase = phase.run(midletName, ms, ruleFile, outStream);
							verdict = verdict && verdictPhase;
			                 int phaseScore = phase.getScore();
			                    if (phaseScore > 0) score += phaseScore;
						}
					}
				} catch (Alert e) {
					throw e;
				} catch (Exception e) {
					e.printStackTrace(Out.getLog());
					String jarName = ms.jarURL;
					if (jarName==null) jarName = ms.jarFile.getName();
					String dumbMessage = "Problem in phase " + phase.getName() + 
					" on midlet " + midletName + " of " + ms.jarURL;
					treatProblem(e,phase,dumbMessage,outStream,xmlFormat);
				} finally {
					if (xmlFormat) {
						outStream.println("</midlet>");
					} else {
						outStream.println(HtmlOutput.closeDiv());
						outStream.println(HtmlOutput.br());
					}
				}
			}
		}

		outStream.println();
		elapsedTime = System.currentTimeMillis() - time;
		configuration.setInAnalysisMode(false);
		return verdict;
	}

	void treatProblem(Exception e, MatosPhase phase, String dumbMessage, PrintStream outStream, boolean xmlFormat) throws Alert {
		String message = "Problem during analysis : ";
		if (phase instanceof AnasootPhase) {
			String missingClass = "";
			//Format of message :"couldn't find type: javax.microedition.lcdui.game.GameCanvas (is your soot-class-path set properly?)"
			String eMessage = e.getMessage();
			if (eMessage!=null) {
				if (eMessage.startsWith("couldn't find type:")) {
					int start = eMessage.indexOf(':');
					int end = eMessage.indexOf('(');
					missingClass = eMessage.substring(start+1, end-1).trim();
					if (missingClass.trim().length()!=0) {
						message += "Couldn't find '" + missingClass + "' among " + ((AnasootPhase)phase).getSootAPIList();
					} else {
						message += eMessage;
					}
				} else {
					message += eMessage;
				}
			} else {
				message += dumbMessage;
			}
		} else {
			String eMessage = e.getMessage();
			if (eMessage!=null) {
				message += e.getMessage();
			} else {
				message += dumbMessage;
			}
		}
		if (!xmlFormat) {
			outStream.println( HtmlOutput.paragraph( HtmlOutput.color("red", HtmlOutput.escape(message))) +  HtmlOutput.br());
		}

		throw Alert.raised(e,dumbMessage);
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

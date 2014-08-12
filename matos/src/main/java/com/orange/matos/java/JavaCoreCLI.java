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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Properties;

import com.orange.matos.core.Alert;
import com.orange.matos.core.Configuration;
import com.orange.matos.core.DownloadParameters;
import com.orange.matos.core.ExtendedProperties;
import com.orange.matos.core.RuleFile;
import com.orange.matos.core.Step;
import com.orange.matos.utils.Downloader;
import com.orange.matos.utils.HtmlOutput;

/**
 * @author Pierre Cregut
 * Main class for the definition of the command line interface for MIDP
 */
public class JavaCoreCLI {

	/**
	 * Prefix for property specifying JAD.
	 */
	static public final String URL_JAD_PROPERTY = "Request-URL-";

	/**
	 * Launch analysis of the given step.
	 * @param step the step to analyse
	 * @param outStream the output
	 * @throws Alert
	 */
	public static void analyseJavaStep(Configuration configuration, JavaStep step, PrintStream outStream) throws Alert {	
		MidletSuite ms = new MidletSuite();
		Properties midletProperties = null; // jar manifest

		// if JAR is not provided, look for it into the JAD. Exit if
		// JAD is not provided either...
		if (! (step.hasJar() || step.hasJad())) {
			throw Alert.raised(null,"No JAD or JAR file provided");
		} 

		// load JAR if provided 
		if (step.hasJar()) {
			initializeWithJar(configuration, step,ms);
		}
		// load JAD if provided. If JAR is not provided, look for its location into the JAD
		if (step.hasJad()) {
			initializeWithJad(configuration, step,ms);
		}
		
		RuleFile rules = new RuleFile(step.getProfileName(), configuration.getProfileManager());

		if (ms.jarFile==null) { 
			throw Alert.raised(null,"Can't get any JAR file to analyse!"); 
		}
		else if (!ms.jarFile.exists()) { throw Alert.raised(null,"The JAR file specified can't be found: "+ms.jarFile.getAbsolutePath()); }
		else {
			JavaBase javaBase = new JavaBase(configuration);
			try {
				// Load JAR MANIFEST
				midletProperties = JavaBase.loadManifest(ms.jarFile,ms.properties);
			} catch (Alert a ) {
				// print java header
				javaBase.printJavaHeader(rules,ms, outStream);
				// print error message in report
				outStream.println(HtmlOutput.bold(HtmlOutput.color("red","Skipped"))+" : "+a.getMessage());
				// and go away by re-throwing the Alert
				throw a;
			}
			// If no midlet specified in command line, find all of them
			// in the JAD or in the manifest (if none is defined, it will not guess them).
			if (step.midletCount() == 0) {
				String def;
				for(int i=1; (def = midletProperties.getProperty("MIDlet-"+i)) != null; i++) {
					try {

						List <String> l = Configuration.parseCommaList(def);
						step.midletList.add(l.get(2)); 
					} catch (Exception e) {
						// print java header
						javaBase.printJavaHeader(rules,ms, outStream);
						// print error message in report
						String message = "MIDlet-"+ i + " attribute is not well-formed";
						outStream.println(HtmlOutput.bold(HtmlOutput.color("red","Skipped"))+" : "+message);
						// and go away by throwing the Alert
						throw Alert.raised(e, message);
					}
				}
			}

			// launch analysis
			Object midletNames [] = step.midletList.toArray();
			boolean success = javaBase.runJavaAnalysis(
						midletNames,ms, rules, outStream);
			step.setScore(javaBase.getScore());
			step.setTime(javaBase.getTime());
			if ((step.getCode()==null)||(step.getCode().equals(""))) {
				step.setCode(ms.jarFile.getAbsolutePath()); 
			}
			
			step.setVerdict(success ? Step.PASSED : Step.FAILED);

		}
	} 

	private static void initializeWithJad(Configuration configuration, JavaStep step, MidletSuite ms) throws Alert {
		try {
			ms.jadFile = Downloader.toLocalFile(configuration, step.getJad(), step.getParameters(), "tmpjad", "jad");
			if (step.getJad().startsWith("http:")) ms.jadURL = step.getJad();
		} catch (Alert e) {
			step.setVerdict(Step.SKIPPED);
			step.setMessage(e.getMessage());
			throw e;
		}

		//check that the jadfile is not an html file...

		ms.properties = loadJadProperties(ms.jadFile);
		if (ms.jarFile==null||!ms.jarFile.exists()) {
			// try to retrieve the JAR URL property
			getJarFileFromJad(configuration, ms,step);
		}
	}

	/**
	 * Load the properties contained in the provided file (supposed to be a JAD file). 
	 * Return a Properties object.
	 * @param jad a JAD file.
	 * @return the properties read from the provided file.
	 */
	public static Properties loadJadProperties(File jad) throws Alert{
		Properties props = new ExtendedProperties("JAD");
		
		try {
			if (jad!=null) {
				InputStream stream = new FileInputStream(jad);
				try {
					props.load(stream);
				} finally {
					stream.close();
				}
				return props;
			} else return props;
		} catch (FileNotFoundException e) {
			throw new Alert("Can't open JAD file "+jad.getAbsolutePath());
		} catch (IOException e) {
			throw new Alert("Can't load properties from JAD file "+jad.getAbsolutePath());
		}
	}

	private static void initializeWithJar(Configuration configuration, JavaStep step, MidletSuite ms) throws Alert {
		if (step.hasJad()) {
			ms.jadFile = initializeJADFile(configuration,step.getJad(), step.getParameters());
		}
		ms.jarFile = initializeJARFile(configuration, step.getCode(), getJadMIDletJarURLProperty(ms.jadFile), step.getParameters());
		ms.jarURL = step.getCode(); // used later 
	}

	private static File getJarFileFromJad(Configuration configuration, MidletSuite ms, JavaStep step) throws Alert {
		Properties jadProperties = ms.properties;
		String jarUrlProperty =	(jadProperties == null) ? null : jadProperties.getProperty("MIDlet-Jar-URL");
		ms.jarURL = jarUrlProperty;
		if (jarUrlProperty==null) {
			throw Alert.raised(null,"JAD file found but it contains no MIDlet-Jar-URL property (mandatory)");
		} else {
			// if the URL is an HTTP address, download the file in temp directory.
			String localJarFileUrl = "";
			if (jarUrlProperty.startsWith("http:")) {
				localJarFileUrl = jarUrlProperty; // used later, not only for download
				ms.jarFile = Downloader.httpDownload(configuration, localJarFileUrl, "tmpjar", "jar", step.getParameters());
			} else {
				int lastSlashIndex = step.getJad().lastIndexOf('/');	
				// if JAD was fetched using HTTP, we assume the JAR
				// path is relative to the JAD location, thus we try to
				// download the JAR from the same place.
				if (step.getJad().startsWith("http:")) {
					String urlExceptFile = step.getJad().substring(0,lastSlashIndex+1);
					localJarFileUrl = urlExceptFile+jarUrlProperty;
					ms.jarFile = Downloader.httpDownload(configuration, localJarFileUrl, "tmpjar", "jar" /*true*/, step.getParameters());

				} else {	
					// if JAD is in current dir (no '/' in its
					// path), JAR is assumed to be there too: JAR
					// url used as is. Else, we prefix it with the
					// path to the JAD (assuming JAR is located at
					// the same place.
					if (lastSlashIndex==-1) {
						localJarFileUrl = jarUrlProperty;
						ms.jarFile = new File(jarUrlProperty);
					}
					else {
						String urlExceptFile = step.getJad().substring(0,lastSlashIndex+1);
						localJarFileUrl = urlExceptFile+jarUrlProperty;
						ms.jarFile = new File(urlExceptFile+jarUrlProperty);
					}
					if (!ms.jarFile.exists()) {
						throw Alert.raised(null,"The JAR file specified into the MIDlet-Jar-URL property can't be found : "+jarUrlProperty);
					} 
				}
			} 
			step.setCode(ms.jarURL);//localJarFileUrl;
		}

		return null;
	}


	/**
	 * Returns the file at the given URI
	 * @param uri
	 * @param login if the file is distant and the connection requires an authentifiaction, the login
	 * @param password if the file is distant and the connection requires an authentifiaction, the password
	 * @param user_agent the user_agent to use to download the file. Use null if not needed 
	 * @return the file present at the given URI, or null if the file does not exist.
	 * @throws Alert
	 */
	public static File initializeJADFile(Configuration cfg, String uri, DownloadParameters dp) throws Alert {
		return Downloader.toLocalFile(cfg, uri, dp, "tmpjad", "jad");
	}

	/**
	 * Returns the file at the given URI
	 * @param uri
	 * @param urifromjad second chance uri, used if uri is down.
	 * @param login if the file is distant and the connection requires an authentifiaction, the login
	 * @param password if the file is distant and the connection requires an authentifiaction, the password
	 * @param user_agent the user_agent to use to download the file. Use null if not needed 
	 * @return the file present at the given URI
	 * @throws Alert
	 */
	public static File initializeJARFile(Configuration configuration, String uri, String urifromjad, DownloadParameters dp) throws Alert {
		return Downloader.toLocalFile(configuration, uri, dp, "tmpjar", "jar");
	}


	/**
	 * Retreives the value of "MIDlet-Jar-URL" property of a jad file.
	 * If it is just a file name without path, the path to the jad is added before the file name, 
	 * assuming they are in the same directory
	 * @param jadFile the jad file
	 * @return the value of the property (with eventually jad's path added in front of it) of null if it does not exists 
	 * @throws Alert if not able to read JAD property file
	 */
	public static String getJadMIDletJarURLProperty(File jadFile) throws Alert {
		if (jadFile!=null) {
			Properties jadProperties = new Properties();
			try {
				InputStream stream = new FileInputStream(jadFile);
				try {
					jadProperties.load(stream);
				} finally { stream.close(); }
			} catch (IOException e) {
				throw Alert.raised(e, "Cannot read JAD property file");
			}
			String MIDletJarURL = jadProperties.getProperty("MIDlet-Jar-URL");
			if ((MIDletJarURL!=null)&&(MIDletJarURL.indexOf(File.separator)<0)) { 
				// assume that jar is beside jad
				String path = jadFile.getParent();
				MIDletJarURL = path + File.separator + MIDletJarURL;
			}
			return MIDletJarURL;
		} else {
			return null;
		}
	}

}

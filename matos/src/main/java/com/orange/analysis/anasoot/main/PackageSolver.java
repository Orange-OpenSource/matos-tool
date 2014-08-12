/*
 * $Id: PackageSolver.java 2279 2013-12-11 14:45:44Z Pierre Cregut $
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
import java.io.PrintStream;
import java.util.Properties;

/**
 * Manage the MIDP components used by the midlet analyzed. 
 * @author Pierre Cregut
 *
 */
public class PackageSolver {


	protected String libs [];
	protected boolean commited [];

	private String midp_configuration, midp_profile;
	private String midpapi, cldcapi;


	/**
	 * Initialize the knowledge on the libraries with the complete classpath to find them. Creates
	 * the UI if it is a GUI based version.
	 * @param fullClassPath the classpath where to find the libraries (the path for each of them).
	 */
	protected void init (String fullClassPath, boolean b) {
		libs = fullClassPath.split(File.pathSeparator);
		commited = new boolean [libs.length];
		for(int i = 0 ; i < libs.length; i++){
			commited[i] = b;
		}
	}

	protected void init (String fullClassPath) {
		init(fullClassPath, true);
	}
	/**
	 * @param prop
	 */
	public void commit(Properties prop) {
		midp_configuration = prop.getProperty("MicroEdition-Configuration","CLDC-1.0");
		midp_profile = prop.getProperty("MicroEdition-Profile","MIDP-2.0");
		if (midp_configuration.equals("CLDC-1.0"))  cldcapi = "cldc10";
		else if (midp_configuration.equals("CLDC-1.1"))  cldcapi = "cldc11";
		else cldcapi="cldc10";

		if (midp_profile.equals("MIDP-1.0"))  midpapi = "midp10";
		else midpapi="midp20";		
	}


	/**
	 * Builds a classpath corresponding to the selection of libraries and configuration/profile
	 * @return a classpath in java format
	 */
	public String getClasspath() {
		StringBuffer result = new StringBuffer();

		result.append(find(cldcapi));
		result.append(File.pathSeparator);
		result.append(find(midpapi));

		for (int i = 0 ; i < libs.length; i ++) {
			if (commited[i]) {
				result.append(File.pathSeparator);
				result.append(libs[i]); 
			}
		}
		return result.toString();
	}

	/**
	 * gives back the configuration
	 * @return the version of CLDC in use 
	 */
	public String getConfiguration(){
		return cldcapi;
	}

	/**
	 * Gives back the profile
	 * @return the version of MIDP in use
	 */
	public String getProfile(){
		return midpapi;
	}

	/**
	 * Gives back the fullname knowing the short name
	 * @param s the short name
	 * @return empty string or the corresponding long name.
	 */
	String find(String s) {
		for(int i = 0; i < libs.length; i++) {
			if (s.equals(shortname(libs[i]))) return libs[i];
		}
		return "";
	}

	/**
	 * Shortname of a library
	 * @param s
	 * @return
	 */
	protected static String shortname(String s) {
		String basename = s.substring(s.lastIndexOf(File.separatorChar) + 1);
		return
			(basename.endsWith(".jar") ) ? basename.substring(0,basename.length() - 4) : basename;
	}

	/** 
	 * This function returns, knowing a commited state of the check-buttons,
	 * a list of initialization to include in the startup wrapper of the midlet
	 */	
	public void writeInit(PrintStream out) {
		out.println("\t" + "com.francetelecom.rd.fakemidp." + capitalize(cldcapi) + ".init();");
		out.println("\t" + "com.francetelecom.rd.fakemidp." + capitalize(midpapi) + ".init();");
		for (int i=0; i < libs.length; i++) 
			if (commited[i])
				out.println("\t" + "com.francetelecom.rd.fakemidp." + 
							capitalize(shortname(libs[i])) +".init();");
	}

	/**
	 * Prepare the initialization of libraries in the Jasmin wrapper that launch the environment.
	 * @param out the printstream used to write the wrapper.
	 */
	public void writeJasminInit(PrintStream out) {
		out.println("\tstaticinvoke <com.francetelecom.rd.fakemidp." + 
				capitalize(cldcapi) + ": void init()> ();");
		out.println("\tstaticinvoke <com.francetelecom.rd.fakemidp." +
				capitalize(midpapi) + ": void init()> ();");
		for (int i=0; i < libs.length; i++) 
			if (commited[i])
				out.println("\tstaticinvoke <com.francetelecom.rd.fakemidp." + 
							capitalize(shortname(libs[i])) + ": void init()> ();");
	}

	/**
	 * Capitalize the first letter of a string.
	 * @param s the input string (usually a configuration or profile.
	 * @return the output (the class)
	 */
	private static String capitalize(String s) {
		return 
			(s==null || s.length() == 0)
			? s 
			: s.substring(0,1).toUpperCase() + s.substring(1);
	}


}

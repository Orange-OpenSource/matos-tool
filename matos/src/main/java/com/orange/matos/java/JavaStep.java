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
import java.net.MalformedURLException;
import java.util.ArrayList;

import com.orange.matos.core.Alert;
import com.orange.matos.core.CmdLine;
import com.orange.matos.core.Configuration;
import com.orange.matos.core.Out;
import com.orange.matos.core.Step;
import com.orange.matos.utils.FileUtilities;

/**
 * @author Pierre Cregut
 * Represents a single midlet suite to analyse
 */
public final class JavaStep extends Step {
	
	/**
	 * List of midlets defined in the suite.
	 */
	public ArrayList <String> midletList = new ArrayList <String>();

	/**
	 * JAD file name
	 */
	private String jad = null;
	
	@Override
	public String toString() {
		String s = 
			"[jar="+(isEmpty(getCode())?"-":getCode())+
			" jad="+(isEmpty(getJad())?"-":getJad())+
			" out="+(isEmpty(getOutFileName())?"-":getOutFileName())+
			" profile="+(isEmpty(getProfileName())?"-":getProfileName())+
			"]";
		return s;
	}
	
	/**
	 * Is the JAR defined.
	 * @return
	 */
	public boolean hasJar() { return !isEmpty(getCode()); }
	/**
	 * Is the JAD defined
	 * @return
	 */
	public boolean hasJad() { return !isEmpty(getJad()); }
	/**
	 * Number of midlets in suite
	 * @return
	 */
	public int midletCount() { return midletList.size(); }
	
	@Override
	public Object clone()  {
		return super.clone();
	}

	/**
	 * Builds a CmdLine for a java analysis from options given on the command line.
	 * @param argv
	 * @return
	 * @throws Alert
	 */
	public static CmdLine parseJavaCmdLine(Configuration configuration, String[] argv) throws Alert {
		JavaStep javaStep = new JavaStep();
		CmdLine cmdLine = new CmdLine();
		try {
			for(int i=0; i < argv.length; i++) {
				String arg = argv[i].trim();
				if (arg.equals("-d")) javaStep.setProfileName(argv[++i].trim());
				else if (arg.equals("-h")){
					Out.getMain().println(CmdLine.help());
					cmdLine.setHelp(true);
				}
				else if (arg.equals("-xml")) continue;
				else if (arg.equals("-jar")) javaStep.setCode(argv[++i].trim());
				else if (arg.equals("-jad")) javaStep.setJad(argv[++i].trim());
				else if (arg.equals("-m")) javaStep.midletList.add(argv[++i].trim());
				else if (arg.equals("-o")) javaStep.setOutFileName(argv[++i].trim());
				else if (arg.equals("-css")) {
				        javaStep.setCssUrl(new File(argv[++i].trim()).toURI().toURL());
				}
				else if (arg.equals("-tmp"))javaStep.setTemporaryDir(argv[++i].trim());
				else if (arg.equals("-log"))javaStep.setLogFileName(argv[++i].trim());
				else if (arg.length() > 0 && arg.charAt(0) == '-') {
					Out.getMain().println(CmdLine.help());
					throw Alert.raised(null,"Unrecognized option : " + arg);
				} else {  // not a flag
					if (FileUtilities.checkSuffix(arg,".jad")) javaStep.setJad(arg);
					else if (FileUtilities.checkSuffix(arg,".jar")) javaStep.setCode(arg);
					else {
						Out.getMain().println(CmdLine.help());
						throw Alert.raised(null,"Unknown file type (use -jar or -jad, or pass a file with .jar or .jad extension): "+arg);
					}
				}
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			Out.getMain().println(CmdLine.help());
			throw Alert.raised(e,"Arguments are missing...");
		} catch (MalformedURLException e) {
		    throw Alert.raised(e,"Bad CSS...");
        }
		// retrieve default security profile name
		if (!javaStep.hasProfile()) {
			javaStep.setProfileName(configuration.string(Configuration.DEFAULT_RULES_KEY));
			if (!javaStep.hasProfile()) throw Alert.raised(null,"No security profile specified, and no default one defined in your configuration file either!");
		}
		cmdLine.setStep(javaStep);
		return cmdLine;
	}

    /**
     * Gets the jad.
     *
     * @return the jad
     */
    public String getJad() {
        return jad;
    }

    /**
     * Sets the jad.
     *
     * @param jad the new jad
     */
    public void setJad(String jad) {
        this.jad = jad;
    }

}

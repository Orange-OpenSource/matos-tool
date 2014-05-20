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
import java.net.MalformedURLException;

import com.orange.matos.core.Alert;
import com.orange.matos.core.CmdLine;
import com.orange.matos.core.Configuration;
import com.orange.matos.core.Out;
import com.orange.matos.core.Step;
import com.orange.matos.utils.FileUtilities;

/**
 * @author piac6784
 * A single application analysis.
 */
public final class AndroidStep extends Step {

    /**
	 * Is the file defined
	 * @return
	 */
	public boolean hasAndroidFile(){ return !isEmpty(getCode()); }

	@Override
	public String toString() {
		String s = 
			"[android file="+(isEmpty(getCode())?"-":getCode())+
			" out="+(isEmpty(getOutFileName())?"-":getOutFileName())+
			" profile="+(isEmpty(getProfileName())?"-":getProfileName())+
			"]" + super.toString();
		return s;
	}

	@Override
	public Object clone() {
		return super.clone();
	}

	/**
	 * Builds a CmdLine for a Android analysis from options given on the command line.
	 * @param argv
	 * @return
	 * @throws Alert
	 */
	public static CmdLine parseAndroidCmdLine(Configuration configuration, String[] argv) throws Alert {
		AndroidStep androidStep = new AndroidStep();
        CmdLine cmdLine = new CmdLine();
        try {
			for(int i=0; i < argv.length; i++) {
				String arg = argv[i].trim();
				if (arg.equals("-d")) androidStep.setProfileName(argv[++i].trim());
				else if (arg.equals("-h")){
					Out.getMain().println(CmdLine.help());
					cmdLine.setHelp(true);
				}
				else if (arg.equals("-jar")) {
					String msg = "ignore option \"-jar " + argv[++i].trim() + "\"";
					Out.getLog().println(msg);
					Out.getMain().println(msg);
				}
				else if (arg.equals("-jad")) {
					String msg = "ignore option \"-jad " + argv[++i].trim() + "\"";
					Out.getLog().println(msg);
					Out.getMain().println(msg);
				}
				else if (arg.equals("-xml")) continue;
				else if (arg.equals("-apk")) androidStep.setCode(argv[++i].trim());
				else if (arg.equals("-o")) androidStep.setOutFileName(argv[++i].trim());
				else if (arg.equals("-css")) {
				    androidStep.setCssUrl(new File(argv[++i].trim()).toURI().toURL());
				}
				else if (arg.equals("-tmp"))androidStep.setTemporaryDir(argv[++i].trim());
				else if (arg.equals("-log"))androidStep.setLogFileName(argv[++i].trim());
				else if (arg.length() > 0 && arg.charAt(0) == '-') {
					Out.getMain().println(CmdLine.help());
					throw Alert.raised(null,"Unrecognized option : " + arg);
				} else {  // not a flag
					if (FileUtilities.checkSuffix(arg,".apk")) androidStep.setCode(arg);
					else {
						Out.getMain().println(CmdLine.help());
						throw Alert.raised(null,"Unknown file type (use -apk, or pass a file with .swf or .sis extension): "+arg);
					}
				}
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			Out.getMain().println(CmdLine.help());
			throw Alert.raised(e,"Arguments are missing...");
		} catch (MalformedURLException e) {
            throw Alert.raised(e,"Bad CSS file...");
        }
		// retrieve default security profile name
		if (!androidStep.hasProfile()) {
			androidStep.setProfileName(configuration.string(Configuration.DEFAULT_RULES_KEY));
			if (!androidStep.hasProfile()) throw Alert.raised(null,"No security profile specified, and no default one defined in your configuration file either!");
		}


		cmdLine.setStep(androidStep);
		return cmdLine;
	}


}

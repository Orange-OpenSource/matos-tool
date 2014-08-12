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
import java.io.PrintStream;

import com.orange.matos.core.Alert;
import com.orange.matos.core.Configuration;
import com.orange.matos.core.DownloadParameters;
import com.orange.matos.core.RuleFile;
import com.orange.matos.core.Step;
import com.orange.matos.utils.Downloader;

/**
 * @author Pierre Cregut
 * Command line interface for Android analysis.
 */
public class AndroidCoreCLI {
	/**
	 * Launch analysis of the given android step.
	 * @param step the step to analyse
	 * @param outStream the output
	 * @throws Alert
	 */
	public static void analyseAndroidStep(Configuration configuration, AndroidStep step,  PrintStream outStream) throws Alert {
		File androidFile = null;


		if (! step.hasAndroidFile()) {
			throw Alert.raised(null,"No APK File provided");
		} else {
			androidFile = initializeAndroidFile(configuration, step.getCode(), step.getParameters());

			RuleFile rules = new RuleFile(step.getProfileName(), configuration.getProfileManager());

			if (!androidFile.exists()) { throw Alert.raised(null,"The Android file specified can't be found: "+androidFile.getAbsolutePath()); }
			else {
				AndroidBase androidBase = new AndroidBase(configuration);
				// launch android analysis
				boolean success = androidBase.runAndroidAnalysis(androidFile, step.getCode(), rules, outStream);
	            step.setScore(androidBase.getScore());
	            step.setTime(androidBase.getTime());
				step.setMessage(androidBase.getMessage());
				if ((step.getCode()==null)||(step.getCode().equals(""))) {
					step.setCode(androidFile.getAbsolutePath()); 
				}
				if (success){
					step.setVerdict(Step.PASSED);
				}else{
					step.setVerdict(Step.FAILED);
				}
			}

		}
	}


	/**
	 * Returns the file at the given URI
	 * @param uri
	 * @param login if the file is distant and the connection requires an authentifiaction, the login
	 * @param password if the file is distant and the connection requires an authentifiaction, the password
	 * @param user_agent the user_agent to use to download the file. Use null if not needed 
	 * @return the file present at the given URI
	 * @throws Alert
	 */
	public static File initializeAndroidFile(Configuration configuration, String uri, DownloadParameters dp) throws Alert {
		return Downloader.toLocalFile(configuration, uri, dp, "tmpAndroid", "apk");
	}


	/**
	 * Compute the short name of a file from its URI.
	 * @param uri file URI
	 * @return a string that is the short name.
	 */
	public static String guessName(String uri) {
		String name = "";
		if (uri.startsWith("http:") && (uri.length()-1)>uri.lastIndexOf("/")){
			name = uri.substring(uri.lastIndexOf("/")+1);//, uri.length()-1);
		}else if (uri.lastIndexOf(File.separator)!=-1 && (uri.length()-1)>uri.lastIndexOf(File.separator)){
			name = uri.substring(uri.lastIndexOf(File.separator)+1);//, uri.length()-1);
		}else{						
			name = uri;
		}
		return name;
	}

}

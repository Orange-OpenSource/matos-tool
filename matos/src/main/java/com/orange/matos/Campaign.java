/*
 * $Id: Campaign.java 2285 2013-12-13 13:07:22Z piac6784 $
 */
// ALERT DONE
package com.orange.matos;

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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;

import org.w3c.dom.Element;

import com.orange.matos.android.AndroidStep;
import com.orange.matos.core.Alert;
import com.orange.matos.core.Configuration;
import com.orange.matos.core.DownloadParameters;
import com.orange.matos.core.Step;
import com.orange.matos.java.JavaStep;

/**
 * This class represents a check-list.
 * @author apenault
 *
 */

public class Campaign extends ArrayList <Step> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String name = "?";
	private String author = "?";
	private String date = "?";
	private String description = "?";
	private String profile = "?";
	private double passedRate = -1;
	private double failedRate = -1;
	private double skippedRate = -1;
	transient final private Configuration configuration;
	
	/**
	 * Instantiates a new campaign.
	 *
	 * @param cfg the cfg
	 */
	public Campaign(Configuration cfg) {
		configuration = cfg;
	}
		
	
	/**
	 * Get security profile
	 * @return
	 */
	String getProfile(){
		return profile;
	}
			
	/**
	 * Read a campaign file, and return a simplified representation of 
	 * what is to be done (abstraction of a campaign).
	 * @param campaignFile The campaign file to read.
	 * @return A campaign structure
	 */
	public void readCampaign(File campaignFile) throws Alert{
		
		CampaignXMLParser parser = new CampaignXMLParser();
		parser.readCampaignFile(campaignFile);
		String stepName;
		
		// get root
		Element root = parser.root;
		name = root.getAttribute("name");
		author = root.getAttribute("author");
		date = root.getAttribute("date");
		description = root.getAttribute("description");
		profile = root.getAttribute("profile");
		
		// get ana elements
		Element[] javaStepList = parser.getElements(root, "anajava");

		// retrieve default security profile name
		String defProfileName = configuration.string(Configuration.DEFAULT_RULES_KEY);
		
		// process every anajava
		for (int i=0; i<javaStepList.length; i++) {
			
			JavaStep javaStep = new JavaStep();
						
			// Get name of the step
			stepName = javaStepList[i].getAttribute("name");
			
			// Get JAD & JAR URLs (prefix with basedir if not an HTTP
			// address. The wellformedness of HTTP addresses is also checked).
			javaStep.setJad(javaStepList[i].getAttribute("jad"));
			if (javaStep.hasJad()) {
				javaStep.setJad(javaStep.getJad().trim()); // remove spaces
				if (javaStep.getJad().startsWith("http:")) {
					try {
						new URL(javaStep.getJad()); 
					}catch (MalformedURLException e) {
						throw Alert.raised(e, "Campaign step number "+i+" -> Invalid URL: "+javaStep.getJad());
					} 
				}
			}
			javaStep.setCode(javaStepList[i].getAttribute("jar"));
			if (javaStep.hasJar()) {
				javaStep.setCode(javaStep.getCode().trim()); 
				if (javaStep.getCode().startsWith("http:")) {
					try {
						new URL(javaStep.getCode()); 
					}catch (MalformedURLException e) {
						throw Alert.raised(e, "Campaign step number "+i+", invalid URL: "+javaStep.getCode());
					} 
				}
			}
			DownloadParameters dp = javaStep.getParameters();
			dp.setLogin(javaStepList[i].getAttribute("login"));
			dp.setPassword(javaStepList[i].getAttribute("password"));
			dp.setUser_agent(javaStepList[i].getAttribute("useragent"));
			
			// Security profile (rule file). Provide default one if not specified.
			initProfile(javaStep, defProfileName);
			
			// Midlets list
			Element[] midlets = parser.getElements(javaStepList[i],"midlet");
			for (int m=0; m<midlets.length; m++) {
				String midletName = midlets[m].getAttribute("name");
				javaStep.midletList.add(midletName);
			}
			
			// Output file : re-uses the 'name' attribute, after
			// testing its validity
			initOutputFile(javaStep, stepName, i);			
			
			// add to campaign
			add(javaStep);
		}  
		
		
		Element[] anaDroidList = parser.getElements(root, "anadroid");
		
		// process every anadroid
		for (int i=0; i<anaDroidList.length; i++) {
			AndroidStep droidStep = new AndroidStep();
			
			// initializes the type of file to analyse
			
			// Get name of the step
			stepName = anaDroidList[i].getAttribute("name");
			
			// Get .sis or .swf file
			droidStep.setCode(anaDroidList[i].getAttribute("file"));
			if (droidStep.hasAndroidFile()) {
				droidStep.setCode(droidStep.getCode().trim()); // remove spaces
				if (droidStep.getCode().startsWith("http:")) {
					try {
						new URL(droidStep.getCode()); 
					}catch (MalformedURLException e) {
						throw Alert.raised(e, "Campaign step number "+i+" -> Invalid URL: "+droidStep.getCode());
					} 
				}
			}
			
	        DownloadParameters dp = droidStep.getParameters();
			dp.setLogin(anaDroidList[i].getAttribute("login"));
			dp.setPassword(anaDroidList[i].getAttribute("password"));
			dp.setUser_agent(anaDroidList[i].getAttribute("useragent"));
			
			initProfile(droidStep, defProfileName);
			
			// Output file : re-uses the 'name' attribute, after
			// testing its validity
			initOutputFile(droidStep, stepName, i+javaStepList.length);
			
			// add to campaign
			add(droidStep);
		}
	}
	
	/**
	 * Initializes the "outputFile" field of the "CmdLine" object with testing its validity.
	 * @param step the command line to update
	 * @param stepName the name of the step
	 * @param stepNumber the number of the step
	 * @throws Alert
	 */
	private void initOutputFile(Step step, String stepName, int stepNumber) throws Alert {
		try {  
			new URI(stepName);
			step.setOutFileName(stepName + ".html");
		} catch (URISyntaxException e) {
			throw Alert.raised(e,"Campaign step number "+stepNumber+", invalid step name: "+stepName+". Can't create an output file based on that name.");
		}
	}

	/**
	 * Initializes the profile name in the given command line
	 * @param step the command line to update
	 * @param defProfileName the default profile
	 * @throws Alert
	 */
	private void initProfile(Step step, String defProfileName) throws Alert {
		step.setProfileName(profile);
		if (!step.hasProfile()) {
			step.setProfileName(defProfileName);
			if (!step.hasProfile()) throw Alert.raised(null,"No default security profile defined in your configuration file. Please define 'anasoot.ruleDefaultFile' or set one when calling MATOS (see help -h).");
		}
	}

	/**
	 * Save the campaign to a file.
	 *
	 * @param fileURI the file uri
	 * @param profile the profile
	 * @throws Alert the alert
	 */
	public void save(String fileURI, String profile) throws Alert{
		CampaignXMLParser parser = new CampaignXMLParser();
		parser.createDocument(this, profile);
		parser.writeCampaignFile(fileURI);
	}
			
	
	
	/**
	 * Computes rates for steps with passed verdict, failed verdict and skipped verdict.
	 *
	 */
	public void computeRates(){
		int passed = 0;
        int failed = 0;
        int skipped = 0;
        int notAnalyzed = 0;
        for (int i=0; i<size(); i++){
        	Step cmdLine = (Step)get(i);
        	if (cmdLine.getVerdict() == Step.PASSED){
        		passed++;
        	}else if (cmdLine.getVerdict() == Step.FAILED){
        		failed++;
        	}else if (cmdLine.getVerdict() == Step.SKIPPED){
        		skipped++;
        	}else if (cmdLine.getVerdict() == Step.NONE){
        		notAnalyzed++;
        	}
        }
        int total = passed+failed+skipped+notAnalyzed;
        passedRate = passed * 100. / total;
        failedRate = failed * 100. / total;
        skippedRate = skipped * 100. / total;
	}

	/**
	 * Gets the percentage of failed steps
	 *
	 * @return the failed rate
	 */
	public double getFailedRate() {
		if (failedRate < 0) computeRates();
		return failedRate;
	}

	/**
	 * Gets the percentage of passed steps.
	 *
	 * @return the passed rate
	 */
	public double getPassedRate() {
		if (passedRate < 0) computeRates();
		return passedRate;
	}

	/**
	 * Gets the percentage of skipped steps.
	 *
	 * @return the skipped rate
	 */
	public double getSkippedRate() {
		if (skippedRate < 0) computeRates();
		return skippedRate;
	}

	String getDescription() {
		return description;
	}

	String getAuthor() {
		return author;
	}

	String getName() {
		return name;
	}
	
	String getDate() {
		return date;
	}
	
}


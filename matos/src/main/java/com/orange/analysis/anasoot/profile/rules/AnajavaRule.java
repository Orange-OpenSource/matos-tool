/*
 * $Id:AnaRule.java 917 2006-09-27 10:15:16 +0200 (mer., 27 sept. 2006) penaulau $
 */
package com.orange.analysis.anasoot.profile.rules;

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
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.orange.analysis.anasoot.AnasootConfig;
import com.orange.analysis.anasoot.printing.GlobalReport;
import com.orange.analysis.anasoot.printing.JavaReport;
import com.orange.analysis.anasoot.printing.ReportUse;
import com.orange.analysis.anasoot.printing.ScoreReport;
import com.orange.analysis.anasoot.printing.StructureReport;
import com.orange.analysis.anasoot.printing.UnresolvedReport;
import com.orange.analysis.anasoot.printing.UsedJSRReport;
import com.orange.matos.core.Alert;
import com.orange.matos.core.Configuration;
import com.orange.matos.core.Out;
import com.orange.matos.core.RuleFile;
import com.orange.matos.core.XMLParser;


/**
 * @author piac6784
 * Description of the set of rules for a MIDP application. Can be
 * specialized for Android.
 */
public class AnajavaRule {

	/**
	 * The set of rules.
	 */
	public Map <String,JavaRule> rules;
	/**
	 * Mapping between classes and unresolved JSR. To be modified.
	 */
	public Map <String, String> unresolvedJSR;
	/**
	 * Mapping between classes and JSR.
	 */
	public Map <String,String> listOfJSR;
	
	/**
	 * List of custom checkers to use. 
	 */
	public List<String>customCheckers = new ArrayList<String>();
	

	
	private XMLParser parser;
	private GlobalReport global;
	protected String configuration;
	protected String profile;

	RuleFile rulefile;
	
	static final String RULE_KIND = "rule";
	static final String REPORT_KIND = "report";
	static final String STRUCTURE_KIND = "structure";
	static final String SCORE_KIND = "score";
	static final String CUSTOM_RULE_KIND = "custom";

	static final String UNRESOLVED_JSR_REPORT = "unresolved";
	static final String USED_JSR_REPORT = "usedJSR";

	private void initJSRResolution(boolean doIt) throws Alert {
		//Add unresolved report
		JavaReport report1 = new UnresolvedReport(UNRESOLVED_JSR_REPORT);
		global.put(UNRESOLVED_JSR_REPORT, report1);

		//add used JSR report
		JavaReport report2 = new UsedJSRReport(USED_JSR_REPORT, doIt);
		global.put(USED_JSR_REPORT, report2);

		//Add unresolved and used jsr in hashTables
		unresolvedJSR = new HashMap<String, String>();
		listOfJSR = new HashMap<String, String>();
		if (doIt) {
			File apiDirectory = new File(System.getProperty("LIB")+File.separator+"api");
			FilenameFilter xmlFilter = new FilenameFilter(){
				@Override
				public boolean accept(File dir, String name){
					return name.endsWith(".xml");
				}
			};
			String [] xmlFiles = apiDirectory.list(xmlFilter);
			if (xmlFiles == null) return;
			for (int i=0; i<xmlFiles.length; i++){
				String xmlPath = System.getProperty("LIB")+File.separator+"api"+File.separator+xmlFiles[i];
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				Document doc_xml = null;
				try{
					DocumentBuilder builder = factory.newDocumentBuilder();
					doc_xml = builder.parse(new File(xmlPath));
				}catch (Exception e){
					e.printStackTrace(Out.getMain());
					throw new Alert("Unable to load unresolved JSRs: "+e.getMessage());
				}
				Element jsrElement = doc_xml.getDocumentElement();
				String jsrName = jsrElement.getAttribute("name");
				boolean resolved = Boolean.parseBoolean(jsrElement.getAttribute("resolved"));
				NodeList classes = jsrElement.getElementsByTagName("class");
				for (int j=0; j<classes.getLength(); j++){
					Element classElem = (Element)classes.item(j);
					String classname = classElem.getAttribute("name");
					if (!resolved) unresolvedJSR.put(classname, jsrName);
					listOfJSR.put(classname, jsrName);
				}
			}		
		}
	}
	
	/**
	 * Initialize the structure
	 * 
	 * @param rulefile rule file to parse from
	 * @param config configuration
	 * @throws IOException
	 * @throws Alert
	 */
	private void init(RuleFile rulefile, AnasootConfig config) throws IOException, Alert{
		if (rulefile == null) {	throw Alert.raised(null, "No rule file given. I quit."); }
		this.rulefile = rulefile;
		rules = new HashMap<String,JavaRule>();
		parser = rulefile.getParser();

		Element ruleDefs [] = parser.getKind(RULE_KIND);
		Element reportDefs [] = parser.getKind(REPORT_KIND);
		Element structureDef [] = parser.getKind(STRUCTURE_KIND);
		Element scoreDef [] = parser.getKind(SCORE_KIND);
		Element customRules [] = parser.getKind(CUSTOM_RULE_KIND);
		global = new GlobalReport();
		if (structureDef.length == 1) { global.setStructureReport(new StructureReport(structureDef[0])); }
		
		if (scoreDef.length == 1) {	global.setScore(new ScoreReport(scoreDef[0])); }
		
		for(Element rep : reportDefs) {
			JavaReport.parse(rep,global, parser, profile);
		}	

		for(Element custom: customRules) {
			String classname = custom.getAttribute("name");
			customCheckers.add(classname);
		}
		initJSRResolution(config.doUsedJSR());
		parseRules(ruleDefs);
	}

	private void parseRules(Element [] ruleDefs) throws IOException {
		for(int i=0; i < ruleDefs.length; i++) {
			Element rule=ruleDefs[i];
			Element impl;
			String ruleName = rule.getAttribute("name");	    
			if ((impl=parser.getElement(rule,"args")) != null) {
				String className = impl.getAttribute("class");
				String signature = impl.getAttribute("signature");
				String reportName = impl.getAttribute("report");
				JavaReport report =  global.get(reportName);
				if (report==null && !reportName.equals("terse")) {
					String msg = 
						"Invalid report name " + reportName + 
						" in definition of rule " + ruleName;
					throw new IOException(msg);
				}
				Element args[] = XMLParser.getElements(impl,"argument");
				ArrayList<Integer> positionArgs = new ArrayList<Integer> ();
				for(int j=0; j < args.length; j++) {
					Integer v = new Integer(args[j].getAttribute("position"));
					positionArgs.add(v);
				}
				JavaRule r = new CallRule(ruleName,className,signature,
						report,positionArgs);
				rules.put(ruleName,r);
			} else if ((impl=parser.getElement(rule,"return")) != null) {
				String className = impl.getAttribute("class");
				String signature = impl.getAttribute("signature");
				String reportName = impl.getAttribute("report");
				JavaReport jr = (reportName == null) ? null : global.get(reportName);
				JavaRule r = new ReturnRule(ruleName,className,signature, jr);
				rules.put(ruleName,r);
			} else if ((impl=parser.getElement(rule,"field")) != null) {
				String className = impl.getAttribute("class");
				String fieldType = impl.getAttribute("type");
				String reportName = impl.getAttribute("report");
				JavaReport jr = (reportName == null) ? null : global.get(reportName);
				JavaRule r = new FieldRule(ruleName,className,fieldType, jr);
				rules.put(ruleName,r);
			} else if ((impl=parser.getElement(rule,"use")) != null) {
				String className = impl.getAttribute("class");
				String signature = impl.getAttribute("signature");
				String reportName = impl.getAttribute("report");
				JavaReport jr =  global.get(reportName);
				if (jr instanceof ReportUse) {
					ReportUse reportUse = (ReportUse) jr;
					JavaRule r = new UseRule(ruleName,className,signature, reportUse);
					rules.put(ruleName,r);
				}
			}
		}
	}
	
	/**
	 * @param rulefile
	 * @param configuration
	 * @param profile
	 * @param config
	 * @throws IOException
	 * @throws Alert
	 */
	public AnajavaRule(RuleFile rulefile, String configuration, String profile, AnasootConfig config) throws IOException, Alert{
		this.configuration = configuration;
		this.profile = profile;
		init(rulefile, config);
	}

	/**
	 * Activate a given global configuration of the tool
	 * @param conf
	 */
	public void activate(Configuration conf) {
		rulefile.activate(conf);
	}


	/**
	 * Get back the list of rules.
	 * @return
	 */

	public Collection<JavaRule> getRules () { return rules.values(); }
	
	/**
	 * Set the MIDP configuration name 
	 * @param configuration
	 */
	public void setConfiguration(String configuration) {
		this.configuration = configuration;
	}

	/**
	 * Get the MIDP configuration name
	 * @return
	 */
	public String getConfiguration(){
		if (configuration.equals("cldc10")){
			return "CLDC 1.0 (JSR 30)";
		} else {
			return "CLDC 1.1 (JSR 139)";
		}
	}

	/**
	 * Set the MIDP profile.
	 * @param profile
	 */
	public void setProfile(String profile) {
		this.profile = profile;
	}

	/**
	 * Get the MIDP profile
	 * @return
	 */
	public String getProfile(){
		if (profile.equals("midp10")){
			return "MIDP 1.0 (JSR 37)";
		} else {
			return "MIDP 2.0 (JSR 118)";
		}
	}

	
	/**
	 * Returns the list of reports.
	 * @return
	 */
	public Collection<JavaReport> getReports() { return global.getReports().values(); }

    /**
     * Gets the global report
     * @return
     */
    public GlobalReport getGlobal() { return global; }

}

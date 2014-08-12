package com.orange.analysis.anasoot.printing;

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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

import com.orange.analysis.anasoot.AnasootConfig;
import com.orange.analysis.anasoot.result.NodeTable;
import com.orange.analysis.anasoot.spy.SpyField;
import com.orange.analysis.anasoot.spy.SpyMethod;
import com.orange.analysis.anasoot.spy.SpyResult;
import com.orange.analysis.anasoot.spy.SpyReturn;
import com.orange.matos.core.Alert;
import com.orange.matos.core.XMLStream;
import com.orange.matos.utils.HtmlOutput;

/**
 * The global structure of the Anasoot report as a HTML file.
 * @author Pierre Cregut
 *
 */
public class StructureReport {
	private static final String CATCH_ALL = "*";
	private static final String STRUCTURED_DIV_KEY = "sdiv";
	private static final String REPORT_KEY = "reportRef";
	private static final String FIELD_KEY = "fieldRef";
	private static final String RETURN_KEY = "returnRef";
	private static final String NODES_KEY = "nodes";
	private static final String CALL_KEY = "callRef";
	private static final String NAME_ATTR = "name";
	private static final String CUSTOM_KEY = "customOutput";
	
	private final Element contents;
	private ScoreReport scoreReport;
	
	private PrintStream out;
	private NodeTable nodeTable;
	private SpyResult result;
	private Set<String> done = new HashSet<String>();
    private Map<String, JavaReport> reports;
	
	/**
	 * @param element The XML element to parse to get the structure of report.
	 */
	public StructureReport(Element element) {
		contents = element;
	}


	/**
	 * Dumps the phantom classes in use in HTML format (only).
	 * @param out the stream to print to.
	 */
	private static void dump_phantoms(PrintStream out, boolean xmlFormat) {
		Scene scene = Scene.v();
		ArrayList<String> bufC = new ArrayList<String>();
		ArrayList<String> bufM = new ArrayList<String>();
		for(Iterator <SootClass> it = scene.getClasses().iterator(); it.hasNext(); ) {
			SootClass c = it.next();
			if (c.isPhantom()) {
				bufC.add(HtmlOutput.escape(c.toString()));
			} else {
				for(Iterator <SootMethod> it2 = c.getMethods().iterator() ; it2.hasNext();){
					SootMethod m = (SootMethod) it2.next();
					if (m.isPhantom()) bufM.add(HtmlOutput.escape(m.toString()));
				}
			}
		}
		if (xmlFormat) {
			XMLStream xmlout = new XMLStream(out);
			if (bufC.size() > 0) {
				xmlout.element("phantomClass");
				for(String c: bufC) {
					xmlout.element("class");
					xmlout.attribute(NAME_ATTR, c);
					xmlout.endElement();
				}
				xmlout.endElement();
			}
			if (bufM.size() > 0) {
				xmlout.element("phantomMethod");
				for(String c: bufM) {
					xmlout.element("method");
					xmlout.attribute("signature", c);
					xmlout.endElement();
				}
				xmlout.endElement();
			}
		}
		// TODO Find w way to propagate to hasOut in AnasootPhase.
		if (bufC.size() > 0) {
			out.println(HtmlOutput.header(3,HtmlOutput.color("red","Warning: analysis done without actual implementations for the following classes:")));
			out.println(HtmlOutput.list(bufC.toArray()));
		}
		if (bufM.size() > 0) {
			out.println(HtmlOutput.header(3,HtmlOutput.color("red","Warning: analysis done without actual implementations for the following methods:")));
			out.println(HtmlOutput.list(bufM.toArray()));
		}
	}

	/**
	 * Prints the result of the Anasoot Phase. It relies on the configuration of the tool,
	 * the call context that summarizes the node used.
	 * @param acf
	 * @param cc
	 * @param structure
	 * @param result
	 * @param out
	 * @throws Alert
	 */
	public static void dump (AnasootConfig acf, GlobalReport global, 
						     SpyResult result, PrintStream out) throws Alert {
	    StructureReport structure = global.getStructure();
	    ScoreReport score = global.getScore();
	    Map<String,JavaReport> reports = global.getReports();
		boolean xmlFormat = acf.xmlFormat();
		// Reset the tables for last seen.
		ReportMessage.resetLast();
		// HACK TO GO AROUND WRONG REPORTS BECAUSE OF PHANTOM REFS
		dump_phantoms(out, xmlFormat);
		if (structure == null || xmlFormat) {
			for (SpyMethod call_rule : result.callUses.values()) {
				call_rule.dump(out, xmlFormat);
			}
			for (SpyReturn sr : result.returns.values()) {
				sr.dump(out, xmlFormat);
			}
			for (SpyField sf : result.fields.values()) {
				sf.dump (out, xmlFormat);
			}
			for (JavaReport jr : reports.values()) {
				jr.tellAll(out);
			}
			if (acf.doPrintNodes()) NodeReport.dumpNodes(out, xmlFormat, result.nodeTable);
		} else {
			structure.dump(out, score, reports, result);
		}
		
	}
	
	private void dump(PrintStream rawOut, ScoreReport score, Map<String,JavaReport> reports, SpyResult res) throws Alert {
		scoreReport = score;
		this.reports = reports;
		this.result = res;
		if (score != null) {
			for(String ruleName: result.callUses.keySet()) score.matchRule(ruleName);
			for(String ruleName: result.returns.keySet()) score.matchRule(ruleName);
			for(String ruleName: result.fields.keySet()) score.matchRule(ruleName);
		}
		this.out = (score != null) ? score.wrap(rawOut) : rawOut;
		this.nodeTable = result.nodeTable;
		dumpNodeList(contents.getChildNodes());
		for (SpyMethod call_rule : result.callUses.values()) { if (call_rule.useful() && !done.contains(call_rule.getName())) { call_rule.dump(out, false);} }
		for (SpyReturn sr : result.returns.values()) {if (sr.useful() && !done.contains(sr.getName())) sr.dump(out, false); }
		for (SpyField sf : result.fields.values()) { if (sf.useful() && !done.contains(sf.getName())) sf.dump (out, false);	}
		for (JavaReport jr : reports.values()) { if (!done.contains(jr.getName())) jr.tellAll (out); }
		if (score != null) score.tell(out, false);
	}


	private void dumpNodeList(NodeList childNodes) throws Alert {
		int l = childNodes.getLength();
		for(int i=0; i < l; i++) {
			dumpNode(childNodes.item(i));
		}
	}


	private void dumpNode(Node item) throws Alert {
		String name = item.getNodeName();
		if (name != null && name.length() > 1 && name.charAt(0) == '#') {
			if (name.equals("#comment")) return;
			else out.println(item.getNodeValue()); // Works for text and cdata-section
		} else if (item instanceof Element) {
			Element elt = (Element) item;
			if (name == null) dumpRegularNode(item, "noname");
			else if (name.equals(STRUCTURED_DIV_KEY)) {
				if (checkUseful(elt)) {
					String divName = elt.getAttribute(NAME_ATTR);
					if (divName != null && scoreReport != null) scoreReport.matchRule(divName); 
					dumpNodeList(item.getChildNodes()); 
				}
			} else if (name.equals(CALL_KEY)) {
				String ruleName = elt.getAttribute(NAME_ATTR);
				if (ruleName == null || ruleName.equals(CATCH_ALL)) {
					for (SpyMethod call_rule : result.callUses.values()) { 
						if(done.contains(call_rule.getName())) continue;
						call_rule.dump(out, false); 
					}
				} else {
					SpyMethod callRule = result.callUses.get(ruleName);
					if (callRule != null) { 
						callRule.dump(out, false);
						done .add(ruleName);
					}
				}
			} else if (name.equals(RETURN_KEY)) {
				String ruleName = elt.getAttribute(NAME_ATTR);
				if (ruleName == null || ruleName.equals(CATCH_ALL)) {
					for (SpyReturn sr : result.returns.values()) {
						if(done.contains(sr.getName())) continue;
						sr.dump(out, false); 
					}
				} else {
					SpyReturn returnRule = result.returns.get(ruleName);
					if (returnRule != null) {
						returnRule.dump(out, false);
						done.add(ruleName);
					}
				}
			} else if (name.equals (FIELD_KEY)) {
				String ruleName = elt.getAttribute(NAME_ATTR);
				if (ruleName == null || ruleName.equals(CATCH_ALL)) {
					for (SpyField sf : result.fields.values()) { 
						if(done.contains(sf.getName())) continue;
						sf.dump (out, false);	
					}
				} else {
					SpyField fieldRule = result.fields.get(ruleName);
					if (fieldRule != null) {
						fieldRule.dump(out, false);
						done.add(ruleName);
					}
				}
			} else if (name.equals(REPORT_KEY)) {
				String reportName = elt.getAttribute(NAME_ATTR);
				if (reportName == null || reportName.equals(CATCH_ALL)) {
					for (JavaReport jr : reports.values()) {
						if(done.contains(jr.getName())) continue;
						jr.tellAll (out); 
					}
				} else {
					JavaReport report = reports.get(reportName);
					if (report != null) {
						report.tellAll(out);
						done.add(reportName);
					}
				}
			} else if (name.equals(NODES_KEY)) {
				NodeReport.dumpNodes(out, false, nodeTable);
			} else if (name.equals(CUSTOM_KEY)) {
				ArrayList<Element> template = new ArrayList<Element>();
				template.add((Element) item);
				ParameterizedOutput.output(out, template, result.customResults);
			} else {
				dumpRegularNode(item, name);
			}
		}		
	}

	private boolean checkUseful(Element elt, String key, Map <String, ?> container) {
		NodeList list = elt.getElementsByTagName(key);
		int l = list.getLength();
		for(int i=0; i < l; i++) {
			String name = ((Element) list.item(i)).getAttribute(NAME_ATTR);
			if (name == null || name.equals(CATCH_ALL) || container.containsKey(name)) {
				return true;
			}
		}
		return false;		
	}
	
	private boolean checkUseful(Element elt) {
		return checkUseful(elt, CALL_KEY, result.callUses) || checkUseful(elt, FIELD_KEY, result.fields)
		|| checkUseful(elt, RETURN_KEY, result.returns)  || checkUseful(elt, REPORT_KEY, reports);
	}


	void dumpRegularNode(Node item, String name) throws Alert {
		out.print("<"); out.print(name); out.print(" ");
		NamedNodeMap attributes = item.getAttributes();
		int length = attributes.getLength();
		for(int i=0; i < length; i++) {
			Attr attr = (Attr) attributes.item(i);
			out.print(" ");
			out.print(attr.getName());
			out.print("=\"");
			out.print(attr.getValue());
			out.print("\"");
		}
		out.print(">");
		dumpNodeList(item.getChildNodes());
		out.print("</"); out.print(name); out.print(">");		
	}
}

package com.orange.analysis.anasoot.apiuse;

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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.tagkit.AbstractHost;

import com.orange.analysis.anasoot.AnasootConfig;
import com.orange.matos.core.Alert;
import com.orange.matos.core.XMLStream;
import com.orange.matos.utils.HtmlOutput;

/**
 * Display the report about API uses from what was 
 * registered in ApplicationApiUse
 * @author tkdn3113
 */
public class ApiUseReport {

	ApplicationApiUse apiUses;

	/**
	 * Constructor.
	 * @param apiUses
	 */
	public ApiUseReport(ApplicationApiUse apiUses) {
		this.apiUses = apiUses;
	}

	/**
	 * Print out the report
	 * @param acf configuration for xml format
	 * @param out printstream.
	 * @throws Alert
	 */
	public void displayAnalysisReport(AnasootConfig acf, PrintStream out) throws Alert {
		boolean xmlFormat = acf.xmlFormat();

		TreeMap<AbstractHost, List<String>> hiddens = apiUses.getHiddenApiUses();
		Map<String, TreeSet<AbstractHost>> unsupported = apiUses.getUnsupportedVersions();

		if(xmlFormat) {
			out.println("");
			out.println("<apiuse>");
			if(!hiddens.isEmpty()) {
				out.println("<hiddenApis>");
				dumpHiddenApiUsesReport(apiUses.getVersions(), hiddens, out, true);
				out.println("</hiddenApis>");
			}
			if(!unsupported.isEmpty()) {
				out.println("<unsupported>");
				dumpUnsupportedVersionsReport(unsupported, out, true);
				out.println("</unsupported>");
			}
			out.println("</apiuse>");
			out.println("");
		} else {
			if(!hiddens.isEmpty()||!unsupported.isEmpty()) {
				out.println(HtmlOutput.openDiv("compatibility"));
				out.println("<h2>Elements version information</h2>");
				if(!hiddens.isEmpty()) {
					out.println("<h3>Hidden apis</h3>");
					dumpHiddenApiUsesReport(apiUses.getVersions(), hiddens, out, false);
				}
				if(!unsupported.isEmpty()) {
					out.println("<h3>Unsupported versions</h3>\n");
					dumpUnsupportedVersionsReport(unsupported, out, false);

				}
				out.println(HtmlOutput.closeDiv());
			}
		}

	}


	private void dumpUnsupportedVersionsReport(
			Map<String, TreeSet<AbstractHost>> unsupported, PrintStream out,
			boolean xmlFormat) {

		if(xmlFormat) {

			XMLStream xmlout = new XMLStream(out);
			for( Entry<String, TreeSet<AbstractHost>> entry : unsupported.entrySet() ) {
				TreeSet<AbstractHost> elements = entry.getValue();
				Iterator<AbstractHost> i = elements.iterator();

				while(i.hasNext()) {
					AbstractHost element = i.next();
					if(element instanceof SootClass) {
						SootClass c = (SootClass) element;
						xmlout.element("class");
						xmlout.attribute("lastUnsupported", entry.getKey());
						xmlout.attribute("class", c.getPackageName()+"."+c.getJavaStyleName());
					} else if(element instanceof SootMethod) {
						SootMethod m = (SootMethod) element;
						SootClass c = m.getDeclaringClass();
						xmlout.element("method");
						xmlout.attribute("lastUnsupported", entry.getKey());
						xmlout.attribute("class", c.getPackageName()+"."+c.getJavaStyleName());
						xmlout.attribute("method", m.getName());
						xmlout.attribute("signature", VersionDatabase.signature(m));
					} else if(element instanceof SootField) {
						SootField f = (SootField) element;
						SootClass c = f.getDeclaringClass();
						xmlout.element("field");
						xmlout.attribute("lastUnsupported", entry.getKey());
						xmlout.attribute("class", c.getPackageName()+"."+c.getJavaStyleName());
						xmlout.attribute("field", f.getName());
						xmlout.attribute("type", f.getType().toString());
					}
				}
				xmlout.close();
			}

		} else {

			out.println("<table class=\"result\">");
			for( Entry<String, TreeSet<AbstractHost>> entry : unsupported.entrySet() ) {
				TreeSet<AbstractHost> elements = entry.getValue();
				Iterator<AbstractHost> i = elements.iterator();
				out.print("<tr><td class=\"head\">");
				out.print(entry.getKey());
				out.println(" and below</td></tr>");
				while(i.hasNext()) {
					AbstractHost element = i.next();
					if(element instanceof SootClass) {
						SootClass c = (SootClass) element;
						out.print("<tr><td class=\"result\">");
						out.print(c.getPackageName());
						out.print(".");
						out.print(c.getJavaStyleName());
						out.println("</td></tr>");
					} else if(element instanceof SootMethod) {
						SootMethod m = (SootMethod) element;
						SootClass c = m.getDeclaringClass();
						out.print("<tr><td class=\"result\">");
						out.print(c.getPackageName());
						out.print(".");
						out.print(c.getJavaStyleName());
						out.print(".");
						out.print(m.getName());
						out.print(" ");
						out.print(VersionDatabase.signature(m));
						out.println("</td></tr>");

					} else if(element instanceof SootField) {
						SootField f = (SootField) element;
						SootClass c = f.getDeclaringClass();
						out.print("<tr><td class=\"result\">");
						out.print(c.getPackageName());
						out.print(".");
						out.print(c.getJavaStyleName());
						out.print(".");
						out.print(f.getName());
						out.print(" ");
						out.print(f.getSignature());
						out.println("</td></tr>");
					}
				}
			}
			out.println("</table>");

		}

	}

	private void dumpHiddenApiUsesReport(List<String> allVersions,
			TreeMap<AbstractHost, List<String>> hiddens, PrintStream out,
			boolean xmlFormat) {
		if(hiddens.isEmpty()) {
			return;
		}

		if(xmlFormat) {
			XMLStream xmlout = new XMLStream(out);
			for( Entry<AbstractHost, List<String>> entry : hiddens.entrySet() ) {
				AbstractHost element = entry.getKey();
				if(element instanceof SootClass) {
					SootClass c = (SootClass) element;
					xmlout.element("class");
					xmlout.attribute("class", c.getPackageName()+"."+c.getJavaStyleName());
				} else if(element instanceof SootMethod) {
					SootMethod m = (SootMethod) element;
					SootClass c = m.getDeclaringClass();
					xmlout.element("method");
					xmlout.attribute("class", c.getPackageName()+"."+c.getJavaStyleName());
					xmlout.attribute("method", m.getName());
					xmlout.attribute("signature", VersionDatabase.signature(m));
				} else if(element instanceof SootField) {
					SootField f = (SootField) element;
					SootClass c = f.getDeclaringClass();
					xmlout.element("field");
					xmlout.attribute("class", c.getPackageName()+"."+c.getJavaStyleName());
					xmlout.attribute("field", f.getName());
					xmlout.attribute("type", f.getType().toString());
				} else {
					return;
				}

				List<String> versions = entry.getValue();
				for(String v : versions) {
					xmlout.element("hidden");
					xmlout.attribute("version", v);
					xmlout.endElement();
				}
				xmlout.close();
			}
		} else {

			out.println("<table class=\"result\">");
			for( Entry<AbstractHost, List<String>> entry : hiddens.entrySet() ) {
				AbstractHost element = entry.getKey();
				if(element instanceof SootClass) {
					SootClass c = (SootClass) element;
					out.print("<tr><td class=\"result\">");
					out.print(HtmlOutput.escape(c.getPackageName()));
					out.print(".");
					out.print(HtmlOutput.escape(c.getJavaStyleName()));
					out.print("</td>");
				} else if(element instanceof SootMethod) {
					SootMethod m = (SootMethod) element;
					SootClass c = m.getDeclaringClass();
					out.print("<tr><td class=\"result\">");
					out.print(HtmlOutput.escape(c.getPackageName()));
					out.print(".");
					out.print(HtmlOutput.escape(c.getJavaStyleName()));
					out.print(".");
					out.print(HtmlOutput.escape(m.getName()));
					out.print(" ");
					out.print(HtmlOutput.escape(VersionDatabase.signature(m)));
					out.print("</td>");
				} else if(element instanceof SootField) {
					SootField f = (SootField) element;
					SootClass c = f.getDeclaringClass();
					out.print("<tr><td class=\"result\">");
					out.print(HtmlOutput.escape(c.getPackageName()));
					out.print(".");
					out.print(HtmlOutput.escape(c.getJavaStyleName()));
					out.print(".");
					out.print(HtmlOutput.escape(f.getName()));
					out.print(" ");
					out.print(HtmlOutput.escape(f.getType().toString()));
					out.print("</td>");
				}

				List<String> versions = entry.getValue();
				for(String v : allVersions) {
					if(versions.contains(v)) {
						out.print("<td class=\"head\">");
						out.print(v);
					} else {
						out.print("<td>");
					}
					out.println("</td>");
				}
				out.println("</tr>");
			}
			out.println("</table>");
			out.println("");

		}

	}

}

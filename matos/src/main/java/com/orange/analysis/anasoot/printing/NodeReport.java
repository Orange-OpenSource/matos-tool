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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import soot.ArrayType;
import soot.ByteType;
import soot.RefType;
import soot.SootClass;

import com.orange.analysis.anasoot.result.NodeTable;
import com.orange.analysis.anasoot.result.ProgramPoint;
import com.orange.matos.core.XMLStream;
import com.orange.matos.utils.HtmlOutput;

/**
 * @author piac6784
 * Prints out the contents of the node table.
 */

public class NodeReport {

	/**
	 * Prints out the contents of the node tables. Depending on the kind of output the result 
	 * is formatted differently
	 * <ul>
	 * <li> If the output is in XML format, then the list of nodes is unsorted. There is always the id
	 * and <emph> all the types of the node</emph> given as embedded class or interface element.
	 * If the element is linked to a program point, there is also the specification of this
	 * program point as a method and an offset. </li>
	 * <li> If the output is in HTML format then the list of nodes is organised per program
	 * points and followed by the nodes that are not linked with a program point. Each sublist
	 * is sorted per node id. The goal is to be both readable and compact.</li>
	 * </ul>
	 * @param out the stream to write the result to
	 * @param xmlFormat whether the output should be in XML format or not
	 * @param nodeTable the CallContext containing the definition of the nodes to dump.
	 * @param arrayAnalysis 
	 */
	static public void dumpNodes (PrintStream out, boolean xmlFormat, NodeTable nodeTable) {
		if (xmlFormat) {
			XMLStream xmlout = new XMLStream(out);
			Map<Integer,ProgramPoint> antiTable = new HashMap<Integer, ProgramPoint>();
			for (Entry <ProgramPoint, Set <Integer>> entry : nodeTable.ppTable.entrySet()) {
				ProgramPoint pp = entry.getKey();
				for(Integer i: entry.getValue()) antiTable.put(i, pp);
			}
			for(int key : nodeTable.nodeTable.keySet()) {
				if (key==-1) continue;
				
				xmlout.element("node");
				xmlout.attribute("id", key);
				ProgramPoint pp = antiTable.get(key);
				if (pp != null) {
					xmlout.attribute("method", pp.method.getSignature());
					xmlout.attribute("offset", pp.offset);
				}
				Object contents = nodeTable.nodeTable.get(key);
				if (contents instanceof RefType) {
					SootClass cl = ((RefType) contents).getSootClass();
					while(cl != null) {
						xmlout.element("class");
						xmlout.attribute("name", NodeReport.fullName(cl));
						xmlout.endElement();
						for(SootClass itf : cl.getInterfaces()) {
							xmlout.element("interface");
							xmlout.attribute("name",NodeReport.fullName(itf));
							xmlout.endElement();
						}
						try {cl = cl.getSuperclass(); }
						catch (Exception e) {cl = null; }
					}
				} else if (contents instanceof ArrayType) {
					ArrayType at = (ArrayType) contents;
					xmlout.element("array");
					xmlout.attribute("base", at.getElementType());
					xmlout.attribute("size", at.numDimensions);
					
					if (at.getElementType().equals(ByteType.v())) {
						xmlout.element("content");
						String content = nodeTable.getByteContent(key,true);
						xmlout.attribute("value", content);
						xmlout.endElement();
					}
					xmlout.endElement();
				} else {
					String classname = (String) contents;
					xmlout.element("string");
					xmlout.attribute("contents", classname);
					xmlout.endElement();
				}
				xmlout.close();
			}
		} else {
			if (nodeTable.nodeTable.size() < 2) return;
			out.println(HtmlOutput.header(1, "Object References"));
			out.println("<ul>");
			Set <Integer> seen = new HashSet <Integer> ();
			for(Entry <ProgramPoint,Set<Integer>> entry : nodeTable.ppTable.entrySet()) {
				ProgramPoint pp = entry.getKey();
				Set <Integer> keySet = entry.getValue();
				seen.addAll(keySet);
				out.println("<li>" + pp.offset + "@" + HtmlOutput.escape(pp.method.getSignature()) );
				out.println("<ul>");
				displayHtmlNodes(out,nodeTable,keySet);
				out.println("</ul>");
				out.println("</li>");
			}
			out.println("<li> Direct object allocation");
			out.println("<ul>");
			Set <Integer> keySet = nodeTable.nodeTable.keySet();
			keySet.removeAll(seen);
			keySet.remove(-1);
			displayHtmlNodes(out,nodeTable,keySet);
			out.println("</ul>");
			out.println("</li>");
			out.println("</ul>");
		}	
	}

	/**
	 * Prints out the kind of content of a node. It is used to build headers
	 * in node table
	 * @param cc where to find info on nodes
	 * @param key identifier of the node.
	 * @return the message to print
	 */
	public static String message(NodeTable cc, int key) {
		String message;
		Object content = cc.nodeTable.get(key);
		if (content instanceof String) message = null;
		else if (content instanceof RefType) {
			SootClass cl = ((RefType) content).getSootClass();
			message = HtmlOutput.escape(cl.getPackageName() + "." + cl.getJavaStyleName());
		} else if (content instanceof ArrayType) {
			message = HtmlOutput.escape(content.toString());
		} else {
			message = "Unknown";
		}
		return message;
	}

	/**
	 * Display a line describing an internal points-to node for the end user. 
	 * @param out the stream
	 * @param nodeTable the context
	 * @param arrayAnalysis 
	 * @param key an integer key identifying uniquely the node.
	 */
	public static void displayHtmlNodes(PrintStream out, NodeTable nodeTable, Set<Integer> keys) {
		HashMap<String,ArrayList<Integer>> groups = new HashMap<String,ArrayList<Integer>>();
		HashMap<Integer,String> arrayValues = new HashMap<Integer, String>();
		HashSet <Integer> done = new HashSet<Integer>();
		// Uglier than this is difficult. How to check that we have a fixpoint ?
		do {
			HashSet <Integer> todo = new HashSet<Integer>();
			todo.addAll(keys);
			todo.removeAll(done);
			for(Integer key : todo) {
				done.add(key);
				String v = nodeTable.getArrayContent(key,false);
				if (v != null) arrayValues.put(key, v);
			}
		} while(!done.containsAll(keys));
		for(Integer key: keys) {
			HashMap<Integer, String> uses = nodeTable.nodeToUse.get(key);
			if (uses == null || uses.size() == 0) continue;
			String msg = message(nodeTable,key);
			if (msg == null) continue;
			ArrayList <Integer> group = groups.get(msg);
			if (group == null) { group = new ArrayList<Integer>(); groups.put(msg, group); }
			group.add(key);
		}
		String [] groupHeaders = groups.keySet().toArray(new String[0]);
		Arrays.sort(groupHeaders);
		out.println("<table class=\"result\">");
		for(String header : groupHeaders) {
			ArrayList<Integer> groupKeys = groups.get(header);
			if (groupKeys.size() == 0) continue;
			out.print(HtmlOutput.row("<td class=\"head\" align=\"center\" colspan=\"2\">" + header + "</td>"));
			
			Collections.sort(groupKeys);
			for(int key: groupKeys) {
				HashMap<Integer,String> uses = nodeTable.nodeToUse.get(key);
				Set<Entry<Integer, String>> entries = uses.entrySet();
				out.print("<tr><td class=\"subhead\"><a name=\"N" + key + "\"><b>[" + key + "]</b></a></td><td class=\"result\">");
				boolean first = true;
				for(Entry<Integer,String> use : entries) {
					if (first) first = false;
					else out.print(",");
					out.print("<a href=\"#U" + key + "-" + use.getKey() + "\">" + use.getValue() + "</a> ");
				}
				String v = arrayValues.get(key);
				if (v != null) { out.print (" ["); out.print(v); out.print("]"); }
				out.println("</td></tr>");
				String contents = nodeTable.getByteContent(key, false);
				if (contents != null) {
					out.print("<tr><td></td><td class=\"result\">");
					out.print(contents);
					out.println("</td></tr>");
				}
			}
		}
		out.println("</table>");
	}

	/**
	 * Pretty printing of a Soot class name.
	 * @param cl
	 * @return
	 */
	public static String fullName(SootClass cl) {
		return 
		cl.getPackageName().equals("")
		? cl.getJavaStyleName()
				: (cl.getPackageName() + "." +  cl.getJavaStyleName());
	}

}

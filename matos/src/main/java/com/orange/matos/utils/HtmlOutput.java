/*
 * $Id: HtmlOutput.java 2285 2013-12-13 13:07:22Z piac6784 $
 */
package com.orange.matos.utils;

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

import java.util.Collection;

import org.apache.commons.lang3.StringEscapeUtils;

/** 
 * A class that contains useful functions to create HTML or regular text.
 * A boolean parameter select whether the output is in text or html mode.  
 */
public class HtmlOutput {
	static boolean html=true;
	
	/** 
	 * Quote the four special HTML characters 
	 */
	public static String escape(String s) {
		// At least some tools complain on the form feed but escapeHtml does not get rid of it.
		return (s==null) ? "" : StringEscapeUtils.escapeXml(s.replaceAll("\f","\\f"));
	}
	
	/**
	 * Mode for HTML output
	 * @param b
	 */
	public static void setHtmlOutput(boolean b) { html=b; }
	
	/**
	 * Create a link
	 * @param s
	 * @param url
	 * @return
	 */
	public static String link(String s, String url) {
		if (html) { return ("<a href=\"" + url + "\">" + s + "</a>\n"); }
		else return (s + "\n");
	}
	
	/**
	 * Warning string
	 * @param msg
	 * @return
	 */
	public static String warning(String msg){
		if (html) {
			return (paragraph(bold(color("orange", "Warning: "))+bold(msg)));
		} else {
			return ("Warning: " + msg + "\n");
		}
	}
	
	/**
	 * Paragraph
	 * @param s
	 * @return
	 */
	public static String paragraph(String s) {
		if (html) { return ("<p>" + s + "</p>\n"); }
		else return (s + "\n");
	}
	
	/**
	 * Change text color
	 * @param color
	 * @param s
	 * @return
	 */
	public static String color(String color, String s) {
		if (html) { 
		    return ("<font color=\"" + color + "\">" + s + "</font>");
		} else return s;
	}
	
	/**
	 * Section header
	 * @param i
	 * @param s
	 * @return
	 */
	public static String header(int i, String s) {
		if (html) { 
			return ("<h" + i + ">" + s + "</h" + i + ">\n");
		}
		else return (s + "\n");
	}
	
	/**
	 * Bold text
	 * @param s
	 * @return
	 */
	public static String bold(String s) {
		if (html) { return ("<b>" + s + "</b>"); } else return s;
	}
	
	/**
	 * Table cell
	 * @param opt
	 * @param s
	 * @return
	 */
	public static String cell(String opt, String s) {
		if (html) { return ("  <td" + opt + ">" + s + "</td>\n"); } 
		else return s + "\t";
	}
	
	/**
	 * Table cell
	 * @param s
	 * @return
	 */
	public static String cell(String s) {
		if (html) { return ("  <td>" + s + "</td>\n"); } 
		else return s + "\t";
	}

	/**
	 * Table cell for a row or column head
	 * @param s
	 * @return
	 */
	public static String cellHead(String s) {
		if (html) { return ("  <th>" + s + "</th>\n"); } 
		else return s + "\t";
	}
	
	/**
	 * Table row
	 * @param opt
	 * @param s
	 * @return
	 */
	public static String row(String opt, String s) {
		if (html) { return ("<tr" + opt + ">" + s + "</tr>\n"); } 
		else return s + "\n";
	}
	
	/**
	 * Table row
	 * @param s
	 * @return
	 */
	public static String row(String s) {
		if (html) { return ("<tr>" + s + "</tr>\n"); } 
		else return s + "\n";
	}
	
	/**
	 * New table
	 * @return
	 */
	public static String openTable() {
		if (html) {
		    return "<table>";
		} else return "";
	}
	
	/**
	 * Close table
	 * @return
	 */
	public static String closeTable() {
		return (html ? "</table>" : "");
	}
	
	/**
	 * Horizontal separator
	 * @return
	 */
	public static String hrule() {
		return (html ? "<hr width='100%'></hr>" : "");
	}
	/**
	 * Line break
	 * @return
	 */
	public static String br() {
		return (html ? "<br></br>" : "");
	}
	
	/**
	 * List of items (array version)
	 * @param l
	 * @return
	 */
	public static String list(Object l[]) {
		StringBuffer buf = new StringBuffer("<ul type=\"disc\">\n");
		for(Object obj: l) { 
			buf.append ("<li>"); buf.append(obj); buf.append("</li>\n");
		}
		buf.append("</ul>\n");
		return buf.toString();
	}
	
	/**
	 * List of items (list version)
	 * @param l
	 * @return
	 */
	public static String list(Collection<?> l) {
		StringBuffer buf = new StringBuffer("<ul type=\"disc\">\n");
		for(Object obj: l) {
			buf.append ("<li>"); buf.append(obj); buf.append("</li>\n");
		}
		buf.append("</ul>\n");
		return buf.toString();
	}

	/**
	 * New text division
	 * @param clz
	 * @return
	 */
	public static String openDiv(String clz) {
		return html ? "<div class=\"" + clz + "\">" : "";
	}
	
	/**
	 * Close text division
	 * @return
	 */
	public static String closeDiv() {
		return html ? "</div>" : "";
	}

}

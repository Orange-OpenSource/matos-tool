package com.orange.matos.core;

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
import java.util.Stack;

import com.orange.matos.utils.HtmlOutput;

/**
 * @author Pierre Cregut
 * Abstract a stram for producing compliant XML
 */
public class XMLStream {
	final PrintStream out;
	private boolean opened = false;
	private Stack <String> elements = new Stack <String> ();
	
	/**
	 * Encapsulate a printstream
	 * @param out
	 */
	public XMLStream(PrintStream out) {
		this.out = out;
	}
	
	/**
	 * Encapulate a printstream and set a root.
	 * @param out
	 * @param name
	 */
	public XMLStream(PrintStream out, String name) {
		this.out = out;
		element(name);
	}
	
	/**
	 * Add an XML element
	 * @param name
	 */
	public void element(String name) {
		if (opened) out.print(">");
		out.print("<"); 
		out.print(name);
		elements.push(name);
		opened = true;
	}
	
	/**
	 * Add an attribute
	 * @param name
	 * @param s
	 */
	public void attribute(String name, Object s) {
		String value = HtmlOutput.escape(s == null ? "-" : s.toString());
		if (!opened) return;
		out.print(" ");
		out.print(name);
		out.print("=\"");
		out.print(value);
		out.print("\"");
	}
	
	/**
	 * Add an attribute
	 * @param name
	 * @param value
	 */
	public void attribute(String name, int value) {
		if (!opened) {
			throw new IllegalStateException("Current element not opened for attribute");
		}
		out.print(" ");
		out.print(name);
		out.print("=\"");
		out.print(value);
		out.print("\"");
	}
	
	/**
	 * Ends an element
	 */
	public void endElement() {
		if (elements.isEmpty()) {
			throw new IllegalStateException("No element to end");
		}
		String ended = elements.pop();
		if (opened) {
			out.print("/>");
			opened = false;
		} else { 
			out.print("</");
			out.print(ended);
			out.print(">");
		}
	}
	
	/**
	 * Close all open elements.
	 */
	public void close() {
		while(!elements.isEmpty()) endElement();
		out.println();
	}

	/**
	 * Print an object regardless of the stream status but close current element for attributes
	 * @param o
	 */
	public void print(Object o) {
		String toPrint = HtmlOutput.escape(o.toString());
		if (opened) {
			out.print(">");
			opened = false;
		}
		out.print(toPrint);
	}
	
	/**
	 * Print an integer.
	 * @param v
	 */
	public void print(int v) {
		if (opened) {
			out.print(">");
			opened = false;
		}
		out.print(v);
	}
	
	/**
	 * Endline (out of band).
	 */
	public void println() {
		out.println();
	}
		
}

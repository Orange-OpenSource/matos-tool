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

import java.io.PrintStream;
import java.util.ArrayList;

import com.orange.matos.core.Configuration;
import com.orange.matos.core.XMLStream;
import com.orange.matos.utils.HtmlOutput;

/**
 * Class used to register the different use of a midlet as defined statically in the JAD/Manifest files.
 * Those use can be: user-level midlet, push-registry midlet, content-handler, etc.
 * @author Pierre Cregut
 *
 */
public class MidletKind {
	final String kind;
	final int position;
	
	private MidletKind(String kind, int position) {
		this.kind = kind;
		this.position = position;
	}
	
	/**
	 * Register a new usage for a midlet in the current configuration.
	 * @param config: the configuration.
	 * @param midletName: the name of the midlet
	 * @param kind: the usage of the midlet as a string.
	 */
	public static void addKind(Configuration config, String midletName, String kind, int pos) {
		MidletKind k = new MidletKind(kind, pos);
		config.setAppInfo(midletName, k);
	}
	
	/**
	 * Dumps all the usage of a given midlet on the output stream.
	 * @param out the stream to print-to (either in HTML or XML format).
	 * @param config the configuration
	 * @param midletName the name of the midlet.
	 */
	public static void dump(PrintStream out, Configuration config, String midletName) {
		ArrayList <String> kinds = new ArrayList <String> ();
		for(Object raw : config.getAppInfo(midletName)) {
			if(raw instanceof MidletKind) {
				kinds.add(((MidletKind) raw).kind + " " + ((MidletKind) raw).position);
			}
		}
		if (config.xmlFormat()) {
			XMLStream xmlout = new XMLStream(out);
			xmlout.element("midlet");
			for(String kind : kinds) {
				xmlout.element("use");
				xmlout.attribute("kind", kind);
				xmlout.endElement();
			}
			xmlout.endElement();
		} else {
			out.print("(");
			boolean first = true;
			if (kinds.size() == 0) {
				out.print(HtmlOutput.color("orange", "No static use for this midlet"));
			} else {
				for(String kind: kinds) { 
					if (first) first = false ; else out.print(", ");
					out.print(kind);
				}
			}
			out.println(")");
		}
	}
}

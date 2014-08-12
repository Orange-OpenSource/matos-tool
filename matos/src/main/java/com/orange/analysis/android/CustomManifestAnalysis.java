package com.orange.analysis.android;

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
import java.util.Properties;

import org.w3c.dom.Element;

import com.orange.analysis.anasoot.printing.ParameterizedOutput;
import com.orange.matos.android.APKDescr;
import com.orange.matos.core.Alert;
import com.orange.matos.core.Configuration;
import com.orange.matos.core.RuleFile;
import com.orange.matos.core.XMLParser;

/**
 * @author Pierre Cregut
 * Analysis and printing of custom rules on manifest.
 */
public class CustomManifestAnalysis {
	private Element[] roots;
	private final Properties props;
    private final Configuration configuration;

	/**
	 * Constructor from a rule file specifyiing what to do, a manifest.
	 * @param file
	 * @param configuration 
	 */
	public CustomManifestAnalysis( RuleFile file, Configuration configuration) {
		XMLParser parser = file.getParser();
		roots = parser.getKind("androidManifest");
		props = new Properties();
		this.configuration =  configuration;
	}

	/**
	 * Perform the analysis on a manifest file.
	 * @param manifest
	 * @throws Alert
	 */
	public void doAnalysis(APKDescr manifest) throws Alert {
		for(Element root : roots ) { 
			for(Element clazzDescr : XMLParser.getElements(root, "class")) {
				String classname = clazzDescr.getAttribute("name");
				if (classname != null) {
					try {
						Class <?> clazz = Class.forName(classname,true, configuration.getCustomClassLoader());
						Class<? extends CustomManifestRule> mfclass = clazz.asSubclass(CustomManifestRule.class);
						CustomManifestRule rule = mfclass.newInstance();
						rule.run(manifest, props);
					} catch (ClassNotFoundException e) {
						throw Alert.raised(e,"No class found for Android manifest analysis : " + classname );
					} catch (InstantiationException e) {
						throw Alert.raised(e, "No class found for Android manifest analysis : " + classname );
					} catch (IllegalAccessException e) {
						throw Alert.raised(e, "Visibility problem for class in Android manifest analysis : " + classname);
					} catch (ClassCastException e) {
						throw Alert.raised(e, "Incorrect class (not a CustomeManifestRule) for Android manifest analysis : " + classname);
					}
				}
			}
		}
		manifest.learn(props);
	}

	/**
	 * Prints the output of the analysis on a stream
	 * @throws Alert
	 */
	public void doXsltOutput(PrintStream out) throws Alert {
		ArrayList <Element> template = new ArrayList<Element>();
		for(Element rule: roots) {
			for(Element output: XMLParser.getElements(rule, "output")) {
				template.add(output);
			}
		}
		// System.out.println(props);
		ParameterizedOutput.output(out, template, props);
	}
}

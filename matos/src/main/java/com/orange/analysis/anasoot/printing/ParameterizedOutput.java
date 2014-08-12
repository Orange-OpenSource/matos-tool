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
import java.util.List;
import java.util.Properties;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.orange.matos.core.Alert;

/**
 * @author Pierre Cregut
 * Generic class to parameterize the output.
 */
public class ParameterizedOutput {

	private static final String RULES_XSLT = "/com/orange/matos/rules.xslt";

    /**
	 * Output a template to a stream using a set of properties to specialize the template. The grammar
	 * is fixed by the xslt transform and comprize at least properties output, conditionals on properties
	 * values and basic loops
	 * @param out
	 * @param template
	 * @param props
	 * @throws Alert
	 */
	public static void output(PrintStream out, List <Element> template, Properties props) throws Alert {
		DocumentBuilder docBuilder;
		try {
			DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
			docBuilder = dbfac.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			Element root = doc.createElement("root");
            doc.appendChild(root);
            Element propertiesBag = doc.createElement("props");
            root.appendChild(propertiesBag);
            for(Entry<?,?> propEntry : props.entrySet()) {
            	Element property = doc.createElement("property");
            	property.setAttribute("key", propEntry.getKey().toString());
            	property.setAttribute("value", propEntry.getValue().toString());
            	propertiesBag.appendChild(property);
            }
            
            for(Element output: template) {
           		root.appendChild(doc.renameNode(doc.importNode(output, true),null,"output"));
            }
            DOMSource source = new DOMSource(doc);
            

            //initialize StreamResult with File object to save to file
            
            Result result = new StreamResult(out);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            /* / DEBUG
            transformerFactory.newTransformer().transform(source, new StreamResult(System.out));
			*/
            StreamSource xsltSource = new StreamSource(ParameterizedOutput.class.getResourceAsStream(RULES_XSLT));
            Transformer xsltTransform = transformerFactory.newTransformer(xsltSource);
            xsltTransform.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            xsltTransform.transform(source, result);
		} catch (ParserConfigurationException e) {
			throw Alert.raised(e, "AndroidManifest Phase : bad parser configuration - please report.");
		} catch (TransformerConfigurationException e) {
			throw Alert.raised(e, "AndroidManifest Phase : bad internal style sheet - please report." + e.getMessageAndLocation());
		} catch (TransformerException e) {
			throw Alert.raised(e, "AndroidManifest Phase : transformation exception probably linked to the rules used (output in androidManifest)");
		}        
	}

}

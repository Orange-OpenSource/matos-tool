/*
 * $Id: CampaignXMLParser.java 2285 2013-12-13 13:07:22Z piac6784 $
 */
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
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.orange.matos.android.AndroidStep;
import com.orange.matos.core.Alert;
import com.orange.matos.core.DownloadParameters;
import com.orange.matos.core.Out;
import com.orange.matos.core.Step;
import com.orange.matos.core.XMLParser;
import com.orange.matos.java.JavaStep;


/** 
 * Defines an XML parser for the campaign files 
 */
public class CampaignXMLParser  {
	
	static class CampaignResolver implements EntityResolver, ErrorHandler {
		
		private static final String CAMPAIGN_DTD = "/com/orange/matos/campaign.dtd";

        // load the DTD for campaign validation. Expected to be
		// found in the LIB directory (defined at launch time of Matos). 
		@Override
		public InputSource resolveEntity (String publicId, String systemId) {	
			if (systemId.endsWith("campaign.dtd")) {
				try {
				    InputStream is = XMLParser.class.getResourceAsStream(CAMPAIGN_DTD);
				    return new InputSource(is);
				} catch (Exception e) {
					// Overriden method, can't make it throw Alert...
					Out.getMain().println(e.getMessage());
					Out.getLog().println(e.getMessage());
					e.printStackTrace(Out.getMain());
					e.printStackTrace(Out.getLog());
					return null;
				}
			} 
			return null;
		}

		@Override
		public void error(SAXParseException exception) throws SAXException {
			Out.getLog().println(exception.getMessage());
			exception.printStackTrace(Out.getLog());
		}

		@Override
		public void fatalError(SAXParseException e) throws SAXException {
			Out.getMain().println(e.getMessage());
			Out.getLog().println(e.getMessage());
			e.printStackTrace(Out.getMain());
			e.printStackTrace(Out.getLog());
		}

		@Override
		public void warning(SAXParseException exception) throws SAXException {
			Out.getLog().println(exception.getMessage());
			exception.printStackTrace(Out.getLog());
		}
	}
	
	Document doc;
	Element root;
	
	CampaignXMLParser(){}
	
	/**
	 * Read a XML file and intialize the doc and the root attributes.
	 * @param f The XML file
	 * @throws Alert To spread exceptions
	 */
	public void readCampaignFile(File f) throws Alert{
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(true);
			DocumentBuilder docbuild = factory.newDocumentBuilder();
			CampaignResolver campResolver = new CampaignResolver();
			docbuild.setEntityResolver(campResolver);
			docbuild.setErrorHandler(campResolver);
			doc = docbuild.parse(f);
			root = doc.getDocumentElement();
		} catch (SAXParseException e) {
			throw Alert.raised(e,"Error at line" +  e.getLineNumber() + " - col " +
					e.getColumnNumber() + " - Entity " 
							+ e.getPublicId() + " in file "+ f.getAbsolutePath() + ".");
		} catch (Exception e) {
			throw Alert.raised(e);
		}
	}
	
	/**
	 * Write the Xml document in a file
	 * @param fileURI URI of the XML file
	 * @throws Alert To spread exceptions
	 */
	public void writeCampaignFile(String fileURI)throws Alert{
		try {			
            // Prepare the DOM document for writing
            Source source = new DOMSource(doc);
    
            // Prepare the output file
            File file = new File(fileURI);
            Result result = new StreamResult(file);
    
            // Write the DOM document to the file
            Transformer xformer = TransformerFactory.newInstance().newTransformer();
            xformer.setOutputProperty(OutputKeys.INDENT, "yes");
            xformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "campaign.dtd");
            xformer.transform(source, result);
        } catch (TransformerConfigurationException e) {
        	throw Alert.raised(e);
        } catch (TransformerException e) {
        	throw Alert.raised(e);
        }
	}
	
	/** gives back the string contents of an element (the text 
	 *  between the two tags)
	 *  @param e the element to explore
	 *  @return the text content
	 */
	public String contents(Element e){
		StringBuffer buf = new StringBuffer();
		NodeList childs = e.getChildNodes();
		for(int i=0; i < childs.getLength(); i++)
			buf.append(childs.item(i));
		return buf.toString();
	}
	
	/** 
	 * Extracts the set of XML elements having a given name in a given XML element
	 * @param e the element to explore
	 * @param name the name of the elements searched
	 * @return an array of elements
	 */
	public Element [] getElements(Element e, String name) {
		NodeList listNodes = e.getElementsByTagName(name);
		int l = listNodes.getLength();
		Element r [] = new Element [l];
		for(int i=0; i < l; i++) r[i] = (Element) listNodes.item(i);
		return r;
	}
	
	/** 
	 * get a given category of entries from the file
	 * @param kind the kind of elements analyzed
	 * @return an array of XML elements of that kind 
	 */	
	public Element [] getKind(String kind) {
		return getElements(root,kind);
	}
	
	
	/** 
	 * Extracts a given XML element having a given name son of a given
	 * XML element. There should be only one such element:
	 * @param e the element to explore
	 * @param name the name of the elements searched
	 * @return the element found
	 */
	public Element getElement(Element e, String name) {
		NodeList listNodes = e.getElementsByTagName(name);
		if (listNodes.getLength() == 1) return (Element) listNodes.item(0);
		return null;
	}

	/**
	 * Creates the document describing the campaign.
	 *
	 * @param campaign the campaign
	 * @param profile the profile
	 * @throws Alert the alert
	 */
	public void createDocument(Campaign campaign, String profile) throws Alert{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		try {
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw Alert.raised(e,"Cannot create document");
		}
		doc = builder.newDocument();
		root = doc.createElement("campaign");
		root.setAttribute("profile", profile);
		doc.appendChild(root);
		int stepNumber=1;
		for (Step step : campaign){
			Element anaElem = null;
			if (step instanceof JavaStep) anaElem = doc.createElement("anajava");
			else if (step instanceof AndroidStep) anaElem = doc.createElement("anadroid");
			else throw Alert.raised(new RuntimeException("unknown element"), "unknown element");
			anaElem.setAttribute("name", "step_"+stepNumber);
			if (step instanceof JavaStep){
				JavaStep javaCmdLine = (JavaStep)step;
				if (javaCmdLine.hasJad()){
					anaElem.setAttribute("jad", javaCmdLine.getJad());
				}
				if (javaCmdLine.hasJar()){
					anaElem.setAttribute("jar", javaCmdLine.getCode());
				}
			} else if (step instanceof AndroidStep) {
				anaElem.setAttribute("file", ((AndroidStep)step).getCode());
			}
			DownloadParameters dp = step.getParameters();
			if (dp.getLogin()!=null) {
				anaElem.setAttribute("login", dp.getLogin());
				anaElem.setAttribute("password", dp.getPassword());
			}
			if (dp.getUserAgent()!=null) {
				anaElem.setAttribute("useragent", dp.getUserAgent());
			}
			
			if (step instanceof JavaStep && !((JavaStep)step).midletList.isEmpty()){
				for (String midletName : ((JavaStep)step).midletList) {
					Element midletElem = doc.createElement("midlet");
					midletElem.setAttribute("name", midletName);
					anaElem.appendChild(midletElem);
				}
			}
			root.appendChild(anaElem);
			stepNumber++;
		}
	}
}

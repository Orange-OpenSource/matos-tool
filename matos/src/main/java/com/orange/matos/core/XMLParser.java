/*
 * $Id: XMLParser.java 2285 2013-12-13 13:07:22Z Pierre Cregut $
 */
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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;


/** 
 * Defines an XML parser for the rule files 
 */
public class XMLParser  {	

	private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	
	
	static class MatosResolver implements EntityResolver {
		private static final String MATOS_DTD = "/com/orange/matos/matos.dtd";

		@Override
		public InputSource resolveEntity (String publicId, String systemId) {			
			try {
				if (systemId.endsWith("matos.dtd")) {
					InputStream is = XMLParser.class.getResourceAsStream(MATOS_DTD);
					return new InputSource(is);
				} else if (systemId.startsWith("jar:file:")) {
				    return new InputSource(systemId);
				} else {
					Reader entityReader = new InputStreamReader(new FileInputStream(new File(new URI(systemId))),"UTF-8");
					return new InputSource(entityReader);	
				}
			} catch (Exception e) { 
				// Overriden method, can't make it throw Alert...
				Out.getMain().println(e.getMessage());
				Out.getLog().println(e.getMessage());
				e.printStackTrace(Out.getMain());
				e.printStackTrace(Out.getLog());
				return null; 
			}
		}
	}
	
	private Document doc;
	private Element root;

	static {
        factory.setValidating(false);
        factory.setXIncludeAware(true);
        factory.setNamespaceAware(true);
	}
	
	/**
	 * Constructor
	 * @param f
	 * @throws Alert
	 */
	public XMLParser(URL resource) throws Alert {

        try {
            System.out.println(resource.toString());
			DocumentBuilder docbuild = factory.newDocumentBuilder();
			docbuild.setEntityResolver(new MatosResolver());
			doc = docbuild.parse(resource.toURI().toString());
			root = doc.getDocumentElement();
			
		} catch (SAXParseException e) {
			e.printStackTrace(Out.getLog());
			throw Alert.raised(e,"Error at line" +  e.getLineNumber() + " - col " +
							e.getColumnNumber() + " - Entity " 
							+ e.getPublicId() + " in file "+  resource + ".");
		} catch (Exception e) {
			e.printStackTrace(Out.getLog());
			throw Alert.raised(e);
		}
	}
	
	
	/** 
	 * gives back the string contents of an element (the text
	 * between the two tags)
	 * @param e the element to explore
	 * @return the text content
	 */

	public String contents(Element e){
		StringBuffer buf = new StringBuffer();
		NodeList childs = e.getChildNodes();
		for(int i=0; i < childs.getLength(); i++){
			Node childNode = childs.item(i);
			int nodeType = childNode.getNodeType();
			if (nodeType == Node.ELEMENT_NODE){
				buf.append("<"+childNode.getNodeName());
				if (childNode.hasAttributes()){
					NamedNodeMap attributes = childNode.getAttributes();
					for (int j=0; j<attributes.getLength(); j++){
						Node attribute = attributes.item(j);
						buf.append (" "+attribute.getNodeName()+"=\""+attribute.getNodeValue()+"\"");
					}
				}
				buf.append(">");
				buf.append(contents((Element)childNode));
				buf.append("</"+childNode.getNodeName()+">");
			}else if (nodeType == Node.TEXT_NODE){
				buf.append(childNode.getNodeValue());
			}			
		}
		return buf.toString();
	}
	
	/** 
	 * Extracts the set of XML elements having a given name in a given XML element
	 * @param e the element to explore
	 * @param name the name of the elements searched
	 * @return an array of elements
	 */
	public static Element [] getElements(Element e, String name) {
		NodeList listNodes = e.getElementsByTagName(name);
		int l = listNodes.getLength();
		Element r [] = new Element [l];
		for(int i=0; i < l; i++) r[i] = (Element) listNodes.item(i);
		return r;
	}
	
	/** 
	 * get a given category of entries from the file (for anasoot mainly)
	 * @param kind the kind of elements analyzed
	 * @return an array of XML elements representing the rules 
	 */	
	public Element [] getKind(String kind) {
		return getElements(root,kind);
	}
	
	/** 
	 * get the options from the rule file
	 * @return an array of XML elements representing the options 
	 */	
	public Element [] getOptions() {
		return getElements(root,"option");
	}
	
	/**
	 * Return an attribute of the root element.
	 * @param attName
	 * @return
	 */
	public String getRootAttribute(String attName) {
	    return root.getAttribute(attName);
	}
	
	/** 
	 * Extracts a given XML element having a given name son of a given
	 * XML element. There should be only one such element:
	 * @param e the element to explore
	 * @param name the name of the elements searched
	 * @return an array of elements
	 */
	public Element getElement(Element e, String name) {
		NodeList listNodes = e.getElementsByTagName(name);
		if (listNodes.getLength() == 1) return (Element) listNodes.item(0);
		else return null;
	}
}

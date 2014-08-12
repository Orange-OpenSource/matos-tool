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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.orange.matos.core.Configuration;

/**
 * @author Pierre Cregut
 * Support class containing file utilities
 */
public class FileUtilities {

	/**
	 * Instantiates a new file utilities support class.
	 */
	public FileUtilities() {}
	
	/**
	 * Copy a source text file into a destination file.
	 * @param in source file
	 * @param out destination file
	 * @throws Exception
	 */
	public static void copyTextFile(File in, File out) throws Exception {
	    Reader fis = new InputStreamReader(new FileInputStream(in),"UTF-8");
		try {
			Writer fos = new OutputStreamWriter(new FileOutputStream(out), "UTF-8");
			try {
				char[] buf = new char[1024];
				int i = 0;
				while ((i = fis.read(buf)) != -1) {
					fos.write(buf, 0, i);
				}
			} finally {
				fos.close();
			}
		} finally {
			fis.close();
		}
	}
	
	/**
	 * Copy from a text file to a stream.
	 * @param in
	 * @param out
	 * @throws IOException
	 */
	public static void copyTextFile(File in, PrintStream out) throws IOException {
		byte[] buf = new byte[1024];
		int i = 0;
		FileInputStream fis = new FileInputStream(in);
		try {
			while ((i = fis.read(buf)) != -1) {	out.write(buf, 0, i); }
		} finally {	fis.close(); }
	}


	/**
	 * Copy a source file into a destination file in a binary way.
	 * @param in source file
	 * @param out destination file
	 * @throws Exception
	 */
	public static void copyBinaryFile(File in, File out) throws Exception {
		FileInputStream fis = new FileInputStream(in);
		try {
			FileOutputStream fos = new FileOutputStream(out);
			try {
				byte[] buf = new byte[1024];
				int i = 0;
				while ((i = fis.read(buf)) != -1) {
					fos.write(buf, 0, i);
				}
			} finally {
				fos.close();
			}
		} finally {
			fis.close();
		}

	}

	
	/**
	 * Check if a given file contains a web page or not
	 * @param file the file to check
	 * @return true if the content of the file is web content, false otherwise
	 * @throws IOException
	 */
	public static boolean checkForWebPage(File file) throws IOException {
		boolean res = false;
		try {
			FileInputStream fis= new FileInputStream(file);
			try {
				byte[] buf = new byte[1024];
				int i = 0;
				// if the file contains enough character
				int byteToRead = 10;
				if (fis.available() >= byteToRead) {
					// 1. read first characters (up to path of css file)
					StringBuilder textBuf = new StringBuilder();
					while ((i < byteToRead) || (i == -1)) {
						i = fis.read(buf);
						textBuf.append(new String(buf,"UTF-8"));
					}
					String text = textBuf.toString();
					if (text.startsWith("<?xml")
							|| text.startsWith("<!DOCTYPE")
							|| text.startsWith("<html>")
							|| text.startsWith("<wml>")) {
						res = true;
					}
				}
			} finally {	fis.close(); }
	    	return res;
	    } catch (IOException e) {
	        System.err.println("Cannot find " + file);
	    	throw e;
	    } 
	}
	
	/**
	 * Verify the extension of a given file
	 * @param src the given file path
	 * @param extension the extension to add if it is necessary
	 * @return the new file path
	 */
	public static String  verifyExtension(String src, String extension) {
		if (src.indexOf(".")!=-1){
			if (!src.substring(src.lastIndexOf(".")).equals(extension)){
				src+=extension;
			}
		}else{
			src+=extension;
		}
		return src;
	}
	
	/**
	 * Remove headers from html output
	 * @param config
	 * @param htmlFileName
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws TransformerException
	 */
	public static void removeHeaders(Configuration config, String htmlFileName) throws ParserConfigurationException, SAXException, IOException, TransformerException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document documentSource = builder.parse(new File(htmlFileName));
		DOMSource domSource = new DOMSource(documentSource);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer(new StreamSource(new File(config.getLibDir(), "forApache.xsl")));
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.METHOD, "html");
        transformer.setOutputProperty(OutputKeys.ENCODING,"ISO-8859-1");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        Result dest = new StreamResult(new File(htmlFileName));
        transformer.transform(domSource, dest); 
	}

    /**
     * Remove a directory. Recursively destroy the files it contains. Should be moved to
     * a set of utility functions.
     * @param f name of the directory to remove
     */
    static public boolean removeDir(File f) {
    	if (f.isDirectory()) {
    		File list [] = f.listFiles();
    		if (list != null) { 
    		    for(int i = 0; i < list.length; i++) removeDir(list[i]);
    		}
    	}
    	return f.delete();
    }

    /** 
     * Utility function that checks the suffix of a string
     * (usually a file name )
     * @param name string where the suffix is searched
     * @param suffix the suffix
     * @return true if the suffix is correct, false otherwise 
     */
    public static boolean checkSuffix(String name, String suffix) {
    	int sz = suffix.length();
    	return name.regionMatches(true,name.length() - sz, suffix, 0 , sz);
    }
}

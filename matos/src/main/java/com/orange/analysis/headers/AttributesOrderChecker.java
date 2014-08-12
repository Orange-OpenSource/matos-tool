
package com.orange.analysis.headers;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Vector;

import com.orange.matos.core.Alert;

/**
 * @author Pierre Cregut Checks that numbered attributes are defined in a given order
 *         This is quite stupid but requested by some profiles.
 */
public class AttributesOrderChecker extends Checker {

    private String attributes;

    private String fileType;

    private InputStream inputStream;

    private String[] attributeInOrderedList;

    private Vector<String> attributeInFile = new Vector<String>();

    /**
     * Constructor version for Jad
     * 
     * @param jadFile the file containing
     * @param jadAttributes the attribute to check
     * @throws FileNotFoundException
     */
    public AttributesOrderChecker(File jadFile, String jadAttributes) throws FileNotFoundException {
        fileType = JAD_TYPE;
        attributes = jadAttributes;
        inputStream = new FileInputStream(jadFile);
    }

    /**
     * Constructor version for Jar
     * 
     * @param jarInputStream
     * @param jarAttributes
     */
    public AttributesOrderChecker(InputStream jarInputStream, String jarAttributes) {
        fileType = JAR_TYPE;
        attributes = jarAttributes;
        inputStream = jarInputStream;
    }

    @Override
    public void check() throws Alert {
        attributeInOrderedList = attributes.split(",");

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            try {
                String line = br.readLine();
                while (line != null) {
                    addAttribute(line);
                    line = br.readLine();
                }
            } finally {
                br.close();
            }
        } catch (IOException ioe) {
            throw Alert.raised(ioe, "Failed to read from the " + fileType);
            // addProblem("Failed to read from the "+fileType); // never called
            // ...
        }
        verdict = verdict && verifyOrder();
    }

    private boolean verifyOrder() {
        int currentIndex = -1;
        for (int i = 0; i < attributeInOrderedList.length; i++) {
            String attribute = attributeInOrderedList[i].trim();
            boolean inVector = false;
            for (String att : attributeInFile) {
                ;
                att = att.trim();
                if (att.equals(attribute)) {
                    inVector = true;
                }
            }
            if (inVector) {
                int newIndex = attributeInFile.indexOf(attribute);
                if (newIndex < currentIndex && verdict) {
                    addProblem("Attributes in " + fileType
                            + " are not in the same order as the order specified in the profile.",
                            "");
                }
                currentIndex = newIndex;
            }
        }
        return verdict;
    }

    private void addAttribute(String aLine) {
        String line = aLine.trim();
        if (line.length() > 0) { // not an empty line
            if ((line.trim().startsWith("#")) || (line.trim().startsWith("!"))) { // this
                                                                                  // is
                                                                                  // a
                                                                                  // comment
                                                                                  // line
                                                                                  // ->
                                                                                  // nothing
                                                                                  // to
                                                                                  // do
            } else {
                String[] splittedLine = line.split(":");
                if (splittedLine.length >= 1) {
                    attributeInFile.add(splittedLine[0]);
                }
            }
        }
    }

    @Override
    public String getAttributeName() {
        return ("Attribute Order Checker");
    }

}

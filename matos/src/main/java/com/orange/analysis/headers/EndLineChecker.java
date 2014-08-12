
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

import com.orange.matos.core.Alert;

/**
 * @author Pierre Cregut Check end of line characters for compatibility with bogus
 *         phones.
 */
public class EndLineChecker extends Checker {

    private String fileType;

    private String endLine;

    private long fileSize;

    private InputStream inputStream;

    private String[] allEndLineCharacters = {
            "\r\n", "\r", "\n"
    };

    private String[] permittedCharacters;

    /**
     * JAD constructor
     * 
     * @param jadFile
     * @param jadEndLine
     * @throws FileNotFoundException
     */
    public EndLineChecker(File jadFile, String jadEndLine) throws FileNotFoundException {
        fileType = JAD_TYPE;
        endLine = jadEndLine;
        fileSize = jadFile.length();
        inputStream = new FileInputStream(jadFile);
    }

    /**
     * JAR Constructor
     * 
     * @param jarInputStream
     * @param jadEndLine
     * @param jarManifestSize
     */
    public EndLineChecker(InputStream jarInputStream, String jadEndLine, long jarManifestSize) {
        fileType = JAR_TYPE;
        endLine = jadEndLine;
        fileSize = jarManifestSize;
        inputStream = jarInputStream;
    }

    @Override
    public void check() throws Alert {
        permittedCharacters = endLine.split(",");
        String strContent = null;
        char[] content = new char[(int) fileSize];
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            try {
                if (br.read(content) < content.length) {
                    throw new IOException("Not the expected size");
                }
                strContent = new String(content);
            } finally {
                br.close();
            }
        } catch (IOException ioe) {
            throw Alert.raised(ioe, "Failed to read from the " + fileType);
            // addProblem("Failed to read from the "+fileType);
        }
        for (int i = 0; i < allEndLineCharacters.length; i++) {
            String endLineCharacter = allEndLineCharacters[i];
            if (strContent.indexOf(endLineCharacter) != -1) {// the character is
                                                             // present
                if (!permitted(endLineCharacter) && verdict) {
                    addProblem("A end line character used in the " + fileType
                            + " is not permitted.", "");
                }
                int endIndex = strContent.indexOf(endLineCharacter);
                while (endIndex != -1) {
                    if (endIndex + endLineCharacter.length() < fileSize) {
                        String endString = strContent.substring(endIndex
                                + endLineCharacter.length());
                        String beginString = strContent.substring(0, endIndex);
                        strContent = beginString + endString;
                    } else {
                        strContent = strContent.substring(0, endIndex);
                    }
                    endIndex = strContent.indexOf(endLineCharacter);
                }
            }
        }
    }

    private boolean permitted(String endLineCharacter) {
        boolean permitted = false;
        int i = 0;
        while (i < permittedCharacters.length && !permitted) {
            if (endLineCharacter.equals(permittedCharacters[i])) {
                permitted = true;
            }
            i++;
        }
        return permitted;
    }

    @Override
    public String getAttributeName() {
        return "End Line Checker";
    }

}

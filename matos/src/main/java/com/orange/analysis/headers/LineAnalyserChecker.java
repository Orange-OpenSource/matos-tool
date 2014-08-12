
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.orange.matos.core.Alert;

/**
 * @author Pierre Cregut Analyze the file line by line. It is an abstract class. Used
 *         to find duplicates (that do not show up in the property file
 *         produced) and line separators. It is a low level checker.
 */
public abstract class LineAnalyserChecker extends Checker {

    protected String fileType;

    protected InputStream inputStream;

    @Override
    public abstract void check() throws Alert;

    /**
     * Get the lines of the input file to prepare the check.
     * 
     * @throws Alert
     */
    public void splitLines() throws Alert {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            try {
                String line = br.readLine();
                while (line != null) {
                    verdict = parseLine(line, verdict) && verdict;
                    line = br.readLine();
                }
            } finally {
                br.close();
            }
        } catch (IOException ioe) {
            throw Alert.raised(ioe, "Failed to read from the " + fileType);
            // addProblem("Failed to read from the "+fileType);
        }
    }

    protected abstract boolean parseLine(String line, boolean verdict);

}

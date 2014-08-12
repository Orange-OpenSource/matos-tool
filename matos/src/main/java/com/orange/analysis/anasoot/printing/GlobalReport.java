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

import java.util.HashMap;
import java.util.Map;

/**
 * Global structure of the report.
 * @author Pierre Cregut
 *
 */
public class GlobalReport {
    /**
     * Reports associated to the rule file
     */
    private Map <String,JavaReport> reports = new HashMap<String,JavaReport>();

    private StructureReport structure = null;
    private ScoreReport score = null;

    /**
     * Set the score part of the report.
     * @param score
     */
    public void setScore(ScoreReport score) {
        this.score = score;
    }
    
    /**
     * Set the structure definition of the report.
     * @param structure
     */
    public void setStructureReport(StructureReport structure) {
        this.structure = structure;
    }

 
    /**
     * Get the main structured report. Structured report tend to replace
     * individual reports as they provide a global structure to the HTML file
     * produced.
     * @return
     */
    public StructureReport getStructure() {
        return structure;
    }

    /**
     * Get the score report that describes how to compute the score of an application.
     * @return
     */
    public ScoreReport getScore() {
        return score;
    }
    
    /**
     * Adds a java report.
     * @param reportName
     * @param report
     */
    public void put(String reportName, JavaReport report) {
        reports.put(reportName, report);
    }
    
    /**
     * Check if a java report is registered.
     * @param reportName
     * @return
     */
    public boolean contains(String reportName) { return reports.containsKey(reportName); }
    
    /**
     * Gets a java report.
     * @param reportName
     * @return
     */
    public JavaReport get(String reportName) { return reports.get(reportName); }
    
    /**
     * Direct access to the report map.
     * @return
     */
    public Map<String, JavaReport> getReports() { return reports; } 

}

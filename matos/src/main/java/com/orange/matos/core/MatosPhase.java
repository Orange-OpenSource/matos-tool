/*
 * $Id: MatosPhase.java 2279 2013-12-11 14:45:44Z piac6784 $
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

import java.io.IOException;
import java.io.PrintStream;


/** 
 * This interface defines the interface between Matos and a phase 
 */
public interface MatosPhase {
    /** 
     * Boolean that defines whether a phase is global to all midlets (ex: descriptor consistency checking) or must be applied to each midlet separately (ex: anasoot) 
     */
    boolean isGlobal();
    /** 
     * The name of a phase: it is used to check if the phase must be activated or not for a given analysis profile 
     */
    public String getName();
        
    /** 
     * Called during the initialisation of the system 
     */
    public void init(Configuration config);

    /** 
     * Callback called on each midlet or on the midletsuite if the phase is global.
     * @param midletName : the name of the midlet
     * @param jarFile : the archive containing the code of the midlet suite
     * @param jadFile : the descriptor of the midlet suite
     * @param props : context describing the properties activated by the profile
     * @param ruleFile : the contents of the analysis profile
     * @param outStream : the stream for the result 
     * @return true only if the phase succeeds. 
     */    
    public boolean run(String midletName, AppDescription desc,
		    RuleFile ruleFile, PrintStream outStream)
		 throws IOException, Alert;
    
    /**
     * An informative message on the phase
     * @return a string or null if nothing to say.
     */
    public String getMessage();
    
    /**
     * Gives back a score for the phase. 
     * @return a positive value or -1 if meaningless for the phase (no score).
     */
    public int getScore();
}

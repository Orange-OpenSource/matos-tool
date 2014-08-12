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

import java.io.File;
import java.util.regex.Pattern;

/**
 * Check MIDlet-Version attribute :
 * <ul> 
 * 	<li> Mandatory attributte in Jad and Jar.
 *  <li> equivalent semantic (ex:  08 is equivalent to 8, 
 *  							1.0 is equivalent to 1.0.0, 
 *  							1.1 is equivalent to 1.1.0 but not to 1.0.1)
 *  </ul>
 * 
 * @author Nicolas Moteau
 */
public class MidletVersionAttrChecker extends MandJadJarAttrChecker {

	/**
	 * Constructor.
	 * @param jadFile
	 */
	public MidletVersionAttrChecker(File jadFile) {
		super("MIDlet-Version",	"^[0-9][0-9]?[.][0-9][0-9]?([.][0-9][0-9]?)?$", jadFile);
	}
	
	@Override
	public void check(){
		super.check();
		if (!areEquivalent(injad, injar)) {
			addProblem("Values differ from the JAR Manifest to the JAD file:", name);
		}
	}
	
	/**
	 * Check for equivalence between MIDlet version in jad and jar
	 * @param jadversion
 	 * @param jarversion
 	 * @return true if equivalent, false otherwise
 	 */	
	public boolean areEquivalent(String jadversion, String jarversion) {
		boolean equi = true;
		if ((jadversion!=null) && (jarversion!=null) && 
			(Pattern.matches(regexp,jadversion)) && (Pattern.matches(regexp,jarversion))) { // if correct values
			
			int jadFstPtIndex = jadversion.indexOf('.'); // cannot be -1 thanks to regexp
			int jadSndPtIndex = jadversion.indexOf('.', jadFstPtIndex+1);
			int jarFstPtIndex = jarversion.indexOf('.'); // cannot be -1 thanks to regexp
			int jarSndPtIndex = jarversion.indexOf('.', jarFstPtIndex+1);
			
			// check Majors
			int majorJad = 0;
			int majorJar = 0;
			majorJad = Integer.valueOf( jadversion.substring(0, jadFstPtIndex) ).intValue();
			majorJar = Integer.valueOf( jarversion.substring(0, jarFstPtIndex) ).intValue();
			if (majorJad!=majorJar) { equi = false; } 
			else { // check Minors
				int minorJad = 0;
				int minorJar = 0;
				if (jadSndPtIndex==-1) {
					minorJad = Integer.valueOf( jadversion.substring(jadFstPtIndex+1) ).intValue();
				} else {
					minorJad = Integer.valueOf( jadversion.substring(jadFstPtIndex+1, jadSndPtIndex) ).intValue();
				}
				if (jarSndPtIndex==-1) { 
					minorJar = Integer.valueOf( jarversion.substring(jarFstPtIndex+1) ).intValue();
				} else {
					minorJar = Integer.valueOf( jarversion.substring(jarFstPtIndex+1, jarSndPtIndex) ).intValue();
				}
				if (minorJad!=minorJar) { equi = false; } 
				else { // check Micros
					int microJad = 0;
					int microJar = 0;
					if (jadSndPtIndex!=-1) {
						microJad = Integer.valueOf( jadversion.substring(jadSndPtIndex+1) ).intValue();
					}
					if (jarSndPtIndex!=-1) {
						microJar = Integer.valueOf( jarversion.substring(jarSndPtIndex+1) ).intValue();;
					}
					if (microJad!=microJar) { equi = false; }
				}
			}
		}
		return equi;
	}

}

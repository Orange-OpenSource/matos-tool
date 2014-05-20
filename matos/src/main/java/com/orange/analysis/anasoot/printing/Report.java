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

import com.orange.analysis.anasoot.result.JavaResult;
import com.orange.matos.core.Alert;

/**
 * @author piac6784
 * Represents a report.
 */
public abstract class Report {
		
	protected String name;
    protected boolean finalVerdict = true;

    /**
     * has an out to print.
     */
    public boolean hasOut = false;
    
    protected static String toRegularExpression(String httpFromCid) {
		StringBuilder httpAuthorized = new StringBuilder();
		if (httpFromCid!=null && httpFromCid.length()!=0){
			httpAuthorized.append("\\x22(");
			String [] urls = httpFromCid.split(",");
			for (int i=1; i<=urls.length; i++){
				httpAuthorized.append(urls[i-1].trim());
				if (i==urls.length){
					httpAuthorized.append(").*");
				}else{
					httpAuthorized.append("|");
				}
			}
		}
		return httpAuthorized.toString();
	}
    
	/**
	 * Verdict of an analysis
	 * @return ok if true
	 */
	public boolean getFinalVerdict() {
		return finalVerdict;
	}

	/**
	 * Main printing function.
	 * @param outStream stream to print to
	 * @param xmlFormat format of printing.
	 * @param result result to be printed.
	 * @param position position.
	 * @throws Alert
	 */
	public abstract void tell(PrintStream outStream, boolean xmlFormat,
			JavaResult result, int position) throws Alert;

	/**
	 * Name of the report.
	 * @return
	 */
	public String getName() { return name; }
}

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
import java.util.HashMap;

import com.orange.analysis.anasoot.result.AbsValue;
import com.orange.analysis.anasoot.result.AndValue;
import com.orange.analysis.anasoot.result.JavaResult;
import com.orange.matos.core.Alert;

/**
 * @author Pierre Cregut
 * Simple messages.
 */
public class ReportMessage extends JavaReport {
	static HashMap <String, String> lastOfKind = new HashMap<String,String>();
	private final String message;
	private final String last;
	
	/**
	 * @param name : name of the message. 
	 * @param message : simple message but may contain
	 * @param last : category for unique printing.
	 */
	public ReportMessage(String name, String message, String last) {
		this.name = name;
		this.message = message;
		this.last = last;
	}
	
	/**
	 * Reset the mechanism that ensures that we only print it once for
	 * a given category. Used for tables.
	 */
	public static void resetLast() {
		lastOfKind.clear();
	}
	
	@Override
	public void tell(PrintStream outStream, boolean xmlFormat, JavaResult result, int position) throws Alert {
		if (last != null) {
			if (last.length() > 1 && last.charAt(0) == '-') {
				if (! this.name.equals(lastOfKind.get(last))) {
					lastOfKind.put(last, this.name);
					return;
				}
			} else {
				if (! this.name.equals(lastOfKind.get(last))) {
					lastOfKind.put(last, this.name);
				} else return;
			}
		}

		AbsValue absvalue = ((JavaResult) result).argument;
		if (position != -1 && absvalue instanceof AndValue) {
			try {
				absvalue = ((AndValue) absvalue).get(position);
			} catch (Exception e) {
				throw Alert.raised(e, "Index of message out of bound for report " + name + ": " + position);
			}
		}
		String val;
		if (message.indexOf("%r") >= 0) {
			absvalue = absvalue.normalize(false);
			val = absvalue.toString();
		} else val = "";
		JavaResult jr = (JavaResult) result;
		if (!xmlFormat) {
			print(outStream, message, val, jr, null);
		}
	}
	
}

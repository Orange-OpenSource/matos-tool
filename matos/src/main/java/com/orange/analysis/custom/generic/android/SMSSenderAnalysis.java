package com.orange.analysis.custom.generic.android;

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

import java.util.Deque;

import soot.Scene;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.Edge;

import com.orange.analysis.anasoot.spy.CallGraphUtil;
import com.orange.analysis.anasoot.spy.CustomSemanticRule;
import com.orange.analysis.anasoot.spy.SpyResult;
import com.orange.matos.core.AppDescription;

/**
 * @author piac6784
 * Analyse how sending SMS is used in the APK. It is a dumb but sometimes effective approach to check delayed sending.
 */
public class SMSSenderAnalysis implements CustomSemanticRule, CallGraphUtil.Visitor {
	String [] sources = {
			"<android.telephony.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>",
			"<android.telephony.SmsManager: void sendMultipartTextMessage(java.lang.String,java.lang.String,java.util.ArrayList,java.util.ArrayList,java.util.ArrayList)>",
			"<android.telephony.SmsManager: void sendDataMessage(java.lang.String,java.lang.String,short,byte[],android.app.PendingIntent,android.app.PendingIntent)>",
			"<android.telephony.gsm.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>",
			"<android.telephony.gsm.SmsManager: void sendMultipartTextMessage(java.lang.String,java.lang.String,java.util.ArrayList,java.util.ArrayList,java.util.ArrayList)>",
			"<android.telephony.gsm.SmsManager: void sendDataMessage(java.lang.String,java.lang.String,short,byte[],android.app.PendingIntent,android.app.PendingIntent)>"
	};
	int countTiming = 0;
	int countNoControl = 0;
	SpyResult result;
	
	/**
     * Makes an array key with a id added.
     * @param key
     * @param id
     * @return
     */
    private static String listItem(String key, int id) { return key + "." + id; }
    
	@Override
	public void run(SpyResult result, AppDescription app) {
		this.result = result;
		Scene scene = Scene.v();
		for(String signature : sources) {
			try {
				SootMethod m = scene.getMethod(signature);
				CallGraphUtil.visitAncestors(m, this);
			} catch (RuntimeException e) { }
		}
		if (countTiming > 0) {
			result.setCustomResult("android.sms.delayed", true);
			result.setCustomResult("android.sms.delayed.count", countTiming);
		}
		if (countNoControl > 0) {
			result.setCustomResult("android.sms.nocontrol", true);
			result.setCustomResult("android.sms.nocontrol.count", countNoControl);
		}
		
	}
	
	private String sigOfMethod(SootMethod m) {
		String s = m.getSignature();
		return s.substring(1, s.length() - 1);
	}
	
	@Override
	public void visit(Edge e, Deque<SootMethod> stack) {
		SootMethod m = e.src();
		System.out.println(m);
		if (m.getSignature().equals("<com.francetelecom.rd.fakeandroid.Wrapper: void main(java.lang.String[])>")) {
			System.out.println(stack);
			boolean assumeControl = false;
			boolean timing = false;
			for(SootMethod mc : stack) {
				if (mc.getName().startsWith("onClick")) { assumeControl = true; }
				if (mc.getDeclaringClass().getName().equals("java.util.Timer")) { timing = true; }
			}
			if (timing || !assumeControl) {
				StringBuilder path = new StringBuilder(sigOfMethod(m));
				for(SootMethod mc : stack) {
					path.append( " -> ");
					path.append(sigOfMethod(mc));
				}
				if (timing) {
					result.setCustomResult(listItem("android.sms.delayed.path",countTiming),path.toString());
					countTiming++;
				} else {
					result.setCustomResult(listItem("android.sms.nocontrol.path",countNoControl),path.toString());
					countNoControl++;
				}
			}
		}
	}

}

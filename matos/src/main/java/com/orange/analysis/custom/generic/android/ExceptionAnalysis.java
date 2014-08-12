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

import java.util.Iterator;

import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Trap;
import soot.Unit;
import soot.jimple.GotoStmt;
import soot.jimple.ReturnVoidStmt;
import soot.tagkit.BytecodeOffsetTag;
import soot.util.Chain;

import com.orange.analysis.anasoot.spy.CustomSemanticRule;
import com.orange.analysis.anasoot.spy.SpyResult;
import com.orange.matos.core.AppDescription;

/**
 * @author Pierre Cregut
 * Analyse trap handlers looking for empty handlers. This is not more and not less than the similar checker in
 * Findbugs.
 */
public class ExceptionAnalysis implements CustomSemanticRule {
    /**
     * Makes an array key with a id added.
     * @param key
     * @param id
     * @return
     */
    private static String listItem(String key, int id) { return key + "." + id; }
    

	@Override
	public void run(SpyResult result, AppDescription app) {
		Scene scene = Scene.v();
		int count = 0;
		Iterator<SootClass> classes = scene.getApplicationClasses().iterator();
		while(classes.hasNext()) {
			SootClass clazz = classes.next();
			for (SootMethod method : clazz.getMethods()) {
				if(!method.hasActiveBody()) continue;
				Body body = method.getActiveBody();
				Chain<Unit> code = body.getUnits();
				Iterator<Trap> traps = body.getTraps().iterator();
				while(traps.hasNext()) {
					boolean bogus = false;
					Trap trap = traps.next();
					Unit lastOfCatch = trap.getEndUnit();
					// System.err.println("Last of catch " + lastOfCatch);
					Unit firstHandler = trap.getHandlerUnit();
					// System.err.println("First of handler " + firstHandler);
					Unit nextHandler = code.getSuccOf(firstHandler);
					// System.err.println("Next handler " + nextHandler);
					if (nextHandler == null) continue;
					Unit handlerTarget = seekTarget(nextHandler);
					if (handlerTarget == null) continue;
					// System.err.println("handler goto " + handlerTarget + handlerTarget.getClass());
					if (handlerTarget instanceof ReturnVoidStmt) {
						bogus = true;
					} else if (lastOfCatch instanceof GotoStmt) {
						bogus = handlerTarget.equals(seekTarget(lastOfCatch));
					} else {
						if (!lastOfCatch.fallsThrough()) continue;
						Unit targetCatch = code.getSuccOf(lastOfCatch);
						if (targetCatch == null) continue; // This had to be a return.
						// System.err.println("Target 1" + targetCatch);
						if (handlerTarget.equals(targetCatch)) bogus = true;
						if (targetCatch.fallsThrough() && !targetCatch.branches()) {
							targetCatch = code.getSuccOf(targetCatch);
							// System.err.println("Target 2" + targetCatch);
							if (handlerTarget.equals(targetCatch)) bogus = true;
						}
					}
					
					// System.err.println(bogus);
					if (bogus) {
						BytecodeOffsetTag tag = (BytecodeOffsetTag) firstHandler.getTag("BytecodeOffsetTag");
						int offset = (tag == null) ? -1 : tag.getBytecodeOffset();
						result.setCustomResult(listItem("android.empty.exception.class",count),clazz.getName());
						result.setCustomResult(listItem("android.empty.exception.method",count),method.getSubSignature());
						result.setCustomResult(listItem("android.empty.exception.pos",count),offset);
						count++;
					}
					
				}
				result.setCustomResult("android.empty.exception.error", count > 0);
				result.setCustomResult("android.empty.exception.count", count);
			}
		}
	}
	
	private Unit seekTarget(Unit u) {
		int count = 10;
		Unit target = u;
		while(count-- > 0 && target != null && target instanceof GotoStmt) {
			target = ((GotoStmt) target).getTarget();
		}
		return target;
	}

}

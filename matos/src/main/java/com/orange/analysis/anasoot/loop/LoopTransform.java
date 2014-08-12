package com.orange.analysis.anasoot.loop;

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
import java.util.Map;
import java.util.Set;

import soot.SceneTransformer;
import soot.SootMethod;

import com.orange.analysis.anasoot.loop.CallbackResolver.Translation;
import com.orange.matos.core.RuleFile;

/**
 * Soot Transformation that performs an analysis of the presence of loops.
 * @author Pierre Cregut
 *
 */
public class LoopTransform extends SceneTransformer {

	private PrintStream outStream;
	private RuleFile rulefile;

	/**
	 * The constructor.
	 * @param outStream where to print result
	 * @param rulefile rules to check.
	 */
	public LoopTransform(PrintStream outStream, RuleFile rulefile) {
		this.outStream = outStream;
		this.rulefile = rulefile;
	}

	@Override
	protected void internalTransform(String phaseName,@SuppressWarnings("rawtypes") Map options) {
		LoopParser loopparser = new LoopParser(rulefile);
		if (!loopparser.configured()) return;
		Set<Translation> translations = loopparser.translations();
		Map<SootMethod, String> criticals = loopparser.criticals();
		Map<SootMethod, String> callbacks = loopparser.callbacks();
		CallbackResolver cba = new CallbackResolver();
		for(Translation tr : translations)
			cba.register(tr);
		// RecAnalysis.doAnalysis(cba,cg);
		Explore explore = new Explore(cba);
		Map<SootMethod, SootMethod> completed_criticals = LoopUtil.complete(criticals);
		for(SootMethod crit : callbacks.keySet())
			explore.doAnalysis(outStream, crit, completed_criticals, callbacks, criticals);
	}

}

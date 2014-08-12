package com.orange.analysis.anasoot.spy;

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

import java.util.Set;

import soot.Unit;

import com.orange.analysis.anasoot.result.AbsValue;

/**
 * @author Pierre Cregut
 * Represents a category of calls for which a specific analysis must be done.
 */
public interface AnalyzeCallHandler {
	/**
	 * @param cd
	 * @param stmt statement where it occurs
	 * @param seen Set of statement already seen to avoid loops.
	 * @return
	 */
	public AbsValue abstractCall(CallDescription cd, Unit stmt, Set <Unit>  seen);
}

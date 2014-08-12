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

import com.orange.analysis.anasoot.result.ProgramPoint;

import soot.SootMethod;
import soot.Type;
import soot.jimple.Stmt;

/**
 * @author Pierre Cregut
 * A program point where the type is used as a diferentiator.
 */
public class TypedProgramPoint extends ProgramPoint {
	Type typ;
	
	/**
	 * Construtctor
	 * @param method method containing the pp
	 * @param stmt statement of the pp
	 * @param typ type of potential implementation considered.
	 */
	public TypedProgramPoint(SootMethod method, Stmt stmt, Type typ) {
		super(method,stmt);
		this.typ = typ;
	}

	@Override
	public int hashCode() {
		return offset ^ method.getNumber();
	}

	@Override
	public ProgramPoint eraseType() {
		return new ProgramPoint(method,stmt);
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (o instanceof TypedProgramPoint) {
			TypedProgramPoint pp = (TypedProgramPoint) o;
			return (pp.method.getNumber() == method.getNumber() && pp.offset == offset && pp.typ.equals(typ));
		} else if (o instanceof ProgramPoint) {
			super.equals(o);
		} return false;
	}	
}

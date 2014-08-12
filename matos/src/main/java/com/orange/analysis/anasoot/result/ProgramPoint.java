package com.orange.analysis.anasoot.result;

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

import soot.SootMethod;
import soot.jimple.Stmt;
import soot.tagkit.BytecodeOffsetTag;

import com.orange.matos.core.XMLStream;

/**
 * @author Pierre Cregut
 *
 */
public class ProgramPoint {
	/**
	 * method.
	 */
	final public SootMethod method;
	/**
	 * offset of statement in method bytecode
	 */
	final public int offset;
	/**
	 * Statement
	 */
	final public Stmt stmt;
	
	/**
	 * Constructor
	 * @param method
	 * @param stmt
	 */
	public ProgramPoint(SootMethod method, Stmt stmt) {
		BytecodeOffsetTag tag = (BytecodeOffsetTag) stmt.getTag("BytecodeOffsetTag");
		offset = (tag == null) ? -1 : tag.getBytecodeOffset(); 
		this.method = method;
		this.stmt = stmt;
	}
	
	/**
	 * XML pretty printing
	 * @param xmlout
	 */
	public void out(XMLStream xmlout) {
		xmlout.element("pp");
		xmlout.attribute("method", method.getSignature());
		xmlout.attribute("offset", offset);
	}
	
	@Override
	public String toString() {
		return "[" + offset + "@" + method + "]";
	}
	
	/**
	 * Forget about differences due to potential candidate type.
	 * @return an equivalent program point without the type
	 */
	public ProgramPoint eraseType() {
		return this;
	}
	
	@Override
	public int hashCode() {
		return offset ^ method.getNumber();
	}
	@Override
	public boolean equals(Object o) {
		if (o == null || ! (o instanceof ProgramPoint)) return false;
		ProgramPoint pp = (ProgramPoint) o;
		return (pp.method.getNumber() == method.getNumber() && pp.offset == offset);
	}
}

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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Set;

import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;

import com.orange.matos.core.XMLStream;

/**
 * Represents a concatenation of abstract results (strings).
 * @author Pierre Cregut
 *
 */
@XmlRootElement(name="Concat")
public class ConcatValue extends AbsValue {
	/**
	 * The list of strings (abstract) contained in the concatenation. 
	 */
	@XmlElementRef
	public final ArrayList <AbsValue> contents;
	
	/**
	 * Empty constructor. Mostly for use with reflection. 
	 */
	public ConcatValue() {
		contents = new ArrayList <AbsValue>();
	}
	
	/**
	 * Constructor for the concatenation of two strings.
	 * @param v1
	 * @param v2
	 */
	public ConcatValue(AbsValue v1,AbsValue v2) {
		contents = new ArrayList <AbsValue>();
		contents.add(v1);
		contents.add(v2);
	}
	
	/**
	 * Constructor for a list of concatenated strings.
	 * @param l
	 */
	public ConcatValue(ArrayList <AbsValue> l) { contents = l; }
	
	/**
	 * Adds a new string to the concatenation (at its end).
	 * @param v
	 */
	public void add(AbsValue v) { contents.add(v); }

	@Override
	public void xml(XMLStream out) {
		out.element("Concat");
		for(AbsValue val : contents) val.xml(out);
		out.endElement();
	}
	
	@Override
	public AbsValue normalize(boolean b, Set <Integer> seen) {
		ArrayList<AbsValue> buffer = new ArrayList<AbsValue>();
		StringBuffer prefixBuffer = new StringBuffer();
		for(AbsValue e: contents) {
			AbsValue n = e.normalize(b, seen);
			if (n instanceof ConcatValue) 
				buffer.addAll(((ConcatValue) n).contents);
			else buffer.add(n);
		}
		int pos = 0;
		for(AbsValue elt: buffer) {
			if (elt instanceof StringValue) {
				prefixBuffer.append(((StringValue) elt).value);
				pos ++;
			} else break;
		}
		String prefix = prefixBuffer.toString();
		ArrayList<AbsValue> result = new ArrayList<AbsValue>();
		if (prefix.length() > 0) result.add(new StringValue(prefix));
		for (int j = pos; j < buffer.size(); j++ ) result.add(buffer.get(j));
		if (result.size() == 1) return result.get(0);
		else return new ConcatValue(result);
	}

	@Override
	public void explore(ValueVisitor visitor, Set <Integer> seen) {
		visitor.visit(this);
		for(AbsValue e: contents) e.explore(visitor,seen);
	}

	@Override
	public void text(PrintStream out) {
		out.println("(");
		boolean first = true;
		for(AbsValue val : contents) {
			if (first) first = false;
			else out.println(" + ");
			val.text(out);
		}
		out.println(")");
	}	
	
	@Override
	public String toString() {
		StringBuilder buf =  new StringBuilder();
		buf.append("(");
		boolean first = true;
		for(AbsValue val : contents) {
			if (first) first = false;
			else buf.append(" + ");
			if (buf.length() > 10000) {buf.append("..."); break; } 
			buf.append(val);
		}
		buf.append(")");	
		return buf.toString();
	}

	@Override
	public boolean isPseudoConstant(Set<MarkValue> s) {
		return false;
	}
	
}

/*
 * $Id: AndValue.java 2279 2013-12-11 14:45:44Z piac6784 $
 */
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
 * This class represents a tuple of results and is generated during the analysis of several
 * arguments of a method call.
 * @author piac6784
 *
 */
@XmlRootElement(name="And")
public class AndValue extends AbsValue {
	@XmlElementRef
	private final ArrayList<AbsValue> vals; // [AbsValue] ArrayList
	
	/**
	 * Empty constructor
	 */
	public AndValue () {
		vals = new ArrayList <AbsValue> ();
	}
	
	/**
	 * Constructor with already a list of elements.
	 * @param lv
	 */
	public AndValue (ArrayList <AbsValue> lv) {
		vals = lv;
	}
	
	/**
	 * Add a new element
	 * @param v
	 */
	public void add(AbsValue v) { vals.add(v); }

	@Override
	public AbsValue normalize(boolean b, Set <Integer> seen) {
		if (vals.size() == 1) return vals.get(0).normalize(b, seen);
		ArrayList<AbsValue> l = new ArrayList<AbsValue>();
		for(AbsValue e: vals) {
			l.add(e.normalize(b, seen));
		}
		return new AndValue(l);
	}
	
	@Override
	public void xml(XMLStream out) {
		out.element("And");
		for(AbsValue val : vals) val.xml(out);
		out.endElement();
	}
	
	@Override
	public String toString() { 
		boolean first = true;
		StringBuffer result = new StringBuffer("[ ");
		for(AbsValue v : vals) {
			if (first) first = false;
			else result.append(" - ");
			result.append(v);
		}
		result.append(" ]");
		return result.toString();
	}

	@Override
	public void explore(ValueVisitor visitor, Set <Integer> seen) {
		visitor.visit(this);
		for(AbsValue e: vals) e.explore(visitor, seen);
	}

	/**
	 * Access the argument at position given as argument in the result.
	 * @param position the position starting from 0 (warning numbering of the and is not the
	 * numbering of the arguments in the  call but the one in the analysis rule).
	 * @return The corresponding contents as AbstractResult.
	 */
	public AbsValue get(int position) {
		return vals.get(position);
	}

	/**
	 * Gives back the number of elements contained.
	 * @return size of array.
	 */
	public int size() {
		return vals.size();
	}

	@Override
	public void text(PrintStream out) {
		out.println("[");
		boolean first = true;
		for(AbsValue val : vals) {
			if (first) first = false;
			else out.println(" &amp;&amp; ");
			val.text(out);
		}
		out.println("]");
	}

	@Override
	public boolean isPseudoConstant(Set<MarkValue> s) {
		for(AbsValue v: vals) { if (!v.isPseudoConstant(s)) return false; } 
		return true;
	}
}

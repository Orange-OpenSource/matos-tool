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
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;

import com.orange.matos.core.XMLStream;

/**
 * @author piac6784
 * Representation of possible different abstract value (disjunction).
 */
@XmlRootElement(name="Or")
public class OrValue extends AbsValue {
	/**
	 * Possible abstract values.
	 */
	@XmlElementRef
	public final List <AbsValue> vals;

	/**
	 * EMpty constructor
	 */
	public OrValue() {
		vals = new ArrayList <AbsValue>();
	}

	/**
	 * Direct construction.
	 * @param l
	 */
	public OrValue(ArrayList<AbsValue> l) { 
		vals = l;
	}

	/**
	 * More generic constructor with an arbitrary collection.
	 * @param l
	 */
	public OrValue(Collection<AbsValue> l) { 
		this();
		vals.addAll(l);
	}

	/**
	 * Adds a new alternative.
	 * @param v
	 */
	public void add(AbsValue v) {
		if (v instanceof OrValue) vals.addAll(((OrValue) v).vals);
		else vals.add(v);
	}

	@Override
	public boolean isEmpty() {
		return vals.size() == 0;
	}

	/**
	 * Simplify the contents. eliminate the construct if only one element.
	 * @return
	 */
	public AbsValue simplify() {
		if (vals.size() == 1) return vals.get(0);
		return this;
	}

	@Override
	public void xml(XMLStream out) {
		out.element("Or");
		for(AbsValue val : vals) val.xml(out);
		out.endElement();
	}

	@Override
	public AbsValue normalize(boolean b, Set <Integer> seen) {
		OrValue r = new OrValue();
		for(AbsValue e: vals) r.add(e.normalize(b,seen));
		if (r.vals.size() == 1) return r.vals.get(0);

		return r;
	}

	@Override
	public String toString() { 
		boolean first = true;
		StringBuffer result = new StringBuffer();
		for(AbsValue v : vals) {
			if (first) first = false;
			else result.append(", ");
			result.append(v);
		}
		return result.toString();
	}

	@Override
	public void explore(ValueVisitor visitor, Set <Integer> seen) {
		visitor.visit(this);
		for(AbsValue e: vals) e.explore(visitor,seen);
	}

	@Override
	public void text(PrintStream out) {

		out.println("[");
		boolean first = true;
		for(AbsValue val : vals) {
			if (first) first = false;
			else out.println(" || ");
			val.text(out);
		}
		out.println("]");
	}

	@Override
	public boolean isPseudoConstant(Set<MarkValue>s) {
		for(AbsValue v: vals) { if (!v.isPseudoConstant(s)) return false; } 
		return true;
	}

}

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
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.orange.matos.core.XMLStream;

/**
 * @author Pierre Cregut
 * Mark an element to avoid recursion through it (kept in seen).
 */
@XmlRootElement(name="Mark")
public class MarkValue extends AbsValue {
	static boolean blockRecursive = true;
	private static int counter = 0;
	private static Set <Integer> seen = new HashSet <Integer>();
	private int ref;
	@XmlElementRef 
	AbsValue value;
	@XmlAttribute(name="name")
	String name;
	@XmlTransient
	boolean multipleOccurrence;
	@XmlTransient
	int status;
	
	final static int STATUS_UNKNOWN = 0;
	final static int STATUS_RECURSIVE = 2;
	final static int STATUS_USELESS = 1;
	
	/**
	 * Empty constructor.
	 */
	public MarkValue() {
		ref = counter++;
		multipleOccurrence = true;
		status = STATUS_UNKNOWN;
	}
	
	/**
	 * Construct a new mark with a name and a content
	 * @param name
	 * @param v
	 */
	public MarkValue(String name, AbsValue v) {
		ref = counter++;
		this.name = name;
		this.value = v;
	}

	/**
	 * Abstract value marked (content)
	 * @return
	 */
	public AbsValue get() { return value; }
	
	/**
	 * Mark it as seen several time (to be displayed).
	 */
	public void seenMoreThanOnce() { multipleOccurrence = true; }
	
	@Override
	public String toString() {
		if (seen.contains(ref)) return "Mark[" + ref + "]";
		else {
			seen.add(ref);
			String v = value.toString();
			seen.remove(ref);
			return "M[" + ref + "," + v + "]";
		}
	}
	/* Note that we test on status to avoid loop during exploration but this works only if it
	 * is set correctly.
	 * @see com.francetelecom.rd.analysis.anasoot.result.AbsValue#explore(com.francetelecom.rd.analysis.anasoot.result.ValueVisitor)
	 */
	@Override
	public void explore(ValueVisitor visitor, Set <Integer> seen) {
		visitor.visit(this);
		if (!seen.contains(ref)) {
			seen.add(ref);
			value.explore(visitor, seen);
		}
	}

	
	/* The visitor takes into account the fact that there may be loops on MarkValue. Note that
	 * parallel use will lead to marks. This should be replaced with a stack based  accumulator.
	 * How to handle it is not clear yet.
	 * @see com.francetelecom.rd.analysis.anasoot.result.AbsValue#normalize(boolean)
	 */
	@Override
	public AbsValue normalize(boolean b, Set <Integer> seen) {
		if (countDown()) return new UnknownValue("*");		
		if (status == STATUS_UNKNOWN) {
			final Set <MarkValue> seen2 = new HashSet <MarkValue> ();
			ValueVisitor visitor = new ValueVisitor() {
				@Override
				public void visit(AbsValue v) {
					if (v instanceof MarkValue) {
						MarkValue mv = (MarkValue) v;
						if (seen2.contains(mv)) mv.status = STATUS_RECURSIVE;
						else seen2.add(mv);
					} 
				}
			};
			this.explore(visitor);
			for(MarkValue v : seen2) {
				if (v.status == STATUS_UNKNOWN) v.status = STATUS_USELESS;
			}
		}
		if (status == STATUS_RECURSIVE) {
			if (seen.contains(ref) || blockRecursive) {
				return UnknownValue.normalized; 
			} else {
				Set <Integer> newseen = new HashSet <Integer>();
				newseen.addAll(seen);
				newseen.add(ref);
				return value.normalize(b,newseen); 
			}
		} else return value.normalize(b,seen);
	}

	@Override
	public void xml(XMLStream out) {
		out.element("Mark");
		out.attribute("name",name);
		out.attribute("ref", ref);
		if (!seen.contains(ref)) {
			seen.add(ref);
			value.xml(out);
			seen.remove(ref);
		}
		out.endElement();
	}

	@Override
	public void text(PrintStream out) {
		out.print ("#" + name);
		if (!seen.contains(ref)) {
			seen.add(ref);
			out.print("[");
			value.text(out);
			out.print("]");
			seen.remove(ref);
		}
		
	}

	@Override
	protected boolean isPseudoConstant(Set<MarkValue> s) {
		if(s.contains(this)) return false;
		s.add(this);
		boolean b = value.isPseudoConstant(s);
		s.remove(this);
		return b;
	}

}

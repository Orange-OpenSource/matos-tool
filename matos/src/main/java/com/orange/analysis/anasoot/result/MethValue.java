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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;

import com.orange.matos.core.XMLStream;
import com.orange.matos.utils.HtmlOutput;

/**
 * @author piac6784
 * An operation but not a binop (more specific).
 */
@XmlRootElement(name="Method")
public class MethValue extends AbsValue {
	@XmlAttribute(name="name")
	String name;
	@XmlElementRef
	final ArrayList <AbsValue> args;
	
	/**
	 * Empty constructor.
	 */
	public MethValue() {
		args = new ArrayList <AbsValue>();
	}
	
	/**
	 * Generic constructor.
	 * @param n
	 * @param l
	 */
	public MethValue(String n, ArrayList<AbsValue> l) { 
		name = n;
		args = l;
	}
	
	/**
	 * Constructor binary method
	 * @param n
	 * @param v1
	 * @param v2
	 */
	public MethValue(String n, AbsValue v1, AbsValue v2) {
		this();
		name = n;
		args.add(v1);
		args.add(v2);
	}
	
	/**
	 * Constructor unary method
	 * @param n
	 * @param v1
	 */
	public MethValue(String n, AbsValue v1) {
		this();
		name = n;
		args.add(v1);
	}
	
	@Override
	public AbsValue normalize(boolean b, Set <Integer> seen) {
		if (b) return new StringValue("\\[" + name + "\\]");
		ArrayList <AbsValue> normArgs = new ArrayList<AbsValue>();
		for(AbsValue arg: args) {
			AbsValue normArg =   arg.normalize(b,seen);
			normArgs.add(normArg);
		}
		return new MethValue(name, normArgs);
	}

	@Override
	public String toString() { return "M " + HtmlOutput.escape(name) + " " + args; }

	@Override
	public void explore(ValueVisitor visitor, Set <Integer> seen) {
		visitor.visit(this);
		for(AbsValue e: args) e.explore(visitor, seen);
	}

	@Override
	public void xml(XMLStream out) {
		out.element("Method");
		out.attribute("name", name);
		for(AbsValue val : args) val.xml(out);
		out.endElement();
	}

	@Override
	public void text(PrintStream out) {
		out.print(HtmlOutput.escape(name) +"(");
		boolean first = true;
		for(AbsValue val : args) {
			if (first) first = false;
			else out.println(", ");
			val.text(out);
		}
		out.print(")");
	}

	@Override
	public boolean isPseudoConstant(Set<MarkValue>s) {
		return false;
	}

}

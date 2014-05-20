/*
 * $Id: BinopValue.java 2279 2013-12-11 14:45:44Z piac6784 $
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
import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.orange.matos.core.Out;
import com.orange.matos.core.XMLStream;

/**
 * @author piac6784
 * Represent a specific binary operation that is kept in abstract value.
 */
@XmlRootElement(name="Binop")
public class BinopValue extends AbsValue{
	/**
	 * Operator name
	 */
	@XmlAttribute
	public String opname;
	
	/**
	 * First argument abstraction
	 */
	@XmlElement(name="val1")
	@XmlJavaTypeAdapter(ValueRefAdapter.class)
	public AbsValue val1;
	
	/**
	 * Second argument abstraction
	 */
	@XmlElement(name="val2")
	@XmlJavaTypeAdapter(ValueRefAdapter.class)
	public AbsValue val2;
	
	/**
	 * Construct a binary operation.
	 * @param opname name
	 * @param val1 first abstraction
	 * @param val2 second abstraction
	 */
	public BinopValue (String opname, AbsValue val1, AbsValue val2) {
		this.opname=opname;
		this.val1 = val1;
		this.val2 = val2;
	}

	/**
	 * Empty constructor.
	 */
	public BinopValue () {
		this.opname=null;
		this.val1 = null;
		this.val2 = null;
	}
	
	@Override
	public AbsValue normalize(boolean b, Set <Integer> seen) {
		Out.getLog().println("bv");
		return new BinopValue(opname, val1.normalize(b, seen), val2.normalize(b, seen));
	}
	
	@Override
	public String toString() { return "(" + val1 + " " + opname + " " + val2 + ")"; }

	@Override
	public void xml(XMLStream out) {
		out.element("Binop");
		out.attribute("opname",opname);
		out.element("val1"); val1.xml(out); out.endElement();
		out.element("val2"); val2.xml(out); out.endElement();
		out.endElement();
	}
	
	@Override
	public void explore(ValueVisitor visitor, Set <Integer> seen) {
		visitor.visit(this);
		if (val1 != null) val1.explore(visitor,seen);
		if (val2 != null) val2.explore(visitor,seen);
	}

	@Override
	public void text(PrintStream out) {
		out.print(opname + "&lt;");
		val1.text(out);
		out.print(", ");
		val2.text(out);
		out.println("&gt;");
	}

	@Override
	public boolean isPseudoConstant(Set<MarkValue> s) {
		return false;
	}
	
}
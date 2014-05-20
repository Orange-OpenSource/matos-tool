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
import javax.xml.bind.annotation.XmlRootElement;

import soot.BooleanType;
import soot.CharType;
import soot.Type;
import soot.jimple.Constant;
import soot.jimple.FloatConstant;
import soot.jimple.IntConstant;
import soot.jimple.LongConstant;
import soot.jimple.NullConstant;

import com.orange.matos.core.XMLStream;
import com.orange.matos.utils.HtmlOutput;

/**
 * @author piac6784
 * Representation of a constant simple value (primitive) */
@XmlRootElement
public class ConstantValue extends AbsValue {
	/**
	 * string representation of primitive integer type
	 */
	final static String INT_TYPE = "int";
	/**
	 * string representation of primitive long type
	 */
	final static String LONG_TYPE = "long";
	/**
	 * string representation of primitive boolean type
	 */
	final static String BOOLEAN_TYPE = "bool";
	/**
	 * string representation of primitive char type
	 */
	final static String CHAR_TYPE = "char";
	/**
	 string representation of primitive float type
	 */
	final static String FLOAT_TYPE = "float";
	/**
	 * For the null constant
	 */
	final static String NULL = "null";
	/**
	 * Another non simple type (usually a bug)
	 */
	final static String UNKNOWN = "unknown";
	
	
	/**
	 * Representation of the type as a string. It is one of the above simple types. 
	 */
	@XmlAttribute(name="type")
	String rep_t;
	/**
	 * Representation of the value as a string.
	 */
	@XmlAttribute(name="value")
	String rep_v;
	
	/**
	 * Empty constructor
	 */
	public ConstantValue() {}
	
	/**
	 * Builds a representation of a constant from its value expressed as
	 * a Soot constant and its Soot type. It is transformed in an equivalent
	 * pair of strings.
	 * @param v the value of the constant
	 * @param t its type.
	 */
	public ConstantValue(Constant v, Type t) { 
		if (v instanceof IntConstant) {
			int n = ((IntConstant) v).value;
			if (t instanceof CharType) {
				rep_t = CHAR_TYPE;
				rep_v = Character.toString((char) n);
			} else if (t instanceof BooleanType) {
				rep_t = BOOLEAN_TYPE;
				rep_v = Boolean.toString((n != 0));
			} else {
				rep_t = INT_TYPE;
				rep_v = Integer.toString(n);
			}
		} else if (v instanceof LongConstant) {
			rep_t = LONG_TYPE;
			rep_v = Long.toString(((LongConstant) v).value);
		} else if (v instanceof FloatConstant) {
			rep_t = FLOAT_TYPE;
			rep_v = Float.toString(((FloatConstant) v).value);
		} else if (v instanceof NullConstant) {
			rep_t = NULL;
			rep_v = NULL;
		} else {
			rep_t = t.toString();
			rep_v = "";
		}
	}	 

	@Override
	public String toString() {
		if (CHAR_TYPE.equals(rep_t)) return "'" + rep_v + "'";
		else if (LONG_TYPE.equals(rep_t)) return rep_v + "l";
		else if (FLOAT_TYPE.equals(rep_t)) return rep_v + "f";
		else return rep_v;
//		return "ConstantValue(" + rep_v + "," + rep_t + ")";
	}
	
	@Override
	public void xml(XMLStream out) {
		out.element("ConstantValue");
		out.attribute("value", rep_v);
		out.attribute("type", rep_t);
		out.endElement();
	}
	@Override
	public void explore(ValueVisitor visitor, Set <Integer> seen) {
		visitor.visit(this);
	}

	@Override
	public AbsValue normalize(boolean b, Set <Integer> seen) {
		if (b) return new StringValue(rep_v);
		return this;
	}

	@Override
	public void text(PrintStream out) {
		out.print("Constant(" + HtmlOutput.escape(rep_v) + "," + HtmlOutput.escape(rep_t) + ")");	
	}

	
	/**
	 * Display in Hexadecimal format the content of a string
	 */
	public void toHex() {
		if (!INT_TYPE.equals(rep_t)) return;
		try {
			int v = Integer.parseInt(rep_v);
			rep_v = "0x" + Integer.toHexString(v);
		} catch (Exception e) {
			rep_v = "???";
		}	
	}

	@Override
	public boolean isPseudoConstant(Set<MarkValue> s) {
		return true;
	}

}

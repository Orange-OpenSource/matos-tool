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

import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;

import com.orange.matos.core.XMLStream;

/**
 * @author Pierre Cregut
 * Representation of a named property (MIDP specific)
 */
@XmlRootElement(name="Property")
public class PropertyValue extends AbsValue {

	@XmlElementRef
	AbsValue name;
	
	/**
	 * Empty constructor 
	 */
	public PropertyValue() {}
	
	/**
	 * Named constructor with abstraction of the name.
	 * @param n
	 */
	public PropertyValue(AbsValue n) {
		name = n;
	}
	
	@Override
	public
	void explore(ValueVisitor visitor, Set <Integer> seen) {
		visitor.visit(this);
		name.explore(visitor, seen);
	}

	@Override
	public String toString() {
		return "AppProperty(" + name + ")";
	}
	
	@Override
	// Warning : this is not able to handle the case getProperty(v1 | v2);
	public AbsValue normalize(boolean b, Set <Integer> seen) {
		if (b) {
			AbsValue normalized_name = name.normalize(b,seen);
			if (normalized_name instanceof StringValue) {
				return new StringValue("\\{" + ((StringValue) normalized_name).value + "\\}");
			} else return UnknownValue.normalized;
		} else {
			return new PropertyValue(name.normalize(b));
		}
	}

	@Override
	public void xml(XMLStream out) {
		out.element("Property");
		name.xml(out);
		out.endElement();
	}

	@Override
	public void text(PrintStream out) {
		out.print("AppProperty(");
		name.text(out);
		out.print(")");
	}
	

	@Override
	public boolean isPseudoConstant(Set<MarkValue>s) {
		return false;
	}


}

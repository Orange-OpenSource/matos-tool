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

import com.orange.matos.core.XMLStream;
import com.orange.matos.utils.HtmlOutput;

/**
 * @author Pierre Cregut
 * An unknown value (Top in abstract interpretation).
 */
@XmlRootElement(name="Unknown")
public class UnknownValue extends AbsValue {
	@XmlAttribute(name="info")
	String info;

	/**
	 * In production, we do not want Jimple code to appear in results.
	 */
	
	boolean debug = false;
	final static UnknownValue star = new UnknownValue("*");
	final static AbsValue normalized = new StringValue("\\*");
	
	/**
	 * empty constructor.
	 */
	public UnknownValue() {info="*";}
	
	/**
	 * Normal constructor with information on the failure to display.
	 * @param info
	 */
	public UnknownValue(String info) {this.info=info;}
	
	@Override
	public AbsValue normalize(boolean b, Set <Integer> seen) {
		if (b) return normalized;
		return this;
	}

	@Override
	public String toString() { return HtmlOutput.escape(debug ? ("<" + info + ">") : "*"); };
	
	@Override
	public void explore(ValueVisitor visitor, Set <Integer> seen) {
		visitor.visit(this);
	}

	@Override
	public void xml(XMLStream out) {
		out.element("Unknown"); out.attribute("info", info); out.endElement();
	}

	@Override
	public void text(PrintStream out) {
		out.print("**&lt;" + HtmlOutput.escape(info) + "&gt;" );
	}

	@Override
	public boolean isPseudoConstant(Set<MarkValue>s) {
		return false;
	}

}

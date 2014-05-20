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
import java.util.HashMap;
import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.orange.matos.core.XMLStream;
import com.orange.matos.utils.HtmlOutput;

/**
 * @author piac6784
 * The abstraction of an object.
 */
@XmlRootElement(name="Node")
public class NodeValue extends AbsValue {
	NodeTable nt = null;
	/**
	 * Identifier of the abstract object
	 */
	@XmlAttribute
	public int ref = -1;
	/**
	 * Empty constructor.
	 */
	public NodeValue() { }
	
	/**
	 * Regular constructor with registration of node table.
	 * @param n
	 * @param nt
	 */
	public NodeValue(int n, NodeTable nt) {ref = n; this.nt = nt; }
	@Override
	public AbsValue normalize(boolean b, Set <Integer> seen) {
		if (b) return new StringValue("\\*");
		return this;
	}
	
	@Override
	public String toString() {
		int label = getUseLabel(ref);
		Object content = nt.get(ref);
		if (content != null && content instanceof String) {
			return HtmlOutput.escape((String) content) ;
		} else {
			String result = "<a name=\"U" + ref + "-" + label + "\" href=\"#N" + ref +"\">["+ref+"]</a><sup>"+ id_of_int(label) + "</sup>";
			String v = nt.getByteContent(ref, false);
			if (v != null) return result + " " + v;
			v = nt.getStringContent(ref,false);
			if (v != null) return result + " " + v;
			return result;
		}
	}
	
	@Override
	public void explore(ValueVisitor visitor, Set <Integer> seen) {
		visitor.visit(this);
	}
	@Override
	public void xml(XMLStream out) {
		out.element("Node"); out.attribute("ref", ref); out.endElement();
	}
	@Override
	public void text(PrintStream out) {
		int label = getUseLabel(ref);
		out.print("<a name=\"U" + label + "\" href=\"#N" + ref +"\">["+ref+"]</a>");
	}
	
	private int getUseLabel(int ref) {
		HashMap<Integer, String> uses = nt.nodeToUse.get(ref);
		if (uses == null) {
			uses = new HashMap<Integer,String>();
			nt.nodeToUse.put(ref, uses);
		}
		// int id  = cc.countUses ++;
		int id = uses.size() + 1;
		uses.put(id, id_of_int(id)); // String.valueOf(id)); // TODO : Put a meaningfull name here.	
		return id;
	}
	
	private String id_of_int(int i) {
		String result = "";
		while (i > 0) {
			char c = (char) ('a' + ((i - 1) % 26));
			i = i / 26;
			result = c + result;
		}
		return result;
	}

	@Override
	public boolean isPseudoConstant(Set<MarkValue>s) {
		return false;
	}
}

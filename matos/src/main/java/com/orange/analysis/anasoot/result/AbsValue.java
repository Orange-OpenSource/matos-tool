/*
 * $Id: AbsValue.java 2279 2013-12-11 14:45:44Z Pierre Cregut $
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

import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;

import com.orange.matos.core.XMLStream;

/**
 * An AbsValue represents an approximation of the contents of a variable.
 * @author Pierre Cregut
 *
 */
/**
 * @author Pierre Cregut
 *
 */
@XmlRootElement
public abstract class AbsValue  {
	
	private static final int MAX_COUNT = 100;
	static int count;
	
	static boolean countDown() {
		if (count <= 0) { return true;
			
		} 
		count --;
		return false;
	}
	
	static void countInit() { count = MAX_COUNT; }
	
	/**
	 * Empty constructor for reflection.
	 */
	public AbsValue() {}
	
	/**
	 * Normalization method more internal that handles recursion. This is the one that must
	 * be implemented by each subclass.
	 * @param b
	 * @param seen
	 * @return
	 */
	public abstract AbsValue normalize(boolean b, Set <Integer> seen);

	/**
	 * Normalize the result computed by the analysis phase. Normalization implies 
	 * some transformations on the contents where some information are lost.The coding
	 * for full normalization is the following:
	 * <ul>
	 * <li> \[m\] means a call to method m
	 * <li> \* is an unknown value
	 * <li> \\ is an escaped anti-slash
	 * </ul>
	 * @param b if set to true, try to project to a complete string value. 
	 * @return the normalized value
	 */
	public AbsValue normalize(boolean b) { 
		try {
			countInit();
			return normalize (b, new HashSet <Integer> ());
		} catch (OutOfMemoryError e) { return new UnknownValue("*[TOO BIG]*"); }
	}
	
	/**
	 * Apply a visitor on one-self. It is a more internal version that handles recursion.
	 * @param visitor A class containing a callback method.
	 */
	public abstract void explore(ValueVisitor visitor, Set <Integer> seen);

	/**
	 * Apply a visitor pattern on the abstract value.
	 * @param visitor
	 */
	public void explore(ValueVisitor visitor) { explore(visitor, new HashSet <Integer>()) ; }
	/**
	 * A Java to XML marshaller for AbsValue
	 */
	static Marshaller marshaller;
	/**
	 * An XML to Java unmarshaller for AbsValue.
	 */
	static Unmarshaller unmarshaller;
	
	static {
		try {
		JAXBContext jc = 
			JAXBContext.newInstance(AbsValue.class,AndValue.class,BinopValue.class,ConcatValue.class,
				ConstantValue.class,  MarkValue.class, MethValue.class, 
				NodeValue.class, OrValue.class, PropertyValue.class, StringValue.class,
				UnknownValue.class, ValueRef.class);
			marshaller = jc.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
			unmarshaller = jc.createUnmarshaller();
		} catch (JAXBException e) {
			e.printStackTrace();
			marshaller = null;
			unmarshaller = null;
		}
	}

	/**
	 * Print out the value in XML format
	 * Will probably be deprecated because it does not handle recursive values
	 * correctly.
	 * @param out The stream to print to.
	 */
	public void xmlOutput(PrintStream out) {
		if (marshaller != null)
			try {
				marshaller.marshal(this, out);
			} catch (JAXBException e) {
				e.printStackTrace();
				out.println("<bogus/>");
			}
	}
	
	/**
	 * Print out the value in XML format. More complex but correct even if there
	 * are loops in values.
	 * @param out A stream to print to wrapped for XML output.
	 */
	public abstract void xml(XMLStream out);
	
	
	/**
	 * Print out the value as a text. It is escaped for HTML output.
	 * @param out
	 */
	public abstract void text(PrintStream out);
	
	/**
	 * Parse an AbsValue from an XML representation on a stream.
	 * @param ins the input stream
	 * @return the XML value.
	 */
	public static AbsValue xmlInput(InputStream ins) {
		if (unmarshaller != null) {
			try {
				return (AbsValue) unmarshaller.unmarshal(ins);
			} catch (JAXBException e) {
				return null;
			}
		} else return null;
	}

	/**
	 * Check if this correspond to the empty set (an OrValue without element).
	 * @return a boolean
	 */
	public boolean isEmpty() {
		return false;
	}
	
	/**
	 * Returns the empty absValue.
	 * @return
	 */
	static public AbsValue empty() {
		return new OrValue();
	}
	
	/**
	 * @author Pierre Cregut
	 * Thrown when there are too many iterations in printing
	 */
	@SuppressWarnings("serial")
	public static class LoopException extends Exception { }
	
	/**
	 * @author Pierre Cregut
	 * Hack to get out of the loop
	 */
	public static class LoopBreaker {
		int HARD_LIMIT = 500;
		private int countLoop = 0;
		/**
		 * If we iterate too much, stop recursive handling of result.
		 * @throws LoopException
		 */
		public void check() throws LoopException {
			if (countLoop ++ > HARD_LIMIT) throw new LoopException();
		}
		
		/**
		 * Current level of iteration
		 * @return
		 */
		public int current() { return countLoop; }
		
		/**
		 * Check if we should loop out.
		 * @param previous
		 * @param e
		 * @throws LoopException
		 */
		public void handle(int previous, LoopException e) throws LoopException {
			if (previous * 4 < HARD_LIMIT) countLoop = previous;
			else throw e; 
		}
	}
	
	private static AbsValue expandConcat(ArrayList<AbsValue> contents, int position, LoopBreaker lb) throws LoopException {
		lb.check();
		OrValue allResults = new OrValue ();
		AbsValue focus = contents.get(position);
		if (! (focus instanceof OrValue)) return null;
		List <AbsValue> alternatives = ((OrValue) focus).vals;
		int current = lb.current();
		try {
		for (AbsValue h: alternatives) {
			ArrayList <AbsValue> copy = new ArrayList <AbsValue> ();
			copy.addAll(contents);
			copy.set(position, h);
			AbsValue result = prefixNormalize(new ConcatValue(copy).normalize(true), lb);
			allResults.add(result);
		}
		} catch (LoopException e) {
			lb.handle(current,e);
			if (position == 0) return new UnknownValue();
			else {
				ArrayList <AbsValue> cts = new ArrayList <AbsValue> ();
				cts.add(contents.get(0));
				cts.add(new UnknownValue());
				return new ConcatValue(cts);
			}
		}
		return allResults.simplify();		
	}
	
	private static AbsValue prefixNormalize(AbsValue v, LoopBreaker lb) throws LoopException {
		lb.check();
		if (v instanceof ConcatValue) {
			ArrayList <AbsValue> contents = ((ConcatValue) v).contents;
			if ((contents.size() > 1) && (contents.get(0) instanceof OrValue)) {
				return expandConcat(contents,0, lb);
			} else if ((contents.size() > 2)
						&& (contents.get(0) instanceof StringValue) 
						&& (contents.get(1) instanceof OrValue)) {
				return expandConcat(contents,1, lb);
			} else return v;
		} else if (v instanceof OrValue) {
			int current = lb.current();
			List <AbsValue> vals = ((OrValue) v).vals;
			OrValue result = new OrValue ();
			try {
				for(AbsValue a: vals) {
					AbsValue r = prefixNormalize(a,lb);
					result.add(r);
				}
				return result; // No need to simplify.
			} catch (LoopException e) {
				lb.handle(current,e);
				return new UnknownValue();
			}
		} else return v;
	}
	
	/**
	 * Taking a normalized value, normalize it further as a disjunction of potential values with
	 * the longest computable prefix
	 * @param v the normalized AbsValue
	 * @return a further normalized result.
	 */
	public static AbsValue prefixNormalize(AbsValue v) {
		try {
			return prefixNormalize(v, new LoopBreaker());
		} catch (LoopException e) { return new UnknownValue(); }
	}

	/**
	 * Check if the value is a constant or a disjunction of constants.
	 * @return
	 */
	public boolean isPseudoConstant() { return isPseudoConstant(new HashSet<MarkValue>()); }
	
	
	/**
	 * Check if it is a pseudo constant with a loop control
	 * @return
	 */
	protected abstract boolean isPseudoConstant(Set<MarkValue> arg);

}

package com.orange.analysis.anasoot.arrayanalysis;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import soot.Local;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.IntConstant;
import soot.jimple.JimpleBody;
import soot.jimple.NewArrayExpr;
import soot.jimple.Stmt;
import soot.jimple.internal.JCastExpr;
import soot.jimple.spark.pag.AllocNode;
import soot.jimple.spark.pag.PAG;
import soot.jimple.spark.sets.P2SetFactory;
import soot.jimple.spark.sets.PointsToSetInternal;

import com.orange.analysis.anasoot.result.AbsValue;
import com.orange.analysis.anasoot.result.NodeTable;
import com.orange.analysis.anasoot.result.NodeValue;
import com.orange.analysis.anasoot.result.OrValue;
import com.orange.analysis.anasoot.spy.CallContext;
import com.orange.analysis.anasoot.spy.LocalAnalysis;
import com.orange.analysis.anasoot.spy.P2SAux;

/**
 * @author piac6784
 * This is the analysis to get an approximation of the content of a byte array.
 */

/**
 * Performs different kinds of array abstraction.
 * 
 * @author piac6784
 *
 */
public class ArrayAnalysis {

	private final static int UNKNOWN_CONTENTS = - 256;
	
	private static class BAAbstraction {
		/**
		 * Set if somebody writes to it. 
		 */
		boolean isSpoiled;

		private BitSet spoiled = new BitSet();
		/**
		 * Contents if not spoiled.
		 */
		TreeMap <Integer,Integer> contents = new TreeMap <Integer,Integer>();

		private int size = -1;
		/**
		 * @param key
		 * @param v
		 */
		public void set(int key, byte v) {
			if(contents.containsKey(key)) spoil(key);
			else contents.put(key, (int) v);
		}

		/**
		 * Set the size of the array.
		 * @param size
		 */
		public void setSize(int size) { this.size = size; }
		
		/**
		 * Declare the full abstraction as spoiled (ie. Top)
		 */
		public void spoil() {
			isSpoiled = true;
			contents = null;
			spoiled = null;
		}

		/**
		 * Set a specific element to Top.
		 * @param index
		 */
		public void spoil(int index) {
			if (isSpoiled) return;
			if (spoiled == null) {
				spoiled = new BitSet();
			}
			spoiled.set(index);
		}

		@Override
		public String toString() {
			if (isSpoiled) return "*";
			StringBuilder result = new StringBuilder();
			result.append("[");
			int last = size == -1 ? (contents.size() > 0 ? contents.lastKey() : 0) : size;
			for(int i=0; i < last; i++) {
				if (i>0) result.append(",");
				if (spoiled.get(i)) result.append("*");
				else {
					Integer v = contents.get(i);
					if(v==null) result.append("_");
					else result.append(Integer.toHexString( v & 0xFF ));
				}
			}
			if (size == -1) result.append("...");
			result.append("]");
			return result.toString();
		}
	}

	private HashMap <Integer,BAAbstraction>repositoryByteArrays = new HashMap <Integer,BAAbstraction>();
	private HashMap <Integer,AbsValue []> repositoryStringArrays = new HashMap <Integer,AbsValue []>();
	private HashMap <Integer,AbsValue> repositoryOtherArrays = new HashMap <Integer,AbsValue>();
	private HashSet <Integer> seen = new HashSet<Integer>();
	
	private BAAbstraction get(int id) {
		BAAbstraction result = repositoryByteArrays.get(id);
		if (result == null) {
			result = new BAAbstraction();
			repositoryByteArrays.put(id, result);
		}
		return result;
	}
	
	private PAG pag;
	/**
	 * Constructor of the analysis. 
	 * @param pag Points to analysis.
	 */
	public ArrayAnalysis(PointsToAnalysis pag ) {
		if (pag instanceof PAG)	this.pag = (PAG) pag;
	}

	/**
	 * Check if the new array is a byte array and extracts its content
	 * if possible.
	 * @param jb
	 * @param stmt a statement with shape <pre> x = new byte [n];</pre>
	 * @param locanalysis 
	 * @param iUnits 
	 */
	public void treatNewArray(JimpleBody jb, AssignStmt stmt, LocalAnalysis locanalysis, Iterator<Unit> iUnits, CallContext cc) {
		if (pag == null) return;
		try {
			Local l = (Local) stmt.getLeftOp();
			PointsToSet pts = pag.reachingObjects(l);
			AbsValue av = P2SAux.p2sContents(cc.nodeTable,pts);
			if (!(av instanceof OrValue)) return;
			List<AbsValue> lv = ((OrValue) av).vals;
			ArrayList <BAAbstraction>absList = new ArrayList<BAAbstraction>();
			for (AbsValue ava : lv) {
				if (! (ava instanceof NodeValue)) continue;
				int id = ((NodeValue) ava).ref;
				absList.add(get(id));
			}
			
			Value szValue = (((NewArrayExpr) (stmt.getRightOp())).getSize());
			int size = getConstantValue(locanalysis,stmt,szValue );
			if (size >= 0) {
				for(BAAbstraction abs: absList) abs.setSize(size); 
				Iterator<Unit> ui = jb.getUnits().iterator(stmt);
				ui.next(); 
				while(true) {
					Unit u = ui.next();
					if (! (u instanceof AssignStmt)) break;
					AssignStmt ast = (AssignStmt) u;
					Value v = ast.getLeftOp();
					if (! (v instanceof ArrayRef)) break;
					ArrayRef ar = (ArrayRef) v;
					if (!ar.getBase().equals(l)) break;
					Value indexValue = ar.getIndex();
					if (!(indexValue instanceof IntConstant)) break;
					int index = ((IntConstant) indexValue).value;
					Value right = ast.getRightOp();
					if (!(right instanceof IntConstant)) break;
					int value = ((IntConstant) right).value;
					if (index >= 0 && index < size) {
						for(BAAbstraction abs:absList)
							abs.set(index, (byte) value);
					}
					iUnits.next();
				}
			}
		} catch(Exception e) {e.printStackTrace(); }
	}

	/**
	 * Tries to extract a constant integer value from a Soot Value. It
	 * must be positive (array index or array size). If it fails, UNKNOWN_CONTENTS is
	 * given back.
	 * @param locanalysis Local def/use analysis of the body
	 * @param stmt Statement containing ref to value
	 * @param v the value itself
	 * @return a positive integer or -1
	 */
	private int getConstantValue(LocalAnalysis locanalysis, Stmt stmt, Value v) {
		if (v instanceof Local) {
			Local l = (Local) v;

			List <Unit> result = locanalysis.getDefsOfAt(l, stmt);
			if (result.size() != 1) return UNKNOWN_CONTENTS;
			Unit content = result.get(0);
			if (content instanceof AssignStmt) {
				AssignStmt ast = (AssignStmt) content;
				Value right = ast.getRightOp();
				if (right instanceof IntConstant) {
					return ((IntConstant) right).value;
				}
				if (right instanceof JCastExpr) {
					Value casted = ((JCastExpr) right).getOp();
					return getConstantValue(locanalysis, ast, casted);
				}
				if (right instanceof Local) {
					return getConstantValue(locanalysis, ast, right);
				}
				return UNKNOWN_CONTENTS;
			} else return UNKNOWN_CONTENTS;
		} else if (v instanceof IntConstant) {
			return ((IntConstant) v).value;
		} else return UNKNOWN_CONTENTS;
	}

	/**
	 * Gives back the abstracted contents.
	 * @param key
	 * @return
	 */
	public String getByteArray(int key, boolean xmlFormat) {
		BAAbstraction abstraction = repositoryByteArrays.get(key);
		return (abstraction!=null) ? abstraction.toString() : null;
	}

	/**
	 * Modify the value for an array assignment
	 * @param ast
	 * @param locanalysis
	 * @param cc
	 */
	public void treatByteArrayAssign(AssignStmt ast, LocalAnalysis locanalysis,
			CallContext cc) {
		if (pag == null) return;
		ArrayRef left = (ArrayRef) ast.getLeftOp();
		Local l = (Local) left.getBase();
		PointsToSet pts = pag.reachingObjects(l);
		int index = getConstantValue(locanalysis, ast, left.getIndex());
		int contents = getConstantValue(locanalysis, ast, ast.getRightOp());
		AbsValue av = P2SAux.p2sContents(cc.nodeTable,pts);
		if (!(av instanceof OrValue)) return;
		List<AbsValue> lv = ((OrValue) av).vals;
		for (AbsValue ava : lv) {
			if (! (ava instanceof NodeValue)) continue;
			int id = ((NodeValue) ava).ref;
			BAAbstraction abs = get(id);
			if (index == UNKNOWN_CONTENTS) abs.spoil();
			else {
				if (abs.contents == null || contents == UNKNOWN_CONTENTS) abs.spoil(index);
				else abs.set(index, (byte) contents);
			}
		}
		
	}
	
	/**
	 * @param cc
	 * @param pts
	 */
	public void registerStringArray(CallContext cc, AssignStmt ast) {
		if (pag == null) return;
		PointsToSet pts = pag.reachingObjects((Local) ast.getLeftOp());
		AbsValue av = P2SAux.p2sContents(cc.nodeTable,pts);
		if (!(av instanceof OrValue)) return;
		List<AbsValue> lv = ((OrValue) av).vals;
		PointsToSet stringPts = pag.reachingObjectsOfArrayElement(pts);
		Set<AbsValue> strings = P2SAux.possibleStringConstantsSet(stringPts);
		for (AbsValue ava : lv) {
			if (! (ava instanceof NodeValue)) continue;
			int id = ((NodeValue) ava).ref;
			this.repositoryStringArrays.put(id, strings.toArray(new AbsValue[0]));
		}
	}
	
	/**
	 * @param id
	 * @return
	 */
	public String getStringArray(int id) {
		AbsValue [] result = repositoryStringArrays.get(id);
		if(result == null) return null;
		return Arrays.toString(result);
	}

	/**
	 * Builds an abstraction for the contents of an array. It is called on registered arrays.
	 * @param nt
	 * @param node
	 */
	public void resolveArray(NodeTable nt, AllocNode node) {
		if (pag == null) return;
		int id = node.getNumber();
		// Even if type stratification was defeated, we will not loop on an array containing itself.
		if (seen.contains(id)) return;
		seen.add(id);
		P2SetFactory ptsf = pag.getSetFactory();
		PointsToSetInternal pts = ptsf.newSet(node.getType(), pag);
		pts.add(node);
		PointsToSet contents = pag.reachingObjectsOfArrayElement(pts);
		AbsValue av = P2SAux.p2sContents(nt, contents);
		repositoryOtherArrays.put(id, av);
	}
	
	/**
	 * Gets the abstraction of an array.
	 * @param id
	 * @return
	 */
	public AbsValue getArrayAbstraction(int id) {
		return repositoryOtherArrays.get(id);
	}
}
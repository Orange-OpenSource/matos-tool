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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import soot.SootClass;
import soot.Type;
import soot.jimple.ClassConstant;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.spark.ondemand.AllocAndContext;
import soot.jimple.spark.pag.AllocNode;

import com.orange.analysis.anasoot.arrayanalysis.ArrayAnalysis;
import com.orange.matos.core.Out;

/**
 * @author Pierre Cregut
 * Table of the abstractions of the different dynamic objects. A node
 * is a point of allocation (with a context if available). 
 */
public class NodeTable {
	
	static private Map <AllocAndContext, Integer> allocAndContextNaming = new HashMap <AllocAndContext, Integer>();

	static private int counter = 1;
	
	/**
	 * Table mapping Soot internal ids for nodes in the pointsto analysis graph 
	 * (used as unique identifiers) to representatives of these which are either
	 * program points or class (for new that is all we've got). 
	 * This is clearly suboptimal, but we do not want to reimplement Spark. 
	 */
	public Map<Integer,Object> nodeTable = new HashMap<Integer,Object>();
	
	/**
	 * Maps that link a node identifier to private identifiers identifying all the uses
	 * of this node as they have been printed. It is the way to build a cross-reference table
	 * of all the uses.
	 */
	public HashMap<Integer, HashMap <Integer, String>> nodeToUse = new HashMap<Integer, HashMap<Integer,String>>();
	
	/**
	 * Program point table.
	 */
	public Map<ProgramPoint,Set <Integer>> ppTable = new HashMap<ProgramPoint,Set <Integer>>();

	private ArrayAnalysis arrayAnalysis;

	private void registerType(int id, AllocNode  node) {
		if (! nodeTable.containsKey(id)) {
			Object rawexpr = node.getNewExpr();
			
			if (rawexpr instanceof ProgramPoint) {
				ProgramPoint pp = ((ProgramPoint) rawexpr).eraseType();
				Type t = node.getType();
				nodeTable.put(id,t);
				Set <Integer> cell = ppTable.get(pp);
				if (cell == null) {
					cell = new HashSet<Integer>();
					ppTable.put(pp, cell);
				}
				cell.add(id);
			} else if (rawexpr instanceof NewExpr) {
				NewExpr ne = (NewExpr) rawexpr;
				Type t = ne.getBaseType();
				nodeTable.put(id,t);
			} else if (rawexpr instanceof NewArrayExpr) {
				NewArrayExpr ne = (NewArrayExpr) rawexpr;
				Type t = ne.getType();
				arrayAnalysis.resolveArray(this, node);
				nodeTable.put(id,t);
			} else if (rawexpr instanceof ClassConstant) {
				ClassConstant classConstant = (ClassConstant) rawexpr;
				String cname = classConstant.value;
				nodeTable.put(id,"[" + cname + "]");
			} else if (rawexpr instanceof String) {
				String string = (String) rawexpr;
				nodeTable.put(id, "\"" + string + "\"");
			} else Out.getLog().println("UNKNOWN NODE " + rawexpr.getClass() + " " +node.getClass());
		}
	}
	
	/**
	 * @param ac
	 * @param r
	 */
	public void addWitness(AllocAndContext ac, ArrayList<AbsValue> r) {
		AllocNode an = ac.alloc;
		Integer id = allocAndContextNaming.get(ac);
		if (id == null) {
			id = counter ++;
			allocAndContextNaming.put(ac,id);
		}
		NodeValue repr = new NodeValue (id, this);
		registerType(id,an);
		r.add(repr);	
	}
	
	/**
	 * Get a node representation from the table
	 * @param ref
	 * @return Usually a type but can be a string for class constants and strings.
	 */
	public Object get(int ref) { return nodeTable.get(ref); }

	/**
	 * Initialisation with the abstraction of an unknown. 
	 * @param baa 
	 */
	public void init(ArrayAnalysis baa) {
		nodeTable.put(-1,new SootClass("*"));
		arrayAnalysis = baa;
	}
	
	/**
	 * @param node
	 * @return
	 */
	public int add(AllocNode node) {
		int id = node.getNumber();
		registerType(id, node);
		return id;
	}

	/**
	 * Gives back a printable form for a given byte array
	 * @param key the identifier of the abstract object.
	 * @param b xml format or not
	 * @return
	 */
	public String getByteContent(int key, boolean b) {
		return arrayAnalysis.getByteArray(key, b);
	}

	/**
	 * Gives back a printable form for a given string array
	 * @param key the identifier of the abstract object.
	 * @param b xml format or not
	 * @return
	 */
	public String getStringContent(int ref, boolean b) {
		return arrayAnalysis.getStringArray(ref);
	}

	/**
	 * Gives back a printable form for any kind of array
	 * @param ref
	 * @param b
	 * @return
	 */
	public String getArrayContent(int ref, boolean b) {
		AbsValue v = arrayAnalysis.getArrayAbstraction(ref);
		return (v==null) ? null : v.toString();
	}
}

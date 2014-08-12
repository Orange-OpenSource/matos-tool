package com.orange.analysis.anasoot.loop;

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
import java.util.Collection;
import java.util.Iterator;

import soot.tagkit.Host;
import soot.tagkit.Tag;

/**
 * @author Pierre Cregut
 * Implementation of tarjan algorithm for computing strongly connected
 * components of a graph. We use taggable elements for the graph so that
 * we can annotate them with the result.
 * @param <E> the class of elements of the graph.
 */
public class Tarjan <E extends Host>{

	/** The Graph interface describes a directed graph. We only implement need
        to know the forward neighbours of the node. Each node is implemented
	as an object but it should also implement the Host interface so that
        it can be tagged. Finally the size element should give back the number
        of nodes of the graph. */
	public static interface Graph <E extends Host> {
		/** Gives an iterator over the neigbours of an object of the graph */
		public Collection <E> neighbours(E obj);
		/** Number of nodes of the graph */
		public int size();
	}

	static class IdTag implements Tag {
		public static final String name = "IdTag";
		private byte [] key;
		private int v;

		IdTag(int v) {
			this.v = v;
			key = new byte [4];
			key[0] = (byte) (v >> 24 & 0xff);
			key[1] = (byte) (v >> 16 & 0xff);
			key[2] = (byte) (v >> 8 & 0xff);
			key[3] = (byte) (v & 0xff);
		}

		@Override
		public String getName() { return name; }
		@Override
		public byte [] getValue() { return key; }
		public static int getValue(IdTag t) { return t.v; }
	}

	int low[];
	int stack[];
	int c_stack = 0;
	int n = 0;
	boolean finished[];
	boolean marked[];

	Graph <E> g;

	//private void debug (String s) { /* System.err.println(s); */ }

	/**
	 * Constructor 
	 * @param g the graph for which we seek strongly connected components
	 */
	public Tarjan(Graph <E> g) {
		this.g = g;
		int l = g.size();
		low = new int [l];
		stack = new int [l];
		marked = new boolean [l];
		finished = new boolean [l];
		
		for(int i=0; i < l; i++) {
			stack[i] = low[i] = 0;
			marked[i] = finished[i] = false;
		}
	}	

	/** Complete analysis */
	public void doAnalysis (Iterator <E> itp) {
		while(itp.hasNext()) {
			E pnode = itp.next();
			if (!pnode.hasTag(IdTag.name)) visit(pnode);
		}
	}

	/** On demand analysis */
	public void doAnalysis (E pnode) {
		if (!pnode.hasTag(IdTag.name)) visit(pnode);
	}

	private int visit(E pnode) {
		int p = n++;
		pnode.addTag(new IdTag(p));
		stack[c_stack++] = p;
		low[p] = p;
		for(E qnode : g.neighbours(pnode)) {
			// Algorithm not adapted for loops on oneself
			if (qnode.hasTag(IdTag.name)) {
				int q = IdTag.getValue ((IdTag)(qnode.getTag(IdTag.name)));
				if (p==q) {
					marked[p] = true;
				}
				if (finished[q]) continue;
				// This mean we are in a loop and not the head
				if (q<low[p]) low[p]=q;
			} else {
				int q = visit(qnode);
				// This mean we are in a loop and not the head
				if (low[q]<low[p]) low[p]=low[q];
			}
		}
		if (low[p] == p) {
			// Then we are the head. Pop out the stack
			if (p == stack[c_stack - 1]) { 
				// component with a single node
				finished[p]=true;
				c_stack--;
			} else {
				int v;
				do {
					v = stack[--c_stack];
					finished[v]=true;
					marked[v] = true;
					low[v] = p; // WARNING IS THIS SAFE ?
				} while (v != p);
			}
		} // else we are not and we are left on the stack.
		return p;
	}

	/**
	 * Is the graph element in a loop ?
	 * @param h
	 * @return
	 */
	public boolean inLoop(E h) {
		int n = IdTag.getValue ((IdTag)(h.getTag(IdTag.name)));
		return marked[n];
	}
	
	/**
	 * The canonical representative of the SCC of the element
	 * @param h
	 * @return
	 */
	public int component(E h) {
		int n = IdTag.getValue ((IdTag)(h.getTag(IdTag.name)));
		return low[n];
	}
	/**
	 * Gets the associated identifier.
	 * @param h
	 * @return
	 */
	public int identifier(E h) {
		return IdTag.getValue ((IdTag)(h.getTag(IdTag.name)));
	}
	
	
}

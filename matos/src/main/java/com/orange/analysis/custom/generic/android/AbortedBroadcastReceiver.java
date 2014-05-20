package com.orange.analysis.custom.generic.android;

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import soot.Local;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Value;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import com.orange.analysis.anasoot.spy.CustomSemanticRule;
import com.orange.analysis.anasoot.spy.SpyResult;
import com.orange.matos.core.AppDescription;
import com.orange.matos.core.Out;

/**
 * @author piac6784
 * Custom rule to study which broadcast receivers use the Abort capability and if this cause a security problem.
 *
 */
public class AbortedBroadcastReceiver implements CustomSemanticRule {
    private static final String ANDROID_CONTENT_INTENT_FILTER = "android.content.IntentFilter";
    private static final String ANDROID_CONTENT_BROADCASTRECEIVER = "android.content.BroadcastReceiver";
    private static final String ANDROID_CONTENT_CONTEXT_WRAPPER = "android.content.ContextWrapper";
    private static final String REGISTER_1_SIGNATURE = "android.content.Intent registerReceiver(android.content.BroadcastReceiver,android.content.IntentFilter)";
    private static final String REGISTER_2_SIGNATURE = "android.content.Intent registerReceiver(android.content.BroadcastReceiver,android.content.IntentFilter,java.lang.String,android.os.Handler)";
    private static final String [] REGISTER_SIGNATURES = {
        REGISTER_1_SIGNATURE, REGISTER_2_SIGNATURE
    };
    private static final String BROADCAST_ABORT_SIGNATURE = "void abortBroadcast()";
    private static final String ACTION_FIELD_SIGNATURE = "java.lang.String action";

    private static final String [] ACTION_STRINGS = { 
        "android.provider.Telephony.SMS_RECEIVED",
        "android.intent.action.NEW_OUTGOING_CALL"  
    };
    private static final String [] MARKER_STRINGS = { "sms","call" };
    private static final String MARKER_PREFIX = "android.aborted.broadcast.";
        

    final private Scene scene;
    final private PointsToAnalysis pag;
    final private CallGraph cg;
    
    
    /**
     * Instantiates a new aborted broadcast receiver.
     */
    public AbortedBroadcastReceiver() {
        scene = Scene.v();
        pag = scene.getPointsToAnalysis();
        cg = scene.getCallGraph(); 
    }
    
    @Override
    public void run(SpyResult result, AppDescription app) {
        Set <String> aborted = abortedBroadcastsTypes();
        System.out.println("ABORTED BROADCAST : " + aborted);
        if (aborted.size() == 0) return;
        for(int i=0; i <  ACTION_STRINGS.length; i++) {
            String action = ACTION_STRINGS[i];
            String marker = MARKER_STRINGS[i];
            Set <String> filtered = dynamicallyFilteredBroadcastTypes(action);
            filtered.retainAll(aborted);
            if(filtered.size() > 0) {
                String warnString = MARKER_PREFIX + marker;
                result.setCustomResult(warnString,true);

                int j = 0;
                String path = warnString + ".path";
                for(String typName : filtered) {
                    result.setCustomResult(listItem(path,j++),typName);
                }
                result.setCustomResult(warnString + ".count",i);                
            }
        }
    }

    
    /**
     * Gives the type of all the broadcast receiver that may be aborted. It looks at all the calls to abortBroadcast and through the Points-to analysis at the
     * actual class of the base arguments of the calls.
     * @return a set of soot type that may be empty.
     */
    private Set<String> abortedBroadcastsTypes() {
        Set <String> result = new HashSet<String>();
        try {
            SootClass receiverClass = scene.getSootClass(ANDROID_CONTENT_BROADCASTRECEIVER);
            SootMethod abortMethod = receiverClass.getMethod(BROADCAST_ABORT_SIGNATURE);
            Iterator<Edge> edges = cg.edgesInto(abortMethod);
            while(edges.hasNext()) {
                Edge edge = edges.next();
                InvokeExpr ie = edge.srcStmt().getInvokeExpr();
                if (ie == null || ! (ie instanceof InstanceInvokeExpr)) continue;
                Value base = ((InstanceInvokeExpr) ie).getBase();
                if (!(base instanceof Local)) continue;
                PointsToSet basePTS = pag.reachingObjects((Local) base);
                addAll(result,basePTS.possibleTypes());
            }
        } catch (RuntimeException e) {
            Out.getLog().println("Error while computing aborted broadcast " + e.getMessage());         
        }
        return result;
    }
    
    /**
     * Gives back the set of types of broadcast receivers for dynamic filters that match a set of given actions.
     * @param actions set of actions captured by receivers of interest.
     * @return
     */
    private Set <String> dynamicallyFilteredBroadcastTypes(String action) {
        Set <String> result = new HashSet<String>();
        SootClass contextClass;
        SootClass intentFilterClass;
        try {
            contextClass = scene.getSootClass(ANDROID_CONTENT_CONTEXT_WRAPPER);
            intentFilterClass = scene.getSootClass(ANDROID_CONTENT_INTENT_FILTER);
            SootField actionField = intentFilterClass.getField(ACTION_FIELD_SIGNATURE);
            for(String registerSig : REGISTER_SIGNATURES) {
                SootMethod regMethod = contextClass.getMethod(registerSig);
                Iterator<Edge> edges = cg.edgesInto(regMethod);
                while(edges.hasNext()) {
                    Edge edge = edges.next();
                    InvokeExpr ie = edge.srcStmt().getInvokeExpr();
                    if (ie.getArgCount() < 2) continue;
                    Value filter = ie.getArg(1);
                    if (! (filter instanceof Local)) continue;
                    PointsToSet pts = pag.reachingObjects((Local) filter, actionField);
                    Set <String> filteredActions = pts.possibleStringConstants();
                    if (filteredActions == null || ! filteredActions.contains(action)) continue;
                    Value receivers = ie.getArg(0);
                    if (! (receivers instanceof Local)) continue;
                    PointsToSet ptsReceiver = pag.reachingObjects((Local) receivers);
                    addAll(result,ptsReceiver.possibleTypes());
                }
            }
        } catch (RuntimeException e) {
            Out.getLog().println("Error while computing dynamic aborted broadcast " + e.getMessage());  
        }
        return result;
    }

    private static void addAll(Collection<String> bag, Collection<Type> typs) {
        for(Type typ: typs) {
            if(!(typ instanceof RefType)) continue;
            bag.add(((RefType) typ).getClassName());
        }
    }
    
    private static String listItem(String key, int id) { return key + "." + id; }
    

/*    
    private static <A> boolean containsAny(final Collection<A> coll1, final Collection<A> coll2) {
        if (coll1.size() < coll2.size()) {
            for (Iterator<A> it = coll1.iterator(); it.hasNext();) {
                if (coll2.contains(it.next())) {
                    return true;
                }
            }
        } else {
            for (Iterator<A> it = coll2.iterator(); it.hasNext();) {
                if (coll1.contains(it.next())) {
                    return true;
                }
            }
        }
        return false;
    }
    */
}

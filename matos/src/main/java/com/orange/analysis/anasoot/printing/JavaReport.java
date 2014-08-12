/*
 * $Id: Report.java 614 2006-04-27 16:47:19 +0200 (jeu., 27 avr. 2006) penaulau $
 */
package com.orange.analysis.anasoot.printing;

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

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;

import org.w3c.dom.Element;

import com.orange.analysis.anasoot.printing.JavaRuleReport.Filter;
import com.orange.analysis.anasoot.result.JavaResult;
import com.orange.matos.core.Alert;
import com.orange.matos.core.XMLParser;
import com.orange.matos.utils.HtmlOutput;

/**
 * Printing the result of analysing a method.
 * @author Pierre Cregut
 *
 */

public abstract class JavaReport extends Report{

    static class IntegerCell {
        int value;
        public IntegerCell(int c) { value = c; }
        public void incr() { value++; }
        @Override
        public String toString() { return String.valueOf(value); }
    }

    final protected static HashMap<Character,IntegerCell> globalCounterTable = 
            new HashMap<Character, IntegerCell>();

    /**
     * Reset the counters to reuse it.
     */
    public static void resetAll() {
        globalCounterTable.clear();
    }

    /**
     * Reset code specific to a given report.
     */
    public void reset() {}

    /**
     * Replaces strings like "%..." in the result message 
     * @param msg the input message
     * @param r the value of spied argument as a raw string.
     * @param jresult the result to print. 
     * @param re result of a match against a regular expression
     * @throws Alert
     */
    static void print (PrintStream out, String msg, String r, JavaResult jresult, Matcher re) throws Alert {
        if (msg.equals("-") || msg.equals("")) return;
        int i = 0,j;
        while((j = msg.indexOf('%',i)) >= 0) {
            if (j > i) out.print(msg.substring(i,j));
            switch (msg.charAt(j+1)) {
                case 'r':
                    if (r!=null) out.print(r); 
                    break;
                case 'x':
                    jresult.argument.text(out);
                    break;
                case 'c':
                    out.print(HtmlOutput.escape(jresult.method_orig.getDeclaringClass().getName()));
                    break;
                case 'o':
                    out.print(jresult.offset_orig);
                    break;
                case 'm':
                    out.print(HtmlOutput.escape(jresult.method_orig.getName()));
                    break;
                case 'n':
                    char code = msg.charAt(j+2);
                    j++;
                    IntegerCell count = globalCounterTable.get(code);
                    if (count == null) {
                        count = new IntegerCell(1);
                        globalCounterTable.put(code, count);
                    }
                    out.print(count);
                    count.incr();
                    break;
                case 's':
                    out.print(HtmlOutput.escape(jresult.method_orig.getSignature()));
                    break;
                case 'C':
                    out.print(HtmlOutput.escape(jresult.method.getDeclaringClass().getName()));
                    break;
                case 'M':
                    out.print(HtmlOutput.escape(jresult.method.getName()));	
                    break;
                case 'S':
                    out.print(HtmlOutput.escape(jresult.method.getSignature())); break;
                case '1': case '2': case '3': case '4': case '5':
                case '6': case '7': case '8': case '9':
                    if (re != null)
                        out.print(HtmlOutput.escape(re.group(msg.charAt(j+1) - '0')));
                    break;
                case '%':
                    out.print('%'); break;
                default:
                    throw Alert.raised(null,"Illegal escaped character in printing rule: " + msg);
            }
            i = j+2;
        }
        out.print(msg.substring(i));
    }

    private static JavaReport parseConjunction(String name, XMLParser parser, Element impl, GlobalReport global) throws IOException {
        Element refDefs [] = XMLParser.getElements(impl,"ref");
        ArrayList<JavaReport> reportList = new ArrayList<JavaReport>();
        ArrayList<Integer> positionList = new ArrayList<Integer>();
        for(int i=0; i < refDefs.length; i++) {
            String ref = refDefs[i].getAttribute("name");
            String posDef = refDefs[i].getAttribute("pos");
            JavaReport reportaux = global.get(ref);
            if (reportaux == null) {
                throw new IOException("unknown report reference " + ref +
                        " in report " +  name + ".");
            }
            int pos = (posDef == null || posDef.equals("")) ? -1 : Integer.parseInt(posDef);
            reportList.add(reportaux);
            positionList.add(pos);
        }
        return new ReportAnd(name,reportList,positionList);

    }

    private static JavaReport parsePseudoConstantRule(String name, XMLParser parser, Element impl) {
        Element passDef = parser.getElement(impl,"pass");
        Element failDef = parser.getElement(impl,"fail");
        if (passDef != null && failDef != null) {
            return new  ReportPseudo(name, parser.contents(passDef),parser.contents(failDef));
        } else return null;

    }

    private static JavaReport parsePseudoStringRule(String name, XMLParser parser, Element impl, String profile) {
        ArrayList<Filter> filters = new ArrayList<Filter> ();
        Element filterDefs []= XMLParser.getElements(impl,"filter");
        for(int i=0; i < filterDefs.length; i++) {
            Element filter=filterDefs[i];
            String pattern=filter.getAttribute("pattern");
            if (pattern == null || pattern.equals("")) continue;

            String profileInReport = filter.getAttribute("profile");
            if (profileInReport.equals("") || profileInReport.equals(profile)){
                String filterName = filter.getAttribute("name");
                if (filterName == null || filterName.equals("")) {
                    filterName = "[" + i + "]";
                }
                String action = parser.contents(filter);
                String verdict = filter.getAttribute("verdict");
                Filter filterDef = new Filter(filterName, pattern, verdict, action);
                filters.add(filterDef);
            }


        }
        Element deflt = parser.getElement(impl,"default");
        String isGlb = impl.getAttribute("global");
        boolean isGlobal = (isGlb != null) && isGlb.equals("true");
        String defaultval = (deflt==null)?"-":parser.contents(deflt);
        String defaultVerdict = (deflt==null)?"-":deflt.getAttribute("verdict");
        return new JavaRuleReport(name,filters,defaultval, defaultVerdict,isGlobal);
    }


    /**
     * Defines a new report according to the XML definition in reportDef. This methods parses all kind of
     * report.
     * @param reportDef
     * @param global
     * @param parser
     * @param httpAuthorized
     * @param profile
     * @throws IOException
     */
    public static void parse(Element reportDef, GlobalReport global, XMLParser parser, String profile) throws IOException {
        String name = reportDef.getAttribute("name");
        if(global.contains(name)) {
            return;
        }
        JavaReport report = null;
        Element impl;
        if ((impl=parser.getElement(reportDef,"pseudoString")) != null) {
            report = parsePseudoStringRule(name, parser, impl, profile);
        } else 	if ((impl=parser.getElement(reportDef,"pseudoConstant")) != null) {
            report = parsePseudoConstantRule(name, parser, impl);
        } else 	if ((impl=parser.getElement(reportDef,"conjunction")) != null) {
            report = parseConjunction(name, parser, impl, global);
        } else 	if ((impl=parser.getElement(reportDef,"forbiddenMethod")) != null) {
            String message = parser.contents(impl);
            String silentAttr = impl.getAttribute("silent");
            boolean silent = silentAttr != null && silentAttr.equals("true");
            report = new ReportUse(name,message, silent);
        } else if ((impl=parser.getElement(reportDef,"message")) != null) {
            String last = impl.getAttribute("unique");
            String message = parser.contents(impl);
            report = new ReportMessage(name, message, last);
        } else {
            throw new IOException("unknown report kind in report " + 
                    name + ".");
        }
        if (report !=null){
            global.put(name, report);
        }
        String aliases = reportDef.getAttribute("alias");
        if (aliases != null) {
            for(String alias : aliases.split(",")) {
                if (alias.length() > 0) global.put(alias,report);
            }
        }

    }
    @Override
    public void tell(PrintStream outStream, boolean xmlFormat, JavaResult result, int position) throws Alert {};
    /**
     * Should print everything. Can be overriden.
     * @param result
     * @throws Alert
     */
    public void tellAll (PrintStream result) throws Alert {};

}

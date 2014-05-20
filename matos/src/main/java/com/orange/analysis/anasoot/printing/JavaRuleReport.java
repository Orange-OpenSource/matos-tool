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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.orange.analysis.anasoot.result.AbsValue;
import com.orange.analysis.anasoot.result.AndValue;
import com.orange.analysis.anasoot.result.ConcatValue;
import com.orange.analysis.anasoot.result.JavaResult;
import com.orange.analysis.anasoot.result.OrValue;
import com.orange.analysis.anasoot.result.StringValue;
import com.orange.matos.core.Alert;
import com.orange.matos.core.XMLStream;
import com.orange.matos.utils.HtmlOutput;

/**
 * Represents a rule for creating a report after the analysis of a midlet. 
 * @author piac6784
 *
 */

public class JavaRuleReport extends JavaReport {

    /**
     * The regular expressions used by the filter.
     */
    private final Filter filters[];

    private final String default_msg;
    /**
     * True if the default case is a success verdict.
     */
    private final boolean default_success;

    /**
     * True if the filters must be run on the value globally and not as prefix
     */
    private final boolean isGlobal;

    private boolean caught;
    private boolean seen;


    /**
     * The different cases that makes a report.
     * @author piac6784
     *
     */
    public static class Filter {
        /**
         * The pattern to match on 
         */
        final public Pattern pattern;
        /**
         * The name of the case
         */
        final public String name;
        /**
         * True if when that branch is taken, the result is considered as safe.
         */
        final public boolean verdict;
        /**
         * What should be printed.
         */
        final public String action;
        /**
         * @param name The name of the filter
         * @param pattern The regular expression defining that must be matched
         * @param verdict The verdict (between PASSED|FAILED|WARNING)
         * @param action The output text for the HTML version.
         */
        public Filter(String name, String pattern, String verdict, String action) {
            this.name = name;
            try {
                this.pattern = Pattern.compile(pattern);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Pattern in rule " + name + ": " + e.getMessage());
            }
            this.verdict = (verdict.equals("PASSED"));
            this.action = action;
        }

    }

    /**
     * Constructor for a report rule corresponding to a case analysis on the result.
     * @param name The name of the rule
     * @param filters Its different case
     * @param dft the message printed when no case match.
     * @param defaultVerdict The verdict when we fail through).
     */
    public JavaRuleReport(String name, ArrayList <Filter> filters, String dft, String defaultVerdict, boolean isGlobal) {
        this.name = name;
        reset();
        this.filters = filters.toArray(new Filter [0]);
        default_msg= dft;
        default_success = defaultVerdict.equals("PASSED");
        this.isGlobal = isGlobal;
    }

    @Override
    public void reset() {
        seen = false;
        caught = false;
    }


    @Override
    public void tell(PrintStream outStream, boolean xmlFormat, JavaResult result, int position) throws Alert {
        JavaResult javaResult = (JavaResult)result;
        AbsValue argument = javaResult.argument;
        if (argument instanceof AndValue) {
            AndValue argAnd = (AndValue) argument;
            if (position != -1 && position < argAnd.size()) {
                argument = argAnd.get(position);
            } else {
                throw Alert.raised(null,"Can only handle simple reports with right position " + name + " " + position);
            }
        }
        if (isGlobal) {
            argument = argument.normalize(false);
            String val = argument.toString();
            loop: for(Filter filter: filters) {
                Matcher ma = filter.pattern.matcher(val);
                if (ma.matches()) {
                    caught = true;
                    if (xmlFormat) {
                        String idref = javaResult.getRef();
                        XMLStream xmlout = new XMLStream(outStream);
                        xmlout.element("report");
                        xmlout.attribute("name", name);
                        xmlout.attribute("ref", idref);
                        xmlout.attribute("choice", filter.name);
                        for(int j=0; j <= ma.groupCount(); j++) {
                            xmlout.element("group");
                            xmlout.attribute("pos", j);
                            xmlout.attribute("value", ma.group(j));
                            xmlout.endElement();
                        }
                        xmlout.close();
                    } else {
                        print (outStream, filter.action, HtmlOutput.escape(val) , javaResult, ma);
                    }
                    hasOut = true;
                    finalVerdict &= filter.verdict;
                    break loop;
                }
            }            
        } else {
            argument = argument.normalize(true);
            OrValue alternatives;
            alternatives = new OrValue();
            alternatives.add(AbsValue.prefixNormalize(argument));
            Set <String> seen = new HashSet<String>();
            for(AbsValue v : alternatives.vals) {
                String prefix;
                if (v instanceof StringValue) prefix = ((StringValue) v).value;
                else if (v instanceof ConcatValue) {
                    ArrayList <AbsValue> contents = ((ConcatValue) v).contents;
                    if ((contents.size() > 1) && (contents.get(0) instanceof StringValue)) {
                        prefix = ((StringValue) contents.get(0)).value;
                    } else prefix = "";
                } else prefix = "";
                if (seen.contains(prefix)) continue;
                seen.add(prefix);

                loop: for(Filter filter: filters) {
                    Pattern re = filter.pattern;
                    Matcher ma;
                    ma = re.matcher(prefix);
                    if (ma.matches()) {
                        caught = true;
                        if (xmlFormat) {
                            String idref = javaResult.getRef();
                            XMLStream xmlout = new XMLStream(outStream);
                            xmlout.element("report");
                            xmlout.attribute("name", name);
                            xmlout.attribute("ref", idref);
                            xmlout.attribute("choice", filter.name);
                            for(int j=0; j <= ma.groupCount(); j++) {
                                xmlout.element("group");
                                xmlout.attribute("pos", j);
                                xmlout.attribute("value", ma.group(j));
                                xmlout.endElement();
                            }
                            xmlout.close();
                        } else {
                            print (outStream, filter.action, HtmlOutput.escape(prefix) , javaResult, ma);
                        }
                        hasOut = true;
                        finalVerdict &= filter.verdict;
                        break loop;
                    }

                }
            }
        }
    }

    /**
     * Prints the result at the end of use.
     * @param out
     * @param xmlFormat
     */
    public void tellAll(PrintStream out, boolean xmlFormat) {
        if (seen && !caught && !default_msg.equals("-")){
            if (!xmlFormat) out.println("<p>" + default_msg + "</p>");
            hasOut = true;
            finalVerdict = finalVerdict && default_success;
        }
    }

}

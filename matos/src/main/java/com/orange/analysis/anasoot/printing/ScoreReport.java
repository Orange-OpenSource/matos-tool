
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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.orange.analysis.anasoot.result.AbsValue;
import com.orange.analysis.anasoot.result.StringValue;
import com.orange.analysis.anasoot.result.ValueVisitor;
import com.orange.matos.core.XMLParser;
import com.orange.matos.core.XMLStream;
import com.orange.matos.utils.HtmlOutput;

/**
 * This class implements a "WAC style" score mechanism that tries to evaluate
 * the risk associated to an application based on simple measures on the report.
 * 
 * @author piac6784
 */
public class ScoreReport {

    private static final String RULE_SCORE_ELT = "rulematch";

    private static final String PATTERN_SCORE_ELT = "stringmatch";

    private static final String CONJUNCTION_SCORE_ELT = "all-of";

    private static final String DISJUNCTION_SCORE_ELT = "any-of";

    private static final String CATCHER_ELT = "catcher";

    private static final String PERMISSION_ELT = "permission";

    private static final String NAME_ATTR = "name";

    private static final String MESG_ATTR = "message";

    private static final String SCORE_ATTR = "score";

    private static final String PATTERN_ATTR = "pattern";

    private static final String RULE_ELT = "rule";

    private static final String ELEMENT_ELT = "element";

    private static final String PERMISSION_ATTR = "id";

    /**
     * All the score elements in the order of their definition.
     */
    ArrayList<ScoreElement> elements = new ArrayList<ScoreElement>();

    /**
     * The score elements restricted to the patterns.
     */
    ArrayList<StringPattern> patterns = new ArrayList<StringPattern>();

    /**
     * The catcher patterns
     */
    ArrayList<StringCatcher> catchers = new ArrayList<StringCatcher>();

    /**
     * The score elements restricted to permissions
     */
    ArrayList<PermissionScore> permissions = new ArrayList<PermissionScore>();

    /**
     * A lookup table to find the score elements corresponding to a given rule.
     * In most cases at most one element will be triggered.
     */
    HashMap<String, ArrayList<UseRule>> rules = new HashMap<String, ArrayList<UseRule>>();

    /**
     * Lookup table to find the elements by name.
     */
    HashMap<String, ScoreElement> elementDictionary = new HashMap<String, ScoreElement>();

    /**
     * The value at which the score is said to be above the acceptable limit.
     */
    int threshold;

    /**
     * Global score. It is negative until it has been computed.
     */
    int score = -1;

    /**
     * A score element describes an elementary contribution to the score. The
     * higher the score is, the worse is the result.
     * 
     * @author piac6784
     */
    public static class ScoreElement {
        final String name;

        final String message;

        boolean isUsed = false;

        final int score;

        /**
         * All score elements are described by their impact (the score) and a
         * message to be printed.
         * 
         * @param message
         * @param score
         */
        public ScoreElement(String name, String message, int score) {
            this.name = name;
            this.message = message;
            this.score = score;
        }

        /**
         * Mark this score element as trigered.
         */
        public void use() {
            isUsed = true;
        }

        /**
         * Accessor to the status of the score element
         * 
         * @return true if it is used.
         */
        public boolean isUsed() {
            return isUsed;
        }

    }

    /**
     * This ScoreElement triggers when any of the rules of a given set has been
     * triggered
     * 
     * @author piac6784
     */
    public static class UseRule extends ScoreElement {
        final String[] rules;

        /**
         * Constructor
         * 
         * @param rules the set of rules that can trigger
         * @param message what is printed when the pattern is encountered
         * @param score the effect on the score
         */
        public UseRule(String[] rules, String name, String message, int score) {
            super(name, message, score);
            this.rules = rules;
        }
    }

    /**
     * This ScoreElement triggers when a permission is used. Exact
     * implementation is left to the profile.
     * 
     * @author piac6784
     */
    public static class PermissionScore extends ScoreElement {
        private final String permission;

        /**
         * Constructor
         * 
         * @param permission the permission name that triggers
         * @param message what is printed when the pattern is encountered
         * @param score the effect on the score
         */
        public PermissionScore(String permission, String name, String message, int score) {
            super(name, message, score);
            this.permission = permission;
        }

        /**
         * Access to the pemission id
         * 
         * @return
         */
        public String getPermission() {
            return permission;
        }
    }

    /**
     * This score is trigered if any of the referenced score has been triggered.
     * Warning : if there is a loop, in references, the isUsed method will loop.
     * 
     * @author piac6784
     */
    public class DisjunctionElement extends ScoreElement {
        final String[] elements;

        /**
         * @param elements
         * @param name
         * @param message
         * @param score
         */
        public DisjunctionElement(String[] elements, String name, String message, int score) {
            super(name, message, score);
            this.elements = elements;
        }

        @Override
        public boolean isUsed() {
            for (String name : elements) {
                ScoreElement elt = elementDictionary.get(name);
                if (elt != null && elt.isUsed())
                    return true;
            }
            return false;
        }
    }

    /**
     * This score is trigered if all of the referenced score have been
     * triggered.
     * 
     * @author piac6784
     */
    public class ConjunctionElement extends ScoreElement {
        final String[] elements;

        /**
         * @param elements
         * @param name
         * @param message
         * @param score
         */
        public ConjunctionElement(String[] elements, String name, String message, int score) {
            super(name, message, score);
            this.elements = elements;
        }

        @Override
        public boolean isUsed() {
            for (String name : elements) {
                ScoreElement elt = elementDictionary.get(name);
                if (elt != null && !elt.isUsed())
                    return false;
            }
            return true;
        }
    }

    /**
     * This ScoreElement triggers when a given pattern is found in the results.
     * 
     * @author piac6784
     */
    public static class StringPattern extends ScoreElement implements ValueVisitor {
        final Matcher matcher;

        final String pattern;

        /**
         * Constructor
         * 
         * @param pattern the pattern to recognize in the strings
         * @param message what is printed when the pattern is encountered
         * @param score the effect on the score
         */
        public StringPattern(String pattern, String name, String message, int score) {
            super(name, message, score);
            this.pattern = pattern;
            this.matcher = Pattern.compile(pattern).matcher("");
        }

        /**
         * Elementary check on a single string.
         * 
         * @param s
         */
        public void match(String s) {
            matcher.reset(s);
            if (matcher.find()) {
                use();
            }
        }

        /**
         * Check if the pattern appears in a result presented as an Abstract
         * Value.
         * 
         * @param v
         */
        public void match(AbsValue v) {
            v.explore(this);
        }

        @Override
        public void visit(AbsValue v) {
            if (v instanceof StringValue)
                match(((StringValue) v).value);
        }
    }

    /**
     * This class represents a filter to catch specific elements in a table.
     * 
     * @author piac6784
     */
    public static class StringCatcher {
        private final static Pattern sanitizerPattern = Pattern.compile("<[^>]*(>|$)");

        final Matcher matcher;

        final String pattern;

        final String name;

        private HashSet<String> table = new HashSet<String>();

        /**
         * Constructor
         * 
         * @param pattern the pattern to recognize in the strings
         * @param message what is printed when the pattern is encountered
         */
        public StringCatcher(String pattern, String name) {
            this.pattern = pattern;
            this.name = name;
            this.matcher = Pattern.compile(pattern).matcher("");
        }

        /**
         * Elementary check on a single string.
         * 
         * @param s
         */
        public void match(String s) {
            matcher.reset(s);
            while (matcher.find()) {
                String raw = matcher.group();
                // Get rid of all spurious html tagging caught.
                String sanitized = sanitizerPattern.matcher(raw).replaceAll("");
                table.add(sanitized);
            }
        }

    }

    /**
     * Overload a PrintStream so that it can check if the patterns stored in the
     * score are encountered in the output printed.
     * 
     * @author piac6784
     */
    public static class ScorePrintStream extends PrintStream {
        private PrintStream ps;

        ScoreReport score;

        /**
         * Constructor that wraps an existing printstream.
         * 
         * @param ps the wrapped printstream
         * @param score the ScoreReport containing the patterns to match
         *            against.
         * @throws UnsupportedEncodingException
         */
        public ScorePrintStream(PrintStream ps, ScoreReport score)
                throws UnsupportedEncodingException {
            super(ps, false, "UTF-8");
            this.ps = ps;
            this.score = score;
        }

        @Override
        public boolean checkError() {
            return ps.checkError();
        }

        @Override
        public void close() {
            ps.close();
        }

        @Override
        public void flush() {
            ps.flush();
        }

        @Override
        public void print(boolean b) {
            ps.print(b);
        }

        @Override
        public void print(char c) {
            ps.print(c);
        }

        @Override
        public void print(char[] s) {
            ps.print(s);
        }

        @Override
        public void print(double d) {
            ps.print(d);
        }

        @Override
        public void print(float f) {
            ps.print(f);
        }

        @Override
        public void print(int i) {
            ps.print(i);
        }

        @Override
        public void print(long l) {
            ps.print(l);
        }

        @Override
        public void print(Object obj) {
            ps.print(obj);
        }

        @Override
        public void print(String s) {
            score.matchString(s);
            ps.print(s);
        }

        @Override
        public void println() {
            ps.println();
        }

        @Override
        public void println(boolean x) {
            ps.println(x);
        }

        @Override
        public void println(char x) {
            ps.println(x);
        }

        @Override
        public void println(char[] x) {
            ps.println(x);
        }

        @Override
        public void println(double x) {
            ps.println(x);
        }

        @Override
        public void println(float x) {
            ps.println(x);
        }

        @Override
        public void println(int x) {
            ps.println(x);
        }

        @Override
        public void println(long x) {
            ps.println(x);
        }

        @Override
        public void println(Object x) {
            ps.println(x);
        }

        @Override
        public void println(String x) {
            score.matchString(x);
            ps.println(x);
        }

        @Override
        public void write(byte[] buf, int off, int len) {
            try {
            	String str = new String(buf, off, len, "UTF-8");
                score.matchString(str);
            } catch (Exception e) {
                throw new RuntimeException("UTF-8 not supported", e); // Cannot happen
            }
            ps.write(buf, off, len);

        }

        @Override
        public void write(byte[] buf) throws IOException {
            score.matchString(new String(buf, "UTF-8"));
            ps.write(buf);
        }

        @Override
        public void write(int b) {
            ps.write(b);
        }
    }

    /**
     * Constructor of the score from the XML element in the rule file.
     * 
     * @param elt
     */
    public ScoreReport(Element elt) {
        threshold = parseIntAttribute(elt, "threshold");
        NodeList childNodes = elt.getChildNodes();
        int l = childNodes.getLength();
        for (int i = 0; i < l; i++) {
            Node node = childNodes.item(i);
            if (!(node instanceof Element))
                continue;
            Element subelt = (Element) node;
            String kind = subelt.getLocalName();
            String name = subelt.getAttribute(NAME_ATTR);
            String msg = subelt.getAttribute(MESG_ATTR);
            int score = parseIntAttribute(subelt, SCORE_ATTR);
            if (kind.equals(RULE_SCORE_ELT)) {
                Element[] ruleElt = XMLParser.getElements(subelt, RULE_ELT);
                int l2 = ruleElt.length;
                String[] ruleNames = new String[l2];
                for (int i2 = 0; i2 < l2; i2++) {
                    ruleNames[i2] = ruleElt[i2].getAttribute(NAME_ATTR);
                }
                UseRule useRule = new UseRule(ruleNames, name, msg, score);
                elements.add(useRule);
                elementDictionary.put(useRule.name, useRule);
                for (String ruleName : ruleNames) {
                    ArrayList<UseRule> cell = rules.get(ruleName);
                    if (cell == null) {
                        cell = new ArrayList<UseRule>();
                        rules.put(ruleName, cell);
                    }
                    cell.add(useRule);
                }
            } else if (kind.equals(PATTERN_SCORE_ELT)) {
                String pattern = subelt.getAttribute(PATTERN_ATTR);
                StringPattern pat = new StringPattern(pattern, name, msg, score);
                elements.add(pat);
                elementDictionary.put(pat.name, pat);
                patterns.add(pat);
            } else if (kind.equals(CONJUNCTION_SCORE_ELT)) {
                Element[] elementElt = XMLParser.getElements(subelt, ELEMENT_ELT);
                int l2 = elementElt.length;
                String[] elementNames = new String[l2];
                for (int i2 = 0; i2 < l2; i2++) {
                    elementNames[i2] = elementElt[i2].getAttribute(NAME_ATTR);
                }
                ConjunctionElement conjRule = new ConjunctionElement(elementNames, name, msg, score);
                elements.add(conjRule);
                elementDictionary.put(conjRule.name, conjRule);
            } else if (kind.equals(DISJUNCTION_SCORE_ELT)) {
                Element[] elementElt = XMLParser.getElements(subelt, ELEMENT_ELT);
                int l2 = elementElt.length;
                String[] elementNames = new String[l2];
                for (int i2 = 0; i2 < l2; i2++) {
                    elementNames[i2] = elementElt[i2].getAttribute(NAME_ATTR);
                }
                DisjunctionElement conjRule = new DisjunctionElement(elementNames, name, msg, score);
                elements.add(conjRule);
                elementDictionary.put(conjRule.name, conjRule);
            } else if (kind.equals(CATCHER_ELT)) {
                String pattern = subelt.getAttribute(PATTERN_ATTR);
                StringCatcher catcher = new StringCatcher(pattern, msg);
                catchers.add(catcher);
            } else if (kind.equals(PERMISSION_ELT)) {
                String permission = subelt.getAttribute(PERMISSION_ATTR);
                PermissionScore perm = new PermissionScore(permission, name, msg, score);
                permissions.add(perm);
                elements.add(perm);
                elementDictionary.put(name, perm);
            }
        }
    }

    /**
     * Get the global score.
     * 
     * @return
     */
    public int getScore() {
        if (score < 0) {
            score = 0;
            for (ScoreElement element : elements) {
                if (element.isUsed())
                    score += element.score;
            }
        }
        return score;
    }

    /**
     * Match globally the score against an element (result).
     * 
     * @param v
     */
    public void matchValue(AbsValue v) {
        for (StringPattern p : patterns) {
            p.match(v);
        }
    }

    /**
     * Match globally the score against a rule name.
     * 
     * @param ruleName
     */
    public void matchRule(String ruleName) {
        ArrayList<UseRule> matchings = rules.get(ruleName);
        if (matchings != null) {
            for (UseRule rule : matchings) {
                rule.use();
            }
        }
    }

    /**
     * Match globally the score against a string.
     * 
     * @param s
     */
    public void matchString(String s) {
        for (StringPattern p : patterns) {
            p.match(s);
        }
        for (StringCatcher c : catchers) {
            c.match(s);
        }
    }

    /**
     * Print out the result of the report. If in XML format, just give back the
     * global score. Otherwise, tries to give a pretty rendering. If the score
     * of an element is 0, it is not printed. The purpose of such element is to
     * be present for conjunctive and disjunctive scores (that should have a non
     * zero score unless they are themselves part of an englobing score).
     * 
     * @param out
     * @param xmlFormat
     */
    public void tell(PrintStream out, boolean xmlFormat) {
        int v = getScore();
        if (xmlFormat) {
            XMLStream xmlout = new XMLStream(out);
            xmlout.element("score");
            xmlout.attribute("value", v);
            xmlout.close();
        } else {
            String category = (v <= threshold / 2) ? "okScore" : (v > threshold ? "badScore"
                    : "dangerScore");
            out.println(HtmlOutput.openDiv("score"));
            out.println("<h1 class=\"" + category + "\">Score: " + v + "</h1>");
            out.println("<table class=\"result\">");
            for (ScoreElement element : elements) {
                if (element.isUsed() && element.score > 0) {
                    out.print("<tr><td class=\"result\">");
                    out.print(element.message);
                    out.print("</td><td class=\"result\">");
                    out.print(element.score);
                    out.println("</td></tr>");
                }
            }
            out.println("</table>");
            out.println(HtmlOutput.closeDiv());
            out.println(HtmlOutput.openDiv("catched"));
            for (StringCatcher catcher : catchers) {
                if (catcher.table.size() == 0)
                    continue;
                out.println(HtmlOutput.header(3, catcher.name));
                out.println(HtmlOutput.list(catcher.table));
                out.println("<ul>");
                for (String elt : catcher.table)
                    out.println("<li>" + elt + "</li>");
                out.println("</ul>");
            }
            out.println(HtmlOutput.closeDiv());
        }

    }

    /**
     * Support method to parse an attribute, coerce it to an integer and give
     * back 0 if not ok.
     * 
     * @param elt
     * @param attr
     * @return
     */
    private static int parseIntAttribute(Element elt, String attr) {
        int v = 0;
        try {
            v = Integer.parseInt(elt.getAttribute(attr));
        } catch (Exception e) {
            v = 0;
        }
        return v;
    }

    /**
     * Wraps a printstream to catch errors.
     * 
     * @param out
     * @return
     */
    public PrintStream wrap(PrintStream out) {
        try {
            return new ScoreReport.ScorePrintStream(out, this);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 not supported", e);
        }
    }

    /**
     * Gives back the threshold where the score is exceeded (bad application).
     * 
     * @return
     */
    public int getThreshold() {
        return threshold;
    }

    /**
     * Get the set of scores associated to permission usage.
     * 
     * @return
     */
    public Collection<PermissionScore> getScorePermissions() {
        return permissions;
    }

}

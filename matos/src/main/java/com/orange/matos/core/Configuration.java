/*
 * $Id: Configuration.java 2285 2013-12-13 13:07:22Z piac6784 $
 */

package com.orange.matos.core;

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

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.orange.matos.utils.FileUtilities;
import com.orange.matos.utils.HtmlOutput;

/**
 * This class contains the generic part of the context of MATOS during the
 * analysis of a given midletsuite.
 * 
 * @author piac6784
 */
public class Configuration {
    
    /** The property name for the temporary folder */
    public static final String MATOS_TEMP_KEY = "matos.temp";

    /** The property name for the home of matos. */
    public static final String MATOS_HOME_KEY = "matos.lib";

    /**
     * Basic style
     */
    public static final String STYLE_SHEET_NAME = "style.css";

    /**
     * Property name for css
     */
    public static final String CSS_FILE_KEY = "matos.cssFile";

    /**
     * Property name for timing
     */
    public static final String TIMING_ENABLED_KEY = "matos.timing";

    /**
	 * 
	 */
    public static final String CONSIDER_SAME_NAME_KEY = "matos.considerSameName";

    /**
     * Output directory
     */
    public static final String OUTPUT_DIRECTORY_KEY = "outputDir";

    /**
     * Default rules
     */
    public static final String DEFAULT_RULES_KEY = "anasoot.ruleDefaultFile";

    /**
     * Config file
     */
    public static final String CONFIG_FILE_NAME = "config.prp";

    /**
     * Property name Proxy defined
     */
    public static final String PROXY_SET_KEY = "proxyset";

    /**
     * Property name : proxy host
     */
    public static final String PROXY_HOST_KEY = "proxyhost";

    /**
     * Property name : proxy port
     */
    public static final String PROXY_PORT_KEY = "proxyport";

    private final static List<ExitHook> exitHooks = new ArrayList<ExitHook>();

    /**
     * Log file path
     */
    public final static String logFilePath = new File(System.getProperty(MATOS_HOME_KEY), "log.txt")
            .getAbsolutePath();

    static private Configuration configuration = null;

    private String configFile;

    private boolean xmlFormat = false;

    /**
     * Base file to create a temp dir
     */
    private File rootTempDir;

    /**
     * Folder for jars
     */
    private String libDir;

    /**
     * Folder for temporary items
     */
    private File tempDir;

    /**
     * Standard CSS file for results.
     */
    private String css;

    /**
     * Is timing enabled
     */
    private boolean timing;

    /**
	 * 
	 */
    private int httpMaxConTime;

    /**
	 * 
	 */
    private int httpMaxDwnTime;

    private Map<String, MatosPhase[]> phases;

    private Properties prop;

    private Properties transientProp;

    private ProfileManager profileManager;

    /**
     * Matos in analysis mode
     */
    private boolean inAnalysisMode = false;

    private Map<String, ArrayList<Object>> appInfo;

    /**
     * File separator for the architecture.
     */
    static final public String fileSeparator = System.getProperty("file.separator");

    /**
     * @param phases
     * @param libDirSpec
     * @throws Alert
     */
    public Configuration(Map<String, MatosPhase[]> phases, String libDirSpec, String tempSpec)
            throws Alert {

        this.phases = phases;
        appInfo = new HashMap<String, ArrayList<Object>>();
        libDir = libDirSpec == null ? System.getProperty(MATOS_HOME_KEY) : libDirSpec;
        if (libDir == null)
            throw new Alert("No LIB system variable");

        String rootTempPath = tempSpec == null ? System.getProperty(MATOS_TEMP_KEY) : tempSpec;
        if (rootTempPath == null) {
            throw new Alert("No TEMP variable");
        }
        setRootTempDir(new File(rootTempPath));
        Thread sdh = new Thread(new ShutdownHook(getTempDir()));
        if (!getTempDir().mkdir()) {
            throw Alert.raised(null, "Cannot create temporary directory");
        }
        Runtime.getRuntime().addShutdownHook(sdh);
        configFile = (new File(libDir, CONFIG_FILE_NAME)).getAbsolutePath();
        // dtdDir = new File(libDir,"dtd").getAbsolutePath();
        reset();

        File ruleDirFile = new File(libDir, string("anasoot.rules"));
        profileManager = new ProfileManager(ruleDirFile);
        timing = bool(TIMING_ENABLED_KEY);
        css = string(CSS_FILE_KEY);

        File cssFile = new File(css);
        if (!cssFile.exists()) {
            css = new File(System.getProperty(MATOS_HOME_KEY), css).getAbsolutePath();
        }

        for (MatosPhase[] pArray : phases.values()) {
            for (MatosPhase p : pArray)
                p.init(this);
        }

        System.setErr(Out.getLog()); // redirect err to log
    }

    /**
     * static hook to get the configuration
     */
    static public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Register a callback to be executed when the user stops the application.
     * Typically for removing transient resource on the desktop.
     * 
     * @param hook
     */
    static void registerHook(ExitHook hook) {
        exitHooks.add(hook);
    }

    /**
     * The official way to quit the application : should remove directories
     * (buggy so we use a hook instead)
     */
    static public void exit(int i) {
        for (ExitHook hook : exitHooks)
            hook.onExit();
        System.exit(i);
    }

    /**
     * The asynchronous way of cleaning up the application. A hook to register
     * with the runtime system.
     * 
     * @author piac6784
     */
    static class ShutdownHook implements Runnable {
        File file;

        ShutdownHook(File file) {
            this.file = file;
        }

        @Override
        public void run() {
            if (!FileUtilities.removeDir(file)) {
                Out.getLog().println("Cannot remove directory " + file);
            }
        }
    }

    /**
     * A hack to be able to use "variables" in the property file and make it
     * independent from the configuration. There is only one such variable :
     * %LIB%
     */
    public String substituteVars(String raw) {
        raw = raw.replace('/', File.separatorChar).replace(':', File.pathSeparatorChar);
        int i = 0, j;
        StringBuffer buf = new StringBuffer();
        String apiLibDir = new File(libDir, "api").getAbsolutePath();
        ;
        while ((j = raw.indexOf("%LIB%", i)) >= 0) {
            buf.append(raw.substring(i, j));
            buf.append(apiLibDir);
            i = j + 5;
        }
        ;
        buf.append(raw.substring(i));
        return buf.toString();
    }

    /**
     * Get a boolean property from the configuration properties of the tool
     * 
     * @param s the name of the property
     * @return the value of the property
     */
    public boolean bool(String s) {
        String v = null;
        if (inAnalysisMode)
            v = transientProp.getProperty(s);
        else
            v = prop.getProperty(s);
        if ((v == null) || (v.equals("true")))
            return true;
        if (v.equals("false"))
            return false;
        // Ne devrait pas mais trop penible
        return true;
    }

    /**
     * A boolean value from properties with default.
     * 
     * @param s
     * @param defaultValue
     * @return
     */
    public boolean bool(String s, boolean defaultValue) {
        String v = null;
        if (inAnalysisMode)
            v = transientProp.getProperty(s);
        else
            v = prop.getProperty(s);
        if (v == null) {
            return defaultValue;
        } else {
            if (v.equals("true")) {
                return true;
            } else if (v.equals("false"))
                return false;
            else
                return defaultValue;
        }
    }

    /**
     * Get an integer property from the configuration properties of the tool
     * 
     * @param s the name of the property
     * @return the value of the property
     */
    public int integer(String s) throws NumberFormatException {
        if (inAnalysisMode)
            return Integer.parseInt(transientProp.getProperty(s));
        else
            return Integer.parseInt(prop.getProperty(s));
    }

    /**
     * Get an integer property from the configuration properties of the tool
     * 
     * @param s the name of the property
     * @param defaultValue a default value
     * @return the value of the property if it exists, or the default value
     *         otherwise
     */
    public int integer(String s, int defaultValue) throws NumberFormatException {
        if (inAnalysisMode)
            return Integer.parseInt(transientProp.getProperty(s, "" + defaultValue));
        else
            return Integer.parseInt(prop.getProperty(s, "" + defaultValue));
    }

    /**
     * Get a string property from the configuration properties of the tool
     * 
     * @param s the name of the property
     * @return the value of the property
     */
    public String string(String s) {
        if (inAnalysisMode)
            return transientProp.getProperty(s);
        else
            return prop.getProperty(s);
    }

    /**
     * Get a string property from the configuration properties of the tool
     * 
     * @param s the name of the property
     * @param d the default value
     * @return the value of the property
     */
    public String string(String s, String d) {
        if (inAnalysisMode)
            return transientProp.getProperty(s, d);
        else
            return prop.getProperty(s, d);
    }

    /**
     * Set a property of the tool in the config.prp file
     * 
     * @param key
     * @param value
     */
    public void setPropertyToSave(String key, String value) {
        prop.setProperty(key, value);
    }

    /**
     * Set a property of the tool only for the current analysis
     * 
     * @param key
     * @param value
     */
    public void setTransientProperty(String key, String value) {
        transientProp.setProperty(key, value);
    }

    /**
     * Reset the tool properties to the one defined in the initial configuration
     * file
     */
    public void reset() throws Alert {
        // prop = new Properties();
        prop = new SortedProperties();
        try {
            InputStream stream = new FileInputStream(configFile);
            try {
                prop.load(stream);
            } finally {
                stream.close();
            }
        } catch (IOException e) {
            throw Alert.raised(e, "Cannot find internal resource file: " + configFile);
        }
    }

    /**
     * Write properties to file
     * 
     * @throws Alert
     */
    public void writeProperties() throws Alert {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(new File(configFile));
            try {
                prop.store(fos, "");
            } finally {
                fos.close();
            }
        } catch (IOException e) {
            throw Alert.raised(e, "Cannot read configuration file.");
        }
    }

    private String toHTML(String value) {
        String htmlValue = value;
        // 1st : go back to ASCII characters
        htmlValue = htmlValue.replaceAll("&lt;", "<");
        htmlValue = htmlValue.replaceAll("&gt;", ">");
        htmlValue = htmlValue.replaceAll("&amp;", "&");

        // 2nd : replace by HTML specifics. Order is important !
        htmlValue = htmlValue.replaceAll("&", "&amp;"); // this will not match
                                                        // "&lt;", "&gt;",
                                                        // "&amp;"
        htmlValue = htmlValue.replaceAll("<", "&lt;");
        htmlValue = htmlValue.replaceAll(">", "&gt;");

        return htmlValue;
    }

    /**
     * Prints the header
     * 
     * @param outStream
     * @param xmlFormat
     * @param head
     * @param value
     */
    public void printHeader(PrintStream outStream, boolean xmlFormat, String head, String value) {
        if (xmlFormat) {
            outStream.println("<header name=\"" + head + "\">"
                    + (value == null ? "-" : toHTML(value)) + "</header>");
        } else {
            outStream.println(HtmlOutput.row(HtmlOutput.cellHead(head == null ? "-" : head)
                    + HtmlOutput.cell(value == null ? "-" : toHTML(value))));
        }
    }

    /**
     * Utility function that parses the lines defining midlets in the JAD or the
     * manifest
     * 
     * @param s the value of the property to parse
     * @return a list containing usually 3 components (midletname, icon, class)
     */
    public static List<String> parseCommaList(String s) {
        String input = s + ",";
        List<String> res = new ArrayList<String>();
        int start = 0;
        int end = input.indexOf(',');
        while (end != -1) {
            res.add(input.substring(start, end).trim());
            start = end + 1;
            end = input.indexOf(',', start);
        }
        return res;
    }

    /**
     * Check if it is a local phase
     * 
     * @param phase
     * @return
     */
    public boolean localphase(MatosPhase phase) {
        return bool(phase.getName() + ".enabled") && !phase.isGlobal();
    }

    /**
     * Check if it is a global phase
     * 
     * @param phase
     * @return
     */
    public boolean globalphase(MatosPhase phase) {
        return bool(phase.getName() + ".enabled") && phase.isGlobal();
    }

    /**
     * Phases for a given language
     * 
     * @param language
     * @return
     */
    public MatosPhase[] phases(String language) {
        return phases.get(language);
    }

    /**
     * Init analysis with transient properties from rule file.
     * 
     * @param ruleFile
     * @throws Alert
     */
    public void initAnalysis(RuleFile ruleFile) throws Alert {
        inAnalysisMode = true;
        transientProp = (Properties) prop.clone();
        reset();
        ruleFile.activate(this);
    }

    /**
     * Gets the list of rules with a given prefix.
     * 
     * @param prefix
     * @return
     * @throws Alert
     */
    public List<String> getRulesWithPrefix(String prefix) {
        List<String> result = new ArrayList<String>();
        List<String> rules = getProfiles();
        for (String rule : rules) {
            if (rule.startsWith(prefix))
                result.add(rule);
        }
        return result;
    }

    /**
     * Gets the list of rules for Android (prefix Android).
     * 
     * @return
     * @throws Alert
     */
    public List<String> getAndroidRules() {
        return getRulesWithPrefix("Android");
    }

    /**
     * Gets the list of rules for MIDP (prefix MIDP)
     * 
     * @return
     * @throws Alert
     */
    public List<String> getMIDPRules() {
        return getRulesWithPrefix("MIDP");
    }

    /**
     * Populate a classloader with the jars in the definition folder for custom
     * rules.
     * 
     * @return
     */
    public ClassLoader getCustomClassLoader() {
        return profileManager.getCustomClassLoader();
    }

    /**
     * Get rule definitions stored in profiles.
     * 
     * @return
     */
    public List<String> getProfiles() {
        return profileManager.getProfiles();
    }

    /**
     * Return the current profile manager (finds rules and specific code to
     * implement rules)
     * 
     * @return
     */
    public ProfileManager getProfileManager() {
        return profileManager;
    }

    /**
     * How to get a single property value by parsing the whole file. This is
     * used only once during initialisation when configuration is not set yet.
     * 
     * @param in the stream on which to read the property file
     * @param key the single key we need
     * @return its value
     * @throws Exception
     */
    public String getPropertiesFromFile(InputStream in, String key) throws Exception {
        Properties prop = new Properties();
        prop.load(in);
        in.close();
        return prop.getProperty(key);
    }

    /**
     * Check if the output of the application should be in XML format or HTML
     * format
     * 
     * @return true for XML, false for HTML
     */
    public boolean xmlFormat() {
        return xmlFormat;
    }

    /**
     * Set the output of the analyser to XML format (the default is HTML).
     */
    public void setXmlOutput() {
        xmlFormat = true;
    }

    /**
     * Dumps the configuration to a stream
     * 
     * @param out
     */
    public void dump(PrintStream out) {
        try {
            this.transientProp.store(out, "debug");
        } catch (IOException e) {
            out.println("Problem dumping");
            e.printStackTrace();
        }
    }

    /**
     * The appInfo structure maintains a list of information for each
     * "application" analysed. For MIDP, these are the midlets.
     * 
     * @param appName the name of the midlet
     * @param obj the opaque value associated to it.
     */
    public void resetAppInfo() {
        appInfo.clear();
    }

    /**
     * Set the appInfo structure maintains a list of information for each
     * "application" analysed. For MIDP, these are the midlets.
     * 
     * @param appName the name of the midlet
     * @param obj the opaque value associated to it.
     */
    public void setAppInfo(String appName, Object obj) {
        ArrayList<Object> contents = appInfo.get(appName);
        if (contents == null) {
            contents = new ArrayList<Object>();
            appInfo.put(appName, contents);
        }
        contents.add(obj);
    }

    /**
     * Gives back a list of all values associated to a given application (never
     * null).
     * 
     * @param appName the name of the application.
     * @return the list of opaque values.
     */
    public ArrayList<Object> getAppInfo(String appName) {
        ArrayList<Object> contents = appInfo.get(appName);
        return (contents == null) ? new ArrayList<Object>() : contents;
    }

    /**
     * Sets timeout for HTTP connection.
     */
    public void setHttpTimeout() {
        httpMaxConTime = 10000; // default value, expressed in milliseconds
        httpMaxDwnTime = 300000;
        try {
            httpMaxConTime = 1000 * integer("httpMaxConnectionTime");
        } catch (NumberFormatException e) {
            Out.getLog().println("bad format for httpMaxConTime");/*
                                                                   * just keep
                                                                   * default
                                                                   * value
                                                                   */
        }
        try {
            httpMaxDwnTime = 1000 * integer("httpMaxDownloadTime");
        } catch (NumberFormatException e) {
            Out.getLog().println("bad format for httpMaDwnTime");
        }
    }

    /**
     * Tells if timing should be displayed (debug function).
     * 
     * @return
     */
    public boolean timingEnabled() {
        return timing;
    }

    /**
     * Standard classath for MIDP
     */
    public String midpClasspath() {
        return expandDirs(substituteVars(string("anasoot.midp-classpath")));
    }

    /**
     * Standard classpath for Android
     */
    public String androidClasspath() {
        return expandDirs(substituteVars(string("anasoot.android-classpath")));
    }

    /**
     * This method adds to Soot the functionality of wildcards patterns present
     * in Java 6 classpath.
     * 
     * @param origPath
     * @return
     */
    private String expandDirs(String origPath) {
        System.out.println("original " + origPath);
        StringBuilder paths = new StringBuilder();
        for (String path : origPath.split(File.pathSeparator)) {
            if (path.endsWith(File.separator + "*")) {
                String folderName = path.substring(0, path.length() - 2);
                File folder = new File(folderName);
                if (folder.exists()) {
                    File[] jars = folder.listFiles(new FileFilter() {
                        @Override
                        public boolean accept(File pathname) {
                            return pathname.getName().endsWith(".jar");
                        }
                    });
                    if (jars != null) {
                        for (File jar : jars) {
                            paths.append(File.pathSeparator);
                            paths.append(jar.getAbsolutePath());
                        }
                    }
                }
            } else {
                paths.append(File.pathSeparator);
                paths.append(path);
            }
        }
        System.out.println("final :" + paths.toString());
        return paths.toString();
    }

    /**
     * Gets the lib dir.
     * 
     * @return the lib dir
     */
    public String getLibDir() {
        return libDir;
    }

    /**
     * Get the temporary directory.
     * 
     * @return
     */
    public File getTempDir() {
        return tempDir;
    }

    /**
     * Gets the root temp dir.
     * 
     * @return the root temp dir
     */
    public File getRootTempDir() {
        return rootTempDir;
    }

    /**
     * Sets the root temp dir.
     * 
     * @param tmp the new root temp dir
     */
    public void setRootTempDir(File tmp) throws Alert {
        rootTempDir = tmp;
        if (!rootTempDir.isDirectory()) {
            String message = "The temporary directory specified for Mobile Application Analyser is: "
                    + rootTempDir
                    + ". I can't find any directory with that name. Please adapt your TEMP environment variable.";
            throw Alert.raised(null, message);
        }

        try {
            tempDir = File.createTempFile("ana", "", rootTempDir);
        } catch (IOException e) {
            throw Alert.raised(e, "Cannot create temporary directory");
        }
        if (getTempDir().exists() && !getTempDir().delete()) {
            Out.getLog().println("Cannot delete temporary directory " + getTempDir());
        }
    }

    /**
     * Gets the path to css file
     *
     * @return the css
     */
    public String getCss() {
        return css;
    }

    /**
     * Gets the http max down time.
     *
     * @return the http max down time
     */
    public int getHttpMaxDwnTime() {
        return httpMaxDwnTime;
    }

    /**
     * Gets the http max connected time
     * @return
     */
    public int getHttpMaxConTime() {
        return httpMaxConTime;
    }

    /**
     * Checks if is in analysis mode.
     *
     * @return true, if is in analysis mode
     */
    public boolean isInAnalysisMode() {
        return inAnalysisMode;
    }

    /**
     * Sets the in analysis mode.
     *
     * @param inAnalysisMode the new in analysis mode
     */
    public void setInAnalysisMode(boolean inAnalysisMode) {
        this.inAnalysisMode = inAnalysisMode;
    }

}

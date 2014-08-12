
package com.orange.analysis.implemchecker;

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
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.util.ClassPath;
import org.apache.bcel.util.Repository;
import org.apache.bcel.util.SyntheticRepository;

import com.orange.matos.core.Alert;
import com.orange.matos.core.AppDescription;
import com.orange.matos.core.Configuration;
import com.orange.matos.core.MatosPhase;
import com.orange.matos.core.Out;
import com.orange.matos.core.RuleFile;
import com.orange.matos.core.XMLStream;
import com.orange.matos.java.MidletSuite;
import com.orange.matos.utils.HtmlOutput;

/**
 * This class implements a phase preliminary to soot analysis which serves two
 * important purposes:
 * <ul>
 * <li>The analysis performed by soot is correct only if the users are denied
 * the right to implement some system interfaces that describe classes giving
 * back reasonnable URI. This phase checks that none of these interfaces are
 * implemented.
 * <li>This phase collect all the implementations of MIDlet and store them in
 * the property file of the midlet. This is a fake property but this is the only
 * simple way to exchange data between phases.
 * </ul>
 * As a long term goal we may try to replace the output result by an XML
 * structure shared by all the phases and accumulating the results.
 * 
 * @author Pierre Cregut
 */
public class ImplemCheckerPhase implements MatosPhase {

    /**
     * Midlet class in MIDP.
     */
    private static final String MIDLET_CLASSNAME = "javax.microedition.midlet.MIDlet";

    /**
     * The key to describe interface for which we do not want implementation in
     * application code
     */
    private static final String FORBIDDEN_ITF_KEY = "implemchecker.forbiddenImplem";

    private Configuration config;

    /**
     * The key that defines if we check all midlets or only visible ones.
     */
    public static final String MATOS_CHECK_ALL_MIDLETS_KEY = "implemchecker.checkAllMidlets";

    /**
     * Message printed when not enough information
     */
    private static final String MESSAGE_BAD_ARGS = "midlet suite expected.";

    @Override
    public String getName() {
        return "classesChecking";
    }

    @Override
    public void init(Configuration config) {
        this.config = config;
    }

    @Override
    public boolean isGlobal() {
        return true;
    }

    @Override
    public boolean run(String midletName, AppDescription descr, RuleFile ruleFile,
            PrintStream outStream) throws Alert {
        if (!(descr instanceof MidletSuite))
            throw Alert.raised(null, MESSAGE_BAD_ARGS);
        MidletSuite ms = (MidletSuite) descr;
        File jarFile = ms.jarFile;
        boolean verdict = true;
        String cp = jarFile.getAbsolutePath() + File.pathSeparator + config.midpClasspath();
        System.setProperty("java.class.path", cp);
        ClassPath classpath = new ClassPath(cp);
        Repository repository = SyntheticRepository.getInstance(classpath);

        String forbiddenImplems = config.string(FORBIDDEN_ITF_KEY);
        Set<String> toMonitor = new HashSet<String>();
        if (forbiddenImplems != null && forbiddenImplems.length() != 0) {
            String[] forbiddenItfs = forbiddenImplems.split(",");
            for (int i = 0; i < forbiddenItfs.length; i++) {
                toMonitor.add(forbiddenItfs[i].trim());
            }
        }
        Set<String> seen = new HashSet<String>();
        Set<String> allMidlets = new HashSet<String>();
        boolean first = true;
        JarFile jf = null;
        try {
            jf = new JarFile(jarFile);

            for (Enumeration<JarEntry> ents = jf.entries(); ents.hasMoreElements();) {

                JarEntry entry = ents.nextElement();
                String name = entry.getName();
                if (name.endsWith(".class")) {

                    String classname = name.substring(0, name.length() - 6).replace('/', '.')
                            .replace('$', '.');
                    JavaClass implem;
                    try {
                        implem = repository.loadClass(classname);
                    } catch (ClassNotFoundException e) {
                        implem = null;
                    }
                    if (implem != null && !implem.isInterface() && !implem.isAbstract()) {
                        ArrayList<String> superClasses;
                        try {
                            superClasses = getSuperClasses(repository, implem);
                            for (String spClass : superClasses) {
                                if (spClass.equals(MIDLET_CLASSNAME)) {
                                    allMidlets.add(implem.getClassName());
                                }
                            }
                        } catch (Exception e) {
                            System.out.println(implem.getRepository().getClassPath());
                            Out.getMain().println("Super class:" + e.getMessage());
                        }
                        String[] itfs = implem.getInterfaceNames();
                        for (String itfName : itfs) {
                            if (toMonitor.contains(itfName)) {
                                if (config.xmlFormat()) {
                                    XMLStream xmlout = new XMLStream(outStream);
                                    xmlout.element("forbiddenImplementation");
                                    xmlout.attribute("interface", itfName);
                                    xmlout.attribute("class", implem.getClassName());
                                } else {
                                    if (first) {
                                        outStream.println(HtmlOutput.header(1,
                                                "Implementation checker"));
                                        first = false;
                                    }
                                    if (!seen.contains(itfName)) {
                                        String msg = "Forbidden implementation of <code>" + itfName
                                                + "</code> interface.";
                                        outStream.println(HtmlOutput.paragraph(HtmlOutput.color(
                                                "red", msg)));
                                        seen.add(itfName);
                                    }
                                    verdict = false;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (jf != null)
                    jf.close();
            } catch (IOException e) {
            }
        }
        for (String midlet : allMidlets) {
            config.setAppInfo("*", midlet);
        }

        return verdict;
    }

    private ArrayList<String> getSuperClasses(Repository repository, JavaClass implem) {
        ArrayList<String> result = new ArrayList<String>();
        String superName = null;
        while ((superName = implem.getSuperclassName()) != null) {
            if (result.contains(superName))
                break;
            result.add(superName);
            try {
                implem = repository.loadClass(superName);
            } catch (ClassNotFoundException e) {
                implem = null;
            }
        }
        return result;
    }

    @Override
    public String getMessage() {
        return null;
    }

    @Override
    public int getScore() {
        return -1;
    }

}

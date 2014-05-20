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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * The Class ProfileManager.
 */
public class ProfileManager {
    
    /** The rule dir file. */
    final private File ruleDirFile;
    
    final private File [] jars;
    
    private URLClassLoader classLoader;

    /**
     * Instantiates a new profile manager.
     *
     * @param ruleDirFile the rule dir file
     */
    public ProfileManager(File ruleDirFile) {
        super();
        this.ruleDirFile = ruleDirFile;
        jars = getJars();
    }
    
    /**
     * Populate a classloader with the jars in the definition folder for custom rules.
     * 
     * @return
     */
    public URLClassLoader getCustomClassLoader() {
        if (classLoader == null) {
            URL[] jarUrls = new URL[jars.length];
            try {
                for (int i = 0; i < jars.length; i++)
                    jarUrls[i] = jars[i].toURI().toURL();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            classLoader = new URLClassLoader(jarUrls,this.getClass().getClassLoader());
        }
        return classLoader;
    }
    
    
    /**
     * Get rule definitions stored in profiles.
     * @return
     */
    public List<String> getProfiles() {
        List<String> result = new ArrayList<String>();
        Pattern rulePattern = Pattern.compile("^rules/([^/]*)\\.xml$");
        for(File jar : jars) {
            try {
                ZipFile zf = new ZipFile(jar);
                Enumeration <? extends ZipEntry> entries = zf.entries();
                while(entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String name = entry.getName();
                    Matcher matcher = rulePattern.matcher(name);
                    if (matcher.matches()) { result.add(matcher.group(1)); }
                }
            } catch (ZipException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
    
    private File [] getJars() {
        File [] jars = ruleDirFile.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".jar");
            }
        });
        return jars;
    }
    
    /**
     * Gets the rule file from the jar.
     *
     * @param rule the rule
     * @return the rule resource
     */
    public URL getRuleResource(String rule) {
        return getCustomClassLoader().findResource("rules/" + rule + ".xml");
    }

}

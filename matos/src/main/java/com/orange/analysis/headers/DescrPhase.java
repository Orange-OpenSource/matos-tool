
package com.orange.analysis.headers;

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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.orange.matos.core.Alert;
import com.orange.matos.core.AppDescription;
import com.orange.matos.core.Configuration;
import com.orange.matos.core.MatosPhase;
import com.orange.matos.core.RuleFile;
import com.orange.matos.core.XMLStream;
import com.orange.matos.java.MidletSuite;
import com.orange.matos.utils.HtmlOutput;

/**
 * @author Pierre Cregut The phase handling the descriptors of the midlet (JAD and
 *         internal manifest).
 */
public class DescrPhase implements MatosPhase {

    private HashMap<String, SimpleAttrChecker> attrMap;

    private ArrayList<RegexpAttributeChecker> regexpList;

    private Configuration config;

    private boolean verdict;

    private PrintStream outStream;

    private String[] mandatoryJarAttribute = {
            "MIDlet-Name", "MIDlet-Version", "MIDlet-Vendor"
    };

    private String[] mandatoryJadAttribute = {
            "MIDlet-Name", "MIDlet-Version", "MIDlet-Vendor", "MIDlet-Jar-URL", "MIDlet-Jar-Size"
    };

    private CertificateChecker certificateChecker;

    @Override
    public boolean isGlobal() {
        return true;
    }

    @Override
    public String getName() {
        return "manifestChecking";
    }

    @Override
    public void init(Configuration config) {
        this.config = config;
    }

    private void initForEachRun(PrintStream outStream, boolean xmlFormat, File jadFile, File jarFile) {
        verdict = true;
        this.outStream = outStream;
        if (!xmlFormat)
            outStream.println(HtmlOutput.header(1, "Descriptors conformity"));
        attrMap = new HashMap<String, SimpleAttrChecker>();
        String midletSuiteNameRegexpSpec = config.string("descriptor.midletSuiteNameRegexp");
        if (midletSuiteNameRegexpSpec != null && midletSuiteNameRegexpSpec.length() == 0)
            midletSuiteNameRegexpSpec = null;
        attrMap.put("MIDlet-Name", new MandJadJarIdentAttrChecker("MIDlet-Name",
                midletSuiteNameRegexpSpec, jadFile));
        attrMap.put("MIDlet-Version", new MidletVersionAttrChecker(jadFile));
        String vendorNameRegexpSpec = config.string("descriptor.vendorNameRegexp");
        if (vendorNameRegexpSpec != null && vendorNameRegexpSpec.length() == 0)
            vendorNameRegexpSpec = null;
        attrMap.put("MIDlet-Vendor", new MandJadJarIdentAttrChecker("MIDlet-Vendor",
                vendorNameRegexpSpec, jadFile));
        attrMap.put("MicroEdition-Profile", new MandJadOrJarAttrChecker("MicroEdition-Profile",
                jadFile));
        attrMap.put("MicroEdition-Configuration", new MandJadOrJarAttrChecker(
                "MicroEdition-Configuration", jadFile));
        attrMap.put("MIDlet-Jar-URL", new MandJadAttrChecker("MIDlet-Jar-URL", jadFile));
        attrMap.put("MIDlet-Jar-Size", new JarSizeAttrChecker(jadFile, jarFile));
        attrMap.put("MIDlet-Description", new SimpleAttrChecker("MIDlet-Description"));
        attrMap.put("MIDlet-Icon", new IconChecker(config, jarFile));
        attrMap.put("MIDlet-Info-URL", new SimpleAttrChecker("MIDlet-Info-URL"));
        attrMap.put("MIDlet-Data-Size", new SimpleAttrChecker("MIDlet-Data-Size", "[0-9]*"));
        attrMap.put("MIDlet-Permissions", new SimpleAttrChecker("MIDlet-Permissions"));
        attrMap.put("MIDlet-Permissions-Opt", new SimpleAttrChecker("MIDlet-Permissions-Opt"));
        attrMap.put("MIDlet-Install-Notify", new SimpleAttrChecker("MIDlet-Install-Notify"));
        attrMap.put("MIDlet-Delete-Notify", new SimpleAttrChecker("MIDlet-Delete-Notify"));
        attrMap.put("MIDlet-Delete-Confirm", new SimpleAttrChecker("MIDlet-Delete-Confirm"));
        regexpList = new ArrayList<RegexpAttributeChecker>();
        certificateChecker = new CertificateChecker();
        regexpList.add(certificateChecker);
        regexpList.add(new MidletAttrChecker(config, jarFile));
        ChapiIdChecker chapiIdChecker = new ChapiIdChecker(config);
        regexpList.add(chapiIdChecker);
        regexpList.add(new ChapiChecker(config, chapiIdChecker));
        regexpList.add(new PushRegistryChecker(config));
        regexpList.add(new OnlyInJADAttrChecker(jadFile));
    }

    /**
     * To report a problem (HTML output) in the Descriptor Phase
     * 
     * @param msg the message to print
     */
    private void problem(String msg) {
        outStream.println(HtmlOutput.paragraph(HtmlOutput.color("red", msg)));
        verdict = false;
    }

    /**
     * To report a successful check (HTML output) in the Descriptor Phase
     * 
     * @param msg the message to print
     */
    private void ok(String msg) {
        outStream.println(HtmlOutput.paragraph(HtmlOutput.color("green", msg)));
    }

    private void manifestConsistency(ZipFile zf, ZipEntry manifest, boolean xmlFormat)
            throws Alert, IOException {
        // verify that attributes do not appear more than once in the jar
        // manifest
        InputStream jarInputStream = zf.getInputStream(manifest);
        long jarManifestSize = manifest.getSize();
        if (jarInputStream != null) {
            try {
                NoDoublePropertiesChecker checker = new NoDoublePropertiesChecker(jarInputStream);
                checkerTreatment(checker, xmlFormat);
            } finally {
                jarInputStream.close();
            }
        }

        // verify the separator character for the jar manifest which must be a
        // ":" character
        // At the moment, an I/O exception occured before this phase if the jar
        // manifest is invalid.
        jarInputStream = zf.getInputStream(manifest);
        if (jarInputStream != null) {
            try {
                Checker checker = new SeparatorChecker(jarInputStream);
                checkerTreatment(checker, xmlFormat);
            } finally {
                jarInputStream.close();
            }
        }

        // verify the end line character for the jar manifest if it is specified
        // in the profile
        jarInputStream = zf.getInputStream(manifest);
        String jarEndLine = config.string("descriptor.jar.endline");
        if (jarInputStream != null && jarEndLine != null && jarEndLine.length() != 0) {
            try {
                Checker checker = new EndLineChecker(jarInputStream, jarEndLine, jarManifestSize);
                checkerTreatment(checker, xmlFormat);
            } finally {
                jarInputStream.close();
            }
        }
        // verify order of attributes in the jar manifest file with the
        // specified option in the profile
        jarInputStream = zf.getInputStream(manifest);
        try {
            String jarAttributes = config.string("descriptor.jar.attributesorder");
            if (jarInputStream != null && jarAttributes != null && jarAttributes.length() != 0) {
                Checker checker = new AttributesOrderChecker(jarInputStream, jarAttributes);
                checkerTreatment(checker, xmlFormat);
            }
        } finally {
            jarInputStream.close();
        }

    }

    private void jadConsistency(File jadFile, boolean xmlFormat) throws Alert,
            FileNotFoundException {
        // verify that attributes do not appear more than once in the jad file
        Checker checker = new NoDoublePropertiesChecker(jadFile);
        checkerTreatment(checker, xmlFormat);

        // verify the separator character for the jad which must be a ":\u0020"
        // or ":\t" character
        checker = new SeparatorChecker(jadFile);
        checkerTreatment(checker, xmlFormat);

        // verify the end line character for the jad if it is specified in the
        // profile
        String jadEndLine = config.string("descriptor.jad.endline");
        if (jadEndLine != null && jadEndLine.length() != 0) {
            checker = new EndLineChecker(jadFile, jadEndLine);
            checkerTreatment(checker, xmlFormat);
        }

        // verify order of attributes in the jad file with the specified option
        // in the profile
        String jadAttributes = config.string("descriptor.jad.attributesorder");
        if (jadAttributes != null && jadAttributes.length() != 0) {
            checker = new AttributesOrderChecker(jadFile, jadAttributes);
            checkerTreatment(checker, xmlFormat);
        }
    }

    boolean isTrustedMidlet(Properties jadProperties, boolean xmlFormat) throws Alert {
        boolean trustedMidlet = false;
        // else, show if the "MIDlet-Jar-RSA-SHA1" is present in the jad
        // if no, the MIDlet suite is considered as an untrusted MIDlet suite
        if (!jadProperties.containsKey("MIDlet-Jar-RSA-SHA1")) {
            trustedMidlet = false;
        } else {
            // init jadMap of certificate object
            for (Map.Entry<Object, Object> e : jadProperties.entrySet()) {
                String key = (String) e.getKey();
                String value = ((String) e.getValue()).trim();
                certificateChecker.inJad(key, value);
            }
            certificateChecker.check();
            if (certificateChecker.getVerdict()) {
                // if they are valid, the MIDlet suite is trusted
                trustedMidlet = true;
                // adds certificates in report
                certificateChecker.writeOkMessage(outStream, xmlFormat);
            } else {
                // else the MIDlet suite is untrusted
                trustedMidlet = false;
                if (xmlFormat) {
                    outStream.println("<checker name=\"Certificate Checker\">");
                    xmlOutput("problem", certificateChecker.problemMessage);
                    outStream.println("</checker>");
                }
                // adds problem message in report
                for (String key : certificateChecker.problemMessage.keySet()) {
                    StringBuilder message = new StringBuilder(key);
                    Set<String> attributes = certificateChecker.problemMessage.get(key);
                    message.append("<br>");
                    message.append("&#160;&#160;&#160;");
                    for (String fragment : attributes) {
                        message.append(fragment + ", ");
                    }
                    String messageString = message.toString();
                    messageString = messageString.substring(0, messageString.lastIndexOf(','));
                    problem(messageString);
                }
                verdict = false;
            }
        }
        return trustedMidlet;
    }

    private void buildAttrMap(File jarFile, File jadFile) {
        // adds mandatory attributes of the jad file in the vector
        String mandatoryJadAttributes = config.string("descriptor.jad.mandatory");
        if (jadFile != null && mandatoryJadAttributes != null
                && mandatoryJadAttributes.length() != 0) {
            String[] attributes = mandatoryJadAttributes.split(",");
            for (int i = 0; i < attributes.length; i++) {
                String attribute = attributes[i].trim();
                if (!inMandatory(attribute, mandatoryJadAttribute)) {
                    attrMap.put(attribute, new MandJadAttrChecker(attribute, jadFile));
                }
            }
        }

        // adds mandatory attributes of the jar manifest in the vector
        String mandatoryJarAttributes = config.string("descriptor.jar.mandatory");
        if (jarFile != null && mandatoryJarAttributes != null
                && mandatoryJarAttributes.length() != 0) {
            String[] attributes = mandatoryJarAttributes.split(",");
            for (int i = 0; i < attributes.length; i++) {
                String attribute = attributes[i].trim();
                if (!inMandatory(attribute, mandatoryJarAttribute)) {
                    attrMap.put(attribute, new MandJarAttrChecker(attribute));
                }
            }
        }

        // adds forbidden attribute of the jad file in the vector
        String forbiddenJadAttributes = config.string("descriptor.jad.forbidden");
        if (jadFile != null && forbiddenJadAttributes != null
                && forbiddenJadAttributes.length() != 0) {
            String[] attributes = forbiddenJadAttributes.split(",");
            for (int i = 0; i < attributes.length; i++) {
                String attribute = attributes[i].trim();
                if (attrMap.get(attribute) == null) {
                    attrMap.put(attribute, new ForbiddenJadAttrChecker(attribute, jadFile));
                }
            }
        }
        // adds forbidden attribute of the jar manifest file in the vector
        String forbiddenJarAttributes = config.string("descriptor.jar.forbidden");
        if (jarFile != null && forbiddenJarAttributes != null
                && forbiddenJarAttributes.length() != 0) {
            String[] attributes = forbiddenJarAttributes.split(",");
            for (int i = 0; i < attributes.length; i++) {
                String attribute = attributes[i].trim();
                if (attrMap.get(attribute) == null) {
                    attrMap.put(attribute, new ForbiddenJarAttrChecker(attribute));
                }
            }
        }
    }

    /**
     * Extract the properties from a manifest (jad file).
     * 
     * @param jadFile
     * @return
     * @throws Alert
     */
    public Properties jadProperties(File jadFile) throws Alert {
        // creates jarProperties and jadProperties objects
        Properties jadProperties = new Properties();
        if (jadFile != null) {
            InputStream stream = null;
            try {
                stream = new FileInputStream(jadFile);
                try {
                    jadProperties.load(stream);
                } finally {
                    stream.close();
                }
            } catch (IOException e) {
                throw Alert.raised(e, "Cannot read JAD property file for header checking");
            }
        }
        return jadProperties;
    }

    /**
     * Extract the properties from a Jar file (real manifest).
     * 
     * @param zf
     * @param manifest
     * @return
     * @throws Alert
     */
    public Properties jarProperties(ZipFile zf, ZipEntry manifest) throws Alert {
        Properties jarProperties = new Properties();
        Manifest mf = null;
        try {
            InputStream jarInputStream = zf.getInputStream(manifest);
            try {
                mf = new Manifest(jarInputStream);
            } finally {
                jarInputStream.close();
            }
        } catch (IOException e) {
            throw Alert.raised(e, "Failed to read from the JAR Manifest");
        }
        Map<String, Attributes> map = mf.getEntries();
        Attributes mainAttributes = mf.getMainAttributes();
        for (Object raw : mainAttributes.keySet()) {
            Name attributeName = (Name) raw; // Attributes is badly programmed
                                             // for 1.6 code.
            String attributeValue = mainAttributes.getValue(attributeName);
            jarProperties.put(attributeName.toString(), attributeValue);
        }
        for (String propertyName : map.keySet()) {
            Attributes attributes = map.get(propertyName);
            for (Object raw : attributes.keySet()) {
                Name attributeName = (Name) raw;
                String attributeValue = attributes.getValue(attributeName);
                jarProperties.put(attributeName.toString(), attributeValue);
            }
        }
        return jarProperties;
    }

    @Override
    public boolean run(String midletName, AppDescription descr, RuleFile ruleFile,
            PrintStream outStream) throws Alert {
        if (!(descr instanceof MidletSuite))
            return true;
        MidletSuite ms = (MidletSuite) descr;
        System.out.println(ms.jarFile);
        File jarFile = ms.jarFile;
        File jadFile = ms.jadFile;
        boolean xmlFormat = config.xmlFormat();
        initForEachRun(outStream, xmlFormat, jadFile, jarFile);
        // get the manifest as an input stream
        try {
            ZipFile zf = new ZipFile(jarFile);
            try {
                ZipEntry manifest = zf.getEntry("META-INF/MANIFEST.MF");
                if (manifest == null) {
                    throw Alert.raised(new IOException(), "Manifest not found");
                }

                manifestConsistency(zf, manifest, xmlFormat);
                if (jadFile != null) {
                    jadConsistency(jadFile, xmlFormat);
                }

                // creates jarProperties and jadProperties objects
                Properties jadProperties = jadProperties(jadFile);
                Properties jarProperties = jarProperties(zf, manifest);

                // verify if the "MIDlet-Jar-RSA-SHA1" is forbidden by a
                // profile's option
                // if yes, consider the MIDlet suite as an untrusted MIDlet
                // suite

                boolean trustedMidlet;
                if (forbiddenTrustedMidlet(jadFile)) {
                    trustedMidlet = false;
                } else {
                    trustedMidlet = isTrustedMidlet(jadProperties, xmlFormat);
                }

                buildAttrMap(jarFile, jadFile);
                setupJarAttr(jarProperties);
                setupJadAttr(jadProperties);
                performCheck(trustedMidlet, xmlFormat);
            } finally {
                zf.close();
            }
        } catch (IOException e) {
            throw Alert.raised(e, "Problem while looking for Manifest.");
        }
        return verdict;
    }

    private boolean forbiddenTrustedMidlet(File jadFile) {
        boolean forbiddenTrustedMidlet = false;
        String jadForbidden = config.string("descriptor.jad.forbidden");
        if (jadFile != null && jadForbidden != null && jadForbidden.length() != 0) {
            String[] attsForbidden = jadForbidden.split(",");
            for (int i = 0; i < attsForbidden.length; i++) {
                String attForbidden = attsForbidden[i];
                if (attForbidden.equals("MIDlet-Jar-RSA-SHA1")) {
                    forbiddenTrustedMidlet = true;
                }
            }
        }
        return forbiddenTrustedMidlet;
    }

    private void performCheck(boolean trustedMidlet, boolean xmlFormat) throws Alert {
        // check the attributes with the value of the trusted boolean
        for (Entry<String, SimpleAttrChecker> e : attrMap.entrySet()) {
            AttributeChecker attribute = e.getValue();
            attribute.check();
            if (trustedMidlet) {
                attribute.checkTrusted();
            }
            checkerTreatment(attribute, xmlFormat);
        }
        // make also the check under the regexp attributes !!!
        for (AttributeChecker attribute : regexpList) {
            attribute.check();
            if (trustedMidlet) {
                attribute.checkTrusted();
            }
            checkerTreatment(attribute, xmlFormat);
        }

        if (verdict && !xmlFormat) {
            ok("No problem detected in the descriptors conformity phase.");
        }
    }

    private void setupJadAttr(Properties jadProperties) {
        // initialize injad attributes
        boolean inList = false;
        for (Map.Entry<Object, Object> e : jadProperties.entrySet()) {
            String key = (String) e.getKey();
            String value = ((String) e.getValue()).trim();
            AttributeChecker attr = (AttributeChecker) attrMap.get(key);
            if (attr != null) {
                attr.inJad(key, value);
                inList = true;
            }
            int i = 0;
            boolean match = false;
            while (i < regexpList.size() && !match) {
                RegexpAttributeChecker rat = (RegexpAttributeChecker) regexpList.get(i);
                match = rat.inJad(key, value);
                i++;
            }
            inList = inList || match;
            if (!inList) {
                if (key.startsWith("MIDlet-") || key.startsWith("MicroEdition-")) {
                    problem("The attribute " + key
                            + " is not a standard attribute but uses a reserved prefix");
                }
            }
        }
    }

    private void setupJarAttr(Properties jarProperties) {
        // initialize injar attributes
        boolean inList = false;
        for (Map.Entry<Object, Object> e : jarProperties.entrySet()) {
            String key = (String) e.getKey();
            String value = (String) e.getValue();
            AttributeChecker attr = (AttributeChecker) attrMap.get(key);
            if (attr != null) {
                attr.inJar(key, value);
                inList = true;
            }
            int i = 0;
            boolean match = false;
            while (i < regexpList.size() && !match) {
                RegexpAttributeChecker rat = (RegexpAttributeChecker) regexpList.get(i);
                match = rat.inJar(key, value);
                i++;
            }
            inList = inList || match;
            if (!inList) {
                if (key.startsWith("MIDlet-") || key.startsWith("MicroEdition-")) {
                    problem("The attribute " + key
                            + " is not a standard attribute but uses a reserved prefix");
                }
            }
        }
    }

    private boolean inMandatory(String attribute, String[] mandAttributes) {
        boolean in = false;
        for (int i = 0; i < mandAttributes.length; i++) {
            String attr = mandAttributes[i];
            if (attribute.equals(attr)) {
                in = true;
            }
        }
        return in;
    }

    private void checkerTreatment(Checker checker, boolean xmlFormat) throws Alert {
        checker.check();
        if (xmlFormat) {
            outStream.println("<checker name=\"" + checker.getAttributeName() + "\">");
            xmlOutput("problem", checker.problemMessage);
            xmlOutput("correct", checker.okMessage);
            xmlOutput("warning", checker.warningMessage);
            outStream.println("</checker>");
        } else {
            if (!checker.getVerdict()) {
                outStream.println(HtmlOutput.header(3, checker.getAttributeName()));
                outStream.println("<ul>");
                for (String key : checker.problemMessage.keySet()) {
                    outStream.println("<li>" + HtmlOutput.color("red", key));
                    Set<String> attributes = checker.problemMessage.get(key);
                    if (attributes.size() > 0) {
                        outStream.println("<ul>");
                        for (String fragment : attributes) {
                            outStream.println("<li>" + HtmlOutput.color("red", fragment) + "</li>");
                        }
                        outStream.println("</ul>");
                    }
                    outStream.println("</li>");
                }
                verdict = false;
            } else {
                if (checker.okMessage.isEmpty() && checker.warningMessage.isEmpty())
                    return;
                outStream.println(HtmlOutput.header(3, checker.getAttributeName()));
                outStream.println("<ul>");
                for (String key : checker.okMessage.keySet()) {
                    outStream.println("<li>" + HtmlOutput.color("green", key));
                    Set<String> attributes = checker.okMessage.get(key);
                    if (attributes.size() > 0) {
                        outStream.println("<ul>");
                        for (String fragment : attributes) {
                            outStream.println("<li>" + HtmlOutput.color("green", fragment)
                                    + "</li>");
                        }
                        outStream.println("</ul>");
                    }
                    outStream.println("</li>");
                }

            }
            // in both verdict cases, display warnings if any.
            for (String key : checker.warningMessage.keySet()) {
                outStream.println("<li>" + HtmlOutput.color("orange", key));
                Set<String> attributes = checker.warningMessage.get(key);
                if (attributes.size() > 0) {
                    outStream.println("<ul>");
                    for (String fragment : attributes) {
                        outStream.println("<li>" + HtmlOutput.color("orange", fragment) + "</li>");
                    }
                    outStream.println("</ul>");
                }
                outStream.println("</li>");
            }
            outStream.println("</ul>");
        }
    }

    private void xmlOutput(String category, HashMap<String, Set<String>> warningMessages) {
        if (warningMessages.size() == 0)
            return;
        XMLStream xmlout = new XMLStream(outStream);
        xmlout.element(category);
        for (Entry<String, Set<String>> entry : warningMessages.entrySet()) {
            String key = entry.getKey();
            xmlout.element("problem");
            xmlout.attribute("description", key);
            for (String message : entry.getValue()) {
                if (message != null && message.length() > 0) {
                    xmlout.element("detail");
                    xmlout.print(message);
                    xmlout.endElement();
                }
            }
            xmlout.endElement();
        }
        xmlout.close();
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

/*
 * $Id: CmdLine.java 2285 2013-12-13 13:07:22Z Pierre Cregut $
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

/**
 * A class to capture the main arguments of a command line, given when
 * invocation of matos from console (command line mode). This is the description
 * of what matos have to do.
 */
public class CmdLine implements Cloneable {

    /* A command line can contains parameters: */
    /** Campaign file. */
    private String campaignFileName = null;

    /** Campaign directory. */
    private String campaignDirName = null;

    /** Step. */
    private Step step = null;

    /** Security profile. */
    private String profileName = null; // spec

    /** Output file. */
    private String outFileName = null;

    /** Css File. */
    private String cssFileName = null;

    /** Log file. */
    private String logFileName = "matos.log"; // DEBUG

    /** help request. */
    protected boolean isHelp = false;

    /** Definition of temp dir. */
    private String temporaryDir = null;

    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * Returns help about command line parameters.
     *
     * @return the string
     */
    public static String help() {

        String help = "";
        help += "MIDLET ANALYSER" + "\n";
        help += "(c) France Telecom Research & Development 2004-2014" + "\n";
        help += "Release " + Release.getTag() + "\n";
        help += "USAGE: matos [<options>] [jarfile | jadfile | apk | checklist] " + "\n";
        help += "  -o <file>         Set <file> as output file (or output directory, if -c option is set). "
                + "\n";
        help += "  -jar <file>       Analyse this JAR <file>, (ignore the one specified in the JAD)."
                + "\n";
        help += "  -jad <file>       Analyse this JAD <file>." + "\n";
        help += "  -c <file>         Run the check-list described in <file> (script mode to launch a whole sequence of analyses)."
                + "\n";
        help += "  -all <directory>  Analyse all JAR, JAD and SWF files which are in <directory>."
                + "\n";
        help += "  -d <def>          Use the security profile named <def>. By default, the profile specifed in the configuration file is used."
                + "\n";
        help += "  -m <midlet>       Do not analyse all midlets of the JAR, but the one called <midlet>. Multiple -m flags may be given."
                + "\n";
        help += "  -css <file>       Use the CSS <file> to control the style and the layout of the HTML result."
                + "\n";
        help += "  -tmp <directory>  Use <directory> as a temporary directory." + "\n";
        help += "  -log <file>       Set <file> as log file." + "\n";
        help += "  -h                Display this help.\n";

        return help;
    }

    /**
     * Builds a string representation of this cmdline, with it's parameters.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "CmdLine:\n" + " campaignFileName: " + campaignFileName + "\n"
                + " campaignDirName: " + campaignDirName + "\n" + " step: ["
                + step.getClass().getName() + "] " + /* step.toString() */"..." + "\n"
                + " profileName: " + profileName + "\n" + " outFileName: " + outFileName + "\n"
                + " cssFileName: " + cssFileName + "\n" + " logFileName: " + logFileName + "\n"
                + " isHelp: " + isHelp + "\n" + " temporaryDir: " + temporaryDir;
    }

    /**
     * Checks for campaign.
     * 
     * @return true, if successful
     */
    public boolean hasCampaign() {
        return !isEmpty(campaignFileName);
    }

    /**
     * Checks for campaign on directory.
     * 
     * @return true, if successful
     */
    public boolean hasCampaignOnDirectory() {
        return !isEmpty(campaignDirName);
    }

    /**
     * Checks for step.
     * 
     * @return true, if successful
     */
    public boolean hasStep() {
        return step != null;
    }

    /**
     * Checks for profile.
     * 
     * @return true, if successful
     */
    public boolean hasProfile() {
        return !isEmpty(profileName);
    }

    /**
     * Checks for css.
     * 
     * @return true, if successful
     */
    public boolean hasCss() {
        return !isEmpty(cssFileName);
    }

    /**
     * Checks for log.
     * 
     * @return true, if successful
     */
    public boolean hasLog() {
        return !isEmpty(logFileName);
    }

    /**
     * Checks for out.
     * 
     * @return true, if successful
     */
    public boolean hasOut() {
        return !isEmpty(outFileName);
    }

    /**
     * Checks for temporary dir.
     * 
     * @return true, if successful
     */
    public boolean hasTemporaryDir() {
        return !isEmpty(temporaryDir);
    }

    /**
     * Checks if is empty.
     *
     * @param s the s
     * @return true, if is empty
     */
    protected boolean isEmpty(String s) {
        return (s == null) || (s.length() == 0);
    }

    /**
     * Builds a CmdLine from options given on the command line (-c, -all).
     *
     * @param configuration the configuration
     * @param argv the argv
     * @return the cmd line
     * @throws Alert the alert
     */
    public static CmdLine parseCampaignCmdLine(Configuration configuration, String[] argv)
            throws Alert {
        CmdLine cmdLine = new CmdLine();
        try {
            for (int i = 0; i < argv.length; i++) {
                String arg = argv[i].trim();
                if (arg.equals("-d"))
                    cmdLine.profileName = argv[++i].trim();
                else if (arg.equals("-h")) {
                    Out.getMain().println(help());
                    cmdLine.isHelp = true;
                } else if (arg.equals("-jar")) {
                    String msg = "ignore option \"-jar " + argv[++i].trim() + "\"";
                    Out.getLog().println(msg);
                    Out.getMain().println(msg);
                } else if (arg.equals("-jad")) {
                    String msg = "ignore option \"-jad " + argv[++i].trim() + "\"";
                    Out.getLog().println(msg);
                    Out.getMain().println(msg);
                } else if (arg.equals("-apk")) {
                    String msg = "ignore option \"-apk " + argv[++i].trim() + "\"";
                    Out.getLog().println(msg);
                    Out.getMain().println(msg);
                } else if (arg.equals("-o"))
                    cmdLine.outFileName = argv[++i].trim();
                else if (arg.equals("-c"))
                    cmdLine.campaignFileName = argv[++i].trim();
                else if (arg.equals("-css"))
                    cmdLine.cssFileName = argv[++i].trim();
                else if (arg.equals("-tmp"))
                    cmdLine.temporaryDir = argv[++i].trim();
                else if (arg.equals("-log"))
                    cmdLine.logFileName = argv[++i].trim();
                else if (arg.equals("-all")) {
                    // campaign on all elements of a directory
                    cmdLine.campaignDirName = argv[++i].trim();
                } else if (arg.length() > 0 && arg.charAt(0) == '-') {
                    Out.getMain().println(help());
                    throw Alert.raised(null, "Unrecognized option : " + arg);
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            Out.getMain().println(help());
            throw Alert.raised(e, "Argument are missing...");
        }
        // retrieve default security profile name
        if (!cmdLine.hasProfile()) {
            cmdLine.profileName = configuration.string(Configuration.DEFAULT_RULES_KEY);
            if (!cmdLine.hasProfile())
                throw Alert
                        .raised(null,
                                "No security profile specified, and no default one defined in your configuration file either!");
        }

        return cmdLine;
    }

    /**
     * Checks if is help.
     * 
     * @return true, if is help
     */
    public boolean isHelp() {
        return isHelp;
    }

    /**
     * Sets the help.
     *
     * @param b the new help
     */
    public void setHelp(boolean b) {
        isHelp = b;
    }

    /**
     * Sets the step.
     *
     * @param step the new step
     */
    public void setStep(Step step) {
        this.step = step;
    }

    /**
     * Gets the css file name.
     *
     * @return the css file name
     */
    public String getCssFileName() {
        return cssFileName;
    }

    /**
     * Sets the out file name.
     *
     * @param out the new out file name
     */
    public void setOutFileName(String out) {
        outFileName = out;
    }

    /**
     * Gets the out file name.
     *
     * @return the out file name
     */
    public String getOutFileName() {
        return outFileName;
    }

    /**
     * Gets the log file name.
     *
     * @return the log file name
     */
    public String getLogFileName() {
        return logFileName;
    }

    /**
     * Gets the step.
     *
     * @return the step
     */
    public Step getStep() {
        return step;
    }

    /**
     * Gets the campaign dir name.
     *
     * @return the campaign dir name
     */
    public String getCampaignDirName() {
        return campaignDirName;
    }
    
    /**
     * Gets the campaign file name.
     *
     * @return the campaign file name
     */
    public String getCampaignFileName() {
        return campaignFileName;
     
    }

    /**
     * Gets the profile name.
     *
     * @return the profile name
     */
    public String getProfileName() {
        return profileName;
    }

    /**
     * Gets the temporary dir.
     *
     * @return the temporary dir
     */
    public String getTemporaryDir() {
        return temporaryDir;
    }
}

/*
 * $Id: CoreCLI.java 2285 2013-12-13 13:07:22Z piac6784 $
 */

package com.orange.matos;

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

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import com.orange.analysis.anasoot.main.AnasootPhase;
import com.orange.analysis.anasoot.main.AndroidPhase;
import com.orange.analysis.anasoot.main.PackageSolver;
import com.orange.analysis.android.AndroidManifestPhase;
import com.orange.analysis.headers.DescrPhase;
import com.orange.analysis.implemchecker.ImplemCheckerPhase;
import com.orange.matos.android.AndroidCoreCLI;
import com.orange.matos.android.AndroidStep;
import com.orange.matos.core.Alert;
import com.orange.matos.core.CmdLine;
import com.orange.matos.core.Configuration;
import com.orange.matos.core.MatosPhase;
import com.orange.matos.core.Out;
import com.orange.matos.core.Step;
import com.orange.matos.java.JavaCoreCLI;
import com.orange.matos.java.JavaStep;
import com.orange.matos.utils.FileUtilities;
import com.orange.matos.utils.HtmlOutput;

/**
 * The generic entry point to the command line version of the program
 */
public class CoreCLI {

    private static final String STYLE_CSS = "/com/orange/matos/style.css";

    /** Constant representing a command line for a single MIDP analysis */
    public final static int JAVA_ANALYSIS_TYPE = 0;

    /** Constant for a multiple step analysis campaign. */
    public final static int CAMPAIGN_ANALYSIS_TYPE = 2;

    /** Constant for a single Android analysis. */
    public final static int ANDROID_ANALYSIS_TYPE = 1;

    /* Analysis modes */
    /**
     * Mode : Campaign is a single file
     */
    static final public int CAMPFILE = 1;

    /**
     * Mode : Campaign is a folder
     */
    static final public int CAMPDIR = 2;

    /**
     * Mode : Campaign is a list of tasks
     */
    static final public int CHECKLIST = 3;

    static final private String OUTPUT_FILE = "output";

    /**
     * Configuration of the tool
     */
    private Configuration configuration;

    /**
     * Initialization phase before execute command lines.
     * 
     */ 
    public CoreCLI(Map<String, MatosPhase[]> phases) throws Alert {
        this(phases, null, null);
    }
    
    /**
     * Initialization phase before execute command lines.
     * 
     * @param phases a Map containing lists of the different phases of the
     *            program. Keys are "java", "android", ..
     */
    
    public CoreCLI(Map<String, MatosPhase[]> phases, String libDirSpec, String tempSpec) throws Alert {

        Configuration configuration = null;
        try {
            configuration = new Configuration(phases, libDirSpec, tempSpec);
        } catch (Exception e) {
            e.printStackTrace(Out.getLog());
            throw Alert.raised(e, "Unable to load configuration ... ");
        }

        this.configuration = configuration;

        // Modify system properties to set HTTP proxy
        // parameters. Note: strangely, it seems that setting
        // proxyHost or/and (?) proxyPort system props implicitly
        // forces proxySet = true.
        setupProxy();

        // Retrieve network time limits (expressed in seconds by user)
        configuration.setHttpTimeout();
    }

    private void setupProxy() {
        try {
            Properties sysProperties = System.getProperties();
            if (configuration.bool(Configuration.PROXY_SET_KEY)) {
                sysProperties.put("proxySet", "true");
                String proxyhost = configuration.string(Configuration.PROXY_HOST_KEY);
                String proxyport = configuration.string(Configuration.PROXY_PORT_KEY);
                if (proxyhost != null) {
                    sysProperties.put("proxyHost", proxyhost);
                    sysProperties.put("http.proxyHost", proxyhost); 
                }
                if (proxyport != null) {
                    sysProperties.put("proxyPort", proxyport);
                    sysProperties.put("http.proxyPort", proxyport);
                }
            } else {
                sysProperties.put("proxySet", "false");
                sysProperties.put("http.proxySet", "false"); // new system
                                                             // property name
            }
        } catch (Exception e) {
            e.printStackTrace(Out.getLog());
        }        
    }

    /**
     * Initialization with a default set of phases.
     * @throws Alert
     */
    public CoreCLI() throws Alert {
        this(defaultPhases());
    }
    
    /**
     * Initialization with a default set of phases.
     * @throws Alert
     */
    public CoreCLI(String libDirSpec, String tempSpec) throws Alert {
        this(defaultPhases(), libDirSpec, tempSpec);
    }
    
    private static Map<String, MatosPhase[]> defaultPhases() {
        Map<String, MatosPhase[]> phases = new HashMap<String, MatosPhase[]>();

        PackageSolver packageSolver = new PackageSolver();

        MatosPhase java_phases[] = {
                new ImplemCheckerPhase(), new DescrPhase(), new AnasootPhase(packageSolver)
        };

        phases.put("java", java_phases);

        MatosPhase android_phases[] = {
                new AndroidManifestPhase(), new AndroidPhase()
        };
        phases.put("android", android_phases);
        return phases;
    }
    

    /**
     * Complete the campaign report currently dumped into the provided print
     * stream. The purpose is 3-fold: complete the campaign global status and
     * end time, add closing tags to ensure HTML is well-formed, and close
     * stream.
     * 
     * @param f Identity of the HTML file to finalize.
     * @param index The associated stream to that file.
     * @param success True if and only if all campaign steps were executed
     *            sucessfully.
     */
    public static void finishCampaignReport(Campaign campaign, File f, PrintStream index,
            boolean success) throws Alert {
        // close HTML file
        index.print("    </table>\n  </body>\n</html>");
        index.close();

        // reload it
        FileInputStream input = null;
        String strContent = null;
        try {
            input = new FileInputStream(f);
            try {
                int len = (int) f.length();
                byte[] content = new byte[len];
                int k = input.read(content);
                strContent = new String(content,0,k,"UTF-8");

            } finally {
                input.close();
            }
        } catch (IOException e) {
            throw Alert.raised(e, "Can't open the result file " + f.getAbsolutePath());
        }
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(2);

        String rates = HtmlOutput.color("green", "PASSED: " + nf.format(campaign.getPassedRate())
                + "%")
                + ", "
                + HtmlOutput.color("red", "FAILED: " + nf.format(campaign.getFailedRate()) + "%")
                + ", "
                + HtmlOutput.color("orange", "SKIPPED: " + nf.format(campaign.getSkippedRate())
                        + "%");

        // add status and end time
        // strContent = strContent.replaceFirst("% NOT FINISHED ! Refresh... %",
        // success ? "All steps were successfully done" :
        // "Some steps were skipped (problems)");
        strContent = strContent.replaceFirst("% NOT FINISHED ! Refresh... %", rates);

        String date = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.MEDIUM, Locale.US)
                .format(new GregorianCalendar().getTime());

        strContent = strContent.replaceFirst("% ENDTIME %", date);

        // save changes
        PrintStream outstream = null;
        try {
            outstream = new PrintStream(new FileOutputStream(f),false,"UTF-8");
        } catch (IOException e) {
            throw Alert.raised(e, "Can't open the result file " + f.getAbsolutePath());
        }
        outstream.print(strContent);
        outstream.close();

    }

    /**
     * Execute step.
     * 
     * @param step the step
     * @throws Alert the alert
     */
    public void executeStep(Step step) throws Alert {
        System.out.println(step);
        boolean xmlFormat = configuration.xmlFormat();

        // Init outstream if needed
        PrintStream outStream = getOutStream(step);
        if (xmlFormat) {
            outStream.println("<?xml version='1.0' encoding='utf-8'?>");
            outStream
                    .println("<!DOCTYPE matres SYSTEM \"http://rd.francetelecom.com/matres.dtd\" []>");
            outStream.println("<root>");
        } else {
 
            printHeaders(outStream, step.getCssUrl());
            outStream.println("<html>\n  <head>\n    <title>Midlet Analyser results</title>\n ");
            outStream
                    .println("  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>");
            if (step.hasCss()) {
                printCss(outStream, step.getCssUrl());
            }
            outStream.println("</head>\n <body>\n");
        }
        try {
            analyseStep(step, outStream);
        } catch (Alert a) {
            step.setMessage(a.getMessage());
            step.setVerdict(Step.SKIPPED);
            Out.getLog().println(a.getMessage());
        } finally {
            if (xmlFormat)
                outStream.println("</root>");
            else
                outStream.println("</body>\n</html>");
            outStream.close();
        }
        configuration.setInAnalysisMode(false);

    }

    /**
     * Computes the stream on which to write the result of the computation. Use
     * what is specified in the step if there is. Otherwise open a file in the
     * temporary directory. The behaviour for Apache mode is more specific (a
     * given name).
     * 
     * @param step the analysis step.
     * @return an output printstream ready.
     * @throws Alert
     */
    private PrintStream getOutStream(Step step) throws Alert {
        PrintStream outStream = null;
        String suffix = configuration.xmlFormat() ? ".xml" : ".html";
        if (step.hasOut()) {
            outStream = getPrintStream(step.getOutFileName());
        } else {
            // verify if the "output.html" file exist
            File file = new File(OUTPUT_FILE + suffix);
            StringBuilder fileName = new StringBuilder(OUTPUT_FILE);
            if (file.exists()) {
                boolean exists = true;
                int i = 1;
                while (exists && i <= 100) {
                    file = new File(OUTPUT_FILE + i + suffix);
                    if (!file.exists()) {
                        exists = false;
                        fileName.append(i);
                    }
                    i++;
                }
            }
            fileName.append(suffix);
            outStream = getPrintStream(fileName.toString());
            step.setOutFileName(file.getAbsolutePath());
            Out.getMain().println("  To view the report, open " + step.getOutFileName());
        }
        return outStream;
    }

    /**
     * Executes analysis specified in the command line.
     * 
     * @param cmdLine the command line to execute
     * @throws Alert
     * @throws Alert
     */
    public void executeCmdLine(CmdLine cmdLine) throws Alert {

        if (cmdLine.hasCampaign()) {
            executeCmdLineCampaign(cmdLine, CAMPFILE);
        } else if (cmdLine.hasCampaignOnDirectory()) {
            executeCmdLineCampaign(cmdLine, CAMPDIR);
        } else if (cmdLine.hasStep()) {
            Step step = cmdLine.getStep();
            executeStep(step);
        } else {
            Out.getLog().println(
                    "Something goes wrong with the command line...\n" + cmdLine.toString());
        }
    }

    /**
     * Launch analysis of the given step.
     * 
     * @param step teh step to analyse
     * @param rules
     * @param outStream the output
     * @throws Alert
     */
    public void analyseStep(Step step, PrintStream outStream) throws Alert {
        if (step instanceof JavaStep) {
            JavaCoreCLI.analyseJavaStep(configuration, (JavaStep) step, outStream);
        } else if (step instanceof AndroidStep) {
            AndroidCoreCLI.analyseAndroidStep(configuration, (AndroidStep) step, outStream);
        } else {
            throw new Alert("Unknowned kind of step: " + step);
        }
    }

    /**
     * Return a print stream to the file absolute path provided. Exit if the
     * given file can't be found.
     * 
     * @param outFilePath The absolute path to a file to print to.
     * @return A print stream to write to the file.
     * @throws UnsupportedEncodingException
     * @throws Alert
     */
    public static PrintStream getPrintStream(String outFilePath) throws Alert {
        PrintStream out = null;
        try {
            out = new PrintStream(new File(outFilePath), "UTF-8");
        } catch (FileNotFoundException e) {
            throw Alert.raised(e, "Can't open output file: " + outFilePath);
        } catch (UnsupportedEncodingException e) {
            throw Alert.raised(e, "Encoding problem");
        }
        return out;
    }

    // -------------------------------------------------------------------------------
    /**
     * The main entry point.
     * 
     * @param argv the arguments given on the command line.
     * @throws Exception
     */
    public void run(String[] argv) throws Alert {
        // Why an init again ?
        // initialize(phases);

        // interpret command line
        CmdLine cmdLine = parse(configuration, argv);

        if (cmdLine != null && !cmdLine.isHelp()) {

            // select the temporary directory if the -tmp option is provided
            if (cmdLine.hasTemporaryDir()) {
                try {
                    String dirName = cmdLine.getTemporaryDir();
                    configuration.setRootTempDir(new File(dirName));
                    if (!configuration.getRootTempDir().isDirectory()) {
                        throw Alert
                                .raised(null,
                                        "The temporary directory specified for MATOS is: "
                                                + dirName
                                                + ". I can't find any directory with that name. Please adapt your -tmp option.");
                    }
                } catch (NullPointerException e) {
                    throw Alert.raised(e,
                            "Can't create temporary directory for Matos. Check your -tmp option.");
                } catch (SecurityException e) {
                    throw Alert.raised(e,
                            "Security problem when checking existence of the temporary directory");
                }
            }

            if (cmdLine.hasLog()) {
                Out.setLog(getPrintStream(cmdLine.getLogFileName()));
            }

            // execute the command line
            executeCmdLine(cmdLine);

        }
    }

    /**
     * Returns a File that represents a destination directory, frome command
     * line parameters
     * 
     * @param cmdLine the parameters
     * @return a File that representes a directory
     */
    private static File initDestDir(CmdLine cmdLine) {
        File destDir = null;
        // ensure that the destination directory is created.
        if (cmdLine.hasOut())
            destDir = new File(cmdLine.getOutFileName());
        else
            destDir = new File("campaign_results");
        if (!destDir.mkdirs()) {
            Out.getLog().println("Cannot create the destination directory " + destDir);
        }
        return destDir;
    }


    /**
     * Execute the command line which contains a campaign file or a directory to
     * analyse.
     * 
     * @param cmdLine The command line.
     * @param mode Type of campaign (campaign file or directory to analyse).
     */
    public void executeCmdLineCampaign(CmdLine cmdLine, int mode) throws Alert {
        File destDir = initDestDir(cmdLine);
        cmdLine.setOutFileName(null);

        File indexFile = new File(destDir, "index.html");
        Campaign campaign = new Campaign(configuration);

        PrintStream index = null;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(indexFile);
            try {
                index = new PrintStream(fos, true, "UTF-8");
                URL cssUrl = cmdLine.getCssFileName() == null ? null :  new File(cmdLine.getCssFileName()).toURI().toURL();
                printHeaders(index, cssUrl);
                index.println(HtmlOutput.header(1, "Midlet Analyser campaign results"));
                index.println("<table width=\"75%\" border=\"1\">");

                File campaignFile = null;
                File campaignDir = null;
                if (mode == CAMPFILE) {
                    // read campaign file and get back a list of new CmdLine(s).
                    campaignFile = new File(cmdLine.getCampaignFileName());
                    campaign.readCampaign(campaignFile);

                    String left = HtmlOutput.cell(HtmlOutput.bold("Campaign name"));
                    String right = HtmlOutput.cell(campaign.getName()
                            + HtmlOutput.br()
                            + "(defined in: "
                            + HtmlOutput.link(cmdLine.getCampaignFileName(), campaignFile.toURI()
                                    .toString()) + ")");
                    index.println(HtmlOutput.row(left + right));
                } else if (mode == CAMPDIR) {
                    campaignDir = new File(cmdLine.getCampaignDirName());
                    Vector<Step> cmdLineVect = initializeCmdLines(campaignDir);
                    campaign.addAll(cmdLineVect);

                    String left = HtmlOutput.cell(HtmlOutput.bold("Directory analysed"));
                    String campPath = campaignDir.getAbsolutePath();
                    String right = HtmlOutput.cell(campPath);
                    index.println(HtmlOutput.row(left + right));
                }

                String left = HtmlOutput.cell(HtmlOutput.bold("Status"));
                String right = HtmlOutput.cell("% NOT FINISHED ! Refresh... %"); // Status
                                                                                 // introduced
                                                                                 // at
                                                                                 // close-time
                index.println(HtmlOutput.row(left + right));

                if (mode == CAMPFILE) {
                    left = HtmlOutput.cell(HtmlOutput.bold("Author"));
                    right = HtmlOutput.cell(campaign.getAuthor());
                    index.println(HtmlOutput.row(left + right));
                }

                left = HtmlOutput.cell(HtmlOutput.bold("Start time"));
                String date = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.MEDIUM,
                        Locale.US).format(new GregorianCalendar().getTime());
                right = HtmlOutput.cell(date);
                index.println(HtmlOutput.row(left + right));

                left = HtmlOutput.cell(HtmlOutput.bold("End time"));
                right = HtmlOutput.cell("% ENDTIME %"); // Introduced at
                                                        // close-time
                index.println(HtmlOutput.row(left + right));

                if (mode == CAMPFILE) {
                    left = HtmlOutput.cell(HtmlOutput.bold("Description"));
                    right = HtmlOutput.cell(campaign.getDescription());
                    index.println(HtmlOutput.row(left + right));
                } else if (mode == CAMPDIR) {
                    left = HtmlOutput.cell(HtmlOutput.bold("Profile"));
                    right = HtmlOutput.cell(cmdLine.getProfileName());
                    index.println(HtmlOutput.row(left + right));
                }

                index.println("</table>");

                index.println("<table width=\"100%\" border=\"1\">");
                String step = HtmlOutput.cell(HtmlOutput.bold("Step"));
                String name = HtmlOutput.cell(HtmlOutput.bold("Name"));
                String profile = "";
                if (mode == CAMPFILE) {
                    profile = HtmlOutput.cell(HtmlOutput.bold("Profile"));
                }
                String inputFiles = HtmlOutput.cell(HtmlOutput.bold("Input file(s)"));
                String anaStatus = HtmlOutput.cell(HtmlOutput.bold("Step status"));
                if (mode == CAMPFILE) {
                    index.println(HtmlOutput.row(step + name + profile + inputFiles + anaStatus));
                } else if (mode == CAMPDIR) {
                    index.println(HtmlOutput.row(step + name + inputFiles + anaStatus));
                }
                index.println(HtmlOutput.br());
                if (mode == CAMPFILE) {
                    Out.getMain().println(
                            "\n* STARTING CAMPAIGN EXECUTION... (" + campaign.size() + " steps)");
                } else if (mode == CAMPDIR) {
                    Out.getMain().println("\n* STARTING CAMPAIGN EXECUTION... ");
                }
                Out.getMain().println("  To view the report, open " + indexFile.getAbsolutePath());

                boolean success = true;
                Out.getLog().println("");
                Out.getLog().println("=== New check-list execution on: " + date);

                // Start analysis (common to all modes)

                for (Step s : campaign) {
                    if (cmdLine.hasCss())
                        s.setCssUrl(cmdLine.getCssFileName() == null ? null : new File(cmdLine.getCssFileName()).toURI().toURL());
                    if (cmdLine.hasProfile())
                        s.setProfileName(cmdLine.getProfileName());
                }
                success = analyseCampaign(campaign, destDir, index); // no
                                                                           // monitor
                                                                           // here

                // Finalise html index file
                if (campaign.size() == 1) {
                    Step theStep = campaign.get(0);
                    File f = new File(theStep.getOutFileName());
                    viewResultFile(f.getAbsolutePath());

                } else {
                    finishCampaignReport(campaign, indexFile, index, success);
                    viewResultFile(indexFile.getAbsolutePath());
                }

                Out.getMain().println("\n* CAMPAIGN EXECUTION FINISHED.");
            } finally {
                fos.close();
            }
        } catch (IOException e) {
            throw Alert.raised(e,
                    "Can't create the results index file " + indexFile.getAbsolutePath());

        }
    }

    /**
     * Execute the analysis of a campaign from a check list.
     * 
     * @param campaign The campaign to analyse.
     * @param destDir The directory for report output.
     * @param mon an analysis monitor to informed about the analysis progression
     *            and status. Can be null.
     */
    public void analyseCheckListCamp(Campaign campaign, File destDir)
            throws Alert {
        if (!destDir.mkdirs()) {
            Out.getLog().println("Cannot create the destination directory " + destDir);
        }

        File indexFile = new File(destDir, "index.html");
        PrintStream index = null;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(indexFile);
            try {
                index = new PrintStream(fos, true,"UTF-8");

                // index.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"");
                // index.println("\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
                String cssFileName = configuration.getCss(); // may be null. In this
                                                        // case, no css used
                URL cssUrl = 
                   cssFileName == null ? null : new File(cssFileName).toURI().toURL();
                printHeaders(index, cssUrl);
               
                index.println(HtmlOutput.header(1, "Check-list results"));
                index.println("<table width=\"75%\" border=\"1\">");
                String left = HtmlOutput.cell(HtmlOutput.bold("Status"));
                String right = HtmlOutput.cell("% NOT FINISHED ! Refresh... %"); // Status
                                                                                 // introduced
                                                                                 // at
                                                                                 // close-time
                index.println(HtmlOutput.row(left + right));
                left = HtmlOutput.cell(HtmlOutput.bold("Start time"));
                String date = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.MEDIUM,
                        Locale.US).format(new GregorianCalendar().getTime());
                right = HtmlOutput.cell(date);
                index.println(HtmlOutput.row(left + right));

                left = HtmlOutput.cell(HtmlOutput.bold("End time"));
                right = HtmlOutput.cell("% ENDTIME %"); // Introduced at
                                                        // close-time
                index.println(HtmlOutput.row(left + right));

                index.println("</table>");
                index.println("<br />");
                index.println("<br />");
                index.println("<table align='center'>");
                index.println("<caption>Statistics</caption>");
                index.println("<tr>");
                index.println("<td>");
                index.println("<img src='statistics.jpeg' />");
                index.println("<td>");
                index.println("<tr>");
                index.println("</table>");

                index.println("<table width=\"100%\" border=\"1\">");
                String step = HtmlOutput.cell(HtmlOutput.bold("Step"));
                String name = HtmlOutput.cell(HtmlOutput.bold("Name"));
                String profile = "";
                profile = HtmlOutput.cell(HtmlOutput.bold("Profile"));

                String inputFiles = HtmlOutput.cell(HtmlOutput.bold("Input file(s)"));
                // String anaStatus =
                // HtmlOutput.cell(HtmlOutput.bold("Step status"));
                String anaVerdict = HtmlOutput.cell(HtmlOutput.bold("Verdict"));
                index.println(HtmlOutput.row(step + name + profile + inputFiles + /*
                                                                                   * anaStatus
                                                                                   * +
                                                                                   */anaVerdict));
                index.println(HtmlOutput.br());
                Out.getMain().println(
                        "\n* STARTING CAMPAIGN EXECUTION... (" + /*
                                                                  * CoreGUI.
                                                                  * javaCheckListTable
                                                                  * .getTable().
                                                                  * getSelectedRowCount
                                                                  * ()
                                                                  */campaign.size() + " steps)");
                Out.getMain().println("  To view the report, open " + indexFile.getAbsolutePath());

                boolean success = true;
                Out.getLog().println("");
                Out.getLog().println("=== New check-list execution on: " + date);
                // success = executeFileCampaign(campaign, null, destDir, index,
                // CHECKLIST);
                success = analyseCampaign(campaign, destDir, index);

                // create file which contains statistic chart
                File statisticFile = new File(destDir, "statistics.jpeg");
                StatisticTool statisticTool = new StatisticTool(campaign);
                statisticTool.createJPEGFile(statisticFile);

                // Finalise html index file
                if (campaign.size() == 1) {
                    Step theStep = campaign.get(0);
                    viewResultFile(theStep.getOutFileName());
                } else {
                    finishCampaignReport(campaign, indexFile, index, success);
                    viewResultFile(indexFile.getAbsolutePath());
                }
                Out.getMain().println("\n* CAMPAIGN EXECUTION FINISHED.");
            } finally {
                fos.close();
            }
        } catch (IOException e) {
            throw Alert.raised(e,
                    "Can't create the results index file " + indexFile.getAbsolutePath());
        }

    }

    private static void viewResultFile(String filename) {
        try {
            Desktop.getDesktop().browse(new File(filename).toURI());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Executes a campaign analysis
     * 
     * @param campaign the campaign to analyse
     * @param destDir the destination directory for reports
     * @param index output stream for index
     * @param mon a monitor to inform about analysis status
     * @return global analysis success (true) or failure (false)
     * @throws Alert
     */
    private boolean analyseCampaign(Campaign campaign, File destDir,
			PrintStream index) throws Alert {
		boolean success = true;

		PrintStream report = null;
		String outFilePath = null;
		String stepName = null;
		String itemName = null;
		String profile = null;
		String inputFiles = null;
		int counter = 1;
		for (Step step : campaign) {
			if (step instanceof JavaStep) {
				JavaStep javaStep = (JavaStep) step;

				// preparation of Java Analysis output
				String jarName = "";
				if (javaStep.getCode().startsWith("http")) {
					jarName = javaStep.getCode()
							.substring(javaStep.getCode().lastIndexOf("/") + 1);
				} else {
					jarName = javaStep.getCode()
							.substring(javaStep.getCode()
									.lastIndexOf(File.separator) + 1);
				}
				if (jarName.lastIndexOf(".") == -1) {
					javaStep.setOutFileName(counter + "-" + jarName + ".html");
				} else {
					javaStep.setOutFileName(counter + "-"
							+ jarName.substring(0, jarName.lastIndexOf("."))
							+ ".html");
				}
				// replace '?' by '-'
				javaStep.setOutFileName(javaStep.getOutFileName().replaceAll("\\?",
						"-"));
				// '/' is not valid in path
				if (javaStep.getOutFileName().indexOf(File.separator) != -1) {
					javaStep.setOutFileName(counter + "-UNKNOWN.html");
				}

				Out.getMain().println(
						"\n* Processing step " + counter + " -> "
								+ javaStep.getOutFileName() + "...");
				outFilePath = destDir.getAbsolutePath() + File.separator
						+ javaStep.getOutFileName();

				// step
				stepName = HtmlOutput.cell(String.valueOf(counter));
				// name of the step
				int indexOfExtension = javaStep.getOutFileName().lastIndexOf(".");
				itemName = javaStep.getOutFileName().substring(0, indexOfExtension);
				itemName = HtmlOutput.cell(HtmlOutput.link(itemName,
						javaStep.getOutFileName()));
				profile = HtmlOutput.cell(javaStep.getProfileName());

				// input file(s)
				Vector<String> vect = initializeJavaCell(javaStep);
				String jadCell = (String) vect.get(0);
				String jarCell = (String) vect.get(1);
				inputFiles = HtmlOutput.cell(jadCell + HtmlOutput.br()
						+ jarCell);
				report = getPrintStream(outFilePath);
				printHeaders(report, javaStep.getCssUrl());
				

				step = javaStep;
			} else if (step instanceof AndroidStep) {
				AndroidStep androidstep = (AndroidStep) step;

				// preparation of Android Analysis output
				String androidFileName = androidstep.getCode()
						.substring(androidstep.getCode()
								.lastIndexOf(File.separator) + 1);
				if (androidFileName.lastIndexOf(".") == -1) {
					androidstep.setOutFileName(counter + "-" + androidFileName
							+ ".html");
				} else {
					androidstep.setOutFileName(counter
							+ "-"
							+ androidFileName.substring(0,
									androidFileName.lastIndexOf(".")) + ".html");
				}
				// replace '?' by '-'
				androidstep.setOutFileName(androidstep.getOutFileName().replaceAll(
						"\\?", "-"));
				// '/' is not valid in path
				if (androidstep.getOutFileName().indexOf(File.separator) != -1) {
					androidstep.setOutFileName(counter + "-UNKNOWN.html");
				}

				Out.getMain().println(
						"\n* Processing step " + counter + " -> "
								+ androidstep.getOutFileName() + "...");
				outFilePath = destDir.getAbsolutePath()
						+ Configuration.fileSeparator + androidstep.getOutFileName();

				// step
				stepName = HtmlOutput.cell(String.valueOf(counter));
				// name of the step
				int indexOfExtension = androidstep.getOutFileName().lastIndexOf(".");
				itemName = androidstep.getOutFileName().substring(0,
						indexOfExtension);
				itemName = HtmlOutput.cell(HtmlOutput.link(itemName,
						androidstep.getOutFileName()));
				// sec profile
				profile = HtmlOutput.cell(androidstep.getProfileName());

				// input file(s)
				Vector<String> vect = initializeAndroidCell(androidstep);
				String androidCell = (String) vect.get(0);
				inputFiles = HtmlOutput.cell(androidCell);
				report = getPrintStream(outFilePath);
				printHeaders(report, androidstep.getCssUrl());
				step = androidstep;
			} else {
				throw new Alert("Unknowned kind of step: " + step);
			}

			String anaVerdict = "";
			String reportURL = "";
			// Notify processing.
			try {
				step.setVerdict(Step.PROCESSING);
				reportURL = step.getOutFileName();
				step.setOutFileName(outFilePath); // put the full path to the
												// report
				analyseStep(step, report);
				String verdict = Step.stringOfVerdict(step.getVerdict());
				String color;
				switch (step.getVerdict()) {
				case Step.FAILED:
					color = "red";
					break;
				case Step.PASSED:
					color = "green";
					break;
				default:
					color = "black";
				}
				// anaStatus =
				// HtmlOutput.cell(HtmlOutput.bold(HtmlOutput.color("black","Done")));
				anaVerdict = HtmlOutput.bold(HtmlOutput.color(color, verdict));
				anaVerdict = HtmlOutput.cell(HtmlOutput.link(anaVerdict,
						reportURL));
				index.println(HtmlOutput.row(stepName + itemName + profile
						+ inputFiles + /* anaStatus */anaVerdict));
				// step.message = "";
			} catch (Alert e) {
				e.printStackTrace(Out.getMain());
				String cause = e.getMessage();
				// anaStatus =
				// HtmlOutput.cell(HtmlOutput.bold(HtmlOutput.color("red","Skipped"))+HtmlOutput.br()+cause);
				// anaVerdict =
				// HtmlOutput.cell(HtmlOutput.bold(HtmlOutput.color("orange","Skipped"))+HtmlOutput.br()+cause);
				anaVerdict = HtmlOutput.bold(HtmlOutput.color("orange",
						"Skipped"));
				anaVerdict = HtmlOutput.cell(HtmlOutput.link(anaVerdict,
						reportURL) + HtmlOutput.br() + cause);

				step.setMessage(cause);
				step.setVerdict(Step.SKIPPED);
				success = false;
				index.println(HtmlOutput.row(stepName + itemName + profile
						+ inputFiles + /* anaStatus */anaVerdict));
				Out.getMain()
						.println("No analysis done... : " + e.getMessage());
			}

			// 3. post analysis output
			report.print("</body>\n</html>");
			report.close();

			configuration.setInAnalysisMode(false);

			// 4. update monitor
			counter++;
		} // end of while


		return success;
	}
    
    private void printCss(PrintStream outStream, URL cssUrl) {
        if (cssUrl == null)
            cssUrl = this.getClass().getResource(STYLE_CSS);
        outStream.println("<style media=\"screen\" type=\"text/css\">");
        try {
            InputStream fis = cssUrl.openStream();
            try {
                byte[] buf = new byte[1024];
                int i = 0;
                while ((i = fis.read(buf)) != -1) {
                    outStream.write(buf, 0, i);
                }
            } finally {
                fis.close();
            }
        } catch (IOException e) {
            Out.getLog().println("Problem writing CSS style");
        }
        outStream.println("</style>");
    }
    
    private void printHeaders(PrintStream outStream, URL cssUrl) {
        
        if (!configuration.xmlFormat()) {
            outStream.println("<html>\n  <head>\n  <title>Midlet Analyser report</title>");
            outStream.println("  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>");
            printCss(outStream, cssUrl);
            outStream.println("</head>\n<body>");
        }
    }

    /**
     * Command line arguments parser.
     * 
     * @param argv the command line.
     * @return a list that contains the names of the midlets to analyse if
     *         specified
     * @throws Alert
     */
    static CmdLine parse(Configuration configuration, String[] argv) throws Alert {

        int analysis_type = -1;
        for (int i = 0; i < argv.length; i++) {
            String arg = argv[i].trim();
            if (arg.equals("-apk") || FileUtilities.checkSuffix(arg, ".apk")) {
                analysis_type = ANDROID_ANALYSIS_TYPE;
                break;
            } else if (arg.equals("-jar") || arg.equals("-jad")
                    || FileUtilities.checkSuffix(arg, ".jad")
                    || FileUtilities.checkSuffix(arg, ".jar")) {
                analysis_type = JAVA_ANALYSIS_TYPE;
                break;
            } else if (arg.equals("-c") || arg.equals("-all")) {
                analysis_type = CAMPAIGN_ANALYSIS_TYPE;
                break;
            } else if (arg.equals("-xml")) {
                configuration.setXmlOutput();
            } else if (arg.equals("-h")) { // help
                Out.getMain().println(CmdLine.help());
                CmdLine cmdLine = new CmdLine();
                cmdLine.setHelp(true);
                return cmdLine;
            }
        }

        CmdLine cmdLine = null;
        switch (analysis_type) {
            case JAVA_ANALYSIS_TYPE:
                cmdLine = JavaStep.parseJavaCmdLine(configuration, argv);
                break;
            case ANDROID_ANALYSIS_TYPE:
                cmdLine = AndroidStep.parseAndroidCmdLine(configuration, argv);
                break;
            case CAMPAIGN_ANALYSIS_TYPE:
                cmdLine = CmdLine.parseCampaignCmdLine(configuration, argv);
                break;
            default: // unknown kind of analysis...
                Out.getMain().println("Unknown kind of analysis. Check arguments.");
                Out.getMain().println(CmdLine.help());
                throw Alert.raised(null, "Unknown kind of analysis. Check arguments.");
        }
        return cmdLine;
    }

    private static Vector<String> initializeJavaCell(JavaStep step) {
        String jadCell = "";
        String jarCell = "";
        Vector<String> vect = new Vector<String>();
        if (step.hasJad()) {
            jadCell = initializeJavaCell(step.getJad(), true);
        } else
            jadCell = "JAD: none provided";
        vect.add(jadCell);
        if (step.hasJar()) {
            jarCell = initializeJavaCell(step.getCode(), false);
        } else
            jarCell = "JAR: retrieved from JAD";
        vect.add(jarCell);
        return vect;
    }

    private static String initializeJavaCell(String fileName, boolean isJad) {
        String label;
        String target;
        if (fileName.startsWith("http:")) {
            label = fileName;
            target = fileName;
        } else {
            label = new File(fileName).getAbsolutePath();
            target = new File(fileName).toURI().toString();
        }
        if (isJad)
            return "JAD: " + HtmlOutput.link(label, target);
        return "JAR: " + HtmlOutput.link(label, target);
    }

    private static Vector<String> initializeAndroidCell(AndroidStep step) {
        Vector<String> vect = new Vector<String>();
        String androidCell = initializeAndroidCell(step.getCode());
        vect.add(androidCell);
        return vect;
    }

    private static String initializeAndroidCell(String fileName) {
        String label;
        String target;
        if (fileName.startsWith("http:")) {
            label = fileName;
            target = fileName;
        } else {
            label = new File(fileName).getAbsolutePath();
            target = new File(fileName).toURI().toString();
        }
        return "Android file: " + HtmlOutput.link(label, target);
    }


    private static Vector<Step> initializeCmdLines(File campaignDir) throws Alert {
        Vector<Step> stepVect = new Vector<Step>();

        FilenameFilter jadFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jad");
            }
        };

        String[] jadFiles = campaignDir.list(jadFilter);

        FilenameFilter jarFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        };

        String[] jarFiles = campaignDir.list(jarFilter);

        // APK files
        FilenameFilter apkFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".apk");
            }
        };

        String[] apkFiles = campaignDir.list(apkFilter);

        if (jadFiles != null)
            Arrays.sort(jadFiles);
        if (jarFiles != null)
            Arrays.sort(jarFiles);
        if (apkFiles != null)
            Arrays.sort(apkFiles);

        Vector<File> jarVect = new Vector<File>();

        if (!(jadFiles == null && jarFiles == null)) {
            if (jadFiles != null) {
                for (int i = 0; i < jadFiles.length; i++) {
                    String jadName = jadFiles[i];
                    File jadFile = new File(campaignDir.getAbsolutePath()
                            + Configuration.fileSeparator + jadName);
                    String jadURI = jadFile.getAbsolutePath();

                    String jarURI = JavaCoreCLI.getJadMIDletJarURLProperty(jadFile);
                    if (jarURI == null) {
                        Out.getLog().println("Cannot find associated jar for JAD " + jadFile);
                        continue;
                    }
                    if (!jarURI.startsWith("http:")) {
                        File jarFile = new File(jadFile.getParent(), jarURI);
                        jarURI = jarFile.getAbsolutePath();
                        jarVect.add(jarFile);
                    }
                    JavaStep javaStep = new JavaStep();
                    javaStep.setJad(jadURI);
                    javaStep.setCode(jarURI);
                    stepVect.add(javaStep);
                }
            }
            if (jarFiles != null) {
                for (int i = 0; i < jarFiles.length; i++) {
                    String jarName = jarFiles[i];
                    File jarFile = new File(campaignDir.getAbsolutePath()
                            + Configuration.fileSeparator + jarName);
                    if (!belong(jarFile, jarVect)) {
                        String jarURI = jarFile.getAbsolutePath();
                        JavaStep javaStep = new JavaStep();
                        javaStep.setCode(jarURI);
                        stepVect.add(javaStep);
                    }
                }
            }
            if (apkFiles != null) {
                String apkURI;
                for (int i = 0; i < apkFiles.length; i++) {
                    String apkName = apkFiles[i];
                    File apkFile = new File(campaignDir.getAbsolutePath()
                            + Configuration.fileSeparator + apkName);
                    apkURI = apkFile.getAbsolutePath();
                    AndroidStep step = new AndroidStep();
                    step.setCode(apkURI);
                    // step.profileName = profileName; // use default ...
                    stepVect.add(step);
                }
            }
        }
        return stepVect;
    }

    /**
     * Tests if a jar file belongs to a vector of jar files.
     * 
     * @param jarFile The jar file.
     * @param jarVect The vector of jar files.
     * @return If the given jar belongs to the given vector.
     */
    public static boolean belong(File jarFile, Vector<File> jarVect) throws Alert {
        if (!jarVect.isEmpty()) {
            try {
                String jar1canonicalPath = jarFile.getCanonicalPath();
                for (File jarFile2 : jarVect) {
                    String jar2canonicalPath = null;
                    try {
                        jar2canonicalPath = jarFile2.getCanonicalPath();
                    } catch (IOException e) {
                        throw Alert.raised(e,
                                "Can't open the jar file " + jarFile2.getAbsolutePath());
                    }
                    if (jar1canonicalPath.equals(jar2canonicalPath)) {
                        return true;
                    }
                }
            } catch (IOException e) {
                throw Alert.raised(e, "Can't open the jar file " + jarFile.getAbsolutePath());
            }
        }
        return false;
    }

    /**
     * The main entry point.
     *
     * @param args the command-line arguments
     */
    public static void main(String args[]) {
        try {
            CoreCLI main = new CoreCLI();
            main.run(args);
        } catch (Alert e) {
            System.err.println("Error during analysis: " + e.getMessage());
        }
    }

    /**
     * Gets the Android profiles.
     *
     * @return the android profiles
     */
    public List<String> getAndroidProfiles() {
        return configuration.getAndroidRules();
    }
    
    /**
     * Gets the MIDP profiles.
     *
     * @return the mIDP profiles
     */
    public List<String> getMIDPProfiles() {
        return configuration.getMIDPRules();
    }

}

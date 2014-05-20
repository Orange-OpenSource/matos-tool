/*
 * $Id: Downloader.java 2279 2013-12-11 14:45:44Z piac6784 $
 */

package com.orange.matos.utils;

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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.HashMap;

import com.orange.matos.core.Alert;
import com.orange.matos.core.Configuration;
import com.orange.matos.core.DownloadParameters;
import com.orange.matos.core.Out;

/**
 * A class to handle an HTTP download session, in an independant thread. The
 * class implements the TimerMonitor interface, to allow for abortion of the
 * session upon timeout. The Timers used just have to call the timeout() method
 * to make the session end. There's a timer to guard the connection phase
 * itself, then a second one to guard the data transfer.
 */
public class Downloader extends Thread implements TimerMonitor {

    private DownloadMonitor monitor;

    private String url;

    private File target;

    private int httpMaxConTime, httpMaxDwnTime;

    private Timer connectionTimer;

    private Timer downloadTimer;

    /**
     * A login to access to the specified URL.
     */
    private String login;

    /**
     * A password to access to the specified URL.
     */
    private String password;

    /**
     * if the connection requires an authentification of the device.
     */
    private String user_agent;

    /** stop condition for this thread */
    private boolean shouldStop = false;

    /**
     * Error message
     */
    private String errorMessage;

    /**
     * Map used to prevent downloading several time the same file (same URI)
     * during an execution of matos
     */
    public static HashMap<String, File> dowloadedFiles = new HashMap<String, File>();

    /**
     * Construct a Downloader to download the file located at the given URL, and
     * save it to the target file passed. The max times allowed for timers are
     * other parameters. The last parameter is the DownloadMonitor to whom
     * report the evolution of the session).
     * 
     * @param url The URL to open
     * @param target The target file where to save the downloaded file
     * @param httpMaxConTime The maximum number of seconds allowed before the
     *            connection timeout
     * @param httpMaxDwnTime The maximum number of seconds allowed before the
     *            transfer timeout
     * @param monitor The download monitor to use to report changes of the
     *            session status
     */
    public Downloader(String url, File target, int httpMaxConTime, int httpMaxDwnTime,
            DownloadMonitor monitor) {
        this.url = url;
        this.target = target;
        this.httpMaxConTime = httpMaxConTime;
        this.httpMaxDwnTime = httpMaxDwnTime;
        this.monitor = monitor;
        connectionTimer = new Timer(httpMaxConTime, this);
        downloadTimer = new Timer(httpMaxDwnTime, this);
    }

    /**
     * Terminates the download session. Dumps the passed message (will be
     * "(stopped)" if null is passed). Also, changes the status of the
     * DownloadMonitor provided at creation, as per the one passed. Finally, the
     * thread is stopped.
     * 
     * @param message The message to dump
     * @param status The status to report to the associated DownloadMonitor.
     *            Valid values are the ones defined in the DownloadMonitor
     *            class.
     * @throws Alert
     */
    public void terminate(String message, int status) {
        if (message == null)
            message = "(stopped)";
        errorMessage = "Download: " + message + "(" + url + ")";
        monitor.setDownloadStatus(status);
        stopAsap();
    }

    /**
     * Method of the TimerMonitor interface. A Timer that expires calls that
     * method, passing a reference to itself. Depending on its identity, the
     * corresponding message and status are passed to terminate().
     * 
     * @param t The Timer object that calls that method, so the Downloader can
     *            identify which timer expired
     */
    @Override
    public void timeout(Timer t) {
        if (t == connectionTimer) {
            terminate("Connection attempt aborted, timeout (" + httpMaxConTime + " s).",
                    DownloadMonitor.DLOAD_TIMEOUT_C);
        } else if (t == downloadTimer) {
            terminate("File transfer aborted, timeout (" + httpMaxDwnTime + " s).",
                    DownloadMonitor.DLOAD_TIMEOUT_T);
        } else {
            String msg = "An unknown timer has expired !";
            Out.getMain().println(msg);
            Out.getLog().println(msg);
            stopAsap();
        }
    }

    /** Stop this thread as soon as possible. */
    public synchronized void stopAsap() {
        shouldStop = true;
        // also stop asap timers
        connectionTimer.stopAsap();
        downloadTimer.stopAsap();
    }

    /**
     * Main entry point of the thread, when started.
     */
    @Override
    public void run() {

        if (isAuthenticate()) {
            Authenticator.setDefault(new MyAuthenticator());
        }

        try {
            if (target == null) { 
                System.err.println("target null");
                return;
            }
            PrintStream outstream = new PrintStream(new FileOutputStream(target),false, "UTF-8");
            try {
                URL urlObj = new URL(url);

                if ((user_agent != null) && (user_agent.trim().length() > 0)) {
                    if (user_agent.indexOf('\n') > 0) {
                        Out.getLog()
                                .println(
                                        "INVALID user_agent: '"
                                                + user_agent
                                                + "' : contains a newline character --> No user agent used");
                    } else {

                        System.getProperties().put("http.agent", user_agent);
                    }
                }

                HttpURLConnection huc = (HttpURLConnection) urlObj.openConnection();
                connectionTimer.start();
                huc.connect();
                InputStream in = null;
                if (!shouldStop) { // no timeout occurs during connection
                    connectionTimer.stopAsap();
                    int code = huc.getResponseCode();
                    if (code != 200) {
                        // huc.disconnect();
                        if (code == 404 || code == 401 || code == 502) {
                            terminate("Connection failed. Server returned HTTP code " + code + ".",
                                    DownloadMonitor.DLOAD_FAILED_NO_NEW_ATTEMPT);
                        } else {
                            terminate("Connection failed. Server returned HTTP code " + code + ".",
                                    DownloadMonitor.DLOAD_FAILED);
                        }
                    } else {
                        // Get an input stream for reading
                        in = huc.getInputStream();
                    }
                }
                if (in != null) {
                    try {

                        BufferedInputStream bufIn = new BufferedInputStream(in);

                        // Read bytes until end of stream, and write them to
                        // local file
                        downloadTimer.start();
                        int data = bufIn.read();
                        while ((!shouldStop) && (data != -1)) {
                            outstream.write(data);
                            data = bufIn.read();
                        }
                        if (!shouldStop) { // no timeout occurs during download
                            downloadTimer.stopAsap();
                            terminate("Completed.", DownloadMonitor.DLOAD_SUCCESS);
                        }

                    } finally {
                        in.close();
                    }
                }
                huc.disconnect();
            } finally {
                outstream.close();
            }
        } catch (MalformedURLException e) {
            terminate("Malformed URL", DownloadMonitor.DLOAD_FAILED);
        } catch (FileNotFoundException e) {
            terminate("Can't create file:" + (target == null ? "-" : target.getAbsolutePath()),
                    DownloadMonitor.DLOAD_FAILED);
        } catch (IOException e) {
            terminate("I/O Error - " + e, DownloadMonitor.DLOAD_FAILED);
        } catch (SecurityException e) {
            terminate("Can't create file (security issue):" + target.getAbsolutePath(),
                    DownloadMonitor.DLOAD_FAILED);
        } finally { // do it what ever exception occurs or not
            System.getProperties().remove("http.agent");
        }
    }

    /**
     * This class allows making a connection with an authentication (login and
     * password).
     * 
     * @author apenault
     */
    public class MyAuthenticator extends Authenticator {
        // This method is called when a password-protected URL is accessed
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(login, password.toCharArray());
        }
    }

    /**
     * To know if the connection must be authenticated by a login and a password
     * 
     * @return true if the connection to the given URL requires a login and a
     *         password
     */
    public boolean isAuthenticate() {
        return (login != null && password != null);
    }

    /**
     * Sets the value of User-Agent to use for the HTTP connection.
     * 
     * @param user_agent
     */
    public void setUser_agent(String user_agent) {
        this.user_agent = user_agent;
    }

    /**
     * Set user name for download
     * 
     * @param login
     */
    public void setLogin(String login) {
        this.login = login;
    }

    /**
     * Set password for download
     * 
     * @param password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Get error message for failed download.
     * 
     * @return
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Download the file located at the given URL, and write it to the given
     * target local directory. Return a File object on that file. Exit if there
     * is any problem while downloading, or to store the downloaded file.
     * 
     * @param location the URL of the file to fetch.
     * @param targetDir the directory where to save the downloaded file.
     * @param isJarFile true if the file to fetch is a JAR, false if it is a
     *            JAD. Used to target the appropriate temporary file.
     * @param login
     * @param password
     * @param user_agent
     * @return the downloaded file.
     * @throws Alert
     */
    static public File httpDownload(Configuration config, String location, String baseName,
            String ext, DownloadParameters dp) throws Alert {
        String login = dp.getLogin();
        String password = dp.getPassword();
        String userAgent = dp.getUserAgent();
        File f = (File) dowloadedFiles.get(location);
        File targetDir = config.getRootTempDir();
        if (f == null) {

            Downloader downloader = null;
            DownloadMonitor downloadMonitor;
            int attemptsDone = 0;
            File target = getTarget(baseName, targetDir, ext);
            // launch a download thread for that file, providing it with
            // a monitor to keep an eye on the status of the operation.
            boolean done = false;
            int maxAttempts = 3;

            // used to implement a strategy a la windows : in case of failure,
            // timeout is double for each retry (until maxAttempts reached).
            int httpConTimeOut = config.getHttpMaxConTime();
            int httpDwnTime = config.getHttpMaxDwnTime();
            while (!done && (attemptsDone < maxAttempts)) {
                downloadMonitor = new DownloadMonitor();
                downloader = new Downloader(location, target, httpConTimeOut/* httpMaxConTime */,
                        httpDwnTime/* httpMaxDwnTime */, downloadMonitor);
                if (login != null && password != null && login.length() != 0) {
                    downloader.setLogin(login);
                    downloader.setPassword(password);
                }
                if ((userAgent != null) && (userAgent.trim().length() > 0)) {
                    downloader.setUser_agent(userAgent);
                }
                downloader.start();
                downloadMonitor.waitForEnd();
                int status = downloadMonitor.getDownloadStatus();
                if (status == DownloadMonitor.DLOAD_SUCCESS) {
                    done = true;
                    attemptsDone++;
                } else {
                    if (status == DownloadMonitor.DLOAD_TIMEOUT_C) {
                        httpConTimeOut = httpConTimeOut * 2;
                        Out.getLog().println("Connexion timeout increased");
                        attemptsDone++;
                    } else if (status == DownloadMonitor.DLOAD_TIMEOUT_T) {
                        httpDwnTime = httpDwnTime * 2;
                        Out.getLog().println("Download timeout increased");
                        attemptsDone++;
                    } else if (status == DownloadMonitor.DLOAD_FAILED) {
                        attemptsDone++;
                    } else if (status == DownloadMonitor.DLOAD_FAILED_NO_NEW_ATTEMPT) {
                        attemptsDone = maxAttempts;
                    }
                    // in all cases:
                    downloader.stopAsap();
                }
            }
            if (done) {
                f = target;
                dowloadedFiles.put(location, f);
            } else {
                f = null;
                throw Alert.raised(null, downloader.getErrorMessage());
            }
        }
        return f;

    }

    private static File getTarget(String baseName, File targetDir, String ext) {
        String tempFileName = baseName;
        String targetFileName = "" + tempFileName;
        targetFileName += "." + ext;
        File target = new File(targetDir, targetFileName);
        int i = 1;
        while (target.exists()) {
            targetFileName = "" + i + tempFileName;
            targetFileName += "." + ext;
            target = new File(targetDir, targetFileName);
            i++;
        }
        target.deleteOnExit();
        return target;
    }

    /**
     * Returns the file at the given URI. Check that we do not get an html file
     * (usually a sign of an authentication problem).
     * 
     * @param configuration full configuration
     * @param uri local or distant uri
     * @param login credentials if distant
     * @param password credentials if distant
     * @param user_agent credentials if distant
     * @param tmpBase base for the local file
     * @param ext extension for the local file.
     * @return a local file or null
     * @throws Alert raised if any problem
     */
    public static File toLocalFile(Configuration configuration, String uri, DownloadParameters dp,
            String tmpBase, String ext) throws Alert {
        File localFile = null;
        if (uri.startsWith("http:"))
            localFile = httpDownload(configuration, uri, tmpBase, ext, dp);
        else
            localFile = new File(uri);

        if ((localFile != null) && (!localFile.exists())) {
            System.out.println(localFile);
            localFile = null;
            return null;
        }

        try {
            boolean isFileValid = !FileUtilities.checkForWebPage(localFile);
            if (!isFileValid) {
                String message = "The file '"
                        + uri
                        + "' is invalid: it looks like an HTML file! Please check the URL and/or login & password";
                throw Alert.raised(null, message);
            }
        } catch (FileNotFoundException fex) {
            throw Alert.raised(fex, "'" + uri + "' file does not exists");
        } catch (IOException ioex) {
            throw Alert.raised(ioex);
        }

        return localFile;

    }
}

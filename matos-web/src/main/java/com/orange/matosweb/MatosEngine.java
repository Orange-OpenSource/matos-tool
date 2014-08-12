
package com.orange.matosweb;

/*
 * #%L
 * Matos
 * $Id:$
 * $HeadURL:$
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
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

import com.orange.matos.CoreCLI;
import com.orange.matos.android.AndroidStep;
import com.orange.matos.core.Alert;
import com.orange.matos.core.Out;
import com.orange.matos.core.Step;
import com.orange.matos.java.JavaStep;

/**
 * @author Pierre Cregut This is the engine servlet. It is where analysis are
 *         launched. One at a time. It must be synchronized with others and
 *         should probably use some kind of message queue to avoid
 *         synchronization issues.
 */
@ManagedBean(name = "matos_engine",eager = true)
@ApplicationScoped
public class MatosEngine implements Runnable, Serializable {

    private static final long serialVersionUID = 1L;

    final private Queue<MatosStep> queue;

    private transient CoreCLI matos;
    
    File privateFolder;

    final transient Thread thread;

    /**
     * Launch a new and usually unique Matos engine.
     */
    public MatosEngine() {
        queue = new LinkedList<MatosStep>();
        thread = new Thread(this);
        thread.start();
    }

    /**
     * Adds a new item to the engine work queue.
     * 
     * @param step
     */
    public void addWork(MatosStep step) {
        synchronized (queue) {
            queue.add(step);
            step.setStatus(MatosStatus.SCHEDULED);
            queue.notify();
        }
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        String tmpDir = System.getProperty("matos.tmpdir");
        try {
            while (true) {
                MatosStep step;
                synchronized (queue) {
                    while (queue.isEmpty()) {
                        queue.wait();
                    }
                    step = queue.poll();
                }

                try {
                    step.setStatus(MatosStatus.COMPUTING);
                    Step matosStep = toInternalFormat(step);
                    MatosStatus status = MatosStatus.TODO;
                    if (matosStep == null) {
                        status = MatosStatus.SKIPPED;
                    } else {
                        Out.setLog(CoreCLI.getPrintStream(new File(tmpDir, "log.txt").getAbsolutePath()));
                        step.setAnalysisDate(System.currentTimeMillis());
                        getMatos().executeStep(matosStep);
                        step.setScore(matosStep.getScore());

                        switch (matosStep.getVerdict()) {
                            case Step.FAILED:
                                status = MatosStatus.FAILED;
                                break;
                            case Step.PASSED:
                                status = MatosStatus.PASSED;
                                break;
                            default:
                                
                                status = MatosStatus.SKIPPED;
                        }
                    }
                    step.setStatus(status);
                } catch (Alert e) {
                    step.setStatus(MatosStatus.SKIPPED);
                    e.printStackTrace();
                }
                                
            }
        } catch (InterruptedException e) {

        }
    }

    private Step toInternalFormat(MatosStep step) {
        String jad = null;
        Step result;
        switch (step.getKind()) {
            case APK_FILE:
                AndroidStep androidStep = new AndroidStep();

                result = androidStep;
                break;
            case MIDP_JAR_JAD:
                jad = step.getDescriptor().getAbsolutePath();
            case MIDP_JAR:
                JavaStep javaStep = new JavaStep();
                javaStep.setJad(jad);
                result = javaStep;
                break;
            default:
                return null;
        }
        File codeFile = step.getCode();
        File outFile = step.getOutFile();
        String profile = step.getProfile();
        if (codeFile == null || outFile == null || profile == null) return null;
        result.setCode(codeFile.getAbsolutePath());
        result.setProfileName(profile);
        result.setOutFileName(outFile.getAbsolutePath());
        return result;
    }

    /**
     * Global folder where all the sessions are stored.
     * @return
     */
    public File getFolder() {
        if (privateFolder == null) {
            String tmpDir = System.getProperty("matos.tmpdir");
            privateFolder = new File(tmpDir, "matos");
            privateFolder.delete();
            if (! privateFolder.mkdir()) return null;
        }
        return privateFolder;
    }

    /**
     * List of names of profiles suitable for Android
     * @return
     */
    public List<String> getAndroidProfiles() {
        return getMatos().getAndroidProfiles();
    }
    
    /**
     * List of names of profiles suitable for MIDP
     * @return
     */
    public List<String> getMIDPProfiles() {
        return getMatos().getMIDPProfiles();
    }

    /**
     * Gets the matos.
     *
     * @return the matos
     */
    public CoreCLI getMatos() {
        if (matos == null) {
            String tmpDir = System.getProperty("matos.tmpdir");
            String libDir = System.getProperty("matos.libdir");
            try {
                matos = new CoreCLI(libDir, tmpDir);
             } catch (Alert e) {
                 matos = null;
                 e.printStackTrace();
             }
        }
        return matos;
    }

}

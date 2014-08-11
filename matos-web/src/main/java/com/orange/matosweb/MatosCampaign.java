
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.myfaces.custom.fileupload.UploadedFile;

/**
 * The Class MatosCampaign.
 * 
 * @author piac6784 A campaign is analyzed as a full block.
 */
public class MatosCampaign implements Serializable {

    /**
     * Date format specification
     */
    public static final String DATE_FORMAT_STRING = "dd/MM/yyyy '-' HH:mm:ss";

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    private static final String BACKUP = "campaing.ser";

    /** The date format. */
    SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_STRING);

    /** The selected. */
    private boolean selected;

    /** The private folder. */
    final private File privateFolder;

    /** The create date. */
    final private Date createDate = new Date();

    /** The modified date. */
    private long modifiedDate;

    /** The apk. */
    private UploadedFile apk;

    /** The jar. */
    private UploadedFile jar;

    /** The jad. */
    private UploadedFile jad;

    /** The midp profile. */
    private String midpProfile;

    /** The android profile. */
    private String androidProfile;

    /** The name. */
    private final String name;

    private final MatosEngine engine;

    /** message for the end-user */
    private String message;

    /**
     * Instantiates a new matos campaign.
     * 
     * @param name the name
     * @param folderBase the folder base
     * @param engine
     */
    public MatosCampaign(String name, File folderBase, MatosEngine engine) throws IOException {
        this.name = name;
        this.engine = engine;
        File folder;
        folder = new File(folderBase, name);
        if (!folder.exists()) {
            if (!folder.mkdir()) throw new IOException("Cannot create folder");
        }
        privateFolder = folder;
        restore();
    }

    /**
     * Read back steps.
     */
    @SuppressWarnings("unchecked")
    private void restore() {
        try {
            File file = new File(privateFolder, BACKUP);
            if (!file.exists())
                return;
            FileInputStream fis = new FileInputStream(file);

            try {
                ObjectInputStream ois = new ObjectInputStream(fis);
                try {
                    steps.addAll((List<MatosStep>) ois.readObject());
                } finally {
                    ois.close();
                }
            } finally {
                fis.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Backup the steps.
     */
    private void backup() {
        try {
            FileOutputStream fos = new FileOutputStream(new File(privateFolder, BACKUP));
            try {
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                try {
                    oos.writeObject(steps);
                    oos.flush();
                } finally {
                    oos.close();
                }
            } finally {
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** The steps. */
    private final List<MatosStep> steps = new ArrayList<MatosStep>();

    /**
     * Adds the android step.
     * 
     * @return the string
     */
    public String addAndroidStep() {
        message = null;
        File apkFile = upload(apk);
        if (apkFile == null) {
            message = "Cannot find APK file";
            return null;
        }
        MatosStep step = new MatosStep(MatosStepKind.APK_FILE, androidProfile, apkFile, null);
        steps.add(step);
        backup();
        return null;
    }

    /**
     * Adds the midp step.
     * 
     * @return the string
     */
    public String addMIDPStep() {
        message = null;
        File codeFile;
        File descrFile = null;
        MatosStepKind kind;
        String profile;

        File jarFile = upload(jar);
        if (jarFile == null) {
            message = "Cannot find JAR file";
            return null;
        }
        codeFile = upload(jar);
        if (jad == null) {
            kind = MatosStepKind.MIDP_JAR;
        } else {
            File jadFile = upload(jad);
            if (jadFile == null) {
                message = "Cannot find JAD file";
                return null;
            }
            kind = MatosStepKind.MIDP_JAR_JAD;
            descrFile = jadFile;
        }
        profile = midpProfile;
        MatosStep step = new MatosStep(kind, profile, codeFile, descrFile);
        steps.add(step);
        backup();
        return null;
    }

    /**
     * Analyze all.
     * 
     * @return null : no change to page
     */
    public String analyzeAll() {
        for (MatosStep step : steps) {
            engine.addWork(step);
        }
        return null;
    }

    /**
     * Analyze steps marked as selected.
     * 
     * @return null no change to page.
     */
    public String analyzeSelected() {
        for (MatosStep step : steps) {
        	if (step.isSelected()) {
        		engine.addWork(step);
        	}
        }
        return null;
    }

    /**
     * Action analyzing a step.
     * 
     * @return null : no change to page
     */
    public String analyze(MatosStep step) {
        engine.addWork(step);
        return null;
    }

    /**
     * Upload.
     * 
     * @param file the file
     * @return the file
     */
    private File upload(UploadedFile file) {
        if (file == null)
            return null;
        String name = FilenameUtils.getName(file.getName());
        File tempFile = new File(privateFolder, name);
        try {
            InputStream is = file.getInputStream();
            try {
                copyTo(is, tempFile);
            } finally {
                is.close();
            }
        } catch (IOException e) {
            tempFile = null;
        }
        return tempFile;
    }

    /**
     * Copy to.
     * 
     * @param is the is
     * @param dest the dest
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static void copyTo(InputStream is, File dest) throws IOException {
        OutputStream os = new FileOutputStream(dest);
        try {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            os.close();
        }
    }

    /**
     * Removes the selected steps.
     * 
     * @return the string
     */
    public String removeSelectedSteps() {
        message = null;
        Iterator<MatosStep> iterator = steps.iterator();
        int c = 0;
        while (iterator.hasNext()) {
            MatosStep step = iterator.next();
            if (step.isSelected()) {
                step.delete();
                iterator.remove();
                c++;
            }
        }
        message = "Removed " + c + " steps.";
        backup();
        return null;
    }

    /**
     * Leave the current page.
     * 
     * @return
     */
    public String leave() {
        message = null;
        backup();
        return "index.xhtml?faces-redirect=true";
    }

    /**
     * Checks if is selected.
     * 
     * @return true, if is selected
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Sets the selected.
     * 
     * @param selected the new selected
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * Gets the private folder.
     * 
     * @return the private folder
     */
    public File getPrivateFolder() {
        return privateFolder;
    }

    /**
     * Gets the printable create date.
     * 
     * @return the printable create date
     */
    public String getPrintableCreateDate() {
        return dateFormat.format(createDate);
    }

    /**
     * Gets the modified date.
     * 
     * @return the modified date
     */
    public long getModifiedDate() {
        return modifiedDate;
    }

    /**
     * Sets the modified date.
     * 
     * @param modifiedDate the new modified date
     */
    public void setModifiedDate(long modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    /**
     * Gets the steps.
     * 
     * @return the steps
     */
    public List<MatosStep> getSteps() {
        return steps;
    }

    /**
     * Gets the midp profile.
     * 
     * @return the midp profile
     */
    public String getMidpProfile() {
        return midpProfile;
    }

    /**
     * Sets the midp profile.
     * 
     * @param midpProfile the new midp profile
     */
    public void setMidpProfile(String midpProfile) {
        this.midpProfile = midpProfile;
    }

    /**
     * Gets the android profile.
     * 
     * @return the android profile
     */
    public String getAndroidProfile() {
        return androidProfile;
    }

    /**
     * Sets the android profile.
     * 
     * @param androidProfile the new android profile
     */
    public void setAndroidProfile(String androidProfile) {
        this.androidProfile = androidProfile;
    }

    /**
     * Gets the apk.
     * 
     * @return the apk
     */
    public UploadedFile getApk() {
        return apk;
    }

    /**
     * Sets the apk.
     * 
     * @param apk the new apk
     */
    public void setApk(UploadedFile apk) {
        this.apk = apk;
    }

    /**
     * Gets the jar.
     * 
     * @return the jar
     */
    public UploadedFile getJar() {
        return jar;
    }

    /**
     * Sets the jar.
     * 
     * @param jar the new jar
     */
    public void setJar(UploadedFile jar) {
        this.jar = jar;
    }

    /**
     * Gets the jad.
     * 
     * @return the jad
     */
    public UploadedFile getJad() {
        return jad;
    }

    /**
     * Sets the jad.
     * 
     * @param jad the new jad
     */
    public void setJad(UploadedFile jad) {
        this.jad = jad;
    }

    /**
     * Gets the name.
     * 
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the message.
     * 
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the message.
     * 
     * @param message the new message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Delete the campaign.
     */
    public boolean delete() {
        for (MatosStep step : steps) {
            step.delete();
        }
        boolean ok = true;
        File[] content = privateFolder.listFiles();
        if (content != null) {
            for (File file : content) {
                ok &= file.delete();
            }
        }
        ok &= privateFolder.delete();
        return ok;
    }
}

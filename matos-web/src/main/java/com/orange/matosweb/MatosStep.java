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
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * The Class MatosStep.
 *
 * @author piac6784
 * A step defines what must be analyzed and with which profile.
 */
public class MatosStep implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private static final String OUT_SUFFIX = ".html";
    
    final private SimpleDateFormat dateFormatter = new SimpleDateFormat(MatosCampaign.DATE_FORMAT_STRING);

    /** Is this step selected. */
	private boolean selected;
	
    /** The kind of application to analyze */
    final private MatosStepKind kind;
    
	/** The code. */
	final private File code;
	
	/** The descriptor. */
	final private File descriptor;
	
	/** The file where the result is stored. */
	final private File outFile;
	
	/** The analysis profile. */
	final private String profile;
	
	/** The analysis date. */
	private long analysisDate = -1;
	
	/** The score. */
	private int score = -1;
	
	/** The status. */
	private MatosStatus status = MatosStatus.TODO;
	
	/**
	 * Creates a step to analyse
	 * @param kind the kind of the code to analyze (MIDP or Android)
	 * @param profile the profile of the analysis
	 * @param codeFile the code to analyze as a file
	 * @param descrFile an optional JAD descriptor file (null ortherwise)
	 */
	public MatosStep(MatosStepKind kind, String profile, File codeFile, File descrFile) {
	    this.kind = kind;
	    this.profile = profile;
	    this.code = codeFile;
	    this.outFile = computeOutPath(code);
	    this.descriptor = descrFile;
    }

    /**
	 * Gets the code.
	 *
	 * @return the code
	 */
	public File getCode() {
		return code;
	}
	

    /**
     * Gets the descriptor.
     *
     * @return the descriptor
     */
    public  synchronized File getDescriptor() {
        return descriptor;
    }
    
    /**
     * Gets the descriptor name.
     *
     * @return the descriptor name
     */
    public String getDescriptorName() {
        return descriptor == null ? null : descriptor.getName();
    }

    /**
     * Gets the profile.
     *
     * @return the profile
     */
    public  String getProfile() {
        return profile;
    }

    /**
     * Gets the kind of analyzed application.
     *
     * @return the kind
     */
    public  MatosStepKind getKind() {
        return kind;
    }

    /**
     * Gets the result file name.
     *
     * @return the out file
     */
    public  File getOutFile() {
        return outFile;
    }

    /**
     * Gets the file name of the result file.
     *
     * @return the out file name
     */
    public  synchronized String getOutFileName() {
        String name = outFile.getName();
        File parentFile = outFile.getParentFile();
        if (parentFile == null) return null;
        String sessionName = parentFile.getName();
        return sessionName + "/" + name;
    }

	/**
	 * Gets the code name.
	 *
	 * @return the code name
	 */
	public synchronized String getCodeName() {
	    return code == null ? null : code.getName();
	}

	/**
	 * Computes the path of the outFile from the name of the code File.
	 * @param code
	 * @return
	 */
	private  File computeOutPath(File code) {
	    if (code == null) return null;
	    File parent = code.getParentFile();
	    String name = code.getName();
	    int suffix = name.lastIndexOf('.');
	    String core = suffix == -1 ? name : name.substring(0,suffix);
        return new File(parent, core + OUT_SUFFIX);
    }
	
	/**
	 * Gets the analysis date.
	 *
	 * @return the analysis date
	 */
	public  synchronized long getAnalysisDate() {
		return analysisDate;
	}
	
	/**
	 * Gets a printable analysis date.
	 *
	 * @return the printable analysis date
	 */
	public synchronized String getPrintableAnalysisDate() {
	    return analysisDate < 0 ? "-" : dateFormatter.format(new Date(analysisDate));
	}
	/**
	 * Sets the analysis date.
	 *
	 * @param analysisDate the new analysis date
	 */
	public  synchronized void setAnalysisDate(long analysisDate) {
		this.analysisDate = analysisDate;
	}
	
	
	/**
	 * Gets the score.
	 *
	 * @return the score
	 */
	public  synchronized int getScore() {
		return score;
	}
	
	/**
	 * Check if there is a valid score to display
	 * @return
	 */
	public  synchronized boolean getHasScore() {
	    return score >= 0;
	}
	
	/**
	 * Sets the score.
	 *
	 * @param score the new score
	 */
	public  synchronized void setScore(int score) {
		this.score = score;
	}
	
	/**
	 * Gets the status.
	 *
	 * @return the status
	 */
	public synchronized MatosStatus getStatus() {
		return status;
	}
	
	/**
	 * Sets the status.
	 *
	 * @param status the new status
	 */
	public synchronized void setStatus(MatosStatus status) {
	    // If the step is deleted then this is the engine trying to push an update on a removed state.
	    // We delete its file again and do not change the status of the step.
	    if (this.status == MatosStatus.DELETED) delete();
	    else this.status = status;
	}

	/**
	 * Checks if is selected.
	 *
	 * @return true, if is selected
	 */
	public  synchronized boolean isSelected() {
		return selected;
	}

	/**
	 * Sets the selected.
	 *
	 * @param selected the new selected
	 */
	public synchronized void setSelected(boolean selected) {
		this.selected = selected;
	}

    /**
     * Deletes a step. This is a user action.
     * @return
     */
    public synchronized boolean delete() {
        setStatus(MatosStatus.DELETED);
        boolean ok = true;
        if (outFile != null && outFile.exists()) ok &= outFile.delete();
        if (code != null && code.exists()) ok &= code.delete();
        if (descriptor != null && descriptor.exists()) ok &= descriptor.delete();
        return ok;
    }
    
}


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
import java.io.FileFilter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

/**
 * The Class MatosSession.
 *
 * @author Pierre Cregut A session represents all the analysis for a given user.
 */
@ManagedBean(name = "matos_session")
@SessionScoped
public class MatosSession implements Serializable {
    
    private static final Logger logger = Logger.getLogger (MatosSession.class.getName());
       
    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /** The user owning the session. */
    private String user;
    
    /** The current campaign name. */
    private String campaignName;
    
    /** The private folder. */
    private File privateFolder;
    
    /** The repository. */
    @ManagedProperty("#{matos_engine}")
    private MatosEngine engine;
    
    /** The campaigns. */
    private List<MatosCampaign> campaigns;

    /** The current. */
    private MatosCampaign current;
    
    /** Message to display. */
    private String message;
    
    /** Style of message to display. */
    private String messageStyle;

    /**
     * Instantiates a new matos session.
     */
    public MatosSession () {
        System.out.println("*** CREATE MATOS SESSION ****");
    }
    
    /**
     * ReadBack campaigns from the folders.
     */
    private void restore() {
        if (campaigns != null) return;
        campaigns = new ArrayList<MatosCampaign>();
        File privateFolder = getPrivateFolder();
        if (privateFolder == null) return;
        File [] dirs = getPrivateFolder().listFiles(new FileFilter() {            
            @Override
            public boolean accept(File arg0) { return arg0.isDirectory(); }
        });
        if (dirs == null) return;
        for(File dir: dirs) {
            try {
                MatosCampaign campaign = new MatosCampaign(dir.getName(), privateFolder, getEngine());
                campaigns.add(campaign);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Cannot create a directory for campaign", e);
            }
        }
    }

    /**
     * Removes the selected campaigns.
     *
     * @return the string
     */
    public String removeSelectedCampaigns() {
        Iterator<MatosCampaign> iterator = campaigns.iterator();
        while (iterator.hasNext()) {
            MatosCampaign campaign = iterator.next();
            if (campaign.isSelected()) {
                campaign.delete();
                iterator.remove();
            }
                
        }
        return null;
    }

    /**
     * Adds a new campaign.
     *
     * @return the string
     */
    public String addNewCampaign() {
        String name = generateName();
        try {
            MatosCampaign campaign = new MatosCampaign(name, getPrivateFolder(), engine);
            campaigns.add(campaign);
        } catch (IOException e)  {
            logger.log(Level.SEVERE, "Cannot create a directory for campaign", e);
        }
        return null;
    }

    /**
     * Gets the private folder.
     *
     * @return the private folder
     */
    public File getPrivateFolder() {
        if (privateFolder == null) {
            privateFolder = new File(engine.getFolder(), getUser());
            if (!privateFolder.exists()) privateFolder.mkdirs();
        }
        return privateFolder;
    }
    /**
     * Gets the user.
     *
     * @return the user
     */
    public String getUser() {
        if (user == null)
            user = FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal().getName();
        return user;
    }

    /**
     * View a given campaign making it the current one.
     *
     * @param campaign the campaign
     * @return the string
     */
    public String view(MatosCampaign campaign) {
        current = campaign;
        return "view.xhtml?faces-redirect=true";
    }

    /**
     * Gets all the campaigns.
     *
     * @return the campaigns
     */
    public List<MatosCampaign> getCampaigns() {
        restore();
        return campaigns;
    }
    
    /**
     * Logout of the current JSF session.
     *
     * @return the string
     */
    public String logout() {
        HttpSession session = ((HttpSession) FacesContext.getCurrentInstance().getExternalContext()
                .getSession(false));
        if (session != null) session.invalidate();
        return "logout.xhtml";
    }

    /**
     * Generate name for campaigns.
     *
     * @return the string
     */
    private String generateName() {
        if (campaignName != null && campaignName.length() > 0) return campaignName;
        int count = 0;
        String name;
        do {
            name = "campaign-" + count++;
        } while (exists(name));
        return null;
    }

    /**
     * Checks campaign existence.
     *
     * @param name the name
     * @return true, if successful
     */
    private boolean exists(String name) {
        for(MatosCampaign campaign : campaigns) {
            if(name.equals(campaign.getName())) return true;
        }
        return false;
    }

    /**
     * Gets the current campaign.
     *
     * @return the current
     */
    public MatosCampaign getCurrent() {
        return current;
    }

    /**
     * Gets the android profiles.
     *
     * @return the android profiles
     */
    public List<String> getAndroidProfiles() {
        return getEngine().getAndroidProfiles();
    }

    /**
     * Gets the midp profiles.
     *
     * @return the midp profiles
     */
    public List<String> getMidpProfiles() {
        return getEngine().getMIDPProfiles();
    }

    /**
     * Gets the campaign name.
     *
     * @return the campaign name
     */
    public String getCampaignName() {
        return campaignName;
    }

    /**
     * Sets the campaign name.
     *
     * @param campaignName the new campaign name
     */
    public void setCampaignName(String campaignName) {
        this.campaignName = campaignName;
    }

    /**
     * Gets the engine.
     *
     * @return the engine
     */
    public MatosEngine getEngine() {
        return engine;
    }

    /**
     * Sets the engine.
     *
     * @param engine the new engine
     */
    public void setEngine(MatosEngine engine) {
        this.engine = engine;
    }

    /**
     * Gets the message style.
     *
     * @return the message style
     */
    public String getMessageStyle() {
        return messageStyle;
    }

    /**
     * Sets the message style.
     *
     * @param messageStyle the new message style
     */
    public void setMessageStyle(String messageStyle) {
        this.messageStyle = messageStyle;
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
     * Sets the error.
     *
     * @param error the new user
     */
    public void setError(String error) {
        this.message = error;
        this.messageStyle = "negative";
    }
    
    /**
     * Sets the confirmation message.
     *
     * @param msg the new confirm
     */
    public void setConfirm(String msg) {
        this.message = msg;
        this.messageStyle = "positive";
    }

}

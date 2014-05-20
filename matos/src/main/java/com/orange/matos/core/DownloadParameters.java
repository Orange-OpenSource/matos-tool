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
 * The Class DownloadParameters.
 */
public class DownloadParameters {
    
    /** Login. */
    private String login = null;
    
    /** Password. */
    private String password = null;
    
    /** User agent for faking a phone. */
    private String user_agent=null;


    /**
     * Gets the login.
     *
     * @return the login
     */
    public String getLogin() {
        return login;
    }

    /**
     * Sets the login.
     *
     * @param login the new login
     */
    public void setLogin(String login) {
        this.login = login;
    }

    /**
     * Gets the password.
     *
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password.
     *
     * @param password the new password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Gets the user_agent.
     *
     * @return the user_agent
     */
    public String getUserAgent() {
        return user_agent;
    }

    /**
     * Sets the user_agent.
     *
     * @param user_agent the new user_agent
     */
    public void setUser_agent(String user_agent) {
        this.user_agent = user_agent;
    }
    
    /**
     * Checks for login.
     *
     * @return true, if successful
     */
    public boolean hasLogin(){ return !isEmpty(getLogin()); }
    
    /**
     * Checks for password.
     *
     * @return true, if successful
     */
    public boolean hasPassword(){ return !isEmpty(getPassword()); }
    
    private static boolean isEmpty(String s) { return (s==null)||(s.length()==0); }
}

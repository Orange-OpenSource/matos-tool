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

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;

/**
 * The Class MatosAuth manage the authentication database.
 */
@ManagedBean(name = "matos_auth")
@SessionScoped
public class MatosAuth implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private static final String SQL_USER_CREATE = "INSERT INTO users VALUES (?,?)";
    private static final String SQL_ROLE_CREATE = "INSERT INTO roles VALUES (?,?)";
    private static final String SQL_SET_PASSWORD = "UPDATE users SET password=? WHERE user=?";
    private static final String SQL_CHECK_USER = "SELECT user FROM users WHERE user=? AND password = ?";
    private static final String SQL_GET_USERS = "SELECT user FROM users WHERE user!='admin'";
    private static final String SQL_DELETE_USER = "DELETE FROM users WHERE user=?";
    private static final String SQL_DELETE_ROLE = "DELETE FROM roles WHERE user=?";
    private static final String SQL_CHECK_ADMIN = "SELECT user FROM roles WHERE user=? AND role='admin'";
    /** The user. */
    transient private String user;
    
    /** The new user. */
    transient private String newUser;
    
    /** indicate if new user is an admin. */
    transient private boolean newUserAdmin;
    
    /** The deleted user. */
    transient private String deletedUser;
    
    /** The old password. */
    transient private String oldPassword;
    
    /** The new password1. */
    transient private String newPassword1;
    
    /** The new password2. */
    transient private String newPassword2;
    
    /** The error. */
    transient private String message;

    /** The connection. */
    transient private Connection connection;
    
    /** The message digest factory. */
    transient private MessageDigest messageDigest;
    
    transient private String messageStyle;
    
    /**
     * Instantiates a new matos auth.
     */
    public MatosAuth() {
        try {
            String dbName = System.getProperty("matos.authdb");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbName);
        } catch (SQLException e) {
            connection = null;
        }
    }
    
    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        try {
            String dbName = System.getProperty("matos.authdb");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbName);
        } catch (SQLException e) {
            connection = null;
        }
    }
    
    /**
     * Creates the user.
     *
     * @return the string
     */
    public String createUser() {
        if (connection == null) {
            setError("There is no password management on this instance");
            return null;
        }
        if(!isAdmin()) {
            setError("Only admin can add users.");
            return null;
        }
        String newUser = this.newUser;
        if (getUsers().contains(newUser)) {
            setError("The user " + newUser + " already exists in the database.");
            return null;
        }
        try {
            PreparedStatement stmt = connection.prepareStatement(SQL_USER_CREATE);
            try {
                stmt.setString(1, newUser);
                stmt.setString(2, getDigest(newUser));
                stmt.executeUpdate();
            } finally {
                stmt.close();
            }
        } catch (SQLException e) {
            setError("Cannot create user " + newUser + " in database");
            return null;
        }
        try {
            PreparedStatement stmt = connection.prepareStatement(SQL_ROLE_CREATE);
            try {
                stmt.setString(1, newUser);
                stmt.setString(2, "person");
                stmt.executeUpdate();
                if (isNewUserAdmin()) {
                    stmt.setString(2, "admin");
                    stmt.executeUpdate();
                }
            } finally {
                stmt.close();
            }
        } catch (SQLException e) {
            setError("Cannot set role for user " + newUser + " in database");
            return null;
        }
        
        setNewUser(null);
        setNewUserAdmin(false);
        setConfirm("User " + newUser + " created.");
        
        return null;
    }
    

    /**
     * Delete user.
     *
     * @return the string
     */
    public String deleteUser() {
        if (connection == null) {
            setError("There is no password management on this instance");
            return null;
        }
        if(!isAdmin()) {
            setError("Only admin can delete users.");
            return null;
        }
        if ("admin".equals(deletedUser)) {
            setError("Cannot delete admin user.");
        }
        try {
            PreparedStatement stmt = connection.prepareStatement(SQL_DELETE_USER);
            try {
                stmt.setString(1, deletedUser);
                stmt.executeUpdate();
            } finally {
                stmt.close();
            }
        } catch (SQLException e) {
            setError("Cannot delete user " + deletedUser + " in database");
            return null;
        }
        try {
            PreparedStatement stmt = connection.prepareStatement(SQL_DELETE_ROLE);
            try {
                stmt.setString(1, deletedUser);
                stmt.executeUpdate();
            } finally {
                stmt.close();
            }
        } catch (SQLException e) {
            setError("Cannot delete user role " + deletedUser + " in database");
            return null;
        }
        setConfirm("User " + deletedUser + " deleted.");
        return null;
    }
    
    /**
     * Change password.
     *
     * @return the string
     */
    public String changePassword() {
        if (connection == null) {
            setError("There is no password management on this instance");
            return null;
        }
        String oldDigest = getDigest(oldPassword);
        String user = getUser();
        boolean ok = false;
        try {
            PreparedStatement stmt = connection.prepareStatement(SQL_CHECK_USER);
            try {
                stmt.setString(2, oldDigest);
                stmt.setString(1, user);
                ResultSet rs = stmt.executeQuery();
                try {
                    ok = rs.next();
                } finally {
                    rs.close();
                }
            } finally {
                stmt.close();
            }
        } catch (SQLException e) {
            setError("Cannot check user in database");
            return null;
        }
        if (!ok) {
            setError("You are not authorized to change the password");
            return null;
        }
        if (! newPassword1.equals(newPassword2)) {
            setError("New passwords are different!");
            return null;
        }
        try {
            String digest = getDigest(newPassword1);
            PreparedStatement stmt = connection.prepareStatement(SQL_SET_PASSWORD);
            try {
                stmt.setString(1, digest);
                stmt.setString(2, user);
                stmt.executeUpdate();
            } finally {
                stmt.close();
            }
        } catch (SQLException e) {
            setError("Cannot set new password in database");
            return null;
        }
        setOldPassword(null);
        setNewPassword1(null);
        setNewPassword2(null);
        setConfirm("Password change succeeded.");
        return null;
    }

    /**
     * Checks if current user is admin.
     *
     * @return true, if is admin
     */
    public boolean isAdmin() {
        if (connection == null) return false;
        String user = getUser();
        boolean ok = false;
        try {
            PreparedStatement stmt = connection.prepareStatement(SQL_CHECK_ADMIN);
            try {
                stmt.setString(1, user);
                ResultSet rs = stmt.executeQuery();
                try {
                    ok = rs.next();
                } finally {
                    rs.close();
                }
            } finally {
                stmt.close();
            }
        } catch (SQLException e) {
            setError("Cannot check user in database");
            return false;
        }
        return ok;
    }
    
    
    /**
     * Is there user management. Used to avoid showing user management option when there is no
     * database.
     * @return true if there is a connection to database.
     */
    public boolean isManaged() {
        return (connection != null);
    }
    
    private String getDigest(String password) {
        if (messageDigest == null) {
            try {
                messageDigest = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                setError("Digest factory problem.");
                return null;
            } 
        }
        try {
            String digest = toHex(messageDigest.digest(password.getBytes("UTF-8")));
            return digest;
        } catch (UnsupportedEncodingException e) {            
            return null;
        }
    }
    
    private static String toHex(final byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            // Eclipse validation gone crazy.
            formatter.format("%02x", new Object [] { Byte.valueOf(b) });
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }

    /**
     * Gets the users.
     *
     * @return the users
     */
    public List<String> getUsers() {
        List<String> result = new ArrayList<String>();
        try {
            Statement stmt = connection.createStatement();
            try {
                ResultSet rs = stmt.executeQuery(SQL_GET_USERS);
                try {
                    while (rs.next()) {
                        result.add(rs.getString(1));
                    }
                } finally {
                    rs.close();
                }
            } finally {
                stmt.close();
            }
        } catch (SQLException e) {
            setError("Problem getting users.");
        }
        return result;
    }

    /**
     * Gets the new user.
     *
     * @return the new user
     */
    public String getNewUser() {
        return newUser;
    }

    /**
     * Sets the new user.
     *
     * @param newUser the new new user
     */
    public void setNewUser(String newUser) {
        this.newUser = newUser;
    }

    /**
     * Gets the deleted user.
     *
     * @return the deleted user
     */
    public String getDeletedUser() {
        return deletedUser;
    }

    /**
     * Sets the deleted user.
     *
     * @param deletedUser the new deleted user
     */
    public void setDeletedUser(String deletedUser) {
        this.deletedUser = deletedUser;
    }

    /**
     * Gets the new password2.
     *
     * @return the new password2
     */
    public String getNewPassword2() {
        return newPassword2;
    }

    /**
     * Sets the new password2.
     *
     * @param newPassword2 the new new password2
     */
    public void setNewPassword2(String newPassword2) {
        this.newPassword2 = newPassword2;
    }

    /**
     * Gets the new password1.
     *
     * @return the new password1
     */
    public String getNewPassword1() {
        return newPassword1;
    }

    /**
     * Sets the new password1.
     *
     * @param newPassword1 the new new password1
     */
    public void setNewPassword1(String newPassword1) {
        this.newPassword1 = newPassword1;
    }

    /**
     * Gets the old password.
     *
     * @return the old password
     */
    public String getOldPassword() {
        return oldPassword;
    }

    /**
     * Sets the old password.
     *
     * @param oldPassword the new old password
     */
    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }
    
    /**
     * Gets the error.
     *
     * @return the error
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the error.
     *
     * @param error the new user
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Sets the message style
     * @return
     */
    public String getMessageStyle() {
        return messageStyle;
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
     * Sets the error.
     *
     * @param msg
     */
    public void setConfirm(String msg) {
        this.message = msg;
        this.messageStyle = "positive";
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
     * Checks if is new user admin.
     *
     * @return true, if is new user admin
     */
    public boolean isNewUserAdmin() {
        return newUserAdmin;
    }

    /**
     * Sets the new user admin.
     *
     * @param newUserAdmin the new new user admin
     */
    public void setNewUserAdmin(boolean newUserAdmin) {
        this.newUserAdmin = newUserAdmin;
    }
}

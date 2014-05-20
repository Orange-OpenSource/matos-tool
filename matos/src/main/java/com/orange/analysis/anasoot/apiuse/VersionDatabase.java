package com.orange.analysis.anasoot.apiuse;

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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;

import com.orange.matos.core.Alert;

/**
 * Interface with a database containing all the versions of Android.
 * @author piac6784
 *
 */
public class VersionDatabase {
	/**
	 * URL Schema to open a database with the sqlite back-end
	 */
	private final static String JDBC_SCHEME = "jdbc:sqlite:";

	/**
	 * Name of the class to load to use the sqlite back-end
	 */
	private final static String JDBC_CLASS = "org.sqlite.JDBC";

	private final static String SUFFIX = "";
	/**
	 * prefix for hidden API in database
	 */
	public final static String HIDDEN_PREFIX = "h";
	/**
	 * Prefix for visible API in database
	 */
	public final static String VISIBLE_PREFIX = "v";

	private final static String FIND_PACKAGE = "SELECT package_id FROM package WHERE package_name = ?;";
	//	private final static String FIND_VERSION = "SELECT version_id FROM version WHERE version_name = ?;";
	private final static String FIND_CLASS = "SELECT class_id FROM class WHERE class_name = ? and package_id = ?;";
	private final static String FIND_TYPE = "SELECT type_id FROM type WHERE type = ?;";
	private final static String FIND_ELEMENT = "SELECT element_id FROM element WHERE element_name = ? and class_id = ? and type_id = ?;";

	private final static String FIND_VERSION_LIST = "SELECT substr(version_name,2,length(version_name)) FROM version GROUP BY substr(version_name,2,length(version_name));";

	private static final String FIND_LAST_UNSUPPORTED_VERSION_FOR_CLASS =	"SELECT substr(version_name,2,length(version_name)) FROM version EXCEPT  SELECT substr(version_name,2,length(version_name)) FROM class_version,version WHERE version.version_id=class_version.version_id AND version_name LIKE '"+VISIBLE_PREFIX+"%' AND class_id=? ORDER BY substr(version_name,2,length(version_name)) DESC LIMIT 0,1;";
	private static final String FIND_LAST_UNSUPPORTED_VERSION_FOR_ELEMENT =	"SELECT substr(version_name,2,length(version_name)) FROM version EXCEPT  SELECT substr(version_name,2,length(version_name)) FROM element_version,version WHERE version.version_id=element_version.version_id AND version_name LIKE '"+VISIBLE_PREFIX+"%' AND element_id=? ORDER BY substr(version_name,2,length(version_name)) DESC LIMIT 0,1;";

	private static final String FIND_VISIBLE_VERSIONS_FOR_CLASS = "SELECT version_name FROM version, class_version WHERE version.version_id=class_version.version_id AND class_version.class_id=? AND version_name LIKE '"+VISIBLE_PREFIX+"%' GROUP BY version_name;";
	private static final String FIND_VISIBLE_VERSIONS_FOR_ELEMENT = "SELECT version_name FROM version, element_version WHERE version.version_id=element_version.version_id AND element_version.element_id=? AND version_name LIKE '"+VISIBLE_PREFIX+"%' GROUP BY version_name;";

	private static final String FIND_HIDDEN_VERSIONS_FOR_CLASS = "SELECT version_name FROM version, class_version WHERE version.version_id=class_version.version_id AND class_version.class_id=? AND version_name LIKE '"+HIDDEN_PREFIX+"%' GROUP BY version_name;";
	private static final String FIND_HIDDEN_VERSIONS_FOR_ELEMENT = "SELECT version_name FROM version, element_version WHERE version.version_id=element_version.version_id AND element_version.element_id=? AND version_name LIKE '"+HIDDEN_PREFIX+"%' GROUP BY version_name;";


	private final PreparedStatement find_package_statement;
	//	private final PreparedStatement find_version_statement;
	private final PreparedStatement find_class_statement;
	private final PreparedStatement find_element_statement;
	private final PreparedStatement find_type_statement;
	private final PreparedStatement find_last_unsupported_version_for_class;
	private final PreparedStatement find_last_unsupported_version_for_element;
	private final PreparedStatement find_hidden_versions_for_class;
	private final PreparedStatement find_visible_versions_for_class;
	private final PreparedStatement find_visible_versions_for_element;
	private final PreparedStatement find_hidden_versions_for_element;

	private final PreparedStatement find_version_list_statement;

	private HashMap<SootClass, Integer> classmap = new HashMap<SootClass, Integer>();
	private HashMap<String, Integer> packagemap = new HashMap<String, Integer>();
	private HashMap<String, Integer> typemap = new HashMap<String, Integer>();

	/**
	 * Database connection.
	 */
	Connection con;
	/**
	 * Meta-data access on the connection and the database.
	 */
	DatabaseMetaData metadata;



	/**
	 * @param dbName The name of the database file to open
	 * @throws SQLException 
	 */
	public VersionDatabase(String dbName) throws SQLException {
		try { Class.forName(JDBC_CLASS);	} 
		catch (ClassNotFoundException e) { assert false; }
		con = DriverManager.getConnection(JDBC_SCHEME + dbName + SUFFIX);
		metadata = con.getMetaData();

		find_package_statement = con.prepareStatement(FIND_PACKAGE);
		find_class_statement = con.prepareStatement(FIND_CLASS);
		find_element_statement = con.prepareStatement(FIND_ELEMENT);
		//		find_version_statement = con.prepareStatement(FIND_VERSION);
		find_version_list_statement = con.prepareStatement(FIND_VERSION_LIST);
		find_type_statement = con.prepareStatement(FIND_TYPE);
		find_last_unsupported_version_for_class = con.prepareStatement(FIND_LAST_UNSUPPORTED_VERSION_FOR_CLASS);
		find_last_unsupported_version_for_element = con.prepareStatement(FIND_LAST_UNSUPPORTED_VERSION_FOR_ELEMENT);
		find_hidden_versions_for_class = con.prepareStatement(FIND_HIDDEN_VERSIONS_FOR_CLASS);
		find_visible_versions_for_class = con.prepareStatement(FIND_VISIBLE_VERSIONS_FOR_CLASS);
		find_hidden_versions_for_element = con.prepareStatement(FIND_HIDDEN_VERSIONS_FOR_ELEMENT);
		find_visible_versions_for_element = con.prepareStatement(FIND_VISIBLE_VERSIONS_FOR_ELEMENT);
	}

	private int findPackage(String name) {
		Integer i = packagemap.get(name);
		if (i != null) return i;
		ResultSet rs = null;
		try {
			find_package_statement.setString(1,name);
			rs = find_package_statement.executeQuery();
			int id = (rs.next()) ? rs.getInt(1) : -1;
			packagemap.put(name, id);
			return id;
		} catch (SQLException e) { e.printStackTrace(); return -1; }
		finally {
			try {
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {e.printStackTrace();}
		}
	}


	int findClass(SootClass c) {
		Integer i = classmap.get(c);
		if (i != null) return i;
		ResultSet rs = null;
		try {
			String packageName = c.getPackageName();
			String className = c.getJavaStyleName();
			int packageId = findPackage(packageName);
			if (packageId == -1)  {
				return -1;
			}
			find_class_statement.setString(1,className);
			find_class_statement.setInt(2, packageId);
			rs = find_class_statement.executeQuery();
			int id = (rs.next()) ? rs.getInt(1) : -1;
			classmap.put(c, id);
			return id;
		} catch (SQLException e) { e.printStackTrace(); return -1; }
		finally {
			try {
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {e.printStackTrace();}
		}
	}

	private int findType(String type) {
		Integer i = typemap.get(type);
		if (i != null) return i;
		ResultSet rs = null;
		try {
			find_type_statement.setString(1,type);
			rs = find_type_statement.executeQuery();
			int id = (rs.next()) ? rs.getInt(1) : -1;
			typemap.put(type,id);
			return id;
		} catch (SQLException e) { e.printStackTrace(); return -1; }
		finally {
			try {
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {e.printStackTrace();}
		}
	}

	private int findElement(String name, String type, SootClass clazz) {
		ResultSet rs = null;
		try {
			int clazzId = findClass(clazz);
			int typeId = findType(type);
			if (clazzId == -1 || typeId == -1) {
				return -1;
			}
			find_element_statement.setString(1,name);
			find_element_statement.setInt(2, clazzId);
			find_element_statement.setInt(3, typeId);
			rs = find_element_statement.executeQuery();
			int id = (rs.next()) ? rs.getInt(1) : -1;
			return id;
		} catch (SQLException e) { e.printStackTrace(); return -1; }
		finally {
			try {
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {e.printStackTrace();}
		}
	}


	/**
	 * Closes the database.
	 * @throws SQLException
	 */
	public void close() throws SQLException {
		con.close();
	}

	/**
	 * Gives the versions stored in the database
	 * @return a list of versions
	 * @throws Alert 
	 */
	public List<String> getVersions() throws Alert {
		ArrayList<String> versions = new ArrayList<String>();
		ResultSet rs = null;
		try {
			rs = find_version_list_statement.executeQuery();
			while(rs.next()) {
				versions.add(rs.getString(1));
			}
		} catch (SQLException e) {throw new Alert("Version database error - " + e.getMessage());}
		finally {
			try {
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {e.printStackTrace();}
		}
		return versions;
	}


	/**
	 * Gives the last version in which a class is not supported (eg. Class c 
	 * exists in 2.3 and more, gives back 2.2)
	 * @return Returns a null object if the class can't be found in the DB, 
	 * an empty String if it is present and visible in every version, or 
	 * is hidden in one version or more and absent in others,
	 * else the last version for which it is not supported
	 * @throws Alert 
	 */
	public String getLastUnsupportedVersionForClass(SootClass c) throws Alert {
		int classId = findClass(c);
		if(classId<0) {
			return null;
		}

		ResultSet rs = null;
		String result = "";
		try {

		    boolean hasHiddens;
		    boolean hasVisible;
			// class's hidden versions
			find_hidden_versions_for_class.setInt(1, classId);
			rs = find_hidden_versions_for_class.executeQuery();
			try {
			    hasHiddens = rs.next()?true:false;
			} finally {
			    rs.close();
			}

			// class's visible versions
			find_visible_versions_for_class.setInt(1, classId);
			rs = find_visible_versions_for_class.executeQuery();
			try {
			    hasVisible = rs.next()?true:false;
			} finally {
			    rs.close();
			}

			// if class is not visible in any version, and is hidden at least once
			// this is to be treated in hidden apis
			if(!hasVisible && hasHiddens) {
				return "";
			}

			find_last_unsupported_version_for_class.setInt(1, classId);
			rs = find_last_unsupported_version_for_class.executeQuery();
			if(rs.next())
				result = rs.getString(1);
			rs.close();

		} catch (SQLException e) {
			throw new Alert("Version database error - " + e.getMessage());
		} catch (IndexOutOfBoundsException e) {
			throw new Alert("Malformed version entries in version database - " + e.getMessage());
		}
		finally {
			try {
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {e.printStackTrace();}
		}
		return result;
	}

	/**
	 * Gives the last version in which an element is not supported (eg. Element e 
	 * exists in 2.3 and more, gives back 2.2)
	 * @return Returns a null object if the element can't be found in the DB, 
	 * an empty String if it is present and visible in every version or 
	 * is hidden in one version or more and absent in others, 
	 * else the last version for which it is not supported
	 * @throws Alert 
	 */
	private String getLastUnsupportedVersionForElement(String elementName, String type,
			SootClass declaringClass) throws Alert {

		int elementId = findElement(elementName, type, declaringClass);
		if(elementId<0) {
			return null;
		}

		ResultSet rs = null;
		String result = "";
		try {

			// element's hidden versions
			find_hidden_versions_for_element.setInt(1, elementId);
			rs = find_hidden_versions_for_element.executeQuery();
			boolean hasHiddens;
			try { 
			    hasHiddens = rs.next()?true:false;
			} finally { rs.close(); }

			// element's visible versions
			find_visible_versions_for_element.setInt(1, elementId);
			rs = find_visible_versions_for_element.executeQuery();
			boolean hasVisible;
			try {
			    hasVisible = rs.next()?true:false;
			} finally { rs.close(); }

			// if element is not visible in any version, and is hidden at least once
			// this is to be treated in hidden apis
			if(!hasVisible && hasHiddens) {
				return "";
			}

			find_last_unsupported_version_for_element.setInt(1, elementId);
			rs = find_last_unsupported_version_for_element.executeQuery();
			try {
			if(rs.next())
				result = rs.getString(1);
			} finally { rs.close(); }

		} catch (SQLException e) {
			throw new Alert("Version database error - " + e.getMessage());
		} catch (IndexOutOfBoundsException e) {
			throw new Alert("Malformed version entries in version database - " + e.getMessage());
		}

		return result;
	}

	/**
	 * Gives the list of version in which a class is hidden only
	 * @return null object if the class cannot be found in the DB, 
	 * an empty List if it is present in one or more versions, else the name
	 * of each version in which it's hidden only
	 * @throws Alert 
	 */
	public List<String> getHiddenApisForClass(SootClass c) throws Alert {
		int classId = findClass(c);
		if(classId<0) {
			return null;
		}

		ArrayList<String> result = new ArrayList<String>();
		ResultSet rs = null;
		try {
			// result = versions in which the class is hidden
			find_hidden_versions_for_class.setInt(1, classId);
			rs = find_hidden_versions_for_class.executeQuery();
			try {
			    while(rs.next()) {
			        result.add(rs.getString(1).substring(1)); 
			    }
			} finally { rs.close(); }

			// if the class is visible in one version or more, 
			// don't show as hidden api (goes to unsupported)
			find_visible_versions_for_class.setInt(1, classId);
			boolean hasVisible;
			rs = find_visible_versions_for_class.executeQuery();
			try {
			    hasVisible = rs.next() ? true : false;
			} finally {
			    rs.close();
			}

			if(hasVisible) {
				result.clear();
				return result;
			}

		} catch (SQLException e) {
			throw new Alert("Version database error - " + e.getMessage());
		} catch (IndexOutOfBoundsException e) {
			throw new Alert("Malformed version entries in version database - " + e.getMessage());
		}

		return result;
	}

	/**
	 * Gives the list of version in which an element is hidden only
	 * @return null object if the element can't be found in the DB,
	 * an empty List if it is present in one or more versions, else the name
	 * of each version in which it's hidden only
	 * @throws Alert 
	 */
	private List<String> getHiddenApisForElement(String element, String type,
			SootClass declaringClass) throws Alert {
		int elementId = findElement(element, type, declaringClass);
		if(elementId<0) {
			return null;
		}

		ArrayList<String> result = new ArrayList<String>();
		ResultSet rs = null;
		try {
			// result = versions in which the element is hidden
			find_hidden_versions_for_element.setInt(1, elementId);
			rs = find_hidden_versions_for_element.executeQuery();
			while(rs.next()) {
				result.add(rs.getString(1).substring(1)); 
			}
			rs.close();

			// if the element is visible in one version or more, 
			// don't show as hidden api (goes to unsupported)
			find_visible_versions_for_element.setInt(1, elementId);
			rs = find_visible_versions_for_element.executeQuery();
			boolean hasVisible = rs.next()?true:false;
			rs.close();

			if(hasVisible) {
				result.clear();
				return result;
			}

		} catch (SQLException e) {
			throw new Alert("Version database error - " + e.getMessage());
		} catch (IndexOutOfBoundsException e) {
			throw new Alert("Malformed version entries in version database - " + e.getMessage());
		}
		finally {
			try {
				if(rs!=null)
					rs.close();
			} catch (SQLException e) {e.printStackTrace();}
		}

		return result;
	}

	/**
	 * Format the signature of method to find in database.
	 * @param m
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static String signature(SootMethod m) {
		return signature(m.getParameterTypes()) + m.getReturnType().toString(); 
	}

	private static String signature(List<Type> args) {
		StringBuilder buf = new StringBuilder("(");
		boolean first = true;
		for (Type arg : args) {
			if (first) first = false;
			else buf.append(",");
			buf.append(arg.toString());
		}
		buf.append(")");
		return buf.toString();
	}



	/**
	 * Name of last version where this element is NOT supported.
	 * @param f
	 * @return
	 * @throws Alert
	 */

	public String getLastUnsupportedVersionForElement(SootMethod m) throws Alert {
		return getLastUnsupportedVersionForElement(m.getName(), signature(m), m.getDeclaringClass());
	}

	/**
	 * Name of last version where this field is NOT supported.
	 * @param f
	 * @return
	 * @throws Alert
	 */
	public String getLastUnsupportedVersionForElement(SootField f) throws Alert {
		return getLastUnsupportedVersionForElement(f.getName(), f.getType().toString(), f.getDeclaringClass());
	}

	/**
	 * List of hidden APIs of a method
	 * @param f
	 * @return
	 * @throws Alert
	 */

	public List<String> getHiddenApisForElement(SootMethod m) throws Alert {
		return getHiddenApisForElement(m.getName(), signature(m), m.getDeclaringClass());
	}
	/**
	 * List of hidden APIs of a field.
	 * @param f
	 * @return
	 * @throws Alert
	 */
	public List<String> getHiddenApisForElement(SootField f) throws Alert {
		return getHiddenApisForElement(f.getName(), f.getType().toString(), f.getDeclaringClass());
	}

}

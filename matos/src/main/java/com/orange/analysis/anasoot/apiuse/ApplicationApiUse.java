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

import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.tagkit.AbstractHost;

import com.orange.analysis.anasoot.main.AndroidWrapper;
import com.orange.matos.core.Alert;

/**
 * Class to register API uses for each element.
 * It stores the list of unsupported versions and the hidden uses per elements.
 * @author Laurent Sebag
 *
 */
public class ApplicationApiUse {

  private HashMap<String, TreeSet<AbstractHost>> unsupported;
  private TreeMap<AbstractHost, List<String>> hiddens;
  private VersionDatabase database;
  private List<String> versions;
  
  /**
   * Comparator to sort elements that either SootClass, SootMethod or SootField
   */
  private final static Comparator<AbstractHost> ElementsComparator = new Comparator<AbstractHost>() {
    @Override
    public int compare(AbstractHost o1, AbstractHost o2) {
      String a;
      String b;
      if(o1 instanceof SootClass) {
        a = "C"+((SootClass) o1).getPackageName() + ((SootClass)o1).getJavaStyleName();
      } else if(o1 instanceof SootMethod) {
        SootClass c = ((SootMethod) o1).getDeclaringClass();
        a = "M"+c.getPackageName() + c.getJavaStyleName() + ((SootMethod)o1).getName() + VersionDatabase.signature((SootMethod) o1);
      } else if(o1 instanceof SootField) {
        SootClass c = ((SootField) o1).getDeclaringClass();
        a = "F"+c.getPackageName() + c.getJavaStyleName() + ((SootField)o1).getName() + ((SootField)o1).getSignature();
      } else {
        return -1;
      }
      if(o2 instanceof SootClass) {
        b = "C"+((SootClass) o2).getPackageName() + ((SootClass)o2).getJavaStyleName();
      } else if(o2 instanceof SootMethod) {
        SootClass c = ((SootMethod) o2).getDeclaringClass();
        b = "M"+c.getPackageName() + c.getJavaStyleName() + ((SootMethod)o2).getName() + VersionDatabase.signature((SootMethod) o2);
      } else if(o2 instanceof SootField) {
        SootClass c = ((SootField) o2).getDeclaringClass();
        b = "F"+c.getPackageName() + c.getJavaStyleName() + ((SootField)o2).getName() + ((SootField)o2).getSignature();
      } else {
        return 1;
      }
      
      return a.compareTo(b);
    }
  };
  
  /**
   * Constructor of the checker. It needs a version database path to work.
   * @param versionDatabasePath
   * @throws Alert
   */
  public ApplicationApiUse(String versionDatabasePath) throws Alert {
    unsupported = new HashMap<String, TreeSet<AbstractHost>>();
    hiddens = new TreeMap<AbstractHost, List<String>>(ElementsComparator);

    try {
      database = new VersionDatabase(versionDatabasePath);
      versions = database.getVersions();
    } catch (SQLException e) {
      throw new Alert("Cannot open version database - " + e.getMessage());
    }
  }
  
  /**
   * Adds an element to the corresponding TreeSet<AbstractHost>
   * in the Map unsupported.
   * @param lastUnsupported
   * @param element
   */
  private void addUnsupported(String lastUnsupported, AbstractHost element) {
    TreeSet<AbstractHost> elements = unsupported.get(lastUnsupported);
    if( elements==null ) {
      elements = new TreeSet<AbstractHost>(ElementsComparator);
      unsupported.put(lastUnsupported, elements);
    }
    elements.add(element);
  }
  
  /**
   * Check the support for a given class
   * @param c class to check
   * @param orig origin class containing the reference
   * @throws Alert
   */
  public void register(SootClass c, SootClass orig ) throws Alert {
    
    String lastUnsupported = database.getLastUnsupportedVersionForClass(c);
    if(lastUnsupported!=null && !lastUnsupported.isEmpty())
      addUnsupported(lastUnsupported, c);
    
    List<String> hiddenVersions = database.getHiddenApisForClass(c);
    if(hiddenVersions!=null && !hiddenVersions.isEmpty()) {
		// System.out.println("Unsupported " + c + " in " + orig);
        hiddens.put(c, hiddenVersions);
    }
    
  }

  /**
   * Check the support for a given field 
   * @param f field to check
   * @param orig origin class containing the reference
   * @throws Alert
   */
  public void register(SootField f, SootClass orig) throws Alert {

    String lastUnsupported = database.getLastUnsupportedVersionForElement(f);
    if(lastUnsupported!=null && !lastUnsupported.isEmpty())
      addUnsupported(lastUnsupported, f);

    List<String> hiddenVersions = database.getHiddenApisForElement(f);
    if(hiddenVersions!=null && !hiddenVersions.isEmpty()) {
      hiddens.put(f, hiddenVersions);
    }

  }
  
  /**
   * @return Returns a Map associating a version to a set of either SootClass, SootField
   * or SootMethod. The version corresponds to the last version in which the element 
   * is not supported. (eg. Element e exists in 2.3 and more, gives back 2.2)
   */
  public Map<String, TreeSet<AbstractHost>> getUnsupportedVersions() {
    return unsupported;
  }

  
  /**
   * @return Returns a Map associating either a SootClass, a SootField
   * or a SootMethod to a list of versions in which the element is hidden only
   */
  public TreeMap<AbstractHost, List<String>> getHiddenApiUses() {
    return hiddens;
  }

  /**
   * List of known versions in the database (without the visible and hidden prefix).
   * @return
   * @throws Alert
   */
  public List<String> getVersions() {
    return versions;
  }

	/**
	 * Check the support for a given method
	 * @param m method to check
	 * @param orig origin class containing the reference
	 * @throws Alert
	 */
	public void register(SootMethod m, SootClass orig) throws Alert{
		String methodName = m.getName();
		if(methodName==null || methodName.equals("<clinit>")) {
			// ignore static class initializers
			return;
		}

		String lastUnsupported = database.getLastUnsupportedVersionForElement(m);
		if(lastUnsupported!=null && !lastUnsupported.isEmpty())
			addUnsupported(lastUnsupported, m);

		List<String> hiddenVersions = database.getHiddenApisForElement(m);
		if(hiddenVersions!=null && !hiddenVersions.isEmpty()) {
			// System.out.println("Unsupported " + m + " in " + orig);
			hiddens.put(m, hiddenVersions);
		}
	}


	/**
	 * Close definitively the database.
	 * @throws Alert
	 */
	public void closeDB() throws Alert {
		if(database!=null)
			try {
				database.close();
			} catch (SQLException e) {
				throw new Alert("Error while closing version database - " + e.getMessage());
			}
	}


	/**
	 * Check that a class is defined by the profile and not by the APK.
	 * @param c
	 * @return
	 */
	public boolean isProfileDefined(SootClass c) {
		return c.getName().startsWith(AndroidWrapper.WRAPPER_PACKAGE) || database.findClass(c) != -1;
	}


}

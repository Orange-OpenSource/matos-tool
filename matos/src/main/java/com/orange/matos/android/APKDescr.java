package com.orange.matos.android;

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

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import soot.RefType;
import soot.SootFieldRef;
import soot.SootMethodRef;

import com.orange.d2j.APKFile;
import com.orange.d2j.D2JLog;
import com.orange.d2j.DexFile;
import com.orange.d2j.Hierarchy;
import com.orange.d2j.manifest.AndroidConfiguration;
import com.orange.d2j.manifest.Component;
import com.orange.d2j.manifest.Filter;
import com.orange.d2j.manifest.ManifestContentHandler;
import com.orange.d2j.manifest.MetaData;
import com.orange.d2j.manifest.Provider;
import com.orange.d2j.manifest.Receiver;
import com.orange.d2j.manifest.Service;
import com.orange.matos.core.AppDescription;
import com.orange.matos.core.XMLStream;
import com.orange.matos.utils.HtmlOutput;

/**
 * @author piac6784
 * Represents the APK file.
 */
public class APKDescr extends AppDescription {

	final private ManifestContentHandler manifest;
	final private Hierarchy hierarchy;
	// This is a hack. Keeps results between phases.
	final APKFile apkFile;
	
	/**
	 * Constructor
	 * @param apkFile
	 * @throws IOException
	 */
	public APKDescr(APKFile apkFile) throws IOException {
		D2JLog.setDebug();
		this.apkFile = apkFile;
		manifest = apkFile.manifest();
		hierarchy = apkFile.hierarchy();
	}
	
	/**
	 * Printable name of the origin file.
	 * @return
	 */
	public String getName() { return apkFile.getName(); }
	
	/**
	 * All the activities provided by the APK
	 * @return
	 */
	public Set<String> getActivities () { return manifest.getActivities(); }
	
	/**
	 * All the activities provided by the APK even if not declared in manifest (not usable).
	 * @return
	 */
	public Set<String> getAllActivities () { 
		return hierarchy.subclasses("android.app.Activity"); 
	}

	/**
	 * All the services implemented by the APK even if not declared in manifest (not usable).
	 * @return
	 */
	public Set<String> getAllServices () { 
		return hierarchy.subclasses("android.app.Service"); 
	}

	/**
	 * All the content providers implemented by the APK even if not declared in manifest (not usable).
	 * @return
	 */	
	public Set<String> getAllContentProviders () { 
		return hierarchy.subclasses("android.content.ContentProvider"); 
	}

	/**
	 * All the broadcast receivers implemented by the APK even if not declared in manifest (not usable).
	 * @return
	 */	
	public Set<String> getAllBroadcastReceivers () { 
		return hierarchy.subclasses("android.content.BroadcastReceiver"); 
	}

	/**
	 * All the views implemented by the APK.
	 * @return
	 */
	public Set<String> getAllViews () { 
		return hierarchy.subclasses("android.view.View"); 
	}

	/**
	 * All the fragments implemented by the APK.
	 * @return
	 */
	public Set<String> getAllFragments () { 
		return hierarchy.subclasses("android.app.Fragment"); 
	}
	
	/**
	 * Get all the implementers in the APK of the set of classes.
	 * @param classes
	 * @return
	 */
	public Set<String>getSubHierarchy(String [] classes) {
		HashSet<String> result = new HashSet<String>();
		for(String c : classes) {
			result.addAll(hierarchy.subclasses(c));
		}
		return result;
	}
	
	/**
	 * Check if a class is abstract in the hierarchy.
	 * @param clazz as a complete string name.
	 * @return
	 */
	public boolean isAbstract(String clazz) { return hierarchy.isAbstract(clazz); }
	/**
	 * All the services provided
	 * @return
	 */
	public Set<String> getServices () { return manifest.getServices(); }
	
	/**
	 * All the content providers
	 * @return
	 */
	public Set<String> getContentProviders () { return manifest.getProviders(); }
	/**
	 * All the broadcast receivers
	 * @return
	 */
	public Set<String> getBroadcastReceivers () { return manifest.getReceivers(); }
	
	/**
	 * Get the representation of the code.
	 * @return
	 */
	public DexFile getCode() { 
		try { apkFile.resetCode(); return apkFile.code(); } 
		catch (IOException e) { return null; }
	}
	
	/**
	 * Akk the filters associated to broadcast receivers.
	 * @return
	 */
	public List <Filter> allBroadcastReceiverFilters() {
		ArrayList <Filter>list = new ArrayList<Filter>();
		for (String receiver : manifest.getReceivers()) {
			list.addAll(manifest.getReceiverFilters(receiver));
		}
		return list;
	}
	
	/**
	 * Print the errors found while parsing in D2J library.
	 * @param xmlFormat kind of output (xml or not)
	 * @param out stream to print to.
	 * @return
	 */
	public boolean resolutionErrors(boolean xmlFormat, PrintStream out) {
		Set <SootMethodRef> unresolvedMethods = apkFile.unresolvedMethods();
		Set <SootFieldRef> unresolvedFields = apkFile.unresolvedFields();
		Set <RefType> unresolvedClasses = apkFile.unresolvedClasses();
		
		boolean hasUnresolvedMethods = unresolvedMethods != null && unresolvedMethods.size() > 0;
		boolean hasUnresolvedFields = unresolvedFields != null && unresolvedFields.size() > 0;
		boolean hasUnresolvedClasses = unresolvedClasses != null && unresolvedClasses.size() > 0;
		boolean result = hasUnresolvedClasses || hasUnresolvedMethods || hasUnresolvedFields; 
		if (result && ! xmlFormat) {
			out.println(HtmlOutput.header(1, "Unknown APIs"));
		}
		if (hasUnresolvedMethods) {
			if (xmlFormat) {
				XMLStream xmlout = new XMLStream(out);
				for (SootMethodRef methodRef : unresolvedMethods) {
					xmlout.element("unknownMethod");
					xmlout.attribute("method", methodRef.getSignature());
					xmlout.endElement();
				}
			} else {
				out.println(HtmlOutput.header(2, HtmlOutput.color("red", "Unknown methods")));
				out.println("<ul>");
				for (SootMethodRef methodRef : unresolvedMethods) {
					out.println("<li>" + HtmlOutput.escape(methodRef.getSignature()) + "</li>" );
				}
				out.println("</ul>");
			}
			
		}

		if (hasUnresolvedFields) {
			if (xmlFormat) {
				XMLStream xmlout = new XMLStream(out);
				for (SootFieldRef fieldRef : unresolvedFields) {
					xmlout.element("unknownField");
					xmlout.attribute("field", fieldRef.getSignature());
					xmlout.endElement();
				}
			} else {
				out.println(HtmlOutput.header(2, HtmlOutput.color("red", "Unknown fields")));
				out.println("<ul>");
				for (SootFieldRef fieldRef : unresolvedFields) {
					out.println("<li>" + HtmlOutput.escape(fieldRef.getSignature()) + "</li>" );
				}
				out.println("</ul>");				
			}
			
		}
		if (hasUnresolvedClasses) {
			if (xmlFormat) {
				XMLStream xmlout = new XMLStream(out);
				for (RefType typeRef : unresolvedClasses) {
					xmlout.element("unknownClass");
					xmlout.attribute("class", typeRef.getClassName());
					xmlout.endElement();
				}
			} else {
				out.println(HtmlOutput.header(2, HtmlOutput.color("red", "Unknown classes")));
				out.println("<ul>");
				for (RefType typeRef : unresolvedClasses) {
					out.println("<li>" + HtmlOutput.escape(typeRef.getClassName()) + "</li>" );
				}
				out.println("</ul>");				
			}
		}

		return result;
	}
	
	/**
	 * Does it support any density.
	 * @return
	 */
	public boolean supportsAnyDensity() { return manifest.supportsAnyDensity(); }
	
	/**
	 * Gives back a set of flags indicating supported screen sizes.
	 * @return
	 */
	public int supportedScreenSizes() { return manifest.supportedScreenSizes(); }
	
	/**
	 * Gives back the set of supported configurations.
	 * @return
	 */
	public Set<AndroidConfiguration> getConfigurations() { return manifest.getConfigurations(); }
	
	/**
	 * Gives back the set of required features.
	 * @return
	 */
	public Set <String> requiredFeatures() { return manifest.requiredFeatures(); }
	/**
	 * Gives back the set of optional features.
	 * @return
	 */
	public Set <String> optionalFeatures() { return manifest.optionalFeatures(); }

	/**
	 * Gives back the set of required libraries.
	 * @return
	 */
	public Set <String> requiredLibraries() { return manifest.requiredLibraries(); }
	/**
	 * Gives back the set of optional libraries.
	 * @return
	 */
	public Set <String> optionalLibraries() { return manifest.optionalLibraries(); }
	
	/**
	 * Gives back the supported OpenGL ES version
	 * @return
	 */
	public String glVersion() { return manifest.glVersion(); }
	
	/**
	 * Gives back minimum Sdk requirement
	 * @return
	 */
	public int minSdk() { return manifest.minSdk(); }
	
	/**
	 * Gives back maximum Sdk requirement
	 * @return
	 */
	public int maxSdk() { return manifest.maxSdk(); }

	/**
	 * Gives back target Sdk requirement
	 * @return
	 */
	public int targetSdk() { return manifest.targetSdk(); }
	
	/**
	 * Gives back permissions requested.
	 * @return
	 */
	public Set <String> requestedPermissions() { return manifest.usedPermissions(); }

	/**
	 * Gives back the classname of the application
	 * @return usually null unless it is defined (rare).
	 */
	public String getApplication() { return manifest.getApplicationClassName(); }
	
	/**
	 * Shared process id if any.
	 * @return
	 */
	public String getProcessId() { return manifest.getApplication().process; }
	/**
	 * Shared user id if any.
	 * @return
	 */
	public String getSharedUserId() { return manifest.getSharedUserId(); }
	/**
	 * Readable version name of the app.
	 * @return
	 */
	public String getVersionName() { return manifest.getVersionName();}
	/**
	 * Increasing version number of the app.
	 * @return
	 */
	public int getVersionCode() { return manifest.getVersionCode();}
	
	/**
	 * Gives back an activity
	 * @param name complete class name
	 * @return
	 */
	public Component getActivity(String name) { return manifest.getActivity(name); }
	
	/**
	 * Gives back a declared broadcast receiver
	 * @param name complete class name
	 * @return
	 */
	public Receiver getReceiver(String name) { return manifest.getReceiver(name); }
	
	/**
	 * Gives back a declared service
	 * @param name complete class name
	 * @return
	 */
	public Service getService(String name) { return manifest.getService(name); }
	
	/**
	 * Gives back a declared content provider
	 * @param name complete class name
	 * @return
	 */
	public Provider getProvider(String name) { return manifest.getProvider(name); }
	
	
	/**
	 * Access an application level meta-data element.
	 * @param name
	 * @return
	 */
	public MetaData getMetaData(String name) { return manifest.getMetaData(name); }
	
}

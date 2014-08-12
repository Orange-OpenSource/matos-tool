package com.orange.analysis.anasoot;

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

import java.io.File;
import java.io.FilenameFilter;

import com.orange.analysis.anasoot.spy.CustomSemanticRule;
import com.orange.matos.core.Alert;
import com.orange.matos.core.Configuration;

/**
 * Configuration parameters specific to Anasoot.
 */
/**
 * @author Pierre Cregut
 *
 */
public class AnasootConfig { 

	/** Internal mode MIDP (analysing a midlet). */
	public static final int MIDP = 1;
	
	/** Internal mode Android (analysing an android APK file). */
	public static final int ANDROID = 2;
	
	/** The configuration. */
	static private AnasootConfig configuration;
	
	/** The config. */
	private Configuration config;
	
	/** The mode. */
	private final int mode;

	/**
	 * Flag : should we treat concatenation in string. 
	 */
	private boolean treatConcatenation = true;
	
	/** Flag : should we follow string buffer objects. */
	private boolean treatStringBuffer = true;
	
	/** Flag : should we follow valueOf. */
	private boolean treatValueOf = true;
	
	/** Flag : should we try to solve Application properties. */
	private boolean treatProperties = true;
	
	/** Flag : should we consider operations on strings. */
	private boolean treatStringOperation = true;
	
	/** Flag : should we try to follow functions. */
	private boolean treatFunctions = true;
	
	/**
	 * Flag : should we handle method return if needed.
	 */
	private boolean treatSpyReturn = true;
	
	/**
	 * Flag : should we handle method parameters.
	 */
	private boolean treatSpyParameter = true;
	
	/**
	 * Flag : should we print node abstractions (eg. for Navmid)
	 */
	private boolean printNodes = true;
	
	/** Flag : should we do an enhanced pointsto analysis. */
	private boolean enhancedAnalysis = false;
	
	
	/** Flag : should we refine with an on-demand pointsto analysis (if used no enhanced analysis). */
	private boolean ondemandAnalysis = false;
	
	/** Flag :. */
	private boolean usedJSR = true;
	
	/**
	 * Flag : do we perform a loop analysis to point out recursive methods.
	 */
	private boolean loopAnalysis = true;

	/**
	 * Flag : do we check API usage in Android.
	 */
	private boolean doApiUse = false;

	/**
	 * Path to the version database.
	 */
	public String databasePath;

	/**
	 * Constructor with a bag of properties.
	 *
	 * @param config the config
	 * @param mode the mode
	 */
	private AnasootConfig (Configuration config, int mode) {
		this.config = config;
		this.mode = mode;
		readProperties();
	}

	/**
	 * Create the new configuration and register it.
	 *
	 * @param config a bag of properties
	 * @param mode the mode
	 * @return gives back the new configuration.
	 */
	static public AnasootConfig newConfiguration(Configuration config, int mode) {
		configuration = new AnasootConfig(config, mode);
		return configuration;
	}

	/**
	 * Get the global configuration.
	 *
	 * @return the unique configuration of the system.
	 */
	static public AnasootConfig getConfiguration() {
		return configuration;
	}

	/**
	 * Read properties from the Property structure and store them in variables.
	 */
	public void readProperties() {
		treatConcatenation = config.bool("anasoot.treatConcatenation");
		treatStringBuffer = config.bool("anasoot.treatStringBuffer");
		treatValueOf = config.bool("anasoot.treatValueOf");
		treatProperties = config.bool("anasoot.treatProperties");
		treatStringOperation = config.bool("anasoot.treatStringOperation");
		treatFunctions = config.bool("anasoot.treatFunctions");

		treatSpyReturn = config.bool("anasoot.treatSpyReturn");
		treatSpyParameter = config.bool("anasoot.treatSpyParameter");
		printNodes = config.bool("anasoot.printNodes");
		enhancedAnalysis = config.bool("anasoot.enhancedPointsto");
		ondemandAnalysis = config.bool("anasoot.onDemandPointsto");
		usedJSR = config.bool("anasoot.usedJSR");
		loopAnalysis = config.bool("anasoot.loopAnalysis", false);
		doApiUse = config.bool("anasoot.androidApiUse");
		databasePath = new File(config.getLibDir(), config.string("anasoot.androidDatabase")).getAbsolutePath();
	}

	/**
	 * Flag : should we use XML format for output.
	 *
	 * @return true, if successful
	 */
	public boolean xmlFormat() {
		return config.xmlFormat();
	}


	/**
	 * A class that can be used to filter filenames that correspond to jar files.
	 * @author Pierre Cregut
	 *
	 */
	static public class JarFileFilter implements FilenameFilter {
		
		/* (non-Javadoc)
		 * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
		 */
		@Override
		public boolean accept(File dir, String name) {
			return name.endsWith(".jar"); 
		}
	}
	
	/**
	 * Check we are working in Android mode.
	 * @return true if so.
	 */
	public boolean isAndroid() { return mode == ANDROID; }

	/**
	 * Gets a class through the custom classLoader.
	 *
	 * @param classname the classname
	 * @return the custom class
	 * @throws Alert the alert
	 */
	public CustomSemanticRule getCustomClass(String classname) throws Alert {
	    try {
	    Class <?> clazz = Class.forName(classname,true, config.getCustomClassLoader());
        Class<? extends CustomSemanticRule> mfclass = clazz.asSubclass(CustomSemanticRule.class);
        CustomSemanticRule rule = mfclass.newInstance();
        return rule;
	    } catch (Exception e) {
	        throw Alert.raised(e,"cannot instantiate custom class " + classname);
	    }
	}

    /**
     * Checks for enhanced points-to analysis.
     *
     * @return true, if successful
     */
    public boolean hasEnhancedAnalysis() {
        return enhancedAnalysis;
    }

    /**
     * Checks for on-demand pointsto analysis.
     *
     * @return true, if successful
     */
    public boolean hasOndemandAnalysis() {
        return ondemandAnalysis;
    }

    /**
     * Do api use.
     *
     * @return true, if successful
     */
    public boolean doApiUse() {
        return doApiUse;
    }

    /**
     * Do loop analysis.
     *
     * @return true, if successful
     */
    public boolean doLoopAnalysis() {
        return loopAnalysis;
    }

    /**
     * Do print nodes.
     *
     * @return true, if successful
     */
    public boolean doPrintNodes() {
        return printNodes;
    }

    /**
     * Do used jsr.
     *
     * @return true, if successful
     */
    public boolean doUsedJSR() {
        return usedJSR;
    }

    /**
     * Treat concatenation.
     *
     * @return true, if successful
     */
    public boolean treatConcatenation() {
        return treatConcatenation;
    }

    /**
     * Treat string buffer.
     *
     * @return true, if successful
     */
    public boolean treatStringBuffer() {
        return treatStringBuffer;
    }

    /**
     * Treat value of.
     *
     * @return true, if successful
     */
    public boolean treatValueOf() {
        return treatValueOf;
    }

    /**
     * Treat properties.
     *
     * @return true, if successful
     */
    public boolean treatProperties() {
        return treatProperties;
    }

    /**
     * Treat string operation.
     *
     * @return true, if successful
     */
    public boolean treatStringOperation() {
        
        return treatStringOperation;
    }

    /**
     * Treat functions.
     *
     * @return true, if successful
     */
    public boolean treatFunctions() {
        return treatFunctions;
    }

    /**
     * Treat spy return.
     *
     * @return true, if successful
     */
    public boolean treatSpyReturn() {
        return treatSpyReturn;
    }

    /**
     * Treat spy parameter.
     *
     * @return true, if successful
     */
    public boolean treatSpyParameter() {
        return treatSpyParameter;
    }
}

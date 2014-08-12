package com.orange.analysis.android;

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
import java.util.List;
import java.util.Set;

import com.orange.d2j.manifest.AndroidConfiguration;
import com.orange.d2j.manifest.Filter;
import com.orange.d2j.manifest.ManifestContentHandler;
import com.orange.matos.android.APKDescr;
import com.orange.matos.core.Alert;
import com.orange.matos.core.AppDescription;
import com.orange.matos.core.Configuration;
import com.orange.matos.core.MatosPhase;
import com.orange.matos.core.RuleFile;
import com.orange.matos.utils.HtmlOutput;

/**
 * This phase extracts relevant information from the manifest file.
 * @author Pierre Cregut
 *
 */
public class AndroidManifestPhase implements MatosPhase {
	private static final String PERMISSION_PREFIX = "android.permission.";
	private static final int PERMISSION_PREFIX_LENGTH = PERMISSION_PREFIX.length();
	private static final String ACTION_PREFIX = "android.provider.telephony.";
	private static final int ACTION_PREFIX_LENGTH = ACTION_PREFIX.length();
	private static final String FEATURE_PREFIX = "android.hardware.";
	private static final int FEATURE_PREFIX_LENGTH = FEATURE_PREFIX.length();

	private final static String PHASE_NAME = "AndroidManifest";
	boolean xmlout;
	private Configuration configuration;
	@Override
	public String getName() {
		return PHASE_NAME;
	}

	@Override
	public void init(Configuration config) {
	    configuration = config;
		xmlout = config.xmlFormat();
	}

	@Override
	public boolean isGlobal() {	return true; }

	private void line(PrintStream out, String head, String value) {
		out.println(HtmlOutput.row(HtmlOutput.cellHead(head) + HtmlOutput.cell(value)));
	}
	@Override
	public boolean run(String midletName, AppDescription desc,
			RuleFile ruleFile, PrintStream out) throws IOException, Alert {
		if (! (desc instanceof APKDescr)) return false;
		APKDescr apk = (APKDescr) desc;
		if (!xmlout) {
			out.println(HtmlOutput.header(1, "Manifest information"));
			out.println(HtmlOutput.openTable());

			String versionName = apk.getVersionName();
			int versionCode = apk.getVersionCode();
			if (versionName != null || versionCode != -1) {
				line(out, "Version (Code)", versionName + "(" + versionCode + ")");
			}

			String sizes = "";
			if ((apk.supportedScreenSizes() & ManifestContentHandler.SMALL_SCREEN) != 0) {
				sizes = (", SMALL");
			}
			if ((apk.supportedScreenSizes() & ManifestContentHandler.NORMAL_SCREEN) != 0) {
				sizes += (", NORMAL");
			}
			if ((apk.supportedScreenSizes() & ManifestContentHandler.LARGE_SCREEN) != 0) {
				sizes += (", LARGE");
			}
			line(out,"Supported screen sizes",(sizes.length() == 0) ? sizes : sizes.substring(2));
			
			line(out, "Supports any density", String.valueOf(apk.supportsAnyDensity()));
			Set<AndroidConfiguration> configs = apk.getConfigurations();
			if (configs.size() > 0) {
				boolean first = true;
				StringBuilder supported = new StringBuilder();
				for(AndroidConfiguration c : configs) {
					if (first) first = false;
					else supported.append(HtmlOutput.br());
					supported.append(c.toString());
				}
				line(out,"Supported configurations", supported.toString());
			}
			Set <String> requestedPermissions = apk.requestedPermissions();
			if (requestedPermissions.size() > 0) {
				boolean first = true;
				StringBuilder perms = new StringBuilder();
				for(String f : requestedPermissions) {
					if (first) first = false;
					else perms.append(", ");
					if (f.startsWith(PERMISSION_PREFIX)) {
						perms.append("<font color=\"red\">");
						perms.append(f.substring(PERMISSION_PREFIX_LENGTH));
						perms.append("</font>");
					} else	perms.append(f);
				}
				line(out,"Requested permissions", perms.toString());
			}
	
			Set <String> requiredFeatures = apk.requiredFeatures();
			if (requiredFeatures.size() > 0) {
				boolean first = true;
				StringBuilder features = new StringBuilder();
				for(String f : requiredFeatures) {
					if (first) first = false;
					else features.append(", ");
					if (f.startsWith(FEATURE_PREFIX)) f = f.substring(FEATURE_PREFIX_LENGTH);
					features.append(f);
				}
				line(out,"Required features", features.toString());
			}
			Set <String> optionalFeatures = apk.optionalFeatures();
			if (optionalFeatures.size() > 0) {
				boolean first = true;
				StringBuilder features = new StringBuilder();
				for(String f : optionalFeatures) {
					if (first) first = false;
					else features.append(", ");
					if (f.startsWith(FEATURE_PREFIX)) f = f.substring(FEATURE_PREFIX_LENGTH);
					features.append(f);
				}
				line(out,"Optional features", features.toString());
			}

			int minSdk = apk.minSdk();
			if (minSdk != -1) { 
				line(out, "Minimum SDK", String.valueOf(minSdk));
			}
			int maxSdk = apk.maxSdk();
			if (maxSdk != -1) {
				line(out, "Maximum SDK", String.valueOf(maxSdk));
			}
			int targetSdk = apk.targetSdk();
			if (targetSdk != -1) {
				line(out, "Target SDK", String.valueOf(targetSdk));
			}
			String sharedUserId = apk.getSharedUserId();
			if (sharedUserId != null) {
				line(out, "Shared User ID", sharedUserId);
			}
			String processId = apk.getProcessId();
			if (processId != null) {
				line(out, "Shared Process ID", processId);
			}
			
			List <Filter> filters = apk.allBroadcastReceiverFilters();
			if (filters.size() > 0) {
				line(out,"Receiver filters", dumpFilters(filters));
			}
			
			out.println(HtmlOutput.closeTable());
		}
		CustomManifestAnalysis cma = new CustomManifestAnalysis(ruleFile, configuration);
		cma.doAnalysis(apk);
		cma.doXsltOutput(out);
		return true;
	}

	private String dumpFilters(List<Filter> filters) {
		boolean isFirst = true;
		StringBuilder sb = new StringBuilder();
		for(Filter filter : filters) {
			if (filter.actions !=  null) {
				for (String action: filter.actions) {
					if (isFirst) isFirst = false;
					else sb.append("<br/>");
					if (action.startsWith(ACTION_PREFIX)) {
						sb.append("<font color=\"red\">");
						sb.append(action.substring(ACTION_PREFIX_LENGTH));
						sb.append("</font>");
					} else	sb.append(action);
					if (filter.priority != 0) sb.append(" - " + filter.priority);
				}
			}
		}
		return sb.toString();
	}

	@Override
	public String getMessage() {
		return null;
	}

    @Override
    public int getScore() {
        return -1;
    }

}

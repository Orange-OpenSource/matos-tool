package com.orange.analysis.headers;

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
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

import com.orange.matos.core.Alert;
import com.orange.matos.core.Configuration;
import com.orange.matos.java.MidletKind;

/**
 * The MIDlet descriptor is a complex attribute and many checks are done by this
 * checker. This function checks the following on each value:
 * <ul>
 * <li>Each description line is made of three components,
 * <li>the name and URL are not empty in any line of the JAR or JAD, A
 * constraint on those names may be defined.
 * <li>classnames mentioned do exist in the JAR file
 * <li>icons mentioned do exist in the JAR file, are correct PNG file and if
 * there is a restriction that they have a correct size.
 * <li>the values in the JAR and JAD coincide (except for spaces).
 * </ul>
 * It also check that there is no "free values" in the list and that the
 * numbering is consistent starting from 1. It can also check that a
 * configurable maximum number of midlets has not been exceeded.
 * 
 * The following options are used:
 * <ul>
 * <li>descriptor.maxMidlets
 * <li>descriptor.midletNameRegexp
 * <li>descriptor.midletIconSize
 * </ul>
 * 
 * @author piac6784
 * 
 */
public class MidletAttrChecker extends RegexpAttributeChecker {

	final private File jarFile;
	final private Configuration config;
	private int maxMidlets = 0;
	private Set<String> checkPNGsize = null;

	/**
	 * Constructor.
	 * 
	 * @param config
	 * @param jarFile
	 */
	public MidletAttrChecker(Configuration config, File jarFile) {
		super("MIDlet-([0-9][0-9]?)");
		this.jarFile = jarFile;
		this.config = config;
	}

	@Override
	public void check() throws Alert {
		maxMidlets = config.integer("descriptor.maxMidlets", 0);
		String midletNameRegexpDescr = config
				.string("descriptor.midletNameRegexp");
		Pattern midletNameRegexp = null;
		if (midletNameRegexpDescr != null && midletNameRegexpDescr.length() > 0) {
			midletNameRegexp = Pattern.compile(midletNameRegexpDescr);
		}
		String authorizedSizes = config.string("descriptor.midletIconSize");
		if (authorizedSizes != null && authorizedSizes.length() > 0) {
			checkPNGsize = new HashSet<String>();
			List<String> components = Configuration
					.parseCommaList(authorizedSizes);
			checkPNGsize.addAll(components);
		}

		int i = 1;
		String key = String.valueOf(i);
		ZipFile zf;
		try {
			try {
				zf = new ZipFile(jarFile);
			} catch (Exception e) {
				zf = null;
			}
			try {
				while (jadMap.containsKey(key) || jarMap.containsKey(key)) {
					MidletDescr jadValue = parse(i, "JAD descriptor", zf,
							(String) jadMap.get(key));
					MidletDescr jarValue = parse(i, "JAR manifest", zf,
							(String) jarMap.get(key));
					if (jarValue != null && jadValue != null) {
						if (!jarValue.name.equals(jadValue.name)) {
							addProblem(
									"Names for midlet "
											+ i
											+ " differ from the JAR Manifest to the JAD file.",
									"");
						}

						if (!jarValue.icon.equals(jadValue.icon)) {
							addProblem(
									"Icons for midlet "
											+ i
											+ " differ from the JAR Manifest to the JAD file.",
									"");
						}
						if (!jarValue.classname.equals(jadValue.classname)) {
							addProblem(
									"Class identifier for midlet "
											+ i
											+ " differs from the JAR Manifest to the JAD file.",
									"");
						}
					}
					MidletDescr value = (jarValue == null) ? jadValue
							: jarValue;
					if (value != null) {
						if (midletNameRegexp != null
								&& !midletNameRegexp.matcher(value.name)
										.matches()) {
							addProblem(
									"Name for midlet "
											+ i
											+ " does not respect the imposed constaints.",
									"");
						}
						MidletKind.addKind(config, value.classname,
								"user service", i);
					}

					jadMap.remove(key);
					jarMap.remove(key);
					key = String.valueOf(++i);
				}
				if ((maxMidlets != 0) && (i - 1) > maxMidlets) {
					addProblem("More than " + maxMidlets
							+ " midlet declared in midlet suite: " + (i - 1),
							"");
				}

				if (jarMap.size() != 0) {
					addProblem(
							"There are gaps in the numbering of midlets in the JAR manifest. The following values are incorrect indices: "
									+ jarMap.keySet(), "");
				}

				if (jadMap.size() != 0) {
					addProblem(
							"There are gaps in the numbering of midlets in the JAD descriptor. The following values are incorrect indices: "
									+ jarMap.keySet(), "");
				}
			} finally {
				zf.close();
			}
		} catch (IOException e) {
			addProblem(
					"IO Error while reading midlet attributes: "
							+ e.getMessage(), "");
		}
	}

	private MidletDescr parse(int i, String from, ZipFile zf, String line) {
		if (line == null)
			return null;
		int i1 = line.indexOf(',');
		int i2 = line.indexOf(',', i1 + 1);
		if (i1 < 0 || i2 < 0) {
			addProblem("Midlet description for midlet " + i
					+ " should have three parameters (name,icon,classname).",
					"");
			return null;
		}
		String name = line.substring(0, i1).trim();
		String icon = line.substring(i1 + 1, i2).trim();
		String classname = line.substring(i2 + 1).trim();
		if (name.equals("")) {
			addProblem("Name for midlet " + i + " not given in the " + from
					+ ".", "");
		}
		if (classname.equals("")) {
			addProblem("Class name for midlet " + i + " not given in the "
					+ from + ".", "");
		}
		if (!icon.equals("") && checkNotExists(zf, icon)) {
			addProblem("Icon for midlet " + i + " mentioned in " + from
					+ " does not exist in the JAR (" + icon + ").", "");
		}
		// checking PNG status on file extension is not a good way! (files
		// without extension are allowed, ...)
		if (!icon.equals("")) {
			if (!IconChecker.isPNG(zf, icon)) {
				addProblem("Icon for midlet " + i + " mentioned in " + from
						+ " does not seem to be a PNG file.", "");
			} else {
				String size;
				if ((checkPNGsize != null)
						&& (!checkPNGsize.contains(size = IconChecker
								.sizeImage(zf, icon)))) {
					addProblem("Size of icon for midlet " + i + " (" + size
							+ ") is not compliant with the restrictions.", "");
				}
			}
		}

		String filename = classname.replace('.', '/') + ".class";
		if (!filename.equals("") && checkNotExists(zf, filename)) {
			addProblem("Class for midlet " + i + " mentioned in " + from
					+ " does not exist in the JAR (" + classname + ").", "");
		}
		;
		return new MidletDescr(name, icon, classname);
	}

	private static class MidletDescr {
		String name, icon, classname;

		MidletDescr(String name, String icon, String classname) {
			this.name = name;
			this.icon = icon;
			this.classname = classname;
		}
	}

	private boolean checkNotExists(ZipFile zf, String entry) {
		if (entry != null && entry.length() > 1 && entry.charAt(0) == '/')
			entry = entry.substring(1);
		return (zf != null && zf.getEntry(entry) == null);
	}

}

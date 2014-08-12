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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;

import com.orange.matos.core.Alert;
import com.orange.matos.core.Configuration;
import com.orange.matos.core.Out;

/**
 * We verify mainly two things on an icon, that it is a correct PNG image and, if a set of
 * authorized sizes is provided, that it is one of those sizes.
 * 
 * It uses <code>descriptor.midletSuiteIconSize</code> option.
 * 
 * @author Pierre Cregut
 *
 */
public class IconChecker extends SimpleAttrChecker {
	final private Configuration config;
	final private File jarFile;
	IconChecker(Configuration config, File jarFile) {
		super("MIDlet-Icon");
		this.config = config;
		this.jarFile = jarFile;
	}

	@Override
	public void check() throws Alert {
		ZipFile zf = null;
		try { 
			zf = new ZipFile(jarFile); 
		} catch (Exception e){ 
			zf=null; 
		}	
		String authorizedSizes = config.string("descriptor.midletSuiteIconSize");
		Set<String> checkPNGsize = null;
		if (authorizedSizes != null && authorizedSizes.length() > 0) {
			checkPNGsize = new HashSet<String>();
			List <String> components = Configuration.parseCommaList(authorizedSizes);
			checkPNGsize.addAll(components);
		}
		String value = (injar != null) ? injar : injad;
		if (value != null){
			if (!isPNG(zf,value)) {
				addProblem("The declared MIDlet suite icon (" + value +") does not exist in the Jar file.","");
			} else {
				String size = sizeImage(zf,value);
				if (checkPNGsize != null && !checkPNGsize.contains(size)) {
					addProblem("The size of the MIDlet suite icon (" + size + ") does not respect the constraints.","");
				}
			}
		}		
		if (zf != null) { try { zf.close(); } catch (IOException ie) {} }
	}

	/**
	 * Returns the size of an image (works at least for PNG)
	 * @param zf
	 * @param entry_name
	 * @return
	 */
	public static String sizeImage(ZipFile zf, String entry_name) {
		String size = null;
		if (zf!=null &&entry_name != null && entry_name.length() > 1) {
			if (entry_name.charAt(0) == '/') entry_name = entry_name.substring(1);
			ZipEntry ze = zf.getEntry(entry_name);
			if (ze == null) return "?";
			try {
				InputStream is = zf.getInputStream(ze);
				try {
					BufferedImage image = ImageIO.read(is);
					size = image==null ? "?" : image.getWidth() + "x" + image.getHeight();
				} finally { is.close();}	
			} catch (IOException e) {
				e.printStackTrace(Out.getLog());
			}
		}
		return size;
	}

	/**
	 * Check if the given zip entry is a PNG file, according to the PNG W3C specification version 1.0 (http://www.w3.org/TR/PNG/) 
	 * @param ze the zip entry to check
	 * @return true if the file is a PNG, false otherwise.
	 */
	public static boolean isPNG(ZipFile zf, String entry_name) {
		boolean isPNG = false;

		if (zf!=null &&entry_name != null && entry_name.length() > 1) {
			if (entry_name.charAt(0) == '/') entry_name = entry_name.substring(1);
			ZipEntry ze = zf.getEntry(entry_name);
			if (zf != null && ze != null) {
				InputStream is = null;
				try {
					is = zf.getInputStream(ze);
					try {
						// reading the first 8 bytes
						byte[] sig = new byte[8];
						int r = is.read(sig);
						if (r == 8 && sig[0] == -119 && sig[1] == 80
								&& sig[2] == 78 && sig[3] == 71 && sig[4] == 13
								&& sig[5] == 10 && sig[6] == 26 && sig[7] == 10)
							isPNG = true;
						// 137 80 78 71 13 10 26 10 is the PNG file's signature
						// according to W3C spec at
						// http://www.w3.org/TR/PNG/#5PNG-file-signature
						// aka -119 80 78 71 13 10 26 10 (8 bit signed, like what is read)
					} finally {
						is.close();
					}
				} catch (IOException e) {
					e.printStackTrace(Out.getLog());
				} 
			} 
		}
		return isPNG;
	}
}

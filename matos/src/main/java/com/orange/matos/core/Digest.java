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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.HashMap;


/**
 * @author Pierre Cregut
 * Message digest.
 */
public class Digest {
	private static final int SIZE = 16384;
	private static final String base = "0123456789abcdef";
	
	/**
	 * HashMap which makes a correspondance between a file and his md5.
	 */
	private static HashMap<File, String> md5Map = new HashMap <File, String> ();
	private static HashMap <File, Long> lastModifiedMap = new HashMap<File, Long>();
	
	/**
	 * Compute the md5 of a file 
	 * @param filename name of the file 
	 * @return md5 as a String
	 */
	public static String runMD5(File file) {

		FileInputStream fis = null;
		String md5;
		try {
			if (md5Map.containsKey(file)){
				if (file.lastModified() == ((Long)lastModifiedMap.get(file)).longValue()){
					return (String)md5Map.get(file);
				}
			}
			fis = new FileInputStream(file);
			md5 = runMD5(fis);
			md5Map.put(file, md5);
			lastModifiedMap.put(file, Long.valueOf(file.lastModified()));
		} catch (FileNotFoundException e) {
			md5 = null;
		} catch (NullPointerException e) {
			md5 = null;
		}
		try {if (fis != null) fis.close(); } catch (IOException e) {}
		return md5;
	}

	/**
	 * Compute MD5 on a given file
	 * @param filename
	 * @return
	 */
	public static String runMD5(String filename) {
		if ((filename==null)||filename.equals("")) return null;
		return runMD5(new File(filename));
	}

	
	/**
	 * Compute the md5 of a file from a FileInputStream 
	 * @param fis a FileInputStream  
	 * @return md5 as a String
	 */
	public static String runMD5(FileInputStream fis) {
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			byte buffer[] = new byte[SIZE];
			while(true) {
				int l=fis.read(buffer);
				if (l== -1 ) break;
				md5.update(buffer,0,l);
			}
			byte digest [] = md5.digest();
			StringBuffer buf = new StringBuffer();
			for(int i=0; i < digest.length; i++) {
				buf.append(base.charAt((digest[i] >> 4) & 0xf));
				buf.append(base.charAt(digest[i] & 0xf));
			}
			return buf.toString();
		}catch (Exception e) {
			//e.printStackTrace(Out.main);
			e.printStackTrace(Out.getLog());
			return null;
		}
	}
	
	/**
	 * Compute the md5 of a file from a FileInputStream 
	 * @param fis a FileInputStream  
	 * @return md5 as a String
	 */
	public static String runSHA1(FileInputStream fis) {
		try {
			MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
			byte buffer[] = new byte[SIZE];
			while(true) {
				int l=fis.read(buffer);
				if (l== -1 ) break;
				sha1.update(buffer,0,l);
			}
			byte digest [] = sha1.digest();
			StringBuffer buf = new StringBuffer();
			for(int i=0; i < digest.length; i++) {
				buf.append(base.charAt((digest[i] >> 4) & 0xf));
				buf.append(base.charAt(digest[i] & 0xf));
			}
			return buf.toString();
		}catch (Exception e) {
			//e.printStackTrace(Out.main);
			e.printStackTrace(Out.getLog());
			return null;
		}
	}
	
}

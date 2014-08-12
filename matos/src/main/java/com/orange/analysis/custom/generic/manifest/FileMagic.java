package com.orange.analysis.custom.generic.manifest;

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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.orange.analysis.android.CustomManifestRule;
import com.orange.matos.android.APKDescr;

/**
 * Scan magic header to identify potential elf files or apks at weird places.
 * 
 * @author Pierre Cregut
 * 
 */
public class FileMagic implements CustomManifestRule {

	private static final String SUSPICIOUS_ELF_NAMES = "suspicious.elf.names";
	private static final String SUSPICIOUS_ELF = "suspicious.elf";
	private static final String SUSPICIOUS_APK_NAMES = "suspicious.apk.names";
	private static final String SUSPICIOUS_APK = "suspicious.apk";
	private static final byte[] ELF_HEADER = { 0x7f, 0x45, 0x4c, 0x46 };
	private static final byte[] ZIP_HEADER = { 0x50, 0x4b, 0x03, 0x04 };

	@Override
	public void run(APKDescr manifest, Properties props) {
		HashSet<String> suspiciousElf = new HashSet<String>();
		HashSet<String> suspiciousApk = new HashSet<String>();
		String name = manifest.getName();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(name);
			try {
				ZipInputStream zis = new ZipInputStream(fis);
				try {
				ZipEntry e;
				byte header[] = new byte[4];
				while ((e = zis.getNextEntry()) != null) {
					if (zis.read(header) != 4)
						continue;
					if (Arrays.equals(header, ELF_HEADER)) {
						if (!e.getName().startsWith("lib/")) {
							suspiciousElf.add(e.getName());
						}
					} else if (Arrays.equals(header, ZIP_HEADER)) {
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						bos.write(header);
						byte buf[] = new byte[4096];
						int l;
						while ((l = zis.read(buf)) != -1) {
							bos.write(buf, 0, l);
						}
						ByteArrayInputStream bis = new ByteArrayInputStream(
								bos.toByteArray());
						ZipInputStream eis = new ZipInputStream(bis);
						ZipEntry ee;
						while ((ee = eis.getNextEntry()) != null) {
							if (ee.getName().equals("classes.dex")) {
								suspiciousApk.add(e.getName());
							}
						}
					}
				}
				} finally {
					zis.close();
				}
			} finally {
				fis.close();
			}
		} catch (IOException e) {
			System.out.println("Bad things happen");
		}
		if (suspiciousElf.size() > 0) {
			props.put(SUSPICIOUS_ELF, true);
			props.put(SUSPICIOUS_ELF_NAMES, suspiciousElf.toString());
		}
		if (suspiciousApk.size() > 0) {
			props.put(SUSPICIOUS_APK, true);
			props.put(SUSPICIOUS_APK_NAMES, suspiciousApk.toString());
		}

	}

}

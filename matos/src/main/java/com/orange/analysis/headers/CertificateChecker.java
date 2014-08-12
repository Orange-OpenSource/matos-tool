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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Vector;

import com.orange.matos.core.Alert;
import com.orange.matos.utils.HtmlOutput;

/**
 * @author Pierre Cregut
 * Verification of properties of the signature of a midlet 
 */
public class CertificateChecker extends RegexpAttributeChecker {
	
	private static final String aux_head = "-----BEGIN CERTIFICATE-----\n";
	private static final String aux_tail = "\n-----END CERTIFICATE-----\n";
	private Vector<CertificatePath> certificatePaths;
	private boolean firstCall = true;

	/**
	 * Empty constructor.
	 */
	public CertificateChecker() {
		super("MIDlet-Certificate-([0-9][0-9]?-([0-9][0-9]?)?)");
		certificatePaths = new Vector<CertificatePath>();
	}

	@Override
	public void check() throws Alert {		
		if (jadMap.size() != 0 && firstCall) {
			// The list of certificate paths is built following the
			// the verification process given in [MIDP2] p 31.
			ArrayList<ArrayList<Certificate>> certificatePathList = new ArrayList<ArrayList<Certificate>> ();
			String val;
			CertificateFactory cf = null;
			try {
				cf = CertificateFactory.getInstance("X.509");
			} catch (CertificateException e1) {
				addProblem("Problem with certificate: "+e1.getMessage(), "");
				return;
			}
			for(int i=1; jadMap.containsKey(i+"-1"); i++) {
				ArrayList<Certificate> certificateList = new ArrayList<Certificate> ();
				for(int j=1; (val=(String) jadMap.get(i+"-"+j))!=null; j++) {
					jadMap.remove(i+"-"+j);
					
					try {
					    InputStream is =
		                        new ByteArrayInputStream( (aux_head + val + aux_tail).getBytes("UTF-8") );
						try {
							Certificate cert = cf.generateCertificate(is);
							certificateList.add(cert);
						} finally {
							is.close();
						}
					} catch (CertificateException e) {
						addProblem("Problem with certificate " +j+ " in path " +i+": "+e.getMessage(), "");
					} catch (IOException e2) {
						addProblem("IO Problem: " + e2.getMessage(),"");
					}
				}
				certificatePathList.add(certificateList);
			}
			for(int i = 0; i < certificatePathList.size(); i++) {
				ArrayList<Certificate> l2 = certificatePathList.get(i);
				CertificatePath path = new CertificatePath();
				path.setNumber(i+1);
				for(int j = 0; j < l2.size(); j++) {
					Certification certif = new Certification();
					certif.setNumber(j+1);
					X509Certificate cert = (X509Certificate) l2.get(j);
					certif.setIssuer(cert.getIssuerDN().toString());
					certif.setSubject(cert.getSubjectDN().toString());
					certif.setBeginDate(cert.getNotBefore().toString());
					certif.setEndDate(cert.getNotAfter().toString());
					path.getCertificates().add(certif);
				}
				certificatePaths.add(path);
			}
		}
		firstCall = false;
	}
	
	/**
	 * @author Pierre Cregut
	 * A certification path is a chain of certificates.
	 */
	public static class CertificatePath{
		
		private int number;		
		private Vector<Certification> certificates;
		
		/**
		 * Empty constructor
		 */
		public CertificatePath(){
			certificates = new Vector<Certification>();
		}

		/**
		 * Gets the chain of certificates as a vector
		 * @return
		 */
		public Vector<Certification> getCertificates() {
			return certificates;
		}

		/**
		 * Number identifying the chain in the JAD
		 * @return
		 */
		public int getNumber() {
			return number;
		}

		/**
		 * Set the identifying number
		 * @param number
		 */
		public void setNumber(int number) {
			this.number = number;
		}
		
	}
	
	/**
	 * Represents a certificate in the chain.
	 * @author Pierre Cregut
	 *
	 */
	public static class Certification{
		
		private int number;
		private String issuer;
		private String subject;
		private String beginDate;
		private String endDate;
		
		/**
		 * First date where certificate is valid
		 * @return
		 */
		public String getBeginDate() {
			return beginDate;
		}
		/**
		 * Set the first date of validity
		 * @param beginDate
		 */
		public void setBeginDate(String beginDate) {
			this.beginDate = beginDate;
		}
		/**
		 * End date of certificate validity
		 * @return
		 */
		public String getEndDate() {
			return endDate;
		}
		/**
		 * Set the end date of certificate validity
		 * @param endDate
		 */
		public void setEndDate(String endDate) {
			this.endDate = endDate;
		}
		/**
		 * Get the issuer of a certificate
		 * @return
		 */
		public String getIssuer() {
			return issuer;
		}
		/**
		 * Set the issuer of a certificate
		 * @param issuer
		 */
		public void setIssuer(String issuer) {
			this.issuer = issuer;
		}
		/**
		 * Get the subject (who it applies to)
		 * @return
		 */
		public String getSubject() {
			return subject;
		}
		/**
		 * Set the subject 
		 * @param subject
		 */
		public void setSubject(String subject) {
			this.subject = subject;
		}
		/**
		 * @return
		 */
		public int getNumber() {
			return number;
		}
		/**
		 * Certificate number 
		 * @param number
		 */
		/**
		 * Cet certificate number.
		 * @param number
		 */
		public void setNumber(int number) {
			this.number = number;
		}
		
	}

	/**
	 * Extract the list of certification paths.
	 * @return
	 */
	public Vector<CertificatePath> getCertificatePaths() {
		return certificatePaths;
	}

	/**
	 * To be displayed if certification chain ok.
	 * @param outStream stream of output.
	 * @param xmlFormat regular or XML format
	 */
	public void writeOkMessage(PrintStream outStream, boolean xmlFormat) {
		if (xmlFormat) {
			outStream.println("<checker name=\"Certificate Checker\">");
			for (CertificatePath path : certificatePaths) {
				outStream.println("<path id=\"" + path.getNumber() + "\">");
				Vector<Certification> certifs = path.getCertificates();
				for (Certification certif : certifs) {
					outStream.println(
							"<certificate id=\""+ certif.getNumber() + 
							"\" issuer=\"" + certif.getIssuer() +
							"\" subject=\"" + certif.getSubject() +
							"\" validFrom=\"" + certif.getBeginDate() +
							"\" validUntil=\"" + certif.getEndDate() + "\"/>");
				}
			}
		} else {
			for (CertificatePath path : certificatePaths) {
				outStream.println(HtmlOutput.header(3,"Certification path "+path.getNumber()));
				Vector<Certification> certifs = path.getCertificates();
				for (Certification certif : certifs) {
					outStream.println(HtmlOutput.header(4,"Certification "+certif.getNumber()));
					String infos [] = {"Issuer: " + certif.getIssuer(),
							"Subject: " + certif.getSubject(),
							"Validity: from " + certif.getBeginDate() +	" until " + certif.getEndDate()
					};
					outStream.println(HtmlOutput.list(infos));
				}
			}
		}
	}

}

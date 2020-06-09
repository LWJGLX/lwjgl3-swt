/*******************************************************************************
 * Copyright (c) 2010, 2012 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.swt.internal.win32;


public class CERT_INFO {
	public int dwVersion;
	public CRYPT_INTEGER_BLOB SerialNumber = new CRYPT_INTEGER_BLOB ();
	public CRYPT_ALGORITHM_IDENTIFIER SignatureAlgorithm = new CRYPT_ALGORITHM_IDENTIFIER ();
	public CERT_NAME_BLOB Issuer = new CERT_NAME_BLOB ();
	public FILETIME NotBefore = new FILETIME ();
	public FILETIME NotAfter = new FILETIME ();
	public CERT_NAME_BLOB Subject = new CERT_NAME_BLOB ();
	public CERT_PUBLIC_KEY_INFO SubjectPublicKeyInfo = new CERT_PUBLIC_KEY_INFO ();
	public CRYPT_BIT_BLOB IssuerUniqueId = new CRYPT_BIT_BLOB ();
	public CRYPT_BIT_BLOB SubjectUniqueId = new CRYPT_BIT_BLOB ();
	public int cExtension;
	/** @field cast=(PCERT_EXTENSION) */
	public long rgExtension;

	public static final int sizeof = OS.CERT_INFO_sizeof ();
}

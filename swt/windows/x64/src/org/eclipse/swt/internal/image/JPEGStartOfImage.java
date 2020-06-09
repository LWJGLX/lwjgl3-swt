/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.swt.internal.image;


final class JPEGStartOfImage extends JPEGFixedSizeSegment {

	public JPEGStartOfImage() {
		super();
	}

	public JPEGStartOfImage(byte[] reference) {
		super(reference);
	}

	public JPEGStartOfImage(LEDataInputStream byteStream) {
		super(byteStream);
	}

	@Override
	public int signature() {
		return JPEGFileFormat.SOI;
	}

	@Override
	public int fixedSize() {
		return 2;
	}
}

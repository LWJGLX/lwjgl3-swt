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
package org.eclipse.swt.graphics;


import org.eclipse.swt.*;
import org.eclipse.swt.internal.gtk.*;

/**
 * Instances of this class are descriptions of GCs in terms
 * of unallocated platform-specific data fields.
 * <p>
 * <b>IMPORTANT:</b> This class is <em>not</em> part of the public
 * API for SWT. It is marked public only so that it can be shared
 * within the packages provided by SWT. It is not available on all
 * platforms, and should never be called from application code.
 * </p>
 * @see <a href="http://www.eclipse.org/swt/">Sample code and further information</a>
 * @noreference This class is not intended to be referenced by clients
 */
public final class GCData {
	public Device device;
	public int style, state = -1;

	/**
	 * <p>
	 * <b>IMPORTANT:</b> This field is <em>not</em> part of the SWT
	 * public API. It is marked public only so that it can be shared
	 * within the packages provided by SWT. It is not available on all
	 * platforms and should never be accessed from application code.
	 * </p>
	 *
	 * @noreference This field is not intended to be referenced by clients.
	 */
	public GdkRGBA foregroundRGBA, backgroundRGBA;

	public Font font;
	public Pattern foregroundPattern;
	public Pattern backgroundPattern;
	public float lineWidth;
	public int lineStyle = SWT.LINE_SOLID;
	public float[] lineDashes;
	public float lineDashesOffset;
	public float lineMiterLimit = 10;
	public int lineCap = SWT.CAP_FLAT;
	public int lineJoin = SWT.JOIN_MITER;
	public boolean xorMode;
	public int alpha = 0xFF;
	public int interpolation = SWT.DEFAULT;
	public Image image;

	public long clipRgn, context, layout, damageRgn, cairo, regionSet;
	/** Usually a GdkWindow on GTK3, or a GdkSurface on GTK4 */
	public long drawable;
	public double cairoXoffset, cairoYoffset;
	public boolean disposeCairo;
	public double[] identity, clippingTransform;
	public String string;
	public int stringWidth = -1;
	public int stringHeight = -1;
	public int drawFlags;
	public boolean realDrawable;
	public int width = -1, height = -1;
}

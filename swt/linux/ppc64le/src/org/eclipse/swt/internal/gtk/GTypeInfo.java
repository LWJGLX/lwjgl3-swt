/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others. All rights reserved.
 * The contents of this file are made available under the terms
 * of the GNU Lesser General Public License (LGPL) Version 2.1 that
 * accompanies this distribution (lgpl-v21.txt).  The LGPL is also
 * available at http://www.gnu.org/licenses/lgpl.html.  If the version
 * of the LGPL at http://www.gnu.org is different to the version of
 * the LGPL accompanying this distribution and there is any conflict
 * between the two license versions, the terms of the LGPL accompanying
 * this distribution shall govern.
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.swt.internal.gtk;


public class GTypeInfo {
	/** @field cast=(guint16) */
	public short class_size;
	/** @field cast=(GBaseInitFunc) */
	public long base_init;
	/** @field cast=(GBaseFinalizeFunc) */
	public long base_finalize;
	/** @field cast=(GClassInitFunc) */
	public long class_init;
	/** @field cast=(GClassFinalizeFunc) */
	public long class_finalize;
	/** @field cast=(gconstpointer) */
	public long class_data;
	/** @field cast=(guint16) */
	public short instance_size;
	/** @field cast=(guint16) */
	public short n_preallocs;
	/** @field cast=(GInstanceInitFunc) */
	public long instance_init;
	/** @field cast=(GTypeValueTable *) */
	public long value_table;
	public static final int sizeof = OS.GTypeInfo_sizeof();
}

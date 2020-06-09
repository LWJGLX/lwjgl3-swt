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


public class GdkEventKey extends GdkEvent {
	/** @field cast=(GdkWindow *) */
	public long window;
	/** @field cast=(gint8) */
	public byte send_event;
	/** @field cast=(guint32) */
	public int time;
	/** @field cast=(guint) */
	public int state;
	/** @field cast=(guint) */
	public int keyval;
	/** @field cast=(gint) */
	public int length;
	/** @field cast=(gchar *) */
	public long string;
	/** @field cast=(guint16) */
	public short hardware_keycode;
	/** @field cast=(guint8) */
	public byte group;
	/** @field cast=(guint) */
	public int is_modifier;
	public static final int sizeof = GDK.GdkEventKey_sizeof();
}

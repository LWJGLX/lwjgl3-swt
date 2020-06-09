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
package org.eclipse.swt.internal.accessibility.gtk;


public class AtkActionIface {
//	GTypeInterface parent;
	/** @field cast=(gboolean (*)()) */
	public long do_action;
	/** @field cast=(gint (*)()) */
	public long get_n_actions;
	/** @field cast=(const gchar *(*)()) */
	public long get_description;
	/** @field cast=(const gchar *(*)()) */
	public long get_name;
	/** @field cast=(const gchar *(*)()) */
	public long get_keybinding;
	/** @field cast=(gboolean (*)()) */
	public long set_description;
//   AtkFunction             pad1;
//   AtkFunction             pad2;
}


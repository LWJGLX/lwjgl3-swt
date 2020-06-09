/*******************************************************************************
 * Copyright (c) 2010, 2012 IBM Corporation and others. All rights reserved.
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

public class AtkEditableTextIface {
	/** @field cast=(gboolean (*)()) */
	public long set_run_attributes;
	/** @field cast=(void (*)()) */
	public long set_text_contents;
	/** @field cast=(void (*)()) */
	public long insert_text;
	/** @field cast=(void (*)()) */
	public long copy_text;
	/** @field cast=(void (*)()) */
	public long cut_text;
	/** @field cast=(void (*)()) */
	public long delete_text;
	/** @field cast=(void (*)()) */
	public long paste_text;
}

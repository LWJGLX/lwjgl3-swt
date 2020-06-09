/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.swt.internal;

/**
 * Instances of this represent a recursive monitor.  Note that this
 * is an empty implementation which does not actually perform locking.
 */
public class Lock {

/**
 * Locks the monitor and returns the lock count. If
 * the lock is owned by another thread, wait until
 * the lock is released.
 *
 * @return the lock count
 */
public int lock() {
	return 0;
}

/**
 * Unlocks the monitor. If the current thread is not
 * the monitor owner, do nothing.
 */
public void unlock() {
}
}

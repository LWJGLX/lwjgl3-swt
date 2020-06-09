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
package org.eclipse.swt.dnd;

import org.eclipse.swt.internal.cocoa.*;
import org.eclipse.swt.widgets.*;

/**
 * This class provides a default drag under effect (eg. select, insert and scroll)
 * when a drag occurs over a <code>Table</code>.
 *
 * <p>Classes that wish to provide their own drag under effect for a <code>Table</code>
 * can extend the <code>TableDropTargetEffect</code> and override any applicable methods
 * in <code>TableDropTargetEffect</code> to display their own drag under effect.</p>
 *
 * Subclasses that override any methods of this class must call the corresponding
 * <code>super</code> method to get the default drag under effect implementation.
 *
 * <p>The feedback value is either one of the FEEDBACK constants defined in
 * class <code>DND</code> which is applicable to instances of this class,
 * or it must be built by <em>bitwise OR</em>'ing together
 * (that is, using the <code>int</code> "|" operator) two or more
 * of those <code>DND</code> effect constants.
 * </p>
 * <dl>
 * <dt><b>Feedback:</b></dt>
 * <dd>FEEDBACK_SELECT, FEEDBACK_SCROLL</dd>
 * </dl>
 *
 * @see DropTargetAdapter
 * @see DropTargetEvent
 * @see <a href="http://www.eclipse.org/swt/">Sample code and further information</a>
 *
 * @since 3.3
 */
public class TableDropTargetEffect extends DropTargetEffect {
	boolean shouldEnableScrolling;

	/**
	 * Creates a new <code>TableDropTargetEffect</code> to handle the drag under effect on the specified
	 * <code>Table</code>.
	 *
	 * @param table the <code>Table</code> over which the user positions the cursor to drop the data
	 */
	public TableDropTargetEffect(Table table) {
		super(table);
	}

	int checkEffect(int effect) {
		// Some effects are mutually exclusive.  Make sure that only one of the mutually exclusive effects has been specified.
		if ((effect & DND.FEEDBACK_SELECT) != 0) effect = effect & ~DND.FEEDBACK_INSERT_AFTER & ~DND.FEEDBACK_INSERT_BEFORE;
		if ((effect & DND.FEEDBACK_INSERT_BEFORE) != 0) effect = effect & ~DND.FEEDBACK_INSERT_AFTER;
		return effect;
	}

	/**
	 * This implementation of <code>dragEnter</code> provides a default drag under effect
	 * for the feedback specified in <code>event.feedback</code>.
	 *
	 * For additional information see <code>DropTargetAdapter.dragEnter</code>.
	 *
	 * Subclasses that override this method should call <code>super.dragEnter(event)</code>
	 * to get the default drag under effect implementation.
	 *
	 * @param event  the information associated with the drag enter event
	 *
	 * @see DropTargetAdapter
	 * @see DropTargetEvent
	 */
	@Override
	public void dragEnter(DropTargetEvent event) {
	}

	/**
	 * This implementation of <code>dragLeave</code> provides a default drag under effect
	 * for the feedback specified in <code>event.feedback</code>.
	 *
	 * For additional information see <code>DropTargetAdapter.dragLeave</code>.
	 *
	 * Subclasses that override this method should call <code>super.dragLeave(event)</code>
	 * to get the default drag under effect implementation.
	 *
	 * @param event  the information associated with the drag leave event
	 *
	 * @see DropTargetAdapter
	 * @see DropTargetEvent
	 */
	@Override
	public void dragLeave(DropTargetEvent event) {
		if (shouldEnableScrolling) {
			shouldEnableScrolling = false;
			OS.objc_msgSend(control.view.id, OS.sel_setShouldScrollClipView_, 1);
			control.redraw();
		}
	}

	/**
	 * This implementation of <code>dragOver</code> provides a default drag under effect
	 * for the feedback specified in <code>event.feedback</code>. The class description
	 * lists the FEEDBACK constants that are applicable to the class.
	 *
	 * For additional information see <code>DropTargetAdapter.dragOver</code>.
	 *
	 * Subclasses that override this method should call <code>super.dragOver(event)</code>
	 * to get the default drag under effect implementation.
	 *
	 * @param event  the information associated with the drag over event
	 *
	 * @see DropTargetAdapter
	 * @see DropTargetEvent
	 * @see DND#FEEDBACK_SELECT
	 * @see DND#FEEDBACK_SCROLL
	 */
	@Override
	public void dragOver(DropTargetEvent event) {
		int effect = checkEffect(event.feedback);
		((DropTarget)event.widget).feedback = effect;
		if ((effect & DND.FEEDBACK_SCROLL) == 0) {
			shouldEnableScrolling = true;
			OS.objc_msgSend(control.view.id, OS.sel_setShouldScrollClipView_, 0);
		} else {
			OS.objc_msgSend(control.view.id, OS.sel_setShouldScrollClipView_, 1);
		}
	}
}

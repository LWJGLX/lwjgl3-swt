/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.swt.internal.cocoa;

public class NSLayoutManager extends NSObject {

public NSLayoutManager() {
	super();
}

public NSLayoutManager(long id) {
	super(id);
}

public NSLayoutManager(id id) {
	super(id);
}

public void addTemporaryAttribute(NSString attrName, id value, NSRange charRange) {
	OS.objc_msgSend(this.id, OS.sel_addTemporaryAttribute_value_forCharacterRange_, attrName != null ? attrName.id : 0, value != null ? value.id : 0, charRange);
}

public void addTextContainer(NSTextContainer container) {
	OS.objc_msgSend(this.id, OS.sel_addTextContainer_, container != null ? container.id : 0);
}

public NSRect boundingRectForGlyphRange(NSRange glyphRange, NSTextContainer container) {
	NSRect result = new NSRect();
	OS.objc_msgSend_stret(result, this.id, OS.sel_boundingRectForGlyphRange_inTextContainer_, glyphRange, container != null ? container.id : 0);
	return result;
}

public long characterIndexForGlyphAtIndex(long glyphIndex) {
	return OS.objc_msgSend(this.id, OS.sel_characterIndexForGlyphAtIndex_, glyphIndex);
}

public double defaultBaselineOffsetForFont(NSFont theFont) {
	return OS.objc_msgSend_fpret(this.id, OS.sel_defaultBaselineOffsetForFont_, theFont != null ? theFont.id : 0);
}

public double defaultLineHeightForFont(NSFont theFont) {
	return OS.objc_msgSend_fpret(this.id, OS.sel_defaultLineHeightForFont_, theFont != null ? theFont.id : 0);
}

public void drawBackgroundForGlyphRange(NSRange glyphsToShow, NSPoint origin) {
	OS.objc_msgSend(this.id, OS.sel_drawBackgroundForGlyphRange_atPoint_, glyphsToShow, origin);
}

public void drawGlyphsForGlyphRange(NSRange glyphsToShow, NSPoint origin) {
	OS.objc_msgSend(this.id, OS.sel_drawGlyphsForGlyphRange_atPoint_, glyphsToShow, origin);
}

public long getGlyphs(long glyphArray, NSRange glyphRange) {
	return OS.objc_msgSend(this.id, OS.sel_getGlyphs_range_, glyphArray, glyphRange);
}

public long getGlyphsInRange(NSRange glyphRange, long glyphBuffer, long charIndexBuffer, long inscribeBuffer, long elasticBuffer, byte[] bidiLevelBuffer) {
	return OS.objc_msgSend(this.id, OS.sel_getGlyphsInRange_glyphs_characterIndexes_glyphInscriptions_elasticBits_bidiLevels_, glyphRange, glyphBuffer, charIndexBuffer, inscribeBuffer, elasticBuffer, bidiLevelBuffer);
}

public long glyphIndexForCharacterAtIndex(long charIndex) {
	return OS.objc_msgSend(this.id, OS.sel_glyphIndexForCharacterAtIndex_, charIndex);
}

public long glyphIndexForPoint(NSPoint point, NSTextContainer container, double[] partialFraction) {
	return OS.objc_msgSend(this.id, OS.sel_glyphIndexForPoint_inTextContainer_fractionOfDistanceThroughGlyph_, point, container != null ? container.id : 0, partialFraction);
}

public NSRange glyphRangeForCharacterRange(NSRange charRange, long actualCharRange) {
	NSRange result = new NSRange();
	OS.objc_msgSend_stret(result, this.id, OS.sel_glyphRangeForCharacterRange_actualCharacterRange_, charRange, actualCharRange);
	return result;
}

public NSRange glyphRangeForTextContainer(NSTextContainer container) {
	NSRange result = new NSRange();
	OS.objc_msgSend_stret(result, this.id, OS.sel_glyphRangeForTextContainer_, container != null ? container.id : 0);
	return result;
}

public NSRect lineFragmentUsedRectForGlyphAtIndex(long glyphIndex, long effectiveGlyphRange) {
	NSRect result = new NSRect();
	OS.objc_msgSend_stret(result, this.id, OS.sel_lineFragmentUsedRectForGlyphAtIndex_effectiveRange_, glyphIndex, effectiveGlyphRange);
	return result;
}

public NSRect lineFragmentUsedRectForGlyphAtIndex(long glyphIndex, long effectiveGlyphRange, boolean flag) {
	NSRect result = new NSRect();
	OS.objc_msgSend_stret(result, this.id, OS.sel_lineFragmentUsedRectForGlyphAtIndex_effectiveRange_withoutAdditionalLayout_, glyphIndex, effectiveGlyphRange, flag);
	return result;
}

public NSPoint locationForGlyphAtIndex(long glyphIndex) {
	NSPoint result = new NSPoint();
	OS.objc_msgSend_stret(result, this.id, OS.sel_locationForGlyphAtIndex_, glyphIndex);
	return result;
}

public long numberOfGlyphs() {
	return OS.objc_msgSend(this.id, OS.sel_numberOfGlyphs);
}

public long rectArrayForCharacterRange(NSRange charRange, NSRange selCharRange, NSTextContainer container, long[] rectCount) {
	return OS.objc_msgSend(this.id, OS.sel_rectArrayForCharacterRange_withinSelectedCharacterRange_inTextContainer_rectCount_, charRange, selCharRange, container != null ? container.id : 0, rectCount);
}

public long rectArrayForGlyphRange(NSRange glyphRange, NSRange selGlyphRange, NSTextContainer container, long[] rectCount) {
	return OS.objc_msgSend(this.id, OS.sel_rectArrayForGlyphRange_withinSelectedGlyphRange_inTextContainer_rectCount_, glyphRange, selGlyphRange, container != null ? container.id : 0, rectCount);
}

public void removeTemporaryAttribute(NSString attrName, NSRange charRange) {
	OS.objc_msgSend(this.id, OS.sel_removeTemporaryAttribute_forCharacterRange_, attrName != null ? attrName.id : 0, charRange);
}

public void setBackgroundLayoutEnabled(boolean backgroundLayoutEnabled) {
	OS.objc_msgSend(this.id, OS.sel_setBackgroundLayoutEnabled_, backgroundLayoutEnabled);
}

public void setTextStorage(NSTextStorage textStorage) {
	OS.objc_msgSend(this.id, OS.sel_setTextStorage_, textStorage != null ? textStorage.id : 0);
}

public void setUsesScreenFonts(boolean usesScreenFonts) {
	OS.objc_msgSend(this.id, OS.sel_setUsesScreenFonts_, usesScreenFonts);
}

public NSTypesetter typesetter() {
	long result = OS.objc_msgSend(this.id, OS.sel_typesetter);
	return result != 0 ? new NSTypesetter(result) : null;
}

public NSRect usedRectForTextContainer(NSTextContainer container) {
	NSRect result = new NSRect();
	OS.objc_msgSend_stret(result, this.id, OS.sel_usedRectForTextContainer_, container != null ? container.id : 0);
	return result;
}

}

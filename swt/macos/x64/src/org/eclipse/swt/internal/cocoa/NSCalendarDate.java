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

public class NSCalendarDate extends NSDate {

public NSCalendarDate() {
	super();
}

public NSCalendarDate(long id) {
	super(id);
}

public NSCalendarDate(id id) {
	super(id);
}

public static NSCalendarDate calendarDate() {
	long result = OS.objc_msgSend(OS.class_NSCalendarDate, OS.sel_calendarDate);
	return result != 0 ? new NSCalendarDate(result) : null;
}

public static NSCalendarDate dateWithYear(long year, long month, long day, long hour, long minute, long second, NSTimeZone aTimeZone) {
	long result = OS.objc_msgSend(OS.class_NSCalendarDate, OS.sel_dateWithYear_month_day_hour_minute_second_timeZone_, year, month, day, hour, minute, second, aTimeZone != null ? aTimeZone.id : 0);
	return result != 0 ? new NSCalendarDate(result) : null;
}

public long dayOfMonth() {
	return OS.objc_msgSend(this.id, OS.sel_dayOfMonth);
}

public long hourOfDay() {
	return OS.objc_msgSend(this.id, OS.sel_hourOfDay);
}

public long minuteOfHour() {
	return OS.objc_msgSend(this.id, OS.sel_minuteOfHour);
}

public long monthOfYear() {
	return OS.objc_msgSend(this.id, OS.sel_monthOfYear);
}

public long secondOfMinute() {
	return OS.objc_msgSend(this.id, OS.sel_secondOfMinute);
}

public NSTimeZone timeZone() {
	long result = OS.objc_msgSend(this.id, OS.sel_timeZone);
	return result != 0 ? new NSTimeZone(result) : null;
}

public long yearOfCommonEra() {
	return OS.objc_msgSend(this.id, OS.sel_yearOfCommonEra);
}

public static NSCalendarDate dateWithTimeIntervalSinceNow(double secs) {
	long result = OS.objc_msgSend(OS.class_NSCalendarDate, OS.sel_dateWithTimeIntervalSinceNow_, secs);
	return result != 0 ? new NSCalendarDate(result) : null;
}

public static NSDate distantFuture() {
	long result = OS.objc_msgSend(OS.class_NSCalendarDate, OS.sel_distantFuture);
	return result != 0 ? new NSDate(result) : null;
}

}

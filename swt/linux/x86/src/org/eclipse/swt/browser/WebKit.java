/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
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
 *     Red Hat Inc. - generification
 *******************************************************************************/
package org.eclipse.swt.browser;


import static org.eclipse.swt.internal.webkit.WebKitGTK.WEBKIT1;
import static org.eclipse.swt.internal.webkit.WebKitGTK.WEBKIT2;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.charset.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.internal.*;
import org.eclipse.swt.internal.gtk.*;
import org.eclipse.swt.internal.webkit.*;
import org.eclipse.swt.internal.webkit.GdkRectangle;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

/**
 * Webkit2 port developer architecture notes: (Dec 2017)
 * ##########################################
 * I'm (Leo Ufimtsev) writing this as part of the completion of the webkit1->webkit2 port,
 * so that either swt/webkit2 maintainers or maybe webkit3 port-guy has a better understanding
 * of what's going on.
 * I didn't write the initial webkit1 implementation and only wrote half of the webkit2 port. So I don't
 * know the details/reasons behind webkit1 decisions too well, I can only speculate.
 *
 *
 * VERSIONS:
 * Versioning for webkit is somewhat confusing because it's trying to incorporate webkit, gtk and (various linux distribution) versions.
 * The way they version webkitGTK is different from webkit.
 *   WebkitGTK:
 *    1.4 - 2013                         (v1 is  webkit1/Gtk2)
 *    2.4 is the last webkit1 version.   [2.0-2.4) is Gtk3.
 *    2.5 is webkit2.                    [2.4-..)  is Gtk3.
 *  Further, linux distributions might refer to webkit1/2 bindings linked against gtk2/3 differently.
 *  E.g on Fedora:
 *     webkitgtk2 = webkit1 / Gtk2
 *     webkitgtk3 = Webkit1 / Gtk3
 *     webkitgtk4 = webkit2 / Gtk3
 *
 * Webkit1 & Webkit2 loading:
 * - This code dynamically uses either webkit1 or webkit2 depending on what's available.
 * - Dynamic bindings are auto generated and linked when the @dynamic keyword is used in WebKitGTK.java
 *   Unlike in OS.java, you don't have to add any code saying what lib the dynamic method is linked to. It's auto-linked to webkit lib by default.
 * - At no point should you have hard-compiled code, because this will cause crashes on older machines without webkit2.
 *   (the exception is the webextension, because it runs as a separate process and is only loaded dynamically).
 * - Try to keep all of your logic in Java and avoid writing custom C-code. (I went down this pit). Because if you
 *   use native code, then you have to write dynamic native code (get function pointers, cast types etc.. big pain in the ass).
 *   (Webextension is again an exception).
 * - Don't try to add webkit2 include flags to pkg-config, as this will tie the swt-glue code to specific webkit versions. Thou shall not do this.
 *   (webextension is an exception).
 *
 * Webextension:
 * - Webkit1 implemented javascript execution and function callback by calling javascript core directly, but on webkit2 we can't do this anymore since
 *   webkit2 runs the network logic in a separate process.
 * - On Webkit2, a webextension is used to provide browserfunction/javascript callback functionality. (See the whole WebkitGDBus.java business).
 * - I've initially implemented javascript execution by running javascript and then waiting in a display-loop until webkit makes a return call.
 *   I then added a whole bunch of logic to avoid deadlocks.
 *   In retrospec, the better approach would be to send things off via GDBus and let the webextension run the javascript synchronously.
 *   But this would take another 1-2 months of implementation time and wouldn't guarantee dead-lock free behaviour as callbacks could potentailly still
 *   cause deadlocks. It's an interesting thought however..
 * - Note, most GDBus tutorials talk about compiling GDBus bindings. But using them dynamically I found is much easier. See this guide:
 *   http://www.cs.grinnell.edu/~rebelsky/Courses/CSC195/2013S/Outlines/
 *
 *
 * EVENT_HANDLING_DOC:
 * - On Pre-webkit1.4, event handles (mouseMove/Click/keyoard/Dnd etc..) were implemented as javascript hooks.
 * 	  but if javascript was not enabled, hooks would not work either.
 * - On Post-Webkit1.4 gtk provided DOM hooks that generated signals. There still seems to be some strange logic
 *   that either uses javascript or webkitDom events to generate the equivalent SWT.MouseDown/SWT.Key* events etc...
 *   I haven't really taken the time to fully understand why there's such a mix and musch between the two. I just left the code as is.
 * - On webkit2, signals are implemented via regular gtk mechanism, hook events and pass them along as we receive them.
 *   I haven't found a need to use the dom events, because webkitgtk seems to adequately meet the requirements via regular gtk
 *   events, but maybe I missed something? Who knows.
 * - With that said, I haven't done a deep dive/investigation in how things work. It works, so I left it.
 *
 * setUrl(..) with 'post data' was implemented in a very hacky way, via native Java due to missing webkit2gtk api.
 * It's the best that could be done at the time, but it could result in strange behavior like some webpages loading in funky ways if post-data is used.
 *
 * Some good resources that I found are as following:
 * - Webkit1 reference: https://webkitgtk.org/reference/webkitgtk/unstable/
 * - Webkit2 reference: https://webkitgtk.org/reference/webkit2gtk/stable/
 *
 * - My github repository has a lot of snippets to prototype individual features (e.g gdbus, barebone webkit extension, GVariants etc..):
 *   https://github.com/LeoUfimtsev/LeoGtk3
 *   Be also mindful about snippets found in org.eclipse.swt.gtk.linux.x86_64 -> snippets -> widget.browser.
 *
 * - To understand GDBus, consider reading this guide:
 *   http://www.cs.grinnell.edu/~rebelsky/Courses/CSC195/2013S/Outlines/
 *   And then see the relevant reference I made in WebkitGDBus.java.
 *   Note, DBus is not the same as GDBus. GDBus is an implementation of the DBus protocol (with it's own quirks).
 *
 * - This is a good starting point for webkit2 extension reading:
 *   https://blogs.igalia.com/carlosgc/2013/09/10/webkit2gtk-web-process-extensions/
 *
 *   [April 2018]
 *   Note on WebKitContext:
 *    We only use a single webcontext, so WebKitGTK.webkit_web_context_get_default() works well for getting this when
 *    needed.
 *
 *
 *
 * ~May the force be with you.
 */
class WebKit extends WebBrowser {
	/**
	 * WebKitWebView
	 * Note, as of time at compleating webkit2, (18th April 2018, we )
	 */
	int /*long*/ webView;
	int /*long*/ scrolledWindow;
	long pageId;

	/** Webkit1 only. Used by the externalObject for javascript callback to java. */
	int /*long*/ webViewData;

	int failureCount, lastKeyCode, lastCharCode;

	String postData;  // Webkit1 only.
	String[] headers; // Webkit1 only.
	byte[] htmlBytes;  // Webkit1 only.
	boolean loadingText, untrustedText; // Webkit1 only.
	BrowserFunction eventFunction; //Webkit1 only.

	boolean ignoreDispose; // Webkit1 & Webkit2.
	boolean tlsError;
	int /*long*/ tlsErrorCertificate;
	String tlsErrorUriString;
	URI tlsErrorUri;
	String tlsErrorType;

	/**
	 * Timeout used for javascript execution / deadlock detection.
	 * Loosely based on the 10s limit commonly found in browsers.
	 * (Except for SWT browser we use 3s as chunks of the UI is blocked).
	 * https://www.nczonline.net/blog/2009/01/05/what-determines-that-a-script-is-long-running/
	 * https://stackoverflow.com/questions/3030024/maximum-execution-time-for-javascript
	 */
	static final int ASYNC_EXEC_TIMEOUT_MS = 10000; // Webkit2.

	static boolean bug522733FirstInstanceCreated = false; //Webkit2 workaround for Bug 522733

	/** Part of workaround in Bug 527738. Prevent old request overring newer request */
	static AtomicInteger w2_bug527738LastRequestCounter = new AtomicInteger(); // Webkit 2 only (Bug 527738)

	/**
	 * Webkit2: In a few situations, evaluate() should not wait for it's asynchronous callback to finish.
	 * This is to avoid deadlocks, see Bug 512001.<br>
	 * 0 means evaluate should wait for callback. <br>
	 * >0 means evaluate should not block. In this case 'null' is returned. This condition is rare. <br>
	 *
	 * <p>Note: This has to be *static*.
	 * Webkit2 seems to share one event queue, as such two webkit2 instances can interfere with each other.
	 * An example of this interfering is when you open a link in a javadoc hover. The new webkit2 in the new tab
	 * interferes with the old instance in the hoverbox.
	 * As such, any locks should apply to all webkit2 instances.</p>
	 */
	private static int nonBlockingEvaluate = 0;

	static int DisabledJSCount;

	/** Webkit1 only. Used for callJava. See JSObjectHasPropertyProc */
	static int /*long*/ ExternalClass;

	static int /*long*/ PostString, WebViewType;
	static Map<LONG, LONG> WindowMappings = new HashMap<> ();
	static Map<LONG, Integer> webKitDownloadStatus = new HashMap<> (); // Webkit2

	static final String ABOUT_BLANK = "about:blank"; //$NON-NLS-1$
	static final String CLASSNAME_EXTERNAL = "External"; //$NON-NLS-1$
	static final String FUNCTIONNAME_CALLJAVA = "callJava"; //$NON-NLS-1$
	static final String HEADER_CONTENTTYPE = "content-type"; //$NON-NLS-1$
	static final String MIMETYPE_FORMURLENCODED = "application/x-www-form-urlencoded"; //$NON-NLS-1$
	static final String OBJECTNAME_EXTERNAL = "external"; //$NON-NLS-1$
	static final String PROPERTY_LENGTH = "length"; //$NON-NLS-1$
	static final String PROPERTY_PROXYHOST = "network.proxy_host"; //$NON-NLS-1$
	static final String PROPERTY_PROXYPORT = "network.proxy_port"; //$NON-NLS-1$
	static final String PROTOCOL_FILE = "file://"; //$NON-NLS-1$
	static final String PROTOCOL_HTTP = "http://"; //$NON-NLS-1$
	static final String URI_FILEROOT = "file:///"; //$NON-NLS-1$
	static final String USER_AGENT = "user-agent"; //$NON-NLS-1$
	static final int MAX_PORT = 65535;
	static final int MAX_PROGRESS = 100;
	static final int[] MIN_VERSION = {1, 2, 0};
	static final int SENTINEL_KEYPRESS = -1;
	static final char SEPARATOR_FILE = File.separatorChar;
	static final int STOP_PROPOGATE = 1;

	static final String DOMEVENT_DRAGSTART = "dragstart"; //$NON-NLS-1$
	static final String DOMEVENT_KEYDOWN = "keydown"; //$NON-NLS-1$
	static final String DOMEVENT_KEYPRESS = "keypress"; //$NON-NLS-1$
	static final String DOMEVENT_KEYUP = "keyup"; //$NON-NLS-1$
	static final String DOMEVENT_MOUSEDOWN = "mousedown"; //$NON-NLS-1$
	static final String DOMEVENT_MOUSEUP = "mouseup"; //$NON-NLS-1$
	static final String DOMEVENT_MOUSEMOVE = "mousemove"; //$NON-NLS-1$
	static final String DOMEVENT_MOUSEOUT = "mouseout"; //$NON-NLS-1$
	static final String DOMEVENT_MOUSEOVER = "mouseover"; //$NON-NLS-1$
	static final String DOMEVENT_MOUSEWHEEL = "mousewheel"; //$NON-NLS-1$

	/* WebKit signal data */
	static final int HOVERING_OVER_LINK = 1;
	static final int NOTIFY_PROGRESS = 2;
	static final int NAVIGATION_POLICY_DECISION_REQUESTED = 3;
	static final int NOTIFY_TITLE = 4;
	static final int POPULATE_POPUP = 5;
	static final int STATUS_BAR_TEXT_CHANGED = 6; // webkit1 only.
	static final int CREATE_WEB_VIEW = 7;
	static final int WEB_VIEW_READY = 8;
	static final int NOTIFY_LOAD_STATUS = 9;
	static final int RESOURCE_REQUEST_STARTING = 10;
	static final int DOWNLOAD_REQUESTED = 11; // Webkit1
	static final int MIME_TYPE_POLICY_DECISION_REQUESTED = 12;
	static final int CLOSE_WEB_VIEW = 13;
	static final int WINDOW_OBJECT_CLEARED = 14;
	static final int CONSOLE_MESSAGE = 15;
	static final int LOAD_CHANGED = 16;
	static final int DECIDE_POLICY = 17;
	static final int MOUSE_TARGET_CHANGED = 18;
	static final int CONTEXT_MENU = 19;
	static final int AUTHENTICATE = 20;
	static final int DECIDE_DESTINATION = 21; // webkit2 only.
	static final int FAILED = 22; // webkit2 only.
	static final int FINISHED = 23; // webkit2 only.
	static final int DOWNLOAD_STARTED = 24;   // Webkit2 (webkit1 equivalent is DOWNLOAD_REQUESTED)
	static final int WIDGET_EVENT = 25;		// Webkit2. Used for events like keyboard/mouse input. See Bug 528549 and Bug 533833.
	static final int LOAD_FAILED_TLS = 26; // Webkit2 only

	static final String KEY_CHECK_SUBWINDOW = "org.eclipse.swt.internal.control.checksubwindow"; //$NON-NLS-1$

	static final String SWT_WEBKITGTK_VERSION = "org.eclipse.swt.internal.webkitgtk.version"; //$NON-NLS-1$

	/* the following Callbacks are never freed */
	static Callback Proc2, Proc3, Proc4, Proc5, Proc6;


	/**
	 * Webkit1  only: For javascript to call java via it's 'callJava'.
	 * For webkit2, see Webkit2JavaCallback.
	 *
	 * Webkit1: - callJava is implemented via an external object
	 * - Creates an object 'external' on javascipt side.
	 * 	 -- see create(..) where it's initialized
	 *   -- see webkit_window_object_cleared where it re-creates it on page-reloads
	 * - Javascript will call 'external.callJava' (where callJava is a property of 'external').
	 *    this triggers JSObjectGetPropertyProc(..) callback, which initializes callJava function.
	 *    Then the external.callJava reaches JSObjectCallAsFunctionProc(..) and subsequently WebKit.java:callJava(..) is called.
	 */
	static Callback JSObjectHasPropertyProc, JSObjectGetPropertyProc, JSObjectCallAsFunctionProc; // webkit1 only.

	/** Webkit1 & Webkit2, Process key/mouse events from javascript. */
	static Callback JSDOMEventProc;

	/** Flag indicating whether TLS errors (like self-signed certificates) are to be ignored. Webkit2 only.*/
	static final boolean ignoreTls;

	static {
			WebViewType = WebKitGTK.webkit_web_view_get_type ();
			Proc2 = new Callback (WebKit.class, "Proc", 2); //$NON-NLS-1$
			if (Proc2.getAddress () == 0) SWT.error (SWT.ERROR_NO_MORE_CALLBACKS);
			Proc3 = new Callback (WebKit.class, "Proc", 3); //$NON-NLS-1$
			if (Proc3.getAddress () == 0) SWT.error (SWT.ERROR_NO_MORE_CALLBACKS);
			Proc4 = new Callback (WebKit.class, "Proc", 4); //$NON-NLS-1$
			if (Proc4.getAddress () == 0) SWT.error (SWT.ERROR_NO_MORE_CALLBACKS);
			Proc5 = new Callback (WebKit.class, "Proc", 5); //$NON-NLS-1$
			if (Proc5.getAddress () == 0) SWT.error (SWT.ERROR_NO_MORE_CALLBACKS);
			Proc6 = new Callback (WebKit.class, "Proc", 6); //$NON-NLS-1$
			if (Proc6.getAddress () == 0) SWT.error (SWT.ERROR_NO_MORE_CALLBACKS);

			if (WEBKIT2) {
				new Webkit2AsyncToSync();
			}

			if (WEBKIT2) {
				Webkit2Extension.init();
			} else {
				JSObjectHasPropertyProc = new Callback (WebKit.class, "JSObjectHasPropertyProc", 3); //$NON-NLS-1$
				if (JSObjectHasPropertyProc.getAddress () == 0) SWT.error (SWT.ERROR_NO_MORE_CALLBACKS);
				JSObjectGetPropertyProc = new Callback (WebKit.class, "JSObjectGetPropertyProc", 4); //$NON-NLS-1$
				if (JSObjectGetPropertyProc.getAddress () == 0) SWT.error (SWT.ERROR_NO_MORE_CALLBACKS);
				JSObjectCallAsFunctionProc = new Callback (WebKit.class, "JSObjectCallAsFunctionProc", 6); //$NON-NLS-1$
				if (JSObjectCallAsFunctionProc.getAddress () == 0) SWT.error (SWT.ERROR_NO_MORE_CALLBACKS);
			}

			JSDOMEventProc = new Callback (WebKit.class, "JSDOMEventProc", 3); //$NON-NLS-1$
			if (JSDOMEventProc.getAddress () == 0) SWT.error (SWT.ERROR_NO_MORE_CALLBACKS);

			NativeClearSessions = () -> {
				if (!WebKitGTK.LibraryLoaded) return;

				if (WEBKIT2) {
					if (WebKitGTK.webkit_get_minor_version() >= 16) {
						// TODO: webkit_website_data_manager_clear currently does not
						 // support more fine grained removals. (I.e, session vs all cookies)
						int /*long*/ context = WebKitGTK.webkit_web_context_get_default();
						int /*long*/ manager = WebKitGTK.webkit_web_context_get_website_data_manager (context);
						WebKitGTK.webkit_website_data_manager_clear(manager, WebKitGTK.WEBKIT_WEBSITE_DATA_COOKIES, 0, 0, 0, 0);
					} else {
						System.err.println("SWT Webkit. Warning, clear cookies only supported on Webkitgtk version 2.16 and above. Your version is:" + internalGetWebKitVersionStr());
					}
				} else {
					int /*long*/ session = WebKitGTK.webkit_get_default_session ();
					int /*long*/ type = WebKitGTK.soup_cookie_jar_get_type ();
					int /*long*/ jar = WebKitGTK.soup_session_get_feature (session, type);
					if (jar == 0) return;
					int /*long*/ cookies = WebKitGTK.soup_cookie_jar_all_cookies (jar);
					int length = OS.g_slist_length (cookies);
					int /*long*/ current = cookies;
					for (int i = 0; i < length; i++) {
						int /*long*/ cookie = OS.g_slist_data (current);
						int /*long*/ expires = WebKitGTK.SoupCookie_expires (cookie);
						if (expires == 0) {
							/* indicates a session cookie */
							WebKitGTK.soup_cookie_jar_delete_cookie (jar, cookie);
						}
						WebKitGTK.soup_cookie_free (cookie);
						current = OS.g_slist_next (current);
					}
					OS.g_slist_free (cookies);
				}

			};

			NativeGetCookie = () -> {
				if (!WebKitGTK.LibraryLoaded) return;

				if (WEBKIT2) {
					// TODO - implement equivalent. Bug 522181
					// Currently 'webkit_get_default_session()' is a webkit1-only function.
					// If it's reached by webkit2, the whole JVM crashes. Better skip for now.
					return;
				}

				int /*long*/ session = WebKitGTK.webkit_get_default_session ();
				int /*long*/ type = WebKitGTK.soup_cookie_jar_get_type ();
				int /*long*/ jar = WebKitGTK.soup_session_get_feature (session, type);
				if (jar == 0) return;
				byte[] bytes = Converter.wcsToMbcs (CookieUrl, true);
				int /*long*/ uri = WebKitGTK.soup_uri_new (bytes);
				if (uri == 0) return;
				int /*long*/ cookies = WebKitGTK.soup_cookie_jar_get_cookies (jar, uri, 0);
				WebKitGTK.soup_uri_free (uri);
				if (cookies == 0) return;
				int length = C.strlen (cookies);
				bytes = new byte[length];
				C.memmove (bytes, cookies, length);
				OS.g_free (cookies);
				String allCookies = new String (Converter.mbcsToWcs (bytes));
				StringTokenizer tokenizer = new StringTokenizer (allCookies, ";"); //$NON-NLS-1$
				while (tokenizer.hasMoreTokens ()) {
					String cookie = tokenizer.nextToken ();
					int index = cookie.indexOf ('=');
					if (index != -1) {
						String name = cookie.substring (0, index).trim ();
						if (name.equals (CookieName)) {
							CookieValue = cookie.substring (index + 1).trim ();
							return;
						}
					}
				}
			};

			NativeSetCookie = () -> {
				if (!WebKitGTK.LibraryLoaded) return;

				if (WEBKIT2) {
					// TODO - implement equivalent. Bug 522181
					// Currently 'webkit_get_default_session()' is a webkit1-only function.
					// If it's reached by webkit2, the whole JVM crashes. Better skip for now.
					return;
				}

				int /*long*/ session = WebKitGTK.webkit_get_default_session ();
				int /*long*/ type = WebKitGTK.soup_cookie_jar_get_type ();
				int /*long*/ jar = WebKitGTK.soup_session_get_feature (session, type);
				if (jar == 0) {
					/* this happens if a navigation has not occurred yet */
					WebKitGTK.soup_session_add_feature_by_type (session, type);
					jar = WebKitGTK.soup_session_get_feature (session, type);
				}
				if (jar == 0) return;
				byte[] bytes = Converter.wcsToMbcs (CookieUrl, true);
				int /*long*/ uri = WebKitGTK.soup_uri_new (bytes);
				if (uri == 0) return;
				bytes = Converter.wcsToMbcs (CookieValue, true);
				int /*long*/ cookie = WebKitGTK.soup_cookie_parse (bytes, uri);
				if (cookie != 0) {
					WebKitGTK.soup_cookie_jar_add_cookie (jar, cookie);
					// the following line is intentionally commented
					// WebKitGTK.soup_cookie_free (cookie);
					CookieResult = true;
				}
				WebKitGTK.soup_uri_free (uri);
			};

			if (NativePendingCookies != null) {
				SetPendingCookies (NativePendingCookies);
				NativePendingCookies = null;
			}
			ignoreTls = WEBKIT2 && "true".equals(System.getProperty("org.eclipse.swt.internal.webkitgtk.ignoretlserrors"));
	}

	@Override
	public void createFunction(BrowserFunction function) {
		if (WEBKIT2) {
			if (!WebkitGDBus.initialized) {
				System.err.println("SWT webkit: WebkitGDBus and/or Webkit2Extension not loaded, BrowserFunction will not work." +
					"Tried to create "+ function.name);
				return;
			}
		}
		super.createFunction(function);
		if (WEBKIT2) {
			String url = this.getUrl().isEmpty() ? "nullURL" : this.getUrl();
			/*
			 * If the proxy to the extension has not yet been loaded, store the BrowserFunction page ID,
			 * function string, and URL in a HashMap. Once the proxy to the extension is loaded, these
			 * functions will be sent to and registered in the extension.
			 */
			if (!WebkitGDBus.proxyToExtension) {
				WebkitGDBus.functionsPending = true;
				ArrayList<ArrayList<String>> list = new ArrayList<>();
				ArrayList<String> functionAndUrl = new ArrayList<>();
				functionAndUrl.add(0, function.functionString);
				functionAndUrl.add(1, url);
				list.add(functionAndUrl);
				ArrayList<ArrayList<String>> existing = WebkitGDBus.pendingBrowserFunctions.putIfAbsent(this.pageId, list);
				if (existing != null) {
					existing.add(functionAndUrl);
				}
			} else {
				// If the proxy to the extension is already loaded, register the function in the extension via DBus
				boolean successful = webkit_extension_modify_function(this.pageId, function.functionString, url, "register");
				if (!successful) {
					System.err.println("SWT webkit: failure registering BrowserFunction " + function.name);
				}
			}
		}
	}

	@Override
	public void destroyFunction (BrowserFunction function) {
		// Only deregister functions if the proxy to the extension has been loaded
		if (WebkitGDBus.proxyToExtension && WEBKIT2) {
			String url = this.getUrl().isEmpty() ? "nullURL" : this.getUrl();
			boolean successful = webkit_extension_modify_function(this.pageId, function.functionString, url, "deregister");
			if (!successful) {
				System.err.println("SWT webkit: failure deregistering BrowserFunction from extension " + function.name);
			}
		}
		super.destroyFunction(function);
	}

	private static String getInternalErrorMsg () {
		String reportErrMsg = "Please report this issue *with steps to reproduce* via:\n"
				+ " https://bugs.eclipse.org/bugs/enter_bug.cgi?"
				+ "alias=&assigned_to=platform-swt-inbox%40eclipse.org&attach_text=&blocked=&bug_file_loc=http%3A%2F%2F&bug_severity=normal"
				+ "&bug_status=NEW&comment=&component=SWT&contenttypeentry=&contenttypemethod=autodetect&contenttypeselection=text%2Fplain"
				+ "&data=&defined_groups=1&dependson=&description=&flag_type-1=X&flag_type-11=X&flag_type-12=X&flag_type-13=X&flag_type-14=X"
				+ "&flag_type-15=X&flag_type-16=X&flag_type-2=X&flag_type-4=X&flag_type-6=X&flag_type-7=X&flag_type-8=X&form_name=enter_bug"
				+ "&keywords=&maketemplate=Remember%20values%20as%20bookmarkable%20template&op_sys=Linux&product=Platform&qa_contact="
				+ "&rep_platform=PC&requestee_type-1=&requestee_type-2=&short_desc=webkit2_BrowserProblem";

		return reportErrMsg + "\nFor bug report, please atatch this stack trace:\n" + getStackTrace();
	}


	private static String getStackTrace() {
		// Get a stacktrace. Note, this doesn't actually throw anything, we just get the stacktrace.
		StringWriter sw = new StringWriter();
		new Throwable("").printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}

	/**
	 * This class deals with the Webkit2 extension.
	 *
	 * Extension is separately loaded and deals Javascript callbacks to Java.
	 * Extension is needed so that Javascript can receive a return value from Java
	 * (for which currently there is no api in WebkitGtk 2.18)
	 */
	static class Webkit2Extension {
		/** Note, if updating this, you need to change it also in webkitgtk_extension.c */
		private static final String javaScriptFunctionName = "webkit2callJava";  // $NON-NLS-1$
		private static final String webkitWebExtensionIdentifier = "webkitWebExtensionIdentifer";  // $NON-NLS-1$
		private static Callback initializeWebExtensions_callback;
		private static int uniqueID = OS.getpid();

		/**
		 * Don't continue initialization if something failed. This allows Browser to carryout some functionality
		 * even if the webextension failed to load.
		 */
		private static boolean loadFailed;

		static String getJavaScriptFunctionName() {
			return javaScriptFunctionName;
		}
		static String getWebExtensionIdentifer() {
			return webkitWebExtensionIdentifier;
		}
		static String getJavaScriptFunctionDeclaration(int /*long*/ webView) {
			return "if (!window.callJava) {\n"
			+ "		window.callJava = function callJava(index, token, args) {\n"
			+ "          return " + javaScriptFunctionName + "('" + String.valueOf(webView) +  "', index, token, args);\n"
			+ "		}\n"
			+ "};\n";
		}

		static void init() {
			/*
			 * Initialize GDBus before the extension, as the extension initialization callback at the C level
			 * sends data back to SWT via GDBus. Failure to load GDBus here will result in crashes.
			 * See bug 536141.
			 */
			gdbus_init();
			initializeWebExtensions_callback = new Callback(Webkit2Extension.class, "initializeWebExtensions_callback", void.class, new Type [] {long.class, long.class});
			if (initializeWebExtensions_callback.getAddress() == 0) SWT.error (SWT.ERROR_NO_MORE_CALLBACKS);
			if (WebKitGTK.webkit_get_minor_version() >= 4) { // Callback exists only since 2.04
				OS.g_signal_connect (WebKitGTK.webkit_web_context_get_default(), WebKitGTK.initialize_web_extensions, initializeWebExtensions_callback.getAddress(), 0);
			}
		}

		/**
		 * GDbus initialization can cause performance slow downs. So we int GDBus in lazy way.
		 * It can be initialized upon first use of BrowserFunction.
		 */
		static boolean gdbus_init() {
			if (WebKitGTK.webkit_get_minor_version() < 4) {
				System.err.println("SWT Webkit: Warning, You are using an old version of webkitgtk. (pre 2.4)"
						+ " BrowserFunction functionality will not be avaliable");
				return false;
			}


			if (!loadFailed) {
				WebkitGDBus.init(String.valueOf(uniqueID));
				return true;
			} else {
				return false;
			}
		}

		/**
		 * This callback is called to initialize webextension.
		 * It is the optimum place to set extension directory and set initialization user data.
		 *
		 * I've experimented with loading webextension later (to see if we can get performance gains),
		 * but found breakage. Webkitgtk doc says it should be loaded as early as possible and specifically best
		 * to do it in this calllback.
		 *
		 * See documenation: WebKitWebExtension (Description)
		 */
		@SuppressWarnings("unused") // Only called directly from C
		private static void initializeWebExtensions_callback (int /*long*/ WebKitWebContext, int /*long*/ user_data) {
			// 1) GDBus:
			// Normally we'd first initialize gdbus channel. But gdbus makes Browser slower and isn't always needed.
			// So WebkitGDBus is lazy-initialized, although it can be initialized here if gdbus is ever needed
			// for more than BrowserFunction, like:
			// WebkitGDBus.init(String.valueOf(uniqueID));
			// Also consider only loading gdbus if the extension initialized properly.

			// 2) Load Webkit Extension:
			// Webkit extensions should be in their own directory.
			String swtVersion = Library.getVersionString();
			File extension;
			try {
				extension = Library.findResource("webkitextensions" + swtVersion ,"swt-webkit2extension", true);
				if (extension == null){
					throw new UnsatisfiedLinkError("SWT Webkit could not find it's webextension");
				}
			} catch (UnsatisfiedLinkError e) {
				System.err.println("SWT Webkit.java Error: Could not find webkit extension. BrowserFunction functionality will not be available. \n"
						+ "(swt version: " + swtVersion + ")" + WebKitGTK.swtWebkitGlueCodeVersion + WebKitGTK.swtWebkitGlueCodeVersionInfo);
				int [] vers = internalGetWebkitVersion();
				System.err.println(String.format("WebKit2Gtk version %s.%s.%s", vers[0], vers[1], vers[2]));
				System.err.println(getInternalErrorMsg());
				loadFailed = true;
				return;
			}

			String extensionsFolder = extension.getParent();
			/* Dev note:
			 * As per
			 * - WebkitSrc: WebKitExtensionManager.cpp,
			 * - IRC discussion with annulen
			 * you cannot load the webextension GModule directly, (webkitgtk 2.18). You can only specify directory and user data.
			 * So we need to treat this  '.so' in a special way.
			 * (as a note, the webprocess would have to load the gmodule).
			 */
			WebKitGTK.webkit_web_context_set_web_extensions_directory(WebKitGTK.webkit_web_context_get_default(), Converter.wcsToMbcs (extensionsFolder, true));
			int /*long*/ gvariantUserData = OS.g_variant_new_int32(uniqueID);
			WebKitGTK.webkit_web_context_set_web_extensions_initialization_user_data(WebKitGTK.webkit_web_context_get_default(), gvariantUserData);
		}

		/**
		 * @param cb_args Raw callback arguments by function.
		 */
		static Object webkit2callJavaCallback(Object [] cb_args) {
			assert cb_args.length == 4;
			Object returnValue = null;
			Long webViewLocal = (Double.valueOf((String) cb_args[0])).longValue();
			Browser browser = FindBrowser((int /*long*/) webViewLocal.longValue());
			Integer functionIndex = ((Double) cb_args[1]).intValue();
			String token = (String) cb_args[2];

			BrowserFunction function = browser.webBrowser.functions.get(functionIndex);
			if (function == null) {
				System.err.println("SWT Webkit Error: Failed to find function with index: " + functionIndex);
				return null;
			}
			if (!function.token.equals(token)) {
				System.err.println("SWT Webkit Error: token mismatch for function with index: " + functionIndex);
				return null;
			}
			try {
				// Call user code. Exceptions can occur.
				nonBlockingEvaluate++;
				Object [] user_args = (Object []) cb_args[3];
				returnValue = function.function(user_args);
			} catch (Exception e ) {
				// - Something went wrong in user code.
				// - Dev note, webkit1 uses a browserFunction and function.isEvaluate for evaluate(),
				//   webkit2 doesn't, so we don't have 'if (function.isEvaluate)' logic here.
				System.err.println("SWT Webkit: Exception occured in user code of function: " + function.name);
				returnValue = WebBrowser.CreateErrorString (e.getLocalizedMessage ());
			} finally {
				nonBlockingEvaluate--;
			}
			return returnValue;
		}
	}

	@Override
	String getJavaCallDeclaration() {
		if (WEBKIT2) {
			return Webkit2Extension.getJavaScriptFunctionDeclaration(webView);
		} else {
			return super.getJavaCallDeclaration();
		}
	}

	/**
	 * Gets the webkit version, within an <code>int[3]</code> array with
	 * <code>{major, minor, micro}</code> version
	 */
	private static int[] internalGetWebkitVersion(){
		int [] vers = new int[3];
		if (WEBKIT2){
			vers[0] = WebKitGTK.webkit_get_major_version ();
			vers[1] = WebKitGTK.webkit_get_minor_version ();
			vers[2] = WebKitGTK.webkit_get_micro_version ();
		} else {
			vers[0] = WebKitGTK.webkit_major_version ();
			vers[1] = WebKitGTK.webkit_minor_version ();
			vers[2] = WebKitGTK.webkit_micro_version ();
		}
		return vers;
	}

	private static String internalGetWebKitVersionStr () {
		int [] vers = internalGetWebkitVersion();
		return String.valueOf(vers[0]) + "." + String.valueOf(vers[1]) + "." + String.valueOf(vers[2]);
	}


static String getString (int /*long*/ strPtr) {
	int length = C.strlen (strPtr);
	byte [] buffer = new byte [length];
	C.memmove (buffer, strPtr, length);
	return new String (Converter.mbcsToWcs (buffer));
}

static Browser FindBrowser (int /*long*/ webView) {
	if (webView == 0) return null;
	int /*long*/ parent = GTK.gtk_widget_get_parent (webView);
	if (WEBKIT1){
		parent = GTK.gtk_widget_get_parent (parent);
	}
	return (Browser)Display.getCurrent ().findWidget (parent);
}

static boolean IsInstalled () {
	if (!WebKitGTK.LibraryLoaded) return false;
	// TODO webkit_check_version() should take care of the following, but for some
	// reason this symbol is missing from the latest build.  If it is present in
	// Linux distro-provided builds then replace the following with this call.
	int [] vers = internalGetWebkitVersion();
	int major = vers[0], minor = vers[1], micro = vers[2];
	return major > MIN_VERSION[0] ||
		(major == MIN_VERSION[0] && minor > MIN_VERSION[1]) ||
		(major == MIN_VERSION[0] && minor == MIN_VERSION[1] && micro >= MIN_VERSION[2]);
}

/**
 * Webkit1 callback. Used when external.callJava is called in javascript.
 * Not used by Webkit2.
 */
static int /*long*/ JSObjectCallAsFunctionProc (int /*long*/ ctx, int /*long*/ function, int /*long*/ thisObject, int /*long*/ argumentCount, int /*long*/ arguments, int /*long*/ exception) {
	if (WEBKIT2) {
		System.err.println("Internal error: SWT JSObjectCallAsFunctionProc. This should never have been called on webkit2.");
		return 0;
	}

	if (WebKitGTK.JSValueIsObjectOfClass (ctx, thisObject, ExternalClass) == 0) {
		return WebKitGTK.JSValueMakeUndefined (ctx);
	}
	int /*long*/ ptr = WebKitGTK.JSObjectGetPrivate (thisObject);
	int /*long*/[] handle = new int /*long*/[1];
	C.memmove (handle, ptr, C.PTR_SIZEOF);
	Browser browser = FindBrowser (handle[0]);
	if (browser == null) return 0;
	WebKit webkit = (WebKit)browser.webBrowser;
	return webkit.callJava (ctx, function, thisObject, argumentCount, arguments, exception);
}

/**
 * This callback is only being ran by webkit1. Only for 'callJava'.
 * It's used to initialize the 'callJava' function pointer in the 'external' object,
 * such that external.callJava reaches Java land.
 */
static int /*long*/ JSObjectGetPropertyProc (int /*long*/ ctx, int /*long*/ object, int /*long*/ propertyName, int /*long*/ exception) {
	if (WEBKIT2) {
		System.err.println("Internal error: SWT WebKit.java:JSObjectGetPropertyProc. This should never have been called on webkit2.");
		return 0;
	}
	byte[] bytes = (FUNCTIONNAME_CALLJAVA + '\0').getBytes (StandardCharsets.UTF_8); //$NON-NLS-1$
	int /*long*/ name = WebKitGTK.JSStringCreateWithUTF8CString (bytes);
	int /*long*/ function = WebKitGTK.JSObjectMakeFunctionWithCallback (ctx, name, JSObjectCallAsFunctionProc.getAddress ());
	WebKitGTK.JSStringRelease (name);
	return function;
}

/**
 * Webkit1: Check if the 'external' object regiseterd earlied has the 'callJava' property.
 */
static int /*long*/ JSObjectHasPropertyProc (int /*long*/ ctx, int /*long*/ object, int /*long*/ propertyName) {
	if (WEBKIT2) {
		System.err.println("Internal error: SWT JSObjectHasPropertyProc. This should never have been called on webkit2.");
		return 0;
	}
	byte[] bytes = (FUNCTIONNAME_CALLJAVA + '\0').getBytes (StandardCharsets.UTF_8); //$NON-NLS-1$
	return WebKitGTK.JSStringIsEqualToUTF8CString (propertyName, bytes);
}

static int /*long*/ JSDOMEventProc (int /*long*/ arg0, int /*long*/ event, int /*long*/ user_data) {
	if (WEBKIT1 && GTK.GTK_IS_SCROLLED_WINDOW (arg0)) {
		/*
		 * Stop the propagation of events that are not consumed by WebKit, before
		 * they reach the parent embedder.  These events have already been received.
		 */
		return user_data;
	}

	// G_TYPE_CHECK_INSTANCE_TYPE is a bad way to check type. See OS.G_TYPE_CHECK_INSTANCE_TYPE.
	// But kept for webkit1 legacy reason. Don't use G_TYPE_CHECK_INSTANCE_TYPE in new code.
	if ((WEBKIT1 && OS.G_TYPE_CHECK_INSTANCE_TYPE (arg0, WebViewType))
		|| (WEBKIT2 && user_data == WIDGET_EVENT)) {
		/*
		* Only consider using GDK events to create SWT events to send if JS is disabled
		* in one or more WebKit instances (indicates that this instance may not be
		* receiving events from the DOM).  This check is done up-front for performance.
		*/
		if ((WEBKIT1 && DisabledJSCount > 0) || WEBKIT2){
			final Browser browser = FindBrowser (arg0);
			if (browser != null &&
					(WEBKIT1 && !browser.webBrowser.jsEnabled)
					|| (WEBKIT2 && user_data == WIDGET_EVENT)){
				/* this instance does need to use the GDK event to create an SWT event to send */
				switch (GDK.GDK_EVENT_TYPE (event)) {
					case GDK.GDK_KEY_PRESS: {
						if (browser.isFocusControl ()) {
							final GdkEventKey gdkEvent = new GdkEventKey ();
							OS.memmove (gdkEvent, event, GdkEventKey.sizeof);
							switch (gdkEvent.keyval) {
								case GDK.GDK_ISO_Left_Tab:
								case GDK.GDK_Tab: {
									if ((gdkEvent.state & (GDK.GDK_CONTROL_MASK | GDK.GDK_MOD1_MASK)) == 0) {
										browser.getDisplay ().asyncExec (() -> {
											if (browser.isDisposed ()) return;
											if (browser.getDisplay ().getFocusControl () == null) {
												int traversal = (gdkEvent.state & GDK.GDK_SHIFT_MASK) != 0 ? SWT.TRAVERSE_TAB_PREVIOUS : SWT.TRAVERSE_TAB_NEXT;
												browser.traverse (traversal);
											}
										});
									}
									break;
								}
								case GDK.GDK_Escape: {
									Event keyEvent = new Event ();
									keyEvent.widget = browser;
									keyEvent.type = SWT.KeyDown;
									keyEvent.keyCode = keyEvent.character = SWT.ESC;
									if ((gdkEvent.state & GDK.GDK_MOD1_MASK) != 0) keyEvent.stateMask |= SWT.ALT;
									if ((gdkEvent.state & GDK.GDK_SHIFT_MASK) != 0) keyEvent.stateMask |= SWT.SHIFT;
									if ((gdkEvent.state & GDK.GDK_CONTROL_MASK) != 0) keyEvent.stateMask |= SWT.CONTROL;
									try { // to avoid deadlocks, evaluate() should not block during listener. See Bug 512001
										  // I.e, evaluate() can be called and script will be executed, but no return value will be provided.
										nonBlockingEvaluate++;
									browser.webBrowser.sendKeyEvent (keyEvent);
									} catch (Exception e) {
										throw e;
									} finally {
										nonBlockingEvaluate--;
									}
									return 1;
								}
							}
						}
						break;
					}
				}
				if (WEBKIT1 || (WEBKIT2 && browser != null)) {
					GTK.gtk_widget_event (browser.handle, event);
				}
			}
		}
		return 0;
	}

	if (WEBKIT1) {
		LONG webViewHandle = WindowMappings.get (new LONG (arg0));
		if (webViewHandle == null) return 0;
		Browser browser = FindBrowser (webViewHandle.value);
		if (browser == null) return 0;
		WebKit webkit = (WebKit)browser.webBrowser;
		if (user_data == WIDGET_EVENT) {
			user_data = 0; // legacy.
		}
		return webkit.handleDOMEvent (event, (int)user_data) ? 0 : STOP_PROPOGATE;
	}

	// Webkit2
	return 0;
}

static int /*long*/ Proc (int /*long*/ handle, int /*long*/ user_data) {
	int /*long*/ webView  = handle;

	if (WEBKIT2 && user_data == FINISHED) {
		// Special case, callback from WebKitDownload instead of webview.
		int /*long*/ webKitDownload = handle;
		return webkit_download_finished(webKitDownload);
	}

	Browser browser = FindBrowser (webView);
	if (browser == null) return 0;
	WebKit webkit = (WebKit)browser.webBrowser;
	return webkit.webViewProc (handle, user_data);
}

static int /*long*/ Proc (int /*long*/ handle, int /*long*/ arg0, int /*long*/ user_data) {
	// As a note, don't use instance checks like 'G_TYPE_CHECK_INSTANCE_TYPE '
	// to determine difference between webview and webcontext as these
	// don't seem to work reliably for all clients. For some clients they always return true.
	// Instead use user_data.

	{ // Deal with Special cases where callback comes not from webview. Handle is not a webview.
		if (WEBKIT1 && user_data == NOTIFY_LOAD_STATUS) {
			// Webkit1 vs 2 note:
			// Notion of 'Webkit frame' is webkit1 port specific. In webkit2 port, web frames are a webextension and aren't used.
			// Special case to handle webkit1 webview notify::load-status. Handle is a webframe not a webview.
			// Note, G_TYPE_CHECK_INSTANCE_TYPE is not a good way to test for type. See it's javadoc.
			//   only kept for webkit1 legacy reason.
			if (OS.G_TYPE_CHECK_INSTANCE_TYPE (handle, WebKitGTK.webkit_web_frame_get_type ())) {
				int /*long*/ webView = WebKitGTK.webkit_web_frame_get_web_view (handle); // webkit1 only.
				Browser browser = FindBrowser (webView);
				if (browser == null) return 0;
				WebKit webkit = (WebKit)browser.webBrowser;
				return webkit.webframe_notify_load_status(handle, arg0);
			}
		}

		if (WEBKIT2 && user_data == DOWNLOAD_STARTED) {
			// This callback comes from WebKitWebContext as oppose to the WebView. So handle is WebContext not Webview.
			// user_function (WebKitWebContext *context, WebKitDownload  *download,  gpointer  user_data)
			int /*long*/ webKitDownload = arg0;
			webkit_download_started(webKitDownload);
			return 0;
		}

		 if (WEBKIT2 && user_data == DECIDE_DESTINATION) {
			// This callback comes from WebKitDownload, so handle is WebKitDownload not webview.
			// gboolean  user_function (WebKitDownload *download, gchar   *suggested_filename, gpointer  user_data)
			int /*long*/ webKitDownload = handle;
			int /*long*/ suggested_filename = arg0;
			return webkit_download_decide_destination(webKitDownload,suggested_filename);
		}

		if (WEBKIT2 && user_data == FAILED) {
			// void user_function (WebKitDownload *download, GError *error, gpointer user_data)
			int /*long*/ webKitDownload = handle;
			return webkit_download_failed(webKitDownload);
		 }
	}

	{ // Callbacks connected with a WebView.
		assert handle != 0 : "Webview shouldn't be null here";
		int /*long*/ webView = handle;
		Browser browser = FindBrowser (webView);
		if (browser == null) return 0;
		WebKit webkit = (WebKit)browser.webBrowser;
		return webkit.webViewProc (webView, arg0, user_data);
	}
}

static int /*long*/ Proc (int /*long*/ handle, int /*long*/ arg0, int /*long*/ arg1, int /*long*/ user_data) {
	Browser browser = FindBrowser (handle);
	if (browser == null) return 0;
	WebKit webkit = (WebKit)browser.webBrowser;
	return webkit.webViewProc (handle, arg0, arg1, user_data);
}

static int /*long*/ Proc (int /*long*/ handle, int /*long*/ arg0, int /*long*/ arg1, int /*long*/ arg2, int /*long*/ user_data) {
	int /*long*/ webView;

	// Note: G_TYPE_CHECK_INSTANCE_TYPE is not a good way to check for instance type, see it's javadoc.
	// Kept only for webkit1 legacy reasons. Do not use in new code.
	if (WEBKIT1 && OS.G_TYPE_CHECK_INSTANCE_TYPE (handle, WebKitGTK.soup_session_get_type ())) {
		webView = user_data;
	} else {
		webView = handle;
	}
	Browser browser = FindBrowser (webView);
	if (browser == null) return 0;
	WebKit webkit = (WebKit)browser.webBrowser;

	if (WEBKIT1 && webView == user_data) {
		return webkit.sessionProc (handle, arg0, arg1, arg2, user_data); // Webkit1's way of authentication.
	} else {
		return webkit.webViewProc (handle, arg0, arg1, arg2, user_data);
	}
}

static int /*long*/ Proc (int /*long*/ handle, int /*long*/ arg0, int /*long*/ arg1, int /*long*/ arg2, int /*long*/ arg3, int /*long*/ user_data) {
	Browser browser = FindBrowser (handle);
	if (browser == null) return 0;
	WebKit webkit = (WebKit)browser.webBrowser;
	return webkit.webViewProc (handle, arg0, arg1, arg2, arg3, user_data);
}

/** Webkit1 only */
int /*long*/ sessionProc (int /*long*/ session, int /*long*/ msg, int /*long*/ auth, int /*long*/ retrying, int /*long*/ user_data) {
	/* authentication challenges are currently the only notification received from the session */
	assert WEBKIT1 : WebKitGTK.Webkit1AssertMsg;
	if (retrying == 0) {
		failureCount = 0;
	} else {
		if (++failureCount >= 3) return 0;
	}

	int /*long*/ uri = WebKitGTK.soup_message_get_uri (msg);
	int /*long*/ uriString = WebKitGTK.soup_uri_to_string (uri, 0);
	int length = C.strlen (uriString);
	byte[] bytes = new byte[length];
	C.memmove (bytes, uriString, length);
	OS.g_free (uriString);
	String location = new String (Converter.mbcsToWcs (bytes));

	for (int i = 0; i < authenticationListeners.length; i++) {
		AuthenticationEvent event = new AuthenticationEvent (browser);
		event.location = location;
		authenticationListeners[i].authenticate (event);
		if (!event.doit) {
			OS.g_signal_stop_emission_by_name (session, WebKitGTK.authenticate);
			return 0;
		}
		if (event.user != null && event.password != null) {
			byte[] userBytes = Converter.wcsToMbcs (event.user, true);
			byte[] passwordBytes = Converter.wcsToMbcs (event.password, true);
			WebKitGTK.soup_auth_authenticate (auth, userBytes, passwordBytes);
			OS.g_signal_stop_emission_by_name (session, WebKitGTK.authenticate);
			return 0;
		}
	}
	return 0;
}

/**
 * Webkit2 only
 * - gboolean user_function (WebKitWebView *web_view, WebKitAuthenticationRequest *request, gpointer user_data)
 * - https://webkitgtk.org/reference/webkit2gtk/stable/WebKitWebView.html#WebKitWebView-authenticate
 */
int /*long*/ webkit_authenticate (int /*long*/ web_view, int /*long*/ request){

	/* authentication challenges are currently the only notification received from the session */
	if (!WebKitGTK.webkit_authentication_request_is_retry(request)) {
		failureCount = 0;
	} else {
		if (++failureCount >= 3) return 0;
	}

	String location = getUrl();

	for (int i = 0; i < authenticationListeners.length; i++) {
		AuthenticationEvent event = new AuthenticationEvent (browser);
		event.location = location;

		try { // to avoid deadlocks, evaluate() should not block during authentication listener. See Bug 512001
			  // I.e, evaluate() can be called and script will be executed, but no return value will be provided.
			nonBlockingEvaluate++;
			authenticationListeners[i].authenticate (event);
		} catch (Exception e) {
			throw e;
		} finally {
			nonBlockingEvaluate--;
		}

		if (!event.doit) {
			WebKitGTK.webkit_authentication_request_cancel (request);
			return 0;
		}
		if (event.user != null && event.password != null) {
			byte[] userBytes = Converter.wcsToMbcs (event.user, true);
			byte[] passwordBytes = Converter.wcsToMbcs (event.password, true);
			int /*long*/ credentials = WebKitGTK.webkit_credential_new (userBytes, passwordBytes, WebKitGTK.WEBKIT_CREDENTIAL_PERSISTENCE_NONE);
			WebKitGTK.webkit_authentication_request_authenticate(request, credentials);
			WebKitGTK.webkit_credential_free(credentials);
			return 0;
		}
	}
	return 0;
}

int /*long*/ webViewProc (int /*long*/ handle, int /*long*/ user_data) {
	switch ((int)/*64*/user_data) {
		case CLOSE_WEB_VIEW: return webkit_close_web_view (handle);
		case WEB_VIEW_READY: return webkit_web_view_ready (handle);
		default: return 0;
	}
}

int /*long*/ webViewProc (int /*long*/ handle, int /*long*/ arg0, int /*long*/ user_data) {
	switch ((int)/*64*/user_data) {
		case CREATE_WEB_VIEW: return webkit_create_web_view (handle, arg0);
		case DOWNLOAD_REQUESTED: return webkit_download_requested (handle, arg0); // webkit1
		case NOTIFY_LOAD_STATUS: return webkit_notify_load_status (handle, arg0); // Webkit1
		case LOAD_CHANGED: return webkit_load_changed (handle, (int) arg0, user_data);
		case NOTIFY_PROGRESS: return webkit_notify_progress (handle, arg0);		  // webkit1 & webkit2.
		case NOTIFY_TITLE: return webkit_notify_title (handle, arg0);
		case POPULATE_POPUP: return webkit_populate_popup (handle, arg0);
		case STATUS_BAR_TEXT_CHANGED: return webkit_status_bar_text_changed (handle, arg0); // Webkit1 only.
		case AUTHENTICATE: return webkit_authenticate (handle, arg0);		// Webkit2 only.
		default: return 0;
	}
}

int /*long*/ webViewProc (int /*long*/ handle, int /*long*/ arg0, int /*long*/ arg1, int /*long*/ user_data) {
	switch ((int)/*64*/user_data) {
		case HOVERING_OVER_LINK: return webkit_hovering_over_link (handle, arg0, arg1);		// Webkit1 only
		case MOUSE_TARGET_CHANGED: return webkit_mouse_target_changed (handle, arg0, arg1); // Webkit2 only.
		case DECIDE_POLICY: return webkit_decide_policy(handle, arg0, (int)arg1, user_data);
		default: return 0;
	}
}

int /*long*/ webViewProc (int /*long*/ handle, int /*long*/ arg0, int /*long*/ arg1, int /*long*/ arg2, int /*long*/ user_data) {
	switch ((int)/*64*/user_data) {
		case CONSOLE_MESSAGE: return webkit_console_message (handle, arg0, arg1, arg2);
		case WINDOW_OBJECT_CLEARED: return webkit_window_object_cleared (handle, arg0, arg1, arg2);
		case CONTEXT_MENU: return webkit_context_menu(handle, arg0, arg1, arg2);
		case LOAD_FAILED_TLS: return webkit_load_failed_tls(handle, arg0, arg1, arg2);
		default: return 0;
	}
}

int /*long*/ webViewProc (int /*long*/ handle, int /*long*/ arg0, int /*long*/ arg1, int /*long*/ arg2, int /*long*/ arg3, int /*long*/ user_data) {
	switch ((int)/*64*/user_data) {
		case MIME_TYPE_POLICY_DECISION_REQUESTED: return webkit_mime_type_policy_decision_requested (handle, arg0, arg1, arg2, arg3);  // Webkit1
		case NAVIGATION_POLICY_DECISION_REQUESTED: return webkit_navigation_policy_decision_requested (handle, arg0, arg1, arg2, arg3);
		case RESOURCE_REQUEST_STARTING: return webkit_resource_request_starting (handle, arg0, arg1, arg2, arg3); // Webkit1
		default: return 0;
	}
}

@Override
public void create (Composite parent, int style) {
	int [] vers = internalGetWebkitVersion();
	if (ExternalClass == 0) {
		System.setProperty(SWT_WEBKITGTK_VERSION,
				String.format("%s.%s.%s", vers[0], vers[1], vers[2])); // $NON-NLS-1$
		if (Device.DEBUG) {
			System.out.println(String.format("WebKit version %s.%s.%s", vers[0], vers[1], vers[2])); //$NON-NLS-1$
		}

		if (WEBKIT1) { // 'external' object only used on webkit1 for javaCall. Webkit2 has a different mechanism.
			JSClassDefinition jsClassDefinition = new JSClassDefinition ();
			byte[] bytes = Converter.wcsToMbcs (CLASSNAME_EXTERNAL, true);
			jsClassDefinition.className = C.malloc (bytes.length);
			C.memmove (jsClassDefinition.className, bytes, bytes.length);

			jsClassDefinition.hasProperty = JSObjectHasPropertyProc.getAddress ();
			jsClassDefinition.getProperty = JSObjectGetPropertyProc.getAddress ();
			int /*long*/ classDefinitionPtr = C.malloc (JSClassDefinition.sizeof);
			WebKitGTK.memmove (classDefinitionPtr, jsClassDefinition, JSClassDefinition.sizeof);

			ExternalClass = WebKitGTK.JSClassCreate (classDefinitionPtr);
		}

		byte [] bytes = Converter.wcsToMbcs ("POST", true); //$NON-NLS-1$
		PostString = C.malloc (bytes.length);
		C.memmove (PostString, bytes, bytes.length);

		/*
		* WebKitGTK version 1.8.x and newer can crash sporadically in
		* webkitWebViewRegisterForIconNotification().  The root issue appears
		* to be WebKitGTK accessing its icon database from a background
		* thread.  Work around this crash by disabling the use of WebKitGTK's
		* icon database, which should not affect the Browser in any way.
		*/
		if (WEBKIT1){
			int /*long*/ database = WebKitGTK.webkit_get_favicon_database ();
			if (database != 0) {
				/* WebKitGTK version is >= 1.8.x */
				WebKitGTK.webkit_favicon_database_set_path (database, 0);
			}
		}
	}

	if (WEBKIT1){
		scrolledWindow = GTK.gtk_scrolled_window_new (0, 0);
		GTK.gtk_scrolled_window_set_policy (scrolledWindow, GTK.GTK_POLICY_AUTOMATIC, GTK.GTK_POLICY_AUTOMATIC);
	}

	webView = WebKitGTK.webkit_web_view_new ();

	// Bug 522733 Webkit2 workaround for crash
	//   As of Webkitgtk 2.18, webkitgtk2 crashes if the first instance of webview is not referenced when JVM shuts down.
	//   There is a exit handler that tries to dereference the first instance [which if not referenced]
	//   leads to a crash. This workaround would benefit from deeper investigation (find root cause etc...).
	// [edit] Bug 530678. Note, it seems that as of Webkit2.18, webkit auto-disposes itself if parent get's disposed.
	//        While not directly related, see onDispose() for how to deal with disposal of this.
	if (WEBKIT2 && !bug522733FirstInstanceCreated && vers[0] == 2 && vers[1] >= 18) {
		bug522733FirstInstanceCreated = true;
		OS.g_object_ref(webView);
	}
	if (ignoreTls) {
		WebKitGTK.webkit_web_context_set_tls_errors_policy(WebKitGTK.webkit_web_view_get_context(webView),
				WebKitGTK.WEBKIT_TLS_ERRORS_POLICY_IGNORE);
		System.out.println("***WARNING: WebKitGTK is configured to ignore TLS errors via -Dorg.eclipse.swt.internal.webkitgtk.ignoretlserrors=true .");
		System.out.println("***WARNING: Please use for development purposes only!");
	}

	if (WEBKIT1) {
		webViewData = C.malloc (C.PTR_SIZEOF);
		C.memmove (webViewData, new int /*long*/[] {webView}, C.PTR_SIZEOF);
	}

	// Documentation for these signals/properties is usually found under signal/property of WebKitWebView.
	// notify_* usually implies a property change. For these, the first arg is typically the webview handle.
	if (WEBKIT1){
		// Webkit1 signal documentation: https://webkitgtk.org/reference/webkitgtk/unstable/webkitgtk-webkitwebview.html#WebKitWebView--progress
		GTK.gtk_container_add (scrolledWindow, webView);
		GTK.gtk_container_add (browser.handle, scrolledWindow);
		GTK.gtk_widget_show (scrolledWindow);
		OS.g_signal_connect (webView, WebKitGTK.close_web_view, 			Proc2.getAddress (), CLOSE_WEB_VIEW);
		OS.g_signal_connect (webView, WebKitGTK.web_view_ready, 			Proc2.getAddress (), WEB_VIEW_READY);

		OS.g_signal_connect (webView, WebKitGTK.hovering_over_link, 		Proc4.getAddress (), HOVERING_OVER_LINK);

		OS.g_signal_connect (webView, WebKitGTK.window_object_cleared, 		Proc5.getAddress (), WINDOW_OBJECT_CLEARED);
		OS.g_signal_connect (webView, WebKitGTK.console_message, 			Proc5.getAddress (), CONSOLE_MESSAGE);

		OS.g_signal_connect (webView, WebKitGTK.navigation_policy_decision_requested, 	Proc6.getAddress (), NAVIGATION_POLICY_DECISION_REQUESTED);
		OS.g_signal_connect (webView, WebKitGTK.mime_type_policy_decision_requested, 	Proc6.getAddress (), MIME_TYPE_POLICY_DECISION_REQUESTED);
		OS.g_signal_connect (webView, WebKitGTK.resource_request_starting, Proc6.getAddress (), RESOURCE_REQUEST_STARTING);

	} else {
		// Webkit2 Signal Documentation: https://webkitgtk.org/reference/webkit2gtk/stable/WebKitWebView.html#WebKitWebView--title
		GTK.gtk_container_add (browser.handle, webView);
		OS.g_signal_connect (webView, WebKitGTK.close, Proc2.getAddress (), CLOSE_WEB_VIEW);
		OS.g_signal_connect (webView, WebKitGTK.ready_to_show, Proc2.getAddress (), WEB_VIEW_READY);
		OS.g_signal_connect (webView, WebKitGTK.decide_policy, Proc4.getAddress (), DECIDE_POLICY);

		OS.g_signal_connect (webView, WebKitGTK.mouse_target_changed, Proc4.getAddress (), MOUSE_TARGET_CHANGED);
		OS.g_signal_connect (webView, WebKitGTK.context_menu, Proc5.getAddress (), CONTEXT_MENU);
		OS.g_signal_connect (webView, WebKitGTK.load_failed_with_tls_errors, Proc5.getAddress (), LOAD_FAILED_TLS);


	}

	// Proc3 is overloaded in that not only Webview connects to it,
	// but also (webkit1) WebFrame and (webkit2) WebKitDownload hook into it as well.
	// Pay extra attention to argument 1 (handle) to prevent wrong type of handle being passed to gtk and causing segfaults. (See 533545)
	if (WEBKIT1) {
		// WebKitWebView* user_function (WebKitWebView  *web_view, WebKitWebFrame *frame, gpointer user_data)
		OS.g_signal_connect (webView, WebKitGTK.create_web_view, 		 Proc3.getAddress (), CREATE_WEB_VIEW);

		// Property change: load-status  (webview is first arg)  https://webkitgtk.org/reference/webkitgtk/unstable/WebKitWebFrame.html#WebKitWebFrame--load-status
		OS.g_signal_connect (webView, WebKitGTK.notify_load_status, 	 Proc3.getAddress (), NOTIFY_LOAD_STATUS);

		// gboolean user_function (WebKitWebView  *web_view, WebKitDownload *download, gpointer        user_data)
		OS.g_signal_connect (webView, WebKitGTK.download_requested, 	 Proc3.getAddress (), DOWNLOAD_REQUESTED);

		// void user_function (WebKitWebView *web_view, GtkMenu *menu, gpointer user_data)
		OS.g_signal_connect (webView, WebKitGTK.populate_popup, 		 Proc3.getAddress (), POPULATE_POPUP);

		// Property change: progress.  (first arg is webview)
		OS.g_signal_connect (webView, WebKitGTK.notify_progress, 		 Proc3.getAddress (), NOTIFY_PROGRESS);

		// void user_function (WebKitWebView *webkitwebview, gchar  *arg1, gpointer user_data)
		OS.g_signal_connect (webView, WebKitGTK.status_bar_text_changed, Proc3.getAddress (), STATUS_BAR_TEXT_CHANGED);

	}
	if (WEBKIT2) { // Note: In Webkit2, webkit_download_started(...) also connects return signals to proc3.
		// GtkWidget* user_function (WebKitWebView *web_view, WebKitNavigationAction *navigation_action,  gpointer  user_data)
		OS.g_signal_connect (webView, WebKitGTK.create, 						Proc3.getAddress (), CREATE_WEB_VIEW);

		//void user_function (WebKitWebView  *web_view,  WebKitLoadEvent load_event,  gpointer  user_data)
		OS.g_signal_connect (webView, WebKitGTK.load_changed, 					Proc3.getAddress (), LOAD_CHANGED);

		// Property change: of 'estimated-load-progress'   args: webview, pspec
		OS.g_signal_connect (webView, WebKitGTK.notify_estimated_load_progress, Proc3.getAddress (), NOTIFY_PROGRESS);

		// gboolean user_function (WebKitWebView *web_view,  WebKitAuthenticationRequest *request,  gpointer user_data)
		OS.g_signal_connect (webView, WebKitGTK.authenticate, 					Proc3.getAddress (), AUTHENTICATE);

		// (!) Note this one's a 'webContext' signal, not webview. See:
		// https://webkitgtk.org/reference/webkit2gtk/stable/WebKitWebContext.html#WebKitWebContext-download-started
		OS.g_signal_connect (WebKitGTK.webkit_web_context_get_default(), WebKitGTK.download_started, Proc3.getAddress (), DOWNLOAD_STARTED);
	}

	GTK.gtk_widget_show (webView);
	GTK.gtk_widget_show (browser.handle);

	// Webview 'title' property. Webkit1 & Webkit2.
	OS.g_signal_connect (webView, WebKitGTK.notify_title, 						Proc3.getAddress (), NOTIFY_TITLE);

	/* Callback to get events before WebKit receives and consumes them */
	if (WEBKIT2) {
		OS.g_signal_connect (webView, OS.button_press_event, JSDOMEventProc.getAddress (), WIDGET_EVENT);
		OS.g_signal_connect (webView, OS.button_release_event, JSDOMEventProc.getAddress (), WIDGET_EVENT);
		OS.g_signal_connect (webView, OS.focus_in_event, JSDOMEventProc.getAddress (), 	WIDGET_EVENT);
		OS.g_signal_connect (webView, OS.focus_out_event, JSDOMEventProc.getAddress (), WIDGET_EVENT);
		// if connecting any other special gtk event to webkit, add SWT.* to w2_passThroughSwtEvents above.
	}
	this.pageId = WebKitGTK.webkit_web_view_get_page_id (webView);
	if (WEBKIT1) {
		OS.g_signal_connect (webView, OS.button_press_event, JSDOMEventProc.getAddress (), 0);
		OS.g_signal_connect (webView, OS.button_release_event, JSDOMEventProc.getAddress (), 0);
	}
	OS.g_signal_connect (webView, OS.key_press_event, JSDOMEventProc.getAddress (),  	WIDGET_EVENT);
	OS.g_signal_connect (webView, OS.key_release_event, JSDOMEventProc.getAddress (),	WIDGET_EVENT);
	OS.g_signal_connect (webView, OS.scroll_event, JSDOMEventProc.getAddress (), 		WIDGET_EVENT);
	OS.g_signal_connect (webView, OS.motion_notify_event, JSDOMEventProc.getAddress (), WIDGET_EVENT);

	/*
	* Callbacks to get the events not consumed by WebKit, and to block
	* them so that they don't get propagated to the parent handle twice.
	* This hook is set after WebKit and is therefore called after WebKit's
	* handler because GTK dispatches events in their order of registration.
	*/
	if (WEBKIT1){
		OS.g_signal_connect (scrolledWindow, OS.button_press_event, JSDOMEventProc.getAddress (), STOP_PROPOGATE);
		OS.g_signal_connect (scrolledWindow, OS.button_release_event, JSDOMEventProc.getAddress (), STOP_PROPOGATE);
		OS.g_signal_connect (scrolledWindow, OS.key_press_event, JSDOMEventProc.getAddress (), STOP_PROPOGATE);
		OS.g_signal_connect (scrolledWindow, OS.key_release_event, JSDOMEventProc.getAddress (), STOP_PROPOGATE);
		OS.g_signal_connect (scrolledWindow, OS.scroll_event, JSDOMEventProc.getAddress (), STOP_PROPOGATE);
		OS.g_signal_connect (scrolledWindow, OS.motion_notify_event, JSDOMEventProc.getAddress (), STOP_PROPOGATE);
	}

	byte[] bytes = Converter.wcsToMbcs ("UTF-8", true); // $NON-NLS-1$

	int /*long*/ settings = WebKitGTK.webkit_web_view_get_settings (webView);
	OS.g_object_set (settings, WebKitGTK.javascript_can_open_windows_automatically, 1, 0);
	OS.g_object_set (settings, WebKitGTK.enable_webgl, 1, 0);

	if (WEBKIT2){
		OS.g_object_set (settings, WebKitGTK.default_charset, bytes, 0);
		if (WebKitGTK.webkit_get_minor_version() >= 14) {
			OS.g_object_set (settings, WebKitGTK.allow_universal_access_from_file_urls, 1, 0);
		} else {
			System.err.println("SWT WEBKIT: Warning, you are using Webkitgtk below version 2.14. Your version is: "
					+ "Your version is: " + internalGetWebKitVersionStr()
					+ "\nJavascript execution limited to same origin due to unimplemented feature of this version.");
		}
	} else {
		OS.g_object_set (settings, WebKitGTK.default_encoding, bytes, 0);
		OS.g_object_set (settings, WebKitGTK.enable_universal_access_from_file_uris, 1, 0);
	}

	Listener listener = event -> {
		switch (event.type) {
			case SWT.Dispose: {
				/* make this handler run after other dispose listeners */
				if (ignoreDispose) {
					ignoreDispose = false;
					break;
				}
				ignoreDispose = true;
				browser.notifyListeners (event.type, event);
				event.type = SWT.NONE;
				onDispose (event);
				break;
			}
			case SWT.FocusIn: {
				if (WEBKIT2 && webView != 0)
					GTK.gtk_widget_grab_focus (webView);
				break;
			}
			case SWT.Resize: {
				onResize (event);
				break;
			}
		}
	};
	browser.addListener (SWT.Dispose, listener);
	browser.addListener (SWT.FocusIn, listener);
	browser.addListener (SWT.KeyDown, listener);
	browser.addListener (SWT.Resize, listener);

	if (WEBKIT1){
		/*
		* Ensure that our Authenticate listener is at the front of the signal
		* queue by removing the default Authenticate listener, adding ours,
		* and then re-adding the default listener.
		*/
		int /*long*/ session = WebKitGTK.webkit_get_default_session ();
		int /*long*/ originalAuth = WebKitGTK.soup_session_get_feature (session, WebKitGTK.webkit_soup_auth_dialog_get_type ());
		if (originalAuth != 0) {
			WebKitGTK.soup_session_feature_detach (originalAuth, session);
		}
		OS.g_signal_connect (session, WebKitGTK.authenticate, Proc5.getAddress (), webView);
		if (originalAuth != 0) {
			WebKitGTK.soup_session_feature_attach (originalAuth, session);
		}

		/*
		* Check for proxy values set as documented java properties and update the
		* session to use these values if needed.
		*/
		String proxyHost = System.getProperty (PROPERTY_PROXYHOST);
		String proxyPortString = System.getProperty (PROPERTY_PROXYPORT);
		int port = -1;
		if (proxyPortString != null) {
			try {
				int value = Integer.valueOf (proxyPortString).intValue ();
				if (0 <= value && value <= MAX_PORT) port = value;
			} catch (NumberFormatException e) {
				/* do nothing, java property has non-integer value */
			}
		}
		if (proxyHost != null || port != -1) {
			if (!proxyHost.startsWith (PROTOCOL_HTTP)) {
				proxyHost = PROTOCOL_HTTP + proxyHost;
			}
			proxyHost += ":" + port; //$NON-NLS-1$
			bytes = Converter.wcsToMbcs (proxyHost, true);
			int /*long*/ uri = WebKitGTK.soup_uri_new (bytes);
			if (uri != 0) {
				OS.g_object_set (session, WebKitGTK.SOUP_SESSION_PROXY_URI, uri, 0);
				WebKitGTK.soup_uri_free (uri);
			}
		}
	}

	if (WEBKIT1) { // HandleWebKitEvent registration. Pre Webkit 1.4 way of handling mouse/keyboard events. Webkit2 uses dom.
		eventFunction = new BrowserFunction (browser, "HandleWebKitEvent") { //$NON-NLS-1$
			@Override
			public Object function(Object[] arguments) {
				return handleEventFromFunction (arguments) ? Boolean.TRUE : Boolean.FALSE;
			}
		};
	}

	/*
	* Bug in WebKitGTK.  MouseOver/MouseLeave events are not consistently sent from
	* the DOM when the mouse enters and exits the browser control, see
	* https://bugs.webkit.org/show_bug.cgi?id=35246.  As a workaround for sending
	* MouseEnter/MouseExit events, swt's default mouse enter/exit mechanism is used,
	* but in order to do this the Browser's default sub-window check behavior must
	* be changed.
	*/
	browser.setData (KEY_CHECK_SUBWINDOW, Boolean.FALSE);

	/*
	 * Bug in WebKitGTK.  In WebKitGTK 1.10.x a crash can occur if an
	 * attempt is made to show a browser before a size has been set on
	 * it.  The workaround is to temporarily give it a size that forces
	 * the native resize events to fire.
	 */
	int major = vers[0], minor = vers[1];
	if (major == 1 && minor >= 10) {
		Rectangle minSize = browser.computeTrim (0, 0, 2, 2);
		Point size = browser.getSize ();
		size.x += minSize.width; size.y += minSize.height;
		browser.setSize (size);
		size.x -= minSize.width; size.y -= minSize.height;
		browser.setSize (size);
	}
}

void addEventHandlers (int /*long*/ web_view, boolean top) {
	/*
	* If JS is disabled (causes DOM events to not be delivered) then do not add event
	* listeners here, DOM events will be inferred from received GDK events instead.
	*/
	if (!jsEnabled) return;

	if (top) {
		if (WEBKIT2) {
			// TODO implement equivalent?
			// As a note, this entire function only seems to do webkit1-only stuff at the moment...
		} else {
			int /*long*/ domDocument = WebKitGTK.webkit_web_view_get_dom_document (web_view); // Webkit1 only
			if (domDocument != 0) {
				WindowMappings.put (new LONG (domDocument), new LONG (web_view));
				WebKitGTK.webkit_dom_event_target_add_event_listener (domDocument, WebKitGTK.dragstart, JSDOMEventProc.getAddress (), 0, SWT.DragDetect);
				WebKitGTK.webkit_dom_event_target_add_event_listener (domDocument, WebKitGTK.keydown, JSDOMEventProc.getAddress (), 0, SWT.KeyDown);
				WebKitGTK.webkit_dom_event_target_add_event_listener (domDocument, WebKitGTK.keypress, JSDOMEventProc.getAddress (), 0, SENTINEL_KEYPRESS);
				WebKitGTK.webkit_dom_event_target_add_event_listener (domDocument, WebKitGTK.keyup, JSDOMEventProc.getAddress (), 0, SWT.KeyUp);
				WebKitGTK.webkit_dom_event_target_add_event_listener (domDocument, WebKitGTK.mousedown, JSDOMEventProc.getAddress (), 0, SWT.MouseDown);
				WebKitGTK.webkit_dom_event_target_add_event_listener (domDocument, WebKitGTK.mousemove, JSDOMEventProc.getAddress (), 0, SWT.MouseMove);
				WebKitGTK.webkit_dom_event_target_add_event_listener (domDocument, WebKitGTK.mouseup, JSDOMEventProc.getAddress (), 0, SWT.MouseUp);
				WebKitGTK.webkit_dom_event_target_add_event_listener (domDocument, WebKitGTK.mousewheel, JSDOMEventProc.getAddress (), 0, SWT.MouseWheel);

				/*
				* The following two lines are intentionally commented because they cannot be used to
				* consistently send MouseEnter/MouseExit events until https://bugs.webkit.org/show_bug.cgi?id=35246
				* is fixed.
				*/
				//WebKitGTK.webkit_dom_event_target_add_event_listener (domWindow, WebKitGTK.mouseover, JSDOMEventProc.getAddress (), 0, SWT.MouseEnter);
				//WebKitGTK.webkit_dom_event_target_add_event_listener (domWindow, WebKitGTK.mouseout, JSDOMEventProc.getAddress (), 0, SWT.MouseExit);
			}
			return;
		}
	}


	if (WEBKIT1) { // add HandleWebKitEvent key/mouse handlers
		/* install the JS call-out to the registered BrowserFunction */
		StringBuilder buffer = new StringBuilder ("window.SWTkeyhandler = function SWTkeyhandler(e) {"); //$NON-NLS-1$
		buffer.append ("try {e.returnValue = HandleWebKitEvent(e.type, e.keyCode, e.charCode, e.altKey, e.ctrlKey, e.shiftKey, e.metaKey);} catch (e) {}};"); //$NON-NLS-1$
		nonBlockingExecute (buffer.toString ());
		buffer = new StringBuilder ("window.SWTmousehandler = function SWTmousehandler(e) {"); //$NON-NLS-1$
		buffer.append ("try {e.returnValue = HandleWebKitEvent(e.type, e.screenX, e.screenY, e.detail, e.button, e.altKey, e.ctrlKey, e.shiftKey, e.metaKey, e.relatedTarget != null);} catch (e) {}};"); //$NON-NLS-1$
		nonBlockingExecute (buffer.toString ());

		if (top) {
			/* DOM API is not available, so add listener to top-level document */
			buffer = new StringBuilder ("document.addEventListener('keydown', SWTkeyhandler, true);"); //$NON-NLS-1$
			buffer.append ("document.addEventListener('keypress', SWTkeyhandler, true);"); //$NON-NLS-1$
			buffer.append ("document.addEventListener('keyup', SWTkeyhandler, true);"); //$NON-NLS-1$
			buffer.append ("document.addEventListener('mousedown', SWTmousehandler, true);"); //$NON-NLS-1$
			buffer.append ("document.addEventListener('mouseup', SWTmousehandler, true);"); //$NON-NLS-1$
			buffer.append ("document.addEventListener('mousemove', SWTmousehandler, true);"); //$NON-NLS-1$
			buffer.append ("document.addEventListener('mousewheel', SWTmousehandler, true);"); //$NON-NLS-1$
			buffer.append ("document.addEventListener('dragstart', SWTmousehandler, true);"); //$NON-NLS-1$

			/*
			* The following two lines are intentionally commented because they cannot be used to
			* consistently send MouseEnter/MouseExit events until https://bugs.webkit.org/show_bug.cgi?id=35246
			* is fixed.
			*/
			//buffer.append ("document.addEventListener('mouseover', SWTmousehandler, true);"); //$NON-NLS-1$
			//buffer.append ("document.addEventListener('mouseout', SWTmousehandler, true);"); //$NON-NLS-1$

			nonBlockingExecute (buffer.toString ());
			return;
		}

		/* add JS event listener in frames */
		buffer = new StringBuilder ("for (var i = 0; i < frames.length; i++) {"); //$NON-NLS-1$
		buffer.append ("frames[i].document.addEventListener('keydown', window.SWTkeyhandler, true);"); //$NON-NLS-1$
		buffer.append ("frames[i].document.addEventListener('keypress', window.SWTkeyhandler, true);"); //$NON-NLS-1$
		buffer.append ("frames[i].document.addEventListener('keyup', window.SWTkeyhandler, true);"); //$NON-NLS-1$
		buffer.append ("frames[i].document.addEventListener('mousedown', window.SWTmousehandler, true);"); //$NON-NLS-1$
		buffer.append ("frames[i].document.addEventListener('mouseup', window.SWTmousehandler, true);"); //$NON-NLS-1$
		buffer.append ("frames[i].document.addEventListener('mousemove', window.SWTmousehandler, true);"); //$NON-NLS-1$
		buffer.append ("frames[i].document.addEventListener('mouseover', window.SWTmousehandler, true);"); //$NON-NLS-1$
		buffer.append ("frames[i].document.addEventListener('mouseout', window.SWTmousehandler, true);"); //$NON-NLS-1$
		buffer.append ("frames[i].document.addEventListener('mousewheel', window.SWTmousehandler, true);"); //$NON-NLS-1$
		buffer.append ("frames[i].document.addEventListener('dragstart', window.SWTmousehandler, true);"); //$NON-NLS-1$
		buffer.append ('}');
		nonBlockingExecute (buffer.toString ());
	}
}

@Override
public boolean back () {
	if (WebKitGTK.webkit_web_view_can_go_back (webView) == 0) return false;
	WebKitGTK.webkit_web_view_go_back (webView);
	return true;
}

@Override
public boolean close () {
	return close (true);
}

// Developer note:
// @return true = leads to disposal. In Browser.java, user is told widget is disposed. Ex in Snippe326 close button is grayed out.
//         false = blocks disposal. In Browser.java, user is told widget was not disposed.
// See Snippet326.
boolean close (boolean showPrompters) {
	assert WEBKIT1 || WEBKIT2;
	if (!jsEnabled) return true;

	String message1 = Compatibility.getMessage("SWT_OnBeforeUnload_Message1"); // $NON-NLS-1$
	String message2 = Compatibility.getMessage("SWT_OnBeforeUnload_Message2"); // $NON-NLS-1$
	String functionName = EXECUTE_ID + "CLOSE"; // $NON-NLS-1$
	StringBuilder buffer = new StringBuilder ("function "); // $NON-NLS-1$
	buffer.append (functionName);
	buffer.append ("(win) {\n"); // $NON-NLS-1$
	buffer.append ("var fn = win.onbeforeunload; if (fn != null) {try {var str = fn(); "); // $NON-NLS-1$
	if (showPrompters) {
		buffer.append ("if (str != null) { "); // $NON-NLS-1$
		buffer.append ("var result = confirm('"); // $NON-NLS-1$
		buffer.append (message1);
		buffer.append ("\\n\\n'+str+'\\n\\n"); // $NON-NLS-1$
		buffer.append (message2);
		buffer.append ("');"); // $NON-NLS-1$
		buffer.append ("if (!result) return false;}"); // $NON-NLS-1$
	}
	buffer.append ("} catch (e) {}}"); // $NON-NLS-1$
	buffer.append ("try {for (var i = 0; i < win.frames.length; i++) {var result = "); // $NON-NLS-1$
	buffer.append (functionName);
	buffer.append ("(win.frames[i]); if (!result) return false;}} catch (e) {} return true;"); // $NON-NLS-1$
	buffer.append ("\n};"); // $NON-NLS-1$
	nonBlockingExecute (buffer.toString ());

	Boolean result;
	if (WEBKIT1) {
		result = (Boolean)evaluate ("return " + functionName +"(window);"); // $NON-NLS-1$ // $NON-NLS-2$
		if (result == null) return false; // Default to prevent disposal.
	} else {
		assert WEBKIT2 : WebKitGTK.Webkit2AssertMsg;
		// Sometimes if a disposal is already underway (ex parent shell disposed), then
		// Webkit1: Silently fails
		// Webkit2: Javascript execution can throw. We have to account for that.
		try {
			result = (Boolean)evaluate ("return " + functionName +"(window);"); // $NON-NLS-1$ // $NON-NLS-2$
			if (result == null) return true; // Default to assume that webkit is disposed and allow disposal of Browser.
		} catch (SWTException e) {
			return true; // Permit browser to be disposed if javascript execution failed.
		}
	}
	return result.booleanValue ();
}


private boolean isJavascriptEnabled() {
	assert WEBKIT2 : WebKitGTK.Webkit2AssertMsg;
	// If you try to run javascript while javascript is turned off, then:
	// - on Webkit1: nothing happens.
	// - on Webkit2: an exception is thrown.
	// To ensure consistent behavior, do not even try to execute js on webkit2 if it's off.
	return webkit_settings_get(WebKitGTK.enable_javascript) != 0;
}

@Override
void nonBlockingExecute(String script) {
	try {
		nonBlockingEvaluate++;
		execute(script);
	} finally {
		nonBlockingEvaluate--;
	}
}

/**
 * Modifies a BrowserFunction in the web extension. This method can be used to register/deregister BrowserFunctions
 * in the web extension, so that those BrowserFunctions are executed upon triggering of the object_cleared callback (in
 * the extension, not in Java).
 *
 * This function will return true if: the operation succeeds synchronously, or if the synchronous call timed out and an
 * asynchronous call was performed instead. All other cases will return false.
 *
 * Supported actions: "register" and "deregister"
 *
 * @param pageId the page ID of the WebKit instance/web page
 * @param function the function string
 * @param url the URL
 * @param action the action being performed on the function, which will be used to form the DBus method name.
 * @return true if the action succeeded (or was performed asynchronously), false if it failed
 */
private boolean webkit_extension_modify_function (long pageId, String function, String url, String action){
	int /*long*/ args[] = { OS.g_variant_new_uint64(pageId),
			OS.g_variant_new_string (Converter.javaStringToCString(function)),
			OS.g_variant_new_string (Converter.javaStringToCString(url))};
	final int /*long*/ argsTuple = OS.g_variant_new_tuple(args, args.length);
	if (argsTuple == 0) return false;
	String dbusMethodName = "webkitgtk_extension_" + action + "_function";
	Object returnVal = WebkitGDBus.callExtensionSync(argsTuple, dbusMethodName);
	if (returnVal instanceof Boolean) {
		return (Boolean) returnVal;
	} else if (returnVal instanceof String) {
		String returnString = (String) returnVal;
		/*
		 * Call the extension asynchronously if a synchronous call times out.
		 * Note: this is a pretty rare case, and usually only happens when running test cases.
		 * See bug 536141.
		 */
		if ("timeout".equals(returnString)) {
			return WebkitGDBus.callExtensionAsync(argsTuple, dbusMethodName);
		}
	}
	return false;
}

@Override
public boolean execute (String script) {
	if (WEBKIT2){
        if (!isJavascriptEnabled()) {
        	System.err.println("SWT Webkit Warning: Attempting to execute javascript when javascript is dissabled."
        			+ "Execution has no effect. Script:\n" + script);
        	return false;
        }
		try {
			Webkit2AsyncToSync.runjavascript(script, this.browser, webView);
		} catch (SWTException e) {
			return false;
		}
		return true;
	} else {
		byte[] scriptBytes = (script + '\0').getBytes (StandardCharsets.UTF_8); //$NON-NLS-1$
		int /*long*/ jsScriptString = WebKitGTK.JSStringCreateWithUTF8CString (scriptBytes);

		// Currently loaded website will be used as 'source file' of the javascript to be exucuted.
		byte[] sourceUrlbytes = (getUrl () + '\0').getBytes (StandardCharsets.UTF_8); //$NON-NLS-1$

		int /*long*/ jsSourceUrlString = WebKitGTK.JSStringCreateWithUTF8CString (sourceUrlbytes);
		int /*long*/ frame = WebKitGTK.webkit_web_view_get_main_frame (webView);
		int /*long*/ context = WebKitGTK.webkit_web_frame_get_global_context (frame);
		int /*long*/ result = WebKitGTK.JSEvaluateScript (context, jsScriptString, 0, jsSourceUrlString, 0, null);
		WebKitGTK.JSStringRelease (jsSourceUrlString);
		WebKitGTK.JSStringRelease (jsScriptString);
		return result != 0;
	}
}

/**
 * Webkit2 introduces async api. However SWT has sync execution model. This class it to convert async api to sync.
 *
 * Be careful about using these methods in synchronous callbacks from webkit, as those can cause deadlocks. (See inner javadocs).
 *
 * The mechanism generates an ID for each callback and waits for that callback to complete.
 */
private static class Webkit2AsyncToSync {

	private static Callback runjavascript_callback;
	private static Callback getText_callback;
	static {
		runjavascript_callback = new Callback(Webkit2AsyncToSync.class, "runjavascript_callback", void.class, new Type[] {long.class, long.class, long.class});
		if (runjavascript_callback.getAddress() == 0) SWT.error (SWT.ERROR_NO_MORE_CALLBACKS);

		getText_callback = new Callback(Webkit2AsyncToSync.class, "getText_callback", void.class, new Type[] {long.class, long.class, long.class});
		if (getText_callback.getAddress() == 0) SWT.error(SWT.ERROR_NO_MORE_CALLBACKS);
	}

	/** Object used to return data from callback to original call */
	private static class Webkit2AsyncReturnObj {
		boolean callbackFinished = false;
		Object returnValue = null; // As note, if browser is disposed during excution, null is returned.

		/** 0=no error. >0 means error. **/
		int errorNum = 0;
		String errorMsg;

		/** Set to true if call timed out. Not set by javascript execution itself */
		boolean swtAsyncTimeout;
	}

	/**
	 * Every callback is tagged with a unique ID.
	 * The ID is used for the callback to find the object via which data is returned
	 * and allow the original call to finish.
	 *
	 * Note: The reason each callback is tagged with an ID is because two(or more) subsequent
	 * evaluate() calls can be started before the first callback comes back.
	 * As such, there would be ambiguity as to which call a callback belongs to, which in turn causes deadlocks.
	 * This is typically seen when a webkit2 signal (e.g closeListener) makes a call to evaluate(),
	 * when the closeListener was triggered by evaluate("window.close()").
	 * An example test case where this is seen is:
	 * org.eclipse.swt.tests.junit.Test_org_eclipse_swt_browser_Browser.test_execute_and_closeListener()
	 */
	private static class CallBackMap {
		private static HashMap<Integer, Webkit2AsyncReturnObj> callbackMap = new HashMap<>();

		static int putObject(Webkit2AsyncReturnObj obj) {
			int id = getNextId();
			callbackMap.put(id, obj);
			return id;
		}
		static Webkit2AsyncReturnObj getObj(int id) {
			return callbackMap.get(id);
		}
		static void removeObject(int id) {
			callbackMap.remove(id);
			removeId(id);
		}

		// Mechanism to generate unique ID's
		private static int nextCallbackId = 1;
		private static HashSet<Integer> usedCallbackIds = new HashSet<>();
		static int getNextId() {
			int value = 0;
			boolean unique = false;
			while (unique == false) {
				value = nextCallbackId;
				unique = !usedCallbackIds.contains(value);
				if (nextCallbackId != Integer.MAX_VALUE)
					nextCallbackId++;
				else
					nextCallbackId = 1;
			}
			usedCallbackIds.add(value);
			return value;
		}
		private static void removeId(int id) {
			usedCallbackIds.remove(id);
		}
	}

	static Object evaluate (String script, Browser browser, int /*long*/ webView)  {
//		/* Wrap script around a temporary function for backwards compatibility,
//		 * user can specify 'return', which may not be at the beginning of the script.
//		 *  Valid scripts:
//		 *      'hi'
//		 *  	return 'hi'
//		 *  	var x = 1; return 'hi'
//		 */
		String swtUniqueExecFunc = "SWTWebkit2TempFunc" + CallBackMap.getNextId() + "()";
		String wrappedScript = "function " + swtUniqueExecFunc +"{" + script + "}; " + swtUniqueExecFunc;
		return runjavascript(wrappedScript, browser, webView);

	}

	/**
	 * Run javascript, wait for a return value.
	 *
	 * Developer note:
	 * Be EXTRA careful with this method, it can cause deadlocks in situations where
	 * javascript is executed in a callback that provides a return value to webkit.
	 * In otherwords, if webkit does a sync callback (one that requires a return value),
	 * then running javascript will lead to a deadlock because webkit will not execute
	 * the javascript until it's sync callback finished.
	 * As a note, SWT's callback mechanism hard-codes 'long' return even when a callback
	 * is actually 'void'. So reference webkit callback signature documentation and not
	 * SWT implementation.
	 *
	 * If in doubt, you should use nonBlockingExecute() where possible :-).
	 *
	 * TODO_SOMEDAY:
	 * - Instead of async js execution and waiting for return value, it might be
	 *   better to use gdbus, connect to webextension and execute JS synchronously.
	 *   See: https://blogs.igalia.com/carlosgc/2013/09/10/webkit2gtk-web-process-extensions/
	 *    'Extending JavaScript'
	 *   Pros:
	 *    - less likely deadlocks would occur due to developer error/not being careful.
	 *    - js execution can work in synchronous callbacks from webkit.
	 *   Cons:
	 *    - High implementation cost/complexity.
	 *    - Unexpected errors/behaviour due to GDBus timeouts.
	 *   Proof of concept:
	 *   https://git.eclipse.org/r/#/c/23416/16/bundles/org.eclipse.swt/Eclipse+SWT+WebKit/gtk/library/webkit_extension.c
	 *     > 'webkit_extension_execute_script'
	 *   Tennative structure:
	 *   - Webextension should create gdbus server, make & communicate UniqueID (pid) to main proc
	 *   - main proc should make a note of webextension's name+uniqueID
	 *   - implement mechanism for packaging Java objects into gvariants, (see WebkitGDBus.java),
	 *   - call webextension over gdbus, parse return value.
	 *
	 */
	static Object runjavascript(String script, Browser browser, int /*long*/ webView) {
		if (nonBlockingEvaluate > 0) {
			// Execute script, but do not wait for async call to complete. (assume it does). Bug 512001.
			WebKitGTK.webkit_web_view_run_javascript(webView, Converter.wcsToMbcs(script, true), 0, 0, 0);
			return null;
		} else {
			// Callback logic: Initiate an async callback and wait for it to finish.
			// The callback comes back in runjavascript_callback(..) below.
			Consumer <Integer> asyncFunc = (callbackId) -> {
				WebKitGTK.webkit_web_view_run_javascript(webView, Converter.wcsToMbcs(script, true), 0, runjavascript_callback.getAddress(), callbackId);
			};

			Webkit2AsyncReturnObj retObj = execAsyncAndWaitForReturn(browser, asyncFunc, " The following javascript was executed:\n" + script +"\n\n");

			if (retObj.swtAsyncTimeout) {
				return null;
			} else if (retObj.errorNum != 0) {
				throw new SWTException(retObj.errorNum, retObj.errorMsg +"\nScript that was evaluated:\n" + script);
			} else {
				// This is also the implicit case where browser was disposed while javascript was executing. It returns null.
				return retObj.returnValue;
			}
		}
	}

	@SuppressWarnings("unused") // Only called directly from C (from javascript).
	private static void runjavascript_callback (int /*long*/ GObject_source, int /*long*/ GAsyncResult, int /*long*/ user_data) {
		int callbackId = (int) user_data;
		Webkit2AsyncReturnObj retObj = CallBackMap.getObj(callbackId);

		if (retObj != null) { // retObj can be null if there was a timeout.
			int /*long*/[] gerror = new int /*long*/ [1]; // GError **
			int /*long*/ js_result = WebKitGTK.webkit_web_view_run_javascript_finish(GObject_source, GAsyncResult, gerror);
			if (js_result == 0) {
				int /*long*/ errMsg = OS.g_error_get_message(gerror[0]);
				String msg = Converter.cCharPtrToJavaString(errMsg, false);
				OS.g_error_free(gerror[0]);

				retObj.errorNum = SWT.ERROR_FAILED_EVALUATE;
				retObj.errorMsg = msg != null ? msg : "";
			} else {
				int /*long*/ context = WebKitGTK.webkit_javascript_result_get_global_context (js_result);
				int /*long*/ value = WebKitGTK.webkit_javascript_result_get_value (js_result);

				try {
					retObj.returnValue = convertToJava(context, value);
				} catch (IllegalArgumentException ex) {
					retObj.errorNum = SWT.ERROR_INVALID_RETURN_VALUE;
					retObj.errorMsg = "Type of return value not is not valid. For supported types see: Browser.evaluate() JavaDoc";
				}
				WebKitGTK.webkit_javascript_result_unref (js_result);
			}
			retObj.callbackFinished = true;
		}
		Display.getCurrent().wake();
	}

	static String getText(Browser browser, int /*long*/ webView) {
		int /*long*/ WebKitWebResource = WebKitGTK.webkit_web_view_get_main_resource(webView);
		if (WebKitWebResource == 0) { // No page yet loaded.
			return "";
		}
		if (nonBlockingEvaluate > 0) {
			System.err.println("SWT Webkit Warning: getText() called inside a synchronous callback, which can lead to a deadlock.\n"
					+ "Avoid using getText in OpenWindowListener, Authentication listener and when webkit is about to change to a new page\n"
					+ "Return value is empty string '' instead of actual text");
			return "";
		}

		Consumer<Integer> asyncFunc = (callbackId) -> WebKitGTK.webkit_web_resource_get_data(WebKitWebResource, 0, getText_callback.getAddress(), callbackId);
		Webkit2AsyncReturnObj retObj = execAsyncAndWaitForReturn(browser, asyncFunc, " getText() was called");

		if (retObj.swtAsyncTimeout)
			return "SWT WEBKIT TIMEOUT ERROR";
		else
			return (String) retObj.returnValue;
	}

	@SuppressWarnings("unused") // Callback only called only by C directly
	private static void getText_callback(int /*long*/ WebResource, int /*long*/ GAsyncResult, int /*long*/ user_data) {
		int callbackId = (int) user_data;
		Webkit2AsyncReturnObj retObj = CallBackMap.getObj(callbackId);

		int /*long*/[] gsize_len = new int /*long*/ [1];
		int /*long*/[] gerrorRes = new int /*long*/ [1]; // GError **
		int /*long*/ guchar_data = WebKitGTK.webkit_web_resource_get_data_finish(WebResource, GAsyncResult, gsize_len, gerrorRes);
		if (gerrorRes[0] != 0 || guchar_data == 0) {
			OS.g_error_free(gerrorRes[0]);
			retObj.returnValue = (String) "";
		} else {
			int len = (int) gsize_len[0];
			byte[] buffer = new byte [len];
			C.memmove (buffer, guchar_data, len);
			String text = Converter.byteToStringViaHeuristic(buffer);
			retObj.returnValue = text;
		}

		retObj.callbackFinished = true;
		Display.getCurrent().wake();
	}
	/**
	 * You should check 'retObj.swtAsyncTimeout' after making a call to this.
	 */
	private static Webkit2AsyncReturnObj execAsyncAndWaitForReturn(Browser browser, Consumer<Integer> asyncFunc, String additionalErrorInfo) {
		Webkit2AsyncReturnObj retObj = new Webkit2AsyncReturnObj();
		int callbackId = CallBackMap.putObject(retObj);
		asyncFunc.accept(callbackId);
		Display display = browser.getDisplay();
		final Instant timeOut = Instant.now().plusMillis(ASYNC_EXEC_TIMEOUT_MS);
		while (!browser.isDisposed()) {
			boolean eventsDispatched = OS.g_main_context_iteration (0, false);
			if (retObj.callbackFinished)
				break;
			else if (Instant.now().isAfter(timeOut)) {
				System.err.println("SWT call to Webkit timed out after " + ASYNC_EXEC_TIMEOUT_MS
						+ "ms. No return value will be provided.\n"
						+ "Possible reasons:\n"
						+ "1) Problem: Your javascript needs more than " + ASYNC_EXEC_TIMEOUT_MS +"ms to execute.\n"
						+ "   Solution: Don't run such javascript, it blocks Eclipse's UI. SWT currently allows such code to complete, but this error is thrown \n"
						+ "     and the return value of execute()/evalute() will be false/null.\n\n"
						+ "2) However, if you believe that your application should execute as expected (in under" + ASYNC_EXEC_TIMEOUT_MS + " ms),\n"
						+ " then it might be a deadlock in SWT/Browser/webkit2 logic.\n"
						+ " I.e, it might be a bug in SWT (e.g this does not occur on Windows/Cocoa, but occurs on Linux). If you believe it to be a bug in SWT, then\n"
						+ getInternalErrorMsg()
						+ "\n Additional information about the error is as following:\n"
						+ additionalErrorInfo);
				retObj.swtAsyncTimeout = true;
				break;
			}
			else if (!eventsDispatched)
				display.sleep();
		}
		CallBackMap.removeObject(callbackId);
		return retObj;
	}
}

@Override
public Object evaluate (String script) throws SWTException {
	if ("".equals(script)) {
		return null; // A litte optimization. Sometimes evaluate() is called with a generated script, where the generated script is sometimes empty.
	}
	if (WEBKIT2){
        if (!isJavascriptEnabled()) {
        	return null;
        }
		return Webkit2AsyncToSync.evaluate(script, this.browser, webView);
	} else {
		return super.evaluate(script);
	}
}

@Override
public boolean forward () {
	if (WEBKIT2 && webView == 0) {
		assert false;
		System.err.println("SWT Webkit: forward() called after widget disposed. Should not have happened.\n" + getInternalErrorMsg());
		return false; // Disposed.
	}
	if (WebKitGTK.webkit_web_view_can_go_forward (webView) == 0) return false;
	WebKitGTK.webkit_web_view_go_forward (webView);
	return true;
}

@Override
public String getBrowserType () {
	return "webkit"; //$NON-NLS-1$
}

@Override
public String getText () {
	if (WEBKIT2) {
		return Webkit2AsyncToSync.getText(browser, webView);
	} else  {
		// Webkit1 only.
		int /*long*/ frame = WebKitGTK.webkit_web_view_get_main_frame (webView);
		int /*long*/ source = WebKitGTK.webkit_web_frame_get_data_source (frame);
		if (source == 0) return "";	//$NON-NLS-1$
		int /*long*/ data = WebKitGTK.webkit_web_data_source_get_data (source);
		if (data == 0) return "";	//$NON-NLS-1$

		int /*long*/ encoding = WebKitGTK.webkit_web_data_source_get_encoding (source);
		int length = C.strlen (encoding);
		byte[] bytes = new byte [length];
		C.memmove (bytes, encoding, length);
		String encodingString = new String (Converter.mbcsToWcs (bytes));

		length = OS.GString_len (data);
		bytes = new byte[length];
		int /*long*/ string = OS.GString_str (data);
		C.memmove (bytes, string, length);

		try {
			return new String (bytes, encodingString);
		} catch (UnsupportedEncodingException e) {
		}
		return new String (Converter.mbcsToWcs (bytes));
	}
}

@Override
public String getUrl () {
	if (webView == 0) {
		assert false;
		System.err.println("SWT Webkit: getUrl() called after widget disposed. Should not have happened.\n" + getInternalErrorMsg());
		return null; // Disposed.
	}
	int /*long*/ uri = WebKitGTK.webkit_web_view_get_uri (webView);

	/* WebKit auto-navigates to about:blank at startup */
	if (uri == 0) return ABOUT_BLANK;

	int length = C.strlen (uri);
	byte[] bytes = new byte[length];
	C.memmove (bytes, uri, length);

	String url = new String (Converter.mbcsToWcs (bytes));
	/*
	 * If the URI indicates that the page is being rendered from memory
	 * (via setText()) then set it to about:blank to be consistent with IE.
	 */
	if (url.equals (URI_FILEROOT)) {
		url = ABOUT_BLANK;
	} else {
		length = URI_FILEROOT.length ();
		if (url.startsWith (URI_FILEROOT) && url.charAt (length) == '#') {
			url = ABOUT_BLANK + url.substring (length);
		}
	}
	return url;
}

boolean handleDOMEvent (int /*long*/ event, int type) {
	/*
	* This method handles JS events that are received through the DOM
	* listener API that was introduced in WebKitGTK 1.4.
	*/
	String typeString = null;
	boolean isMouseEvent = false;
	switch (type) {
		case SWT.DragDetect: {
			typeString = "dragstart"; //$NON-NLS-1$
			isMouseEvent = true;
			break;
		}
		case SWT.MouseDown: {
			typeString = "mousedown"; //$NON-NLS-1$
			isMouseEvent = true;
			break;
		}
		case SWT.MouseMove: {
			typeString = "mousemove"; //$NON-NLS-1$
			isMouseEvent = true;
			break;
		}
		case SWT.MouseUp: {
			typeString = "mouseup"; //$NON-NLS-1$
			isMouseEvent = true;
			break;
		}
		case SWT.MouseWheel: {
			typeString = "mousewheel"; //$NON-NLS-1$
			isMouseEvent = true;
			break;
		}
		case SWT.KeyDown: {
			typeString = "keydown"; //$NON-NLS-1$
			break;
		}
		case SWT.KeyUp: {
			typeString = "keyup"; //$NON-NLS-1$
			break;
		}
		case SENTINEL_KEYPRESS: {
			typeString = "keypress"; //$NON-NLS-1$
			break;
		}
	}

	if (isMouseEvent) {
		int screenX = (int)WebKitGTK.webkit_dom_mouse_event_get_screen_x (event);
		int screenY = (int)WebKitGTK.webkit_dom_mouse_event_get_screen_y (event);
		int button = (int)WebKitGTK.webkit_dom_mouse_event_get_button (event) + 1;
		boolean altKey = WebKitGTK.webkit_dom_mouse_event_get_alt_key (event) != 0;
		boolean ctrlKey = WebKitGTK.webkit_dom_mouse_event_get_ctrl_key (event) != 0;
		boolean shiftKey = WebKitGTK.webkit_dom_mouse_event_get_shift_key (event) != 0;
		boolean metaKey = WebKitGTK.webkit_dom_mouse_event_get_meta_key (event) != 0;
		int detail = (int)WebKitGTK.webkit_dom_ui_event_get_detail (event);
		boolean hasRelatedTarget = false; //WebKitGTK.webkit_dom_mouse_event_get_related_target (event) != 0;
		return handleMouseEvent(typeString, screenX, screenY, detail, button, altKey, ctrlKey, shiftKey, metaKey, hasRelatedTarget);
	}

	/* key event */
	int keyEventState = 0;
	int /*long*/ eventPtr = GTK.gtk_get_current_event ();
	if (eventPtr != 0) {
		GdkEventKey gdkEvent = new GdkEventKey ();
		OS.memmove (gdkEvent, eventPtr, GdkEventKey.sizeof);
		switch (gdkEvent.type) {
			case GDK.GDK_KEY_PRESS:
			case GDK.GDK_KEY_RELEASE:
				keyEventState = gdkEvent.state;
				break;
		}
		GDK.gdk_event_free (eventPtr);
	}
	int keyCode = (int)WebKitGTK.webkit_dom_ui_event_get_key_code (event);
	int charCode = (int)WebKitGTK.webkit_dom_ui_event_get_char_code (event);
	boolean altKey = (keyEventState & GDK.GDK_MOD1_MASK) != 0;
	boolean ctrlKey = (keyEventState & GDK.GDK_CONTROL_MASK) != 0;
	boolean shiftKey = (keyEventState & GDK.GDK_SHIFT_MASK) != 0;
	return handleKeyEvent(typeString, keyCode, charCode, altKey, ctrlKey, shiftKey, false);
}

boolean handleEventFromFunction (Object[] arguments) {
	/*
	* Prior to WebKitGTK 1.4 there was no API for hooking DOM listeners.
	* As a workaround, eventFunction was introduced to capture JS events
	* and report them back to the java side.  This method handles these
	* events by extracting their arguments and passing them to the
	* handleKeyEvent()/handleMouseEvent() event handler methods.
	*/

	/*
	* The arguments for key events are:
	* 	argument 0: type (String)
	* 	argument 1: keyCode (Double)
	* 	argument 2: charCode (Double)
	* 	argument 3: altKey (Boolean)
	* 	argument 4: ctrlKey (Boolean)
	* 	argument 5: shiftKey (Boolean)
	* 	argument 6: metaKey (Boolean)
	* 	returns doit
	*
	* The arguments for mouse events are:
	* 	argument 0: type (String)
	* 	argument 1: screenX (Double)
	* 	argument 2: screenY (Double)
	* 	argument 3: detail (Double)
	* 	argument 4: button (Double)
	* 	argument 5: altKey (Boolean)
	* 	argument 6: ctrlKey (Boolean)
	* 	argument 7: shiftKey (Boolean)
	* 	argument 8: metaKey (Boolean)
	* 	argument 9: hasRelatedTarget (Boolean)
	* 	returns doit
	*/

	String type = (String)arguments[0];
	if (type.equals (DOMEVENT_KEYDOWN) || type.equals (DOMEVENT_KEYPRESS) || type.equals (DOMEVENT_KEYUP)) {
		return handleKeyEvent(
			type,
			((Double)arguments[1]).intValue (),
			((Double)arguments[2]).intValue (),
			((Boolean)arguments[3]).booleanValue (),
			((Boolean)arguments[4]).booleanValue (),
			((Boolean)arguments[5]).booleanValue (),
			((Boolean)arguments[6]).booleanValue ());
	}

	return handleMouseEvent(
		type,
		((Double)arguments[1]).intValue (),
		((Double)arguments[2]).intValue (),
		((Double)arguments[3]).intValue (),
		(arguments[4] != null ? ((Double)arguments[4]).intValue () : 0) + 1,
		((Boolean)arguments[5]).booleanValue (),
		((Boolean)arguments[6]).booleanValue (),
		((Boolean)arguments[7]).booleanValue (),
		((Boolean)arguments[8]).booleanValue (),
		((Boolean)arguments[9]).booleanValue ());
}

boolean handleKeyEvent (String type, int keyCode, int charCode, boolean altKey, boolean ctrlKey, boolean shiftKey, boolean metaKey) {
	if (type.equals (DOMEVENT_KEYDOWN)) {
		keyCode = translateKey (keyCode);
		lastKeyCode = keyCode;
		switch (keyCode) {
			case SWT.SHIFT:
			case SWT.CONTROL:
			case SWT.ALT:
			case SWT.CAPS_LOCK:
			case SWT.NUM_LOCK:
			case SWT.SCROLL_LOCK:
			case SWT.COMMAND:
			case SWT.ESC:
			case SWT.TAB:
			case SWT.PAUSE:
			case SWT.BS:
			case SWT.INSERT:
			case SWT.DEL:
			case SWT.HOME:
			case SWT.END:
			case SWT.PAGE_UP:
			case SWT.PAGE_DOWN:
			case SWT.ARROW_DOWN:
			case SWT.ARROW_UP:
			case SWT.ARROW_LEFT:
			case SWT.ARROW_RIGHT:
			case SWT.F1:
			case SWT.F2:
			case SWT.F3:
			case SWT.F4:
			case SWT.F5:
			case SWT.F6:
			case SWT.F7:
			case SWT.F8:
			case SWT.F9:
			case SWT.F10:
			case SWT.F11:
			case SWT.F12: {
				/* keypress events will not be received for these keys, so send KeyDowns for them now */

				Event keyEvent = new Event ();
				keyEvent.widget = browser;
				keyEvent.type = type.equals (DOMEVENT_KEYDOWN) ? SWT.KeyDown : SWT.KeyUp;
				keyEvent.keyCode = keyCode;
				switch (keyCode) {
					case SWT.BS: keyEvent.character = SWT.BS; break;
					case SWT.DEL: keyEvent.character = SWT.DEL; break;
					case SWT.ESC: keyEvent.character = SWT.ESC; break;
					case SWT.TAB: keyEvent.character = SWT.TAB; break;
				}
				lastCharCode = keyEvent.character;
				keyEvent.stateMask = (altKey ? SWT.ALT : 0) | (ctrlKey ? SWT.CTRL : 0) | (shiftKey ? SWT.SHIFT : 0) | (metaKey ? SWT.COMMAND : 0);
				keyEvent.stateMask &= ~keyCode;		/* remove current keydown if it's a state key */
				final int stateMask = keyEvent.stateMask;
				if (!sendKeyEvent (keyEvent) || browser.isDisposed ()) return false;

				if (browser.isFocusControl ()) {
					if (keyCode == SWT.TAB && (stateMask & (SWT.CTRL | SWT.ALT)) == 0) {
						browser.getDisplay ().asyncExec (() -> {
							if (browser.isDisposed ()) return;
							if (browser.getDisplay ().getFocusControl () == null) {
								int traversal = (stateMask & SWT.SHIFT) != 0 ? SWT.TRAVERSE_TAB_PREVIOUS : SWT.TRAVERSE_TAB_NEXT;
								browser.traverse (traversal);
							}
						});
					}
				}
				break;
			}
		}
		return true;
	}

	if (type.equals (DOMEVENT_KEYPRESS)) {
		/*
		* if keydown could not determine a keycode for this key then it's a
		* key for which key events are not sent (eg.- the Windows key)
		*/
		if (lastKeyCode == 0) return true;

		lastCharCode = charCode;
		if (ctrlKey && (0 <= lastCharCode && lastCharCode <= 0x7F)) {
			if ('a' <= lastCharCode && lastCharCode <= 'z') lastCharCode -= 'a' - 'A';
			if (64 <= lastCharCode && lastCharCode <= 95) lastCharCode -= 64;
		}

		Event keyEvent = new Event ();
		keyEvent.widget = browser;
		keyEvent.type = SWT.KeyDown;
		keyEvent.keyCode = lastKeyCode;
		keyEvent.character = (char)lastCharCode;
		keyEvent.stateMask = (altKey ? SWT.ALT : 0) | (ctrlKey ? SWT.CTRL : 0) | (shiftKey ? SWT.SHIFT : 0) | (metaKey ? SWT.COMMAND : 0);
		return sendKeyEvent (keyEvent) && !browser.isDisposed ();
	}

	/* keyup */

	keyCode = translateKey (keyCode);
	if (keyCode == 0) {
		/* indicates a key for which key events are not sent */
		return true;
	}
	if (keyCode != lastKeyCode) {
		/* keyup does not correspond to the last keydown */
		lastKeyCode = keyCode;
		lastCharCode = 0;
	}

	Event keyEvent = new Event ();
	keyEvent.widget = browser;
	keyEvent.type = SWT.KeyUp;
	keyEvent.keyCode = lastKeyCode;
	keyEvent.character = (char)lastCharCode;
	keyEvent.stateMask = (altKey ? SWT.ALT : 0) | (ctrlKey ? SWT.CTRL : 0) | (shiftKey ? SWT.SHIFT : 0) | (metaKey ? SWT.COMMAND : 0);
	switch (lastKeyCode) {
		case SWT.SHIFT:
		case SWT.CONTROL:
		case SWT.ALT:
		case SWT.COMMAND: {
			keyEvent.stateMask |= lastKeyCode;
		}
	}
	browser.notifyListeners (keyEvent.type, keyEvent);
	lastKeyCode = lastCharCode = 0;
	return keyEvent.doit && !browser.isDisposed ();
}

boolean handleMouseEvent (String type, int screenX, int screenY, int detail, int button, boolean altKey, boolean ctrlKey, boolean shiftKey, boolean metaKey, boolean hasRelatedTarget) {
	/*
	 * MouseOver and MouseOut events are fired any time the mouse enters or exits
	 * any element within the Browser.  To ensure that SWT events are only
	 * fired for mouse movements into or out of the Browser, do not fire an
	 * event if there is a related target element.
	 */

	/*
	* The following is intentionally commented because MouseOver and MouseOut events
	* are not being hooked until https://bugs.webkit.org/show_bug.cgi?id=35246 is fixed.
	*/
	//if (type.equals (DOMEVENT_MOUSEOVER) || type.equals (DOMEVENT_MOUSEOUT)) {
	//	if (((Boolean)arguments[9]).booleanValue ()) return true;
	//}

	/*
	 * The position of mouse events is received in screen-relative coordinates
	 * in order to handle pages with frames, since frames express their event
	 * coordinates relative to themselves rather than relative to their top-
	 * level page.  Convert screen-relative coordinates to be browser-relative.
	 */
	Point position = new Point (screenX, screenY);
	position = browser.getDisplay ().map (null, browser, position);

	Event mouseEvent = new Event ();
	mouseEvent.widget = browser;
	mouseEvent.x = position.x;
	mouseEvent.y = position.y;
	int mask = (altKey ? SWT.ALT : 0) | (ctrlKey ? SWT.CTRL : 0) | (shiftKey ? SWT.SHIFT : 0) | (metaKey ? SWT.COMMAND : 0);
	mouseEvent.stateMask = mask;

	if (type.equals (DOMEVENT_MOUSEDOWN)) {
		mouseEvent.type = SWT.MouseDown;
		mouseEvent.count = detail;
		mouseEvent.button = button;
		browser.notifyListeners (mouseEvent.type, mouseEvent);
		if (browser.isDisposed ()) return true;
		if (detail == 2) {
			mouseEvent = new Event ();
			mouseEvent.type = SWT.MouseDoubleClick;
			mouseEvent.widget = browser;
			mouseEvent.x = position.x;
			mouseEvent.y = position.y;
			mouseEvent.stateMask = mask;
			mouseEvent.count = detail;
			mouseEvent.button = button;
			browser.notifyListeners (mouseEvent.type, mouseEvent);
		}
		return true;
	}

	if (type.equals (DOMEVENT_MOUSEUP)) {
		mouseEvent.type = SWT.MouseUp;
		mouseEvent.count = detail;
		mouseEvent.button = button;
	} else if (type.equals (DOMEVENT_MOUSEMOVE)) {
		mouseEvent.type = SWT.MouseMove;
	} else if (type.equals (DOMEVENT_MOUSEWHEEL)) {
		mouseEvent.type = SWT.MouseWheel;
		mouseEvent.count = detail;

	/*
	* The following is intentionally commented because MouseOver and MouseOut events
	* are not being hooked until https://bugs.webkit.org/show_bug.cgi?id=35246 is fixed.
	*/
	//} else if (type.equals (DOMEVENT_MOUSEOVER)) {
	//	mouseEvent.type = SWT.MouseEnter;
	//} else if (type.equals (DOMEVENT_MOUSEOUT)) {
	//	mouseEvent.type = SWT.MouseExit;

	} else if (type.equals (DOMEVENT_DRAGSTART)) {
		mouseEvent.type = SWT.DragDetect;
		mouseEvent.button = button;
		switch (mouseEvent.button) {
			case 1: mouseEvent.stateMask |= SWT.BUTTON1; break;
			case 2: mouseEvent.stateMask |= SWT.BUTTON2; break;
			case 3: mouseEvent.stateMask |= SWT.BUTTON3; break;
			case 4: mouseEvent.stateMask |= SWT.BUTTON4; break;
			case 5: mouseEvent.stateMask |= SWT.BUTTON5; break;
		}
	}

	browser.notifyListeners (mouseEvent.type, mouseEvent);
	return true;
}

int /*long*/ handleLoadCommitted (int /*long*/ uri, boolean top) {
	int length = C.strlen (uri);
	byte[] bytes = new byte[length];
	C.memmove (bytes, uri, length);
	String url = new String (Converter.mbcsToWcs (bytes));
	/*
	 * If the URI indicates that the page is being rendered from memory
	 * (via setText()) then set it to about:blank to be consistent with IE.
	 */
	if (url.equals (URI_FILEROOT)) {
		url = ABOUT_BLANK;
	} else {
		length = URI_FILEROOT.length ();
		if (url.startsWith (URI_FILEROOT) && url.charAt (length) == '#') {
			url = ABOUT_BLANK + url.substring (length);
		}
	}

	// Bug 511797 : On webkit2, this code is only reached once per page load.
	if (WEBKIT1) {
		/*
		* Webkit1:
		* Each invocation of setText() causes webkit_notify_load_status to be invoked
		* twice, once for the initial navigate to about:blank, and once for the auto-navigate
		* to about:blank that WebKit does when webkit_web_view_load_string is invoked.  If
		* this is the first webkit_notify_load_status callback received for a setText()
		* invocation then do not send any events or re-install registered BrowserFunctions.
		*/
		if (top && url.startsWith(ABOUT_BLANK) && htmlBytes != null) return 0;
	}

	LocationEvent event = new LocationEvent (browser);
	event.display = browser.getDisplay ();
	event.widget = browser;
	event.location = url;
	event.top = top;
	Runnable fireLocationChanged = () ->  {
		if (browser.isDisposed ()) return;
		for (int i = 0; i < locationListeners.length; i++) {
			locationListeners[i].changed (event);
		}
	};
	if (WEBKIT2) {
		browser.getDisplay().asyncExec(fireLocationChanged);
	} else {
		fireLocationChanged.run();
	}
	return 0;
}

private void fireNewTitleEvent(String title){
	if (WEBKIT1) {
		// titleListener is already handled/fired in webkit_notify_title()
		// [which is triggered by 'notify::title'. No need to fire it twice.
		//
		// This function is called by load_change / notify_load_status, which doesn't necessarily mean the
		// title has actually changed. Further title can also be changed by javascript on the same page,
		// thus page_load is not a proper way to trigger title_change.
		// It's not clear when notify::title was introduced, (sometime in Webkit1 by the looks?)
		// thus keeping code below for webkit1/legacy reasons.
		TitleEvent newEvent = new TitleEvent (browser);
		newEvent.display = browser.getDisplay ();
		newEvent.widget = browser;
		newEvent.title = title;
		for (int i = 0; i < titleListeners.length; i++) {
			titleListeners[i].changed (newEvent);
		}
	}
}

/**
 * This method is reached by:
 * Webkit1: WebkitWebView notify::load-status
 *  - simple change in property
 * 	- https://webkitgtk.org/reference/webkitgtk/unstable/webkitgtk-webkitwebview.html#WebKitWebView--load-status
 *
 * Webkit2: WebKitWebView load-changed signal
 * 	- void user_function (WebKitWebView  *web_view, WebKitLoadEvent load_event, gpointer user_data)
 *  - https://webkitgtk.org/reference/webkit2gtk/stable/WebKitWebView.html#WebKitWebView-load-changed
 *  - Note: As there is no return value, safe to fire asynchronously.
 */
private void fireProgressCompletedEvent(){
	Runnable fireProgressEvents = () -> {
		if (browser.isDisposed() || progressListeners == null) return;
		ProgressEvent progress = new ProgressEvent (browser);
		progress.display = browser.getDisplay ();
		progress.widget = browser;
		progress.current = MAX_PROGRESS;
		progress.total = MAX_PROGRESS;
		for (int i = 0; i < progressListeners.length; i++) {
			progressListeners[i].completed (progress);
		}
	};
	if (WEBKIT2)
		browser.getDisplay().asyncExec(fireProgressEvents);
	else
		fireProgressEvents.run();
}

/** Webkit1 only.
 *  (Webkit2 equivalent is webkit_load_changed())
 */
int /*long*/ handleLoadFinished (int /*long*/ uri, boolean top) {
	assert WEBKIT1 : WebKitGTK.Webkit1AssertMsg;
	int length = C.strlen (uri);
	byte[] bytes = new byte[length];
	C.memmove (bytes, uri, length);
	String url = new String (Converter.mbcsToWcs (bytes));
	/*
	 * If the URI indicates that the page is being rendered from memory
	 * (via setText()) then set it to about:blank to be consistent with IE.
	 */
	if (url.equals (URI_FILEROOT)) {
		url = ABOUT_BLANK;
	} else {
		length = URI_FILEROOT.length ();
		if (url.startsWith (URI_FILEROOT) && url.charAt (length) == '#') {
			url = ABOUT_BLANK + url.substring (length);
		}
	}

	/*
	 * If htmlBytes is not null then there is html from a previous setText() call
	 * waiting to be set into the about:blank page once it has completed loading.
	 */
	if (top && htmlBytes != null) {
		if (url.startsWith(ABOUT_BLANK)) {
			loadingText = true;
			byte[] mimeType = Converter.wcsToMbcs ("text/html", true);  //$NON-NLS-1$
			byte[] encoding = Converter.wcsToMbcs (StandardCharsets.UTF_8.displayName(), true);  //$NON-NLS-1$
			byte[] uriBytes;
			if (untrustedText) {
				uriBytes = Converter.wcsToMbcs (ABOUT_BLANK, true);
			} else {
				uriBytes = Converter.wcsToMbcs (URI_FILEROOT, true);
			}
			WebKitGTK.webkit_web_view_load_string (webView, htmlBytes, mimeType, encoding, uriBytes);
			htmlBytes = null;
		}
	}

	/*
	* The webkit_web_view_load_string() invocation above will trigger a second
	* webkit_web_view_load_string callback when it is completed.  Wait for this
	* second callback to come before sending the title or completed events.
	*/
	if (!loadingText) {
		/*
		* To be consistent with other platforms a title event should be fired
		* when a top-level page has completed loading.  A page with a <title>
		* tag will do this automatically when the notify::title signal is received.
		* However a page without a <title> tag will not do this by default, so fire
		* the event here with the page's url as the title.
		*/
		if (top) {
			int /*long*/ frame = WebKitGTK.webkit_web_view_get_main_frame (webView);
			int /*long*/ title = WebKitGTK.webkit_web_frame_get_title (frame);
			if (title == 0) {
				fireNewTitleEvent(url);
				if (browser.isDisposed ()) return 0;
			}
		}

		fireProgressCompletedEvent();
	}
	loadingText = false;

	return 0;
}

@Override
public boolean isBackEnabled () {
	if (WEBKIT2 && webView == 0)
		return false; //disposed.
	return WebKitGTK.webkit_web_view_can_go_back (webView) != 0;
}

@Override
public boolean isForwardEnabled () {
	return WebKitGTK.webkit_web_view_can_go_forward (webView) != 0;
}

void onDispose (Event e) {
	/* Browser could have been disposed by one of the Dispose listeners */
	if (!browser.isDisposed()) {
		/* invoke onbeforeunload handlers */
		if (!browser.isClosing) {
			close (false);
		}
	}

	for (BrowserFunction function : functions.values()) {
		function.dispose(false);
	}
	functions = null;

	if (WEBKIT1) {
		// event function/external object only used by webkit1. For Webkit2, see Webkit2JavaCallback
		if (eventFunction != null) {
			eventFunction.dispose (false);
			eventFunction = null;
		}
		C.free (webViewData);
		postData = null;
		headers = null;
		htmlBytes = null;
	}
	if (WEBKIT2 && WebKitGTK.webkit_get_minor_version() >= 18) {
		// Bug 530678.
		// * As of Webkit 2.18, (it seems) webkitGtk auto-disposes itself when the parent is disposed.
		// * This can cause a deadlock inside Webkit process if WebkitGTK widget's parent is disposed during a callback.
		//   This is because webkit process is waiting for it's callback to finish which never completes
		//   because parent's disposal also disposed webkitGTK widget. (Note Webkit process vs WebkitGtk widget).
		// * To break the deadlock, we unparent webkitGtk temporarily and unref (dispose) it later after callback is done.
		//
		// If you change dispose logic, to check that you haven't introduced memory leaks, test via:
		// org.eclipse.swt.tests.junit.memoryleak.Test_Memory_Leak.test_Browser()
		OS.g_object_ref (webView);
		GTK.gtk_container_remove (GTK.gtk_widget_get_parent (webView), webView);
		int /*long*/ webViewTempRef = webView;
		browser.getDisplay().asyncExec(() -> {
			OS.g_object_unref (webViewTempRef);
		});
		webView = 0;
	}
}

void onResize (Event e) {
	Rectangle rect = DPIUtil.autoScaleUp(browser.getClientArea ());
	if (WEBKIT2){
		if (webView == 0)
			return;
		GTK.gtk_widget_set_size_request (webView, rect.width, rect.height);
	} else {
		GTK.gtk_widget_set_size_request (scrolledWindow, rect.width, rect.height);
	}
}

void openDownloadWindow (final int /*long*/ webkitDownload) {
	assert WEBKIT1 : WebKitGTK.Webkit1AssertMsg;
	openDownloadWindow(webkitDownload, null);
}

void openDownloadWindow (final int /*long*/ webkitDownload, final String suggested_filename) {
	final Shell shell = new Shell ();
	String msg = Compatibility.getMessage ("SWT_FileDownload"); //$NON-NLS-1$
	shell.setText (msg);
	GridLayout gridLayout = new GridLayout ();
	gridLayout.marginHeight = 15;
	gridLayout.marginWidth = 15;
	gridLayout.verticalSpacing = 20;
	shell.setLayout (gridLayout);

	String nameString;
	if (WEBKIT1) {
		int /*long*/ name = WebKitGTK.webkit_download_get_suggested_filename (webkitDownload);
		int length = C.strlen (name);
		byte[] bytes = new byte[length];
		C.memmove (bytes, name, length);
		nameString = new String (Converter.mbcsToWcs (bytes));
	} else {
		nameString = suggested_filename;
	}

	int /*long*/ url;
	if (WEBKIT1) {
		url = WebKitGTK.webkit_download_get_uri (webkitDownload);
	} else {
		int /*long*/ request = WebKitGTK.webkit_download_get_request(webkitDownload);
		url = WebKitGTK.webkit_uri_request_get_uri(request);
	}
	int length = C.strlen (url);
	byte[] bytes = new byte[length];
	C.memmove (bytes, url, length);
	String urlString = new String (Converter.mbcsToWcs (bytes));
	msg = Compatibility.getMessage ("SWT_Download_Location", new Object[] {nameString, urlString}); //$NON-NLS-1$
	Label nameLabel = new Label (shell, SWT.WRAP);
	nameLabel.setText (msg);
	GridData data = new GridData ();
	Monitor monitor = browser.getMonitor ();
	int maxWidth = monitor.getBounds ().width / 2;
	int width = nameLabel.computeSize (SWT.DEFAULT, SWT.DEFAULT).x;
	data.widthHint = Math.min (width, maxWidth);
	data.horizontalAlignment = GridData.FILL;
	data.grabExcessHorizontalSpace = true;
	nameLabel.setLayoutData (data);

	final Label statusLabel = new Label (shell, SWT.NONE);
	statusLabel.setText (Compatibility.getMessage ("SWT_Download_Started")); //$NON-NLS-1$
	data = new GridData (GridData.FILL_BOTH);
	statusLabel.setLayoutData (data);

	final Button cancel = new Button (shell, SWT.PUSH);
	cancel.setText (Compatibility.getMessage ("SWT_Cancel")); //$NON-NLS-1$
	data = new GridData ();
	data.horizontalAlignment = GridData.CENTER;
	cancel.setLayoutData (data);
	final Listener cancelListener = event -> {
		if (WEBKIT2) {
			webKitDownloadStatus.put(new LONG(webkitDownload), WebKitGTK.WEBKIT_DOWNLOAD_STATUS_CANCELLED);
		}
		WebKitGTK.webkit_download_cancel (webkitDownload);
	};
	cancel.addListener (SWT.Selection, cancelListener);

	OS.g_object_ref (webkitDownload);
	final Display display = browser.getDisplay ();
	final int INTERVAL = 500;
	display.timerExec (INTERVAL, new Runnable () {
		@Override
		public void run () {
			int status = 0; // 0 allows download window to continue
			if (WEBKIT1) {
				status = WebKitGTK.webkit_download_get_status (webkitDownload);
			} else {
				status = webKitDownloadStatus.containsKey(new LONG(webkitDownload)) ? webKitDownloadStatus.get(new LONG(webkitDownload)) : 0;
			}
			if (shell.isDisposed () || status == WebKitGTK.WEBKIT_DOWNLOAD_STATUS_FINISHED || status == WebKitGTK.WEBKIT_DOWNLOAD_STATUS_CANCELLED) {
				shell.dispose ();
				display.timerExec (-1, this);
				OS.g_object_unref (webkitDownload);
				if (WEBKIT2) {
					webKitDownloadStatus.remove(new LONG(webkitDownload));
				}
				return;
			}
			if (status == WebKitGTK.WEBKIT_DOWNLOAD_STATUS_ERROR) {
				statusLabel.setText (Compatibility.getMessage ("SWT_Download_Error")); //$NON-NLS-1$
				display.timerExec (-1, this);
				OS.g_object_unref (webkitDownload);
				cancel.removeListener (SWT.Selection, cancelListener);
				cancel.addListener (SWT.Selection, event -> shell.dispose ());
				if (WEBKIT2) {
					webKitDownloadStatus.remove(new LONG(webkitDownload));
				}
				return;
			}

			long current = 0;
			long total = 0;
			if (WEBKIT1) {
				current = WebKitGTK.webkit_download_get_current_size (webkitDownload) / 1024L;
				total = WebKitGTK.webkit_download_get_total_size (webkitDownload) / 1024L;
			} else {
				current = WebKitGTK.webkit_download_get_received_data_length(webkitDownload) / 1024L;
				int /*long*/ response = WebKitGTK.webkit_download_get_response(webkitDownload);
				total = WebKitGTK.webkit_uri_response_get_content_length(response) / 1024L;
			}
			String message = Compatibility.getMessage ("SWT_Download_Status", new Object[] {Long.valueOf(current), new Long(total)}); //$NON-NLS-1$
			statusLabel.setText (message);
			display.timerExec (INTERVAL, this);
		}
	});

	shell.pack ();
	shell.open ();
}

@Override
public void refresh () {
	if (WEBKIT2 && webView == 0)
		return; //disposed.
	WebKitGTK.webkit_web_view_reload (webView);
}

@Override
public boolean setText (String html, boolean trusted) {
	/* convert the String containing HTML to an array of bytes with UTF-8 data */
	byte[] html_bytes = (html + '\0').getBytes (StandardCharsets.UTF_8); //$NON-NLS-1$

	/*
	* If this.htmlBytes is not null then the about:blank page is already being loaded,
	* so no navigate is required.  Just set the html that is to be shown.
	*/
	boolean blankLoading = htmlBytes != null; // Webkit1 only.
	if (WEBKIT1) {
		this.htmlBytes = html_bytes;
		untrustedText = !trusted;
	}

	if (WEBKIT2) {
		w2_bug527738LastRequestCounter.incrementAndGet();
		byte[] uriBytes;
		if (!trusted) {
			uriBytes = Converter.wcsToMbcs (ABOUT_BLANK, true);
		} else {
			uriBytes = Converter.wcsToMbcs (URI_FILEROOT, true);
		}
		WebKitGTK.webkit_web_view_load_html (webView, html_bytes, uriBytes);
	} else {
		if (blankLoading) return true;

		byte[] uriBytes = Converter.wcsToMbcs (ABOUT_BLANK, true);
		WebKitGTK.webkit_web_view_load_uri (webView, uriBytes);
	}

	return true;
}

@Override
public boolean setUrl (String url, String postData, String[] headers) {
	if (WEBKIT1) {
		this.postData = postData;
		this.headers = headers;
	}
	if (WEBKIT2) {
		w2_bug527738LastRequestCounter.incrementAndGet();
	}

	if (WEBKIT2 && webView == 0)
		return false; // disposed.

	/*
	* WebKitGTK attempts to open the exact url string that is passed to it and
	* will not infer a protocol if it's not specified.  Detect the case of an
	* invalid URL string and try to fix it by prepending an appropriate protocol.
	*/
	try {
		new URL(url);
	} catch (MalformedURLException e) {
		String testUrl = null;
		if (url.charAt (0) == SEPARATOR_FILE) {
			/* appears to be a local file */
			testUrl = PROTOCOL_FILE + url;
		} else {
			testUrl = PROTOCOL_HTTP + url;
		}
		try {
			new URL (testUrl);
			url = testUrl;		/* adding the protocol made the url valid */
		} catch (MalformedURLException e2) {
			/* adding the protocol did not make the url valid, so do nothing */
		}
	}

	/*
	* Feature of WebKit.  The user-agent header value cannot be overridden
	* by changing it in the resource request.  The workaround is to detect
	* here whether the user-agent is being overridden, and if so, temporarily
	* set the value on the WebView when initiating the load request and then
	* remove it afterwards.
	*/
	int /*long*/ settings = WebKitGTK.webkit_web_view_get_settings (webView);
	if (headers != null) {
		for (int i = 0; i < headers.length; i++) {
			String current = headers[i];
			if (current != null) {
				int index = current.indexOf (':');
				if (index != -1) {
					String key = current.substring (0, index).trim ();
					String value = current.substring (index + 1).trim ();
					if (key.length () > 0 && value.length () > 0) {
						if (key.equalsIgnoreCase (USER_AGENT)) {
							byte[] bytes = Converter.wcsToMbcs (value, true);
							OS.g_object_set (settings, WebKitGTK.user_agent, bytes, 0);
						}
					}
				}
			}
		}
	}

	byte[] uriBytes = Converter.wcsToMbcs (url, true);

	if (WEBKIT2 && postData==null && headers != null) {
		int /*long*/ request = WebKitGTK.webkit_uri_request_new (uriBytes);
		int /*long*/ requestHeaders = WebKitGTK.webkit_uri_request_get_http_headers (request);
		if (requestHeaders != 0) {
			addRequestHeaders(requestHeaders, headers);
		}
		WebKitGTK.webkit_web_view_load_request (webView, request);
		OS.g_object_set (settings, WebKitGTK.user_agent, 0, 0);
		return true;
	}

	// Bug 527738
	// Webkit2 doesn't have api to set url with data. (2.18). While we wait for them to implement,
	// this  workaround uses java to query a server and then manually populate webkit with content.
	// This should be version guarded and replaced with proper functions once webkit2 has implemented api.
	if (WEBKIT2 && postData != null
			&& OS.GLIB_VERSION >= OS.VERSION(2, 32, 0)) {  // OS.g_bytes_new (and unref) introduced in glib 2.32
		final String base_url = url;

		// Use Webkit User-Agent
		int /*long*/ [] user_agent_str_ptr = new int /*long*/ [1];
		OS.g_object_get (settings, WebKitGTK.user_agent, user_agent_str_ptr, 0);
		final String userAgent = Converter.cCharPtrToJavaString(user_agent_str_ptr[0], true);
		final int lastRequest = w2_bug527738LastRequestCounter.incrementAndGet(); // Webkit 2 only
		Thread send_request = new Thread(() -> {
			String html = null;
			String mime_type = null;
			String encoding_type = null;
			try {
				URL base = new URL(base_url);
				URLConnection url_conn = base.openConnection();
				if (url_conn instanceof HttpURLConnection) {
					HttpURLConnection conn = (HttpURLConnection) url_conn;

					{ // Configure connection.
						conn.setRequestMethod("POST"); //$NON-NLS-1$

						// Use Webkit Accept
						conn.setRequestProperty( "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"); //$NON-NLS-1$ $NON-NLS-2$

						conn.setRequestProperty("User-Agent", userAgent); //$NON-NLS-1$
						conn.setDoOutput(true); // because default value is false

						// Set headers
						if (headers != null) {
							for (String header : headers) {
								int index = header.indexOf(':');
								if (index > 0) {
									String key = header.substring(0, index).trim();
									String value = header.substring(index + 1).trim();
									conn.setRequestProperty(key, value);
								}
							}
						}
					}

					{ // Query server
						try (OutputStream out = conn.getOutputStream()) {
							out.write(postData.getBytes());
						}

						StringBuilder response = new StringBuilder();
						try (BufferedReader buff = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
							char [] cbuff = new char[4096];
							while (buff.read(cbuff, 0, cbuff.length) > 0) {
								response.append(new String(cbuff));
								Arrays.fill(cbuff, '\0');
							}
						}
						html = response.toString();
					}

					{ // Extract result meta data
						// Get Media Type from Content-Type
						String content_type = conn.getContentType();
						int paramaterSeparatorIndex = content_type.indexOf(';');
						mime_type = paramaterSeparatorIndex > 0 ? content_type.substring(0, paramaterSeparatorIndex) : content_type;

						// Get Encoding if defined
						if (content_type.indexOf(';') > 0) {
							String [] attrs = content_type.split(";");
							for (String attr : attrs) {
								int i = attr.indexOf('=');
								if (i > 0) {
									String key = attr.substring(0, i).trim();
									String value = attr.substring(i + 1).trim();
									if ("charset".equalsIgnoreCase(key)) { //$NON-NLS-1$
										encoding_type = value;
									}
								}
							}
						}
					}
				}
			} catch (IOException e) { // MalformedURLException is an IOException also.
				html = e.getMessage();
			} finally {
				if (html != null && lastRequest == w2_bug527738LastRequestCounter.get()) {
					final String final_html = html;
					final String final_mime_type = mime_type;
					final String final_encoding_type = encoding_type;
					Display.getDefault().syncExec(() -> {
						byte [] html_bytes = Converter.wcsToMbcs(final_html, false);
						byte [] mime_type_bytes = final_mime_type != null ? Converter.javaStringToCString(final_mime_type) : Converter.javaStringToCString("text/plain");
						byte [] encoding_bytes = final_encoding_type != null ? Converter.wcsToMbcs(final_encoding_type, true) : new byte [] {0};
						int /*long*/ gByte = OS.g_bytes_new(html_bytes, html_bytes.length);
						WebKitGTK.webkit_web_view_load_bytes (webView, gByte, mime_type_bytes, encoding_bytes, uriBytes);
						OS.g_bytes_unref (gByte); // as per glib/tests/keyfile:test_bytes()..
						OS.g_object_set (settings, WebKitGTK.user_agent, 0, 0);
					});
				}
			}
		});
		send_request.start();
	} else {
		WebKitGTK.webkit_web_view_load_uri (webView, uriBytes);
	}

	// Handle when !(WEBKIT2 && postData != null)
	if (WEBKIT1 || (WEBKIT2 && postData == null)) {
		OS.g_object_set (settings, WebKitGTK.user_agent, 0, 0);
	}
	return true;
}

@Override
public void stop () {
	WebKitGTK.webkit_web_view_stop_loading (webView);
}

int /*long*/ webframe_notify_load_status (int /*long*/ web_frame, int /*long*/ pspec) {
	assert WEBKIT1 : WebKitGTK.Webkit1AssertMsg;
	int status = WebKitGTK.webkit_web_frame_get_load_status (web_frame);
	switch (status) {
		case WebKitGTK.WEBKIT_LOAD_COMMITTED: {
			int /*long*/ uri = WebKitGTK.webkit_web_frame_get_uri (web_frame);
			return handleLoadCommitted (uri, false);
		}
		case WebKitGTK.WEBKIT_LOAD_FINISHED: {
			/*
			* If this frame navigation was isolated to this frame (eg.- a link was
			* clicked in the frame, as opposed to this frame being created in
			* response to navigating to a main document containing frames) then
			* treat this as a completed load.
			*/
			int /*long*/ parentFrame = WebKitGTK.webkit_web_frame_get_parent (web_frame);
			if (WebKitGTK.webkit_web_frame_get_load_status (parentFrame) == WebKitGTK.WEBKIT_LOAD_FINISHED) {
				int /*long*/ uri = WebKitGTK.webkit_web_frame_get_uri (web_frame);
				return handleLoadFinished (uri, false);
			}
		}
	}
	return 0;
}

/**
 * Webkit1:
 *  - WebkitWebView 'close-web-view' signal.
 * 	- gboolean user_function (WebKitWebView *web_view, gpointer user_data);   // observe return value.
 *	- https://webkitgtk.org/reference/webkitgtk/unstable/webkitgtk-webkitwebview.html#WebKitWebView-close-web-view
 *
 * Webkit2:
 * 	- WebKitWebView 'close' signal
 *  - void user_function (WebKitWebView *web_view, gpointer user_data); // observe *no* return value.
 *  - https://webkitgtk.org/reference/webkit2gtk/stable/WebKitWebView.html#WebKitWebView-close
 */
int /*long*/ webkit_close_web_view (int /*long*/ web_view) {
	WindowEvent newEvent = new WindowEvent (browser);
	newEvent.display = browser.getDisplay ();
	newEvent.widget = browser;
	Runnable fireCloseWindowListeners = () -> {
		if (browser.isDisposed()) return;
		for (int i = 0; i < closeWindowListeners.length; i++) {
			closeWindowListeners[i].close (newEvent);
		}
		browser.dispose ();
	};
	if (WEBKIT2) {
		 // There is a subtle difference in Webkit1 vs Webkit2, in that on webkit2 this signal doesn't expect a return value.
		 // As such, we can safley execute the SWT listeners later to avoid deadlocks. See bug 512001
		browser.getDisplay().asyncExec(fireCloseWindowListeners);
	} else {
		fireCloseWindowListeners.run();
	}
	return 0;
}

int /*long*/ webkit_console_message (int /*long*/ web_view, int /*long*/ message, int /*long*/ line, int /*long*/ source_id) {
	return 1;	/* stop the message from being written to stderr */
}

int /*long*/ webkit_create_web_view (int /*long*/ web_view, int /*long*/ frame) {
	WindowEvent newEvent = new WindowEvent (browser);
	newEvent.display = browser.getDisplay ();
	newEvent.widget = browser;
	newEvent.required = true;
	Runnable fireOpenWindowListeners = () -> {
		if (openWindowListeners != null) {
			for (int i = 0; i < openWindowListeners.length; i++) {
				openWindowListeners[i].open (newEvent);
			}
		}
	};
	if (WEBKIT2) {
		try {
			nonBlockingEvaluate++; 	  // running evaluate() inside openWindowListener and waiting for return leads to deadlock. Bug 512001
			fireOpenWindowListeners.run();// Permit evaluate()/execute() to execute scripts in listener, but do not provide return value.
		} catch (Exception e) {
			throw e; // rethrow execption if thrown, but decrement counter first.
		} finally {
			nonBlockingEvaluate--;
		}
	} else {
		fireOpenWindowListeners.run();
	}
	Browser browser = null;
	if (newEvent.browser != null && newEvent.browser.webBrowser instanceof WebKit) {
		browser = newEvent.browser;
	}
	if (browser != null && !browser.isDisposed ()) {
		return ((WebKit)browser.webBrowser).webView;
	}
	return 0;
}

int /*long*/ webkit_download_requested (int /*long*/ web_view, int /*long*/ download) {
	assert WEBKIT1 : WebKitGTK.Webkit1AssertMsg;
	int /*long*/ name = WebKitGTK.webkit_download_get_suggested_filename (download);
	int length = C.strlen (name);
	byte[] bytes = new byte[length];
	C.memmove (bytes, name, length);
	final String nameString = new String (Converter.mbcsToWcs (bytes));
	final int /*long*/ request = WebKitGTK.webkit_download_get_network_request (download);
	OS.g_object_ref (request);
	/*
	 * As of WebKitGTK 1.8.x attempting to show a FileDialog in this callback causes
	 * a hang.  The workaround is to open it asynchronously with a new download.
	 */
	browser.getDisplay ().asyncExec (() -> {
		if (!browser.isDisposed ()) {
			FileDialog dialog = new FileDialog (browser.getShell (), SWT.SAVE);
			dialog.setFileName (nameString);
			String title = Compatibility.getMessage ("SWT_FileDownload"); //$NON-NLS-1$
			dialog.setText (title);
			String path = dialog.open ();
			if (path != null) {
				path = URI_FILEROOT + path;
				int /*long*/ newDownload = WebKitGTK.webkit_download_new (request);
				byte[] uriBytes = Converter.wcsToMbcs (path, true);
				WebKitGTK.webkit_download_set_destination_uri (newDownload, uriBytes);
				openDownloadWindow (newDownload);
				WebKitGTK.webkit_download_start (newDownload);
				OS.g_object_unref (newDownload);
			}
		}
		OS.g_object_unref (request);
	});
	return 1;
}

static int /*long*/ webkit_download_started(int /*long*/ webKitDownload) {
	assert WEBKIT2 : WebKitGTK.Webkit2AssertMsg;
	OS._g_signal_connect(webKitDownload, WebKitGTK.decide_destination, Proc3.getAddress(), DECIDE_DESTINATION);
	OS._g_signal_connect(webKitDownload, WebKitGTK.failed, Proc3.getAddress(), FAILED);
	OS._g_signal_connect(webKitDownload, WebKitGTK.finished, Proc2.getAddress(), FINISHED);
	return 1;
}


static int /*long*/ webkit_download_decide_destination(int /*long*/ webKitDownload, int /*long*/ suggested_filename) {
	assert WEBKIT2 : WebKitGTK.Webkit2AssertMsg;
	final String fileName = getString(suggested_filename);
	int /*long*/ webView = WebKitGTK.webkit_download_get_web_view(webKitDownload);
	if (webView != 0) {
		Browser browser = FindBrowser (webView);
		if (browser == null || browser.isDisposed() || browser.isClosing) return 0;

		FileDialog dialog = new FileDialog (browser.getShell (), SWT.SAVE);
		dialog.setFileName (fileName);
		String title = Compatibility.getMessage ("SWT_FileDownload"); //$NON-NLS-1$
		dialog.setText (title);
		String path = dialog.open ();
		if (path != null) {
			path = URI_FILEROOT + path;
			byte[] uriBytes = Converter.wcsToMbcs (path, true);

			if (WebKitGTK.webkit_get_minor_version() >= 6) {
				WebKitGTK.webkit_download_set_allow_overwrite (webKitDownload, true);
			}
			WebKitGTK.webkit_download_set_destination (webKitDownload, uriBytes);
			((WebKit)browser.webBrowser).openDownloadWindow(webKitDownload, fileName);
		}
	}
	return 0;
}

static int /*long*/ webkit_download_finished(int /*long*/ download) {
	assert WEBKIT2 : WebKitGTK.Webkit2AssertMsg;
	// A failed signal may have been recorded prior. The finish signal is now being called.
	if (!webKitDownloadStatus.containsKey(new LONG(download))) {
		webKitDownloadStatus.put(new LONG(download), WebKitGTK.WEBKIT_DOWNLOAD_STATUS_FINISHED);
	}
	return 0;
}

static int /*long*/ webkit_download_failed(int /*long*/ download) {
	assert WEBKIT2 : WebKitGTK.Webkit2AssertMsg;
	// A cancel may have been issued resulting in this signal call. Preserve the original cause.
	if (!webKitDownloadStatus.containsKey(new LONG(download))) {
		webKitDownloadStatus.put(new LONG(download), WebKitGTK.WEBKIT_DOWNLOAD_STATUS_ERROR);
	}
	return 0;
}

/**
 *  Webkit2 only. WebkitWebView mouse-target-changed
 * - void user_function (WebKitWebView *web_view, WebKitHitTestResult *hit_test_result, guint modifiers, gpointer user_data)
 * - https://webkitgtk.org/reference/webkit2gtk/stable/WebKitWebView.html#WebKitWebView-mouse-target-changed
 * */
int /*long*/ webkit_mouse_target_changed (int /*long*/ web_view, int /*long*/ hit_test_result, int /*long*/ modifiers) {
	assert WEBKIT2 : WebKitGTK.Webkit2AssertMsg;
	if (WebKitGTK.webkit_hit_test_result_context_is_link(hit_test_result)){
		int /*long*/ uri = WebKitGTK.webkit_hit_test_result_get_link_uri(hit_test_result);
		int /*long*/ title = WebKitGTK.webkit_hit_test_result_get_link_title(hit_test_result);
		return webkit_hovering_over_link(web_view, title, uri);
	}

	return 0;
}

/**
 * Webkit1: WebkitWebView hovering-over-link signal
 * - void user_function (WebKitWebView *web_view, gchar *title, gchar *uri, gpointer user_data)
 * - https://webkitgtk.org/reference/webkitgtk/unstable/webkitgtk-webkitwebview.html#WebKitWebView-hovering-over-link
 *
 * Webkit2: WebkitWebView mouse-target-change
 * - Normally this signal is called for many different events, e.g hoveing over an image.
 *   But in our case, in webkit_mouse_target_changed() we filter out everything except mouse_over_link events.
 *
 *   Since there is no return value, it is safe to run asynchronously.
 */
int /*long*/ webkit_hovering_over_link (int /*long*/ web_view, int /*long*/ title, int /*long*/ uri) {
	if (uri != 0) {
		int length = C.strlen (uri);
		byte[] bytes = new byte[length];
		C.memmove (bytes, uri, length);
		String text = new String (Converter.mbcsToWcs (bytes));
		StatusTextEvent event = new StatusTextEvent (browser);
		event.display = browser.getDisplay ();
		event.widget = browser;
		event.text = text;
		Runnable fireStatusTextListener = () -> {
			if (browser.isDisposed() || statusTextListeners == null) return;
			for (int i = 0; i < statusTextListeners.length; i++) {
				statusTextListeners[i].changed (event);
			}
		};
		if (WEBKIT2)
			browser.getDisplay().asyncExec(fireStatusTextListener);
		else
			fireStatusTextListener.run();
	}
	return 0;
}

int /*long*/ webkit_mime_type_policy_decision_requested (int /*long*/ web_view, int /*long*/ frame, int /*long*/ request, int /*long*/ mimetype, int /*long*/ policy_decision) {
	assert WEBKIT1 : WebKitGTK.Webkit1AssertMsg;
	boolean canShow = WebKitGTK.webkit_web_view_can_show_mime_type (webView, mimetype) != 0;
	if (!canShow) {
		WebKitGTK.webkit_web_policy_decision_download (policy_decision);
		return 1;
	}
	return 0;
}

/** Webkit1 only */
int /*long*/ webkit_navigation_policy_decision_requested (int /*long*/ web_view, int /*long*/ frame, int /*long*/ request, int /*long*/ navigation_action, int /*long*/ policy_decision) {
	assert WEBKIT1 : WebKitGTK.Webkit1AssertMsg;
	if (loadingText) {
		/*
		 * WebKit is auto-navigating to about:blank in response to a
		 * webkit_web_view_load_string() invocation.  This navigate
		 * should always proceed without sending an event since it is
		 * preceded by an explicit navigate to about:blank in setText().
		 */
		return 0;
	}

	int /*long*/ uri = WebKitGTK.webkit_network_request_get_uri (request);
	int length = C.strlen (uri);
	byte[] bytes = new byte[length];
	C.memmove (bytes, uri, length);

	String url = new String (Converter.mbcsToWcs (bytes));
	/*
	 * If the URI indicates that the page is being rendered from memory
	 * (via setText()) then set it to about:blank to be consistent with IE.
	 */
	if (url.equals (URI_FILEROOT)) {
		url = ABOUT_BLANK;
	} else {
		length = URI_FILEROOT.length ();
		if (url.startsWith (URI_FILEROOT) && url.charAt (length) == '#') {
			url = ABOUT_BLANK + url.substring (length);
		}
	}

	LocationEvent newEvent = new LocationEvent (browser);
	newEvent.display = browser.getDisplay ();
	newEvent.widget = browser;
	newEvent.location = url;
	newEvent.doit = true;
	if (locationListeners != null) {
		for (int i = 0; i < locationListeners.length; i++) {
			locationListeners[i].changing (newEvent);
		}
	}
	if (newEvent.doit && !browser.isDisposed ()) {
		if (jsEnabled != jsEnabledOnNextPage) {
			jsEnabled = jsEnabledOnNextPage;
			DisabledJSCount += !jsEnabled ? 1 : -1;
			webkit_settings_set(WebKitGTK.enable_scripts, jsEnabled ? 1 : 0);
		}

		/* hook status change signal if frame is a newly-created sub-frame */
		int /*long*/ mainFrame = WebKitGTK.webkit_web_view_get_main_frame (webView);
		if (frame != mainFrame) {
			int id = OS.g_signal_handler_find (frame, OS.G_SIGNAL_MATCH_FUNC | OS.G_SIGNAL_MATCH_DATA, 0, 0, 0, Proc3.getAddress (), NOTIFY_LOAD_STATUS);
			if (id == 0) {
				OS.g_signal_connect (frame, WebKitGTK.notify_load_status, Proc3.getAddress (), NOTIFY_LOAD_STATUS);
			}
		}

		/*
		* The following line is intentionally commented.  For some reason, invoking
		* webkit_web_policy_decision_use(policy_decision) causes the Flash plug-in
		* to crash when navigating to a page with Flash.  Since returning from this
		* callback without invoking webkit_web_policy_decision_ignore(policy_decision)
		* implies that the page should be loaded, it's fine to not invoke
		* webkit_web_policy_decision_use(policy_decision) here.
		*/
		//WebKitGTK.webkit_web_policy_decision_use (policy_decision);
	} else {
		WebKitGTK.webkit_web_policy_decision_ignore (policy_decision);
	}
	return 0;
}

/** Webkit2 only */
int /*long*/ webkit_decide_policy (int /*long*/ web_view, int /*long*/ decision, int decision_type, int /*long*/ user_data) {
	assert WEBKIT2 : WebKitGTK.Webkit2AssertMsg;
	switch (decision_type) {
    case WebKitGTK.WEBKIT_POLICY_DECISION_TYPE_NAVIGATION_ACTION:
       int /*long*/ request = WebKitGTK. webkit_navigation_policy_decision_get_request(decision);
       if (request == 0){
          return 0;
       }
       int /*long*/ uri = WebKitGTK.webkit_uri_request_get_uri (request);
       String url = getString(uri);
       /*
        * If the URI indicates that the page is being rendered from memory
        * (via setText()) then set it to about:blank to be consistent with IE.
        */
       if (url.equals (URI_FILEROOT)) {
          url = ABOUT_BLANK;
       } else {
          int length = URI_FILEROOT.length ();
          if (url.startsWith (URI_FILEROOT) && url.charAt (length) == '#') {
             url = ABOUT_BLANK + url.substring (length);
          }
       }

       LocationEvent newEvent = new LocationEvent (browser);
       newEvent.display = browser.getDisplay ();
       newEvent.widget = browser;
       newEvent.location = url;
       newEvent.doit = true;

       try {
	       nonBlockingEvaluate++;
	       if (locationListeners != null) {
	          for (int i = 0; i < locationListeners.length; i++) {
	             locationListeners[i].changing (newEvent);
	          }
	       }
       } catch (Exception e) {
    	   throw e;
       } finally {
    	  nonBlockingEvaluate--;
       }

       if (newEvent.doit && !browser.isDisposed ()) {
          if (jsEnabled != jsEnabledOnNextPage) {
             jsEnabled = jsEnabledOnNextPage;
             webkit_settings_set(WebKitGTK.enable_javascript, jsEnabled ? 1 : 0);
          }
       }
       if(!newEvent.doit){
         WebKitGTK.webkit_policy_decision_ignore (decision);
       }
       break;
    case WebKitGTK.WEBKIT_POLICY_DECISION_TYPE_NEW_WINDOW_ACTION:
        break;
    case WebKitGTK.WEBKIT_POLICY_DECISION_TYPE_RESPONSE:
       int /*long*/ response = WebKitGTK.webkit_response_policy_decision_get_response(decision);
       int /*long*/ mime_type = WebKitGTK.webkit_uri_response_get_mime_type(response);
       boolean canShow = WebKitGTK.webkit_web_view_can_show_mime_type (webView, mime_type) != 0;
       if (!canShow) {
         WebKitGTK.webkit_policy_decision_download (decision);
         return 1;
       }
       break;
    default:
        /* Making no decision results in webkit_policy_decision_use(). */
        return 0;
    }
    return 0;
}

int /*long*/ webkit_notify_load_status (int /*long*/ web_view, int /*long*/ pspec) {
	assert WEBKIT1 : WebKitGTK.Webkit1AssertMsg;
	int status = WebKitGTK.webkit_web_view_get_load_status (webView);
	switch (status) {
		case WebKitGTK.WEBKIT_LOAD_COMMITTED: {
			int /*long*/ uri = WebKitGTK.webkit_web_view_get_uri (webView);
			return handleLoadCommitted (uri, true);
		}
		case WebKitGTK.WEBKIT_LOAD_FINISHED: {
			int /*long*/ uri = WebKitGTK.webkit_web_view_get_uri (webView);
			return handleLoadFinished (uri, true);
		}
	}
	return 0;
}

/**
 * This method is only called by Webkit2.
 * The webkit1 equivalent is webkit_window_object_cleared;
 */
int /*long*/ webkit_load_changed (int /*long*/ web_view, int status, long user_data) {
	assert WEBKIT2 : WebKitGTK.Webkit2AssertMsg;
	switch (status) {
		case WebKitGTK.WEBKIT2_LOAD_COMMITTED: {
			int /*long*/ uri = WebKitGTK.webkit_web_view_get_uri (webView);
			return handleLoadCommitted (uri, true);
		}
		case WebKitGTK.WEBKIT2_LOAD_FINISHED: {
			addEventHandlers (web_view, true);

			int /*long*/ title = WebKitGTK.webkit_web_view_get_title (webView);
			if (title == 0) {
				int /*long*/ uri = WebKitGTK.webkit_web_view_get_uri (webView);
				fireNewTitleEvent(getString(uri));
			}
			fireProgressCompletedEvent();

			/*
			 * If there is a pending TLS error, handle it by prompting the user for input.
			 * This is done by popping up a message box and asking if the user would like
			 * ignore warnings for this host. Clicking yes will do so, clicking no will
			 * load the previous page.
			 *
			 *  Not applicable if the ignoreTls flag has been set. See bug 531341.
			 */
			if (tlsError && !ignoreTls) {
				tlsError = false;
				String javaHost = tlsErrorUri.getHost();
				MessageBox prompt = new MessageBox (browser.getShell(), SWT.YES | SWT.NO);
				prompt.setText(SWT.getMessage("SWT_InvalidCert_Title"));
				String specific = tlsErrorType.isEmpty() ? "\n\n" : "\n\n" + tlsErrorType + "\n\n";
				String message = SWT.getMessage("SWT_InvalidCert_Message", new Object[] {javaHost}) +
						specific + SWT.getMessage("SWT_InvalidCert_Connect");
				prompt.setMessage(message);
				int result = prompt.open();
				if (result == SWT.YES) {
					int /*long*/ webkitcontext = WebKitGTK.webkit_web_view_get_context(web_view);
					if (javaHost != null) {
						byte [] host = Converter.javaStringToCString(javaHost);
						WebKitGTK.webkit_web_context_allow_tls_certificate_for_host(webkitcontext, tlsErrorCertificate, host);
						WebKitGTK.webkit_web_view_reload (web_view);
					} else {
						System.err.println("***ERROR: Unable to parse host from URI!");
					}
				} else {
					back();
				}
				// De-reference Webkit certificate so it can be freed
				if (tlsErrorCertificate != 0) {
					OS.g_object_unref (tlsErrorCertificate);
					tlsErrorCertificate = 0;
				}
			}

			return 0;
		}
	}
	return 0;
}

/**
 * This method is only called by Webkit2.
 *
 * Called in cases where a web page failed to load due to TLS errors
 * (self-signed certificates, as an example).
 */
int /*long*/ webkit_load_failed_tls (int /*long*/ web_view, int /*long*/ failing_uri, int /*long*/ certificate, int /*long*/ error) {
	assert WEBKIT2 : WebKitGTK.Webkit2AssertMsg;
	if (!ignoreTls) {
		// Set tlsError flag so that the user can be prompted once this "bad" page has finished loading
		tlsError = true;
		OS.g_object_ref(certificate);
		tlsErrorCertificate = certificate;
		convertUri (failing_uri);
		switch ((int)/*64*/error) {
			case WebKitGTK.G_TLS_CERTIFICATE_UNKNOWN_CA: {
				tlsErrorType = SWT.getMessage("SWT_InvalidCert_UnknownCA");
				break;
			}
			case WebKitGTK.G_TLS_CERTIFICATE_BAD_IDENTITY: {
				tlsErrorType = SWT.getMessage("SWT_InvalidCert_BadIdentity");
				break;
			}
			case WebKitGTK.G_TLS_CERTIFICATE_NOT_ACTIVATED: {
				tlsErrorType = SWT.getMessage("SWT_InvalidCert_NotActivated");
				break;
			}
			case WebKitGTK.G_TLS_CERTIFICATE_EXPIRED: {
				tlsErrorType = SWT.getMessage("SWT_InvalidCert_Expired");
				break;
			}
			case WebKitGTK.G_TLS_CERTIFICATE_REVOKED: {
				tlsErrorType = SWT.getMessage("SWT_InvalidCert_Revoked");
				break;
			}
			case WebKitGTK.G_TLS_CERTIFICATE_INSECURE: {
				tlsErrorType = SWT.getMessage("SWT_InvalidCert_Insecure");
				break;
			}
			case WebKitGTK.G_TLS_CERTIFICATE_GENERIC_ERROR: {
				tlsErrorType = SWT.getMessage("SWT_InvalidCert_GenericError");
				break;
			}
			case WebKitGTK.G_TLS_CERTIFICATE_VALIDATE_ALL: {
				tlsErrorType = SWT.getMessage("SWT_InvalidCert_ValidateAll");
				break;
			}
			default: {
				tlsErrorType = SWT.getMessage("SWT_InvalidCert_GenericError");
				break;
			}
		}
	}
	return 0;
}

/**
 * Converts a WebKit URI into a Java URI object.
 *
 * @param webkitUri a long pointing to the URI in C string form (gchar *)
 * @throws URISyntaxException if the string violates RFC 2396, or is otherwise
 * malformed
 */
void convertUri (int /*long*/ webkitUri) {
	try {
		tlsErrorUriString = Converter.cCharPtrToJavaString(webkitUri, false);
		tlsErrorUri = new URI (tlsErrorUriString);
	} catch (URISyntaxException e) {
		System.err.println("***ERROR: Malformed URI from WebKit!");
		return;
	}
}

/**
 * Triggered by a change in property. (both gdouble[0,1])
 * Webkit1: WebkitWebview notify::progress
 * 	https://webkitgtk.org/reference/webkitgtk/unstable/webkitgtk-webkitwebview.html#WebKitWebView--progress
 * Webkit2: WebkitWebview notify::estimated-load-progress
 *  https://webkitgtk.org/reference/webkit2gtk/stable/WebKitWebView.html#WebKitWebView--estimated-load-progress
 *
 *  No return value required. Thus safe to run asynchronously.
 */
int /*long*/ webkit_notify_progress (int /*long*/ web_view, int /*long*/ pspec) {
	assert WEBKIT1 || WEBKIT2;
	ProgressEvent event = new ProgressEvent (browser);
	event.display = browser.getDisplay ();
	event.widget = browser;
	double progress = 0;
	if (WEBKIT2){
		progress = WebKitGTK.webkit_web_view_get_estimated_load_progress (webView);
	} else {
		progress = WebKitGTK.webkit_web_view_get_progress (webView);
	}
	event.current = (int) (progress * MAX_PROGRESS);
	event.total = MAX_PROGRESS;
	Runnable fireProgressChangedEvents = () -> {
		if (browser.isDisposed() || progressListeners == null) return;
		for (int i = 0; i < progressListeners.length; i++) {
			progressListeners[i].changed (event);
		}
	};
	if (WEBKIT2)
		browser.getDisplay().asyncExec(fireProgressChangedEvents);
	else
		fireProgressChangedEvents.run();
	return 0;
}

/**
 * Triggerd by webkit's 'notify::title' signal and forwarded to this function.
 * The signal doesn't have documentation (2.15.4), but is mentioned here:
 * https://webkitgtk.org/reference/webkit2gtk/stable/WebKitWebView.html#webkit-web-view-get-title
 *
 * It doesn't look it would require a return value, so running in asyncExec should be fine.
 */
int /*long*/ webkit_notify_title (int /*long*/ web_view, int /*long*/ pspec) {
	assert WEBKIT1 || WEBKIT2;
	int /*long*/ title = WebKitGTK.webkit_web_view_get_title (webView);
	String titleString;
	if (title == 0) {
		titleString = ""; //$NON-NLS-1$
	} else {
		int length = C.strlen (title);
		byte[] bytes = new byte[length];
		C.memmove (bytes, title, length);
		titleString = new String (Converter.mbcsToWcs (bytes));
	}
	TitleEvent event = new TitleEvent (browser);
	event.display = browser.getDisplay ();
	event.widget = browser;
	event.title = titleString;
	Runnable fireTitleListener = () -> {
		for (int i = 0; i < titleListeners.length; i++) {
			titleListeners[i].changed (event);
		}
	};
	if (WEBKIT2)
		browser.getDisplay().asyncExec(fireTitleListener);
	else
		fireTitleListener.run();
	return 0;
}

int /*long*/ webkit_context_menu (int /*long*/ web_view, int /*long*/ context_menu, int /*long*/ eventXXX, int /*long*/ hit_test_result) {
	Point pt = browser.getDisplay ().getCursorLocation (); // might break on Wayland? Wouldn't hurt to verify.
	Event event = new Event ();
	event.x = pt.x;
	event.y = pt.y;
	browser.notifyListeners (SWT.MenuDetect, event);
	if (!event.doit) {
		// Do not display the menu
		return 1;
	}

	Menu menu = browser.getMenu ();
	if (menu != null && !menu.isDisposed ()) {
		if (pt.x != event.x || pt.y != event.y) {
			menu.setLocation (event.x, event.y);
		}
		menu.setVisible (true);
		// Do not display the webkit menu
		return 1;
	}
	return 0;
}

// Seems to be reached only by Webkit1 at the moment.
int /*long*/ webkit_populate_popup (int /*long*/ web_view, int /*long*/ webkit_menu) {
	Point pt = browser.getDisplay ().getCursorLocation ();
	Event event = new Event ();
	event.x = pt.x;
	event.y = pt.y;
	browser.notifyListeners (SWT.MenuDetect, event);
	if (!event.doit) {
		/* clear the menu */
		int /*long*/ children = GTK.gtk_container_get_children (webkit_menu);
		int /*long*/ current = children;
		while (current != 0) {
			int /*long*/ item = OS.g_list_data (current);
			GTK.gtk_container_remove (webkit_menu, item);
			current = OS.g_list_next (current);
		}
		OS.g_list_free (children);
		return 0;
	}
	Menu menu = browser.getMenu ();
	if (menu != null && !menu.isDisposed ()) {
		if (pt.x != event.x || pt.y != event.y) {
			menu.setLocation (event.x, event.y);
		}
		menu.setVisible (true);
		/* clear the menu */
		int /*long*/ children = GTK.gtk_container_get_children (webkit_menu);
		int /*long*/ current = children;
		while (current != 0) {
			int /*long*/ item = OS.g_list_data (current);
			GTK.gtk_container_remove (webkit_menu, item);
			current = OS.g_list_next (current);
		}
		OS.g_list_free (children);
	}
	return 0;
}

private void addRequestHeaders(int /*long*/ requestHeaders, String[] headers){
	for (int i = 0; i < headers.length; i++) {
		String current = headers[i];
		if (current != null) {
			int index = current.indexOf (':');
			if (index != -1) {
				String key = current.substring (0, index).trim ();
				String value = current.substring (index + 1).trim ();
				if (key.length () > 0 && value.length () > 0) {
					byte[] nameBytes = Converter.wcsToMbcs (key, true);
					byte[] valueBytes = Converter.wcsToMbcs (value, true);
					WebKitGTK.soup_message_headers_append (requestHeaders, nameBytes, valueBytes);
				}
			}
		}
	}

}

int /*long*/ webkit_resource_request_starting (int /*long*/ web_view, int /*long*/ web_frame, int /*long*/ web_resource, int /*long*/ request, int /*long*/ response) {
	assert WEBKIT1;
	if (postData != null || headers != null) {
		int /*long*/ message = WebKitGTK.webkit_network_request_get_message (request);
		if (message == 0) {
			headers = null;
			postData = null;
		} else {
			if (postData != null) {
				// Set the message method type to POST
				WebKitGTK.SoupMessage_method (message, PostString);
				int /*long*/ body = WebKitGTK.SoupMessage_request_body (message);
				byte[] bytes = Converter.wcsToMbcs (postData, false);
				int /*long*/ data = C.malloc (bytes.length);
				C.memmove (data, bytes, bytes.length);
				WebKitGTK.soup_message_body_append (body, WebKitGTK.SOUP_MEMORY_TAKE, data, bytes.length);
				WebKitGTK.soup_message_body_flatten (body);

				if (headers == null) headers = new String[0];
				boolean found = false;
				for (int i = 0; i < headers.length; i++) {
					int index = headers[i].indexOf (':');
					if (index != -1) {
						String name = headers[i].substring (0, index).trim ().toLowerCase ();
						if (name.equals (HEADER_CONTENTTYPE)) {
							found = true;
							break;
						}
					}
				}
				if (!found) {
					String[] temp = new String[headers.length + 1];
					System.arraycopy (headers, 0, temp, 0, headers.length);
					temp[headers.length] = HEADER_CONTENTTYPE + ':' + MIMETYPE_FORMURLENCODED;
					headers = temp;
				}
				postData = null;
			}

			/* headers */
			int /*long*/ requestHeaders = WebKitGTK.SoupMessage_request_headers (message);
			addRequestHeaders(requestHeaders, headers);
			headers = null;
		}
	}

	return 0;
}

/**
 * Webkit1 only.
 * Normally triggered by javascript that runs "window.status=txt".
 *
 * On webkit2 this signal doesn't exist anymore.
 * In general, window.status=text is not supported on most newer browsers anymore.
 * status bar now only changes when you hover you mouse over it.
 */
int /*long*/ webkit_status_bar_text_changed (int /*long*/ web_view, int /*long*/ text) {
	int length = C.strlen (text);
	byte[] bytes = new byte[length];
	C.memmove (bytes, text, length);
	StatusTextEvent statusText = new StatusTextEvent (browser);
	statusText.display = browser.getDisplay ();
	statusText.widget = browser;
	statusText.text = new String (Converter.mbcsToWcs (bytes));
	for (int i = 0; i < statusTextListeners.length; i++) {
		statusTextListeners[i].changed (statusText);
	}
	return 0;
}

/**
 * Emitted after "create" on the newly created WebKitWebView when it should be displayed to the user.
 *
 * Webkit1 signal: web-view-ready
 *   https://webkitgtk.org/reference/webkitgtk/unstable/webkitgtk-webkitwebview.html#WebKitWebView-web-view-ready
 * Webkit2 signal: ready-to-show
 *   https://webkitgtk.org/reference/webkitgtk/unstable/webkitgtk-webkitwebview.html#WebKitWebView-web-view-ready
 * Note in webkit2, no return value has to be provided in callback.
 */
int /*long*/ webkit_web_view_ready (int /*long*/ web_view) {
	WindowEvent newEvent = new WindowEvent (browser);
	newEvent.display = browser.getDisplay ();
	newEvent.widget = browser;

	if (WEBKIT1) {
		int /*long*/ webKitWebWindowFeatures = WebKitGTK.webkit_web_view_get_window_features (webView);
		newEvent.addressBar = webkit_settings_get(webKitWebWindowFeatures, WebKitGTK.locationbar_visible) != 0;
		newEvent.menuBar = webkit_settings_get(webKitWebWindowFeatures, WebKitGTK.menubar_visible) != 0;
		newEvent.statusBar = webkit_settings_get(webKitWebWindowFeatures, WebKitGTK.statusbar_visible) != 0;
		newEvent.addressBar = webkit_settings_get(webKitWebWindowFeatures, WebKitGTK.toolbar_visible) != 0;
		int x =  webkit_settings_get(webKitWebWindowFeatures, WebKitGTK.x);
		int y =  webkit_settings_get(webKitWebWindowFeatures, WebKitGTK.y);
		int width =  webkit_settings_get(webKitWebWindowFeatures, WebKitGTK.width);
		int height =  webkit_settings_get(webKitWebWindowFeatures, WebKitGTK.height);
		if (x != -1 && y != -1)
			newEvent.location = new Point (x,y);
		if (width != -1 && height != -1)
			newEvent.size = new Point (width,height);
	} else {
		assert WEBKIT2 : WebKitGTK.Webkit2AssertMsg;
		int /*long*/ properties = WebKitGTK.webkit_web_view_get_window_properties(webView);
		newEvent.addressBar = webkit_settings_get(properties, WebKitGTK.locationbar_visible) != 0;
		newEvent.menuBar = webkit_settings_get(properties, WebKitGTK.menubar_visible) != 0;
		newEvent.statusBar = webkit_settings_get(properties, WebKitGTK.statusbar_visible) != 0;
		newEvent.toolBar = webkit_settings_get(properties, WebKitGTK.toolbar_visible) != 0;

		GdkRectangle rect = new GdkRectangle();
		WebKitGTK.webkit_window_properties_get_geometry(properties, rect);
		newEvent.location = new Point(Math.max(0, rect.x),Math.max(0, rect.y));

		int width = rect.width;
		int height = rect.height;
		if (height == 100 && width == 100) {
			// On Webkit1, if no height/width is specified, reasonable defaults are given.
			// On Webkit2, if no height/width is specified, then minimum (which is 100) is allocated to popus.
			// This makes popups very small.
			// For better cross-platform consistency (Win/Cocoa/Gtk), we give more reasonable defaults (2/3 the size of a screen).
			Rectangle primaryMonitorBounds = browser.getDisplay ().getPrimaryMonitor().getBounds();
			height = (int) (primaryMonitorBounds.height * 0.66);
			width = (int) (primaryMonitorBounds.width * 0.66);
		}
		newEvent.size = new Point(width, height);
	}

	Runnable fireVisibilityListeners = () -> {
		if (browser.isDisposed()) return;
		for (int i = 0; i < visibilityWindowListeners.length; i++) {
			visibilityWindowListeners[i].show (newEvent);
		}
	};
	if (WEBKIT2) {
		// Postpone execution of listener, to avoid deadlocks in case evaluate() is
		// called in the listener while another signal is being handled. See bug 512001.
		// evaluate() can safely be called in this listener with no adverse effects.
		browser.getDisplay().asyncExec(fireVisibilityListeners);
	} else {
		fireVisibilityListeners.run();
	}
	return 0;
}

/**
 * This method is only called by Webkit1.
 * The webkit2 equivalent is webkit_load_changed(..):caseWEBKIT2__LOAD_FINISHED
 */
int /*long*/ webkit_window_object_cleared (int /*long*/ web_view, int /*long*/ frame, int /*long*/ context, int /*long*/ window_object) {
	assert WEBKIT1 : WebKitGTK.Webkit1AssertMsg;
	int /*long*/ globalObject = WebKitGTK.JSContextGetGlobalObject (context);
	int /*long*/ externalObject = WebKitGTK.JSObjectMake (context, ExternalClass, webViewData);
	byte[] bytes = (OBJECTNAME_EXTERNAL + '\0').getBytes (StandardCharsets.UTF_8);
	int /*long*/ name = WebKitGTK.JSStringCreateWithUTF8CString (bytes);
	WebKitGTK.JSObjectSetProperty (context, globalObject, name, externalObject, 0, null);
	WebKitGTK.JSStringRelease (name);

	registerBrowserFunctions(); // Bug 508217
	int /*long*/ mainFrame = WebKitGTK.webkit_web_view_get_main_frame (webView);
	boolean top = mainFrame == frame;
	addEventHandlers (web_view, top);
	return 0;
}

/** Webkit1 & Webkit2
 * @return An integer value for the property is returned. For boolean settings, 0 indicates false, 1 indicates true. -1= is error.*/
private int webkit_settings_get(byte [] property) {
	if (WEBKIT2 && webView == 0) { // already disposed.
		return -1; // error.
	}
	int /*long*/ settings = WebKitGTK.webkit_web_view_get_settings (webView);
	return webkit_settings_get(settings, property);
}

/** Webkit1 & Webkit2
 * @return An integer value for the property is returned. For boolean settings, 0 indicates false, 1 indicates true */
private int webkit_settings_get(int /*long*/ settings, byte[] property) {
	int[] result = new int[1];
	OS.g_object_get (settings, property, result, 0);
	return result[0];
}

/** Webkit1 & Webkit2 */
private void webkit_settings_set(byte [] property, int value) {
	if (WEBKIT2 && webView == 0) { // already disposed.
		return;
	}
	int /*long*/ settings = WebKitGTK.webkit_web_view_get_settings (webView);
	OS.g_object_set(settings, property, value, 0);
}

private void registerBrowserFunctions() {
	for (BrowserFunction current : functions.values()) {
		nonBlockingExecute(current.functionString);
	}
}

/**
 * Webkit1 callback for javascript to call java.
 */
int /*long*/ callJava (int /*long*/ ctx, int /*long*/ func, int /*long*/ thisObject, int /*long*/ argumentCount, int /*long*/ arguments, int /*long*/ exception) {
	Object returnValue = null;
	if (argumentCount == 3) {
		// Javastring array: <int: function index>, <string: token>, <array: javascript args>
		// 1st arg: Function index
		int /*long*/[] result = new int /*long*/[1];
		C.memmove (result, arguments, C.PTR_SIZEOF);
		int type = WebKitGTK.JSValueGetType (ctx, result[0]);
		if (type == WebKitGTK.kJSTypeNumber) {
			int index = ((Double)convertToJava (ctx, result[0])).intValue ();
			result[0] = 0;
			Integer key = new Integer (index);
			// 2nd arg: function token
			C.memmove (result, arguments + C.PTR_SIZEOF, C.PTR_SIZEOF);
			type = WebKitGTK.JSValueGetType (ctx, result[0]);
			if (type == WebKitGTK.kJSTypeString) {
				String token = (String)convertToJava (ctx, result[0]);
				BrowserFunction function = functions.get (key);
				if (function != null && token.equals (function.token)) {
					try {
						// 3rd Arg: paramaters given from Javascript
						C.memmove (result, arguments + 2 * C.PTR_SIZEOF, C.PTR_SIZEOF);
						Object temp = convertToJava (ctx, result[0]);
						if (temp instanceof Object[]) {
							Object[] args = (Object[])temp;
							try {
								returnValue = function.function (args);
							} catch (Exception e) {
								/* exception during function invocation */
								returnValue = WebBrowser.CreateErrorString (e.getLocalizedMessage ());
							}
						}
					} catch (IllegalArgumentException e) {
						/* invalid argument value type */
						if (function.isEvaluate) {
							/* notify the function so that a java exception can be thrown */
							function.function (new String[] {WebBrowser.CreateErrorString (new SWTException (SWT.ERROR_INVALID_RETURN_VALUE).getLocalizedMessage ())});
						}
						returnValue = WebBrowser.CreateErrorString (e.getLocalizedMessage ());
					}
				}
			}
		}
	}
	return convertToJS (ctx, returnValue);
}

int /*long*/ convertToJS (int /*long*/ ctx, Object value) {
	if (value == null) {
		return WebKitGTK.JSValueMakeUndefined (ctx);
	}
	if (value instanceof String) {
		byte[] bytes = ((String)value + '\0').getBytes (StandardCharsets.UTF_8); //$NON-NLS-1$
		int /*long*/ stringRef = WebKitGTK.JSStringCreateWithUTF8CString (bytes);
		int /*long*/ result = WebKitGTK.JSValueMakeString (ctx, stringRef);
		WebKitGTK.JSStringRelease (stringRef);
		return result;
	}
	if (value instanceof Boolean) {
		return WebKitGTK.JSValueMakeBoolean (ctx, ((Boolean)value).booleanValue () ? 1 : 0);
	}
	if (value instanceof Number) {
		return WebKitGTK.JSValueMakeNumber (ctx, ((Number)value).doubleValue ());
	}
	if (value instanceof Object[]) {
		Object[] arrayValue = (Object[]) value;
		int length = arrayValue.length;
		int /*long*/[] arguments = new int /*long*/[length];
		for (int i = 0; i < length; i++) {
			Object javaObject = arrayValue[i];
			int /*long*/ jsObject = convertToJS (ctx, javaObject);
			arguments[i] = jsObject;
		}
		return WebKitGTK.JSObjectMakeArray (ctx, length, arguments, null);
	}
	SWT.error (SWT.ERROR_INVALID_RETURN_VALUE);
	return 0;
}

static Object convertToJava (int /*long*/ ctx, int /*long*/ value) {
	int type = WebKitGTK.JSValueGetType (ctx, value);
	switch (type) {
		case WebKitGTK.kJSTypeBoolean: {
			int result = (int)WebKitGTK.JSValueToNumber (ctx, value, null);
			return new Boolean (result != 0);
		}
		case WebKitGTK.kJSTypeNumber: {
			double result = WebKitGTK.JSValueToNumber (ctx, value, null);
			return Double.valueOf(result);
		}
		case WebKitGTK.kJSTypeString: {
			int /*long*/ string = WebKitGTK.JSValueToStringCopy (ctx, value, null);
			if (string == 0) return ""; //$NON-NLS-1$
			int /*long*/ length = WebKitGTK.JSStringGetMaximumUTF8CStringSize (string);
			byte[] bytes = new byte[(int)/*64*/length];
			length = WebKitGTK.JSStringGetUTF8CString (string, bytes, length);
			WebKitGTK.JSStringRelease (string);
			/* length-1 is needed below to exclude the terminator character */
			return new String (bytes, 0, (int)/*64*/length - 1, StandardCharsets.UTF_8);
		}
		case WebKitGTK.kJSTypeNull:
			// FALL THROUGH
		case WebKitGTK.kJSTypeUndefined: return null;
		case WebKitGTK.kJSTypeObject: {
			byte[] bytes = (PROPERTY_LENGTH + '\0').getBytes (StandardCharsets.UTF_8); //$NON-NLS-1$
			int /*long*/ propertyName = WebKitGTK.JSStringCreateWithUTF8CString (bytes);
			int /*long*/ valuePtr = WebKitGTK.JSObjectGetProperty (ctx, value, propertyName, null);
			WebKitGTK.JSStringRelease (propertyName);
			type = WebKitGTK.JSValueGetType (ctx, valuePtr);
			if (type == WebKitGTK.kJSTypeNumber) {
				int length = (int)WebKitGTK.JSValueToNumber (ctx, valuePtr, null);
				Object[] result = new Object[length];
				for (int i = 0; i < length; i++) {
					int /*long*/ current = WebKitGTK.JSObjectGetPropertyAtIndex (ctx, value, i, null);
					if (current != 0) {
						result[i] = convertToJava (ctx, current);
					}
				}
				return result;
			}
		}
	}
	SWT.error (SWT.ERROR_INVALID_ARGUMENT);
	return null;
}

}

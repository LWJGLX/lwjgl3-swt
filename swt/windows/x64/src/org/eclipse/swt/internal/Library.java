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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.swt.internal;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import java.util.function.*;
import java.util.jar.*;

public class Library {

	/* SWT Version - Mmmm (M=major, mmm=minor) */

	/**
	 * SWT Major version number (must be >= 0)
	 */
	static int MAJOR_VERSION = 4;

	/**
	 * SWT Minor version number (must be in the range 0..999)
	 */
	static int MINOR_VERSION = 932;

	/**
	 * SWT revision number (must be >= 0)
	 */
	static int REVISION = 8;

	/**
	 * The JAVA and SWT versions
	 */
	public static final int JAVA_VERSION, SWT_VERSION;
	public static final String USER_HOME;

	static final String SEPARATOR;
	static final String DELIMITER;

	static final String JAVA_LIB_PATH = "java.library.path";
	static final String SWT_LIB_PATH = "swt.library.path";


	/* 64-bit support */
	static final boolean IS_64 = longConst() == (long /*int*/)longConst();
	static final String SUFFIX_64 = "-64";	//$NON-NLS-1$
	static final String SWT_LIB_DIR;

static {
	DELIMITER = System.getProperty("line.separator"); //$NON-NLS-1$
	SEPARATOR = File.separator;
	USER_HOME = System.getProperty ("user.home");
	SWT_LIB_DIR = ".swt" + SEPARATOR + "lib" + SEPARATOR + os() + SEPARATOR + arch(); //$NON-NLS-1$ $NON-NLS-2$
	JAVA_VERSION = parseVersion(System.getProperty("java.version")); //$NON-NLS-1$
	SWT_VERSION = SWT_VERSION(MAJOR_VERSION, MINOR_VERSION);
}

static String arch() {
	String osArch = System.getProperty("os.arch"); //$NON-NLS-1$
	if (osArch.equals ("i386") || osArch.equals ("i686")) return "x86"; //$NON-NLS-1$ $NON-NLS-2$ $NON-NLS-3$
	if (osArch.equals ("amd64")) return "x86_64"; //$NON-NLS-1$ $NON-NLS-2$
	return osArch;
}

static String os() {
	String osName = System.getProperty("os.name"); //$NON-NLS-1$
	if (osName.equals ("Linux")) return "linux"; //$NON-NLS-1$ $NON-NLS-2$
	if (osName.equals ("Mac OS X")) return "macosx"; //$NON-NLS-1$ $NON-NLS-2$
	if (osName.startsWith ("Win")) return "win32"; //$NON-NLS-1$ $NON-NLS-2$
	return osName;
}

static void chmod(String permision, String path) {
	if (os().equals ("win32")) return; //$NON-NLS-1$
	try {
		Runtime.getRuntime ().exec (new String []{"chmod", permision, path}).waitFor(); //$NON-NLS-1$
	} catch (Throwable e) {}
}

/* Use method instead of in-lined constants to avoid compiler warnings */
static long longConst() {
	return 0x1FFFFFFFFL;
}

static int parseVersion(String version) {
	if (version == null) return 0;
	int major = 0, minor = 0, micro = 0;
	int length = version.length(), index = 0, start = 0;
	while (index < length && Character.isDigit(version.charAt(index))) index++;
	try {
		if (start < length) major = Integer.parseInt(version.substring(start, index));
	} catch (NumberFormatException e) {}
	start = ++index;
	while (index < length && Character.isDigit(version.charAt(index))) index++;
	try {
		if (start < length) minor = Integer.parseInt(version.substring(start, index));
	} catch (NumberFormatException e) {}
	start = ++index;
	while (index < length && Character.isDigit(version.charAt(index))) index++;
	try {
		if (start < length) micro = Integer.parseInt(version.substring(start, index));
	} catch (NumberFormatException e) {}
	return JAVA_VERSION(major, minor, micro);
}

/**
 * Returns the Java version number as an integer.
 *
 * @param major
 * @param minor
 * @param micro
 * @return the version
 */
public static int JAVA_VERSION (int major, int minor, int micro) {
	return (major << 16) + (minor << 8) + micro;
}

/**
 * Returns the SWT version number as an integer.
 *
 * @param major
 * @param minor
 * @return the version
 */
public static int SWT_VERSION (int major, int minor) {
	return major * 1000 + minor;
}

private static boolean extractResource(String resourceName, File outFile) {
	try (InputStream inputStream = Library.class.getResourceAsStream (resourceName)) {
		if (inputStream == null) return false;
		Files.copy(inputStream, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
	} catch (Throwable e) {
		return false;
	}

	return true;
}

/**
 * Extract file with 'mappedName' into path 'extractToFilePath'.
 * Does not overwrite existing file.
 * Does not leave trash on error.
 * @param extractToFilePath full path of where the file is to be extacted to, inc name of file,
 *                          e.g /home/USER/.swt/lib/linux/x86_64/libswt-MYLIB-gtk-4826.so
 * @param mappedName file to be searched in jar.
 * @return	true upon success, failure if something went wrong.
 */
static boolean extract (String extractToFilePath, String mappedName) {
	File file = new File(extractToFilePath);
	if (file.exists ()) return true;

	// Write to temp file first, so that other processes don't see
	// partially written library on disk
	File tempFile;
	try {
		tempFile = File.createTempFile (file.getName(), ".tmp", file.getParentFile()); //$NON-NLS-1$
	} catch (Throwable e) {
		return false;
				}

	// Extract resource
	String resourceName = "/" + mappedName; //$NON-NLS-1$
	if (!extractResource (resourceName, tempFile)) {
		tempFile.delete();
		return false;
			}

	// Make it executable
	chmod ("755", tempFile.getPath()); //$NON-NLS-1$

	// "Publish" file now that it's ready to use.
	// If there is a file already, then someone published while we were
	// extracting, just delete our file and consider it a success.
	try {
		Files.move (tempFile.toPath(), file.toPath());
	} catch (Throwable e) {
		tempFile.delete();
	}

	return true;
}

static boolean isLoadable () {
	URL url = Platform.class.getClassLoader ().getResource ("org/eclipse/swt/internal/Library.class"); //$NON-NLS-1$
	if (!url.getProtocol ().equals ("jar")) { //$NON-NLS-1$
		/* SWT is presumably running in a development environment */
		return true;
	}

	Attributes attributes = null;
	try {
		URLConnection connection = url.openConnection();
		if (!(connection instanceof JarURLConnection)) {
			/* should never happen for a "jar:" url */
			return false;
		}
		JarURLConnection jc = (JarURLConnection) connection;
		attributes = jc.getMainAttributes();
	} catch (IOException e) {
		/* should never happen for a valid SWT jar with the expected manifest values */
		return false;
	}

	String os = os ();
	String arch = arch ();
	String manifestOS = attributes.getValue ("SWT-OS"); //$NON-NLS-1$
	String manifestArch = attributes.getValue ("SWT-Arch"); //$NON-NLS-1$
	if (arch.equals (manifestArch) && os.equals (manifestOS)) {
		return true;
	}

	return false;
}

static boolean load (String libName, StringBuilder message) {
	try {
		if (libName.contains (SEPARATOR)) {
			System.load (libName);
		} else {
			System.loadLibrary (libName);
		}
		return true;
	} catch (UnsatisfiedLinkError e) {
		if (message.length() == 0) message.append(DELIMITER);
		message.append('\t');
		message.append(e.getMessage());
		message.append(DELIMITER);
	}
	return false;
}

/**
 * Loads the shared library that matches the version of the
 * Java code which is currently running.  SWT shared libraries
 * follow an encoding scheme where the major, minor and revision
 * numbers are embedded in the library name and this along with
 * <code>name</code> is used to load the library.  If this fails,
 * <code>name</code> is used in another attempt to load the library,
 * this time ignoring the SWT version encoding scheme.
 *
 * @param name the name of the library to load
 */
public static void loadLibrary (String name) {
	loadLibrary (name, true);
}

/**
 * Loads the shared library that matches the version of the
 * Java code which is currently running.  SWT shared libraries
 * follow an encoding scheme where the major, minor and revision
 * numbers are embedded in the library name and this along with
 * <code>name</code> is used to load the library.  If this fails,
 * <code>name</code> is used in another attempt to load the library,
 * this time ignoring the SWT version encoding scheme.
 *
 * @param name the name of the library to load
 * @param mapName true if the name should be mapped, false otherwise
 */
public static void loadLibrary (String name, boolean mapName) {
	String prop = System.getProperty ("sun.arch.data.model"); //$NON-NLS-1$
	if (prop == null) prop = System.getProperty ("com.ibm.vm.bitmode"); //$NON-NLS-1$
	if (prop != null) {
		if ("32".equals (prop) && IS_64) { //$NON-NLS-1$
			throw new UnsatisfiedLinkError ("Cannot load 64-bit SWT libraries on 32-bit JVM"); //$NON-NLS-1$
		}
		if ("64".equals (prop) && !IS_64) { //$NON-NLS-1$
			throw new UnsatisfiedLinkError ("Cannot load 32-bit SWT libraries on 64-bit JVM"); //$NON-NLS-1$
		}
	}

	/* Compute the library name and mapped name */
	String libName1, libName2, mappedName1, mappedName2;
	if (mapName) {
		String version = getVersionString ();
		libName1 = name + "-" + Platform.PLATFORM + "-" + version;  //$NON-NLS-1$ //$NON-NLS-2$
		libName2 = name + "-" + Platform.PLATFORM;  //$NON-NLS-1$
		mappedName1 = mapLibraryName (libName1);
		mappedName2 = mapLibraryName (libName2);
	} else {
		libName1 = libName2 = mappedName1 = mappedName2 = name;
	}

	StringBuilder message = new StringBuilder();

	/* Try loading library from swt library path */
	String path = System.getProperty (SWT_LIB_PATH); //$NON-NLS-1$
	if (path != null) {
		path = new File (path).getAbsolutePath ();
		if (load (path + SEPARATOR + mappedName1, message)) return;
		if (mapName && load (path + SEPARATOR + mappedName2, message)) return;
	}

	/* Try loading library from java library path */
	if (load (libName1, message)) return;
	if (mapName && load (libName2, message)) return;

	/* Try loading library from the tmp directory if swt library path is not specified.
	 * Create the tmp folder if it doesn't exist. Tmp folder looks like this:
	 * ~/.swt/lib/<platform>/<arch>/
	 */
	String fileName1 = mappedName1;
	String fileName2 = mappedName2;
	if (path == null) {
		path = USER_HOME;
		File dir = new File (path, SWT_LIB_DIR);
		if ((dir.exists () && dir.isDirectory ()) || dir.mkdirs ()) { // Create if not exist.
			path = dir.getAbsolutePath ();
		} else {
			/* fall back to using the home dir directory */
			if (IS_64) {
				fileName1 = mapLibraryName (libName1 + SUFFIX_64);
				fileName2 = mapLibraryName (libName2 + SUFFIX_64);
			}
		}
		if (load (path + SEPARATOR + fileName1, message)) return;
		if (mapName && load (path + SEPARATOR + fileName2, message)) return;
	}

	/* Try extracting and loading library from jar. */
	if (path != null) {
		if (extract (path + SEPARATOR + fileName1, mappedName1)) {
			if (load(path + SEPARATOR + fileName1, message)) return;
		}
		if (mapName && extract (path + SEPARATOR + fileName2, mappedName2)) {
			if (load(path + SEPARATOR + fileName2, message)) return;
		}
	}

	/* Failed to find the library */
	throw new UnsatisfiedLinkError ("Could not load SWT library. Reasons: " + message.toString()); //$NON-NLS-1$
}

static String mapLibraryName (String libName) {
	/* SWT libraries in the Macintosh use the extension .jnilib but the some VMs map to .dylib. */
	libName = System.mapLibraryName (libName);
	String ext = ".dylib"; //$NON-NLS-1$
	if (libName.endsWith(ext)) {
		libName = libName.substring(0, libName.length() - ext.length()) + ".jnilib"; //$NON-NLS-1$
	}
	return libName;
}

/**
 * @return String Combined SWT version like 4826
 */
public static String getVersionString () {
	String version = System.getProperty ("swt.version"); //$NON-NLS-1$
	if (version == null) {
		version = "" + MAJOR_VERSION; //$NON-NLS-1$
		/* Force 3 digits in minor version number */
		if (MINOR_VERSION < 10) {
			version += "00"; //$NON-NLS-1$
		} else {
			if (MINOR_VERSION < 100) version += "0"; //$NON-NLS-1$
		}
		version += MINOR_VERSION;
		/* No "r" until first revision */
		if (REVISION > 0) version += "r" + REVISION; //$NON-NLS-1$
	}
	return version;
}


/**
 * Locates a resource located either in java library path, swt library path, or attempts to extract it from inside swt.jar file.
 * This function supports a single level subfolder, e.g SubFolder/resource.
 *
 * Dev note: (17/12/07) This has been developed and throughly tested on GTK. Designed to work on Cocoa/Win as well, but not tested.
 *
 * @param subDir  'null' or a folder name without slashes. E.g Correct: 'mysubdir',  incorrect: '/subdir/'.
 *                Platform specific Slashes will be added automatically.
 * @param resourceName e.g swt-webkitgtk
 * @param mapResourceName  true if you like platform specific mapping applied to resource name. e.g  MyLib -> libMyLib-gtk-4826.so
 */
public static File findResource(String subDir, String resourceName, boolean mapResourceName){

	//We construct a 'maybe' subdirectory path. 'Maybe' because if no subDir given, then it's an empty string "".
	                                                                         //       subdir  e.g:  subdir
	String maybeSubDirPath = subDir != null ? subDir + SEPARATOR : "";       //               e.g:  subdir/  or ""
	String maybeSubDirPathWithPrefix = subDir != null ? SEPARATOR + maybeSubDirPath : ""; //  e.g: /subdir/  or ""
	final String finalResourceName = mapResourceName ?
			mapLibraryName(resourceName + "-" + Platform.PLATFORM + "-" + getVersionString ()) // e.g libMyLib-gtk-3826.so
			: resourceName;

	// 1) Look for the resource in the java/swt library path(s)
	// This code commonly finds the resource if the swt project is a required project and the swt binary (for your platform)
	// project is open in your workplace  (found in the JAVA_LIBRARY_PATH) or if you're explicitly specified SWT_LIBRARY_PATH.
	{
		Function<String, File> lookForFileInPath = searchPath -> {
			String classpath = System.getProperty(searchPath);
			if (classpath != null){
				String[] paths = classpath.split(":");
				for (String path : paths) {
				File file = new File(path + SEPARATOR + maybeSubDirPath + finalResourceName);
					if (file.exists()){
						return file;
					}
				}
			}
			return null;
		};
		File result = null;
		for (String path : new String[] {JAVA_LIB_PATH,SWT_LIB_PATH}) {
			result = lookForFileInPath.apply(path);
			if (result != null)
				return result;
		}
	}

	// 2) If SWT is ran as OSGI bundle (e.g inside Eclipse), then local resources are extracted to
	// eclipse/configuration/org.eclipse.osgi/NN/N/.cp/<resource> and we're given a pointer to the file.
	{
		// If this is an OSGI bundle look for the resource using getResource
		URL url = Library.class.getClassLoader().getResource(maybeSubDirPathWithPrefix + finalResourceName);
		URLConnection connection;
		try {
			connection = url.openConnection();
			Method getFileURLMethod = connection.getClass().getMethod("getFileURL");
			if (getFileURLMethod != null){
				// This method does the actual extraction of file to: ../eclipse/configuration/org.eclipse.osgi/NN/N/.cp/<SubDir>/resource.ext
				URL result = (URL) getFileURLMethod.invoke(connection);
				return new File(result.toURI());
			}
		} catch (Exception e) {
			// If any exceptions are thrown the resource cannot be located this way.
		}
	}

	// 3) Need to try to pull the resource out of the swt.jar.
	// Look for the resource in the user's home directory, (if already extracted in the temp swt folder. (~/.swt/lib...)
	// Extract from the swt.jar if not there already.
	{
		// Developer note:
		// To test this piece of code, you need to compile SWT into a jar and use it in a test project. E.g
		//   cd ~/git/eclipse.platform.swt.binaries/bundles/org.eclipse.swt.gtk.linux.x86_64/
		//   mvn clean verify -Pbuild-individual-bundles -Dnative=gtk.linux.x86_64
		// then ./target/ will contain org.eclipse.swt.gtk.linux.x86_64-3.106.100-SNAPSHOT.jar (and it's source),
		//  you can copy those into your test swt project and test that your resource is extracted into something like ~/.swt/...
		// Lastly, if using subDir, you need to edit the build.properties and specify the folder you wish to have included in your jar in the includes.
		File file = new File (USER_HOME + SEPARATOR +  SWT_LIB_DIR + maybeSubDirPathWithPrefix, finalResourceName);
		if (file.exists()){
			return file;
		} else { // Try to extract file from jar if not found.

			// Create temp directory if it doesn't exist
			File tempDir = new File (USER_HOME, SWT_LIB_DIR + maybeSubDirPathWithPrefix);
			if ((!tempDir.exists () || tempDir.isDirectory ())) {
				tempDir.mkdirs ();
			}

			if (extract(file.getPath(), maybeSubDirPath + finalResourceName)) {
				if (file.exists()) {
					return file;
				}
			}
		}
	}
	throw new UnsatisfiedLinkError("Could not find resource" + resourceName +  (subDir != null ? " (in subdirectory: " + subDir + " )" : ""));
}


}

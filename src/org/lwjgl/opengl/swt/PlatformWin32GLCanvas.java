package org.lwjgl.opengl.swt;

import org.eclipse.swt.widgets.Composite;

/**
 * Windows-specific implementation of methods for GLCanvas.
 * 
 * @author Kai Burjack
 */
class PlatformWin32GLCanvas extends PlatformGLCanvas {
    private static final String USE_OWNDC_KEY = "org.eclipse.swt.internal.win32.useOwnDC";

    int checkStyle(Composite parent, int style) {
        // Somehow we need to temporarily set 'org.eclipse.swt.internal.win32.useOwnDC'
        // to true or else context creation on Windows fails...
        if (parent != null) {
            if (!org.eclipse.swt.internal.win32.OS.IsWinCE
                    && org.eclipse.swt.internal.win32.OS.WIN32_VERSION >= org.eclipse.swt.internal.win32.OS.VERSION(6, 0)) {
                parent.getDisplay().setData(USE_OWNDC_KEY, Boolean.TRUE);
            }
        }
        return style;
    }

    void resetStyle(Composite parent) {
        parent.getDisplay().setData(USE_OWNDC_KEY, Boolean.FALSE);
    }
}

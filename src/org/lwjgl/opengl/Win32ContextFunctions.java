package org.lwjgl.opengl;

import static org.lwjgl.system.MemoryUtil.memAddressSafe;

import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.WGL;
import org.lwjgl.opengl.WGLARBCreateContext;
import org.lwjgl.opengl.WGLARBCreateContextProfile;
import org.lwjgl.opengl.WGLARBMultisample;
import org.lwjgl.opengl.WGLARBPixelFormat;
import org.lwjgl.system.APIBuffer;
import org.lwjgl.system.APIUtil;
import org.lwjgl.system.JNI;
import org.lwjgl.system.windows.GDI32;
import org.lwjgl.system.windows.PIXELFORMATDESCRIPTOR;
import org.lwjgl.system.windows.User32;

/**
 * Windows implementation of {@link ContextFunctions}.
 * 
 * @author Kai Burjack
 */
public class Win32ContextFunctions implements ContextFunctions {

    private static final WGL wgl;
    static {
        // Cache the WGL instance
        wgl = new WGL(GL.getFunctionProvider());
    }

    private static boolean atLeast32(int major, int minor) {
        return major == 3 && minor >= 2 || major > 3;
    }

    private static boolean atLeast30(int major, int minor) {
        return major == 3 && minor >= 0 || major > 3;
    }

    /**
     * Validate the given {@link GLContextAttributes} and throw an exception on validation error.
     * 
     * @param attribs
     *            the {@link GLContextAttributes} to validate
     */
    public static void validateAttributes(GLContextAttributes attribs) {
        if (attribs.alphaSize < 0) {
            throw new IllegalArgumentException("Alpha bits cannot be less than 0");
        }
        if (attribs.redSize < 0) {
            throw new IllegalArgumentException("Red bits cannot be less than 0");
        }
        if (attribs.greenSize < 0) {
            throw new IllegalArgumentException("Green bits cannot be less than 0");
        }
        if (attribs.blueSize < 0) {
            throw new IllegalArgumentException("Blue bits cannot be less than 0");
        }
        if (attribs.stencilSize < 0) {
            throw new IllegalArgumentException("Stencil bits cannot be less than 0");
        }
        if (attribs.depthSize < 0) {
            throw new IllegalArgumentException("Depth bits cannot be less than 0");
        }
        if (attribs.forwardCompatible && !atLeast30(attribs.majorVersion, attribs.minorVersion)) {
            throw new IllegalArgumentException("Forward-compatibility is only defined for OpenGL version 3.0 and above");
        }
        if ((attribs.compatibility || attribs.core) && !atLeast32(attribs.majorVersion, attribs.minorVersion)) {
            throw new IllegalArgumentException("Context profiles are only defined for OpenGL version 3.2 and above");
        }
    }

    private void encodePixelFormatAttribs(IntBuffer ib, GLContextAttributes attribs) {
        ib.put(WGLARBPixelFormat.WGL_DRAW_TO_WINDOW_ARB).put(1);
        ib.put(WGLARBPixelFormat.WGL_SUPPORT_OPENGL_ARB).put(1);
        ib.put(WGLARBPixelFormat.WGL_ACCELERATION_ARB).put(WGLARBPixelFormat.WGL_FULL_ACCELERATION_ARB);
        if (attribs.doubleBuffer)
            ib.put(WGLARBPixelFormat.WGL_DOUBLE_BUFFER_ARB).put(1);
        ib.put(WGLARBPixelFormat.WGL_PIXEL_TYPE_ARB).put(WGLARBPixelFormat.WGL_TYPE_RGBA_ARB);
        ib.put(WGLARBPixelFormat.WGL_RED_BITS_ARB).put(attribs.redSize);
        ib.put(WGLARBPixelFormat.WGL_GREEN_BITS_ARB).put(attribs.greenSize);
        ib.put(WGLARBPixelFormat.WGL_BLUE_BITS_ARB).put(attribs.blueSize);
        ib.put(WGLARBPixelFormat.WGL_ALPHA_BITS_ARB).put(attribs.alphaSize);
        ib.put(WGLARBPixelFormat.WGL_DEPTH_BITS_ARB).put(attribs.depthSize);
        ib.put(WGLARBPixelFormat.WGL_STENCIL_BITS_ARB).put(attribs.stencilSize);
        if (attribs.samples > 1) {
            ib.put(WGLARBMultisample.WGL_SAMPLE_BUFFERS_ARB).put(1);
            ib.put(WGLARBMultisample.WGL_SAMPLES_ARB).put(attribs.samples);
        }
        ib.put(0);
    }

    /**
     * Create a GL context using WGL and return the opaque handle to be used by other methods such as {@link #makeCurrent(long, long)}, {@link #isCurrent(long)}
     * or {@link #deleteContext(long)}.
     * 
     * @see #makeCurrent(long, long)
     * @see #isCurrent(long)
     * @see #deleteContext(long)
     * 
     * @param windowHandle
     *            the window handle
     * @param dummyWindowHandle
     *            used to query supported pixel formats
     * @param attribs
     *            the {@link GLContextAttributes}
     * @return the opaque handle of the new context
     * @throws OpenGLContextException
     */
    public long create(long windowHandle, long dummyWindowHandle, GLContextAttributes attribs) throws OpenGLContextException {
        // Validate context attributes
        validateAttributes(attribs);

        // Find this exact pixel format, though for now without multisampling. This comes later!
        PIXELFORMATDESCRIPTOR pfd = PIXELFORMATDESCRIPTOR.create();
        pfd.nSize((short) PIXELFORMATDESCRIPTOR.SIZEOF);
        pfd.nVersion((short) 1); // this should always be 1
        pfd.dwLayerMask(GDI32.PFD_MAIN_PLANE);
        pfd.iPixelType((byte) GDI32.PFD_TYPE_RGBA);
        int flags = GDI32.PFD_DRAW_TO_WINDOW | GDI32.PFD_SUPPORT_OPENGL;
        if (attribs.doubleBuffer)
            flags |= GDI32.PFD_DOUBLEBUFFER;
        if (attribs.stereo)
            flags |= GDI32.PFD_STEREO;
        pfd.dwFlags(flags);
        pfd.cRedBits((byte) attribs.redSize);
        pfd.cGreenBits((byte) attribs.greenSize);
        pfd.cBlueBits((byte) attribs.blueSize);
        pfd.cAlphaBits((byte) attribs.alphaSize);
        pfd.cDepthBits((byte) attribs.depthSize);
        pfd.cStencilBits((byte) attribs.stencilSize);
        pfd.cAccumRedBits((byte) attribs.accumRedSize);
        pfd.cAccumGreenBits((byte) attribs.accumGreenSize);
        pfd.cAccumBlueBits((byte) attribs.accumBlueSize);
        pfd.cAccumAlphaBits((byte) attribs.accumAlphaSize);
        pfd.cAccumBits((byte) (attribs.accumRedSize + attribs.accumGreenSize + attribs.accumBlueSize + attribs.accumAlphaSize));
        long hDCdummy = User32.GetDC(dummyWindowHandle);
        int pixelFormat = GDI32.ChoosePixelFormat(hDCdummy, pfd);
        if (pixelFormat == 0 || GDI32.SetPixelFormat(hDCdummy, pixelFormat, pfd) == 0) {
            // Pixel format unsupported
            User32.ReleaseDC(dummyWindowHandle, hDCdummy);
            throw new OpenGLContextException("Unsupported pixel format");
        }

        /*
         * Next, create a dummy context using Opengl32.lib's wglCreateContext. This should ALWAYS work, but won't give us a "new"/"core" context if we requested
         * that and also does not support multisampling. But we use this "dummy" context then to request the required WGL function pointers to create a new
         * OpenGL >= 3.0 context and with optional multisampling.
         */
        long dummyContext = JNI.callPP(wgl.CreateContext, hDCdummy);
        if (dummyContext == 0) {
            User32.ReleaseDC(dummyWindowHandle, hDCdummy);
            throw new OpenGLContextException("Failed to create OpenGL context");
        }

        // If version was < 3.0 and no multisampling is requested we are done.
        if (attribs.majorVersion < 3 && attribs.minorVersion <= 0 && attribs.samples <= 1) {
            User32.ReleaseDC(dummyWindowHandle, hDCdummy);

            /* Finally, create the real context on the real window */
            long hDC = User32.GetDC(windowHandle);
            GDI32.SetPixelFormat(hDC, pixelFormat, pfd);
            JNI.callPI(wgl.DeleteContext, dummyContext);
            long context = JNI.callPP(wgl.CreateContext, hDC);
            User32.ReleaseDC(windowHandle, hDC);
            return context;
        }

        // If we want a GL >= 3.0 context or multisampling, we next find the ARB extension to create a "new" context.
        // But because this is the first time we actually need to make a context current, we store the currently
        // active context (if any) to restore it later.
        long currentContext = JNI.callP(wgl.GetCurrentContext);
        long currentDc = JNI.callP(wgl.GetCurrentDC);

        // Make the new dummy context current
        int success = JNI.callPPI(wgl.MakeCurrent, hDCdummy, dummyContext);
        User32.ReleaseDC(windowHandle, hDCdummy);
        if (success == 0) {
            JNI.callPI(wgl.DeleteContext, dummyContext);
            JNI.callPPI(wgl.MakeCurrent, currentDc, currentContext);
            throw new OpenGLContextException("Failed to make OpenGL context current");
        }

        // Obtain wglCreateContextAttribsARB function pointer
        APIBuffer buffer = APIUtil.apiBuffer();
        int procEncoded = buffer.stringParamASCII("wglCreateContextAttribsARB", true);
        long adr = buffer.address(procEncoded);
        long wglCreateContextAttribsARBAddr = JNI.callPP(wgl.GetProcAddress, adr);
        if (wglCreateContextAttribsARBAddr == 0L) {
            JNI.callPI(wgl.DeleteContext, dummyContext);
            JNI.callPPI(wgl.MakeCurrent, currentDc, currentContext);
            throw new OpenGLContextException("No support for wglCreateContextAttribsARB. Cannot create OpenGL context.");
        }

        IntBuffer attribList = BufferUtils.createIntBuffer(32);
        long attribListAddr = memAddressSafe(attribList);
        long hDC = User32.GetDC(windowHandle);

        // Obtain wglChoosePixelFormatARB if multisampling is requested
        if (attribs.samples > 1) {
            procEncoded = buffer.stringParamASCII("wglChoosePixelFormatARB", true);
            adr = buffer.address(procEncoded);
            long wglChoosePixelFormatARBAddr = JNI.callPP(wgl.GetProcAddress, adr);
            if (wglChoosePixelFormatARBAddr == 0L) {
                User32.ReleaseDC(windowHandle, hDC);
                JNI.callPI(wgl.DeleteContext, dummyContext);
                JNI.callPPI(wgl.MakeCurrent, currentDc, currentContext);
                throw new OpenGLContextException("No support for wglChoosePixelFormatARB. Cannot query supported pixel formats.");
            }
            encodePixelFormatAttribs(attribList, attribs);
            // Obtain device context of the real window
            IntBuffer piFormats = BufferUtils.createIntBuffer(2);
            long piFormatsAddr = memAddressSafe(piFormats);
            int succ = JNI.callPPPIPPI(wglChoosePixelFormatARBAddr, hDC, attribListAddr, 0L, 1, piFormatsAddr + 4, piFormatsAddr);
            int numFormats = piFormats.get(0);
            if (succ == 0 || numFormats == 0) {
                User32.ReleaseDC(windowHandle, hDC);
                JNI.callPI(wgl.DeleteContext, dummyContext);
                JNI.callPPI(wgl.MakeCurrent, currentDc, currentContext);
                throw new OpenGLContextException("No supported pixel format found.");
            }
            pixelFormat = piFormats.get(1);
            // Describe pixel format
            int pixFmtIndex = GDI32.DescribePixelFormat(hDC, pixelFormat, pfd);
            if (pixFmtIndex == 0) {
                User32.ReleaseDC(windowHandle, hDC);
                JNI.callPI(wgl.DeleteContext, dummyContext);
                JNI.callPPI(wgl.MakeCurrent, currentDc, currentContext);
                throw new OpenGLContextException("Failed to validate supported pixel format.");
            }
        }

        // Compose the attributes list
        attribList.rewind();
        if (attribs.majorVersion >= 3) {
            attribList.put(WGLARBCreateContext.WGL_CONTEXT_MAJOR_VERSION_ARB).put(attribs.majorVersion);
            attribList.put(WGLARBCreateContext.WGL_CONTEXT_MINOR_VERSION_ARB).put(attribs.minorVersion);
        }
        int profile = 0;
        if (attribs.compatibility) {
            profile = WGLARBCreateContextProfile.WGL_CONTEXT_COMPATIBILITY_PROFILE_BIT_ARB;
        }
        if (attribs.core) {
            profile = WGLARBCreateContextProfile.WGL_CONTEXT_CORE_PROFILE_BIT_ARB;
        }
        if (profile > 0)
            attribList.put(WGLARBCreateContextProfile.WGL_CONTEXT_PROFILE_MASK_ARB).put(profile);
        int contextFlags = 0;
        if (attribs.debug) {
            contextFlags |= WGLARBCreateContext.WGL_CONTEXT_DEBUG_BIT_ARB;
        }
        if (attribs.forwardCompatible) {
            contextFlags |= WGLARBCreateContext.WGL_CONTEXT_FORWARD_COMPATIBLE_BIT_ARB;
        }
        if (contextFlags > 0)
            attribList.put(WGLARBCreateContext.WGL_CONTEXT_FLAGS_ARB).put(contextFlags);
        attribList.put(0).put(0);
        // Set pixelformat
        int succ = GDI32.SetPixelFormat(hDC, pixelFormat, pfd);
        if (succ == 0) {
            User32.ReleaseDC(windowHandle, hDC);
            JNI.callPI(wgl.DeleteContext, dummyContext);
            JNI.callPPI(wgl.MakeCurrent, currentDc, currentContext);
            throw new OpenGLContextException("Failed to set pixel format.");
        }
        // And create new context with it
        long newCtx = JNI.callPPPP(wglCreateContextAttribsARBAddr, hDC, 0L, attribListAddr);
        User32.ReleaseDC(windowHandle, hDC);
        if (newCtx == 0L) {
            // Make old context current
            JNI.callPPI(wgl.MakeCurrent, currentDc, currentContext);
            // Could not create new context
            throw new OpenGLContextException("Failed to create OpenGL context.");
        }
        // Restore old context
        JNI.callPPI(wgl.MakeCurrent, currentDc, currentContext);
        // Destroy dummy context
        JNI.callPI(wgl.DeleteContext, dummyContext);
        return newCtx;
    }

    public boolean isCurrent(long context) {
        long ret = JNI.callP(wgl.GetCurrentContext);
        return ret == context;
    }

    public boolean makeCurrent(long windowHandle, long context) {
        long hDC = User32.GetDC(windowHandle);
        int ret = JNI.callPPI(wgl.MakeCurrent, hDC, context);
        User32.ReleaseDC(windowHandle, hDC);
        return ret == 1;
    }

    public boolean deleteContext(long context) {
        int ret = JNI.callPI(wgl.DeleteContext, context);
        return ret == 1;
    }

    public boolean swapBuffers(long windowHandle) {
        long hDC = User32.GetDC(windowHandle);
        int ret = GDI32.SwapBuffers(hDC);
        User32.ReleaseDC(windowHandle, hDC);
        return ret == 1;
    }

}
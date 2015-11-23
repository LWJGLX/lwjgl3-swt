package org.lwjgl.opengl;

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
import org.lwjgl.system.MemoryUtil;
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
        if (attribs.profile < 0 || attribs.profile > 2) {
            throw new IllegalArgumentException("Invalid profile.");
        }
        if (attribs.samples < 0) {
            throw new IllegalArgumentException("Invalid samples count");
        }
        if (attribs.profile > 0 && !atLeast32(attribs.majorVersion, attribs.minorVersion)) {
            throw new IllegalArgumentException("Context profiles are only defined for OpenGL version 3.2 and above");
        }
    }

    private void encodePixelFormatAttribs(IntBuffer ib, GLContextAttributes attribs) {
        ib.put(WGLARBPixelFormat.WGL_DRAW_TO_WINDOW_ARB).put(1);
        ib.put(WGLARBPixelFormat.WGL_SUPPORT_OPENGL_ARB).put(1);
        ib.put(WGLARBPixelFormat.WGL_ACCELERATION_ARB).put(WGLARBPixelFormat.WGL_FULL_ACCELERATION_ARB);
        if (attribs.doubleBuffer)
            ib.put(WGLARBPixelFormat.WGL_DOUBLE_BUFFER_ARB).put(1);
        if (attribs.floatPixelFormat)
            ib.put(WGLARBPixelFormat.WGL_PIXEL_TYPE_ARB).put(WGLARBPixelFormatFloat.WGL_TYPE_RGBA_FLOAT_ARB);
        else
            ib.put(WGLARBPixelFormat.WGL_PIXEL_TYPE_ARB).put(WGLARBPixelFormat.WGL_TYPE_RGBA_ARB);
        ib.put(WGLARBPixelFormat.WGL_RED_BITS_ARB).put(attribs.redSize);
        ib.put(WGLARBPixelFormat.WGL_GREEN_BITS_ARB).put(attribs.greenSize);
        ib.put(WGLARBPixelFormat.WGL_BLUE_BITS_ARB).put(attribs.blueSize);
        ib.put(WGLARBPixelFormat.WGL_ALPHA_BITS_ARB).put(attribs.alphaSize);
        ib.put(WGLARBPixelFormat.WGL_DEPTH_BITS_ARB).put(attribs.depthSize);
        ib.put(WGLARBPixelFormat.WGL_STENCIL_BITS_ARB).put(attribs.stencilSize);
        ib.put(WGLARBPixelFormat.WGL_ACCUM_RED_BITS_ARB).put(attribs.accumRedSize);
        ib.put(WGLARBPixelFormat.WGL_ACCUM_GREEN_BITS_ARB).put(attribs.accumGreenSize);
        ib.put(WGLARBPixelFormat.WGL_ACCUM_BLUE_BITS_ARB).put(attribs.accumBlueSize);
        ib.put(WGLARBPixelFormat.WGL_ACCUM_ALPHA_BITS_ARB).put(attribs.accumAlphaSize);
        ib.put(WGLARBPixelFormat.WGL_ACCUM_BITS_ARB).put(attribs.accumRedSize + attribs.accumGreenSize + attribs.accumBlueSize + attribs.accumAlphaSize);
        if (attribs.sRGB)
            ib.put(WGLEXTFramebufferSRGB.WGL_FRAMEBUFFER_SRGB_CAPABLE_EXT).put(1);
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
        if (dummyContext == 0L) {
            User32.ReleaseDC(dummyWindowHandle, hDCdummy);
            throw new OpenGLContextException("Failed to create OpenGL context");
        }

        APIBuffer buffer = APIUtil.apiBuffer();
        long bufferAddr = buffer.address();

        // Save current context to restore it later
        long currentContext = JNI.callP(wgl.GetCurrentContext);
        long currentDc = JNI.callP(wgl.GetCurrentDC);

        // Make the new dummy context current
        int success = JNI.callPPI(wgl.MakeCurrent, hDCdummy, dummyContext);
        if (success == 0) {
            User32.ReleaseDC(dummyWindowHandle, hDCdummy);
            JNI.callPI(wgl.DeleteContext, dummyContext);
            throw new OpenGLContextException("Failed to make OpenGL context current");
        }

        // Query supported WGL extensions
        String wglExtensions = null;
        int procEncoded = buffer.stringParamASCII("wglGetExtensionsStringARB", true);
        long adr = buffer.address(procEncoded);
        long wglGetExtensionsStringARBAddr = JNI.callPP(wgl.GetProcAddress, adr);
        if (wglGetExtensionsStringARBAddr != 0L) {
            long str = JNI.callPP(wglGetExtensionsStringARBAddr, hDCdummy);
            if (str != 0L) {
                wglExtensions = MemoryUtil.memDecodeASCII(str);
            } else {
                wglExtensions = "";
            }
        } else {
            wglExtensions = "";
        }

        success = User32.ReleaseDC(dummyWindowHandle, hDCdummy);
        if (success == 0) {
            JNI.callPI(wgl.DeleteContext, dummyContext);
            JNI.callPPI(wgl.MakeCurrent, currentDc, currentContext);
            throw new OpenGLContextException("Could not release dummy DC");
        }

        // For some constellations of context attributes, we can stop right here.
        if (!atLeast30(attribs.majorVersion, attribs.minorVersion) && attribs.samples <= 1 && !attribs.sRGB && !attribs.floatPixelFormat
                && attribs.contextReleaseBehavior == 0) {
            /* Finally, create the real context on the real window */
            long hDC = User32.GetDC(windowHandle);
            GDI32.SetPixelFormat(hDC, pixelFormat, pfd);
            success = JNI.callPI(wgl.DeleteContext, dummyContext);
            if (success == 0) {
                JNI.callPPI(wgl.MakeCurrent, currentDc, currentContext);
                throw new OpenGLContextException("Could not delete dummy GL context");
            }
            long context = JNI.callPP(wgl.CreateContext, hDC);

            if (attribs.swapInterval > 0) {
                boolean has_WGL_EXT_swap_control = wglExtensions.contains("WGL_EXT_swap_control");
                if (!has_WGL_EXT_swap_control) {
                    User32.ReleaseDC(windowHandle, hDC);
                    JNI.callPPI(wgl.MakeCurrent, currentDc, currentContext);
                    throw new OpenGLContextException("Swap interval requested but WGL_EXT_swap_control is unavailable");
                }
                // Make context current to set the swap interval
                success = JNI.callPPI(wgl.MakeCurrent, hDC, context);
                if (success == 0) {
                    User32.ReleaseDC(windowHandle, hDC);
                    JNI.callPPI(wgl.MakeCurrent, currentDc, currentContext);
                    throw new OpenGLContextException("Could not make GL context current");
                }
                procEncoded = buffer.stringParamASCII("wglSwapIntervalEXT", true);
                adr = buffer.address(procEncoded);
                long wglSwapIntervalEXTAddr = JNI.callPP(wgl.GetProcAddress, adr);
                if (wglSwapIntervalEXTAddr != 0L) {
                    JNI.callII(wglSwapIntervalEXTAddr, attribs.swapInterval);
                }
                // Restore old context
                JNI.callPPI(wgl.MakeCurrent, currentDc, currentContext);
            }

            success = User32.ReleaseDC(windowHandle, hDC);
            if (success == 0) {
                JNI.callPPI(wgl.MakeCurrent, currentDc, currentContext);
                throw new OpenGLContextException("Could not release DC");
            }

            /* Check if we want to share context */
            if (attribs.shareContext != 0L) {
                int succ = JNI.callPPI(wgl.ShareLists, context, attribs.shareContext);
                if (succ == 0) {
                    JNI.callPI(wgl.DeleteContext, context);
                    JNI.callPPI(wgl.MakeCurrent, currentDc, currentContext);
                    throw new OpenGLContextException("Failed while configuring context sharing.");
                }
            }

            return context;
        }

        // Obtain wglCreateContextAttribsARB function pointer
        procEncoded = buffer.stringParamASCII("wglCreateContextAttribsARB", true);
        adr = buffer.address(procEncoded);
        long wglCreateContextAttribsARBAddr = JNI.callPP(wgl.GetProcAddress, adr);
        if (wglCreateContextAttribsARBAddr == 0L) {
            JNI.callPI(wgl.DeleteContext, dummyContext);
            JNI.callPPI(wgl.MakeCurrent, currentDc, currentContext);
            throw new OpenGLContextException("No support for wglCreateContextAttribsARB. Cannot create OpenGL context.");
        }

        IntBuffer attribList = BufferUtils.createIntBuffer(64);
        long attribListAddr = MemoryUtil.memAddress(attribList);
        long hDC = User32.GetDC(windowHandle);

        // Obtain wglChoosePixelFormatARB if multisampling or sRGB or floating point pixel format is requested
        if (attribs.samples > 1 || attribs.sRGB || attribs.floatPixelFormat) {
            procEncoded = buffer.stringParamASCII("wglChoosePixelFormatARB", true);
            adr = buffer.address(procEncoded);
            long wglChoosePixelFormatARBAddr = JNI.callPP(wgl.GetProcAddress, adr);
            if (wglChoosePixelFormatARBAddr == 0L) {
                User32.ReleaseDC(windowHandle, hDC);
                JNI.callPI(wgl.DeleteContext, dummyContext);
                JNI.callPPI(wgl.MakeCurrent, currentDc, currentContext);
                throw new OpenGLContextException("No support for wglChoosePixelFormatARB. Cannot query supported pixel formats.");
            }
            if (attribs.samples > 1) {
                boolean has_WGL_ARB_multisample = wglExtensions.contains("WGL_ARB_multisample");
                if (!has_WGL_ARB_multisample) {
                    User32.ReleaseDC(windowHandle, hDC);
                    JNI.callPI(wgl.DeleteContext, dummyContext);
                    JNI.callPPI(wgl.MakeCurrent, currentDc, currentContext);
                    throw new OpenGLContextException("Multisampling requested but WGL_ARB_multisample is unavailable");
                }
            }
            // Query matching pixel formats
            encodePixelFormatAttribs(attribList, attribs);
            int succ = JNI.callPPPIPPI(wglChoosePixelFormatARBAddr, hDC, attribListAddr, 0L, 1, bufferAddr + 4, bufferAddr);
            int numFormats = buffer.buffer().getInt(0);
            if (succ == 0 || numFormats == 0) {
                User32.ReleaseDC(windowHandle, hDC);
                JNI.callPI(wgl.DeleteContext, dummyContext);
                JNI.callPPI(wgl.MakeCurrent, currentDc, currentContext);
                throw new OpenGLContextException("No supported pixel format found.");
            }
            pixelFormat = buffer.buffer().getInt(4);
            // Describe pixel format for the PIXELFORMATDESCRIPTOR to match the chosen format
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
        if (atLeast30(attribs.majorVersion, attribs.minorVersion)) {
            attribList.put(WGLARBCreateContext.WGL_CONTEXT_MAJOR_VERSION_ARB).put(attribs.majorVersion);
            attribList.put(WGLARBCreateContext.WGL_CONTEXT_MINOR_VERSION_ARB).put(attribs.minorVersion);
        }
        int profile = 0;
        if (attribs.profile == GLContextAttributes.OPENGL_COMPATIBILITY_PROFILE) {
            profile = WGLARBCreateContextProfile.WGL_CONTEXT_COMPATIBILITY_PROFILE_BIT_ARB;
        } else if (attribs.profile == GLContextAttributes.OPENGL_CORE_PROFILE) {
            profile = WGLARBCreateContextProfile.WGL_CONTEXT_CORE_PROFILE_BIT_ARB;
        }
        if (profile > 0) {
            boolean has_WGL_ARB_create_context_profile = wglExtensions.contains("WGL_ARB_create_context_profile");
            if (!has_WGL_ARB_create_context_profile) {
                User32.ReleaseDC(windowHandle, hDC);
                JNI.callPI(wgl.DeleteContext, dummyContext);
                JNI.callPPI(wgl.MakeCurrent, currentDc, currentContext);
                throw new OpenGLContextException("OpenGL profile requested but WGL_ARB_create_context_profile is unavailable");
            }
            attribList.put(WGLARBCreateContextProfile.WGL_CONTEXT_PROFILE_MASK_ARB).put(profile);
        }
        int contextFlags = 0;
        if (attribs.debug) {
            contextFlags |= WGLARBCreateContext.WGL_CONTEXT_DEBUG_BIT_ARB;
        }
        if (attribs.forwardCompatible) {
            contextFlags |= WGLARBCreateContext.WGL_CONTEXT_FORWARD_COMPATIBLE_BIT_ARB;
        }
        if (contextFlags > 0)
            attribList.put(WGLARBCreateContext.WGL_CONTEXT_FLAGS_ARB).put(contextFlags);
        if (attribs.contextReleaseBehavior > 0) {
            boolean has_WGL_ARB_context_flush_control = wglExtensions.contains("WGL_ARB_context_flush_control");
            if (!has_WGL_ARB_context_flush_control) {
                User32.ReleaseDC(windowHandle, hDC);
                JNI.callPI(wgl.DeleteContext, dummyContext);
                JNI.callPPI(wgl.MakeCurrent, currentDc, currentContext);
                throw new OpenGLContextException("Context release behaviour requested but WGL_ARB_context_flush_control is unavailable");
            }
            if (attribs.contextReleaseBehavior == GLContextAttributes.CONTEXT_RELEASE_BEHAVIOR_NONE)
                attribList.put(WGLARBContextFlushControl.WGL_CONTEXT_RELEASE_BEHAVIOR_ARB).put(WGLARBContextFlushControl.WGL_CONTEXT_RELEASE_BEHAVIOR_NONE_ARB);
            else if (attribs.contextReleaseBehavior == GLContextAttributes.CONTEXT_RELEASE_BEHAVIOR_FLUSH)
                attribList.put(WGLARBContextFlushControl.WGL_CONTEXT_RELEASE_BEHAVIOR_ARB)
                        .put(WGLARBContextFlushControl.WGL_CONTEXT_RELEASE_BEHAVIOR_FLUSH_ARB);
        }
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
        long newCtx = JNI.callPPPP(wglCreateContextAttribsARBAddr, hDC, attribs.shareContext, attribListAddr);
        if (newCtx == 0L) {
            User32.ReleaseDC(windowHandle, hDC);
            JNI.callPI(wgl.DeleteContext, dummyContext);
            JNI.callPPI(wgl.MakeCurrent, currentDc, currentContext);
            throw new OpenGLContextException("Failed to create OpenGL context.");
        }
        if (attribs.swapInterval > 0) {
            boolean has_WGL_EXT_swap_control = wglExtensions.contains("WGL_EXT_swap_control");
            if (!has_WGL_EXT_swap_control) {
                User32.ReleaseDC(windowHandle, hDC);
                JNI.callPI(wgl.DeleteContext, dummyContext);
                JNI.callPPI(wgl.MakeCurrent, currentDc, currentContext);
                throw new OpenGLContextException("Swap interval requested but WGL_EXT_swap_control is unavailable");
            }
            // Make context current to set the swap interval
            JNI.callPPI(wgl.MakeCurrent, hDC, newCtx);
            procEncoded = buffer.stringParamASCII("wglSwapIntervalEXT", true);
            adr = buffer.address(procEncoded);
            long wglSwapIntervalEXTAddr = JNI.callPP(wgl.GetProcAddress, adr);
            if (wglSwapIntervalEXTAddr != 0L) {
                JNI.callII(wglSwapIntervalEXTAddr, attribs.swapInterval);
            }
        }
        User32.ReleaseDC(windowHandle, hDC);
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

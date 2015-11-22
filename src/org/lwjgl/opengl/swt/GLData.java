package org.lwjgl.opengl.swt;

import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.GLContextAttributes;

/**
 * Drop-in replacement for SWT's {@link org.eclipse.swt.opengl.GLData} class.
 * 
 * @author Kai Burjack
 */
public class GLData {

    /*
     * The following fields are taken from SWT's original GLData
     */

    /**
     * Whether to use double-buffering. It defaults to <code>true</code>.
     */
    public boolean doubleBuffer = true;
    /**
     * Whether to use different LEFT and RIGHT backbuffers for stereo rendering. It defaults to <code>false</code>.
     */
    public boolean stereo;
    /**
     * The number of bits for the red color channel. It defaults to 8.
     */
    public int redSize = 8;
    /**
     * The number of bits for the green color channel. It defaults to 8.
     */
    public int greenSize = 8;
    /**
     * The number of bits for the blue color channel. It defaults to 8.
     */
    public int blueSize = 8;
    /**
     * The number of bits for the alpha color channel. It defaults to 8.
     */
    public int alphaSize = 8;
    /**
     * The number of bits for the depth channel. It defaults to 24.
     */
    public int depthSize = 24;
    /**
     * The number of bits for the stencil channel. It defaults to 0.
     */
    public int stencilSize;
    /**
     * The number of bits for the red accumulator color channel. It defaults to 0.
     */
    public int accumRedSize;
    /**
     * The number of bits for the green accumulator color channel. It defaults to 0.
     */
    public int accumGreenSize;
    /**
     * The number of bits for the blue accumulator color channel. It defaults to 0.
     */
    public int accumBlueSize;
    /**
     * The number of bits for the alpha accumulator color channel. It defaults to 0.
     */
    public int accumAlphaSize;
    public int sampleBuffers;
    public int samples;
    public GLCanvas shareContext;

    /*
     * New fields not in SWT's GLData
     */

    /**
     * Constant for the core profile. This is only valid when ({@link #majorVersion}.{@link #minorVersion}) is at least 3.2.
     * 
     * @see #profile
     */
    public static final int OPENGL_CORE_PROFILE = 1;
    /**
     * Constant for the compatibility profile.
     * 
     * @see #profile
     */
    public static final int OPENGL_COMPATIBILITY_PROFILE = 2;

    /**
     * The major GL context version to use. It defaults to 0 for "not specified".
     */
    public int majorVersion;
    /**
     * The minor GL context version to use. If {@link #majorVersion} is 0 this field is unused.
     */
    public int minorVersion;
    /**
     * Whether a forward-compatible context should be created. This has only an effect when ({@link #majorVersion}. {@link #minorVersion}) is at least 3.2.
     */
    public boolean forwardCompatible;
    /**
     * Whether a context with the compatibility profile should be created.
     */
    public boolean compatibility;
    /**
     * The profile to use. Defaults to 0, which means "not specified".
     */
    public int profile;
    /**
     * Whether a debug context should be requested.
     */
    public boolean debug;

    /**
     * Convert this {@link GLData} to an equivalent {@link GLContextAttributes} object to be used with {@link GLContext#create(long, GLContextAttributes)}.
     * 
     * @return the created {@link GLContextAttributes} object
     */
    public GLContextAttributes toContextAttributes() {
        GLContextAttributes attribs = new GLContextAttributes();
        attribs.accumAlphaSize = accumAlphaSize;
        attribs.accumBlueSize = accumBlueSize;
        attribs.accumGreenSize = accumGreenSize;
        attribs.accumGreenSize = accumGreenSize;
        attribs.alphaSize = alphaSize;
        attribs.blueSize = blueSize;
        attribs.profile = profile;
        attribs.debug = debug;
        attribs.depthSize = depthSize;
        attribs.doubleBuffer = doubleBuffer;
        attribs.forwardCompatible = forwardCompatible;
        attribs.greenSize = greenSize;
        attribs.majorVersion = majorVersion;
        attribs.minorVersion = minorVersion;
        attribs.redSize = redSize;
        attribs.sampleBuffers = sampleBuffers;
        attribs.samples = samples;
        attribs.stencilSize = stencilSize;
        attribs.stereo = stereo;
        attribs.shareContext = shareContext != null ? shareContext.context : 0L;
        return attribs;
    }

}

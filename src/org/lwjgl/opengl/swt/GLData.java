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
    /**
     * This is ignored. It will implicitly be 1 if {@link #samples} is set to a value greater than or equal to 1.
     */
    public int sampleBuffers;
    /**
     * The number of (coverage) samples for multisampling. Multisampling will only be requested for a value greater than or equal to 1.
     */
    public int samples;
    /**
     * The {@link GLCanvas} whose context objects should be shared with the context created using <code>this</code> GLData.
     */
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
     * Constant for doing nothing on context switch.
     * 
     * @see #contextReleaseBehavior
     */
    public static final int CONTEXT_RELEASE_BEHAVIOR_NONE = 1;
    /**
     * Constant for flushing GL pipeline on context switch.
     * 
     * @see #contextReleaseBehavior
     */
    public static final int CONTEXT_RELEASE_BEHAVIOR_FLUSH = 2;

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
     * The profile to use. Defaults to 0, which means "not specified".
     */
    public int profile;
    /**
     * Whether a debug context should be requested.
     */
    public boolean debug;
    /**
     * Set the swap interval.
     */
    public Integer swapInterval;
    /**
     * Whether to use sRGB color space.
     */
    public boolean sRGB;
    /**
     * Whether to use a floating point pixel format.
     */
    public boolean pixelFormatFloat;
    /**
     * Specify the behavior on context switch.
     */
    public int contextReleaseBehavior;
    /**
     * The number of color samples per pixel. This is only valid when {@link #samples} is at least 1.
     */
    public int colorSamplesNV;
    /**
     * The swap group index. Use this to synchronize buffer swaps across multiple windows on the same system.
     */
    public int swapGroupNV;
    /**
     * The swap barrier index. Use this to synchronize buffer swaps across multiple systems. This requires a Nvidia G-Sync card.
     */
    public int swapBarrierNV;
    /**
     * Whether robust buffer access should be used.
     */
    public boolean robustness;
    /**
     * When {@link #robustness} is <code>true</code> then this specifies whether a GL_LOSE_CONTEXT_ON_RESET_ARB reset notification is sent, as described by GL_ARB_robustness.
     */
    public boolean loseContextOnReset;
    /**
     * When {@link #robustness} is <code>true</code> and {@link #loseContextOnReset} is <code>true</code> then this specifies whether a graphics reset only affects
     * the current application and no other application in the system.
     */
    public boolean contextResetIsolation;

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
        attribs.accumRedSize = accumRedSize;
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
        attribs.swapInterval = swapInterval;
        attribs.sRGB = sRGB;
        attribs.floatPixelFormat = pixelFormatFloat;
        attribs.contextReleaseBehavior = contextReleaseBehavior;
        attribs.colorSamplesNV = colorSamplesNV;
        attribs.swapGroupNV = swapGroupNV;
        attribs.swapBarrierNV = swapBarrierNV;
        attribs.robustness = robustness;
        attribs.loseContextOnReset = loseContextOnReset;
        attribs.contextResetIsolation = contextResetIsolation;
        attribs.shareContext = shareContext != null ? shareContext.context : 0L;
        return attribs;
    }

    /**
     * Convert the given {@link GLContextAttributes} to an equivalent {@link GLData} object.
     * 
     * @return the created {@link GLData} object
     */
    public void fromContextAttributes(GLContextAttributes ctx) {
        accumAlphaSize = ctx.accumAlphaSize;
        accumBlueSize = ctx.accumBlueSize;
        accumGreenSize = ctx.accumGreenSize;
        accumRedSize = ctx.accumRedSize;
        alphaSize = ctx.alphaSize;
        blueSize = ctx.blueSize;
        profile = ctx.profile;
        debug = ctx.debug;
        depthSize = ctx.depthSize;
        doubleBuffer = ctx.doubleBuffer;
        forwardCompatible = ctx.forwardCompatible;
        greenSize = ctx.greenSize;
        majorVersion = ctx.majorVersion;
        minorVersion = ctx.minorVersion;
        redSize = ctx.redSize;
        sampleBuffers = ctx.sampleBuffers;
        samples = ctx.samples;
        stencilSize = ctx.stencilSize;
        stereo = ctx.stereo;
        swapInterval = ctx.swapInterval;
        sRGB = ctx.sRGB;
        pixelFormatFloat = ctx.floatPixelFormat;
        contextReleaseBehavior = ctx.contextReleaseBehavior;
        colorSamplesNV = ctx.colorSamplesNV;
        swapGroupNV = ctx.swapGroupNV;
        swapBarrierNV = ctx.swapBarrierNV;
        robustness = ctx.robustness;
        loseContextOnReset = ctx.loseContextOnReset;
        contextResetIsolation = ctx.contextResetIsolation;
    }

}

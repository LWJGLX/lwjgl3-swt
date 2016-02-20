package org.lwjgl.vulkan.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.lwjgl.system.Platform;

public class VKCanvas extends Canvas {
    private static PlatformVKCanvas platformCanvas;
    static {
        String platformClassName;
        switch (Platform.get()) {
        case WINDOWS:
            platformClassName = "org.lwjgl.vulkan.swt.PlatformWin32VKCanvas";
            break;
        default:
            throw new AssertionError("NYI");
        }
        try {
            @SuppressWarnings("unchecked")
            Class<? extends PlatformVKCanvas> clazz = (Class<? extends PlatformVKCanvas>) VKCanvas.class.getClassLoader().loadClass(platformClassName);
            platformCanvas = clazz.newInstance();
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Platform-specific VKCanvas class not found: " + platformClassName);
        } catch (InstantiationException e) {
            throw new AssertionError("Could not instantiate platform-specific VKCanvas class: " + platformClassName);
        } catch (IllegalAccessException e) {
            throw new AssertionError("Could not instantiate platform-specific VKCanvas class: " + platformClassName);
        }
    }

    /**
     * The Vulkan surface handle for this {@link VKCanvas}.
     */
    public long surface;

    /**
     * Create a VKCanvas widget using the attributes described in the VKData object provided.
     *
     * @param parent a composite widget
     * @param style the bitwise OR'ing of widget styles
     * @param data the requested attributes of the VKCanvas
     *
     * @exception IllegalArgumentException
     * <ul>
     * <li>ERROR_NULL_ARGUMENT when the data is null
     * <li>ERROR_UNSUPPORTED_DEPTH when the requested attributes cannot be provided
     * </ul>
     */
    public VKCanvas(Composite parent, int style, VKData data) {
        super(parent, platformCanvas.checkStyle(parent, style));
        if (Platform.get() == Platform.WINDOWS) {
            platformCanvas.resetStyle(parent);
        }
        if (data == null)
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        surface = platformCanvas.create(this, data);
    }

}

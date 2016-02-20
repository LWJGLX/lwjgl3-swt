package org.lwjgl.vulkan.swt;

import static org.lwjgl.system.JNI.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRWin32Surface.*;

import org.eclipse.swt.SWTException;
import org.eclipse.swt.internal.win32.OS;
import org.eclipse.swt.widgets.Composite;
import org.lwjgl.PointerBuffer;

import org.lwjgl.vulkan.VkWin32SurfaceCreateInfoKHR;

public class PlatformWin32VKCanvas implements PlatformVKCanvas {
    private static final String USE_OWNDC_KEY = "org.eclipse.swt.internal.win32.useOwnDC";

    public int checkStyle(Composite parent, int style) {
        // Somehow we need to temporarily set 'org.eclipse.swt.internal.win32.useOwnDC' to true or else context creation on Windows fails...
        if (parent != null) {
            if (!org.eclipse.swt.internal.win32.OS.IsWinCE
                    && org.eclipse.swt.internal.win32.OS.WIN32_VERSION >= org.eclipse.swt.internal.win32.OS.VERSION(6, 0)) {
                parent.getDisplay().setData(USE_OWNDC_KEY, Boolean.TRUE);
            }
        }
        return style;
    }

    public void resetStyle(Composite parent) {
        parent.getDisplay().setData(USE_OWNDC_KEY, Boolean.FALSE);
    }

    @Override
    public long create(Composite composite, VKData data) {
        // Obtain vkCreateWin32SurfaceKHR function pointer
        long vkCreateWin32SurfaceKHR = vkGetInstanceProcAddr(data.instance, "vkCreateWin32SurfaceKHR");
        if (vkCreateWin32SurfaceKHR == 0L) {
            throw new SWTException("vkCreateWin32SurfaceKHR unavailable for VkInstance: " + data.instance);
        }

        VkWin32SurfaceCreateInfoKHR sci = VkWin32SurfaceCreateInfoKHR.calloc();
        sci.sType(VK_STRUCTURE_TYPE_WIN32_SURFACE_CREATE_INFO_KHR);
        sci.hinstance(OS.GetModuleHandle(null));
        sci.hwnd(composite.handle);

        PointerBuffer pSurface = memAllocPointer(1);
        int err = callPPPPI(vkCreateWin32SurfaceKHR, data.instance, sci.address(), 0L, pSurface.address0());
        long surface = pSurface.get(0);
        memFree(pSurface);
        sci.free();
        if (err != VK_SUCCESS) {
            throw new SWTException("Calling vkCreateWin32SurfaceKHR failed with error: " + err);
        }
        return surface;
    }

}

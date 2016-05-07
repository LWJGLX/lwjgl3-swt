package org.lwjgl.vulkan.swt;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRWin32Surface.*;
import static org.lwjgl.vulkan.KHRXlibSurface.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VKUtil.*;
import static org.lwjgl.vulkan.swt.VKUtil.*;

import java.nio.ByteBuffer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;

/**
 * Shows how to create a simple Vulkan instance and a {@link VKCanvas}.
 * 
 * @author Kai Burjack
 */
public class SimpleDemo {

	/**
     * Create a Vulkan instance using LWJGL 3.
     * 
     * @return the VkInstance handle
     */
    private static VkInstance createInstance() {
        VkApplicationInfo appInfo = VkApplicationInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                .pApplicationName(memUTF8("SWT Vulkan Demo"))
                .pEngineName(memUTF8(""))
                .apiVersion(VK_MAKE_VERSION(1, 0, 2));
        ByteBuffer VK_KHR_SURFACE_EXTENSION = memUTF8(VK_KHR_SURFACE_EXTENSION_NAME);
        ByteBuffer VK_KHR_OS_SURFACE_EXTENSION;
        if (Platform.get() == Platform.WINDOWS)
            VK_KHR_OS_SURFACE_EXTENSION = memUTF8(VK_KHR_WIN32_SURFACE_EXTENSION_NAME);
        else
            VK_KHR_OS_SURFACE_EXTENSION = memUTF8(VK_KHR_XLIB_SURFACE_EXTENSION_NAME);
        PointerBuffer ppEnabledExtensionNames = memAllocPointer(2);
        ppEnabledExtensionNames.put(VK_KHR_SURFACE_EXTENSION);
        ppEnabledExtensionNames.put(VK_KHR_OS_SURFACE_EXTENSION);
        ppEnabledExtensionNames.flip();
        VkInstanceCreateInfo pCreateInfo = VkInstanceCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                .pNext(0L)
                .pApplicationInfo(appInfo);
        if (ppEnabledExtensionNames.remaining() > 0) {
            pCreateInfo.ppEnabledExtensionNames(ppEnabledExtensionNames);
        }
        PointerBuffer pInstance = MemoryUtil.memAllocPointer(1);
        int err = vkCreateInstance(pCreateInfo, null, pInstance);
        if (err != VK_SUCCESS) {
            throw new RuntimeException("Failed to create VkInstance: " + translateVulkanResult(err));
        }
        long instance = pInstance.get(0);
        memFree(pInstance);
        VkInstance ret = new VkInstance(instance, pCreateInfo);
        memFree(ppEnabledExtensionNames);
        memFree(VK_KHR_OS_SURFACE_EXTENSION);
        memFree(VK_KHR_SURFACE_EXTENSION);
        appInfo.free();
        return ret;
    }

    public static void main(String[] args) {
        // Create the Vulkan instance
        VkInstance instance = createInstance();

        // Create SWT Display, Shell and VKCanvas
        final Display display = new Display();
        final Shell shell = new Shell(display, SWT.SHELL_TRIM | SWT.NO_BACKGROUND | SWT.NO_REDRAW_RESIZE);
        shell.setLayout(new FillLayout());
        shell.addListener(SWT.Traverse, new Listener() {
            public void handleEvent(Event event) {
                switch (event.detail) {
                case SWT.TRAVERSE_ESCAPE:
                    shell.close();
                    event.detail = SWT.TRAVERSE_NONE;
                    event.doit = false;
                    break;
                }
            }
        });
        VKData data = new VKData();
        data.instance = instance; // <- set Vulkan instance
        final VKCanvas canvas = new VKCanvas(shell, SWT.NO_BACKGROUND | SWT.NO_REDRAW_RESIZE, data);
        shell.setSize(800, 600);
        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }
        canvas.dispose();
        display.dispose();
    }

}

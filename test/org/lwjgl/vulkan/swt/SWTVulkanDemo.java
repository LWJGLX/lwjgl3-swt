package org.lwjgl.vulkan.swt;

import static org.lwjgl.system.MemoryUtil.memEncodeASCII;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.vulkan.EXTDebugReport.VK_EXT_DEBUG_REPORT_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRWin32Surface.VK_KHR_WIN32_SURFACE_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRXlibSurface.VK_KHR_XLIB_SURFACE_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.vkCreateInstance;

import java.nio.ByteBuffer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.MemoryUtil.BufferAllocator;
import org.lwjgl.system.Platform;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;

/**
 * Shows how to create a simple Vulkan instance and a {@link VKCanvas}.
 * 
 * @author Kai Burjack
 */
public class SWTVulkanDemo {

    /**
     * Create a Vulkan instance using LWJGL 3.
     * 
     * @return the VkInstance handle
     */
    private static VkInstance createInstance() {
        VkApplicationInfo appInfo = VkApplicationInfo.calloc();
        appInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO);
        appInfo.pApplicationName("SWT Vulkan Demo");
        appInfo.pEngineName("");
        appInfo.apiVersion(VkUtil.VK_MAKE_VERSION(1, 0, 3));
        PointerBuffer ppEnabledExtensionNames = MemoryUtil.memAllocPointer(2);
        ByteBuffer VK_KHR_SURFACE_EXTENSION;
        if (Platform.get() == Platform.WINDOWS)
            VK_KHR_SURFACE_EXTENSION = memEncodeASCII(VK_KHR_WIN32_SURFACE_EXTENSION_NAME, BufferAllocator.MALLOC);
        else
            VK_KHR_SURFACE_EXTENSION = memEncodeASCII(VK_KHR_XLIB_SURFACE_EXTENSION_NAME, BufferAllocator.MALLOC);
        ppEnabledExtensionNames.put(VK_KHR_SURFACE_EXTENSION);
        ByteBuffer VK_EXT_DEBUG_REPORT_EXTENSION = memEncodeASCII(VK_EXT_DEBUG_REPORT_EXTENSION_NAME, BufferAllocator.MALLOC);
        ppEnabledExtensionNames.put(VK_EXT_DEBUG_REPORT_EXTENSION);
        VkInstanceCreateInfo pCreateInfo = VkInstanceCreateInfo.calloc();
        pCreateInfo.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
        pCreateInfo.pNext(0L);
        pCreateInfo.pApplicationInfo(appInfo);
        if (ppEnabledExtensionNames.position() > 0) {
            pCreateInfo.enabledExtensionCount(ppEnabledExtensionNames.position());
            pCreateInfo.ppEnabledExtensionNames(ppEnabledExtensionNames.flip());
        }
        PointerBuffer pInstance = MemoryUtil.memAllocPointer(1);
        int err = vkCreateInstance(pCreateInfo, null, pInstance);
        if (err != VK_SUCCESS) {
            throw new RuntimeException("Failed to create VkInstance: " + VkUtil.translateError(err));
        }
        long instance = pInstance.get(0);
        memFree(pInstance);
        pCreateInfo.free();
        memFree(VK_EXT_DEBUG_REPORT_EXTENSION);
        memFree(VK_KHR_SURFACE_EXTENSION);
        memFree(ppEnabledExtensionNames);
        appInfo.free();
        return new VkInstance(instance);
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

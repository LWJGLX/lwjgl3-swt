package org.lwjgl.vulkan.swt;

import org.eclipse.swt.widgets.Composite;

public interface PlatformVKCanvas {

    int checkStyle(Composite parent, int style);

    void resetStyle(Composite parent);

    long create(Composite composite, VKData data);

}

## What is it?

Revised drop-in implementation of SWT's GLCanvas.

## What does it get me?

Support for:
- creating OpenGL 3.0 and 3.2 core/compatibility contexts (including debug/forward compatible)
- OpenGL ES contexts
- no error contexts
- floating-point and sRGB pixel formats
- multisampled framebuffers (also with different number of color samples - Nvidia only)
- v-sync/swap control
- context flush control
- robust buffer access (with application/share-group isolation)
- sync'ing buffer swaps over multiple windows and cards - Nvidia only
- delay before swap - Nvidia only

## Building with gradle / maven as a dependency

The easiest way to use this library with gradle / maven is to use jitpack.io. Unfortunatly, with the build.gradle present on master the build will always be made for linux when using jitpack.

To fix this problem you need to specify to jitpack which branch of this repo you want to use ([WIN-Gradle](https://github.com/ProtectedVariable/lwjgl3-swt/tree/WIN-Gradle), [OSX-Gradle](https://github.com/ProtectedVariable/lwjgl3-swt/tree/OSX-Gradle), [LINUX-Gradle](https://github.com/ProtectedVariable/lwjgl3-swt/tree/LINUX-Gradle)) to build for each OS.

The Groovy / XML code can be generated from https://jitpack.io/#ProtectedVariable/lwjgl3-swt (check the branch tab)

For example, to use the Windows version:
```Groovy
dependencies {
  implementation 'com.github.ProtectedVariable:lwjgl3-swt:WIN-Gradle-SNAPSHOT'
}
```
## Why does it exist?

The above features have been lacking for some years in the SWT-provided GLCanvas implementation.
The purpose of the new implementation on top of LWJGL 3 is to have full support for those features in an OpenGL SWT application.

## How to use it?

In your existing SWT application just replace all imports of `org.eclipse.swt.opengl.*` with `org.lwjgl.opengl.swt.*`.
The new implementation is a drop-in replacement, which means that your current SWT code should work like before.
To use the new features you can use the new fields in the `GLData` class, such as using real multisampled framebuffers or creating a OpenGL 3.2 core context.

If your current OpenGL SWT setup looks like this:
```Java
Display display = new Display();
Shell shell = new Shell(display);
shell.setLayout(new FillLayout());
GLData data = new GLData();
GLCanvas canvas = new GLCanvas(shell, 0, data);
```
then adding multisampling and using a OpenGL 3.2 core context is as easy as doing:
```Java
Display display = new Display();
Shell shell = new Shell(display);
shell.setLayout(new FillLayout());
GLData data = new GLData();
data.profile = GLData.Profile.CORE;
data.majorVersion = 3;
data.minorVersion = 2;
data.samples = 4; // 4x multisampling
data.swapInterval = 1; // for enabling v-sync (swapbuffers sync'ed to monitor refresh)
GLCanvas canvas = new GLCanvas(shell, 0, data);
```

## Vulkan support

Much like with the GLCanvas/GLData for OpenGL there is now also first exprimental Win32 support for Vulkan:
```Java
Display display = new Display();
Shell shell = new Shell(display);
shell.setLayout(new FillLayout());
VKData data = new VKData();
data.instance = instance; // <- the VkInstance created outside via LWJGL 3
VKCanvas canvas = new VKCanvas(shell, 0, data);
long surface = canvas.surface;
```

## What is planned for the future?

Support for:
- Vulkan
- ~~OS X~~ (Done)
- associating rendering contexts with specific GPUs on Nvidia and AMD
- pbuffers (there are interesting extensions that are only supported for pbuffers, such as EXT_packed_float and NV_video_output)

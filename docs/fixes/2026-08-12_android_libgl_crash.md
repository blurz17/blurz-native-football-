# Fix: Android Runtime Crash (libGL.so.1 Not Found)

**Date:** 2026-08-12
**Phase:** 1 (Foundation)

## The Bug
When launching the APK on Android, the game crashed immediately before opening. 
The ADB logcat showed:
`Error: dlopen failed: library "libGL.so.1" not found: needed by libmain.so`

## Root Cause
The engine uses `gl4es` to translate OpenGL calls to OpenGL ES. 
By default, the `gl4es` CMake script builds itself as a shared library (`libGL.so.1`). Android's Native Development Kit (NDK) and OS do not provide `libGL.so.1`, but `libmain.so` was dynamically linked against it, expecting it to exist at runtime.

## The Fix
We converted `gl4es` to be built statically directly into `libmain.so`.

1. **Static Linking**: Modified `third_party/CMakeLists.txt` to set `STATICLIB ON` for `gl4es`. This bakes the translation layer directly into our main game library.
2. **Dynamic Loader Enabled**: Removed the `NO_LOADER` flag so `gl4es` could dynamically load `libGLESv2.so` and `libEGL.so` on the Android device at runtime using `dlopen`.
3. **EGL Support**: Removed `NOEGL` so `gl4es` could use its bundled EGL headers for initialization.
4. **Link Order**: Updated the root `CMakeLists.txt` to link `dl` (the dynamic loader library on Android) and reordered the linking so `GL` appears before the native `GLESv2` and `EGL` libraries.

## Result
The game successfully builds a standalone `libmain.so` that dynamically locates the device's native OpenGL ES hardware drivers without crashing on startup. 

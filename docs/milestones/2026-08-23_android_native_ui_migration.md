# Android Native UI Migration Guide

This document explains the architecture and the steps taken to replace the legacy C++ based GUI in Gameplay Football with a modern, native Android UI architecture.

## Overview
Previously, the game used an internal C++ rendering system (`Gui2` / `MenuTask`) for all menus, options, and team selection screens. While functional, it lacked the modern aesthetics, animations, and ease of modification that standard Android UI provides.

The new approach bypasses the C++ `MenuTask` entirely for the main application flow. Instead, it uses standard Android Java `Activity` classes (with modern styles) layered **on top** of, or navigated **before**, the primary `SDLActivity` which handles the 3D OpenGL engine.

## New UI Flow
The Java UI follows this general Activity stack:
1. `MainMenuActivity`: The entry point (replacing C++ main menu).
2. `PlayMenuActivity`: Allows choosing between quick match, career, etc.
3. `TeamSelectActivity`: Lets the user pick home and away teams. Passes team IDs as Intent Extras.
4. `MatchOptionsActivity`: Lets the user configure match duration and difficulty.
5. `SDLActivity`: The main native C++ engine.

## Seamless Loading Architecture (The "Ghost Buttons" Fix)
One of the major challenges of replacing the UI was transitioning smoothly from the Java UI (`MatchOptionsActivity`) into the 3D C++ engine (`SDLActivity`) without visual glitches (e.g., black screens or premature control rendering). 

The following solutions were implemented to achieve a flawless loading sequence:

### 1. Preserving the UI Backstack
Previously, activities like `MatchOptionsActivity` used `android:noHistory="true"` in the `AndroidManifest.xml`. This caused Android to destroy the UI the exact millisecond the user clicked "START MATCH" to launch `SDLActivity`. 
- **Fix:** Removed `noHistory="true"`. The `MatchOptionsActivity` now safely stays alive in the background while the match loads. 

### 2. Translucent SDL Window
By default, when `SDLActivity` launches, Android draws a solid black background behind its OpenGL `SurfaceView`. Since the C++ engine (`Match::Match`) blocks the thread for 10-15 seconds while loading stadium geometry, this resulted in a long black screen.
- **Fix:** Applied `@android:style/Theme.Translucent.NoTitleBar.Fullscreen` to `SDLActivity` in `AndroidManifest.xml`. Because the OpenGL `SurfaceView` also uses `PixelFormat.TRANSPARENT`, the `SDLActivity` remains completely see-through until the C++ engine swaps its first 3D frame. This allows the `MatchOptionsActivity` (which displays a spinning BarLoader) to remain fully visible through the transparent `SDLActivity` window!

### 3. JNI-Triggered Virtual Controls
The `MobileControlsView` handles the on-screen joystick and buttons. Because `SDLActivity` is translucent, any Android Views attached to `SDLActivity` (like the joystick) would immediately draw on top of the transparent window (producing "ghost buttons" over the loader).
- **Fix:** `MobileControlsView` is now initialized as `View.GONE` when starting a quick match. The C++ engine contains a JNI hook at the very end of the `Match::Match` constructor:
  ```cpp
  // Inside match.cpp
  jmethodID mid = env->GetMethodID(cls, "setMatchMode", "(Z)V");
  env->CallVoidMethod(activity, mid, true);
  ```
  This JNI call triggers `SDLActivity.setMatchMode(true)`, which then sets the `MobileControlsView` to `View.VISIBLE`. 
  **Result:** The virtual controls magically pop into existence at the exact millisecond the 3D stadium finishes loading and covers the screen.

## How to Build
Always run `build_release.bat` located in `platform/android/` to compile the app correctly with the appropriate Java environment paths and NDK configuration.

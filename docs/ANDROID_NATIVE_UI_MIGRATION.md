# Android Native UI Migration Guide

## Overview
This document outlines the process and architecture for migrating the game's user interface from the old in-engine C++ rendering system to the modern, hardware-accelerated Android Native UI system (Java/XML).

By moving to Android Native UI, we achieved:
- Better performance and battery life (offloading UI rendering from the 3D engine).
- Immediate access to Android system features (input methods, native scrolling, accessibility).
- A much smaller memory footprint.
- Easier iteration for menus and layouts.

## Architecture

### 1. The Activity Flow
The application now uses standard Android Activities instead of rendering different UI scenes inside the C++ engine loop.

**`SplashActivity`**
- The entry point of the game.
- Displays the `blurz_logo` and handles loading any essential initialization data.
- Automatically transitions to `MainMenuActivity`.

**`MainMenuActivity`**
- The core hub for navigating the game modes.
- Implemented using standard Android Layouts (`LinearLayout`, `Button`).
- Launches the `MatchOptionsActivity` when the player decides to start a game.

**`MatchOptionsActivity`**
- Allows the user to select teams, stadiums, and match duration.
- Contains the "Play" button which packages the match configuration into an Android `Intent`.
- Launches `SDLActivity` (the main game engine).

**`SDLActivity`** (The Match Engine)
- The core C++ game engine surface.
- We have completely stripped out the virtual UI buttons and menus from the 3D rendering pipeline.
- The C++ engine now purely focuses on physics and 3D graphics. All pre-match interaction happens in Native Android UI.
- The `Intent` extras passed from `MatchOptionsActivity` are sent to the C++ engine via JNI to configure the match setup.

## Technical Implementation Details

### Stripping the Old C++ UI
Previously, the `Match` system was burdened with rendering UI overlays, virtual buttons, and menu screens. 
- The `RenderOverlay2D` queue in `opengl_renderer3d.cpp` is now only used for essential in-game HUD elements (like scoreboards or player names), rather than full interactive menus.
- The `VirtualButton` classes and heavy UI texture buffers have been stripped or bypassed.

### JNI Communication
To bridge the gap between the Native Android UI and the C++ Engine:
- The Match configuration (Team A, Team B, Time, etc.) is serialized into a string/JSON or individual Intent Extras.
- `SDLActivity.java` reads these extras upon launch.
- Before `SDL_main` initializes the actual gameplay, these parameters are passed down through a custom JNI bridge into the C++ `Match` system.

### Performance Optimizations
Because the UI is now handled by Android's hardware composer, the 3D engine is much lighter. Additionally, we've implemented:
- **FBO Downscaling:** Internal G-Buffers are locked to 720p (1280x720) and upscaled via `glViewport` to the native display, saving massive memory bandwidth.
- **RGBA8 Framebuffers:** Switched accumulation and G-buffers from `RGBA16F` to `RGBA8`, halving memory consumption.
- **Physics Tick Rate:** Capped at 16ms (60 FPS) to prevent runaway CPU simulation overhead.

## How to Add New Native Menus
1. Create a new Java `Activity` (e.g., `SettingsActivity.java`).
2. Create its corresponding layout XML in `res/layout/`.
3. Register the Activity in `AndroidManifest.xml`.
4. Launch it via standard Android Intents from `MainMenuActivity`.
5. If the new menu configures something that affects the 3D engine, add it as an extra to the `Intent` that eventually launches `SDLActivity`.

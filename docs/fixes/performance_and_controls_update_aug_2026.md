# Android Performance & Controls Update (August 2026)

## 1. Performance Optimizations
To achieve smooth, 60 FPS gameplay on mobile (Android) hardware, the following optimizations were made to the rendering pipeline:

- **VSync Enablement (`opengl_renderer3d.cpp`)**: 
  - Enabled `SDL_GL_SetSwapInterval(1)` in the OpenGL context creation. This forces the game to synchronize with the device's display refresh rate (e.g., 60Hz), preventing screen tearing and smoothing out frame delivery.

- **Disabled Shadow Maps (`graphics_task.cpp`)**: 
  - Placed `#ifndef __ANDROID__` guards around the `GraphicsTaskCommand_RenderShadowMaps` and `EnqueueShadowMap` logic. 
  - Generating dynamic shadow maps requires multiple rendering passes, which is too heavy for most mobile GPUs (Mali/Adreno). Disabling this provides a massive performance boost.

- **Post-Process Shader Simplification (`postprocess.frag`)**: 
  - Removed **SSAO** (Screen Space Ambient Occlusion) and **Edge Blur** (Anti-Aliasing) logic. 
  - Both effects required expensive loops that repeatedly sampled the `map_modifier` texture. On mobile, texture lookups in tight fragment shader loops significantly bottleneck performance.

- **Configuration Tuning (`football.config`)**: 
  - Adjusted internal engine variables for lower-end hardware compatibility.

## 2. Controls and UI Fixes

- **Menu Cursor Movement Bug (`MobileControlsView.java`)**: 
  - **Issue**: The menu cursor (D-Pad) would occasionally move in the opposite direction when dragged.
  - **Root Cause**: The raw distance values of the floating D-Pad were being passed directly into SDL's axis state. If the user dragged their thumb past the base radius, values greater than `1.0` were passed in. SDL expects values between `[-1.0, 1.0]`, multiplies them by `32767`, and casts to a signed 16-bit integer (`Sint16`). A value > `1.0` caused an integer overflow (wrapping around to negative numbers), causing opposite movement inputs.
  - **Fix**: Implemented strict `[-1.0, 1.0]` clamping using `Math.max` and `Math.min` before passing axis values to SDL.

- **Player Cursor Visibility (`smalldebugcircle.ase`)**: 
  - **Issue**: The active player indicator (the 3D ring underneath the player) was barely visible.
  - **Root Cause**: The 3D model used `green.png` as its texture, causing it to blend completely into the green pitch grass.
  - **Fix**: Replaced the texture reference in `smalldebugcircle.ase` to use `red.png`, providing high contrast and clear visibility during matches.

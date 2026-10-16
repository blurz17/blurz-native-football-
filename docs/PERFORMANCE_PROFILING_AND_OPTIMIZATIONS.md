# Performance Profiling and Engine Optimizations (Android 1080p 60 FPS)

This document records the profiling architecture, telemetry findings, and optimizations implemented to achieve smooth 60 FPS performance at native Full HD (1080p, 2340x1080) on mobile devices (ARM Mali GPU).

---

## 1. Dynamic & Static Telemetry System

To eliminate guesswork and quantify all performance bottlenecks with exact measurements, an engine-level profiler was created.

### Files Added:
- [`engine/base/profiler.hpp`](file:///E:/MY-PROJECTS/android_native_football/engine/base/profiler.hpp)
- [`engine/base/profiler.cpp`](file:///E:/MY-PROJECTS/android_native_football/engine/base/profiler.cpp)

### Instrumentation Points:
1. **OpenGL Renderer Passes (`r3d_messages.cpp`)**:
   - `lastGeometryMs`: Geometry pass and shadow pass duration.
   - `lastAmbientMs`: SSAO and ambient lighting pass duration.
   - `lastLightMs`: Deferred lighting accumulation pass duration.
   - `lastPostprocessMs`: HDR tone mapping, bloom, and postprocessing.
   - `lastRenderMs`: Total GPU pass duration.
2. **CPU Game Tasks (`gametask.cpp`)**:
   - `lastCpuSkinningMs`: Total CPU vertex skinning time across all active humanoids.
   - `lastPhysicsMs`: ODE match physics and AI update step time.
3. **EGL Presentation (`r3d_messages.hpp`)**:
   - Hooked into `Renderer3DMessage_SwapBuffers::Execute` to track real frame-to-frame presentation delta.

### Telemetry Outputs:
- **In-Game HUD Overlay (`match.cpp`)**:
  Real-time top-left caption color-coded by performance:
  - 🟢 **Green ($\ge 55$ FPS)**: Smooth 60 FPS.
  - 🟡 **Yellow ($35 - 54$ FPS)**: Minor drops.
  - 🔴 **Red ($< 35$ FPS)**: Stutter / bottleneck zone.
  Displays: `FPS [1% Low] | FrameTime (GPU ms, CPU Skin ms)`.
- **Periodic Logcat Logging**: Outputs every 1 second:
  `[PERF] FPS: 60.1 (1%Low: 56.4) | Frame: 16.6ms | GPU: 2.7ms | CPU Skin: 3.2ms | Phys: 2.1ms`
- **Spike Detector**: Triggers automatically whenever a frame takes $> 33.3\text{ ms}$ ($< 30\text{ FPS}$):
  `[PERF_SPIKE] Frame #N took Xms! Details -> GPU: ... CPU Skin: ... Phys: ...`
- **Static Benchmark Report**: On match exit or game shutdown, outputs a statistical summary including min/max FPS, 1% low FPS, 0.1% low FPS, and sub-system averages to `benchmark_report.txt` and logcat.

---

## 2. Telemetry Analysis and Key Findings

Analysis of a live 9,200+ line logcat trace revealed two distinct bottlenecks:

### Finding A: GPU Overhead is Minimal at 1080p Full HD
- **Average GPU Total**: **2.7 ms** (Geometry: 1.3 ms, Lighting: 0.1 ms, Post: 0.3 ms).
- The Mali GPU easily processes native 1080p deferred rendering in under 3 ms per frame thanks to:
  - Switching Android G-buffer attachments (`Albedo`, `Normal`, `Aux`, `Modifier`) from `RGBA16F` to `RGBA8`.
  - Reducing SSAO kernel samples from 16 to 8 and eliminating unneeded depth edge lookups.
  - Replacing the 16-tap postprocess blur with a 4-tap bilinear filter.

### Finding B: Android OS Adaptive Refresh Rate Capping to 30 Hz
- Logcat entries showed `IsHRR=false` and `setFrameRateCategory: no preference (boost timeout)`.
- Without explicit frame rate requests, Android 11+ and Samsung display power managers throttle the `SurfaceView` compositor to **30 Hz** after touch inputs idle, locking swap intervals to 33.3 ms even when the GPU finished in 3 ms.

### Finding C: CPU Skinning Spikes During Crowded Gameplay
- During corner kicks and goalmouth scrambles with 15–22 players close to the camera, CPU skinning jumped from 5 ms up to **20.5 ms**, pushing frame times to ~50 ms (20 FPS).
- Every vertex was needlessly transforming tangent and bitangent vectors for models that do not use normal maps.

---

## 3. Implemented Fixes

### 1. Android Display 60 Hz Lock
- **`platform/android/app/src/main/java/org/libsdl/app/SDLActivity.java`**:
  Requested a $\ge 60\text{ Hz}$ display mode ID and set `WindowManager.LayoutParams.preferredRefreshRate = 60.0f`.
- **`platform/android/app/src/main/java/org/libsdl/app/SDLSurface.java`**:
  Explicitly called `Surface.setFrameRate(60.0f, Surface.FRAME_RATE_COMPATIBILITY_DEFAULT)` in `surfaceCreated()` and `surfaceChanged()`.

### 2. CPU Skinning Optimization (`humanoidbase.cpp`)
- Bypassed tangent and bitangent recalculation during animated matches (only uploaded once on initial load).
- Pre-scaled joint positions outside the per-vertex loop.
- Replaced 1.9M inner-loop `memcpy()` calls with direct 3-float assignments.
- *For detailed mathematical and architectural breakdown, see [`docs/CPU_SKINNING_OPTIMIZATION.md`](file:///E:/MY-PROJECTS/android_native_football/docs/CPU_SKINNING_OPTIMIZATION.md).*

---

## 4. Verification & Testing

To verify the impact of future optimizations:
1. Run `build_fast_release.bat`.
2. Inspect the top-left HUD during gameplay.
3. Filter logcat for `[PERF]` and `[PERF_SPIKE]`:
   ```powershell
   adb logcat -s "PerformanceProfiler:*" "FootballGame:*"
   ```
4. Verify that `Skin:` remains $\le 5\text{ ms}$ and overall FPS stays steady at 60 FPS in Full HD.

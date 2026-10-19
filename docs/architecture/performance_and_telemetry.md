# Performance Telemetry & Profiling Architecture

## 1. Profiler Subsystem Design

To maintain strict performance budgets on mobile hardware (targeting 1080p @ 60 FPS minimum on ARM Mali-G68 / Exynos 1380), the engine includes an integrated telemetry and profiling bus.

```
┌────────────────────────────────────────────────────────┐
│                   Engine Profiler                      │
│   • engine/base/profiler.hpp & profiler.cpp            │
└───────────────────────────┬────────────────────────────┘
                            │ Continuous Metrics Collection
┌───────────────────────────▼────────────────────────────┐
│                  Instrumentation Points                │
│   • Geometry & Lighting Pass Time (GPU ms)             │
│   • CPU Vertex Skinning Time (Humanoid ms)             │
│   • ODE Physics & AI Step Time (Physics ms)            │
│   • eglSwapBuffers Frame-to-Frame Presentation Delta   │
└───────────────────────────┬────────────────────────────┘
                            │ Telemetry Dispatch
┌───────────────────────────▼────────────────────────────┐
│                    Telemetry Outputs                   │
│   1. Real-time In-Game HUD Overlay (Top-Left)          │
│   2. Periodic Android Logcat Summaries (1 Hz)          │
│   3. Automatic Spike Trap Detector (> 33.3 ms)         │
│   4. Benchmark Summary Report on Exit                  │
└────────────────────────────────────────────────────────┘
```

---

## 2. Telemetry Telemetry Breakdown

### 2.1 In-Game HUD Telemetry (`match.cpp`)
Displays live metrics color-coded by performance thresholds:
- 🟢 **Green ($\ge 55$ FPS)**: Fluid target framerate.
- 🟡 **Yellow ($35 - 54$ FPS)**: Performance degradation.
- 🔴 **Red ($< 35$ FPS)**: Critical bottleneck.

**Overlay Format**:
```
FPS: 59.8 [1% Low: 56.2] | Frame: 16.7ms (GPU: 2.8ms, Skin: 3.1ms, Phys: 1.9ms)
```

### 2.2 Periodic Logcat Summaries
Every 1000 milliseconds, the profiler writes a compact log entry:
```
[PERF] FPS: 60.1 (1%Low: 56.4) | Frame: 16.6ms | GPU: 2.7ms | CPU Skin: 3.2ms | Phys: 2.1ms
```

### 2.3 Spike Detector Trap
Whenever any single frame takes longer than $33.3\text{ ms}$ (dropping below 30 FPS), a detailed diagnostic alert is emitted:
```
[PERF_SPIKE] Frame #4120 took 41.2ms! Details -> GPU: 3.1ms | CPU Skin: 18.2ms | Phys: 4.8ms | Other: 15.1ms
```

---

## 3. Telemetry Findings & Performance Optimization Matrix

| Subsystem | Bottleneck Identified | Optimization Implemented | Result |
| :--- | :--- | :--- | :--- |
| **Android Display Compositor** | Throttling to 30 Hz when touches idle | Set `preferredDisplayModeId` and `FRAME_RATE_COMPATIBILITY_FIXED_SOURCE` | Locks 60Hz/120Hz refresh |
| **Engine Scheduler** | Hardcoded 16ms sleep clamp in `scheduler.cpp` | Removed sleep clamp; dynamic delta scheduling | Supports 120 FPS simulation |
| **Humanoid Mesh Init** | $O(N^2)$ distance checks during duplicate vertex search | 3D grid spatial hash (`std::unordered_map`) | Init time cut from 6.5s to 42ms |
| **CPU Vertex Skinning** | Redundant tangent/bitangent calculations | Skipped normal map vector math for meshes without normal maps | Skinning time cut from 20ms to 3.2ms |
| **G-Buffer Memory Bandwidth** | 16-bit half float (`RGBA16F`) textures | Converted attachments to 8-bit (`RGBA8`) | GPU pass takes < 3ms at 1080p |

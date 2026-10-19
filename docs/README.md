# 📚 Android Native Football — Documentation Hub

Welcome to the comprehensive technical documentation for **Android Native Football (GPFB)**.
This documentation follows the [Diátaxis Framework](https://diataxis.fr/) and industry standards for software documentation.

---

## 🧭 Documentation Map (Diátaxis Navigation Grid)

```
                       LEARNING-ORIENTED                     PROBLEM-ORIENTED
                ┌──────────────────────────────┬──────────────────────────────┐
                │          TUTORIALS           │        HOW-TO GUIDES         │
                │                              │                              │
PRACTICAL       │ • Getting Started & Build    │ • Adding 3D Models (glTF/FBX)│
                │ • Training Mode Walkthrough  │ • Creating a New Drill       │
                │                              │ • Blender MCP Automation     │
                ├──────────────────────────────┼──────────────────────────────┤
                │          REFERENCE           │         EXPLANATION          │
                │                              │                              │
THEORETICAL     │ • JNI & C++ API Reference    │ • System Architecture & Flow │
                │ • Controls & HUD Spec        │ • Concurrency & putBuffer    │
                │ • 30-Commit Changelog        │ • SO(2) Camera Math          │
                │ • Architectural Records (ADR)│ • Asset Pipeline & Hashing   │
                └──────────────────────────────┴──────────────────────────────┘
                       INFORMATION-ORIENTED                  UNDERSTANDING-ORIENTED
```

---

## 📑 Complete Document Directory

### 1. 🚀 Tutorials & Quickstart
* **[Build & Installation Guide](guides/build_and_installation.md)**: Toolchain requirements (JDK 17, Android NDK, CMake), compilation instructions, APK deployment via ADB, and logcat telemetry.
* **[Training Mode User Guide](guides/training_mode_user_guide.md)**: How to launch training, navigate drills via the 6-card pause overlay, execute free kicks, and toggle camera perspectives.

### 2. 💡 How-To Guides
* **[Developer & 3D Asset Workflow Guide](guides/developer_and_asset_workflow.md)**: Drop-in glTF/FBX replacement workflow, Blender MCP server setup, and creating custom training scenarios.

### 3. 🏗️ Architecture & Concepts (Explanation)
* **[System Architecture & Overview](architecture/overview.md)**: Platform, Game, and Engine layer decomposition, module ownership, and data flow.
* **[Threading Model & Concurrency](architecture/threading_and_concurrency.md)**: Detailed analysis of the simulation thread (`Thread-1`), worker pool (`Thread-6`), render thread, JNI atomics, and the `putBufferMutex` synchronization.
* **[Touch Input & Camera Systems](architecture/input_and_camera_systems.md)**: 360° virtual joystick, 3rd-person Player Cam, and the $\text{SO}(2)$ camera-relative directional projection math.
* **[Training Mode Subsystem](architecture/training_mode_subsystem.md)**: Decoupled drill manager, center kickoff Free Play setup, defensive wall calculations, and opponent AI suppression.
* **[3D Asset Pipeline & Rendering](architecture/asset_pipeline_and_rendering.md)**: glTF 2.0 / GLB & FBX loaders, `ufbx`, $O(N)$ spatial hash vertex deduplication, CR7 model, and mobile deferred rendering.
* **[Performance Telemetry & Profiling](architecture/performance_and_telemetry.md)**: Real-time HUD telemetry, spike detection traps, and Mali GPU optimizations.

### 4. ⚖️ Architectural Decision Records (ADR)
* **[ADR Index & Guidelines](adr/index.md)**: Catalog of all formal architectural decision records.
  * **[ADR-001](adr/ADR-001_putbuffer_mutex_synchronization.md)**: Thread Synchronization for Render Worker Buffer Swaps (`putBufferMutex`).
  * **[ADR-002](adr/ADR-002_camera_relative_analog_movement_so2.md)**: Camera-Relative Analog Movement Projection in $\text{SO}(2)$.
  * **[ADR-003](adr/ADR-003_training_mode_isolation_and_atomic_queues.md)**: Isolated Training Subsystem and Lock-Free Atomic JNI Command Queues.
  * **[ADR-004](adr/ADR-004_modern_asset_pipeline_gltf_fbx_auto_discovery.md)**: Dual Modern Asset Loaders (glTF 2.0 / FBX) with Transparent Fallback.
  * **[ADR-005](adr/ADR-005_direct_jni_touch_input_pipeline.md)**: Zero-Latency Direct JNI Touch Input Dispatch vs. SDL Event Polling.
  * **[ADR-006](adr/ADR-006_display_refresh_rate_and_scheduler_timing.md)**: Android High Refresh Rate Management and Scheduler Frame Timing.

### 5. 📖 Reference
* **[30-Commit Changelog](CHANGELOG.md)**: Comprehensive, standardized Keep-a-Changelog record of all 30 recent commits (`abf0aef` through `4365f41`).
* **[Native C++ & JNI Bridge API Reference](reference/api_and_jni_reference.md)**: JNI method declarations, bitmask button IDs, camera modes, and training drill enumerations.
* **[Mobile Controls & HUD Specification](reference/controls_and_hud_spec.md)**: UI layout, button offsets, touch deadzones, colors, and typography specs.

---

## 🏛️ Historical Milestones & Fixes
For historical records and legacy post-mortems from earlier engine development phases:
- [Performance & Controls Update (August 2026)](fixes/performance_and_controls_update_aug_2026.md)
- [Mali Invisible Players Investigation](fixes/2026-08-17_mali_invisible_players.md)
- [Android LibGL Crash Investigation](fixes/2026-08-12_android_libgl_crash.md)
- [Android Native UI Migration](milestones/2026-08-23_android_native_ui_migration.md)

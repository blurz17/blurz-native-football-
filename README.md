# Android Native Football (GPFB)

An open-source, high-performance 3D football (soccer) simulation game natively compiled for Android (ARM64) and desktop systems via C++, SDL2, OpenGL ES, and ODE physics.

![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Windows%20%7C%20Linux-green.svg)
![Architecture](https://img.shields.io/badge/Architecture-ARM64--v8a%20%7C%20x86__64-blue.svg)
![Graphics](https://img.shields.io/badge/Graphics-OpenGL%20ES%202.0%20%2F%203.0-orange.svg)
![Standards](https://img.shields.io/badge/Docs-Di%C3%A1taxis%20%7C%20ADR%20%7C%20Keep%20a%20Changelog-blueviolet.svg)

---

## 📖 Table of Contents

- [Overview](#overview)
- [Documentation Hub (Diátaxis)](#-documentation-hub-diátaxis)
- [Key Features](#-key-features)
- [Architecture & Concurrency](#-architecture--concurrency)
- [Building & Installation](#-building--installation)
- [Controls & Gameplay](#-controls--gameplay)
- [Last 30 Commits Summary](#-last-30-commits-summary)

---

## 🌟 Overview

Android Native Football is built on the Gameplay Football engine, overhauled for modern mobile devices with:
* High-performance native C++ core with multi-threaded rendering and physics.
* Comprehensive **Training Mode** subsystem with Free Play, Free Kicks, Corners, and Penalty Shootouts.
* PES Mobile-style touch HUD controls and customizable camera angles.
* Immersive **3rd-Person Player Camera** with 360° camera-relative analog controls.
* High-fidelity 3D player models (CR7 / Al Nassr) and authentic match ball physics.
* Zero-latency direct JNI touch input dispatch and thread-safe buffer synchronization.

---

## 📚 Documentation Hub (Diátaxis)

The project documentation is organized using the **[Diátaxis Framework](https://diataxis.fr/)**. For the master index, visit the **[Master Documentation Hub](docs/README.md)**.

| Category | Document | Description |
| :--- | :--- | :--- |
| **Tutorials** | **[Build & Installation Guide](docs/guides/build_and_installation.md)** | Step-by-step toolchain setup, compiling release APK, and ADB deployment. |
| | **[Training Mode User Guide](docs/guides/training_mode_user_guide.md)** | Walkthrough of all 4 drills, set-piece controls, and pause menu navigation. |
| **How-To** | **[Developer & 3D Asset Workflow](docs/guides/developer_and_asset_workflow.md)** | Adding 3D models (glTF/FBX), kits, Blender MCP automation, and new drills. |
| **Architecture** | **[System Overview](docs/architecture/overview.md)** | Three-tier architecture, module ownership, and data flow. |
| | **[Threading & Concurrency](docs/architecture/threading_and_concurrency.md)** | Game thread, worker pool (`Thread-6`), JNI atomics, and `putBufferMutex`. |
| | **[Touch Input & Camera Systems](docs/architecture/input_and_camera_systems.md)** | 360° virtual joystick, 3rd-person cam, and $\text{SO}(2)$ projection math. |
| | **[Training Mode Subsystem](docs/architecture/training_mode_subsystem.md)** | Decoupled drill manager, center kickoff, and opponent AI suppression. |
| | **[3D Asset Pipeline & Rendering](docs/architecture/asset_pipeline_and_rendering.md)** | glTF 2.0 / FBX loaders, $O(N)$ spatial hashing, CR7 model, and G-Buffer. |
| | **[Performance & Telemetry](docs/architecture/performance_and_telemetry.md)** | In-game profiler, HUD overlay, spike traps, and Mali GPU optimizations. |
| **Decisions (ADR)** | **[Architectural Decision Records Index](docs/adr/index.md)** | Catalog of 6 formal ADRs (ADR-001 through ADR-006). |
| **Reference** | **[30-Commit Changelog](docs/CHANGELOG.md)** | Exhaustive Keep-a-Changelog record of commits `abf0aef` through `4365f41`. |
| | **[Native C++ & JNI API Reference](docs/reference/api_and_jni_reference.md)** | JNI bridge functions, touch input signatures, and game enumerations. |
| | **[Mobile Controls & HUD Specification](docs/reference/controls_and_hud_spec.md)** | Visual layout, button coordinates, touch deadzones, and typography. |

---

## ⚡ Key Features

### 1. Dedicated Training Mode
* **Free Play (Solo Practice)**: Starts at the center circle $(0, 0, 0.11)$ with a stationary ball and full 11-player tactical team deployment. Opponent outfielders are parked off the pitch, and the goalkeeper guards the line without aggressively rushing out.
* **Direct Free Kicks**: 4 realistic shooting variations (Center 24m, Left Angle 23m, Right Angle 23m, Long Range 32m) against a 3-man defensive wall.
* **Corner Kicks**: Cross deliveries from left and right corners with aerial heading opportunities.
* **Penalty Shootout**: 1v1 spot kicks from 11 meters against the goalkeeper.
* **PES Mobile Training Menu**: Dedicated in-game overlay to swap drills, toggle variations, switch camera views, or instantly reset the scenario.

### 2. Player Camera (3rd-Person 3D View)
* Dynamic over-the-shoulder tracking following the active possession player.
* Set-piece behind-the-kicker camera focusing on defensive walls and goal targets.
* **$\text{SO}(2)$ Camera-Relative Controls**: Analog stick swiping directly translates to screen-relative movement (Up = Forward, Down = Backward, Left = Left, Right = Right).

### 3. Stability & Thread Safety
* Complete synchronization between physics/gameplay thread and render worker thread (`Thread-6`).
* Thread-safe `putBufferMutex` preventing race conditions during animation buffer swaps.
* Lock-free atomic command queue decoupling Android UI JNI commands from native C++ loops.

---

## 🛠️ Building & Installation

### Prerequisites
* **Android SDK & NDK** (NDK r25+ recommended, e.g. Unity OpenJDK & NDK).
* **CMake** 3.22+.
* **Java 17+**.

### Build Command (Windows PowerShell)
```powershell
$env:JAVA_HOME = "D:\unity\6000.4.2f1\Editor\Data\PlaybackEngines\AndroidPlayer\OpenJDK"
$env:ANDROID_NDK_HOME = "D:\unity\6000.4.2f1\Editor\Data\PlaybackEngines\AndroidPlayer\NDK"
cd platform\android
.\gradlew.bat assembleRelease
```

The output release APK is generated at:
* `platform/android/app/build/outputs/apk/release/app-release.apk`
* Mirrored in project root: `app-release.apk`

### Installing via ADB
```bash
adb install -r app-release.apk
```

---

## 🎮 Controls & Gameplay

### Touch Controls (PES Mobile Style)
* **Left Screen**: Floating 360° virtual analog stick (continuous analog magnitude & direction).
* **Right Screen**: Action buttons:
  * **Short Pass / Pressure**: Low ground pass to targeted teammate or defensive pressure.
  * **Through Ball**: Angled through-pass into space.
  * **Shoot**: Power-meter shot towards the goal corners.
  * **Sprint**: Accelerate to maximum velocity.
  * **Player Switch**: Semi-assisted or manual player switching.
* **Top Right**: Pause / Menu button and Camera toggle button.

---

## 🕒 Last 30 Commits Summary

For the full detailed chronicle with problem statements, root causes, and diffs, see **[`docs/CHANGELOG.md`](docs/CHANGELOG.md)**.

```
4365f41  feat: complete training mode, 3rd-person camera-relative controls, thread safety fixes, and docs
a3b5247  feat(camera): center kickoff angle, configure 3 presets, and support single-player close-up zoom
7d7910e  feat(player, ball): add realistic CR7 3D model, Al Nassr kit, and Adidas Al Rihla ball with glTF
0eabd45  fix(perf): eliminate 16ms graphics delay on Android, force config sync, prevent 60Hz mode lock
6eb6058  fix(refresh_rate, scheduler): unlock 120Hz display modes, fix scheduler dueEntry refresh, add swap
a728c30  fix(player, perf): eliminate 16ms scheduler sleep cap, restore stable software deformation
80a622e  fix(pipeline, perf): unlock 120Hz display, enable smooth FBX players and inlined GPU skinning
df83504  feat(graphics, humanoid): implement Hardware GPU Vertex Skinning with static VBOs
44005e7  fix(fbxloader, profiler): fix inverted UV coords causing black kits and stabilize 1% low FPS
bb57581  fix(fbxloader): apply node geometry_to_world transform so player vertices match scene coords
187ace4  fix(loaders): resolve out_of_range in get_file_name and sanitize FBX texture paths
e0fec9a  fix(pipeline): reset hairstyle local origins to head bone and prioritize FBX loading
5fcd6e7  feat(pipeline): modernize 3D player models and hairstyles with Blender MCP, glTF & FBX
82d85f1  feat(pipeline): Modern 3D asset pipeline supporting glTF 2.0 (GLB) and FBX with auto-discovery
c41165b  feat(perf): implement performance telemetry, 60Hz display lock, and CPU skinning optimization
6db7b86  fix(ui): apply exact relative offsets and scale for action buttons and joystick from reference
42ed750  fix(ui): adjust joystick cursor, remove moving line, and fix action button positions and spacing
37902f6  feat(ui): redesign in-match gameplay HUD to match EA Sports FC Mobile style
f31a36e  feat(ui): balance pause menu card proportions, scale icons and typography responsively
31d1e36  feat(ui): refine compact 6-card pause grid with Replay and authentic PES white sheet sub-screens
06b3a9a  feat(ui): refine PES 5-card pause grid layout, touch handling, and authentic live telemetry
c15bde1  feat(ui): complete PES 5-card settings grid, team crests, visual options, replay controls
7abf05f  feat(ui): implement PES Mobile style pause menu and fix engine freeze and camera settings
51fa632  fix(gameplay): prevent stop/restart hiccup during dash/sprint
36e89fe  fix(gameplay): fix ball loss during turns and sprint dribbling
f01917b  Fix dribble ball retention: dynamic possession window, turf-glued ball control, prevent autoBias
7898447  Fix touch input pipeline: disable accelerometer hint, direct JNI axis update, 360-degree analog
fbeea89  Revert "Fix analog stick determinism, goalkeeper pass aiming, and modernize HUD with clean UI"
20bdf23  Fix analog stick determinism, goalkeeper pass aiming, and modernize HUD with clean minimalist UI
abf0aef  fix(controls): resolve goalkeeper pass right-bias and dribble movement interruption
```

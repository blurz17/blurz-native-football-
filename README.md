# Android Native Football (GPFB)

An open-source, high-performance 3D football (soccer) simulation game natively compiled for Android (ARM64) and desktop systems via C++, SDL2, OpenGL ES, and ODE physics.

![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Windows%20%7C%20Linux-green.svg)
![Architecture](https://img.shields.io/badge/Architecture-ARM64--v8a%20%7C%20x86__64-blue.svg)
![Graphics](https://img.shields.io/badge/Graphics-OpenGL%20ES%202.0%20%2F%203.0-orange.svg)

---

## 📖 Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Documentation Index](#documentation-index)
- [Project Architecture](#project-architecture)
- [Building & Installation](#building--installation)
- [Controls & Gameplay](#controls--gameplay)
- [Directory Structure](#directory-structure)

---

## 🌟 Overview

Android Native Football is built on the Gameplay Football engine, overhauled for modern mobile devices with:
* High-performance native C++ core with multi-threaded rendering and physics.
* Comprehensive **Training Mode** subsystem with Free Play, Free Kicks, Corners, and Penalty Shootouts.
* PES Mobile-style touch HUD controls and customizable camera angles.
* Immersive **3rd-Person Player Camera** with 360° camera-relative analog controls.
* High-fidelity 3D player models (CR7 / Al Nassr) and authentic match ball physics.

---

## 📚 Documentation Index

For in-depth technical documentation, refer to the following guides in the [`docs/`](docs/) directory:

| Document | Description |
| :--- | :--- |
| **[Architecture & Systems](docs/ARCHITECTURE.md)** | Multi-threaded game loop, memory buffering, thread synchronization (`putBufferMutex`), and physics pipeline. |
| **[Training Mode Subsystem](docs/TRAINING_MODE.md)** | Decoupled training manager, drill mechanics, center kick-off Free Play, AI suppression, and UI integration. |
| **[Camera & Control Systems](docs/CAMERA_AND_CONTROLS.md)** | Broadcast camera presets, 3rd-person Player Cam, and SO(2) camera-relative joystick coordinate transformation math. |
| **[Changelog & History](docs/CHANGELOG.md)** | Comprehensive log of all updates, optimizations, bug fixes, and multi-threaded crash resolutions. |

---

## ⚡ Key Features

### 1. Dedicated Training Mode
* **Free Play (Solo Practice)**: Starts at the center circle with a stationary ball and full 11-player tactical team deployment. Opponent outfielders are parked off the pitch, and the goalkeeper guards the line without aggressively rushing out.
* **Direct Free Kicks**: 4 realistic shooting variations (Center 24m, Left Angle 23m, Right Angle 23m, Long Range 32m) against a 3-man defensive wall.
* **Corner Kicks**: Cross deliveries from left and right corners with aerial heading opportunities.
* **Penalty Shootout**: 1v1 spot kicks from 11 meters against the goalkeeper.
* **PES Mobile Training Menu**: Dedicated in-game overlay to swap drills, toggle variations, switch camera views, or instantly reset the scenario.

### 2. Player Camera (3rd-Person 3D View)
* Dynamic over-the-shoulder tracking following the active possession player.
* Set-piece behind-the-kicker camera focusing on defensive walls and goal targets.
* **Camera-Relative Controls**: Analog stick swiping directly translates to screen-relative movement (Up = Forward, Down = Backward, Left = Left, Right = Right).

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

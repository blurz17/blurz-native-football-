# System Architecture & Technical Overview

## 1. Architectural Philosophy
Android Native Football (GPFB) is designed with three core architectural pillars:
1. **Native Performance**: Pure C++ core utilizing SDL2, OpenGL ES 2.0/3.0, and Open Dynamics Engine (ODE) for physics.
2. **Modular Decoupling**: Game modes (Match vs. Training), input pipelines, asset loaders, and UI layers are isolated through clean interfaces and asynchronous boundaries.
3. **Multi-Threaded Concurrency**: Separate threads for simulation, skeletal animation interpolation, rendering, and platform UI events, synchronized via lock-free atomics and scoped mutexes.

---

## 2. High-Level Subsystem Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Android Platform & UI Layer                      │
│   • SDLActivity (Display Modes, Lifecycle, Asset Extraction)          │
│   • MobileControlsView (Touch Joystick, Action Buttons, Pause Menus)   │
│   • MainMenuActivity (Game Mode Selector, Drill Selector)             │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Direct JNI Bridge (Atomic Queues)
┌───────────────────────────────────▼────────────────────────────────────┐
│                             Game Simulation                             │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │ Match Orchestrator (Match Time, Camera, Set-pieces, Rules)     │   │
│   ├────────────────────────────────────────────────────────────────┤   │
│   │ Training Subsystem (TrainingManager, FreePlay, Drills, AI Suppr)│  │
│   ├────────────────────────────────────────────────────────────────┤   │
│   │ Player & AI Subsystem (Humanoid, ElizaController, TeamAI)      │   │
│   └────────────────────────────────────────────────────────────────┘   │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Thread-safe Buffer Swaps (putBufferMutex)
┌───────────────────────────────────▼────────────────────────────────────┐
│                             Engine Subsystems                           │
│   • GameTask & Scheduler (Multi-threaded task graph)                  │
│   • Skeletal Animation Worker Pool (Thread-6 / PutPhase)               │
│   • OpenGL ES Deferred Renderer (G-Buffer, Lighting, Postprocess)     │
│   • Asset Loaders (GLTFLoader, FBXLoader, ASELoader, Auto-Discovery)   │
│   • Physics Engine (ODE Ball & Collision Geometry)                    │
│   • Engine Profiler & Telemetry Bus                                    │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Directory Layout & Module Ownership

| Directory | Subsystem / Responsibility | Key Classes / Files |
| :--- | :--- | :--- |
| `platform/android/` | Android native project, SDL activity, Java UI, touch controls | `SDLActivity.java`, `MobileControlsView.java` |
| `game/` | Football simulation, rules, players, AI, match orchestration | `match.cpp`, `team.cpp`, `referee.cpp` |
| `game/training/` | Isolated training mode manager, drill mechanics, AI suppression | `training_manager.cpp`, `training_mode.hpp` |
| `game/onthepitch/player/` | Player humanoid physics, skeletal deformation, controller input | `humanoid.cpp`, `humanoidbase.cpp`, `humancontroller.cpp` |
| `engine/` | Core engine, task scheduling, math, memory, resource caches | `blunted.cpp`, `scheduler.cpp`, `geometry.cpp` |
| `engine/loaders/` | 3D mesh loaders for glTF 2.0 (GLB), Autodesk FBX, and 3ds Max ASE | `gltfloader.cpp`, `fbxloader.cpp`, `aseloader.cpp` |
| `engine/systems/graphics/` | OpenGL ES deferred rendering, shaders, textures, materials | `opengl_renderer3d.cpp`, `graphics_geometry.cpp` |
| `engine/managers/` | Singleton managers for input, resources, and scenes | `usereventmanager.cpp`, `resourcemanager.hpp` |
| `assets/media/` | Shaders, 3D meshes (GLB, FBX), textures (PNG, JPG), audio | `objects/players/`, `objects/balls/`, `shaders/` |
| `tools/` | Asset conversion utilities and Blender MCP automation scripts | `convert_ase_to_glb.py`, `start_blender_mcp.py` |

---

## 4. Key Data Flows

### A. Touch Input Flow
1. User touches the screen in `MobileControlsView.java`.
2. Java calculates 360° normalized analog vectors and button states.
3. Invokes native JNI methods (`nativeSetMobileDirection`, `nativeSetMobileButton`).
4. Native bridge immediately updates `UserEventManager` in native memory.
5. `HumanController::Process()` samples `UserEventManager`, applies camera-relative transformation, and commands player locomotion.

### B. Frame Simulation & Rendering Pipeline
1. **Game Thread (`Thread-1`)**: Runs `GameTask::ProcessPhase()` at 100 Hz fixed simulation step:
   - Evaluates ball physics and collision with pitch and player bones.
   - Evaluates AI decisions or human input.
   - Updates player animation state machines.
   - Calls `HumanoidBase::PreparePutBuffers()` under `putBufferMutex`.
2. **Worker Pool (`Thread-6`)**: Runs `GameTask::PutPhase()`:
   - Calls `HumanoidBase::FetchPutBuffers()` under `putBufferMutex`.
   - Interpolates skeletal node matrices between physics ticks.
   - Calculates vertex offsets and bone deformation.
3. **Render Thread**:
   - Executes G-Buffer geometry pass (`simple.vert`, `simple.frag`).
   - Accumulates deferred lighting and ambient occlusion.
   - Executes tone mapping postprocessing pass (`postprocess.frag`).
   - Presents swap buffer (`eglSwapBuffers`).

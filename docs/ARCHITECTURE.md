# Architecture & Concurrency Model

This document outlines the technical architecture, threading model, memory synchronization, and pipeline of Android Native Football.

---

## 1. System Layers

The codebase is structured into three distinct layers:

```
┌───────────────────────────────────────────────────────────┐
│              Platform / UI Layer (Android / Java)          │
│   SDLActivity, MobileControlsView, MainMenuActivity       │
└─────────────────────────────┬─────────────────────────────┘
                              │ JNI (Thread-safe Atomic Queues)
┌─────────────────────────────▼─────────────────────────────┐
│                       Game Layer                           │
│   Match, Team, Player, Referee, TrainingManager           │
└─────────────────────────────┬─────────────────────────────┘
                              │ Engine API
┌─────────────────────────────▼─────────────────────────────┐
│                      Engine Layer                         │
│   GameTask, Scheduler, HumanoidBase, OpenGLRenderer3D    │
└───────────────────────────────────────────────────────────┘
```

1. **Platform Layer (`platform/android/`)**:
   * Handles touch input, display surfaces, sound output, lifecycle management, and Android native bindings via SDL2.
   * `MobileControlsView.java` renders the PES Mobile HUD, touch analog stick, action buttons, camera switchers, and the dedicated training menu.

2. **Game Layer (`game/`)**:
   * Simulates football rules, player mechanics, AI tactics, set-pieces, ball physics, animations, and the isolated training manager.
   * `Match`: Central orchestrator managing match time, teams, camera positioning, and set-pieces.
   * `TrainingManager` (`game/training/`): Fully decoupled module driving Free Play, Free Kicks, Corners, and Penalties.

3. **Engine Layer (`engine/`)**:
   * Manages multithreaded task scheduling, 3D math (`Vector3`, `Quaternion`), OpenGL ES rendering, and ODE physics integration.

---

## 2. Multi-Threaded Execution Model

The game executes across three primary threads:

```
[ Android UI Thread (JNI) ] ──(Atomics)──► [ Game Main Thread (Thread-1) ]
                                                    │
                                                    │ PreparePutBuffers() [putBufferMutex]
                                                    ▼
                                           [ Render Worker Thread (Thread-6) ]
                                                    │ FetchPutBuffers() [putBufferMutex]
                                                    ▼
                                           [ OpenGL Render Thread ]
```

### 1. Game Main Thread (`ProcessPhase`)
* Executes at fixed intervals (typically 100 Hz / 10ms ticks).
* Evaluates referee decisions, human controller inputs, AI tactical strategies, player physics simulation, and ball movement.
* Updates the active training drills and checks goal conditions.

### 2. Render Worker Pool (`Thread-6` / `PutPhase`)
* Asynchronously fetches visual transformation matrices from physics objects (`HumanoidBase::FetchPutBuffers()`).
* Interpolates skeletal joint transforms and geometry offsets between simulation ticks for ultra-smooth 60+ FPS animation rendering.

### 3. Android UI Thread (JNI)
* Dispatches user touch inputs (analog stick, action buttons, pause menu toggles).
* Communicates with C++ through JNI entry points in `engine/managers/usereventmanager.cpp`.

---

## 3. Concurrency Protection & Crash Resolution

### The Race Condition (`SIGSEGV` in `__assign_multi`)
* **Symptom**:
  ```
  signal 11 (SIGSEGV), code 1 (SEGV_MAPERR), fault addr 0x0000000000000000
  backtrace:
    #00 ... std::__ndk1::__tree<...>::__assign_multi
    #01 ... HumanoidBase::FetchPutBuffers(unsigned long)+72
    #02 ... GameTask::PutPhase()
  ```
* **Root Cause**:
  `HumanoidBase::buf_animApplyBuffer` contains an internal red-black tree (`std::map<std::string, BiasedOffset>`). When JNI or the match loop called `ResetSituation()` or `PreparePutBuffers()` on the game thread, `buf_animApplyBuffer` was being written to concurrently while the worker thread (`Thread-6`) was copying it via `FetchPutBuffers()`:
  ```cpp
  fetchedbuf_animApplyBuffer = buf_animApplyBuffer; // Data race on std::map tree nodes
  ```
  This concurrent read/write corrupted internal tree pointers, leading to a null pointer dereference.

### The Solution: `putBufferMutex` & Safe Copy Assignment
1. **Mutex Synchronization**:
   Added a dedicated `boost::mutex putBufferMutex;` to [`HumanoidBase`](file:///e:/MY-PROJECTS/android%20native%20football/game/onthepitch/player/humanoid/humanoidbase.hpp).
   * Writes in `PreparePutBuffers()` are protected under `boost::mutex::scoped_lock lock(putBufferMutex);`.
   * Reads in `FetchPutBuffers()` are protected under `boost::mutex::scoped_lock lock(putBufferMutex);`.
   * Resets in `ResetSituation()` are protected under `boost::mutex::scoped_lock lock(putBufferMutex);`.

2. **Safe Assignment Operator**:
   Defined an explicit, safe copy assignment operator `AnimApplyBuffer::operator=` ensuring self-assignment checks and atomic buffer pointer validation.

3. **Atomic JNI Queuing in `TrainingManager`**:
   All asynchronous drill changes, resets, and camera toggles from the Java UI thread are buffered using lock-free atomics:
   ```cpp
   std::atomic<int> pendingSetDrill{-1};
   std::atomic<bool> pendingResetRequest{false};
   std::atomic<bool> pendingNextDrill{false};
   std::atomic<bool> pendingPrevDrill{false};
   std::atomic<bool> pendingNextVariation{false};
   ```
   Commands are processed strictly on the game thread inside `Match::Process()` before physics steps occur, eliminating cross-thread state corruption.

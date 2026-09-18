# Step-by-Step Code Changes & Engine Optimization Summary

This document details all structural and performance modifications made to resolve frame pacing, thread starvation, and rubber-banding issues in the **Blurz Football** engine on Android.

---

## 1. Disabling Audio System (Crash Prevention)
* **File:** [main.cpp](file:///E:/MY-PROJECTS/android_native_football/engine/main.cpp)
* **Rationale:** On Android devices without a full OpenAL spatial audio backend, initializing `AudioSystem` resulted in a `SIGSEGV` native crash.
* **Code Modification:**
```cpp
// Commented out audio system initialization
// audioSystem->Initialize();
```

---

## 2. Enabling VSync Frame Pacing
* **File:** [opengl_renderer3d.cpp](file:///E:/MY-PROJECTS/android_native_football/engine/systems/graphics/rendering/opengl_renderer3d.cpp)
* **Rationale:** Without VSync, Android graphics buffers swap asynchronously without screen cadence, resulting in tearing and micro-stuttering.
* **Code Modification:**
```cpp
// Enable VSync (1 = sync to display refresh rate)
SDL_GL_SetSwapInterval(1);
```

---

## 3. Resolving VSync Thread Starvation
* **File:** [scheduler.cpp](file:///E:/MY-PROJECTS/android_native_football/engine/framework/scheduler.cpp)
* **Rationale:** When VSync is enabled, the graphics thread blocks waiting for display refresh. Unbounded tasks (`sequenceTime = 0`) built up negative priority debt (e.g. `-16.6ms`), causing the scheduler to prioritize rendering repeatedly over physics.
* **Code Modification:**
```cpp
if (program->taskSequence->GetSkippable()) {
  // Unbounded skippable sequences (e.g. graphics) should not build negative priority debt
  if (program->taskSequence->GetSequenceTime() == 0) {
    timeUntilDueEntry_ms = 0;
  } else {
    timeUntilDueEntry_ms = (program->sequenceStartTime + program->taskSequence->GetSequenceTime()) - time_ms;
  }
}
```

---

## 4. Fixing Rubber-Banding Frame Drops
* **File:** [scheduler.cpp](file:///E:/MY-PROJECTS/android_native_football/engine/framework/scheduler.cpp)
* **Rationale:** Shifting universe `startTime` to drop physics debt caused the `TemporalSmoother` interpolation buffer to read from the future, causing players to jump backward in time.
* **Code Modification:**
```cpp
// Fast-forward snapshot counter instead of altering startTime
if (timeUntilDueEntry_ms < -50) {
  int droppedFrames = (-timeUntilDueEntry_ms) / program->taskSequence->GetSequenceTime();
  program->timesRan += droppedFrames;
  timeUntilDueEntry_ms += droppedFrames * program->taskSequence->GetSequenceTime();
}
```

---

## 5. Decoupling Main Loop from VSync Blocking (Threading Consistency)
* **File:** [graphics_task.cpp](file:///E:/MY-PROJECTS/android_native_football/engine/systems/graphics/graphics_task.cpp)
* **Rationale:** Synchronously waiting for `swapBuffers->Wait()` blocked the engine's main scheduler thread while waiting for OpenGL VSync. This caused alternating slow periods (blocked waiting for VSync) followed by fast periods (physics catching up).
* **Code Modification:**
```cpp
// Render3D processes messages asynchronously on its own thread.
// Do NOT block the scheduler main thread waiting for VSync!
/*
if (swapBuffers != boost::intrusive_ptr<Renderer3DMessage_SwapBuffers>()) {
  swapBuffers->Wait();
}
*/
```

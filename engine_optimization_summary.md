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

---

## 6. Target 60 FPS Graphics Frame Pacing (Preventing Thermal Throttling & Flooding)
* **File:** [football.config](file:///E:/MY-PROJECTS/android_native_football/platform/android/app/src/main/assets/football.config)
* **Rationale:** Setting `graphics3d_frametime_ms=0` forced the engine scheduler to dispatch graphics frames at 300+ FPS unbounded. On mobile devices, this flooded the graphics message queue and caused immediate thermal throttling (DVFS), which created erratic CPU slowdowns followed by physics catch-up bursts. Changing `graphics3d_frametime_ms=16` paces the rendering to a smooth 60 FPS target (16ms interval).
* **Code Modification:**
```ini
# Optimized for smooth 60fps on Android
graphics3d_frametime_ms=16
```

---

## 7. Replacing OpenGL `glMapBufferRange` Stalls with Asynchronous `glBufferSubData`
* **File:** [opengl_renderer3d.cpp](file:///E:/MY-PROJECTS/android_native_football/engine/systems/graphics/rendering/opengl_renderer3d.cpp)
* **Rationale:** On mobile GPUs (ARM Mali / Qualcomm Adreno), `glMapBufferRange` triggers synchronous CPU-GPU cache flushes and pipeline stalls. Switching to `glBufferSubData` allows mobile GPU drivers to stream dynamic vertex updates asynchronously without memory barriers.
* **Code Modification:**
```cpp
#ifdef __ANDROID__
// Fast asynchronous buffer upload on Mobile GPUs (avoids glMapBufferRange CPU-GPU sync stalls)
mapping.glBufferSubData(GL_ARRAY_BUFFER, 0, verticesDataSize * sizeof(float), vertices);
#else
mapping.glBufferData(GL_ARRAY_BUFFER, verticesDataSize * sizeof(float), NULL, GL_DYNAMIC_DRAW);
float *ptr = (float*)mapping.glMapBufferRange(GL_ARRAY_BUFFER, 0, verticesDataSize * sizeof(float), GL_MAP_WRITE_BIT | GL_MAP_UNSYNCHRONIZED_BIT);
memcpy(ptr, vertices, verticesDataSize * sizeof(float));
mapping.glUnmapBuffer(GL_ARRAY_BUFFER);
#endif
```

---

## 8. Removing Per-Player VBO Update Wait Locks (22x Frame Stall Elimination)
* **File:** [vertexbuffer.cpp](file:///E:/MY-PROJECTS/android_native_football/engine/systems/graphics/resources/vertexbuffer.cpp)
* **Rationale:** Every frame, 22 player skeletal animation meshes are updated. Calling `updateVertexBuffer->Wait()` inside `CreateOrUpdateVertexBuffer()` forced the worker threads to synchronously block 22 times PER FRAME waiting for the OpenGL thread to process each VBO update. Removing `Wait()` allows player mesh animation updates to complete asynchronously.
* **Code Modification:**
```cpp
boost::intrusive_ptr<Renderer3DMessage_UpdateVertexBuffer> updateVertexBuffer(new Renderer3DMessage_UpdateVertexBuffer(vertexBufferID, vertices, verticesDataSize));
renderer3D->messageQueue.PushMessage(updateVertexBuffer);
// Commented out synchronous wait to prevent 22 thread stalls per frame
// updateVertexBuffer->Wait();
```



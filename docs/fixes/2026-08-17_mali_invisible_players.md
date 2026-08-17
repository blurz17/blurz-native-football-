# Fix: Invisible Players on Mali / Adreno GPUs (Oppo)

**Date:** 2026-08-17
**Phase:** 1 (Foundation)

## The Bug
When running the game on certain devices (like Oppo with ARM Mali GPUs), the static environment (pitch) and shadows rendered perfectly, but the players themselves were completely invisible.

## Root Cause
The 3D player models in this engine use **CPU Skinning** for skeletal animation. Every frame, the game calculates the new vertex positions for the player meshes on the CPU and uploads them to the GPU.
The OpenGL renderer was using `glMapBufferRange(GL_ARRAY_BUFFER, ..., GL_MAP_UNSYNCHRONIZED_BIT)` combined with `memcpy` to update these vertices dynamically. 
However, on older/stricter mobile GPU drivers (specifically Mali via our `gl4es` translation layer), mapping an orphaned buffer asynchronously like this can fail to sync properly or return an invalid pointer, causing the dynamic vertex data to be completely corrupted or dropped.

Static meshes (like the pitch) were not affected because they were uploaded once during creation using the safer `glBufferSubData`.

## The Fix
We replaced the `glMapBufferRange` block in `engine/framework/systems/graphics/rendering/opengl_renderer3d.cpp::UpdateVertexBuffer()` with a standard `glBufferSubData()` call.

```cpp
// Previous Code:
// float *ptr = (float*)mapping.glMapBufferRange(GL_ARRAY_BUFFER, 0, verticesDataSize * sizeof(float), GL_MAP_WRITE_BIT | GL_MAP_UNSYNCHRONIZED_BIT);
// memcpy(ptr, vertices, verticesDataSize * sizeof(float));
// mapping.glUnmapBuffer(GL_ARRAY_BUFFER);

// New Code:
mapping.glBufferSubData(GL_ARRAY_BUFFER, 0, verticesDataSize * sizeof(float), vertices);
```

This guarantees the vertex buffer is reliably copied from the CPU to the GPU on all mobile chipsets without relying on advanced, asynchronously mapped memory buffers that have historically flaky driver support on Android.

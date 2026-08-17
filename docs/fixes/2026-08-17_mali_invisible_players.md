# Fix: Invisible Players on Mali / Adreno GPUs (Oppo)

**Date:** 2026-08-17
**Phase:** 1 (Foundation)

## The Bug
When running the game on certain devices (like Oppo with ARM Mali GPUs), the static environment (pitch) and shadows rendered perfectly, but the players themselves were completely invisible.

## The True Root Cause (Invalid `glDrawRangeElements` Bounds)
Initially, it was suspected that `glMapBufferRange` was failing to sync the dynamically updated vertex buffers on the Mali driver. While avoiding `glMapBufferRange` is a good practice for mobile GPUs, the actual cause of the invisible geometry was an improper usage of `glDrawRangeElements`.

In `OpenGLRenderer3D::DrawBufferChunk()`, the draw call was written as:
```cpp
mapping.glDrawRangeElements(GL_TRIANGLES, startIndex, startIndex + count, count, GL_UNSIGNED_INT, BUFFER_OFFSET(startIndex * sizeof(unsigned int)));
```
The OpenGL spec requires the `start` and `end` parameters of `glDrawRangeElements` to represent the **minimum** and **maximum** vertex indices fetched from the array. However, the engine was passing `startIndex` (the offset into the index buffer) as the minimum, and `startIndex + count` as the maximum. 

For static meshes like the pitch, the vertex indices perfectly aligned sequentially with the index buffer, so the bounds were coincidentally correct.
For animated, CPU-skinned meshes like the players, the index buffer referenced vertices randomly throughout the VBO. 

The Samsung Adreno driver ignored the invalid `start/end` bounds and processed all required vertices anyway. The ARM Mali driver strictly enforced these bounds, dropped the out-of-bounds vertices, and rendered the players completely invisible.

## The Fix
We replaced the `glDrawRangeElements` call with the standard, safer `glDrawElements` call, which does not require specifying min/max index bounds.

```cpp
// Previous Code:
// mapping.glDrawRangeElements(GL_TRIANGLES, startIndex, startIndex + count, count, GL_UNSIGNED_INT, BUFFER_OFFSET(startIndex * sizeof(unsigned int)));

// New Code:
mapping.glDrawElements(GL_TRIANGLES, count, GL_UNSIGNED_INT, BUFFER_OFFSET(startIndex * sizeof(unsigned int)));
```
This instantly resolves the invisible geometry bug on all strict OpenGL ES drivers.

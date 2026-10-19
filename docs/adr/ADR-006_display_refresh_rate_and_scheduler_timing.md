# ADR-006: Android High Refresh Rate Management and Scheduler Frame Timing

## Status
**Accepted** (2026-10-17)

## Context
During telemetry profiling on modern Android devices with 90Hz and 120Hz AMOLED displays (e.g. Samsung Galaxy A54), the engine exhibited unexpected frame-rate throttling down to 30 FPS.

### Root Causes
1. **Android OS Power Management (Adaptive Refresh Rate Throttling)**:
   - When no touch events occurred or when no explicit display mode was requested, Android's `SurfaceControl` downgraded the display to 30 Hz to save power (`IsHRR=false`).
2. **Hardcoded Engine Scheduler Sleep**:
   - `scheduler.cpp` contained a legacy 16ms sleep clamp designed for 60Hz desktop displays, capping simulation ticks and creating artificial wait intervals.
3. **Android EGL Sync Delays**:
   - Explicit `eglWaitGL` or delays in the presentation layer blocked worker threads.

## Decision Drivers
1. **Fluid Frame Rates**: Guarantee solid 60 FPS minimum, with 90Hz/120Hz capability on supported hardware.
2. **Prevent Compositor Throttling**: Signal Android Choreographer and Window Manager that continuous high-frequency rendering is required.

## Decision Outcome
1. **Display Mode Request in `SDLActivity.java`**:
   - Queried display supported modes (`Display.getSupportedModes()`) to select the highest available refresh rate (up to 120Hz).
   - Set `window.getAttributes().preferredDisplayModeId` and requested `Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE`.
2. **Removed Scheduler Sleep Cap (`scheduler.cpp`)**:
   - Removed the hardcoded 16ms minimum sleep clamp, allowing task scheduling based on exact high-resolution timestamps.
3. **Optimized G-Buffer Attachments for Mobile Mali GPU**:
   - Switched deferred render attachments from 16-bit float (`RGBA16F`) to 8-bit (`RGBA8`), cutting memory bandwidth in half.
   - Reduced SSAO samples from 16 to 8.

## Consequences
### Positive
- Eliminated 30 Hz throttling; games maintain rock-solid 60 FPS with smooth 90Hz/120Hz headroom.
- Mali GPU deferred rendering pass finishes in under 3 ms at native 1080p.

### Negative / Trade-offs
- Higher battery consumption at 120Hz compared to 60Hz.

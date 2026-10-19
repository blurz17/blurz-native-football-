# ADR-005: Zero-Latency Direct JNI Touch Input Dispatch vs. SDL Event Polling

## Status
**Accepted** (2026-10-09)

## Context
Mobile touch input initially flowed through SDL2's default Android event queue:
```
Android onTouchEvent() -> SDL_SendTouch() -> SDL Event Queue -> Engine PollEvent() -> UserEventManager
```
This legacy pipeline suffered from critical flaws:
1. **Frame Latency**: Inputs experienced up to 2 frames (16–33 ms) of queuing latency.
2. **Directional Quantization**: Direction inputs were mapped to standard 8-way digital joypad hats, destroying fine 360° analog curve control.
3. **Ghost Accelerometer Interference**: SDL's accelerometer emulation injected unwanted tilt inputs into joystick axes.

## Decision Drivers
1. **Zero Input Lag**: Ultra-responsive response required for football dribbling and shooting.
2. **Continuous 360° Floating Precision**: Analog magnitude ($0.0 \dots 1.0$) and angle must be preserved with full float precision.
3. **Elimination of Spurious Inputs**: Remove accelerometer noise completely.

## Decision Outcome
1. **Direct JNI Method Invocation**:
   Bypassed the SDL event queue for joystick and button events:
   [`MobileControlsView.java`](file:///e:/MY-PROJECTS/android%20native%20football/platform/android/app/src/main/java/org/libsdl/app/MobileControlsView.java) calls native C++ directly:
   ```java
   nativeSetMobileDirection(mCurrentAxisX, mCurrentAxisY);
   nativeSetMobileButton(eButtonId, isDown);
   ```
2. **Native Entry Points in `UserEventManager`**:
   [`engine/managers/usereventmanager.cpp`](file:///e:/MY-PROJECTS/android%20native%20football/engine/managers/usereventmanager.cpp):
   ```cpp
   extern "C" JNIEXPORT void JNICALL
   Java_org_libsdl_app_MobileControlsView_nativeSetMobileDirection(JNIEnv* env, jclass clazz, jfloat x, jfloat y) {
       UserEventManager::GetInstance().SetMobileDirection(x, y);
   }
   ```
3. **Dynamic Floating Joystick Base**:
   The virtual joystick dynamically repositions its base to the user's initial touch-down coordinate on the left half of the screen, tracking thumb movement without requiring fixed screen anchors.
4. **Disabled Accelerometer Hints**:
   Set `SDL_SetHint(SDL_HINT_ACCELEROMETER_AS_JOYSTICK, "0")` to block sensor noise.

## Consequences
### Positive
- Sub-millisecond touch input latency directly feeding the simulation tick.
- Smooth, continuous 360° steering.
- Total elimination of phantom tilt movements.

### Negative / Trade-offs
- Bypasses standard SDL input abstraction, but Android mobile controls require platform-specific tuning anyway.

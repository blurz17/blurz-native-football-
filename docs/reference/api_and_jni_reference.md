# Native C++ & JNI Bridge API Reference

This document provides a technical reference for all native C++ entry points, JNI bridge methods, and input flags connecting Android Java UI to the native engine.

---

## 1. Native JNI Bridge Functions

The JNI entry points are located in [`engine/managers/usereventmanager.cpp`](file:///e:/MY-PROJECTS/android%20native%20football/engine/managers/usereventmanager.cpp) and declared as `native` methods in `org.libsdl.app.MobileControlsView`:

### 1.1 Touch Input & Virtual Joystick

```cpp
extern "C" JNIEXPORT void JNICALL
Java_org_libsdl_app_MobileControlsView_nativeSetMobileDirection(
    JNIEnv* env, jclass clazz, jfloat x, jfloat y
);
```
- **Parameters**:
  - `x`: Horizontal axis from $-1.0$ (Full Left) to $+1.0$ (Full Right).
  - `y`: Vertical axis from $-1.0$ (Full Down) to $+1.0$ (Full Up).
- **Behavior**: Writes directly into `UserEventManager::mobileDirectionX` and `mobileDirectionY` with sub-millisecond latency.

---

### 1.2 Action Buttons

```cpp
extern "C" JNIEXPORT void JNICALL
Java_org_libsdl_app_MobileControlsView_nativeSetMobileButton(
    JNIEnv* env, jclass clazz, jint buttonId, jboolean isDown
);
```
- **Button IDs (`e_ButtonID`)**:
  - `0`: Pass / Pressure (`e_ButtonFunction_ShortPass`)
  - `1`: High Pass / Slide Tackle (`e_ButtonFunction_HighPass`)
  - `2`: Shoot / Clearance (`e_ButtonFunction_Shot`)
  - `3`: Through Ball / Keeper Rush (`e_ButtonFunction_ThroughPass`)
  - `4`: Dash / Sprint (`e_ButtonFunction_Dash`)
  - `5`: Player Switch (`e_ButtonFunction_PlayerSwitch`)

---

### 1.3 Training Mode Controls

```cpp
extern "C" JNIEXPORT void JNICALL
Java_org_libsdl_app_MobileControlsView_nativeTrainingSetDrill(
    JNIEnv* env, jclass clazz, jint drillId
);

extern "C" JNIEXPORT void JNICALL
Java_org_libsdl_app_MobileControlsView_nativeTrainingResetDrill(
    JNIEnv* env, jclass clazz
);

extern "C" JNIEXPORT void JNICALL
Java_org_libsdl_app_MobileControlsView_nativeTrainingNextDrill(
    JNIEnv* env, jclass clazz
);

extern "C" JNIEXPORT void JNICALL
Java_org_libsdl_app_MobileControlsView_nativeTrainingPrevDrill(
    JNIEnv* env, jclass clazz
);

extern "C" JNIEXPORT void JNICALL
Java_org_libsdl_app_MobileControlsView_nativeTrainingNextVariation(
    JNIEnv* env, jclass clazz
);
```
- **Parameters**:
  - `drillId`: Integer mapping to `e_TrainingDrill`:
    - `0`: Free Play
    - `1`: Free Kick
    - `2`: Corner Kick
    - `3`: Penalty Shootout
- **Behavior**: Queued atomically in `TrainingManager` via `std::atomic` to prevent cross-thread race conditions.

---

### 1.4 Camera Controls

```cpp
extern "C" JNIEXPORT void JNICALL
Java_org_libsdl_app_MobileControlsView_nativeTogglePlayerCamera(
    JNIEnv* env, jclass clazz
);

extern "C" JNIEXPORT void JNICALL
Java_org_libsdl_app_MobileControlsView_nativeCycleCameraPreset(
    JNIEnv* env, jclass clazz
);
```
- **Behavior**: Toggles between Broadcast Tele and 3rd-Person Player Cam, or cycles broadcast camera presets (Balanced, Tactical, Action).

---

## 2. Core Game Enumerations

### 2.1 Camera Modes (`game/gamedefines.hpp`)
```cpp
enum e_CameraMode {
    e_CameraMode_Normal = 0,    // Broadcast Tele
    e_CameraMode_Player = 1,    // 3rd-Person Player Tracking
    e_CameraMode_Corner = 2,    // Behind Corner Flag
    e_CameraMode_FreeKick = 3,  // Behind Free Kick Taker
    e_CameraMode_Penalty = 4    // Behind Penalty Taker
};
```

### 2.2 Training Drills (`game/training/training_mode.hpp`)
```cpp
enum e_TrainingDrill {
    e_TrainingDrill_FreePlay = 0,
    e_TrainingDrill_FreeKick,
    e_TrainingDrill_CornerKick,
    e_TrainingDrill_Penalty,
    e_TrainingDrill_Count
};
```

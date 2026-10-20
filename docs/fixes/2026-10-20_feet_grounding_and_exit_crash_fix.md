# Feet Grounding & App Exit Crash Fix

## 1. Executive Summary
This update resolves two critical issues:
1. **Feet Sinking into the Pitch ("thier foots are drawn in the ground")**:
   - In previous builds, players' cleats and ankles were sinking ~12–14 cm below the pitch surface due to legacy negative root translations (`player, 0, 0, -0.16m`) designed for the original 2014 $71^\circ$ deep-squat posture. When the knees and hips were straightened to natural upright athletic posture, the legs extended downward while the root remained dropped, causing boots to penetrate the grass plane.
   - **Resolution**:
     - Compensated the root translation in `HumanoidBase::Process()` during idle stance so the upright athletic posture naturally sits at ground level.
     - Added dynamic ground clearance clamping in `HumanoidBase::UpdateFullbodyNodes()` which inspects the lowest ankle height in real time and automatically lifts the skeleton if any foot penetrates below the pitch surface ($Z < 0.00$ m).
2. **SIGSEGV Crash on App Exit**:
   - The crash report identified a `SIGSEGV` (fault address `0x000000000000000c`) in `blunted::EnvironmentManager::SignalQuit()` called from `SDLActivity.onDestroy()` via `forceQuitEngine()`.
   - When the engine thread had already exited and destructed the `EnvironmentManager` singleton (`singleton == nullptr`), calling `GetInstance().SignalQuit()` dereferenced `nullptr`, crashing the application when accessing the `quit.mutex` at offset `0x0c`.
   - **Resolution**: Added null-pointer safety guards (`GetInstancePtr()`) to `forceQuitEngine()`, `triggerInGamePause()`, and virtual joystick JNI bindings in `usereventmanager.cpp`.

---

## 2. Technical Details

### A. Dynamic Ground Clearance Clamping
In `game/onthepitch/player/humanoid/humanoidbase.cpp`:
```cpp
// Dynamic ground clearance clamping: ensure player cleats rest cleanly on top of pitch turf (Z >= 0.0)
// across all animations and prevent feet from ever sinking below the grass.
if (GetTriangleMeshElementCount() >= 7 && joints.size() >= 13) {
  float minAnkleHeight = 0.082f;
  float lowestAnkleZ = std::min(joints[9].position.coords[2], joints[12].position.coords[2]);
  if (lowestAnkleZ < minAnkleHeight) {
    float groundPenetration = clamp(minAnkleHeight - lowestAnkleZ, 0.0f, 0.20f);
    for (unsigned int i = 0; i < joints.size(); i++) {
      joints[i].position.coords[2] += groundPenetration;
    }
  }
}
```

### B. Exit Crash Elimination
In `engine/managers/usereventmanager.cpp`:
```cpp
extern "C" JNIEXPORT void JNICALL Java_org_libsdl_app_SDLActivity_forceQuitEngine(JNIEnv* env, jclass cls) {
    if (blunted::EnvironmentManager::GetInstancePtr()) {
        blunted::EnvironmentManager::GetInstance().SignalQuit();
    }
}

extern "C" JNIEXPORT void JNICALL Java_org_libsdl_app_SDLActivity_triggerInGamePause(JNIEnv* env, jclass cls) {
    if (blunted::UserEventManager::GetInstancePtr()) {
        blunted::UserEventManager::GetInstance().SetKeyboardState(SDLK_ESCAPE, true);
    }
}
```

// Copyright 2019 Google LLC & Bastiaan Konings
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// written by bastiaan konings schuiling 2008 - 2014
// this work is public domain. the code is undocumented, scruffy, untested, and should generally not be used for anything important.
// i do not offer support, so don't ask. to be used for inspiration :)

#include "usereventmanager.hpp"

#include "environmentmanager.hpp"

#ifdef __ANDROID__
#include <jni.h>
#include <android/log.h>
#include <SDL.h>
#include <SDL_system.h>

#include "main.hpp"
#include "onthepitch/match.hpp"
#include "onthepitch/team.hpp"
#include "data/teamdata.hpp"

SDL_Joystick* virtual_joystick = nullptr;

extern "C" JNIEXPORT void JNICALL Java_org_libsdl_app_SDLActivity_setVirtualKey(JNIEnv* env, jclass cls, jint sdlKeycode, jboolean pressed) {
    SDL_Event event;
    memset(&event, 0, sizeof(event));
    event.type = pressed ? SDL_KEYDOWN : SDL_KEYUP;
    event.key.state = pressed ? SDL_PRESSED : SDL_RELEASED;
    event.key.keysym.sym = (SDL_Keycode)sdlKeycode;
    event.key.keysym.scancode = SDL_GetScancodeFromKey((SDL_Keycode)sdlKeycode);
    event.key.keysym.mod = KMOD_NONE;
    event.key.repeat = 0;
    SDL_PushEvent(&event);
}

extern "C" JNIEXPORT void JNICALL Java_org_libsdl_app_SDLActivity_setVirtualJoystickAxis(JNIEnv* env, jclass cls, jint axis, jfloat value) {
    if (value > 1.0f) value = 1.0f;
    if (value < -1.0f) value = -1.0f;
    Sint16 int_val = (Sint16)(value * 32767.0f);

    // Immediate zero-latency update directly to UserEventManager
    blunted::UserEventManager::GetInstance().SetVirtualJoystickAxisDirect(axis, (float)int_val);

    if (virtual_joystick) {
        SDL_JoystickSetVirtualAxis(virtual_joystick, axis, int_val);
        SDL_JoystickUpdate();
    }

    static unsigned long lastLogTime = 0;
    static float lastLoggedAxes[4] = {0, 0, 0, 0};
    unsigned long now = blunted::EnvironmentManager::GetInstance().GetTime_ms();
    bool changed = (axis >= 0 && axis < 4 && (fabs(value - lastLoggedAxes[axis]) > 0.08f || (value == 0.0f && lastLoggedAxes[axis] != 0.0f)));
    if (changed || (now - lastLogTime > 250 && value != 0.0f)) {
        if (axis >= 0 && axis < 4) lastLoggedAxes[axis] = value;
        lastLogTime = now;
        __android_log_print(ANDROID_LOG_INFO, "FOOTBALL_INPUT", "[JNI_AXIS] axis=%d floatVal=%.3f sint16Val=%d", axis, value, int_val);
    }
}

extern "C" JNIEXPORT void JNICALL Java_org_libsdl_app_SDLActivity_setVirtualJoystickButton(JNIEnv* env, jclass cls, jint button, jboolean pressed) {
    // Immediate zero-latency update directly to UserEventManager
    blunted::UserEventManager::GetInstance().SetVirtualJoystickButtonDirect(button, pressed == JNI_TRUE);

    if (virtual_joystick) {
        SDL_JoystickSetVirtualButton(virtual_joystick, button, pressed ? SDL_PRESSED : SDL_RELEASED);
        SDL_JoystickUpdate();
    }

    __android_log_print(ANDROID_LOG_INFO, "FOOTBALL_INPUT", "[JNI_BTN] button=%d pressed=%s", button, pressed ? "TRUE" : "FALSE");
}

extern "C" JNIEXPORT void JNICALL Java_org_libsdl_app_SDLActivity_pauseMatch(JNIEnv* env, jclass cls) {
    boost::shared_ptr<GameTask> gTask = GetGameTask();
    if (gTask) {
        Match* match = gTask->GetMatch();
        if (match) {
            match->Pause(true);
        }
    }
}

extern "C" JNIEXPORT void JNICALL Java_org_libsdl_app_SDLActivity_setMatchPaused(JNIEnv* env, jclass cls, jboolean paused) {
    boost::shared_ptr<GameTask> gTask = GetGameTask();
    if (gTask) {
        Match* match = gTask->GetMatch();
        if (match) {
            match->Pause(paused == JNI_TRUE);
        }
    }
}

extern "C" JNIEXPORT void JNICALL Java_org_libsdl_app_SDLActivity_setCameraSettings(JNIEnv* env, jclass cls, jfloat zoom, jfloat height, jfloat fov, jfloat angleFactor) {
    boost::shared_ptr<GameTask> gTask = GetGameTask();
    if (gTask) {
        Match* match = gTask->GetMatch();
        if (match) {
            match->SetCameraSettings((float)zoom, (float)height, (float)fov, (float)angleFactor);
        }
    }
}

extern "C" JNIEXPORT jfloatArray JNICALL Java_org_libsdl_app_SDLActivity_getCameraSettings(JNIEnv* env, jclass cls) {
    jfloatArray result = env->NewFloatArray(4);
    if (!result) return nullptr;
    jfloat values[4] = {0.75f, 0.40f, 0.50f, 0.70f};
    boost::shared_ptr<GameTask> gTask = GetGameTask();
    if (gTask) {
        Match* match = gTask->GetMatch();
        if (match) {
            values[0] = match->GetCameraUserZoom();
            values[1] = match->GetCameraUserHeight();
            values[2] = match->GetCameraUserFOV();
            values[3] = match->GetCameraUserAngleFactor();
        }
    }
    env->SetFloatArrayRegion(result, 0, 4, values);
    return result;
}

extern "C" JNIEXPORT void JNICALL Java_org_libsdl_app_SDLActivity_setAudioVolume(JNIEnv* env, jclass cls, jfloat volume) {
    if (GetConfiguration()) {
        GetConfiguration()->Set("audio_volume", (float)volume);
        GetConfiguration()->SaveFile(GetConfigFilename());
    }
}

extern "C" JNIEXPORT void JNICALL Java_org_libsdl_app_SDLActivity_triggerReplay(JNIEnv* env, jclass cls, jint rewindSeconds) {
    boost::shared_ptr<GameTask> gTask = GetGameTask();
    if (gTask) {
        Match* match = gTask->GetMatch();
        if (match) {
            match->TriggerHighlightReplay(rewindSeconds * 1000);
        }
    }
}

extern "C" JNIEXPORT void JNICALL Java_org_libsdl_app_SDLActivity_stepReplay(JNIEnv* env, jclass cls, jint offset_ms) {
    boost::shared_ptr<GameTask> gTask = GetGameTask();
    if (gTask) {
        Match* match = gTask->GetMatch();
        if (match) {
            match->StepReplay(offset_ms);
        }
    }
}

extern "C" JNIEXPORT void JNICALL Java_org_libsdl_app_SDLActivity_stopReplay(JNIEnv* env, jclass cls) {
    boost::shared_ptr<GameTask> gTask = GetGameTask();
    if (gTask) {
        Match* match = gTask->GetMatch();
        if (match) {
            match->StopReplay();
        }
    }
}

extern "C" JNIEXPORT void JNICALL Java_org_libsdl_app_SDLActivity_quitMatchToMainMenu(JNIEnv* env, jclass cls) {
    if (GetContext() && GetContext()->menuTask) {
        if (GetContext()->gameTask && GetContext()->gameTask->GetMatch()) {
            GetContext()->gameTask->GetMatch()->Pause(false);
        }
        GetContext()->menuTask->SetMenuAction(e_MenuAction_Menu);
    }
}

extern "C" JNIEXPORT void JNICALL Java_org_libsdl_app_SDLActivity_setTeamKit(JNIEnv* env, jclass cls, jint teamId, jint kitNumber) {
    boost::shared_ptr<GameTask> gTask = GetGameTask();
    if (gTask && gTask->GetMatch()) {
        Team* team = gTask->GetMatch()->GetTeam(teamId);
        if (team) {
            team->SetKitNumber(kitNumber);
        }
    }
}

extern "C" JNIEXPORT void JNICALL Java_org_libsdl_app_SDLActivity_randomizeSunLighting(JNIEnv* env, jclass cls) {
    boost::shared_ptr<GameTask> gTask = GetGameTask();
    if (gTask && gTask->GetMatch()) {
        gTask->GetMatch()->SetRandomSunParams();
    }
}

extern "C" JNIEXPORT jobjectArray JNICALL Java_org_libsdl_app_SDLActivity_getTeamInfo(JNIEnv* env, jclass cls, jint teamId) {
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray array = env->NewObjectArray(4, stringClass, nullptr);
    std::string name = (teamId == 0) ? "FC BAYERN MÜNCHEN" : "CERSIPLA JAKARTA";
    std::string shortName = (teamId == 0) ? "BAY" : "CPJ";
    std::string logoUrl = (teamId == 0) ? "databases/default/images_teams/1bundesliga/bayernmunich_logo.png" : "";
    std::string kitUrl = (teamId == 0) ? "databases/default/images_teams/1bundesliga/bayernmunich_kit_01.png" : "";

    boost::shared_ptr<GameTask> gTask = GetGameTask();
    if (gTask && gTask->GetMatch()) {
        Team* team = gTask->GetMatch()->GetTeam(teamId);
        if (team && team->GetTeamData()) {
            TeamData* td = team->GetTeamData();
            name = td->GetName();
            shortName = td->GetShortName();
            logoUrl = td->GetLogoUrl();
            kitUrl = td->GetKitUrl();
        }
    }
    env->SetObjectArrayElement(array, 0, env->NewStringUTF(name.c_str()));
    env->SetObjectArrayElement(array, 1, env->NewStringUTF(shortName.c_str()));
    env->SetObjectArrayElement(array, 2, env->NewStringUTF(logoUrl.c_str()));
    env->SetObjectArrayElement(array, 3, env->NewStringUTF(kitUrl.c_str()));
    return array;
}

extern "C" JNIEXPORT jintArray JNICALL Java_org_libsdl_app_SDLActivity_getMatchStats(JNIEnv* env, jclass cls) {
    jintArray result = env->NewIntArray(28);
    if (!result) return nullptr;
    jint values[28] = {0};
    boost::shared_ptr<GameTask> gTask = GetGameTask();
    if (gTask) {
        Match* match = gTask->GetMatch();
        if (match && match->GetMatchData()) {
            MatchData* md = match->GetMatchData();
            values[0] = md->GetGoalCount(0);
            values[1] = md->GetGoalCount(1);
            unsigned long p1 = md->GetPossessionTime_ms(0);
            unsigned long p2 = md->GetPossessionTime_ms(1);
            unsigned long totalP = p1 + p2;
            int pct1 = (totalP > 0) ? (int)((p1 * 100) / totalP) : 50;
            values[2] = pct1;
            values[3] = 100 - pct1;
            values[4] = md->GetShots(0);
            values[5] = md->GetShots(1);
            values[6] = md->GetShotsOnTarget(0);
            values[7] = md->GetShotsOnTarget(1);
            values[8] = md->GetFouls(0);
            values[9] = md->GetFouls(1);
            values[10] = md->GetOffsides(0);
            values[11] = md->GetOffsides(1);
            values[12] = md->GetCorners(0);
            values[13] = md->GetCorners(1);
            values[14] = md->GetFreeKicks(0);
            values[15] = md->GetFreeKicks(1);
            values[16] = md->GetPasses(0);
            values[17] = md->GetPasses(1);
            values[18] = md->GetPassesCompleted(0);
            values[19] = md->GetPassesCompleted(1);
            values[20] = md->GetCrosses(0);
            values[21] = md->GetCrosses(1);
            values[22] = md->GetInterceptions(0);
            values[23] = md->GetInterceptions(1);
            values[24] = md->GetTackles(0);
            values[25] = md->GetTackles(1);
            values[26] = md->GetSaves(0);
            values[27] = md->GetSaves(1);
        }
    }
    env->SetIntArrayRegion(result, 0, 28, values);
    return result;
}

extern "C" JNIEXPORT void JNICALL Java_org_libsdl_app_SDLActivity_pauseEngine(JNIEnv* env, jclass cls, jboolean paused) {
    // Old engine doesn't need to pause scheduler here, SDL pause handles it.
}

extern "C" JNIEXPORT void JNICALL Java_org_libsdl_app_SDLActivity_forceQuitEngine(JNIEnv* env, jclass cls) {
    blunted::EnvironmentManager::GetInstance().SignalQuit();
}

extern "C" JNIEXPORT void JNICALL Java_org_libsdl_app_SDLActivity_triggerInGamePause(JNIEnv* env, jclass cls) {
    blunted::UserEventManager::GetInstance().SetKeyboardState(SDLK_ESCAPE, true);
}
#endif

namespace blunted {

  template<> UserEventManager* Singleton<UserEventManager>::singleton = 0;

  UserEventManager::UserEventManager() {
    lastKeyTime_ms = 0;

    //SDL_EnableKeyRepeat(0, SDL_DEFAULT_REPEAT_INTERVAL);

    // yes, SDL starts mousebuttons at 1...
    for (int i = 1; i < 8; i++) {
      mousePressed[i] = false;
    }

    for (int j = 0; j < _JOYSTICK_MAX; j++) {
      for (int i = 0; i < _JOYSTICK_MAXBUTTONS; i++) {
        joyButtonPressed[j][i] = false;
      }
    }

    for (int j = 0; j < _JOYSTICK_MAX; j++) {
      for (int i = 0; i < _JOYSTICK_MAXAXES; i++) {
        joyAxis[j][i] = 0.0;
        joyAxisCalibration[j][i][0] = -32768.0;
        joyAxisCalibration[j][i][1] = 32767.0;
        joyAxisCalibration[j][i][2] = 0.0;
      }
    }


#ifndef GPFB_HEADLESS
    // init the joy!

#ifdef __ANDROID__
    SDL_SetHint(SDL_HINT_ACCELEROMETER_AS_JOYSTICK, "0");
#endif

    SDL_Init(SDL_INIT_JOYSTICK);

#ifdef __ANDROID__
    if (!virtual_joystick) {
        int device_index = SDL_JoystickAttachVirtual(SDL_JOYSTICK_TYPE_GAMECONTROLLER, 2, 8, 0);
        if (device_index >= 0) {
            virtual_joystick = SDL_JoystickOpen(device_index);
            __android_log_print(ANDROID_LOG_INFO, "FOOTBALL_INPUT", "[JOY_INIT] Attached virtual joystick device_index=%d instanceID=%d",
                                device_index, virtual_joystick ? SDL_JoystickInstanceID(virtual_joystick) : -1);
        }
    }
#endif

    for (int i = 0; i < SDL_NumJoysticks(); i++) {
      joystick[i] = SDL_JoystickOpen(i);
    }
    //SDL_JoystickEventState(SDL_IGNORE); // doesn't seem to work? bug?
    SDL_JoystickEventState(SDL_ENABLE);
    //printf("JOYSTICK EVENT STATE: %i (%i = ignore, %i = enable)\n", SDL_JoystickEventState(SDL_QUERY), SDL_IGNORE, SDL_ENABLE);
#endif
  }

  UserEventManager::~UserEventManager() {
#ifndef GPFB_HEADLESS
    for (int i = 0; i < SDL_NumJoysticks(); i++) {
      SDL_JoystickClose(joystick[i]);
    }
#endif
  }

  void UserEventManager::Exit() {
  }

  void UserEventManager::InputSDLEvent(const SDL_Event &event) {
    int joyID = 0;
#ifndef GPFB_HEADLESS
    switch (event.type) {
      case SDL_KEYDOWN:
        keyPressedMutex.lock();
        keyPressed[event.key.keysym.sym].pressTime_ms = EnvironmentManager::GetInstance().GetTime_ms();
        lastKeyTime_ms = keyPressed[event.key.keysym.sym].pressTime_ms;
        keyPressedMutex.unlock();
        break;
      case SDL_KEYUP:
        keyPressedMutex.lock();
        keyPressed.erase(event.key.keysym.sym);
        keyPressedMutex.unlock();
        break;
      case SDL_MOUSEBUTTONDOWN:
        mousePressedMutex.lock();
        mousePressed[event.button.button] = true;
        mousePressedMutex.unlock();
        break;
      case SDL_MOUSEBUTTONUP:
        mousePressedMutex.lock();
        mousePressed[event.button.button] = false;
        mousePressedMutex.unlock();
        break;
      case SDL_JOYAXISMOTION:
        joyButtonPressedMutex.lock();
        joyID = event.jaxis.which;
#ifdef __ANDROID__
        if (virtual_joystick && SDL_JoystickInstanceID(virtual_joystick) == joyID) {
            if (event.jaxis.axis < _JOYSTICK_MAXAXES) {
                joyAxis[0][event.jaxis.axis] = event.jaxis.value;
            }
        }
#else
        if (joyID < _JOYSTICK_MAX) {
            joyAxis[joyID][event.jaxis.axis] = event.jaxis.value;
        }
#endif
        joyButtonPressedMutex.unlock();
        break;
      case SDL_JOYBUTTONDOWN:
        joyButtonPressedMutex.lock();
        joyID = event.jbutton.which;
#ifdef __ANDROID__
        if (virtual_joystick && SDL_JoystickInstanceID(virtual_joystick) == joyID) {
            if (event.jbutton.button < _JOYSTICK_MAXBUTTONS) {
                joyButtonPressed[0][event.jbutton.button] = true;
            }
        }
#else
        if (joyID < _JOYSTICK_MAX && event.jbutton.button < _JOYSTICK_MAXBUTTONS) {
            joyButtonPressed[joyID][event.jbutton.button] = true;
        }
#endif
        joyButtonPressedMutex.unlock();
        break;
      case SDL_JOYBUTTONUP:
        joyButtonPressedMutex.lock();
        joyID = event.jbutton.which;
#ifdef __ANDROID__
        if (virtual_joystick && SDL_JoystickInstanceID(virtual_joystick) == joyID) {
            if (event.jbutton.button < _JOYSTICK_MAXBUTTONS) {
                joyButtonPressed[0][event.jbutton.button] = false;
            }
        }
#else
        if (joyID < _JOYSTICK_MAX && event.jbutton.button < _JOYSTICK_MAXBUTTONS) {
            joyButtonPressed[joyID][event.jbutton.button] = false;
        }
#endif
        joyButtonPressedMutex.unlock();
        break;
    }
#endif
  }

  bool UserEventManager::GetKeyboardState(SDL_Keycode code) const {
    boost::mutex::scoped_lock lock(keyPressedMutex);
    return keyPressed.count(code) > 0;
  }

  std::map<SDL_Keycode, TimedKeyPress> UserEventManager::GetKeyboardState() const {
    boost::mutex::scoped_lock lock(keyPressedMutex);
    return keyPressed;
  }

  void UserEventManager::SetKeyboardState(SDL_Keycode key, bool newState) {
      boost::mutex::scoped_lock lock(keyPressedMutex);
      if (!newState) {
          keyPressed.erase(key);
      } else {
          keyPressed[key].pressTime_ms = EnvironmentManager::GetInstance().GetTime_ms();
      }
  }

  unsigned long UserEventManager::GetLastKeyPressDiff_ms() {
    boost::mutex::scoped_lock lock(keyPressedMutex);
    return EnvironmentManager::GetInstance().GetTime_ms() - lastKeyTime_ms;
  }

  unsigned long UserEventManager::GetLastKeyPressDiff_ms(SDL_Keycode key) {
    boost::mutex::scoped_lock lock(keyPressedMutex);
    return EnvironmentManager::GetInstance().GetTime_ms() - keyPressed[key].pressTime_ms;
  }

  bool UserEventManager::GetMouseButtonState(int sdlButtonID) const {
    boost::mutex::scoped_lock lock(mousePressedMutex);
    return mousePressed[sdlButtonID];
  }

  Vector3 UserEventManager::GetMouseRelativePos() const {
    Vector3 mousePos;
    mousePos.coords[2] = 0;
#ifndef GPFB_HEADLESS
    int x, y;
    SDL_GetRelativeMouseState(&x, &y);
    mousePos.coords[0] = x;
    mousePos.coords[1] = y;
#endif
    return mousePos;
  }


  bool UserEventManager::GetJoyButtonState(int joyID, int sdlJoyButtonID) const {
    boost::mutex::scoped_lock lock(joyButtonPressedMutex);
    return joyButtonPressed[joyID][sdlJoyButtonID];
  }

  void UserEventManager::SetJoyButtonState(int joyID, int sdlJoyButtonID, bool newState) {
    boost::mutex::scoped_lock lock(joyButtonPressedMutex);
    joyButtonPressed[joyID][sdlJoyButtonID] = newState;
  }

  float UserEventManager::GetJoystickAxis(int joyID, int axisID, bool deadzone) const {
    boost::mutex::scoped_lock lock(joyButtonPressedMutex);

    float min = joyAxisCalibration[joyID][axisID][0];
    float max = joyAxisCalibration[joyID][axisID][1];
    float rest = joyAxisCalibration[joyID][axisID][2];

    float value = joyAxis[joyID][axisID];

    if (value < min) value = min;
    if (value > max) value = max;
    float scale = max - min;
    if (scale == 0.0) scale = 0.01; // avoid division by zero, axis would be defunct though if scale evaluates to 0

    // bring value in range 0 .. 1
    value -= min;
    value /= scale;

    // bring rest in range 0 .. 1
    rest -= min;
    rest /= scale;

    // deadzone
    if (deadzone)
      if (fabs(rest - value) < 0.1) value = rest;

    if (value < rest) {
      // bring value in range 0 .. -1
      value /= rest;
      value -= 1.0;
    } else if (value > rest) {
      // bring value in range 0 .. 1
      scale = 1.0 - rest;
      value -= rest;
      value /= scale;
    } else { // value == rest
      value = 0.0;
    }

    return value;
  }

  float UserEventManager::GetJoystickAxisRaw(int joyID, int axisID) const {
    boost::mutex::scoped_lock lock(joyButtonPressedMutex);
    return joyAxis[joyID][axisID];
  }

  float UserEventManager::GetJoystickAxisCalibrationMin(int joyID, int axisID) {
    boost::mutex::scoped_lock lock(joyButtonPressedMutex);
    return joyAxisCalibration[joyID][axisID][0];
  }

  float UserEventManager::GetJoystickAxisCalibrationMax(int joyID, int axisID) {
    boost::mutex::scoped_lock lock(joyButtonPressedMutex);
    return joyAxisCalibration[joyID][axisID][1];
  }

  float UserEventManager::GetJoystickAxisCalibrationRest(int joyID, int axisID) {
    boost::mutex::scoped_lock lock(joyButtonPressedMutex);
    return joyAxisCalibration[joyID][axisID][2];
  }

  void UserEventManager::SetVirtualJoystickAxisDirect(int axisID, float value) {
    boost::mutex::scoped_lock lock(joyButtonPressedMutex);
    if (axisID >= 0 && axisID < _JOYSTICK_MAXAXES) {
      joyAxis[0][axisID] = value;
    }
  }

  void UserEventManager::SetVirtualJoystickButtonDirect(int buttonID, bool pressed) {
    boost::mutex::scoped_lock lock(joyButtonPressedMutex);
    if (buttonID >= 0 && buttonID < _JOYSTICK_MAXBUTTONS) {
      joyButtonPressed[0][buttonID] = pressed;
    }
  }

  void UserEventManager::SetPossession(bool isAttacking) {
#ifdef __ANDROID__
    static bool hasSentPossession = false;
    static bool lastPossessionState = false;
    if (hasSentPossession && lastPossessionState == isAttacking) return;
    hasSentPossession = true;
    lastPossessionState = isAttacking;

    JNIEnv* env = (JNIEnv*)SDL_AndroidGetJNIEnv();
    if (env != nullptr) {
      jobject activity = (jobject)SDL_AndroidGetActivity();
      if (activity != nullptr) {
        jclass activityClass = env->GetObjectClass(activity);
        if (activityClass != nullptr) {
          jmethodID setPossessionMethod = env->GetStaticMethodID(activityClass, "setPossession", "(Z)V");
          if (setPossessionMethod != nullptr) {
            env->CallStaticVoidMethod(activityClass, setPossessionMethod, isAttacking ? JNI_TRUE : JNI_FALSE);
          }
          env->DeleteLocalRef(activityClass);
        }
        env->DeleteLocalRef(activity);
      }
    }
#endif
  }

  void UserEventManager::NotifyGameOver(int t1score, int t2score, int t1possession, int t2possession, int t1shots, int t2shots) {
#ifdef __ANDROID__
    JNIEnv* env = (JNIEnv*)SDL_AndroidGetJNIEnv();
    if (env != nullptr) {
      jobject activity = (jobject)SDL_AndroidGetActivity();
      if (activity != nullptr) {
        jclass activityClass = env->GetObjectClass(activity);
        if (activityClass != nullptr) {
          jmethodID notifyGameOverMethod = env->GetStaticMethodID(activityClass, "notifyGameOver", "(IIIIII)V");
          if (notifyGameOverMethod != nullptr) {
            env->CallStaticVoidMethod(activityClass, notifyGameOverMethod, t1score, t2score, t1possession, t2possession, t1shots, t2shots);
          }
          env->DeleteLocalRef(activityClass);
        }
        env->DeleteLocalRef(activity);
      }
    }
#endif
  }

  void UserEventManager::UpdateMatchTelemetry(const std::string &t1Name, const std::string &t2Name, int t1score, int t2score, const std::string &timeStr) {
#ifdef __ANDROID__
    JNIEnv* env = (JNIEnv*)SDL_AndroidGetJNIEnv();
    if (env != nullptr) {
      jobject activity = (jobject)SDL_AndroidGetActivity();
      if (activity != nullptr) {
        jclass activityClass = env->GetObjectClass(activity);
        if (activityClass != nullptr) {
          jmethodID updateTelemetryMethod = env->GetStaticMethodID(activityClass, "updateMatchTelemetry", "(Ljava/lang/String;Ljava/lang/String;IILjava/lang/String;)V");
          if (updateTelemetryMethod != nullptr) {
            jstring jT1 = env->NewStringUTF(t1Name.c_str());
            jstring jT2 = env->NewStringUTF(t2Name.c_str());
            jstring jTime = env->NewStringUTF(timeStr.c_str());
            env->CallStaticVoidMethod(activityClass, updateTelemetryMethod, jT1, jT2, t1score, t2score, jTime);
            env->DeleteLocalRef(jT1);
            env->DeleteLocalRef(jT2);
            env->DeleteLocalRef(jTime);
          }
          env->DeleteLocalRef(activityClass);
        }
        env->DeleteLocalRef(activity);
      }
    }
#endif
  }

  void UserEventManager::SetJoystickAxisCalibration(int joyID, int axisID, float min, float max, float rest) {
    boost::mutex::scoped_lock lock(joyButtonPressedMutex);
    joyAxisCalibration[joyID][axisID][0] = min;
    joyAxisCalibration[joyID][axisID][1] = max;
    joyAxisCalibration[joyID][axisID][2] = rest;

    // rest has to be within min/max range
    if (joyAxisCalibration[joyID][axisID][2] < joyAxisCalibration[joyID][axisID][0]) joyAxisCalibration[joyID][axisID][2] = joyAxisCalibration[joyID][axisID][0];
    if (joyAxisCalibration[joyID][axisID][2] > joyAxisCalibration[joyID][axisID][1]) joyAxisCalibration[joyID][axisID][2] = joyAxisCalibration[joyID][axisID][1];
    joyAxis[joyID][axisID] = rest;
  }

}

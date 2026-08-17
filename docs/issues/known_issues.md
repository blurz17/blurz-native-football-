# Known Issues & Missing Features

## Active Issues
* **In-Game Pause Button Unresponsive**: The "Pause" button on the Java overlay UI does not successfully trigger the `BIT_START` / `SDLK_ESCAPE` pause menu in the C++ engine. (Target: Phase 2)
* **Java UI Performance Bottleneck**: The Android UI layer (Java) bridging touch events via JNI to the C++ engine causes stuttering and garbage collection pauses. (Target: Phase 3)

## Planned Features
* **Native C++ Touch UI**: Remove the Java overlay entirely and implement `SDL_FINGERDOWN` touch controls native to the C++ engine for locked 60FPS gameplay.
* **Google Play Games Integration**: For free cloud saving and easy login.
* **Local Multiplayer (Split Screen)**: Two virtual joysticks on the same device.

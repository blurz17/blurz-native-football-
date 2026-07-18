# blurz-native-football-

A native Android port of the open-source Gameplay Football engine, originally created by Bastiaan Konings and later updated by Google LLC.

## Features

- Full 3D football simulation running natively on Android (ARM64)
- Custom touch controls with virtual joystick and action buttons
- Menu navigation via on-screen D-PAD
- Dual control modes: Match Mode (joystick + action buttons) and Menu Mode (D-PAD + Enter/Back)
- OpenGL ES 3.x rendering
- OpenAL audio

## Tech Stack

| Component | Technology |
|-----------|------------|
| Engine | C++ (GameplayFootball) |
| Graphics | OpenGL ES via SDL2 |
| Audio | OpenAL Soft |
| Platform | Android NDK (arm64-v8a) |
| Build | Gradle + ndk-build |
| JDK | JDK 17 |

## Building

### Prerequisites

- JDK 17
- Android NDK (r25+)
- Android SDK (API 33+)
- SDL2 2.28.5 source — Download from https://github.com/libsdl-org/SDL/releases/tag/release-2.28.5 and extract to the project root as `SDL2-2.28.5/`

### Build APK

```bash
cd android-project
# Set JAVA_HOME to your JDK 17 path
export JAVA_HOME=/path/to/jdk-17
./gradlew assembleRelease
```

The APK will be output to:
`android-project/app/build/outputs/apk/release/app-release.apk`

## Project Structure

```
android_native_football/
├── LICENSE                     # Proprietary license
├── THIRD_PARTY_NOTICES.md      # Open-source acknowledgments
├── README.md                   # This file
└── android-project/
    └── app/
        ├── build.gradle
        ├── jni/
        │   └── src/            # C++ engine source code
        └── src/main/java/
            └── org/libsdl/app/
                ├── SDLActivity.java         # Main activity
                ├── SDLSurface.java          # OpenGL rendering surface
                └── MobileControlsView.java  # Touch controls overlay
```

## License

This project is **proprietary software**. All rights reserved.
See [LICENSE](LICENSE) for details.

This project incorporates open-source components.
See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for details.

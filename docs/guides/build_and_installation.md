# Build & Installation Guide

This guide provides instructions for compiling, packaging, and installing Android Native Football from source.

---

## 1. Prerequisites & Toolchain Setup

### Required Tools:
- **Operating System**: Windows 10/11, macOS, or Linux.
- **Java Development Kit (JDK)**: OpenJDK 17 or higher.
- **Android SDK**: API Level 33+ (Android 13+).
- **Android NDK**: NDK r25b or higher (e.g., Unity NDK or standalone Android NDK).
- **CMake**: Version 3.22.1+.
- **Android Debug Bridge (ADB)**: Included with Android SDK Platform-Tools.

### Environment Variables (Windows PowerShell Example):
```powershell
$env:JAVA_HOME = "D:\unity\6000.4.2f1\Editor\Data\PlaybackEngines\AndroidPlayer\OpenJDK"
$env:ANDROID_HOME = "C:\Users\<Username>\AppData\Local\Android\Sdk"
$env:ANDROID_NDK_HOME = "D:\unity\6000.4.2f1\Editor\Data\PlaybackEngines\AndroidPlayer\NDK"
$env:PATH = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:PATH"
```

---

## 2. Compiling the Release APK

1. Navigate to the Android platform directory:
   ```powershell
   cd platform\android
   ```
2. Build the native shared libraries (`libmain.so`, `libSDL2.so`, etc.) and assemble the release APK using Gradle:
   ```powershell
   .\gradlew.bat assembleRelease
   ```
3. The generated release APK will be located at:
   - `platform/android/app/build/outputs/apk/release/app-release.apk`
   - A copy is mirrored at the project root: `app-release.apk`

---

## 3. Installing on Device via ADB

1. Enable **Developer Options** and **USB Debugging** on your Android device.
2. Connect your device via USB and verify connection:
   ```bash
   adb devices
   ```
3. Install the APK with overwrite permissions:
   ```bash
   adb install -r platform/android/app/build/outputs/apk/release/app-release.apk
   ```

---

## 4. Live Telemetry & Logcat Monitoring

To monitor in-game performance telemetry, memory allocations, and engine alerts in real time:

```bash
adb logcat -s "SDL/APP" "GameplayFootball" "PERF" "DEBUG"
```

To filter specifically for performance output:
```bash
adb logcat | Select-String "\[PERF"
```

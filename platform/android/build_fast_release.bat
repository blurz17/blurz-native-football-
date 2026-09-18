@echo off
setlocal

:: Point to the Unity JDK on your D: drive
set JAVA_HOME=D:\unity\6000.4.2f1\Editor\Data\PlaybackEngines\AndroidPlayer\OpenJDK

:: Force Gradle to use the E: drive so it NEVER touches your full C: drive again!
set GRADLE_USER_HOME=E:\.gradle

echo Switching to non-spaced directory...
cd /d "E:\MY-PROJECTS\android_native_football\platform\android"

echo Starting Android FAST Native Build (Release)...
call gradlew.bat assembleRelease --no-daemon

if %errorlevel% neq 0 (
    echo ------------------------------------------
    echo BUILD FAILED!
    echo ------------------------------------------
    exit /b %errorlevel%
) else (
    echo ------------------------------------------
    echo BUILD SUCCESSFUL!
    echo Your APK is ready here:
    echo app\build\outputs\apk\release\app-release.apk
    echo ------------------------------------------
)

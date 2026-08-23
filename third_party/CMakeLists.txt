# Third Party Libraries Build

# Note: Many of these libraries might not have native CMake support for Android 
# out of the box, or might require their own custom toolchains. 
# We will add them as we resolve the build issues.

add_subdirectory(SDL_android)

set(SDL2IMAGE_VENDORED ON CACHE BOOL "" FORCE)
add_subdirectory(SDL2_image_android)
add_subdirectory(SDL2_gfx)

set(SDL2TTF_VENDORED ON CACHE BOOL "" FORCE)
set(SDL2TTF_INSTALL OFF CACHE BOOL "" FORCE)
set(SDL2TTF_SAMPLES OFF CACHE BOOL "" FORCE)
add_subdirectory(SDL2_ttf)
set(ALSOFT_UTILS OFF CACHE BOOL "" FORCE)
set(ALSOFT_EXAMPLES OFF CACHE BOOL "" FORCE)
set(ALSOFT_TESTS OFF CACHE BOOL "" FORCE)
set(ALSOFT_INSTALL OFF CACHE BOOL "" FORCE)
add_subdirectory(openal-soft)
add_subdirectory(sqlite)

set(ODE_WITH_DEMOS OFF CACHE BOOL "" FORCE)
set(ODE_DOUBLE_PRECISION OFF CACHE BOOL "" FORCE)
add_subdirectory(ode)
# gl4es: Build as STATIC library on Android to avoid libGL.so.1 runtime dependency.
# We keep the dynamic loader enabled so gl4es can dlopen("libGLESv2.so") at runtime,
# which does exist on Android. The static build just prevents libmain.so from having
# a linker dependency on "libGL.so.1".
set(STATICLIB ON CACHE BOOL "Build gl4es as a static library" FORCE)
set(NOX11 ON CACHE BOOL "No X11 on Android" FORCE)
set(NO_GBM ON CACHE BOOL "No GBM on Android" FORCE)
set(USE_ANDROID_LOG ON CACHE BOOL "Use Android logcat" FORCE)
set(DEFAULT_ES 2 CACHE STRING "Default to GLES2" FORCE)
add_subdirectory(gl4es)

cmake_minimum_required(VERSION 3.10)
project(blurz_native_football)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Source directories
set(ENGINE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/engine)
set(GAME_DIR ${CMAKE_CURRENT_SOURCE_DIR}/game)
set(PLATFORM_DIR ${CMAKE_CURRENT_SOURCE_DIR}/platform)

# Game & Engine sources
file(GLOB_RECURSE ALL_SOURCES 
    "${ENGINE_DIR}/*.cpp"
    "${ENGINE_DIR}/*.hpp"
    "${ENGINE_DIR}/*.c"
    "${ENGINE_DIR}/*.h"
    "${GAME_DIR}/*.cpp"
    "${GAME_DIR}/*.hpp"
    "${GAME_DIR}/*.c"
    "${GAME_DIR}/*.h"
    "${PLATFORM_DIR}/android/android_assets.cpp"
    "${PLATFORM_DIR}/android/android_assets.hpp"
)

# Third party libraries
add_subdirectory(third_party)

include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${ENGINE_DIR}
    ${GAME_DIR}
    ${PLATFORM_DIR}
    ${ENGINE_DIR}/fake_boost
    ${CMAKE_CURRENT_SOURCE_DIR}/third_party/SDL_android/include
    ${CMAKE_CURRENT_SOURCE_DIR}/third_party/gl4es/include
    ${CMAKE_CURRENT_SOURCE_DIR}/third_party/ode/include
    ${CMAKE_CURRENT_SOURCE_DIR}/third_party/sqlite/sqlite-amalgamation-3450300
    ${CMAKE_CURRENT_SOURCE_DIR}/third_party/openal-soft/include
    ${CMAKE_CURRENT_SOURCE_DIR}/third_party/SDL2_image_android
    ${CMAKE_CURRENT_SOURCE_DIR}/third_party/SDL2_ttf
    ${CMAKE_CURRENT_SOURCE_DIR}/third_party/SDL2_gfx
)

if(ANDROID)
    # Android specific libraries
    find_library(log-lib log)
    find_library(android-lib android)
    find_library(GLESv3-lib GLESv3)
    find_library(GLESv2-lib GLESv2)
    find_library(EGL-lib EGL)
    
    # Normally we would build SDL2 via CMake too, but for now we assume 
    # Gradle/Android.mk handles SDL2 shared library building, or we link against the prebuilt.
    # We will build the main game as a shared library for Android
    add_library(main SHARED ${ALL_SOURCES})
    
    target_compile_definitions(main PRIVATE GPFB_ANDROID)
    target_link_libraries(main
        ${log-lib}
        ${android-lib}
        GL
        ${GLESv2-lib}
        ${GLESv3-lib}
        ${EGL-lib}
        dl
        SDL2
        SDL2_image
        SDL2_ttf
        SDL2_gfx
        ODE
        sqlite3
        OpenAL
    )
else()
    # PC/Windows/Mac Build
    add_executable(gameplayfootball ${ALL_SOURCES})
endif()

# Tests disabled - test files reference the old engine directory layout
# and are incompatible with the ported old-project engine structure.

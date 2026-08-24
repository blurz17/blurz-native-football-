cmake_minimum_required(VERSION 3.10)
project(blurz_native_football)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Source directories
set(ENGINE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/engine)
set(GAME_DIR ${CMAKE_CURRENT_SOURCE_DIR}/game)

# Engine sources
file(GLOB_RECURSE ENGINE_SOURCES 
    "${ENGINE_DIR}/*.cpp"
    "${ENGINE_DIR}/*.hpp"
    "${ENGINE_DIR}/*.c"
    "${ENGINE_DIR}/*.h"
)

# Game sources
file(GLOB_RECURSE GAME_SOURCES 
    "${GAME_DIR}/*.cpp"
    "${GAME_DIR}/*.hpp"
)

# Third party libraries
add_subdirectory(third_party)

include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${ENGINE_DIR}
    ${GAME_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/engine/fake_boost
    ${CMAKE_CURRENT_SOURCE_DIR}/third_party/SDL_android/include
    ${CMAKE_CURRENT_SOURCE_DIR}/third_party/gl4es/include
    ${CMAKE_CURRENT_SOURCE_DIR}/third_party/ode/include
    ${CMAKE_CURRENT_SOURCE_DIR}/third_party/sqlite/sqlite-amalgamation-3450300
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
    add_library(main SHARED ${ENGINE_SOURCES} ${GAME_SOURCES})
    
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
        # OpenAL
    )
else()
    # PC/Windows/Mac Build
    add_executable(gameplayfootball ${ENGINE_SOURCES} ${GAME_SOURCES})
endif()

# Tests
include(FetchContent)
FetchContent_Declare(
  googletest
  URL https://github.com/google/googletest/archive/03597a01ee50ed33e9dfd640b249b4be3799d395.zip
)
# For Windows: Prevent overriding the parent project's compiler/linker settings
set(gtest_force_shared_crt ON CACHE BOOL "" FORCE)
FetchContent_MakeAvailable(googletest)

list(REMOVE_ITEM ENGINE_SOURCES "${ENGINE_DIR}/main.cpp")

file(GLOB_RECURSE TEST_SOURCES 
    "${CMAKE_CURRENT_SOURCE_DIR}/test/*.cpp"
    "${CMAKE_CURRENT_SOURCE_DIR}/test/*.hpp"
)
list(REMOVE_ITEM TEST_SOURCES "${CMAKE_CURRENT_SOURCE_DIR}/test/test_main.cpp")

if(TEST_SOURCES)
    add_executable(run_tests ${TEST_SOURCES} ${ENGINE_SOURCES} ${GAME_SOURCES})
    target_link_libraries(run_tests
        gtest_main
        SDL2
        SDL2_image
        SDL2_ttf
        SDL2_gfx
        ODE
        sqlite3
    )
    if(NOT ANDROID)
        include(GoogleTest)
        gtest_discover_tests(run_tests)
    endif()
    
    if(ANDROID)
        target_compile_definitions(run_tests PRIVATE GPFB_ANDROID)
        target_link_libraries(run_tests ${log-lib} ${android-lib} GL ${GLESv2-lib} ${GLESv3-lib} ${EGL-lib} dl)
    endif()
endif()

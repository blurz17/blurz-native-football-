cmake_minimum_required(VERSION 3.10)
project(SDL2_gfx C)

file(GLOB SRC_FILES "*.c")

add_library(SDL2_gfx STATIC ${SRC_FILES})
target_include_directories(SDL2_gfx PUBLIC 
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_SOURCE_DIR}/third_party/SDL_android/include
)

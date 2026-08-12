cmake_minimum_required(VERSION 3.10)
project(AL C)

add_library(AL INTERFACE)
target_include_directories(AL INTERFACE ${CMAKE_CURRENT_SOURCE_DIR}/..)

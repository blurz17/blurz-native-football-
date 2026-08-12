cmake_minimum_required(VERSION 3.10)
project(sqlite C)

add_library(sqlite3 STATIC sqlite-amalgamation-3450300/sqlite3.c)
target_include_directories(sqlite3 PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/sqlite-amalgamation-3450300)

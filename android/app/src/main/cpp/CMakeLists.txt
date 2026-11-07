cmake_minimum_required(VERSION 3.22)
project(ambr_native LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(ambr_native SHARED
    jni_bridge.cpp
    coverage_engine.cpp
)

target_compile_options(ambr_native PRIVATE -O2 -Wall -Wextra)
target_link_libraries(ambr_native log)

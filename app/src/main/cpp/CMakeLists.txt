cmake_minimum_required(VERSION 3.4.1)
project(PrimeProFast)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -ffast-math -funroll-loops -DNDEBUG")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3 -ffast-math -funroll-loops -DNDEBUG")

set(GMP_INCLUDE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/gmp/include")
set(GMP_LIBRARY_DIR "${CMAKE_CURRENT_SOURCE_DIR}/gmp/lib")
include_directories(${GMP_INCLUDE_DIR})

if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(GMP_LIB_PATH "${GMP_LIBRARY_DIR}/arm64-v8a")
elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
    set(GMP_LIB_PATH "${GMP_LIBRARY_DIR}/armeabi-v7a")
elseif(ANDROID_ABI STREQUAL "x86")
    set(GMP_LIB_PATH "${GMP_LIBRARY_DIR}/x86")
elseif(ANDROID_ABI STREQUAL "x86_64")
    set(GMP_LIB_PATH "${GMP_LIBRARY_DIR}/x86_64")
else()
    set(GMP_LIB_PATH "${GMP_LIBRARY_DIR}/arm64-v8a")
endif()

add_library(ppf_engine STATIC
    ppf_engine.cpp
    ppf_standalone_engine.cpp
    ppf_c_api.cpp
)

target_link_libraries(ppf_engine ${GMP_LIB_PATH}/libgmp.a ${GMP_LIB_PATH}/libgmpxx.a)

add_library(super_primos SHARED
    super_primos.cpp
)

add_library(super_primos_exe SHARED
    super_primos_standalone.cpp
)

find_library(log-lib log)

target_link_libraries(super_primos ppf_engine ${log-lib})
target_link_libraries(super_primos_exe ppf_engine ${log-lib})

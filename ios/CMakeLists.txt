cmake_minimum_required(VERSION 3.22)
project(PrimeProFastIOS LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(CPP_ROOT "${CMAKE_CURRENT_LIST_DIR}/../app/src/main/cpp")
set(GMP_INCLUDE "${CPP_ROOT}/gmp/include")
set(GMP_LIB "${CPP_ROOT}/gmp/lib/ios-arm64")

add_library(ppf_core STATIC
    "${CPP_ROOT}/ppf_engine.cpp"
    "${CPP_ROOT}/ppf_standalone_engine.cpp"
    "${CPP_ROOT}/ppf_c_api.cpp"
)

target_include_directories(ppf_core PUBLIC "${CPP_ROOT}" "${GMP_INCLUDE}")
target_compile_options(ppf_core PRIVATE -O3 -ffast-math -funroll-loops -DNDEBUG)

if(EXISTS "${GMP_LIB}/lib/libgmp.a" AND EXISTS "${GMP_LIB}/lib/libgmpxx.a")
    set(GMP_A "${GMP_LIB}/lib/libgmp.a")
    set(GMPXX_A "${GMP_LIB}/lib/libgmpxx.a")
elseif(EXISTS "${GMP_LIB}/libgmp.a" AND EXISTS "${GMP_LIB}/libgmpxx.a")
    set(GMP_A "${GMP_LIB}/libgmp.a")
    set(GMPXX_A "${GMP_LIB}/libgmpxx.a")
else()
    message(FATAL_ERROR "GMP iOS incompleto (libgmp.a + libgmpxx.a). Rode scripts/build-gmp-ios.sh")
endif()

target_link_libraries(ppf_core PUBLIC "${GMP_A}" "${GMPXX_A}")

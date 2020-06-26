cmake_minimum_required(VERSION 3.4.1)

include_directories("${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp/include")
add_library(realsense2 SHARED IMPORTED)

set_target_properties(
        realsense2
        PROPERTIES
        IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/build/jni/${ANDROID_ABI}/librealsense2.so
)

add_library(real_sense SHARED src/main/cpp/real_sense.cpp)

find_library(log-lib log)

target_link_libraries(
        real_sense
        ${log-lib}
        realsense2
)


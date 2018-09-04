# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Sets some warning/error conditions and which version of C/C++ to use
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -flto -std=c11")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -flto -Wall -Werror -Wno-error=unused-command-line-argument -std=c++17")

# Fix LTO on release
string(REPLACE "-Os" "-O3" CMAKE_C_FLAGS_RELEASE ${CMAKE_C_FLAGS_RELEASE})
string(REPLACE "-Os" "-O3" CMAKE_C_FLAGS ${CMAKE_C_FLAGS})
string(REPLACE "-Os" "-O3" CMAKE_CXX_FLAGS_RELEASE ${CMAKE_CXX_FLAGS_RELEASE})
string(REPLACE "-Os" "-O3" CMAKE_CXX_FLAGS ${CMAKE_CXX_FLAGS})

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
             chatauth

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             src/main/cpp/chatauth.cpp
             src/main/cpp/base64.h )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       chatauth

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )
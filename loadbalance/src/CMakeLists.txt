#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#
# CMake configuration.
#

cmake_minimum_required(VERSION 2.6 FATAL_ERROR)

list(APPEND CMAKE_MODULE_PATH ${CMAKE_SOURCE_DIR}/..)
include(HadoopCommon)

# Source and test locations.
set(SRC main/native/src/alluxio/)
set(TST main/native/src/test/)

#
# Main configuration.
#

# The caller must specify where the generated headers have been placed.
if(NOT GENERATED_JAVAH)
    message(FATAL_ERROR "You must set the CMake variable GENERATED_JAVAH")
endif()

# Configure JNI.:
include(HadoopJNI)

set(STORED_CMAKE_FIND_LIBRARY_SUFFIXES ${CMAKE_FIND_LIBRARY_SUFFIXES})
#hadoop_set_find_shared_library_version("2")
find_library(ISAL_LIBRARY
    NAMES isal
     PATHS /usr/lib)
set(CMAKE_FIND_LIBRARY_SUFFIXES ${STORED_CMAKE_FIND_LIBRARY_SUFFIXES})

if (ISAL_LIBRARY)
    GET_FILENAME_COMPONENT(HADOOP_ISAL_LIBRARY ${ISAL_LIBRARY} NAME)
	message("The name of isal library is=${HADOOP_ISAL_LIBRARY}")    
    set(ISAL_INCLUDE_DIR ${SRC}/erasurecode)
    set(ISAL_SOURCE_FILES
        ${SRC}/erasurecode/isal_load.c
        ${SRC}/erasurecode/erasure_code.c
        ${SRC}/erasurecode/gf_util.c
        ${SRC}/erasurecode/dump.c
        ${SRC}/erasurecode/erasure_coder.c
        ${SRC}/erasurecode/jni_erasure_code_native.c
        ${SRC}/erasurecode/jni_common.c
        ${SRC}/erasurecode/jni_rs_encoder.c
        ${SRC}/erasurecode/jni_rs_decoder.c)

        add_executable(erasure_code_test
        ${SRC}/erasurecode/isal_load.c
        ${SRC}/erasurecode/erasure_code.c
        ${SRC}/erasurecode/gf_util.c
        ${SRC}/erasurecode/dump.c
        ${SRC}/erasurecode/erasure_coder.c
        ${TST}/erasurecode/erasure_code_test.c
        )
        message("CMAKE_DL_LIBS=${CMAKE_DL_LIBS}")
        target_link_libraries(erasure_code_test ${CMAKE_DL_LIBS})
else (ISAL_LIBRARY)
    IF(REQUIRE_ISAL)
        MESSAGE(FATAL_ERROR "Required ISA-L library could not be found.  ISAL_LIBRARY=${ISAL_LIBRARY}, CUSTOM_ISAL_PREFIX=${CUSTOM_ISAL_PREFIX}")
    ENDIF(REQUIRE_ISAL)
endif (ISAL_LIBRARY)

# Check for platform-specific functions and libraries.
include(CheckFunctionExists)
include(CheckLibraryExists)
check_function_exists(sync_file_range HAVE_SYNC_FILE_RANGE)
check_function_exists(posix_fadvise HAVE_POSIX_FADVISE)
check_library_exists(dl dlopen "" NEED_LINK_DL)

# Configure the build.
message("The generated javah directory is=${GENERATED_JAVAH}")
message("binary dir is ${CMAKE_BINARY_DIR} and library output path=${LIBRARY_OUTPUT_PATH}")
include_directories(
    ${GENERATED_JAVAH}
    main/native/src
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/src
    ${CMAKE_BINARY_DIR}
    ${JNI_INCLUDE_DIRS}
    ${ISAL_INCLUDE_DIR}
    ${SRC}/util
)
configure_file(${CMAKE_SOURCE_DIR}/config.h.cmake ${CMAKE_BINARY_DIR}/config.h)

set(CMAKE_BUILD_WITH_INSTALL_RPATH TRUE)
hadoop_add_dual_library(alluxioec
	main/native/src/exception.c
    ${ISAL_SOURCE_FILES}
    ${SRC}/util/NativeCodeLoader.c
)
if(NEED_LINK_DL)
   set(LIB_DL dl)
endif()

hadoop_target_link_dual_libraries(alluxioec ${LIB_DL} ${JAVA_JVM_LIBRARY})
#set(LIBHADOOP_VERSION "1.0.0")
#set_target_properties(hadoop PROPERTIES SOVERSION ${LIBHADOOP_VERSION})
hadoop_dual_output_directory(alluxioec /usr/lib)

# By embedding '$ORIGIN' into the RPATH of libhadoop.so, dlopen will look in
# the directory containing libhadoop.so. However, $ORIGIN is not supported by
# all operating systems.
#if(${CMAKE_SYSTEM_NAME} MATCHES "Linux|SunOS")
#    set(RPATH "\$ORIGIN/")
#    if(EXTRA_LIBHADOOP_RPATH)
#        set(RPATH "${RPATH}:${EXTRA_LIBHADOOP_RPATH}/")
#    endif()
#    set_target_properties(hadoop PROPERTIES INSTALL_RPATH "${RPATH}")
#endif()


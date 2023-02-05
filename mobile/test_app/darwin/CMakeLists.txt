cmake_minimum_required(VERSION 3.1)

find_program(CCACHE_PROGRAM ccache)
if(CCACHE_PROGRAM)
    message(STATUS "Using ccache")
    set_property(GLOBAL PROPERTY RULE_LAUNCH_COMPILE "${CCACHE_PROGRAM}")
endif()

project(metrokit_testapp)

set(CMAKE_CXX_STANDARD 14)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -std=c14 -Wall -Wextra -Werror")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++14 -Wall -Wextra")
set(CMAKE_C_FLAGS_DEBUG "${CMAKE_C_FLAGS_DEBUG} -O0 -g")
set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -O0 -g")
set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} -O2")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O2")

set(ENV_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../../env)
set(PKG_ROOT ${ENV_DIR}/pkg-root)
set(METROKIT_ROOT ${CMAKE_CURRENT_SOURCE_DIR}/../../../metrokit)
set(PKG_ROOT_LOCAL ${PKG_ROOT}/local/darwin.x86-64)
set(FRAMEWORKS_PATH ${PKG_ROOT_LOCAL}/lib)
set(LIBS_PATH ${PKG_ROOT_LOCAL}/lib)
set(RPATH ${PKG_ROOT_LOCAL}/lib)

include_directories(SYSTEM ${PKG_ROOT}/include)
include_directories(SYSTEM ${PKG_ROOT_LOCAL}/include)

# FIXME: Framework headers should be included automatically by target_link_libraries
file(GLOB QT_FRAMEWORKS ${PKG_ROOT_LOCAL}/lib/Qt*.framework)
foreach(QT_FRAMEWORK ${QT_FRAMEWORKS})
    include_directories(SYSTEM ${QT_FRAMEWORK})
endforeach()

add_subdirectory(${METROKIT_ROOT} ${CMAKE_CURRENT_BINARY_DIR}/metrokit)

message(STATUS "Generating moc files")
set(QMOC_PATH ${PKG_ROOT_LOCAL}/bin/moc)
execute_process(COMMAND ${QMOC_PATH} ${CMAKE_CURRENT_SOURCE_DIR}/src/main.cpp -o ${CMAKE_CURRENT_SOURCE_DIR}/src/main.moc)

add_executable(app src/main.cpp)
target_link_libraries(app "-L${LIBS_PATH} -F${FRAMEWORKS_PATH} -rpath ${RPATH}")
target_link_libraries(app libmetrokit)

add_executable(testrunner ${PKG_ROOT}/lib/tools/gradle-build/lib-testapp/darwin/main.cpp)
target_link_libraries(testrunner "-Wl,-all_load -L${LIBS_PATH} -F${FRAMEWORKS_PATH} -rpath ${RPATH}")
target_link_libraries(testrunner libtest_metrokit)

# FIXME: Copying runtime and mapkit because it has no rpath specified
file(COPY ${PKG_ROOT_LOCAL}/lib/libruntime.dylib DESTINATION ${CMAKE_CURRENT_BINARY_DIR})
file(COPY ${PKG_ROOT_LOCAL}/lib/liblibs-mapkit.dylib DESTINATION ${CMAKE_CURRENT_BINARY_DIR})

# FIXME: Find a better way to export plugins
file(COPY ${PKG_ROOT_LOCAL}/plugins/platforms DESTINATION ${CMAKE_CURRENT_BINARY_DIR})

# Generate meta from IDL and copy headers to ${PKG_ROOT}/include
message(STATUS "Generating meta info")
execute_process(
        COMMAND ${PKG_ROOT}/set_environment.sh gradle pkg --no-daemon -PINSTALL
        WORKING_DIRECTORY ${METROKIT_ROOT})

# Copy resources to application binary folder after build
add_custom_command(
        TARGET app
        POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E remove_directory ${CMAKE_CURRENT_BINARY_DIR}/res
        COMMAND ${CMAKE_COMMAND} -E copy_directory ${CMAKE_CURRENT_SOURCE_DIR}/src/res ${CMAKE_CURRENT_BINARY_DIR}/res
        COMMENT "Copying resources to build directory")

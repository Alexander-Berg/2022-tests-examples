cmake_minimum_required(VERSION 3.5.1)

project(navi_tests)

string(TOLOWER ${CMAKE_SYSTEM_NAME} PLATFORM)

# Set MAPKIT_ARTIFACTS
include("../../../../../tools/cmake/utils/setup.cmake")
detect_mapkit_artifacts(MAPKIT_ARTIFACTS MAPKIT_PLATFORM_ARTIFACTS x86-64)

add_definitions(-DTESTING_ENV)

include("../../../../../tools/cmake/utils/setup_compiler_flags.cmake")

if(CMAKE_SYSTEM_NAME MATCHES Linux)
    add_compile_cxx_only_options(-nostdinc++)
endif()

include_directories(SYSTEM
${MAPKIT_ARTIFACTS}/include
${MAPKIT_ARTIFACTS}/include/contrib/libs/eigen
${MAPKIT_PLATFORM_ARTIFACTS}/include
)

set(CMAKE_PREFIX_PATH ${MAPKIT_PLATFORM_ARTIFACTS})
find_package(Qt5 COMPONENTS Core REQUIRED)

add_subdirectory(../../lib lib)
add_subdirectory(../../navikit navikit)
add_subdirectory(../../naviprovider naviprovider)
add_subdirectory(.. navi)
add_subdirectory(../environment_test navi/environment_test)

set(SOURCES
    ut_main.cpp
)

add_executable(navi_tests ${SOURCES})

if(CMAKE_SYSTEM_NAME MATCHES Linux)
    target_link_libraries(navi_tests -Wl,--whole-archive)
endif()

macro(add_tests TARGET FOLDER)
    foreach(MODULE ${ARGN})
        add_subdirectory(../../${FOLDER}/${MODULE}/tests ${FOLDER}/${MODULE})
        target_link_libraries(${TARGET} test_${FOLDER}_${MODULE})
    endforeach()
endmacro()

add_tests(navi_tests navi
#    ads
#    audio_focus
#    bookmarks
#    common
#    gas_stations
#    intent_parser
#    interaction
#    navikit
#    passport
#    places
#    points_history
#    profiling
#    settings
#    scheme_parser
#    search
#    special_projects
#    ui
)

add_tests(navi_tests navikit
#    activity_tracking
#    auth
#    common
#    destination_suggest
#    map
#    providers
#    report
#    road_events
#    route_editor
#    routing
#    subscriptions
#    sync
)

add_tests(navi_tests naviprovider
    naviprovider
)

target_link_libraries(navi_tests
    naviprovider
    navi
    navikit
    module_environment_test
)

if(CMAKE_SYSTEM_NAME MATCHES Linux)
    target_link_libraries(navi_tests -Wl,--no-whole-archive)
endif()

target_link_libraries(navi_tests
    "-L${MAPKIT_PLATFORM_ARTIFACTS}/lib"
    maps-mobile
    Qt5::Core
)

if(CMAKE_SYSTEM_NAME MATCHES Darwin)
    target_link_libraries(navi_tests
        "-Wl,-all_load -iframework ${MAPKIT_PLATFORM_ARTIFACTS}/lib -framework OpenAL -framework OpenGL -rpath ${MAPKIT_PLATFORM_ARTIFACTS}/lib"
    )

    add_custom_command(TARGET navi_tests POST_BUILD
        COMMAND install_name_tool -change libmaps-mobile.dylib @rpath/libmaps-mobile.dylib $<TARGET_FILE:navi_tests>
    )

    set(LD_PREFIX "DY")
endif()

if(CMAKE_SYSTEM_NAME MATCHES Linux)
    target_link_libraries(navi_tests "-lpthread -lGL -ldl")

    set(LD_PREFIX "")
endif()

add_custom_target(run_navi_tests
    COMMAND env ${LD_PREFIX}LD_LIBRARY_PATH=${MAPKIT_PLATFORM_ARTIFACTS}/lib $<TARGET_FILE:navi_tests>
    DEPENDS navi_tests
    USES_TERMINAL
)

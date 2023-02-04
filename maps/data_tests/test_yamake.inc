YT_UNITTEST()

PEERDIR(
    library/cpp/testing/gmock_in_unittest
    library/cpp/testing/unittest
    mapreduce/yt/client
    mapreduce/yt/tests/yt_unittest_main
    maps/renderer/denormalization/lib/tasks/tests/data_tests
)

SRCS(
    test.cpp
)

DATA(
    arcadia/maps/doc/schemas/ymapsdf/garden/json
    arcadia/maps/renderer/denormalization/test_data
    arcadia/maps/renderer/denormalization/lib/tasks/tests/data_tests
)

DEPENDS(
    maps/renderer/denormalization/test_data
)

INCLUDE(${ARCADIA_ROOT}/mapreduce/yt/python/recipe/recipe.inc)

SIZE(MEDIUM)
TAG(slow)

REQUIREMENTS(ram:17)

END()

#include <library/cpp/testing/unittest/registar.h>

#include <maps/renderer/denormalization/lib/tasks/tests/data_tests/common.h>

namespace maps::renderer::denormalization {

Y_UNIT_TEST_SUITE(data_tests) {

Y_UNIT_TEST_F(hd_road_marking_test, DataTestFixture)
{
    run("hd_road_marking_test",
        TestType::ExpectedData,
        {"HdRoadMarkingLQueryTmpTask", "HdRoadMarkingLTask"});
}

} // Y_UNIT_TEST_SUITE(data_tests)

} // namespace maps::renderer::denormalization

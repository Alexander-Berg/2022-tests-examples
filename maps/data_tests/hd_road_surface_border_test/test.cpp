#include <library/cpp/testing/unittest/registar.h>

#include <maps/renderer/denormalization/lib/tasks/tests/data_tests/common.h>

namespace maps::renderer::denormalization {

Y_UNIT_TEST_SUITE(data_tests) {

Y_UNIT_TEST_F(hd_road_surface_border_test, DataTestFixture)
{
    run("hd_road_surface_border_test",
        TestType::ExpectedData,
        {"HdRoadSurfaceTask", "HdRoadSurfaceBorderTask"});
}

} // Y_UNIT_TEST_SUITE(data_tests)

} // namespace maps::renderer::denormalization

#include <library/cpp/testing/unittest/registar.h>

#include <maps/renderer/denormalization/lib/tasks/tests/data_tests/common.h>

namespace maps::renderer::denormalization {

Y_UNIT_TEST_SUITE(data_tests) {

Y_UNIT_TEST_F(indoor_plan_data_test, DataTestFixture)
{
    run("indoor_plan_data_test",
        TestType::ExpectedData,
        {"IndoorPlanDataQueryTmpTask", "IndoorPlanDataTask"});
}

} // Y_UNIT_TEST_SUITE(data_tests)

} // namespace maps::renderer::denormalization

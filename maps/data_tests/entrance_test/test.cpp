#include <library/cpp/testing/unittest/registar.h>

#include <maps/renderer/denormalization/lib/tasks/tests/data_tests/common.h>

namespace maps::renderer::denormalization {

Y_UNIT_TEST_SUITE(data_tests) {

Y_UNIT_TEST_F(entrance_test, DataTestFixture)
{
    run("entrance_test",
        TestType::ExpectedData,
        {
            "EntranceGeomQueryTmpTask",
            "EntranceBldTmpTask",
            "EntranceQueryTmpTask",
            "EntranceTask",
        });
}

} // Y_UNIT_TEST_SUITE(data_tests)

} // namespace maps::renderer::denormalization

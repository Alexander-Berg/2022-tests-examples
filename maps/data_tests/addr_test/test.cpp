#include <library/cpp/testing/unittest/registar.h>

#include <maps/renderer/denormalization/lib/tasks/tests/data_tests/common.h>

namespace maps::renderer::denormalization {

Y_UNIT_TEST_SUITE(data_tests) {

Y_UNIT_TEST_F(addr_test, DataTestFixture)
{
    run("addr_test",
        TestType::ExpectedData,
        {"AddrQueryTmpTask", "AddrQueryBldTmpTask", "AddrQueryPoiTmpTask", "AddrTask"});
}

} // Y_UNIT_TEST_SUITE(data_tests)

} // namespace maps::renderer::denormalization

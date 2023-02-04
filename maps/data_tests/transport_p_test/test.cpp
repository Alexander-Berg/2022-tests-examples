#include <library/cpp/testing/unittest/registar.h>

#include <maps/renderer/denormalization/lib/tasks/tests/data_tests/common.h>

namespace maps::renderer::denormalization {

Y_UNIT_TEST_SUITE(data_tests) {

Y_UNIT_TEST_F(transport_p_test, DataTestFixture)
{
    run("transport_p_test",
        TestType::ExpectedData,
        {"FtSourceTmpTask",
         "FtUriTmpTask",
         "MtrTmpTask",
         "TransportQueryTmpTask",
         "TransportTmpTask",
         "TransportPointTask"});
}

} // Y_UNIT_TEST_SUITE(data_tests)

} // namespace maps::renderer::denormalization

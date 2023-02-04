#include <library/cpp/testing/unittest/registar.h>

#include <maps/renderer/denormalization/lib/tasks/tests/data_tests/common.h>

namespace maps::renderer::denormalization {

Y_UNIT_TEST_SUITE(data_tests) {

Y_UNIT_TEST_F(ft_nm_tmp_test, DataTestFixture)
{
    run("ft_nm_tmp_test", TestType::ExpectedData, {"FtNmQueryTmpTask", "FtNmTmpTask"});
}

} // Y_UNIT_TEST_SUITE(data_tests)

} // namespace maps::renderer::denormalization

#include <library/cpp/testing/unittest/registar.h>

#include <maps/renderer/denormalization/lib/tasks/tests/data_tests/common.h>

namespace maps::renderer::denormalization {

Y_UNIT_TEST_SUITE(data_tests) {

Y_UNIT_TEST_F(ad_non_recognition_tmp_test, DataTestFixture)
{
    run("ad_non_recognition_tmp_test", TestType::ExpectedData, {"AdNonRecognitionTmpTask"});
}

} // Y_UNIT_TEST_SUITE(data_tests)

} // namespace maps::renderer::denormalization

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/system/fstat.h>

namespace NInfra::NServiceController::NDaemonTest {

Y_UNIT_TEST_SUITE(ServiceControllerDaemonTest) {

Y_UNIT_TEST(TestDiskUsage) {
    TFileStat stat(BinaryPath("infra/service_controller/daemons/service_controller/service_controller"));

    UNIT_ADD_METRIC("disk_usage", stat.Size);
    UNIT_ASSERT_C(stat.Size < 1500 * ((size_t)1 << 20), stat.Size);  // 1500MB
}

}

} // namespace NInfra::NServiceController::NDaemonTest

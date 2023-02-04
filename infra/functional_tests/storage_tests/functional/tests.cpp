#include <infra/yp_service_discovery/functional_tests/common/storage_tests/common.h>

using namespace NJson;
using namespace NInfra;
using namespace NYP;
using namespace NServiceDiscovery;

Y_UNIT_TEST_SUITE(Functional) {
    Y_UNIT_TEST(TestRunAndShutdown) {
        InitEndpointStorages();
        InitPodStorages();
        InitNodeStorages();
        RunDaemon(GetPatchedConfigOptions());

        UNIT_ASSERT_VALUES_EQUAL(Ping().Content, EXPECTED_PING_RESPONSE);

        StopDaemon();
    }
}

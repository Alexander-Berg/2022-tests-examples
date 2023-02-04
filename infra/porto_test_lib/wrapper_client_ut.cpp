#include "wrapper_client.h"

#include <infra/pod_agent/libs/porto_client/mock_client.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

namespace NInfra::NPodAgent::NWrapperPortoClientTest {

TLoggerConfig GetLoggerConfig() {
    TLoggerConfig result;
    result.SetBackend(TLoggerConfig_ELogBackend_STDERR);
    result.SetLevel("ERROR");
    return result;
}

static TLogger logger(GetLoggerConfig());

Y_UNIT_TEST_SUITE(TestWrapperPortoClient) {

Y_UNIT_TEST(CreateClient) {
    UNIT_ASSERT_NO_EXCEPTION(TWrapperPortoClient(new TMockPortoClient()));
}

Y_UNIT_TEST(SwitchLogFrame) {
    class TMyPortoClient: public TWrapperPortoClient {
    public:
        TMyPortoClient(TPortoClientPtr client)
            : TWrapperPortoClient(client)
        {}
    };

    {
        TPortoClientPtr client = new TWrapperPortoClient(new TMockPortoClient());
        UNIT_ASSERT_UNEQUAL_C(dynamic_cast<TWrapperPortoClient*>(client->SwitchLogFrame(logger.SpawnFrame()).Get()), nullptr, "bad object type");
        UNIT_ASSERT_EQUAL_C(dynamic_cast<TMyPortoClient*>(client->SwitchLogFrame(logger.SpawnFrame()).Get()), nullptr, "bad object type");
    }
    {
        TPortoClientPtr client = new TMyPortoClient(new TMockPortoClient());
        UNIT_ASSERT_UNEQUAL_C(dynamic_cast<TWrapperPortoClient*>(client->SwitchLogFrame(logger.SpawnFrame()).Get()), nullptr, "bad object type");
        UNIT_ASSERT_UNEQUAL_C(dynamic_cast<TMyPortoClient*>(client->SwitchLogFrame(logger.SpawnFrame()).Get()), nullptr, "bad object type");
    }
}

Y_UNIT_TEST(BasicFunctions) {
    TPortoClientPtr client = new TWrapperPortoClient(new TMockPortoClient());
    TPortoContainerName containerName("test");

    client->Start(containerName).Success();
    client->Stop(containerName).Success();
}

}

} // NInfra::NPodAgent::NWrapperPortoClientTest


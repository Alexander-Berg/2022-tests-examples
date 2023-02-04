#include "tvm.h"

#include <maps/renderer/staticapi/yacare/tests/test_utils/apikeys_mock_server.h>
#include <maps/renderer/staticapi/yacare/tests/test_utils/mock_servers.h>
#include <maps/renderer/staticapi/yacare/tests/test_utils/test_utils.h>

#include <maps/infra/yacare/include/test_utils.h>

#include <library/cpp/testing/gtest/gtest.h>

using namespace testing;

namespace maps::staticapi::tests {

namespace {

http::MockResponse callEnterpriseApi(const std::string& path)
{
    http::MockRequest req(http::GET, "http://localhost" + path);
    req.headers["Host"] = "enterprise.static-maps.yandex.ru";
    return yacare::performTestRequest(req);
}

} // namespace

TEST(enterprise, valid_key)
{
    auto srv = makeAllMockServers();
    ApikeysMockServer apikeys;

    auto res = callEnterpriseApi("/1.x/?ll=30.3,60&z=9&l=map&key=" + APIKEYS_TEST_KEY_ACTIVE);
    EXPECT_EQ(res.status, 200);

    auto stats = apikeys.waitUseCounterAndStop(1);
    EXPECT_EQ(stats.useCounter, 1);

    std::string commonParams =
        "?service_token=" + apikeysServiceToken() +
        "&key=" + APIKEYS_TEST_KEY_ACTIVE;
    EXPECT_THAT(stats.urls, ElementsAre(
        APIKEYS_CHECK_KEY_URL + commonParams + "&user_ip=127.0.0.1",
        APIKEYS_UPDATE_COUNTERS_URL + commonParams + "&hits=1"));

    auto apikeysTvmTicket = tvmTicketFor("apikeys");
    EXPECT_THAT(stats.serviceTickets, ElementsAre(apikeysTvmTicket, apikeysTvmTicket));
}

TEST(enterprise, invalid_key)
{
    auto srv = makeAllMockServers();
    ApikeysMockServer apikeys;

    auto res = callEnterpriseApi("/1.x/?ll=30.3,60&z=9&l=map&key=" + APIKEYS_TEST_KEY_BANNED);
    EXPECT_EQ(res.status, 401);

    res = callEnterpriseApi("/1.x/?ll=30.3,60&z=9&l=map&key=" + APIKEYS_TEST_KEY_MISSING);
    EXPECT_EQ(res.status, 401);

    res = callEnterpriseApi("/1.x/?ll=30.3,60&z=9&l=map");
    EXPECT_EQ(res.status, 401);
    EXPECT_THAT(res.body, HasSubstr("Empty key is not allowed"));

    auto stats = apikeys.waitUseCounterAndStop(0);
    EXPECT_EQ(stats.useCounter, 0);
}

TEST(enterprise, apikeys_unavailable)
{
    auto srv = makeAllMockServers();
    ApikeysMockServer apikeys;

    auto res = callEnterpriseApi("/1.x/?ll=30.3,60&z=9&l=map&key=" + APIKEYS_TEST_KEY_500_CHECK_KEY);
    EXPECT_EQ(res.status, 200);

    res = callEnterpriseApi("/1.x/?ll=30.3,60&z=9&l=map&key=" + APIKEYS_TEST_KEY_500_UPDATE_COUNTERS);
    EXPECT_EQ(res.status, 200);

    auto stats = apikeys.waitUseCounterAndStop(2);
    EXPECT_EQ(stats.useCounter, 2);
}

} // namespace maps::staticapi::tests

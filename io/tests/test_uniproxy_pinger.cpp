#include <yandex_io/services/aliced/uniproxy_pinger/uniproxy_pinger.h>

#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <json/value.h>

#include <library/cpp/testing/unittest/registar.h>

#include <chrono>
#include <optional>

using namespace quasar;

struct UniProxyPingerFixture: public QuasarUnitTestFixture {};

Y_UNIT_TEST_SUITE_F(UniProxyPingerTest, UniProxyPingerFixture) {
    Y_UNIT_TEST(ParseSettingsTest) {
        {
            const Json::Value settings{Json::arrayValue};
            UNIT_ASSERT_EQUAL(UniProxyPinger::parseSettings(settings), std::nullopt);
        }

        {
            const UniProxyPinger::Settings defaultSettings;
            const Json::Value settings{Json::objectValue};
            UNIT_ASSERT_EQUAL(*UniProxyPinger::parseSettings(settings), defaultSettings);
        }

        {
            using namespace std::chrono_literals;

            const UniProxyPinger::Settings nondefaultSettings{
                .url = "some_url",
                .interval = 24s,
                .timeout = 12s,
                .parallelPings = 3,
                .payloadBytes = 48,
                .retriesCount = 6,
            };

            Json::Value jsonSettings{Json::objectValue};
            jsonSettings["url"] = nondefaultSettings.url;
            jsonSettings["timeoutMs"] = static_cast<int>(nondefaultSettings.timeout.count());
            jsonSettings["intervalMs"] = static_cast<int>(nondefaultSettings.interval.count());
            jsonSettings["parallelPings"] = nondefaultSettings.parallelPings;
            jsonSettings["payloadBytes"] = nondefaultSettings.payloadBytes;
            jsonSettings["retriesCount"] = nondefaultSettings.retriesCount;

            UNIT_ASSERT_EQUAL(*UniProxyPinger::parseSettings(jsonSettings), nondefaultSettings);
        }
    }

    Y_UNIT_TEST(CreationTest) {
        const Json::Value jsonSettings{Json::objectValue};
        UniProxyPinger pinger{*UniProxyPinger::parseSettings(jsonSettings), getDeviceForTests()};
    }
}

#include <yandex_io/libs/device/device.h>

#include <yandex_io/libs/configuration/configuration.h>
#include <yandex_io/libs/json_utils/json_utils.h>

#include <yandex_io/tests/testlib/test_utils.h>

#include <library/cpp/testing/unittest/registar.h>

#include <unordered_map>

using namespace YandexIO;

Y_UNIT_TEST_SUITE(DeviceUserAgents) {
    Y_UNIT_TEST(testDefaultConfiguredUserAgentGeneration) {
        const std::string config = R"~({"common": {
        "deviceType": "unknown",
        "softwareVersion": "randomGarbageVersion"
        }})~";

        auto device = std::make_shared<Device>("device_id", // deviceId
                                               std::make_unique<Configuration>(quasar::parseJson(config)),
                                               nullptr, // telemetry
                                               nullptr  // HAL
        );

        UNIT_ASSERT_VALUES_EQUAL(device->getUserAgent(), "Mozilla\\/5.0 (Linux; Android 6.0.1; Station Build\\/MOB30J; wv) AppleWebKit\\/537.36 (KHTML, like Gecko) Version\\/4.0 Chrome\\/61.0.3163.98 Safari\\/537.36 YandexStation\\/2.3.0.3.373060213.20190204.develop.ENG (YandexIO)");
        UNIT_ASSERT_VALUES_EQUAL(device->getDeprecatedUserAgent(), "unknown/randomGarbageVersion");
    }

    Y_UNIT_TEST(testConfiguredUserAgentOverride) {
        const std::string config = R"~({"common": {
        "userAgent": "UnknownBrowser/1.0 YandexStation/100500.unknown (Yandex IO) __QUASAR_VERSION_PLACEHOLDER__",
        "deviceType": "unknown",
        "softwareVersion": "randomGarbageVersion"
        }})~";

        auto device = std::make_shared<Device>("device_id", // deviceId
                                               std::make_unique<Configuration>(quasar::parseJson(config)),
                                               nullptr, // telemetry
                                               nullptr  // HAL
        );
        UNIT_ASSERT_VALUES_EQUAL(device->getUserAgent(), "UnknownBrowser/1.0 YandexStation/100500.unknown (Yandex IO) randomGarbageVersion");
        UNIT_ASSERT_VALUES_EQUAL(device->getDeprecatedUserAgent(), "unknown/randomGarbageVersion");
    }
}

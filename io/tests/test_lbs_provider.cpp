#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/http_client/mock/http_client.h>
#include <yandex_io/modules/geolocation/providers/lbs_provider.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;
using namespace testing;
using namespace YandexIO;

Y_UNIT_TEST_SUITE(testLbsProvider) {
    Y_UNIT_TEST(testRequest) {
        const auto mockHttpClient = std::make_shared<mock::MockHttpClient>();
        const auto provider = LbsProvider(mockHttpClient, "https://api.lbs.yandex.net", "yandexstation", "DEVICE_ID");

        EXPECT_CALL(*mockHttpClient, post)
            .WillOnce([](std::string_view tag, const std::string& url, const std::string& data, const IHttpClient::Headers& headers) {
                UNIT_ASSERT_VALUES_EQUAL(tag, "geolocation");
                UNIT_ASSERT_VALUES_EQUAL(url, "https://api.lbs.yandex.net/geolocation?yandex_io_platform=yandexstation&yandex_io_device_id=DEVICE_ID");
                UNIT_ASSERT(headers.contains("Content-Type"));
                UNIT_ASSERT_VALUES_EQUAL(headers.at("Content-Type"), "application/json");

                UNIT_ASSERT_VALUES_EQUAL(data.substr(0, 5), "json=");
                const auto request = parseJson(data.substr(5));
                UNIT_ASSERT(!request["common"]["version"].isNull());
                UNIT_ASSERT(!request["common"]["api_key"].isNull());
                UNIT_ASSERT(request["wifi_networks"].isArray());
                UNIT_ASSERT_VALUES_EQUAL(request["wifi_networks"].size(), 4);
                UNIT_ASSERT_VALUES_EQUAL(tryGetString(request["wifi_networks"][0], "mac", ""), "50:ff:20:67:c0:69");
                UNIT_ASSERT_VALUES_EQUAL(tryGetInt(request["wifi_networks"][0], "signal_strength", 0), -75);
                UNIT_ASSERT_VALUES_EQUAL(tryGetString(request["wifi_networks"][1], "mac", ""), "0c:68:03:af:e5:ce");
                UNIT_ASSERT_VALUES_EQUAL(tryGetInt(request["wifi_networks"][1], "signal_strength", 0), -76);
                UNIT_ASSERT_VALUES_EQUAL(tryGetString(request["wifi_networks"][2], "mac", ""), "0c:68:03:ae:2a:bf");
                UNIT_ASSERT_VALUES_EQUAL(tryGetInt(request["wifi_networks"][2], "signal_strength", 0), -55);
                UNIT_ASSERT_VALUES_EQUAL(tryGetString(request["wifi_networks"][3], "mac", ""), "70:8b:cd:c6:bf:e8");
                UNIT_ASSERT_VALUES_EQUAL(tryGetInt(request["wifi_networks"][3], "signal_strength", 0), -68);

                return IHttpClient::HttpResponse(
                    200,
                    "application/json",
                    "{\n"
                    "   \"position\":\n"
                    "      {\n"
                    "         \"latitude\":37.57070580712891,\n"
                    "         \"longitude\":126.97694609887695,\n"
                    "         \"altitude\":0.0,\n"
                    "         \"precision\":140.0,\n"
                    "         \"altitude_precision\":30.0,\n"
                    "         \"type\":\"wifi\"\n"
                    "      }\n"
                    "}");
            });

        const auto location = provider.request(
            {{.rssi = -75,
              .mac = "50:ff:20:67:c0:69"},
             {.rssi = -76,
              .mac = "0c:68:03:af:e5:ce"},
             {.rssi = -55,
              .mac = "0c:68:03:ae:2a:bf"},
             {.rssi = -68,
              .mac = "70:8b:cd:c6:bf:e8"}});

        UNIT_ASSERT(location.has_value());
        UNIT_ASSERT(location->precision.has_value());
        UNIT_ASSERT_VALUES_EQUAL(location->latitude, 37.57070580712891);
        UNIT_ASSERT_VALUES_EQUAL(location->longitude, 126.97694609887695);
        UNIT_ASSERT_VALUES_EQUAL(location->precision.value(), 140.0);
    }
}

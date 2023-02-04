#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/http_client/mock/http_client.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/telemetry/mock/mock_telemetry.h>
#include <yandex_io/modules/geolocation/geolocation.h>
#include <yandex_io/protos/model_objects.pb.h>
#include <yandex_io/sdk/sdk_state.h>
#include <yandex_io/tests/testlib/test_callback_queue.h>
#include <yandex_io/tests/testlib/null_sdk/null_sdk_interface.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/folder/path.h>

#include <fstream>
#include <memory>

using namespace quasar;
using namespace testing;
using namespace YandexIO;

namespace {

    class TestGeolocationListener: public Geolocation::IListener {
    public:
        void onLocationChanged(const Location& location) override {
            location_ = location;
        }

        void onTimezoneChanged(const Timezone& timezone) override {
            timezone_ = timezone;
        }

        Location getLocation() {
            return location_;
        }

        Timezone getTimezone() {
            return timezone_;
        }

    private:
        Location location_;
        Timezone timezone_;
    };

    class GeolocationTestFixture: public QuasarUnitTestFixtureWithoutIpc {
    public:
        void SetUp(NUnitTest::TTestContext& context) override {
            QuasarUnitTestFixtureWithoutIpc::SetUp(context);
            enableLoggingToTelemetry(mockTelemetry);

            testCallbackQueue = std::make_shared<TestCallbackQueue>();
            mockHttpClient = std::make_shared<mock::MockHttpClient>();

            quasarDir = JoinFsPaths(tryGetRamDrivePath(), "quasar-" + quasar::makeUUID());
            locationBackupPath = JoinFsPaths(quasarDir, "location.dat");
            timezoneBackupPath = JoinFsPaths(quasarDir, "timezone.dat");
            quasarDir.ForceDelete();
            quasarDir.MkDirs();

            Geolocation::Settings settings;
            settings.deviceType = getString(getDeviceForTests()->configuration()->getServiceConfig("common"), "deviceType");
            settings.deviceId = QuasarUnitTestFixtureWithoutIpc::makeTestDeviceId();
            settings.lbsBackendUrl = "https://api.lbs.yandex.net";
            settings.quasarBackendUrl = "https://quasar.yandex.net";
            settings.backupLocationPath = locationBackupPath;
            settings.backupTimezonePath = timezoneBackupPath;

            geolocation = Geolocation::create(
                std::make_shared<NullSDKInterface>(),
                testCallbackQueue,
                mockHttpClient,
                settings);

            listener = std::make_shared<TestGeolocationListener>();
            geolocation->addListener(listener);
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            quasarDir.ForceDelete();
            Base::TearDown(context);
        }

    public:
        std::shared_ptr<MockTelemetry> mockTelemetry = std::make_shared<StrictMock<MockTelemetry>>();
        std::shared_ptr<TestCallbackQueue> testCallbackQueue;
        std::shared_ptr<mock::MockHttpClient> mockHttpClient;
        std::shared_ptr<Geolocation> geolocation;
        std::shared_ptr<TestGeolocationListener> listener;
        TFsPath quasarDir;
        std::string locationBackupPath;
        std::string timezoneBackupPath;
    };

} // anonymous namespace

Y_UNIT_TEST_SUITE_F(GeolocationTest, GeolocationTestFixture) {
    Y_UNIT_TEST(testScheduleUpdate) {
        SDKState state = {
            .wifiList = {
                {.rssi = -75,
                 .mac = "50:ff:20:67:c0:69"},
                {.rssi = -76,
                 .mac = "0c:68:03:af:e5:ce"},
                {.rssi = -55,
                 .mac = "0c:68:03:ae:2a:bf"},
                {.rssi = -68,
                 .mac = "70:8b:cd:c6:bf:e8"}}};

        EXPECT_CALL(*mockHttpClient, post)
            .WillOnce([state](std::string_view tag, const std::string& url, const std::string& data, const IHttpClient::Headers& headers) {
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
                UNIT_ASSERT_VALUES_EQUAL(tryGetString(request["wifi_networks"][0], "mac", ""), state.wifiList[0].mac);
                UNIT_ASSERT_VALUES_EQUAL(tryGetInt(request["wifi_networks"][0], "signal_strength", 0), state.wifiList[0].rssi);
                UNIT_ASSERT_VALUES_EQUAL(tryGetString(request["wifi_networks"][1], "mac", ""), state.wifiList[1].mac);
                UNIT_ASSERT_VALUES_EQUAL(tryGetInt(request["wifi_networks"][1], "signal_strength", 0), state.wifiList[1].rssi);
                UNIT_ASSERT_VALUES_EQUAL(tryGetString(request["wifi_networks"][2], "mac", ""), state.wifiList[2].mac);
                UNIT_ASSERT_VALUES_EQUAL(tryGetInt(request["wifi_networks"][2], "signal_strength", 0), state.wifiList[2].rssi);
                UNIT_ASSERT_VALUES_EQUAL(tryGetString(request["wifi_networks"][3], "mac", ""), state.wifiList[3].mac);
                UNIT_ASSERT_VALUES_EQUAL(tryGetInt(request["wifi_networks"][3], "signal_strength", 0), state.wifiList[3].rssi);

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

        EXPECT_CALL(*mockHttpClient, get)
            .WillOnce([](std::string_view tag, const std::string& url, const IHttpClient::Headers& headers) {
                UNIT_ASSERT_VALUES_EQUAL(tag, "get-timezone");
                UNIT_ASSERT_VALUES_EQUAL(url, "https://quasar.yandex.net/get_timezone?lat=37.5707&lon=126.977");
                UNIT_ASSERT(headers.empty());

                return IHttpClient::HttpResponse(
                    200,
                    "application/json",
                    "{\n"
                    "   \"offset_sec\":32400,\n"
                    "   \"status\":\"ok\",\n"
                    "   \"timezone\":\"Asia/Seoul\"\n"
                    "}");
            });

        geolocation->onSDKState(state);

        UNIT_ASSERT_VALUES_EQUAL(listener->getLocation().latitude, 37.57070580712891);
        UNIT_ASSERT_VALUES_EQUAL(listener->getLocation().longitude, 126.97694609887695);
        UNIT_ASSERT_VALUES_EQUAL(listener->getLocation().precision.value_or(0), 140.0);

        UNIT_ASSERT_VALUES_EQUAL(listener->getTimezone().timezoneName, "Asia/Seoul");
        UNIT_ASSERT_VALUES_EQUAL(listener->getTimezone().timezoneOffsetSec, 32400);
    }

    Y_UNIT_TEST(testLbsBackendTimeout) {
        const auto defaultLocation = listener->getLocation();
        const auto defaultTimezone = listener->getTimezone();

        SDKState state = {
            .wifiList = {{.rssi = -74,
                          .mac = "8e:7e:2c:a5:56:d3"}}};

        EXPECT_CALL(*mockHttpClient, post)
            .WillOnce([state](std::string_view tag, const std::string& url, const std::string& data, const IHttpClient::Headers& headers) {
                UNIT_ASSERT_VALUES_EQUAL(tag, "geolocation");
                UNIT_ASSERT_VALUES_EQUAL(url, "https://api.lbs.yandex.net/geolocation?yandex_io_platform=yandexstation&yandex_io_device_id=DEVICE_ID");
                UNIT_ASSERT(headers.contains("Content-Type"));
                UNIT_ASSERT_VALUES_EQUAL(headers.at("Content-Type"), "application/json");

                UNIT_ASSERT_VALUES_EQUAL(data.substr(0, 5), "json=");
                const auto request = parseJson(data.substr(5));
                UNIT_ASSERT(!request["common"]["version"].isNull());
                UNIT_ASSERT(!request["common"]["api_key"].isNull());
                UNIT_ASSERT(request["wifi_networks"].isArray());
                UNIT_ASSERT_VALUES_EQUAL(request["wifi_networks"].size(), 1);
                UNIT_ASSERT_VALUES_EQUAL(tryGetString(request["wifi_networks"][0], "mac", ""), state.wifiList[0].mac);
                UNIT_ASSERT_VALUES_EQUAL(tryGetInt(request["wifi_networks"][0], "signal_strength", 0), state.wifiList[0].rssi);

                return IHttpClient::HttpResponse(408, "application/json", "{}");
            });

        EXPECT_CALL(*mockHttpClient, get).Times(0);

        geolocation->onSDKState(state);

        // Geolocation and timezone will not change in the case of backend error
        UNIT_ASSERT(listener->getLocation() == defaultLocation);
        UNIT_ASSERT(listener->getTimezone() == defaultTimezone);
    }

    Y_UNIT_TEST(testQuasarBackendTimeout) {
        const auto defaultLocation = listener->getLocation();
        const auto defaultTimezone = listener->getTimezone();

        SDKState state = {
            .wifiList = {{.rssi = -74,
                          .mac = "8e:7e:2c:a5:56:d3"}}};

        EXPECT_CALL(*mockHttpClient, post)
            .WillOnce([state](std::string_view tag, const std::string& url, const std::string& data, const IHttpClient::Headers& headers) {
                UNIT_ASSERT_VALUES_EQUAL(tag, "geolocation");
                UNIT_ASSERT_VALUES_EQUAL(url, "https://api.lbs.yandex.net/geolocation?yandex_io_platform=yandexstation&yandex_io_device_id=DEVICE_ID");
                UNIT_ASSERT(headers.contains("Content-Type"));
                UNIT_ASSERT_VALUES_EQUAL(headers.at("Content-Type"), "application/json");

                UNIT_ASSERT_VALUES_EQUAL(data.substr(0, 5), "json=");
                const auto request = parseJson(data.substr(5));
                UNIT_ASSERT(!request["common"]["version"].isNull());
                UNIT_ASSERT(!request["common"]["api_key"].isNull());
                UNIT_ASSERT(request["wifi_networks"].isArray());
                UNIT_ASSERT_VALUES_EQUAL(request["wifi_networks"].size(), 1);
                UNIT_ASSERT_VALUES_EQUAL(tryGetString(request["wifi_networks"][0], "mac", ""), state.wifiList[0].mac);
                UNIT_ASSERT_VALUES_EQUAL(tryGetInt(request["wifi_networks"][0], "signal_strength", 0), state.wifiList[0].rssi);

                return IHttpClient::HttpResponse(
                    200,
                    "application/json",
                    "{\n"
                    "   \"position\":\n"
                    "      {\n"
                    "         \"latitude\":60.32444381713867,\n"
                    "         \"longitude\":24.62943458557129,\n"
                    "         \"altitude\":0.0,\n"
                    "         \"precision\":100000.0,\n"
                    "         \"altitude_precision\":30.0,\n"
                    "         \"type\":\"ip\"\n"
                    "      }\n"
                    "}");
            });

        EXPECT_CALL(*mockHttpClient, get)
            .WillOnce([](std::string_view tag, const std::string& url, const IHttpClient::Headers& headers) {
                UNIT_ASSERT_VALUES_EQUAL(tag, "get-timezone");
                UNIT_ASSERT_VALUES_EQUAL(url, "https://quasar.yandex.net/get_timezone?lat=60.3244&lon=24.6294");
                UNIT_ASSERT(headers.empty());

                return IHttpClient::HttpResponse(408, "application/json", "{}");
            });

        geolocation->onSDKState(state);

        // Geolocation and timezone will not change in the case of backend error
        UNIT_ASSERT(listener->getLocation() == defaultLocation);
        UNIT_ASSERT(listener->getTimezone() == defaultTimezone);
    }

    Y_UNIT_TEST(testQuasarBackendServerErrors) {
        const auto defaultLocation = listener->getLocation();
        const auto defaultTimezone = listener->getTimezone();

        SDKState state = {
            .wifiList = {{.rssi = -74,
                          .mac = "8e:7e:2c:a5:56:d3"}}};

        EXPECT_CALL(*mockHttpClient, post)
            .WillRepeatedly([state](std::string_view tag, const std::string& url, const std::string& data, const IHttpClient::Headers& headers) {
                UNIT_ASSERT_VALUES_EQUAL(tag, "geolocation");
                UNIT_ASSERT_VALUES_EQUAL(url, "https://api.lbs.yandex.net/geolocation?yandex_io_platform=yandexstation&yandex_io_device_id=DEVICE_ID");
                UNIT_ASSERT(headers.contains("Content-Type"));
                UNIT_ASSERT_VALUES_EQUAL(headers.at("Content-Type"), "application/json");

                UNIT_ASSERT_VALUES_EQUAL(data.substr(0, 5), "json=");
                const auto request = parseJson(data.substr(5));
                UNIT_ASSERT(!request["common"]["version"].isNull());
                UNIT_ASSERT(!request["common"]["api_key"].isNull());
                UNIT_ASSERT(request["wifi_networks"].isArray());
                UNIT_ASSERT_VALUES_EQUAL(request["wifi_networks"].size(), 1);
                UNIT_ASSERT_VALUES_EQUAL(tryGetString(request["wifi_networks"][0], "mac", ""), state.wifiList[0].mac);
                UNIT_ASSERT_VALUES_EQUAL(tryGetInt(request["wifi_networks"][0], "signal_strength", 0), state.wifiList[0].rssi);

                return IHttpClient::HttpResponse(
                    200,
                    "application/json",
                    "{\n"
                    "   \"position\":\n"
                    "      {\n"
                    "         \"latitude\":60.32444381713867,\n"
                    "         \"longitude\":24.62943458557129,\n"
                    "         \"altitude\":0.0,\n"
                    "         \"precision\":100000.0,\n"
                    "         \"altitude_precision\":30.0,\n"
                    "         \"type\":\"ip\"\n"
                    "      }\n"
                    "}");
            });

        size_t calls_counter = 0;
        EXPECT_CALL(*mockHttpClient, get)
            .WillRepeatedly([&calls_counter](std::string_view tag, const std::string& url, const IHttpClient::Headers& headers) {
                UNIT_ASSERT_VALUES_EQUAL(tag, "get-timezone");
                UNIT_ASSERT_VALUES_EQUAL(url, "https://quasar.yandex.net/get_timezone?lat=60.3244&lon=24.6294");
                UNIT_ASSERT(headers.empty());

                if (calls_counter < 2) {
                    ++calls_counter;
                    return IHttpClient::HttpResponse(500, "application/json", "{}");
                } else if (calls_counter > 2) {
                    UNIT_ASSERT(false);
                }

                ++calls_counter;
                return IHttpClient::HttpResponse(
                    200,
                    "application/json",
                    "{\n"
                    "   \"offset_sec\":7200,\n"
                    "   \"status\":\"ok\",\n"
                    "   \"timezone\":\"Europe/Helsinki\"\n"
                    "}");
            });

        geolocation->onSDKState(state);           // first try of update
        testCallbackQueue->pumpDelayedCallback(); // second try of update

        // Geolocation and timezone will not change in the case of backend error
        UNIT_ASSERT(listener->getLocation() == defaultLocation);
        UNIT_ASSERT(listener->getTimezone() == defaultTimezone);

        // Successfull update is expected at the third time
        testCallbackQueue->pumpDelayedCallback();

        UNIT_ASSERT_VALUES_EQUAL(listener->getLocation().latitude, 60.32444381713867);
        UNIT_ASSERT_VALUES_EQUAL(listener->getLocation().longitude, 24.62943458557129);
        UNIT_ASSERT_VALUES_EQUAL(listener->getLocation().precision.value_or(0), 100000.0);

        UNIT_ASSERT_VALUES_EQUAL(listener->getTimezone().timezoneName, "Europe/Helsinki");
        UNIT_ASSERT_VALUES_EQUAL(listener->getTimezone().timezoneOffsetSec, 7200);
    }

    Y_UNIT_TEST(testOverrideByDeviceConfig) {
        SDKState state = {
            .wifiList = {{.rssi = -74,
                          .mac = "8e:7e:2c:a5:56:d3"}}};

        const auto config = "{\n"
                            "    \"latitude\": 38.716182,\n"
                            "    \"longitude\": -9.141589\n"
                            "}";

        EXPECT_CALL(*mockHttpClient, post)
            .WillRepeatedly([state](std::string_view tag, const std::string& url, const std::string& data, const IHttpClient::Headers& headers) {
                UNIT_ASSERT_VALUES_EQUAL(tag, "geolocation");
                UNIT_ASSERT_VALUES_EQUAL(url, "https://api.lbs.yandex.net/geolocation?yandex_io_platform=yandexstation&yandex_io_device_id=DEVICE_ID");
                UNIT_ASSERT(headers.contains("Content-Type"));
                UNIT_ASSERT_VALUES_EQUAL(headers.at("Content-Type"), "application/json");

                UNIT_ASSERT_VALUES_EQUAL(data.substr(0, 5), "json=");
                const auto request = parseJson(data.substr(5));
                UNIT_ASSERT(!request["common"]["version"].isNull());
                UNIT_ASSERT(!request["common"]["api_key"].isNull());
                UNIT_ASSERT(request["wifi_networks"].isArray());
                UNIT_ASSERT_VALUES_EQUAL(request["wifi_networks"].size(), 1);
                UNIT_ASSERT_VALUES_EQUAL(tryGetString(request["wifi_networks"][0], "mac", ""), state.wifiList[0].mac);
                UNIT_ASSERT_VALUES_EQUAL(tryGetInt(request["wifi_networks"][0], "signal_strength", 0), state.wifiList[0].rssi);

                return IHttpClient::HttpResponse(
                    200,
                    "application/json",
                    "{\n"
                    "   \"position\":\n"
                    "      {\n"
                    "         \"latitude\":62.007491,\n"
                    "         \"longitude\":129.850297,\n"
                    "         \"altitude\":0.0,\n"
                    "         \"precision\":100000.0,\n"
                    "         \"altitude_precision\":30.0,\n"
                    "         \"type\":\"wifi\"\n"
                    "      }\n"
                    "}");
            });

        EXPECT_CALL(*mockHttpClient, get)
            .WillRepeatedly([](std::string_view tag, const std::string& url, const IHttpClient::Headers& headers) {
                UNIT_ASSERT_VALUES_EQUAL(tag, "get-timezone");
                UNIT_ASSERT(headers.empty());
                UNIT_ASSERT(url == "https://quasar.yandex.net/get_timezone?lat=62.0075&lon=129.85" || url == "https://quasar.yandex.net/get_timezone?lat=38.7162&lon=-9.14159");

                if (url == "https://quasar.yandex.net/get_timezone?lat=62.0075&lon=129.85") {
                    return IHttpClient::HttpResponse(
                        200,
                        "application/json",
                        "{\n"
                        "   \"offset_sec\":32400,\n"
                        "   \"status\":\"ok\",\n"
                        "   \"timezone\":\"Asia/Yakutsk\"\n"
                        "}");
                }
                return IHttpClient::HttpResponse(
                    200,
                    "application/json",
                    "{\n"
                    "   \"offset_sec\":0,\n"
                    "   \"status\":\"ok\",\n"
                    "   \"timezone\":\"Europe/Lisbon\"\n"
                    "}");
            });

        geolocation->onSDKState(state);
        geolocation->onDeviceConfig("location", config);

        UNIT_ASSERT_VALUES_EQUAL(listener->getLocation().latitude, 38.716182);
        UNIT_ASSERT_VALUES_EQUAL(listener->getLocation().longitude, -9.141589);
        UNIT_ASSERT(!listener->getLocation().precision.has_value());

        UNIT_ASSERT_VALUES_EQUAL(listener->getTimezone().timezoneName, "Europe/Lisbon");
        UNIT_ASSERT_VALUES_EQUAL(listener->getTimezone().timezoneOffsetSec, 0);

        geolocation->onDeviceConfig("location", "{}");

        UNIT_ASSERT_VALUES_EQUAL(listener->getLocation().latitude, 62.007491);
        UNIT_ASSERT_VALUES_EQUAL(listener->getLocation().longitude, 129.850297);
        UNIT_ASSERT_VALUES_EQUAL(listener->getLocation().precision.value_or(0), 100000.0);

        UNIT_ASSERT_VALUES_EQUAL(listener->getTimezone().timezoneName, "Asia/Yakutsk");
        UNIT_ASSERT_VALUES_EQUAL(listener->getTimezone().timezoneOffsetSec, 32400);
    }

    Y_UNIT_TEST(testOverrideBySystemConfig) {
        SDKState state = {
            .wifiList = {{.rssi = -74,
                          .mac = "8e:7e:2c:a5:56:d3"}}};

        EXPECT_CALL(*mockHttpClient, post)
            .WillRepeatedly([state](std::string_view tag, const std::string& url, const std::string& data, const IHttpClient::Headers& headers) {
                UNIT_ASSERT_VALUES_EQUAL(tag, "geolocation");
                UNIT_ASSERT_VALUES_EQUAL(url, "https://api.lbs.yandex.net/geolocation?yandex_io_platform=yandexstation&yandex_io_device_id=DEVICE_ID");
                UNIT_ASSERT(headers.contains("Content-Type"));
                UNIT_ASSERT_VALUES_EQUAL(headers.at("Content-Type"), "application/json");

                UNIT_ASSERT_VALUES_EQUAL(data.substr(0, 5), "json=");
                const auto request = parseJson(data.substr(5));
                UNIT_ASSERT(!request["common"]["version"].isNull());
                UNIT_ASSERT(!request["common"]["api_key"].isNull());
                UNIT_ASSERT(request["wifi_networks"].isArray());
                UNIT_ASSERT_VALUES_EQUAL(request["wifi_networks"].size(), 1);
                UNIT_ASSERT_VALUES_EQUAL(tryGetString(request["wifi_networks"][0], "mac", ""), state.wifiList[0].mac);
                UNIT_ASSERT_VALUES_EQUAL(tryGetInt(request["wifi_networks"][0], "signal_strength", 0), state.wifiList[0].rssi);

                return IHttpClient::HttpResponse(
                    200,
                    "application/json",
                    "{\n"
                    "   \"position\":\n"
                    "      {\n"
                    "         \"latitude\":62.007491,\n"
                    "         \"longitude\":129.850297,\n"
                    "         \"altitude\":0.0,\n"
                    "         \"precision\":100000.0,\n"
                    "         \"altitude_precision\":30.0,\n"
                    "         \"type\":\"wifi\"\n"
                    "      }\n"
                    "}");
            });

        EXPECT_CALL(*mockHttpClient, get)
            .WillRepeatedly([](std::string_view tag, const std::string& url, const IHttpClient::Headers& headers) {
                UNIT_ASSERT_VALUES_EQUAL(tag, "get-timezone");
                UNIT_ASSERT(headers.empty());
                UNIT_ASSERT(url == "https://quasar.yandex.net/get_timezone?lat=38.7162&lon=-9.14159" || url == "https://quasar.yandex.net/get_timezone?lat=62.0075&lon=129.85");
                if (url == "https://quasar.yandex.net/get_timezone?lat=38.7162&lon=-9.14159") {
                    return IHttpClient::HttpResponse(
                        200,
                        "application/json",
                        "{\n"
                        "   \"offset_sec\":0,\n"
                        "   \"status\":\"ok\",\n"
                        "   \"timezone\":\"Europe/Lisbon\"\n"
                        "}");
                }
                return IHttpClient::HttpResponse(
                    200,
                    "application/json",
                    "{\n"
                    "   \"offset_sec\":32400,\n"
                    "   \"status\":\"ok\",\n"
                    "   \"timezone\":\"Asia/Yakutsk\"\n"
                    "}");
            });

        geolocation->onSDKState(state);

        auto config = "{\n"
                      "   \"location\": {\n"
                      "      \"latitude\": 38.716182,\n"
                      "      \"longitude\": -9.141589\n"
                      "   },\n"
                      "   \"timezone\": {\n"
                      "      \"offset_sec\": 0,\n"
                      "      \"timezone_name\": \"Europe/Lisbon\"\n"
                      "   }\n"
                      "}";
        geolocation->onSystemConfig("locationdSettings", config);

        UNIT_ASSERT_VALUES_EQUAL(listener->getLocation().latitude, 38.716182);
        UNIT_ASSERT_VALUES_EQUAL(listener->getLocation().longitude, -9.141589);

        UNIT_ASSERT_VALUES_EQUAL(listener->getTimezone().timezoneName, "Europe/Lisbon");
        UNIT_ASSERT_VALUES_EQUAL(listener->getTimezone().timezoneOffsetSec, 0);

        config = "{\n"
                 "   \"location\": {\n"
                 "      \"latitude\": 38.716182,\n"
                 "      \"longitude\": -9.141589\n"
                 "   }\n"
                 "}";
        geolocation->onSystemConfig("locationdSettings", config);

        UNIT_ASSERT_VALUES_EQUAL(listener->getLocation().latitude, 38.716182);
        UNIT_ASSERT_VALUES_EQUAL(listener->getLocation().longitude, -9.141589);
        UNIT_ASSERT(!listener->getLocation().precision.has_value());

        UNIT_ASSERT_VALUES_EQUAL(listener->getTimezone().timezoneName, "Europe/Lisbon");
        UNIT_ASSERT_VALUES_EQUAL(listener->getTimezone().timezoneOffsetSec, 0);

        config = "{\n"
                 "   \"timezone\": {\n"
                 "      \"offset_sec\": 0,\n"
                 "      \"timezone_name\": \"Europe/Lisbon\"\n"
                 "   }\n"
                 "}";
        geolocation->onSystemConfig("locationdSettings", config);

        UNIT_ASSERT_VALUES_EQUAL(listener->getLocation().latitude, 62.007491);
        UNIT_ASSERT_VALUES_EQUAL(listener->getLocation().longitude, 129.850297);
        UNIT_ASSERT_VALUES_EQUAL(listener->getLocation().precision.value_or(0), 100000.0);

        UNIT_ASSERT_VALUES_EQUAL(listener->getTimezone().timezoneName, "Europe/Lisbon");
        UNIT_ASSERT_VALUES_EQUAL(listener->getTimezone().timezoneOffsetSec, 0);

        geolocation->onSystemConfig("locationdSettings", "{}");

        UNIT_ASSERT_VALUES_EQUAL(listener->getLocation().latitude, 62.007491);
        UNIT_ASSERT_VALUES_EQUAL(listener->getLocation().longitude, 129.850297);
        UNIT_ASSERT_VALUES_EQUAL(listener->getLocation().precision.value_or(0), 100000.0);

        UNIT_ASSERT_VALUES_EQUAL(listener->getTimezone().timezoneName, "Asia/Yakutsk");
        UNIT_ASSERT_VALUES_EQUAL(listener->getTimezone().timezoneOffsetSec, 32400);
    }

    Y_UNIT_TEST(testOverrideByDeviceAndSystemConfig) {
        SDKState state = {
            .wifiList = {{.rssi = -74,
                          .mac = "8e:7e:2c:a5:56:d3"}}};

        EXPECT_CALL(*mockHttpClient, post)
            .WillRepeatedly([state](std::string_view tag, const std::string& url, const std::string& data, const IHttpClient::Headers& headers) {
                UNIT_ASSERT_VALUES_EQUAL(tag, "geolocation");
                UNIT_ASSERT_VALUES_EQUAL(url, "https://api.lbs.yandex.net/geolocation?yandex_io_platform=yandexstation&yandex_io_device_id=DEVICE_ID");
                UNIT_ASSERT(headers.contains("Content-Type"));
                UNIT_ASSERT_VALUES_EQUAL(headers.at("Content-Type"), "application/json");

                UNIT_ASSERT_VALUES_EQUAL(data.substr(0, 5), "json=");
                const auto request = parseJson(data.substr(5));
                UNIT_ASSERT(!request["common"]["version"].isNull());
                UNIT_ASSERT(!request["common"]["api_key"].isNull());
                UNIT_ASSERT(request["wifi_networks"].isArray());
                UNIT_ASSERT_VALUES_EQUAL(request["wifi_networks"].size(), 1);
                UNIT_ASSERT_VALUES_EQUAL(tryGetString(request["wifi_networks"][0], "mac", ""), state.wifiList[0].mac);
                UNIT_ASSERT_VALUES_EQUAL(tryGetInt(request["wifi_networks"][0], "signal_strength", 0), state.wifiList[0].rssi);

                return IHttpClient::HttpResponse(
                    200,
                    "application/json",
                    "{\n"
                    "   \"position\":\n"
                    "      {\n"
                    "         \"latitude\":62.007491,\n"
                    "         \"longitude\":129.850297,\n"
                    "         \"altitude\":0.0,\n"
                    "         \"precision\":100000.0,\n"
                    "         \"altitude_precision\":30.0,\n"
                    "         \"type\":\"wifi\"\n"
                    "      }\n"
                    "}");
            });

        EXPECT_CALL(*mockHttpClient, get)
            .WillRepeatedly([](std::string_view tag, const std::string& url, const IHttpClient::Headers& headers) {
                UNIT_ASSERT_VALUES_EQUAL(tag, "get-timezone");
                UNIT_ASSERT(headers.empty());
                UNIT_ASSERT(url == "https://quasar.yandex.net/get_timezone?lat=62.0075&lon=129.85" || url == "https://quasar.yandex.net/get_timezone?lat=38.7162&lon=-9.14159");

                if (url == "https://quasar.yandex.net/get_timezone?lat=62.0075&lon=129.85") {
                    return IHttpClient::HttpResponse(
                        200,
                        "application/json",
                        "{\n"
                        "   \"offset_sec\":32400,\n"
                        "   \"status\":\"ok\",\n"
                        "   \"timezone\":\"Asia/Yakutsk\"\n"
                        "}");
                }
                return IHttpClient::HttpResponse(
                    200,
                    "application/json",
                    "{\n"
                    "   \"offset_sec\":0,\n"
                    "   \"status\":\"ok\",\n"
                    "   \"timezone\":\"Europe/Lisbon\"\n"
                    "}");
            });

        geolocation->onSDKState(state);

        UNIT_ASSERT_VALUES_EQUAL(listener->getLocation().latitude, 62.007491);
        UNIT_ASSERT_VALUES_EQUAL(listener->getLocation().longitude, 129.850297);
        UNIT_ASSERT_VALUES_EQUAL(listener->getLocation().precision.value_or(0), 100000.0);

        UNIT_ASSERT_VALUES_EQUAL(listener->getTimezone().timezoneName, "Asia/Yakutsk");
        UNIT_ASSERT_VALUES_EQUAL(listener->getTimezone().timezoneOffsetSec, 32400);

        const auto deviceConfig = "{\n"
                                  "    \"latitude\": 38.716182,\n"
                                  "    \"longitude\": -9.141589\n"
                                  "}";
        geolocation->onDeviceConfig("location", deviceConfig);

        UNIT_ASSERT_VALUES_EQUAL(listener->getLocation().latitude, 38.716182);
        UNIT_ASSERT_VALUES_EQUAL(listener->getLocation().longitude, -9.141589);
        UNIT_ASSERT(!listener->getLocation().precision.has_value());

        UNIT_ASSERT_VALUES_EQUAL(listener->getTimezone().timezoneName, "Europe/Lisbon");
        UNIT_ASSERT_VALUES_EQUAL(listener->getTimezone().timezoneOffsetSec, 0);

        const auto systemConfig = "{\n"
                                  "   \"location\": {\n"
                                  "      \"latitude\": -33.86,\n"
                                  "      \"longitude\": 151.21\n"
                                  "   },\n"
                                  "   \"timezone\": {\n"
                                  "      \"offset_sec\": 36000,\n"
                                  "      \"timezone_name\": \"Australia/Sydney\"\n"
                                  "   }\n"
                                  "}";
        geolocation->onSystemConfig("locationdSettings", systemConfig);

        UNIT_ASSERT_VALUES_EQUAL(listener->getLocation().latitude, -33.86);
        UNIT_ASSERT_VALUES_EQUAL(listener->getLocation().longitude, 151.21);
        UNIT_ASSERT(!listener->getLocation().precision.has_value());

        UNIT_ASSERT_VALUES_EQUAL(listener->getTimezone().timezoneName, "Australia/Sydney");
        UNIT_ASSERT_VALUES_EQUAL(listener->getTimezone().timezoneOffsetSec, 36000);

        geolocation->onSystemConfig("locationdSettings", "{}");

        UNIT_ASSERT_VALUES_EQUAL(listener->getLocation().latitude, 38.716182);
        UNIT_ASSERT_VALUES_EQUAL(listener->getLocation().longitude, -9.141589);
        UNIT_ASSERT(!listener->getLocation().precision.has_value());

        UNIT_ASSERT_VALUES_EQUAL(listener->getTimezone().timezoneName, "Europe/Lisbon");
        UNIT_ASSERT_VALUES_EQUAL(listener->getTimezone().timezoneOffsetSec, 0);

        geolocation->onDeviceConfig("location", "{}");

        UNIT_ASSERT_VALUES_EQUAL(listener->getLocation().latitude, 62.007491);
        UNIT_ASSERT_VALUES_EQUAL(listener->getLocation().longitude, 129.850297);
        UNIT_ASSERT_VALUES_EQUAL(listener->getLocation().precision.value_or(0), 100000.0);

        UNIT_ASSERT_VALUES_EQUAL(listener->getTimezone().timezoneName, "Asia/Yakutsk");
        UNIT_ASSERT_VALUES_EQUAL(listener->getTimezone().timezoneOffsetSec, 32400);
    }

    Y_UNIT_TEST(testGeolocationBackup) {
        proto::Location location;
        proto::Timezone timezone;

        location.set_latitude(-33.86);
        location.set_longitude(151.21);
        location.set_precision(140.0);

        timezone.set_timezone_name("Australia/Sydney");
        timezone.set_timezone_offset_sec(36000);

        std::ofstream(locationBackupPath, std::ofstream::trunc) << location.SerializeAsString();
        std::ofstream(timezoneBackupPath, std::ofstream::trunc) << timezone.SerializeAsString();

        // After adding a new listener it gets values from backup
        std::shared_ptr<TestGeolocationListener> otherListener = std::make_shared<TestGeolocationListener>();
        geolocation->addListener(otherListener);

        UNIT_ASSERT_VALUES_EQUAL(otherListener->getLocation().latitude, -33.86);
        UNIT_ASSERT_VALUES_EQUAL(otherListener->getLocation().longitude, 151.21);
        UNIT_ASSERT_VALUES_EQUAL(otherListener->getLocation().precision.value_or(0), 140.0);

        UNIT_ASSERT_VALUES_EQUAL(otherListener->getTimezone().timezoneName, "Australia/Sydney");
        UNIT_ASSERT_VALUES_EQUAL(otherListener->getTimezone().timezoneOffsetSec, 36000);

        const auto config = "{\n"
                            "   \"location\": {\n"
                            "      \"latitude\": 38.716182,\n"
                            "      \"longitude\": -9.141589\n"
                            "   },\n"
                            "   \"timezone\": {\n"
                            "      \"offset_sec\": 0,\n"
                            "      \"timezone_name\": \"Europe/Lisbon\"\n"
                            "   }\n"
                            "}";
        geolocation->onSystemConfig("locationdSettings", config);

        UNIT_ASSERT_VALUES_EQUAL(otherListener->getLocation().latitude, 38.716182);
        UNIT_ASSERT_VALUES_EQUAL(otherListener->getLocation().longitude, -9.141589);
        UNIT_ASSERT(!listener->getLocation().precision.has_value());

        UNIT_ASSERT_VALUES_EQUAL(otherListener->getTimezone().timezoneName, "Europe/Lisbon");
        UNIT_ASSERT_VALUES_EQUAL(otherListener->getTimezone().timezoneOffsetSec, 0);

        // Backup must be updated after any value change
        UNIT_ASSERT(location.ParseFromString(TString(getFileContent(locationBackupPath))));
        UNIT_ASSERT(location.has_latitude());
        UNIT_ASSERT(location.has_longitude());
        UNIT_ASSERT(!location.has_precision());
        UNIT_ASSERT_VALUES_EQUAL(location.latitude(), 38.716182);
        UNIT_ASSERT_VALUES_EQUAL(location.longitude(), -9.141589);

        UNIT_ASSERT(timezone.ParseFromString(TString(getFileContent(timezoneBackupPath))));
        UNIT_ASSERT(timezone.has_timezone_name());
        UNIT_ASSERT(timezone.has_timezone_offset_sec());
        UNIT_ASSERT_VALUES_EQUAL(timezone.timezone_name(), "Europe/Lisbon");
        UNIT_ASSERT_VALUES_EQUAL(timezone.timezone_offset_sec(), 0);
    }
}

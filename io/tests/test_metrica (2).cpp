#include <yandex_io/metrica/metricad/app_metrica_endpoint.h>
#include <yandex_io/metrica/metricad/connector/metrica_connector.h>

#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/ipc/i_server.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/libs/metrica/base/utils.h>
#include <yandex_io/libs/threading/steady_condition_variable.h>
#include <yandex_io/tests/testlib/test_http_server.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <util/folder/path.h>

#include <future>
#include <memory>

using namespace quasar;
using namespace quasar::proto;
using namespace quasar::TestUtils;
using namespace YandexIO;

namespace {

    struct Fixture: public QuasarUnitTestFixture {
        YandexIO::Configuration::TestGuard testGuard;

        TestHttpServer mockToSendFromMetricaServer;
        TestHttpServer mockBackend;
        std::shared_ptr<ipc::IServer> mockMonitord;

        using Base = QuasarUnitTestFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            std::remove(TEST_DB_NAME);
            std::remove(SESSION_ID_PERSISTENT_PART);
            std::remove(SESSION_ID_TEMPORARY_PART);

            Base::TearDown(context);
        }

        static constexpr const char* TEST_DB_NAME = "test_data_base.db";

        void initServers() {
            Json::Value& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
            config["metricad"]["metricaSessionIdPersistentPart"] = SESSION_ID_PERSISTENT_PART;
            config["metricad"]["metricaSessionIdTemporaryPart"] = SESSION_ID_TEMPORARY_PART;
            config["metricad"]["eventBlacklist"] = Json::Value(Json::arrayValue);
            config["metricad"]["envKeysBlacklist"] = Json::Value(Json::objectValue);

            unlink(TEST_DB_NAME);
            config["common"]["backendUrl"] = "http://localhost:" + to_string(mockBackend.start(getPort()));
            config["common"]["eventsDatabase"]["runtime"]["maxSizeKb"] = 1024 /*KB*/;
            config["common"]["eventsDatabase"]["runtime"]["filename"] = TEST_DB_NAME;

            config["testServerUrl"] = "http://localhost:" + to_string(mockToSendFromMetricaServer.start(getPort()));

            mockMonitord = createIpcServerForTests("monitord");
            mockMonitord->listenService();
        }

        static constexpr const char* SESSION_ID_PERSISTENT_PART = "metrica/session_id_part.txt";
        static constexpr const char* SESSION_ID_TEMPORARY_PART = "temp_metrica/session_id_part.txt";
    };

}; // namespace

Y_UNIT_TEST_SUITE_F(AppMetricaEndpointTests, Fixture) {
    Y_UNIT_TEST(testMetricaLatencyPoints) {
        std::promise<void> testEventReceived;

        mockToSendFromMetricaServer.onHandlePayload = [&](const TestHttpServer::Headers& /*headers*/,
                                                          const std::string& payload, TestHttpServer::HttpConnection& handler) {
            handler.doReplay(200, "text/html", "{"
                                               "   \"status\": \"ok\","
                                               "   \"sign\": \"1\","
                                               "   \"ts\": \"2\""
                                               "}");
            ReportMessage rep;
            Y_PROTOBUF_SUPPRESS_NODISCARD rep.ParseFromString(TString(payload));
            /* Check that Report has default time zone set up */

            UNIT_ASSERT_VALUES_EQUAL(rep.send_time().time_zone(), 3 * 60 * 60 /* 3 hours */);
            for (const auto& session : rep.sessions()) {
                for (const auto& event : session.events()) {
                    if (event.name() == "testEvent") {
                        const Json::Value value = parseJson(event.value());
                        UNIT_ASSERT(value.isMember("value"));
                        UNIT_ASSERT(value["value"].asInt() >= 0);
                        testEventReceived.set_value();
                    }
                }
            }
        };
        initServers();

        const Json::Value& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
        AppMetricaEndpoint mockAppMetricaEndpoint{
            getDeviceForTests(),
            ipcFactoryForTests(),
            std::make_shared<ReportConfiguration>(3, std::vector<std::string>{config["testServerUrl"].asString()}, *getDeviceForTests()),
            std::chrono::milliseconds(100),
        };

        MetricaConnector connector(ipcFactoryForTests(), "metricad");

        connector.params()->setNetworkStatus({
            YandexIO::ITelemetry::IParams::NetworkStatus::CONNECTED,
            YandexIO::ITelemetry::IParams::ConnectionType::WIFI,
        });

        /* have to be sure that metricad started up -> so it won't miss message from wifid */
        connector.waitUntilConnected();
        auto latencyPoint = connector.createLatencyPoint();
        connector.reportLatency(latencyPoint, "testEvent");

        testEventReceived.get_future().get();
        UNIT_ASSERT(true);
    }

    Y_UNIT_TEST(testMetricaLocationTimezone) {
        SteadyConditionVariable condVar;
        bool testEventReceived{false};
        bool hasLocation{false};      // check that location is a part of session
        bool hasTimezone{false};      // check that have timezone environment variable
        bool hasTimezoneShift{false}; // check that have timezone shift sec as a part of report
        std::mutex mutex;             // guards bool flags

        mockToSendFromMetricaServer.onHandlePayload = [&](const TestHttpServer::Headers& /*headers*/,
                                                          const std::string& payload, TestHttpServer::HttpConnection& handler) {
            std::lock_guard<std::mutex> guard(mutex);
            handler.doReplay(200, "text/html", "{"
                                               "   \"status\": \"ok\","
                                               "   \"sign\": \"1\","
                                               "   \"ts\": \"2\""
                                               "}");
            ReportMessage rep;
            Y_PROTOBUF_SUPPRESS_NODISCARD rep.ParseFromString(TString(payload));
            UNIT_ASSERT(rep.has_send_time());
            UNIT_ASSERT(rep.send_time().has_time_zone());
            for (const auto& env : rep.app_environment()) {
                if (env.name() == "timezone") {
                    UNIT_ASSERT_VALUES_EQUAL(env.value(), "Pacific Daylight Time");
                    hasTimezone = true;
                }
            }
            /* Check that timezone is set up in report */
            if (rep.send_time().time_zone() == (-7 /*h*/ * 60 /*min*/ * 60 /*sec*/)) {
                hasTimezoneShift = true;
            }

            for (const auto& session : rep.sessions()) {
                /* Note: there should be only one session in report */
                UNIT_ASSERT(session.has_session_desc());
                if (session.session_desc().has_location()) {
                    UNIT_ASSERT_VALUES_EQUAL(session.session_desc().location().lat(), -12.3);
                    UNIT_ASSERT_VALUES_EQUAL(session.session_desc().location().lon(), 24.12);
                    hasLocation = true;
                }

                for (const auto& event : session.events()) {
                    if (event.name() == "testEvent") {
                        Json::Value value = parseJson(event.value());
                        UNIT_ASSERT(value.isMember("value"));
                        UNIT_ASSERT(value["value"].asInt() >= 0);
                        testEventReceived = true;
                    }
                }
            }
            condVar.notify_one();
        };
        initServers();

        const Json::Value& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
        AppMetricaEndpoint mockAppMetricaEndpoint{
            getDeviceForTests(),
            ipcFactoryForTests(),
            std::make_shared<ReportConfiguration>(3, std::vector<std::string>{config["testServerUrl"].asString()}, *getDeviceForTests()),
            std::chrono::milliseconds(100),
        };

        MetricaConnector connector(ipcFactoryForTests(), "metricad");

        connector.params()->setNetworkStatus({
            YandexIO::ITelemetry::IParams::NetworkStatus::CONNECTED,
            YandexIO::ITelemetry::IParams::ConnectionType::WIFI,
        });

        connector.params()->setLocation(-12.3, 24.12);
        connector.params()->setTimezone("Pacific Daylight Time", -7 /*h*/ * 60 /*min*/ * 60 /*sec*/);

        std::unique_lock<std::mutex> lock(mutex);
        auto checkTestFlags = [&]() {
            return testEventReceived && hasLocation && hasTimezone && hasTimezoneShift;
        };
        /* Timezone can be set up asynchronously to report environment and to protobuf report part (time_zone).
         * Same for location. It's okay that it takes some time and do not happen instantly
         * Simply ping AppMetricaEndpoint with random metric value until all test flags are set up
         */
        while (!checkTestFlags()) {
            testEventReceived = false;
            /* Need to trigger event, otherwise metrica won't be sent and we'll not be able to check timezone*/
            auto latencyPoint = connector.createLatencyPoint();
            connector.reportLatency(latencyPoint, "testEvent");
            condVar.wait(lock, [&]() {
                return testEventReceived;
            });
        }

    }

    Y_UNIT_TEST(testMetricaWifiList) {
        SteadyConditionVariable condVar;
        bool testEventReceived{false};
        bool hasNetworkInfo{false};
        std::mutex mutex; // guards bool values

        const std::vector<ITelemetry::IParams::WifiNetwork> networks{
            ITelemetry::IParams::WifiNetwork{"FE:ED:DE:AD:BE:EF", -24, "ssid1", true},
            ITelemetry::IParams::WifiNetwork{"CA:FE:C0:FF:EE:00", -13, "ssid2", false}};

        mockToSendFromMetricaServer.onHandlePayload = [&](const TestHttpServer::Headers& /*headers*/,
                                                          const std::string& payload, TestHttpServer::HttpConnection& handler) {
            std::lock_guard<std::mutex> guard(mutex);
            handler.doReplay(200, "text/html", "{"
                                               "   \"status\": \"ok\","
                                               "   \"sign\": \"1\","
                                               "   \"ts\": \"2\""
                                               "}");
            ReportMessage rep;
            Y_PROTOBUF_SUPPRESS_NODISCARD rep.ParseFromString(TString(payload));

            for (const auto& session : rep.sessions()) {
                /* Note: there should be only one session in report */
                for (const auto& event : session.events()) {
                    if (event.name() != "testEvent") {
                        continue;
                    }
                    UNIT_ASSERT(event.has_network_info());
                    const auto& networkInfo = event.network_info();
                    UNIT_ASSERT(networkInfo.has_connection_type());
                    UNIT_ASSERT_VALUES_EQUAL(int(networkInfo.connection_type()), int(ReportMessage_Session_ConnectionType_CONNECTION_WIFI));
                    /* Test checks that device set up 2 wifi networks */
                    if (static_cast<size_t>(networkInfo.wifi_networks_size()) == networks.size()) {
                        /* found expected network info in report */
                        hasNetworkInfo = true;
                        for (int i = 0; i < networkInfo.wifi_networks_size(); ++i) {
                            UNIT_ASSERT_VALUES_EQUAL(networks[i].ssid, networkInfo.wifi_networks(i).ssid());
                            UNIT_ASSERT_VALUES_EQUAL(networks[i].mac, networkInfo.wifi_networks(i).mac());
                            UNIT_ASSERT_VALUES_EQUAL(networks[i].rssi, networkInfo.wifi_networks(i).signal_strength());
                            UNIT_ASSERT_VALUES_EQUAL(networks[i].isConnected, networkInfo.wifi_networks(i).is_connected());
                        }
                    }
                    testEventReceived = true;
                }
            }
            condVar.notify_one();
        };
        initServers();

        const Json::Value& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
        AppMetricaEndpoint mockAppMetricaEndpoint{
            getDeviceForTests(),
            ipcFactoryForTests(),
            std::make_shared<ReportConfiguration>(3, std::vector<std::string>{config["testServerUrl"].asString()}, *getDeviceForTests()),
            std::chrono::milliseconds(100),
        };

        MetricaConnector connector(ipcFactoryForTests(), "metricad");

        connector.params()->setNetworkStatus({
            YandexIO::ITelemetry::IParams::NetworkStatus::CONNECTED,
            YandexIO::ITelemetry::IParams::ConnectionType::WIFI,
        });

        connector.params()->setWifiNetworks(networks);

        std::unique_lock<std::mutex> lock(mutex);
        testEventReceived = false;
        hasNetworkInfo = false;
        /* Need to trigger event, otherwise metrica won't be sent */
        connector.reportEvent("testEvent");
        condVar.wait(lock, [&]() {
            return testEventReceived && hasNetworkInfo;
        });
    }

    Y_UNIT_TEST(testMetricaDeletesEnvironmentVariables) {
        std::unique_ptr<std::promise<bool>> hasEnvironmentVariable;

        mockToSendFromMetricaServer.onHandlePayload = [&](const TestHttpServer::Headers& /*headers*/,
                                                          const std::string& payload, TestHttpServer::HttpConnection& handler) {
            handler.doReplay(200, "text/html", "{"
                                               "   \"status\": \"ok\","
                                               "   \"sign\": \"1\","
                                               "   \"ts\": \"2\""
                                               "}");
            ReportMessage rep;
            Y_PROTOBUF_SUPPRESS_NODISCARD rep.ParseFromString(TString(payload));

            bool eventFound = false;
            for (const auto& session : rep.sessions()) {
                for (const auto& event : session.events()) {
                    if (event.name() == "testEvent") {
                        eventFound = true;
                    }
                }
            }
            if (eventFound) {
                bool hasEnvVar = false;
                for (const auto& envVar : rep.app_environment()) {
                    if (envVar.name() == "testEnvironmentKey" && envVar.value() == "testEnvironmentValue") {
                        hasEnvVar = true;
                        break;
                    }
                }
                hasEnvironmentVariable->set_value(hasEnvVar);
            }
        };
        initServers();

        const Json::Value& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
        AppMetricaEndpoint mockAppMetricaEndpoint{
            getDeviceForTests(),
            ipcFactoryForTests(),
            std::make_shared<ReportConfiguration>(3, std::vector<std::string>{config["testServerUrl"].asString()}, *getDeviceForTests()),
            std::chrono::milliseconds(100),
        };

        MetricaConnector connector(ipcFactoryForTests(), "metricad");

        connector.params()->setNetworkStatus({
            YandexIO::ITelemetry::IParams::NetworkStatus::CONNECTED,
            YandexIO::ITelemetry::IParams::ConnectionType::WIFI,
        });

        hasEnvironmentVariable = std::make_unique<std::promise<bool>>();
        connector.putAppEnvironmentValue("testEnvironmentKey", "testEnvironmentValue");
        connector.reportEvent("testEvent");
        auto future = hasEnvironmentVariable->get_future();
        UNIT_ASSERT_VALUES_EQUAL(future.get(), true);

        hasEnvironmentVariable = std::make_unique<std::promise<bool>>();
        connector.deleteAppEnvironmentValue("testEnvironmentKey");
        connector.reportEvent("testEvent");
        future = hasEnvironmentVariable->get_future();
        UNIT_ASSERT_VALUES_EQUAL(future.get(), false);
    }

    Y_UNIT_TEST(testMetricaEventBlacklist) {
        std::unique_ptr<std::promise<void>> gotReport;
        std::set<std::string> allowedEvents = {
            "testEvent1",
            "testEvent2",
        };

        mockToSendFromMetricaServer.onHandlePayload = [&](
                                                          const TestHttpServer::Headers& /*headers*/,
                                                          const std::string& payload, TestHttpServer::HttpConnection& handler) {
            handler.doReplay(
                200, "text/html",
                "{"
                "   \"status\": \"ok\","
                "   \"sign\": \"1\","
                "   \"ts\": \"2\""
                "}");

            ReportMessage rep;
            Y_PROTOBUF_SUPPRESS_NODISCARD rep.ParseFromString(TString(payload));

            bool eventFound = false;
            for (const auto& session : rep.sessions()) {
                for (const auto& event : session.events()) {
                    YIO_LOG_DEBUG("Session: " << session.id() << ", event: " << event.name());

                    if (!event.name().empty()) {
                        eventFound = true;
                    }
                }
            }

            if (eventFound) {
                for (const auto& session : rep.sessions()) {
                    for (const auto& event : session.events()) {
                        if (allowedEvents.find(event.name()) == allowedEvents.end()) {
                            UNIT_FAIL("Got unallowed event <" + event.name() + ">");
                        }
                    }
                }

                gotReport->set_value();
            }
        };
        initServers();

        const Json::Value& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
        AppMetricaEndpoint mockAppMetricaEndpoint{
            getDeviceForTests(),
            ipcFactoryForTests(),
            std::make_shared<ReportConfiguration>(3, std::vector<std::string>{config["testServerUrl"].asString()}, *getDeviceForTests()),
            std::chrono::milliseconds(100),
        };

        MetricaConnector connector(ipcFactoryForTests(), "metricad");

        connector.params()->setNetworkStatus({
            YandexIO::ITelemetry::IParams::NetworkStatus::CONNECTED,
            YandexIO::ITelemetry::IParams::ConnectionType::WIFI,
        });

        std::unique_ptr<std::promise<void>> gotConfig;
        mockAppMetricaEndpoint.afterHandleMetricaMessage = [&](const MetricaMessage& message) {
            if (message.has_config()) {
                gotConfig->set_value();
            }
        };

        // check that events are delivering
        gotReport = std::make_unique<std::promise<void>>();
        connector.reportEvent("testEvent2");
        gotReport->get_future().wait();

        // ban testEvent2
        Json::Value userConfig;
        auto blacklist = Json::Value(Json::arrayValue);
        blacklist.append("testEvent2");
        userConfig["system_config"]["metricad"]["consumers"]["appmetrica"]["eventBlacklist"] = blacklist;
        gotConfig = std::make_unique<std::promise<void>>();
        connector.params()->setConfig(jsonToString(userConfig));
        gotConfig->get_future().wait();

        gotReport = std::make_unique<std::promise<void>>();
        // these two should be skipped
        allowedEvents.erase("testEvent2");
        connector.reportEvent("testEvent2");
        connector.reportEvent("testEvent2", YandexIO::ITelemetry::SKIP_DATABASE);
        // this one should be delivered
        connector.reportEvent("testEvent1");
        gotReport->get_future().wait();

        // reset config
        userConfig["system_config"]["metricad"]["consumers"] = Json::Value(Json::objectValue);
        gotConfig = std::make_unique<std::promise<void>>();
        connector.params()->setConfig(jsonToString(userConfig));
        gotConfig->get_future().wait();

        // testEvent2 should be delivering again
        allowedEvents.insert("testEvent2");
        gotReport = std::make_unique<std::promise<void>>();
        connector.reportEvent("testEvent2");
        gotReport->get_future().wait();
    }

    Y_UNIT_TEST(testMetricaEnvKeysBlacklist) {
        std::unique_ptr<std::promise<void>> gotReport;
        std::set<std::string> allowedEnvKeys;

        mockToSendFromMetricaServer.onHandlePayload = [&](
                                                          const TestHttpServer::Headers& /*headers*/,
                                                          const std::string& payload, TestHttpServer::HttpConnection& handler) {
            handler.doReplay(
                200, "text/html",
                "{"
                "   \"status\": \"ok\","
                "   \"sign\": \"1\","
                "   \"ts\": \"2\""
                "}");

            ReportMessage rep;
            Y_PROTOBUF_SUPPRESS_NODISCARD rep.ParseFromString(TString(payload));

            bool eventFound = false;
            for (const auto& session : rep.sessions()) {
                for (const auto& event : session.events()) {
                    YIO_LOG_DEBUG("Session: " << session.id() << ", event: " << event.name());

                    if (!event.name().empty()) {
                        eventFound = true;
                    }
                }
            }

            if (eventFound) {
                for (const auto& envVar : rep.app_environment()) {
                    if (allowedEnvKeys.find(envVar.name()) == allowedEnvKeys.end()) {
                        UNIT_FAIL("Got unallowed env key <" + envVar.name() + ">");
                    }
                }

                gotReport->set_value();
            }
        };
        initServers();

        const Json::Value& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
        AppMetricaEndpoint mockAppMetricaEndpoint{
            getDeviceForTests(),
            ipcFactoryForTests(),
            std::make_shared<ReportConfiguration>(3, std::vector<std::string>{config["testServerUrl"].asString()}, *getDeviceForTests()),
            std::chrono::milliseconds(100),
        };

        MetricaConnector connector(ipcFactoryForTests(), "metricad");

        connector.params()->setNetworkStatus({
            YandexIO::ITelemetry::IParams::NetworkStatus::CONNECTED,
            YandexIO::ITelemetry::IParams::ConnectionType::WIFI,
        });

        std::unique_ptr<std::promise<void>> gotConfig;
        mockAppMetricaEndpoint.afterHandleMetricaMessage = [&](const MetricaMessage& message) {
            if (message.has_config()) {
                gotConfig->set_value();
            }
        };

        // check that env variables get into report
        connector.putAppEnvironmentValue("envKey1", "");
        connector.putAppEnvironmentValue("envKey2", "");

        allowedEnvKeys = {"envKey1", "envKey2"};
        gotReport = std::make_unique<std::promise<void>>();
        connector.reportEvent("testEvent");
        gotReport->get_future().wait();

        // ban envKey2 & envKey3
        auto blacklist = parseJson(R"({"envKey2": true, "envKey3": true})");
        allowedEnvKeys = {"envKey1"};

        Json::Value userConfig;
        userConfig["system_config"]["metricad"]["consumers"]["appmetrica"]["envKeysBlacklist"] = blacklist;
        gotConfig = std::make_unique<std::promise<void>>();
        connector.params()->setConfig(jsonToString(userConfig));
        gotConfig->get_future().wait();

        gotReport = std::make_unique<std::promise<void>>();
        connector.reportEvent("testEvent");
        gotReport->get_future().wait();

        // should not get into report
        connector.putAppEnvironmentValue("envKey3", "");

        gotReport = std::make_unique<std::promise<void>>();
        connector.reportEvent("testEvent");
        gotReport->get_future().wait();

        // reset config
        userConfig["system_config"]["metricad"]["consumers"] = Json::Value(Json::objectValue);
        gotConfig = std::make_unique<std::promise<void>>();
        connector.params()->setConfig(jsonToString(userConfig));
        gotConfig->get_future().wait();

        // all 3 keys should get into report
        allowedEnvKeys = {"envKey1", "envKey2", "envKey3"};
        gotReport = std::make_unique<std::promise<void>>();
        connector.reportEvent("testEvent2");
        gotReport->get_future().wait();
    }

    Y_UNIT_TEST(testSaveLoadMetadata) {
        const auto savedMetadata = MetricaMetadata{
            .UUID = "some_uuid",
            .deviceID = "some_device_id",
        };

        const auto tmpFilepath{JoinFsPaths(tryGetRamDrivePath(), "file.json")};

        AppMetricaEndpoint::saveMetadata(tmpFilepath, savedMetadata);

        const auto loadedMetadata{AppMetricaEndpoint::loadMetadata(tmpFilepath)};

        UNIT_ASSERT_EQUAL(savedMetadata, loadedMetadata);
    }

} // test suite end

#include <yandex_io/services/pushd/push_endpoint.h>
#include <yandex_io/services/pushd/xiva_operations.h>

#include <yandex_io/services/authd/account_storage.h>
#include <yandex_io/services/authd/auth_endpoint.h>

#include <yandex_io/interfaces/auth/mock/auth_provider.h>
#include <yandex_io/interfaces/user_config/mock/user_config_provider.h>
#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/http_client/http_client.h>
#include <yandex_io/libs/ipc/i_connector.h>
#include <yandex_io/libs/ipc/i_server.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/threading/steady_condition_variable.h>
#include <yandex_io/protos/quasar_proto.pb.h>
#include <yandex_io/tests/testlib/mock_ws_server.h>
#include <yandex_io/tests/testlib/test_http_server.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/telemetry_test_fixture.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <curl/multi.h>

#include <library/cpp/testing/unittest/registar.h>

#include <future>
#include <memory>
#include <thread>

using namespace quasar;
using namespace quasar::TestUtils;
using namespace quasar::proto;

namespace {

    struct CurlGuard {
        CurlGuard() {
            curl_global_init(CURL_GLOBAL_ALL);
        }

        ~CurlGuard() {
            curl_global_cleanup();
        }
    };

    class ServicesInit: public virtual QuasarUnitTestFixture {
    public:
        CurlGuard curlGuard; /* Create curlGuard first to avoid race conditions around curl_global_init */

        YandexIO::Configuration::TestGuard testGuard;
        TestHttpServer mockBackend;
        std::shared_ptr<mock::AuthProvider> mockAuthProvider;
        std::shared_ptr<mock::UserConfigProvider> mockUserConfigProvider;
        std::mutex mutex_;
        std::string signTs;
        int mockBackendRequestsCnt = 0;
        std::string passportUid = "123";
        const std::string authToken = "123";

        using Base = QuasarUnitTestFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);
            SetUpInternal();
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            Base::TearDown(context);
        }

        void SetUpInternal() {
            auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
            signTs = std::to_string(time(nullptr) + 100);
            mockBackend.onHandlePayload = [&](const TestHttpServer::Headers& header, const std::string& /*payload*/, TestHttpServer::HttpConnection& handler) {
                UNIT_ASSERT_VALUES_EQUAL(header.verb, "POST");
                UNIT_ASSERT_VALUES_EQUAL(header.resource, "/push_subscribe");
                UNIT_ASSERT_VALUES_EQUAL(header.getHeader("authorization"), "OAuth " + authToken);
                UNIT_ASSERT_VALUES_EQUAL(header.queryParams.getValue("device_id"), getDeviceForTests()->deviceId());
                UNIT_ASSERT_VALUES_EQUAL(header.queryParams.getValue("uuid"), passportUid);
                UNIT_ASSERT_VALUES_EQUAL(header.queryParams.getValue("platform"), getDeviceForTests()->configuration()->getDeviceType());
                handler.doReplay(200, "text/html", "{"
                                                   "   \"status\": \"ok\","
                                                   "   \"sign\": \"1\","
                                                   "   \"ts\": \"" +
                                                       signTs + "\""
                                                                "}");
                std::unique_lock<std::mutex> lock(mutex_);
                mockBackendRequestsCnt++;
            };

            config["common"]["backendUrl"] = "http://localhost:" + std::to_string(mockBackend.start(getPort()));

            mockAuthProvider = std::make_shared<mock::AuthProvider>();
            mockAuthProvider->setOwner(
                AuthInfo2{
                    .source = AuthInfo2::Source::AUTHD,
                    .authToken = authToken,
                    .passportUid = passportUid,
                    .tag = 1,
                });
            mockUserConfigProvider = std::make_shared<mock::UserConfigProvider>(nullptr);
        }
    };

    class ServicesInitWithMetrica: public TelemetryTestFixture, public ServicesInit {
    public:
        ServicesInitWithMetrica()
            : TelemetryTestFixture()
            , ServicesInit()
        {
        }

        void SetUp(NUnitTest::TTestContext& context) override {
            TelemetryTestFixture::SetUp(context);
            ServicesInit::SetUpInternal();
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            TelemetryTestFixture::TearDown(context);
        }
    };

} // namespace

Y_UNIT_TEST_SUITE(PushdTests) {

    Y_UNIT_TEST_F(testPushServiceConnection, ServicesInit)
    {
        auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);

        MockWSServer mockXiva(getPort());
        config["pushd"]["xivaSubscribeUrl"] = "wss://localhost:" + std::to_string(mockXiva.getPort());

        PushEndpoint endpoint(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockUserConfigProvider, false);

        std::unique_ptr<std::promise<std::string>> messagePromise = nullptr;
        std::promise<void> xivaClientConnectedPromise;
        auto pushdConnector = createIpcConnectorForTests("pushd");
        pushdConnector->setConnectHandler([&pushdConnector]() {
            QuasarMessage message;
            message.mutable_subscribe_to_pushd_xiva_websocket_events();
            pushdConnector->sendMessage(std::move(message));
        });

        pushdConnector->setMessageHandler([&](const auto& msg) mutable {
            if (msg->has_xiva_websocket_client_state()) {
                if (msg->xiva_websocket_client_state().state() == WebsocketClientState::CONNECTED) {
                    xivaClientConnectedPromise.set_value();
                }
            } else if (msg->has_push_notification()) {
                messagePromise->set_value(msg->push_notification().operation());
            }
        });

        pushdConnector->connectToService();
        pushdConnector->waitUntilConnected();

        mockXiva.waitUntilConnections(1, 0);
        xivaClientConnectedPromise.get_future().wait();
        UNIT_ASSERT_VALUES_EQUAL(endpoint.getCurrentConnectionUrl(),
                                 "wss://localhost:" + std::to_string(mockXiva.getPort()) +
                                     "?&service=quasar-realtime,messenger-prod&client=q&session=" + getDeviceForTests()->deviceId() +
                                     "&sign=1&ts=" + signTs + "&user=" + passportUid);
        messagePromise = std::make_unique<std::promise<std::string>>();
        mockXiva.send("{\"operation\":\"ping\",\"server-interval-sec\":60}");
        mockXiva.send("{\"operation\":\"update_config\",\"message\":\"lol\"}");

        // Waiting for message
        UNIT_ASSERT_VALUES_EQUAL("update_config", messagePromise->get_future().get());
        UNIT_ASSERT_VALUES_EQUAL(mockXiva.getConnectionOpenedCnt(), 1);

        std::unique_lock<std::mutex> lock(mutex_);
        UNIT_ASSERT(mockBackendRequestsCnt != 0);
    }

    Y_UNIT_TEST_F(testPushServicePing, ServicesInit)
    {
        auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);

        SteadyConditionVariable condVar_;
        MockWSServer mockXiva(getPort());
        config["pushd"]["xivaSubscribeUrl"] = "wss://localhost:" + std::to_string(mockXiva.getPort());

        PushEndpoint endpoint(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockUserConfigProvider, false);
        mockXiva.waitUntilConnections(1, 0);
        UNIT_ASSERT_VALUES_EQUAL(endpoint.getCurrentConnectionUrl(),
                                 "wss://localhost:" + std::to_string(mockXiva.getPort()) +
                                     "?&service=quasar-realtime,messenger-prod&client=q&session=" + getDeviceForTests()->deviceId() +
                                     "&sign=1&ts=" + signTs + "&user=" + passportUid);
        mockXiva.send("{\"operation\":\"ping\",\"server-interval-sec\":0}");

        mockXiva.onOpen = [&](MockWSServer::ConnectionPtr /*connection*/) {
            condVar_.notify_one();
        };
        waitUntil(condVar_, [&]() {
            return mockXiva.getConnectionOpenedCnt() == 2;
        });
        UNIT_ASSERT_VALUES_EQUAL(mockXiva.getConnectionOpenedCnt(), 2);
        UNIT_ASSERT_VALUES_EQUAL(mockXiva.getConnectionClosedCnt(), 1);
        std::cout << "Done" << std::endl;
        std::unique_lock<std::mutex> lock(mutex_);
        UNIT_ASSERT(mockBackendRequestsCnt != 0);
    }

    Y_UNIT_TEST_F(testPushServiceBackendIsLate, ServicesInit)
    {
        mockBackend.stop();
        auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);

        MockWSServer mockXiva(getPort());
        config["pushd"]["xivaSubscribeUrl"] = "wss://localhost:" + std::to_string(mockXiva.getPort());

        PushEndpoint endpoint(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockUserConfigProvider, false);

        std::unique_ptr<std::promise<std::string>> messagePromise = nullptr;
        auto pushdConnector = createIpcConnectorForTests("pushd");
        pushdConnector->setMessageHandler([&](const auto& msg) {
            if (msg->has_push_notification()) {
                messagePromise->set_value(msg->push_notification().operation());
            }
        });
        pushdConnector->connectToService();
        pushdConnector->waitUntilConnected();

        /* Sleep here to emulate that Backend do not answer */
        std::this_thread::sleep_for(std::chrono::seconds(2));

        TestHttpServer mockBackend;
        signTs = std::to_string(time(nullptr) + 100);
        mockBackend.onHandlePayload = [&](const TestHttpServer::Headers& /*headers*/, const std::string& /*payload*/, TestHttpServer::HttpConnection& handler) {
            handler.doReplay(200, "text/html", "{"
                                               "   \"status\": \"ok\","
                                               "   \"sign\": \"1\","
                                               "   \"ts\": \"" +
                                                   signTs + "\""
                                                            "}");
        };
        config["common"]["backendUrl"] = "http://localhost:" + std::to_string(mockBackend.start(getPort()));
        mockXiva.waitUntilConnections(1, 0);
        UNIT_ASSERT_VALUES_EQUAL(endpoint.getCurrentConnectionUrl(),
                                 "wss://localhost:" + std::to_string(mockXiva.getPort()) +
                                     "?&service=quasar-realtime,messenger-prod&client=q&session=" + getDeviceForTests()->deviceId() +
                                     "&sign=1&ts=" + signTs + "&user=" + passportUid);
        messagePromise = std::make_unique<std::promise<std::string>>();
        mockXiva.send("{\"operation\":\"ping\",\"server-interval-sec\":60}");
        mockXiva.send("{\"operation\":\"push\",\"message\":\"lol\"}");

        UNIT_ASSERT_VALUES_EQUAL(XivaOperations::PUSH, messagePromise->get_future().get());
        UNIT_ASSERT_VALUES_EQUAL(mockXiva.getConnectionOpenedCnt(), 1);
        UNIT_ASSERT_VALUES_EQUAL(mockXiva.getConnectionClosedCnt(), 0);
        std::cout << "Done" << std::endl;
    }

    Y_UNIT_TEST_F(testPushdAuthStateChanged, ServicesInit)
    {
        auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);

        MockWSServer mockXiva(getPort());
        config["pushd"]["xivaSubscribeUrl"] = "wss://localhost:" + std::to_string(mockXiva.getPort());

        PushEndpoint endpoint(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockUserConfigProvider, false);
        mockXiva.waitUntilConnections(1, 0);
        UNIT_ASSERT_VALUES_EQUAL(endpoint.getCurrentConnectionUrl(),
                                 "wss://localhost:" + std::to_string(mockXiva.getPort()) +
                                     "?&service=quasar-realtime,messenger-prod&client=q&session=" + getDeviceForTests()->deviceId() +
                                     "&sign=1&ts=" + signTs + "&user=" + passportUid);
        mockXiva.send("{\"operation\":\"ping\",\"server-interval-sec\":60}");

        passportUid = "1234";
        auto authInfo = *mockAuthProvider->ownerAuthInfo().value();
        authInfo.passportUid = passportUid;
        mockAuthProvider->setOwner(authInfo);

        mockXiva.waitUntilConnections(2, 1);

        UNIT_ASSERT_VALUES_EQUAL(endpoint.getCurrentConnectionUrl(),
                                 "wss://localhost:" + std::to_string(mockXiva.getPort()) +
                                     "?&service=quasar-realtime,messenger-prod&client=q&session=" + getDeviceForTests()->deviceId() +
                                     "&sign=1&ts=" + signTs + "&user=" + passportUid);
    }

    Y_UNIT_TEST_F(testPushdSignExpired, ServicesInit)
    {
        signTs = std::to_string(time(nullptr) + 2);
        mockBackend.onHandlePayload = [&](const TestHttpServer::Headers& /*headers*/, const std::string& /*payload*/, TestHttpServer::HttpConnection& handler) {
            handler.doReplay(200, "text/html", "{"
                                               "   \"status\": \"ok\","
                                               "   \"sign\": \"1\","
                                               "   \"ts\": \"" +
                                                   signTs + "\""
                                                            "}");
        };
        auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);

        MockWSServer mockXiva(getPort());
        config["pushd"]["xivaSubscribeUrl"] = "wss://localhost:" + std::to_string(mockXiva.getPort());

        PushEndpoint endpoint(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockUserConfigProvider, false);
        mockXiva.waitUntilConnections(1, 0);

        UNIT_ASSERT_VALUES_EQUAL(endpoint.getCurrentConnectionUrl(),
                                 "wss://localhost:" + std::to_string(mockXiva.getPort()) +
                                     "?&service=quasar-realtime,messenger-prod&client=q&session=" + getDeviceForTests()->deviceId() +
                                     "&sign=1&ts=" + signTs + "&user=" + passportUid);

        signTs = std::to_string(time(nullptr) + 179);
        mockBackend.onHandlePayload = [&](const TestHttpServer::Headers& /*headers*/, const std::string& /*payload*/, TestHttpServer::HttpConnection& handler) {
            handler.doReplay(200, "text/html", "{"
                                               "   \"status\": \"ok\","
                                               "   \"sign\": \"1\","
                                               "   \"ts\": \"" +
                                                   signTs + "\""
                                                            "}");
        };
        mockXiva.send("{\"operation\":\"ping\",\"server-interval-sec\":60}");
        mockXiva.waitUntilConnections(2, 1);

        UNIT_ASSERT_VALUES_EQUAL(endpoint.getCurrentConnectionUrl(),
                                 "wss://localhost:" + std::to_string(mockXiva.getPort()) +
                                     "?&service=quasar-realtime,messenger-prod&client=q&session=" + getDeviceForTests()->deviceId() +
                                     "&sign=1&ts=" + signTs + "&user=" + passportUid);
    }

    Y_UNIT_TEST_F(testPushdJsonWithoutMessage, ServicesInit)
    {
        auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);

        MockWSServer mockXiva(getPort());
        config["pushd"]["xivaSubscribeUrl"] = "wss://localhost:" + std::to_string(mockXiva.getPort());

        PushEndpoint endpoint(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockUserConfigProvider, false);
        auto pushdConnector = createIpcConnectorForTests("pushd");
        std::unique_ptr<std::promise<std::string>> operationPromise = nullptr;
        std::unique_ptr<std::promise<std::string>> messagePromise = nullptr;
        pushdConnector->setMessageHandler([&](const auto& msg) {
            if (msg->has_push_notification()) {
                operationPromise->set_value(msg->push_notification().operation());
                messagePromise->set_value(msg->push_notification().message());
            }
        });
        pushdConnector->connectToService();
        pushdConnector->waitUntilConnected();

        mockXiva.waitUntilConnections(1, 0);

        /* prepare promises before send */
        messagePromise = std::make_unique<std::promise<std::string>>();
        operationPromise = std::make_unique<std::promise<std::string>>();
        mockXiva.send("{\"operation\":\"ping\",\"server-interval-sec\":60}");
        {
            Json::Value xivaBrokenMessage;
            xivaBrokenMessage["operation"] = XivaOperations::PUSH;
            mockXiva.send(jsonToString(xivaBrokenMessage));
        }

        // Waiting for message
        const auto operation = operationPromise->get_future().get();
        const auto message = messagePromise->get_future().get();
        UNIT_ASSERT_VALUES_EQUAL(XivaOperations::PUSH, operation);
        UNIT_ASSERT_VALUES_EQUAL(message, "{}");
        try {
            const auto stub = parseJson(message);
        } catch (const Json::Exception& e) {
            UNIT_FAIL("Message json is not valid!");
        }
    }

    Y_UNIT_TEST_F(testPushdInterfaceXivaMessage, ServicesInit)
    {
        auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);

        MockWSServer mockXiva(getPort());
        config["pushd"]["xivaSubscribeUrl"] = "wss://localhost:" + std::to_string(mockXiva.getPort());

        PushEndpoint endpoint(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockUserConfigProvider, false);
        auto pushdConnector = createIpcConnectorForTests("pushd");
        std::unique_ptr<std::promise<std::string>> operationPromise = nullptr;
        std::unique_ptr<std::promise<std::string>> messagePromise = nullptr;
        pushdConnector->setMessageHandler([&](const auto& msg) {
            if (msg->has_push_notification()) {
                operationPromise->set_value(msg->push_notification().operation());
            }
        });
        pushdConnector->connectToService();
        pushdConnector->waitUntilConnected();

        mockXiva.waitUntilConnections(1, 0);

        /* prepare promises before send */
        operationPromise = std::make_unique<std::promise<std::string>>();
        mockXiva.send("{\"operation\":\"ping\",\"server-interval-sec\":60}");
        {
            Json::Value xivaMessage;
            xivaMessage["operation"] = XivaOperations::WEB;
            xivaMessage["message"] = "some_message";
            mockXiva.send(jsonToString(xivaMessage));
        }

        // Waiting for message
        const std::string web_operation = operationPromise->get_future().get();
        /* Test That XivaOperations::WEB is "web" string. This string is used in java. So if it changes in c++
         * test should notify to change it in Android!!!
         */
        if (web_operation != "web") {
            UNIT_FAIL("XivaOperations::WEB string changed! Make sure it's same as in JAVA!");
        }

        /* prepare promises before send */
        operationPromise = std::make_unique<std::promise<std::string>>();
        mockXiva.send("{\"operation\":\"ping\",\"server-interval-sec\":60}");
        {
            Json::Value xivaMessage;
            xivaMessage["operation"] = XivaOperations::BOTTOM_SHEET;
            xivaMessage["message"] = "some message body to open bottom sheet on interface";
            mockXiva.send(jsonToString(xivaMessage));
        }

        // Waiting for message
        const std::string bottom_operation = operationPromise->get_future().get();
        /* Test That XivaOperations::BOTTOM_SHEET is "bottom_sheet" string. This string is used in java. So if it changes in c++
         * test should notify to change it in Android!!!
         */
        if (bottom_operation != "bottom_sheet") {
            UNIT_FAIL("XivaOperations::BOTTOM_SHEET string changed! Make sure it's same as in JAVA!");
        }
    }

    Y_UNIT_TEST_F(testPushdDropUndeclaredXivaOperation, ServicesInitWithMetrica) {
        auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);

        std::promise<std::string> metricaUnknownXivaValuePromise;
        std::promise<std::string> metricaBrokenXivaJsonValuePromise;
        setEventListener([&](const std::string& event, const std::string& eventJson, YandexIO::ITelemetry::Flags /*flags*/) {
            /* Pushd should send event when receive unknown Operation */
            if (event == "unknown_xiva_operation") {
                UNIT_ASSERT(!eventJson.empty());
                metricaUnknownXivaValuePromise.set_value(eventJson);
            } else if (event == "xiva_broken_json") {
                UNIT_ASSERT(!eventJson.empty());
                metricaBrokenXivaJsonValuePromise.set_value(eventJson);
            }
        });

        MockWSServer mockXiva(getPort());
        config["pushd"]["xivaSubscribeUrl"] = "wss://localhost:" + std::to_string(mockXiva.getPort());

        PushEndpoint endpoint(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockUserConfigProvider, false);
        auto pushdConnector = createIpcConnectorForTests("pushd");
        pushdConnector->setMessageHandler([&](const auto& msg) {
            /* Should not receive any push! */
            UNIT_ASSERT(!msg->has_push_notification());
        });
        pushdConnector->connectToService();
        pushdConnector->waitUntilConnected();

        mockXiva.waitUntilConnections(1, 0);

        /* ping to so pushd won't try to reconnect */
        mockXiva.send("{\"operation\":\"ping\",\"server-interval-sec\":60}");

        /* Check that unknown operation cause sending metrica */
        Json::Value xivaBrokenMessage;
        /* Send a xiva message with Not Declared operation in Quasar. Pushd should drop it and send metrica */
        xivaBrokenMessage["operation"] = "unknown_operation";
        const std::string xivaBrokenMessageJsonString = jsonToString(xivaBrokenMessage);
        mockXiva.send(xivaBrokenMessageJsonString);

        // Waiting for Metrica
        const std::string metricaValue = metricaUnknownXivaValuePromise.get_future().get();
        /* Make sure that pushd send whole json to metrica */
        UNIT_ASSERT_VALUES_EQUAL(metricaValue, xivaBrokenMessageJsonString);

        /* Send a xiva message with Not Declared operation in Quasar. Pushd should drop it and send metrica */
        const std::string xivaNotAJson = "{not a json}}";
        mockXiva.send(xivaNotAJson);

        // Waiting for another Metrica
        const std::string brokenJsonMetricaValue = metricaBrokenXivaJsonValuePromise.get_future().get();
        /* Make sure that pushd send whole json to metrica */
        UNIT_ASSERT_VALUES_EQUAL(brokenJsonMetricaValue, xivaNotAJson);
    }

    Y_UNIT_TEST_F(testPushdCustomXivaSubscribeUrl, ServicesInit)
    {
        auto& systemConfig = getDeviceForTests()->configuration()->getMutableConfig(testGuard);

        MockWSServer defaultMockXiva(getPort());
        const std::string DEFAULT_XIVA_URL = "wss://localhost:" + std::to_string(defaultMockXiva.getPort());
        const std::string REST_OF_URL = "?&service=quasar-realtime,messenger-prod&client=q&session=" +
                                        getDeviceForTests()->deviceId() + "&sign=1&ts=" + signTs + "&user=" + passportUid;

        systemConfig["pushd"]["xivaSubscribeUrl"] = DEFAULT_XIVA_URL;
        PushEndpoint endpoint(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockUserConfigProvider, false);

        waitUntil([&]() {
            return (endpoint.getCurrentConnectionUrl() == DEFAULT_XIVA_URL + REST_OF_URL);
        });

        // check that custom url is set properly
        MockWSServer customMockXiva(getPort());
        const std::string CUSTOM_XIVA_URL = "wss://localhost:" + std::to_string(customMockXiva.getPort());

        auto userConfig = quasar::UserConfig{.auth = quasar::UserConfig::Auth::SUCCESS};
        userConfig.system["xivaSubscribeUrl"] = CUSTOM_XIVA_URL;
        mockUserConfigProvider->setUserConfig(userConfig);

        waitUntil([&]() {
            return (endpoint.getCurrentConnectionUrl() == CUSTOM_XIVA_URL + REST_OF_URL);
        });

        // check that config url is restored properly
        userConfig = *mockUserConfigProvider->userConfig().value();
        userConfig.system.clear();
        mockUserConfigProvider->setUserConfig(userConfig);

        waitUntil([&]() {
            return (endpoint.getCurrentConnectionUrl() == DEFAULT_XIVA_URL + REST_OF_URL);
        });
    }

    Y_UNIT_TEST_F(testPushdSendBinaryMessage, ServicesInit)
    {
        auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);

        std::promise<std::string> promise;
        MockWSServer mockXiva(getPort());
        mockXiva.onBinaryMessage = [&promise](const std::string& message) mutable {
            promise.set_value(message);
        };

        config["pushd"]["xivaSubscribeUrl"] = "wss://localhost:" + std::to_string(mockXiva.getPort());

        PushEndpoint endpoint(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockUserConfigProvider, false);

        std::promise<void> xivaClientConnectedPromise;
        auto pushdConnector = createIpcConnectorForTests("pushd");
        pushdConnector->setConnectHandler([&pushdConnector]() {
            QuasarMessage message;
            message.mutable_subscribe_to_pushd_xiva_websocket_events();
            pushdConnector->sendMessage(std::move(message));
        });

        pushdConnector->setMessageHandler([&xivaClientConnectedPromise](const auto& message) mutable {
            if (message->has_xiva_websocket_client_state()) {
                if (message->xiva_websocket_client_state().state() == WebsocketClientState::CONNECTED) {
                    xivaClientConnectedPromise.set_value();
                }
            }
        });
        pushdConnector->connectToService();
        pushdConnector->waitUntilConnected();

        mockXiva.waitUntilConnections(1, 0);
        /* Make sure that xiva client is connected */
        xivaClientConnectedPromise.get_future().wait();

        {
            QuasarMessage message;
            *message.mutable_xiva_websocket_send_binary_message() = "binary_message";
            pushdConnector->sendMessage(std::move(message));
        }

        auto f = promise.get_future();
        UNIT_ASSERT_VALUES_EQUAL(f.get(), "binary_message");
    }

    Y_UNIT_TEST_F(testPushdRecvBinaryMessage, ServicesInit)
    {
        auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);

        MockWSServer mockXiva(getPort());

        config["pushd"]["xivaSubscribeUrl"] = "wss://localhost:" + std::to_string(mockXiva.getPort());

        PushEndpoint endpoint(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockUserConfigProvider, false);

        {
            // this connector doesn't recv binary message because it doesn't send "subscribe_to_pushd_xiva_websocket_events"
            auto pushdConnector = createIpcConnectorForTests("pushd");

            std::promise<std::string> promise;

            pushdConnector->setMessageHandler([&promise](const auto& message) mutable {
                if (message->has_xiva_websocket_recv_binary_message()) {
                    promise.set_value(message->xiva_websocket_recv_binary_message());
                }
            });

            pushdConnector->connectToService();
            pushdConnector->waitUntilConnected();

            mockXiva.sendBinary("binary_message");

            auto f = promise.get_future();
            UNIT_ASSERT(f.wait_for(std::chrono::seconds(1)) == std::future_status::timeout);
        }

        {
            std::promise<void> xivaClientConnectedPromise;
            std::promise<std::string> binaryMessagePromise;
            // this connector receives binary message because it sends "subscribe_to_pushd_xiva_websocket_events"
            auto pushdConnector = createIpcConnectorForTests("pushd");
            pushdConnector->setConnectHandler([&pushdConnector]() {
                QuasarMessage message;
                message.mutable_subscribe_to_pushd_xiva_websocket_events();
                pushdConnector->sendMessage(std::move(message));
            });

            pushdConnector->setMessageHandler([&xivaClientConnectedPromise, &binaryMessagePromise](const auto& message) mutable {
                if (message->has_xiva_websocket_client_state()) {
                    if (message->xiva_websocket_client_state().state() == WebsocketClientState::CONNECTED) {
                        xivaClientConnectedPromise.set_value();
                    }
                }
                if (message->has_xiva_websocket_recv_binary_message()) {
                    binaryMessagePromise.set_value(message->xiva_websocket_recv_binary_message());
                }
            });

            pushdConnector->connectToService();
            pushdConnector->waitUntilConnected();

            xivaClientConnectedPromise.get_future().wait();

            mockXiva.sendBinary("binary_message");

            auto f = binaryMessagePromise.get_future();
            UNIT_ASSERT_VALUES_EQUAL(f.get(), "binary_message");
        }
    }

    Y_UNIT_TEST_F(testPushdSendsStatus, ServicesInit)
    {
        auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);

        MockWSServer mockXiva(getPort());

        config["pushd"]["xivaSubscribeUrl"] = "wss://localhost:" + std::to_string(mockXiva.getPort());

        PushEndpoint endpoint(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockUserConfigProvider, false);

        std::promise<void> promise;
        auto pushdConnector = createIpcConnectorForTests("pushd");

        pushdConnector->setConnectHandler([&pushdConnector]() {
            QuasarMessage message;
            message.mutable_subscribe_to_pushd_xiva_websocket_events();
            pushdConnector->sendMessage(std::move(message));
        });

        pushdConnector->setMessageHandler([&promise](const auto& message) mutable {
            if (message->has_xiva_websocket_client_state()) {
                if (message->xiva_websocket_client_state().state() == WebsocketClientState::CONNECTED) {
                    promise.set_value();
                }
            }
        });

        pushdConnector->connectToService();
        pushdConnector->waitUntilConnected();

        auto f = promise.get_future();
        f.wait();
    }

    Y_UNIT_TEST_F(testPushdXivaSubscriptionsDefault, ServicesInit)
    {
        auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);

        MockWSServer mockXiva(getPort());

        config["pushd"]["xivaSubscribeUrl"] = "wss://localhost:" + std::to_string(mockXiva.getPort());

        PushEndpoint endpoint(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockUserConfigProvider, false);

        auto pushdConnector = createIpcConnectorForTests("pushd");
        pushdConnector->connectToService();
        pushdConnector->waitUntilConnected();

        QuasarMessage request;
        request.mutable_list_xiva_subscriptions();

        auto response = pushdConnector->sendRequestSync(std::move(request), std::chrono::seconds(5));
        UNIT_ASSERT(response->has_xiva_subscriptions());
        UNIT_ASSERT(response->xiva_subscriptions().xiva_subscriptions().size() == 2);
        UNIT_ASSERT_VALUES_EQUAL(response->xiva_subscriptions().xiva_subscriptions().Get(0), "quasar-realtime");
        UNIT_ASSERT_VALUES_EQUAL(response->xiva_subscriptions().xiva_subscriptions().Get(1), "messenger-prod");
    }

    Y_UNIT_TEST_F(testPushdXivaSubscriptionsCustomOneService, ServicesInit)
    {
        auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);

        MockWSServer mockXiva(getPort());

        config["pushd"]["xivaSubscribeUrl"] = "wss://localhost:" + std::to_string(mockXiva.getPort());
        config["pushd"]["xivaServices"] = "XXX";

        PushEndpoint endpoint(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockUserConfigProvider, false);

        auto pushdConnector = createIpcConnectorForTests("pushd");
        pushdConnector->connectToService();
        pushdConnector->waitUntilConnected();

        QuasarMessage request;
        request.mutable_list_xiva_subscriptions();

        auto response = pushdConnector->sendRequestSync(std::move(request), std::chrono::seconds(5));
        UNIT_ASSERT(response->has_xiva_subscriptions());
        UNIT_ASSERT(response->xiva_subscriptions().xiva_subscriptions().size() == 1);
        UNIT_ASSERT_VALUES_EQUAL(response->xiva_subscriptions().xiva_subscriptions().Get(0), "XXX");
    }

    Y_UNIT_TEST_F(testPushdXivaSubscriptionsCustomManyServices, ServicesInit)
    {
        auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);

        MockWSServer mockXiva(getPort());

        config["pushd"]["xivaSubscribeUrl"] = "wss://localhost:" + std::to_string(mockXiva.getPort());
        config["pushd"]["xivaServices"] = "XXX,YYY";

        PushEndpoint endpoint(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockUserConfigProvider, false);

        auto pushdConnector = createIpcConnectorForTests("pushd");
        pushdConnector->connectToService();
        pushdConnector->waitUntilConnected();

        QuasarMessage request;
        request.mutable_list_xiva_subscriptions();

        auto response = pushdConnector->sendRequestSync(std::move(request), std::chrono::seconds(5));
        UNIT_ASSERT(response->has_xiva_subscriptions());
        UNIT_ASSERT(response->xiva_subscriptions().xiva_subscriptions().size() == 2);
        UNIT_ASSERT_VALUES_EQUAL(response->xiva_subscriptions().xiva_subscriptions().Get(0), "XXX");
        UNIT_ASSERT_VALUES_EQUAL(response->xiva_subscriptions().xiva_subscriptions().Get(1), "YYY");
    }

    Y_UNIT_TEST_F(testPushdXivaSubscriptionsChanged, ServicesInit)
    {
        auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);

        MockWSServer mockXiva(getPort());

        config["pushd"]["xivaSubscribeUrl"] = "wss://localhost:" + std::to_string(mockXiva.getPort());
        config["pushd"]["xivaServices"] = "default1,default2";

        PushEndpoint endpoint(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockUserConfigProvider, false);

        std::mutex lock;
        quasar::SteadyConditionVariable cv;
        int xivaSubscriptionsUpdatedCounter = 0;
        std::vector<std::string> lastXivaSubscriptionsUpdate;

        auto pushdConnector = createIpcConnectorForTests("pushd");

        pushdConnector->setMessageHandler([&lock, &cv, &xivaSubscriptionsUpdatedCounter, &lastXivaSubscriptionsUpdate](const auto& message) {
            if (message->has_xiva_subscriptions()) {
                std::lock_guard<std::mutex> g(lock);
                ++xivaSubscriptionsUpdatedCounter;
                lastXivaSubscriptionsUpdate = {
                    message->xiva_subscriptions().xiva_subscriptions().begin(),
                    message->xiva_subscriptions().xiva_subscriptions().cend(),
                };
                cv.notify_one();
            }
        });

        pushdConnector->connectToService();
        pushdConnector->waitUntilConnected();

        // Check new connected client receives xiva subscriptions list from pushd
        {
            std::unique_lock<std::mutex> g(lock);
            cv.wait(g, [&xivaSubscriptionsUpdatedCounter]() {
                return xivaSubscriptionsUpdatedCounter > 0;
            });

            UNIT_ASSERT_VALUES_EQUAL(xivaSubscriptionsUpdatedCounter, 1);
            UNIT_ASSERT_VALUES_EQUAL(lastXivaSubscriptionsUpdate.size(), 2);
            UNIT_ASSERT_VALUES_EQUAL(lastXivaSubscriptionsUpdate[0], "default1");
            UNIT_ASSERT_VALUES_EQUAL(lastXivaSubscriptionsUpdate[1], "default2");
        }

        QuasarMessage request;
        request.mutable_list_xiva_subscriptions();
        auto response = pushdConnector->sendRequestSync(QuasarMessage{request}, std::chrono::seconds(5));
        UNIT_ASSERT(response->has_xiva_subscriptions());
        UNIT_ASSERT_VALUES_EQUAL(response->xiva_subscriptions().xiva_subscriptions().Get(0), "default1");
        UNIT_ASSERT_VALUES_EQUAL(response->xiva_subscriptions().xiva_subscriptions().Get(1), "default2");

        auto userConfig = quasar::UserConfig{.auth = quasar::UserConfig::Auth::SUCCESS};
        userConfig.system["pushd"]["xivaServices"] = "custom1,custom2";
        mockUserConfigProvider->setUserConfig(userConfig);

        // Check connected client receives xiva subscriptions list on ins modification
        {
            std::unique_lock<std::mutex> g(lock);
            cv.wait(g, [&xivaSubscriptionsUpdatedCounter]() {
                return xivaSubscriptionsUpdatedCounter > 1;
            });

            UNIT_ASSERT_VALUES_EQUAL(xivaSubscriptionsUpdatedCounter, 2);
            UNIT_ASSERT_VALUES_EQUAL(lastXivaSubscriptionsUpdate.size(), 2);
            UNIT_ASSERT_VALUES_EQUAL(lastXivaSubscriptionsUpdate[0], "custom1");
            UNIT_ASSERT_VALUES_EQUAL(lastXivaSubscriptionsUpdate[1], "custom2");
        }

        response = pushdConnector->sendRequestSync(QuasarMessage{request}, std::chrono::seconds(5));
        UNIT_ASSERT(response->has_xiva_subscriptions());
        UNIT_ASSERT_VALUES_EQUAL(response->xiva_subscriptions().xiva_subscriptions().Get(0), "custom1");
        UNIT_ASSERT_VALUES_EQUAL(response->xiva_subscriptions().xiva_subscriptions().Get(1), "custom2");

        userConfig = *mockUserConfigProvider->userConfig().value();
        userConfig.system.clear();
        mockUserConfigProvider->setUserConfig(userConfig);

        {
            std::unique_lock<std::mutex> g(lock);
            cv.wait(g, [&xivaSubscriptionsUpdatedCounter]() {
                return xivaSubscriptionsUpdatedCounter > 2;
            });

            UNIT_ASSERT_VALUES_EQUAL(xivaSubscriptionsUpdatedCounter, 3);
            UNIT_ASSERT_VALUES_EQUAL(lastXivaSubscriptionsUpdate.size(), 2);
            UNIT_ASSERT_VALUES_EQUAL(lastXivaSubscriptionsUpdate[0], "default1");
            UNIT_ASSERT_VALUES_EQUAL(lastXivaSubscriptionsUpdate[1], "default2");
        }

        response = pushdConnector->sendRequestSync(QuasarMessage{request}, std::chrono::seconds(5));
        UNIT_ASSERT(response->has_xiva_subscriptions());
        UNIT_ASSERT_VALUES_EQUAL(response->xiva_subscriptions().xiva_subscriptions().Get(0), "default1");
        UNIT_ASSERT_VALUES_EQUAL(response->xiva_subscriptions().xiva_subscriptions().Get(1), "default2");
    }

}

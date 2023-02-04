#include <yandex_io/libs/glagol_sdk/connector2.h>
#include <yandex_io/libs/glagol_sdk/discovery.h>
#include <yandex_io/libs/glagol_sdk/avahi_wrapper/avahi_browse_client.h>

#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/http_client/http_client.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/libs/telemetry/null/null_metrica.h>
#include <yandex_io/libs/websocket/websocket_server.h>

#include <yandex_io/interfaces/auth/mock/auth_provider.h>
#include <yandex_io/tests/testlib/test_certificates.h>
#include <yandex_io/tests/testlib/test_http_server.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <chrono>
#include <cstdio>
#include <future>
#include <iostream>
#include <map>
#include <memory>

using namespace quasar;
using namespace quasar::TestUtils;
using namespace glagol;
using namespace glagol::ext;

using namespace quasar::proto;

namespace {

    class MockDiscoveredItem: public DiscoveredItem {
        OnDiscovery callback;
        DeviceId id{.id = "id12", .platform = "platform12"};
        std::optional<ConnectionData> data;

    public:
        std::function<void()> onBadCert;
        std::function<void()> onDiscoverRequest;

        void doDiscovery(std::string host, int port, bool incorrectCert = false) {
            data = ConnectionData{
                .protocol = Protocol::IPV4,
                .uri = host + ":" + std::to_string(port),
                .tlsCertificate = (incorrectCert ? TestKeys().INCORRECT_PUBLIC_PEM : TestKeys().PUBLIC_PEM),
            };
            if (callback) {
                callback(*data);
            }
        }

        void invalidCert() override {
            UNIT_ASSERT(onBadCert);
            onBadCert();
        }
        void discover(OnDiscovery cb) override {
            callback = cb;
            if (onDiscoverRequest) {
                onDiscoverRequest();
            }
            if (data) {
                callback(*data);
            }
        }
        const DeviceId& getDeviceId() const override {
            return id;
        }
        void stopDiscovery() override {
            callback = nullptr;
        }
        ~MockDiscoveredItem() {
        }
    };

    struct MockTelemetry: public NullMetrica {
        std::vector<std::pair<std::string, std::string>> events;
        std::vector<std::string> errors;

        void reportEvent(const std::string& eventName, const std::string& eventValue, ITelemetry::Flags /*flags*/) override {
            YIO_LOG_INFO(eventName << ": " << eventValue);
            events.emplace_back(eventName, eventValue);
        }
        void reportError(const std::string& errorEventName, ITelemetry::Flags /*flags*/) override {
            errors.emplace_back(errorEventName);
        }

        void checkEvent(const char* name, const char* value) {
            UNIT_ASSERT(!events.empty());
            const auto& event = events.at(0);
            UNIT_ASSERT_VALUES_EQUAL(event.first, name);
            auto jsonVal = parseJson(event.second);
            UNIT_ASSERT_VALUES_EQUAL(jsonVal["api"].asString(), value);
        }

        template <typename Func_>
        void onEvent(const std::string& name, Func_ f) {
            auto iter = std::find_if(std::begin(events), std::end(events), [&name](const auto& p) { return p.first == name; });
            UNIT_ASSERT_C(iter != std::end(events), "Absent event " + name);
            auto jsonVal = parseJson(iter->second);
            f(jsonVal);
        }

        void checkConnectEvent(const std::string& name) {
            onEvent(name,
                    [](const auto& jsonVal) {
                        UNIT_ASSERT(jsonVal.isMember("deviceId"));
                        UNIT_ASSERT_EQUAL(jsonVal["deviceId"].asString(), "id12");
                        UNIT_ASSERT(jsonVal.isMember("platform"));
                        UNIT_ASSERT_EQUAL(jsonVal["platform"].asString(), "platform12");
                        UNIT_ASSERT(jsonVal.isMember("glagolConnectWsPeerL3Protocol"));
                        UNIT_ASSERT_EQUAL(jsonVal["glagolConnectWsPeerL3Protocol"].asString(), "ipv4");
                    });
        }
    };

    class MockMetricaFixture: public QuasarUnitTestFixture {
    public:
        std::shared_ptr<MockTelemetry> mockMetricaClient;

        using Base = QuasarUnitTestFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            mockMetricaClient = std::make_shared<MockTelemetry>();

            setDeviceForTests(std::make_unique<YandexIO::Device>(
                QuasarUnitTestFixture::makeTestDeviceId(),
                QuasarUnitTestFixture::makeTestConfiguration(),
                mockMetricaClient,
                QuasarUnitTestFixture::makeTestHAL()));
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            Base::TearDown(context);
        }
    };

    class ServicesInit: public MockMetricaFixture {
    public:
        std::string devicesJsonString =
            "{"
            "   \"status\": \"ok\","
            "   \"devices\": [{"
            "       \"activation_code\": 2248508915,"
            "       \"config\": {"
            "           \"masterDevice\": {"
            "               \"deviceId\": \"deviceId1\""
            "           },"
            "            \"name\": \"name1\""
            "       },"
            "       \"glagol\": {"
            "           \"security\": {"
            "               \"server_certificate\": \"" +
            TestKeys().PUBLIC_PEM + "\","
                                    "               \"server_private_key\": \"" +
            TestKeys().PRIVATE_PEM + "\""
                                     "           }"
                                     "       },"
                                     "       \"id\": \"id12\","
                                     "       \"name\": \"quasar\","
                                     "       \"platform\": \"platform12\","
                                     "       \"promocode_activated\": false,"
                                     "       \"tags\": [\"plus360\",\"amediateka90\"]"
                                     "   }]"
                                     "}";
        std::string devicesJsonIncorrectCertsString =
            "{"
            "   \"status\": \"ok\","
            "   \"devices\": [{"
            "       \"activation_code\": 2248508915,"
            "       \"config\": {"
            "           \"masterDevice\": {"
            "               \"deviceId\": \"deviceId1\""
            "           },"
            "            \"name\": \"name1\""
            "       },"
            "       \"glagol\": {"
            "           \"security\": {"
            "               \"server_certificate\": \"" +
            TestKeys().INCORRECT_PUBLIC_PEM + "\","
                                              "               \"server_private_key\": \"" +
            TestKeys().PRIVATE_PEM + "\""
                                     "           }"
                                     "       },"
                                     "       \"id\": \"id12\","
                                     "       \"name\": \"quasar\","
                                     "       \"platform\": \"platform12\","
                                     "       \"promocode_activated\": false,"
                                     "       \"tags\": [\"plus360\",\"amediateka90\"]"
                                     "   }]"
                                     "}";
        YandexIO::Configuration::TestGuard testConfigGuard;
        TestHttpServer mockBackend;
        TestHttpServer mockToSendFromMetricaServer;
        BackendApi::Settings backendSettings;
        Connector::Settings connectorSettings;
        std::unique_ptr<WebsocketServer> server;
        WebsocketServer::Settings wsSettings;
        std::shared_ptr<MockDiscoveredItem> mockDiscoveredItem;
        std::unique_ptr<Connector> connector;
        std::atomic<int> backendTokenRequestsCnt{0};
        std::atomic<int> backendCertRequestsCnt{0};
        std::atomic<bool> backendResponse200{true};
        std::atomic<bool> backendResponseIncorrectCertsOnce{false};

        using Base = MockMetricaFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            backendSettings = {
                .url = "http://localhost:" + std::to_string(mockBackend.start(getPort())),
                .token = "test_token",
            };

            mockDiscoveredItem = std::make_shared<MockDiscoveredItem>();

            /* Init curl before using it. Otherwise double free may be caused */
            HttpClient::globalInit();

            mockBackend.onHandlePayload = [&](const TestHttpServer::Headers& header,
                                              const std::string& /* payload */, TestHttpServer::HttpConnection& handler) {
                YIO_LOG_INFO("QUERY " << header.resource);
                UNIT_ASSERT_VALUES_EQUAL(header.getHeader("authorization"), "OAuth test_token");
                if (backendResponse200) {
                    if (header.resource == "/glagol/device_list") {
                        if (backendResponseIncorrectCertsOnce) {
                            backendResponseIncorrectCertsOnce = false;
                            handler.doReplay(200, "application/json", devicesJsonIncorrectCertsString);
                        } else {
                            handler.doReplay(200, "application/json", devicesJsonString);
                        }
                        ++backendCertRequestsCnt;
                    } else if (header.resource == "/glagol/token") {
                        handler.doReplay(200, "application/json", "{\"status\":\"ok\",\"token\":\"jwt_token\"}");
                        ++backendTokenRequestsCnt;
                    }
                } else {
                    handler.doReplay(500, "application/json", "{}");
                }
            };
            // connectorSettings.pingInterval = std::chrono::seconds(100);
            //         connectorSettings.deviceId = DeviceId{"id12", "platform12"};
            //  FIXME connectorSettings.backendPoller.retry.maxMs = 100;

            wsSettings.tls.keyPemBuffer = TestKeys().PRIVATE_PEM;
            wsSettings.tls.crtPemBuffer = TestKeys().PUBLIC_PEM; // TODO[katayd] check wsSettings in WebsocketServer
            wsSettings.port = getPort();
            server = std::make_unique<WebsocketServer>(wsSettings, std::make_shared<NullMetrica>());
            auto backendApi = std::make_shared<glagol::BackendApi>(std::make_shared<quasar::mock::AuthProvider>(), getDeviceForTests());
            backendApi->setSettings(backendSettings);
            connector = std::make_unique<Connector>(std::move(backendApi), mockMetricaClient);
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            Base::TearDown(context);
        }

        void connectorConnect() const {
            connector->connect(mockDiscoveredItem, connectorSettings);
        }
    };

    bool cmpStates(const std::vector<Connector::State>& expectation, const std::vector<Connector::State>& reality) {
        if (expectation == reality) {
            return true;
        }

        std::cout << std::endl
                  << "Expectation:" << std::endl;
        for (auto el : expectation) {
            std::cout << std::to_string(el) << " -> ";
        }
        std::cout << std::endl
                  << "Reality:" << std::endl;
        for (auto el : reality) {
            std::cout << std::to_string(el) << " -> ";
        }
        std::cout << std::endl;
        return false;
    }

} // namespace

Y_UNIT_TEST_SUITE(TestGlagolConnector) {
    Y_UNIT_TEST_F(testGlagolSDKBase, ServicesInit)
    {
        std::vector<Connector::State> all_states;
        connector->setOnStateChangedCallback([&all_states](Connector::State state) {
            all_states.push_back(state);
        });

        // FIXME UNIT_ASSERT(connector->getState() == Connector::State::STOPPED);
        connectorConnect();
        YIO_LOG_INFO("Discovering wait");
        UNIT_ASSERT(connector->waitForState(Connector::State::DISCOVERING));
        YIO_LOG_INFO("discovering catched");

        mockDiscoveredItem->doDiscovery("localhost", 1234); // "wrong" port to catch "connecting" state
        YIO_LOG_INFO("doDiscovery done");

        UNIT_ASSERT(connector->waitForState(Connector::State::CONNECTING));
        YIO_LOG_INFO("Connecting catched");

        mockDiscoveredItem->doDiscovery("localhost", server->start());

        UNIT_ASSERT(connector->waitForState(Connector::State::CONNECTED));

        std::promise<std::string> messagePromise;
        server->setOnMessageHandler([&](WebsocketServer::ConnectionHdl /* hdl */, const std::string& message) {
            YIO_LOG_INFO("GOT MESSAGE: " << message);
            messagePromise.set_value(message);
        });
        connector->send(model::RequestPayloadFactory::getPingPayload());

        auto jsonMessage = parseJson(messagePromise.get_future().get());
        UNIT_ASSERT_VALUES_EQUAL(jsonMessage["payload"]["command"].asString(), "ping");
        UNIT_ASSERT_VALUES_EQUAL(jsonMessage["conversationToken"].asString(), "jwt_token");

        server.reset(); // need to catch gsdkConnectWsClose event

        UNIT_ASSERT(connector->waitForState(Connector::State::CONNECTING));

        connector->disconnectAsync();

        UNIT_ASSERT(connector->waitForState(Connector::State::STOPPED));

        std::vector<Connector::State> all_states_expectation({
            Connector::State::DISCOVERING,
            Connector::State::REQUESTING_BACKEND,
            Connector::State::CONNECTING,
            Connector::State::CONNECTED,
            Connector::State::CONNECTING,
            Connector::State::DISCONNECTING,
            Connector::State::STOPPED,
        });

        UNIT_ASSERT(cmpStates(all_states_expectation, all_states));

        UNIT_ASSERT_VALUES_EQUAL(backendTokenRequestsCnt.load(), 1);

        mockMetricaClient->checkConnectEvent("gsdkConnectWsOpen");
        mockMetricaClient->checkConnectEvent("gsdkConnectWsClose");
        /*
    mockMetricaClient->onEvent(
        "gsdkDiscoveryMdnsSuccess",
        [](const auto &jsonVal) {
            UNIT_ASSERT(jsonVal.isMember("address"));
            UNIT_ASSERT_EQUAL(jsonVal["address"].asString(), "localhost");
            UNIT_ASSERT(jsonVal.isMember("port"));
            UNIT_ASSERT_EQUAL(jsonVal["port"].asInt(), 1234);
        });
    */
        connector.reset();
    }

    Y_UNIT_TEST_F(testGlagolSDKSendsWithResponses, ServicesInit)
    {
        // connectorSettings.pingInterval = std::chrono::seconds(100);
        connectorConnect();

        std::string responseId;
        server->setOnMessageHandler([this, &responseId](WebsocketServer::ConnectionHdl /* hdl */, const std::string& message) {
            YIO_LOG_INFO("WS SENDING RESPONSE");
            auto jsonMsg = parseJson(message);

            Json::Value responseJsonMsg;
            responseJsonMsg["id"] = responseId;
            responseJsonMsg["requestId"] = jsonMsg["id"].asString();
            responseJsonMsg["status"] = "SUCCESS";
            server->sendAll(Json::FastWriter().write(responseJsonMsg));
        });

        mockDiscoveredItem->doDiscovery("localhost", server->start());
        UNIT_ASSERT(connector->waitForState(Connector::State::CONNECTED));
        YIO_LOG_INFO("should be CONNECTED");

        std::promise<std::string> responsePromise;
        connector->setOnMessageCallback([&responsePromise](const model::IncomingMessage& response) {
            responsePromise.set_value(response.id);
        });
        responseId = "123";
        connector->send(model::RequestPayloadFactory::getPingPayload());
        YIO_LOG_INFO("send1");
        UNIT_ASSERT_VALUES_EQUAL(responsePromise.get_future().get(), responseId);
        YIO_LOG_INFO("received1");
        connector->setOnMessageCallback(nullptr);

        std::promise<std::string> responsePromise2;
        responseId = "124";
        connector->send(model::RequestPayloadFactory::getPingPayload(), [&responsePromise2](const model::IncomingMessage& response) {
            responsePromise2.set_value(response.id);
        });
        YIO_LOG_INFO("send2");
        UNIT_ASSERT_VALUES_EQUAL(responsePromise2.get_future().get(), responseId);
        YIO_LOG_INFO("received2");

        responseId = "125";
        auto response = connector->sendSync(model::RequestPayloadFactory::getPingPayload());
        UNIT_ASSERT_VALUES_EQUAL(response.id, responseId);
        UNIT_ASSERT(response.request && response.request->responseStatus == model::ResponseStatus::SUCCESS);
        connector.reset();
    }

    Y_UNIT_TEST_F(testGlagolStateUpdate, ServicesInit)
    {
        connectorConnect();

        mockDiscoveredItem->doDiscovery("localhost", server->start());

        UNIT_ASSERT(connector->waitForState(Connector::State::CONNECTED));
        YIO_LOG_INFO("should be CONNECTED");

        std::promise<std::string> responsePromise;
        connector->setOnMessageCallback([&responsePromise](const model::IncomingMessage& response) {
            responsePromise.set_value(response.id);
        });

        Json::Value responseJsonMsg;
        responseJsonMsg["id"] = "test";
        responseJsonMsg["status"] = "SUCCESS";
        server->sendAll(Json::FastWriter().write(responseJsonMsg));

        std::future<std::string> futureMsg = responsePromise.get_future();
        std::future_status status = futureMsg.wait_for(std::chrono::milliseconds(500));
        if (status == std::future_status::ready) {
            UNIT_ASSERT_VALUES_EQUAL(futureMsg.get(), "test");
        } else {
            UNIT_FAIL("future did not resolve in 500 ms");
        }
        connector.reset();
    }

    Y_UNIT_TEST_F(testGlagolSDKConnectorDisconnect, ServicesInit)
    {
        std::vector<Connector::State> all_states;
        connector->setOnStateChangedCallback([&all_states](Connector::State state) {
            all_states.push_back(state);
        });
        connectorConnect();

        mockDiscoveredItem->doDiscovery("localhost", server->start());

        UNIT_ASSERT(connector->waitForState(Connector::State::CONNECTED));

        connector->disconnectAsync();
        UNIT_ASSERT(connector->waitForState(Connector::State::STOPPED));

        std::vector<Connector::State> all_states_expectation({
            Connector::State::DISCOVERING,
            Connector::State::REQUESTING_BACKEND,
            Connector::State::CONNECTING,
            Connector::State::CONNECTED,
            Connector::State::DISCONNECTING,
            Connector::State::STOPPED,
        });

        UNIT_ASSERT(cmpStates(all_states_expectation, all_states));
        connector.reset();
    }

    Y_UNIT_TEST_F(testGlagolSDKInvalidToken, ServicesInit)
    {
        std::promise<WebsocketServer::ConnectionHdl> connectionHdlPromise;
        server->setOnOpenHandler([&connectionHdlPromise](WebsocketServer::ConnectionHdl connectionHdl) {
            connectionHdlPromise.set_value(connectionHdl);
        });

        std::vector<Connector::State> all_states;
        connector->setOnStateChangedCallback([&all_states](Connector::State state) {
            all_states.push_back(state);
        });
        connectorSettings.backend.retryDelay.init = std::chrono::milliseconds(100);
        connectorConnect();

        mockDiscoveredItem->doDiscovery("localhost", server->start());

        UNIT_ASSERT(connector->waitForState(Connector::State::CONNECTED));
        auto hdl = connectionHdlPromise.get_future().get();
        server->setOnOpenHandler(nullptr);
        //    backendResponse200 = false;
        server->close(hdl, "Invalid token", Websocket::StatusCode ::INVALID_TOKEN);
        UNIT_ASSERT(connector->waitForState(Connector::State::REQUESTING_BACKEND));
        server.reset();
        //    backendResponse200 = true;
        server = std::make_unique<WebsocketServer>(wsSettings, std::make_shared<NullMetrica>());
        server->start();
        UNIT_ASSERT(connector->waitForState(Connector::State::CONNECTED));
        connector->disconnectAsync();
        UNIT_ASSERT(connector->waitForState(Connector::State::STOPPED));

        std::vector<Connector::State> all_states_expectation({
            Connector::State::DISCOVERING,
            Connector::State::REQUESTING_BACKEND,
            Connector::State::CONNECTING,
            Connector::State::CONNECTED,
            Connector::State::REQUESTING_BACKEND,
            Connector::State::CONNECTING,
            Connector::State::CONNECTED,
            Connector::State::DISCONNECTING,
            Connector::State::STOPPED,
        });

        UNIT_ASSERT(cmpStates(all_states_expectation, all_states));
        UNIT_ASSERT(backendTokenRequestsCnt >= 2);
        connector.reset();
    }

    Y_UNIT_TEST_F(testGlagolSDKInvalidTokenFast, ServicesInit)
    {
        std::promise<WebsocketServer::ConnectionHdl> connectionHdlPromise;
        server->setOnOpenHandler([&connectionHdlPromise](WebsocketServer::ConnectionHdl connectionHdl) {
            connectionHdlPromise.set_value(connectionHdl);
        });

        std::vector<Connector::State> all_states;
        connector->setOnStateChangedCallback([&all_states](Connector::State state) {
            all_states.push_back(state);
        });
        connectorSettings.backend.retryDelay.init = std::chrono::milliseconds(100);
        connectorConnect();

        mockDiscoveredItem->onBadCert = []() {
            YIO_LOG_INFO("OnBadCert called");
        };

        mockDiscoveredItem->doDiscovery("localhost", server->start());

        UNIT_ASSERT(connector->waitForState(Connector::State::CONNECTED));
        auto hdl = connectionHdlPromise.get_future().get();
        server->setOnOpenHandler(nullptr);
        server->close(hdl, "Invalid token", Websocket::StatusCode ::INVALID_TOKEN);
        UNIT_ASSERT(connector->waitForState(Connector::State::REQUESTING_BACKEND));
        server.reset();
        server = std::make_unique<WebsocketServer>(wsSettings, std::make_shared<NullMetrica>());
        server->start();
        UNIT_ASSERT(connector->waitForState(Connector::State::CONNECTED));
        connector->disconnectAsync();
        UNIT_ASSERT(connector->waitForState(Connector::State::STOPPED));

        std::vector<Connector::State> all_states_expectation({
            Connector::State::DISCOVERING,
            Connector::State::REQUESTING_BACKEND,
            Connector::State::CONNECTING,
            Connector::State::CONNECTED,
            Connector::State::REQUESTING_BACKEND,
            Connector::State::CONNECTING,
            Connector::State::CONNECTED,
            Connector::State::DISCONNECTING,
            Connector::State::STOPPED,
        });

        UNIT_ASSERT(cmpStates(all_states_expectation, all_states));
        UNIT_ASSERT(backendTokenRequestsCnt >= 2);
        connector.reset();
    }

    Y_UNIT_TEST_F(testGlagolSDKInvalidCert, ServicesInit)
    {
        std::promise<WebsocketServer::ConnectionHdl> connectionHdlPromise;
        server->setOnOpenHandler([&connectionHdlPromise](WebsocketServer::ConnectionHdl connectionHdl) {
            connectionHdlPromise.set_value(connectionHdl);
        });

        std::vector<Connector::State> all_states;
        connector->setOnStateChangedCallback([&all_states](Connector::State state) {
            all_states.push_back(state);
        });
        connectorSettings.backend.retryDelay.init = std::chrono::milliseconds(100);
        connectorConnect();

        const auto port = server->start();
        bool wrongCertReported = false;
        mockDiscoveredItem->onBadCert = [&wrongCertReported, this, port]() {
            wrongCertReported = true;
            mockDiscoveredItem->onDiscoverRequest = [this, port]() {
                mockDiscoveredItem->doDiscovery("localhost", port);
            };
        };
        mockDiscoveredItem->doDiscovery("localhost", port, true);

        UNIT_ASSERT(connector->waitForState(Connector::State::CONNECTED));
        auto hdl = connectionHdlPromise.get_future().get();
        server->setOnOpenHandler(nullptr);

        std::vector<Connector::State> all_states_expectation({
            Connector::State::DISCOVERING,
            Connector::State::REQUESTING_BACKEND,
            Connector::State::CONNECTING,
            Connector::State::REQUESTING_BACKEND,
            Connector::State::CONNECTING,
            Connector::State::CONNECTED,
            Connector::State::DISCONNECTING,
            Connector::State::STOPPED,
        });
        connector->disconnectAsync();
        UNIT_ASSERT(connector->waitForState(Connector::State::STOPPED));

        UNIT_ASSERT(cmpStates(all_states_expectation, all_states));
        UNIT_ASSERT(backendTokenRequestsCnt == 1);
        UNIT_ASSERT(wrongCertReported);
        connector.reset();
    }
}

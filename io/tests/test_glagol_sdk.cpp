#include <yandex_io/libs/glagol_sdk/connector.h>
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

using namespace quasar::proto;

namespace {

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
            if (name != std::string_view("http_request")) {
                auto jsonVal = parseJson(event.second);
                UNIT_ASSERT_VALUES_EQUAL(jsonVal["api"].asString(), value);
            }
        }

        template <typename Func_>
        void onEvent(const std::string& name, Func_ f) {
            auto iter = std::find_if(std::begin(events), std::end(events), [&name](const auto& p) { return p.first == name; });
            UNIT_ASSERT(iter != std::end(events));
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
            "       \"networkInfo\": {"
            "           \"ip_addresses\": [\"1\",\"2\"],"
            "           \"mac_addresses\": [\"3\",\"4\"],"
            "           \"external_port\": 12345"
            "       },"
            "       \"group\":{\"devices\":[]},"
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
        BackendApi::Settings backendSettings;
        Connector::Settings connectorSettings;
        std::unique_ptr<WebsocketServer> server;
        WebsocketServer::Settings wsSettings;
        std::shared_ptr<AvahiBrowseClient> browseClient;
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

            /* Init curl before using it. Otherwise double free may be caused */
            HttpClient::globalInit();

            AvahiBrowseClient::initForTest();
            browseClient = std::make_shared<AvahiBrowseClient>();

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
            connectorSettings.deviceId = DeviceId{"id12", "platform12"};
            // FIXME connectorSettings.backendPoller.retry.maxMs = 100;

            wsSettings.tls.keyPemBuffer = TestKeys().PRIVATE_PEM;
            wsSettings.tls.crtPemBuffer = TestKeys().PUBLIC_PEM; // TODO[katayd] check wsSettings in WebsocketServer
            wsSettings.port = getPort();
            server = std::make_unique<WebsocketServer>(wsSettings, std::make_shared<NullMetrica>());
            auto discovery = std::make_shared<glagol::Discovery>(
                std::make_shared<quasar::NamedCallbackQueue>("testGlagolSDK_ServicesInit"),
                nullptr,
                browseClient,
                glagol::Discovery::Settings{.serviceType = "type", .serviceNamePrefix = ""});
            auto backendApi = std::make_shared<glagol::BackendApi>(std::make_shared<quasar::mock::AuthProvider>(), getDeviceForTests());
            backendApi->setSettings(backendSettings);
            connector = std::make_unique<Connector>(discovery, std::move(backendApi), mockMetricaClient);
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

Y_UNIT_TEST_SUITE(TestGlagolSdk) {
    Y_UNIT_TEST_F(testGlagolSDKBase, ServicesInit)
    {
        std::vector<Connector::State> all_states;
        connector->setOnStateChangedCallback([&all_states](Connector::State state) {
            all_states.push_back(state);
        });

        // FIXME UNIT_ASSERT(connector->getState() == Connector::State::STOPPED);
        connector->connect(connectorSettings);
        UNIT_ASSERT(connector->waitForState(Connector::State::DISCOVERING));

        AvahiBrowseClient::Item item{"name", "type", "local"};
        AvahiBrowseClient::Item::Info info{"hostname", 1234, "localhost", {}};
        info.txt = std::map<std::string, std::string>{{"deviceId", "id12"}, {"platform", "platform12"}};

        auto eventTrigger = browseClient->getTestEventTrigger();
        eventTrigger.onBrowserNew(item);
        eventTrigger.onNewResolved(item, info);
        UNIT_ASSERT(connector->waitForState(Connector::State::CONNECTING));
        eventTrigger.onBrowserRemove(item);

        info.hostname = "localhost";
        info.port = server->start();

        eventTrigger.onBrowserNew(item);
        eventTrigger.onNewResolved(item, info);

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

        UNIT_ASSERT_VALUES_EQUAL(backendTokenRequestsCnt.load(), 1);

        mockMetricaClient->checkConnectEvent("gsdkConnectWsOpen");
        mockMetricaClient->checkConnectEvent("gsdkConnectWsClose");

        mockMetricaClient->onEvent(
            "gsdkDiscoveryMdnsSuccess",
            [](const auto& jsonVal) {
                UNIT_ASSERT(jsonVal.isMember("address"));
                UNIT_ASSERT_EQUAL(jsonVal["address"].asString(), "localhost");
                UNIT_ASSERT(jsonVal.isMember("port"));
                UNIT_ASSERT_EQUAL(jsonVal["port"].asInt(), 1234);
            });
        connector.reset();
    }

    Y_UNIT_TEST_F(testGlagolSDKSendsWithResponses, ServicesInit)
    {
        // connectorSettings.pingInterval = std::chrono::seconds(100);
        connector->connect(connectorSettings);

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

        AvahiBrowseClient::Item item{"name", "type", "local"};
        AvahiBrowseClient::Item::Info info{"hostname", server->start(), "localhost", {}};
        info.txt = std::map<std::string, std::string>{{"deviceId", "id12"}, {"platform", "platform12"}};

        auto eventTrigger = browseClient->getTestEventTrigger();
        eventTrigger.onBrowserNew(item);
        eventTrigger.onNewResolved(item, info);
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
        connector->connect(connectorSettings);

        AvahiBrowseClient::Item item{"name", "type", "local"};
        AvahiBrowseClient::Item::Info info{"hostname", server->start(), "localhost", {}};
        info.txt = std::map<std::string, std::string>{{"deviceId", "id12"}, {"platform", "platform12"}};

        auto eventTrigger = browseClient->getTestEventTrigger();
        eventTrigger.onBrowserNew(item);
        eventTrigger.onNewResolved(item, info);
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

    Y_UNIT_TEST_F(testGlagolSDKConnectorDiscoveryLoss, ServicesInit)
    {
        std::vector<Connector::State> all_states;
        AvahiBrowseClient::initForTest();

        AvahiBrowseClient::Item item{"name", "type", "local"};
        AvahiBrowseClient::Item::Info info{"hostname", server->start(), "localhost", {}};
        info.txt = std::map<std::string, std::string>{{"deviceId", "id12"}, {"platform", "platform12"}};

        auto eventTrigger = browseClient->getTestEventTrigger();
        eventTrigger.onBrowserNew(item);
        eventTrigger.onNewResolved(item, info);

        connector->setOnStateChangedCallback([&all_states](Connector::State state) {
            all_states.push_back(state);
        });
        connector->connect(connectorSettings);

        eventTrigger.onBrowserNew(item);
        eventTrigger.onNewResolved(item, info);
        UNIT_ASSERT(connector->waitForState(Connector::State::CONNECTED));

        eventTrigger.onBrowserRemove(item);

        eventTrigger.onBrowserNew(item);
        eventTrigger.onNewResolved(item, info);

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

        AvahiBrowseClient::initForTest();

        AvahiBrowseClient::Item item{"name", "type", "local"};
        AvahiBrowseClient::Item::Info info{"hostname", server->start(), "localhost", {}};
        info.txt = std::map<std::string, std::string>{{"deviceId", "id12"}, {"platform", "platform12"}};

        auto eventTrigger = browseClient->getTestEventTrigger();
        eventTrigger.onBrowserNew(item);
        eventTrigger.onNewResolved(item, info);

        std::vector<Connector::State> all_states;
        connector->setOnStateChangedCallback([&all_states](Connector::State state) {
            all_states.push_back(state);
        });
        connectorSettings.backend.retryDelay.init = std::chrono::milliseconds(100);
        connector->connect(connectorSettings);

        eventTrigger.onBrowserNew(item);
        eventTrigger.onNewResolved(item, info);

        UNIT_ASSERT(connector->waitForState(Connector::State::CONNECTED));
        auto hdl = connectionHdlPromise.get_future().get();
        server->setOnOpenHandler(nullptr);
        backendResponse200 = false;
        server->close(hdl, "Invalid token", Websocket::StatusCode ::INVALID_TOKEN);
        UNIT_ASSERT(connector->waitForState(Connector::State::REQUESTING_BACKEND));
        backendResponse200 = true;
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

        AvahiBrowseClient::initForTest();
        AvahiBrowseClient::Item item{"name", "type", "local"};
        AvahiBrowseClient::Item::Info info{"hostname", server->start(), "localhost", {}};
        info.txt = std::map<std::string, std::string>{{"deviceId", "id12"}, {"platform", "platform12"}};

        auto eventTrigger = browseClient->getTestEventTrigger();
        eventTrigger.onBrowserNew(item);
        eventTrigger.onNewResolved(item, info);

        std::vector<Connector::State> all_states;
        connector->setOnStateChangedCallback([&all_states](Connector::State state) {
            all_states.push_back(state);
        });
        backendResponseIncorrectCertsOnce = true;
        connectorSettings.backend.retryDelay.init = std::chrono::milliseconds(100);
        connector->connect(connectorSettings);

        eventTrigger.onBrowserNew(item);
        eventTrigger.onNewResolved(item, info);

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
        UNIT_ASSERT(backendTokenRequestsCnt >= 2);
        connector.reset();
    }

    Y_UNIT_TEST_F(testGlagolSDKDiscoveryNoDeviceIdOrPlatform, QuasarUnitTestFixture)
    {
        AvahiBrowseClient::initForTest();
        auto browseClient = std::make_shared<AvahiBrowseClient>();

        Discovery::Settings settings;
        settings.serviceType = "type";
        settings.serviceNamePrefix = "";
        auto discovery = std::make_shared<Discovery>(nullptr, browseClient, settings, [](const Discovery::Result& /*res*/) {});

        AvahiBrowseClient::Item item{"name", "type", "local"};
        AvahiBrowseClient::Item::Info info{"localhost", 124, "localhost", {}};

        std::this_thread::sleep_for(std::chrono::seconds(1)); // works without sleep

        auto eventTrigger = browseClient->getTestEventTrigger();
        eventTrigger.onBrowserNew(item);
        eventTrigger.onNewResolved(item, info);

        eventTrigger.onBrowserRemove(item);
    }

    Y_UNIT_TEST_F(testGlagolSDKDiscoveryBadWeakPtr, QuasarUnitTestFixture)
    {
        AvahiBrowseClient::initForTest();
        auto browseClient = std::make_shared<AvahiBrowseClient>();

        Discovery::Settings settings;
        auto discovery = std::make_shared<Discovery>(nullptr, browseClient, settings, [](const Discovery::Result& /*res*/) {});
    }

    Y_UNIT_TEST_F(testGlagolSDKBackendApiDevice_parsing, ServicesInit)
    {
        glagol::BackendApi backendApi(std::make_shared<quasar::mock::AuthProvider>(), getDeviceForTests());
        backendApi.setSettings(backendSettings);

        UNIT_ASSERT(backendApi.getConnectedDevicesList().size() == 1);
        auto device = backendApi.getConnectedDevicesList().at(DeviceId{"id12", "platform12"});
        UNIT_ASSERT(device.config.masterDevice.id == "deviceId1");
        UNIT_ASSERT(device.config.name == "name1");
        UNIT_ASSERT(device.glagol.security.serverPrivateKey == TestKeys().PRIVATE_PEM);
        UNIT_ASSERT(device.glagol.security.serverCertificate == TestKeys().PUBLIC_PEM);
        UNIT_ASSERT(device.name == "quasar");
        UNIT_ASSERT(device.group.isObject());
        UNIT_ASSERT(device.group.isMember("devices"));
        UNIT_ASSERT(device.group["devices"].isArray());
        UNIT_ASSERT(!device.promocodeActivated);
        UNIT_ASSERT_EQUAL(device.networkInfo.externalPort, 12345);
        UNIT_ASSERT(device.networkInfo.IPs == std::vector<std::string>({"1", "2"}));
        UNIT_ASSERT(device.networkInfo.MACs == std::vector<std::string>({"3", "4"}));
        const auto& tags = device.tags;
        std::list<std::string> expectedTags{"plus360", "amediateka90"};
        auto checkCollections = [](const auto& a, const auto& b) {
            UNIT_ASSERT_VALUES_EQUAL(a.size(), b.size());
            for (const auto& val : a) {
                UNIT_ASSERT(std::find(b.begin(), b.end(), val) != b.end());
            }
        };
        checkCollections(tags, expectedTags);
        mockMetricaClient->checkEvent("http_request", "device_list");
    }

    Y_UNIT_TEST_F(testGlagolSDKBackendApiDevice500ResponseCode, MockMetricaFixture)
    {
        TestHttpServer mockBackend;
        mockBackend.onHandlePayload = [&](const TestHttpServer::Headers& header,
                                          const std::string& /* payload */, TestHttpServer::HttpConnection& handler) {
            UNIT_ASSERT_VALUES_EQUAL(header.getHeader("authorization"), "OAuth test_token");
            handler.doReplay(500, "application/json", "lol");
        };

        glagol::BackendApi::Settings settings;
        settings.url = "http://localhost:" + std::to_string(mockBackend.start(getPort()));
        settings.token = "test_token";
        glagol::BackendApi backendApi(std::make_shared<quasar::mock::AuthProvider>(), getDeviceForTests());
        backendApi.setSettings(settings);

        try {
            auto devices = backendApi.getConnectedDevicesList();
            UNIT_ASSERT(false);
        } catch (const glagol::BackendApi::Non200ResponseCodeException& exception) {
            UNIT_ASSERT_VALUES_EQUAL(exception.getResponseCode(), 500);
        }
    }

    Y_UNIT_TEST_F(testGlagolSDKBackendApiGetToken, MockMetricaFixture)
    {
        TestHttpServer mockBackend;
        mockBackend.onHandlePayload = [&](const TestHttpServer::Headers& header,
                                          const std::string& /* payload */, TestHttpServer::HttpConnection& handler) {
            UNIT_ASSERT_VALUES_EQUAL("device_id=id1&platform=platform1", header.query);
            UNIT_ASSERT_VALUES_EQUAL(header.getHeader("authorization"), "OAuth test_token");
            handler.doReplay(200, "application/json", "{\"status\":\"ok\", \"token\": \"token123\"}");
        };

        glagol::BackendApi::Settings settings;
        settings.url = "http://localhost:" + std::to_string(mockBackend.start(getPort()));
        settings.token = "test_token";
        glagol::BackendApi backendApi(std::make_shared<quasar::mock::AuthProvider>(), getDeviceForTests());
        backendApi.setSettings(settings);

        std::string token = backendApi.getToken(DeviceId{"id1", "platform1"});
        UNIT_ASSERT_VALUES_EQUAL(token, "token123");
        mockMetricaClient->checkEvent("http_request", "token");
    }

    Y_UNIT_TEST_F(testGlagolSDKBackendApiCheckToken, MockMetricaFixture)
    {
        TestHttpServer mockBackend;
        mockBackend.onHandlePayload = [&](const TestHttpServer::Headers& header,
                                          const std::string& payload, TestHttpServer::HttpConnection& handler) {
            UNIT_ASSERT_VALUES_EQUAL(payload, "token123");
            UNIT_ASSERT_VALUES_EQUAL(header.getHeader("authorization"), "OAuth test_token");
            if (header.resource == "/glagol/check_token") {
                handler.doReplay(200, "application/json", "{\"status\":\"ok\", \"valid\": true}");
            } else if (header.resource == "/glagol/v2.0/check_token") {
                handler.doReplay(200, "application/json", "{\"status\":\"ok\", \"owner\": true, \"guest\": false}");
            }
            handler.doReplay(500, "application/json", "{\"status\":\"fail\"}");
        };

        glagol::BackendApi::Settings settings;
        settings.url = "http://localhost:" + std::to_string(mockBackend.start(getPort()));
        settings.token = "test_token";
        glagol::BackendApi backendApi(std::make_shared<quasar::mock::AuthProvider>(), getDeviceForTests());
        backendApi.setSettings(settings);

        UNIT_ASSERT(backendApi.checkToken("token123"));
        mockMetricaClient->checkEvent("http_request", "check_token");
        auto result = backendApi.checkToken2("token123");
        UNIT_ASSERT(result.owner);
        UNIT_ASSERT(!result.guest);
    }

    Y_UNIT_TEST_F(networkInfoSerialize, ServicesInit) {
        using NetworkInfo = IBackendApi::NetworkInfo;
        NetworkInfo src{
            .ts = std::uint64_t(time(nullptr)),
            .IPs = {"1", "2", "3"},
            .MACs = {"4", "5", "6"},
            .wifiSsid = "test",
            .externalPort = 12345,
            .stereopairRole = RoleNest::LEADER,
        };
        auto str = src.serialize();
        NetworkInfo dst = NetworkInfo::fromJson(str);
        UNIT_ASSERT_EQUAL(src.ts, dst.ts);
        UNIT_ASSERT(src.IPs == dst.IPs);
        UNIT_ASSERT(src.MACs == dst.MACs);
        UNIT_ASSERT_EQUAL(src.wifiSsid, dst.wifiSsid);
        UNIT_ASSERT_EQUAL(src.externalPort, dst.externalPort);
        UNIT_ASSERT(src.stereopairRole == dst.stereopairRole);
    }
}

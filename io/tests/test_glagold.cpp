#include <yandex_io/services/glagold/discovered_items.h>
#include <yandex_io/services/glagold/glagol.h>
#include <yandex_io/services/glagold/glagol_ws_server.h>
#include <yandex_io/services/glagold/mdns_responder.h>
#include <yandex_io/services/glagold/mdnsd_nsd_messager.h>
#include <yandex_io/services/glagold/resolves_tracker.h>

#include <yandex_io/interfaces/auth/mock/auth_provider.h>
#include <yandex_io/interfaces/device_state/mock/device_state_provider.h>
#include <yandex_io/interfaces/multiroom/mock/multiroom_provider.h>
#include <yandex_io/interfaces/stereo_pair/mock/stereo_pair_provider.h>
#include <yandex_io/libs/base/directives.h>
#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/glagol_sdk/connector.h>
#include <yandex_io/libs/glagol_sdk/avahi_wrapper/avahi_browse_client.h>
#include <yandex_io/libs/ipc/i_server.h>
#include <yandex_io/libs/jwt/jwt.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/libs/telemetry/null/null_metrica.h>
#include <yandex_io/libs/websocket/websocket_server.h>
#include <yandex_io/tests/testlib/test_certificates.h>
#include <yandex_io/tests/testlib/test_certificates2.h>
#include <yandex_io/tests/testlib/test_hal.h>
#include <yandex_io/tests/testlib/test_http_server.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/telemetry_test_fixture.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>
#include <yandex_io/tests/testlib/null_endpoint_storage/null_endpoint_storage.h>
#include <yandex_io/tests/testlib/null_sdk/null_sdk_interface.h>

#include <library/cpp/testing/unittest/registar.h>

#include <chrono>
#include <cstdio>
#include <fstream>
#include <future>
#include <iostream>
#include <map>

using namespace quasar;
using namespace quasar::TestUtils;
using namespace glagol;
using namespace std::chrono;
using namespace testing;

using namespace quasar::proto;

namespace {
    struct RESOLVE_STRS {
        const static std::string TYPE;
        const static std::string PREFIX;
    };

    const std::string RESOLVE_STRS::TYPE = "_yandexio._tcp"; // FIXME : define this in one place
    const std::string RESOLVE_STRS::PREFIX = "YandexIOReceiver-";

    std::string genToken(const std::string& deviceId, const std::string& platform, time_t exp, bool guest = false) {
        JwtPtr jwt;
        jwt_t* t = nullptr;
        jwt_new(&t);
        jwt.reset(t);

        jwt_add_grant(jwt.get(), "iss", "quasar-backend");
        jwt_add_grant(jwt.get(), "aud", "glagol");
        jwt_add_grant(jwt.get(), "sub", deviceId.c_str());
        jwt_add_grant_int(jwt.get(), "exp", exp);
        jwt_add_grant(jwt.get(), "plt", platform.c_str());
        if (guest) {
            jwt_add_grant_bool(jwt.get(), "gst", true);
        }
        jwt_set_alg(jwt.get(), JWT_ALG_HS256, (const unsigned char*)"key", 3);
        auto encodedString = JwtStringPtr(jwt_encode_str(jwt.get()));
        return std::string(encodedString.get());
    }

    QuasarMessage makeAccountDevicesListMessage(const glagol::BackendApi::DevicesMap& accountDevices) {
        QuasarMessage message;
        auto devices = message.mutable_account_devices_list();
        for (auto& [id, dev] : accountDevices) {
            auto device = devices->add_account_devices();
            device->set_id(TString(id.id));
            device->set_platform(TString(id.platform));
            device->set_server_certificate(TString(dev.glagol.security.serverCertificate));
            device->set_server_private_key(TString(dev.glagol.security.serverPrivateKey));
            device->set_guest_mode(dev.guestMode);
            device->set_name(TString(dev.name));
        }
        return message;
    }

    QuasarMessage makeTestNetworkStatusMessage() {
        QuasarMessage message;
        auto networkStatus = message.mutable_network_status();
        auto wifiStatus = networkStatus->mutable_wifi_status();
        auto wifiInfo = wifiStatus->mutable_current_network();
        wifiInfo->set_ssid("TEST_WIFI_SSID");
        wifiInfo->set_mac("TEST_WIFI_MAC");
        return message;
    }

    std::string randomLowercaseLatinString(std::random_device& rd) {
        std::uniform_int_distribution<int> dist(0, 25);
        std::string rval;
        const int len = 10 + dist(rd);
        for (int i = 0; i < len; ++i) {
            rval += static_cast<char>('a' + dist(rd));
        };
        return rval;
    };

    class ServicesInit: public TelemetryTestFixture {
    public:
        std::string devicesJsonString;
        TestUtils::TestHttpServer mockBackend;
        Connector::Settings connectorSettings;
        std::shared_ptr<ipc::IServer> mockFirstRun;
        ipc::IServer::MessageHandler syncdMessageHandler;
        std::shared_ptr<ipc::IServer> mockSyncd;
        std::shared_ptr<ipc::IServer> mockVideod;
        std::shared_ptr<ipc::IServer> mockMediad;
        std::shared_ptr<ipc::IServer> mockUpdatesd;
        ipc::IServer::MessageHandler alicedMessageHandler;
        std::shared_ptr<ipc::IServer> mockAliced;
        ipc::IServer::MessageHandler excomMessageHandler;
        std::shared_ptr<ipc::IServer> mockExcom;
        ipc::IServer::MessageHandler mdnsdMessageHandler;
        std::shared_ptr<ipc::IServer> mockMdnsd;
        std::shared_ptr<ipc::IServer> mockNetworkd;

        std::shared_ptr<mock::AuthProvider> mockAuthProvider;
        std::shared_ptr<mock::DeviceStateProvider> mockDeviceStateProvider;
        std::shared_ptr<mock::MultiroomProvider> mockMultiroomProvider;
        std::shared_ptr<mock::StereoPairProvider> mockStereoPairProvider;

        YandexIO::Configuration::TestGuard testGuard;
        std::string jwtToken;
        std::string fixtureBackendUrl;
        std::atomic<int> deviceListRequestsCnt{0};
        SteadyConditionVariable deviceListRequestCondVar;

        glagol::BackendApi::DevicesMap accountDevices;

        std::string makeClusterHelo(auto fromDeviceId, const std::string& token) {
            Json::Value val = Json::objectValue;
            val["conversationToken"] = token;
            auto& payload = val["payload"];
            payload["from_device_id"] = fromDeviceId;
            payload["command"] = "clusterHelo";
            return jsonToString(val);
        };

        std::string makeClusterHelo(auto fromDeviceId) {
            return makeClusterHelo(fromDeviceId, jwtToken);
        };

        std::string genMyToken(time_t exp, bool guestMode = false) {
            const Json::Value& config = getDeviceForTests()->configuration()->getCommonConfig();
            return genToken(getDeviceForTests()->deviceId(), config["deviceType"].asString(), exp, guestMode);
        }

        std::string genMyGuestToken() {
            return genMyToken(time(nullptr) + 60, true);
        }

        static glagol::BackendApi::DevicesMap makeAccountDevices(const std::string& deviceId) {
            glagol::BackendApi::DevicesMap result;
            glagol::DeviceId id{deviceId, "yandexstation"};
            auto& dev = result[id];
            dev.glagol.security.serverCertificate = TestKeys().PUBLIC_PEM;
            dev.glagol.security.serverPrivateKey = TestKeys().PRIVATE_PEM;
            dev.name = "quasar";
            return result;
        }

        void initDevicesJsonString() {
            devicesJsonString =
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
                                         "       \"id\": \"" +
                getDeviceForTests()->deviceId() + "\","
                                                  "       \"name\": \"quasar\","
                                                  "       \"platform\": \"yandexstation\","
                                                  "       \"promocode_activated\": false,"
                                                  "       \"tags\": [\"plus360\",\"amediateka90\"]"
                                                  "   },{"
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
                                         "       \"id\": \"" +
                getDeviceForTests()->deviceId() + "_1\","
                                                  "       \"name\": \"quasar\","
                                                  "       \"platform\": \"yandexmini\","
                                                  "       \"promocode_activated\": false,"
                                                  "       \"tags\": [\"plus360\",\"amediateka90\"]"
                                                  "   }]"
                                                  "}";
        }

        using Base = TelemetryTestFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            Json::Value& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
            accountDevices = makeAccountDevices(getDeviceForTests()->deviceId());

            const auto backendPort = getPort();
            config["common"]["backendUrl"] = fixtureBackendUrl = "localhost:" + std::to_string(backendPort);
            config["glagold"]["externalPort"] = getPort();

            mockBackend.onHandlePayload = [&](const TestHttpServer::Headers& headers, const std::string& body, TestHttpServer::HttpConnection& handler) {
                YIO_LOG_INFO("QUERYYYYYYYYY: " << headers.resource << ", HEADERSSSSS: " << headers.getHeader("authorization"));
                UNIT_ASSERT(headers.getHeader("authorization") == "OAuth test_token" || headers.getHeader("authorization") == "OAuth test_token1");
                if (headers.resource == "/glagol/device_list") {
                    ++deviceListRequestsCnt;
                    deviceListRequestCondVar.notify_one();
                    handler.doReplay(200, "application/json", devicesJsonString);
                } else if (headers.resource == "/glagol/token") {
                    auto reqDeviceId = headers.queryParams.getValue("device_id");
                    auto returnToken =
                        reqDeviceId == getDeviceForTests()->deviceId()
                            ? jwtToken
                            : genToken(reqDeviceId, headers.queryParams.getValue("platform"), time(nullptr) + 60);
                    YIO_LOG_INFO("returning token: " << returnToken);
                    handler.doReplay(200, "application/json", "{\"status\":\"ok\",\"token\":\"" + returnToken + "\"}");
                } else if (headers.resource == "/glagol/check_token") {
                    handler.doReplay(200, "application/json", "{\"status\":\"ok\",\"valid\": true}");
                } else if (headers.resource == "/glagol/v2.0/check_token") {
                    auto parsedToken = decodeJWT(body);
                    if (getBoolOrFalseGrantFromJWT(parsedToken.get(), "gst")) {
                        handler.doReplay(200, "application/json", "{\"status\":\"ok\",\"owner\": false, \"guest\": true}");
                    } else {
                        handler.doReplay(200, "application/json", "{\"status\":\"ok\",\"owner\": true, \"guest\": false}");
                    }
                }
            };

            mockFirstRun = createIpcServerForTests("firstrund");
            mockFirstRun->setClientConnectedHandler([](auto& connection) {
                QuasarMessage message;
                message.set_configuration_state(quasar::proto::ConfigurationState::CONFIGURED);
                connection.send(std::move(message));
            });

            mockUpdatesd = createIpcServerForTests("updatesd");
            mockAliced = createIpcServerForTests("aliced");
            mockAliced->setMessageHandler([this](const auto& msg, auto& conn) {
                if (alicedMessageHandler) {
                    alicedMessageHandler(msg, conn);
                }
            });
            mockMediad = createIpcServerForTests("mediad");
            mockVideod = createIpcServerForTests("videod");
            mockNetworkd = createIpcServerForTests("networkd");

            mockSyncd = createIpcServerForTests("syncd");
            mockSyncd->setMessageHandler([this](const auto& msg, auto& conn) {
                if (syncdMessageHandler) {
                    syncdMessageHandler(msg, conn);
                }
            });
            mockSyncd->setClientConnectedHandler([this](auto& connection) {
                connection.send(makeAccountDevicesListMessage(accountDevices));
            });

            mockMdnsd = createIpcServerForTests("mdnsd");
            mockMdnsd->setMessageHandler([this](const auto& msg, auto& conn) {
                if (mdnsdMessageHandler) {
                    mdnsdMessageHandler(msg, conn);
                }
            });
            if (!mdnsdMessageHandler) {
                setMockMdnsdMessageHandler([](const auto& message, auto& connection) {
                    if (message->has_nsd_disable()) {
                        QuasarMessage answer;
                        answer.mutable_nsd_disable_completed();
                        connection.send(std::move(answer));
                    }
                });
            }

            mockExcom = createIpcServerForTests("external_commandsd");
            mockExcom->setMessageHandler([this](const auto& msg, auto& conn) {
                if (excomMessageHandler) {
                    excomMessageHandler(msg, conn);
                }
            });

            jwtToken = genMyToken(time(nullptr) + 60);

            mockBackend.start(backendPort);

            mockFirstRun->listenService();
            mockUpdatesd->listenService();
            mockAliced->listenService();
            mockMediad->listenService();
            mockVideod->listenService();
            mockSyncd->listenService();
            mockExcom->listenService();
            mockMdnsd->listenService();
            mockNetworkd->listenService();

            mockAuthProvider = std::make_shared<mock::AuthProvider>();
            mockAuthProvider->setOwner(
                AuthInfo2{
                    .source = AuthInfo2::Source::AUTHD,
                    .authToken = "test_token",
                    .passportUid = "123",
                    .tag = 1600000000,
                });
            mockDeviceStateProvider = std::make_shared<mock::DeviceStateProvider>();
            mockDeviceStateProvider->setDeviceState(mock::defaultDeviceState());

            mockMultiroomProvider = std::make_shared<mock::MultiroomProvider>();
            mockStereoPairProvider = std::make_shared<mock::StereoPairProvider>();
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            Base::TearDown(context);
        }

        void setSyncdMessageHandler(ipc::IServer::MessageHandler handler) {
            syncdMessageHandler = std::move(handler);
        }

        void setAlicedMessageHandler(ipc::IServer::MessageHandler handler) {
            alicedMessageHandler = std::move(handler);
        }

        void setExternalCommandsMessageHandler(ipc::IServer::MessageHandler handler) {
            excomMessageHandler = std::move(handler);
        }

        void setMockMdnsdMessageHandler(ipc::IServer::MessageHandler handler) {
            mdnsdMessageHandler = std::move(handler);
        }
    };

    /**
     * Fixture to run a simple interaction tests with live Glagol.
     *
     * use oneshot(Json::Value) to send a request.
     */
    class GlagolInit: public ServicesInit {
    public:
        class TestBackedApi: public glagol::IBackendApi {
            const std::string token;

        public:
            std::function<void(const DeviceId&)> invalidCertCb_;

            TestBackedApi(const std::string t)
                : token(t)
                      {};

            DevicesMap getConnectedDevicesList() override {
                return {};
            }

            std::string getToken(const DeviceId& /*deviceId*/) override {
                return token;
            }
            bool checkToken(const std::string& /*token*/) override {
                return true;
            }

            TokenCheckResult checkToken2(const std::string& /*token*/) override {
                return {.owner = true, .guest = false};
            }

            void invalidCert(const DeviceId& id) override {
                if (invalidCertCb_) {
                    invalidCertCb_(id);
                }
            }
            void invalidToken(const DeviceId& /*deviceId*/) override {
            }
        };

        std::shared_ptr<TestBackedApi> makeTestBackendApi() {
            return std::make_shared<TestBackedApi>(jwtToken);
        }

        static GlagolCluster::Settings makeClusterSettings() {
            GlagolCluster::Settings result;
            result.peerCleanupPeriod = std::chrono::seconds(5);
            result.peerKpaTimeout = std::chrono::seconds(4);
            result.peersExchange = false;
            result.saveFile = std::string();
            return result;
        }

        class MockResponderStatus: public ResponderStatus {
            void pollCompleted(std::string /*lastStatus*/) override{};

            std::string getResponderStatusJson() override {
                return "mock";
            }

            bool isNetworkAvailable() const override {
                return true;
            }
        };

        class MockMndsHolder: public MDNSHolder {
            std::shared_ptr<ResponderStatus> responderStatus = std::make_shared<MockResponderStatus>();
            ResolveHandler* resolveHandler;

        public:
            MockMndsHolder(ResolveHandler* handler)
                : resolveHandler(handler)
                      {};

            std::shared_ptr<ResponderStatus> getResponderStatusProvider() override {
                return responderStatus;
            }
            void setNetworkAvailability(bool /*isAvailable*/) override{};
            void reconfigure() override{};

            void anounce(const DeviceId& id, const std::string& address, int port) {
                ResolveItem item{
                    .name = RESOLVE_STRS::PREFIX + id.id,
                    .type = RESOLVE_STRS::TYPE,
                    .domain = "local",
                };
                ResolveItem::Info info{
                    .hostname = id.id,
                    .port = port,
                    .address = address,
                };

                info.txt = {
                    {"deviceId", id.id},
                    {"platform", id.platform},
                    {"cluster", "yes"}};

                resolveHandler->newResolve(item, info);
            }
        };

        std::shared_ptr<MockMndsHolder> mockMdnsHolder;

        virtual MDNSHolderFactory makeMockMdnsHolderFactory() {
            return [this](std::shared_ptr<YandexIO::IDevice> /*device*/, const MdnsSettings& /*settings*/, ResolveHandler* resolveHandler, MDNSTransportOpt /*options*/) -> MDNSHolderPtr {
                mockMdnsHolder = std::make_shared<MockMndsHolder>(resolveHandler);
                return mockMdnsHolder;
            };
        };

        SteadyConditionVariable oneShotClientConnectedCondVar;
        std::mutex oneShotClientConnectedMutex;
        bool oneShotClientConnected{false};
        std::shared_ptr<MdnsdMessagerFactory> mdnsdMessagerFactory_;
        std::shared_ptr<Glagol> glagol;
        std::shared_ptr<WebsocketClient::DisposableClient> glagolOneShotClient;
        std::promise<bool> glagolOneShotSuccess;

        using Base = ServicesInit;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            const Json::Value& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);

            int port = config["glagold"]["externalPort"].asInt();
            auto ipcFactory = ipcFactoryForTests();
            mdnsdMessagerFactory_ = std::make_unique<MdnsdMessagerFactory>(ipcFactory);
            glagol = std::make_shared<Glagol>(
                getDeviceForTests(),
                ipcFactoryForTests(),
                mockAuthProvider,
                mockDeviceStateProvider,
                mockMultiroomProvider,
                mockStereoPairProvider,
                makeMockMdnsHolderFactory(),
                config["glagold"],
                Glagol::Settings{
                    .heartbeatTelemetry = true,
                    .nsdInsteadOfAvahi = true,
                    .skipConnectionResolves = true,
                    .avahi = {.restrictToIPv4 = true},
                    .cluster = makeClusterSettings(),
                },
                std::make_shared<YandexIO::NullSDKInterface>());
            /* Wait until GlagolWsServer will create WebsocketServer */
            glagol->waitWsServerStart();

            WebsocketClient::DisposableClient::Settings settings;
            settings.tls.crtBuffer = TestKeys().PUBLIC_PEM;
            settings.tls.verifyHostname = false;
            glagolOneShotClient = WebsocketClient::DisposableClient::create("wss://localhost:" + std::to_string(port) + "/", settings, std::make_shared<NullMetrica>());
            glagolOneShotClient->onConnect = [this](std::weak_ptr<WebsocketClient::DisposableClient> /*client*/) {
                std::lock_guard<std::mutex> guard(oneShotClientConnectedMutex);
                oneShotClientConnected = true;
                oneShotClientConnectedCondVar.notify_one();
            };
            glagolOneShotClient->onMessage = [this](std::weak_ptr<WebsocketClient::DisposableClient> /*client*/, const std::string& msg) {
                Json::Value jmsg = parseJson(msg);

                if (jmsg["status"] == "SUCCESS") {
                    glagolOneShotSuccess.set_value(true);
                } else {
                    YIO_LOG_WARN("Received response but not success: <" << msg << ">");
                }
            };

            glagolOneShotClient->connectAsyncWithTimeout();
            /* Make sure that client is connected */
            waitUntil(oneShotClientConnectedCondVar, oneShotClientConnectedMutex, [this]() {
                return oneShotClientConnected;
            });
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            Base::TearDown(context);
        }

        // sends a one-shot JSON request, waits for Glagol to return a success response for that (indefinitely)

        void oneshot(const Json::Value request, const std::string& token) {
            Json::Value actualRequest(request);
            actualRequest["conversationToken"] = token;

            glagolOneShotClient->send(jsonToString(actualRequest));
            UNIT_ASSERT(glagolOneShotSuccess.get_future().get() == true);
        }

        void oneshot(const Json::Value request) {
            oneshot(request, jwtToken);
        }

        int getFixtureExternalPort() {
            auto& config = getDeviceForTests()->configuration()->getServiceConfig("glagold");
            return config["externalPort"].asInt();
        }
    };

    struct GlagolInitNsd: public GlagolInit {
        MDNSHolderFactory makeMockMdnsHolderFactory() override {
            return createDefaultMndsFactory(*mdnsdMessagerFactory_);
        }

        void SetUp(NUnitTest::TTestContext& context) override {
            setMockMdnsdMessageHandler([this](const auto& message, auto& connection) {
                if (message->has_nsd_disable()) {
                    YIO_LOG_INFO("nsd_disable came");
                    nsdEnabled = false;
                    QuasarMessage answer;
                    answer.mutable_nsd_disable_completed();
                    connection.send(std::move(answer));
                }
                if (message->has_nsd_enable()) {
                    YIO_LOG_INFO("nsd_enable came");
                    nsdEnabled = true;
                    nsdEnablePromise.set_value();
                }
            });
            GlagolInit::SetUp(context);
        }

        std::atomic_bool nsdEnabled{true};
        std::promise<void> nsdEnablePromise;
    };

    class DiscoveredItemsBadTlsFixture: public GlagolInit {
    public:
        std::shared_ptr<TestBackedApi> testBackedApi;
        std::string ID_PREFIX;
        glagol::DeviceId fixtureDeviceId;
        std::shared_ptr<IDiscoveredItems> dItems;
        std::mutex connMutex;
        std::condition_variable connCond;
        glagol::IDiscoveredItems::ConnectorPtr connected;

        void waitForConnection() {
            YIO_LOG_INFO("Wait for connection");
            std::unique_lock lock(connMutex);
            bool success = connCond.wait_for(lock, std::chrono::seconds(50),
                                             [this]() {
                                                 return connected != nullptr;
                                             });
            UNIT_ASSERT_C(success, "Timeouted while waiting for connection");
            YIO_LOG_INFO("Connected");
        }

        struct Storage: public IDiscoveredItems::Storage {
            std::string content;
            void save(const std::string& data) override {
                content = data;
            }

            std::string getContent() override {
                return content;
            }
        };

        std::shared_ptr<Storage> storage;

        using Base = GlagolInit;

        void makeDiscoveryItems() {
            dItems = glagol::createDiscoveredItems({.id = ID_PREFIX + "_2", .platform = "yandexstation_2"},
                                                   getDeviceForTests()->telemetry(),
                                                   testBackedApi,
                                                   storage,
                                                   [this]() {
                                                       return std::make_shared<glagol::ext::Connector>(testBackedApi,
                                                                                                       std::make_shared<NullMetrica>());
                                                   });

            dItems->setOnDiscoveryCallback([this](const glagol::DeviceId& /*deviceId*/, glagol::IDiscoveredItems::ConnectorPtr connector) {
                connector->setOnStateChangedCallback([](auto /*state*/) {}); // do nothing in tests
                std::scoped_lock lock(connMutex);
                connected = connector;
                connCond.notify_one();
            });
        }

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            storage = std::make_shared<Storage>();

            testBackedApi = makeTestBackendApi();
            ID_PREFIX = getDeviceForTests()->deviceId();
            fixtureDeviceId = glagol::DeviceId{.id = ID_PREFIX, .platform = "yandexstation"};
            makeDiscoveryItems();

            glagol::DeviceId moreId = {
                .id = ID_PREFIX + "_2",
                .platform = "yandexstation_2",
            };
            auto& dev = accountDevices[moreId];
            dev.name = "quasar";
            dev.glagol.security.serverCertificate = TestKeys().INCORRECT_PUBLIC_PEM;
            dev.glagol.security.serverPrivateKey = TestKeys().PRIVATE_PEM;
            accountDevices[fixtureDeviceId].glagol.security.serverCertificate = TestKeys().INCORRECT_PUBLIC_PEM;
            dItems->updateAccountDevices(accountDevices);
        };

        void TearDown(NUnitTest::TTestContext& context) override {
            Base::TearDown(context);
        }
    };

    struct TestResolveHandler: public quasar::ResolveHandler {
        const std::string prefix;
        std::mutex mutex;
        std::vector<std::tuple<ResolveItem, ResolveItem::Info>> resolves;
        std::condition_variable cond;

        TestResolveHandler(std::string p)
            : prefix(std::move(p))
        {
        }

        void newResolve(const ResolveItem& item, const ResolveItem::Info& details) override {
            YIO_LOG_INFO("Resolved " << item.name << " type(" << item.type << ") domain(" << item.domain
                                     << ") hostname(" << details.hostname << ") port(" << details.port << ")address(" << details.address << ")");
            if (details.hostname == prefix + ".local") {
                std::lock_guard<std::mutex> lock(mutex);
                resolves.emplace_back(item, details);
                cond.notify_one();
            }
        }

        void wait() {
            std::unique_lock<std::mutex> lock(mutex);
            cond.wait(lock, [this]() {
                return !resolves.empty();
            });
        }

        void removeResolve(const ResolveItem& /*item*/) override {
        }
    };

} // namespace

Y_UNIT_TEST_SUITE(TestGlagold) {

    Y_UNIT_TEST_F(mdnsResponder, TelemetryTestFixture) {
        if (GetTestParam("runMdnsResponder", "") != std::string("true")) {
            YIO_LOG_INFO("test skipped by default, turn on by adding --test-param runMdnsResponder=true");
            return;
        }
        std::random_device rd;
        const std::string randomPrefix = "testGlagold_" + randomLowercaseLatinString(rd) + "_";
        TestResolveHandler resHndl(randomPrefix + getDeviceForTests()->deviceId());

        quasar::MDNSResponder::Settings settings;
        settings.externalPort = 1234;
        settings.hostnamePrefix = randomPrefix;
        settings.avahi.restrictToIPv4 = true;
        settings.avahi.flags.stereopair = true;

        quasar::MDNSResponder responder(getDeviceForTests(),
                                        settings,
                                        &resHndl);

        resHndl.wait();
        std::lock_guard<std::mutex> lock(resHndl.mutex);
        if (resHndl.resolves.size() > 1) {
            std::set<std::string> addresses;
            for (const auto& [item, details] : resHndl.resolves) {
                addresses.insert(details.address);
                auto spIter = details.txt.find("sp");
                UNIT_ASSERT(spIter != details.txt.end());
                UNIT_ASSERT_EQUAL(spIter->second, "follower");
            }
            UNIT_ASSERT_VALUES_EQUAL(resHndl.resolves.size(), addresses.size());
        };
    }

    Y_UNIT_TEST(unpackQuasarSystemConfig)
    {
        auto jsonConfig = parseJson(
            "{\"glagolHeartbeat\":true,"
            "\"nsdInsteadOfAvahi\":true,"
            "\"restrictAvahiToIPv4\":true,"
            "\"glagold\":{"
            "\"peersExchange\":true,"
            "\"minimumPeersExchangeInterval\":123,"
            "\"peerCleanupPeriod\":456,"
            "\"peerKpaTimeout\":789,"
            "\"avahiRatelimitBurst\":1234,"
            "\"avahiRatelimitIntervalUsec\":5678"
            "}}");
        auto settings = Glagol::fromSystemConfig(jsonConfig);
        UNIT_ASSERT(settings.heartbeatTelemetry);
        UNIT_ASSERT(settings.nsdInsteadOfAvahi);
        UNIT_ASSERT(settings.cluster.peersExchange);
        UNIT_ASSERT(settings.cluster.minimumPeersExchangeInterval == std::chrono::seconds(123));
        UNIT_ASSERT(settings.cluster.peerCleanupPeriod == std::chrono::seconds(456));
        UNIT_ASSERT(settings.cluster.peerKpaTimeout == std::chrono::seconds(789));
        UNIT_ASSERT(settings.avahi.restrictToIPv4);
        UNIT_ASSERT_EQUAL(settings.avahi.ratelimitBurst, 1234);
        UNIT_ASSERT_EQUAL(settings.avahi.ratelimitIntervalUsec, 5678);
    }

    Y_UNIT_TEST_F(testGlagolPing, GlagolInit)
    {
        Json::Value request;
        request["payload"]["command"] = "ping";

        oneshot(request); // checks that ping returns success
    }

    Y_UNIT_TEST_F(testGlagolWsServer, ServicesInit)
    {
        /* Create variables that will be captured by server */
        // for check 1
        std::mutex mutex1;
        std::atomic_int receivedCnt{0};
        std::promise<int> cntPromise1;

        // for check 2
        std::mutex mutex2;
        bool received2 = false;
        std::promise<int> cntPromise2;

        // for check 3
        std::mutex mutex3;
        bool received3 = false;
        std::promise<int> cntPromise3;

        // check 1 -- connect to WS and observe a single entry in GlagoldState
        GlagolWsServer server(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockDeviceStateProvider);
        server.updateAccountDevices(accountDevices);
        server.setOnMessage([](WebsocketServer::ConnectionHdl /*hdl*/, GlagolWsServer::MsgConnInfo /*info*/, const Json::Value& /* request */) {
            YIO_LOG_INFO("on message");
        });
        server.setOnConnectionsChanged([&](GlagoldState state) {
            YIO_LOG_INFO("1; connected");
            std::unique_lock<std::mutex> lock(mutex1);
            ++receivedCnt;
            cntPromise1.set_value(state.connections().size());
        });
        server.start();
        std::mutex clientConnectionMutex;
        std::atomic_bool clientHasConnection{false};
        SteadyConditionVariable clientConnectionCondVar;
        WebsocketClient::DisposableClient::Settings settings;
        settings.tls.crtBuffer = TestKeys().PUBLIC_PEM;
        settings.tls.verifyHostname = false;
        const auto serverUrl = "wss://localhost:" + std::to_string(server.getPort()) + "/";
        auto client = WebsocketClient::DisposableClient::create(serverUrl, settings, std::make_shared<NullMetrica>());
        YIO_LOG_INFO("Connecting to " << serverUrl);
        client->onConnect = [&](std::weak_ptr<WebsocketClient::DisposableClient> /* wthis */) {
            YIO_LOG_INFO("Connected");
            std::lock_guard<std::mutex> guard(clientConnectionMutex);
            clientHasConnection = true;
            clientConnectionCondVar.notify_one();
        };

        client->onDisconnect = [&](std::weak_ptr<WebsocketClient::DisposableClient> /*client*/, const Websocket::ConnectionInfo& /*info*/) {
            YIO_LOG_INFO("Disconnected");
            std::lock_guard<std::mutex> guard(clientConnectionMutex);
            clientHasConnection = false;
            clientConnectionCondVar.notify_one();
        };

        client->connectAsyncWithTimeout();

        {
            /* Make sure that client connected to server */
            std::unique_lock<std::mutex> lock(clientConnectionMutex);
            clientConnectionCondVar.wait(lock, [&]() {
                return clientHasConnection.load();
            });
        }
        client->send("{\"conversationToken\": \"" + jwtToken + "\"}");
        UNIT_ASSERT_VALUES_EQUAL(1, cntPromise1.get_future().get());
        UNIT_ASSERT_VALUES_EQUAL(receivedCnt.load(), 1);
        YIO_LOG_INFO("1st check done");

        // check 2 -- restart via startupMessage and observe 0 connections in notification (probably?)
        server.setOnConnectionsChanged([&](GlagoldState state) {
            YIO_LOG_INFO("2; lost ");
            std::unique_lock<std::mutex> lock(mutex2);
            if (!received2) {
                cntPromise2.set_value(state.connections().size());
            }
            received2 = true;
        });

        mockAuthProvider->setOwner(
            AuthInfo2{
                .source = AuthInfo2::Source::AUTHD,
                .authToken = "test_token1",
                .passportUid = "1234",
                .tag = 1600000000,
            });

        server.updateAccountDevices({});
        UNIT_ASSERT_VALUES_EQUAL(0, cntPromise2.get_future().get());

        YIO_LOG_INFO("Wait client to disconnect ");
        {
            std::unique_lock<std::mutex> lock(clientConnectionMutex);
            clientConnectionCondVar.wait(lock, [&]() {
                return !clientHasConnection.load();
            });
        }

        server.updateAccountDevices(accountDevices);
        YIO_LOG_INFO("2nd check done");

        // check 3 -- connect again and observer 1 connection exactly
        server.setOnConnectionsChanged([&](GlagoldState state) {
            YIO_LOG_INFO("3; connected");
            std::unique_lock<std::mutex> lock(mutex3);
            if (!received3) {
                cntPromise3.set_value(state.connections().size());
            }
            received3 = true;
        });

        // server need a time to restart, so we will try to connect again if failed
        client->onFailure = [](std::weak_ptr<WebsocketClient::DisposableClient> wclient, const Websocket::ConnectionInfo& /*info*/) {
            YIO_LOG_INFO("Seems server is restarting, give another chance to connect");
            if (auto client = wclient.lock()) {
                std::this_thread::sleep_for(std::chrono::seconds(1));
                client->connectAsyncWithTimeout();
            }
        };

        client->connectAsyncWithTimeout();
        {
            /* Make sure that client connected to server */
            YIO_LOG_INFO("Wait client to connect again ");
            std::unique_lock<std::mutex> lock(clientConnectionMutex);
            clientConnectionCondVar.wait(lock, [&]() {
                return clientHasConnection.load();
            });
        }

        client->send("{\"conversationToken\": \"" + jwtToken + "\"}");
        UNIT_ASSERT_VALUES_EQUAL(1, cntPromise3.get_future().get());
        YIO_LOG_INFO("3rd check done");
    }

    Y_UNIT_TEST_F(testGlagolWsServerTokenExpiration, ServicesInit)
    {
        jwtToken = genMyToken(time(nullptr) - 10);

        GlagolWsServer server(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockDeviceStateProvider);
        server.updateAccountDevices(accountDevices);
        server.setOnMessage([](WebsocketServer::ConnectionHdl /*hdl*/, GlagolWsServer::MsgConnInfo /*info*/, const Json::Value& /* request */) {
            YIO_LOG_INFO("on message");
        });
        server.start();

        std::promise<void> connectedPromise;
        std::promise<std::string> closedPromise;
        WebsocketClient::DisposableClient::Settings settings;
        settings.tls.crtBuffer = TestKeys().PUBLIC_PEM;
        settings.tls.verifyHostname = false;
        auto client = WebsocketClient::DisposableClient::create("wss://localhost:" + std::to_string(server.getPort()) + "/", settings, std::make_shared<NullMetrica>());
        client->onConnect = [&](std::weak_ptr<WebsocketClient::DisposableClient> /* wthis */) {
            connectedPromise.set_value();
        };
        client->onDisconnect = [&](std::weak_ptr<WebsocketClient::DisposableClient> /*client*/, const Websocket::ConnectionInfo& connectionInfo) {
            YIO_LOG_INFO(connectionInfo.toString());
            closedPromise.set_value(connectionInfo.toString());
        };
        client->connectAsyncWithTimeout();
        /* Wait until Client connects to server */
        connectedPromise.get_future().get();
        YIO_LOG_INFO("Client connected");

        /* Send message with expired token */
        client->send("{\"conversationToken\": \"" + jwtToken + "\"}");
        YIO_LOG_INFO("Message with expired token is sent. Wait for client disconnect");
        closedPromise.get_future().get();
    }

    Y_UNIT_TEST_F(testGlagolWsServerTokenWrongDevice, ServicesInit)
    {
        jwtToken = genToken("lol", "kek", time(nullptr) + 60);
        GlagolWsServer server(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockDeviceStateProvider);
        server.updateAccountDevices(accountDevices);
        server.setOnMessage([](WebsocketServer::ConnectionHdl /*hdl*/, GlagolWsServer::MsgConnInfo /*info*/, const Json::Value& /* request */) {
            YIO_LOG_INFO("on message");
        });
        server.start();

        WebsocketClient::DisposableClient::Settings settings;
        settings.tls.crtBuffer = TestKeys().PUBLIC_PEM;
        settings.tls.verifyHostname = false;
        std::promise<std::string> closed;
        std::promise<void> connectedPromise;
        auto client = WebsocketClient::DisposableClient::create("wss://localhost:" + std::to_string(server.getPort()) + "/", settings, std::make_shared<NullMetrica>());
        client->onConnect = [&](std::weak_ptr<WebsocketClient::DisposableClient> /*client*/) {
            connectedPromise.set_value();
        };
        client->onDisconnect = [&](std::weak_ptr<WebsocketClient::DisposableClient> /*client*/, const Websocket::ConnectionInfo& connectionInfo) {
            YIO_LOG_INFO(connectionInfo.toString());
            closed.set_value(connectionInfo.toString());
        };
        client->connectAsyncWithTimeout();
        /* Wait until Client connects to server */
        connectedPromise.get_future().get();
        YIO_LOG_INFO("Client connected");
        client->send("{\"conversationToken\": \"" + jwtToken + "\"}");
        YIO_LOG_INFO("Message sent with wrong device id in jwt token is sent");
        closed.get_future().get();
    }

    Y_UNIT_TEST_F(testControlRequestIsSentToAliced, GlagolInit)
    {
        std::promise<QuasarMessage> toAlice;

        setAlicedMessageHandler([&](const auto& quasarMessage, auto& /*connection*/) {
            toAlice.set_value(*quasarMessage);
        });

        Json::Value request;
        request["payload"] = parseJson("{\"command\": \"control\", \"action\": \"go_right\"}");
        oneshot(request);

        QuasarMessage controlMessage = toAlice.get_future().get();

        UNIT_ASSERT(controlMessage.has_control_request());

        UNIT_ASSERT(controlMessage.control_request().has_navigation_request());
        UNIT_ASSERT(controlMessage.control_request().navigation_request().has_go_right());

        UNIT_ASSERT(controlMessage.control_request().has_origin());
        UNIT_ASSERT_EQUAL(controlMessage.control_request().origin(), ControlRequest_Origin_TOUCH);
    }

    Y_UNIT_TEST_F(testControlRequestModeIsCorrect, GlagolInit)
    {
        std::promise<QuasarMessage> toAlice;

        setAlicedMessageHandler([&](const auto& quasarMessage, auto& /*connection*/) {
            toAlice.set_value(*quasarMessage);
        });

        Json::Value request;
        request["id"] = "some_uuid";
        request["payload"] = parseJson("{\"command\": \"control\", \"action\": \"go_left\", \"mode\": \"native\"}");
        oneshot(request);

        QuasarMessage controlMessage = toAlice.get_future().get();

        UNIT_ASSERT(controlMessage.has_control_request());

        UNIT_ASSERT(controlMessage.control_request().has_navigation_request());
        UNIT_ASSERT(controlMessage.control_request().navigation_request().has_go_left());
        UNIT_ASSERT(controlMessage.control_request().navigation_request().go_left().has_native_mode());
        UNIT_ASSERT(controlMessage.control_request().has_vins_request_id());
        UNIT_ASSERT_VALUES_EQUAL(request["id"].asString(), controlMessage.control_request().vins_request_id());
    }

    Y_UNIT_TEST_F(testActionRequestWithModeSet, GlagolInit)
    {
        std::promise<QuasarMessage> toAlice;

        setAlicedMessageHandler([&](const auto& quasarMessage, auto& /*connection*/) {
            toAlice.set_value(*quasarMessage);
        });

        Json::Value request;
        request["payload"] = parseJson("{\"command\": \"control\", \"action\": \"click_action\", \"mode\": \"native\"}");
        oneshot(request);

        QuasarMessage controlMessage = toAlice.get_future().get();

        UNIT_ASSERT(controlMessage.has_control_request());

        UNIT_ASSERT(controlMessage.control_request().has_action_request());
    }

    Y_UNIT_TEST_F(playerContinueExternalCommandHasRequestId, GlagolInit)
    {
        std::promise<QuasarMessage> toAlice;

        setAlicedMessageHandler([&](const auto& quasarMessage, auto& /*connection*/) {
            toAlice.set_value(*quasarMessage);
        });

        Json::Value request;
        request["id"] = "123";
        request["payload"] = parseJson("{\"command\": \"play\"}");
        oneshot(request);

        QuasarMessage message = toAlice.get_future().get();

        UNIT_ASSERT(message.has_directive());
        const auto& directive = message.directive();

        YIO_LOG_INFO(directive.request_id());
        UNIT_ASSERT_EQUAL(directive.name(), Directives::PLAYBACK_PLAY);
        UNIT_ASSERT_EQUAL(directive.request_id(), "123");
    }

    // TODO: refactor tests in order to be able to send json messages
    //      into glagol and check conversion into QuasarMessages
    Y_UNIT_TEST_F(testGlagolJsonToControlRequest, QuasarUnitTestFixture)
    {
        QuasarMessage msg;
        // cover all actions
        std::string vinsRequestId = "uuid";
        Json::Value payload = parseJson("{\"action\": \"go_right\"}");
        UNIT_ASSERT(Glagol::buildControlRequest(payload, msg, vinsRequestId));
        UNIT_ASSERT(msg.control_request().navigation_request().has_go_right());
        UNIT_ASSERT_EQUAL(msg.control_request().origin(), ControlRequest_Origin_TOUCH);
        msg.clear_control_request();

        payload = parseJson("{\"action\": \"go_left\"}");
        UNIT_ASSERT(Glagol::buildControlRequest(payload, msg, vinsRequestId));
        UNIT_ASSERT(msg.control_request().navigation_request().has_go_left());
        UNIT_ASSERT_EQUAL(msg.control_request().origin(), ControlRequest_Origin_TOUCH);
        msg.clear_control_request();

        payload = parseJson("{\"action\": \"go_up\"}");
        UNIT_ASSERT(Glagol::buildControlRequest(payload, msg, vinsRequestId));
        UNIT_ASSERT(msg.control_request().navigation_request().has_go_up());
        UNIT_ASSERT_EQUAL(msg.control_request().origin(), ControlRequest_Origin_TOUCH);
        msg.clear_control_request();

        payload = parseJson("{\"action\": \"go_down\"}");
        UNIT_ASSERT(Glagol::buildControlRequest(payload, msg, vinsRequestId));
        UNIT_ASSERT(msg.control_request().navigation_request().has_go_down());
        UNIT_ASSERT_EQUAL(msg.control_request().origin(), ControlRequest_Origin_TOUCH);
        msg.clear_control_request();

        payload = parseJson("{\"action\": \"click_action\"}");
        UNIT_ASSERT(Glagol::buildControlRequest(payload, msg, vinsRequestId));
        UNIT_ASSERT(msg.control_request().has_action_request());
        UNIT_ASSERT_EQUAL(msg.control_request().origin(), ControlRequest_Origin_TOUCH);
        msg.clear_control_request();

        // cover wrong actions
        payload = parseJson("{\"action\": \"bla bla bla\"}");
        UNIT_ASSERT_VALUES_EQUAL(false, Glagol::buildControlRequest(payload, msg, vinsRequestId));
        UNIT_ASSERT_VALUES_EQUAL(false, msg.has_control_request());
        msg.clear_control_request();

        payload = parseJson("{\"not an action\": \"bla bla bla\"}");
        UNIT_ASSERT_VALUES_EQUAL(false, Glagol::buildControlRequest(payload, msg, vinsRequestId));
        UNIT_ASSERT_VALUES_EQUAL(false, msg.has_control_request());
        msg.clear_control_request();

        // cover scroll amount
        payload = parseJson("{\"not an action\": \"bla bla bla\", \"scrollAmount\":\"till_end\"}"); // will not be handled without proper action
        UNIT_ASSERT_VALUES_EQUAL(false, Glagol::buildControlRequest(payload, msg, vinsRequestId));
        UNIT_ASSERT_VALUES_EQUAL(false, msg.has_control_request());
        msg.clear_control_request();

        payload = parseJson("{\"action\": \"go_down\", \"scrollAmount\":\"till_end\"}");
        UNIT_ASSERT(Glagol::buildControlRequest(payload, msg, vinsRequestId));
        UNIT_ASSERT(msg.control_request().navigation_request().has_go_down());
        UNIT_ASSERT(msg.control_request().navigation_request().go_down().has_scroll_amount());
        UNIT_ASSERT(msg.control_request().navigation_request().go_down().scroll_amount().has_till_end());
        UNIT_ASSERT_EQUAL(msg.control_request().origin(), ControlRequest_Origin_TOUCH);
        msg.clear_control_request();

        payload = parseJson("{\"action\": \"go_down\", \"scrollAmount\":\"few\"}");
        UNIT_ASSERT(Glagol::buildControlRequest(payload, msg, vinsRequestId));
        UNIT_ASSERT(msg.control_request().navigation_request().has_go_down());
        UNIT_ASSERT(msg.control_request().navigation_request().go_down().has_scroll_amount());
        UNIT_ASSERT(msg.control_request().navigation_request().go_down().scroll_amount().has_few());
        UNIT_ASSERT_EQUAL(msg.control_request().origin(), ControlRequest_Origin_TOUCH);
        msg.clear_control_request();

        payload = parseJson("{\"action\": \"go_down\", \"scrollAmount\":\"many\"}");
        UNIT_ASSERT(Glagol::buildControlRequest(payload, msg, vinsRequestId));
        UNIT_ASSERT(msg.control_request().navigation_request().has_go_down());
        UNIT_ASSERT(msg.control_request().navigation_request().go_down().has_scroll_amount());
        UNIT_ASSERT(msg.control_request().navigation_request().go_down().scroll_amount().has_many());
        UNIT_ASSERT_EQUAL(msg.control_request().origin(), ControlRequest_Origin_TOUCH);
        msg.clear_control_request();

        // exact scroll amount should be handled only with exact value provided
        payload = parseJson("{\"action\": \"go_down\", \"scrollAmount\":\"exact\"}");
        UNIT_ASSERT_VALUES_EQUAL(false, Glagol::buildControlRequest(payload, msg, vinsRequestId));
        msg.clear_control_request();

        // only integer exact value will be accepted
        payload = parseJson("{\"action\": \"go_down\", \"scrollAmount\":\"exact\", \"scrollExactValue\":3}");
        UNIT_ASSERT(Glagol::buildControlRequest(payload, msg, vinsRequestId));
        UNIT_ASSERT(msg.control_request().navigation_request().has_go_down());
        UNIT_ASSERT(msg.control_request().navigation_request().go_down().has_scroll_amount());
        UNIT_ASSERT(msg.control_request().navigation_request().go_down().scroll_amount().has_exact());
        UNIT_ASSERT_VALUES_EQUAL(3, msg.control_request().navigation_request().go_down().scroll_amount().exact().value());
        UNIT_ASSERT_EQUAL(msg.control_request().origin(), ControlRequest_Origin_TOUCH);
        msg.clear_control_request();

        // wrong type of exact value will be discarded
        payload = parseJson("{\"action\": \"go_down\", \"scrollAmount\":\"exact\", \"scrollExactValue\":\"bla bla bla\"}");
        UNIT_ASSERT_VALUES_EQUAL(false, Glagol::buildControlRequest(payload, msg, vinsRequestId));
    }

    Y_UNIT_TEST_F(testGlagolWsServerApplyNewCertificatesAfterInitMode, ServicesInit)
    {
        auto device = getDeviceForTests();
        GlagolWsServer server(device, ipcFactoryForTests(), mockAuthProvider, mockDeviceStateProvider);
        server.updateAccountDevices(accountDevices);
        server.setOnMessage([](WebsocketServer::ConnectionHdl /*hdl*/, GlagolWsServer::MsgConnInfo /*info*/, const Json::Value& /* request */) {
            YIO_LOG_INFO("on message");
        });
        server.start();

        server.waitServerStart();
        {
            // check client connect with first certificate
            WebsocketClient::DisposableClient::Settings settings;
            settings.tls.crtBuffer = TestKeys().PUBLIC_PEM;
            settings.tls.verifyHostname = false;
            const auto serverUrl = "wss://localhost:" + std::to_string(server.getPort()) + "/";
            auto client = WebsocketClient::DisposableClient::create(serverUrl, settings, std::make_shared<NullMetrica>());
            YIO_LOG_INFO("Connecting to " << serverUrl);
            std::promise<void> connectedPromise;
            client->onConnect = [&](std::weak_ptr<WebsocketClient::DisposableClient> /* wthis */) {
                YIO_LOG_INFO("Successefully connected with first certificate");
                connectedPromise.set_value();
            };

            std::promise<void> disconnectPromise;
            client->onDisconnect = [&](std::weak_ptr<WebsocketClient::DisposableClient> /*client*/, const Websocket::ConnectionInfo& /*info*/) {
                YIO_LOG_INFO("Disconnected.");
                disconnectPromise.set_value();
            };

            client->connectAsyncWithTimeout();
            connectedPromise.get_future().get();

            YIO_LOG_INFO("GlagolWSServer started. Go to configuring mode");

            glagol::BackendApi::DevicesMap result;
            glagol::DeviceId id{device->deviceId(), "yandexstation"};
            auto& dev = result[id];
            dev.glagol.security.serverCertificate = TestKeys2().PUBLIC_PEM;
            dev.glagol.security.serverPrivateKey = TestKeys2().PRIVATE_PEM;
            dev.name = "quasar";
            server.updateAccountDevices(result);
            YIO_LOG_INFO("Certificate changed.");

            disconnectPromise.get_future().get();
            YIO_LOG_INFO("GlagolWSServer seems to be stopped.");
        }

        server.waitServerStart();
        YIO_LOG_INFO("Server started. Checking connection");

        {
            // check client connect with first certificate
            WebsocketClient::DisposableClient::Settings settings;
            settings.tls.crtBuffer = TestKeys2().PUBLIC_PEM;
            settings.tls.verifyHostname = false;
            const auto serverUrl = "wss://localhost:" + std::to_string(server.getPort()) + "/";
            auto client = WebsocketClient::DisposableClient::create(serverUrl, settings, std::make_shared<NullMetrica>());
            YIO_LOG_INFO("Connecting to " << serverUrl);
            std::promise<void> connectedPromise;
            client->onConnect = [&](std::weak_ptr<WebsocketClient::DisposableClient> /* wthis */) {
                YIO_LOG_INFO("Connected");
                connectedPromise.set_value();
            };

            client->connectAsyncWithTimeout();
            connectedPromise.get_future().get();
            YIO_LOG_INFO("Successfully connected with new certificate. All OK.");
        }
        // check client connect with second certificate
        UNIT_ASSERT(true);
    }

    Y_UNIT_TEST_F(testHeartbeatTelemetry, GlagolInit)
    {
        std::mutex mutex;
        std::condition_variable cond;
        std::optional<Json::Value> eventValue;
        unsigned heartbeatsCount = 0;
        setEventListener([&](const std::string& name, const std::string& value, YandexIO::ITelemetry::Flags /*flags*/) {
            if (name == "glagold_heartbeat") {
                ++heartbeatsCount;
                std::lock_guard<std::mutex> lock(mutex);
                eventValue = parseJson(value);
                cond.notify_one();
            }
        });

        auto waitHeartbeat = [&]() -> Json::Value {
            std::unique_lock<std::mutex> lock(mutex);
            cond.wait(lock, [&]() { return eventValue.has_value(); });
            Json::Value result = std::move(*eventValue);
            eventValue.reset();
            return result;
        };

        glagol->waitWsServerStart();

        const std::string CLIENT_DEVICE_ID = "TEST2";
        {
            auto moreDevices = accountDevices;
            glagol::DeviceId moreId = {CLIENT_DEVICE_ID, "yandexmini"};
            auto& dev = moreDevices[moreId];
            dev.glagol.security.serverCertificate = TestKeys().PUBLIC_PEM;
            dev.glagol.security.serverPrivateKey = TestKeys().PRIVATE_PEM;
            dev.name = "quasar";
            mockSyncd->sendToAll(makeAccountDevicesListMessage(moreDevices));
        }

        WebsocketClient client(std::make_shared<NullMetrica>());
        WebsocketClient::Settings clientSettings;
        clientSettings.tls.crtBuffer = TestKeys().PUBLIC_PEM;
        clientSettings.tls.verifyHostname = false;
        clientSettings.reconnect.enabled = false;
        clientSettings.ping.enabled = true;

        clientSettings.url = "wss://localhost:" + std::to_string(getFixtureExternalPort()) + "/";

        std::promise<void> clientPromise;
        client.setOnConnectHandler([&clientPromise]() {
            clientPromise.set_value();
        });

        std::promise<std::string> msgPromise;
        client.setOnMessageHandler([&msgPromise](const std::string& msg) {
            msgPromise.set_value(msg);
        });
        std::promise<void> disconnectPromise;
        client.setOnDisconnectHandler([&disconnectPromise](auto /*info*/) {
            disconnectPromise.set_value();
        });

        client.connectAsync(clientSettings);

        clientPromise.get_future().get();

        client.unsafeSend(makeClusterHelo(CLIENT_DEVICE_ID)); // mimic to cluster

        YIO_LOG_INFO(msgPromise.get_future().get());

        glagol->forceHeartbeatTelemetry();
        {
            auto connEvent = waitHeartbeat();
            YIO_LOG_INFO(connEvent);
            UNIT_ASSERT(connEvent.isMember(CLIENT_DEVICE_ID));
            UNIT_ASSERT(connEvent[CLIENT_DEVICE_ID].isMember("cluster_host"));
            UNIT_ASSERT(connEvent["stats"]["websocket_is_active"].asBool());
        }

        mockDeviceStateProvider->setDeviceState({.configuration = DeviceState::Configuration::CONFIGURED, .update = DeviceState::Update::HAS_CRITICAL});
        disconnectPromise.get_future().get();
        glagol->forceHeartbeatTelemetry();
        {
            auto connEvent = waitHeartbeat();
            YIO_LOG_INFO(connEvent);
            UNIT_ASSERT(!connEvent.isMember(CLIENT_DEVICE_ID));
            UNIT_ASSERT(!connEvent["stats"]["websocket_is_active"].asBool());
            UNIT_ASSERT(!connEvent["stats"].isMember("wifi_ssid"));
            UNIT_ASSERT(!connEvent["stats"].isMember("wifi_mac"));
        }

        mockNetworkd->sendToAll(makeTestNetworkStatusMessage());
        while (true) {
            std::this_thread::sleep_for(std::chrono::seconds(1));
            glagol->forceHeartbeatTelemetry();
            YIO_LOG_INFO("Waiting heartbeat with ssid and mac");
            auto connEvent = waitHeartbeat();
            const auto& stats = connEvent["stats"];
            if (stats.isMember("wifi_ssid") && stats.isMember("wifi_mac")) {
                UNIT_ASSERT_VALUES_EQUAL(stats["wifi_ssid"].asString(), "TEST_WIFI_SSID");
                UNIT_ASSERT_VALUES_EQUAL(stats["wifi_mac"].asString(), "TEST_WIFI_MAC");
                break;
            }
        }
        mockStereoPairProvider->setStereoPairState({
            .role = StereoPairState::Role::FOLLOWER,
            .channel = StereoPairState::Channel::LEFT,
            .partnerDeviceId = "TEST_2",
            .connectivity = StereoPairState::Connectivity::TWOWAY,
        });
        while (true) {
            std::this_thread::sleep_for(std::chrono::seconds(1));
            glagol->forceHeartbeatTelemetry();
            YIO_LOG_INFO("Waiting heartbeat with stereopair");
            auto connEvent = waitHeartbeat();
            const auto& stats = connEvent["stats"];
            if (stats.isMember("stereopair")) {
                const auto& st = stats["stereopair"];
                UNIT_ASSERT_VALUES_EQUAL(st["role"].asString(), "FOLLOWER");
                UNIT_ASSERT_VALUES_EQUAL(st["channel"].asString(), "left");
                UNIT_ASSERT_VALUES_EQUAL(st["connectivity"].asString(), "TWOWAY");
                UNIT_ASSERT_VALUES_EQUAL(st["partnerId"].asString(), "TEST_2");
                break;
            }
        }
    }

    Y_UNIT_TEST_F(isKnownAccountDevice, ServicesInit)
    {
        auto deviceListRequest = []() {};
        GlagolCluster cluster(getDeviceForTests(),
                              ipcFactoryForTests(),
                              mockAuthProvider,
                              [](auto /*res*/) {},
                              deviceListRequest,
                              std::make_shared<YandexIO::NullEndpointStorage>());

        std::random_device rd;

        auto randomDeviceType = [&rd]() -> std::string {
            const char* devTypes[] = {"yandexmini", "yandexmicro", "yandexmidi", "yandexstation", "yandexstation_2", "jbl_link_protable", "jbl_link_music", "lightcom", "dexp"};
            std::uniform_int_distribution<int> dist(0, std::size(devTypes) - 1);
            return devTypes[dist(rd)];
        };

        const int devicesCount = 50;
        std::vector<std::string> ids;
        {
            ids.reserve(devicesCount);
            glagol::BackendApi::DevicesMap randomAccountDevices;
            for (int i = 0; i < devicesCount; ++i) {
                glagol::DeviceId id{randomLowercaseLatinString(rd), randomDeviceType()};
                ids.emplace_back(id.id);
                auto& dev = randomAccountDevices[id];
                dev.name = "quasar";
            };
            cluster.updateAccountDevices(randomAccountDevices);
        }
        std::random_shuffle(std::begin(ids), std::end(ids));
        std::promise<void> queuePromise;
        cluster.lifecycleExecute([&queuePromise]() {
            queuePromise.set_value();
        });
        queuePromise.get_future().get();
        UNIT_ASSERT_EQUAL(ids.size(), devicesCount);
        for (const auto& id : ids) {
            UNIT_ASSERT(cluster.isKnownAccountDevice(id));
        }

        for (const auto& id : ids) {
            UNIT_ASSERT(!cluster.isKnownAccountDevice(id + "_"));
        }
    }

    Y_UNIT_TEST_F(disoveryResultContent, ServicesInit)
    {
        std::promise<glagol::IDiscovery::Result> discoveriesPromise;

        GlagolCluster cluster(
            getDeviceForTests(),
            ipcFactoryForTests(),
            mockAuthProvider,
            [&](const glagol::IDiscovery::Result& result) {
                discoveriesPromise.set_value(result);
            },
            []() {},
            std::make_shared<YandexIO::NullEndpointStorage>());

        cluster.updateAccountDevices(accountDevices);
        YIO_LOG_INFO("Waiting for accounts processed");
        std::promise<void> lifecyclePush;
        cluster.lifecycleExecute([&lifecyclePush]() {
            lifecyclePush.set_value();
        });
        lifecyclePush.get_future().get();

        {
            glagol::ResolveItem::Info::StringMap txt;
            txt["cluster"] = "yes";
            txt["deviceId"] = accountDevices.begin()->first.id;
            txt["platform"] = accountDevices.begin()->first.platform;

            cluster.getResolveHandler()->newResolve({.name = "YandexIOReceiver-TEST", .type = "_yandexio._tcp", .domain = "local"},
                                                    {.hostname = "localhost", .port = 1234, .address = "1.2.3.4", .txt = txt});
        }

        auto discoveries = discoveriesPromise.get_future().get();

        UNIT_ASSERT_EQUAL(discoveries.items.size(), 1);

        for (const auto& [id, item] : discoveries.items) {
            UNIT_ASSERT(item.name.starts_with(RESOLVE_STRS::PREFIX));
            UNIT_ASSERT_EQUAL(item.accountDeviceName, accountDevices[id].name);
            UNIT_ASSERT_EQUAL(item.address, "1.2.3.4");
            UNIT_ASSERT_EQUAL(item.port, 1234);
            UNIT_ASSERT(item.isAccountDevice);
            UNIT_ASSERT(item.cluster);
        }
    }

    Y_UNIT_TEST_F(rerequestDevicesListOnTlsError, ServicesInit)
    {
        GlagolWsServer server(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockDeviceStateProvider);
        server.updateAccountDevices(accountDevices);

        server.start();
        const auto wsPort = server.getPort();

        YIO_LOG_INFO("glagolWsServer listening on " << wsPort);

        std::promise<void> requestPromise;
        auto deviceListRequest = [&requestPromise]() {
            YIO_LOG_INFO("reRequested");
            requestPromise.set_value();
        };
        std::mutex discoveriesMutex;
        std::condition_variable discoveriesCond;
        glagol::IDiscovery::Result discoveries;

        GlagolCluster cluster(getDeviceForTests(),
                              ipcFactoryForTests(),
                              mockAuthProvider,
                              [&](const glagol::IDiscovery::Result& result) {
                                  std::lock_guard<std::mutex> lock(discoveriesMutex);
                                  discoveries = result;
                                  discoveriesCond.notify_one();
                              },
                              deviceListRequest,
                              std::make_shared<YandexIO::NullEndpointStorage>());

        glagol::BackendApi::DevicesMap badAccountDevices;
        {
            glagol::DeviceId id{getDeviceForTests()->deviceId() + "_1", "yandexmini"};
            auto& dev = badAccountDevices[id];
            dev.glagol.security.serverCertificate = TestKeys().INCORRECT_PUBLIC_PEM;
            dev.glagol.security.serverPrivateKey = TestKeys().PRIVATE_PEM;
            dev.name = "quasar";
            cluster.updateAccountDevices(badAccountDevices);
        };

        auto makeMockInfo = [&badAccountDevices, wsPort]() {
            glagol::ResolveItem::Info::StringMap txt;
            txt["cluster"] = "yes";
            txt["deviceId"] = badAccountDevices.begin()->first.id;
            txt["platform"] = badAccountDevices.begin()->first.platform;
            return glagol::ResolveItem::Info{.hostname = "localhost", .port = wsPort, .address = "localhost", .txt = txt};
        };

        cluster.getResolveHandler()->newResolve({.name = "YandexIOReceiver-TEST", .type = "_yandexio._tcp", .domain = "local"},
                                                makeMockInfo());
        YIO_LOG_INFO("Waiting for discovery");
        {
            std::unique_lock<std::mutex> lock(discoveriesMutex);
            discoveriesCond.wait(lock, [&]() {
                return !discoveries.items.empty();
            });
        }

        YIO_LOG_INFO("Waiting for rerequest");
        requestPromise.get_future().get();
        /*
    cluster.getResolveHandler()->removeResolve({.name = "YandexIOReceiver-TEST", .type = "_yandexio._tcp", .domain = "local"});
    YIO_LOG_INFO("Waiting for empty discoveries");
    std::unique_lock<std::mutex> lock(discoveriesMutex);
    discoveriesCond.wait(lock, [&]() {
        return discoveries.items.empty();
    });
    */
        cluster.updateAccountDevices({});
    }

    Y_UNIT_TEST_F(rerequestDevicesListOnUnknownDeviceInCluster, GlagolInit)
    {
        std::promise<void> requestPromise;
        setSyncdMessageHandler([&](const auto& message, auto& /*connection*/) {
            if (message->has_rerequest_devices_list()) {
                requestPromise.set_value();
            }
        });
        glagol->waitWsServerStart();
        WebsocketClient client(std::make_shared<NullMetrica>());
        WebsocketClient::Settings clientSettings;
        clientSettings.tls.crtBuffer = TestKeys().PUBLIC_PEM;
        clientSettings.tls.verifyHostname = false;
        clientSettings.reconnect.enabled = false;
        clientSettings.ping.enabled = true;

        clientSettings.url = "wss://localhost:" + std::to_string(getFixtureExternalPort()) + "/";

        client.connectAsync(clientSettings);
        YIO_LOG_INFO("Wait for connecting");
        std::promise<void> clientPromise;
        client.setOnConnectHandler([&clientPromise]() {
            clientPromise.set_value();
        });
        clientPromise.get_future().get();

        YIO_LOG_INFO("Connected. Send cluster helo and wait for devices rerequest");
        client.unsafeSend(makeClusterHelo("AnotherTestId")); // mimic to cluster
        requestPromise.get_future().get();
    }

    Y_UNIT_TEST_F(nsdReconfigure, GlagolInitNsd) {
        bool reconfigured = false;
        std::mutex mutex;
        std::condition_variable cond;
        glagol->setOnReconfigure([&mutex, &cond, &reconfigured]() {
            YIO_LOG_INFO("OnReconfigured!");
            std::lock_guard<std::mutex> lock(mutex);
            reconfigured = true;
            cond.notify_one();
        });

        auto sendConfig = [this](bool nsdInsteadOfAvahi) {
            YIO_LOG_DEBUG("Sending config " << (nsdInsteadOfAvahi ? "true" : "faslse"));
            Json::Value systemConfig(Json::objectValue);
            Json::Value config;
            systemConfig["nsdInsteadOfAvahi"] = nsdInsteadOfAvahi;
            config["system_config"] = systemConfig;
            QuasarMessage msg;
            msg.mutable_user_config_update()->set_config(jsonToString(config));
            mockSyncd->sendToAll(QuasarMessage{msg});
        };

        nsdEnablePromise.get_future().get();
        nsdEnablePromise = std::promise<void>();
        std::unique_lock<std::mutex> lock(mutex);
        auto waitForReconfigure = [&lock, &reconfigured, &cond]() {
            YIO_LOG_DEBUG("Waiting for reconfiguration");
            auto rval = cond.wait_for(lock, std::chrono::seconds(20),
                                      [&reconfigured]() { return reconfigured; });
            UNIT_ASSERT_C(rval, "timeouted while waiting for reconfigure");
            YIO_LOG_INFO("Reconfigured");
            reconfigured = false;
        };

        sendConfig(false);
        while (nsdEnabled) { // thereotically we can catch first reconfigure with nsd enabled
            waitForReconfigure();
            YIO_LOG_DEBUG("nsdEnabled = " << nsdEnabled);
        }
        UNIT_ASSERT(!nsdEnabled);

        YIO_LOG_INFO("Reconfigured. Trying back.");
        sendConfig(true);
        waitForReconfigure();
        YIO_LOG_INFO("Wait for nsdEnablePromise.");
        lock.unlock();
        nsdEnablePromise.get_future().get();
        lock.lock();
        UNIT_ASSERT(nsdEnabled);

        YIO_LOG_INFO("Reconfigured. And trying back again.");
        sendConfig(false);
        waitForReconfigure();
        UNIT_ASSERT(!nsdEnabled);
    }

    Y_UNIT_TEST_F(setOverridedChannelLeft, GlagolInit)
    {
        Json::Value request;
        request["payload"]["command"] = "setOverridedChannel";
        request["payload"]["channel"] = "left";

        EXPECT_CALL(*mockStereoPairProvider, overrideChannel(_)).WillRepeatedly(Invoke([&](const quasar::StereoPairState::Channel& channel) {
            UNIT_ASSERT(channel == quasar::StereoPairState::Channel::LEFT);
        }));
        oneshot(request);
    }

    Y_UNIT_TEST_F(setOverridedChannelUndefined, GlagolInit)
    {
        Json::Value request;
        request["payload"]["command"] = "setOverridedChannel";

        EXPECT_CALL(*mockStereoPairProvider, overrideChannel(_)).WillRepeatedly(Invoke([&](const quasar::StereoPairState::Channel& channel) {
            UNIT_ASSERT(channel == quasar::StereoPairState::Channel::UNDEFINED);
        }));
        oneshot(request);
    }

    /*
    ... resolve cluster1(device) to cluster1
    ... resolve cluster2(device) to cluster1
    cluster1 --(share peers)-> server2
                               server2 --(onMessage)-> cluster2
                                                       cluster2 --(connect)-> server1
    ... removeResolve from cluster1 ...
    cluster1 --(share peers)-> server2
                               server2 --(onMessage)-> cluster2
                                                       cluster2 --(heartbeat)->
 */

    Y_UNIT_TEST_F(sharingDiscoveries, GlagolInit)
    {
        auto testDevice1 = std::make_shared<YandexIO::Device>(
            getDeviceForTests()->deviceId() + "_1",
            QuasarUnitTestFixture::makeTestConfiguration(),
            std::make_shared<NullMetrica>(),
            QuasarUnitTestFixture::makeTestHAL());

        YandexIO::Configuration::TestGuard testGuard1;
        Json::Value& config1 = testDevice1->configuration()->getMutableConfig(testGuard1);
        auto wsPort1Config = getPort();
        config1["glagold"]["externalPort"] = wsPort1Config;
        config1["common"]["deviceType"] = "yandexmini";
        config1["common"]["backendUrl"] = fixtureBackendUrl;

        YIO_LOG_INFO("Prepared second device " << testDevice1->deviceId() << " " << testDevice1->configuration()->getDeviceType());

        glagol::BackendApi::DevicesMap accountDevices1 = accountDevices;
        {
            glagol::DeviceId id{testDevice1->deviceId(), "yandexmini"};
            auto& dev = accountDevices1[id];
            dev.glagol.security.serverCertificate = TestKeys().PUBLIC_PEM;
            dev.glagol.security.serverPrivateKey = TestKeys().PRIVATE_PEM;
            dev.name = "quasar";
        };

        mockSyncd->sendToAll(makeAccountDevicesListMessage(accountDevices1));

        const auto wsPort2 = getFixtureExternalPort();

        YIO_LOG_INFO("server2 is listening on " << wsPort2);

        std::mutex disconnectMutex;
        std::condition_variable disconnectCond;
        bool disconnectedFromServer1 = false;

        { // block to shutdown cluster1 to disconnect it from cluster2

            auto mockDeviceStateProvider1 = std::make_shared<mock::DeviceStateProvider>();
            mockDeviceStateProvider1->setDeviceState(mock::defaultDeviceState());
            GlagolCluster cluster1(testDevice1,
                                   ipcFactoryForTests(),
                                   mockAuthProvider,
                                   [](auto /*res*/) {},
                                   nullptr,
                                   std::make_shared<YandexIO::NullEndpointStorage>());
            GlagolWsServer server1(testDevice1, ipcFactoryForTests(), mockAuthProvider, mockDeviceStateProvider1);

            int clusterHeloCount = 0;
            std::promise<void> clusterHeloPromise;
            std::promise<void> clusterHeloPromise2;
            server1.setOnMessage([&](WebsocketServer::ConnectionHdl /*hdl*/, GlagolWsServer::MsgConnInfo /*info*/, Json::Value request) {
                const Json::Value& payload = request["payload"];
                std::string command = payload["command"].asString();
                if (command == "clusterHelo" && getString(payload, "from_device_id") == getDeviceForTests()->deviceId()) {
                    YIO_LOG_INFO("Back connection happened (due to discovery share)");

                    if (clusterHeloCount++) {
                        clusterHeloPromise2.set_value();
                    } else {
                        clusterHeloPromise.set_value();
                    }
                }
            });

            server1.setOnConnectionsChanged([&](const auto glagoldState) {
                if (glagoldState.connections().size() == 0) {
                    YIO_LOG_INFO("All disconnected");
                    std::lock_guard<std::mutex> lock(disconnectMutex);
                    disconnectedFromServer1 = true;
                    disconnectCond.notify_one();
                }
            });

            server1.updateAccountDevices(accountDevices1);
            cluster1.updateAccountDevices(accountDevices1);

            server1.start();
            const auto wsPort1 = server1.getPort();

            YIO_LOG_INFO("server1 is listening on " << wsPort1);

            /* resolve self and server2 to cluster1 */

            auto makeMockInfo = [](const glagol::DeviceId& deviceId, int port) {
                glagol::ResolveItem::Info::StringMap txt;
                txt["cluster"] = "yes";
                txt["deviceId"] = deviceId.id;
                txt["platform"] = deviceId.platform;
                return glagol::ResolveItem::Info{.hostname = "localhost", .port = port, .address = "localhost", .txt = txt};
            };

            for (const auto& [deviceId, info] : accountDevices1) {
                cluster1.getResolveHandler()->newResolve({.name = "YandexIOReceiver-" + deviceId.id + "." + deviceId.platform, .type = "_yandexio._tcp", .domain = "local"},
                                                         makeMockInfo(deviceId, testDevice1->deviceId() == deviceId.id ? wsPort1 : wsPort2));
            }

            // sharing discoveries is turned off by default, turning it on
            {
                auto clusterSettings = makeClusterSettings();
                clusterSettings.peersExchange = true;
                clusterSettings.minimumPeersExchangeInterval = std::chrono::seconds(5);
                cluster1.updateSettings(clusterSettings);
            }

            auto updatePeersExchangeSetttings = [this](bool value) {
                Json::Value config;
                Json::Value glagoldConfig(Json::objectValue);
                glagoldConfig["peersExchange"] = value;
                glagoldConfig["minimumPeersExchangeInterval"] = 5;
                glagoldConfig["peerKpaTimeout"] = 4;
                auto& systemConfig = config["system_config"];
                systemConfig["nsdInsteadOfAvahi"] = true;
                systemConfig["glagolHeartbeat"] = true;
                systemConfig["glagold"] = glagoldConfig;
                YIO_LOG_INFO("Update settings for cluster2 to " << config);
                QuasarMessage msg;
                msg.mutable_user_config_update()->set_config(jsonToString(config));
                mockSyncd->sendToAll(std::move(msg));
            };

            updatePeersExchangeSetttings(true);

            YIO_LOG_INFO("Waiting for discovery share and next connecting from cluster2 to cluster1");
            clusterHeloPromise.get_future().get();

            YIO_LOG_INFO("Disable peers exchange on cluster2");
            updatePeersExchangeSetttings(false);

            mockDeviceStateProvider1->setDeviceState({.configuration = DeviceState::Configuration::CONFIGURED, .update = DeviceState::Update::HAS_CRITICAL});
            YIO_LOG_INFO("Wait for server1 down");
            {
                std::unique_lock<std::mutex> lock(disconnectMutex);
                disconnectCond.wait(lock, [&]() { return disconnectedFromServer1; });
            }
            YIO_LOG_INFO("Clients disconnected");
            server1.setOnClose(nullptr);
            mockDeviceStateProvider1->setDeviceState(mock::defaultDeviceState());
            YIO_LOG_INFO("Wait for server1 up");
            server1.getPort();

            YIO_LOG_INFO("Enable peers exchange on cluster2 again");
            updatePeersExchangeSetttings(true);

            YIO_LOG_INFO("Waiting for conenct back again");

            clusterHeloPromise2.get_future().get();
        }
        /*
    YIO_LOG_INFO("cluster1 disappeared. Wait for lost discoveries in cluster2");

    std::mutex mutex;
    std::condition_variable cond;
    bool sharesDisappears = false;
    setEventListener([&](const std::string &name, const std::string &value, YandexIO::ITelemetry::Flags){
        if (name == "glagold_heartbeat") {
            auto jsonValue = parseJson(value);
            if (!jsonValue.isMember(testDevice1->deviceId())) {
                std::lock_guard<std::mutex> lock(mutex);
                sharesDisappears = true;
                cond.notify_one();
            }
        }
    });
    std::unique_lock<std::mutex> lock(mutex);
    while(!sharesDisappears) {
        glagol->forceHeartbeatTelemetry();
        cond.wait_for(lock, std::chrono::seconds(2),
                      [&sharesDisappears]{ return sharesDisappears; });
    }
    */
    }

    Y_UNIT_TEST_F(discoveredItems, GlagolInit)
    {
        auto testBackedApi = makeTestBackendApi();
        const std::string ID_PREFIX = getDeviceForTests()->deviceId();
        auto dItems = glagol::createDiscoveredItems({.id = ID_PREFIX + "_2", .platform = "yandexstation_2"},
                                                    getDeviceForTests()->telemetry(),
                                                    testBackedApi,
                                                    nullptr,
                                                    [testBackedApi]() {
                                                        return std::make_shared<glagol::ext::Connector>(testBackedApi,
                                                                                                        std::make_shared<NullMetrica>());
                                                    });
        dItems->setSettings({.connectingPoolSize = 1});

        std::promise<void> connected[2];
        std::list<glagol::IDiscoveredItems::ConnectorPtr> connectors;
        dItems->setOnDiscoveryCallback([&ID_PREFIX, &connected, &connectors](const glagol::DeviceId& id, glagol::IDiscoveredItems::ConnectorPtr connector) {
            if (id.id == ID_PREFIX) {
                connected[0].set_value();
            } else if (id.id == ID_PREFIX + "_1") {
                connected[1].set_value();
                connectors.push_back(connector); // need to store this to lock proper resolve
            }
        });

        const char* platforms[] = {"yandexstation", "yandexmini", "yandexstation_2"};
        glagol::BackendApi::DevicesMap accountDevices1 = accountDevices;
        {
            for (int i = 1; i < 3; ++i) {
                glagol::DeviceId moreId = {
                    .id = ID_PREFIX + "_" + std::to_string(i),
                    .platform = platforms[i],
                };
                auto& dev = accountDevices1[moreId];
                dev.name = "quasar";
                dev.glagol.security.serverCertificate = TestKeys().PUBLIC_PEM;
                dev.glagol.security.serverPrivateKey = TestKeys().PRIVATE_PEM;
            };
            dItems->updateAccountDevices(accountDevices1);
        };
        const auto wsPort0 = getFixtureExternalPort();

        YIO_LOG_INFO("server0 is listening on " << wsPort0);

        glagol::ResolveItem::Info::StringMap txt;
        txt["deviceId"] = ID_PREFIX + "_1";
        txt["platform"] = platforms[1];
        txt["cluster"] = "yes";
        const glagol::ResolveItem resolveItem1{RESOLVE_STRS::PREFIX + ID_PREFIX + "_1", RESOLVE_STRS::TYPE, "local"};
        dItems->newResolve(resolveItem1,
                           glagol::ResolveItem::Info{.hostname = "localhost", .port = 1234, .address = "localhost", .txt = txt});

        txt["deviceId"] = ID_PREFIX;
        txt["platform"] = platforms[0];
        const glagol::ResolveItem resolveItem0 = {RESOLVE_STRS::PREFIX + ID_PREFIX, RESOLVE_STRS::TYPE, "local"};
        const glagol::ResolveItem::Info resolveInfo0 = {.hostname = "localhost", .port = wsPort0, .address = "localhost", .txt = txt};
        dItems->newResolve(resolveItem0, resolveInfo0);

        YIO_LOG_INFO("Sleep for internal DiscoveredItems timeout");
        std::this_thread::sleep_for(std::chrono::seconds(11));
        dItems->periodicCheck();
        YIO_LOG_INFO("Waiting for connect to second resolve");
        connected[0].get_future().get();

        auto testDevice1 = std::make_shared<YandexIO::Device>(
            getDeviceForTests()->deviceId() + "_1",
            QuasarUnitTestFixture::makeTestConfiguration(),
            std::make_shared<NullMetrica>(),
            QuasarUnitTestFixture::makeTestHAL());

        YandexIO::Configuration::TestGuard testGuard1;
        Json::Value& config1 = testDevice1->configuration()->getMutableConfig(testGuard1);
        auto wsPort1Config = getPort();
        config1["glagold"]["externalPort"] = wsPort1Config;
        config1["common"]["deviceType"] = "yandexmini";
        config1["common"]["backendUrl"] = fixtureBackendUrl;

        auto mockDeviceStateProvider1 = std::make_shared<mock::DeviceStateProvider>();
        mockDeviceStateProvider1->setDeviceState(mock::defaultDeviceState());
        GlagolWsServer server1(testDevice1, ipcFactoryForTests(), mockAuthProvider, mockDeviceStateProvider1);
        server1.updateAccountDevices(accountDevices1);
        YIO_LOG_INFO("Starting _1 configured on port " << wsPort1Config);
        server1.start();
        const auto wsPort1 = server1.getPort();
        YIO_LOG_INFO("Server for _1 device run on port " << wsPort1);

        txt["deviceId"] = ID_PREFIX + "_1";
        txt["platform"] = platforms[1];
        const glagol::ResolveItem::Info resolveInfo1{.hostname = "localhost", .port = wsPort1, .address = "localhost", .txt = txt};
        dItems->newResolve(resolveItem1, resolveInfo1);
        YIO_LOG_INFO("Waiting for reconect to new resolve");
        std::this_thread::sleep_for(std::chrono::seconds(11));
        dItems->periodicCheck();
        connected[1].get_future().get();

        std::deque<glagol::IDiscoveredItems::ConnectorPtr> unusedConnectors;
        dItems->setOnDiscoveryCallback([&unusedConnectors](const glagol::DeviceId& id, glagol::IDiscoveredItems::ConnectorPtr connector) {
            YIO_LOG_DEBUG("Connector to " << id.id << " not used");
            unusedConnectors.push_back(connector);
        });

        int totalResolves = 0;
        int knownItemsMap = 0;
        dItems->eachResolvedItem([&](auto item, auto info) {
            ++totalResolves;
            if (item == resolveItem0) {
                knownItemsMap |= 1;
                info.hostname = resolveInfo0.hostname;
                UNIT_ASSERT(info == resolveInfo0);
            } else if (item == resolveItem1) {
                knownItemsMap |= 2;

                YIO_LOG_DEBUG(info.address << ':' << info.port << " vs " << resolveInfo1.address << ':' << resolveInfo1.port);
                info.hostname = resolveInfo1.hostname;
                UNIT_ASSERT(info == resolveInfo1);
            } else {
                UNIT_ASSERT_C(false, "wrong resolve " + item.name);
            }
        });
        UNIT_ASSERT_VALUES_EQUAL(totalResolves, 2);
        UNIT_ASSERT_VALUES_EQUAL(knownItemsMap, 3);

        int aroundMap = 0;
        for (const auto& [id, dev] : dItems->getDevicesAround()) {
            if (id.id == ID_PREFIX) {
                aroundMap |= 1;
                UNIT_ASSERT_EQUAL(id.platform, platforms[0]);
            } else if (id.id == ID_PREFIX + "_1") {
                aroundMap |= 2;
                UNIT_ASSERT_EQUAL(id.platform, platforms[1]);
            } else {
                UNIT_ASSERT_C(false, "wrong device " + id.id + " " + id.platform);
            }
        }
        UNIT_ASSERT_VALUES_EQUAL(aroundMap, 3);
    }

    Y_UNIT_TEST_F(discoveredItemsHandleResolveViaAccountDevices, GlagolInit)
    {
        auto testBackedApi = makeTestBackendApi();
        const std::string ID_PREFIX = getDeviceForTests()->deviceId();
        auto dItems = glagol::createDiscoveredItems({.id = ID_PREFIX + "_2", .platform = "yandexstation_2"},
                                                    getDeviceForTests()->telemetry(),
                                                    testBackedApi,
                                                    nullptr,
                                                    [testBackedApi]() {
                                                        return std::make_shared<glagol::ext::Connector>(testBackedApi,
                                                                                                        std::make_shared<NullMetrica>());
                                                    });
        std::promise<glagol::IDiscoveredItems::ConnectorPtr> connected;
        dItems->setOnDiscoveryCallback([&connected](const glagol::DeviceId& /*id*/, glagol::IDiscoveredItems::ConnectorPtr connector) {
            connected.set_value(connector);
        });
        glagol::BackendApi::DevicesMap accountDevices1 = accountDevices;
        auto& networkInfo = accountDevices1.begin()->second.networkInfo;
        networkInfo.externalPort = getFixtureExternalPort();
        networkInfo.IPs.push_back("localhost");
        dItems->updateAccountDevices(accountDevices1);
        connected.get_future().get();
    }

    Y_UNIT_TEST_F(discoveredItemsWrongTlsShort, DiscoveredItemsBadTlsFixture)
    {
        const auto wsPort0 = getFixtureExternalPort();

        YIO_LOG_INFO("server0 is listening on " << wsPort0);

        glagol::ResolveItem::Info::StringMap txt;
        txt["cluster"] = "yes";
        txt["deviceId"] = ID_PREFIX;
        txt["platform"] = fixtureDeviceId.platform;
        const glagol::ResolveItem resolveItem0 = {RESOLVE_STRS::PREFIX + ID_PREFIX, RESOLVE_STRS::TYPE, "local"};
        const glagol::ResolveItem::Info resolveInfo0 = {.hostname = "localhost", .port = wsPort0, .address = "localhost", .txt = txt};

        bool tlsChanged = false;
        testBackedApi->invalidCertCb_ = [&](const glagol::DeviceId& id) {
            YIO_LOG_INFO("Invalid cert");
            UNIT_ASSERT(id == fixtureDeviceId);
            accountDevices[id].glagol.security.serverCertificate = TestKeys().PUBLIC_PEM;
            dItems->updateAccountDevices(accountDevices);
            tlsChanged = true;
        };

        dItems->newResolve(resolveItem0, resolveInfo0);
        waitForConnection();
        UNIT_ASSERT(tlsChanged);
    }

    Y_UNIT_TEST_F(discoveredItemsWrongTlsLong, DiscoveredItemsBadTlsFixture)
    {
        const auto wsPort0 = getFixtureExternalPort();

        YIO_LOG_INFO("server0 is listening on " << wsPort0);

        glagol::ResolveItem::Info::StringMap txt;
        txt["cluster"] = "yes";
        txt["deviceId"] = ID_PREFIX;
        txt["platform"] = fixtureDeviceId.platform;
        const glagol::ResolveItem resolveItem0 = {RESOLVE_STRS::PREFIX + ID_PREFIX, RESOLVE_STRS::TYPE, "local"};
        const glagol::ResolveItem::Info resolveInfo0 = {.hostname = "localhost", .port = wsPort0, .address = "localhost", .txt = txt};

        std::promise<void> tlsChanged;
        testBackedApi->invalidCertCb_ = [&](const glagol::DeviceId& id) {
            YIO_LOG_INFO("Invalid cert");
            UNIT_ASSERT(id == fixtureDeviceId);
            tlsChanged.set_value();
        };

        dItems->newResolve(resolveItem0, resolveInfo0);

        tlsChanged.get_future().get();

        accountDevices[fixtureDeviceId].glagol.security.serverCertificate = TestKeys().PUBLIC_PEM;
        dItems->updateAccountDevices(accountDevices);

        waitForConnection();
    }

    Y_UNIT_TEST_F(discoveredItemsNeighborsUpdatedOnAccountDeviceUpdates, DiscoveredItemsBadTlsFixture)
    {
        std::mutex updateMutex;
        std::condition_variable updateCond;
        bool updated = false;
        dItems->setOnNeighborsUpdated([&]() {
            std::scoped_lock<std::mutex> lock(updateMutex);
            updated = true;
            updateCond.notify_all();
        });

        auto waitForUpdate = [&]() {
            {
                YIO_LOG_INFO("Waiting for update discoveries");
                std::unique_lock<std::mutex> lock(updateMutex);
                updateCond.wait(lock, [&updated]() {
                    return updated;
                });
                updated = false;
            }
            return dItems->getDiscoveries().items;
        };

        glagol::ResolveItem::Info::StringMap txt;
        txt["cluster"] = "yes";
        txt["deviceId"] = fixtureDeviceId.id;
        txt["platform"] = fixtureDeviceId.platform;
        const glagol::ResolveItem resolveItem0 = {RESOLVE_STRS::PREFIX + ID_PREFIX, RESOLVE_STRS::TYPE, "local"};
        const glagol::ResolveItem::Info resolveInfo0 = {.hostname = "localhost", .port = 1234, .address = "1.2.3.4", .txt = txt};

        dItems->newResolve(resolveItem0, resolveInfo0);

        dItems->updateAccountDevices(accountDevices);
        auto discoveries = waitForUpdate();

        UNIT_ASSERT(discoveries.count(fixtureDeviceId));

        accountDevices[fixtureDeviceId].name = "anothername";
        dItems->updateAccountDevices(accountDevices);
        discoveries = waitForUpdate();
        UNIT_ASSERT(discoveries.count(fixtureDeviceId));
        UNIT_ASSERT_EQUAL(discoveries[fixtureDeviceId].accountDeviceName, accountDevices[fixtureDeviceId].name);

        {
            auto accountDevices2 = accountDevices;
            accountDevices2.erase(fixtureDeviceId);
            dItems->updateAccountDevices(accountDevices2);
            discoveries = waitForUpdate();
            UNIT_ASSERT(!discoveries[fixtureDeviceId].isAccountDevice);
        }

        dItems->setSettings({.outdatingPeriod = std::chrono::seconds(1)});
        std::this_thread::sleep_for(std::chrono::seconds(11));
        dItems->periodicCheck();
        YIO_LOG_INFO("Final check for update");
        discoveries = waitForUpdate();
        UNIT_ASSERT(discoveries.empty());
    }

    Y_UNIT_TEST_F(discoveredItemsNeighborsUpdatedOnConnectionAddressChange, DiscoveredItemsBadTlsFixture)
    {
        std::mutex updateMutex;
        std::condition_variable updateCond;
        bool updated = false;
        dItems->setOnNeighborsUpdated([&]() {
            std::scoped_lock<std::mutex> lock(updateMutex);
            updated = true;
            updateCond.notify_all();
        });

        auto waitForUpdate = [&]() {
            {
                YIO_LOG_INFO("Waiting for update discoveries");
                std::unique_lock<std::mutex> lock(updateMutex);
                bool success = updateCond.wait_for(lock, std::chrono::seconds(60), [&updated]() {
                    return updated;
                });
                UNIT_ASSERT_C(success, "Waited too long for new discoveries");
                updated = false;
            }
            return dItems->getDiscoveries().items;
        };

        const glagol::DeviceId id{.id = ID_PREFIX, .platform = "yandexstation"};

        dItems->addResolve(id, {.address = "1.2.3.4", .port = 1234, .cluster = true}, ResolveSource::MDNS);
        dItems->addResolve(id, {.address = "1.2.3.6", .port = 1234, .cluster = true}, ResolveSource::MDNS);
        dItems->updateAccountDevices(accountDevices);
        auto discoveries = waitForUpdate();
        std::this_thread::sleep_for(std::chrono::seconds(11));
        dItems->periodicCheck();
        waitForUpdate();
        dItems.reset();
    }

    Y_UNIT_TEST_F(discoveredItemsStore, DiscoveredItemsBadTlsFixture)
    {
        const auto wsPort0 = getFixtureExternalPort();
        dItems->setSettings({.saveInterval = std::chrono::seconds(1)});
        glagol::ResolveItem::Info::StringMap txt;
        txt["cluster"] = "yes";
        txt["deviceId"] = ID_PREFIX;
        txt["platform"] = fixtureDeviceId.platform;
        const glagol::ResolveItem resolveItem0 = {RESOLVE_STRS::PREFIX + ID_PREFIX, RESOLVE_STRS::TYPE, "local"};
        const glagol::ResolveItem::Info resolveInfo0 = {.hostname = "localhost", .port = wsPort0, .address = "localhost", .txt = txt};
        dItems->newResolve(resolveItem0, resolveInfo0);

        std::this_thread::sleep_for(std::chrono::seconds(2));
        dItems->periodicCheck();

        UNIT_ASSERT(!storage->content.empty());

        dItems.reset();

        makeDiscoveryItems();

        accountDevices[fixtureDeviceId].glagol.security.serverCertificate = TestKeys().PUBLIC_PEM;
        dItems->updateAccountDevices(accountDevices);
        UNIT_ASSERT(!dItems->noDevicesAround());
        waitForConnection();
    }

    Y_UNIT_TEST_F(discoveredItemsDoNotForgetConnected, DiscoveredItemsBadTlsFixture)
    {
        accountDevices[fixtureDeviceId].glagol.security.serverCertificate = TestKeys().PUBLIC_PEM;
        dItems->updateAccountDevices(accountDevices);
        {
            dItems->addResolve(fixtureDeviceId, ResolveInfo{.address = "localhost", .port = getFixtureExternalPort(), .cluster = true}, ResolveSource::MDNS);
        }
        waitForConnection();
        dItems->setSettings({.outdatingPeriod = std::chrono::seconds(1)});
        std::this_thread::sleep_for(std::chrono::seconds(2));
        connected.reset();
        waitForConnection();
    }

    Y_UNIT_TEST_F(guestModeTokens, ServicesInit)
    {
        GlagolWsServer server(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockDeviceStateProvider);
        server.updateAccountDevices(accountDevices);
        server.setOnMessage([&server](WebsocketServer::ConnectionHdl hdl, GlagolWsServer::MsgConnInfo info, const Json::Value& /* request */) {
            YIO_LOG_INFO("on message");
            if (info.guestMode) {
                server.send(hdl, "{\"status\": \"guest\"}");
            } else {
                server.send(hdl, "{\"status\": \"owner\"}");
            }
        });
        server.setGuestMode(true);
        server.start();
        std::mutex clientConnectionMutex;
        std::atomic_bool clientHasConnection{false};
        SteadyConditionVariable clientConnectionCondVar;
        WebsocketClient::DisposableClient::Settings settings;
        settings.tls.crtBuffer = TestKeys().PUBLIC_PEM;
        settings.tls.verifyHostname = false;
        const auto serverUrl = "wss://localhost:" + std::to_string(server.getPort()) + "/";
        auto client = WebsocketClient::DisposableClient::create(serverUrl, settings, std::make_shared<NullMetrica>());

        client->onConnect = [&](std::weak_ptr<WebsocketClient::DisposableClient> /* wthis */) {
            YIO_LOG_INFO("Connected");
            std::lock_guard<std::mutex> guard(clientConnectionMutex);
            clientHasConnection = true;
            clientConnectionCondVar.notify_one();
        };

        client->onDisconnect = [&](std::weak_ptr<WebsocketClient::DisposableClient> /*client*/, const Websocket::ConnectionInfo& connInfo) {
            YIO_LOG_INFO("Disconnected");
            std::lock_guard<std::mutex> guard(clientConnectionMutex);
            clientHasConnection = false;
            clientConnectionCondVar.notify_one();
            UNIT_ASSERT(connInfo.remote.closeCode == Websocket::StatusCode::INVALID_TOKEN);
        };

        std::promise<void> guestPromise;
        client->onMessage = [&guestPromise](std::weak_ptr<WebsocketClient::DisposableClient> /*client*/, const std::string& msg) {
            Json::Value jmsg = parseJson(msg);

            if (jmsg["status"] == "guest") {
                guestPromise.set_value();
            } else {
                YIO_LOG_WARN("Received response but not guest: <" << msg << ">");
            }
        };

        YIO_LOG_INFO("Connecting to " << serverUrl);
        client->connectAsyncWithTimeout();

        {
            /* Make sure that client connected to server */
            std::unique_lock<std::mutex> lock(clientConnectionMutex);
            clientConnectionCondVar.wait(lock, [&]() {
                return clientHasConnection.load();
            });
        }
        const auto jwtGuestToken = genMyGuestToken();
        client->send("{\"conversationToken\": \"" + jwtGuestToken + "\"}");
        YIO_LOG_INFO("wait for guest accepted");
        guestPromise.get_future().get();

        server.setGuestMode(false);
        client->send("{\"conversationToken\": \"" + jwtGuestToken + "\"}");
    }

    Y_UNIT_TEST_F(guestModeCommands, GlagolInit)
    {
        glagol::BackendApi::DevicesMap accountDevices1 = accountDevices;
        const std::string CLIENT_DEVICE_ID = "TEST2";
        {
            glagol::DeviceId moreId = {CLIENT_DEVICE_ID, "yandexmini"};
            auto& dev = accountDevices1[moreId];
            dev.name = "quasar";
        };
        accountDevices1[{getDeviceForTests()->deviceId(), "yandexstation"}].guestMode = true;
        mockSyncd->sendToAll(makeAccountDevicesListMessage(accountDevices1));

        WebsocketClient client(std::make_shared<NullMetrica>());
        WebsocketClient::Settings clientSettings;
        clientSettings.tls.crtBuffer = TestKeys().PUBLIC_PEM;
        clientSettings.tls.verifyHostname = false;
        clientSettings.reconnect.enabled = false;
        clientSettings.ping.enabled = true;

        clientSettings.url = "wss://localhost:" + std::to_string(getFixtureExternalPort()) + "/";

        std::mutex msgMutex;
        SteadyConditionVariable msgCondVar;
        bool connected = false;
        std::string msg;
        client.setOnConnectHandler([&]() {
            std::lock_guard<std::mutex> lock(msgMutex);
            connected = true;
            msgCondVar.notify_all();
        });

        client.setOnMessageHandler([&](const std::string& inMsg) {
            std::lock_guard<std::mutex> lock(msgMutex);
            msg = inMsg;
            msgCondVar.notify_all();
        });

        auto waitConnect = [&]() {
            std::unique_lock<std::mutex> lock(msgMutex);
            msgCondVar.wait(lock, [&]() { return connected; });
            YIO_LOG_INFO("Client connected");
        };

        client.setOnDisconnectHandler([&](const Websocket::ConnectionInfo& connInfo) {
            YIO_LOG_INFO("Client disconnected");
            if (connInfo.remote.closeCode == Websocket::StatusCode::INVALID_TOKEN) {
                YIO_LOG_INFO("Wrong token. Maybe guest mode still not turned on, retry.");
                client.connectAsync(clientSettings);
                waitConnect();
            }
        });

        client.connectAsync(clientSettings);
        waitConnect();

        auto guestToken = genMyGuestToken();
        const auto command = makeClusterHelo(CLIENT_DEVICE_ID, guestToken);
        Json::Value jsonMsg;
        auto waitMsg = [&]() {
            std::unique_lock<std::mutex> lock(msgMutex);
            msgCondVar.wait(lock, [&]() { return !msg.empty(); });
            jsonMsg = parseJson(msg);
            msg.clear();
            YIO_LOG_INFO("Message came: " << jsonMsg);
        };

        while (true) {
            client.unsafeSend(command);
            waitMsg();
            if (jsonMsg.isMember("error") && jsonMsg["error"].asString() == Glagol::GUEST_FORBIDDEN_ERROR) {
                break;
            }
        }

        Json::Value request;
        request["payload"]["command"] = "ping";

        oneshot(request, guestToken);

        // same via first client

        request["conversationToken"] = guestToken;
        client.unsafeSend(jsonToString(request));
        waitMsg();
        UNIT_ASSERT(jsonMsg.isMember("status"));
        UNIT_ASSERT_EQUAL(jsonMsg["status"].asString(), "SUCCESS");
        UNIT_ASSERT(jsonMsg.isMember("state")); // if cluster hello passed as command 'state' will be absent here
    }

    Y_UNIT_TEST(resolvesTracker) {
        glagol::ResolvesTracker tracker;
        UNIT_ASSERT(!tracker.hasResolve());
        UNIT_ASSERT(!tracker.hasClusterResolve());
        tracker.addResolve({.address = "1.2.3.4", .protocol = ResolveInfo::Protocol::IPV4, .port = 1234}, ResolveSource::MDNS);
        UNIT_ASSERT(tracker.hasResolve());
        UNIT_ASSERT(!tracker.hasClusterResolve());
        tracker.addResolve({.address = "1.2.3.5", .protocol = ResolveInfo::Protocol::IPV4, .port = 1234, .cluster = true}, ResolveSource::MDNS);
        UNIT_ASSERT(tracker.hasClusterResolve());
        // checking selecting next resolve
        auto res1 = tracker.getLockedResolve();
        UNIT_ASSERT(res1.uri() == tracker.getLastResolve().uri());
        tracker.unlock(false);
        auto res2 = tracker.getLockedResolve();
        UNIT_ASSERT(res1.uri() != res2.uri());
        UNIT_ASSERT(res2.uri() == tracker.getLastResolve().uri());
        // checking choosing last connect
        {
            auto res3 = tracker.getLockedResolve();
            UNIT_ASSERT_EQUAL(res3.uri(), res2.uri());
        }
        std::this_thread::sleep_for(std::chrono::seconds(7));
        tracker.unlock(true);
        tracker.removeOutdates(std::chrono::seconds(5));
        UNIT_ASSERT(!tracker.isCluster());
        UNIT_ASSERT(!tracker.hasClusterResolve());
        UNIT_ASSERT(tracker.hasResolve());
        auto res3 = tracker.getLockedResolve();
        UNIT_ASSERT_EQUAL(res2.uri(), res3.uri());
        tracker.unlock(false);
        std::this_thread::sleep_for(std::chrono::seconds(1));
        tracker.removeOutdates(std::chrono::seconds(1));
        UNIT_ASSERT(!tracker.hasResolve());
        tracker.addResolve(res1, ResolveSource::MDNS);
        UNIT_ASSERT(tracker.hasClusterResolve());
        UNIT_ASSERT(tracker.isCluster());
    }

    Y_UNIT_TEST(resolvesTrackerSearialize) {

        auto check = [](glagol::ResolvesTracker& tracker) {
            UNIT_ASSERT(tracker.hasClusterResolve());
            auto res2 = tracker.getLockedResolve();
            UNIT_ASSERT_EQUAL(res2.address, "ff80::1");
            UNIT_ASSERT_EQUAL(res2.port, 5678);
            UNIT_ASSERT(res2.protocol == ResolveInfo::Protocol::IPV6);
            tracker.unlock(false);
            auto res1 = tracker.getLockedResolve();
            UNIT_ASSERT_EQUAL(res1.address, "1.2.3.4");
            UNIT_ASSERT_EQUAL(res1.port, 1234);
            UNIT_ASSERT(res1.protocol == ResolveInfo::Protocol::IPV4);
            tracker.unlock(false);
        };

        Json::Value storage;
        {
            glagol::ResolvesTracker tracker;
            tracker.addResolve({.address = "1.2.3.4", .protocol = ResolveInfo::Protocol::IPV4, .port = 1234}, ResolveSource::MDNS);
            tracker.addResolve({.address = "ff80::1", .protocol = ResolveInfo::Protocol::IPV6, .port = 5678, .cluster = true}, ResolveSource::MDNS);
            YIO_LOG_INFO("Check tracker content before serialization");
            check(tracker);
            storage = tracker.serialize();
        }

        glagol::ResolvesTracker tracker;
        tracker.deserialize(storage);
        YIO_LOG_INFO("Check tracker content after deserialization back");
        check(tracker);
    }

    Y_UNIT_TEST(resolvesTrackerNotOutdatedConnected) {
        glagol::ResolvesTracker tracker;
        tracker.addResolve({.address = "1.2.3.4", .protocol = ResolveInfo::Protocol::IPV4, .port = 1234}, ResolveSource::MDNS);
        const auto res1 = tracker.getLockedResolve();
        tracker.addResolve({.address = "1.2.3.5", .protocol = ResolveInfo::Protocol::IPV4, .port = 1234}, ResolveSource::MDNS);
        std::this_thread::sleep_for(std::chrono::seconds(2));
        tracker.removeOutdates(std::chrono::seconds(1));
        tracker.unlock(true);
        const auto res2 = tracker.getLastResolve();
        UNIT_ASSERT_EQUAL(res1.uri(), res2.uri());
    }

    Y_UNIT_TEST_F(glagolConnectViaMockResolve, GlagolInit) {
        auto testDevice2 = std::make_shared<YandexIO::Device>(
            getDeviceForTests()->deviceId() + "_1",
            QuasarUnitTestFixture::makeTestConfiguration(),
            std::make_shared<NullMetrica>(),
            QuasarUnitTestFixture::makeTestHAL());

        glagol::DeviceId glagolId2 = {
            .id = testDevice2->deviceId(),
            .platform = "yandexmini",
        };

        auto& dev = accountDevices[glagolId2];
        dev.glagol.security.serverCertificate = TestKeys().PUBLIC_PEM;
        dev.glagol.security.serverPrivateKey = TestKeys().PRIVATE_PEM;
        dev.name = "quasar";
        mockSyncd->sendToAll(makeAccountDevicesListMessage(accountDevices));

        YandexIO::Configuration::TestGuard testGuard2;
        Json::Value& config2 = testDevice2->configuration()->getMutableConfig(testGuard2);
        const auto wsPort2 = getPort();
        { // prepare second device
            const Json::Value& config = getDeviceForTests()->configuration()->getCommonConfig();

            config2["glagold"]["externalPort"] = wsPort2;
            config2["common"]["deviceType"] = glagolId2.platform;
            config2["common"]["backendUrl"] = config["backendUrl"];
        }

        auto mockDeviceStateProvider2 = std::make_shared<mock::DeviceStateProvider>();
        mockDeviceStateProvider2->setDeviceState(mock::defaultDeviceState());

        GlagolWsServer server(testDevice2, ipcFactoryForTests(), mockAuthProvider, mockDeviceStateProvider2);

        server.updateAccountDevices(accountDevices);

        std::promise<void> message;
        server.setOnMessage([this, &message](WebsocketServer::ConnectionHdl /*hdl*/, GlagolWsServer::MsgConnInfo /*info*/, const Json::Value& request) {
            const Json::Value& payload = request["payload"];
            if (payload["command"].asString() == "clusterHelo" &&
                payload["from_device_id"].asString() == getDeviceForTests()->deviceId()) {
                message.set_value();
            }
        });

        server.start();

        mockMdnsHolder->anounce(glagolId2, "localhost", wsPort2);

        message.get_future().get();
    }

}

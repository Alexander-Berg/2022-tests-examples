#include <yandex_io/capabilities/file_player/interfaces/mocks/mock_i_file_player_capability.h>

#include <yandex_io/services/firstrund/access_point.h>
#include <yandex_io/services/firstrund/first_run_endpoint.h>

#include <yandex_io/interfaces/auth/mock/auth_provider.h>
#include <yandex_io/interfaces/device_state/mock/device_state_provider.h>
#include <yandex_io/interfaces/updates/mock/updates_provider.h>
#include <yandex_io/interfaces/user_config/mock/user_config_provider.h>
#include <yandex_io/libs/base/crc32.h>
#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/cryptography/cryptography.h>
#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/http_client/http_client.h>
#include <yandex_io/libs/ipc/i_server.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/threading/steady_condition_variable.h>
#include <yandex_io/sdk/auth_observer.h>
#include <yandex_io/sdk/device_mode_observer.h>
#include <yandex_io/tests/testlib/test_http_server.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/folder/path.h>

#include <cstdio>
#include <fstream>
#include <future>
#include <memory>

using namespace quasar;
using namespace quasar::proto;
using namespace quasar::TestUtils;

namespace {
    class TestAuthObserver: public YandexIO::AuthObserver {
    public:
        void onAuthenticationStatus(const std::string& oauthCode, bool isOk, const std::string& /*message*/) override {
            std::scoped_lock<std::mutex> guard(mutex_);
            lastTryOauthCode_ = oauthCode;
            lastTrySuccess_ = isOk;
            CV_.notify_all();
        }
        void onInvalidOAuthToken(const std::string& /*oauthToken*/) override {
        }
        void onInvalidAuthentication(const std::string& /*uid*/) override {
        }

        bool lastTryResults(std::chrono::seconds seconds, std::pair<std::string, bool>& result) {
            std::unique_lock<std::mutex> lock(mutex_);
            CV_.wait_for(lock, seconds, [this]() {
                return !lastTryOauthCode_.empty();
            });
            if (lastTryOauthCode_.empty()) {
                return false;
            }
            result.first = lastTryOauthCode_;
            result.second = lastTrySuccess_;
            lastTryOauthCode_.clear();
            return true;
        }

    private:
        std::string lastTryOauthCode_;
        bool lastTrySuccess_ = false;
        std::mutex mutex_;
        SteadyConditionVariable CV_;
    };

    struct MockAccessPoint: public AccessPoint {
        void start(const std::string& /*accessPointName*/) override {
            started_ = true;
        }
        void stop() override {
            started_ = false;
        }
        bool isStarted() const override {
            return started_;
        }

    private:
        bool started_ = false;
    };

    class IOHubWrapper {
    public:
        IOHubWrapper(std::shared_ptr<ipc::IIpcFactory> ipcFactory)
            : server_(ipcFactory->createIpcServer("iohub_services"))
        {
            server_->setClientConnectedHandler([](auto& connection) {
                /* Send start allow_init_configuration_state message, so firstrund will connect to "authd" */
                proto::QuasarMessage msg;
                msg.mutable_io_control()->mutable_allow_init_configuration_state();
                connection.send(std::move(msg));
            });
            server_->setMessageHandler([this](const auto& msg, auto& conn) {
                if (handler_) {
                    handler_(msg, conn);
                }
            });
            server_->listenService();
        }

        ~IOHubWrapper() {
            server_->shutdown();
        }
        void setMessageHandler(std::function<void(const quasar::ipc::SharedMessage& request, quasar::ipc::IServer::IClientConnection& connection)> handler) {
            handler_ = std::move(handler);
        }

        void authenticate(const std::string xcode) {
            proto::QuasarMessage message;
            message.mutable_io_control()->set_authenticate(TString(xcode));
            server_->sendToAll(std::move(message));
        }

    private:
        std::function<void(const quasar::ipc::SharedMessage& request, quasar::ipc::IServer::IClientConnection& connection)> handler_;
        std::shared_ptr<ipc::IServer> server_;
    };

    struct Fixture: public QuasarUnitTestFixture {
        using OnMessageHandler = std::function<void(const ipc::SharedMessage& request, ipc::IServer::IClientConnection& connection)>;

        using Base = QuasarUnitTestFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            mockWifid = createIpcServerForTests("wifid");
            mockPushd = createIpcServerForTests("pushd");
            mockNetworkd = createIpcServerForTests("networkd");

            mockWifid->setMessageHandler([this](const auto& msg, auto& conn) {
                if (wifidMessageHandler_) {
                    wifidMessageHandler_(msg, conn);
                }
            });

            quasarDirPath = JoinFsPaths(tryGetRamDrivePath(), "quasarDir-" + makeUUID());
            firstGreetingFilename = JoinFsPaths(quasarDirPath, "first_greeting_done");
            registeredFilename = JoinFsPaths(quasarDirPath, "registered");
            wifiDataFilename = JoinFsPaths(quasarDirPath, "wifi.dat");

            /* Clean up test files */
            quasarDirPath.ForceDelete();
            quasarDirPath.MkDirs();

            auto& config = getDeviceForTests()->configuration()->getMutableConfig(configGuard);

            config["firstrund"]["wifiStoragePath"] = wifiDataFilename.GetPath();
            config["firstrund"]["firstGreetingDoneFilePath"] = firstGreetingFilename.GetPath();
            config["firstrund"]["registeredFilePath"] = registeredFilename.GetPath();

            mockAuthProvider = std::make_shared<mock::AuthProvider>();
            mockUpdatesProvider = std::make_shared<mock::UpdatesProvider>();
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            /* Clean up test files */
            quasarDirPath.ForceDelete();

            Base::TearDown(context);
        }

        void start(bool setupDefaultCallbacks = true, bool waitConfiguringMode = true) {
            auto& config = getDeviceForTests()->configuration()->getMutableConfig(configGuard);

            mockWifid->listenService();
            mockPushd->listenService();
            mockNetworkd->listenService();

            const int backendPort = mockBackend.start(getPort());
            config["common"]["backendUrl"] = "http://localhost:" + std::to_string(backendPort);

            /* allocate port for firstrund */
            const auto firstRundHttpPort = getPort();
            config["firstrund"]["httpPort"] = firstRundHttpPort;

            ioSDK_ = std::make_unique<IOHubWrapper>(ipcFactoryForTests());

            if (setupDefaultCallbacks) {
                setSendStartUpInfoOnStart();
                setWifiResponseOk();
            }

            mockAuthProvider->setOwner(
                AuthInfo2{
                    .source = AuthInfo2::Source::AUTHD,
                    .authToken = "$authToken$",
                    .passportUid = "123",
                    .tag = 1600000000,
                });
            auto mockDeviceStateProvider = std::make_shared<mock::DeviceStateProvider>();
            mockDeviceStateProvider->setDeviceState(mock::defaultDeviceState());
            auto mockUserConfigProvider = std::make_shared<mock::UserConfigProvider>(nullptr);
            auto mockFilePlayerCapability = std::make_shared<YandexIO::MockIFilePlayerCapability>();

            endpoint = std::make_unique<FirstRunEndpoint>(getDeviceForTests(),
                                                          ipcFactoryForTests(),
                                                          mockAuthProvider,
                                                          mockDeviceStateProvider,
                                                          mockUpdatesProvider,
                                                          mockUserConfigProvider,
                                                          mockFilePlayerCapability);
            accessPoint = new MockAccessPoint();
            endpoint->resetAccessPoint(accessPoint);
            endpoint->setMinConnectError(std::chrono::milliseconds(100));
            endpoint->start();
            baseUrl = "http://localhost:" + std::to_string(firstRundHttpPort);

            /* Make sure that Firstrund connect to authd after DeviceConext::onQuasarStart handler
             * 1 connection is for Activation State Receiver inside CheckToken, second for FirstrunEndpoint
             */
            endpoint->waitConnectorsConnections();
            /* Make sure FirstRunEndpoint enter configuration mode */
            if (waitConfiguringMode) {
                TestUtils::waitUntil([this]() {
                    return endpoint->isConfigurationMode();
                });
            }
        }

        void setSendStartUpInfoOnStart() const {
            mockAuthProvider->setOwner(AuthInfo2{});
        }

        void setWifiMessageHandler(OnMessageHandler handler) {
            wifidMessageHandler_ = std::move(handler);
        }

        void setWifiResponseOk() {
            setWifiMessageHandler([](const ipc::SharedMessage& request, auto& connection) {
                QuasarMessage response;
                response.set_request_id(request->request_id());
                response.mutable_wifi_connect_response()->set_status(WifiConnectResponse::OK);
                connection.send(std::move(response));
            });
        }

        OnMessageHandler wifidMessageHandler_;

        TFsPath quasarDirPath;
        TFsPath firstGreetingFilename;
        TFsPath registeredFilename;
        TFsPath wifiDataFilename;

        MockAccessPoint* accessPoint;
        std::shared_ptr<mock::AuthProvider> mockAuthProvider;
        std::shared_ptr<mock::UpdatesProvider> mockUpdatesProvider;
        std::shared_ptr<ipc::IServer> mockWifid;
        std::shared_ptr<ipc::IServer> mockPushd;
        std::shared_ptr<ipc::IServer> mockNetworkd;
        TestHttpServer mockBackend;
        YandexIO::Configuration::TestGuard configGuard;
        std::unique_ptr<FirstRunEndpoint> endpoint;
        std::string baseUrl;
        std::unique_ptr<IOHubWrapper> ioSDK_;
    };

    struct AutoFixture: public Fixture {
        using Base = Fixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            start();
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            Base::TearDown(context);
        }
    };
} // namespace

Y_UNIT_TEST_SUITE(FirstRunEndpointTest) {

    Y_UNIT_TEST_F(testFirstRunEndpointPing, AutoFixture) {
        HttpClient client("test", getDeviceForTests());

        auto response = client.get("testreq", baseUrl + "/ping");

        UNIT_ASSERT_VALUES_EQUAL(response.responseCode, 200);
        UNIT_ASSERT_VALUES_EQUAL(response.contentType, "application/json");
        auto body = parseJson(response.body);
        UNIT_ASSERT_VALUES_EQUAL(body["status"].asString(), "ok");
    }

    Y_UNIT_TEST_F(testFirstRunEndpointInfo, AutoFixture) {
        HttpClient client("test", getDeviceForTests());

        auto response = client.get("testreq", baseUrl + "/info");

        UNIT_ASSERT_VALUES_EQUAL(response.responseCode, 200);
        UNIT_ASSERT_VALUES_EQUAL(response.contentType, "application/json");
        auto body = parseJson(response.body);
        UNIT_ASSERT(!body["uuid"].asString().empty());
        UNIT_ASSERT(body["crypto_key"].isString());
    }

    Y_UNIT_TEST_F(testFirstRunEndpointSsid, AutoFixture) {
        setWifiMessageHandler([&](const ipc::SharedMessage& request, auto& connection) {
            QuasarMessage response;
            response.set_request_id(request->request_id());
            auto wifiList = response.mutable_wifi_list();
            auto hotspot = wifiList->add_hotspots();
            hotspot->set_ssid("MobDevInternet");
            hotspot->set_secure(true);
            hotspot->set_is_corporate(false);

            /* Corporate networks should be skipped by firstrund */
            hotspot = wifiList->add_hotspots();
            hotspot->set_ssid("corporate network");
            hotspot->set_secure(false);
            hotspot->set_is_corporate(true);

            hotspot = wifiList->add_hotspots();
            hotspot->set_ssid("myquasar");
            hotspot->set_secure(false);
            hotspot->set_is_corporate(false);

            connection.send(std::move(response));
        });

        HttpClient client("test", getDeviceForTests());
        auto response = client.get("testreq", baseUrl + "/ssid");

        UNIT_ASSERT_VALUES_EQUAL(response.responseCode, 200);
        auto body = parseJson(response.body);
        UNIT_ASSERT(body["ssid"].isArray());

        UNIT_ASSERT_VALUES_EQUAL(body["ssid"].size(), 2U);

        UNIT_ASSERT_VALUES_EQUAL(body["ssid"][0]["ssid"].asString(), "MobDevInternet");
        UNIT_ASSERT_VALUES_EQUAL(body["ssid"][0]["secure"].asBool(), true);
        UNIT_ASSERT_VALUES_EQUAL(body["ssid"][1]["ssid"].asString(), "myquasar");
        UNIT_ASSERT_VALUES_EQUAL(body["ssid"][1]["secure"].asBool(), false);
    }

    Y_UNIT_TEST_F(testFirstRunEndpointFullCycle, AutoFixture) {
        std::mutex testMutex;
        SteadyConditionVariable testCV;

        bool connectReceived = false;
        bool addUserRequestReceived = false;
        bool changeUserRequestReceived = false;
        std::string password = "abracadabra";
        setWifiMessageHandler([&](const ipc::SharedMessage& sharedRequest, auto& connection) {
            std::unique_lock<std::mutex> lock(testMutex);
            const auto& request = *sharedRequest;
            QuasarMessage response;
            response.set_request_id(request.request_id());
            if (request.has_wifi_list_request()) {
                auto wifiList = response.mutable_wifi_list();
                auto hotspot = wifiList->add_hotspots();
                hotspot->set_ssid("MobDevInternet");
                hotspot->set_secure(true);
                hotspot = wifiList->add_hotspots();
                hotspot->set_ssid("myquasar");
                hotspot->set_secure(false);
            } else if (request.has_wifi_connect()) {
                UNIT_ASSERT_VALUES_EQUAL(request.wifi_connect().wifi_id(), "MobDevInternet");
                UNIT_ASSERT_VALUES_EQUAL(request.wifi_connect().password(), password);
                UNIT_ASSERT(request.wifi_connect().has_wifi_type());
                UNIT_ASSERT_EQUAL(request.wifi_connect().wifi_type(), WifiType::WPA);
                connectReceived = true;
                testCV.notify_all();
                lock.unlock();
                std::this_thread::sleep_for(std::chrono::seconds(1)); // Emulate long wifi connecting
                lock.lock();
                UNIT_ASSERT(!addUserRequestReceived);
                response.mutable_wifi_connect_response()->set_status(WifiConnectResponse::OK);
            }
            connection.send(std::move(response));
        });

        mockUpdatesProvider->setConfirmUpdateApply([&] {});
        mockUpdatesProvider->setCheckUpdates([&] {});
        mockAuthProvider->setAddUser(
            [&](const std::string& authCode, UserType /*userType*/, bool /*withXToken*/, std::chrono::milliseconds /*timeout*/) {
                UNIT_ASSERT_VALUES_EQUAL(authCode, "the_x_token_code");

                IAuthProvider::AddUserResponse response;
                response.status = IAuthProvider::AddUserResponse::Status::OK;
                response.authToken = "the_auth_token";
                response.id = 424242;

                addUserRequestReceived = true;
                return response;
            });
        mockAuthProvider->setChangeUser(
            [&](int64_t id, std::chrono::milliseconds /*timeout*/) {
                changeUserRequestReceived = true;
                UNIT_ASSERT_VALUES_EQUAL(id, 424242);
                return IAuthProvider::ChangeUserResponse{
                    .status = IAuthProvider::ChangeUserResponse::Status::OK,
                };
            });

        bool registerReceived = false;
        mockBackend.onHandlePayload = [&](const TestHttpServer::Headers& header,
                                          const std::string& /*payload*/, TestHttpServer::HttpConnection& handler) {
            std::scoped_lock<std::mutex> lock(testMutex);
            UNIT_ASSERT_VALUES_EQUAL(header.resource, "/register");
            UNIT_ASSERT_VALUES_EQUAL(header.getHeader("authorization"), "OAuth the_auth_token");
            UNIT_ASSERT_VALUES_EQUAL(header.getHeader("x-quasar-signature-version"), "2");
            UNIT_ASSERT_VALUES_EQUAL(header.queryParams.getValue("device_id"), getDeviceForTests()->deviceId());
            UNIT_ASSERT_VALUES_EQUAL(header.queryParams.getValue("name"), "quasar");
            UNIT_ASSERT_VALUES_EQUAL(header.queryParams.getValue("platform"), "yandexstation");
            UNIT_ASSERT_VALUES_EQUAL(header.queryParams.getValue("activation_code"), std::to_string(getCrc32("the_x_token_code")));

            Cryptography crypto;
            crypto.loadPublicKey(ArcadiaSourceRoot() + "/yandex_io/misc/cryptography/public.pem");

            UNIT_ASSERT(crypto.checkSignature(header.query, base64Decode(urlDecode(header.getHeader("x-quasar-signature")))));

            registerReceived = true;
            testCV.notify_all();
            handler.doReplay(200, "application/json", "{\"status\":\"ok\"}");
        };

        HttpClient client("test", getDeviceForTests());
        client.setTimeout(std::chrono::milliseconds{100 * 1000});

        auto response = client.get("testreq", baseUrl + "/info");
        Json::Value info = parseJson(response.body);
        Cryptography cryptography;
        cryptography.setPublicKey(getString(info, "crypto_key"));

        response = client.get("testreq", baseUrl + "/ssid");

        UNIT_ASSERT_VALUES_EQUAL(response.responseCode, 200);
        auto body = parseJson(response.body);
        UNIT_ASSERT(body["ssid"].isArray());

        UNIT_ASSERT_VALUES_EQUAL(body["ssid"].size(), 2U);

        UNIT_ASSERT_VALUES_EQUAL(body["ssid"][0]["ssid"].asString(), "MobDevInternet");
        UNIT_ASSERT_VALUES_EQUAL(body["ssid"][0]["secure"].asBool(), true);
        UNIT_ASSERT(body["ssid"][1]["ssid"].isString());
        UNIT_ASSERT_VALUES_EQUAL(body["ssid"][1]["ssid"].asString(), "myquasar");
        UNIT_ASSERT_VALUES_EQUAL(body["ssid"][1]["secure"].asBool(), false);

        Json::Value connect;
        connect["ssid"] = body["ssid"][0]["ssid"].asString();
        std::string encrypted = cryptography.encrypt(password);
        connect["password"] = base64Encode(encrypted.c_str(), encrypted.length());
        encrypted = cryptography.encrypt("the_x_token_code");
        connect["xtoken_code"] = base64Encode(encrypted.c_str(), encrypted.length());
        connect["wifi_type"] = "WPA";
        response = client.post("testreq", baseUrl + "/connect", jsonToString(connect));

        {
            std::unique_lock<std::mutex> lock(testMutex);
            UNIT_ASSERT_VALUES_EQUAL(response.responseCode, 200);
            body = parseJson(response.body);
            UNIT_ASSERT_VALUES_EQUAL(body["status"].asString(), "ok");
            testCV.wait(lock, [&connectReceived, &addUserRequestReceived, &changeUserRequestReceived, &registerReceived]() {
                return connectReceived && addUserRequestReceived && changeUserRequestReceived && registerReceived;
            });
        }
    }

    Y_UNIT_TEST_F(testFirstRunEndpointCodeExpired, Fixture) {
        setWifiMessageHandler([&](const ipc::SharedMessage& request, auto& connection) {
            QuasarMessage response;
            response.set_request_id(request->request_id());
            response.mutable_wifi_connect_response()->set_status(WifiConnectResponse::OK);
            connection.send(std::move(response));
        });

        mockAuthProvider->setAddUser(
            [&](const std::string& /*authCode*/, UserType /*userType*/, bool /*withXToken*/, std::chrono::milliseconds /*timeout*/) {
                IAuthProvider::AddUserResponse response;
                response.status = IAuthProvider::AddUserResponse::Status::CODE_EXPIRED;
                return response;
            });
        start();

        HttpClient client("test", getDeviceForTests());
        client.setTimeout(std::chrono::milliseconds{100 * 1000});

        Json::Value connect;
        connect["ssid"] = "MobDevInternet";
        connect["password"] = "abracadabra";
        connect["xtoken_code"] = "some_code";
        connect["plain"] = true;
        HttpClient::HttpResponse response = client.post("testreq", baseUrl + "/connect", jsonToString(connect));
        UNIT_ASSERT_VALUES_EQUAL(response.responseCode, 200);
        Json::Value body = parseJson(response.body);

        UNIT_ASSERT_VALUES_EQUAL(body["status"].asString(), "error");
        UNIT_ASSERT_VALUES_EQUAL(body["error_code"].asInt(), 5);
        UNIT_ASSERT_VALUES_EQUAL(body["data"].asString(), "CODE_EXPIRED");
    }

    Y_UNIT_TEST_F(testFirstRunEndpointMultipleConnect, Fixture) {
        std::atomic_int wifiMessageReceived{0};
        const std::string password = "abracadabra";
        const std::string ssid1 = "ssid1";
        const std::string ssid2 = "ssid2";
        std::promise<void> expectedMessagesReceived;
        setWifiMessageHandler([&](const ipc::SharedMessage& sharedRequest, auto& connection) {
            const auto& request = *sharedRequest;
            QuasarMessage response;
            response.set_request_id(request.request_id());
            if (0 == wifiMessageReceived) {
                UNIT_ASSERT(request.has_wifi_networks_disable());
            } else if (1 == wifiMessageReceived) {
                UNIT_ASSERT(request.has_wifi_connect());
                UNIT_ASSERT_VALUES_EQUAL(request.wifi_connect().wifi_id(), ssid1);
                response.mutable_wifi_connect_response()->set_status(WifiConnectResponse::AUTH_ERROR);
            } else if (2 == wifiMessageReceived) {
                UNIT_ASSERT(request.has_wifi_connect());
                UNIT_ASSERT_VALUES_EQUAL(request.wifi_connect().wifi_id(), ssid2);
                response.mutable_wifi_connect_response()->set_status(WifiConnectResponse::OK);
            } else if (3 == wifiMessageReceived) {
                UNIT_ASSERT(request.has_wifi_networks_enable());
                expectedMessagesReceived.set_value();
            } else {
                UNIT_FAIL("wifiMessageReceived = " + std::to_string(wifiMessageReceived));
            }

            ++wifiMessageReceived;
            connection.send(std::move(response));
        });

        mockUpdatesProvider->setConfirmUpdateApply([&] {});
        mockUpdatesProvider->setCheckUpdates([&] {});
        mockAuthProvider->setAddUser(
            [&](const std::string& /*authCode*/, UserType /*userType*/, bool /*withXToken*/, std::chrono::milliseconds /*timeout*/) {
                IAuthProvider::AddUserResponse response;
                response.status = IAuthProvider::AddUserResponse::Status::OK;
                response.authToken = "the_auth_token";
                response.id = 424242;
                return response;
            });
        mockAuthProvider->setChangeUser(
            [&](int64_t id, std::chrono::milliseconds /*timeout*/) {
                UNIT_ASSERT_VALUES_EQUAL(id, 424242);
                return IAuthProvider::ChangeUserResponse{
                    .status = IAuthProvider::ChangeUserResponse::Status::OK,
                };
            });

        mockBackend.onHandlePayload = [&](const TestHttpServer::Headers& /*headers*/,
                                          const std::string& /*payload*/, TestHttpServer::HttpConnection& handler) {
            handler.doReplay(200, "application/json", "{\"status\":\"ok\"}");
        };

        /* Set up default authd callback with empty startup_info */
        setSendStartUpInfoOnStart();
        /* Create endpoint and start actual test */
        start(false);

        HttpClient client("test", getDeviceForTests());
        client.setTimeout(std::chrono::milliseconds{100 * 1000});

        Json::Value connect;
        connect["ssid"][0] = ssid1;
        connect["ssid"][1] = ssid2;
        connect["password"] = password;
        connect["xtoken_code"] = "xtoken_code";
        connect["plain"] = true;
        auto response = client.post("testreq", baseUrl + "/connect", jsonToString(connect));
        // Since there's an async message exchange going on between firstrund and wifid
        expectedMessagesReceived.get_future().get();

        UNIT_ASSERT_VALUES_EQUAL(response.responseCode, 200);
        const auto body = parseJson(response.body);
        UNIT_ASSERT_VALUES_EQUAL(body["status"].asString(), "ok");
        UNIT_ASSERT_VALUES_EQUAL(wifiMessageReceived.load(), 4);
    }

    Y_UNIT_TEST_F(testFirstRunEndpointConnectError, AutoFixture) {
        setWifiMessageHandler([&](const ipc::SharedMessage& request, auto& connection) {
            QuasarMessage response;
            response.set_request_id(request->request_id());
            response.mutable_wifi_connect_response()->set_status(WifiConnectResponse::AUTH_ERROR);
            connection.send(std::move(response));
        });

        HttpClient client("test", getDeviceForTests());
        Json::Value connect;
        connect["ssid"] = "myquasar";
        connect["password"] = "password";
        connect["xtoken_code"] = "12345";
        connect["plain"] = true;
        auto response = client.post("testreq", baseUrl + "/connect", jsonToString(connect));
        Json::Value body = parseJson(response.body);
        UNIT_ASSERT_VALUES_EQUAL(body["status"].asString(), "error");
        UNIT_ASSERT_VALUES_EQUAL(body["data"].asString(), "WIFI_AUTH_ERROR");
        UNIT_ASSERT_VALUES_EQUAL(body["error_code"].asInt(), 2);
    }

    Y_UNIT_TEST_F(testFirstRunEndpointUninitializedStartup, Fixture) {
        mockAuthProvider->setOwner(AuthInfo2{});
        setWifiMessageHandler([&](const ipc::SharedMessage& request, auto& connection) {
            QuasarMessage response;
            response.set_request_id(request->request_id());
            connection.send(std::move(response));
        });

        /* Create endpoint but do not set up default mockAuthd and mockWifid callbacks */
        start(false);

        std::promise<void> messagePromise;
        auto connector = createIpcConnectorForTests("firstrund");
        connector->setMessageHandler([&](const auto& message) {
            if (message->has_configuration_state() &&
                message->configuration_state() == proto::ConfigurationState::CONFIGURING) {
                messagePromise.set_value();
            }
        });
        connector->connectToService();

        messagePromise.get_future().get();
        /* Make sure that accessPoint starts in Configuration Mode */
        TestUtils::waitUntil([this]() {
            return accessPoint->isStarted();
        });
    }

    Y_UNIT_TEST_F(testWifiTypeBug, AutoFixture) {
        std::promise<QuasarMessage> connectMessagePromise;
        setWifiMessageHandler([&](const ipc::SharedMessage& request, auto& connection) {
            QuasarMessage response;
            response.set_request_id(request->request_id());
            connection.send(std::move(response));
            if (request->has_wifi_connect()) {
                connectMessagePromise.set_value(*request);
            }
        });
        Json::Value connect;
        connect["ssid"] = "MobDevInternet";
        connect["password"] = "111";
        connect["xtoken_code"] = "2222";
        connect["plain"] = true;
        HttpClient client("test", getDeviceForTests());
        client.setTimeout(std::chrono::milliseconds{100 * 1000});
        auto response = client.post("testreq", baseUrl + "/connect", jsonToString(connect));
        auto connectMessage = connectMessagePromise.get_future().get();
        UNIT_ASSERT_EQUAL(connectMessage.wifi_connect().wifi_type(), WifiType::UNKNOWN_WIFI_TYPE);
    }

    Y_UNIT_TEST_F(testWifiMonitorDeadlock, Fixture) {
        setWifiMessageHandler([&](const ipc::SharedMessage& request, auto& connection) {
            QuasarMessage response;
            response.set_request_id(request->request_id());
            connection.send(std::move(response));
        });

        mockNetworkd->setClientConnectedHandler([&](auto& connection) {
            QuasarMessage message;
            message.mutable_network_status()->set_status(proto::NetworkStatus::NOT_CONNECTED);
            connection.send(std::move(message));
        });

        /* Create endpoint but do not set up default mockWifid callbacks */
        start(false);

        Json::Value connect;
        connect["ssid"] = "MobDevInternet";
        connect["password"] = "111";
        connect["xtoken_code"] = "2222";
        connect["plain"] = true;
        HttpClient client("test", getDeviceForTests());
        client.setTimeout(std::chrono::milliseconds{100 * 1000});
        auto response = client.post("testreq", baseUrl + "/connect", jsonToString(connect));
    }

    Y_UNIT_TEST_F(testFirstGreeting, Fixture) {
        setWifiMessageHandler([](const ipc::SharedMessage& request, auto& connection) {
            QuasarMessage response;
            response.set_request_id(request->request_id());
            response.mutable_wifi_connect_response()->set_status(WifiConnectResponse::OK);
            connection.send(std::move(response));
        });

        /* File register is set up after successful device configuration. But first_greeting_done
         * is set up only when device is set up AND downloaded first crit (if there is one).
         * So this test check that after first crit OTA FirstRunEndpoint will play "configure_success.wav"
         * and will save first_greeting_done flag
         */
        registeredFilename.Touch();
        firstGreetingFilename.ForceDelete();
        mockUpdatesProvider->setWaitUpdateState(
            [&]() {
                return UpdatesState2::Critical::NO;
            });
        mockUpdatesProvider->setConfirmUpdateApply([&] {});
        mockUpdatesProvider->setCheckUpdates([&] {});
        /* Create endpoint, but use custom callbacks and
         * Do not wait CONFIGURING mode because Firstrund should setup CONFIGURED mode
         */
        start(false /* use default callbacks */, false /* wait configuring mode */);

        std::promise<void> configurationStatePromise;
        auto connector = createIpcConnectorForTests("firstrund");
        connector->setMessageHandler([&](const auto& message) {
            if (message->has_configuration_state() && message->configuration_state() == proto::ConfigurationState::CONFIGURED) {
                configurationStatePromise.set_value();
            }
        });
        connector->connectToService();

        /* FirstrunEndpoint should send check_updates message, so updatesd will visit check_updates asap */
        configurationStatePromise.get_future().get();
        TestUtils::waitUntil([this]() {
            return quasarDirPath.Exists();
        });
    }

    Y_UNIT_TEST_F(testFirstrunReauthsAtIOSDK, Fixture) {
        const std::string oauthCode = "testOauthCode";
        const std::string wrongOauthCode = "testWrongOauthCode";
        const std::string oauthToken = "testOauthToken";
        const std::string wrongOauthToken = "testWrongOauthToken";

        mockUpdatesProvider->setConfirmUpdateApply([&] {});
        mockUpdatesProvider->setCheckUpdates([&] {});
        mockAuthProvider->setAddUser(
            [&](const std::string& authCode, UserType /*userType*/, bool /*withXToken*/, std::chrono::milliseconds /*timeout*/) {
                IAuthProvider::AddUserResponse response;
                response.status = IAuthProvider::AddUserResponse::Status::OK;
                if (oauthCode == authCode) {
                    response.authToken = oauthToken;
                } else {
                    response.authToken = wrongOauthToken;
                }
                response.xToken = oauthCode;
                response.id = 142857;
                return response;
            });
        mockAuthProvider->setChangeUser(
            [&](int64_t id, std::chrono::milliseconds /*timeout*/) {
                UNIT_ASSERT_VALUES_EQUAL(id, 142857);
                return IAuthProvider::ChangeUserResponse{
                    .status = IAuthProvider::ChangeUserResponse::Status::OK,
                };
            });

        mockBackend.onHandlePayload = [&](const TestHttpServer::Headers& header,
                                          const std::string& /*payload*/, TestHttpServer::HttpConnection& handler) {
            UNIT_ASSERT_VALUES_EQUAL(header.resource, "/register");
            UNIT_ASSERT_VALUES_EQUAL(header.queryParams.getValue("device_id"), getDeviceForTests()->deviceId());
            UNIT_ASSERT_VALUES_EQUAL(header.queryParams.getValue("name"), "quasar");
            UNIT_ASSERT_VALUES_EQUAL(header.queryParams.getValue("platform"), "yandexstation");
            if (header.getHeader("authorization") == "OAuth " + oauthToken) {
                handler.doReplay(200, "application/json", "{\"status\":\"ok\"}");
            } else {
                handler.doReplay(400, "application/json", "{\"status\":\"not ok\"}");
            }
        };
        start();

        auto authObserver = std::make_shared<TestAuthObserver>();
        std::pair<std::string, bool> results;
        ioSDK_->setMessageHandler([authObserver](const auto& msg, auto& /*connection*/) {
            if (msg->has_io_event() && msg->io_event().has_authentication_status()) {
                const auto& status = msg->io_event().authentication_status();
                authObserver->onAuthenticationStatus(status.oauth_code(), status.is_ok(), status.response());
            }
        });
        ioSDK_->authenticate(oauthCode);
        auto isReceived = authObserver->lastTryResults(std::chrono::seconds(10), results);
        UNIT_ASSERT(isReceived);
        UNIT_ASSERT_VALUES_EQUAL(results.first, oauthCode);
        UNIT_ASSERT(results.second);

        ioSDK_->authenticate(wrongOauthCode);
        isReceived = authObserver->lastTryResults(std::chrono::seconds(10), results);
        UNIT_ASSERT(isReceived);
        UNIT_ASSERT_VALUES_EQUAL(results.first, wrongOauthCode);
        UNIT_ASSERT(!results.second);
    }

    Y_UNIT_TEST_F(testSDKAuthStopsInitMode, Fixture) {
        const std::string oauthCode = "testOauthCode";
        const std::string oauthToken = "testOauthToken";

        mockUpdatesProvider->setConfirmUpdateApply([&] {});
        mockUpdatesProvider->setCheckUpdates([&] {});

        mockAuthProvider->setAddUser(
            [&](const std::string& /*authCode*/, UserType /*userType*/, bool /*withXToken*/, std::chrono::milliseconds /*timeout*/) {
                IAuthProvider::AddUserResponse response;
                response.status = IAuthProvider::AddUserResponse::Status::OK;
                response.authToken = oauthToken;
                response.xToken = oauthCode;
                response.id = 142857;
                return response;
            });
        mockAuthProvider->setChangeUser(
            [&](int64_t id, std::chrono::milliseconds /*timeout*/) {
                auto authInfo = *mockAuthProvider->ownerAuthInfo().value();
                authInfo.passportUid = std::to_string(id);
                mockAuthProvider->setOwner(authInfo);
                return IAuthProvider::ChangeUserResponse{
                    .status = IAuthProvider::ChangeUserResponse::Status::OK,
                };
            });

        mockBackend.onHandlePayload = [&](const TestHttpServer::Headers& header,
                                          const std::string& /*payload*/, TestHttpServer::HttpConnection& handler) {
            UNIT_ASSERT_VALUES_EQUAL(header.resource, "/register");
            UNIT_ASSERT_VALUES_EQUAL(header.queryParams.getValue("device_id"), getDeviceForTests()->deviceId());
            UNIT_ASSERT_VALUES_EQUAL(header.queryParams.getValue("name"), "quasar");
            UNIT_ASSERT_VALUES_EQUAL(header.queryParams.getValue("platform"), "yandexstation");
            handler.doReplay(200, "application/json", "{\"status\":\"ok\"}");
        };
        start();

        // Make sure that calling authenticate stops setup mode
        ioSDK_->authenticate(oauthCode);
        TestUtils::waitUntil([this]() {
            return !endpoint->isConfigurationMode();
        });
        UNIT_ASSERT(!endpoint->isConfigurationMode());
        mockBackend.stop();
    }

    Y_UNIT_TEST_F(testFirstrundSendCheckUpdates, Fixture) {
        /* Set up env for successfull Configuring */
        mockBackend.onHandlePayload = [&](const TestHttpServer::Headers& /*headers*/,
                                          const std::string& /*payload*/, TestHttpServer::HttpConnection& handler) {
            handler.doReplay(200, "application/json", "{\"status\":\"ok\"}");
        };

        mockUpdatesProvider->setConfirmUpdateApply([&] {});
        mockUpdatesProvider->setCheckUpdates([&] {});
        mockAuthProvider->setAddUser(
            [&](const std::string& /*authCode*/, UserType /*userType*/, bool /*withXToken*/, std::chrono::milliseconds /*timeout*/) {
                IAuthProvider::AddUserResponse response;
                response.status = IAuthProvider::AddUserResponse::Status::OK;
                response.authToken = "oauthToken";
                response.xToken = "xtoken";
                response.id = 142857;
                return response;
            });
        mockAuthProvider->setChangeUser(
            [&](int64_t id, std::chrono::milliseconds /*timeout*/) {
                auto authInfo = *mockAuthProvider->ownerAuthInfo().value();
                authInfo.passportUid = std::to_string(id);
                mockAuthProvider->setOwner(authInfo);
                return IAuthProvider::ChangeUserResponse{
                    .status = IAuthProvider::ChangeUserResponse::Status::OK,
                };
            });

        /* Set up actual test handler: Check that Firstrund send check_updates message when enter CONFIGURED mode */
        start();

        /* Start Configuring */
        HttpClient client("test", getDeviceForTests());
        client.setTimeout(std::chrono::milliseconds{100 * 1000});
        Json::Value connect;
        connect["ssid"] = "MobDevInternet";
        connect["password"] = "abracadabra";
        connect["xtoken_code"] = "some_code";
        connect["plain"] = true;
        const HttpClient::HttpResponse response = client.post("testreq", baseUrl + "/connect", jsonToString(connect));
        /* check that firstrund completed configuration */
        UNIT_ASSERT_VALUES_EQUAL(response.responseCode, 200);

        /* Make sure that it is actually configured */
        TestUtils::waitUntil([this]() {
            return !endpoint->isConfigurationMode();
        });
    }

    Y_UNIT_TEST_F(testFirstrundStartsLedAnimationOnSetupStart, AutoFixture) {
        auto wifiConnectingPromise = std::make_shared<std::promise<void>>();

        ioSDK_->setMessageHandler([weakPromise{std::weak_ptr<std::promise<void>>(wifiConnectingPromise)}](const auto& msg, auto& /*connection*/) {
            if (msg->has_io_event() && msg->io_event().has_on_connecting_to_network()) {
                if (auto promise = weakPromise.lock()) {
                    promise->set_value();
                }
            }
        });

        setWifiMessageHandler([&](const auto& request, auto& connection) {
            QuasarMessage response;
            response.set_request_id(request->request_id());
            connection.send(std::move(response));
        });

        /* Start Configuring */
        HttpClient client("test", getDeviceForTests());
        client.setTimeout(std::chrono::milliseconds{100 * 1000});
        Json::Value connect;
        connect["ssid"] = "MobDevInternet";
        connect["password"] = "abracadabra";
        connect["xtoken_code"] = "some_code";
        connect["plain"] = true;
        client.post("testreq", baseUrl + "/connect", jsonToString(connect));

        wifiConnectingPromise->get_future().get();
    }

    Y_UNIT_TEST_F(testFirstRunEndpointFullCycleViaEthernet, AutoFixture) {
        std::atomic<int> addUserRequestReceived(0);
        std::atomic<int> changeUserRequestReceived(0);
        std::atomic<int> wifiConnectRequestReceived(0);
        const std::string password = "abracadabra";

        mockUpdatesProvider->setConfirmUpdateApply([&] {});
        mockUpdatesProvider->setCheckUpdates([&] {});
        mockAuthProvider->setAddUser(
            [&](const std::string& authCode, UserType /*userType*/, bool /*withXToken*/, std::chrono::milliseconds /*timeout*/) {
                UNIT_ASSERT_VALUES_EQUAL(authCode, "the_x_token_code");

                ++addUserRequestReceived;
                IAuthProvider::AddUserResponse response;
                response.status = IAuthProvider::AddUserResponse::Status::OK;
                response.authToken = "the_auth_token";
                response.id = 424242;
                return response;
            });
        mockAuthProvider->setChangeUser(
            [&](int64_t id, std::chrono::milliseconds /*timeout*/) {
                UNIT_ASSERT_VALUES_EQUAL(id, 424242);
                ++changeUserRequestReceived;
                return IAuthProvider::ChangeUserResponse{
                    .status = IAuthProvider::ChangeUserResponse::Status::OK,
                };
            });

        setWifiMessageHandler([&](const auto& request, auto& connection) {
            QuasarMessage response;
            response.set_request_id(request->request_id());
            connection.send(std::move(response));
            if (request->has_wifi_connect()) {
                wifiConnectRequestReceived++;
            }
        });

        std::atomic<bool> registerReceived(false);
        mockBackend.onHandlePayload = [&](const TestHttpServer::Headers& header,
                                          const std::string& /*payload*/, TestHttpServer::HttpConnection& handler) {
            UNIT_ASSERT_VALUES_EQUAL(header.resource, "/register");
            UNIT_ASSERT_VALUES_EQUAL(header.getHeader("authorization"), "OAuth the_auth_token");
            UNIT_ASSERT_VALUES_EQUAL(header.getHeader("x-quasar-signature-version"), "2");
            UNIT_ASSERT_VALUES_EQUAL(header.queryParams.getValue("device_id"), getDeviceForTests()->deviceId());
            UNIT_ASSERT_VALUES_EQUAL(header.queryParams.getValue("name"), "quasar");
            UNIT_ASSERT_VALUES_EQUAL(header.queryParams.getValue("platform"), "yandexstation");
            UNIT_ASSERT_VALUES_EQUAL(header.queryParams.getValue("activation_code"), std::to_string(getCrc32("the_x_token_code")));

            Cryptography crypto;
            crypto.loadPublicKey(ArcadiaSourceRoot() + "/yandex_io/misc/cryptography/public.pem");

            UNIT_ASSERT(crypto.checkSignature(header.query, base64Decode(urlDecode(header.getHeader("x-quasar-signature")))));

            registerReceived = true;
            handler.doReplay(200, "application/json", "{\"status\":\"ok\"}");
        };

        HttpClient client("test", getDeviceForTests());
        client.setTimeout(std::chrono::milliseconds{100 * 1000});

        auto response = client.get("testreq", baseUrl + "/info");
        Json::Value info = parseJson(response.body);
        Cryptography cryptography;
        cryptography.setPublicKey(getString(info, "crypto_key"));

        Json::Value connect;
        std::string encrypted = cryptography.encrypt("the_x_token_code");
        connect["xtoken_code"] = base64Encode(encrypted.c_str(), encrypted.length());
        connect["wifi_type"] = "NONE";
        response = client.post("testreq", baseUrl + "/connect", jsonToString(connect));

        UNIT_ASSERT_VALUES_EQUAL(response.responseCode, 200);
        auto body = parseJson(response.body);
        UNIT_ASSERT_VALUES_EQUAL(body["status"].asString(), "ok");
        UNIT_ASSERT_VALUES_EQUAL(addUserRequestReceived.load(), 1);
        UNIT_ASSERT_VALUES_EQUAL(changeUserRequestReceived.load(), 1);
        UNIT_ASSERT(registerReceived);
        UNIT_ASSERT_VALUES_EQUAL(wifiConnectRequestReceived.load(), 0);
    }

}

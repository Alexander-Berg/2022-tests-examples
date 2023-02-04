#include <yandex_io/services/syncd/sync_endpoint.h>

#include <yandex_io/interfaces/auth/mock/auth_provider.h>
#include <yandex_io/interfaces/device_state/mock/device_state_provider.h>
#include <yandex_io/libs/base/crc32.h>
#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/ipc/i_connector.h>
#include <yandex_io/libs/ipc/i_server.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/sdk/backend_config_observer.h>
#include <yandex_io/tests/testlib/test_http_server.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/telemetry_test_fixture.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <util/folder/path.h>

#include <fstream>
#include <future>
#include <memory>
#include <string>

#include <fcntl.h>

using namespace quasar;
using namespace quasar::TestUtils;
using namespace quasar::proto;

namespace {

    constexpr std::chrono::milliseconds MAX_UPDATE_PERIOD_TEST = std::chrono::seconds(120);
    constexpr std::chrono::milliseconds MIN_UPDATE_PERIOD_TEST = std::chrono::seconds(1);

    class BackoffRetriesMock: public IBackoffRetries {
    public:
        MOCK_METHOD(void, initCheckPeriod, (const std::chrono::milliseconds, const std::chrono::milliseconds, const std::optional<std::chrono::milliseconds>, const std::optional<std::chrono::milliseconds>), (override));
        MOCK_METHOD(void, updateCheckPeriod, (const std::optional<std::chrono::milliseconds>, const std::optional<std::chrono::milliseconds>), (override));

        MOCK_METHOD(std::chrono::milliseconds, calcHttpClientRetriesDelay, (const int), (override));
        MOCK_METHOD(std::chrono::milliseconds, getDelayBetweenCalls, (), (override));
        MOCK_METHOD(void, resetDelayBetweenCallsToDefault, (), (override));
        MOCK_METHOD(void, spedUpDelayBetweenCalls, (), (override));
        MOCK_METHOD(void, increaseDelayBetweenCalls, (), (override));
    };

    class MockAccountDevices: public glagol::IAccountDevices {
        IDeviceListChangedSignal& deviceListChangedSignal_;
        std::function<void()> onScheduleUpdate_;

    public:
        MockAccountDevices(IDeviceListChangedSignal& deviceListChangedSignal,
                           std::function<void()> onScheduleUpdate)
            : deviceListChangedSignal_(deviceListChangedSignal)
            , onScheduleUpdate_(std::move(onScheduleUpdate))
        {
        }

        using BackendApi = glagol::BackendApi;

        static BackendApi::DevicesMap mockDevices() {
            BackendApi::DevicesMap result;
            glagol::DeviceId id;
            id.id = "1";
            id.platform = "platform";
            auto& device = result[id];
            device.name = "name";
            return result;
        }

        BackendApi::DevicesMap devices() const noexcept override {
            return mockDevices();
        }

        IDeviceListChangedSignal& deviceListChangedSignal() noexcept override {
            return deviceListChangedSignal_;
        }

        bool setSettings(const BackendApi::Settings& settings) override {
            return !settings.token.empty();
        }

        bool resumeUpdate() noexcept override {
            return true;
        };

        bool scheduleUpdate() noexcept override {
            if (onScheduleUpdate_) {
                onScheduleUpdate_();
            }
            return true;
        };
    };

    std::unique_ptr<quasar::SignalExternal<glagol::IAccountDevices::IDeviceListChangedSignal>> makeMockAccountDevicesSignal(quasar::Lifetime& lifetime) {
        return std::make_unique<quasar::SignalExternal<glagol::IAccountDevices::IDeviceListChangedSignal>>(
            [](bool /*onConnected*/) {
                return MockAccountDevices::mockDevices();
            },
            lifetime);
    }

    class IOHubWrapper {
    public:
        IOHubWrapper(std::shared_ptr<ipc::IIpcFactory> ipcFactory)
            : server_(ipcFactory->createIpcServer("iohub_services"))
        {
        }

        void start() {
            server_->listenService();
        }

        void setMessageHandler(std::function<void(const quasar::ipc::SharedMessage& request, quasar::ipc::IServer::IClientConnection& connection)> handler) {
            server_->setMessageHandler(std::move(handler));
        }

        void subscribeToDeviceConfig(const TString& config) {
            server_->setClientConnectedHandler([config](auto& connection) {
                proto::QuasarMessage message;
                message.mutable_io_control()->set_subscribe_to_device_config(config);
                connection.send(std::move(message));
            });
        }

        void subscribeToSystemConfig(const TString& config) {
            server_->setClientConnectedHandler([config](auto& connection) {
                proto::QuasarMessage message;
                message.mutable_io_control()->set_subscribe_to_system_config(config);
                connection.send(std::move(message));
            });
        }

        void subscribeToAccountConfig(const TString& config) {
            server_->setClientConnectedHandler([config](auto& connection) {
                proto::QuasarMessage message;
                message.mutable_io_control()->set_subscribe_to_account_config(config);
                connection.send(std::move(message));
            });
        }

        void waitConnection() {
            server_->waitConnectionsAtLeast(1);
        }

    private:
        std::shared_ptr<quasar::ipc::IServer> server_;
    };

    struct BaseFixture: public virtual QuasarUnitTestFixture {
        using Base = QuasarUnitTestFixture;

        std::unique_ptr<IOHubWrapper> ioHub_;

        TFsPath quasarDirPath;
        TFsPath stubConfigStoragePath;
        std::string authToken = "123";

        Json::Value getConfigResponse() {
            Json::Value response;

            response["status"] = "ok";
            response["subscription"]["mode"] = "transaction";

            response["config"]["account_config"]["spotter"] = "alisa";
            response["config"]["device_config"]["name"] = "test_value";
            response["config"]["system_config"] = Json::nullValue;
            if (sendSystemConfigFoo.load()) {
                response["config"]["system_config"]["foo"] = "bar";
            }

            if (hasGroup) {
                response["group"]["id"] = 1;
                response["group"]["name"] = "groupname";
                response["group"]["secret"] = "s3cr3t";

                response["group"]["devices"][0]["id"] = "12345";
                response["group"]["devices"][0]["platform"] = "yandexmodule";
                response["group"]["devices"][0]["role"] = "follower";

                response["group"]["devices"][1]["id"] = "123456";
                response["group"]["devices"][1]["platform"] = "yandexstation";
                response["group"]["devices"][1]["role"] = "leader";
            }
            return response;
        }

        quasar::Lifetime devicesSignalLifetime;
        std::unique_ptr<quasar::SignalExternal<glagol::IAccountDevices::IDeviceListChangedSignal>> devicesSignal;
        std::function<void()> onDevicesScheduleUpdate;

        auto makeAccountDevicesMaker() {
            return [this](std::shared_ptr<glagol::BackendApi> /*backendApi*/, std::string /*accountDeviceCacheFile*/) {
                return std::make_unique<MockAccountDevices>(*devicesSignal, onDevicesScheduleUpdate);
            };
        }

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);
            SetUpInternal();
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            TearDownInternal();
            Base::TearDown(context);
        }

        void SetUpInternal() {
            devicesSignal = makeMockAccountDevicesSignal(devicesSignalLifetime);
            quasarDirPath = JoinFsPaths(tryGetRamDrivePath(), "quasarDir-" + makeUUID());
            stubConfigStoragePath = JoinFsPaths(quasarDirPath, "syncd_stub_storage");
            quasarDirPath.ForceDelete();
            quasarDirPath.MkDirs();

            const int backendPort = mockBackend.start(getPort());

            auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
            config["common"]["backendUrl"] = "http://localhost:" + std::to_string(backendPort);
            config["syncd"]["configPath"] = stubConfigStoragePath.GetPath();

            /* Prepare iosdk hub port, so it DeviceContext will connect to right server */
            ioHub_ = std::make_unique<IOHubWrapper>(ipcFactoryForTests());

            // simulate get_sync_info response from backend
            mockBackend.onHandlePayload = [&](const TestHttpServer::Headers& header, const std::string& /*payload*/, TestHttpServer::HttpConnection& handler) {
                UNIT_ASSERT_VALUES_EQUAL(header.verb, "GET");
                UNIT_ASSERT_VALUES_EQUAL(header.resource, "/get_sync_info");
                UNIT_ASSERT_VALUES_EQUAL(header.getHeader("authorization"), "OAuth " + authToken);
                UNIT_ASSERT_VALUES_EQUAL(header.queryParams.getValue("device_id"), getDeviceForTests()->deviceId());
                UNIT_ASSERT_VALUES_EQUAL(header.queryParams.getValue("platform"), getDeviceForTests()->configuration()->getDeviceType());

                Json::Value response = getConfigResponse();

                handler.doReplay(200, "application/json", jsonToString(response));
                YIO_LOG_INFO("mockBackend send");
            };

            mockAuthProvider = std::make_shared<mock::AuthProvider>();
            mockAuthProvider->setOwner(
                AuthInfo2{
                    .source = AuthInfo2::Source::AUTHD,
                    .authToken = authToken,
                    .passportUid = "123",
                    .tag = 1600000000,
                });
            mockDeviceStateProvider = std::make_shared<mock::DeviceStateProvider>();
            mockDeviceStateProvider->setDeviceState(mock::defaultDeviceState());
        }

        void TearDownInternal() {
            quasarDirPath.ForceDelete();
            devicesSignalLifetime.die();
        }

        YandexIO::Configuration::TestGuard testGuard;

        TestHttpServer mockBackend;

        std::atomic_bool sendSystemConfigFoo{true};
        bool hasGroup{false}; // if config has a group

        std::shared_ptr<mock::AuthProvider> mockAuthProvider;
        std::shared_ptr<mock::DeviceStateProvider> mockDeviceStateProvider;
    };

    /* Fixture to tests that SyncEndpoint send metrics */
    struct SyncdTestWithMetricsFixture: public TelemetryTestFixture, public BaseFixture {
        SyncdTestWithMetricsFixture()
            : TelemetryTestFixture()
            ,             /* Init Yandex::Device with Telemetry for metrics tests */
            BaseFixture() /* Init base test environment */
        {
        }

        void SetUp(NUnitTest::TTestContext& context) override {
            TelemetryTestFixture::SetUp(context);
            BaseFixture::SetUpInternal();
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            BaseFixture::TearDownInternal();
            TelemetryTestFixture::TearDown(context);
        }
    };

    class TestBackendConfigObserver: public YandexIO::BackendConfigObserver {
    public:
        void onSystemConfig(const std::string& configName, const std::string& jsonConfigValue) override {
            onConfig(configName, jsonConfigValue);
        }

        void onDeviceConfig(const std::string& configName, const std::string& jsonConfigValue) override {
            onConfig(configName, jsonConfigValue);
        }

        void onAccountConfig(const std::string& configName, const std::string& jsonConfigValue) override {
            onConfig(configName, jsonConfigValue);
        }

        void setConfigName(std::string name) {
            std::scoped_lock guard(mutex_);
            configName_ = std::move(name);
        }

        void waitReceived(const std::string& value) {
            std::unique_lock<std::mutex> lock(mutex_);
            YIO_LOG_INFO("Wait for value: " << value);
            CV_.wait(lock, [&]() { return jsonToString(value_) == value; });
        }

    private:
        void onConfig(const std::string& configName, const std::string& jsonConfigValue) {
            std::lock_guard<std::mutex> guard(mutex_);
            YIO_LOG_INFO("OnBackendConfig: " << configName << " value: " << jsonConfigValue);
            if (configName == configName_) {
                value_ = parseJson(jsonConfigValue);
                CV_.notify_one();
            }
        }

    private:
        std::string configName_;
        Json::Value value_ = Json::nullValue;
        std::mutex mutex_;
        SteadyConditionVariable CV_;
    };

} /* anonymous namespace */

Y_UNIT_TEST_SUITE(SyncEndpointTests) {

    Y_UNIT_TEST_F(shouldSendUserConfigUpdateMessagesToAll_whenStarts, BaseFixture) {
        // Arrange
        hasGroup = false;
        std::shared_ptr<BackoffRetriesWithRandomPolicy> delayTimingsPolicy = std::make_shared<BackoffRetriesWithRandomPolicy>(getCrc32(getDeviceForTests()->deviceId()) + getNowTimestampMs());

        SyncEndpoint syncEndpoint(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockDeviceStateProvider, delayTimingsPolicy, makeAccountDevicesMaker(), MAX_UPDATE_PERIOD_TEST, MIN_UPDATE_PERIOD_TEST);
        std::promise<UserConfig> messagePromise;

        auto syncdConnector = createIpcConnectorForTests("syncd");
        syncdConnector->setMessageHandler([&](const auto& message) {
            if (message->has_user_config_update()) {
                messagePromise.set_value(message->user_config_update());
            }
        });

        // Act
        syncdConnector->connectToService();
        syncdConnector->waitUntilConnected();

        auto future = messagePromise.get_future();

        // Assert
        auto config = parseJson(future.get().config());
        UNIT_ASSERT_VALUES_EQUAL(config["device_config"]["name"].asString(), "test_value");
    }

    Y_UNIT_TEST_F(shouldSendActualConfigInFirstMessage_whenActualConfigReceived, BaseFixture) {
        // Arrange
        std::shared_ptr<BackoffRetriesWithRandomPolicy> delayTimingsPolicy = std::make_shared<BackoffRetriesWithRandomPolicy>(getCrc32(getDeviceForTests()->deviceId()) + getNowTimestampMs());

        std::promise<UserConfig> currentConfigPromise;
        auto syncdConnector = createIpcConnectorForTests("syncd");

        syncdConnector->setMessageHandler([&currentConfigPromise, promiseSent = false](const auto& message) mutable {
            if (!promiseSent) {
                if (message->has_user_config_update()) {
                    currentConfigPromise.set_value(message->user_config_update());
                } else if (message->has_cached_user_config()) {
                    currentConfigPromise.set_value(message->cached_user_config());
                }
                promiseSent = true;
            }
        });

        auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
        config["syncd"]["configPath"] = stubConfigStoragePath.GetPath();

        quasar::proto::ConfigStorage configStorage;
        configStorage.set_current_passport_uid("42");
        auto userConfig = configStorage.mutable_user_config()->Add();
        userConfig->set_passport_uid("42");
        userConfig->set_config("{\"device_config\":{\"name\": \"cached_value\"}}\n");
        userConfig->set_group_config("{}\n");

        std::ofstream storage(config["syncd"]["configPath"].asString());
        storage << configStorage.SerializeAsString();
        storage.close();

        // Act
        SyncEndpoint syncEndpoint(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockDeviceStateProvider, delayTimingsPolicy, makeAccountDevicesMaker(), MAX_UPDATE_PERIOD_TEST, MIN_UPDATE_PERIOD_TEST);
        TestUtils::waitUntil([&syncEndpoint]() {
            return syncEndpoint.wasSyncReceived();
        });

        syncdConnector->connectToService();
        syncdConnector->waitUntilConnected();

        auto future = currentConfigPromise.get_future();

        // Assert
        auto receivedConfig = parseJson(future.get().config());
        UNIT_ASSERT_VALUES_EQUAL(receivedConfig["device_config"]["name"].asString(), "test_value");
    }

    Y_UNIT_TEST_F(shouldSendCachedConfigInFirstMessage_whenActualConfigIsNotReceivedYet, BaseFixture) {
        // Arrange
        std::shared_ptr<BackoffRetriesWithRandomPolicy> delayTimingsPolicy = std::make_shared<BackoffRetriesWithRandomPolicy>(getCrc32(getDeviceForTests()->deviceId()) + getNowTimestampMs());

        std::promise<UserConfig> currentConfigPromise;
        auto syncdConnector = createIpcConnectorForTests("syncd");
        syncdConnector->setMessageHandler([&currentConfigPromise, promiseSent = false](const auto& message) mutable {
            if (!promiseSent) {
                if (message->has_user_config_update()) {
                    currentConfigPromise.set_value(message->user_config_update());
                } else if (message->has_cached_user_config()) {
                    currentConfigPromise.set_value(message->cached_user_config());
                }
                promiseSent = true;
            }
        });

        auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
        config["syncd"]["configPath"] = stubConfigStoragePath.GetPath();

        quasar::proto::ConfigStorage configStorage;
        configStorage.set_current_passport_uid("42");
        auto userConfig = configStorage.mutable_user_config()->Add();
        userConfig->set_passport_uid("42");
        userConfig->set_config("{\"device_config\":{\"name\": \"cached_value\"}}\n");
        userConfig->set_group_config("{}\n");

        std::ofstream storage(config["syncd"]["configPath"].asString());
        storage << configStorage.SerializeAsString();
        storage.close();

        std::promise<void> configRequestAcceptedPromise;
        mockBackend.onHandlePayload = [&](const TestHttpServer::Headers& /*headers*/, const std::string& /*payload*/, TestHttpServer::HttpConnection& handler) {
            // we should wait until first request for config is accepted at
            // SyncEndpoint
            configRequestAcceptedPromise.get_future().wait();
            Json::Value response;
            handler.doReplay(200, "application/json", jsonToString(response));
        };

        // Act
        SyncEndpoint syncEndpoint(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockDeviceStateProvider, delayTimingsPolicy, makeAccountDevicesMaker(), MAX_UPDATE_PERIOD_TEST, MIN_UPDATE_PERIOD_TEST);
        syncdConnector->connectToService();
        syncdConnector->waitUntilConnected();

        auto future = currentConfigPromise.get_future();
        future.wait();
        configRequestAcceptedPromise.set_value();

        // Assert
        auto receivedConfig = parseJson(future.get().config());
        UNIT_ASSERT_VALUES_EQUAL(receivedConfig["device_config"]["name"].asString(), "cached_value");
    }

    Y_UNIT_TEST_F(shouldSetEnvironmentValuesTakenFromQuasmodrom_whenStarts, SyncdTestWithMetricsFixture) {
        // Arrange
        std::shared_ptr<BackoffRetriesWithRandomPolicy> delayTimingsPolicy = std::make_shared<BackoffRetriesWithRandomPolicy>(getCrc32(getDeviceForTests()->deviceId()) + getNowTimestampMs());

        auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
        std::promise<void> fstEnvVar;
        std::promise<void> sndEnvVar;

        mockBackend.onHandlePayload = [&](const TestHttpServer::Headers& /*headers*/,
                                          const std::string& /*payload*/, TestHttpServer::HttpConnection& handler) {
            Json::Value response;

            response["status"] = "ok";
            response["config"]["system_config"]["appmetrikaReportEnvironment"]["foo"] = "bar";
            response["config"]["system_config"]["appmetrikaReportEnvironment"]["test_key"] = "test_value";

            handler.doReplay(200, "application/json", jsonToString(response));
        };

        setAppEnvironmentListener([&](const std::string key, const std::string& value) {
            if (key == "foo" && value == "bar") {
                fstEnvVar.set_value();
            } else if (key == "test_key" && value == "test_value") {
                sndEnvVar.set_value();
            }
        });
        config["syncd"]["configPath"] = stubConfigStoragePath.GetPath();

        // Act
        SyncEndpoint syncEndpoint(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockDeviceStateProvider, delayTimingsPolicy, makeAccountDevicesMaker(), MAX_UPDATE_PERIOD_TEST, MIN_UPDATE_PERIOD_TEST);

        // Assert
        fstEnvVar.get_future().get();
        sndEnvVar.get_future().get();
        UNIT_ASSERT(true);
    }

    Y_UNIT_TEST_F(shouldSetEnvironmentValuesTakenFromSavedQuasmodromConfig_whenStarts, SyncdTestWithMetricsFixture) {
        // Arrange
        std::shared_ptr<BackoffRetriesWithRandomPolicy> delayTimingsPolicy = std::make_shared<BackoffRetriesWithRandomPolicy>(getCrc32(getDeviceForTests()->deviceId()) + getNowTimestampMs());

        auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
        std::promise<void> fstEnvVar;
        std::promise<void> sndEnvVar;

        setAppEnvironmentListener([&](const std::string key, const std::string& value) {
            if (key == "foo" && value == "bar") {
                fstEnvVar.set_value();
            } else if (key == "test_key" && value == "test_value") {
                sndEnvVar.set_value();
            }
        });
        config["syncd"]["configPath"] = stubConfigStoragePath.GetPath();

        quasar::proto::ConfigStorage configStorage;
        configStorage.set_current_passport_uid("123");
        auto userConfig = configStorage.mutable_user_config()->Add();
        userConfig->set_passport_uid("123");
        userConfig->set_config("{\"system_config\":{\"appmetrikaReportEnvironment\":{\"foo\":\"bar\",\"test_key\":\"test_value\"}}}\n");
        userConfig->set_group_config("{}\n");

        std::ofstream storage(config["syncd"]["configPath"].asString());
        storage << configStorage.SerializeAsString();
        storage.close();

        // Act
        SyncEndpoint syncEndpoint(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockDeviceStateProvider, delayTimingsPolicy, makeAccountDevicesMaker(), MAX_UPDATE_PERIOD_TEST, MIN_UPDATE_PERIOD_TEST);

        // Assert
        fstEnvVar.get_future().get();
        sndEnvVar.get_future().get();
        UNIT_ASSERT(true);
    }

    Y_UNIT_TEST_F(givenSdkSubscribedToConfigUpdates_itShouldReceiveConfigUpdates_whenSyncEndpointStarts, BaseFixture) {
        std::shared_ptr<BackoffRetriesWithRandomPolicy> delayTimingsPolicy = std::make_shared<BackoffRetriesWithRandomPolicy>(getCrc32(getDeviceForTests()->deviceId()) + getNowTimestampMs());
        // First we add config observer, then subscribe
        auto testBackendConfigObserver = std::make_shared<TestBackendConfigObserver>();
        testBackendConfigObserver->setConfigName("name");
        ioHub_->setMessageHandler([testBackendConfigObserver](const auto& msg, auto& /*connection*/) {
            if (msg->io_event().has_device_config()) {
                const auto& config = msg->io_event().device_config();
                testBackendConfigObserver->onDeviceConfig(config.name(), config.value());
            }
        });
        ioHub_->subscribeToDeviceConfig("name");
        ioHub_->start();
        // Act
        SyncEndpoint syncEndpoint(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockDeviceStateProvider, delayTimingsPolicy, makeAccountDevicesMaker(), MAX_UPDATE_PERIOD_TEST, MIN_UPDATE_PERIOD_TEST);
        Json::Value testValue("test_value");
        testBackendConfigObserver->waitReceived(jsonToString(testValue));
        UNIT_ASSERT(true);
    }

    Y_UNIT_TEST_F(testYandexIOSDK_systemConfig_subscribtion, BaseFixture) {
        std::shared_ptr<BackoffRetriesWithRandomPolicy> delayTimingsPolicy = std::make_shared<BackoffRetriesWithRandomPolicy>(getCrc32(getDeviceForTests()->deviceId()) + getNowTimestampMs());
        // First we add config observer, then subscribe
        std::shared_ptr<TestBackendConfigObserver> testBackendConfigObserver = std::make_shared<TestBackendConfigObserver>();
        testBackendConfigObserver->setConfigName("foo");
        ioHub_->setMessageHandler([testBackendConfigObserver](const auto& msg, auto& /*connection*/) {
            if (msg->io_event().has_backend_config()) {
                const auto& config = msg->io_event().backend_config();
                testBackendConfigObserver->onSystemConfig(config.name(), config.value());
            }
        });
        ioHub_->subscribeToSystemConfig("foo");
        ioHub_->start();

        // Act
        SyncEndpoint syncEndpoint(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockDeviceStateProvider, delayTimingsPolicy, makeAccountDevicesMaker(), MAX_UPDATE_PERIOD_TEST, MIN_UPDATE_PERIOD_TEST);
        Json::Value bar("bar");
        testBackendConfigObserver->waitReceived(jsonToString(bar));
        sendSystemConfigFoo = false;
        /* When config disappear from "system_config" should get nullValue */
        testBackendConfigObserver->waitReceived(jsonToString(Json::nullValue));

        UNIT_ASSERT(true);
    }

    Y_UNIT_TEST_F(testYandexIOSDK_accountConfig_subscription, BaseFixture) {
        std::shared_ptr<BackoffRetriesWithRandomPolicy> delayTimingsPolicy = std::make_shared<BackoffRetriesWithRandomPolicy>(getCrc32(getDeviceForTests()->deviceId()) + getNowTimestampMs());
        // First we add config observer, then subscribe
        auto testBackendConfigObserver = std::make_shared<TestBackendConfigObserver>();
        testBackendConfigObserver->setConfigName("spotter");
        ioHub_->setMessageHandler([testBackendConfigObserver](const auto& msg, auto& /*connection*/) {
            if (msg->io_event().has_account_config()) {
                const auto& config = msg->io_event().account_config();
                testBackendConfigObserver->onAccountConfig(config.name(), config.value());
            }
        });
        ioHub_->subscribeToAccountConfig("spotter");
        ioHub_->start();
        // Act
        SyncEndpoint syncEndpoint(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockDeviceStateProvider, delayTimingsPolicy, makeAccountDevicesMaker(), MAX_UPDATE_PERIOD_TEST, MIN_UPDATE_PERIOD_TEST);
        Json::Value testValue("alisa");
        testBackendConfigObserver->waitReceived(jsonToString(testValue));
        UNIT_ASSERT(true);
    }

    Y_UNIT_TEST_F(shouldSendConfigUpdateEvent_whenAuthTokenChanges, BaseFixture) {
        // Arrange
        std::atomic<bool> newAuthSeen = false;
        auto delayTimingsPolicy = std::make_shared<BackoffRetriesWithRandomPolicy>(
            getCrc32(getDeviceForTests()->deviceId()) + getNowTimestampMs());

        const std::string newToken = "new_token";

        mockBackend.onHandlePayload = [this, &newAuthSeen, &newToken](const TestHttpServer::Headers& header, const std::string& /*payload*/, TestHttpServer::HttpConnection& handler) {
            Json::Value response = getConfigResponse();
            if (header.getHeader("authorization") == "OAuth " + authToken) {
                // Make old config different to the new one
                response["config"]["device_config"]["name"] = "test_value_old";
                handler.doReplay(200, "application/json", jsonToString(response));
                return;
            }

            newAuthSeen.store(true);
            UNIT_ASSERT_VALUES_EQUAL(header.verb, "GET");
            UNIT_ASSERT_VALUES_EQUAL(header.resource, "/get_sync_info");
            UNIT_ASSERT_VALUES_EQUAL(header.getHeader("authorization"), "OAuth " + newToken);
            UNIT_ASSERT_VALUES_EQUAL(header.queryParams.getValue("device_id"), getDeviceForTests()->deviceId());
            UNIT_ASSERT_VALUES_EQUAL(header.queryParams.getValue("platform"), getDeviceForTests()->configuration()->getDeviceType());

            handler.doReplay(200, "application/json", jsonToString(response));
        };

        std::promise<bool> messagePromise;
        auto syncdConnector = createIpcConnectorForTests("syncd");
        syncdConnector->setMessageHandler([&](const auto& message) {
            if (newAuthSeen.load() && message->has_user_config_update()) {
                messagePromise.set_value(true);
            }
        });

        // Act

        SyncEndpoint syncdEndpoint(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockDeviceStateProvider, delayTimingsPolicy, makeAccountDevicesMaker(), MAX_UPDATE_PERIOD_TEST, MIN_UPDATE_PERIOD_TEST);
        syncdConnector->connectToService();
        syncdConnector->waitUntilConnected();

        auto oldAuthInfo = mockAuthProvider->ownerAuthInfo().value();
        auto newAuthInfo = *oldAuthInfo;
        newAuthInfo.authToken = newToken;
        mockAuthProvider->setOwner(newAuthInfo);

        // Assert
        messagePromise.get_future().get();
        UNIT_ASSERT(true);
    }

    Y_UNIT_TEST_F(shouldSendAccountDevicesList, BaseFixture) {
        // Arrange
        std::shared_ptr<BackoffRetriesWithRandomPolicy> delayTimingsPolicy = std::make_shared<BackoffRetriesWithRandomPolicy>(getCrc32(getDeviceForTests()->deviceId()) + getNowTimestampMs());
        SyncEndpoint syncdEndpoint(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockDeviceStateProvider, delayTimingsPolicy, makeAccountDevicesMaker(), MAX_UPDATE_PERIOD_TEST, MIN_UPDATE_PERIOD_TEST);

        { // check devices list after update
            bool configReceived = false;
            bool devicesReceived = false;
            std::promise<bool> messagePromise;
            auto syncdConnector = createIpcConnectorForTests("syncd");
            syncdConnector->setMessageHandler([&](const auto& message) {
                if (message->has_user_config_update()) {
                    configReceived = true;
                }
                if (message->has_account_devices_list()) {
                    devicesReceived = true;
                }
                if (configReceived && devicesReceived) {
                    messagePromise.set_value(true);
                }
            });

            // Act
            syncdConnector->connectToService();
            syncdConnector->waitUntilConnected();
            devicesSignal->emit();

            // Assert
            messagePromise.get_future().get();
        };

        { // check devices list in connect
            std::promise<bool> messagePromise;
            auto syncdConnector = createIpcConnectorForTests("syncd");
            syncdConnector->setMessageHandler([&](const auto& message) {
                if (message->has_account_devices_list()) {
                    messagePromise.set_value(true);
                }
            });
            syncdConnector->connectToService();
            syncdConnector->waitUntilConnected();
            messagePromise.get_future().get();
        }
        UNIT_ASSERT(true);
    }

    Y_UNIT_TEST_F(shouldUpdateAccountDevicesListOnNetworkInfoPush, BaseFixture) {
        std::atomic_int schedules{0};
        std::promise<void> schedulePromise;
        onDevicesScheduleUpdate = [&]() {
            YIO_LOG_DEBUG("Scheduled update");
            if (++schedules > 1) {
                schedulePromise.set_value();
            }
        };
        std::shared_ptr<BackoffRetriesMock> mockDelayTimingsPolicy = std::make_shared<BackoffRetriesMock>();
        auto mockPushd = createIpcServerForTests("pushd");
        mockPushd->listenService();
        SyncEndpoint syncdEndpoint(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockDeviceStateProvider, mockDelayTimingsPolicy, makeAccountDevicesMaker(), MAX_UPDATE_PERIOD_TEST, MIN_UPDATE_PERIOD_TEST);
        QuasarMessage msg;
        auto pushMsg = msg.mutable_push_notification();
        pushMsg->set_message(TString("{\"update_fields\":[\"networkInfo\"]}"));
        pushMsg->set_operation(TString("update_device_state"));
        mockPushd->sendToAll(std::move(msg));

        schedulePromise.get_future().get();
    }

    Y_UNIT_TEST_F(shouldUpdateUserConfig_whenConfiguratingStateIsConfigured, BaseFixture) {
        // Arrange
        std::shared_ptr<BackoffRetriesWithRandomPolicy> delayTimingsPolicy = std::make_shared<BackoffRetriesWithRandomPolicy>(getCrc32(getDeviceForTests()->deviceId()) + getNowTimestampMs());

        DeviceState configuringDeviceState;
        configuringDeviceState.configuration = DeviceState::Configuration::CONFIGURING;
        mockDeviceStateProvider->setDeviceState(configuringDeviceState);

        SyncEndpoint syncdEndpoint(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockDeviceStateProvider, delayTimingsPolicy, makeAccountDevicesMaker(), MAX_UPDATE_PERIOD_TEST, MIN_UPDATE_PERIOD_TEST);

        // check user config updated after changing device state
        std::promise<bool> messagePromise;
        auto syncdConnector = createIpcConnectorForTests("syncd");
        syncdConnector->setMessageHandler([&](const auto& message) {
            if (message->has_user_config_update()) {
                messagePromise.set_value(true);
            }
        });

        // Act
        syncdConnector->connectToService();
        syncdConnector->waitUntilConnected();

        auto promiseFuture = messagePromise.get_future();
        auto res = promiseFuture.wait_for(std::chrono::seconds(2));
        UNIT_ASSERT(res == std::future_status::timeout);

        DeviceState configuredDeviceState;
        configuredDeviceState.configuration = DeviceState::Configuration::CONFIGURED;
        mockDeviceStateProvider->setDeviceState(configuredDeviceState);
        promiseFuture.get();

        UNIT_ASSERT(true);
    }

    Y_UNIT_TEST_F(shouldSendConfigUpdateEvent_whenCurrentUserRefreshes, BaseFixture) {
        // Arrange
        std::shared_ptr<BackoffRetriesWithRandomPolicy> delayTimingsPolicy = std::make_shared<BackoffRetriesWithRandomPolicy>(getCrc32(getDeviceForTests()->deviceId()) + getNowTimestampMs());

        std::atomic<int> handleCounter{0};
        auto syncdConnector = createIpcConnectorForTests("syncd");
        syncdConnector->setMessageHandler([&](const auto& message) {
            if (message->has_user_config_update()) {
                ++handleCounter;
            }
        });

        // Act

        SyncEndpoint syncdEndpoint(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockDeviceStateProvider, delayTimingsPolicy, makeAccountDevicesMaker(), MAX_UPDATE_PERIOD_TEST, MIN_UPDATE_PERIOD_TEST);
        syncdConnector->connectToService();
        syncdConnector->waitUntilConnected();
        mockAuthProvider->setOwner(*mockAuthProvider->ownerAuthInfo().value());
        std::atomic<int> expectedCounter = handleCounter.load() + 1;

        // Assert
        doUntil([&]() { return handleCounter >= expectedCounter; }, 1000);
        UNIT_ASSERT(true);
    }

    Y_UNIT_TEST_F(shouldSendRequestToBackend_whenAuthTokenNotNeeded, BaseFixture) {
        auto delayTimingsPolicy = std::make_shared<BackoffRetriesWithRandomPolicy>(
            getCrc32(getDeviceForTests()->deviceId()) + getNowTimestampMs());

        EventChecker<bool> checker({false, false, true, false, true});

        mockBackend.onHandlePayload = [&](const TestHttpServer::Headers& headers,
                                          const std::string& /*payload*/,
                                          TestHttpServer::HttpConnection& handler) {
            Json::Value response = getConfigResponse();
            handler.doReplay(200, "application/json", jsonToString(response));
            UNIT_ASSERT(checker.addEvent(headers.hasHeader("authorization")));
        };

        auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
        config["syncd"]["authTokenNotNeeded"] = true;

        auto emptyAuthInfo = AuthInfo2{
            .source = AuthInfo2::Source::AUTHD,
            .authToken = "",
            .passportUid = "",
            .tag = 1600000000,
        };
        auto notEmptyAuthInfo = AuthInfo2{
            .source = AuthInfo2::Source::AUTHD,
            .authToken = "authToken",
            .passportUid = "passportUid",
            .tag = 1600000001,
        };

        mockAuthProvider->setOwner(emptyAuthInfo);
        SyncEndpoint syncdEndpoint(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockDeviceStateProvider, delayTimingsPolicy, makeAccountDevicesMaker(), MAX_UPDATE_PERIOD_TEST, MIN_UPDATE_PERIOD_TEST);

        checker.waitForEvents(1);
        checker.waitForEvents(2);
        mockAuthProvider->setOwner(notEmptyAuthInfo);
        checker.waitForEvents(3);
        mockAuthProvider->setOwner(emptyAuthInfo);
        checker.waitForEvents(4);
        mockAuthProvider->setOwner(notEmptyAuthInfo);
        checker.waitAllEvents();
    }

    Y_UNIT_TEST_F(shouldNotSendRequestToBackend_whenAuthTokenNeeded, BaseFixture) {
        std::shared_ptr<BackoffRetriesWithRandomPolicy> delayTimingsPolicy = std::make_shared<BackoffRetriesWithRandomPolicy>(getCrc32(getDeviceForTests()->deviceId()) + getNowTimestampMs());

        EventChecker<bool> checker({true, true});

        mockBackend.onHandlePayload = [&](const TestHttpServer::Headers& headers,
                                          const std::string& /*payload*/,
                                          TestHttpServer::HttpConnection& /*connection*/) {
            UNIT_ASSERT(checker.addEvent(headers.hasHeader("authorization")));
        };

        auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
        config["syncd"]["authTokenNotNeeded"] = false;

        auto emptyAuthInfo = AuthInfo2{
            .source = AuthInfo2::Source::AUTHD,
            .authToken = "",
            .passportUid = "",
            .tag = 1600000000,
        };
        auto notEmptyAuthInfo = AuthInfo2{
            .source = AuthInfo2::Source::AUTHD,
            .authToken = "authToken",
            .passportUid = "passportUid",
            .tag = 1600000001,
        };

        mockAuthProvider->setOwner(emptyAuthInfo);
        SyncEndpoint syncdEndpoint(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockDeviceStateProvider, delayTimingsPolicy, makeAccountDevicesMaker(), MAX_UPDATE_PERIOD_TEST, MIN_UPDATE_PERIOD_TEST);

        mockAuthProvider->setOwner(notEmptyAuthInfo);
        checker.waitForEvents(1);
        mockAuthProvider->setOwner(emptyAuthInfo);
        mockAuthProvider->setOwner(notEmptyAuthInfo);
        checker.waitAllEvents();
    }

    Y_UNIT_TEST_F(shouldUpdateCurrentUpdatePeriod_whenHaveUpdateInConfig, BaseFixture) {
        // Arrange
        std::shared_ptr<BackoffRetriesMock> mockDelayTimingsPolicy = std::make_shared<BackoffRetriesMock>();
        std::promise<void> periodSet;

        EXPECT_CALL(*mockDelayTimingsPolicy, getDelayBetweenCalls)
            .WillRepeatedly(testing::Return(std::chrono::milliseconds(1 * 1000)));
        EXPECT_CALL(*mockDelayTimingsPolicy, updateCheckPeriod(std::optional<std::chrono::milliseconds>{90 * 1000},
                                                               std::optional<std::chrono::milliseconds>{90 * 1000}))
            .WillOnce([&]() { periodSet.set_value(); });
        EXPECT_CALL(*mockDelayTimingsPolicy, resetDelayBetweenCallsToDefault()).Times(testing::AtLeast(1));

        auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);

        mockBackend.onHandlePayload = [&](const TestHttpServer::Headers& /*headers*/,
                                          const std::string& /*payload*/, TestHttpServer::HttpConnection& handler) {
            Json::Value response;

            response["status"] = "ok";
            response["config"]["system_config"]["syncd"]["pollPeriodSec"] = 90;

            handler.doReplay(200, "application/json", jsonToString(response));
        };

        config["syncd"]["configPath"] = stubConfigStoragePath.GetPath();

        // Act
        SyncEndpoint syncEndpoint(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockDeviceStateProvider, mockDelayTimingsPolicy, makeAccountDevicesMaker(), MAX_UPDATE_PERIOD_TEST, MIN_UPDATE_PERIOD_TEST);

        // Assert
        periodSet.get_future().get();
        UNIT_ASSERT(true);
    }

    Y_UNIT_TEST_F(shouldSetUpdatePeriodToDefault_whenDoesntHaveUpdateInConfig, BaseFixture) {
        // Arrange
        std::shared_ptr<BackoffRetriesMock> mockDelayTimingsPolicy = std::make_shared<BackoffRetriesMock>();
        std::promise<void> periodUnset;

        EXPECT_CALL(*mockDelayTimingsPolicy, getDelayBetweenCalls)
            .WillRepeatedly(testing::Return(std::chrono::milliseconds(1 * 1000)));
        EXPECT_CALL(*mockDelayTimingsPolicy, updateCheckPeriod(std::optional<std::chrono::milliseconds>{1 * 1000},
                                                               std::optional<std::chrono::milliseconds>{1 * 1000}))
            .WillOnce([&]() { periodUnset.set_value(); });
        EXPECT_CALL(*mockDelayTimingsPolicy, resetDelayBetweenCallsToDefault()).Times(testing::AtLeast(1));

        auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);

        mockBackend.onHandlePayload = [&](const TestHttpServer::Headers& /*headers*/,
                                          const std::string& /*payload*/, TestHttpServer::HttpConnection& handler) {
            Json::Value response;

            response["status"] = "ok";
            response["config"]["system_config"]["foo"] = "bar";
            response["config"]["system_config"]["syncd"]["foo"] = "bar";

            handler.doReplay(200, "application/json", jsonToString(response));
        };

        config["syncd"]["configPath"] = stubConfigStoragePath.GetPath();

        // Act
        SyncEndpoint syncEndpoint(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockDeviceStateProvider, mockDelayTimingsPolicy, makeAccountDevicesMaker(), MAX_UPDATE_PERIOD_TEST, MIN_UPDATE_PERIOD_TEST);

        // Assert
        periodUnset.get_future().get();
        UNIT_ASSERT(true);
    }

    Y_UNIT_TEST_F(shouldSetUpdatePeriodToDefault_whenDoesntHaveSyncdConfig, BaseFixture) {
        // Arrange
        std::shared_ptr<BackoffRetriesMock> mockDelayTimingsPolicy = std::make_shared<BackoffRetriesMock>();
        std::promise<void> periodUnset;

        EXPECT_CALL(*mockDelayTimingsPolicy, updateCheckPeriod(std::optional<std::chrono::milliseconds>{1 * 1000},
                                                               std::optional<std::chrono::milliseconds>{1 * 1000}))
            .WillOnce([&]() { periodUnset.set_value(); });
        EXPECT_CALL(*mockDelayTimingsPolicy, resetDelayBetweenCallsToDefault()).Times(testing::AtLeast(1));

        auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);

        mockBackend.onHandlePayload = [&](const TestHttpServer::Headers& /*headers*/,
                                          const std::string& /*payload*/, TestHttpServer::HttpConnection& handler) {
            Json::Value response;

            response["status"] = "ok";
            response["config"]["system_config"]["foo"] = "bar";

            handler.doReplay(200, "application/json", jsonToString(response));
        };

        config["syncd"]["configPath"] = stubConfigStoragePath.GetPath();

        // Act
        SyncEndpoint syncEndpoint(getDeviceForTests(), ipcFactoryForTests(), mockAuthProvider, mockDeviceStateProvider, mockDelayTimingsPolicy, makeAccountDevicesMaker(), MAX_UPDATE_PERIOD_TEST, MIN_UPDATE_PERIOD_TEST);

        // Assert
        periodUnset.get_future().get();
        UNIT_ASSERT(true);
    }
}

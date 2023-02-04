#include <yandex_io/capabilities/file_player/interfaces/mocks/mock_i_file_player_capability.h>

#include <yandex_io/services/firstrund/first_run_endpoint.h>

#include <yandex_io/interfaces/auth/mock/auth_provider.h>
#include <yandex_io/interfaces/device_state/mock/device_state_provider.h>
#include <yandex_io/interfaces/updates/mock/updates_provider.h>
#include <yandex_io/interfaces/user_config/mock/user_config_provider.h>
#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/http_client/http_client.h>
#include <yandex_io/libs/ipc/i_connector.h>
#include <yandex_io/libs/ipc/i_server.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/libs/threading/steady_condition_variable.h>
#include <yandex_io/tests/testlib/test_http_server.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/folder/path.h>

#include <fstream>
#include <future>
#include <memory>
#include <mutex>

using namespace quasar;
using namespace quasar::TestUtils;

namespace {

    struct Fixture: public QuasarUnitTestFixture {
        using Base = QuasarUnitTestFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            mockWifid_ = createIpcServerForTests("wifid");
            mockNetworkd_ = createIpcServerForTests("networkd");
            iosdkHub_ = createIpcServerForTests("iohub_services");

            quasarDirPath = JoinFsPaths(tryGetRamDrivePath(), "quasarDir-" + makeUUID());
            firstGreetingFilename = JoinFsPaths(quasarDirPath, "first_greeting_done");
            registeredFilename = JoinFsPaths(quasarDirPath, "registered");
            wifiDataFilename = JoinFsPaths(quasarDirPath, "wifi.dat");

            quasarDirPath.ForceDelete();
            quasarDirPath.MkDirs();

            auto& config = getDeviceForTests()->configuration()->getMutableConfig(configGuard_);

            config["firstrund"]["wifiStoragePath"] = wifiDataFilename.GetPath();
            config["firstrund"]["firstGreetingDoneFilePath"] = firstGreetingFilename.GetPath();
            config["firstrund"]["registeredFilePath"] = registeredFilename.GetPath();
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            /* Clean up test files */
            quasarDirPath.ForceDelete();

            Base::TearDown(context);
        }

        void setupBaseEnvironment() {
            auto& config = getDeviceForTests()->configuration()->getMutableConfig(configGuard_);

            /* set up small period for OTA postpones */
            config["firstrund"]["otaPostponesPeriodSec"] = 1;

            const int backendPort = getPort();
            config["common"]["backendUrl"] = "http://localhost:" + std::to_string(backendPort);

            mockBackend_.start(backendPort);

            startMockIpcServers({"pushd"});

            /* Prepare "iohub_services" port, so it DeviceContext will connect to right server */
            iosdkHub_->setClientConnectedHandler([](auto& connection) {
                /* Send start allow_init_configuration_state message, so firstrund will connect to "authd" */
                proto::QuasarMessage msg;
                msg.mutable_io_control()->mutable_allow_init_configuration_state();
                connection.send(std::move(msg));
            });

            mockUpdatesProvider_ = std::make_shared<mock::UpdatesProvider>();
            mockAuthProvider_ = std::make_shared<mock::AuthProvider>();
        }

        void startServers() const {
            mockWifid_->listenService();
            mockNetworkd_->listenService();
            iosdkHub_->listenService();
        }

        void setupNotConfiguredEnvironment() {
            setupBaseEnvironment();
            /* send empty startup info, so Firstrund will enter CONFIGURING mode */
            mockAuthProvider_->setOwner(
                AuthInfo2{
                    .source = AuthInfo2::Source::AUTHD,
                    .authToken = "",
                    .passportUid = "",
                    .tag = 1,
                });
        }

        void setupToBeConfiguredEnvironment() {
            setupBaseEnvironment();
            /* send empty startup info, so Firstrund will enter CONFIGURING mode */
            mockAuthProvider_->setOwner(
                AuthInfo2{
                    .source = AuthInfo2::Source::AUTHD,
                    .authToken = "",
                    .passportUid = "",
                    .tag = 1,
                });

            /* 200 response when firstrund tries to register */
            mockBackend_.onHandlePayload = [](const TestHttpServer::Headers& /*headers*/,
                                              const std::string /*payload*/, TestHttpServer::HttpConnection& handler) {
                handler.doReplay(200, "application/json", "{\"status\":\"ok\"}");
            };
            /* Pretnd that set up wifi successfully*/
            mockWifid_->setMessageHandler([](const auto& request, auto& connection) {
                proto::QuasarMessage response;
                response.set_request_id(request->request_id());
                response.mutable_wifi_connect_response()->set_status(proto::WifiConnectResponse::OK);
                connection.send(std::move(response));
            });

            /* add response for all "auth" requests */
            mockAuthProvider_->setOwner(
                AuthInfo2{
                    .source = AuthInfo2::Source::AUTHD,
                    .authToken = "the_auth_token",
                    .passportUid = "123",
                    .tag = 2,
                });
        }

        void setupConfiguredEnvironment() {
            setupBaseEnvironment();
            /* send startup info with some token, so Firstrund will think that device is already registered */
            mockAuthProvider_->setOwner(
                AuthInfo2{
                    .source = AuthInfo2::Source::AUTHD,
                    .authToken = "some_token",
                    .passportUid = "123",
                    .tag = 3,
                });
            /* second precondition to be CONFIGURED is "registered" file, so set up it */
            registeredFilename.Touch();
        }

        void createEndpoint() {
            auto mockDeviceStateProvider = std::make_shared<mock::DeviceStateProvider>();
            mockDeviceStateProvider->setDeviceState(mock::defaultDeviceState());
            auto mockUserConfigProvider = std::make_shared<mock::UserConfigProvider>(nullptr);
            auto mockFilePlayerCapability = std::make_shared<YandexIO::MockIFilePlayerCapability>();

            auto& config = getDeviceForTests()->configuration()->getMutableConfig(configGuard_);
            config["firstrund"]["httpPort"] = getPort();
            endpoint_ = std::make_unique<FirstRunEndpoint>(getDeviceForTests(),
                                                           ipcFactoryForTests(),
                                                           mockAuthProvider_,
                                                           mockDeviceStateProvider,
                                                           mockUpdatesProvider_,
                                                           mockUserConfigProvider,
                                                           mockFilePlayerCapability);
            endpoint_->setMinConnectError(std::chrono::milliseconds(100));
            endpoint_->start();
            endpoint_->waitConnectorsConnections();
        }

        void waitConfigurationState(proto::ConfigurationState state) {
            std::promise<void> configuredPromise;
            auto toFirstrund = createIpcConnectorForTests("firstrund");
            toFirstrund->setMessageHandler([&](const auto& msg) {
                if (msg->has_configuration_state() && msg->configuration_state() == state) {
                    configuredPromise.set_value();
                }
            });
            toFirstrund->connectToService();
            toFirstrund->waitUntilConnected();
            configuredPromise.get_future().get();
            YIO_LOG_INFO("Device Entered: " << state << " state");
        }

        std::string buildFirstrundHttpUrl() const {
            auto& config = getDeviceForTests()->configuration()->getMutableConfig(configGuard_);
            return "http://localhost:" + config["firstrund"]["httpPort"].asString();
        }
        /**
         * @brief Configure device with wifi and token, so device will enter CONFIGURED mode
         */
        void registerDevice() {
            YIO_LOG_INFO("Register Device")
            Json::Value connect;
            connect["ssid"] = "MobDevInternet";
            connect["password"] = "111";
            connect["xtoken_code"] = "2222";
            connect["plain"] = true;
            HttpClient client("test", getDeviceForTests());
            client.setTimeout(std::chrono::milliseconds{10000});
            client.post("connect", buildFirstrundHttpUrl() + "/connect", jsonToString(connect));
        }

        TFsPath quasarDirPath;
        TFsPath firstGreetingFilename;
        TFsPath registeredFilename;
        TFsPath wifiDataFilename;

        std::shared_ptr<mock::AuthProvider> mockAuthProvider_;
        std::shared_ptr<mock::UpdatesProvider> mockUpdatesProvider_;
        std::shared_ptr<ipc::IServer> mockWifid_;
        std::shared_ptr<ipc::IServer> mockNetworkd_;
        std::shared_ptr<ipc::IServer> iosdkHub_;
        TestHttpServer mockBackend_;
        YandexIO::Configuration::TestGuard configGuard_;
        std::unique_ptr<FirstRunEndpoint> endpoint_;
    };
} // namespace

Y_UNIT_TEST_SUITE_F(FirstrundAndUpdatesdRelations, Fixture) {
    Y_UNIT_TEST(TestFirstrundSendOtaPostponesInConfiguringMode) {
        /* Set up default env when Device is not configured. So it should be in CONFIGURING mode */
        setupNotConfiguredEnvironment();
        startServers();
        std::atomic<int> postponedCounter{0};
        mockUpdatesProvider_->setPostponeUpdateApply(
            [&] {
                ++postponedCounter;
            });

        createEndpoint();

        waitConfigurationState(proto::ConfigurationState::CONFIGURING);

        /* When Firstrund receive "ready apply update" message in CONFIGURING mode it should Postpone updates */
        auto us = *mockUpdatesProvider_->updatesState().value();
        us.readyToApplyUpdateFlag = true;
        mockUpdatesProvider_->setUpdatesState(us);

        /* Make sure that firstrund didn't confirm ota */
        TestUtils::waitUntil([&] { return postponedCounter.load() > 0; });
    }

    Y_UNIT_TEST(TestFirstrundSendOtaApplyConfirmInConfiguredMode) {
        /* Set up default env when Device is not configured. So it should be in CONFIGURING mode */
        setupConfiguredEnvironment();
        startServers();
        mockUpdatesProvider_->setCheckUpdates([] {});
        mockUpdatesProvider_->setWaitUpdateState([] { return UpdatesState2::Critical::NO; });
        std::atomic<int> confirmCounter{0};
        mockUpdatesProvider_->setConfirmUpdateApply(
            [&] {
                ++confirmCounter;
            });

        createEndpoint();

        waitConfigurationState(proto::ConfigurationState::CONFIGURED);
        /* When Firstrund receive "ready apply update" message in CONFIGURED mode it should Confirm update */
        auto us = *mockUpdatesProvider_->updatesState().value();
        us.readyToApplyUpdateFlag = true;
        mockUpdatesProvider_->setUpdatesState(us);

        TestUtils::waitUntil([&] { return confirmCounter.load() > 0; });
    }

    Y_UNIT_TEST(TestFirstrundSendConfirmAfterSetup) {
        /* Set up default env when Device is not configured. So it should be in CONFIGURING mode */
        setupToBeConfiguredEnvironment();
        startServers();
        std::atomic<int> postponedCounter{0};
        mockUpdatesProvider_->setPostponeUpdateApply(
            [&] {
                ++postponedCounter;
            });
        createEndpoint();

        waitConfigurationState(proto::ConfigurationState::CONFIGURING);
        /* Device is in CONFIGURING State, Send ReadyApplyUpdate message */
        auto us = *mockUpdatesProvider_->updatesState().value();
        us.readyToApplyUpdateFlag = true;
        mockUpdatesProvider_->setUpdatesState(us);

        /* Wait until receive few postpones */
        TestUtils::waitUntil([&] { return postponedCounter.load() > 1; });

        /* Make sure that firstrund enter CONFIGURED state and confirm OTA */
        mockUpdatesProvider_->setCheckUpdates([] {});
        std::atomic<int> confirmCounter{0};
        mockUpdatesProvider_->setConfirmUpdateApply(
            [&] {
                ++confirmCounter;
            });

        /* "register" device, so firstrund will enter CONFIGURED mode. After that Firstrund should "confirm ota" */
        mockAuthProvider_->setAddUser(
            [&](const std::string& /*authCode*/, UserType /*userType*/, bool /*withXToken*/, std::chrono::milliseconds /*timeout*/) {
                IAuthProvider::AddUserResponse response;
                response.status = IAuthProvider::AddUserResponse::Status::OK;
                response.authToken = "$$oauthToken$$";
                response.xToken = "$$xtoken$$";
                response.id = 234857;
                return response;
            });
        mockAuthProvider_->setChangeUser(
            [&](int64_t id, std::chrono::milliseconds /*timeout*/) {
                auto authInfo = *mockAuthProvider_->ownerAuthInfo().value();
                authInfo.passportUid = std::to_string(id);
                mockAuthProvider_->setOwner(authInfo);
                return IAuthProvider::ChangeUserResponse{
                    .status = IAuthProvider::ChangeUserResponse::Status::OK,
                };
            });

        registerDevice();

        waitConfigurationState(proto::ConfigurationState::CONFIGURED);

        /* Make sure that firstrund do not send postpones after confirming OTA */
        mockUpdatesProvider_->setPostponeUpdateApply(nullptr);

        /* Wait confirm OTA */
        TestUtils::waitUntil([&] { return confirmCounter.load() > 0; });

        /* Awaiting eunexpected postpones calls */
        std::this_thread::sleep_for(std::chrono::milliseconds(2500));
    }
}

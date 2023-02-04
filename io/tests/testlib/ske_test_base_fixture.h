#pragma once

#include "audio_source_client.h"
#include "test_audio_player.h"
#include "test_voice_dialog.h"

#include <yandex_io/services/aliced/speechkit_endpoint.h>

#include <yandex_io/capabilities/alice/interfaces/vins_request.h>
#include <yandex_io/interfaces/auth/mock/auth_provider.h>
#include <yandex_io/interfaces/clock_tower/mock/clock_tower_provider.h>
#include <yandex_io/interfaces/device_state/mock/device_state_provider.h>
#include <yandex_io/interfaces/glagol/mock/glagol_cluster_provider.h>
#include <yandex_io/interfaces/multiroom/mock/multiroom_provider.h>
#include <yandex_io/interfaces/user_config/mock/user_config_provider.h>
#include <yandex_io/libs/ipc/i_server.h>
#include <yandex_io/modules/spotter_configurer/spotter_configurer.h>
#include <yandex_io/services/aliced/tests/testlib/mock_sdk.h>
#include <yandex_io/services/aliced/tests/testlib/mock_spotter_capability_proxy.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <google/protobuf/util/message_differencer.h>

#include <library/cpp/testing/unittest/env.h>

#include <util/folder/path.h>

#include <chrono>
#include <memory>

namespace quasar::TestUtils {

    constexpr int AUDIO_DEVICE_WAIT_TIMEOUT = 1000;

    class SKETestBaseFixture: public QuasarUnitTestFixture {
    public:
        using Base = QuasarUnitTestFixture;
        using State = QuasarVoiceDialogTestImpl::State;

        using OnMessageHandler = ipc::IServer::MessageHandler;
        using OnClientConnected = ipc::IServer::ClientHandler;

        SKETestBaseFixture(bool initDefaultMocks = true);

        void SetUp(NUnitTest::TTestContext& context) override;
        void TearDown(NUnitTest::TTestContext& context) override;

        void initDefaultMocks();
        void initialize(const std::set<std::string>& disabledServices);

        void startEndpoint();

        void sendSync(std::shared_ptr<ipc::IServer> server, const proto::QuasarMessage& message);

        void setMediadMessageHandler(OnMessageHandler handler);
        void setInterfacedMessageHandler(OnMessageHandler handler);
        void setNetworkdClientConnectedHandler(OnClientConnected handler);

        void changeSpotterWord(const std::string& word);

    protected:
        static void createSoundFile(const TFsPath& dir, const std::string& fileName);

    private:
        void waitForSpotterModelsUpdated(std::function<void()> action);

        /* Data */
    protected:
        YandexIO::Configuration::TestGuard testGuard;

        /* Actual mock servers */
        std::shared_ptr<ipc::IServer> mockAudioClientd;
        std::shared_ptr<ipc::IServer> mockAlarmd;
        std::shared_ptr<ipc::IServer> mockBrickd;
        std::shared_ptr<ipc::IServer> mockSyncd;
        OnClientConnected networkdClientConnectedHandler;
        std::shared_ptr<ipc::IServer> mockNetworkd;
        OnMessageHandler mediadMessageHandler;
        OnMessageHandler interfacedMessageHandler;
        std::shared_ptr<ipc::IServer> mockMediad;
        std::shared_ptr<ipc::IServer> mockInterfaced;

        std::shared_ptr<mock::AuthProvider> mockAuthProvider;
        std::shared_ptr<mock::ClockTowerProvider> mockClockTowerProvider;
        std::shared_ptr<mock::DeviceStateProvider> mockDeviceStateProvider;
        std::shared_ptr<mock::GlagolClusterProvider> mockGlagolClusterProvider;
        std::shared_ptr<mock::MultiroomProvider> mockMultiroomProvider;
        std::shared_ptr<mock::UserConfigProvider> mockUserConfigProvider;

        std::shared_ptr<AliceConfig> aliceConfig;
        std::shared_ptr<AliceDeviceState> aliceDeviceState;

        class IoHubWrapper {
        public:
            IoHubWrapper(std::shared_ptr<ipc::IIpcFactory> ipcFactory);
            void toggleConversation(const YandexIO::VinsRequest::EventSource& eventSource = YandexIO::VinsRequest::createHardwareButtonClickEventSource());
            void startConversation(const YandexIO::VinsRequest::EventSource& eventSource = YandexIO::VinsRequest::createHardwareButtonClickEventSource());
            void stopConversation();
            void blockVoiceAssistant(const std::string& source, const std::optional<std::string>& errorSound);
            void unblockVoiceAssistant(const std::string& source);
            void bluetoothSinkConnected(const std::string& addr, const std::string& name);
            void bluetoothSinkDisconnected(const std::string& addr, const std::string& name);

        private:
            std::shared_ptr<ipc::IServer> server_;
            std::shared_ptr<ipc::IConnector> alicedConnector_;
            const std::string aliceRemoteObjectId_;
        };
        std::unique_ptr<IoHubWrapper> ioSDK;

        struct OnQuasarMessageReceivedCallback {
            void onMessage(const proto::QuasarMessage& message);
            void setMessage(const proto::QuasarMessage& message);
            void waitUntilDelivered() const;

        private:
            proto::QuasarMessage message_;
            std::atomic<bool> isReady_{false};

            mutable std::mutex mutex_;
            mutable SteadyConditionVariable cv_;
        };
        OnQuasarMessageReceivedCallback onEndpointMessageReceivedCallback_;

        std::shared_ptr<MockAudioSourceClient> testAudioSourceClient;
        std::shared_ptr<NamedCallbackQueue> queue;
        std::shared_ptr<SpeechkitEndpoint> endpoint;
        std::shared_ptr<TestAudioPlayer> testAudioPlayer;
        std::shared_ptr<QuasarVoiceDialogTestImpl> testVoiceDialog;
        std::shared_ptr<TestVins> testVins;
        std::shared_ptr<YandexIO::SpotterConfigurer> spotterConfigurer_;
        ICallbackQueue* spotterConfigurerQueue_;
        std::shared_ptr<testing::NiceMock<MockSpotterCapabilityProxy>> activationSpotterCapability_;
        std::shared_ptr<testing::NiceMock<MockSdk>> mockSdk_;

        TFsPath pathSpotterAlice;
        TFsPath pathSpotterYandex;
        TFsPath pathSpotterAdditional;
        TFsPath pathSpotterNaviOld;
        TFsPath pathTemp;

        const bool initDefaultMocks_;

        TFsPath soundsDir;
    };

} // namespace quasar::TestUtils

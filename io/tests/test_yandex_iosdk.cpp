#include <yandex_io/sdk/yandex_iosdk.h>
#include <yandex_io/sdk/private/device_context.h>

#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/ipc/i_server.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/protos/quasar_proto.pb.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <alice/megamind/protos/common/frame.pb.h>

#include <library/cpp/testing/unittest/registar.h>

#include <future>
#include <memory>

using namespace quasar;
using namespace quasar::TestUtils;
using namespace YandexIO;

namespace {
    class IOHubWrapper {
    public:
        explicit IOHubWrapper(std::shared_ptr<ipc::IIpcFactory> ipcFactory)
            : server_(ipcFactory->createIpcServer("iohub_clients"))
        {
        }

        void start() {
            server_->listenService();
        }

        void waitConnection() {
            server_->waitConnectionsAtLeast(1);
        }

        void setMessageHandler(std::function<void(const ipc::SharedMessage& msg, ipc::IServer::IClientConnection& connection)> handler) {
            server_->setMessageHandler(std::move(handler));
        }

        void setSetupMode(bool isSetup) {
            if (isSetup) {
                fireStartSetup(false);
            } else {
                fireFinishSetup();
            }
        }
        void fireSoundDataTransferStart() {
            proto::QuasarMessage message;
            message.mutable_io_event()->mutable_on_sound_data_transfer_start();
            server_->sendToAll(std::move(message));
        }

        void fireConnectingToNetwork() {
            proto::QuasarMessage message;
            message.mutable_io_event()->mutable_on_connecting_to_network();
            server_->sendToAll(std::move(message));
        }

        void fireSetupError() {
            proto::QuasarMessage message;
            message.mutable_io_event()->mutable_on_setup_error();
            server_->sendToAll(std::move(message));
        }

        void fireStartSetup(bool isFirstSetup) {
            YIO_LOG_INFO("Start setup: " << isFirstSetup);
            proto::QuasarMessage message;
            message.mutable_io_event()->set_on_start_setup(isFirstSetup);
            server_->sendToAll(std::move(message));
        }

        void fireFinishSetup() {
            proto::QuasarMessage message;
            message.mutable_io_event()->mutable_on_finish_setup();
            server_->sendToAll(std::move(message));
        }

        void fireConversationError() {
            proto::QuasarMessage message;
            message.mutable_io_event()->mutable_on_conversation_error();
            server_->sendToAll(std::move(message));
        }

        void fireEnqueueAlarm(const quasar::proto::IOEvent_AlarmType& alarmType, const std::string& alarmId) {
            proto::QuasarMessage message;
            message.mutable_io_event()->mutable_on_alarm_enqueued()->set_type(alarmType);
            message.mutable_io_event()->mutable_on_alarm_enqueued()->set_id(TString(alarmId));
            server_->sendToAll(std::move(message));
        }

        void fireStartAlarm(const quasar::proto::IOEvent_AlarmType& alarmType) {
            proto::QuasarMessage message;
            message.mutable_io_event()->set_on_alarm_started(alarmType);
            server_->sendToAll(std::move(message));
        }

        void fireStopAlarm(const quasar::proto::IOEvent_AlarmType& alarmType) {
            proto::QuasarMessage message;
            message.mutable_io_event()->mutable_on_alarm_stopped()->set_type(alarmType);
            server_->sendToAll(std::move(message));
        }

        void fireAlarmStopRemainingMedia() {
            proto::QuasarMessage message;
            message.mutable_io_event()->mutable_on_alarm_stop_remaining_media();
            server_->sendToAll(std::move(message));
        }

        void fireAlarmsSettingsChanged(const quasar::proto::AlarmsSettings& alarmsSettings) {
            proto::QuasarMessage message;
            message.mutable_io_event()->mutable_on_alarms_settings_changed()->CopyFrom(alarmsSettings);
            server_->sendToAll(std::move(message));
        }

        void fireSDKState(const proto::IOEvent::SDKState& state) {
            proto::QuasarMessage msg;
            msg.mutable_io_event()->mutable_sdk_state()->CopyFrom(state);
            server_->sendToAll(std::move(msg));
        }

    private:
        std::shared_ptr<ipc::IServer> server_;
    };

    class Fixture: public QuasarUnitTestFixture {
    public:
        using Base = QuasarUnitTestFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            startMockIpcServers({"aliced"});
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            Base::TearDown(context);
        }

        YandexIOSDK instance;
    };

    class AutoFixture: public Fixture {
    public:
        using Base = Fixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            ioHub = std::make_unique<IOHubWrapper>(ipcFactoryForTests());
            instance.init(ipcFactoryForTests(), "test", "id");
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            Base::TearDown(context);
        }

        void startIoHub() const {
            ioHub->start();
            ioHub->waitConnection();
        }

        std::unique_ptr<IOHubWrapper> ioHub;
    };
} // namespace

Y_UNIT_TEST_SUITE(TestYandexIOSDK) {

    Y_UNIT_TEST_F(testYandexIOSDKDeviceModeObserver, AutoFixture) {
        std::promise<void> startSetupPromise;
        std::promise<void> soundDataTransferPromise;
        std::promise<void> connectingWifiPromise;
        std::promise<void> errorPromise;
        std::promise<void> finishSetupPromise;
        std::promise<void> configureSuccessUpdatePromise;

        class MockDeviceModeObserver: public DeviceModeObserver {
        public:
            MockDeviceModeObserver(
                std::promise<void>& startSetupPromise,
                std::promise<void>& soundDataTransferPromise,
                std::promise<void>& connectingWifiPromise,
                std::promise<void>& errorPromise,
                std::promise<void>& finishSetupPromise,
                std::promise<void>& configureSuccessUpdatePromise)
                :

                startSetupPromise_(startSetupPromise)
                , soundDataTransferPromise_(soundDataTransferPromise)
                , connectingWifiPromise_(connectingWifiPromise)
                , errorPromise_(errorPromise)
                , finishSetupPromise_(finishSetupPromise)
                , configureSuccessUpdatePromise_(configureSuccessUpdatePromise)
            {
                // No operations.
            }
            void onStartSetup(bool /*isFirstSetup*/) override {
                startSetupPromise_.set_value();
            }
            void onSoundDataTransferStart() override {
                soundDataTransferPromise_.set_value();
            }

            void onConnectingToNetwork() override {
                connectingWifiPromise_.set_value();
            }
            void onSetupError() override {
                errorPromise_.set_value();
            }

            void onFinishSetup() override {
                finishSetupPromise_.set_value();
            }

            void onConfigureSuccessUpdate(DeviceModeObserver::ConfigurationSuccessState /*configurationSuccessState*/) override {
                configureSuccessUpdatePromise_.set_value();
            }

        private:
            std::promise<void>& startSetupPromise_;
            std::promise<void>& soundDataTransferPromise_;
            std::promise<void>& connectingWifiPromise_;
            std::promise<void>& errorPromise_;
            std::promise<void>& finishSetupPromise_;
            std::promise<void>& configureSuccessUpdatePromise_;
        };

        startIoHub();

        auto testObserver = std::make_shared<MockDeviceModeObserver>(
            startSetupPromise, soundDataTransferPromise,
            connectingWifiPromise, errorPromise,
            finishSetupPromise, configureSuccessUpdatePromise);
        instance.addDeviceModeObserver(testObserver);
        ioHub->setSetupMode(true);
        startSetupPromise.get_future().get();
        ioHub->fireSoundDataTransferStart();
        soundDataTransferPromise.get_future().get();
        ioHub->fireConnectingToNetwork();
        connectingWifiPromise.get_future().get();
        ioHub->fireSetupError();
        errorPromise.get_future().get();
        ioHub->setSetupMode(false);
        finishSetupPromise.get_future().get();
        UNIT_ASSERT(true);
    }

    Y_UNIT_TEST_F(testYandexIOSDKSDKStateObserver, AutoFixture) {
        std::promise<void> sdkStatePromise;

        class MockSDKStateObserver: public SDKStateObserver {
        public:
            MockSDKStateObserver(std::promise<void>& sdkStatePromise)
                : sdkStatePromise_(sdkStatePromise)
            {
                // No operations.
            }
            void onSDKState(const YandexIO::SDKState& /*state*/) override {
                sdkStatePromise_.set_value();
            }

        private:
            std::promise<void>& sdkStatePromise_;
        };

        startIoHub();

        auto testObserver = std::make_shared<MockSDKStateObserver>(sdkStatePromise);
        instance.addSDKStateObserver(testObserver);

        quasar::proto::IOEvent_SDKState state;
        ioHub->fireSDKState(state);
        sdkStatePromise.get_future().get();
        UNIT_ASSERT(true);
    }

    Y_UNIT_TEST_F(testYandexIOSDKAlarmObserver, AutoFixture) {
        std::promise<void> alarmEnqueuedPromise;
        std::promise<void> alarmStartedPromise;
        std::promise<void> alarmStoppedPromise;
        std::promise<void> alarmStoppedMediaPromise;
        std::promise<void> alarmsSettingsPromise;

        class MockAlarmObserver: public AlarmObserver {
        public:
            MockAlarmObserver(std::promise<void>& alarmEnqueuedPromise, std::promise<void>& alarmStartedPromise,
                              std::promise<void>& alarmStoppedPromise, std::promise<void>& alarmStoppedMediaPromise,
                              std::promise<void>& alarmsSettingsPromise)
                : alarmEnqueuedPromise_(alarmEnqueuedPromise)
                , alarmStartedPromise_(alarmStartedPromise)
                , alarmStoppedPromise_(alarmStoppedPromise)
                , alarmStoppedMediaPromise_(alarmStoppedMediaPromise)
                , alarmsSettingsPromise_(alarmsSettingsPromise)
            {
                // No operations.
            }
            void onAlarmEnqueued(AlarmType /*alarmType*/, const std::string& /*alarmId*/) override {
                alarmEnqueuedPromise_.set_value();
            }
            void onAlarmStarted(AlarmType /*alarmType*/) override {
                alarmStartedPromise_.set_value();
            }

            void onAlarmStopped(AlarmType /*alarmType*/, bool /*hasRemainingMedia*/) override {
                alarmStoppedPromise_.set_value();
            }

            void onAlarmStopRemainingMedia() override {
                alarmStoppedMediaPromise_.set_value();
            }

            void onAlarmsSettingsChanged(const AlarmsSettings& /*alarmsSettings*/) override {
                alarmsSettingsPromise_.set_value();
            }

        private:
            std::promise<void>& alarmEnqueuedPromise_;
            std::promise<void>& alarmStartedPromise_;
            std::promise<void>& alarmStoppedPromise_;
            std::promise<void>& alarmStoppedMediaPromise_;
            std::promise<void>& alarmsSettingsPromise_;
        };

        startIoHub();

        auto testObserver = std::make_shared<MockAlarmObserver>(alarmEnqueuedPromise, alarmStartedPromise,
                                                                alarmStoppedPromise, alarmStoppedMediaPromise, alarmsSettingsPromise);
        instance.addAlarmObserver(testObserver);
        ioHub->fireEnqueueAlarm(quasar::proto::IOEvent::CLASSIC_ALARM, "testAlarmId");
        alarmEnqueuedPromise.get_future().get();
        ioHub->fireStartAlarm(quasar::proto::IOEvent::CLASSIC_ALARM);
        alarmStartedPromise.get_future().get();
        ioHub->fireStopAlarm(quasar::proto::IOEvent::CLASSIC_ALARM);
        alarmStoppedPromise.get_future().get();
        ioHub->fireAlarmStopRemainingMedia();
        alarmStoppedMediaPromise.get_future().get();
        quasar::proto::AlarmsSettings alarmsSettings;
        ioHub->fireAlarmsSettingsChanged(alarmsSettings);
        alarmsSettingsPromise.get_future().get();
        UNIT_ASSERT(true);
    }

    Y_UNIT_TEST_F(testYandexIOSDKUpdateAllow, Fixture) {
        instance.init(ipcFactoryForTests(), "test", "id");

        {
            int counter = 0;
            std::unique_ptr<std::promise<bool>> promise;
            auto ioHub = std::make_unique<IOHubWrapper>(ipcFactoryForTests());
            ioHub->setMessageHandler([&](const auto& msg, auto& /*connection*/) {
                if (msg->io_control().has_allow_update()) {
                    if (counter == 0) {
                        promise->set_value(msg->io_control().allow_update().for_all());
                    } else {
                        UNIT_FAIL("Received second event");
                    }
                    ++counter;
                }
            });
            ioHub->start();

            // If we don't send anything explicitly we don't receive any message
            promise = std::make_unique<std::promise<bool>>();
            auto result = promise->get_future().wait_for(std::chrono::seconds(1));
            UNIT_ASSERT(result == std::future_status::timeout);

            promise = std::make_unique<std::promise<bool>>();
            instance.setAllowUpdate(false, false);
            bool receivedValue = promise->get_future().get();

            UNIT_ASSERT(!receivedValue);
        }

        {
            int counter = 0;
            std::promise<bool> firstPromise;
            std::promise<bool> secondPromise;

            auto ioHub = std::make_unique<IOHubWrapper>(ipcFactoryForTests());
            ioHub->setMessageHandler([&](const auto& msg, auto& /*connection*/) {
                if (msg->io_control().has_allow_update()) {
                    const auto allowAll = msg->io_control().allow_update().for_all();
                    std::cout << "Received: " << allowAll << std::endl;
                    if (counter == 0) {
                        firstPromise.set_value(allowAll);
                    } else if (counter == 1) {
                        secondPromise.set_value(allowAll);
                    } else {
                        UNIT_FAIL("Received third event");
                    }
                    ++counter;
                }
            });
            ioHub->start();

            bool firstValue = firstPromise.get_future().get();
            instance.setAllowUpdate(true, true);
            bool secondValue = secondPromise.get_future().get();

            UNIT_ASSERT(!firstValue);
            UNIT_ASSERT(secondValue);
        }
    }

    Y_UNIT_TEST_F(testProvideUserAccount, Fixture) {
        std::promise<std::pair<std::string, std::string>> infoPromise;
        IOHubWrapper ioHub(ipcFactoryForTests());

        ioHub.setMessageHandler([&](const auto& msg, auto& /*connection*/) {
            if (msg->io_control().has_provide_user_account_info()) {
                const auto& info = msg->io_control().provide_user_account_info();
                infoPromise.set_value(std::make_pair(info.oauth_token(), info.uid()));
            }
        });

        ioHub.start();
        YandexIOSDK sdk;
        sdk.init(ipcFactoryForTests(), "test", "id");

        // Make sure that hub and sdk are connected to each other
        sdk.waitUntilConnected();
        ioHub.waitConnection();

        sdk.provideUserAccountInfo("token", "uid");

        const auto info = infoPromise.get_future().get();
        UNIT_ASSERT_VALUES_EQUAL(info.first, "token");
        UNIT_ASSERT_VALUES_EQUAL(info.second, "uid");
    }

    Y_UNIT_TEST_F(testProvideUserAccountOnConnect, Fixture) {
        std::promise<std::pair<std::string, std::string>> infoPromise;
        IOHubWrapper ioHub(ipcFactoryForTests());

        ioHub.setMessageHandler([&](const auto& msg, auto& /*connecton*/) {
            if (msg->io_control().has_provide_user_account_info()) {
                const auto& info = msg->io_control().provide_user_account_info();
                infoPromise.set_value(std::make_pair(info.oauth_token(), info.uid()));
            }
        });

        ioHub.start();
        YandexIOSDK sdk;
        sdk.init(ipcFactoryForTests(), "test", "id");
        sdk.provideUserAccountInfo("token", "uid");

        const auto info = infoPromise.get_future().get();
        UNIT_ASSERT_VALUES_EQUAL(info.first, "token");
        UNIT_ASSERT_VALUES_EQUAL(info.second, "uid");
    }

    Y_UNIT_TEST_F(testActiveActions, AutoFixture) {
        std::promise<bool> hasActiveActionsPromise;
        bool hasMainFrame = false;
        bool hasTypedSemanticFrame = false;
        bool hasDoNothingSemanticFrame = false;
        ioHub->setMessageHandler([&](const auto& msg, auto& /*connection*/) {
            if (msg->io_control().has_active_actions()) {
                const auto semanticFrames = msg->io_control().active_actions().semanticframes();
                if (semanticFrames.count("main")) {
                    hasMainFrame = true;
                    if (semanticFrames.at("main").has_typedsemanticframe()) {
                        hasTypedSemanticFrame = true;
                        hasDoNothingSemanticFrame = semanticFrames.at("main").typedsemanticframe().has_donothingsemanticframe();
                    }
                }
                hasActiveActionsPromise.set_value(true);
            }
        });

        startIoHub();

        YandexIOSDK sdk;
        sdk.init(ipcFactoryForTests(), "test", "id");
        // Make sure that hub and sdk are connected to each other
        sdk.waitUntilConnected();
        ioHub->waitConnection();

        NAlice::TDoNothingSemanticFrame frame;
        NAlice::TTypedSemanticFrame typedFrame;
        typedFrame.mutable_donothingsemanticframe()->CopyFrom(frame);
        NAlice::TSemanticFrameRequestData frameData;
        frameData.mutable_typedsemanticframe()->CopyFrom(typedFrame);
        NAlice::TDeviceState::TActiveActions activeActions;
        activeActions.mutable_semanticframes()->insert(google::protobuf::MapPair(TString("main"), frameData));
        sdk.setActiveActions(activeActions);

        const auto hasActiveActions = hasActiveActionsPromise.get_future().get();
        UNIT_ASSERT(hasActiveActions);
        UNIT_ASSERT(hasMainFrame);
        UNIT_ASSERT(hasTypedSemanticFrame);
        UNIT_ASSERT(hasDoNothingSemanticFrame);
    }
}

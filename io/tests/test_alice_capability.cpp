#include "mocks/mock_i_player.h"

#include <yandex_io/capabilities/alice/interfaces/mocks/mock_i_alice_request_events.h>
#include <yandex_io/capabilities/file_player/interfaces/mocks/mock_i_file_player_capability.h>
#include <yandex_io/interfaces/device_state/mock/device_state_provider.h>
#include <yandex_io/interfaces/stereo_pair/mock/stereo_pair_provider.h>
#include <yandex_io/libs/activity_tracker/tests/mocks/mock_activity_tracker.h>
#include <yandex_io/libs/base/directives.h>
#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/ipc/mock/mock_i_connector.h>
#include <yandex_io/libs/ipc/mock/server.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/sdk/interfaces/directive.h>
#include <yandex_io/sdk/private/device_context.h>
#include <yandex_io/services/aliced/capabilities/alice_capability/alice_capability.h>
#include <yandex_io/services/aliced/device_state/alice_device_state.h>
#include <yandex_io/services/aliced/directive_processor/directive_processor.h>
#include <yandex_io/services/aliced/directive_processor/mocks/mock_i_directive_processor.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

using namespace quasar;
using namespace testing;
using namespace NAlice;
using namespace YandexIO;

namespace {

    template <class TDirectiveProcessor>
    class TAliceCapabilityFixture: public QuasarUnitTestFixture {
    public:
        void SetUp(NUnitTest::TTestContext& context) override {
            QuasarUnitTestFixture::SetUp(context);
            init();
        }

    protected:
        std::shared_ptr<Directive> createTtsDirective(proto::AudioChannel channel, std::shared_ptr<MockIPlayer> player) {
            Directive::Data data(Directives::TTS_PLAY_PLACEHOLDER, "local_action",
                                 std::optional<quasar::proto::AudioChannel>(channel));
            data.setContext("", "requestId", "parentRequestId", "");
            return std::make_shared<PlayTtsDirective>(std::move(data), player, false);
        }

    private:
        void init() {
            deviceContext_ = std::make_shared<YandexIO::DeviceContext>(ipcFactoryForTests());
            mockDeviceStateProvider_->setDeviceState(mock::defaultDeviceState());

            directiveProcessor_ = std::make_shared<TDirectiveProcessor>();
            stereoPairProvider_ = std::make_shared<quasar::mock::StereoPairProvider>();
            filePlayerCapabilityMock_ = std::make_shared<MockIFilePlayerCapability>();

            capability_ = std::make_shared<AliceCapability>(
                aliceConfig_,
                deviceState_,
                activityTrackerMock_,
                directiveProcessor_,
                *deviceContext_,
                device_->telemetry(),
                std::weak_ptr<QuasarVoiceDialog>(),
                std::make_shared<quasar::ipc::mock::Server>(),
                std::make_shared<NiceMock<ipc::mock::MockIConnector>>(),
                nullptr,
                std::weak_ptr<YandexIO::IRemotingRegistry>(),
                stereoPairProvider_,
                filePlayerCapabilityMock_);

            if (!directiveProcessor_->addDirectiveHandler(capability_)) {
                YIO_LOG_INFO("addDirectiveHandler failed");
            }
        }

    public:
        ActivityTracker activityTracker_;
        MockActivityTracker activityTrackerMock_;
        std::shared_ptr<TDirectiveProcessor> directiveProcessor_;
        std::shared_ptr<AliceCapability> capability_;
        AliceConfig aliceConfig_{nullptr, Json::Value()};
        std::shared_ptr<YandexIO::DeviceContext> deviceContext_;
        std::shared_ptr<mock::DeviceStateProvider> mockDeviceStateProvider_ = std::make_shared<mock::DeviceStateProvider>();
        AliceDeviceState deviceState_{"", nullptr, mockDeviceStateProvider_, EnvironmentStateHolder("", nullptr)};
        std::shared_ptr<quasar::mock::StereoPairProvider> stereoPairProvider_;
        std::shared_ptr<MockIFilePlayerCapability> filePlayerCapabilityMock_;
    };

    using AliceCapabilityFixture = TAliceCapabilityFixture<YandexIO::DirectiveProcessor>;
    using AliceCapabilityDirectiveProcessorMockFixture = TAliceCapabilityFixture<MockIDirectiveProcessor>;

} // anonymous namespace

Y_UNIT_TEST_SUITE(AliceCapabilityTest) {
    Y_UNIT_TEST_F(testHandleDirective_Listen, AliceCapabilityFixture) {
        VinsRequest::EventSource eventSource;
        eventSource.set_event(TSpeechKitRequestProto_TEventSource_EEvent_Directive);
        eventSource.set_source(TSpeechKitRequestProto_TEventSource_ESource_Software);
        eventSource.set_type(TSpeechKitRequestProto_TEventSource_EType_Voice);
        eventSource.set_id(TString(makeUUID()));

        auto request = VinsRequest::createListenRequest(Directive::Data(Directives::LISTEN, "local_action"), eventSource);
        auto directive = std::make_shared<AliceRequestDirective>(std::move(request), nullptr, true);

        {
            InSequence sequence;

            EXPECT_CALL(activityTrackerMock_, addActivity(_)).WillOnce(Invoke([&](const YandexIO::IActivityPtr& activity) -> bool {
                UNIT_ASSERT(activity->getAudioChannel() == proto::DIALOG_CHANNEL);
                return activityTracker_.addActivity(activity);
            }));

            EXPECT_CALL(activityTrackerMock_, removeActivity(_)).WillOnce(Invoke([&](const YandexIO::IActivityPtr& activity) -> bool {
                UNIT_ASSERT(activity->getAudioChannel() == proto::DIALOG_CHANNEL);
                return activityTracker_.removeActivity(activity);
            }));
        }

        capability_->handleDirective(directive);
        capability_->cancelDirective(directive);
    }

    Y_UNIT_TEST_F(testHandleDirective_StartMusicRecognizer, AliceCapabilityFixture) {
        VinsRequest::EventSource eventSource;
        eventSource.set_event(TSpeechKitRequestProto_TEventSource_EEvent_Directive);
        eventSource.set_source(TSpeechKitRequestProto_TEventSource_ESource_Software);
        eventSource.set_type(TSpeechKitRequestProto_TEventSource_EType_Music);
        eventSource.set_id(TString(makeUUID()));

        auto request = VinsRequest::createListenRequest(Directive::Data(Directives::START_MUSIC_RECOGNIZER, "local_action"), eventSource);
        auto directive = std::make_shared<AliceRequestDirective>(std::move(request), nullptr, true);

        {
            InSequence sequence;

            EXPECT_CALL(activityTrackerMock_, addActivity(_)).WillOnce(Invoke([&](const YandexIO::IActivityPtr& activity) -> bool {
                UNIT_ASSERT(activity->getAudioChannel() == proto::DIALOG_CHANNEL);
                return activityTracker_.addActivity(activity);
            }));

            EXPECT_CALL(activityTrackerMock_, removeActivity(_)).WillOnce(Invoke([&](const YandexIO::IActivityPtr& activity) -> bool {
                UNIT_ASSERT(activity->getAudioChannel() == proto::DIALOG_CHANNEL);
                return activityTracker_.removeActivity(activity);
            }));
        }

        capability_->handleDirective(directive);
        capability_->cancelDirective(directive);
    }

    Y_UNIT_TEST_F(testHandleDirective_PlayTtsPlaceholder_DialogChannel, AliceCapabilityFixture) {
        const proto::AudioChannel channel = proto::DIALOG_CHANNEL;
        auto ttsPlayerMock = std::make_shared<MockIPlayer>();

        auto directive = createTtsDirective(channel, ttsPlayerMock);

        {
            InSequence sequence;

            EXPECT_CALL(activityTrackerMock_, addActivity(_)).WillOnce(Invoke([&](const YandexIO::IActivityPtr& activity) -> bool {
                UNIT_ASSERT(activity->getAudioChannel() == channel);
                return activityTracker_.addActivity(activity);
            }));

            EXPECT_CALL(*ttsPlayerMock, setListener(_));
            EXPECT_CALL(*ttsPlayerMock, play(_)).WillOnce([&](const proto::AudioAnalyticsContext& context) {
                ASSERT_TRUE(context.vins_request_id() == "requestId");
                ASSERT_TRUE(context.vins_parent_request_id() == "parentRequestId");
                capability_->onPlayingBegin(nullptr);
            });

            EXPECT_CALL(*ttsPlayerMock, cancel());
            EXPECT_CALL(activityTrackerMock_, removeActivity(_)).WillOnce(Invoke([&](const YandexIO::IActivityPtr& activity) -> bool {
                UNIT_ASSERT(activity->getAudioChannel() == channel);
                return activityTracker_.removeActivity(activity);
            }));
        }

        capability_->handleDirective(directive);
        capability_->cancelDirective(directive);
    }

    Y_UNIT_TEST_F(testStartVoiceInputWhenNotConfigured, AliceCapabilityFixture) {
        mockDeviceStateProvider_->setDeviceState(DeviceState{
            .configuration = DeviceState::Configuration::CONFIGURING,
            .networkStatus = mockDeviceStateProvider_->deviceState().value()->networkStatus,
            .update = mockDeviceStateProvider_->deviceState().value()->update,
        });

        auto aliceRequestEvents = std::make_shared<YandexIO::MockIAliceRequestEvents>();

        EXPECT_CALL(*aliceRequestEvents, onAliceRequestError(_, "DeviceNotReady", "Настройте станцию заново"));
        EXPECT_CALL(*filePlayerCapabilityMock_, playSoundFile(_, _, _, _)).Times(0);

        capability_->startVoiceInput(VinsRequest::createHardwareButtonClickEventSource(), aliceRequestEvents);
    }

    Y_UNIT_TEST_F(testStartServerActionWhenCriticalUpdate, AliceCapabilityFixture) {
        mockDeviceStateProvider_->setDeviceState(DeviceState{
            .configuration = mockDeviceStateProvider_->deviceState().value()->configuration,
            .networkStatus = mockDeviceStateProvider_->deviceState().value()->networkStatus,
            .update = DeviceState::Update::HAS_CRITICAL,
        });

        {
            auto aliceRequestEvents = std::make_shared<YandexIO::MockIAliceRequestEvents>();

            EXPECT_CALL(*aliceRequestEvents, onAliceRequestError(_, "DeviceNotReady", "Настройте станцию заново")).Times(1);
            EXPECT_CALL(*filePlayerCapabilityMock_, playSoundFile(_, _, _, _)).Times(0);

            capability_->startVoiceInput(VinsRequest::createHardwareButtonClickEventSource(), aliceRequestEvents);
        }

        {
            auto aliceRequestEvents = std::make_shared<testing::StrictMock<YandexIO::MockIAliceRequestEvents>>();

            EXPECT_CALL(*aliceRequestEvents, onAliceRequestStarted(_)).Times(1);
            EXPECT_CALL(*filePlayerCapabilityMock_, playSoundFile(_, _, _, _)).Times(0);

            auto request = VinsRequest::createServerActionRequest(YandexIO::Directive::Data("directive_name", "server_action"));
            capability_->startRequest(std::move(request), aliceRequestEvents);
        }
    }

    Y_UNIT_TEST_F(testStartRequestWithoutTokenByNonUserAction, AliceCapabilityFixture) {
        auto aliceRequestEvents = std::make_shared<YandexIO::MockIAliceRequestEvents>();

        EXPECT_CALL(*aliceRequestEvents, onAliceRequestError(_, "DeviceNotReady", "Настройте устройство заново")).Times(1);
        EXPECT_CALL(*filePlayerCapabilityMock_, playSoundFile("auth_failed.wav", std::optional<proto::AudioChannel>(proto::DIALOG_CHANNEL), _, _)).Times(1);

        deviceState_.setAuthFailed(true);
        capability_->startVoiceInput(VinsRequest::createHardwareButtonClickEventSource(), aliceRequestEvents);
    }

    Y_UNIT_TEST_F(testCancelDirectiveDontCallHandlerCompleted, AliceCapabilityDirectiveProcessorMockFixture) {
        auto ttsPlayerMock = std::make_shared<MockIPlayer>();
        auto ttsDirective = createTtsDirective(proto::DIALOG_CHANNEL, ttsPlayerMock);

        EXPECT_CALL(*directiveProcessor_, onHandleDirectiveCompleted(_, Matcher<bool>(_))).Times(0);

        capability_->handleDirective(ttsDirective);
        capability_->cancelDirective(ttsDirective);
    }

}

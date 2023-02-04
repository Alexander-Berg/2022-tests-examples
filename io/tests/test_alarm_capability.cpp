#include <yandex_io/capabilities/alarm/interfaces/mocks/mock_i_alarm_capability_listener.h>
#include <yandex_io/capabilities/alice/interfaces/mocks/mock_i_alice_capability.h>
#include <yandex_io/capabilities/file_player/interfaces/mocks/mock_i_file_player_capability.h>
#include <yandex_io/capabilities/playback_control/interfaces/mocks/mock_playback_control_capability.h>

#include <yandex_io/services/aliced/capabilities/alarm_capability/alarm_capability.h>
#include <yandex_io/services/aliced/capabilities/alice_capability/directives/alice_request_directive.h>
#include <yandex_io/services/aliced/directive_processor/mocks/mock_i_directive_processor.h>
#include <yandex_io/services/aliced/device_state/alice_device_state.h>
#include <yandex_io/services/aliced/device_state/mocks/mock_i_alice_device_state.h>
#include <yandex_io/tests/testlib/test_callback_queue.h>

#include <yandex_io/libs/base/directives.h>
#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/ipc/mock/mock_i_connector.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/libs/protobuf_utils/debug.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>
#include <yandex_io/tests/testlib/null_device_state_capability/null_device_state_capability.h>

using namespace quasar;
using namespace quasar::TestUtils;
using namespace testing;
using namespace YandexIO;

namespace {
    std::shared_ptr<Directive> buildDirective(const std::string& name,
                                              const std::string& requestId,
                                              const std::string& payloadStr) {
        Json::Value payload = tryParseJsonOrEmpty(payloadStr);
        YIO_LOG_INFO("Creating directive " << name);
        Directive::Data data(name, "local_action", std::move(payload));
        data.requestId = requestId;
        return std::make_shared<Directive>(std::move(data));
    }

    std::shared_ptr<Directive> buildDirective(const std::string& name) {
        return buildDirective(name, makeUUID(), "");
    }

    proto::Alarm buildAlarm() {
        proto::Alarm alarm;
        alarm.set_id("alarm_id");
        alarm.set_alarm_type(proto::Alarm::ALARM);
        return alarm;
    }

    proto::Alarm buildMediaAlarm() {
        proto::Alarm alarm;
        alarm.set_id("media_alarm_id");
        alarm.set_alarm_type(proto::Alarm::MEDIA_ALARM);
        return alarm;
    }

    proto::Alarm buildTimer() {
        proto::Alarm alarm;
        alarm.set_id("timer_id");
        alarm.set_alarm_type(proto::Alarm::TIMER);
        return alarm;
    }

    ipc::SharedMessage buildAlarmFiredMessage(const proto::Alarm& alarm) {
        return ipc::buildMessage([&alarm](auto& msg) {
            msg.mutable_alarm_event()->mutable_alarm_fired()->CopyFrom(alarm);
        });
    }

    ipc::SharedMessage buildAlarmApprovedMessage(const proto::Alarm& alarm) {
        return ipc::buildMessage([&alarm](auto& msg) {
            msg.mutable_alarm_event()->mutable_alarm_approved()->CopyFrom(alarm);
        });
    }

    ipc::SharedMessage buildAlarmStoppedMessage(const proto::Alarm& alarm, bool stopMedia = false) {
        return ipc::buildMessage([&alarm, stopMedia](auto& msg) {
            msg.mutable_alarm_event()->mutable_alarm_stopped()->mutable_alarm()->CopyFrom(alarm);
            msg.mutable_alarm_event()->mutable_alarm_stopped()->set_stop_media(stopMedia);
        });
    }

    ipc::SharedMessage buildAlarmStateMessage(const TString& ical, const Json::Value& settings) {
        return ipc::buildMessage([&settings, &ical](auto& msg) {
            msg.mutable_alarms_state()->set_icalendar_state(ical);
            msg.mutable_alarms_state()->set_media_alarm_setting(jsonToString(settings));
        });
    }

    bool verifyAlarmMusicPlayDirective(const std::shared_ptr<Directive>& directive, const std::string& alarmId) {
        if (directive == nullptr) {
            YIO_LOG_WARN("Should not be nullptr");
            return false;
        }
        if (!directive->is(quasar::Directives::MUSIC_PLAY)) {
            YIO_LOG_WARN("Directive is not MUSIC_PLAY, it is " << directive->getData().name);
            return false;
        }
        if (directive->getRequestId() != alarmId) {
            YIO_LOG_WARN("Request_id doesn't match alarmId");
            return false;
        }

        return true;
    }

    bool verifyAlarmRadioPlayDirective(const std::shared_ptr<Directive>& directive, const std::string& alarmId) {
        if (directive == nullptr) {
            YIO_LOG_WARN("Should not be nullptr");
            return false;
        }
        if (!directive->is(quasar::Directives::RADIO_PLAY)) {
            YIO_LOG_WARN("Directive is not RADIO_PLAY, it is " << directive->getData().name);
            return false;
        }
        if (directive->getRequestId() != alarmId) {
            YIO_LOG_WARN("Request_id doesn't match alarmId");
            return false;
        }
        if (!directive->getData().payload.isMember("force_restart_player") || directive->getData().payload["force_restart_player"] != true) {
            YIO_LOG_WARN("Payload is missing expected 'force_restart_player'");
            return false;
        }

        return true;
    }

    bool verifyAlarmAudioPlayDirective(const std::shared_ptr<Directive>& directive, const std::string& alarmId) {
        if (directive == nullptr) {
            YIO_LOG_WARN("Should not be nullptr");
            return false;
        }
        if (!directive->is(quasar::Directives::AUDIO_PLAY)) {
            YIO_LOG_WARN("Directive is not AUDIO_PLAY, it is " << directive->getData().name);
            return false;
        }
        if (directive->getRequestId() != alarmId && directive->getData().parentRequestId != alarmId) {
            YIO_LOG_WARN("Request_id and parent_requst_id doesn't match alarmId");
            return false;
        }

        return true;
    }

    MATCHER(VerifyAlarmStartDirective, "description") {
        const std::shared_ptr<Directive>& directive = *arg.begin();
        if (directive == nullptr) {
            *result_listener << "Should not be nullptr";
            return false;
        }
        if (!directive->is(quasar::Directives::ALARM_START)) {
            *result_listener << "Directive is not ALARM_START, it is " << directive->getData().name;
            return false;
        }

        return true;
    }

    MATCHER(VerifyAlarmStopDirective, "description") {
        const std::shared_ptr<Directive>& directive = *arg.begin();
        if (directive == nullptr) {
            *result_listener << "Should not be nullptr";
            return false;
        }
        if (!directive->is(quasar::Directives::ALARM_STOP)) {
            *result_listener << "Directive is not ALARM_STOP, it is " << directive->getData().name;
            return false;
        }

        return true;
    }

    MATCHER_P(VerifyPlayParams, alarmId, "description") {
        std::optional<YandexIO::IFilePlayerCapability::PlayParams> params = arg;
        if (!params.has_value() || params->playLooped != true || params->parentRequestId != alarmId) {
            *result_listener << "Wrong play params";
            return false;
        }
        return true;
    }

    MATCHER_P(VerifyConfirmAlarmStartedMessage, expectedAlarm, "description") {
        const proto::QuasarMessage& message = *arg;
        if (!message.has_alarm_event() || !message.alarm_event().has_alarm_confirmed()) {
            *result_listener << " no alarm_confirmed";
            return false;
        }
        auto alarm = message.alarm_event().alarm_confirmed();
        if (expectedAlarm.id() != alarm.id()) {
            *result_listener << " ConfirmAlarmStarted alarm.id != expectedAlarm.id";
            return false;
        }
        if (expectedAlarm.alarm_type() != alarm.alarm_type()) {
            *result_listener << " alarm.alarm_type != expectedAlarm.alarm_type";
            return false;
        }

        return true;
    }

    MATCHER_P(VerifyStopAlarmMessage, shouldStopMedia, "description") {
        const proto::QuasarMessage& message = *arg;
        if (!message.has_alarm_message() || !message.alarm_message().has_stop_alarm() || !message.alarm_message().stop_alarm().has_stop_media()) {
            *result_listener << " no stop_media";
            return false;
        }
        bool stopMedia = message.alarm_message().stop_alarm().stop_media();
        if (stopMedia != shouldStopMedia) {
            *result_listener << " stopMedia != shouldStopMedia";
            return false;
        }
        return true;
    }

    MATCHER_P(VerifyPlayingAlarm, isTimer, "description") {
        const NAlice::TDeviceState::TAlarmState& alarm = arg;
        if (isTimer) {
            return true;
        }
        return alarm.GetCurrentlyPlaying();
    }

    MATCHER(VerifyStopAnyRemainingMediaMessage, "description") {
        const proto::QuasarMessage& message = *arg;
        if (!message.has_alarm_message() || !message.alarm_message().has_alarm_stop_directive()) {
            *result_listener << " no alarm_stop_directive";
            return false;
        }
        return true;
    }
} // namespace

namespace {

    class MockDeviceStateCapability: public NullDeviceStateCapability {
    public:
        MOCK_METHOD(void, setICalendar, (const TString&), (override));
        MOCK_METHOD(void, setAlarmState, (const NAlice::TDeviceState::TAlarmState&), (override));
        MOCK_METHOD(void, setTimersState, (const NAlice::TDeviceState::TTimers&), (override));
    };

    class AlarmCapabilityFixture: public QuasarUnitTestFixture {
    public:
        void SetUp(NUnitTest::TTestContext& context) override {
            QuasarUnitTestFixture::SetUp(context);
            init();
        }

        void expectOnAlarmStarted(const proto::Alarm& expectedAlarm) const {
            EXPECT_CALL(*deviceStateCapabilityMock, setTimersState);
            EXPECT_CALL(*deviceStateCapabilityMock, setAlarmState(VerifyPlayingAlarm(expectedAlarm.alarm_type() == proto::Alarm::TIMER)));
            EXPECT_CALL(*alarmConnectorMock, sendMessage(Matcher<const quasar::ipc::SharedMessage&>(VerifyConfirmAlarmStartedMessage(expectedAlarm))));
            EXPECT_CALL(*alarmListener, onAlarmStarted());
        }

        void expectOnAlarmStopped() const {
            EXPECT_CALL(*deviceStateCapabilityMock, setTimersState);
            EXPECT_CALL(*deviceStateCapabilityMock, setAlarmState);
            EXPECT_CALL(*alarmListener, onAlarmStopped());
        }

        void expectHandleServerAction(const std::string& alarmId, const Json::Value& settings) const {
            EXPECT_CALL(*aliceCapabilityMock, startRequest(_, _))
                .WillOnce(Invoke([settings, &alarmId](std::shared_ptr<VinsRequest> request, std::shared_ptr<IAliceRequestEvents> events) {
                    Y_UNUSED(events);
                    UNIT_ASSERT_VALUES_EQUAL(request->getId(), alarmId);
                    UNIT_ASSERT_VALUES_EQUAL(request->getIsSilent(), true);
                    auto reqServerAction = settings["server_action"];
                    reqServerAction["payload"]["typed_semantic_frame"]["music_play_semantic_frame"]["alarm_id"]["string_value"] = alarmId;
                    UNIT_ASSERT_VALUES_EQUAL(jsonToString(request->getEvent()), jsonToString(reqServerAction));
                }));
        }

        void expectAlarmStartDirective() const {
            EXPECT_CALL(*directiveProcessorMock, addDirectives(VerifyAlarmStartDirective()))
                .WillOnce(Invoke([this](std::list<std::shared_ptr<Directive>> directives) {
                    capability->preprocessDirectives(directives);
                    handleDirectives(directives);
                }));
        }

        void expectAlarmStopDirective() const {
            EXPECT_CALL(*directiveProcessorMock, addDirectives(VerifyAlarmStopDirective()))
                .WillOnce(Invoke([this](std::list<std::shared_ptr<Directive>> directives) {
                    capability->preprocessDirectives(directives);
                    handleDirectives(directives);
                }));
        }

        void expectPlaySoundFile(const std::string& alarmId) const {
            EXPECT_CALL(*filePlayerCapabilityMock, playSoundFile("timer.wav", std::optional<proto::AudioChannel>(proto::DIALOG_CHANNEL), VerifyPlayParams(alarmId), _));
        }

        void expectStopSoundFile() const {
            EXPECT_CALL(*filePlayerCapabilityMock, stopSoundFile("timer.wav"));
        }

        void handleDirectives(const std::list<std::shared_ptr<Directive>>& directives) const {
            const auto& supported = capability->getSupportedDirectiveNames();
            for (const auto& directive : directives) {
                if (supported.contains(directive->getData().name)) {
                    capability->handleDirective(directive);
                }
            }
        }

        void prepareMediaAlarmSetting(const Json::Value& settings) const {
            capability->handleAlarmdMessage(buildAlarmStateMessage("ical", settings));
        }

        static Json::Value getMusicAlarmSetting() {
            Json::Value payload;
            payload["server_action"]["data"]["from_alarm"] = true;
            payload["server_action"]["name"] = "@@mm_semantic_frame";
            payload["server_action"]["type"] = "server_action";

            payload["sound_alarm_setting"]["type"] = "music";
            payload["sound_alarm_setting"]["info"]["artists"] = "JD";
            payload["sound_alarm_setting"]["info"]["album"] = "Scrubs";

            return payload;
        }

        static Json::Value getRadioAlarmSetting() {
            Json::Value payload;
            payload["server_action"]["data"]["from_alarm"] = true;
            payload["server_action"]["name"] = "@@mm_semantic_frame";
            payload["server_action"]["type"] = "server_action";

            payload["sound_alarm_setting"]["type"] = "radio";
            payload["sound_alarm_setting"]["info"]["radioId"] = "HIMYM";
            payload["sound_alarm_setting"]["info"]["radioTitle"] = "Hey Beautiful";

            return payload;
        }

        static std::shared_ptr<Directive> getMusicPlayDirective(const std::string& requestId) {
            return buildDirective(Directives::MUSIC_PLAY, requestId, "{\"session_id\":\"session_id\"}");
        }

        static std::shared_ptr<Directive> getRadioPlayDirective(const std::string& requestId) {
            return buildDirective(Directives::RADIO_PLAY, requestId, "{\"streamUrl\":\"some_url\"}");
        }

        static std::shared_ptr<Directive> getAudioPlayDirective(const std::string& requestId, const std::string& parentReqId = "") {
            YandexIO::Directive::Data data(Directives::AUDIO_PLAY, "server_action", std::optional<proto::AudioChannel>(proto::CONTENT_CHANNEL));
            data.setContext("", requestId, parentReqId, "");
            return std::make_shared<Directive>(std::move(data));
        }

        static std::shared_ptr<Directive> getListenDirective(const std::string& requestId) {
            YandexIO::Directive::Data data(Directives::LISTEN, "local_action");
            data.requestId = requestId;
            auto request = VinsRequest::createListenRequest(data, VinsRequest::createHardwareButtonClickEventSource());
            return std::make_shared<AliceRequestDirective>(std::move(request), nullptr, true);
        }

        void resetTest() {
            capability = std::make_shared<AlarmCapability>(
                worker,
                getDeviceForTests(),
                ipcFactoryForTests(),
                std::weak_ptr<YandexIO::IRemotingRegistry>(),
                directiveProcessorMock,
                aliceCapabilityMock,
                filePlayerCapabilityMock,
                playbackCapabilityMock,
                deviceStateCapabilityMock);
            capability->addListener(alarmListener);
        }

    private:
        void init() {
            worker = std::make_shared<TestCallbackQueue>();

            alarmConnectorMock = ipcFactoryForTests()->allocateGMockIpcConnector("alarmd");

            alarmListener = std::make_shared<MockIAlarmCapabilityListener>();

            directiveProcessorMock = std::make_shared<MockIDirectiveProcessor>();
            aliceCapabilityMock = std::make_shared<MockIAliceCapability>();
            filePlayerCapabilityMock = std::make_shared<MockIFilePlayerCapability>();
            playbackCapabilityMock = std::make_shared<MockPlaybackControlCapability>();
            deviceStateCapabilityMock = std::make_shared<MockDeviceStateCapability>();

            mockAliced = createIpcServerForTests("aliced");
            mockAliced->setMessageHandler([this](const auto& message, auto& /*connection*/) {
                if (message->has_directive()) {
                    auto directive = YandexIO::Directive::createDirectiveFromProtobuf(message->directive());
                    YIO_LOG_INFO("aliced received directive " << directive->format());
                    directiveProcessorMock->addDirectives({std::move(directive)});
                }
            });
            mockAliced->listenService();

            resetTest();
        }

    public:
        std::shared_ptr<MockIAlarmCapabilityListener> alarmListener;
        std::shared_ptr<MockIDirectiveProcessor> directiveProcessorMock;
        std::shared_ptr<MockIAliceCapability> aliceCapabilityMock;
        std::shared_ptr<MockIFilePlayerCapability> filePlayerCapabilityMock;
        std::shared_ptr<MockPlaybackControlCapability> playbackCapabilityMock;
        std::shared_ptr<MockDeviceStateCapability> deviceStateCapabilityMock;

        std::shared_ptr<AlarmCapability> capability;

        std::shared_ptr<ipc::IServer> mockAliced;
        std::shared_ptr<TestCallbackQueue> worker;

        std::shared_ptr<ipc::mock::MockIConnector> alarmConnectorMock;

    private:
        AliceDeviceState deviceState_{"", nullptr, nullptr, EnvironmentStateHolder("", nullptr)};
    };

} // anonymous namespace

Y_UNIT_TEST_SUITE_F(AlarmCapabilityTest_testPlayingAlarm, AlarmCapabilityFixture) {
    Y_UNIT_TEST(testOnAlarmFired) {
        auto runTest = [this](proto::Alarm alarm) {
            EXPECT_CALL(*aliceCapabilityMock, cancelDialog());

            capability->handleAlarmdMessage(buildAlarmFiredMessage(alarm));
            resetTest();
        };
        {
            runTest(buildAlarm());
        }
        {
            runTest(buildMediaAlarm());
        }
        {
            runTest(buildTimer());
        }
    }

    Y_UNIT_TEST(testBasicAlarmOrTimer_onApproved) {
        auto runTest = [this](proto::Alarm alarm) {
            EXPECT_CALL(*aliceCapabilityMock, cancelDialog());

            capability->handleAlarmdMessage(buildAlarmFiredMessage(alarm));

            expectAlarmStartDirective();
            expectPlaySoundFile(alarm.id());
            expectOnAlarmStarted(alarm);

            capability->handleAlarmdMessage(buildAlarmApprovedMessage(alarm));

            resetTest();
        };
        {
            runTest(buildAlarm());
        }
        {
            runTest(buildTimer());
        }
    }

    Y_UNIT_TEST(testBasicAlarmOrTimer_onStopped) {
        auto runTest = [this](proto::Alarm alarm) {
            EXPECT_CALL(*aliceCapabilityMock, cancelDialog());

            capability->handleAlarmdMessage(buildAlarmFiredMessage(alarm));

            expectAlarmStartDirective();
            expectPlaySoundFile(alarm.id());
            expectOnAlarmStarted(alarm);

            capability->handleAlarmdMessage(buildAlarmApprovedMessage(alarm));

            expectStopSoundFile();
            expectOnAlarmStopped();

            capability->handleAlarmdMessage(buildAlarmStoppedMessage(alarm));

            resetTest();
        };
        {
            runTest(buildAlarm());
        }
        {
            runTest(buildTimer());
        }
    }

    Y_UNIT_TEST(testBasicAlarmOrTimer_stopAlarmByDirective_dialogChannel) {
        auto runTest = [this](proto::Alarm alarm) {
            EXPECT_CALL(*aliceCapabilityMock, cancelDialog());

            capability->handleAlarmdMessage(buildAlarmFiredMessage(alarm));

            expectAlarmStartDirective();
            expectPlaySoundFile(alarm.id());
            expectOnAlarmStarted(alarm);

            capability->handleAlarmdMessage(buildAlarmApprovedMessage(alarm));

            EXPECT_CALL(*alarmConnectorMock,
                        sendMessage(Matcher<const quasar::ipc::SharedMessage&>(VerifyStopAlarmMessage(true))));
            auto list = std::list<std::shared_ptr<Directive>>{getListenDirective("someRequestId")};
            capability->preprocessDirectives(list);
            handleDirectives(list);

            expectStopSoundFile();
            expectOnAlarmStopped();

            capability->handleAlarmdMessage(buildAlarmStoppedMessage(alarm, true));

            resetTest();
        };
        {
            runTest(buildAlarm());
        }
        {
            runTest(buildTimer());
        }
    }

    Y_UNIT_TEST(testBasicAlarmOrTimer_stopAlarmByDirective_serverAlarmStop) {
        auto runTest = [this](proto::Alarm alarm) {
            EXPECT_CALL(*aliceCapabilityMock, cancelDialog());

            capability->handleAlarmdMessage(buildAlarmFiredMessage(alarm));

            expectAlarmStartDirective();
            expectPlaySoundFile(alarm.id());
            expectOnAlarmStarted(alarm);

            capability->handleAlarmdMessage(buildAlarmApprovedMessage(alarm));

            EXPECT_CALL(*alarmConnectorMock,
                        sendMessage(Matcher<const quasar::ipc::SharedMessage&>(VerifyStopAlarmMessage(true))));

            auto list = std::list<std::shared_ptr<Directive>>{buildDirective(Directives::ALARM_STOP)};
            capability->preprocessDirectives(list);
            handleDirectives(list);

            expectStopSoundFile();
            expectOnAlarmStopped();

            capability->handleAlarmdMessage(buildAlarmStoppedMessage(alarm, true));

            resetTest();
        };
        {
            runTest(buildAlarm());
        }
        {
            runTest(buildTimer());
        }
    }

    Y_UNIT_TEST(testBasicAlarmOrTimer_mediaDirectivesDontStopAlarm) {
        auto runTest = [this](proto::Alarm alarm) {
            EXPECT_CALL(*aliceCapabilityMock, cancelDialog());

            expectAlarmStartDirective();
            expectPlaySoundFile(alarm.id());
            expectOnAlarmStarted(alarm);

            capability->handleAlarmdMessage(buildAlarmFiredMessage(alarm));
            capability->handleAlarmdMessage(buildAlarmApprovedMessage(alarm));

            EXPECT_CALL(*alarmConnectorMock,
                        sendMessage(Matcher<const quasar::ipc::SharedMessage&>(VerifyStopAlarmMessage(true))))
                .Times(0);
            EXPECT_CALL(*alarmConnectorMock,
                        sendMessage(Matcher<const quasar::ipc::SharedMessage&>(VerifyStopAlarmMessage(false))))
                .Times(
                    0);

            auto list = std::list<std::shared_ptr<Directive>>{getAudioPlayDirective("someRequestId")};
            capability->preprocessDirectives(list);
            handleDirectives(list);

            resetTest();
        };
        {
            runTest(buildAlarm());
        }
        {
            runTest(buildTimer());
        }
    }

    Y_UNIT_TEST(testMediaAlarm_onApproved_emptyMediaSetting) {
        auto alarm = buildMediaAlarm();

        EXPECT_CALL(*aliceCapabilityMock, cancelDialog());
        capability->handleAlarmdMessage(buildAlarmFiredMessage(alarm));

        expectAlarmStartDirective();

        auto fallbackAlarm = alarm;
        fallbackAlarm.set_alarm_type(proto::Alarm::ALARM);
        expectPlaySoundFile(fallbackAlarm.id());
        expectOnAlarmStarted(fallbackAlarm);

        capability->handleAlarmdMessage(buildAlarmApprovedMessage(alarm));
    }

    Y_UNIT_TEST(testMediaAlarm_onApproved_mediaNotConfirmedInTime_noDirective) {
        const auto settings = getMusicAlarmSetting();
        prepareMediaAlarmSetting(getMusicAlarmSetting());
        auto alarm = buildMediaAlarm();

        EXPECT_CALL(*aliceCapabilityMock, cancelDialog());

        capability->handleAlarmdMessage(buildAlarmFiredMessage(alarm));

        expectAlarmStartDirective();
        expectHandleServerAction(alarm.id(), getMusicAlarmSetting());

        EXPECT_CALL(*aliceCapabilityMock, cancelDialog());

        auto fallbackAlarm = alarm;
        fallbackAlarm.set_alarm_type(proto::Alarm::ALARM);
        expectPlaySoundFile(fallbackAlarm.id());
        expectOnAlarmStarted(fallbackAlarm);

        capability->handleAlarmdMessage(buildAlarmApprovedMessage(alarm));

        worker->pumpDelayedCallback();
    }

    Y_UNIT_TEST(testMediaAlarm_onApproved_mediaNotConfirmedInTime_noConfirm_legacyPlayer) {
        prepareMediaAlarmSetting(getMusicAlarmSetting());
        auto alarm = buildMediaAlarm();

        EXPECT_CALL(*aliceCapabilityMock, cancelDialog());

        capability->handleAlarmdMessage(buildAlarmFiredMessage(alarm));

        expectAlarmStartDirective();

        expectHandleServerAction(alarm.id(), getMusicAlarmSetting());

        capability->handleAlarmdMessage(buildAlarmApprovedMessage(alarm));

        auto list = std::list<std::shared_ptr<Directive>>{getMusicPlayDirective(alarm.id())};
        capability->preprocessDirectives(list);
        UNIT_ASSERT_VALUES_EQUAL(list.size(), 1);
        ASSERT_TRUE(verifyAlarmMusicPlayDirective(list.front(), alarm.id()));
        capability->onDirectiveHandled(list.front());

        std::promise<void> wait;
        EXPECT_CALL(*playbackCapabilityMock, pause).WillOnce([&]() {
            wait.set_value();
        });

        auto fallbackAlarm = alarm;
        fallbackAlarm.set_alarm_type(proto::Alarm::ALARM);
        expectPlaySoundFile(fallbackAlarm.id());
        expectOnAlarmStarted(fallbackAlarm);

        worker->pumpDelayedCallback();
        wait.get_future().get();
    }

    Y_UNIT_TEST(testMediaAlarm_onApproved_mediaNotConfirmedInTime_noConfirm_audioPlayer) {
        prepareMediaAlarmSetting(getMusicAlarmSetting());
        auto alarm = buildMediaAlarm();

        EXPECT_CALL(*aliceCapabilityMock, cancelDialog());

        capability->handleAlarmdMessage(buildAlarmFiredMessage(alarm));

        expectAlarmStartDirective();

        expectHandleServerAction(alarm.id(), getMusicAlarmSetting());

        capability->handleAlarmdMessage(buildAlarmApprovedMessage(alarm));

        auto list = std::list<std::shared_ptr<Directive>>{getAudioPlayDirective(alarm.id())};
        capability->preprocessDirectives(list);
        UNIT_ASSERT_VALUES_EQUAL(list.size(), 1);
        ASSERT_TRUE(verifyAlarmAudioPlayDirective(list.front(), alarm.id()));
        capability->onDirectiveHandled(list.front());

        std::promise<void> wait;
        EXPECT_CALL(*playbackCapabilityMock, pause).WillOnce([&]() {
            wait.set_value();
        });

        auto fallbackAlarm = alarm;
        fallbackAlarm.set_alarm_type(proto::Alarm::ALARM);
        expectPlaySoundFile(fallbackAlarm.id());
        expectOnAlarmStarted(fallbackAlarm);

        worker->pumpDelayedCallback();
        wait.get_future().get();
    }

    Y_UNIT_TEST(testMediaAlarm_onApproved_audioPlayerFailed) {
        prepareMediaAlarmSetting(getMusicAlarmSetting());
        auto alarm = buildMediaAlarm();

        EXPECT_CALL(*aliceCapabilityMock, cancelDialog());

        capability->handleAlarmdMessage(buildAlarmFiredMessage(alarm));

        expectAlarmStartDirective();

        expectHandleServerAction(alarm.id(), getMusicAlarmSetting());

        capability->handleAlarmdMessage(buildAlarmApprovedMessage(alarm));

        auto list = std::list<std::shared_ptr<Directive>>{getAudioPlayDirective(alarm.id())};
        capability->preprocessDirectives(list);
        UNIT_ASSERT_VALUES_EQUAL(list.size(), 1);
        ASSERT_TRUE(verifyAlarmAudioPlayDirective(list.front(), alarm.id()));
        capability->onDirectiveHandled(list.front());

        auto fallbackAlarm = alarm;
        fallbackAlarm.set_alarm_type(proto::Alarm::ALARM);
        expectPlaySoundFile(fallbackAlarm.id());
        expectOnAlarmStarted(fallbackAlarm);
        capability->onDirectiveCompleted(list.front(), YandexIO::IDirectiveProcessorListener::Result::FAIL);

        worker->pumpDelayedCallback();
    }

    Y_UNIT_TEST(testMediaAlarm_onApproved_mediaConfirmed_music) {
        prepareMediaAlarmSetting(getMusicAlarmSetting());
        auto alarm = buildMediaAlarm();

        EXPECT_CALL(*aliceCapabilityMock, cancelDialog());

        capability->handleAlarmdMessage(buildAlarmFiredMessage(alarm));

        expectAlarmStartDirective();

        expectHandleServerAction(alarm.id(), getMusicAlarmSetting());

        capability->handleAlarmdMessage(buildAlarmApprovedMessage(alarm));

        auto list = std::list<std::shared_ptr<Directive>>{getMusicPlayDirective(alarm.id())};
        capability->preprocessDirectives(list);
        UNIT_ASSERT_VALUES_EQUAL(list.size(), 1);
        ASSERT_TRUE(verifyAlarmMusicPlayDirective(list.front(), alarm.id()));
        capability->onDirectiveHandled(list.front());

        expectOnAlarmStarted(alarm);
        capability->onDirectiveStarted(list.front());
    }

    Y_UNIT_TEST(testMediaAlarm_onApproved_mediaConfirmed_radio) {
        prepareMediaAlarmSetting(getRadioAlarmSetting());
        auto alarm = buildMediaAlarm();

        EXPECT_CALL(*aliceCapabilityMock, cancelDialog());

        capability->handleAlarmdMessage(buildAlarmFiredMessage(alarm));

        expectAlarmStartDirective();

        expectHandleServerAction(alarm.id(), getMusicAlarmSetting());

        capability->handleAlarmdMessage(buildAlarmApprovedMessage(alarm));

        auto list = std::list<std::shared_ptr<Directive>>{getRadioPlayDirective(alarm.id())};
        capability->preprocessDirectives(list);
        UNIT_ASSERT_VALUES_EQUAL(list.size(), 1);
        ASSERT_TRUE(verifyAlarmRadioPlayDirective(list.front(), alarm.id()));
        capability->onDirectiveHandled(list.front());

        expectOnAlarmStarted(alarm);
        capability->onDirectiveStarted(list.front());
    }

    Y_UNIT_TEST(testMediaAlarm_onApproved_mediaConfirmed_audioPlayer) {
        prepareMediaAlarmSetting(getMusicAlarmSetting());
        auto alarm = buildMediaAlarm();

        EXPECT_CALL(*aliceCapabilityMock, cancelDialog());

        capability->handleAlarmdMessage(buildAlarmFiredMessage(alarm));

        expectAlarmStartDirective();

        expectHandleServerAction(alarm.id(), getMusicAlarmSetting());

        capability->handleAlarmdMessage(buildAlarmApprovedMessage(alarm));

        auto list = std::list<std::shared_ptr<Directive>>{getAudioPlayDirective(alarm.id())};
        capability->preprocessDirectives(list);
        UNIT_ASSERT_VALUES_EQUAL(list.size(), 1);
        ASSERT_TRUE(verifyAlarmAudioPlayDirective(list.front(), alarm.id()));
        capability->onDirectiveHandled(list.front());

        expectOnAlarmStarted(alarm);
        capability->onDirectiveStarted(list.front());
    }

    Y_UNIT_TEST(testMediaAlarm_onStopped_stopMedia_legacyPlayer) {
        prepareMediaAlarmSetting(getMusicAlarmSetting());
        auto alarm = buildMediaAlarm();

        EXPECT_CALL(*aliceCapabilityMock, cancelDialog());

        capability->handleAlarmdMessage(buildAlarmFiredMessage(alarm));

        expectAlarmStartDirective();

        capability->handleAlarmdMessage(buildAlarmApprovedMessage(alarm));

        auto list = std::list<std::shared_ptr<Directive>>{getMusicPlayDirective(alarm.id())};
        capability->preprocessDirectives(list);
        capability->onDirectiveHandled(list.front());

        expectOnAlarmStarted(alarm);
        capability->onDirectiveStarted(list.front());

        expectOnAlarmStopped();
        std::promise<void> wait;
        EXPECT_CALL(*playbackCapabilityMock, pause).WillOnce([&]() {
            wait.set_value();
        });

        capability->handleAlarmdMessage(buildAlarmStoppedMessage(alarm, true));
        wait.get_future().get();
    }

    Y_UNIT_TEST(testMediaAlarm_onStopped_stopMedia_audioPlayer) {
        prepareMediaAlarmSetting(getMusicAlarmSetting());
        auto alarm = buildMediaAlarm();

        EXPECT_CALL(*aliceCapabilityMock, cancelDialog());

        capability->handleAlarmdMessage(buildAlarmFiredMessage(alarm));

        expectAlarmStartDirective();

        capability->handleAlarmdMessage(buildAlarmApprovedMessage(alarm));

        auto list = std::list<std::shared_ptr<Directive>>{getAudioPlayDirective(alarm.id())};
        capability->preprocessDirectives(list);
        capability->onDirectiveHandled(list.front());

        expectOnAlarmStarted(alarm);
        capability->onDirectiveStarted(list.front());

        expectOnAlarmStopped();
        std::promise<void> wait;
        EXPECT_CALL(*playbackCapabilityMock, pause).WillOnce([&]() {
            wait.set_value();
        });

        capability->handleAlarmdMessage(buildAlarmStoppedMessage(alarm, true));
        wait.get_future().get();
    }

    Y_UNIT_TEST(testMediaAlarm_onStopped_dontStopMedia_legacyPlayer) {
        prepareMediaAlarmSetting(getMusicAlarmSetting());
        auto alarm = buildMediaAlarm();

        EXPECT_CALL(*aliceCapabilityMock, cancelDialog());

        capability->handleAlarmdMessage(buildAlarmFiredMessage(alarm));

        expectAlarmStartDirective();

        capability->handleAlarmdMessage(buildAlarmApprovedMessage(alarm));

        auto list = std::list<std::shared_ptr<Directive>>{getMusicPlayDirective(alarm.id())};
        capability->preprocessDirectives(list);
        capability->onDirectiveHandled(list.front());
        capability->onDirectiveStarted(list.front());

        expectOnAlarmStopped();
        EXPECT_CALL(*playbackCapabilityMock, pause).Times(0);

        capability->handleAlarmdMessage(buildAlarmStoppedMessage(alarm, false));
    }

    Y_UNIT_TEST(testMediaAlarm_onStopped_dontStopMedia_audioPlayer) {
        prepareMediaAlarmSetting(getMusicAlarmSetting());
        auto alarm = buildMediaAlarm();

        EXPECT_CALL(*aliceCapabilityMock, cancelDialog());

        capability->handleAlarmdMessage(buildAlarmFiredMessage(alarm));

        expectAlarmStartDirective();

        capability->handleAlarmdMessage(buildAlarmApprovedMessage(alarm));

        auto list = std::list<std::shared_ptr<Directive>>{getAudioPlayDirective(alarm.id())};
        capability->preprocessDirectives(list);
        capability->onDirectiveHandled(list.front());
        capability->onDirectiveStarted(list.front());

        expectOnAlarmStopped();
        EXPECT_CALL(*playbackCapabilityMock, pause).Times(0);

        capability->handleAlarmdMessage(buildAlarmStoppedMessage(alarm, false));
    }

    Y_UNIT_TEST(testMediaAlarm_stopAlarmByDirective_dialogChannel_legacyPlayer) {
        prepareMediaAlarmSetting(getMusicAlarmSetting());
        auto alarm = buildMediaAlarm();

        EXPECT_CALL(*aliceCapabilityMock, cancelDialog());

        capability->handleAlarmdMessage(buildAlarmFiredMessage(alarm));

        expectAlarmStartDirective();

        capability->handleAlarmdMessage(buildAlarmApprovedMessage(alarm));

        auto list = std::list<std::shared_ptr<Directive>>{getMusicPlayDirective(alarm.id())};
        capability->preprocessDirectives(list);
        capability->onDirectiveHandled(list.front());
        capability->onDirectiveStarted(list.front());

        EXPECT_CALL(*alarmConnectorMock, sendMessage(Matcher<const quasar::ipc::SharedMessage&>(VerifyStopAlarmMessage(false))));

        list = std::list<std::shared_ptr<Directive>>{getListenDirective("someRequestId")};
        capability->preprocessDirectives(list);
        handleDirectives(list);

        expectOnAlarmStopped();
        EXPECT_CALL(*playbackCapabilityMock, pause).Times(0);
        capability->handleAlarmdMessage(buildAlarmStoppedMessage(alarm, false));
    }

    Y_UNIT_TEST(testMediaAlarm_stopAlarmByDirective_dialogChannel_audioPlayer) {
        prepareMediaAlarmSetting(getMusicAlarmSetting());
        auto alarm = buildMediaAlarm();

        EXPECT_CALL(*aliceCapabilityMock, cancelDialog());

        capability->handleAlarmdMessage(buildAlarmFiredMessage(alarm));

        expectAlarmStartDirective();

        capability->handleAlarmdMessage(buildAlarmApprovedMessage(alarm));

        auto list = std::list<std::shared_ptr<Directive>>{getAudioPlayDirective(alarm.id())};
        capability->preprocessDirectives(list);
        capability->onDirectiveHandled(list.front());
        capability->onDirectiveStarted(list.front());

        EXPECT_CALL(*alarmConnectorMock, sendMessage(Matcher<const quasar::ipc::SharedMessage&>(VerifyStopAlarmMessage(false))));

        list = std::list<std::shared_ptr<Directive>>{getListenDirective("someRequestId")};
        capability->preprocessDirectives(list);
        handleDirectives(list);

        expectOnAlarmStopped();
        EXPECT_CALL(*playbackCapabilityMock, pause).Times(0);
        capability->handleAlarmdMessage(buildAlarmStoppedMessage(alarm, false));
    }

    Y_UNIT_TEST(testMediaAlarm_stopAlarmByDirective_contentChannel_legacyPlayer) {
        prepareMediaAlarmSetting(getMusicAlarmSetting());
        auto alarm = buildMediaAlarm();

        EXPECT_CALL(*aliceCapabilityMock, cancelDialog());

        capability->handleAlarmdMessage(buildAlarmFiredMessage(alarm));

        expectAlarmStartDirective();

        capability->handleAlarmdMessage(buildAlarmApprovedMessage(alarm));

        auto list = std::list<std::shared_ptr<Directive>>{getMusicPlayDirective(alarm.id())};
        capability->preprocessDirectives(list);
        capability->onDirectiveHandled(list.front());
        capability->onDirectiveStarted(list.front());

        EXPECT_CALL(*alarmConnectorMock, sendMessage(Matcher<const quasar::ipc::SharedMessage&>(VerifyStopAlarmMessage(false))));

        list = std::list<std::shared_ptr<Directive>>{getAudioPlayDirective("someRequestId")};
        capability->preprocessDirectives(list);
        handleDirectives(list);

        expectOnAlarmStopped();
        EXPECT_CALL(*playbackCapabilityMock, pause).Times(0);
        capability->handleAlarmdMessage(buildAlarmStoppedMessage(alarm, false));
    }

    Y_UNIT_TEST(testMediaAlarm_stopAlarmByDirective_contentChannel_audioPlayer) {
        prepareMediaAlarmSetting(getMusicAlarmSetting());
        auto alarm = buildMediaAlarm();

        EXPECT_CALL(*aliceCapabilityMock, cancelDialog());

        capability->handleAlarmdMessage(buildAlarmFiredMessage(alarm));

        expectAlarmStartDirective();

        capability->handleAlarmdMessage(buildAlarmApprovedMessage(alarm));

        auto list = std::list<std::shared_ptr<Directive>>{getAudioPlayDirective(alarm.id())};
        capability->preprocessDirectives(list);
        capability->onDirectiveHandled(list.front());
        capability->onDirectiveStarted(list.front());

        EXPECT_CALL(*alarmConnectorMock, sendMessage(Matcher<const quasar::ipc::SharedMessage&>(VerifyStopAlarmMessage(false))));

        list = std::list<std::shared_ptr<Directive>>{getAudioPlayDirective("someRequestId")};
        capability->preprocessDirectives(list);
        handleDirectives(list);

        expectOnAlarmStopped();
        EXPECT_CALL(*playbackCapabilityMock, pause).Times(0);
        capability->handleAlarmdMessage(buildAlarmStoppedMessage(alarm, false));
    }

    Y_UNIT_TEST(testMediaAlarm_stopAlarmByDirective_serverAlarmStop_legacyPlayer) {
        prepareMediaAlarmSetting(getMusicAlarmSetting());
        auto alarm = buildMediaAlarm();

        EXPECT_CALL(*aliceCapabilityMock, cancelDialog());

        capability->handleAlarmdMessage(buildAlarmFiredMessage(alarm));

        expectAlarmStartDirective();

        capability->handleAlarmdMessage(buildAlarmApprovedMessage(alarm));

        auto list = std::list<std::shared_ptr<Directive>>{getMusicPlayDirective(alarm.id())};
        capability->preprocessDirectives(list);
        capability->onDirectiveHandled(list.front());
        capability->onDirectiveStarted(list.front());

        EXPECT_CALL(*alarmConnectorMock, sendMessage(Matcher<const quasar::ipc::SharedMessage&>(VerifyStopAlarmMessage(true))));

        list = std::list<std::shared_ptr<Directive>>{buildDirective(Directives::ALARM_STOP)};
        capability->preprocessDirectives(list);
        handleDirectives(list);

        expectOnAlarmStopped();
        std::promise<void> wait;
        EXPECT_CALL(*playbackCapabilityMock, pause).WillOnce([&]() {
            wait.set_value();
        });
        capability->handleAlarmdMessage(buildAlarmStoppedMessage(alarm, true));
        wait.get_future().get();
    }

    Y_UNIT_TEST(testMediaAlarm_stopAlarmByDirective_serverAlarmStop_audioPlayer) {
        prepareMediaAlarmSetting(getMusicAlarmSetting());
        auto alarm = buildMediaAlarm();

        EXPECT_CALL(*aliceCapabilityMock, cancelDialog());

        capability->handleAlarmdMessage(buildAlarmFiredMessage(alarm));

        expectAlarmStartDirective();

        capability->handleAlarmdMessage(buildAlarmApprovedMessage(alarm));

        auto list = std::list<std::shared_ptr<Directive>>{getAudioPlayDirective(alarm.id())};
        capability->preprocessDirectives(list);
        capability->onDirectiveHandled(list.front());
        capability->onDirectiveStarted(list.front());

        EXPECT_CALL(*alarmConnectorMock, sendMessage(Matcher<const quasar::ipc::SharedMessage&>(VerifyStopAlarmMessage(true))));

        list = std::list<std::shared_ptr<Directive>>{buildDirective(Directives::ALARM_STOP)};
        capability->preprocessDirectives(list);
        handleDirectives(list);

        expectOnAlarmStopped();
        std::promise<void> wait;
        EXPECT_CALL(*playbackCapabilityMock, pause).WillOnce([&]() {
            wait.set_value();
        });
        capability->handleAlarmdMessage(buildAlarmStoppedMessage(alarm, true));
        wait.get_future().get();
    }

    Y_UNIT_TEST(testMediaAlarm_stopByHandle_legacyPlayer) {
        prepareMediaAlarmSetting(getMusicAlarmSetting());
        auto alarm = buildMediaAlarm();

        EXPECT_CALL(*aliceCapabilityMock, cancelDialog());

        capability->handleAlarmdMessage(buildAlarmFiredMessage(alarm));

        expectAlarmStartDirective();

        capability->handleAlarmdMessage(buildAlarmApprovedMessage(alarm));

        auto list = std::list<std::shared_ptr<Directive>>{getMusicPlayDirective(alarm.id())};
        capability->preprocessDirectives(list);
        capability->onDirectiveHandled(list.front());
        capability->onDirectiveStarted(list.front());

        EXPECT_CALL(*alarmConnectorMock, sendMessage(Matcher<const quasar::ipc::SharedMessage&>(VerifyStopAlarmMessage(true))));
        EXPECT_CALL(*filePlayerCapabilityMock, playSoundFile(_, _, _, _)).Times(0);

        expectAlarmStopDirective();
        capability->stopAlarm();

        expectOnAlarmStopped();
        std::promise<void> wait;
        EXPECT_CALL(*playbackCapabilityMock, pause).WillOnce([&]() {
            wait.set_value();
        });
        capability->handleAlarmdMessage(buildAlarmStoppedMessage(alarm, true));
        wait.get_future().get();
    }

    Y_UNIT_TEST(testMediaAlarm_stopByHandle_audioPlayer) {
        prepareMediaAlarmSetting(getMusicAlarmSetting());
        auto alarm = buildMediaAlarm();

        EXPECT_CALL(*aliceCapabilityMock, cancelDialog());

        capability->handleAlarmdMessage(buildAlarmFiredMessage(alarm));

        expectAlarmStartDirective();

        capability->handleAlarmdMessage(buildAlarmApprovedMessage(alarm));

        auto list = std::list<std::shared_ptr<Directive>>{getAudioPlayDirective(alarm.id())};
        capability->preprocessDirectives(list);
        capability->onDirectiveHandled(list.front());
        capability->onDirectiveStarted(list.front());

        EXPECT_CALL(*alarmConnectorMock, sendMessage(Matcher<const quasar::ipc::SharedMessage&>(VerifyStopAlarmMessage(true))));
        EXPECT_CALL(*filePlayerCapabilityMock, playSoundFile(_, _, _, _)).Times(0);

        expectAlarmStopDirective();
        capability->stopAlarm();

        expectOnAlarmStopped();
        std::promise<void> wait;
        EXPECT_CALL(*playbackCapabilityMock, pause).WillOnce([&]() {
            wait.set_value();
        });
        capability->handleAlarmdMessage(buildAlarmStoppedMessage(alarm, true));
        wait.get_future().get();
    }

    Y_UNIT_TEST(testAudioPlayerAlarm_failedAfterStarted) {
        prepareMediaAlarmSetting(getMusicAlarmSetting());
        auto alarm = buildMediaAlarm();

        EXPECT_CALL(*aliceCapabilityMock, cancelDialog());

        capability->handleAlarmdMessage(buildAlarmFiredMessage(alarm));

        expectAlarmStartDirective();

        expectHandleServerAction(alarm.id(), getMusicAlarmSetting());

        capability->handleAlarmdMessage(buildAlarmApprovedMessage(alarm));

        auto list = std::list<std::shared_ptr<Directive>>{getAudioPlayDirective(alarm.id())};
        capability->preprocessDirectives(list);
        UNIT_ASSERT_VALUES_EQUAL(list.size(), 1);
        ASSERT_TRUE(verifyAlarmAudioPlayDirective(list.front(), alarm.id()));
        capability->onDirectiveHandled(list.front());

        expectOnAlarmStarted(alarm);
        capability->onDirectiveStarted(list.front());

        auto fallbackAlarm = alarm;
        fallbackAlarm.set_alarm_type(proto::Alarm::ALARM);
        expectPlaySoundFile(fallbackAlarm.id());
        expectOnAlarmStarted(fallbackAlarm);
        capability->onDirectiveCompleted(list.front(), YandexIO::IDirectiveProcessorListener::Result::FAIL);
    }

    Y_UNIT_TEST(testAudioPlayerAlarm_completedAndFailed) {
        prepareMediaAlarmSetting(getMusicAlarmSetting());
        auto alarm = buildMediaAlarm();

        EXPECT_CALL(*aliceCapabilityMock, cancelDialog());

        capability->handleAlarmdMessage(buildAlarmFiredMessage(alarm));

        expectAlarmStartDirective();

        expectHandleServerAction(alarm.id(), getMusicAlarmSetting());

        capability->handleAlarmdMessage(buildAlarmApprovedMessage(alarm));

        auto list = std::list<std::shared_ptr<Directive>>{getAudioPlayDirective(alarm.id())};
        capability->preprocessDirectives(list);
        ASSERT_TRUE(verifyAlarmAudioPlayDirective(list.front(), alarm.id()));
        capability->onDirectiveHandled(list.front());

        expectOnAlarmStarted(alarm);
        capability->onDirectiveStarted(list.front());
        capability->onDirectiveCompleted(list.front(), YandexIO::IDirectiveProcessorListener::Result::SUCCESS);

        list = std::list<std::shared_ptr<Directive>>{getAudioPlayDirective("some_new_req_id", alarm.id())};
        capability->preprocessDirectives(list);
        ASSERT_TRUE(verifyAlarmAudioPlayDirective(list.front(), alarm.id()));
        capability->onDirectiveHandled(list.front());

        auto fallbackAlarm = alarm;
        fallbackAlarm.set_alarm_type(proto::Alarm::ALARM);
        expectPlaySoundFile(fallbackAlarm.id());
        expectOnAlarmStarted(fallbackAlarm);
        capability->onDirectiveCompleted(list.front(), YandexIO::IDirectiveProcessorListener::Result::FAIL);
    }

    Y_UNIT_TEST(testAudioPlayerAlarm_completedAndStarted) {
        prepareMediaAlarmSetting(getMusicAlarmSetting());
        auto alarm = buildMediaAlarm();

        EXPECT_CALL(*aliceCapabilityMock, cancelDialog());

        capability->handleAlarmdMessage(buildAlarmFiredMessage(alarm));

        expectAlarmStartDirective();

        expectHandleServerAction(alarm.id(), getMusicAlarmSetting());

        capability->handleAlarmdMessage(buildAlarmApprovedMessage(alarm));

        auto list = std::list<std::shared_ptr<Directive>>{getAudioPlayDirective(alarm.id())};
        capability->preprocessDirectives(list);
        ASSERT_TRUE(verifyAlarmAudioPlayDirective(list.front(), alarm.id()));
        capability->onDirectiveHandled(list.front());

        expectOnAlarmStarted(alarm);
        capability->onDirectiveStarted(list.front());
        capability->onDirectiveCompleted(list.front(), YandexIO::IDirectiveProcessorListener::Result::SUCCESS);

        list = std::list<std::shared_ptr<Directive>>{getAudioPlayDirective("some_new_req_id", alarm.id())};
        capability->preprocessDirectives(list);
        ASSERT_TRUE(verifyAlarmAudioPlayDirective(list.front(), alarm.id()));
        capability->onDirectiveHandled(list.front());
        capability->onDirectiveStarted(list.front());

        EXPECT_CALL(*directiveProcessorMock, addDirectives(_)).Times(0);
        worker->pumpDelayedCallback();
    }

    Y_UNIT_TEST(testApproveAlarm_while_soundFileAlarm) {
        EXPECT_CALL(*aliceCapabilityMock, cancelDialog());

        {
            auto alarm = buildAlarm();
            capability->handleAlarmdMessage(buildAlarmFiredMessage(alarm));

            expectAlarmStartDirective();
            expectPlaySoundFile(alarm.id());
            expectOnAlarmStarted(alarm);

            capability->handleAlarmdMessage(buildAlarmApprovedMessage(alarm));
        }

        {
            auto replacingAlarm = buildAlarm();
            replacingAlarm.set_id("new_amazing_id");

            expectStopSoundFile();
            EXPECT_CALL(*alarmConnectorMock, sendMessage(Matcher<const quasar::ipc::SharedMessage&>(VerifyStopAlarmMessage(true)))).Times(0);
            EXPECT_CALL(*alarmConnectorMock, sendMessage(Matcher<const quasar::ipc::SharedMessage&>(VerifyStopAlarmMessage(false)))).Times(0);

            expectAlarmStartDirective();
            expectPlaySoundFile(replacingAlarm.id());
            expectOnAlarmStarted(replacingAlarm);

            capability->handleAlarmdMessage(buildAlarmApprovedMessage(replacingAlarm));
        }
    }

    Y_UNIT_TEST(testApproveAlarm_while_waitForDirectives) {
        prepareMediaAlarmSetting(getMusicAlarmSetting());
        auto alarm = buildMediaAlarm();

        EXPECT_CALL(*aliceCapabilityMock, cancelDialog());

        capability->handleAlarmdMessage(buildAlarmFiredMessage(alarm));

        expectAlarmStartDirective();

        expectHandleServerAction(alarm.id(), getMusicAlarmSetting());

        capability->handleAlarmdMessage(buildAlarmApprovedMessage(alarm));

        auto replacingAlarm = buildMediaAlarm();
        replacingAlarm.set_id("new_amazing_id");

        expectAlarmStartDirective();
        EXPECT_CALL(*aliceCapabilityMock, cancelDialog());

        expectHandleServerAction(replacingAlarm.id(), getMusicAlarmSetting());

        capability->handleAlarmdMessage(buildAlarmApprovedMessage(replacingAlarm));
    }

    Y_UNIT_TEST(testApproveAlarm_while_legacyPlayerAlarm) {
        prepareMediaAlarmSetting(getMusicAlarmSetting());
        auto alarm = buildMediaAlarm();

        EXPECT_CALL(*aliceCapabilityMock, cancelDialog());

        capability->handleAlarmdMessage(buildAlarmFiredMessage(alarm));

        expectAlarmStartDirective();

        expectHandleServerAction(alarm.id(), getMusicAlarmSetting());

        capability->handleAlarmdMessage(buildAlarmApprovedMessage(alarm));

        auto list = std::list<std::shared_ptr<Directive>>{getMusicPlayDirective(alarm.id())};
        capability->preprocessDirectives(list);
        capability->onDirectiveHandled(list.front());
        capability->onDirectiveStarted(list.front());

        auto replacingAlarm = buildMediaAlarm();
        replacingAlarm.set_id("new_amazing_id");

        expectAlarmStartDirective();
        std::promise<void> wait;
        EXPECT_CALL(*playbackCapabilityMock, pause).WillOnce([&]() {
            wait.set_value();
        });

        expectHandleServerAction(replacingAlarm.id(), getMusicAlarmSetting());

        capability->handleAlarmdMessage(buildAlarmApprovedMessage(replacingAlarm));
        wait.get_future().get();
    }

    Y_UNIT_TEST(testAlarmStopDirective_onIdle) {
        EXPECT_CALL(*alarmConnectorMock, sendMessage(Matcher<const quasar::ipc::SharedMessage&>(VerifyStopAnyRemainingMediaMessage()))).Times(0);
        EXPECT_CALL(*directiveProcessorMock, addDirectives(_)).Times(0);

        auto list = std::list<std::shared_ptr<Directive>>{buildDirective(Directives::ALARM_STOP)};
        capability->preprocessDirectives(list);
        handleDirectives(list);
    }

    Y_UNIT_TEST(testAlarmStopDirective_afterMediaAlarm_legacyPlayer) {
        prepareMediaAlarmSetting(getMusicAlarmSetting());
        auto alarm = buildMediaAlarm();

        capability->handleAlarmdMessage(buildAlarmFiredMessage(alarm));
        expectAlarmStartDirective();

        capability->handleAlarmdMessage(buildAlarmApprovedMessage(alarm));

        auto list = std::list<std::shared_ptr<Directive>>{getMusicPlayDirective(alarm.id())};
        capability->preprocessDirectives(list);
        capability->onDirectiveHandled(list.front());
        capability->onDirectiveStarted(list.front());

        expectOnAlarmStopped();
        capability->handleAlarmdMessage(buildAlarmStoppedMessage(alarm, false));

        EXPECT_CALL(*alarmConnectorMock, sendMessage(Matcher<const quasar::ipc::SharedMessage&>(VerifyStopAnyRemainingMediaMessage())));
        std::promise<void> wait;
        EXPECT_CALL(*playbackCapabilityMock, pause).WillOnce([&]() {
            wait.set_value();
        });

        list = std::list<std::shared_ptr<Directive>>{buildDirective(Directives::ALARM_STOP)};
        capability->preprocessDirectives(list);
        handleDirectives(list);
        wait.get_future().get();
    }

    Y_UNIT_TEST(testAlarmStopDirective_afterMediaAlarm_audioPlayer) {
        prepareMediaAlarmSetting(getMusicAlarmSetting());
        auto alarm = buildMediaAlarm();

        capability->handleAlarmdMessage(buildAlarmFiredMessage(alarm));
        expectAlarmStartDirective();

        capability->handleAlarmdMessage(buildAlarmApprovedMessage(alarm));

        auto list = std::list<std::shared_ptr<Directive>>{getAudioPlayDirective(alarm.id())};
        capability->preprocessDirectives(list);
        capability->onDirectiveHandled(list.front());
        capability->onDirectiveStarted(list.front());

        expectOnAlarmStopped();
        capability->handleAlarmdMessage(buildAlarmStoppedMessage(alarm, false));

        EXPECT_CALL(*alarmConnectorMock, sendMessage(Matcher<const quasar::ipc::SharedMessage&>(VerifyStopAnyRemainingMediaMessage())));
        std::promise<void> wait;
        EXPECT_CALL(*playbackCapabilityMock, pause).WillOnce([&]() {
            wait.set_value();
        });

        list = std::list<std::shared_ptr<Directive>>{buildDirective(Directives::ALARM_STOP)};
        capability->preprocessDirectives(list);
        handleDirectives(list);
        wait.get_future().get();
    }
}

namespace {
    MATCHER_P(VerifyAddTimerMessage, fireDelaySec, "description") {
        const proto::QuasarMessage& message = *arg;
        if (!message.has_alarm_message() || !message.alarm_message().has_add_alarm()) {
            *result_listener << " no add_alarm";
            return false;
        }
        auto alarm = message.alarm_message().add_alarm();
        if (!alarm.has_alarm_type() || !alarm.has_id() || !alarm.has_duration_seconds() || !alarm.has_start_timestamp_ms()) {
            *result_listener << " add_alarm missing some fields";
            return false;
        }
        if (alarm.alarm_type() != proto::Alarm::TIMER) {
            *result_listener << " unexpected type";
            return false;
        }
        if (alarm.duration_seconds() != fireDelaySec) {
            *result_listener << " duration_seconds != fireDelaySec";
            return false;
        }
        return true;
    }

    MATCHER_P3(VerifyAddCommandTimerMessage, fireDelaySec, directiveName, directivePayload, "description") {
        const proto::QuasarMessage& message = *arg;
        if (!message.has_alarm_message() || !message.alarm_message().has_add_alarm()) {
            *result_listener << " no add_alarm";
            return false;
        }
        auto alarm = message.alarm_message().add_alarm();
        if (!alarm.has_alarm_type() || !alarm.has_id() || !alarm.has_duration_seconds() || !alarm.has_start_timestamp_ms()) {
            *result_listener << " add_alarm missing some fields";
            return false;
        }
        if (alarm.alarm_type() != proto::Alarm::COMMAND_TIMER) {
            *result_listener << " unexpected type";
            return false;
        }
        if (alarm.duration_seconds() != fireDelaySec) {
            *result_listener << " duration_seconds != fireDelaySec";
            return false;
        }
        if (alarm.command_list().size() == 0) {
            *result_listener << " directives expected";
            return false;
        }
        const auto& directive = alarm.command_list(0);
        if (directive.name() != directiveName) {
            *result_listener << " directive name doesn't match: " << directive.name() << " != " << directiveName;
            return false;
        }
        auto payload = parseJson(directive.payload());
        if (payload != directivePayload) {
            *result_listener << " directive payload doesn't match: " << directive.payload() << " != " << jsonToString(directivePayload);
            return false;
        }
        return true;
    }

    MATCHER_P(VerifyRemoveAlarmMessage, alarmId, "description") {
        const proto::QuasarMessage& message = *arg;
        if (!message.has_alarm_message() || !message.alarm_message().has_remove_alarm_id()) {
            *result_listener << " no remove_alarm_id";
            return false;
        }
        if (message.alarm_message().remove_alarm_id() != alarmId) {
            *result_listener << " remove_alarm_id != alarmId";
            return false;
        }
        return true;
    }

    MATCHER_P(VerifyPauseAlarmMessage, alarmId, "description") {
        const proto::QuasarMessage& message = *arg;
        if (!message.has_alarm_message() || !message.alarm_message().has_pause_alarm_id()) {
            *result_listener << " no pause_alarm_id";
            return false;
        }
        if (message.alarm_message().pause_alarm_id() != alarmId) {
            *result_listener << " pause_alarm_id != alarmId";
            return false;
        }
        return true;
    }

    MATCHER_P(VerifyResumeAlarmMessage, alarmId, "description") {
        const proto::QuasarMessage& message = *arg;
        if (!message.has_alarm_message() || !message.alarm_message().has_resume_alarm_id()) {
            *result_listener << " no resume_alarm_id";
            return false;
        }
        if (message.alarm_message().resume_alarm_id() != alarmId) {
            *result_listener << " resume_alarm_id != alarmId";
            return false;
        }
        return true;
    }

    MATCHER_P(VerifyAlarmSetICalStateMessage, expectedICal, "description") {
        const proto::QuasarMessage& message = *arg;
        if (!message.has_alarms_state() || !message.alarms_state().has_icalendar_state()) {
            *result_listener << " no icalendar_state";
            return false;
        }
        if (message.alarms_state().icalendar_state() != expectedICal) {
            *result_listener << " icalendar_state != expectedICal";
            return false;
        }
        return true;
    }

    MATCHER_P(VerifyAlarmSetSoundMessage, expectedMedia, "description") {
        const proto::QuasarMessage& message = *arg;
        if (!message.has_alarm_set_sound() || !message.alarm_set_sound().has_payload()) {
            *result_listener << " no alarm_set_sound.payload";
            return false;
        }
        if (message.alarm_set_sound().payload() != expectedMedia) {
            *result_listener << " alarm_set_sound.payload != expectedMedia";
            return false;
        }
        return true;
    }

    MATCHER(VerifyAlarmResetSoundMessage, "description") {
        const proto::QuasarMessage& message = *arg;
        if (!message.has_alarm_reset_sound()) {
            *result_listener << " no alarm_reset_sound";
            return false;
        }
        return true;
    }

    MATCHER_P(VerifySetMaxLevelMessage, expectedVolume, "description") {
        const proto::QuasarMessage& message = *arg;
        if (!message.has_alarms_settings() || !message.alarms_settings().has_max_volume_level()) {
            *result_listener << " no max_volume_level";
            return false;
        }
        if (message.alarms_settings().max_volume_level() != expectedVolume) {
            *result_listener << "max_volume_level != expectedVolume";
            return false;
        }
        return true;
    }
} // namespace

Y_UNIT_TEST_SUITE_F(AlarmCapabilityTest_testHandlingAlarmDirectives, AlarmCapabilityFixture) {
    Y_UNIT_TEST(testSetTimerDirective) {
        EXPECT_CALL(*alarmConnectorMock, sendMessage(Matcher<const quasar::ipc::SharedMessage&>(VerifyAddTimerMessage(10))));

        auto directive = buildDirective(Directives::SET_TIMER, makeUUID(), "{\"duration\": 10}");
        capability->handleDirective(directive);
    }

    Y_UNIT_TEST(testSetTimerDirective_withDirectives) {
        const auto dirName = "takeover_humans";
        auto dirPayload = Json::Value();
        dirPayload["sound"] = "evil_laughter";

        EXPECT_CALL(*alarmConnectorMock, sendMessage(Matcher<const quasar::ipc::SharedMessage&>(VerifyAddCommandTimerMessage(10, dirName, dirPayload))));

        std::stringstream payload;
        payload << "{\"duration\": 10, \"directives\": [{\"name\": \"" << dirName << "\", \"payload\": " << jsonToString(dirPayload) << "}]}";
        auto directive = buildDirective(Directives::SET_TIMER, makeUUID(), payload.str());
        capability->handleDirective(directive);
    }

    Y_UNIT_TEST(testCancelTimerDirective) {
        const auto timerId = "timer_id";
        EXPECT_CALL(*alarmConnectorMock, sendMessage(Matcher<const quasar::ipc::SharedMessage&>(VerifyRemoveAlarmMessage(timerId))));

        std::stringstream payload;
        payload << "{\"timer_id\": \"" << timerId << "\"}";
        auto directive = buildDirective(Directives::CANCEL_TIMER, makeUUID(), payload.str());
        capability->handleDirective(directive);
    }

    Y_UNIT_TEST(testPauseTimerDirective) {
        const auto timerId = "timer_id";
        EXPECT_CALL(*alarmConnectorMock, sendMessage(Matcher<const quasar::ipc::SharedMessage&>(VerifyPauseAlarmMessage(timerId))));

        std::stringstream payload;
        payload << "{\"timer_id\": \"" << timerId << "\"}";
        auto directive = buildDirective(Directives::PAUSE_TIMER, makeUUID(), payload.str());
        capability->handleDirective(directive);
    }

    Y_UNIT_TEST(testResumeTimerDirective) {
        const auto timerId = "timer_id";
        EXPECT_CALL(*alarmConnectorMock, sendMessage(Matcher<const quasar::ipc::SharedMessage&>(VerifyResumeAlarmMessage(timerId))));

        std::stringstream payload;
        payload << "{\"timer_id\": \"" << timerId << "\"}";
        auto directive = buildDirective(Directives::RESUME_TIMER, makeUUID(), payload.str());
        capability->handleDirective(directive);
    }

    Y_UNIT_TEST(testAlarmsUpdateDirective) {
        const auto state = "some_nice_ical";
        EXPECT_CALL(*alarmConnectorMock, sendMessage(Matcher<const quasar::ipc::SharedMessage&>(VerifyAlarmSetICalStateMessage(state))));

        std::stringstream payload;
        payload << "{\"state\": \"" << state << "\"}";
        auto directive = buildDirective(Directives::ALARMS_UPDATE, makeUUID(), payload.str());
        capability->handleDirective(directive);
    }

    Y_UNIT_TEST(testAlarmSetSoundDirective) {
        const auto media = "{\"media\":\"some_nice_media_alarm\"}\n";
        EXPECT_CALL(*alarmConnectorMock, sendMessage(Matcher<const quasar::ipc::SharedMessage&>(VerifyAlarmSetSoundMessage(media))));

        auto directive = buildDirective(Directives::ALARM_SET_SOUND, makeUUID(), media);
        capability->handleDirective(directive);
    }

    Y_UNIT_TEST(testAlarmResetSoundDirective) {
        EXPECT_CALL(*alarmConnectorMock, sendMessage(Matcher<const quasar::ipc::SharedMessage&>(VerifyAlarmResetSoundMessage())));

        auto directive = buildDirective(Directives::ALARM_RESET_SOUND);
        capability->handleDirective(directive);
    }

    Y_UNIT_TEST(testAlarmSetMaxLevelDirective) {
        const auto volume = 5;
        EXPECT_CALL(*alarmConnectorMock, sendMessage(Matcher<const quasar::ipc::SharedMessage&>(VerifySetMaxLevelMessage(volume))));

        std::stringstream payload;
        payload << "{\"new_level\": " << std::to_string(volume) << "}";
        auto directive = buildDirective(Directives::ALARM_SET_MAX_LEVEL, makeUUID(), payload.str());
        capability->handleDirective(directive);
    }
}

Y_UNIT_TEST_SUITE_F(AlarmCapabilityTest_testProvidingAlarmState, AlarmCapabilityFixture) {
    Y_UNIT_TEST(testProvidesTimersState) {
        proto::TimersState timersState;
        proto::Alarm* timer = timersState.add_timers();
        timer->set_alarm_type(proto::Alarm::TIMER);
        timer->set_id("magnificent_id");

        EXPECT_CALL(*deviceStateCapabilityMock, setTimersState(_))
            .WillOnce(Invoke([&timersState](const NAlice::TDeviceState::TTimers& _timersState) {
                UNIT_ASSERT_VALUES_EQUAL(timersState.timers().size(), _timersState.GetActiveTimers().size());
                UNIT_ASSERT_VALUES_EQUAL(timersState.timers()[0].id(), _timersState.GetActiveTimers()[0].GetTimerId());
            }));

        auto message = ipc::buildMessage([&timersState](auto& msg) {
            msg.mutable_timers_state()->CopyFrom(timersState);
        });
        capability->handleAlarmdMessage(message);
    }

    Y_UNIT_TEST(testProvidesAlarmsState) {
        const std::string icalendarState = "some beautiful icalendar_string";

        proto::AlarmsSettings alarmsSettings;
        alarmsSettings.set_max_volume_level(5);
        alarmsSettings.set_min_volume_level(2);
        alarmsSettings.set_volume_raise_step_ms(100);

        {
            /// test message with media settings
            proto::AlarmsState alarmsState;
            alarmsState.set_icalendar_state(TString(icalendarState));

            Json::Value mediaAlarmSetting;
            mediaAlarmSetting["server_action"]["data"]["from_alarm"] = true;
            mediaAlarmSetting["server_action"]["name"] = "bass_action";
            mediaAlarmSetting["server_action"]["type"] = "server_action";

            mediaAlarmSetting["sound_alarm_setting"]["type"] = "music";
            mediaAlarmSetting["sound_alarm_setting"]["info"]["artists"] = "the best artist in the world";
            mediaAlarmSetting["sound_alarm_setting"]["info"]["album"] = "the magnificent album";
            alarmsState.set_media_alarm_setting(jsonToString(mediaAlarmSetting));

            alarmsState.mutable_alarms_settings()->CopyFrom(alarmsSettings);

            EXPECT_CALL(*deviceStateCapabilityMock, setICalendar(_))
                .WillOnce(Invoke([&icalendarState](const std::string& _icalendarState) {
                    UNIT_ASSERT_VALUES_EQUAL(icalendarState, _icalendarState);
                }));

            EXPECT_CALL(*deviceStateCapabilityMock, setAlarmState(_))
                .WillOnce(Invoke([&mediaAlarmSetting, &alarmsSettings](const NAlice::TDeviceState::TAlarmState& alarmState) {
                    UNIT_ASSERT(mediaAlarmSetting["sound_alarm_setting"] == convertMessageToJson(alarmState.GetSoundAlarmSetting()).value());
                    UNIT_ASSERT(alarmsSettings.max_volume_level() == static_cast<int32_t>(alarmState.GetMaxSoundLevel()));
                }));

            auto message = ipc::buildMessage([&alarmsState](auto& msg) {
                msg.mutable_alarms_state()->CopyFrom(alarmsState);
            });
            capability->handleAlarmdMessage(message);
        }

        {
            /// test message without mediaSettings
            proto::AlarmsState alarmsState;
            alarmsState.set_icalendar_state(TString(icalendarState));

            alarmsState.mutable_alarms_settings()->CopyFrom(alarmsSettings);

            EXPECT_CALL(*deviceStateCapabilityMock, setICalendar(_))
                .WillOnce(Invoke([&icalendarState](const std::string& _icalendarState) {
                    UNIT_ASSERT_VALUES_EQUAL(icalendarState, _icalendarState);
                }));

            EXPECT_CALL(*deviceStateCapabilityMock, setAlarmState(_))
                .WillOnce(Invoke([&alarmsSettings](const NAlice::TDeviceState::TAlarmState& alarmState) {
                    UNIT_ASSERT(Json::objectValue == convertMessageToJson(alarmState.GetSoundAlarmSetting()).value_or(Json::objectValue));
                    UNIT_ASSERT(alarmsSettings.max_volume_level() == static_cast<int32_t>(alarmState.GetMaxSoundLevel()));
                }));

            auto message = ipc::buildMessage([&alarmsState](auto& msg) {
                msg.mutable_alarms_state()->CopyFrom(alarmsState);
            });
            capability->handleAlarmdMessage(message);
        }
    }
}

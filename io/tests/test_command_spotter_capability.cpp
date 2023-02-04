#include <yandex_io/services/aliced/capabilities/spotter_capability/command_spotter_capability.h>
#include <yandex_io/services/aliced/alice_config/alice_config.h>
#include <yandex_io/services/aliced/device_state/mocks/mock_i_alice_device_state.h>
#include <yandex_io/services/aliced/directive_processor/mocks/mock_i_directive_processor.h>
#include <yandex_io/capabilities/alice/interfaces/mocks/mock_i_alice_capability.h>
#include <yandex_io/capabilities/playback_control/interfaces/mocks/mock_playback_control_capability.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/libs/base/directives.h>

#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>
#include <yandex_io/tests/testlib/test_callback_queue.h>

using namespace quasar;
using namespace testing;
using namespace YandexIO;

namespace {
    class NullQuasarVoiceDialog: public QuasarVoiceDialog {
        void update(const AliceConfig& /*config*/) override {
        }
        void setSettings(::SpeechKit::VoiceServiceSettings /*settings*/) override {
        }
        void setPlayer(::SpeechKit::AudioPlayer::SharedPtr /*player*/) override {
        }
        void setSynchronizeStatePayload(const std::string& /*synchronizeStatePayload*/) override {
        }
        void startVoiceInput(const SpeechKit::UniProxy::Header& /*header*/, const std::string& /*voiceInput*/, bool /*jingle*/) override {
        }
        void startMusicInput(const SpeechKit::UniProxy::Header& /*header*/, const std::string& /*musicInput*/, bool /*jingle*/) override {
        }
        void stopInput() override {
        }
        void startPhraseSpotter() override {
        }
        void startCommandSpotter(SpeechKit::PhraseSpotterSettings /*settings*/) override {
        }
        void stopCommandSpotter() override {
        }
        void cancel(bool /*silent*/) override {
        }
        void playCancelSound() override {
        }
        void prepare(const std::string& /*uuid*/, const std::string& /*yandexUid*/,
                     const std::string& /*timezone*/, const std::string& /*group*/, const std::string& /*subgroup*/) override {
        }
        void sendEvent(const SpeechKit::UniProxy::Header& /*header*/, const std::string& /*payload*/) override {
        }
        void startTextInput(const SpeechKit::UniProxy::Header& /*header*/, const std::string& /*payload*/) override {
        }
        void startInterruptionSpotter() override {
        }
        void stopInterruptionSpotter() override {
        }
        void updatePrevReqId(const std::string& /*requestId*/) override {
        }

        void onTtsStarted(const std::string& /*messageId*/) override {
        }
        void onTtsCompleted(const std::string& /*messageId*/) override {
        }
        void onTtsError(const SpeechKit::Error& /*error*/, const std::string& /*messageId*/) override {
        }
    };

    class Fixture: public QuasarUnitTestFixture {
    public:
        void SetUp(NUnitTest::TTestContext& context) override {
            QuasarUnitTestFixture::SetUp(context);
            init();
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            QuasarUnitTestFixture::TearDown(context);
            fakeSpotterPath_.ForceDelete();
        }

        void init() {
            Json::Value config;
            config["enableCommandSpotters"] = true;
            config["enableSpotters"] = true;

            aliceConfig_ = std::make_shared<AliceConfig>(getDeviceForTests(), config);
            capability_ = std::make_shared<CommandSpotterCapability>(worker_, *aliceConfig_,
                                                                     deviceState_, directiveProcessorMock_, getDeviceForTests()->telemetry(),
                                                                     aliceCapabilityMock_, playbackCapabilityMock_, voiceDialog_, std::weak_ptr<YandexIO::IRemotingRegistry>());

            initModelsPaths();
        }

        void initModelsPaths() {
            quasar::proto::Remoting remoting;
            auto method = remoting.mutable_spotter_capability_method();
            method->set_method(quasar::proto::Remoting::SpotterCapabilityMethod::SET_MODEL_PATHS);

            const std::set<TString> spotters{
                SpotterTypes::GENERAL,
                SpotterTypes::NAVIGATION,
                SpotterTypes::VIDEO,
                SpotterTypes::MUSIC,
                SpotterTypes::STOP};
            for (const auto& spotter : spotters) {
                auto path = method->add_model_path();
                path->set_type(spotter);
                path->set_path(fakeSpotterPath_.GetPath());
            }

            const std::shared_ptr<YandexIO::IRemoteObject> remotingApi = capability_;
            remotingApi->handleRemotingMessage(remoting, nullptr);
        }

        void setAliceState(proto::AliceState::State state) const {
            proto::AliceState aliceState;
            aliceState.set_state(state);

            std::shared_ptr<YandexIO::IAliceCapabilityListener> aliceApi = capability_;
            aliceApi->onAliceStateChanged(aliceState);
        }

    public:
        MockIAliceDeviceState deviceState_;
        std::shared_ptr<CommandSpotterCapability> capability_;
        std::shared_ptr<AliceConfig> aliceConfig_;
        const TFsPath fakeSpotterPath_{JoinFsPaths(tryGetRamDrivePath(), SpotterTypes::GENERAL)};

        const std::shared_ptr<QuasarVoiceDialog> voiceDialog_ = std::make_shared<NullQuasarVoiceDialog>();
        const std::shared_ptr<MockIDirectiveProcessor> directiveProcessorMock_ = std::make_shared<MockIDirectiveProcessor>();
        const std::shared_ptr<MockIAliceCapability> aliceCapabilityMock_ = std::make_shared<MockIAliceCapability>();
        const std::shared_ptr<MockPlaybackControlCapability> playbackCapabilityMock_ = std::make_shared<MockPlaybackControlCapability>();
        const std::shared_ptr<TestCallbackQueue> worker_ = std::make_shared<TestCallbackQueue>();
    };

    MATCHER_P(VerifyDirective, directiveName, "description") {
        const std::shared_ptr<Directive>& directive = *arg.begin();
        if (directive == nullptr) {
            *result_listener << "Should not be nullptr";
            return false;
        }
        if (!directive->is(directiveName)) {
            *result_listener << "Directive is not << " << directiveName << ", it is " << directive->getData().name;
            return false;
        }

        return true;
    }

} // anonymous namespace

Y_UNIT_TEST_SUITE_F(CommandSpotterCapabilityTest, Fixture) {
    Y_UNIT_TEST(stop) {
        std::optional<proto::Alarm> playingAlarm;
        NAlice::TDeviceState::TVideo videoState;

        ON_CALL(deviceState_, hasPlayingAlarm())
            .WillByDefault(Invoke([&playingAlarm]() -> bool {
                return playingAlarm.has_value();
            }));

        ON_CALL(deviceState_, getVideoState())
            .WillByDefault(Invoke([&videoState]() -> const NAlice::TDeviceState::TVideo& {
                return videoState;
            }));

        // default behavior -> stop music/video
        setAliceState(proto::AliceState::IDLE);
        EXPECT_CALL(*playbackCapabilityMock_, pause());
        capability_->onCommandPhraseSpotted("стоп");

        // stop alarm
        playingAlarm = proto::Alarm();
        EXPECT_CALL(*directiveProcessorMock_, addDirectives(VerifyDirective(quasar::Directives::ALARM_STOP)));
        capability_->onCommandPhraseSpotted("стоп");

        // stop tts
        playingAlarm.reset();
        setAliceState(proto::AliceState::SPEAKING);
        EXPECT_CALL(*aliceCapabilityMock_, stopConversation());
        capability_->onCommandPhraseSpotted("стоп");
    }

    Y_UNIT_TEST(testNavigation) {
        const auto commandToDirective = std::map<std::string, std::string>{
            {"выше", std::string(Directives::GO_UP)},
            {"ниже", std::string(Directives::GO_DOWN)},
            {"дальше", std::string(Directives::GO_FORWARD)},
            {"назад", std::string(Directives::GO_BACKWARD)}};

        ON_CALL(deviceState_, hasPlayingAlarm())
            .WillByDefault(Invoke([]() -> bool {
                return false;
            }));

        ON_CALL(deviceState_, isLongListenerScreen()).WillByDefault(Invoke([]() {
            return true;
        }));
        NAlice::TDeviceState::TVideo state;
        state.SetCurrentScreen("screen");
        ON_CALL(deviceState_, getVideoState()).WillByDefault(Invoke([&state]() -> const NAlice::TDeviceState::TVideo& {
            return state;
        }));
        setAliceState(proto::AliceState::IDLE);

        for (const auto& [command, directive] : commandToDirective) {
            YIO_LOG_INFO(command);
            EXPECT_CALL(*directiveProcessorMock_, addDirectives(VerifyDirective(directive)));
            capability_->onCommandPhraseSpotted(command);
        }
    }

    Y_UNIT_TEST(testVolume) {
        const auto commandToDirective = std::map<std::string, std::string>{
            {"тише", std::string(Directives::SOUND_QUITER)},
            {"громче", std::string(Directives::SOUND_LOUDER)}};

        for (const auto& [command, directive] : commandToDirective) {
            EXPECT_CALL(*directiveProcessorMock_, addDirectives(VerifyDirective(directive)));
            capability_->onCommandPhraseSpotted(command);
        }
    }

    Y_UNIT_TEST(testContinue) {
        EXPECT_CALL(*playbackCapabilityMock_, play());
        capability_->onCommandPhraseSpotted("продолжи");
    }
}

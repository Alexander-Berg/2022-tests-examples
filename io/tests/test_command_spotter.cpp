#include "yandex_io/protos/quasar_proto_forward.h"

#include <yandex_io/libs/base/directives.h>
#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/ipc/i_server.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/libs/spotter_types/spotter_types.h>
#include <yandex_io/services/aliced/tests/testlib/ske_test_base_fixture.h>
#include <yandex_io/tests/testlib/test_http_server.h>
#include <yandex_io/tests/testlib/test_spotter.h>
#include <yandex_io/tests/testlib/test_utils.h>

#include <util/folder/path.h>

using namespace quasar;
using namespace quasar::proto;
using namespace quasar::TestUtils;

namespace {

    class CommandSpotterTestFixture: public SKETestBaseFixture {
    public:
        using Base = SKETestBaseFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            if (testVoiceDialog != nullptr) {
                testVoiceDialog->stop();
            }
            deleteSpotters();

            Base::TearDown(context);
        }

        void initialize() {
            SKETestBaseFixture::initialize({});

            mockSpotterStorage.onHandlePayload = [](const TestHttpServer::Headers& headers, const std::string& payload,
                                                    TestHttpServer::HttpConnection& handler) {
                Y_UNUSED(headers);
                Y_UNUSED(payload);
                const Spotter spotter = createSpotter();
                YIO_LOG_DEBUG("Sent spotter data, size=" << spotter.gzipData.size());
                handler.doReplay(200, "application/x-tar", spotter.gzipData);
            };

            mockSpotterStorage.start(getPort());

            mockUpdatesd = createIpcServerForTests("updatesd");
            mockWifid = createIpcServerForTests("wifid");

            mockUpdatesd->listenService();
            mockWifid->listenService();
            createSpotters();
        }

        void start() {
            UNIT_ASSERT(testVoiceDialog->waitForState(State::IDLE));
            startEndpoint();
            UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));
        }

        void createSpotters() const {
            generalSpotterPath.MkDirs();
            navigationSpotterPath.MkDirs();
            videoSpotterPath.MkDirs();
            musicSpotterPath.MkDirs();
            stopSpotterPath.MkDirs();
            spotterWorkingDir.MkDirs();
        }

        void deleteSpotters() const {
            generalSpotterPath.ForceDelete();
            navigationSpotterPath.ForceDelete();
            videoSpotterPath.ForceDelete();
            musicSpotterPath.ForceDelete();
            stopSpotterPath.ForceDelete();
            spotterWorkingDir.ForceDelete();
        }

        void setConfig(const Json::Value& config) {
            QuasarMessage msg;
            msg.mutable_user_config_update()->set_config(jsonToString(config));
            sendSync(mockSyncd, msg);
            testVoiceDialog->waitForState(State::SPOTTER);
        }

        void enableCommandSpotters(bool enable) {
            Json::Value config;
            config["system_config"]["enableCommandSpotters"] = enable;
            config["account_config"]["spotter"] = "alisa";
            config["account_config"]["contentAccess"] = "medium";
            QuasarMessage msg;
            msg.mutable_user_config_update()->set_config(jsonToString(config));
            sendSync(mockSyncd, msg);
            UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));

            if (enable) {
                testVoiceDialog->waitForCommandSpotter(SpotterTypes::GENERAL);
            }
        }

    public:
        std::shared_ptr<ipc::IServer> mockUpdatesd;
        std::shared_ptr<ipc::IServer> mockWifid;

        TestHttpServer mockSpotterStorage;

        const TFsPath generalSpotterPath{JoinFsPaths(tryGetRamDrivePath(), SpotterTypes::GENERAL)};
        const TFsPath navigationSpotterPath{JoinFsPaths(tryGetRamDrivePath(), SpotterTypes::NAVIGATION)};
        const TFsPath musicSpotterPath{JoinFsPaths(tryGetRamDrivePath(), SpotterTypes::VIDEO)};
        const TFsPath videoSpotterPath{JoinFsPaths(tryGetRamDrivePath(), SpotterTypes::MUSIC)};
        const TFsPath stopSpotterPath{JoinFsPaths(tryGetRamDrivePath(), SpotterTypes::STOP)};
        const TFsPath spotterWorkingDir{JoinFsPaths(tryGetRamDrivePath(), "activation/alisa")};
    };

    class CommandSpotterTestAutoStartFixture: public CommandSpotterTestFixture {
    public:
        using Base = CommandSpotterTestFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            initialize();
            start();
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            Base::TearDown(context);
        }
    };

} // namespace

Y_UNIT_TEST_SUITE(CommandSpotterTest) {

    Y_UNIT_TEST_F(testCommandNavigationSpotter, CommandSpotterTestAutoStartFixture) {
        enableCommandSpotters(true);

        {
            QuasarMessage appState;
            appState.mutable_app_state()->mutable_screen_state()->set_screen_type(proto::AppState_ScreenType_MORDOVIA_WEBVIEW);
            sendSync(mockInterfaced, appState);
        }

        testVoiceDialog->waitForCommandSpotter(SpotterTypes::NAVIGATION);
        testVoiceDialog->SayCommand("включи");
        UNIT_ASSERT(testVoiceDialog->waitForState(State::LISTENING));
    }

    Y_UNIT_TEST_F(testCommandVideoSpotter, CommandSpotterTestAutoStartFixture) {
        enableCommandSpotters(true);

        {
            QuasarMessage appState;
            appState.mutable_app_state()->mutable_video_state()->set_is_paused(false);
            appState.mutable_app_state()->mutable_video_state()->mutable_video()->mutable_raw();
            sendSync(mockInterfaced, appState);
        }

        testVoiceDialog->waitForCommandSpotter(SpotterTypes::VIDEO);

        testVoiceDialog->SayCommand("включи");
        UNIT_ASSERT(testVoiceDialog->waitForState(State::LISTENING));
    }

    Y_UNIT_TEST_F(testCommandSpottersStartedOnScreenTypeChanging, CommandSpotterTestAutoStartFixture) {
        enableCommandSpotters(true);

        {
            QuasarMessage appState;
            appState.mutable_app_state()->mutable_screen_state()->set_screen_type(proto::AppState_ScreenType_MUSIC_PLAYER);
            sendSync(mockInterfaced, appState);
        }
        testVoiceDialog->waitForCommandSpotter(SpotterTypes::MUSIC);

        {
            QuasarMessage appState;
            appState.mutable_app_state()->mutable_screen_state()->set_screen_type(proto::AppState_ScreenType_VIDEO_PLAYER);
            sendSync(mockInterfaced, appState);
        }
        testVoiceDialog->waitForCommandSpotter(SpotterTypes::VIDEO);

        {
            QuasarMessage appState;
            appState.mutable_app_state()->mutable_screen_state()->set_screen_type(proto::AppState_ScreenType_RADIO_PLAYER);
            sendSync(mockInterfaced, appState);
        }
        testVoiceDialog->waitForCommandSpotter(SpotterTypes::MUSIC);
    }

    Y_UNIT_TEST_F(testCommandTakeFreeAudioFocus, CommandSpotterTestFixture) {
        std::promise<bool> dialogChannelRequested;

        setMediadMessageHandler([&](const auto& message, auto& connection) {
            Y_UNUSED(connection);
            if (message->has_media_request()) {
                if (message->media_request().has_take_audio_focus()) {
                    dialogChannelRequested.set_value(true);
                }
            }
        });

        initialize();
        start();
        enableCommandSpotters(true);

        testVoiceDialog->SayCommand("включи");
        UNIT_ASSERT(testVoiceDialog->waitForState(State::LISTENING));
        UNIT_ASSERT(dialogChannelRequested.get_future().get());
    }

    Y_UNIT_TEST_F(CheckCommandSpotterSettingsFromConfig, CommandSpotterTestAutoStartFixture) {
        Json::Value config;

        {
            Json::Value accountConfig;
            accountConfig["spotter"] = "alisa";
            accountConfig["contentAccess"] = "medium";
            config["account_config"] = std::move(accountConfig);
        }

        Json::Value systemConfig;
        {
            Json::Value voiceDialogSettings;
            voiceDialogSettings["spotterLoggingHeadMillis"] = 9000;
            voiceDialogSettings["spotterLoggingTailMillis"] = 1000;
            systemConfig["voiceDialogSettings"] = std::move(voiceDialogSettings);
            systemConfig["enableCommandSpotters"] = true;
            config["system_config"] = systemConfig;
        }

        setConfig(config);

        {
            const auto generalSpotterSettings = testVoiceDialog->waitForCommandSpotter(SpotterTypes::GENERAL);
            UNIT_ASSERT_VALUES_EQUAL(generalSpotterSettings.soundLoggerSettings.soundLengthBeforeTrigger.count(),
                                     systemConfig["voiceDialogSettings"]["spotterLoggingHeadMillis"].asInt());
            UNIT_ASSERT_VALUES_EQUAL(generalSpotterSettings.soundLoggerSettings.soundLengthAfterTrigger.count(),
                                     systemConfig["voiceDialogSettings"]["spotterLoggingTailMillis"].asInt());
            UNIT_ASSERT_EQUAL(generalSpotterSettings.soundLoggerSettings.soundFormat, SpeechKit::SoundFormat::OPUS);
            UNIT_ASSERT_VALUES_EQUAL(generalSpotterSettings.rareEventPercent, 0);
            UNIT_ASSERT_VALUES_EQUAL(generalSpotterSettings.rareEventSoundLoggerSettings.soundLengthBeforeTrigger.count(), 0);
            UNIT_ASSERT_VALUES_EQUAL(generalSpotterSettings.rareEventSoundLoggerSettings.soundLengthAfterTrigger.count(), 0);
        }

        {
            systemConfig["voiceDialogSettings"]["soundLoggingFormat"] = Json::Value("opus");
            systemConfig["voiceDialogSettings"]["spotterLoggingRareEventPercent"] = Json::Value(5);
            systemConfig["voiceDialogSettings"]["spotterLoggingRareEventHeadMillis"] = Json::Value(5000);
            systemConfig["voiceDialogSettings"]["spotterLoggingRareEventTailMillis"] = Json::Value(1000);
            config["system_config"] = systemConfig;

            setConfig(config);

            {
                QuasarMessage appState;
                appState.mutable_app_state()->mutable_screen_state()->set_screen_type(proto::AppState_ScreenType_MUSIC_PLAYER);
                sendSync(mockInterfaced, appState);
            }
            const auto musicSpotterSettings = testVoiceDialog->waitForCommandSpotter(SpotterTypes::MUSIC);

            UNIT_ASSERT_VALUES_EQUAL(musicSpotterSettings.soundLoggerSettings.soundLengthBeforeTrigger.count(),
                                     systemConfig["voiceDialogSettings"]["spotterLoggingHeadMillis"].asInt());
            UNIT_ASSERT_VALUES_EQUAL(musicSpotterSettings.soundLoggerSettings.soundLengthAfterTrigger.count(),
                                     systemConfig["voiceDialogSettings"]["spotterLoggingTailMillis"].asInt());
            UNIT_ASSERT_EQUAL(musicSpotterSettings.soundLoggerSettings.soundFormat, SpeechKit::SoundFormat::OPUS);
            UNIT_ASSERT_EQUAL(musicSpotterSettings.rareEventSoundLoggerSettings.soundFormat, SpeechKit::SoundFormat::PCM);
            UNIT_ASSERT_VALUES_EQUAL(musicSpotterSettings.rareEventPercent,
                                     systemConfig["voiceDialogSettings"]["spotterLoggingRareEventPercent"].asInt());
            UNIT_ASSERT_VALUES_EQUAL(musicSpotterSettings.rareEventSoundLoggerSettings.soundLengthBeforeTrigger.count(),
                                     systemConfig["voiceDialogSettings"]["spotterLoggingRareEventHeadMillis"].asInt());
            UNIT_ASSERT_VALUES_EQUAL(musicSpotterSettings.rareEventSoundLoggerSettings.soundLengthAfterTrigger.count(),
                                     systemConfig["voiceDialogSettings"]["spotterLoggingRareEventTailMillis"].asInt());
        }
    }
}

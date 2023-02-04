#include <yandex_io/services/aliced/directive_processor/mocks/mock_i_directive_processor.h>
#include <yandex_io/services/aliced/capabilities/playback_control_capability/playback_control_capability.h>
#include <yandex_io/services/aliced/capabilities/multiroom_capability/multiroom_directives.h>
#include <yandex_io/services/aliced/device_state/alice_device_state.h>
#include <yandex_io/tests/testlib/test_callback_queue.h>

#include <yandex_io/libs/base/directives.h>
#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/libs/protobuf_utils/debug.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <yandex_io/protos/capabilities/device_state_capability.pb.h>

using namespace quasar;
using namespace quasar::TestUtils;
using namespace testing;
using namespace YandexIO;

namespace {

    void setAudioClient(bool playing, AliceDeviceState& deviceState) {
        YIO_LOG_INFO("SETTING AUDIO_CLIENT: " << playing);
        NAlice::TCapabilityHolder state;
        auto* audioPlayer = state.MutableDeviceStateCapability()->MutableState()->MutableDeviceState()->MutableAudioPlayer();
        audioPlayer->SetPlayerState(playing ? NAlice::TDeviceState::TAudioPlayer::Playing : NAlice::TDeviceState::TAudioPlayer::Stopped);
        const auto nowMs = getNowTimestampMs();
        audioPlayer->SetLastPlayTimestamp(nowMs);
        audioPlayer->SetLastStopTimestamp(nowMs);
        audioPlayer->MutableCurrentlyPlaying()->SetStreamId("666");

        deviceState.onCapabilityStateChanged(nullptr, state);

        proto::AppState appState;
        appState.mutable_audio_player_state()->mutable_player_descriptor()->set_type(proto::AudioPlayerDescriptor::AUDIO);
        deviceState.setAppState(appState);
    }

    void setRadio(bool playing, AliceDeviceState& deviceState) {
        YIO_LOG_INFO("SETTING RADIO: " << playing);
        Json::Value radioState;
        radioState["player"]["pause"] = !playing;
        radioState["player"]["timestamp"] = getNowTimestampMs() / 1000;
        radioState["currently_playing"]["radioId"] = "322";

        NAlice::TCapabilityHolder state;
        auto converted = convertJsonToProtobuf<google::protobuf::Struct>(quasar::jsonToString(radioState)).value();
        state.MutableDeviceStateCapability()->MutableState()->MutableDeviceState()->MutableRadio()->Swap(&converted);
        deviceState.onCapabilityStateChanged(nullptr, state);
    }

    void setMusic(bool playing, AliceDeviceState& deviceState) {
        YIO_LOG_INFO("SETTING MUSIC: " << playing);
        NAlice::TDeviceState::TMusic music;
        music.MutableCurrentlyPlaying()->SetTrackId("777");
        music.MutablePlayer()->SetPause(!playing);
        music.MutablePlayer()->SetTimestamp(getNowTimestampMs() / 1000);

        NAlice::TCapabilityHolder state;
        state.MutableDeviceStateCapability()->MutableState()->MutableDeviceState()->MutableMusic()->Swap(&music);
        deviceState.onCapabilityStateChanged(nullptr, state);
    }

    void setBluetooth(bool playing, AliceDeviceState& deviceState) {
        YIO_LOG_INFO("SETTING BLUETOOTh: " << playing);
        NAlice::TDeviceState::TBluetooth bluetooth;
        bluetooth.AddCurrentConnections()->SetName("kek");
        bluetooth.MutablePlayer()->SetPause(!playing);
        const auto nowMs = getNowTimestampMs() / 1000;
        bluetooth.MutablePlayer()->SetTimestamp(nowMs);
        bluetooth.SetLastPlayTimestamp(nowMs);

        NAlice::TCapabilityHolder state;
        state.MutableDeviceStateCapability()->MutableState()->MutableDeviceState()->MutableBluetooth()->Swap(&bluetooth);
        deviceState.onCapabilityStateChanged(nullptr, state);
    }

} // namespace

namespace {

    struct PlaybackTestCase {
        std::string inputDirective;
        std::string outputDirective;
        std::function<void(bool, AliceDeviceState&)> setterFunc;
        bool playing;
    };

} // namespace

namespace {
    class PlaybackControlCapabilityFixture: public QuasarUnitTestFixture {
    public:
        void SetUp(NUnitTest::TTestContext& context) override {
            QuasarUnitTestFixture::SetUp(context);
            init();
        }

        void runTests(const std::vector<PlaybackTestCase>& cases) {
            for (const auto& testCase : cases) {
                init();
                testCase.setterFunc(testCase.playing, *deviceState);
                EXPECT_CALL(*directiveProcessorMock, addDirectives).WillOnce([&testCase](std::list<std::shared_ptr<Directive>> directives) {
                    UNIT_ASSERT(directives.size() == 1);
                    UNIT_ASSERT_VALUES_EQUAL(directives.front()->getData().name, testCase.outputDirective);
                });
                capability->handleDirective(std::make_shared<Directive>(Directive::Data(testCase.inputDirective, "local_action")));
            }
        }

    protected:
        void init() {
            deviceState = std::make_shared<AliceDeviceState>("", nullptr, nullptr, EnvironmentStateHolder("", nullptr));
            worker = std::make_shared<TestCallbackQueue>();
            directiveProcessorMock = std::make_shared<MockIDirectiveProcessor>();
            glagolConnectorMock = ipcFactoryForTests()->allocateGMockIpcConnector("glagold");
            capability = std::make_shared<PlaybackControlCapability>(
                worker, device_, directiveProcessorMock, *deviceState,
                glagolConnectorMock, nullptr, std::weak_ptr<YandexIO::IRemotingRegistry>());
        }

    public:
        std::shared_ptr<MockIDirectiveProcessor> directiveProcessorMock;
        std::shared_ptr<PlaybackControlCapability> capability;
        std::shared_ptr<TestCallbackQueue> worker;
        std::shared_ptr<AliceDeviceState> deviceState;
        std::shared_ptr<ipc::mock::MockIConnector> glagolConnectorMock;
    };

} // namespace

Y_UNIT_TEST_SUITE_F(PlaybackControlCapabilityTest, PlaybackControlCapabilityFixture) {
    Y_UNIT_TEST(testPlay) {

        std::vector<PlaybackTestCase> cases = {
            PlaybackTestCase{Directives::PLAYBACK_PLAY, Directives::PLAYER_CONTINUE, &setMusic, false},
            PlaybackTestCase{Directives::PLAYBACK_PLAY, Directives::ALICE_REQUEST, &setAudioClient, false},
            PlaybackTestCase{Directives::PLAYBACK_PLAY, Directives::PLAYER_CONTINUE, &setRadio, false},
            PlaybackTestCase{Directives::PLAYBACK_PLAY, Directives::BLUETOOTH_PLAYER_PLAY, &setBluetooth, false},
            PlaybackTestCase{Directives::PLAYBACK_PLAY, Directives::PLAYER_CONTINUE, &setMusic, true},
            PlaybackTestCase{Directives::PLAYBACK_PLAY, Directives::ALICE_REQUEST, &setAudioClient, true},
            PlaybackTestCase{Directives::PLAYBACK_PLAY, Directives::PLAYER_CONTINUE, &setRadio, true},
            PlaybackTestCase{Directives::PLAYBACK_PLAY, Directives::BLUETOOTH_PLAYER_PLAY, &setBluetooth, true},
        };

        runTests(cases);
    }

    Y_UNIT_TEST(testPause) {

        std::vector<PlaybackTestCase> cases = {
            PlaybackTestCase{Directives::PLAYBACK_PAUSE, Directives::PLAYER_PAUSE, &setMusic, false},
            PlaybackTestCase{Directives::PLAYBACK_PAUSE, Directives::AUDIO_STOP, &setAudioClient, false},
            PlaybackTestCase{Directives::PLAYBACK_PAUSE, Directives::PLAYER_PAUSE, &setRadio, false},
            PlaybackTestCase{Directives::PLAYBACK_PAUSE, Directives::BLUETOOTH_PLAYER_PAUSE, &setBluetooth, false},
            PlaybackTestCase{Directives::PLAYBACK_PAUSE, Directives::PLAYER_PAUSE, &setMusic, true},
            PlaybackTestCase{Directives::PLAYBACK_PAUSE, Directives::AUDIO_STOP, &setAudioClient, true},
            PlaybackTestCase{Directives::PLAYBACK_PAUSE, Directives::PLAYER_PAUSE, &setRadio, true},
            PlaybackTestCase{Directives::PLAYBACK_PAUSE, Directives::BLUETOOTH_PLAYER_PAUSE, &setBluetooth, true},
        };

        runTests(cases);
    }

    Y_UNIT_TEST(testNext) {

        std::vector<PlaybackTestCase> cases = {
            PlaybackTestCase{Directives::PLAYBACK_NEXT, Directives::PLAYER_NEXT_TRACK, &setMusic, false},
            PlaybackTestCase{Directives::PLAYBACK_NEXT, Directives::AUDIO_CLIENT_NEXT_TRACK, &setAudioClient, false},
            PlaybackTestCase{Directives::PLAYBACK_NEXT, Directives::BLUETOOTH_PLAYER_NEXT, &setBluetooth, false},
            PlaybackTestCase{Directives::PLAYBACK_NEXT, Directives::PLAYER_NEXT_TRACK, &setMusic, true},
            PlaybackTestCase{Directives::PLAYBACK_NEXT, Directives::AUDIO_CLIENT_NEXT_TRACK, &setAudioClient, true},
            PlaybackTestCase{Directives::PLAYBACK_NEXT, Directives::BLUETOOTH_PLAYER_NEXT, &setBluetooth, true},
        };

        runTests(cases);
    }

    Y_UNIT_TEST(testPrev) {

        std::vector<PlaybackTestCase> cases = {
            PlaybackTestCase{Directives::PLAYBACK_PREV, Directives::PLAYER_PREVIOUS_TRACK, &setMusic, false},
            PlaybackTestCase{Directives::PLAYBACK_PREV, Directives::AUDIO_CLIENT_PREV_TRACK, &setAudioClient, false},
            PlaybackTestCase{Directives::PLAYBACK_PREV, Directives::BLUETOOTH_PLAYER_PREV, &setBluetooth, false},
            PlaybackTestCase{Directives::PLAYBACK_PREV, Directives::PLAYER_PREVIOUS_TRACK, &setMusic, true},
            PlaybackTestCase{Directives::PLAYBACK_PREV, Directives::AUDIO_CLIENT_PREV_TRACK, &setAudioClient, true},
            PlaybackTestCase{Directives::PLAYBACK_PREV, Directives::BLUETOOTH_PLAYER_PREV, &setBluetooth, true},
        };

        runTests(cases);
    }

    Y_UNIT_TEST(testLike) {

        std::vector<PlaybackTestCase> cases = {
            PlaybackTestCase{Directives::PLAYBACK_LIKE, Directives::PLAYER_LIKE, &setMusic, false},
            PlaybackTestCase{Directives::PLAYBACK_LIKE, Directives::PLAYER_LIKE, &setMusic, true},
        };

        runTests(cases);
    }

    Y_UNIT_TEST(testDislike) {

        std::vector<PlaybackTestCase> cases = {
            PlaybackTestCase{Directives::PLAYBACK_DISLIKE, Directives::PLAYER_DISLIKE, &setMusic, false},
            PlaybackTestCase{Directives::PLAYBACK_DISLIKE, Directives::PLAYER_DISLIKE, &setMusic, true},
        };

        runTests(cases);
    }

    Y_UNIT_TEST(testRewind) {

        std::vector<PlaybackTestCase> cases = {
            PlaybackTestCase{Directives::PLAYBACK_REWIND, Directives::PLAYER_REWIND, &setMusic, false},
            PlaybackTestCase{Directives::PLAYBACK_REWIND, Directives::AUDIO_REWIND, &setAudioClient, false},
            PlaybackTestCase{Directives::PLAYBACK_REWIND, Directives::PLAYER_REWIND, &setMusic, true},
            PlaybackTestCase{Directives::PLAYBACK_REWIND, Directives::AUDIO_REWIND, &setAudioClient, true},
        };

        runTests(cases);
    }

    Y_UNIT_TEST(testTogglePlayPause) {
        std::vector<PlaybackTestCase> cases = {
            PlaybackTestCase{Directives::PLAYBACK_TOGGLE_PLAY_PAUSE, Directives::ALICE_REQUEST, &setAudioClient, false},
            PlaybackTestCase{Directives::PLAYBACK_TOGGLE_PLAY_PAUSE, Directives::AUDIO_STOP, &setAudioClient, true},
            PlaybackTestCase{Directives::PLAYBACK_TOGGLE_PLAY_PAUSE, Directives::BLUETOOTH_PLAYER_PLAY, &setBluetooth, false},
            PlaybackTestCase{Directives::PLAYBACK_TOGGLE_PLAY_PAUSE, Directives::BLUETOOTH_PLAYER_PAUSE, &setBluetooth, true},
            PlaybackTestCase{Directives::PLAYBACK_TOGGLE_PLAY_PAUSE, Directives::PLAYER_CONTINUE, &setMusic, false},
            PlaybackTestCase{Directives::PLAYBACK_TOGGLE_PLAY_PAUSE, Directives::PLAYER_PAUSE, &setMusic, true},
            PlaybackTestCase{Directives::PLAYBACK_TOGGLE_PLAY_PAUSE, Directives::PLAYER_CONTINUE, &setRadio, false},
            PlaybackTestCase{Directives::PLAYBACK_TOGGLE_PLAY_PAUSE, Directives::PLAYER_PAUSE, &setRadio, true},
        };

        runTests(cases);
    }
}

#include <yandex_io/libs/telemetry/mock/mock_telemetry.h>
#include <yandex_io/services/aliced/capabilities/audio_player_capability/media_stats.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

#include <memory>

using namespace quasar;
using namespace testing;

namespace {

    class MediaStatsFixture: public QuasarUnitTestFixture {
    public:
        void SetUp(NUnitTest::TTestContext& context) override {
            QuasarUnitTestFixture::SetUp(context);
            setDeviceForTests(std::make_unique<YandexIO::Device>(
                QuasarUnitTestFixture::makeTestDeviceId(),
                QuasarUnitTestFixture::makeTestConfiguration(),
                mockTelemetry,
                QuasarUnitTestFixture::makeTestHAL()));
            enableLoggingToTelemetry(mockTelemetry);
            mediaStats = std::make_unique<MediaStats>(getDeviceForTests());
        }

    public:
        std::unique_ptr<MediaStats> mediaStats;
        std::shared_ptr<YandexIO::MockTelemetry> mockTelemetry = std::make_shared<StrictMock<YandexIO::MockTelemetry>>();
    };

    proto::AudioClientEvent buildAudioClientStartedEvent(const std::string& vinsRequesId) {
        proto::AudioClientEvent event;
        event.set_event(proto::AudioClientEvent::STATE_CHANGED);
        event.set_state(proto::AudioClientState::PLAYING);
        event.mutable_player_descriptor()->set_type(proto::AudioPlayerDescriptor::AUDIO);
        event.mutable_audio()->mutable_analytics_context()->set_vins_request_id(TString(vinsRequesId));
        return event;
    }

    proto::AudioClientEvent buildAudioClientFinishedEvent(const std::string& vinsRequesId) {
        proto::AudioClientEvent event;
        event.set_event(proto::AudioClientEvent::STATE_CHANGED);
        event.set_state(proto::AudioClientState::FINISHED);
        event.mutable_player_descriptor()->set_type(proto::AudioPlayerDescriptor::AUDIO);
        event.mutable_audio()->mutable_analytics_context()->set_vins_request_id(TString(vinsRequesId));
        return event;
    }

    proto::LegacyPlayerStateChanged buildMusicPlayerStartedEvent(const std::string& vinsRequesId) {
        proto::LegacyPlayerStateChanged event;
        event.set_state(proto::LegacyPlayerStateChanged::STARTED);
        event.set_player_type(proto::LegacyPlayerStateChanged::YANDEX_MUSIC);
        event.set_vins_request_id(TString(vinsRequesId));
        return event;
    }

} // anonymous namespace

Y_UNIT_TEST_SUITE(MediaStatsTest) {
    Y_UNIT_TEST_F(testAudioClientStats, MediaStatsFixture) {
        {
            InSequence s;
            EXPECT_CALL(*mockTelemetry, reportEvent("audioClientStartedFromVoiceInput", _, _)).Times(1);
            EXPECT_CALL(*mockTelemetry, reportEvent("audioClientStartedFromTtsEnd", _, _)).Times(1);
            EXPECT_CALL(*mockTelemetry, reportEvent("audioClientStartedFromPrev", _, _)).Times(0);
            EXPECT_CALL(*mockTelemetry, reportEvent("musicPlayerStartedFromVoiceInput", _, _)).Times(0);
            EXPECT_CALL(*mockTelemetry, reportEvent("musicPlayerStartedFromTtsEnd", _, _)).Times(0);
            EXPECT_CALL(*mockTelemetry, reportEvent("musicPlayerStartedFromPrev", _, _)).Times(0);
        }

        mediaStats->onVoiceInputStarted("vinsRequestId");
        mediaStats->onTtsCompleted();
        mediaStats->onAudioClientEvent(buildAudioClientStartedEvent("vinsRequestId"));
    }

    Y_UNIT_TEST_F(testAudioClientStatsWithoutTts, MediaStatsFixture) {
        {
            EXPECT_CALL(*mockTelemetry, reportEvent("audioClientStartedFromVoiceInput", _, _)).Times(1);
            EXPECT_CALL(*mockTelemetry, reportEvent("audioClientStartedFromTtsEnd", _, _)).Times(0);
            EXPECT_CALL(*mockTelemetry, reportEvent("audioClientStartedFromPrev", _, _)).Times(0);
            EXPECT_CALL(*mockTelemetry, reportEvent("musicPlayerStartedFromVoiceInput", _, _)).Times(0);
            EXPECT_CALL(*mockTelemetry, reportEvent("musicPlayerStartedFromTtsEnd", _, _)).Times(0);
            EXPECT_CALL(*mockTelemetry, reportEvent("musicPlayerStartedFromPrev", _, _)).Times(0);
        }

        mediaStats->onVoiceInputStarted("vinsRequestId");
        mediaStats->onAudioClientEvent(buildAudioClientStartedEvent("vinsRequestId"));
    }

    Y_UNIT_TEST_F(testAudioClientStartedFromPrev, MediaStatsFixture) {
        {
            InSequence s;
            EXPECT_CALL(*mockTelemetry, reportEvent("audioClientStartedFromVoiceInput", _, _)).Times(1);
            EXPECT_CALL(*mockTelemetry, reportEvent("audioClientStartedFromTtsEnd", _, _)).Times(1);
            EXPECT_CALL(*mockTelemetry, reportEvent("audioClientStartedFromPrev", _, _)).Times(2);
            EXPECT_CALL(*mockTelemetry, reportEvent("musicPlayerStartedFromVoiceInput", _, _)).Times(0);
            EXPECT_CALL(*mockTelemetry, reportEvent("musicPlayerStartedFromTtsEnd", _, _)).Times(0);
            EXPECT_CALL(*mockTelemetry, reportEvent("musicPlayerStartedFromPrev", _, _)).Times(0);
        }

        mediaStats->onVoiceInputStarted("firstSong");
        mediaStats->onTtsCompleted();
        mediaStats->onAudioClientEvent(buildAudioClientStartedEvent("firstSong"));
        mediaStats->onAudioClientEvent(buildAudioClientFinishedEvent("firstSong"));
        mediaStats->onAudioClientEvent(buildAudioClientStartedEvent("secondSong"));
        mediaStats->onAudioClientEvent(buildAudioClientFinishedEvent("secondSong"));
        mediaStats->onAudioClientEvent(buildAudioClientStartedEvent("thirdSong"));
    }

    Y_UNIT_TEST_F(testMusicPlayerStats, MediaStatsFixture) {
        {
            InSequence s;
            EXPECT_CALL(*mockTelemetry, reportEvent("audioClientStartedFromVoiceInput", _, _)).Times(0);
            EXPECT_CALL(*mockTelemetry, reportEvent("audioClientStartedFromTtsEnd", _, _)).Times(0);
            EXPECT_CALL(*mockTelemetry, reportEvent("audioClientStartedFromPrev", _, _)).Times(0);
            EXPECT_CALL(*mockTelemetry, reportEvent("musicPlayerStartedFromVoiceInput", _, _)).Times(1);
            EXPECT_CALL(*mockTelemetry, reportEvent("musicPlayerStartedFromTtsEnd", _, _)).Times(1);
            EXPECT_CALL(*mockTelemetry, reportEvent("musicPlayerStartedFromPrev", _, _)).Times(0);
        }

        mediaStats->onVoiceInputStarted("vinsRequestId");
        mediaStats->onTtsCompleted();
        mediaStats->onLegacyPlayerStateChanged(buildMusicPlayerStartedEvent("vinsRequestId"));
    }

    Y_UNIT_TEST_F(testMusicPlayerStatsWithoutTts, MediaStatsFixture) {
        {
            EXPECT_CALL(*mockTelemetry, reportEvent("audioClientStartedFromVoiceInput", _, _)).Times(0);
            EXPECT_CALL(*mockTelemetry, reportEvent("audioClientStartedFromTtsEnd", _, _)).Times(0);
            EXPECT_CALL(*mockTelemetry, reportEvent("audioClientStartedFromPrev", _, _)).Times(0);
            EXPECT_CALL(*mockTelemetry, reportEvent("musicPlayerStartedFromVoiceInput", _, _)).Times(1);
            EXPECT_CALL(*mockTelemetry, reportEvent("musicPlayerStartedFromTtsEnd", _, _)).Times(0);
            EXPECT_CALL(*mockTelemetry, reportEvent("musicPlayerStartedFromPrev", _, _)).Times(0);
        }

        mediaStats->onVoiceInputStarted("vinsRequestId");
        mediaStats->onLegacyPlayerStateChanged(buildMusicPlayerStartedEvent("vinsRequestId"));
    }

    Y_UNIT_TEST_F(testMusicPlayerStartedFromPrev, MediaStatsFixture) {
        {
            InSequence s;
            EXPECT_CALL(*mockTelemetry, reportEvent("audioClientStartedFromVoiceInput", _, _)).Times(0);
            EXPECT_CALL(*mockTelemetry, reportEvent("audioClientStartedFromTtsEnd", _, _)).Times(0);
            EXPECT_CALL(*mockTelemetry, reportEvent("audioClientStartedFromPrev", _, _)).Times(0);
            EXPECT_CALL(*mockTelemetry, reportEvent("musicPlayerStartedFromVoiceInput", _, _)).Times(1);
            EXPECT_CALL(*mockTelemetry, reportEvent("musicPlayerStartedFromTtsEnd", _, _)).Times(1);
            EXPECT_CALL(*mockTelemetry, reportEvent("musicPlayerStartedFromPrev", _, _)).Times(0);
        }

        mediaStats->onVoiceInputStarted("firstSong");
        mediaStats->onTtsCompleted();
        mediaStats->onLegacyPlayerStateChanged(buildMusicPlayerStartedEvent("firstSong"));
        mediaStats->onLegacyPlayerStateChanged(buildMusicPlayerStartedEvent("secondSong"));
        mediaStats->onLegacyPlayerStateChanged(buildMusicPlayerStartedEvent("thirdSong"));
    }

    Y_UNIT_TEST_F(testStatsForSwitchingPlayers, MediaStatsFixture) {
        {
            InSequence s;
            EXPECT_CALL(*mockTelemetry, reportEvent("audioClientStartedFromVoiceInput", _, _)).Times(1);
            EXPECT_CALL(*mockTelemetry, reportEvent("audioClientStartedFromTtsEnd", _, _)).Times(1);
            EXPECT_CALL(*mockTelemetry, reportEvent("audioClientStartedFromPrev", _, _)).Times(0);
            EXPECT_CALL(*mockTelemetry, reportEvent("musicPlayerStartedFromVoiceInput", _, _)).Times(0);
            EXPECT_CALL(*mockTelemetry, reportEvent("musicPlayerStartedFromTtsEnd", _, _)).Times(0);
            EXPECT_CALL(*mockTelemetry, reportEvent("musicPlayerStartedFromPrev", _, _)).Times(0);
        }

        mediaStats->onVoiceInputStarted("firstSong");
        mediaStats->onTtsCompleted();
        mediaStats->onAudioClientEvent(buildAudioClientStartedEvent("firstSong"));
        mediaStats->onAudioClientEvent(buildAudioClientFinishedEvent("firstSong"));
        mediaStats->onLegacyPlayerStateChanged(buildMusicPlayerStartedEvent("secondSong"));
    }

    Y_UNIT_TEST_F(testStatsForAudioPlayerInterruption, MediaStatsFixture) {
        EXPECT_CALL(*mockTelemetry, reportEvent(_, _, _)).Times(0);

        mediaStats->onVoiceInputStarted("musicRequest");
        mediaStats->onVoiceInputStarted("someOtherRequest");
        mediaStats->onTtsCompleted();
        mediaStats->onAudioClientEvent(buildAudioClientStartedEvent("musicRequest"));
    }
}

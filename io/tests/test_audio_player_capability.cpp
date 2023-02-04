
#include <yandex_io/capabilities/file_player/interfaces/mocks/mock_i_file_player_capability.h>
#include <yandex_io/interfaces/device_state/mock/device_state_provider.h>
#include <yandex_io/libs/activity_tracker/tests/mocks/mock_activity_tracker.h>
#include <yandex_io/libs/base/directives.h>
#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/ipc/mock/mock_i_connector.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/sdk/interfaces/directive.h>
#include <yandex_io/sdk/private/device_context.h>
#include <yandex_io/services/aliced/capabilities/audio_player_capability/audio_player_capability.h>
#include <yandex_io/services/aliced/directive_processor/directive_processor.h>
#include <yandex_io/services/aliced/directive_processor/mocks/mock_i_directive_processor.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>
#include <yandex_io/tests/testlib/null_device_state_capability/null_device_state_capability.h>
#include <yandex_io/services/aliced/capabilities/alice_capability/directives/alice_request_directive.h>

using namespace quasar;
using namespace testing;
using namespace YandexIO;

namespace {

    class TAudioPlayerCapabilityFixture: public QuasarUnitTestFixture {
    public:
        void SetUp(NUnitTest::TTestContext& context) override {
            QuasarUnitTestFixture::SetUp(context);
            init();
        }

    private:
        void init() {
            const auto factory = ipcFactoryForTests();
            deviceContext_ = std::make_shared<YandexIO::DeviceContext>(factory);

            directiveProcessor_ = std::make_shared<MockIDirectiveProcessor>();
            filePlayerCapabilityMock_ = std::make_shared<MockIFilePlayerCapability>();
            audioClientConnectorMock_ = std::make_shared<ipc::mock::MockIConnector>();

            capability_ = std::make_shared<AudioPlayerCapability>(
                activityTrackerMock_,
                directiveProcessor_,
                audioClientConnectorMock_,
                *deviceContext_,
                device_,
                filePlayerCapabilityMock_,
                std::make_shared<NullDeviceStateCapability>());
        }

    protected:
        static std::shared_ptr<Directive> createAudioPlayDirective(
            const std::string& streamId, const std::string& vinsRequestId)
        {
            Directive::Data data;
            data.name = quasar::Directives::AUDIO_PLAY;
            data.type = "client_action";
            data.requestId = vinsRequestId;
            data.payload["stream"]["id"] = streamId;

            return std::make_shared<Directive>(std::move(data));
        }

        static std::shared_ptr<Directive> createAudioStopDirective()
        {
            Directive::Data data;
            data.name = quasar::Directives::AUDIO_STOP;

            return std::make_shared<Directive>(std::move(data));
        }

        static std::shared_ptr<Directive> createAudioRewindDirective()
        {
            Directive::Data data;
            data.name = quasar::Directives::AUDIO_REWIND;
            data.payload["type"] = "Absolute";

            return std::make_shared<Directive>(std::move(data));
        }

        static std::shared_ptr<Directive> createGlagolMetadataDirective()
        {
            Directive::Data data;
            data.name = quasar::Directives::GLAGOL_METADATA;

            return std::make_shared<Directive>(std::move(data));
        }

        static std::shared_ptr<Directive> createAudioClientPrevDirective()
        {
            Directive::Data data;
            data.name = quasar::Directives::AUDIO_CLIENT_PREV_TRACK;

            return std::make_shared<Directive>(std::move(data));
        }

        static std::shared_ptr<Directive> createAudioClientNextDirective()
        {
            Directive::Data data;
            data.name = quasar::Directives::AUDIO_CLIENT_NEXT_TRACK;

            return std::make_shared<Directive>(std::move(data));
        }

        static ipc::SharedMessage createAudioClientPlayingMessage(
            const std::string& playerId, const std::string& streamId)
        {
            return ipc::buildMessage([&](auto& msg) {
                auto event = msg.mutable_audio_client_event();
                event->set_state(proto::AudioClientState::PLAYING);
                event->set_event(proto::AudioClientEvent::STATE_CHANGED);
                event->mutable_player_descriptor()->set_player_id(TString(playerId));
                event->mutable_player_descriptor()->set_stream_id(TString(streamId));
                event->mutable_player_descriptor()->set_type(proto::AudioPlayerDescriptor::AUDIO);
            });
        }

    public:
        ActivityTracker activityTracker_;
        MockActivityTracker activityTrackerMock_;
        std::shared_ptr<MockIDirectiveProcessor> directiveProcessor_;
        std::shared_ptr<AudioPlayerCapability> capability_;
        std::shared_ptr<YandexIO::DeviceContext> deviceContext_;
        std::shared_ptr<MockIFilePlayerCapability> filePlayerCapabilityMock_;
        std::shared_ptr<ipc::mock::MockIConnector> audioClientConnectorMock_;
    };

} // anonymous namespace

Y_UNIT_TEST_SUITE(AudioPlayerCapabilityTest) {
    Y_UNIT_TEST_F(testHandleDirective_AudioPlay, TAudioPlayerCapabilityFixture) {
        const std::string streamId = "streamId";
        const std::string requestId = "requestId";
        auto directive = createAudioPlayDirective(streamId, requestId);

        EXPECT_CALL(activityTrackerMock_, addActivity(_)).WillOnce(Invoke([&](const YandexIO::IActivityPtr& activity) -> bool {
            UNIT_ASSERT(activity->activityName() == "AudioPlayerCapability");
            UNIT_ASSERT(activity->getAudioChannel() == proto::CONTENT_CHANNEL);

            return true;
        }));

        EXPECT_CALL(*audioClientConnectorMock_, sendMessage(Matcher<const quasar::ipc::SharedMessage&>(_)))
            .WillOnce(Invoke([&](const ipc::SharedMessage& sharedMessage) {
                UNIT_ASSERT(sharedMessage->has_media_request() && sharedMessage->media_request().has_play_audio());
                UNIT_ASSERT(sharedMessage->media_request().is_prefetch() == false);

                const auto& playAudio = sharedMessage->media_request().play_audio();
                UNIT_ASSERT(playAudio.id() == streamId);
                UNIT_ASSERT(playAudio.has_analytics_context());
                UNIT_ASSERT(playAudio.analytics_context().vins_request_id() == requestId);

                return true;
            }));

        capability_->handleDirective(directive);
    }

    Y_UNIT_TEST_F(testHandleDirective_AudioStop, TAudioPlayerCapabilityFixture) {
        auto directive = createAudioStopDirective();

        EXPECT_CALL(*audioClientConnectorMock_, sendMessage(Matcher<const quasar::ipc::SharedMessage&>(_)))
            .WillOnce(Invoke([&](const ipc::SharedMessage& sharedMessage) {
                UNIT_ASSERT(sharedMessage->has_media_request());

                const auto& mediaRequest = sharedMessage->media_request();
                UNIT_ASSERT(mediaRequest.has_clean_players());
                UNIT_ASSERT(mediaRequest.has_player_descriptor());
                UNIT_ASSERT(mediaRequest.player_descriptor().type() == proto::AudioPlayerDescriptor::AUDIO);

                return true;
            }));

        capability_->handleDirective(directive);
    }

    Y_UNIT_TEST_F(testHandleDirective_AudioRewind, TAudioPlayerCapabilityFixture) {
        auto directive = createAudioRewindDirective();

        EXPECT_CALL(*audioClientConnectorMock_, sendMessage(Matcher<const quasar::ipc::SharedMessage&>(_)))
            .WillOnce(Invoke([&](const ipc::SharedMessage& sharedMessage) {
                UNIT_ASSERT(sharedMessage->has_media_request());

                const auto& mediaRequest = sharedMessage->media_request();
                UNIT_ASSERT(mediaRequest.has_rewind());
                UNIT_ASSERT(mediaRequest.has_player_descriptor());
                UNIT_ASSERT(mediaRequest.player_descriptor().type() == proto::AudioPlayerDescriptor::AUDIO);

                return true;
            }));

        capability_->handleDirective(directive);
    }

    Y_UNIT_TEST_F(testHandleDirective_GlagolMetadata, TAudioPlayerCapabilityFixture) {
        auto directive = createGlagolMetadataDirective();

        EXPECT_CALL(*audioClientConnectorMock_, sendMessage(Matcher<const quasar::ipc::SharedMessage&>(_)))
            .WillOnce(Invoke([&](const ipc::SharedMessage& sharedMessage) {
                UNIT_ASSERT(sharedMessage->has_media_request());

                const auto& mediaRequest = sharedMessage->media_request();
                UNIT_ASSERT(mediaRequest.has_metadata());
                UNIT_ASSERT(mediaRequest.metadata().has_music_metadata());
                UNIT_ASSERT(mediaRequest.has_player_descriptor());
                UNIT_ASSERT(mediaRequest.player_descriptor().type() == proto::AudioPlayerDescriptor::AUDIO);

                return true;
            }));

        capability_->handleDirective(directive);
    }

    Y_UNIT_TEST_F(testHandleDirective_AudioClientPrevTrack, TAudioPlayerCapabilityFixture) {
        EXPECT_CALL(*directiveProcessor_, addDirectives(_))
            .WillOnce(Invoke([&](const std::list<std::shared_ptr<Directive>>& directives) {
                UNIT_ASSERT(directives.size() == 1);

                auto requestDirective = std::dynamic_pointer_cast<AliceRequestDirective>(*directives.begin());
                UNIT_ASSERT(requestDirective != nullptr);

                auto request = requestDirective->getRequest();
                UNIT_ASSERT(request != nullptr);

                const auto& event = request->getEvent();
                UNIT_ASSERT(event["name"] == "@@mm_semantic_frame");
                UNIT_ASSERT(event["payload"].isMember("typed_semantic_frame"));

                const auto& tsf = event["payload"]["typed_semantic_frame"];
                UNIT_ASSERT(tsf.isMember("player_prev_track_semantic_frame"));
            }));

        capability_->handleDirective(createAudioClientPrevDirective());
    }

    Y_UNIT_TEST_F(testHandleDirective_AudioClientNextTrack, TAudioPlayerCapabilityFixture) {
        EXPECT_CALL(*directiveProcessor_, addDirectives(_))
            .WillOnce(Invoke([&](const std::list<std::shared_ptr<Directive>>& directives) {
                UNIT_ASSERT(directives.size() == 1);

                auto requestDirective = std::dynamic_pointer_cast<AliceRequestDirective>(*directives.begin());
                UNIT_ASSERT(requestDirective != nullptr);

                auto request = requestDirective->getRequest();
                UNIT_ASSERT(request != nullptr);

                const auto& event = request->getEvent();
                UNIT_ASSERT(event["name"] == "@@mm_semantic_frame");
                UNIT_ASSERT(event["payload"].isMember("typed_semantic_frame"));

                const auto& tsf = event["payload"]["typed_semantic_frame"];
                UNIT_ASSERT(tsf.isMember("player_next_track_semantic_frame"));
            }));

        capability_->handleDirective(createAudioClientNextDirective());
    }

    Y_UNIT_TEST_F(testPrefetchDirective_AudioPlay, TAudioPlayerCapabilityFixture) {
        const std::string streamId = "streamId";
        const std::string requestId = "requestId";
        auto directive = createAudioPlayDirective(streamId, requestId);

        EXPECT_CALL(activityTrackerMock_, addActivity(_)).Times(0);
        EXPECT_CALL(*audioClientConnectorMock_, sendMessage(Matcher<const quasar::ipc::SharedMessage&>(_)))
            .WillOnce(Invoke([&](const ipc::SharedMessage& sharedMessage) {
                UNIT_ASSERT(sharedMessage->has_media_request() && sharedMessage->media_request().has_play_audio());
                UNIT_ASSERT(sharedMessage->media_request().is_prefetch() == true);

                const auto& playAudio = sharedMessage->media_request().play_audio();
                UNIT_ASSERT(playAudio.id() == streamId);
                UNIT_ASSERT(playAudio.has_analytics_context());
                UNIT_ASSERT(playAudio.analytics_context().vins_request_id() == requestId);

                return true;
            }));

        capability_->prefetchDirective(directive);
    }

    Y_UNIT_TEST_F(testOnQuasarMessage_unblocksPrefetchOnPlaying, TAudioPlayerCapabilityFixture) {
        std::string playerId;
        const std::string streamId = "streamId";
        const std::string requestId = "requestId";
        auto directive = createAudioPlayDirective(streamId, requestId);
        directive->setBlocksSubsequentPrefetch(true);

        EXPECT_CALL(activityTrackerMock_, addActivity(_)).WillOnce(Invoke([&](const YandexIO::IActivityPtr& activity) -> bool {
            UNIT_ASSERT(activity->activityName() == "AudioPlayerCapability");
            UNIT_ASSERT(activity->getAudioChannel() == proto::CONTENT_CHANNEL);

            return true;
        }));

        EXPECT_CALL(*audioClientConnectorMock_, sendMessage(Matcher<const quasar::ipc::SharedMessage&>(_)))
            .WillOnce(Invoke([&](const ipc::SharedMessage& sharedMessage) {
                UNIT_ASSERT(sharedMessage->has_media_request() && sharedMessage->media_request().has_play_audio());
                UNIT_ASSERT(sharedMessage->media_request().is_prefetch() == false);

                const auto& playAudio = sharedMessage->media_request().play_audio();
                UNIT_ASSERT(playAudio.id() == streamId);
                UNIT_ASSERT(playAudio.has_analytics_context());
                UNIT_ASSERT(playAudio.analytics_context().vins_request_id() == requestId);

                playerId = sharedMessage->media_request().player_descriptor().player_id();
                return true;
            }));

        EXPECT_CALL(*directiveProcessor_, onBlockPrefetchChanged(directive));

        capability_->handleDirective(directive);
        capability_->onQuasarMessage(createAudioClientPlayingMessage(playerId, streamId));

        ASSERT_FALSE(directive->isBlocksSubsequentPrefetch());
    }

}

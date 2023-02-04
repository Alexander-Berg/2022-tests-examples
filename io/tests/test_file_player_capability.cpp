#include <yandex_io/capabilities/file_player/interfaces/mocks/mock_i_play_sound_file_listener.h>
#include <yandex_io/libs/activity_tracker/tests/mocks/mock_activity_tracker.h>
#include <yandex_io/libs/activity_tracker/tests/mocks/mock_i_activity.h>
#include <yandex_io/libs/base/directives.h>
#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/ipc/mock/server.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/sdk/interfaces/directive.h>
#include <yandex_io/sdk/private/remoting/remoting_message_router.h>
#include <yandex_io/sdk/private/remoting/mocks/mock_i_remoting_connection.h>

#include <yandex_io/services/aliced/capabilities/alice_capability/directives/alice_request_directive.h>
#include <yandex_io/services/aliced/capabilities/file_player_capability/file_player_capability.h>
#include <yandex_io/services/aliced/directive_processor/directive_processor.h>
#include <yandex_io/services/aliced/directive_processor/mocks/mock_i_directive_processor.h>

#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/test_callback_queue.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <yandex_io/protos/quasar_proto.pb.h>

using namespace quasar;
using namespace quasar::TestUtils;
using namespace testing;
using namespace YandexIO;

namespace {

    MATCHER_P(VerifyActivity, channel, "description") {
        const IActivityPtr& activity = arg;
        if (activity->getAudioChannel() != channel) {
            *result_listener << " activity with unepected channel";
            return false;
        }
        return true;
    }

    MATCHER_P(VerifySoundFilePlayDirective, channelOpt, "description") {
        const std::shared_ptr<Directive>& directive = *arg.begin();
        if (directive == nullptr) {
            *result_listener << "Should not be nullptr";
            return false;
        }
        if (!directive->is(Directives::SOUND_FILE_PLAY)) {
            *result_listener << "Directive is not SOUND_FILE_PLAY, it is " << directive->getData().name;
            return false;
        }
        if (directive->getData().channel != channelOpt) {
            *result_listener << "Directive channel is not expected";
            return false;
        }

        return true;
    }

    MATCHER(VerifySoundFileStopDirective, "description") {
        const std::shared_ptr<Directive>& directive = *arg.begin();
        if (directive == nullptr) {
            *result_listener << "Should not be nullptr";
            return false;
        }
        if (!directive->is(Directives::SOUND_FILE_STOP)) {
            *result_listener << "Directive is not SOUND_FILE_STOP, it is " << directive->getData().name;
            return false;
        }

        return true;
    }

    MATCHER_P2(VerifyPlayAudioMessage, fileName, channelOpt, "description") {
        const proto::QuasarMessage& message = *arg;

        if (!message.has_media_request() || !message.media_request().has_play_audio()) {
            *result_listener << " no play_audio";
            return false;
        }
        const auto& playAudio = message.media_request().play_audio();
        if (!playAudio.has_format() || playAudio.format() != proto::Audio::AUDIO_FILE) {
            *result_listener << " playAudio has wrong format";
            return false;
        }
        if (!playAudio.has_file_path()) {
            *result_listener << " alarm.alarm_type != expectedAlarm.alarm_type";
            return false;
        }
        TFsPath path(message.media_request().play_audio().file_path());
        if (path.GetName() != fileName) {
            *result_listener << "Unexpected sound_file. Got " << path.GetName() << ", expected " << fileName;
            return false;
        }

        if (!message.media_request().has_player_descriptor()) {
            *result_listener << " no player_descriptor in play audio";
            return false;
        }
        const auto& descriptor = message.media_request().player_descriptor();
        if (!descriptor.has_type() || descriptor.type() != proto::AudioPlayerDescriptor::FILE_PLAYER) {
            *result_listener << " wrong descriptor type";
            return false;
        }
        if (!channelOpt.has_value()) {
            if (descriptor.has_audio_channel()) {
                *result_listener << " no player_descriptor has unexpected audio_channel";
                return false;
            }
            return true;
        }
        if (!descriptor.has_audio_channel() || descriptor.audio_channel() != channelOpt.value()) {
            *result_listener << " unexpected audio_channel";
            return false;
        }

        return true;
    }

    MATCHER_P(VerifyCleanPlayersMessage, playerId, "description") {
        const proto::QuasarMessage& message = *arg;

        if (!message.has_media_request() || !message.media_request().has_clean_players()) {
            *result_listener << " no clean_players";
            return false;
        }

        if (!message.media_request().has_player_descriptor()) {
            *result_listener << " no player_descriptor in play audio";
            return false;
        }
        const auto& descriptor = message.media_request().player_descriptor();
        if (!descriptor.has_type() || descriptor.type() != proto::AudioPlayerDescriptor::FILE_PLAYER) {
            *result_listener << " wrong descriptor type";
            return false;
        }
        if (!descriptor.has_player_id() || descriptor.player_id() != playerId) {
            *result_listener << " wrong playerId";
            return false;
        }

        return true;
    }

    MATCHER_P(VerifyReplayMessage, playerId, "description") {
        const proto::QuasarMessage& message = *arg;

        if (!message.has_media_request() || !message.media_request().has_replay()) {
            *result_listener << " no replay";
            return false;
        }

        if (!message.media_request().has_player_descriptor()) {
            *result_listener << " no player_descriptor in play audio";
            return false;
        }
        const auto& descriptor = message.media_request().player_descriptor();
        if (!descriptor.has_type() || descriptor.type() != proto::AudioPlayerDescriptor::FILE_PLAYER) {
            *result_listener << " wrong descriptor type";
            return false;
        }
        if (!descriptor.has_player_id() || descriptor.player_id() != playerId) {
            *result_listener << " wrong playerId";
            return false;
        }

        return true;
    }

} // namespace

namespace {
    class FilePlayerCapabilityFixture: public QuasarUnitTestFixture {
    public:
        void SetUp(NUnitTest::TTestContext& context) override {
            QuasarUnitTestFixture::SetUp(context);
            init();
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            directiveProcessor->clear();
            soundsDir_.ForceDelete();
            QuasarUnitTestFixture::TearDown(context);
        }

        void expectPlayAudio(const std::string& file, std::optional<proto::AudioChannel> channel, std::string& playerId) const {
            EXPECT_CALL(*audioClientConnectorMock, sendMessage(Matcher<const ipc::SharedMessage&>(VerifyPlayAudioMessage(file, channel))))
                .WillOnce(Invoke([&playerId](const ipc::SharedMessage& message) -> bool {
                    playerId = message->media_request().player_descriptor().player_id();
                    return true;
                }));
        }

        void expectSoundFilePlayDirective(std::optional<proto::AudioChannel> channelOpt) {
            EXPECT_CALL(*directiveProcessorMock, addDirectives(VerifySoundFilePlayDirective(channelOpt)))
                .WillOnce(Invoke([this](std::list<std::shared_ptr<Directive>> directives) {
                    directiveProcessor->addDirectives(directives);
                }));
        }

        void expectSoundFileStopDirective() {
            EXPECT_CALL(*directiveProcessorMock, addDirectives(VerifySoundFileStopDirective()))
                .WillOnce(Invoke([this](std::list<std::shared_ptr<Directive>> directives) {
                    directiveProcessor->addDirectives(directives);
                }));
        }

        static ipc::SharedMessage buildPlayingEvent(const std::string& playerId) {
            return ipc::buildMessage([&playerId](auto& msg) {
                auto event = msg.mutable_audio_client_event();
                event->set_event(proto::AudioClientEvent::STATE_CHANGED);
                event->mutable_player_descriptor()->set_player_id(TString(playerId));
                event->mutable_player_descriptor()->set_type(proto::AudioPlayerDescriptor::FILE_PLAYER);
                event->set_state(proto::AudioClientState::PLAYING);
            });
        }

        static ipc::SharedMessage buildFinishedEvent(const std::string& playerId) {
            return ipc::buildMessage([&playerId](auto& msg) {
                auto event = msg.mutable_audio_client_event();
                event->set_event(proto::AudioClientEvent::STATE_CHANGED);
                event->mutable_player_descriptor()->set_player_id(TString(playerId));
                event->mutable_player_descriptor()->set_type(proto::AudioPlayerDescriptor::FILE_PLAYER);
                event->set_state(proto::AudioClientState::FINISHED);
            });
        }

        static ipc::SharedMessage buildFailedEvent(const std::string& playerId) {
            return ipc::buildMessage([&playerId](auto& msg) {
                auto event = msg.mutable_audio_client_event();
                event->set_event(proto::AudioClientEvent::STATE_CHANGED);
                event->mutable_player_descriptor()->set_player_id(TString(playerId));
                event->mutable_player_descriptor()->set_type(proto::AudioPlayerDescriptor::FILE_PLAYER);
                event->set_state(proto::AudioClientState::FAILED);
            });
        }

        std::string createFile(const std::string& file) const {
            TFsPath path = JoinFsPaths(soundsDir_, file);
            path.Touch();
            return file;
        }

        void innerSetUp() {
            auto basePath = tryGetRamDrivePath();
            soundsDir_ = JoinFsPaths(basePath, "soundsDir-" + makeUUID());
            soundsDir_.ForceDelete();
            soundsDir_.MkDirs();

            Json::Value& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard_);
            config["soundd"]["soundsPath"] = soundsDir_.GetPath();

            directiveProcessor = std::make_shared<DirectiveProcessor>();
            directiveProcessorMock = std::make_shared<MockIDirectiveProcessor>();
            ON_CALL(*directiveProcessorMock, addDirectives(_))
                .WillByDefault(Invoke([this](std::list<std::shared_ptr<Directive>> directives) {
                    directiveProcessor->addDirectives(directives);
                }));

            audioClientConnectorMock = ipcFactoryForTests()->allocateGMockIpcConnector("audioclient");
        }

        void init() {
            innerSetUp();
            EXPECT_CALL(*audioClientConnectorMock, setConnectHandler(_))
                .WillOnce(Invoke([](std::function<void()> func) {
                    func();
                }));

            capability = std::make_shared<FilePlayerCapability>(
                std::make_shared<TestCallbackQueue>(),
                getDeviceForTests(),
                activityTrackerMock,
                directiveProcessorMock,
                ipcFactoryForTests(),
                std::weak_ptr<YandexIO::IRemotingRegistry>());

            ASSERT_TRUE(directiveProcessor->addDirectiveHandler(capability));
        }

    public:
        std::shared_ptr<FilePlayerCapability> capability;

        std::shared_ptr<MockIDirectiveProcessor> directiveProcessorMock;
        std::shared_ptr<DirectiveProcessor> directiveProcessor;

        std::shared_ptr<ipc::mock::MockIConnector> audioClientConnectorMock;

        MockActivityTracker activityTrackerMock;

    private:
        YandexIO::Configuration::TestGuard testGuard_;
        TFsPath soundsDir_;
    };

} // namespace

Y_UNIT_TEST_SUITE_F(FilePlayerCapabilityTest_Basic, FilePlayerCapabilityFixture) {
    Y_UNIT_TEST(testPlaySoundFile) {
        const auto file = createFile("sound_file.wav");
        const auto channel = std::optional<proto::AudioChannel>(proto::DIALOG_CHANNEL);

        EXPECT_CALL(activityTrackerMock, addActivity(VerifyActivity(channel.value())));
        expectSoundFilePlayDirective(channel);

        std::string playerId;
        expectPlayAudio(file, channel, playerId);
        capability->playSoundFile(file, channel);

        EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveStarted(_));
        capability->onAudioClientMessage(buildPlayingEvent(playerId));
    }

    Y_UNIT_TEST(testPlaySoundFile_noChannel) {
        const auto file = createFile("sound_file.wav");
        const auto channel = std::optional<proto::AudioChannel>(std::nullopt);

        EXPECT_CALL(activityTrackerMock, addActivity(_)).Times(0);
        expectSoundFilePlayDirective(channel);

        std::string playerId;
        expectPlayAudio(file, channel, playerId);
        capability->playSoundFile(file, channel);

        EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveStarted(_));
        capability->onAudioClientMessage(buildPlayingEvent(playerId));
    }

    Y_UNIT_TEST(testStopSoundFile) {
        const auto file = createFile("sound_file.wav");
        const auto channel = std::optional<proto::AudioChannel>(proto::DIALOG_CHANNEL);

        std::string playerId;
        expectPlayAudio(file, channel, playerId);
        capability->playSoundFile(file, channel);

        EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveCompleted(_, true));
        EXPECT_CALL(activityTrackerMock, removeActivity(VerifyActivity(channel.value())));
        expectSoundFileStopDirective();
        EXPECT_CALL(*audioClientConnectorMock, sendMessage(Matcher<const ipc::SharedMessage&>(VerifyCleanPlayersMessage(playerId))));

        capability->stopSoundFile(file);
    }

    Y_UNIT_TEST(testStopSoundFile_noChannel) {
        const auto file = createFile("sound_file.wav");
        const auto channel = std::optional<proto::AudioChannel>(std::nullopt);

        std::string playerId;
        expectPlayAudio(file, channel, playerId);
        capability->playSoundFile(file, channel);

        EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveCompleted(_, true));
        EXPECT_CALL(activityTrackerMock, removeActivity(_)).Times(0);
        expectSoundFileStopDirective();
        EXPECT_CALL(*audioClientConnectorMock, sendMessage(Matcher<const ipc::SharedMessage&>(VerifyCleanPlayersMessage(playerId))));

        capability->stopSoundFile(file);
    }

    Y_UNIT_TEST(testFilePlayFinished) {
        const auto file = createFile("sound_file.wav");
        const auto channel = std::optional<proto::AudioChannel>(proto::DIALOG_CHANNEL);

        std::string playerId;
        expectPlayAudio(file, channel, playerId);
        capability->playSoundFile(file, channel);

        EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveStarted(_));
        capability->onAudioClientMessage(buildPlayingEvent(playerId));

        EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveCompleted(_, true));
        EXPECT_CALL(activityTrackerMock, removeActivity(VerifyActivity(channel.value())));
        EXPECT_CALL(*audioClientConnectorMock, sendMessage(Matcher<const ipc::SharedMessage&>(VerifyCleanPlayersMessage(playerId))));
        capability->onAudioClientMessage(buildFinishedEvent(playerId));
    }

    Y_UNIT_TEST(testFilePlayFailed) {
        const auto file = createFile("sound_file.wav");
        const auto channel = std::optional<proto::AudioChannel>(std::nullopt);

        std::string playerId;
        expectPlayAudio(file, channel, playerId);
        capability->playSoundFile(file, channel);

        EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveStarted(_));
        capability->onAudioClientMessage(buildPlayingEvent(playerId));

        EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveCompleted(_, false));
        EXPECT_CALL(activityTrackerMock, removeActivity(_)).Times(0);
        EXPECT_CALL(*audioClientConnectorMock, sendMessage(Matcher<const ipc::SharedMessage&>(VerifyCleanPlayersMessage(playerId))));
        capability->onAudioClientMessage(buildFailedEvent(playerId));
    }

    Y_UNIT_TEST(testCancelFilePlay) {
        const auto file = createFile("sound_file.wav");
        const auto channel = std::optional<proto::AudioChannel>(proto::DIALOG_CHANNEL);

        std::string playerId;
        expectPlayAudio(file, channel, playerId);
        capability->playSoundFile(file, channel);

        EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveStarted(_));
        capability->onAudioClientMessage(buildPlayingEvent(playerId));

        EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveCompleted(_, false)).Times(0);
        EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveCompleted(_, true)).Times(0);
        EXPECT_CALL(activityTrackerMock, removeActivity(VerifyActivity(channel.value())));
        EXPECT_CALL(*audioClientConnectorMock, sendMessage(Matcher<const ipc::SharedMessage&>(VerifyCleanPlayersMessage(playerId))));

        YandexIO::Directive::Data data(Directives::LISTEN, "local_action");
        data.requestId = "someRequestId";
        auto request = VinsRequest::createListenRequest(data, VinsRequest::createHardwareButtonClickEventSource());
        directiveProcessor->addDirectives(std::list<std::shared_ptr<Directive>>{
            std::make_shared<AliceRequestDirective>(std::move(request), nullptr, true)});
    }
}

Y_UNIT_TEST_SUITE_F(FilePlayerCapabilityTest_PlayParams, FilePlayerCapabilityFixture) {
    Y_UNIT_TEST(testRequestId) {
        const auto file = createFile("sound_file.wav");
        const auto channel = std::optional<proto::AudioChannel>(proto::DIALOG_CHANNEL);
        const auto requestId = "request_id";

        EXPECT_CALL(*directiveProcessorMock, addDirectives(VerifySoundFilePlayDirective(channel)))
            .WillOnce(Invoke([&requestId](std::list<std::shared_ptr<Directive>> directives) {
                UNIT_ASSERT_VALUES_EQUAL(directives.front()->getData().requestId, requestId);
            }));

        YandexIO::IFilePlayerCapability::PlayParams playParams;
        playParams.requestId = requestId;
        capability->playSoundFile(file, channel, playParams);
    }

    Y_UNIT_TEST(testParentRequestId) {
        const auto file = createFile("sound_file.wav");
        const auto channel = std::optional<proto::AudioChannel>(proto::DIALOG_CHANNEL);
        const auto parentRequestId = "parent_request_id";

        EXPECT_CALL(*directiveProcessorMock, addDirectives(VerifySoundFilePlayDirective(channel)))
            .WillOnce(Invoke([&parentRequestId](std::list<std::shared_ptr<Directive>> directives) {
                UNIT_ASSERT_VALUES_EQUAL(directives.front()->getData().parentRequestId, parentRequestId);
            }));

        YandexIO::IFilePlayerCapability::PlayParams playParams;
        playParams.parentRequestId = parentRequestId;
        capability->playSoundFile(file, channel, playParams);
    }

    Y_UNIT_TEST(testPlayTimes) {
        const auto file = createFile("sound_file.wav");
        const auto channel = std::optional<proto::AudioChannel>(proto::DIALOG_CHANNEL);
        const uint32_t playTimes = 3;

        EXPECT_CALL(activityTrackerMock, addActivity(VerifyActivity(channel.value()))).Times(1);
        EXPECT_CALL(activityTrackerMock, removeActivity(VerifyActivity(channel.value()))).Times(1);
        EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveStarted(_)).Times(1);
        EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveCompleted(_, true)).Times(1);

        std::string playerId;
        expectPlayAudio(file, channel, playerId);

        YandexIO::IFilePlayerCapability::PlayParams playParams;
        playParams.playTimes = playTimes;
        capability->playSoundFile(file, channel, playParams);

        EXPECT_CALL(*audioClientConnectorMock, sendMessage(Matcher<const ipc::SharedMessage&>(VerifyCleanPlayersMessage(playerId)))).Times(1);
        EXPECT_CALL(*audioClientConnectorMock, sendMessage(Matcher<const ipc::SharedMessage&>(VerifyReplayMessage(playerId))))
            .Times(playTimes - 1)
            .WillRepeatedly(Invoke([this, &playerId](const ipc::SharedMessage& /*message*/) {
                capability->onAudioClientMessage(buildPlayingEvent(playerId));
                capability->onAudioClientMessage(buildFinishedEvent(playerId));
                return true;
            }));

        capability->onAudioClientMessage(buildPlayingEvent(playerId));
        capability->onAudioClientMessage(buildFinishedEvent(playerId));
    }

    Y_UNIT_TEST(testPlayLooped) {
        const auto file = createFile("sound_file.wav");
        const auto channel = std::optional<proto::AudioChannel>(proto::DIALOG_CHANNEL);

        EXPECT_CALL(activityTrackerMock, addActivity(VerifyActivity(channel.value()))).Times(1);
        EXPECT_CALL(activityTrackerMock, removeActivity(VerifyActivity(channel.value()))).Times(1);
        EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveStarted(_)).Times(1);
        EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveCompleted(_, true)).Times(1);

        std::string playerId;
        expectPlayAudio(file, channel, playerId);

        YandexIO::IFilePlayerCapability::PlayParams playParams;
        playParams.playLooped = true;
        capability->playSoundFile(file, channel, playParams);

        EXPECT_CALL(*audioClientConnectorMock, sendMessage(Matcher<const ipc::SharedMessage&>(VerifyCleanPlayersMessage(playerId)))).Times(1);

        capability->onAudioClientMessage(buildPlayingEvent(playerId));
        /// 10 is close enough to infinite, right?
        for (int i = 0; i < 10; ++i) {
            EXPECT_CALL(*audioClientConnectorMock,
                        sendMessage(Matcher<const ipc::SharedMessage&>(VerifyReplayMessage(playerId))));
            capability->onAudioClientMessage(buildFinishedEvent(playerId));
            capability->onAudioClientMessage(buildPlayingEvent(playerId));
        }

        capability->stopSoundFile(file);
    }
}

namespace {
    class TestListenerFixture: public FilePlayerCapabilityFixture {
    public:
        void SetUp(NUnitTest::TTestContext& context) override {
            FilePlayerCapabilityFixture::SetUp(context);
            init();
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            FilePlayerCapabilityFixture::TearDown(context);
        }

        void init() {
            file = createFile("sound_file.wav");
            channel = std::optional<proto::AudioChannel>(proto::DIALOG_CHANNEL);
            requestId = makeUUID();
            mockListener = std::make_shared<MockIPlaySoundFileListener>();
        }

    public:
        std::string file;
        std::optional<proto::AudioChannel> channel;
        std::string requestId;
        std::shared_ptr<MockIPlaySoundFileListener> mockListener;
    };

} // namespace

Y_UNIT_TEST_SUITE_F(FilePlayerCapabilityTest_Listener, TestListenerFixture) {
    Y_UNIT_TEST(testOnStartedCallback) {
        std::string playerId;
        expectPlayAudio(file, channel, playerId);

        YandexIO::IFilePlayerCapability::PlayParams playParams;
        playParams.requestId = requestId;
        capability->playSoundFile(file, channel, playParams, mockListener);

        EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveStarted(_)).Times(1);
        EXPECT_CALL(*mockListener, onStarted());
        capability->onAudioClientMessage(buildPlayingEvent(playerId));
    }

    Y_UNIT_TEST(testOnCompletedCallback_finished) {
        std::string playerId;
        expectPlayAudio(file, channel, playerId);

        YandexIO::IFilePlayerCapability::PlayParams playParams;
        playParams.requestId = requestId;
        capability->playSoundFile(file, channel, playParams, mockListener);

        EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveStarted(_)).Times(1);
        EXPECT_CALL(*mockListener, onStarted());
        capability->onAudioClientMessage(buildPlayingEvent(playerId));

        EXPECT_CALL(*audioClientConnectorMock, sendMessage(Matcher<const ipc::SharedMessage&>(VerifyCleanPlayersMessage(playerId)))).Times(1);
        EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveCompleted(_, true)).Times(1);
        EXPECT_CALL(*mockListener, onCompleted());
        capability->onAudioClientMessage(buildFinishedEvent(playerId));
    }

    Y_UNIT_TEST(testOnCompletedCallback_failed) {
        std::string playerId;
        expectPlayAudio(file, channel, playerId);

        YandexIO::IFilePlayerCapability::PlayParams playParams;
        playParams.requestId = requestId;
        capability->playSoundFile(file, channel, playParams, mockListener);

        EXPECT_CALL(*audioClientConnectorMock, sendMessage(Matcher<const ipc::SharedMessage&>(VerifyCleanPlayersMessage(playerId)))).Times(1);
        EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveCompleted(_, false)).Times(1);
        EXPECT_CALL(*mockListener, onCompleted());
        capability->onAudioClientMessage(buildFailedEvent(playerId));
    }

    Y_UNIT_TEST(testOnCompletedCallback_cancelled) {
        std::string playerId;
        expectPlayAudio(file, channel, playerId);

        YandexIO::IFilePlayerCapability::PlayParams playParams;
        playParams.requestId = requestId;
        capability->playSoundFile(file, channel, playParams, mockListener);

        EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveStarted(_)).Times(1);
        EXPECT_CALL(*mockListener, onStarted());
        capability->onAudioClientMessage(buildPlayingEvent(playerId));

        EXPECT_CALL(*audioClientConnectorMock, sendMessage(Matcher<const ipc::SharedMessage&>(VerifyCleanPlayersMessage(playerId)))).Times(1);

        EXPECT_CALL(*mockListener, onCompleted());
        YandexIO::Directive::Data data(Directives::LISTEN, "local_action");
        data.requestId = "someRequestId";
        auto request = VinsRequest::createListenRequest(data, VinsRequest::createHardwareButtonClickEventSource());
        directiveProcessor->addDirectives(std::list<std::shared_ptr<Directive>>{
            std::make_shared<AliceRequestDirective>(std::move(request), nullptr, true)});
    }

    Y_UNIT_TEST(testOnCompletedCallback_fileIsAbsent) {
        const auto path = "/randomDir100500/pepega.wav";

        expectSoundFilePlayDirective(channel);
        EXPECT_CALL(*audioClientConnectorMock, sendMessage(Matcher<const ipc::SharedMessage&>(_))).Times(0);
        EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveCompleted(_, false));
        EXPECT_CALL(*mockListener, onCompleted());

        YandexIO::IFilePlayerCapability::PlayParams playParams;
        playParams.requestId = requestId;
        capability->playSoundFile(path, channel, playParams, mockListener);
    }
}

Y_UNIT_TEST_SUITE_F(FilePlayerCapabilityTest_Additional, FilePlayerCapabilityFixture) {
    Y_UNIT_TEST(testPlaySeveralFiles) {
        EXPECT_CALL(activityTrackerMock, removeActivity(_)).Times(0);

        std::string dialogChannelPlayerId;
        {
            const auto file = createFile("dialog_channel.wav");
            const auto channel = std::optional<proto::AudioChannel>(proto::DIALOG_CHANNEL);

            EXPECT_CALL(activityTrackerMock, addActivity(VerifyActivity(channel.value())));
            expectSoundFilePlayDirective(channel);

            expectPlayAudio(file, channel, dialogChannelPlayerId);
            capability->playSoundFile(file, channel);

            EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveStarted(_));
            capability->onAudioClientMessage(buildPlayingEvent(dialogChannelPlayerId));
        }
        EXPECT_CALL(*audioClientConnectorMock, sendMessage(Matcher<const ipc::SharedMessage&>(VerifyCleanPlayersMessage(dialogChannelPlayerId)))).Times(0);

        std::string contentChannelPlayerId;
        {
            const auto file = createFile("content_channel.wav");
            const auto channel = std::optional<proto::AudioChannel>(proto::CONTENT_CHANNEL);

            EXPECT_CALL(activityTrackerMock, addActivity(VerifyActivity(channel.value())));
            expectSoundFilePlayDirective(channel);

            expectPlayAudio(file, channel, contentChannelPlayerId);
            capability->playSoundFile(file, channel);

            EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveStarted(_));
            capability->onAudioClientMessage(buildPlayingEvent(contentChannelPlayerId));
        }
        EXPECT_CALL(*audioClientConnectorMock, sendMessage(Matcher<const ipc::SharedMessage&>(VerifyCleanPlayersMessage(contentChannelPlayerId)))).Times(0);

        std::string noChannelPlayerId;
        {
            const auto file = createFile("no_channel.wav");
            const auto channel = std::optional<proto::AudioChannel>(std::nullopt);

            expectSoundFilePlayDirective(channel);

            expectPlayAudio(file, channel, noChannelPlayerId);
            capability->playSoundFile(file, channel);

            EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveStarted(_));
            capability->onAudioClientMessage(buildPlayingEvent(noChannelPlayerId));
        }

        EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveCompleted(_, true));
        EXPECT_CALL(*audioClientConnectorMock, sendMessage(Matcher<const ipc::SharedMessage&>(VerifyCleanPlayersMessage(noChannelPlayerId))));
        capability->onAudioClientMessage(buildFinishedEvent(noChannelPlayerId));
    }

    Y_UNIT_TEST(testPlaySameFileTwice) {
        const auto file = createFile("sound_file.wav");
        const auto channel = std::optional<proto::AudioChannel>(std::nullopt);

        std::string firstPlayerId;
        std::string secondPlayerId;

        {
            expectPlayAudio(file, channel, firstPlayerId);
            capability->playSoundFile(file, channel);

            EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveStarted(_));
            capability->onAudioClientMessage(buildPlayingEvent(firstPlayerId));
        }

        {
            EXPECT_CALL(*audioClientConnectorMock, sendMessage(Matcher<const ipc::SharedMessage&>(VerifyCleanPlayersMessage(firstPlayerId))));
            EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveCompleted(_, true));

            expectPlayAudio(file, channel, secondPlayerId);
            capability->playSoundFile(file, channel);

            EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveStarted(_));
            capability->onAudioClientMessage(buildPlayingEvent(secondPlayerId));
        }
    }

    Y_UNIT_TEST(testFileIsAbsent) {
        const auto file = "poggers.wav";
        const auto channel = std::optional<proto::AudioChannel>(proto::DIALOG_CHANNEL);

        expectSoundFilePlayDirective(channel);
        EXPECT_CALL(*audioClientConnectorMock, sendMessage(Matcher<const ipc::SharedMessage&>(_))).Times(0);
        EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveCompleted(_, false));

        capability->playSoundFile(file, channel);
    }

    Y_UNIT_TEST(testAbsoluteFilePath) {
        const auto channel = std::optional<proto::AudioChannel>(proto::DIALOG_CHANNEL);

        auto basePath = tryGetRamDrivePath();
        TFsPath newSoundsDir = JoinFsPaths(basePath, "newSoundsDir-" + makeUUID());
        newSoundsDir.ForceDelete();
        newSoundsDir.MkDirs();
        std::string file = "sound_file.wav";
        TFsPath path = JoinFsPaths(newSoundsDir, file);
        path.Touch();

        EXPECT_CALL(activityTrackerMock, addActivity(VerifyActivity(channel.value())));
        expectSoundFilePlayDirective(channel);

        std::string playerId;
        expectPlayAudio(file, channel, playerId);
        capability->playSoundFile(path.GetPath(), channel);

        EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveStarted(_));
        capability->onAudioClientMessage(buildPlayingEvent(playerId));

        newSoundsDir.ForceDelete();
    }

    Y_UNIT_TEST(testAbsoluteFilePath_IsAbsent) {
        const auto path = "/randomDir100500/pepega.wav";
        const auto channel = std::optional<proto::AudioChannel>(proto::DIALOG_CHANNEL);

        expectSoundFilePlayDirective(channel);
        EXPECT_CALL(*audioClientConnectorMock, sendMessage(Matcher<const ipc::SharedMessage&>(_))).Times(0);
        EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveCompleted(_, false));

        capability->playSoundFile(path, channel);
    }
}

namespace {
    MATCHER(VerifyHandshakeResponseEventMessage, "description") {
        const quasar::proto::Remoting& remoting = arg;
        if (!remoting.has_file_player_capability_events_method()) {
            *result_listener << "no file_player_capability_event";
            return false;
        }
        const auto& method = remoting.file_player_capability_events_method();
        if (!method.has_handshake_response()) {
            *result_listener << "not handshake_response event";
        }
        return true;
    }

    MATCHER_P(VerifyPlayStartedEventMessage, requestId, "description") {
        const quasar::proto::Remoting& remoting = arg;
        if (!remoting.has_file_player_capability_events_method()) {
            *result_listener << "no file_player_capability_event";
            return false;
        }
        const auto& method = remoting.file_player_capability_events_method();
        if (!method.has_directive_event() || !method.directive_event().has_started_event()) {
            *result_listener << "not started_event event";
        }
        const auto& directive = method.directive_event().directive();
        if (!directive.has_request_id() || directive.request_id() != requestId) {
            *result_listener << "request_id != requestId";
            return false;
        }
        return true;
    }

    MATCHER_P(VerifyPlayCompletedEventMessage, requestId, "description") {
        const quasar::proto::Remoting& remoting = arg;
        if (!remoting.has_file_player_capability_events_method()) {
            *result_listener << "no file_player_capability_event";
            return false;
        }
        const auto& method = remoting.file_player_capability_events_method();
        if (!method.has_directive_event() || !method.directive_event().has_completed_event()) {
            *result_listener << "not completed_event event";
        }
        const auto& directive = method.directive_event().directive();
        if (!directive.has_request_id() || directive.request_id() != requestId) {
            *result_listener << "request_id != requestId";
            return false;
        }
        return true;
    }
} // namespace

namespace {
    class TestRemotingFixture: public FilePlayerCapabilityFixture {
    public:
        static proto::Remoting buildRemotingHandshakeMessage() {
            proto::Remoting remoting;
            auto method = remoting.mutable_file_player_capability_method();
            method->set_method(quasar::proto::Remoting::FilePlayerCapabilityMethod::HANDSHAKE_REQUEST);

            return remoting;
        }

        static proto::Remoting buildRemotingPlayMessage(const std::string& fileName,
                                                        std::optional<quasar::proto::AudioChannel> channel,
                                                        std::string requestId) {
            proto::Remoting remoting;
            auto method = remoting.mutable_file_player_capability_method();
            method->set_method(quasar::proto::Remoting::FilePlayerCapabilityMethod::PLAY_SOUND_FILE);
            method->set_file_name(TString(fileName));
            if (channel.has_value()) {
                method->set_channel(channel.value());
            }
            method->mutable_play_params()->set_request_id(TString(requestId));
            method->set_send_events(true);

            return remoting;
        }

        static proto::Remoting buildRemotingStopMessage(const std::string& fileName) {
            proto::Remoting remoting;
            auto method = remoting.mutable_file_player_capability_method();
            method->set_method(quasar::proto::Remoting::FilePlayerCapabilityMethod::STOP_SOUND_FILE);
            method->set_file_name(TString(fileName));

            return remoting;
        }

    public:
        void SetUp(NUnitTest::TTestContext& context) override {
            FilePlayerCapabilityFixture::SetUp(context);
            init();
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            FilePlayerCapabilityFixture::TearDown(context);
        }

        void init() {
            remoteConnectionMock = std::make_shared<YandexIO::MockIRemotingConnection>();

            file = createFile("sound_file.wav");
            channel = std::optional<proto::AudioChannel>(proto::DIALOG_CHANNEL);
            requestId = makeUUID();
        }

    public:
        std::shared_ptr<YandexIO::MockIRemotingConnection> remoteConnectionMock;
        std::string file;
        std::optional<proto::AudioChannel> channel;
        std::string requestId;
    };

} // namespace

Y_UNIT_TEST_SUITE_F(FilePlayerCapabilityTest_remoting, TestRemotingFixture) {
    Y_UNIT_TEST(testHandshake) {
        EXPECT_CALL(*remoteConnectionMock, sendMessage(VerifyHandshakeResponseEventMessage()));
        capability->handleRemotingMessage(buildRemotingHandshakeMessage(), remoteConnectionMock);
    }

    Y_UNIT_TEST(testPlay) {
        EXPECT_CALL(activityTrackerMock, addActivity(VerifyActivity(channel.value())));
        expectSoundFilePlayDirective(channel);

        std::string playerId;
        expectPlayAudio(file, channel, playerId);

        capability->handleRemotingMessage(buildRemotingPlayMessage(file, channel, requestId), remoteConnectionMock);

        EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveStarted(_));
        EXPECT_CALL(*remoteConnectionMock, sendMessage(VerifyPlayStartedEventMessage(requestId)));
        capability->onAudioClientMessage(buildPlayingEvent(playerId));
    }

    Y_UNIT_TEST(testStopSoundFile) {
        std::string playerId;
        expectPlayAudio(file, channel, playerId);
        capability->handleRemotingMessage(buildRemotingPlayMessage(file, channel, requestId), remoteConnectionMock);

        EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveStarted(_));
        EXPECT_CALL(*remoteConnectionMock, sendMessage(VerifyPlayStartedEventMessage(requestId)));
        capability->onAudioClientMessage(buildPlayingEvent(playerId));

        EXPECT_CALL(*remoteConnectionMock, sendMessage(VerifyPlayCompletedEventMessage(requestId)));
        EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveCompleted(_, true));
        EXPECT_CALL(activityTrackerMock, removeActivity(VerifyActivity(channel.value())));
        expectSoundFileStopDirective();
        EXPECT_CALL(*audioClientConnectorMock, sendMessage(Matcher<const ipc::SharedMessage&>(VerifyCleanPlayersMessage(playerId))));

        capability->handleRemotingMessage(buildRemotingStopMessage(file), remoteConnectionMock);
    }

    Y_UNIT_TEST(testFilePlayFinished) {
        std::string playerId;
        expectPlayAudio(file, channel, playerId);
        capability->handleRemotingMessage(buildRemotingPlayMessage(file, channel, requestId), remoteConnectionMock);

        EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveStarted(_));
        EXPECT_CALL(*remoteConnectionMock, sendMessage(VerifyPlayStartedEventMessage(requestId)));
        capability->onAudioClientMessage(buildPlayingEvent(playerId));

        EXPECT_CALL(*remoteConnectionMock, sendMessage(VerifyPlayCompletedEventMessage(requestId)));
        EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveCompleted(_, true));
        EXPECT_CALL(activityTrackerMock, removeActivity(VerifyActivity(channel.value())));
        EXPECT_CALL(*audioClientConnectorMock, sendMessage(Matcher<const ipc::SharedMessage&>(VerifyCleanPlayersMessage(playerId))));
        capability->onAudioClientMessage(buildFinishedEvent(playerId));
    }

    Y_UNIT_TEST(testFilePlayFailed) {
        std::string playerId;
        expectPlayAudio(file, channel, playerId);
        capability->handleRemotingMessage(buildRemotingPlayMessage(file, channel, requestId), remoteConnectionMock);

        EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveStarted(_));
        EXPECT_CALL(*remoteConnectionMock, sendMessage(VerifyPlayStartedEventMessage(requestId)));
        capability->onAudioClientMessage(buildPlayingEvent(playerId));

        EXPECT_CALL(*remoteConnectionMock, sendMessage(VerifyPlayCompletedEventMessage(requestId)));
        EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveCompleted(_, false));
        EXPECT_CALL(activityTrackerMock, removeActivity(VerifyActivity(channel.value())));
        EXPECT_CALL(*audioClientConnectorMock, sendMessage(Matcher<const ipc::SharedMessage&>(VerifyCleanPlayersMessage(playerId))));
        capability->onAudioClientMessage(buildFailedEvent(playerId));
    }

    Y_UNIT_TEST(testCancelFilePlay) {
        std::string playerId;
        expectPlayAudio(file, channel, playerId);
        capability->handleRemotingMessage(buildRemotingPlayMessage(file, channel, requestId), remoteConnectionMock);

        EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveStarted(_));
        EXPECT_CALL(*remoteConnectionMock, sendMessage(VerifyPlayStartedEventMessage(requestId)));
        capability->onAudioClientMessage(buildPlayingEvent(playerId));

        EXPECT_CALL(*remoteConnectionMock, sendMessage(VerifyPlayCompletedEventMessage(requestId)));
        EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveCompleted(_, false)).Times(0);
        EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveCompleted(_, true)).Times(0);
        EXPECT_CALL(activityTrackerMock, removeActivity(VerifyActivity(channel.value())));
        EXPECT_CALL(*audioClientConnectorMock, sendMessage(Matcher<const ipc::SharedMessage&>(VerifyCleanPlayersMessage(playerId))));

        YandexIO::Directive::Data data(Directives::LISTEN, "local_action");
        data.requestId = "someRequestId";
        auto request = VinsRequest::createListenRequest(data, VinsRequest::createHardwareButtonClickEventSource());
        directiveProcessor->addDirectives(std::list<std::shared_ptr<Directive>>{
            std::make_shared<AliceRequestDirective>(std::move(request), nullptr, true)});
    }
}

namespace {
    class TestSchedulingFixture: public TestRemotingFixture {
        void SetUp(NUnitTest::TTestContext& context) override {
            QuasarUnitTestFixture::SetUp(context);
            init();
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            TestRemotingFixture::TearDown(context);
        }

        void init() {
            innerSetUp();
            EXPECT_CALL(*audioClientConnectorMock, setConnectHandler(_))
                .WillOnce(Invoke([this](std::function<void()> func) {
                    connectHandler = func;
                }));
            capability = std::make_shared<FilePlayerCapability>(
                std::make_shared<TestCallbackQueue>(),
                getDeviceForTests(),
                activityTrackerMock,
                directiveProcessorMock,
                ipcFactoryForTests(),
                std::weak_ptr<YandexIO::IRemotingRegistry>());
            ASSERT_TRUE(directiveProcessor->addDirectiveHandler(capability));

            remoteConnectionMock = std::make_shared<YandexIO::MockIRemotingConnection>();
        }

    public:
        std::function<void()> connectHandler;
    };
} // namespace

Y_UNIT_TEST_SUITE_F(FilePlayerCapabilityTest_scheduling, TestSchedulingFixture) {
    Y_UNIT_TEST(testHandshake) {
        capability->handleRemotingMessage(buildRemotingHandshakeMessage(), remoteConnectionMock);

        EXPECT_CALL(*remoteConnectionMock, sendMessage(VerifyHandshakeResponseEventMessage()));
        connectHandler();
    }

    Y_UNIT_TEST(testOrder) {
        auto anotherRemoteConnectionMock = std::make_shared<YandexIO::MockIRemotingConnection>();

        capability->handleRemotingMessage(buildRemotingHandshakeMessage(), remoteConnectionMock);
        capability->handleRemotingMessage(buildRemotingHandshakeMessage(), anotherRemoteConnectionMock);

        {
            InSequence seq;
            EXPECT_CALL(*remoteConnectionMock, sendMessage(VerifyHandshakeResponseEventMessage()));
            EXPECT_CALL(*anotherRemoteConnectionMock, sendMessage(VerifyHandshakeResponseEventMessage()));
        }
        connectHandler();
    }
}

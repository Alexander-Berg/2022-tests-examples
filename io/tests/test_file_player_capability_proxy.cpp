#include <yandex_io/capabilities/file_player/file_player_capability_proxy.h>
#include <yandex_io/capabilities/file_player/interfaces/mocks/mock_i_play_sound_file_listener.h>

#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/ipc/mock/mock_i_connector.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/tests/testlib/test_callback_queue.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <yandex_io/sdk/private/remoting/mocks/mock_i_remoting_connection.h>

#include <future>

using namespace quasar;
using namespace testing;
using namespace YandexIO;

using ProtoPlayParams = proto::Remoting::FilePlayerCapabilityMethod::FilePlayerPlayParams;

namespace {
    MATCHER(VerifyHandshakeRequestMessage, "description") {
        const quasar::proto::Remoting& remoting = arg;
        if (!remoting.has_file_player_capability_method()) {
            *result_listener << "no remoting message";
            return false;
        }
        const auto& method = remoting.file_player_capability_method();
        if (!method.has_method() || method.method() != proto::Remoting::FilePlayerCapabilityMethod::STOP_SOUND_FILE) {
            *result_listener << "not HANDSHAKE_REQUEST method";
        }
        return true;
    }

    MATCHER_P(VerifyStopSoundFileMessage, fileName, "description") {
        const quasar::proto::Remoting& remoting = arg;
        if (!remoting.has_file_player_capability_method()) {
            *result_listener << "no remoting message";
            return false;
        }
        const auto& method = remoting.file_player_capability_method();
        if (!method.has_method() || method.method() != proto::Remoting::FilePlayerCapabilityMethod::STOP_SOUND_FILE) {
            *result_listener << "not STOP_SOUND_FILE method";
        }
        if (!method.has_file_name() || method.file_name() != fileName) {
            *result_listener << "file_name != fileName";
            return false;
        }
        return true;
    }

    MATCHER_P3(VerifyPlaySoundFileMessage, fileName, optChannel, sendEvents, "description") {
        const quasar::proto::Remoting& remoting = arg;
        if (!remoting.has_file_player_capability_method()) {
            *result_listener << "no remoting message";
            return false;
        }
        const auto& method = remoting.file_player_capability_method();
        if (!method.has_method() || method.method() != proto::Remoting::FilePlayerCapabilityMethod::PLAY_SOUND_FILE) {
            *result_listener << "not PLAY_SOUND_FILE method";
        }
        if (!method.has_file_name() || method.file_name() != fileName) {
            *result_listener << "file_name != fileName";
            return false;
        }
        if (method.send_events() != sendEvents) {
            *result_listener << "send_events != sendEvents";
            return false;
        }

        if (!optChannel.has_value()) {
            if (method.has_channel()) {
                *result_listener << "found unexpected channel";
                return false;
            }
        } else if (!method.has_channel() || optChannel.value() != method.channel()) {
            *result_listener << "wrong channel";
            return false;
        }
        return true;
    }
} // anonymous namespace

namespace {
    class FilePlayerCapabilityProxyFixture: public QuasarUnitTestFixture {
    public:
        void SetUp(NUnitTest::TTestContext& context) override {
            QuasarUnitTestFixture::SetUp(context);
            init();
        }

        void expectPlayMessageWithPlayParams(std::function<void(ProtoPlayParams)> func, bool sendEvents = false) {
            EXPECT_CALL(*remoteConnectionMock,
                        sendMessage(VerifyPlaySoundFileMessage(fileName, dialogChannel, sendEvents)))
                .WillOnce(Invoke([func](const quasar::proto::Remoting& remoting) {
                    UNIT_ASSERT(remoting.file_player_capability_method().has_play_params() == true);
                    func(remoting.file_player_capability_method().play_params());
                }));
        }

        void expectHandshake() {
            EXPECT_CALL(*remoteConnectionMock,
                        sendMessage(VerifyHandshakeRequestMessage()))
                .WillOnce(Invoke([this](const quasar::proto::Remoting& /*message*/) {
                    quasar::proto::Remoting remoting;
                    remoting.mutable_file_player_capability_events_method()->mutable_handshake_response();

                    capability->handleRemotingMessage(buildHandshakeResponse(), remoteConnectionMock);
                }));
        }

    protected:
        static quasar::proto::Remoting buildHandshakeResponse() {
            quasar::proto::Remoting remoting;
            remoting.mutable_file_player_capability_events_method()->mutable_handshake_response();

            return remoting;
        }

        void init() {
            remoteConnectionMock = std::make_shared<YandexIO::MockIRemotingConnection>();
            capability = std::make_shared<FilePlayerCapabilityProxy>(std::weak_ptr<YandexIO::IRemotingRegistry>(),
                                                                     std::make_shared<TestCallbackQueue>());

            expectHandshake();

            capability->handleRemotingConnect(remoteConnectionMock);
        }

    public:
        const std::string fileName = "pepega.mp3";
        const std::optional<proto::AudioChannel> dialogChannel = proto::DIALOG_CHANNEL;

        std::shared_ptr<FilePlayerCapabilityProxy> capability;
        std::shared_ptr<YandexIO::MockIRemotingConnection> remoteConnectionMock;
    };

} // anonymous namespace

Y_UNIT_TEST_SUITE_F(FilePlayerCapabilityProxyTest, FilePlayerCapabilityProxyFixture) {
    Y_UNIT_TEST(testStopSoundFile) {
        EXPECT_CALL(*remoteConnectionMock,
                    sendMessage(VerifyStopSoundFileMessage(fileName)));

        capability->stopSoundFile(fileName);
    }

    Y_UNIT_TEST(testPlaySoundFile) {
        {
            const auto channel = std::optional<proto::AudioChannel>(proto::DIALOG_CHANNEL);

            EXPECT_CALL(*remoteConnectionMock,
                        sendMessage(VerifyPlaySoundFileMessage(fileName, channel, false)));

            capability->playSoundFile(fileName, channel);
        }
        {
            const auto channel = std::optional<proto::AudioChannel>(std::nullopt);

            EXPECT_CALL(*remoteConnectionMock,
                        sendMessage(VerifyPlaySoundFileMessage(fileName, channel, false)));

            capability->playSoundFile(fileName, channel);
        }
    }

    Y_UNIT_TEST(testPlaySoundFileParams_requestId) {
        const std::string requestId = makeUUID();

        expectPlayMessageWithPlayParams([requestId](ProtoPlayParams protoPlayParams) {
            UNIT_ASSERT(protoPlayParams.has_request_id() == true);
            UNIT_ASSERT_VALUES_EQUAL(protoPlayParams.request_id(), requestId);
        });

        IFilePlayerCapability::PlayParams params{
            .requestId = requestId};
        capability->playSoundFile(fileName, dialogChannel, params);
    }

    Y_UNIT_TEST(testPlaySoundFileParams_requestId_empty) {
        expectPlayMessageWithPlayParams([](ProtoPlayParams protoPlayParams) {
            YIO_LOG_INFO("If not specified, request_id should be generated by proxy");
            UNIT_ASSERT(protoPlayParams.has_request_id() == true);
        });

        capability->playSoundFile(fileName, dialogChannel);
    }

    Y_UNIT_TEST(testPlaySoundFileParams_parentRequestId) {
        const std::string parentRequestId = makeUUID();

        expectPlayMessageWithPlayParams([parentRequestId](ProtoPlayParams protoPlayParams) {
            UNIT_ASSERT(protoPlayParams.has_parent_request_id() == true);
            UNIT_ASSERT_VALUES_EQUAL(protoPlayParams.parent_request_id(), parentRequestId);
        });

        IFilePlayerCapability::PlayParams params{
            .parentRequestId = parentRequestId};
        capability->playSoundFile(fileName, dialogChannel, params);
    }

    Y_UNIT_TEST(testPlaySoundFileParams_playLooped) {
        expectPlayMessageWithPlayParams([](ProtoPlayParams protoPlayParams) {
            UNIT_ASSERT(protoPlayParams.has_play_looped() == true);
            UNIT_ASSERT_VALUES_EQUAL(protoPlayParams.play_looped(), true);
        });

        IFilePlayerCapability::PlayParams params{
            .playLooped = true};
        capability->playSoundFile(fileName, dialogChannel, params);
    }

    Y_UNIT_TEST(testPlaySoundFileParams_playTimes) {
        uint32_t playTimes = 2;

        expectPlayMessageWithPlayParams([playTimes](ProtoPlayParams protoPlayParams) {
            UNIT_ASSERT(protoPlayParams.has_play_times() == true);
            UNIT_ASSERT_VALUES_EQUAL(protoPlayParams.play_times(), playTimes);
        });

        IFilePlayerCapability::PlayParams params{
            .playTimes = playTimes};
        capability->playSoundFile(fileName, dialogChannel, params);
    }
}

namespace {
    class TestCallbacksFixture: public FilePlayerCapabilityProxyFixture {
    public:
        void SetUp(NUnitTest::TTestContext& context) override {
            QuasarUnitTestFixture::SetUp(context);
            init();
            FilePlayerCapabilityProxyFixture::init();
        }

        void expectCallbackPlayMessage() {
            expectPlayMessageWithPlayParams([this](ProtoPlayParams protoPlayParams) {
                UNIT_ASSERT(protoPlayParams.has_request_id() == true);
                UNIT_ASSERT_VALUES_EQUAL(protoPlayParams.request_id(), requestId);

                playMessageReceived.set_value();
            }, true);
        }

    private:
        void init() {
            mockListener = std::make_shared<MockIPlaySoundFileListener>();
        }

    public:
        std::shared_ptr<MockIPlaySoundFileListener> mockListener;
        std::promise<void> playMessageReceived;
        std::string requestId = makeUUID();
    };

} // anonymous namespace

Y_UNIT_TEST_SUITE_F(FilePlayerCapabilityProxyTestCallbacks, TestCallbacksFixture) {
    Y_UNIT_TEST(testOnStarted) {
        expectCallbackPlayMessage();

        IFilePlayerCapability::PlayParams params{
            .requestId = requestId};
        capability->playSoundFile(fileName, dialogChannel, params, mockListener);
        playMessageReceived.get_future().get();

        EXPECT_CALL(*mockListener, onStarted());

        quasar::proto::Remoting remoting;
        auto method = remoting.mutable_file_player_capability_events_method();
        method->mutable_directive_event()->mutable_started_event();
        method->mutable_directive_event()->mutable_directive()->set_request_id(TString(requestId));
        capability->handleRemotingMessage(remoting, remoteConnectionMock);
    }

    Y_UNIT_TEST(testOnCompleted) {
        expectCallbackPlayMessage();

        IFilePlayerCapability::PlayParams params{
            .requestId = requestId};
        capability->playSoundFile(fileName, dialogChannel, params, mockListener);
        playMessageReceived.get_future().get();

        EXPECT_CALL(*mockListener, onCompleted());

        quasar::proto::Remoting remoting;
        auto method = remoting.mutable_file_player_capability_events_method();
        method->mutable_directive_event()->set_completed_event(proto::DirectiveEvent::SUCCESS);
        method->mutable_directive_event()->mutable_directive()->set_request_id(TString(requestId));
        capability->handleRemotingMessage(remoting, remoteConnectionMock);
    }

    Y_UNIT_TEST(testWrongReqId) {
        expectCallbackPlayMessage();

        IFilePlayerCapability::PlayParams params{
            .requestId = requestId};
        capability->playSoundFile(fileName, dialogChannel, params, mockListener);
        playMessageReceived.get_future().get();

        EXPECT_CALL(*mockListener, onStarted()).Times(0);
        EXPECT_CALL(*mockListener, onCompleted()).Times(0);

        quasar::proto::Remoting remoting;
        auto method = remoting.mutable_file_player_capability_events_method();
        method->mutable_directive_event()->set_completed_event(proto::DirectiveEvent::SUCCESS);
        method->mutable_directive_event()->mutable_directive()->set_request_id(TString(makeUUID()));
        capability->handleRemotingMessage(remoting, remoteConnectionMock);
    }
}

namespace {
    class TestSchedulingFixture: public FilePlayerCapabilityProxyFixture {
    public:
        void SetUp(NUnitTest::TTestContext& context) override {
            QuasarUnitTestFixture::SetUp(context);
            init();
        }

    private:
        void init() {
            capability = std::make_shared<FilePlayerCapabilityProxy>(std::weak_ptr<YandexIO::IRemotingRegistry>(),
                                                                     std::make_shared<TestCallbackQueue>());
            remoteConnectionMock = std::make_shared<YandexIO::MockIRemotingConnection>();
        }

    public:
        std::function<void()> connectHandler;
    };

} // anonymous namespace

Y_UNIT_TEST_SUITE_F(FilePlayerCapabilityProxyTestScheduling, TestSchedulingFixture) {
    Y_UNIT_TEST(testPlayScheduling) {
        const auto channel = std::optional<proto::AudioChannel>(proto::DIALOG_CHANNEL);
        capability->playSoundFile(fileName, channel);

        {
            InSequence seq;
            expectHandshake();
            EXPECT_CALL(*remoteConnectionMock,
                        sendMessage(VerifyPlaySoundFileMessage(fileName, channel, false)));
        }
        capability->handleRemotingConnect(remoteConnectionMock);
    }

    Y_UNIT_TEST(testSchedulingOrder) {
        const std::string firstFile = "kek.mp3";
        const auto firstChannel = std::optional<proto::AudioChannel>(proto::DIALOG_CHANNEL);
        const std::string secondFile = "shmek.mp3";
        const auto secondChannel = std::optional<proto::AudioChannel>(proto::CONTENT_CHANNEL);
        const std::string thirdFile = "pfek.mp3";
        const auto thirdChannel = std::optional<proto::AudioChannel>(std::nullopt);

        capability->playSoundFile(firstFile, firstChannel);
        capability->playSoundFile(secondFile, secondChannel);
        capability->playSoundFile(thirdFile, thirdChannel);

        {
            InSequence seq;
            expectHandshake();
            EXPECT_CALL(*remoteConnectionMock,
                        sendMessage(VerifyPlaySoundFileMessage(firstFile, firstChannel, false)));
            EXPECT_CALL(*remoteConnectionMock,
                        sendMessage(VerifyPlaySoundFileMessage(secondFile, secondChannel, false)));
            EXPECT_CALL(*remoteConnectionMock,
                        sendMessage(VerifyPlaySoundFileMessage(thirdFile, thirdChannel, false)));
        }
        capability->handleRemotingConnect(remoteConnectionMock);
    }

    Y_UNIT_TEST(testStopCancelsScheduledPlay) {
        const auto channel = std::optional<proto::AudioChannel>(proto::DIALOG_CHANNEL);

        capability->playSoundFile(fileName, channel);
        capability->stopSoundFile(fileName);

        expectHandshake();
        EXPECT_CALL(*remoteConnectionMock,
                    sendMessage(VerifyPlaySoundFileMessage(fileName, channel, false)))
            .Times(0);
        EXPECT_CALL(*remoteConnectionMock,
                    sendMessage(VerifyStopSoundFileMessage(fileName)))
            .Times(0);

        capability->handleRemotingConnect(remoteConnectionMock);
    }

    Y_UNIT_TEST(testPlaySameSoundOnlyOnce) {
        const auto channel = std::optional<proto::AudioChannel>(proto::DIALOG_CHANNEL);

        capability->playSoundFile(fileName, channel);
        capability->playSoundFile(fileName, channel);
        capability->playSoundFile(fileName, channel);

        expectHandshake();
        EXPECT_CALL(*remoteConnectionMock,
                    sendMessage(VerifyPlaySoundFileMessage(fileName, channel, false)))
            .Times(1);

        capability->handleRemotingConnect(remoteConnectionMock);
    }

    Y_UNIT_TEST(testReplaceSoundsInSameChannel) {
        const std::string firstFile = "kek.mp3";
        const std::string secondFile = "shmek.mp3";
        const auto channel = std::optional<proto::AudioChannel>(proto::DIALOG_CHANNEL);

        capability->playSoundFile(firstFile, channel);
        capability->playSoundFile(secondFile, channel);

        expectHandshake();
        EXPECT_CALL(*remoteConnectionMock,
                    sendMessage(VerifyPlaySoundFileMessage(firstFile, channel, false)))
            .Times(0);
        EXPECT_CALL(*remoteConnectionMock,
                    sendMessage(VerifyPlaySoundFileMessage(secondFile, channel, false)))
            .Times(1);
        capability->handleRemotingConnect(remoteConnectionMock);
    }
}

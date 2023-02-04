#include <yandex_io/services/aliced/capabilities/legacy_player_capability/legacy_player_capability.h>
#include <yandex_io/services/aliced/device_state/alice_device_state.h>
#include <yandex_io/services/aliced/directive_processor/mocks/mock_i_directive_processor.h>

#include <yandex_io/libs/base/directives.h>
#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/ipc/mock/mock_i_connector.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/logging/logging.h>

#include <yandex_io/sdk/interfaces/directive.h>

#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/null_device_state_capability/null_device_state_capability.h>
#include <yandex_io/tests/testlib/test_callback_queue.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <yandex_io/protos/quasar_proto.pb.h>

using namespace quasar;
using namespace testing;
using namespace YandexIO;

namespace {
    bool verifyMediaResume(const proto::MediaRequest& request) {
        if (!request.has_resume()) {
            YIO_LOG_WARN("There is no media_request.resume event");
            return false;
        }

        return true;
    }

    bool verifyPlayAudio(const proto::MediaRequest& request) {
        if (!request.has_play_audio()) {
            YIO_LOG_WARN("There is no media_request.play_audio event");
            return false;
        }

        return true;
    }

    bool verifyPlayRadio(const proto::MediaRequest& request) {
        if (!request.has_play_radio()) {
            YIO_LOG_WARN("There is no media_request.play_radio event");
            return false;
        }

        return true;
    }

    std::string generateUUID() {
        auto uuid = makeUUID();
        YIO_LOG_INFO("Generated uuid: " << uuid);
        return uuid;
    }

    std::shared_ptr<Directive> buildDirective(const std::string& name,
                                              const std::string& requestId,
                                              const std::string& payloadStr) {
        Json::Value payload = tryParseJsonOrEmpty(payloadStr);
        YIO_LOG_INFO("Creating directive " << name);
        Directive::Data data(name, "local_action", std::move(payload));
        data.requestId = requestId;
        return std::make_shared<Directive>(std::move(data));
    }

    std::shared_ptr<Directive> getMusicPlayDirective(const std::string& requestId) {
        return buildDirective(Directives::MUSIC_PLAY, requestId, "{\"session_id\":\"session_id\"}");
    }

    std::shared_ptr<Directive> getRadioPlayDirective(const std::string& requestId) {
        return buildDirective(Directives::RADIO_PLAY, requestId, "{\"streamUrl\":\"some_url\"}");
    }
} // namespace

namespace {

    class LegacyPlayerCapabilityFixture: public QuasarUnitTestFixture {
    public:
        void SetUp(NUnitTest::TTestContext& context) override {
            QuasarUnitTestFixture::SetUp(context);
            init();
        }

        void expectOnHandleDirectiveStarted(std::shared_ptr<Directive> expectedDirective) const {
            EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveStarted(_))
                .WillOnce(Invoke([expectedDirective](std::shared_ptr<Directive> directive) {
                    UNIT_ASSERT_VALUES_EQUAL(expectedDirective->getData().name, directive->getData().name);
                    UNIT_ASSERT_VALUES_EQUAL(expectedDirective->getRequestId(), directive->getRequestId());
                }));
        }

    private:
        void init() {
            directiveProcessorMock = std::make_shared<MockIDirectiveProcessor>();

            mediadPromise = std::make_unique<std::promise<proto::MediaRequest>>();
            mockMediad = createIpcServerForTests("mediad");
            mockMediad->setMessageHandler([this](const auto& request, auto& /*connection*/) {
                if (request->has_media_request()) {
                    mediadPromise->set_value(request->media_request());
                }
            });
            mockMediad->listenService();

            interfacedPromise = std::make_unique<std::promise<proto::MediaRequest>>();
            mockInterfaced = createIpcServerForTests("interfaced");
            mockInterfaced->setMessageHandler([this](const auto& request, auto& /*connection*/) {
                if (request->has_media_request()) {
                    interfacedPromise->set_value(request->media_request());
                }
            });
            mockInterfaced->listenService();

            toInterfaced_ = createIpcConnectorForTests("interfaced");
            toInterfaced_->connectToService();

            toMediad_ = createIpcConnectorForTests("mediad");
            toMediad_->connectToService();

            YandexIO::Configuration::TestGuard testGuard;
            Json::Value& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
            config["interfaced"]["port"] = 1;
            ASSERT_TRUE(getDeviceForTests()->configuration()->hasServiceConfig("interfaced"));

            deviceContext_ = std::make_shared<YandexIO::DeviceContext>(ipcFactoryForTests());

            capability = std::make_shared<LegacyPlayerCapability>(
                directiveProcessorMock,
                deviceState,
                getDeviceForTests(),
                *deviceContext_,
                toMediad_,
                toInterfaced_,
                std::make_shared<NullDeviceStateCapability>());
        }

    public:
        std::shared_ptr<MockIDirectiveProcessor> directiveProcessorMock;
        AliceDeviceState deviceState{"", nullptr, nullptr, EnvironmentStateHolder("", nullptr)};
        std::shared_ptr<LegacyPlayerCapability> capability;

        std::unique_ptr<std::promise<proto::MediaRequest>> mediadPromise;
        std::unique_ptr<std::promise<proto::MediaRequest>> interfacedPromise;

        std::shared_ptr<ipc::IServer> mockMediad;
        std::shared_ptr<ipc::IServer> mockInterfaced;
        std::shared_ptr<ipc::IConnector> toMediad_;
        std::shared_ptr<ipc::IConnector> toInterfaced_;
        std::shared_ptr<YandexIO::DeviceContext> deviceContext_;
    };

} // anonymous namespace

Y_UNIT_TEST_SUITE(LegacyPlayerCapabilityTest) {
    Y_UNIT_TEST_F(testPlayerContinueDirective, LegacyPlayerCapabilityFixture) {
        {
            Json::Value payload;
            payload["player"] = "music";
            auto directive = std::make_shared<Directive>(
                Directive::Data(Directives::PLAYER_CONTINUE, "local_action", std::move(payload)));

            capability->handleDirective(directive);

            const auto mediadMessage = mediadPromise->get_future().get();
            ASSERT_TRUE(verifyMediaResume(mediadMessage));
            const auto interfacedMessage = interfacedPromise->get_future().get();
            ASSERT_TRUE(verifyPlayAudio(interfacedMessage));
        }

        interfacedPromise = std::make_unique<std::promise<proto::MediaRequest>>();

        {
            Json::Value payload;
            payload["player"] = "video";
            auto directive = std::make_shared<Directive>(
                Directive::Data(Directives::PLAYER_CONTINUE, "local_action", std::move(payload)));

            capability->handleDirective(directive);
            const auto interfacedMessage = interfacedPromise->get_future().get();
            ASSERT_TRUE(verifyMediaResume(interfacedMessage));
        }
    }

    Y_UNIT_TEST_F(testNotifyDirectiveStarted_music, LegacyPlayerCapabilityFixture) {
        const auto directive = getMusicPlayDirective(generateUUID());

        capability->handleDirective(directive);

        const auto mediadMessage = mediadPromise->get_future().get();
        ASSERT_TRUE(verifyPlayAudio(mediadMessage));

        expectOnHandleDirectiveStarted(directive);

        auto message = ipc::buildMessage([&mediadMessage](auto& msg) {
            msg.mutable_legacy_player_state_changed()->set_state(proto::LegacyPlayerStateChanged::STARTED);
            msg.mutable_legacy_player_state_changed()->set_player_type(proto::LegacyPlayerStateChanged::YANDEX_MUSIC);
            msg.mutable_legacy_player_state_changed()->set_vins_request_id(mediadMessage.vins_request_id());
        });

        capability->handleMediadMessage(message);
    }

    Y_UNIT_TEST_F(testNotifyDirectiveStarted_radio, LegacyPlayerCapabilityFixture) {
        const auto directive = getRadioPlayDirective(generateUUID());

        capability->handleDirective(directive);

        const auto mediadMessage = mediadPromise->get_future().get();
        ASSERT_TRUE(verifyPlayRadio(mediadMessage));

        expectOnHandleDirectiveStarted(directive);

        auto message = ipc::buildMessage([&mediadMessage](auto& msg) {
            msg.mutable_legacy_player_state_changed()->set_state(proto::LegacyPlayerStateChanged::STARTED);
            msg.mutable_legacy_player_state_changed()->set_player_type(proto::LegacyPlayerStateChanged::YANDEX_RADIO);
            msg.mutable_legacy_player_state_changed()->set_vins_request_id(mediadMessage.vins_request_id());
        });

        capability->handleMediadMessage(message);
    }

    Y_UNIT_TEST_F(testNotifyDirectiveStarted_javaRadio, LegacyPlayerCapabilityFixture) {
        const auto directive = getRadioPlayDirective(generateUUID());

        capability->handleDirective(directive);

        const auto mediadMessage = mediadPromise->get_future().get();
        ASSERT_TRUE(verifyPlayRadio(mediadMessage));

        expectOnHandleDirectiveStarted(directive);

        auto message = ipc::buildMessage([&mediadMessage](auto& msg) {
            msg.mutable_legacy_player_state_changed()->set_state(proto::LegacyPlayerStateChanged::STARTED);
            msg.mutable_legacy_player_state_changed()->set_player_type(proto::LegacyPlayerStateChanged::JAVA_RADIO);
            msg.mutable_legacy_player_state_changed()->set_vins_request_id(mediadMessage.vins_request_id());
        });

        capability->handleMediadMessage(message);
    }

    Y_UNIT_TEST_F(testNotifyDirectiveStarted_dontTriggerOnOldDirectives, LegacyPlayerCapabilityFixture) {
        const auto oldDirective = getMusicPlayDirective(generateUUID());
        capability->handleDirective(oldDirective);
        const auto oldMediadMessage = mediadPromise->get_future().get();
        ASSERT_TRUE(verifyPlayAudio(oldMediadMessage));

        mediadPromise = std::make_unique<std::promise<proto::MediaRequest>>();
        const auto directive = getMusicPlayDirective(generateUUID());
        capability->handleDirective(directive);
        const auto mediadMessage = mediadPromise->get_future().get();
        ASSERT_TRUE(verifyPlayAudio(mediadMessage));

        EXPECT_CALL(*directiveProcessorMock, onHandleDirectiveStarted(_)).Times(0);
        auto message = ipc::buildMessage([&oldMediadMessage](auto& msg) {
            msg.mutable_legacy_player_state_changed()->set_state(proto::LegacyPlayerStateChanged::STARTED);
            msg.mutable_legacy_player_state_changed()->set_player_type(proto::LegacyPlayerStateChanged::YANDEX_MUSIC);
            msg.mutable_legacy_player_state_changed()->set_vins_request_id(oldMediadMessage.vins_request_id());
        });
        capability->handleMediadMessage(message);
    }
}

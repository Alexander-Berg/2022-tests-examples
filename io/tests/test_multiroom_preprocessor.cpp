#include <yandex_io/services/aliced/capabilities/multiroom_capability/multiroom_preprocessor.h>
#include <yandex_io/services/aliced/capabilities/mrforwarder_capability/mrforwarder.h>

#include <yandex_io/interfaces/multiroom/mock/multiroom_provider.h>
#include <yandex_io/libs/base/directives.h>
#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/libs/json_utils/json_utils.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

#include <yandex_io/interfaces/multiroom/mock/multiroom_provider.h>
#include <yandex_io/tests/testlib/unittest_helper/logging_test_fixture.h>

using namespace quasar;
using namespace testing;
using namespace YandexIO;

namespace {

    class MultiroomPreprocessorTestFixture: public QuasarLoggingTestFixture {
    public:
        MultiroomPreprocessorTestFixture()
            : myDeviceId_("myDeviceId")
        {
        }

        void SetUp(NUnitTest::TTestContext& context) override {
            QuasarLoggingTestFixture::SetUp(context);

            aliceDeviceState_ = std::make_shared<AliceDeviceState>(
                myDeviceId_, nullptr, nullptr, EnvironmentStateHolder("", nullptr));
            mockMultiroomProvider_ = std::make_shared<mock::MultiroomProvider>();

            preprocessor_ = std::make_shared<MultiroomPreprocessor>(
                myDeviceId_, mockMultiroomProvider_, aliceDeviceState_);
        }

    protected:
        const std::string myDeviceId_;
        std::shared_ptr<MultiroomPreprocessor> preprocessor_;
        std::shared_ptr<AliceDeviceState> aliceDeviceState_;
        std::shared_ptr<mock::MultiroomProvider> mockMultiroomProvider_;
    };

    Y_UNIT_TEST_SUITE(MultiroomPreprocessorTest) {
        Y_UNIT_TEST_F(testEnsureMultiroomToken_DoNothingWhenTokenExists, MultiroomPreprocessorTestFixture) {
            const std::string multiroomToken = "testMultiroomToken";

            for (const auto& name : {Directives::START_MULTIROOM, Directives::AUDIO_PLAY}) {
                Directive::Data data(name, "local_action");
                data.payload["multiroom_token"] = multiroomToken;
                std::list<std::shared_ptr<Directive>> directives;
                directives.push_back(std::make_shared<Directive>(std::move(data)));

                preprocessor_->preprocessDirectives(directives);

                ASSERT_FALSE(directives.empty());
                for (const auto& directive : directives) {
                    UNIT_ASSERT(directive->getData().payload["multiroom_token"] == multiroomToken);
                }
            }
        }

        Y_UNIT_TEST_F(testEnsureMultiroomToken_GeneratesWhenTokenEmpty, MultiroomPreprocessorTestFixture) {
            const std::string vinsRequestId = makeUUID();
            const std::string expectedMultiroomToken = makeMultiroomToken(vinsRequestId);

            for (const auto& name : {Directives::START_MULTIROOM, Directives::AUDIO_PLAY}) {
                Directive::Data data(name, "local_action");
                data.requestId = vinsRequestId;
                data.payload["room_id"] = "testRoomId";
                std::list<std::shared_ptr<Directive>> directives;
                directives.push_back(std::make_shared<Directive>(std::move(data)));

                preprocessor_->preprocessDirectives(directives);

                ASSERT_FALSE(directives.empty());
                for (const auto& directive : directives) {
                    ASSERT_FALSE(directive->getData().payload.isMember("room_id"));
                    ASSERT_TRUE(directive->getData().payload["multiroom_token"] == expectedMultiroomToken);
                }
            }
        }

        Y_UNIT_TEST_F(testEnsureMultiroomToken_ReusesTokenByParentRequestId, MultiroomPreprocessorTestFixture) {
            const std::string vinsRequestId = makeUUID();
            const std::string expectedMultiroomToken = "testMultiroomToken";

            {
                Directive::Data data(Directives::START_MULTIROOM, "local_action");
                data.requestId = vinsRequestId;
                data.payload["multiroom_token"] = expectedMultiroomToken;
                std::list<std::shared_ptr<Directive>> directives;
                directives.push_back(std::make_shared<Directive>(std::move(data)));
                preprocessor_->preprocessDirectives(directives);
            }

            Directive::Data data(Directives::AUDIO_PLAY, "local_action");
            data.requestId = makeUUID();
            data.parentRequestId = vinsRequestId;
            std::list<std::shared_ptr<Directive>> directives;
            directives.push_back(std::make_shared<Directive>(std::move(data)));
            preprocessor_->preprocessDirectives(directives);

            ASSERT_FALSE(directives.empty());
            ASSERT_TRUE((*directives.begin())->getData().payload["multiroom_token"] == expectedMultiroomToken);
        }

        Y_UNIT_TEST_F(testEnsureMultiroomToken_InMusicPlay, MultiroomPreprocessorTestFixture) {
            const std::string vinsRequestId = makeUUID();
            const std::string expectedMultiroomToken = makeMultiroomToken(vinsRequestId);

            Directive::Data data(Directives::MUSIC_PLAY, "local_action");
            data.requestId = vinsRequestId;
            std::list<std::shared_ptr<Directive>> directives;
            directives.push_back(std::make_shared<Directive>(std::move(data)));

            preprocessor_->preprocessDirectives(directives);

            ASSERT_FALSE(directives.empty());
            for (const auto& directive : directives) {
                ASSERT_TRUE(directive->getData().payload["multiroom_token"] == expectedMultiroomToken);
            }
        }

        Y_UNIT_TEST_F(testEnsureMultiroomToken_InMusicPlayFromCurrentBroadcast, MultiroomPreprocessorTestFixture) {
            const std::string vinsRequestId = makeUUID();
            const std::string expectedMultiroomToken = makeMultiroomToken(vinsRequestId);

            Directive::Data data(Directives::MUSIC_PLAY, "local_action");
            std::list<std::shared_ptr<Directive>> directives;
            directives.push_back(std::make_shared<Directive>(std::move(data)));

            MultiroomState state;
            state.mode = MultiroomState::Mode::MASTER;
            auto broadcast = std::make_shared<MultiroomState::Broadcast>();
            broadcast->multiroomToken = expectedMultiroomToken;
            broadcast->playingState = MultiroomState::PlayingState::PLAYING;
            state.broadcast = broadcast;
            mockMultiroomProvider_->setMultiroomState(state);

            preprocessor_->preprocessDirectives(directives);

            ASSERT_FALSE(directives.empty());
            for (const auto& directive : directives) {
                ASSERT_TRUE(directive->getData().payload["multiroom_token"] == expectedMultiroomToken);
            }
        }

        Y_UNIT_TEST_F(testEnsureMultiroomToken_SkipsOtherEndpointId, MultiroomPreprocessorTestFixture) {
            const std::string vinsRequestId = makeUUID();
            const std::string expectedMultiroomToken = makeMultiroomToken(vinsRequestId);

            for (const auto& name : {Directives::START_MULTIROOM, Directives::AUDIO_PLAY, Directives::MUSIC_PLAY}) {
                Directive::Data data(name, "local_action");
                data.requestId = vinsRequestId;
                data.endpointId = "otherDeviceId";
                std::list<std::shared_ptr<Directive>> directives;
                directives.push_back(std::make_shared<Directive>(std::move(data)));

                preprocessor_->preprocessDirectives(directives);

                ASSERT_FALSE(directives.empty());
                for (const auto& directive : directives) {
                    ASSERT_FALSE(directive->getData().payload.isMember("multiroom_token"));
                }
            }
        }

        Y_UNIT_TEST_F(testForwardDirectives_replacesOtherDeviceId, MultiroomPreprocessorTestFixture) {
            const std::string otherDeviceId = "otherDeviceId";

            Directive::Data data("any_name", "local_action");
            data.roomDeviceIds = {otherDeviceId};
            std::list<std::shared_ptr<Directive>> directives;
            directives.push_back(std::make_shared<Directive>(std::move(data)));

            preprocessor_->preprocessDirectives(directives);

            ASSERT_FALSE(directives.empty());
            auto firstDirective = *directives.begin();
            ASSERT_TRUE(firstDirective->getData().endpointId == quasar::MRFORWARDER_ENDPOINT_ID);
        }

        Y_UNIT_TEST_F(testForwardDirectives_skipReplaceForOwnDevice, MultiroomPreprocessorTestFixture) {
            const std::string otherDeviceId = "otherDeviceId";

            Directive::Data data("any_name", "local_action");
            data.roomDeviceIds = {otherDeviceId, myDeviceId_};
            std::list<std::shared_ptr<Directive>> directives;
            directives.push_back(std::make_shared<Directive>(std::move(data)));

            preprocessor_->preprocessDirectives(directives);

            ASSERT_FALSE(directives.empty());
            auto firstDirective = *directives.begin();
            ASSERT_TRUE(firstDirective->getData().endpointId.empty());
            ASSERT_TRUE(firstDirective->getData().roomDeviceIds.empty());
        }

        Y_UNIT_TEST_F(testForwardDirectives_skipForEndpointId, MultiroomPreprocessorTestFixture) {
            const std::string otherDeviceId = "otherDeviceId";
            const std::string otherEndpointId = "otherEndpointId";

            Directive::Data data("any_name", "local_action");
            data.endpointId = otherEndpointId;
            data.roomDeviceIds = {otherDeviceId};
            std::list<std::shared_ptr<Directive>> directives;
            directives.push_back(std::make_shared<Directive>(std::move(data)));

            preprocessor_->preprocessDirectives(directives);

            ASSERT_FALSE(directives.empty());
            auto firstDirective = *directives.begin();
            ASSERT_TRUE(firstDirective->getData().endpointId == otherEndpointId);
        }

    }

} // namespace

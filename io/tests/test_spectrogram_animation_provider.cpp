#include <yandex_io/interfaces/spectrogram_animation/connector/spectrogram_animation_provider.h>

#include <yandex_io/libs/base/named_callback_queue.h>
#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/ipc/mock/connector.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/protos/quasar_proto.pb.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;
using namespace quasar::TestUtils;
using namespace testing;

namespace {
    struct Fixture: public QuasarUnitTestFixture {
        Fixture() {
            mockConnector = std::make_shared<ipc::mock::Connector>();
        }

        void setupDefaultExpectCalls()
        {
            EXPECT_CALL(*mockConnector, setMessageHandler(_)).WillOnce(Invoke([&](ipc::IConnector::MessageHandler arg) {
                mockMessageHandler = arg;
            }));

            EXPECT_CALL(*mockConnector, tryConnectToService()).Times(1);
        }
        YandexIO::Configuration::TestGuard testGuard;

        std::shared_ptr<ipc::mock::Connector> mockConnector;
        ipc::IConnector::MessageHandler mockMessageHandler;
    };

} // namespace

Y_UNIT_TEST_SUITE_F(SpectrogramAnimationProvider, Fixture) {
    Y_UNIT_TEST(testSpectrogramAnimationStateInitial)
    {
        setupDefaultExpectCalls();
        SpectrogramAnimationProvider spectrogramAnimationProvider(mockConnector);

        auto state = spectrogramAnimationProvider.spectrogramAnimationState().value();

        UNIT_ASSERT(state);
        UNIT_ASSERT(*state == SpectrogramAnimationState{});
    }

    Y_UNIT_TEST(testSpectrogramAnimationStateChanged)
    {
        setupDefaultExpectCalls();
        SpectrogramAnimationProvider spectrogramAnimationProvider(mockConnector);

        auto state = spectrogramAnimationProvider.spectrogramAnimationState().value();
        UNIT_ASSERT(mockMessageHandler);

        std::atomic<int> signalCount{0};
        spectrogramAnimationProvider.spectrogramAnimationState().connect(
            [&](const auto& /*state*/) {
                ++signalCount;
            }, Lifetime::immortal);
        UNIT_ASSERT_VALUES_EQUAL(signalCount.load(), 1); // initial connect

        proto::QuasarMessage message;
        message.mutable_spectrogram_animation_message()->mutable_state()->set_source(proto::SpectrogramAnimation::State::LOCAL);
        message.mutable_spectrogram_animation_message()->mutable_state()->set_configs(R"({"abc":{},"def":{}})");
        message.mutable_spectrogram_animation_message()->mutable_state()->set_current("def");
        message.mutable_spectrogram_animation_message()->mutable_state()->set_extra_data("ddddata");
        mockMessageHandler(ipc::UniqueMessage{message});

        // Check 1. Get new state
        UNIT_ASSERT_VALUES_EQUAL(signalCount.load(), 2); // new state
        state = spectrogramAnimationProvider.spectrogramAnimationState().value();
        UNIT_ASSERT_VALUES_EQUAL((int)state->source, (int)SpectrogramAnimationState::Source::LOCAL);
        UNIT_ASSERT_VALUES_EQUAL(state->configs, R"({"abc":{},"def":{}})");
        UNIT_ASSERT_VALUES_EQUAL(state->current, "def");
        UNIT_ASSERT_VALUES_EQUAL(state->extraData, "ddddata");

        // Check 2. No changes
        mockMessageHandler(ipc::UniqueMessage{message}); // repeate same state
        UNIT_ASSERT_VALUES_EQUAL(signalCount.load(), 2); // no changes
        auto state2 = spectrogramAnimationProvider.spectrogramAnimationState().value();
        UNIT_ASSERT(*state == *state2);

        // Check 3. Partial changes
        message.mutable_spectrogram_animation_message()->mutable_state()->set_source(proto::SpectrogramAnimation::State::EXTERNAL);
        mockMessageHandler(ipc::UniqueMessage{message});
        UNIT_ASSERT_VALUES_EQUAL(signalCount.load(), 3);
        state = spectrogramAnimationProvider.spectrogramAnimationState().value();
        UNIT_ASSERT_VALUES_EQUAL((int)state->source, (int)SpectrogramAnimationState::Source::EXTERNAL);
        UNIT_ASSERT_VALUES_EQUAL(state->configs, R"({"abc":{},"def":{}})");
        UNIT_ASSERT_VALUES_EQUAL(state->current, "def");
        UNIT_ASSERT_VALUES_EQUAL(state->extraData, "ddddata");
    }

    Y_UNIT_TEST(testSetExternalPresets)
    {
        setupDefaultExpectCalls();
        SpectrogramAnimationProvider spectrogramAnimationProvider(mockConnector);
        UNIT_ASSERT(mockMessageHandler);

        EXPECT_CALL(*mockConnector, sendMessage(Matcher<ipc::Message&&>(_))).WillOnce(Invoke([&](ipc::Message&& m) {
            UNIT_ASSERT(m.has_spectrogram_animation_message());
            UNIT_ASSERT(m.spectrogram_animation_message().has_set_external_presets_signal());
            UNIT_ASSERT_VALUES_EQUAL(m.spectrogram_animation_message().set_external_presets_signal().configs(), "test_configs");
            UNIT_ASSERT_VALUES_EQUAL(m.spectrogram_animation_message().set_external_presets_signal().current(), "test_current");
            UNIT_ASSERT_VALUES_EQUAL(m.spectrogram_animation_message().set_external_presets_signal().extra_data(), "test_extra_data");
            return true;
        }));

        spectrogramAnimationProvider.setExternalPresets("test_configs", "test_current", "test_extra_data");
    }
}

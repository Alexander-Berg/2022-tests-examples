#include <yandex_io/interfaces/stereo_pair/connector/stereo_pair_provider.h>

#include <yandex_io/libs/base/named_callback_queue.h>
#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/ipc/mock/connector.h>
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

            EXPECT_CALL(*mockConnector, connectToService()).Times(1);
        }
        YandexIO::Configuration::TestGuard testGuard;

        std::shared_ptr<ipc::mock::Connector> mockConnector;
        ipc::IConnector::MessageHandler mockMessageHandler;
    };

} // namespace

Y_UNIT_TEST_SUITE_F(StereoPairProvider, Fixture) {
    Y_UNIT_TEST(testStereoPairStateInitial)
    {
        setupDefaultExpectCalls();
        StereoPairProvider stereoPairProvider("DEVICE_ID", mockConnector);

        auto state = stereoPairProvider.stereoPairState().value();

        UNIT_ASSERT(state);
        UNIT_ASSERT_VALUES_EQUAL((int)state->role, (int)StereoPairState::Role::UNDEFINED);
        UNIT_ASSERT_VALUES_EQUAL((int)state->connectivity, (int)StereoPairState::Connectivity::UNDEFINED);
    }

    Y_UNIT_TEST(testStereoPairStateChanged)
    {
        setupDefaultExpectCalls();
        StereoPairProvider stereoPairProvider("DEVICE_ID", mockConnector);
        UNIT_ASSERT(mockMessageHandler);

        auto state1 = stereoPairProvider.stereoPairState().value();
        UNIT_ASSERT(state1);

        auto message = ipc::buildMessage([](auto& msg) {
            auto& s = *msg.mutable_stereo_pair_message()->mutable_state();
            s.set_role(proto::StereoPair::LEADER);
            s.set_connectivity(proto::StereoPair::ONEWAY);
            s.set_channel(proto::StereoPair::CH_RIGHT);
            s.set_stereo_player_status(proto::StereoPair::PLAYER_PARTNER_NOT_READY);
        });
        mockMessageHandler(message);

        auto state2 = stereoPairProvider.stereoPairState().value();
        UNIT_ASSERT(state2);
        UNIT_ASSERT(state1 != state2);
        UNIT_ASSERT(*state1 != *state2);

        UNIT_ASSERT_VALUES_EQUAL((int)state2->role, (int)StereoPairState::Role::LEADER);
        UNIT_ASSERT_VALUES_EQUAL((int)state2->connectivity, (int)StereoPairState::Connectivity::ONEWAY);
        UNIT_ASSERT_VALUES_EQUAL((int)state2->channel, (int)StereoPairState::Channel::RIGHT);
        UNIT_ASSERT_VALUES_EQUAL((int)state2->stereoPlayerStatus, (int)StereoPairState::StereoPlayerStatus::PARTNER_NOT_READY);
    }

    Y_UNIT_TEST(testSpeakNotReadyNotification)
    {
        setupDefaultExpectCalls();
        StereoPairProvider stereoPairProvider("MY_DEVICE_ID", mockConnector);

        EXPECT_CALL(*mockConnector, sendMessage(Matcher<ipc::Message&&>(_)))
            .WillOnce(Invoke([&](ipc::Message&& m) {
                UNIT_ASSERT(m.has_stereo_pair_message());
                UNIT_ASSERT(m.stereo_pair_message().has_speak_not_ready_notification_request());
                UNIT_ASSERT_VALUES_EQUAL(
                    (int)m.stereo_pair_message().speak_not_ready_notification_request().reason(),
                    (int)proto::StereoPair::SpeakNotReadyNotificationReqest::PLAYER_NOT_READY);
                return true;
            }));

        stereoPairProvider.speakNotReadyNotification(IStereoPairProvider::NotReadyReason::PLAYER_NOT_READY);
    }

    Y_UNIT_TEST(testOverrideChannel)
    {
        setupDefaultExpectCalls();
        StereoPairProvider stereoPairProvider("MY_DEVICE_ID", mockConnector);

        EXPECT_CALL(*mockConnector, sendMessage(Matcher<ipc::Message&&>(_)))
            .WillOnce(Invoke([&](ipc::Message&& m) {
                UNIT_ASSERT(m.has_stereo_pair_message());
                UNIT_ASSERT(m.stereo_pair_message().has_override_channel_request());
                UNIT_ASSERT_VALUES_EQUAL(m.stereo_pair_message().override_channel_request().channel(), "undefined");
                return true;
            }))
            .WillOnce(Invoke([&](ipc::Message&& m) {
                UNIT_ASSERT(m.has_stereo_pair_message());
                UNIT_ASSERT(m.stereo_pair_message().has_override_channel_request());
                UNIT_ASSERT_VALUES_EQUAL(m.stereo_pair_message().override_channel_request().channel(), "all");
                return true;
            }))
            .WillOnce(Invoke([&](ipc::Message&& m) {
                UNIT_ASSERT(m.has_stereo_pair_message());
                UNIT_ASSERT(m.stereo_pair_message().has_override_channel_request());
                UNIT_ASSERT_VALUES_EQUAL(m.stereo_pair_message().override_channel_request().channel(), "right");
                return true;
            }))
            .WillOnce(Invoke([&](ipc::Message&& m) {
                UNIT_ASSERT(m.has_stereo_pair_message());
                UNIT_ASSERT(m.stereo_pair_message().has_override_channel_request());
                UNIT_ASSERT_VALUES_EQUAL(m.stereo_pair_message().override_channel_request().channel(), "left");
                return true;
            }));

        stereoPairProvider.overrideChannel(StereoPairState::Channel::UNDEFINED);
        stereoPairProvider.overrideChannel(StereoPairState::Channel::ALL);
        stereoPairProvider.overrideChannel(StereoPairState::Channel::RIGHT);
        stereoPairProvider.overrideChannel(StereoPairState::Channel::LEFT);
    }

    Y_UNIT_TEST(testInitialPairingSignal)
    {
        setupDefaultExpectCalls();
        StereoPairProvider stereoPairProvider("DEVICE_ID", mockConnector);
        UNIT_ASSERT(mockMessageHandler);

        std::chrono::milliseconds elapsed{0};
        stereoPairProvider.initialPairingSignal().connect(
            [&](std::chrono::milliseconds e, const std::shared_ptr<const StereoPairState>& /*state*/) {
                elapsed = e;
            }, Lifetime::immortal);

        {
            auto message = ipc::buildMessage([](auto& msg) {
                auto& s = *msg.mutable_stereo_pair_message()->mutable_state();
                s.set_role(proto::StereoPair::LEADER);
                s.set_connectivity(proto::StereoPair::ONEWAY);
            });
            mockMessageHandler(message);
        }
        UNIT_ASSERT(elapsed == std::chrono::milliseconds{});

        {
            auto message = ipc::buildMessage([](auto& msg) {
                auto& s = *msg.mutable_stereo_pair_message()->mutable_state();
                s.set_role(proto::StereoPair::LEADER);
                s.set_connectivity(proto::StereoPair::ONEWAY);
                s.set_initial_pairing_time_ms(1);
            });
            mockMessageHandler(message);
        }
        UNIT_ASSERT(elapsed != std::chrono::milliseconds{});
    }

    Y_UNIT_TEST(testUserEventRequest)
    {
        setupDefaultExpectCalls();
        StereoPairProvider stereoPairProvider("MY_DEVICE_ID", mockConnector);

        EXPECT_CALL(*mockConnector, sendMessage(Matcher<ipc::Message&&>(_)))
            .WillOnce(Invoke([&](ipc::Message&& m) {
                UNIT_ASSERT(m.has_stereo_pair_message());
                UNIT_ASSERT(m.stereo_pair_message().has_user_event_request());
                UNIT_ASSERT_VALUES_EQUAL(m.stereo_pair_message().user_event_request().event_id(), "CustomUserEvent");
                UNIT_ASSERT_VALUES_EQUAL(m.stereo_pair_message().user_event_request().payload(), "{}");
                return true;
            }));
        stereoPairProvider.userEvent("CustomUserEvent", Json::Value{});

        Json::Value payload;
        EXPECT_CALL(*mockConnector, sendMessage(Matcher<ipc::Message&&>(_)))
            .WillOnce(Invoke([&](ipc::Message&& m) {
                UNIT_ASSERT(m.has_stereo_pair_message());
                UNIT_ASSERT(m.stereo_pair_message().has_user_event_request());
                UNIT_ASSERT_VALUES_EQUAL(m.stereo_pair_message().user_event_request().event_id(), "CustomUserEvent");
                UNIT_ASSERT_VALUES_EQUAL(m.stereo_pair_message().user_event_request().payload(), "{\"flag\":\"FLAG\"}");
                return true;
            }));
        payload["flag"] = "FLAG";
        stereoPairProvider.userEvent("CustomUserEvent", payload);

    }

    Y_UNIT_TEST(testUSerEventSignal)
    {
        setupDefaultExpectCalls();
        StereoPairProvider stereoPairProvider("DEVICE_ID", mockConnector);
        UNIT_ASSERT(mockMessageHandler);

        std::atomic<bool> signalReceived{false};
        stereoPairProvider.userEventSignal().connect(
            [&](const std::string& eventId, const Json::Value& payload) {
                UNIT_ASSERT(!signalReceived.load());
                UNIT_ASSERT_VALUES_EQUAL(eventId, "CustomUserEvent");
                UNIT_ASSERT(payload.isMember("flag"));
                UNIT_ASSERT_VALUES_EQUAL(payload["flag"].asString(), "FLAG");
                signalReceived = true;
            }, Lifetime::immortal);

        {
            auto message = ipc::buildMessage([](auto& msg) {
                auto& ues = *msg.mutable_stereo_pair_message()->mutable_user_event_signal();
                ues.set_event_id("CustomUserEvent");
                ues.set_payload("{\"flag\":\"FLAG\"}");
            });
            mockMessageHandler(message);
        }
        UNIT_ASSERT_VALUES_EQUAL(signalReceived.load(), true);
    }
}

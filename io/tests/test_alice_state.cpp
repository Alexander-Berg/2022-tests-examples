#include <yandex_io/services/aliced/speechkit_endpoint.h>
#include <yandex_io/services/aliced/tests/testlib/ske_test_base_fixture.h>

#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/tests/testlib/test_utils.h>

#include <yandex_io/libs/protobuf_utils/debug.h>

#include <json/json.h>

#include <functional>
#include <mutex>
#include <string>

using namespace quasar;
using namespace quasar::TestUtils;

namespace {

    class SKETestAliceStateFixture: public SKETestBaseFixture {
    public:
        using Base = SKETestBaseFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            setInterfacedMessageHandler([this](const auto& msg, auto& /*connection*/) {
                if (!msg->has_alice_state()) {
                    return;
                }

                std::lock_guard lock(statesMutex_);
                if (!mustntReceiveNewStates_) {
                    aliceStates_.push_back(msg->alice_state());
                    statesCV_.notify_one();
                }
            });

            Base::SetUp(context);
            initialize({});
        }

        void checkState(std::deque<std::function<void(const proto::AliceState&)>> checks) {
            for (const auto& check : checks) {
                std::unique_lock lock{statesMutex_};
                statesCV_.wait(lock, [this] {
                    return !aliceStates_.empty();
                });
                check(aliceStates_.front());
                aliceStates_.pop_front();
            }
        }

        void prohibitNewStates() {
            std::lock_guard guard{statesMutex_};
            mustntReceiveNewStates_ = true;
        }

        void allowNewStates() {
            std::lock_guard guard{statesMutex_};
            mustntReceiveNewStates_ = false;
        }

    private:
        std::deque<proto::AliceState> aliceStates_;
        std::mutex statesMutex_;
        SteadyConditionVariable statesCV_;
        bool mustntReceiveNewStates_{false};
    };

} // namespace

Y_UNIT_TEST_SUITE_F(AliceStateTest, SKETestAliceStateFixture) {
    Y_UNIT_TEST(testAliceStateCommonWorkflow) {
        UNIT_ASSERT(testVoiceDialog->waitForState(State::IDLE));
        startEndpoint();
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));

        testVoiceDialog->waitForYandexUid();
        checkState({[](const proto::AliceState& state) {
            UNIT_ASSERT_EQUAL(state.state(), proto::AliceState::IDLE);
        }});

        ioSDK->toggleConversation();
        UNIT_ASSERT(testVoiceDialog->waitForState(State::LISTENING));
        checkState({
            [](const proto::AliceState& state) {
                // Listening request is started
                UNIT_ASSERT_EQUAL(state.state(), proto::AliceState::LISTENING);
            },
        });

        ioSDK->toggleConversation();
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));
        checkState({[](const proto::AliceState& state) {
            // Spotter is started
            UNIT_ASSERT_EQUAL(state.state(), proto::AliceState::IDLE);
        }});

        ioSDK->startConversation();
        UNIT_ASSERT(testVoiceDialog->waitForState(State::LISTENING));
        checkState({[](const proto::AliceState& state) {
            UNIT_ASSERT_EQUAL(state.state(), proto::AliceState::LISTENING);
        }});

        testVoiceDialog->Say("привет");
        checkState({[](const proto::AliceState& state) {
                        // Got recognition result
                        UNIT_ASSERT_EQUAL(state.state(), proto::AliceState::LISTENING);
                        UNIT_ASSERT(state.has_recognized_phrase());
                        UNIT_ASSERT_EQUAL(state.recognized_phrase(), "привет");
                    },
                    [](const proto::AliceState& state) {
                        // Vins request is started
                        UNIT_ASSERT_EQUAL(state.state(), proto::AliceState::BUSY);
                    },
                    [](const proto::AliceState& state) {
                        // Got vins response
                        UNIT_ASSERT_EQUAL(state.state(), proto::AliceState::BUSY);
                        UNIT_ASSERT(state.has_vins_response());
                    }});
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPEAKING_WITH_SPOTTER));
        checkState({[](const proto::AliceState& state) {
            // TTS is started
            UNIT_ASSERT_EQUAL(state.state(), proto::AliceState::SPEAKING);
        }});

        ioSDK->stopConversation();
        checkState({[](const proto::AliceState& state) {
            UNIT_ASSERT_EQUAL(state.state(), proto::AliceState::IDLE);
        }});
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));

        ////////////////////////
        ioSDK->startConversation();
        UNIT_ASSERT(testVoiceDialog->waitForState(State::LISTENING));
        checkState({[](const proto::AliceState& state) {
            UNIT_ASSERT_EQUAL(state.state(), proto::AliceState::LISTENING);
        }});

        testVoiceDialog->Say("что играет");
        checkState({[](const proto::AliceState& state) {
                        // Got recognition result
                        UNIT_ASSERT_EQUAL(state.state(), proto::AliceState::LISTENING);
                        UNIT_ASSERT(state.has_recognized_phrase());
                        UNIT_ASSERT_EQUAL(state.recognized_phrase(), "что играет");
                    },
                    [](const proto::AliceState& state) {
                        // Vins request is started
                        UNIT_ASSERT_EQUAL(state.state(), proto::AliceState::BUSY);
                    },
                    [](const proto::AliceState& state) {
                        // Got vins response
                        UNIT_ASSERT_EQUAL(state.state(), proto::AliceState::BUSY);
                        UNIT_ASSERT(state.has_vins_response());
                    }});

        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPEAKING_WITH_SPOTTER));
        checkState({[](const proto::AliceState& state) {
            // TTS is started
            UNIT_ASSERT_EQUAL(state.state(), proto::AliceState::SPEAKING);
        }});

        testVoiceDialog->getSpeech();
        UNIT_ASSERT(testVoiceDialog->waitForState(State::LISTENING));
        checkState({[](const proto::AliceState& state) {
                        UNIT_ASSERT_EQUAL(state.state(), proto::AliceState::SHAZAM);
                    },
                    [](const proto::AliceState& state) {
                        UNIT_ASSERT_EQUAL(state.state(), proto::AliceState::LISTENING);
                    }});

        ioSDK->stopConversation();
        checkState({[](const proto::AliceState& state) {
            UNIT_ASSERT_EQUAL(state.state(), proto::AliceState::IDLE);
        }});
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));
    }

    Y_UNIT_TEST(testAliceStateDisableBySDK_dialogStopsWhenProhibiting) {
        UNIT_ASSERT(testVoiceDialog->waitForState(State::IDLE));
        startEndpoint();
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));

        testVoiceDialog->waitForYandexUid();
        checkState({[](const proto::AliceState& state) {
            UNIT_ASSERT_EQUAL(state.state(), proto::AliceState::IDLE);
        }});

        ioSDK->toggleConversation();
        // Alice not blocked, must start LISTENING
        UNIT_ASSERT(testVoiceDialog->waitForState(State::LISTENING));
        checkState({[](const proto::AliceState& state) {
            UNIT_ASSERT_EQUAL(state.state(), proto::AliceState::LISTENING);
        }});

        ioSDK->blockVoiceAssistant("source1", std::nullopt);
        // Must immediately stop listening
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));
        checkState({[](const proto::AliceState& state) {
            UNIT_ASSERT_EQUAL(state.state(), proto::AliceState::IDLE);
        }});
    }

    Y_UNIT_TEST(testAliceStateDisableBySDK_dialogDoesntStartWhenProhibited) {
        UNIT_ASSERT(testVoiceDialog->waitForState(State::IDLE));
        startEndpoint();
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));

        testVoiceDialog->waitForYandexUid();
        checkState({[](const proto::AliceState& state) {
            UNIT_ASSERT_EQUAL(state.state(), proto::AliceState::IDLE);
        }});

        ioSDK->blockVoiceAssistant("source1", std::nullopt);
        prohibitNewStates();
        ioSDK->startConversation();
        ioSDK->blockVoiceAssistant("source2", std::nullopt);
        ioSDK->startConversation();
        ioSDK->unblockVoiceAssistant("source1");
        ioSDK->startConversation();
    }

    Y_UNIT_TEST(testExternalAliceState) {
        startEndpoint();

        auto alicedConnector = createIpcConnectorForTests("aliced");
        alicedConnector->connectToService();
        alicedConnector->waitUntilConnected();

        YIO_LOG_INFO("Start checking...");
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));

        checkState({[](const proto::AliceState& state) {
            UNIT_ASSERT_EQUAL(state.state(), proto::AliceState::IDLE);
        }});

        auto sendExternalAliceState = [&](proto::AliceState state) {
            proto::QuasarMessage message;
            message.mutable_external_alice_state()->CopyFrom(state);
            alicedConnector->sendMessage(std::move(message));
        };

        YIO_LOG_INFO("Send SPEAKING");
        proto::AliceState state;
        state.set_state(proto::AliceState::SPEAKING);
        sendExternalAliceState(state);
        YIO_LOG_INFO("Wait SPEAKING");
        checkState({[](const proto::AliceState& state) {
            UNIT_ASSERT_EQUAL(state.state(), proto::AliceState::SPEAKING);
        }});

        YIO_LOG_INFO("Send LISTENING");
        state.set_state(proto::AliceState::LISTENING);
        sendExternalAliceState(state);
        YIO_LOG_INFO("Wait LISTENING");
        checkState({[](const proto::AliceState& state) {
            UNIT_ASSERT_EQUAL(state.state(), proto::AliceState::LISTENING);
        }});

        YIO_LOG_INFO("Send BUSY");
        state.set_state(proto::AliceState::BUSY);
        sendExternalAliceState(state);
        YIO_LOG_INFO("Wait BUSY");
        checkState({[](const proto::AliceState& state) {
            UNIT_ASSERT_EQUAL(state.state(), proto::AliceState::BUSY);
        }});

        YIO_LOG_INFO("Send IDLE");
        state.set_state(proto::AliceState::IDLE);
        sendExternalAliceState(state);
        YIO_LOG_INFO("Wait IDLE");
        checkState({[](const proto::AliceState& state) {
            UNIT_ASSERT_EQUAL(state.state(), proto::AliceState::IDLE);
        }});
    }
}

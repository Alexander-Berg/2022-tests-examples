#include <yandex_io/libs/gogol/gogol_session.h>

#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

#include <future>

using namespace quasar::gogol;

namespace {

    // common variables for different tests and matchers
    constexpr auto DURATION = std::chrono::seconds(157);

    // gmocks
    class GogolMetricsSenderMock: public IGogolMetricsSender {
    public:
        MOCK_METHOD(void, sendMetric, (Json::Value), (override));
        MOCK_METHOD(void, setQueueSize, (std::optional<int>), (override));
    };

    std::optional<std::string> verifyDefaultMembers(const Json::Value& json) {
        if (!json.isMember("eventName")) {
            return "Invalid eventName";
        }
        if (!json.isMember("eventType")) {
            return "Invalid eventType";
        }
        if (!json.isMember("timestamp")) {
            return "Invalid timestamp";
        }
        const auto ts = std::chrono::milliseconds(json["timestamp"].asInt64());
        const auto ts5SecBefore = std::chrono::system_clock::now() - std::chrono::seconds(5);
        const auto ts5SecBeforeInMs = std::chrono::duration_cast<std::chrono::milliseconds>(ts5SecBefore.time_since_epoch());
        if (ts5SecBeforeInMs > ts) {
            return "Invalid timestamp: (now - 5sec) > timestamp";
        }
        const auto diff = ts - ts5SecBeforeInMs;
        if (diff > std::chrono::seconds(5)) {
            return "Invalid timestamp: (diff) > 5sec";
        }
        return std::nullopt;
    }

    std::optional<std::string> verifyStalledCommon(const Json::Value& json, int count, std::chrono::seconds stalledDuration, std::chrono::seconds fullStalledTime) {
        // ensure stalled count increases
        if (json["stalledCount"].asInt() != count) {
            return "Invalid stalled count";
        }
        // ensure full stalled timespent include all stalls
        const auto stalledTime = std::chrono::seconds(json["stalledTime"].asInt());
        if (stalledTime < fullStalledTime) {
            return "Invalid stalled time (stalledTimeSpent)";
        }

        // stalledId should always be == stalledId - 1
        if (json["data"]["stalledId"].asInt() != (count - 1)) {
            return "Invalid stalled id";
        }

        // ensure current stall duration is correct
        const auto duration = std::chrono::seconds(json["data"]["stalledDuration"].asInt());
        if (duration < stalledDuration) {
            return "Invalid current stalled duration";
        }
        if (json["labels"]["reason"].asString() != "Recover") {
            return "Invalid reason";
        }
        return std::nullopt;
    }

    MATCHER(SuppressPlayerAlive, "description") {
        const Json::Value& json = arg;
        return json["eventName"] == "PlayerAlive";
    }

    MATCHER(VerifyCreatePlayer, "description") {
        const Json::Value& json = arg;
        if (const auto error = verifyDefaultMembers(json)) {
            *result_listener << "Invalid default members: " << *error;
            return false;
        }

        if (json["eventName"] != "CreatePlayer") {
            return false;
        }

        return true;
    }

    MATCHER_P(VerifyDestroyPlayer, reason, "description") {
        const Json::Value& json = arg;
        if (const auto error = verifyDefaultMembers(json)) {
            *result_listener << "Invalid default members: " << *error;
            return false;
        }

        if (json["eventName"] != "DestroyPlayer") {
            return false;
        }

        if (json["data"]["reason"].asString() != reason) {
            *result_listener << "Invalid reason: " << json["data"]["reason"].asString();
            return false;
        }

        return true;
    }

    MATCHER_P3(VerifyStalled, count, stalledDuration, fullStalledTime, "description") {
        const Json::Value& json = arg;
        if (const auto error = verifyDefaultMembers(json)) {
            *result_listener << "Invalid default members: " << *error;
            return false;
        }

        if (json["eventName"] != "Stalled") {
            return false;
        }
        if (json["stalledCount"].asInt() != count) {
            return false;
        }

        if (const auto error = verifyStalledCommon(json, count, stalledDuration, fullStalledTime)) {
            *result_listener << "Invalid stalled common members: " << *error;
            return false;
        }

        // verify duration string label
        if (json["labels"]["stalledDuration"].asString() != std::to_string(stalledDuration.count())) {
            return false;
        }

        return true;
    }

    MATCHER_P3(VerifyStalledEnd, count, stalledDuration, fullStalledTime, "description") {
        const Json::Value& json = arg;
        if (const auto error = verifyDefaultMembers(json)) {
            *result_listener << "Invalid default members: " << *error;
            return false;
        }

        if (json["eventName"] != "StalledEnd") {
            return false;
        }

        if (const auto error = verifyStalledCommon(json, count, stalledDuration, fullStalledTime)) {
            *result_listener << "Invalid stalled common members: " << *error;
            return false;
        }

        return true;
    }

    MATCHER(VerifyStart, "description") {
        const Json::Value& json = arg;
        if (const auto error = verifyDefaultMembers(json)) {
            *result_listener << "Invalid default members: " << *error;
            return false;
        }

        if (json["eventName"] != "Start") {
            *result_listener << "Invalid eventName";
            return false;
        }

        if (json["data"]["duration"].asInt() != DURATION.count()) {
            *result_listener << "Invalid duration";
            return false;
        }
        return true;
    }

    MATCHER_P(VerifyNSecWatched, n, "description") {
        const Json::Value& json = arg;
        if (const auto error = verifyDefaultMembers(json)) {
            *result_listener << "Invalid default members: " << *error;
            return false;
        }

        if (json["eventName"] != (std::to_string(n) + "SecWatched")) {
            *result_listener << "Invalid eventName";
            return false;
        }

        if (json["data"]["duration"].asInt() != DURATION.count()) {
            *result_listener << "Invalid duration";
            return false;
        }
        return true;
    }

    MATCHER_P(Verify30SecHeartbeat, watchedSec, "description") {
        const Json::Value& json = arg;
        if (const auto error = verifyDefaultMembers(json)) {
            *result_listener << "Invalid default members: " << *error;
            return false;
        }

        if (json["eventName"] != "30SecHeartbeat") {
            *result_listener << "Invalid eventName";
            return false;
        }
        const auto passedWatchedSec = json["data"]["watchedSec"].asInt();
        if (passedWatchedSec != watchedSec) {
            *result_listener << "Invalid [\"data\"][\"watchedSec\"]. Passed: " << passedWatchedSec << ". Expected: " << watchedSec;
            return false;
        }
        return true;
    }

    MATCHER_P2(VerifyCustomError, errorName, errorMessage, "description") {
        const Json::Value& json = arg;
        if (const auto error = verifyDefaultMembers(json)) {
            *result_listener << "Invalid default members: " << *error;
            return false;
        }

        if (json["eventType"].asString() != "error") {
            *result_listener << "Invalid event type";
            return false;
        }

        if (json["eventName"].asString() != errorName) {
            *result_listener << "Invalid eventName";
            return false;
        }
        if (!json["data"].isMember("isFatal") || !json["data"]["isFatal"].isBool()) {
            *result_listener << "isFatal bool value is required";
            return false;
        }
        if (json["data"]["message"].asString() != errorMessage) {
            *result_listener << "Invalid error message";
            return false;
        }
        return true;
    }

} // namespace

Y_UNIT_TEST_SUITE_F(GogolSessionTest, QuasarUnitTestFixtureWithoutIpc) {
    Y_UNIT_TEST(CreateDestroyPlayer) {
        std::promise<void> expectation;

        auto waitExpectation = [&]() {
            expectation.get_future().get();
            expectation = std::promise<void>();
        };

        const auto sender = std::make_shared<GogolMetricsSenderMock>();
        GogolSession gogol(sender);

        // suppress PlayerAlive for this test.
        EXPECT_CALL(*sender, sendMetric(SuppressPlayerAlive())).Times(testing::AnyNumber());

        EXPECT_CALL(*sender, sendMetric(VerifyCreatePlayer())).WillOnce(testing::Invoke([&]() {
            expectation.set_value();
        }));
        gogol.createPlayer();

        waitExpectation();

        EXPECT_CALL(*sender, sendMetric(VerifyDestroyPlayer("reason"))).WillOnce(testing::Invoke([&]() {
            expectation.set_value();
        }));
        gogol.destroyPlayer("reason");
        waitExpectation();
    }

    Y_UNIT_TEST(Stalled) {
        std::promise<void> expectation;

        auto waitExpectation = [&]() {
            expectation.get_future().get();
            expectation = std::promise<void>();
        };

        const auto sender = std::make_shared<GogolMetricsSenderMock>();
        GogolSession gogol(sender);

        // suppress PlayerAlive for this test.
        EXPECT_CALL(*sender, sendMetric(SuppressPlayerAlive())).Times(testing::AnyNumber());

        {
            testing::InSequence s;
            // stalled session 1
            EXPECT_CALL(*sender, sendMetric(VerifyStalled(1, std::chrono::seconds(0), std::chrono::seconds(0))));    // stalled0
            EXPECT_CALL(*sender, sendMetric(VerifyStalled(1, std::chrono::seconds(1), std::chrono::seconds(1))));    // stalled1
            EXPECT_CALL(*sender, sendMetric(VerifyStalledEnd(1, std::chrono::seconds(1), std::chrono::seconds(1)))); // stalledEnd
            // stalled session 2
            EXPECT_CALL(*sender, sendMetric(VerifyStalled(2, std::chrono::seconds(0), std::chrono::seconds(1)))); // stalled0
            EXPECT_CALL(*sender, sendMetric(VerifyStalled(2, std::chrono::seconds(1), std::chrono::seconds(2)))); // stalled1
            EXPECT_CALL(*sender, sendMetric(VerifyStalledEnd(2, std::chrono::seconds(1), std::chrono::seconds(2)))).WillOnce(testing::Invoke([&]() {
                expectation.set_value();
            }));
        }

        // stalled session 1
        gogol.handleStalled();
        std::this_thread::sleep_for(std::chrono::milliseconds(1100));
        gogol.handleStalled();
        gogol.handleStalledEnd();

        // stalled session 2
        // second session will include StalledTimeSpent from session1, so fullStalledTimespent should be 2 sec (approximately)
        gogol.handleStalled();
        std::this_thread::sleep_for(std::chrono::milliseconds(1100));
        gogol.handleStalled();
        gogol.handleStalledEnd();

        waitExpectation();
    }

    Y_UNIT_TEST(NSecondsWatched) {
        std::promise<void> expectation;

        auto waitExpectation = [&]() {
            expectation.get_future().get();
            expectation = std::promise<void>();
        };

        const auto sender = std::make_shared<GogolMetricsSenderMock>();
        GogolSession gogol(sender);

        // suppress PlayerAlive for this test.
        EXPECT_CALL(*sender, sendMetric(SuppressPlayerAlive())).Times(testing::AnyNumber());

        {
            testing::InSequence s;
            // start should be called after fisrt progess call
            EXPECT_CALL(*sender, sendMetric(VerifyStart())).WillOnce(testing::Invoke([&]() {
                expectation.set_value();
            }));

            // 4SecWatched
            EXPECT_CALL(*sender, sendMetric(VerifyNSecWatched(4))).WillOnce(testing::Invoke([&]() {
                expectation.set_value();
            }));

            // 10SecWatched
            EXPECT_CALL(*sender, sendMetric(VerifyNSecWatched(10))).WillOnce(testing::Invoke([&]() {
                expectation.set_value();
            }));

            // 30SecHeartbeat
            EXPECT_CALL(*sender, sendMetric(Verify30SecHeartbeat(30))).WillOnce(testing::Invoke([&]() {
                expectation.set_value();
            }));
            // 30SecHeartbeat
            EXPECT_CALL(*sender, sendMetric(Verify30SecHeartbeat(60))).WillOnce(testing::Invoke([&]() {
                expectation.set_value();
            }));
        }

        gogol.handleProgress(std::chrono::seconds(0), DURATION);
        waitExpectation(); // wait Start

        // nothing to be called
        gogol.handleProgress(std::chrono::seconds(1), DURATION);
        gogol.handleProgress(std::chrono::seconds(2), DURATION);
        gogol.handleProgress(std::chrono::seconds(3), DURATION);
        gogol.handleProgress(std::chrono::milliseconds(3999), DURATION);

        // ensure that gogol will send 4SecWatched metrics when progress will change from 3999ms to 4000ms
        // but it should not be called again when 4001ms progress will be passed
        gogol.handleProgress(std::chrono::seconds(4), DURATION); // 4SecWatched
        waitExpectation();

        gogol.handleProgress(std::chrono::milliseconds(4001), DURATION); // nothing to be called

        // emulate progess
        gogol.handleProgress(std::chrono::seconds(5), DURATION); // nothing to be called
        gogol.handleProgress(std::chrono::seconds(8), DURATION); // nothing to be called

        gogol.handleProgress(std::chrono::seconds(10), DURATION); // 10SecWatched
        waitExpectation();

        // 30SecHeartbeat
        gogol.handleProgress(std::chrono::seconds(30), DURATION); // 30SecHeartbeat
        waitExpectation();

        gogol.handleProgress(std::chrono::seconds(45), DURATION); // nothing to be called

        // test one more heartbeat
        gogol.handleProgress(std::chrono::seconds(60), DURATION); // 30SecHeartbeat
        waitExpectation();
    }

    Y_UNIT_TEST(CustomError) {
        std::promise<void> expectation;
        auto waitExpectation = [&]() {
            expectation.get_future().get();
            expectation = std::promise<void>();
        };

        const auto sender = std::make_shared<GogolMetricsSenderMock>();
        GogolSession gogol(sender);

        // suppress PlayerAlive for this test.
        EXPECT_CALL(*sender, sendMetric(SuppressPlayerAlive())).Times(testing::AnyNumber());

        EXPECT_CALL(*sender, sendMetric(VerifyCustomError("gstreamer.SomeError", "error message 1"))).WillOnce(testing::Invoke([&]() {
            expectation.set_value();
        }));
        gogol.handleError("gstreamer.SomeError", "error message 1");
        waitExpectation();

        EXPECT_CALL(*sender, sendMetric(VerifyCustomError("gstreamer.OtherError", "error message 2"))).WillOnce(testing::Invoke([&]() {
            expectation.set_value();
        }));
        gogol.handleError("gstreamer.OtherError", "error message 2");
        waitExpectation();
    }
}

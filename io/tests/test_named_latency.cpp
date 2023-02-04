#include <yandex_io/libs/telemetry/named_latency.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/logging/logging.h>

#include <yandex_io/tests/testlib/unittest_helper/telemetry_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

namespace {
    class MainFixture: public TelemetryTestFixture {
        using Base = TelemetryTestFixture;

    public:
        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);
            reporter = std::make_unique<YandexIO::NamedLatency>(getDeviceForTests()->telemetry());

            setEventListener([this](const std::string& eventName, const std::string& eventValue, YandexIO::ITelemetry::Flags /*flags*/) {
                {
                    std::scoped_lock lock(mutex_);
                    events_.emplace_back(eventName, quasar::tryParseJsonOrEmpty(eventValue));
                }
                cond_.notify_all();
            });
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            Base::TearDown(context);
        }

        std::tuple<std::string, Json::Value> waitForEvent() {
            std::unique_lock lock(mutex_);
            cond_.wait(lock, [this]() {
                return !events_.empty();
            });

            auto result = events_.front();
            events_.pop_front();
            return result;
        }

        Json::Value waitForEvent(const std::string& name) {
            while (true) {
                auto [eventName, eventValue] = waitForEvent();
                if (eventName == name) {
                    return eventValue;
                }
            }
            return Json::Value();
        }

        std::unique_ptr<YandexIO::NamedLatency> reporter;

    private:
        std::mutex mutex_;
        std::condition_variable cond_;
        std::list<std::tuple<std::string, Json::Value>> events_;
    };

} // namespace

Y_UNIT_TEST_SUITE(NamedLatencyTest) {
    Y_UNIT_TEST_F(BasicUsage, MainFixture) {
        reporter->createLatencyPoint("test1");
        std::this_thread::sleep_for(std::chrono::seconds(1));
        reporter->reportLatency("test1", "testEvent1", false, std::string("{\"t\":1}"));
        {
            auto eventValue = waitForEvent("testEvent1");
            UNIT_ASSERT_EQUAL(eventValue["t"].asInt(), 1);
            UNIT_ASSERT(eventValue["value"].asInt() >= 1000); // milliseconds
        }
        reporter->createLatencyPoint("test2");
        std::this_thread::sleep_for(std::chrono::seconds(1));
        reporter->reportLatency("test2", "testEvent2", true);
        {
            auto eventValue = waitForEvent("testEvent2");
            UNIT_ASSERT(eventValue["value"].asInt() >= 1000);
        }
        reporter->reportLatency("test1", "testEvent1", true);
        auto eventValue = waitForEvent("testEvent1");
        UNIT_ASSERT(eventValue["value"].asInt() >= 2000);
    }
}

#include <fstream>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <maps/libs/json/include/value.h>

#include <maps/infra/roquefort/lib/stat_counter.h>
#include <maps/infra/roquefort/lib/profile.h>

using namespace std::chrono_literals;
using namespace maps::roquefort;

class LogSourceMock : public LogSource {
public:
    MOCK_METHOD(std::optional<LogDose>, dose, (), (override));
};

class StatCounterFixture : public NUnitTest::TBaseFixture {
public:
    void SetUp(NUnitTest::TTestContext&) override {
        logSourceMock_ = std::make_shared<LogSourceMock>();
        statCounter_ = std::make_shared<StatCounter>(logSourceMock_, buildMetrics());
        std::ifstream testLog("./files/test_stat_counter.log");
        std::getline(testLog, tskvLine_);
    }

    std::shared_ptr<LogSourceMock> logSourceMock() {
        return logSourceMock_;
    }

    std::shared_ptr<StatCounter> statCounter() {
        return statCounter_;
    }

    LogDose makeLogDose(size_t lines) const {
        std::stringstream page;
        for (size_t i = 0; i < lines; ++i) {
            page << tskvLine_ << std::endl;
        }
        return LogDose{page.str(), UPDATE_TIMEOUT};
    }

protected:
    static constexpr auto UPDATE_TIMEOUT = 1000ms;
    static constexpr auto EXPECTED_PULL_TIMEOUT = 5000ms;

private:
    static std::vector<std::unique_ptr<MetricGroup>> buildMetrics() {
        auto service = ServiceConfig("./files/test-counter.conf");
        auto profile = createProfile("counter");
        return { profile->createMetrics({service}) };
    }

    std::shared_ptr<LogSourceMock> logSourceMock_;
    std::shared_ptr<StatCounter> statCounter_;
    std::string tskvLine_;
};

Y_UNIT_TEST_SUITE_F(StatCounterTestSuite, StatCounterFixture) {
    Y_UNIT_TEST(DontCrashOnEmptyCounters) {
        statCounter()->run();
        Y_UNUSED(statCounter()->stats());
    }

    Y_UNIT_TEST(DontCrashOnConsequentPulls) {
        statCounter()->run();
        Y_UNUSED(statCounter()->stats());
        Y_UNUSED(statCounter()->stats());
    }

    Y_UNIT_TEST(CounterWindow) {
        EXPECT_CALL(*logSourceMock(), dose())
            .WillOnce([this]() { return std::optional{makeLogDose(1)}; }) // 1
            .WillOnce([this]() { return std::optional{makeLogDose(1)}; }) // 2
            .WillOnce([this]() { return std::optional{makeLogDose(1)}; }) // 3
            .WillOnce([this]() { return std::optional{makeLogDose(1)}; }) // 4
            .WillOnce([this]() { return std::optional{makeLogDose(1)}; }) // 5
            .WillOnce([this]() { return std::optional{makeLogDose(11)}; });
        statCounter()->run();

        std::this_thread::sleep_for(UPDATE_TIMEOUT / 5);
        auto firstStats = maps::json::Value::fromString(statCounter()->stats());
        UNIT_ASSERT(firstStats.isArray());
        UNIT_ASSERT_VALUES_EQUAL(firstStats.size(), 6);
        UNIT_ASSERT_VALUES_EQUAL(firstStats[1][0].as<std::string>(), "line_read_ammv");
        EXPECT_NEAR(firstStats[1][1].as<double>(), 1.0, 0.01);

        std::this_thread::sleep_for(EXPECTED_PULL_TIMEOUT * 1.1);
        auto secondStats = maps::json::Value::fromString(statCounter()->stats());
        UNIT_ASSERT(secondStats.isArray());
        UNIT_ASSERT_VALUES_EQUAL(secondStats.size(), 6);
        UNIT_ASSERT_VALUES_EQUAL(secondStats[1][0].as<std::string>(), "line_read_ammv");
        EXPECT_NEAR(secondStats[1][1].as<double>(), 3.0, 0.01);
    }

    Y_UNIT_TEST(CantKeepUpThenOnlyPerformance) {
        auto blockUpdateThread = [this]() {
            std::this_thread::sleep_for(EXPECTED_PULL_TIMEOUT * 3);
            return std::optional{makeLogDose(0)};
        };
        EXPECT_CALL(*logSourceMock(), dose())
            .WillOnce(blockUpdateThread);
        statCounter()->run();

        std::this_thread::sleep_for(UPDATE_TIMEOUT / 5);
        static constexpr size_t nIterations = 2;
        for (size_t i = 0; i < nIterations; ++i) {
            auto stats = maps::json::Value::fromString(statCounter()->stats());
            UNIT_ASSERT(stats.isArray());
            UNIT_ASSERT_VALUES_EQUAL(stats.size(), 3);
            UNIT_ASSERT_VALUES_EQUAL(stats[0][0].as<std::string>(), "parsing_time_axxv");
            UNIT_ASSERT_VALUES_EQUAL(stats[1][0].as<std::string>(), "line_read_ammv");
            UNIT_ASSERT_VALUES_EQUAL(stats[1][1].as<size_t>(), 0);
            UNIT_ASSERT_VALUES_EQUAL(stats[2][0].as<std::string>(), "parse_errors_ammv");
            UNIT_ASSERT_VALUES_EQUAL(stats[2][1].as<size_t>(), 0);
            std::this_thread::sleep_for(EXPECTED_PULL_TIMEOUT * 1.1);
        }
    }
}

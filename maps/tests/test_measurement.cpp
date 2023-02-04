#include <library/cpp/testing/gtest/gtest.h>

#include <maps/analyzer/libs/travel_time/include/measurement.h>
#include <maps/analyzer/libs/travel_time/include/travel_time.h>

#include <boost/date_time/posix_time/posix_time_types.hpp>

#include <algorithm>
#include <iterator>
#include <limits>
#include <vector>

namespace maps::analyzer::travel_time {

namespace pt = boost::posix_time;

const auto TIME = pt::from_iso_string("20180101T000000");
const auto LEAVE_TIME = pt::from_iso_string("20180101T000001");
const double TRAVEL_TIME = 10.;

TEST(TestTravelTimeMeasurement, TestLessComparator) {
    const TravelTimeMeasurement m1{TIME, TRAVEL_TIME};
    const TravelTimeMeasurement m2{TIME + pt::seconds(1), TRAVEL_TIME};
    const TravelTimeMeasurement m3{TIME, TRAVEL_TIME + 1};
    const TravelTimeMeasurement m4{TIME, TRAVEL_TIME};
    const TravelTimeMeasurement m5{TIME - pt::seconds(1), TRAVEL_TIME};

    EXPECT_TRUE(m1 < m2);
    EXPECT_TRUE(m1 < m3);
    EXPECT_FALSE(m1 < m4);
    EXPECT_FALSE(m1 < m5);
}

TEST(TestMeasurementsCollector, TestSize) {
    MeasurementsCollector c(pt::seconds(2));
    c.addPoint(LEAVE_TIME, TIME, TRAVEL_TIME);
    c.addPoint(LEAVE_TIME, TIME, TRAVEL_TIME);
    c.addPoint(LEAVE_TIME, TIME + pt::seconds(1), TRAVEL_TIME);
    c.addPoint(LEAVE_TIME + pt::seconds(1), TIME, TRAVEL_TIME);
    EXPECT_EQ(c.size(), 4u);
}

TEST(TestMeasurementsCollector, TestAddPoint) {
    MeasurementsCollector c(pt::seconds(2));
    c.addPoint(LEAVE_TIME, TIME, TRAVEL_TIME);
    c.addPoint(LEAVE_TIME, TIME, TRAVEL_TIME);
    c.addPoint(LEAVE_TIME + pt::seconds(1), TIME, UNDEFINED_TRAVEL_TIME);
    EXPECT_EQ(c.size(), 2u);
}

TEST(TestMeasurementsCollector, TestLastSignalTime) {
    MeasurementsCollector c(pt::seconds(2));
    c.addPoint(LEAVE_TIME, TIME, TRAVEL_TIME);
    EXPECT_EQ(c.lastSignalTime(), LEAVE_TIME);
    c.addPoint(LEAVE_TIME + pt::seconds(1), TIME, TRAVEL_TIME);
    EXPECT_EQ(c.lastSignalTime(), LEAVE_TIME + pt::seconds(1));
}

TEST(TestMeasurementsCollector, TestClearOldPoints) {
    MeasurementsCollector first(pt::seconds(2));
    std::vector<pt::ptime> times = {
        TIME,
        TIME + pt::seconds(1),
        TIME + pt::seconds(2),
        TIME + pt::seconds(3),
        TIME + pt::seconds(4),
    };
    for (const auto& t: times) {
        first.addPoint(LEAVE_TIME, t, TRAVEL_TIME);
    }
    first.clearOldPoints(times.front());
    EXPECT_EQ(first.size(), times.size());
    first.clearOldPoints(times[2]);
    EXPECT_EQ(first.size(), times.size());
    first.clearOldPoints(times.back());
    EXPECT_EQ(first.size(), times.size() - 2);

    MeasurementsCollector second(pt::seconds(0));
    for (const auto& t: times) {
        second.addPoint(LEAVE_TIME, t, TRAVEL_TIME);
    }
    second.clearOldPoints(times.front());
    EXPECT_EQ(second.size(), times.size());
    second.clearOldPoints(times[2]);
    EXPECT_EQ(second.size(), times.size() - 2);
    second.clearOldPoints(times.back());
    EXPECT_EQ(second.size(), times.size() - 4);
}

TEST(TestMeasurementsCollector, TestMeasurements) {
    MeasurementsCollector c(pt::seconds(2));
    std::vector<pt::ptime> times = {
        TIME,
        TIME + pt::seconds(1),
        TIME + pt::seconds(2),
        TIME + pt::seconds(3),
        TIME + pt::seconds(4)
    };
    for (auto iter = std::rbegin(times); iter != std::rend(times); ++iter) {
        c.addPoint(LEAVE_TIME, *iter, TRAVEL_TIME);
    }
    const auto measurements = c.measurements();
    EXPECT_TRUE(std::is_sorted(std::begin(measurements), std::end(measurements)));
}

TEST(TestOther, TestGetTravelTimesFromMeasurements) {
    std::vector<TravelTimeMeasurement> measurements = {
        {TIME, TRAVEL_TIME},
        {TIME + pt::seconds(1), TRAVEL_TIME + 1},
        {TIME + pt::seconds(2), TRAVEL_TIME + 2},
        {TIME + pt::seconds(3), TRAVEL_TIME + 3},
        {TIME + pt::seconds(4), TRAVEL_TIME + 4},
    };
}

} // maps::analyzer::travel_time

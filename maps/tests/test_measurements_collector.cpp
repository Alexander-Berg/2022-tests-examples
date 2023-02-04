#include <library/cpp/testing/gtest/gtest.h>

#include <maps/analyzer/libs/travel_time/include/measurement.h>
#include <maps/analyzer/libs/travel_time/include/travel_time.h>

namespace travel_time = maps::analyzer::travel_time;
namespace pt = boost::posix_time;

inline pt::ptime createTime(size_t hours, size_t minutes) {
    return pt::from_iso_string("20170301T000000") + pt::hours(hours) + pt::minutes(minutes);
}

inline void addPoint(
    travel_time::MeasurementsCollector& collector,
    const pt::ptime& time,
    travel_time::TravelTime travelTime
) {
    collector.addPoint(
        time,
        time,
        travelTime
    );
}

void checkMeasurements(
    const std::vector<travel_time::TravelTimeMeasurement>& measurements,
    std::vector<travel_time::TravelTime> etalonTimes
) {
    std::vector<travel_time::TravelTime> times;
    times.reserve(measurements.size());
    for (const auto& m : measurements) {
        times.push_back(m.travelTime);
    }
    EXPECT_EQ(times, etalonTimes);
}

TEST(TestBuildJams, TestMeasurementsCollector) {
    travel_time::MeasurementsCollector collector(pt::hours(4));

    std::vector<std::vector<travel_time::TravelTime>> correctMeasurements = {
        { 10.0, 20.0 },
        { 10.0, 20.0, 30.0, 40.0 },
        { 10.0, 20.0, 30.0, 40.0, 50.0, 60.0 },
        { 10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0 },
        { 30.0, 40.0, 50.0, 60.0, 70.0, 80.0 },
        { 50.0, 60.0, 70.0, 80.0 },
        { 70.0, 80.0 },
        {}
    };

    std::size_t noMoreSignals = 4;
    std::size_t collectUntil = 8;
    for (std::size_t i = 0; i < collectUntil; ++i) {
        if (i < noMoreSignals) {
            addPoint(collector, createTime(i, 20), static_cast<travel_time::TravelTime>(i) * 20.0 + 10.0);
            addPoint(collector, createTime(i, 40), static_cast<travel_time::TravelTime>(i) * 20.0 + 20.0);
        }
        collector.clearOldPoints(createTime(i + 1, 0));
        checkMeasurements(
            collector.measurements(),
            correctMeasurements[i]
        );
    }
}

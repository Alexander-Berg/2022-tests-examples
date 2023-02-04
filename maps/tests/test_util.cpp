#include <library/cpp/testing/gtest/gtest.h>

#include <maps/analyzer/libs/travel_time/include/travel_time.h>
#include <maps/analyzer/libs/travel_time/include/util.h>

#include <algorithm>
#include <iterator>

namespace maps::analyzer::travel_time {

TEST(TestUtils, TestConvertTravelTimeToSpkm) {
    EXPECT_EQ(convertTravelTimeToSpkm(0, 1), 0);
    EXPECT_EQ(convertTravelTimeToSpkm(10, 10), 1000);
    EXPECT_EQ(convertTravelTimeToSpkm(200, 100), 2000);
}

TEST(TestUtils, TestConvertSpkmToTravelTime) {
    EXPECT_EQ(convertSpkmToTravelTime(0, 1), 0);
    EXPECT_EQ(convertSpkmToTravelTime(1000, 10), 10);
    EXPECT_EQ(convertSpkmToTravelTime(2000, 100), 200);
}

TEST(TestUtils, TestGetTravelTimesFromMeasurements) {
    const pt::ptime t(
        boost::gregorian::date(2018, boost::date_time::Apr, 1), pt::hours(2)
    );
    const TravelTime travelTime = 10;
    const std::vector<TravelTimeMeasurement> measurements = {
        {t, travelTime},
        {t + pt::seconds(1), travelTime + 1},
        {t + pt::seconds(2), travelTime + 2},
        {t + pt::seconds(3), travelTime + 3},
        {t + pt::seconds(4), travelTime + 4},
    };
    const std::vector<TravelTime> etalonTravelTimes = {
        travelTime,
        travelTime + 1,
        travelTime + 2,
        travelTime + 3,
        travelTime + 4,
    };
    const auto travelTimes = getTravelTimesFromMeasurements(measurements);
    EXPECT_TRUE(std::equal(
        std::begin(etalonTravelTimes),
        std::end(etalonTravelTimes),
        std::begin(travelTimes)
    ));
}

} // maps::analyzer::travel_time

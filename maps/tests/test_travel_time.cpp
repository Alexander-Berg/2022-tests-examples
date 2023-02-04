#include <library/cpp/testing/gtest/gtest.h>

#include <maps/analyzer/libs/travel_time/include/travel_time.h>

#include <cmath>
#include <limits>

namespace maps::analyzer::travel_time {

TEST(TestTravelTime, TestIsValid) {
    EXPECT_TRUE(isValid(0.0));
    EXPECT_TRUE(isValid(-1e-10));
    EXPECT_FALSE(isValid(-1e-9));
    EXPECT_FALSE(isValid(NAN));
    EXPECT_FALSE(isValid(std::numeric_limits<TravelTime>::quiet_NaN()));
}

} // maps::analyzer::travel_time

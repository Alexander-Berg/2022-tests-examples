#include <maps/automotive/remote_access/libs/time/format.h>
#include <library/cpp/testing/gtest/gtest.h>
#include <util/datetime/constants.h>

namespace ma = maps::automotive;

TEST(test_maps_automotive_time, FormattedTime)
{
    time_t firstJanuaryMidnightUtc = 1420070460; // 2015-01-01 00:01:00
    int32_t moscowTzOffset = 3 * SECONDS_IN_HOUR;
    int32_t newyorkTzOffset = -5 * SECONDS_IN_HOUR;

    EXPECT_EQ(
        ma::getFormattedTime(firstJanuaryMidnightUtc, 0),
        "2015-01-01T00:01:00.000+0000");

    EXPECT_EQ(
        ma::getFormattedTime(firstJanuaryMidnightUtc, moscowTzOffset),
        "2015-01-01T03:01:00.000+0300");

    EXPECT_EQ(
        ma::getFormattedTime(firstJanuaryMidnightUtc, newyorkTzOffset),
        "2014-12-31T19:01:00.000-0500");
}

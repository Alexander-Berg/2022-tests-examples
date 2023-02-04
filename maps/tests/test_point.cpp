#include <library/cpp/testing/gtest/gtest.h>

#include <maps/analyzer/libs/travel_time/include/point.h>
#include <maps/analyzer/libs/travel_time/include/travel_time.h>

#include <boost/date_time/posix_time/posix_time_types.hpp>

namespace maps::analyzer::travel_time {

namespace pt = boost::posix_time;

const auto ENTER_TIME = pt::from_iso_string("20180101T000000");
const auto LEAVE_TIME = pt::from_iso_string("20180101T000001");
const auto TIME_FOR_INTERPOLATION_WINDOW = pt::from_iso_string("20180101T000002");
const auto TIME_FOR_WEIGHT_ORDER = pt::from_iso_string("20180101T000003");
const TravelTime TRAVEL_TIME = 10;

TEST(TestTravelTimePoint, TestByInterpolationWindowTime) {
    const TravelTimePoint p1{
        ENTER_TIME,
        LEAVE_TIME,
        TIME_FOR_INTERPOLATION_WINDOW,
        TIME_FOR_WEIGHT_ORDER,
        TRAVEL_TIME
    };
    const TravelTimePoint p2{
        ENTER_TIME,
        LEAVE_TIME,
        TIME_FOR_INTERPOLATION_WINDOW + pt::seconds(1),
        TIME_FOR_WEIGHT_ORDER,
        TRAVEL_TIME
    };
    const TravelTimePoint p3{
        ENTER_TIME,
        LEAVE_TIME,
        TIME_FOR_INTERPOLATION_WINDOW,
        TIME_FOR_WEIGHT_ORDER,
        TRAVEL_TIME + 1
    };
    const TravelTimePoint p4{
        ENTER_TIME,
        LEAVE_TIME,
        TIME_FOR_INTERPOLATION_WINDOW,
        TIME_FOR_WEIGHT_ORDER,
        TRAVEL_TIME
    };
    const TravelTimePoint p5{
        ENTER_TIME,
        LEAVE_TIME,
        TIME_FOR_INTERPOLATION_WINDOW - pt::seconds(1),
        TIME_FOR_WEIGHT_ORDER,
        TRAVEL_TIME - 1
    };
    EXPECT_TRUE(byInterpolationWindowTime(p1, p2));
    EXPECT_TRUE(byInterpolationWindowTime(p1, p3));
    EXPECT_FALSE(byInterpolationWindowTime(p1, p4));
    EXPECT_FALSE(byInterpolationWindowTime(p1, p5));
}

TEST(TestTravelTimePoint, TestByWeightOrderTime) {
    const TravelTimePoint p1{
        ENTER_TIME,
        LEAVE_TIME,
        TIME_FOR_INTERPOLATION_WINDOW,
        TIME_FOR_WEIGHT_ORDER,
        TRAVEL_TIME
    };
    const TravelTimePoint p2{
        ENTER_TIME,
        LEAVE_TIME,
        TIME_FOR_INTERPOLATION_WINDOW,
        TIME_FOR_WEIGHT_ORDER + pt::seconds(1),
        TRAVEL_TIME
    };
    const TravelTimePoint p3{
        ENTER_TIME,
        LEAVE_TIME,
        TIME_FOR_INTERPOLATION_WINDOW,
        TIME_FOR_WEIGHT_ORDER,
        TRAVEL_TIME + 1
    };
    const TravelTimePoint p4{
        ENTER_TIME,
        LEAVE_TIME,
        TIME_FOR_INTERPOLATION_WINDOW,
        TIME_FOR_WEIGHT_ORDER,
        TRAVEL_TIME
    };
    const TravelTimePoint p5{
        ENTER_TIME,
        LEAVE_TIME,
        TIME_FOR_INTERPOLATION_WINDOW,
        TIME_FOR_WEIGHT_ORDER - pt::seconds(1),
        TRAVEL_TIME - 1
    };
    EXPECT_TRUE(byWeightOrderTime(p1, p2));
    EXPECT_TRUE(byWeightOrderTime(p1, p3));
    EXPECT_FALSE(byWeightOrderTime(p1, p4));
    EXPECT_FALSE(byWeightOrderTime(p1, p5));
}

} // maps::analyzer::travel_time

#include <maps/analyzer/libs/time_interpolator/include/util.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace mti = maps::analyzer::time_interpolator;
namespace pt = boost::posix_time;
namespace greg = boost::gregorian;

template<class Time>
void checkRoundTo(
    const Time& time, const pt::time_duration& step,
    const Time& correctRoundedTime
) {
    const Time roundedTime(mti::roundTo(time, step));
    EXPECT_EQ(roundedTime, correctRoundedTime);
}

void checkDivision(
    const pt::time_duration& divisor, const pt::time_duration& dividend,
    long correctQuotient, const pt::time_duration& correctRemainder
) {
    const long quotient = divisor / dividend;
    const pt::time_duration remainder = divisor % dividend;
    EXPECT_EQ(quotient, correctQuotient);
    EXPECT_EQ(remainder, correctRemainder);
}

TEST(InterpolatorUtilTests, All) {
    greg::date date(2010, 3, 1);

    pt::ptime time(date, pt::time_duration(15, 43, 57));
    pt::time_duration step = pt::minutes(15);
    pt::ptime correctRoundedTime(date, pt::time_duration(15, 30, 0));
    checkRoundTo(time, step, correctRoundedTime);

    step = pt::minutes(1);
    correctRoundedTime = pt::ptime(date, pt::time_duration(15, 43, 0));
    checkRoundTo(time, step, correctRoundedTime);

    pt::time_duration duration(6, 25, 48);

    step = pt::seconds(10);
    pt::time_duration correctRoundedDuration(6, 25, 40);
    checkRoundTo(duration, step, correctRoundedDuration);

    step = pt::minutes(6);
    correctRoundedDuration = pt::time_duration(6, 24, 0);
    checkRoundTo(duration, step, correctRoundedDuration);


    checkDivision(
        pt::time_duration(5, 5, 3), pt::time_duration(2, 0, 0),
        2, pt::time_duration(1, 5, 3)
    );

    checkDivision(
        pt::time_duration(-5, 15, 30), pt::time_duration(1, 0, 0),
        -5, pt::time_duration(0, -15, 30)
    );

    checkDivision(
        pt::time_duration(4, 0, 43), pt::time_duration(-1, 0, 0),
        -4, pt::time_duration(0, 0, 43)
    );

    checkDivision(
        pt::time_duration(-5, 0, 0), pt::time_duration(-1, 0, 1),
        4, pt::time_duration(0, -59, 56)
    );
}

#include <maps/libs/common/include/moving_stat.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <util/random/fast.h>

#include <stdlib.h>
#include <climits>

using namespace std::literals::chrono_literals;

double rand(double min, double max)
{
    static TFastRng32 engine(17, 0);
    return min + engine.GenRandReal1() * (max - min);
}

template <class Rep, class Period>
void advance(
        std::chrono::steady_clock::time_point& now,
        std::chrono::duration<Rep, Period> duration) {
    now += std::chrono::duration_cast<
        std::chrono::steady_clock::duration>(duration);
}

struct Fixture {
    // Fed by random values during tests; may diverge from expected results a little bit.
    maps::MovingAverage<double> ravg;
    maps::MovingQuantile<double> rq95;

    // Fed by predefined series; must match expected results exactly.
    maps::MovingAverage<long> davg;
    maps::MovingQuantile<long> dq95;

    Fixture():
        ravg(1s), rq95(1s, 0.95),
        davg(1s), dq95(1s, 0.95)
    {}

    void push(double d, long l, std::chrono::steady_clock::time_point now)
    {
        ravg.push(d, now);
        rq95.push(d, now);
        davg.push(l, now);
        dq95.push(l, now);
    }
};

TEST(Moving_stat_tests, MovingStatTest)
{
    Fixture f;

    std::chrono::steady_clock::time_point now(0s);

    for (size_t i = 0; i != 10000; ++i)
        f.push(rand(0, 1000), i % 1000, now);

    EXPECT_NEAR(f.ravg.value(now), 500, 10.0);
    EXPECT_NEAR(f.rq95.value(now), 950, 10.0);
    EXPECT_EQ(f.davg.value(now), 499);
    EXPECT_EQ(f.dq95.value(now), 950);

    advance(now, 0.5s);

    for (size_t i = 0; i != 10000; ++i)
        f.push(rand(10000, 11000), (i % 1000) + 10000, now);

    EXPECT_NEAR(f.ravg.value(now), 5500, 5.0);
    EXPECT_NEAR(f.rq95.value(now), 10900, 10.0);
    EXPECT_EQ(f.davg.value(now), 5499);
    EXPECT_EQ(f.dq95.value(now), 10900);

    advance(now, 0.75s);

    EXPECT_NEAR(f.ravg.value(now), 10500, 10.0);
    EXPECT_NEAR(f.rq95.value(now), 10950, 10.0);
    EXPECT_EQ(f.davg.value(now), 10499);
    EXPECT_EQ(f.dq95.value(now), 10950);

    advance(now, 0.75s);

    EXPECT_NEAR(f.ravg.value(now), 0, 1e-15);
    EXPECT_NEAR(f.rq95.value(now), 0, 1e-15);
    EXPECT_EQ(f.davg.value(now), 0);
    EXPECT_EQ(f.dq95.value(now), 0);
}

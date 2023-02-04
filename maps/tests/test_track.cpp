#include "data.h"

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <maps/analyzer/libs/masstransit/time_prediction/track.h>

#include <string>

using namespace maps::masstransit::time_prediction;

constexpr double EPS = 1e-5;

void test(const TrackPart& val, double length, double time, std::size_t stops) {
    EXPECT_NEAR(val.length(), length, EPS);
    EXPECT_NEAR(val.travelTime(), time, EPS);
    EXPECT_EQ(val.stops(), stops);
}

void test(const TrackPartsSequence& val, std::vector<std::tuple<double, double, double>> exp) {
    std::size_t i = 0;
    for (auto it = val.crbegin(); it != val.crend(); ++it, ++i) {
        EXPECT_LT(i, exp.size());
        test(*it, std::get<0>(exp[i]), std::get<1>(exp[i]), std::get<2>(exp[i]));
    }
    EXPECT_EQ(i, exp.size());
}

Y_UNIT_TEST_SUITE(TestMasstransitTrack) {

    void cmp(const TrackPart::EdgePartWithTime& lhs, const TrackPart::EdgePartWithTime& rhs) {
        EXPECT_EQ(lhs.edge.index, rhs.edge.index);
        EXPECT_NEAR(lhs.edge.from, rhs.edge.from, EPS);
        EXPECT_NEAR(lhs.edge.to, rhs.edge.to, EPS);
        EXPECT_NEAR(lhs.time, rhs.time, EPS);
    }

    Y_UNIT_TEST(TestTrack) {
        TrackPart t{threadData};

        const std::vector<TrackPart::EdgePartWithTime> edges {
            {{0, 0., 1.}, 7.},
            {{1, 0., .4}, 4.},
            {{1, .4, 1.}, 6.},
            {{2, 0., .1}, 3.},
        };

        for (const auto& s : edges) {
            t.pushBack(s);
        }

        test(t, /*length*/48.03232791, /*time*/20., /*stop*/1);

        auto popped = t.popFront(12.499755);
        cmp(popped, {{0, 0., 1.}, 7.});
        popped = t.popFront(12.499755);
        cmp(popped, {{1, 0., .5}, 5.});

        test(t, /*length*/35.53257291, /*time*/8., /*stop*/0);

        t.pushBack({{2, .1, 1.}, 20.});
        t.pushBack({{3, 0., .5}, 15.});
        t.pushBack({{3, .5, 1.}, 15.});

        test(t, /*length*/473.1561045, /*time*/58., /*stop*/0);

        const std::vector<TrackPart::EdgePartWithTime> expected {
            {{1, .5, 1.}, 5.},
            {{2, 0., .1}, 3.},
            {{2, .1, 1.}, 20.}, // this 2 edges
            {{3, 0., 1.}, 30.}, // should be merged
        };

        cmp(t.frontPart(), expected.front());
        cmp(t.backPart(), expected.back());

        auto expIt = expected.begin();
        for (const auto& swt : t.parts()) {
            EXPECT_NE(expIt, expected.end());
            cmp(swt, *expIt);
            expIt++;
        }
        EXPECT_EQ(expIt, expected.end());

        UNIT_ASSERT_EXCEPTION(
            t.pushBack({{5, 0., 1.}, 10.}),
            maps::RuntimeError
        ); // There is a gap between edges
    }

    Y_UNIT_TEST(TestTrackSequence) {
        const auto lengths = std::vector{100., 200.};
        TrackPartsSequence ts{threadData, lengths};

        ts.pushBack({{0, 0., 1.}, 7.});
        ts.pushBack({{1, 0., 1.}, 5.});
        ts.pushBack({{2, 0., .3}, 10.});

        test(ts, {
            // length   time   stop
            {94.097961, 22.,    1},
            {0.,        0.,     0}
        });

        ts.pushBack({{2, .3, 1.}, 25.});
        ts.pushBack({{3, 0., .1}, 5.});

        test(ts, {
            // length   time   stop
            {100.,      16.9344, 0},
            {178.36050, 35.0656, 1}
        });

        ts.pushBack({{3, .1, .9}, 40.});
        ts.pushBack({{3, .9, 1.}, 10.});
        ts.pushBack({{4, 0., 1.}, 15.});

        test(ts, {
            // length   time    stop
            {100.,      36.27688, 0},
            {200.,      40.64682, 0}
        });
    }
}

#include "data.h"

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <maps/analyzer/libs/masstransit/time_prediction/binder.h>
#include <maps/analyzer/libs/masstransit/time_prediction/path.h>

#include <string>

using namespace maps::masstransit::time_prediction;

constexpr double EPS = 1e-5;

Y_UNIT_TEST_SUITE(TestMasstransitPath) {
    Y_UNIT_TEST(TestIterPath) {
        constexpr double STOP_DURATION = 7.;

        const auto path = buildPathBySignals(
            {{{0, .0}, 1600000000}, {{2, .1}, 1600000035}},
            threadData, STOP_DURATION
        );

        std::vector<TrackPart::EdgePartWithTime> expected {
            {{0, 0., 1.}, STOP_DURATION},
            {{1, 0., 1.}, 14.57323},
            {{2, 0., .1}, 13.42676},
        };

        EXPECT_EQ(path.size(), 1);
        const auto& pathPart = *path.begin();
        EXPECT_EQ(pathPart.size(), expected.size());

        for (std::size_t i = 0; i < pathPart.size(); ++i) {
            EXPECT_EQ(pathPart[i].edge.index, expected[i].edge.index);
            EXPECT_NEAR(pathPart[i].edge.from, expected[i].edge.from, EPS);
            EXPECT_NEAR(pathPart[i].edge.to, expected[i].edge.to, EPS);
            EXPECT_NEAR(pathPart[i].time, expected[i].time, EPS);
        }
    }
}

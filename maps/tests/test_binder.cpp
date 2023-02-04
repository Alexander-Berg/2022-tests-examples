#include "data.h"

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <maps/analyzer/libs/masstransit/time_prediction/binder.h>

#include <string>

using namespace maps::masstransit::time_prediction;

template<> void Out<BindingStatus>(IOutputStream& out, BindingStatus st) {
    switch(st) {
    case BindingStatus::OK:
        out << "BindingStatus::OK";
        break;
    case BindingStatus::OUT_OF_TRACK:
        out << "BindingStatus::OUT_OF_TRACK";
        break;
    case BindingStatus::BACK_IN_TRACK:
        out << "BindingStatus::BACK_IN_TRACK";
        break;
    case BindingStatus::BACK_IN_TIME:
        out << "BindingStatus::BACK_IN_TIME";
        break;
    }
}

constexpr double EPS = 1e-5;

Y_UNIT_TEST_SUITE(TestMasstransitBinder) {
    Y_UNIT_TEST(TestSimple) {
        std::vector<ThreadBoundPoint> bound;

        const ThreadUnboundPoint tup1{48.032327, 1600000000};
        const ThreadUnboundPoint tup2{71.065137, 1600000010};

        const BindingStatus st1 = bind(tup1, threadData, bound);
        EXPECT_EQ(st1, BindingStatus::OK);

        const BindingStatus st2 = bind(tup2, threadData, bound);
        EXPECT_EQ(st2, BindingStatus::OK);

        std::vector<ThreadBoundPoint> expected {
            {{2, .1}, 1600000000},
            {{2, .2}, 1600000010},
        };

        EXPECT_EQ(bound.size(), expected.size());

        for (std::size_t i = 0; i < bound.size(); ++i) {
            EXPECT_EQ(bound[i].point.edgeIndex, expected[i].point.edgeIndex);
            EXPECT_NEAR(bound[i].point.pos, expected[i].point.pos, EPS);
            EXPECT_NEAR(bound[i].timestamp, expected[i].timestamp, EPS);
        }
    }

    Y_UNIT_TEST(TestOutOfTrack) {
        std::vector<ThreadBoundPoint> bound;

        const ThreadUnboundPoint tup{1000., 1600000000};
        const BindingStatus st = bind(tup, threadData, bound);
        EXPECT_EQ(st, BindingStatus::OUT_OF_TRACK);
    }

    Y_UNIT_TEST(TestBackInTrack) {
        std::vector<ThreadBoundPoint> bound;

        const ThreadUnboundPoint tup1{48.032327, 1600000000};
        const ThreadUnboundPoint tup2{11.065137, 1600000010};

        const BindingStatus st1 = bind(tup1, threadData, bound);
        EXPECT_EQ(st1, BindingStatus::OK);

        const BindingStatus st2 = bind(tup2, threadData, bound);
        EXPECT_EQ(st2, BindingStatus::BACK_IN_TRACK);
    }


    Y_UNIT_TEST(TestBackInTime) {
        std::vector<ThreadBoundPoint> bound;

        const ThreadUnboundPoint tup1{48.032327, 1600000010};
        const ThreadUnboundPoint tup2{51.065137, 1600000000};

        const BindingStatus st1 = bind(tup1, threadData, bound);
        EXPECT_EQ(st1, BindingStatus::OK);

        const BindingStatus st2 = bind(tup2, threadData, bound);
        EXPECT_EQ(st2, BindingStatus::BACK_IN_TIME);
    }
}

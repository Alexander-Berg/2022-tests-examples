#include "maps/b2bgeo/on_demand/libs/state/include/courier.h"
#include "maps/b2bgeo/on_demand/libs/state/include/matrices.h"

#include <maps/b2bgeo/libs/time/timerange.h>

#include <library/cpp/testing/gtest/gtest.h>

using namespace maps::b2bgeo::common;

namespace maps::b2bgeo::on_demand::state {

namespace {

const cost_matrices::RoutingClass CLASS_ID = "test_class";
constexpr Capacity ZERO_CAPACITY = Capacity{Weight{0.}, Units{0.}};

Matrices create2x2Matrices()
{
    Matrices res{std::make_unique<cost_matrices::MatricesInfo>()};
    auto& matrices = res.matricesInfo->matrices;
    matrices[CLASS_ID] = cost_matrices::TimeDependentCostMatrices(
        {time::TimeHalfOpenRange(time::TimeRangeBase::makeLongest()),
         std::make_shared<cost_matrices::DistanceDurationMatrix>(
             4, cost_matrices::DistanceDuration(10., 10.)),
         2});
    return res;
}

} // anonymous namespace

TEST(TestCourier, finalLocationReturnsCurrentPosIfCourierIsFree)
{
    const auto now = TimePoint{};
    const auto matrices = create2x2Matrices();
    const auto curPos = Position{Point{1., 1.}, MatrixId{0}};
    const auto courier =
        Courier{Courier::Id{"1"}, curPos, ZERO_CAPACITY, CLASS_ID, {}, {}, now};

    const auto [time, pos] = courier.finalLocation(now, matrices);

    EXPECT_EQ(time, now);
    EXPECT_EQ(pos, curPos);
}

TEST(TestCourier, finalLocationAddsDurationForOnePoint)
{
    const auto now = TimePoint{};
    const auto matrices = create2x2Matrices();
    const auto curPos = Position{Point{1., 1.}, MatrixId{0}};
    const auto nextPos = Position{Point{1., 1.}, MatrixId{1}};
    const auto nextLoc =
        Location{DepotLoc{Depot::Id{"1"}}, nextPos, now, now, Distance{}};
    const auto courier = Courier{
        Courier::Id{"1"}, curPos, ZERO_CAPACITY, CLASS_ID, {nextLoc}, {}, now};

    const auto [time, pos] = courier.finalLocation(now, matrices);

    EXPECT_EQ(time, now + Duration{10});
    EXPECT_EQ(pos, nextPos);
}

} // namespace maps::b2bgeo::on_demand::state

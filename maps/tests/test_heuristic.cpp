#include "maps/b2bgeo/on_demand/libs/algorithm/impl/heuristic.h"

#include <library/cpp/testing/gtest/gtest.h>

using namespace maps::b2bgeo::on_demand::state;

namespace maps::b2bgeo::on_demand::algorithm {

namespace {

const auto COURIER_ID = Courier::Id{"courier"};
const auto DEPOT_ID = Depot::Id{"depot"};
const auto DEPOT_POS = Position{common::Point{1., 1.}};
const auto ORDER_ID = Order::Id{"order"};
const auto DELIVERY_POS = Position{common::Point{2., 2.}};

State defaultState()
{
    return {
        {},
        buildMap<Depot>({{DEPOT_ID, DEPOT_POS, Duration{}}}),
        buildMap<Courier>(
            {{COURIER_ID, DEPOT_POS, Capacity{Weight{100.}, Units{100.}}}}),
        buildMap<Order>(
            {{ORDER_ID,
              TimePoint{},
              Duration{},
              DELIVERY_POS,
              OrderTime{TimeWindow{TimePoint{}, TimePoint{Duration{100}}}},
              ShipmentSize{Weight{0.}, Units{0.}},
              DEPOT_ID}}),
        {},
        {},
        {}};
}

} // anonymous namespace

TEST(HeuristicTests, heuristicDecidesBasedOnDepartureTime)
{
    const auto state = defaultState();

    const auto depotLoc = Location{
        DepotLoc{DEPOT_ID}, DEPOT_POS, state.now, state.now, Distance{0.}};
    auto deliveryLoc = Location{
        DeliveryLoc{{ORDER_ID}},
        DELIVERY_POS,
        state.now,
        state.now,
        Distance{1000.}};

    {
        const auto [accepted, dropped, time] = applyWaitHeuristic(
            state, {Assignment{COURIER_ID, {depotLoc, deliveryLoc}}});
        EXPECT_EQ(dropped.size(), 1U);
        EXPECT_EQ(accepted.size(), 0U);
        EXPECT_GT(time, state.now);
    }

    {
        deliveryLoc.departureTime += Duration{100};
        const auto [accepted, dropped, time] = applyWaitHeuristic(
            state, {Assignment{COURIER_ID, {depotLoc, deliveryLoc}}});
        EXPECT_EQ(dropped.size(), 0U);
        EXPECT_EQ(accepted.size(), 1U);
    }
}

TEST(HeuristicTests, heuristicDecidesBasedOnCourierLoad)
{
    auto state = defaultState();

    const auto depotLoc = Location{
        DepotLoc{DEPOT_ID}, DEPOT_POS, state.now, state.now, Distance{0.}};
    auto deliveryLoc = Location{
        DeliveryLoc{{ORDER_ID}},
        DELIVERY_POS,
        state.now,
        state.now,
        Distance{1000.}};

    {
        const auto [accepted, dropped, time] = applyWaitHeuristic(
            state, {Assignment{COURIER_ID, {depotLoc, deliveryLoc}}});
        EXPECT_EQ(dropped.size(), 1U);
        EXPECT_EQ(accepted.size(), 0U);
        EXPECT_GT(time, state.now);
    }

    {
        state.ordersToAssign.at(ORDER_ID).size.units = Units{100.};
        const auto [accepted, dropped, time] = applyWaitHeuristic(
            state, {Assignment{COURIER_ID, {depotLoc, deliveryLoc}}});
        EXPECT_EQ(dropped.size(), 0U);
        EXPECT_EQ(accepted.size(), 1U);
    }
}

} // namespace maps::b2bgeo::on_demand::algorithm

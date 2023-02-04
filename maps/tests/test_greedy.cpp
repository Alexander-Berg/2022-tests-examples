#include "maps/b2bgeo/on_demand/libs/algorithm/impl/greedy.h"

#include "maps/b2bgeo/on_demand/libs/matrices/include/init_matrices.h"

#include <library/cpp/testing/gtest/gtest.h>

using namespace maps::b2bgeo::on_demand::algorithm;
using namespace maps::b2bgeo::on_demand::state;

namespace {

Depot buildDepot(const std::string& id, double position)
{
    return {Depot::Id{id}, Position{{position, position}}, Duration{10.0}};
}

Courier buildCourier(const std::string& id, double position)
{
    return {
        Courier::Id{id},
        Position{{position, position}},
        Capacity{Weight{3.0}, Units{3.0}},
        maps::b2bgeo::cost_matrices::DEFAULT_ROUTING_CLASS};
}

Order buildOrder(const std::string& id, double position, const std::string& depotId)
{
    return {
        Order::Id{id},
        TimePoint{Duration{1}},
        Duration{10.0},
        Position{{position, position}},
        OrderTime{Asap{}},
        ShipmentSize{Weight{1.0}, Units{1.0}},
        Depot::Id{depotId}};
}

State buildSimpleState()
{
    using namespace maps::b2bgeo;
    using namespace maps::b2bgeo::on_demand;
    using namespace maps::b2bgeo::on_demand::matrices;
    using namespace maps::b2bgeo::on_demand::state;
    TimePoint startAt{Duration{1}};
    std::vector<Courier> couriers{buildCourier("1", 1.0)};
    std::vector<Order> orders{buildOrder("1", 1.1, "1")};
    std::vector<Depot> depots{buildDepot("1", 1.0)};
    Matrices matrices = initMatrices(
        maps::b2bgeo::traffic_info::MatrixRouter::Geodesic,
        {},
        traffic_info::RoutingConfig(RuntimeEnv::dev),
        couriers,
        orders,
        depots);
    return {
        startAt,
        buildMap(depots),
        buildMap(couriers),
        buildMap(orders),
        std::move(matrices),
        {},
        state::SolverOptions()};
}

State buildTwoOrdersState()
{
    using namespace maps::b2bgeo;
    using namespace maps::b2bgeo::on_demand;
    using namespace maps::b2bgeo::on_demand::state;
    TimePoint startAt{Duration{1}};
    std::vector<state::Courier> couriers{
        buildCourier("1", 1.1), buildCourier("2", 2.1)};
    std::vector<state::Order> orders{
        buildOrder("1", 1.2, "1"),
        buildOrder("2", 2.2, "2"),
    };
    std::vector<state::Depot> depots{buildDepot("1", 1.0), buildDepot("2", 2.0)};
    state::Matrices matrices = maps::b2bgeo::on_demand::matrices::initMatrices(
        maps::b2bgeo::traffic_info::MatrixRouter::Geodesic,
        {},
        traffic_info::RoutingConfig(RuntimeEnv::dev),
        couriers,
        orders,
        depots);
    return {
        startAt,
        buildMap(depots),
        buildMap(couriers),
        buildMap(orders),
        std::move(matrices),
        {},
        state::SolverOptions()};
}

} // anonymous namespace

TEST(SolveTests, emptyState)
{
    State emptyState;
    auto result = greedySolve({}, {});

    EXPECT_EQ(result.size(), 0U);
}

TEST(SolveTests, returnsNotEmptyAssings)
{
    State state = buildSimpleState();
    auto result = greedySolve(state, {});
    EXPECT_EQ(result.size(), 1U);
}

TEST(SolveTests, assignsToCourier)
{
    State state = buildSimpleState();
    auto result = greedySolve(state, {});

    auto& assignment = result[0];
    EXPECT_EQ(assignment.courierId.get(), "1");
}

TEST(SolveTests, assignsCorrectLocations)
{
    State state = buildSimpleState();
    auto result = greedySolve(state, {});

    auto& assignment = result[0];
    EXPECT_EQ(assignment.locations.size(), 2U);
    EXPECT_EQ(
        assignment.locations[0].position.point,
        state.depots.at(Depot::Id{"1"}).position.point);
    EXPECT_EQ(
        assignment.locations[1].position.point,
        state.ordersToAssign.at(Order::Id{"1"}).deliveryTo.point);
}

TEST(SolveTests, assingAllOrders)
{
    State state = buildTwoOrdersState();
    auto result = greedySolve(state, {});

    EXPECT_EQ(result.size(), 2U);
}

TEST(SolveTests, assingToAllCouriers)
{
    State state = buildTwoOrdersState();
    auto result = greedySolve(state, {});
    std::vector<std::string> courierIds;
    std::transform(
        result.begin(),
        result.end(),
        std::back_inserter(courierIds),
        [](const Assignment& a) { return a.courierId.get(); });
    sort(courierIds.begin(), courierIds.end());
    std::vector<std::string> expected{"1", "2"};

    EXPECT_EQ(courierIds, expected);
}

TEST(SolveTests, assingToCorrectDepots)
{
    State state = buildTwoOrdersState();
    auto result = greedySolve(state, {});
    for (const Assignment& assignment: result) {
        if (assignment.courierId.get() == "1")
            EXPECT_EQ(
                assignment.locations[0].position.point,
                state.depots.at(Depot::Id{"1"}).position.point);
        else
            EXPECT_EQ(
                assignment.locations[0].position.point,
                state.depots.at(Depot::Id{"2"}).position.point);
    }
}

TEST(SolveTests, assingToCorrectOrders)
{
    State state = buildTwoOrdersState();
    auto result = greedySolve(state, {});
    for (const Assignment& assignment: result) {
        if (assignment.courierId.get() == "1")
            EXPECT_EQ(
                assignment.locations[1].position.point,
                state.ordersToAssign.at(Order::Id{"1"}).deliveryTo.point);
        else
            EXPECT_EQ(
                assignment.locations[1].position.point,
                state.ordersToAssign.at(Order::Id{"2"}).deliveryTo.point);
    }
}

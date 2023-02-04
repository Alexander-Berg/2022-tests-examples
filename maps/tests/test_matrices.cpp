#include "maps/b2bgeo/on_demand/libs/matrices/include/init_matrices.h"

#include <library/cpp/testing/gtest/gtest.h>

#include <vector>

using namespace maps::b2bgeo;
using namespace maps::b2bgeo::on_demand::matrices;
using namespace maps::b2bgeo::on_demand::state;

namespace {

Depot buildDepot(const std::string& id, double position)
{
    return {Depot::Id{id}, Position{{position, position}}, Duration{10.0}};
}

Courier buildCourier(
    const std::string& id,
    double position,
    cost_matrices::RoutingClass routingClass = cost_matrices::DEFAULT_ROUTING_CLASS)
{
    return {
        Courier::Id{id},
        Position{{position, position}},
        Capacity{Weight{3.0}, Units{3.0}},
        std::move(routingClass)};
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

} // anonymous namespace

TEST(Matrices, simpleMatrix)
{
    std::vector<Courier> couriers{buildCourier("1", 1.0)};
    std::vector<Order> orders{buildOrder("1", 1.1, "1")};
    std::vector<Depot> depots{buildDepot("1", 1.2)};
    auto result = initMatrices(
        traffic_info::MatrixRouter::Geodesic,
        "",
        traffic_info::RoutingConfig(RuntimeEnv::dev),
        couriers,
        orders,
        depots);
    const auto& matrices = result.matricesInfo->matrices;
    EXPECT_EQ(matrices.size(), 1U);
    EXPECT_EQ(matrices.begin()->second.size(), 1U);
    EXPECT_EQ(matrices.begin()->second.at(0).stride, 3U);
}

TEST(Matrices, simpleMatrixWithNotUniqueLocations)
{
    std::vector<Courier> couriers{buildCourier("1", 1.0)};
    std::vector<Order> orders{buildOrder("1", 1.0, "1")};
    std::vector<Depot> depots{buildDepot("1", 1.0)};
    auto result = initMatrices(
        traffic_info::MatrixRouter::Geodesic,
        "",
        traffic_info::RoutingConfig(RuntimeEnv::dev),
        couriers,
        orders,
        depots);
    const auto& matrices = result.matricesInfo->matrices;
    EXPECT_EQ(matrices.size(), 1U);
    EXPECT_EQ(matrices.begin()->second.size(), 1U);
    EXPECT_EQ(matrices.begin()->second.at(0).stride, 1U);
}

TEST(Matrices, noOrdersThrowException)
{
    std::vector<Courier> couriers{buildCourier("1", 1.0)};
    std::vector<Order> orders{};
    std::vector<Depot> depots{buildDepot("1", 1.1)};
    EXPECT_THROW(
        initMatrices(
            traffic_info::MatrixRouter::Geodesic,
            "",
            traffic_info::RoutingConfig(RuntimeEnv::dev),
            couriers,
            orders,
            depots),
        maps::Exception);
}

TEST(Matrices, noDepotsMatrix)
{
    std::vector<Courier> couriers{buildCourier("1", 1.0)};
    std::vector<Order> orders{buildOrder("1", 1.1, "1")};
    std::vector<Depot> depots{};
    auto result = initMatrices(
        traffic_info::MatrixRouter::Geodesic,
        "",
        traffic_info::RoutingConfig(RuntimeEnv::dev),
        couriers,
        orders,
        depots);
    const auto& matrices = result.matricesInfo->matrices;
    EXPECT_EQ(matrices.size(), 1U);
    EXPECT_EQ(matrices.begin()->second.size(), 1U);
    EXPECT_EQ(matrices.begin()->second.at(0).stride, 2U);
}

TEST(Matrices, noCouriersThrowsException)
{
    std::vector<Courier> couriers{};
    std::vector<Order> orders{buildOrder("1", 1.1, "1")};
    std::vector<Depot> depots{buildDepot("1", 1.2)};
    EXPECT_THROW(
        initMatrices(
            traffic_info::MatrixRouter::Geodesic,
            "",
            traffic_info::RoutingConfig(RuntimeEnv::dev),
            couriers,
            orders,
            depots),
        std::runtime_error);
}

TEST(Matrices, emptyMatrixThrowsException)
{
    std::vector<Courier> couriers{};
    std::vector<Order> orders{};
    std::vector<Depot> depots{};
    EXPECT_THROW(
        initMatrices(
            traffic_info::MatrixRouter::Geodesic,
            "",
            traffic_info::RoutingConfig(RuntimeEnv::dev),
            couriers,
            orders,
            depots),
        std::runtime_error);
}

TEST(Matrices, complexMatrix)
{
    std::vector<Courier> couriers{buildCourier("1", 1.0), buildCourier("2", 2.0)};
    std::vector<Order> orders{
        buildOrder("1", 1.1, "1"),
        buildOrder("2", 1.2, "1"),
        buildOrder("3", 1.1, "1"),
        buildOrder("4", 2.2, "2")};
    std::vector<Depot> depots{buildDepot("1", 1.15), buildDepot("2", 2.1)};
    auto result = initMatrices(
        traffic_info::MatrixRouter::Geodesic,
        "",
        traffic_info::RoutingConfig(RuntimeEnv::dev),
        couriers,
        orders,
        depots);
    const auto& matrices = result.matricesInfo->matrices;
    EXPECT_EQ(matrices.size(), 1U);
    EXPECT_EQ(matrices.begin()->second.size(), 1U);
    EXPECT_EQ(matrices.begin()->second.at(0).stride, 7U);
}

TEST(Matrices, differentRoutingClasses)
{
    using namespace maps::b2bgeo::traffic_info;
    std::vector<Courier> couriers{
        buildCourier("1", 1.0, VehicleClass::BICYCLE.id),
        buildCourier("2", 2.0, VehicleClass::DRIVING.id),
        buildCourier("3", 3.0, VehicleClass::TRANSIT.id),
        buildCourier("4", 4.0, VehicleClass::TRUCK.id),
        buildCourier("5", 5.0, VehicleClass::WALKING.id)};
    std::vector<Order> orders{buildOrder("1", 1.1, "1")};
    std::vector<Depot> depots{buildDepot("1", 1.15)};
    auto result = initMatrices(
        traffic_info::MatrixRouter::Geodesic,
        "",
        traffic_info::RoutingConfig(RuntimeEnv::dev),
        couriers,
        orders,
        depots);
    const auto& matrices = result.matricesInfo->matrices;
    EXPECT_EQ(matrices.size(), 5U);
}

TEST(Matrices, throwsWhenRoutingClassIsUnknown)
{
    using namespace maps::b2bgeo::traffic_info;
    std::vector<Courier> couriers{buildCourier("1", 1.0, "unknown")};
    std::vector<Order> orders{buildOrder("1", 1.1, "1")};
    std::vector<Depot> depots{buildDepot("1", 1.15)};
    EXPECT_THROW(
        initMatrices(
            traffic_info::MatrixRouter::Geodesic,
            "",
            traffic_info::RoutingConfig(RuntimeEnv::dev),
            couriers,
            orders,
            depots),
        std::runtime_error);
}

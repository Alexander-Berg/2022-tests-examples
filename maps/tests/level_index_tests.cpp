#include <boost/test/unit_test.hpp>
#include <random>

#include <maps/indoor/libs/indoor_positioning/include/level_index.h>
#include <maps/indoor/libs/indoor_positioning/include/plan_level_id.h>
#include <maps/indoor/libs/indoor_positioning/include/transmitter.h>
#include <maps/indoor/libs/indoor_positioning/include/utils.h>

#include "geometry.h"

using namespace boost::unit_test;
using namespace INDOOR_POSITIONING_NAMESPACE;

namespace {

const auto TX_TYPE = TransmitterType::BEACON;

struct TestLevelIndex
{
    TestLevelIndex()
        : planLevels({
            {"plan", "1"},
            {"plan", "2"}})
        , transmitters({
            {"tx1", TX_TYPE, planLevels[0], {30.000, 40.000}, TransmitterRssiModel()},
            {"tx2", TX_TYPE, planLevels[0], {30.001, 40.000}, TransmitterRssiModel()},
            {"tx3", TX_TYPE, planLevels[0], {30.001, 40.001}, TransmitterRssiModel()},
            {"tx4", TX_TYPE, planLevels[0], {30.000, 40.001}, TransmitterRssiModel()},
            {"tx5", TX_TYPE, planLevels[1], {30.000, 40.000}, TransmitterRssiModel()},
            {"tx6", TX_TYPE, planLevels[1], {30.001, 40.000}, TransmitterRssiModel()},
            {"tx7", TX_TYPE, planLevels[1], {30.001, 40.001}, TransmitterRssiModel()},
            {"tx8", TX_TYPE, planLevels[1], {30.000, 40.001}, TransmitterRssiModel()}})
        , levelIndex(createLevelIndex(transmitters))
    {}

    std::vector<PlanLevelId> planLevels;
    Transmitters transmitters;
    std::shared_ptr<LevelIndex> levelIndex;
};

} // namespace

BOOST_AUTO_TEST_CASE(level_index_find_level)
{
    TestLevelIndex test;
    for(const auto& tx : test.transmitters) {
        auto levelInfo = test.levelIndex->findLevel(tx.planLevel);
        BOOST_CHECK(levelInfo != nullptr);
        BOOST_CHECK(levelInfo->planLevel() == tx.planLevel);
        BOOST_CHECK(levelInfo->findTransmitter(tx.id) != nullptr);
    }
}

BOOST_AUTO_TEST_CASE(level_info_geometry)
{
    const auto planLevel = PlanLevelId{"plan", "1"};
    const auto originPoint = Point{60.000000, 40.000000};

    // Data contains tuples of the form:
    //  (xyPoint, area(geometry), area(bbox)
    using ItemType = std::tuple<XYPoint, double, double>;
    const auto data = std::vector<ItemType>{
        ItemType{XYPoint{0.0,  0.0}, 400.0, 400.0},
        ItemType{XYPoint{10.0, 10.0}, 700.0, 900.0},
        ItemType{XYPoint{20.0, 20.0}, 1000.0, 1600.0},
        ItemType{XYPoint{30.0, 30.0}, 1300.0, 2500.0},
        ItemType{XYPoint{40.0, 40.0}, 1600.0, 3600.0},
        ItemType{XYPoint{50.0, 50.0}, 1900.0, 4900.0}};

    LevelInfo level(planLevel, originPoint);

    for(const auto& item : data) {
        const double x = std::get<0>(item).x;
        const double y = std::get<0>(item).y;
        level.addGeometry(createMultiPolygon({
            pointFromLocalMetricCS(XYPoint{x - 10, y - 10}, originPoint),
            pointFromLocalMetricCS(XYPoint{x + 10, y - 10}, originPoint),
            pointFromLocalMetricCS(XYPoint{x + 10, y + 10}, originPoint),
            pointFromLocalMetricCS(XYPoint{x - 10, y + 10}, originPoint),
            pointFromLocalMetricCS(XYPoint{x - 10, y - 10}, originPoint)}));

        // Check geometry connectivity
        BOOST_CHECK_EQUAL(
            level.xyGeometry().size(),
            1);

        // Check geometry area
        BOOST_CHECK_CLOSE(
            boost::geometry::area(level.xyGeometry()),
            std::get<1>(item),
            1e-6);

        // Check bbox area
        BOOST_CHECK_CLOSE(
            boost::geometry::area(level.xyBox()),
            std::get<2>(item),
            1e-6);
    }
}

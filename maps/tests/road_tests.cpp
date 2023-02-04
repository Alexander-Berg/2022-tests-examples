#include <library/cpp/testing/unittest/gtest.h>

#include <maps/libs/geolib/include/polyline.h>
#include <maps/libs/geolib/include/conversion.h>
#include <maps/libs/geolib/include/test_tools/comparison.h>

#include <maps/wikimap/mapspro/services/autocart/libs/geometry/include/hex_wkb.h>

#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/objects/include/road.h>

using namespace testing;

namespace maps::wiki::autocart::pipeline {

namespace tests {

Y_UNIT_TEST_SUITE(road_tests)
{

Y_UNIT_TEST(road_from_mercator_geom_test)
{
    const geolib3::Polyline2 gtMercatorGeom({{1., 0.}, {0., 0.}, {0., 1.}});
    const int64_t gtFc = 3;
    const int64_t gtFow = 8;

    Road road = Road::fromMercatorGeom(gtMercatorGeom);
    road.setFc(gtFc);
    road.setFow(gtFow);

    EXPECT_EQ(road.getFc(), gtFc);
    EXPECT_EQ(road.getFow(), gtFow);
    EXPECT_TRUE(geolib3::test_tools::approximateEqual(
                    gtMercatorGeom,
                    road.toMercatorGeom(), geolib3::EPS));
    EXPECT_TRUE(geolib3::test_tools::approximateEqual(
                    geolib3::convertMercatorToGeodetic(gtMercatorGeom),
                    road.toGeodeticGeom(), geolib3::EPS));
}

Y_UNIT_TEST(road_from_geodetic_geom_test)
{
    const geolib3::Polyline2 gtGeodeticGeom({{1., 0.}, {0., 0.}, {0., 1.}});
    const int64_t gtFc = 3;
    const int64_t gtFow = 8;

    Road road = Road::fromGeodeticGeom(gtGeodeticGeom);
    road.setFc(gtFc);
    road.setFow(gtFow);

    EXPECT_EQ(road.getFc(), gtFc);
    EXPECT_EQ(road.getFow(), gtFow);
    EXPECT_TRUE(geolib3::test_tools::approximateEqual(
                    gtGeodeticGeom,
                    road.toGeodeticGeom(), geolib3::EPS));
    EXPECT_TRUE(geolib3::test_tools::approximateEqual(
                    geolib3::convertGeodeticToMercator(gtGeodeticGeom),
                    road.toMercatorGeom(), geolib3::EPS));
}

Y_UNIT_TEST(compare_test)
{
    int64_t fc = 1;
    int64_t fow = 2;
    Road road = Road::fromGeodeticGeom(geolib3::Polyline2({{0., 0.}, {1., 0.}, {1., 1.}}));
    road.setFc(fc);
    road.setFow(fow);

    Road eqRoad = Road::fromGeodeticGeom(road.toGeodeticGeom());
    eqRoad.setFc(fc);
    eqRoad.setFow(fow);

    int64_t notEqFC = 3;
    int64_t notEqFOW = 4;
    Road notEqRoad1 = Road::fromGeodeticGeom(geolib3::Polyline2({{0., 0.}, {1., 0.}, {1., 2.}}));
    notEqRoad1.setFc(fc);
    notEqRoad1.setFow(fow);

    Road notEqRoad2 = Road::fromGeodeticGeom(road.toGeodeticGeom());
    notEqRoad2.setFc(fc);
    notEqRoad2.setFow(notEqFOW);

    Road notEqRoad3 = Road::fromGeodeticGeom(road.toGeodeticGeom());
    notEqRoad3.setFc(notEqFC);
    notEqRoad3.setFow(fow);

    EXPECT_TRUE(road == eqRoad);
    EXPECT_FALSE(road == notEqRoad1);
    EXPECT_FALSE(road == notEqRoad2);
    EXPECT_FALSE(road == notEqRoad3);
}

Y_UNIT_TEST(road_from_yt_node_test)
{
    Road road = Road::fromGeodeticGeom(geolib3::Polyline2({{0., 0.}, {1., 0.}, {1., 1.}}));
    road.setFc(3);
    road.setFow(10);

    EXPECT_TRUE(road == Road::fromYTNode(road.toYTNode()));

    NYT::TNode node;
    road.toYTNode(node);
    EXPECT_TRUE(road == Road::fromYTNode(node));
}

} // Y_UNIT_TEST_SUITE(road_tests)

} // namespace test

} // namespace maps::wiki::autocart::pipeline

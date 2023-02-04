#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <maps/libs/geolib/include/polygon.h>
#include <maps/libs/geolib/include/conversion.h>
#include <maps/libs/geolib/include/test_tools/comparison.h>

#include <maps/wikimap/mapspro/services/autocart/libs/geometry/include/hex_wkb.h>

#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/objects/include/building.h>

using namespace testing;

namespace maps::wiki::autocart::pipeline {

namespace tests {

Y_UNIT_TEST_SUITE(building_tests)
{

Y_UNIT_TEST(building_from_mercator_geom_test)
{
    const geolib3::Polygon2 mercatorGeom({{1., 0.}, {0., 0.}, {0., 1.}});

    Building bld = Building::fromMercatorGeom(mercatorGeom);

    EXPECT_TRUE(geolib3::test_tools::approximateEqual(
                    mercatorGeom,
                    bld.toMercatorGeom(), geolib3::EPS));
    EXPECT_TRUE(geolib3::test_tools::approximateEqual(
                    geolib3::convertMercatorToGeodetic(mercatorGeom),
                    bld.toGeodeticGeom(), geolib3::EPS));
}

Y_UNIT_TEST(building_from_geodetic_geom_test)
{
    const geolib3::Polygon2 geodeticGeom({{1., 0.}, {0., 0.}, {0., 1.}});

    Building bld = Building::fromGeodeticGeom(geodeticGeom);

    EXPECT_TRUE(geolib3::test_tools::approximateEqual(
                    geodeticGeom,
                    bld.toGeodeticGeom(), geolib3::EPS));
    EXPECT_TRUE(geolib3::test_tools::approximateEqual(
                    geolib3::convertGeodeticToMercator(geodeticGeom),
                    bld.toMercatorGeom(), geolib3::EPS));
}

Y_UNIT_TEST(building_yt_node_test)
{
    Building gtBld = Building::fromMercatorGeom(
        geolib3::Polygon2({{1., 0.}, {0., 0.}, {0., 1.}})
    );
    gtBld.setId(1);
    gtBld.setHeight(3);
    gtBld.setFTTypeId(FTTypeId::URBAN_RESIDENTIAL);

    EXPECT_TRUE(gtBld == Building::fromYTNode(gtBld.toYTNode()));

    NYT::TNode node;
    gtBld.toYTNode(node);
    EXPECT_TRUE(gtBld == Building::fromYTNode(node));
}

Y_UNIT_TEST(building_json_test)
{
    Building gtBld = Building::fromMercatorGeom(
        geolib3::Polygon2({{1., 0.}, {0., 0.}, {0., 1.}})
    );
    gtBld.setId(1);
    gtBld.setHeight(3);
    gtBld.setFTTypeId(FTTypeId::URBAN_RESIDENTIAL);

    std::stringstream ss;
    json::Builder builder(ss);
    builder << [&](json::ObjectBuilder b) {
        gtBld.toJson(b);
    };

    Building testBld = Building::fromJson(json::Value::fromStream(ss));

    EXPECT_TRUE(gtBld == testBld);
}

Y_UNIT_TEST(compare_test)
{
    int64_t id = 1;
    int64_t height = 3;
    FTTypeId ftTypeId = FTTypeId::URBAN_RESIDENTIAL;
    Building bld = Building::fromMercatorGeom(geolib3::Polygon2({{1., 0.}, {0., 0.}, {0., 1.}}));
    bld.setId(id);
    bld.setHeight(height);
    bld.setFTTypeId(ftTypeId);

    Building eqBld = Building::fromMercatorGeom(bld.toMercatorGeom());
    eqBld.setId(id);
    eqBld.setHeight(height);
    eqBld.setFTTypeId(ftTypeId);

    int64_t notEqId = 2;
    int64_t notEqHeight = 6;
    FTTypeId notEqFTTypeId = FTTypeId::URBAN_INDUSTRIAL;
    Building notEqBld1 = Building::fromMercatorGeom(geolib3::Polygon2({{1., 0.}, {0., 0.}, {0., 2.}}));
    notEqBld1.setId(id);
    notEqBld1.setHeight(height);
    notEqBld1.setFTTypeId(ftTypeId);

    Building notEqBld2 = Building::fromMercatorGeom(bld.toMercatorGeom());
    notEqBld2.setId(notEqId);
    notEqBld2.setHeight(height);
    notEqBld2.setFTTypeId(ftTypeId);

    Building notEqBld3 = Building::fromMercatorGeom(bld.toMercatorGeom());
    notEqBld2.setId(id);
    notEqBld2.setHeight(notEqHeight);
    notEqBld2.setFTTypeId(notEqFTTypeId);

    EXPECT_TRUE(bld == eqBld);
    EXPECT_FALSE(bld == notEqBld1);
    EXPECT_FALSE(bld == notEqBld2);
    EXPECT_FALSE(bld == notEqBld3);
}

} // Y_UNIT_TEST_SUITE(building_tests)

} // namespace test

} // namespace maps::wiki::autocart::pipeline

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <maps/libs/geolib/include/multipolygon.h>
#include <maps/libs/geolib/include/conversion.h>
#include <maps/libs/geolib/include/test_tools/comparison.h>

#include <maps/wikimap/mapspro/services/autocart/libs/geometry/include/hex_wkb.h>

#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/objects/include/area.h>

using namespace testing;

namespace maps::wiki::autocart::pipeline {

namespace tests {

Y_UNIT_TEST_SUITE(area_tests)
{

Y_UNIT_TEST(area_from_mercator_geom_test)
{
    const geolib3::MultiPolygon2 mercatorGeom({
        geolib3::Polygon2({{1., 0.}, {0., 0.}, {0., 1.}})
    });

    Area area = Area::fromMercatorGeom(mercatorGeom);

    EXPECT_TRUE(geolib3::test_tools::approximateEqual(
                    mercatorGeom,
                    area.toMercatorGeom(), geolib3::EPS));
    EXPECT_TRUE(geolib3::test_tools::approximateEqual(
                    geolib3::convertMercatorToGeodetic(mercatorGeom),
                    area.toGeodeticGeom(), geolib3::EPS));
}

Y_UNIT_TEST(area_from_geodetic_geom_test)
{
    const geolib3::MultiPolygon2 geodeticGeom({
        geolib3::Polygon2({{1., 0.}, {0., 0.}, {0., 1.}})
    });

    Area area = Area::fromGeodeticGeom(geodeticGeom);

    EXPECT_TRUE(geolib3::test_tools::approximateEqual(
                    geodeticGeom,
                    area.toGeodeticGeom(), geolib3::EPS));
    EXPECT_TRUE(geolib3::test_tools::approximateEqual(
                    geolib3::convertGeodeticToMercator(geodeticGeom),
                    area.toMercatorGeom(), geolib3::EPS));
}

Y_UNIT_TEST(area_yt_node_test)
{
    Area gtArea = Area::fromMercatorGeom(
        geolib3::MultiPolygon2({
            geolib3::Polygon2({{1., 0.}, {0., 0.}, {0., 1.}})
        })
    );
    gtArea.setFTTypeId(FTTypeId::URBAN_RESIDENTIAL);

    EXPECT_TRUE(gtArea == Area::fromYTNode(gtArea.toYTNode()));

    NYT::TNode node;
    gtArea.toYTNode(node);
    EXPECT_TRUE(gtArea == Area::fromYTNode(node));
}

} // Y_UNIT_TEST_SUITE(area_tests)

} // namespace test

} // namespace maps::wiki::autocart::pipeline

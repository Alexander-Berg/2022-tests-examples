#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <maps/libs/geolib/include/point.h>
#include <maps/libs/geolib/include/conversion.h>
#include <maps/libs/geolib/include/test_tools/comparison.h>

#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/objects/include/dwellplace.h>

using namespace testing;

namespace maps::wiki::autocart::pipeline {

namespace tests {

Y_UNIT_TEST_SUITE(dwellplace_tests)
{

Y_UNIT_TEST(dwellplace_from_mercator_geom_test)
{
    const geolib3::Point2 gtMercatorGeom(1., 0.);

    Dwellplace place = Dwellplace::fromMercatorGeom(gtMercatorGeom);

    EXPECT_TRUE(geolib3::test_tools::approximateEqual(
                    gtMercatorGeom,
                    place.toMercatorGeom(), geolib3::EPS));
    EXPECT_TRUE(geolib3::test_tools::approximateEqual(
                    geolib3::convertMercatorToGeodetic(gtMercatorGeom),
                    place.toGeodeticGeom(), geolib3::EPS));
}

Y_UNIT_TEST(dwellplace_from_geodetic_geom_test)
{
    const geolib3::Point2 gtGeodeticGeom(1., 0.);

    Dwellplace place = Dwellplace::fromGeodeticGeom(gtGeodeticGeom);

    EXPECT_TRUE(geolib3::test_tools::approximateEqual(
                    gtGeodeticGeom, place.toGeodeticGeom(), geolib3::EPS));
    EXPECT_TRUE(geolib3::test_tools::approximateEqual(
                    geolib3::convertGeodeticToMercator(gtGeodeticGeom),
                    place.toMercatorGeom(), geolib3::EPS));
}

Y_UNIT_TEST(dwellplace_from_yt_node_test)
{
    const Dwellplace place = Dwellplace::fromGeodeticGeom({1., 0.});

    EXPECT_TRUE(place == Dwellplace::fromYTNode(place.toYTNode()));

    NYT::TNode node;
    place.toYTNode(node);
    EXPECT_TRUE(place == Dwellplace::fromYTNode(node));
}


Y_UNIT_TEST(compare_test)
{
    const Dwellplace place = Dwellplace::fromGeodeticGeom({1., 0.});
    const Dwellplace eqPlace = Dwellplace::fromGeodeticGeom(place.toGeodeticGeom());
    const Dwellplace notEqPlace = Dwellplace::fromGeodeticGeom({1., 1.});

    EXPECT_TRUE(place == eqPlace);
    EXPECT_FALSE(place == notEqPlace);
}

} // Y_UNIT_TEST_SUITE(dwellplace_tests)

} // namespace test

} // namespace maps::wiki::autocart::pipeline

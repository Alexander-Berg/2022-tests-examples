#include <library/cpp/testing/unittest/gtest.h>

#include <maps/libs/geolib/include/point.h>
#include <maps/libs/geolib/include/bounding_box.h>
#include <maps/libs/geolib/include/conversion.h>
#include <maps/libs/geolib/include/test_tools/comparison.h>

#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/objects/include/bbox.h>

using namespace testing;

namespace maps::wiki::autocart::pipeline {

namespace tests {

Y_UNIT_TEST_SUITE(bbox_tests)
{

Y_UNIT_TEST(bbox_from_mercator_geom_test)
{
    const geolib3::BoundingBox gtMercatorGeom{{0., 1.}, {2., 3.}};

    BBox bbox = BBox::fromMercatorGeom(gtMercatorGeom);

    EXPECT_TRUE(geolib3::test_tools::approximateEqual(
                    gtMercatorGeom,
                    bbox.toMercatorGeom(), geolib3::EPS));
    EXPECT_TRUE(geolib3::test_tools::approximateEqual(
                    geolib3::convertMercatorToGeodetic(gtMercatorGeom),
                    bbox.toGeodeticGeom(), geolib3::EPS));
}

Y_UNIT_TEST(bbox_from_geodetic_geom_test)
{
    const geolib3::BoundingBox gtGeodeticGeom{{0., 1.}, {2., 3.}};

    BBox bbox = BBox::fromGeodeticGeom(gtGeodeticGeom);

    EXPECT_TRUE(geolib3::test_tools::approximateEqual(
                    gtGeodeticGeom, bbox.toGeodeticGeom(), geolib3::EPS));
    EXPECT_TRUE(geolib3::test_tools::approximateEqual(
                    geolib3::convertGeodeticToMercator(gtGeodeticGeom),
                    bbox.toMercatorGeom(), geolib3::EPS));
}

Y_UNIT_TEST(compare_test)
{
    const BBox bbox = BBox::fromGeodeticGeom(geolib3::BoundingBox({0., 1.}, {2., 3.}));
    const BBox eqBBox = BBox::fromGeodeticGeom(bbox.toGeodeticGeom());
    const BBox notEqBBox = BBox::fromGeodeticGeom(geolib3::BoundingBox({0., 1.}, {2., 5.}));

    EXPECT_TRUE(bbox == eqBBox);
    EXPECT_FALSE(bbox == notEqBBox);
}

Y_UNIT_TEST(bbox_from_yt_node_test)
{
    const BBox bbox = BBox::fromGeodeticGeom(geolib3::BoundingBox({0., 1.}, {2., 3.}));
    EXPECT_TRUE(bbox == BBox::fromYTNode(bbox.toYTNode()));

    NYT::TNode node;
    bbox.toYTNode(node);
    EXPECT_TRUE(bbox == BBox::fromYTNode(node));
}

} // Y_UNIT_TEST_SUITE(bbox_tests)

} // namespace test

} // namespace maps::wiki::autocart::pipeline

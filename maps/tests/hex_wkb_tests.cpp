#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <maps/libs/geolib/include/multipolygon.h>
#include <maps/libs/geolib/include/spatial_relation.h>

#include <maps/wikimap/mapspro/services/autocart/libs/geometry/include/hex_wkb.h>

namespace maps::wiki::autocart::tests {

Y_UNIT_TEST_SUITE(hex_wkb_tests)
{

    Y_UNIT_TEST(multipolygon_to_hex_wkb)
    {
        geolib3::MultiPolygon2 multiPolygon({
            geolib3::Polygon2({{0, 0}, {1, 0}, {1, 1}, {0, 1}}),
            geolib3::Polygon2({{2, 0}, {3, 0}, {3, 1}, {2, 1}})
        });

        std::string hexWKB = multiPolygonToHexWKB(multiPolygon);
        geolib3::MultiPolygon2 testMultiPolygon = hexWKBToMultiPolygon(hexWKB);

        EXPECT_TRUE(geolib3::spatialRelation(
            multiPolygon, testMultiPolygon,
            geolib3::SpatialRelation::Equals));
    }

} //Y_UNIT_TEST_SUITE(hex_wkb_tests)

} //namespace maps::wiki::autocart::tests


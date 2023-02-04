#include "../cut/cutter.h"
#include "../geom/internal.h"
#include "../geom/polygon.h"

#include <boost/test/unit_test.hpp>

#include <maps/libs/geolib/include/serialization.h>
#include <maps/libs/geolib/include/spatial_relation.h>

using namespace maps::coverage5;
using namespace maps;

void checkEquals(
    const geom::StandaloneMultiPolygon& multipolygon, const char* wkt)
{
    BOOST_CHECK(
        spatialRelation(
            *internal::internal2geolibGeometry(multipolygon, true),
            geolib3::WKT::read<geolib3::MultiPolygon2>(wkt),
            geolib3::Equals));
}

BOOST_AUTO_TEST_CASE(cutter_test)
{
    // This test checks the cutting algorithm in a special corner case when
    // cutting a polygon produces two border parts one of which starts exactly
    // at the same point where other ends. If not handled properly this case
    // can lead to an invalid result with self-intersecting polygons.

    // +--+  +----+
    // |  |  |    |
    // |  |  | +  | <---------- Cut line
    // |  |  |/|  |
    // |  |    |  |
    // |  +----+  |
    // |          |
    // +----------+

    auto polygon = geolib3::WKT::read<geolib3::Polygon2>(
        "POLYGON ((0 0, 3 0, 3 4, 1.5 4, 1.5 1.5, 2 2, 2 1, 1 1, 1 4, 0 4, 0 0))"
    );

    cut::Cutter cutter(cut::CutLine(geom::Coord::Y, 2));
    cut::PolygonalCut result =
        cutter.cut(*internal::geolib2internalGeometry(polygon));

    checkEquals(result.less, "MULTIPOLYGON (((1.5 2, 1.5 1.5, 2 2, 1.5 2)), \
        ((2 2, 2 1, 1 1, 1 2, 0 2, 0 0, 3 0, 3 2, 2 2)))");
    checkEquals(result.greater, "MULTIPOLYGON (((3 2, 3 4, 1.5 4, 1.5 2, 3 2)), \
        ((1 2, 1 4, 0 4, 0 2, 1 2)))");
}

BOOST_AUTO_TEST_CASE(internal_ring_touches_external_ring_test)
{
    // +-------+
    // |       |
    // |   +   |
    // |  / \  |
    // |  + +  | <----- Cut line
    // |  \ /  |
    // +---+---+

    auto polygon = geolib3::WKT::read<geolib3::Polygon2>(
        "POLYGON ((0 0, 2 0, 4 0, 4 4, 0 4, 0 0), (2 0, 1 1, 2 2, 3 1, 2 0))"
    );

    cut::Cutter cutter(cut::CutLine(geom::Coord::Y, 1));
    cut::PolygonalCut result =
        cutter.cut(*internal::geolib2internalGeometry(polygon));

    checkEquals(result.less, "MULTIPOLYGON (((0 0, 2 0, 1 1, 0 1, 0 0)), \
        ((2 0, 4 0, 4 1, 3 1, 2 0)))");
    checkEquals(result.greater, "MULTIPOLYGON (((0 1, 1 1, 2 2, 3 1, 4 1, 4 4, 0 4, 0 1)))");
}

BOOST_AUTO_TEST_CASE(triangle_cutter_test)
{
    // This test checks polygon cutting algorithm correctness in another
    // corner case, when only one polygon point is on a cut line.

    auto polygon = geolib3::WKT::read<geolib3::Polygon2>(
        "POLYGON ((0 0, 2 0, 1 1, 0 0))"
    );

    cut::Cutter cutter(cut::CutLine(geom::Coord::Y, 1));
    cut::PolygonalCut result =
        cutter.cut(*internal::geolib2internalGeometry(polygon));

    checkEquals(result.less, "MULTIPOLYGON (((0 0, 2 0, 1 1, 0 0)))");

    BOOST_CHECK(result.greater.area() == 0);
}


#include "common.h"
#include "../utils/geom.h"

#include <maps/libs/common/include/exception.h>
#include <maps/libs/geolib/include/point.h>
#include <maps/libs/geolib/include/polyline.h>
#include <maps/libs/geolib/include/polygon.h>
#include <maps/libs/geolib/include/spatial_relation.h>

#include <boost/test/unit_test.hpp>

#include <boost/optional.hpp>
#include <string>

namespace maps {
namespace wiki {
namespace groupedit {
namespace tests {

namespace gl = maps::geolib3;

namespace {

const std::string POINT_WKT = "POINT (1 2)";
const std::string POLYLINE_WKT = "LINESTRING (1 1, 1 2, 2 2, 2 1)";
const std::string POLYGON_WKT = "POLYGON ((0 5, 5 0, 0 -5, -5 0, 0 5))";

} // namespace

BOOST_AUTO_TEST_CASE( check_polygon_contains )
{
    auto polygon = geolib3::WKB::read<gl::Polygon2>(
        wkt2wkb(POLYGON_WKT));

    BOOST_CHECK(utils::contains(polygon, wkt2wkb(POINT_WKT)));
    BOOST_CHECK(utils::contains(polygon, wkt2wkb(POLYLINE_WKT)));
    BOOST_CHECK(utils::contains(polygon, wkt2wkb(
        "POLYGON ((-1 1, 1 1, 1 -1, -1 -1, -1 1))")));
    BOOST_CHECK(utils::contains(polygon, wkt2wkb(
        "POLYGON ((0 4, 4 0, 0 -4, -4 0, 0 4),"
            " (0 2, 2 0, 0 -2, -2 0, 0 2))")));

    BOOST_CHECK(!utils::contains(polygon, wkt2wkb("POINT (5 5)")));
    BOOST_CHECK(!utils::contains(polygon, wkt2wkb(
        "LINESTRING (0 0, 10 10)")));
    BOOST_CHECK(!utils::contains(polygon, wkt2wkb(
        "LINESTRING (0 10, 10 10)")));
    BOOST_CHECK(!utils::contains(polygon, wkt2wkb(
        "POLYGON ((9 10, 10 9, 9 8, 8 9, 9 10))")));
    BOOST_CHECK(!utils::contains(polygon, wkt2wkb(
        "POLYGON ((-10 10, 10 10, 10 -10, -10 -10, -10 10))")));
    BOOST_CHECK(!utils::contains(polygon, wkt2wkb(
        "POLYGON ((-10 10, 10 10, 10 -10, -10 -10, -10 10),"
            " (-6 6, 6 6, 6 -6, -6 -6, -6 6))")));
}

BOOST_AUTO_TEST_CASE( check_polygon_intersects )
{
    auto polygon = geolib3::WKB::read<gl::Polygon2>(
        wkt2wkb(POLYGON_WKT));

    BOOST_CHECK(utils::intersects(polygon, wkt2wkb(POINT_WKT)));
    BOOST_CHECK(utils::intersects(polygon, wkt2wkb(POLYLINE_WKT)));
    BOOST_CHECK(utils::intersects(polygon, wkt2wkb(
        "POLYGON ((-1 1, 1 1, 1 -1, -1 -1, -1 1))")));
    BOOST_CHECK(utils::intersects(polygon, wkt2wkb(
        "POLYGON ((0 4, 4 0, 0 -4, -4 0, 0 4),"
            " (0 2, 2 0, 0 -2, -2 0, 0 2))")));
    BOOST_CHECK(utils::intersects(polygon, wkt2wkb(
        "LINESTRING (0 0, 10 10)")));
    BOOST_CHECK(utils::intersects(polygon, wkt2wkb(
        "POLYGON ((-10 10, 10 10, 10 -10, -10 -10, -10 10))")));

    BOOST_CHECK(!utils::intersects(polygon, wkt2wkb("POINT (5 5)")));
    BOOST_CHECK(!utils::intersects(polygon, wkt2wkb(
        "LINESTRING (0 10, 10 10)")));
    BOOST_CHECK(!utils::intersects(polygon, wkt2wkb(
        "POLYGON ((9 10, 10 9, 9 8, 8 9, 9 10))")));
    BOOST_CHECK(!utils::intersects(polygon, wkt2wkb(
        "POLYGON ((-10 10, 10 10, 10 -10, -10 -10, -10 10),"
            " (-6 6, 6 6, 6 -6, -6 -6, -6 6))")));
}

BOOST_AUTO_TEST_CASE( check_polyline_intersects )
{
    auto polyline = geolib3::WKB::read<gl::Polyline2>(
        wkt2wkb(POLYLINE_WKT));

    BOOST_CHECK(utils::intersects(polyline, wkt2wkb(POINT_WKT)));
    BOOST_CHECK(utils::intersects(polyline, wkt2wkb(POLYGON_WKT)));
    BOOST_CHECK(utils::intersects(polyline, wkt2wkb("POINT (1.5 2)")));
    BOOST_CHECK(utils::intersects(polyline, wkt2wkb(
        "POLYGON ((0 -1, 3 -1, 3 3, 0 -1))")));
    BOOST_CHECK(utils::intersects(polyline, wkt2wkb(
        "LINESTRING (0 1, 3 1)")));
    BOOST_CHECK(utils::intersects(polyline, wkt2wkb(
        "POLYGON ((-1 -1, 3 -1, 3 4, -1 4, -1 -1),"
            " (0 0, 0 3, 2 3, 2 0, 0 0))")));

    BOOST_CHECK(!utils::intersects(polyline, wkt2wkb("POINT (1.5 1)")));
    BOOST_CHECK(!utils::intersects(polyline, wkt2wkb(
        "LINESTRING (0 0, 3 0)")));
    BOOST_CHECK(!utils::intersects(polyline, wkt2wkb(
        "POLYGON ((1 0, 2 0, 1.5 1.5, 1 0))")));
    BOOST_CHECK(!utils::intersects(polyline, wkt2wkb(
        "POLYGON ((-1 -1, 4 -1, 4 5, -1 5, -1 -1),"
            " (0 0, 1.5 4, 3 0, 0 0))")));
}

} // namespace tests
} // namespace groupedit
} // namespace wiki
} // namespace maps

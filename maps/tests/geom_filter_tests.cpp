#include "common.h"
#include "../geometry_filter.h"

#include <maps/libs/geolib/include/bounding_box.h>
#include <maps/libs/geolib/include/point.h>

#include <boost/test/unit_test.hpp>

#include <string>

namespace maps {
namespace wiki {
namespace groupedit {
namespace tests {

namespace {

const std::string POLYGON_AOI_WKT =
    "POLYGON ((0 0, 5 0, 5 5, 0 5, 0 0), (2 1, 1 3, 2 3, 3 2, 2 1))";
const std::string POLYLINE_AOI_WKT = "LINESTRING (0 0, 3 3)";

const std::string POINT_IN_WKT = "POINT (1 1)";
const std::string POINT_ISPL_WKT = "POINT (2 2)";
const std::string POINT_OUT_WKT = "POINT (-1 -1)";
const std::string POLYLINE_IN_WKT = "LINESTRING (2 4, 4 1, 5 1)";
const std::string POLYLINE_ISPG_WKT = "LINESTRING (0 4, 2 6)";
const std::string POLYLINE_OUT_WKT = "LINESTRING (-2 -2, -1 -1)";
const std::string POLYGON_IN_WKT =
    "POLYGON ((2 0, 4 2, 2 4, 0 2, 2 0), (2 1, 1 3, 2 3, 3 2, 2 1))";
const std::string POLYGON_ISPG_WKT = "POLYGON ((1 4, 4 4, 4 1, 6 6, 1 4))";
const std::string POLYGON_OUT_WKT =
    "POLYGON ((-2 -2, 7 -2, 7 7, -2 7, -2 -2), (-1 -1, -1 6, 6 6, 6 -1, -1 -1))";

} // namespace

BOOST_AUTO_TEST_CASE( check_default )
{
    GeometryFilter filter;

    BOOST_CHECK(!filter.boundingBox());

    BOOST_CHECK(filter.apply(std::nullopt));
    BOOST_CHECK(filter.apply(wkt2wkb(POINT_IN_WKT)));
    BOOST_CHECK(filter.apply(wkt2wkb(POINT_ISPL_WKT)));
    BOOST_CHECK(filter.apply(wkt2wkb(POINT_OUT_WKT)));
    BOOST_CHECK(filter.apply(wkt2wkb(POLYLINE_IN_WKT)));
    BOOST_CHECK(filter.apply(wkt2wkb(POLYLINE_ISPG_WKT)));
    BOOST_CHECK(filter.apply(wkt2wkb(POLYLINE_OUT_WKT)));
    BOOST_CHECK(filter.apply(wkt2wkb(POLYGON_IN_WKT)));
    BOOST_CHECK(filter.apply(wkt2wkb(POLYGON_ISPG_WKT)));
    BOOST_CHECK(filter.apply(wkt2wkb(POLYGON_OUT_WKT)));
}

BOOST_AUTO_TEST_CASE( check_within_polygon )
{
    GeometryFilter filter(
        GeomPredicate::Within,
        wkt2wkb(POLYGON_AOI_WKT));

    BOOST_REQUIRE(filter.boundingBox());
    BOOST_CHECK(*filter.boundingBox() ==
        geolib3::BoundingBox({0.0, 0.0}, {5.0, 5.0}));

    BOOST_CHECK(filter.apply(wkt2wkb(POINT_IN_WKT)));
    BOOST_CHECK(filter.apply(wkt2wkb(POLYLINE_IN_WKT)));
    BOOST_CHECK(filter.apply(wkt2wkb(POLYGON_IN_WKT)));

    BOOST_CHECK(!filter.apply(std::nullopt));
    BOOST_CHECK(!filter.apply(wkt2wkb(POINT_ISPL_WKT)));
    BOOST_CHECK(!filter.apply(wkt2wkb(POINT_OUT_WKT)));
    BOOST_CHECK(!filter.apply(wkt2wkb(POLYLINE_ISPG_WKT)));
    BOOST_CHECK(!filter.apply(wkt2wkb(POLYLINE_OUT_WKT)));
    BOOST_CHECK(!filter.apply(wkt2wkb(POLYGON_ISPG_WKT)));
    BOOST_CHECK(!filter.apply(wkt2wkb(POLYGON_OUT_WKT)));
}

BOOST_AUTO_TEST_CASE( check_intersects_polygon )
{
    GeometryFilter filter(
        GeomPredicate::Intersects,
        wkt2wkb(POLYGON_AOI_WKT));

    BOOST_REQUIRE(filter.boundingBox());
    BOOST_CHECK(*filter.boundingBox() ==
        geolib3::BoundingBox({0.0, 0.0}, {5.0, 5.0}));

    BOOST_CHECK(filter.apply(wkt2wkb(POINT_IN_WKT)));
    BOOST_CHECK(filter.apply(wkt2wkb(POLYLINE_IN_WKT)));
    BOOST_CHECK(filter.apply(wkt2wkb(POLYLINE_ISPG_WKT)));
    BOOST_CHECK(filter.apply(wkt2wkb(POLYGON_IN_WKT)));
    BOOST_CHECK(filter.apply(wkt2wkb(POLYGON_ISPG_WKT)));

    BOOST_CHECK(!filter.apply(std::nullopt));
    BOOST_CHECK(!filter.apply(wkt2wkb(POINT_ISPL_WKT)));
    BOOST_CHECK(!filter.apply(wkt2wkb(POINT_OUT_WKT)));
    BOOST_CHECK(!filter.apply(wkt2wkb(POLYLINE_OUT_WKT)));
    BOOST_CHECK(!filter.apply(wkt2wkb(POLYGON_OUT_WKT)));
}

BOOST_AUTO_TEST_CASE( check_intersects_polyline )
{
    GeometryFilter filter(
        GeomPredicate::Intersects,
        wkt2wkb(POLYLINE_AOI_WKT));

    BOOST_REQUIRE(filter.boundingBox());
    BOOST_CHECK(*filter.boundingBox() ==
        geolib3::BoundingBox({0.0, 0.0}, {3.0, 3.0}));

    BOOST_CHECK(filter.apply(wkt2wkb(POINT_IN_WKT)));
    BOOST_CHECK(filter.apply(wkt2wkb(POINT_ISPL_WKT)));
    BOOST_CHECK(filter.apply(wkt2wkb(POLYLINE_IN_WKT)));
    BOOST_CHECK(filter.apply(wkt2wkb(POLYGON_IN_WKT)));

    BOOST_CHECK(!filter.apply(std::nullopt));
    BOOST_CHECK(!filter.apply(wkt2wkb(POINT_OUT_WKT)));
    BOOST_CHECK(!filter.apply(wkt2wkb(POLYLINE_ISPG_WKT)));
    BOOST_CHECK(!filter.apply(wkt2wkb(POLYLINE_OUT_WKT)));
    BOOST_CHECK(!filter.apply(wkt2wkb(POLYGON_ISPG_WKT)));
    BOOST_CHECK(!filter.apply(wkt2wkb(POLYGON_OUT_WKT)));
}

} // namespace tests
} // namespace groupedit
} // namespace wiki
} // namespace maps

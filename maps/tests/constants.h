#pragma once

#include <maps/libs/geolib/include/multipolygon.h>
#include <maps/libs/geolib/include/point.h>
#include <maps/libs/geolib/include/polygon.h>
#include <maps/libs/common/include/hex.h>

#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>
#include <util/stream/output.h>


namespace maps::wikimap::feedback::pushes::addresses::prepare_dwellplaces::tests {

const std::string YMAPSDF_PATH = "//tmp/ymapsdf";
const std::string AD_GEOM_TABLE = YMAPSDF_PATH + "/" + AD_GEOM;
const std::string AD_NM_TABLE = YMAPSDF_PATH + "/" + AD_NM;
const std::string AD_TABLE = YMAPSDF_PATH + "/" + AD;

const std::string DWELLPLACES1 = "//tmp/dwellplaces/2020-08-01";
const std::string DWELLPLACES2 = "//tmp/dwellplaces/2020-08-02";
const std::string OUTPUT_PATH = "//tmp/output";

// geometry examples from https://ru.wikipedia.org/wiki/WKT#Well-known_text
const geolib3::Polygon2 POLYGON = geolib3::WKT::read<geolib3::Polygon2>(
    "POLYGON ((35 10, 45 45, 15 40, 10 20, 35 10), (20 30, 35 35, 30 20, 20 30))"
);
const std::string SHAPE1 = hexEncodeLowercase(
    geolib3::EWKB::toBytes<geolib3::SpatialReference::Epsg4326, geolib3::Polygon2>(
        POLYGON
    )
);
const geolib3::MultiPolygon2 MULTIPOLYGON = geolib3::WKT::read<geolib3::MultiPolygon2>(
    "MULTIPOLYGON (((40 40, 20 45, 45 30, 40 40)), "
    "((20 35, 10 30, 10 10, 30 5, 45 20, 20 35), "
    "(30 20, 20 15, 20 25, 30 20)))"
);
const std::string SHAPE2 = hexEncodeLowercase(
    geolib3::EWKB::toBytes<geolib3::SpatialReference::Epsg4326, geolib3::MultiPolygon2>(
        MULTIPOLYGON
    )
);
const std::string SHAPE3 = hexEncodeLowercase(
    geolib3::EWKB::toBytes<geolib3::SpatialReference::Epsg4326, geolib3::Point2>(
        geolib3::WKT::read<geolib3::Point2>("POINT (30 10)")
    )
);

} // namespace maps::wikimap::feedback::pushes::addresses::prepare_dwellplaces::tests

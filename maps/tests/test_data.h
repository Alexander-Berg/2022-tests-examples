#pragma once

#include <maps/libs/geolib/include/multipolygon.h>
#include <maps/libs/geolib/include/polygon.h>

#include <string>

namespace maps::factory::db::tests {

const auto TEST_MOSAIC_SOURCE_NAME = "just_test_mosaic_source";

const geolib3::MultiPolygon2 TEST_MOSAIC_SOURCE_GEOMETRY{{
    geolib3::Polygon2{geolib3::PointsVector{
        {0.0, 0.0},
        {2.0, 0.0},
        {2.0, 2.0},
        {0.0, 2.0},
        {0.0, 0.0}
    }}
}};

// Shorthand for WorldView-2
const auto TEST_MOSAIC_SOURCE_SATELLITE = "WV-2";

} // namespace maps::factory::db::tests

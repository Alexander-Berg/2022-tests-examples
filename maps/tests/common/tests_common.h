#pragma once

#include <maps/garden/modules/carparks/place_shields/lib/bounding_box.h>
#include <maps/garden/modules/carparks/place_shields/lib/common.h>

#include <maps/libs/geolib/include/polyline.h>
#include <maps/libs/geolib/include/test_tools/io_operations.h>

#include <maps/libs/common/include/hex.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>

#include <boost/optional/optional_io.hpp>
#include <string>


// Helper functions to enable BOOST_CHECK_EQUAL for polyline etc.

namespace maps::geolib3 {

using geolib3::io::operator<<;

} // namespace maps::geolib3

namespace maps::carparks::place_shields {

using maps::geolib3::EWKB;

template <typename Geometry>
std::string ewkbString(Geometry ewkbGeom)
{
    return maps::hexEncodeUppercase(
        EWKB::toBytes<SpatialReference::Epsg4326>(ewkbGeom));
}

bool operator==(const BoundingBox& a, const BoundingBox& b);
std::ostream& operator<<(std::ostream& stream, BoundingBox bbox);

bool operator==(const Carpark& a, const Carpark& b);
std::ostream& operator<<(std::ostream& stream, const Carpark& b);

bool operator==(const CarparkTiedToStreet& a, const CarparkTiedToStreet& b);
std::ostream& operator<<(std::ostream& stream, const CarparkTiedToStreet& b);

bool operator==(const Street& a, const Street& b);
std::ostream& operator<<(std::ostream& stream, const Street& street);

bool operator==(const LineWithShield& a, const LineWithShield& b);
std::ostream& operator<<(std::ostream& stream, const LineWithShield& line);

/// helper function to shorten polylines hard-coding in tests
maps::geolib3::Polyline2 makePolyline(
    const std::vector<maps::geolib3::Point2>& points);

} // namespace maps::carparks::place_shields

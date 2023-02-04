#include "tests_common.h"

#include <maps/libs/geolib/include/test_tools/comparison.h>
#include <boost/format.hpp>

#include <cmath>


// This is a relative value
const double EPS = 1e-4;

namespace maps::geolib3 {

bool equal(const Point2& a, const Point2& b)
{
    if (std::isnan(a.x()) && std::isnan(a.y())
            && std::isnan(b.x()) && std::isnan(b.y())) {
        return true;
    }
    return test_tools::approximateEqual(a, b, ::EPS);
}

} // namespace maps::geolib3

namespace maps::carparks::place_shields {

bool operator==(const BoundingBox& a, const BoundingBox& b)
{
    return geolib3::test_tools::approximateEqual(
                a.lowerCorner(), b.lowerCorner(), ::EPS)
        && geolib3::test_tools::approximateEqual(
                b.upperCorner(), b.upperCorner(), ::EPS);
}

std::ostream& operator<<(std::ostream& stream, const BoundingBox bbox)
{
    stream << "{" << bbox.minX()
           << "," << bbox.minY()
           << "," << bbox.maxX()
           << "," << bbox.maxY()
           << "}";
    return stream;
}

bool operator==(const Carpark& a, const Carpark& b)
{
    return a.bbox == b.bbox
           //&& a.id == b.id  // do not check id as it is order-dependend
           && a.line == b.line
           && a.info == b.info
           && a.zone == b.zone
           && a.shieldRadius == b.shieldRadius;
}

std::ostream& operator<<(std::ostream& stream, const Carpark& b)
{
    stream << "{"
        << "bbox=" << b.bbox
        << " id=" << b.id
        << " line=" << b.line
        << " info=" << b.info
        << " zone=" << b.zone
        << " shieldRadius=" << b.shieldRadius
        << "}";
    return stream;
}

bool operator==(const CarparkTiedToStreet& a, const CarparkTiedToStreet& b)
{
    return static_cast<Carpark>(a) == static_cast<Carpark>(b)
           && a.streetId == b.streetId
           && a.streetSide == b.streetSide;
}

std::ostream& operator<<(std::ostream& stream, const CarparkTiedToStreet& b)
{
    stream << "{"
           << static_cast<Carpark>(b)
           << " streetId=" << b.streetId
           << " streetSide=" << (int)b.streetSide
           << "}";
    return stream;
}

bool operator==(const Street& a, const Street& b)
{
    return a.bbox == b.bbox
        && a.line == b.line
        // && a.id == b.id  // do not check id as it is order-dependend
        && a.originalId == b.originalId
        && a.priority == b.priority;
}

std::ostream& operator<<(std::ostream& stream, const Street& street)
{
    stream << "{"
           << "bbox=" << street.bbox
           << " line=" << street.line
           << " id=" << street.id
           << " originalId=" << street.originalId
           << " priority=" << street.priority
           << "}";
    return stream;
}

bool operator==(const LineWithShield& a, const LineWithShield& b)
{
    return  //do not check bbox as it is dynamically set inside tested code
        a.line == b.line
        && a.info == b.info
        && a.streetOriginalId == b.streetOriginalId
        && a.shieldRadius == b.shieldRadius
        && equal(a.shield, b.shield)
        && a.shieldType == b.shieldType;
}

std::ostream& operator<<(std::ostream& stream, const LineWithShield& line)
{
    stream << "{"
           << "bbox=" << line.bbox
           << " line=" << line.line
           << " info=" << line.info
           << " streetOriginalId=" << line.streetOriginalId
           << " shieldRadius=" << line.shieldRadius
           << " shield=" << line.shield
           << " shieldType=" << static_cast<int>(line.shieldType)
           << "}";
    return stream;
}

maps::geolib3::Polyline2 makePolyline(
    const std::vector<maps::geolib3::Point2>& points)
{
    return maps::geolib3::Polyline2(points);
}

} // namespace maps::carparks::place_shields

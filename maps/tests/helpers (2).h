#pragma once

#include "../cut/cut_line.h"

#include <yandex/maps/wiki/geom_tools/common.h>

#include <maps/libs/geolib/include/spatial_relation.h>
#include <maps/libs/geolib/include/serialization.h>

#include <string>

namespace maps {
namespace wiki {
namespace geom_tools {
namespace test {

void
checkPolygons(
    const std::string& message,
    const GeolibPolygonVector& expected, const GeolibPolygonVector& received);

template <class Transform>
geolib3::LinearRing2
transform(const geolib3::LinearRing2& ring, const Transform& trf)
{
    geolib3::PointsVector points;
    points.reserve(ring.pointsNumber());
    for (size_t i = 0; i < ring.pointsNumber(); ++i) {
        points.push_back(trf(ring.pointAt(i)));
    }
    return geolib3::LinearRing2(points);
}

template <class Transform>
geolib3::Polygon2
transform(const geolib3::Polygon2& polygon, const Transform& trf)
{
    std::vector<geolib3::LinearRing2> holes;
    holes.reserve(polygon.interiorRingsNumber());
    for (size_t i = 0; i < polygon.interiorRingsNumber(); ++i) {
        holes.push_back(transform(polygon.interiorRingAt(i), trf));
    }
    return geolib3::Polygon2(transform(polygon.exteriorRing(), trf), holes, false);
}

template <class Transform>
GeolibPolygonVector
transform(const GeolibPolygonVector& polygons, const Transform& trf)
{
    GeolibPolygonVector result;
    result.reserve(polygons.size());
    for (const auto& poly : polygons) {
        result.push_back(transform(poly, trf));
    }
    return result;
}

class TransposeTransform {
public:
    geolib3::Point2 operator () (const geolib3::Point2& p) const
    { return geolib3::Point2(p.y(), p.x()); }
};

class ReflectTransform {
public:
    explicit ReflectTransform(const CutLine& cutLine)
        : cutLine_(cutLine)
    {}

    geolib3::Point2 operator () (const geolib3::Point2& p) const
    {
        return cutLine_.direction() == Direction::X
            ? geolib3::Point2(2 * cutLine_.coord() - p.x(), p.y())
            : geolib3::Point2(p.x(), 2 * cutLine_.coord() - p.y());
    }

private:
    CutLine cutLine_;
};

} // namespace test
} // namespace geom_tools
} // namespace wiki
} // namespace maps

#include "common.h"

#include <maps/renderer/libs/base/include/geom/vertices.h>
#include <maps/libs/geolib/include/conversion.h>

namespace maps::carparks::dump {

namespace {
bool equals(const std::vector<maps::renderer::base::Vertex>& points,
            const std::vector<geolib3::Point2>& expected)
{
    return points.size() == expected.size()
           && std::equal(points.begin(), points.end(), expected.begin(),
                         [](maps::renderer::base::Vertex pt1, geolib3::Point2 pt2) {
                             auto expectedMercator = geolib3::geoPoint2Mercator(pt2);
                             // for some reason, geometry loses much of precision
                             // however, the coordinate system here is Web Mercator
                             // i.e. typical coordinates are on the order of 1e7,
                             // and coordinate difference of 1e-1 is less than 10 centimeters in reality
                             static const double EPS = 1e-1;
                             return fabs(pt1.x - expectedMercator.x()) < EPS
                                    && fabs(pt1.y - expectedMercator.y()) < EPS;
                         });
}
} // namespace

void run(const std::string& command)
{
    std::cout << "Executing command " + command << std::endl;
    int status = std::system(command.c_str());
    if (status != 0) {
        throw std::runtime_error("Non-zero exit code");
    }
}

bool featureGeometryEqualToExpected(
    const ft::Feature& feature,
    ft::FeatureType featureType,
    const std::vector<geolib3::Point2>& expected)
{
    switch (featureType) {
    case ft::FeatureType::Point: {
        auto point = feature.geom().shapes().front();
        return equals({point}, expected);
    }
    case ft::FeatureType::Polygon: {
        return equals(feature.geom().shapes(), expected);
    }
    case ft::FeatureType::Polyline: {
        return equals(feature.geom().contours(), expected);
    }
    default: {
        throw LogicError() << "Unexpected featureType";
    }
    } // switch (feature)
}

} // namespace maps::carparks::dump

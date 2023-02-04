#include "polygon_builder_formatter.h"

#include <yandex/maps/geotex/common.h>
#include <yandex/maps/wiki/test_tools/doc/conversion.h>

namespace maps {
namespace wiki {
namespace geom_tools {
namespace test {
namespace {

geolib3::Polyline2 toPolyline(const geolib3::LinearRing2& ring)
{
    geolib3::PointsVector points;
    const auto pointsNumber = ring.pointsNumber();
    points.reserve(pointsNumber + 1);
    for (size_t i = 0; i != pointsNumber; ++i) {
        points.push_back(ring.pointAt(i));
    }
    points.push_back(ring.pointAt(0));
    return geolib3::Polyline2(points);
}

} // namespace

test_tools::doc::LayerPtrList
PolygonBuilderTestFormatter::geomLayersBefore(const TestType& test) const
{
    test_tools::doc::LayerPtrList res;
    for (size_t i = 0; i != test.shells.size(); ++i) {
        auto layer = test_tools::doc::toLayer
            ( "exterior " + std::to_string(i)
            , "green"
            , test_tools::doc::ShowLineDirection::Show
            );
        layer->append(std::make_shared<geotex::Polyline>(toPolyline(test.shells[i])));
        res.push_back(layer);
    }
    for (size_t i = 0; i != test.holes.size(); ++i) {
        auto layer = test_tools::doc::toLayer
            ( "interior " + std::to_string(i)
            , "red"
            , test_tools::doc::ShowLineDirection::Show
            );
        layer->append(std::make_shared<geotex::Polyline>(toPolyline(test.holes[i])));
        res.push_back(layer);
    }
    return res;
}

test_tools::doc::LayerPtrList
PolygonBuilderTestFormatter::geomLayersAfter(const TestType& test) const
{
    test_tools::doc::LayerPtrList res;
    if (test.expectedPolygons) {
        for (size_t i = 0; i != test.expectedPolygons->size(); ++i) {
            res.splice(res.end(), test_tools::doc::toLayers
                ( test.expectedPolygons->at(i)
                , "expected " + std::to_string(i)
                , "orange"
                ));
        }
    }
    return res;
}

} // namespace test
} // namespace geom_tools
} // namespace wiki
} // namespace maps

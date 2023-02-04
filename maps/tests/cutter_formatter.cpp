#include "cutter_formatter.h"

#include <maps/libs/geolib/include/intersection.h>
#include <yandex/maps/geotex/common.h>
#include <yandex/maps/wiki/test_tools/doc/conversion.h>

namespace maps {
namespace wiki {
namespace geom_tools {
namespace test {

test_tools::doc::LayerPtrList
PolygonCutterTestFormatter::geomLayersBefore(const TestType& test) const
{
    test_tools::doc::LayerPtrList res;
    res.splice(res.end(), test_tools::doc::toLayers(test.polygon, "input", "green"));

    auto cutLine = test_tools::doc::toLayer
        ("cut line", "red", test_tools::doc::ShowLineDirection::Hide);
    auto points = geolib3::intersection
        ( test.cutLine.line()
        , test.polygon.boundingBox()
        );
    switch (points.size()) {
    case 1:
        cutLine->append(std::make_shared<geotex::Point>(points.front()));
        break;
    case 2: {
            geolib3::Segment2 segment(points.front(), points.back());
            cutLine->append(std::make_shared<geotex::Segment>(segment));
        }
        break;
    default:
        throw LogicError("invalid intersection");
    }
    res.push_back(cutLine);
    return res;
}

test_tools::doc::LayerPtrList
PolygonCutterTestFormatter::geomLayersAfter(const TestType& test) const
{
    test_tools::doc::LayerPtrList res;
    for (size_t i = 0; i != test.expectedResult.less.size(); ++i) {
        res.splice(res.end(), test_tools::doc::toLayers
            ( test.expectedResult.less[i]
            , "less " + std::to_string(i)
            , "yellow"
            ));
    }
    for (size_t i = 0; i != test.expectedResult.greater.size(); ++i) {
        res.splice(res.end(), test_tools::doc::toLayers
            ( test.expectedResult.greater[i]
            , "greater " + std::to_string(i)
            , "orange"
            ));
    }
    return res;
}

} // namespace test
} // namespace geom_tools
} // namespace wiki
} // namespace maps

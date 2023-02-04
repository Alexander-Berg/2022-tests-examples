#include "partitioner_formatter.h"

#include <maps/libs/geolib/include/intersection.h>
#include <yandex/maps/geotex/common.h>
#include <yandex/maps/wiki/test_tools/doc/conversion.h>

namespace maps {
namespace wiki {
namespace geom_tools {
namespace test {

test_tools::doc::LayerPtrList
PolygonPartitionTestFormatter::geomLayersBefore(const TestType& test) const
{
    test_tools::doc::LayerPtrList res;
    for (size_t i = 0; i != test.polygons.size(); ++i) {
        res.splice(res.end(), test_tools::doc::toLayers
            ( test.polygons[i]
            , "input " + std::to_string(i)
            , "green"
            ));
    }
    return res;
}

test_tools::doc::LayerPtrList
PolygonPartitionTestFormatter::geomLayersAfter(const TestType& test) const
{
    test_tools::doc::LayerPtrList res;
    for (size_t i = 0; i != test.expected.size(); ++i) {
        res.splice(res.end(), test_tools::doc::toLayers
            ( test.expected[i]
            , "output " + std::to_string(i)
            , "red"
            ));
    }
    return res;
}

} // namespace test
} // namespace geom_tools
} // namespace wiki
} // namespace maps

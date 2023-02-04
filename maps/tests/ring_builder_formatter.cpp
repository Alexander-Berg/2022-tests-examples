#include "ring_builder_formatter.h"

#include <yandex/maps/geotex/common.h>
#include <yandex/maps/wiki/test_tools/doc/conversion.h>

namespace maps {
namespace wiki {
namespace geom_tools {
namespace test {

test_tools::doc::LayerPtrList
RingBuilderTestFormatter::geomLayersBefore(const TestType& test) const
{
    test_tools::doc::LayerPtrList res;
    for (size_t i = 0; i != test.geoms.size(); ++i) {
        const auto& geom = test.geoms[i];
        auto layer = test_tools::doc::toLayer
            ( "input " + std::to_string(i)
            , "green"
            , test_tools::doc::ShowLineDirection::Show
            );
        layer->append(std::make_shared<geotex::Point>(geom.points().front()));
        layer->append(std::make_shared<geotex::Polyline>(geom));
        layer->append(std::make_shared<geotex::Point>(geom.points().back()));
        res.push_back(layer);
    }
    return res;
}

test_tools::doc::LayerPtrList
RingBuilderTestFormatter::geomLayersAfter(const TestType& test) const
{
    test_tools::doc::LayerPtrList res;
    if (test.expectedRing) {
        res.push_back(test_tools::doc::toLayer(*test.expectedRing, "ring", "red"));
    }
    return res;
}

} // namespace test
} // namespace geom_tools
} // namespace wiki
} // namespace maps

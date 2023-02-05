#pragma once

#include "gen_doc/test_formatter.h"
#include "test_types/save_edge_test_data.h"

#include <yandex/maps/geotex/document.h>

namespace maps {
namespace wiki {
namespace topo {
namespace doc {

class SaveEdgeTestFormatter : public TestFormatter<test::SaveEdgeTestData> {
public:
    typedef test::SaveEdgeTestData TestType;

    explicit SaveEdgeTestFormatter(geotex::Document& document)
        : TestFormatter(document)
    {}

protected:

    virtual LayerPtrList geomLayersBefore(const TestType& testData) const;
    virtual LayerPtrList geomLayersAfter(const TestType& testData) const;

    virtual LayerPtrList highlightLayersBefore(const TestType& testData) const;

    std::list<geolib3::BoundingBox>
    customGeomViewRects(const TestType& testData) const;
};

} // namespace doc
} // namespace topo
} // namespace wiki
} // namespace maps

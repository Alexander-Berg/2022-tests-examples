#pragma once

#include "gen_doc/test_formatter.h"
#include "test_types/snap_nodes_test_data.h"

#include <yandex/maps/geotex/document.h>

namespace maps {
namespace wiki {
namespace topo {
namespace doc {

class SnapNodesTestFormatter : public TestFormatter<test::SnapNodesTestData> {
public:
    typedef test::SnapNodesTestData TestType;

    explicit SnapNodesTestFormatter(geotex::Document& document)
        : TestFormatter(document)
    {}

protected:

    virtual LayerPtrList geomLayersBefore(const TestType& testData) const;
    virtual LayerPtrList geomLayersAfter(const TestType& testData) const;
};

} // namespace doc
} // namespace topo
} // namespace wiki
} // namespace maps

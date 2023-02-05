#include "snap_nodes_test_formatter.h"

#include "gen_doc/geotex_conversion.h"
#include "gen_doc/styles.h"
#include "gen_doc/layer_builder.h"
#include "gen_doc/common_layers_helpers.h"

#include "test_tools/storage_diff_helpers.h"

namespace maps {
namespace wiki {
namespace topo {
namespace doc {

SnapNodesTestFormatter::LayerPtrList
SnapNodesTestFormatter::geomLayersBefore(const TestType& test) const
{
    std::list<geotex::LayerPtr> layers;

    NodeIDSet affectedNodes = test.nodeIds();

    GeomGeotexConvertor objectPrinter;
    LayerBuilder<GeomGeotexConvertor> builder(objectPrinter, layers);

    builder.add(
        {"all edges before", style::ALL_EDGES},
        test::MockStorageEdgeRangeAdaptor(test.original()));

    builder.add(
        {"all nodes before", style::ALL_NODES},
        test::MockStorageNodeRangeAdaptor(test.original()),
        [&affectedNodes] (const test::Node& node) { return !affectedNodes.count(node.id); });

    builder.add(
        {"affected nodes before", style::AFFECTED_NODES_BEFORE},
        test::MockStorageNodeRangeAdaptor(test.original()),
        [&affectedNodes] (const test::Node& node) { return affectedNodes.count(node.id); });

    return layers;
}

SnapNodesTestFormatter::LayerPtrList
SnapNodesTestFormatter::geomLayersAfter(const TestType& test)
const
{
    std::list<geotex::LayerPtr> layers;

    GeomGeotexConvertor objectPrinter;
    LayerBuilder<GeomGeotexConvertor> builder(objectPrinter, layers);

    if (test.expectedError()) {
        builder.add(
            {"all edges after", style::ALL_EDGES},
            test::MockStorageEdgeRangeAdaptor(test.original()));
        builder.add(
            {"all nodes after", style::ALL_NODES},
            test::MockStorageNodeRangeAdaptor(test.original()));

        return layers;
    }

    test::EdgesDiffData edgesDiff = {
        {{}, {}, {}},
        test::edgesDiff(test.original(), test.result())
    };

    buildCommonResultEdgesLayers(test.result(), builder, edgesDiff);

    // nodes

    test::NodesDiff nodesDiff = test::nodesDiff(test.original(), test.result());

    buildCommonResultNodesLayers(test.result(), builder, nodesDiff);

    return layers;
}

} // namespace doc
} // namespace topo
} // namespace wiki
} // namespace maps

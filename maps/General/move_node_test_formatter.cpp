#include "gen_doc/move_node_test_formatter.h"

#include "gen_doc/geotex_conversion.h"
#include "gen_doc/styles.h"
#include "gen_doc/layer_builder.h"
#include "gen_doc/common_layers_helpers.h"

#include "test_tools/storage_diff_helpers.h"

namespace maps {
namespace wiki {
namespace topo {
namespace doc {

MoveNodeTestFormatter::LayerPtrList
MoveNodeTestFormatter::geomLayersBefore(const TestType& test)
    const
{
    std::list<geotex::LayerPtr> layers;

    GeomGeotexConvertor objectPrinter;
    LayerBuilder<GeomGeotexConvertor> builder(objectPrinter, layers);

    auto storageEdgeAdaptor = test::MockStorageEdgeRangeAdaptor(test.original());

    builder.add(
        {"all edges before", style::ALL_EDGES},
        storageEdgeAdaptor,
        [&] (const test::Edge& edge)
        {
            return edge.start != test.nodeId() && edge.end != test.nodeId();
        });

    builder.add(
        {"affected edges before", style::EDITED_EDGES_BEFORE},
        storageEdgeAdaptor,
        [&] (const test::Edge& edge)
        {
            return edge.start == test.nodeId() || edge.end == test.nodeId();
        });

    auto storageNodeAdaptor = test::MockStorageNodeRangeAdaptor(test.original());

    builder.add(
        {"all nodes before", style::ALL_NODES},
        storageNodeAdaptor,
        [&] (const test::Node& n) { return n.id != test.nodeId(); });

    builder.add(
        {"moved node before", style::MOVED_NOVE},
        storageNodeAdaptor,
        [&] (const test::Node& n) { return n.id == test.nodeId(); });
    builder.add(
        {"moved node before highlight", style::MOVED_NODE_HIGHLIGHT_BEFORE},
        storageNodeAdaptor,
        [&] (const test::Node& n) { return n.id == test.nodeId(); });

    builder.add(
        {"moved node new pos", style::NEW_NODE_POS},
        geolib3::PointsVector{test.pos()});

    return layers;
}

MoveNodeTestFormatter::LayerPtrList
MoveNodeTestFormatter::geomLayersAfter(const TestType& test)
    const
{
    std::list<geotex::LayerPtr> layers;

    GeomGeotexConvertor objectPrinter;
    LayerBuilder<GeomGeotexConvertor> builder(objectPrinter, layers);

    if (test.expectedError() || test.type() == TestType::Incorrect) {
        builder.add(
            {"all edges after", style::ALL_EDGES},
            test::MockStorageEdgeRangeAdaptor(test.original()));
        builder.add(
            {"all nodes after", style::ALL_NODES},
            test::MockStorageNodeRangeAdaptor(test.original()));

        return layers;
    }

    auto edgesDiff = test::edgesDiffDataByNode(
        test.nodeId(), test.splitEdges(), test.original(), test.result());

    buildCommonResultEdgesLayers(test.result(), builder, edgesDiff);

    // nodes

    test::NodesDiff nodesDiff = test::nodesDiff(test.original(), test.result());

    buildCommonResultNodesLayers(test.result(), builder, nodesDiff);

    builder.add(
        {"moved node after highlight", style::MOVED_NODE_HIGHLIGHT_AFTER},
        test::MockStorageNodeRangeAdaptor(test.result()),
        [&] (const test::Node& n) { return n.id == test.nodeId(); });

    return layers;
}

MoveNodeTestFormatter::LayerPtrList
MoveNodeTestFormatter::highlightLayersBefore(const TestType& test)
    const
{
    if (!test.printInfo()) {
        return {};
    }

    return highlightLayers(
        *test.printInfo(), test.restrictions(), test.original(), nullptr);
}

std::list<geolib3::BoundingBox>
MoveNodeTestFormatter::customGeomViewRects(
    const TestType& testData) const
{
    return doc::customGeomViewRects(
        testData, testData.restrictions().junctionGravity());
}
} // namespace doc
} // namespace topo
} // namespace wiki
} // namespace maps

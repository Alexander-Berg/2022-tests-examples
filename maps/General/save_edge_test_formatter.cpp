#include "save_edge_test_formatter.h"

#include "gen_doc/geotex_conversion.h"
#include "gen_doc/styles.h"
#include "gen_doc/layer_builder.h"
#include "gen_doc/common_layers_helpers.h"

#include "test_tools/storage_diff_helpers.h"

namespace maps {
namespace wiki {
namespace topo {
namespace doc {

SaveEdgeTestFormatter::LayerPtrList
SaveEdgeTestFormatter::geomLayersBefore(const TestType& test)
    const
{
    std::list<geotex::LayerPtr> layers;

    const SourceEdgeID sourceId = test.edgeId();
    NodeIDSet affectedNodes =
        test::nodesWithChangedIncidences(test.original(), test.result());

    GeomGeotexConvertor objectPrinter;
    LayerBuilder<GeomGeotexConvertor> builder(objectPrinter, layers);

    builder.add(
        {"all edges before", style::ALL_EDGES},
        test::MockStorageEdgeRangeAdaptor(test.original()),
        [&] (const test::Edge& edge) { return !sourceId.exists() || sourceId.id() != edge.id; });

    builder.add(
        {"edited edge before", style::EDITED_EDGES_BEFORE},
        test::MockStorageEdgeRangeAdaptor(test.original()),
        [&] (const test::Edge& edge) { return sourceId.exists() && sourceId.id() == edge.id; });

    builder.add(
        {"all nodes before", style::ALL_NODES},
        test::MockStorageNodeRangeAdaptor(test.original()),
        [&affectedNodes] (const test::Node& node) { return !affectedNodes.count(node.id); });

    builder.add(
        {"affected nodes before", style::AFFECTED_NODES_BEFORE},
        test::MockStorageNodeRangeAdaptor(test.original()),
        [&affectedNodes] (const test::Node& node) { return affectedNodes.count(node.id); });

    builder.add(
        {"new geom", style::NEW_GEOM},
        geolib3::PolylinesVector{test.newPolyline()});

    builder.add(
        {"new geom endpoints", style::NEW_EDGE_ENDPOINTS},
        geolib3::PointsVector{test.newPolyline().points().front(), test.newPolyline().points().back()});

    builder.add(
        {"requested split points", style::REQUESTED_SPLIT_POINTS},
        test.splitPoints());

    return layers;
}

SaveEdgeTestFormatter::LayerPtrList
SaveEdgeTestFormatter::geomLayersAfter(const TestType& test)
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

    auto edgesDiff = test::edgesDiffDataByEdge(
        test.edgeId(), test.splitEdges(), test.original(), test.result());

    buildCommonResultEdgesLayers(test.result(), builder, edgesDiff);

    // nodes

    test::NodesDiff nodesDiff = test::nodesDiff(test.original(), test.result());

    buildCommonResultNodesLayers(test.result(), builder, nodesDiff);

    return layers;
}

SaveEdgeTestFormatter::LayerPtrList
SaveEdgeTestFormatter::highlightLayersBefore(const TestType& test)
    const
{
    if (!test.printInfo()) {
        return {};
    }

    return doc::highlightLayers(
        *test.printInfo(), test.restrictions(), test.original(), &test.newPolyline());
}

std::list<geolib3::BoundingBox>
SaveEdgeTestFormatter::customGeomViewRects(
    const TestType& testData) const
{
    return doc::customGeomViewRects(
        testData, testData.restrictions().junctionGravity());
}

} // namespace doc
} // namespace topo
} // namespace wiki
} // namespace maps

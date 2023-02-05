#include "face_validation_test_formatter.h"

#include "gen_doc/geotex_conversion.h"
#include "gen_doc/styles.h"
#include "gen_doc/layer_builder.h"
#include "gen_doc/common_layers_helpers.h"

#include "test_tools/storage_diff_helpers.h"

namespace maps {
namespace wiki {
namespace topo {
namespace doc {

FaceValidationTestFormatter::LayerPtrList
FaceValidationTestFormatter::geomLayersBefore(const TestType& test)
    const
{
    std::list<geotex::LayerPtr> layers;

    GeomGeotexConvertor objectPrinter;
    LayerBuilder<GeomGeotexConvertor> builder(objectPrinter, layers);

    auto storageEdgesAdaptor = test::MockStorageEdgeRangeAdaptor(test.original());
    auto storageNodesAdaptor = test::MockStorageNodeRangeAdaptor(test.original());

    if (!test.original().faceExists(test.faceId())) {
        builder.add({"all edges", style::ALL_EDGES}, storageEdgesAdaptor);
        builder.add({"all nodes", style::ALL_NODES}, storageNodesAdaptor);
        return layers;
    }

    const test::Face& face = test.original().testFace(test.faceId());

    builder.add(
        {"all edges", style::ALL_EDGES},
        storageEdgesAdaptor,
        [&face] (const test::Edge& e) { return !face.edgeIds.count(e.id); });

    builder.add(
        {"face edges", style::FACE_EDGES_BEFORE},
        storageEdgesAdaptor,
        [&face] (const test::Edge& e) { return face.edgeIds.count(e.id); });

    builder.add({"all nodes", style::ALL_NODES}, storageNodesAdaptor);

    return layers;
}

FaceValidationTestFormatter::LayerPtrList
FaceValidationTestFormatter::geomLayersAfter(const TestType& test)
    const
{
    std::list<geotex::LayerPtr> layers;

    GeomGeotexConvertor objectPrinter;
    LayerBuilder<GeomGeotexConvertor> builder(objectPrinter, layers);

    auto storageEdgesAdaptor = test::MockStorageEdgeRangeAdaptor(test.result());

    auto resultCopy = test.result();
    resultCopy.setOriginal(&test.original());

    const test::Face& face = resultCopy.testFace(test.faceId());
    auto faceDiff = resultCopy.faceDiff(test.faceId());

    builder.add(
        {"all edges", style::ALL_EDGES},
        storageEdgesAdaptor,
        [&faceDiff, face] (const test::Edge& e)
        {
            return !face.edgeIds.count(e.id) &&
                !faceDiff.added.count(e.id) &&
                !faceDiff.changed.count(e.id) &&
                !faceDiff.removed.count(e.id);
        });

    builder.add(
        {"unaffected face edges", style::FACE_EDGES_BEFORE},
        storageEdgesAdaptor,
        [&faceDiff, face] (const test::Edge& e)
        {
            return face.edgeIds.count(e.id) &&
                !faceDiff.added.count(e.id) &&
                !faceDiff.changed.count(e.id) &&
                !faceDiff.removed.count(e.id);
        });

    builder.add(
        {"added edges", style::ADDED_FACE_EDGES},
        storageEdgesAdaptor,
        [&faceDiff] (const test::Edge& e) { return faceDiff.added.count(e.id); });

    builder.add(
        {"changed edges", style::CHANGED_FACE_EDGES},
        storageEdgesAdaptor,
        [&faceDiff] (const test::Edge& e) { return faceDiff.changed.count(e.id); });

    builder.add(
        {"removed edges", style::REMOVED_FACE_EDGES},
        storageEdgesAdaptor,
        [&faceDiff] (const test::Edge& e) { return faceDiff.removed.count(e.id); });

    // nodes

    test::NodesDiff nodesDiff = test::nodesDiff(test.original(), test.result());

    buildCommonResultNodesLayers(test.result(), builder, nodesDiff);

    return layers;
}

} // namespace doc
} // namespace topo
} // namespace wiki
} // namespace maps

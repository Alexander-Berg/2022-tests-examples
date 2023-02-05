#pragma once

#include "../test_types/common.h"
#include "../test_types/mock_storage.h"

namespace maps {
namespace wiki {
namespace topo {
namespace test {

// edges

EdgeIDSet
addedEdges(const MockStorage& original, const MockStorage& result);

EdgeIDSet
movedEdges(const MockStorage& original, const MockStorage& result);

EdgeIDSet
deletedEdges(const MockStorage& original, const MockStorage& result);

EdgeIDSet
edgesWithModifiedGeom(const MockStorage& original, const MockStorage& result);

EdgeIDSet
allAffectedEdgeIds(const MockStorage& original, const MockStorage& result);


struct EdgesDiff
{
    EdgeIDSet newIds;
    EdgeIDSet changedGeomIds;
    EdgeIDSet movedIds;
};

struct EdgesDiffData
{
    // diff for edges explicitly saved
    EdgesDiff editedEdgesDiff;
    // diff for edges implicitly splitted as the result of other edges editing
    EdgesDiff affectedEdgesDiff;
};

EdgesDiffData
edgesDiffDataByEdge(
    SourceEdgeID id,
    const SplitEdges& splitEdges,
    const MockStorage& original,
    const MockStorage& result);

EdgesDiffData
edgesDiffDataByNode(
    NodeID id,
    const SplitEdges& splitEdges,
    const MockStorage& original,
    const MockStorage& result);

inline EdgesDiff
edgesDiff(const MockStorage& original, const MockStorage& result)
{
    return {
        addedEdges(original, result),
        edgesWithModifiedGeom(original, result),
        deletedEdges(original, result)
    };
}

// nodes

NodeIDSet
addedNodes(const MockStorage& original, const MockStorage& result);

NodeIDSet
nodesWithChangedIncidences(const MockStorage& original, const MockStorage& result);

NodeIDSet
nodesWithChangedGeom(const MockStorage& original, const MockStorage& result);

struct NodesDiff
{
    NodeIDSet newIds;
    NodeIDSet changedGeomIds;
    NodeIDSet changedIncidencesIds;
};

NodesDiff
nodesDiff(const MockStorage& original, const MockStorage& result);

} // namespace test
} // namespace topo
} // namespace wiki
} // namespace maps

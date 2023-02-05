#include "storage_diff_helpers.h"

#include "geom_comparison.h"

namespace maps {
namespace wiki {
namespace topo {
namespace test {

EdgeIDSet
addedEdges(const MockStorage& original, const MockStorage& result)
{
    EdgeIDSet added;
    for (auto id : result.allEdgeIds()) {
        if (!original.edgeExists(id)) {
            added.insert(id);
        }
    }
    return added;
}

EdgeIDSet
movedEdges(const MockStorage& original, const MockStorage& result)
{
    EdgeIDSet moved;
    for (auto id : result.allEdgeIds()) {
        if (!original.edgeExists(id)) {
            continue;
        }
        const test::Edge& origEdge = original.testEdge(id);
        const test::Edge& resEdge = result.testEdge(id);
        if (origEdge.start != resEdge.start || origEdge.end != resEdge.end) {
            moved.insert(id);
        }
    }
    return moved;
}

EdgeIDSet
deletedEdges(const MockStorage& original, const MockStorage& result)
{
    EdgeIDSet deleted;
    for (auto id : original.allEdgeIds()) {
        if (!result.edgeExists(id)) {
            deleted.insert(id);
        }
    }
    return deleted;
}

EdgeIDSet
edgesWithModifiedGeom(const MockStorage& original, const MockStorage& result)
{
    EdgeIDSet res;
    for (auto id : result.allEdgeIds()) {
        if (!original.edgeExists(id)) {
            continue;
        }
        const test::Edge& origEdge = original.testEdge(id);
        const test::Edge& resEdge = result.testEdge(id);
        if (!test::compare(origEdge.geom, resEdge.geom)) {
            res.insert(id);
        }
    }
    return res;
}

EdgeIDSet
allAffectedEdgeIds(const MockStorage& original, const MockStorage& result)
{
    auto modified = addedEdges(original, result);

    auto changed = edgesWithModifiedGeom(original, result);
    modified.insert(changed.begin(), changed.end());

    auto moved = movedEdges(original, result);
    modified.insert(moved.begin(), moved.end());

    return modified;
}

NodeIDSet
addedNodes(const MockStorage& original, const MockStorage& result)
{
    NodeIDSet added;
    for (auto id : result.allNodeIds()) {
        if (!original.nodeExists(id)) {
            added.insert(id);
        }
    }
    return added;
}

NodeIDSet
nodesWithChangedIncidences(const MockStorage& original, const MockStorage& result)
{
    auto nodeInc = [] (const IncidencesByNodeMap& map)
    {
        ASSERT(map.size() == 1);
        std::map<EdgeID, IncidenceType> result;
        for (const auto& inc : map.begin()->second) {
            result.insert(inc);
        }
        return result;
    };

    NodeIDSet res;
    test::MockStorage origCopy = original;
    test::MockStorage resCopy = result;
    for (auto id : result.allNodeIds()) {
        if (!original.nodeExists(id)) {
            continue;
        }
        if (nodeInc(origCopy.incidencesByNodes({id})) !=
            nodeInc(resCopy.incidencesByNodes({id})))
        {
            res.insert(id);
        }
    }
    return res;
}

NodeIDSet
nodesWithChangedGeom(const MockStorage& original, const MockStorage& result)
{
    NodeIDSet res;
    for (auto id : result.allNodeIds()) {
        if (!original.nodeExists(id)) {
            continue;
        }
        if (!test::compare(original.testNode(id).pos, result.testNode(id).pos)) {
            res.insert(id);
        }
    }
    return res;
}

namespace {

EdgesDiffData
edgesDiffDataImpl(
    const EdgeIDSet& editedEdgeIds,
    const SplitEdges& splitEdges,
    const MockStorage& original,
    const MockStorage& result)
{
    EdgesDiffData res;
    EdgeIDSet affectedEdgeIds;
    for (const auto& split : splitEdges) {
        if (split.sourceId.exists() && !editedEdgeIds.count(split.sourceId.id())) {
            affectedEdgeIds.insert(split.resultedEdges.begin(), split.resultedEdges.end());
        }
    }
    for (auto id : addedEdges(original, result)) {
        (affectedEdgeIds.count(id)
            ? res.affectedEdgesDiff
            : res.editedEdgesDiff).newIds.insert(id);
    }
    auto changedAndMovedIds = edgesWithModifiedGeom(original, result);
    auto movedIds = movedEdges(original, result);
    EdgeIDSet changedIds;
    std::set_difference(
        changedAndMovedIds.begin(), changedAndMovedIds.end(),
        movedIds.begin(), movedIds.end(),
        std::inserter(changedIds, changedIds.end()));
    for (auto id : movedIds) {
        (affectedEdgeIds.count(id)
            ? res.affectedEdgesDiff
            : res.editedEdgesDiff).movedIds.insert(id);
    }
    for (auto id : changedIds) {
        (affectedEdgeIds.count(id)
            ? res.affectedEdgesDiff
            : res.editedEdgesDiff).changedGeomIds.insert(id);
    }
    return res;
}

} // namespace

EdgesDiffData
edgesDiffDataByEdge(
    SourceEdgeID id,
    const SplitEdges& splitEdges,
    const MockStorage& original,
    const MockStorage& result)
{
    EdgeIDSet editedIds;
    if (id.exists()) {
        editedIds.insert(id.id());
    }
    return edgesDiffDataImpl(editedIds, splitEdges, original, result);
}

EdgesDiffData
edgesDiffDataByNode(
    NodeID id,
    const SplitEdges& splitEdges,
    const MockStorage& original,
    const MockStorage& result)
{
    REQUIRE(original.nodeExists(id), "Node " << id << " not found in original storage");
    const auto& inc = test::MockStorage(original).incidencesByNodes({id});
    ASSERT(inc.size() == 1);
    EdgeIDSet editedIds;
    for (const auto& pair : inc.begin()->second) {
        editedIds.insert(pair.first);
    }
    return edgesDiffDataImpl(editedIds, splitEdges, original, result);
}


NodesDiff
nodesDiff(const MockStorage& original, const MockStorage& result)
{
    auto changedAndMovedIds = nodesWithChangedGeom(original, result);
    auto changedIncidencesIds = nodesWithChangedIncidences(original, result);
    EdgeIDSet changedGeomIds;
    std::set_difference(
        changedAndMovedIds.begin(), changedAndMovedIds.end(),
        changedIncidencesIds.begin(), changedIncidencesIds.end(),
        std::inserter(changedGeomIds, changedGeomIds.end()));

    return {addedNodes(original, result), changedGeomIds, changedIncidencesIds};
}

} // namespace test
} // namespace topo
} // namespace wiki
} // namespace maps

#include "events_builder.h"

#include "geom_comparison.h"
#include "storage_diff_helpers.h"
#include "../util/io.h"

#include <yandex/maps/wiki/topo/common.h>

namespace maps {
namespace wiki {
namespace topo {
namespace test {


std::list<topo::SplitEdgeEventData>
makeSplitEventData(
    const SourceEdgeID& sourceEdgeId,
    const SplitEdges& ids,
    const geolib3::Polyline2& alignedPolyline,
    const test::MockStorage& originalStorage,
    const test::MockStorage& resultStorage)
{

    std::list<topo::SplitEdgeEventData> res;
    for (const auto& splitData : ids) {
        EdgeIDVector edgeIds = splitData.resultedEdges;
        SplitPointPtrVector splitPoints;
        SplitPolylinePtrVector splitPolylines;
        auto partToKeepIDIndex =
            boost::make_optional<OptionalIndex::value_type>(false, {});
        geolib3::Polyline2 polyline;

        if (splitData.sourceId != sourceEdgeId) {
            polyline = originalStorage.testEdge(splitData.sourceId.id()).geom;
        } else {
            polyline = alignedPolyline;
        }

        for (size_t i = 0; i < edgeIds.size(); ++i) {
            const test::Edge& edge = resultStorage.testEdge(edgeIds[i]);

            splitPolylines.push_back(SplitPolylinePtr{new SplitPolyline{edge.geom, OptionalEdgeID {edgeIds[i]}}});
            const test::Node& startNode = resultStorage.testNode(edge.start);

            if (splitData.sourceId == sourceEdgeId) {
                const test::Node& endNode = resultStorage.testNode(edge.end);

                if ((geolib3::distanceAlongFromStart(polyline, startNode.pos) <
                        geolib3::distanceAlongFromStart(polyline, endNode.pos) && (i + 1) < edgeIds.size())
                            || ((i + 1) == edgeIds.size() && startNode.pos != polyline.points().back())) {
                    splitPoints.push_back(SplitPointPtr{new SplitPoint{startNode.pos, startNode.id}});
                } else {
                    splitPoints.push_back(SplitPointPtr{new SplitPoint{endNode.pos, endNode.id}});
                }
            } else {
                splitPoints.push_back(SplitPointPtr{new SplitPoint{startNode.pos, startNode.id}});
            }

            // set partToKeepIDIndex
            if (edge.id == splitData.sourceId.id()) {
                partToKeepIDIndex = i;
            }
        }

        const test::Edge& edge = resultStorage.testEdge(edgeIds.back());
        const test::Node& endNode = resultStorage.testNode(edge.end);
        if(splitData.sourceId == sourceEdgeId) {
            const test::Node& startNode = resultStorage.testNode(edge.start);

            if (polyline.points().back() != startNode.pos) {
                splitPoints.push_back(SplitPointPtr{new SplitPoint{endNode.pos, endNode.id}});
            } else {
                splitPoints.push_back(SplitPointPtr{new SplitPoint{startNode.pos, startNode.id}});
            }
        } else {
             splitPoints.push_back(SplitPointPtr{new SplitPoint{endNode.pos, endNode.id}});
        }


        res.emplace_back(
            splitData.sourceId,
            /* isEditedEdge = */splitData.sourceId == sourceEdgeId,
            polyline,
            splitPoints,
            splitPolylines,
            partToKeepIDIndex
        );
    }

    return res;
}

std::list<topo::AddEdgeEventData>
makeAddEventData(
    const SourceEdgeID& sourceId,
    const test::SplitEdges& splitIds,
    const test::MockStorage& originalStorage,
    const test::MockStorage& resultStorage)
{
    std::list<topo::AddEdgeEventData> res;

    for (const auto& id : addedEdges(originalStorage, resultStorage)) {
        const test::Edge& edge = resultStorage.testEdge(id);
        auto it = std::find_if(splitIds.begin(), splitIds.end(),
            [&] (SplitEdges::const_reference splitEdge) -> bool
            {
                EdgeIDSet idSet(
                    splitEdge.resultedEdges.begin(),
                    splitEdge.resultedEdges.end());
                return splitEdge.sourceId != sourceId && idSet.find(id) != idSet.end();
            }
        );
        if (it == splitIds.end() || id == sourceId.id()) {
            res.emplace_back(id, sourceId, edge.start, edge.end, edge.geom);
        } else {
            res.emplace_back(id, it->sourceId, edge.start, edge.end, edge.geom);
        }
    }
    return res;
}

std::list<topo::MoveEdgeEventData>
makeMoveEventData(
    const test::MockStorage& originalStorage,
    const test::MockStorage& resultStorage)
{
    std::list<topo::MoveEdgeEventData> res;

    for (auto id : movedEdges(originalStorage, resultStorage)) {
        const test::Edge& originalEdge = originalStorage.testEdge(id);
        const test::Edge& resultEdge = resultStorage.testEdge(id);
        auto newStart = boost::optional<NodeID>(
            originalEdge.start != resultEdge.start,
            resultEdge.start);
        auto newEnd = boost::optional<NodeID>(
            originalEdge.end != resultEdge.end,
            resultEdge.end);
        ASSERT(newStart || newEnd);
        res.emplace_back(id, newStart, newEnd, resultEdge.geom,
            IncidentNodes(originalEdge.start, originalEdge.end));
    }
    return res;
}

boost::optional<topo::MergeNodesEventData>
makeMergeNodesEventData(
    NodeID nodeId,
    boost::optional<NodeID> mergedNodeId,
    const test::MockStorage& resultStorage)
{
    if (!mergedNodeId) {
        return boost::none;
    }

    ASSERT(
        !resultStorage.nodeExists(nodeId) || !resultStorage.nodeExists(*mergedNodeId));
    NodeID mergedId = nodeId;
    NodeID deletedId = *mergedNodeId;
    if (resultStorage.nodeExists(*mergedNodeId)) {
        std::swap(mergedId, deletedId);
    }
    return topo::MergeNodesEventData(
        deletedId, mergedId, resultStorage.testNode(mergedId).pos);
}

topo::MergeEdgesEventData
makeMergeEdgesEventData(
    NodeID commonNodeId,
    const test::MockStorage& originalStorage,
    const test::MockStorage& resultStorage)
{
    const auto& nodeIncidences =
        test::MockStorage(originalStorage).incidencesByNodes({commonNodeId});
    ASSERT(nodeIncidences.size() == 1);
    const auto& incVector = nodeIncidences.begin()->second;
    REQUIRE(incVector.size() == 2, "Node degree > 2 for correct merge edges test");
    EdgeID deletedEdgeId = incVector.front().first;
    EdgeID mergedEdgeId = incVector.back().first;
    if (resultStorage.edgeExists(deletedEdgeId)) {
        REQUIRE(!resultStorage.edgeExists(mergedEdgeId),
            "No deleted edge after MergeEdges event, common node id "
                << util::print(commonNodeId)
            );
        std::swap(deletedEdgeId, mergedEdgeId);
    } else {
        REQUIRE(resultStorage.edgeExists(mergedEdgeId),
            "No deleted edge after MergeEdges event, common node id "
                << util::print(commonNodeId)
        );
    }

    return MergeEdgesEventData {
        mergedEdgeId,
        deletedEdgeId,
        commonNodeId,
        resultStorage.testEdge(mergedEdgeId).geom.points(),
        incVector.front().second != incVector.back().second
    };
}

std::list<topo::DeleteEdgeEventData>
makeDeleteEventData(
    const test::MockStorage& originalStorage,
    const test::MockStorage& resultStorage)
{
    std::list<topo::DeleteEdgeEventData> result;

    for (auto id : deletedEdges(originalStorage, resultStorage)) {
        const test::Edge& e = originalStorage.testEdge(id);
        result.push_back({id, e.start, e.end});
    }

    return result;
}

} // namespace test
} // namespace topo
} // namespace wiki
} // namespace maps

#include "test_graph.h"

#include <maps/libs/common/include/exception.h>

#include <boost/test/unit_test.hpp>

#include <string>
#include <utility>

namespace maps {
namespace wiki {
namespace graph {
namespace tests {

TestGraph::TestGraph(EdgesByNodeId&& fromEdgesByNodeId, EdgesByNodeId&& toEdgesByNodeId)
    : fromEdgesByNodeId_(std::move(fromEdgesByNodeId))
    , toEdgesByNodeId_(std::move(toEdgesByNodeId))
{}

NodeIdSet
TestGraph::nodeIds() const
{
    NodeIdSet result;
    for (const auto& nodeIdEdgesPair : fromEdgesByNodeId_) {
        result.insert(nodeIdEdgesPair.first);
        for (const auto& edge : nodeIdEdgesPair.second) {
            result.insert(edge.endNodeId());
        }
    }
    return result;
}

Edges
TestGraph::outEdges(NodeID nodeId) const
{
    const auto it = fromEdgesByNodeId_.find(nodeId);
    REQUIRE(
        it != fromEdgesByNodeId_.end(),
        "There is no node with id " << nodeId
    );
    return it->second;
}

Edges
TestGraph::inEdges(NodeID nodeId) const
{
    const auto it = toEdgesByNodeId_.find(nodeId);
    REQUIRE(
        it != toEdgesByNodeId_.end(),
        "There is no node with id " << nodeId
    );
    return it->second;
}

TestGraph
TestGraph::fromJson(const json::Value& graph)
{
    EdgesByNodeId fromEdgesByNodeId;
    EdgesByNodeId toEdgesByNodeId;

    for (const auto& id: graph.fields()) {
        NodeID startNodeId = std::stoll(id);
        toEdgesByNodeId.insert({startNodeId, Edges()});

        Edges fromEdgeList;
        for (const auto& edge: graph[id]) {
            const NodeID endNodeId = edge["endNodeId"].as<NodeID>();
            const double weight = edge["weight"].as<double>(1.0);

            fromEdgeList.emplace_back(startNodeId, endNodeId, weight);
            toEdgesByNodeId[endNodeId].emplace_back(startNodeId, endNodeId, weight);
        }

        fromEdgesByNodeId[startNodeId] = std::move(fromEdgeList);
    }

    return TestGraph{
        std::move(fromEdgesByNodeId),
        std::move(toEdgesByNodeId),
    };
}

} // namespace tests
} // namespace graph
} // namespace wiki
} // namespace maps

#pragma once

#include <yandex/maps/wiki/graph/graph.h>
#include <maps/libs/json/include/value.h>

#include <map>
#include <utility>
#include <vector>

namespace maps {
namespace wiki {
namespace graph {
namespace tests {

typedef std::map<NodeID, Edges> EdgesByNodeId;

class TestGraph {

public:
    TestGraph(EdgesByNodeId&& fromEdgesByNodeId, EdgesByNodeId&& toEdgesByNodeId);

    NodeIdSet nodeIds() const;

    Edges outEdges(NodeID nodeId) const;

    Edges inEdges(NodeID nodeId) const;

    static TestGraph fromJson(const json::Value& json);

private:
    EdgesByNodeId fromEdgesByNodeId_;
    EdgesByNodeId toEdgesByNodeId_;
};

} // namespace tests
} // namespace graph
} // namespace wiki
} // namespace maps

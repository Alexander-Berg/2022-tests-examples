#include "test_io.h"


namespace maps {
namespace wiki {
namespace topo {
namespace test {



std::ostream& operator<<(std::ostream& out, const std::pair<NodeID, NodeID>& id)
{
    out << "(" << id.first << ", " << id.second << ")";
    return out;
}


std::ostream& operator<<(std::ostream& out, const IncidentNodes& nodes)
{
    out << "[" << nodes.start << ", " << nodes.end << "]";
    return out;
}


std::ostream& operator<<(std::ostream& out, const IncidentEdges& edges)
{
    out << "[";
    for (const auto& edge : edges) {
        out << "[id = " << edge.first << ", start: " << edge.second << "]";
    }
    out << "]";
    return out;
}


std::ostream& operator<<(std::ostream& out, const IncidencesByNodeMap& nodesIncs)
{
    out << "[";
    for (const auto& nodeIncs : nodesIncs) {
        out << "[node id = " << nodeIncs.first << ", edges " << nodeIncs.second << "]";
    }
    out << "]";
    return out;
}


std::ostream& operator<<(std::ostream& out, const IncidencesByEdgeMap& edgesIncs)
{
    out << "[";
    for (const auto& edgeIncs : edgesIncs) {
        out << "[edge id = " << edgeIncs.first << ", nodes " << edgeIncs.second << "]";
    }
    out << "]";
    return out;
}


} // namespace test
} // namespace topo
} // namespace wiki
} // namespace maps


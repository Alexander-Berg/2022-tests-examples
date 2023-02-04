#define BOOST_TEST_ALTERNATIVE_INIT_API

#include <yandex/maps/wiki/graph/traversal.h>
#include <yandex/maps/wiki/common/string_utils.h>

#include "common.h"
#include "io.h"
#include "test_graph.h"

#include <boost/filesystem.hpp>
#include <boost/test/unit_test.hpp>

#include <algorithm>
#include <functional>
#include <iterator>
#include <memory>
#include <string>
#include <vector>

namespace bt = boost::unit_test;

namespace maps {
namespace wiki {
namespace graph {
namespace tests {

typedef std::map<NodeID, size_t> NodeIdToDistance;

struct BFSTest {
    TestGraph graph;
    NodeID fromNodeId;
    NodeIdSet breakNodeIds;
    NodeIdToDistance nodeIdToDistance;
};

NodeIdToDistance nodeIdToDistanceFromJson(const json::Value& json)
{
    NodeIdToDistance nodeIdToDistance;
    for (const auto& id: json.fields()) {
        const NodeID nodeId = std::stoll(id);
        const size_t stamp = json[id].as<size_t>();

        nodeIdToDistance.emplace(nodeId, stamp);
    }

    return nodeIdToDistance;
}

BFSTest testFromJson(const json::Value& json)
{
    return {
        TestGraph::fromJson(json["graph"]),
        json["input"]["fromNodeId"].as<NodeID>(),
        json["input"]["breakNodeIds"].as<NodeIdSet>(),
        nodeIdToDistanceFromJson(json["expected"])
    };
}

void check(BFSTest test)
{
    NodeIdSet missedNodeIds;
    for (const auto pair: test.nodeIdToDistance) {
        missedNodeIds.insert(pair.first);
    }

    auto step = [&](NodeID fromNodeId, NodeID toNodeId, size_t distance) {
        const auto it = test.nodeIdToDistance.find(toNodeId);
        BOOST_REQUIRE_MESSAGE(
            it != test.nodeIdToDistance.end(),
            "Unexpected node id " << toNodeId
        );

        const size_t expectedDistance = it->second;
        BOOST_REQUIRE_MESSAGE(
            expectedDistance == distance,
            "For node id " << toNodeId << " expected distance " << expectedDistance <<
                ", but returned " << distance

        );

        bool isPossibleMove = false;
        for (const auto& edge: test.graph.inEdges(toNodeId)) {
            if (edge.startNodeId() == fromNodeId) {
                isPossibleMove = true;
            }
        }

        BOOST_REQUIRE_MESSAGE(
            isPossibleMove,
            "There is no edge for nodes" << fromNodeId << " -> " << toNodeId
        );

        missedNodeIds.erase(toNodeId);
    };

    breadthFirstSearch(
        /* outEdges = */[&](NodeID nodeId) {
            return test.graph.outEdges(nodeId);
        },
        /* step = */ step,
        /* fromNodeId = */ test.fromNodeId,
        /* isBreakNode = */ [&test](NodeID nodeId) {
            return test.breakNodeIds.count(nodeId);
        }
    );

    BOOST_REQUIRE_MESSAGE(
        missedNodeIds.empty(),
        "Missed node ids " << printCollection(missedNodeIds)
    );
}

} // namespace tests
} // namespace graph
} // namespace wiki
} // namespace maps

boost::unit_test::test_suite* init_unit_test_suite(int, char**)
{
    maps::wiki::graph::tests::initTestSuite("traversal/bfs",
        maps::wiki::graph::tests::testFromJson,
        maps::wiki::graph::tests::check);
    return nullptr;
}

#ifdef YANDEX_MAPS_BUILD
bool init_unit_test_suite()
{
    init_unit_test_suite(0, NULL);
    return true;
}

int main(int argc, char* argv[])
{
    return bt::unit_test_main(&init_unit_test_suite, argc, argv);
}
#endif //YANDEX_MAPS_BUILD

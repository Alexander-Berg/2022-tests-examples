#define BOOST_TEST_ALTERNATIVE_INIT_API

#include <yandex/maps/wiki/graph/shortest_path.h>
#include <yandex/maps/wiki/common/string_utils.h>

#include "common.h"
#include "io.h"
#include "test_graph.h"

#include <boost/filesystem.hpp>
#include <boost/test/unit_test.hpp>

#include <functional>
#include <string>
#include <vector>


namespace bt = boost::unit_test;

namespace maps {
namespace wiki {
namespace graph {
namespace tests {

struct ShortestPathTest {
    TestGraph graph;
    NodeID fromNodeId;
    NodeID toNodeId;
    ShortestPath expected;
};

ShortestPathTest testFromJson(const json::Value& json)
{
    return {
        TestGraph::fromJson(json["graph"]),
        json["input"]["fromNodeId"].as<NodeID>(),
        json["input"]["toNodeId"].as<NodeID>(),
        ShortestPath(
            json["expected"]["path"].as<Path>(),
            json["expected"]["length"].as<double>()
        )
    };
}

void check(const ShortestPathTest& test)
{
    const auto output = findShortestPath(
        /* outEdges = */ [&test](NodeID nodeId) {
            return test.graph.outEdges(nodeId);
        },
        /* fromNodeId = */ test.fromNodeId,
        /* isBreakNode = */ [&test](NodeID nodeId) {
            return nodeId == test.toNodeId;
        }
    );

    BOOST_REQUIRE_MESSAGE(
        test.expected.path() == output.path(),
        "Pathes mismatch, expected " << printCollection(test.expected.path())
            << ", output " << printCollection(output.path())
    );

    BOOST_REQUIRE_MESSAGE(
        test.expected.length() == output.length(),
        "Length mismatches, expected " << test.expected.length()
            << ", output " << output.length()
    );
}

} // namespace tests
} // namespace graph
} // namespace wiki
} // namespace maps

boost::unit_test::test_suite* init_unit_test_suite(int, char**)
{
    maps::wiki::graph::tests::initTestSuite("shortest_path",
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

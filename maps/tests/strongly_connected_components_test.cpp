#define BOOST_TEST_ALTERNATIVE_INIT_API

#include <yandex/maps/wiki/graph/strongly_connected_components.h>

#include "common.h"
#include "io.h"
#include "test_graph.h"

#include <boost/test/unit_test.hpp>

namespace bt = boost::unit_test;

namespace maps {
namespace wiki {
namespace graph {
namespace tests {

struct StronglyConnectedComponentsTest
{
    TestGraph graph;
    NodeIds fromNodeIds;
    std::vector<NodeIds> components;
};

StronglyConnectedComponentsTest testFromJson(const json::Value& json)
{
    return {
        TestGraph::fromJson(json["graph"]),
        json["input"]["fromNodeIds"].as<NodeIds>(),
        json["expected"].as<std::vector<NodeIds>>()
    };
}

void check(const StronglyConnectedComponentsTest& test)
{
    StronglyConnectedComponentsFinder sccFinder;
    for (auto fromNodeId : test.fromNodeIds) {
        sccFinder.exploreNode(
            /* fromNodeId = */ fromNodeId,
            /* outEdges = */ [&test](NodeID nodeId) {
                return test.graph.outEdges(nodeId);
            });
    }
    const auto& components = sccFinder.stronglyConnectedComponents();

    BOOST_REQUIRE_MESSAGE(
        test.components.size() == components.size(),
        "Number of components mismatch, expected " << test.components.size()
            << ", output " << components.size()
    );

    for (auto component: components) {
        std::sort(component.begin(), component.end());

        bool foundMatchedTestComponent = false;
        for (const auto& testComponent : test.components) {
            if (component.size() == testComponent.size() &&
                std::equal(component.begin(), component.end(), testComponent.begin())) {
                    foundMatchedTestComponent = true;
                    break;
            }
        }
        BOOST_REQUIRE_MESSAGE(
            foundMatchedTestComponent,
            "Mismatched component " << printCollection(component)
        );
    }
}

} // namespace tests
} // namespace graph
} // namespace wiki
} // namespace maps

boost::unit_test::test_suite* init_unit_test_suite(int, char**)
{
    maps::wiki::graph::tests::initTestSuite("strongly_connected_components",
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

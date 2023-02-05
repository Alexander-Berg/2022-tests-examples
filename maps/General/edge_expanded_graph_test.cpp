#include "library/cpp/testing/unittest/env.h"
#include "library/cpp/testing/unittest/registar.h"

#include "packer.h"
#include "test_graph.h"
#include "edge_expanded_graph.h"
#include "ut/topology_checker.h"
#include "ut/test_util.h"

#include <unordered_map>
#include <vector>

#include <maps/libs/locale/include/convert.h>

namespace coverage = maps::coverage5;

namespace {

const std::string TEST_BORDER_PATH = "/dev/null";

const std::unordered_map<mrg::EdgeId, Cost> CUSTOM_COSTS = {
    {mrg::EdgeId(7), {2, 2}},
    {mrg::EdgeId(8), {2, 2}},
    {mrg::EdgeId(11), {3, 3}}
};

class DummyCostCalculator : public CostCalculator {
public:
    Cost operator()(mrg::EdgeId edgeId) const override {
        if (CUSTOM_COSTS.count(edgeId)) {
            return CUSTOM_COSTS.at(edgeId);
        } else {
            return Cost {
                1 /* edge penalty weight */,
                1 /* edge penalty duration */ };
        }
    }

    boost::optional<Cost> turnCost(
            mrg::EdgeId /* sourceEdge */,
            mrg::EdgeId /* targetEdge */) const override {
        return Cost {
            0 /* turn penalty weight */,
            0 /* turn penalty duration */ };
    }

    bool requiresAccessPass(
            mrg::EdgeId /* sourceEdge */,
            mrg::EdgeId /* targetEdge */) const override {
        return false;
    }
};

} // namespace

Y_UNIT_TEST_SUITE(OfflineExpandedGraph) {
    Y_UNIT_TEST(EdgeExpandedGraphTest) {
        buildSmallGraph();
        maps::road_graph::Graph roadGraph(TEST_ROAD_GRAPH_PATH);
        maps::road_graph::PersistentIndex persistentIdx(TEST_PERSISTENT_INDEX_PATH);
        maps::succinct_rtree::Rtree rtree(TEST_RTREE_PATH, roadGraph);
        coverage::Coverage coverage(TEST_COVERAGE_DIR);
        DummyCostCalculator costCalculator;

        flatbuffers::FlatBufferBuilder fbBuilder;

        Packer(
            &rtree,
            &roadGraph,
            maps::locale::to<maps::locale::Locale>("ru_RU"),
            &persistentIdx,
            &costCalculator,
            BinaryPath("maps/data/test/geobase/geodata5.bin"),
            &fbBuilder,
            TEST_BORDER_PATH).pack(
                coverage[TEST_COVERAGE_LAYER], {TEST_REGION_ID});

        auto drivingData = od::GetDrivingData(fbBuilder.GetBufferPointer());

        GraphTopologyChecker topology(drivingData);
        TestEdgeExpandedGraph testGraph(drivingData->edgeExpandedGraph());

        for (VertexId s = 0; s < testGraph.verticesNumber() ; ++s) {
            for (VertexId t = 0; t < testGraph.verticesNumber(); ++t) {
                const auto path = findPath(testGraph, s, t);
                if (s == t) {
                    UNIT_ASSERT(path.weight == 0);
                    UNIT_ASSERT(path.vertices == std::vector<VertexId>({s}));
                } else {
                    UNIT_ASSERT(path.weight != INVALID_WEIGHT);
                }
            }
        }

        // Edge Expanded Graph is very similar to the graph in the test_graph.hpp file
        // but the main difference is that usual Graph's edges correspond to vertices of EdgeExpandedGraph
        // In terms of edges of Graph path is looked from edge Es to edge Et and path goes through
        // intermediate edges Ei, for example path from E12 to E11 is E12 -> E14 -> E11

        auto toOriginal = [&] (const std::vector<VertexId>& edges) {
            std::vector<size_t> result(edges.size());
            std::transform(
                edges.begin(),
                edges.end(),
                result.begin(),
                [&] (const EdgeId& e) {
                    return topology.originalEdgeId(e).value();
                });
            return result;
        };

        auto path = findPath(
            testGraph,
            topology.findEdge(mrg::EdgeId(12)),
            topology.findEdge(mrg::EdgeId(11)));
        UNIT_ASSERT(path.weight == 2);
        UNIT_ASSERT(toOriginal(path.vertices) ==
            std::vector<size_t>({12, 16, 11}));

        path = findPath(
            testGraph,
            topology.findEdge(mrg::EdgeId(0)),
            topology.findEdge(mrg::EdgeId(3)));
        UNIT_ASSERT(path.weight == 2);
        UNIT_ASSERT(toOriginal(path.vertices) ==
            std::vector<size_t>({0, 4, 3}));

        path = findPath(
            testGraph,
            topology.findEdge(mrg::EdgeId(0)),
            topology.findEdge(mrg::EdgeId(4)));
        UNIT_ASSERT(path.weight == 1);
        UNIT_ASSERT(toOriginal(path.vertices) ==
            std::vector<size_t>({0, 4}));

        path = findPath(
            testGraph,
            topology.findEdge(mrg::EdgeId(0)),
            topology.findEdge(mrg::EdgeId(8)));
        UNIT_ASSERT(path.weight == 2);
        UNIT_ASSERT(toOriginal(path.vertices) ==
            std::vector<size_t>({0, 5, 8}));

        path = findPath(
            testGraph,
            topology.findEdge(mrg::EdgeId(0)),
            topology.findEdge(mrg::EdgeId(6)));
        UNIT_ASSERT(path.weight == 3);
        UNIT_ASSERT(toOriginal(path.vertices) ==
            std::vector<size_t>({0, 4, 1, 6}));

        path = findPath(
            testGraph,
            topology.findEdge(mrg::EdgeId(0)),
            topology.findEdge(mrg::EdgeId(7)));
        UNIT_ASSERT(path.weight == 3);
        UNIT_ASSERT(toOriginal(path.vertices) ==
            std::vector<size_t>({0, 4, 1, 7}));

        path = findPath(
            testGraph,
            topology.findEdge(mrg::EdgeId(10)),
            topology.findEdge(mrg::EdgeId(7)));
        UNIT_ASSERT(path.weight == 3);
        UNIT_ASSERT(toOriginal(path.vertices) ==
            std::vector<size_t>({10, 4, 1, 7}));

        // Check that path actually depends on weight
        path = findPath(
            testGraph,
            topology.findEdge(mrg::EdgeId(15)),
            topology.findEdge(mrg::EdgeId(10)));
        UNIT_ASSERT(path.weight == 3);
        UNIT_ASSERT(toOriginal(path.vertices) ==
            std::vector<size_t>({15, 6, 2, 10}));

        path = findPath(
            testGraph,
            topology.findEdge(mrg::EdgeId(3)),
            topology.findEdge(mrg::EdgeId(6)));
        UNIT_ASSERT(path.weight == 3);
        UNIT_ASSERT(toOriginal(path.vertices) ==
            std::vector<size_t>({3, 12, 15, 6}));
    }
}

#include <library/cpp/testing/unittest/registar.h>

#include <maps/libs/leptidea/include/graph_topology.h>
#include <maps/libs/leptidea/include/types.h>
#include <maps/libs/leptidea/tests/lib/test_data_generation.h>

using namespace leptidea7;

void testTopology(const GraphTopology& topology) {
    UNIT_ASSERT_EQUAL(topology.verticesNumber(), VertexId(10));
    UNIT_ASSERT_EQUAL(topology.levelsNumber(),   LevelId(4));
    UNIT_ASSERT_EQUAL(topology.edgesNumber(),    EdgeId(11));
    UNIT_ASSERT_EQUAL(topology.version(),        std::string("test"));

    UNIT_ASSERT_EQUAL(topology.componentsNumber(LevelId(0)), GraphComponentId(10));
    UNIT_ASSERT_EQUAL(topology.componentsNumber(LevelId(1)), GraphComponentId(4));
    UNIT_ASSERT_EQUAL(topology.componentsNumber(LevelId(2)), GraphComponentId(2));
    UNIT_ASSERT_EQUAL(topology.componentsNumber(LevelId(3)), GraphComponentId(1));

    auto checkIsRange = [](
            const maps::xrange_view::XRangeView<GraphComponentId>& v, size_t first, size_t last) {
        ASSERT(last >= first);
        UNIT_ASSERT_EQUAL(v.size(), last - first + 1);
        UNIT_ASSERT(std::is_sorted(v.begin(), v.end()));
        UNIT_ASSERT_EQUAL((*(v.begin())).value(), first);
        UNIT_ASSERT_EQUAL((*(v.end() - 1)).value(), last);
    };

    checkIsRange(topology.componentsAtLevel(LevelId(0)), 0,  9);
    checkIsRange(topology.componentsAtLevel(LevelId(1)), 10, 13);
    checkIsRange(topology.componentsAtLevel(LevelId(2)), 14, 15);
    checkIsRange(topology.componentsAtLevel(LevelId(3)), 16, 16);

    UNIT_ASSERT_EQUAL(
        topology.parentComponent(GraphComponentId(2)),  GraphComponentId(10));
    UNIT_ASSERT_EQUAL(
        topology.parentComponent(GraphComponentId(12)), GraphComponentId(15));
    UNIT_ASSERT_EQUAL(
        topology.parentComponent(GraphComponentId(14)), GraphComponentId(16));

    UNIT_ASSERT_EQUAL(topology.level(GraphComponentId(6)),  LevelId(0));
    UNIT_ASSERT_EQUAL(topology.level(GraphComponentId(11)), LevelId(1));
    UNIT_ASSERT_EQUAL(topology.level(GraphComponentId(15)), LevelId(2));
    UNIT_ASSERT_EQUAL(topology.level(GraphComponentId(16)), LevelId(3));

    UNIT_ASSERT_EQUAL(topology.roadGraph().edge(EdgeId(7)).source, VertexId(7));
    UNIT_ASSERT_EQUAL(topology.roadGraph().edge(EdgeId(5)).target, VertexId(6));

    UNIT_ASSERT_EQUAL(
        topology.componentAtLevel(VertexId(3), LevelId(2)), GraphComponentId(14));

    UNIT_ASSERT_EQUAL(
        topology.sourceComponentAtLevel(EdgeId(2),  LevelId(0)),
        GraphComponentId(2));
    UNIT_ASSERT_EQUAL(
        topology.sourceComponentAtLevel(EdgeId(2),  LevelId(1)),
        GraphComponentId(10));
    UNIT_ASSERT_EQUAL(
        topology.targetComponentAtLevel(EdgeId(10), LevelId(2)),
        GraphComponentId(14));

    {
        const auto outEdges = topology.outEdgeIds(GraphComponentId(15));
        UNIT_ASSERT_EQUAL(
            std::vector<EdgeId>(outEdges.begin(), outEdges.end()),
            std::vector<EdgeId>({EdgeId(9), EdgeId(10)}));
    }
    {
        const auto inEdges = topology.inEdgeIds(GraphComponentId(11));
        UNIT_ASSERT_EQUAL(
            std::vector<EdgeId>(inEdges.begin(), inEdges.end()),
            std::vector<EdgeId>({EdgeId(2), EdgeId(10)}));
    }
    UNIT_ASSERT_EQUAL(
        topology.outEdgeIndex(EdgeId(10), GraphComponentId(15)), OutIndex(1));
    UNIT_ASSERT_EQUAL(
        topology.inEdgeIndex(EdgeId(2), GraphComponentId(11)), InIndex(0));
    UNIT_ASSERT_EQUAL(
        topology.inEdgeIndex(EdgeId(10), GraphComponentId(11)), InIndex(1));

    UNIT_ASSERT_EQUAL(topology.level(EdgeId(1)), LevelId(0));
    UNIT_ASSERT_EQUAL(topology.level(EdgeId(2)), LevelId(1));
    UNIT_ASSERT_EQUAL(topology.level(EdgeId(4)), LevelId(2));
    UNIT_ASSERT_EQUAL(topology.level(EdgeId(10)), LevelId(2));
}

Y_UNIT_TEST_SUITE(CreateTopology) {

Y_UNIT_TEST(create_topology_test) {
    TopologyDescription descr = {
            { {0, 1, 2},  {3, 4} },
            { {5, 6, 7},  {8, 9} }
        };
    std::vector<Point2> points;
    Edges edges;
    for (size_t i = 0; i < 10; ++i) {
        points.emplace_back(i, 0);
        edges.emplace_back(i, (i + 1) % 10);
    }
    edges.emplace_back(9, 3);
    std::vector<rg::MutableEdgeData> edgesData(
        11, buildEdgeData(20 /*speed*/, false /*toll*/, rg::AccessId::Automobile));

    const std::unique_ptr<rg::Graph> roadGraph =
        buildRoadGraph(edges, edgesData, {}, {}, {}, {}, {}, {}, 10);

    UNIT_ASSERT_EQUAL(roadGraph->version(), "test");
    auto topo = buildTopology(*roadGraph, descr.asPartitioning());

    testTopology(GraphTopology(roadGraph.get(), std::move(topo)));
}

}

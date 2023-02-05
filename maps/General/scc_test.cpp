#include <library/cpp/testing/unittest/registar.h>

#include <maps/libs/road_graph/serialization/include/serialization.h>
#include <maps/libs/road_graph_scc/include/graph_scc.h>
#include <maps/libs/road_graph_scc/include/graph_scc_builder.h>

using namespace maps::road_graph;
using namespace maps::road_graph::literals;

namespace {

std::unique_ptr<Graph> makeRoadGraph(
    const std::string& version,
    const std::vector<VertexId>& vertices,
    const std::vector<Edge>& edges)
{
    MutableGraph mutableGraph(
        version, vertices.size(), edges.size(), 0 /* roadsNumber */);

    mutableGraph.setBuiltForAccessIdMask(AccessId::Automobile);

    for (const VertexId vertexId : vertices) {
        mutableGraph.setVertexGeometry(vertexId, maps::geolib3::Point2(0, 0));
    }

    for (const Edge& edge : edges) {
        MutableEdgeData mutableEdgeData = {};
        mutableEdgeData.accessIdMask = AccessId::Automobile;
        mutableEdgeData.geometry.add(maps::geolib3::Point2(0, 0));
        mutableEdgeData.geometry.add(maps::geolib3::Point2(0, 0));

        mutableGraph.setEdge(edge, /*isBase*/ true);
        mutableGraph.setEdgeData(edge.id, mutableEdgeData);
    }

    return serialize(mutableGraph);
}

constexpr std::string_view ROAD_GRAPH_SCC_FILE_NAME = "road_graph_scc";

} // namespace

Y_UNIT_TEST_SUITE(RoadGraphSccTest) {

/*
Graph 1

    v0                v1
    o-----<----->-----o
    |                 |
    ^                 ^
    |                 |
    v                 v
    |                 |
    o-----<----->-----o
    v3                v2
*/

Y_UNIT_TEST(SingleSccGraph) {
    const std::vector<VertexId> vertices{0_v, 1_v, 2_v, 3_v};
    const std::vector<Edge> edges = {
        {0_e, 0_v, 1_v},
        {1_e, 0_v, 3_v},
        {2_e, 1_v, 0_v},
        {3_e, 1_v, 2_v},
        {4_e, 2_v, 1_v},
        {5_e, 2_v, 3_v},
        {6_e, 3_v, 2_v},
        {7_e, 3_v, 0_v}
    };

    const std::unique_ptr<Graph> graph = makeRoadGraph("test_1", vertices, edges);

    buildStronglyConnectedComponents(*graph, ROAD_GRAPH_SCC_FILE_NAME);

    const GraphStronglyConnectedComponents graphScc(ROAD_GRAPH_SCC_FILE_NAME);

    UNIT_ASSERT_VALUES_EQUAL(graph->edgesNumber(), graphScc.edgesNumber());

    UNIT_ASSERT_VALUES_EQUAL(graphScc.edgesNumber().value(), 8);

    for (const Edge& edge : edges) {
        const auto component = graphScc.component(edge.id, AccessId::Automobile);
        UNIT_ASSERT_VALUES_EQUAL(component.id, StronglyConnectedComponentId(0));
        UNIT_ASSERT_VALUES_EQUAL(component.accessIdMask, AccessId::Automobile);
        UNIT_ASSERT_VALUES_EQUAL(component.size, 8);
        UNIT_ASSERT_VALUES_EQUAL(component.id, graphScc.componentId(edge.id, AccessId::Automobile));
    }
}

/*
Graph 2

    v0                v1
    o-----<----->-----o
    |                 |
    ^                 ^
    |                 |
    v                 v
    |                 |
    o-----<----->-----o
    v3                v2
 
    v4                v5
    o-----<----->-----o
*/

Y_UNIT_TEST(TwoSccGraph) {
    const std::vector<VertexId> vertices{0_v, 1_v, 2_v, 3_v, 4_v, 5_v};
    const std::vector<Edge> edges = {
        {0_e, 0_v, 1_v},
        {1_e, 0_v, 3_v},
        {2_e, 1_v, 0_v},
        {3_e, 1_v, 2_v},
        {4_e, 2_v, 1_v},
        {5_e, 2_v, 3_v},
        {6_e, 3_v, 2_v},
        {7_e, 3_v, 0_v},
        {8_e, 4_v, 5_v},
        {9_e, 5_v, 4_v}
    };

    const std::unique_ptr<Graph> graph = makeRoadGraph("test_2", vertices, edges);

    buildStronglyConnectedComponents(*graph, ROAD_GRAPH_SCC_FILE_NAME);

    const GraphStronglyConnectedComponents graphScc(ROAD_GRAPH_SCC_FILE_NAME);

    UNIT_ASSERT_VALUES_EQUAL(graph->edgesNumber(), graphScc.edgesNumber());

    UNIT_ASSERT_VALUES_EQUAL(graphScc.edgesNumber().value(), 10);

    const StronglyConnectedComponent e0scc = graphScc.component(0_e, AccessId::Automobile);
    for (size_t i = 0; i < 8; ++i) {
        const Edge& edge = edges[i];
        const auto component = graphScc.component(edge.id, AccessId::Automobile);
        UNIT_ASSERT_VALUES_EQUAL(component.id, e0scc.id);
        UNIT_ASSERT_VALUES_EQUAL(component.accessIdMask, AccessId::Automobile);
        UNIT_ASSERT_VALUES_EQUAL(component.size, 8);
        UNIT_ASSERT_VALUES_EQUAL(component.id, graphScc.componentId(edge.id, AccessId::Automobile));
    }

    const StronglyConnectedComponent e8scc = graphScc.component(8_e, AccessId::Automobile);
    UNIT_ASSERT(e8scc.id != e0scc.id);
    for (size_t i = 8; i < 10; ++i) {
        const Edge& edge = edges[i];
        const auto component = graphScc.component(edge.id, AccessId::Automobile);
        UNIT_ASSERT_VALUES_EQUAL(component.id, e8scc.id);
        UNIT_ASSERT_VALUES_EQUAL(component.accessIdMask, AccessId::Automobile);
        UNIT_ASSERT_VALUES_EQUAL(component.size, 2);
        UNIT_ASSERT_VALUES_EQUAL(component.id, graphScc.componentId(edge.id, AccessId::Automobile));
    }
}

/*
Graph 3

    v0                v1
    o-----<----->-----o
    |                 |
    ^                 ^
    |                 |
    v                 v
    |                 |
 v3 o-----<----->-----o v2
    |                
    v                 
    |
    o-----<----->-----o
    v4                v5
*/

Y_UNIT_TEST(WeakComponentGraph) {
    const std::vector<VertexId> vertices{0_v, 1_v, 2_v, 3_v, 4_v, 5_v};
    const std::vector<Edge> edges = {
        {0_e, 0_v, 1_v},
        {1_e, 0_v, 3_v},
        {2_e, 1_v, 0_v},
        {3_e, 1_v, 2_v},
        {4_e, 2_v, 1_v},
        {5_e, 2_v, 3_v},
        {6_e, 3_v, 2_v},
        {7_e, 3_v, 0_v},
        {8_e, 3_v, 4_v},
        {9_e, 4_v, 5_v},
        {10_e, 5_v, 4_v}
    };

    const std::unique_ptr<Graph> graph = makeRoadGraph("test_3", vertices, edges);

    buildStronglyConnectedComponents(*graph, ROAD_GRAPH_SCC_FILE_NAME);

    const GraphStronglyConnectedComponents graphScc(ROAD_GRAPH_SCC_FILE_NAME);

    UNIT_ASSERT_VALUES_EQUAL(graph->edgesNumber(), graphScc.edgesNumber());

    UNIT_ASSERT_VALUES_EQUAL(graphScc.edgesNumber().value(), 11);

    const StronglyConnectedComponent e0scc = graphScc.component(0_e, AccessId::Automobile);
    for (size_t i = 0; i < 8; ++i) {
        const Edge& edge = edges[i];
        const auto component = graphScc.component(edge.id, AccessId::Automobile);
        UNIT_ASSERT_VALUES_EQUAL(component.id, e0scc.id);
        UNIT_ASSERT_VALUES_EQUAL(component.accessIdMask, AccessId::Automobile);
        UNIT_ASSERT_VALUES_EQUAL(component.size, 8);
        UNIT_ASSERT_VALUES_EQUAL(component.id, graphScc.componentId(edge.id, AccessId::Automobile));
    }

    const StronglyConnectedComponent e8scc = graphScc.component(8_e, AccessId::Automobile);
    UNIT_ASSERT(e8scc.id != e0scc.id);
    UNIT_ASSERT_VALUES_EQUAL(e8scc.accessIdMask, AccessId::Automobile);
    UNIT_ASSERT_VALUES_EQUAL(e8scc.size, 1);
    UNIT_ASSERT_VALUES_EQUAL(e8scc.id, graphScc.componentId(8_e, AccessId::Automobile));

    const StronglyConnectedComponent e9scc = graphScc.component(9_e, AccessId::Automobile);
    UNIT_ASSERT(e9scc.id != e0scc.id);
    UNIT_ASSERT(e9scc.id != e8scc.id);
    for (size_t i = 9; i < 11; ++i) {
        const Edge& edge = edges[i];
        const auto component = graphScc.component(edge.id, AccessId::Automobile);
        UNIT_ASSERT_VALUES_EQUAL(component.id, e9scc.id);
        UNIT_ASSERT_VALUES_EQUAL(component.accessIdMask, AccessId::Automobile);
        UNIT_ASSERT_VALUES_EQUAL(component.size, 2);
        UNIT_ASSERT_VALUES_EQUAL(component.id, graphScc.componentId(edge.id, AccessId::Automobile));
    }
}

};

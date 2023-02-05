#include <maps/tools/conn_checker/graph_connectivity_checker/lib/graph_connectivity.h>

#include <maps/libs/log8/include/log8.h>
#include <maps/libs/road_graph/include/graph.h>
#include <maps/libs/road_graph/serialization/include/serialization.h>

#include <library/cpp/testing/unittest/registar.h>

#include <type_traits>
#include <set>

using namespace maps::road_graph;

using namespace maps::geolib3;

using namespace maps::road_graph::literals;

using namespace maps::road_graph_connectivity;

void addEdge(
    MutableGraph& graph,
    EdgeId edgeId,
    VertexId source,
    VertexId target)
{
    graph.setEdge(Edge{edgeId, source, target}, true /* Base */);

    MutableEdgeData data = {};
    data.accessIdMask = AccessId::Automobile;
    data.geometry = Polyline2(std::vector<Point2>{
        Point2{0.0, 0.0},
        Point2{0.0, 0.0}});
    graph.setEdgeData(edgeId, data);
}

// builds test graph as circle, where the last edge connects the last and the first vertices
std::unique_ptr<Graph> buildTestGraph(size_t edgeCount)
{
    MutableGraph mutableGraph("test_traits", edgeCount, edgeCount, 0);
    mutableGraph.setBuiltForAccessIdMask(AccessId::Automobile | AccessId::Truck | AccessId::Taxi);

    for (size_t vertex = 0; vertex < edgeCount; ++vertex) {
        mutableGraph.setVertexGeometry(VertexId(vertex) , Point2{0, 0});
    }

    for (size_t edge = 0; edge < edgeCount; ++edge) {
        auto from = VertexId(edge);
        auto to = VertexId((edge + 1) % edgeCount);
        addEdge(mutableGraph, EdgeId(edge), from, to);
    }
    return serialize(mutableGraph);
}

std::vector<ComponentId> addComponents(
        GraphConnectivity& connectivity,
        const Graph& graph,
        AccessId accessId,
        const std::vector<std::vector<EdgeId>>& componentData)
{
    std::vector<Component> components;
    components.reserve(componentData.size());

    for (const auto& edgeIds: componentData) {
        ComponentBuilder builder;
        for (EdgeId edgeId: edgeIds) {
            builder.addEdge(
                graph.edge(edgeId),
                graph.edgeData(edgeId).polyline());
        }
        components.emplace_back(std::move(builder).build());
    }
    return connectivity.addComponents(std::move(components), accessId);
}

Y_UNIT_TEST_SUITE(GraphConnectivityCheck)
{

Y_UNIT_TEST(addComponents)
{
    std::unique_ptr<Graph> graphPointer = buildTestGraph(3);
    UNIT_ASSERT(graphPointer);
    const Graph& graph = *graphPointer;
    GraphConnectivity connectivity(graph);

    std::vector component0 = {0_e, 1_e};
    std::vector component1 = {2_e};

    auto carComponentIds = addComponents(
        connectivity,
        graph,
        AccessId::Automobile,
        { component0 });

    auto truckComponentIds = addComponents(
        connectivity,
        graph,
        AccessId::Truck,
        { component0, component1});

    auto taxiComponentIds = addComponents(
        connectivity,
        graph,
        AccessId::Taxi,
        { component1 });

    UNIT_ASSERT_VALUES_EQUAL(carComponentIds.size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(truckComponentIds.size(), 2);
    UNIT_ASSERT_VALUES_EQUAL(taxiComponentIds.size(), 1);

    UNIT_ASSERT_VALUES_EQUAL(connectivity.componentIds().size(), 2);
    UNIT_ASSERT_VALUES_EQUAL(connectivity.componentsNumber(), 2);

    const auto& autoIds = connectivity.componentIds(AccessId::Automobile);
    UNIT_ASSERT_EQUAL(
        std::set(autoIds.begin(), autoIds.end()),
        std::set(carComponentIds.begin(), carComponentIds.end()));

    const auto& truckIds = connectivity.componentIds(AccessId::Truck);
    UNIT_ASSERT_EQUAL(
        std::set(truckIds.begin(), truckIds.end()),
        std::set(truckComponentIds.begin(), truckComponentIds.end()));

    const auto& taxiIds = connectivity.componentIds(AccessId::Taxi);
    UNIT_ASSERT_EQUAL(
        std::set(taxiIds.begin(), taxiIds.end()),
        std::set(taxiComponentIds.begin(), taxiComponentIds.end()));

    const Component& carComponent0 = connectivity.component(carComponentIds[0]);
    UNIT_ASSERT_VALUES_EQUAL(carComponent0.edgeIds(), component0);

    const Component& truckComponent0 = connectivity.component(truckComponentIds[0]);
    UNIT_ASSERT_VALUES_EQUAL(truckComponent0.edgeIds(), component0);

    const Component& truckComponent1 = connectivity.component(truckComponentIds[1]);
    UNIT_ASSERT_VALUES_EQUAL(truckComponent1.edgeIds(), component1);

    const Component& taxiComponent0 = connectivity.component(taxiComponentIds[0]);
    UNIT_ASSERT_VALUES_EQUAL(taxiComponent0.edgeIds(), component1);
}

Y_UNIT_TEST(checkVertexToComponentIdMapping)
{
    // (0_v)---0_e---(1_v)
    //   |             |
    //   |             |
    //  5_e           1_e
    //   |             |
    //   |             |
    // (5_v)         (2_v)
    //   |             |
    //   |             |
    //  4_e           2_e
    //   |             |
    //   |             |
    // (4_v)---3_e---(3_v)

    std::unique_ptr<Graph> graphPointer = buildTestGraph(6);
    UNIT_ASSERT(graphPointer);
    const Graph& graph = *graphPointer;
    GraphConnectivity connectivity(graph);

    std::vector carComponent0 = {0_e, 1_e};
    std::vector carComponent1 = {1_e, 2_e};
    std::vector taxiComponent = {4_e};

    addComponents(
        connectivity,
        graph,
        AccessId::Automobile,
        {carComponent0, carComponent1});

    addComponents(
        connectivity,
        graph,
        AccessId::Taxi,
        {taxiComponent});

    INFO() << "Checking car vertex to component vector mapping";
    auto carVertexComponentIds = connectivity.verticesComponentIds(AccessId::Automobile);

    UNIT_ASSERT_VALUES_EQUAL(carVertexComponentIds.at(0).size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(carVertexComponentIds.at(1).size(), 2);
    UNIT_ASSERT_VALUES_EQUAL(carVertexComponentIds.at(2).size(), 2);
    UNIT_ASSERT_VALUES_EQUAL(carVertexComponentIds.at(3).size(), 1);

    ComponentId carVertex0ComponentId = carVertexComponentIds.at(0).front();
    const Component& carVertex0Component = connectivity.component(carVertex0ComponentId);
    UNIT_ASSERT_VALUES_EQUAL(carVertex0Component.edgeIds(), carComponent0);

    INFO() << "Checking taxi vertex to component vector mapping";
    auto taxiVertexComponentIds = connectivity.verticesComponentIds(AccessId::Taxi);
    UNIT_ASSERT_VALUES_EQUAL(taxiVertexComponentIds.at(4).size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(taxiVertexComponentIds.at(5).size(), 1);

    ComponentId taxiComponentId = taxiVertexComponentIds.at(4).front();
    UNIT_ASSERT_VALUES_EQUAL(taxiComponentId, taxiVertexComponentIds.at(5).front());
    UNIT_ASSERT_VALUES_EQUAL(taxiComponent, connectivity.component(taxiComponentId).edgeIds());
}

}

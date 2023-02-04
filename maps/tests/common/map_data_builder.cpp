#include "map_data_builder.h"

#include <maps/libs/edge_persistent_index/packer/lib/include/persistent_index_builder.h>
#include <yandex/maps/renderer/proj/mercator.h>
#include <yandex/maps/renderer/proj/tile.h>


namespace maps::jams::renderer::tests {

TestGraphBuilder::TestGraphBuilder(size_t edgesNumber)
    : graph_("test_trf", edgesNumber + 1, edgesNumber, 0)
    , edgesNumber_(edgesNumber)
    , vertexId_(0)
    , edgeId_(0)
    , vertex_(0, 0)
{
    graph_.setVertexGeometry(vertexId_, vertex_);
}

std::pair<road_graph::EdgeId, geolib3::Point2> TestGraphBuilder::addEdge(
    geolib3::Point2 mercator,
    int category)
{
    ASSERT(edgeId_.value() < edgesNumber_);

    auto lonLat =
        maps::renderer::proj::mercToLonLat({mercator.x(), mercator.y()});
    geolib3::Point2 nextVertex{lonLat.x, lonLat.y};
    graph_.setVertexGeometry(vertexId_ + 1, nextVertex);

    graph_.setEdge({edgeId_, vertexId_, vertexId_ + 1}, true);

    road_graph::MutableEdgeData edgeData{};
    edgeData.category = category;
    edgeData.geometry.add(vertex_);
    edgeData.geometry.add(nextVertex);
    graph_.setEdgeData(edgeId_, edgeData);

    vertex_ = nextVertex;
    vertexId_++;
    return {edgeId_++, nextVertex};
}

std::unique_ptr<road_graph::Graph> TestGraphBuilder::serialize() const
{
    ASSERT(edgeId_.value() == edgesNumber_);
    return road_graph::serialize(graph_);
}

std::unique_ptr<road_graph::PersistentIndex> makePersistentIndex(
    size_t edgesNumber)
{
    road_graph::PersistentIndexBuilder persistenIndexBuilder("test_trf");
    for (size_t index = 0; index < edgesNumber; ++index) {
        persistenIndexBuilder.setEdgePersistentId(
            road_graph::EdgeId(index), road_graph::LongEdgeId(index));
    }
    return std::make_unique<road_graph::PersistentIndex>(
        persistenIndexBuilder.build());
}

void buildMapData(
    const std::vector<TestJam>& testJams,
    const std::vector<TestEvent>& testEvents,
    const MapDataPaths& paths,
    time_t timestamp)
{
    size_t edgesNumber = testEvents.size();
    for (const auto& jam: testJams) {
        edgesNumber += jam.geometry.size();
    }

    TestGraphBuilder graph(edgesNumber);

    common::standalone::ShardedJams jams;
    for (const auto& jam: testJams) {
        std::vector<road_graph::EdgeId> edges;
        graph.addEdge(jam.geometry.at(0), jam.category);
        for (size_t i = 1; i < jam.geometry.size(); ++i) {
            auto edgeId = graph.addEdge(jam.geometry[i], jam.category).first;
            edges.push_back(edgeId);
        }
        jams[jam.category].emplace_back(
            jam.speed,
            jam.severity,
            jam.category,
            jam.iso,
            jam.oneWayRoad,
            0,
            0,
            std::move(edges));
    }
    std::promise<common::standalone::ShardedJams> jamsPromise;
    jamsPromise.set_value(jams);

    std::unique_ptr<road_graph::PersistentIndex> persistentIndex =
        makePersistentIndex(edgesNumber);
    router::Jams events(*persistentIndex);

    for (const auto& event: testEvents) {
        auto [edgeId, lonLat] = graph.addEdge(event.geometry, event.category);
        events.addEvent(
            router::Jams::SegmentId{edgeId, road_graph::SegmentIndex(0)},
            1,
            event.id,
            event.type,
            event.tags,
            event.description,
            lonLat.x(),
            lonLat.y(),
            0,
            event.moderated,
            event.startTime,
            event.endTime,
            event.lanes);
    }

    auto serializedGraph = graph.serialize();

    build(
        serializedGraph.get(),
        jamsPromise.get_future(),
        events,
        [](size_t) -> time_t { return 0; },
        paths,
        timestamp);
}

} // namespace maps::jams::renderer::tests

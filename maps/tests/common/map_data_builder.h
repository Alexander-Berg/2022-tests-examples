#pragma once
#include <maps/jams/libs/joiner/include/types.h>
#include <maps/jams/renderer2/common/create_map/lib/storage_builder.h>
#include <maps/jams/renderer2/common/yacare/lib/i18n.h>
#include <maps/jams/renderer2/common/yacare/lib/map_data.h>
#include <maps/jams/renderer2/common/yacare/lib/util.h>
#include <maps/libs/road_graph/serialization/include/serialization.h>


namespace maps::jams::renderer::tests {

struct TestJam {
    double speed = 60;
    int category = 1;
    common::Severity severity = common::Severity::Free;
    bool oneWayRoad = true;
    std::string iso = "RU";
    std::vector<geolib3::Point2> geometry;
};

struct TestEvent {
    int category = 1;
    std::string id;
    std::string type;
    std::vector<std::string> tags;
    std::string description;
    geolib3::Point2 geometry;
    std::optional<bool> moderated;
    std::optional<time_t> startTime;
    std::optional<time_t> endTime;
    std::vector<std::string> lanes;
};

class TestGraphBuilder {
public:
    TestGraphBuilder(size_t edgesNumber);

    std::pair<road_graph::EdgeId, geolib3::Point2> addEdge(
        geolib3::Point2 mercator,
        int category);

    std::unique_ptr<road_graph::Graph> serialize() const;

private:
    road_graph::MutableGraph graph_;
    size_t edgesNumber_;
    road_graph::VertexId vertexId_;
    road_graph::EdgeId edgeId_;
    geolib3::Point2 vertex_;
};

void buildMapData(
    const std::vector<TestJam>& testJams,
    const std::vector<TestEvent>& testEvents,
    const MapDataPaths& paths,
    time_t timestamp);

} // namespace maps::jams::renderer::tests

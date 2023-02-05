#include "test_graph.h"

#include <yandex/maps/coverage5/builder.h>
#include <maps/libs/geolib/include/bounding_box.h>
#include <maps/libs/geolib/include/point.h>
#include <maps/libs/geolib/include/polyline.h>
#include <maps/libs/geolib/include/polygon.h>
#include <maps/libs/edge_persistent_index/packer/lib/include/persistent_index_builder.h>
#include <maps/libs/locale/include/convert.h>

#include <maps/libs/road_graph/serialization/include/serialization.h>
#include <maps/libs/succinct_rtree/serialization/include/packer.h>

#include <boost/filesystem.hpp>
#include <boost/lexical_cast.hpp>

#include <fstream>
#include <vector>

namespace coverage = maps::coverage5;
namespace geolib = maps::geolib3;
namespace mrg = maps::road_graph;

void buildSmallGraphData()
{
    maps::road_graph::MutableGraph mutableGraph(
        TEST_VERSION, TEST_VERTEX_DATA.size(), TEST_EDGES.size(), 2);
    maps::road_graph::PersistentIndexBuilder persistentIndexBuilder(TEST_VERSION);

    uint32_t i = 0;
    for (const auto& v : TEST_VERTEX_DATA) {
        mutableGraph.setVertexGeometry(mrg::VertexId{i++}, v);
    }

    persistentIndexBuilder.reserve(TEST_EDGES.size());
    for (const auto& e : TEST_EDGES) {
        maps::road_graph::MutableEdgeData data = maps::road_graph::MutableEdgeData();

        // We set some attributes to check the output edge data is written
        // correctly
        if (e.id == mrg::EdgeId{0}) {
            data.type = maps::road_graph::EdgeType::Roundabout;
            data.structType = maps::road_graph::EdgeStructType::Tunnel;
            data.category = 2;
            data.endsWithTrafficLight = true;
            data.endsWithTrafficLightInReverseDirection = false;
            data.isToll = true;
            data.roads.reserve(1);
            data.roads.push_back(mrg::RoadId{1});
        }

        if (e.id == mrg::EdgeId{2}) {
            data.endsWithTrafficLightInReverseDirection = true;
        }

        if (e.id == mrg::EdgeId{4}) {
            data.endsWithTrafficLightInReverseDirection = false;
        }

        geolib::Polyline2 geometry(geolib::PointsVector({
            TEST_VERTEX_DATA[e.source.value()], TEST_VERTEX_DATA[e.target.value()]}));
        if (TEST_EDGE_GEOMETRIES.count(e.id) > 0) {
            geometry = geolib::Polyline2(TEST_EDGE_GEOMETRIES.at(e.id));
        }
        data.geometry = geometry;
        data.length = geoLength(geometry);
        data.accessIdMask = maps::road_graph::AccessId::Automobile;
        data.speed = TEST_EDGE_SPEED;

        maps::road_graph::Edge edge;
        edge.id = e.id;
        edge.source = e.source;
        edge.target = e.target;

        if (e.base == e.id) {
            mutableGraph.setEdge(edge, true /* is base */);
            if (e.reverse >= e.id) {
                if (TEST_LANES.count(e.id) > 0) {
                    data.lanes = TEST_LANES.at(e.id);
                }
                if (e.reverse != e.id && TEST_LANES.count(e.reverse) > 0) {
                    data.lanesInReverseDirection = TEST_LANES.at(e.reverse);
                }
                if (e.reverse != e.id) {
                    data.accessIdMaskInReverseDirection = maps::road_graph::AccessId::Automobile;
                }
                mutableGraph.setEdgeData(e.id, data);
            }
        } else {
            mutableGraph.setEdge(edge, false /* not base */);
        }
        if (e.reverse != e.id) {
            mutableGraph.setEdgeReverse(e.id, e.reverse);
        }
        persistentIndexBuilder.setEdgePersistentId(
            e.id, mrg::LongEdgeId{e.id.value()});
    }

    for (const auto& kv: TEST_MANOEUVRE_ANNOTATIONS) {
        mutableGraph.addDirectionAnnotation(kv.first, kv.second);
    }

    mutableGraph.addRoadToponymTranslation(
        mrg::RoadId{0},
        maps::locale::to<maps::locale::Locale>("en"),
        "test"
    );

    mutableGraph.addRoadToponymTranslation(
        mrg::RoadId{1},
        maps::locale::to<maps::locale::Locale>("ru"),
        "тест"
    );

    mutableGraph.addRoadToponymTranslation(
        mrg::RoadId{1},
        maps::locale::to<maps::locale::Locale>("en"),
        "test"
    );

    std::vector<VertexEdges> vertexEdges(TEST_VERTEX_DATA.size());
    for (const auto& e: TEST_EDGES) {
        vertexEdges[e.source.value()].outEdges.push_back(e.id);
        vertexEdges[e.target.value()].inEdges.push_back(e.id);
    }

    for (size_t i = 0; i < TEST_VERTEX_DATA.size(); ++i) {
        for (size_t from = 0; from < vertexEdges[i].inEdges.size(); ++from) {
            for (size_t to = 0; to < vertexEdges[i].outEdges.size(); ++to) {
                const auto fromEdgeId = vertexEdges[i].inEdges[from];
                const auto toEdgeId = vertexEdges[i].outEdges[to];
                const auto accessPass = turnPenaltyOracle().requiresAccessPass(
                        fromEdgeId, toEdgeId);
                if (accessPass) {
                    mutableGraph.addAccessPass(
                        mrg::EdgeId{fromEdgeId},
                        mrg::EdgeId{toEdgeId},
                        mrg::AccessId::Automobile);
                }
            }
        }
    }

    maps::road_graph::serialize(mutableGraph, TEST_ROAD_GRAPH_PATH);
    persistentIndexBuilder.save(TEST_PERSISTENT_INDEX_PATH);
}

void buildSmallGraphRTree(const maps::road_graph::Graph& roadGraph)
{
    maps::succinct_rtree::packRtree(roadGraph, TEST_RTREE_PATH);
}

void buildSmallGraphCoverage(const geolib::Polygon2& polygon)
{
    auto cvBuilder = coverage::dataLayerBuilder(
        TEST_COVERAGE_LAYER_PATH,
        TEST_COVERAGE_LAYER,
        TEST_VERSION,
        boost::none);
    cvBuilder->addRegion(
        TEST_REGION_ID,
        boost::none,
        boost::none,
        "",
        {},
        geolib::MultiPolygon2({polygon}));
    cvBuilder->build();
}

float TurnPenaltyOracle::operator()(mrg::EdgeId from, mrg::EdgeId to) const
{
    const auto key = std::make_pair(from, to);
    auto iter = penalties_.find(key);

    if (iter == penalties_.end()) {
        iter = penalties_.emplace(key, penalties_.size() * 10).first;
    }

    return iter->second;
}

bool TurnPenaltyOracle::requiresAccessPass(mrg::EdgeId from, mrg::EdgeId to) const
{
    const auto key = std::make_pair(from, to);
    auto iter = accessPass_.find(key);

    if (iter == accessPass_.end()) {
        iter = accessPass_.emplace(key, accessPass_.size() % 2).first;
    }

    return iter->second;
}

TurnPenaltyOracle& turnPenaltyOracle() {
    static TurnPenaltyOracle oracle;
    return oracle;
}

void buildSmallGraph() {
    boost::filesystem::create_directory(TEST_DIR);

    geolib::BoundingBox graphBBox(TEST_VERTEX_DATA[0], TEST_VERTEX_DATA[0]);
    for (size_t i = 0; i != OUTSIDE_VERTICES_START; ++i) {
        graphBBox = geolib::expand(graphBBox, TEST_VERTEX_DATA[i]);
    }
    graphBBox = geolib::resizeByValue(graphBBox, 1.);

    buildSmallGraphData();

    maps::road_graph::Graph roadGraph(TEST_ROAD_GRAPH_PATH);

    buildSmallGraphRTree(roadGraph);
    buildSmallGraphCoverage(graphBBox.polygon());
}

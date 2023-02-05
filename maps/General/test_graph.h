#pragma once

#include <yandex/maps/coverage5/region.h>
#include <maps/libs/geolib/include/point.h>

#include <maps/libs/road_graph/include/direction_annotation.h>
#include <maps/libs/road_graph/include/lane.h>
#include <maps/libs/road_graph/include/types.h>

#include <string>

namespace geolib = maps::geolib3;
namespace mrg = maps::road_graph;
// Test graph:
//                               --E2,E3->
//                       ----------<--->-----------
//                       |       <-E8,E9--        |
//                       |                        |
//                       |                        |
//           --E19->     |  --E0->   V1  --E5->   |    --E13->
//     V5 * --------> V0 *-----------*------------* V3 --------> * V5
//         \    /        |  <-E4--      <-E10--  /|   \    /
//  --E20-> \  /       | |^       --E7->        / | ^  \  / --E15->
//           \/        E1|E6  -----------------/  |E16  \/
//           /\        V ||  /    <-E11--       | | |   /\
//  --E21-> /  \         |  /                  E12|    /  \ --E17->
//         /    \        | /                    V |   /    \
//     V6 * --------> V2 *------------------------* V4 --------> * V6
//           --E22->              <-E15--               --E18->
//
// Non-base edges should follow their base!
// E2 is base for E3
// E8 is base for E9
// E2 is reverse for E8
//
// Not pictured is V5 and V6 which are located elsewhere and have
// edges that cross the region border.

const float TEST_EDGE_SPEED = 10;

const std::vector<geolib::Point2> TEST_VERTEX_DATA = {
    {-10, 15}, // V0
    {0, 15}, // V1
    {-10.001, 10}, // V2
    {20, 15.004}, // V3
    {20, 10},  // V4
    {30, 30}, // V5,
    {-30, -30}, // V6
};

const size_t OUTSIDE_VERTICES_START = 5;

struct Edge {
    Edge(uint32_t id, uint32_t source, uint32_t target, uint32_t base, uint32_t reverse)
        : id(mrg::EdgeId{id})
        , source(mrg::VertexId{source})
        , target(mrg::VertexId{target})
        , base(mrg::EdgeId{base})
        , reverse(mrg::EdgeId{reverse})
    {}

    mrg::EdgeId id;
    mrg::VertexId source;
    mrg::VertexId target;
    mrg::EdgeId base;
    // Reverse pointing to itself means no reverse
    mrg::EdgeId reverse;
};

const std::vector<Edge> TEST_EDGES = {
    { 0,  0,  1,  0,  0}, // E0
    { 1,  0,  2,  1,  1}, // E1
    { 2,  0,  3,  2,  8}, // E2
    { 3,  0,  3,  2,  8}, // E3
    { 4,  1,  0,  4,  4}, // E4
    { 5,  1,  3,  5,  5}, // E5
    { 6,  2,  0,  6,  6}, // E6
    { 7,  2,  3,  7,  7}, // E7
    { 8,  3,  0,  8,  2}, // E8
    { 9,  3,  0,  8,  2}, // E9
    {10,  3,  1, 10, 10}, // E10
    {11,  3,  2, 11, 11}, // E11
    {12,  3,  4, 12, 12}, // E12
    {13,  3,  5, 13, 13}, // E13
    {14,  3,  6, 14, 14}, // E14
    {15,  4,  2, 15, 15}, // E15
    {16,  4,  3, 16, 16}, // E16
    {17,  4,  5, 17, 17}, // E17
    {18,  4,  6, 18, 18}, // E18
    {19,  5,  1, 19, 19}, // E19
    {20,  5,  2, 20, 20}, // E20
    {21,  6,  1, 21, 21}, // E21
    {22,  6,  2, 22, 22}, // E22
};

// Edge id -> geometry
// Non-specified edges consist of source and target only
const std::map<mrg::EdgeId, std::vector<geolib::Point2>> TEST_EDGE_GEOMETRIES = {
    {mrg::EdgeId{2}, {{-10, 15}, {-20, 0}, {40, 30}, {20, 15.004}}},
    {mrg::EdgeId{15}, {{20, 10}, {15, 10}, {0, 5}, {-10.001, 10}}}
};

// Base edge id -> lanes
const std::map<mrg::EdgeId, std::vector<mrg::Lane>> TEST_LANES = {
    {
        mrg::EdgeId{0},
        {
            mrg::Lane{mrg::LaneKind::PlainLane,
                static_cast<uint64_t>(mrg::Direction::StraightAhead)}
        }
    },
    {
        mrg::EdgeId{1},
        {
            mrg::Lane{mrg::LaneKind::PlainLane,
                static_cast<uint64_t>(mrg::Direction::Left45)}
        }
    },
    {
        mrg::EdgeId{2},
        {
            mrg::Lane{mrg::LaneKind::PlainLane,
                static_cast<uint64_t>(mrg::Direction::StraightAhead)},
            mrg::Lane{mrg::LaneKind::PlainLane,
                static_cast<uint64_t>(mrg::Direction::Right135)},
            mrg::Lane{mrg::LaneKind::PlainLane,
                static_cast<uint64_t>(mrg::Direction::Right90)}
        }
    },
    // { 4, {}}, // assume lanes are unknown for edge 4
    {
        mrg::EdgeId{5},
        {
            mrg::Lane{mrg::LaneKind::PlainLane,
                static_cast<uint64_t>(mrg::Direction::Left90) |
                static_cast<uint64_t>(mrg::Direction::Right90) |
                static_cast<uint64_t>(mrg::Direction::Right45)}
        }
    },
    {
        mrg::EdgeId{6},
        {
            mrg::Lane{mrg::LaneKind::BikeLane,
                static_cast<uint64_t>(mrg::Direction::StraightAhead) |
                static_cast<uint64_t>(mrg::Direction::Right90)},
            mrg::Lane{mrg::LaneKind::PlainLane,
                static_cast<uint64_t>(mrg::Direction::Right90)}
        }
    },
    {
        mrg::EdgeId{7},
        {
            mrg::Lane{mrg::LaneKind::PlainLane,
                static_cast<uint64_t>(mrg::Direction::Left45) |
                static_cast<uint64_t>(mrg::Direction::Left135) |
                static_cast<uint64_t>(mrg::Direction::Right45)}
        }
    },
    {
        mrg::EdgeId{8},
        {
            mrg::Lane{mrg::LaneKind::PlainLane,
                static_cast<uint64_t>(mrg::Direction::StraightAhead) |
                static_cast<uint64_t>(mrg::Direction::Left90)},
            mrg::Lane{mrg::LaneKind::PlainLane,
                static_cast<uint64_t>(mrg::Direction::StraightAhead)}
        }
    },
    {
        mrg::EdgeId{10},
        {
            mrg::Lane{mrg::LaneKind::PlainLane,
                static_cast<uint64_t>(mrg::Direction::StraightAhead)}
        }
    },
    {
        mrg::EdgeId{11},
        {
            mrg::Lane{mrg::LaneKind::PlainLane,
                static_cast<uint64_t>(mrg::Direction::Right45)}
        }
    },
    {
        mrg::EdgeId{12},
        {
            mrg::Lane{mrg::LaneKind::PlainLane,
                static_cast<uint64_t>(mrg::Direction::Right45)},
            mrg::Lane{mrg::LaneKind::PlainLane,
                static_cast<uint64_t>(mrg::Direction::Right45)}
        }
    },
    {
        mrg::EdgeId{13},
        {}
    },
    {
        mrg::EdgeId{14},
        {
            mrg::Lane{mrg::LaneKind::PlainLane,
                static_cast<uint64_t>(mrg::Direction::Left45) |
                static_cast<uint64_t>(mrg::Direction::Left90)},
            mrg::Lane{mrg::LaneKind::PlainLane,
                static_cast<uint64_t>(mrg::Direction::StraightAhead) |
                static_cast<uint64_t>(mrg::Direction::Left90)},
            mrg::Lane{mrg::LaneKind::PlainLane,
                static_cast<uint64_t>(mrg::Direction::StraightAhead)}
        }
    },
    {
        mrg::EdgeId{15},
        {
            mrg::Lane{mrg::LaneKind::TramLane,
                static_cast<uint64_t>(mrg::Direction::StraightAhead) |
                static_cast<uint64_t>(mrg::Direction::Left90) |
                static_cast<uint64_t>(mrg::Direction::Left180)},
            mrg::Lane{mrg::LaneKind::PlainLane,
                static_cast<uint64_t>(mrg::Direction::StraightAhead) |
                static_cast<uint64_t>(mrg::Direction::Right90)}
        }
    },
    {
        mrg::EdgeId{16},
        {
            mrg::Lane{mrg::LaneKind::PlainLane,
                static_cast<uint64_t>(mrg::Direction::StraightAhead) |
                static_cast<uint64_t>(mrg::Direction::Left90) |
                static_cast<uint64_t>(mrg::Direction::Left180)},
            mrg::Lane{mrg::LaneKind::PlainLane,
                static_cast<uint64_t>(mrg::Direction::StraightAhead) |
                static_cast<uint64_t>(mrg::Direction::Right90)}
        }
    },
    {
        mrg::EdgeId{17},
        {
            mrg::Lane{mrg::LaneKind::PlainLane,
                static_cast<uint64_t>(mrg::Direction::StraightAhead) |
                static_cast<uint64_t>(mrg::Direction::Left90) |
                static_cast<uint64_t>(mrg::Direction::Left180)},
            mrg::Lane{mrg::LaneKind::PlainLane,
                static_cast<uint64_t>(mrg::Direction::StraightAhead) |
                static_cast<uint64_t>(mrg::Direction::Right90)}
        }
    },
    {
        mrg::EdgeId{18},
        {
            mrg::Lane{mrg::LaneKind::PlainLane,
                static_cast<uint64_t>(mrg::Direction::StraightAhead) |
                static_cast<uint64_t>(mrg::Direction::Left90) |
                static_cast<uint64_t>(mrg::Direction::Left180)},
            mrg::Lane{mrg::LaneKind::BusLane,
                static_cast<uint64_t>(mrg::Direction::StraightAhead) |
                static_cast<uint64_t>(mrg::Direction::Right90)}
        }
    },
    {
        mrg::EdgeId{19},
        {
            mrg::Lane{mrg::LaneKind::PlainLane,
                static_cast<uint64_t>(mrg::Direction::StraightAhead) |
                static_cast<uint64_t>(mrg::Direction::Left90) |
                static_cast<uint64_t>(mrg::Direction::Left180)},
            mrg::Lane{mrg::LaneKind::PlainLane,
                static_cast<uint64_t>(mrg::Direction::StraightAhead) |
                static_cast<uint64_t>(mrg::Direction::Right90)}
        }
    },
    {
        mrg::EdgeId{20},
        {
            mrg::Lane{mrg::LaneKind::PlainLane,
                static_cast<uint64_t>(mrg::Direction::StraightAhead) |
                static_cast<uint64_t>(mrg::Direction::Left90) |
                static_cast<uint64_t>(mrg::Direction::Left180)},
            mrg::Lane{mrg::LaneKind::PlainLane,
                static_cast<uint64_t>(mrg::Direction::StraightAhead) |
                static_cast<uint64_t>(mrg::Direction::Right90)}
        }
    },
    {
        mrg::EdgeId{21},
        {
            mrg::Lane{mrg::LaneKind::PlainLane,
                static_cast<uint64_t>(mrg::Direction::StraightAhead) |
                static_cast<uint64_t>(mrg::Direction::Left90) |
                static_cast<uint64_t>(mrg::Direction::Left180)},
            mrg::Lane{mrg::LaneKind::PlainLane,
                static_cast<uint64_t>(mrg::Direction::StraightAhead) |
                static_cast<uint64_t>(mrg::Direction::Right90)}
        }
    },
    {
        mrg::EdgeId{22},
        {
            mrg::Lane{mrg::LaneKind::BusLane,
                static_cast<uint64_t>(mrg::Direction::StraightAhead) |
                static_cast<uint64_t>(mrg::Direction::Left90) |
                static_cast<uint64_t>(mrg::Direction::Left180)},
            mrg::Lane{mrg::LaneKind::PlainLane,
                static_cast<uint64_t>(mrg::Direction::StraightAhead) |
                static_cast<uint64_t>(mrg::Direction::Right90)}
        }
    }
};

const std::map<std::vector<mrg::EdgeId>, mrg::DirectionAnnotation> TEST_MANOEUVRE_ANNOTATIONS = {
    {{mrg::EdgeId{8}, mrg::EdgeId{0}}, {0, 0, mrg::Direction::Left90}},
    {{mrg::EdgeId{8}, mrg::EdgeId{1}}, {0, 0, mrg::Direction::StraightAhead}},
    {{mrg::EdgeId{16}, mrg::EdgeId{8}}, {0, 1, mrg::Direction::StraightAhead}},
    {{mrg::EdgeId{16}, mrg::EdgeId{12}}, {0, 0, mrg::Direction::Left180}},
    {{mrg::EdgeId{16}, mrg::EdgeId{11}}, {0, 0, mrg::Direction::Left135}},
    {{mrg::EdgeId{16}, mrg::EdgeId{13}}, {1, 1, mrg::Direction::Right90}},
    {{mrg::EdgeId{5}, mrg::EdgeId{8}}, {0, 0, mrg::Direction::Left90}},
    {{mrg::EdgeId{5}, mrg::EdgeId{12}}, {1, 1, mrg::Direction::Right90}},
};

const std::string TEST_VERSION = "test";
const std::string TEST_DIR = "genfiles";
const std::string TEST_ROAD_GRAPH_PATH = "genfiles/road_graph.fb";
const std::string TEST_PERSISTENT_INDEX_PATH = "genfiles/persistentidx.fb";
const std::string TEST_RTREE_PATH = "genfiles/rtree.fb";
const std::string TEST_COVERAGE_LAYER = "layer";
const std::string TEST_COVERAGE_DIR = TEST_DIR;
const std::string TEST_COVERAGE_LAYER_PATH = "genfiles/layer.mms.1";
const maps::coverage5::RegionId TEST_REGION_ID = 1;

struct VertexEdges {
    std::vector<mrg::EdgeId> inEdges;
    std::vector<mrg::EdgeId> outEdges;
};

class TurnPenaltyOracle {
public:
    float operator()(mrg::EdgeId from, mrg::EdgeId to) const;
    bool requiresAccessPass(mrg::EdgeId from, mrg::EdgeId to) const;

private:
    mutable std::map<std::pair<mrg::EdgeId, mrg::EdgeId>, float> penalties_;
    mutable std::map<std::pair<mrg::EdgeId, mrg::EdgeId>, bool> accessPass_;
};

TurnPenaltyOracle& turnPenaltyOracle();

// Uses the credentials above
void buildSmallGraph();

#include "../../test_types/save_edge_test_data.h"

#include "../suite.h"

#include <yandex/maps/wiki/topo/common.h>

#include <maps/libs/geolib/include/point.h>
#include <maps/libs/geolib/include/segment.h>
#include <maps/libs/geolib/include/polyline.h>

#include <vector>

using namespace maps::wiki::topo;
using namespace maps::wiki::topo::test;
using namespace maps::geolib3;

using maps::wiki::topo::EXISTS;
using maps::wiki::topo::NOT_EXISTS;
using maps::wiki::topo::SourceEdgeID;
using maps::wiki::topo::TopologyRestrictions;
using maps::wiki::topo::Limits;
using maps::wiki::topo::SplitPoint;

using maps::wiki::topo::test::MockStorage;
using maps::wiki::topo::test::MockStorageDiff;
using maps::wiki::topo::test::Node;
using maps::wiki::topo::test::Edge;

TEST_SUITE_START(intersection_common, SaveEdgeTestData)

TEST_DATA(Test_1_1)
{
    "Multiple intersections around existing edge polyline node.",
    MockStorage {
        {
            test::Node { 1, {3, 2} },
            test::Node { 2, {5, 4} },
            test::Node { 3, {5, 7} },
            test::Node { 4, {8, 4} },
            test::Node { 5, {12, 4} },
            test::Node { 6, {13, 6} },
            test::Node { 7, {15, 4} }
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{3, 2}, {5, 4}} } },
            test::Edge {2, 2, 3, Polyline2 { {{5, 4}, {3, 7}, {6, 5}, {8, 6}, {5, 7}} } },
            test::Edge {3, 4, 5, Polyline2 { PointsVector {{8, 4}, {12, 4}} } },
            test::Edge {4, 5, 6, Polyline2 { {{12, 4}, {10, 6}, {13, 6}} } },
            test::Edge {5, 5, 7, Polyline2 { {{12, 4}, {11, 2}, {13, 1}, {15, 4}} } }
        }
    },
    SourceEdgeID { 205, NOT_EXISTS },
    Polyline2 { PointsVector {{10, 7}, {8 - (1e-3 - 1e-4), 6}, {10, 5}} }, // new polyline
    Polyline2 { PointsVector {{10, 7}, {8, 6}, {10, 5}} }, // aligned polyline
    PointsVector {}, // splits requested
    TopologyRestrictions {
        1e-3, // tolerance
        1e-3, // junction gravity
        1e-3, // vertex gravity
        1e-3, // group junction gravity
        1e-3, // group junction snap to vertex
        Limits<double> { 1e-3 }, // segment
        Limits<double> { 1e-3 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {
        { SourceEdgeID { 205, NOT_EXISTS }, EdgeIDVector {205, 210} },
        { SourceEdgeID { 2, EXISTS }, EdgeIDVector {2, 200} }
    },
    MockStorageDiff {
        {
            {100, test::Node { 100, {8, 6} } },
            {105, test::Node { 105, {10, 7} } },
            {110, test::Node { 110, {10, 5} } }
        },
        {
            {2, test::Edge {2, 2, 100, Polyline2 { {{5, 4}, {3, 7}, {6, 5}, {8, 6}} } } },
            {200, test::Edge {200, 100, 3, Polyline2 { PointsVector {{8, 6}, {5, 7}} } } },
            {205, test::Edge {205, 105, 100, Polyline2 { PointsVector {{10, 7}, {8, 6}} } } },
            {210, test::Edge {210, 100, 110, Polyline2 { PointsVector {{8, 6}, {10, 5}} } } }
        }
    },
    PrintInfo {
        PointsVector { {8, 6} },
        std::vector<PointGravity> {
            {{8, 6}, GravityType::VertexGravity}
        },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {} // gravity of created/edited edge
    }
};

TEST_DATA(Test_1_2)
{
    "Multiple intersections around existing edge polyline node.",
    MockStorage {
        {
            test::Node { 1, {3, 2} },
            test::Node { 2, {5, 4} },
            test::Node { 3, {5, 7} },
            test::Node { 4, {8, 4} },
            test::Node { 5, {12, 4} },
            test::Node { 6, {13, 6} },
            test::Node { 7, {15, 4} }
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{3, 2}, {5, 4}} } },
            test::Edge {2, 2, 3, Polyline2 { {{5, 4}, {3, 7}, {6, 5}, {8, 6}, {5, 7}} } },
            test::Edge {3, 4, 5, Polyline2 { PointsVector {{8, 4}, {12, 4}} } },
            test::Edge {4, 5, 6, Polyline2 { {{12, 4}, {10, 6}, {13, 6}} } },
            test::Edge {5, 5, 7, Polyline2 { {{12, 4}, {11, 2}, {13, 1}, {15, 4}} } }
        }
    },
    SourceEdgeID { 205, NOT_EXISTS },
    Polyline2 { PointsVector {{10, 7}, {8 - 1e-3, 6}, {10, 5}} },
    Polyline2 { PointsVector {{10, 7}, {8, 6}, {10, 5}} },
    PointsVector {}, // splits requested
    TopologyRestrictions {
        1e-3, // tolerance
        1e-3, // junction gravity
        1e-3, // vertex gravity
        1e-3, // group junction gravity
        1e-3, // group junction snap to vertex
        Limits<double> { 1e-3 }, // segment
        Limits<double> { 1e-3 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {
        { SourceEdgeID { 205, NOT_EXISTS }, EdgeIDVector {205, 210} },
        { SourceEdgeID { 2, EXISTS }, EdgeIDVector {2, 200} }
    },
    MockStorageDiff {
        {
            {100, test::Node { 100, {8, 6} } },
            {105, test::Node { 105, {10, 7} } },
            {110, test::Node { 110, {10, 5} } }
        },
        {
            {2, test::Edge {2, 2, 100, Polyline2 { {{5, 4}, {3, 7}, {6, 5}, {8, 6}} } } },
            {200, test::Edge {200, 100, 3, Polyline2 { PointsVector {{8, 6}, {5, 7}} } } },
            {205, test::Edge {205, 105, 100, Polyline2 { PointsVector {{10, 7}, {8, 6}} } } },
            {210, test::Edge {210, 100, 110, Polyline2 { PointsVector {{8, 6}, {10, 5}} } } }
        }
    },
    PrintInfo {
        PointsVector { {8, 6} },
        std::vector<PointGravity> {
            {{8, 6}, GravityType::VertexGravity}
        },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {} // gravity of created/edited edge
    }
};

TEST_DATA(Test_1_3)
{
    "Multiple intersections around existing edge polyline node, with overlaps.",
    MockStorage {
        {
            test::Node { 1, {3, 2} },
            test::Node { 2, {5, 4} },
            test::Node { 3, {5, 7} },
            test::Node { 4, {8, 4} },
            test::Node { 5, {12, 4} },
            test::Node { 6, {13, 6} },
            test::Node { 7, {15, 4} }
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{3, 2}, {5, 4}} } },
            test::Edge {2, 2, 3, Polyline2 { {{5, 4}, {3, 7}, {6, 5}, {8, 6}, {5, 7}} } },
            test::Edge {3, 4, 5, Polyline2 { PointsVector {{8, 4}, {12, 4}} } },
            test::Edge {4, 5, 6, Polyline2 { {{12, 4}, {10, 6}, {13, 6}} } },
            test::Edge {5, 5, 7, Polyline2 { {{12, 4}, {11, 2}, {13, 1}, {15, 4}} } }
        }
    },
    SourceEdgeID { 205, NOT_EXISTS },
    Polyline2 { PointsVector {{10, 7}, {8 - (1e-3 - 1e-4) * 2.0 / std::sqrt(5), 6 - (1e-3 - 1e-4) * 1.0 / std::sqrt(5)}, {10, 5}} },
    Polyline2 { PointsVector {{10, 7}, {8, 6}, {10, 5}} },
    PointsVector {}, // splits requested
    TopologyRestrictions {
        1e-3, // tolerance
        1e-3, // junction gravity
        1e-3, // vertex gravity
        1e-3, // group junction gravity
        1e-3, // group junction snap to vertex
        Limits<double> { 1e-3 }, // segment
        Limits<double> { 1e-3 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {
        { SourceEdgeID { 205, NOT_EXISTS }, EdgeIDVector {205, 210} },
        { SourceEdgeID { 2, EXISTS }, EdgeIDVector {2, 200} }
    },
    MockStorageDiff {
        {
            {100, test::Node { 100, {8, 6} } },
            {105, test::Node { 105, {10, 7} } },
            {110, test::Node { 110, {10, 5} } }
        },
        {
            {2, test::Edge {2, 2, 100, Polyline2 { {{5, 4}, {3, 7}, {6, 5}, {8, 6}} } } },
            {200, test::Edge {200, 100, 3, Polyline2 { PointsVector {{8, 6}, {5, 7}} } } },
            {205, test::Edge {205, 105, 100, Polyline2 { PointsVector {{10, 7}, {8, 6}} } } },
            {210, test::Edge {210, 100, 110, Polyline2 { PointsVector {{8, 6}, {10, 5}} } } }
        }
    },
    PrintInfo {
        PointsVector { {8, 6} },
        std::vector<PointGravity> {
            {{8, 6}, GravityType::VertexGravity}
        },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {} // gravity of created/edited edge
    }
};

TEST_DATA(Test_1_4)
{
    "Multiple intersections around existing edge polyline node.",
    MockStorage {
        {
            test::Node { 1, {3, 2} },
            test::Node { 2, {5, 4} },
            test::Node { 3, {5, 7} }
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{3, 2}, {5, 4}} } },
            test::Edge {2, 2, 3, Polyline2 { {{5, 4}, {3, 7}, {6, 5}, {8, 6}, {5, 7}} } }
        }
    },
    SourceEdgeID { 205, NOT_EXISTS },
    Polyline2 { PointsVector {{7, 8}, {8 - (1e-3 - 1e-4) / 2.0, 6}, {9, 8}} },
    Polyline2 { PointsVector {{7, 8}, {8, 6}, {9, 8}} },
    PointsVector {}, // splits requested
    TopologyRestrictions {
        1e-3, // tolerance
        1e-3, // junction gravity
        1e-3, // vertex gravity
        1e-3, // group junction gravity
        1e-3, // group junction snap to vertex
        Limits<double> { 1e-3 }, // segment
        Limits<double> { 1e-3 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {
        { SourceEdgeID { 205, NOT_EXISTS }, EdgeIDVector {205, 210} },
        { SourceEdgeID { 2, EXISTS }, EdgeIDVector {2, 200} }
    },
    MockStorageDiff {
        {
            {100, test::Node { 100, {8, 6} } },
            {105, test::Node { 105, {7, 8} } },
            {110, test::Node { 110, {9, 8} } }
        },
        {
            {2, test::Edge {2, 2, 100, Polyline2 { {{5, 4}, {3, 7}, {6, 5}, {8, 6}} } } },
            {200, test::Edge {200, 100, 3, Polyline2 { PointsVector {{8, 6}, {5, 7}} } } },
            {205, test::Edge {205, 105, 100, Polyline2 { PointsVector {{7, 8}, {8, 6}} } } },
            {210, test::Edge {210, 100, 110, Polyline2 { PointsVector {{8, 6}, {9, 8}} } } }
        }
    },
    PrintInfo {
        PointsVector { {8, 6} },
        std::vector<PointGravity> {
            {{8, 6}, GravityType::VertexGravity}
        },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {} // gravity of created/edited edge
    }
};

TEST_DATA(Test_1_5)
{
    "Multiple intersections around geom polyline node.",
    MockStorage {
        {
            test::Node { 4, {8, 4} },
            test::Node { 5, {12, 4} },
            test::Node { 6, {13, 6} },
            test::Node { 7, {15, 4} }
        },
        {
            test::Edge {3, 4, 5, Polyline2 { PointsVector {{8, 4}, {12, 4}} } },
            test::Edge {4, 5, 6, Polyline2 { {{12, 4}, {10, 6}, {13, 6}} } },
            test::Edge {5, 5, 7, Polyline2 { {{12, 4}, {11, 2}, {13, 1}, {15, 4}} } }
        }
    },
    SourceEdgeID { 205, NOT_EXISTS },
    Polyline2 { PointsVector {{11, 8}, {12, 6}, {13, 8}} },
    Polyline2 { PointsVector {{11, 8}, {12, 6}, {13, 8}} },
    PointsVector {}, // splits requested
    TopologyRestrictions {
        1e-3, // tolerance
        1e-3, // junction gravity
        1e-3, // vertex gravity
        1e-3, // group junction gravity
        1e-3, // group junction snap to vertex
        Limits<double> { 1e-3 }, // segment
        Limits<double> { 1e-3 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {
        { SourceEdgeID { 205, NOT_EXISTS }, EdgeIDVector {205, 210} },
        { SourceEdgeID { 4, EXISTS }, EdgeIDVector {4, 200} }
    },
    MockStorageDiff {
        {
            {100, test::Node { 100, {11, 8} } },
            {105, test::Node { 105, {12, 6} } },
            {110, test::Node { 110, {13, 8} } }
        },
        {
            {4, test::Edge {4, 5, 105, Polyline2 { PointsVector {{12, 4}, {10, 6}, {12, 6}} } } },
            {200, test::Edge {200, 105, 6, Polyline2 { PointsVector {{12, 6}, {13, 6}} } } },
            {205, test::Edge {205, 100, 105, Polyline2 { PointsVector {{11, 8}, {12, 6}} } } },
            {210, test::Edge {210, 105, 110, Polyline2 { PointsVector {{12, 6}, {13, 8}} } } }
        }
    },
    PrintInfo {
        PointsVector { {12, 6} },
        std::vector<PointGravity> {
            {{12, 6}, GravityType::VertexGravity}
        },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {} // gravity of created/edited edge
    }
};

TEST_DATA(Test_1_6)
{
    "Multiple intersections around edge geom polyline node.",
    MockStorage {
        {
            test::Node { 1, {3, 2} },
            test::Node { 2, {5, 4} },
            test::Node { 3, {5, 7} },
            test::Node { 4, {8, 4} },
            test::Node { 5, {12, 4} },
            test::Node { 6, {13, 6} },
            test::Node { 7, {15, 4} }
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{3, 2}, {5, 4}} } },
            test::Edge {2, 2, 3, Polyline2 { {{5, 4}, {3, 7}, {6, 5}, {8, 6}, {5, 7}} } },
            test::Edge {3, 4, 5, Polyline2 { PointsVector {{8, 4}, {12, 4}} } },
            test::Edge {4, 5, 6, Polyline2 { {{12, 4}, {10, 6}, {13, 6}} } },
            test::Edge {5, 5, 7, Polyline2 { {{12, 4}, {11, 2}, {13, 1}, {15, 4}} } }
        }
    },
    SourceEdgeID { 205, NOT_EXISTS },
    Polyline2 { PointsVector {{8 - (1e-3 - 1e-4) * 1.0 / std::sqrt(10.0), 7}, {8 - (1e-3 - 1e-4) * 1.0 / std::sqrt(10.0), 5}} },
    Polyline2 {
        PointsVector {
            {8 - (1e-3 - 1e-4) * 1.0 / std::sqrt(10.0), 7},
            {8, 6},
            {8 - (1e-3 - 1e-4) * 1.0 / std::sqrt(10.0), 5}
        }
    },
    PointsVector {}, // splits requested
    TopologyRestrictions {
        1e-3, // tolerance
        1e-3, // junction gravity
        1e-3, // vertex gravity
        1e-3, // group junction gravity
        1e-3, // group junction snap to vertex
        Limits<double> { 1e-3 }, // segment
        Limits<double> { 1e-3 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {
        { SourceEdgeID { 205, NOT_EXISTS }, EdgeIDVector {205, 210} },
        { SourceEdgeID { 2, EXISTS }, EdgeIDVector {2, 200} }
    },
    MockStorageDiff {
        {
            {100, test::Node { 100, {8 - (1e-3 - 1e-4) * 1.0 / std::sqrt(10.0), 7} } },
            {105, test::Node { 105, {8, 6} } },
            {110, test::Node { 110, {8 - (1e-3 - 1e-4) * 1.0 / std::sqrt(10.0), 5} } }
        },
        {
            {2, test::Edge {2, 2, 105, Polyline2 { PointsVector {{5, 4}, {3, 7}, {6, 5}, {8, 6}} } } },
            {200, test::Edge {200, 105, 3, Polyline2 { PointsVector {{8, 6}, {5, 7}} } } },
            {205, test::Edge {205, 100, 105, Polyline2 { PointsVector {{8 - (1e-3 - 1e-4) * 1.0 / std::sqrt(10.0), 7}, {8, 6}} } } },
            {210, test::Edge {210, 105, 110, Polyline2 { PointsVector {{8, 6}, {8 - (1e-3 - 1e-4) * 1.0 / std::sqrt(10.0), 5}} } } }
        }
    },
    PrintInfo {
        PointsVector { {8, 6} },
        std::vector<PointGravity> {
            {{8, 6}, GravityType::Tolerance}
        },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {} // gravity of created/edited edge
    }
};

namespace {

const double delta_1_6_2 = ((1e-3 - 1e-4) / std::sqrt(2.0)) / std::sqrt(10.0);

} // namespace

TEST_DATA(Test_1_6_2)
{
    "Multiple intersections around edge geom polyline node, distance > gravity.",
    MockStorage {
        {
            test::Node { 4, {8, 4} },
            test::Node { 5, {12, 4} },
            test::Node { 6, {13, 6} },
            test::Node { 7, {15, 4} }
        },
        {
            test::Edge {3, 4, 5, Polyline2 { PointsVector {{8, 4}, {12, 4}} } },
            test::Edge {4, 5, 6, Polyline2 { {{12, 4}, {10, 6}, {13, 6}} } },
            test::Edge {5, 5, 7, Polyline2 { {{12, 4}, {11, 2}, {13, 1}, {15, 4}} } }
        }
    },
    SourceEdgeID { 205, NOT_EXISTS },
    Polyline2 { PointsVector {
        {10 + 2.0 / 3.0 + 3 * delta_1_6_2, 3 + delta_1_6_2},
        {12 + 3 * delta_1_6_2, -1 + delta_1_6_2}}
    },
    Polyline2 {
        PointsVector {
            {10 + 2.0 / 3.0 + 3 * delta_1_6_2, 3 + delta_1_6_2},
            {11, 2},
            {12 + 3 * delta_1_6_2, -1 + delta_1_6_2}
        }
    },
    PointsVector {}, // splits requested
    TopologyRestrictions {
        1e-3, // tolerance
        1e-3, // junction gravity
        1e-3, // vertex gravity
        1e-3, // group junction gravity
        1e-3, // group junction snap to vertex
        Limits<double> { 1e-3 }, // segment
        Limits<double> { 1e-3 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {
        { SourceEdgeID { 205, NOT_EXISTS }, EdgeIDVector {205, 210} },
        { SourceEdgeID { 5, EXISTS }, EdgeIDVector {200, 5} }
    },
    MockStorageDiff {
        {
            {100, test::Node { 100, {10 + 2.0 / 3.0 + 3 * delta_1_6_2, 3 + delta_1_6_2} } },
            {105, test::Node { 105, {11, 2} } },
            {110, test::Node { 110, {12 + 3 * delta_1_6_2, -1 + delta_1_6_2} } }
        },
        {
            {200, test::Edge {200, 5, 105, Polyline2 { PointsVector {{12, 4}, {11, 2}} } } },
            {5, test::Edge {5, 105, 7, Polyline2 { {{11, 2}, {13, 1}, {15, 4}} } } },
            {205, test::Edge {205, 100, 105, Polyline2 { PointsVector {{10 + 2.0 / 3.0 + 3 * delta_1_6_2, 3 + delta_1_6_2}, {11, 2}} } } },
            {210, test::Edge {210, 105, 110, Polyline2 { PointsVector {{11, 2}, {12 + 3 * delta_1_6_2, -1 + delta_1_6_2}} } } }
        }
    },
    PrintInfo {
        PointsVector { {11, 2} },
        std::vector<PointGravity> {
            {{11, 2}, GravityType::Tolerance}
        },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {} // gravity of created/edited edge
    }
};

TEST_SUITE_END(intersection_common)

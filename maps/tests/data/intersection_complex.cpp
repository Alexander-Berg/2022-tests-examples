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

TEST_SUITE_START(intersection_complex, SaveEdgeTestData)

TEST_DATA(Test_1_1)
{
    "Snap end at existing node, with intersection.",
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
    Polyline2 { {{12, 4}, {14, 5}, {10, 5}}},
    Polyline2 { {{12, 4}, {14, 5}, {10, 5}}},
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
        { SourceEdgeID { 4, EXISTS }, EdgeIDVector {200, 4} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {11, 5} } },
            { 105, test::Node { 105, {10, 5} } }
        },
        {
            { 4, test::Edge {4, 100, 6, Polyline2 { {{11, 5}, {10, 6}, {13, 6}} } } },
            { 200, test::Edge {200, 5, 100, Polyline2 { PointsVector {{12, 4}, {11, 5}} } } },
            { 205, test::Edge {205, 5, 100, Polyline2 { {{12, 4}, {14, 5}, {11, 5}} } } },
            { 210, test::Edge {210, 100, 105, Polyline2 { PointsVector {{11, 5}, {10, 5}} } } }
        }
    },
    PrintInfo {
        PointsVector { {12, 4}, {11, 5} },
        std::vector<PointGravity> {
            {Point2 {12, 4}, GravityType::JunctionGravity}
        },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_1_2)
{
    "Snap both ends at existing nodes.",
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
    SourceEdgeID { 100500, NOT_EXISTS },
    Polyline2 { PointsVector {{12 - (1e-3 - 1e-4), 4}, {15 + (1e-3 - 1e-4), 4}}},
    Polyline2 { PointsVector {{12, 4}, {15, 4}}},
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
    SplitEdges {},
    MockStorageDiff {
        {},
        {
            { 200, test::Edge {200, 5, 7, Polyline2 { PointsVector {{12, 4}, {15, 4}} } } }
        }
    },
    PrintInfo {
        PointsVector { {12, 4}, {15, 4} },
        std::vector<PointGravity> {
            {Point2 {12, 4}, GravityType::JunctionGravity},
            {Point2 {15, 4}, GravityType::JunctionGravity}
        },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_1_3)
{
    "Snap both ends at existing nodes with overlapping.",
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
    SourceEdgeID { 7, NOT_EXISTS },
    Polyline2 { PointsVector {{12 - (1e-3 - 1e-4), 4}, {11.5, 3}, {13, 1}, {15 + (1e-3 - 1e-4), 4}}},
    Polyline2 { PointsVector {{12, 4}, {11.5, 3}, {13, 1}, {15, 4}}},
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

        MergeOfOverlappedEdgesPolicy::Allowed
    },
    SplitEdges {
        { SourceEdgeID { 7, NOT_EXISTS }, EdgeIDVector {6, 7, 8} },
        { SourceEdgeID { 5, EXISTS }, EdgeIDVector {6, 5, 8} }
    },
    MockStorageDiff {
        {
            { 8, test::Node { 8, {11.5, 3} } },
            { 9, test::Node { 9, {13, 1} } }
        },
        {
            { 5, test::Edge {5, 8, 9, Polyline2 { PointsVector {{11.5, 3}, {11, 2}, {13, 1}} } } },
            { 6, test::Edge {6, 5, 8, Polyline2 { PointsVector {{12, 4}, {11.5, 3}} } } },
            { 7, test::Edge {7, 8, 9, Polyline2 { PointsVector {{11.5, 3}, {13, 1}} } } },
            { 8, test::Edge {8, 9, 7, Polyline2 { PointsVector {{13, 1}, {15, 4}} } } }
        }
    },
    PrintInfo {
        PointsVector { {12, 4}, {11.5, 3}, {13, 1}, {15, 4} },
        std::vector<PointGravity> {
            {Point2 {12, 4}, GravityType::JunctionGravity},
            {Point2 {15, 4}, GravityType::JunctionGravity},
            {Point2 {13, 1}, GravityType::VertexGravity}
        },
        std::map<EdgeID, GravityTypesSet> {
            {5, GravityTypesSet {GravityType::VertexGravity}},
        },
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_1_4)
{
    "No snap end at existing node, no snap to edge, with intersection, pass through node.",
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
    SourceEdgeID { 200, NOT_EXISTS },
    Polyline2 { PointsVector {{3 - 1e-3 / std::sqrt(2) - 2e-4, 2 + 1e-3 / std::sqrt(2) + 1e-4}, {5, 0}}},
    Polyline2 { PointsVector {{3 - 1e-3 / std::sqrt(2) - 2e-4, 2 + 1e-3 / std::sqrt(2) + 1e-4}, {3, 2}, {5, 0}}},
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
        { SourceEdgeID { 200, NOT_EXISTS }, EdgeIDVector {200, 205} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {3 - 1e-3 / std::sqrt(2) - 2e-4, 2 + 1e-3 / std::sqrt(2) + 1e-4} } },
            { 105, test::Node { 105, {5, 0} } }
        },
        {
            { 200, test::Edge {200, 100, 1, Polyline2 { PointsVector {{3 - 1e-3 / std::sqrt(2) - 2e-4, 2 + 1e-3 / std::sqrt(2) + 1e-4}, {3, 2}} } } },
            { 205, test::Edge {205, 1, 105, Polyline2 { PointsVector {{3, 2}, {5, 0}} } } }
        }
    },
    PrintInfo {
        PointsVector { {3, 2} },
        std::vector<PointGravity> {
            {Point2 {3, 2}, GravityType::Tolerance},
            {Point2 {3 - 1e-3 / std::sqrt(2) - 2e-4, 2 + 1e-3 / std::sqrt(2) + 1e-4}, GravityType::JunctionGravity},
        },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_2_1)
{
    "Snap both ends at existing edges.",
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
    SourceEdgeID { 100500, NOT_EXISTS },
    Polyline2 { PointsVector {{6 + (1e-3 - 1e-4), 5}, {10 - (1e-3 - 1e-4), 6}}},
    Polyline2 { PointsVector {{6, 5}, {10, 6}}},
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
        { SourceEdgeID { 2, EXISTS }, EdgeIDVector {205, 2} },
        { SourceEdgeID { 4, EXISTS }, EdgeIDVector {4, 210} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {6, 5} } },
            { 105, test::Node { 105, {10, 6} } }
        },
        {
            { 200, test::Edge {200, 100, 105, Polyline2 { PointsVector {{6, 5}, {10, 6}} } } },
            { 205, test::Edge {205, 2, 100, Polyline2 { {{5, 4}, {3, 7}, {6, 5}} } } },
            { 2, test::Edge {2, 100, 3, Polyline2 { {{6, 5}, {8, 6}, {5, 7}} } } },
            { 4, test::Edge {4, 5, 105, Polyline2 { PointsVector {{12, 4}, {10, 6}} } } },
            { 210, test::Edge {210, 105, 6, Polyline2 { PointsVector {{10, 6}, {13, 6}} } } }
        }
    },
    PrintInfo {
        PointsVector { {6, 5}, {10, 6} },
        std::vector<PointGravity> {
            {Point2 {6, 5}, GravityType::JunctionGravity},
            {Point2 {10, 6}, GravityType::JunctionGravity}
        },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_2_2)
{
    "Snap end at existing node and other to existing edge.",
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
    SourceEdgeID { 100500, NOT_EXISTS },
    Polyline2 { {{5, 4 - (1e-3 - 1e-4)}, {9, 2}, {9, 4 + (1e-3 - 1e-4)}}},
    Polyline2 { {{5, 4}, {9, 2}, {9, 4}}},
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
        { SourceEdgeID { 3, EXISTS }, EdgeIDVector {205, 3} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {9, 4} } },
        },
        {
            { 200, test::Edge { 200, 2, 100, Polyline2 { {{5, 4}, {9, 2}, {9, 4}} } } },
            { 205, test::Edge { 205, 4, 100, Polyline2 { PointsVector {{8, 4}, {9, 4}} } } },
            { 3, test::Edge { 3, 100, 5, Polyline2 { PointsVector {{9, 4}, {12, 4}} } } }

        }
    },
    PrintInfo {
        PointsVector { {5, 4}, {9, 4} },
        std::vector<PointGravity> {
            {{5, 4}, GravityType::JunctionGravity}
        },
        std::map<EdgeID, GravityTypesSet> {
            {3, GravityTypesSet {GravityType::JunctionGravity}}
        },
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_3_1)
{
    "contr-ERROR 6 existing node is NOT snapping to incident edge, "
        "adding new edge with endpoint snapping to this edge is correct",
    MockStorage {
        {
            test::Node { 1, {3, 2 + (1e-3 + 1e-4) * std::sqrt(2)} },
            test::Node { 2, {5, 2} }
        },
        {
            test::Edge {1, 1, 2, Polyline2 {{ {3, 2 + (1e-3 + 1e-4) * std::sqrt(2)}, {2, 4}, {1, 2}, {5, 2} }}}
        }
    },
    SourceEdgeID { 200, NOT_EXISTS },
    Polyline2 { PointsVector {{3, 2 - (1e-3 - 1e-4)}, {3, 0}}},
    Polyline2 { PointsVector {{3, 2}, {3, 0}}},
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
        { SourceEdgeID { 1, EXISTS }, EdgeIDVector {1, 205} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {3, 2} } },
            { 105, test::Node { 105, {3, 0} } }
        },
        {
            { 200, test::Edge { 200, 100, 105, Polyline2 { PointsVector {{3, 2}, {3, 0}} } } },
            { 1, test::Edge { 1, 1, 100, Polyline2 { {{3, 2 + (1e-3 + 1e-4) * std::sqrt(2)}, {2, 4}, {1, 2}, {3, 2}} } } },
            { 205, test::Edge { 205, 100, 2, Polyline2 { PointsVector {{3, 2}, {5, 2}} } } }
        }
    },
    PrintInfo {
        PointsVector { {3, 2} },
        std::vector<PointGravity> {},
        std::map<EdgeID, GravityTypesSet> {
            {1, GravityTypesSet{GravityType::JunctionGravity}}
        },
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_3_2)
{
    "contr-ERROR 7 existing node is NOT snapping to incident edge, "
        "adding new edge that intersects this edge near this node, but with NO snap, is correct",
    MockStorage {
        {
            test::Node { 1, {3, 2 + (1e-3 + 1e-4) * std::sqrt(2)} },
            test::Node { 2, {5, 2} }
        },
        {
            test::Edge {1, 1, 2, Polyline2 {{ {3, 2 + (1e-3 + 1e-4) * std::sqrt(2)}, {2, 4}, {1, 2}, {5, 2} }}}
        }
    },
    SourceEdgeID { 200, NOT_EXISTS },
    Polyline2 { PointsVector {{1, 1}, {5, 3}} },
    Polyline2 { PointsVector {{1, 1}, {5, 3}} },
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
        { SourceEdgeID { 200, NOT_EXISTS }, EdgeIDVector {200, 205} },
        { SourceEdgeID { 1, EXISTS }, EdgeIDVector {1, 210} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {1, 1} } },
            { 105, test::Node { 105, {3, 2} } },
            { 110, test::Node { 110, {5, 3} } }
        },
        {
            { 200, test::Edge { 200, 100, 105, Polyline2 { PointsVector {{1, 1}, {3, 2}} } } },
            { 205, test::Edge { 205, 105, 110, Polyline2 { PointsVector {{3, 2}, {5, 3}} } } },
            { 1, test::Edge { 1, 1, 105, Polyline2 { {{3, 2 + (1e-3 + 1e-4) * std::sqrt(2)}, {2, 4}, {1, 2}, {3, 2}} } } },
            { 210, test::Edge { 210, 105, 2, Polyline2 { PointsVector {{3, 2}, {5, 2}} } } }
        }
    },
    PrintInfo {
        PointsVector { {3, 2}},
        std::vector<PointGravity> {
            {{3, 2}, GravityType::JunctionGravity}
        },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_3_3)
{
    "contr-ERROR group 3 existing node is NOT snapping to incident edge, "
        "adding new edge that intersects this edge near this node, with snap, is correct",
    MockStorage {
        {
            test::Node { 1, {3, 2 + (1e-3 - 1e-4)} },
            test::Node { 2, {5, 2} }
        },
        {
            test::Edge {1, 1, 2, Polyline2 {{{3, 2 + (1e-3 - 1e-4)}, {2, 4}, {1, 2}, {5, 2}}} }
        }
    },
    SourceEdgeID { 200, NOT_EXISTS },
    Polyline2 { PointsVector {{1, 1}, {5, 3}} },
    Polyline2 { PointsVector {{1, 1}, {3, 2 + (1e-3 - 1e-4)}, {5, 3}} },
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
        { SourceEdgeID { 200, NOT_EXISTS }, EdgeIDVector {200, 205, 210} },
        { SourceEdgeID { 1, EXISTS }, EdgeIDVector {1, 215} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {1, 1} } },
            { 105, test::Node { 105, {1 + 2.0 / (1 + (1e-3 - 1e-4)), 2} } },
            { 110, test::Node { 110, {5, 3} } }
        },
        {
            { 200, test::Edge { 200, 100, 105, Polyline2 { PointsVector {{1, 1}, {1 + 2.0 / (1 + (1e-3 - 1e-4)), 2}} } } },
            { 205, test::Edge { 205, 105, 1, Polyline2 { PointsVector {{1 + 2.0 / (1 + (1e-3 - 1e-4)), 2}, {3, 2 + (1e-3 - 1e-4)}} } } },
            { 210, test::Edge { 210, 1, 110, Polyline2 { PointsVector {{3, 2 + (1e-3 - 1e-4)}, {5, 3}} } } },
            { 1, test::Edge { 1, 1, 105, Polyline2 { {{3, 2 + (1e-3 - 1e-4)}, {2, 4}, {1, 2}, {1 + 2.0 / (1 + (1e-3 - 1e-4)), 2}} } } },
            { 215, test::Edge { 215, 105, 2, Polyline2 { PointsVector {{1 + 2.0 / (1 + (1e-3 - 1e-4)), 2}, {5, 2}} } } }
        }
    },
    PrintInfo {
        PointsVector { {3 - (1e-3 - 1e-4), 2 + (1e-3 - 1e-4)} },
        std::vector<PointGravity> { {{3, 2 + (1e-3 - 1e-4)}, GravityType::JunctionGravity} },
        std::map<EdgeID, GravityTypesSet> {
            {1, GravityTypesSet {GravityType::JunctionGravity}}
        },
        std::set<GravityType> {}
    },
};

TEST_DATA(Test_4_1)
{
    "Complex snap endpoints, pass through, intersections.",
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
    SourceEdgeID { 200, NOT_EXISTS },
    Polyline2 {{{5 - (1e-3 - 1e-4), 4}, {2, 7 - 5e-4}, {8, 7}, {8, 5}, {11, 7}, {11, 6 - (1e-3 - 1e-4)}}},
    Polyline2 {{{5, 4}, {2, 7 - 5e-4}, {3, 7}, {5, 7}, {8, 7}, {8, 6}, {8, 5}, {11, 7}, {11, 6}}},
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
        { SourceEdgeID { 200, NOT_EXISTS }, EdgeIDVector {200, 205, 210, 215} },
        { SourceEdgeID { 2, EXISTS }, EdgeIDVector {220, 2, 225} },
        { SourceEdgeID { 4, EXISTS }, EdgeIDVector {4, 230} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {3, 7} } },
            { 105, test::Node { 105, {8, 6} } },
            { 110, test::Node { 110, {11, 6} } }
        },
        {
            { 200, test::Edge { 200, 2, 100, Polyline2 { {{5, 4}, {2, 7 - 5e-4}, {3, 7}} } } },
            { 205, test::Edge { 205, 100, 3, Polyline2 { PointsVector {{3, 7}, {5, 7}} } } },
            { 210, test::Edge { 210, 3, 105, Polyline2 { {{5, 7}, {8, 7}, {8, 6}} } } },
            { 215, test::Edge { 215, 105, 110, Polyline2 { {{8, 6}, {8, 5}, {11, 7}, {11, 6}} } } },
            { 220, test::Edge { 220, 2, 100, Polyline2 { PointsVector {{5, 4}, {3, 7}} } } },
            { 2, test::Edge { 2, 100, 105, Polyline2 { {{3, 7}, {6, 5}, {8, 6}} } } },
            { 225, test::Edge { 225, 105, 3, Polyline2 { PointsVector {{8, 6}, {5, 7}} } } },
            { 4, test::Edge { 4, 5, 110, Polyline2 { {{12, 4}, {10, 6}, {11, 6}} } } },
            { 230, test::Edge { 230, 110, 6, Polyline2 { PointsVector {{11, 6}, {13, 6}} } } }
        }
    },
    PrintInfo {
        PointsVector { {5, 4}, {3, 7}, {5, 7}, {8, 6}, {11, 6} },
        std::vector<PointGravity> {
            {{5, 4}, GravityType::JunctionGravity},
            {{3, 7}, GravityType::Tolerance},
            {{5, 7}, GravityType::JunctionGravity},
            {{8, 6}, GravityType::Tolerance}
        },
        std::map<EdgeID, GravityTypesSet> {
            {4, GravityTypesSet {GravityType::JunctionGravity}}
        },
        std::set<GravityType> {}
    }
};

// existing edges

TEST_DATA(Test_5_1)
{
    "Modify existing edge.",
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
    SourceEdgeID { 4, EXISTS },
    Polyline2 { {{12 + (1e-3 - 1e-4), 4}, {10, 7}, {13, 6}}},
    Polyline2 { {{12, 4}, {10, 7}, {13, 6}}},
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
    SplitEdges {},
    MockStorageDiff {
        {},
        {
            { 4, test::Edge { 4, 5, 6, Polyline2 { {{12, 4}, {10, 7}, {13, 6}} } } }
        }
    },
    PrintInfo {
        PointsVector { {12, 4}, {13, 6} },
        std::vector<PointGravity> {
            {{12, 4}, GravityType::JunctionGravity},
            {{13, 6}, GravityType::Tolerance}
        },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_5_3)
{
    "Modify existing edge, overlap with its previous geom.",
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
    SourceEdgeID { 4, EXISTS },
    Polyline2 { {{12 + (1e-3 - 1e-4), 4}, {10, 6}, {11, 6}}},
    Polyline2 { {{12, 4}, {10, 6}, {11, 6}}},
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
    SplitEdges {},
    MockStorageDiff {
        {
            { 6, test::Node { 6, {11, 6} } }
        },
        {
            { 4, test::Edge { 4, 5, 6, Polyline2 { {{12, 4}, {10, 6}, {11, 6}} } } }
        }
    },
    PrintInfo {
        PointsVector { {12, 4} },
        std::vector<PointGravity> {
            {{12, 4}, GravityType::JunctionGravity}
        },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_5_4)
{
    "Modify edge, check not to snap to its previous geom.",
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
    SourceEdgeID { 4, EXISTS },
    Polyline2 { {{12 - (1e-3 + 1e-4), 4 + (1e-3 + 1e-4) * 2.0 / 3.0}, {10, 6}, {13, 6}}},
    Polyline2 { {{12 - (1e-3 + 1e-4), 4}, {10, 6}, {13, 6}}},
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
        { SourceEdgeID { 3, EXISTS }, EdgeIDVector {3, 200} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {12 - (1e-3 + 1e-4), 4} } }
        },
        {
            { 4, test::Edge { 4, 100, 6, Polyline2 { {{12 - (1e-3 + 1e-4), 4}, {10, 6}, {13, 6}} } } },
            { 3, test::Edge { 3, 4, 100, Polyline2 { PointsVector {{8, 4}, {12 - (1e-3 + 1e-4), 4}} } } },
            { 200, test::Edge { 200, 100, 5, Polyline2 { PointsVector {{12 - (1e-3 + 1e-4), 4}, {12, 4}} } } }
        }
    },
    PrintInfo {
        PointsVector { {12 - (1e-3 + 1e-4), 4}, {13, 6} },
        std::vector<PointGravity> {
            {{12, 4}, GravityType::JunctionGravity},
            {{13, 6}, GravityType::Tolerance}
        },
        std::map<EdgeID, GravityTypesSet> {
            {3, GravityTypesSet {GravityType::JunctionGravity}}
        },
        std::set<GravityType> {}
    }
};

TEST_SUITE_END(intersection_complex)

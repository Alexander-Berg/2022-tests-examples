#include "../test_types/save_edge_test_data.h"

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
using maps::wiki::topo::test::Node;
using maps::wiki::topo::test::Edge;

TEST_SUITE_START(intersection_split_request, SaveEdgeTestData)

TEST_DATA(Test_1_1)
{
    "Simple case, no interactions.",
    MockStorage {
        test::NodeDataVector {},
        test::EdgeDataVector {}
    },
    SourceEdgeID { 205, NOT_EXISTS },
    Polyline2 { {{4, 6}, {6, 7}, {8, 7}, {10, 6}}}, // new polyline
    Polyline2 { {{4, 6}, {6, 7}, {8, 7}, {10, 6}}}, // aligned polyline
    PointsVector { {4, 6}, {6, 7}, {8, 7}, {10, 6} }, // splits requested
    TopologyRestrictions {
        1e-1, // tolerance
        1e-1, // junction gravity
        1e-1, // vertex gravity
        1e-1, // group junction gravity
        1e-1, // group junction snap to vertex
        Limits<double> { 1e-1 }, // segment
        Limits<double> { 1e-1 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {
        { {205, NOT_EXISTS}, EdgeIDVector {200, 205, 210} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {4, 6} } },
            { 105, test::Node { 105, {6, 7} } },
            { 110, test::Node { 110, {8, 7} } },
            { 115, test::Node { 115, {10, 6} } }
        },
        {
            { 200, test::Edge { 200, 100, 105, Polyline2 { PointsVector {{4, 6}, {6, 7}} } } },
            { 205, test::Edge { 205, 105, 110, Polyline2 { PointsVector {{6, 7}, {8, 7}} } } },
            { 210, test::Edge { 210, 110, 115, Polyline2 { PointsVector {{8, 7}, {10, 6}} } } }
        }
    },
    PrintInfo {
        PointsVector { {4, 6}, {6, 7}, {8, 7}, {10, 6} },
        std::vector<PointGravity> {
            {{4, 6}, GravityType::JunctionGravity},
            {{6, 7}, GravityType::JunctionGravity},
            {{8, 7}, GravityType::JunctionGravity},
            {{10, 6}, GravityType::JunctionGravity}
        },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_1_2)
{
    "Simple case, inaccurate split request.",
    MockStorage {
        test::NodeDataVector {},
        test::EdgeDataVector {}
    },
    SourceEdgeID { 205, NOT_EXISTS },
    Polyline2 { {{4, 6}, {6, 7}, {8, 7}, {10, 6}}}, // new polyline
    Polyline2 { {{4, 6}, {6, 7}, {8, 7}, {10, 6}}}, // aligned polyline
    PointsVector { {4 + 4e-2, 6}, {6, 7 + 4e-2}, {8 - 4e-2, 7}, {10, 6 - 4e-2} }, // splits requested
    TopologyRestrictions {
        1e-1, // tolerance
        1e-1, // junction gravity
        1e-1, // vertex gravity
        1e-1, // group junction gravity
        1e-1, // group junction snap to vertex
        Limits<double> { 1e-1 }, // segment
        Limits<double> { 1e-1 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {
        { {205, NOT_EXISTS}, EdgeIDVector {200, 205, 210} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {4, 6} } },
            { 105, test::Node { 105, {6, 7} } },
            { 110, test::Node { 110, {8, 7} } },
            { 115, test::Node { 115, {10, 6} } }
        },
        {
            { 200, test::Edge { 200, 100, 105, Polyline2 { PointsVector {{4, 6}, {6, 7}} } } },
            { 205, test::Edge { 205, 105, 110, Polyline2 { PointsVector {{6, 7}, {8, 7}} } } },
            { 210, test::Edge { 210, 110, 115, Polyline2 { PointsVector {{8, 7}, {10, 6}} } } }
        }
    },
    PrintInfo {
        PointsVector { {4, 6}, {6, 7}, {8, 7}, {10, 6} },
        std::vector<PointGravity> {
            {{4, 6}, GravityType::JunctionGravity},
            {{6, 7}, GravityType::JunctionGravity},
            {{8, 7}, GravityType::JunctionGravity},
            {{10, 6}, GravityType::JunctionGravity}
        },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_1_3)
{
    "Intersecting at existing node, with intersections.",
    MockStorage {
        {
            test::Node { 1, {2, 1} },
            test::Node { 2, {2, 5} },
            test::Node { 3, {5, 4} },
            test::Node { 4, {7, 2} },
            test::Node { 5, {11, 2} }
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{2, 1}, {2, 5}} } },
            test::Edge {2, 1, 3, Polyline2 { PointsVector {{2, 1}, {5, 4}} } },
            test::Edge {3, 3, 4, Polyline2 { PointsVector {{5, 4}, {7, 2}} } },
            test::Edge {4, 4, 5, Polyline2 { {{7, 2}, {9, 4}, {11, 2}} } }
        }
    },
    SourceEdgeID { 200, NOT_EXISTS },
    Polyline2 { {{4, 6}, {5, 4 - 4e-2}, {6, 6}}}, // new polyline
    Polyline2 { {{4, 6}, {5, 4}, {6, 6}}}, // aligned polyline
    PointsVector { {5, 4 - 4e-2} }, // splits requested
    TopologyRestrictions {
        1e-1, // tolerance
        1e-1, // junction gravity
        1e-1, // vertex gravity
        1e-1, // group junction gravity
        1e-1, // group junction snap to vertex
        Limits<double> { 1e-1 }, // segment
        Limits<double> { 1e-1 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {
        { {200, NOT_EXISTS}, EdgeIDVector {200, 205} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {4, 6} } },
            { 105, test::Node { 105, {6, 6} } }
        },
        {
            { 200, test::Edge { 200, 100, 3, Polyline2 { PointsVector {{4, 6}, {5, 4}} } } },
            { 205, test::Edge { 205, 3, 105, Polyline2 { PointsVector {{5, 4}, {6, 6}} } } }
        }
    },
    PrintInfo {
        PointsVector { {5, 4} },
        std::vector<PointGravity> { {{5, 4}, GravityType::JunctionGravity} },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_1_4)
{
    "Intersecting at existing node, without intersections.",
    MockStorage {
        {
            test::Node { 1, {2, 1} },
            test::Node { 2, {2, 5} },
            test::Node { 3, {5, 4} },
            test::Node { 4, {7, 2} },
            test::Node { 5, {11, 2} }
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{2, 1}, {2, 5}} } },
            test::Edge {2, 1, 3, Polyline2 { PointsVector {{2, 1}, {5, 4}} } },
            test::Edge {3, 3, 4, Polyline2 { PointsVector {{5, 4}, {7, 2}} } },
            test::Edge {4, 4, 5, Polyline2 { {{7, 2}, {9, 4}, {11, 2}} } }
        }
    },
    SourceEdgeID { 205, NOT_EXISTS },
    Polyline2 { {{4, 6}, {5, 4 + 4e-2}, {6, 6}}}, // new polyline
    Polyline2 { {{4, 6}, {5, 4}, {6, 6}}}, // aligned polyline
    PointsVector { {5, 4 + 4e-2} }, // splits requested
    TopologyRestrictions {
        1e-1, // tolerance
        1e-1, // junction gravity
        1e-1, // vertex gravity
        1e-1, // group junction gravity
        1e-1, // group junction snap to vertex
        Limits<double> { 1e-1 }, // segment
        Limits<double> { 1e-1 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {
        { {205, NOT_EXISTS}, EdgeIDVector {200, 205} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {4, 6} } },
            { 105, test::Node { 105, {6, 6} } }
        },
        {
            { 200, test::Edge { 200, 100, 3, Polyline2 { PointsVector {{4, 6}, {5, 4}} } } },
            { 205, test::Edge { 205, 3, 105, Polyline2 { PointsVector {{5, 4}, {6, 6}} } } }
        }
    },
    PrintInfo {
        PointsVector { {5, 4} },
        std::vector<PointGravity> { {{5, 4}, GravityType::JunctionGravity} },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_1_5)
{
    "Intersecting at existing edge vertex, with intersections.",
    MockStorage {
        {
            test::Node { 1, {2, 1} },
            test::Node { 2, {2, 5} },
            test::Node { 3, {5, 4} },
            test::Node { 4, {7, 2} },
            test::Node { 5, {11, 2} }
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{2, 1}, {2, 5}} } },
            test::Edge {2, 1, 3, Polyline2 { PointsVector {{2, 1}, {5, 4}} } },
            test::Edge {3, 3, 4, Polyline2 { PointsVector {{5, 4}, {7, 2}} } },
            test::Edge {4, 4, 5, Polyline2 { {{7, 2}, {9, 4}, {11, 2}} } }
        }
    },
    SourceEdgeID { 205, NOT_EXISTS },
    Polyline2 { {{8, 6}, {9, 4 - 4e-2}, {10, 6}}}, // new polyline
    Polyline2 { {{8, 6}, {9, 4}, {10, 6}}}, // aligned polyline
    PointsVector { {9, 4 - 4e-2} }, // splits requested
    TopologyRestrictions {
        1e-1, // tolerance
        1e-1, // junction gravity
        1e-1, // vertex gravity
        1e-1, // group junction gravity
        1e-1, // group junction snap to vertex
        Limits<double> { 1e-1 }, // segment
        Limits<double> { 1e-1 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {
        { {205, NOT_EXISTS}, EdgeIDVector {200, 205} },
        { {4, EXISTS}, EdgeIDVector {4, 210} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {8, 6} } },
            { 105, test::Node { 105, {9, 4} } },
            { 110, test::Node { 110, {10, 6} } }
        },
        {
            { 200, test::Edge { 200, 100, 105, Polyline2 { PointsVector {{8, 6}, {9, 4}} } } },
            { 205, test::Edge { 205, 105, 110, Polyline2 { PointsVector {{9, 4}, {10, 6}} } } },
            { 4, test::Edge { 4, 4, 105, Polyline2 { PointsVector {{7, 2}, {9, 4}} } } },
            { 210, test::Edge { 210, 105, 5, Polyline2 { PointsVector {{9, 4}, {11, 2}} } } }
        }
    },
    PrintInfo {
        PointsVector { {9, 4} },
        std::vector<PointGravity> { {{9, 4}, GravityType::JunctionGravity} },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_1_6)
{
    "Intersecting at existing node, two splits requested.",
    MockStorage {
        {
            test::Node { 1, {2, 1} },
            test::Node { 2, {2, 5} },
            test::Node { 3, {5, 4} },
            test::Node { 4, {7, 2} },
            test::Node { 5, {11, 2} }
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{2, 1}, {2, 5}} } },
            test::Edge {2, 1, 3, Polyline2 { PointsVector {{2, 1}, {5, 4}} } },
            test::Edge {3, 3, 4, Polyline2 { PointsVector {{5, 4}, {7, 2}} } },
            test::Edge {4, 4, 5, Polyline2 { PointsVector {{7, 2}, {9, 4}, {11, 2}} } }
        }
    },
    SourceEdgeID { 200, NOT_EXISTS },
    Polyline2 { PointsVector {{3, 7}, {5, 7}, {5, 4 - 4e-2}, {7, 6 - 4e-2}}}, // new polyline
    Polyline2 { PointsVector {{3, 7}, {5, 7}, {5, 4}, {7, 6 - 4e-2}}}, // aligned polyline
    PointsVector { {5, 4 - 4e-2}, {5, 7} }, // splits requested
    TopologyRestrictions {
        1e-1, // tolerance
        1e-1, // junction gravity
        1e-1, // vertex gravity
        1e-1, // group junction gravity
        1e-1, // group junction snap to vertex
        Limits<double> { 1e-1 }, // segment
        Limits<double> { 1e-1 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {
        { {200, NOT_EXISTS}, EdgeIDVector {200, 205, 210} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {3, 7} } },
            { 105, test::Node { 105, {5, 7} } },
            { 110, test::Node { 110, {7, 6 - 4e-2} } }
        },
        {
            { 200, test::Edge { 200, 100, 105, Polyline2 { PointsVector {{3, 7}, {5, 7}} } } },
            { 205, test::Edge { 205, 105, 3, Polyline2 { PointsVector {{5, 7}, {5, 4}} } } },
            { 210, test::Edge { 210, 3, 110, Polyline2 { PointsVector {{5, 4}, {7, 6 - 4e-2}} } } }
        }
    },
    PrintInfo {
        PointsVector { {5, 4}, {5, 7} },
        std::vector<PointGravity> {
            {{5, 4}, GravityType::JunctionGravity},
            {{5, 7}, GravityType::JunctionGravity}
        },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_1_7)
{
    "Intersecting at existing node, split at end.",
    MockStorage {
        {
            test::Node { 1, {2, 1} },
            test::Node { 2, {2, 5} },
            test::Node { 3, {5, 4} },
            test::Node { 4, {7, 2} },
            test::Node { 5, {11, 2} }
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{2, 1}, {2, 5}} } },
            test::Edge {2, 1, 3, Polyline2 { PointsVector {{2, 1}, {5, 4}} } },
            test::Edge {3, 3, 4, Polyline2 { PointsVector {{5, 4}, {7, 2}} } },
            test::Edge {4, 4, 5, Polyline2 { PointsVector {{7, 2}, {9, 4}, {11, 2}} } }
        }
    },
    SourceEdgeID { 200, NOT_EXISTS },
    Polyline2 { PointsVector {{5 - (1e-1 - 1e-2) / std::sqrt(2.0), 4 - (1e-1 - 1e-2) / std::sqrt(2.0)}, {7, 4}}}, // new polyline
    Polyline2 { PointsVector {{5, 4}, {7, 4}}}, // aligned polyline
    PointsVector { {5 - (1e-1 - 1e-2) / std::sqrt(2.0), 4 - (1e-1 - 1e-2) / std::sqrt(2.0)} }, // splits requested
    TopologyRestrictions {
        1e-1, // tolerance
        1e-1, // junction gravity
        1e-1, // vertex gravity
        1e-1, // group junction gravity
        1e-1, // group junction snap to vertex
        Limits<double> { 1e-1 }, // segment
        Limits<double> { 1e-1 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {},
    MockStorageDiff {
        {
            { 100, test::Node { 100, {7, 4} } }
        },
        {
            { 200, test::Edge { 200, 3, 100, Polyline2 { PointsVector {{5, 4}, {7, 4}} } } }
        }
    },
    PrintInfo {
        PointsVector { {5, 4} },
        std::vector<PointGravity> { {{5, 4}, GravityType::JunctionGravity} },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_1_8)
{
    "Split points at intersection points.",
    MockStorage {
        {
            test::Node { 1, {2, 1} },
            test::Node { 2, {2, 5} },
            test::Node { 3, {5, 4} },
            test::Node { 4, {7, 2} },
            test::Node { 5, {11, 2} }
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{2, 1}, {2, 5}} } },
            test::Edge {2, 1, 3, Polyline2 { PointsVector {{2, 1}, {5, 4}} } },
            test::Edge {3, 3, 4, Polyline2 { PointsVector {{5, 4}, {7, 2}} } },
            test::Edge {4, 4, 5, Polyline2 { PointsVector {{7, 2}, {9, 4}, {11, 2}} } }
        }
    },
    SourceEdgeID { 205, NOT_EXISTS },
    Polyline2 { PointsVector {{1, 3}, {5, 3}}}, // new polyline
    Polyline2 { PointsVector {{1, 3}, {2, 3}, {4, 3}, {5, 3}}}, // aligned polyline
    PointsVector {
        {2 + (1e-1 - 1e-2) / std::sqrt(2.0), 3 - (1e-1 - 1e-2) / std::sqrt(2.0)},
        {4 - (1e-1 - 1e-2) / std::sqrt(2.0), 3 + (1e-1 - 1e-2) / std::sqrt(2.0)}
    }, // splits requested
    TopologyRestrictions {
        1e-1, // tolerance
        1e-1, // junction gravity
        1e-1, // vertex gravity
        1e-1, // group junction gravity
        1e-1, // group junction snap to vertex
        Limits<double> { 1e-1 }, // segment
        Limits<double> { 1e-1 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {
        { {205, NOT_EXISTS}, EdgeIDVector {200, 205, 210} },
        { {1, EXISTS}, EdgeIDVector {300, 1} },
        { {2, EXISTS}, EdgeIDVector {400, 2} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {1, 3} } },
            { 105, test::Node { 105, {2, 3} } },
            { 110, test::Node { 110, {4, 3} } },
            { 115, test::Node { 115, {5, 3} } }
        },
        {
            { 200, test::Edge { 200, 100, 105, Polyline2 { PointsVector {{1, 3}, {2, 3}} } } },
            { 205, test::Edge { 205, 105, 110, Polyline2 { PointsVector {{2, 3}, {4, 3}} } } },
            { 210, test::Edge { 210, 110, 115, Polyline2 { PointsVector {{4, 3}, {5, 3}} } } },
            { 300, test::Edge { 300, 1, 105, Polyline2 { PointsVector {{2, 1}, {2, 3}} } } },
            { 1, test::Edge { 1, 105, 2, Polyline2 { PointsVector {{2, 3}, {2, 5}} } } },
            { 400, test::Edge { 400, 1, 110, Polyline2 { PointsVector {{2, 1}, {4, 3}} } } },
            { 2, test::Edge { 2, 110, 3, Polyline2 { PointsVector {{4, 3}, {5, 4}} } } }
        }
    },
    PrintInfo {
        PointsVector { {2, 3}, {4, 3} },
        std::vector<PointGravity> {
            {{2, 3}, GravityType::JunctionGravity},
            {{4, 3}, GravityType::JunctionGravity}
        },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_2_1)
{
    "Continue through start, split at old start.",
    MockStorage {
        {
            test::Node { 1, {2, 1} },
            test::Node { 2, {2, 5} }
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{2, 1}, {2, 5}} } }
        }
    },
    SourceEdgeID { 1, EXISTS },
    Polyline2 { PointsVector {{2, 0}, {2, 1}, {2, 5}}}, // new polyline
    Polyline2 { PointsVector {{2, 0}, {2, 1}, {2, 5}}}, // aligned polyline
    PointsVector { {2, 1} }, // splits requested
    TopologyRestrictions {
        1e-1, // tolerance
        1e-1, // junction gravity
        1e-1, // vertex gravity
        1e-1, // group junction gravity
        1e-1, // group junction snap to vertex
        Limits<double> { 1e-1 }, // segment
        Limits<double> { 1e-1 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {
        { {1, EXISTS}, EdgeIDVector {1, 200} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {2, 0} } }
        },
        {
            { 1, test::Edge { 1, 100, 1, Polyline2 { PointsVector {{2, 0}, {2, 1}} } } },
            { 200, test::Edge { 200, 1, 2, Polyline2 { PointsVector {{2, 1}, {2, 5}} } } }
        }
    },
    PrintInfo {
        PointsVector {},
        std::vector<PointGravity> {},
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_2_2)
{
    "Continue through end, split at old end.",
    MockStorage {
        {
            test::Node { 1, {2, 1} },
            test::Node { 2, {2, 5} }
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{2, 1}, {2, 5}} } }
        }
    },
    SourceEdgeID { 1, EXISTS },
    Polyline2 { PointsVector {{2, 1}, {2, 5}, {2, 7}}}, // new polyline
    Polyline2 { PointsVector {{2, 1}, {2, 5}, {2, 7}}}, // aligned polyline
    PointsVector { {2, 5} }, // splits requested
    TopologyRestrictions {
        1e-1, // tolerance
        1e-1, // junction gravity
        1e-1, // vertex gravity
        1e-1, // group junction gravity
        1e-1, // group junction snap to vertex
        Limits<double> { 1e-1 }, // segment
        Limits<double> { 1e-1 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {
        { {1, EXISTS}, EdgeIDVector {1, 200} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {2, 7} } }
        },
        {
            { 1, test::Edge { 1, 1, 2, Polyline2 { PointsVector {{2, 1}, {2, 5}} } } },
            { 200, test::Edge { 200, 2, 100, Polyline2 { PointsVector {{2, 5}, {2, 7}} } } }
        }
    },
    PrintInfo {
        PointsVector {},
        std::vector<PointGravity> {},
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_2_3)
{
    "Continue through start and end, split at old start and end.",
    MockStorage {
        {
            test::Node { 1, {2, 1} },
            test::Node { 2, {2, 5} }
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{2, 1}, {2, 5}} } }
        }
    },
    SourceEdgeID { 1, EXISTS },
    Polyline2 { PointsVector {{2, 0}, {2, 1}, {2, 5}, {2, 7}}}, // new polyline
    Polyline2 { PointsVector {{2, 0}, {2, 1}, {2, 5}, {2, 7}}}, // aligned polyline
    PointsVector { {2, 1}, {2, 5} }, // splits requested
    TopologyRestrictions {
        1e-1, // tolerance
        1e-1, // junction gravity
        1e-1, // vertex gravity
        1e-1, // group junction gravity
        1e-1, // group junction snap to vertex
        Limits<double> { 1e-1 }, // segment
        Limits<double> { 1e-1 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {
        { {1, EXISTS}, EdgeIDVector {200, 1, 205} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {2, 0} } },
            { 105, test::Node { 105, {2, 7} } }
        },
        {
            { 200, test::Edge { 200, 100, 1, Polyline2 { PointsVector {{2, 0}, {2, 1}} } } },
            { 1, test::Edge { 1, 1, 2, Polyline2 { PointsVector {{2, 1}, {2, 5}} } } },
            { 205, test::Edge { 205, 2, 105, Polyline2 { PointsVector {{2, 5}, {2, 7}} } } }
        }
    },
    PrintInfo {
        PointsVector {},
        std::vector<PointGravity> {},
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_2_4)
{
    "Continue through start and end, split at old start.",
    MockStorage {
        {
            test::Node { 1, {2, 1} },
            test::Node { 2, {2, 5} }
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{2, 1}, {2, 5}} } }
        }
    },
    SourceEdgeID { 1, EXISTS },
    Polyline2 { PointsVector {{2, 0}, {2, 1}, {2, 5}, {2, 7}}}, // new polyline
    Polyline2 { PointsVector {{2, 0}, {2, 1}, {2, 5}, {2, 7}}}, // aligned polyline
    PointsVector { {2, 1} }, // splits requested
    TopologyRestrictions {
        1e-1, // tolerance
        1e-1, // junction gravity
        1e-1, // vertex gravity
        1e-1, // group junction gravity
        1e-1, // group junction snap to vertex
        Limits<double> { 1e-1 }, // segment
        Limits<double> { 1e-1 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {
        { {1, EXISTS}, EdgeIDVector {200, 1} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {2, 0} } },
            { 2, test::Node { 2, {2, 7} } }
        },
        {
            { 200, test::Edge { 200, 100, 1, Polyline2 { PointsVector {{2, 0}, {2, 1}} } } },
            { 1, test::Edge { 1, 1, 2, Polyline2 { PointsVector {{2, 1}, {2, 5}, {2, 7}} } } }
        }
    },
    PrintInfo {
        PointsVector {},
        std::vector<PointGravity> {},
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_2_5)
{
    "Continue through start and end, split at old end.",
    MockStorage {
        {
            test::Node { 1, {2, 1} },
            test::Node { 2, {2, 5} }
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{2, 1}, {2, 5}} } }
        }
    },
    SourceEdgeID { 1, EXISTS },
    Polyline2 { PointsVector {{2, 0}, {2, 1}, {2, 5}, {2, 7}}}, // new polyline
    Polyline2 { PointsVector {{2, 0}, {2, 1}, {2, 5}, {2, 7}}}, // aligned polyline
    PointsVector { {2, 5} }, // splits requested
    TopologyRestrictions {
        1e-1, // tolerance
        1e-1, // junction gravity
        1e-1, // vertex gravity
        1e-1, // group junction gravity
        1e-1, // group junction snap to vertex
        Limits<double> { 1e-1 }, // segment
        Limits<double> { 1e-1 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {
        { {1, EXISTS}, EdgeIDVector {200, 1} }
    },
    MockStorageDiff {
        {
            { 1, test::Node { 1, {2, 0} } },
            { 100, test::Node { 100, {2, 7} } }
        },
        {
            { 200, test::Edge { 200, 1, 2, Polyline2 { PointsVector {{2, 0}, {2, 1}, {2, 5}} } } },
            { 1, test::Edge { 1, 2, 100, Polyline2 { PointsVector {{2, 5}, {2, 7}} } } }
        }
    },
    PrintInfo {
        PointsVector {},
        std::vector<PointGravity> {},
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_2_6)
{
    "Continue through start and end, split at old end.",
    MockStorage {
        {
            test::Node { 1, {2, 1} },
            test::Node { 2, {2, 5} }
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{2, 1}, {2, 5}} } }
        }
    },
    SourceEdgeID { 1, EXISTS },
    Polyline2 { PointsVector {{2, 0}, {2, 1}, {2, 5}, {2, 7}}}, // new polyline
    Polyline2 { PointsVector {{2, 0}, {2, 1}, {2, 5}, {2, 7}}}, // aligned polyline
    PointsVector { {2, 5} }, // splits requested
    TopologyRestrictions {
        1e-1, // tolerance
        1e-1, // junction gravity
        1e-1, // vertex gravity
        1e-1, // group junction gravity
        1e-1, // group junction snap to vertex
        Limits<double> { 1e-1 }, // segment
        Limits<double> { 1e-1 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {
        { {1, EXISTS}, EdgeIDVector {200, 1} }
    },
    MockStorageDiff {
        {
            { 1, test::Node { 1, {2, 0} } },
            { 100, test::Node { 100, {2, 7} } }
        },
        {
            { 200, test::Edge { 200, 1, 2, Polyline2 { PointsVector {{2, 0}, {2, 1}, {2, 5}} } } },
            { 1, test::Edge { 1, 2, 100, Polyline2 { PointsVector {{2, 5}, {2, 7}} } } }
        }
    },
    PrintInfo {
        PointsVector {},
        std::vector<PointGravity> {},
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_2_7)
{
    "Change direction, split at old start.",
    MockStorage {
        {
            test::Node { 1, {2, 1} },
            test::Node { 2, {2, 5} }
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{2, 1}, {2, 5}} } }
        }
    },
    SourceEdgeID { 1, EXISTS },
    Polyline2 { PointsVector {{2, 7}, {2, 5}, {2, 1}, {2, 0}}}, // new polyline
    Polyline2 { PointsVector {{2, 7}, {2, 5}, {2, 1}, {2, 0}}}, // aligned polyline
    PointsVector { {2, 1} }, // splits requested
    TopologyRestrictions {
        1e-1, // tolerance
        1e-1, // junction gravity
        1e-1, // vertex gravity
        1e-1, // group junction gravity
        1e-1, // group junction snap to vertex
        Limits<double> { 1e-1 }, // segment
        Limits<double> { 1e-1 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {
        { {1, EXISTS}, EdgeIDVector {200, 1} }
    },
    MockStorageDiff {
        {
            { 2, test::Node { 2, {2, 0} } },
            { 100, test::Node { 100, {2, 7} } }
        },
        {
            { 200, test::Edge { 200, 100, 1, Polyline2 { PointsVector {{2, 7}, {2, 5}, {2, 1}} } } },
            { 1, test::Edge { 1, 1, 2, Polyline2 { PointsVector {{2, 1}, {2, 0}} } } }
        }
    },
    PrintInfo {
        PointsVector {},
        std::vector<PointGravity> {},
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_3_1)
{
    "New edge, split at node within gravity of start (on polyline).",
    MockStorage {
        {
            test::Node { 1, {2, 0} },
            test::Node { 2, {3, 2} }
        },
        {}
    },
    SourceEdgeID { 100, NOT_EXISTS },
    Polyline2 { PointsVector {{2, 0}, {2, 5e-2}, {2, 5}}}, // new polyline
    Polyline2 { PointsVector {{2, 0}, {2, 5e-2}, {2, 5}}}, // aligned polyline
    PointsVector { {2, 5e-2 + 1e-3} }, // splits requested
    TopologyRestrictions {
        1e-2, // tolerance
        1e-1, // junction gravity
        1e-2, // vertex gravity
        1e-1, // group junction gravity
        1e-2, // group junction snap to vertex
        Limits<double> { 1e-2 }, // segment
        Limits<double> { 1e-2 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {
        { {100, NOT_EXISTS}, EdgeIDVector {200} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {2, 5} } }
        },
        {
            { 200, test::Edge { 200, 1, 100, Polyline2 { PointsVector {{2, 0}, {2, 5e-2}, {2, 5}} } } },
        }
    },
    PrintInfo {
        PointsVector {{2, 0}},
        std::vector<PointGravity> {
            {{2, 0}, GravityType::JunctionGravity}
        },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_3_2)
{
    "New edge, split gravity tolerance of existing node.",
    MockStorage {
        {
            test::Node { 1, {2, 0} },
            test::Node { 2, {3, 2} }
        },
        {}
    },
    SourceEdgeID { 200, NOT_EXISTS },
    Polyline2 { PointsVector {{2, 0}, {3 - 4e-2, 2}, {2, 5}}}, // new polyline
    Polyline2 { PointsVector {{2, 0}, {3, 2}, {2, 5}}}, // aligned polyline
    PointsVector { /*{3 - 4e-2, 2}*/ }, // splits requested
    TopologyRestrictions {
        3e-2, // tolerance
        1e-1, // junction gravity
        5e-2, // vertex gravity
        1e-1, // group junction gravity
        3e-2, // group junction snap to vertex
        Limits<double> { 5e-2 }, // segment
        Limits<double> { 5e-2 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {
        { {200, NOT_EXISTS}, EdgeIDVector {200, 205} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {2, 5} } }
        },
        {
            { 200, test::Edge { 200, 1, 2, Polyline2 { PointsVector {{2, 0}, {3, 2}} } } },
            { 205, test::Edge { 205, 2, 100, Polyline2 { PointsVector {{3, 2}, {2, 5}} } } }
        }
    },
    PrintInfo {
        PointsVector {{3, 2}},
        std::vector<PointGravity> {
            {{3, 2}, GravityType::JunctionGravity},
            {{3, 2}, GravityType::VertexGravity},
            {{3, 2}, GravityType::Tolerance}
        },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_3_3)
{
    "New edge, split out of tolerance of existing node but within gravity.",
    MockStorage {
        {
            test::Node { 1, {2, 0} },
            test::Node { 2, {3.07, 3} }
        },
        {}
    },
    SourceEdgeID { 200, NOT_EXISTS },
    Polyline2 { PointsVector {{2, 0}, {3, 3}, {2, 5}}}, // new polyline
    Polyline2 { PointsVector {{2, 0}, {3, 3}, {2, 5}}}, // aligned polyline
    PointsVector { {3, 3} }, // splits requested
    TopologyRestrictions {
        5e-2, // tolerance
        1e-1, // junction gravity
        5e-2, // vertex gravity
        1e-1, // group junction gravity
        5e-2, // group junction snap to vertex
        Limits<double> { 5e-2 }, // segment
        Limits<double> { 5e-2 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {
        { {200, NOT_EXISTS}, EdgeIDVector {200, 205} }
    },
    MockStorageDiff {
        {
            { 104, test::Node { 104, {3, 3} } },
            { 105, test::Node { 105, {2, 5} } }
        },
        {
            { 200, test::Edge { 200, 1, 104, Polyline2 { PointsVector {{2, 0}, {3, 3}} } } },
            { 205, test::Edge { 205, 104, 105, Polyline2 { PointsVector {{3, 3}, {2, 5}} } } }
        }
    },
    PrintInfo {
        PointsVector {{3, 3}},
        std::vector<PointGravity> {
            {{3, 3}, GravityType::JunctionGravity},
            {{3, 3}, GravityType::Tolerance}
        },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_4_1)
{
    "Split at internal node snapped to existing node.",
    MockStorage {
        {
            test::Node { 1, {2, 0} },
            test::Node { 2, {3, 2} }
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{2, 0}, {3, 2}} } }
        }
    },
    SourceEdgeID { 200, NOT_EXISTS },
    Polyline2 { PointsVector {{4, 0}, {3, 2.05}, {4, 3}}}, // new polyline
    Polyline2 { PointsVector {{4, 0}, {3, 2}, {4, 3}}}, // aligned polyline
    PointsVector { {3 - 5e-2, 2} }, // splits requested
    TopologyRestrictions {
        3e-2, // tolerance
        1e-1, // junction gravity
        5e-2, // vertex gravity
        1e-1, // group junction gravity
        1e-1, // group junction snap to vertex
        Limits<double> { 1e-1 }, // segment
        Limits<double> { 1e-1 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {
        { {200, NOT_EXISTS}, EdgeIDVector {200, 205} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {4, 0} } },
            { 105, test::Node { 105, {4, 3} } }
        },
        {
            { 200, test::Edge { 200, 100, 2, Polyline2 { PointsVector {{4, 0}, {3, 2}} } } },
            { 205, test::Edge { 205, 2, 105, Polyline2 { PointsVector {{3, 2}, {4, 3}} } } }
        }
    },
    PrintInfo {
        PointsVector { {3, 2} },
        std::vector<PointGravity> {
            {{3, 2}, GravityType::JunctionGravity},
            {{3, 2}, GravityType::VertexGravity},
            {{3, 2}, GravityType::Tolerance}
        },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_4_2)
{
    "Split at internal node snapped to existing node.",
    MockStorage {
        {
            test::Node { 1, {2, 0} },
            test::Node { 2, {3, 2} }
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{2, 0}, {3, 2}} } }
        }
    },
    SourceEdgeID { 200, NOT_EXISTS },
    Polyline2 { PointsVector {{4, 0}, {3, 1.96}, {4, 3}}}, // new polyline
    Polyline2 { PointsVector {{4, 0}, {3, 2}, {4, 3}}}, // aligned polyline
    PointsVector { {2.92, 1.92} }, // splits requested
    TopologyRestrictions {
        1e-1, // tolerance
        1e-1, // junction gravity
        1e-1, // vertex gravity
        1e-1, // group junction gravity
        1e-1, // group junction snap to vertex
        Limits<double> { 1e-1 }, // segment
        Limits<double> { 1e-1 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {
        { {200, NOT_EXISTS}, EdgeIDVector {200, 205} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {4, 0} } },
            { 105, test::Node { 105, {4, 3} } }
        },
        {
            { 200, test::Edge { 200, 100, 2, Polyline2 { PointsVector {{4, 0}, {3, 2}} } } },
            { 205, test::Edge { 205, 2, 105, Polyline2 { PointsVector {{3, 2}, {4, 3}} } } }
        }
    },
    PrintInfo {
        PointsVector {{3, 2}},
        std::vector<PointGravity> {
            {{3, 2}, GravityType::JunctionGravity},
            {{3, 1.96}, GravityType::JunctionGravity}
        },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    },
    ErrorCode::Unsupported
};

TEST_DATA(Test_5_1)
{
    "Split at vertex with overlapping.",
    MockStorage {
        {
            test::Node { 1, {0, 0} },
            test::Node { 2, {3, 0} }
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{0, 0}, {3, 0}} } }
        }
    },
    SourceEdgeID { 2, NOT_EXISTS },
    Polyline2 { PointsVector {{0, 1}, {1, 0}, {3, 0}, {3, 2}}}, // new polyline
    Polyline2 { PointsVector {{0, 1}, {1, 0}, {3, 0}, {3, 2}}}, // aligned polyline
    PointsVector { {1, 0} }, // splits requested
    TopologyRestrictions {
        1e-1, // tolerance
        1e-1, // junction gravity
        1e-1, // vertex gravity
        1e-1, // group junction gravity
        1e-1, // group junction snap to vertex
        Limits<double> { 1e-1 }, // segment
        Limits<double> { 1e-1 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Allowed
    },
    SplitEdges {
        { {1, EXISTS}, EdgeIDVector {1, 3} },
        { {2, NOT_EXISTS}, EdgeIDVector {2, 3, 4} }
    },
    MockStorageDiff {
        {
            { 3, test::Node { 3, {0, 1} } },
            { 4, test::Node { 4, {1, 0} } },
            { 5, test::Node { 5, {3, 2} } }
        },
        {
            { 1, test::Edge { 1, 1, 4, Polyline2 { PointsVector {{0, 0}, {1, 0}} } } },
            { 2, test::Edge { 2, 3, 4, Polyline2 { PointsVector {{0, 1}, {1, 0}} } } },
            { 3, test::Edge { 3, 4, 2, Polyline2 { PointsVector {{1, 0}, {3, 0}} } } },
            { 4, test::Edge { 4, 2, 5, Polyline2 { PointsVector {{3, 0}, {3, 2}} } } }
        }
    }
};

TEST_DATA(Test_5_2)
{
    "Split at segment with overlapping.",
    MockStorage {
        {
            test::Node { 1, {0, 0} },
            test::Node { 2, {3, 0} }
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{0, 0}, {3, 0}} } }
        }
    },
    SourceEdgeID { 2, NOT_EXISTS },
    Polyline2 { PointsVector {{0, 1}, {1, 0}, {3, 0}, {3, 2}}}, // new polyline
    Polyline2 { PointsVector {{0, 1}, {1, 0}, {2, 0}, {3, 0}, {3, 2}}}, // aligned polyline
    PointsVector { {1, 0}, {2, 0}, {3, 0} }, // splits requested
    TopologyRestrictions {
        1e-1, // tolerance
        1e-1, // junction gravity
        1e-1, // vertex gravity
        1e-1, // group junction gravity
        1e-1, // group junction snap to vertex
        Limits<double> { 1e-1 }, // segment
        Limits<double> { 1e-1 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Allowed
    },
    SplitEdges {
        { {1, EXISTS}, EdgeIDVector {1, 3, 4} },
        { {2, NOT_EXISTS}, EdgeIDVector {2, 3, 4, 5} }
    },
    MockStorageDiff {
        {
            { 3, test::Node { 3, {0, 1} } },
            { 4, test::Node { 4, {1, 0} } },
            { 5, test::Node { 5, {2, 0} } },
            { 6, test::Node { 6, {3, 2} } }
        },
        {
            { 1, test::Edge { 1, 1, 4, Polyline2 { PointsVector {{0, 0}, {1, 0}} } } },
            { 2, test::Edge { 2, 3, 4, Polyline2 { PointsVector {{0, 1}, {1, 0}} } } },
            { 3, test::Edge { 3, 4, 5, Polyline2 { PointsVector {{1, 0}, {2, 0}} } } },
            { 4, test::Edge { 4, 5, 2, Polyline2 { PointsVector {{2, 0}, {3, 0}} } } },
            { 5, test::Edge { 5, 2, 6, Polyline2 { PointsVector {{3, 0}, {3, 2}} } } }
        }
    }
};

TEST_DATA(Test_5_3)
{
    "Split at segment.",
    MockStorage {
        {
            test::Node { 1, {0, 0} },
            test::Node { 2, {3, 1} }
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{0, 0}, {2, 0}, {3, 1}} } }
        }
    },
    SourceEdgeID { 1, EXISTS },
    Polyline2 { PointsVector {{0, 0}, {2, 0}, {3, 1}}}, // new polyline
    Polyline2 { PointsVector {{0, 0}, {1, 0}, {2, 0}, {3, 1}}}, // aligned polyline
    PointsVector { {1, 0}, {2, 0}, {3, 1} }, // splits requested
    TopologyRestrictions {
        1e-1, // tolerance
        1e-1, // junction gravity
        1e-1, // vertex gravity
        1e-1, // group junction gravity
        1e-1, // group junction snap to vertex
        Limits<double> { 1e-1 }, // segment
        Limits<double> { 1e-1 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Allowed
    },
    SplitEdges {
        { {1, EXISTS}, EdgeIDVector {1, 2, 3} }
    },
    MockStorageDiff {
        {
            { 3, test::Node { 3, {1, 0} } },
            { 4, test::Node { 4, {2, 0} } }
        },
        {
            { 1, test::Edge { 1, 1, 3, Polyline2 { PointsVector {{0, 0}, {1, 0}} } } },
            { 2, test::Edge { 2, 3, 4, Polyline2 { PointsVector {{1, 0}, {2, 0}} } } },
            { 3, test::Edge { 3, 4, 2, Polyline2 { PointsVector {{2, 0}, {3, 1}} } } }
        }
    }
};


TEST_SUITE_END(intersection_split_request)

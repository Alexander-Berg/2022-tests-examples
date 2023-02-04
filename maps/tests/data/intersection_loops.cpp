#include "../test_types/mock_storage.h"
#include "../test_types/save_edge_test_data.h"
#include "../suite.h"

#include <yandex/maps/wiki/topo/common.h>

#include <maps/libs/geolib/include/point.h>
#include <maps/libs/geolib/include/segment.h>
#include <maps/libs/geolib/include/polyline.h>

#include <vector>
#include <map>
#include <initializer_list>

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

TEST_SUITE_START(intersection_loops, SaveEdgeTestData)

TEST_DATA(Test_1_1_2)
{
    "Closed polylines allowed.",
    MockStorage {
        NodeDataVector {},
        EdgeDataVector {}
    },
    SourceEdgeID { 1, NOT_EXISTS },
    Polyline2 { {{10, 10}, {10, 20}, {20, 10}, {10, 10}}}, // new polyline
    Polyline2 { {{10, 10}, {10, 20}, {20, 10}, {10, 10}}}, // aligned polyline
    PointsVector {}, // splits requested
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
            { 100, test::Node { 100, {10, 10} } }
        },
        {
            { 1, test::Edge {1, 100, 100, Polyline2 { {{10, 10}, {10, 20}, {20, 10}, {10, 10}} } } }
        }
    },
    PrintInfo {
        PointsVector { {10, 10} },
        std::vector<PointGravity> {},
        std::map<EdgeID, test::GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_1_1_3)
{
    "Ends snap to one node.",
    MockStorage {
        {
            test::Node { 1, {2, 1} },
            test::Node { 2, {2, 1 + 1e-1} },
            test::Node { 3, {5, 4} },
            test::Node { 4, {2, 3} }
        },
        {
            test::Edge {1, 1, 3, Polyline2 { PointsVector {{2, 1}, {5, 4}} } },
            test::Edge {2, 2, 4, Polyline2 { PointsVector {{2, 1 + 1e-1}, {2, 3}} } },
            test::Edge {3, 4, 3, Polyline2 { PointsVector {{2, 3}, {5, 4}} } }
        }
    },
    SourceEdgeID { 2, EXISTS },
    Polyline2 { {{2, 1}, {2, 0}, {4, 0}, {2, 1 + 7e-2}}}, // new polyline
    Polyline2 { {{2, 1}, {2, 0}, {4, 0}, {2, 1}}}, // aligned polyline
    PointsVector {}, // splits requested
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
            {2, boost::none}
        },
        {
            { 2, test::Edge {2, 1, 1, Polyline2 { {{2, 1}, {2, 0}, {4, 0}, {2, 1}} } } }
        }
    },
    PrintInfo {
        PointsVector { {2, 1} },
        std::vector<PointGravity> {
            {Point2{2, 1}, GravityType::JunctionGravity},
            {Point2{2, 1.1}, GravityType::JunctionGravity}
        },
        std::map<EdgeID, test::GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_1_2_1)
{
    "Ends snap to one edge.",
    MockStorage {
        {
            test::Node { 1, {2, 1} },
            test::Node { 2, {2, 1 + 1e-1} },
            test::Node { 3, {5, 4} },
            test::Node { 4, {2, 3} }
        },
        {
            test::Edge {1, 1, 3, Polyline2 { PointsVector {{2, 1}, {5, 4}} } },
            test::Edge {2, 2, 4, Polyline2 { PointsVector {{2, 1 + 1e-1}, {2, 3}} } },
            test::Edge {3, 4, 3, Polyline2 { PointsVector {{2, 3}, {5, 4}} } }
        }
    },
    SourceEdgeID { 200, NOT_EXISTS },
    Polyline2 { {{2 - 1e-3, 2}, {0, 0}, {0, 4}, {2 - 1e-3, 2}}}, // new polyline
    Polyline2 { {{2, 2}, {0, 0}, {0, 4}, {2, 2}}}, // aligned polyline
    PointsVector {}, // splits requested
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
        { SourceEdgeID { 2, EXISTS }, EdgeIDVector {201, 2} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {2, 2} } }
        },
        {
            { 2, test::Edge {2, 100, 4, Polyline2 { PointsVector {{2, 2}, {2, 3}} } } },
            { 200, test::Edge {200, 100, 100, Polyline2 { {{2, 2}, {0, 0}, {0, 4}, {2, 2}} } } },
            { 201, test::Edge {201, 2, 100, Polyline2 { PointsVector {{2, 1 + 1e-1}, {2, 2}} } } }
        }
    },
    PrintInfo {
        PointsVector { {2, 2} },
        std::vector<PointGravity> {},
        std::map<EdgeID, test::GravityTypesSet> { {2, test::GravityTypesSet{GravityType::JunctionGravity}} },
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_1_2_2)
{
    "Ends snap to one edge with edges' overlapping.",
    MockStorage {
        {
            test::Node { 1, {2, 1} },
            test::Node { 2, {5, 4} },
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{2, 1}, {5, 4}} } },
            test::Edge {2, 1, 2, Polyline2 { PointsVector {{2, 1}, {2, 3}, {5, 4}} } },
        }
    },
    SourceEdgeID { 201, NOT_EXISTS },
    Polyline2 { {{2 - 1e-2, 2}, {2, 0}, {0, 2}, {2, 4}, {2 + 1e-2, 2 + (1e-1 - 1e-2)}}}, // new polyline
    Polyline2 { {{2, 2}, {2, 0}, {0, 2}, {2, 4}, {2, 2}}}, // aligned polyline
    PointsVector {}, // splits requested
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
        { SourceEdgeID { 2, EXISTS }, EdgeIDVector {200, 202, 2} },
        { SourceEdgeID { 201, NOT_EXISTS }, EdgeIDVector {200, 201, 202} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {2, 2} } },
            { 101, test::Node { 101, {2, 3} } }
        },
        {
            { 2, test::Edge {2, 101, 2, Polyline2 { PointsVector {{2, 3}, {5, 4}} } } },
            { 200, test::Edge {200, 1, 100, Polyline2 { PointsVector {{2, 1}, {2, 2}} } } },
            { 201, test::Edge {201, 1, 101, Polyline2 { PointsVector {{2, 1}, {2, 0}, {0, 2}, {2, 4}, {2, 3}} } } },
            { 202, test::Edge {202, 100, 101, Polyline2 { PointsVector {{2, 2}, {2, 3}} } } }
        }
    },
    PrintInfo {
        PointsVector { {2, 2} },
        std::vector<PointGravity> { {Point2{2 - 1e-2, 2}, GravityType::JunctionGravity} },
        std::map<EdgeID, test::GravityTypesSet> { {2, test::GravityTypesSet{GravityType::JunctionGravity}} },
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_1_2_3)
{
    "Ends snap to one edge with edges' overlapping.",
    MockStorage {
        {
            test::Node { 1, {1, 1} }
        },
        {
            test::Edge {1, 1, 1, Polyline2 { PointsVector {{1, 1}, {1, 4}, {3, 4}, {3, 1}, {1, 1}} } },
        }
    },
    SourceEdgeID { 2, NOT_EXISTS },
    Polyline2 { PointsVector {{1, 3}, {1, 2}, {0, 2}, {0, 3}, {1, 3}}}, // new polyline
    Polyline2 { PointsVector {{1, 3}, {1, 2}, {0, 2}, {0, 3}, {1, 3}}}, // aligned polyline
    PointsVector {}, // splits requested
    TopologyRestrictions {
        1e-3, // tolerance
        1e-3, // junction gravity
        1e-3, // vertex gravity
        1e-3, // group junction gravity
        1e-3, // group junction snap to vertex
        Limits<double> { 1e-1 }, // segment
        Limits<double> { 1e-1 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Allowed
    },
    SplitEdges {
        { SourceEdgeID { 1, EXISTS }, EdgeIDVector {1, 3, 4} },
        { SourceEdgeID { 2, NOT_EXISTS }, EdgeIDVector {3, 2} }
    },
    MockStorageDiff {
        {
            { 2, test::Node { 2, {1, 2} } },
            { 3, test::Node { 3, {1, 3} } }
        },
        {
            { 1, test::Edge {1, 1, 2, Polyline2 { PointsVector {{1, 1}, {1, 2}} } } },
            { 2, test::Edge {2, 2, 3, Polyline2 { PointsVector {{1, 2}, {0, 2}, {0, 3}, {1, 3}} } } },
            { 3, test::Edge {3, 2, 3, Polyline2 { PointsVector {{1, 2}, {1, 3} } } } },
            { 4, test::Edge {4, 3, 1, Polyline2 { PointsVector {{1, 3}, {1, 4}, {3, 4}, {3, 1}, {1, 1}} } } }
        }
    },
    PrintInfo {
        PointsVector {},
        std::vector<PointGravity> {},
        std::map<EdgeID, test::GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_1_2_4)
{
    "Ends snap to one point with overlapping.",
    MockStorage {
        {
            test::Node { 1, {2, 1} },
            test::Node { 2, {5, 4} }

        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{2, 1}, {5, 4}} } },
            test::Edge {2, 1, 2, Polyline2 { PointsVector {{2, 1}, {2, 3}, {5, 4}} } }
        }
    },
    SourceEdgeID { 200, NOT_EXISTS },
    Polyline2 { {{2 + 5e-2, 3 - 5e-2}, {3, 3}, {3, 2}, {2 - 3e-2, 2}, {2 - 3e-2, 3}}},
    Polyline2 { {{2, 3}, {3, 3}, {3, 2}, {2, 2}, {2, 3}}},
    PointsVector {}, // splits requested
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
        { SourceEdgeID {1, EXISTS}, EdgeIDVector{201, 1} },
        { SourceEdgeID {2, EXISTS}, EdgeIDVector{204, 203, 2} },
        { SourceEdgeID {200, NOT_EXISTS}, EdgeIDVector{200, 202, 203} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {3, 2} } },
            { 101, test::Node { 101, {2, 2} } },
            { 102, test::Node { 102, {2, 3} } }
        },
        {
            { 1, test::Edge {1, 100, 2, Polyline2 { PointsVector {{3, 2}, {5, 4}} } } },
            { 2, test::Edge {2, 102, 2, Polyline2 { PointsVector {{2, 3}, {5, 4}} } } },
            { 200, test::Edge {200, 102, 100, Polyline2 { PointsVector {{2, 3}, {3, 3}, {3, 2}} } } },
            { 201, test::Edge {201, 1, 100, Polyline2 { PointsVector {{2, 1}, {3, 2}} } } },
            { 202, test::Edge {202, 100, 101, Polyline2 { PointsVector {{3, 2}, {2, 2}} } } },
            { 203, test::Edge {203, 101, 102, Polyline2 { PointsVector {{2, 2}, {2, 3}} } } },
            { 204, test::Edge {204, 1, 101, Polyline2 { PointsVector {{2, 1}, {2, 2}} } } }

        }
    },
    PrintInfo {
        PointsVector { {2, 3} },
        std::vector<PointGravity> {
            {Point2{2, 3}, GravityType::JunctionGravity}
        },
        std::map<EdgeID, test::GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_3_1)
{
    "Splits requested at merging endpoints.",
    MockStorage {
        {
            test::Node { 1, {2, 1} },
            test::Node { 2, {2, 1 + 1e-1} },
            test::Node { 3, {5, 4} },
            test::Node { 4, {2, 3} }
        },
        {
            test::Edge {1, 1, 3, Polyline2 { PointsVector {{2, 1}, {5, 4}} } },
            test::Edge {2, 2, 4, Polyline2 { PointsVector {{2, 1 + 1e-1}, {2, 3}} } }
        }
    },
    SourceEdgeID { 200, NOT_EXISTS },
    Polyline2 { {{2, 1}, {2, 0}, {4, 0}, {2, 1 + 7e-3}}},
    Polyline2 { {{2, 1}, {2, 0}, {4, 0}, {2, 1}}},
    PointsVector { {2, 1}, {2, 1 + 7e-3} }, // splits requested
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
        {},
        {
            { 200, test::Edge {200, 1, 1, Polyline2 { {{2, 1}, {2, 0}, {4, 0}, {2, 1}} } } }
        }
    },
    PrintInfo {
        PointsVector { {2, 1} },
        std::vector<PointGravity> { {Point2{2, 1}, GravityType::JunctionGravity} },
        std::map<EdgeID, test::GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_4_1)
{
    "Simple split request.",
    MockStorage {
        { test::Node { 1, {2, 2} } },
        { test::Edge { 1, 1, 1, Polyline2 { {{2, 2}, {2, 5}, {4, 4}, {2, 2}} } } }
    },
    SourceEdgeID { 100500, NOT_EXISTS },
    Polyline2 { PointsVector {{0, 3}, {2, 3}}},
    Polyline2 { PointsVector {{0, 3}, {2, 3}}},
    PointsVector { {2, 3} }, // splits requested
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
        { SourceEdgeID { 1, EXISTS }, EdgeIDVector {200, 1} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {0, 3} } },
            { 105, test::Node { 105, {2, 3} } }
        },
        {
            { 200, test::Edge { 200, 1, 105, Polyline2 { PointsVector {{2, 2}, {2, 3}} } } },
            { 1, test::Edge { 1, 105, 1, Polyline2 { {{2, 3}, {2, 5}, {4, 4}, {2, 2}} } } },
            { 205, test::Edge { 205, 100, 105, Polyline2 { PointsVector {{0, 3}, {2, 3}} } } }
        }
    },
    PrintInfo {
        PointsVector { {2, 3} },
        std::vector<PointGravity> { {Point2{2, 3}, GravityType::JunctionGravity} },
        std::map<EdgeID, test::GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_SUITE_END(intersection_loops)

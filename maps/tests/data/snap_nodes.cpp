#include "../../test_types/common.h"
#include "../suite.h"

#include "../../events_data.h"
#include "../../test_types/snap_nodes_test_data.h"

#include <yandex/maps/wiki/topo/events.h>

#include <maps/libs/geolib/include/point.h>
#include <maps/libs/geolib/include/segment.h>
#include <maps/libs/geolib/include/polyline.h>

#include <string>
#include <map>
#include <vector>

using namespace maps;
using namespace maps::wiki;
using namespace maps::wiki::topo;
using namespace maps::wiki::topo::test;
using namespace maps::geolib3;

TEST_SUITE_START(snap_nodes_tests, SnapNodesTestData)

TEST_DATA(test_1_1)
{
    "simple snap, dangling nodes",
    MockStorage {
        test::NodeDataVector {
            { 1, {1, 1} },
            { 2, {10, 1} },
            { 3, {1, 3} },
            { 4, {2, 0.9} },
            { 5, {7, 0.9} },
            { 6, {7, -1} }
        },
        test::EdgeDataVector {
            { 1, 1, 2, Polyline2 { PointsVector {{1, 1}, {10, 1}} } },
            { 2, 3, 4, Polyline2 { PointsVector {{1, 3}, {2, 0.9}} } },
            { 3, 5, 6, Polyline2 { PointsVector {{7, 0.9}, {7, -1}} } }
        }
    },
    NodeIDSet { 4, 5 },
    TopologyRestrictions {
        2e-1, // tolerance
        2e-1, // junction gravity
        2e-1, // vertex gravity
        2e-1, // group junction gravity
        2e-1, // group junction snap to vertex
        Limits<double> { 2e-1 }, // segment
        Limits<double> { 2e-1 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {
        { SourceEdgeID { 1, EXISTS }, EdgeIDVector {10, 1, 11} },
    },
    MockStorageDiff {
        {
            { 4, test::Node {4, {2, 1}} },
            { 5, test::Node {5, {7, 1}} }
        },
        {
            { 1, test::Edge {1, 4, 5, Polyline2 { PointsVector {{2, 1}, {7, 1}} } } },
            { 2, test::Edge {2, 3, 4, Polyline2 { PointsVector {{1, 3}, {2, 1}} } } },
            { 3, test::Edge {3, 5, 6, Polyline2 { PointsVector {{7, 1}, {7, -1}} } } },
            { 10, test::Edge {10, 1, 4, Polyline2 { PointsVector {{1, 1}, {2, 1}} } } },
            { 11, test::Edge {11, 5, 2, Polyline2 { PointsVector {{7, 1}, {10, 1}} } } }
        }
    }
};

TEST_DATA(test_1_3)
{
    "snap node, node degree 2",
    MockStorage {
        test::NodeDataVector {
            { 1, {1, 1} },
            { 2, {10, 1} },
            { 3, {1, 3} },
            { 4, {2, 0.9} },
            { 6, {2, -1} }
        },
        test::EdgeDataVector {
            { 1, 1, 2, Polyline2 { PointsVector {{1, 1}, {10, 1}} } },
            { 2, 3, 4, Polyline2 { PointsVector {{1, 3}, {2, 0.9}} } },
            { 3, 4, 6, Polyline2 { PointsVector {{2, 0.9}, {2, -1}} } }
        }
    },
    NodeIDSet { 4 },
    TopologyRestrictions {
        2e-1, // tolerance
        2e-1, // junction gravity
        2e-1, // vertex gravity
        2e-1, // group junction gravity
        2e-1, // group junction snap to vertex
        Limits<double> { 2e-1 }, // segment
        Limits<double> { 2e-1 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {
        { SourceEdgeID { 1, EXISTS }, EdgeIDVector {1, 10} },
    },
    MockStorageDiff {
        {
            { 4, test::Node {4, {2, 1}} }
        },
        {
            { 1, test::Edge {1, 1, 4, Polyline2 { PointsVector {{1, 1}, {2, 1}} } } },
            { 2, test::Edge {2, 3, 4, Polyline2 { PointsVector {{1, 3}, {2, 1}} } } },
            { 3, test::Edge {3, 4, 6, Polyline2 { PointsVector {{2, 1}, {2, -1}} } } },
            { 10, test::Edge {10, 4, 2, Polyline2 { PointsVector {{2, 1}, {10, 1}} } } }
        }
    }
};

TEST_DATA(test_2_1)
{
    "merge nodes",
    MockStorage {
        test::NodeDataVector {
            { 1, {1, 1} },
            { 2, {10, 1} },
            { 3, {1, 3} },
            { 4, {2, 1.1} },
            { 5, {2, 0.9} },
            { 6, {2, -1} }
        },
        test::EdgeDataVector {
            { 1, 1, 2, Polyline2 { PointsVector {{1, 1}, {10, 1}} } },
            { 2, 3, 4, Polyline2 { PointsVector {{1, 3}, {2, 1.1}} } },
            { 3, 5, 6, Polyline2 { PointsVector {{2, 0.9}, {2, -1}} } }
        }
    },
    NodeIDSet { 4, 5 },
    TopologyRestrictions {
        2e-1, // tolerance
        2e-1, // junction gravity
        2e-1, // vertex gravity
        2e-1, // group junction gravity
        2e-1, // group junction snap to vertex
        Limits<double> { 2e-1 }, // segment
        Limits<double> { 2e-1 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {
        { SourceEdgeID { 1, EXISTS }, EdgeIDVector {1, 10} },
    },
    MockStorageDiff {
        {
            { 4, test::Node {4, {2, 1}} },
            { 5, boost::none }
        },
        {
            { 1, test::Edge {1, 1, 4, Polyline2 { PointsVector {{1, 1}, {2, 1}} } } },
            { 2, test::Edge {2, 3, 4, Polyline2 { PointsVector {{1, 3}, {2, 1}} } } },
            { 3, test::Edge {3, 4, 6, Polyline2 { PointsVector {{2, 1}, {2, -1}} } } },
            { 10, test::Edge {10, 4, 2, Polyline2 { PointsVector {{2, 1}, {10, 1}} } } }
        }
    }
};

TEST_DATA(test_3_1)
{
    "complex snap",
    MockStorage {
        test::NodeDataVector {
            { 1, {2, 2} },
            { 2, {7.9, 2} },
            { 3, {8, 1} },
            { 4, {8, 9} },
            { 5, {7.9, 6} },
            { 6, {3.1, 6} },
            { 7, {3, 2.1} },
            { 8, {3, 9} },
            { 9, {5.5, 1.9} },
            { 10, {5.5, 0} },
            { 11, {2, 2.1} },
            { 12, {2, 6} },
            { 13, {1.9, 2} },
            { 14, {0, 2} },
            { 15, {1.99, 1.99} },
            { 16, {0, 0} }
        },
        test::EdgeDataVector {
            { 1, 1, 2, Polyline2 { PointsVector {{2, 2}, {7.9, 2}} } },
            { 2, 3, 4, Polyline2 { PointsVector {{8, 1}, {8, 9}} } },
            { 3, 5, 6, Polyline2 { PointsVector {{7.9, 6}, {3.1, 6}} } },
            { 4, 7, 8, Polyline2 { PointsVector {{3, 2.1}, {3, 9}} } },
            { 5, 9, 10, Polyline2 { PointsVector {{5.5, 1.9}, {5.5, 0}} } },
            { 6, 11, 12, Polyline2 { PointsVector {{2, 2.1}, {2, 6}} } },
            { 7, 13, 14, Polyline2 { PointsVector {{1.9, 2}, {0, 2}} } },
            { 8, 15, 16, Polyline2 { PointsVector {{1.99, 1.99}, {0, 0}} } }
        }
    },
    NodeIDSet { 2, 5, 6, 7, 9, 11, 13, 15 },
    TopologyRestrictions {
        2e-1, // tolerance
        2e-1, // junction gravity
        2e-1, // vertex gravity
        2e-1, // group junction gravity
        2e-1, // group junction snap to vertex
        Limits<double> { 2e-1 }, // segment
        Limits<double> { 2e-1 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {
        { SourceEdgeID { 1, EXISTS }, EdgeIDVector {1, 10, 11} },
        { SourceEdgeID { 2, EXISTS }, EdgeIDVector {2, 12, 13} },
        { SourceEdgeID { 4, EXISTS }, EdgeIDVector {4, 14} }
    },
    MockStorageDiff {
        {
            { 2, test::Node {2, {8, 2}} },
            { 5, test::Node {5, {8, 6}} },
            { 6, test::Node {6, {3, 6}} },
            { 7, test::Node {7, {3, 2}} },
            { 9, test::Node {9, {5.5, 2}} },
            { 11, boost::none },
            { 13, boost::none },
            { 15, boost::none }
        },
        {
            { 1, test::Edge {1, 1, 7, Polyline2 { PointsVector {{2, 2}, {3, 2}} } } },
            { 2, test::Edge {2, 3, 2, Polyline2 { PointsVector {{8, 1}, {8, 2}} } } },
            { 3, test::Edge {3, 5, 6, Polyline2 { PointsVector {{8, 6}, {3, 6}} } } },
            { 4, test::Edge {4, 7, 6, Polyline2 { PointsVector {{3, 2}, {3, 6}} } } },
            { 5, test::Edge {5, 9, 10, Polyline2 { PointsVector {{5.5, 2}, {5.5, 0}} } } },
            { 6, test::Edge {6, 1, 12, Polyline2 { PointsVector {{2, 2}, {2, 6}} } } },
            { 7, test::Edge {7, 1, 14, Polyline2 { PointsVector {{2, 2}, {0, 2}} } } },
            { 8, test::Edge {8, 1, 16, Polyline2 { PointsVector {{2, 2}, {0, 0}} } } },
            { 10, test::Edge {10, 7, 9, Polyline2 { PointsVector {{3, 2}, {5.5, 2}} } } },
            { 11, test::Edge {11, 9, 2, Polyline2 { PointsVector {{5.5, 2}, {8, 2}} } } },
            { 12, test::Edge {12, 2, 5, Polyline2 { PointsVector {{8, 2}, {8, 6}} } } },
            { 13, test::Edge {13, 5, 4, Polyline2 { PointsVector {{8, 6}, {8, 9}} } } },
            { 14, test::Edge {14, 6, 8, Polyline2 { PointsVector {{3, 6}, {3, 9}} } } }
        }
    }
};

TEST_DATA(test_4_1)
{
    "merge nodes, snap to node only after snap to edge",
    MockStorage {
        test::NodeDataVector {
            { 1, {1, 4} },
            { 2, {10, 4} },
            { 3, {1 + 1.9e-1, 4 - 1.9e-1} },
            { 4, {10, 1} }
        },
        test::EdgeDataVector {
            { 1, 1, 2, Polyline2 { PointsVector {{1, 4}, {10, 4}} } },
            { 2, 3, 4, Polyline2 { PointsVector {{1 + 1.9e-1, 4 - 1.9e-1}, {10, 1}} } }
        }
    },
    NodeIDSet { 3 },
    TopologyRestrictions {
        2e-1, // tolerance
        2e-1, // junction gravity
        2e-1, // vertex gravity
        2e-1, // group junction gravity
        2e-1, // group junction snap to vertex
        Limits<double> { 2e-1 }, // segment
        Limits<double> { 2e-1 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {},
    MockStorageDiff {
        {
            { 3, boost::none }
        },
        {
            { 2, test::Edge {2, 1, 4, Polyline2 { PointsVector {{1, 4}, {10, 1}} } } }
        }
    }
};

TEST_SUITE_END(snap_nodes_tests)

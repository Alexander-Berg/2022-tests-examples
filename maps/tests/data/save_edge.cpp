#include "../../test_types/common.h"
#include "../suite.h"

#include "../../events_data.h"
#include "../../test_types/save_edge_test_data.h"

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

TEST_SUITE_START(editing_edges_tests, SaveEdgeTestData)

TEST_DATA(Test_1_1)
{
    "Create isolated edge in empty dataset.",
    MockStorage {
        test::NodeDataVector {},
        test::EdgeDataVector {}
    },
    {1, NOT_EXISTS},     // edge id
    Polyline2 { {{3, 5}, {5, 3}, {2, 3}} }, // new geometry
    Polyline2 { {{3, 5}, {5, 3}, {2, 3}} }, // aligned geometry
    PointsVector {},    // split points
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
            { 1, test::Node {1, {3, 5}} },
            { 2, test::Node {2, {2, 3}} }
        },
        {
            { 1, test::Edge {1, 1, 2, Polyline2 { {{3, 5}, {5, 3}, {2, 3}} } } }
        }
    }
};

TEST_DATA(Test_2_1)
{
    "Add new edge that intersects existing one.",
    MockStorage {
        {
            test::Node {1, {1, 1}},
            test::Node {2, {5, 5}}
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{1, 1}, {5, 5}} } }
        }
    },
    {2, NOT_EXISTS},     // edge id
    Polyline2 { {{3, 5}, {5, 3}, {2, 3}} }, // new polyline
    Polyline2 { {{3, 5}, {5, 3}, {2, 3}} }, // aigned polyline
    PointsVector {},    // split points
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
        { SourceEdgeID { 1, EXISTS }, EdgeIDVector {1, 3, 4} },
        { SourceEdgeID { 2, NOT_EXISTS }, EdgeIDVector {6, 2, 5} }
    },
    MockStorageDiff {
        {
            { 3, test::Node {3, {2, 3}} },
            { 4, test::Node {4, {3, 5}} },
            { 5, test::Node {5, {3, 3}} },
            { 6, test::Node {6, {4, 4}} }
        },
        {
            { 1, test::Edge {1, 1, 5, Polyline2 { PointsVector {{1, 1}, {3, 3}} } } },
            { 2, test::Edge {2, 6, 5, Polyline2 { {{4, 4}, {5, 3}, {3, 3}} } } },
            { 3, test::Edge {3, 5, 6, Polyline2 { PointsVector {{3, 3}, {4, 4}} } } },
            { 4, test::Edge {4, 6, 2, Polyline2 { PointsVector {{4, 4}, {5, 5}} } } },
            { 5, test::Edge {5, 5, 3, Polyline2 { PointsVector {{3, 3}, {2, 3}} } } },
            { 6, test::Edge {6, 4, 6, Polyline2 { PointsVector {{3, 5}, {4, 4}} } } }
        }
    }
};

TEST_DATA(Test_2_2)
{
    "Edit existing edge.",
    MockStorage {
        {
            test::Node {1, {1, 1}},
            test::Node {2, {1, 4}},
            test::Node {3, {4, 3}}
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{1, 1}, {1, 4}} } },
            test::Edge {2, 1, 3, Polyline2 { PointsVector {{1, 1}, {4, 3}} } }
        }
    },
    {2, EXISTS},     // edge id
    Polyline2 { PointsVector {{1, 4}, {4, 3}} }, // new polyline
    Polyline2 { PointsVector {{1, 4}, {4, 3}} }, // aligned polyline
    PointsVector {},    // split points
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
            { 3, test::Node {3, {4, 3}} }
        },
        {
            { 2, test::Edge {2, 2, 3, Polyline2 { PointsVector {{1, 4}, {4, 3}} } } }
        }
    }
};

TEST_DATA(Test_2_3)
{
    "Add new edge. Existing nodes are used.",
    MockStorage {
        {
            test::Node {1, {1, 2}},
            test::Node {2, {1, 4}},
            test::Node {3, {4, 2}},
            test::Node {4, {6, 4}},
            test::Node {5, {5, 1}}
        },
        {
            test::Edge {2, 1, 2, Polyline2 { PointsVector {{1, 2}, {1, 4}} } },
            test::Edge {3, 3, 4, Polyline2 { PointsVector {{4, 2}, {6, 4}} } },
            test::Edge {4, 3, 5, Polyline2 { PointsVector {{4, 2}, {5, 1}} } }
        }
    },
    {1, NOT_EXISTS},     // edge id
    Polyline2 { PointsVector {{1, 2}, {4, 2}} }, // new polyline
    Polyline2 { PointsVector {{1, 2}, {4, 2}} }, // aligned aligned
    PointsVector {},    // split points
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
            { 1, test::Edge {1, 1, 3, Polyline2 { PointsVector {{1, 2}, {4, 2}} } } }
        }
    }
};

TEST_DATA(Test_2_4)
{
    "Edit edge. New geometry intersects previous one.",
    MockStorage {
        {
            test::Node {1, {3, 3}},
            test::Node {2, {6, 3}}
        },
        {
            test::Edge {3, 1, 2, Polyline2 { PointsVector {{3, 3}, {6, 3}} } }
        }
    },
    {3, EXISTS},     // edge id
    Polyline2 { {{1, 2}, {5, 4}, {8, 1}} }, // new polyline
    Polyline2 { {{1, 2}, {5, 4}, {8, 1}} }, // aligned polyline
    PointsVector {},    // split points
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
            { 1, test::Node {1, {1, 2}} },
            { 2, test::Node {2, {8, 1}} }
        },
        {
            { 3, test::Edge {3, 1, 2, Polyline2 { {{1, 2}, {5, 4}, {8, 1}} } } }
        }
    }
};

TEST_DATA(Test_2_5)
{
    "Add new edge, that overlappes with existing.",
    MockStorage {
        {
            test::Node {1, {0, 1}},
            test::Node {2, {6, 1}}
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{0, 1}, {6, 1}} } }
        }
    },
    {2, NOT_EXISTS},     // edge id
    Polyline2 { PointsVector{{5, 1}, {2, 1}} }, // new polyline
    Polyline2 { PointsVector{{5, 1}, {2, 1}} }, // aligned polyline
    PointsVector {},    // split points
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
        { SourceEdgeID { 1, EXISTS }, EdgeIDVector {1, 2, 3} },
        { SourceEdgeID { 2, NOT_EXISTS }, EdgeIDVector {2} }
    },
    MockStorageDiff {
        {
            { 3, test::Node {3, {2, 1}} },
            { 4, test::Node {4, {5, 1}} }
        },
        {
            { 1, test::Edge {1, 1, 3, Polyline2 { PointsVector{{0, 1}, {2, 1}} } } },
            { 2, test::Edge {2, 3, 4, Polyline2 { PointsVector{{2, 1}, {5, 1}} } } },
            { 3, test::Edge {3, 4, 2, Polyline2 { PointsVector{{5, 1}, {6, 1}} } } }
        }
    }
};

TEST_DATA(Test_2_6)
{
    "Simple case of partitial overlapping.",
    MockStorage {
        {
            test::Node {1, {0, 0}},
            test::Node {2, {6, 3}}
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{0, 0}, {6, 3}} } }
        }
    },
    {2, NOT_EXISTS},     // edge id
    Polyline2 { PointsVector{{2, 5}, {2, 1}, {4, 2}, {4, 5}} }, // new polyline
    Polyline2 { PointsVector{{2, 5}, {2, 1}, {4, 2}, {4, 5}} }, // aligned polyline
    PointsVector {},    // split points
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
        { SourceEdgeID { 1, EXISTS }, EdgeIDVector {1, 3, 4} },
        { SourceEdgeID { 2, NOT_EXISTS }, EdgeIDVector {2, 3, 5} }
    },
    MockStorageDiff {
        {
            { 3, test::Node {3, {2, 5}} },
            { 4, test::Node {4, {2, 1}} },
            { 5, test::Node {5, {4, 2}} },
            { 6, test::Node {6, {4, 5}} }
        },
        {
            { 1, test::Edge {1, 1, 4, Polyline2 { PointsVector{{0, 0}, {2, 1}} } } },
            { 2, test::Edge {2, 3, 4, Polyline2 { PointsVector{{2, 5}, {2, 1}} } } },
            { 3, test::Edge {3, 4, 5, Polyline2 { PointsVector{{2, 1}, {4, 2}} } } },
            { 4, test::Edge {4, 5, 2, Polyline2 { PointsVector{{4, 2}, {6, 3}} } } },
            { 5, test::Edge {5, 5, 6, Polyline2 { PointsVector{{4, 2}, {4, 5}} } } }
        }
    }
};

TEST_DATA(Test_2_7)
{
    "Full overlapping with 2 existing edges.",
    MockStorage {
        {
            test::Node {1, {1, 1}},
            test::Node {2, {3, 3}},
            test::Node {3, {4, 4}}
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{1, 1}, {3, 3}} } },
            test::Edge {2, 2, 3, Polyline2 { PointsVector {{3, 3}, {4, 4}} } }
        }
    },
    {3, NOT_EXISTS},     // edge id
    Polyline2 { PointsVector{{4, 4}, {1, 1}} }, // new polyline
    Polyline2 { PointsVector{{4, 4}, {1, 1}} }, // aligned polyline
    PointsVector {},    // split points
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
        { SourceEdgeID { 1, EXISTS }, EdgeIDVector {1} },
        { SourceEdgeID { 2, EXISTS }, EdgeIDVector {3} },
        { SourceEdgeID { 3, NOT_EXISTS }, EdgeIDVector {3, 1} }
    },
    MockStorageDiff {
        {},
        {
            { 1, test::Edge {1, 1, 2, Polyline2 { PointsVector{{1, 1}, {3, 3}} } } },
            { 2, boost::none },
            { 3, test::Edge {3, 2, 3, Polyline2 { PointsVector{{3, 3}, {4, 4}} } } }
        }
    }
};

TEST_DATA(Test_3_1)
{
    "Add new vertex to edge.",
    MockStorage {
        {
            test::Node {1, {1, 4}},
            test::Node {2, {2, 2}},
            test::Node {3, {6, 2}}
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{1, 4}, {2, 2}} } },
            test::Edge {2, 2, 3, Polyline2 { PointsVector {{2, 2}, {6, 2}} } }
        }
    },
    {2, EXISTS},     // edge id
    Polyline2 { {{2, 2}, {4, 3}, {6, 2}} }, // new polyline
    Polyline2 { {{2, 2}, {4, 3}, {6, 2}} }, // aligned polyline
    PointsVector {},    // split points
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
            { 2, test::Edge {2, 2, 3, Polyline2 { {{2, 2}, {4, 3}, {6, 2}} } } }
        }
    }
};

TEST_DATA(Test_3_2)
{
    "Complex overlapping.",
    MockStorage {
        {
            test::Node {1, {-2, 2}},
            test::Node {2, {4, 0}},
            test::Node {3, {-3, 0}},
            test::Node {4, {2, -2}}
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{-2, 2}, {0, 0}, {4, 0}} } },
            test::Edge {2, 3, 4, Polyline2 { PointsVector {{-3, 0}, {2, -2}} } }
        }
    },
    {2, EXISTS},     // edge id
    Polyline2 { PointsVector {{-3, 0}, {-1, 1}, {0, 0}, {2, 0}, {2, -2}} }, // new polyline
    Polyline2 { PointsVector {{-3, 0}, {-1, 1}, {0, 0}, {2, 0}, {2, -2}} }, // aligned polyline
    PointsVector {},    // split points
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
        { SourceEdgeID { 1, EXISTS }, EdgeIDVector {1, 3, 4} },
        { SourceEdgeID { 2, EXISTS }, EdgeIDVector {2, 3, 5} }
    },
    MockStorageDiff {
        {
            { 5, test::Node {5, {-1, 1}} },
            { 6, test::Node {6, {2, 0}} }
        },
        {
            { 1, test::Edge {1, 1, 5, Polyline2 { PointsVector{{-2, 2}, {-1, 1}} } } },
            { 2, test::Edge {2, 3, 5, Polyline2 { PointsVector{{-3, 0}, {-1, 1}} } } },
            { 3, test::Edge {3, 5, 6, Polyline2 { PointsVector{{-1, 1}, {0, 0}, {2, 0}} } } },
            { 4, test::Edge {4, 6, 2, Polyline2 { PointsVector{{2, 0}, {4, 0}} } } },
            { 5, test::Edge {5, 6, 4, Polyline2 { PointsVector{{2, 0}, {2, -2}} } } }
        }
    }
};

TEST_DATA(Test_3_3)
{
    "Double overlapping.",
    MockStorage {
        {
            test::Node {1, {-4, 1}},
            test::Node {2, {5, 1}}
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{-4, 1}, {-1, 1}, {1, -2}, {3, 1}, {5, 1}} } },
        }
    },
    {2, NOT_EXISTS},     // edge id
    Polyline2 { PointsVector {{-2, 1}, {4, 1}} }, // new polyline
    Polyline2 { PointsVector {{-2, 1}, {4, 1}} }, // aligned polyline
    PointsVector {},    // split points
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
        { SourceEdgeID { 1, EXISTS }, EdgeIDVector {1, 6, 3, 4, 5} },
        { SourceEdgeID { 2, NOT_EXISTS }, EdgeIDVector {6, 2, 4} }
    },
    MockStorageDiff {
        {
            { 3, test::Node {3, {-2, 1}} },
            { 4, test::Node {4, {-1, 1}} },
            { 5, test::Node {5, {3, 1}} },
            { 6, test::Node {6, {4, 1}} }
        },
        {
            { 1, test::Edge {1, 1, 3, Polyline2 { PointsVector{{-4, 1}, {-2, 1}} } } },
            { 2, test::Edge {2, 4, 5, Polyline2 { PointsVector{{-1, 1}, {3, 1}} } } },
            { 3, test::Edge {3, 4, 5, Polyline2 { PointsVector{{-1, 1}, {1, -2}, {3, 1}} } } },
            { 4, test::Edge {4, 5, 6, Polyline2 { PointsVector{{3, 1}, {4, 1}} } } },
            { 5, test::Edge {5, 6, 2, Polyline2 { PointsVector{{4, 1}, {5, 1}} } } },
            { 6, test::Edge {6, 3, 4, Polyline2 { PointsVector{{-2, 1}, {-1, 1}} } } }
        }
    }
};

TEST_DATA(Test_3_4)
{
    "Common segment.",
    MockStorage {
        {
            test::Node {1, {2, 2}}
        },
        {
            test::Edge {1, 1, 1, Polyline2 { PointsVector {{2, 2}, {0, 2}, {0, 0}, {2, 0}, {2, 2}} } },
        }
    },
    {2, NOT_EXISTS},     // edge id
    Polyline2 { PointsVector {{0, 2}, {-2, 2}, {-2, 0}, {0, 0}, {0, 2}} }, // new polyline
    Polyline2 { PointsVector {{0, 2}, {-2, 2}, {-2, 0}, {0, 0}, {0, 2}} }, // aligned polyline
    PointsVector {},    // split points
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
        { SourceEdgeID { 1, EXISTS }, EdgeIDVector {1, 3, 4} },
        { SourceEdgeID { 2, NOT_EXISTS }, EdgeIDVector {2, 3} }
    },
    MockStorageDiff {
        {
            { 2, test::Node {2, {0, 2}} },
            { 3, test::Node {3, {0, 0}} },
        },
        {
            { 1, test::Edge {1, 1, 2, Polyline2 { PointsVector{{2, 2}, {0, 2}} } } },
            { 2, test::Edge {2, 2, 3, Polyline2 { PointsVector{{0, 2}, {-2, 2}, {-2, 0}, {0, 0}} } } },
            { 3, test::Edge {3, 2, 3, Polyline2 { PointsVector{{0, 2}, {0, 0}} } } },
            { 4, test::Edge {4, 3, 1, Polyline2 { PointsVector{{0, 0}, {2, 0}, {2, 2}} } } }
        }
    }
};


TEST_DATA(Test_4_1)
{
    "Add new edge, multiple intersections.",
    MockStorage {
        {
            test::Node {1, {1, 1}},
            test::Node {2, {3, 5}},
            test::Node {3, {7, 5}},
            test::Node {4, {7, 2}}
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{1, 1}, {3, 5}} } },
            test::Edge {2, 2, 3, Polyline2 { PointsVector {{3, 5}, {7, 5}} } },
            test::Edge {3, 3, 4, Polyline2 { {{7, 5}, {4, 2}, {7, 2}} } }
        }
    },
    {4, NOT_EXISTS},     // edge id
    Polyline2 { {{5, 6}, {2, 3}, {6, 1}, {6, 6}} }, // new polyline
    Polyline2 { {{5, 6}, {2, 3}, {6, 1}, {6, 6}} }, // aligned polyline
    PointsVector {},    // split points
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
        { SourceEdgeID { 1, EXISTS }, EdgeIDVector {1, 5} },
        { SourceEdgeID { 2, EXISTS }, EdgeIDVector {2, 6, 7} },
        { SourceEdgeID { 3, EXISTS }, EdgeIDVector {8, 3, 9, 10} },
        { SourceEdgeID { 4, NOT_EXISTS }, EdgeIDVector {12, 11, 4, 13, 14, 15, 16} }
    },
    MockStorageDiff {
        {
            { 5, test::Node {5, {5, 6}} },
            { 6, test::Node {6, {6, 6}} },
            { 7, test::Node {7, {2, 3}} },
            { 8, test::Node {8, {4, 5}} },
            { 9, test::Node {9, {4, 2}} },
            { 10, test::Node {10, {6, 2}} },
            { 11, test::Node {11, {6, 4}} },
            { 12, test::Node {12, {6, 5}} }
        },
        {
            { 1, test::Edge {1, 1, 7, Polyline2 { PointsVector {{1, 1}, {2, 3}} } } },
            { 2, test::Edge {2, 2, 8, Polyline2 { PointsVector {{3, 5}, {4, 5}} } } },
            { 3, test::Edge {3, 11, 9, Polyline2 { PointsVector {{6, 4}, {4, 2}} } } },
            { 4, test::Edge {4, 7, 9, Polyline2 { PointsVector {{2, 3}, {4, 2}} } } },
            { 5, test::Edge {5, 7, 2, Polyline2 { PointsVector {{2, 3}, {3, 5}} } } },
            { 6, test::Edge {6, 8, 12, Polyline2 { PointsVector {{4, 5}, {6, 5}} } } },
            { 7, test::Edge {7, 12, 3, Polyline2 { PointsVector {{6, 5}, {7, 5}} } } },
            { 8, test::Edge {8, 3, 11, Polyline2 { PointsVector {{7, 5}, {6, 4}} } } },
            { 9, test::Edge {9, 9, 10, Polyline2 { PointsVector {{4, 2}, {6, 2}} } } },
            { 10, test::Edge {10, 10, 4, Polyline2 { PointsVector {{6, 2}, {7, 2}} } } },
            { 11, test::Edge {11, 8, 7, Polyline2 { PointsVector {{4, 5}, {2, 3}} } } },
            { 12, test::Edge {12, 5, 8, Polyline2 { PointsVector {{5, 6}, {4, 5}} } } },
            { 13, test::Edge {13, 9, 10, Polyline2 { {{4, 2}, {6, 1}, {6, 2}} } } },
            { 14, test::Edge {14, 10, 11, Polyline2 { PointsVector {{6, 2}, {6, 4}} } } },
            { 15, test::Edge {15, 11, 12, Polyline2 { PointsVector {{6, 4}, {6, 5}} } } },
            { 16, test::Edge {16, 12, 6, Polyline2 { PointsVector {{6, 5}, {6, 6}} } } }
        }
    }
};

TEST_DATA(Test_4_2)
{
    "Edit existing edge, multiple intersections.",
    MockStorage {
        {
            test::Node {1, {1, 1}},
            test::Node {2, {3, 5}},
            test::Node {3, {7, 5}},
            test::Node {4, {7, 2}}
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{1, 1}, {3, 5}} } },
            test::Edge {2, 2, 3, Polyline2 { PointsVector {{3, 5}, {7, 5}} } },
            test::Edge {3, 3, 4, Polyline2 { {{7, 5}, {4, 2}, {7, 2}} } },
            test::Edge {4, 3, 4, Polyline2 { PointsVector {{7, 5}, {7, 2}} } }
        }
    },
    {4, EXISTS},     // edge id
    Polyline2 { {{5, 6}, {2, 3}, {6, 1}, {6, 6}} }, // new polyline
    Polyline2 { {{5, 6}, {2, 3}, {6, 1}, {6, 6}} }, // aligned polyline
    PointsVector {},    // split points
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
        { SourceEdgeID { 1, EXISTS }, EdgeIDVector {1, 5} },
        { SourceEdgeID { 2, EXISTS }, EdgeIDVector {2, 6, 7} },
        { SourceEdgeID { 3, EXISTS }, EdgeIDVector {8, 3, 9, 10} },
        { SourceEdgeID { 4, EXISTS }, EdgeIDVector {12, 11, 4, 13, 14, 15, 16} }
    },
    MockStorageDiff {
        {
            { 5, test::Node {5, {5, 6}} },
            { 6, test::Node {6, {6, 6}} },
            { 7, test::Node {7, {2, 3}} },
            { 8, test::Node {8, {4, 5}} },
            { 9, test::Node {9, {4, 2}} },
            { 10, test::Node {10, {6, 2}} },
            { 11, test::Node {11, {6, 4}} },
            { 12, test::Node {12, {6, 5}} }
        },
        {
            { 1, test::Edge {1, 1, 7, Polyline2 { PointsVector {{1, 1}, {2, 3}} } } },
            { 2, test::Edge {2, 2, 8, Polyline2 { PointsVector {{3, 5}, {4, 5}} } } },
            { 3, test::Edge {3, 11, 9, Polyline2 { PointsVector {{6, 4}, {4, 2}} } } },
            { 4, test::Edge {4, 7, 9, Polyline2 { PointsVector {{2, 3}, {4, 2}} } } },
            { 5, test::Edge {5, 7, 2, Polyline2 { PointsVector {{2, 3}, {3, 5}} } } },
            { 6, test::Edge {6, 8, 12, Polyline2 { PointsVector {{4, 5}, {6, 5}} } } },
            { 7, test::Edge {7, 12, 3, Polyline2 { PointsVector {{6, 5}, {7, 5}} } } },
            { 8, test::Edge {8, 3, 11, Polyline2 { PointsVector {{7, 5}, {6, 4}} } } },
            { 9, test::Edge {9, 9, 10, Polyline2 { PointsVector {{4, 2}, {6, 2}} } } },
            { 10, test::Edge {10, 10, 4, Polyline2 { PointsVector {{6, 2}, {7, 2}} } } },
            { 11, test::Edge {11, 8, 7, Polyline2 { PointsVector {{4, 5}, {2, 3}} } } },
            { 12, test::Edge {12, 5, 8, Polyline2 { PointsVector {{5, 6}, {4, 5}} } } },
            { 13, test::Edge {13, 9, 10, Polyline2 { {{4, 2}, {6, 1}, {6, 2}} } } },
            { 14, test::Edge {14, 10, 11, Polyline2 { PointsVector {{6, 2}, {6, 4}} } } },
            { 15, test::Edge {15, 11, 12, Polyline2 { PointsVector {{6, 4}, {6, 5}} } } },
            { 16, test::Edge {16, 12, 6, Polyline2 { PointsVector {{6, 5}, {6, 6}} } } }
        }
    }
};

TEST_DATA(Test_4_3)
{
    "Add new edge, complex multiple overlapping of closed polylines.",
    MockStorage {
        {
            test::Node {1, {0, 0}},
            test::Node {2, {5, 2}},
        },
        {
            test::Edge {1, 1, 1, Polyline2 { PointsVector {{0, 0}, {0, 7}, {6, 7}, {6, 0}, {0, 0}} } },
            test::Edge {2, 2, 2, Polyline2 { PointsVector {{5, 2}, {5, 4}, {2, 4}, {2, 2}, {5, 2}} } }
        }
    },
    {3, NOT_EXISTS},     // edge id
    Polyline2 { {{0, 4}, {2, 4}, {2, 0}, {0, 0}, {0, 4}} }, // new polyline
    Polyline2 { {{0, 4}, {2, 4}, {2, 0}, {0, 0}, {0, 4}} }, // aligned polyline
    PointsVector {},    // split points
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
        { SourceEdgeID { 1, EXISTS }, EdgeIDVector {4, 1, 5} },
        { SourceEdgeID { 2, EXISTS }, EdgeIDVector {2, 6, 8} },
        { SourceEdgeID { 3, NOT_EXISTS }, EdgeIDVector {3, 6, 7, 5, 4} },
    },
    MockStorageDiff {
        {
            { 3, test::Node {3, {0, 4}} },
            { 4, test::Node {4, {2, 4}} },
            { 5, test::Node {5, {2, 2}} },
            { 6, test::Node {6, {2, 0}} },
        },
        {
            { 1, test::Edge {1, 3, 6, Polyline2 { PointsVector {{0, 4}, {0, 7}, {6, 7}, {6, 0}, {2, 0}} } } },
            { 2, test::Edge {2, 2, 4, Polyline2 { PointsVector {{5, 2}, {5, 4}, {2, 4}} } } },
            { 3, test::Edge {3, 3, 4, Polyline2 { PointsVector {{0, 4}, {2, 4}} } } },
            { 4, test::Edge {4, 1, 3, Polyline2 { PointsVector {{0, 0}, {0, 4}} } } },
            { 5, test::Edge {5, 6, 1, Polyline2 { PointsVector {{2, 0}, {0, 0}} } } },
            { 6, test::Edge {6, 4, 5, Polyline2 { PointsVector {{2, 4}, {2, 2}} } } },
            { 7, test::Edge {7, 5, 6, Polyline2 { PointsVector {{2, 2}, {2, 0}} } } },
            { 8, test::Edge {8, 5, 2, Polyline2 { PointsVector {{2, 2}, {5, 2}} } } }
        }
    }
};

TEST_DATA(Test_4_4)
{
    "Add new edge, complex multiple overlapping.",
    MockStorage {
        {
            test::Node {1, {-4, -2}},
            test::Node {2, {2, -2}},
            test::Node {3, {3, 2}},
            test::Node {4, {3, -1}},
            test::Node {5, {-4, 3}},
            test::Node {6, {2, 3}}
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{-4, -2}, {2, -2}} } },
            test::Edge {2, 3, 4, Polyline2 { PointsVector {{3, 2}, {3, -1}} } },
            test::Edge {3, 5, 6, Polyline2 { PointsVector {{-4, 3}, {2, 3}} } }
        }
    },
    {4, NOT_EXISTS}, // edge id
    Polyline2 { {{-4, -2}, {-2, -2}, {-2, -1}, {1, -1}, {1, -2}, {3, 0}, {3, 3}, {-1, 3}} }, // new polyline
    Polyline2 { {{-4, -2}, {-2, -2}, {-2, -1}, {1, -1}, {1, -2}, {3, 0}, {3, 3}, {-1, 3}} }, // aligned polyline
    PointsVector {},    // split points
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
        { SourceEdgeID { 1, EXISTS }, EdgeIDVector {5, 1, 6} },
        { SourceEdgeID { 2, EXISTS }, EdgeIDVector {8, 2} },
        { SourceEdgeID { 3, EXISTS }, EdgeIDVector {3, 10} },
        { SourceEdgeID { 4, NOT_EXISTS }, EdgeIDVector {5, 4, 7, 8, 9, 10} }
    },
    MockStorageDiff {
        {
            { 7, test::Node {7, {-2, -2}} },
            { 8, test::Node {8, {1, -2}} },
            { 9, test::Node {9, {3, 0}} },
            { 10, test::Node {10, {-1, 3}} }
        },
        {
            { 1, test::Edge {1, 7, 8, Polyline2 { PointsVector {{-2, -2}, {1, -2}} } } },
            { 2, test::Edge {2, 9, 4, Polyline2 { PointsVector {{3, 0}, {3, -1}} } } },
            { 3, test::Edge {3, 5, 10, Polyline2 { PointsVector {{-4, 3}, {-1, 3}} } } },
            { 5, test::Edge {5, 1, 7, Polyline2 { PointsVector {{-4, -2}, {-2, -2}} } } },
            { 4, test::Edge {4, 7, 8, Polyline2 { PointsVector {{-2, -2}, {-2, -1}, {1, -1}, {1, -2}} } } },
            { 6, test::Edge {6, 8, 2, Polyline2 { PointsVector {{1, -2}, {2, -2}} } } },
            { 7, test::Edge {7, 8, 9, Polyline2 { PointsVector {{1, -2}, {3, 0}} } } },
            { 8, test::Edge {8, 3, 9, Polyline2 { PointsVector {{3, 2}, {3, 0}} } } },
            { 9, test::Edge {9, 3, 6, Polyline2 { PointsVector {{3, 2}, {3, 3}, {2, 3}} } } },
            { 10, test::Edge {10, 10, 6, Polyline2 { PointsVector {{-1, 3}, {2, 3}} } } }
        }
    }
};

TEST_DATA(Test_5_1)
{
    "Add new edge with requested split points.",
    MockStorage {
        {
            test::Node {1, {2, 1}},
            test::Node {2, {2, 5}}
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{2, 1}, {2, 5}} } }
        }
    },
    {2, NOT_EXISTS},     // edge id
    Polyline2 { PointsVector {{1, 2}, {6, 2}} }, // new polyline
    Polyline2 { PointsVector {{1, 2}, {4, 2}, {6, 2}} }, // aligned polyline
    PointsVector { {4, 2} },    // split points
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
        { SourceEdgeID { 1, EXISTS }, EdgeIDVector {3, 1} },
        { SourceEdgeID { 2, NOT_EXISTS }, EdgeIDVector {4, 2, 5} }
    },
    MockStorageDiff {
        {
            { 3, test::Node {3, {1, 2}} },
            { 4, test::Node {4, {2, 2}} },
            { 5, test::Node {5, {4, 2}} },
            { 6, test::Node {6, {6, 2}} }
        },
        {
            { 1, test::Edge {1, 4, 2, Polyline2 { PointsVector {{2, 2}, {2, 5}} } } },
            { 2, test::Edge {2, 4, 5, Polyline2 { PointsVector {{2, 2}, {4, 2}} } } },
            { 3, test::Edge {3, 1, 4, Polyline2 { PointsVector {{2, 1}, {2, 2}} } } },
            { 4, test::Edge {4, 3, 4, Polyline2 { PointsVector {{1, 2}, {2, 2}} } } },
            { 5, test::Edge {5, 5, 6, Polyline2 { PointsVector {{4, 2}, {6, 2}} } } }
        }
    }
};

TEST_DATA(Test_5_2)
{
    "Edit edge with requested split points.",
    MockStorage {
        {
            test::Node {1, {2, 1}},
            test::Node {2, {2, 5}},
            test::Node {7, {8, 1}},
            test::Node {8, {8, 5}}
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{2, 1}, {2, 5}} } },
            test::Edge {2, 7, 8, Polyline2 { PointsVector {{8, 1}, {8, 5}} } }
        }
    },
    {2, EXISTS},     // edge id
    Polyline2 { PointsVector {{1, 2}, {6, 2}} }, // new polyline
    Polyline2 { PointsVector {{1, 2}, {4, 2}, {6, 2}} }, // aligned polyline
    PointsVector { {4, 2} },    // split points
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
        { SourceEdgeID { 1, EXISTS }, EdgeIDVector {3, 1} },
        { SourceEdgeID { 2, EXISTS }, EdgeIDVector {4, 2, 5} }
    },
    MockStorageDiff {
        {
            { 4, test::Node {4, {2, 2}} },
            { 5, test::Node {5, {4, 2}} },
            { 7, test::Node {7, {1, 2}} },
            { 8, test::Node {8, {6, 2}} }
        },
        {
            { 1, test::Edge {1, 4, 2, Polyline2 { PointsVector {{2, 2}, {2, 5}} } } },
            { 2, test::Edge {2, 4, 5, Polyline2 { PointsVector {{2, 2}, {4, 2}} } } },
            { 3, test::Edge {3, 1, 4, Polyline2 { PointsVector {{2, 1}, {2, 2}} } } },
            { 4, test::Edge {4, 7, 4, Polyline2 { PointsVector {{1, 2}, {2, 2}} } } },
            { 5, test::Edge {5, 5, 8, Polyline2 { PointsVector {{4, 2}, {6, 2}} } } }
        }
    }
};

TEST_DATA(Test_6_1)
{
    "Move edge end.",
    MockStorage {
        {
            test::Node {1, {1, 1}},
            test::Node {2, {1, 4}},
            test::Node {3, {4, 2}}
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{1, 1}, {1, 4}} } },
            test::Edge {2, 1, 3, Polyline2 { PointsVector {{1, 1}, {4, 2}} } }
        }
    },
    {2, EXISTS},     // edge id
    Polyline2 { PointsVector {{4, 2}, {2, 4}} }, // new polyline
    Polyline2 { PointsVector {{4, 2}, {2, 4}} }, // aligned polyline
    PointsVector {},    // split points
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
            { 5, test::Node {5, {4, 2}} },
            { 3, test::Node {3, {2, 4}} }
        },
        {
            { 2, test::Edge {2, 5, 3, Polyline2 { PointsVector {{4, 2}, {2, 4}} } } }
        }
    }
};

TEST_DATA(Test_6_2)
{
    "Move edge end with overlapping.",
    MockStorage {
        {
            test::Node {1, {1, 1}},
            test::Node {2, {5, 3}},
            test::Node {3, {5, 0}}
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{1, 1}, {5, 3}} } },
            test::Edge {2, 2, 3, Polyline2 { PointsVector {{5, 3}, {5, 0}} } }
        }
    },
    {2, EXISTS},     // edge id
    Polyline2 { PointsVector {{5, 3}, {3, 2}} }, // new polyline
    Polyline2 { PointsVector {{5, 3}, {3, 2}} }, // aligned polyline
    PointsVector {},    // split points
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
        { SourceEdgeID { 1, EXISTS }, EdgeIDVector {1, 2} },
        { SourceEdgeID { 2, EXISTS }, EdgeIDVector {2} }
    },
    MockStorageDiff {
        {
            { 3, test::Node {3, {3, 2}} },
        },
        {
            { 1, test::Edge {1, 1, 3, Polyline2 { PointsVector {{1, 1}, {3, 2}} } } },
            { 2, test::Edge {2, 3, 2, Polyline2 { PointsVector {{3, 2}, {5, 3}} } } }
        }
    }
};

TEST_DATA(Test_7_1)
{
    "Swap endpoints.",
    MockStorage {
        {
            test::Node {1, {1, 1}},
            test::Node {2, {1, 4}}
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{1, 1}, {1, 4}} } }
        }
    },
    {1, EXISTS},     // edge id
    Polyline2 { PointsVector {{1, 4}, {1, 1}} }, // new polyline
    Polyline2 { PointsVector {{1, 4}, {1, 1}} }, // aligned polyline
    PointsVector {},    // split points
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
            { 2, test::Node {2, {1, 1}} },
            { 1, test::Node {1, {1, 4}} }
        },
        {
            { 1, test::Edge {1, 1, 2, Polyline2 { PointsVector {{1, 4}, {1, 1}} } } }
        }
    }
};

TEST_DATA(Test_8_1)
{
    "Swap endpoints and request split.",
    MockStorage {
        {
            test::Node {1, {1, 1}},
            test::Node {2, {1, 3}},
            test::Node {3, {1, 5}}
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{1, 1}, {1, 3}} } },
            test::Edge {2, 2, 3, Polyline2 { PointsVector {{1, 3}, {1, 5}} } }
        }
    },
    {1, EXISTS},     // edge id
    Polyline2 { {{1, 1}, {1, 3}, {4, 3}} }, // new polyline
    Polyline2 { {{1, 1}, {1, 3}, {4, 3}} }, // aligned polyline
    PointsVector { {1, 3} },    // split points
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
        { SourceEdgeID { 1, EXISTS }, EdgeIDVector {3, 1} }
    },
    MockStorageDiff {
        {
            { 4, test::Node {4, {4, 3}} }
        },
        {
            { 1, test::Edge {1, 2, 4, Polyline2 { PointsVector {{1, 3}, {4, 3}} } } },
            { 3, test::Edge {3, 1, 2, Polyline2 { PointsVector {{1, 1}, {1, 3}} } } }
        }
    }
};

// Tests with closed polylines

TEST_DATA(Test_9_1)
{
    "Create closed edge in empty dataset.",
    MockStorage {
        test::NodeDataVector {},
        test::EdgeDataVector {}
    },
    {1, NOT_EXISTS},     // edge id
    Polyline2 { {{1, 2}, {3, 1}, {2, 4}, {1, 2}} }, // new polyline
    Polyline2 { {{1, 2}, {3, 1}, {2, 4}, {1, 2}} }, // aligned polyline
    PointsVector {},    // split points
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
            { 1, test::Node {1, {1, 2}} }
        },
        {
            { 1, test::Edge {1, 1, 1, Polyline2 { {{1, 2}, {3, 1}, {2, 4}, {1, 2}} } } }
        }
    }
};

TEST_DATA(Test_9_2)
{
    "Create closed edge.",
    MockStorage {
        {
            test::Node {1, {1, 2}},
            test::Node {2, {1, 5}}
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{1, 2}, {1, 5}} } }
        }
    },
    {2, NOT_EXISTS},     // edge id
    Polyline2 { {{1, 2}, {3, 1}, {2, 4}, {1, 2}} }, // new polyline
    Polyline2 { {{1, 2}, {3, 1}, {2, 4}, {1, 2}} }, // aligned polyline
    PointsVector {},    // split points
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
            { 2, test::Edge {2, 1, 1, Polyline2 { {{1, 2}, {3, 1}, {2, 4}, {1, 2}} } } }
        }
    }
};

TEST_DATA(Test_9_3)
{
    "Edit closed edge.",
    MockStorage {
        {
            test::Node {1, {1, 2}}
        },
        {
            test::Edge {1, 1, 1, Polyline2 { {{1, 2}, {3, 1}, {2, 4}, {1, 2}} } }
        }
    },
    {1, EXISTS},     // edge id
    Polyline2 { {{1, 2}, {4, 3}, {2, 4}, {1, 2}} }, // new polyline
    Polyline2 { {{1, 2}, {4, 3}, {2, 4}, {1, 2}} }, // aligned polyline
    PointsVector {},    // split points
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
            { 1, test::Edge {1, 1, 1, Polyline2 { {{1, 2}, {4, 3}, {2, 4}, {1, 2}} } } }
        }
    }
};

TEST_DATA(Test_9_4)
{
    "Close unclosed edge.",
    MockStorage {
        {
            test::Node {1, {1, 2}},
            test::Node {2, {1, 5}}
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{1, 2}, {1, 5}} } }
        }
    },
    {1, EXISTS},     // edge id
    Polyline2 { {{1, 2}, {1, 5}, {2, 4}, {1, 2}} }, // new polyline
    Polyline2 { {{1, 2}, {1, 5}, {2, 4}, {1, 2}} }, // aligned
    PointsVector {},    // split points
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
            { 2, boost::none }
        },
        {
            { 1, test::Edge {1, 1, 1, Polyline2 { {{1, 2}, {1, 5}, {2, 4}, {1, 2}} } } }
        }
    }
};

TEST_DATA(Test_9_5)
{
    "Unclose closed edge.",
    MockStorage {
        {
            test::Node {1, {1, 2}}
        },
        {
            test::Edge {1, 1, 1, Polyline2 { {{1, 2}, {1, 5}, {2, 4}, {1, 2}} } }
        }
    },
    {1, EXISTS},     // edge id
    Polyline2 { PointsVector {{1, 2}, {1, 5}} }, // new polyline
    Polyline2 { PointsVector {{1, 2}, {1, 5}} }, // aligned polyline
    PointsVector {},    // split points
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
            { 2, test::Node {2, {1, 5}} }
        },
        {
            { 1, test::Edge {1, 1, 2, Polyline2 { PointsVector {{1, 2}, {1, 5}} } } }
        }
    }
};

TEST_DATA(Test_9_6)
{
    "Cut closed edge with split request.",
    MockStorage {
        {
            test::Node {1, {1, 2}}
        },
        {
            test::Edge {1, 1, 1, Polyline2 { {{1, 2}, {1, 5}, {2, 4}, {1, 2}} } }
        }
    },
    {1, EXISTS},     // edge id
    Polyline2 { {{1, 2}, {1, 5}, {2, 4}, {1, 2}} }, // new polyline
    Polyline2 { {{1, 2}, {1, 5}, {2, 4}, {1, 2}} }, // aligned polyline
    PointsVector { {1, 5} },    // split points
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
        { SourceEdgeID { 1, EXISTS }, EdgeIDVector {3, 1} }
    },
    MockStorageDiff {
        {
            { 2, test::Node {2, {1, 5}} }
        },
        {
            {1, test::Edge {1, 2, 1, Polyline2 { {{1, 5}, {2, 4}, {1, 2}} } } },
            {3, test::Edge {3, 1, 2, Polyline2 { PointsVector {{1, 2}, {1, 5}} } } }
        }
    }
};

TEST_DATA(Test_9_7)
{
    "Cut closed edge with new edge.",
    MockStorage {
        {
            test::Node {1, {1, 2}}
        },
        {
            test::Edge {1, 1, 1, Polyline2 { {{1, 2}, {1, 5}, {2, 4}, {1, 2}} } }
        }
    },
    {2, NOT_EXISTS},     // edge id
    Polyline2 { PointsVector {{0, 4}, {1, 4}} }, // new polyline
    Polyline2 { PointsVector {{0, 4}, {1, 4}} }, // aligned polyline
    PointsVector {},    // split points
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
        { SourceEdgeID { 1, EXISTS }, EdgeIDVector {3, 1} }
    },
    MockStorageDiff {
        {
            { 3, test::Node {3, {1, 4}} },
            { 4, test::Node {4, {0, 4}} }
        },
        {
            { 1, test::Edge {1, 3, 1, Polyline2 { {{1, 4}, {1, 5}, {2, 4}, {1, 2}} } } },
            { 2, test::Edge {2, 4, 3, Polyline2 { PointsVector {{0, 4}, {1, 4}} } } },
            { 3, test::Edge {3, 1, 3, Polyline2 { PointsVector {{1, 2}, {1, 4}} } } }
        }
    }
};

TEST_DATA(Test_9_8)
{
    "Create closed edge which is splitted at saving.",
    MockStorage {
        {
            test::Node {1, {0, 3}},
            test::Node {2, {1, 3}}
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{0, 3}, {1, 3}} } }
        }
    },
    {2, NOT_EXISTS},     // edge id
    Polyline2 { {{1, 2}, {1, 5}, {2, 4}, {1, 2}} }, // new polyline
    Polyline2 { {{1, 2}, {1, 5}, {2, 4}, {1, 2}} }, // aligned polyline
    PointsVector {},    // split points
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
        { SourceEdgeID { 2, NOT_EXISTS }, EdgeIDVector {2, 3} }
    },
    MockStorageDiff {
        {
            { 3, test::Node {3, {1, 2}} }
        },
        {
            { 2, test::Edge {2, 3, 2, Polyline2 { PointsVector {{1, 2}, {1, 3}} } } },
            { 3, test::Edge {3, 2, 3, Polyline2 { {{1, 3}, {1, 5}, {2, 4}, {1, 2}} } } }
        }
    }
};

TEST_DATA(Test_9_9)
{
    "Create closed edge with requested split points.",
    MockStorage {
        test::NodeDataVector {},
        test::EdgeDataVector {}
    },
    {1, NOT_EXISTS},     // edge id
    Polyline2 { {{1, 2}, {1, 5}, {2, 4}, {1, 2}} }, // new polyline
    Polyline2 { {{1, 2}, {1, 5}, {2, 4}, {1, 2}} }, // aligned polyline
    PointsVector { {1, 2}, {1, 5}, {2, 4} },    // split points
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
        { SourceEdgeID { 1, NOT_EXISTS }, EdgeIDVector {1, 2, 3} }
    },
    MockStorageDiff {
        {
            { 1, test::Node {1, {1, 2}} },
            { 2, test::Node {2, {1, 5}} },
            { 3, test::Node {3, {2, 4}} }
        },
        {
            { 1, test::Edge {1, 1, 2, Polyline2 { PointsVector {{1, 2}, {1, 5}} } } },
            { 2, test::Edge {2, 2, 3, Polyline2 { PointsVector {{1, 5}, {2, 4}} } } },
            { 3, test::Edge {3, 3, 1, Polyline2 { PointsVector {{2, 4}, {1, 2}} } } }
        }
    }
};

TEST_DATA(Test_9_10)
{
    "No unexpected intersection test.",
    MockStorage {
        {
            test::Node {1, {4146560.07159840, 7503079.19913133}}
        },
        {
            test::Edge {
                1, 1, 1,
                Polyline2 {
                    {
                        {4146560.07159840, 7503079.19913133},
                        {4146072.78554308, 7503095.91973127},
                        {4146082.34017161, 7502620.57696162},
                        {4146658.00654088, 7502634.90890442},
                        {4146560.07159840, 7503079.19913133}
                    }
                }
            }
        }
    },
    {2, NOT_EXISTS}, // edge id
    Polyline2 {      // new polyline
        PointsVector {
            {4146578.53176938, 7502995.45298979},
            {4146639.49483044, 7502718.88885912},
            {4146935.09076842, 7502845.11073221},
            {4146865.81971154, 7503072.03315992},
            {4146578.53176938, 7502995.45298979}
        }
    },
    Polyline2 {      // aligned polyline
        PointsVector {
            {4146578.53176938, 7502995.45298979},
            {4146639.49483044, 7502718.88885912},
            {4146935.09076842, 7502845.11073221},
            {4146865.81971154, 7503072.03315992},
            {4146578.53176938, 7502995.45298979}
        }
    },
    PointsVector {},  // split points
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
        { SourceEdgeID { 1, EXISTS }, EdgeIDVector {1, 3, 4} },
        { SourceEdgeID { 2, NOT_EXISTS }, EdgeIDVector {3, 2} }
    },
    MockStorageDiff {
        {
            { 2, test::Node {2, {4146578.53176938, 7502995.45298979}} },
            { 3, test::Node {3, {4146639.49483044, 7502718.88885912}} }
        },
        {
            { 1, test::Edge {
                    1, 1, 3,
                    Polyline2 {
                        PointsVector {
                            {4146560.07159840, 7503079.19913133},
                            {4146072.78554308, 7503095.91973127},
                            {4146082.34017161, 7502620.57696162},
                            {4146658.00654088, 7502634.90890442},
                            {4146639.49483044, 7502718.88885912}
                        }
                    }
                }
            },
            { 2, test::Edge {
                    2, 3, 2,
                    Polyline2 {
                        PointsVector {
                            {4146639.49483044, 7502718.88885912},
                            {4146935.09076842, 7502845.11073221},
                            {4146865.81971154, 7503072.03315992},
                            {4146578.53176938, 7502995.45298979}
                        }
                    }
                }
            },
            { 3, test::Edge {
                    3, 3, 2,
                    Polyline2 {
                        PointsVector {
                            {4146639.49483044, 7502718.88885912},
                            {4146578.53176938, 7502995.45298979}
                        }
                    }
                }
            },
            { 4, test::Edge {
                    4, 2, 1,
                    Polyline2 {
                        PointsVector {
                            {4146578.53176938, 7502995.45298979},
                            {4146560.07159840, 7503079.19913133}
                        }
                    }
                }
            }
        }
    }
};

TEST_DATA(Test_10_1)
{
    "Request split points at vertices.",
    MockStorage {
        {
            test::Node {1, {1, 2}}
        },
        {
            test::Edge {1, 1, 1, Polyline2 { {{1, 2}, {1, 5}, {2, 4}, {1, 2}} } }
        }
    },
    {1, EXISTS},     // edge id
    Polyline2 { {{1, 2}, {1, 5}, {2, 4}, {1, 2}} }, // new polyline
    Polyline2 { {{1, 2}, {1, 5}, {2, 4}, {1, 2}} }, // aligned polyline
    PointsVector { {1, 2}, {1, 5}, {2, 4} },    // split points
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
        { SourceEdgeID { 1, EXISTS }, EdgeIDVector {1, 2, 3} }
    },
    MockStorageDiff {
        {
            { 2, test::Node {2, {1, 5}} },
            { 3, test::Node {3, {2, 4}} }
        },
        {
            { 1, test::Edge {1, 1, 2, Polyline2 { PointsVector {{1, 2}, {1, 5}} } } },
            { 2, test::Edge {2, 2, 3, Polyline2 { PointsVector {{1, 5}, {2, 4}} } } },
            { 3, test::Edge {3, 3, 1, Polyline2 { PointsVector {{2, 4}, {1, 2}} } } }
        }
    }
};

TEST_DATA(Test_10_2)
{
    "Add new closed edge to node which already has one.",
    MockStorage {
        {
            test::Node {1, {1, 2}}
        },
        {
            test::Edge {1, 1, 1, Polyline2 { {{1, 2}, {1, 5}, {2, 4}, {1, 2}} } }
        }
    },
    {2, NOT_EXISTS},     // edge id
    Polyline2 { {{1, 2 - 6e-2}, {1, 0}, {2, 0}, {2, 2}, {1 + 1e-2, 2 - 1e-2}} }, // new polyline
    Polyline2 { {{1, 2}, {1, 0}, {2, 0}, {2, 2}, {1, 2}} }, // aligned polyline
    PointsVector {},    // split points
    TopologyRestrictions {
        5e-2, // tolerance
        1e-1, // junction gravity
        5e-2, // vertex gravity
        1e-1, // group junction gravity
        5e-2, // group junction snap to vertex
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
            { 2, test::Edge {2, 1, 1, Polyline2 { {{1, 2}, {1, 0}, {2, 0}, {2, 2}, {1, 2}} } } }
        }
    }
};

TEST_DATA(Test_10_3)
{
    "Add new closed edge to node which already has one with overlapping.",
    MockStorage {
        {
            test::Node {1, {1, 2}}
        },
        {
            test::Edge {1, 1, 1, Polyline2 { {{1, 2}, {1, 5}, {4, 2}, {1, 2}} } }
        }
    },
    {2, NOT_EXISTS},     // edge id
    Polyline2 { {{1, 2 - 6e-2}, {1, 4}, {2, 2}, {1 + 1e-2, 2 - 1e-2}} }, // new polyline
    Polyline2 { {{1, 2}, {1, 4}, {2, 2}, {1, 2}} }, // aligned polyline
    PointsVector {},    // split points
    TopologyRestrictions {
        5e-2, // tolerance
        1e-1, // junction gravity
        5e-2, // vertex gravity
        1e-1, // group junction gravity
        5e-2, // group junction snap to vertex
        Limits<double> { 1e-1 }, // segment
        Limits<double> { 1e-1 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Allowed
    },
    SplitEdges {
        { SourceEdgeID { 1, EXISTS }, EdgeIDVector {4, 1, 3} },
        { SourceEdgeID { 2, NOT_EXISTS }, EdgeIDVector {4, 2, 3} }
    },
    MockStorageDiff {
        {
            { 2, test::Node {2, {1, 4}} },
            { 3, test::Node {3, {2, 2}} }
        },
        {
            { 1, test::Edge {1, 2, 3, Polyline2 { PointsVector {{1, 4}, {1, 5}, {4, 2}, {2, 2}} } } },
            { 2, test::Edge {2, 2, 3, Polyline2 { PointsVector {{1, 4}, {2, 2}} } } },
            { 3, test::Edge {3, 3, 1, Polyline2 { PointsVector {{2, 2}, {1, 2}} } } },
            { 4, test::Edge {4, 1, 2, Polyline2 { PointsVector {{1, 2}, {1, 4}} } } }
        }
    }
};

TEST_DATA(Test_10_4)
{
    "Move loop polyline.",
    MockStorage {
        {
            test::Node {1, {1, 2}}
        },
        {
            test::Edge {1, 1, 1, Polyline2 { PointsVector {{1, 2}, {1, 5}, {3, 2}, {1, 2}} } }
        }
    },
    {1, EXISTS},     // edge id
    Polyline2 { PointsVector {{2, 1}, {2, 4}, {4, 1}, {2, 1}} }, // new polyline
    Polyline2 { PointsVector {{2, 1}, {2, 4}, {4, 1}, {2, 1}} }, // aligned polyline
    PointsVector {},    // split points
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
            { 1, test::Node {1, {2, 1}} }
        },
        {
            { 1, test::Edge {1, 1, 1, Polyline2 { PointsVector {{2, 1}, {2, 4}, {4, 1}, {2, 1}} } } }
        }
    }
};

TEST_SUITE_END(editing_edges_tests)

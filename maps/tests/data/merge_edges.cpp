#include "../test_types/common.h"
#include "../suite.h"

#include "../../events_data.h"
#include "../../test_types/merge_edges_test_data.h"

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

TEST_SUITE_START(merge_edges_tests, MergeEdgesTestData)

TEST_DATA(test_1)
{
    "Merging edges of one direction, leave end node",
    test::MockStorage {
        {   test::Node {1, {1, 1}},
            test::Node {2, {2, 2}},
            test::Node {3, {3, 3}}
        },
        {   test::Edge { 1, 1, 2, Polyline2 { PointsVector {{1, 1}, {2, 2}} } },
            test::Edge { 2, 2, 3, Polyline2 { PointsVector {{2, 2}, {3, 3}} } }
        }
    },
    2, // common node id
    TopologyRestrictions {
        1e-2, // tolerance
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
    MockStorageDiff {
        {
            { 2, boost::none }
        },
        {
            { 1, boost::none },
            { 2, test::Edge {2, 1, 3, Polyline2 { {{1, 1}, {2, 2}, {3, 3}} } } }
        }
    }
};

TEST_DATA(test_2)
{
    "Merging edges of one direction, leave start node",
    test::MockStorage {
        {   test::Node {1, {1, 1}},
            test::Node {2, {2, 2}},
            test::Node {3, {3, 3}}
        },
        {   test::Edge { 1, 2, 1, Polyline2 { PointsVector {{2, 2}, {1, 1}} } },
            test::Edge { 2, 3, 2, Polyline2 { PointsVector {{3, 3}, {2, 2}} } }
        }
    },
    2, // common node id
    TopologyRestrictions {
        1e-2, // tolerance
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
    MockStorageDiff {
        {
            { 2, boost::none }
        },
        {
            { 1, boost::none },
            { 2, test::Edge {2, 3, 1, Polyline2 { {{3, 3}, {2, 2}, {1, 1}} } } }
        }
    }
};

TEST_DATA(test_3)
{
    "Merging edges of different directions, common start, leave end node",
    test::MockStorage {
        {   test::Node {1, {1, 1}},
            test::Node {2, {2, 2}},
            test::Node {3, {3, 3}}
        },
        {   test::Edge { 1, 2, 1, Polyline2 { PointsVector {{2, 2}, {1, 1}} } },
            test::Edge { 2, 2, 3, Polyline2 { PointsVector {{2, 2}, {3, 3}} } }
        }
    },
    2, // common node id
    TopologyRestrictions {
        1e-2, // tolerance
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
    MockStorageDiff {
        {
            { 2, boost::none }
        },
        {
            { 1, boost::none },
            { 2, test::Edge {2, 1, 3, Polyline2 { {{1, 1}, {2, 2}, {3, 3}} } } }
        }
    }
};

TEST_DATA(test_4)
{
    "Merging edges of different directions, common end, leave start node",
    test::MockStorage {
        {   test::Node {1, {1, 1}},
            test::Node {2, {2, 2}},
            test::Node {3, {3, 3}}
        },
        {   test::Edge { 1, 1, 2, Polyline2 { PointsVector {{1, 1}, {2, 2}} } },
            test::Edge { 2, 3, 2, Polyline2 { PointsVector {{3, 3}, {2, 2}} } }
        }
    },
    NodeID {2}, // common node id
    TopologyRestrictions {
        1e-2, // tolerance
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
    MockStorageDiff {
        {
            { 2, boost::none }
        },
        {
            { 1, boost::none },
            { 2, test::Edge {2, 3, 1, Polyline2 { {{3, 3}, {2, 2}, {1, 1}} } } }
        }
    }
};

TEST_DATA(test_5)
{
    "Check other links are not damaged",
    test::MockStorage {
        {   test::Node {1, {2, 2}},
            test::Node {2, {4, 3}},
            test::Node {3, {6, 3}},
            test::Node {4, {0, 2}},
            test::Node {5, {1, 3}},
            test::Node {6, {7, 4}},
            test::Node {7, {7, 3}}
        },
        {   test::Edge { 1, 1, 2, Polyline2 { PointsVector {{2, 2}, {4, 3}} } },
            test::Edge { 2, 2, 3, Polyline2 { PointsVector {{4, 3}, {6, 3}} } },
            test::Edge { 3, 1, 3, Polyline2 { PointsVector {{2, 2}, {6, 3}} } },
            test::Edge { 4, 1, 4, Polyline2 { PointsVector {{2, 2}, {0, 2}} } },
            test::Edge { 5, 1, 5, Polyline2 { PointsVector {{2, 2}, {1, 3}} } },
            test::Edge { 6, 3, 6, Polyline2 { PointsVector {{6, 3}, {7, 4}} } },
            test::Edge { 7, 3, 7, Polyline2 { PointsVector {{6, 3}, {7, 3}} } }
        }
    },
    NodeID {2}, // common node id
    TopologyRestrictions {
        1e-2, // tolerance
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
    MockStorageDiff {
        {
            { 2, boost::none }
        },
        {
            { 1, boost::none },
            { 2, test::Edge {2, 1, 3, Polyline2 { {{2, 2}, {4, 3}, {6, 3}} } } }
        }
    }
};

TEST_DATA(test_6) {
    "Check for more complex polylines",
    test::MockStorage {
        {   test::Node {1, {1, 1}},
            test::Node {2, {2, 3}},
            test::Node {3, {6, 4}},
            test::Node {4, {4, 1}}
        },
        {   test::Edge { 1, 1, 2, Polyline2 { {{1, 1}, {1, 4}, {2, 3}} } },
            test::Edge { 2, 2, 3, Polyline2 { {{2, 3}, {4, 3}, {6, 4}} } },
            test::Edge { 3, 3, 4, Polyline2 { PointsVector {{6, 4}, {4, 1}} } },
            test::Edge { 4, 1, 4, Polyline2 { PointsVector {{1, 1}, {4, 1}} } }
        }
    },
    NodeID {2}, // common node id
    TopologyRestrictions {
        1e-2, // tolerance
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
    MockStorageDiff {
        {
            { 2, boost::none }
        },
        {
            { 1, boost::none },
            { 2, test::Edge {2, 1, 3, Polyline2 { {{1, 1}, {1, 4}, {2, 3}, {4, 3}, {6, 4}} } } }
        }
    }
};

TEST_DATA(test_7)
{
    "Erroneous",
    test::MockStorage {
        {   test::Node {1, {1, 1}},
            test::Node {2, {2, 1}},
            test::Node {3, {3, 1}},
            test::Node {4, {2, 2}}
        },
        {   test::Edge { 1, 1, 2, Polyline2 { PointsVector {{1, 1}, {2, 1}} } },
            test::Edge { 2, 2, 3, Polyline2 { PointsVector {{2, 1}, {3, 1}} } },
            test::Edge { 3, 2, 4, Polyline2 { PointsVector {{2, 1}, {2, 2}} } }
        }
    },
    NodeID {2}, // common node id
    TopologyRestrictions {
        1e-2, // tolerance
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
    MockStorageDiff {
        {},
        {}
    },
    ErrorCode::NodeDeletionForbidden
};

TEST_DATA(test_8_1)
{
    "Check merging to form a closed edge",
    test::MockStorage {
        {   test::Node {1, {1, 1}},
            test::Node {2, {1, 3}}
        },
        {   test::Edge { 1, 1, 2, Polyline2 { PointsVector {{1, 1}, {1, 3}} } },
            test::Edge { 2, 2, 1, Polyline2 { {{1, 3}, {3, 3}, {3, 1}, {1, 1}} } }
        }
    },
    NodeID {2}, // common node id
    TopologyRestrictions {
        1e-2, // tolerance
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
    MockStorageDiff {
        {
            { 2, boost::none }
        },
        {
            { 1, boost::none },
            { 2, test::Edge {2, 1, 1, Polyline2 { {{1, 1}, {1, 3}, {3, 3}, {3, 1}, {1, 1}} } } }
        }
    }
};

/*
TEST_DATA(test_8_2)
{
    "Check error when merging to form a closed edge with no closed edges allowed",
    test::MockStorage {
        {   test::Node {1, {1, 1}},
            test::Node {2, {1, 3}}
        },
        {   test::Edge { 1, 1, 2, Polyline2 { PointsVector {{1, 1}, {1, 3}} } },
            test::Edge { 2, 2, 1, Polyline2 { {{1, 3}, {3, 3}, {3, 1}, {1, 1}} } }
        }
    },
    NodeID {2}, // common node id
    TopologyRestrictions {
        1e-2, // tolerance
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
    MockStorageDiff {
        {},
        {}
    },
    ErrorCode::SelfIntersection
};
*/

TEST_SUITE_END(merge_edges_tests)

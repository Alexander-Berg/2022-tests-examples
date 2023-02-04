#include "../../test_types/common.h"
#include "../suite.h"

#include "../../events_data.h"
#include "../../test_types/move_node_test_data.h"

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

TEST_SUITE_START(move_node_tests, MoveNodeTestData)

TEST_DATA(test_1)
{
    "",
    MockStorage {
        {
            test::Node {1, {1, 1}},
            test::Node {2, {2, 4}}
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{1, 1}, {2, 4}} }}
        }
    },
    MoveNodeTestData::Type::Correct,
    2,  // node id
    Point2 {4, 1},  // new pos
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
    boost::none, // mergedNodeId
    SplitEdges {},
    MockStorageDiff {
        {
            { 2, test::Node {2, {4, 1}} }
        },
        {
            { 1, test::Edge {1, 1, 2, Polyline2 { PointsVector {{1, 1}, {4, 1}} }} }
        }
    }
};

TEST_DATA(test_2)
{
    "",
    MockStorage {
        {
            test::Node {1, {1, 1}},
            test::Node {2, {2, 4}},
            test::Node {3, {4, 2}},
            test::Node {4, {0, 0}}
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{1, 1}, {2, 4}} }},
            test::Edge {2, 1, 3, Polyline2 { PointsVector {{1, 1}, {4, 2}} }},
            test::Edge {3, 1, 4, Polyline2 { PointsVector {{1, 1}, {0, 0}} }}
        }
    },
    MoveNodeTestData::Type::Correct,
    1,  // node id
    Point2 {4, 4},  // new pos
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
    boost::none, // mergedNodeId
    SplitEdges {},
    MockStorageDiff {
        {
            { 1, test::Node {1, {4, 4}} }
        },
        {
            { 1, test::Edge {1, 1, 2, Polyline2 { PointsVector {{4, 4}, {2, 4}} } } },
            { 2, test::Edge {2, 1, 3, Polyline2 { PointsVector {{4, 4}, {4, 2}} } } },
            { 3, test::Edge {3, 1, 4, Polyline2 { PointsVector {{4, 4}, {0, 0}} } } }
        }
    }
};

TEST_DATA(test_3)
{
    "",
    MockStorage {
        {
            test::Node {1, {1, 4}},
            test::Node {2, {1, 1}},
            test::Node {3, {3, 2}},
            test::Node {4, {4, 2}},
            test::Node {5, {6, 4}},
            test::Node {6, {6, 1}}
        },
        {
            test::Edge {1, 1, 3, Polyline2 { PointsVector {{1, 4}, {3, 2}} }},
            test::Edge {2, 2, 3, Polyline2 { PointsVector {{1, 1}, {3, 2}} }},
            test::Edge {3, 4, 5, Polyline2 { PointsVector {{4, 2}, {6, 4}} }},
            test::Edge {4, 4, 6, Polyline2 { PointsVector {{4, 2}, {6, 1}} }}
        }
    },
    MoveNodeTestData::Type::Correct,
    3,  // node id
    Point2 {4 + 4e-2, 2},  // new pos
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
    4, // mergedNodeId
    SplitEdges {},
    MockStorageDiff {
        {
            { 3, test::Node {3, {4, 2}} },
            { 4, boost::none }
        },
        {
            { 1, test::Edge {1, 1, 3, Polyline2 { PointsVector {{1, 4}, {4, 2}} } } },
            { 2, test::Edge {2, 2, 3, Polyline2 { PointsVector {{1, 1}, {4, 2}} } } },
            { 3, test::Edge {3, 3, 5, Polyline2 { PointsVector {{4, 2}, {6, 4}} } } },
            { 4, test::Edge {4, 3, 6, Polyline2 { PointsVector {{4, 2}, {6, 1}} } } }
        }
    }
};

TEST_DATA(test_with_intersections)
{
    "",
    MockStorage {
        {
            test::Node {1, {1, 3}},
            test::Node {2, {6, 3}},
            test::Node {3, {1, 1}},
            test::Node {4, {6, 1}},
            test::Node {5, {3, 1}}
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{1, 3}, {6, 3}} }},
            test::Edge {2, 1, 3, Polyline2 { PointsVector {{1, 3}, {1, 1}} }},
            test::Edge {3, 2, 4, Polyline2 { PointsVector {{6, 3}, {6, 1}} }},
            test::Edge {4, 3, 5, Polyline2 { PointsVector {{1, 1}, {3, 1}} }},
            test::Edge {5, 5, 4, Polyline2 { PointsVector {{3, 1}, {6, 1}} }}
        }
    },
    MoveNodeTestData::Type::WithoutCheck,
    5,  // node id
    Point2 {3.5, 3.5},  // new pos
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
    boost::none, // mergedNodeId
    SplitEdges {
        { {1, EXISTS}, EdgeIDVector {1, 6, 7} },
        { {4, EXISTS}, EdgeIDVector {4, 8} },
        { {5, EXISTS}, EdgeIDVector {9, 5} }
    },
    MockStorageDiff {
        {
            { 5, test::Node {5, {3.5, 3.5}} },
            { 6, test::Node {6, {3, 3}} },
            { 7, test::Node {7, {4, 3}} }
        },
        {
            { 1, test::Edge {1, 1, 6, Polyline2 { PointsVector {{1, 3}, {3, 3}} } } },
            { 4, test::Edge {4, 3, 6, Polyline2 { PointsVector {{1, 1}, {3, 3}} } } },
            { 5, test::Edge {5, 7, 4, Polyline2 { PointsVector {{4, 3}, {6, 1}} } } },
            { 6, test::Edge {6, 6, 7, Polyline2 { PointsVector {{3, 3}, {4, 3}} } } },
            { 7, test::Edge {7, 7, 2, Polyline2 { PointsVector {{4, 3}, {6, 3}} } } },
            { 8, test::Edge {8, 6, 5, Polyline2 { PointsVector {{3, 3}, {3.5, 3.5}} } } },
            { 9, test::Edge {9, 5, 7, Polyline2 { PointsVector {{3.5, 3.5}, {4, 3}} } } }
        }
    }
};

TEST_DATA(test_with_intersections_and_merge)
{
    "",
    MockStorage {
        {
            test::Node {1, {1, 3}},
            test::Node {2, {6, 3}},
            test::Node {3, {1, 1}},
            test::Node {4, {6, 1}},
            test::Node {5, {3, 1}},
            test::Node {6, {8, 3}},
            test::Node {7, {4, 6}},
            test::Node {8, {7, 5}}
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{1, 3}, {6, 3}} }},
            test::Edge {2, 1, 3, Polyline2 { PointsVector {{1, 3}, {1, 1}} }},
            test::Edge {3, 2, 4, Polyline2 { PointsVector {{6, 3}, {6, 1}} }},
            test::Edge {4, 3, 5, Polyline2 { PointsVector {{1, 1}, {3, 1}} }},
            test::Edge {5, 5, 4, Polyline2 { PointsVector {{3, 1}, {6, 1}} }},
            test::Edge {6, 7, 8, Polyline2 { PointsVector {{4, 6}, {7, 5}} }},
            test::Edge {7, 2, 6, Polyline2 { PointsVector {{6, 3}, {8, 3}} }}
        }
    },
    MoveNodeTestData::Type::WithoutCheck,
    5,  // node id
    Point2 {7, 5 + 4e-2},  // new pos
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
    8, // mergedNodeId
    SplitEdges {
        { {1, EXISTS}, EdgeIDVector {1, 9} },
        { {7, EXISTS}, EdgeIDVector {8, 7} }
    },
    MockStorageDiff {
        {
            { 5, test::Node {5, {7, 5}} },
            { 8, boost::none },
            { 9, test::Node {9, {4, 3}} },
            { 10, test::Node {10, {6.5, 3}} }
        },
        {
            { 1, test::Edge {1, 1, 9, Polyline2 { PointsVector {{1, 3}, {4, 3}} } } },
            { 4, test::Edge {4, 3, 9, Polyline2 { PointsVector {{1, 1}, {4, 3}} } } },
            { 5, test::Edge {5, 5, 10, Polyline2 { PointsVector {{7, 5}, {6.5, 3}} } } },
            { 6, test::Edge {6, 7, 5, Polyline2 { PointsVector {{4, 6}, {7, 5}} } } },
            { 7, test::Edge {7, 10, 6, Polyline2 { PointsVector {{6.5, 3}, {8, 3}} } } },
            { 8, test::Edge {8, 2, 10, Polyline2 { PointsVector {{6, 3}, {6.5, 3}} } } },
            { 9, test::Edge {9, 9, 2, Polyline2 { PointsVector {{4, 3}, {6, 3}} } } },
            { 10, test::Edge {10, 9, 5, Polyline2 { PointsVector {{4, 3}, {7, 5}} } } },
            { 11, test::Edge {11, 10, 4, Polyline2 { PointsVector {{6.5, 3}, {6, 1}} } } }
        }
    }
};

TEST_DATA(test_with_intersections_forbidden)
{
    "",
    MockStorage {
        {
            test::Node {1, {1, 3}},
            test::Node {2, {6, 3}},
            test::Node {3, {1, 1}},
            test::Node {4, {6, 1}},
            test::Node {5, {3, 1}},
            test::Node {6, {8, 3}},
            test::Node {7, {4, 6}},
            test::Node {8, {7, 5}}
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{1, 3}, {6, 3}} }},
            test::Edge {2, 1, 3, Polyline2 { PointsVector {{1, 3}, {1, 1}} }},
            test::Edge {3, 2, 4, Polyline2 { PointsVector {{6, 3}, {6, 1}} }},
            test::Edge {4, 3, 5, Polyline2 { PointsVector {{1, 1}, {3, 1}} }},
            test::Edge {5, 5, 4, Polyline2 { PointsVector {{3, 1}, {6, 1}} }},
            test::Edge {6, 7, 8, Polyline2 { PointsVector {{4, 6}, {7, 5}} }},
            test::Edge {7, 2, 6, Polyline2 { PointsVector {{6, 3}, {8, 3}} }}
        }
    },
    MoveNodeTestData::Type::Incorrect,
    5,  // node id
    Point2 {7, 5 + 4e-2},  // new pos
    TopologyRestrictions {
        1e-2, // tolerance
        1e-1, // junction gravity
        5e-2, // vertex gravity
        1e-1, // group junction gravity
        5e-2, // group junction snap to vertex
        Limits<double> { 1e-1 }, // segment
        Limits<double> { 1e-1 }, // edge
        NO_INTERSECTIONS_WITH_EDGE,
        NO_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    8, // mergedNodeId
    SplitEdges {},
    MockStorageDiff {
        {},
        {}
    },
    boost::none, // PrintInfo
    ErrorCode::TooManyIntersectionsWithElement
};

TEST_DATA(test_snap_to_edge)
{
    "",
    MockStorage {
        {
            test::Node {1, {1, 1}},
            test::Node {2, {3, 3}},
            test::Node {3, {5, 1}},
            test::Node {4, {1, 4}},
            test::Node {5, {6, 4}}
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{1, 1}, {3, 3}} }},
            test::Edge {2, 2, 3, Polyline2 { PointsVector {{3, 3}, {5, 1}} }},
            test::Edge {3, 4, 5, Polyline2 { PointsVector {{1, 4}, {6, 4}} }}
        }
    },
    MoveNodeTestData::Type::Correct,
    2,  // node id
    Point2 {3, 4 - 4e-2},  // new pos
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
    boost::none, // mergedNodeId
    SplitEdges {
        { SourceEdgeID { 3, EXISTS }, EdgeIDVector {4, 3} }
    },
    MockStorageDiff {
        {
            { 2, test::Node {2, {3, 4}} }
        },
        {
            { 1, test::Edge {1, 1, 2, Polyline2 { PointsVector {{1, 1}, {3, 4}} } } },
            { 2, test::Edge {2, 2, 3, Polyline2 { PointsVector {{3, 4}, {5, 1}} } } },
            { 3, test::Edge {3, 2, 5, Polyline2 { PointsVector {{3, 4}, {6, 4}} } } },
            { 4, test::Edge {4, 4, 2, Polyline2 { PointsVector {{1, 4}, {3, 4}} } } }
        }
    }
};

TEST_DATA(test_snap_to_edge_intersections_forbidden)
{
    "",
    MockStorage {
        {
            test::Node {1, {1, 1}},
            test::Node {2, {3, 3}},
            test::Node {3, {5, 1}},
            test::Node {4, {1, 4}},
            test::Node {5, {6, 4}}
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{1, 1}, {3, 3}} }},
            test::Edge {2, 2, 3, Polyline2 { PointsVector {{3, 3}, {5, 1}} }},
            test::Edge {3, 4, 5, Polyline2 { PointsVector {{1, 4}, {6, 4}} }}
        }
    },
    MoveNodeTestData::Type::Incorrect,
    2,  // node id
    Point2 {3, 4 - 4e-2},  // new pos
    TopologyRestrictions {
        1e-2, // tolerance
        1e-1, // junction gravity
        5e-2, // vertex gravity
        1e-1, // group junction gravity
        5e-2, // group junction snap to vertex
        Limits<double> { 1e-1 }, // segment
        Limits<double> { 1e-1 }, // edge
        NO_INTERSECTIONS_WITH_EDGE,
        NO_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    boost::none, // mergedNodeId
    SplitEdges {},
    MockStorageDiff {
        {},
        {}
    },
    boost::none, // PrintInfo
    ErrorCode::TooManyIntersectionsWithNetwork
};

TEST_DATA(test_snap_to_incident_edge_old_geom_no_error)
{
    "",
    MockStorage {
        {
            test::Node {1, {1, 1}},
            test::Node {2, {3, 1}},
            test::Node {3, {5, 1}}
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{1, 1}, {3, 1}} }},
            test::Edge {2, 2, 3, Polyline2 { PointsVector {{3, 1}, {5, 1}} }}
        }
    },
    MoveNodeTestData::Type::Correct,
    2,  // node id
    Point2 {4, 1},  // new pos
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
    boost::none, // mergedNodeId
    SplitEdges {},
    MockStorageDiff {
        {
            { 2, test::Node {2, {4, 1}} }
        },
        {
            { 1, test::Edge {1, 1, 2, Polyline2 { PointsVector {{1, 1}, {4, 1}} } } },
            { 2, test::Edge {2, 2, 3, Polyline2 { PointsVector {{4, 1}, {5, 1}} } } }
        }
    }
};

TEST_DATA(test_snap_to_incident_edge_new_geom_error)
{
    "",
    MockStorage {
        {
            test::Node {1, {1, 3}},
            test::Node {2, {3, 1}}
        },
        {
            test::Edge {1, 1, 2, Polyline2 { {{1, 3}, {3, 3}, {5, 2}, {3, 1}} }}
        }
    },
    MoveNodeTestData::Type::Incorrect,
    2,  // node id
    Point2 {2, 3},  // new pos
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
    boost::none, // mergedNodeId
    SplitEdges {},
    MockStorageDiff {
        {},
        {}
    },
    boost::none, // PrintInfo
    ErrorCode::SelfIntersection
};

TEST_DATA(test_move_within_gravity_to_old_pos)
{
    "",
    MockStorage {
        {
            test::Node {1, {1, 3}},
            test::Node {2, {3, 1}}
        },
        {
            test::Edge {1, 1, 2, Polyline2 { {{1, 3}, {3, 3}, {5, 2}, {3, 1}} }}
        }
    },
    MoveNodeTestData::Type::Correct,
    2,  // node id
    Point2 {3, 1 - 4e-2},  // new pos
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
    boost::none, // mergedNodeId
    SplitEdges {},
    MockStorageDiff {
        {
            { 2, test::Node {2, {3, 1 - 4e-2}} }
        },
        {
            { 1,  test::Edge {1, 1, 2, Polyline2 { {{1, 3}, {3, 3}, {5, 2}, {3, 1 - 4e-2}} } } }
        }
    }
};

TEST_DATA(test_move_closed_edge)
{
    "",
    MockStorage {
        {
            test::Node {1, {1, 3}},
            test::Node {2, {3, 4}},
            test::Node {3, {1, 4}}
        },
        {
            test::Edge {1, 1, 1, Polyline2 { {{1, 3}, {1, 1}, {3, 3}, {1, 3}} }},
            test::Edge {2, 1, 2, Polyline2 { PointsVector {{1, 3}, {3, 4}} }},
            test::Edge {3, 1, 3, Polyline2 { PointsVector {{1, 3}, {1, 4}} }},
            test::Edge {4, 2, 3, Polyline2 { PointsVector {{3, 4}, {1, 4}} }}
        }
    },
    MoveNodeTestData::Type::Correct,
    1,  // node id
    Point2 {0, 3},  // new pos
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
    boost::none, // mergedNodeId
    SplitEdges {},
    MockStorageDiff {
        {
            { 1, test::Node {1, {0, 3}} }
        },
        {
            { 1, test::Edge {1, 1, 1, Polyline2 { {{0, 3}, {1, 1}, {3, 3}, {0, 3}} } } },
            { 2, test::Edge {2, 1, 2, Polyline2 { PointsVector {{0, 3}, {3, 4}} } } },
            { 3, test::Edge {3, 1, 3, Polyline2 { PointsVector {{0, 3}, {1, 4}} } } }
        }
    }
};

TEST_SUITE_END(move_node_tests)

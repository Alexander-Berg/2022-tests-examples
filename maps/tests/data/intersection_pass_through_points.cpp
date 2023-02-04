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

TEST_SUITE_START(intersection_pass_through_points, SaveEdgeTestData)

TEST_DATA(Test_1_1)
{
    "Pass through existing node, exact match.",
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
    SourceEdgeID { 200, NOT_EXISTS },
    Polyline2 { PointsVector {{13, 4}, {13, 7}}}, // new polyline
    Polyline2 { PointsVector {{13, 4}, {13, 7}}}, // aligned polyline
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
            { 100, test::Node { 100, {13, 4} } },
            { 105, test::Node { 105, {13, 7} } }
        },
        {
            { 200, test::Edge {200, 100, 6, Polyline2 { PointsVector {{13, 4}, {13, 6}} } } },
            { 205, test::Edge {205, 6, 105, Polyline2 { PointsVector {{13, 6}, {13, 7}} } } }
        }
    },
    PrintInfo {
        PointsVector { {13, 6} },
        std::vector<PointGravity> {
            {{13, 4}, GravityType::JunctionGravity},
            {{13, 7}, GravityType::JunctionGravity}
        },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> { GravityType::JunctionGravity } // gravity of created/edited edge
    }
};

TEST_DATA(Test_1_2)
{
    "Pass through existing node, within node, without intersection.",
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
    SourceEdgeID { 200, NOT_EXISTS },
    Polyline2 { PointsVector {{13 + (5e-2 - 5e-3), 4}, {13 + (5e-2 - 5e-3), 7}}}, // new polyline
    Polyline2 { PointsVector {{13 + (5e-2 - 5e-3), 4}, {13, 6}, {13 + (5e-2 - 5e-3), 7}}}, // new polyline
    PointsVector {}, // splits requested
    TopologyRestrictions {
        5e-2, // tolerance
        5e-2, // junction gravity
        5e-2, // vertex gravity
        5e-2, // group junction gravity
        5e-2, // group junction snap to vertex
        Limits<double> { 5e-2 }, // segment
        Limits<double> { 5e-2 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {
        { SourceEdgeID { 200, NOT_EXISTS }, EdgeIDVector {200, 205} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {13 + (5e-2 - 5e-3), 4} } },
            { 105, test::Node { 105, {13 + (5e-2 - 5e-3), 7} } }
        },
        {
            { 200, test::Edge {200, 100, 6, Polyline2 { PointsVector {{13 + (5e-2 - 5e-3), 4}, {13, 6}} } } },
            { 205, test::Edge {205, 6, 105, Polyline2 { PointsVector {{13, 6}, {13 + (5e-2 - 5e-3), 7}} } } }
        }
    },
    PrintInfo {
        PointsVector { {13, 6} },
        std::vector<PointGravity> {},
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> { GravityType::JunctionGravity } // gravity of created/edited edge
    }
};

TEST_DATA(Test_1_3)
{
    "Pass through existing node, within node, with intersection.",
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
    SourceEdgeID { 200, NOT_EXISTS },
    Polyline2 { PointsVector {{13 - (5e-2 - 5e-3), 4}, {13 - (5e-2 - 5e-3), 7}}}, // new polyline
    Polyline2 { PointsVector {{13 - (5e-2 - 5e-3), 4}, {13, 6}, {13 - (5e-2 - 5e-3), 7}}}, // aligned polyline
    PointsVector {}, // splits requested
    TopologyRestrictions {
        5e-2, // tolerance
        5e-2, // junction gravity
        5e-2, // vertex gravity
        5e-2, // group junction gravity
        5e-2, // group junction snap to vertex
        Limits<double> { 5e-2 }, // segment
        Limits<double> { 5e-2 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {
        { SourceEdgeID { 200, NOT_EXISTS }, EdgeIDVector {200, 205} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {13 - (5e-2 - 5e-3), 4} } },
            { 105, test::Node { 105, {13 - (5e-2 - 5e-3), 7} } }
        },
        {
            { 200, test::Edge {200, 100, 6, Polyline2 { PointsVector {{13 - (5e-2 - 5e-3), 4}, {13, 6}} } } },
            { 205, test::Edge {205, 6, 105, Polyline2 { PointsVector {{13, 6}, {13 - (5e-2 - 5e-3), 7}} } } }
        }
    },
    PrintInfo {
        PointsVector { {13, 6} },
        std::vector<PointGravity> {},
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> { GravityType::JunctionGravity } // gravity of created/edited edge
    }
};

TEST_DATA(Test_2_1)
{
    "Pass through existing node, several edges, without intersections.",
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
    Polyline2 { PointsVector {{5 + (1e-3 - 1e-4), 2}, {5 + (1e-3 - 1e-4), 5}}}, // new polyline
    Polyline2 { PointsVector {{5 + (1e-3 - 1e-4), 2}, {5, 4}, {5 + (1e-3 - 1e-4), 5}}}, // aligned polyline
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
            { 100, test::Node { 100, {5 + (1e-3 - 1e-4), 2} } },
            { 105, test::Node { 105, {5 + (1e-3 - 1e-4), 5} } }
        },
        {
            { 200, test::Edge {200, 100, 2, Polyline2 { PointsVector {{5 + (1e-3 - 1e-4), 2}, {5, 4}} } } },
            { 205, test::Edge {205, 2, 105, Polyline2 { PointsVector {{5, 4}, {5 + (1e-3 - 1e-4), 5}} } } }
        }
    },
    PrintInfo {
        PointsVector { {5, 4} },
        std::vector<PointGravity> {},
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> { GravityType::Tolerance } // gravity of created/edited edge
    }
};

TEST_DATA(Test_2_2)
{
    "Pass through existing node, several edges, exact match.",
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
    Polyline2 { PointsVector {{5, 2}, {5, 5}}}, // new polyline
    Polyline2 { PointsVector {{5, 2}, {5, 4}, {5, 5}}}, // aligned polyline
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
            { 100, test::Node { 100, {5, 2} } },
            { 105, test::Node { 105, {5, 5} } }
        },
        {
            { 200, test::Edge {200, 100, 2, Polyline2 { PointsVector {{5, 2}, {5, 4}} } } },
            { 205, test::Edge {205, 2, 105, Polyline2 { PointsVector {{5, 4}, {5, 5}} } } }
        }
    },
    PrintInfo {
        PointsVector { {5, 4} },
        std::vector<PointGravity> {},
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> { GravityType::Tolerance } // gravity of created/edited edge
    }
};

TEST_DATA(Test_2_4)
{
    "Pass through existing node, several edges, with intersection.",
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
    Polyline2 { PointsVector {{5 - (1e-3 - 1e-4), 2}, {5 - (1e-3 - 1e-4), 5}}}, // new polyline
    Polyline2 { PointsVector {{5 - (1e-3 - 1e-4), 2}, {5, 4}, {5 - (1e-3 - 1e-4), 5}}}, // aligned polyline
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
            { 100, test::Node { 100, {5 - (1e-3 - 1e-4), 2} } },
            { 105, test::Node { 105, {5 - (1e-3 - 1e-4), 5} } }
        },
        {
            { 200, test::Edge {200, 100, 2, Polyline2 { PointsVector {{5 - (1e-3 - 1e-4), 2}, {5, 4}} } } },
            { 205, test::Edge {205, 2, 105, Polyline2 { PointsVector {{5, 4}, {5 - (1e-3 - 1e-4), 5}} } } }
        }
    },
    PrintInfo {
        PointsVector { {5, 4} },
        std::vector<PointGravity> {},
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> { GravityType::Tolerance } // gravity of created/edited edge
    }
};

TEST_DATA(Test_3_1)
{
    "Pass through existing node, several nodes.",
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
    Polyline2 { PointsVector {{12 + (1e-3 - 1e-4), 7}, {16, 3}}}, // new polyline
    Polyline2 { PointsVector {{12 + (1e-3 - 1e-4), 7}, {13, 6}, {15, 4}, {16, 3}}}, // aligned polyline
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
        { SourceEdgeID { 205, NOT_EXISTS }, EdgeIDVector {200, 205, 210} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {12 + (1e-3 - 1e-4), 7} } },
            { 105, test::Node { 105, {16, 3} } }
        },
        {
            { 200, test::Edge {200, 100, 6, Polyline2 { PointsVector {{12 + (1e-3 - 1e-4), 7}, {13, 6}} } } },
            { 205, test::Edge {205, 6, 7, Polyline2 { PointsVector {{13, 6}, {15, 4}} } } },
            { 210, test::Edge {210, 7, 105, Polyline2 { PointsVector {{15, 4}, {16, 3}} } } }
        }
    },
    PrintInfo {
        PointsVector { {13, 6}, {15, 4} },
        std::vector<PointGravity> {},
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> { GravityType::Tolerance } // gravity of created/edited edge
    }
};

TEST_DATA(Test_4_1)
{
    "Pass through existing vertices, several vertices.",
    MockStorage {
        {
            test::Node { 1, {8, 4} },
            test::Node { 2, {12, 4} }
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{8, 4}, {12, 4}} } },
            test::Edge {2, 2, 2, Polyline2 { {{12, 4}, {11, 2}, {13, 1}, {15, 4}, {13, 6}, {10, 6}, {12, 4}} } }
        }
    },
    SourceEdgeID { 6, NOT_EXISTS },
    Polyline2 { PointsVector {{12.01, 7.01}, {16.01, 3.01}}}, // new polyline
    Polyline2 { PointsVector {{12.01, 7.01}, {13, 6}, {15, 4}, {16.01, 3.01}}}, // aligned polyline
    PointsVector {}, // splits requested
    TopologyRestrictions {
        5e-2, // tolerance
        1e-1, // junction gravity
        6e-2, // vertex gravity
        1e-1, // group junction gravity
        6e-2, // group junction snap to vertex
        Limits<double> { 1e-1 }, // segment
        Limits<double> { 1e-1 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Allowed
    },
    SplitEdges {
        { SourceEdgeID { 2, EXISTS }, EdgeIDVector {2, 3, 4} },
        { SourceEdgeID { 6, NOT_EXISTS }, EdgeIDVector {5, 3, 6} }
    },
    MockStorageDiff {
        {
            { 3, test::Node { 3, {12.01, 7.01} } },
            { 4, test::Node { 4, {13, 6} } },
            { 5, test::Node { 5, {15, 4} } },
            { 6, test::Node { 6, {16.01, 3.01} } }
        },
        {
            { 2, test::Edge {2, 2, 5, Polyline2 { PointsVector {{12, 4}, {11, 2}, {13, 1}, {15, 4}} } } },
            { 3, test::Edge {3, 5, 4, Polyline2 { PointsVector {{15, 4}, {13, 6}} } } },
            { 4, test::Edge {4, 4, 2, Polyline2 { PointsVector {{13, 6}, {10, 6}, {12, 4}} } } },
            { 5, test::Edge {5, 3, 4, Polyline2 { PointsVector {{12.01, 7.01}, {13, 6}} } } },
            { 6, test::Edge {6, 5, 6, Polyline2 { PointsVector {{15, 4}, {16.01, 3.01}} } } }
        }
    },
    PrintInfo {
        PointsVector { {13, 6}, {15, 4} },
        std::vector<PointGravity> {},
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> { GravityType::Tolerance } // gravity of created/edited edge
    }
};

TEST_DATA(Test_4_2)
{
    "Pass through existing vertices, several edges.",
    MockStorage {
        {
            test::Node { 1, {1, 2} },
            test::Node { 2, {3, 2} },
            test::Node { 3, {0, 2} },
            test::Node { 4, {-1, 2} },
            test::Node { 5, {-1, 0.08} },
            test::Node { 6, {-2, -0.08} },
            test::Node { 7, {-1, -2} }
        },
        {
            test::Edge {1, 1, 2, Polyline2 { {{1, 2}, {1, 0.04}, {3, -0.04}, {3, 2}} } },
            test::Edge {2, 3, 4, Polyline2 { {{0, 2}, {0, 0.04}, {-1, 2}} } },
            test::Edge {3, 4, 5, Polyline2 { PointsVector {{-1, 2}, {-1, 0.08}} } },
            test::Edge {4, 6, 7, Polyline2 { PointsVector {{-2, -0.08}, {-1, -2}} } }
        }
    },
    SourceEdgeID { 5, NOT_EXISTS },
    Polyline2 { PointsVector {{-3, 0}, {4, 0}}}, // new polyline
    Polyline2 {  // aligned polyline
        PointsVector{{-3, 0}, {-2, -0.08}, {-1, 0.08}, {0, 0.04}, {1, 0.04}, {3, -0.04}, {4, 0}}
    },
    PointsVector {}, // splits requested
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
        { SourceEdgeID { 1, EXISTS }, EdgeIDVector {1, 9, 11} },
        { SourceEdgeID { 2, EXISTS }, EdgeIDVector {2, 12} },
        { SourceEdgeID { 5, NOT_EXISTS }, EdgeIDVector {5, 6, 7, 8, 9, 10} }
    },
    MockStorageDiff {
        {
            { 8, test::Node { 8, {-3, 0} } },
            { 9, test::Node { 9, {0, 0.04} } },
            { 10, test::Node { 10, {1, 0.04} } },
            { 11, test::Node { 11, {3, -0.04} } },
            { 12, test::Node { 12, {4, 0} } }
        },
        {
            { 1, test::Edge {1, 1, 10, Polyline2 { PointsVector {{1, 2}, {1, 0.04}} } } },
            { 2, test::Edge {2, 3, 9, Polyline2 { PointsVector {{0, 2}, {0, 0.04}} } } },
            { 5, test::Edge {5, 8, 6, Polyline2 { PointsVector {{-3, 0}, {-2, -0.08}} } } },
            { 6, test::Edge {6, 6, 5, Polyline2 { PointsVector {{-2, -0.08}, {-1, 0.08}} } } },
            { 7, test::Edge {7, 5, 9, Polyline2 { PointsVector {{-1, 0.08}, {0, 0.04}} } } },
            { 8, test::Edge {8, 9, 10, Polyline2 { PointsVector {{0, 0.04}, {1, 0.04}} } } },
            { 9, test::Edge {9, 10, 11, Polyline2 { PointsVector {{1, 0.04}, {3, -0.04}} } } },
            { 10, test::Edge {10, 11, 12, Polyline2 { PointsVector {{3, -0.04}, {4, 0}} } } },
            { 11, test::Edge {11, 11, 2, Polyline2 { PointsVector {{3, -0.04}, {3, 2}} } } },
            { 12, test::Edge {12, 9, 4, Polyline2 { PointsVector {{0, 0.04}, {-1, 2}} } } }
        }
    },
    PrintInfo {
        PointsVector { {-2, 0}, {-1, 0}, {0, 0}, {1, 0}, {3, 0} },
        std::vector<PointGravity> {},
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> { // gravity of created/edited edge
            GravityType::JunctionGravity,
            GravityType::Tolerance
        }
    }
};

TEST_SUITE_END(intersection_pass_through_points)

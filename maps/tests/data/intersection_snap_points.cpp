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

TEST_SUITE_START(intersection_snap_points, SaveEdgeTestData)

TEST_DATA(Test_1_1_1)
{
    "Snap end at existing node.",
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
    Polyline2 { PointsVector {{3, 2}, {7, 2}}}, // new polyline
    Polyline2 { PointsVector {{3, 2}, {7, 2}}}, // aligned polyline
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
            { 100, test::Node { 100, {7, 2} } }
        },
        {
            { 200, test::Edge { 200, 1, 100, Polyline2 { PointsVector {{3, 2}, {7, 2}} } } }
        }
    },
    PrintInfo {
        PointsVector { {3, 2} },
        std::vector<PointGravity> {
            {{3, 2}, GravityType::JunctionGravity}
        },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_1_1_2)
{
    "Snap end at existing node, with several incident edges.",
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
    Polyline2 { PointsVector {{12, 4}, {15, 6}}}, // new polyline
    Polyline2 { PointsVector {{12, 4}, {15, 6}}}, // aligned polyline
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
            { 100, test::Node { 100, {15, 6} } }
        },
        {
            { 200, test::Edge { 200, 5, 100, Polyline2 { PointsVector {{12, 4}, {15, 6}} } } }
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

TEST_DATA(Test_1_1_3)
{
    "Snap end at existing node within gravity.",
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
    Polyline2 { PointsVector {{3 + (5e-2 - 5e-3), 2}, {7, 2}}}, // new polyline
    Polyline2 { PointsVector {{3, 2}, {7, 2}}}, // aligned polyline
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
    SplitEdges {},
    MockStorageDiff {
        {
            { 100, test::Node { 100, {7, 2} } }
        },
        {
            { 200, test::Edge { 200, 1, 100, Polyline2 { PointsVector {{3, 2}, {7, 2}} } } }
        }
    },
    PrintInfo {
        PointsVector { {3, 2} },
        std::vector<PointGravity> {
            {{3, 2}, GravityType::JunctionGravity}
        },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_1_1_4)
{
    "Snap end at existing node within gravity, several incident edges.",
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
    Polyline2 { PointsVector {{12 + 5e-2 / std::sqrt(2) - 5e-3, 4 + 5e-2 / std::sqrt(2) - 5e-3}, {15, 6}}}, // new polyline
    Polyline2 { PointsVector {{12, 4 }, {15, 6}}}, // aligned polyline
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
    SplitEdges {},
    MockStorageDiff {
        {
            { 100, test::Node { 100, {15, 6} } }
        },
        {
            { 200, test::Edge { 200, 5, 100, Polyline2 { PointsVector {{12, 4}, {15, 6}} } } }
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

TEST_DATA(Test_1_1_5)
{
    "Snap end at existing node with intersection.",
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
    Polyline2 { PointsVector {{3, 2 + (1e-2 - 1e-3) / 2.0}, {5, 2}}}, // new polyline
    Polyline2 { PointsVector {{3, 2}, {5, 2}}}, // aligned polyline
    PointsVector {}, // splits requested
    TopologyRestrictions {
        1e-2, // tolerance
        1e-2, // junction gravity
        1e-2, // vertex gravity
        1e-2, // group junction gravity
        1e-2, // group junction snap to vertex
        Limits<double> { 1e-2 }, // segment
        Limits<double> { 1e-2 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {},
    MockStorageDiff {
        {
            { 100, test::Node { 100, {5, 2} } }
        },
        {
            { 200, test::Edge { 200, 1, 100, Polyline2 { PointsVector {{3, 2}, {5, 2}} } } }
        }
    },
    PrintInfo {
        PointsVector { {3, 2} },
        std::vector<PointGravity> {
            {{3, 2}, GravityType::JunctionGravity}
        },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_1_1_6)
{
    "Snap end at existing node with intersection, pass through node.",
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
    Polyline2 { PointsVector {{3 - 1e-3 / std::sqrt(2) + 1e-4, 2 + 1e-3 / std::sqrt(2) - 1e-4}, {5, 0}}}, // new polyline
    Polyline2 { PointsVector {{3, 2}, {5, 0}}}, // aligned polyline
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
            { 100, test::Node { 100, {5, 0} } }
        },
        {
            { 200, test::Edge { 200, 1, 100, Polyline2 { PointsVector {{3, 2}, {5, 0}} } } }
        }
    },
    PrintInfo {
        PointsVector { {3, 2} },
        std::vector<PointGravity> {
            {{3, 2}, GravityType::JunctionGravity}
        },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_1_1_7)
{
    "Snap end at existing node, multiple edge intersections.",
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
    Polyline2 { PointsVector {{10, 2}, {12, 4 + (1e-3 - 1e-4)}}}, // new polyline
    Polyline2 { PointsVector {{10, 2}, {12, 4}}}, // aligned polyline
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
            { 100, test::Node { 100, {10, 2} } }
        },
        {
            { 200, test::Edge { 200, 100, 5, Polyline2 { PointsVector {{10, 2}, {12, 4}} } } }
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

TEST_DATA(Test_1_1_8)
{
    "Snap end at existing node, multiple edge intersections.",
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
    Polyline2 { PointsVector {{12 - (1e-3 - 1e-4), 4}, {13, 5}}}, // new polyline
    Polyline2 { PointsVector {{12, 4}, {13, 5}}}, // aligned polyline
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
            { 100, test::Node { 100, {13, 5} } }
        },
        {
            { 200, test::Edge { 200, 5, 100, Polyline2 { PointsVector {{12, 4}, {13, 5}} } } }
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

TEST_DATA(Test_1_1_9)
{
    "Snap end at existing node, multiple edge intersections and pass through node.",
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
    Polyline2 { PointsVector {{12, 4}, {12, 2}}}, // new polyline
    Polyline2 { PointsVector {{12, 4}, {12, 2}}}, // aligned polyline
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
            { 100, test::Node { 100, {12, 2} } }
        },
        {
            { 200, test::Edge { 200, 5, 100, Polyline2 { PointsVector {{12, 4}, {12, 2}} } } }
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

namespace {

const double delta_1_1_10 = (1e-2 - 1e-3) / std::sqrt(2.0);

} // namespace

TEST_DATA(Test_1_1_10)
{
    "Snap end at existing, multiple edge intersections.",
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
    Polyline2 { PointsVector {{5 - delta_1_1_10, 4 - delta_1_1_10 - 5e-4}, {5.1, 5}}}, // new polyline
    Polyline2 { PointsVector {{5, 4}, {5.1, 5}}}, // aligned polyline
    PointsVector {}, // splits requested
    TopologyRestrictions {
        1e-2, // tolerance
        1e-2, // junction gravity
        1e-2, // vertex gravity
        1e-2, // group junction gravity
        1e-2, // group junction snap to vertex
        Limits<double> { 1e-2 }, // segment
        Limits<double> { 1e-2 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {},
    MockStorageDiff {
        {
            { 100, test::Node { 100, {5.1, 5} } }
        },
        {
            { 200, test::Edge { 200, 2, 100, Polyline2 { PointsVector {{5, 4}, {5.1, 5}} } } }
        }
    },
    PrintInfo {
        PointsVector { {5, 4} },
        std::vector<PointGravity> {
            {{5, 4}, GravityType::JunctionGravity}
        },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_1_2_1)
{
    "Snap end to node, single edge, with overlap.",
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
    Polyline2 { PointsVector {{8 + (1e-2 - 1e-3), 4}, {6, 4}}}, // new polyline
    Polyline2 { PointsVector {{8, 4}, {6, 4}}}, // aligned polyline
    PointsVector {}, // splits requested
    TopologyRestrictions {
        1e-2, // tolerance
        1e-2, // junction gravity
        1e-2, // vertex gravity
        1e-2, // group junction gravity
        1e-2, // group junction snap to vertex
        Limits<double> { 1e-2 }, // segment
        Limits<double> { 1e-2 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {},
    MockStorageDiff {
        {
            { 100, test::Node { 100, {6, 4} } }
        },
        {
            { 200, test::Edge { 200, 4, 100, Polyline2 { PointsVector {{8, 4}, {6, 4}} } } }
        }
    },
    PrintInfo {
        PointsVector { {8, 4} },
        std::vector<PointGravity> {
            {{8, 4}, GravityType::JunctionGravity}
        },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_1_2_2)
{
    "Snap end to node, multiple edges, with overlap.",
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
    Polyline2 { PointsVector {{12 - (1e-3 - 1e-4), 4}, {14, 4}}}, // new polyline
    Polyline2 { PointsVector {{12, 4}, {14, 4}}}, // aligned polyline
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
            { 100, test::Node { 100, {14, 4} } }
        },
        {
            { 200, test::Edge { 200, 5, 100, Polyline2 { PointsVector {{12, 4}, {14, 4}} } } }
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

TEST_DATA(Test_1_3_1)
{
    "Snap to node through edge, without intersection.",
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
    Polyline2 { PointsVector {{3 + 1e-1 + 1e-2, 2}, {7, 2}}}, // new polyline
    Polyline2 { PointsVector {{3, 2}, {7, 2}}}, // aligned polyline
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
            { 100, test::Node { 100, {7, 2} } }
        },
        {
            { 200, test::Edge { 200, 1, 100, Polyline2 { PointsVector {{3, 2}, {7, 2}} } } }
        }
    },
    PrintInfo {
        PointsVector { {3, 2} },
        std::vector<PointGravity> { {{3, 2}, GravityType::JunctionGravity} },
        std::map<EdgeID, GravityTypesSet> {
            {1, GravityTypesSet {GravityType::JunctionGravity}}
        },
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_1_3_2)
{
    "Snap to node through edge, with intersection.",
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
    Polyline2 { PointsVector {{3, 2 + (1e-3 + 1e-4)}, {5, 0}}}, // new polyline
    Polyline2 { PointsVector {{3, 2}, {5, 0}}}, // aligned polyline
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
            { 100, test::Node { 100, {5, 0} } }
        },
        {
            { 200, test::Edge { 200, 1, 100, Polyline2 { PointsVector {{3, 2}, {5, 0}} } } }
        }
    },
    PrintInfo {
        PointsVector { {3, 2} },
        std::vector<PointGravity> { {{3, 2}, GravityType::JunctionGravity} },
        std::map<EdgeID, GravityTypesSet> {
            {1, GravityTypesSet {GravityType::JunctionGravity}}
        },
        std::set<GravityType> {}
    }
};

namespace {

const double delta_1_4 = (1e-2 + 2e-3) * (1 + 1e-2 - 1e-3) / (1e-2 - 1e-3);

}  // namespace

TEST_DATA(Test_1_4)
{
    "Intersection with edge out of gravity.",
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
    Polyline2 { PointsVector {{8, 4 + (1e-2 - 1e-3)}, {8 + delta_1_4, 3}}}, // new polyline
    Polyline2 { PointsVector {{8, 4 }, {8, 3}}}, // aligned polyline
    PointsVector {}, // splits requested
    TopologyRestrictions {
        1e-2, // tolerance
        1e-2, // junction gravity
        1e-2, // vertex gravity
        1e-2, // group junction gravity
        1e-2, // group junction snap to vertex
        Limits<double> { 1e-2 }, // segment
        Limits<double> { 1e-2 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {},
    MockStorageDiff {
        {
            { 100, test::Node { 100, {8 + delta_1_4, 3} } }
        },
        {
            { 200, test::Edge { 200, 4, 100, Polyline2 { PointsVector {{8, 4}, {8 + delta_1_4, 3}} } } }
        }
    },
    PrintInfo {
        PointsVector { {8, 4} },
        std::vector<PointGravity> {
            {{8, 4}, GravityType::JunctionGravity}
        },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

// snap endpoint to edge

TEST_DATA(Test_2_1_1)
{
    "Snap end at existing edge, with intersection.",
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
    Polyline2 { PointsVector {{9, 2}, {9, 4 + (1e-3 - 1e-4)}}}, // new polyline
    Polyline2 { PointsVector {{9, 2}, {9, 4}}}, // aligned polyline
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
        { { 3, EXISTS }, EdgeIDVector {3, 205} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {9, 2} } },
            { 105, test::Node { 105, {9, 4} } }
        },
        {
            { 200, test::Edge { 200, 100, 105, Polyline2 { PointsVector {{9, 2}, {9, 4}} } } },
            { 3, test::Edge { 3, 4, 105, Polyline2 { PointsVector {{8, 4}, {9, 4}} } } },
            { 205, test::Edge { 205, 105, 5, Polyline2 { PointsVector {{9, 4}, {12, 4}} } } }
        }
    },
    PrintInfo {
        PointsVector { {9, 4} },
        std::vector<PointGravity> {},
        std::map<EdgeID, GravityTypesSet> {
            {3, GravityTypesSet{GravityType::JunctionGravity}}
        },
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_2_1_2)
{
    "Snap end at existing edge, exact match, with intersection.",
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
    Polyline2 { PointsVector {{9, 2}, {9, 4}}}, // new polyline
    Polyline2 { PointsVector {{9, 2}, {9, 4}}}, // aligned polyline
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
        { { 3, EXISTS }, EdgeIDVector {3, 205} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {9, 2} } },
            { 105, test::Node { 105, {9, 4} } }
        },
        {
            { 200, test::Edge { 200, 100, 105, Polyline2 { PointsVector {{9, 2}, {9, 4}} } } },
            { 3, test::Edge { 3, 4, 105, Polyline2 { PointsVector {{8, 4}, {9, 4}} } } },
            { 205, test::Edge { 205, 105, 5, Polyline2 { PointsVector {{9, 4}, {12, 4}} } } }
        }
    },
    PrintInfo {
        PointsVector { {9, 4} },
        std::vector<PointGravity> {},
        std::map<EdgeID, GravityTypesSet> {
            {3, GravityTypesSet{GravityType::JunctionGravity}}
        },
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_2_1_3)
{
    "Snap end at existing edge, without intersection.",
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
    Polyline2 { PointsVector {{9, 2}, {9, 4 - (1e-3 - 1e-4)}}}, // new polyline
    Polyline2 { PointsVector {{9, 2}, {9, 4}}}, // aligned polyline
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
        { { 3, EXISTS }, EdgeIDVector {3, 205} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {9, 2} } },
            { 105, test::Node { 105, {9, 4} } }
        },
        {
            { 200, test::Edge { 200, 100, 105, Polyline2 { PointsVector {{9, 2}, {9, 4}} } } },
            { 3, test::Edge { 3, 4, 105, Polyline2 { PointsVector {{8, 4}, {9, 4}} } } },
            { 205, test::Edge { 205, 105, 5, Polyline2 { PointsVector {{9, 4}, {12, 4}} } } }
        }
    },
    PrintInfo {
        PointsVector { {9, 4} },
        std::vector<PointGravity> {},
        std::map<EdgeID, GravityTypesSet> {
            {3, GravityTypesSet{GravityType::JunctionGravity}}
        },
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_2_2_1)
{
    "Snap end at existing edge, without intersection, snap to edge internal vertex.",
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
    Polyline2 { PointsVector {{3 + (1e-3 - 1e-4), 7}, {4, 9}}}, // new polyline
    Polyline2 { PointsVector {{3 , 7}, {4, 9}}}, // aligned polyline
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
        { { 2, EXISTS }, EdgeIDVector {2, 205} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {3, 7} } },
            { 105, test::Node { 105, {4, 9} } }
        },
        {
            { 200, test::Edge { 200, 100, 105, Polyline2 { PointsVector {{3, 7}, {4, 9}} } } },
            { 2, test::Edge { 2, 2, 100, Polyline2 { PointsVector {{5, 4}, {3, 7}} } } },
            { 205, test::Edge { 205, 100, 3, Polyline2 { {{3, 7}, {6, 5}, {8, 6}, {5, 7}} } } }
        }
    },
    PrintInfo {
        PointsVector { {3, 7} },
        std::vector<PointGravity> { {{3, 7}, GravityType::JunctionGravity} },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_2_2_2)
{
    "Snap to edge internal vertex, with intersections, distance < gravity",
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
    Polyline2 { PointsVector {{3, 7 - (1e-3 - 1e-4)}, {4, 9}}}, // new polyline
    Polyline2 { PointsVector {{3, 7}, {4, 9}}}, // aligned polyline
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
        { { 2, EXISTS }, EdgeIDVector {2, 205} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {3, 7} } },
            { 105, test::Node { 105, {4, 9} } }
        },
        {
            { 200, test::Edge { 200, 100, 105, Polyline2 { PointsVector {{3, 7}, {4, 9}} } } },
            { 2, test::Edge { 2, 2, 100, Polyline2 { PointsVector {{5, 4}, {3, 7}} } } },
            { 205, test::Edge { 205, 100, 3, Polyline2 { {{3, 7}, {6, 5}, {8, 6}, {5, 7}} } } }
        }
    },
    PrintInfo {
        PointsVector { {3, 7} },
        std::vector<PointGravity> { {{3, 7}, GravityType::JunctionGravity} },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

namespace {

const double delta_2_2_3 = (1e-3 - 1e-4) / std::sqrt(2.0) / std::sqrt(10.0);

} // namespace

TEST_DATA(Test_2_2_3)
{
    "Snap to edge geom node, multiple intersections, distance > gravity.",
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
    Polyline2 { // new polyline
     PointsVector {
         {11 + 2 * delta_2_2_3, 2 + 4 * delta_2_2_3 + 5e-5},
         {12 + 3 * delta_2_2_3, -1 + delta_2_2_3}
     }
    },
    Polyline2 {
     PointsVector { // aligned polyline
         {11, 2},
         {12 + 3 * delta_2_2_3, -1 + delta_2_2_3}
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
        { { 5, EXISTS }, EdgeIDVector {205, 5} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {11, 2} } },
            { 105, test::Node { 105, {12 + 3 * delta_2_2_3, -1 + delta_2_2_3} } }
        },
        {
            { 200, test::Edge { 200, 100, 105, Polyline2 { PointsVector {{11, 2}, {12 + 3 * delta_2_2_3, -1 + delta_2_2_3}} } } },
            { 205, test::Edge { 205, 5, 100, Polyline2 { PointsVector {{12, 4}, {11, 2}} } } },
            { 5, test::Edge { 5, 100, 7, Polyline2 { {{11, 2}, {13, 1}, {15, 4}} } } }
        }
    },
    PrintInfo {
        PointsVector { {11, 2} },
        std::vector<PointGravity> {
            {{11, 2}, GravityType::JunctionGravity}
        },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

namespace {

const double delta_2_3 = (1e-3 + 1e-4) * 5.0 / 6.0;

} // namespace

TEST_DATA(Test_2_3)
{
    "New edge, check edge to snap",
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
    Polyline2 { PointsVector {{12 - (1e-3 + 1e-4), 4 + (1e-3 + 1e-4) * 2.0 / 3.0}, {9, 6.5}, {11, 6.5}}}, // new polyline
    Polyline2 { PointsVector {{12 - delta_2_3, 4 + delta_2_3}, {9, 6.5}, {11, 6.5}}}, // aligned polyline
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
        { { 4, EXISTS }, EdgeIDVector {205, 4} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {12 - delta_2_3, 4 + delta_2_3} } },
            { 105, test::Node { 105, {11, 6.5} } }
        },
        {
            { 200, test::Edge { 200, 100, 105, Polyline2 { {{12 - delta_2_3, 4 + delta_2_3}, {9, 6.5}, {11, 6.5}} } } },
            { 205, test::Edge { 205, 5, 100, Polyline2 { PointsVector {{12, 4}, {12 - delta_2_3, 4 + delta_2_3}} } } },
            { 4, test::Edge { 4, 100, 6, Polyline2 { {{12 - delta_2_3, 4 + delta_2_3}, {10, 6}, {13, 6}} } } }
        }
    },
    PrintInfo {
        PointsVector { {12 - delta_2_3, 4 + delta_2_3} },
        std::vector<PointGravity> { {{12, 4}, GravityType::JunctionGravity} },
        std::map<EdgeID, GravityTypesSet> {
            { 3, GravityTypesSet {GravityType::JunctionGravity}},
            { 4, GravityTypesSet {GravityType::JunctionGravity}}
        },
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_2_4)
{
    "Modify existing edge, check not to snap endpoint at its previous geom, end node reusage",
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
    Polyline2 { PointsVector {{12 + (1e-3 - 1e-4), 4}, {10, 7}, {11, 6}}}, // new polyline
    Polyline2 { PointsVector {{12, 4}, {10, 7}, {11, 6}}}, // aligned polyline
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
            { 4, test::Edge { 4, 5, 6, Polyline2 { {{12, 4}, {10, 7}, {11, 6}} } } }
        }
    },
    PrintInfo {
        PointsVector { {12, 4} },
        std::vector<PointGravity> { {{12, 4}, GravityType::JunctionGravity} },
        std::map<EdgeID, GravityTypesSet> {
            { 4, GravityTypesSet {GravityType::JunctionGravity}}
        },
        std::set<GravityType> {}
    }
};

namespace {

const double delta_2_5 = 1e-3 + 1e-4;

} // namespace

TEST_DATA(Test_2_5)
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
    Polyline2 { PointsVector {{12 - delta_2_5, 4 + delta_2_5 * 2.0 / 3.0}, {10, 6}, {13, 6}}}, // new polyline
    Polyline2 { PointsVector {{12 - delta_2_5, 4}, {10, 6}, {13, 6}}}, // aligned polyline
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
        { { 3, EXISTS }, EdgeIDVector {3, 200} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {12 - delta_2_5, 4} } }
        },
        {
            { 4, test::Edge { 4, 100, 6, Polyline2 { {{12 - delta_2_5, 4}, {10, 6}, {13, 6}} } } },
            { 3, test::Edge { 3, 4, 100, Polyline2 { PointsVector {{8, 4}, {12 - delta_2_5, 4}} } } },
            { 200, test::Edge { 200, 100, 5, Polyline2 { PointsVector {{12 - delta_2_5, 4}, {12, 4}} } } }
        }
    },
    PrintInfo {
        PointsVector { {12 - delta_2_5, 4}, {13, 6} },
        std::vector<PointGravity> {
            {{12, 4}, GravityType::JunctionGravity},
            {{13, 6}, GravityType::JunctionGravity}
        },
        std::map<EdgeID, GravityTypesSet> {
            { 4, GravityTypesSet{GravityType::JunctionGravity}}
        },
        std::set<GravityType> {}
    }
};

// snap to nowhere

namespace {

const double delta_3_1 = 1e-3 + 1e-4;

} // namespace

TEST_DATA(Test_3_1)
{
    "New edge, no endpoint snap.",
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
    Polyline2 { PointsVector {{8, 4 - delta_3_1}, {10, 4 - delta_3_1}}}, // new polyline
    Polyline2 { PointsVector {{8, 4 - delta_3_1}, {10, 4 - delta_3_1}}}, // aligned polyline
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
            { 100, test::Node { 100, {8, 4 - delta_3_1} } },
            { 105, test::Node { 105, {10, 4 - delta_3_1} } }
        },
        {
            { 200, test::Edge { 200, 100, 105, Polyline2 { PointsVector {{8, 4 - delta_3_1}, {10, 4 - delta_3_1}} } } }
        }
    },
    PrintInfo {
        PointsVector { {8, 4}, {10, 4} },
        std::vector<PointGravity> { {{8, 4}, GravityType::JunctionGravity} },
        std::map<EdgeID, GravityTypesSet> {
            {3, GravityTypesSet {GravityType::JunctionGravity}}
        },
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_3_2)
{
    "Snapped to one of two existing nodes.",
    MockStorage {
        {
            test::Node { 1, {7, 8} },
            test::Node { 2, {9 - (1e-3) / 2.0, 8} },
            test::Node { 3, {9 + (1e-3 + 1e-4) / 2.0, 8} },
            test::Node { 4, {11, 8} }
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{7, 8}, {9 - (1e-3) / 2.0, 8}} } },
            test::Edge {2, 3, 4, Polyline2 { PointsVector {{9 + (1e-3 + 1e-4) / 2.0, 8}, {11, 8}} } }
        }
    },
    SourceEdgeID { 200, NOT_EXISTS },
    Polyline2 { PointsVector {{9, 10}, {9, 8}}}, // new polyline
    Polyline2 { PointsVector {{9, 10}, {9 - (1e-3) / 2.0, 8}}}, // aligned polyline
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
             { 100, test::Node { 100, {9, 10} } }
        },
        {
            { 200, test::Edge { 200, 100, 2, Polyline2 { PointsVector {{9, 10}, {9 - (1e-3) / 2.0, 8}} } } }
        }
    },
    PrintInfo {
        PointsVector { {9, 8} },
        std::vector<PointGravity> {
            {{9 - (1e-3) / 2.0, 8}, GravityType::JunctionGravity},
            {{9 + (1e-3 + 1e-4) / 2.0, 8}, GravityType::JunctionGravity}
        },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

namespace {

const double delta_4_1_1 = (1e-3 + 1e-4) * std::sqrt(2);

} // namespace

TEST_DATA(Test_4_1)
{
    "Snap to edge. Node is out of gravity.",
    MockStorage {
        {
            test::Node { 1, {3, 2 + delta_4_1_1} },
            test::Node { 2, {5, 2} }
        },
        {
            test::Edge {1, 1, 2, Polyline2 { {{3, 2 + delta_4_1_1}, {2, 4}, {1, 2}, {5, 2}} } }
        }
    },
    SourceEdgeID { 205, NOT_EXISTS },
    Polyline2 { PointsVector {{3, 2 - (1e-3 - 1e-4)}, {3, 0}}}, // new polyline
    Polyline2 { PointsVector {{3, 2}, {3, 0}}}, // aligned polyline
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
        { { 1, EXISTS }, EdgeIDVector {1, 200} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {3, 2} } },
            { 105, test::Node { 105, {3, 0} } }
        },
        {
            { 1, test::Edge { 1, 1, 100, Polyline2 { {{3, 2 + delta_4_1_1}, {2, 4}, {1, 2}, {3, 2}} } } },
            { 200, test::Edge { 200, 100, 2, Polyline2 { PointsVector {{3, 2}, {5, 2}} } } },
            { 205, test::Edge { 205, 100, 105, Polyline2 { PointsVector {{3, 2}, {3, 0}} } } }
        }
    },
    PrintInfo {
        PointsVector {{3, 2}},
        std::vector<PointGravity> {
             {{3, 2}, GravityType::JunctionGravity}
        },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_4_2)
{
    "Cross the edge and snap to node.",
    MockStorage {
        {
            test::Node { 1, {3, 2.15} },
            test::Node { 2, {5, 2} }
        },
        {
            test::Edge {1, 1, 2, Polyline2 { {{3, 2.15}, {2, 4}, {1, 2}, {5, 2}} } }
        }
    },
    SourceEdgeID { 2, NOT_EXISTS },
    Polyline2 { PointsVector {{3, 2.08}, {3, 0}}}, // new polyline
    Polyline2 { PointsVector {{3, 2.15}, {3, 0}}}, // aligned polyline
    PointsVector {}, // splits requested
    TopologyRestrictions {
        1e-3, // tolerance
        1e-1, // junction gravity
        1e-3, // vertex gravity
        1e-1, // group junction gravity
        1e-3, // group junction snap to vertex
        Limits<double> { 1e-1 }, // segment
        Limits<double> { 1e-1 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {
        {{ 1, EXISTS }, EdgeIDVector {1, 4}},
        {{ 2, NOT_EXISTS }, EdgeIDVector {2, 3}}
    },
    MockStorageDiff {
        {
            { 3, test::Node { 3, {3, 2} } },
            { 4, test::Node { 4, {3, 0} } }
        },
        {
            { 1, test::Edge { 1, 1, 3, Polyline2 { {{3, 2.15}, {2, 4}, {1, 2}, {3, 2}} } } },
            { 4, test::Edge { 4, 3, 2, Polyline2 { PointsVector {{3, 2}, {5, 2}} } } },
            { 2, test::Edge { 2, 1, 3, Polyline2 { PointsVector {{3, 2.15}, {3, 2}} } } },
            { 3, test::Edge { 3, 3, 4, Polyline2 { PointsVector {{3, 2}, {3, 0}} } } }
        }
    },
    PrintInfo {
        PointsVector {{3, 2.08}},
        std::vector<PointGravity> {
             {{3, 2}, GravityType::JunctionGravity},
             {{3, 2.15}, GravityType::JunctionGravity}
        },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_5_1)
{
    "Move edge start close to previous start, reuse node.",
    MockStorage {
        {
            test::Node { 1, {3, 2} },
            test::Node { 2, {5, 2} }
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{3, 2}, {5, 2}} } }
        }
    },
    SourceEdgeID { 1, EXISTS },
    Polyline2 { PointsVector {{3, 2 - 5e-2}, {5, 2}}}, // new polyline
    Polyline2 { PointsVector {{3, 2 - 5e-2}, {5, 2}}}, // aligned polyline
    PointsVector {}, // splits requested
    TopologyRestrictions {
        1e-2, // tolerance
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
    SplitEdges {},
    MockStorageDiff {
        {
            { 1, test::Node { 1, {3, 2 - 5e-2} } }
        },
        {
            { 1, test::Edge { 1, 1, 2, Polyline2 { PointsVector {{3, 2 - 5e-2}, {5, 2}} } } }
        }
    },
    PrintInfo {
        PointsVector {{3, 2}, {5, 2}},
        std::vector<PointGravity> {
            {{3, 2}, GravityType::Tolerance},
            {{3, 2}, GravityType::JunctionGravity},
            {{5, 2}, GravityType::Tolerance}
        },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_5_2)
{
    "Move edge end close to previous end, reuse node.",
    MockStorage {
        {
            test::Node { 1, {3, 2} },
            test::Node { 2, {5, 2} }
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{3, 2}, {5, 2}} } }
        }
    },
    SourceEdgeID { 1, EXISTS },
    Polyline2 { PointsVector {{3, 2}, {5, 2 - 5e-2}}}, // new polyline
    Polyline2 { PointsVector {{3, 2}, {5, 2 - 5e-2}}}, // aligned polyline
    PointsVector {}, // splits requested
    TopologyRestrictions {
        1e-2, // tolerance
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
    SplitEdges {},
    MockStorageDiff {
        {
            { 2, test::Node { 2, {5, 2 - 5e-2} } }
        },
        {
            { 1, test::Edge { 1, 1, 2, Polyline2 { PointsVector {{3, 2}, {5, 2 - 5e-2}} } } }
        }
    },
    PrintInfo {
        PointsVector {{3, 2}, {5, 2}},
        std::vector<PointGravity> {
            {{3, 2}, GravityType::Tolerance},
            {{5, 2}, GravityType::JunctionGravity},
            {{5, 2}, GravityType::Tolerance}
        },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_6_1)
{
    "Simple tracing of points.",
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
    Polyline2 { PointsVector {{1, 8e-2}, {2, -4e-2}, {3, 2e-2}, {4, 1}}}, // new polyline
    Polyline2 { PointsVector {{1, 0}, {3, 0}, {4, 1}}}, // aligned polyline
    PointsVector {}, // splits requested
    TopologyRestrictions {
        1e-3, // tolerance
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
        { { 1, EXISTS }, EdgeIDVector {1, 3} },
        { { 2, NOT_EXISTS }, EdgeIDVector {3, 2} }
    },
    MockStorageDiff {
        {
            { 3, test::Node { 3, {1, 0} } },
            { 4, test::Node { 4, {4, 1} } }
        },
        {
            { 1, test::Edge { 1, 1, 3, Polyline2 { PointsVector {{0, 0}, {1, 0}} } } },
            { 2, test::Edge { 2, 2, 4, Polyline2 { PointsVector {{3, 0}, {4, 1}} } } },
            { 3, test::Edge { 3, 3, 2, Polyline2 { PointsVector {{1, 0}, {2, 0}, {3, 0}} } } }
        }
    },
    PrintInfo {
        PointsVector {{1, 0}, {2, 0}, {3, 0}},
        std::vector<PointGravity> {
             {{3, 0}, GravityType::JunctionGravity}
        },
        std::map<EdgeID, GravityTypesSet> {
            {1, GravityTypesSet {GravityType::JunctionGravity}}
        },
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_6_2)
{
    "Simple case of merge.",
    MockStorage {
        {
            test::Node { 1, {0, 0} },
            test::Node { 2, {5, 0} }
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{0, 0}, {3, 0}, {5, 0}} } },
        }
    },
    SourceEdgeID {2, NOT_EXISTS },
    Polyline2 { PointsVector {{-4e-2, 5e-2},  {5, -6e-2}}}, // new polyline
    Polyline2 { PointsVector {{0, 0},  {3, 0}, {5, 0}}}, // aligned polyline
    PointsVector {}, // splits requested
    TopologyRestrictions {
        1e-3, // tolerance
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
        { { 1, EXISTS }, EdgeIDVector {2} },
        { { 2, NOT_EXISTS }, EdgeIDVector {2} }
    },
    MockStorageDiff {
        {},
        {
            { 1, boost::none },
            { 2, test::Edge { 2, 1, 2, Polyline2 { PointsVector {{0, 0},  {3, 0}, {5, 0}} } } },
        }
    },
    PrintInfo {
         PointsVector { {0, 0}, {5, 0} },
         std::vector<PointGravity> {
             {{0, 0}, GravityType::JunctionGravity},
             {{5, 0}, GravityType::JunctionGravity}
         },
        std::map<EdgeID, GravityTypesSet> {},
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_6_3)
{
    "Complex tracing.",
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
    SourceEdgeID { 3, NOT_EXISTS },
    Polyline2 { PointsVector {{5, 2}, {4 - 2e-2, 3 + 2e-2}, {5, 4 - 4e-2}, {6 - 2e-2, 5 - 3e-2}, {8 + 4e-2, 6 + 2e-2}}}, // new polyline
    Polyline2 { PointsVector {{5, 2}, {4, 3}, {5, 4}, {6, 5}, {8, 6}}}, // aligned polyline
    PointsVector {}, // splits requested
    TopologyRestrictions {
        1e-3, // tolerance
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
        { { 1, EXISTS }, EdgeIDVector {1, 4} },
        { { 2, EXISTS }, EdgeIDVector {2, 6, 7} },
        { { 3, NOT_EXISTS }, EdgeIDVector {3, 4, 5, 6} }
    },
    MockStorageDiff {
        {
            { 4, test::Node { 4, {5, 2} } },
            { 5, test::Node { 5, {4, 3} } },
            { 6, test::Node { 6, {6, 5} } },
            { 7, test::Node { 7, {8, 6} } }
        },
        {
            { 1, test::Edge { 1, 1, 5, Polyline2 { PointsVector {{3, 2}, {4, 3}} } } },
            { 2, test::Edge { 2, 2, 6, Polyline2 { PointsVector {{5, 4}, {3, 7}, {6, 5}} } } },
            { 3, test::Edge { 3, 4, 5, Polyline2 { PointsVector {{5, 2}, {4, 3}} } } },
            { 4, test::Edge { 4, 5, 2, Polyline2 { PointsVector {{4, 3}, {5, 4}} } } },
            { 5, test::Edge { 5, 2, 6, Polyline2 { PointsVector {{5, 4}, {6, 5}} } } },
            { 6, test::Edge { 6, 6, 7, Polyline2 { PointsVector {{6, 5}, {8, 6}} } } },
            { 7, test::Edge { 7, 7, 3, Polyline2 { PointsVector {{8, 6}, {5, 7}} } } }
        }
    },
    PrintInfo {
        PointsVector { {4, 3}, {5, 4}, {6, 5}, {8, 6} },
        std::vector<PointGravity> {
            {{5, 4}, GravityType::VertexGravity},
            {{6, 5}, GravityType::VertexGravity},
            {{8, 6}, GravityType::JunctionGravity}
        },
        std::map<EdgeID, GravityTypesSet> {
            {1, GravityTypesSet {GravityType::VertexGravity}}
        },
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_6_4)
{
    "Complex tracing without snapping at some points.",
    MockStorage {
        {
            test::Node { 1, {3, 2} },
            test::Node { 2, {5, 4} },
            test::Node { 3, {5, 7} }
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{3, 2}, {5, 4}} } },
            test::Edge {2, 2, 3, Polyline2 { {{5, 4}, {3, 7}, {6, 5}, {8, 5}, {5, 7}} } }
        }
    },
    SourceEdgeID { 3, NOT_EXISTS },
    Polyline2 { PointsVector {{5, 2}, {3.99, 3.01}, {5.02, 4}, {9, 5.4}}}, // new polyline
    Polyline2 { PointsVector {{5, 2}, {4, 3}, {5, 4}, {8, 5}, {9, 5.4}}}, // aligned polyline
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
        { { 1, EXISTS }, EdgeIDVector {1, 4} },
        { { 2, EXISTS }, EdgeIDVector {2, 7} },
        { { 3, NOT_EXISTS }, EdgeIDVector {5, 4, 3, 6} }
    },
    MockStorageDiff {
        {
            { 4, test::Node { 4, {5, 2} } },
            { 5, test::Node { 5, {4, 3} } },
            { 6, test::Node { 6, {8, 5} } },
            { 7, test::Node { 7, {9, 5.4} } }
        },
        {
            { 1, test::Edge { 1, 1, 5, Polyline2 { PointsVector {{3, 2}, {4, 3}} } } },
            { 2, test::Edge { 2, 2, 6, Polyline2 { PointsVector {{5, 4}, {3, 7}, {6, 5}, {8, 5}} } } },
            { 3, test::Edge { 3, 2, 6, Polyline2 { PointsVector {{5, 4}, {8, 5}} } } },
            { 4, test::Edge { 4, 5, 2, Polyline2 { PointsVector {{4, 3}, {5, 4}} } } },
            { 5, test::Edge { 5, 4, 5, Polyline2 { PointsVector {{5, 2}, {4, 3}} } } },
            { 6, test::Edge { 6, 6, 7, Polyline2 { PointsVector {{8, 5}, {9, 5.4}} } } },
            { 7, test::Edge { 7, 6, 3, Polyline2 { PointsVector {{8, 5}, {5, 7}} } } }
        }
    },
    PrintInfo {
        PointsVector { {4, 3}, {5, 4}, {8, 5} },
        std::vector<PointGravity> {
            {{5, 4}, GravityType::VertexGravity},
            {{8, 5}, GravityType::Tolerance},
        },
        std::map<EdgeID, GravityTypesSet> {
            {1, GravityTypesSet {GravityType::VertexGravity}},
        },
        std::set<GravityType> {}
    }
};

TEST_DATA(Test_6_5)
{
    "Complex tracing with two edges.",
    MockStorage {
        {
            test::Node { 1, {-2, 2} },
            test::Node { 2, {2, 2} },
            test::Node { 3, {-2, 0} },
            test::Node { 4, {2, 0} }
        },
        {
            test::Edge {1, 1, 2, Polyline2 { PointsVector {{-2, 2}, {0, 3}, {2, 2}} } },
            test::Edge {2, 3, 4, Polyline2 { PointsVector {{-2, 0}, {0, 1}, {2, 0}} } }
        }
    },
    SourceEdgeID { 3, NOT_EXISTS },
    Polyline2 { PointsVector {{2.07, 2}, {2.04, 0.02}, {1.01, 0.52}, {0, 1.04}, {-1.02, 2.54}, {0.04, 3.02}, {2, 2.09}}}, // new polyline
    Polyline2 { PointsVector {{2, 2}, {2, 0}, {1, 0.5}, {0, 1}, {-1, 2.5}, {0, 3}, {1, 2.4}, {2, 2}}}, // aligned polyline
    PointsVector {}, // splits requested
    TopologyRestrictions {
        1e-3, // tolerance
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
        { { 1, EXISTS }, EdgeIDVector {1, 6} },
        { { 2, EXISTS }, EdgeIDVector {2, 4} },
        { { 3, NOT_EXISTS }, EdgeIDVector {3, 4, 5, 6} }
    },
    MockStorageDiff {
        {
            { 5, test::Node { 5, {0, 1} } },
            { 6, test::Node { 6, {-1, 2.5} } }
        },
        {
            { 1, test::Edge { 1, 1, 6, Polyline2 { PointsVector {{-2, 2}, {-1, 2.5}} } } },
            { 2, test::Edge { 2, 3, 5, Polyline2 { PointsVector {{-2, 0}, {0, 1}} } } },
            { 3, test::Edge { 3, 2, 4, Polyline2 { PointsVector {{2, 2}, {2, 0}} } } },
            { 4, test::Edge { 4, 5, 4, Polyline2 { PointsVector {{0, 1}, {1, 0.5}, {2, 0}} } } },
            { 5, test::Edge { 5, 5, 6, Polyline2 { PointsVector {{0, 1}, {-1, 2.5}} } } },
            { 6, test::Edge { 6, 6, 2, Polyline2 { PointsVector {{-1, 2.5}, {0, 3}, {2, 2}} } } }
        }
    },
    PrintInfo {
        PointsVector { {2, 2}, {2, 0}, {1, 0}, {0, 1}, {-1, 2.5}, {0, 3}},
        std::vector<PointGravity> {
            {{2, 2}, GravityType::JunctionGravity},
            {{2, 0}, GravityType::VertexGravity},
            {{0, 1}, GravityType::VertexGravity},
            {{2, 0}, GravityType::VertexGravity},
            {{0, 3}, GravityType::VertexGravity}
        },
        std::map<EdgeID, GravityTypesSet> {
            {1, GravityTypesSet {GravityType::VertexGravity}},
        },
        std::set<GravityType> {}
    }
};

TEST_SUITE_END(intersection_snap_points)

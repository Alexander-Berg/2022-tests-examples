#include "../suite.h"
#include "../../test_types/mock_storage.h"
#include "../../test_types/save_edge_test_data.h"

#include <yandex/maps/wiki/topo/common.h>
#include <yandex/maps/wiki/topo/events.h>

#include <maps/libs/geolib/include/point.h>
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

TEST_SUITE_START(intersection_incorrect, SaveEdgeTestData)

TEST_DATA(Test_1_1)
{
    "COMPLEX snap endpoints, pass through, overlaps created",
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
    SourceEdgeID { 6, NOT_EXISTS },
    Polyline2 { {{4.9990001, 4}, {2.9990001, 7}, {8, 7}, {8, 5}, {11, 7}, {11, 5.9990001}}},
    Polyline2 {},  // no aligned polyline
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
        {}
    },
    PrintInfo {
        PointsVector {},
        std::vector<PointGravity> {},
        std::map<EdgeID, test::GravityTypesSet> {},
        std::set<GravityType> {} // gravity of created/edited edge
    },
    ErrorCode::MergeOfOverlappedEdgesForbidden
};

TEST_DATA(Test_1_2)
{
    "2 ERROR snap end at existing node through projection, self-intersection",
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
    Polyline2 { {{3 + 1e-3 + 1e-4, 2}, {3, 0}, {3, 3}}},
    Polyline2 {}, // no aligned polyline
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
        {}
    },
    PrintInfo {
        PointsVector {},
        std::vector<PointGravity> {},
        std::map<EdgeID, test::GravityTypesSet> {},
        std::set<GravityType> {} // gravity of created/edited edge
    },
    ErrorCode::SelfOverlap
};

TEST_DATA(Test_1_3)
{
    "4 ERROR snap end at existing node, overlapping with existing edge created",
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
    SourceEdgeID { 6, NOT_EXISTS },
    Polyline2 { PointsVector {{12, 4}, {8 + 1e-3 - 1e-4, 8}}},
    Polyline2 {}, // no aligned polyline
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
        {}
    },
    PrintInfo {
        PointsVector {},
        std::vector<PointGravity> {},
        std::map<EdgeID, test::GravityTypesSet> {},
        std::set<GravityType> {} // gravity of created/edited edge
    },
    ErrorCode::MergeOfOverlappedEdgesForbidden
};

TEST_DATA(Test_1_4)
{
    "5 ERROR intersection of existing edge creates two points within gravity",
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
    SourceEdgeID { 6, NOT_EXISTS },
    Polyline2 { PointsVector {{10, 6 - (1e-3 + 1e-4) / std::tan(27.5 / 180.0)}, {10 + (std::tan(27.5 / 180.0) + (1e-3 + 1e-4)), 7}}},
    Polyline2 {}, // no aligned polyline
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
        {}
    },
    PrintInfo {
        PointsVector { {10, 6} },
        std::vector<PointGravity> {
            {Point2 {10, 6}, GravityType::JunctionGravity},
            {Point2 {10 + 1e-3, 6}, GravityType::JunctionGravity}
        },
        std::map<EdgeID, test::GravityTypesSet> {},
        std::set<GravityType> {} // gravity of created/edited edge
    },
    ErrorCode::DegenerateSegment
};

TEST_DATA(Test_2_1)
{
    "6 ERROR existing node is snapping to incident edge, "
    "adding new edge with endpoint snapping to this edge and then to node creates self-intersection of existing edge",
    MockStorage {
         {
             test::Node { 1, {3, 2 + (1e-3 - 6e-4)} },
             test::Node { 2, {5, 2} }
        },
        {
             test::Edge {1, 1, 2, Polyline2 { PointsVector {{3, 2 + (1e-3 - 6e-4)}, {2, 4}, {1, 2}, {5, 2}} } }
        }
    },
    SourceEdgeID { 100500, NOT_EXISTS },
    Polyline2 { PointsVector {{3, 2 - (1e-3 - 6e-4)}, {3, 0}}},
    Polyline2 {},
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
            { 100, test::Node { 100, {3, 0} } }
        },
        {
            { 200, test::Edge { 200, 1, 100, Polyline2 { PointsVector {{3, 2 + (1e-3 - 6e-4)}, {3, 0}} } } }
        }
    },
    PrintInfo {
        PointsVector { {3, 2}, {3, 2 + (1e-3 - 6e-4)} },
        std::vector<PointGravity> {
            {Point2 {3, 2}, GravityType::JunctionGravity},
            {Point2 {3, 2 + (1e-3 - 6e-4)}, GravityType::JunctionGravity}
        },
        std::map<EdgeID, test::GravityTypesSet> {
            {1, GravityTypesSet{GravityType::JunctionGravity}}
        },
        std::set<GravityType> {} // gravity of created/edited edge
    },
    ErrorCode::DegenerateSegment
};

TEST_DATA(Test_2_2)
{
    "7 ERROR existing node is snapping to incident edge, "
    "adding new edge that intersects this edge near this node creates self-intersection of existing edge",
    MockStorage {
         {
             test::Node { 1, {3, 2 + (1e-3 - 5e-4)} },
             test::Node { 2, {5, 2} }
        },
        {
             test::Edge {1, 1, 2, Polyline2 { {{3, 2 + (1e-3 - 5e-4)}, {2, 4}, {1, 2}, {5, 2}} } }
        }
    },
    SourceEdgeID { 200, NOT_EXISTS },
    Polyline2 { PointsVector {{1, 1 + (1e-3 - 5e-4)}, {5, 3}}},
    Polyline2 { PointsVector {{1, 1 + (1e-3 - 5e-4)}, {3, 2 + (1e-3 - 5e-4)}, {5, 3}}},
    PointsVector {}, // splits requested
    TopologyRestrictions {
        1e-3, // tolerance
        2e-3, // junction gravity
        1e-3, // vertex gravity
        2e-3, // group junction gravity
        1e-3, // group junction snap to vertex
        Limits<double> { 2e-3 }, // segment
        Limits<double> { 2e-3 }, // edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    SplitEdges {
        { { 200, NOT_EXISTS }, EdgeIDVector {200, 205, 210} },
        { { 1, EXISTS }, EdgeIDVector {1, 215} }
    },
    MockStorageDiff {
        {
            { 100, test::Node { 100, {1, 1 + (1e-3 - 5e-4)} } },
            { 105, test::Node { 105, {5, 3} } },
            { 110, test::Node { 110, {3 - 2 * (1e-3 - 5e-4), 2} } }
        },
        {
            { 200, test::Edge { 200, 100, 110, Polyline2 { PointsVector {{1, 1 + (1e-3 - 5e-4)}, {3 - 2 * (1e-3 - 5e-4), 2}} } } },
            { 205, test::Edge { 205, 110, 1, Polyline2 { PointsVector {{3 - 2 * (1e-3 - 5e-4), 2}, {3, 2 + (1e-3 - 5e-4)}} } } },
            { 210, test::Edge { 210, 1, 105, Polyline2 { PointsVector {{3, 2 + (1e-3 - 5e-4)}, {5, 3}} } } },
            { 1, test::Edge { 1, 1, 110, Polyline2 { {{3, 2 + (1e-3 - 5e-4)}, {2, 4}, {1, 2}, {3 - 2 * (1e-3 - 5e-4), 2}} } } },
            { 215, test::Edge { 215, 110, 2, Polyline2 { PointsVector {{3 - 2 * (1e-3 - 5e-4), 2}, {5, 2}} } } }
        }
    },
    PrintInfo {
        PointsVector { {3, 2} },
        std::vector<PointGravity> {
            {Point2 {3, 2 + (1e-3 - 5e-4)}, GravityType::JunctionGravity}
        },
        std::map<EdgeID, test::GravityTypesSet> {},
        std::set<GravityType> {} // gravity of created/edited edge
    },
    ErrorCode::DegenerateSegment
};

TEST_SUITE_END(intersection_incorrect)

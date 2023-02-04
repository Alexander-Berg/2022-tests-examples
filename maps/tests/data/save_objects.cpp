#include "../../test_types/common.h"
#include "../suite.h"

#include "../../events_data.h"
#include "../../test_types/save_objects_test_data.h"

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

TEST_SUITE_START(save_objects_tests, SaveObjectsTestData)

TEST_DATA(test_1_1)
{
    "",
    MockStorage {
        test::NodeDataVector {
            { 1, {2, 3} },
            { 2, {5, 4} },
            { 3, {1, 5} },
            { 4, {6, 6} },
            { 5, {6, 1} }
        },
        test::EdgeDataVector {
            { 1, 1, 2, Polyline2 { PointsVector {{2, 3}, {5, 4}} } },
            { 2, 1, 3, Polyline2 { PointsVector {{2, 3}, {1, 5}} } },
            { 3, 1, 1, Polyline2 { {{2, 3}, {0, 1}, {3, 1}, {2, 3}} } },
            { 4, 2, 4, Polyline2 { PointsVector {{5, 4}, {6, 6}} } },
            { 5, 2, 5, Polyline2 { PointsVector {{5, 4}, {6, 1}} } }
        }
    },
    Editor::TopologyData {
        {
            { 1, {3, 2} },
            { 2, {7, 4} }
        },
        {} // edges
    },
    TopologyRestrictions {
        1e-2, // tolerance
        1e-1, // junction gravity
        5e-2, // vertex gravity
        1e-1, // group junction gravity
        5e-2, // group junction snap to vertex
        Limits<double> { 1e-1 }, /// segment
        Limits<double> { 1e-1 }, /// edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    MockStorageDiff {
        {
            { 1, test::Node {1, {3, 2}} },
            { 2, test::Node {2, {7, 4}} }
        },
        {
            { 1, test::Edge {1, 1, 2, Polyline2 { PointsVector {{3, 2}, {7, 4}} } } },
            { 2, test::Edge {2, 1, 3, Polyline2 { PointsVector {{3, 2}, {1, 5}} } } },
            { 3, test::Edge {3, 1, 1, Polyline2 { {{3, 2}, {0, 1}, {3, 1}, {3, 2}} } } },
            { 4, test::Edge {4, 2, 4, Polyline2 { PointsVector {{7, 4}, {6, 6}} } } },
            { 5, test::Edge {5, 2, 5, Polyline2 { PointsVector {{7, 4}, {6, 1}} } } }
        }
    }
};

TEST_DATA(test_2_1)
{
    "",
    MockStorage {
        test::NodeDataVector {
            { 1, {0, 0} },
            { 2, {0, 4} },
            { 3, {4, 0} },
            { 4, {4, 4} }
        },
        test::EdgeDataVector {
            { 1, 1, 2, Polyline2 { PointsVector {{0, 0}, {0, 4}} } },
            { 2, 4, 2, Polyline2 { PointsVector {{4, 4}, {0, 4}} } }
        }
    },
    Editor::TopologyData {
        {
            { 2, Point2 {2, 2} }
        }, // nodes
        {
            { SourceEdgeID{1, EXISTS}, {}, Polyline2 { PointsVector {{4, 0}, {2, 2}} } },
            { SourceEdgeID{2, EXISTS}, {}, Polyline2 { PointsVector {{0, 0}, {2, 2}} } }
        }
    },
    TopologyRestrictions {
        1e-2, // tolerance
        1e-1, // junction gravity
        5e-2, // vertex gravity
        1e-1, // group junction gravity
        5e-2, // group junction snap to vertex
        Limits<double> { 1e-1 }, /// segment
        Limits<double> { 1e-1 }, /// edge
        UNLIMITED_MAX_INTERSECTIONS_WITH_EDGE,
        UNLIMITED_MAX_INTERSECTED_EDGES,

        MergeOfOverlappedEdgesPolicy::Forbidden
    },
    MockStorageDiff {
        {
            {2, test::Node {2, Point2 {2, 2} }}
        },
        {
            { 1, test::Edge {1, 3, 2, Polyline2 { PointsVector {{4, 0}, {2, 2}} } } },
            { 2, test::Edge {2, 1, 2, Polyline2 { PointsVector {{0, 0}, {2, 2}} } } }
        }
    }
};

TEST_SUITE_END(save_objects_tests)

#include "ring_builder.h"
#include "suite.h"

using namespace maps;
using namespace maps::wiki;
using namespace maps::wiki::geom_tools;
using namespace maps::wiki::geom_tools::test;

TEST_SUITE_START(ring_builder_tests, RingBuilderTestData)

TEST_DATA(ring_from_single_edge)
{
    geolib3::PolylinesVector {
        geolib3::Polyline2 {{ {0, 0}, {10, 0}, {10, 10}, {0, 10}, {0, 0} }}
    },
    geolib3::EPS,
    geolib3::LinearRing2 {{ {0, 0}, {10, 0}, {10, 10}, {0, 10}, {0, 0} }}
};

TEST_DATA(ring_from_several_edges)
{
    geolib3::PolylinesVector {
        geolib3::Polyline2 { geolib3::PointsVector {{0, 0}, {10, 0}} },
        geolib3::Polyline2 { geolib3::PointsVector {{10, 0}, {10, 10}} },
        geolib3::Polyline2 { geolib3::PointsVector {{10, 10}, {0, 10}} },
        geolib3::Polyline2 { geolib3::PointsVector {{0, 10}, {0, 0}} }
    },
    geolib3::EPS,
    geolib3::LinearRing2{{ {0, 0}, {10, 0}, {10, 10}, {0, 10}, {0, 0} }}
};

TEST_DATA(ring_from_several_edges_and_epsilon_touch)
{
    geolib3::PolylinesVector {
        geolib3::Polyline2 { geolib3::PointsVector {{0, 0}, {10, 0}} },
        geolib3::Polyline2 { geolib3::PointsVector {{10, 0}, {10, 8-geolib3::EPS}} },
        geolib3::Polyline2 { geolib3::PointsVector {{10, 8-geolib3::EPS}, {9, 7}} },
        geolib3::Polyline2 { geolib3::PointsVector {{9, 7}, {9, 9}} },
        geolib3::Polyline2 { geolib3::PointsVector {{9, 9}, {10, 8+geolib3::EPS}} },
        geolib3::Polyline2 { geolib3::PointsVector {{10, 8+geolib3::EPS}, {10, 10}} },
        geolib3::Polyline2 { geolib3::PointsVector {{10, 10}, {0, 10}} },
        geolib3::Polyline2 { geolib3::PointsVector {{0, 10}, {0, 0}} }
    },
    geolib3::EPS,
    geolib3::LinearRing2{{ {0, 0}, {10, 0}, {10, 8-geolib3::EPS}, {9, 7}, {9, 9}, {10, 8+geolib3::EPS}, {10, 10}, {0, 10}, {0, 0} }}
};

TEST_DATA(unclosed_ring)
{
    geolib3::PolylinesVector {
        geolib3::Polyline2 {{ {0, 0}, {10, 0}, {10, 10}, {0, 10}, {0, 20} }}
    },
    geolib3::EPS,
    boost::none
};

TEST_DATA(ring_from_two_components)
{
    geolib3::PolylinesVector {
        geolib3::Polyline2 { geolib3::PointsVector {{0, 0}, {10, 0}} },
        geolib3::Polyline2 { geolib3::PointsVector {{10, 0}, {10, 10}} },
        geolib3::Polyline2 { geolib3::PointsVector {{10, 10}, {0, 10}} },
        geolib3::Polyline2 { geolib3::PointsVector {{0, 10}, {0, 0}} },
        geolib3::Polyline2 { geolib3::PointsVector {{1, 1}, {9, 1}} },
        geolib3::Polyline2 { geolib3::PointsVector {{9, 1}, {9, 9}} },
        geolib3::Polyline2 { geolib3::PointsVector {{9, 9}, {1, 9}} },
        geolib3::Polyline2 { geolib3::PointsVector {{1, 9}, {1, 1}} }
    },
    geolib3::EPS,
    boost::none
};

TEST_DATA(ring_with_wrong_node_incidences)
{
    geolib3::PolylinesVector {
        geolib3::Polyline2 { geolib3::PointsVector {{0, 0}, {10, 0}} },
        geolib3::Polyline2 { geolib3::PointsVector {{10, 0}, {10, 10}} },
        geolib3::Polyline2 { geolib3::PointsVector {{10, 0}, {0, 10}} },
        geolib3::Polyline2 { geolib3::PointsVector {{0, 10}, {0, 0}} }
    },
    geolib3::EPS,
    boost::none
};

TEST_SUITE_END(ring_builder_tests)

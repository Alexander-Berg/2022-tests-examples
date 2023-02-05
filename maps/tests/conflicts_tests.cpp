#include <library/cpp/testing/unittest/registar.h>

#include <maps/wikimap/jams_arm2/libs/common/include/conflicts.h>

#include <iostream>

namespace maps {
namespace wiki {
namespace jams_arm2 {
namespace tests {

Y_UNIT_TEST_SUITE(segments_overlaps)
{

Y_UNIT_TEST(regular_case)
{
    UNIT_ASSERT(segmentsOverlaps(
        geolib3::Segment2({0, 0}, {1000, 0}),
        geolib3::Segment2({500, 0}, {2000, 0})
    ));

    UNIT_ASSERT(!segmentsOverlaps(
        geolib3::Segment2({-500, 0}, {500, 0}),
        geolib3::Segment2({0, -500}, {0, 500})
    ));

    UNIT_ASSERT(!segmentsOverlaps(
        geolib3::Segment2({-500, 0}, {500, 0}),
        geolib3::Segment2({0, 0}, {0, 500})
    ));
}

Y_UNIT_TEST(equal_points)
{
    UNIT_ASSERT(segmentsOverlaps(
        geolib3::Segment2({0, 0}, {1000, 0}),
        geolib3::Segment2({0, 0}, {2000, 0})
    ));
    UNIT_ASSERT(segmentsOverlaps(
        geolib3::Segment2({0, 0}, {1000, 0}),
        geolib3::Segment2({0, 0}, {1000, 0})
    ));
    UNIT_ASSERT(!segmentsOverlaps(
        geolib3::Segment2({1000, 0}, {0, 0}),
        geolib3::Segment2({0, 0}, {1000, 0})
    ));
}

Y_UNIT_TEST(almost_null_angle)
{
    UNIT_ASSERT(segmentsOverlaps(
        geolib3::Segment2({1000000, 2}, {10, 0}),
        geolib3::Segment2({1000000, 0}, {0, 0})
    ));

}

Y_UNIT_TEST(tiny_intersections)
{
    UNIT_ASSERT(segmentsOverlaps(
        geolib3::Segment2({0, 0}, {1000, 0}),
        geolib3::Segment2({998, 0}, {2000, 0})
    ));

    UNIT_ASSERT(!segmentsOverlaps(
        geolib3::Segment2({0, 0}, {1000, 0}),
        geolib3::Segment2({1000, 0}, {2000, 0})
    ));
}

} // test suite end

Y_UNIT_TEST_SUITE(edges_conflict)
{

Y_UNIT_TEST(test1)
{
    UNIT_ASSERT(edgesConflict(
                    PersistentEdgeIds({3, -2, 11, 8, -32}),
                    PersistentEdgeIds({-14, 5, 7, 99, 8})
    ));

    UNIT_ASSERT(!edgesConflict(
                    PersistentEdgeIds({3, -2, 11, 8, -32}),
                    PersistentEdgeIds({-14, 5, 7, 99, 5, -45, 0})
    ));

    UNIT_ASSERT(!edgesConflict(
                    PersistentEdgeIds(),
                    PersistentEdgeIds({-14, 5, 7, 99, 5, -45, 0})
    ));
}

} // test suite end

} // namespace tests
} // namespace jams_arm2
} // namespace wiki
} // namespace maps

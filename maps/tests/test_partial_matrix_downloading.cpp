#include <maps/b2bgeo/libs/traffic_info/traffic_info_p.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

// Test suites related to downloading cost matrices by parts.

Y_UNIT_TEST_SUITE(SplitLocations)
{

using namespace maps::b2bgeo::traffic_info::detail;
using namespace maps::b2bgeo::traffic_info;
using namespace maps::b2bgeo::common;

Y_UNIT_TEST(test_split_locations_one_group) {
    Points points;
    points.resize(8);

    const size_t maxRangeSize = 10;

    // Expecting one group.
    std::vector<PointsRange> expectedRanges;
    expectedRanges.emplace_back(points.begin(), points.end());

    const std::vector<PointsRange> ranges = splitLocations(points, maxRangeSize);

    EXPECT_EQ(ranges, expectedRanges);
}

Y_UNIT_TEST(test_split_locations_equal_maximal_groups) {
    Points points;
    points.resize(10);

    const size_t maxRangeSize = 5;

    // Expecting two groups, 5 locations in each.
    std::vector<PointsRange> expectedRanges;
    expectedRanges.emplace_back(points.begin(), points.begin() + 5);
    expectedRanges.emplace_back(points.begin() + 5, points.end());

    const std::vector<PointsRange> ranges = splitLocations(points, maxRangeSize);

    EXPECT_EQ(ranges, expectedRanges);
}

Y_UNIT_TEST(test_split_locations_unequal_groups) {
    Points points;
    points.resize(11);

    const size_t maxRangeSize = 4;

    // Expecting three groups: 4, 4 and 3 locations.
    std::vector<PointsRange> expectedRanges;
    expectedRanges.emplace_back(points.begin(), points.begin() + 4);
    expectedRanges.emplace_back(points.begin() + 4, points.begin() + 8);
    expectedRanges.emplace_back(points.begin() + 8, points.end());

    const std::vector<PointsRange> ranges = splitLocations(points, maxRangeSize);

    EXPECT_EQ(ranges, expectedRanges);
}

Y_UNIT_TEST(test_split_locations_equal_smaller_groups) {
    Points points;
    points.resize(9);

    const size_t maxRangeSize = 4;

    // We will have to split locations into 3 groups.
    // Locations are distributed between groups as equally as possible,
    // so we get 3 groups of 3 locations each.
    std::vector<PointsRange> expectedRanges;
    expectedRanges.emplace_back(points.begin(), points.begin() + 3);
    expectedRanges.emplace_back(points.begin() + 3, points.begin() + 6);
    expectedRanges.emplace_back(points.begin() + 6, points.end());

    const std::vector<PointsRange> ranges = splitLocations(points, maxRangeSize);

    EXPECT_EQ(ranges, expectedRanges);
}

} // Y_UNIT_TEST_SUITE(SplitLocations)

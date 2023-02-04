#include "maps/b2bgeo/libs/traffic_info/unique_points.h"

#include <maps/b2bgeo/libs/common/location.h>

#include <library/cpp/testing/unittest/registar.h>

#include <iomanip>
#include <iostream>

using namespace maps::b2bgeo;
using namespace maps::b2bgeo::traffic_info;

Y_UNIT_TEST_SUITE(test_unique_points) {

Y_UNIT_TEST(empty_input_produces_empty_results)
{
    const common::Locations locations;
    const auto [points, mapping] = uniquePoints(locations);

    UNIT_ASSERT(points.empty());
    UNIT_ASSERT(mapping.empty());
    UNIT_ASSERT(pointsFromMapping(locations, mapping).empty());
}

Y_UNIT_TEST(unique_points_produces_the_same_points_if_points_are_different)
{
    const common::Locations locations{{{0, 0}, 10}, {{1, 1}, 20}};
    const auto [points, mapping] = uniquePoints(locations);

    const common::Points expectedPoints{{0, 0}, {1, 1}};
    const cost_matrices::LocationIdToIndexMap expectedMapping{{10, common::MatrixId(0)}, {20, common::MatrixId(1)}};
    UNIT_ASSERT_EQUAL(points, expectedPoints);
    UNIT_ASSERT_EQUAL(mapping, expectedMapping);
    UNIT_ASSERT_EQUAL(pointsFromMapping(locations, mapping), expectedPoints);
}

Y_UNIT_TEST(close_points_are_considered_unique)
{
    const common::Locations locations{{{50 + common::SAME_PLACE_THRESHOLD_DEGREES / 2, 50}, 10}, {{50, 50}, 20}};
    const auto [points, mapping] = uniquePoints(locations);

    const common::Points expectedPoints{{50, 50}};
    const cost_matrices::LocationIdToIndexMap expectedMapping{{10, common::MatrixId(0)}, {20, common::MatrixId(0)}};
    UNIT_ASSERT_EQUAL(points, expectedPoints);
    UNIT_ASSERT_EQUAL(mapping, expectedMapping);
    UNIT_ASSERT_EQUAL(pointsFromMapping(locations, mapping), expectedPoints);
}

Y_UNIT_TEST(empty_mapping_returns_the_same_points)
{
    const common::Locations locations{{{50, 50}, 10}, {{50, 50}, 20}};
    const common::Points expectedPoints{{50, 50}, {50, 50}};
    UNIT_ASSERT_EQUAL(pointsFromMapping(locations, {}), expectedPoints);
}

} // Y_UNIT_TEST_SUITE(test_unique_points)

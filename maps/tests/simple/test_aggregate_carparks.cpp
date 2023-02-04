#include <maps/garden/modules/carparks/place_shields/tests/common/tests_common.h>

#include <maps/garden/modules/carparks/place_shields/lib/aggregate_carparks.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

#include <boost/filesystem.hpp>

using namespace maps::carparks::place_shields;
using namespace maps::carparks::common2;
using namespace maps;

void runTest(const std::unordered_map<carparks::place_shields::Id, std::vector<CarparkTiedToStreet>>&
                 carparksOnStreets,
             const std::vector<Carpark>& nonTiedCarparks,
             const std::vector<Street>& streets,
             const std::vector<LineWithShield>& expectedLines)
{
    auto lines = aggregateCarparksByZones(carparksOnStreets, nonTiedCarparks,
                                          streets);

    // The order of streets is not guaranteed because it is unordered map
    std::stable_sort(lines.begin(),
                     lines.end(),
                     [](const LineWithShield& a, const LineWithShield& b)
                     { return a.streetOriginalId < b.streetOriginalId; });

    ASSERT_EQ(lines.size(), expectedLines.size());
    for (size_t i = 0; i < lines.size(); i++) {
        ASSERT_EQ(lines[i], expectedLines[i]);
    }
}

// 1 meter is about 1e-5 degrees

Y_UNIT_TEST_SUITE(AggregateZonesSuite) {

Y_UNIT_TEST(simple_test)
{
    runTest({{0, {{{0,
                    makePolyline({{110e-5, 100e-5}, {110e-5, 200e-5}}),
                    {222, CarparkType::Toll, "RU", "org456", "100"},
                    "org456",
                    15},
                   0,
                   geolib3::Clockwise}}}},

            {},

            {{makePolyline({{100e-5, 100e-5}, {100e-5, 200e-5}}), 0, 20, 1}},

            {{makePolyline({{110e-5, 100e-5}, {110e-5, 200e-5}}),
              {222, CarparkType::Toll, "RU", "org456", "100"},
              20,
              15,
              {}}});
}

Y_UNIT_TEST(two_carparks_are_merged_oneside)
{
    runTest({{0, {{{0,
                    makePolyline({{110e-5, 100e-5}, {110e-5, 150e-5}}),
                    {222, CarparkType::Toll, "RU", "org456", "100"},
                    "org456",
                    15},
                   0,
                   geolib3::Clockwise},
                  {{0,
                    makePolyline({{130e-5, 200e-5}, {130e-5, 250e-5}}),
                    {222, CarparkType::Toll, "RU", "org456", "100"},
                    "org456",
                    15},
                   0,
                   geolib3::Clockwise}}}},

            {},

            {{makePolyline({{100e-5, 0}, {100e-5, 500e-5}}), 0, 20, 1}},

            {{makePolyline({{120e-5, 100e-5}, {120e-5, 250e-5}}),
              {222, CarparkType::Toll, "RU", "org456", "100"},
              20,
              15,
              {}}});
}

Y_UNIT_TEST(two_carparks_are_merged_twoside)
{
    runTest({{0, {{{0,
                    makePolyline({{110e-5, 100e-5}, {110e-5, 150e-5}}),
                    {222, CarparkType::Toll, "RU", "org456", "100"},
                    "org456",
                    15},
                   0,
                   geolib3::Clockwise},
                  {{0,
                    makePolyline({{80e-5, 200e-5}, {80e-5, 250e-5}}),
                    {222, CarparkType::Toll, "RU", "org456", "100"},
                    "org456",
                    15},
                   0,
                   geolib3::Counterclockwise}}}},

            {},

            {{makePolyline({{100e-5, 0}, {100e-5, 500e-5}}), 0, 20, 1}},

            {{makePolyline({{100e-5, 100e-5}, {100e-5, 250e-5}}),
              {222, CarparkType::Toll, "RU", "org456", "100"},
              20,
              15,
              {}}});
}

Y_UNIT_TEST(two_carparks_are_merged_twoside_overlapping)
{
    runTest({{0, {{{0,
                    makePolyline({{110e-5, 200e-5}, {110e-5, 100e-5}}),
                    {222, CarparkType::Toll, "RU", "org456", "100"},
                    "org456",
                    15},
                   0,
                   geolib3::Clockwise},
                  {{0,
                    makePolyline({{80e-5, 150e-5}, {80e-5, 250e-5}}),
                    {222, CarparkType::Toll, "RU", "org456", "100"},
                    "org456",
                    15},
                   0,
                   geolib3::Counterclockwise}}}},

            {},

            {{makePolyline({{100e-5, 500e-5}, {100e-5, 0}}), 0, 20, 1}},

            {{makePolyline({{100e-5, 250e-5}, {100e-5, 100e-5}}),
              {222, CarparkType::Toll, "RU", "org456", "100"},
              20,
              15,
              {}}});
}

Y_UNIT_TEST(three_carparks_ABA)
{
    runTest({{0, {{{0,
                    makePolyline({{110e-5, 100e-5}, {110e-5, 199e-5}}),
                    {222, CarparkType::Toll, "RU", "org456", "100"},
                    "org456",
                    15},
                   0,
                   geolib3::Clockwise},
                  {{0,
                    makePolyline({{110e-5, 200e-5}, {110e-5, 299e-5}}),
                    {222, CarparkType::Free, "RU", "", "100"},
                    "__free-zone",
                    15},
                   0,
                   geolib3::Clockwise},
                  {{0,
                    makePolyline({{110e-5, 300e-5}, {110e-5, 400e-5}}),
                    {222, CarparkType::Toll, "RU", "org456", "100"},
                    "org456",
                    15},
                   0,
                   geolib3::Clockwise}}}},

            {},

            {{makePolyline({{100e-5, 0}, {100e-5, 500e-5}}), 0, 20, 1}},

            {{makePolyline({{110e-5, 100e-5}, {110e-5, 199e-5}}),
              {222, CarparkType::Toll, "RU", "org456", "100"},
              20,
              15,
              {}},
             {makePolyline({{110e-5, 200e-5}, {110e-5, 299e-5}}),
              {222, CarparkType::Free, "RU", "", "100"},
              20,
              15,
              {}},
             {makePolyline({{110e-5, 300e-5}, {110e-5, 400e-5}}),
              {222, CarparkType::Toll, "RU", "org456", "100"},
              20,
              15,
              {}}});
}

Y_UNIT_TEST(three_carparks_ABA_twoside)
{
    runTest({{0, {{{0,
                    makePolyline({{110e-5, 100e-5}, {110e-5, 149e-5}}),
                    {222, CarparkType::Toll, "RU", "org456", "100"},
                    "org456",
                    15},
                   0,
                   geolib3::Clockwise},
                  {{0,
                    makePolyline({{80e-5, 150e-5}, {80e-5, 199e-5}}),
                    {222, CarparkType::Free, "RU", "", "100"},
                    "__free-zone",
                    15},
                   0,
                   geolib3::Counterclockwise},
                  {{0,
                    makePolyline({{110e-5, 200e-5}, {110e-5, 250e-5}}),
                    {222, CarparkType::Toll, "RU", "org456", "100"},
                    "org456",
                    15},
                   0,
                   geolib3::Clockwise}}}},

            {},

            {{makePolyline({{100e-5, 0}, {100e-5, 500e-5}}), 0, 20, 1}},

            {{makePolyline({{80e-5, 150e-5}, {80e-5, 199e-5}}),
              {222, CarparkType::Free, "RU", "", "100"},
              20,
              15,
              {}},
             {makePolyline({{110e-5, 100e-5}, {110e-5, 250e-5}}),
              {222, CarparkType::Toll, "RU", "org456", "100"},
              20,
              15,
              {}}});
}

Y_UNIT_TEST(only_free)
{
    runTest({{0, {{{0,
                    makePolyline({{110e-5, 100e-5}, {110e-5, 200e-5}}),
                    {222, CarparkType::Free, "RU", "", "100"},
                    "__free-zone",
                    15},
                   0,
                   geolib3::Clockwise}}}},

            {},

            {{makePolyline({{100e-5, 100e-5}, {100e-5, 200e-5}}), 0, 20, 1}},

            {});
}

Y_UNIT_TEST(two_carparks_are_not_merged_if_too_far)
{
    runTest({{0, {{{0,
                    makePolyline({{110e-5, 100e-5}, {110e-5, 200e-5}}),
                    {222, CarparkType::Toll, "RU", "org456", "100"},
                    "org456",
                    15},
                   0,
                   geolib3::Clockwise},
                  {{0,
                    makePolyline({{110e-5, 2000e-5}, {110e-5, 2100e-5}}),
                    {222, CarparkType::Toll, "RU", "org456", "100"},
                    "org456",
                    15},
                   0,
                   geolib3::Clockwise}}}},

            {},

            {{makePolyline({{100e-5, 0}, {100e-5, 3000e-5}}), 0, 20, 1}},

            {{makePolyline({{110e-5, 100e-5}, {110e-5, 200e-5}}),
              {222, CarparkType::Toll, "RU", "org456", "100"},
              20,
              15,
              {}},
             {makePolyline({{110e-5, 2000e-5}, {110e-5, 2100e-5}}),
              {222, CarparkType::Toll, "RU", "org456", "100"},
              20,
              15,
              {}}});
}

Y_UNIT_TEST(long_line_is_splitted)
{
    runTest({{0, {{{0,
                    makePolyline({{110e-5, 0}, {110e-5, 500e-5}}),
                    {222, CarparkType::Toll, "RU", "org456", "100"},
                    "org456",
                    15},
                   0,
                   geolib3::Clockwise}}}},

            {},

            {{makePolyline({{100e-5, 0}, {100e-5, 1000e-5}}), 0, 20, 1}},

            {{makePolyline({{110e-5, 0}, {110e-5, 250e-5}}),
              {222, CarparkType::Toll, "RU", "org456", "100"},
              20,
              15,
              {}},
             {makePolyline({{110e-5, 250e-5}, {110e-5, 500e-5}}),
              {222, CarparkType::Toll, "RU", "org456", "100"},
              20,
              15,
              {}}});
}

Y_UNIT_TEST(two_side_and_one_side)
{
    runTest({{0, {{{0,
                    makePolyline({{110e-5, 100e-5}, {110e-5, 200e-5}}),
                    {222, CarparkType::Toll, "RU", "org456", "100"},
                    "org456",
                    15},
                   0,
                   geolib3::Clockwise},
                  {{0,
                    makePolyline({{110e-5, 300e-5}, {110e-5, 400e-5}}),
                    {222, CarparkType::Toll, "RU", "org123", "100"},
                    "org123",
                    15},
                   0,
                   geolib3::Clockwise},
                  {{0,
                    makePolyline({{80e-5, 300e-5}, {80e-5, 400e-5}}),
                    {222, CarparkType::Toll, "RU", "org123", "100"},
                    "org123",
                    15},
                   0,
                   geolib3::Counterclockwise},
                  {{0,
                    makePolyline({{110e-5, 500e-5}, {110e-5, 600e-5}}),
                    {222, CarparkType::Toll, "RU", "org456", "100"},
                    "org456",
                    15},
                   0,
                   geolib3::Clockwise}}}},

            {},

            {{makePolyline({{100e-5, 0}, {100e-5, 3000e-5}}), 0, 20, 1}},

            {{makePolyline({{100e-5, 300e-5}, {100e-5, 400e-5}}),
              {222, CarparkType::Toll, "RU", "org123", "100"},
              20,
              15,
              {}},
             {makePolyline({{110e-5, 100e-5}, {110e-5, 200e-5}}),
              {222, CarparkType::Toll, "RU", "org456", "100"},
              20,
              15,
              {}},
             {makePolyline({{110e-5, 500e-5}, {110e-5, 600e-5}}),
              {222, CarparkType::Toll, "RU", "org456", "100"},
              20,
              15,
              {}}});
}

Y_UNIT_TEST(two_streets_and_offstreet)
{
    runTest({{0, {{{0,
                    makePolyline({{110e-5, 100e-5}, {110e-5, 200e-5}}),
                    {222, CarparkType::Toll, "RU", "org456", "100"},
                    "org456",
                    15},
                   0,
                   geolib3::Clockwise}}},
             {1, {{{0,
                   makePolyline({{510e-5, 300e-5}, {510e-5, 400e-5}}),
                   {222, CarparkType::Toll, "RU", "org123", "100"},
                   "org123",
                   15},
                  1,
                  geolib3::Clockwise}}}},

            {{0,
              makePolyline({{1000e-5, 300e-5}, {1000e-5, 400e-5}}),
              {222, CarparkType::Toll, "RU", "org123", "100"},
              "org123",
              15}},

            {{makePolyline({{100e-5, 0}, {100e-5, 1000e-5}}), 0, 20, 1},
             {makePolyline({{500e-5, 0}, {500e-5, 1000e-5}}), 1, 50, 1}},

            {{makePolyline({{1000e-5, 300e-5}, {1000e-5, 400e-5}}),
              {222, CarparkType::Toll, "RU", "org123", "100"},
              boost::none,
              15,
              {}},
             {makePolyline({{110e-5, 100e-5}, {110e-5, 200e-5}}),
              {222, CarparkType::Toll, "RU", "org456", "100"},
              20,
              15,
              {}},
             {makePolyline({{510e-5, 300e-5}, {510e-5, 400e-5}}),
              {222, CarparkType::Toll, "RU", "org123", "100"},
              50,
              15,
              {}}});
}

Y_UNIT_TEST(many_points_with_street)
{
    runTest({{0, {{{0,
                    makePolyline(
                        {{110e-5, 100e-5}, {110e-5, 160e-5}, {110e-5, 200e-5}}),
                    {222, CarparkType::Toll, "RU", "org456", "100"},
                    "org456",
                    15},
                   0,
                   geolib3::Clockwise}}}},

            {},

            {{makePolyline({{100e-5, 100e-5}, {100e-5, 150e-5}, {100e-5, 170e-5}, {100e-5, 200e-5}}),
              0, 20, 1}},

            {{makePolyline({{110e-5, 100e-5}, {110e-5, 150e-5}, {110e-5, 170e-5}, {110e-5, 200e-5}}),
              {222, CarparkType::Toll, "RU", "org456", "100"},
              20,
              15,
              {}}});
}

Y_UNIT_TEST(many_points_no_street)
{
    runTest({},

            {{0,
              makePolyline(
                  {{110e-5, 100e-5}, {110e-5, 160e-5}, {110e-5, 200e-5}}),
              {222, CarparkType::Toll, "RU", "org456", "100"},
              "org456",
              15}},

            {{makePolyline({{100e-5, 100e-5}, {100e-5, 150e-5}, {100e-5, 170e-5}, {100e-5, 200e-5}}),
              0, 20, 1}},

            {{makePolyline({{110e-5, 100e-5}, {110e-5, 160e-5}, {110e-5, 200e-5}}),
              {222, CarparkType::Toll, "RU", "org456", "100"},
              boost::none,
              15,
              {}}});
}

Y_UNIT_TEST(start_coincides_with_end)
{
    runTest({{0, {{{0,
                    makePolyline(
                        {{110e-5, 100e-5}, {110e-5, 200e-5}, {110e-5, 100e-5}}),
                    {222, CarparkType::Toll, "RU", "org456", "100"},
                    "org456",
                    15},
                   0,
                   geolib3::Clockwise}}}},

            {},

            {{makePolyline({{100e-5, 100e-5}, {100e-5, 200e-5}}), 0, 20, 1}},

            {{makePolyline({{110e-5, 100e-5}, {110e-5, 100e-5}}),
              {222, CarparkType::Toll, "RU", "org456", "100"},
              20,
              15,
              {}}});
}

Y_UNIT_TEST(slanted_offset_calculated_correctly)
{
    runTest({{0, {{{0,
                    makePolyline({{90e-5, 60 + 110e-5}, {140e-5, 60 + 160e-5}}),
                    {222, CarparkType::Toll, "RU", "org456", "100"},
                    "org456",
                    15},
                   0,
                   geolib3::Counterclockwise},
                  {{0,
                    makePolyline(
                        {{140e-5, 60 + 160e-5}, {190e-5, 60 + 210e-5}}),
                    {222, CarparkType::Toll, "RU", "org456", "100"},
                    "org456",
                    15},
                   0,
                   geolib3::Counterclockwise}}}},

            {},

            {{makePolyline({{100e-5, 60+100e-5}, {300e-5, 60+300e-5}}), 0, 20, 1}},

            {{makePolyline({{90e-5, 60+110e-5}, {190e-5, 60+210e-5}}),
              {222, CarparkType::Toll, "RU", "org456", "100"},
              20,
              15,
              {}}});
}

} // Y_UNIT_TEST_SUITE(AggregateZonesSuite)

#include <maps/garden/modules/carparks/place_shields/lib/place_shields.h>
#include <maps/garden/modules/carparks/place_shields/tests/common/tests_common.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

#include <boost/filesystem.hpp>

using namespace maps::carparks::place_shields;
using namespace maps::carparks::common2;
using namespace maps;

void runTest(const std::vector<LineWithShield>& inputLines,
             const std::vector<Street>& inputStreets,
             const std::vector<LineWithShield>& expectedLines)
{
    Streets streets;
    streets.streets = inputStreets;
    for (const auto& street: streets.streets)
        streets.rtree.insert(&street);

    auto lines = inputLines;

    placeShields(lines, streets);

    ASSERT_EQ(lines.size(), expectedLines.size());
    for (size_t i = 0; i < lines.size(); i++) {
        ASSERT_EQ(lines[i], expectedLines[i]);
    }
}

// 1 meter is about 1e-5 degrees

Y_UNIT_TEST_SUITE(PlaceShieldsSuite) {

Y_UNIT_TEST(simple_test)
{
    runTest({{makePolyline({{101e-5, 100e-5}, {101e-5, 200e-5}}),
              {222, CarparkType::Free, "RU", "org456", "100"},
              20,
              15,
              {}}},

            {},

            {{makePolyline({{101e-5, 100e-5}, {101e-5, 200e-5}}),
              {222, CarparkType::Free, "RU", "org456", "100"},
              20,
              15,
              {101e-5, 150e-5}}});
}

Y_UNIT_TEST(intersecting_shields)
{
    runTest({{makePolyline({{101e-5, 100e-5}, {101e-5, 190e-5}}),
              {222, CarparkType::Free, "RU", "org456", "100"},
              20,
              15,
              {}},
             {makePolyline({{101e-5, 110e-5}, {101e-5, 190e-5}}),
               {333, CarparkType::Free, "RU", "org456", "100"},
               20,
               15,
               {}}},

            {},

            {{makePolyline({{101e-5, 110e-5}, {101e-5, 190e-5}}),
              {333, CarparkType::Free, "RU", "org456", "100"},
              20,
              15,
              {101e-5, 150e-5}},
             {makePolyline({{101e-5, 100e-5}, {101e-5, 190e-5}}),
              {222, CarparkType::Free, "RU", "org456", "100"},
              20,
              15,
              {101e-5, 122.5e-5}}});
}

Y_UNIT_TEST(shield_away_of_street)
{
    runTest({{makePolyline({{100e-5, 100e-5}, {100e-5, 290e-5}}),
              {222, CarparkType::Free, "RU", "org456", "100"},
              20,
              15,
              {}}},

            {{makePolyline({{0, 200e-5}, {200e-5, 200e-5}}), 0, 30, 2}},

            {{makePolyline({{100e-5, 100e-5}, {100e-5, 290e-5}}),
              {222, CarparkType::Free, "RU", "org456", "100"},
              20,
              15,
              {100e-5, 172.2e-5}}});
}

Y_UNIT_TEST(own_street_is_not_considered)
{
    runTest({{makePolyline({{100e-5, 100e-5}, {100e-5, 300e-5}}),
              {222, CarparkType::Free, "RU", "org456", "100"},
              20,
              15,
              {}}},

            {{makePolyline({{200e-5, 0}, {200e-5, 200e-5}}), 0, 20, 2}},

            {{makePolyline({{100e-5, 100e-5}, {100e-5, 300e-5}}),
              {222, CarparkType::Free, "RU", "org456", "100"},
              20,
              15,
              {100e-5, 200e-5}}});
}

Y_UNIT_TEST(street_is_not_considered_for_offstreet_parking)
{
    runTest({{makePolyline({{100e-5, 100e-5}, {100e-5, 300e-5}}),
              {222, CarparkType::Free, "RU", "org456", "100"},
              boost::none,
              15,
              {}}},

            {{makePolyline({{200e-5, 0}, {200e-5, 200e-5}}), 0, 20, 2}},

            {{makePolyline({{100e-5, 100e-5}, {100e-5, 300e-5}}),
              {222, CarparkType::Free, "RU", "org456", "100"},
              boost::none,
              15,
              {100e-5, 200e-5}}});
}

Y_UNIT_TEST(parallel_street_is_workarounded)
{
    runTest({{makePolyline({{100e-5, 100e-5}, {100e-5, 300e-5}}),
              {222, CarparkType::Free, "RU", "org456", "100"},
              20,
              15,
              {}}},

            {{makePolyline({{100e-5, 100e-5}, {100e-5, 300e-5}}), 0, 30, 2}},

            {{makePolyline({{100e-5, 100e-5}, {100e-5, 300e-5}}),
              {222, CarparkType::Free, "RU", "org456", "100"},
              20,
              15,
              {100e-5, 200e-5}}});
}

Y_UNIT_TEST(intersection_is_minimized)
{
    runTest({{makePolyline({{100e-5, 100e-5}, {100e-5, 125e-5}}),
              {222, CarparkType::Free, "RU", "org456", "100"},
              20,
              10,
              {}},
             {makePolyline({{100e-5, 125e-5}, {100e-5, 125e-5}}),
              {333, CarparkType::Free, "RU", "org456", "100"},
              20,
              10,
              {}}},

            {{makePolyline({{100e-5, 100e-5}, {200e-5, 100e-5}}), 0, 30, 2}},

            {{makePolyline({{100e-5, 125e-5}, {100e-5, 125e-5}}),
              {333, CarparkType::Free, "RU", "org456", "100"},
              20,
              10,
              {100e-5, 125e-5}},
             {makePolyline({{100e-5, 100e-5}, {100e-5, 125e-5}}),
              {222, CarparkType::Free, "RU", "org456", "100"},
              20,
              10,
              {100e-5, 114.75e-5}}});
}

Y_UNIT_TEST(geo_deformation_is_accounted_for)
{
    runTest({{makePolyline({{100e-5, 100e-5}, {100e-5, 125e-5}, {125e-5, 125e-5}}),
              {222, CarparkType::Free, "RU", "org456", "100"},
              boost::none,
              10,
              {}}},

            {},

            {{makePolyline({{100e-5, 100e-5}, {100e-5, 125e-5}, {125e-5, 125e-5}}),
              {222, CarparkType::Free, "RU", "org456", "100"},
              boost::none,
              10,
              {100.084e-5, 125e-5}}});
}

Y_UNIT_TEST(dynamic_markers_simple_test)
{
    runTest({{makePolyline({{101e-5, 100e-5}, {101e-5, 200e-5}}),
              {222, CarparkType::Free, "RU", "org456", "100"},
              20,
              15,
              {},
              ShieldType::DynamicData}},

            {},

            {{makePolyline({{101e-5, 100e-5}, {101e-5, 200e-5}}),
              {222, CarparkType::Free, "RU", "org456", "100"},
              20,
              15,
              {101e-5, 150e-5},
              ShieldType::DynamicData}});
}

Y_UNIT_TEST(dynamic_markers_do_not_move_price_tags_test)
{
    runTest({{makePolyline({{25e-5, 0}, {75e-5, 0}}),
              {222, CarparkType::Free, "RU", "org456", "100"},
              20,
              15,
              {},
              ShieldType::DynamicData},
             {makePolyline({{0, 0}, {100e-5, 0}}),
              {222, CarparkType::Free, "RU", "org456", "100"},
              20,
              15,
              {},
              ShieldType::PriceTag}},

            {},

            {{makePolyline({{0, 0}, {100e-5, 0}}),
              {222, CarparkType::Free, "RU", "org456", "100"},
              20,
              15,
              {50e-5, 0},
              ShieldType::PriceTag},
             {makePolyline({{25e-5, 0}, {75e-5, 0}}),
              {222, CarparkType::Free, "RU", "org456", "100"},
              20,
              15,
              {25.5e-5, 0},
              ShieldType::DynamicData}});
}

} // Y_UNIT_TEST_SUITE(PlaceShieldsSuite)

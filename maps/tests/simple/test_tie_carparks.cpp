#include <maps/garden/modules/carparks/place_shields/lib/tie_carparks.h>
#include <maps/garden/modules/carparks/place_shields/tests/common/tests_common.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

#include <boost/filesystem.hpp>

using namespace maps::carparks::place_shields;
using namespace maps::carparks::common2;
using namespace maps;

template <class CarparkType>
void compare(const std::vector<CarparkType>& expected,
             const std::vector<CarparkType>& generated)
{
    ASSERT_EQ(expected.size(), generated.size());
    for (size_t i = 0; i < expected.size(); i++) {
        ASSERT_EQ(expected[i], generated[i]);
    }
}

void runTest(const std::vector<Carpark>& inputCarparks,
             const std::vector<Street>& inputStreets,
             const TieResult& expectedResult)
{
    Streets streets;
    streets.streets = inputStreets;
    for (const auto& street: streets.streets)
        streets.rtree.insert(&street);

    auto carparks = inputCarparks;

    auto result = tieCarparks(carparks, streets);

    ASSERT_EQ(result.carparksOnStreets.size(),
                           expectedResult.carparksOnStreets.size());
    for (const auto& object: result.carparksOnStreets) {
        const auto streetId = object.first;
        ASSERT_TRUE(expectedResult.carparksOnStreets.count(streetId));
        compare(expectedResult.carparksOnStreets.at(streetId), object.second);
    }

    compare(expectedResult.nonTiedCarparks, result.nonTiedCarparks);
}

// 1 meter is about 1e-5 degrees

Y_UNIT_TEST_SUITE(TieCarparksSuite) {

Y_UNIT_TEST(simple_test)
{
    runTest({{0,
              makePolyline({{101e-5, 100e-5}, {101e-5, 200e-5}}),
              {222, CarparkType::Free, "RU", "org456", "100"},
              "__free-zone",
              15},
             {1,
              makePolyline({{99e-5, 100e-5}, {99e-5, 200e-5}}),
              {222, CarparkType::Toll, "RU", "org456", "100"},
              "org",
              15}},

            {{makePolyline({{0, 100e-5}, {0, 200e-5}}), 0, 20, 2},
             {makePolyline({{100e-5, 100e-5}, {100e-5, 200e-5}}), 1, 20, 2}},

            {{{1, {{{0,
                     makePolyline({{101e-5, 100e-5}, {101e-5, 200e-5}}),
                     {222, CarparkType::Free, "RU", "org456", "100"},
                     "__free-zone",
                     15},
                    1,
                    geolib3::Clockwise},
                   {{1,
                     makePolyline({{99e-5, 100e-5}, {99e-5, 200e-5}}),
                     {222, CarparkType::Toll, "RU", "org456", "100"},
                     "org",
                     15},
                    1,
                    geolib3::Counterclockwise}}}},
             {}});
}

Y_UNIT_TEST(carpark_off_street)
{
    runTest({{0,
              makePolyline({{99e-5, 98e-5}, {99e-5, 99e-5}}),
              {222, CarparkType::Free, "RU", "org456", "100"},
              "__free-zone",
              15},
             {1,
              makePolyline({{99e-5, 201e-5}, {99e-5, 202e-5}}),
              {222, CarparkType::Free, "RU", "org456", "100"},
              "__free-zone",
              15}},

            {{makePolyline({{100e-5, 100e-5}, {100e-5, 200e-5}}), 0, 20, 2}},

            {{{0,
               {{{0,
                  makePolyline({{99e-5, 98e-5}, {99e-5, 99e-5}}),
                  {222, CarparkType::Free, "RU", "org456", "100"},
                  "__free-zone",
                  15},
                 0,
                 geolib3::Counterclockwise},
                {{1,
                  makePolyline({{99e-5, 201e-5}, {99e-5, 202e-5}}),
                  {222, CarparkType::Free, "RU", "org456", "100"},
                  "__free-zone",
                  15},
                 0,
                 geolib3::Counterclockwise}}
              }},
             {}});
}

// https://st.yandex-team.ru/MAPSGARDEN-4231
Y_UNIT_TEST(carpark_off_short_street)
{
    runTest({{0,
              makePolyline({{99e-5, 98e-5}, {99e-5, 99e-5}}),
              {222, CarparkType::Free, "RU", "org456", "100"},
              "__free-zone",
              15},
             {1,
              makePolyline({{99e-5, 101e-5}, {99e-5, 102e-5}}),
              {222, CarparkType::Free, "RU", "org456", "100"},
              "__free-zone",
              15}},

            {{makePolyline({{100e-5, 100e-5}, {100e-5, 100.1e-5}}), 0, 20, 2}},

            {{{0,
               {{{0,
                  makePolyline({{99e-5, 98e-5}, {99e-5, 99e-5}}),
                  {222, CarparkType::Free, "RU", "org456", "100"},
                  "__free-zone",
                  15},
                 0,
                 geolib3::Counterclockwise},
                {{1,
                  makePolyline({{99e-5, 101e-5}, {99e-5, 102e-5}}),
                  {222, CarparkType::Free, "RU", "org456", "100"},
                  "__free-zone",
                  15},
                 0,
                 geolib3::Counterclockwise}}
              }},
             {}});
}

Y_UNIT_TEST(carpark_far_away_from_street)
{
    runTest({{0,
              makePolyline({{2e-2, 100e-5}, {2e-2, 200e-5}}),
              {222, CarparkType::Free, "RU", "org456", "100"},
              "__free-zone",
              15}},

            {{makePolyline({{100e-5, 100e-5}, {100e-5, 200e-5}}), 0, 20, 2}},

            {{},
             {{0,
               makePolyline({{2e-2, 100e-5}, {2e-2, 200e-5}}),
               {222, CarparkType::Free, "RU", "org456", "100"},
               "__free-zone",
               15}}});
}

Y_UNIT_TEST(wrong_types_not_tied)
{
    runTest({{0,
              makePolyline({{101e-5, 100e-5}, {101e-5, 200e-5}}),
              {222, CarparkType::Restricted, "RU", "org456", "100"},
              "__free-zone",
              15},
             {1,
              makePolyline({{99e-5, 100e-5}, {99e-5, 200e-5}}),
              {222, CarparkType::Prohibited, "RU", "org456", "100"},
              "__free-zone",
              15}},

            {{makePolyline({{0, 100e-5}, {0, 200e-5}}), 0, 20, 2},
             {makePolyline({{100e-5, 100e-5}, {100e-5, 200e-5}}), 1, 20, 2}},

            {{},
             {{0,
               makePolyline({{101e-5, 100e-5}, {101e-5, 200e-5}}),
               {222, CarparkType::Restricted, "RU", "org456", "100"},
               "__free-zone",
               15},
              {1,
               makePolyline({{99e-5, 100e-5}, {99e-5, 200e-5}}),
               {222, CarparkType::Prohibited, "RU", "org456", "100"},
               "__free-zone",
               15}}});
}

Y_UNIT_TEST(several_streets_nearby)
{
    runTest({{0,
              makePolyline({{110e-5, 100e-5}, {110e-5, 200e-5}}),
              {222, CarparkType::Free, "RU", "org456", "100"},
              "__free-zone",
              15}},

            {{makePolyline({{100e-5, 100e-5}, {200e-5, 100e-5}}), 0, 20, 2},
             {makePolyline({{100e-5, 100e-5}, {100e-5, 200e-5}}), 1, 20, 1},
             {makePolyline({{100e-5, 200e-5}, {200e-5, 200e-5}}), 2, 20, 2}},

            {{{1,
               {{{0,
                  makePolyline({{110e-5, 100e-5}, {110e-5, 200e-5}}),
                  {222, CarparkType::Free, "RU", "org456", "100"},
                  "__free-zone",
                  15},
                 1,
                 geolib3::Clockwise}}}},
             {}});
}

Y_UNIT_TEST(several_streets_with_different_priority_nearby)
{
    runTest({{0,
              makePolyline({{110e-5, 100e-5}, {110e-5, 200e-5}}),
              {222, CarparkType::Free, "RU", "org456", "100"},
              "__free-zone",
              15}},

            {{makePolyline({{102e-5, 100e-5}, {102e-5, 200e-5}}), 0, 20, 1},
             {makePolyline({{100e-5, 100e-5}, {100e-5, 200e-5}}), 1, 20, 1},
             {makePolyline({{101e-5, 100e-5}, {101e-5, 200e-5}}), 2, 20, 2}},

            {{{2,
               {{{0,
                  makePolyline({{110e-5, 100e-5}, {110e-5, 200e-5}}),
                  {222, CarparkType::Free, "RU", "org456", "100"},
                  "__free-zone",
                  15},
                 2,
                 geolib3::Clockwise}}}},
             {}});
}

Y_UNIT_TEST(geo_distance_is_accounted_correctly)
{
    runTest({{0,
              makePolyline({{40+120e-5, 40+109e-5}, {40+110e-5, 40+120e-5}}),
              {222, CarparkType::Free, "RU", "org456", "100"},
              "__free-zone",
              15}},

            {{makePolyline(
                {{40+100e-5, 40+100e-5}, {40+200e-5, 40+100e-5}}), 0, 20, 2},
             {makePolyline(
                 {{40+100e-5, 40+100e-5}, {40+100e-5, 40+200e-5}}), 1, 20, 2}},

            {{{1, {{{0,
                     makePolyline({{40 + 120e-5, 40 + 109e-5},
                                   {40 + 110e-5, 40 + 120e-5}}),
                     {222, CarparkType::Free, "RU", "org456", "100"},
                     "__free-zone",
                     15},
                    1,
                    geolib3::Clockwise}}}},
             {}});
}

} // Y_UNIT_TEST_SUITE(TieCarparksSuite)

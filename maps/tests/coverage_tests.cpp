#include <maps/infopoint/tests/common/fixture.h>
#include <maps/infopoint/lib/misc/coverage.h>
#include <maps/infopoint/lib/misc/timezones.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <cstdlib>

using namespace infopoint;

const std::string zones_bin_regions = BinaryPath("maps/data/test/geobase/zones_bin");

TEST_F(Fixture, testRegionsMoscowPoint)
{
    maps::geolib3::Point2 p(37.6, 55.7);
    std::vector<int> expected {213, 1, 225, 10000};
    std::vector<int> actual = getRegionIdsForPoint(p);
    EXPECT_EQ(actual, expected);
}

TEST_F(Fixture, testRegionsIstanbulPoint)
{
    maps::geolib3::Point2 p(28.98, 41.05);
    std::vector<int> expected {11508, 103728, 983, 10000};
    std::vector<int> actual = getRegionIdsForPoint(p);
    EXPECT_EQ(actual, expected);
}

TEST_F(Fixture, testGeobase)
{
    infopoint::tzDataPath() = zones_bin_regions;
    EXPECT_EQ(
        tzOffset(getRegionIdsForPoint({37.6, 55.7}), 1416237353),
        60 * 60 * 3);
}

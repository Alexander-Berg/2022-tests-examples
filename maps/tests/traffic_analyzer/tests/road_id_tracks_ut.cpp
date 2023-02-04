#include <maps/wikimap/mapspro/tools/traffic_analyzer/lib/road_id_tracks.h>

#include <library/cpp/testing/unittest/gtest.h>

namespace mwt = maps::wiki::traffic_analyzer;
namespace bg = boost::gregorian;

TEST(road_id_tracks, tracksNum)
{
    std::unordered_map<uint64_t, uint32_t> map = {
        {1, 2},
        {1000000000000000ull, 4000000000u}
    };

    mwt::PersIdToTracks idToTracks(
        bg::date_period(
            bg::date(2018, bg::Jan, 30),
            bg::date(2018, bg::Jan, 31)),
        std::move(map)
    );

    // existing
    ASSERT_EQ(idToTracks.tracksNum(1), 2);

    // existing, bigger than int
    ASSERT_EQ(idToTracks.tracksNum(1000000000000000ull), 4000000000u);

    // non-existing
    ASSERT_EQ(idToTracks.tracksNum(123), 0);
}

#include <maps/automotive/radio_service/lib/radio_search/radio_data.h>
#include <maps/automotive/radio_service/lib/radio_search/radio_stations.h>
#include <maps/automotive/proto/fm.pb.h>
#include <maps/libs/http/include/test_utils.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <vector>
#include <string>

using namespace maps::automotive;
using StationProtobuf = yandex::automotive::proto::fm::StationBroadcast;
using StationsProtobuf = yandex::automotive::proto::fm::BroadcastsInfo;

namespace {

struct TestStation {
    std::string name;
    unsigned frequency;
    std::string groupId;
    double centerLon;
    double centerLat;
    uint32_t radius;
};

StationsProtobuf convertStationsToProtobuf(const std::vector<TestStation>& stations) {
    StationsProtobuf stationsProtobuf;
    stationsProtobuf.set_timestamp(1);
    for (const auto& station : stations) {
        StationProtobuf* stationProtobuf = stationsProtobuf.add_broadcasts();
        stationProtobuf->set_group_id(station.groupId.c_str());
        stationProtobuf->set_name(station.name.c_str());
        stationProtobuf->mutable_center()->set_lon(station.centerLon);
        stationProtobuf->mutable_center()->set_lat(station.centerLat);
        stationProtobuf->set_frequency(station.frequency);
        stationProtobuf->set_radius(station.radius);
    }
    return stationsProtobuf;
}

const std::unordered_map<maps::automotive::GroupId, std::vector<std::string>> SYNONYMS = {};

} // anonymous

using namespace maps::geolib3;

TEST(Towers, SimpleMerge)
{
    const auto proto = convertStationsToProtobuf({
        {"европа плюс", 10000, "1", 0., 1., 10},
        {"радио дача", 11000, "2", 1., 0., 10},
        {"монте карло", 12000, "3", 1., 1., 10}
    });
    const auto towers = BroadcastTree(proto, SYNONYMS);
    ASSERT_EQ(towers.size(), 3u);
}

TEST(Towers, MergeStationsWithEqualCentre)
{
    const auto proto = convertStationsToProtobuf({
        {"европа плюс", 10000, "1", 55.733980, 37.587093, 10},
        {"радио дача", 11000, "2", 55.733980, 37.587093, 10},
        {"монте карло", 12000, "3", 55.733980, 37.587093, 10}
    });
    const auto towers = BroadcastTree(proto, SYNONYMS);
    ASSERT_EQ(towers.size(), 1u);
}

TEST(Towers, MergeStationsWithSlightlyDifferentCentre)
{
    const auto proto = convertStationsToProtobuf({
        {"европа плюс", 10000, "1", 55.733979, 37.587093, 10},
        {"радио дача", 11000, "2", 55.733980, 37.587093, 10},
        {"монте карло", 12000, "3", 55.733980, 37.587094, 10}
    });
    const auto towers = BroadcastTree(proto, SYNONYMS);
    ASSERT_EQ(towers.size(), 3u);
}

TEST(Towers, MergeRepetitiveStations)
{
    const auto proto = convertStationsToProtobuf({
        {"европа плюс", 10000, "1", 0., 1., 10},
        {"европа плюс", 10000, "1", 0., 1., 10},
        {"европа плюс", 10000, "1", 0., 1., 10}
    });
    const auto towers = BroadcastTree(proto, SYNONYMS);
    ASSERT_EQ(towers.size(), 1u);
}

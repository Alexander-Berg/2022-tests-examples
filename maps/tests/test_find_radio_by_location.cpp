#include <maps/automotive/radio_service/lib/radio_search/radio_data.h>
#include <maps/automotive/radio_service/lib/radio_search/radio_stations.h>
#include <maps/automotive/radio_service/lib/radio_search/kdtree_util.h>
#include <maps/automotive/proto/fm.pb.h>
#include <maps/automotive/proto/internal/fm_radio_names.pb.h>
#include <maps/libs/http/include/test_utils.h>

#include <maps/libs/kdtree/include/kdtree.h>
#include <maps/libs/geolib/include/point.h>
#include <maps/libs/geolib/include/vector.h>
#include <maps/libs/geolib/include/distance.h>
#include <maps/libs/common/include/file_utils.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

#include <google/protobuf/text_format.h>

#include <vector>
#include <string>

using namespace maps::automotive;
using namespace maps::kdtree;
using namespace maps::geolib3;

namespace {

const std::string& writeBinaryStationList()
{
    const auto stationsList = maps::common::readFileToString(SRC_("data/FindRadio_StationList.prototxt"));
    yandex::automotive::proto::fm::BroadcastsInfo broadcastsProto;
    REQUIRE(
        NProtoBuf::TextFormat::ParseFromString(google::protobuf::string(stationsList), &broadcastsProto),
        "Error parsing station list");
    TString binary;
    Y_PROTOBUF_SUPPRESS_NODISCARD broadcastsProto.SerializeToString(&binary);
    static std::string filename = "stationlist.pb";
    maps::common::writeFile(filename, binary);
    return filename;
}

class FindRadioByLocation: public ::testing::Test {
public:
    FindRadioByLocation()
        : radioData(writeBinaryStationList(), SRC_("data/FindRadio_StationsNames.prototxt"))
        , location(55.73, 37.58)
    {
    }

    RadioData radioData;
    Point2 location;
};

} // anonymous

TEST_F(FindRadioByLocation, SimpleSearchByFrequencyWithPositiveResult)
{
    ASSERT_EQ((RadioStation{.name = "дача", .frequency = 11000}),
        (radioData.findRadio(location, RequestRadioInfo{.radioFrequency = 11000}).value_or(RadioStation{})));
}

TEST_F(FindRadioByLocation, TowersCantReachLocation)
{
    ASSERT_FALSE(radioData.findRadio(location, RequestRadioInfo{.radioFrequency = 13000}).has_value());
}

TEST_F(FindRadioByLocation, NoSuchRadioName)
{
    ASSERT_FALSE(radioData.findRadio(location, RequestRadioInfo{.radioName = "ретро фм"}).has_value());
}

TEST_F(FindRadioByLocation, NoSuchRadioFrequency)
{
    ASSERT_FALSE(radioData.findRadio(location, RequestRadioInfo{.radioFrequency = 14000}).has_value());
}

TEST_F(FindRadioByLocation, CheckTowerIsNearest)
{
    ASSERT_EQ((RadioStation{.name = "европа плюс", .frequency = 12000}),
        (radioData.findRadio(location, RequestRadioInfo{.radioName = "европа плюс"}).value_or(RadioStation())));
}

TEST_F(FindRadioByLocation, FindWithSynonym)
{
    ASSERT_EQ((RadioStation{.name = "европа плюс", .frequency = 12000}),
        (radioData.findRadio(location, RequestRadioInfo{.radioName = "европка"}).value_or(RadioStation())));
}

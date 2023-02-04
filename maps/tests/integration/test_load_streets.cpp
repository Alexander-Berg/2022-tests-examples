#include <maps/garden/modules/carparks/place_shields/lib/load_streets.h>
#include <maps/garden/modules/carparks/place_shields/tests/common/tests_common.h>
#include <maps/garden/modules/carparks/test_utils/yt_test_utils.h>
#include <mapreduce/yt/interface/client.h>
#include <maps/libs/common/include/hex.h>

#include <boost/format.hpp>
#include <library/cpp/testing/unittest/registar.h>


namespace {

namespace yt_test_utils = maps::carparks::test_utils::yt;
using maps::geolib3::EWKB;
using yt_test_utils::YtFixture;
using StreetsType = std::vector<std::pair<maps::geolib3::Polyline2, int>>;
using namespace maps::carparks::place_shields;

const std::string RD_GEOM_PATH = "//home/ymapsdf/cis1_yandex/rd_geom";
const std::string RD_EL_PATH = "//home/ymapsdf/cis1_yandex/rd_el";

class PlaceShieldLoadStreetsFixture : public YtFixture {
public:
    PlaceShieldLoadStreetsFixture() = default;

    void prepareTestData()
    {
        auto tablePath = NYT::TYPath(RD_GEOM_PATH);
        createTable(tablePath);
        auto writer = client_->CreateTableWriter<NYT::TNode>(tablePath);
        writer->AddRow(
            NYT::TNode::CreateMap()
                ("shape", TString(ewkbString(
                    makePolyline({{1e-3, 2e-3}, {3e-3, 4e-3}, {5e-3, 6e-3}}))))
            );
        writer->AddRow(
            NYT::TNode::CreateMap()
                ("shape", TString(ewkbString(
                    makePolyline({{0, 0}, {0.15, 0}}))))
            );
        writer->AddRow(
            NYT::TNode::CreateMap()
                ("shape", TString(ewkbString<std::vector<maps::geolib3::Polyline2>>({
                    {makePolyline({{7e-3, 8e-3}, {8e-3, 7e-3}})},
                    {makePolyline({{9e-3, 10e-3}, {10e-3, 9e-3}})}
                })))
            );

        writer->Finish();
        tablePath = NYT::TYPath(RD_EL_PATH);
        createTable(tablePath);
        writer = client_->CreateTableWriter<NYT::TNode>(tablePath);
        writer->AddRow(
            NYT::TNode::CreateMap()
                ("shape", TString(ewkbString(
                    makePolyline({{4e-3, 5e-3}, {6e-3, 7e-3}}))))
                ("fc", 3)
            );
        writer->AddRow(
            NYT::TNode::CreateMap()
                ("shape", TString(ewkbString(
                    makePolyline({{4e-3, 5e-3}, {6e-3, 7e-3}}))))
                ("fc", 8)
            );
        writer->Finish();


    }
};

void verifyData(
    const Streets& streets,
    StreetsType expectedStreets)
{
    ASSERT_EQ(streets.streets.size(), expectedStreets.size());
    for (size_t i = 0; i < streets.streets.size(); i++) {
        const auto& street = streets.streets[i];
        bool found = false;
        for (auto it = expectedStreets.begin();
             it != expectedStreets.end();
             it++) {
            if (street.line == it->first && street.priority == it->second) {
                    found = true;
                    expectedStreets.erase(it);
                    break;
                }
        }
        ASSERT_TRUE(found) << "Generated street "
            << streets.streets[i] << " not expected";
    }

    // simple check that rtree is somehow working
    BoundingBox bbox(maps::geolib3::BoundingBox({-80, -80}, {80, 80}));
    auto range = streets.rtree.equal_range(bbox);
    int nStreetsInRtree = std::distance(range.begin(), range.end());
    ASSERT_EQ(streets.streets.size(), (const unsigned long)nStreetsInRtree);
}

} // namespace

Y_UNIT_TEST_SUITE_F(PlaceShieldStreetSuite, PlaceShieldLoadStreetsFixture) {

Y_UNIT_TEST(LoadStreetsCase)
{
    prepareTestData();

    auto streets = loadStreets(RD_GEOM_PATH, RD_EL_PATH);

    verifyData(
        *streets,
        {{makePolyline({{1e-3, 2e-3}, {3e-3, 4e-3}, {5e-3, 6e-3}}), 2},
         {makePolyline({{0, 0}, {0.075, 0}}), 2},
         {makePolyline({{0.075, 0}, {0.15, 0}}), 2},
         {makePolyline({{7e-3, 8e-3}, {8e-3, 7e-3}}), 2},
         {makePolyline({{9e-3, 10e-3}, {10e-3, 9e-3}}), 2},
         {makePolyline({{4e-3, 5e-3}, {6e-3, 7e-3}}), 1}
        });
}

} // Y_UNIT_TEST_SUITE(PlaceShieldStreetSuite)

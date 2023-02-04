#include <maps/garden/modules/carparks/place_shields/lib/load_carparks.h>
#include <maps/garden/modules/carparks/place_shields/tests/common/tests_common.h>
#include <maps/garden/modules/carparks/test_utils/yt_test_utils.h>
#include <mapreduce/yt/util/ypath_join.h>

#include <library/cpp/testing/unittest/registar.h>


using namespace maps::carparks::place_shields;

namespace {
namespace place_shields = maps::carparks::place_shields;
using namespace maps::carparks::test_utils::yt;
using CarparkType = maps::carparks::common2::CarparkType;

const std::string LINES_TABLE = "l";

class PlaceShieldsLoadCarparksFixture : public YtFixture {
public:
    void prepareTestData(const std::map<Id, Carpark>& carparks)
    {
        auto tablePath = NYT::JoinYPaths(CARPARKS_FOLDER, LINES_TABLE);
        createTable(tablePath);
        auto writer = client_->CreateTableWriter<NYT::TNode>(tablePath);
        for (const auto& object: carparks) {
            const auto& carpark = object.second;
            writer->AddRow(
            NYT::TNode::CreateMap()
                ("id", static_cast<int64_t>(carpark.info.id()))
                ("type", TString(toString(carpark.info.type())))
                ("geom_geo", TString(place_shields::ewkbString(carpark.line)))
                ("isocode", TString(carpark.info.isocode()))
                ("org_id", TString(boost::lexical_cast<std::string>(carpark.info.orgId())))
                ("price", TString(boost::lexical_cast<std::string>(carpark.info.price())))
            );

        }
        writer->Finish();
    }
};

std::map<Id, Carpark> createTestData()
{
    std::map<Id, Carpark> testData;
    testData.emplace(111, Carpark{
        0,
        makePolyline({{0, 0}, {1, 1}}),
        {111, CarparkType::Toll, "RU", "org123", "100"},
        "org123",
        7});
    testData.emplace(222, Carpark{
        0,
        makePolyline({{2, 2}, {3, 3}}),
        {222, CarparkType::Free, "RU", "org456", "100"},
        "__free-zone",
        7});
    testData.emplace(333, Carpark{
        0,
        makePolyline({{4, 4}, {5, 5}}),
        {333, CarparkType::Toll, "RU", "org-no-price", ""},
        "__unknown-zone-333",
        7});
    testData.emplace(444, Carpark{
        0,
        makePolyline({{6, 6}, {7, 7}}),
        {444, CarparkType::Prohibited, "RU", "", "100"},
        "",
        1});
    return testData;
};

} // namespace

Y_UNIT_TEST_SUITE_F(PlaceShieldCarparksSuite, PlaceShieldsLoadCarparksFixture) {

Y_UNIT_TEST(LoadCarparksCase)
{
    auto testData = createTestData();

    createTable(NYT::JoinYPaths(CARPARKS_FOLDER, LINES_TABLE));
    prepareTestData(testData);

    auto carparks = loadCarparks(NYT::TYPath(CARPARKS_FOLDER));

    ASSERT_EQ(carparks.size(), testData.size());
    auto notSeenCarparks = testData;
    for (const auto& carpark: carparks) {
        auto found = notSeenCarparks.find(carpark.info.id());
        ASSERT_NE(found, notSeenCarparks.end());
        ASSERT_EQ(carpark, found->second);
        notSeenCarparks.erase(found);
    }
}

} // Y_UNIT_TEST_SUITE(PlaceShieldCarparksSuite)

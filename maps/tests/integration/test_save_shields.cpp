#include <maps/garden/modules/carparks/place_shields/lib/save_shields.h>
#include <maps/garden/modules/carparks/place_shields/tests/common/tests_common.h>
#include <maps/garden/modules/carparks/test_utils/yt_test_utils.h>
#include <mapreduce/yt/interface/client.h>
#include <mapreduce/yt/util/ypath_join.h>
#include <maps/libs/common/include/hex.h>

#include <boost/format.hpp>
#include <boost/lexical_cast.hpp>
#include <library/cpp/testing/unittest/registar.h>

using namespace maps::carparks::common2;
using namespace maps::carparks::test_utils::yt;
using namespace maps::geolib3;

namespace {
namespace place_shields = maps::carparks::place_shields;
using place_shields::ShieldType;
using place_shields::LineWithShield;

const std::string SHIELDS_TABLE = "s";
const std::string SHIELD_LINES_TABLE = "sl";
const std::string DYNAMIC_MARKERS_TABLE = "dynamic_markers";

struct ExpectedShield {
    CarparkInfo<mms::Standalone> info;
    Point2 point;
};

class PlaceShieldSaveShieldsFixture : public YtFixture {
public:
    PlaceShieldSaveShieldsFixture() = default;

    void verify(
        const std::string& table,
        const std::map<place_shields::Id, ExpectedShield>& expectedShields)
    {
        auto carparksPath = NYT::JoinYPaths(CARPARKS_FOLDER, table);
        auto reader = client_->CreateTableReader<NYT::TNode>(carparksPath);

        std::vector<ExpectedShield> foundShields;
        for (; reader->IsValid(); reader->Next()) {
            auto& row = reader->GetRow();
            auto shieldPoint = EWKB::read<SpatialReference::Epsg4326, Point2>(
                maps::hexDecode(row["geom_geo"].AsString()));
            auto id = static_cast<place_shields::Id>(row["id"].AsInt64());
            auto orgId = row["org_id"].IsNull() ? "" : row["org_id"].AsString();
            auto price = row["price"].IsNull() ? "" : row["price"].AsString();
            CarparkInfo<mms::Standalone> info(
                id,
                boost::lexical_cast<CarparkType>(row["type"].AsString()),
                row["isocode"].AsString(),
                orgId,
                price);

            foundShields.push_back({std::move(info), shieldPoint});
        }

        ASSERT_EQ(foundShields.size(), expectedShields.size());

        for (const auto& shield: foundShields) {
            const auto& expected = expectedShields.at(shield.info.id());
            ASSERT_EQ(shield.info, expected.info);

            ASSERT_DOUBLE_EQ(shield.point.x(), expected.point.x());
            ASSERT_DOUBLE_EQ(shield.point.y(), expected.point.y());
        }
    }
};

} // namespace

Y_UNIT_TEST_SUITE_F(PlaceShieldSuite, PlaceShieldSaveShieldsFixture) {

Y_UNIT_TEST(SaveShieldsCase)
{
    createTable(NYT::JoinYPaths(CARPARKS_FOLDER, SHIELDS_TABLE));
    createTable(NYT::JoinYPaths(CARPARKS_FOLDER, SHIELD_LINES_TABLE));
    createTable(NYT::JoinYPaths(CARPARKS_FOLDER, DYNAMIC_MARKERS_TABLE));

    std::vector<LineWithShield> testData {
        {place_shields::makePolyline({{1.01, 1.10}, {1.01, 1.90}}),
         {333, CarparkType::Free, "RU", "org456", "100"},
         20,
         15,
         {1.01, 1.50},
         ShieldType::PriceTag},
        {place_shields::makePolyline({{2.01, 2.10}, {2.01, 2.90}}),
         {333, CarparkType::Free, "RU", "org456", "100"},
         20,
         15,
         {2.01, 2.50},
         ShieldType::PriceTag},
        {place_shields::makePolyline({{2.01, 2.10}, {2.01, 2.90}}),
         {100, CarparkType::Free, "RU", "org456", "100"},
         20,
         15,
         {3.01, 2.50},
         ShieldType::DynamicData},
        {place_shields::makePolyline({{1.00, 1.00}, {1.00, 1.90}}),
         {222, CarparkType::Toll, "RU", "org456", "100"},
         20,
         15,
         {1.00, 1.198},
         ShieldType::PriceTag},
        {place_shields::makePolyline({{2.01, 2.10}, {2.01, 2.90}}),
         {150, CarparkType::Toll, "RU", "org456", "100"},
         20,
         15,
         {4.01, 2.70},
         ShieldType::DynamicData}};

    saveShields(testData,
                CARPARKS_FOLDER,
                2, // regionNumber
                3 // totalRegions
    );

    verify(
        SHIELDS_TABLE,
        {{2, {{2, CarparkType::Free, "RU", "org456", "100"}, {1.01, 1.50}}},
         {5, {{5, CarparkType::Free, "RU", "org456", "100"}, {2.01, 2.50}}},
         {8, {{8, CarparkType::Toll, "RU", "org456", "100"}, {1.00, 1.198}}}});

    verify(
        DYNAMIC_MARKERS_TABLE,
        {
            {100,
             {{100, CarparkType::Free, "RU", "org456", "100"}, {3.01, 2.50}}},
            {150,
             {{150, CarparkType::Toll, "RU", "org456", "100"}, {4.01, 2.70}}},
        });
}

} // Y_UNIT_TEST_SUITE(PlaceShieldSuite)

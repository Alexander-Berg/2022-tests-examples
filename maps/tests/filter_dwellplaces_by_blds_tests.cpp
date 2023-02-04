#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>

#include <maps/libs/geolib/include/point.h>
#include <maps/libs/geolib/include/polygon.h>
#include <maps/libs/geolib/include/test_tools/comparison.h>

#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/objects/include/building.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/objects/include/dwellplace.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/detection/include/state.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/detection/include/filter_dwellplaces_by_blds.h>

#include <mapreduce/yt/util/temp_table.h>

#include <maps/libs/tile/include/const.h>

using namespace testing;

namespace maps::wiki::autocart::pipeline {

namespace tests {

Y_UNIT_TEST_SUITE(filter_dwellplaces_by_blds_tests)
{

Y_UNIT_TEST(basic_test)
{
    const std::vector<Dwellplace> places{
        Dwellplace::fromMercatorGeom(geolib3::Point2(1., 1.)),
        Dwellplace::fromMercatorGeom(geolib3::Point2(5., 5.)),
        Dwellplace::fromMercatorGeom(geolib3::Point2(7., 7.))
    };
    const std::vector<Building> blds{
        Building::fromMercatorGeom(
            geolib3::Polygon2({{2., 1.}, {3., 1.}, {3., 2.}, {2., 2.}})),
        Building::fromMercatorGeom(
            geolib3::Polygon2({{3., 5.}, {4., 5.}, {4., 6.}, {3., 6.}}))
    };
    const double distanceInMeters = 1.5;
    const std::vector<Dwellplace> expectedPlaces{places[2]};

    NYT::IClientPtr client = NYT::NTesting::CreateTestClient();

    const TString STATE_PATH = "//home/state";
    State::initialize(client, STATE_PATH);

    NYT::TTempTable inputYTTable(client);
    NYT::TTempTable bldsYTTable(client);
    NYT::TTempTable outputYTTable(client);

    auto placeWriter = client->CreateTableWriter<NYT::TNode>(inputYTTable.Name());
    for (const Dwellplace& place : places) {
        placeWriter->AddRow(place.toYTNode());
    }
    placeWriter->Finish();
    auto bldWriter = client->CreateTableWriter<NYT::TNode>(bldsYTTable.Name());
    for (const Building& bld : blds) {
        bldWriter->AddRow(bld.toYTNode());
    }
    bldWriter->Finish();

    NYT::ITransactionPtr txn = client->StartTransaction();

    filterDwellplacesByBuildings(
        txn,
        inputYTTable.Name(),
        bldsYTTable.Name(),
        distanceInMeters,
        outputYTTable.Name()
    );

    txn->Commit();

    std::vector<Dwellplace> testPlaces;
    auto reader = client->CreateTableReader<NYT::TNode>(outputYTTable.Name());
    for (; reader->IsValid(); reader->Next()) {
        const auto& row = reader->GetRow();
        testPlaces.emplace_back(Dwellplace::fromYTNode(row));
    }

    EXPECT_TRUE(
        std::is_permutation(
            testPlaces.begin(), testPlaces.end(),
            expectedPlaces.begin()
        )
    );

    State::remove(client);
}

} // Y_UNIT_TEST_SUITE(filter_dwellplaces_by_blds_tests)

} // namespace test

} // namespace maps::wiki::autocart::pipeline

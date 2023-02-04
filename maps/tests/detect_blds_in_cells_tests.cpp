#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>

#include <maps/libs/geolib/include/const.h>
#include <maps/libs/geolib/include/point.h>
#include <maps/libs/geolib/include/polygon.h>
#include <maps/libs/geolib/include/polyline.h>
#include <maps/libs/geolib/include/bounding_box.h>
#include <maps/libs/geolib/include/test_tools/comparison.h>

#include <maps/libs/tile/include/utils.h>

#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/objects/include/bbox.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/objects/include/area.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/objects/include/road.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/objects/include/building.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/detection/include/detect_blds_in_cells.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/detection/tests/tiles_server_fixture.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/detection/include/state.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/config/include/config.h>

#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/detection/tests/config_template.h>

#include <mapreduce/yt/util/temp_table.h>

#include <util/generic/size_literals.h>

#include <opencv2/opencv.hpp>

using namespace testing;

namespace maps::wiki::autocart::pipeline {

namespace tests {

namespace {

static const std::string CONFIG_TEMPLATE
    = "maps/wikimap/mapspro/services/autocart/pipeline/libs/detection/tests/configs/detector.json";

} // namespace

Y_UNIT_TEST_SUITE_F(detect_blds_in_cells_tests, TilesServerFixture)
{

Y_UNIT_TEST(detect_blds_in_cells_test)
{
    DetectorConfig config(
        makeConfigFromTemplate(BinaryPath(CONFIG_TEMPLATE), tileSourceUrl())
    );

    const BBox bbox = BBox::fromGeodeticGeom({{37.933032, 51.300595}, {37.935430, 51.302091}});

    NYT::IClientPtr client = NYT::NTesting::CreateTestClient();

    State::initialize(client, "//home/state");

    NYT::TTempTable inputYTTable(client);
    NYT::TTempTable outputYTTable(client);

    auto writer = client->CreateTableWriter<NYT::TNode>(inputYTTable.Name());
    NYT::TNode node;
    bbox.toYTNode(node);
    roadsToYTNode({}, node); // without roads
    bldsToYTNode({}, node); // without buildings
    areasToYTNode({}, node); // without areas
    writer->AddRow(node);
    writer->Finish();

    NYT::ITransactionPtr txn = client->StartTransaction();

    detectBuildingsInCells(
        txn,
        inputYTTable.Name(),
        config,
        outputYTTable.Name()
    );

    txn->Commit();

    std::vector<Building> blds;
    auto reader = client->CreateTableReader<NYT::TNode>(outputYTTable.Name());
    for (; reader->IsValid(); reader->Next()) {
        const auto& row = reader->GetRow();
        blds.push_back(Building::fromYTNode(row));
    }
    EXPECT_TRUE(!blds.empty());

    State::remove(client);
}

} // Y_UNIT_TEST_SUITE(detect_blds_in_cells_tests)

} // namespace test

} // namespace maps::wiki::autocart::pipeline

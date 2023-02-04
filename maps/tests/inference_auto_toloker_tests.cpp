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

#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/objects/include/building.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/toloka/include/states.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/detection/include/inference_auto_toloker.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/detection/tests/tiles_server_fixture.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/storage/include/detection_results.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/config/include/config.h>

#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/detection/tests/config_template.h>

#include <mapreduce/yt/util/temp_table.h>

using namespace testing;

namespace maps::wiki::autocart::pipeline {

namespace tests {

namespace {

static const std::string CONFIG_TEMPLATE
    = "maps/wikimap/mapspro/services/autocart/pipeline/libs/detection/tests/configs/auto_toloker.json";

} // namespace

Y_UNIT_TEST_SUITE_F(inference_auto_toloker_tests, TilesServerFixture)
{

Y_UNIT_TEST(inference_auto_toloker_test)
{
    AutoTolokerConfig config(
        makeConfigFromTemplate(BinaryPath(CONFIG_TEMPLATE), tileSourceUrl())
    );

    const Building goodBld = Building::fromMercatorGeom(
        geolib3::Polygon2({
            {3092799.855150931515, -3013898.696583164390},
            {3092811.136413619388, -3013900.859227217268},
            {3092815.701346064918, -3013877.046615970321},
            {3092804.420083377045, -3013874.883971919306}
        })
    );
    const Building badBld = Building::fromMercatorGeom(
        geolib3::Polygon2({
            {3092905.296689810231, -3013941.984457024839},
            {3092892.456274354365, -3013939.534907308873},
            {3092890.204773317557, -3013951.337161632255},
            {3092903.045188773423, -3013953.786711348221}
        })
    );

    NYT::IClientPtr client = NYT::NTesting::CreateTestClient();

    NYT::TTempTable inputYTTable(client);
    NYT::TTempTable outputYTTable(client);

    auto writer = client->CreateTableWriter<NYT::TNode>(inputYTTable.Name());
    writer->AddRow(goodBld.toYTNode());
    writer->AddRow(badBld.toYTNode());
    writer->Finish();


    NYT::ITransactionPtr txn = client->StartTransaction();

    inferenceAutoToloker(
        txn,
        inputYTTable.Name(),
        config,
        outputYTTable.Name(),
        EraseBatch::No
    );

    txn->Commit();


    auto reader = client->CreateTableReader<NYT::TNode>(outputYTTable.Name());
    for (; reader->IsValid(); reader->Next()) {
        const NYT::TNode& row = reader->GetRow();
        DetectionResult result = DetectionResult::fromYTNode(row);
        EXPECT_TRUE(result.bld == goodBld || result.bld == badBld);
        if (result.bld == goodBld) {
            EXPECT_EQ(result.state, TolokaState::Yes);
            EXPECT_TRUE(result.confidence >= config.threshold());
        } else {
            EXPECT_EQ(result.state, TolokaState::No);
            EXPECT_TRUE(result.confidence < config.threshold());
        }
    }
}

} // Y_UNIT_TEST_SUITE(inference_auto_toloker_tests)

} // namespace test

} // namespace maps::wiki::autocart::pipeline

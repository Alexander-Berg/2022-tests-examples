#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>

#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/objects/include/building.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/detection/include/state.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/detection/include/remove_rejected.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/config/include/config.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/storage/include/yt_storage.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/storage/include/detection_results.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/storage/include/tolokers_results.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/storage/include/assessors_results.h>

#include <mapreduce/yt/util/temp_table.h>

using namespace testing;

namespace maps::wiki::autocart::pipeline {

namespace tests {

namespace {

static const std::string CONFIG_TEMPLATE
    = "maps/wikimap/mapspro/services/autocart/pipeline/libs/detection/tests/configs/detector.json";

} // namespace

Y_UNIT_TEST_SUITE(remove_rejected_tests)
{

Y_UNIT_TEST(base_test)
{
    DetectorConfig config(BinaryPath(CONFIG_TEMPLATE));
    const TString STATE_PATH = "//home/state";
    const TString STORAGE_PATH = "//home/storage";
    const TString INPUT_YT_TABLE_PATH = "//home/input";
    const TString OUTPUT_YT_TABLE_PATH = "//home/output";
    NYT::IClientPtr client = NYT::NTesting::CreateTestClient();
    NYT::ITransactionPtr txn = client->StartTransaction();
    createYTStorage(txn, STORAGE_PATH);
    txn->Commit();
    State::initialize(client, STATE_PATH);
    const std::vector<DetectionResult> INPUT_RESULTS{
        {
            0,
            Building::fromMercatorGeom(
                geolib3::Polygon2({
                    {1000., 1000.},
                    {2000., 1000.},
                    {2000., 2000.},
                    {1000., 2000.}
                })
            ),
            TolokaState::Yes,
            0.9
        },
        {
            1,
            Building::fromMercatorGeom(
                geolib3::Polygon2({
                    {2000., 2000.},
                    {3000., 2000.},
                    {3000., 3000.},
                    {2000., 3000.}
                })
            ),
            TolokaState::Yes,
            0.9
        },
        {
            2,
            Building::fromMercatorGeom(
                geolib3::Polygon2({
                    {3000., 3000.},
                    {4000., 3000.},
                    {4000., 4000.},
                    {3000., 4000.}
                })
            ),
            TolokaState::Yes,
            0.9
        },
        {
            3,
            Building::fromMercatorGeom(
                geolib3::Polygon2({
                    {4000., 4000.},
                    {5000., 4000.},
                    {5000., 5000.},
                    {4000., 5000.}
                })
            ),
            TolokaState::Yes,
            0.9
        }
    };
    const TString REGION = "region";
    const uint64_t ISSUE_ID = 16;
    const std::vector<TolokersResult> TOLOKERS{
        {
            0,
            Building::fromMercatorGeom(
                geolib3::Polygon2({
                    {1000., 1000.},
                    {1900., 1000.},
                    {1900., 2000.},
                    {1000., 2000.}
                })
            ),
            {{"user-1", TolokaState::No}, {"user-2", TolokaState::No}},
            TolokaState::No,
            "task-1"
        },
        {
            1,
            Building::fromMercatorGeom(
                geolib3::Polygon2({
                    {2000., 2000.},
                    {2950., 2000.},
                    {2950., 3000.},
                    {2000., 3000.}
                })
            ),
            {{"user-1", TolokaState::No}, {"user-2", TolokaState::No}},
            TolokaState::No,
            "task-2"
        },
        {
            2,
            Building::fromMercatorGeom(
                geolib3::Polygon2({
                    {3000., 3000.},
                    {4000., 3000.},
                    {4000., 4000.},
                    {3000., 4000.}
                })
            ),
            {{"user-1", TolokaState::Yes}, {"user-2", TolokaState::Yes}},
            TolokaState::Yes,
            "task-3"
        },
    };
    const std::vector<AssessorsResult> ASSESSORS{
        {
            2,
            Building::fromMercatorGeom(
                geolib3::Polygon2({
                    {3000., 3000.},
                    {3500., 3000.},
                    {3500., 4000.},
                    {3000., 4000.}
                })
            ),
            TolokaState::No,
            "assessor-1",
            "task-1",
            "login-1"
        },
        {
            3,
            Building::fromMercatorGeom(
                geolib3::Polygon2({
                    {4000., 4000.},
                    {4900., 4000.},
                    {4900., 5000.},
                    {4000., 5000.}
                })
            ),
            TolokaState::No,
            "assessor-2",
            "task-2",
            "login-2"
        }

    };
    const std::vector<DetectionResult> EXPECTED_RESULTS{
        {
            2,
            Building::fromMercatorGeom(
                geolib3::Polygon2({
                    {3000., 3000.},
                    {4000., 3000.},
                    {4000., 4000.},
                    {3000., 4000.}
                })
            ),
            TolokaState::Yes,
            0.9
        }
    };

    YTStorageClient storage(client, STORAGE_PATH);
    NYT::TTableWriterPtr<NYT::TNode> writer
        = client->CreateTableWriter<NYT::TNode>(INPUT_YT_TABLE_PATH);
    for (const DetectionResult& result : INPUT_RESULTS) {
        writer->AddRow(result.toYTNode());
    }
    writer->Finish();
    storage.saveResults(REGION, ISSUE_ID, TOLOKERS);
    storage.saveResults(REGION, ISSUE_ID, ASSESSORS);

    removeRejectedBuildings(
        client,
        INPUT_YT_TABLE_PATH,
        config,
        storage,
        OUTPUT_YT_TABLE_PATH
    );

    std::vector<DetectionResult> results;
    NYT::TTableReaderPtr<NYT::TNode> reader
        = client->CreateTableReader<NYT::TNode>(OUTPUT_YT_TABLE_PATH);
    for (; reader->IsValid(); reader->Next()) {
        results.push_back(DetectionResult::fromYTNode(reader->GetRow()));
    }

    EXPECT_TRUE(
        std::is_permutation(
            results.begin(), results.end(),
            EXPECTED_RESULTS.begin()
        )
    );

    client->Remove(STORAGE_PATH, NYT::TRemoveOptions().Recursive(true).Force(true));
    State::remove(client);
}

} // Y_UNIT_TEST_SUITE(remove_rejected_tests)

} // namespace test

} // namespace maps::wiki::autocart::pipeline

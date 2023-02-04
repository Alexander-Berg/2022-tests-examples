#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>

#include <maps/libs/geolib/include/point.h>
#include <maps/libs/geolib/include/test_tools/comparison.h>

#include <maps/libs/common/include/file_utils.h>

#include <maps/wikimap/mapspro/services/autocart/libs/geometry/include/hex_wkb.h>

#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/storage/include/yt_storage.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/storage/include/publication_results.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/storage/include/tolokers_results.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/storage/include/assessors_results.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/storage/include/detection_results.h>

#include <maps/libs/chrono/include/days.h>

#include <chrono>

using namespace testing;

namespace maps::wiki::autocart::pipeline {

namespace tests {

namespace {

static const std::string JSON_DIR
    = "maps/wikimap/mapspro/services/autocart/pipeline/libs/storage/tests/json";

template <typename Result>
void testResults(
    YTStorageClient& storage,
    const TString& region, uint64_t issueId)
{
    std::vector<Result> results;
    json::Value resultsJson = json::Value::fromFile(
        BinaryPath(maps::common::joinPath(
            JSON_DIR, Result::getName() + "_" + region + "_" + ToString(issueId) + ".json"))
    );
    for (const json::Value& resultJson : resultsJson) {
        results.push_back(Result::fromJson(resultJson));
    };

    size_t prevCount = storage.getResultsCount<Result>();

    EXPECT_TRUE(!storage.isResultsExists<Result>(region, issueId));
    storage.saveResults(region, issueId, results);
    EXPECT_TRUE(storage.isResultsExists<Result>(region, issueId));

    size_t curCount = storage.getResultsCount<Result>();

    std::vector<Result> loadedResults;
    storage.getResults(region, issueId, &loadedResults);

    EXPECT_TRUE(
        std::is_permutation(
            results.begin(), results.end(),
            loadedResults.begin()
        )
    );
    EXPECT_EQ(curCount - prevCount, results.size());
}

} // namespace

Y_UNIT_TEST_SUITE(yt_storage_tests)
{

Y_UNIT_TEST(region_to_issue_id_map_test)
{
    const TString storagePath = "//home/storage";
    NYT::IClientPtr ytClient = NYT::NTesting::CreateTestClient();
    NYT::ITransactionPtr txn = ytClient->StartTransaction();
    createYTStorage(txn, storagePath);
    txn->Commit();
    YTStorageClient storage(ytClient, storagePath);

    // initial map should be empty
    std::unordered_map<TString, ProcessedIssue> regionToIssue = storage.getRegionToIssueMap();
    EXPECT_TRUE(regionToIssue.empty());

    // update issueId for first not exists region
    const TString region1 = "1";
    const uint64_t issue1Id = 11;
    storage.updateIssue(region1, issue1Id);

    regionToIssue = storage.getRegionToIssueMap();
    EXPECT_EQ(regionToIssue.size(), 1u);
    EXPECT_EQ(regionToIssue.begin()->first, region1);
    EXPECT_EQ(regionToIssue.begin()->second.issueId, issue1Id);
    chrono::TimePoint region1Date = regionToIssue.begin()->second.date;
    EXPECT_TRUE(
        std::chrono::duration_cast<std::chrono::minutes>(
            region1Date - chrono::TimePoint::clock::now()).count() < 2
    );

    // update issueId for second not exists region
    const TString region2 = "2";
    storage.updateIssue(region2, issue1Id);

    regionToIssue = storage.getRegionToIssueMap();
    EXPECT_EQ(regionToIssue.size(), 2u);
    auto it1 = regionToIssue.find(region1);
    EXPECT_TRUE(it1 != regionToIssue.end());
    EXPECT_EQ(it1->first, region1);
    EXPECT_EQ(it1->second.issueId, issue1Id);
    EXPECT_EQ(it1->second.date, region1Date);
    auto it2 = regionToIssue.find(region2);
    EXPECT_TRUE(it2 != regionToIssue.end());
    EXPECT_EQ(it2->first, region2);
    EXPECT_EQ(it2->second.issueId, issue1Id);
    chrono::TimePoint region2Date = it2->second.date;
    EXPECT_TRUE(
        std::chrono::duration_cast<std::chrono::minutes>(
            region2Date - chrono::TimePoint::clock::now()).count() < 2
    );

    // update issueId for first exists region
    const uint64_t issue2Id = 12;
    storage.updateIssue(region1, issue2Id);

    regionToIssue = storage.getRegionToIssueMap();
    EXPECT_EQ(regionToIssue.size(), 2u);
    it1 = regionToIssue.find(region1);
    EXPECT_TRUE(it1 != regionToIssue.end());
    EXPECT_EQ(it1->first, region1);
    EXPECT_EQ(it1->second.issueId, issue2Id);
    EXPECT_TRUE(it1->second.date > region1Date);
    EXPECT_TRUE(
        std::chrono::duration_cast<std::chrono::minutes>(
            it1->second.date - chrono::TimePoint::clock::now()).count() < 2
    );
    it2 = regionToIssue.find(region2);
    EXPECT_TRUE(it2 != regionToIssue.end());
    EXPECT_EQ(it2->first, region2);
    EXPECT_EQ(it2->second.issueId, issue1Id);
    EXPECT_EQ(it2->second.date, region2Date);

    ytClient->Remove(storagePath, NYT::TRemoveOptions().Recursive(true).Force(true));

    EXPECT_TRUE(!ytClient->Exists(storagePath));
}

Y_UNIT_TEST(results_test)
{
    const TString storagePath = "//home/storage";
    NYT::IClientPtr ytClient = NYT::NTesting::CreateTestClient();
    NYT::ITransactionPtr txn = ytClient->StartTransaction();
    createYTStorage(txn, storagePath);
    txn->Commit();
    YTStorageClient storage(ytClient, storagePath);

    // add detection results
    const TString region1 = "1";
    const uint64_t issue1Id = 1;
    testResults<DetectionResult>(storage, region1, issue1Id);

    // add detection results for same region and another issue id
    const uint64_t issue2Id = 2;
    testResults<DetectionResult>(storage, region1, issue2Id);

    // add tolokers results
    const TString region3 = "3";
    testResults<TolokersResult>(storage, region3, issue1Id);

    // add tolokers results for same region and another issue id
    testResults<TolokersResult>(storage, region3, issue2Id);

    // add assessors results
    const TString region4 = "4";
    testResults<AssessorsResult>(storage, region4, issue1Id);

    // add assessors results for same region and another issue id
    testResults<AssessorsResult>(storage, region4, issue2Id);

    // add publication results
    const TString region2 = "2";
    testResults<PublicationResult>(storage, region2, issue1Id);

    // add publication results for same region and another issue id
    testResults<PublicationResult>(storage, region2, issue2Id);

    ytClient->Remove(storagePath, NYT::TRemoveOptions().Recursive(true).Force(true));

    EXPECT_TRUE(!ytClient->Exists(storagePath));
}

Y_UNIT_TEST(execution_test)
{
    const TString storagePath = "//home/storage";
    NYT::IClientPtr ytClient = NYT::NTesting::CreateTestClient();
    NYT::ITransactionPtr txn = ytClient->StartTransaction();
    createYTStorage(txn, storagePath);
    txn->Commit();
    YTStorageClient storage(ytClient, storagePath);

    const geolib3::Polygon2 geoGeom1({{0., 0.}, {1., 0.}, {1., 1.}, {0., 1.}});
    const NYT::TNode REGION_NODE1 = NYT::TNode()
        ("bld_recognition_region:name", "name1")
        ("bld_recognition_region:shape", TString(polygonToHexWKB(geoGeom1)))
        ("bld_recognition_region:use_dwellplaces", false)
        ("bld_recognition_region:use_tolokers", true)
        ("bld_recognition_region:use_assessors", false)
        ("bld_recognition_region:is_active", true)
        ("bld_recognition_region:object_id", 7111917u)
        ("bld_recognition_region:commit_id", 8111917u);
    const BldRecognitionRegion region1 = BldRecognitionRegion::fromYTNode(REGION_NODE1);
    const uint64_t issueId1 = 1;

    const geolib3::Polygon2 geoGeom2({{1., 0.}, {2., 0.}, {2., 1.}, {1., 1.}});
    const NYT::TNode REGION_NODE2 = NYT::TNode()
        ("bld_recognition_region:name", "name2")
        ("bld_recognition_region:shape", TString(polygonToHexWKB(geoGeom2)))
        ("bld_recognition_region:use_dwellplaces", true)
        ("bld_recognition_region:use_tolokers", false)
        ("bld_recognition_region:use_assessors", true)
        ("bld_recognition_region:is_active", false)
        ("bld_recognition_region:object_id", 22061941u)
        ("bld_recognition_region:commit_id", 9051945u);
    const BldRecognitionRegion region2 = BldRecognitionRegion::fromYTNode(REGION_NODE2);
    const uint64_t issueId2 = 2;


    chrono::TimePoint tp1 = chrono::TimePoint::clock::now();
    storage.saveExecution(region1, issueId1);
    chrono::TimePoint tp2 = chrono::TimePoint::clock::now();
    storage.saveExecution(region2, issueId2);
    chrono::TimePoint tp3 = chrono::TimePoint::clock::now();

    std::vector<Execution> history = storage.getExecutionHistory();

    EXPECT_EQ(history.size(), 2u);

    EXPECT_EQ(history[0].region, region1);
    EXPECT_EQ(history[0].issueId, issueId1);
    EXPECT_TRUE(
        std::chrono::floor<std::chrono::seconds>(tp1) <=
        std::chrono::round<std::chrono::seconds>(history[0].date)
    );
    EXPECT_TRUE(
        std::chrono::round<std::chrono::seconds>(history[0].date) <=
        std::chrono::ceil<std::chrono::seconds>(tp2)
    );

    EXPECT_EQ(history[1].region, region2);
    EXPECT_EQ(history[1].issueId, issueId2);
    EXPECT_TRUE(
        std::chrono::floor<std::chrono::seconds>(tp2) <=
        std::chrono::round<std::chrono::seconds>(history[1].date)
    );
    EXPECT_TRUE(
        std::chrono::round<std::chrono::seconds>(history[1].date) <=
        std::chrono::ceil<std::chrono::seconds>(tp3)
    );

    ytClient->Remove(storagePath, NYT::TRemoveOptions().Recursive(true).Force(true));

    EXPECT_TRUE(!ytClient->Exists(storagePath));
}

Y_UNIT_TEST(satellite_release_test)
{
    const TString storagePath = "//home/storage";
    NYT::IClientPtr ytClient = NYT::NTesting::CreateTestClient();
    NYT::ITransactionPtr txn = ytClient->StartTransaction();
    createYTStorage(txn, storagePath);
    txn->Commit();
    YTStorageClient storage(ytClient, storagePath);

    EXPECT_TRUE(!storage.hasReleases());

    const Release RELEASE{1, 2};
    const ReleaseAttrs ATTRS{
        ReleaseGeometries{
            {
                10, 18, // zmin, zmax
                geolib3::MultiPolygon2({
                    geolib3::Polygon2({
                        {0., 0.},
                        {1000., 0.},
                        {1000., 1000.},
                        {0., 1000.}
                    })
                })
            },
            {
                12, 20, // zmin. zmax
                geolib3::MultiPolygon2({
                    geolib3::Polygon2({
                        {1000., 1000.},
                        {2000., 1000.},
                        {2000., 2000.},
                        {1000., 2000.}
                    })
                })
            }
        },
        chrono::TimePoint::clock::now()
    };

    storage.addRelease(RELEASE, ATTRS);

    EXPECT_TRUE(storage.hasReleases());

    std::map<Release, ReleaseAttrs> releaseToAttrs = storage.getAllReleasesAttrs();
    EXPECT_EQ(releaseToAttrs.size(), 1u);
    const Release& testRelease = releaseToAttrs.begin()->first;
    EXPECT_EQ(RELEASE, testRelease);
    const ReleaseAttrs& testAttrs = releaseToAttrs.begin()->second;
    EXPECT_EQ(ATTRS.geometries.size(), testAttrs.geometries.size());
    EXPECT_TRUE(std::chrono::duration_cast<std::chrono::minutes>(ATTRS.date - testAttrs.date).count() < 2);

    EXPECT_TRUE(
        std::is_permutation(
            ATTRS.geometries.begin(), ATTRS.geometries.end(),
            testAttrs.geometries.begin()
        )
    );

    // Failed to re-add release
    EXPECT_THROW(
        storage.addRelease(RELEASE, ATTRS),
        maps::Exception
    );

    EXPECT_EQ(storage.getLastRelease(), RELEASE);

    ytClient->Remove(storagePath, NYT::TRemoveOptions().Recursive(true).Force(true));

    EXPECT_TRUE(!ytClient->Exists(storagePath));
}

Y_UNIT_TEST(releases_coverage_test)
{
    const TString storagePath = "//home/storage";
    NYT::IClientPtr ytClient = NYT::NTesting::CreateTestClient();
    NYT::ITransactionPtr txn = ytClient->StartTransaction();
    createYTStorage(txn, storagePath);
    txn->Commit();
    YTStorageClient storage(ytClient, storagePath);
    const int zoom = 18;
    const ReleasesCoverage coverage{
        zoom,
        {123, 5456},
        geolib3::MultiPolygon2({
            geolib3::Polygon2({
                {0., 0.},
                {10000., 0.},
                {10000., 10000.},
                {0., 10000.}
            })
        })
    };

    EXPECT_TRUE(!storage.getCoverageAtZoom(zoom).has_value());

    storage.updateCoverage(coverage);
    std::optional<ReleasesCoverage> testCoverage = storage.getCoverageAtZoom(zoom);

    EXPECT_TRUE(testCoverage.has_value());
    EXPECT_EQ(coverage.z, testCoverage->z);
    EXPECT_EQ(coverage.lastRelease.releaseId, testCoverage->lastRelease.releaseId);
    EXPECT_EQ(coverage.lastRelease.issueId, testCoverage->lastRelease.issueId);
    EXPECT_TRUE(
        geolib3::test_tools::approximateEqual(
            coverage.mercatorGeom, testCoverage->mercatorGeom,
            geolib3::EPS
        )
    );

    // Failed to re-update coverage
    EXPECT_THROW(
        storage.updateCoverage(coverage),
        maps::Exception
    );

    ytClient->Remove(storagePath, NYT::TRemoveOptions().Recursive(true).Force(true));

    EXPECT_TRUE(!ytClient->Exists(storagePath));
}

Y_UNIT_TEST(update_coverage_test)
{
    const TString storagePath = "//home/storage";
    NYT::IClientPtr ytClient = NYT::NTesting::CreateTestClient();
    NYT::ITransactionPtr txn = ytClient->StartTransaction();
    createYTStorage(txn, storagePath);
    txn->Commit();
    YTStorageClient storage(ytClient, storagePath);
    const int zoom = 18;
    const ReleasesCoverage coverage{
        zoom,
        {123, 5456},
        geolib3::MultiPolygon2({
            geolib3::Polygon2({
                {0., 0.},
                {10000., 0.},
                {10000., 10000.},
                {0., 10000.}
            })
        })
    };

    const std::map<Release, ReleaseAttrs> releaseToAttrs{
        {
            {10000, 10000},
            {
                ReleaseGeometries{
                    {
                        10, 18, // zmin, zmax
                        geolib3::MultiPolygon2({
                            geolib3::Polygon2({
                                {0., 10000.},
                                {10000., 10000.},
                                {10000., 20000.},
                                {0., 20000.}
                            })
                        })
                    },
                },
                chrono::TimePoint::clock::now()
            }
        }
    };

    EXPECT_TRUE(!storage.getCoverageAtZoom(zoom).has_value());

    storage.updateCoverage(coverage);
    storage.updateCoverage(releaseToAttrs, zoom);

    std::optional<ReleasesCoverage> testCoverage = storage.getCoverageAtZoom(zoom);

    EXPECT_TRUE(testCoverage.has_value());
    EXPECT_EQ(zoom, testCoverage->z);
    EXPECT_EQ(releaseToAttrs.begin()->first.releaseId, testCoverage->lastRelease.releaseId);
    EXPECT_EQ(releaseToAttrs.begin()->first.issueId, testCoverage->lastRelease.issueId);

    EXPECT_TRUE(
        geolib3::test_tools::approximateEqual(
            geolib3::MultiPolygon2({
                geolib3::Polygon2({
                    {10000.0, 10000.0},
                    {10000.0, 0.0},
                    {0.0, 0.0},
                    {0.0, 10000.0},
                    {0.0, 20000.0},
                    {10000.0, 20000.0}
                })
            }),
            testCoverage->mercatorGeom,
            geolib3::EPS
        )
    );

    ytClient->Remove(storagePath, NYT::TRemoveOptions().Recursive(true).Force(true));

    EXPECT_TRUE(!ytClient->Exists(storagePath));
}

} // Y_UNIT_TEST_SUITE(yt_storage_tests)

} // namespace test

} // namespace maps::wiki::autocart::pipeline

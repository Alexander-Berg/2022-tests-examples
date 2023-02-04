#include <maps/factory/services/backend/tests/test_utils.h>
#include <maps/factory/services/backend/lib/modify_dem_patch.h>

#include <maps/factory/libs/db/dem_patch_gateway.h>
#include <maps/factory/libs/db/dem_release_gateway.h>
#include <maps/factory/libs/common/eigen.h>
#include <maps/factory/libs/common/json.h>
#include <maps/factory/libs/common/s3_keys.h>
#include <maps/factory/libs/unittest/tests_common.h>
#include <maps/factory/libs/dataset/dem_tile_dataset.h>
#include <maps/factory/libs/geometry/transformation.h>
#include <maps/factory/libs/geometry/geolib.h>
#include <maps/factory/libs/sproto_helpers/dem_release.h>
#include <maps/factory/libs/storage/local_storage.h>
#include <maps/factory/libs/storage/s3_storage.h>

#include <gmock/gmock.h>
#include <util/system/env.h>

namespace maps::factory::backend::tests {

namespace {

using namespace sproto_helpers;
using namespace testing;
using namespace geometry;
using namespace image;

const auto fixGdalData = maps::factory::tests::fixGdalDataFolderForTests();
const auto fixDemData = maps::factory::tests::changeGlobalDemPath();

class DemFixture : public BackendFixture {
public:
    DemFixture()
        : testDemRelease_(createTestDemRelease())
        , testDemPatch_(createTestDemPatch())
    {
        auto txn = txnHandle();
        db::DemReleaseGateway(*txn).insert(testDemRelease_);
        testDemPatch_.setReleaseId(testDemRelease_.id());
        db::DemPatchGateway(*txn).insert(testDemPatch_);
        txn->commit();
    }

    const db::DemRelease& testDemRelease() { return testDemRelease_; }

    const db::DemPatch& testDemPatch() { return testDemPatch_; }

    db::DemPatch loadPatch(db::Id id) { return db::DemPatchGateway(*txnHandle()).loadById(id); }

    db::DemPatch loadPatch() { return loadPatch(testDemPatch_.id()); }

    void changeTestReleaseStatus(const db::DemReleaseStatus status)
    {
        auto txn = txnHandle();
        testDemRelease_.setStatus(status);
        db::DemReleaseGateway(*txn).update(testDemRelease_);
        txn->commit();
    }

    void checkTestReleaseStatus(const db::DemReleaseStatus status)
    {
        const auto release = db::DemReleaseGateway(*txnHandle())
            .loadById(testDemRelease_.id());
        EXPECT_EQ(release.status(), status);
    }

    void checkResultTile(
        const db::DemPatch& patch,
        const tile::Tile& tile,
        const MultiPoint3d& points)
    {
        ASSERT_TRUE(patch.hasTiles() && patch.tiles()->count(tile));
        dataset::MultiPoint3dDataset ds(&points, Shared<dataset::TEmptyBlockCache>(), tile.z());
        const auto expected = ds.readTile(tile);
        
        const auto version = patch.tiles()->at(tile);
        const auto patchPath = getDemPatchPath(
            configuration()->demPatches(), patch.id());
        storage::S3Pool s3Pool(configuration()->config().s3Auth());
        auto storage = storage::TileStorage(
            s3Pool.storage(patchPath), TILE_FILE_PATTERN, version);
        const dataset::Int16Image result =
                dataset::readDemTileFromStorage(tile, storage);
        EXPECT_TRUE((result.array() == expected.array()).all());
    }

    void prepareGetElevationData()
    {
        auto txn = txnHandle();
        auto patch = testDemPatch();
        const CoordinateTransformation geoToMerc(geodeticSr(), mercatorSr());
        const auto patchBox = geoToMerc * boxFromPoints(30.0, 30.0, 40.0, 40.0);
        patch.setBbox(patchBox);
        db::DemPatchGateway(*txn).update(patch);
        auto release = db::DemRelease("other_release")
            .setStatus(db::DemReleaseStatus::Production)
            .setIssueId(1);
        db::DemReleaseGateway(*txn).insert(release);
        auto otherPatch = createTestDemPatch(release.id());
        otherPatch.setBbox(patchBox);
        db::DemPatchGateway(*txn).insert(otherPatch);
        txn->commit();
    }

    void replaceDemPatches(const std::string& path)
    {
        auto jsonConfig = configuration()->config().json();
        json::Builder builder;
        builder << [&](json::ObjectBuilder builder) {
            builder["dem"] << [&](json::ObjectBuilder builder) {
                builder["patches"] << path;
            };
        };
        addOrReplaceJsonData(jsonConfig, json::Value::fromString(builder.str()));
        reInitConfiguration({jsonConfig});
    }

private:
    db::DemRelease testDemRelease_;
    db::DemPatch testDemPatch_;
};

storage::S3Ptr setUpLocalPatchStorage(const std::string& bucket)
{
    const std::string port = GetEnv("S3MDS_PORT");
    REQUIRE(!port.empty(), "Cannot connect to local S3 server");
    const std::string testEndpoint = "http://localhost:" + port;
    const auto testS3Auth = storage::S3CloudStorage::keyAuth(
        {.accessKeyId = "1234567890", .secretKey = "abcdefabcdef"});
    auto s3 = storage::s3Storage(testEndpoint, bucket, "", testS3Auth);
    s3->createBucket();
    const auto demPatches = configuration()->demPatches();
    auto patchesDir = storage::localStorage(demPatches);
    patchesDir->copyAll(*s3);
    return s3;
}

using dem_api_should = DemFixture;

http::URL makeUrlDem(const std::string& handle)
{
    return http::URL("http://localhost/v1/dem/" + handle);
}

std::string boxParam(const Box2d& bbox)
{
    return std::to_string(bbox.min().x()) + "," + std::to_string(bbox.min().y()) + "~" +
           std::to_string(bbox.max().x()) + "," + std::to_string(bbox.max().y());
}

Box2d toGeo(const Box2d& bbox)
{
    return toBox2d(geolib3::convertMercatorToGeodetic(toGeolibBox(bbox)));
}

void testReleaseStateTransition(
    pgpool3::TransactionHandle& txn,
    db::DemRelease release,
    const db::DemReleaseStatus from,
    const db::DemReleaseStatus to,
    const int64_t status = 200)
{
    release.setStatus(from);
    db::DemReleaseGateway(*txn).update(release);
    txn->commit();

    sdem::DemReleaseUpdateData updateData;
    updateData.targetStatus() = convertToSproto(to);

    http::MockRequest request(
        http::POST,
        makeUrlDem("releases/update")
            .addParam("id", std::to_string(release.id()))
    );
    request.body = boost::lexical_cast<std::string>(updateData);
    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, status)
                    << "Got status " << std::to_string(response.status)
                        << " while expecting " << std::to_string(status)
                        << " during the transition from " << from << " to " << to;
}

void testDeleteReleaseFailed(const db::Id releaseId)
{
    http::MockRequest request(
        http::DELETE,
        makeUrlDem("releases/delete")
            .addParam("id", std::to_string(releaseId))
    );
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 400);
}

MultiPoint3d diskPoints(const int val = 142)
{
    return {
        {4077227.5257595889, 4482802.3978407308, val},
        {4077380.3998161592, 4482649.5237841606, val},
        {4076921.7776464485, 4482496.6497275904, val},
        {4077074.6517030187, 4482802.3978407308, val},
        {4077074.6517030187, 4482955.2718973029, val},
        {4076921.7776464485, 4482649.5237841606, val},
        {4077227.5257595889, 4482496.6497275904, val},
        {4076768.9035898782, 4482649.5237841606, val},
        {4077227.5257595889, 4482649.5237841606, val},
        {4077074.6517030187, 4482343.7756710202, val},
        {4077074.6517030187, 4482496.6497275904, val},
        {4076921.7776464485, 4482802.3978407308, val},
        {4077074.6517030187, 4482649.5237841606, val},
    };
}

} // namespace

TEST_F(dem_api_should, get_elevation_range)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    prepareGetElevationData();
    http::MockRequest request(http::GET, makeUrlDem("get_elevation_range")
        .addParam("bbox", "36.9734343,37.4976192~36.9755036,37.4959734")
    );
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    auto sprotoResponse = boost::lexical_cast<sdem::DemElevationRange>(response.body);
    EXPECT_EQ(sprotoResponse.min(), 668);
    EXPECT_EQ(sprotoResponse.max(), 684);
}

TEST_F(dem_api_should, get_elevation)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    prepareGetElevationData();
    http::MockRequest request(http::GET, makeUrlDem("get_elevation")
        .addParam("ll", "36.975233,37.497449")
    );
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    auto sprotoResponse = boost::lexical_cast<sdem::DemElevation>(response.body);
    EXPECT_EQ(sprotoResponse.value(), 678);
}

TEST_F(dem_api_should, get_elevation_range_with_patch)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    prepareGetElevationData();
    http::MockRequest request(http::GET, makeUrlDem("get_elevation_range")
        .addParam("bbox", "36.8262,37.3457~36.9141,37.4158")
        .addParam("release_id", testDemRelease().id())
        .addParam("patch_ids", testDemPatch().id())
    );

    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    auto sprotoResponse = boost::lexical_cast<sdem::DemElevationRange>(response.body);
    EXPECT_EQ(sprotoResponse.min(), 30);
    EXPECT_EQ(sprotoResponse.max(), 775);
}

TEST_F(dem_api_should, get_elevation_with_patch)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    prepareGetElevationData();
    http::MockRequest request(http::GET, makeUrlDem("get_elevation")
        .addParam("ll", "36.8431010,37.5882665")
        .addParam("release_id", testDemRelease().id())
        .addParam("patch_ids", testDemPatch().id())
    );
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    auto sprotoResponse = boost::lexical_cast<sdem::DemElevation>(response.body);
    EXPECT_EQ(sprotoResponse.value(), 80);
}

TEST_F(dem_api_should, check_release_id_in_get_elevation_range)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));
    http::MockRequest request(http::GET, makeUrlDem("get_elevation_range")
        .addParam("bbox", "36.9734343,37.4976192~36.9755036,37.4959734")
        .addParam("release_id", testDemRelease().id() + 1)
    );
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 400);
}

TEST_F(dem_api_should, check_release_id_in_get_elevation)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    http::MockRequest request(http::GET, makeUrlDem("get_elevation")
        .addParam("ll", "36.975233,37.497449")
        .addParam("release_id", testDemRelease().id() + 1)
    );
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 400);
}

TEST_F(dem_api_should, prevent_patch_update_on_production_release)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));
    {
        http::MockRequest request(http::POST, makeUrlDem("draw")
            .addParam("ll", "0,0")
            .addParam("radius", "0")
            .addParam("elevation", 0)
            .addParam("patch_id", testDemPatch().id())
        );
        auto response = yacare::performTestRequest(request);
        ASSERT_EQ(response.status, 400);
    }
    {
        http::MockRequest request(http::POST, makeUrlDem("erase")
            .addParam("bbox", "-180,-90~180,90")
            .addParam("patch_id", testDemPatch().id())
        );
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 400);
    }
    {
        sdem::DemPatchCreateData createData;
        createData.releaseId() = std::to_string(testDemRelease().id());
        createData.name() = DEM_PATCH_NAME_U;
        createData.description() = DEM_PATCH_DESCRIPTION_U;

        http::MockRequest request(http::POST, makeUrlDem("patches/create"));
        request.body = boost::lexical_cast<std::string>(createData);
        auto response = yacare::performTestRequest(request);
        ASSERT_EQ(response.status, 400);
    }
    {
        sdem::DemPatchUpdateData updateData;
        updateData.name() = DEM_PATCH_NAME_U;
        updateData.description() = DEM_PATCH_DESCRIPTION_U;

        http::MockRequest request(
            http::POST,
            makeUrlDem("patches/update")
                .addParam("id", testDemPatch().id())
        );
        request.body = boost::lexical_cast<std::string>(updateData);
        auto response = yacare::performTestRequest(request);
        ASSERT_EQ(response.status, 400);
    }
}

TEST_F(dem_api_should, draw_point_in_empty_patch)
{
    auto s3 = setUpLocalPatchStorage(
        ::testing::UnitTest::GetInstance()->current_test_info()->name());
    replaceDemPatches(s3->absPath().native());
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));
    changeTestReleaseStatus(db::DemReleaseStatus::New);

    db::DemPatch emptyPatch("empty_patch", testDemRelease().id());
    {
        auto txn = txnHandle();
        db::DemPatchGateway(*txn).insert(emptyPatch);
        txn->commit();
    }

    const int val = 142;
    MultiPoint3d corrections{{4077074.6517030186, 4482649.5237841606, val}};
    http::MockRequest request(http::POST, makeUrlDem("draw")
        .addParam("ll", "36.6249847,37.4973686")
        .addParam("radius", "0")
        .addParam("elevation", val)
        .addParam("patch_id", emptyPatch.id())
    );
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    const auto result = loadPatch(emptyPatch.id());
    ASSERT_TRUE(result.hasBbox());
    EXPECT_DOUBLE_EQ(result.bbox()->min().x(), 4076998.2146747299);
    EXPECT_DOUBLE_EQ(result.bbox()->max().x(), 4077151.0887313001);
    EXPECT_DOUBLE_EQ(result.bbox()->min().y(), 4482573.0867558755);
    EXPECT_DOUBLE_EQ(result.bbox()->max().y(), 4482725.9608124457);
    EXPECT_GT(result.version(), emptyPatch.version());
    ASSERT_TRUE(result.hasTiles());
    EXPECT_EQ(result.tiles()->size(), 1u);
    checkResultTile(result, DEM_PATCH_TILE, corrections);
}

TEST_F(dem_api_should, draw_point)
{
    auto s3 = setUpLocalPatchStorage(
        ::testing::UnitTest::GetInstance()->current_test_info()->name());
    replaceDemPatches(s3->absPath().native());
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));
    changeTestReleaseStatus(db::DemReleaseStatus::New);

    const int val = 42;
    http::MockRequest request(http::POST, makeUrlDem("draw")
        .addParam("ll", "36.6249847,37.4973686")
        .addParam("radius", "0")
        .addParam("elevation", val)
        .addParam("patch_id", testDemPatch().id())
    );
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    const auto result = loadPatch(testDemPatch().id());
    ASSERT_TRUE(result.hasBbox());
    EXPECT_DOUBLE_EQ(result.bbox()->min().x(), DEM_BBOX.min().x());
    EXPECT_DOUBLE_EQ(result.bbox()->max().x(), DEM_BBOX.max().x());
    EXPECT_DOUBLE_EQ(result.bbox()->min().y(), DEM_BBOX.min().y());
    EXPECT_DOUBLE_EQ(result.bbox()->max().y(), DEM_BBOX.max().y());
    EXPECT_GT(result.version(), testDemPatch().version());
    ASSERT_TRUE(result.hasTiles());
    EXPECT_EQ(result.tiles()->size(), 1u);
    
    MultiPoint3d corrections = geometry::MultiPoint3d::fromWktFile(factory::tests::TEST_PATCH1);
    corrections.emplace_back(4077074.6517030186, 4482649.5237841606, val);
    checkResultTile(result, DEM_PATCH_TILE, corrections);
}

TEST_F(dem_api_should, draw_disk)
{
    auto s3 = setUpLocalPatchStorage(
        ::testing::UnitTest::GetInstance()->current_test_info()->name());
    replaceDemPatches(s3->absPath().native());
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));
    changeTestReleaseStatus(db::DemReleaseStatus::New);

    const int val = 142;
    http::MockRequest request(http::POST, makeUrlDem("draw")
        .addParam("ll", "36.6249847,37.4973686")
        .addParam("radius", "300")
        .addParam("elevation", val)
        .addParam("patch_id", testDemPatch().id())
    );
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    const auto result = loadPatch(testDemPatch().id());
    ASSERT_TRUE(result.hasBbox());
    EXPECT_DOUBLE_EQ(result.bbox()->min().x(), DEM_BBOX.min().x());
    EXPECT_DOUBLE_EQ(result.bbox()->max().x(), DEM_BBOX.max().x());
    EXPECT_DOUBLE_EQ(result.bbox()->min().y(), DEM_BBOX.min().y());
    EXPECT_DOUBLE_EQ(result.bbox()->max().y(), DEM_BBOX.max().y());
    EXPECT_GT(result.version(), testDemPatch().version());
    ASSERT_TRUE(result.hasTiles());
    EXPECT_EQ(result.tiles()->size(), 1u);

    MultiPoint3d corrections = geometry::MultiPoint3d::fromWktFile(factory::tests::TEST_PATCH1);
    const MultiPoint3d newPoints = diskPoints();
    corrections.insert(corrections.end(), newPoints.begin(), newPoints.end());
    checkResultTile(result, DEM_PATCH_TILE, corrections);
}

TEST_F(dem_api_should, draw_same_disk_many_times)
{
    auto s3 = setUpLocalPatchStorage(
        ::testing::UnitTest::GetInstance()->current_test_info()->name());
    replaceDemPatches(s3->absPath().native());
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));
    changeTestReleaseStatus(db::DemReleaseStatus::New);

    const int val = 142;
    http::MockRequest request(http::POST, makeUrlDem("draw")
        .addParam("ll", "36.6249847,37.4973686")
        .addParam("radius", "300")
        .addParam("elevation", val)
        .addParam("patch_id", testDemPatch().id())
    );
    auto response1 = yacare::performTestRequest(request);
    ASSERT_EQ(response1.status, 200);
    auto response2 = yacare::performTestRequest(request);
    ASSERT_EQ(response2.status, 200);
    auto response3 = yacare::performTestRequest(request);
    ASSERT_EQ(response3.status, 200);

    const auto result = loadPatch(testDemPatch().id());
    ASSERT_TRUE(result.hasBbox());
    EXPECT_DOUBLE_EQ(result.bbox()->min().x(), DEM_BBOX.min().x());
    EXPECT_DOUBLE_EQ(result.bbox()->max().x(), DEM_BBOX.max().x());
    EXPECT_DOUBLE_EQ(result.bbox()->min().y(), DEM_BBOX.min().y());
    EXPECT_DOUBLE_EQ(result.bbox()->max().y(), DEM_BBOX.max().y());
    EXPECT_GT(result.version(), testDemPatch().version());
    ASSERT_TRUE(result.hasTiles());
    EXPECT_EQ(result.tiles()->size(), 1u);

    MultiPoint3d corrections = geometry::MultiPoint3d::fromWktFile(factory::tests::TEST_PATCH1);
    const MultiPoint3d newPoints = diskPoints();
    corrections.insert(corrections.end(), newPoints.begin(), newPoints.end());
    checkResultTile(result, DEM_PATCH_TILE, corrections);
}

TEST_F(dem_api_should, draw_different_disk_many_times)
{
    auto s3 = setUpLocalPatchStorage(
        ::testing::UnitTest::GetInstance()->current_test_info()->name());
    replaceDemPatches(s3->absPath().native());
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));
    changeTestReleaseStatus(db::DemReleaseStatus::New);

    const int val = 142;
    for (size_t i = 0; i != 3; ++i) {
        http::MockRequest request(http::POST, makeUrlDem("draw")
            .addParam("ll", "36.6249847,37.4973686")
            .addParam("radius", "300")
            .addParam("elevation", val + i)
            .addParam("patch_id", testDemPatch().id())
        );
        auto response = yacare::performTestRequest(request);
        ASSERT_EQ(response.status, 200);

        const auto result = loadPatch(testDemPatch().id());
        ASSERT_TRUE(result.hasBbox());
        EXPECT_DOUBLE_EQ(result.bbox()->min().x(), DEM_BBOX.min().x());
        EXPECT_DOUBLE_EQ(result.bbox()->max().x(), DEM_BBOX.max().x());
        EXPECT_DOUBLE_EQ(result.bbox()->min().y(), DEM_BBOX.min().y());
        EXPECT_DOUBLE_EQ(result.bbox()->max().y(), DEM_BBOX.max().y());
        EXPECT_GT(result.version(), testDemPatch().version());
        ASSERT_TRUE(result.hasTiles());
        EXPECT_EQ(result.tiles()->size(), 1u);

        MultiPoint3d corrections = geometry::MultiPoint3d::fromWktFile(factory::tests::TEST_PATCH1);
        const MultiPoint3d newPoints = diskPoints(val + i);
        corrections.insert(corrections.end(), newPoints.begin(), newPoints.end());
        checkResultTile(result, DEM_PATCH_TILE, corrections);
    }
}

TEST_F(dem_api_should, erase_all_points)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));
    changeTestReleaseStatus(db::DemReleaseStatus::New);

    http::MockRequest request(http::POST, makeUrlDem("erase")
        .addParam("bbox", "-180,-90~180,90")
        .addParam("patch_id", testDemPatch().id())
    );
    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 200);

    const auto result = loadPatch(testDemPatch().id());
    EXPECT_FALSE(result.hasTiles());
    EXPECT_FALSE(result.hasBbox());
    EXPECT_GT(result.version(), testDemPatch().version());
}

TEST_F(dem_api_should, erase_one_point)
{
    auto s3 = setUpLocalPatchStorage(
        ::testing::UnitTest::GetInstance()->current_test_info()->name());
    replaceDemPatches(s3->absPath().native());
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));
    changeTestReleaseStatus(db::DemReleaseStatus::New);

    http::MockRequest request(http::POST, makeUrlDem("erase")
        .addParam("bbox", "36.8431010,37.5882665~36.8431010,37.5882665")
        .addParam("patch_id", testDemPatch().id())
    );
    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 200);

    const auto result = loadPatch();
    ASSERT_TRUE(result.hasBbox());
    EXPECT_GT(result.version(), testDemPatch().version());
    MultiPoint3d corrections = geometry::MultiPoint3d::fromWktFile(factory::tests::TEST_PATCH1);
    corrections.pop_back();
    checkResultTile(result, DEM_PATCH_TILE, corrections);
}

TEST_F(dem_api_should, erase_tiles_fully_in_bbox)
{
    auto s3 = setUpLocalPatchStorage(
        ::testing::UnitTest::GetInstance()->current_test_info()->name());
    replaceDemPatches(s3->absPath().native());
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));
    changeTestReleaseStatus(db::DemReleaseStatus::New);

    const CoordinateTransformation merToGeo(mercatorSr(), geodeticSr());
    const auto bboxMin = merToGeo * (DEM_BBOX.min() - Vector2d(1000, -1000));
    const auto bboxMax = merToGeo * (DEM_BBOX.max() + Vector2d(1000, -1000));

    http::MockRequest request(http::POST, makeUrlDem("erase")
        .addParam("bbox", boxParam({bboxMin, bboxMax}))
        .addParam("patch_id", testDemPatch().id())
    );
    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 200);

    const auto result = loadPatch();
    EXPECT_FALSE(result.hasBbox());
    EXPECT_FALSE(result.hasTiles());
}

TEST_F(dem_api_should, erase_empty_border_tiles)
{
    auto s3 = setUpLocalPatchStorage(
        ::testing::UnitTest::GetInstance()->current_test_info()->name());
    replaceDemPatches(s3->absPath().native());
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));
    changeTestReleaseStatus(db::DemReleaseStatus::New);

    const CoordinateTransformation merToGeo(mercatorSr(), geodeticSr());
    const auto bboxMin = merToGeo * (DEM_BBOX.min() + Vector2d(1000, -1000));
    const auto bboxMax = merToGeo * (DEM_BBOX.max() + Vector2d(1000, -1000));

    http::MockRequest request(http::POST, makeUrlDem("erase")
        .addParam("bbox", boxParam({bboxMin, bboxMax}))
        .addParam("patch_id", testDemPatch().id())
    );
    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 200);

    const auto result = loadPatch();
    EXPECT_FALSE(result.hasBbox());
    EXPECT_FALSE(result.hasTiles());
}

TEST_F(dem_api_should, test_dem_releases_api)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    http::MockRequest request(http::GET, makeUrlDem("releases"));
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    auto responseReleases = boost::lexical_cast<sdem::DemReleases>(response.body);
    ASSERT_EQ(responseReleases.releases().size(), 1u);

    auto responseRelease = responseReleases.releases().at(0);
    EXPECT_EQ(responseRelease.name(), DEM_RELEASE_NAME);
    EXPECT_EQ(*responseRelease.description(), DEM_RELEASE_DESCRIPTION);
    EXPECT_EQ(*responseRelease.currentStatus(), convertToSproto(DEM_STATUS));
}

TEST_F(dem_api_should, test_dem_releases_api_get)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    http::MockRequest request(
        http::GET,
        makeUrlDem("releases/get")
            .addParam("id", std::to_string(testDemRelease().id()))
    );
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    auto responseRelease = boost::lexical_cast<sdem::DemRelease>(response.body);
    EXPECT_EQ(responseRelease.name(), DEM_RELEASE_NAME);
    EXPECT_EQ(*responseRelease.description(), DEM_RELEASE_DESCRIPTION);
    EXPECT_EQ(*responseRelease.currentStatus(), convertToSproto(DEM_STATUS));
}

TEST_F(dem_api_should, test_dem_releases_api_search)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));
    {
        http::MockRequest request(
            http::GET,
            makeUrlDem("releases/search")
                .addParam("text", "wrong_query")
        );
        auto response = yacare::performTestRequest(request);
        ASSERT_EQ(response.status, 200);
        auto responseReleases = boost::lexical_cast<sdem::DemReleases>(response.body);
        EXPECT_EQ(responseReleases.releases().size(), 0u);
    }
    {
        http::MockRequest request(
            http::GET,
            makeUrlDem("releases/search")
                .addParam("text", "test_dem")
        );
        auto response = yacare::performTestRequest(request);
        ASSERT_EQ(response.status, 200);
        auto responseReleases = boost::lexical_cast<sdem::DemReleases>(response.body);
        EXPECT_EQ(responseReleases.releases().size(), 1u);
    }
    {
        const auto bbox = toGeo(DEM_BBOX);
        http::MockRequest request(
            http::GET,
            makeUrlDem("releases/search")
                .addParam("bbox", boxParam(bbox))
        );
        auto response = yacare::performTestRequest(request);
        ASSERT_EQ(response.status, 200);
        auto responseReleases = boost::lexical_cast<sdem::DemReleases>(response.body);
        EXPECT_EQ(responseReleases.releases().size(), 1u);
    }
    {
        const auto bboxSide = DEM_BBOX.max().x() - DEM_BBOX.min().x();
        const auto shift = Vector2d::Constant(2 * bboxSide);
        auto bbox = toGeo(shifted(DEM_BBOX, shift));
        http::MockRequest request(
            http::GET,
            makeUrlDem("releases/search")
                .addParam("bbox", boxParam(bbox))
        );
        auto response = yacare::performTestRequest(request);
        ASSERT_EQ(response.status, 200);
        auto responseReleases = boost::lexical_cast<sdem::DemReleases>(response.body);
        EXPECT_EQ(responseReleases.releases().size(), 0u);
    }
    {
        http::MockRequest request(
            http::GET,
            makeUrlDem("releases/search")
                .addParam("status", "new")
        );
        auto response = yacare::performTestRequest(request);
        ASSERT_EQ(response.status, 200);
        auto responseReleases = boost::lexical_cast<sdem::DemReleases>(response.body);
        EXPECT_EQ(responseReleases.releases().size(), 0u);
    }
    {
        http::MockRequest request(
            http::GET,
            makeUrlDem("releases/search")
                .addParam("status", "production")
        );
        auto response = yacare::performTestRequest(request);
        ASSERT_EQ(response.status, 200);
        auto responseReleases = boost::lexical_cast<sdem::DemReleases>(response.body);
        EXPECT_EQ(responseReleases.releases().size(), 1u);
    }
}

TEST_F(dem_api_should, test_dem_releases_api_create)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    sdem::DemReleaseCreateData createData;
    createData.name() = DEM_RELEASE_NAME_U;
    createData.description() = DEM_RELEASE_DESCRIPTION_U;

    http::MockRequest request(http::POST, makeUrlDem("releases/create"));
    request.body = boost::lexical_cast<std::string>(createData);
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    auto responseRelease = boost::lexical_cast<sdem::DemRelease>(response.body);
    EXPECT_NE(responseRelease.id(), std::to_string(testDemRelease().id()));
    EXPECT_EQ(responseRelease.name(), DEM_RELEASE_NAME_U);
    EXPECT_EQ(*responseRelease.description(), DEM_RELEASE_DESCRIPTION_U);
    EXPECT_EQ(*responseRelease.currentStatus(), convertToSproto(DEM_STATUS_U));

    auto release = db::DemReleaseGateway(*txnHandle())
        .loadById(db::parseId(responseRelease.id()));
    EXPECT_EQ(release.name(), DEM_RELEASE_NAME_U);
    EXPECT_EQ(release.description(), DEM_RELEASE_DESCRIPTION_U);
    EXPECT_EQ(release.status(), DEM_STATUS_U);
}

TEST_F(dem_api_should, test_dem_releases_api_update)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    sdem::DemReleaseUpdateData updateData;
    updateData.name() = DEM_RELEASE_NAME_U;
    updateData.description() = DEM_RELEASE_DESCRIPTION_U;
    updateData.targetStatus() = convertToSproto(DEM_STATUS_U);

    http::MockRequest request(
        http::POST,
        makeUrlDem("releases/update")
            .addParam("id", std::to_string(testDemRelease().id()))
    );
    request.body = boost::lexical_cast<std::string>(updateData);
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    auto responseRelease = boost::lexical_cast<sdem::DemRelease>(response.body);
    EXPECT_EQ(responseRelease.id(), std::to_string(testDemRelease().id()));
    EXPECT_EQ(responseRelease.name(), DEM_RELEASE_NAME_U);
    EXPECT_EQ(*responseRelease.description(), DEM_RELEASE_DESCRIPTION_U);

    auto release = db::DemReleaseGateway(*txnHandle())
        .loadById(db::parseId(responseRelease.id()));
    EXPECT_EQ(release.name(), DEM_RELEASE_NAME_U);
    EXPECT_EQ(release.description(), DEM_RELEASE_DESCRIPTION_U);
}

TEST_F(dem_api_should, test_dem_releases_api_simple_transitions)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    auto txn = txnHandle();
    testReleaseStateTransition(txn, testDemRelease(),
        db::DemReleaseStatus::MovingToProduction, db::DemReleaseStatus::Production);
    checkTestReleaseStatus(db::DemReleaseStatus::MovingToProduction);

    txn = txnHandle();
    testReleaseStateTransition(txn, testDemRelease(),
        db::DemReleaseStatus::MovingToProduction, db::DemReleaseStatus::New);
    checkTestReleaseStatus(db::DemReleaseStatus::RollbackProductionToNew);

    txn = txnHandle();
    testReleaseStateTransition(txn, testDemRelease(),
        db::DemReleaseStatus::RollbackProductionToNew, db::DemReleaseStatus::New);
    checkTestReleaseStatus(db::DemReleaseStatus::RollbackProductionToNew);

    txn = txnHandle();
    testReleaseStateTransition(txn, testDemRelease(),
        db::DemReleaseStatus::New, db::DemReleaseStatus::Production);
    checkTestReleaseStatus(db::DemReleaseStatus::MovingToProduction);

    txn = txnHandle();
    testReleaseStateTransition(txn, testDemRelease(),
        db::DemReleaseStatus::Production, db::DemReleaseStatus::New);
    checkTestReleaseStatus(db::DemReleaseStatus::RollbackProductionToNew);

    txn = txnHandle();
    testReleaseStateTransition(txn, testDemRelease(),
        db::DemReleaseStatus::RollbackProductionToNew, db::DemReleaseStatus::Production,
        /*bad request*/ 400);
}

TEST_F(dem_api_should, test_dem_releases_api_conflicting_transactions)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    auto conflict = createTestDemRelease()
        .setStatus(db::DemReleaseStatus::MovingToProduction);

    auto txn = txnHandle();
    db::DemReleaseGateway(*txn).insert(conflict);
    txn->commit();

    txn = txnHandle();
    testReleaseStateTransition(txn, testDemRelease(),
        db::DemReleaseStatus::New, db::DemReleaseStatus::Production,
        /*bad request*/ 400);

    txn = txnHandle();
    testReleaseStateTransition(txn, testDemRelease(),
        db::DemReleaseStatus::Production, db::DemReleaseStatus::New,
        /*bad request*/ 400);
}

TEST_F(dem_api_should, test_dem_releases_api_check_last_issue_id)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    auto conflict = createTestDemRelease()
        .setIssueId(testDemRelease().issueId().value() + 1);
    {
        auto txn = txnHandle();
        db::DemReleaseGateway(*txn).insert(conflict);
        txn->commit();
    }
    {
        auto txn = txnHandle();
        testReleaseStateTransition(txn, testDemRelease(),
            db::DemReleaseStatus::Production, db::DemReleaseStatus::New,
            /*bad request*/ 400);
    }
}

TEST_F(dem_api_should, test_dem_releases_api_delete)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    auto release = testDemRelease();
    auto txn = txnHandle();
    release.setStatus(db::DemReleaseStatus::New);
    db::DemReleaseGateway(*txn).update(release);
    txn->commit();

    http::MockRequest request(
        http::DELETE,
        makeUrlDem("releases/delete")
            .addParam("id", std::to_string(release.id()))
    );
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);
    EXPECT_FALSE(db::DemReleaseGateway(*txnHandle()).existsById(release.id()));
}

TEST_F(dem_api_should, test_dem_releases_api_cannot_delete_not_new)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));
    auto release = testDemRelease();
    {
        release.setStatus(db::DemReleaseStatus::Production);
        auto txn = txnHandle();
        db::DemReleaseGateway(*txn).update(release);
        txn->commit();
        testDeleteReleaseFailed(release.id());
    }
    {
        release.setStatus(db::DemReleaseStatus::MovingToProduction);
        auto txn = txnHandle();
        db::DemReleaseGateway(*txn).update(release);
        txn->commit();
        testDeleteReleaseFailed(release.id());
    }
    {
        release.setStatus(db::DemReleaseStatus::RollbackProductionToNew);
        auto txn = txnHandle();
        db::DemReleaseGateway(*txn).update(release);
        txn->commit();
        testDeleteReleaseFailed(release.id());
    }
}

TEST_F(dem_api_should, test_dem_patches_api)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    http::MockRequest request(
        http::GET,
        makeUrlDem("patches")
            .addParam("release_id", std::to_string(testDemRelease().id()))
    );
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    auto responsePatches = boost::lexical_cast<sdem::DemPatches>(response.body);
    ASSERT_EQ(responsePatches.patches().size(), 1u);

    auto responsePatch = responsePatches.patches().at(0);
    EXPECT_EQ(responsePatch.name(), DEM_PATCH_NAME);
    EXPECT_EQ(*responsePatch.description(), DEM_PATCH_DESCRIPTION);
    EXPECT_EQ(geolib3::sproto::decode(*responsePatch.boundingBox()),
              geolib3::convertMercatorToGeodetic(toGeolibBox(DEM_BBOX)));
    EXPECT_EQ(responsePatch.releaseId(), std::to_string(testDemRelease().id()));
}

TEST_F(dem_api_should, test_dem_patches_api_get)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    http::MockRequest request(
        http::GET,
        makeUrlDem("patches/get")
            .addParam("id", std::to_string(testDemPatch().id()))
    );
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    auto responsePatch = boost::lexical_cast<sdem::DemPatch>(response.body);
    EXPECT_EQ(responsePatch.id(), std::to_string(testDemPatch().id()));
    EXPECT_EQ(responsePatch.name(), DEM_PATCH_NAME);
    EXPECT_EQ(*responsePatch.description(), DEM_PATCH_DESCRIPTION);
    EXPECT_EQ(geolib3::sproto::decode(*responsePatch.boundingBox()),
              geolib3::convertMercatorToGeodetic(toGeolibBox(DEM_BBOX)));
    EXPECT_EQ(responsePatch.releaseId(), std::to_string(testDemRelease().id()));
}

TEST_F(dem_api_should, test_dem_patches_api_search)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));
    {
        http::MockRequest request(
            http::GET,
            makeUrlDem("patches/search")
                .addParam("text", "wrong_query")
        );
        auto response = yacare::performTestRequest(request);
        ASSERT_EQ(response.status, 200);
        auto responsePatches = boost::lexical_cast<sdem::DemPatches>(response.body);
        EXPECT_EQ(responsePatches.patches().size(), 0u);
    }
    {
        http::MockRequest request(
            http::GET,
            makeUrlDem("patches/search")
                .addParam("text", "test_dem")
        );
        auto response = yacare::performTestRequest(request);
        ASSERT_EQ(response.status, 200);
        auto responsePatches = boost::lexical_cast<sdem::DemPatches>(response.body);
        EXPECT_EQ(responsePatches.patches().size(), 1u);
    }
    {
        const auto bbox = toGeo(DEM_BBOX);
        http::MockRequest request(
            http::GET,
            makeUrlDem("patches/search")
                .addParam("bbox", boxParam(bbox))
        );
        auto response = yacare::performTestRequest(request);
        ASSERT_EQ(response.status, 200);
        auto responsePatches = boost::lexical_cast<sdem::DemPatches>(response.body);
        EXPECT_EQ(responsePatches.patches().size(), 1u);
    }
    {
        const auto bboxSide = DEM_BBOX.max().x() - DEM_BBOX.min().x();
        const auto shift = Vector2d::Constant(2 * bboxSide);
        auto bbox = toGeo(shifted(DEM_BBOX, shift));
        http::MockRequest request(
            http::GET,
            makeUrlDem("patches/search")
                .addParam("bbox", boxParam(bbox))
        );
        auto response = yacare::performTestRequest(request);
        ASSERT_EQ(response.status, 200);
        auto responsePatches = boost::lexical_cast<sdem::DemPatches>(response.body);
        EXPECT_EQ(responsePatches.patches().size(), 0u);
    }
}

TEST_F(dem_api_should, test_dem_patches_api_bbox)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    http::MockRequest request(
        http::GET,
        makeUrlDem("patches/bbox")
            .addParam("id", std::to_string(testDemPatch().id()))
    );
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    auto responseBbox = boost::lexical_cast<scommon::geometry::BoundingBox>(response.body);
    EXPECT_EQ(geolib3::sproto::decode(responseBbox),
              geolib3::convertMercatorToGeodetic(toGeolibBox(DEM_BBOX)));
}

TEST_F(dem_api_should, test_dem_patches_api_create)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));
    changeTestReleaseStatus(db::DemReleaseStatus::New);

    sdem::DemPatchCreateData createData;
    createData.releaseId() = std::to_string(testDemRelease().id());
    createData.name() = DEM_PATCH_NAME_U;
    createData.description() = DEM_PATCH_DESCRIPTION_U;

    http::MockRequest request(http::POST, makeUrlDem("patches/create"));
    request.body = boost::lexical_cast<std::string>(createData);
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    auto responsePatch = boost::lexical_cast<sdem::DemPatch>(response.body);
    EXPECT_NE(responsePatch.id(), std::to_string(testDemPatch().id()));
    EXPECT_EQ(responsePatch.name(), DEM_PATCH_NAME_U);
    EXPECT_EQ(*responsePatch.description(), DEM_PATCH_DESCRIPTION_U);
    EXPECT_FALSE(responsePatch.boundingBox());
    EXPECT_EQ(responsePatch.releaseId(), std::to_string(testDemRelease().id()));

    auto patch = db::DemPatchGateway(*txnHandle())
        .loadById(db::parseId(responsePatch.id()));
    EXPECT_EQ(patch.name(), DEM_PATCH_NAME_U);
    EXPECT_EQ(patch.description(), DEM_PATCH_DESCRIPTION_U);
    EXPECT_FALSE(patch.bbox());
    EXPECT_EQ(patch.releaseId(), testDemRelease().id());
}

TEST_F(dem_api_should, test_dem_patches_api_update)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));
    changeTestReleaseStatus(db::DemReleaseStatus::New);

    sdem::DemPatchUpdateData updateData;
    updateData.name() = DEM_PATCH_NAME_U;
    updateData.description() = DEM_PATCH_DESCRIPTION_U;

    http::MockRequest request(
        http::POST,
        makeUrlDem("patches/update")
            .addParam("id", std::to_string(testDemPatch().id()))
    );
    request.body = boost::lexical_cast<std::string>(updateData);
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    auto responsePatch = boost::lexical_cast<sdem::DemPatch>(response.body);
    EXPECT_EQ(responsePatch.id(), std::to_string(testDemPatch().id()));
    EXPECT_EQ(responsePatch.name(), DEM_PATCH_NAME_U);
    EXPECT_EQ(*responsePatch.description(), DEM_PATCH_DESCRIPTION_U);
    EXPECT_EQ(geolib3::sproto::decode(*responsePatch.boundingBox()),
              geolib3::convertMercatorToGeodetic(toGeolibBox(DEM_BBOX)));
    EXPECT_EQ(responsePatch.releaseId(), std::to_string(testDemRelease().id()));

    auto patch = db::DemPatchGateway(*txnHandle()).loadById(testDemPatch().id());
    EXPECT_EQ(patch.name(), DEM_PATCH_NAME_U);
    EXPECT_EQ(patch.description(), DEM_PATCH_DESCRIPTION_U);
    EXPECT_EQ(toGeolibBox(patch.bbox().value()), toGeolibBox(DEM_BBOX));
    EXPECT_EQ(patch.releaseId(), testDemRelease().id());
}

TEST_F(dem_api_should, test_dem_patches_api_delete)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    http::MockRequest request(
        http::DELETE,
        makeUrlDem("patches/delete")
            .addParam("id", std::to_string(testDemPatch().id()))
    );
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);
    EXPECT_FALSE(db::DemPatchGateway(*txnHandle()).existsById(testDemPatch().id()));
}

} // namespace maps::factory::backend::tests

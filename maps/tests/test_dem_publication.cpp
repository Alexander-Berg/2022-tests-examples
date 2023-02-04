#include <maps/factory/libs/processing/dem_publication.h>

#include <maps/factory/libs/dataset/dem_tile_dataset.h>
#include <maps/factory/libs/dataset/tiles.h>
#include <maps/factory/libs/db/dem_release_gateway.h>
#include <maps/factory/libs/db/dem_patch_gateway.h>
#include <maps/factory/libs/rendering/dem_utils.h>
#include <maps/factory/libs/rendering/impl/patched_dem_tile_dataset.h>
#include <maps/factory/libs/geometry/geometry.h>
#include <maps/factory/libs/storage/local_storage.h>

#include <maps/factory/libs/processing/tests/test_context.h>
#include <maps/factory/libs/processing/tests/test_s3.h>
#include <maps/factory/libs/unittest/tests_common.h>
#include <maps/factory/libs/unittest/fixture.h>

namespace maps::factory::processing::tests {
using namespace testing;
using namespace storage;

namespace {

using factory::tests::TEST_PATCH1_TILES;
using factory::tests::TEST_PATCH2_TILES;

const int fixDemPath = factory::tests::changeGlobalDemPath();
constexpr int DEM_ZOOM = 10;

db::DemRelease prepareDemRelease(pgpool3::TransactionHandle& txn)
{
    const geometry::MercatorPixelPointBbox pointBox(DEM_ZOOM);
    db::DemRelease release("SomeRelease");
    release.setIssueId(1);
    db::DemReleaseGateway(*txn).insert(release);
    const auto patch1 = geometry::MultiPoint3d::fromWktFile(factory::tests::TEST_PATCH1);
    const auto patch2 = geometry::MultiPoint3d::fromWktFile(factory::tests::TEST_PATCH2);
    db::DemPatches patches{
        db::DemPatch("FirstPatch", release.id())
            .extendBbox(patch1.bbox(pointBox))
            .setTiles(TEST_PATCH1_TILES),
        db::DemPatch("SecondPatch", release.id())
            .extendBbox(patch2.bbox(pointBox))
            .setTiles(TEST_PATCH2_TILES)
    };
    db::DemPatchGateway(*txn).insert(patches);
    return release;
}

class DemPublicationFixture : public unittest::Fixture {
public:
    DemPublicationFixture()
        : Fixture()
        , pgPool_(
            Shared<pgpool3::Pool>(
                postgres().connectionString(),
                pgpool3::PoolConstants(1, 5, 1, 5)))
        , bc_(Shared<dataset::TLruBlockCache>(256_MB))
    {
        dataset::DemTileDataset::registerDriver(bc_, DEM_ZOOM, "", testS3Auth());
        rendering::PatchedDemTileDataset::registerDriver(
            bc_, pgPool_, DEM_ZOOM, factory::tests::DEM_PATCH_TILES, "", testS3Auth());
    }

    void deregisterDrivers()
    {
        rendering::PatchedDemTileDataset::deregisterDriver();
        dataset::DemTileDataset::deregisterDriver();
    }

    pgpool3::TransactionHandle txnHandle()
    {
        return pgPool_->masterWriteableTransaction();
    }

private:
    std::shared_ptr<pgpool3::Pool> pgPool_;
    dataset::BlockCachePtr bc_;
};

} // namespace

Y_UNIT_TEST_SUITE(dem_publication_tasks_should) {

Y_UNIT_TEST(mark_dem_release)
{
    DemPublicationFixture fixture;
    TestContext ctx(fixture.postgres().connectionString());
    db::DemRelease release("SomeRelease");
    release.setStatus(db::DemReleaseStatus::MovingToProduction);
    release.setIssueId(1);
    db::DemReleaseGateway(ctx.transaction()).insert(release);

    {
        MarkDemReleasePublishedToProduction{}(ReleaseId(release.id()), ctx);
        db::DemReleaseGateway(ctx.transaction()).reload(release);
        EXPECT_EQ(release.status(), db::DemReleaseStatus::Production);
    }

    release.setStatus(db::DemReleaseStatus::RollbackProductionToNew);
    db::DemReleaseGateway(ctx.transaction()).update(release);

    {
        MarkDemReleaseRevertedToNew{}(ReleaseId(release.id()), ctx);
        db::DemReleaseGateway(ctx.transaction()).reload(release);
        EXPECT_EQ(release.status(), db::DemReleaseStatus::New);
        EXPECT_FALSE(release.issueId());
    }
    fixture.deregisterDrivers();
}

Y_UNIT_TEST(create_dem_tiles)
{
    DemPublicationFixture fixture;
    auto txn = fixture.txnHandle();
    const auto release = prepareDemRelease(txn);
    txn->commit();

    const auto s3Root = testS3(this->Name_);
    const auto s3Dem = s3Root->dir("dem_tiles");
    const auto s3Out = s3Root->dir("dem_tiles_out");
    auto demPath = rendering::getDemPath();
    if (demPath.starts_with("DEM_TILE:")) {
        demPath = demPath.substr(9);
    }
    localStorage(demPath)->copyAll(*s3Dem);

    TestContext ctx(fixture.postgres().connectionString());
    CreateDemTiles{
        .pool = S3Pool(testS3Auth()),
        .demPath = s3Dem->absPath().native(),
        .outPath = s3Out->absPath().native(),
        .demZoom = 10,
        .demPattern = "",
    }(ReleaseId(release.id()), ctx);
    ASSERT_THAT(s3Out->list(Select::Files), Not(IsEmpty()));

    const auto dataDir = localStorage(SRC_("data/patched_dem_tiles").c_str());
    const auto paths = dataDir->list(Select::Files);
    for (const auto& path: paths) {
        const auto expected = dataset::TifDemTile::readImageFromFile(dataDir->file(path)->absPathStr());
        const auto img = dataset::TifDemTile::readImageFromFile(s3Out->file(path)->absPathStr());
        EXPECT_LE(img.meanAbsDifference(expected), 1.5) << path;
    }
    fixture.deregisterDrivers();
}

Y_UNIT_TEST(remove_dem_tiles)
{
    DemPublicationFixture fixture;
    auto txn = fixture.txnHandle();
    const auto release = prepareDemRelease(txn);
    txn->commit();

    const auto s3Root = testS3(this->Name_);
    const auto s3Dem = s3Root->dir("dem_tiles");
    const auto demPath = rendering::getDemPath();
    auto demDir = localStorage(demPath);
    demDir->copyAll(*s3Dem);
    auto patchedDir = localStorage(SRC_("data/patched_dem_tiles").c_str());
    patchedDir->copyAll(*s3Dem);

    TestContext ctx(fixture.postgres().connectionString());
    RemoveDemTiles{
        .pool = S3Pool(testS3Auth()),
        .tilesPath = s3Dem->absPath().native(),
        .demZoom = 10,
        .demPattern = "",
    }(ReleaseId(release.id()), ctx);

    for (const auto& path: demDir->list(Select::Files)) {
        EXPECT_TRUE(s3Dem->file(path)->exists());
    }
    for (const auto& path: patchedDir->list(Select::Files)) {
        EXPECT_FALSE(s3Dem->file(path)->exists());
    }
    fixture.deregisterDrivers();
}

} //suite
} // namespace maps::factory::processing::tests

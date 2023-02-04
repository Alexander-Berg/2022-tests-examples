#include "test_data.h"

#include <maps/factory/libs/config/config.h>
#include <maps/factory/libs/dataset/tiles.h>
#include <maps/factory/libs/db/common.h>
#include <maps/factory/libs/rendering/dem_utils.h>
#include <maps/factory/libs/rendering/impl/patched_dem_tile_dataset.h>

#include <maps/factory/libs/db/dem_release_gateway.h>
#include <maps/factory/libs/db/dem_patch_gateway.h>
#include <maps/factory/libs/unittest/tests_common.h>
#include <maps/factory/libs/unittest/fixture.h>

#include <maps/libs/sql_chemistry/include/sequence.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace maps::factory::rendering::tests {
namespace {
using namespace testing;

using factory::tests::TEST_TILE1;
using factory::tests::TEST_TILE2;
using factory::tests::TEST_PATCH1;
using factory::tests::TEST_PATCH2;
using factory::tests::TEST_PATCH1_TILES;
using factory::tests::TEST_PATCH2_TILES;

const int fixDemPath = factory::tests::changeGlobalDemPath();

const auto CUR_DEM_PATH = SRC_("data/dem_tiles");

void prepareDemRelease(
    sql_chemistry::Transaction& txn,
    db::DemRelease& release,
    const Box2d& bbox,
    const db::IssuedTileMap& tiles,
    int issue)
{
    release.setIssueId(issue);
    release.setStatus(db::DemReleaseStatus::Production);
    db::DemReleaseGateway(txn).insert(release);
    db::DemPatch demPatch(release.name() + "_patch", release.id());
    demPatch.extendBbox(bbox);
    demPatch.setTiles(tiles);
    db::DemPatchGateway(txn).insert(demPatch);
}

class DemTilesFixture final : public unittest::Fixture {
public:
    DemTilesFixture()
        : Fixture()
        , release1_("release1")
        , release2_("release2")
        , config_(config::Config::fromResource())
        , pgPool_(config_.sharedDbPool())
        , bc_(Shared<dataset::TLruBlockCache>())
    {
        auto txn = txnHandle();
        const auto patch1 = geometry::MultiPoint3d::fromWktFile(TEST_PATCH1);
        const auto patch2 = geometry::MultiPoint3d::fromWktFile(TEST_PATCH2);
        prepareDemRelease(*txn, release1_, patch1.bbox(), TEST_PATCH1_TILES, 1);
        prepareDemRelease(*txn, release2_, patch2.bbox(), TEST_PATCH2_TILES, 2);
        txn->commit();

        dataset::DemTileDataset::registerDriver(bc_, config_.demZoom(), "");
        PatchedDemTileDataset::registerDriver(
            bc_, pgPool_, config_.demZoom(), factory::tests::DEM_PATCH_TILES, "");
    }

    void unPublishRelease2()
    {
        release2_.setStatus(db::DemReleaseStatus::New);
        release2_.resetIssueId();
        auto txn = txnHandle();
        db::DemReleaseGateway(*txn).update(release2_);
        txn->commit();
    }

    ~DemTilesFixture() override
    {
        dataset::DemTileDataset::deregisterDriver();
        PatchedDemTileDataset::deregisterDriver();
    }

    pgpool3::TransactionHandle txnHandle()
    {
        return pgPool_->masterWriteableTransaction();
    }

    const db::DemRelease& release1() { return release1_; }

    const db::DemRelease& release2() { return release2_; }

private:
    db::DemRelease release1_;
    db::DemRelease release2_;
    config::Config config_;
    std::shared_ptr<pgpool3::Pool> pgPool_;
    dataset::BlockCachePtr bc_;
};

} // namespace

Y_UNIT_TEST_SUITE(dem_tile_loader_should) {

Y_UNIT_TEST(get_base_issue)
{
    DemTilesFixture fixture;
    auto txn = fixture.txnHandle();
    EXPECT_EQ(loadDemIssueId(*txn, TEST_TILE1), fixture.release2().issueId().value());
    EXPECT_EQ(loadDemIssueId(*txn, TEST_TILE1, fixture.release1().id()),
        fixture.release1().issueId().value());
    EXPECT_EQ(loadDemIssueId(*txn, TEST_TILE1, fixture.release2().id(), db::Ids{2}),
        fixture.release1().issueId().value());
    EXPECT_EQ(loadDemIssueId(*txn, TEST_TILE1, fixture.release1().id(), db::Ids{1}), 0);
    EXPECT_EQ(loadDemIssueId(*txn, TEST_TILE2), fixture.release2().issueId().value());
    EXPECT_EQ(loadDemIssueId(*txn, TEST_TILE2, fixture.release1().id()), 0);
}

Y_UNIT_TEST(load_patched_names)
{
    DemTilesFixture fixture;
    fixture.unPublishRelease2();
    {
        auto txn = fixture.txnHandle();
        const auto issueId = loadDemIssueId(*txn, TEST_TILE1, fixture.release1().id());
        const auto patches = loadDemPatches(*txn, fixture.release1().id());
        const PatchedDemName name(CUR_DEM_PATH, issueId, patches);
        const PatchedDemName expected(CUR_DEM_PATH, 1, {1}, {0});
        EXPECT_EQ(name.str(), expected.str());
    }
    {
        auto txn = fixture.txnHandle();
        const auto issueId = loadDemIssueId(*txn, TEST_TILE1, fixture.release2().id());
        const auto patches = loadDemPatches(*txn, fixture.release2().id());
        const PatchedDemName name(CUR_DEM_PATH, issueId, patches);
        const PatchedDemName expected(CUR_DEM_PATH, 1, {2}, {0});
        EXPECT_EQ(name.str(), expected.str());
    }
}

Y_UNIT_TEST(load_patched_tiles)
{
    DemTilesFixture fixture;
    fixture.unPublishRelease2();
    {
        const PatchedDemName name(CUR_DEM_PATH, 1, {1}, {0});
        dataset::TDataset ds = dataset::OpenDataset(name.str());
        const auto loadedTile = dataset::readTile<int16_t>(ds, TEST_TILE1);
        const auto expectedTile = dataset::TifDemTile::readImageFromFile(SRC_("data/issue1_tile.tif").c_str());
        EXPECT_EQ(loadedTile, expectedTile);
    }
    {
        const PatchedDemName name(CUR_DEM_PATH, 1, {2}, {0});
        dataset::TDataset ds = dataset::OpenDataset(name.str());
        const auto loadedTile = dataset::readTile<int16_t>(ds, TEST_TILE1);
        const auto expectedTile = dataset::TifDemTile::readImageFromFile(SRC_("data/issue2_tile.tif").c_str());
        EXPECT_EQ(loadedTile, expectedTile);
    }
}

} // suite
} // namespace maps::factory::rendering::tests

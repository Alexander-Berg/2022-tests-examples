#include <maps/factory/libs/config/config.h>
#include <maps/factory/libs/dataset/memory_file.h>
#include <maps/factory/libs/dataset/tiles.h>
#include <maps/factory/libs/geometry/tiles.h>
#include <maps/factory/libs/storage/s3_pool.h>
#include <maps/factory/libs/rendering/impl/issued_dem_tile_dataset.h>

#include <maps/factory/libs/unittest/tests_common.h>
#include <maps/factory/libs/unittest/fixture.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <util/stream/buffer.h>

namespace maps::factory::rendering::tests {
namespace {
using namespace testing;

using factory::tests::TEST_TILE1;

const int fixDemPath = factory::tests::changeGlobalDemPath();

const auto DEM_PATH = SRC_("data/dem_tiles");

const db::ReleaseIssue TEST_ISSUE = 10;

class DemTilesFixture final : public unittest::Fixture {
public:
    DemTilesFixture()
        : Fixture()
        , config_(config::Config::fromResource())
        , bc_(Shared<dataset::TLruBlockCache>()) {}

    dataset::BlockCachePtr blockCache() { return bc_; }

    int zoom() { return config_.demZoom(); }

private:
    config::Config config_;
    dataset::BlockCachePtr bc_;
};

dataset::Int16Image tileImageFromStorage(
    storage::TileStorage& storage,
    const tile::Tile& tile)
{
    const auto file = storage.file(tile);
    const auto data = file->readToString();
    dataset::MemoryFile tmp = dataset::MemoryFile::unique("dem_tile.tif");
    tmp.write(data);
    dataset::TDataset ds = dataset::OpenDataset(tmp.path());
    dataset::Int16Image dstImg(geometry::TILE_SIZES, 1);
    ds.Read(dstImg, ds.bounds().cast<double>());
    return dstImg;
}

} // namespace

Y_UNIT_TEST_SUITE(tile_dem_issue_map_serializer_should) {

using TileMapSerializer = TSerializer<TileDemIssueMap>;

Y_UNIT_TEST(save_and_load_map)
{
    const TileDemIssueMap demIssueMap{
        {tile::Tile(616, 397, 10), 1},
        {tile::Tile(617, 398, 11), 2}
    };
    TBufferOutput output;
    TileMapSerializer::Save(&output, demIssueMap);

    TileDemIssueMap resDemIssueMap;
    TBufferInput input(output.Buffer());
    TileMapSerializer::Load(&input, resDemIssueMap);
    EXPECT_EQ(demIssueMap, resDemIssueMap);
}

} // suite

Y_UNIT_TEST_SUITE(issued_dem_tile_loader_should) {

Y_UNIT_TEST(load_production_tiles)
{
    DemTilesFixture fixture;
    TileDemIssueMap issueMap;
    issueMap[TEST_TILE1] = TEST_ISSUE;

    IssuedDemTileDataset::registerDriver(
        fixture.blockCache(), fixture.zoom(),
        storage::TileStorage::HEX_TILE_NAME, issueMap);

    const storage::AuthPtr auth;
    auto s3Pool = storage::S3Pool(auth);
    storage::TileStorage storage(
        s3Pool.storage(std::string(SRC_("data/dem_tiles/").c_str())),
        storage::TileStorage::HEX_TILE_NAME, 10);

    dataset::TDataset ds = dataset::OpenDataset(
        IssuedDemTileDataset::PREFIX + SRC_("data/dem_tiles/"));
    const auto loadedTile = dataset::readTile<int16_t>(ds, TEST_TILE1);
    const auto expectedTile = tileImageFromStorage(storage, TEST_TILE1);
    EXPECT_EQ(loadedTile, expectedTile);
    IssuedDemTileDataset::deregisterDriver();
}

Y_UNIT_TEST(load_base_tiles_without_issue)
{
    DemTilesFixture fixture;
    TileDemIssueMap issueMap;

    IssuedDemTileDataset::registerDriver(
        fixture.blockCache(), fixture.zoom(),
        storage::TileStorage::HEX_TILE_NAME, issueMap);

    const storage::AuthPtr auth;
    auto s3Pool = storage::S3Pool(auth);
    storage::TileStorage storage(
        s3Pool.storage(std::string(SRC_("data/dem_tiles/").c_str())),
        storage::TileStorage::HEX_TILE_NAME, storage::BASE_ISSUE_ID);

    dataset::TDataset ds = dataset::OpenDataset(
        IssuedDemTileDataset::PREFIX + SRC_("data/dem_tiles/"));
    const auto loadedTile = dataset::readTile<int16_t>(ds, TEST_TILE1);
    const auto expectedTile = tileImageFromStorage(storage, TEST_TILE1);
    EXPECT_EQ(loadedTile, expectedTile);
    IssuedDemTileDataset::deregisterDriver();
}

} // suite
} // namespace maps::factory::rendering::tests

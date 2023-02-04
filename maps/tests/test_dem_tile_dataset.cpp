#include <maps/factory/libs/dataset/dem_tile_dataset.h>

#include <maps/factory/libs/dataset/tiles.h>

#include <maps/factory/libs/unittest/tests_common.h>
#include <maps/factory/libs/common/s3_keys.h>

#include <util/generic/scope.h>

namespace maps::factory::dataset::tests {
using namespace factory::tests;
using namespace image;

static const auto _ = fixGdalDataFolderForTests();

Y_UNIT_TEST_SUITE(dem_tile_dataset_should) {

constexpr int DEM_ZOOM = 10;

Y_UNIT_TEST(read_data)
{
    DemTileDataset::registerDriver(Shared<TEmptyBlockCache>(), DEM_ZOOM, "");
    Y_DEFER { DemTileDataset::deregisterDriver(); };

    const auto path =
        ArcadiaSourceRoot() + "/maps/factory/test_data/dem_tiles";
    const auto pathTile =
        ArcadiaSourceRoot() + "/maps/factory/test_data/dem_tiles/aAIAAAAAAACNAQAAAAAAAAoAAAAAAAAA";

    TDataset ds = OpenDataset(DemTileDataset::PREFIX + path);
    TDataset dsTile = OpenDataset(pathTile);

    Int16Image data(dsTile.size(), 1);
    ds.Read(data, ds.Site().ProjToPix() * dsTile.Site().ProjBounds());
    Int16Image dataTile = dsTile.Read<int16_t>();

    EXPECT_EQ(data, dataTile);
}

} // suite
} // namespace maps::factory::dataset::tests

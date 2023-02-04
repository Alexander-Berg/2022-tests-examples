#include <maps/factory/libs/dataset/hillshade.h>

#include <maps/factory/libs/dataset/tiles.h>
#include <maps/factory/libs/dataset/dem_tile_dataset.h>

#include <maps/factory/libs/unittest/tests_common.h>
#include <maps/factory/libs/common/s3_keys.h>

#include <util/generic/scope.h>

namespace maps::factory::dataset::tests {
using namespace factory::tests;
using namespace image;

static const auto _ = fixGdalDataFolderForTests();

Y_UNIT_TEST_SUITE(hillshade_should) {

constexpr int DEM_ZOOM = 10;

Y_UNIT_TEST(shade_tile)
{
    DemTileDataset::registerDriver(Shared<TEmptyBlockCache>(), DEM_ZOOM, "");
    Y_DEFER { DemTileDataset::deregisterDriver(); };

    const auto path =
        ArcadiaSourceRoot() + "/maps/factory/test_data/dem_tiles";

    TDataset ds = OpenDataset(DemTileDataset::PREFIX + path);
    const tile::Tile tile = extractTileFromKey("aAIAAAAAAACNAQAAAAAAAAoAAAAAAAAA");

    const UInt8Image img =
        dataset::Hillshade()
            .setComputeEdges()
            .hillshade(tile, ds);

    const UInt8Image expected = UInt8Image::fromFile(SRC_("data/hillshade_616_397_10.png"));
    EXPECT_EQ(img, expected);
}

} // suite
} // namespace maps::factory::dataset::tests

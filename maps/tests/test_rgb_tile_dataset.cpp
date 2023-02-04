#include <maps/factory/libs/dataset/rgb_tile_dataset.h>

#include <maps/factory/libs/common/eigen.h>

#include <maps/factory/libs/unittest/tests_common.h>

namespace maps::factory::dataset::tests {
using namespace factory::tests;

Y_UNIT_TEST_SUITE(http_dataset_should) {

Y_UNIT_TEST(read_tile)
{
    auto bc = std::make_shared<TLruBlockCache>();
    RgbTileDataset
        ds(storage::TileStorage::fromPath(SRC_("data/sat_tiles/sat_{x}_{y}_{z}.png").c_str()), 3, bc);
    {
        UInt8Image img({512, 512}, 4);
        ds.ds().Read<uint8_t>(img, boxFromPoints<double>(0, 512, 512, 1024));
        EXPECT_EQ(img.firstBandsImage(3).maxAbsDifference(
            UInt8Image::fromFile(SRC_("data/sat_tile_1.png"))), 0);
        EXPECT_TRUE(img.isLastBandWhite());
    }
    {
        UInt8Image img({128, 128}, 4);
        ds.ds().Read<uint8_t>(img, boxFromPoints<double>(0, 512, 512, 1024));
        EXPECT_EQ(img.firstBandsImage(3).maxAbsDifference(
            UInt8Image::fromFile(SRC_("data/sat_tile_2.png"))), 0);
        EXPECT_TRUE(img.isLastBandWhite());
    }
    {
        UInt8Image img({256, 256}, 4);
        ds.ds().Read<uint8_t>(img, boxFromPoints<double>(100, 500, 356, 756));
        EXPECT_EQ(img.firstBandsImage(3).maxAbsDifference(
            UInt8Image::fromFile(SRC_("data/sat_tile_3.png"))), 0);
        EXPECT_TRUE(img.isLastBandWhite());
    }
}

Y_UNIT_TEST(read_transparent_when_tile_not_found)
{
    auto bc = std::make_shared<TLruBlockCache>();
    RgbTileDataset ds(storage::TileStorage(SRC_("data/sat_tiles/").c_str(), "sat_{x}_{y}_{z}.png"), 3, bc);
    ds.setEmptyColor(image::Color::black());

    UInt8Image img({256, 256}, 4);
    ds.ds().Read<uint8_t>(img, boxFromPoints<double>(0, 0, 256, 256));

    EXPECT_TRUE(img.isZero());
}

Y_UNIT_TEST(read_gray_when_tile_not_found)
{
    auto bc = std::make_shared<TLruBlockCache>();
    RgbTileDataset ds(storage::TileStorage(SRC_("data/sat_tiles/").c_str(), "sat_{x}_{y}_{z}.png"), 3, bc);

    UInt8Image img({256, 256}, 4);
    ds.ds().Read<uint8_t>(img, boxFromPoints<double>(0, 0, 256, 256));

    EXPECT_TRUE(img.firstBandsImage(3).isConstant(RgbTileDataset::EMPTY_COLOR));
    EXPECT_TRUE(img.opaque());
}

} // suite
} // namespace maps::factory::dataset::tests

#include <maps/factory/libs/dataset/multipoint_dataset.h>

#include <maps/factory/libs/image/draw.h>

#include <maps/factory/libs/unittest/tests_common.h>

#include <maps/libs/tile/include/tile.h>

namespace maps::factory::dataset::tests {
using namespace maps::factory::tests;
namespace {

constexpr unsigned DEM_ZOOM = 10;

constexpr tile::Tile TILE(619, 321, DEM_ZOOM);

MultiPoint3d testPoints()
{
    return {
        {4220407.5, 7451228.6, 110},
        {4218173.3, 7467041.7, 120},
        {4223204.2, 7443525.4, 130},
        {4200645.4, 7465859.4, 140},
        {4198397.1, 7457474.1, 150},
        {4206209.5, 7460405.5, 160},
        {4201802.3, 7455886.4, 170},
        {4224792.4, 7471650.1, 180},
    };
}

} // namespace

Y_UNIT_TEST_SUITE(multipoint_2d_dataset_should) {

Y_UNIT_TEST(draw_at_points_zoom)
{
    const MultiPoint2d points = testPoints().withoutZ();
    MultiPoint2dDataset ds(&points, Shared<TEmptyBlockCache>(), DEM_ZOOM);
    const UInt8Image img = ds.readTile(TILE);
    const UInt8Image expected =
        OpenDataset(SRC_("data/draw_points_619_321_10.tif")).get().Read<uint8_t>().inverted();
    EXPECT_EQ(expected.meanAbsDifference(img), 0);
}

Y_UNIT_TEST(draw_parent_tile)
{
    const tile::Tile parent(TILE.x() / 2, TILE.y() / 2, TILE.z() - 1);
    const MultiPoint2d points = testPoints().withoutZ();
    MultiPoint2dDataset ds(&points, Shared<TEmptyBlockCache>(), static_cast<int>(parent.z()));
    const UInt8Image img = ds.readTile(parent);
    const UInt8Image expected =
        OpenDataset(SRC_("data/draw_points_309_160_9.tif")).get().Read<uint8_t>();
    EXPECT_EQ(expected.meanAbsDifference(img), 0);
}

Y_UNIT_TEST(draw_child_tile)
{
    const tile::Tile child(TILE.x() * 2, TILE.y() * 2, TILE.z() + 1);
    const MultiPoint2d points = testPoints().withoutZ();
    MultiPoint2dDataset ds(&points, Shared<TEmptyBlockCache>(), DEM_ZOOM);
    const UInt8Image img = ds.readTile(child);
    const UInt8Image expected =
        OpenDataset(SRC_("data/draw_points_1238_642_11.tif")).get().Read<uint8_t>();
    EXPECT_EQ(expected.meanAbsDifference(img), 0);
}

} // suite

Y_UNIT_TEST_SUITE(multipoint_3d_dataset_should) {

Y_UNIT_TEST(draw_at_points_zoom)
{
    const MultiPoint3d points = testPoints();
    MultiPoint3dDataset ds(&points, Shared<TEmptyBlockCache>(), DEM_ZOOM);
    const Int16Image img = ds.readTile(TILE);
    const Int16Image expected =
        OpenDataset(SRC_("data/draw_points_z_nodata_619_321_10.tif")).get().Read<int16_t>();
    EXPECT_EQ(expected.meanAbsDifference(img), 0);
}

Y_UNIT_TEST(draw_parent_tile)
{
    const tile::Tile parent(TILE.x() / 2, TILE.y() / 2, TILE.z() - 1);
    const MultiPoint3d points = testPoints();
    MultiPoint3dDataset ds(&points, Shared<TEmptyBlockCache>(), static_cast<int>(parent.z()));
    const Int16Image img = ds.readTile(parent, ResampleAlg::Bilinear);
    const Int16Image expected =
        OpenDataset(SRC_("data/draw_points_z_nodata_309_160_9.tif")).get().Read<int16_t>();
    EXPECT_EQ(expected.meanAbsDifference(img), 0);
}

Y_UNIT_TEST(draw_child_tile)
{
    const tile::Tile child(TILE.x() * 2, TILE.y() * 2, TILE.z() + 1);
    const MultiPoint3d points = testPoints();
    MultiPoint3dDataset ds(&points, Shared<TEmptyBlockCache>(), DEM_ZOOM);
    const Int16Image img = ds.readTile(child);
    const Int16Image expected =
        OpenDataset(SRC_("data/draw_points_z_nodata_1238_642_11.tif")).get().Read<int16_t>();
    EXPECT_EQ(expected.meanAbsDifference(img), 0);
}

} // suite
} // namespace maps::factory::dataset::tests

#include <maps/factory/libs/dataset/tiles.h>

#include <maps/factory/libs/dataset/create_warped.h>
#include <maps/factory/libs/dataset/memory_file.h>
#include <maps/factory/libs/dataset/create_raster_dataset.h>

#include <maps/factory/libs/geometry/tiles.h>
#include <maps/factory/libs/common/eigen.h>
#include <maps/factory/libs/unittest/tests_common.h>

#include <maps/libs/geolib/include/const.h>

namespace maps::factory::dataset::tests {
using namespace maps::factory::tests;

const tile::Tile INTERSECTED_TILES[]{
    {9870, 6357, 14}, // TL corner
    {9870, 6360, 14}, // BL corner
    {9877, 6357, 14}, // TR corner
    {9877, 6360, 14}, // BR corner
    {9870, 6358, 14}, // L side
    {9871, 6359, 14}, // B side
    {9871, 6357, 14}, // T side
    {9877, 6358, 14}, // R side
    {9872, 6358, 14}, // Inside
    {9875, 6359, 14}, // Inside
};

const tile::Tile OUTSIDE_TILES[]{
    {9878, 6358, 14},
    {9873, 6356, 14},
};

const auto DATASET_PATH =
    ArcadiaSourceRoot() + "/maps/factory/test_data/dem/058800151040_01_DEM_MERCATOR.TIF";

Y_UNIT_TEST_SUITE(zoom_utils_should) {

Y_UNIT_TEST(get_mercator)
{
    TDataset ds = OpenDataset(IKONOS_PATH);
    EXPECT_EQ(ds.Site().MaxTileZoom(), 11);
}

Y_UNIT_TEST(get_utm)
{
    TDataset ds = OpenDataset{WV_PATH};
    EXPECT_EQ(ds.Site().MaxTileZoom(), 17);
}

Y_UNIT_TEST(get_geodetic)
{
    TDataset ds = OpenDataset(SRTM_PATH);
    EXPECT_EQ(ds.Site().MaxTileZoom(), 11);
}

Y_UNIT_TEST(get_proper_overview_ind)
{
    const int maxZoom = 16;
    const int overviewsCount = 6;
    const int minZoom = maxZoom - overviewsCount;
    EXPECT_EQ(bestMatchOverviewInd(maxZoom + 1, maxZoom, overviewsCount), -1);
    EXPECT_EQ(bestMatchOverviewInd(minZoom - 1, maxZoom, overviewsCount), overviewsCount - 1);
    for (int requestedZoom = maxZoom, expectedInd = -1;
         requestedZoom >= minZoom;
         --requestedZoom, ++expectedInd) {
        EXPECT_EQ(bestMatchOverviewInd(requestedZoom, maxZoom, overviewsCount), expectedInd);
    }
}

} // suite

Y_UNIT_TEST_SUITE(tile_utils_should) {

Y_UNIT_TEST(get_simple_tile_name)
{
    EXPECT_EQ(tileFileName(tile::Tile(0, 0, 0)), "0_0_0");
    EXPECT_EQ(tileFileName(tile::Tile(1, 2, 3)), "1_2_3");
}

Y_UNIT_TEST(parse_tile_name)
{
    EXPECT_EQ(parseTileInput("0,0,0"), tile::Tile(0, 0, 0));
    EXPECT_EQ(parseTileInput("1,2,3"), tile::Tile(1, 2, 3));
    EXPECT_EQ(parseTileInput("1432,7453,20"), tile::Tile(1432, 7453, 20));
}

Y_UNIT_TEST(get_tile_name_using_placeholders)
{
    EXPECT_EQ(tileFileName(tile::Tile(0, 0, 0), "/a/b/c_{x}_{y}_{z}.png"),
        "/a/b/c_0_0_0.png");
    EXPECT_EQ(tileFileName(tile::Tile(1, 2, 3), "http://some-site.com/tiles?x={x}&y={y}&z={z}"),
        "http://some-site.com/tiles?x=1&y=2&z=3");
}

} // suite
Y_UNIT_TEST_SUITE(tile_reader_should) {

const Vector2d SHIFT_METERS(150, 300);

const int BUFFER_PIX = 64;

void save(const Int16Image& img, const tile::Tile& tile, const std::string& suff)
{
    TDataset tileDs = createTifTile(tile,
        "/home/unril/arc/arcadia/maps/factory/libs/dataset/tests/data/dem_tiles/" +
        tileFileName(tile) + suff +
        ".tif", 1, TDataType::Int16);
    tileDs.setNodata(DEM_NODATA_VALUE);
    tileDs.Write(img);
}

Y_UNIT_TEST(read_intersected_tiles)
{
    TDataset ds = OpenDataset(DATASET_PATH);
    for (const auto& tile: INTERSECTED_TILES) {
        auto reader = TileReader<int16_t>(tile, ds)
            .setNodata(DEM_NODATA_VALUE);
        const auto bufImg = reader.readBufferedImage();
        ASSERT_TRUE(bufImg);
        const Int16Image tileImg = reader.clipBufferedImage(*bufImg);
        TDataset expected = OpenDataset(SRC_("data/dem_tiles/" + tileFileName(tile) + ".tif"));
        EXPECT_EQ(tileImg.maxAbsDifference(expected.Read<int16_t>()), 0);
    }
}

Y_UNIT_TEST(read_outside_tiles)
{
    TDataset ds = OpenDataset(DATASET_PATH);
    for (const auto& tile: OUTSIDE_TILES) {
        TileReader<int16_t> reader(tile, ds);
        EXPECT_FALSE(reader.readBufferedImage());
    }
}

Y_UNIT_TEST(read_intersected_tiles_with_buffer_pixels)
{
    TDataset ds = OpenDataset(DATASET_PATH);
    for (const auto& tile: INTERSECTED_TILES) {
        auto reader = TileReader<int16_t>(tile, ds)
            .setBuffer(BUFFER_PIX)
            .setNodata(DEM_NODATA_VALUE);
        auto readerNoBuf = TileReader<int16_t>(tile, ds)
            .setNodata(DEM_NODATA_VALUE);
        const auto bufImg = reader.readBufferedImage();
        ASSERT_TRUE(bufImg);
        const auto bufImgNoBuf = readerNoBuf.readBufferedImage();
        EXPECT_GT(bufImg->width() + bufImg->height(), bufImgNoBuf->width() + bufImgNoBuf->height());
        const Int16Image tileImg = reader.clipBufferedImage(*bufImg);
        TDataset expected = OpenDataset(SRC_("data/dem_tiles/" + tileFileName(tile) + ".tif"));
        EXPECT_EQ(tileImg.maxAbsDifference(expected.Read<int16_t>()), 0);
    }
}

Y_UNIT_TEST(read_intersected_tiles_with_shift)
{
    TDataset ds = OpenDataset(DATASET_PATH);
    for (const auto& tile: INTERSECTED_TILES) {
        auto reader = TileReader<int16_t>(tile, ds)
            .setShift(SHIFT_METERS)
            .setNodata(DEM_NODATA_VALUE);
        const auto bufImg = reader.readBufferedImage();
        if (tile.y() == 6360) {
            ASSERT_FALSE(bufImg);
            continue; // Skip tiles that have become outside.
        }
        ASSERT_TRUE(bufImg);
        const Int16Image tileImg = reader.clipBufferedImage(*bufImg);
        TDataset expected = OpenDataset(SRC_("data/dem_tiles/" + tileFileName(tile) + "_shifted.tif"));
        EXPECT_EQ(tileImg.maxAbsDifference(expected.Read<int16_t>()), 0);
    }
}

Y_UNIT_TEST(read_intersected_tiles_with_shift_and_buffer)
{
    TDataset ds = OpenDataset(DATASET_PATH);
    for (const auto& tile: INTERSECTED_TILES) {
        auto reader = TileReader<int16_t>(tile, ds)
            .setBuffer(BUFFER_PIX)
            .setShift(SHIFT_METERS)
            .setNodata(DEM_NODATA_VALUE);
        auto readerNoBuf = TileReader<int16_t>(tile, ds)
            .setShift(SHIFT_METERS)
            .setNodata(DEM_NODATA_VALUE);
        const auto bufImg = reader.readBufferedImage();
        if (tile.y() == 6360) {
            ASSERT_FALSE(bufImg);
            continue; // Skip tiles that have become outside.
        }
        ASSERT_TRUE(bufImg);
        const auto bufImgNoBuf = readerNoBuf.readBufferedImage();
        EXPECT_GT(bufImg->width() + bufImg->height(), bufImgNoBuf->width() + bufImgNoBuf->height());
        const Int16Image tileImg = reader.clipBufferedImage(*bufImg);
        TDataset expected = OpenDataset(SRC_("data/dem_tiles/" + tileFileName(tile) + "_shifted.tif"));
        EXPECT_EQ(tileImg.maxAbsDifference(expected.Read<int16_t>()), 0);
    }
}

Y_UNIT_TEST(read_outised_tiles_with_shift_and_buffer)
{
    TDataset ds = OpenDataset(DATASET_PATH);
    for (const auto& tile: OUTSIDE_TILES) {
        auto reader = TileReader<int16_t>(tile, ds)
            .setBuffer(BUFFER_PIX)
            .setShift(SHIFT_METERS)
            .setNodata(DEM_NODATA_VALUE);
        const auto bufImg = reader.readBufferedImage();
        EXPECT_FALSE(bufImg);
    }
}

} //suite
Y_UNIT_TEST_SUITE(read_tile_should) {

Y_UNIT_TEST(read_intersected_tiles)
{
    TDataset ds = OpenDataset(DATASET_PATH);
    for (const auto& tile: INTERSECTED_TILES) {
        const auto img = readTile<int16_t>(ds, tile);
        TDataset expected = OpenDataset(SRC_("data/dem_tiles/" + tileFileName(tile) + ".tif"));
        EXPECT_EQ(img.maxAbsDifference(expected.Read<int16_t>()), 0);
    }
}

Y_UNIT_TEST(read_non_mercator_intersected_tiles)
{
    const auto resampleAlg = ResampleAlg::Cubic;
    TDataset ds = OpenDataset(DATASET_PATH);
    ds.setNodata(DEM_NODATA_VALUE);
    auto warpedTmp = MemoryFile::unique("warped.tif");
    TDataset warpedDs = CreateWarped(ds.ref(), warpedTmp.path())
        .setNodata(*ds.Nodata())
        .setResample(resampleAlg)
        .setTargetSrs(geodeticSr());
    warpedDs.deleteOnClose();
    for (const auto& tile: INTERSECTED_TILES) {
        const auto img = readTile<int16_t>(warpedDs, tile, resampleAlg);
        TDataset expected = OpenDataset(SRC_("data/dem_tiles/" + tileFileName(tile) + "_warped.tif"));
        EXPECT_EQ(img.maxAbsDifference(expected.Read<int16_t>()), 0);
    }
}

Y_UNIT_TEST(read_outside_tiles)
{
    TDataset ds = OpenDataset(DATASET_PATH);
    for (const auto& tile: OUTSIDE_TILES) {
        const auto img = readTile<int16_t>(ds, tile);
        EXPECT_TRUE(img.isConstant(DEM_NODATA_VALUE));
    }
}

} // suite
} //namespace maps::factory::dataset::tests

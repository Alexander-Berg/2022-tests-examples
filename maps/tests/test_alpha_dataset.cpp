#include <maps/factory/libs/dataset/alpha_dataset.h>

#include <maps/factory/libs/dataset/tiles.h>

#include <maps/factory/libs/geometry/tiles.h>
#include <maps/factory/libs/unittest/tests_common.h>

#include <maps/libs/tile/include/tile.h>
#include <maps/libs/common/include/file_utils.h>

namespace maps::factory::dataset::tests {
using namespace maps::factory::tests;

namespace {

const std::string srcDir = ArcadiaSourceRoot() + "/maps/factory/libs/image/tests/data/";

UInt8Image drawPolygonInTile(const tile::Tile& tile, const Geometry& poly)
{
    BlockCachePtr bc = std::make_shared<TLruBlockCache>();
    AlphaDataset ads(poly.copy(), "", tile.z(), bc, AlphaDataset::WithOverviews);
    ads.setBufferPix(-0.5);
    UInt8Image img({256, 256}, 1);
    ads.bestWorldMatch(tile.z())->ds().Read(img, displayBox(tile).cast<double>());
    return img;
}

}

Y_UNIT_TEST_SUITE(alpha_dataset_should) {

Y_UNIT_TEST(draw_polygon_in_tile_boundary)
{
    constexpr tile::Tile tile(78972, 50872, 17);
    Geometry geom = makePolygonGeometry(mercatorBox(tile), mercatorSr());
    UInt8Image img = drawPolygonInTile(tile, geom);
    EXPECT_LE(img.maxAbsDifference(UInt8Image::max(BLOCK_SIZES, 1)), 0)
                    << tileFileName(tile);
}

Y_UNIT_TEST(draw_polygon_out_tile_boundary)
{
    constexpr tile::Tile center(78972, 50872, 17);
    Geometry geom = makePolygonGeometry(mercatorBox(center), mercatorSr()).buffered(-1e-3);
    for (int i = -1; i <= 1; ++i) {
        for (int j = -1; j <= 1; ++j) {
            if (i == 0 && j == 0) { continue; }
            const tile::Tile tile(center.x() + i, center.y() + j, center.z());
            UInt8Image img = drawPolygonInTile(tile, geom);
            EXPECT_LE(img.maxAbsDifference(UInt8Image::zero(BLOCK_SIZES, 1)), 0);
        }
    }
}

Y_UNIT_TEST(draw_polygon_in_tile_diagonal)
{
    constexpr tile::Tile tile(78972, 50872, 17);
    const Box2d box = mercatorBox(tile);
    const Vector2d rt[]{
        {box.max().x(), box.min().y()},
        {box.max().x(), box.max().y()},
        {box.min().x(), box.max().y()},
    };
    const Vector2d lt[]{
        {box.min().x(), box.min().y()},
        {box.max().x(), box.max().y()},
        {box.min().x(), box.max().y()},
    };
    UInt8Image imgRt = drawPolygonInTile(tile, makePolygonGeometry(rt, mercatorSr()));
    UInt8Image imgLt = drawPolygonInTile(tile, makePolygonGeometry(lt, mercatorSr()));
    EXPECT_LE(imgRt.maxAbsDifference(UInt8Image::fromFile(srcDir + "draw_diagonal_poly_rt.png")), 0);
    EXPECT_LE(imgLt.maxAbsDifference(UInt8Image::fromFile(srcDir + "draw_diagonal_poly_lt.png")), 0);
}

Y_UNIT_TEST(draw_polygon_in_tile_convex)
{
    Geometry geom = Geometry::fromGeoJson(
        common::readFileToString(SRC_("data/boundary_convex.geojson")));
    geom.transformTo(mercatorSr());
    static const tile::Tile tiles[]{
        {308, 160, 9}, {308, 161, 9},
        {309, 160, 9}, {309, 161, 9},
        {154, 80, 8}, {38, 20, 6},
    };
    for (const auto& tile: tiles) {
        const UInt8Image img = drawPolygonInTile(tile, geom);
        EXPECT_LE(img.maxAbsDifference(
            UInt8Image::fromFile(srcDir + "draw_convex_poly_" + tileFileName(tile) + ".png")), 0);
    }
}

Y_UNIT_TEST(draw_polygon_in_tile_concave)
{
    Geometry geom = Geometry::fromGeoJson(
        common::readFileToString(SRC_("data/boundary.geojson")));
    geom.transformTo(mercatorSr());
    static const tile::Tile tiles[]{
        {308, 160, 9}, {308, 161, 9},
        {309, 160, 9}, {309, 161, 9},
        {154, 80, 8}, {38, 20, 6},
    };
    for (const auto& tile: tiles) {
        const UInt8Image img = drawPolygonInTile(tile, geom);
        EXPECT_LE(img.maxAbsDifference(
            UInt8Image::fromFile(srcDir + "draw_concave_poly_" + tileFileName(tile) + ".png")), 0);
    }
}

} // suite

} //namespace maps::factory::dataset::tests

#include "test_data.h"

#include <maps/factory/libs/rendering/tile_server.h>

#include <maps/factory/libs/common/eigen.h>
#include <maps/factory/libs/geometry/geometry.h>
#include <maps/factory/libs/geometry/geolib.h>
#include <maps/factory/libs/dataset/tiles.h>
#include <maps/factory/libs/dataset/dem_tile_dataset.h>
#include <maps/factory/libs/delivery/cog.h>
#include <maps/factory/libs/rendering/impl/patched_dem_tile_dataset.h>
#include <maps/factory/libs/storage/storage.h>
#include <maps/factory/libs/unittest/fixture.h>

#include <maps/libs/concurrent/include/threadpool.h>
#include <maps/libs/tile/include/utils.h>

#include <maps/factory/libs/unittest/tests_common.h>

namespace maps::factory::rendering::tests {
using namespace dataset;

namespace {

const int fixDemPath = factory::tests::changeGlobalDemPath();

constexpr tile::Tile tiles[]{
    {4935, 3179, 13},
    {9872, 6358, 14},
    {19742, 12717, 15},
    {39483, 25435, 16},
    {78972, 50872, 17},
    {157949, 101742, 18},
};

MosaicInfo testInfo()
{
    const auto mercatorGeom = toGeolibMultiPolygon(
        Geometry::fromGeoJson(delivery::Cog(*storage::storageFromDir(DG_COG)).contourGeoJson())
            .transformedTo(mercatorSr()));
    return MosaicInfo{.cogPath = DG_COG, .demPath = DEM_PATH, .mercatorGeom=mercatorGeom};
}

class TileFixture final : public unittest::Fixture {
public:
    TileFixture()
        : Fixture()
        , config_(config::Config::fromResource())
        , pgPool_(config_.sharedDbPool())
        , bc_(Shared<dataset::TLruBlockCache>(256_MB))
    {
        dataset::DemTileDataset::registerDriver(bc_, config_.demZoom(), "");
        PatchedDemTileDataset::registerDriver(
            bc_, pgPool_, config_.demZoom(), factory::tests::DEM_PATCH_TILES, "");
    }

    ~TileFixture() override
    {
        dataset::DemTileDataset::deregisterDriver();
        PatchedDemTileDataset::deregisterDriver();
    }

private:
    config::Config config_;
    std::shared_ptr<pgpool3::Pool> pgPool_;
    dataset::BlockCachePtr bc_;
};

} // namespace

Y_UNIT_TEST_SUITE(tile_server_should) {

Y_UNIT_TEST(render_tiles)
{
    TileFixture fixture;
    auto bc = Shared<TLruBlockCache>(6_GB);
    TileServer ts(bc);
    MosaicInfo info = testInfo();
    for (const tile::Tile& tile: tiles) {
        const UInt8Image res = ts.render({tile, {&info, 1}});
        const std::string exp = dgTilePath(tile);
        EXPECT_THAT(res, ImgNearFile(exp, 0.1)) << tileFileName(tile);
    }
}

Y_UNIT_TEST(render_with_small_cache)
{
    TileFixture fixture;
    auto bc = Shared<TLruBlockCache>(15000000);
    TileServer ts(bc);
    MosaicInfo info = testInfo();
    for (const tile::Tile& tile: tiles) {
        const UInt8Image res = ts.render({tile, {&info, 1}});
        const UInt8Image exp = UInt8Image::fromFile(dgTilePath(tile));
        EXPECT_LE(res.meanAbsDifference(exp), 0.25) << tile;
    }
}

Y_UNIT_TEST(render_concurrently)
{
    TileFixture fixture;
    concurrent::ThreadPool pool;

    auto bc = Shared<TLruBlockCache>(6_GB);
    TileServer ts(bc);
    MosaicInfo info = testInfo();
    const size_t concurentTests = 50;
    std::vector<std::future<std::tuple<tile::Tile, UInt8Image>>> futures;
    for (size_t testNum = 0; testNum != concurentTests; ++testNum) {
        for (int i = 0; i < 2; ++i) {
            for (const auto& tile: tiles) {
                futures.push_back(
                    pool.async(
                        [tile, &ts, &info] {
                            return std::make_tuple(tile, ts.render({tile, {&info, 1}}));
                        }));
            }
        }
    }

    for (auto& future: futures) {
        const auto&[tile, res] = future.get();
        const UInt8Image exp = UInt8Image::fromFile(dgTilePath(tile));
        EXPECT_LE(res.meanAbsDifference(exp), 0.25) << tile;
    }
}

Y_UNIT_TEST(shift_tile_raster)
{
    TileFixture fixture;
    auto bc = Shared<TLruBlockCache>(6_GB);
    TileServer ts(bc);
    MosaicInfo info = testInfo();
    const auto mercatorGeom = toGeom(info.mercatorGeom);

    const Array2i shifts[]{
        {-20, -10},
        {20, 10},
    };
    for (const Array2i& shift: shifts) {
        for (const tile::Tile& tile: tiles) {
            info.shiftMeters = shift.cast<double>() * maps::tile::zoomToResolution(tile.z());
            auto geom = mercatorGeom.copy();
            geom.shift(info.shiftMeters);
            info.mercatorGeom = toGeolibMultiPolygon(geom);

            const auto name = "shift_" + std::to_string(shift.x()) + "_" + std::to_string(shift.y()) + "_" +
                              tileFileName(tile);
            const UInt8Image res = ts.render({tile, {&info, 1}});
            const UInt8Image exp = UInt8Image::fromFile(DG_TILES_DIR + "/shift/" + name);
            EXPECT_LE(res.meanAbsDifference(exp), 0.25) << tile << " " << shift.transpose();
        }
    }
}

Y_UNIT_TEST(shift_tile_geom)
{
    TileFixture fixture;
    auto bc = Shared<TLruBlockCache>(6_GB);
    TileServer ts(bc);
    MosaicInfo info = testInfo();
    const Array2i shifts[]{
        {-20, -10},
        {20, 10},
    };
    for (const Array2i& shift: shifts) {
        for (const tile::Tile& tile: tiles) {
            info.shiftMeters = shift.cast<double>() * maps::tile::zoomToResolution(tile.z());
            info.geometryShiftMeters = info.shiftMeters;

            const auto name = "shift_" + std::to_string(shift.x()) + "_" + std::to_string(shift.y()) + "_" +
                              tileFileName(tile);
            const UInt8Image res = ts.render({tile, {&info, 1}});
            const UInt8Image exp = UInt8Image::fromFile(DG_TILES_DIR + "/shift/" + name);
            EXPECT_LE(res.meanAbsDifference(exp), 0.25) << tile << " " << shift.transpose();
        }
    }
}

Y_UNIT_TEST(unsharp_tile)
{
    TileFixture fixture;
    auto bc = Shared<TLruBlockCache>(6_GB);
    TileServer ts(bc);
    MosaicInfo info = testInfo();
    UnsharpOptions unsharp{.amount=5};
    info.parameters.add(parameter::UNSHARP_AMOUNT, unsharp.amount);
    for (const tile::Tile& tile: tiles) {
        UInt8Image res = ts.render({tile, {&info, 1}});
        UInt8Image exp = UInt8Image::fromFile(dgTilePath(tile));
        UInt8ImageBase bgr = exp.firstBandsImage(3);
        bgr.correctColorsInplace({}, unsharp);
        EXPECT_LE(res.firstBandsImage(3).meanAbsDifference(bgr), 0.5) << tile;
        EXPECT_LE(res.bandImage(3).maxAbsDifference(exp.bandImage(3)), 1) << tile;
    }
}

Y_UNIT_TEST(color_correct_tile)
{
    TileFixture fixture;
    auto bc = Shared<TLruBlockCache>(6_GB);
    TileServer ts(bc);
    MosaicInfo info = testInfo();
    ColorCorrectionOptions cco{.saturation = -0.5, .lightness=0.5};
    info.parameters.add(parameter::COLOR_SATURATION, cco.saturation);
    info.parameters.add(parameter::COLOR_LIGHTNESS, cco.lightness);
    for (const tile::Tile& tile: tiles) {
        UInt8Image res = ts.render({tile, {&info, 1}});
        UInt8Image exp = UInt8Image::fromFile(dgTilePath(tile));
        exp.firstBandsImage(3).correctColorsInplace(cco);
        EXPECT_LE(res.meanAbsDifference(exp), 0.1) << tile;
    }
}

Y_UNIT_TEST(edit_contour)
{
    TileFixture fixture;
    auto bc = Shared<TLruBlockCache>(6_GB);
    TileServer ts(bc);
    MosaicInfo info = testInfo();
    const auto original = info.mercatorGeom;
    for (const tile::Tile& tile: tiles) {
        const auto box = mercatorBox(tile);
        const auto part = toGeolibMultiPolygon(
            makePolygonGeometry(
                {box.min(), Vector2d(box.min().x(), box.max().x()), box.max()}));

        info.mercatorGeom = original;
        const UInt8Image res1 = ts.render({tile, {&info, 1}});

        info.mercatorGeom = part;
        const UInt8Image res2 = ts.render({tile, {&info, 1}});

        info.mercatorGeom = original;
        const UInt8Image res3 = ts.render({tile, {&info, 1}});

        info.mercatorGeom = part;
        const UInt8Image res4 = ts.render({tile, {&info, 1}});

        const auto name = "part_" + tileFileName(tile);
        const UInt8Image expFull = UInt8Image::fromFile(dgTilePath(tile));
        const UInt8Image expPart = UInt8Image::fromFile(DG_TILES_DIR + "/part/" + name);
        EXPECT_LE(res1.meanAbsDifference(expFull), 0.25) << tile;
        EXPECT_LE(res2.meanAbsDifference(expPart), 0.25) << tile;
        EXPECT_LE(res3.meanAbsDifference(expFull), 0.25) << tile;
        EXPECT_LE(res4.meanAbsDifference(expPart), 0.25) << tile;
    }
}

Y_UNIT_TEST(render_tile_with_background)
{
    TileFixture fixture;
    auto bc = Shared<TLruBlockCache>(6_GB);
    TileServer ts(bc, {}, SRC_("data/058800151040_01_TILES/prod_{x}_{y}_{z}.png"));
    const MosaicInfo info = testInfo();
    const tile::Tile& tile = tiles[0];
    const UInt8Image res = ts.render({tile, {&info, 1}});
    const std::string exp = SRC_("data/058800151040_01_TILES/with_background_4935_3179_13.png");
    EXPECT_THAT(res, ImgNearFile(exp, 0.1)) << tile;
}

Y_UNIT_TEST(render_tile_with_secret_region)
{
    TileFixture fixture;
    const MosaicInfo info = testInfo();
    for (const tile::Tile& tile: tiles) {
        const auto box = mercatorBox(tile);
        const auto part = toGeolibMultiPolygon(
            makePolygonGeometry(
                {box.min(), Vector2d(box.min().x(), box.max().x()), box.max()}));
        auto bc = Shared<TLruBlockCache>(6_GB);
        TileServer ts(bc);
        const UInt8Image res = ts.render({tile, {&info, 1}, SecretRegions("test", {part})});
        const std::string exp = SRC_("data/058800151040_01_TILES/secret/with_secret_" + tileFileName(tile));
        EXPECT_THAT(res, ImgNearFile(exp, 0.1)) << tile;
    }
}

Y_UNIT_TEST(render_rgb_tile)
{
    TileFixture fixture;
    auto bc = Shared<TLruBlockCache>(6_GB);
    TileServer ts(bc);

    const auto mercatorGeom = toGeolibMultiPolygon(
        Geometry::fromGeoJson(delivery::Cog(*storage::storageFromDir(SCANEX_COG)).contourGeoJson())
            .transformedTo(mercatorSr()));
    MosaicInfo info{.cogPath=SCANEX_COG, .mercatorGeom=mercatorGeom};
    constexpr tile::Tile rgbTiles[]{
        {34679, 23853, 16},
        {69359, 47704, 17}
    };
    for (const tile::Tile& tile: rgbTiles) {
        const UInt8Image res = ts.render({tile, {&info, 1}});
        const std::string exp = SCANEX_TILES_DIR + "/a_" + tileFileName(tile);
        EXPECT_THAT(res, ImgNearFile(exp, 0.1)) << tile;
    }
}

Y_UNIT_TEST(render_rgb_tile_with_corners_in_tile_borders)
{
    TileFixture fixture;
    auto bc = Shared<TLruBlockCache>(6_GB);
    TileServer ts(bc);
    const auto mercatorGeom = toGeolibMultiPolygon(
        Geometry::fromGeoJson(delivery::Cog(*storage::storageFromDir("./scanex_20201165")).contourGeoJson())
            .transformedTo(mercatorSr()));
    MosaicInfo info{.cogPath="./scanex_20201165", .mercatorGeom=mercatorGeom};
    constexpr tile::Tile rgbTiles[]{
        {676, 342, 10},
        {676, 343, 10}
    };
    for (const tile::Tile& tile: rgbTiles) {
        UInt8Image res = ts.render({tile, {&info, 1}});
        const std::string exp = SRC_("data/20201165_TILES/" + tileFileName(tile));
        EXPECT_THAT(res, ImgNearFile(exp, 0.1)) << tile;
    }
}

} // suite

} // maps::factory::rendering::tests

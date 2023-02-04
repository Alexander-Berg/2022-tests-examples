#include <library/cpp/testing/common/env.h>
#include <library/cpp/testing/gtest/gtest.h>

#include "fixture.h"
#include "test_data.h"

#include <maps/factory/libs/dataset/dem_tile_dataset.h>
#include <maps/factory/libs/dataset/tiles.h>
#include <maps/factory/libs/db/dem_release_gateway.h>
#include <maps/factory/libs/db/dem_patch_gateway.h>
#include <maps/factory/libs/geometry/tiles.h>

#include <maps/factory/libs/unittest/tests_common.h>

#include <maps/infra/yacare/include/test_utils.h>

namespace maps::factory::renderer::tests {
namespace {

const int fixDemPath = factory::tests::changeGlobalDemPath();

using image::UInt8Image;
using dataset::Int16Image;

using factory::tests::TEST_TILE1;
using factory::tests::TEST_PATCH1_TILES;

constexpr int DEM_ZOOM = 10;

db::DemRelease insertDemRelease(pqxx::connection& conn)
{
    pqxx::work txn(conn);
    db::DemRelease r("test_dem_release");
    db::DemReleaseGateway(txn).insert(r);
    txn.commit();
    return r;
}

db::DemPatch insertDemPatch(
    pqxx::connection& conn,
    const db::Id releaseId,
    geometry::MultiPoint3d patchData,
    db::IssuedTileMap tiles)
{
    const geometry::MercatorPixelPointBbox pointBox(DEM_ZOOM);
    pqxx::work txn(conn);
    db::DemPatch p("test_dem_patch", releaseId);
    p.extendBbox(patchData.bbox(pointBox));
    p.setTiles(tiles);
    db::DemPatchGateway(txn).insert(p);
    txn.commit();
    return p;
}

http::URL makeUrlRender(const std::string& handle, const tile::Tile& tile)
{
    return http::URL("http://localhost/v1/dem/render/" + handle)
        .addParam("x", tile.x()).addParam("y", tile.y()).addParam("z", tile.z());
}

} // namespace

using dem_render_should = SetupUserFixture;

TEST_F(dem_render_should, render_patch)
{
    pqxx::connection conn(postgres().connectionString());
    const auto patchData = geometry::MultiPoint3d::fromWktFile(factory::tests::TEST_PATCH1);
    const auto release = insertDemRelease(conn);
    const auto patch = insertDemPatch(
        conn, release.id(), patchData, TEST_PATCH1_TILES);

    http::MockRequest req(
        http::GET, makeUrlRender("patch", TEST_TILE1)
            .addParam("release_id", release.id())
            .addParam("fill", "#cb4154")
    );
    auto resp = yacare::performTestRequest(req);
    EXPECT_EQ(resp.status, 200);
    auto expected = UInt8Image::fromFile(SRC_("data/render_patch_616_397_10.png"));
    auto received = UInt8Image::fromBuffer(resp.body);
    EXPECT_LE(received.meanAbsDifference(expected), 0);
}

TEST_F(dem_render_should, render_patch_different_zoom)
{
    pqxx::connection conn(postgres().connectionString());
    const auto patchData = geometry::MultiPoint3d::fromWktFile(factory::tests::TEST_PATCH1);
    const auto release = insertDemRelease(conn);
    const auto patch = insertDemPatch(
        conn, release.id(), patchData, TEST_PATCH1_TILES);

    const auto tile = tile::Tile(
        TEST_TILE1.x() * 4,
        TEST_TILE1.y() * 4 + 1,
        TEST_TILE1.z() + 2);
    http::MockRequest req(
        http::GET, makeUrlRender("patch", tile)
            .addParam("release_id", release.id())
            .addParam("fill", "#cb4154")
    );
    auto resp = yacare::performTestRequest(req);
    EXPECT_EQ(resp.status, 200);
    auto expected = UInt8Image::fromFile(SRC_("data/render_patch_2464_1589_12.png"));
    auto received = UInt8Image::fromBuffer(resp.body);
    EXPECT_LE(received.meanAbsDifference(expected), 0);
}

TEST_F(dem_render_should, render_empty_patch)
{
    pqxx::connection conn(postgres().connectionString());
    const auto patchData = geometry::MultiPoint3d::fromWktFile(factory::tests::TEST_PATCH1);
    const auto release = insertDemRelease(conn);
    const auto patch = insertDemPatch(
        conn, release.id(), patchData, TEST_PATCH1_TILES);

    const tile::Tile tile(TEST_TILE1.x() + 100, TEST_TILE1.y(), TEST_TILE1.z());
    http::MockRequest req(
        http::GET, makeUrlRender("patch", tile)
            .addParam("release_id", release.id())
    );
    auto resp = yacare::performTestRequest(req);
    EXPECT_EQ(resp.status, 204);
}

TEST_F(dem_render_should, render_grayscale)
{
    http::MockRequest req(
        http::GET, makeUrlRender("grayscale", TEST_TILE1)
            .addParam("min", 500)
            .addParam("max", 1500)
    );
    auto resp = yacare::performTestRequest(req);
    EXPECT_EQ(resp.status, 200);
    auto expected = UInt8Image::fromFile(SRC_("data/render_grayscale_616_397_10.png"));
    auto received = UInt8Image::fromBuffer(resp.body);
    EXPECT_LE(received.meanAbsDifference(expected), 0);
}

TEST_F(dem_render_should, render_grayscale_with_patch)
{
    pqxx::connection conn(postgres().connectionString());
    const auto patchData = geometry::MultiPoint3d::fromWktFile(factory::tests::TEST_PATCH1);
    const auto release = insertDemRelease(conn);
    const auto patch = insertDemPatch(
        conn, release.id(), patchData, TEST_PATCH1_TILES);

    const auto tile = tile::Tile(
        TEST_TILE1.x() * 4,
        TEST_TILE1.y() * 4 + 1,
        TEST_TILE1.z() + 2);
    http::MockRequest req(
        http::GET, makeUrlRender("grayscale", tile)
            .addParam("min", 500)
            .addParam("max", 1500)
            .addParam("release_id", release.id())
    );
    auto resp = yacare::performTestRequest(req);
    ASSERT_EQ(resp.status, 200);
    auto expected = UInt8Image::fromFile(SRC_("data/render_grayscale_2464_1589_12.png"));
    auto received = UInt8Image::fromBuffer(resp.body);
    EXPECT_LE(received.meanAbsDifference(expected), 0);
}

TEST_F(dem_render_should, render_hillshade)
{
    http::MockRequest req(http::GET, makeUrlRender("hillshade", TEST_TILE1));
    auto resp = yacare::performTestRequest(req);
    EXPECT_EQ(resp.status, 200);
    auto expected = UInt8Image::fromFile(SRC_("data/render_hillshade_616_397_10.png"));
    auto received = UInt8Image::fromBuffer(resp.body);
    EXPECT_LE(received.meanAbsDifference(expected), 0);
}

TEST_F(dem_render_should, render_raw)
{
    http::MockRequest req(http::GET, makeUrlRender("raw", TEST_TILE1));
    auto resp = yacare::performTestRequest(req);
    EXPECT_EQ(resp.status, 200);

    const auto expected = dataset::TifDemTile::readImageFromFile(
        SRC_("data/render_raw_616_397_10.tiff").data());
    const auto received = dataset::TifDemTile::fromString(resp.body).img();
    EXPECT_LE(received.meanAbsDifference(expected), 0);
}

TEST_F(dem_render_should, consider_zoom_restriction)
{
    const auto tile = tile::Tile(
        TEST_TILE1.x(),
        TEST_TILE1.y(),
        configuration()->demZoom() - 3
    );
    {
        http::MockRequest req(http::GET, makeUrlRender("grayscale", tile)
            .addParam("min", 500)
            .addParam("max", 1500)
        );
        auto resp = yacare::performTestRequest(req);
        EXPECT_EQ(resp.status, 200);
        auto received = UInt8Image::fromBuffer(resp.body);
        EXPECT_TRUE(received.isLastBandBlack());
    }
    {
        http::MockRequest req(http::GET, makeUrlRender("hillshade", tile));
        auto resp = yacare::performTestRequest(req);
        EXPECT_EQ(resp.status, 200);
        auto received = UInt8Image::fromBuffer(resp.body);
        EXPECT_TRUE(received.isLastBandBlack());
    }
    {
        http::MockRequest req(
            http::GET, makeUrlRender("patch", tile)
        );
        auto resp = yacare::performTestRequest(req);
        EXPECT_EQ(resp.status, 200);
        auto received = UInt8Image::fromBuffer(resp.body);
        EXPECT_TRUE(received.isLastBandBlack());
    }
}

} // namespace maps::factory::renderer::tests

#include <library/cpp/testing/common/env.h>
#include <library/cpp/testing/gtest/gtest.h>

#include "fixture.h"
#include "test_data.h"

#include <maps/factory/libs/unittest/tests_common.h>
#include <maps/factory/libs/common/s3_keys.h>
#include <maps/factory/libs/db/dem_release_gateway.h>
#include <maps/factory/libs/db/dem_patch_gateway.h>
#include <maps/factory/libs/db/mosaic_gateway.h>
#include <maps/factory/libs/db/mosaic_source_gateway.h>
#include <maps/factory/libs/db/mosaic_source_pipeline.h>
#include <maps/factory/libs/db/release_gateway.h>
#include <maps/factory/libs/db/secret_object_gateway.h>
#include <maps/factory/libs/geometry/geolib.h>
#include <maps/factory/libs/dataset/tiles.h>
#include <maps/factory/libs/services/yacare.h>
#include <maps/factory/services/sputnica_back/lib/filter.h>

#include <maps/infra/yacare/include/request.h>
#include <maps/infra/yacare/include/test_utils.h>

#include <maps/infra/tvm2lua/ticket_parser2_lua/lib/lua_api.h>
#include <maps/libs/auth/include/tvm.h>
#include <maps/libs/common/include/file_utils.h>
#include <maps/libs/log8/include/log8.h>
#include <maps/libs/tile/include/geometry.h>

namespace maps::factory::renderer::tests {
using namespace services;

namespace {

const int fixDataFolders = factory::tests::fixGdalDataFolderForTests();

const int fixDemPath = factory::tests::changeGlobalDemPath();

using image::UInt8Image;

constexpr tile::Tile DG_TILE(78972, 50872, 17);

db::MosaicSource insertDgMosaicSource(pqxx::connection& conn)
{
    pqxx::work txn(conn);
    const db::MosaicSource result = makeNewDigitalGlobeMosaicSource(txn);
    txn.commit();
    return result;
}

db::Mosaic insertMosaic(pqxx::transaction_base& txn, const db::MosaicSource& ms)
{
    auto mosaic = db::Mosaic(
        ms.id(),
        1 /* zOrder */,
        MOSAIC_ZMIN,
        MOSAIC_ZMAX - 1,
        ms.mercatorGeom());
    db::MosaicGateway(txn).insert(mosaic);
    return mosaic;
}

db::Mosaic insertMosaic(pqxx::connection& conn, const db::MosaicSource& ms)
{
    pqxx::work txn(conn);
    auto mosaic = insertMosaic(txn, ms);
    txn.commit();
    return mosaic;
}

db::SecretObject insertSecretObject(pqxx::connection& conn)
{
    pqxx::work txn(conn);
    const auto box = dataset::mercatorBox(DG_TILE);
    auto geom = geometry::toGeolibMultiPolygon(dataset::makePolygonGeometry(
        {box.min(), Vector2d(box.min().x(), box.max().x()), box.max()}));
    auto secretObject = db::SecretObject("secret", geom);
    db::SecretObjectGateway(txn).insert(secretObject);
    txn.commit();
    return secretObject;
}

} // namespace

TEST_F(SetupUserFixture, rendering_should_empty_response_for_empty_filter)
{
    pqxx::connection conn(postgres().connectionString());
    db::MosaicSource ms = insertDgMosaicSource(conn);
    http::MockRequest req(http::GET, http::URL("http://localhost/v1/rendering/render_satellite")
        .addParam("x", DG_TILE.x()).addParam("y", DG_TILE.y()).addParam("z", DG_TILE.z())
    );
    auto resp = yacare::performTestRequest(req);
    EXPECT_EQ(resp.status, 204);
}

TEST_F(SetupUserFixture, rendering_should_rendering_should_empty_when_no_such_ms_found)
{
    pqxx::connection conn(postgres().connectionString());
    db::MosaicSource ms = insertDgMosaicSource(conn);
    http::MockRequest req(http::GET, http::URL("http://localhost/v1/rendering/render_satellite")
        .addParam("mosaic_source_ids", 999)
        .addParam("x", DG_TILE.x()).addParam("y", DG_TILE.y()).addParam("z", DG_TILE.z())
    );
    auto resp = yacare::performTestRequest(req);
    EXPECT_EQ(resp.status, 204);
}

TEST_F(SetupUserFixture, rendering_should_rendering_should_empty_when_tile_is_outside)
{
    pqxx::connection conn(postgres().connectionString());
    db::MosaicSource ms = insertDgMosaicSource(conn);
    http::MockRequest req(http::GET, http::URL("http://localhost/v1/rendering/render_satellite")
        .addParam("mosaic_source_ids", ms.id())
        .addParam("x", DG_TILE.x() + 100).addParam("y", DG_TILE.y() + 100).addParam("z", DG_TILE.z())
    );
    auto resp = yacare::performTestRequest(req);
    EXPECT_EQ(resp.status, 204);
}

TEST_F(SetupUserFixture, rendering_should_rendering_should_render_ms)
{
    pqxx::connection conn(postgres().connectionString());
    db::MosaicSource ms = insertDgMosaicSource(conn);
    http::MockRequest req(http::GET, http::URL("http://localhost/v1/rendering/render_satellite")
        .addParam("mosaic_source_ids", ms.id())
        .addParam("x", DG_TILE.x()).addParam("y", DG_TILE.y()).addParam("z", DG_TILE.z())
    );
    auto resp = yacare::performTestRequest(req);
    ASSERT_EQ(resp.status, 200);
    auto expected = UInt8Image::fromFile(
        ArcadiaSourceRoot() +
        "/maps/factory/libs/rendering/tests/data/058800151040_01_TILES/78972_50872_17.png");
    auto received = UInt8Image::fromBuffer(resp.body);
    EXPECT_LE(received.meanAbsDifference(expected), 1.5);
}

TEST_F(SetupUserFixture, rendering_should_rendering_should_render_with_color_correction)
{
    pqxx::connection conn(postgres().connectionString());
    db::MosaicSource ms = insertDgMosaicSource(conn);
    db::Mosaic m = insertMosaic(conn, ms);
    http::MockRequest req(http::GET, http::URL("http://localhost/v1/rendering/render_satellite")
        .addParam("mosaic_ids", m.id())
        .addParam("x", DG_TILE.x()).addParam("y", DG_TILE.y()).addParam("z", DG_TILE.z())
        .addParam("saturation", 75).addParam("lightness", -25)
        .addParam("unsharp_sigma", 2)
    );
    auto resp = yacare::performTestRequest(req);
    ASSERT_EQ(resp.status, 200);
    auto expected = UInt8Image::fromFile(SRC_("data/corrected_78972_50872_17.png"));
    auto received = UInt8Image::fromBuffer(resp.body);
    EXPECT_LE(received.meanAbsDifference(expected), 1.5);
}

TEST_F(SetupUserFixture, rendering_should_rendering_should_cache_using_etag)
{
    const UInt8Image expected = UInt8Image::fromFile(
        ArcadiaSourceRoot() +
        "/maps/factory/libs/rendering/tests/data/058800151040_01_TILES/78972_50872_17.png");
    pqxx::connection conn(postgres().connectionString());
    db::MosaicSource ms = insertDgMosaicSource(conn);
    db::Mosaic m = insertMosaic(conn, ms);
    http::MockRequest req(http::GET, http::URL("http://localhost/v1/rendering/render_satellite")
        .addParam("mosaic_ids", m.id())
        .addParam("x", DG_TILE.x()).addParam("y", DG_TILE.y()).addParam("z", DG_TILE.z())
    );
    {
        auto resp = yacare::performTestRequest(req);
        ASSERT_EQ(resp.status, 200);
        ASSERT_EQ(resp.headers.count(ETAG_HEADER), 1u);
        const std::string etag = resp.headers[ETAG_HEADER];
        EXPECT_FALSE(etag.empty());
        EXPECT_LE(UInt8Image::fromBuffer(resp.body).meanAbsDifference(expected), 1.5);
        req.headers["If-None-Match"] = etag;
        EXPECT_EQ(resp.headers.at("Cache-Control"), "no-cache");
    }
    {
        auto resp = yacare::performTestRequest(req);
        ASSERT_EQ(resp.status, 304);
    }
    {
        auto resp = yacare::performTestRequest(req);
        ASSERT_EQ(resp.status, 304);
    }
    {
        req.url.addParam("saturation", 75);
        auto resp = yacare::performTestRequest(req);
        ASSERT_EQ(resp.status, 200);
        ASSERT_EQ(resp.headers.count(ETAG_HEADER), 1u);
        EXPECT_NE(resp.headers[ETAG_HEADER], req.headers["If-None-Match"]);
        EXPECT_GT(UInt8Image::fromBuffer(resp.body).meanAbsDifference(expected), 1.5);
    }
}

TEST_F(SetupUserFixture, rendering_should_rendering_should_render_mosaic)
{
    pqxx::connection conn(postgres().connectionString());
    db::MosaicSource mosaicSource = insertDgMosaicSource(conn);
    db::Mosaic mosaic = insertMosaic(conn, mosaicSource);
    http::MockRequest req(http::GET, http::URL("http://localhost/v1/rendering/render_satellite")
        .addParam("mosaic_ids", mosaic.id())
        .addParam("x", DG_TILE.x()).addParam("y", DG_TILE.y()).addParam("z", DG_TILE.z())
    );
    auto resp = yacare::performTestRequest(req);
    ASSERT_EQ(resp.status, 200);
    auto expected = UInt8Image::fromFile(
        ArcadiaSourceRoot() +
        "/maps/factory/libs/rendering/tests/data/058800151040_01_TILES/78972_50872_17.png");
    auto received = UInt8Image::fromBuffer(resp.body);
    EXPECT_LE(received.meanAbsDifference(expected), 1.5);
}

TEST_F(SetupUserFixture, rendering_should_rendering_should_render_node_id)
{
    pqxx::connection conn(postgres().connectionString());
    db::MosaicSource mosaicSource = insertDgMosaicSource(conn);
    db::Mosaic mosaic = insertMosaic(conn, mosaicSource);
    pqxx::work txn(conn);
    db::MosaicSourcePipeline{txn}.transition(mosaicSource, db::MosaicSourceStatus::Ready);
    db::MosaicSourceGateway{txn}.update(mosaicSource);
    auto release = db::Release("draft_release").setStatus(db::ReleaseStatus::New);
    db::ReleaseGateway(txn).insert(release);
    mosaic.setReleaseId(release.id());
    db::MosaicGateway(txn).update(mosaic);
    txn.commit();

    std::vector<std::string> testNodeIds{"releases", "deliveries"};
    for (const auto& nodeId: testNodeIds) {
        http::MockRequest req(http::GET, http::URL("http://localhost/v1/rendering/render_satellite")
            .addParam("node_id", nodeId)
            .addParam("x", DG_TILE.x()).addParam("y", DG_TILE.y()).addParam("z", DG_TILE.z())
        );
        auto resp = yacare::performTestRequest(req);
        ASSERT_EQ(resp.status, 200);
        auto expected = UInt8Image::fromFile(
            ArcadiaSourceRoot() +
            "/maps/factory/libs/rendering/tests/data/058800151040_01_TILES/78972_50872_17.png");
        auto received = UInt8Image::fromBuffer(resp.body);
        EXPECT_LE(received.meanAbsDifference(expected), 1.5);
    }
}

TEST_F(SetupUserFixture, rendering_should_rendering_should_render_preview)
{
    pqxx::connection conn(postgres().connectionString());
    std::vector<db::MosaicSource> mosaicSources = {
        insertDgMosaicSource(conn), insertDgMosaicSource(conn)};
    std::vector<db::Mosaic> mosaics = {
        insertMosaic(conn, mosaicSources.at(0)), insertMosaic(conn, mosaicSources.at(1))};
    pqxx::work txn(conn);
    db::MosaicSourcePipeline{txn}.transition(mosaicSources[0], db::MosaicSourceStatus::Ready);
    db::MosaicSourcePipeline{txn}.transition(mosaicSources[1], db::MosaicSourceStatus::Ready);
    db::MosaicSourceGateway{txn}.update(mosaicSources);
    auto release = db::Release("draft_release").setStatus(db::ReleaseStatus::New);
    db::ReleaseGateway(txn).insert(release);
    mosaics.at(0).setReleaseId(release.id());
    mosaics.at(1).setReleaseId(release.id());
    db::MosaicGateway(txn).update(mosaics);
    txn.commit();

    std::vector<std::string> testNodeIds{"releases", "deliveries"};
    for (const auto& nodeId: testNodeIds) {
        SCOPED_TRACE(nodeId);
        http::MockRequest req(http::GET, http::URL("http://localhost/v1/rendering/render_satellite")
            .addParam("node_id", nodeId)
            .addParam("mosaic_ids", mosaics.at(0).id())
            .addParam("zindex_up", true)
            .addParam("shift_x", 100)
            .addParam("shift_y", -100)
            .addParam("x", 78972).addParam("y", 50868).addParam("z", DG_TILE.z())
        );
        auto resp = yacare::performTestRequest(req);
        ASSERT_EQ(resp.status, 200);
        auto expected = UInt8Image::fromFile(
            ArcadiaSourceRoot() +
            "/maps/factory/services/renderer/tests/data/" + nodeId + "_preview.png");
        auto received = UInt8Image::fromBuffer(resp.body);
        EXPECT_LE(received.meanAbsDifference(expected), 1.5);
    }
}

TEST_F(SetupUserFixture, rendering_should_rendering_should_render_color_correction)
{
    pqxx::connection conn(postgres().connectionString());
    db::MosaicSource mosaicSource = insertDgMosaicSource(conn);
    db::Mosaic mosaic = insertMosaic(conn, mosaicSource);
    http::MockRequest req(http::GET, http::URL("http://localhost/v1/rendering/render_satellite")
        .addParam("mosaic_ids", mosaic.id())
        .addParam("lightness", 50)
        .addParam("hue", 50)
        .addParam("saturation", 50)
        .addParam("x", DG_TILE.x()).addParam("y", DG_TILE.y()).addParam("z", DG_TILE.z())
    );
    auto resp = yacare::performTestRequest(req);
    ASSERT_EQ(resp.status, 200);
    auto expected = UInt8Image::fromFile(
        ArcadiaSourceRoot() +
        "/maps/factory/services/renderer/tests/data/true_corrected_image.png");
    auto received = UInt8Image::fromBuffer(resp.body);
    EXPECT_LE(received.meanAbsDifference(expected), 1.5);
}

TEST_F(SetupUserFixture, rendering_should_rendering_should_render_mosaic_with_secret)
{
    pqxx::connection conn(postgres().connectionString());
    db::MosaicSource mosaicSource = insertDgMosaicSource(conn);
    db::Mosaic mosaic = insertMosaic(conn, mosaicSource);
    db::SecretObject secretObject = insertSecretObject(conn);
    http::MockRequest req(http::GET, http::URL("http://localhost/v1/rendering/render_satellite")
        .addParam("mosaic_ids", mosaic.id())
        .addParam("x", DG_TILE.x()).addParam("y", DG_TILE.y()).addParam("z", DG_TILE.z())
        .addParam("blur_masking_zones", "1")
    );
    auto resp = yacare::performTestRequest(req);
    ASSERT_EQ(resp.status, 200);
    auto expected = UInt8Image::fromFile(
        ArcadiaSourceRoot() +
        "/maps/factory/services/renderer/tests/data/blurred_masking_zones_image.png");
    auto received = UInt8Image::fromBuffer(resp.body);
    EXPECT_LE(received.meanAbsDifference(expected), 1.5);
}

TEST_F(SetupUserFixture, rendering_should_rendering_should_secret_object_is_used_in_etag)
{
    pqxx::connection conn(postgres().connectionString());
    db::MosaicSource mosaicSource = insertDgMosaicSource(conn);
    db::Mosaic mosaic = insertMosaic(conn, mosaicSource);
    db::SecretObject secretObject = insertSecretObject(conn);
    http::MockRequest blurredReq(http::GET, http::URL("http://localhost/v1/rendering/render_satellite")
        .addParam("mosaic_ids", mosaic.id())
        .addParam("x", DG_TILE.x()).addParam("y", DG_TILE.y()).addParam("z", DG_TILE.z())
        .addParam("blur_masking_zones", "1")
    );
    auto blurredResp = yacare::performTestRequest(blurredReq);

    http::MockRequest plainReq(http::GET, http::URL("http://localhost/v1/rendering/render_satellite")
        .addParam("mosaic_ids", mosaic.id())
        .addParam("x", DG_TILE.x()).addParam("y", DG_TILE.y()).addParam("z", DG_TILE.z())
    );
    auto plainResp = yacare::performTestRequest(plainReq);

    EXPECT_NE(blurredResp.headers[ETAG_HEADER], plainResp.headers[ETAG_HEADER]);
}

TEST_F(SetupUserFixture, rendering_should_rendender_with_new_relief)
{
    json::Builder builder;
    builder << [&](maps::json::ObjectBuilder builder) {
        builder["dem"] << [&](maps::json::ObjectBuilder builder) {
            builder["zoom"] = 10;
            builder["patches"] = ArcadiaSourceRoot() +
                "/maps/factory/services/renderer/tests/data/dem_patches";
        };
    };
    config::Config config(json::Value::fromString(builder.str()));

    const auto curConfiguration =
        services::getConfigurationImpl<RendererConfiguration>();
    rendering::deregisterDemDrivers();
    rendering::registerDemDrivers(
        config,
        curConfiguration->blockCache(),
        curConfiguration->pgPool()
    );

    pqxx::connection conn(postgres().connectionString());
    std::vector<db::MosaicSource> mosaicSources = {
        insertDgMosaicSource(conn), insertDgMosaicSource(conn)};
    std::vector<db::Mosaic> mosaics = {
        insertMosaic(conn, mosaicSources.at(0)), insertMosaic(conn, mosaicSources.at(1))};
    pqxx::work txn(conn);
    db::MosaicSourcePipeline{txn}.transition(mosaicSources[0], db::MosaicSourceStatus::Ready);
    db::MosaicSourcePipeline{txn}.transition(mosaicSources[1], db::MosaicSourceStatus::Ready);
    db::MosaicSourceGateway{txn}.update(mosaicSources);
    auto release = db::Release("draft_release").setStatus(db::ReleaseStatus::New);
    db::ReleaseGateway(txn).insert(release);
    mosaics.at(0).setReleaseId(release.id());
    mosaics.at(1).setReleaseId(release.id());
    db::MosaicGateway(txn).update(mosaics);

    db::DemRelease demRelease("test_dem_release");
    db::DemReleaseGateway(txn).insert(demRelease);
    db::DemPatch demPatch("test_dem_patch", demRelease.id());
    const auto patch = geometry::MultiPoint3d::fromWkt(
        "MULTIPOINT ("
        "4108121.8 4484448.4 600,"
        "4108273.2 4484525.1 600)"
    );
    demPatch.extendBbox(patch.bbox());
    demPatch.setTiles({
        {{616, 397, 10}, 0}
    });
    db::DemPatchGateway(txn).insert(demPatch);
    txn.commit();

    std::vector<std::string> testNodeIds{"releases", "deliveries"};
    for (const auto& nodeId: testNodeIds) {
        http::MockRequest req(http::GET, http::URL("http://localhost/v1/rendering/render_satellite")
            .addParam("node_id", nodeId)
            .addParam("mosaic_ids", mosaics.at(0).id())
            .addParam("zindex_up", true)
            .addParam("x", 78972).addParam("y", 50868).addParam("z", DG_TILE.z())
            .addParam("dem_release_id", demRelease.id())
        );
        auto resp = yacare::performTestRequest(req);
        ASSERT_EQ(resp.status, 200);
        auto expected = UInt8Image::fromFile(
            ArcadiaSourceRoot() +
            "/maps/factory/services/renderer/tests/data/warped_sat_rendering.png");
        auto received = UInt8Image::fromBuffer(resp.body);
        EXPECT_LE(received.meanAbsDifference(expected), 1.5);
    }

    for (const auto& nodeId: testNodeIds) {
        http::MockRequest req(http::GET, http::URL("http://localhost/v1/rendering/render_satellite")
            .addParam("node_id", nodeId)
            .addParam("mosaic_ids", mosaics.at(0).id())
            .addParam("zindex_up", true)
            .addParam("x", 78972).addParam("y", 50868).addParam("z", DG_TILE.z())
        );
        auto resp = yacare::performTestRequest(req);
        ASSERT_EQ(resp.status, 200);
        auto expected = UInt8Image::fromFile(
            ArcadiaSourceRoot() +
            "/maps/factory/services/renderer/tests/data/sat_rendering.png");
        auto received = UInt8Image::fromBuffer(resp.body);
        EXPECT_LE(received.meanAbsDifference(expected), 1.5);
    }
}

TEST_F(RendererFixture, rendering_should_render_unauthorized)
{
    pqxx::connection conn(postgres().connectionString());
    db::MosaicSource ms = insertDgMosaicSource(conn);
    http::MockRequest req(http::GET, http::URL("http://localhost/v1/rendering/render_satellite")
        .addParam("mosaic_source_ids", ms.id())
        .addParam("x", DG_TILE.x()).addParam("y", DG_TILE.y()).addParam("z", DG_TILE.z())
    );
    auto resp = yacare::performTestRequest(req);
    ASSERT_EQ(resp.status, 401);
}

TEST_F(RendererFixture, rendering_should_render_access_without_rights)
{
    auth::UserInfo userInfo{};
    userInfo.setLogin(SetupUserFixture::USER_NAME);
    yacare::tests::UserInfoFixture userInfoFixture(userInfo);
    pqxx::connection conn(postgres().connectionString());
    db::MosaicSource ms = insertDgMosaicSource(conn);
    http::MockRequest req(http::GET, http::URL("http://localhost/v1/rendering/render_satellite")
        .addParam("mosaic_source_ids", ms.id())
        .addParam("x", DG_TILE.x()).addParam("y", DG_TILE.y()).addParam("z", DG_TILE.z())
    );
    auto resp = yacare::performTestRequest(req);
    ASSERT_EQ(resp.status, 403);
}

TEST_F(RendererFixture, rendering_should_render_internal_authorized)
{
    pqxx::connection conn(postgres().connectionString());
    db::MosaicSource ms = insertDgMosaicSource(conn);
    http::MockRequest req(http::GET, http::URL("http://localhost/v1/rendering/render_satellite_internal")
        .addParam("mosaic_source_ids", ms.id())
        .addParam("x", DG_TILE.x()).addParam("y", DG_TILE.y()).addParam("z", DG_TILE.z())
    );
    req.headers[maps::auth::SERVICE_TICKET_HEADER] = "fake";
    req.headers[maps::tvm2::SRC_TVM_ID_HEADER] = "maps-core-factory-renderer";
    auto resp = yacare::performTestRequest(req);
    ASSERT_EQ(resp.status, 200);
    auto expected = UInt8Image::fromFile(
        ArcadiaSourceRoot() +
        "/maps/factory/libs/rendering/tests/data/058800151040_01_TILES/78972_50872_17.png");
    auto received = UInt8Image::fromBuffer(resp.body);
    EXPECT_LE(received.meanAbsDifference(expected), 1.5);
}

} // namespace maps::factory::renderer::tests

#include <maps/factory/libs/processing/build_coverage.h>
#include <maps/factory/libs/processing/publication.h>
#include <maps/factory/libs/processing/render_tiles.h>

#include <maps/factory/libs/geometry/geolib.h>
#include <maps/factory/libs/image/image.h>
#include <maps/factory/libs/db/aoi_gateway.h>
#include <maps/factory/libs/db/delivery_gateway.h>
#include <maps/factory/libs/db/mosaic_gateway.h>
#include <maps/factory/libs/db/mosaic_source_gateway.h>
#include <maps/factory/libs/db/mosaic_source_pipeline.h>
#include <maps/factory/libs/db/order_gateway.h>
#include <maps/factory/libs/db/release_gateway.h>
#include <maps/factory/libs/db/source_gateway.h>
#include <maps/factory/libs/delivery/cog.h>
#include <maps/factory/libs/storage/local_storage.h>
#include <maps/factory/libs/tasks/impl/tasks_gateway.h>

#include <maps/factory/libs/processing/tests/test_context.h>
#include <maps/factory/libs/unittest/tests_common.h>
#include <maps/factory/libs/unittest/fixture.h>

namespace maps::factory::processing::tests {
using namespace testing;
using namespace maps::factory::tests;
using namespace maps::factory::storage;
using namespace maps::factory::geometry;
using namespace maps::factory::image;

namespace {

const std::string root = ArcadiaSourceRoot();
const std::string dgPath = root + "/maps/factory/test_data/dg_deliveries/058800151040_01";
const std::string cogPath = root + "/maps/factory/test_data/cog/dg_058800151040_01_P001";
const std::string demPath = root + "/maps/factory/test_data/dem/058800151040_01_DEM.TIF";

const double MOSAIC_RES = impl::ISRAEL_RESOLUTION_METERS_PER_PX_THRESHOLD / 2;
const std::string TEST_GEOID_COVERAGE_PATH = BinaryPath("maps/data/test/geoid/geoid.mms.1");
const auto ISRAEL_MOSAIC_GEOMETRY = geolib3::MultiPolygon2({
    geolib3::BoundingBox(
        {3868493.2899, 3747506.5309},
        {3874550.9243, 3753538.3168}
    ).polygon()
});

db::Release prepareRelease(TestContext& ctx)
{
    geolib3::MultiPolygon2 geom = toGeolibMultiPolygon(
        Geometry::fromGeoJson(delivery::Cog(*storage::storageFromDir(cogPath)).contourGeoJson())
            .transformedTo(mercatorSr()));

    db::Order order(2020, db::OrderType::Tasking);
    db::OrderGateway(ctx.transaction()).insert(order);

    db::Source source("test_source", db::SourceType::Local, "/");
    db::SourceGateway(ctx.transaction()).insert(source);

    db::Delivery delivery(source.id(), "2019-01-01", "test_delivery", "/");
    db::DeliveryGateway(ctx.transaction()).insert(delivery);

    db::Aoi aoi(order.id(), "Kahramanmaras_AC", geom.polygonAt(0));
    db::AoiGateway(ctx.transaction()).insert(aoi);

    db::MosaicSource ms("058800151040_01");
    ms.setDeliveryId(delivery.id());
    ms.setMercatorGeom(geolib3::MultiPolygon2{{geom}});
    ms.setMinZoom(10);
    ms.setMaxZoom(16);
    ms.setSatellite("WV03");
    ms.setOrderId(order.id());
    ms.setAoiId(aoi.id());
    ms.setCogPath(cogPath);
    db::MosaicSourcePipeline(ctx.transaction()).insertNew(ms);
    db::MosaicSourcePipeline(ctx.transaction()).transition(ms,
        db::MosaicSourceStatus::Ready, db::UserRole(unittest::TEST_CUSTOMER_USER_ID, db::Role::Customer));

    db::Release release = db::Release("Test_release")
        .setStatus(db::ReleaseStatus::Ready)
        .setIssue(1);
    db::ReleaseGateway(ctx.transaction()).insert(release);

    db::Mosaic mosaic(
        ms.id(),
        0,
        10,
        15,
        ms.mercatorGeom()
    );
    mosaic.setReleaseId(release.id());
    db::MosaicGateway(ctx.transaction()).insert(mosaic);
    ctx.commit();

    return release;
}

} // namespace

Y_UNIT_TEST_SUITE(publication_tasks_should) {

Y_UNIT_TEST(mark_release_published_to_testing)
{
    unittest::Fixture fixture;
    TestContext ctx(fixture.postgres().connectionString());
    db::Release release("SomeRelease");
    release.setStatus(db::ReleaseStatus::MovingToTesting);
    release.setIssue(1);
    db::ReleaseGateway(ctx.transaction()).insert(release);

    MarkReleasePublishedToTesting{}(ReleaseId(release.id()), ctx);

    db::ReleaseGateway(ctx.transaction()).reload(release);
    EXPECT_EQ(release.status(), db::ReleaseStatus::Testing);
}

Y_UNIT_TEST(mark_release_published_to_production)
{
    unittest::Fixture fixture;
    TestContext ctx(fixture.postgres().connectionString());
    db::Release release("SomeRelease");
    release.setStatus(db::ReleaseStatus::MovingToProduction);
    release.setIssue(1);
    db::ReleaseGateway(ctx.transaction()).insert(release);

    MarkReleasePublishedToProduction{}(ReleaseId(release.id()), ctx);

    db::ReleaseGateway(ctx.transaction()).reload(release);
    EXPECT_EQ(release.status(), db::ReleaseStatus::Production);
}

Y_UNIT_TEST(build_mobile_coverage)
{
    const auto tmpDir = localStorage("./tmp")->dir(this->Name_);
    BuildMobileCoverage{}(ReleaseVersion("1.2.3"), LocalDirectoryPath(tmpDir->absPathStr()));
    EXPECT_EQ(tmpDir->file(impl::MOBILE_COVERAGE_FILE_NAME)->readToString(),
        "{ \"version\" : \"1.2.3\" }\n");
}

Y_UNIT_TEST(render_release_tiles)
{
    unittest::Fixture fixture;
    TestContext ctx(fixture.postgres().connectionString());

    const db::Release release = prepareRelease(ctx);
    const auto outDir = localStorage("./tmp")->dir(this->Name_);

    const RenderReleaseTiles worker{
        .outputDirectory = outDir->absPath(),
        .tileNamePattern = "{x}_{y}_{z}.jpg",
        .demPath = demPath,
    };
    worker(ReleaseId(release.id()), ctx);

    const auto tilesDir = localStorage(SRC_("data/tiles").c_str());
    const auto paths = tilesDir->list(Select::Files);
    EXPECT_THAT(paths, Not(IsEmpty()));

    for (const auto& path: paths) {
        const auto expected = UInt8Image::fromFile(tilesDir->file(path)->absPathStr());
        const auto img = UInt8Image::fromFile(outDir->file(path)->absPathStr());
        EXPECT_LE(img.meanAbsDifference(expected), 2) << path;
    }
}

Y_UNIT_TEST(render_release_tiles_with_background)
{
    unittest::Fixture fixture;
    TestContext ctx(fixture.postgres().connectionString());

    const db::Release release = prepareRelease(ctx);
    const auto outDir = localStorage("./tmp")->dir(this->Name_);

    const RenderReleaseTiles worker{
        .outputDirectory = outDir->absPath(),
        .backgroundTilesPath = SRC_("data/background_tiles/{x}_{y}_{z}.png").c_str(),
        .tileNamePattern = "{x}_{y}_{z}.jpg",
        .demPath = demPath,
    };
    worker(ReleaseId(release.id()), ctx);

    const auto tilesDir = localStorage(SRC_("data/tiles_with_background/").c_str());
    const auto paths = tilesDir->list(Select::Files);
    EXPECT_THAT(paths, Not(IsEmpty()));

    for (const auto& path: paths) {
        const auto expected = UInt8Image::fromFile(tilesDir->file(path)->absPathStr());
        const auto img = UInt8Image::fromFile(outDir->file(path)->absPathStr());
        EXPECT_LE(img.meanAbsDifference(expected), 2) << path;
    }
}

Y_UNIT_TEST(render_release_tiles_with_repeating_mosaic)
{
    unittest::Fixture fixture;
    TestContext ctx(fixture.postgres().connectionString());

    const db::Release release = prepareRelease(ctx);
    const auto outDir = localStorage("./tmp")->dir(this->Name_);

    {
        auto mosaic = db::MosaicGateway(ctx.transaction()).load()[0];
        mosaic.setMinZoom(10);
        mosaic.setMaxZoom(13);
        db::MosaicGateway(ctx.transaction()).update(mosaic);

        db::Mosaic other(
            mosaic.mosaicSourceId(),
            mosaic.zOrder() + 1,
            mosaic.minZoom(),
            mosaic.maxZoom(),
            toGeolibMultiPolygon(
                toGeom(mosaic.mercatorGeom())
                    .shifted({0, -1000})
            )
        );
        other.setShift({0, -1000});
        other.setReleaseId(*mosaic.releaseId());
        db::MosaicGateway(ctx.transaction()).insert(other);
        ctx.commit();
    }

    const RenderReleaseTiles worker{
        .outputDirectory = outDir->absPath(),
        .tileNamePattern = "{x}_{y}_{z}.jpg",
        .demPath = demPath,
    };
    worker(ReleaseId(release.id()), ctx);

    const auto tilesDir = localStorage(SRC_("data/tiles_with_repeating_mosaic").c_str());
    const auto paths = tilesDir->list(Select::Files);
    EXPECT_THAT(paths, Not(IsEmpty()));

    for (const auto& path: paths) {
        const auto expected = UInt8Image::fromFile(tilesDir->file(path)->absPathStr());
        const auto img = UInt8Image::fromFile(outDir->file(path)->absPathStr());
        EXPECT_LE(img.meanAbsDifference(expected), 2) << path;
    }
}

Y_UNIT_TEST(render_release_tiles_with_404_background)
{
    unittest::Fixture fixture;
    TestContext ctx(fixture.postgres().connectionString());

    const db::Release release = prepareRelease(ctx);
    const auto outDir = localStorage("./tmp")->dir(this->Name_)->dir("out");
    const auto emptyDir = localStorage("./tmp")->dir(this->Name_)->dir("empty");

    const RenderReleaseTiles worker{
        .outputDirectory = outDir->absPath(),
        .backgroundTilesPath = emptyDir->absPathStr(),
        .tileNamePattern = "{x}_{y}_{z}.jpg",
        .demPath = demPath,
    };
    worker(ReleaseId(release.id()), ctx);

    const auto tilesDir = localStorage(SRC_("data/tiles_with_404_background/").c_str());
    const auto paths = tilesDir->list(Select::Files);
    EXPECT_THAT(paths, Not(IsEmpty()));

    for (const auto& path: paths) {
        const auto expected = UInt8Image::fromFile(tilesDir->file(path)->absPathStr());
        const auto img = UInt8Image::fromFile(outDir->file(path)->absPathStr());
        EXPECT_LE(img.meanAbsDifference(expected), 2) << path;
    }
}

Y_UNIT_TEST(remove_release_tiles)
{
    unittest::Fixture fixture;
    TestContext ctx(fixture.postgres().connectionString());

    const db::Release release = prepareRelease(ctx);
    const auto outDir = localStorage("./tmp")->dir(this->Name_);

    const auto tilesDir = localStorage(SRC_("data/tiles").c_str());
    tilesDir->copyAll(*outDir);
    EXPECT_THAT(outDir->list(Select::Files), Not(IsEmpty()));
    outDir->file("do_not_remove.jpg")->writeString("do not remove");

    const RemoveReleaseTiles worker{
        .outputDirectory = outDir->absPath(),
        .tileNamePattern = "{x}_{y}_{z}.jpg",
    };
    worker(ReleaseId(release.id()), ctx);

    EXPECT_THAT(outDir->list(Select::Files), ElementsAre("do_not_remove.jpg"));
}

Y_UNIT_TEST(catch_high_res_israel_images)
{
    unittest::Fixture fixture;
    TestContext ctx(fixture.postgres().connectionString());
    db::Release release("SomeRelease");
    {
        release.setStatus(db::ReleaseStatus::MovingToTesting);
        release.setIssue(1);
        db::ReleaseGateway(ctx.transaction()).insert(release);

        db::MosaicSource ms("SomeMosaicSource");
        ms.setMercatorGeom(ISRAEL_MOSAIC_GEOMETRY);
        ms.setResolutionMeterPerPx(MOSAIC_RES);
        db::MosaicSourceGateway(ctx.transaction()).insert(ms);

        db::Mosaic mosaic(
            ms.id(),
            0,
            10,
            15,
            ms.mercatorGeom()
        );
        mosaic.setReleaseId(release.id());
        db::MosaicGateway(ctx.transaction()).insert(mosaic);
    }

    EXPECT_TRUE(
        impl::hasHighResIsraelImages(
            release,
            ctx.transaction(),
            TEST_GEOID_COVERAGE_PATH
        )
    );
}

} //suite
} //namespace maps::factory::processing::tests

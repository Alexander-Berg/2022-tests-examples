#include <maps/factory/libs/processing/accept_scanex.h>

#include <maps/factory/libs/db/delivery_gateway.h>
#include <maps/factory/libs/db/mosaic_gateway.h>
#include <maps/factory/libs/db/mosaic_source_gateway.h>
#include <maps/factory/libs/db/mosaic_source_status_event_gateway.h>
#include <maps/factory/libs/db/rgb_product_gateway.h>
#include <maps/factory/libs/processing/cloud_optimize.h>
#include <maps/factory/libs/processing/tests/test_context.h>
#include <maps/factory/libs/storage/local_storage.h>
#include <maps/factory/libs/tasks/impl/tasks_gateway.h>

#include <maps/factory/libs/unittest/tests_common.h>
#include <maps/factory/libs/unittest/fixture.h>

namespace maps::factory::processing::tests {
using namespace testing;
using namespace maps::factory::tests;
using namespace maps::factory::storage;
using namespace maps::factory::delivery;

namespace {
const std::string DL_PATH =
    ArcadiaSourceRoot() + "/maps/factory/test_data/scanex_deliveries/11123809_extracted_mini";
} // namespace

Y_UNIT_TEST_SUITE(accept_scanex_should) {

Y_UNIT_TEST(collect_metadata)
{
    const ScanexDelivery dl(storage::localStorage(DL_PATH));
    const auto val = impl::collectMetadata(dl.metadata());
    EXPECT_EQ(val["clouds"].as<std::string>(), "0.000000");
    EXPECT_EQ(val["datetime"].as<std::string>(), "2017-08-09 03:16:14");
    EXPECT_EQ(val["image_id"].as<std::string>(),
        "AS_SP_51742_1_623_SO17016225-94-01_DS_SPOT6_201708090316140_FR1_FR1_SV1_SV1_E112N60_01871");
    EXPECT_EQ(val["strip_id"].as<std::string>(), "DS_SPOT6_201708090316140_FR1_FR1_SV1_SV1_E112N60_01871");
    EXPECT_EQ(val["sun_azum"].as<std::string>(), "153.008632");
    EXPECT_EQ(val["sun_elev"].as<std::string>(), "43.893939");
    EXPECT_EQ(val["azim_angle"].as<std::string>(), "-2.495224");
    EXPECT_EQ(val["elev"].as<std::string>(), "15.563884");
    EXPECT_EQ(val["satellite"].as<std::string>(), "SPOT-6");
}

Y_UNIT_TEST(get_s3_directory_name)
{
    const ScanexDelivery dl(storage::localStorage(DL_PATH));
    const auto name = impl::s3DirectoryName(123, dl.metadata());
    EXPECT_EQ(name, "123-SPOT6_PMS_201708090316140_ORT_2438229101_X1.Y2");
}

Y_UNIT_TEST(load_deliveries_id_with_url)
{
    unittest::Fixture fixture;
    TestContext ctx(fixture.postgres().connectionString());

    db::Delivery dl({}, "01.01.2021", "test", "");
    const fs::path path = "some/test/path";

    db::DeliveryGateway(ctx.transaction()).insert(dl);
    ctx.commit();
    EXPECT_THAT(impl::loadDeliveryIdsToUrls(ctx.pool()), IsEmpty());

    dl.setDownloadUrl(path.native());
    db::DeliveryGateway(ctx.transaction()).update(dl);
    ctx.commit();
    EXPECT_THAT(impl::loadDeliveryIdsToUrls(ctx.pool()), IsEmpty());

    dl.enable();
    db::DeliveryGateway(ctx.transaction()).update(dl);
    ctx.commit();
    const auto dls = impl::loadDeliveryIdsToUrls(ctx.pool());
    EXPECT_EQ(dls.front().id, dl.id());
    EXPECT_EQ(dls.front().url.native(), path);
}

Y_UNIT_TEST(try_list_directories)
{
    const auto local = storage::localStorage("./tmp")->dir(this->Name_);
    local->dir("dir")->file(".tmp")->touch();
    const auto res = impl::tryListDirectories(local);
    ASSERT_EQ(res.size(), 1u);
    EXPECT_EQ(res[0]->absPathStr(), local->dir("dir")->absPathStr());
}

Y_UNIT_TEST(check_product_uploaded)
{
    unittest::Fixture fixture;
    TestContext ctx(fixture.postgres().connectionString());
    const auto local = storage::localStorage("./tmp")->dir(this->Name_);
    EXPECT_FALSE(impl::isScanexProductUploaded(ctx.pool(), local));
    db::RgbProductGateway(ctx.transaction())
        .insert(db::RgbProduct(local->absPathStr(), {}, db::RgbProductType::Scanex));
    ctx.commit();
    EXPECT_TRUE(impl::isScanexProductUploaded(ctx.pool(), local));

}

Y_UNIT_TEST(register_product)
{
    unittest::Fixture fixture;
    TestContext ctx(fixture.postgres().connectionString());
    const auto local = storage::localStorage("./tmp")->dir(this->Name_);
    const auto ftpDir = local->dir("ftp");
    const auto dstDir = local->dir("dst");

    const int64_t dbDeliveryId = 1;
    impl::registerScanexProduct(ctx.pool(), dbDeliveryId, ftpDir, dstDir);

    const auto products = db::RgbProductGateway(ctx.transaction()).load();
    ASSERT_EQ(products.size(), 1u);
    EXPECT_EQ(products[0].path(), dstDir->absPathStr());
    EXPECT_EQ(products[0].type(), db::RgbProductType::Scanex);
    EXPECT_TRUE(impl::isScanexProductUploaded(ctx.pool(), dstDir));

    const auto tasks = tasks::TasksGateway(ctx.transaction())
        .load(sql_chemistry::orderBy(tasks::table::Task::name));
    ASSERT_EQ(tasks.size(), 1u);
    EXPECT_EQ(tasks[0].name(), ProcessNewScanexDelivery::name);
}

Y_UNIT_TEST(download_product)
{
    unittest::Fixture fixture;
    TestContext ctx(fixture.postgres().connectionString());
    const auto local = storage::localStorage("./tmp")->dir(this->Name_);
    const auto ftpDir = local->dir("ftp");
    const auto dstDir = local->dir("dst");
    const auto tmpDir = local->dir("tmp");
    const int64_t deliveryId = 42;
    const auto ftpDl = ScanexDelivery(storage::localStorage(DL_PATH)).copyTo(ftpDir);
    const auto resultDir = dstDir->dir("42-SPOT6_PMS_201708090316140_ORT_2438229101_X1.Y2");

    // Not ready
    impl::downloadProduct(ctx.pool(), dstDir, tmpDir, deliveryId, ftpDir);
    EXPECT_EQ(tmpDir->list(Select::FilesRecursive).size(), 0u);
    EXPECT_EQ(dstDir->list(Select::FilesRecursive).size(), 0u);
    EXPECT_FALSE(ftpDir->file("mosaic.kill")->exists());
    EXPECT_FALSE(ftpDir->file("mosaic.broken")->exists());
    EXPECT_EQ(db::RgbProductGateway(*ctx.readOnlyTransaction()).count(), 0u);

    // Ready
    ftpDl.createReadyFile();
    impl::downloadProduct(ctx.pool(), dstDir, tmpDir, deliveryId, ftpDir);
    EXPECT_EQ(tmpDir->list(Select::FilesRecursive).size(), 0u);
    EXPECT_THAT(toStrings(resultDir->list(Select::Files)), ElementsAre(
        "SPOT6_PMS_201708090316140_ORT_2438229101_X1.Y2.TIF",
        "SPOT6_PMS_201708090316140_ORT_2438229101_X1.Y2.TIF.md5",
        "SPOT6_PMS_201708090316140_ORT_2438229101_X1.Y2.mid",
        "SPOT6_PMS_201708090316140_ORT_2438229101_X1.Y2.mid.md5",
        "SPOT6_PMS_201708090316140_ORT_2438229101_X1.Y2.mif",
        "SPOT6_PMS_201708090316140_ORT_2438229101_X1.Y2.mif.md5"
    ));
    EXPECT_TRUE(ftpDir->file("mosaic.kill")->exists());
    EXPECT_FALSE(ftpDir->file("mosaic.broken")->exists());
    const auto products = db::RgbProductGateway(*ctx.readOnlyTransaction()).load();
    ASSERT_EQ(products.size(), 1u);
    EXPECT_EQ(products[0].path(), resultDir->absPathStr());

    // Already downloaded
    dstDir->removeAll();
    impl::downloadProduct(ctx.pool(), dstDir, tmpDir, deliveryId, ftpDir);
    EXPECT_EQ(tmpDir->list(Select::FilesRecursive).size(), 0u);
    EXPECT_EQ(dstDir->list(Select::FilesRecursive).size(), 0u);
    EXPECT_TRUE(ftpDir->file("mosaic.kill")->exists());
    EXPECT_FALSE(ftpDir->file("mosaic.broken")->exists());
    EXPECT_EQ(db::RgbProductGateway(*ctx.readOnlyTransaction()).count(), 1u);
}

Y_UNIT_TEST(process_new_delivery)
{
    unittest::Fixture fixture;
    TestContext ctx(fixture.postgres().connectionString());
    const int64_t deliveryId = 42;
    const std::string name = "SPOT6_PMS_201708090316140_ORT_2438229101_X1.Y2";
    const auto local = storage::localStorage("./tmp")->dir(this->Name_)
                                                     ->dir(std::to_string(deliveryId) + "-" + name);
    ScanexDelivery(storage::localStorage(DL_PATH)).copyTo(local);
    db::RgbProductGateway(ctx.transaction())
        .insert(db::RgbProduct(local->absPathStr(), {}, db::RgbProductType::Scanex));
    ctx.commit();

    const ProcessNewScanexDelivery worker{.minZoom = 2};
    worker(S3DirectoryPath(local->absPathStr()), DbDeliveryId(deliveryId), ctx);

    ASSERT_EQ(ctx.tasks().size(), 1u);
    EXPECT_EQ(ctx.tasks()[0].name(), CloudOptimizeScanexDelivery::name);

    const auto products = db::RgbProductGateway(ctx.transaction()).load();
    const auto sources = db::MosaicSourceGateway(ctx.transaction()).load();
    const auto mosaics = db::MosaicGateway(ctx.transaction()).load();
    const auto events = db::MosaicSourceStatusEventGateway(ctx.transaction()).load();
    ASSERT_EQ(products.size(), 1u);
    ASSERT_EQ(sources.size(), 1u);
    ASSERT_EQ(mosaics.size(), 1u);
    ASSERT_EQ(events.size(), 1u);

    EXPECT_EQ(products[0].mosaicSourceId(), sources[0].id());
    EXPECT_EQ(sources[0].name(), name);
    EXPECT_EQ(sources[0].status(), db::MosaicSourceStatus::New);
    EXPECT_GT(sources[0].mercatorGeom().area(), 0);
    EXPECT_EQ(sources[0].minZoom(), 2u);
    EXPECT_EQ(sources[0].maxZoom(), 6u);
    EXPECT_EQ(sources[0].deliveryId(), deliveryId);
    EXPECT_FALSE(sources[0].orderId());
    EXPECT_FALSE(sources[0].aoiId());
    EXPECT_FALSE(sources[0].projectId());
    EXPECT_EQ(sources[0].satellite(), "SPOT-6");
    EXPECT_FALSE(sources[0].cogPath());
    EXPECT_NEAR(sources[0].resolutionMeterPerPx().value(), 0.035555556, 1e-3);
    EXPECT_NEAR(sources[0].offnadir().value(), 15.563884, 1e-3);
    EXPECT_NEAR(sources[0].heading().value(), -2.495224, 1e-3);
    EXPECT_EQ(sources[0].collectionDate(), chrono::parseSqlDateTime("2017-08-09 03:16:14"));

    EXPECT_EQ(mosaics[0].mosaicSourceId(), sources[0].id());
    EXPECT_FALSE(mosaics[0].releaseId());
    EXPECT_EQ(mosaics[0].zOrder(), 1);
    EXPECT_EQ(mosaics[0].minZoom(), 2u);
    EXPECT_EQ(mosaics[0].maxZoom(), 7u);
    EXPECT_GT(mosaics[0].mercatorGeom().area(), 0);
}

} // suite

} //namespace maps::factory::delivery::tests

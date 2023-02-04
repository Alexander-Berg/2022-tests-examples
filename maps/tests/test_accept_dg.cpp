#include <maps/factory/libs/processing/accept_dg.h>

#include <maps/factory/libs/db/aoi_gateway.h>
#include <maps/factory/libs/db/delivery_gateway.h>
#include <maps/factory/libs/db/dg_delivery_gateway.h>
#include <maps/factory/libs/db/mosaic_gateway.h>
#include <maps/factory/libs/db/mosaic_source_gateway.h>
#include <maps/factory/libs/db/mosaic_source_status_event_gateway.h>
#include <maps/factory/libs/db/order_gateway.h>
#include <maps/factory/libs/db/project_gateway.h>
#include <maps/factory/libs/db/source_gateway.h>
#include <maps/factory/libs/delivery/cog.h>
#include <maps/factory/libs/geometry/geolib.h>
#include <maps/factory/libs/processing/cloud_optimize.h>
#include <maps/factory/libs/processing/tests/test_context.h>
#include <maps/factory/libs/processing/tests/test_s3.h>
#include <maps/factory/libs/storage/local_storage.h>
#include <maps/factory/libs/tasks/generic_worker.h>
#include <maps/factory/libs/tasks/team.h>
#include <maps/factory/libs/unittest/fixture.h>
#include <maps/factory/libs/unittest/tests_common.h>

namespace maps::factory::processing::tests {
using namespace testing;
using namespace maps::factory::tests;
using namespace maps::factory::storage;

Y_UNIT_TEST_SUITE(accept_dg_tasks_should) {

const std::string dataDgPath =
    ArcadiaSourceRoot() + "/maps/factory/test_data/dg_deliveries/058800151040_01";
const std::string dataCogPath =
    ArcadiaSourceRoot() + "/maps/factory/test_data/cog/dg_058800151040_01_P001";
const std::string dataDemPath =
    ArcadiaSourceRoot() + "/maps/factory/test_data/dem/058800151040_01_DEM.TIF";
const std::string dbDeliveryName = "DIGITAL_GLOBE_NEW";

static geolib3::Polygon2 deliveryContour()
{
    return delivery::Cog(*localStorage(dataCogPath)).contourMercator().polygonAt(0);
}

static void setupDeliveryData(sql_chemistry::Transaction& txn, const delivery::DgDelivery& dl)
{
    db::Order order(2019, db::OrderType::Tasking);
    db::OrderGateway(txn).insert(order);

    db::Aoi aoi(order.id(), dl.areaDescription(), deliveryContour());
    db::AoiGateway(txn).insert(aoi);

    db::Source source("source", db::SourceType::DigitalGlobe, "/");
    db::SourceGateway(txn).insert(source);

    db::DeliveryGateway(txn).insert(
        db::Delivery(source.id(), "2019-01-01", dbDeliveryName, "/"));
}

Y_UNIT_TEST(warp_contour)
{
    const auto local = localStorage("./tmp")->dir(Name_);
    localStorage(dataCogPath)->copyAll(*local);

    const delivery::Cog cog(*local);
    const auto warped = impl::warpContour(cog, dataDemPath);
    auto geomArea = warped.area();
    auto originalArea = cog.contourMercator().area();

    EXPECT_GT(originalArea, 0.0);
    EXPECT_GT(geomArea, 0.0);
    EXPECT_NE(geomArea, originalArea);
    EXPECT_NEAR(geomArea, originalArea, 1e6);
}

Y_UNIT_TEST(download_product)
{
    const auto s3Root = testS3(this->Name_);
    const auto s3 = s3Root->dir("delivery_path");
    const delivery::DgDelivery dl(localStorage(dataDgPath));
    dl.copyIndexedTo(s3);
    const auto local = localStorage("./tmp")->dir(this->Name_);

    unittest::Fixture fixture;
    TestContext context(fixture.postgres().connectionString());
    const DownloadProductFromS3 worker{.pool = S3Pool{testS3Auth()}};

    EXPECT_THROW(worker(
        LocalDirectoryPath(local->absPath().native()), processing::DgProductId(dl.products().at(0).id().id()),
        context), RuntimeError);

    db::DgDeliveryGateway(context.transaction()).insert(
        db::DgDelivery(dl.orderNumber(), s3->absPath().native()));

    worker(LocalDirectoryPath(local->absPath().native()), DgProductId(dl.products().at(0).id().id()),
        context);

    const delivery::DgDelivery localDl(local);
    EXPECT_THAT(toStrings(localDl.files(delivery::DgFiles::Missing)), IsEmpty());
}

Y_UNIT_TEST(remove_delivery)
{
    const auto s3Root = testS3(this->Name_);
    const auto s3Push = s3Root->dir("push_dir");
    const auto s3Src = s3Root->dir("source_dir");
    const delivery::DgDelivery dl(localStorage(dataDgPath));
    dl.copyIndexedTo(s3Push);

    unittest::Fixture fixture;
    TestContext context(fixture.postgres().connectionString());
    const RemoveCopiedDgDelivery worker{.pool = S3Pool{testS3Auth()}};

    EXPECT_THROW(worker(S3DirectoryPath(s3Push->absPath().native()), context), RuntimeError);

    db::DgDeliveryGateway(context.transaction()).insert(
        db::DgDelivery(dl.orderNumber(), s3Src->absPath().native()));

    EXPECT_THROW(worker(S3DirectoryPath(s3Push->absPath().native()), context), RuntimeError);

    dl.copyIndexedTo(s3Src);

    worker(S3DirectoryPath(s3Push->absPath().native()), context);
    EXPECT_THAT(toStrings(s3Push->list(Select::FilesRecursive)), IsEmpty());
}

Y_UNIT_TEST(find_aoi)
{
    unittest::Fixture fixture;
    pqxx::connection conn(fixture.postgres().connectionString());
    pqxx::work txn(conn);
    db::Order order(2000, db::OrderType::Tasking);
    db::OrderGateway(txn).insert(order);
    constexpr auto aoiName = "some_aoi";
    const auto contour = deliveryContour();
    const auto contourGeom = geometry::toGeom(contour);
    EXPECT_THROW(Y_UNUSED(db::AoiGateway(txn).bestMatch(order.id(), aoiName, contourGeom)), RuntimeError);
    db::Aoi aoi(order.id(), aoiName, contour);
    db::AoiGateway(txn).insert(aoi);
    EXPECT_EQ(db::AoiGateway(txn).bestMatch(order.id(), aoiName, contourGeom).id(), aoi.id());
}

Y_UNIT_TEST(ensure_project_exists)
{
    using namespace db::table::alias;
    unittest::Fixture fixture;
    pqxx::connection conn(fixture.postgres().connectionString());
    pqxx::work txn(conn);
    constexpr auto project = "some_project";
    db::ProjectGateway(txn).ensureExists(project);
    EXPECT_EQ(db::ProjectGateway(txn).count(_Project::id == project), 1u);
    db::ProjectGateway(txn).ensureExists(project);
    EXPECT_EQ(db::ProjectGateway(txn).count(_Project::id == project), 1u);
}

Y_UNIT_TEST(accept_dg_delivery)
{
    using namespace db::table::alias;

    const auto local = localStorage("./tmp")->dir(this->Name_);
    localStorage(dataCogPath)->copyAll(*local);

    const auto s3Root = testS3(this->Name_);
    const auto s3DlDir = s3Root->dir("source_dir");
    const auto s3CogDir = s3Root->dir("cog_path");
    auto dl = delivery::DgDelivery(localStorage(dataDgPath)).copyIndexedTo(s3DlDir);

    const auto prod = dl.products().at(0);
    const delivery::Cog cog(*local);

    unittest::Fixture fixture;
    TestContext context(fixture.postgres().connectionString());

    setupDeliveryData(context.transaction(), dl);
    auto dbDl = db::DgDelivery(dl.orderNumber(), dl.storage().absPath().native());
    dbDl.setOrderId(dl.parseOrderId().value());
    dbDl.setAreaDescription(dl.areaDescription());
    db::DgDeliveryGateway(context.transaction()).insert(dbDl);

    const AcceptDgDelivery worker{
        .s3CogDir = s3CogDir->absPath(),
        .demPath = dataDemPath,
        .dbDeliveryName = dbDeliveryName,
        .pool = S3Pool{testS3Auth()},
    };

    const MosaicSourceId msId = worker(
        DgProductId{prod.id().id()},
        LocalDirectoryPath{local->absPath().native()},
        context);

    EXPECT_EQ(msId.val(), 1);
    const auto ms = db::MosaicSourceGateway(context.transaction()).loadById(msId.val());
    EXPECT_EQ(ms.name(), prod.id().id());

    const auto event = db::MosaicSourceStatusEventGateway(context.transaction())
        .loadOne(_MosaicSourceStatusEvent::mosaicSourceId == ms.id());
    EXPECT_EQ(event.status(), db::MosaicSourceStatus::New);

    const auto m = db::MosaicGateway(context.transaction())
        .loadOne(_Mosaic::mosaicSourceId == ms.id());

    const auto dgProd = db::DgProductGateway(context.transaction())
        .loadOne(_DgProduct::mosaicSourceId == ms.id());
    EXPECT_EQ(dgProd.id(), prod.id());

    EXPECT_THAT(toStrings(delivery::Cog::missingFiles(*s3CogDir->dir("1"))), IsEmpty());
}

Y_UNIT_TEST(process_dg_delivery)
{
    const auto tmpDir = localStorage("./tmp")->dir(this->Name_);
    const auto s3Root = testS3(this->Name_);
    const auto s3PushDir = s3Root->dir("push_dir");
    const auto s3SrcDir = s3Root->dir("source_dir");
    const auto s3CogDir = s3Root->dir("cog_path");
    const auto dlPush = delivery::DgDelivery(localStorage(dataDgPath)).copyIndexedTo(s3PushDir);
    std::this_thread::sleep_for(std::chrono::milliseconds(100));
    auto prod = dlPush.products().at(0);

    tasks::Team team("test_team");
    team.add(tasks::typedWorker(CreateTempDirectory{tmpDir->absPath()}));
    team.add(tasks::typedWorker(DownloadProductFromS3{
        .pool = S3Pool{testS3Auth()}
    }));
    team.add(tasks::typedWorker(RemoveLocalDirectory{}));
    team.add(tasks::typedWorker(GenerateDgCog{}));
    team.add(tasks::typedWorker(AcceptDgDelivery{
        .s3CogDir = s3CogDir->absPath(),
        .demPath = dataDemPath,
        .dbDeliveryName = dbDeliveryName,
        .pool = S3Pool{testS3Auth()},
    }));
    team.add(tasks::typedWorker(ProcessNewDgDelivery{
        .pool = S3Pool{testS3Auth()}
    }));
    team.add(tasks::typedWorker(RemoveCopiedDgDelivery{
        .pool = S3Pool{testS3Auth()}
    }));

    unittest::Fixture fixture;
    pgpool3::Pool pool(fixture.postgres().connectionString(), pgpool3::PoolConstants(1, 2, 0, 0));

    {
        auto txn = pool.masterWriteableTransaction();
        setupDeliveryData(*txn, dlPush);
        txn->commit();
    }

    impl::copyAndRegisterDelivery(pool, dlPush, s3SrcDir);

    tasks::Schedule schedule(pool);
    team.work(schedule);

    auto txn = pool.masterWriteableTransaction();
    const auto ms = db::MosaicSourceGateway(*txn).tryLoadOne();
    ASSERT_TRUE(ms);
    EXPECT_EQ(ms->id(), 1);
    EXPECT_EQ(ms->name(), prod.id().id());
    ASSERT_TRUE(ms->cogPath());
    EXPECT_EQ(*ms->cogPath(), s3CogDir->absPath().native() + "/1");
    EXPECT_THAT(toStrings(delivery::Cog::missingFiles(*s3CogDir->dir("1"))), IsEmpty());
    EXPECT_THAT(toStrings(tmpDir->list(Select::FilesRecursive)), IsEmpty());
}

} // suite

} //namespace maps::factory::delivery::tests

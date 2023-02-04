#include <maps/factory/libs/processing/cloud_optimize.h>

#include <maps/factory/libs/unittest/tests_common.h>
#include <maps/factory/libs/dataset/dataset.h>
#include <maps/factory/libs/geometry/geometry.h>
#include <maps/factory/libs/geometry/geolib.h>
#include <maps/factory/libs/db/dg_delivery_gateway.h>
#include <maps/factory/libs/db/mosaic_source_gateway.h>
#include <maps/factory/libs/delivery/cog.h>
#include <maps/factory/libs/processing/tests/test_context.h>
#include <maps/factory/libs/processing/tests/test_s3.h>
#include <maps/factory/libs/storage/local_storage.h>
#include <maps/factory/libs/tasks/generic_worker.h>
#include <maps/factory/libs/tasks/team.h>
#include <maps/factory/libs/unittest/fixture.h>

#include <maps/libs/log8/include/log8.h>

namespace maps::factory::processing::tests {
using namespace testing;
using namespace maps::factory::tests;
using namespace maps::factory::storage;

Y_UNIT_TEST_SUITE(cloud_optimize_dg_should) {

Y_UNIT_TEST(upload_cog_to_s3)
{
    const auto s3 = testS3(this->Name_)->dir("uploaded_data");
    const auto local = localStorage("./tmp")->dir(this->Name_);

    delivery::Cog cog;
    cog.setContourGeoJson("{}")
       .setMulImage(delivery::CogImage().setRelativePath("MUL.TIF"))
       .setPanImage(delivery::CogImage().setRelativePath("PAN.TIF"))
       .setPreviewImage(delivery::CogImage().setRelativePath("PREVIEW.JPG"));

    local->file(cog.mulImage().relativePath())->writeString("mul");
    local->file(cog.panImage()->relativePath())->writeString("pan");
    local->file(cog.previewImage().relativePath())->writeString("preview");
    local->file(delivery::Cog::manifestFileName)->writeString(cog.json());

    const UploadCogToS3 worker{};
    worker(
        LocalDirectoryPath(local->absPath().native()),
        S3DirectoryPath(s3->absPath().native()));

    EXPECT_EQ(delivery::Cog(* s3), cog);
    EXPECT_EQ(s3->file(cog.mulImage().relativePath())->readToString(), "mul");
    EXPECT_EQ(s3->file(cog.panImage()->relativePath())->readToString(), "pan");
    EXPECT_EQ(s3->file(cog.previewImage().relativePath())->readToString(), "preview");
}

Y_UNIT_TEST(register_cog)
{
    const auto s3 = testS3(this->Name_)->dir("uploaded_data");
    unittest::Fixture fixture;
    TestContext context(fixture.postgres().connectionString());
    db::MosaicSource ms("test_mosaic_source");
    db::MosaicSourceGateway(context.transaction()).insert(ms);

    const RegisterCog worker;
    worker(
        MosaicSourceId(ms.id()),
        S3DirectoryPath(s3->absPath().native()),
        context);

    const auto result = db::MosaicSourceGateway(context.transaction()).loadById(ms.id());
    EXPECT_EQ(result.cogPath(), s3->absPath().native());
}

Y_UNIT_TEST(cloud_optimize_dg_delivery)
{
    unittest::Fixture fixture;
    TestContext context(fixture.postgres().connectionString());

    const std::string dataPath = ArcadiaSourceRoot()
                                 + "/maps/factory/test_data/dg_deliveries/058800151040_01";
    const auto local = localStorage("./tmp")->dir(this->Name_);
    localStorage(dataPath)->copyAll(*local);

    const GenerateDgCog worker{};
    auto result = worker(
        LocalDirectoryPath(local->absPath().native()), DgProductId("058800151040_01_P001"));

    auto localResult = localStorage(result.val());
    delivery::Cog cog(*localResult);

    EXPECT_EQ(cog.color(), delivery::CogColorChannels::PanMul16);
    EXPECT_EQ(cog.maxZoom(), 18);
    EXPECT_TRUE(cog.panImage());
    EXPECT_TRUE(cog.rpc());
    EXPECT_NEAR(cog.statistics().covariance.mean()(0), 46, 1);
    EXPECT_GE(cog.statistics().histogram.computeMax(0), 100);

    const auto mul = localResult->file(cog.mulImage().relativePath());
    const auto pan = localResult->file(cog.panImage()->relativePath());
    EXPECT_TRUE(mul->exists());
    EXPECT_TRUE(pan->exists());
    dataset::TDataset mulDs = dataset::OpenDataset(mul->absPath());
    dataset::TDataset panDs = dataset::OpenDataset(pan->absPath());
    EXPECT_EQ(geometry::SpatialRef(cog.spatialRefWkt()), panDs.projection());
    EXPECT_EQ(cog.rpc(), panDs.Rpc());
    EXPECT_THAT(cog.mulImage().size(), EigEq(mulDs.size()));
    EXPECT_THAT(cog.mulImage().transform().matrix(), EigEq(mulDs.Site().PixToProj().matrix()));
    EXPECT_THAT(cog.panImage()->size(), EigEq(panDs.size()));
    EXPECT_THAT(cog.panImage()->transform().matrix(), EigEq(panDs.Site().PixToProj().matrix()));

    const auto geom = geometry::Geometry::fromGeoJson(cog.contourGeoJson());
    EXPECT_GT(geom.area(), 0.0);

    const auto prev = localResult->file(cog.previewImage().relativePath());
    EXPECT_TRUE(prev->exists());
}

Y_UNIT_TEST(run_cloud_optimize_dg_delivery_sequence)
{
    unittest::Fixture fixture;
    TestContext context(fixture.postgres().connectionString());

    const std::string dgDir =
        ArcadiaSourceRoot() + "/maps/factory/test_data/dg_deliveries/058800151040_01";

    const auto s3Root = testS3(this->Name_);
    const auto s3Dg = s3Root->dir("dg");
    const auto s3Cog = s3Root->dir("cog");
    const auto tmpDir = localStorage("./tmp")->dir(this->Name_);
    localStorage(dgDir)->copyAll(*s3Dg);

    tasks::Team team("test_team");

    team.add(tasks::typedWorker(CreateTempDirectory{tmpDir->absPath()}));
    team.add(tasks::typedWorker(DownloadProductFromS3{.pool = {testS3Auth()}}));
    team.add(tasks::typedWorker(GenerateDgCog{}));
    team.add(tasks::typedWorker(UploadCogToS3CogDir{.s3CogDir=s3Cog->absPath(), .pool = {testS3Auth()},}));
    team.add(tasks::typedWorker(RemoveLocalDirectory{}));
    team.add(tasks::typedWorker(RegisterCog{}));
    team.add(tasks::typedWorker(CloudOptimizeDgDelivery{}));

    pgpool3::Pool pool(fixture.postgres().connectionString(), pgpool3::PoolConstants(1, 1, 0, 0));

    const std::string orderNo = "058800151040_01";
    int64_t msId;
    {
        auto txn = pool.masterWriteableTransaction();
        db::MosaicSource ms("test_mosaic_source");
        db::MosaicSourceGateway(*txn).insert(ms);
        msId = ms.id();
        db::DgDeliveryGateway(*txn).insert(
            db::DgDelivery(orderNo, s3Dg->absPath().native()));

        const auto dl = delivery::DgDelivery(dgDir);
        auto dbProd = db::DgProduct({"058800151040_01", "P001"}, msId);
        db::DgProductGateway(*txn).insert(dbProd);
        txn->commit();
    }

    tasks::Schedule schedule(pool);
    schedule.add({CloudOptimizeDgDelivery::name, MosaicSourceId(msId)});
    team.work(schedule);

    auto resultCogDir = s3Cog->dir(std::to_string(msId));
    EXPECT_THAT(toStrings(resultCogDir->list(Select::Files)),
        UnorderedElementsAre("COG.JSON", "MUL.TIF", "PAN.TIF", "PREVIEW.JPG"));
    EXPECT_EQ(db::MosaicSourceGateway(*pool.masterWriteableTransaction())
        .loadById(msId).cogPath().value(), resultCogDir->absPath().native());
    EXPECT_THAT(toStrings(tmpDir->list(Select::FilesRecursive)), IsEmpty());
}

} // suite

} //namespace maps::factory::delivery::tests

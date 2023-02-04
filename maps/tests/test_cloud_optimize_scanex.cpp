#include <maps/factory/libs/processing/cloud_optimize.h>

#include <maps/factory/libs/unittest/tests_common.h>
#include <maps/factory/libs/dataset/dataset.h>
#include <maps/factory/libs/geometry/geometry.h>
#include <maps/factory/libs/db/mosaic_source_gateway.h>
#include <maps/factory/libs/db/rgb_product_gateway.h>
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

Y_UNIT_TEST_SUITE(cloud_optimize_scanex_should) {

const std::string DATA_ROOT = ArcadiaSourceRoot() + "/maps/factory/test_data/scanex_deliveries/";
const std::string DATA_DIR = DATA_ROOT + "11123809_extracted_mini";

constexpr auto expectedGeom =
    R"({"type":"Polygon","coordinates":[[[111.806051333333,60.3197339621776],[111.806051333333,60.3205963333334],[112.148339247563,60.3205963333334],[112.148407918044,60.1362847621838],[112.085791258627,60.1364213306885],[112.085780240081,60.1364213001296],[111.995852791707,60.1357263430325],[111.811861842458,60.136141672489],[111.81174968018,60.1391700539895],[111.810360149537,60.1829402692547],[111.810221273008,60.1962724160383],[111.810220357746,60.1963074032189],[111.809247756006,60.2195109018759],[111.808830991711,60.2363203951113],[111.808828057921,60.2363781198401],[111.808552287263,60.2399631383917],[111.807857999477,60.2682900800526],[111.807851532046,60.2683856331903],[111.807578810148,60.2708401302707],[111.806884992837,60.2988703496577],[111.806878532046,60.2989656331903],[111.806606160948,60.3014169730741],[111.806329131665,60.3159610104444],[111.806326295076,60.3160219786476],[111.806051333333,60.3197339621776]]]})";

constexpr auto expectedGeomMulti =
    R"({"type":"MultiPolygon","coordinates":[[[[10.501745838945419,44.121170771164451],[10.502021898586081,44.120423368648552],[10.501711885395187,44.119170067962941],[10.501745838945419,44.121170771164451]]],[[[10.501617262489768,44.113593156294684],[10.501713302376009,44.112860653302555],[10.501597432731634,44.112424184744135],[10.501617262489768,44.113593156294684]]],[[[10.501501406894535,44.106762250992716],[10.501563995189601,44.106432887518402],[10.501569236711049,44.106409790060546],[10.502068583408684,44.104519337283534],[10.501968481982592,44.102952981481565],[10.501972193541635,44.10287911263633],[10.502491593614204,44.099732577203078],[10.501381088739512,44.099665227420218],[10.501501406894535,44.106762250992716]]]]})";

Y_UNIT_TEST(cloud_optimize_scanex_delivery)
{
    const auto local = localStorage("./tmp")->dir(this->Name_);
    localStorage(DATA_DIR)->copyAll(*local);

    const GenerateScanexCog worker{.threads = -8};
    const LocalDirectoryPath outDir = worker(
        LocalDirectoryPath(local->absPath().native()),
        MercatorContourWkt(""));

    const auto out = localStorage(outDir.val());
    delivery::Cog cog(*out);

    EXPECT_EQ(cog.color(), delivery::CogColorChannels::Rgb8);
    EXPECT_EQ(cog.maxZoom(), 6);
    EXPECT_EQ(cog.spatialRefWkt(), dataset::MERCATOR_WKT);
    EXPECT_FALSE(cog.panImage());
    EXPECT_FALSE(cog.rpc());
    EXPECT_NEAR(cog.statistics().covariance.mean()(0), 25, 1);
    EXPECT_GE(cog.statistics().histogram.computeMax(0), 100);

    auto mul = out->file(cog.mulImage().relativePath());
    EXPECT_TRUE(mul->exists());
    dataset::TDataset mulDs = dataset::OpenDataset(mul->absPath());
    EXPECT_THAT(cog.mulImage().size(), EigEq(mulDs.size()));
    EXPECT_THAT(cog.mulImage().transform().matrix(), EigEq(mulDs.Site().PixToProj().matrix()));

    auto geom = geometry::Geometry::fromGeoJson(cog.contourGeoJson());
    EXPECT_TRUE(geom.isEquals(*geometry::Geometry::fromGeoJson(expectedGeom)));

    auto prev = out->file(cog.previewImage().relativePath());
    EXPECT_TRUE(prev->exists());
}

Y_UNIT_TEST(cloud_optimize_scanex_delivery_with_multy_geom)
{
    const auto local = localStorage("./tmp")->dir(this->Name_);
    localStorage(DATA_ROOT + "13174627_geom")->copyAll(*local);

    const GenerateScanexCog worker;
    const LocalDirectoryPath outDir = worker(
        LocalDirectoryPath(local->absPath().native()),
        MercatorContourWkt(""));

    const auto out = localStorage(outDir.val());
    delivery::Cog cog(*out);

    EXPECT_EQ(cog.color(), delivery::CogColorChannels::Rgb8);
    EXPECT_EQ(cog.maxZoom(), 16);
    EXPECT_EQ(cog.spatialRefWkt(), dataset::MERCATOR_WKT);
    EXPECT_FALSE(cog.panImage());
    EXPECT_FALSE(cog.rpc());
    EXPECT_NEAR(cog.statistics().covariance.mean()(0), 6, 1);
    EXPECT_GE(cog.statistics().histogram.computeMax(0), 240);

    auto mul = out->file(cog.mulImage().relativePath());
    EXPECT_TRUE(mul->exists());
    dataset::TDataset mulDs = dataset::OpenDataset(mul->absPath());
    EXPECT_THAT(cog.mulImage().size(), EigEq(mulDs.size()));
    EXPECT_THAT(cog.mulImage().transform().matrix(), EigEq(mulDs.Site().PixToProj().matrix()));

    auto geom = geometry::Geometry::fromGeoJson(cog.contourGeoJson());
    auto mulBox = dataset::makePolygonGeometry(mulDs.Site().ProjBoundsIn(geometry::geodeticSr()));
    EXPECT_TRUE(geom.isWithin(*mulBox));
    EXPECT_GE(geom.similarity(*geometry::Geometry::fromGeoJson(expectedGeomMulti)), 0.999999);

    auto prev = out->file(cog.previewImage().relativePath());
    EXPECT_TRUE(prev->exists());
}

Y_UNIT_TEST(cloud_optimize_scanex_delivery_with_all_black_pixels)
{
    const auto local = localStorage("./tmp")->dir(this->Name_);
    localStorage(DATA_ROOT + "15675831_all_black")->copyAll(*local);

    const GenerateScanexCog worker;
    const LocalDirectoryPath outDir = worker(
        LocalDirectoryPath(local->absPath().native()),
        MercatorContourWkt(""));

    const auto out = localStorage(outDir.val());
    delivery::Cog cog(*out);

    EXPECT_EQ(cog.color(), delivery::CogColorChannels::Rgb8);
    EXPECT_EQ(cog.maxZoom(), 13);
    EXPECT_EQ(cog.spatialRefWkt(), dataset::MERCATOR_WKT);
    EXPECT_FALSE(cog.panImage());
    EXPECT_FALSE(cog.rpc());
    EXPECT_NEAR(cog.statistics().covariance.mean()(0), 26, 1);
    EXPECT_GE(cog.statistics().histogram.computeMax(0), 200);

    auto mul = out->file(cog.mulImage().relativePath());
    EXPECT_TRUE(mul->exists());
    dataset::TDataset mulDs = dataset::OpenDataset(mul->absPath());
    EXPECT_THAT(cog.mulImage().size(), EigEq(mulDs.size()));
    EXPECT_THAT(cog.mulImage().transform().matrix(), EigEq(mulDs.Site().PixToProj().matrix()));

    auto geom = geometry::Geometry::fromGeoJson(cog.contourGeoJson());
    auto mulBox = dataset::makePolygonGeometry(mulDs.Site().ProjBoundsIn(geometry::geodeticSr()));
    EXPECT_TRUE(geom.isWithin(*mulBox));

    auto prev = out->file(cog.previewImage().relativePath());
    EXPECT_TRUE(prev->exists());
}

Y_UNIT_TEST(run_optimize_scanex_delivery_sequence)
{
    const auto s3Root = testS3(this->Name_);
    const auto s3Rgb = s3Root->dir("rgb");
    const auto s3Cog = s3Root->dir("cog");
    const auto tmpDir = localStorage("./tmp")->dir(this->Name_);
    localStorage(DATA_DIR)->copyAll(*s3Rgb);

    tasks::Team team("test_team");

    team.add(tasks::typedWorker(CreateTempDirectory{tmpDir->absPath()}));
    team.add(tasks::typedWorker(DownloadDirectoryFromS3{.pool = {testS3Auth()}}));
    team.add(tasks::typedWorker(GenerateScanexCog{}));
    team.add(tasks::typedWorker(UploadCogToS3CogDir{.s3CogDir=s3Cog->absPath(), .pool = {testS3Auth()},}));
    team.add(tasks::typedWorker(RemoveLocalDirectory{}));
    team.add(tasks::typedWorker(RegisterCog{}));
    team.add(tasks::typedWorker(CloudOptimizeScanexDelivery{}));
    team.add(tasks::typedWorker(MakeMosaicSourceReady{}));

    unittest::Fixture fixture;
    pgpool3::Pool pool(fixture.postgres().connectionString(), pgpool3::PoolConstants(1, 1, 0, 0));

    int64_t msId;
    {
        auto txn = pool.masterWriteableTransaction();
        db::MosaicSource ms("test_mosaic_source");
        db::MosaicSourceGateway(*txn).insert(ms);
        msId = ms.id();
        db::RgbProduct prod(s3Rgb->absPath().native(), msId, db::RgbProductType::Scanex);
        db::RgbProductGateway(*txn).insert(prod);
        txn->commit();
    }

    tasks::Schedule schedule(pool);
    schedule.add({CloudOptimizeScanexDelivery::name, MosaicSourceId(msId)});
    team.work(schedule);

    auto resultCogDir = s3Cog->dir(std::to_string(msId));
    EXPECT_THAT(toStrings(resultCogDir->list(Select::Files)),
        UnorderedElementsAre("COG.JSON", "MUL.TIF", "PREVIEW.JPG"));
    const auto ms = db::MosaicSourceGateway(*pool.masterWriteableTransaction())
        .loadById(msId);
    EXPECT_EQ(ms.cogPath().value(), resultCogDir->absPath().native());
    EXPECT_EQ(ms.status(), db::MosaicSourceStatus::Ready);
    EXPECT_THAT(toStrings(tmpDir->list(Select::FilesRecursive)), IsEmpty());
}

} // suite

} //namespace maps::factory::delivery::tests

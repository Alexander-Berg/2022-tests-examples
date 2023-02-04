#include <maps/factory/libs/processing/old_scanex.h>

#include <maps/factory/libs/processing/tests/test_context.h>
#include <maps/factory/libs/processing/tests/test_s3.h>

#include <maps/factory/libs/unittest/tests_common.h>
#include <maps/factory/libs/db/mosaic_source_gateway.h>
#include <maps/factory/libs/db/rgb_product_gateway.h>
#include <maps/factory/libs/storage/local_storage.h>
#include <maps/factory/libs/tasks/generic_worker.h>
#include <maps/factory/libs/tasks/team.h>
#include <maps/factory/libs/unittest/fixture.h>

namespace maps::factory::processing::tests {
using namespace testing;
using namespace maps::factory::tests;
using namespace maps::factory::storage;

Y_UNIT_TEST_SUITE(scanex_tasks_should) {

Y_UNIT_TEST(check_scanex_delivery)
{
    const std::string dataRoot = ArcadiaSourceRoot() + "/maps/factory/test_data/scanex_deliveries";
    const auto dataDir = localStorage(dataRoot);
    const auto extractedDataDir = dataDir->dir("11123809_extracted_mini");

    const CheckScanexDelivery worker;
    EXPECT_NO_THROW(worker(LocalDirectoryPath(extractedDataDir->absPath().native())));
    EXPECT_THROW(worker(LocalDirectoryPath(dataDir->absPath().native())), RuntimeError);
}

Y_UNIT_TEST(register_scanex_product)
{
    const auto s3 = testS3(this->Name_)->dir("uploaded_data");
    unittest::Fixture fixture;
    TestContext context(fixture.postgres().connectionString());

    db::MosaicSource ms("test_mosaic_source");
    db::MosaicSourceGateway(context.transaction()).insert(ms);

    const RegisterScanexProduct worker;
    worker(
        MosaicSourceId(ms.id()),
        S3DirectoryPath(s3->absPath().native()),
        context);

    const auto products = db::RgbProductGateway(context.transaction()).load();
    EXPECT_EQ(products.size(), 1u);
    EXPECT_EQ(products[0].path(), s3->absPath());
    EXPECT_EQ(products[0].mosaicSourceId(), ms.id());
    EXPECT_EQ(products[0].type(), db::RgbProductType::Scanex);
}

} // suite

} //namespace maps::factory::delivery::tests

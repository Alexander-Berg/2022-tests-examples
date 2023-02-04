#include <maps/factory/libs/db/mosaic_gateway.h>
#include <maps/factory/libs/db/mosaic_source_gateway.h>
#include <maps/factory/libs/db/release_gateway.h>
#include <maps/factory/libs/db/release_validation_gateway.h>
#include <maps/factory/libs/db/tests/test_data.h>

#include <maps/factory/libs/unittest/tests_common.h>
#include <maps/factory/libs/unittest/fixture.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <pqxx/pqxx>

namespace maps::factory::db::tests {
using namespace factory::tests;
using namespace db::tests;
using namespace table::alias;

Y_UNIT_TEST_SUITE(test_release_validation_gateways) {

Y_UNIT_TEST(test_creating_release_validation)
{
    const std::string TEST_RELEASE_NAME = "test_release_name";
    Release testRelease(TEST_RELEASE_NAME);

    unittest::Fixture fixture;
    pqxx::connection conn(fixture.postgres().connectionString());
    {
        pqxx::work txn(conn);
        ReleaseGateway gtw(txn);
        gtw.insert(testRelease);
        txn.commit();
    }

    Validation testValidation(testRelease.id());
    {
        pqxx::work txn(conn);
        ValidationGateway gtw(txn);
        gtw.insert(testValidation);
        txn.commit();
        EXPECT_EQ(testValidation.id(), 1);
    }

    {
        pqxx::work txn(conn);
        ValidationGateway gtw(txn);
        Validation dbValidation = gtw.loadById(testValidation.id());
        EXPECT_EQ(testValidation.releaseId(), dbValidation.releaseId());
        EXPECT_EQ(testValidation.status(), dbValidation.status());
    }

    {
        pqxx::work txn(conn);
        ValidationGateway gtw(txn);
        testValidation.setStatusToCanceled();
        gtw.update(testValidation);
        Validation dbValidation = gtw.loadById(testValidation.id());
        EXPECT_EQ(dbValidation.status(), ValidationStatus::Canceled);
    }
}

Y_UNIT_TEST(test_creating_release_validation_error)
{
    const Id MOSAIC_ID = 1;
    const Id OTHER_MOSAIC_ID = 2;
    const Id VALIDATION_ID = 1;
    const ValidationErrorType TYPE = ValidationErrorType::WrongZoomMax;
    const json::Value DATA = json::Value{json::repr::ObjectRepr{}};
    const auto INTERSECTION = TEST_MOSAIC_SOURCE_GEOMETRY;
    unittest::Fixture fixture;
    pqxx::connection conn(fixture.postgres().connectionString());

    {
        pqxx::work txn(conn);
        MosaicSource source(TEST_MOSAIC_SOURCE_NAME);
        source.setMercatorGeom(TEST_MOSAIC_SOURCE_GEOMETRY);
        MosaicSourceGateway(txn).insert(source);

        Mosaic mosaic(source.id(), 1, 2, 3, source.mercatorGeom());
        MosaicGateway(txn).insert(mosaic);
        mosaic = Mosaic(source.id(), 1, 2, 3, source.mercatorGeom());
        MosaicGateway(txn).insert(mosaic);

        Release release("release_name");
        ReleaseGateway(txn).insert(release);

        Validation validation(release.id());
        ValidationGateway(txn).insert(validation);
        txn.commit();
    }

    ValidationError testValidationError(
        VALIDATION_ID, MOSAIC_ID, OTHER_MOSAIC_ID, TYPE);
    testValidationError.setData(DATA);
    testValidationError.setIntersection(INTERSECTION);
    {
        pqxx::work txn(conn);
        ValidationErrorGateway gtw(txn);
        gtw.insert(testValidationError);
        txn.commit();
        ASSERT_EQ(testValidationError.id(), 1);
    }

    {
        pqxx::work txn(conn);
        ValidationErrorGateway gtw(txn);
        auto dbValidationError = gtw.loadById(testValidationError.id());
        EXPECT_EQ(dbValidationError.validationId(), VALIDATION_ID);
        EXPECT_EQ(dbValidationError.sortedMosaicIds(), std::make_pair(MOSAIC_ID, OTHER_MOSAIC_ID));
        EXPECT_EQ(dbValidationError.type(), TYPE);
        EXPECT_EQ(dbValidationError.data(), DATA);
        EXPECT_FALSE(dbValidationError.muted());
        ASSERT_TRUE(dbValidationError.intersection());
        EXPECT_THAT(*dbValidationError.intersection(), GeoEq(INTERSECTION));
    }
}

} // suite

} // namespace maps::factory::db::tests

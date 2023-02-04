#include <maps/factory/libs/processing/validate_release.h>

#include <maps/factory/libs/db/mosaic_gateway.h>
#include <maps/factory/libs/db/mosaic_source_gateway.h>
#include <maps/factory/libs/db/release_gateway.h>
#include <maps/factory/libs/db/release_validation_gateway.h>

#include <maps/factory/libs/sproto_helpers/release_validation.h>

#include <maps/factory/libs/processing/tests/test_context.h>
#include <maps/factory/libs/unittest/tests_common.h>
#include <maps/factory/libs/unittest/fixture.h>

namespace maps::factory::processing::tests {

namespace {

const std::string MOSAIC_ID = "1";
const uint32_t ZORDER = 10;
const int64_t MIN_ZOOM = 1;
const int64_t MAX_ZOOM = 18;
const double RESOLUTION = 1.0;
const std::string COLLECTION_DATE = "2020-01-01 01:01:01";
const geolib3::MultiPolygon2 GEOMETRY({
    geolib3::Polygon2(
        geolib3::PointsVector{
            {0, 0},
            {0, 5},
            {5, 5},
            {5, 0}
        }
    )
});

const int64_t CONFLICTING_RELEASE_ID = 2;
const std::string CONFLICTING_MOSAIC_ID = "2";

void prepareData(
    const std::string& collectionDate,
    double resolution,
    uint32_t zorder,
    int64_t maxZoom,
    TestContext& ctx)
{
    auto mosaicSource = db::MosaicSource("mosaicSource");
    mosaicSource.setResolutionMeterPerPx(RESOLUTION);
    mosaicSource.setCollectionDate(
        chrono::parseIntegralDateTime(COLLECTION_DATE, "%Y-%m-%d %H:%M:%S"));
    db::MosaicSourceGateway(ctx.transaction()).insert(mosaicSource);

    auto release = db::Release("release");
    release.setStatus(db::ReleaseStatus::Production);
    db::ReleaseGateway(ctx.transaction()).insert(release);

    auto mosaic = db::Mosaic(mosaicSource.id(),
        ZORDER, MIN_ZOOM, MAX_ZOOM, GEOMETRY);
    mosaic.setReleaseId(release.id());
    db::MosaicGateway(ctx.transaction()).insert(mosaic);

    release = db::Release("testRelease");
    db::ReleaseGateway(ctx.transaction()).insert(release);

    mosaicSource = db::MosaicSource("testMosaicSource");
    mosaicSource.setResolutionMeterPerPx(resolution);
    mosaicSource.setCollectionDate(
        chrono::parseIntegralDateTime(collectionDate, "%Y-%m-%d %H:%M:%S"));
    db::MosaicSourceGateway(ctx.transaction()).insert(mosaicSource);

    mosaic = db::Mosaic(mosaicSource.id(), zorder,
        MIN_ZOOM, maxZoom, GEOMETRY);
    mosaic.setReleaseId(release.id());
    db::MosaicGateway(ctx.transaction()).insert(mosaic);
}

impl::ValidationWithErrors runValidation(
    TestContext& ctx, const int64_t releaseId = CONFLICTING_RELEASE_ID)
{
    db::Validation validation(releaseId);
    db::ValidationGateway(ctx.transaction()).insert(validation);

    const ValidateRelease worker;
    worker(ReleaseValidationId(validation.id()), ctx);

    validation = db::ValidationGateway(ctx.transaction()).loadById(validation.id());
    return impl::ValidationWithErrors{
        validation, loadValidationResults(validation, ctx.transaction())};
}

}

Y_UNIT_TEST_SUITE(validate_release_tasks_should) {

Y_UNIT_TEST(collect_max_zoom_conflicts)
{
    unittest::Fixture fixture;
    TestContext ctx(fixture.postgres().connectionString());

    const auto maxZoom = MAX_ZOOM - 1;
    prepareData(COLLECTION_DATE, RESOLUTION, ZORDER + 1, maxZoom, ctx);

    const auto validationWithResult = runValidation(ctx);
    const auto validation = validationWithResult.validation;
    const auto validationResult = sproto_helpers::convertToSproto(validationWithResult.results);

    ASSERT_EQ(validation.status(), db::ValidationStatus::Finished);
    ASSERT_EQ(validationResult.errors().size(), 1u);
    ASSERT_TRUE(validationResult.errors().at(0).wrongZoomMaxValue());

    auto zoomMaxError = *validationResult.errors().at(0).wrongZoomMaxValue();
    EXPECT_EQ(zoomMaxError.mosaicId(), CONFLICTING_MOSAIC_ID);
    EXPECT_EQ(zoomMaxError.mosaicZoomMax(), maxZoom);
    EXPECT_EQ(zoomMaxError.conflictedMosaicId(), MOSAIC_ID);
    EXPECT_EQ(zoomMaxError.conflictedMosaicZoomMax(), MAX_ZOOM);
}

Y_UNIT_TEST(collect_z_order_conflicts)
{
    unittest::Fixture fixture;
    TestContext ctx(fixture.postgres().connectionString());

    const auto zorder = ZORDER - 1;
    prepareData(COLLECTION_DATE, RESOLUTION, zorder, MAX_ZOOM + 1, ctx);

    const auto validationWithResult = runValidation(ctx);
    const auto validation = validationWithResult.validation;
    const auto validationResult = sproto_helpers::convertToSproto(validationWithResult.results);

    ASSERT_EQ(validation.status(), db::ValidationStatus::Finished);
    ASSERT_EQ(validationResult.errors().size(), 1u);
    ASSERT_TRUE(validationResult.errors().at(0).wrongZindex());

    auto zindexError = *validationResult.errors().at(0).wrongZindex();
    EXPECT_EQ(*zindexError.reason(),
        sproto_helpers::srelease::ValidationError::WrongZindex::Reason::PUBLISHED);
    EXPECT_EQ(zindexError.mosaicId(), CONFLICTING_MOSAIC_ID);
    EXPECT_EQ(zindexError.mosaicZindex(), zorder);
    EXPECT_EQ(zindexError.conflictedMosaicId(), MOSAIC_ID);
    EXPECT_EQ(zindexError.conflictedMosaicZindex(), ZORDER);
}

Y_UNIT_TEST(collect_resolution_z_order_conflicts)
{
    unittest::Fixture fixture;
    TestContext ctx(fixture.postgres().connectionString());

    const auto zorder = ZORDER + 1;
    prepareData(COLLECTION_DATE, RESOLUTION + 1, zorder, MAX_ZOOM + 1, ctx);

    const auto validationWithResult = runValidation(ctx);
    const auto validation = validationWithResult.validation;
    const auto validationResult = sproto_helpers::convertToSproto(validationWithResult.results);

    ASSERT_EQ(validation.status(), db::ValidationStatus::Finished);
    ASSERT_EQ(validationResult.errors().size(), 1u);
    ASSERT_TRUE(validationResult.errors().at(0).wrongZindex());

    auto zindexError = *validationResult.errors().at(0).wrongZindex();
    EXPECT_EQ(*zindexError.reason(),
        sproto_helpers::srelease::ValidationError::WrongZindex::Reason::RESOLUTION);
    EXPECT_EQ(zindexError.mosaicId(), CONFLICTING_MOSAIC_ID);
    EXPECT_EQ(zindexError.mosaicZindex(), zorder);
    EXPECT_EQ(zindexError.conflictedMosaicId(), MOSAIC_ID);
    EXPECT_EQ(zindexError.conflictedMosaicZindex(), ZORDER);
}

Y_UNIT_TEST(collect_resolution_z_order_conflicts_skip_by_threshold)
{
    unittest::Fixture fixture;
    TestContext ctx(fixture.postgres().connectionString());

    const auto zorder = ZORDER + 1;
    prepareData(COLLECTION_DATE, RESOLUTION + 0.2, zorder, MAX_ZOOM + 1, ctx);

    const auto validationWithResult = runValidation(ctx);
    const auto validation = validationWithResult.validation;
    const auto validationResult = sproto_helpers::convertToSproto(validationWithResult.results);

    ASSERT_EQ(validation.status(), db::ValidationStatus::Finished);
    ASSERT_EQ(validationResult.errors().size(), 0u);
}

Y_UNIT_TEST(collect_old_collection_date_conflicts)
{
    unittest::Fixture fixture;
    TestContext ctx(fixture.postgres().connectionString());

    const auto collectionDate = "2019-01-01 01:01:01";
    prepareData(collectionDate, RESOLUTION, ZORDER + 1, MAX_ZOOM + 1, ctx);

    const auto validationWithResult = runValidation(ctx);
    const auto validation = validationWithResult.validation;
    const auto validationResult = sproto_helpers::convertToSproto(validationWithResult.results);

    ASSERT_EQ(validation.status(), db::ValidationStatus::Finished);
    ASSERT_EQ(validationResult.errors().size(), 1u);
    ASSERT_TRUE(validationResult.errors().at(0).oldCollectionDate());

    auto oldDateError = *validationResult.errors().at(0).oldCollectionDate();
    EXPECT_EQ(oldDateError.mosaicId(), CONFLICTING_MOSAIC_ID);
    EXPECT_EQ(sproto_helpers::convertTimeToString(
        oldDateError.mosaicCollectionDate()), collectionDate);
    EXPECT_EQ(oldDateError.conflictedMosaicId(), MOSAIC_ID);
    EXPECT_EQ(sproto_helpers::convertTimeToString(
        oldDateError.conflictedMosaicCollectionDate()), COLLECTION_DATE);
}

Y_UNIT_TEST(collect_old_collection_date_conflicts_skip_by_zorder)
{
    unittest::Fixture fixture;
    TestContext ctx(fixture.postgres().connectionString());

    const auto collectionDate = "2019-01-01 01:01:01";
    prepareData(collectionDate, RESOLUTION, ZORDER - 1, MAX_ZOOM + 1, ctx);

    const auto validationWithResult = runValidation(ctx);
    const auto validation = validationWithResult.validation;
    const auto validationResult = sproto_helpers::convertToSproto(validationWithResult.results);

    ASSERT_EQ(validation.status(), db::ValidationStatus::Finished);
    ASSERT_EQ(validationResult.errors().size(), 1u);
    ASSERT_TRUE(validationResult.errors().at(0).wrongZindex());
}

Y_UNIT_TEST(skip_small_collection_date_conflicts)
{
    unittest::Fixture fixture;
    TestContext ctx(fixture.postgres().connectionString());

    const auto collectionDate = "2020-12-20 01:01:01";
    prepareData(collectionDate, RESOLUTION, ZORDER + 1, MAX_ZOOM + 1, ctx);

    const auto validationWithResult = runValidation(ctx);
    const auto validation = validationWithResult.validation;
    const auto validationResult = sproto_helpers::convertToSproto(validationWithResult.results);

    ASSERT_EQ(validation.status(), db::ValidationStatus::Finished);
    ASSERT_EQ(validationResult.errors().size(), 0u);
}

Y_UNIT_TEST(skip_fully_covered_mosaics)
{
    unittest::Fixture fixture;
    TestContext ctx(fixture.postgres().connectionString());

    const auto maxZoom = MAX_ZOOM - 1;
    prepareData(COLLECTION_DATE, RESOLUTION, ZORDER + 1, maxZoom, ctx);

    auto mosaic = db::Mosaic(1, ZORDER - 1, MIN_ZOOM, MAX_ZOOM,
        geolib3::MultiPolygon2({
            geolib3::Polygon2(
                geolib3::PointsVector{
                    {1, 1},
                    {1, 4},
                    {4, 4},
                    {4, 1}
                }
            )
        }));
    mosaic.setReleaseId(1);
    db::MosaicGateway(ctx.transaction()).insert(mosaic);

    const auto validationWithResult = runValidation(ctx);
    const auto validation = validationWithResult.validation;
    const auto validationResult = sproto_helpers::convertToSproto(validationWithResult.results);

    ASSERT_EQ(validation.status(), db::ValidationStatus::Finished);
    ASSERT_EQ(validationResult.errors().size(), 1u);
    ASSERT_TRUE(validationResult.errors().at(0).wrongZoomMaxValue());

    auto zoomMaxError = *validationResult.errors().at(0).wrongZoomMaxValue();
    EXPECT_EQ(zoomMaxError.mosaicId(), CONFLICTING_MOSAIC_ID);
    EXPECT_EQ(zoomMaxError.conflictedMosaicId(), MOSAIC_ID);
}

Y_UNIT_TEST(collect_internal_release_conflicts)
{
    unittest::Fixture fixture;
    TestContext ctx(fixture.postgres().connectionString());

    const auto collectionDate = "2019-01-01 01:01:01";
    const auto zorder = ZORDER + 1;
    const auto maxZoom = MAX_ZOOM - 1;
    prepareData(collectionDate, RESOLUTION + 1, zorder, maxZoom, ctx);

    auto mosaic = db::MosaicGateway(ctx.transaction()).loadById(1);
    mosaic.setReleaseId(2);
    db::MosaicGateway(ctx.transaction()).update(mosaic);

    const auto validationWithResult = runValidation(ctx);
    const auto validation = validationWithResult.validation;
    const auto validationResult = sproto_helpers::convertToSproto(validationWithResult.results);

    ASSERT_EQ(validation.status(), db::ValidationStatus::Finished);
    ASSERT_EQ(validationResult.errors().size(), 3u);
}

Y_UNIT_TEST(properly_inherit_muted_flags)
{
    unittest::Fixture fixture;
    TestContext ctx(fixture.postgres().connectionString());

    const auto zorder = ZORDER - 1;
    prepareData(COLLECTION_DATE, RESOLUTION, zorder, MAX_ZOOM + 1, ctx);

    auto validationWithResult = runValidation(ctx);
    auto validation = validationWithResult.validation;
    ASSERT_EQ(validation.status(), db::ValidationStatus::Finished);
    ASSERT_EQ(validationWithResult.results.size(), 1u);

    auto error = validationWithResult.results.at(0);
    error.setMuted(true);
    db::ValidationErrorGateway(ctx.transaction()).update(error);

    validationWithResult = runValidation(ctx);
    validation = validationWithResult.validation;
    ASSERT_EQ(validation.status(), db::ValidationStatus::Finished);
    ASSERT_EQ(validationWithResult.results.size(), 1u);
    EXPECT_TRUE(validationWithResult.results.at(0).muted());

    auto mosaic = db::MosaicGateway(ctx.transaction()).loadById(2);
    mosaic.setZOrder(zorder - 1);
    db::MosaicGateway(ctx.transaction()).update(mosaic);

    validationWithResult = runValidation(ctx);
    validation = validationWithResult.validation;
    ASSERT_EQ(validation.status(), db::ValidationStatus::Finished);
    ASSERT_EQ(validationWithResult.results.size(), 1u);
    EXPECT_FALSE(validationWithResult.results.at(0).muted());
}

} // suite
} // namespace maps::factory::processing::tests

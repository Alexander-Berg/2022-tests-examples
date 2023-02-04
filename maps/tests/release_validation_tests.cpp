#include <maps/factory/libs/sproto_helpers/release_validation_errors.h>
#include <maps/factory/libs/sproto_helpers/release_validation.h>
#include <maps/factory/libs/sproto_helpers/tests/test_utils.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::factory::sproto_helpers::tests {

namespace {

const auto STATUS = db::ValidationStatus::Running;
const auto CREATED_AT = chrono::TimePoint::clock::now();
const auto FINISHED_AT = chrono::TimePoint::clock::now();
const Id RELEASE_ID = 1;
const Id VALIDATION_ID = 1;

const std::string MOSAIC_ID = "2";
const std::string CONFLICTED_MOSAIC_ID = "1";
const uint32_t ZOOM_MAX = 1;
const uint32_t CONFLICTED_ZOOM_MAX = 2;
const int64_t ZINDEX = 3;
const int64_t CONFLICTED_ZINDEX = 4;
const auto REASON = WrongZindex::Reason::PUBLISHED;
const std::string COLLECTION_DATE = "2020-01-01 01:01:01";
const std::string CONFLICTED_COLLECTION_DATE = "2021-01-01 01:01:01";
const bool MUTED = true;

const geolib3::MultiPolygon2 INTERSECTION({
    geolib3::Polygon2(
        geolib3::PointsVector{
            {0, 0},
            {0, 5},
            {5, 5},
            {5, 0}
        }
    )
});

db::ValidationErrors dbValidationResult()
{
    auto maxZoomError = json::Value{
        {"type", json::Value{WRONG_ZOOM_MAX}},
        {"mosaicId", json::Value{MOSAIC_ID}},
        {"mosaicZoomMax", json::Value{ZOOM_MAX}},
        {"conflictedMosaicId", json::Value{CONFLICTED_MOSAIC_ID}},
        {"conflictedMosaicZoomMax", json::Value{CONFLICTED_ZOOM_MAX}}
    };

    auto zindexError = json::Value{
        {"type", json::Value{WRONG_ZINDEX}},
        {"reason", json::Value{REASON}},
        {"mosaicId", json::Value{MOSAIC_ID}},
        {"mosaicZindex", json::Value{ZINDEX}},
        {"conflictedMosaicId", json::Value{CONFLICTED_MOSAIC_ID}},
        {"conflictedMosaicZindex", json::Value{CONFLICTED_ZINDEX}}
    };

    auto oldDateError = json::Value{
        {"type", json::Value{OLD_COLLECTION_DATE}},
        {"mosaicId", json::Value{MOSAIC_ID}},
        {"mosaicCollectionDate", json::Value{COLLECTION_DATE}},
        {"conflictedMosaicId", json::Value{CONFLICTED_MOSAIC_ID}},
        {"conflictedMosaicCollectionDate", json::Value{CONFLICTED_COLLECTION_DATE}}
    };

    db::ValidationErrors validationResult;
    const auto mosaicId = parseId(MOSAIC_ID);
    const auto conflictedMosaicId = parseId(CONFLICTED_MOSAIC_ID);

    validationResult.push_back(
        db::ValidationError(
            VALIDATION_ID, mosaicId, conflictedMosaicId,
            db::ValidationErrorType::WrongZoomMax
        )
    );
    validationResult.back()
                    .setData(maxZoomError)
                    .setMuted(MUTED)
                    .setIntersection(INTERSECTION);

    validationResult.push_back(
        db::ValidationError(
            VALIDATION_ID, mosaicId, conflictedMosaicId,
            db::ValidationErrorType::WrongZoomMax
        )
    );
    validationResult.back()
                    .setData(zindexError)
                    .setMuted(MUTED)
                    .setIntersection(INTERSECTION);

    validationResult.push_back(
        db::ValidationError(
            VALIDATION_ID, mosaicId, conflictedMosaicId,
            db::ValidationErrorType::OldCollectionDate
        )
    );
    validationResult.back()
                    .setData(oldDateError)
                    .setMuted(MUTED)
                    .setIntersection(INTERSECTION);

    return validationResult;
}

db::Validation dbReleaseValidation()
{
    auto releaseValidation = db::Validation(RELEASE_ID);
    releaseValidation.setStatus(STATUS);
    releaseValidation.setCreatedAt(CREATED_AT);
    releaseValidation.setFinishedAt(FINISHED_AT);
    return releaseValidation;
}

} // namespace

TEST(test_convert_validation_results, test_convert_to_sproto)
{
    auto validationResult = convertToSproto(dbValidationResult());
    ASSERT_EQ(validationResult.errors().size(), 3u);

    WrongZoomMaxValue zoomMaxError;
    WrongZindex zindexError;
    OldCollectionDate oldDateError;
    for (const auto& error: validationResult.errors()) {
        if (error.wrongZoomMaxValue()) {
            zoomMaxError = *error.wrongZoomMaxValue();
        } else if (error.wrongZindex()) {
            zindexError = *error.wrongZindex();
        } else if (error.oldCollectionDate()) {
            oldDateError = *error.oldCollectionDate();
        }
        EXPECT_TRUE(error.muted() && *error.muted() == MUTED);
        EXPECT_TRUE(error.intersection() &&
                    convertFromSprotoGeoGeometry(*error.intersection()) == INTERSECTION);
    }

    EXPECT_EQ(zoomMaxError.mosaicId(), MOSAIC_ID);
    EXPECT_EQ(zoomMaxError.mosaicZoomMax(), ZOOM_MAX);
    EXPECT_EQ(zoomMaxError.conflictedMosaicId(), CONFLICTED_MOSAIC_ID);
    EXPECT_EQ(zoomMaxError.conflictedMosaicZoomMax(), CONFLICTED_ZOOM_MAX);

    EXPECT_EQ(*zindexError.reason(), REASON);
    EXPECT_EQ(zindexError.mosaicId(), MOSAIC_ID);
    EXPECT_EQ(zindexError.mosaicZindex(), ZINDEX);
    EXPECT_EQ(zindexError.conflictedMosaicId(), CONFLICTED_MOSAIC_ID);
    EXPECT_EQ(zindexError.conflictedMosaicZindex(), CONFLICTED_ZINDEX);

    EXPECT_EQ(oldDateError.mosaicId(), MOSAIC_ID);
    EXPECT_EQ(
        convertTimeToString(oldDateError.mosaicCollectionDate()),
        convertTimeToString(convertToSproto(COLLECTION_DATE))
    );
    EXPECT_EQ(oldDateError.conflictedMosaicId(), CONFLICTED_MOSAIC_ID);
    EXPECT_EQ(
        convertTimeToString(oldDateError.conflictedMosaicCollectionDate()),
        convertTimeToString(convertToSproto(CONFLICTED_COLLECTION_DATE))
    );
}

TEST(test_convert_release_validation, test_convert_to_sproto)
{
    auto releaseValidation = convertToSproto(dbReleaseValidation());
    EXPECT_EQ(releaseValidation.releaseId(), std::to_string(RELEASE_ID));
    EXPECT_EQ(*releaseValidation.status(), convertToSproto(STATUS));
    EXPECT_EQ(
        convertTimeToString(releaseValidation.createdAt()),
        convertTimeToString(convertToSproto(CREATED_AT))
    );
    EXPECT_EQ(
        convertTimeToString(*releaseValidation.finishedAt()),
        convertTimeToString(convertToSproto(FINISHED_AT))
    );
}

TEST(test_convert_validation_errors, test_convert_to_json)
{
    const auto dbResults = dbValidationResult();
    const auto zoomMaxError = WrongZoomMaxError{
        parseId(MOSAIC_ID), parseId(CONFLICTED_MOSAIC_ID),
        ZOOM_MAX, CONFLICTED_ZOOM_MAX, 0, false, INTERSECTION
    };
    const auto oldDateError = OldCollectionDateError{
        parseId(MOSAIC_ID), parseId(CONFLICTED_MOSAIC_ID),
        convertTimeFromString(COLLECTION_DATE),
        convertTimeFromString(CONFLICTED_COLLECTION_DATE),
        0, false, INTERSECTION
    };
    const auto zindexError = WrongZindexError{
        parseId(MOSAIC_ID), parseId(CONFLICTED_MOSAIC_ID),
        ZINDEX, CONFLICTED_ZINDEX, 0, false, INTERSECTION
    };

    EXPECT_EQ(toJson(zoomMaxError), dbResults.at(0).data());
    EXPECT_EQ(toJson(oldDateError), dbResults.at(2).data());
    EXPECT_EQ(toJson(zindexError), dbResults.at(1).data());
}

} // namespace maps::factory::sproto_helpers::tests

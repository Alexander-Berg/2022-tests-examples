#include <maps/factory/libs/sproto_helpers/mosaic.h>
#include <maps/factory/libs/sproto_helpers/rendering_params.h>
#include <maps/factory/libs/sproto_helpers/etag_utils.h>

#include <maps/factory/libs/sproto_helpers/tests/test_utils.h>

#include <maps/libs/chrono/include/time_point.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::factory::sproto_helpers::tests {

namespace {

const Id SOURCE_ID = 1;
const int32_t ZORDER = 1;
const size_t MIN_ZOOM = 1;
const size_t MAX_ZOOM = 18;
const geolib3::Vector2 SHIFT = {1, 1};
const Id RELEASE_ID = 2;

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

const int32_t HUE = 120;
const int32_t SATURATION = 1;
const int32_t LIGHTNESS = 1;
const double SIGMA = 0.5;
const double RADIUS = 0.5;
const auto COLOR_CORRECTION_PARAMS = json::Value{
    {"hue", json::Value{HUE}},
    {"saturation", json::Value{SATURATION}},
    {"lightness", json::Value{LIGHTNESS}}
};
const auto SHARPING_PARAMS = json::Value{
    {"sigma", json::Value{SIGMA}},
    {"radius", json::Value{RADIUS}}
};
const auto RENDERING_PARAMS = json::Value{
    {"hue", json::Value{HUE}},
    {"saturation", json::Value{SATURATION}},
    {"lightness", json::Value{LIGHTNESS}},
    {"sigma", json::Value{SIGMA}},
    {"radius", json::Value{RADIUS}}
};

const auto TIME_FORMAT = "%Y-%m-%d %H:%M:%S";
const auto MODIFIED_AT_STR = "2021-01-01 00:00:00";
const auto MODIFIED_AT =
    chrono::parseIntegralDateTime(MODIFIED_AT_STR, TIME_FORMAT);
const std::string MODIFIED_BY = "John";

db::Mosaic dbMosaic()
{
    auto mosaic = db::Mosaic(
        SOURCE_ID, ZORDER, MIN_ZOOM, MAX_ZOOM, GEOMETRY);
    mosaic.setShift(SHIFT);
    mosaic.setReleaseId(RELEASE_ID);
    mosaic.setRenderingParams(RENDERING_PARAMS);
    mosaic.setModifiedAt(MODIFIED_AT);
    mosaic.setModifiedBy(MODIFIED_BY);
    return mosaic;
}

smosaics::Mosaic sprotoMosaic()
{
    smosaics::Mosaic mosaic;
    mosaic.mosaicSourceId() = std::to_string(SOURCE_ID);
    mosaic.releaseId() = std::to_string(RELEASE_ID);
    mosaic.mercatorShift()->x() = SHIFT.x();
    mosaic.mercatorShift()->y() = SHIFT.y();
    mosaic.zoomMin() = MIN_ZOOM;
    mosaic.zoomMax() = MAX_ZOOM;
    mosaic.geometry() = convertToSprotoGeoGeometry(GEOMETRY);
    mosaic.zIndex() = ZORDER;
    mosaic.colorCorrectionParams() =
        *convertToSproto<smosaics::ColorCorrectionParams>(COLOR_CORRECTION_PARAMS);
    mosaic.sharpingParams() =
        *convertToSproto<smosaics::SharpingParams>(SHARPING_PARAMS);
    mosaic.modifiedAt() = convertToSproto(MODIFIED_AT);
    mosaic.modifiedBy() = MODIFIED_BY;
    return mosaic;
}

} // namespace

TEST(test_convert_mosaic, test_convert_to_sproto)
{
    auto smosaic = convertToSproto(dbMosaic());
    EXPECT_EQ(smosaic.mosaicSourceId(), std::to_string(SOURCE_ID));
    EXPECT_EQ(smosaic.releaseId(), std::to_string(RELEASE_ID));
    EXPECT_TRUE(
        (smosaic.mercatorShift()->x() == SHIFT.x()) &&
        (smosaic.mercatorShift()->y() == SHIFT.y())
    );
    EXPECT_EQ(smosaic.zoomMin(), MIN_ZOOM);
    EXPECT_EQ(smosaic.zoomMax(), MAX_ZOOM);
    EXPECT_TRUE(
        geolib3::convertGeodeticToMercator(
            decodeSprotoGeometry<geolib3::MultiPolygon2>(*smosaic.geometry())
        ) == GEOMETRY
    );
    EXPECT_EQ(smosaic.zIndex(), ZORDER);
    EXPECT_EQ(convertFromSproto(
        *smosaic.colorCorrectionParams()), COLOR_CORRECTION_PARAMS);
    EXPECT_EQ(convertFromSproto(
        *smosaic.sharpingParams()), SHARPING_PARAMS);
    EXPECT_EQ(
        convertFromSproto(*smosaic.modifiedAt()),
        MODIFIED_AT
    );
    EXPECT_EQ(*smosaic.modifiedBy(), MODIFIED_BY);
}

TEST(test_convert_mosaic, test_convert_from_sproto)
{
    auto mosaic = convertFromSproto(sprotoMosaic());
    EXPECT_EQ(mosaic.mosaicSourceId(), SOURCE_ID);
    EXPECT_EQ(mosaic.releaseId(), RELEASE_ID);
    EXPECT_EQ(mosaic.zOrder(), ZORDER);
    EXPECT_EQ(mosaic.minZoom(), MIN_ZOOM);
    EXPECT_EQ(mosaic.maxZoom(), MAX_ZOOM);
    EXPECT_TRUE(
        (mosaic.shift().x() == SHIFT.x()) &&
        (mosaic.shift().y() == SHIFT.y())
    );
    EXPECT_TRUE(mosaic.mercatorGeom() == GEOMETRY);
    EXPECT_EQ(*mosaic.renderingParams(), RENDERING_PARAMS);
    EXPECT_EQ(mosaic.modifiedAt(), MODIFIED_AT);
    EXPECT_EQ(mosaic.modifiedBy(), MODIFIED_BY);
}

TEST(test_etag_utils, test_mosaic_etag)
{
    const auto original = sprotoMosaic();
    auto copy = original;

    EXPECT_EQ(calculateEtag(original), calculateEtag(copy));

    copy.mosaicSourceId() = "0";
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
    copy = original;

    copy.releaseId() = "0";
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
    copy = original;

    copy.zoomMin() = 2;
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
    copy = original;

    copy.zoomMax() = 17;
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
    copy = original;

    copy.zIndex() = 2;
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
    copy = original;

    copy.mercatorShift()->x() = 0;
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
    copy = original;

    copy.geometry() = convertToSprotoGeoGeometry(
        geolib3::MultiPolygon2({
            geolib3::Polygon2(
                geolib3::PointsVector{
                    {0, 0},
                    {0, 6},
                    {6, 6},
                    {6, 0}
                }
            )
        })
    );
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
    copy = original;

    copy.colorCorrectionParams() = {};
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
    copy = original;

    copy.sharpingParams() = {};
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
    copy = original;

    copy.modifiedAt() = convertToSproto("2021-01-01 00:00:01");
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
    copy = original;

    copy.modifiedBy() = "Vlad";
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
    copy = original;
}

TEST(test_etag_utils, test_shift_precision)
{
    auto firstMosaic = dbMosaic();
    auto otherMosaic = dbMosaic();

    firstMosaic.setShift({1.234567890123456, 1.234567890123456});
    otherMosaic.setShift({1.234567890123457, 1.234567890123457});

    EXPECT_EQ(calculateEtag(convertToSproto(firstMosaic)),
        calculateEtag(convertToSproto(otherMosaic)));

    firstMosaic.setShift({1.23456789012345, 1.23456789012345});
    otherMosaic.setShift({1.23456789012346, 1.23456789012346});

    EXPECT_NE(calculateEtag(convertToSproto(firstMosaic)),
        calculateEtag(convertToSproto(otherMosaic)));
}

} // namespace maps::factory::sproto_helpers::tests

#include <maps/factory/libs/sproto_helpers/mosaic_source.h>
#include <maps/factory/libs/sproto_helpers/etag_utils.h>

#include <maps/factory/libs/sproto_helpers/tests/test_utils.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::factory::sproto_helpers::tests {

namespace {

const Id DELIVERY_ID = 1;
const Id ORDER_ID = 2;
const Id AOI_ID = 3;
const std::string NAME = "test_mosaic_source";
const auto STATUS = db::MosaicSourceStatus::New;
const std::string SATELLITE = "sat1";
const double RESOLUTION = 1.0;
const double OFFNADIR = 20.0;
const double HEADING = 10.0;
const auto TIME_FORMAT = "%Y-%m-%d %H:%M:%S";
const auto COLLECTION_TIME_STR = "2021-01-01 00:00:00";
const geolib3::Vector2 SHIFT = {1, 1};
const auto COLLECTION_TIME =
    chrono::parseIntegralDateTime(COLLECTION_TIME_STR, TIME_FORMAT);
const auto METADATA = json::Value{
    {"elev", json::Value{std::to_string(OFFNADIR)}},
    {"azim_angle", json::Value{std::to_string(HEADING)}},
    {"datetime", json::Value{COLLECTION_TIME_STR}}
};
const std::vector<std::string> TAGS = {"tags"};

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

db::MosaicSource dbMosaicSource()
{
    db::MosaicSource mosaicSource(NAME);
    mosaicSource.setStatus(STATUS);
    mosaicSource.setMercatorGeom(GEOMETRY);
    mosaicSource.setDeliveryId(DELIVERY_ID);
    mosaicSource.setOrderId(ORDER_ID);
    mosaicSource.setAoiId(AOI_ID);
    mosaicSource.setSatellite(SATELLITE);
    mosaicSource.setMetadata(METADATA);
    mosaicSource.setResolutionMeterPerPx(RESOLUTION);
    mosaicSource.setOffnadir(OFFNADIR);
    mosaicSource.setHeading(HEADING);
    mosaicSource.setCollectionDate(COLLECTION_TIME);
    mosaicSource.setTags(TAGS);
    return mosaicSource;
}

smosaics::MosaicSource sprotoMosaicSource()
{
    smosaics::MosaicSource mosaicSource;
    mosaicSource.name() = NAME;
    mosaicSource.deliveryId() = std::to_string(DELIVERY_ID);
    mosaicSource.orderId() = std::to_string(ORDER_ID);
    mosaicSource.aoiId() = std::to_string(AOI_ID);
    mosaicSource.status() = convertToSproto(STATUS);
    mosaicSource.satellite() = SATELLITE;
    putGeometryToSproto(mosaicSource, GEOMETRY);
    putMetadataToSproto(mosaicSource, METADATA);
    mosaicSource.resolutionMeterPerPx() = RESOLUTION;
    mosaicSource.offnadir() = OFFNADIR;
    mosaicSource.heading() = HEADING;
    mosaicSource.collectionDate() = convertToSproto(COLLECTION_TIME);
    return mosaicSource;
}

}

TEST(test_convert_mosaic_source, test_convert_to_sproto)
{
    auto sMosaicSource = convertToSproto(dbMosaicSource());
    EXPECT_EQ(sMosaicSource.name(), NAME);
    EXPECT_EQ(*sMosaicSource.status(), convertToSproto(STATUS));
    EXPECT_EQ(sMosaicSource.satellite(), SATELLITE);
    EXPECT_TRUE(
        convertFromSprotoGeoGeometry(
            unpackSprotoRepeated<scommon::geometry::Polygon>(
                sMosaicSource.geometry()
            )
        ) == GEOMETRY
    );
    EXPECT_EQ(
        convertFromSproto(
            unpackSprotoRepeated<scommon::KeyValuePair>(
                sMosaicSource.metadata()
            )
        ),
        METADATA
    );
    EXPECT_EQ(*sMosaicSource.resolutionMeterPerPx(), RESOLUTION);
    EXPECT_EQ(*sMosaicSource.offnadir(), OFFNADIR);
    EXPECT_EQ(*sMosaicSource.heading(), HEADING);
    EXPECT_EQ(
        convertFromSproto(*sMosaicSource.collectionDate()),
        COLLECTION_TIME
    );
}

TEST(test_convert_mosaic_source, test_convert_from_sproto)
{
    auto mosaicSource = convertFromSproto(sprotoMosaicSource());
    EXPECT_EQ(mosaicSource.name(), NAME);
    EXPECT_EQ(mosaicSource.status(), STATUS);
    EXPECT_EQ(mosaicSource.satellite(), SATELLITE);
    EXPECT_TRUE(mosaicSource.mercatorGeom() == GEOMETRY);
    EXPECT_EQ(mosaicSource.metadata(), METADATA);
    EXPECT_EQ(*mosaicSource.resolutionMeterPerPx(), RESOLUTION);
    EXPECT_EQ(*mosaicSource.offnadir(), OFFNADIR);
    EXPECT_EQ(*mosaicSource.heading(), HEADING);
    EXPECT_EQ(*mosaicSource.collectionDate(), COLLECTION_TIME);
}

TEST(test_etag_utils, test_mosaic_source_etag)
{
    const auto original = sprotoMosaicSource();
    auto copy = original;

    EXPECT_EQ(calculateEtag(original), calculateEtag(copy));

    copy.name() = "updated_name";
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
    copy = original;

    copy.satellite() = "updated_satellite";
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
    copy = original;

    copy.geometry() = {};
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
    copy = original;

    copy.metadata() = {};
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
    copy = original;

    copy.tags().push_back("new_tag");
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
    copy = original;

    copy.status() = convertToSproto(db::MosaicSourceStatus::Ready);
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
    copy = original;

    copy.aoiId() = "1";
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
    copy = original;

    copy.orderId() = "1";
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
    copy = original;

    copy.resolutionMeterPerPx() = 2.0;
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
    copy = original;

    copy.offnadir() = 10.0;
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
    copy = original;

    copy.heading() = 5.0;
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
    copy = original;

    copy.collectionDate() = convertToSproto("2021-01-01 00:00:01");
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
    copy = original;

    copy.deliveryId() = "0";
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
}

} // namespace maps::factory::sproto_helpers::tests

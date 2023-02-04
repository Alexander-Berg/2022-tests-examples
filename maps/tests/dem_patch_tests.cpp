#include <maps/factory/libs/sproto_helpers/dem_patch.h>
#include <maps/factory/libs/sproto_helpers/etag_utils.h>

#include <maps/factory/libs/geometry/geolib.h>

#include <maps/factory/libs/unittest/tests_common.h>

#include <maps/libs/chrono/include/time_point.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::factory::sproto_helpers::tests {
using namespace factory::tests;

namespace {

const auto NAME = "test_patch";
const auto RELEASE_ID = 1;
const auto DESCRIPTION = "test_description";
const auto COG_PATH = "test_cog_path";
const auto BOUNDING_BOX = geolib3::BoundingBox({0, 0}, {1, 1});
const auto HAS_FILE = true;

const auto TIME_FORMAT = "%Y-%m-%d %H:%M:%S";
const auto MODIFIED_AT_STR = "2021-01-01 00:00:00";
const auto MODIFIED_AT = chrono::parseIntegralDateTime(MODIFIED_AT_STR, TIME_FORMAT);
const std::string MODIFIED_BY = "John";

db::DemPatch dbDemPatch()
{
    auto patch = db::DemPatch(NAME, RELEASE_ID)
        .setDescription(DESCRIPTION)
        .setBbox(geometry::toBox2d(BOUNDING_BOX))
        .setCogPath(COG_PATH)
        .setModifiedAt(MODIFIED_AT)
        .setModifiedBy(MODIFIED_BY);
    return patch;
}

sdem::DemPatch sprotoDemPatch()
{
    sdem::DemPatch patch;
    patch.name() = NAME;
    patch.description() = DESCRIPTION;
    patch.boundingBox() = convertToSprotoGeoGeometry(BOUNDING_BOX);
    patch.hasFile() = HAS_FILE;
    patch.releaseId() = std::to_string(RELEASE_ID);
    patch.modifiedAt() = convertToSproto(MODIFIED_AT);
    patch.modifiedBy() = MODIFIED_BY;
    return patch;
}

} // namespace

TEST(test_convert_dem_patch, test_convert_to_sproto)
{
    db::DemPatch patch = dbDemPatch();
    auto spatch = convertToSproto(patch);
    EXPECT_EQ(spatch.name(), NAME);
    EXPECT_EQ(
        geolib3::convertGeodeticToMercator(
            decodeSprotoGeometry<geolib3::BoundingBox>(*spatch.boundingBox())
        ), BOUNDING_BOX
    );
    EXPECT_EQ(spatch.hasFile(), HAS_FILE);
    EXPECT_EQ(spatch.releaseId(), std::to_string(RELEASE_ID));
    EXPECT_EQ(*spatch.description(), DESCRIPTION);
    EXPECT_EQ(convertFromSproto(*spatch.modifiedAt()), MODIFIED_AT);
    EXPECT_EQ(*spatch.modifiedBy(), MODIFIED_BY);
}

TEST(test_convert_dem_patch, test_convert_from_sproto)
{
    db::DemPatch patch = convertFromSproto(sprotoDemPatch());
    EXPECT_EQ(patch.name(), NAME);
    EXPECT_EQ(patch.description(), DESCRIPTION);
    EXPECT_EQ(patch.releaseId(), RELEASE_ID);
    EXPECT_EQ(geometry::toGeolibBox(patch.bbox().value()), BOUNDING_BOX);
    EXPECT_EQ(patch.modifiedAt(), MODIFIED_AT);
    EXPECT_EQ(patch.modifiedBy(), MODIFIED_BY);
}

TEST(test_etag_utils, test_dem_patch_etag)
{
    const auto original = sprotoDemPatch();
    auto copy = original;

    EXPECT_EQ(calculateEtag(original), calculateEtag(copy));

    copy.name() = "updated_name";
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
    copy = original;

    copy.description() = "updated_description";
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
    copy = original;

    copy.hasFile() = !HAS_FILE;
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
    copy = original;

    copy.releaseId() = std::to_string(RELEASE_ID + 1);
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
    copy = original;

    copy.boundingBox() = convertToSprotoGeoGeometry(
        geolib3::BoundingBox({1, 1}, {2, 2}));
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
    copy = original;

    copy.modifiedAt() = convertToSproto("2021-01-01 00:00:01");
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
    copy = original;

    copy.modifiedBy() = "Vlad";
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
    copy = original;
}

} // namespace maps::factory::sproto_helpers::tests

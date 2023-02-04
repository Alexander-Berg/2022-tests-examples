#include <maps/factory/libs/sproto_helpers/dem_release.h>
#include <maps/factory/libs/sproto_helpers/etag_utils.h>

#include <maps/libs/chrono/include/time_point.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::factory::sproto_helpers::tests {

namespace {

const auto NAME = "test_release";
const auto DESCRIPTION = "test_description";
const auto CURRENT_STATUS = db::DemReleaseStatus::New;
const auto TARGET_STATUS = db::DemReleaseStatus::Production;

const auto TIME_FORMAT = "%Y-%m-%d %H:%M:%S";
const auto MODIFIED_AT_STR = "2021-01-01 00:00:00";
const auto MODIFIED_AT =
    chrono::parseIntegralDateTime(MODIFIED_AT_STR, TIME_FORMAT);
const std::string MODIFIED_BY = "John";

db::DemRelease dbDemRelease()
{
    auto release = db::DemRelease(NAME)
        .setDescription(DESCRIPTION)
        .setStatus(CURRENT_STATUS)
        .setIssueId(1)
        .setModifiedAt(MODIFIED_AT)
        .setModifiedBy(MODIFIED_BY);
    return release;
}

sdem::DemRelease sprotoDemRelease()
{
    sdem::DemRelease release;
    release.name() = NAME;
    release.description() = DESCRIPTION;
    release.currentStatus() = convertToSproto(CURRENT_STATUS);
    release.targetStatus() = convertToSproto(TARGET_STATUS);
    release.modifiedAt() = convertToSproto(MODIFIED_AT);
    release.modifiedBy() = MODIFIED_BY;
    return release;
}

} // namespace

TEST(test_convert_dem_release, test_status_conversion)
{
    const std::vector<std::pair<db::DemReleaseStatus, sdem::DemRelease::Status>>
        DB_TO_SPROTO_RELEASE_STATUSES{
        {db::DemReleaseStatus::New, sdem::DemRelease::Status::NEW},
        {db::DemReleaseStatus::Production, sdem::DemRelease::Status::PRODUCTION}
    };

    for (const auto&[dbStatus, protoStatus]: DB_TO_SPROTO_RELEASE_STATUSES) {
        EXPECT_EQ(dbStatus, convertFromSproto(protoStatus));
        EXPECT_EQ(protoStatus, convertToSproto(dbStatus));
    }
}

TEST(test_convert_dem_release, test_convert_to_sproto)
{
    auto release = dbDemRelease();
    auto srelease = convertToSproto(release);
    EXPECT_EQ(srelease.name(), NAME);
    EXPECT_EQ(*srelease.description(), DESCRIPTION);
    EXPECT_EQ(*srelease.currentStatus(), convertToSproto(CURRENT_STATUS));
    EXPECT_EQ(*srelease.targetStatus(), convertToSproto(CURRENT_STATUS));
    EXPECT_EQ(*srelease.issueId(), std::to_string(release.issueId().value()));
    EXPECT_EQ(
        convertFromSproto(*srelease.modifiedAt()),
        MODIFIED_AT
    );
    EXPECT_EQ(*srelease.modifiedBy(), MODIFIED_BY);
}

TEST(test_convert_dem_release, test_convert_from_sproto)
{
    auto release = convertFromSproto(sprotoDemRelease());
    EXPECT_EQ(release.name(), NAME);
    EXPECT_EQ(release.description(), DESCRIPTION);
    EXPECT_EQ(release.sourceStatus(), TARGET_STATUS);
    EXPECT_EQ(release.targetStatus(), TARGET_STATUS);
    EXPECT_EQ(release.modifiedAt(), MODIFIED_AT);
    EXPECT_EQ(release.modifiedBy(), MODIFIED_BY);
}

TEST(test_etag_utils, test_dem_release_etag)
{
    const auto original = sprotoDemRelease();
    auto copy = original;

    EXPECT_EQ(calculateEtag(original), calculateEtag(copy));

    copy.name() = "updated_name";
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
    copy = original;

    copy.description() = "updated_description";
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
    copy = original;

    copy.currentStatus() = convertToSproto(TARGET_STATUS);
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
    copy = original;

    copy.targetStatus() = convertToSproto(CURRENT_STATUS);
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

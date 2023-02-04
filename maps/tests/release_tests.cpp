#include <maps/factory/libs/sproto_helpers/release.h>
#include <maps/factory/libs/sproto_helpers/etag_utils.h>

#include <maps/libs/chrono/include/time_point.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::factory::sproto_helpers::tests {

namespace {

const auto NAME = "test_release";
const auto CURRENT_STATUS = db::ReleaseStatus::New;
const auto TARGET_STATUS = db::ReleaseStatus::Ready;

const auto TIME_FORMAT = "%Y-%m-%d %H:%M:%S";
const auto MODIFIED_AT_STR = "2021-01-01 00:00:00";
const auto MODIFIED_AT =
    chrono::parseIntegralDateTime(MODIFIED_AT_STR, TIME_FORMAT);
const std::string MODIFIED_BY = "John";

db::Release dbRelease()
{
    auto release = db::Release(NAME)
        .setStatus(CURRENT_STATUS)
        .setIssue(1)
        .setModifiedAt(MODIFIED_AT)
        .setModifiedBy(MODIFIED_BY);
    return release;
}

srelease::Release sprotoRelease()
{
    srelease::Release release;
    release.name() = NAME;
    release.currentStatus() = convertToSproto(CURRENT_STATUS);
    release.targetStatus() = convertToSproto(TARGET_STATUS);
    release.modifiedAt() = convertToSproto(MODIFIED_AT);
    release.modifiedBy() = MODIFIED_BY;
    return release;
}

} // namespace

TEST(test_convert_release, test_status_conversion)
{
    const std::vector<std::pair<db::ReleaseStatus, srelease::Release::Status>>
        DB_TO_SPROTO_RELEASE_STATUSES{
        {db::ReleaseStatus::Frozen, srelease::Release::Status::FROZEN},
        {db::ReleaseStatus::New, srelease::Release::Status::DRAFT},
        {db::ReleaseStatus::Ready, srelease::Release::Status::READY},
        {db::ReleaseStatus::Testing, srelease::Release::Status::TESTING},
        {db::ReleaseStatus::Production, srelease::Release::Status::PRODUCTION}
    };

    for (const auto&[dbStatus, protoStatus]: DB_TO_SPROTO_RELEASE_STATUSES) {
        EXPECT_EQ(dbStatus, convertFromSproto(protoStatus));
        EXPECT_EQ(protoStatus, convertToSproto(dbStatus));
    }
}

TEST(test_convert_release, test_convert_to_sproto)
{
    auto release = dbRelease();
    auto srelease = convertToSproto(release);
    EXPECT_EQ(srelease.name(), NAME);
    EXPECT_EQ(*srelease.currentStatus(), convertToSproto(CURRENT_STATUS));
    EXPECT_EQ(*srelease.targetStatus(), convertToSproto(CURRENT_STATUS));
    EXPECT_EQ(*srelease.issueId(), release.issue().value());
    EXPECT_EQ(
        convertFromSproto(*srelease.modifiedAt()),
        MODIFIED_AT
    );
    EXPECT_EQ(*srelease.modifiedBy(), MODIFIED_BY);
}

TEST(test_convert_release, test_convert_from_sproto)
{
    auto release = convertFromSproto(sprotoRelease());
    EXPECT_EQ(release.name(), NAME);
    EXPECT_EQ(release.sourceStatus(), TARGET_STATUS);
    EXPECT_EQ(release.targetStatus(), TARGET_STATUS);
    EXPECT_EQ(release.modifiedAt(), MODIFIED_AT);
    EXPECT_EQ(release.modifiedBy(), MODIFIED_BY);
}

TEST(test_etag_utils, test_release_etag)
{
    const auto original = sprotoRelease();
    auto copy = original;

    EXPECT_EQ(calculateEtag(original), calculateEtag(copy));

    copy.name() = "updated_name";
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

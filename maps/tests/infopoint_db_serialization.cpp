#include <maps/infopoint/lib/backend/query/point.h>
#include <maps/infopoint/lib/comments/moderation_policy.h>
#include <maps/infopoint/lib/moderation/state.h>
#include <maps/infopoint/lib/point/infopoint.h>
#include <maps/infopoint/lib/point/point_uuid.h>

#include <maps/libs/geolib/include/point.h>

#include <contrib/libs/mongo-cxx-driver/bsoncxx/types/bson_value/view.hpp>
#include <library/cpp/testing/gtest/gtest.h>

#include <chrono>

namespace {

using namespace infopoint;
using std::chrono::system_clock;

using bsoncxx::builder::basic::make_array;
using bsoncxx::builder::basic::make_document;
using bsoncxx::builder::basic::kvp;
using bsoncxx::types::b_date;

const auto EPSILON = 1e-06;

const auto POINT_BSON = make_document(
    kvp("_id", "bc55b020-dcfe-490a-b8ba-001abb3f4705"),
    kvp("point", make_document(
        kvp("lat", 40.0),
        kvp("lon", -20.0))),
    kvp("version", 1),
    kvp("tags", make_array("lane_control", "police")),
    kvp("begin", db::toMongoDate(1306161608)),
    kvp("modified", db::toMongoDate(1306161608)),
    kvp("end", make_document(
        kvp("auto", false),
        kvp("time", b_date(std::chrono::seconds(1306168808))))),
    kvp("originalConfidence", 0.5),
    kvp("confidence", 0.5),
    kvp("rating", 0.5),
    kvp("direction", 123.0),
    kvp("votingDisabled", false),
    kvp("attribution", make_document(
        kvp("author", "urn:login:author"),
        kvp("owner", "urn:login:owner"),
        kvp("owner_ip", "1.2.3.4"),
        kvp("owner_port", "1234"))),
    kvp("description", make_document(
        kvp("content", "Comment text"),
        kvp("moderation", make_document(
            kvp("status", "approved"),
            kvp("source", "clean_web"),
            kvp("verdicts", make_array("first", "second")),
            kvp(
                "started",
                b_date(std::chrono::seconds(1306168810))),
            kvp(
                "finished",
                b_date(std::chrono::seconds(1306168812))),
            kvp(
                "next_retry",
                b_date(std::chrono::seconds(1306168810))))))),
    kvp("address", "Улица им. Шмулица"),
    kvp("locale", "zh-CN"),
    kvp("deleted", false),
    kvp("comments",
        make_array(
            make_document(
                kvp("moderation", make_document(
                    kvp("status", "pending"),
                    kvp("verdicts", make_array("first", "second")),
                    kvp(
                        "started",
                        b_date(std::chrono::seconds(1306168810))),
                    kvp("source", "clean_web"),
                    kvp(
                        "next_retry",
                        b_date(std::chrono::seconds(1306168810)))
                ))
            ),
            make_document(
                kvp("moderation", make_document(
                    kvp("status", "approved"),
                    kvp("verdicts", make_array("first", "second")),
                    kvp(
                        "started",
                        b_date(std::chrono::seconds(1306168810))),
                    kvp("source", "clean_web"),
                    kvp(
                        "finished",
                        b_date(std::chrono::seconds(1306168812))),
                    kvp(
                        "next_retry",
                        b_date(std::chrono::seconds(1306168810)))
                ))
            )
        )
    )
);

} // namespace

TEST(infopoint_database_serialization, load)
{
    auto checkLoadedPoint =
            [](const Infopoint& point, int expectedCommentCount) {
        EXPECT_EQ(point.type, PointType(PointTags({"lane_control", "police"})));
        EXPECT_EQ(point.type.legacyTypeDeprecated(), "lane camera");

        EXPECT_EQ(point.begin, system_clock::from_time_t(1306161608));
        EXPECT_EQ(point.end, system_clock::from_time_t(1306168808));
        EXPECT_EQ(point.modified, system_clock::from_time_t(1306161608));
        EXPECT_EQ(
            point.uuid,
            toPointUuid("bc55b020-dcfe-490a-b8ba-001abb3f4705"));

        EXPECT_EQ(point.storedVersion, 1);
        EXPECT_NEAR(point.position.y(), 40.0, 0.01);
        EXPECT_NEAR(point.position.x(), -20.0, 0.01);
        EXPECT_EQ(point.address, "Улица им. Шмулица");

        EXPECT_EQ(point.moderation.status, moderation::Status::Approved);
        EXPECT_THAT(
            point.moderation.verdicts,
            testing::ElementsAre(
                moderation::Verdict("first"),
                moderation::Verdict("second")
            )
        );
        EXPECT_EQ(
            point.moderation.nextRetry, system_clock::from_time_t(1306168810));
        EXPECT_EQ(
            point.moderation.finished, system_clock::from_time_t(1306168812));
        EXPECT_EQ(*point.moderation.source, moderation::Source::CleanWeb);

        ASSERT_TRUE(point.locale);
        EXPECT_EQ(*point.locale, "zh-CN");
        EXPECT_EQ(point.commentCount, expectedCommentCount);
    };

    checkLoadedPoint(
        db::toPoint(POINT_BSON, CommentsExportModerationPolicy::DontFilter),
        2 /* expectedCommentCount */);
    checkLoadedPoint(
        db::toPoint(POINT_BSON, CommentsExportModerationPolicy::OnlyModerated),
        1 /* expectedCommentCount */);
}

TEST(infopoint_database_serialization, save)
{
    const auto point = db::toPoint(
        POINT_BSON, CommentsExportModerationPolicy::DontFilter);

    const auto& reference = POINT_BSON.view();
    const auto value = db::query::toBSON(point);
    const auto actual = value.view();
    EXPECT_EQ(actual["_id"].get_value(), reference["_id"].get_value());
    EXPECT_NEAR(
        actual["point"]["lon"].get_double().value,
        reference["point"]["lon"].get_double().value, EPSILON);
    EXPECT_NEAR(
        actual["point"]["lat"].get_double().value,
        reference["point"]["lat"].get_double().value, EPSILON);
    EXPECT_NEAR(
        actual["direction"].get_double().value,
        reference["direction"].get_double().value, EPSILON);
    EXPECT_EQ(
        actual["version"].get_int32(),
        reference["version"].get_int32() + 1);
    EXPECT_EQ(actual["tags"].get_value(), reference["tags"].get_value());
    EXPECT_EQ(actual["begin"].get_value(), reference["begin"].get_value());
    EXPECT_EQ(
        actual["modified"].get_value(), reference["modified"].get_value());
    EXPECT_EQ(
        actual["end"]["auto"].get_value(),
        reference["end"]["auto"].get_value());
    EXPECT_EQ(
        actual["end"]["time"].get_value(),
        reference["end"]["time"].get_value());
    EXPECT_NEAR(
        actual["confidence"].get_double().value,
        reference["confidence"].get_double().value, EPSILON);
    EXPECT_NEAR(
        actual["originalConfidence"].get_double().value,
        reference["originalConfidence"].get_double().value, EPSILON);
    EXPECT_NEAR(
        actual["rating"].get_double().value,
        reference["rating"].get_double().value, EPSILON);
    EXPECT_EQ(
        actual["votingDisabled"].get_value(),
        reference["votingDisabled"].get_value());

    EXPECT_EQ(
        actual["attribution"].get_value(),
        reference["attribution"].get_value());

    EXPECT_EQ(
        actual["description"].get_value(),
        reference["description"].get_value());

    EXPECT_EQ(
        actual["address"].get_value(),
        reference["address"].get_value());
    EXPECT_EQ(
        actual["locale"].get_value(),
        reference["locale"].get_value());
    EXPECT_EQ(
        actual["deleted"].get_value(),
        reference["deleted"].get_value());
}

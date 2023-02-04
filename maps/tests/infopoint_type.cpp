#include <maps/infopoint/lib/point/point_type.h>

#include <maps/libs/common/include/exception.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <vector>

using namespace infopoint;


TEST(InfopointType, TagsToLegacyTypeMapping)
{
    EXPECT_EQ(PointType(PointTags({"other"})).legacyTypeDeprecated(),
        "other");
    EXPECT_EQ(PointType(PointTags({"chat"})).legacyTypeDeprecated(),
        "chat");
    EXPECT_EQ(PointType(PointTags({"local_chat"})).legacyTypeDeprecated(),
        "chat");
    EXPECT_EQ(PointType(PointTags({"danger"})).legacyTypeDeprecated(),
        "danger");
    EXPECT_EQ(PointType(PointTags({"police"})).legacyTypeDeprecated(),
        "speed camera");
    EXPECT_EQ(
        PointType(PointTags({"police", "mobile_control"}))
            .legacyTypeDeprecated(),
        "speed camera");

    EXPECT_EQ(
        PointType(PointTags({"police", "speed_control"}))
            .legacyTypeDeprecated(),
        "speed camera");
    EXPECT_EQ(
        PointType(PointTags({"police", "lane_control"}))
            .legacyTypeDeprecated(),
        "lane camera");
    EXPECT_EQ(
        PointType(PointTags({"police", "speed_control", "lane_control"}))
            .legacyTypeDeprecated(),
        "speed camera");
    EXPECT_EQ(
        PointType(PointTags({"police", "road_marking_control"}))
            .legacyTypeDeprecated(),
        "lane camera");
    EXPECT_EQ(
        PointType(PointTags({"police", "cross_road_control"}))
            .legacyTypeDeprecated(),
        "lane camera");
    EXPECT_EQ(
        PointType(PointTags({"police", "no_stopping_control"}))
            .legacyTypeDeprecated(),
        "other");
    EXPECT_EQ(
        PointType(PointTags({"accident"})).legacyTypeDeprecated(),
        "accident");
    EXPECT_EQ(
        PointType(PointTags({"other"})).legacyTypeDeprecated(),
        "other");
    EXPECT_EQ(
        PointType(PointTags({"school"})).legacyTypeDeprecated(),
        "other");
    EXPECT_EQ(
        PointType(PointTags({"other", "traffic_alert"}))
            .legacyTypeDeprecated(),
        "other");
}

TEST(InfopointType, TagsValidity)
{
    EXPECT_THROW(PointType(PointTags{}), InvalidTagsError);
    EXPECT_THROW(PointType(PointTags{""}), InvalidTagsError);
    EXPECT_THROW(
        PointType(PointTags{"no_such_tag"}), InvalidTagsError);
    EXPECT_THROW(
        PointType(PointTags{"traffic_alert, other"}), InvalidTagsError);
    EXPECT_THROW(
        PointType(PointTags{"police", "no_such_tag"}), InvalidTagsError);
    EXPECT_THROW(
        PointType(PointTags{"police", "danger"}), InvalidTagsError);
    EXPECT_THROW(
        PointType(PointTags{"chat", "local_chat"}), InvalidTagsError);

    EXPECT_NO_THROW(PointType(PointTags{"police"}));
    EXPECT_NO_THROW(
        PointType(PointTags{"police", "speed_control", "mobile_control"}));
    EXPECT_NO_THROW(PointType(PointTags{"danger"}));
    EXPECT_NO_THROW(
        PointType(PointTags{"danger", "overtaking_danger"}));
    EXPECT_NO_THROW(PointType(PointTags{"other"}));
    EXPECT_NO_THROW(PointType(PointTags{"traffic_alert", "other"}));
    EXPECT_NO_THROW(PointType(PointTags{"feedback"}));
}

TEST(InfopointType, LegacyTypeToTagsMapping)
{
    EXPECT_THROW(PointType("Unknown Type"), maps::RuntimeError);

    EXPECT_EQ(PointType(PointLegacyType("other")).tags(),
        PointTags({"other"}));
    EXPECT_EQ(PointType(PointLegacyType("chat")).tags(),
        PointTags({"chat"}));
    EXPECT_EQ(PointType(PointLegacyType("feedback")).tags(),
        PointTags({"feedback"}));
    EXPECT_EQ(PointType(PointLegacyType("police")).tags(),
        PointTags({"speed_control", "police"}));
    EXPECT_EQ(PointType(PointLegacyType("police post")).tags(),
        PointTags({"police"}));
    EXPECT_EQ(PointType(PointLegacyType("danger")).tags(),
        PointTags({"danger"}));
    EXPECT_EQ(PointType(PointLegacyType("speed camera")).tags(),
        PointTags({"speed_control", "police"}));
}

TEST(InfopointType, RelationalOperator)
{
    std::vector<PointType> points = {
        PointType("accident"),
        PointType("police"),
        PointType(PointTags{"police", "lane_control"}),
        PointType(PointTags{"police", "cross_road_control"}),
    };
    for (size_t i = 0; i < points.size(); ++i) {
        for (size_t j = 0; j < points.size(); ++j) {
            EXPECT_EQ((points[i] == points[j]), (i == j));
            EXPECT_EQ((points[i] != points[j]), (i != j));
        }
    }

    EXPECT_EQ(
        PointType(PointTags{"chat"}),
        PointType(PointTags{"chat", "chat"}));
    EXPECT_EQ(
        PointType(PointTags{"traffic_alert", "other"}),
        PointType(PointTags{"other", "traffic_alert"}));
}

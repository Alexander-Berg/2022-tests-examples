#include <maps/infopoint/lib/moderation/apply_verdicts_to_text.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/common/env.h>


using namespace infopoint;
using namespace infopoint::moderation;

const std::string MODERATION_RULES_CONFIG = ArcadiaSourceRoot() + "/maps/infopoint/conf/settings.json.stable";

TEST(ModerationVerdictsMapping, StatusMapping)
{
    auto config = maps::json::Value::fromFile(MODERATION_RULES_CONFIG);
    loadRules(config["moderation_rules"]);

    auto nextTarget = [](TargetType type, Verdicts verdicts, PointTags tags = {}) {
        return TargetMetadata{
            .type = type,
            .pointPosition = {0, 0},
            .pointTags = tags,
            .verdicts = std::move(verdicts)
        };
    };

    EXPECT_EQ(
        toModerationStatus(
            nextTarget(TargetType::Point, {})),
        Status::Disapproved);
    EXPECT_EQ(
        toModerationStatus(
            nextTarget(TargetType::Comment, {})),
        Status::Disapproved);

    EXPECT_EQ(
        toModerationStatus(
            nextTarget(TargetType::Point, {Verdict("road_other")})),
        Status::Approved);
    EXPECT_EQ(
        toModerationStatus(
            nextTarget(TargetType::Comment, {Verdict("road_other")})),
        Status::Approved);

    EXPECT_EQ(
        toModerationStatus(
            nextTarget(TargetType::Point, {
                Verdict("road_other"),
                Verdict("no_such_verdict")})),
        Status::Approved);
    EXPECT_EQ(
        toModerationStatus(
            nextTarget(TargetType::Comment, {
                Verdict("road_other"),
                Verdict("no_such_verdict")})),
        Status::Approved);

    EXPECT_EQ(
        toModerationStatus(
            nextTarget(TargetType::Point, {Verdict("text_toloka_no_sense")})),
        Status::Disapproved);
    EXPECT_EQ(
        toModerationStatus(
            nextTarget(TargetType::Comment, {Verdict("text_toloka_no_sense")})),
        Status::Disapproved);

    EXPECT_EQ(
        toModerationStatus(
            nextTarget(TargetType::Point, {
                Verdict("text_toloka_no_sense"),
                Verdict("no_such_verdict"),})),
        Status::Disapproved);
    EXPECT_EQ(
        toModerationStatus(
            nextTarget(TargetType::Comment, {
                Verdict("text_toloka_no_sense"),
                Verdict("no_such_verdict"),})),
        Status::Disapproved);

    EXPECT_EQ(
        toModerationStatus(
            nextTarget(TargetType::Point, {
                Verdict("text_toloka_no_sense"),
                Verdict("road_other"),})),
        Status::Disapproved);
    EXPECT_EQ(
        toModerationStatus(
            nextTarget(TargetType::Comment, {
                Verdict("text_toloka_no_sense"),
                Verdict("road_other")})),
        Status::Disapproved);

    EXPECT_EQ(
        toModerationStatus(
            nextTarget(TargetType::Point, {
                Verdict("road_accident"),
                Verdict("road_time"),
                Verdict("road_other")})),
        Status::Approved);
    EXPECT_EQ(
        toModerationStatus(
            nextTarget(TargetType::Comment, {
                Verdict("road_accident"),
                Verdict("road_time"),
                Verdict("road_other")})),
        Status::Approved);

    EXPECT_EQ(
        toModerationStatus(
            nextTarget(TargetType::Point, {
                Verdict("road_accident"),
                Verdict("road_time"),
                Verdict("road_general_talks")})),
        Status::Disapproved);
    EXPECT_EQ(
        toModerationStatus(
            nextTarget(TargetType::Comment, {
                Verdict("road_accident"),
                Verdict("road_time"),
                Verdict("road_general_talks")})),
        Status::Disapproved);

    // road_general_talks is not allowed for chat/local_chat event descriptions
    EXPECT_EQ(
        toModerationStatus(
            nextTarget(TargetType::Point, {
                Verdict("road_accident"),
                Verdict("road_general_talks")},
                {"local_chat"})),
        Status::Disapproved);
    EXPECT_EQ(
        toModerationStatus(
            nextTarget(TargetType::Comment, {
                Verdict("road_accident"),
                Verdict("road_general_talks")},
                {"local_chat"})),
        Status::Disapproved);

    EXPECT_EQ(
        toModerationStatus(
            nextTarget(TargetType::Point, {
                Verdict("road_general_talks"),
                Verdict("road_accident")},
                {"chat"})),
        Status::Disapproved);
    EXPECT_EQ(
        toModerationStatus(
            nextTarget(TargetType::Comment, {
                Verdict("road_general_talks"),
                Verdict("road_accident")},
                {"chat"})),
        Status::Disapproved);

    // in theory there could be several verdicts
    EXPECT_EQ(
        toModerationStatus(
            nextTarget(TargetType::Point, {
                Verdict("road_accident"),
                Verdict("empty_text")},
                {"chat", "local_chat"})),
        Status::Disapproved);

    EXPECT_EQ(
        toModerationStatus(
            nextTarget(TargetType::Point, {
                Verdict("empty_text")})),
        Status::Approved);
    EXPECT_EQ(
        toModerationStatus(
            nextTarget(TargetType::Comment, {
                Verdict("empty_text")},
                {"accident"})),
        Status::Disapproved);
}

TEST(ModerationVerdictsMapping, TypeMapping)
{
    EXPECT_EQ(
        getPointTypeAfterModeration(
            PointType(PointTags{"reconstruction"}), {}),
        PointType(PointTags{"reconstruction"}));

    EXPECT_EQ(
        getPointTypeAfterModeration(
            PointType(PointTags{"reconstruction"}), {
                Verdict("road_reconstruction"),
            }),
        PointType(PointTags{"reconstruction"}));

    EXPECT_EQ(
        getPointTypeAfterModeration(
            PointType(PointTags{"reconstruction"}), {
                Verdict("road_reconstruction"),
                Verdict("road_accident"),
            }),
        PointType(PointTags{"reconstruction"}));

    EXPECT_EQ(
        getPointTypeAfterModeration(
            PointType(PointTags{"police", "speed_control"}), {
                Verdict("road_reconstruction"),
                Verdict("road_accident"),
            }),
        PointType(PointTags{"accident"}));

    EXPECT_EQ(
        getPointTypeAfterModeration(
            PointType(PointTags{"police", "speed_control"}), {
                Verdict("road_other"),
            }),
        PointType(PointTags{"other"}));

    EXPECT_EQ(
        getPointTypeAfterModeration(
            PointType(PointTags{"chat"}), {
                Verdict("road_reconstruction"),
                Verdict("road_accident"),
            }),
        PointType(PointTags{"chat"}));

    EXPECT_EQ(
        getPointTypeAfterModeration(
            PointType(PointTags{"chat"}), {
                Verdict("road_general_talks"),
                Verdict("road_accident"),
            }),
        PointType(PointTags{"local_chat"}));

    // https://st.yandex-team.ru/MAPSCORE-4958
    EXPECT_EQ(
        getPointTypeAfterModeration(
            PointType(PointTags{"local_chat"}), {
                Verdict("road_accident"),
            }),
        PointType(PointTags{"chat"}));

    EXPECT_EQ(
        getPointTypeAfterModeration(
            PointType(PointTags{"police", "speed_control"}), {
                Verdict("road_time"),
                Verdict("road_place"),
            }),
        PointType(PointTags{"police", "speed_control"}));

    EXPECT_EQ(
        getPointTypeAfterModeration(
            PointType(PointTags{"other"}), {
                Verdict("road_time"),
                Verdict("road_place"),
                Verdict("text_toloka_insult"),
            }),
        PointType(PointTags{"other"}));

    EXPECT_EQ(
        getPointTypeAfterModeration(
            PointType(PointTags{"other"}), {
                Verdict("road_time"),
                Verdict("road_place"),
                Verdict("text_toloka_insult"),
                Verdict("road_police"),
                Verdict("no_such_verdict"),
            }),
        PointType(PointTags{"police", "speed_control"}));
}

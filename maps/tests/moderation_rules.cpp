#include <library/cpp/testing/common/env.h>
#include <library/cpp/testing/gtest/gtest.h>
#include <maps/infopoint/lib/moderation/rule.h>
#include <maps/infopoint/tests/common/fixture.h>

#include "../lib/misc/conf.h"


using namespace infopoint::moderation;
using namespace infopoint;

const std::string SIMPLE_PREDICATES_CHECK_CONFIG = ArcadiaSourceRoot() + "/maps/infopoint/tests/data/moderation_rules/simple_predicates.json";
const std::string CHECK_SEQUENCE_CONFIG = ArcadiaSourceRoot() + "/maps/infopoint/tests/data/moderation_rules/sequence.json";

struct TestCase
{
    TargetMetadata target;
    Status expectedStatus;
    std::string name;
};

void test(std::vector<TestCase> testCases) {
    for (const auto& [target, expectedStatus, name] : testCases) {
        EXPECT_EQ(toModerationStatus(target), expectedStatus)
            << (name.empty()
                    ? ""
                    : "Case \"" + name + "\" failed");
    }
};

TEST(ModerationRules, SimplePredicatesCheck)
{
    auto config = maps::json::Value::fromFile(SIMPLE_PREDICATES_CHECK_CONFIG);
    loadRules(config["moderation_rules"]);

    auto testCases = std::vector<TestCase>{
    {
        TargetMetadata{.pointRegionIds = {0}},
        Status::Disapproved,
        "Rule not found"
    },
    {
        TargetMetadata{.pointRegionIds = {1}},
        Status:: Approved,
        "Has any region"
    },
    {
        TargetMetadata{
            .pointRegionIds = {2},
            .pointTags = {"police"}
        },
        Status:: Approved,
        "Has any tag"
    },
    {
        TargetMetadata{
            .pointRegionIds = {3},
            .pointTags = {"chat"}
        },
        Status:: Approved,
        "Has no tag"
    },
    {
        TargetMetadata{
            .pointRegionIds = {4},
            .verdicts = {Verdict("road_accident"), Verdict("nonroad")}
        },
        Status:: Approved,
        "Has any verdict"
    },
    {
        TargetMetadata{
            .pointRegionIds = {5},
            .verdicts = {Verdict("road_accident"), Verdict("nonroad")}
        },
        Status:: Approved,
        "Verdicts subset of"
    },
    {
        TargetMetadata{
            .type = TargetType::Comment,
            .pointRegionIds = {6}
        },
        Status:: Approved,
        "Has Type"
    },
    {
        TargetMetadata{
            .pointPosition = {40.2, 40.2},
            .pointRegionIds = {7}
        },
        Status:: Approved,
        "Within bbox"
    },
    {
        TargetMetadata{
            .pointRegionIds = {8},
        },
        Status:: Approved,
        "Empty verdicts approve"
    },
    {
        TargetMetadata{
            .pointRegionIds = {9},
        },
        Status:: Disapproved,
        "Empty verdicts disapprove"
    },
    {
        TargetMetadata{
            .pointRegionIds = {10},
        },
        Status:: Disapproved,
        "Default rule"
    },
    };

    test(testCases);
}

TEST(ModerationRules, ApplicationSequenceCheck)
{
    auto config = maps::json::Value::fromFile(CHECK_SEQUENCE_CONFIG);
    loadRules(config["moderation_rules"]);

    auto testCases = std::vector<TestCase>{
    {
        TargetMetadata{
            .type = TargetType::Comment,
            .pointTags = {"chat"},
            .verdicts = {Verdict("road_general_talks")}
        },
        Status::Approved,
        "check sequence of rules 1"
    },
    {
        TargetMetadata{
            .type = TargetType::Point,
            .pointTags = {"chat"},
            .verdicts = {Verdict("road_general_talks")}
        },
        Status::Disapproved,
        "check sequence of rules 2"
    },
    {
        TargetMetadata{
            .type = TargetType::Point,
            .pointTags = {"chat"},
            .verdicts = {
                Verdict("nonroad"), 
                Verdict("road_general_talks")}
        },
        Status::Disapproved,
        "check sequence of rules 3"
    },
    {
        TargetMetadata{
            .type = TargetType::Point,
            .pointPosition = {11, 11},
            .verdicts = {
                Verdict("nonroad"), 
                Verdict("road_general_talks")}
        },
        Status::Approved,
        "check sequence of rules 4"
    },
    {
        TargetMetadata{
            .type = TargetType::Point,
            .pointPosition = {11, 11},
            .verdicts = {}
        },
        Status::Disapproved,
        "check sequence of rules 5"
    },
    {
        TargetMetadata{
            .type = TargetType::Comment,
            .pointTags = {"local_chat"},
            .verdicts = {{
                Verdict("road_detour"), 
                Verdict("road_spam")}}
        },
        Status::Approved,
        "check sequence of rules 6"
    },
    {
        TargetMetadata{
            .type = TargetType::Comment,
            .pointTags = {"local_chat"},
            .verdicts = {{
                Verdict("new_verdict"), 
                Verdict("road_detour"), 
                Verdict("road_spam")}}
        },
        Status::Approved,
        "check sequence of rules 7"
    },
    {
        TargetMetadata{
            .type = TargetType::Comment,
            .pointTags = {"local_chat"},
            .verdicts = {{
                Verdict("text_toloka_obscene"), 
                Verdict("road_detour"), 
                Verdict("road_spam")}}
        },
        Status::Disapproved,
        "check sequence of rules 8"
    },
    };
    test(testCases);
}

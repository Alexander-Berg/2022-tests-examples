#include <maps/automotive/radio_service/lib/radio_search/radio_search.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/gtest/gtest.h>

#include <iostream>

using namespace maps::automotive;

struct TestData {
    std::string radioName;

    struct Query {
        std::string text;
        bool shouldPassThreshold;
    };
    std::vector<Query> queries;
};

const std::vector<std::string> TEST_STOP_WORDS = {
    "так",
    "давай",
    "быстрее",
    "еще",
};

const TVector<TestData> TEST_CASES = {
    { "европа плюс", {
        /* sorted by result weight*/
        {"европу плюс",   true},
        {"европу плюс так",true},
        {"европу плюс а", true},
        {"европу плюс даа",true},
        {"европку плюс", true},
        {"европку давай", false},
        {"дачное плюс", false},
    }},
    { "дача", {
        {"дачу", true},
        {"дачу быстрее", true},
        {"дачу еще быстрее", true},
        {"дачу мне быстрее", false},
        {"дачку", false},
        {"дачное", false},
    }},
    {"русское", {
        {"русская", true},
        {"русский", true},
        {"российское", false}
    }},
    {"монте карло", {
        {"монте карла", true},
        {"монте карлик", true},
        {"монте", false},
        {"карло", false},
        {"домик монте", false},
    }},
    {"самое крутое радио россии", {
        {"крутое радио россии", true},
        {"самое крутое радио", true},
    }},
};

TEST(radio_search, check_test_queries) {
    std::map<std::string, std::string> gidToName;
    RadioSearch::Stations stations;
    for (size_t i = 0; i < TEST_CASES.size(); ++i) {
        const auto groupId = std::to_string(i);
        stations.push_back({.synonyms = {TEST_CASES[i].radioName}, .groupId = groupId});
        gidToName.insert({groupId, TEST_CASES[i].radioName});
    }

    RadioSearch storage(stations, TEST_STOP_WORDS);

    bool testPassed = true;
    for (const auto& testCase: TEST_CASES) {
        float prevWeight = 1.f;
        for (const auto& query: testCase.queries) {
            const auto bestMatch = storage.findStation(query.text);

            const bool rightThreshold = query.shouldPassThreshold ==
                    (bestMatch->weight > RadioSearch::WEIGHT_THRESHOLD);
            const bool matchGid = testCase.radioName == gidToName.at(bestMatch->groupId);
            const bool rightOrder = bestMatch->weight <= prevWeight;

            std::cout << query.text << " -> "
                      << gidToName.at(bestMatch->groupId) << (matchGid ? "" : " (!)")
                      << " = " << bestMatch->weight << (rightThreshold ? "" : " (!)")
                      << (rightOrder ? "" : "^")
                      << std::endl;
            testPassed &= rightThreshold && matchGid && rightOrder;
            prevWeight = bestMatch->weight;
        }
    }
    ASSERT_TRUE(testPassed) << "Read stdout for more information";
}

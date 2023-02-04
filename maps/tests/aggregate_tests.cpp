#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <maps/libs/common/include/file_utils.h>

#include <maps/libs/json/include/value.h>

#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/assignments/include/aggregate.h>

using namespace testing;

namespace maps::wiki::autocart::pipeline {

namespace tests {

namespace {

const std::string JSON_DIR
    = "maps/wikimap/mapspro/services/autocart/pipeline/libs/assignments/tests/json";

} // namespace

Y_UNIT_TEST_SUITE(aggregate_results_tests)
{

Y_UNIT_TEST(aggregate_tolokers_results_test)
{
    const std::string source = TolokersResult::getName();
    const std::string INPUT_JSON_PATH
       = BinaryPath(common::joinPath(JSON_DIR, source + "_results.json"));
    const std::string AGGREGATED_JSON_PATH
       = BinaryPath(common::joinPath(JSON_DIR, source + "_aggregated.json"));

    const json::Value assignments = json::Value::fromFile(INPUT_JSON_PATH);
    const json::Value gtResultsJson = json::Value::fromFile(AGGREGATED_JSON_PATH);

    std::vector<TolokersResult> gtResults;
    for (const json::Value& gtResultJson : gtResultsJson) {
        gtResults.push_back(TolokersResult::fromJson(gtResultJson));
    }

    std::vector<TolokersResult> testResults
        = aggregateTolokersAssignments(assignments);

    EXPECT_TRUE(
        std::is_permutation(
            gtResults.begin(), gtResults.end(),
            testResults.begin()
        )
    );
}

Y_UNIT_TEST(aggregate_assessors_results_test)
{
    const std::string source = AssessorsResult::getName();
    const std::string INPUT_JSON_PATH
       = BinaryPath(common::joinPath(JSON_DIR, source + "_results.json"));
    const std::string AGGREGATED_JSON_PATH
       = BinaryPath(common::joinPath(JSON_DIR, source + "_aggregated.json"));

    const json::Value assignments = json::Value::fromFile(INPUT_JSON_PATH);
    const json::Value gtResultsJson = json::Value::fromFile(AGGREGATED_JSON_PATH);

    const std::map<std::string, std::string> loginByWorkerId{
        {"worker-id-0", "login-0"},
        {"worker-id-1", "login-1"},
        {"worker-id-2", "login-2"},
    };

    std::vector<AssessorsResult> gtResults;
    for (const json::Value& gtResultJson : gtResultsJson) {
        gtResults.push_back(AssessorsResult::fromJson(gtResultJson));
    }

    std::vector<AssessorsResult> testResults
        = aggregateAssessorsAssignments(assignments, loginByWorkerId);

    EXPECT_TRUE(
        std::is_permutation(
            gtResults.begin(), gtResults.end(),
            testResults.begin()
        )
    );
}

} // Y_UNIT_TEST_SUITE(aggregate_results_tests)

} // namespace test

} // namespace maps::wiki::autocart::pipeline

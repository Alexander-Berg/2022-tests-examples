#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <maps/libs/common/include/exception.h>

#include <maps/libs/http/include/test_utils.h>
#include <maps/libs/common/include/file_utils.h>

#include <maps/libs/chrono/include/time_point.h>

#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/assessors/include/statistics.h>

#include <chrono>
#include <fstream>
#include <unordered_map>

using namespace testing;

namespace maps::wiki::autocart::pipeline {

namespace tests {

namespace {

static const std::string SCHEMA = "https://";

static const std::string TOLOKA_HOST = "sandbox.toloka.yandex.ru";

static const std::string PROJECT_ID = "project";

} // namespace


Y_UNIT_TEST_SUITE(statistics_tests)
{

Y_UNIT_TEST(completed_tasks_for_each_assessor_test)
{
    const std::vector<std::string> POOL_IDS{"1", "2"};
    const std::string POOLS_JSON_PATH
        = "maps/wikimap/mapspro/services/autocart/pipeline/libs/assessors/tests/json/pools_1.json";
    const std::unordered_map<std::string, std::string> POOL_ID_TO_ASSIGNMENTS_PATH{
        {"1", "maps/wikimap/mapspro/services/autocart/pipeline/libs/assessors/tests/json/assignments_1.json"},
        {"2", "maps/wikimap/mapspro/services/autocart/pipeline/libs/assessors/tests/json/assignments_2.json"}
    };
    const std::unordered_map<std::string, AssessorStatistics> GT_ASSESSOR_ID_TO_STATISTICS{
        {"assessor-1", {2, 0.5}},
        {"assessor-2", {5, 1.}}
    };


    mrc::toloka::io::TolokaClient client(TOLOKA_HOST, "fake-token");
    client.setSchema(SCHEMA)
          .setMaxRequestAttempts(5)
          .setTimeout(std::chrono::seconds(1))
          .setRetryInitialTimeout(std::chrono::milliseconds(10))
          .setRetryTimeoutBackoff(1);

    http::MockHandle poolsMockHandle = http::addMock(
        SCHEMA + TOLOKA_HOST + "/api/v1/pools",
        [&](const http::MockRequest& request) {
            if (request.url.params().find("project_id=" + PROJECT_ID) != std::string::npos) {
                return http::MockResponse(
                    common::readFileToString(BinaryPath(POOLS_JSON_PATH))
                );
            } else {
                return http::MockResponse::withStatus(404);
            }
        }
    );

    http::MockHandle assignmentsMockHandle = http::addMock(
        SCHEMA + TOLOKA_HOST + "/api/v1/assignments",
        [&](const http::MockRequest& request) {
            for (const std::string& poolId : POOL_IDS) {
                if (request.url.params().find("pool_id=" + poolId) != std::string::npos) {
                    return http::MockResponse(
                        common::readFileToString(BinaryPath(POOL_ID_TO_ASSIGNMENTS_PATH.at(poolId)))
                    );
                }
            }
            return http::MockResponse::withStatus(404);
        }
    );


    chrono::TimePoint startTimePoint = chrono::parseIsoDateTime("2019-10-14T00:00:00.0");
    chrono::TimePoint endTimePoint = chrono::parseIsoDateTime("2019-10-15T00:00:00.0");

    std::unordered_map<std::string, AssessorStatistics> assessorIdToStatistics
        = getAssessorIdToStatistics(client, PROJECT_ID, startTimePoint, endTimePoint);

    EXPECT_EQ(assessorIdToStatistics.size(), GT_ASSESSOR_ID_TO_STATISTICS.size());
    for (const auto& [id, statistics] : GT_ASSESSOR_ID_TO_STATISTICS) {
        auto it = assessorIdToStatistics.find(id);
        EXPECT_TRUE(it != assessorIdToStatistics.end());
        EXPECT_EQ(statistics.completedTasksCount, it->second.completedTasksCount);
        EXPECT_EQ(statistics.quality, it->second.quality);
    }
}

Y_UNIT_TEST(completed_tasks_for_one_assessor_test)
{
    const std::vector<std::string> POOL_IDS{"3"};
    const std::string POOLS_JSON_PATH
        = "maps/wikimap/mapspro/services/autocart/pipeline/libs/assessors/tests/json/pools_2.json";
    const std::unordered_map<std::string, std::string> POOL_ID_TO_ASSIGNMENTS_PATH{
        {"3", "maps/wikimap/mapspro/services/autocart/pipeline/libs/assessors/tests/json/assignments_3.json"},
    };
    const std::string ASSESSOR_ID = "assessor-3";
    const std::vector<AssessorTaskSolution> ASSESSOR_SOLUTIONS{
        {
            {"https://yandex.ru/map1.jpg", "https://yandex.ru/bld1.jpg"},
            {TolokaState::Yes, 3, 102},
            AssessorGoldenAnswer{
                TolokaState::Yes,
                HeightRange(3, 6),
                FTTypeList(std::set<int>({102, 106}))
            }
        },
        {
            {"https://yandex.ru/map2.jpg", "https://yandex.ru/bld2.jpg"},
            {TolokaState::Yes, 9, 106},
            std::nullopt
        }
    };

    mrc::toloka::io::TolokaClient client(TOLOKA_HOST, "fake-token");
    client.setSchema(SCHEMA)
          .setMaxRequestAttempts(5)
          .setTimeout(std::chrono::seconds(1))
          .setRetryInitialTimeout(std::chrono::milliseconds(10))
          .setRetryTimeoutBackoff(1);

    http::MockHandle poolsMockHandle = http::addMock(
        SCHEMA + TOLOKA_HOST + "/api/v1/pools",
        [&](const http::MockRequest& request) {
            if (request.url.params().find("project_id=" + PROJECT_ID) != std::string::npos) {
                return http::MockResponse(
                    common::readFileToString(BinaryPath(POOLS_JSON_PATH))
                );
            } else {
                return http::MockResponse::withStatus(404);
            }
        }
    );

    http::MockHandle assignmentsMockHandle = http::addMock(
        SCHEMA + TOLOKA_HOST + "/api/v1/assignments",
        [&](const http::MockRequest& request) {
            for (const std::string& poolId : POOL_IDS) {
                if (request.url.params().find("pool_id=" + poolId) != std::string::npos) {
                    return http::MockResponse(
                        common::readFileToString(BinaryPath(POOL_ID_TO_ASSIGNMENTS_PATH.at(poolId)))
                    );
                }
            }
            return http::MockResponse::withStatus(404);
        }
    );


    chrono::TimePoint startTimePoint = chrono::parseIsoDateTime("2019-10-14T00:00:00.0");
    chrono::TimePoint endTimePoint = chrono::parseIsoDateTime("2019-10-15T00:00:00.0");

    std::vector<AssessorTaskSolution> solutions
        = getAssessorSolutions(client, ASSESSOR_ID, PROJECT_ID, startTimePoint, endTimePoint);

    EXPECT_EQ(solutions.size(), ASSESSOR_SOLUTIONS.size());
    for (size_t i = 0; i < solutions.size(); i++) {
        EXPECT_EQ(solutions[i].task.bldURL, ASSESSOR_SOLUTIONS[i].task.bldURL);
        EXPECT_EQ(solutions[i].task.mapURL, ASSESSOR_SOLUTIONS[i].task.mapURL);
        EXPECT_EQ(solutions[i].answer.state, ASSESSOR_SOLUTIONS[i].answer.state);
        EXPECT_EQ(solutions[i].answer.height, ASSESSOR_SOLUTIONS[i].answer.height);
        EXPECT_EQ(solutions[i].answer.ftTypeId, ASSESSOR_SOLUTIONS[i].answer.ftTypeId);
        EXPECT_TRUE(
            (solutions[i].golden.has_value() && ASSESSOR_SOLUTIONS[i].golden.has_value()) ||
            (!solutions[i].golden.has_value() && !ASSESSOR_SOLUTIONS[i].golden.has_value())
        );
        if (solutions[i].golden.has_value()) {
            EXPECT_EQ(solutions[i].golden->state, ASSESSOR_SOLUTIONS[i].golden->state);
            EXPECT_EQ(solutions[i].golden->heightRange.min, ASSESSOR_SOLUTIONS[i].golden->heightRange.min);
            EXPECT_EQ(solutions[i].golden->heightRange.max, ASSESSOR_SOLUTIONS[i].golden->heightRange.max);
            EXPECT_TRUE(solutions[i].golden->ftTypeList.ids == ASSESSOR_SOLUTIONS[i].golden->ftTypeList.ids);
        }
    }
}

} // Y_UNIT_TEST_SUITE(statistics_tests)

} // namespace test

} // namespace maps::wiki::autocart::pipeline

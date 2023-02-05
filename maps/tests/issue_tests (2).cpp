#include "helpers.h"
#include <yandex/maps/wiki/diffalert/storage/issue_creator.h>
#include <yandex/maps/wiki/diffalert/storage/results_viewer.h>
#include <yandex/maps/wiki/diffalert/storage/results_writer.h>
#include <yandex/maps/wiki/diffalert/storage/stored_message.h>
#include <yandex/maps/wiki/unittest/arcadia.h>

#include <maps/libs/http/include/test_utils.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace maps::wiki::diffalert::tests {

namespace {

const TaskId TEST_TASK_ID = 123;
const TId TEST_OBJECT_ID_1 = 1;
const TId BRANCH_ID = 456;

const std::string REPORTER_LOGIN = "test-user";

const std::string EMPTY_DESCRIPTION;
const std::string EMPTY_CATEGORY_ID;

const std::string CATEGORY_GROUP = "rd";

const int HTTP_CODE_CONFLICT = 409;

const std::string STARTREK_URL = "https://st-api.test.yandex-team.ru";
const std::string OAUTH_TOKEN = "test token";

const std::string PAGE_URL = "https://mpro.maps.yandex.ru/?ll=37.622504%2C55.753215&z=10&l=mp%23sat&branch=0&activity=longtasks&id=295355&viewer=diffalert-results&type=diffalert&created-by=all";

} // namespace

Y_UNIT_TEST_SUITE_F(startrek, unittest::ArcadiaDbFixture) {

Y_UNIT_TEST(test_create_issue)
{
    {
        ResultsWriter writer(TEST_TASK_ID, pool());
        std::list<StoredMessage> data;
        data.emplace_back(
            Message(TEST_OBJECT_ID_1, Priority{0, 1}, "test-description", Message::Scope::WholeObject),
            "test-category", "name-ru", HasOwnName::Yes, Envelope());
        writer.put(std::move(data));
        writer.finish();
    }

    auto txn = pool().masterWriteableTransaction();

    ResultsViewer viewer(TEST_TASK_ID, *txn);
    auto messages = viewer.messages({}, SortKind::BySize, 0, 1);

    IssueCreator issueCreator(STARTREK_URL, OAUTH_TOKEN);

    // Try create an issue for the existent message
    {
        auto stMock = http::addMock(
            STARTREK_URL + "/v2/issues",
            [&](const http::MockRequest& request) {
                auto requestBody = json::Value::fromString(request.body);

                EXPECT_EQ(requestBody["queue"].as<std::string>(), "MAPSERRORS");
                EXPECT_EQ(
                    requestBody["unique"].as<std::string>(),
                    "diffalert_" + std::to_string(messages.front().id()));
                EXPECT_EQ(
                    requestBody["summary"].as<std::string>(),
                    "Ошибка в релизной ветке #" + std::to_string(BRANCH_ID));

                std::set<std::string> EXPECTED_TAGS = {
                    "rd",
                    "diffalert_priority_0",
                    "object_1",
                    "diffalert_1",
                    "login_test-user"
                };
                EXPECT_EQ(
                    requestBody["tags"].as<std::set<std::string>>(),
                    EXPECTED_TAGS);

                return http::MockResponse::fromFile(SRC_("tests_data/create_issue_response.json"));
            });

        auto key = issueCreator.getOrCreateIssue(
            *txn, messages.front(), BRANCH_ID, REPORTER_LOGIN, CATEGORY_GROUP, PAGE_URL);

        EXPECT_EQ(key, "MAPSERRORS-1");
    }

    // Try get already created issue
    {
        auto stMock = http::addMock(
            STARTREK_URL + "/v2/issues",
            [](const http::MockRequest&) {
                EXPECT_TRUE(false); //startrek should not be called
                return http::MockResponse();
            });

        auto key = issueCreator.getOrCreateIssue(
            *txn, messages.front(), BRANCH_ID, REPORTER_LOGIN, CATEGORY_GROUP, PAGE_URL);

        EXPECT_EQ(key, "MAPSERRORS-1");
    }

    txn->exec("TRUNCATE diffalert.startrek_issue");

    // Try load already created issue
    {
        auto stMock = http::addMock(
            STARTREK_URL + "/v2/issues",
            [&](const http::MockRequest& request) {
                if (request.method == http::GET) {
                    auto uniqueTag = "diffalert_" + std::to_string(messages.front().id());
                    EXPECT_FALSE(request.url.params().find(uniqueTag) == std::string::npos);

                    auto response = http::MockResponse::fromFile(SRC_("tests_data/load_issues_response.json"));
                    response.headers.emplace("X-Total-Count", "1");
                    return response;
                } else {
                    return http::MockResponse::withStatus(HTTP_CODE_CONFLICT);
                }
            });

        auto key = issueCreator.getOrCreateIssue(
            *txn, messages.front(), BRANCH_ID, REPORTER_LOGIN, CATEGORY_GROUP, PAGE_URL);

        EXPECT_EQ(key, "MAPSERRORS-1");
    }
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::diffalert::tests

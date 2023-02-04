#include <yandex/maps/wiki/common/geom.h>
#include <yandex/maps/wiki/validator/storage/issue_creator.h>
#include <yandex/maps/wiki/validator/storage/message_attributes_filter.h>
#include <yandex/maps/wiki/validator/storage/messages_writer.h>
#include <yandex/maps/wiki/validator/storage/results_gateway.h>
#include <yandex/maps/wiki/validator/storage/stored_message_data.h>

#include "helpers.h"
#include <yandex/maps/wiki/unittest/arcadia.h>
#include <maps/libs/http/include/test_utils.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/env.h>

#include <boost/lexical_cast.hpp>

namespace maps::wiki::validator::tests {

namespace {

const TId TEST_BRANCH_ID = 123;
const TTaskId TEST_TASK_ID = 456;
const auto TEST_MESSAGE_ID = storage::MessageId(1, 1);

const Severity TEST_SEVERITY = Severity::Critical;
const std::string TEST_CHECK_ID = "strong_connectivity";
const std::string TEST_DESCRIPTION = "extraneous-scc";

const std::string TEST_LOGIN = "test-user";
const std::string TEST_PAGE_URL = "https://mpro.mr-spock.mpro.maps.dev.yandex.ru/?ll=50.0%2C50.0&z=15&l=mp%23sat&branch=0&activity=longtasks&id=123&viewer=validation-results";

const std::string STARTREK_URL = "https://st-api.test.yandex-team.ru";
const std::string OAUTH_TOKEN = "test-token";

const int HTTP_CODE_CONFLICT = 409;

} // namespace

Y_UNIT_TEST_SUITE_F(startrek, unittest::ArcadiaDbFixture) {

Y_UNIT_TEST(test_create_issue)
{
    {
        storage::MessagesWriter writer(pool(), TEST_TASK_ID);
        std::vector<Message> data;
        data.emplace_back(Message(TEST_SEVERITY, TEST_CHECK_ID, TEST_DESCRIPTION, RegionType::Important,
            common::wkt2wkb("POINT(0 0)"), { revision::RevisionID(1, 1), revision::RevisionID(2, 2) }));
        writer.writeMessagesBatchWithRetries(data);
    }

    auto txn = pool().masterWriteableTransaction();

    storage::ResultsGateway gateway(*txn, TEST_TASK_ID);
    auto messages = gateway.messages(storage::MessageAttributesFilter(), headCommitSnapshot(*txn), 0, 10, TEST_UID);

    storage::IssueCreator issueCreator(STARTREK_URL, OAUTH_TOKEN);

    // Try create an issue for the existent message
    {
        auto stMock = http::addMock(
            STARTREK_URL + "/v2/issues",
            [&](const http::MockRequest& request) {
                auto requestBody = json::Value::fromString(request.body);

                EXPECT_EQ(requestBody["queue"].as<std::string>(), "MAPSERRORS");
                EXPECT_EQ(
                    requestBody["unique"].as<std::string>(),
                    "validation_" + boost::lexical_cast<std::string>(TEST_MESSAGE_ID));
                EXPECT_EQ(
                    requestBody["summary"].as<std::string>(),
                    "Ошибка в релизной ветке #" + std::to_string(TEST_BRANCH_ID));

                std::set<std::string> EXPECTED_TAGS = {
                    "validation_1-1",
                    "severity_crit",
                    "check_strong_connectivity",
                    "description_extraneous-scc",
                    "login_test-user"
                };
                EXPECT_EQ(
                    requestBody["tags"].as<std::set<std::string>>(),
                    EXPECTED_TAGS);

                return http::MockResponse::fromFile(SRC_("data/create_issue_response.json"));
            });

        auto key = issueCreator.getOrCreateIssue(
            *txn, TEST_MESSAGE_ID, TEST_BRANCH_ID, TEST_LOGIN, TEST_PAGE_URL);

        EXPECT_EQ(key, "MAPSERRORS-1");
    }

    // Try get already created issue
    {
        auto stMock = http::addMock(
            STARTREK_URL + "/v2/issues",
            [](const http::MockRequest&) {
                EXPECT_TRUE(false); // Startrek should not be called
                return http::MockResponse();
            });

        auto key = issueCreator.getOrCreateIssue(
            *txn, TEST_MESSAGE_ID, TEST_BRANCH_ID, TEST_LOGIN, TEST_PAGE_URL);

        EXPECT_EQ(key, "MAPSERRORS-1");
    }

    txn->exec("TRUNCATE diffalert.startrek_issue");

    // Try load already created issue
    {
        auto stMock = http::addMock(
            STARTREK_URL + "/v2/issues",
            [&](const http::MockRequest& request) {
                if (request.method == http::GET) {
                    auto uniqueTag = "validation_" +
                        boost::lexical_cast<std::string>(TEST_MESSAGE_ID);
                    EXPECT_FALSE(request.url.params().find(uniqueTag) == std::string::npos);

                    auto response = http::MockResponse::fromFile(SRC_("data/load_issues_response.json"));
                    response.headers.emplace("X-Total-Count", "1");
                    return response;
                } else {
                    return http::MockResponse::withStatus(HTTP_CODE_CONFLICT);
                }
            });

        auto key = issueCreator.getOrCreateIssue(
            *txn, TEST_MESSAGE_ID, TEST_BRANCH_ID, TEST_LOGIN, TEST_PAGE_URL);

        EXPECT_EQ(key, "MAPSERRORS-1");
    }
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::validator::tests

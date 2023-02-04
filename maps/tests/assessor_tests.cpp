#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <maps/libs/common/include/exception.h>

#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>

#include <maps/libs/http/include/test_utils.h>
#include <maps/libs/common/include/file_utils.h>

#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/assessors/include/assessor.h>

#include <fstream>
#include <unordered_map>

using namespace testing;

namespace maps::wiki::autocart::pipeline {

namespace tests {

namespace {

static const std::string TABLE_JSON_PATH
    = "maps/wikimap/mapspro/services/autocart/pipeline/libs/assessors/tests/json/table.json";

static const std::vector<Assessor> GT_ASSESSORS{
    {"id-1", "login-1", "company-1"},
    {"id-2", "login-2", "company-2"}
};

} // namespace


Y_UNIT_TEST_SUITE(assessor_tests)
{

Y_UNIT_TEST(wiki_table_test)
{
    const std::string name = "users/table";

    http::MockHandle wikiMockHandle = http::addMock(
        "https://wiki-api.yandex-team.ru/_api/frontend/" + name + "/.grid",
        [&](const http::MockRequest&) {
            return http::MockResponse(
                common::readFileToString(BinaryPath(TABLE_JSON_PATH))
            );
        }
    );

    std::vector<Assessor> assessors = loadAssessorsFromWikiTable(name, "fake-token");

    EXPECT_TRUE(
        std::is_permutation(
            assessors.begin(), assessors.end(),
            GT_ASSESSORS.begin(),
            [](const Assessor& lhs, const Assessor& rhs) {
                return lhs.id == rhs.id
                    && lhs.login == rhs.login
                    && lhs.company == rhs.company;
            }
        )
    );
}

Y_UNIT_TEST(login_by_worker_id)
{
    const std::string name = "users/table";

    http::MockHandle wikiMockHandle = http::addMock(
        "https://wiki-api.yandex-team.ru/_api/frontend/" + name + "/.grid",
        [&](const http::MockRequest&) {
            return http::MockResponse(
                common::readFileToString(BinaryPath(TABLE_JSON_PATH))
            );
        }
    );

    const std::map<std::string, std::string> loginByWorkerId
        = loadLoginByWorkerIdFromWikiTable(name, "fake-token");

    const std::map<std::string, std::string> expected{
        {"id-1", "login-1"},
        {"id-2", "login-2"},
    };

    EXPECT_TRUE(loginByWorkerId == expected);
}

Y_UNIT_TEST(yt_table_test)
{
    const TString name = "//home/table";

    NYT::IClientPtr client = NYT::NTesting::CreateTestClient();

    NYT::TTableWriterPtr<NYT::TNode> writer
        = client->CreateTableWriter<NYT::TNode>(name);
    for (const Assessor& assessor : GT_ASSESSORS) {
        writer->AddRow(assessor.toYTNode());
    }

    std::vector<Assessor> assessors = loadAssessorsFromYTTable(client, name);

    EXPECT_TRUE(
        std::is_permutation(
            assessors.begin(), assessors.end(),
            GT_ASSESSORS.begin(),
            [](const Assessor& lhs, const Assessor& rhs) {
                return lhs.id == rhs.id
                    && lhs.login == rhs.login
                    && lhs.company == rhs.company;
            }
        )
    );
}

Y_UNIT_TEST(less_test)
{
    EXPECT_TRUE(Assessor({"a", "a", "a"}) < Assessor({"a", "a", "b"}));
    EXPECT_TRUE(Assessor({"a", "a", "b"}) < Assessor({"a", "b", "a"}));
    EXPECT_TRUE(Assessor({"a", "a", "b"}) < Assessor({"b", "a", "a"}));
    EXPECT_FALSE(Assessor({"a", "a", "b"}) < Assessor({"a", "a", "a"}));
    EXPECT_FALSE(Assessor({"a", "b", "a"}) < Assessor({"a", "a", "a"}));
    EXPECT_FALSE(Assessor({"b", "a", "a"}) < Assessor({"a", "a", "a"}));
}

Y_UNIT_TEST(equal_test)
{
    EXPECT_TRUE(Assessor({"a", "b", "c"}) == Assessor({"a", "b", "c"}));
    EXPECT_FALSE(Assessor({"a", "b", "c"}) == Assessor({"a", "b", "d"}));
    EXPECT_FALSE(Assessor({"a", "b", "c"}) == Assessor({"a", "d", "c"}));
    EXPECT_FALSE(Assessor({"a", "b", "c"}) == Assessor({"d", "b", "c"}));
}

} // Y_UNIT_TEST_SUITE(assessor_tests)

} // namespace test

} // namespace maps::wiki::autocart::pipeline

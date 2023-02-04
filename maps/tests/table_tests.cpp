#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <maps/libs/common/include/exception.h>

#include <maps/libs/http/include/test_utils.h>
#include <maps/libs/common/include/file_utils.h>

#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/wiki/include/table.h>

#include <fstream>
#include <unordered_map>

using namespace testing;

namespace maps::wiki::autocart::pipeline {

namespace tests {

namespace {

static const std::string TABLE_JSON_PATH
    = "maps/wikimap/mapspro/services/autocart/pipeline/libs/wiki/tests/json/table.json";

static const WikiTable GT_WIKI_TABLE{
    {{"login", "login-1"}, {"id", "id-1"}, {"company", "company-1"}},
    {{"login", "login-2"}, {"id", "id-2"}, {"company", "company-2"}},
};

} // namespace


Y_UNIT_TEST_SUITE(wiki_table_tests)
{

Y_UNIT_TEST(base_test)
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

    WikiTable testWikiTable = loadWikiTable(name, "fake-token");

    EXPECT_EQ(testWikiTable.size(), GT_WIKI_TABLE.size());
    for (size_t i = 0; i < testWikiTable.size(); i++) {
        for (const auto& [name, value] : testWikiTable[i]) {
            EXPECT_TRUE(GT_WIKI_TABLE[i].count(name) > 0);
            EXPECT_EQ(GT_WIKI_TABLE[i].at(name), value);
        }
    }
}

} // Y_UNIT_TEST_SUITE(wiki_table_tests)

} // namespace test

} // namespace maps::wiki::autocart::pipeline

#include <yandex/maps/wiki/editor_takeout/editor_takeout.h>
#include <yandex/maps/wiki/unittest/arcadia.h>
#include <maps/libs/common/include/file_utils.h>
#include <maps/libs/json/include/value.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <fstream>
#include <string>

using namespace std::string_literals;

namespace maps::wiki::editor_takeout::tests {

namespace {

const TId TEST_UID = 1;

const auto JSON_DATE_TIME = "dateTime"s;

const auto TEST_FILENAME_SUFFIX_EDITS = "edits_1.json"s;
const auto TEST_FILENAME_SUFFIX_MESSAGES = "messages_1.json"s;

const auto TEST_DATA_EMPTY_EDITS = "[]"s;
const auto TEST_DATA_EMPTY_MESSAGES = "[]"s;

void
testUploadCallback(
    const std::string& filenameSuffix,
    const std::string data,
    const std::string& testFilenameSuffix,
    const std::string& testData)
{
    UNIT_ASSERT_VALUES_EQUAL(filenameSuffix, testFilenameSuffix);

    auto jsonData = json::Value::fromString(data);
    auto resultJsonData = json::Value::fromString(testData);

    UNIT_ASSERT_EQUAL(jsonData, resultJsonData);
}

} // namespace

Y_UNIT_TEST_SUITE(editor_takeout_tests) {

Y_UNIT_TEST_F(test_empty_edits, unittest::ArcadiaDbFixture) {
    generateTakeoutEdits(pool(), TEST_UID,
        [&](const std::string& filenameSuffix, const std::string data) {
            return testUploadCallback(
                filenameSuffix,
                data,
                TEST_FILENAME_SUFFIX_EDITS,
                TEST_DATA_EMPTY_EDITS);
        });
}

Y_UNIT_TEST_F(test_empty_messages, unittest::ArcadiaDbFixture) {
    generateTakeoutMessages(pool(), TEST_UID,
        [&](const std::string& filenameSuffix, const std::string data) {
            return testUploadCallback(
                filenameSuffix,
                data,
                TEST_FILENAME_SUFFIX_MESSAGES,
                TEST_DATA_EMPTY_MESSAGES);
        });
}

Y_UNIT_TEST_F(test_takeout_edits, unittest::ArcadiaDbFixture) {
    database().executeSql(common::readFileToString(
        SRC_("data/test_takeout_edits_input.sql")));
    generateTakeoutEdits(pool(), TEST_UID,
        [&](const std::string& filenameSuffix, const std::string data) {
            return testUploadCallback(
                filenameSuffix,
                data,
                TEST_FILENAME_SUFFIX_EDITS,
                common::readFileToString(
                    SRC_("data/test_takeout_edits_result.json"s)));
        });
}

Y_UNIT_TEST_F(test_takeout_messages, unittest::ArcadiaDbFixture) {
    database().executeSql(common::readFileToString(
        SRC_("data/test_takeout_messages_input.sql")));
    generateTakeoutMessages(pool(), TEST_UID,
        [&](const std::string& filenameSuffix, const std::string data) {
            return testUploadCallback(
                filenameSuffix,
                data,
                TEST_FILENAME_SUFFIX_MESSAGES,
                common::readFileToString(
                    SRC_("data/test_takeout_messages_result.json"s)));
        });
}

} // Y_UNIT_TEST_SUITE(editor_takeout_tests)

} // namespace maps::wiki::editor_takeout::tests

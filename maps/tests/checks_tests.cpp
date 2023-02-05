#include "helpers.h"
#include "parsed_message.h"

#include <yandex/maps/wiki/diffalert/runner.h>
#include <yandex/maps/wiki/diffalert/revision/runner.h>
#include <yandex/maps/wiki/diffalert/revision/diff_context.h>
#include <yandex/maps/wiki/diffalert/revision/editor_config.h>

#include <maps/libs/common/include/file_utils.h>
#include <maps/libs/json/include/value.h>

#include <boost/algorithm/string/replace.hpp>
#include <boost/date_time/gregorian/gregorian.hpp>

#include <chrono>
#include <filesystem>

namespace fs = std::filesystem;
namespace bg = boost::gregorian;

namespace maps {
namespace wiki {
namespace diffalert {
namespace tests {

namespace {

const std::string CHECKS_DATA_DIR_PATH = "checks_tests_data";

struct TestCase
{
    std::string name;

    std::string beforeJsonFile;
    std::string afterJsonFile;
    std::vector<ParsedMessage> editorExpected;
    std::vector<ParsedMessage> longtaskExpected;
};

std::vector<TestCase> discoverTestCases()
{
    std::vector<TestCase> result;

    fs::path casesDir(dataPath(CHECKS_DATA_DIR_PATH));
    fs::directory_iterator endIt;
    for (fs::directory_iterator it(casesDir); it != endIt; ++it) {
        const auto& casePath = it->path();
        const auto casePathStr = casePath.string();
        if (!casePathStr.ends_with(".case.json")) {
            continue;
        }

        auto caseJson = json::Value::fromFile(casePathStr);

        TestCase testCase;

        testCase.name = casePath.filename().string();
        testCase.beforeJsonFile = dataPath(CHECKS_DATA_DIR_PATH + "/" + caseJson["before"].toString());
        testCase.afterJsonFile = dataPath(CHECKS_DATA_DIR_PATH + "/" + caseJson["after"].toString());

        if (caseJson.hasField("expected")) {
            testCase.longtaskExpected = messagesFromJson(caseJson["expected"]);
            testCase.editorExpected = testCase.longtaskExpected;
        }
        if (caseJson.hasField("editor_specific_expected")) {
            auto editorMessages =
                messagesFromJson(caseJson["editor_specific_expected"]);
            testCase.editorExpected.insert(
                testCase.editorExpected.end(),
                editorMessages.begin(),
                editorMessages.end());
        }
        if (caseJson.hasField("longtask_specific_expected")) {
            auto longtaskMessages =
                messagesFromJson(caseJson["longtask_specific_expected"]);
            testCase.longtaskExpected.insert(
                testCase.longtaskExpected.end(),
                longtaskMessages.begin(),
                longtaskMessages.end());
        }

        result.push_back(std::move(testCase));
    }

    return result;
}

std::string jsonFileLoader(const std::string& jsonFile)
{
    REQUIRE(fs::exists(fs::path(jsonFile)), "File " << jsonFile << " is not found");

    auto str = maps::common::readFileToString(jsonFile);
    if (jsonFile.ends_with(".json.tmpl")) {
        auto today = bg::day_clock::local_day();
        auto nextYear = today + bg::date_duration(365);
        auto todayPlus45 = today + bg::date_duration(45);
        auto tomorrow = today + bg::date_duration(1);
        auto yesterday = today - bg::date_duration(1);

        boost::replace_all(str, "FAR_DATE", bg::to_iso_string(nextYear));
        boost::replace_all(str, "WARN_DATE", bg::to_iso_string(todayPlus45));
        boost::replace_all(str, "CRIT_DATE", bg::to_iso_string(tomorrow));
        boost::replace_all(str, "PAST_DATE", bg::to_iso_string(yesterday));
    }
    return std::move(str);
}

void runTest(const TestCase& testCase)
{
    try {
        auto snapshotsPair = loadData(
            testCase.beforeJsonFile, testCase.afterJsonFile, jsonFileLoader);

        EditorConfig editorConfig(EDITOR_CONFIG_PATH);
        auto result = LongtaskDiffContext::compareSnapshots(
                snapshotsPair.oldBranch, snapshotsPair.oldSnapshotId,
                snapshotsPair.newBranch, snapshotsPair.newSnapshotId,
                RevisionDB::pool(),
                ViewDB::pool(),
                editorConfig);

        UNIT_ASSERT(result.badObjects().empty());
        UNIT_ASSERT(result.badRelations().empty());

        compare(
            messagesFromOutput(runEditorChecks, result.diffContexts()),
            testCase.editorExpected,
            "editor"
        );
        compare(
            messagesFromOutput(runLongTaskChecks, result.diffContexts()),
            testCase.longtaskExpected,
            "longtask"
        );
    } catch (const maps::Exception& ex) {
        FATAL() << "TRACE: " << ex;
        throw;
    }
}

} // namespace

Y_UNIT_TEST_SUITE_F(checks, SetLogLevelFixture) {

struct TestCaseExecutor : public ::NUnitTest::TBaseTestCase
{
    TestCaseExecutor(TestCase testCase)
        : ::NUnitTest::TBaseTestCase()
        , testCase_(std::move(testCase))
    {
        Name_ = testCase_.name.c_str();
    }

    void Execute_(NUnitTest::TTestContext&) override
    {
        runTest(testCase_);
    }

    TestCase testCase_;
};

struct ChecksTestRegistration
{
    ChecksTestRegistration()
    {
        auto testCases = discoverTestCases();
        for (const auto& testCase : testCases) {
            TCurrentTest::AddTest([testCase = std::move(testCase)]() {
                return ::MakeHolder<TestCaseExecutor>(std::move(testCase));
            });
        }
    }
};
static ChecksTestRegistration checksTestRegistration;

} // Y_UNIT_TEST_SUITE_F

} // namespace tests
} // namespace diffalert
} // namespace wiki
} // namespace maps

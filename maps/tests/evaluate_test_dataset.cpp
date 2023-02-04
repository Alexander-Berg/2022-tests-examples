#include <maps/wikimap/mapspro/services/autocart/tools/auto_toloker/test/lib/include/evaluate_classifier.h>

#include <yandex/maps/shell_cmd.h>
#include <maps/libs/common/include/exception.h>
#include <maps/libs/common/include/file_utils.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include <vector>
#include <utility>

using namespace testing;

namespace maps::wiki::autocart {

namespace tests {

namespace {

const std::string TEST_DATASET_ARCHIVE
    = common::joinPath(SRC_("data"), "test_dataset.tar.gz");

const std::string TEST_DATASET_DIR = SRC_("dataset");

void createDir(const std::string& path) {
    std::string removeAndCreateCmd = "rm -fdr " + path + "; mkdir " + path;
    maps::shell::runCmd(removeAndCreateCmd);
}

void removeDir(const std::string& path) {
    std::string removeCmd = "rm -fdr " + path;
    maps::shell::runCmd(removeCmd);
}

void unpack(const std::string& path, const std::string outputDir) {
    std::string unpackCmd = "tar -xf " + path + " -C " + outputDir;
    shell::runCmd(unpackCmd);
}

} // namespace

Y_UNIT_TEST_SUITE(auto_toloker_evaluate_classifier)
{

Y_UNIT_TEST(evaluate_on_test_dataset)
{
    TestResult expectedResult;
    expectedResult.truePositive = 52388;
    expectedResult.falsePositive = 11408;
    expectedResult.trueNegative = 63740;
    expectedResult.falseNegative = 10078;

    createDir(TEST_DATASET_DIR);
    unpack(TEST_DATASET_ARCHIVE, TEST_DATASET_DIR);
    TestResult testResult = evaluateClassifier(TEST_DATASET_DIR);
    removeDir(TEST_DATASET_DIR);

    EXPECT_THAT(testResult.truePositive, Eq(expectedResult.truePositive));
    EXPECT_THAT(testResult.falsePositive, Eq(expectedResult.falsePositive));
    EXPECT_THAT(testResult.trueNegative, Eq(expectedResult.trueNegative));
    EXPECT_THAT(testResult.falseNegative, Eq(expectedResult.falseNegative));
}

} // Y_UNIT_TEST_SUITE(auto_toloker_evaluate_classifier)

} // namespace test

} // namespace maps::wiki::autocart

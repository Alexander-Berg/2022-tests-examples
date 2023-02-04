#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include <maps/libs/common/include/exception.h>
#include <maps/libs/common/include/file_utils.h>

#include <maps/libs/json/include/value.h>

#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/config/include/config.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/config/include/strings.h>

namespace maps::wiki::autocart::pipeline {

namespace tests {

namespace {

const std::string DETECTOR_CONFIG_PATH
    = "maps/wikimap/mapspro/services/autocart/pipeline/libs/config/tests/json/detector.json";

const std::string TOLOKERS_CONFIG_PATH
    = "maps/wikimap/mapspro/services/autocart/pipeline/libs/config/tests/json/tolokers.json";

const std::string ASSESSORS_CONFIG_PATH
    = "maps/wikimap/mapspro/services/autocart/pipeline/libs/config/tests/json/assessors.json";

const std::string AUTO_TOLOKER_CONFIG_PATH
    = "maps/wikimap/mapspro/services/autocart/pipeline/libs/config/tests/json/auto_toloker.json";

const std::string YT_CONFIG_PATH
    = "maps/wikimap/mapspro/services/autocart/pipeline/libs/config/tests/json/yt.json";

}

Y_UNIT_TEST_SUITE(config_tests)
{

Y_UNIT_TEST(detector_config_test)
{
    const json::Value value = json::Value::fromFile(BinaryPath(DETECTOR_CONFIG_PATH));

    const DetectorConfig config(BinaryPath(DETECTOR_CONFIG_PATH));

    EXPECT_EQ(
        config.dwellplacesYTFolder(),
        TString(value[FIELD_DWELLPLACES_YT_FOLDER].as<std::string>())
    );

    EXPECT_DOUBLE_EQ(
        config.cellSizeMercator(),
        value[FIELD_CELL_SIZE_MERCATOR].as<double>()
    );
    EXPECT_DOUBLE_EQ(
        config.padSizeMercator(),
        value[FIELD_PAD_SIZE_MERCATOR].as<double>()
    );
    EXPECT_DOUBLE_EQ(
        config.dwellplaceDistanceMeters(),
        value[FIELD_DWELLPLACE_DISTANCE_METERS].as<double>()
    );
    EXPECT_EQ(
        config.mode(),
        TString(value[FIELD_MODE].as<std::string>())
    );
    EXPECT_EQ(
        config.zoom(),
        value[FIELD_ZOOM].as<size_t>()
    );
    EXPECT_EQ(
        config.jobSize(),
        value[FIELD_JOB_SIZE].as<size_t>()
    );
    EXPECT_EQ(
        config.runningJobCount(),
        value[FIELD_RUNNING_JOB_COUNT].as<size_t>()
    );
    EXPECT_EQ(
        config.tileSourceURL(),
        TString(value[FIELD_TILE_SOURCE_URL].as<std::string>())
    );
    EXPECT_EQ(
        config.rejectedIOUThreshold(),
        value[FIELD_REJECTED_IOU_THRESHOLD].as<double>()
    );
}

Y_UNIT_TEST(auto_toloker_config_test)
{
    const json::Value value = json::Value::fromFile(BinaryPath(AUTO_TOLOKER_CONFIG_PATH));

    const AutoTolokerConfig config(BinaryPath(AUTO_TOLOKER_CONFIG_PATH));

    EXPECT_DOUBLE_EQ(
        config.threshold(),
        value[FIELD_THRESHOLD].as<double>()
    );
    EXPECT_EQ(
        config.zoom(),
        value[FIELD_ZOOM].as<size_t>()
    );
    EXPECT_DOUBLE_EQ(
        config.padRatio(),
        value[FIELD_PAD_RATIO].as<double>()
    );
    EXPECT_EQ(
        config.jobSize(),
        value[FIELD_JOB_SIZE].as<size_t>()
    );
    EXPECT_EQ(
        config.runningJobCount(),
        value[FIELD_RUNNING_JOB_COUNT].as<size_t>()
    );
    EXPECT_EQ(
        config.tileSourceURL(),
        TString(value[FIELD_TILE_SOURCE_URL].as<std::string>())
    );
}

Y_UNIT_TEST(tolokers_config_test)
{
    const json::Value value = json::Value::fromFile(BinaryPath(TOLOKERS_CONFIG_PATH));

    const TolokersConfig config(BinaryPath(TOLOKERS_CONFIG_PATH));

    EXPECT_EQ(
        config.minGoldenTasksCount(),
        value[FIELD_MIXER_CONFIG][FIELD_MIN_GOLDEN_TASKS_COUNT].as<size_t>()
    );
    EXPECT_EQ(
        config.goldenTasksCount(),
        value[FIELD_MIXER_CONFIG][FIELD_GOLDEN_TASKS_COUNT].as<size_t>()
    );
    EXPECT_EQ(
        config.minRealTasksCount(),
        value[FIELD_MIXER_CONFIG][FIELD_MIN_REAL_TASKS_COUNT].as<size_t>()
    );
    EXPECT_EQ(
        config.realTasksCount(),
        value[FIELD_MIXER_CONFIG][FIELD_REAL_TASKS_COUNT].as<size_t>()
    );
    EXPECT_DOUBLE_EQ(
        config.rewardPerAssignment(),
        value[FIELD_REWARD_PER_ASSIGNMENT].as<double>()
    );
}

Y_UNIT_TEST(assessors_config_test)
{
    const json::Value value = json::Value::fromFile(BinaryPath(ASSESSORS_CONFIG_PATH));

    const AssessorsConfig config(BinaryPath(ASSESSORS_CONFIG_PATH));

    EXPECT_DOUBLE_EQ(
        config.rewardPerTask(),
        value[FIELD_REWARD_PER_TASK].as<double>()
    );
    EXPECT_EQ(
        config.tasksCount(),
        value[FIELD_TASKS_COUNT].as<size_t>()
    );
    EXPECT_EQ(
        config.goldenSuitesFrequency(),
        value[FIELD_GOLDEN_SUITES_FREQUENCY].as<size_t>()
    );
    EXPECT_EQ(
        config.goldenTasksCount(),
        value[FIELD_GOLDEN_TASKS_COUNT].as<size_t>()
    );

}

Y_UNIT_TEST(yt_config_test)
{
    const json::Value value = json::Value::fromFile(BinaryPath(YT_CONFIG_PATH));

    const YTConfig config(BinaryPath(YT_CONFIG_PATH));

    EXPECT_EQ(
        config.statePath(),
        TString(value[FIELD_STATE_PATH].as<std::string>())
    );
    EXPECT_EQ(
        config.storagePath(),
        TString(value[FIELD_STORAGE_PATH].as<std::string>())
    );
}


} // Y_UNIT_TEST_SUITE(config_tests)

} // namespace tests

} // namespace maps::wiki::autocart::pipeline

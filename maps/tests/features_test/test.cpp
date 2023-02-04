#include <library/cpp/geobase/lookup.hpp>

#include <maps/analyzer/libs/calendar/include/fb/reader.h>
#include <maps/analyzer/libs/consts/include/time.h>
#include <maps/analyzer/libs/jams_level_prediction/include/features.h>
#include <maps/analyzer/libs/jams_level_prediction/include/mms_storage.h>
#include <maps/analyzer/libs/jams_level_prediction/include/model.h>
#include <maps/analyzer/libs/ml/features.h>
#include <maps/libs/common/include/temporary_dir.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <filesystem>
#include <map>
#include <fstream>
#include <string>

namespace calendar = maps::analyzer::calendar;
namespace fs = std::filesystem;
namespace ml = maps::analyzer::ml;

using namespace maps::analyzer::jams_level_prediction;

const std::string TEST_DATA_ROOT = "maps/analyzer/libs/jams_statistical_data_4/tests/data";
const auto CALENDAR_BIN_PATH = static_cast<std::string>(BinaryPath("maps/data/test/calendar/calendar.fb"));
const auto GEOBASE_BIN_PATH = static_cast<std::string>(BinaryPath("maps/data/test/geobase/geodata4.bin"));
const auto TZDATA_PATH = static_cast<std::string>(BinaryPath("maps/data/test/geobase/zones_bin"));
constexpr auto EPS = 1e-3;

const NGeobase::TLookup geobase{
    NGeobase::TLookup::TInitTraits()
        .Datafile(GEOBASE_BIN_PATH)
        .TzDataPath(TZDATA_PATH)
};

const calendar::flat_buffers::CalendarStorage cal{
    CALENDAR_BIN_PATH,
    false, // populate
    calendar::flat_buffers::CalendarStorage::ExpirationPolicy::THROW
};

ModelConfig makeConfig(
    const std::map<std::string, float>& commonFs,
    const std::map<std::string, float>& historyFs,
    const std::map<std::string, float>& geoFs
) {
    maps::json::Builder builder;
    builder << [&](maps::json::ObjectBuilder b) {
        b["normalization"] = "DEFAULT";
        b["features"] = [&](maps::json::ObjectBuilder lev1) {
            lev1["common"] = [&](maps::json::ObjectBuilder lev2) {
                lev2["numerical"] = [&](maps::json::ArrayBuilder arr) {
                    for (const auto& [fname, _]: commonFs) {
                        arr.put(fname);
                    }
                };
            };
            lev1["history"] = [&](maps::json::ObjectBuilder lev2) {
                lev2["numerical"] = [&](maps::json::ArrayBuilder arr) {
                    for (const auto& [fname, _]: historyFs) {
                        arr.put(fname);
                    }
                };
            };
            lev1["geo"] = [&](maps::json::ObjectBuilder lev2) {
                lev2["numerical"] = [&](maps::json::ArrayBuilder arr) {
                    for (const auto& [fname, _]: geoFs) {
                        arr.put(fname);
                    }
                };
            };
        };
    };
    return ModelConfig::fromString(builder.str());
}

struct HistoryEntry {
    NGeobase::TId regionId;
    time_t hourStamp;
    Level beginLevel;
    double avgLevel;
    uint64_t beginLag;
};

storage::JamsLevelHistoryStorageReader makeHistory(
    const std::vector<HistoryEntry>& history,
    const calendar::flat_buffers::CalendarStorage& calendar,
    const NGeobase::TLookup& geobase,
    const std::string tmpPath
) {
    storage::JamsLevelHistoryStorageBuilder builder;

    for (const auto& histEl: history) {
        const auto localTimeInfo = LocalTimeInfo{
            histEl.hourStamp + geobase.GetTimezoneById(histEl.regionId).Offset
        };
        builder.add(
            histEl.regionId,
            calendar::getDayType(
                calendar,
                calendar::LocalDate{localTimeInfo.daysSinceEpoch(), geobase.GetCountryId(histEl.regionId)}
            ),
            localTimeInfo.hour(),
            storage::LevelHourInfo{
                static_cast<uint64_t>(histEl.hourStamp),
                histEl.beginLevel,
                histEl.avgLevel,
                histEl.beginLag
            }
        );
    }

    builder.setVersion("test-tmp");
    storage::writeStorageToFile(tmpPath, builder);

    return storage::mapStorageFromFile(tmpPath, false, false);
};

Y_UNIT_TEST_SUITE(JamsLevelPredictionFeaturesTest) {
    Y_UNIT_TEST(TestCase1) {
        const Level currentLevel = 1;
        const time_t currentTime = 1567296060;
        const time_t predictTime = 1567299600;
        const NGeobase::TId regionId = 213;

        // use ordered map to fix features order
        std::map<std::string, float> expectedCommonVals {
            {"CUR_LEVEL",               1.0},
            {"CUR_TIMESTAMP",           1567296060.0},
            {"AHEAD",                   3540.0},
            {"AHEAD_HOUR",              1.0},
            {"PREDH_HOUR",              4.0},
            {"PREDH_DAY_OF_MONTH",      1.0},
            {"PREDH_MONTH",             9.0},
            {"PREDH_YEAR",              2019.0},
            {"PREDH_DAY_OF_YEAR",       244.0},
            {"PREDH_DAY_OF_YEAR_NORM",  245.0},
            {"PREDH_DAY_TYPE",          static_cast<float>(calendar::DayType::OTHER_HOLIDAY)},
            {"PREDH_NEXT_DAY_TYPE",     static_cast<float>(calendar::DayType::FIRST_WEEKDAY)},
            {"PREDH_PREV_DAY_TYPE",     static_cast<float>(calendar::DayType::FIRST_HOLIDAY)},
        };
        std::map<std::string, float> expectedHistoryVals {
            {"PREDH_BEG_LAST_3_AVG",    5.5},
            {"PREDH_BEG_LAST_3_MIN",    5.0},
            {"CURH_BEG_LAST_3_AVG",     3.0},
            {"CURH_BEG_LAST_3_MIN",     2.0},
            {"CURH_AVG_LAST_3_AVG",     4.0},
            {"CURH_AVG_LAST_3_MIN",     3.0},
        };
        std::map<std::string, float> expectedGeoVals {
            {"REGION_ID",               213.0},
            {"COUNTRY_ID",              225.0},
            {"REGION_LAT",              55.753},
            {"REGION_LON",              37.620},
            {"REGION_POPULATION",       11612943.0},
        };

        const auto modelConfig = makeConfig(expectedCommonVals, expectedHistoryVals, expectedGeoVals);

        maps::common::TemporaryDir tempDir{
            fs::path{
                static_cast<std::string>(BinaryPath(TEST_DATA_ROOT))
        }};
        const auto tempFile = tempDir.path() / "level_history.mms";

        const auto levelHistory = makeHistory(
            {
                { 213, 1564876800, 1, 6, 10 }, // 04 Aug 2019, 12pm
                { 213, 1565481600, 2, 5, 20 }, // 11 Aug 2019, 12pm
                { 213, 1566086400, 3, 4, 30 }, // 18 Aug 2019, 12pm
                { 213, 1566691200, 4, 3, 40 }, // 25 Aug 2019, 12pm

                { 213, 1566090000, 5, 2, 40 }, // 18 Aug 2019, 01am
                { 213, 1566694800, 6, 1, 40 }, // 25 Aug 2019, 01am
            },
            cal, geobase, tempFile
        );

        auto storage = ml::FeaturesStorage::fromConfig(modelConfig);

        ml::calcAllFeatures(
            storage, modelConfig,
            {Common::Data{
                currentLevel,
                currentTime,
                predictTime,
                regionId,
                geobase,
                cal
            }},
            {History::Data{
                currentTime,
                predictTime,
                regionId,
                levelHistory,
                geobase,
                cal
            }},
            {Geo::Data{
                regionId,
                geobase,
            }}
        );
        const auto features = storage.nums;

        ASSERT_EQ(
            features.size(),
            expectedCommonVals.size() + expectedHistoryVals.size() + expectedGeoVals.size()
        );

        std::size_t i = 0;
        for (const auto& [fname, expected]: expectedCommonVals) {
            EXPECT_NEAR(features[i], expected, EPS);
            ++i;
        }
        for (const auto& [fname, expected]: expectedHistoryVals) {
            EXPECT_NEAR(features[i], expected, EPS);
            ++i;
        }
        for (const auto& [fname, expected]: expectedGeoVals) {
            EXPECT_NEAR(features[i], expected, EPS);
            ++i;
        }
    }
}

Y_UNIT_TEST_SUITE(JamsLevelPredictionNormalizeTest) {
    Y_UNIT_TEST(TestCase1) {
        EXPECT_EQ(levelFromPrediction(-1e10), 0);
        EXPECT_EQ(levelFromPrediction(-100.), 0);
        EXPECT_EQ(levelFromPrediction(-0.5), 0);
        EXPECT_EQ(levelFromPrediction(-0.1), 0);
        EXPECT_EQ(levelFromPrediction(0.), 0);
        EXPECT_EQ(levelFromPrediction(0.1), 0);
        EXPECT_EQ(levelFromPrediction(0.9), 1);
        EXPECT_EQ(levelFromPrediction(1.), 1);
        EXPECT_EQ(levelFromPrediction(9.0), 9);
        EXPECT_EQ(levelFromPrediction(9.1), 9);
        EXPECT_EQ(levelFromPrediction(9.9), 10);
        EXPECT_EQ(levelFromPrediction(10.), 10);
        EXPECT_EQ(levelFromPrediction(10.1), 10);
        EXPECT_EQ(levelFromPrediction(10.9), 10);
        EXPECT_EQ(levelFromPrediction(11.), 10);
        EXPECT_EQ(levelFromPrediction(11.9), 10);
        EXPECT_EQ(levelFromPrediction(100.), 10);
        EXPECT_EQ(levelFromPrediction(1e10), 10);
    }
}

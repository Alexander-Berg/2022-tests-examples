#include <library/cpp/geobase/lookup.hpp>
#include <library/cpp/yson/node/node.h>
#include <library/cpp/yson/node/node_io.h>

#include <maps/analyzer/libs/calendar/include/fb/reader.h>
#include <maps/analyzer/libs/common/include/columns.h>
#include <maps/analyzer/libs/jams_level_prediction/include/mms_storage.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <util/generic/string.h>
#include <util/stream/file.h>

#include <string>
#include <fstream>

namespace calendar = maps::analyzer::calendar;
namespace columns = maps::analyzer::columns;

using namespace maps::analyzer::jams_level_prediction;

const auto CALENDAR_BIN_PATH = static_cast<std::string>(BinaryPath("maps/data/test/calendar/calendar.fb"));
const auto GEOBASE_BIN_PATH = static_cast<std::string>(BinaryPath("maps/data/test/geobase/geodata4.bin"));
const auto TZDATA_PATH = static_cast<std::string>(BinaryPath("maps/data/test/geobase/zones_bin"));

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

const std::string TEST_DATA_ROOT = "maps/analyzer/libs/jams_level_prediction/tests/data/";
const std::string VERSION = "test";
const std::string STORAGE_PATH = BinaryPath(TEST_DATA_ROOT + "storage.mms");
const std::string TABLE_PATH = BinaryPath(TEST_DATA_ROOT + "table.yson");

constexpr auto EPS = 1e-3;

bool operator==(const storage::LevelHourInfo& lhs, const storage::LevelHourInfo& rhs) {
    return lhs.hourStamp == rhs.hourStamp && lhs.beginLevel == rhs.beginLevel &&
        lhs.beginLag == rhs.beginLag && std::abs(lhs.avgLevel - rhs.avgLevel) < EPS;
}

bool operator!=(const storage::LevelHourInfo& lhs, const storage::LevelHourInfo& rhs) {
    return !(lhs == rhs);
}

template<typename F>
void forEachRow(const NYT::TNode::TListType& rows, F f) {
    for(const auto& row: rows) {
        const auto regionId = row[columns::REGION_ID].AsInt64();
        const auto hourStamp = row["hour_stamp"].AsUint64();
        const auto beginLevel = row["begin_level"].AsUint64();
        const auto avgLevel = row["avg_level"].AsDouble();
        const auto beginLag = row["begin_level_lag"].AsUint64();

        const auto localTimeInfo = LocalTimeInfo{static_cast<time_t>(hourStamp + geobase.GetTimezoneById(regionId).Offset)};
        const auto dayType = calendar::getDayType(
            cal,
            calendar::LocalDate{localTimeInfo.daysSinceEpoch(), geobase.GetCountryId(regionId)}
        );
        f(regionId, hourStamp, beginLevel, avgLevel, beginLag, localTimeInfo, dayType);
    }
}

Y_UNIT_TEST_SUITE(JamsLevelHistoryStorageTest) {
    Y_UNIT_TEST(BuildTest) {
        TFileInput inputTable(TABLE_PATH.c_str());
        const auto rows = NYT::NodeFromYsonStream(&inputTable, ::NYson::EYsonType::ListFragment).AsList();

        storage::JamsLevelHistoryStorageBuilder builder;
        builder.setVersion(VERSION);

        forEachRow(rows, [&](
                auto regionId, auto hourStamp, auto beginLevel, auto avgLevel,
                auto beginLag, auto localTimeInfo, auto dayType
            ) {
                builder.add(
                    static_cast<NGeobase::TId>(regionId),
                    dayType, localTimeInfo.hour(),
                    storage::LevelHourInfo{
                        hourStamp,
                        static_cast<uint8_t>(beginLevel),
                        avgLevel,
                        beginLag
                    }
                );
        });

        storage::writeStorageToFile(STORAGE_PATH, builder);

        const auto storage = storage::mapStorageFromFile(
            STORAGE_PATH,
            false, // populate
            false // mlock
        );

        ASSERT_STREQ(storage->version(), VERSION);

        forEachRow(rows, [&](
                auto regionId, auto hourStamp, auto beginLevel, auto avgLevel,
                auto beginLag, auto localTimeInfo, auto dayType
            ) {
                const auto history = storage->get(
                    static_cast<NGeobase::TId>(regionId),
                    dayType, localTimeInfo.hour()
                );
                ASSERT(history);

                const storage::LevelHourInfo expected{
                    hourStamp,
                    static_cast<uint8_t>(beginLevel),
                    avgLevel,
                    beginLag
                };

                {
                    const auto [begin, end] = history->lastValuesAt(1, hourStamp + 1);
                    ASSERT_EQ(std::distance(begin, end), 1);
                    ASSERT(*begin == expected);
                }
                {
                    const auto [begin, end] = history->lastValuesAt(1, hourStamp);
                    for (auto it = begin; it != end; ++it) {
                        ASSERT(*it != expected);
                    }
                }
        });
    }
}

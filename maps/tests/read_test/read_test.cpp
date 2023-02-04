#include <library/cpp/geobase/lookup.hpp>
#include <maps/analyzer/libs/calendar/include/fb/reader.h>
#include <maps/analyzer/libs/calendar/include/calendar.h>
#include <maps/analyzer/libs/consts/include/time.h>
#include <maps/libs/chrono/include/time_point.h>
#include <maps/libs/geolib/include/point.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <util/generic/string.h>
#include <util/stream/file.h>

#include <string>
#include <fstream>

namespace calendar = maps::analyzer::calendar;

uint32_t getDaysSinceEpoch(const std::string& str) {
    return maps::chrono::sinceEpoch<std::chrono::seconds>(
        maps::chrono::parseIsoDate(str)
    ) / maps::analyzer::consts::SECONDS_PER_DAY; // change to `std::chrono::days` in C++20
}

const auto GEOBASE_BIN_PATH = static_cast<std::string>(BinaryPath("maps/data/test/geobase/geodata4.bin"));
const auto TZDATA_PATH = static_cast<std::string>(BinaryPath("maps/data/test/geobase/zones_bin"));

const NGeobase::TLookup geobase(
    NGeobase::TLookup::TInitTraits()
        .Datafile(GEOBASE_BIN_PATH)
        .TzDataPath(TZDATA_PATH)
);

const std::string TEST_DATA_ROOT = "maps/analyzer/libs/calendar/tests/data/";
const std::string VERSION = "test";

const auto cal = calendar::loadCalendar(BinaryPath(TEST_DATA_ROOT + "calendar.fb"));

Y_UNIT_TEST_SUITE(CalendarTest) {
    Y_UNIT_TEST(ReadFlatbufferVersion) {
        EXPECT_EQ(cal.version(), VERSION);
    }

    Y_UNIT_TEST(ReadFlatbufferHoliday) {
        const auto info = cal.getDateInfo({getDaysSinceEpoch("2019-01-02"), static_cast<NGeobase::TId>(225)});
        EXPECT_TRUE(info.type == calendar::DateInfo::DateType::Holiday);
        EXPECT_EQ(info.isHoliday, true);
        EXPECT_EQ(info.isTransfer, false);
        EXPECT_TRUE(info.holidayName);
        EXPECT_EQ(*info.holidayName, "Новогодние каникулы");
    }

    Y_UNIT_TEST(ReadFlatbufferWeekend) {
        const auto info = cal.getDateInfo({getDaysSinceEpoch("2019-01-19"), static_cast<NGeobase::TId>(225)});
        EXPECT_TRUE(info.type == calendar::DateInfo::DateType::Weekend);
        EXPECT_EQ(info.isHoliday, true);
        EXPECT_EQ(info.isTransfer, false);
        EXPECT_FALSE(info.holidayName);
    }

    Y_UNIT_TEST(ReadFlatbufferWeekday) {
        const auto info = cal.getDateInfo({getDaysSinceEpoch("2019-01-02"), static_cast<NGeobase::TId>(149)});
        EXPECT_TRUE(info.type == calendar::DateInfo::DateType::Weekday);
        EXPECT_EQ(info.isHoliday, false);
        EXPECT_EQ(info.isTransfer, false);
        EXPECT_FALSE(info.holidayName);
    }

    Y_UNIT_TEST(FallbackWeekday) {
        const auto info = cal.getDateInfo({getDaysSinceEpoch("2020-01-02"), static_cast<NGeobase::TId>(225)});
        EXPECT_TRUE(info.type == calendar::DateInfo::DateType::Weekday);
        EXPECT_EQ(info.isHoliday, false);
        EXPECT_EQ(info.isTransfer, false);
        EXPECT_FALSE(info.holidayName);
    }

    Y_UNIT_TEST(FallbackWeekend) {
        const auto info = cal.getDateInfo({getDaysSinceEpoch("2020-01-25"), static_cast<NGeobase::TId>(225)});
        EXPECT_TRUE(info.type == calendar::DateInfo::DateType::Weekend);
        EXPECT_EQ(info.isHoliday, true);
        EXPECT_EQ(info.isTransfer, false);
        EXPECT_FALSE(info.holidayName);
    }

    Y_UNIT_TEST(CheckExpired) {
        const auto calThrow = calendar::flat_buffers::CalendarStorage{
            BinaryPath(TEST_DATA_ROOT + "calendar.fb"),
            false, // populate
            calendar::flat_buffers::CalendarStorage::ExpirationPolicy::THROW
        };
        EXPECT_THROW(
            calThrow.getDateInfo(
                {getDaysSinceEpoch("2020-01-25"), static_cast<NGeobase::TId>(225)}
            ),
            maps::Exception
        );
    }

    Y_UNIT_TEST(GetByTimestampAndPosition) {
        const auto localDate = calendar::getLocalDate(
            1546988400, // 2019-01-08T23:00:00 UTC (2019-01-08 in Moscow)
            maps::geolib3::Point2{37.6, 55.7}, // Moscow
            geobase
        );

        {
            const auto info = cal.getDateInfo(localDate);
            EXPECT_TRUE(info.type == calendar::DateInfo::DateType::Weekday);
            EXPECT_EQ(info.isHoliday, false);
            EXPECT_EQ(info.isTransfer, false);
            EXPECT_FALSE(info.holidayName);
        }
        {
            const auto info = cal.getDateInfo(localDate.prev());
            EXPECT_TRUE(info.type == calendar::DateInfo::DateType::Holiday);
            EXPECT_EQ(info.isHoliday, true);
            EXPECT_EQ(info.isTransfer, false);
            EXPECT_TRUE(info.holidayName);
            EXPECT_EQ(*info.holidayName, "Новогодние каникулы");
        }
        {
            const auto info = cal.getDateInfo(localDate.next().next().next());
            EXPECT_TRUE(info.type == calendar::DateInfo::DateType::Weekend);
            EXPECT_EQ(info.isHoliday, true);
            EXPECT_EQ(info.isTransfer, false);
            EXPECT_TRUE(info.holidayName);
            EXPECT_EQ(*info.holidayName, "День работника прокуратуры РФ");
        }
    }
}

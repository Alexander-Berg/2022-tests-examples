#include <yandex/maps/wiki/common/schedule.h>
#include <maps/libs/common/include/exception.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <util/stream/output.h>

#include <sstream>

template <>
void Out<maps::wiki::common::Schedule>( // NOLINT
    IOutputStream& os,
    const maps::wiki::common::Schedule& schedule)
{
    std::ostringstream ss;
    ss << schedule;
    os << ss.str();
}

namespace maps::wiki::common::tests {

Y_UNIT_TEST_SUITE(schedules) {

Y_UNIT_TEST(test_schedules_frequency_intersection)
{
    auto schedule1 = Schedule(
        "1010", "1212", WeekdayFlags::Workdays,
        "1000", "2000", 0);
    auto schedule2 = Schedule(
        "", "", WeekdayFlags::Tuesday | WeekdayFlags::Holidays,
        "0900", "1800", 15);
    auto schedule3 = Schedule(
        "0301", "1103", WeekdayFlags::Monday,
        "0000", "2359", 0);
    auto schedule4 = Schedule(
        "0301", "1103", WeekdayFlags::Tuesday,
        "0000", "2359", 0);
    auto schedule5 = Schedule(
        "1104", "0228", WeekdayFlags::Workdays,
        "0000", "2359", 0);
    auto schedule6 = Schedule(
        "0101", "0102", WeekdayFlags::Workdays,
        "", "", 0);

    UNIT_ASSERT(schedule1.intersects(schedule2));
    UNIT_ASSERT(!schedule3.intersects(schedule4));
    UNIT_ASSERT(!schedule5.intersects(schedule4));

    for (const auto& days : schedule2.dayRanges()) {
        UNIT_ASSERT(days.first == 1 && days.second == 366);
    }

    UNIT_ASSERT(!schedule6.startTime());
    UNIT_ASSERT(!schedule6.endTime());
    UNIT_ASSERT_VALUES_EQUAL(schedule6.frequency(), 0);
}

Y_UNIT_TEST(test_schedules_overnight_intersection)
{
    auto schedule1 = Schedule(
        "0101", "0110", WeekdayFlags::Saturday | WeekdayFlags::Sunday,
        "2200", "0200", 5);

    auto schedule2 = Schedule(
        "0101", "0110", WeekdayFlags::Monday | WeekdayFlags::Tuesday,
        "0100", "0300", 5);
    auto schedule3 = Schedule(
        "0101", "0110", WeekdayFlags::Tuesday,
        "0100", "0300", 5);
    auto schedule4 = Schedule(
        "0111", "0131", WeekdayFlags::Monday | WeekdayFlags::Tuesday,
        "0100", "0300", 10);
    auto schedule5 = Schedule(
        "0111", "0131", WeekdayFlags::Tuesday,
        "0100", "0300", 10);

    auto schedule6 = Schedule(
        "0101", "0110", WeekdayFlags::Monday | WeekdayFlags::Tuesday,
        {"0130"});
    auto schedule7 = Schedule(
        "0101", "0110", WeekdayFlags::Tuesday,
        {"0130"});
    auto schedule8 = Schedule(
        "0111", "0131", WeekdayFlags::Monday | WeekdayFlags::Tuesday,
        {"0130"});
    auto schedule9 = Schedule(
        "0111", "0131", WeekdayFlags::Tuesday,
        {"0130"});

    auto schedule10 = Schedule(
        "0101", "0110", WeekdayFlags::Monday | WeekdayFlags::Tuesday,
        "0200", "0300", 5);
    auto schedule11 = Schedule(
        "0101", "0110", WeekdayFlags::Monday | WeekdayFlags::Tuesday,
        "0300", "2300", 5);

    UNIT_ASSERT(schedule1.intersects(schedule2));
    UNIT_ASSERT(!schedule1.intersects(schedule3));
    UNIT_ASSERT(schedule1.intersects(schedule4));
    UNIT_ASSERT(!schedule1.intersects(schedule5));

    UNIT_ASSERT(schedule1.intersects(schedule6));
    UNIT_ASSERT(!schedule1.intersects(schedule7));
    UNIT_ASSERT(schedule1.intersects(schedule8));
    UNIT_ASSERT(!schedule1.intersects(schedule9));

    UNIT_ASSERT(!schedule1.intersects(schedule10));
    UNIT_ASSERT(!schedule1.intersects(schedule11));

    auto schedule12 = Schedule(
        "0101", "1231", WeekdayFlags::Monday,
        "2200", "0200", 5);
    auto schedule13 = Schedule(
        "0101", "1231", WeekdayFlags::Tuesday,
        "2200", "0200", 5);

    UNIT_ASSERT(!schedule12.intersects(schedule13));

    auto schedule14 = Schedule(
        "0101", "1231", WeekdayFlags::Monday,
        "2200", "0200", 5);
    auto schedule15 = Schedule(
        "0101", "1231", WeekdayFlags::Wednesday,
        "0100", "0300", 5);

    UNIT_ASSERT(!schedule14.intersects(schedule15));
}

Y_UNIT_TEST(test_schedules_point_intersection)
{
    auto schedule1 = Schedule(
        "1010", "1212", WeekdayFlags::Workdays,
        "1001", "1017", 1);
    auto schedule2 = Schedule(
        "1010", "1212", WeekdayFlags::Workdays,
        "1017", "1030", 1);

    UNIT_ASSERT(!schedule1.intersects(schedule2));

    // Overnight
    auto schedule3 = Schedule(
        "1010", "1212", WeekdayFlags::Workdays,
        "2355", "0017", 5);
    auto schedule4 = Schedule(
        "1010", "1212", WeekdayFlags::Workdays,
        "0017", "0030", 1);
    UNIT_ASSERT(!schedule3.intersects(schedule4));
}

Y_UNIT_TEST(test_schedules_union_non_interval)
{
    // Itersection by dates, but not by weekdays
    {
        Schedule schedule1("0101", "0101", WeekdayFlags::Monday, {"0500"});
        Schedule schedule2("0102", "1231", WeekdayFlags::Tuesday, {"0600"});

        auto result = unionSchedules({schedule1}, {schedule2});
        UNIT_ASSERT_VALUES_EQUAL(result.size(), 2);

        UNIT_ASSERT_VALUES_EQUAL(result[0], schedule1);
        UNIT_ASSERT_VALUES_EQUAL(result[1], schedule2);
    }
    // Itersection by weekdays, but not by dates
    {
        Schedule schedule1(
            "0101", "0501", WeekdayFlags::Monday | WeekdayFlags::Tuesday,
            {"0500"});
        Schedule schedule2(
            "0301", "0701", WeekdayFlags::Wednesday | WeekdayFlags::Thursday,
            {"0600"});

        auto result = unionSchedules({schedule1}, {schedule2});
        UNIT_ASSERT_VALUES_EQUAL(result.size(), 2);

        UNIT_ASSERT_VALUES_EQUAL(result[0], schedule1);
        UNIT_ASSERT_VALUES_EQUAL(result[1], schedule2);
    }
    // 1-day intersection
    {
        Schedule schedule1("0101", "0101", WeekdayFlags::Monday, {"0500"});
        Schedule schedule2("0101", "1231", WeekdayFlags::Monday, {"0600"});

        auto result = unionSchedules({schedule1}, {schedule2});
        UNIT_ASSERT_VALUES_EQUAL(result.size(), 2);

        UNIT_ASSERT_VALUES_EQUAL(result[0],
            Schedule("0101", "0101", WeekdayFlags::Monday, {"0500", "0600"}));
        UNIT_ASSERT_VALUES_EQUAL(result[1],
            Schedule("0102", "1231", WeekdayFlags::Monday, {"0600"}));
    }
    // First schedule is for later dates than second
    {
        Schedule schedule1("0201", "0401", WeekdayFlags::Monday, {"0500"});
        Schedule schedule2("0101", "0301", WeekdayFlags::Monday, {"0600"});

        auto result = unionSchedules({schedule1}, {schedule2});
        UNIT_ASSERT_VALUES_EQUAL(result.size(), 3);

        UNIT_ASSERT_VALUES_EQUAL(result[0],
            Schedule("0101", "0131", WeekdayFlags::Monday, {"0600"}));
        UNIT_ASSERT_VALUES_EQUAL(result[1],
            Schedule("0201", "0301", WeekdayFlags::Monday, {"0500", "0600"}));
        UNIT_ASSERT_VALUES_EQUAL(result[2],
            Schedule("0302", "0401", WeekdayFlags::Monday, {"0500"}));
    }
    // New Year in one of schedules
    {
        Schedule schedule1("0201", "0101", WeekdayFlags::Monday, {"0500"});
        Schedule schedule2("0102", "1231", WeekdayFlags::Monday, {"0600"});

        auto result = unionSchedules({schedule1}, {schedule2});
        UNIT_ASSERT_VALUES_EQUAL(result.size(), 3);

        UNIT_ASSERT_VALUES_EQUAL(result[0],
            Schedule("0101", "0101", WeekdayFlags::Monday, {"0500"}));
        UNIT_ASSERT_VALUES_EQUAL(result[1],
            Schedule("0102", "0131", WeekdayFlags::Monday, {"0600"}));
        UNIT_ASSERT_VALUES_EQUAL(result[2],
            Schedule("0201", "1231", WeekdayFlags::Monday, {"0500", "0600"}));
    }
    // New Year in both of schedules (symmetric)
    {
        Schedule schedule1("1101", "0301", WeekdayFlags::Monday, {"0500"});
        Schedule schedule2("1201", "0201", WeekdayFlags::Monday, {"0600"});

        auto result = unionSchedules({schedule1}, {schedule2});
        UNIT_ASSERT_VALUES_EQUAL(result.size(), 4);

        UNIT_ASSERT_VALUES_EQUAL(result[0],
            Schedule("0101", "0201", WeekdayFlags::Monday, {"0500", "0600"}));
        UNIT_ASSERT_VALUES_EQUAL(result[1],
            Schedule("0202", "0301", WeekdayFlags::Monday, {"0500"}));
        UNIT_ASSERT_VALUES_EQUAL(result[2],
            Schedule("1101", "1130", WeekdayFlags::Monday, {"0500"}));
        UNIT_ASSERT_VALUES_EQUAL(result[3],
            Schedule("1201", "1231", WeekdayFlags::Monday, {"0500", "0600"}));
    }
    // New Year in both of schedules (non-symmetric)
    {
        Schedule schedule1("1101", "0201", WeekdayFlags::Monday, {"0500"});
        Schedule schedule2("1201", "0301", WeekdayFlags::Monday, {"0600"});

        auto result = unionSchedules({schedule1}, {schedule2});
        UNIT_ASSERT_VALUES_EQUAL(result.size(), 4);

        UNIT_ASSERT_VALUES_EQUAL(result[0],
            Schedule("0101", "0201", WeekdayFlags::Monday, {"0500", "0600"}));
        UNIT_ASSERT_VALUES_EQUAL(result[1],
            Schedule("0202", "0301", WeekdayFlags::Monday, {"0600"}));
        UNIT_ASSERT_VALUES_EQUAL(result[2],
            Schedule("1101", "1130", WeekdayFlags::Monday, {"0500"}));
        UNIT_ASSERT_VALUES_EQUAL(result[3],
            Schedule("1201", "1231", WeekdayFlags::Monday, {"0500", "0600"}));
    }
    // Complex example for single schedules
    {
        Schedule schedule1(
            "0201", "0731",
            WeekdayFlags::Monday | WeekdayFlags::Wednesday | WeekdayFlags::Friday,
            {"0100", "0200", "0300"});
        Schedule schedule2(
            "0531", "1130",
            WeekdayFlags::Monday | WeekdayFlags::Friday | WeekdayFlags::Sunday,
            {"0300", "0400", "0500"});

        auto result = unionSchedules({schedule1}, {schedule2});
        UNIT_ASSERT_VALUES_EQUAL(result.size(), 5);

        UNIT_ASSERT_VALUES_EQUAL(result[0], Schedule(
            "0201", "0530",
            WeekdayFlags::Monday | WeekdayFlags::Wednesday | WeekdayFlags::Friday,
            {"0100", "0200", "0300"}));
        UNIT_ASSERT_VALUES_EQUAL(result[1], Schedule(
            "0531", "0731",
            WeekdayFlags::Wednesday,
            {"0100", "0200", "0300"}));
        UNIT_ASSERT_VALUES_EQUAL(result[2], Schedule(
            "0531", "0731",
            WeekdayFlags::Monday | WeekdayFlags::Friday,
            {"0100", "0200", "0300", "0400", "0500"}));
        UNIT_ASSERT_VALUES_EQUAL(result[3], Schedule(
            "0531", "0731",
            WeekdayFlags::Sunday,
            {"0300", "0400", "0500"}));
        UNIT_ASSERT_VALUES_EQUAL(result[4], Schedule(
            "0801", "1130",
            WeekdayFlags::Monday | WeekdayFlags::Friday | WeekdayFlags::Sunday,
            {"0300", "0400", "0500"}));
    }
    // Schedule vectors union
    {
        Schedule schedule1("0201", "0229", WeekdayFlags::Workdays, {"0500"});
        Schedule schedule2("0401", "0630", WeekdayFlags::Workdays, {"0500"});
        Schedule schedule3("0801", "0831", WeekdayFlags::Workdays, {"0500"});
        Schedule schedule4("0101", "0430", WeekdayFlags::Workdays, {"0600"});
        Schedule schedule5("0601", "0831", WeekdayFlags::Friday | WeekdayFlags::Holidays, {"0600"});

        auto result = unionSchedules(
            {schedule1, schedule2, schedule3},
            {schedule4, schedule5});
        UNIT_ASSERT_VALUES_EQUAL(result.size(), 12);

        UNIT_ASSERT_VALUES_EQUAL(result[0],
            Schedule("0101", "0131", WeekdayFlags::Workdays, {"0600"}));
        UNIT_ASSERT_VALUES_EQUAL(result[1],
            Schedule("0201", "0229", WeekdayFlags::Workdays, {"0500", "0600"}));
        UNIT_ASSERT_VALUES_EQUAL(result[2],
            Schedule("0301", "0331", WeekdayFlags::Workdays, {"0600"}));
        UNIT_ASSERT_VALUES_EQUAL(result[3],
            Schedule("0401", "0430", WeekdayFlags::Workdays, {"0500", "0600"}));
        UNIT_ASSERT_VALUES_EQUAL(result[4],
            Schedule("0501", "0531", WeekdayFlags::Workdays, {"0500"}));
        UNIT_ASSERT_VALUES_EQUAL(result[5], Schedule(
                "0601", "0630",
                WeekdayFlags::Monday | WeekdayFlags::Tuesday |
                WeekdayFlags::Wednesday | WeekdayFlags::Thursday,
                {"0500"}));
        UNIT_ASSERT_VALUES_EQUAL(result[6],
            Schedule("0601", "0630", WeekdayFlags::Friday, {"0500", "0600"}));
        UNIT_ASSERT_VALUES_EQUAL(result[7],
            Schedule("0601", "0630", WeekdayFlags::Holidays, {"0600"}));
        UNIT_ASSERT_VALUES_EQUAL(result[8],
            Schedule("0701", "0731", WeekdayFlags::Friday | WeekdayFlags::Holidays, {"0600"}));
        UNIT_ASSERT_VALUES_EQUAL(result[9], Schedule(
                "0801", "0831",
                WeekdayFlags::Monday | WeekdayFlags::Tuesday |
                WeekdayFlags::Wednesday | WeekdayFlags::Thursday,
                {"0500"}));
        UNIT_ASSERT_VALUES_EQUAL(result[10],
            Schedule("0801", "0831", WeekdayFlags::Friday, {"0500", "0600"}));
        UNIT_ASSERT_VALUES_EQUAL(result[11],
            Schedule("0801", "0831", WeekdayFlags::Holidays, {"0600"}));
    }
    // Sequential union: https://n.maps.yandex.ru/#!/objects/2037273732
    {
        Schedule schedule1("0101", "1231", WeekdayFlags::Workdays | WeekdayFlags::Saturday, {"0855"});
        Schedule schedule2("0102", "0108", WeekdayFlags::All, {"1555"});
        Schedule schedule3("0102", "0229", WeekdayFlags::Workdays, {"0455", "0755"});
        Schedule schedule4("0109", "1231", WeekdayFlags::Friday | WeekdayFlags::Sunday, {"1555"});

        auto result = unionSchedules({schedule1}, {schedule2});
        result = unionSchedules(result, {schedule3});
        result = unionSchedules(result, {schedule4});

        UNIT_ASSERT_VALUES_EQUAL(result.size(), 11);

        UNIT_ASSERT_VALUES_EQUAL(result[0],
            Schedule("0101", "0101", WeekdayFlags::Workdays | WeekdayFlags::Saturday, {"0855"}));
        UNIT_ASSERT_VALUES_EQUAL(result[1],
            Schedule("0102", "0108", WeekdayFlags::Workdays, {"0455", "0755", "0855", "1555"}));
        UNIT_ASSERT_VALUES_EQUAL(result[2],
            Schedule("0102", "0108", WeekdayFlags::Saturday, {"0855", "1555"}));
        UNIT_ASSERT_VALUES_EQUAL(result[3],
            Schedule("0102", "0108", WeekdayFlags::Sunday, {"1555"}));
        UNIT_ASSERT_VALUES_EQUAL(result[4],
            Schedule(
                "0109", "0229",
                WeekdayFlags::Monday | WeekdayFlags::Tuesday |
                WeekdayFlags::Wednesday | WeekdayFlags::Thursday,
                {"0455", "0755", "0855"}));
        UNIT_ASSERT_VALUES_EQUAL(result[5],
            Schedule("0109", "0229", WeekdayFlags::Friday, {"0455", "0755", "0855", "1555"}));
        UNIT_ASSERT_VALUES_EQUAL(result[6],
            Schedule("0109", "0229", WeekdayFlags::Saturday, {"0855"}));
        UNIT_ASSERT_VALUES_EQUAL(result[7],
            Schedule("0109", "0229", WeekdayFlags::Sunday, {"1555"}));
        UNIT_ASSERT_VALUES_EQUAL(result[8],
            Schedule("0301", "1231", WeekdayFlags::Friday, {"0855", "1555"}));
        UNIT_ASSERT_VALUES_EQUAL(result[9],
            Schedule(
                "0301", "1231",
                WeekdayFlags::Monday | WeekdayFlags::Tuesday | WeekdayFlags::Wednesday |
                WeekdayFlags::Thursday | WeekdayFlags::Saturday,
                {"0855"}));
        UNIT_ASSERT_VALUES_EQUAL(result[10],
            Schedule("0301", "1231", WeekdayFlags::Sunday, {"1555"}));
    }
    // Identical replace
    {
        Schedule schedule1("0201", "0229", WeekdayFlags::Workdays, {"0500"});
        Schedule schedule2("0201", "0229", WeekdayFlags::Workdays, {"0600"});

        auto result = replaceSchedules({schedule1}, {schedule2});
        UNIT_ASSERT_VALUES_EQUAL(result.size(), 1);

        UNIT_ASSERT_VALUES_EQUAL(result[0], schedule2);
    }
    // Non-intersecting replace
    {
        Schedule schedule1("0201", "0229", WeekdayFlags::Workdays, {"0500"});
        Schedule schedule2("0201", "0229", WeekdayFlags::Holidays, {"0600"});
        Schedule schedule3("0401", "0630", WeekdayFlags::All, {"0700"});

        auto result = replaceSchedules(
            {schedule1, schedule2},
            {schedule3});
        UNIT_ASSERT_VALUES_EQUAL(result.size(), 3);

        UNIT_ASSERT_VALUES_EQUAL(result[0], schedule1);
        UNIT_ASSERT_VALUES_EQUAL(result[1], schedule2);
        UNIT_ASSERT_VALUES_EQUAL(result[2], schedule3);
    }
    // Schedule vectors replace
    {
        Schedule schedule1("0201", "0229", WeekdayFlags::Workdays, {"0500"});
        Schedule schedule2("0401", "0630", WeekdayFlags::Workdays, {"0500"});
        Schedule schedule3("0801", "0831", WeekdayFlags::Workdays, {"0500"});
        Schedule schedule4("0201", "0229", WeekdayFlags::Workdays, {"0600"});
        Schedule schedule5("0601", "0831", WeekdayFlags::Friday | WeekdayFlags::Holidays, {"0600"});

        auto result = replaceSchedules(
            {schedule1, schedule2, schedule3},
            {schedule4, schedule5});
        UNIT_ASSERT_VALUES_EQUAL(result.size(), 3);

        UNIT_ASSERT_VALUES_EQUAL(result[0], schedule4);
        UNIT_ASSERT_VALUES_EQUAL(result[1],
            Schedule("0401", "0531", WeekdayFlags::Workdays, {"0500"}));
        UNIT_ASSERT_VALUES_EQUAL(result[2], schedule5);
    }
}

Y_UNIT_TEST(test_schedules_union_interval)
{
    // Un-unionable schedules
    {
        Schedule schedule1("0101", "0201", WeekdayFlags::All, "0000", "0400", 30);
        Schedule schedule2("0101", "0201", WeekdayFlags::All, "0300", "0500", 20);
        Schedule schedule3("0101", "0201", WeekdayFlags::All, "0500", "0600", 10);
        Schedule schedule4("0201", "0301", WeekdayFlags::All, {"0200"});

        UNIT_ASSERT_EXCEPTION(
            unionSchedules({schedule1}, {schedule2}),
            maps::RuntimeError);
        UNIT_ASSERT_EXCEPTION(
            unionSchedules({schedule1}, {schedule4}),
            maps::RuntimeError);

        UNIT_ASSERT_NO_EXCEPTION(
            unionSchedules({schedule2}, {schedule3}));
        UNIT_ASSERT_NO_EXCEPTION(
            unionSchedules({schedule2}, {schedule4}));
    }
    // Interval schedules union
    {
        Schedule schedule1("0101", "0229", WeekdayFlags::Monday | WeekdayFlags::Tuesday, "0600", "0900", 10);
        Schedule schedule2("0201", "0331", WeekdayFlags::Tuesday | WeekdayFlags::Wednesday, "1200", "1500", 20);

        auto result = unionSchedules({schedule1}, {schedule2});
        UNIT_ASSERT_VALUES_EQUAL(result.size(), 2);

        UNIT_ASSERT_VALUES_EQUAL(result[0], schedule1);
        UNIT_ASSERT_VALUES_EQUAL(result[1], schedule2);
    }
    // Interval schedules replace
    {
        Schedule schedule1("0101", "0229", WeekdayFlags::Monday | WeekdayFlags::Tuesday, "0600", "0900", 10);
        Schedule schedule2("0201", "0331", WeekdayFlags::Tuesday | WeekdayFlags::Wednesday, "1200", "1500", 20);

        auto result = replaceSchedules({schedule1}, {schedule2});
        UNIT_ASSERT_VALUES_EQUAL(result.size(), 2);

        UNIT_ASSERT_VALUES_EQUAL(result[0],
            Schedule("0101", "0131", WeekdayFlags::Monday | WeekdayFlags::Tuesday, "0600", "0900", 10));
        UNIT_ASSERT_VALUES_EQUAL(result[1], schedule2);
    }
    // Interval and non-interval schedules union
    {
        Schedule schedule1("0101", "0229", WeekdayFlags::Monday | WeekdayFlags::Tuesday, "0600", "0900", 10);
        Schedule schedule2("0201", "0331", WeekdayFlags::Tuesday | WeekdayFlags::Wednesday, {"1200", "1500"});

        auto result = unionSchedules({schedule1}, {schedule2});
        UNIT_ASSERT_VALUES_EQUAL(result.size(), 2);

        UNIT_ASSERT_VALUES_EQUAL(result[0], schedule1);
        UNIT_ASSERT_VALUES_EQUAL(result[1], schedule2);
    }
    // Replace interval schedule by non-interval
    {
        Schedule schedule1("0101", "0229", WeekdayFlags::Monday | WeekdayFlags::Tuesday, "0600", "0900", 10);
        Schedule schedule2("0201", "0331", WeekdayFlags::Tuesday | WeekdayFlags::Wednesday, {"1200", "1500"});

        auto result = replaceSchedules({schedule1}, {schedule2});
        UNIT_ASSERT_VALUES_EQUAL(result.size(), 2);

        UNIT_ASSERT_VALUES_EQUAL(result[0],
            Schedule("0101", "0131", WeekdayFlags::Monday | WeekdayFlags::Tuesday, "0600", "0900", 10));
        UNIT_ASSERT_VALUES_EQUAL(result[1], schedule2);
    }
    // Replace non-interval schedule by interval
    {
        Schedule schedule1("0101", "0229", WeekdayFlags::Monday | WeekdayFlags::Tuesday, {"1200", "1500"});
        Schedule schedule2("0201", "0331", WeekdayFlags::Tuesday | WeekdayFlags::Wednesday, "0600", "0900", 10);

        auto result = replaceSchedules({schedule1}, {schedule2});
        UNIT_ASSERT_VALUES_EQUAL(result.size(), 2);

        UNIT_ASSERT_VALUES_EQUAL(result[0],
            Schedule("0101", "0131", WeekdayFlags::Monday | WeekdayFlags::Tuesday, {"1200", "1500"}));
        UNIT_ASSERT_VALUES_EQUAL(result[1], schedule2);
    }
    // Sequential union, no real merge needed: https://n.maps.yandex.ru/#!/objects/4436726020
    {
        Schedules schedules = {
            Schedule("0301", "1130", WeekdayFlags::Workdays, "0640", "2030", 540),
            Schedule("0301", "1130", WeekdayFlags::Saturday, "0650", "2000", 540),
            Schedule("0301", "1130", WeekdayFlags::Saturday, "2000", "2030", 900),
            Schedule("0301", "1130", WeekdayFlags::Workdays | WeekdayFlags::Saturday, {"2100"}),
            Schedule("0301", "1130", WeekdayFlags::Sunday, "0650", "1900", 600),
            Schedule("0301", "1130", WeekdayFlags::Sunday, "1900", "2015", 900),
            Schedule("0601", "0831", WeekdayFlags::Sunday, "2130", "2200", 1800),
            Schedule("1201", "0229", WeekdayFlags::Workdays, {"1920", "1940", "2000", "2030", "2100"}),
            Schedule("1201", "0229", WeekdayFlags::Saturday, {"1820", "1850", "1920", "1940", "2005"}),
            Schedule("1201", "0229", WeekdayFlags::Sunday, {"1825", "1850", "1915", "1940", "2005"})
        };

        Schedules result;
        for (const auto& schedule : schedules) {
            result = unionSchedules(result, {schedule});
        }

        UNIT_ASSERT_VALUES_EQUAL(result.size(), schedules.size());
        for (size_t i = 0; i < schedules.size(); ++i) {
            UNIT_ASSERT_VALUES_EQUAL(result[i], schedules[i]);
        }
    }
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::common::tests

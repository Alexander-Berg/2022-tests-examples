#include <yandex/maps/wiki/common/date_time.h>
#include <maps/libs/common/include/exception.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <util/stream/output.h>

#include <sstream>
#include <string>

template <>
void Out<maps::wiki::common::WeekdayFlags>( // NOLINT
    IOutputStream& os,
    maps::wiki::common::WeekdayFlags weekdays)
{
    os << static_cast<int>(weekdays);
}

template <>
void Out<maps::wiki::common::Date>( // NOLINT
    IOutputStream& os,
    const maps::wiki::common::Date& date)
{
    std::ostringstream ss;
    ss << date;
    os << ss.str();
}

template <>
void Out<maps::wiki::common::Time>( // NOLINT
    IOutputStream& os,
    const maps::wiki::common::Time& time)
{
    std::ostringstream ss;
    ss << time;
    os << ss.str();
}

namespace maps::wiki::common::tests {

Y_UNIT_TEST_SUITE(date_time) {

Y_UNIT_TEST(test_valid_date)
{
    UNIT_ASSERT(!Date("0001").isValid());
    UNIT_ASSERT(!Date("0100").isValid());
    UNIT_ASSERT(Date("0101").isValid());
    UNIT_ASSERT(Date("1231").isValid());
    UNIT_ASSERT(!Date("1232").isValid());
    UNIT_ASSERT(!Date("3112").isValid());
}

Y_UNIT_TEST(test_add_days_to_date)
{
    UNIT_ASSERT_VALUES_EQUAL(
        Date(Year(2000), Month(2), Day(10)).next(),
        Date(Year(2000), Month(2), Day(11)));
    UNIT_ASSERT_VALUES_EQUAL(
        Date(Year(2000), Month(2), Day(29)).next(),
        Date(Year(2000), Month(3), Day(1)));
    UNIT_ASSERT_VALUES_EQUAL(
        Date(Year(2000), Month(4), Day(30)).next(),
        Date(Year(2000), Month(5), Day(1)));
    UNIT_ASSERT_VALUES_EQUAL(
        Date(Year(2000), Month(12), Day(31)).next(),
        Date(Year(2001), Month(1), Day(1)));

    UNIT_ASSERT_VALUES_EQUAL(
        Date(Year(2000), Month(2), Day(1)).addDays(7),
        Date(Year(2000), Month(2), Day(8)));
    UNIT_ASSERT_VALUES_EQUAL(
        Date(Year(2000), Month(2), Day(1)).addDays(28),
        Date(Year(2000), Month(2), Day(29)));
    UNIT_ASSERT_EXCEPTION(
        Date(Year(2000), Month(2), Day(1)).addDays(366),
        maps::RuntimeError);

    UNIT_ASSERT_VALUES_EQUAL(
        Date(Year(2000), Month(4), Day(20)).addDays(-6),
        Date(Year(2000), Month(4), Day(14)));
    UNIT_ASSERT_VALUES_EQUAL(
        Date(Year(2000), Month(3), Day(1)).addDays(-1),
        Date(Year(2000), Month(2), Day(29)));
    UNIT_ASSERT_VALUES_EQUAL(
        Date(Year(2000), Month(1), Day(1)).addDays(-1),
        Date(Year(1999), Month(12), Day(31)));
    UNIT_ASSERT_EXCEPTION(
        Date(Year(2000), Month(1), Day(1)).addDays(-366),
        maps::RuntimeError);
}

Y_UNIT_TEST(test_weekday)
{
    UNIT_ASSERT_VALUES_EQUAL(
        Date(Year(2020), Month(2), Day(28)).weekday(),
        WeekdayFlags::Friday);
    UNIT_ASSERT_VALUES_EQUAL(
        Date(Year(2020), Month(2), Day(29)).weekday(),
        WeekdayFlags::Saturday);
    UNIT_ASSERT_VALUES_EQUAL(
        Date(Year(2020), Month(3), Day(1)).weekday(),
        WeekdayFlags::Sunday);
    UNIT_ASSERT_VALUES_EQUAL(
        Date(Year(2020), Month(3), Day(2)).weekday(),
        WeekdayFlags::Monday);
    UNIT_ASSERT_VALUES_EQUAL(
        Date(Year(2020), Month(3), Day(3)).weekday(),
        WeekdayFlags::Tuesday);
    UNIT_ASSERT_VALUES_EQUAL(
        Date(Year(2020), Month(3), Day(4)).weekday(),
        WeekdayFlags::Wednesday);
    UNIT_ASSERT_VALUES_EQUAL(
        Date(Year(2020), Month(3), Day(5)).weekday(),
        WeekdayFlags::Thursday);

    UNIT_ASSERT_VALUES_EQUAL(
        Date(Year(2020), Month(12), Day(31)).weekday(),
        WeekdayFlags::Thursday);
    UNIT_ASSERT_VALUES_EQUAL(
        Date(Year(2021), Month(1), Day(1)).weekday(),
        WeekdayFlags::Friday);
    UNIT_ASSERT_VALUES_EQUAL(
        Date(Year(2021), Month(2), Day(28)).weekday(),
        WeekdayFlags::Sunday);
    UNIT_ASSERT_VALUES_EQUAL(
        Date(Year(2021), Month(2), Day(29)).weekday(),
        WeekdayFlags::Monday);
    UNIT_ASSERT_VALUES_EQUAL(
        Date(Year(2021), Month(12), Day(31)).weekday(),
        WeekdayFlags::Friday);
}

Y_UNIT_TEST(test_canonical_date_time)
{
    const std::string t1 = "2014-10-16T18:43:51+04:00";
    const std::string t11 = "2014-10-16T18:43:51.786786786+04:00";
    const std::string t2 = "2014-10-16 18:43:51+04:00";
    const std::string t3 = "2014-10-16 18:43:51+04";
    const std::string t4 = "2014-10-16 18:43:51-4";
    const std::string t41 = "2014-10-16 18:43:51.45645-4";
    const std::string t5 = "2014-10-16T18:43:51";
    const std::string t6 = "2014-10-16T18:43:51-4:00";

    UNIT_ASSERT_STRINGS_EQUAL(canonicalDateTimeString(t1, WithTimeZone::Yes), t1);
    UNIT_ASSERT_STRINGS_EQUAL(canonicalDateTimeString(t11, WithTimeZone::Yes), t1);
    UNIT_ASSERT_STRINGS_EQUAL(canonicalDateTimeString(t1, WithTimeZone::No), t5);
    UNIT_ASSERT_STRINGS_EQUAL(canonicalDateTimeString(t2, WithTimeZone::Yes), t1);
    UNIT_ASSERT_STRINGS_EQUAL(canonicalDateTimeString(t2, WithTimeZone::No), t5);
    UNIT_ASSERT_STRINGS_EQUAL(canonicalDateTimeString(t3, WithTimeZone::Yes), t1);
    UNIT_ASSERT_STRINGS_EQUAL(canonicalDateTimeString(t3, WithTimeZone::No), t5);
    UNIT_ASSERT_STRINGS_EQUAL(canonicalDateTimeString(t4, WithTimeZone::Yes), t6);
    UNIT_ASSERT_STRINGS_EQUAL(canonicalDateTimeString(t4, WithTimeZone::No), t5);
    UNIT_ASSERT_STRINGS_EQUAL(canonicalDateTimeString(t41, WithTimeZone::Yes), t6);
    UNIT_ASSERT_STRINGS_EQUAL(canonicalDateTimeString(t41, WithTimeZone::No), t5);
    UNIT_ASSERT_STRINGS_EQUAL(canonicalDateTimeString(t5, WithTimeZone::No), t5);
    UNIT_ASSERT_STRINGS_EQUAL(canonicalDateTimeString(t5, WithTimeZone::Yes), t5);
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::common::tests

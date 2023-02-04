#include <maps/b2bgeo/libs/time/timerange.h>
#include <maps/b2bgeo/libs/time/iso_datetime.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

#include <contrib/libs/cctz/include/cctz/civil_time.h>
#include <contrib/libs/cctz/include/cctz/time_zone.h>


using namespace maps::b2bgeo::time;

namespace {

const int TZ_SHIFT_MSK = 3;
const int MIDNIGHT_MSK = 1524690000;

int toSeconds(int days, int hours, int minutes, int seconds)
{
    return 86400 * days + 3600 * hours + 60 * minutes + seconds;
}

int timeFromString(const std::string& timeStr)
{
    return static_cast<int>(Time::fromStringRelativeFormat(timeStr));
}

} // anonymous namespace

Y_UNIT_TEST_SUITE(time_tests) {
    Y_UNIT_TEST(test_get_local_midnight)
    {
        ASSERT_EQ(getLocalMidnight(MIDNIGHT_MSK, TZ_SHIFT_MSK), MIDNIGHT_MSK);

        ASSERT_EQ(getLocalMidnight(MIDNIGHT_MSK + 1, TZ_SHIFT_MSK),
                  MIDNIGHT_MSK);
        ASSERT_EQ(getLocalMidnight(MIDNIGHT_MSK + 8 * HOUR_S, TZ_SHIFT_MSK),
                  MIDNIGHT_MSK);

        ASSERT_EQ(getLocalMidnight(MIDNIGHT_MSK - 1, TZ_SHIFT_MSK),
                  MIDNIGHT_MSK - DAY_S);
        ASSERT_EQ(getLocalMidnight(MIDNIGHT_MSK - 8 * HOUR_S, TZ_SHIFT_MSK),
                  MIDNIGHT_MSK - DAY_S);
    }

    Y_UNIT_TEST(test_time_from_string_old_format)
    {
        ASSERT_EQ(timeFromString("15:00"), toSeconds(0, 15, 0, 0));
        ASSERT_EQ(timeFromString("17 :  30:21"), toSeconds(0, 17, 30, 21));
        ASSERT_EQ(timeFromString("1 0:02  "), toSeconds(0, 10, 2, 0));
        ASSERT_EQ(timeFromString("2.5:3:1"), toSeconds(2, 5, 3, 1));
        ASSERT_EQ(timeFromString("2.05:003:021"), toSeconds(2, 5, 3, 21));
    }

    Y_UNIT_TEST(test_time_from_string_old_format_throw)
    {
        maps::b2bgeo::localization::initialize();
        ASSERT_THROW(
            Time::fromStringRelativeFormat("60:15"),
            TimeRangeInvalidFormatLocalized);
        ASSERT_THROW(
            Time::fromStringRelativeFormat("24:30:15"),
            TimeRangeInvalidFormatLocalized);
        ASSERT_THROW(
            Time::fromStringRelativeFormat("07:30:60"),
            TimeRangeInvalidFormatLocalized);
        ASSERT_THROW(
            Time::fromStringRelativeFormat("2.4.30"),
            TimeRangeInvalidFormatLocalized);
        ASSERT_THROW(
            Time::fromStringRelativeFormat("15:15:15:15"),
            TimeRangeInvalidFormatLocalized);
        ASSERT_THROW(
            Time::fromStringRelativeFormat("-2.15:10"),
            TimeRangeInvalidFormatLocalized);
        ASSERT_THROW(
            Time::fromStringRelativeFormat("15:10c"),
            TimeRangeInvalidFormatLocalized);
        ASSERT_THROW(
            Time::fromStringRelativeFormat("15::30"),
            TimeRangeInvalidFormatLocalized);
        ASSERT_THROW(
            Time::fromStringRelativeFormat(""),
            TimeRangeInvalidFormatLocalized);
        ASSERT_THROW(
            Time::fromStringRelativeFormat("1."),
            TimeRangeInvalidFormatLocalized);
    }

    Y_UNIT_TEST(test_time_from_string)
    {
        cctz::time_zone moscowTz;
        cctz::load_time_zone("Europe/Moscow", &moscowTz);
        const auto moscowLocalMidnight = getTimestamp(
            cctz::convert(cctz::civil_second(2018, 9, 7, 0, 0, 0), moscowTz));
        const auto res = Time::fromString("2018-09-07T10:00:00+03", moscowLocalMidnight);
        ASSERT_EQ(res, 10 * HOUR_S);
        ASSERT_EQ(Time::fromString("10:00:00", moscowLocalMidnight), 10 * HOUR_S);
        ASSERT_EQ(Time::fromString("1.10:00:00", moscowLocalMidnight), 10 * HOUR_S + DAY_S);
        ASSERT_EQ(Time::fromString("2018-09-08T10:00:00+03", moscowLocalMidnight), 10 * HOUR_S + DAY_S);
    }

    Y_UNIT_TEST(test_time_from_string_throw)
    {
        maps::b2bgeo::localization::initialize();
        ASSERT_THROW(Time::fromString("10:00:00T", MIDNIGHT_MSK), TimeRangeInvalidFormatLocalized);
        ASSERT_THROW(Time::fromString("0.2018-09-08T10:00:00+03", MIDNIGHT_MSK), TimeRangeInvalidFormatLocalized);
    }

    Y_UNIT_TEST(test_time_to_string)
    {
        cctz::time_zone moscowTz;
        cctz::load_time_zone("Europe/Moscow", &moscowTz);
        const auto moscowLocalMidnight = getTimestamp(
            cctz::convert(cctz::civil_second(2018, 9, 7, 0, 0, 0), moscowTz));
        const auto res = Time::fromString("2018-09-07T10:00:00+03", moscowLocalMidnight);
        ASSERT_EQ("10:00:00", Time::toStringRelative(res));
    }

    Y_UNIT_TEST(test_time_to_string_iso8601)
    {
        cctz::time_zone moscowTz;
        cctz::load_time_zone("Europe/Moscow", &moscowTz);
        const auto moscowLocalMidnight = getTimestamp(
            cctz::convert(cctz::civil_second(2018, 9, 7, 0, 0, 0), moscowTz));
        const auto res = Time::fromString("2018-09-07T10:00:00+03", moscowLocalMidnight);
        ASSERT_EQ("2018-09-07T10:00:00+03:00", Time::toStringIso8601(res, moscowLocalMidnight, TZ_SHIFT_MSK));
    }
}
Y_UNIT_TEST_SUITE(time_range_tests)
{
    Y_UNIT_TEST(test_timerange_from_string)
    {
        cctz::time_zone moscowTz;
        cctz::load_time_zone("Europe/Moscow", &moscowTz);
        const auto moscowLocalMidnight = getTimestamp(
            cctz::convert(cctz::civil_second(2018, 9, 1, 0, 0, 0), moscowTz));

        const auto res = TimeRange::fromStringAbsoluteFormat(
            "2018-9-1T10:00:00+03/2018-9-1T11:00:00+03", moscowLocalMidnight);
        ASSERT_EQ(res, TimeRange::fromString(
                           "2018-9-1T10:00:00+03/2018-9-1T11:00:00+03",
                           moscowLocalMidnight));
        ASSERT_EQ(res, TimeRange::fromString("0.10:00:00-0.11:00:00",
                                             moscowLocalMidnight));
        ASSERT_EQ(res, TimeRange::fromString("10:00:00-11:00:00",
                                             moscowLocalMidnight));
        ASSERT_EQ(res,
                  TimeRange::fromString("10:00-11:00", moscowLocalMidnight));
        ASSERT_EQ(res, TimeRange::fromString("10-11", moscowLocalMidnight));

        ASSERT_DOUBLE_EQ(res.begin, 10 * HOUR_S);
        ASSERT_DOUBLE_EQ(res.end, 11 * HOUR_S);
    }

    Y_UNIT_TEST(test_timerange_from_string_throw)
    {
        maps::b2bgeo::localization::initialize();
        ASSERT_THROW(
            TimeRange::fromString("00:00:00-1.06.00.00", MIDNIGHT_MSK),
            TimeRangeInvalidFormatLocalized);
        ASSERT_THROW(
            TimeRange::fromString("00:00:00-2018-9-1T11:00:00+03", MIDNIGHT_MSK),
            TimeRangeInvalidFormatLocalized);
        ASSERT_THROW(
            TimeRange::fromString("13 - 12", MIDNIGHT_MSK),
            TimeRangeInvalidFormatLocalized);
    }

    Y_UNIT_TEST(test_timerange_from_string_incorrect_time_window_end)
    {
        maps::b2bgeo::localization::initialize();
        cctz::time_zone moscowTz;
        cctz::load_time_zone("Europe/Moscow", &moscowTz);
        const auto moscowLocalMidnight = getTimestamp(
            cctz::convert(cctz::civil_second(2018, 9, 2, 0, 0, 0), moscowTz));
        ASSERT_THROW(
            TimeRange::fromString("2018-9-1T10:00:00+03/2018-9-1T11:00:00+03", moscowLocalMidnight),
            TimeRangeInvalidFormatLocalized);
    }

    Y_UNIT_TEST(test_timerange_to_string)
    {
        cctz::time_zone moscowTz;
        cctz::load_time_zone("Europe/Moscow", &moscowTz);
        const auto moscowLocalMidnight = getTimestamp(
            cctz::convert(cctz::civil_second(2018, 9, 1, 0, 0, 0), moscowTz));

        const auto res = TimeRange::fromString(
            "2018-9-1T10:00:00+03/2018-9-1T11:00:00+03", moscowLocalMidnight);
        ASSERT_EQ(res, TimeRange::fromString(
            "10:00:00-11:00:00", moscowLocalMidnight));
        ASSERT_EQ(
            "10:00:00-11:00:00", res.toStringRelative());
    }

    Y_UNIT_TEST(test_timerange_to_string_iso8601)
    {
        cctz::time_zone moscowTz;
        cctz::load_time_zone("Europe/Moscow", &moscowTz);
        const auto moscowLocalMidnight = getTimestamp(
            cctz::convert(cctz::civil_second(2018, 9, 1, 0, 0, 0), moscowTz));

        const auto res = TimeRange::fromString(
            "2018-9-1T10:00:00+03/2018-9-1T11:00:00+03", moscowLocalMidnight);
        ASSERT_EQ(res, TimeRange::fromString(
            "10:00:00-11:00:00", moscowLocalMidnight));
        ASSERT_EQ(
            "2018-09-01T10:00:00+03:00/2018-09-01T11:00:00+03:00",
            res.toStringIso8601(moscowLocalMidnight, TZ_SHIFT_MSK));
    }

};

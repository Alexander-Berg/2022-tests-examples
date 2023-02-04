#include <maps/b2bgeo/libs/time/datetime.h>
#include <maps/b2bgeo/libs/time/iso_datetime.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

#include <contrib/libs/cctz/include/cctz/civil_time.h>
#include <contrib/libs/cctz/include/cctz/time_zone.h>


using namespace maps::b2bgeo::time;

Y_UNIT_TEST_SUITE(iso_datetime_tests){
Y_UNIT_TEST(get_timestamp){
    const cctz::time_zone tz;
    const auto tp = cctz::convert(cctz::civil_second(2018, 4, 26, 0, 0, 0), tz);
    ASSERT_DOUBLE_EQ(getTimestamp(tp), 1524700800);
}

Y_UNIT_TEST(utc_timestamp)
{
    const cctz::time_zone tz;

    ASSERT_EQ(parseDatetime("2018-09-02T12:30:00Z"),
              cctz::convert(cctz::civil_second(2018, 9, 2, 12, 30, 0), tz));

    ASSERT_EQ(parseDatetime("2018-09-02T12:30Z"),
              cctz::convert(cctz::civil_second(2018, 9, 2, 12, 30, 0), tz));
}

Y_UNIT_TEST(timestamp_with_time_zone)
{
    const cctz::time_zone tz;

    ASSERT_EQ(
        parseDatetime("2018-09-01T10:30+01:00"),
        cctz::convert(cctz::civil_second(2018, 9, 1, 10 - 1, 30, 0), tz));

    ASSERT_EQ(
        parseDatetime("2018-09-01T10:30+02:00"),
        cctz::convert(cctz::civil_second(2018, 9, 1, 10 - 2, 30, 0), tz));

    ASSERT_EQ(
        parseDatetime("2018-09-01T10:30:10+02:00"),
        cctz::convert(cctz::civil_second(2018, 9, 1, 10 - 2, 30, 10), tz));

    ASSERT_EQ(
        parseDatetime("2018-09-01T10:30:10.5+02:00"),
        cctz::convert(cctz::civil_second(2018, 9, 1, 10 - 2, 30, 10), tz)
            + std::chrono::milliseconds{500});

    ASSERT_EQ(
        parseDatetime("2018-09-01T10:30:10+02"),
        cctz::convert(cctz::civil_second(2018, 9, 1, 10 - 2, 30, 10), tz));

    ASSERT_EQ(parseDatetime("2018-09-01T10:30:10+02:30"),
              cctz::convert(
                  cctz::civil_second(2018, 9, 1, 10 - 2, 30 - 30, 10), tz));

    ASSERT_EQ(
        parseDatetime("2018-09-01T10:30:10-02"),
        cctz::convert(cctz::civil_second(2018, 9, 1, 10 + 2, 30, 10), tz));

    ASSERT_EQ(parseDatetime("2018-09-01T10:30:10-02:30"),
              cctz::convert(
                  cctz::civil_second(2018, 9, 1, 10 + 2, 30 + 30, 10), tz));

    ASSERT_EQ(
        parseDatetime("2018-9-1T10:30:10+02"),
        cctz::convert(cctz::civil_second(2018, 9, 1, 10 - 2, 30, 10), tz));

    ASSERT_EQ(
        parseDatetime("  2018-9-1T10:30:10+02 "),
        cctz::convert(cctz::civil_second(2018, 9, 1, 10 - 2, 30, 10), tz));

    ASSERT_EQ(
        parseDatetime("2018-09-01T10:30:10+02:00:00"),
        cctz::convert(cctz::civil_second(2018, 9, 1, 10 - 2, 30, 10), tz));
}

Y_UNIT_TEST(invalid_string)
{
    ASSERT_THROW(parseDatetime("Invalid"), TimeRangeInvalidFormat);
    ASSERT_THROW(parseDatetime("2018-09-02T12:30:00"), TimeRangeInvalidFormat);
    ASSERT_THROW(parseDatetime("2018-09-01T10:30:10+02qqq"),
                 TimeRangeInvalidFormat);
    ASSERT_THROW(parseDatetime("2018.09.01T10:30:10+02"),
                 TimeRangeInvalidFormat);
}

Y_UNIT_TEST(invalid_value)
{
    ASSERT_THROW(parseDatetime("2018-13-02T12:30:00Z"), TimeRangeInvalidFormat);
    ASSERT_THROW(parseDatetime("2018-09-32T12:30:00Z"), TimeRangeInvalidFormat);
    ASSERT_THROW(parseDatetime("2018-09-32T24:00:00Z"), TimeRangeInvalidFormat);
    ASSERT_THROW(parseDatetime("2018-09-32T01:60:00Z"), TimeRangeInvalidFormat);
    ASSERT_THROW(parseDatetime("2018-09-32T01:00:60Z"), TimeRangeInvalidFormat);
}

Y_UNIT_TEST(invalid_time_zone)
{
    ASSERT_THROW(parseDatetime("2018-09-01T10:30:10+24"),
                 TimeRangeInvalidFormat);
    ASSERT_THROW(parseDatetime("2018-09-01T10:30:10-24"),
                 TimeRangeInvalidFormat);

    ASSERT_THROW(parseDatetime("2018-09-01T10:30:10+2"), TimeRangeInvalidFormat);
    ASSERT_THROW(parseDatetime("2018-09-01T10:30:10+2:1"),
                 TimeRangeInvalidFormat);
    ASSERT_THROW(parseDatetime("2018-09-01T10:30:10Z+02"),
                 TimeRangeInvalidFormat);
}

Y_UNIT_TEST(time_interval)
{
    const cctz::time_zone tz;
    {
        auto res = parseTimeInterval("2018-9-1T10:30:10Z/2018-9-1T10:30:20Z");
        ASSERT_DOUBLE_EQ(
            res.begin, getTimestamp(cctz::convert(
                           cctz::civil_second(2018, 9, 1, 10, 30, 10), tz)));
        ASSERT_DOUBLE_EQ(
            res.end, getTimestamp(cctz::convert(
                         cctz::civil_second(2018, 9, 1, 10, 30, 20), tz)));
    }

    {
        auto res = parseTimeInterval("2018-9-1T10:30:10Z/2018-9-1T10:30:10Z");
        ASSERT_DOUBLE_EQ(
            res.begin, getTimestamp(cctz::convert(
                           cctz::civil_second(2018, 9, 1, 10, 30, 10), tz)));
        ASSERT_DOUBLE_EQ(
            res.end, getTimestamp(cctz::convert(
                         cctz::civil_second(2018, 9, 1, 10, 30, 10), tz)));
    }
    {
        auto res = parseTimeInterval(
            "2018-09-01T10:30:10+02/2018-09-01T10:30:20+01");
        ASSERT_DOUBLE_EQ(
            res.begin,
            getTimestamp(cctz::convert(
                cctz::civil_second(2018, 9, 1, 10 - 2, 30, 10), tz)));
        ASSERT_DOUBLE_EQ(
            res.end,
            getTimestamp(cctz::convert(
                cctz::civil_second(2018, 9, 1, 10 - 1, 30, 20), tz)));
    }
}

Y_UNIT_TEST(invalid_time_interval)
{
    ASSERT_THROW(
        parseTimeInterval("2018-09-01T10:30:10+02-2018-09-01T10:30:20+01"),
        TimeRangeInvalidFormat);

    ASSERT_THROW(parseTimeInterval(
                     "2018-9-1T10:30:10Invalid/2018-9-1T10:30:10Invalid"),
                 TimeRangeInvalidFormat);

    ASSERT_THROW(parseTimeInterval("2018-09-01T10:30:10+01/1.10:30:00"),
                 TimeRangeInvalidFormat);

    ASSERT_THROW(
        parseTimeInterval("2018-09-01T10:30:10+02/2018-09-01T10:30:20+01/"
                          "2018-09-01T10:30:20+00"),
        TimeRangeInvalidFormat);
}

Y_UNIT_TEST(time_interval_end_less_than_begin)
{
    ASSERT_THROW(parseTimeInterval("2018-9-1T10:30:20Z/2018-9-1T10:30:10Z"),
                 TimeRangeInvalidFormat);

    ASSERT_THROW(
        parseTimeInterval("2018-09-01T10:30:10+01/2018-09-01T10:30:20+02"),
        TimeRangeInvalidFormat);
}

Y_UNIT_TEST(time_get_as_string_iso8601)
{
    {
        const auto secSinceEpoch = 1524700800;

        ASSERT_EQ(
            "2018-04-26T00:00:00+00:00",
            getTimeAsStringIso8601(secSinceEpoch, 0));

        ASSERT_EQ(
            "2018-04-25T22:00:00-02:00",
            getTimeAsStringIso8601(secSinceEpoch, -2));

        ASSERT_EQ(
            "2018-04-26T03:00:00+03:00",
            getTimeAsStringIso8601(secSinceEpoch, 3));

        ASSERT_EQ(
            "2018-04-26T03:30:00+03:30",
            getTimeAsStringIso8601(secSinceEpoch, 3.5));
    }

    {
        const auto secSinceEpoch = 1524690000;

        ASSERT_EQ(
            "2018-04-25T21:00:00+00:00",
            getTimeAsStringIso8601(secSinceEpoch, 0));

        ASSERT_EQ(
            "2018-04-25T19:00:00-02:00",
            getTimeAsStringIso8601(secSinceEpoch, -2));

        ASSERT_EQ(
            "2018-04-26T00:00:00+03:00",
            getTimeAsStringIso8601(secSinceEpoch, 3));

        ASSERT_EQ(
            "2018-04-26T00:30:00+03:30",
            getTimeAsStringIso8601(secSinceEpoch, 3.5));
    }
}
}
;

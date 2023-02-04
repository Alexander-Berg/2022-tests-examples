#include <maps/libs/chrono/include/time_point.h>
#include <maps/libs/chrono/include/days.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <chrono>

namespace maps::chrono::tests {

using namespace std::literals::chrono_literals;
using namespace maps::chrono::literals;

const std::string GLOBAL_TIME_STRING = "2017-01-02T08:30:00Z";
const auto GLOBAL_TIME_POINT = parseIsoDateTime(GLOBAL_TIME_STRING);

Y_UNIT_TEST_SUITE(test_date_time)
{

Y_UNIT_TEST(test_global_initialization_works)
{
    const auto LOCAL_TIME_POINT = parseIsoDateTime(GLOBAL_TIME_STRING);
    EXPECT_EQ(LOCAL_TIME_POINT, GLOBAL_TIME_POINT);
}

Y_UNIT_TEST(test_parsing_fmt_date_time)
{
    const auto zeroTime = parseIntegralDateTime("1970/01/01 00:00:00", "%Y/%m/%d %H:%M:%S");
    EXPECT_EQ(zeroTime.time_since_epoch(), 0h);

    const auto oneHourTime = parseIntegralDateTime("01 hours 00 minutes 00 seconds at 1970-01-01", "%H hours %M minutes %S seconds at %Y-%m-%d");
    EXPECT_EQ(oneHourTime.time_since_epoch(), 1h);

    EXPECT_THROW(
        parseIntegralDateTime("2000/01/01 00:00:00", "%Y%m%d-%H%M%S"),
        MalformedDateTime
    );
}

Y_UNIT_TEST(test_parsing_sql_date_time)
{
    const std::string UNIX_EPOCH_UTC = "1970-01-01 00:00:00.00000";
    auto utc = parseSqlDateTime(UNIX_EPOCH_UTC);
    EXPECT_EQ(utc.time_since_epoch(), 0h);

    const std::string UNIX_EPOCH_PLUS_PATCH = "1970-01-01 00:00:00.123456789";
    auto epochPlusPatch = parseSqlDateTime(UNIX_EPOCH_PLUS_PATCH);
    EXPECT_EQ(epochPlusPatch.time_since_epoch(), 123456789ns);

    const std::string UNIX_EPOCH_NEGATIVE_PLUS_PATCH = "1970-01-01 00:00:00.123456789+03:00";
    auto epochNegativePlusPatch = parseSqlDateTime(UNIX_EPOCH_NEGATIVE_PLUS_PATCH);
    EXPECT_EQ(epochNegativePlusPatch.time_since_epoch(), -3h + 123456789ns);

    const std::string UNIX_EPOCH_MSK = "1970-01-01 00:00:00.00000+03";
    auto msk = parseSqlDateTime(UNIX_EPOCH_MSK);
    EXPECT_EQ(msk.time_since_epoch(), -3h);

    const std::string UNIX_EPOCH_HONDURAS = "1970-01-01 00:00:00.00000-06";
    auto honduras = parseSqlDateTime(UNIX_EPOCH_HONDURAS);
    EXPECT_EQ(honduras.time_since_epoch(), 6h);

    const std::string UNIX_EPOCH_PARTIAL = "1970-01-01 00:00:00.00000-04:30";
    auto partial = parseSqlDateTime(UNIX_EPOCH_PARTIAL);
    EXPECT_EQ(partial.time_since_epoch(), 4h + 30min);

    const std::string ONCE_UPON_A_TIME = "1970-02-02 00:03:00.5000";
    auto onceUpon = parseSqlDateTime(ONCE_UPON_A_TIME);
    auto onceUponSinceEpoch = 32_days + 3min + 500ms;
    EXPECT_EQ(onceUpon.time_since_epoch(), onceUponSinceEpoch);

    const std::vector<std::string> INVALID_SQL_DATE_TIMES{
        ""
        "OLOLO",
        "1970-13-01 00:00:00",
        "1970-01-40 00:00:00",
        "2015-01-01 00:00:00.абырвалг",
        "2015-01-01 00:00:00:77"
    };
    for (const auto& invalidDateTime: INVALID_SQL_DATE_TIMES) {
        EXPECT_THROW(
            parseSqlDateTime(invalidDateTime),
            MalformedDateTime
        );
    }
}

Y_UNIT_TEST(test_parsing_iso_date_time)
{
    const std::string UNIX_EPOCH_PLUS_PATCH = "1970-01-02T03:04:05";
    auto epochPlusPatch = parseIsoDateTime(UNIX_EPOCH_PLUS_PATCH);
    EXPECT_EQ(
        epochPlusPatch.time_since_epoch(),
        (1_days + 3h + 4min + 5s)
    );

    const std::vector<std::string> INVALID_ISO_DATE_TIMES{
        ""
        "OLOLO",
        "1970-13-01T00:00:00",
        "1970-01-40T00:00:00",
        "2015-01-01T00:00:00.абырвалг",
        "2015-01-01T00:00:00:77Z"
    };
    for (const auto& invalidDateTime: INVALID_ISO_DATE_TIMES) {
        EXPECT_THROW(
            parseSqlDateTime(invalidDateTime),
            MalformedDateTime
        );
    }
}

Y_UNIT_TEST(test_parsing_iso_date)
{
    const std::string UNIX_EPOCH_PLUS_PATCH = "1970-01-02";
    auto epochPlusPatch = parseIsoDate(UNIX_EPOCH_PLUS_PATCH);
    EXPECT_EQ(
        epochPlusPatch.time_since_epoch(),
        (1_days)
    );

    const std::vector<std::string> INVALID_ISO_DATES{
        ""
        "OLOLO",
        "1970-13-01",
    };
    for (const auto& invalidDate: INVALID_ISO_DATES) {
        EXPECT_THROW(
            parseIsoDate(invalidDate),
            MalformedDateTime
        );
    }
}

Y_UNIT_TEST(test_parsing_http_date_time)
{
    auto epoch = parseHttpDateTime("Thu, 01 Jan 1970 00:00:00 GMT");
    EXPECT_EQ(epoch.time_since_epoch(), 0s);

    auto example = parseHttpDateTime("Sun, 04 Jan 1970 08:49:37 GMT");
    EXPECT_EQ(example.time_since_epoch(), 3_days + 8h + 49min + 37s);

    const std::vector<std::string> INVALID_HTTP_DATE_TIMES{
        "",
        "OLOLO",
        "Thu, 40 Jan 1970 00:00:00 GMT",
    };
    for (const auto& invalidDateTime: INVALID_HTTP_DATE_TIMES) {
        EXPECT_THROW(
            parseHttpDateTime(invalidDateTime),
            MalformedDateTime
        ) << "Where invalidDateTime = '" << invalidDateTime << "'";
    }
}

Y_UNIT_TEST(test_format_fmt_date_time)
{
    const char* FMT = "%Y/%m/%d %H:%M:%S";
    const std::string UNIX_EPOCH_UTC = "1970/01/01 00:00:00";
    EXPECT_EQ(
        UNIX_EPOCH_UTC,
        formatIntegralDateTime(parseIntegralDateTime(UNIX_EPOCH_UTC, FMT), FMT)
    );
}

Y_UNIT_TEST(test_format_sql_date_time)
{
    const std::string UNIX_EPOCH_UTC = "1970-01-01 00:00:00+00:00";
    EXPECT_EQ(
        UNIX_EPOCH_UTC,
        formatSqlDateTime(parseSqlDateTime(UNIX_EPOCH_UTC))
    );

    const std::string UNIX_EPOCH_PLUS_PATCH = "2015-10-15 17:23:44.123456789+00:00";
    EXPECT_EQ(
        UNIX_EPOCH_PLUS_PATCH,
        formatSqlDateTime(parseSqlDateTime(UNIX_EPOCH_PLUS_PATCH))
    );

    const std::string UNIX_EPOCH_FRACTIONAL_TZ = "1970-01-01 00:00:00-03:30";
    const std::string EXPECTED = "1970-01-01 03:30:00+00:00";
    EXPECT_EQ(
        EXPECTED,
        formatSqlDateTime(parseSqlDateTime(UNIX_EPOCH_FRACTIONAL_TZ))
    );

    const std::string UNIX_EPOCH_NEGATIVE_PLUS_PATCH = "1970-01-01 00:00:00.123456789+03:00";
    const std::string EXPECTED_NEGATIVE = "1969-12-31 21:00:00.123456789+00:00";
    EXPECT_EQ(
        EXPECTED_NEGATIVE,
        formatSqlDateTime(parseSqlDateTime(UNIX_EPOCH_NEGATIVE_PLUS_PATCH))
    );
}

Y_UNIT_TEST(test_format_iso_date_time)
{
    const std::string UNIX_EPOCH_UTC = "1970-01-01T00:00:00Z";
    EXPECT_EQ(
        formatIsoDateTime(parseIsoDateTime(UNIX_EPOCH_UTC)),
        UNIX_EPOCH_UTC
    );

    const std::string UNIX_EPOCH_PLUS_PATCH = "2015-10-15T17:23:44.123456000Z";
    EXPECT_EQ(
        formatIsoDateTime(parseIsoDateTime(UNIX_EPOCH_PLUS_PATCH)),
        UNIX_EPOCH_PLUS_PATCH
    );

    const std::string UNIX_EPOCH_NEGATIVE_PLUS_PATCH = "1970-01-01T00:00:00.123456789+03:00";
    const std::string EXPECTED_NEGATIVE = "1969-12-31T21:00:00.123456789Z";
    EXPECT_EQ(
        formatIsoDateTime(parseIsoDateTime(UNIX_EPOCH_NEGATIVE_PLUS_PATCH)),
        EXPECTED_NEGATIVE
    );

    const std::string UNIX_EPOCH_FRACTIONAL_TZ = "1970-01-01T00:00:00-03:30";
    const std::string EXPECTED = "1970-01-01T03:30:00Z";
    EXPECT_EQ(
        formatIsoDateTime(parseIsoDateTime(UNIX_EPOCH_FRACTIONAL_TZ)),
        EXPECTED
    );
}

Y_UNIT_TEST(test_get_time_span)
{
    { // Minutes
        const auto [begin, end] = getComprisingTimeSpan<std::chrono::minutes>(parseIsoDateTime("2019-01-25T10:47:05"));
        EXPECT_EQ(begin, parseIsoDateTime("2019-01-25T10:47:00"));
        EXPECT_EQ(end,   parseIsoDateTime("2019-01-25T10:48:00"));
    }

    { // Days
        const auto [begin, end] = getComprisingTimeSpan<Days>(parseIsoDateTime("2019-01-25T10:47:05"));
        EXPECT_EQ(begin, parseIsoDateTime("2019-01-25T00:00:00"));
        EXPECT_EQ(end,   parseIsoDateTime("2019-01-26T00:00:00"));
    }

// Windows port strptime from util/datetime/systime.h can't parse a time before the epoch
#ifndef _WIN32
    { // Time before the epoch
        const auto [begin, end] = getComprisingTimeSpan<Days>(parseIsoDateTime("1960-01-25T10:47:05"));
        EXPECT_EQ(begin, parseIsoDateTime("1960-01-25T00:00:00"));
        EXPECT_EQ(end,   parseIsoDateTime("1960-01-26T00:00:00"));
    }
#endif

    { // Time around the epoch beginning
        { // Just after the epoch
            const auto [begin, end] = getComprisingTimeSpan<Days>(parseIsoDateTime("1970-01-01T00:00:00"));
            EXPECT_EQ(begin, parseIsoDateTime("1970-01-01T00:00:00"));
            EXPECT_EQ(end,   parseIsoDateTime("1970-01-02T00:00:00"));
        }

// Windows port strptime from util/datetime/systime.h can't parse a time before the epoch
#ifndef _WIN32
        { // Just before the epoch
            const auto [begin, end] = getComprisingTimeSpan<Days>(parseIsoDateTime("1969-12-31T23:59:59"));
            EXPECT_EQ(begin, parseIsoDateTime("1969-12-31T00:00:00"));
            EXPECT_EQ(end,   parseIsoDateTime("1970-01-01T00:00:00"));
        }
#endif
    }

    { // Periods longer than a day
        { // Weeks
            using Weeks = std::chrono::duration<int, std::ratio<604800>>;
            const auto [begin, end] = getComprisingTimeSpan<Weeks>(parseIsoDateTime("2019-01-25T10:47:05")); // Friday

            // The interval is from Thursday to Thursday, because the 1st January of 1970 is Thursday
            EXPECT_EQ(begin, parseIsoDateTime("2019-01-24 00:00:00")); // Thursday
            EXPECT_EQ(end,   parseIsoDateTime("2019-01-31 00:00:00")); // Thursday
        }

        { // Months
            using Months = std::chrono::duration<int, std::ratio<2629746>>;
            const auto [begin, end] = getComprisingTimeSpan<Months>(parseIsoDateTime("2019-01-25T10:47:05"));

            // A typical month has fractional number of days, so begin and end
            // are fractional too.
            EXPECT_EQ(begin, parseIsoDateTime("2018-12-31 21:10:48"));
            EXPECT_EQ(end,   parseIsoDateTime("2019-01-31 07:39:54"));
        }
    }
}

Y_UNIT_TEST(test_format_http_date_time)
{
    std::chrono::system_clock::time_point epoch;
    EXPECT_EQ(formatHttpDateTime(epoch), "Thu, 01 Jan 1970 00:00:00 GMT");

    auto example = epoch + 3_days + 8h + 49min + 37s;
    EXPECT_EQ(formatHttpDateTime(example), "Sun, 04 Jan 1970 08:49:37 GMT");
}

Y_UNIT_TEST(test_parse_near_times)
{
    auto str1 = "2019-12-26T07:52:01.126378999Z";
    auto str2 = "2019-12-26T07:52:01.126379000Z";
    auto time1 = parseIsoDateTime(str1);
    auto time2 = parseIsoDateTime(str2);
    auto result1 = formatIsoDateTime(time1);
    auto result2 = formatIsoDateTime(time2);

    EXPECT_EQ(result1, str1);
    EXPECT_EQ(result2, str2);
    EXPECT_NE(time1, time2);
    EXPECT_NE(result1, result2);
}

Y_UNIT_TEST(test_overflow)
{
    auto positiveOverflowTime = convertToUnixTime(TimePoint::max());
    EXPECT_EQ(
        positiveOverflowTime,
        convertToUnixTime(convertFromUnixTime(positiveOverflowTime))
    );
    EXPECT_THROW(convertFromUnixTime(positiveOverflowTime + 1), RuntimeError);

    auto negativeOverflowTime = convertToUnixTime(TimePoint::min());
    EXPECT_EQ(
        negativeOverflowTime,
        convertToUnixTime(convertFromUnixTime(negativeOverflowTime))
    );
    EXPECT_THROW(convertFromUnixTime(negativeOverflowTime - 1), RuntimeError);
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::chrono::tests

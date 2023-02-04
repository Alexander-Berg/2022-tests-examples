#include <maps/automotive/parking/fastcgi/parking_api/lib/time_utils.h>

#include <library/cpp/testing/unittest/registar.h>

#include <maps/libs/common/include/exception.h>

#include <boost/date_time/posix_time/posix_time.hpp>
#include <boost/date_time/posix_time/posix_time_duration.hpp>

#include <string>

namespace maps::automotive::parking::tests {

Y_UNIT_TEST_SUITE(test_time_utils) {

    Y_UNIT_TEST(format_time_test)
    {
        boost::posix_time::ptime time1 = boost::posix_time::time_from_string("2016/2/19 9:07:00");
        UNIT_ASSERT_EQUAL(formatTime(time1), "09:07");
        boost::posix_time::ptime time2 = boost::posix_time::time_from_string("2016/2/19 16:59:20");
        UNIT_ASSERT_EQUAL(formatTime(time2), "16:59");
        boost::posix_time::ptime time3 = boost::posix_time::time_from_string("2016/2/19 3:31:10");
        UNIT_ASSERT_EQUAL(formatTime(time3), "03:31");
    }

    Y_UNIT_TEST(parse_string_time_zone_test)
    {
        boost::posix_time::time_duration tz1(3, 0, 0);
        UNIT_ASSERT_EQUAL(parseStringTimeZone("GMT+3"), tz1);
        boost::posix_time::time_duration tz2(0, 0, 0);
        UNIT_ASSERT_EQUAL(parseStringTimeZone("UTC"), tz2);
        boost::posix_time::time_duration tz3(3, 30, 0);
        UNIT_ASSERT_EQUAL(parseStringTimeZone("+03:30"), tz3);
    }

    Y_UNIT_TEST(duration_since_epoch_test)
    {
        boost::posix_time::ptime time1 = boost::posix_time::time_from_string("1999/2/19 9:07:10");
        boost::posix_time::time_duration dse1(255393, 7, 10);
        UNIT_ASSERT_EQUAL(durationSinceEpoch(time1), dse1);
        boost::posix_time::ptime time2 = boost::posix_time::time_from_string("2016/5/10 3:00:01");
        boost::posix_time::time_duration dse2(406347, 0, 1);
        UNIT_ASSERT_EQUAL(durationSinceEpoch(time2), dse2);
        boost::posix_time::ptime time3 = boost::posix_time::time_from_string("2100/6/30 18:35:05");
        boost::posix_time::time_duration dse3(1143906, 35, 5);
        UNIT_ASSERT_EQUAL(durationSinceEpoch(time3), dse3);
    }

    Y_UNIT_TEST(convert_string_iso8601_test_with_time_zone)
    {
        yandex::maps::proto::common2::i18n::Time protoTime;
        writeStringISO8601ToProtoTime("2017-07-01T18:14:35.463+03:00", protoTime);
        UNIT_ASSERT_EQUAL(protoTime.value(), 1498922075);
        UNIT_ASSERT_EQUAL(protoTime.tz_offset(), 10800);
        UNIT_ASSERT_EQUAL(protoTime.text(), "18:14");
    }

    Y_UNIT_TEST(convert_string_iso8601_test_without_time_zone)
    {
        yandex::maps::proto::common2::i18n::Time protoTime;
        writeStringISO8601ToProtoTime("2019-02-10T17:10:00.000", protoTime);
        UNIT_ASSERT_EQUAL(protoTime.value(), 1549818600);
        UNIT_ASSERT_EQUAL(protoTime.tz_offset(), 0);
        UNIT_ASSERT_EQUAL(protoTime.text(), "17:10");
    }

    Y_UNIT_TEST(convert_string_iso8601_test_with_z_time_zone)
    {
        yandex::maps::proto::common2::i18n::Time protoTime;
        writeStringISO8601ToProtoTime("2019-02-10T17:10:00.000Z", protoTime);
        UNIT_ASSERT_EQUAL(protoTime.value(), 1549818600);
        UNIT_ASSERT_EQUAL(protoTime.tz_offset(), 0);
        UNIT_ASSERT_EQUAL(protoTime.text(), "17:10");
    }

    Y_UNIT_TEST(convert_string_iso8601_test_with_negative_time_zone)
    {
        yandex::maps::proto::common2::i18n::Time protoTime;
        writeStringISO8601ToProtoTime("2019-02-10T10:10:00.000-02:00", protoTime);
        UNIT_ASSERT_EQUAL(protoTime.value(), 1549800600);
        UNIT_ASSERT_EQUAL(protoTime.tz_offset(), -7200);
        UNIT_ASSERT_EQUAL(protoTime.text(), "10:10");
    }

    Y_UNIT_TEST(convert_string_iso8601_test_wrong_date)
    {
        yandex::maps::proto::common2::i18n::Time time;
        UNIT_ASSERT_EXCEPTION(writeStringISO8601ToProtoTime("2017070118:04:35.463+03:00", time), std::exception);
    }

    Y_UNIT_TEST(convert_string_iso8601_test_wrong_time)
    {
        yandex::maps::proto::common2::i18n::Time time;
        UNIT_ASSERT_EXCEPTION(writeStringISO8601ToProtoTime("2017-07-01T180435.463+03:00", time), std::exception);
    }


} //Y_UNIT_TEST_SUITE

} //maps::automotive::parking::tests

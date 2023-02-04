#include <maps/b2bgeo/libs/time/datetime.h>
#include <maps/b2bgeo/libs/time/timerange.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>


using namespace maps::b2bgeo::time;

const int MIDNIGHT = 1524700800;

Y_UNIT_TEST_SUITE(datetime_tests) {
    Y_UNIT_TEST(from_string_midnight)
    {
        ASSERT_EQ(
            DateTime::fromString("2018-04-26", "%Y-%m-%d"),
            MIDNIGHT);

        ASSERT_EQ(
            DateTime::fromString("2018-4-26", "%Y-%m-%d"),
            MIDNIGHT);

        ASSERT_EQ(
            DateTime::fromString("2018-04-27", "%Y-%m-%d"),
            MIDNIGHT + DAY_S);
    }
    Y_UNIT_TEST(date_incorrect_format)
    {
        ASSERT_THROW(
            DateTime::fromString("02-03-2018", "%Y-%m-%d"),
            TimeRangeInvalidFormat);

        ASSERT_THROW(
            DateTime::fromString("2018-02-30", "%Y-%m-%d"),
            TimeRangeInvalidFormat);

        ASSERT_THROW(
            DateTime::fromString("2018-2-xx", "%Y-%m-%d"),
            TimeRangeInvalidFormat);

        ASSERT_THROW(
            DateTime::fromString("2020-20-20", "%Y-%m-%d"),
            TimeRangeInvalidFormat);

        ASSERT_THROW(
            DateTime::fromString("2018-03-32", "%Y-%m-%d"),
            TimeRangeInvalidFormat);

        ASSERT_THROW(
            DateTime::fromString("2018/03/02", "%Y-%m-%d"),
            TimeRangeInvalidFormat);

    }
};

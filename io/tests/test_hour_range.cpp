#include <yandex_io/libs/base/hour_range.h>

#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;

Y_UNIT_TEST_SUITE_F(TestHourRange, QuasarUnitTestFixture) {
    Y_UNIT_TEST(testHourRange)
    {
        HourRange range(0, 20, 0); // 00:00:00 - 20:59:59 in UTC
        UNIT_ASSERT(range.containsUtcHour(0));
        UNIT_ASSERT(range.containsUtcHour(15));
        UNIT_ASSERT(range.containsUtcHour(20));
        UNIT_ASSERT(!range.containsUtcHour(21));
        UNIT_ASSERT(!range.containsUtcHour(23));

        range = HourRange(23, 7, 0); // 23:00:00 - 07:59:59 in UTC
        UNIT_ASSERT(range.containsUtcHour(23));
        UNIT_ASSERT(range.containsUtcHour(0));
        UNIT_ASSERT(range.containsUtcHour(7));
        UNIT_ASSERT(!range.containsUtcHour(8));
        UNIT_ASSERT(!range.containsUtcHour(22));

        range = HourRange(23, 7, 3); // 23:00:00 - 07:59:59 in UTC+3
        UNIT_ASSERT(range.containsUtcHour(20));
        UNIT_ASSERT(range.containsUtcHour(0));
        UNIT_ASSERT(range.containsUtcHour(4));
        UNIT_ASSERT(!range.containsUtcHour(5));
        UNIT_ASSERT(!range.containsUtcHour(19));

        range.setTimezoneShiftHours(5); // 23:00:00 - 07:59:59 in UTC+5
        UNIT_ASSERT(range.containsUtcHour(18));
        UNIT_ASSERT(range.containsUtcHour(0));
        UNIT_ASSERT(range.containsUtcHour(2));
        UNIT_ASSERT(!range.containsUtcHour(3));
        UNIT_ASSERT(!range.containsUtcHour(17));

        range.setTimezoneShiftHours(0); // 23:00:00 - 07:59:59 in UTC
        UNIT_ASSERT(range.containsUtcHour(23));
        UNIT_ASSERT(range.containsUtcHour(0));
        UNIT_ASSERT(range.containsUtcHour(7));
        UNIT_ASSERT(!range.containsUtcHour(8));
        UNIT_ASSERT(!range.containsUtcHour(22));

        range.setTimezoneShiftHours(-2); // 23:00:00 - 07:59:59 in UTC-2
        UNIT_ASSERT(range.containsUtcHour(1));
        UNIT_ASSERT(range.containsUtcHour(9));
        UNIT_ASSERT(range.containsUtcHour(3));
        UNIT_ASSERT(!range.containsUtcHour(23));
        UNIT_ASSERT(!range.containsUtcHour(10));

        range = HourRange(1, 2, 3); // 01:00:00 - 02:59:59 in UTC+3;
        UNIT_ASSERT(!range.containsUtcHour(21));
        UNIT_ASSERT(range.containsUtcHour(22));
        UNIT_ASSERT(range.containsUtcHour(23));
        UNIT_ASSERT(!range.containsUtcHour(0));
        UNIT_ASSERT(!range.containsUtcHour(10));

        range = HourRange(22, 3, 4); // 01:00:00 - 02:59:59 in UTC+3;
        UNIT_ASSERT(!range.containsUtcHour(17));
        UNIT_ASSERT(range.containsUtcHour(18));
        UNIT_ASSERT(range.containsUtcHour(19));
        UNIT_ASSERT(!range.containsUtcHour(0));
        UNIT_ASSERT(!range.containsUtcHour(10));

        range = HourRange(0, 23); // 00:00:00 - 23:59:59 in UTC;
        UNIT_ASSERT(range.containsUtcHour(0));
        UNIT_ASSERT(range.containsUtcHour(5));
        UNIT_ASSERT(range.containsUtcHour(12));
        UNIT_ASSERT(range.containsUtcHour(23));
    }
}

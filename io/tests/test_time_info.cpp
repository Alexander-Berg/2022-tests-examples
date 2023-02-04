#include <yandex_io/services/do_not_disturb/time_info.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;

void checkParseSuccess(const std::string& input, int exp_hours, int exp_minutes, int exp_seconds,
                       bool hasTimezone = false, int exp_timeZoneOffsetSeconds = 0) {
    const std::optional<TimeInfo> actual = TimeInfo::parse(input);
    UNIT_ASSERT(actual.has_value());
    UNIT_ASSERT_VALUES_EQUAL(exp_hours, actual->hours_);
    UNIT_ASSERT_VALUES_EQUAL(exp_minutes, actual->minutes_);
    UNIT_ASSERT_VALUES_EQUAL(exp_seconds, actual->seconds_);
    if (hasTimezone) {
        UNIT_ASSERT_VALUES_EQUAL(exp_timeZoneOffsetSeconds, actual->timezoneOffsetSec_);
    }
}

void checkParseFail(const std::string& input) {
    const std::optional<TimeInfo> actual = TimeInfo::parse(input);
    UNIT_ASSERT(!actual.has_value());
}

Y_UNIT_TEST_SUITE(testTimeInfo) {
    Y_UNIT_TEST(testToString) {
        UNIT_ASSERT_VALUES_EQUAL(TimeInfo().to_string(), "00:00:00");
        UNIT_ASSERT_VALUES_EQUAL(TimeInfo(1, 0, 0).to_string(), "01:00:00");
        UNIT_ASSERT_VALUES_EQUAL(TimeInfo(0, 1, 0).to_string(), "00:01:00");
        UNIT_ASSERT_VALUES_EQUAL(TimeInfo(0, 0, 1).to_string(), "00:00:01");
        UNIT_ASSERT_VALUES_EQUAL(TimeInfo(24, 0, 0).to_string(), "24:00:00");
        UNIT_ASSERT_VALUES_EQUAL(TimeInfo(0, 59, 0).to_string(), "00:59:00");
        UNIT_ASSERT_VALUES_EQUAL(TimeInfo(0, 0, 59).to_string(), "00:00:59");

        UNIT_ASSERT_VALUES_EQUAL(TimeInfo(0, 0, 59, true, 3 * 60 * 60).to_string(), "00:00:59+03:00");
        UNIT_ASSERT_VALUES_EQUAL(TimeInfo(0, 0, 59, true, -3 * 60 * 60).to_string(), "00:00:59-03:00");
    }

    Y_UNIT_TEST(testParsing) {
        checkParseSuccess("00:00", 0, 0, 0);

        checkParseSuccess("00:05", 0, 5, 0);
        checkParseSuccess("05:00", 5, 0, 0);
        checkParseSuccess("05:05", 5, 5, 0);

        checkParseSuccess("12:40", 12, 40, 0);
        checkParseSuccess("23:15", 23, 15, 0);

        checkParseSuccess("00:00:00", 0, 0, 0);
        checkParseSuccess("00:00:20", 0, 0, 20);
        checkParseSuccess("00:00:02", 0, 0, 2);
        checkParseSuccess("00:00:22", 0, 0, 22);

        checkParseSuccess("1:1", 1, 1, 0);
        checkParseSuccess("01:001", 1, 1, 0);
        checkParseSuccess("001:01", 1, 1, 0);
        checkParseSuccess("00:00:00001", 0, 0, 1);

        checkParseSuccess("00:00Something", 0, 0, 0);
        checkParseSuccess("00:00:00Something", 0, 0, 0);

        checkParseSuccess("00:00:20+03:00", 0, 0, 20, true, 3 * 60 * 60);
        checkParseSuccess("00:00:20+0300", 0, 0, 20, true, 3 * 60 * 60);
        checkParseSuccess("00:00:20-03:00", 0, 0, 20, true, -3 * 60 * 60);
        checkParseSuccess("00:00:20-0300", 0, 0, 20, true, -3 * 60 * 60);
        checkParseSuccess("00:00:20+03", 0, 0, 20, true, 3 * 60 * 60);
        checkParseSuccess("00:00+03", 0, 0, 0, true, 3 * 60 * 60);
        checkParseSuccess("00:00-03", 0, 0, 0, true, -3 * 60 * 60);

        checkParseFail("000");
        checkParseFail("00.00");
        checkParseFail("00000");
        checkParseFail("0x10:0x20");
        checkParseFail("NULL");

        checkParseFail("AA:00:00");
        checkParseFail("00:AA:00");
        checkParseFail("00:00:AA");
        checkParseFail("00A00:00");

        checkParseFail("25:00:00");
        checkParseFail("00:60:00");
        checkParseFail("00:00:60");

        checkParseFail("-1:00:00");
        checkParseFail("00:-1:00");
        checkParseFail("00:00:-1");
    }

    Y_UNIT_TEST(testTimezones) {
        UNIT_ASSERT(TimeInfo::parse("22:00:20+03:00").value() == TimeInfo::parse("00:00:20+05:00").value());
        UNIT_ASSERT(TimeInfo::parse("00:00:20+05:00").value() == TimeInfo::parse("22:00:20+03:00").value());

        UNIT_ASSERT(TimeInfo::parse("00:00:00-12:00").value() == TimeInfo::parse("02:00:00+14:00").value());
        UNIT_ASSERT(TimeInfo::parse("02:00:00+14:00").value() == TimeInfo::parse("00:00:00-12:00").value());

        UNIT_ASSERT(TimeInfo::parse("02:00:00+05:00").value() == TimeInfo::parse("02:00:00+05:00").value());
        UNIT_ASSERT(TimeInfo::parse("02:00:00+05:00").value() != TimeInfo::parse("02:00:00+03:00").value());

        UNIT_ASSERT(TimeInfo::parse("22:00:20+03:00").value() < TimeInfo::parse("02:00:20+06:00").value());
        UNIT_ASSERT(TimeInfo::parse("02:00:20+06:00").value() > TimeInfo::parse("22:00:20+03:00").value());

        UNIT_ASSERT(TimeInfo::parse("22:00:20-12:00").value() < TimeInfo::parse("01:00:20+14:00").value());
        UNIT_ASSERT(TimeInfo::parse("23:30:20-12:00").value() > TimeInfo::parse("00:00:00+14:00").value());
    }

    Y_UNIT_TEST(testCalculateNextTimeDeltaSeconds) {
        // current time > next time
        UNIT_ASSERT(TimeInfo::parse("22:00:00+05:00").value().calculateNextTimeDeltaSeconds(TimeInfo::parse("10:00:00+05:00").value()) == 12 * 60 * 60);
        UNIT_ASSERT(TimeInfo::parse("14:00:00+05:00").value().calculateNextTimeDeltaSeconds(TimeInfo::parse("10:00:00+05:00").value()) == 20 * 60 * 60);

        UNIT_ASSERT(TimeInfo::parse("20:00:00+05:00").value().calculateNextTimeDeltaSeconds(TimeInfo::parse("10:00:00+07:00").value()) == 12 * 60 * 60);
        UNIT_ASSERT(TimeInfo::parse("14:00:00+05:00").value().calculateNextTimeDeltaSeconds(TimeInfo::parse("07:00:00+07:00").value()) == 15 * 60 * 60);

        UNIT_ASSERT(TimeInfo::parse("22:00:00+07:00").value().calculateNextTimeDeltaSeconds(TimeInfo::parse("10:00:00+10:00").value()) == 9 * 60 * 60);
        UNIT_ASSERT(TimeInfo::parse("20:00:00+05:00").value().calculateNextTimeDeltaSeconds(TimeInfo::parse("10:00:00+11:00").value()) == 8 * 60 * 60);

        UNIT_ASSERT(TimeInfo::parse("02:00:00+05:00").value().calculateNextTimeDeltaSeconds(TimeInfo::parse("03:00:00+01:00").value()) == 5 * 60 * 60);
        UNIT_ASSERT(TimeInfo::parse("00:00:00+05:00").value().calculateNextTimeDeltaSeconds(TimeInfo::parse("03:00:00+02:00").value()) == 6 * 60 * 60);

        // current time < next time
        UNIT_ASSERT(TimeInfo::parse("02:00:00+05:00").value().calculateNextTimeDeltaSeconds(TimeInfo::parse("22:00:00+05:00").value()) == 20 * 60 * 60);
        UNIT_ASSERT(TimeInfo::parse("00:00:00+05:00").value().calculateNextTimeDeltaSeconds(TimeInfo::parse("22:00:00+05:00").value()) == 22 * 60 * 60);

        UNIT_ASSERT(TimeInfo::parse("10:00:00+10:00").value().calculateNextTimeDeltaSeconds(TimeInfo::parse("22:00:00+07:00").value()) == 15 * 60 * 60);

        UNIT_ASSERT(TimeInfo::parse("10:00:00+05:00").value().calculateNextTimeDeltaSeconds(TimeInfo::parse("20:00:00+07:00").value()) == 8 * 60 * 60);
        UNIT_ASSERT(TimeInfo::parse("07:00:00+05:00").value().calculateNextTimeDeltaSeconds(TimeInfo::parse("14:00:00+07:00").value()) == 5 * 60 * 60);

        UNIT_ASSERT(TimeInfo::parse("10:00:00+11:00").value().calculateNextTimeDeltaSeconds(TimeInfo::parse("20:00:00+05:00").value()) == 16 * 60 * 60);
    }

    Y_UNIT_TEST(testGetDeltaSeconds) {
        // current time > next time
        UNIT_ASSERT(TimeInfo::parse("22:00:00+05:00").value().getDeltaSeconds(TimeInfo::parse("10:00:00+05:00").value()) == 12 * 60 * 60);
        UNIT_ASSERT(TimeInfo::parse("14:00:00+05:00").value().getDeltaSeconds(TimeInfo::parse("10:00:00+05:00").value()) == 4 * 60 * 60);
        UNIT_ASSERT(TimeInfo::parse("20:00:00+05:00").value().getDeltaSeconds(TimeInfo::parse("10:00:00+07:00").value()) == 12 * 60 * 60);
        UNIT_ASSERT(TimeInfo::parse("14:00:00+05:00").value().getDeltaSeconds(TimeInfo::parse("07:00:00+07:00").value()) == 9 * 60 * 60);
        UNIT_ASSERT(TimeInfo::parse("22:00:00+07:00").value().getDeltaSeconds(TimeInfo::parse("10:00:00+10:00").value()) == 15 * 60 * 60);
        UNIT_ASSERT(TimeInfo::parse("02:00:00+05:00").value().getDeltaSeconds(TimeInfo::parse("03:00:00+01:00").value()) == 19 * 60 * 60);
        UNIT_ASSERT(TimeInfo::parse("00:00:00+05:00").value().getDeltaSeconds(TimeInfo::parse("03:00:00+02:00").value()) == 18 * 60 * 60);
        UNIT_ASSERT(TimeInfo::parse("10:00:00+11:00").value().getDeltaSeconds(TimeInfo::parse("20:00:00+05:00").value()) == 8 * 60 * 60);
        UNIT_ASSERT(TimeInfo::parse("02:00:00+05:00").value().getDeltaSeconds(TimeInfo::parse("22:00:00+05:00").value()) == 4 * 60 * 60);
        UNIT_ASSERT(TimeInfo::parse("00:00:00+05:00").value().getDeltaSeconds(TimeInfo::parse("22:00:00+05:00").value()) == 2 * 60 * 60);

        // current time < next time
        UNIT_ASSERT(TimeInfo::parse("20:00:00+05:00").value().getDeltaSeconds(TimeInfo::parse("10:00:00+11:00").value()) == -8 * 60 * 60);

        UNIT_ASSERT(TimeInfo::parse("10:00:00+10:00").value().getDeltaSeconds(TimeInfo::parse("22:00:00+07:00").value()) == -15 * 60 * 60);

        UNIT_ASSERT(TimeInfo::parse("10:00:00+05:00").value().getDeltaSeconds(TimeInfo::parse("20:00:00+07:00").value()) == -8 * 60 * 60);
        UNIT_ASSERT(TimeInfo::parse("07:00:00+05:00").value().getDeltaSeconds(TimeInfo::parse("14:00:00+07:00").value()) == -5 * 60 * 60);
    }

    Y_UNIT_TEST(testInsideIntervals) {
        UNIT_ASSERT(TimeInfo::parse("23:00:00+03:00").value().insideInterval(TimeInfo::parse("23:00:00+03:00").value(), TimeInfo::parse("09:00:00+03:00").value()));
        UNIT_ASSERT(TimeInfo::parse("23:00:01+03:00").value().insideInterval(TimeInfo::parse("23:00:00+03:00").value(), TimeInfo::parse("09:00:00+03:00").value()));
        UNIT_ASSERT(TimeInfo::parse("00:00:00+03:00").value().insideInterval(TimeInfo::parse("23:00:00+03:00").value(), TimeInfo::parse("09:00:00+03:00").value()));
        UNIT_ASSERT(TimeInfo::parse("08:59:59+03:00").value().insideInterval(TimeInfo::parse("23:00:00+03:00").value(), TimeInfo::parse("09:00:00+03:00").value()));
        UNIT_ASSERT(TimeInfo::parse("01:00:00+05:00").value().insideInterval(TimeInfo::parse("23:00:00+03:00").value(), TimeInfo::parse("09:00:00+03:00").value()));
        UNIT_ASSERT(TimeInfo::parse("10:59:59+05:00").value().insideInterval(TimeInfo::parse("23:00:00+03:00").value(), TimeInfo::parse("09:00:00+03:00").value()));
        UNIT_ASSERT(!TimeInfo::parse("22:59:59+03:00").value().insideInterval(TimeInfo::parse("23:00:00+03:00").value(), TimeInfo::parse("09:00:00+03:00").value()));
        UNIT_ASSERT(!TimeInfo::parse("09:00:00+03:00").value().insideInterval(TimeInfo::parse("23:00:00+03:00").value(), TimeInfo::parse("09:00:00+03:00").value()));
        UNIT_ASSERT(!TimeInfo::parse("09:00:01+03:00").value().insideInterval(TimeInfo::parse("23:00:00+03:00").value(), TimeInfo::parse("09:00:00+03:00").value()));
        UNIT_ASSERT(!TimeInfo::parse("11:00:00+05:00").value().insideInterval(TimeInfo::parse("23:00:00+03:00").value(), TimeInfo::parse("09:00:00+03:00").value()));
        UNIT_ASSERT(!TimeInfo::parse("00:59:59+05:00").value().insideInterval(TimeInfo::parse("23:00:00+03:00").value(), TimeInfo::parse("09:00:00+03:00").value()));

        UNIT_ASSERT(TimeInfo::parse("10:00:00+03:00").value().insideInterval(TimeInfo::parse("10:00:00+03:00").value(), TimeInfo::parse("21:00:00+03:00").value()));
        UNIT_ASSERT(TimeInfo::parse("10:00:01+03:00").value().insideInterval(TimeInfo::parse("10:00:00+03:00").value(), TimeInfo::parse("21:00:00+03:00").value()));
        UNIT_ASSERT(TimeInfo::parse("20:59:59+03:00").value().insideInterval(TimeInfo::parse("10:00:00+03:00").value(), TimeInfo::parse("21:00:00+03:00").value()));
        UNIT_ASSERT(TimeInfo::parse("12:00:00+05:00").value().insideInterval(TimeInfo::parse("10:00:00+03:00").value(), TimeInfo::parse("21:00:00+03:00").value()));
        UNIT_ASSERT(TimeInfo::parse("22:59:59+05:00").value().insideInterval(TimeInfo::parse("10:00:00+03:00").value(), TimeInfo::parse("21:00:00+03:00").value()));
        UNIT_ASSERT(!TimeInfo::parse("09:59:59+03:00").value().insideInterval(TimeInfo::parse("10:00:00+03:00").value(), TimeInfo::parse("21:00:00+03:00").value()));
        UNIT_ASSERT(!TimeInfo::parse("21:00:00+03:00").value().insideInterval(TimeInfo::parse("10:00:00+03:00").value(), TimeInfo::parse("21:00:00+03:00").value()));
        UNIT_ASSERT(!TimeInfo::parse("21:00:01+03:00").value().insideInterval(TimeInfo::parse("10:00:00+03:00").value(), TimeInfo::parse("21:00:00+03:00").value()));
        UNIT_ASSERT(!TimeInfo::parse("23:00:00+05:00").value().insideInterval(TimeInfo::parse("10:00:00+03:00").value(), TimeInfo::parse("21:00:00+03:00").value()));
        UNIT_ASSERT(!TimeInfo::parse("11:59:59+05:00").value().insideInterval(TimeInfo::parse("10:00:00+03:00").value(), TimeInfo::parse("21:00:00+03:00").value()));
    }
}

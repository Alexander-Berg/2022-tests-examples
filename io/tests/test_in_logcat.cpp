#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>
#include <yandex_io/libs/base/utils.h>

extern "C" {
#include <yandex_io/services/fluent-bitd/in_logcat/logprint.h>
}

#include <yandex_io/services/fluent-bitd/in_logcat/entities.h>
#include <fluent-bit/flb_input_plugin.h>

Y_UNIT_TEST_SUITE(FluentBitInLogcatTests) {
    Y_UNIT_TEST(testParseAndFormat) {
        auto dump = quasar::getFileContent(ArcadiaSourceRoot() + "/yandex_io/services/fluent-bitd/in_logcat/tests/data/logd.dump");
        struct log_msg* entry = reinterpret_cast<struct log_msg*>(dump.data());
        AndroidLogEntry parsed;
        flb_input_instance ins;
        ins.log_level = FLB_LOG_OFF;
        int result = parseLogEntry(entry, dump.length(), &parsed, &ins);
        UNIT_ASSERT_EQUAL(result, 0);
        UNIT_ASSERT_EQUAL(parsed.priority, ANDROID_LOG_INFO);
        UNIT_ASSERT_EQUAL(parsed.tv_sec, 1642083180);
        UNIT_ASSERT_EQUAL(parsed.tv_nsec, 650088835);
        UNIT_ASSERT_EQUAL(parsed.uid, 0);
        UNIT_ASSERT_EQUAL(parsed.pid, 3006);
        UNIT_ASSERT_EQUAL(parsed.tid, 3006);
        UNIT_ASSERT_STRINGS_EQUAL(parsed.tag, "vold");
        UNIT_ASSERT_EQUAL(parsed.tagLen, 5);
        UNIT_ASSERT_STRINGS_EQUAL(parsed.message, "Vold 3.0 (the awakening) firing up");
        UNIT_ASSERT_EQUAL(parsed.messageLen, 34);

        char buf[4096];
        size_t line_length;
        setenv("TZ", "Europe/Moscow", 1);
        tzset();
        char* line = formatLogLine(buf, sizeof(buf), &parsed, &line_length);
        UNIT_ASSERT_STRINGS_EQUAL(line, "01-13 17:13:00.650  3006  3006 I vold    : Vold 3.0 (the awakening) firing up");
    }

    Y_UNIT_TEST(testParseAndFormatWithBrokenLogEntry) {
        char dump[6] = "hello";
        log_msg* entry = reinterpret_cast<log_msg*>(dump);
        AndroidLogEntry parsed;
        flb_input_instance ins;
        ins.log_level = FLB_LOG_OFF;
        int result = parseLogEntry(entry, 6, &parsed, &ins);
        UNIT_ASSERT_EQUAL(result, -1);
    }

    Y_UNIT_TEST(testParseAndFormatWithEmptyLogEntry) {
        char dump[] = "";
        log_msg* entry = reinterpret_cast<log_msg*>(dump);
        AndroidLogEntry parsed;
        flb_input_instance ins;
        ins.log_level = FLB_LOG_OFF;
        int result = parseLogEntry(entry, 0, &parsed, &ins);
        UNIT_ASSERT_EQUAL(result, -1);
    }

    Y_UNIT_TEST(testParseAndFormatWithoutPermission) {
        auto dump = quasar::getFileContent(ArcadiaSourceRoot() + "/yandex_io/services/fluent-bitd/in_logcat/tests/data/logd-without-permission.dump");
        struct log_msg* entry = reinterpret_cast<struct log_msg*>(dump.data());
        AndroidLogEntry parsed;
        flb_input_instance ins;
        ins.log_level = FLB_LOG_OFF;
        int result = parseLogEntry(entry, dump.length(), &parsed, &ins);
        UNIT_ASSERT_EQUAL(result, 0);
        UNIT_ASSERT_EQUAL(parsed.priority, ANDROID_LOG_INFO);
        UNIT_ASSERT_EQUAL(parsed.tv_sec, 1642532909);
        UNIT_ASSERT_EQUAL(parsed.tv_nsec, 52703512);
        UNIT_ASSERT_EQUAL(parsed.uid, -1);
        UNIT_ASSERT_EQUAL(parsed.pid, 15920);
        UNIT_ASSERT_EQUAL(parsed.tid, 15920);
        UNIT_ASSERT_STRINGS_EQUAL(parsed.tag, "chatty");
        UNIT_ASSERT_EQUAL(parsed.tagLen, 7);
        UNIT_ASSERT_STRINGS_EQUAL(parsed.message, "uid=10064(com.yandex.io.sdk) expire 62 lines");
        UNIT_ASSERT_EQUAL(parsed.messageLen, 44);

        char buf[4096];
        size_t line_length;
        setenv("TZ", "Europe/Moscow", 1);
        tzset();
        char* line = formatLogLine(buf, sizeof(buf), &parsed, &line_length);
        UNIT_ASSERT_STRINGS_EQUAL(line, "01-18 22:08:29.052 15920 15920 I chatty  : uid=10064(com.yandex.io.sdk) expire 62 lines");
    }
}

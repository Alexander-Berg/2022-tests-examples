#include <maps/automotive/parking/fastcgi/parking_receiver/lib/useragent_filter.h>

#include <library/cpp/testing/unittest/registar.h>

#include <maps/infra/yacare/include/yacare.h>

#include <string>

namespace maps::automotive::parking::receiver::tests {

namespace  {
const std::string USER_AGENT_AFTERMARKET = "yandex.auto/1.4.3.1305 datasync/32.5.30 runtime/106.12.45 android/6.0.1 (faurecia-Coagent; t3-duster-yaAM; ru_RU)";
const std::string USER_AGENT_NON_AFTERMARKET = "yandex.auto/1.4.3.1305 datasync/32.5.30 runtime/106.12.45 android/6.0.1 (faurecia-Coagent; anything-else; ru_RU)";
const std::string USER_AGENT_NISSAN = "yandex.auto/1.7.2.10003 mapkit/198.45.19 runtime/193.17.7 android/4.4.2 (Freescale; V8AUTO-MX6Q; ru_RU)";
const std::string USER_AGENT_NISSAN_AVM = "yandex.auto/1.7.2.10003 (personal) android/4.4.2 (nissan; xtrail-avm (caska-imx6); ru_RU)";
const std::string USER_AGENT_UNSUPPORTED_OEM = "yandex.auto/1.4.5.2919 mapkit/134.4.36 runtime/147.2.4 android/4.4.2 (ECARX; XE1127H; ru_RU)";
const std::string USER_AGENT_BAD = "yandex.auto/1.4.3.1305 datasync/32.5.30 runtime/106.12.45 android/6.0.1 (bad user agent)";
}

Y_UNIT_TEST_SUITE(test_useragent_filter) {

    Y_UNIT_TEST(check_true_case_aftermarket)
    {
        UNIT_ASSERT(isSupportedHeadUnit(USER_AGENT_AFTERMARKET));
    }

    Y_UNIT_TEST(check_false_case_non_aftermarket)
    {
        UNIT_ASSERT(!isSupportedHeadUnit(USER_AGENT_NON_AFTERMARKET));
    }

    Y_UNIT_TEST(check_bad_user_agent_case)
    {
        UNIT_ASSERT_EXCEPTION(isSupportedHeadUnit(USER_AGENT_BAD), yacare::errors::BadRequest);
    }

    Y_UNIT_TEST(check_true_case_nissan_imx6)
    {
        UNIT_ASSERT(isSupportedHeadUnit(USER_AGENT_NISSAN));
    }

    Y_UNIT_TEST(check_true_case_nissan_avm)
    {
        UNIT_ASSERT(isSupportedHeadUnit(USER_AGENT_NISSAN_AVM));
    }

    Y_UNIT_TEST(check_false_case_unsupported_oem)
    {
        UNIT_ASSERT(!isSupportedHeadUnit(USER_AGENT_UNSUPPORTED_OEM));
    }

} //Y_UNIT_TEST_SUITE

} //maps::automotive::parking::tests

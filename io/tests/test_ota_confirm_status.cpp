#include <yandex_io/services/updatesd/ota_confirm_status.h>

#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <thread>

using namespace quasar;

Y_UNIT_TEST_SUITE_F(TestOtaConfirmStatus, QuasarUnitTestFixture) {
    Y_UNIT_TEST(testConfirm) {
        constexpr auto softTimeout = std::chrono::hours(10);
        constexpr auto hardTimeout = std::chrono::hours(10);
        OtaConfirmStatus confirmStatus(softTimeout, hardTimeout);

        std::thread t = std::thread([&]() {
            TestUtils::waitUntil([&]() {
                return confirmStatus.isWaitingOtaConfirm();
            });
            confirmStatus.confirm();
        });

        /* If ota confirmed in timeouts -> should return true, since we need to apply OTA */
        UNIT_ASSERT(confirmStatus.waitOtaConfirm());
        t.join();
    }

    Y_UNIT_TEST(testCancel) {
        constexpr auto softTimeout = std::chrono::hours(10);
        constexpr auto hardTimeout = std::chrono::hours(10);
        OtaConfirmStatus confirmStatus(softTimeout, hardTimeout);

        std::thread t = std::thread([&]() {
            TestUtils::waitUntil([&]() {
                return confirmStatus.isWaitingOtaConfirm();
            });
            confirmStatus.cancel();
        });

        /* If ota canceled in timeouts -> should return false, since we do not need to apply OTA */
        UNIT_ASSERT(!confirmStatus.waitOtaConfirm());
        t.join();
    }

    Y_UNIT_TEST(testSoftTimeout) {
        constexpr auto softTimeout = std::chrono::seconds(1);
        constexpr auto hardTimeout = std::chrono::seconds(2);
        OtaConfirmStatus confirmStatus(softTimeout, hardTimeout);
        /* Wait should timeout. If there wasn't confirm -> apply ota anyway */
        UNIT_ASSERT(confirmStatus.waitOtaConfirm());
    }

    Y_UNIT_TEST(testHardTimeout) {
        /* set up Soft deadline slightly less than hard deadline, so postpone busyloop will for sure
         * postpone soft deadline and waitOtaConfirm will ends by HardDeadline
         */
        constexpr auto softTimeout = std::chrono::seconds(9);
        constexpr auto hardTimeout = std::chrono::seconds(10);
        OtaConfirmStatus confirmStatus(softTimeout, hardTimeout);

        std::thread t = std::thread([&]() {
            TestUtils::waitUntil([&]() {
                return confirmStatus.isWaitingOtaConfirm();
            });
            /* postpone busy loop. Postpone until hard deadline */
            while (confirmStatus.isWaitingOtaConfirm()) {
                confirmStatus.postpone();
            }
        });

        /* Wait should timeout by HardDeadline, postpone busyloop should move soft deadline */
        UNIT_ASSERT(confirmStatus.waitOtaConfirm());

        t.join();
    }

    Y_UNIT_TEST(testPostponeConfirmRaw) {
        constexpr auto softTimeout = std::chrono::hours(1);
        constexpr auto hardTimeout = std::chrono::hours(1);
        OtaConfirmStatus confirmStatus(softTimeout, hardTimeout);
        std::thread t = std::thread([&]() {
            TestUtils::waitUntil([&]() {
                return confirmStatus.isWaitingOtaConfirm();
            });
            /* Check that even after postpone -> confirm will not be skipped */
            confirmStatus.postpone();
            confirmStatus.confirm();
        });

        /* Wait should timeout by HardDeadline, postpone busyloop should move soft deadline */
        UNIT_ASSERT(confirmStatus.waitOtaConfirm());

        t.join();
    }
}

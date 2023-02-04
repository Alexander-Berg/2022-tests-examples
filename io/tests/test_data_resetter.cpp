#include <yandex_io/modules/data_reset/data_reset_executor.h>
#include <yandex_io/modules/data_reset/data_resetter.h>
#include <yandex_io/modules/data_reset/callback_listener/callback_data_reset_state_listener.h>

#include <yandex_io/modules/device_state/extended_device_state.h>

#include <yandex_io/libs/threading/steady_condition_variable.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/null_sdk/null_sdk_interface.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;
using namespace YandexIO;

namespace {
    class TestableDataResetExecutor: public DataResetExecutor {
    public:
        void execute() override {
            std::scoped_lock guard(mutex_);
            executed_ = true;
            condVar_.notify_one();
        }

        void reset() {
            std::scoped_lock guard(mutex_);
            executed_ = false;
        }

        void wait() {
            std::unique_lock lock(mutex_);
            condVar_.wait(lock, [this]() {
                return executed_;
            });
        }

        bool wasExecuted() const {
            std::scoped_lock guard(mutex_);
            return executed_;
        }

    private:
        bool executed_{false};
        mutable std::mutex mutex_;
        SteadyConditionVariable condVar_;
    };
} // namespace

Y_UNIT_TEST_SUITE_F(TestDataResetter, QuasarUnitTestFixture) {
    Y_UNIT_TEST(TestScheduleConfirm) {
        auto dataResetExecutor = std::make_shared<TestableDataResetExecutor>();

        bool listenerRecievedWaitConfirm{false};
        bool listenerRecievedInProgress{false};
        std::condition_variable cv;
        std::mutex m;
        auto callbackListener = std::make_shared<CallbackDataResetStateListener>(
            [&](ExtendedDeviceState::DataResetState state) {
                std::scoped_lock guard(m);
                if (state == ExtendedDeviceState::DataResetState::DATA_RESET_WAIT_CONFIRM) {
                    listenerRecievedWaitConfirm = true;
                } else if (state == ExtendedDeviceState::DataResetState::DATA_RESET_IN_PROGRESS) {
                    listenerRecievedInProgress = true;
                }
                cv.notify_one();
            });

        auto cancelScheduleTime = std::chrono::seconds(0);
        auto confirmTimeout = std::chrono::seconds(100);
        DataResetter::DataResetterSettings settings("", "", "", cancelScheduleTime, confirmTimeout);
        DataResetter resetter(std::make_shared<NullSDKInterface>(), dataResetExecutor, settings);
        resetter.addListener(callbackListener);
        resetter.schedule();
        TestUtils::waitUntil([&]() {
            return resetter.isWaitingConfirm();
        });
        {
            std::unique_lock lk(m);
            cv.wait(lk, [&]() { return listenerRecievedWaitConfirm; });
        }
        UNIT_ASSERT(resetter.isWaitingConfirm());
        resetter.confirmDataReset();
        dataResetExecutor->wait();
        {
            std::unique_lock lk(m);
            cv.wait(lk, [&]() { return listenerRecievedInProgress; });
        }
    }

    Y_UNIT_TEST(TestDataRestNotConfirmed) {
        auto dataResetExecutor = std::make_shared<TestableDataResetExecutor>();

        bool listenerRecievedNull = false;
        std::condition_variable cv;
        std::mutex m;
        auto callbackListener = std::make_shared<CallbackDataResetStateListener>(
            [&](ExtendedDeviceState::DataResetState state) {
                if (state == ExtendedDeviceState::DataResetState::NONE) {
                    std::scoped_lock guard(m);
                    listenerRecievedNull = true;
                    cv.notify_one();
                }
            });

        auto cancelScheduleTime = std::chrono::seconds(0);
        auto confirmTimeout = std::chrono::seconds(1);
        DataResetter::DataResetterSettings settings("", "", "", cancelScheduleTime, confirmTimeout);
        DataResetter resetter(std::make_shared<NullSDKInterface>(), dataResetExecutor, settings);
        resetter.addListener(callbackListener);
        resetter.schedule();
        /* Wait until dataResetter will wait for confirm */
        TestUtils::waitUntil([&]() {
            return resetter.isWaitingConfirm();
        });
        /* Wait until dataResetter will cancel data reset after timeout */
        TestUtils::waitUntil([&]() {
            return !resetter.isWaitingConfirm();
        });
        {
            std::unique_lock lk(m);
            cv.wait(lk, [&]() { return listenerRecievedNull; });
        }
        /* Should not be executed without confirm */
        UNIT_ASSERT(!dataResetExecutor->wasExecuted());
    }

    Y_UNIT_TEST(TestCancelSchedule) {
        auto dataResetExecutor = std::make_shared<TestableDataResetExecutor>();

        auto cancelScheduleTime = std::chrono::seconds(100);
        auto confirmTimeout = std::chrono::seconds(100);
        DataResetter::DataResetterSettings settings("", "", "", cancelScheduleTime, confirmTimeout);
        DataResetter resetter(std::make_shared<NullSDKInterface>(), dataResetExecutor, settings);
        resetter.schedule();
        /* Wait until dataResetter will wait for confirm */
        TestUtils::waitUntil([&]() {
            return resetter.isScheduled();
        });
        resetter.cancelSchedule();
        /* Should be not scheduled after cancel */
        TestUtils::waitUntil([&]() {
            return !resetter.isScheduled();
        });
    }
}

#include <yandex_io/libs/threading/periodic_executor.h>

#include <yandex_io/libs/errno/errno_exception.h>

#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <atomic>
#include <chrono>
#include <future>
#include <memory>

#include <sys/time.h>

using namespace quasar;

namespace {
    const auto ETERNITY = std::chrono::milliseconds(1000000000);
} // namespace

Y_UNIT_TEST_SUITE_F(TestPeriodicExecutor, QuasarUnitTestFixture) {
    Y_UNIT_TEST(testPeriodicExecutor)
    {
        std::atomic_int counter = 0;
        auto incrementIt = [&]() {
            ++counter;
        };

        PeriodicExecutor executor(incrementIt, std::chrono::milliseconds(200), PeriodicExecutor::PeriodicType::CALLBACK_FIRST);

        auto checkCounter = [&](int expectation) {
            return [&counter, expectation]() {
                return counter == expectation;
            };
        };
        TestUtils::waitUntil(checkCounter(1));
        executor.executeNow();
        TestUtils::waitUntil(checkCounter(2));
        TestUtils::waitUntil(checkCounter(3));
        TestUtils::waitUntil(checkCounter(4));
        executor.setPeriodTime(std::chrono::milliseconds(1000)); /* Change next schedule period*/
        TestUtils::waitUntil(checkCounter(5));
        std::this_thread::sleep_for(std::chrono::milliseconds(250));
        UNIT_ASSERT_EQUAL(counter, 5); /* Shouldn't change */
        TestUtils::waitUntil(checkCounter(6));
        UNIT_ASSERT(true);
    }

    Y_UNIT_TEST(testPeriodicExecutorOneshot)
    {
        int counter = 0;
        auto incrementIt = [&]() {
            ++counter;
        };

        PeriodicExecutor executor(incrementIt, std::chrono::milliseconds(100), PeriodicExecutor::PeriodicType::ONE_SHOT);

        std::this_thread::sleep_for(std::chrono::milliseconds(25));
        UNIT_ASSERT_EQUAL(counter, 0);

        std::this_thread::sleep_for(std::chrono::milliseconds(125));
        UNIT_ASSERT_EQUAL(counter, 1);

        std::this_thread::sleep_for(std::chrono::milliseconds(250));
        UNIT_ASSERT_EQUAL(counter, 1);
    }

    Y_UNIT_TEST(testPeriodicExecutorDestructorDoesntHang)
    {
        auto doSleep = [&]() {
            std::this_thread::sleep_for(std::chrono::milliseconds(100));
        };

        PeriodicExecutor executor(doSleep, ETERNITY);
        std::this_thread::sleep_for(std::chrono::milliseconds(50));

        UNIT_ASSERT(true);
    }

    Y_UNIT_TEST(testPeriodicExecutorSleepFirstDestructorDoesntHang)
    {
        auto doSleep = [&]() {
            std::this_thread::sleep_for(std::chrono::milliseconds(100));
        };

        std::unique_ptr<PeriodicExecutor> executor(new PeriodicExecutor(doSleep, ETERNITY, PeriodicExecutor::PeriodicType::SLEEP_FIRST));
        std::this_thread::sleep_for(std::chrono::milliseconds(50));
        executor.reset(nullptr);
        UNIT_ASSERT(true);
    }

    Y_UNIT_TEST(testPeriodicExecutorSleepFirstSkipCallbackInDestructor)
    {
        auto throwCallback = [&]() {
            throw std::runtime_error("Callback should not be called");
        };

        std::unique_ptr<PeriodicExecutor> executor(new PeriodicExecutor(throwCallback, ETERNITY, PeriodicExecutor::PeriodicType::SLEEP_FIRST));
        std::this_thread::sleep_for(std::chrono::milliseconds(50));
        executor.reset(nullptr);
        UNIT_ASSERT(true);
    }

    // This test is added because std::condition_variable is not steady when time is changing.
    // Even when using the steady_clock.
    // For reproducing the issue disable automatic time adjustment and run test as root user
    /*
Y_UNIT_TEST(testPeriodicExecutorTimeChange)
{
    int counter = 0;
    auto incrementIt = [&]() {
        ++counter;
    };

    struct timeval tv;
    gettimeofday(&tv, nullptr);
    PeriodicExecutor executor(incrementIt, std::chrono::seconds(10), PeriodicExecutor::PeriodicType::ONE_SHOT);
    std::this_thread::sleep_for(std::chrono::seconds(5));
    tv.tv_sec -= 3600;
    if (settimeofday(&tv, nullptr) != 0)
        throw ErrnoException(errno, "Cannot set time");
    std::this_thread::sleep_for(std::chrono::seconds(11));
    UNIT_ASSERT_EQUAL(counter, 1);
}
*/

    Y_UNIT_TEST(testPeriodicExecutorCallbackFirst)
    {
        std::promise<void> callbackDonePromise;
        auto callback = [&]() {
            callbackDonePromise.set_value();
        };
        PeriodicExecutor executor(callback, ETERNITY, PeriodicExecutor::PeriodicType::CALLBACK_FIRST);
        callbackDonePromise.get_future().get();
        UNIT_ASSERT(true);
    }

    Y_UNIT_TEST(testPeriodicExecutorSleepFirst)
    {
        std::promise<void> callbackDonePromise;
        auto callback = [&]() {
            try {
                callbackDonePromise.set_value();
            } catch (const std::future_error& e) {
                /* skip "Promise already satisfied" error */
            }
        };
        PeriodicExecutor executor(callback, std::chrono::seconds(5), PeriodicExecutor::PeriodicType::SLEEP_FIRST);

        /* Check that callback isn't called immediately */
        auto callbackDoneFuture = callbackDonePromise.get_future();
        std::future_status status = callbackDoneFuture.wait_for(std::chrono::seconds(1));
        UNIT_ASSERT(status == std::future_status::timeout);

        /* Make sure that callback is called */
        status = callbackDoneFuture.wait_for(std::chrono::seconds(10));
        UNIT_ASSERT(status == std::future_status::ready);
    }

    Y_UNIT_TEST(testPeriodicExecutorPassThisToCallback)
    {
        std::promise<PeriodicExecutor*> pointerPromise;
        auto callback = [&](PeriodicExecutor* pointer) {
            pointerPromise.set_value(pointer);
        };
        PeriodicExecutor executor(PeriodicExecutor::PECallback(callback), ETERNITY, PeriodicExecutor::PeriodicType::CALLBACK_FIRST);
        const auto pointer = pointerPromise.get_future().get();
        /* Check that pointer passed to callback equals to pointer to PeriodicExecutor */
        UNIT_ASSERT_EQUAL(pointer, &executor);
    }
}

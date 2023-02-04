#include <yandex_io/libs/threading/steady_condition_variable.h>

#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <atomic>
#include <future>
#include <thread>

using namespace quasar;

Y_UNIT_TEST_SUITE_F(TestSteadyConditionVariable, QuasarUnitTestFixture) {
    Y_UNIT_TEST(testSteadyConditionVariableWaitUntil)
    {
        SteadyConditionVariable v;
        std::mutex m;
        bool cond = false;

        std::promise<bool> finished;

        std::thread t1([&]() {
            std::unique_lock lock(m);
            finished.set_value(v.wait_until(lock, std::chrono::steady_clock::now() + std::chrono::seconds(10), [&]() { return cond; }));
        });

        auto future = finished.get_future();
        const auto futureWaitRet = future.wait_for(std::chrono::milliseconds(100));
        if (futureWaitRet != std::future_status::timeout) {
            UNIT_FAIL("Future result was set up before cond var notifying thread even started");
        }
        std::thread t2([&]()
                       {
                           std::unique_lock lock(m);
                           cond = true;
                           v.notify_one();
                       });

        UNIT_ASSERT(future.wait_for(std::chrono::seconds(5)) == std::future_status::ready);
        UNIT_ASSERT(future.get());

        t1.join();
        t2.join();
    }

    Y_UNIT_TEST(testSteadyConditionVariableWaitFor)
    {
        SteadyConditionVariable v;
        std::mutex m;
        bool cond = false;

        std::promise<bool> finished;

        std::thread t1([&]() {
            std::unique_lock lock(m);
            finished.set_value(v.wait_for(lock, std::chrono::seconds(10), [&]() { return cond; }));
        });

        auto future = finished.get_future();
        const auto futureWaitRet = future.wait_for(std::chrono::milliseconds(100));
        if (futureWaitRet != std::future_status::timeout) {
            UNIT_FAIL("Future result was set up before cond var notifying thread even started");
        }

        std::thread t2([&]()
                       {
                           std::unique_lock lock(m);
                           cond = true;
                           v.notify_one();
                       });

        UNIT_ASSERT(future.wait_for(std::chrono::seconds(5)) == std::future_status::ready);
        UNIT_ASSERT(future.get());

        t1.join();
        t2.join();
    }

    Y_UNIT_TEST(testSteadyConditionVariableTimeout)
    {
        SteadyConditionVariable v;
        std::mutex m;
        std::unique_lock lock(m);
        auto status = v.wait_until(lock, std::chrono::steady_clock::now() + std::chrono::milliseconds(100));
        UNIT_ASSERT(status == std::cv_status::timeout);
        status = v.wait_for(lock, std::chrono::milliseconds(100));
        UNIT_ASSERT(status == std::cv_status::timeout);
    }

    Y_UNIT_TEST(testSteadyConditionVariableNotifyAll)
    {
        SteadyConditionVariable v;
        std::mutex m;
        bool cond = false;

        std::promise<bool> finished1;
        std::promise<bool> finished2;

        std::thread t1([&]() {
            std::unique_lock lock(m);
            finished1.set_value(v.wait_until(lock, std::chrono::steady_clock::now() + std::chrono::seconds(10), [&]() { return cond; }));
        });

        std::thread t2([&]() {
            std::unique_lock lock(m);
            finished2.set_value(v.wait_until(lock, std::chrono::steady_clock::now() + std::chrono::seconds(10), [&]() { return cond; }));
        });

        auto future1 = finished1.get_future();
        const auto futureWaitRet1 = future1.wait_for(std::chrono::milliseconds(100));
        if (futureWaitRet1 != std::future_status::timeout) {
            UNIT_FAIL("Future1 result was set up before cond var notifying thread even started");
        }

        auto future2 = finished2.get_future();
        const auto futureWaitRet2 = future2.wait_for(std::chrono::milliseconds(100));
        if (futureWaitRet2 != std::future_status::timeout) {
            UNIT_FAIL("Future2 result was set up before cond var notifying thread even started");
        }

        std::thread t3([&]() {
            std::unique_lock lock(m);
            cond = true;
            v.notify_all();
        });

        UNIT_ASSERT(future1.wait_for(std::chrono::seconds(5)) == std::future_status::ready);
        UNIT_ASSERT(future1.get());

        UNIT_ASSERT(future2.wait_for(std::chrono::seconds(5)) == std::future_status::ready);
        UNIT_ASSERT(future2.get());

        t1.join();
        t2.join();
        t3.join();
    }

    Y_UNIT_TEST(testSteadyConditionVariableWaitPred)
    {
        SteadyConditionVariable v;
        std::mutex m;
        bool cond = false;
        std::atomic_int wakeUpCounter{0};

        std::promise<bool> finished;

        std::thread t1([&]() {
            std::unique_lock lock(m);
            v.wait(lock, [&cond, &wakeUpCounter]() {
                ++wakeUpCounter;
                return cond;
            });
            finished.set_value(cond);
        });

        auto future = finished.get_future();
        const auto futureWaitRet = future.wait_for(std::chrono::milliseconds(100));
        if (futureWaitRet != std::future_status::timeout) {
            UNIT_FAIL("Future result was set up before cond var notifying thread even started");
        }

        std::thread t2([&]()
                       {
                           std::unique_lock lock(m);
                           cond = false;
                           v.notify_one();
                           lock.unlock();

                           TestUtils::doUntil([&wakeUpCounter]() { return wakeUpCounter != 0; }, 10 * 1000);

                           lock.lock();
                           cond = true;
                           v.notify_one();
                       });

        UNIT_ASSERT(future.get());
        UNIT_ASSERT_GE(wakeUpCounter, 2); // cond var should wake-up at least twice
        t1.join();
        t2.join();
    }

    Y_UNIT_TEST(testSteadyConditionVariableWaitUntilNow)
    {
        SteadyConditionVariable v;
        std::mutex m;
        std::unique_lock lock(m);
        UNIT_ASSERT_VALUES_EQUAL(int(v.wait_until(lock, std::chrono::steady_clock::now())), int(std::cv_status::timeout));
    }

    Y_UNIT_TEST(testSteadyConditionVariableWaitUntilBackward)
    {
        SteadyConditionVariable v;
        std::mutex m;
        std::unique_lock lock(m);
        UNIT_ASSERT_VALUES_EQUAL(int(v.wait_until(lock, std::chrono::steady_clock::now() - std::chrono::milliseconds(999))),
                                 int(std::cv_status::timeout));
    }
}

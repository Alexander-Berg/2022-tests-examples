#include <yandex_io/libs/threading/callback_queue.h>
#include <yandex_io/libs/threading/lifetime.h>
#include <yandex_io/libs/threading/timer_service.h>

#include <yandex_io/libs/logging/logging.h>

#include <yandex_io/tests/testlib/test_utils.h>

#include <library/cpp/testing/unittest/registar.h>

#include <atomic>
#include <cstdlib>

using namespace quasar;
using namespace quasar::TestUtils;

namespace {
    constexpr std::chrono::milliseconds maxTimeoutError{300};
} // namespace

#define FUZZY_TIME_POINT_COMPARE(tp, exp) \
    UNIT_ASSERT((tp) >= (exp));           \
    UNIT_ASSERT((tp) < (exp) + maxTimeoutError);

Y_UNIT_TEST_SUITE(TimerService) {
    Y_UNIT_TEST(testCtor)
    {
        UNIT_ASSERT_NO_EXCEPTION(TimerService{});
    }

    Y_UNIT_TEST(testSimple)
    {
        Lifetime lifetime;
        auto callbackQueue = std::make_shared<CallbackQueue>();

        std::atomic<bool> ready{false};

        TimerService ts;
        auto timeout = std::chrono::seconds{1};
        auto now0 = std::chrono::steady_clock::now();
        auto task = ts.createDelayedTask(timeout,
                                         [&]() {
                                             UNIT_ASSERT(callbackQueue->isWorkingThread());
                                             ready = true;
                                         }, lifetime, callbackQueue);
        UNIT_ASSERT(task);
        doUntil([&]() { return ready.load(); }, 5 * 1000);

        auto now1 = std::chrono::steady_clock::now();
        UNIT_ASSERT(now1 >= now0 + timeout);
        UNIT_ASSERT(now1 < now0 + timeout + maxTimeoutError);
    }

    Y_UNIT_TEST(testSimpleMulti)
    {
        Lifetime lifetime;
        auto callbackQueue = std::make_shared<CallbackQueue>();

        std::vector<std::pair<int, std::chrono::steady_clock::time_point>> stones;
        std::atomic<bool> ready{false};

        TimerService ts;
        auto timeout = std::chrono::seconds{1};
        auto now0 = std::chrono::steady_clock::now();

        std::atomic<bool> timer1{false};
        ts.createDelayedTask(1 * timeout,
                             [&]() {
                                 UNIT_ASSERT(timer1.load() == false);
                                 timer1 = true;
                                 UNIT_ASSERT(callbackQueue->isWorkingThread());
                                 stones.push_back(std::make_pair(1, std::chrono::steady_clock::now()));
                             }, lifetime, callbackQueue);

        std::atomic<bool> timer2{false};
        ts.createDelayedTask(2 * timeout,
                             [&]() {
                                 UNIT_ASSERT(timer2.load() == false);
                                 timer2 = true;
                                 UNIT_ASSERT(callbackQueue->isWorkingThread());
                                 stones.push_back(std::make_pair(2, std::chrono::steady_clock::now()));
                             }, lifetime, callbackQueue);

        std::atomic<bool> timer3{false};
        ts.createDelayedTask(3 * timeout,
                             [&]() {
                                 UNIT_ASSERT(timer3.load() == false);
                                 timer3 = true;
                                 UNIT_ASSERT(callbackQueue->isWorkingThread());
                                 stones.push_back(std::make_pair(3, std::chrono::steady_clock::now()));
                                 ready = true;
                             }, lifetime, callbackQueue);
        doUntil([&]() { return ready.load(); }, 6 * 1000);

        auto nowX = std::chrono::steady_clock::now();
        UNIT_ASSERT(nowX >= now0 + 3 * timeout);

        UNIT_ASSERT_VALUES_EQUAL(stones.size(), 3);
        UNIT_ASSERT_VALUES_EQUAL(stones[0].first, 1);
        UNIT_ASSERT_VALUES_EQUAL(stones[1].first, 2);
        UNIT_ASSERT_VALUES_EQUAL(stones[2].first, 3);
        UNIT_ASSERT(stones[0].second >= now0 + 1 * timeout);
        UNIT_ASSERT(stones[0].second < now0 + 1 * timeout + maxTimeoutError);
        UNIT_ASSERT(stones[1].second >= now0 + 2 * timeout);
        UNIT_ASSERT(stones[1].second < now0 + 2 * timeout + maxTimeoutError);
        UNIT_ASSERT(stones[2].second >= now0 + 3 * timeout);
        UNIT_ASSERT(stones[2].second < now0 + 3 * timeout + maxTimeoutError);
    }

    Y_UNIT_TEST(testSimpleMultiReverse)
    {
        Lifetime lifetime;
        auto callbackQueue = std::make_shared<CallbackQueue>();

        std::vector<std::pair<int, std::chrono::steady_clock::time_point>> stones;
        std::atomic<bool> ready{false};

        TimerService ts;
        auto timeout = std::chrono::seconds{1};
        auto now0 = std::chrono::steady_clock::now();

        /* Second before First */
        std::atomic<bool> timer2{false};
        ts.createDelayedTask(2 * timeout,
                             [&]() {
                                 UNIT_ASSERT(timer2.load() == false);
                                 timer2 = true;
                                 UNIT_ASSERT(callbackQueue->isWorkingThread());
                                 stones.push_back(std::make_pair(2, std::chrono::steady_clock::now()));
                                 ready = true;
                             }, lifetime, callbackQueue);

        /* First after Second */
        std::atomic<bool> timer1{false};
        ts.createDelayedTask(1 * timeout,
                             [&]() {
                                 UNIT_ASSERT(timer1.load() == false);
                                 timer1 = true;
                                 UNIT_ASSERT(callbackQueue->isWorkingThread());
                                 stones.push_back(std::make_pair(1, std::chrono::steady_clock::now()));
                             }, lifetime, callbackQueue);

        doUntil([&]() { return ready.load(); }, 6 * 1000);

        auto nowX = std::chrono::steady_clock::now();
        UNIT_ASSERT(nowX >= now0 + 2 * timeout);

        UNIT_ASSERT_VALUES_EQUAL(stones.size(), 2);
        UNIT_ASSERT_VALUES_EQUAL(stones[0].first, 1);
        UNIT_ASSERT_VALUES_EQUAL(stones[1].first, 2);
        FUZZY_TIME_POINT_COMPARE(stones[0].second, now0 + 1 * timeout);
        FUZZY_TIME_POINT_COMPARE(stones[1].second, now0 + 2 * timeout);
    }

    Y_UNIT_TEST(testPeriodic)
    {
        Lifetime lifetime;
        auto callbackQueue = std::make_shared<CallbackQueue>();

        std::vector<std::pair<int, std::chrono::steady_clock::time_point>> stones;

        TimerService ts;
        auto timeout1 = std::chrono::milliseconds{300};
        auto timeout2 = std::chrono::milliseconds{700};
        auto now0 = std::chrono::steady_clock::now();

        constexpr int expectedCounter1 = 5;
        std::atomic<int> counter1{0};
        ts.createPeriodicTask(timeout1,
                              [&]() {
                                  UNIT_ASSERT(callbackQueue->isWorkingThread());
                                  stones.push_back(std::make_pair(1, std::chrono::steady_clock::now()));
                                  ++counter1;
                              }, lifetime, callbackQueue);

        std::atomic<int> counter2{0};
        ts.createPeriodicTask(timeout2,
                              [&]() {
                                  UNIT_ASSERT(callbackQueue->isWorkingThread());
                                  stones.push_back(std::make_pair(2, std::chrono::steady_clock::now()));
                                  ++counter2;
                              }, lifetime, callbackQueue);

        doUntil([&]() { return counter1.load() == expectedCounter1; }, 6 * 1000);

        auto nowX = std::chrono::steady_clock::now();
        UNIT_ASSERT(nowX >= now0 + expectedCounter1 * timeout1);

        UNIT_ASSERT_VALUES_EQUAL(stones.size(), 7);
        UNIT_ASSERT_VALUES_EQUAL(stones[0].first, 1);
        UNIT_ASSERT_VALUES_EQUAL(stones[1].first, 1);
        UNIT_ASSERT_VALUES_EQUAL(stones[2].first, 2);
        UNIT_ASSERT_VALUES_EQUAL(stones[3].first, 1);
        UNIT_ASSERT_VALUES_EQUAL(stones[4].first, 1);
        UNIT_ASSERT_VALUES_EQUAL(stones[5].first, 2);
        UNIT_ASSERT_VALUES_EQUAL(stones[6].first, 1);
        FUZZY_TIME_POINT_COMPARE(stones[0].second, now0 + 1 * timeout1);
        FUZZY_TIME_POINT_COMPARE(stones[1].second, now0 + 2 * timeout1);
        FUZZY_TIME_POINT_COMPARE(stones[2].second, now0 + 1 * timeout2);
        FUZZY_TIME_POINT_COMPARE(stones[3].second, now0 + 3 * timeout1);
        FUZZY_TIME_POINT_COMPARE(stones[4].second, now0 + 4 * timeout1);
        FUZZY_TIME_POINT_COMPARE(stones[5].second, now0 + 2 * timeout2);
        FUZZY_TIME_POINT_COMPARE(stones[6].second, now0 + 5 * timeout1);
    }

    Y_UNIT_TEST(testSimpleExpired)
    {
        Lifetime lifetime;
        auto callbackQueue = std::make_shared<CallbackQueue>();

        std::atomic<bool> ready{false};

        TimerService ts;
        auto timeout = std::chrono::seconds{1};
        auto task = ts.createDelayedTask(timeout,
                                         [&]() {
                                             UNIT_ASSERT(callbackQueue->isWorkingThread());
                                             ready = true;
                                         }, lifetime, callbackQueue);
        UNIT_ASSERT(task);
        UNIT_ASSERT_VALUES_EQUAL(task->expired(), false);
        doUntil([&]() { return ready.load(); }, 2 * std::chrono::duration_cast<std::chrono::milliseconds>(timeout).count());

        UNIT_ASSERT(ready.load());
        UNIT_ASSERT_VALUES_EQUAL(task->expired(), true);
    }

    Y_UNIT_TEST(testSimpleStop)
    {
        Lifetime lifetime;
        auto callbackQueue = std::make_shared<CallbackQueue>();

        std::atomic<bool> ready{false};

        TimerService ts;
        auto timeout = std::chrono::seconds{1};
        auto task = ts.createDelayedTask(timeout,
                                         [&]() {
                                             UNIT_ASSERT(callbackQueue->isWorkingThread());
                                             ready = true;
                                         }, lifetime, callbackQueue);
        UNIT_ASSERT(task);
        auto stop1 = task->stop();
        UNIT_ASSERT_VALUES_EQUAL(stop1, true);
        doUntil([&]() { return ready.load(); }, 2 * std::chrono::duration_cast<std::chrono::milliseconds>(timeout).count());

        UNIT_ASSERT(ready.load() == false);
        auto stop2 = task->stop();
        UNIT_ASSERT_VALUES_EQUAL(stop2, false);
    }

    Y_UNIT_TEST(testSimpleRestart)
    {
        Lifetime lifetime;
        auto callbackQueue = std::make_shared<CallbackQueue>();

        std::atomic<bool> ready{false};

        TimerService ts;
        auto timeout1 = std::chrono::seconds{1000000}; // never fire!
        auto timeout2 = std::chrono::seconds{1};
        auto task = ts.createPeriodicTask(timeout1,
                                          [&]() {
                                              UNIT_ASSERT(callbackQueue->isWorkingThread());
                                              ready = true;
                                          }, lifetime, callbackQueue);
        UNIT_ASSERT(task);
        std::this_thread::sleep_for(std::chrono::milliseconds{50}); // be sure to timer already started

        auto restart = task->restart(timeout2);
        UNIT_ASSERT_VALUES_EQUAL(restart, true);
        waitUntil([&]() { return ready.load(); });

        UNIT_ASSERT(ready.load());
        UNIT_ASSERT_VALUES_EQUAL(task->expired(), false);
        UNIT_ASSERT_VALUES_EQUAL(task->restart(timeout1), true);
    }

    Y_UNIT_TEST(testLifetimeCheck)
    {
        Lifetime lifetime;
        auto callbackQueue = std::make_shared<CallbackQueue>();

        std::atomic<bool> ready{false};

        TimerService ts;
        auto timeout = std::chrono::seconds{1};
        auto task = ts.createDelayedTask(timeout,
                                         [&]() {
                                             UNIT_ASSERT(callbackQueue->isWorkingThread());
                                             ready = true;
                                         }, lifetime, callbackQueue);
        UNIT_ASSERT(task);
        lifetime.die();
        UNIT_ASSERT(task->expired());
        doUntil([&]() { return ready.load(); }, 2 * 1000);

        UNIT_ASSERT(!ready.load());
    }

    Y_UNIT_TEST(testTaskHolder0)
    {
        TaskHolder<IDelayedTask> holder;
        UNIT_ASSERT(!holder);
    }

    Y_UNIT_TEST(testTaskHolder1)
    {
        Lifetime lifetime;
        auto callbackQueue = std::make_shared<CallbackQueue>();

        std::atomic<bool> ready{false};

        TimerService ts;
        auto timeout = std::chrono::seconds{10};
        auto task = ts.createDelayedTask(timeout,
                                         [&]() {
                                             UNIT_ASSERT(callbackQueue->isWorkingThread());
                                             ready = true;
                                         }, lifetime, callbackQueue);
        UNIT_ASSERT(task);
        UNIT_ASSERT(!task->expired());
        {
            TaskHolder<IDelayedTask> holder1(task);
            UNIT_ASSERT(holder1);
            UNIT_ASSERT(!!holder1);
            UNIT_ASSERT(!holder1->expired());
            {
                auto tmp(std::move(holder1));
                UNIT_ASSERT(!tmp->expired());
            }
            UNIT_ASSERT(!holder1); // NOLINT(bugprone-use-after-move)
        }
        UNIT_ASSERT(task->expired());
    }

    Y_UNIT_TEST(testFlood)
    {
        constexpr auto testTime = std::chrono::seconds{3};

        Lifetime lifetime;
        std::vector<std::shared_ptr<CallbackQueue>> callbackQueues = {
            std::make_shared<CallbackQueue>(),
            std::make_shared<CallbackQueue>(),
            std::make_shared<CallbackQueue>(),
            std::make_shared<CallbackQueue>(),
            std::make_shared<CallbackQueue>(),
            std::make_shared<CallbackQueue>(),
            std::make_shared<CallbackQueue>(),
            std::make_shared<CallbackQueue>(),
            std::make_shared<CallbackQueue>(),
            std::make_shared<CallbackQueue>(),
            std::make_shared<CallbackQueue>(),
            std::make_shared<CallbackQueue>(),
            std::make_shared<CallbackQueue>(),
            std::make_shared<CallbackQueue>(),
            std::make_shared<CallbackQueue>(),
            std::make_shared<CallbackQueue>(),
        };
        std::atomic<size_t> scheduled = 0;
        std::atomic<size_t> executed = 0;
        TimerService ts;
        auto till = std::chrono::steady_clock::now() + testTime;
        std::srand(0);
        YIO_LOG_INFO("testFlood: begin flood while");
        while (std::chrono::steady_clock::now() < till && scheduled < 1000000) {
            int timeout = std::rand() % 900 + 100;
            size_t queueIndex = std::rand() % callbackQueues.size();
            int sleep = (queueIndex % 5 == 0 ? std::rand() % 5 : 0);
            ts.createPeriodicTask(std::chrono::milliseconds{timeout},
                                  [&executed, sleep] {
                                      if (sleep) {
                                          std::this_thread::sleep_for(std::chrono::milliseconds{sleep});
                                      }
                                      ++executed;
                                  }, lifetime, callbackQueues[queueIndex]);
            ++scheduled;
        }
        YIO_LOG_INFO("testFlood: after while scheduled=" << scheduled.load() << ", executed=" << executed.load() << ", use_count=" << lifetime.use_count());
        std::this_thread::sleep_for(till - std::chrono::steady_clock::now());
        YIO_LOG_INFO("testFlood: before lifetime.die(), use_count=" << lifetime.use_count());
        lifetime.die();
        YIO_LOG_INFO("testFlood: after lifetime.die(), use_count=" << lifetime.use_count());
        YIO_LOG_INFO("testFlood: scheduled=" << scheduled.load() << ", executed=" << executed.load() << ", use_count=" << lifetime.use_count());
        UNIT_ASSERT(scheduled.load() > 100);
        UNIT_ASSERT(executed.load() > 1);
    }
}

#include <yandex_io/libs/ipc/asio/asio_callback_pool.h>
#include <yandex_io/libs/ipc/asio/asio_callback_queue.h>

#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/libs/threading/timer_service.h>

#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <array>
#include <random>

using namespace quasar;
using namespace quasar::ipc::detail::asio_ipc;
using namespace quasar::TestUtils;

namespace {

    class Fixture: public QuasarUnitTestFixtureWithoutIpc {
    public:
        using Base = QuasarUnitTestFixtureWithoutIpc;

        Fixture()
            : generator((std::random_device())())
        {
        }

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);
            timerService = std::make_shared<TimerService>(
                [](std::exception_ptr exptr) {
                    try {
                        std::rethrow_exception(exptr);
                    } catch (const std::exception& ex) {
                        YIO_LOG_WARN("Exception in timer service: " << ex.what());
                    } catch (...) {
                        YIO_LOG_WARN("Unexpected and unknown exception in timer service");
                    }
                });
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            Base::TearDown(context);
        }

        std::shared_ptr<AsioCallbackPool> createAsioCallbackPool(size_t n) {
            return std::make_shared<AsioCallbackPool>("TestPool", n, timerService);
        }

    public:
        std::mt19937 generator;
        std::shared_ptr<TimerService> timerService;
    };

} // Anonymous namespace

Y_UNIT_TEST_SUITE_F(AsioCallbackQueue, Fixture) {
    Y_UNIT_TEST(Ctor)
    {
        UNIT_ASSERT(createAsioCallbackPool(1));
    }

    Y_UNIT_TEST(WorkerThread1_TwoQueue)
    {
        std::mutex mutex;
        std::vector<int> results;

        auto pool = createAsioCallbackPool(1);
        auto cq1 = pool->createAsioCallbackQueue("cq1");
        auto cq2 = pool->createAsioCallbackQueue("cq2");

        auto inserter = [&](int v) {
            std::unique_lock lock(mutex);
            results.push_back(v);
            lock.unlock();
            std::this_thread::sleep_for(std::chrono::milliseconds{10});
        };

        cq1->add([&] { inserter(1); });
        cq2->add([&] { inserter(2); });
        cq1->add([&] { inserter(3); });
        cq2->add([&] { inserter(4); });
        cq2->add([&] { inserter(5); });
        cq2->add([&] { inserter(6); });
        cq1->add([&] { inserter(7); });
        cq1->add([&] { inserter(8); });
        cq2->add([&] { inserter(9); });
        cq1->add([&] { inserter(10); });
        cq1->add([&] { inserter(11); });
        cq2->add([&] { inserter(12); });
        cq2->add([&] { inserter(13); });
        cq2->add([&] { inserter(14); });
        cq1->add([&] { inserter(15); });
        cq1->add([&] { inserter(16); });

        flushCallbackQueue(cq1, cq2);

        std::vector<int> expected;
        for (size_t i = 1; i <= 16; ++i) {
            expected.push_back(i);
        }
        UNIT_ASSERT_VALUES_EQUAL(join(results, ", "), join(expected, ", "));
    }

    Y_UNIT_TEST(WorkerThread2_TwoQueue)
    {
        std::uniform_int_distribution<uint8_t> d(0, 15);
        std::vector<int> results1;
        std::vector<int> results2;

        auto pool = createAsioCallbackPool(1);
        auto cq1 = pool->createAsioCallbackQueue("cq1");
        auto cq2 = pool->createAsioCallbackQueue("cq2");

        auto inserter1 = [&](int v) {
            auto m = d(generator);
            results1.push_back(v);
            std::this_thread::sleep_for(std::chrono::milliseconds{10 + m});
        };

        auto inserter2 = [&](int v) {
            auto m = d(generator);
            results2.push_back(v);
            std::this_thread::sleep_for(std::chrono::milliseconds{10 + m});
        };

        cq1->add([&] { inserter1(1); });
        cq2->add([&] { inserter2(2); });
        cq1->add([&] { inserter1(3); });
        cq2->add([&] { inserter2(4); });
        cq2->add([&] { inserter2(5); });
        cq2->add([&] { inserter2(6); });
        cq1->add([&] { inserter1(7); });
        cq1->add([&] { inserter1(8); });
        cq2->add([&] { inserter2(9); });
        cq1->add([&] { inserter1(10); });

        flushCallbackQueue(cq1, cq2);

        std::vector<int> expected1{1, 3, 7, 8, 10};
        std::vector<int> expected2{2, 4, 5, 6, 9};

        UNIT_ASSERT_VALUES_EQUAL(join(results1, ", "), join(expected1, ", "));
        UNIT_ASSERT_VALUES_EQUAL(join(results2, ", "), join(expected2, ", "));
    }

    Y_UNIT_TEST(WorkerThread2_TwoQueue_HeavyTask)
    {
        std::vector<int> results1;
        std::vector<int> results2;

        auto pool = createAsioCallbackPool(2);
        auto cq1 = pool->createAsioCallbackQueue("cq1");
        auto cq2 = pool->createAsioCallbackQueue("cq2");

        std::atomic<int> stopper{0};
        std::atomic<int> inserter_stage_before{-1};
        std::atomic<int> inserter_stage_after{-1};
        auto inserter1 = [&](int v) {
            YIO_LOG_INFO("WorkerThread2_TwoQueue_HeavyTask: insert1 v=" << v << " [BEGIN]");
            inserter_stage_before = v;
            doUntil([&] { return stopper > v; }, 2000);
            UNIT_ASSERT_C(stopper > v, "v=" << v);
            results1.push_back(v);
            inserter_stage_after = v;
            YIO_LOG_INFO("WorkerThread2_TwoQueue_HeavyTask: insert1 v=" << v << " [FIN]");
        };
        cq1->add([&] { inserter1(0); });
        cq1->add([&] { inserter1(1); });

        auto inserter2 = [&](int v) {
            YIO_LOG_INFO("WorkerThread2_TwoQueue_HeavyTask: insert2 v=" << v);
            results2.push_back(v);
        };
        cq2->add([&] { inserter2(0); });
        cq2->add([&] { inserter2(1); });
        cq2->add([&] { inserter2(2); });
        cq2->add([&] { inserter2(3); });

        // cq1 doesn't block cq2
        flushCallbackQueue(cq2);
        UNIT_ASSERT_VALUES_EQUAL(join(results2, ", "), "0, 1, 2, 3");

        // cq1 still running task "0"
        UNIT_ASSERT_VALUES_EQUAL(results1.size(), 0);
        stopper = 1;
        doUntil([&] { return inserter_stage_before == 1; }, 2000); // second task in cq1 starts
        UNIT_ASSERT_VALUES_EQUAL(inserter_stage_before.load(), 1);
        UNIT_ASSERT_VALUES_EQUAL(results1.size(), 1);
        UNIT_ASSERT_VALUES_EQUAL(results1[0], 0);

        stopper = 2;
        doUntil([&] { return inserter_stage_after == 1; }, 2000); // second task in cq1 finish
        UNIT_ASSERT_VALUES_EQUAL(inserter_stage_after.load(), 1);
        UNIT_ASSERT_VALUES_EQUAL(join(results1, ", "), "0, 1");

        flushCallbackQueue(cq1);
    }

    Y_UNIT_TEST(WorkerThread_DelayedTasks)
    {
        std::atomic<size_t> counter{0};

        constexpr size_t totalQueues = 8;
        auto pool = createAsioCallbackPool(totalQueues / 2);
        std::array<std::shared_ptr<AsioCallbackQueue>, totalQueues> queues;
        std::array<std::atomic<size_t>, totalQueues> taskFinishCounter;
        std::array<size_t, totalQueues> taskExpectedCounter;
        for (size_t i = 0; i < totalQueues; ++i) {
            queues[i] = (pool->createAsioCallbackQueue("cq" + std::to_string(i)));
            taskFinishCounter[i] = 0;
            taskExpectedCounter[i] = 0;
        }

        constexpr size_t totalTasks = 10000;
        constexpr size_t minMs = 10;
        constexpr size_t maxMs = 200;
        YIO_LOG_INFO("Schedule " << totalTasks << " tasks..");
        std::uniform_int_distribution<size_t> dIndex(0, totalQueues - 1);
        std::uniform_int_distribution<size_t> dMs(minMs, maxMs);
        for (size_t i = 0; i < totalTasks; ++i) {
            size_t index = dIndex(generator);
            auto ms = std::chrono::milliseconds{dMs(generator)};
            queues[index]->addDelayed([&, ii = index] { ++counter; ++taskFinishCounter[ii]; }, ms);
            ++taskExpectedCounter[index];
        }
        YIO_LOG_INFO("Awaiting task execution... (unfinished tasks = " << (totalTasks - counter.load()) << ")");

        doUntil([&] { return counter >= totalTasks; }, maxMs + 5000);
        UNIT_ASSERT_VALUES_EQUAL(totalTasks, counter.load());
        for (auto& queue : queues) {
            flushCallbackQueue(queue);
        }
        UNIT_ASSERT_VALUES_EQUAL(totalTasks, counter.load());
        YIO_LOG_INFO("All task finished");

        for (size_t i = 0; i < totalQueues; ++i) {
            YIO_LOG_INFO(queues[i]->name() << ": expected=" << taskExpectedCounter[i] << ", finished=" << taskFinishCounter[i].load() << (taskExpectedCounter[i] != taskFinishCounter[i] ? " [FAIL]" : ""));
            UNIT_ASSERT_VALUES_EQUAL(taskExpectedCounter[i], taskFinishCounter[i].load());
        }
    }

    Y_UNIT_TEST(RatRacing)
    {
        constexpr size_t totalQueues = 512;
        constexpr size_t taskPerQueue = 8 * 1024;
        std::atomic<size_t> finishedTasks{0};
        std::atomic<size_t> finishedQueue{0};
        std::atomic<bool> damFlag{true};
        auto pool = createAsioCallbackPool(12);
        std::array<std::shared_ptr<AsioCallbackQueue>, totalQueues> queues;
        std::array<std::atomic<size_t>, totalQueues> finishTs;

        YIO_LOG_INFO("Create all queues (totalQueues=" << totalQueues << ")");
        auto damAwaiter = [&] {
            while (damFlag) {
                std::this_thread::yield();
            }
        };

        for (size_t i = 0; i < totalQueues; ++i) {
            queues[i] = (pool->createAsioCallbackQueue("cq" + std::to_string(i)));
            queues[i]->add([&] { damAwaiter(); });
        }

        YIO_LOG_INFO("Schedule tasks (taskPerQueue=" << taskPerQueue << ")");
        for (size_t j = 0; j < taskPerQueue; ++j) {
            for (size_t i = 0; i < totalQueues; ++i) {
                queues[i]->add([&] { ++finishedTasks; });
            }
        }
        for (size_t i = 0; i < totalQueues; ++i) {
            queues[i]->add(
                [&, iq = i] {
                    auto dur = std::chrono::steady_clock::now() - std::chrono::steady_clock::time_point{};
                    finishTs[iq] = static_cast<size_t>(std::chrono::duration_cast<std::chrono::milliseconds>(dur).count());
                    ++finishedQueue;
                });
        }
        YIO_LOG_INFO("Let the rat race begin!");
        auto dur0 = std::chrono::steady_clock::now() - std::chrono::steady_clock::time_point{};
        size_t t0 = static_cast<size_t>(std::chrono::duration_cast<std::chrono::milliseconds>(dur0).count());
        damFlag = false;
        doUntil([&] { return finishedQueue >= totalQueues; }, 30000);
        UNIT_ASSERT_VALUES_EQUAL(finishedQueue.load(), totalQueues);
        UNIT_ASSERT_VALUES_EQUAL(finishedTasks.load(), totalQueues* taskPerQueue);

        std::array<double, totalQueues> measure;
        for (size_t i = 0; i < totalQueues; ++i) {
            measure[i] = static_cast<double>(finishTs[i] - t0);
        }
        double average = std::reduce(measure.begin(), measure.end()) / measure.size();
        double sigma2 = 0.;
        for (size_t i = 0; i < measure.size(); ++i) {
            sigma2 += (measure[i] - average) * (measure[i] - average);
        }
        sigma2 = sigma2 / (measure.size() - 1);
        double sigma = std::sqrt(sigma2);
        double _1sigma = 0;
        double _2sigma = 0;
        double _3sigma = 0;
        for (size_t i = 0; i < measure.size(); ++i) {
            auto l = std::abs(measure[i] - average);
            _1sigma += (l <= 1 * sigma ? 1 : 0);
            _2sigma += (l <= 2 * sigma ? 1 : 0);
            _3sigma += (l <= 3 * sigma ? 1 : 0);
        }
        double p1 = _1sigma / measure.size();
        double p2 = _2sigma / measure.size();
        double p3 = _3sigma / measure.size();
        YIO_LOG_INFO("Queue average execution time " << average << " ms");
        YIO_LOG_INFO("Queue standard deviation " << sigma << " ms");
        YIO_LOG_INFO("      1*sigma = " << p1 << " (" << _1sigma << ")");
        YIO_LOG_INFO("      2*sigma = " << p2 << " (" << _2sigma << ")");
        YIO_LOG_INFO("      3*sigma = " << p3 << " (" << _3sigma << ")");
        UNIT_ASSERT(p1 > .5);
        UNIT_ASSERT(p2 > .9);
        UNIT_ASSERT(p3 > .95);
    }

    Y_UNIT_TEST(DestoryPoolWhileQueueWorks)
    {
        auto pool = createAsioCallbackPool(2);
        auto cq1 = pool->createAsioCallbackQueue("cq1");
        auto cq2 = pool->createAsioCallbackQueue("cq2");

        std::atomic<int> jobDone1{0};
        std::atomic<int> jobDone2{0};
        std::atomic<bool> damFlag{true};
        auto damAwaiter = [&] {
            while (damFlag) {
                std::this_thread::yield();
            }
        };
        cq1->add([&] { damAwaiter(); ++jobDone1; });
        cq1->add([&] { ++jobDone1; });
        cq1->add([&] { ++jobDone1; });

        cq2->add([&] { damAwaiter(); ++jobDone2; });
        cq2->add([&] { ++jobDone2; });
        cq2->shutdown();               // No more task will sheduled
        cq2->add([&] { ++jobDone2; }); // will not run

        std::atomic<bool> destroyerFlag{false};
        auto destroyer = std::async(std::launch::async, [&] { destroyerFlag = true; pool->destroy(); });
        waitUntil([&] { return destroyerFlag.load(); });
        std::this_thread::sleep_for(std::chrono::seconds{1}); // I don't know how to guaranty that destroy() already calls
        damFlag = false;
        destroyer.wait();
        cq1->add([&] { ++jobDone1; }); // will not run
        cq2->add([&] { ++jobDone2; }); // will not run

        flushCallbackQueue(cq1, cq2);

        UNIT_ASSERT_VALUES_EQUAL(jobDone1.load(), 3);
        UNIT_ASSERT_VALUES_EQUAL(jobDone2.load(), 2);
    }

    Y_UNIT_TEST(QueueCircularReferenceToItself)
    {
        std::atomic<bool> damFlag{true};
        auto damAwaiter = [&] {
            while (damFlag) {
                std::this_thread::yield();
            }
        };
        std::atomic<int> jobDone1{0};
        // **NOTE ** Here we are forced to set 1 worker thread, since a situation may arise
        //           when a free thread temporarily captures a weak reference to the queue
        //           (it make it strong) from orderQueue_ to schedule execution, but after
        //           making sure that the queue is already in operation, it releases the link,
        //           but it can it will turn out that a task (44) that does not have a
        //           reference to the queue  can be executed.
        auto pool = createAsioCallbackPool(1);
        auto cq1 = pool->createAsioCallbackQueue("cq1");
        std::weak_ptr<AsioCallbackQueue> wcq1 = cq1;

        {
            auto strongScheduler = [&, queue = cq1](int n) {
                YIO_LOG_INFO("QueueCircularReferenceToItself Schedule task #" << n);
                queue->add([&, queue, n] {
                    UNIT_ASSERT(queue); // Keep strong reference!
                    YIO_LOG_INFO("QueueCircularReferenceToItself Task #" << n);
                    ++jobDone1;
                });
            };
            auto weakScheduler = [&, queue = cq1](int n) {
                YIO_LOG_INFO("QueueCircularReferenceToItself Schedule never call task #" << n);
                queue->add([n] {
                    // No strong reference on queue
                    YIO_LOG_INFO("QueueCircularReferenceToItself Task #" << n << ". BUT WHY?!");
                    UNIT_ASSERT(false);
                });
            };
            cq1->add([&] {
                YIO_LOG_INFO("QueueCircularReferenceToItself Task #1");
                damAwaiter();
                ++jobDone1;
            });
            cq1->add([&jobDone1, strongScheduler] {
                YIO_LOG_INFO("QueueCircularReferenceToItself Task #2");
                ++jobDone1;
                strongScheduler(22);
            });
            cq1->add([&jobDone1, strongScheduler] {
                YIO_LOG_INFO("QueueCircularReferenceToItself Task #3");
                ++jobDone1;
                strongScheduler(33);
            });
            cq1->add([&jobDone1, weakScheduler] {
                YIO_LOG_INFO("QueueCircularReferenceToItself Task #4");
                ++jobDone1;
                weakScheduler(44);
            });
        }
        cq1.reset();
        YIO_LOG_INFO("Reset external reference on callback queue");
        UNIT_ASSERT(!wcq1.expired());

        YIO_LOG_INFO("Release \"DAM\" flag...");
        damFlag = false;
        waitUntil([&] { return wcq1.expired(); });
        pool.reset(); // Remove ALL

        UNIT_ASSERT_VALUES_EQUAL(jobDone1.load(), 6);
    }
}

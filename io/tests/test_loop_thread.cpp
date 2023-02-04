#include <yandex_io/callkit/util/loop_thread.h>

#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <chrono>
#include <condition_variable>
#include <functional>
#include <mutex>

namespace {

    struct TestTask: std::enable_shared_from_this<TestTask> {
        TestTask()
            : callCount(0u)
            , order(-1)
            , mainThreadId(std::this_thread::get_id())
        {
        }

        virtual ~TestTask() = default;

        std::function<void()> get() {
            std::weak_ptr<TestTask> weakPtr = shared_from_this();
            return [=] {
                auto task = weakPtr.lock();
                UNIT_ASSERT(task); // task died
                if (!task) {
                    return;
                }
                auto oldThread = threadId.exchange(std::this_thread::get_id());
                UNIT_ASSERT(mainThreadId != threadId.load());
                if (callCount) {
                    UNIT_ASSERT(oldThread == threadId.load());
                } else {
                    order = taskOrderCounter_++;
                }
                std::unique_lock<std::mutex> lock(mutex_);
                callCount++;
                runImpl();
                condition_.notify_all();
            };
        }

        void awaitCall(size_t count = 1u) {
            std::unique_lock<std::mutex> lock(mutex_);
            condition_.wait_for(lock, std::chrono::seconds(5),
                                [=] { return callCount == count; });
        }

        std::atomic<size_t> callCount;
        int order;
        std::atomic<std::thread::id> threadId;
        const std::thread::id mainThreadId;

        static void reset() {
            taskOrderCounter_ = 0;
        }

    protected:
        virtual void runImpl() {
        }

    private:
        static int taskOrderCounter_;

        std::mutex mutex_;
        std::condition_variable condition_;
    };

    int TestTask::taskOrderCounter_ = 0;

    struct TestBusyTask: public TestTask {
        TestBusyTask(int sleepMilliseconds)
            : sleepMilliseconds(sleepMilliseconds)
        {
        }
        void runImpl() override {
            std::this_thread::sleep_for(
                std::chrono::milliseconds(sleepMilliseconds));
        }

        const int sleepMilliseconds;
    };

} // namespace

Y_UNIT_TEST_SUITE_F(LoopThread, QuasarUnitTestFixture) {
    Y_UNIT_TEST(base_test) {
        auto loop = messenger::LoopThread::create();

        TestTask::reset();

        auto task1 = std::make_shared<TestTask>();
        auto task2 = std::make_shared<TestTask>();
        loop->execute(task1->get());
        loop->execute([&] {
            loop->execute(task1->get());
            loop->execute(task2->get());
        });

        task2->awaitCall();
        loop->destroyBlocked();

        UNIT_ASSERT_VALUES_EQUAL(2u, task1->callCount.load());
        UNIT_ASSERT_VALUES_EQUAL(1u, task2->callCount.load());
        UNIT_ASSERT_VALUES_EQUAL(0, task1->order);
        UNIT_ASSERT_VALUES_EQUAL(1, task2->order);
        UNIT_ASSERT_EQUAL(task1->threadId.load(), task2->threadId.load());
    }

    Y_UNIT_TEST(task_cancelled) {
        auto loop = messenger::LoopThread::create();

        TestTask::reset();

        auto task1 = std::make_shared<TestTask>();
        auto task2 = std::make_shared<TestTask>();
        auto marker = std::make_shared<TestTask>();
        messenger::ScopedExecutor scopedExecutor;
        loop->execute([&] { scopedExecutor = messenger::ScopedExecutor::create(loop); });
        loop->execute([&] { scopedExecutor.execute(task1->get()); });
        task1->awaitCall();
        loop->execute([&] {
            scopedExecutor.execute(task2->get());
            scopedExecutor.reset();
        });
        loop->execute(marker->get());

        marker->awaitCall();
        loop->destroyBlocked();

        UNIT_ASSERT(task1->callCount);
        UNIT_ASSERT(!task2->callCount);
    }

    Y_UNIT_TEST(idle_task_called) {
        auto loop = messenger::LoopThread::create();

        TestTask::reset();

        auto task1 = std::make_shared<TestBusyTask>(100);
        auto task2 = std::make_shared<TestTask>();
        auto task3 = std::make_shared<TestTask>();
        auto taskIdle = std::make_shared<TestTask>();

        loop->execute(task1->get());
        loop->executeOnIdle(taskIdle->get());
        loop->execute(task2->get());
        loop->execute(task3->get());

        taskIdle->awaitCall();

        loop->destroyBlocked();

        UNIT_ASSERT(task2->callCount);
        UNIT_ASSERT_VALUES_EQUAL(0, task1->order);
        UNIT_ASSERT_VALUES_EQUAL(1, task2->order);
    }

    Y_UNIT_TEST(delayed_task_called) {
        auto loop = messenger::LoopThread::create();

        TestTask::reset();

        auto task1 = std::make_shared<TestBusyTask>(100);
        auto task2 = std::make_shared<TestTask>();
        auto taskDelayed = std::make_shared<TestTask>();
        loop->executeDelayed(taskDelayed->get(), std::chrono::milliseconds(100));
        loop->execute(task1->get());
        loop->execute(task2->get());

        task2->awaitCall();

        loop->destroyBlocked();

        UNIT_ASSERT(task2->callCount);
        UNIT_ASSERT_VALUES_EQUAL(0, task1->order);
        UNIT_ASSERT_VALUES_EQUAL(1, taskDelayed->order);
        UNIT_ASSERT_VALUES_EQUAL(2, task2->order);
    }

    Y_UNIT_TEST(task_order) {
        auto loop = messenger::LoopThread::create();

        TestTask::reset();

        auto task1 = std::make_shared<TestBusyTask>(100);
        auto task2 = std::make_shared<TestBusyTask>(100);
        auto taskDelayed1 = std::make_shared<TestTask>();
        auto taskDelayed2 = std::make_shared<TestTask>();
        auto taskDelayed3 = std::make_shared<TestTask>();
        auto taskIdle1 = std::make_shared<TestTask>();
        auto taskIdle2 = std::make_shared<TestTask>();

        loop->executeDelayed(taskDelayed1->get(), std::chrono::milliseconds(100));
        loop->executeDelayed(taskDelayed2->get(), std::chrono::milliseconds(100));
        loop->executeDelayed(taskDelayed3->get(), std::chrono::milliseconds(200));
        loop->execute(task1->get());
        loop->execute(task2->get());
        loop->executeOnIdle(taskIdle1->get());
        loop->executeOnIdle(taskIdle2->get());

        taskIdle2->awaitCall();

        loop->destroyBlocked();

        UNIT_ASSERT_VALUES_EQUAL(0, task1->order);
        UNIT_ASSERT_VALUES_EQUAL(1, taskDelayed1->order);
        UNIT_ASSERT_VALUES_EQUAL(2, taskDelayed2->order);
        UNIT_ASSERT_VALUES_EQUAL(3, task2->order);
        UNIT_ASSERT_VALUES_EQUAL(4, taskDelayed3->order);
        UNIT_ASSERT_VALUES_EQUAL(5, taskIdle1->order);
        UNIT_ASSERT_VALUES_EQUAL(6, taskIdle2->order);
    }

    Y_UNIT_TEST(later_added_task_called_erlier) {
        auto loop = messenger::LoopThread::create();

        TestTask::reset();

        auto lateTask = std::make_shared<TestTask>();
        auto earlyTask = std::make_shared<TestTask>();

        loop->executeDelayed(lateTask->get(), std::chrono::seconds(4));
        // some time to make the thread sleep
        std::this_thread::sleep_for(std::chrono::milliseconds(500));
        loop->executeDelayed(earlyTask->get(), std::chrono::seconds(1));
        // +200ms extra to allow to wakeup and run
        std::this_thread::sleep_for(std::chrono::milliseconds(1200));

        UNIT_ASSERT(earlyTask->callCount);

        lateTask->awaitCall();
        loop->destroyBlocked();

        UNIT_ASSERT_VALUES_EQUAL(0, earlyTask->order);
        UNIT_ASSERT_VALUES_EQUAL(1, lateTask->order);
    }

    Y_UNIT_TEST(delayed_task_added_to_empty_sleeping_queue) {
        auto loop = messenger::LoopThread::create();

        TestTask::reset();

        auto delayedTask = std::make_shared<TestTask>();

        // some time to make the thread sleep
        std::this_thread::sleep_for(std::chrono::milliseconds(500));
        loop->executeDelayed(delayedTask->get(), std::chrono::seconds(1));
        // +200ms extra to allow to wakeup and run
        std::this_thread::sleep_for(std::chrono::milliseconds(1200));

        UNIT_ASSERT(delayedTask->callCount);

        loop->destroyBlocked();
    }

    Y_UNIT_TEST(idle_tasks_dont_respect_delayed_tasks) {
        auto loop = messenger::LoopThread::create();

        TestTask::reset();

        auto taskRegular = std::make_shared<TestBusyTask>(100);
        auto taskIdle = std::make_shared<TestTask>();
        auto taskDelayed = std::make_shared<TestTask>();

        loop->executeDelayed(taskDelayed->get(), std::chrono::milliseconds(300));
        loop->execute(taskRegular->get());
        loop->executeOnIdle(taskIdle->get());

        taskIdle->awaitCall();
        Y_VERIFY(!taskDelayed->callCount);
        UNIT_ASSERT(loop->isIdle());

        taskDelayed->awaitCall();
        loop->destroyBlocked();

        UNIT_ASSERT_VALUES_EQUAL(0u, taskRegular->order);
        UNIT_ASSERT_VALUES_EQUAL(1u, taskIdle->order);
        UNIT_ASSERT_VALUES_EQUAL(2u, taskDelayed->order);
    }
}

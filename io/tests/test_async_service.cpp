#include <yandex_io/callkit/util/async_service.h>
#include <yandex_io/callkit/util/async_task.h>
#include <yandex_io/callkit/util/loop_thread.h>
#include <yandex_io/callkit/util/weak_utils.h>

#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <chrono>
#include <functional>
#include <future>
#include <memory>
#include <string>

using namespace messenger;
using namespace std::chrono_literals;

struct TestTask: public AsyncTask {
    TestTask(std::shared_ptr<LoopThread> workerThread,
             std::chrono::milliseconds duration)
        : workerThread(std::move(workerThread))
        , duration(duration)
    {
    }
    virtual ~TestTask() {
    }
    void run() override {
        UNIT_ASSERT(workerThread->checkOutside());
        std::this_thread::sleep_for(duration);
        bgRun = true;
    }
    void onFinished() override {
        UNIT_ASSERT(workerThread->checkInside());
        finished = true;
    }
    std::string getName() override {
        return "Test task";
    }

    bool bgRun = false;
    bool finished = false;

    std::shared_ptr<LoopThread> workerThread;
    std::chrono::milliseconds duration;
};

struct TestFixture: QuasarUnitTestFixture {
    TestFixture()
        : scope_(new int(0))
    {
        loop = messenger::LoopThread::create();
        loop->execute(bindWeak(scope_, [this] {
            service = std::make_shared<AsyncService>(loop);
        }));
    }

    ~TestFixture() {
        loop->execute([&] { service.reset(); });
        loop->destroyBlocked();
    }

    std::shared_ptr<TestTask>
    newTask(std::chrono::milliseconds duration = 100ms) {
        return std::make_shared<TestTask>(loop, duration);
    }

    void doOnWorker(std::function<void()> job) const {
        std::promise<bool> promise;
        auto future = promise.get_future();
        loop->execute([&] {
            job();
            service->runOnIdle([&] { promise.set_value(true); });
        });
        future.wait_for(5s);
    }

    std::shared_ptr<AsyncService> service;

    std::shared_ptr<LoopThread> loop;

private:
    std::shared_ptr<int> scope_;
};

Y_UNIT_TEST_SUITE_F(AsyncService, TestFixture) {
    Y_UNIT_TEST(task_done) {
        auto task = newTask();

        doOnWorker([&] { service->start(task); });

        UNIT_ASSERT(task->bgRun);
        UNIT_ASSERT(task->finished);
        UNIT_ASSERT(!task->isCancelled());
    }

    Y_UNIT_TEST(task_cancelled) {
        auto task = newTask();

        doOnWorker([&] {
            service->start(task);
            service->cancel(task);
        });

        UNIT_ASSERT(!task->finished);
        UNIT_ASSERT(task->isCancelled());
    }

    Y_UNIT_TEST(test_task4_works) {
        std::vector<std::shared_ptr<TestTask>> tasks;
        for (int i = 0; i < 3; ++i) {
            tasks.push_back(newTask(1s));
        }
        auto task4 = newTask(1s);
        doOnWorker([&] {
            for (const auto& task : tasks) {
                service->start(task);
            }
            service->start(task4);
            for (const auto& task : tasks) {
                service->cancel(task);
            }
        });
        UNIT_ASSERT(task4->bgRun);
        UNIT_ASSERT(task4->finished);
        for (const auto& task : tasks) {
            UNIT_ASSERT(!task->finished);
            UNIT_ASSERT(task->isCancelled());
        }
    }

    Y_UNIT_TEST(test_task5_works) {
        std::vector<std::shared_ptr<TestTask>> tasks;
        for (int i = 0; i < 4; ++i) {
            tasks.push_back(newTask(1s));
        }
        auto task5 = newTask(1s);
        doOnWorker([&] {
            for (const auto& task : tasks) {
                service->start(task);
            }
            service->start(task5);
            for (const auto& task : tasks) {
                service->cancel(task);
            }
        });

        UNIT_ASSERT(task5->bgRun);
        UNIT_ASSERT(task5->finished);
        for (const auto& task : tasks) {
            UNIT_ASSERT(!task->finished);
            UNIT_ASSERT(task->isCancelled());
        }
    }
}

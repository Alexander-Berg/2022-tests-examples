#include <maps/libs/common/include/exception.h>
#include <maps/libs/concurrent/include/threadpool.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <algorithm>
#include <atomic>
#include <cctype>
#include <condition_variable>
#include <cstdlib>
#include <queue>
#include <numeric>
#include <vector>

namespace maps::concurrent::tests {

void quickJob() {
    int buffer[] = {1, 2, 3, 4, 5, 6, 7};
    while (std::next_permutation(buffer, buffer + sizeof(buffer)/sizeof(buffer[0])));

}

int longJob(int a) {
    int buffer[] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    while (std::next_permutation(buffer, buffer + sizeof(buffer)/sizeof(buffer[0])));
    return a;
}

struct IntTask {
    void operator() (int) {
    }
};

int exceptionJob(int i) {
    throw maps::RuntimeError() << "test exception";
    return i;
}

struct BlockingStringTask {
    static std::atomic<bool> done;
    static std::condition_variable doneCv;
    static std::mutex doneCvMutex;

    void operator() (std::string /* unused */) {
        std::unique_lock lock(doneCvMutex);
        doneCv.wait(
            lock,
            [&]() -> bool {
                return done;
            }
        );
    }
};
std::atomic<bool> BlockingStringTask::done = false;
std::condition_variable BlockingStringTask::doneCv;
std::mutex BlockingStringTask::doneCvMutex;

Y_UNIT_TEST_SUITE(test_threadpool) {

Y_UNIT_TEST(FullQueueRejectTestNoExc) {
    BlockingStringTask::done = false;

    TaskSpecificThreadPool<BlockingStringTask, std::string> tp(
        ThreadsNumber(1),
        QueueCapacity(1)
    );

    //Thread pool has space for one task and only one thread which can take this task for execution

    //first task will be taken by the only thread from thread pool. This thread will block until done
    tp.add(std::string("first task"));

    //second task will stuck in queue
    tp.add(std::string("second task"));

    //queue now is full, so next non-blocking add should fail
    std::string thirdTask("third task");
    ASSERT_FALSE(tp.tryAdd(std::move(thirdTask)));
    ASSERT_EQ(thirdTask, "third task");

    //waking up all threads, so they can live happy ever after
    BlockingStringTask::done = true;
    BlockingStringTask::doneCv.notify_all();
}

Y_UNIT_TEST(ErrorHandlerTest) {
    bool failed = false;
    ThreadPool tp(ThreadsNumber(24), QueueCapacity(10));
    tp.setErrorHandler([&failed](const std::exception &) { failed = true; });
    for (int i = 0; i < 1000; ++i) {
        tp.add(std::bind(exceptionJob, 1));
    }
    EXPECT_TRUE(failed);
}

Y_UNIT_TEST(ZeroParams) {
    EXPECT_THROW(
        ThreadPool(ThreadsNumber(0), DEFAULT_QUEUE_CAPACITY),
        Exception
    );

    EXPECT_THROW(
        ThreadPool(DEFAULT_THREADS_NUMBER, QueueCapacity(0)),
        Exception
    );
}

Y_UNIT_TEST(TaskSpecificThreadPoolTest) {
    TaskSpecificThreadPool<IntTask, int> tp;
    tp.add(rand());
}

Y_UNIT_TEST(JoinAndDestructorTest) {
    TaskSpecificThreadPool<IntTask, int> tp(ThreadsNumber(1));
    tp.add(rand());
    tp.join();
}

Y_UNIT_TEST(NormalMode) {
    std::vector<size_t> threadNums {1, 16, 128};
    std::vector<size_t> queueSizes {1, 64};
    std::vector<size_t> jobNums {1, 16, 2048};
    for (auto threadNum : threadNums) {
        for (auto queueSize : queueSizes) {
            for (auto jobNum : jobNums) {
                ThreadPool tp(
                    (ThreadsNumber(threadNum)),
                    (QueueCapacity(queueSize))
                );
                EXPECT_EQ(tp.capacity(), queueSize);
                for (size_t i = 0; i < jobNum; ++i) {
                    tp.add(quickJob);
                }
            }
        }
    }
}

Y_UNIT_TEST(DrainMode) {
    constexpr int NUM = 200;
    ThreadPool tp(ThreadsNumber(32));
    std::atomic<int> counter{0};
    for (int i = 0; i != NUM; ++i) {
        for (int j = 0; j != NUM; ++j) {
            tp.add([&counter] { ++counter; });
        }

        tp.drain();
        EXPECT_EQ(counter.load(), (i + 1) * NUM);
    }
}

Y_UNIT_TEST(AsyncTestException) {
    ThreadPool tp(ThreadsNumber(24), QueueCapacity(100));
    auto result1 = tp.async(exceptionJob, 1);
    bool thrown = true, catched = false;
    try {
        result1.get();
        thrown = false;
    } catch (maps::Exception & e){
        catched = true;
    }
    ASSERT_TRUE(thrown);
    ASSERT_TRUE(catched);

}

Y_UNIT_TEST(AsyncTest) {
    ThreadPool tp(ThreadsNumber(24), QueueCapacity(100));
    auto result1 = tp.async(longJob, 1);
    auto result2 = tp.async(longJob, 2);
    auto result3 = tp.async(longJob, 3);
    auto result4 = tp.async(std::bind(longJob, 4));
    auto result5 = tp.async(std::bind(longJob, 5));
    auto result6 = tp.async(std::bind(longJob, 6));
    EXPECT_EQ(
        result1.get() + result2.get() + result3.get() +
        result4.get() + result5.get() + result6.get(),
        21
    );
}

Y_UNIT_TEST(WorkCounterTest) {
    WorkCounter counter;

    for (size_t i = 0; i != 10; ++i) {
        ++counter;
    }
    ThreadPool tp(ThreadsNumber(10), DEFAULT_QUEUE_CAPACITY);
    for (size_t i = 0; i != 10; ++i) {
        tp.add([&] { --counter; });
    }
    tp.join();

    // Nobody changes counter now, so it is good enough that the next
    // line does not hang.
    counter.waitUntilZero();
}

} //Y_UNIT_TEST_SUITE

} //namespace maps::concurrent::tests

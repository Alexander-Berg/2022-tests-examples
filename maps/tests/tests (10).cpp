#include <yandex/maps/wiki/threadutils/executor.h>
#include <yandex/maps/wiki/threadutils/scheduler.h>
#include <yandex/maps/wiki/threadutils/threadpool.h>
#include <yandex/maps/wiki/threadutils/threadedqueue.hpp>
#include <maps/libs/common/include/exception.h>
#include <maps/libs/common/include/profiletimer.h>

#include <boost/test/unit_test.hpp>
#include <algorithm>
#include <atomic>
#include <chrono>
#include <condition_variable>
#include <future>
#include <numeric>
#include <thread>

namespace maps::wiki::tests {

static std::mutex boostCheckMutex;
#define BOOST_SAFE_CHECK( P ) \
    do { \
        std::lock_guard<std::mutex> lock(boostCheckMutex); \
        BOOST_CHECK( ( P ) ); \
    } while (0)

const size_t THRESHOLD = 17;

enum class JobState
{
    NotStarted,
    Done
};

std::ostream& operator<<(std::ostream& os, JobState state)
{
    switch(state) {
        case JobState::NotStarted: return os << "not started";
        case JobState::Done: return os << "done";
    }
    throw std::logic_error("Unexpected job state");
}

void produce(ThreadedQueue<int>& queue)
{
    for (int i = 0; i < 10; ++i) {
        queue.push(i);
    }
}

void consume(ThreadedQueue<int>& queue, int &counter)
{
    ThreadedQueue<int>::RawContainer buffer;
    do {
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
        queue.popAll(buffer);
        BOOST_SAFE_CHECK(buffer.size() <= THRESHOLD);
        for (const auto x : buffer) {
            counter += x;
        }
    } while (!buffer.empty());
}

BOOST_AUTO_TEST_CASE(test_queue)
{
    ThreadedQueue<int> queue(THRESHOLD);
    std::unique_ptr<ThreadPool> producers(new ThreadPool (10));
    for (int i = 0; i < 100; ++i) {
        producers->push(std::bind(produce, std::ref(queue)));
    }

    std::unique_ptr<ThreadPool> consumers(new ThreadPool (10));
    std::vector<int> counters(20);
    for (auto& counter : counters) {
        consumers->push(std::bind(consume, std::ref(queue), std::ref(counter)));
    }

    producers->shutdown();
    producers.reset();
    queue.finish();
    consumers->shutdown();
    consumers.reset();
    BOOST_CHECK_EQUAL(queue.pushedItemsCount(), 100 * 10);
    BOOST_CHECK_EQUAL(queue.poppedItemsCount(), 100 * 10);
    BOOST_CHECK_EQUAL(queue.pendingItemsCount(), 0);
    BOOST_CHECK_EQUAL(std::accumulate(counters.begin(), counters.end(), 0), 45 * 100);
}

BOOST_AUTO_TEST_CASE(test_zero_threads)
{
    BOOST_CHECK_THROW(ThreadPool pool(0), maps::Exception);
}

class WaitAndThrow
{
public:
    void run()
    {
        std::unique_lock<std::mutex> lock(mutex_);
        cond_.wait(lock);
        throw maps::Exception() << "Something bad happened";
    }

    void wakeUp()
    {
        cond_.notify_all();
    }

private:
    std::mutex mutex_;
    std::condition_variable cond_;
};

BOOST_AUTO_TEST_CASE(test_activetasks)
{
    ThreadPool pool(1);
    WaitAndThrow runner;
    pool.push(std::bind(&WaitAndThrow::run, &runner));
    std::this_thread::sleep_for(std::chrono::seconds(1));
    BOOST_CHECK_EQUAL(pool.activeTasksCount(), 1);
    runner.wakeUp();
    std::this_thread::sleep_for(std::chrono::seconds(1));
    BOOST_CHECK_EQUAL(pool.activeTasksCount(), 0);
}

void longtask(std::atomic<size_t>& counter)
{
    std::this_thread::sleep_for(std::chrono::seconds(1));
    ++counter;
}

BOOST_AUTO_TEST_CASE(test_shutdown_empty)
{
    ThreadPool pool(4);
    pool.shutdown();
}

BOOST_AUTO_TEST_CASE(test_shutdown_wait)
{
    const size_t THREADS_COUNT = 2;
    const size_t TASKS_COUNT = 4; // > THREADS_COUNT
    std::atomic<size_t> counter(0);

    ThreadPool pool(THREADS_COUNT);
    for (size_t i = 0; i < TASKS_COUNT; ++i) {
        pool.push(std::bind(longtask, std::ref(counter)));
    }
    pool.shutdown();
    BOOST_CHECK_EQUAL(counter, TASKS_COUNT);
}

BOOST_AUTO_TEST_CASE(test_executer_all)
{
    const size_t THREADS_COUNT = 2;
    const size_t TASKS_COUNT = THREADS_COUNT + 1;
    std::atomic<size_t> counter(0);

    ThreadPool pool(THREADS_COUNT);
    Executor executor;
    for (size_t i = 0; i < TASKS_COUNT; ++i) {
        executor.addTask(std::bind(longtask, std::ref(counter)));
    }

    ProfileTimer pt;
    executor.executeAll(pool);
    BOOST_CHECK_EQUAL(counter, TASKS_COUNT);
    auto durationTime = pt.getElapsedTimeNumber();
    BOOST_CHECK(durationTime > 1 - 0.1); // 1 second - delta 100 ms
    BOOST_CHECK(durationTime < 1 + 0.9); // 1 second + delta 900 ms
    pool.shutdown();
}

BOOST_AUTO_TEST_CASE(test_executer_all_in_threads)
{
    const size_t THREADS_COUNT = 2;
    const size_t TASKS_COUNT = THREADS_COUNT + 1;
    std::atomic<size_t> counter(0);

    ThreadPool pool(THREADS_COUNT);
    Executor executor;
    for (size_t i = 0; i < TASKS_COUNT; ++i) {
        executor.addTask(std::bind(longtask, std::ref(counter)));
    }

    ProfileTimer pt;
    executor.executeAllInThreads(pool);
    BOOST_CHECK_EQUAL(counter, TASKS_COUNT);
    auto durationTime = pt.getElapsedTimeNumber();
    BOOST_CHECK(durationTime > 2 - 0.1); // 2 seconds - delta 100 ms
    BOOST_CHECK(durationTime < 2 + 0.9); // 2 seconds + delta 900 ms
    pool.shutdown();
}

BOOST_AUTO_TEST_CASE(test_executer_all_in_threads_check_pool_unavailable)
{
    const auto DUMMY = [](){};

    Executor executor;
    executor.addTask(DUMMY);

    ThreadPool pool(1);
    pool.shutdown();
    BOOST_CHECK_THROW(executor.executeAllInThreads(pool), maps::RuntimeError);
}

void stopPool(ThreadPool& pool)
{
    std::this_thread::sleep_for(std::chrono::seconds(1));
    pool.stop();
}

BOOST_AUTO_TEST_CASE(test_shutdown_stop)
{
    const size_t THREADS_COUNT = 2;
    const size_t TASKS_COUNT = 5; // > THREADS_COUNT
    std::atomic<size_t> counter(0);

    ThreadPool pool(THREADS_COUNT);
    for (size_t i = 0; i < TASKS_COUNT; ++i) {
        pool.push(std::bind(longtask, std::ref(counter)));
    }
    std::thread stopper(stopPool, std::ref(pool));
    pool.shutdown();
    stopper.join();
    BOOST_CHECK_EQUAL(pool.threadsCount(), THREADS_COUNT);
    BOOST_CHECK_EQUAL(pool.activeTasksCount(), 0);
    BOOST_CHECK_GE(pool.pendingTasksCount(), 1);
    BOOST_CHECK_LE(pool.pendingTasksCount(), 3);
}

BOOST_AUTO_TEST_CASE(test_scheduler)
{
    const size_t THREADS_COUNT = 2;
    const size_t JOBS_COUNT = 5;

    ThreadPool threadPool(THREADS_COUNT);
    Scheduler scheduler;

    std::vector<JobState> jobStates;
    for (size_t i = 0; i < JOBS_COUNT; ++i) {
        jobStates.push_back(JobState::NotStarted);
        scheduler.addTask(
            [&, i](){ jobStates[i] = JobState::Done; },
            [&](const Scheduler::Runner& runner) { threadPool.push(runner); },
            {}
        );
    }
    scheduler.executeAll();

    std::vector<JobState> expectedStates(JOBS_COUNT, JobState::Done);
    BOOST_CHECK_EQUAL_COLLECTIONS(jobStates.begin(), jobStates.end(),
        expectedStates.begin(), expectedStates.end());
}

BOOST_AUTO_TEST_CASE(test_scheduler_cancel)
{
    const size_t THREADS_COUNT = 1;
    const size_t JOBS_COUNT = 5;
    const std::chrono::milliseconds JOB_DURATION_MS(200); // 0.2 sec
    std::atomic<bool> isCanceled(false);

    ThreadPool threadPool(THREADS_COUNT);

    Scheduler scheduler;
    scheduler.setIsCanceledChecker([&]{ return isCanceled.load(); });

    std::vector<JobState> jobStates;
    for (size_t i = 0; i < JOBS_COUNT; ++i) {
        jobStates.push_back(JobState::NotStarted);
        scheduler.addTask(
            [&, i](){
                std::this_thread::sleep_for(JOB_DURATION_MS);
                jobStates[i] = JobState::Done;
            },
            [&](const Scheduler::Runner& runner) { threadPool.push(runner); },
            {}
        );
    }

    auto fut = std::async(std::launch::async,
        [&scheduler]{ scheduler.executeAll(); });

    // Allow only the first and the second jobs to start
    std::this_thread::sleep_for(JOB_DURATION_MS * 1.5);
    isCanceled = true;
    BOOST_CHECK_THROW(fut.get(), ExecutionCanceled);

    std::vector<JobState> expectedStates(JOBS_COUNT, JobState::NotStarted);
    expectedStates[0] = JobState::Done;
    expectedStates[1] = JobState::Done;
    BOOST_CHECK_EQUAL_COLLECTIONS(jobStates.begin(), jobStates.end(),
        expectedStates.begin(), expectedStates.end());
}

} // namespace maps::wiki::tests

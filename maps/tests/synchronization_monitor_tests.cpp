#include <maps/automotive/parking/fastcgi/parking_api/lib/synchronization_monitor.h>

#include <library/cpp/testing/unittest/registar.h>

namespace maps::automotive::parking::tests {

using namespace std::literals::chrono_literals;

struct SynchronizationWaiter {
    void synchronize() {
        synchronized.store(true);
        lastSyncThreadId = std::this_thread::get_id();
        cv.notify_one();
    }

    template<typename Duration>
    void waitForSynchronization(Duration waitTime) {
        std::unique_lock<std::mutex> lock(m);
        cv.wait_for(lock, waitTime, [&]() { return synchronized.load(); });
    }

    void waitForSynchronization() {
        waitForSynchronization(2s);
    }

    std::atomic<bool> synchronized = false;
    std::condition_variable cv;
    std::mutex m;
    std::thread::id lastSyncThreadId;
};

Y_UNIT_TEST_SUITE(test_synchronization_monitor) {

    static constexpr auto DATA_SIZE_DIFFERENCE_THRESHOLD = 0.1;
    static constexpr auto DATA_SIZE_COMPARISON_LOWER_LIMIT = 100;

    Y_UNIT_TEST(check_starts_synchronization_when_needed)
    {
        SynchronizationWaiter waiter;
        SynchronizationMonitor monitor(
            DATA_SIZE_DIFFERENCE_THRESHOLD,
            DATA_SIZE_COMPARISON_LOWER_LIMIT,
            1h,
            [&]() { waiter.synchronize(); }
        );
        monitor.start();

        monitor.scheduleSynchronizationIfNeeded(1000, 0);

        waiter.waitForSynchronization();
        UNIT_ASSERT(waiter.synchronized);
        UNIT_ASSERT(std::this_thread::get_id() != waiter.lastSyncThreadId);

        waiter.synchronized = false;
        monitor.scheduleSynchronizationIfNeeded(1000, 800);

        waiter.waitForSynchronization();
        UNIT_ASSERT(waiter.synchronized);

        waiter.synchronized = false;
        monitor.stop();
        monitor.scheduleSynchronizationIfNeeded(1000, 0);

        waiter.waitForSynchronization();
        UNIT_ASSERT(!waiter.synchronized);

        monitor.start();
        waiter.waitForSynchronization();
        UNIT_ASSERT(waiter.synchronized);
    }

    Y_UNIT_TEST(check_doesnt_start_synchronization_when_not_needed)
    {
        SynchronizationWaiter waiter;
        SynchronizationMonitor monitor(
            DATA_SIZE_DIFFERENCE_THRESHOLD,
            DATA_SIZE_COMPARISON_LOWER_LIMIT,
            1h,
            [&]() { waiter.synchronize(); }
        );
        monitor.start();

        monitor.scheduleSynchronizationIfNeeded(0, 0);
        waiter.waitForSynchronization();
        UNIT_ASSERT(!waiter.synchronized);

        monitor.scheduleSynchronizationIfNeeded(1, 0);
        waiter.waitForSynchronization();
        UNIT_ASSERT(!waiter.synchronized);

        monitor.scheduleSynchronizationIfNeeded(0, 1);
        waiter.waitForSynchronization();
        UNIT_ASSERT(!waiter.synchronized);

        monitor.scheduleSynchronizationIfNeeded(2000, 3000);
        waiter.waitForSynchronization();
        UNIT_ASSERT(!waiter.synchronized);
    }

    Y_UNIT_TEST(check_starts_synchronization_periodically)
    {
        SynchronizationWaiter waiter;
        SynchronizationMonitor monitor(
            DATA_SIZE_DIFFERENCE_THRESHOLD,
            DATA_SIZE_COMPARISON_LOWER_LIMIT,
            1s,
            [&]() { waiter.synchronize(); }
        );
        monitor.start();

        waiter.waitForSynchronization();
        UNIT_ASSERT(waiter.synchronized);
    }
}

} // namespace maps::automotive::parking::tests

#include <maps/libs/concurrent/include/latch.h>
#include <maps/libs/common/include/exception.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <cstddef>
#include <stdexcept>
#include <thread>
#include <vector>
#include <atomic>

namespace maps::concurrent::tests {

Y_UNIT_TEST_SUITE(test_latch) {

Y_UNIT_TEST(must_be_unlocked_when_count_is_zero)
{
    constexpr size_t NUM_THREADS = 5;
    Latch latch(NUM_THREADS);

    EXPECT_FALSE(latch.allArrived());

    std::vector<std::thread> threads;
    for (size_t i = 0; i < NUM_THREADS; ++i) {
        threads.emplace_back([&] { latch.arrive(); });
    }
    for (auto&& thread : threads) {
        thread.join();
    }

    EXPECT_TRUE(latch.allArrived());
}

Y_UNIT_TEST(allows_basic_fork_join_synchronization)
{
    constexpr size_t NUM_THREADS = 5;
    constexpr size_t ITEMS_PER_THREAD = 100;
    std::vector<std::thread> threads;
    threads.reserve(NUM_THREADS);
    constexpr size_t EXPECTED_TOTAL = NUM_THREADS * ITEMS_PER_THREAD;
    Latch latch(EXPECTED_TOTAL);
    std::atomic_size_t itemsProcessed(0);

    for (size_t i = 0; i < NUM_THREADS; ++i) {
        threads.emplace_back([&](size_t limit) {
                                 for (size_t i = 0; i < limit; ++i) {
                                     ++itemsProcessed;
                                     latch.countDown(1);
                                 }
                             },
                             ITEMS_PER_THREAD);
    }
    latch.wait();

    EXPECT_EQ(EXPECTED_TOTAL, itemsProcessed);
    for (auto&& thread : threads) {
        thread.join();
    }
}

Y_UNIT_TEST(must_not_be_unlocked_before_all_arrived)
{
    std::atomic_size_t arrived(0);
    constexpr size_t NUM_THREADS = 5;
    Latch latch(NUM_THREADS);
    std::vector<std::thread> threads;
    for (size_t i = 0; i < NUM_THREADS - 1; ++i) {
        threads.emplace_back([&] {
            latch.arriveAndWait();
            ++arrived;
        });
    }
    EXPECT_FALSE(latch.allArrived());
    EXPECT_EQ(arrived.load(), 0u);
    latch.arriveAndWait();
    ++arrived;
    for (auto& thread : threads) {
        thread.join();
    }
    EXPECT_EQ(NUM_THREADS, arrived.load());
}

Y_UNIT_TEST(must_throw_when_too_many_arrived)
{
    Latch latch(2);
    EXPECT_THROW(latch.countDown(3), LogicError);
}

Y_UNIT_TEST(arrive_guards_must_trigger_on_scope_exit)
{
    Latch latch(1);
    EXPECT_FALSE(latch.allArrived());
    latch.arrive();
    EXPECT_TRUE(latch.allArrived());
}

Y_UNIT_TEST(arrive_can_be_waited_with_timeout)
{
    const std::chrono::milliseconds TIMEOUT(50);
    Latch latch(1);

    EXPECT_FALSE(latch.waitFor(TIMEOUT));

    latch.arrive();
    EXPECT_TRUE(latch.waitFor(TIMEOUT));
}

Y_UNIT_TEST(arrive_can_be_waited_until_timepoint)
{
    const std::chrono::milliseconds TIMEOUT(50);
    Latch latch(1);

    EXPECT_FALSE(  // in the past
        latch.waitUntil(std::chrono::steady_clock::now() - TIMEOUT)
    );
    EXPECT_FALSE(  // in the future
        latch.waitUntil(std::chrono::steady_clock::now() + TIMEOUT)
    );

    latch.arrive();
    EXPECT_TRUE(
        latch.waitUntil(std::chrono::steady_clock::now() + TIMEOUT)
    );
}

} //Y_UNIT_TEST_SUITE

} //namespace maps::concurrent::tests

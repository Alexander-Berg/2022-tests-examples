#include <maps/libs/concurrent/include/spin_lock.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <array>
#include <mutex>
#include <thread>

namespace maps::concurrent::tests {

Y_UNIT_TEST_SUITE(test_spin_lock) {

Y_UNIT_TEST(basic_test) {
    SpinLock lock;
    ASSERT_FALSE(lock.is_locked());
    {
        std::lock_guard guard(lock);
        ASSERT_TRUE(lock.is_locked());
    }
    ASSERT_FALSE(lock.is_locked());
}

Y_UNIT_TEST(multithreaded_test) {
    SpinLock lock;
    size_t counter = 0;
    constexpr size_t numIterations = 1000;
    constexpr size_t numThreads = 4;
    std::array<std::thread, numThreads> threads;
    for (auto& thread: threads) {
        thread = std::thread([&]() {
            for (size_t i = 0; i < numIterations; ++i) {
                std::lock_guard _(lock);
                ++counter;
            };
        });
    }
    for (auto& thread: threads) {
        thread.join();
    }
    EXPECT_EQ(counter, numIterations * numThreads);
}

} //Y_UNIT_TEST_SUITE

} //namespace maps::concurrent::tests

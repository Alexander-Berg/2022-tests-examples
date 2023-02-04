#include <maps/libs/concurrent/include/background_thread.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <atomic>
#include <chrono>
#include <thread>

namespace maps::concurrent::tests {

Y_UNIT_TEST_SUITE(test_background_thread) {

Y_UNIT_TEST(background_thread_works)
{
    std::atomic<size_t> counter{0};
    {
        BackgroundThread activity(
            [&]() {
                ++counter;
            },
            std::chrono::milliseconds(100)
        );
        activity.start();
        std::this_thread::sleep_for(std::chrono::seconds(1));
    }
    //Using non-strict check in order to avoid test flaps
    EXPECT_TRUE(counter >= 9 && counter <= 11);
}

Y_UNIT_TEST(background_thread_wait_first_run_works)
{
    std::atomic<size_t> counter{0};
    size_t savedCounter = 0;
    {
        BackgroundThread activity(
            [&]() {
                ++counter;
            },
            std::chrono::milliseconds(100)
        );
        activity.start();
        activity.waitFirstRun();
        savedCounter = counter;
        std::this_thread::sleep_for(std::chrono::milliseconds(300));
    }
    EXPECT_TRUE(savedCounter > 0);
    EXPECT_TRUE(counter > 1);
}

Y_UNIT_TEST(background_thread_delay_run_works)
{
    std::atomic<size_t> counter{0};
    size_t checkPoint1 = 0;
    size_t checkPoint2 = 0;
    {
        BackgroundThread activity(
            [&]() {
                ++counter;
            },
            std::chrono::milliseconds(1000)
        );
        activity.startDelayed();
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
        checkPoint1 = counter;
        activity.waitFirstRun();
        checkPoint2 = counter;
        std::this_thread::sleep_for(std::chrono::milliseconds(2000));
    }
    EXPECT_EQ(checkPoint1, 0u);
    EXPECT_EQ(checkPoint2, 1u);
    EXPECT_TRUE(counter > 1);
}

Y_UNIT_TEST(background_thread_delay_run_cancel_on_overdue)
{
    std::atomic<size_t> counter{0};
    {
        BackgroundThread activity(
            [&]() {
                ++counter;
            },
            std::chrono::milliseconds(500)
        );
        activity.startDelayed();
        // Let background thread live some time less than its next run.
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
    }
    // Background thread is destroyed, counter shouldn't increment.
    std::this_thread::sleep_for(std::chrono::milliseconds(1000));
    EXPECT_EQ(counter, 0u);
}

Y_UNIT_TEST(backround_thread_exits_early)
{
    std::atomic<size_t> counter{0};
    auto start = std::chrono::system_clock::now();
    {
        BackgroundThread activity(
            [&]() {
                ++counter;
            },
            std::chrono::seconds(1000)
        );
        activity.start();
        std::this_thread::sleep_for(std::chrono::seconds(1));
    }
    auto finish = std::chrono::system_clock::now();
    EXPECT_EQ(counter, 1u);
    //Using non-strict check in order to avoid test flaps
    EXPECT_LT(
        std::chrono::duration_cast<std::chrono::milliseconds>(finish - start),
        std::chrono::milliseconds(2000)
    );
}

Y_UNIT_TEST(background_thread_can_be_woken_up)
{
    std::atomic<size_t> counter{0};
    BackgroundThread activity(
        [&]() {
            ++counter;
        },
        std::chrono::seconds(1000)
    );
    activity.start();

    std::this_thread::sleep_for(std::chrono::milliseconds(500));
    EXPECT_EQ(counter, 1u);
    activity.wakeUp();
    std::this_thread::sleep_for(std::chrono::milliseconds(500));
    EXPECT_EQ(counter, 2u);
    activity.wakeUp();
    std::this_thread::sleep_for(std::chrono::milliseconds(500));
    EXPECT_EQ(counter, 3u);
}

} //Y_UNIT_TEST_SUITE

} // namespace maps::concurrent::tests

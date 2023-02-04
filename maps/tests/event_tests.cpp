#include <maps/libs/concurrent/include/event.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <chrono>
#include <thread>

namespace maps::concurrent::tests {

using namespace std::chrono_literals;

Y_UNIT_TEST_SUITE(test_single_wait_event)
{

Y_UNIT_TEST(simple_wait)
{
    SingleWaitEvent event;
    auto start = std::chrono::high_resolution_clock::now();
    std::thread notifier([&event] {
        std::this_thread::sleep_for(2s);
        event.notifyOne();
    });
    event.wait();
    auto end = std::chrono::high_resolution_clock::now();
    notifier.join();
    auto passed = std::chrono::duration_cast<std::chrono::seconds>(end - start);
    EXPECT_GE(passed, 2s);
}

Y_UNIT_TEST(concurrent_invocation)
{
    SingleWaitEvent event;
    auto start = std::chrono::high_resolution_clock::now();
    event.notifyOne();
    // wait should immediately exit with invocation status equal to true
    EXPECT_TRUE(event.waitFor(2s));
    auto end = std::chrono::high_resolution_clock::now();
    auto passed = std::chrono::duration_cast<std::chrono::seconds>(end - start);
    EXPECT_LT(passed, 2s);
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::concurrent::tests

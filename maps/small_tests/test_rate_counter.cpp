#include <yandex/maps/wiki/common/rate_counter.h>

#include <library/cpp/testing/unittest/registar.h>
#include <thread>

namespace maps::wiki::common::tests {

namespace {
const std::string TEST_KEY = "123";
const size_t CACHE_SIZE = 10;
const std::chrono::milliseconds CACHE_TIME(500);
} // namespace

Y_UNIT_TEST_SUITE(rate_counter) {

Y_UNIT_TEST(test_add_simple)
{
    RateCounter<std::string> rateCounter(CACHE_SIZE, CACHE_TIME);
    UNIT_ASSERT_EQUAL(rateCounter.add(TEST_KEY), 1);
    UNIT_ASSERT_EQUAL(rateCounter.add(TEST_KEY), 2);
    UNIT_ASSERT_EQUAL(rateCounter.add(TEST_KEY), 3);
    std::this_thread::sleep_for(CACHE_TIME * 2);
    UNIT_ASSERT_EQUAL(rateCounter.add(TEST_KEY), 1);
}

Y_UNIT_TEST(test_add_multikeys)
{
    const std::vector<std::string> keys = {
        TEST_KEY + "_0",
        TEST_KEY + "_1",
        TEST_KEY + "_2",
        TEST_KEY + "_3",
    };
    RateCounter<std::string> rateCounter(CACHE_SIZE, CACHE_TIME);
    for (const auto& key : keys) {
        UNIT_ASSERT_EQUAL(rateCounter.add(key), 1);
        UNIT_ASSERT_EQUAL(rateCounter.add(key), 2);
        UNIT_ASSERT_EQUAL(rateCounter.add(key), 3);
    }
}

Y_UNIT_TEST(test_rate)
{
    RateCounter<std::string> rateCounter(CACHE_SIZE, std::chrono::milliseconds(500));
    for (size_t pass = 1; pass <= 5; ++pass) {
        UNIT_ASSERT_EQUAL(rateCounter.add(TEST_KEY), pass);
        std::this_thread::sleep_for(std::chrono::milliseconds(90)); // 90, 180, 270, 360, 450 ms
    }
    std::this_thread::sleep_for(std::chrono::milliseconds(200)); // ~650 ms, -2 values (90, 180)
    UNIT_ASSERT_EQUAL(rateCounter.add(TEST_KEY), 4); // 3 -> 4
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::common::tests

#include <maps/libs/chrono/include/format.h>
#include <maps/libs/chrono/include/days.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace maps::chrono::tests {
using namespace std::literals::chrono_literals;
using namespace maps::chrono::literals;

Y_UNIT_TEST_SUITE(test_format_duration) {

Y_UNIT_TEST(test_print_short_name) {
    std::stringstream ss;
    ss
        << format(1_days) << ", "
        << format(2h) << ", "
        << format(3min) << ", "
        << format(4s) << ", "
        << format(5ms) << ", "
        << format(6us) << ", "
        << format(7ns);
    EXPECT_EQ(ss.str(),
        "1 d, 2 h, 3 min, 4 s, 5 ms, 6 us, 7 ns");
}

Y_UNIT_TEST(test_to_string) {
    EXPECT_EQ(format(1_days).toString(), "1 d");
    EXPECT_EQ(format(2h).toString(), "2 h");
    EXPECT_EQ(format(3min).toString(), "3 min");
    EXPECT_EQ(format(4s).toString(), "4 s");
    EXPECT_EQ(format(5ms).toString(), "5 ms");
    EXPECT_EQ(format(6us).toString(), "6 us");
    EXPECT_EQ(format(7ns).toString(), "7 ns");
}

Y_UNIT_TEST(test_print_short_name_with_larger_period) {
    std::stringstream ss;
    ss
        << format<std::chrono::hours>(1_days) << ", "
        << format<std::chrono::minutes>(2h) << ", "
        << format<std::chrono::seconds>(3min) << ", "
        << format<std::chrono::milliseconds>(4s) << ", "
        << format<std::chrono::microseconds>(5ms) << ", "
        << format<std::chrono::nanoseconds>(6us);
    EXPECT_EQ(ss.str(),
        "24 h, 120 min, 180 s, 4000 ms, 5000 us, 6000 ns");
}

Y_UNIT_TEST(test_print_short_name_with_shorter_period) {
    std::stringstream ss;
    ss
        << format<maps::chrono::Days>(3h) << ", "
        << format<std::chrono::hours>(3min) << ", "
        << format<std::chrono::minutes>(6s) << ", "
        << format<std::chrono::seconds>(5ms) << ", "
        << format<std::chrono::milliseconds>(6us) << ", "
        << format<std::chrono::microseconds>(7ns);
    EXPECT_EQ(ss.str(),
        "0.125 d, 0.05 h, 0.1 min, 0.005 s, 0.006 ms, 0.007 us");
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::chrono::tests

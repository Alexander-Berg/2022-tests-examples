#include <maps/factory/libs/common/timers.h>

#include <maps/factory/libs/unittest/tests_common.h>

namespace maps::factory::tests {

Y_UNIT_TEST_SUITE(intervals_should) {

Y_UNIT_TEST(print_one_interval)
{
    TimeIntervals timer("Call");
    EXPECT_THAT(timer.toString(), MatchesRegex(
        R"(Call 0.0+ms \(\))"));
    timer.end();
    EXPECT_THAT(timer.toString(), MatchesRegex(
        R"(Call [0-9]+(\.?[0-9]+)?ms \( [0-9]+(\.?[0-9]+)?ms \))"));
}

Y_UNIT_TEST(print_many_intervals)
{
    TimeIntervals timer("Call");
    EXPECT_THAT(timer.toString(), MatchesRegex(
        R"(Call 0.0+ms \(\))"));
    timer.end("first");
    EXPECT_THAT(timer.toString(), MatchesRegex(
        R"(Call [0-9]+(\.?[0-9]+)?ms \( first [0-9]+(\.?[0-9]+)?ms \))"));
    timer.end("second");
    EXPECT_THAT(timer.toString(), MatchesRegex(
        R"(Call [0-9]+(\.?[0-9]+)?ms \( first [0-9]+(\.?[0-9]+)?ms \+ second [0-9]+(\.?[0-9]+)?ms \))"));
}

} // suite

} // namespace maps::factory::tests

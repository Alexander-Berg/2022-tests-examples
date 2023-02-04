#include <maps/factory/libs/common/functional.h>

#include <maps/factory/libs/unittest/tests_common.h>

namespace maps::factory::tests {

Y_UNIT_TEST_SUITE(functional_should) {

Y_UNIT_TEST(map_vector)
{
    const std::vector<int> vec{1, 2};
    const double arr[]{3.0, 4.0};
    const auto adder = [&](auto v) { return v + 1; };
    EXPECT_THAT(map(vec, adder), ElementsAre(2, 3));
    EXPECT_THAT(map(arr, adder), ElementsAre(4.0, 5.0));
}

Y_UNIT_TEST(filter_vector)
{
    const std::vector<int> vec{-1, 0, 1, 2};
    const double arr[]{-2.0, 0.0, 3.0, 4.0};
    const auto isPositive = [&](auto v) { return v > 0; };
    EXPECT_THAT(filter(vec, isPositive), ElementsAre(1, 2));
    EXPECT_THAT(filter(arr, isPositive), ElementsAre(3.0, 4.0));
}

Y_UNIT_TEST(map_optional)
{
    const std::optional<int> opt1{1};
    const std::optional<int> opt2{};
    const auto adder = [&](auto v) { return v + 1; };
    EXPECT_THAT(map(opt1, adder), Eq(2));
    EXPECT_THAT(map(opt2, adder), Eq(std::nullopt));
}

} // suite
} //namespace maps::factory::tests

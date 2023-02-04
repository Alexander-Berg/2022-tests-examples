#include <maps/analyzer/libs/utils/include/constexpr_pow.h>

#include <library/cpp/testing/gtest/gtest.h>

using maps::analyzer::constexprPow;

TEST(ConstexprPowTest, ConstexprPow) {
    EXPECT_EQ(constexprPow(10., 0u), 1.);
    EXPECT_EQ(constexprPow(10., 1u), 10.);
    EXPECT_EQ(constexprPow(10., 2u), 100.);
    EXPECT_EQ(constexprPow(10., 3u), 1000.);
    EXPECT_EQ(constexprPow(10., 4u), 10000.);
    EXPECT_EQ(constexprPow(10., 5u), 100000.);
}

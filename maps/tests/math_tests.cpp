#include <maps/libs/common/include/math.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::common::tests {

TEST(Math_tests, kahan_summation_test)
{
    const auto intToDouble = [](int i) -> double { return i; };
    std::vector<int> ints{1, 2, 3, 4};
    EXPECT_EQ(
        sumKahan(
            ints.begin(), ints.end(),
            intToDouble
        ),
        10.0
    );
    EXPECT_EQ(
        sumKahan(
            ints.cbegin(), ints.cend(),
            intToDouble
        ),
        10.0
    );
    EXPECT_EQ(
        sumKahan(
            ints
        ),
        10.0
    );

    std::vector<double> doubles(/* count */ 100000000, /* value */ 0.00000001);
    double properSum = sumKahan(
        doubles
    );
    EXPECT_TRUE(properSum == 1.0);

    //checking if stupid summation algorithm isn't working properly
    double stupidSum = 0;
    for (auto d: doubles) {
        stupidSum += d;
    }
    EXPECT_TRUE(stupidSum != 1);
}

TEST(Math_tests, partial_kahan_summation_test)
{
    const auto intToDouble = [](int i) -> double { return i; };
    std::vector<int> ints{1, 2, 3, 4};
    std::vector<double> partialSum(ints.size());
    EXPECT_EQ(
        partialSumKahan(
            ints.begin(), ints.end(),
            partialSum.begin(),
            intToDouble
        ),
        10.0
    );
    EXPECT_THAT(
        partialSum,
        testing::ElementsAre(1.0, 3.0, 6.0, 10.0)
    );
}

} // namespace maps::common::tests

#include <maps/factory/libs/common/minmax.h>

#include <maps/factory/libs/unittest/tests_common.h>

namespace maps::factory::tests {

Y_UNIT_TEST_SUITE(minmax_should) {

using Eigen::VectorXd;
using Eigen::VectorXi;

constexpr double EPS = 1e-12;

Y_UNIT_TEST(calculate_min_and_max_of_vector)
{
    VectorXd data = VectorXd::Random(200);
    MinMax<1> mm;
    mm.addRows(data);
    const double min = data.array().minCoeff();
    const double max = data.array().maxCoeff();
    EXPECT_NEAR(mm.min()(0), min, EPS);
    EXPECT_NEAR(mm.max()(0), max, EPS);
    EXPECT_NEAR(mm.range()(0), max - min, EPS);
    EXPECT_FALSE(mm.isEmpty());
}

Y_UNIT_TEST(calculate_min_and_max_of_one_element)
{
    const int val = 42;
    VectorXi data = VectorXi::Constant(1, val);
    MinMax<1> mm;
    mm.addRows(data);
    EXPECT_EQ(mm.min()(0), val);
    EXPECT_EQ(mm.max()(0), val);
    EXPECT_EQ(mm.range()(0), 0);
    EXPECT_FALSE(mm.isEmpty());;
}

Y_UNIT_TEST(be_empty_when_nothing_added)
{
    MinMax<1> mm;
    EXPECT_TRUE(mm.isEmpty());
}

} // suite
} //namespace maps::factory::tests

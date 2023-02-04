#include <maps/factory/libs/common/intersection_over_union.h>

#include <maps/factory/libs/unittest/tests_common.h>

namespace maps::factory::tests {

Y_UNIT_TEST_SUITE(iou_should) {

Y_UNIT_TEST(all_false)
{
    IntersectionOverUnion iou(Eigen::Array4d::Zero(), Eigen::Array4d::Zero());
    EXPECT_EQ(iou.total(), 4);
    EXPECT_EQ(iou.truePositive(), 0);
    EXPECT_EQ(iou.trueNegative(), 4);
    EXPECT_EQ(iou.falsePositive(), 0);
    EXPECT_EQ(iou.falseNegative(), 0);
    EXPECT_DOUBLE_EQ(iou.iou(), 0);
    EXPECT_DOUBLE_EQ(iou.precision(), 0);
    EXPECT_DOUBLE_EQ(iou.recall(), 0);
    EXPECT_DOUBLE_EQ(iou.sensitivity(), 1);
    EXPECT_DOUBLE_EQ(iou.accuracy(), 1);
    EXPECT_DOUBLE_EQ(iou.balancedAccuracy(), 0.5);
    EXPECT_DOUBLE_EQ(iou.f1Score(), 0);
}

Y_UNIT_TEST(all_true)
{
    IntersectionOverUnion iou(Eigen::Array4d::Ones(), Eigen::Array4d::Ones());
    EXPECT_EQ(iou.truePositive(), 4);
    EXPECT_EQ(iou.trueNegative(), 0);
    EXPECT_EQ(iou.falsePositive(), 0);
    EXPECT_EQ(iou.falseNegative(), 0);
    EXPECT_DOUBLE_EQ(iou.iou(), 1);
    EXPECT_DOUBLE_EQ(iou.precision(), 1);
    EXPECT_DOUBLE_EQ(iou.recall(), 1);
    EXPECT_DOUBLE_EQ(iou.sensitivity(), 0);
    EXPECT_DOUBLE_EQ(iou.accuracy(), 1);
    EXPECT_DOUBLE_EQ(iou.balancedAccuracy(), 0.5);
    EXPECT_DOUBLE_EQ(iou.f1Score(), 1);
}

Y_UNIT_TEST(all_false_negative)
{
    IntersectionOverUnion iou(Eigen::Array4d::Zero(), Eigen::Array4d::Ones());
    EXPECT_EQ(iou.truePositive(), 0);
    EXPECT_EQ(iou.trueNegative(), 0);
    EXPECT_EQ(iou.falsePositive(), 0);
    EXPECT_EQ(iou.falseNegative(), 4);
    EXPECT_DOUBLE_EQ(iou.iou(), 0);
    EXPECT_DOUBLE_EQ(iou.precision(), 0);
    EXPECT_DOUBLE_EQ(iou.recall(), 0);
    EXPECT_DOUBLE_EQ(iou.sensitivity(), 0);
    EXPECT_DOUBLE_EQ(iou.accuracy(), 0);
    EXPECT_DOUBLE_EQ(iou.balancedAccuracy(), 0);
    EXPECT_DOUBLE_EQ(iou.f1Score(), 0);
}

Y_UNIT_TEST(all_false_positive)
{
    IntersectionOverUnion iou(Eigen::Array4d::Ones(), Eigen::Array4d::Zero());
    EXPECT_EQ(iou.truePositive(), 0);
    EXPECT_EQ(iou.trueNegative(), 0);
    EXPECT_EQ(iou.falsePositive(), 4);
    EXPECT_EQ(iou.falseNegative(), 0);
    EXPECT_DOUBLE_EQ(iou.iou(), 0);
    EXPECT_DOUBLE_EQ(iou.precision(), 0);
    EXPECT_DOUBLE_EQ(iou.recall(), 0);
    EXPECT_DOUBLE_EQ(iou.sensitivity(), 0);
    EXPECT_DOUBLE_EQ(iou.accuracy(), 0);
    EXPECT_DOUBLE_EQ(iou.balancedAccuracy(), 0);
    EXPECT_DOUBLE_EQ(iou.f1Score(), 0);
}

Y_UNIT_TEST(different_cases)
{
    Eigen::Array4d given, real;
    given << 0, 1, 0, 1;
    real << 0, 0, 1, 1;
    IntersectionOverUnion iou(given, real);
    EXPECT_EQ(iou.total(), 4);
    EXPECT_EQ(iou.truePositive(), 1);
    EXPECT_EQ(iou.trueNegative(), 1);
    EXPECT_EQ(iou.falsePositive(), 1);
    EXPECT_EQ(iou.falseNegative(), 1);
    EXPECT_DOUBLE_EQ(iou.iou(), 1.0 / 3.0);
    EXPECT_DOUBLE_EQ(iou.precision(), 0.5);
    EXPECT_DOUBLE_EQ(iou.recall(), 0.5);
    EXPECT_DOUBLE_EQ(iou.sensitivity(), 0.5);
    EXPECT_DOUBLE_EQ(iou.accuracy(), 0.5);
    EXPECT_DOUBLE_EQ(iou.balancedAccuracy(), 0.5);
    EXPECT_DOUBLE_EQ(iou.f1Score(), 0.5);
}

Y_UNIT_TEST(merge)
{
    IntersectionOverUnion iou;
    iou += IntersectionOverUnion(Eigen::Array2d(0, 1), Eigen::Array2d(0, 0));
    iou += IntersectionOverUnion(Eigen::Array2d(0, 1), Eigen::Array2d(1, 1));
    EXPECT_EQ(iou.total(), 4);
    EXPECT_EQ(iou.truePositive(), 1);
    EXPECT_EQ(iou.trueNegative(), 1);
    EXPECT_EQ(iou.falsePositive(), 1);
    EXPECT_EQ(iou.falseNegative(), 1);
    EXPECT_DOUBLE_EQ(iou.iou(), 1.0 / 3.0);
}

} // suite

} // namespace maps::factory::tests

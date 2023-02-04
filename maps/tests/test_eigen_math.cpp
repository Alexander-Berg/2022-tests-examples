#include <maps/factory/libs/common/eigen.h>

#include <maps/factory/libs/unittest/tests_common.h>

namespace maps::factory::tests {

Y_UNIT_TEST_SUITE(eigen_math_should) {

Y_UNIT_TEST(is_near_int)
{
    EXPECT_TRUE(isNearInt(-10));
    EXPECT_TRUE(isNearInt(0));
    EXPECT_TRUE(isNearInt(0.999999));
    EXPECT_TRUE(isNearInt(1));
    EXPECT_TRUE(isNearInt(100));
    EXPECT_TRUE(isNearInt(1.000001));

    EXPECT_FALSE(isNearInt(-10.5));
    EXPECT_FALSE(isNearInt(0.5));
    EXPECT_FALSE(isNearInt(0.9));
    EXPECT_FALSE(isNearInt(1.1));
    EXPECT_FALSE(isNearInt(100.5));
}

Y_UNIT_TEST(is_near)
{
    constexpr double eps = 1e-6;
    EXPECT_TRUE(isNear(-10, -10));
    EXPECT_TRUE(isNear(-10, -10 - eps));
    EXPECT_TRUE(isNear(0, eps));
    EXPECT_TRUE(isNear(1 - eps, 1));
    EXPECT_TRUE(isNear(1, 1 + eps));
    EXPECT_TRUE(isNear(100, 100 + eps));
    EXPECT_TRUE(isNear(1 + eps, 1));
    EXPECT_TRUE(isNear(10, 10));

    EXPECT_FALSE(isNear(-10, -11));
    EXPECT_FALSE(isNear(-10, 11));
    EXPECT_FALSE(isNear(-10, -10.1));
    EXPECT_FALSE(isNear(0, 0.1));
    EXPECT_FALSE(isNear(1 - eps, 0.9));
    EXPECT_FALSE(isNear(1, 1.1));
    EXPECT_FALSE(isNear(100, 100.1));
    EXPECT_FALSE(isNear(1.1, 1));
    EXPECT_FALSE(isNear(10, 11));
    EXPECT_FALSE(isNear(10, -11));
}

Y_UNIT_TEST(ceil_div)
{
    for (int a = 0; a <= 10; ++a) {
        for (int b = 1; b <= 10; ++b) {
            EXPECT_EQ(ceilDiv(a, b), static_cast<int>(std::ceil(1.0 * a / b)));
        }
    }
    EXPECT_THAT(ceilDiv(Array2i{10, 20}, Array2i{3, 7}), EigEq(Array2i{4, 3}));
}

Y_UNIT_TEST(floor_div)
{
    for (int a = 0; a <= 10; ++a) {
        for (int b = 1; b <= 10; ++b) {
            EXPECT_EQ(floorDiv(a, b), static_cast<int>(std::floor(1.0 * a / b)));
        }
    }
    EXPECT_THAT(floorDiv(Array2i{10, 20}, Array2i{3, 7}), EigEq(Array2i{3, 2}));
}

Y_UNIT_TEST(create_box)
{
    Box2i expected{Array2i{10, 20}, Array2i{30, 40}};
    EXPECT_THAT(boxFromPoints(10, 20, 30, 40), EigEq(expected));
    EXPECT_THAT(boxFromPoints(Array2i{10, 20}, Array2i{30, 40}), EigEq(expected));
    EXPECT_THAT(boxFromPoints(Array2i{30, 40}, Array2i{10, 20}), EigEq(expected));
    EXPECT_THAT(boxFromPoints(Array2i{30, 20}, Array2i{10, 40}), EigEq(expected));

    EXPECT_THAT(boxFromOriginAndSizes(Array2i{10, 20}, Array2i{20, 20}), EigEq(expected));
    EXPECT_THAT(boxFromOriginAndSizes(Array2i{30, 20}, Array2i{-20, 20}), EigEq(expected));
    EXPECT_THAT(boxFromOriginAndSizes(Array2i{30, 40}, Array2i{-20, -20}), EigEq(expected));
    EXPECT_THAT(boxFromOriginAndSize(Array2i{10, 20}, 20), EigEq(expected));
    EXPECT_THAT(boxFromOriginAndSize(Array2i{30, 40}, -20), EigEq(expected));

    EXPECT_THAT(boxFromSizes(10, 20), EigEq(Box2i{Array2i{0, 0}, Array2i{10, 20}}));
    EXPECT_THAT(boxFromSizes(Array2i{10, 20}), EigEq(Box2i{Array2i{0, 0}, Array2i{10, 20}}));
    EXPECT_THAT(boxFromSizes(Array2i{-10, 20}), EigEq(Box2i{Array2i{-10, 0}, Array2i{0, 20}}));
    EXPECT_THAT(boxFromSizes(Array2i{-10, -20}), EigEq(Box2i{Array2i{-10, -20}, Array2i{0, 0}}));
    EXPECT_THAT(boxFromSize(20), EigEq(Box2i{Array2i{0, 0}, Array2i{20, 20}}));
    EXPECT_THAT(boxFromSize(-20), EigEq(Box2i{Array2i{-20, -20}, Array2i{0, 0}}));

    EXPECT_THAT(boxFromTileOffsetAndSize(Array2i{10, 12}, 5),
        EigEq(Box2i{Array2i{50, 60}, Array2i{55, 65}}));
}

Y_UNIT_TEST(trim_box)
{
    {
        auto box = boxFromPoints(10, 20, 30, 40);
        auto expected = boxFromPoints(10, 20, 30, 30);
        EXPECT_THAT(trimmed(box, Array2i{35, 30}), EigEq(expected));
    }
    {
        auto box = boxFromPoints(-10, -20, 10, 20);
        auto expected = boxFromPoints(0, 0, 10, 20);
        EXPECT_THAT(trimmed(box, Array2i{35, 30}), EigEq(expected));
    }
    {
        auto box = boxFromPoints(-10, -20, 0, 0);
        auto result = trimmed(box, Array2i{35, 30});
        EXPECT_FALSE(result.isEmpty());
        EXPECT_FALSE(isPositive(result));
    }
    {
        auto box = boxFromPoints(-10, -20, -1, 0);
        EXPECT_TRUE(trimmed(box, Array2i{35, 30}).isEmpty());
    }
}

Y_UNIT_TEST(transform_box)
{
    auto box = boxFromPoints<double>(10, 20, 30, 40);
    auto expected = boxFromPoints<double>(110, 220, 130, 240);
    Affine2d transform{Eigen::Translation2d{100, 200}};
    EXPECT_THAT(transformed(box, transform), EigEq(expected));
    EXPECT_THAT((transform * box), EigEq(expected));
}

Y_UNIT_TEST(uniform_scale_box)
{
    auto box = boxFromPoints(10, 20, 30, 40);
    auto expected = boxFromPoints(20, 40, 60, 80);
    EXPECT_THAT(scaled(box, 2), EigEq(expected));
    EXPECT_THAT((box * 2), EigEq(expected));
    EXPECT_THAT((2 * box), EigEq(expected));
}

Y_UNIT_TEST(scale_box)
{
    auto box = boxFromPoints(10, 20, 30, 40);
    auto expected = boxFromPoints(10, 40, 30, 80);
    const Array2i factor{1, 2};
    EXPECT_THAT(scaled(box, factor), EigEq(expected));
    EXPECT_THAT((box * factor), EigEq(expected));
    EXPECT_THAT((factor * box), EigEq(expected));
}

Y_UNIT_TEST(buffer_box)
{
    auto box = boxFromPoints(10, 20, 30, 40);
    auto expected = boxFromPoints(9, 19, 31, 41);
    EXPECT_THAT(buffered(box, 1), EigEq(expected));
}

Y_UNIT_TEST(round_box)
{
    auto box = boxFromPoints<double>(10.5, 20.5, 30.5, 40.5);
    auto expected = boxFromPoints<double>(11, 21, 30, 40);
    EXPECT_THAT(roundedInwards(box), EigEq(expected));
}

Y_UNIT_TEST(check_box_has_volume)
{
    auto box = boxFromPoints(10, 20, 30, 40);
    auto box2 = boxFromPoints(10, 20, 30, 20);
    auto box3 = Box2i{};
    EXPECT_TRUE(isPositive(box));
    EXPECT_FALSE(isPositive(box2));
    EXPECT_FALSE(isPositive(box3));
    EXPECT_FALSE(!box);
    EXPECT_TRUE(!box2);
    EXPECT_TRUE(!box3);
}

Y_UNIT_TEST(convert_zero_volume_box_to_empty)
{
    auto box = boxFromPoints(10, 20, 30, 40);
    auto box2 = boxFromPoints(10, 20, 30, 20);
    auto box3 = Box2i{};
    EXPECT_TRUE(normalized(box).isApprox(box));
    EXPECT_TRUE(normalized(box2).isEmpty());
    EXPECT_TRUE(normalized(box3).isEmpty());
}

Y_UNIT_TEST(get_box_points)
{
    Eigen::Matrix<int, 4, 2> expected;
    expected <<
        10, 20,
        30, 20,
        10, 40,
        30, 40;
    EXPECT_THAT(points(boxFromPoints(10, 20, 30, 40)), EigEq(expected));
}

Y_UNIT_TEST(get_box_from_points)
{
    Eigen::Matrix<int, 6, 2> points;
    points <<
        10, 20,
        30, 20,
        10, 40,
        30, 40,
        15, 25,
        25, 35;
    EXPECT_THAT(boundsFromPoints<int>(points), EigEq(boxFromPoints(10, 20, 30, 40)));
}

Y_UNIT_TEST(get_bounds_from_geo_transform_and_size)
{
    Affine2d pixToCoord;
    pixToCoord.matrix() <<
        10, 0, 20,
        0, 30, 40,
        0, 0, 1;
    EXPECT_THAT(boundsFromTransform(pixToCoord, Array2i(100, 200)), EigEq(
        boxFromPoints<double>(20, 40, 20 + 10 * 100, 40 + 30 * 200)));
}

Y_UNIT_TEST(swap_x_y)
{
    EXPECT_THAT(swapAxis(boxFromPoints(10, 20, 30, 40)), EigEq(
        boxFromPoints(20, 10, 40, 30)));
}

Y_UNIT_TEST(random_double_point)
{
    const Box2d box(Point2d(-1, 3), Point2d(4, 5));
    for (int i = 0; i < 10; ++i) {
        const Point2d point = randomPoint(box);
        EXPECT_TRUE(box.contains(point)) << PrintVector(point);
    }
}

Y_UNIT_TEST(random_int_point)
{
    const Box2i box(Point2i(-1, 3), Point2i(4, 5));
    for (int i = 0; i < 10; ++i) {
        const Point2i point = randomPoint(box);
        EXPECT_TRUE(box.contains(point)) << PrintVector(point);
    }
}

} // suite

} // namespace maps::factory::tests

#include <maps/factory/libs/image/multipoint_brush.h>

#include <maps/factory/libs/unittest/tests_common.h>

namespace maps::factory::image::tests {
using namespace maps::factory::tests;
using namespace geometry;

Y_UNIT_TEST_SUITE(multipoint_brush_should) {

std::string imgFile() { return SRC_("data/multipoint_brush.png"); }

MultiPoint3i imgPoints()
{
    return {
        {4, 2, 48}, {5, 2, 77}, {3, 3, 48}, {4, 3, 129},
        {5, 3, 174}, {6, 3, 102}, {3, 4, 77}, {4, 4, 174},
        {5, 4, 220}, {6, 4, 139}, {4, 5, 102}, {5, 5, 139},
    };
}

Y_UNIT_TEST(get_3d_points_from_image)
{
    const UInt8Image img = UInt8Image::fromFile(imgFile());
    const MultiPointBrush3i<uint8_t> brush(img, 0);
    EXPECT_EQ(brush.points(), imgPoints());
}

Y_UNIT_TEST(draw_3d_points)
{
    MultiPointBrush3i<uint8_t> brush(imgPoints());
    const UInt8Image expected = UInt8Image::fromFile(imgFile());
    const UInt8Image img = brush.draw(expected.size(), expected.bands());
    EXPECT_EQ(img, expected);
}

Y_UNIT_TEST(get_2d_points_from_image)
{
    const UInt8Image img = UInt8Image::fromFile(imgFile());
    const MultiPointBrush2i<uint8_t> brush(img, 0);
    EXPECT_EQ(brush.points(), imgPoints().withoutZ());
}

Y_UNIT_TEST(draw_2d_points_with_color)
{
    const uint8_t color = 55;
    MultiPointBrush2i<uint8_t> brush(imgPoints().withoutZ(), Color::constant(color));
    UInt8Image expected = UInt8Image::fromFile(imgFile());
    expected.forEach([&](uint8_t& v) { if (v != 0) { v = color; }});
    const UInt8Image img = brush.draw(expected.size(), expected.bands());
    EXPECT_EQ(img, expected);
}

Y_UNIT_TEST(skip_outside_points)
{
    const int size = 8;
    const MultiPoint2i points{{-1, 1}, {1, -1}, {size, 1}, {1, size}};
    MultiPointBrush2i<uint8_t> brush(points, Color::white<uint8_t>());
    const UInt8Image img = brush.draw(Size2i::Constant(size), 1, Color::black());
    EXPECT_TRUE(img.isZero());
}

Y_UNIT_TEST(draw_corner_points)
{
    const int size = 4;
    const int last = size - 1;
    const MultiPoint2i points{{0, 0}, {last, 0}, {0, last}, {last, last}};
    MultiPointBrush2i<uint8_t> brush(points, Color::white<uint8_t>());
    const UInt8Image img = brush.draw(Size2i::Constant(size), 1, Color::black());
    UInt8Image expected = UInt8Image::zero(Size2i::Constant(size), 1);
    expected.val(0, 0, 0) = 255;
    expected.val(last, 0, 0) = 255;
    expected.val(0, last, 0) = 255;
    expected.val(last, last, 0) = 255;
    EXPECT_EQ(img, expected);
}

Y_UNIT_TEST(brush_from_corner_points)
{
    const int size = 4;
    const int last = size - 1;
    UInt8Image img = UInt8Image::zero(Size2i::Constant(size), 1);
    img.val(0, 0, 0) = 255;
    img.val(last, 0, 0) = 255;
    img.val(0, last, 0) = 255;
    img.val(last, last, 0) = 255;
    MultiPointBrush2i<uint8_t> brush(img, 0);
    const UInt8Image result = brush.draw(Size2i::Constant(size), 1, Color::black());
    EXPECT_EQ(result, img);
}

} // suite

} // namespace maps::factory::image::tests

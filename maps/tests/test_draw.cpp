#include <maps/factory/libs/image/draw.h>

#include <maps/factory/libs/geometry/tiles.h>

#include <maps/factory/libs/unittest/tests_common.h>

namespace maps::factory::image::tests {
using namespace maps::factory::tests;
using namespace geometry;

Y_UNIT_TEST_SUITE(draw_should) {

Y_UNIT_TEST(draw_line_8_connected)
{
    const Array2i size(5, 5);
    UInt8Image img(size, 1);
    const auto line = [&](double x0, double y0, double x1, double y1) {
        img.setZero();
        drawLine(img, {{x0, y0}, {x1, y1}}, Color(8), false);
    };
    line(0, 0, 4, 4);
    EXPECT_EQ(img, UInt8Image(size, 1, {
        8, 0, 0, 0, 0,
        0, 8, 0, 0, 0,
        0, 0, 8, 0, 0,
        0, 0, 0, 8, 0,
        0, 0, 0, 0, 8,
    }));
    line(3, 0, 1, 4);
    EXPECT_EQ(img, UInt8Image(size, 1, {
        0, 0, 0, 8, 0,
        0, 0, 8, 0, 0,
        0, 0, 8, 0, 0,
        0, 8, 0, 0, 0,
        0, 8, 0, 0, 0,
    }));
    line(2, 1, 2, 3);
    EXPECT_EQ(img, UInt8Image(size, 1, {
        0, 0, 0, 0, 0,
        0, 0, 8, 0, 0,
        0, 0, 8, 0, 0,
        0, 0, 8, 0, 0,
        0, 0, 0, 0, 0,
    }));
    line(1, 2, 3, 2);
    EXPECT_EQ(img, UInt8Image(size, 1, {
        0, 0, 0, 0, 0,
        0, 0, 0, 0, 0,
        0, 8, 8, 8, 0,
        0, 0, 0, 0, 0,
        0, 0, 0, 0, 0,
    }));
    line(4, 1, 0, 3);
    EXPECT_EQ(img, UInt8Image(size, 1, {
        0, 0, 0, 0, 0,
        0, 0, 0, 0, 8,
        0, 0, 8, 8, 0,
        8, 8, 0, 0, 0,
        0, 0, 0, 0, 0,
    }));
    line(0.5, 0, 3.5, 4);
    EXPECT_EQ(img, UInt8Image(size, 1, {
        0, 8, 0, 0, 0,
        0, 8, 0, 0, 0,
        0, 0, 8, 0, 0,
        0, 0, 0, 8, 0,
        0, 0, 0, 8, 8,
    }));
}

Y_UNIT_TEST(draw_line_4_connected)
{
    const Array2i size(5, 5);
    UInt8Image img(size, 1);
    const auto line = [&](double x0, double y0, double x1, double y1) {
        img.setZero();
        drawLine(img, {{x0, y0}, {x1, y1}}, Color(8), true);
    };
    line(0, 0, 4, 4);
    EXPECT_EQ(img, UInt8Image(size, 1, {
        8, 8, 0, 0, 0,
        0, 8, 8, 0, 0,
        0, 0, 8, 8, 0,
        0, 0, 0, 8, 8,
        0, 0, 0, 0, 8,
    }));
    line(3, 0, 1, 4);
    EXPECT_EQ(img, UInt8Image(size, 1, {
        0, 0, 0, 8, 0,
        0, 0, 8, 8, 0,
        0, 0, 8, 0, 0,
        0, 8, 8, 0, 0,
        0, 8, 0, 0, 0,
    }));
    line(2, 1, 2, 3);
    EXPECT_EQ(img, UInt8Image(size, 1, {
        0, 0, 0, 0, 0,
        0, 0, 8, 0, 0,
        0, 0, 8, 0, 0,
        0, 0, 8, 0, 0,
        0, 0, 0, 0, 0,
    }));
    line(1, 2, 3, 2);
    EXPECT_EQ(img, UInt8Image(size, 1, {
        0, 0, 0, 0, 0,
        0, 0, 0, 0, 0,
        0, 8, 8, 8, 0,
        0, 0, 0, 0, 0,
        0, 0, 0, 0, 0,
    }));
    line(4, 1, 0, 3);
    EXPECT_EQ(img, UInt8Image(size, 1, {
        0, 0, 0, 0, 0,
        0, 0, 0, 8, 8,
        0, 8, 8, 8, 0,
        8, 8, 0, 0, 0,
        0, 0, 0, 0, 0,
    }));
    line(0.5, 0, 3.5, 4);
    EXPECT_EQ(img, UInt8Image(size, 1, {
        0, 8, 0, 0, 0,
        0, 8, 8, 0, 0,
        0, 0, 8, 8, 0,
        0, 0, 0, 8, 8,
        0, 0, 0, 0, 8,
    }));
}

Y_UNIT_TEST(draw_disk)
{
    const Array2i size(5, 5);
    UInt8Image img(size, 1);
    const auto disk = [&](double r, double x, double y) {
        img.setZero();
        drawDisk(img, r, {x, y}, Color(8));
    };
    disk(1, 2, 2);
    EXPECT_EQ(img, UInt8Image(size, 1, {
        0, 0, 0, 0, 0,
        0, 0, 8, 0, 0,
        0, 8, 8, 8, 0,
        0, 0, 8, 0, 0,
        0, 0, 0, 0, 0,
    }));
    disk(2, 2, 2);
    EXPECT_EQ(img, UInt8Image(size, 1, {
        0, 0, 8, 0, 0,
        0, 8, 8, 8, 0,
        8, 8, 8, 8, 8,
        0, 8, 8, 8, 0,
        0, 0, 8, 0, 0,
    }));
    disk(3, 0, 0);
    EXPECT_EQ(img, UInt8Image(size, 1, {
        8, 8, 8, 8, 0,
        8, 8, 8, 0, 0,
        8, 8, 8, 0, 0,
        8, 0, 0, 0, 0,
        0, 0, 0, 0, 0,
    }));
}

Y_UNIT_TEST(median_blur)
{
    const Array2i size(5, 5);
    UInt8Image img(size, 1, {
        8, 0, 0, 0, 0,
        0, 0, 8, 0, 0,
        0, 8, 1, 8, 0,
        0, 0, 8, 8, 8,
        0, 0, 0, 8, 0,
    });
    img.medianBlurInplace(3);
    EXPECT_EQ(img, UInt8Image(size, 1, {
        0, 0, 0, 0, 0,
        0, 0, 0, 0, 0,
        0, 0, 8, 8, 0,
        0, 0, 8, 8, 8,
        0, 0, 0, 8, 8,
    }));
}

Y_UNIT_TEST(draw_logo)
{
    UInt8Image img = UInt8Image::fromFile(SRC_("data/lenna.png"));
    {
        UInt8Image imgLogo = img.clone();
        drawLogo(imgLogo, {-10, -5});
        EXPECT_EQ(imgLogo, UInt8Image::fromFile(SRC_("data/logo1.png")));
    }
    {
        UInt8Image imgLogo = img.clone();
        drawLogo(imgLogo, {0, 0});
        EXPECT_EQ(imgLogo, UInt8Image::fromFile(SRC_("data/logo2.png")));
    }
    {
        UInt8Image imgLogo = img.clone();
        drawLogo(imgLogo, {10, 5});
        EXPECT_EQ(imgLogo, UInt8Image::fromFile(SRC_("data/logo3.png")));
    }
    {
        UInt8Image imgLogo = img.clone();
        drawLogo(imgLogo, {110, 120});
        EXPECT_EQ(imgLogo, UInt8Image::fromFile(SRC_("data/logo4.png")));
    }
}

Y_UNIT_TEST(draw_polygon)
{
    const Array2i size(5, 5);
    Geometry poly = Geometry::fromWkt("POLYGON ((1 1, 4 2, 1 4, 1 1))");
    UInt8Image img = UInt8Image::zero(size, 1);
    drawPolygon(img, poly, Color(8));
    EXPECT_EQ(img, UInt8Image(size, 1, {
        0, 0, 0, 0, 0,
        0, 8, 0, 0, 0,
        0, 8, 8, 8, 8,
        0, 8, 8, 0, 0,
        0, 0, 0, 0, 0,
    }));
}

Y_UNIT_TEST(draw_logo_in_tile)
{
    const tile::Tile tiles[]{
        {13108, 5370, 14},  // with logo
        {13107, 5370, 14},  // without logo
        {26213, 10743, 15}, // with half of logo
    };
    for (const auto& tile: tiles) {
        UInt8Image img = UInt8Image::color({0, 0, 255}, TILE_SIZES, 3); // BGR
        drawLogoInTile(img, tile);
        const auto file = SRC_("data/logo_tile_" + tileFileName(tile) + ".png");
        EXPECT_EQ(img, UInt8Image::fromFile(file));
    }
}

Y_UNIT_TEST(draw_tile_name)
{
    const tile::Tile tile{4194303, 4194302, 22};
    UInt8Image img = drawTileName(tile);
    const auto file = SRC_("data/name_tile_" + tileFileName(tile) + ".png");
    EXPECT_EQ(img, UInt8Image::fromFile(file));
}

} // suite
} // namespace maps::factory::image::tests

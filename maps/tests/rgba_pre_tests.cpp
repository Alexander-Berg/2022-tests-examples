#include <maps/libs/color/include/rgba_pre.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace maps::color::tests {

Y_UNIT_TEST_SUITE(rgba_pre_should) {

Y_UNIT_TEST(premultiply_alpha) {
    RgbaPre transparent = premultiply(Rgba{0x22, 0x44, 0x88, 0});
    EXPECT_EQ(transparent.red, 0);
    EXPECT_EQ(transparent.green, 0);
    EXPECT_EQ(transparent.blue, 0);
    EXPECT_EQ(transparent.alpha, 0);

    RgbaPre opaque = premultiply(Rgba{0x22, 0x44, 0x88, 0xff});
    EXPECT_EQ(opaque.red, 0x22);
    EXPECT_EQ(opaque.green, 0x44);
    EXPECT_EQ(opaque.blue, 0x88);
    EXPECT_EQ(opaque.alpha, 0xff);

    RgbaPre semiTransparent = premultiply(Rgba{0x22, 0x44, 0x88, 0x7f});
    EXPECT_EQ(semiTransparent.red, 0x11);
    EXPECT_EQ(semiTransparent.green, 0x22);
    EXPECT_EQ(semiTransparent.blue, 0x44);
    EXPECT_EQ(semiTransparent.alpha, 0x7f);

    RgbaPre quarterTransparent = premultiply(Rgba{0x22, 0x44, 0x88, 0x40});
    EXPECT_EQ(quarterTransparent.red, 0x09);
    EXPECT_EQ(quarterTransparent.green, 0x11);
    EXPECT_EQ(quarterTransparent.blue, 0x22);
    EXPECT_EQ(quarterTransparent.alpha, 0x40);

    RgbaPre red =  premultiply(Rgba{0xff, 0x00, 0x00, 0xaa});
    EXPECT_EQ(red.red, 0xaa);
    EXPECT_EQ(red.green, 0x00);
    EXPECT_EQ(red.blue, 0x00);
    EXPECT_EQ(red.alpha, 0xaa);
}

Y_UNIT_TEST(demultiply_alpha) {
    EXPECT_EQ(demultiply(RgbaPre{0x11, 0x22, 0x44, 0xff}).hexNotation(), "#112244");
    EXPECT_EQ(demultiply(RgbaPre{0x11, 0x22, 0x44, 0x7f}).hexNotation(), "#2244887f");
    EXPECT_EQ(demultiply(RgbaPre{0x09, 0x11, 0x22, 0x40}).hexNotation(), "#23438740");
    EXPECT_EQ(demultiply(RgbaPre{0x09, 0x11, 0x22, 0x00}).hexNotation(), "#00000000");
    EXPECT_EQ(demultiply(RgbaPre{0xaa, 0x00, 0x00, 0xaa}).hexNotation(), "#ff0000aa");
}

} //Y_UNIT_TEST_SUITE

} //namespace maps::color::tests

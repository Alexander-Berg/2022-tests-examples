#include <maps/libs/color/include/hsl.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

using namespace testing;

namespace maps::color::tests {

Y_UNIT_TEST_SUITE(hsl_should) {

Y_UNIT_TEST(convert_hsl_to_rgb) {
    // https://en.wikipedia.org/wiki/HSL_and_HSV#Examples
    const std::vector<std::pair<Rgba, Hsla>> data = {
        {{0x00, 0x00, 0x00}, {  0.0, 0.000, 0.000}},
        {{0x7f, 0x7f, 0x7f}, {  0.0, 0.000, 0.498}},
        {{0x80, 0x80, 0x80}, {  0.0, 0.000, 0.502}},
        {{0xff, 0xff, 0xff}, {  0.0, 0.000, 1.000}},
        {{0xff, 0x00, 0x00}, {  0.0, 1.000, 0.500}},
        {{0xbf, 0xbf, 0x00}, { 60.0, 1.000, 0.375}},
        {{0x00, 0x80, 0x00}, {120.0, 1.000, 0.250}},
        {{0x80, 0xff, 0xff}, {180.0, 1.000, 0.750}},
        {{0x80, 0x80, 0xff}, {240.0, 1.000, 0.750}},
        {{0xbf, 0x40, 0xbf}, {300.0, 0.500, 0.500}},
        {{0xa0, 0xa4, 0x24}, { 61.8, 0.638, 0.393}},
        {{0x41, 0x1b, 0xea}, {251.1, 0.832, 0.511}},
        {{0x1e, 0xac, 0x41}, {134.9, 0.707, 0.396}},
        {{0xf0, 0xc8, 0x0e}, { 49.5, 0.890, 0.498}},
        {{0xb4, 0x30, 0xe5}, {283.7, 0.775, 0.543}},
        {{0xed, 0x76, 0x51}, { 14.3, 0.817, 0.624}},
        {{0xfe, 0xf8, 0x88}, { 56.9, 0.985, 0.765}},
        {{0x19, 0xcb, 0x97}, {162.4, 0.779, 0.447}},
        {{0x36, 0x26, 0x98}, {248.3, 0.601, 0.373}},
        {{0x7e, 0x7e, 0xb8}, {240.0, 0.290, 0.608}},
    };
    for (auto [rgb, hsl] : data) {
        EXPECT_EQ(hslToRgb(hsl).hexNotation(), rgb.hexNotation());

        hsl.hue += 360;
        EXPECT_EQ(hslToRgb(hsl).hexNotation(), rgb.hexNotation()) << "+360";
        hsl.hue += 360;
        EXPECT_EQ(hslToRgb(hsl).hexNotation(), rgb.hexNotation()) << "+720";
        hsl.hue -= 1080;
        EXPECT_EQ(hslToRgb(hsl).hexNotation(), rgb.hexNotation()) << "-360";
        hsl.hue -= 360;
        EXPECT_EQ(hslToRgb(hsl).hexNotation(), rgb.hexNotation()) << "-720";
        hsl.hue += 720;

        auto hslCalc = rgbToHsl(rgb);
        EXPECT_THAT(hslCalc.hue, FloatNear(hsl.hue, 0.2)) << rgb.hexNotation();
        EXPECT_THAT(hslCalc.saturation, FloatNear(hsl.saturation, 0.005)) << rgb.hexNotation();
        EXPECT_THAT(hslCalc.lightness, FloatNear(hsl.lightness, 0.001)) << rgb.hexNotation();

        EXPECT_EQ(hslToRgb(hslCalc).hexNotation(), rgb.hexNotation());
    }
}

Y_UNIT_TEST(convert_alpha) {
    EXPECT_EQ(hslToRgb({0, 0, 0, 0.0f}).alpha, 0x00);
    EXPECT_EQ(hslToRgb({0, 0, 0, 0.1f}).alpha, 0x1a);
    EXPECT_EQ(hslToRgb({0, 0, 0, 0.5f}).alpha, 0x80);
    EXPECT_EQ(hslToRgb({60, 1, 0.5, 0.5f}).alpha, 0x80);
    EXPECT_EQ(hslToRgb({0, 0, 0, 1.0f}).alpha, 0xff);

    EXPECT_THAT(rgbToHsl({0, 0, 0, 0x00}).alpha, FloatEq(0));
    EXPECT_THAT(rgbToHsl({0, 0, 0, 0x1a}).alpha, FloatNear(0.102, 0.001));
    EXPECT_THAT(rgbToHsl({0, 0, 0, 0x80}).alpha, FloatNear(0.502, 0.001));
    EXPECT_THAT(rgbToHsl({0xff, 0, 0, 0x80}).alpha, FloatNear(0.502, 0.001));
    EXPECT_THAT(rgbToHsl({0, 0, 0, 0xff}).alpha, FloatEq(1));
}

} //Y_UNIT_TEST_SUITE

} //namespace maps::color::tests

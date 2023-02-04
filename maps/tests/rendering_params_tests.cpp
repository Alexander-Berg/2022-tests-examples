#include <maps/factory/libs/sproto_helpers/rendering_params.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::factory::sproto_helpers::tests {

namespace {

const int32_t HUE = 120;
const int32_t SATURATION = 1;
const int32_t LIGHTNESS = 1;
const double SIGMA = 0.5;
const double RADIUS = 0.5;
const auto COLOR_CORRECTION_PARAMS = json::Value{
    {"hue", json::Value{HUE}},
    {"saturation", json::Value{SATURATION}},
    {"lightness", json::Value{LIGHTNESS}}
};
const auto SHARPING_PARAMS = json::Value{
    {"sigma", json::Value{SIGMA}},
    {"radius", json::Value{RADIUS}}
};

smosaics::ColorCorrectionParams sprotoColorCorrectionParams()
{
    smosaics::ColorCorrectionParams colorCorrectionParams;
    colorCorrectionParams.hue() = HUE;
    colorCorrectionParams.saturation() = SATURATION;
    colorCorrectionParams.lightness() = LIGHTNESS;
    return colorCorrectionParams;
}

smosaics::SharpingParams sprotoSharpingParams()
{
    smosaics::SharpingParams sharpingParams;
    sharpingParams.sigma() = SIGMA;
    sharpingParams.radius() = RADIUS;
    return sharpingParams;
}

} // namespace

TEST(test_convert_color_correction_params, test_convert_to_sproto)
{
    auto colorCorrectionParams =
        *convertToSproto<smosaics::ColorCorrectionParams>(COLOR_CORRECTION_PARAMS);
    EXPECT_EQ(*colorCorrectionParams.hue(), HUE);
    EXPECT_EQ(*colorCorrectionParams.saturation(), SATURATION);
    EXPECT_EQ(*colorCorrectionParams.lightness(), LIGHTNESS);
}

TEST(test_convert_color_correction_params, test_convert_from_sproto)
{
    auto colorCorrectionParams = convertFromSproto(sprotoColorCorrectionParams());
    EXPECT_EQ(colorCorrectionParams["hue"].as<int32_t>(), HUE);
    EXPECT_EQ(colorCorrectionParams["saturation"].as<int32_t>(), SATURATION);
    EXPECT_EQ(colorCorrectionParams["lightness"].as<int32_t>(), LIGHTNESS);
}

TEST(test_convert_sharping_params, test_convert_to_sproto)
{
    auto sharpingParams =
        *convertToSproto<smosaics::SharpingParams>(SHARPING_PARAMS);
    EXPECT_EQ(sharpingParams.sigma(), SIGMA);
    EXPECT_EQ(*sharpingParams.radius(), RADIUS);
}

TEST(test_convert_sharping_params, test_convert_from_sproto)
{
    auto sharpingParams = convertFromSproto(sprotoSharpingParams());
    EXPECT_EQ(sharpingParams["sigma"].as<double>(), SIGMA);
    EXPECT_EQ(sharpingParams["radius"].as<double>(), RADIUS);
}

} // namespace maps::factory::sproto_helpers::tests

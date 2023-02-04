#include <maps/factory/libs/image/color_correction.h>

#include <maps/factory/libs/unittest/tests_common.h>

namespace maps::factory::image::tests {
using namespace maps::factory::tests;

Y_UNIT_TEST_SUITE(color_correction_should) {

const Eigen::Array3d rangeMin(100, 200, 300);
const Eigen::Array3d rangeMax(65000, 64000, 63000);
constexpr double gamma = 2;
//
//Y_UNIT_TEST(scale_gamma_and_trim)
//{
//    VsiPath path = std::string("./tmp/") + this->Name_ + ".tif";
//    constexpr int bands = 3;
//    const Array2i sz(7, 13);
//    Dataset ds = CreateInMemory().setSize(sz).setBands(bands).setType(DataType::UInt16);
//    ds.setIdentityGeoTransform();
//
//    UInt16Image srcImg = UInt16Image::random(sz, bands);
//    ds.Write(srcImg);
//
//    Dataset result = CreateTiff(path).like(ds).setType(DataType::Byte).setBands(3);
//    RgbColorConversion toRgb{rangeMin, rangeMax, gamma};
//    UInt8Image rgbBlock({1, 1}, 1);
//    ds.ForEachBlock<float>({}, [&](FloatImageBase& img, auto& origin) {
//        rgbBlock.resizeUninitialized(img.size(), img.bands());
//        toRgb(img, rgbBlock);
//        result.Write(rgbBlock, origin);
//    });
//
//    // Transform data to match the result.
//    for (Index i = 0; i < srcImg.indices(); ++i) {
//        Eigen::Array3d row = srcImg.pix(i).transpose().cast<double>();
//        // Scale (scale stage)
//        row = (row - rangeMin) / (rangeMax - rangeMin);
//        // Gamma (gamma stage)
//        row = row.pow(1 / gamma) * 255;
//        // Round and trim (gdal when writing to uint8_t dataset)
//        row = row.round().max(0).min(255);
//        srcImg.pix(i) = row.cast<uint16_t>();
//    }
//
//    EXPECT_EQ(result.Read<uint16_t>(), srcImg);
//}

Y_UNIT_TEST(save_to_json)
{
    RgbColorConversion rs{rangeMin, rangeMax, gamma};
    constexpr auto json =
        R"({"gamma":2,"percentile":0,"contrast":false,"min":[100,200,300],"max":[65000,64000,63000]})";
    EXPECT_EQ((json::Builder() << rs).str(), json);
}

Y_UNIT_TEST(load_from_json)
{
    RgbColorConversion rs{rangeMin, rangeMax, gamma};
    constexpr auto json = R"({"gamma":2,"min":[100,200,300],"contrast":false,"max":[65000,64000,63000]})";
    RgbColorConversion loaded = RgbColorConversion().init(json::Value::fromString(json));
    EXPECT_EQ(loaded.gamma, rs.gamma);
    EXPECT_THAT(loaded.min, EigEq(rs.min));
    EXPECT_THAT(loaded.max, EigEq(rs.max));
}

Y_UNIT_TEST(get_histogram)
{
    const Array2i size(4, 2);
    UInt8Image img(size, 2, {
        1, 2, 3, 4,
        5, 6, 1, 2,

        5, 4, 3, 2,
        3, 4, 5, 6,
    });
    Histogram<Eigen::Dynamic> hist = histogram(img);
    EXPECT_EQ(hist.json(), "{\"max\":255,\"bins\":[[0,2,2,1,1,1,1],[0,0,1,2,2,2,1]]}");
}

Y_UNIT_TEST(get_histogram_percentiles)
{
    UInt8Image img = UInt8Image::fromFile(std::string(SRC_("data/lenna.png")));
    Histogram<Eigen::Dynamic> hist = histogram(img);
    EXPECT_THAT(hist.computePercentile(0.05).transpose().eval(), EigEq(Eigen::Array3d(62, 22, 91)));
    EXPECT_THAT(hist.computePercentile(0.95).transpose().eval(), EigEq(Eigen::Array3d(170, 195, 238)));
}

Y_UNIT_TEST(stretch_histogram_each_band)
{
    UInt8Image img = UInt8Image::fromFile(std::string(SRC_("data/lenna.png")));
    UInt8Image result = autoLevelsHistogram(img);
    UInt8Image expected = UInt8Image::fromFile(std::string(SRC_("data/lenna_auto_levels_hist.png")));
    EXPECT_LE(result.maxAbsDifference(expected), 1.0);
}

Y_UNIT_TEST(auto_contrast_histogram)
{
    UInt8Image img = UInt8Image::fromFile(std::string(SRC_("data/lenna.png")));
    UInt8Image result = autoContrastHistogram(img);
    UInt8Image expected = UInt8Image::fromFile(std::string(SRC_("data/lenna_auto_contrast_hist.png")));
    EXPECT_LE(result.maxAbsDifference(expected), 1.0);
}

Y_UNIT_TEST(normalize_histogram)
{
    UInt8Image img = UInt8Image::fromFile(std::string(SRC_("data/lenna.png")));
    UInt8Image result = normalizeHistogram(img);
    UInt8Image expected = UInt8Image::fromFile(std::string(SRC_("data/lenna_normalize_hist.png")));
    EXPECT_LE(result.maxAbsDifference(expected), 1.0);
}

Y_UNIT_TEST(equalize_histogram)
{
    UInt8Image img = UInt8Image::fromFile(std::string(SRC_("data/lenna.png")));
    UInt8Image result = equalizeHistogram(img);
    UInt8Image expected = UInt8Image::fromFile(std::string(SRC_("data/lenna_equalize_hist.png")));
    EXPECT_LE(result.maxAbsDifference(expected), 1.0);
}

Y_UNIT_TEST(match_histogram)
{
    UInt8Image img = UInt8Image::fromFile(std::string(SRC_("data/tile1.png")));
    UInt8Image ref = UInt8Image::fromFile(std::string(SRC_("data/tile2.png")));
    UInt8Image result = matchHistogram(img, ref);
    UInt8Image expected = UInt8Image::fromFile(std::string(SRC_("data/tile_matched.png")));
    EXPECT_LE(result.meanAbsDifference(expected), 1.0);
}

Y_UNIT_TEST(match_equalized_histogram)
{
    UInt8Image img = UInt8Image::fromFile(std::string(SRC_("data/lenna_equalize_hist.png")));
    UInt8Image ref = UInt8Image::fromFile(std::string(SRC_("data/lenna.png")));
    UInt8Image result = matchHistogram(img, ref);
    EXPECT_LE(result.meanAbsDifference(ref), 1.0);
}

Y_UNIT_TEST(match_monochrome_histogram)
{
    const Array2i size(2, 2);
    UInt8Image blackImg = UInt8Image::zero(size, 2);
    UInt8Image whiteImg = UInt8Image::max(size, 2);
    EXPECT_EQ(matchHistogram(blackImg, whiteImg), whiteImg);
    EXPECT_EQ(matchHistogram(whiteImg, blackImg), blackImg);
    EXPECT_EQ(matchHistogram(blackImg, blackImg), blackImg);
    EXPECT_EQ(matchHistogram(whiteImg, whiteImg), whiteImg);
}

Y_UNIT_TEST(match_one_pixel_histogram)
{
    const Array2i size(1, 1);
    UInt8Image img = UInt8Image::random(size, 3);
    UInt8Image ref = UInt8Image::random(size, 3);
    EXPECT_EQ(matchHistogram(img, ref), ref);
}

} // suite

} //namespace maps::factory::image::tests

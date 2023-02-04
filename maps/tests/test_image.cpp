#include <maps/factory/libs/image/image.h>

#include <maps/factory/libs/unittest/tests_common.h>

#include <maps/libs/common/include/file_utils.h>
#include <maps/libs/json/include/value.h>
#include <maps/libs/tile/include/tile.h>

#include <contrib/libs/opencv/include/opencv2/core.hpp>
#include <contrib/libs/opencv/include/opencv2/imgproc.hpp>

#include <random>

namespace maps::factory::image::tests {
using namespace maps::factory::tests;

Y_UNIT_TEST_SUITE(matrix_image_should) {

const Array2i BLOCK_SIZES(256, 256);

Y_UNIT_TEST(save_load_json_blur_options)
{
    BlurOptions opt{.sigma=1.1, .kernel=5, .border=BorderType::Reflect101};
    std::string json = (json::Builder() << opt).str();
    BlurOptions res{};
    res.init(json::Value::fromString(json));

    EXPECT_EQ(res.sigma, opt.sigma);
    EXPECT_EQ(res.border, opt.border);
    EXPECT_EQ(res.kernel, opt.kernel);
}

Y_UNIT_TEST(save_load_json_unsharp_options)
{
    UnsharpOptions opt{
        .amount = 0.9, .sigma=1.1, .kernel=5, .border=BorderType::Reflect101,
        .maskGamma = 2.2, .threshold = 3.3, .smoothThresholdMin = 5.5, .smoothThresholdMax = 4.4
    };
    std::string json = (json::Builder() << opt).str();
    UnsharpOptions res{};
    res.init(json::Value::fromString(json));

    EXPECT_EQ(res.amount, opt.amount);
    EXPECT_EQ(res.sigma, opt.sigma);
    EXPECT_EQ(res.border, opt.border);
    EXPECT_EQ(res.kernel, opt.kernel);
    EXPECT_EQ(res.maskGamma, opt.maskGamma);
    EXPECT_EQ(res.threshold, opt.threshold);
    EXPECT_EQ(res.smoothThresholdMax, opt.smoothThresholdMax);
    EXPECT_EQ(res.smoothThresholdMin, opt.smoothThresholdMin);
}

Y_UNIT_TEST(get_point_from_index)
{
    const Size2i sz(3, 5);
    Image<int> img = Image<int>::zero(sz, 3);
    for (Index r = 0; r < img.rows(); ++r) {
        for (Index c = 0; c < img.cols(); ++c) {
            const Index i = img.index(r, c);
            const auto p = img.pointFromIndex(i);
            EXPECT_EQ   (p.x(), c);
            EXPECT_EQ(p.y(), r);
            EXPECT_EQ(img.index(p), i);
        }
    }
}

Y_UNIT_TEST(get_and_set_colors)
{
    const Size2i sz(2, 2);
    const Point2i p1(0, 1);
    const Point2i p2(1, 0);
    Image<uint8_t> img = Image<uint8_t>::zero(sz, 2);
    img.setColor(Color::constant(55));
    img.setColor(p1, Color::white<uint8_t>());
    img.setColor(p2, Color::black());
    Image<uint8_t> expected(sz, 2);
    expected.band(0) <<
        55, 0,
        255, 55;
    expected.band(1) <<
        55, 0,
        255, 55;
    EXPECT_EQ(img, expected);
}

Y_UNIT_TEST(create_zero)
{
    const Size2i sz(10, 20);
    Image<int> img = Image<int>::zero(sz, 3);
    EXPECT_TRUE(img.array().isZero());
    EXPECT_TRUE(img.isZero());
    EXPECT_EQ(img.array().rows(), sz.prod());
    EXPECT_EQ(img.array().cols(), 3);
    EXPECT_THAT(img.size(), EigEq(sz));
    EXPECT_EQ(img.indices(), sz.prod());
    EXPECT_EQ(img.bands(), 3);
    EXPECT_EQ(img.width(), sz.x());
    EXPECT_EQ(img.height(), sz.y());
    EXPECT_EQ(img.elementSizeBytes, 4);
    EXPECT_EQ(img.Dim().TotalSizeBytes(), 10 * 20 * 3 * 4);
}

Y_UNIT_TEST(swap_bands)
{
    const Size2i sz(10, 20);
    Image<int> img = Image<int>::random(sz, 3);
    Image<int> imgResult = img.clone();
    EXPECT_EQ(imgResult.bands(), img.bands());
    EXPECT_THAT(imgResult.size(), EigEq(img.size()));
    EXPECT_THAT(imgResult.array(), EigEq(img.array()));
    imgResult.swapBands(0, 2);
    EXPECT_THAT(imgResult.band(0), EigEq(img.band(2)));
    EXPECT_THAT(imgResult.band(1), EigEq(img.band(1)));
    EXPECT_THAT(imgResult.band(2), EigEq(img.band(0)));
    imgResult.swapBands(0, 2);
    EXPECT_THAT(imgResult.array(), EigEq(img.array()));
    imgResult.swapBands(0, 0);
    EXPECT_THAT(imgResult.array(), EigEq(img.array()));
}

Y_UNIT_TEST(redistribute)
{
    using T = uint16_t;
    const std::pair<Size2i, Size2i> input[]{
        {{7, 11}, {3, 5}},
        {{13, 11}, {13, 5}},
        {{13, 7}, {5, 7}},
        {{7, 7}, {5, 5}},
        {{5, 5}, {4, 5}},
        {{5, 5}, {5, 4}},
        {{5, 5}, {1, 1}},
    };
    for (int bands = 1; bands < 5; ++bands) {
        for (const auto&[sz, szRead]: input) {
            Image<T> imgRead = Image<T>::random(szRead, bands);

            Image<T> img = Image<T>::zero(sz, bands);
            std::memcpy(img.data(), imgRead.data(), imgRead.Dim().TotalSizeBytes());
            img.extendInplace(szRead);

            Image<T> expected = Image<T>::zero(sz, bands);
            for (int b = 0; b < bands; b++) {
                expected.band(b).block(0, 0, szRead.y(), szRead.x()) = imgRead.band(b);
            }

            ASSERT_THAT(img.array().row(0).eval(), EigEq(expected.array().row(0).eval()));
            EXPECT_THAT(img.array().eval(), EigEq(expected.array().eval()));
        }
    }
}

Y_UNIT_TEST(index_access)
{
    const Size2i sz(2, 3);
    Image<int> img(sz, 2);
    img.band(0) <<
        1, 2,
        3, 4,
        5, 6;
    img.band(1) <<
        10, 20,
        30, 40,
        50, 60;

    //                       row, col
    EXPECT_THAT(Array2i(img.pix(0, 0)), EigEq(Array2i(1, 10)));
    EXPECT_THAT(Array2i(img.pix(1, 0)), EigEq(Array2i(3, 30)));
    EXPECT_THAT(Array2i(img.pix(0, 1)), EigEq(Array2i(2, 20)));
    EXPECT_THAT(Array2i(img.pix(1, 1)), EigEq(Array2i(4, 40)));

    for (int col = 0; col < img.cols(); ++col) {
        for (int row = 0; row < img.rows(); ++row) {
            EXPECT_THAT(Array2i(img.pix(row, col)),
                EigEq(Array2i(img.band(0)(row, col), img.band(1)(row, col))));
        }
    }
}

Y_UNIT_TEST(copy_to_cv)
{
    const Size2i sz(5, 7);
    Image<int> img = Image<int>::random(sz, 3);
    cv::Mat m = img.cv();
    EXPECT_EQ(m.depth(), CV_32S);
    EXPECT_EQ(m.channels(), img.bands());
    EXPECT_EQ(m.rows, img.rows());
    EXPECT_EQ(m.cols, img.cols());
    EXPECT_TRUE(m.isContinuous());
    EXPECT_FALSE(m.empty());
    for (int row = 0; row < img.rows(); ++row) {
        for (int col = 0; col < img.cols(); ++col) {
            const auto pix = m.at<cv::Vec<int, 3>>(row, col);
            EXPECT_THAT(Eigen::Array3i(pix(0), pix(1), pix(2)),
                EigEq(Eigen::Array3i(img.pix(row, col))));
        }
    }
}

Y_UNIT_TEST(copy_from_cv)
{
    const Size2i sz(5, 7);
    Image<int> img = Image<int>::random(sz, 3);
    cv::Mat m = img.cv();
    Image<int> result(m);
    EXPECT_EQ(result.cols(), img.cols());
    EXPECT_EQ(result.rows(), img.rows());
    EXPECT_EQ(result.bands(), img.bands());
    EXPECT_THAT(result.array().eval(), EigEq(img.array().eval()));
}

Y_UNIT_TEST(saturate_cast)
{
    const Size2i sz(3, 3);
    FloatImage imgF = FloatImage::zero(sz, 1);
    imgF.array() <<
        -2, -1, 0,
        0.1, 0.51, 0.9,
        0.999, 1, 2;
    UInt8Image imgB = UInt8Image::zero(sz, 1);
    imgB.array() <<
        0, 0, 0,
        26, 130, 230,
        255, 255, 255;

    Image<double> result = imgF.cast<double>();
    EXPECT_EQ(result.cols(), imgF.cols());
    EXPECT_EQ(result.rows(), imgF.rows());
    EXPECT_EQ(result.bands(), imgF.bands());
    EXPECT_THAT(result.array().eval(), EigEq(imgF.array().cast<double>().eval()));

    EXPECT_THAT(imgF.cast<int>().array().eval(),
        EigEq(imgF.array().round().cast<int>().eval()));
    EXPECT_THAT(imgF.cast<uint8_t>().array().eval(),
        EigEq(imgF.array().round().max(0).min(255).cast<uint8_t>().eval()));
    EXPECT_EQ(imgF.cast<uint8_t>(255.0), imgB);
    EXPECT_THAT(imgB.cast<double>(1.0 / 255.0).array().eval(),
        EigEq((imgB.array().cast<double>() / 255.0).eval()));
}

Y_UNIT_TEST(color_cast)
{
    const Size2i sz(7, 5);
    FloatImage imgF = FloatImage::random(sz, 2);
    UInt8Image imgB = UInt8Image::random(sz, 2);

    EXPECT_EQ(imgF.colorCast<uint8_t>(), imgF.cast<uint8_t>(255.0));
    EXPECT_EQ(imgF.colorCast<uint16_t>(), imgF.cast<uint16_t>(65535.0));

    EXPECT_EQ(imgB.colorCast<float>(), imgB.cast<float>(1 / 255.0));
    EXPECT_EQ(imgB.colorCast<double>(), imgB.cast<double>(1 / 255.0));
    EXPECT_EQ(imgB.colorCast<uint16_t>(), imgB.cast<uint16_t>(65535.0 / 255.0));
}

Y_UNIT_TEST(alpha_blend_black)
{
    const Size2i sz(3, 3);
    FloatImage img = FloatImage::zero(sz, 2);
    FloatImage bottom = FloatImage::zero(sz, 2);

    img.alphaBlend(bottom);

    EXPECT_TRUE(img.isZero());
}

Y_UNIT_TEST(alpha_blend_opaque_top)
{
    const Size2i sz(3, 3);
    FloatImage img = FloatImage::zero(sz, 2);
    img.band(0).block(0, 0, 2, 2).setConstant(1);
    img.band(1).setConstant(1);
    FloatImage bottom = FloatImage::zero(sz, 2);
    bottom.band(0).block(1, 1, 2, 2).setConstant(1);
    bottom.band(1).setConstant(1);
    FloatImage orig = img.clone();

    img.alphaBlend(bottom);

    EXPECT_EQ(img, orig);
}

Y_UNIT_TEST(alpha_blend_transparent_top)
{
    const Size2i sz(3, 3);
    FloatImage img = FloatImage::zero(sz, 2);
    img.band(0).block(0, 0, 2, 2).setConstant(1);
    img.band(1).setConstant(0);
    FloatImage bottom = FloatImage::zero(sz, 2);
    bottom.band(0).block(1, 1, 2, 2).setConstant(1);
    bottom.band(1).setConstant(1);

    img.alphaBlend(bottom);

    EXPECT_EQ(img, bottom);
}

Y_UNIT_TEST(alpha_blend_by_mask)
{
    const Size2i sz(3, 3);
    FloatImage img = FloatImage::zero(sz, 2);
    img.band(0).setConstant(1);
    img.band(1).col(1).setConstant(1);
    FloatImage bottom = FloatImage::zero(sz, 2);
    bottom.band(0).row(1).setConstant(1);
    bottom.band(1).setConstant(1);

    img.alphaBlend(bottom);

    FloatImage expected = FloatImage::zero(sz, 2);
    expected.band(0).row(1).setConstant(1);
    expected.band(0).col(1).setConstant(1);
    expected.band(1).setConstant(1);
    EXPECT_EQ(img, expected);
}

Y_UNIT_TEST(alpha_blend_uint8)
{
    const Size2i sz(6, 7);
    UInt8Image img = UInt8Image::random(sz, 4);
    UInt8Image bottom = UInt8Image::random(sz, 4);
    FloatImage expected = img.cast<float>(1.0f / 255.0f);
    expected.alphaBlend(bottom.cast<float>(1.0f / 255.0f));

    img.alphaBlend(bottom);

    EXPECT_EQ(img, expected.cast<uint8_t>(255.0f));
}

Y_UNIT_TEST(alpha_blend_uint8_images)
{
    UInt8Image img = UInt8Image::fromFile(SRC_("data/top.png"));
    UInt8Image bottom = UInt8Image::fromFile(SRC_("data/bottom.png"));
    UInt8Image result = UInt8Image::fromFile(SRC_("data/alpha_blend.png"));

    {
        EigenMallocNotAllowed notAllowed;
        img.alphaBlend(bottom);
    }
    EXPECT_EQ(img, result);
}

Y_UNIT_TEST(alpha_blend_top)
{
    UInt8Image top = UInt8Image::fromFile(SRC_("data/top.png"));
    UInt8Image img = UInt8Image::fromFile(SRC_("data/bottom.png"));
    UInt8Image result = UInt8Image::fromFile(SRC_("data/alpha_blend.png"));

    {
        EigenMallocNotAllowed notAllowed;
        img.alphaBlendTop(top);
    }
    EXPECT_EQ(img, result);
}

Y_UNIT_TEST(lighten_blend)
{
    const Size2i sz(2, 2);
    UInt8Image img = UInt8Image(sz, 1, {10, 2, 30, 4});
    UInt8Image other = UInt8Image(sz, 1, {1, 20, 3, 40});

    img.lightenBlend(other);

    UInt8Image expected = UInt8Image(sz, 1, {10, 20, 30, 40});
    EXPECT_EQ(img, expected);
}

Y_UNIT_TEST(nodata_blend)
{
    const uint8_t nd = 128;
    const Size2i sz(2, 2);
    UInt8Image img = UInt8Image(sz, 1, {1, 2, 3, 4});
    UInt8Image other = UInt8Image(sz, 1, {10, nd, 30, nd});

    img.nodataBlend(other, nd);

    UInt8Image expected = UInt8Image(sz, 1, {10, 2, 30, 4});
    EXPECT_EQ(img, expected);
}

Y_UNIT_TEST(check_transparency)
{
    const Size2i sz(4, 4);
    UInt8Image img = UInt8Image::zero(sz, 2);
    EXPECT_TRUE(img.isLastBandBlack());
    EXPECT_FALSE(img.isLastBandWhite());

    img.val(0, 0, 0) = 1;
    EXPECT_TRUE(img.isLastBandBlack());
    EXPECT_FALSE(img.isLastBandWhite());

    img.val(0, 0, 1) = 1;
    EXPECT_FALSE(img.isLastBandBlack());
    EXPECT_FALSE(img.isLastBandWhite());

    img.setConstantBand(255, 1);
    EXPECT_FALSE(img.isLastBandBlack());
    EXPECT_TRUE(img.isLastBandWhite());
}

Y_UNIT_TEST(save_load_to_file_uint8)
{
    const Size2i sz(8, 8);
    const auto file = boost::filesystem::path("./tmp") / this->Name_ / "img.png";
    UInt8Image img = UInt8Image::random(sz, 3);
    img.save(file);
    UInt8Image loaded = UInt8Image::fromFile(file);
    EXPECT_EQ(loaded, img);
}

Y_UNIT_TEST(save_load_to_file_uint16)
{
    const Size2i sz(48, 32);
    const auto file = boost::filesystem::path("./tmp") / this->Name_ / "img.png";
    UInt16Image img = UInt16Image::random(sz, 4);
    img.save(file);
    UInt16Image loaded = UInt16Image::fromFile(file);
    EXPECT_EQ(loaded, img);
}

Y_UNIT_TEST(save_load_to_file_with_convert)
{
    const Size2i sz(32, 48);
    const auto file = boost::filesystem::path("./tmp") / this->Name_ / "img.png";
    UInt8Image img = UInt8Image::random(sz, 1);
    img.save(file);
    FloatImage loaded = FloatImage::fromFile(file);
    EXPECT_EQ(loaded.cast<uint8_t>(), img);
}

Y_UNIT_TEST(save_load_to_buffer)
{
    const Size2i sz(32, 48);
    UInt8Image img = UInt8Image::random(sz, 3);
    const auto buffer = img.png();
    UInt8Image loaded = UInt8Image::fromBuffer(buffer);
    EXPECT_EQ(loaded, img);
}

Y_UNIT_TEST(save_load_to_buffer_with_convert)
{
    const Size2i sz(32, 48);
    UInt8Image img = UInt8Image::random(sz, 3);
    const auto buffer = img.png(true);
    Image<int> loaded = Image<int>::fromBuffer(buffer);
    EXPECT_EQ(loaded.cast<uint8_t>(), img);
}

Y_UNIT_TEST(read_block)
{
    const Size2i sz(4, 3);
    Image<int> img = Image<int>::zero(sz, 2);
    img.band(0) <<
        1, 2, 3, 4,
        5, 6, 7, 8,
        9, 10, 11, 12;
    img.band(1) <<
        10, 20, 30, 40,
        50, 60, 70, 80,
        90, 100, 110, 120;

    EXPECT_EQ(img.block(sz), img);

    {
        const Box2i box(Array2i(1, 1), Array2i(3, 2));
        Image<int> expected = Image<int>::zero(box.sizes(), 2);
        expected.band(0) <<
            6, 7;
        expected.band(1) <<
            60, 70;
        EXPECT_EQ(img.block(box), expected);
        EXPECT_EQ(img.shrink(Array2i(1, 1)), expected);
    }
    {
        const Box2i box(Array2i(0, 0), Array2i(2, 3));
        Image<int> expected = Image<int>::zero(box.sizes(), 2);
        expected.band(0) <<
            1, 2,
            5, 6,
            9, 10;
        expected.band(1) <<
            10, 20,
            50, 60,
            90, 100;
        EXPECT_EQ(img.block(box), expected);
    }
    {
        const Box2i box(Array2i(0, 2), Array2i(4, 3));
        Image<int> expected = Image<int>::zero(box.sizes(), 2);
        expected.band(0) <<
            9, 10, 11, 12;
        expected.band(1) <<
            90, 100, 110, 120;
        EXPECT_EQ(img.block(box), expected);
    }
}

Y_UNIT_TEST(extend)
{
    const Size2i sz(3, 2);
    Image<int> img = Image<int>::zero(sz, 2);
    img.band(0) <<
        1, 2, 3,
        4, 5, 6;
    img.band(1) <<
        10, 20, 30,
        40, 50, 60;

    EXPECT_EQ(img.extend(Array2i::Zero(), BorderType::Zero), img);
    EXPECT_EQ(img.extend(Array2i::Zero(), BorderType::Replicate), img);
    EXPECT_EQ(img.extend(Array2i::Zero(), BorderType::Reflect101), img);

    {
        Image<int> expected = Image<int>::zero({5, 4}, 2);
        expected.band(0) <<
            0, 0, 0, 0, 0,
            0, 1, 2, 3, 0,
            0, 4, 5, 6, 0,
            0, 0, 0, 0, 0;
        expected.band(1) <<
            0, 0, 0, 0, 0,
            0, 10, 20, 30, 0,
            0, 40, 50, 60, 0,
            0, 0, 0, 0, 0;
        EXPECT_EQ(img.extend(Array2i::Constant(1), BorderType::Zero), expected);
    }
    {
        Image<int> expected = Image<int>::zero({5, 4}, 2);
        expected.band(0) <<
            1, 1, 2, 3, 3,
            1, 1, 2, 3, 3,
            4, 4, 5, 6, 6,
            4, 4, 5, 6, 6;
        expected.band(1) <<
            10, 10, 20, 30, 30,
            10, 10, 20, 30, 30,
            40, 40, 50, 60, 60,
            40, 40, 50, 60, 60;
        EXPECT_EQ(img.extend(Array2i::Constant(1), BorderType::Replicate), expected);
    }
    {
        Image<int> expected = Image<int>::zero({5, 4}, 2);
        expected.band(0) <<
            5, 4, 5, 6, 5,
            2, 1, 2, 3, 2,
            5, 4, 5, 6, 5,
            2, 1, 2, 3, 2;
        expected.band(1) <<
            50, 40, 50, 60, 50,
            20, 10, 20, 30, 20,
            50, 40, 50, 60, 50,
            20, 10, 20, 30, 20;
        EXPECT_EQ(img.extend(Array2i::Constant(1), BorderType::Reflect101), expected);
    }
}

Y_UNIT_TEST(assign_block)
{
    const Size2i sz(4, 3);
    Int32Image img(sz, 1, {
        1, 2, 3, 4,
        5, 6, 7, 8,
        9, 10, 11, 12
    });
    Int32Image src(sz, 1, {
        10, 20, 30, 40,
        50, 60, 70, 80,
        90, 100, 110, 120
    });
    const auto assign = [&](int dstMinX, int dstMinY, int dstMaxX, int dstMaxY, int srcOffX, int srcOffY) {
        Int32Image dst = img.clone();
        dst.assignBlock(Box2i(Point2i(dstMinX, dstMinY), Point2i(dstMaxX, dstMaxY)),
            src, Point2i(srcOffX, srcOffY));
        return dst;
    };
    {
        Int32Image dst = assign(0, 0, 4, 3, 0, 0);
        EXPECT_EQ(dst, src);
    }
    {
        Int32Image dst = assign(0, 0, 3, 2, 0, 0);
        Int32Image exp(sz, 1, {
            10, 20, 30, 4,
            50, 60, 70, 8,
            9, 10, 11, 12
        });
        EXPECT_EQ(dst, exp);
    }
    {
        Int32Image dst = assign(1, 1, 4, 3, 0, 0);
        Int32Image exp(sz, 1, {
            1, 2, 3, 4,
            5, 10, 20, 30,
            9, 50, 60, 70
        });
        EXPECT_EQ(dst, exp);
    }
    {
        Int32Image dst = assign(0, 0, 3, 2, 1, 1);
        Int32Image exp(sz, 1, {
            60, 70, 80, 4,
            100, 110, 120, 8,
            9, 10, 11, 12
        });
        EXPECT_EQ(dst, exp);
    }
    {
        Int32Image dst = assign(1, 1, 4, 3, 1, 1);
        Int32Image exp(sz, 1, {
            1, 2, 3, 4,
            5, 60, 70, 80,
            9, 100, 110, 120
        });
        EXPECT_EQ(dst, exp);
    }
    {
        EXPECT_EQ(assign(0, 0, 0, 0, 0, 0), img);
        EXPECT_EQ(assign(0, 0, 1, 0, 0, 0), img);
        EXPECT_EQ(assign(0, 0, 0, 1, 0, 0), img);
        EXPECT_EQ(assign(2, 0, 2, 2, 0, 0), img);
        EXPECT_EQ(assign(0, 2, 2, 2, 0, 0), img);
        EXPECT_EQ(assign(0, 0, 4, 3, 4, 3), img);
    }
}

Y_UNIT_TEST(assign_block_small)
{
    const Size2i sz(4, 3);
    Int32Image img(sz, 1, {
        1, 2, 3, 4,
        5, 6, 7, 8,
        9, 10, 11, 12
    });
    Int32Image block({2, 2}, 1, {
        20, 21,
        22, 23,
    });
    const Point2i origin(2, 1);
    img.assignBlock(origin, block);
    Int32Image exp(sz, 1, {
        1, 2, 3, 4,
        5, 6, 20, 21,
        9, 10, 22, 23
    });
    EXPECT_EQ(img, exp);
}

Y_UNIT_TEST(assign_random_block)
{
    std::random_device rd;
    std::mt19937 gen(rd());
    std::uniform_int_distribution<int> dSize(0, 5);
    std::uniform_int_distribution<int> dOff(-5, 5);
    const Size2i sz(4, 3);
    Int32Image dstOrig(sz, 1, {
        1, 2, 3, 4,
        5, 6, 7, 8,
        9, 10, 11, 12
    });
    Int32Image src(sz, 1, {
        10, 20, 30, 40,
        50, 60, 70, 80,
        90, 100, 110, 120
    });
    for (int i = 0; i < 100; ++i) {
        const Array2i dstOff(dOff(gen), dOff(gen));
        const Array2i srcOff(dOff(gen), dOff(gen));
        const Array2i size(dSize(gen), dSize(gen));
        Int32Image dst = dstOrig.clone();
        for (int dy = dstOff.y(), sy = srcOff.y(); dy < dstOff.y() + size.y(); ++dy, ++sy) {
            for (int dx = dstOff.x(), sx = srcOff.x(); dx < dstOff.x() + size.x(); ++dx, ++sx) {
                if (
                    dx < 0 || dx >= dst.size().x() ||
                    dy < 0 || dy >= dst.size().y() ||
                    sx < 0 || sx >= src.size().x() ||
                    sy < 0 || sy >= src.size().y()
                    ) {
                    continue;
                }
                dst.val(dy, dx, 0) = src.val(sy, sx, 0);
            }
        }
        Int32Image dstRes = dstOrig.clone();
        dstRes.assignBlock(dstOff, size, src, srcOff);
        EXPECT_EQ(dstRes, dst) << "dOff " << dstOff.transpose() << "; sOff " << srcOff.transpose()
                        << "; size " << size.transpose();
    }
}

Y_UNIT_TEST(extend_compare_to_cv)
{
    const Size2i sz(6, 7);
    UInt8Image img = UInt8Image::random(sz, 3);
    cv::Mat mat = img.cv();
    for (int bx: {0, 1, 2, 3}) {
        for (int by: {0, 1, 2, 3}) {
            cv::Mat result;
            cv::copyMakeBorder(mat, result, by, by, bx, bx, cv::BORDER_CONSTANT);
            EXPECT_EQ(img.extend(Array2i(bx, by), BorderType::Zero), UInt8Image(result));
            cv::copyMakeBorder(mat, result, by, by, bx, bx, cv::BORDER_REPLICATE);
            EXPECT_EQ(img.extend(Array2i(bx, by), BorderType::Replicate), UInt8Image(result));
            cv::copyMakeBorder(mat, result, by, by, bx, bx, cv::BORDER_REFLECT101);
            EXPECT_EQ(img.extend(Array2i(bx, by), BorderType::Reflect101), UInt8Image(result));
        }
    }
}

Y_UNIT_TEST(gaussian_blur_uint8)
{
    UInt8Image img = UInt8Image::fromFile(SRC_("data/top.png"));
    UInt8Image expected = UInt8Image::fromFile(SRC_("data/blur_5x1_top.png"));

    UInt8Image result = img.gaussianBlur({1, 11, BorderType::Replicate});
    EXPECT_LE(result.maxAbsDifference(expected), 1);
}

Y_UNIT_TEST(gaussian_blur_float)
{
    const Size2i sz(11, 13);
    FloatImage img = FloatImage::random(sz, 3);
    cv::Mat mat = img.cv();
    for (int r: {3, 2, 1}) {
        for (double s: {2.0, 1.0, 0.5}) {
            cv::Mat result;
            const int k = r * 2 + 1;
            cv::GaussianBlur(mat, result, {k, k}, s, s, cv::BORDER_REFLECT101);
            EXPECT_EQ(img.gaussianBlur({s, k, BorderType::Reflect101}), FloatImage(result));
        }
    }
}

Y_UNIT_TEST(gaussian_blur_inplace)
{
    const Size2i sz(11, 13);
    FloatImage img = FloatImage::random(sz, 3);
    FloatImage expected = img.gaussianBlur({1});

    img.gaussianBlurInplace({1});

    EXPECT_EQ(img, expected);
}

Y_UNIT_TEST(lenna_unsharp)
{
    std::string root = SRC_("data");
    UInt8Image img = UInt8Image::fromFile(root + "/lenna.png");

    {
        auto res = img.gaussianBlur({2, 11, BorderType::Replicate});
        auto exp = UInt8Image::fromFile(root + "/lenna_blur_11x2.png");
        EXPECT_EQ(res, exp);
    }
    {
        auto res = img.unsharp({4, 0.5, 11, BorderType::Replicate});
        auto exp = UInt8Image::fromFile(root + "/lenna_sharp_4,0.5.png");
        EXPECT_EQ(res, exp);
    }
    {
        auto res = img.unsharpLab({4, 0.5, 11, BorderType::Replicate});
        auto exp = UInt8Image::fromFile(root + "/lenna_sharp_lab_4,0.5.png");
        EXPECT_EQ(res, exp);
    }
    {
        auto res = img.unsharpLab({.amount = 4, .sigma = 0.5, .kernel = 11, .border = BorderType::Replicate, .maskGamma=0.5});
        auto exp = UInt8Image::fromFile(root + "/lenna_sharp_lab_4,0.5,g0.5.png");
        EXPECT_EQ(res, exp);
    }
    {
        auto res = img.unsharpLab({.amount = 4, .sigma = 0.5, .kernel = 11, .border = BorderType::Replicate, .maskGamma=2});
        auto exp = UInt8Image::fromFile(root + "/lenna_sharp_lab_4,0.5,g2.png");
        EXPECT_EQ(res, exp);
    }
    {
        auto res = img.unsharpLab({.amount = 4, .sigma = 0.5, .kernel = 11, .border = BorderType::Replicate,
            .threshold = 0.002, .smoothThresholdMin=0.15, .smoothThresholdMax=0.01});
        auto exp = UInt8Image::fromFile(root + "/lenna_sharp_lab_4,0.5,thr.png");
        EXPECT_EQ(res, exp);
    }
    {
        auto res1 = img.unsharp({.amount = 3, .sigma = 0.5, .kernel =11, .border = BorderType::Replicate, .threshold = 1e-5});
        auto res2 = img.unsharp({.amount = 3, .sigma = 0.5, .kernel =11, .border = BorderType::Replicate, .threshold = 0});
        EXPECT_EQ(res1, res2);
    }
}

Y_UNIT_TEST(not_shift_when_sharping)
{
    std::string root = SRC_("data");
    UInt8Image img = UInt8Image::fromFile(root + "/lenna.png");
    EXPECT_LE(img.unsharp({.amount=1e-3}).maxAbsDifference(img), 1);
    EXPECT_LE(img.unsharpLab({.amount=1e-3}).maxAbsDifference(img), 1);
}

Y_UNIT_TEST(clamp)
{
    const Size2i sz(2, 3);
    FloatImage img = FloatImage::zero(sz, 1);
    img.array() << -0.1, 0.0, 0.1, 0.9, 1.0, 1.1;
    FloatImage expected = FloatImage::zero(sz, 1);
    expected.array() << 0.0, 0.0, 0.1, 0.9, 1.0, 1.0;

    {
        EigenMallocNotAllowed notAllowed;
        img.clamp();
    }

    EXPECT_EQ(img, expected);
}

Y_UNIT_TEST(apply_thresholds)
{
    const Size2i sz(5, 5);
    FloatImage img = FloatImage::zero(sz, 1);
    img.array() <<
        -2.75, -2.5, -2.25, -2.0,
        -1.75, -1.5, -1.25, -1.0, -0.75,
        -0.5, -0.25, -0.1, 0.0, 0.1, 0.25, 0.5,
        0.75, 1.0, 1.25, 1.5, 1.75,
        2.0, 2.25, 2.5, 2.75;
    FloatImage expected = FloatImage::zero(sz, 1);
    expected.array() <<
        -2.125, -2.0, -1.875, -1.75,
        -1.5625, -1.375, -1.1875, -1.0, -0.75,
        -0.4524187090179798, -0.1761720224296784, 0, 0, 0, 0.1761720224296784, 0.4524187090179798,
        0.75, 1.0, 1.1875, 1.375, 1.5625,
        1.75, 1.875, 2.0, 2.125;

    img.applyThresholds(0.2f, 0.6f, 1.0f);

    EXPECT_EQ(img, expected);
}

Y_UNIT_TEST(unsharp_uint8)
{
    UInt8Image img = UInt8Image::fromFile(SRC_("data/blur_5x1_top.png"));
    UInt8Image expected = UInt8Image::fromFile(SRC_("data/unsharp_5x1_blur.png"));

    UInt8Image result = img.unsharp(UnsharpOptions{
        .amount = 1, .sigma=1, .kernel=11, .border=BorderType::Replicate
    });
    EXPECT_LE(result.maxAbsDifference(expected), 2);
}

Y_UNIT_TEST(unsharp_inplace)
{
    FloatImage img =
        UInt8Image::fromFile(SRC_("data/blur_5x1_top.png")).colorCast<float>();
    FloatImage expected =
        UInt8Image::fromFile(SRC_("data/unsharp_5x1_blur.png")).colorCast<float>();

    img.unsharpInplace(UnsharpOptions{
        .amount = 1, .sigma=1, .kernel=5, .border=BorderType::Replicate, .threshold = 0.0001
    });

    EXPECT_LE(img.maxAbsDifference(expected), 2);
}

Y_UNIT_TEST(unsharp_float)
{
    UInt8Image img = UInt8Image::fromFile(SRC_("data/blur_5x1_top.png"));
    UInt8Image expected = UInt8Image::fromFile(SRC_("data/unsharp_5x1_blur.png"));

    FloatImage result = img.colorCast<float>().unsharp(UnsharpOptions{
        .amount = 1, .sigma=1, .kernel=11, .border=BorderType::Replicate,
    });

    EXPECT_LE(result.colorCast<uint8_t>().maxAbsDifference(expected), 2);
}

Y_UNIT_TEST(unsharp_uint8_with_thresholds)
{
    UInt8Image img = UInt8Image::fromFile(SRC_("data/blur_5x1_top.png"));
    UInt8Image expected = UInt8Image::fromFile(SRC_("data/unsharp_5x1+4+0.1_blur.png"));

    UInt8Image result = img.unsharp(UnsharpOptions{
        .amount = 4, .sigma=1, .kernel=11, .border=BorderType::Replicate, .threshold = 0.1 / 2
    });
    EXPECT_LE(result.meanAbsDifference(expected), 1.5);
}

Y_UNIT_TEST(correct_gamma)
{
    UInt8Image src = UInt8Image::fromFile(SRC_("data/blur_5x1_top.png"));
    {
        UInt8Image img = src.clone();
        img.correctGammaInplace(2);
        EXPECT_EQ(img, UInt8Image::fromFile(SRC_("data/gamma_2_blur.png")));
    }
    {
        UInt8Image img = src.clone();
        img.correctGammaInplace(0.5);
        EXPECT_EQ(img, UInt8Image::fromFile(SRC_("data/gamma_0.5_blur.png")));
    }
    {
        UInt8Image img = src.clone();
        img.correctGammaInplace(1.3);
        EXPECT_EQ(img, UInt8Image::fromFile(SRC_("data/gamma_1.3_blur.png")));
    }
}

Y_UNIT_TEST(convert_bgr_to_hls)
{
    const Size2i sz(7, 1);
    FloatImage bgrF = FloatImage::zero(sz, 3);
    //              B    G    R
    bgrF.pix(0) << 0.0, 0.0, 0.0;
    bgrF.pix(1) << 1.0, 0.0, 0.0;
    bgrF.pix(2) << 0.0, 1.0, 0.0;
    bgrF.pix(3) << 0.0, 0.0, 1.0;
    bgrF.pix(4) << 1.0, 1.0, 1.0;
    bgrF.pix(5) << 0.5, 0.5, 0.5;
    bgrF.pix(6) << 0.2, 0.4, 0.6;
    UInt8Image bgrU = bgrF.cast<uint8_t>(255);
    FloatImage hlsF = FloatImage::zero(sz, 3);
    UInt8Image hlsU = UInt8Image::zero(sz, 3);
    //          H [0,360), L [0,1], S [0,1]
    hlsF.pix(0) << 0.0, 0.0, 0.0;
    hlsF.pix(1) << 240, 0.5, 1.0;
    hlsF.pix(2) << 120, 0.5, 1.0;
    hlsF.pix(3) << 0.0, 0.5, 1.0;
    hlsF.pix(4) << 0.0, 1.0, 0.0;
    hlsF.pix(5) << 0.0, 0.5, 0.0;
    hlsF.pix(6) << 30, 0.4, 0.5;
    //          H/2, L*255, S*255
    hlsU.pix(0) << 0, 0, 0;
    hlsU.pix(1) << 120, 128, 255;
    hlsU.pix(2) << 60, 128, 255;
    hlsU.pix(3) << 0, 128, 255;
    hlsU.pix(4) << 0, 255, 0;
    hlsU.pix(5) << 0, 128, 0;
    hlsU.pix(6) << 15, 102, 127;

    FloatImage resF = bgrF.clone();
    UInt8Image resU = bgrU.clone();
    resF.convertColorsInplace(ColorSpace::BGR, ColorSpace::HLS);
    resU.convertColorsInplace(ColorSpace::BGR, ColorSpace::HLS);

    EXPECT_EQ(resF, hlsF);
    EXPECT_EQ(resU, hlsU);

    resF.convertColorsInplace(ColorSpace::HLS, ColorSpace::BGR);
    resU.convertColorsInplace(ColorSpace::HLS, ColorSpace::BGR);

    EXPECT_EQ(resF, bgrF);
    EXPECT_LE(resU.maxAbsDifference(bgrU), 1);
}

Y_UNIT_TEST(convert_bgr_to_lab)
{
    const Size2i sz(7, 1);
    FloatImage bgrF = FloatImage::zero(sz, 3);
    //               B    G    R
    bgrF.pix(0) << 0.0, 0.0, 0.0;
    bgrF.pix(1) << 1.0, 0.0, 0.0;
    bgrF.pix(2) << 0.0, 1.0, 0.0;
    bgrF.pix(3) << 0.0, 0.0, 1.0;
    bgrF.pix(4) << 1.0, 1.0, 1.0;
    bgrF.pix(5) << 0.5, 0.5, 0.5;
    bgrF.pix(6) << 0.2, 0.4, 0.6;
    UInt8Image bgrU = bgrF.cast<uint8_t>(255);
    FloatImage labF = FloatImage::zero(sz, 3);
    UInt8Image labU = UInt8Image::zero(sz, 3);
    //          L [0,100], a [-100,100], b [-100,100]
    labF.pix(0) << 0.0, 0.0, 0.0;
    labF.pix(1) << 32.3, 79.2, -107.9;
    labF.pix(2) << 87.8, -86.2, 83.1;
    labF.pix(3) << 53.2, 80.1, 67.2;
    labF.pix(4) << 100, 0.0, 0.0;
    labF.pix(5) << 53.4, 0.0, 0.0;
    labF.pix(6) << 47.5, 15.5, 36.5;
    //          L*2.55, a+128, b+128
    labU.pix(0) << 0, 128, 128;
    labU.pix(1) << 82, 207, 20;
    labU.pix(2) << 224, 42, 211;
    labU.pix(3) << 136, 208, 195;
    labU.pix(4) << 255, 128, 128;
    labU.pix(5) << 137, 128, 128;
    labU.pix(6) << 122, 143, 164;

    FloatImage resF = bgrF.clone();
    UInt8Image resU = bgrU.clone();
    resF.convertColorsInplace(ColorSpace::BGR, ColorSpace::Lab);
    resU.convertColorsInplace(ColorSpace::BGR, ColorSpace::Lab);

    EXPECT_LE(resF.maxAbsDifference(labF), 0.1);
    EXPECT_EQ(resU, labU);

    resF.convertColorsInplace(ColorSpace::Lab, ColorSpace::BGR);
    resU.convertColorsInplace(ColorSpace::Lab, ColorSpace::BGR);

    EXPECT_LE(resF.maxAbsDifference(bgrF), 0.1);
    EXPECT_LE(resU.meanAbsDifference(bgrU), 0.8);
}

Y_UNIT_TEST(scale_values)
{
    const Size2i sz(5, 1);
    FloatImage img = FloatImage::zero(sz, 3);
    img.pix(0) << 0, -1, 1;
    img.pix(1) << 2.5, 0, 1.5;
    img.pix(2) << 5, 1, 2;
    img.pix(3) << -1, -2, 0;
    img.pix(4) << 6, 2, 3;
    UInt8Image exp = UInt8Image::zero(sz, 3);
    exp.pix(0) << 0, 0, 0;
    exp.pix(1) << 128, 128, 128;
    exp.pix(2) << 255, 255, 255;
    exp.pix(3) << 0, 0, 0;
    exp.pix(4) << 255, 255, 255;

    UInt8Image res = img.scaleCast<uint8_t>(
        Eigen::Array3f(0.f, -1.f, 1.f), Eigen::Array3f(5.f, 1.f, 2.f));

    EXPECT_EQ(res, exp);
}

Y_UNIT_TEST(scale_values_inplace)
{
    const Size2i sz(5, 1);
    FloatImage img = FloatImage::zero(sz, 3);
    img.pix(0) << 0, -1, 1;
    img.pix(1) << 2.5, 0, 1.5;
    img.pix(2) << 5, 1, 2;
    img.pix(3) << -1, -2, 0;
    img.pix(4) << 6, 2, 3;
    FloatImage exp = FloatImage::zero(sz, 3);
    exp.pix(0) << 0, 0, 0;
    exp.pix(1) << 0.5, 0.5, 0.5;
    exp.pix(2) << 1, 1, 1;
    exp.pix(3) << 0, 0, 0;
    exp.pix(4) << 1, 1, 1;

    img.scaleAssignFrom(img,
        Eigen::Array3f(0.f, -1.f, 1.f), Eigen::Array3f(5.f, 1.f, 2.f));
    img.clamp();

    EXPECT_EQ(img, exp);
}

Y_UNIT_TEST(change_image_data_size)
{
    const Size2i size(2, 2);
    const Size2i extSize(3, 3);
    FloatImage img = FloatImage::zero(size, 1);
    img.band(0) <<
        1, 2,
        3, 4;

    img.resize(extSize);
    img.extendInplace(size);

    FloatImage expExtend = FloatImage::zero(extSize, 1);
    expExtend.band(0) <<
        1, 2, 0,
        3, 4, 0,
        0, 0, 0;
    EXPECT_EQ(img, expExtend);

    img.shrinkInplace(size);
    img.resize(size);

    FloatImage expShrink = FloatImage::zero(size, 1);
    expShrink.band(0) <<
        1, 2,
        3, 4;
    EXPECT_EQ(img, expShrink);
}

Y_UNIT_TEST(shrink_inplace)
{
    const Size2i newSize(13, 11);
    const Size2i size(15, 17);
    FloatImage img = FloatImage::random(size, 3);
    FloatImage exp = img.block(newSize);

    img.shrinkInplace(newSize);
    img.resize(newSize);

    EXPECT_EQ(img, exp);
}

Y_UNIT_TEST(extend_inplace)
{
    const Size2i size(13, 11);
    const Size2i newSize(15, 17);
    FloatImage img = FloatImage::random(size, 3);
    FloatImage exp = img.clone();

    img.resize(newSize);
    img.extendInplace(size);

    EXPECT_EQ(img.block(size), exp);
}

Y_UNIT_TEST(extend_and_shrink_inplace)
{
    const Size2i size(13, 11);
    const Size2i newSize(15, 17);
    FloatImage img = FloatImage::random(size, 3);
    FloatImage exp = img.clone();

    img.resize(newSize);
    img.extendInplace(size);
    img.shrinkInplace(size);
    img.resize(size);

    EXPECT_EQ(img, exp);
}

Y_UNIT_TEST(pixel_access)
{
    Int32Image img = Int32Image::zero({3, 2}, 2);
    img.band(0) <<
        1, 2, 3,
        4, 5, 6;
    img.band(1) <<
        10, 20, 30,
        40, 50, 60;
    EXPECT_THAT(img.pix(0), EigEq(Eigen::RowVector2i(1, 10)));
    EXPECT_THAT(img.pix(1), EigEq(Eigen::RowVector2i(2, 20)));
    EXPECT_THAT(img.pix(3), EigEq(Eigen::RowVector2i(4, 40)));
    EXPECT_THAT(img.pix(5), EigEq(Eigen::RowVector2i(6, 60)));
    EXPECT_THAT(img.pix(0, 0), EigEq(Eigen::RowVector2i(1, 10)));
    EXPECT_THAT(img.pix(0, 1), EigEq(Eigen::RowVector2i(2, 20)));
    EXPECT_THAT(img.pix(1, 0), EigEq(Eigen::RowVector2i(4, 40)));
    EXPECT_THAT(img.pix(1, 2), EigEq(Eigen::RowVector2i(6, 60)));
}

Y_UNIT_TEST(calculate_hash)
{
    Int32Image img1({3, 2}, 2, {
        1, 2, 3, 4, 5, 6,
        10, 20, 30, 40, 50, 60,
    });
    Int32Image img2({2, 3}, 2, {
        1, 2, 3, 4, 5, 6,
        10, 20, 30, 40, 50, 60,
    });
    UInt8Image img3({3, 2}, 2, {
        1, 2, 3, 4, 5, 6,
        10, 20, 30, 40, 50, 60,
    });
    Int32Image img4({3, 2}, 2, {
        0, 2, 3, 4, 5, 6,
        10, 20, 30, 40, 50, 60,
    });
    Int32Image img5({3, 2}, 2, {
        0, 2, 3, 4, 5, 6,
        10, 20, 30, 40, 50, 0,
    });
    const std::array<size_t, 5> hashes{img1.hash(), img2.hash(), img3.hash(), img4.hash(), img5.hash()};
    for (size_t i = 0; i < hashes.size(); ++i) {
        for (size_t j = i + 1; j < hashes.size(); ++j) {
            EXPECT_NE(hashes[i], hashes[j]) << i << " " << j;
        }
    }
}

Y_UNIT_TEST(color_correction_identity)
{
    const UInt8Image img = UInt8Image::fromFile(SRC_("data/lenna.png"));
    UInt8Image cImg = img.clone();
    cImg.correctColorsInplace({});
    EXPECT_EQ(img, cImg);
}

Y_UNIT_TEST(color_correction_lightness)
{
    {
        UInt8Image img = UInt8Image::zero(BLOCK_SIZES, 3);
        img.correctColorsInplace({.hue = 0, .saturation = 0, .lightness = -1});
        EXPECT_EQ(img.maxAbsDifference(UInt8Image::zero(BLOCK_SIZES, 3)), 0);
    }
    {
        UInt8Image img = UInt8Image::max(BLOCK_SIZES, 3);
        img.correctColorsInplace({.hue = 0, .saturation = 0, .lightness = 1});
        EXPECT_EQ(img.maxAbsDifference(UInt8Image::max(BLOCK_SIZES, 3)), 0);
    }
    {
        UInt8Image img = UInt8Image::fromFile(SRC_("data/lenna.png"));
        img.correctColorsInplace({.hue = 0, .saturation = 0, .lightness = -1});
        EXPECT_EQ(img.maxAbsDifference(UInt8Image::zero(img.size(), 3)), 0);
    }
}

Y_UNIT_TEST(color_correction_unsharp)
{
    UInt8Image img = UInt8Image::fromFile(SRC_("data/lenna.png"));
    img.correctColorsInplace({}, {4, 0.5, 11, BorderType::Replicate});
    EXPECT_EQ(img.maxAbsDifference(UInt8Image::fromFile(SRC_("data/lenna_cc_sharp.png"))), 0);
}

Y_UNIT_TEST(color_correction_hsl)
{
    const std::pair<ColorCorrectionOptions, std::string> params[]{
        {{1, 0., 0.}, "lenna_cc_high_hue.png"},
        {{-1, 0., 0.}, "lenna_cc_low_hue.png"},
        {{0., 1, 0.}, "lenna_cc_high_saturation.png"},
        {{0., -1, 0.}, "lenna_cc_low_saturation.png"},
        {{0., 0., 1}, "lenna_cc_high_lightness.png"},
        {{0., 0., -1}, "lenna_cc_low_lightness.png"},
        {{0.15, 0.2, 0.1}, "lenna_cc_sample.png"},
    };
    const UInt8Image img = UInt8Image::fromFile(SRC_("data/lenna.png"));
    for (const auto&[param, file]: params) {
        UInt8Image cImg = img.clone();
        cImg.correctColorsInplace(param);
        EXPECT_EQ(cImg.maxAbsDifference(UInt8Image::fromFile(SRC_("data/" + file))), 0);
    }
}

Y_UNIT_TEST(fill_zero_borders)
{
    const Array2i size(5, 5);
    UInt8Image img(size, 1, {
        0, 0, 0, 0, 0,
        0, 0, 1, 0, 0,
        0, 2, 5, 4, 0,
        0, 0, 3, 0, 0,
        0, 0, 0, 0, 0,
    });
    img.fillZeroBorders();
    EXPECT_EQ(img, UInt8Image(size, 1, {
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1,
        2, 2, 5, 4, 4,
        3, 3, 3, 3, 3,
        3, 3, 3, 3, 3,
    }));
}

Y_UNIT_TEST(for_each)
{
    const Array2i size(3, 2);
    UInt8Image img(size, 1, {
        1, 100, 0,
        0, 255, 1,
    });
    img.forEach([&](uint8_t& v) { v += 1; });
    EXPECT_EQ(img, UInt8Image(size, 1, {
        2, 101, 1,
        1, 0, 2,
    }));
}

Y_UNIT_TEST(add_and_subtract_uint8)
{
    const Array2i size(3, 2);
    UInt8Image img1(size, 1, {
        1, 100, 1,
        0, 255, 255,
    });
    UInt8Image img2(size, 1, {
        10, 100, 0,
        0, 255, 1,
    });
    EXPECT_EQ(img1.add(img2), UInt8Image(size, 1, {
        11, 200, 1,
        0, 255, 255,
    }));
    EXPECT_EQ(img1.subtract(img2), UInt8Image(size, 1, {
        0, 0, 1,
        0, 0, 254,
    }));
}

Y_UNIT_TEST(add_and_subtract_int16)
{
    const Array2i size(3, 2);
    Int16Image img1(size, 1, {
        1, 100, 1,
        0, 255, 255,
    });
    Int16Image img2(size, 1, {
        10, 100, 0,
        0, 255, 1,
    });
    EXPECT_EQ(img1.add(img2), Int16Image(size, 1, {
        11, 200, 1,
        0, 510, 256,
    }));
    EXPECT_EQ(img1.subtract(img2), Int16Image(size, 1, {
        -9, 0, 1,
        0, 0, 254,
    }));
}

} // suite
} // namespace maps::factory::image::tests

#include <maps/factory/libs/image/ergas.h>
#include <maps/factory/libs/image/weighted_brovey.h>
#include <maps/factory/libs/dataset/dataset.h>

#include <maps/factory/libs/unittest/tests_common.h>

namespace maps::factory::image::tests {
using namespace maps::factory::tests;

Y_UNIT_TEST_SUITE(ergas_should) {

Y_UNIT_TEST(be_nonnegative)
{
    const Array2i size(100, 100);
    auto img = UInt8Image::random(size, 3);
    auto ref = UInt8Image::random(size, 4);
    EXPECT_GE(ergasSpectral(img, ref, 3), 0);
    EXPECT_GE(ergasSpatial(img, ref, 3), 0);
}

Y_UNIT_TEST(not_differ_the_same_images)
{
    const Array2i size(100, 100);
    auto img = FloatImage::random(size, 4);
    EXPECT_EQ(ergasSpectral(img, img, 3), 0);
}

Y_UNIT_TEST(prefer_brovey_to_zero_or_random)
{
    dataset::TDataset refDs = dataset::OpenDataset(std::string(SRC_("data/vladykino.tif"))).onlyRaster();
    FloatImage ref = refDs.Read<float>();

    WeightedBrovey<4> fuse{};
    FloatImage imgBrovey = FloatImage::zero(ref.size(), ref.bands());
    fuse(ref, imgBrovey);

    UInt16Image ref16 = ref.scaleCast<uint16_t>(0, 1u << 12);
    UInt16Image imgBrovey16 = imgBrovey.scaleCast<uint16_t>(0, 1u << 12);

    auto imgZero = UInt16Image::zero(ref.size(), 4);
    EXPECT_LT(ergasSpectral(imgBrovey16, ref16, 3), ergasSpectral(imgZero, ref16, 3));
    EXPECT_LT(ergasSpatial(imgBrovey16, ref16, 3), ergasSpatial(imgZero, ref16, 3));

    auto imgRandom = UInt16Image::random(ref.size(), 4);
    EXPECT_LT(ergasSpectral(imgBrovey16, ref16, 3), ergasSpectral(imgRandom, ref16, 3));
    EXPECT_LT(ergasSpatial(imgBrovey16, ref16, 3), ergasSpatial(imgRandom, ref16, 3));
}

}

}
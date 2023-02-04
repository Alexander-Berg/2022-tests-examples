#include <maps/factory/libs/dataset/local_rgb_dataset.h>

#include <maps/factory/libs/unittest/tests_common.h>

namespace maps::factory::dataset::tests {
using namespace factory::tests;

Y_UNIT_TEST_SUITE(local_rgb_dataset_should) {

Y_UNIT_TEST(read_mask_from_grayscale_image)
{
    //const std::string path = ArcadiaSourceRoot() + "/maps/factory/test_data/cog/scanex_13174627/MUL.tif";
    //Dataset ds = OpenDataset(path);
    //TLocalRgbDataset rgbDs(0, path, ds.Site(), makeBlockCache());

    //const UInt16Image img = ds.Read<uint16_t>();
    //UInt8Image expected(img.size(), 1);
    //for (Index i = 0; i < img.indices(); ++i) {
    //    const bool invalid = img.val(i, 0) == 0;
    //    expected.val(i, 0) = invalid ? ZeroMaskDataset::INVALID_VALUE : ZeroMaskDataset::VALID_VALUE;
    //}
    //const UInt8Image maskImg = maskDs.ds().Read<uint8_t>();
    //EXPECT_DOUBLE_EQ(maskImg.maxAbsDifference(expected), 0.0);
}

} // suite
} // namespace maps::factory::dataset::tests

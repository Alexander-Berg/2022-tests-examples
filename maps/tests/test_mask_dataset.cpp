#include <maps/factory/libs/dataset/mask_dataset.h>

#include <maps/factory/libs/unittest/tests_common.h>

namespace maps::factory::dataset::tests {
using namespace factory::tests;

Y_UNIT_TEST_SUITE(zero_mask_dataset_should) {

Y_UNIT_TEST(read_mask_from_grayscale_image)
{
    const auto path = ArcadiaSourceRoot() + "/maps/factory/test_data/geotif_corpus/moscow_32637.tif";
    TDataset ds = OpenDataset(path);
    ZeroMaskDataset maskDs(ds.ref(), Shared<TEmptyBlockCache>());
    const UInt16Image img = ds.Read<uint16_t>();
    UInt8Image expected(img.size(), 1);
    for (Index i = 0; i < img.indices(); ++i) {
        const bool invalid = img.val(i, 0) == 0;
        expected.val(i, 0) = invalid ? ZeroMaskDataset::INVALID_VALUE : ZeroMaskDataset::VALID_VALUE;
    }
    const UInt8Image maskImg = maskDs.ds().Read<uint8_t>();
    EXPECT_DOUBLE_EQ(maskImg.maxAbsDifference(expected), 0.0);
}

Y_UNIT_TEST(read_mask_from_multiband_image)
{
    const auto path = ArcadiaSourceRoot() + "/maps/factory/test_data/geotif_corpus/ikonos_3395.tif";
    TDataset ds = OpenDataset(path);
    ZeroMaskDataset maskDs(ds.ref(), Shared<TEmptyBlockCache>());
    const UInt16Image img = ds.Read<uint16_t>();
    UInt8Image expected(img.size(), 1);
    for (Index i = 0; i < img.indices(); ++i) {
        const bool invalid = (img.pix(i) == 0).all();
        expected.val(i, 0) = invalid ? ZeroMaskDataset::INVALID_VALUE : ZeroMaskDataset::VALID_VALUE;
    }
    UInt8Image maskImg = maskDs.ds().Read<uint8_t>();
    EXPECT_DOUBLE_EQ(maskImg.maxAbsDifference(expected), 0.0);
}

} // suite
} // namespace maps::factory::dataset::tests

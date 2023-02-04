#include <maps/factory/libs/dataset/vrt_dataset.h>

#include <maps/factory/libs/image/image.h>

#include <maps/factory/libs/unittest/tests_common.h>

namespace maps::factory::dataset::tests {
using namespace maps::factory::tests;

Y_UNIT_TEST_SUITE(vrt_dataset_should) {

Y_UNIT_TEST(reopen_self)
{
    TDataset ds = OpenDataset(MOSCOW_PATH);
    VrtDataset vrt(ds.size());
    vrt.copyInfoFrom(ds);
    vrt.add(ds);

    TDataset reopened = vrt.reopen();
    EXPECT_EQ(reopened.projection(), ds.projection());
    EXPECT_THAT(reopened.size(), EigEq(ds.size()));
    EXPECT_THAT(reopened.Site().PixToProj().matrix(), EigEq(ds.Site().PixToProj().matrix()));
    EXPECT_EQ(reopened.Read<uint16_t>(), ds.Read<uint16_t>());
}

Y_UNIT_TEST(shift_origin)
{
    // When opening result .vrt file on QGis, it will have different origin,
    // but map will be in the same place. All areas without data will be filled by zeros.
    TDataset srcDs = OpenDataset(MOSCOW_PATH);
    const Size2i srcSize = srcDs.size();
    const Point2i origin(2, 4);

    VrtDataset vrt(srcSize);
    vrt.copyInfoFrom(srcDs);
    vrt.shiftOriginToPix(origin);
    vrt.add(srcDs);

    UInt8Image srcImg = srcDs.Read<uint8_t>();
    UInt8Image img = vrt.Read<uint8_t>();

    EXPECT_NE(img, srcImg);
    EXPECT_EQ(img.block(srcSize.matrix() - origin), srcImg.block(origin, srcSize.matrix() - origin));
}

} // suite

} //namespace maps::factory::dataset::tests

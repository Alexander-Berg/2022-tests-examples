#include <maps/factory/libs/dataset/adaptive_buffering.h>

#include <maps/factory/libs/unittest/tests_common.h>

namespace maps::factory::dataset::tests {
using namespace maps::factory::tests;

Y_UNIT_TEST_SUITE(adaptive_buffer_should) {

Y_UNIT_TEST(not_buffer_when_all_raster_not_black)
{
    TDataset ds = OpenDataset(SRTM_PATH);
    Geometry bounds = makePolygonGeometry(ds.Site().ProjBounds()).buffered(-0.1);
    bounds.assignSpatialReference(ds.projection());
    AdaptiveBuffering ab{};
    ab.setSegmentLengthInPixels(2);
    auto[buffered, state] = ab(bounds, ds);
    EXPECT_TRUE(ab.valid(state));
    EXPECT_TRUE(buffered.isEquals(*bounds));
}

Y_UNIT_TEST(buffer_until_no_black_pixels_inside)
{
    TDataset ds = OpenDataset(IKONOS_PATH);
    Geometry bounds = makePolygonGeometry(ds.Site().ProjBounds()).buffered(-0.1);
    bounds.assignSpatialReference(ds.projection());
    AdaptiveBuffering ab{};
    ab.setMaxBlackPixels(0);
    ab.setSegmentLengthInPixels(1);
    auto[buffered, state] = ab(bounds, ds);
    EXPECT_TRUE(ab.valid(state));
    EXPECT_DOUBLE_EQ(state.bufferDistance, -3210.3551879699999);
    EXPECT_EQ(state.iterations, 42);
    EXPECT_TRUE(buffered.isEquals(*bounds.buffered(state.bufferDistance)));
}

} // suite

} //namespace maps::factory::dataset::tests

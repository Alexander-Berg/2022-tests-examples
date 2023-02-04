#include <maps/jams/renderer2/common/yacare/lib/render.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::jams::renderer::tests {

using namespace maps::renderer;
using namespace ::testing;

Y_UNIT_TEST_SUITE(render_tests) {

Y_UNIT_TEST(make_layer_filter)
{
    data_set::LayerInfo trf("trf", feature::FeatureType::Polyline);
    data_set::LayerInfo trfe("trfe", feature::FeatureType::Point);
    data_set::LayerInfo road("road", feature::FeatureType::Polyline);
    data_set::LayerInfo trj("trj", feature::FeatureType::Polyline);

    auto layerFilter = makeLayerFilter({});
    EXPECT_FALSE(layerFilter(trf));
    EXPECT_FALSE(layerFilter(trfe));
    EXPECT_FALSE(layerFilter(road));
    EXPECT_FALSE(layerFilter(trj));

    layerFilter = makeLayerFilter({"trf"});
    EXPECT_TRUE(layerFilter(trf));
    EXPECT_FALSE(layerFilter(trfe));
    EXPECT_FALSE(layerFilter(road));
    EXPECT_FALSE(layerFilter(trj));

    layerFilter = makeLayerFilter({"trj"});
    EXPECT_TRUE(layerFilter(trf));
    EXPECT_FALSE(layerFilter(trfe));
    EXPECT_FALSE(layerFilter(road));
    EXPECT_FALSE(layerFilter(trj));

    layerFilter = makeLayerFilter({"trf", "trfe"});
    EXPECT_TRUE(layerFilter(trf));
    EXPECT_FALSE(layerFilter(trfe));
    EXPECT_FALSE(layerFilter(road));
    EXPECT_FALSE(layerFilter(trj));

    layerFilter = makeLayerFilter({"trje"});
    EXPECT_FALSE(layerFilter(trf));
    EXPECT_FALSE(layerFilter(trfe));
    EXPECT_FALSE(layerFilter(road));
    EXPECT_FALSE(layerFilter(trj));

    layerFilter = makeLayerFilter({"closed", "speed_control"});
    EXPECT_FALSE(layerFilter(trf));
    EXPECT_FALSE(layerFilter(trfe));
    EXPECT_FALSE(layerFilter(road));
    EXPECT_FALSE(layerFilter(trj));

    layerFilter = makeLayerFilter({"speed_control", "trf"});
    EXPECT_TRUE(layerFilter(trf));
    EXPECT_FALSE(layerFilter(trfe));
    EXPECT_FALSE(layerFilter(road));
    EXPECT_FALSE(layerFilter(trj));

    layerFilter = makeLayerFilter({"road"});
    EXPECT_FALSE(layerFilter(trf));
    EXPECT_FALSE(layerFilter(trfe)); // maybe road is a new tag
    EXPECT_FALSE(layerFilter(road));
    EXPECT_FALSE(layerFilter(trj));
}

Y_UNIT_TEST(make_event_allowed_tags)
{
    EXPECT_THAT(
        makeEventAllowedTags({}),
        IsEmpty());

    EXPECT_THAT(
        makeEventAllowedTags({"trf"}),
        IsEmpty());

    EXPECT_THAT(
        makeEventAllowedTags({"trfe"}),
        IsEmpty());

    EXPECT_THAT(
        makeEventAllowedTags({"closed", "speed_control"}),
        UnorderedElementsAre("closed", "speed_control"));

    EXPECT_THAT(
        makeEventAllowedTags({"closed", "speed_control", "trf", "trj"}),
        UnorderedElementsAre("closed", "speed_control"));

    EXPECT_THAT(
        makeEventAllowedTags({"closed", "speed_control", "trfe"}),
        IsEmpty());

    EXPECT_THAT(
        makeEventAllowedTags({"closed", "speed_control", "trje"}),
        IsEmpty());

    EXPECT_THAT(
        makeEventAllowedTags({"speed_camera", "lane_camera", "road"}),
        UnorderedElementsAre("speed_control", "lane_control", "road"));
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::jams::renderer::tests

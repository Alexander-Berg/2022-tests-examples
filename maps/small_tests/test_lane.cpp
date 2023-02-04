#include <yandex/maps/wiki/common/rd/lane.h>

#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::common::tests {

Y_UNIT_TEST_SUITE(lane) {

Y_UNIT_TEST(test_lanes_repack)
{
    const std::string& testLanes = "L180,L90:auto;L90,S:auto;S:auto;S,R45:auto;R45,R135:bus;N:bike";
    auto lanes = lanesFromString(testLanes);
    UNIT_ASSERT_VALUES_EQUAL(lanes.size(), 6);
    UNIT_ASSERT(lanes[0].contains(LaneDirection::Left180));
    UNIT_ASSERT_EQUAL(lanes[0].kind(), LaneKind::Auto);
    UNIT_ASSERT(lanes.back().contains(LaneDirection::None));
    UNIT_ASSERT_EQUAL(lanes.back().kind(), LaneKind::Bike);
    UNIT_ASSERT_STRINGS_EQUAL(testLanes, toString(lanes));
}

Y_UNIT_TEST(should_create_empty_lanes)
{
    UNIT_ASSERT(lanesFromString("").empty());
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::common::tests

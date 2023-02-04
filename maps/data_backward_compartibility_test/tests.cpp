
#include <yandex/maps/coverage5/coverage.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/common/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::coverage5::tests {

Y_UNIT_TEST_SUITE(data_backward_compartibility_should) {

Y_UNIT_TEST(test_open_geoid_layer)
{
    const std::string TEST_GEOID_PATH = BinaryPath("maps/data/test/geoid");
    Coverage coverage(TEST_GEOID_PATH);
    const Layer& layer = coverage["geoid"];
    auto maybeRegion = layer.minAreaRegion(geolib3::Point2(37, 55), boost::none);
    EXPECT_TRUE(maybeRegion);
    EXPECT_TRUE(maybeRegion->id());
    EXPECT_EQ(*maybeRegion->id(), 10693);
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::coverage5::tests

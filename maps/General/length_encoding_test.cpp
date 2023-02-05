#include <library/cpp/testing/unittest/registar.h>

#include <maps/libs/road_graph/impl/length_encoding.h>

namespace rg = maps::road_graph;

Y_UNIT_TEST_SUITE(LengthEncoding) {

Y_UNIT_TEST(WeightPackUnpack) {
    UNIT_ASSERT_EQUAL(1.0f, rg::decodeLength(rg::encodeLength(0.9999f)));
    UNIT_ASSERT_EQUAL(0.9f, rg::decodeLength(rg::encodeLength(0.9099f)));
    UNIT_ASSERT_EQUAL(0.9f, rg::decodeLength(rg::encodeLength(0.9f)));
    UNIT_ASSERT_EQUAL(1.2f, rg::decodeLength(rg::encodeLength(1.1850f)));
}

};

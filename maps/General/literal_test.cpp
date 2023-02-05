#include <library/cpp/testing/unittest/registar.h>

#include <maps/libs/road_graph/include/types.h>

using namespace maps::road_graph::literals;
using namespace maps::road_graph;

Y_DECLARE_OUT_SPEC(inline, VertexId, stream, value) {
    stream << value.value();
}

Y_DECLARE_OUT_SPEC(inline, EdgeId, stream, value) {
    stream << value.value();
}

Y_DECLARE_OUT_SPEC(inline, SegmentIndex, stream, value) {
    stream << value.value();
}

Y_UNIT_TEST_SUITE(Literals) {

Y_UNIT_TEST(LiteralTest) {
    UNIT_ASSERT_VALUES_EQUAL(maps::road_graph::EdgeId(123), 123_e);
    UNIT_ASSERT_VALUES_EQUAL(maps::road_graph::VertexId(123), 123_v);
    UNIT_ASSERT_VALUES_EQUAL(maps::road_graph::SegmentIndex(123), 123_seg);
}

};

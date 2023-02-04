#include <maps/garden/modules/ymapsdf_osm/lib/common/point_lookup.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <map>

using maps::geolib3::Point2;
using namespace maps::garden::modules::ymapsdf::geometry;

namespace {
double toMeter(double degree) {
    return 40000000.0 / 360.0 * degree;
}
} // namespace

Y_UNIT_TEST_SUITE(TileCutterTestSuite) {

Y_UNIT_TEST(point_lookup_test) {
    std::map<PointId, Point2> testData {
        {1, Point2(0.0, 0.0)},
        {2, Point2(0.0, 9.0)},
        {3, Point2(10.0, 10.0)},
        {4, Point2(10.0, 0.0)},
        {5, Point2(5.0, 5.0)},
        {6, Point2(15.0, 15.0)},
        {7, Point2(15.01, 15.01)}
    };
    KDTreeBuilder builder;
    for (const auto [id, point]: testData) {
        builder.add(toKDTreePoint(point), id);
    }
    PointLookup lookup(builder.build());

    ASSERT_EQ(lookup.findNearestPoint(toKDTreePoint(Point2(-0.00001, 0.00001)), toMeter(0.000015))->id, 1);
    ASSERT_EQ(lookup.findNearestPoint(toKDTreePoint(Point2(-1.0, -1.0)), toMeter(2.0))->id, 1);
    ASSERT_EQ(lookup.findNearestPoint(toKDTreePoint(Point2(1.0, 1.0)), toMeter(2.0))->id, 1);
    ASSERT_EQ(lookup.findNearestPoint(toKDTreePoint(Point2(1.0, 1.0)), toMeter(1.0)), std::nullopt);
    ASSERT_EQ(lookup.findNearestPoint(toKDTreePoint(Point2(0.1, 5.0)), toMeter(6.0))->id, 2);
    ASSERT_EQ(lookup.findNearestPoint(toKDTreePoint(Point2(4.0, 4.0)), toMeter(2.0))->id, 5);
    ASSERT_EQ(lookup.findNearestPoint(toKDTreePoint(Point2(4.0, 4.0)), toMeter(20.0))->id, 5);
    ASSERT_EQ(lookup.findNearestPoint(toKDTreePoint(Point2(12.0, 12.0)), toMeter(6.0))->id, 3);
    ASSERT_EQ(lookup.findNearestPoint(toKDTreePoint(Point2(13.0, 13.0)), toMeter(6.0))->id, 6);
    ASSERT_EQ(lookup.findNearestPoint(toKDTreePoint(Point2(13.0, 13.0)), toMeter(1.0)), std::nullopt);
    ASSERT_EQ(lookup.findNearestPoint(toKDTreePoint(Point2(15.009, 15.008)), toMeter(0.01))->id, 7);
    ASSERT_EQ(lookup.findNearestPoint(toKDTreePoint(Point2(15.009, 15.008)), toMeter(0.001)), std::nullopt);
}

} // Y_UNIT_TEST_SUITE(TileCutterTestSuite)

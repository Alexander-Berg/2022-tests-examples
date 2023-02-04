#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <maps/bicycle/router/lib/output/proto.h>
#include <maps/bicycle/router/lib/uri.h>

#include <yandex/maps/proto/bicycle/route.pb.h>
#include <yandex/maps/proto/uri/uri.pb.h>

namespace maps::bicycle::tests {

using namespace maps::geolib3;
using namespace yandex::maps::proto::bicycle;
using namespace yandex::maps::proto::common2::geo_object;
using maps::bicycle::Construction;
using maps::bicycle::TrafficType;
namespace proto_uri = yandex::maps::proto::uri;

namespace {

Path path0()
{
    return Path(
        {WayPoint{{0.1, 2.3}}, WayPoint{{4.5, 6.7}}},
        /* polyline = */ Polyline2{
            std::vector<Point2>{{0.1, 2.3}, {4.5, 6.7}}},
        {Path::Leg{
            Weight{/* distance = */ 8.9, /* time = */ 10.11},
            /* begin = */ 0, /* end = */ 1}},
        /* accessPassesIndexes = */ {},
        /* constructions = */ {{Construction::ID::UNKNOWN, 1}},
        /* trafficType = */ {{TrafficType::ID::AUTO, 1}},
        /* vehicleType = */ VehicleType::Bicycle,
        /* bindingSpeed = */ 0.5
    );
}

Path path1()
{
    return Path(
        {WayPoint{{12.13, 14.15}}, WayPoint{{16.17, 18.19}}},
        /* polyline = */ Polyline2{
            std::vector<Point2>{{12.13, 14.15}, {16.17, 18.19}}},
        {Path::Leg{
                Weight{/* distance = */ 20.21, /* time = */ 22.23},
                 /* begin = */ 0, /* end = */ 1}},
        /* accessPassesIndexes = */ {0},
        /* constructions = */ {{Construction::ID::UNKNOWN, 1}},
        /* trafficType = */ {{TrafficType::ID::OTHER, 1}},
        /* vehicleType = */ VehicleType::Bicycle,
        /* bindingSpeed = */ 0.5
    );
}

} // namespace


#define CHECK_WEIGHT(data, expectedDistance, expectedTime) \
    ASSERT_TRUE((data).has_weight()); \
    ASSERT_TRUE((data).weight().has_distance()); \
    ASSERT_EQ((data).weight().distance().value(), expectedDistance); \
    ASSERT_TRUE((data).weight().has_time()); \
    ASSERT_EQ((data).weight().time().value(), expectedTime);


#define CHECK_FLAGS(data, expectedAccessPass, expectedAutoRoad) \
    ASSERT_TRUE((data).has_flags()); \
    ASSERT_TRUE((data).flags().has_requires_access_pass()); \
    EXPECT_EQ((data).flags().requires_access_pass(), expectedAccessPass); \
    ASSERT_TRUE((data).flags().has_has_auto_road()); \
    EXPECT_EQ((data).flags().has_auto_road(), expectedAutoRoad);

#define CHECK_URI(uri) \
    ASSERT_EQ((uri).uri().size(), 1); \
    ASSERT_TRUE((uri).uri(0).has_uri()); \
    ASSERT_TRUE((uri).uri(0).uri().StartsWith(maps::bicycle::uri::URI_PREFIX));


Y_UNIT_TEST_SUITE(Proto)
{

Y_UNIT_TEST(AGeoObject)
{
    Path path(
        {WayPoint{{12.13, 14.15}}, WayPoint{{16.17, 18.19}, {{16.175, 18.195}}}},
        Polyline2{std::vector<Point2>{
            {12.13, 14.15}, {0.1, 2.3},  {4.5, 6.7}, {16.17, 18.19}
        }},
        {Path::Leg{
            Weight{/* distance = */ 8.9, /* time = */ 10.11},
            /* begin = */ 0, /* end = */ 3}},
        /* accessPassesIndexes = */ {},
        /* constructions = */ {{Construction::ID::UNKNOWN, 1}},
        /* trafficTypes = */ {{TrafficType::ID::OTHER, 1}},
        /* vehicleType = */ VehicleType::Bicycle,
        /* bindingSpeed = */ 0.5
       );

    auto actual = proto::toGeoObject(path, "UNUSED_ROUTE_ID", std::locale());

    ASSERT_EQ(actual.geometry_size(), 1);
    ASSERT_TRUE(actual.geometry(0).has_polyline());

    ASSERT_TRUE(actual.geometry(0).polyline().has_lons());
    ASSERT_TRUE(actual.geometry(0).polyline().lons().has_first());
    EXPECT_EQ(actual.geometry(0).polyline().lons().first(), 12130000);
    ASSERT_EQ(actual.geometry(0).polyline().lons().deltas_size(), 3);
    EXPECT_EQ(actual.geometry(0).polyline().lons().deltas(0), 100000 - 12130000);
    EXPECT_EQ(actual.geometry(0).polyline().lons().deltas(1), 4500000 - 100000);
    EXPECT_EQ(actual.geometry(0).polyline().lons().deltas(2), 16170000 - 4500000);

    ASSERT_TRUE(actual.geometry(0).polyline().has_lats());
    ASSERT_TRUE(actual.geometry(0).polyline().lats().has_first());
    EXPECT_EQ(actual.geometry(0).polyline().lats().first(), 14150000);
    ASSERT_EQ(actual.geometry(0).polyline().lats().deltas_size(), 3);
    EXPECT_EQ(actual.geometry(0).polyline().lats().deltas(0), 2300000 - 14150000);
    EXPECT_EQ(actual.geometry(0).polyline().lats().deltas(1), 6700000 - 2300000);
    EXPECT_EQ(actual.geometry(0).polyline().lats().deltas(2), 18190000 - 6700000);

    ASSERT_EQ(actual.metadata_size(), 2);
    ASSERT_TRUE(actual.metadata(0).HasExtension(route::ROUTE_METADATA));
    ASSERT_TRUE(actual.metadata(1).HasExtension(proto_uri::GEO_OBJECT_METADATA));

    auto route = actual.metadata(0).GetExtension(route::ROUTE_METADATA);
    CHECK_WEIGHT(route, /* distance = */ 8, /* time = */ 10);
    CHECK_FLAGS(route, /* access_pass = */ false, /* auto_road = */ false);

    auto uri = actual.metadata(1).GetExtension(proto_uri::GEO_OBJECT_METADATA);
    CHECK_URI(uri);

    ASSERT_EQ(route.way_point_size(), 2);
    EXPECT_EQ(route.way_point(0).position().lon(), 12.13);
    EXPECT_EQ(route.way_point(0).position().lat(), 14.15);
    EXPECT_FALSE(route.way_point(0).has_selected_arrival_point());

    EXPECT_EQ(route.way_point(1).position().lon(), 16.17);
    EXPECT_EQ(route.way_point(1).position().lat(), 18.19);
    ASSERT_TRUE(route.way_point(1).has_selected_arrival_point());
    EXPECT_EQ(route.way_point(1).selected_arrival_point().lon(), 16.175);
    EXPECT_EQ(route.way_point(1).selected_arrival_point().lat(), 18.195);
}

Y_UNIT_TEST(AResponse)
{
    auto actualObject0 = proto::toGeoObject(path0(), "UNUSED_ROUTE_ID", std::locale());

    ASSERT_EQ(actualObject0.geometry_size(), 1);
    ASSERT_TRUE(actualObject0.geometry(0).has_polyline());
    ASSERT_TRUE(actualObject0.geometry(0).polyline().has_lats());

    ASSERT_TRUE(actualObject0.geometry(0).polyline().has_lons());
    ASSERT_TRUE(actualObject0.geometry(0).polyline().lons().has_first());
    ASSERT_EQ(actualObject0.geometry(0).polyline().lons().first(), 100000);
    ASSERT_EQ(actualObject0.geometry(0).polyline().lons().deltas_size(), 1);
    ASSERT_EQ(actualObject0.geometry(0).polyline().lons().deltas(0), 4500000 - 100000);

    ASSERT_TRUE(actualObject0.geometry(0).polyline().lats().has_first());
    ASSERT_EQ(actualObject0.geometry(0).polyline().lats().first(), 2300000);
    ASSERT_EQ(actualObject0.geometry(0).polyline().lats().deltas_size(), 1);
    ASSERT_EQ(actualObject0.geometry(0).polyline().lats().deltas(0), 6700000 - 2300000);

    ASSERT_EQ(actualObject0.metadata_size(), 2);
    ASSERT_TRUE(actualObject0.metadata(0).HasExtension(route::ROUTE_METADATA));
    ASSERT_TRUE(actualObject0.metadata(1).HasExtension(proto_uri::GEO_OBJECT_METADATA));

    auto route = actualObject0.metadata(0).GetExtension(route::ROUTE_METADATA);
    CHECK_WEIGHT(route, /* distance = */ 8, /* time = */ 10);
    CHECK_FLAGS(route, /* access_pass = */ false, /* auto_road = */ true);

    auto uri = actualObject0.metadata(1).GetExtension(proto_uri::GEO_OBJECT_METADATA);
    CHECK_URI(uri);

    auto actualObject1 = proto::toGeoObject(path1(), "UNUSED_ROUTE_ID", std::locale());

    ASSERT_EQ(actualObject1.geometry_size(), 1);
    ASSERT_TRUE(actualObject1.geometry(0).has_polyline());
    ASSERT_TRUE(actualObject1.geometry(0).polyline().has_lats());

    ASSERT_TRUE(actualObject1.geometry(0).polyline().has_lons());
    ASSERT_TRUE(actualObject1.geometry(0).polyline().lons().has_first());
    ASSERT_EQ(actualObject1.geometry(0).polyline().lons().first(), 12130000);
    ASSERT_EQ(actualObject1.geometry(0).polyline().lons().deltas_size(), 1);
    ASSERT_EQ(actualObject1.geometry(0).polyline().lons().deltas(0), 16170000 - 12130000);

    ASSERT_TRUE(actualObject1.geometry(0).polyline().lats().has_first());
    ASSERT_EQ(actualObject1.geometry(0).polyline().lats().first(), 14150000);
    ASSERT_EQ(actualObject1.geometry(0).polyline().lats().deltas_size(), 1);
    ASSERT_EQ(actualObject1.geometry(0).polyline().lats().deltas(0), 18190000 - 14150000);

    ASSERT_EQ(actualObject1.metadata_size(), 2);
    ASSERT_TRUE(actualObject1.metadata(0).HasExtension(route::ROUTE_METADATA));
    ASSERT_TRUE(actualObject1.metadata(1).HasExtension(proto_uri::GEO_OBJECT_METADATA));

    route = actualObject1.metadata(0).GetExtension(route::ROUTE_METADATA);
    CHECK_WEIGHT(route, /* distance = */ 20, /* time = */ 22);
    CHECK_FLAGS(route, /* access_pass = */ true, /* auto_road = */ false);

    uri = actualObject1.metadata(1).GetExtension(proto_uri::GEO_OBJECT_METADATA);
    CHECK_URI(uri);
}

Y_UNIT_TEST(Summaries)
{
    auto summaries = proto::toSummaries(
        {path0().summary(), path1().summary()}, std::locale());

    ASSERT_EQ(summaries.summaries_size(), 2);

    const auto& summary0 = summaries.summaries(0);
    CHECK_WEIGHT(summary0, /* distance = */ 8, /* time = */ 10);
    CHECK_FLAGS(summary0, /* access_pass = */ false, /* auto_road = */ true);

    const auto& summary1 = summaries.summaries(1);
    CHECK_WEIGHT(summary1, /* distance = */ 20, /* time = */ 22);
    CHECK_FLAGS(summary1, /* access_pass = */ true, /* auto_road = */ false);
}

}

} // namespace maps::bicycle::tests

#include <maps/factory/libs/db/aoi_gateway.h>

#include <maps/factory/libs/db/order_gateway.h>

#include <maps/factory/libs/unittest/fixture.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace maps::factory::db::tests {

Y_UNIT_TEST_SUITE(test_aoi_gateway) {

Y_UNIT_TEST(best_match)
{
    unittest::Fixture fixture;
    pqxx::connection conn(fixture.postgres().connectionString());
    pqxx::work txn(conn);

    Order order(2021, OrderType::Tasking);
    OrderGateway(txn).insert(order);

    constexpr auto polyMosaic =
        "POLYGON((37.2416 55.9544,38.0271 55.9544,38.02716 55.5090,37.2416 55.5090,37.2416 55.9544))";
    constexpr auto polyNotIntersected =
        "POLYGON((39.2305 55.7107,39.8018 55.7107,39.8018 55.3750,39.2305 55.3750,39.2305 55.7107))";
    constexpr auto polyIntersected =
        "POLYGON((37.6210 55.9113,38.5933 55.9113,38.5933 55.7849,37.6210 55.7849,37.6210 55.9113))";
    constexpr auto polyMostIntersected =
        "POLYGON((37.2970 55.8466,38.0550 55.8466,38.0550 55.5183,37.2970 55.5183,37.2970 55.8466))";

    Aoi aoiDisjoint(order.id(), "aoi_1", geolib3::WKT::read<geolib3::Polygon2>(polyNotIntersected));
    Aoi aoiIntersected(order.id(), "aoi_2", geolib3::WKT::read<geolib3::Polygon2>(polyIntersected));
    Aoi aoiMostIntersected(order.id(), "aoi_3", geolib3::WKT::read<geolib3::Polygon2>(polyMostIntersected));

    AoiGateway gtw(txn);
    gtw.insert(aoiDisjoint);
    gtw.insert(aoiIntersected);
    gtw.insert(aoiMostIntersected);

    const auto contour = geometry::Geometry::fromWkt(polyMosaic);

    EXPECT_EQ(gtw.bestMatch(order.id(), aoiIntersected.name(), contour).id(), aoiIntersected.id());
    EXPECT_EQ(gtw.bestMatch(order.id(), aoiMostIntersected.name(), contour).id(), aoiMostIntersected.id());
    EXPECT_THROW(Y_UNUSED(gtw.bestMatch(order.id(), aoiDisjoint.name(), contour)), RuntimeError);
    EXPECT_EQ(gtw.bestMatch(order.id(), "wrong_name", contour).id(), aoiMostIntersected.id());
    EXPECT_THROW(Y_UNUSED(gtw.bestMatch(order.id() + 1, aoiMostIntersected.name(), contour)), RuntimeError);
}

} // suite
} // namespace maps::factory::db::tests

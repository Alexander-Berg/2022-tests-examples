#include "../../alternatives_selector/rerouting_comparators.h"
#include "../utils.h"
#include "utils.h"

#include <library/cpp/testing/unittest/registar.h>

using maps::routing::alternatives_selector::ComparingResult;
using maps::routing::alternatives_selector::RouteSimilarityComparator;
using maps::routing::alternatives_selector::TraitSimilarityComparator;

Y_UNIT_TEST_SUITE(rerouting_comparators_tests) {

Y_UNIT_TEST(trait_similarity_comparator) {
    RouteTraits userRouteTraits;
    AlternativeInfo route1(makeRoute());
    AlternativeInfo route2(makeRoute());

    UNIT_ASSERT_EQUAL(
        TraitSimilarityComparator(userRouteTraits).compare(route1, route2),
        ComparingResult::Equal);

    userRouteTraits.set_has_tolls(false);
    route1.result.traits.set_has_tolls(true);

    UNIT_ASSERT_EQUAL(
        TraitSimilarityComparator(userRouteTraits).compare(route1, route2),
        ComparingResult::SecondBetter);

    userRouteTraits.set_has_tolls(true);

    UNIT_ASSERT_EQUAL(
        TraitSimilarityComparator(userRouteTraits).compare(route1, route2),
        ComparingResult::Equal);
}

Y_UNIT_TEST(route_similarity_comparator) {
    Route firstRoute {
        {
            makePathSegment(153163, 0, 0, 0),
            makePathSegment(139761, 0, 0, 0),
            makePathSegment(139796, 0, 0, 0),
            makePathSegment(139799, 0, 0, 0),
            makePathSegment(52898, 0, 0, 0),
            makePathSegment(52900, 0, 0, 0),
            makePathSegment(153387, 0, 0, 0),
            makePathSegment(8155, 0, 0, 0),
        },
        {
            RequestPointWithPosition(
                0, RequestPoint(maps::geolib3::Point2(0, 0))),
            RequestPointWithPosition(
                8, RequestPoint(maps::geolib3::Point2(0, 0)))
        }
    };

    Route secondRoute {
        {
            makePathSegment(153163, 0, 0, 0),
            makePathSegment(139761, 0, 0, 0),
            makePathSegment(139796, 0, 0, 0),
            makePathSegment(139799, 0, 0, 0),
            makePathSegment(52898, 0, 0, 0),
            makePathSegment(52901, 0, 0, 0),
            makePathSegment(203955, 0, 0, 0),
            makePathSegment(166125, 0, 0, 0),
            makePathSegment(160203, 0, 0, 0),
            makePathSegment(160204, 0, 0, 0),
            makePathSegment(160204, 0, 0, 0),
            makePathSegment(8155, 0, 0, 0),
        },
        {
            RequestPointWithPosition(
                0, RequestPoint(maps::geolib3::Point2(0, 0))),
            RequestPointWithPosition(
                12, RequestPoint(maps::geolib3::Point2(0, 0)))
        }
    };

    std::vector<AlternativeInfo> routes;
    routes.emplace_back(
        RouterResult(
            VehicleParameters(), Avoid(), std::move(firstRoute), &ROAD_GRAPH));
    routes.emplace_back(
        RouterResult(
            VehicleParameters(), Avoid(), std::move(secondRoute), &ROAD_GRAPH));

    routes.at(0).result.jamsTime = 16.0f;
    routes.at(0).result.length = 1162.1f;

    routes.at(1).result.jamsTime = 10.0f;
    routes.at(1).result.length = 1731.3f;

    UNIT_ASSERT_EQUAL(
        RouteSimilarityComparator(
                std::optional<RouteGeometry>(routes.at(0).result.geometry),
                routes,
                1.5f /*maxRouteDurationRatio*/,
                0.95f /*minSharing*/)
            .compare(routes.at(0), routes.at(1)),
        ComparingResult::Equal);

    routes.at(0).result.jamsTime = 14.0f;

    UNIT_ASSERT_EQUAL(
        RouteSimilarityComparator(
                std::optional<RouteGeometry>(routes.at(0).result.geometry),
                routes,
                1.5f /*maxRouteDurationRatio*/,
                0.95f /*minSharing*/)
            .compare(routes.at(0), routes.at(1)),
        ComparingResult::FirstBetter);
}

}

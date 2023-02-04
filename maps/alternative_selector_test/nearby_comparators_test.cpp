#include "../../alternatives_selector/comparators.h"
#include "../../alternatives_selector/nearby_comparators.h"
#include "../../alternatives_selector/selector.h"
#include "utils.h"

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

using maps::geolib3::Point2;
using maps::routing::PathSegment;

using maps::routing::alternatives_selector::TraitsNestingComparator;
using maps::routing::alternatives_selector::ComparingResult;

namespace {

template <class T, class... Values>
Route routeWithSegments(T PathSegment::* pointer, Values&&... values)
{
    using maps::road_graph::EdgeId;

    Route route;

    auto addValue = [&route, pointer] (auto&& value) {
        PathSegment segment{EdgeId(0), 0, 0, 0, 0, 0};
        segment.*pointer = value;
        route.pathSegments.emplace_back(segment);
    };

    route.requestPoints.emplace_back(
        0, RequestPoint(Point2(0, 0)));

    (addValue(values), ...);

    route.requestPoints.emplace_back(
        route.pathSegments.size(), RequestPoint(Point2(0, 0)));

    return route;
}

} // namespace

void setTraits(
    RouteTraits& traits,
    bool requiresAccessPass,
    bool crossesBorders,
    bool hasFerries,
    bool blocked)
{
    traits.set_requires_access_pass(requiresAccessPass);
    traits.set_crosses_borders(crossesBorders);
    traits.set_has_boat_ferries(hasFerries);
    traits.set_blocked(blocked);
}

Y_UNIT_TEST_SUITE(nearby_alternatives_comparators_tests)
{

Y_UNIT_TEST(traits_nesting_comparator_test)
{
    RouterResult userRoute = makeRoute();
    AlternativeInfo alternative1{makeRoute()};
    AlternativeInfo alternative2{makeRoute()};

    setTraits(userRoute.traits, false, false, false, false);
    std::unique_ptr<TraitsNestingComparator> comparator(
        std::make_unique<TraitsNestingComparator>(userRoute.traits));

   auto makeAlternative = [](
        bool requiresAccessPass,
        bool crossesBorders,
        bool hasFerries,
        bool blocked) {
        AlternativeInfo alternative{makeRoute()};
        setTraits(
            alternative.result.traits,
            requiresAccessPass,
            crossesBorders,
            hasFerries,
            blocked);
        return alternative;
    };
    // both are good
    UNIT_ASSERT_EQUAL(comparator->compare(
        makeAlternative(false, false, false, false),
        makeAlternative(false, false, false, false)), ComparingResult::Equal);

    UNIT_ASSERT_EQUAL(comparator->compare(
        makeAlternative(false, false, false, false),
        makeAlternative(false, false, true, false)), ComparingResult::FirstBetter);

    UNIT_ASSERT_EQUAL(comparator->compare(
        makeAlternative(false, true, false, false),
        makeAlternative(false, false, false, false)), ComparingResult::SecondBetter);

    UNIT_ASSERT_EQUAL(
        comparator->isAcceptable(makeAlternative(false, true, false, false)), false);
    UNIT_ASSERT_EQUAL(
        comparator->isAcceptable(makeAlternative(false, false, true, false)), false);

    setTraits(userRoute.traits, false, true, true, false);
    comparator.reset(new TraitsNestingComparator(userRoute.traits));

    // both are good
    UNIT_ASSERT_EQUAL(comparator->compare(
        makeAlternative(false, false, true, false),
        makeAlternative(false, true, false, false)), ComparingResult::Equal);

    UNIT_ASSERT_EQUAL(comparator->compare(
        makeAlternative(false, true, false, false),
        makeAlternative(false, true, true, true)), ComparingResult::FirstBetter);

    UNIT_ASSERT_EQUAL(comparator->compare(
        makeAlternative(true, true, true, false),
        makeAlternative(false, false, true, false)), ComparingResult::SecondBetter);

    UNIT_ASSERT_EQUAL(
        comparator->isAcceptable(makeAlternative(true, true, true, false)), false);
    UNIT_ASSERT_EQUAL(
        comparator->isAcceptable(makeAlternative(false, true, true, true)), false);
}

Y_UNIT_TEST(end_direction_comparator_test)
{
    using maps::road_graph::EdgeId;
    using maps::routing::alternatives_selector::EndDirectionComparator;
    auto makeAlternative = [] (
        std::optional<EdgeId::ValueType> lastEdgeId,
        std::optional<EdgeId::ValueType> lastEdgeBaseId,
        bool endsInLivingZone = false)
    {
        auto info = AlternativeInfo(makeRoute());
        info.nearbyInfo = NearbyInfo{};
        if (lastEdgeId) {
            info.nearbyInfo->lastEdgeId = EdgeId{*lastEdgeId};
        }
        if (lastEdgeBaseId) {
            info.nearbyInfo->lastEdgeBaseId = EdgeId{*lastEdgeBaseId};
        }
        info.nearbyInfo->endsInLivingZone = endsInLivingZone;
        return info;
    };

    // User route ends on edge 100. Its reverse base edge is 1.
    const auto comparator = EndDirectionComparator(EdgeId{100}, EdgeId{1});

    // Various alternatives
    auto perfect               = makeAlternative(100, 1, false);
    auto noSegments            = makeAlternative({}, {}, false);
    auto totallyWrong          = makeAlternative(200, 2, false);
    auto wrongSide             = makeAlternative(200, 1, false);
    auto wrongSideInLivingZone = makeAlternative(200, 1, true);

    UNIT_ASSERT_VALUES_EQUAL(comparator.compare(perfect, noSegments), ComparingResult::FirstBetter);
    UNIT_ASSERT_VALUES_EQUAL(comparator.compare(perfect, totallyWrong), ComparingResult::FirstBetter);
    UNIT_ASSERT_VALUES_EQUAL(comparator.compare(perfect, wrongSide), ComparingResult::FirstBetter);
    UNIT_ASSERT_VALUES_EQUAL(comparator.compare(perfect, wrongSideInLivingZone), ComparingResult::Equal);
}

Y_UNIT_TEST(time_comparator_test)
{
    using maps::road_graph::EdgeId;
    using maps::routing::alternatives_selector::TimeComparator;

    auto segment = [] (uint32_t edgeId, double time, double jamsTime) {
        return PathSegment({EdgeId(edgeId), 0, time, jamsTime, 0, 0});
    };

    auto routerResult = [&segment] (uint32_t edgeId, double time, double jamsTime, int index) {
        RouterResult result(
            VehicleParameters(),
            Avoid(),
            Route {
                {
                    segment(1, 10, 10),
                    segment(edgeId, time, jamsTime),
                    segment(2, 10, 10),
                },
                {
                    RequestPointWithPosition(0, RequestPoint(Point2(0, 0))),
                    RequestPointWithPosition(3, RequestPoint(Point2(0, 0)))
                }
            },
            &ROAD_GRAPH);
        result.alternativeIndex = index;
        return result;
    };

    auto userRouterResult = routerResult(3, 10, 10, 0);

    auto alternative = [&routerResult] (uint32_t edgeId, double time, double jamsTime, int index) {
        const NearbyInfo nearbyInfo {
            .onUserRouteForkSegmentIndex = 1,
            .onUserRouteJoinSegmentIndex = 2
        };
        return AlternativeInfo(routerResult(edgeId, time, jamsTime, index), nullptr, nearbyInfo);
    };

    TimeComparator comparator(
        userRouterResult,
        2,      // minJamsTimeDelta
        1);     // maxSlowdown

    std::vector<AlternativeInfo> fastAlternatives;
    fastAlternatives.emplace_back(alternative(4, 10, 10, 1));    // jams time is small
    fastAlternatives.emplace_back(alternative(5, 100, 7, 2));    // jams time is small

    std::vector<AlternativeInfo> slowAlternatives;
    slowAlternatives.emplace_back(alternative(6, 100, 8, 3));    // jams time is small, but not enough
    slowAlternatives.emplace_back(alternative(7, 21, 100, 4));   // time is small, but not enough

    auto expectValue = [&comparator] (
        const std::vector<AlternativeInfo>& lhs,
        const std::vector<AlternativeInfo>& rhs,
        ComparingResult value)
    {
        for (const auto& leftInfo : lhs) {
            for (const auto& rightInfo : rhs) {
                UNIT_ASSERT_VALUES_EQUAL(
                    comparator.compare(leftInfo, rightInfo), value);
            }
        }
    };

    expectValue(fastAlternatives, fastAlternatives, ComparingResult::Equal);
    expectValue(fastAlternatives, slowAlternatives, ComparingResult::FirstBetter);
    expectValue(slowAlternatives, fastAlternatives, ComparingResult::SecondBetter);

    auto expectAcceptableValue = [&comparator] (
            const std::vector<AlternativeInfo>& alternatives, bool value) {
        for (const auto& alternative: alternatives) {
            UNIT_ASSERT_VALUES_EQUAL(comparator.isAcceptable(alternative), value);
        }
    };

    expectAcceptableValue(slowAlternatives, false /* is not acceptable */);
    expectAcceptableValue(fastAlternatives, true /* is acceptable */);
}

Y_UNIT_TEST(sharing_comparator_test)
{
    using maps::routing::alternatives_selector::SharingWithRouteComparator;

    Route userRoute = routeWithSegments(
        &PathSegment::length, 1, MIN_DISTANCE_TO_JOIN - 1, 10000);

    SharingWithRouteComparator comparator(userRoute);

    auto alternative = [] (size_t forkIndex, size_t joinIndex) {
        const NearbyInfo nearbyInfo {
            .forkSegmentIndex = forkIndex,
            .joinSegmentIndex = joinIndex
        };
        return AlternativeInfo(
            {
                VehicleParameters(),
                Avoid(),
                routeWithSegments(&PathSegment::length, 1, 1, 1),
                &ROAD_GRAPH
            },
            nullptr,
            nearbyInfo);
    };

    UNIT_ASSERT_VALUES_EQUAL(
        comparator.compare(alternative(0, 1), alternative(1, 2)), ComparingResult::Equal);
    UNIT_ASSERT_VALUES_EQUAL(
        comparator.compare(alternative(1, 2), alternative(0, 2)), ComparingResult::SecondBetter);
    UNIT_ASSERT_VALUES_EQUAL(
        comparator.compare(alternative(0, 2), alternative(2, 3)), ComparingResult::Equal);

}

Y_UNIT_TEST(fork_distance_comparator_test)
{
    using maps::routing::alternatives_selector::ForkDistanceComparator;

    const double maxDistanceToFork = 100;

    Route userRoute = routeWithSegments(
        &PathSegment::length, 1, maxDistanceToFork - 1, 1, 10000);

    auto alternative = [] (size_t forkIndex) {
        const NearbyInfo nearbyInfo {
            .forkSegmentIndex = forkIndex,
            .joinSegmentIndex = 4
        };
        return AlternativeInfo(
            {
                VehicleParameters(),
                Avoid(),
                routeWithSegments(&PathSegment::length, 1, 1, 1, 1),
                &ROAD_GRAPH
            },
            nullptr,
            nearbyInfo);
    };

    ForkDistanceComparator comparator(userRoute, maxDistanceToFork);

    UNIT_ASSERT_VALUES_EQUAL(
        comparator.compare(alternative(1), alternative(2)), ComparingResult::Equal);
    UNIT_ASSERT_VALUES_EQUAL(
        comparator.compare(alternative(2), alternative(3)), ComparingResult::FirstBetter);
    UNIT_ASSERT_VALUES_EQUAL(
        comparator.isAcceptable(alternative(3)), false);
    UNIT_ASSERT_VALUES_EQUAL(
        comparator.isAcceptable(alternative(4)), false);
}

}

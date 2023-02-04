#include "../../alternatives_selector/comparators.h"
#include "utils.h"

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

using maps::routing::alternatives_selector::BlockedRouteComparator;
using maps::routing::alternatives_selector::ComparingResult;

Y_UNIT_TEST_SUITE(blocked_comparators_tests) {

Y_UNIT_TEST(select_blocked_alternative) {
    std::optional<double> blockedMaxJamsTime = 1000;
    BlockedRouteComparator comparator(blockedMaxJamsTime);

    auto blockedRoute = AlternativeInfo(makeRoute());
    blockedRoute.result.traits.set_blocked(true);
    blockedRoute.result.jamsTime = 1001.0;
    blockedRoute.result.jamSegments = {
        createJamSegment(JamType::Closed),
        createJamSegment(JamType::Free),
        createJamSegment(JamType::Free)
    };

    auto nonBlockedRoute = AlternativeInfo(makeRoute());
    nonBlockedRoute.result.traits.set_blocked(false);
    nonBlockedRoute.result.jamsTime = 2000.0;
    nonBlockedRoute.result.jamSegments = {
        createJamSegment(JamType::Free),
        createJamSegment(JamType::Free),
        createJamSegment(JamType::Free)
    };

    auto blockedFastRoute = AlternativeInfo(makeRoute());
    blockedFastRoute.result.traits.set_blocked(true);
    blockedFastRoute.result.jamsTime = 999.0;
    blockedFastRoute.result.jamSegments = {
        createJamSegment(JamType::Free),
        createJamSegment(JamType::Closed),
        createJamSegment(JamType::Free)
    };

    UNIT_ASSERT_EQUAL(
        comparator.compare(blockedFastRoute, blockedRoute),
        ComparingResult::FirstBetter);

    UNIT_ASSERT_EQUAL(
        comparator.compare(nonBlockedRoute, blockedRoute),
        ComparingResult::FirstBetter);

    UNIT_ASSERT_EQUAL(
        comparator.compare(blockedFastRoute, nonBlockedRoute),
        ComparingResult::Equal);
}

Y_UNIT_TEST(drop_alternatives_with_more_blocked_sequences) {
    std::optional<double> blockedMaxJamsTime;
    BlockedRouteComparator comparator(blockedMaxJamsTime);

    auto routeWithShorterBlockedSequence = AlternativeInfo(makeRoute());
    routeWithShorterBlockedSequence.result.traits.set_blocked(true);
    routeWithShorterBlockedSequence.result.jamSegments = {
        createJamSegment(JamType::Free),
        createJamSegment(JamType::Closed),
        createJamSegment(JamType::Free)
    };

    auto routeWithLongerBlockedSequence = AlternativeInfo(makeRoute());
    routeWithLongerBlockedSequence.result.traits.set_blocked(true);
    routeWithLongerBlockedSequence.result.jamSegments = {
        createJamSegment(JamType::Free),
        createJamSegment(JamType::Closed),
        createJamSegment(JamType::Closed),
        createJamSegment(JamType::Free)
    };

    auto routeWithTwoBlockedSequences = AlternativeInfo(makeRoute());
    routeWithTwoBlockedSequences.result.traits.set_blocked(true);
    routeWithTwoBlockedSequences.result.jamSegments = {
        createJamSegment(JamType::Free),
        createJamSegment(JamType::Closed),
        createJamSegment(JamType::Free),
        createJamSegment(JamType::Closed),
        createJamSegment(JamType::Free)
    };

    auto nonBlockedRoute = AlternativeInfo(makeRoute());
    nonBlockedRoute.result.traits.set_blocked(false);
    nonBlockedRoute.result.jamSegments = {
        createJamSegment(JamType::Free),
        createJamSegment(JamType::Free),
        createJamSegment(JamType::Free)
    };

    UNIT_ASSERT_EQUAL(
        comparator.compare(nonBlockedRoute, routeWithShorterBlockedSequence),
        ComparingResult::FirstBetter);

    UNIT_ASSERT_EQUAL(
        comparator.compare(
            routeWithShorterBlockedSequence,
            routeWithTwoBlockedSequences),
        ComparingResult::FirstBetter);

    UNIT_ASSERT_EQUAL(
        comparator.compare(
            routeWithShorterBlockedSequence,
            routeWithShorterBlockedSequence),
        ComparingResult::Equal);

    UNIT_ASSERT_EQUAL(
        comparator.compare(
            routeWithShorterBlockedSequence,
            routeWithLongerBlockedSequence),
        ComparingResult::Equal);

    UNIT_ASSERT_EQUAL(
        comparator.compare(nonBlockedRoute, nonBlockedRoute),
        ComparingResult::Equal);
}

}

#include "../../alternatives_selector/nearby_comparators.h"
#include "../../alternatives_selector/selector.h"
#include "utils.h"

#include <library/cpp/testing/unittest/registar.h>

using maps::routing::alternatives_selector::ComparingResult;
using maps::routing::alternatives_selector::compare;
using maps::routing::alternatives_selector::findBestAlternative;
using maps::routing::alternatives_selector::isAcceptable;
using maps::routing::alternatives_selector::SourceScopeComparator;
using maps::routing::alternatives_selector::TimeRouteComparator;
using maps::routing::alternatives_selector::TraitsNestingComparator;
using maps::routing::alternatives_selector::FasterThanUserRouteComparator;

Y_UNIT_TEST_SUITE(alternative_selector_tests) {

Y_UNIT_TEST(findBestAlternative) {
    std::vector<AlternativeInfo> alternatives;

    auto checkBest = [&](std::vector<AlternativeInfo>::const_iterator best) {
        std::tuple<SourceScopeComparator, TimeRouteComparator> comparators = std::make_tuple(
            SourceScopeComparator(),
            TimeRouteComparator(maps::routing::SortByTime::Time)
        );
        UNIT_ASSERT_EQUAL(
            findBestAlternative(
                alternatives.begin(),
                alternatives.end(),
                comparators),
            best);
    };

    alternatives.emplace_back(makeRoute());
    alternatives.emplace_back(makeRoute());

    alternatives[0].result.sourceScope = 1;
    alternatives[0].result.time = 1.0;

    alternatives[1].result.sourceScope = 1;
    alternatives[1].result.time = 2.0;

    checkBest(alternatives.begin());

    alternatives[0].result.sourceScope = 1;
    alternatives[0].result.time = 2.0;

    alternatives[1].result.sourceScope = 1;
    alternatives[1].result.time = 1.0;

    checkBest(alternatives.begin() + 1);

    alternatives[1].result.sourceScope = 2;

    checkBest(alternatives.begin());
}

Y_UNIT_TEST(isBestAlternative) {
    std::vector<AlternativeInfo> alternatives;
    auto comparators = std::make_tuple(
        SourceScopeComparator(),
        TimeRouteComparator(maps::routing::SortByTime::Time)
    );

    AlternativeInfo alternative(makeRoute());

    auto checkBest = [&](bool isBest) {
        UNIT_ASSERT_VALUES_EQUAL(
            compare(
                comparators,
                alternative,
                alternatives.begin(),
                alternatives.end()) == ComparingResult::FirstBetter,
            isBest);
    };

    alternatives.emplace_back(makeRoute());
    alternatives.emplace_back(makeRoute());

    alternative.result.sourceScope = 1;
    alternative.result.time = 1.0;

    alternatives[0].result.sourceScope = 1;
    alternatives[0].result.time = 2.0;

    alternatives[1].result.sourceScope = 1;
    alternatives[1].result.time = 3.0;

    checkBest(true);

    alternatives[1].result.sourceScope = 0;

    checkBest(false);
}

Y_UNIT_TEST(isAcceptable) {
    AlternativeInfo alternative(makeRoute());

    RouteTraits routeTraits = alternative.result.traits;
    routeTraits.set_has_boat_ferries(true);

    auto comparators = std::make_tuple(
        TraitsNestingComparator(routeTraits)
    );

    auto checkIsAcceptable = [&](
            const AlternativeInfo& alternative,
            bool isBest) {
        UNIT_ASSERT_VALUES_EQUAL(
            isAcceptable(
                alternative,
                comparators),
            isBest);
    };

    checkIsAcceptable(alternative, true);

    alternative.result.traits.set_has_boat_ferries(true);
    checkIsAcceptable(alternative, true);

    alternative.result.traits.set_has_boat_ferries(false);
    alternative.result.traits.set_crosses_borders(true);
    checkIsAcceptable(alternative, false);

    alternative.result.traits.set_has_boat_ferries(true);
    alternative.result.traits.set_crosses_borders(true);
    checkIsAcceptable(alternative, false);
}

}

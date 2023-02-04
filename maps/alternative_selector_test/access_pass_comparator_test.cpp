#include "utils.h"

#include <maps/routing/router/yacare/alternatives_selector/comparators.h>
#include <maps/routing/router/yacare/alternatives_selector/selector.h>

#include <maps/routing/router/yacare/router_result.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

using maps::routing::alternatives_selector::ComparingResult;
using maps::routing::alternatives_selector::AccessPassComparator;

Y_UNIT_TEST_SUITE(accesspass_comparator_test) {

Y_UNIT_TEST(sort_by_access_pass) {
    AccessPassComparator comparator;

    auto info1 = AlternativeInfo(makeRoute());
    info1.result.accessPassCount = 1;

    auto info2 = AlternativeInfo(makeRoute());
    info2.result.accessPassCount = 2;

    UNIT_ASSERT_EQUAL(
        comparator.compare(info1, info2),
        ComparingResult::FirstBetter);

    UNIT_ASSERT_EQUAL(
        comparator.compare(info2, info1),
        ComparingResult::SecondBetter);

    info2.result.accessPassCount = 1;

    UNIT_ASSERT_EQUAL(
        comparator.compare(info1, info2),
        ComparingResult::Equal);
}

Y_UNIT_TEST(select_route_with_minimal_access_pass_count) {
    auto info1 = makeRoute();
    info1.length = 1000.0;
    info1.jamsTime = 1000.0;
    info1.lengthInPoorCondition = 0.0;
    info1.accessPassCount = 2;
    info1.alternativeIndex = 0;

    auto info2 = makeRoute();
    info2.length = 2000.0;
    info2.jamsTime = 2000.0;
    info2.lengthInPoorCondition = 10.0;
    info2.accessPassCount = 1;
    info2.alternativeIndex = 1;

    std::vector<RouterResult> alternatives;
    alternatives.emplace_back(std::move(info1));
    alternatives.emplace_back(std::move(info2));

    auto result = maps::routing::alternatives_selector::selectBestAlternatives(
        std::move(alternatives),
        std::nullopt,
        maps::routing::SortByTime::JamsTime,
        Avoid(),
        true,
        0.7 /* maxRelativeSharing */,
        3 /* numAlternatives */,
        VehicleType::CAR,
        maps::routing::Config(),
        &ROAD_GRAPH);

    UNIT_ASSERT_VALUES_EQUAL(result.size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(result[0].alternativeIndex, 1);
}

Y_UNIT_TEST(select_route_with_open_pass)
{
    AccessPassComparator comparator;

    auto info1 = AlternativeInfo(makeRoute());
    info1.result.accessPassCount = 1;

    auto info2 = AlternativeInfo(makeRoute());
    info2.result.accessPassCount = 1;
    info2.result.openAccessPassCount = 1;

    UNIT_ASSERT_EQUAL(
        comparator.compare(info1, info2),
        ComparingResult::SecondBetter);

    info1.result.accessPassCount = 0;
    UNIT_ASSERT_EQUAL(
        comparator.compare(info1, info2),
        ComparingResult::Equal);

    info2.result.accessPassCount = 2;
    info2.result.openAccessPassCount = 2;

    UNIT_ASSERT_EQUAL(
        comparator.compare(info1, info2),
        ComparingResult::Equal);

    info1.result.accessPassCount = 1;

    UNIT_ASSERT_EQUAL(
        comparator.compare(info1, info2),
        ComparingResult::SecondBetter);
}

}

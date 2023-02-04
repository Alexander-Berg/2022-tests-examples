#include "../../alternatives_selector/comparators.h"
#include "../../alternatives_selector/selector.h"
#include "utils.h"

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

using maps::routing::alternatives_selector::ComparingResult;
using maps::routing::alternatives_selector::RoadQualityComparator;

Y_UNIT_TEST_SUITE(road_quality_comparator_tests) {

Y_UNIT_TEST(second_route_has_no_ford_crossings) {
    RoadQualityComparator comparator(&ROAD_GRAPH);

    auto info1 = AlternativeInfo(makeRoute());
    info1.result.fordCrossingCount = 1;
    info1.result.traits.set_has_ford_crossings(true);
    info1.result.length = 1000.0;
    info1.result.jamsTime = 1000.0;
    info1.result.lengthInPoorCondition = 20.0;
    info1.result.lengthBothUnpavedAndInPoorCondition = 20.0;

    auto info2 = AlternativeInfo(makeRoute());
    info2.result.fordCrossingCount = 0;
    info2.result.traits.set_has_ford_crossings(false);
    info2.result.length = 2000.0;
    info2.result.jamsTime = 2000.0;
    info2.result.lengthInPoorCondition = 40.0;
    info2.result.lengthBothUnpavedAndInPoorCondition = 40.0;

    UNIT_ASSERT_EQUAL(
        comparator.compare(info1, info2),
        ComparingResult::SecondBetter);
}

Y_UNIT_TEST(second_route_has_less_ford_crossings_than_first) {
    RoadQualityComparator comparator(&ROAD_GRAPH);

    auto info1 = AlternativeInfo(makeRoute());
    info1.result.fordCrossingCount = 3;
    info1.result.traits.set_has_ford_crossings(true);
    info1.result.length = 1000.0;
    info1.result.jamsTime = 1000.0;
    info1.result.lengthInPoorCondition = 20.0;
    info1.result.lengthBothUnpavedAndInPoorCondition = 20.0;

    auto info2 = AlternativeInfo(makeRoute());
    info2.result.fordCrossingCount = 2;
    info2.result.traits.set_has_ford_crossings(true);
    info2.result.length = 2000.0;
    info2.result.jamsTime = 2000.0;
    info2.result.lengthInPoorCondition = 40.0;
    info2.result.lengthBothUnpavedAndInPoorCondition = 40.0;

    UNIT_ASSERT_EQUAL(
        comparator.compare(info1, info2),
        ComparingResult::SecondBetter);
}

Y_UNIT_TEST(first_route_has_no_unpaved_or_in_poor_condition) {
    RoadQualityComparator comparator(&ROAD_GRAPH);

    auto info1 = AlternativeInfo(makeRoute());
    info1.result.length = 1000.0;
    info1.result.jamsTime = 1000.0;
    info1.result.lengthInPoorCondition = 20.0;
    info1.result.lengthBothUnpavedAndInPoorCondition = 20.0;

    auto info2 = AlternativeInfo(makeRoute());
    info2.result.length = 2000.0;
    info2.result.jamsTime = 2000.0;
    info2.result.lengthInPoorCondition = 0.0;
    info2.result.lengthBothUnpavedAndInPoorCondition = 0.0;

    UNIT_ASSERT_EQUAL(
        comparator.compare(info1, info2),
        ComparingResult::SecondBetter);
}

Y_UNIT_TEST(select_route_has_unpaved_or_in_poor_condition_last) {
    auto info1 = makeRoute();
    info1.length = 1000.0;
    info1.jamsTime = 1000.0;
    info1.lengthInPoorCondition = 40.0;
    info1.lengthBothUnpavedAndInPoorCondition = 40.0;
    info1.alternativeIndex = 0;

    auto info2 = makeRoute();
    info2.length = 2000.0;
    info2.jamsTime = 2000.0;
    info2.lengthInPoorCondition = 0.0;
    info2.lengthBothUnpavedAndInPoorCondition = 0.0;
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

    UNIT_ASSERT_VALUES_EQUAL(result.size(), 2);
    UNIT_ASSERT_VALUES_EQUAL(result[0].alternativeIndex, 1);
    UNIT_ASSERT_VALUES_EQUAL(result[1].alternativeIndex, 0);
}

}

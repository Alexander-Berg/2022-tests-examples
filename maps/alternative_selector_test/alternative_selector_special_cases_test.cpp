#include "../../alternatives_selector/comparators.h"
#include "../../alternatives_selector/selector.h"
#include "utils.h"

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

using maps::geolib3::Point2;
using maps::routing::PathSegment;

using maps::routing::alternatives_selector::findBestAlternative;
using maps::routing::alternatives_selector::selectAlternative;
using maps::routing::alternatives_selector::selectBestAlternatives;
using maps::routing::alternatives_selector::TollsComparator;
using maps::routing::alternatives_selector::RouteRankComparator;
using maps::routing::alternatives_selector::SourceScopeComparator;
using maps::routing::alternatives_selector::TimeRouteComparator;

Y_UNIT_TEST_SUITE(alternative_selector_special_cases_tests) {

Y_UNIT_TEST(select_alternative_test) {
    std::vector<AlternativeInfo> alternatives;
    std::vector<AlternativeInfo> selected;

    UNIT_ASSERT(
        !selectAlternative(
            alternatives,
            selected,
            "",
            std::make_tuple(
                SourceScopeComparator(),
                TimeRouteComparator(maps::routing::SortByTime::Time)
            )
        )
    );

    alternatives.emplace_back(makeRoute());

    alternatives[0].result.sourceScope = 1;
    alternatives[0].result.time = 1.0;

    UNIT_ASSERT(
        selectAlternative(
            alternatives,
            selected,
            "A",
            std::make_tuple(
                SourceScopeComparator(),
                TimeRouteComparator(maps::routing::SortByTime::Time)
            )
        )
    );
    UNIT_ASSERT_VALUES_EQUAL(alternatives.size(), 0);
    UNIT_ASSERT_VALUES_EQUAL(selected.size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(selected[0].result.reason, "A");

    alternatives.emplace_back(makeRoute());

    alternatives[0].result.sourceScope = 1;
    alternatives[0].result.time = 0.5;

    UNIT_ASSERT(
        selectAlternative(
            alternatives,
            selected,
            "B",
            std::make_tuple(
                SourceScopeComparator(),
                TimeRouteComparator(maps::routing::SortByTime::Time)
            )
        )
    );
    UNIT_ASSERT_VALUES_EQUAL(alternatives.size(), 0);
    UNIT_ASSERT_VALUES_EQUAL(selected.size(), 2);
    UNIT_ASSERT_VALUES_EQUAL(selected[1].result.reason, "B");

    alternatives.emplace_back(makeRoute());

    alternatives[0].result.sourceScope = 2;
    alternatives[0].result.time = 1.0;

    UNIT_ASSERT(
        !selectAlternative(
            alternatives,
            selected,
            "C",
            std::make_tuple(
                SourceScopeComparator(),
                TimeRouteComparator(maps::routing::SortByTime::Time)
            )
        )
    );
    UNIT_ASSERT_VALUES_EQUAL(alternatives.size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(selected.size(), 2);
}

Y_UNIT_TEST(toll_free_alternative_test) {
    std::vector<RouterResult> alternatives;

    alternatives.emplace_back(makeRoute());
    alternatives.emplace_back(makeRoute());
    alternatives.emplace_back(makeRoute());

    alternatives[0].traits.set_has_tolls(true);
    alternatives[0].jamsTime = 1.0;
    alternatives[0].alternativeIndex = 0;

    alternatives[1].traits.set_has_tolls(false);
    alternatives[1].jamsTime = 3.0;
    alternatives[1].alternativeIndex = 1;

    alternatives[2].traits.set_has_tolls(true);
    alternatives[2].jamsTime = 2.0;
    alternatives[2].alternativeIndex = 2;

    auto result = selectBestAlternatives(
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
    UNIT_ASSERT_VALUES_EQUAL(result[0].alternativeIndex, 0);
    UNIT_ASSERT_VALUES_EQUAL(result[1].alternativeIndex, 1);
}

Y_UNIT_TEST(use_jams_alternative_test) {
    auto makeAlternatives = []() {
        std::vector<RouterResult> alternatives;

        alternatives.emplace_back(makeRoute());
        alternatives.emplace_back(makeRoute());
        alternatives.emplace_back(makeRoute());

        alternatives[0].time = 10.0;
        alternatives[0].jamsTime = 1.0;
        alternatives[0].alternativeIndex = 0;

        alternatives[1].time = 1.0;
        alternatives[1].jamsTime = 10.0;
        alternatives[1].alternativeIndex = 1;

        alternatives[2].time = 30.0;
        alternatives[2].jamsTime = 30.0;
        alternatives[2].alternativeIndex = 2;

        return alternatives;
    };

    auto resultWithJams = selectBestAlternatives(
        makeAlternatives(),
        std::nullopt,
        maps::routing::SortByTime::JamsTime,
        Avoid(),
        true,
        0.7 /* maxRelativeSharing */,
        3 /* numAlternatives */,
        VehicleType::CAR,
        maps::routing::Config(),
        &ROAD_GRAPH);

    UNIT_ASSERT_VALUES_EQUAL(resultWithJams.size(), 2);
    UNIT_ASSERT_VALUES_EQUAL(resultWithJams[0].alternativeIndex, 0);
    UNIT_ASSERT_VALUES_EQUAL(resultWithJams[1].alternativeIndex, 1);

    auto resultWithNoJams = selectBestAlternatives(
        makeAlternatives(),
        std::nullopt,
        maps::routing::SortByTime::Time,
        Avoid(),
        true,
        0.7 /* maxRelativeSharing */,
        3 /* numAlternatives */,
        VehicleType::CAR,
        maps::routing::Config(),
        &ROAD_GRAPH);

    UNIT_ASSERT_VALUES_EQUAL(resultWithNoJams.size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(resultWithNoJams[0].alternativeIndex, 1);
}

Y_UNIT_TEST(select_blocked_if_it_is_faster_enough) {
    std::vector<RouterResult> alternatives;

    alternatives.emplace_back(makeRoute());
    alternatives.emplace_back(makeRoute());

    alternatives[0].traits.set_blocked(true);
    alternatives[0].jamsTime = 1300.0;
    alternatives[0].alternativeIndex = 1;
    alternatives[0].jamSegments = {
        createJamSegment(JamType::Closed),
        createJamSegment(JamType::Closed),
        createJamSegment(JamType::Closed)
    };

    alternatives[1].traits.set_blocked(false);
    alternatives[1].jamsTime = 3000.0;
    alternatives[1].alternativeIndex = 0;
    alternatives[1].jamSegments = {
        createJamSegment(JamType::Free),
        createJamSegment(JamType::Free),
        createJamSegment(JamType::Free),
    };

    auto result = selectBestAlternatives(
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

Y_UNIT_TEST(dont_select_blocked_if_not_faster_enough) {
    std::vector<RouterResult> alternatives;

    alternatives.emplace_back(makeRoute());
    alternatives.emplace_back(makeRoute());

    alternatives[0].traits.set_blocked(true);
    alternatives[0].jamsTime = 2.8;
    alternatives[0].alternativeIndex = 0;
    alternatives[0].jamSegments = {
        createJamSegment(JamType::Free),
        createJamSegment(JamType::Closed),
        createJamSegment(JamType::Free)
    };

    alternatives[1].traits.set_blocked(false);
    alternatives[1].jamsTime = 3.0;
    alternatives[1].alternativeIndex = 1;
    alternatives[1].jamSegments = {
        createJamSegment(JamType::Free),
        createJamSegment(JamType::Free),
        createJamSegment(JamType::Free)
    };

    auto result = selectBestAlternatives(
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

Y_UNIT_TEST(select_crossing_border_route_test) {
    std::vector<RouterResult> alternatives;

    alternatives.emplace_back(makeRoute());
    alternatives.emplace_back(makeRoute());
    alternatives.emplace_back(makeRoute());

    alternatives[0].length = 2.0;
    alternatives[0].alternativeIndex = 0;
    alternatives[0].jamsTime = 1;
    alternatives[0].traits.set_crosses_borders(false);
    alternatives[0].time = 3;

    alternatives[1].length = 5.5;
    alternatives[1].alternativeIndex = 1;
    alternatives[1].jamsTime = 2;
    alternatives[1].traits.set_crosses_borders(true);
    alternatives[1].time = 2;

    alternatives[2].length = 6.5;
    alternatives[2].alternativeIndex = 2;
    alternatives[2].jamsTime = 3;
    alternatives[2].traits.set_crosses_borders(true);
    alternatives[2].time = 1;

    auto result = selectBestAlternatives(
        std::move(alternatives),
        std::nullopt,
        maps::routing::SortByTime::JamsTime,
        Avoid {AvoidTolls::YES},
        true,
        0.7 /* maxRelativeSharing */,
        3 /* numAlternatives */,
        VehicleType::CAR,
        maps::routing::Config(),
        &ROAD_GRAPH);

    UNIT_ASSERT_VALUES_EQUAL(result.size(), 2);
    UNIT_ASSERT_VALUES_EQUAL(result[0].alternativeIndex, 0);
    UNIT_ASSERT_VALUES_EQUAL(result[1].alternativeIndex, 1);
}

Y_UNIT_TEST(drop_crossing_border_route_test) {
    std::vector<RouterResult> alternatives;

    alternatives.emplace_back(makeRoute());
    alternatives.emplace_back(makeRoute());
    alternatives.emplace_back(makeRoute());

    alternatives[0].length = 2.0;
    alternatives[0].alternativeIndex = 0;
    alternatives[0].jamsTime = 1;
    alternatives[0].traits.set_crosses_borders(false);
    alternatives[0].traits.set_has_tolls(true);

    alternatives[1].length = 6.2;
    alternatives[1].alternativeIndex = 1;
    alternatives[1].jamsTime = 2;
    alternatives[1].traits.set_crosses_borders(true);
    alternatives[1].traits.set_has_tolls(false);

    alternatives[2].length = 6.5;
    alternatives[2].alternativeIndex = 2;
    alternatives[2].jamsTime = 3;
    alternatives[2].traits.set_crosses_borders(true);
    alternatives[2].traits.set_has_tolls(false);

    auto result = selectBestAlternatives(
        std::move(alternatives),
        std::nullopt,
        maps::routing::SortByTime::Time,
        Avoid {AvoidTolls::YES},
        true,
        0.7 /* maxRelativeSharing */,
        3 /* numAlternatives */,
        VehicleType::CAR,
        maps::routing::Config(),
        &ROAD_GRAPH);

    UNIT_ASSERT_VALUES_EQUAL(result.size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(result[0].alternativeIndex, 0);
}

Y_UNIT_TEST(drop_long_routes_test) {
    std::vector<RouterResult> alternatives;

    alternatives.emplace_back(makeRoute());
    alternatives.emplace_back(makeRoute());
    alternatives.emplace_back(makeRoute());

    alternatives[0].length = 1.0;
    alternatives[0].alternativeIndex = 0;

    alternatives[1].length = 3.1;
    alternatives[1].alternativeIndex = 1;

    alternatives[2].length = 4.0;
    alternatives[2].alternativeIndex = 2;

    auto result = selectBestAlternatives(
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
    UNIT_ASSERT_VALUES_EQUAL(result[0].alternativeIndex, 0);
}

}

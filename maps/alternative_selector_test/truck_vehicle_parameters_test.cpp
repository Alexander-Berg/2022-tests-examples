#include "../../alternatives_selector/comparators.h"
#include "../../alternatives_selector/selector.h"
#include "utils.h"

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

using maps::routing::alternatives_selector::ComparingResult;
using maps::routing::alternatives_selector::VehicleRestrictionsComparator;

Y_UNIT_TEST_SUITE(truck_vehicle_parameters_tests) {

Y_UNIT_TEST(crossing_strict_vehicle_restriction) {
    VehicleRestrictionsComparator comparator;

    auto info1 = AlternativeInfo(makeRoute());
    info1.result.routeVehicleRestrictions.hasStrictVehicleRestrictions = true;
    info1.result.routeVehicleRestrictions.crossingNonStrictVehicleRestrictionCount = 1;

    auto info2 = AlternativeInfo(makeRoute());
    info2.result.routeVehicleRestrictions.hasStrictVehicleRestrictions = false;
    info2.result.routeVehicleRestrictions.crossingNonStrictVehicleRestrictionCount = 5;

    UNIT_ASSERT_EQUAL(
        comparator.compare(info1, info2),
        ComparingResult::SecondBetter);
}

Y_UNIT_TEST(crossing_non_strict_vehicle_restriction) {
    VehicleRestrictionsComparator comparator;

    auto info1 = AlternativeInfo(makeRoute());
    info1.result.routeVehicleRestrictions.hasStrictVehicleRestrictions = true;
    info1.result.routeVehicleRestrictions.crossingNonStrictVehicleRestrictionCount = 5;

    auto info2 = AlternativeInfo(makeRoute());
    info2.result.routeVehicleRestrictions.hasStrictVehicleRestrictions = true;
    info2.result.routeVehicleRestrictions.crossingNonStrictVehicleRestrictionCount = 3;

    auto info3 = AlternativeInfo(makeRoute());
    info3.result.routeVehicleRestrictions.hasStrictVehicleRestrictions = false;
    info3.result.routeVehicleRestrictions.crossingNonStrictVehicleRestrictionCount = 10;

    UNIT_ASSERT_EQUAL(
        comparator.compare(info1, info2),
        ComparingResult::SecondBetter);

    UNIT_ASSERT_EQUAL(
        comparator.compare(info2, info3),
        ComparingResult::SecondBetter);
}

}

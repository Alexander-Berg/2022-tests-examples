#include "../../alternatives_selector/comparators.h"
#include "utils.h"

#include <library/cpp/testing/unittest/registar.h>

using maps::routing::alternatives_selector::ComparingResult;
using maps::routing::alternatives_selector::OptionalComparator;
using maps::routing::alternatives_selector::VehicleRestrictionsComparator;

Y_UNIT_TEST_SUITE(optional_comparator_test) {

Y_UNIT_TEST(is_acceptable_turned_off) {
    OptionalComparator<VehicleRestrictionsComparator>
        comparatorTurnedOff(false);

    auto info1 = AlternativeInfo(makeRoute());
    info1.result.routeVehicleRestrictions.hasStrictVehicleRestrictions = true;
    info1.result.routeVehicleRestrictions.crossingNonStrictVehicleRestrictionCount = 1;

    UNIT_ASSERT_EQUAL(comparatorTurnedOff.isAcceptable(info1), true);
}

Y_UNIT_TEST(is_acceptable_turned_on) {
    OptionalComparator<VehicleRestrictionsComparator>
        comparatorTurnedOn(true);

    auto info1 = AlternativeInfo(makeRoute());
    info1.result.routeVehicleRestrictions.hasStrictVehicleRestrictions = false;
    info1.result.routeVehicleRestrictions.crossingNonStrictVehicleRestrictionCount = 1;

    UNIT_ASSERT_EQUAL(comparatorTurnedOn.isAcceptable(info1), true);

    auto info2 = AlternativeInfo(makeRoute());
    info2.result.routeVehicleRestrictions.hasStrictVehicleRestrictions = true;
    info2.result.routeVehicleRestrictions.crossingNonStrictVehicleRestrictionCount = 1;

    UNIT_ASSERT_EQUAL(comparatorTurnedOn.isAcceptable(info2), false);
}

}

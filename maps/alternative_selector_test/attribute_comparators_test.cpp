#include "../../alternatives_selector/comparators.h"
#include "utils.h"

#include <library/cpp/testing/unittest/registar.h>

using maps::routing::alternatives_selector::ComparingResult;
using maps::routing::alternatives_selector::OptionalComparator;
using maps::routing::alternatives_selector::TollsComparator;

Y_UNIT_TEST_SUITE(attribute_comparators_tests) {

Y_UNIT_TEST(tolls_comparator) {
    auto comparator = OptionalComparator<TollsComparator>(true);

    auto tollRoute = AlternativeInfo(makeRoute());
    tollRoute.result.traits.set_has_tolls(true);

    auto tollFreeRoute = AlternativeInfo(makeRoute());
    tollFreeRoute.result.traits.set_has_tolls(false);

    UNIT_ASSERT_EQUAL(
        comparator.compare(tollRoute, tollFreeRoute),
        ComparingResult::SecondBetter);

    UNIT_ASSERT_EQUAL(
        comparator.compare(tollFreeRoute, tollRoute),
        ComparingResult::FirstBetter);

    comparator = OptionalComparator<TollsComparator>(false);

    UNIT_ASSERT_EQUAL(
        comparator.compare(tollFreeRoute, tollRoute),
        ComparingResult::Equal);

    UNIT_ASSERT_EQUAL(
        comparator.compare(tollRoute, tollFreeRoute),
        ComparingResult::Equal);
}

}

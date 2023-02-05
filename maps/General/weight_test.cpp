#include "library/cpp/testing/unittest/registar.h"

#include "weight.h"
#include "test_util.h"

#include <numeric>

Y_UNIT_TEST_SUITE(WeightTestSuite) {

Y_UNIT_TEST(Weight) {
    for (int weight = 0; weight < 10000000; weight += 10) {
        const int mangledWeight = decodeWeight(encodeWeight(weight));
        UNIT_ASSERT(weightsAreClose(weight, mangledWeight));
    }
}

Y_UNIT_TEST(InvalidWeight) {
    const int invalidWeight = std::numeric_limits<int32_t>::max();
    UNIT_ASSERT_EQUAL(decodeWeight(encodeWeight(invalidWeight)), invalidWeight);
}

}

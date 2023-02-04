#include <library/cpp/testing/unittest/registar.h>
#include <maps/analyzer/libs/algo/include/encoding.h>

namespace maps {
namespace analyzer {
namespace algo {

Y_UNIT_TEST_SUITE(TestCurcularValueEncoding)
{
    Y_UNIT_TEST(TestCall)
    {
        const auto result1 = encodeCircularValue(0, 1);
        CircularValueEncoding etalon1 = {1, 0};
        UNIT_ASSERT_DOUBLES_EQUAL(etalon1.cos, result1.cos, 1e-6);
        UNIT_ASSERT_DOUBLES_EQUAL(etalon1.sin, result1.sin, 1e-6);

        const auto result2 = encodeCircularValue(1, 2);
        CircularValueEncoding etalon2 = {-1, 0};
        UNIT_ASSERT_DOUBLES_EQUAL(etalon2.cos, result2.cos, 1e-6);
        UNIT_ASSERT_DOUBLES_EQUAL(etalon2.sin, result2.sin, 1e-6);

        const auto result3 = encodeCircularValue(1, 4);
        CircularValueEncoding etalon3 = {0, 1};
        UNIT_ASSERT_DOUBLES_EQUAL(etalon3.cos, result3.cos, 1e-6);
        UNIT_ASSERT_DOUBLES_EQUAL(etalon3.sin, result3.sin, 1e-6);

        const auto result4 = encodeCircularValue(3, 4);
        CircularValueEncoding etalon4 = {0, -1};
        UNIT_ASSERT_DOUBLES_EQUAL(etalon4.cos, result4.cos, 1e-6);
        UNIT_ASSERT_DOUBLES_EQUAL(etalon4.sin, result4.sin, 1e-6);
    }
};

} // algo
} // analyzer
} // maps

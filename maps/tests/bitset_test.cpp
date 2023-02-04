#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <maps/analyzer/libs/constexpr_types/bitset.h>

using namespace maps::analyzer::constexpr_types;

Y_UNIT_TEST_SUITE(TestConstexprBitset) {
    Y_UNIT_TEST(TestInitialization) {
        Bitset<10> b;
        for (std::size_t i = 0; i < 10; ++i) {
            EXPECT_FALSE(b.test(i));
        }
    }

    Y_UNIT_TEST(TestSet) {
        Bitset<10> b;
        for (std::size_t i = 0; i < 10; ++i) {
            b.set<true>(i);
            EXPECT_TRUE(b.test(i));

            b.set<false>(i);
            EXPECT_FALSE(b.test(i));
        }
    }

    Y_UNIT_TEST(TestOutOfBounds) {
        Bitset<10> b;
        UNIT_ASSERT_EXCEPTION(b.test(10), std::range_error);
    }
}

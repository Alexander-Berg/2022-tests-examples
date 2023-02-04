#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <maps/analyzer/libs/constexpr_types/hash.h>

using namespace maps::analyzer::constexpr_types;

Y_UNIT_TEST_SUITE(TestConstexprHash) {
    constexpr Hash<std::string_view> h;

    Y_UNIT_TEST(TestEqual) {
        const std::string s = {'a', 'b', 'c'};
        EXPECT_EQ(h(s), h("abc"));
    }
}

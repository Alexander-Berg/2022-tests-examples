#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <maps/analyzer/libs/constexpr_types/unordered_map.h>

using namespace maps::analyzer::constexpr_types;

struct User {
    std::size_t age;
    std::string_view city;
};

Y_UNIT_TEST_SUITE(TestConstexprUnorderedMap) {
    constexpr UnorderedMap<std::string_view, User, 4> mapa(
        std::pair{"Ivan", User{20, "Moscow"}},
        std::pair{"Petr", User{24, "Moscow"}},
        std::pair{"Alex", User{18, "Minsk"}},
        std::pair{"Emma", User{28, "NYC"}}
    );

    Y_UNIT_TEST(ExistingValueTest) {
        EXPECT_EQ(mapa.at("Emma").city, "NYC");
        EXPECT_EQ(mapa.at("Ivan").age, 20);
    }

    Y_UNIT_TEST(AbsentValueTest) {
        UNIT_ASSERT_EXCEPTION(mapa.at("James"), std::range_error);
    }
}

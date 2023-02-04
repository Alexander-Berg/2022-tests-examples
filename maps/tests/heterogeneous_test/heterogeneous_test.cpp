#include <maps/analyzer/libs/utils/include/heterogeneous.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <tuple>
#include <string>
#include <sstream>


namespace ma = maps::analyzer;
using namespace std::string_literals;


Y_UNIT_TEST_SUITE(HeterogeneousTest) {
    Y_UNIT_TEST(ForTest) {
        std::tuple t{1, "hello", false};

        std::string expected = "1hello0";
        std::ostringstream ostr;
        ma::for_each(t, [&](auto&& v) {
            ostr << v;
        });

        EXPECT_EQ(ostr.str(), expected);
    }

    Y_UNIT_TEST(MapTest) {
        std::tuple t{1, "hello", false};

        std::tuple expected{"1"s, "hello"s, "0"s};

        auto result = ma::map_each(t, [&](auto&& v) {
            std::ostringstream ostr;
            ostr << v;
            return ostr.str();
        });

        EXPECT_EQ(result, expected);
    }

    Y_UNIT_TEST(ForIdxTest) {
        std::tuple t{1, "hello", false};
        std::tuple t2{"smth", 10, 'a'};

        std::string expected = "[0] 1 smth [1] hello 10 [2] 0 a ";

        std::ostringstream ostr;
        ma::for_each_idx(t, [&](auto&& v, auto&& idx) {
            auto&& v2 = std::get<std::remove_cvref_t<decltype(idx)>::value>(t2);
            ostr << "[" << idx << "] " << v << " " << v2 << " ";
        });

        EXPECT_EQ(ostr.str(), expected);
    }

    Y_UNIT_TEST(MapIdxTest) {
        std::tuple t{1, "hello", false};
        std::tuple t2{"smth", 10, 'a'};

        std::tuple expected{
            std::pair{1, "smth"},
            std::pair{"hello", 10},
            std::pair{false, 'a'}
        };

        auto result = ma::map_each_idx(t, [&](auto&& v, auto&& idx) {
            auto&& v2 = std::get<std::remove_cvref_t<decltype(idx)>::value>(t2);
            return std::pair{v, v2};
        });

        EXPECT_EQ(result, expected);
    }
}

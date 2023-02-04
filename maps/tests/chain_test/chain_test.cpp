#include <maps/analyzer/libs/utils/include/chain.h>
#include <maps/analyzer/libs/utils/include/range_view.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <vector>
#include <list>
#include <set>


namespace ma = maps::analyzer;


template <typename ...Its>
std::vector<int> fillFrom(const ma::Chain<Its...>& ch) {
    std::vector<int> result;
    for (auto v: ch) {
        result.push_back(v);
    }
    return result;
}

template <class ...Args>
void chainTest(const std::vector<int>& expected, Args&& ...args) {
    auto result = fillFrom(ma::chain(args...));
    EXPECT_EQ(result, expected);
    auto cresult = fillFrom(ma::cchain(args...));
    EXPECT_EQ(cresult, expected);
}


Y_UNIT_TEST_SUITE(ChainTest) {
    Y_UNIT_TEST(SimpleTest) {
        std::vector<int> x{1, 2, 3};
        std::list<int> y{4, 5, 6};
        std::vector<int> z{7, 8, 9};

        chainTest(
            {1, 2, 3, 4, 5, 6, 7, 8, 9},
            x, y, z
        );
    }

    Y_UNIT_TEST(EmptyTest) {
        std::vector<int> x{};
        std::list<int> y{};
        std::list<int> z{};

        chainTest(
            {},
            x, y, z
        );
    }

    Y_UNIT_TEST(SemiEmptyTest) {
        std::vector<int> x{};
        std::list<int> y{4, 5, 6};
        std::vector<int> z;
        std::list<int> t{7, 8, 9};
        std::list<int> w{};

        chainTest(
            {4, 5, 6, 7, 8, 9},
            x, y, z, t, w
        );
    }

    Y_UNIT_TEST(SetTest) {
        // set values are const
        std::vector<int> x{1, 2, 3};
        std::set<int> y{6, 5, 4};
        std::list<int> z{7, 8, 9};

        std::vector<int> expected{1, 2, 3, 4, 5, 6, 7, 8, 9};
        auto cresult = fillFrom(ma::cchain(x, y, z));
        EXPECT_EQ(cresult, expected);
    }
}

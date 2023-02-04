#include <maps/analyzer/libs/interval_tree/include/interval_tree.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <ostream>
#include <string>
#include <vector>
#include <set>


namespace itree = maps::analyzer::interval_tree;

struct Text {
    std::size_t from;
    std::size_t to;
    std::string text;
};

inline bool operator< (const Text& lhs, const Text& rhs) { return lhs.text < rhs.text; }
inline bool operator== (const Text& lhs, const Text& rhs) {
    return lhs.from == rhs.from && lhs.to == rhs.to && lhs.text == rhs.text;
}

std::ostream& operator<< (std::ostream& ostr, const Text& t) {
    return ostr << t.text;
}


std::set<Text> queryEtalon(const std::vector<Text>& words, std::size_t f, std::size_t t) {
    std::set<Text> result;
    for (const auto& w: words) {
        if (t >= w.from && f <= w.to) {
            result.insert(w);
        }
    }
    return result;
}

std::set<Text> queryResult(const itree::IntervalTree<std::size_t, Text>& tree, std::size_t f, std::size_t t) {
    std::set<Text> result;
    for (const auto& w: tree.intersects(f, t)) {
        result.insert(w);
    }
    return result;
}


Y_UNIT_TEST_SUITE(IntervalTreeTest) {
    Y_UNIT_TEST(PointsTest) {
        std::string base = "0123456789ABCDEF";
        std::vector<Text> words;

        for (std::size_t i = 0; i < base.length(); ++i) {
            words.push_back({i, i, base.substr(i, 1)});
        }

        auto tree = itree::intervalTree(std::vector{words}, [](const Text& t) { return std::pair{t.from, t.to}; });

        for (std::size_t i = 0; i < base.length(); ++i) {
            auto expected = queryEtalon(words, i, i);
            auto result = queryResult(tree, i, i);
            EXPECT_EQ(expected, result);
        }
    }

    Y_UNIT_TEST(RangeTest) {
        std::string base = "0123456789ABCDEF";
        std::vector<Text> words;

        for (std::size_t i = 0; i < base.length(); ++i) {
            for (std::size_t j = 1; j <= base.length() - i; ++j) {
                words.push_back({i, i + j - 1, base.substr(i, j)});
            }
        }

        auto tree = itree::intervalTree(words.cbegin(), words.cend(), [](const Text& t) { return std::pair{t.from, t.to}; });

        for (std::size_t i = 0; i < base.length(); ++i) {
            for (std::size_t j = i; j < base.length(); ++j) {
                auto expected = queryEtalon(words, i, j);
                auto result = queryResult(tree, i, j);
                EXPECT_EQ(expected, result);
            }
        }
    }

    Y_UNIT_TEST(EmptyTest) {
        std::vector<Text> words;

        auto tree = itree::intervalTree(std::move(words), [](const Text& t) { return std::pair{t.from, t.to}; });

        std::size_t cnt = 0;
        for (const auto& t: tree.ccontains(0)) {
            std::ignore = t;
            ++cnt;
        }
        for (const auto& t: tree.cintersects(0, 10)) {
            std::ignore = t;
            ++cnt;
        }
        EXPECT_EQ(cnt, 0u);
    }
}

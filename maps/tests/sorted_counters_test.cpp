#include <maps/infra/ratelimiter2/common/include/sorted_counters.h>
#include <maps/infra/ratelimiter2/common/include/test_helpers.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <numeric>
#include <algorithm>

namespace maps::rate_limiter2::tests {

using TestCounters = impl::SortedMap<std::string, int>;

Y_UNIT_TEST_SUITE(sorted_counters_test_) {

Y_UNIT_TEST(plus)
{
    auto result =
        TestCounters({{"a", 1}, {"b", 2}, {"d", 6}}) +
        TestCounters({{"a", 3}, {"c", 5}});
    TestCounters expected({{"a", 4}, {"b", 2}, {"c", 5}, {"d", 6}});

    EXPECT_EQ(result, expected);
}

Y_UNIT_TEST(minus)
{
    auto result =
        TestCounters({{"a", 1}, {"b", 2}}) -
        TestCounters({{"a", 3}, {"c", 4}});
    TestCounters expected({{"a", -2}, {"b", 2}, {"c", -4}});

    EXPECT_EQ(result, expected);
}

Y_UNIT_TEST(plus_equal)
{
    auto result = TestCounters({{"a", 1}, {"b", 2}});
    result += TestCounters({{"a", 3}, {"c", 5}});
    TestCounters expected({{"a", 4}, {"b", 2}, {"c", 5}});

    EXPECT_EQ(result, expected);
}

Y_UNIT_TEST(clamp)
{
    auto result = TestCounters({{"a", 1}, {"b", 2}});
    clamp(result, TestCounters({{"a", 3}, {"c", 4}}));
    TestCounters expected({{"a", 3}, {"b", 2}, {"c", 4}});

    EXPECT_EQ(result, expected);
}

Y_UNIT_TEST(n_way_merge)
{
    std::vector<TestCounters> ops = {
            TestCounters({{"a", 1}, {"b", 2}}),
            TestCounters({{"a", 3},           {"c", 4}}),
            TestCounters({{"a", 5}, {"b", 9},          {"d", 0}})};

    auto result = nWayMerge(
        std::vector<TestCounters*>{&ops[0], &ops[1], &ops[2]},
        [](const std::string&, std::vector<int> values)
        { return std::accumulate(values.begin(), values.end(), 0); });
    TestCounters expected({{"a", 9}, {"b", 11}, {"c", 4}, {"d", 0}});

    EXPECT_EQ(result, expected);
}

Y_UNIT_TEST(n_way_merge_empty)
{
    std::vector<TestCounters> ops = {
            TestCounters({{"a", 1}, {"b", 2}}),
            TestCounters()};

    auto result = nWayMerge(
        std::vector<TestCounters*>{&ops[0], &ops[1]},
        [](const std::string&, std::vector<int> values)
        { return std::accumulate(values.begin(), values.end(), 0); });
    TestCounters expected({{"a", 1}, {"b", 2}});

    EXPECT_EQ(result, expected);
}

Y_UNIT_TEST(find_key)
{
    auto data = TestCounters({{"1", 1}, {"2", 2}, {"5", 5}, {"9", 9}});

    EXPECT_TRUE(data.find("0") == std::end(data));
    EXPECT_TRUE(data.find("3") == std::end(data));
    EXPECT_TRUE(data.find("99") == std::end(data));

    EXPECT_EQ(data.find("2")->second, 2);
    EXPECT_EQ(data.find("5")->second, 5);
    EXPECT_EQ(data.find("9")->second, 9);
}

Y_UNIT_TEST(index_operator)
{
    auto data = TestCounters({{"1", 1}, {"2", 2}, {"5", 5}, {"9", 9}});

    EXPECT_EQ(data["0"], 0);
    EXPECT_EQ(data["1"], 1);
    EXPECT_EQ(data["3"], 0);
    data["5"] = 555;
    EXPECT_EQ(data["5"], 555);
    data["7"] = 7;
    EXPECT_EQ(data["7"], 7);
    EXPECT_EQ(data["9"], 9);
    EXPECT_EQ(data["99"], 0);

    EXPECT_TRUE(data.storage() == TestCounters::Storage({
        {"0", 0}, {"1", 1}, {"2", 2}, {"3", 0}, {"5", 555}, {"7", 7}, {"9", 9}, {"99", 0}}));
}

Y_UNIT_TEST(erase)
{
    TestCounters data({{"1", 1}, {"2", 2}, {"5", 5}, {"9", 9}});

    data.erase(
        std::remove_if(data.begin(), data.end(),
                [](const auto& entry) { return entry.second < 5; }),
        data.end()
    );
    EXPECT_EQ(data, TestCounters({{"5", 5}, {"9", 9}}));

    data.erase(data.begin(), data.end());
    EXPECT_EQ(data, TestCounters());
}

} // Y_UNIT_TEST_SUITE

}  // namespace maps::rate_limiter2::tests

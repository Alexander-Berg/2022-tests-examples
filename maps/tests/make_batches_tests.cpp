#include <maps/libs/common/include/make_batches.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <deque>
#include <list>
#include <set>
#include <unordered_set>
#include <vector>

namespace maps::common::tests {

namespace {

template<typename Container>
void performTest()
{
    Container values{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    EXPECT_EQ(makeBatches(values, 3).size(), 4u);
    int lastVal = 0;
    for (const auto& batch: makeBatches(values, 3)) {
        for (const auto& val: batch) {
            EXPECT_EQ(val, lastVal + 1);
            lastVal = val;
        }
    }
    EXPECT_EQ(lastVal, 10);
}

} //anonymous namespace

TEST(Make_batches_tests, make_batches_test)
{
    performTest<std::list<int>>();
    performTest<std::deque<int>>();
    performTest<std::set<int>>();
    performTest<std::vector<int>>();

    std::unordered_set<int> values{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    EXPECT_EQ(makeBatches(values, 3).size(), 4u);
}

} //namespace maps::common::tests

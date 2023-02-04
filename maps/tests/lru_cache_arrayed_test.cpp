#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <maps/analyzer/libs/lru_cache/include/lru_cache_arrayed.h>

#include <string>

using maps::analyzer::LRUCacheArrayed;

Y_UNIT_TEST_SUITE(TestLruCacheArrayed) {
    struct TestGenerator {
        std::shared_ptr<std::string> operator()() {
            ++cnt;
            return std::make_shared<std::string>("test_" + std::to_string(cnt));
        }

        std::size_t cnt = 0;
    };

    void test(std::shared_ptr<std::string> p, std::string_view v) {
        EXPECT_TRUE(p);
        EXPECT_EQ(*p, v);
    }

    Y_UNIT_TEST(TestBase) {
        LRUCacheArrayed<std::string> cache{10, 3};

        TestGenerator gen;
        const auto f = std::bind(&TestGenerator::operator(), &gen);

        test(cache.findInsert(5, f), "test_1"); // not full yet
        test(cache.findInsert(7, f), "test_2"); // not full yet
        test(cache.findInsert(9, f), "test_3"); // not full yet
        test(cache.findInsert(5, f), "test_1"); // promotes 5
        test(cache.findInsert(3, f), "test_4"); // removes 7
        test(cache.findInsert(7, f), "test_5"); // removes 9
        EXPECT_EQ(gen.cnt, 5);
    }
}

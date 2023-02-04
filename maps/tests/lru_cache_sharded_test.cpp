#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <maps/analyzer/libs/lru_cache/include/sharded_cache.h>

#include <string>

using maps::analyzer::ThreadSafeLruCacheSharded;

Y_UNIT_TEST_SUITE(TestLruCacheSharder) {
    struct TestGenerator {
        std::shared_ptr<std::string> operator()(const std::string& s) {
            ++cnt;
            return std::make_shared<std::string>("test_" + s);
        }

        std::size_t cnt = 0;
    };

    void test(std::shared_ptr<std::string> p, std::string_view v) {
        EXPECT_TRUE(p);
        EXPECT_EQ(*p, v);
    }

    Y_UNIT_TEST(TestBase) {
        ThreadSafeLruCacheSharded<std::string, std::string> cache{4, 2};

        TestGenerator gen;
        const auto f = std::bind(&TestGenerator::operator(), &gen, std::placeholders::_1);

        test(cache.findInsert("key_1", f), "test_key_1");
        test(cache.findInsert("key_1", f), "test_key_1");
        EXPECT_EQ(gen.cnt, 1);
    }
}

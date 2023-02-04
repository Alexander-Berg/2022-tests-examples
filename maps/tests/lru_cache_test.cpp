#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>

#include <maps/analyzer/libs/lru_cache/include/lru_cache.h>

#include <string>

using maps::analyzer::LruCache;

Y_UNIT_TEST_SUITE(TestLruCache) {
    Y_UNIT_TEST(TestLruCacheRemoves) {
        LruCache<int, std::string> cache{3};
        cache.insert(0, "zero");
        cache.insert(1, "one");
        cache.insert(2, "two");
        // peek value and ensure it's not promoted
        UNIT_ASSERT_C(cache.peek(0), "0 should exist in cache");
        UNIT_ASSERT_EQUAL(*cache.peek(0), "zero");
        cache.insert(3, "three");
        cache.insert(4, "four");


        UNIT_ASSERT_C(!cache.peek(0), "should remove out 0");
        UNIT_ASSERT_C(!cache.peek(1), "should remove out 1");
        UNIT_ASSERT_C(cache.peek(2) != nullptr, "should preserve 2");
        UNIT_ASSERT_C(cache.peek(3) != nullptr, "should preserve 3");
        UNIT_ASSERT_C(cache.peek(4) != nullptr, "should preserve 4");
        UNIT_ASSERT_EQUAL(cache.size(), 3);
    }

    Y_UNIT_TEST(TestLruCachePromotes) {
        LruCache<int, std::string> cache{3};
        cache.insert(0, "zero");
        cache.insert(1, "one");
        cache.insert(2, "two");
        if (auto* s = cache.get(0)) {
            *s = "zero-overwritten";
        }
        cache.insert(3, "three");
        cache.insert(4, "four");

        UNIT_ASSERT_C(cache.peek(0) != nullptr, "should preserve 0 as being promoted");
        UNIT_ASSERT_EQUAL(*cache.peek(0), "zero-overwritten");
        UNIT_ASSERT_C(!cache.peek(1), "should remove out 1");
        UNIT_ASSERT_C(!cache.peek(2), "should remove out 2");
        UNIT_ASSERT_C(cache.peek(3) != nullptr, "should preserve 3");
        UNIT_ASSERT_C(cache.peek(4) != nullptr, "should preserve 4");
        UNIT_ASSERT_EQUAL(cache.size(), 3);
    }

    Y_UNIT_TEST(TestLruCacheEmplaces) {
        LruCache<int, std::string> cache{3};
        const auto defaultString = [](int) { return std::string{}; };
        cache.getOrEmplace(0, defaultString) = "zero";
        cache.getOrEmplace(1, defaultString) = "one";
        cache.getOrEmplace(2, defaultString) = "two";
        cache.getOrEmplace(0, defaultString) = "zero-overwritten";
        cache.getOrEmplace(3, defaultString) = "three";
        cache.getOrEmplace(4, defaultString) = "four";

        UNIT_ASSERT_C(cache.peek(0) != nullptr, "should preserve 0 as being promoted");
        UNIT_ASSERT_EQUAL(*cache.peek(0), "zero-overwritten");
        UNIT_ASSERT_C(!cache.peek(1), "should remove out 1");
        UNIT_ASSERT_C(!cache.peek(2), "should remove out 2");
        UNIT_ASSERT_C(cache.peek(3) != nullptr, "should preserve 3");
        UNIT_ASSERT_C(cache.peek(4) != nullptr, "should preserve 4");
        UNIT_ASSERT_EQUAL(cache.size(), 3);
    }
}

#include <maps/analyzer/libs/utils/include/callable.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <functional>
#include <type_traits>

using CallbackFn = std::function<void ()>;
using CallbackFn2 = std::function<void (int)>;

Y_UNIT_TEST_SUITE(CallbackTest) {
    Y_UNIT_TEST(NoArgsFunctionTest) {
        bool called = false;
        CallbackFn cb;
        maps::analyzer::callback(cb);
        EXPECT_FALSE(called);
        cb = [&]() { called = true; };
        maps::analyzer::callback(cb);
        EXPECT_TRUE(called);
    }

    Y_UNIT_TEST(ArgsFunctionTest) {
        int value = 0;
        CallbackFn2 cb;
        maps::analyzer::callback(cb, 10);
        EXPECT_EQ(value, 0);
        cb = [&](int x) { value = x; };
        maps::analyzer::callback(cb, 10);
        EXPECT_EQ(value, 10);
    }

    Y_UNIT_TEST(WrapTest) {
        using Fn = const std::function<int(int)>;

        const auto fn = maps::analyzer::wrapCallable([](int a) { return a * 2; });
        const auto fn2 = maps::analyzer::wrapCallable(fn);

        static_assert(std::is_same_v<decltype(fn), Fn>);
        static_assert(std::is_same_v<decltype(fn2), Fn>);

        EXPECT_EQ(fn(10), fn2(10));
    }

    Y_UNIT_TEST(WrapIfTest) {
        const auto fn = maps::analyzer::wrapCallableIf(true, [](int a) { return a * 2; });
        const auto fn2 = maps::analyzer::wrapCallableIf(false, [](int a) { return a * 2; });
        const auto fn3 = maps::analyzer::wrapCallableIf(true, fn);
        const auto fn4 = maps::analyzer::wrapCallableIf(false, fn);

        EXPECT_TRUE(fn);
        EXPECT_FALSE(fn2);
        EXPECT_TRUE(fn3);
        EXPECT_FALSE(fn4);
    }
}

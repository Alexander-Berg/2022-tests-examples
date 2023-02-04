#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

#include <maps/analyzer/libs/backtrace/include/backtrace.h>

#include <stdexcept>


void foo(bool throwInt) {
    if (throwInt) {
        throw 0;
    }
    throw std::runtime_error("some error");
}

void bar(bool throwInt) { foo(throwInt); }


void checkRuntimeError(const TString& msg) {
    EXPECT_TRUE(msg.Contains("some error"));
    EXPECT_TRUE(msg.Contains("backtrace_test.cpp:13:0 in")); // omitting " foo(bool)" because of autotests
    EXPECT_TRUE(msg.Contains("backtrace_test.cpp:16:0 in")); // omitting " bar(bool)" because of autotests
}

void checkIntError(const TString& msg) {
    EXPECT_TRUE(msg.Contains("unknown exception"));
    EXPECT_TRUE(msg.Contains("backtrace_test.cpp:11:0 in")); // omitting " foo(bool)" because of autotests
    EXPECT_TRUE(msg.Contains("backtrace_test.cpp:16:0 in")); // omitting " bar(bool)" because of autotests
}


TEST(ContainsLocationsTest, BacktraceTest) {
    try {
        bar(false);
    } catch (const std::exception& e) {
        auto msg = maps::analyzer::exceptionMessage(e);
        checkRuntimeError(msg);
    }

    try {
        bar(false);
    } catch ( ... ) {
        auto msg = maps::analyzer::currentExceptionMessage();
        checkRuntimeError(msg);
    }

    try {
        bar(true);
    } catch ( ... ) {
        auto msg = maps::analyzer::currentExceptionMessage();
        checkIntError(msg);
    }
}

#include <maps/libs/concurrent/include/scoped_guard.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <stdexcept>

namespace maps {
namespace concurrent {
namespace tests {

Y_UNIT_TEST_SUITE(test_scoped_guard) {

Y_UNIT_TEST(scope_guard_must_be_executed_on_scope_exit)
{
    auto called = false;
    {
        ScopedGuard guard([&] { called = true; });
        EXPECT_FALSE(called);
    }
    EXPECT_TRUE(called);
}

Y_UNIT_TEST(scope_guard_must_call_function_on_exception)
{
    auto called = false;
    try {
        ScopedGuard guard([&] { called = true; });
        throw std::runtime_error("bad luck");
    }
    catch (...) {
    }
    EXPECT_TRUE(called);
}

} //Y_UNIT_TEST_SUITE

} //namespace tests
} //namespace concurrent
} //namespace maps

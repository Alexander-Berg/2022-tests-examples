#include <maps/libs/common/include/exception.h>
#include <maps/libs/common/include/type_traits.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <util/system/compiler.h>

#include <array>
#include <iostream>

// This file tests windows (MSVC) version as well
namespace maps::common::tests {

TEST(Exception_test, BasicThrow)
{
    using namespace ::testing;
    EXPECT_THAT([]{ throw Exception(); }, Throws<Exception>());
}

struct Custom {};
std::ostream& operator <<(std::ostream& out, const Custom&) { return out << "custom"; }

TEST(Exception_test, MessageCorrect)
{
    try {
        throw Exception() << "message" << ' ' << 42 << ' ' << Custom();
        GTEST_FAIL() << "Exception not thrown";
    } catch (const Exception& ex) {
        EXPECT_STREQ(ex.what(), "message 42 custom");
    }
}

TEST(Exception_test, NonTemporary)
{
    Exception ex;
    ex << "message " << 42;
    try {
        throw ex;
    } catch (const Exception& ex) {
        EXPECT_STREQ(ex.what(), "message 42");
    }
}

TEST(Exception_test, FormatDoubles)
{
    try {
        throw Exception() << 1. << ' ' << 0.29 << ' ' << 0.5 << ' ' << 1. / 3 << ' ' << 0.1 << ' ' << 1.234e-9 << ' ' << 1.234e20 << ' ' << 1.234e30;
        GTEST_FAIL() << "Exception not thrown";
    } catch (const Exception& ex) {
        EXPECT_STREQ(ex.what(), "1 0.29 0.5 0.3333333333333333 0.1 0.000000001234 123400000000000000000 1.234e+30");
    }
}

TEST(Exception_test, FormatFloats)
{
    try {
        throw Exception() << 1.f << ' ' << 0.29f << ' ' << 0.5f << ' ' << 1.f / 3 << ' ' << 0.1f << ' ' << 1.234e-9f << ' ' << 1.234e20f << ' ' << 1.234e30f;
        GTEST_FAIL() << "Exception not thrown";
    } catch (const Exception& ex) {
        EXPECT_STREQ(ex.what(), "1 0.29 0.5 0.33333334 0.1 0.000000001234 123400000000000000000 1.234e+30");
    }
}

class CustomException : public Exception {
public:
    static const int OUTPUT_OVERRIDE_LEVEL = 2;

    template <class E, class T>
    friend std::enable_if_t<
        std::is_base_of_v<CustomException, std::remove_reference_t<E>>
            && !std::is_base_of_v<std::exception, std::remove_reference_t<T>>
            && std::remove_reference_t<E>::OUTPUT_OVERRIDE_LEVEL == 2,
        E&&>
    operator <<(E&& ex, T&& data) {
        static_cast<Exception&&>(ex) << std::forward<T>(data);
        static_cast<Exception&&>(ex) << " 42";
        return std::forward<E>(ex);
    }
};

TEST(Exception_test, OverrideFormat)
{
    try {
        throw CustomException() << "apple" << " orange";
    } catch (CustomException& ex) {
        EXPECT_STREQ(ex.what(), "apple 42 orange 42");
        ex << " pear";
        try {
            throw ex;
        } catch (const CustomException& ex) {
            EXPECT_STREQ(ex.what(), "apple 42 orange 42 pear 42");
            // ex << " cherry"; // should not compile - constant object
        }

    }

    try {
        throw CustomException() << "message";
    } catch (const Exception& ex) {
        EXPECT_STREQ(ex.what(), "message 42");

        try {
            throw Exception(ex) << " extra";
        } catch (const Exception& ex) {
            EXPECT_STREQ(ex.what(), "message 42 extra");
            // ex << 42; // should not compile - constant object
        }
    }
}

void Y_NO_INLINE someBooFunction()
{
    throw Exception("Boo!");
}

// On Windows the test can't find .pdb to resolve functions names.
#ifndef _WIN32
TEST(Exception_test, Backtrace)
{
    try {
        someBooFunction();
    } catch (const Exception& ex) {
        std::ostringstream ss;
        ss << ex;
        EXPECT_THAT(ss.str(), testing::HasSubstr("someBooFunction"));
    }
}
#endif

TEST(Exception_test, CheckAssert)
{
    const auto doAssert = [](int i){ ASSERT(i < 2); };
    EXPECT_NO_THROW(doAssert(1));
    EXPECT_THROW(doAssert(4), LogicError);
}

TEST(Exception_test, Attributes)
{
    Exception ex;
    ex.attrs().emplace("one", "two");
    EXPECT_EQ(ex.attrs().count("one"), 1u);
}

TEST(Exception_test, Chaining)
{
    try {
        try {
            someBooFunction();
        } catch (const Exception& ex) {
            EXPECT_FALSE(ex.cause());
            throw Exception("Underlying error: ") << ex.what();
        }
    } catch (const Exception& ex) {
        EXPECT_TRUE(ex.cause());
        EXPECT_STREQ(ex.what(), "Underlying error: Boo!");
#ifndef _WIN32
        //FIXME: https://st.yandex-team.ru/HEREBEDRAGONS-245
        EXPECT_STREQ(ex.cause()->what(), "Boo!");
#endif
    }
}

TEST(Exception_test, ChainingNonYandex)
{
    try {
        try { throw std::runtime_error("std::runtime_error"); }
        catch (std::exception&) { throw RuntimeError() << "chained exception"; }
    }
    catch (RuntimeError&) {}
}

TEST(Exception_test, ChainingConstChar)
{
    using namespace ::testing;
    EXPECT_THAT(
        []() {
            try {
                throw "error from contrib";
            } catch (const char* ex) {
                throw RuntimeError() << "wrapped(" << ex << ")";
            }
        },
        ThrowsMessage<Exception>(HasSubstr("wrapped(error from contrib)"))
    );
}

TEST(Exception_test, SpecialExceptionHandledInCatchAll)
{
    using namespace ::testing;
    struct SpecialException {};
    EXPECT_THAT(
        []() {
            try {
                throw SpecialException();
            } catch (...) {
                Exception("will not affect SpecialException");
                throw;
            }
        },
        Throws<SpecialException>()
    );
}

namespace other::maps {

constexpr int sumWithAssertion(int a, int b)
{
    ASSERT(a + b == b + a);
    return (a + b);
}

constexpr int sumWithRequire(int a, int b)
{
    REQUIRE(
        a + b == b + a,
        "This world is doomed."
    );
    return (a + b);
}

} // namespace other::maps

TEST(Exception_test, using_assert_and_require_in_constexpr)
{
    constexpr auto TWO_PLUS_TWO = other::maps::sumWithAssertion(2, 2);
    std::array<int, TWO_PLUS_TWO> fourInts{{0, 1, 2, 3}};
    static_assert(TWO_PLUS_TWO == 4, "Test failed");
    static_assert(fourInts.size() == 4, "Test failed");

    constexpr auto ONE_PLUS_ONE = other::maps::sumWithRequire(1, 1);
    std::array<double, ONE_PLUS_ONE> twoDoubles{{1.0, 2.0}};
    static_assert(ONE_PLUS_ONE == 2, "Test failed");
    static_assert(twoDoubles.size() == 2, "Test failed");
}

static_assert(maps::is_ostreamable_to<Exception, std::string>);
static_assert(maps::is_ostreamable_to<RuntimeError, int>);
static_assert(!maps::is_ostreamable_to<Exception, std::exception>);
static_assert(!maps::is_ostreamable_to<RuntimeError, Exception>);

TEST(Exception_test, require_with_custom_exception)
{
    using namespace ::testing;
    class CustomError : public Exception {
    public:
        using Exception::Exception;
    };

    EXPECT_NO_THROW(std::invoke([]() { REQUIRE(true, "ok"); }));

    EXPECT_THAT(
        []() {
            REQUIRE(false, "test-error");
        },
        ThrowsMessage<RuntimeError>(HasSubstr("test-error"))
    );

    EXPECT_THAT(
        []() {
            REQUIRE(false, 10 << " != " << 20);
        },
        ThrowsMessage<RuntimeError>(HasSubstr("10 != 20"))
    );

    EXPECT_THAT(
        []() {
            REQUIRE(false, RuntimeError("runtime"));
        },
        ThrowsMessage<RuntimeError>(HasSubstr("runtime"))
    );

    EXPECT_THAT(
        []() {
            const std::string message = "local message";
            REQUIRE(false, message);
        },
        ThrowsMessage<RuntimeError>(HasSubstr("local message"))
    );

    EXPECT_THAT(
        []() {
            REQUIRE(false, CustomError() << "custom error");
        },
        ThrowsMessage<CustomError>(HasSubstr("custom error"))
    );

    EXPECT_THAT(
        []() {
            CustomError custom("local custom");
            REQUIRE(false, custom);
        },
        ThrowsMessage<CustomError>(HasSubstr("local custom"))
    );

    EXPECT_THAT(
        []() {
            const CustomError constCustom("local const custom");
            REQUIRE(false, constCustom);
        },
        ThrowsMessage<CustomError>(HasSubstr("local const custom"))
    );

    EXPECT_THAT(
        []() {
            REQUIRE(false, std::runtime_error("std_exception"));
        },
        ThrowsMessage<std::runtime_error>(HasSubstr("std_exception"))
    );

    EXPECT_THAT(
        []() {
            ASSERT(false && "Test assert");
        },
        ThrowsMessage<LogicError>(HasSubstr("assertion failed: false && \"Test assert\""))
    );
}

} //namespace maps::common::tests

#include <maps/libs/common/include/retry.h>
#include <maps/libs/common/include/exception.h>
#include <maps/libs/common/include/profiletimer.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::common::tests {

namespace {

constexpr int GOOD_VALUE = 31337;
constexpr int BAD_VALUE = 100500;

class NonAssignable {
public:
    NonAssignable(int value) : value_(value) {}
    NonAssignable(NonAssignable&& other) : value_(other.value_) {}

    int value() const { return value_; }

    bool operator==(const NonAssignable& other) const
    {
        return value_ == other.value_;
    }

    NonAssignable(const NonAssignable&) = delete;
    NonAssignable& operator=(const NonAssignable&) = delete;
    NonAssignable& operator=(NonAssignable&&) = delete;
private:
    const int value_;
};

/*
 * Returns a function which throws on first and second attempts,
 * returns BAD_VALUE on third and GOOD_VALUE on fourth
 */
auto makeRetryFunc(size_t& attempt) {
    return [&]() -> NonAssignable {
        ++attempt;
        switch (attempt) {
            case 1:
                throw RuntimeError("Bad luck");
            case 2:
                throw LogicError("Very bad luck");
            case 3:
                return BAD_VALUE;
            default:
                return GOOD_VALUE;
        }
    };
}

} //anonymous namespace

TEST(retry_tests, test_retrying_once) {
    size_t attempt = 0;
    EXPECT_THROW(
        maps::common::retry(
            makeRetryFunc(attempt),
            RetryPolicy(),
            [](const Expected<NonAssignable>& maybeResult) {
                return maybeResult.valid();
            }
        ),
        RetryNumberExceeded
    );
    EXPECT_EQ(attempt, 1u);
}

TEST(retry_tests, test_retrying_three_times) {
    size_t attempt = 0;
    EXPECT_THROW(
        maps::common::retry(
            makeRetryFunc(attempt),
            RetryPolicy()
                .setTryNumber(3),
            [](const Expected<NonAssignable>& maybeResult) {
                return maybeResult.valid() && maybeResult.get() == GOOD_VALUE;
            }
        ),
        RetryNumberExceeded
    );
    EXPECT_EQ(attempt, 3u);
}

TEST(retry_tests, test_retrying_four_times) {
    auto validate = [](const Expected<NonAssignable>& maybeResult) {
        return maybeResult.valid() && maybeResult.get() == GOOD_VALUE;
    };
    size_t attempt = 0;
    EXPECT_EQ(
        maps::common::retry(
            makeRetryFunc(attempt),
            RetryPolicy()
                .setTryNumber(4),
            validate
        ),
        GOOD_VALUE
    );
    EXPECT_EQ(attempt, 4u);
}

TEST(retry_tests, test_default_validation) {
    size_t attempt = 0;
    EXPECT_EQ(
        maps::common::retry(
            makeRetryFunc(attempt),
            RetryPolicy()
                .setTryNumber(3)
        ),
        BAD_VALUE
    );
    EXPECT_EQ(attempt, 3u);
}

TEST(retry_tests, test_retrying_only_specific_expections) {
    size_t attempt = 0;

    /*
     * This validation will retry RuntimeError,
     * but will stop iterations immediately, if LogicError occurrs
     */
    auto validateResult = [](const Expected<NonAssignable>& maybeVal) {
        if (maybeVal.hasException<LogicError>()) {
            maybeVal.rethrowException();
        };
        return maybeVal.valid() && maybeVal.get() == GOOD_VALUE;
    };
    EXPECT_THROW(
        maps::common::retry(
            makeRetryFunc(attempt),
            RetryPolicy()
                .setTryNumber(4),
            validateResult
        ),
        LogicError
    );
    EXPECT_EQ(attempt, 2u);
}

TEST(retry_tests, test_void_result) {
    EXPECT_NO_THROW(
        maps::common::retry(
            []() -> void { },
            RetryPolicy()
        )
    );

    EXPECT_THROW(
        maps::common::retry(
            []() -> void { throw LogicError(); },
            RetryPolicy(),
            [](const Expected<void>&) {
                return true;
            }
        ),
        LogicError
    );
}

TEST(retry_tests, test_error_message) {
    int attempt = 0;
    auto func = [&attempt]() -> void {
        throw Exception("ERROR[" + std::to_string(++attempt) + "]");
    };
    try {
        maps::common::retry(func, RetryPolicy().setTryNumber(2));
        FAIL() << "expected retry exception";
    } catch (const RetryNumberExceeded& e) {
        EXPECT_THAT(e.what(), testing::HasSubstr("ERROR[2]"));
        ASSERT_THAT(e.cause(), testing::NotNull());
#ifndef _WIN32
        //FIXME: https://st.yandex-team.ru/HEREBEDRAGONS-245
        EXPECT_THAT(e.cause()->what(), testing::StrEq("ERROR[2]"));
#endif
    } catch (...) {
        FAIL() << "expected retry exception";
    }
}

TEST(retry_tests, test_non_exceptional_error_message) {
    auto makeErrorMessage = [](const NonAssignable& wrongResult) {
         return "invalid value " + std::to_string(wrongResult.value());
    };
    try {
        int attempt = 0;
        maps::common::retry(
            [&attempt]() -> NonAssignable { return ++attempt; },
            RetryPolicy()
                .setTryNumber(3),
            [](const auto&) { return false; },
            makeErrorMessage
        );
        FAIL() << "expected retry exception";
    } catch (const RetryNumberExceeded& e) {
        EXPECT_THAT(e.what(), testing::HasSubstr("invalid value 3"));
    } catch (...) {
        FAIL() << "expected retry exception";
    }
}

TEST(retry_tests, test_cooldown) {
    ProfileTimer timer;
    EXPECT_THROW(
        maps::common::retry(
            []() -> void {},
            RetryPolicy()
                .setTryNumber(3)
                .setInitialCooldown(std::chrono::milliseconds(300))
                .setCooldownBackoff(3.0),
            [](const auto&) { return false; }
        ),
        RetryNumberExceeded
    );
    double execTime = timer.getElapsedTimeNumber(); // ~ 1.2 sec
    EXPECT_THAT(execTime, testing::Gt(1.0));
    EXPECT_THAT(execTime, testing::Lt(2.0));
}

} //namespace maps::common::tests

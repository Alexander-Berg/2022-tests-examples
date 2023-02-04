#include <maps/libs/common/include/expected.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <climits>
#include <stdexcept>
#include <string>
#include <cstddef>

namespace maps::common::tests {

namespace {

struct Stat {
    size_t copyCtorCalls = 0;
    size_t moveCtorCalls = 0;
    size_t copyAssignCalls = 0;
    size_t moveAssignCalls = 0;
    size_t dtorCalls = 0;
};

struct Recorder {
    Recorder(Stat& stat) : stat_(&stat) {}

    Recorder(const Recorder& rhs) : stat_(rhs.stat_)
    {
        stat_->copyCtorCalls += 1;
    }

    Recorder(Recorder&& rhs) noexcept : stat_(rhs.stat_) { stat_->moveCtorCalls += 1; }

    Recorder& operator=(const Recorder&)
    {
        stat_->copyAssignCalls += 1;
        return *this;
    }

    Recorder& operator=(Recorder&&)
    {
        stat_->moveAssignCalls += 1;
        return *this;
    }

    ~Recorder() { stat_->dtorCalls += 1; }

private:
    Stat* stat_;
};

struct NonAssignable {
    NonAssignable(Stat& stat) : stat_(&stat) {}

    NonAssignable(NonAssignable&& rhs) : stat_(rhs.stat_)
    {
        if (stat_)
            stat_->moveCtorCalls += 1;
        rhs.stat_ = nullptr;
    }

    ~NonAssignable()
    {
        if (stat_)
            stat_->dtorCalls += 1;
    }

    NonAssignable(const NonAssignable&) = delete;
    NonAssignable& operator=(const NonAssignable&) = delete;
    NonAssignable& operator=(NonAssignable&&) = delete;

    const Stat* stat() const
    {
        return stat_;
    }

private:
    Stat* stat_;
};

// Dummy exception class.
class Flame : public std::exception {};

} // anonymous namespace

TEST(Expected_tests, expected_can_be_constructed_from_object)
{
    Expected<int> justInt(5);
    EXPECT_TRUE(justInt.valid());
    EXPECT_EQ(5, justInt.get());
}

TEST(Expected_tests, expected_can_be_constructed_from_exception)
{
    auto failure = Expected<int>::fromException(std::bad_alloc());
    EXPECT_FALSE(failure.valid());
    EXPECT_TRUE(failure.hasException<std::bad_alloc>());
    EXPECT_THROW(failure.get(), std::bad_alloc);
}

TEST(Expected_tests, expected_can_be_constructed_from_lambda)
{
    auto failure = expectedFromCode([]() -> int {
        REQUIRE(false, "Bad luck");
        return 1;
    });
    EXPECT_FALSE(failure.valid());
    EXPECT_TRUE(failure.hasException<std::exception>());
    EXPECT_TRUE(failure.hasException<Exception>());
    EXPECT_FALSE(failure.hasException<std::logic_error>());
    EXPECT_THROW(failure.get(), Exception);
    EXPECT_THROW_MESSAGE_HAS_SUBSTR(
        failure.rethrowException(),
        maps::RuntimeError,
        "Bad luck");

    auto box = expectedFromCode([] { return std::string("ok"); });
    EXPECT_TRUE(box.valid());
    EXPECT_EQ("ok", box.get());
}

TEST(Expected_tests, expected_can_be_copied_and_moved_when_valid)
{
    std::string value("magic");
    Expected<std::string> boxedString(value);
    Expected<std::string> copy(boxedString);

    EXPECT_TRUE(boxedString.valid());
    EXPECT_TRUE(copy.valid());
    EXPECT_EQ(value, copy.get());
    EXPECT_EQ(boxedString.get(), copy.get());

    Expected<std::string> moved(std::move(copy));
    EXPECT_TRUE(moved.valid());
    EXPECT_EQ(value, moved.get());
}

TEST(Expected_tests, expected_can_be_copied_and_moved_when_invalid)
{
    Expected<int> failure = Expected<int>::fromException(std::bad_alloc());
    Expected<int> copiedFailure(failure);
    EXPECT_FALSE(failure.valid());
    EXPECT_FALSE(copiedFailure.valid());
    EXPECT_TRUE(copiedFailure.hasException<std::bad_alloc>());

    Expected<int> movedFailure(std::move(copiedFailure));
    EXPECT_FALSE(movedFailure.valid());
    EXPECT_TRUE(movedFailure.hasException<std::bad_alloc>());
}

TEST(Expected_tests, expected_can_be_copy_and_move_assigned)
{
    Expected<std::string> boxed("magic");
    auto copy = boxed;
    EXPECT_TRUE(copy.valid());
    EXPECT_EQ(boxed.get(), copy.get());

    auto moved = std::move(copy);
    EXPECT_TRUE(moved.valid());
    EXPECT_EQ(boxed.get(), moved.get());
}

TEST(Expected_tests, expected_must_throw_on_null_exception_ptr)
{
    EXPECT_THROW(Expected<int>(std::exception_ptr{}), Exception);
}

TEST(Expected_tests, expected_prevents_exception_slicing)
{
    EXPECT_THROW(
        Expected<int>::fromException<std::exception>(std::bad_alloc()),
        Exception);
}

TEST(Expected_tests, expected_retrows_exception)
{
    auto exp = Expected<int>::fromException(RuntimeError());
    EXPECT_THROW(
        exp.rethrowException(),
        RuntimeError
    );
}

TEST(Expected_tests, expected_asserts_upon_rethrow_if_contains_value)
{
    Expected<int> exp(31337);
    EXPECT_THROW(
        exp.rethrowException(),
        LogicError
    );
}

TEST(Expected_tests, expected_provides_swap)
{
    std::string value1("AAA");
    std::string value2("BBB");
    Expected<std::string> fullBox1(value1);
    Expected<std::string> fullBox2(value2);
    auto logicFailure
        = Expected<std::string>::fromException(std::logic_error("Ouch"));
    auto allocFailure
        = Expected<std::string>::fromException(std::bad_alloc());

    //swapping two valid Expected instances
    fullBox1.swap(fullBox2);
    EXPECT_TRUE(fullBox1.valid());
    EXPECT_TRUE(fullBox2.valid());
    EXPECT_EQ(value1, fullBox2.get());
    EXPECT_EQ(value2, fullBox1.get());
    fullBox1.swap(fullBox2);

    //swapping two exceptional Expected instances
    EXPECT_TRUE(logicFailure.hasException<std::logic_error>());
    EXPECT_TRUE(allocFailure.hasException<std::bad_alloc>());
    logicFailure.swap(allocFailure);

    EXPECT_FALSE(logicFailure.valid());
    EXPECT_FALSE(allocFailure.valid());
    EXPECT_TRUE(logicFailure.hasException<std::bad_alloc>());
    EXPECT_TRUE(allocFailure.hasException<std::logic_error>());
    allocFailure.swap(logicFailure);

    //swapping valid Expected instance with an exceptional one
    fullBox1.swap(logicFailure);
    EXPECT_FALSE(fullBox1.valid());
    EXPECT_TRUE(fullBox1.hasException<std::logic_error>());
    EXPECT_TRUE(logicFailure.valid());
    EXPECT_EQ(value1, logicFailure.get());
    fullBox1.swap(logicFailure);
}

TEST(Expected_tests, expected_provides_emplace)
{
    using testing::Eq;
    using testing::Gt;

    Stat stat1;
    Stat stat2;
    NonAssignable value1(stat1);
    NonAssignable value2(stat2);

    Expected<NonAssignable> expected(std::move(value1));
    ASSERT_TRUE(expected.valid());
    EXPECT_THAT(expected.get().stat(), Eq(&stat1));
    EXPECT_THAT(stat1.dtorCalls, Eq(0u));
    EXPECT_THAT(stat2.dtorCalls, Eq(0u));
    EXPECT_THAT(stat1.moveCtorCalls, Gt(0u));
    EXPECT_THAT(stat2.moveCtorCalls, Eq(0u));

    expected.emplace(std::move(value2));
    ASSERT_TRUE(expected.valid());
    EXPECT_THAT(expected.get().stat(), Eq(&stat2));
    EXPECT_THAT(stat1.dtorCalls, Eq(1u));
    EXPECT_THAT(stat2.dtorCalls, Eq(0u));
    EXPECT_THAT(stat1.moveCtorCalls, Gt(0u));
    EXPECT_THAT(stat2.moveCtorCalls, Gt(0u));

    expected.emplace(std::make_exception_ptr(std::bad_alloc()));
    EXPECT_FALSE(expected.valid());
    EXPECT_THAT(stat1.dtorCalls, Eq(1u));
    EXPECT_THAT(stat2.dtorCalls, Eq(1u));

    Stat stat3;
    Expected<NonAssignable> expected2(NonAssignable{stat3});
    EXPECT_THAT(stat3.moveCtorCalls, Gt(0u));
    auto prevMoveCtorCalls = stat3.moveCtorCalls;

    expected.emplace(std::move(expected2));
    ASSERT_TRUE(expected.valid());
    EXPECT_THAT(expected.get().stat(), Eq(&stat3));
    EXPECT_THAT(stat3.dtorCalls, Eq(0u));
    EXPECT_THAT(stat3.moveCtorCalls, Gt(prevMoveCtorCalls));

    expected.emplace(Expected<NonAssignable>::fromException(std::bad_alloc()));
    EXPECT_FALSE(expected.valid());
    EXPECT_THAT(stat3.dtorCalls, Eq(1u));

    // emplace void
    Expected<void> expVoid;
    EXPECT_TRUE(expVoid.valid());
    expVoid.emplace(std::make_exception_ptr(std::bad_alloc()));
    EXPECT_FALSE(expVoid.valid());
    expVoid.emplace(Expected<void>{});
    EXPECT_TRUE(expVoid.valid());
    expVoid.emplace(Expected<void>::fromException(std::bad_alloc()));
    EXPECT_FALSE(expVoid.valid());
}

TEST(Expected_tests, expected_ctor_calls_object_copy_ctor_and_dtor)
{
    Stat copyStat;
    Recorder recorder(copyStat);
    {
        Expected<Recorder> box(recorder);
    }
    EXPECT_EQ(1u, copyStat.copyCtorCalls);
    EXPECT_EQ(1u, copyStat.moveCtorCalls);
    EXPECT_EQ(2u, copyStat.dtorCalls);
}

TEST(Expected_tests, expected_move_ctor_calls_object_move_ctor_and_dtor)
{
    Stat moveStat;
    Recorder recorder(moveStat);
    {
        Expected<Recorder> box(std::move(recorder));
    }
    EXPECT_EQ(2u, moveStat.moveCtorCalls);
    EXPECT_EQ(2u, moveStat.dtorCalls);
}

TEST(Expected_tests, expected_precise_when_calling_dtors)
{
    Stat stat;
    Recorder recorder(stat);
    {
        Expected<Recorder> box1(recorder);
        Expected<Recorder> box2(recorder);
        Expected<Recorder> box3(std::move(recorder));
        auto badBox = Expected<Recorder>::fromException(std::bad_alloc());
        box1 = box2;
        box1 = std::move(box2);
        box3.swap(box1);
        box3 = badBox;
        box1.swap(badBox);
    }
    EXPECT_EQ(stat.dtorCalls, stat.copyCtorCalls + stat.moveCtorCalls);
}

TEST(Expected_tests, expected_void_instantiation_and_ops)
{
    Expected<void> toMove;
    EXPECT_TRUE(toMove.valid());

    Expected<void> moved = std::move(toMove);
    EXPECT_TRUE(moved.valid());

    auto badVoidExpected = Expected<void>::fromException(std::bad_alloc());
    EXPECT_FALSE(badVoidExpected.valid());
    EXPECT_TRUE(badVoidExpected.hasException<std::bad_alloc>());
    EXPECT_THROW(badVoidExpected.rethrowException(), std::bad_alloc);
}

TEST(Expected_tests, expect_void_from_code)
{
    auto voider = []() -> void {
        return;
    };

    auto thrower = []() -> void {
        throw std::bad_alloc();
    };

    auto goodExpected = expectedFromCode(voider);
    EXPECT_TRUE(goodExpected.valid());

    auto badExpected = expectedFromCode(thrower);
    EXPECT_FALSE(badExpected.valid());
    EXPECT_TRUE(badExpected.hasException<std::bad_alloc>());
}

TEST(Expected_tests, expect_provides_dereference)
{
    int value = 42;
    auto valueProvider = [=] {
        return value;
    };
    auto flamethrower = [] () -> decltype(valueProvider) {
        throw Flame();
    };

    // Value checks.
    auto expected = expectedFromCode(valueProvider);
    const auto constExpected = expectedFromCode(valueProvider);
    EXPECT_NO_THROW(*expected);
    EXPECT_EQ(*expected, value);
    EXPECT_EQ(*expected, *constExpected);

    // Assignment checks.
    *expected = value + 1;
    EXPECT_EQ(*expected, value + 1);

    // Exception checks.
    auto badExpected = expectedFromCode(flamethrower);
    const auto constBadExpected = expectedFromCode(flamethrower);
    EXPECT_THROW(*badExpected, Flame);
    EXPECT_THROW(*constBadExpected, Flame);
}

TEST(Expected_tests, expect_maps_value) {
    constexpr auto toString = [](int v) { return std::to_string(v); };

    Expected<int> value = 5;
    Expected<std::string> valueStr = value.map(toString);
    ASSERT_TRUE(valueStr.valid());
    EXPECT_EQ(valueStr.get(), "5");

    value = Expected<int>::fromException(std::runtime_error("invalid value"));
    valueStr = value.map(toString);
    EXPECT_TRUE(valueStr.hasException());
    EXPECT_TRUE(valueStr.hasException<std::runtime_error>());
}

TEST(Expected_tests, expect_propagate_exception) {
    constexpr auto provideValue = [](bool err) -> Expected<int> {
        if (err) {
            return Expected<int>::fromException(std::runtime_error("invalid value"));
        } else {
            return 42;
        }
    };

    constexpr auto convertedValue = [=](bool err) -> Expected<std::string> {
        auto value = provideValue(err);
        if (!value.valid()) {
            return value.getException(); // propagate
        } else {
            return std::to_string(value.get());
        }
    };

    auto expected = convertedValue(false);
    ASSERT_TRUE(expected.valid());
    EXPECT_EQ(expected.get(), "42");

    auto unexpected = convertedValue(true);
    EXPECT_TRUE(unexpected.hasException());
    EXPECT_TRUE(unexpected.hasException<std::runtime_error>());
}

} //namespace maps::common::tests

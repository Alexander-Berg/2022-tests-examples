#pragma once

#include <maps/libs/json/include/value.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <string>
#include <utility>

namespace yacare::tests {

namespace impl {

/// JSON field matcher
template<typename T>
class HasFieldMatcher {
public:
    using is_gtest_matcher = void;

public:
    HasFieldMatcher(
        std::string field,
        ::testing::Matcher<const T&> valueMatcher)
        : field_{std::move(field)}, valueMatcher_{std::move(valueMatcher)}
    { }

    // NOLINTNEXTLINE(readability-identifier-naming)
    bool MatchAndExplain(
        const maps::json::Value& value,
        ::testing::MatchResultListener* os) const
    {
        if (!value.isObject()) {
            *os << "is not of object type";
            return false;
        }

        if (!value.hasField(field_)) {
            *os << "has no field named '" << field_ << "'";
            return false;
        }

        try {
            if constexpr (std::is_same_v<T, maps::json::Value>) {
                return valueMatcher_.MatchAndExplain(value[field_], os);
            }
            else {
                return valueMatcher_.MatchAndExplain(
                    value[field_].template as<T>(), os);
            }
        } catch (const std::exception& ex) {
            *os << "whose value is of wrong type (" << ex.what() << ")";
            return false;
        }
    }

    // NOLINTNEXTLINE(readability-identifier-naming)
    void DescribeTo(std::ostream* os) const
    {
        *os << "is Json object with field '" << field_ << "' and value ";
        valueMatcher_.DescribeTo(os);
    }

    // NOLINTNEXTLINE(readability-identifier-naming)
    void DescribeNegationTo(std::ostream* os) const
    {
        *os << "not (";
        DescribeTo(os);
        *os << ")";
    }

private:
    std::string field_;
    ::testing::Matcher<const T&> valueMatcher_;
};
} // namespace impl

/// Makes JSON field matcher with key `name` and value matching `valueMatcher`
///
/// @param[in] name matched field key
/// @param[in] valueMatcher matcher for the value denoted by `name` key
template<typename T>
::testing::Matcher<const maps::json::Value&> hasField(
    const std::string& name, ::testing::Matcher<const T&> valueMatcher)
{
    return impl::HasFieldMatcher<T>{name, std::move(valueMatcher)};
}

} // namespace yacare::tests

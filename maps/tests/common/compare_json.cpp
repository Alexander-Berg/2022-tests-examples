#include "compare_json.h"
#include <maps/libs/log8/include/log8.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <sstream>
#include <string_view>

namespace infopoint {

namespace {

std::string errorMessage(
    std::string_view prefix,
    std::string_view message,
    std::string_view path)
{
    return (std::stringstream() << prefix << message << " at " << path).str();
}

std::string errorMessage(
    std::string_view prefix,
    std::string_view message,
    std::string_view path,
    const auto& actual,
    const auto& expected)
{
    return (std::stringstream()
        << errorMessage(prefix, message, path)
        << ". Actual: " << actual << ". Expected: " << expected).str();
}

} // namespace

void assertJsonValuesEqual(
    const maps::json::Value& actual,
    const maps::json::Value& expected,
    std::string_view prefix)
{
    ASSERT_EQ(actual.type(), expected.type())
        << errorMessage(
            prefix,
            "different type of field",
            expected.evaluatePointer(),
            actual.type(),
            expected.type());

    if (actual.isNumber()) {
        constexpr double EPSILON = 1.0e-6;
        ASSERT_NEAR(actual.as<double>(), expected.as<double>(), EPSILON)
            << errorMessage(
                prefix,
                "different number values",
                expected.evaluatePointer(),
                actual.toString(),
                expected.toString());
        return;
    }

    if (actual.isObject()) {
        ASSERT_EQ(actual.size(), expected.size())
            << errorMessage(
                prefix,
                "different number of elements",
                expected.evaluatePointer(),
                actual.size(),
                expected.size());

        for (auto& field: expected.fields()) {
            ASSERT_TRUE(actual.hasField(field))
                << errorMessage(
                    prefix,
                    "absent field",
                    expected[field].evaluatePointer()
                    );

            assertJsonValuesEqual(actual[field], expected[field], prefix);
        }
        return;
    }

    if (actual.isArray()) {
        ASSERT_EQ(actual.size(), expected.size())
            << errorMessage(
                prefix,
                "different number of elements",
                expected.evaluatePointer(),
                actual.size(),
                expected.size());

        for (size_t i = 0; i < expected.size(); ++i) {
            assertJsonValuesEqual(actual[i], expected[i], prefix);
        }
        return;
    }

    ASSERT_EQ(actual, expected)
        << errorMessage(
            prefix,
            "different values",
            expected.evaluatePointer(),
            actual.toString(),
            expected.toString());
}
} // namespace infopoint

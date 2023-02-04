#include <yandex_io/libs/logging/helpers.h>

#include <library/cpp/testing/unittest/registar.h>

#include <util/generic/size_literals.h>

using namespace quasar::Logging::helpers;

Y_UNIT_TEST_SUITE(LoggingHelpers) {
    Y_UNIT_TEST(LogLevelFromString) {
        UNIT_ASSERT_EQUAL(getLogLevelFromString("TRACE"), spdlog::level::trace);
        UNIT_ASSERT_EQUAL(getLogLevelFromString("DEBUG"), spdlog::level::debug);
        UNIT_ASSERT_EQUAL(getLogLevelFromString("INFO"), spdlog::level::info);
        UNIT_ASSERT_EQUAL(getLogLevelFromString("WARN"), spdlog::level::warn);
        UNIT_ASSERT_EQUAL(getLogLevelFromString("ERROR"), spdlog::level::err);
        UNIT_ASSERT_EQUAL(getLogLevelFromString("FATAL"), spdlog::level::critical);

        UNIT_ASSERT_EXCEPTION_CONTAINS(getLogLevelFromString("FooBar"), std::runtime_error, "Unknown logging level");
    }

    Y_UNIT_TEST(ParseFileSize) {
        const auto cases = std::vector<std::pair<ui64, std::vector<std::string_view>>>{
            {
                {10, {"10", "10b", "10B"}},
                {10_KB, {"10kb", "10KB"}},
                {10_MB, {"10mb", "10MB"}},
                {10_GB, {"10gb", "10GB"}},
            },
        };
        for (const auto& [number, strs] : cases) {
            for (const auto& str : strs) {
                UNIT_ASSERT_VALUES_EQUAL_C(parseFileSize(str), number, str);
            }
        }

        UNIT_ASSERT_EXCEPTION_CONTAINS(parseFileSize("10k"), std::runtime_error, "Invalid file size");
        UNIT_ASSERT_EXCEPTION_CONTAINS(parseFileSize("10ZB"), std::runtime_error, "Invalid file size");
    }
}

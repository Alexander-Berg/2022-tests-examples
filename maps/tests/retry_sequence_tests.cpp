#include <maps/infra/apiteka/agent/lib/retry_sequence.h>

#include <contrib/restricted/googletest/googletest/include/gtest/gtest.h>
#include <contrib/restricted/googletest/googlemock/include/gmock/gmock-matchers.h>
#include <contrib/restricted/googletest/googlemock/include/gmock/gmock-more-matchers.h>
#include <contrib/restricted/googletest/googlemock/include/gmock/gmock-spec-builders.h>

using namespace testing;
using namespace std::literals;

namespace maps::apiteka::tests {
namespace samples {
const detail::RetrySequence LONG_SEQUENCE{
    {.tryNumber = 4, .initialCooldown = 10ns, .cooldownBackoff = 2.0}};
} // namespace samples
TEST(retry_tests, zero_try_number_leads_to_empty_sequence)
{
    ASSERT_THAT(detail::RetrySequence{{.tryNumber = 0}}, ElementsAre());
}

TEST(retry_tests, single_try_number)
{
    ASSERT_THAT(detail::RetrySequence{{.tryNumber = 1}}, ElementsAre(std::nullopt));
}

TEST(retry_tests, test_retry_sequence)
{
    EXPECT_THAT(samples::LONG_SEQUENCE, ElementsAre(10ns, 20ns, 40ns, std::nullopt));
}

TEST(retry_tests, range_based_usage_pattern)
{
    MockFunction<void()> mock;
    EXPECT_CALL(mock, Call)
        .Times(std::distance(
            samples::LONG_SEQUENCE.begin(), samples::LONG_SEQUENCE.end()));
    for (const auto& cooldown: samples::LONG_SEQUENCE) {
        mock.Call();
        if (!cooldown)
            break;
    }
}
} // namespace maps::apiteka::tests

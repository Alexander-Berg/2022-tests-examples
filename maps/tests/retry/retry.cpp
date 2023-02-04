#include <maps/infra/ecstatic/coordinator/lib/retry.h>

#include <library/cpp/testing/unittest/registar.h>

namespace retry {
Y_UNIT_TEST_SUITE(test_retry)
{
Y_UNIT_TEST(test_status_string)
{
    const Status status {Retry {3, 60}, Error {2, 60}, 89};
    auto const expected = "2 attempt out of 4, next in 0:31 seconds";

    UNIT_ASSERT_VALUES_EQUAL(status.string(), expected);
}

Y_UNIT_TEST(test_status_string_expired)
{
    const Status status {Retry {3, 60}, Error {2, 60}, 121};
    auto const expected = "2 attempt out of 4, next in 0:00 seconds";

    UNIT_ASSERT_VALUES_EQUAL(status.string(), expected);
}

Y_UNIT_TEST(test_status_string_failed_3_of_4)
{
    const Status status {Retry(3, 60), Error(3, 0), 29};
    auto const expected = "3 attempt out of 4, next in 0:31 seconds";

    UNIT_ASSERT_VALUES_EQUAL(status.string(), expected);
}

Y_UNIT_TEST(test_status_string_failed_4_of_4)
{
    const Status status {Retry(3, 60), Error(4, 0), 29};
    auto const expected = "4 attempt out of 4";

    UNIT_ASSERT_VALUES_EQUAL(status.string(), expected);
}

Y_UNIT_TEST(test_retry_empty_bson_object)
{
    const bsoncxx::document::view object;
    const Retry retry {object};
    UNIT_ASSERT_VALUES_EQUAL(retry.count, 0);
    UNIT_ASSERT_VALUES_EQUAL(retry.intervalSeconds, 0);
    UNIT_ASSERT_VALUES_EQUAL(retry.enabled(), false);
}

Y_UNIT_TEST(test_error_empty_bson_object)
{
    const bsoncxx::document::view object;
    const Error error {object};
    UNIT_ASSERT_VALUES_EQUAL(error.count, 1);
    UNIT_ASSERT_VALUES_EQUAL(error.time, 0);
}
}
} // namespace retry

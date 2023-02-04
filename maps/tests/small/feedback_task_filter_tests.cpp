#include <yandex/maps/wiki/social/feedback/task_filter.h>

#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::social::feedback::tests {

Y_UNIT_TEST_SUITE(feedback_task_filter)
{
    Y_UNIT_TEST(should_fail_filter_by_source_not_in_on_empty_input)
    {
        UNIT_ASSERT_EXCEPTION(
            TaskFilter().sourceNotIn({}),
            LogicError
        );
    }
} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::social::feedback::tests

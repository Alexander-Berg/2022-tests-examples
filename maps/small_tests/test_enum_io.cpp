#include <maps/wikimap/feedback/api/src/libs/feedback_task_query_builder/select_query.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::feedback::api::dbqueries::tests {

using feedback_task_query_builder::SelectOrder;

Y_UNIT_TEST_SUITE(test_enum_io)
{

Y_UNIT_TEST(select_order_multiple_representations)
{
    UNIT_ASSERT_VALUES_EQUAL(
        maps::enum_io::fromString<SelectOrder>("created_at ASC"),
        SelectOrder::CreatedAtAsc);
    UNIT_ASSERT_VALUES_EQUAL(
        maps::enum_io::fromString<SelectOrder>("created_at DESC"),
        SelectOrder::CreatedAtDesc);
    UNIT_ASSERT_VALUES_EQUAL(
        maps::enum_io::fromString<SelectOrder>("updated_at ASC"),
        SelectOrder::UpdatedAtAsc);
    UNIT_ASSERT_VALUES_EQUAL(
        maps::enum_io::fromString<SelectOrder>("updated_at DESC"),
        SelectOrder::UpdatedAtDesc);

    UNIT_ASSERT_VALUES_EQUAL(
        maps::enum_io::fromString<SelectOrder>("created_at_asc"),
        SelectOrder::CreatedAtAsc);
    UNIT_ASSERT_VALUES_EQUAL(
        maps::enum_io::fromString<SelectOrder>("created_at_desc"),
        SelectOrder::CreatedAtDesc);
    UNIT_ASSERT_VALUES_EQUAL(
        maps::enum_io::fromString<SelectOrder>("updated_at_asc"),
        SelectOrder::UpdatedAtAsc);
    UNIT_ASSERT_VALUES_EQUAL(
        maps::enum_io::fromString<SelectOrder>("updated_at_desc"),
        SelectOrder::UpdatedAtDesc);

    UNIT_ASSERT_VALUES_EQUAL(
        toString(SelectOrder::CreatedAtAsc),
        "created_at ASC");
    UNIT_ASSERT_VALUES_EQUAL(
        toString(SelectOrder::CreatedAtDesc),
        "created_at DESC");
    UNIT_ASSERT_VALUES_EQUAL(
        toString(SelectOrder::UpdatedAtAsc),
        "updated_at ASC");
    UNIT_ASSERT_VALUES_EQUAL(
        toString(SelectOrder::UpdatedAtDesc),
        "updated_at DESC");
}

} // test_enum_io suite

} // namespace maps::wiki::feedback::api::dbqueries::tests

#include <library/cpp/testing/unittest/registar.h>
#include <maps/wikimap/mapspro/libs/social/include/yandex/maps/wiki/social/feedback/task_filter.h>
#include "maps/wikimap/mapspro/libs/social/tests/medium/feedback_helpers.h"
#include "maps/wikimap/mapspro/libs/social/tests/medium/helpers.h"

namespace maps::wiki::social::tests {

using namespace feedback;

Y_UNIT_TEST_SUITE(feedback_filter) {

Y_UNIT_TEST_F(uiTaskStatus, DbFixture)
{
    pqxx::work txn(conn);
    TaskFilter filter;

    filter.uiFilterStatus(std::nullopt);
    UNIT_ASSERT_VALUES_EQUAL(filter.whereClause(txn), "TRUE AND bucket IN ('outgoing','need-info')");

    filter.uiFilterStatus(UIFilterStatus::Opened);
    UNIT_ASSERT_VALUES_EQUAL(filter.whereClause(txn), "TRUE AND resolved_by IS NULL AND bucket IN ('outgoing')");

    filter.uiFilterStatus(UIFilterStatus::NeedInfo);
    UNIT_ASSERT_VALUES_EQUAL(filter.whereClause(txn), "TRUE AND resolved_by IS NULL AND bucket IN ('need-info')");

    filter.uiFilterStatus(UIFilterStatus::Resolved);
    UNIT_ASSERT_VALUES_EQUAL(filter.whereClause(txn), "TRUE AND resolved_by IS NOT NULL AND bucket IN ('outgoing')");
}

}

} // namespace maps::wiki::social::feedback::tests

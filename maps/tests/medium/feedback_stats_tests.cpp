#include "helpers.h"

#include <maps/wikimap/mapspro/libs/social/feedback/stats.h>

#include <library/cpp/testing/unittest/registar.h>

#include <fmt/format.h>


namespace maps::wiki::social::feedback::stats::tests {

using namespace maps::wiki::social::tests;


namespace {

void createHistoryItem(
    pqxx::transaction_base& txn,
    std::string modifiedAt,
    TUid modifiedBy,
    TaskOperation operation)
{
    using namespace fmt::literals;

    txn.exec(
        fmt::format(
            "INSERT INTO social.feedback_history "
            "(feedback_task_id, modified_at, modified_by, operation) "
            "VALUES (1, {modified_at}, {modified_by}, '{operation}')",

            "modified_at"_a = modifiedAt,
            "modified_by"_a = modifiedBy,
            "operation"_a = toString(operation)
        )
    );
}

} // namespace


Y_UNIT_TEST_SUITE(countOperationsToday)
{
    Y_UNIT_TEST_F(shouldCountForTimeZone, DbFixture)
    {
        pqxx::work txn(conn);

        createHistoryItem(txn, "CONCAT(CURRENT_DATE, ' 00:15 UTC')::timestamptz + '-1 hours'::interval", 1, TaskOperation::Accept);
        createHistoryItem(txn, "CONCAT(CURRENT_DATE, ' 00:15 UTC')::timestamptz + ' 0 hours'::interval", 1, TaskOperation::Accept);
        createHistoryItem(txn, "CONCAT(CURRENT_DATE, ' 00:15 UTC')::timestamptz + '+1 hours'::interval", 1, TaskOperation::Accept);

        // Time zone offset is equal to difference of UTC and local time, so
        // offset is negative if the local time zone is ahead of UTC.
        // For example: UTC - MSK = UTC - (UTC + 03:00) = -03:00 = -180
        UNIT_ASSERT_EQUAL(countOperationsToday(txn, {TaskOperation::Accept}, 1, -120), 3); // +02:00
        UNIT_ASSERT_EQUAL(countOperationsToday(txn, {TaskOperation::Accept}, 1,  -60), 3); // +01:00
        UNIT_ASSERT_EQUAL(countOperationsToday(txn, {TaskOperation::Accept}, 1,    0), 2); // UTC
        UNIT_ASSERT_EQUAL(countOperationsToday(txn, {TaskOperation::Accept}, 1,   60), 1); // -01:00
        UNIT_ASSERT_EQUAL(countOperationsToday(txn, {TaskOperation::Accept}, 1,  120), 0); // -02:00
    }

    Y_UNIT_TEST_F(shouldCountForUid, DbFixture)
    {
        pqxx::work txn(conn);

        createHistoryItem(txn, "CONCAT(CURRENT_DATE, ' 00:15 UTC')::timestamptz", 1, TaskOperation::Accept);
        createHistoryItem(txn, "CONCAT(CURRENT_DATE, ' 00:15 UTC')::timestamptz", 2, TaskOperation::Accept);
        createHistoryItem(txn, "CONCAT(CURRENT_DATE, ' 00:15 UTC')::timestamptz", 1, TaskOperation::Accept);

        UNIT_ASSERT_EQUAL(countOperationsToday(txn, {TaskOperation::Accept}, 1, 0), 2);
        UNIT_ASSERT_EQUAL(countOperationsToday(txn, {TaskOperation::Accept}, 2, 0), 1);
        UNIT_ASSERT_EQUAL(countOperationsToday(txn, {TaskOperation::Accept}, 3, 0), 0);
    }

    Y_UNIT_TEST_F(shouldCountPassedOperationsOnly, DbFixture)
    {
        pqxx::work txn(conn);

        createHistoryItem(txn, "CONCAT(CURRENT_DATE, ' 00:15 UTC')::timestamptz", 1, TaskOperation::Accept);
        createHistoryItem(txn, "CONCAT(CURRENT_DATE, ' 00:15 UTC')::timestamptz", 1, TaskOperation::ChangePosition);
        createHistoryItem(txn, "CONCAT(CURRENT_DATE, ' 00:15 UTC')::timestamptz", 1, TaskOperation::Accept);
        createHistoryItem(txn, "CONCAT(CURRENT_DATE, ' 00:15 UTC')::timestamptz", 1, TaskOperation::NeedInfo);
        createHistoryItem(txn, "CONCAT(CURRENT_DATE, ' 00:15 UTC')::timestamptz", 1, TaskOperation::Reject);

        UNIT_ASSERT_EQUAL(countOperationsToday(txn, {TaskOperation::Accept, TaskOperation::Reject}, 1, 0), 3);
        UNIT_ASSERT_EQUAL(countOperationsToday(txn, {TaskOperation::NeedInfo, TaskOperation::ChangePosition}, 1, 0), 2);
        UNIT_ASSERT_EQUAL(countOperationsToday(txn, {TaskOperation::ChangeType}, 1, 0), 0);
    }
} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::social::feedback::stats::tests

#include "helpers.h"
#include <maps/wikimap/mapspro/libs/social/suspicious_users.h>
#include <maps/wikimap/mapspro/libs/social/tasks/acquire.h>

#include <yandex/maps/wiki/social/gateway.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

#include <cstdint>
#include <string>

namespace maps::wiki::social::tests {

using namespace testing;


Y_UNIT_TEST_SUITE_F(suspiciousUsers, DbFixture) {
    Y_UNIT_TEST(shouldRemoveOldUsers) {
        applyQuery(
            "INSERT INTO suspicious_users VALUES "
            "(1, NOW() - interval '13 days'),"
            "(2, NOW() - interval '15 days'),"
            "(3, NOW() - interval '1 day'),"
            "(4, NOW() - interval '2 months')"
        );

        {
            pqxx::work txn(conn);
            Gateway(txn).removeOldSuspiciousUsers();
            txn.commit();
            EXPECT_THAT(suspiciousUsersIds(), ElementsAre(1, 3));
        }

        {
            pqxx::work txn(conn);
            Gateway(txn).removeOldSuspiciousUsers(chrono::Days(10));
            txn.commit();
            EXPECT_THAT(suspiciousUsersIds(), ElementsAre(3));
        }
    }

    Y_UNIT_TEST(shouldAddAndUpdateEntry) {
        applyQuery(
            "INSERT INTO suspicious_users VALUES "
            "(1, NOW() - interval '8 hours', NOW() - interval '7 hours', NOW() - interval '6 hours', 0),"
            "(3, NOW() - interval '8 hours', NOW() - interval '7 hours', NOW() - interval '10 sec', 10),"
            "(4, NOW() - interval '8 hours', NOW() - interval '7 hours', NOW() - interval '6 hours', 0),"
            "(5, NOW() - interval '8 hours', NOW() - interval '7 hours', NOW() - interval '6 hours', 0)"
        );

        auto registeredOrUnbannedAt =
            applyQuery(
                "SELECT registered_or_unbanned_at FROM suspicious_users LIMIT 1"
            )[0][0].as<std::string>();

        {   // Should update last commit time
            pqxx::work txn(conn);
            suspicious_users::onTaskCreated(txn, 1, registeredOrUnbannedAt);
            txn.commit();

            ASSERT_THAT(suspiciousUsersIds(), ElementsAre(1, 3, 4, 5));
            EXPECT_TRUE(execForSuspiciousUser<bool>(1, "NOW() - registered_or_unbanned_at <= interval '8 hours 5 sec'"));
            EXPECT_TRUE(execForSuspiciousUser<bool>(1, "first_commit_at < NOW() - interval '7 hours'"));
            EXPECT_TRUE(execForSuspiciousUser<bool>(1, "NOW() - last_commit_at < interval '1 sec'"));
            EXPECT_TRUE(execForSuspiciousUser<bool>(1, "changes_0_30_sec = 0"));
        }

        {   // Should add new entry
            pqxx::work txn(conn);
            suspicious_users::onTaskCreated(txn, 2, 8_hours_ago);
            txn.commit();

            ASSERT_THAT(suspiciousUsersIds(), ElementsAre(1, 2, 3, 4, 5));
            EXPECT_TRUE(execForSuspiciousUser<bool>(2, "NOW() - registered_or_unbanned_at <= interval '8 hours 5 sec'"));
            EXPECT_TRUE(execForSuspiciousUser<bool>(2, "NOW() - first_commit_at < interval '1 sec'"));
            EXPECT_TRUE(execForSuspiciousUser<bool>(2, "NOW() - last_commit_at < interval '1 sec'"));
            EXPECT_TRUE(execForSuspiciousUser<bool>(2, "changes_0_30_sec = 0"));
        }

        {   // Should update last commit time and statistics
            pqxx::work txn(conn);
            suspicious_users::onTaskCreated(txn, 3, registeredOrUnbannedAt);
            txn.commit();

            ASSERT_THAT(suspiciousUsersIds(), ElementsAre(1, 2, 3, 4, 5));
            EXPECT_TRUE(execForSuspiciousUser<bool>(3, "NOW() - registered_or_unbanned_at <= interval '8 hours 5 sec'"));
            EXPECT_TRUE(execForSuspiciousUser<bool>(3, "first_commit_at < NOW() - interval '7 hours'"));
            EXPECT_TRUE(execForSuspiciousUser<bool>(3, "NOW() - last_commit_at < interval '1 sec'"));
            EXPECT_TRUE(execForSuspiciousUser<bool>(3, "changes_0_30_sec = 11"));
        }

        {   // Should update unban and first_commit_at times
            pqxx::work txn(conn);
            suspicious_users::onTaskCreated(txn, 4, 2_hours_ago);
            txn.commit();

            ASSERT_THAT(suspiciousUsersIds(), ElementsAre(1, 2, 3, 4, 5));
            EXPECT_TRUE(execForSuspiciousUser<bool>(4, "NOW() - registered_or_unbanned_at <= interval '2 hours 5 sec'"));
            EXPECT_TRUE(execForSuspiciousUser<bool>(4, "NOW() - first_commit_at < interval '1 sec'"));
            EXPECT_TRUE(execForSuspiciousUser<bool>(4, "NOW() - last_commit_at < interval '1 sec'"));
            EXPECT_TRUE(execForSuspiciousUser<bool>(4, "changes_0_30_sec = 0"));
        }

        {   // Should not do any update (the user is not suspicious)
            pqxx::work txn(conn);
            suspicious_users::onTaskCreated(txn, 5, 25_hours_ago);
            txn.commit();

            ASSERT_THAT(suspiciousUsersIds(), ElementsAre(1, 2, 3, 4, 5));
            EXPECT_TRUE(execForSuspiciousUser<bool>(5, "registered_or_unbanned_at - (NOW() - interval '8 hours') < interval '1 sec'"));
            EXPECT_TRUE(execForSuspiciousUser<bool>(5, "first_commit_at - (NOW() - interval '7 hours') < interval '1 sec'"));
            EXPECT_TRUE(execForSuspiciousUser<bool>(5, "last_commit_at - (NOW() - interval '6 hours') < interval '1 sec'"));
            EXPECT_TRUE(execForSuspiciousUser<bool>(5, "changes_0_30_sec = 0"));
        }
    }

    Y_UNIT_TEST(shouldFilterSuspiciousUsers) {
        const TUid UID = 10;
        const size_t LIMIT = 10;

        // Remove unnecessary constraints
        applyQuery(
            "ALTER TABLE commit_event "
            "ALTER COLUMN branch_id SET DEFAULT 1,"
            "ALTER COLUMN action SET DEFAULT '',"
            "ALTER COLUMN bounds SET DEFAULT '';"
        );

        // Prepare data
        applyQuery(
            "INSERT INTO suspicious_users "
            "(uid, registered_or_unbanned_at, first_commit_at, changes_0_30_sec) VALUES "
            "(1, NOW() - interval '8 hours', NOW() - interval '7 hours 59 min 10 sec', 0)," // suspicious: < 1 minute since registration.
            "(2, NOW() - interval '8 hours', NOW() - interval '7 hours 58 min 50 sec', 0)," // non-suspicious: > 1 minute since registration.
            "(3, NOW() - interval '8 hours', NOW() - interval '6 hours', 1);" // suspicious: there are changes within 30 seconds timeframe.
            "INSERT INTO task "
            "(event_id, commit_id, created_at) VALUES "
            "(1, 1, now()), (2, 2, now()), (3, 3, now());"
            "INSERT INTO commit_event "
            "(event_id, created_by, commit_id, created_at) VALUES "
            "(1, 1, 1, now()), (2, 2, 2, now()), (3, 3, 3, now());"
        );

        { // Acquire all tasks
            pqxx::work txn(conn);
            auto tasks = tasks::acquire(
                txn, UID,
                EventFilter(),
                LIMIT,
                TasksOrder::NewestFirst,
                TEST_MODERATION_TIME_INTERVALS);
            EXPECT_THAT(getIds(tasks), ElementsAre(1, 2, 3));
        }

        { // Acquire suspicious tasks
            pqxx::work txn(conn);
            auto tasks = tasks::acquire(
                txn, UID,
                EventFilter().suspiciousUsers(true),
                LIMIT,
                TasksOrder::NewestFirst,
                TEST_MODERATION_TIME_INTERVALS);
            EXPECT_THAT(getIds(tasks), ElementsAre(1, 3));
        }

        { // Acquire non-suspicious tasks
            pqxx::work txn(conn);
            auto tasks = tasks::acquire(
                txn, UID,
                EventFilter().suspiciousUsers(false),
                LIMIT,
                TasksOrder::NewestFirst,
                TEST_MODERATION_TIME_INTERVALS);
            EXPECT_THAT(getIds(tasks), ElementsAre(2));
        }
    }
}

} // namespace maps::wiki::social::tests

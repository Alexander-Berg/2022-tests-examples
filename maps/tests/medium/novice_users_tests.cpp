#include <maps/wikimap/mapspro/libs/social/tasks/acquire.h>
#include <maps/wikimap/mapspro/libs/social/tasks/create.h>
#include <maps/wikimap/mapspro/libs/social/tasks/release.h>

#include <yandex/maps/wiki/social/gateway.h>

#include <maps/wikimap/mapspro/libs/social/tests/helpers/event_creator.h>
#include "helpers.h"

#include <maps/libs/chrono/include/time_point.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

#include <string>
#include <vector>

namespace maps::wiki::social::tests {

using namespace testing;

namespace {

const auto USER_CREATED_OR_UNBANNED_AT_LONG_AGO =
    chrono::formatSqlDateTime(
        std::chrono::system_clock::now() - std::chrono::hours(72)
    );
const auto USER_CREATED_OR_UNBANNED_AT_RECENTLY =
    chrono::formatSqlDateTime(
        std::chrono::system_clock::now() - std::chrono::hours(2)
    );


bool isCreatedByNovice(pqxx::transaction_base& txn, TId eventId)
{
    return txn.exec(
        "SELECT is_created_by_novice "
        "FROM social.task "
        "WHERE event_id = " + std::to_string(eventId)
    )[0][0].as<bool>(false);
}


auto
createTasksForUsers(pqxx::transaction_base& txn, std::set<TUid> uids)
{
    std::map<TUid, TId> result;

    for (const auto uid: uids) {
        const auto task = tasks::create(
            txn,
            EventCreator(txn).uid(uid),
            USER_CREATED_OR_UNBANNED_AT_LONG_AGO,
            tasks::Accepted::No
        );
        result.emplace(uid, task.event().id());
    }

    return result;
}

} // namespace


Y_UNIT_TEST_SUITE_F(noviceUsers, DbFixture) {
    Y_UNIT_TEST(shouldCreateTasksWithNoviceMarkers) {
        {   // Should sum categories
            pqxx::work txn(conn);
            txn.exec(
                "INSERT INTO social.skills "
                "(uid, category_id, resolve_resolution, amount) VALUES "
                "(1, 'bld', 'accept', 7),"
                "(1, 'rd', 'accept', 7)," // user 1 made 14 changes
                "(2, 'bld', 'accept', 8),"
                "(2, 'rd', 'accept', 8);" // whereas user 2 made 16

                "INSERT INTO social.stats "
                "(uid, first_commit_at) VALUES "
                "(1, NOW() - '1 year'::interval),"
                "(2, NOW() - '1 year'::interval);"
            );

            auto uidToEventId = createTasksForUsers(txn, {1, 2});
            EXPECT_TRUE(isCreatedByNovice(txn, uidToEventId.at(1)));
            EXPECT_FALSE(isCreatedByNovice(txn, uidToEventId.at(2)));
        }

        {   // Should count accepted only
            pqxx::work txn(conn);
            txn.exec(
                "INSERT INTO social.skills "
                "(uid, category_id, resolve_resolution, amount) VALUES "
                "(1, 'bld', 'edit', 5),"
                "(2, 'bld', 'edit', 50),"
                "(3, 'bld', 'accept', 5),"
                "(4, 'bld', 'accept', 50),"
                "(5, 'bld', 'revert', 5),"
                "(6, 'bld', 'revert', 50);"

                "INSERT INTO social.stats "
                "(uid, first_commit_at) VALUES "
                "(1, NOW() - '1 year'::interval),"
                "(2, NOW() - '1 year'::interval),"
                "(3, NOW() - '1 year'::interval),"
                "(4, NOW() - '1 year'::interval),"
                "(5, NOW() - '1 year'::interval),"
                "(6, NOW() - '1 year'::interval);"
            );

            auto uidToEventId = createTasksForUsers(txn, {1, 2, 3, 4, 5, 6});
            EXPECT_TRUE(isCreatedByNovice(txn, uidToEventId.at(1)));
            EXPECT_TRUE(isCreatedByNovice(txn, uidToEventId.at(2)));
            EXPECT_TRUE(isCreatedByNovice(txn, uidToEventId.at(3)));
            EXPECT_FALSE(isCreatedByNovice(txn, uidToEventId.at(4)));
            EXPECT_TRUE(isCreatedByNovice(txn, uidToEventId.at(5)));
            EXPECT_TRUE(isCreatedByNovice(txn, uidToEventId.at(6)));
        }

        {   // Should count users with first change made less than 24 hours ago
            pqxx::work txn(conn);
            txn.exec(
                "INSERT INTO social.skills "
                "(uid, category_id, resolve_resolution, amount) VALUES "
                "(1, 'bld', 'accept', 50),"
                "(2, 'bld', 'accept', 50),"
                "(3, 'bld', 'accept', 50),"
                "(4, 'bld', 'accept', 50);"

                "INSERT INTO social.stats "
                "(uid, first_commit_at) VALUES "
                // No entry for the user 1, it's a novice that has not been yet
                // processed by the service 'social'.
                "(2, NULL)," // NULL - the user is not novice (first commit was created before this mechanism introduction)
                "(3, NOW() - '1 hour'::interval),"
                "(4, NOW() - '1 year'::interval);"
            );

            auto uidToEventId = createTasksForUsers(txn, {1, 2, 3, 4});
            EXPECT_TRUE(isCreatedByNovice(txn, uidToEventId.at(1)));
            EXPECT_FALSE(isCreatedByNovice(txn, uidToEventId.at(2)));
            EXPECT_TRUE(isCreatedByNovice(txn, uidToEventId.at(3)));
            EXPECT_FALSE(isCreatedByNovice(txn, uidToEventId.at(4)));
        }

        {   // Should count recently unbanned
            pqxx::work txn(conn);
            txn.exec(
                "INSERT INTO social.skills "
                "(uid, category_id, resolve_resolution, amount) VALUES "
                "(1, 'bld', 'accept', 50),"
                "(2, 'bld', 'accept', 50);"

                "INSERT INTO social.stats "
                "(uid, first_commit_at) VALUES "
                "(1, NOW() - '1 year'::interval),"
                "(2, NOW() - '1 year'::interval);"
            );

            const auto novice_task = tasks::create(
                txn,
                EventCreator(txn).uid(1),
                USER_CREATED_OR_UNBANNED_AT_RECENTLY,
                tasks::Accepted::No
            );

            const auto non_novice_task = tasks::create(
                txn,
                EventCreator(txn).uid(1),
                USER_CREATED_OR_UNBANNED_AT_LONG_AGO,
                tasks::Accepted::No
            );

            EXPECT_TRUE(isCreatedByNovice(txn, novice_task.event().id()));
            EXPECT_FALSE(isCreatedByNovice(txn, non_novice_task.event().id()));
        }
    }

    Y_UNIT_TEST(shouldFilterNoviceTasks) {
        const TUid MODERATOR_UID = 100;
        const size_t LIMIT = 10;

        using namespace unittest;

        pqxx::work txn(conn);
        txn.exec(
            "INSERT INTO social.skills "
            "(uid, category_id, resolve_resolution, amount) VALUES "
            "(1, 'bld', 'accept', 100);"

            "INSERT INTO social.stats "
            "(uid, first_commit_at) VALUES "
            "(1, NOW() - '1 year'::interval);"
        );

        auto uidToEventId = createTasksForUsers(txn, {1, 2});
        EXPECT_FALSE(isCreatedByNovice(txn, uidToEventId.at(1)));
        EXPECT_TRUE(isCreatedByNovice(txn, uidToEventId.at(2)));

        const auto novices_tasks = tasks::acquire(
            txn, MODERATOR_UID,
            EventFilter().noviceUsers(true),
            LIMIT,
            TasksOrder::NewestFirst,
            TEST_MODERATION_TIME_INTERVALS);
        EXPECT_THAT(getIds(novices_tasks), ElementsAre(2));
        tasks::release(txn, MODERATOR_UID);

        const auto non_novices_tasks = tasks::acquire(
            txn, MODERATOR_UID,
            EventFilter().noviceUsers(false),
            LIMIT,
            TasksOrder::NewestFirst,
            TEST_MODERATION_TIME_INTERVALS);
        EXPECT_THAT(getIds(non_novices_tasks), ElementsAre(1));
        tasks::release(txn, MODERATOR_UID);

        const auto all_tasks = tasks::acquire(
            txn, MODERATOR_UID,
            EventFilter(),
            LIMIT,
            TasksOrder::NewestFirst,
            TEST_MODERATION_TIME_INTERVALS);
        EXPECT_THAT(getIds(all_tasks), ElementsAre(1, 2));
        tasks::release(txn, MODERATOR_UID);
    }
}

} // maps::wiki::social::tests

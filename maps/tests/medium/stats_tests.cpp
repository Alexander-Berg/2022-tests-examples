#include <maps/wikimap/mapspro/libs/social/include/yandex/maps/wiki/social/gateway.h>
#include <maps/wikimap/mapspro/libs/social/tests/helpers/task_creator.h>
#include <maps/wikimap/mapspro/libs/social/tests/helpers/event_creator.h>
#include <maps/wikimap/mapspro/libs/unittest/include/yandex/maps/wiki/unittest/unittest.h>
#include "helpers.h"

#include <library/cpp/testing/unittest/registar.h>

#include <chrono>


namespace maps::wiki::social::tests {

using namespace std::chrono_literals;
using unittest::txnNow;

Y_UNIT_TEST_SUITE(stats_tests) {
    Y_UNIT_TEST_F(should_filter_active_task_stats_by_categories, DbFixture)
    {
        pqxx::work txn(conn);
        Gateway gateway(txn);

        TaskCreator(txn).event(EventCreator(txn).aoiIds({1}).primaryObjData({10, "bld",  "label", "notes"})).createdAt("NOW() - '5 days'::interval")();
        TaskCreator(txn).event(EventCreator(txn).aoiIds({1}).primaryObjData({11, "rd",   "label", "notes"})).createdAt("NOW() - '5 days'::interval")();
        TaskCreator(txn).event(EventCreator(txn).aoiIds({1}).primaryObjData({12, "cond", "label", "notes"})).createdAt("NOW() - '5 days'::interval")();

        auto activeStats = gateway
            .taskStatsConsole(ModerationMode::Moderator)
            .setFilterByCategories({"bld", "cond"})
            .activeTaskStatsByAoi(TEST_MODERATION_TIME_INTERVALS);
        UNIT_ASSERT(activeStats.count(1));
        UNIT_ASSERT_EQUAL(activeStats.at(1).count, 2);

        activeStats = gateway
            .taskStatsConsole(ModerationMode::SuperModerator)
            .setFilterByCategories({"bld", "rd"})
            .activeTaskStatsByAoi(TEST_MODERATION_TIME_INTERVALS);
        UNIT_ASSERT_EQUAL(activeStats.size(), 1);
        UNIT_ASSERT(activeStats.count(1));
        UNIT_ASSERT_EQUAL(activeStats.at(1).count, 2);

        activeStats = gateway
            .taskStatsConsole(ModerationMode::Supervisor)
            .setFilterByCategories({"rd", "cond"})
            .activeTaskStatsByAoi(TEST_MODERATION_TIME_INTERVALS);
        UNIT_ASSERT_EQUAL(activeStats.size(), 1);
        UNIT_ASSERT(activeStats.count(1));
        UNIT_ASSERT_EQUAL(activeStats.at(1).count, 2);
    }

    Y_UNIT_TEST_F(should_filter_active_task_stats_by_event_type, DbFixture)
    {
        pqxx::work txn(conn);
        Gateway gateway(txn);

        TaskCreator(txn).event(EventCreator(txn).aoiIds({1}).type(EventType::Edit              )).createdAt("NOW() - '5 days'::interval")();
        TaskCreator(txn).event(EventCreator(txn).aoiIds({1}).type(EventType::Complaint         )).createdAt("NOW() - '5 days'::interval")();
        TaskCreator(txn).event(EventCreator(txn).aoiIds({1}).type(EventType::RequestForDeletion)).createdAt("NOW() - '5 days'::interval")();
        TaskCreator(txn).event(EventCreator(txn).aoiIds({1}).type(EventType::Edit              )).createdAt("NOW() - '5 days'::interval")();

        auto activeStats = gateway
            .taskStatsConsole(ModerationMode::Moderator)
            .setFilterByEventType(EventType::Edit)
            .activeTaskStatsByAoi(TEST_MODERATION_TIME_INTERVALS);
        UNIT_ASSERT_EQUAL(activeStats.size(), 1);
        UNIT_ASSERT(activeStats.count(1));
        UNIT_ASSERT_EQUAL(activeStats.at(1).count, 2);

        activeStats = gateway
            .taskStatsConsole(ModerationMode::SuperModerator)
            .setFilterByEventType(EventType::Complaint)
            .activeTaskStatsByAoi(TEST_MODERATION_TIME_INTERVALS);
        UNIT_ASSERT_EQUAL(activeStats.size(), 1);
        UNIT_ASSERT(activeStats.count(1));
        UNIT_ASSERT_EQUAL(activeStats.at(1).count, 1);

        activeStats = gateway
            .taskStatsConsole(ModerationMode::Supervisor)
            .setFilterByEventType(EventType::RequestForDeletion)
            .activeTaskStatsByAoi(TEST_MODERATION_TIME_INTERVALS);
        UNIT_ASSERT_EQUAL(activeStats.size(), 1);
        UNIT_ASSERT(activeStats.count(1));
        UNIT_ASSERT_EQUAL(activeStats.at(1).count, 1);
    }

    Y_UNIT_TEST_F(should_group_active_task_stats_by_aoi, DbFixture)
    {
        pqxx::work txn(conn);
        Gateway gateway(txn);

        TaskCreator(txn).event(EventCreator(txn).aoiIds({1, 2, 3})).createdAt("NOW() - '5 days'::interval")();
        TaskCreator(txn).event(EventCreator(txn).aoiIds({1, 2   })).createdAt("NOW() - '5 days'::interval")();
        TaskCreator(txn).event(EventCreator(txn).aoiIds({   2   })).createdAt("NOW() - '5 days'::interval")();

        auto activeStats = gateway
            .taskStatsConsole(ModerationMode::Moderator)
            .activeTaskStatsByAoi(TEST_MODERATION_TIME_INTERVALS);
        UNIT_ASSERT_EQUAL(activeStats.size(), 3);
        UNIT_ASSERT(activeStats.count(1));
        UNIT_ASSERT_EQUAL(activeStats.at(1).count, 2);
        UNIT_ASSERT(activeStats.count(2));
        UNIT_ASSERT_EQUAL(activeStats.at(2).count, 3);
        UNIT_ASSERT(activeStats.count(3));
        UNIT_ASSERT_EQUAL(activeStats.at(3).count, 1);

        activeStats = gateway
            .taskStatsConsole(ModerationMode::SuperModerator)
            .activeTaskStatsByAoi(TEST_MODERATION_TIME_INTERVALS);
        UNIT_ASSERT_EQUAL(activeStats.size(), 3);
        UNIT_ASSERT(activeStats.count(1));
        UNIT_ASSERT_EQUAL(activeStats.at(1).count, 2);
        UNIT_ASSERT(activeStats.count(2));
        UNIT_ASSERT_EQUAL(activeStats.at(2).count, 3);
        UNIT_ASSERT(activeStats.count(3));
        UNIT_ASSERT_EQUAL(activeStats.at(3).count, 1);

        activeStats = gateway
            .taskStatsConsole(ModerationMode::Supervisor)
            .activeTaskStatsByAoi(TEST_MODERATION_TIME_INTERVALS);
        UNIT_ASSERT_EQUAL(activeStats.size(), 3);
        UNIT_ASSERT(activeStats.count(1));
        UNIT_ASSERT_EQUAL(activeStats.at(1).count, 2);
        UNIT_ASSERT(activeStats.count(2));
        UNIT_ASSERT_EQUAL(activeStats.at(2).count, 3);
        UNIT_ASSERT(activeStats.count(3));
        UNIT_ASSERT_EQUAL(activeStats.at(3).count, 1);
    }

    Y_UNIT_TEST_F(should_get_oldest_active_task, DbFixture)
    {
        pqxx::work txn(conn);
        Gateway gateway(txn);
        const auto NOW = txnNow(txn);

        // Unresolved tasks (all)
        TaskCreator(txn).event(EventCreator(txn).aoiIds({1   })).createdAt("NOW() - '107 hours'::interval")();
        TaskCreator(txn).event(EventCreator(txn).aoiIds({1, 2})).createdAt("NOW() - '106 hours'::interval")();
        TaskCreator(txn).event(EventCreator(txn).aoiIds({   2})).createdAt("NOW() - '105 hours'::interval")();

        // Resolved tasks (supermoderator and supervisor)
        TaskCreator(txn)
            .event(EventCreator(txn).aoiIds({1}))
            .createdAt("NOW() - '104 hours'::interval")
            .resolved(1, ResolveResolution::Accept, "NOW() - '103 hours'::interval")();
        TaskCreator(txn)
            .event(EventCreator(txn).aoiIds({1, 2}))
            .createdAt("NOW() - '102 hours'::interval")
            .resolved(1, ResolveResolution::Accept, "NOW() - '101 hours':: interval")();

        auto activeStats = gateway
            .taskStatsConsole(ModerationMode::Moderator)
            .activeTaskStatsByAoi(TEST_MODERATION_TIME_INTERVALS);
        UNIT_ASSERT_EQUAL(
            chrono::parseSqlDateTime(activeStats.at(1).oldestTaskActiveSince),
            NOW - 107h
        );
        UNIT_ASSERT_EQUAL(
            chrono::parseSqlDateTime(activeStats.at(2).oldestTaskActiveSince),
            NOW - 106h
        );

        activeStats = gateway
            .taskStatsConsole(ModerationMode::SuperModerator)
            .activeTaskStatsByAoi(TEST_MODERATION_TIME_INTERVALS);
        UNIT_ASSERT_EQUAL(
            chrono::parseSqlDateTime(activeStats.at(1).oldestTaskActiveSince),
            NOW - 103h + 5min
        );
        UNIT_ASSERT_EQUAL(
            chrono::parseSqlDateTime(activeStats.at(2).oldestTaskActiveSince),
            NOW - 101h + 5min
        );

        activeStats = gateway
            .taskStatsConsole(ModerationMode::Supervisor)
            .activeTaskStatsByAoi(TEST_MODERATION_TIME_INTERVALS);
        UNIT_ASSERT_EQUAL(
            chrono::parseSqlDateTime(activeStats.at(1).oldestTaskActiveSince),
            NOW - 103h + 5min
        );
        UNIT_ASSERT_EQUAL(
            chrono::parseSqlDateTime(activeStats.at(2).oldestTaskActiveSince),
            NOW - 101h + 5min
        );
    }

    Y_UNIT_TEST_F(should_get_recent_new_tasks, DbFixture)
    {
        pqxx::work txn(conn);
        Gateway gateway(txn);

        TaskCreator(txn)
            .event(EventCreator(txn).aoiIds({1}))
            .createdAt("NOW() - '12 hours'::interval")
            .resolved(1, ResolveResolution::Accept, "NOW()")();
        TaskCreator(txn)
            .event(EventCreator(txn).aoiIds({1}))
            .createdAt("NOW() - '36 hours'::interval")
            .resolved(1, ResolveResolution::Accept, "NOW() - '1 hour'::interval")();
        TaskCreator(txn)
            .event(EventCreator(txn).aoiIds({1}))
            .createdAt("NOW() - '36 hours'::interval")
            .resolved(1, ResolveResolution::Accept, "NOW() - '32 hours'::interval")();

        // created_at > now - day
        auto aoiToRecentNewTaskCounts = gateway
            .taskStatsConsole(ModerationMode::Moderator)
            .recentNewTaskCountsByAoi(TEST_MODERATION_TIME_INTERVALS);
        UNIT_ASSERT_EQUAL(aoiToRecentNewTaskCounts.size(), 1);
        UNIT_ASSERT(aoiToRecentNewTaskCounts.count(1));
        UNIT_ASSERT_EQUAL(aoiToRecentNewTaskCounts.at(1), 1);

        // supermoderator and supervisor
        // created_at > now - delay(1 day) - day
        // created_at < now - delay(1 day)
        // or
        // resolved_at > now - delay(5 min) - day
        // resolved_at < now - delay(5 min)
        aoiToRecentNewTaskCounts = gateway
            .taskStatsConsole(ModerationMode::SuperModerator)
            .recentNewTaskCountsByAoi(TEST_MODERATION_TIME_INTERVALS);
        UNIT_ASSERT_EQUAL(aoiToRecentNewTaskCounts.size(), 1);
        UNIT_ASSERT(aoiToRecentNewTaskCounts.count(1));
        UNIT_ASSERT_EQUAL(aoiToRecentNewTaskCounts.at(1), 2);

        aoiToRecentNewTaskCounts = gateway
            .taskStatsConsole(ModerationMode::Supervisor)
            .recentNewTaskCountsByAoi(TEST_MODERATION_TIME_INTERVALS);
        UNIT_ASSERT_EQUAL(aoiToRecentNewTaskCounts.size(), 1);
        UNIT_ASSERT(aoiToRecentNewTaskCounts.count(1));
        UNIT_ASSERT_EQUAL(aoiToRecentNewTaskCounts.at(1), 2);
    }

    Y_UNIT_TEST_F(should_filter_recent_new_tasks_by_categories, DbFixture)
    {
        pqxx::work txn(conn);
        Gateway gateway(txn);

        TaskCreator(txn)
            .event(EventCreator(txn).aoiIds({1}).primaryObjData({10, "bld", "label", "notes"}))
            .createdAt("NOW() - '12 hours'::interval")();
        TaskCreator(txn)
            .event(EventCreator(txn).aoiIds({1}).primaryObjData({10, "cond", "label", "notes"}))
            .createdAt("NOW() - '12 hours'::interval")();
        TaskCreator(txn)
            .event(EventCreator(txn).aoiIds({1}).primaryObjData({10, "bld", "label", "notes"}))
            .createdAt("NOW() - '36 hours'::interval")();
        TaskCreator(txn)
            .event(EventCreator(txn).aoiIds({1}).primaryObjData({10, "cond", "label", "notes"}))
            .createdAt("NOW() - '36 hours'::interval")();

        // created_at > now - day
        auto aoiToRecentNewTaskCounts = gateway
            .taskStatsConsole(ModerationMode::Moderator)
            .setFilterByCategories({"bld"})
            .recentNewTaskCountsByAoi(TEST_MODERATION_TIME_INTERVALS);
        UNIT_ASSERT_EQUAL(aoiToRecentNewTaskCounts.size(), 1);
        UNIT_ASSERT(aoiToRecentNewTaskCounts.count(1));
        UNIT_ASSERT_EQUAL(aoiToRecentNewTaskCounts.at(1), 1);

        // supermoderator and supervisor
        // created_at > now - delay(1 day) - day
        // created_at < now - delay(1 day)
        // or
        // resolved_at > now - delay(5 min) - day
        // resolved_at < now - delay(5 min)
        aoiToRecentNewTaskCounts = gateway
            .taskStatsConsole(ModerationMode::SuperModerator)
            .setFilterByCategories({"cond"})
            .recentNewTaskCountsByAoi(TEST_MODERATION_TIME_INTERVALS);
        UNIT_ASSERT_EQUAL(aoiToRecentNewTaskCounts.size(), 1);
        UNIT_ASSERT(aoiToRecentNewTaskCounts.count(1));
        UNIT_ASSERT_EQUAL(aoiToRecentNewTaskCounts.at(1), 1);

        aoiToRecentNewTaskCounts = gateway
            .taskStatsConsole(ModerationMode::Supervisor)
            .setFilterByCategories({"bld"})
            .recentNewTaskCountsByAoi(TEST_MODERATION_TIME_INTERVALS);
        UNIT_ASSERT_EQUAL(aoiToRecentNewTaskCounts.size(), 1);
        UNIT_ASSERT(aoiToRecentNewTaskCounts.count(1));
        UNIT_ASSERT_EQUAL(aoiToRecentNewTaskCounts.at(1), 1);
    }

    Y_UNIT_TEST_F(should_filter_recent_new_tasks_by_event_type, DbFixture)
    {
        pqxx::work txn(conn);
        Gateway gateway(txn);

        TaskCreator(txn)
            .event(EventCreator(txn).aoiIds({1}).type(EventType::Edit))
            .createdAt("NOW() - '12 hours'::interval")();
        TaskCreator(txn)
            .event(EventCreator(txn).aoiIds({1}).type(EventType::Complaint))
            .createdAt("NOW() - '12 hours'::interval")();
        TaskCreator(txn)
            .event(EventCreator(txn).aoiIds({1}).type(EventType::Edit))
            .createdAt("NOW() - '36 hours'::interval")();
        TaskCreator(txn)
            .event(EventCreator(txn).aoiIds({1}).type(EventType::Complaint))
            .createdAt("NOW() - '36 hours'::interval")();

        // created_at > now - day
        auto aoiToRecentNewTaskCounts = gateway
            .taskStatsConsole(ModerationMode::Moderator)
            .setFilterByEventType(EventType::Edit)
            .recentNewTaskCountsByAoi(TEST_MODERATION_TIME_INTERVALS);
        UNIT_ASSERT_EQUAL(aoiToRecentNewTaskCounts.size(), 1);
        UNIT_ASSERT(aoiToRecentNewTaskCounts.count(1));
        UNIT_ASSERT_EQUAL(aoiToRecentNewTaskCounts.at(1), 1);

        // supermoderator and supervisor
        // created_at > now - delay(1 day) - day
        // created_at < now - delay(1 day)
        // or
        // resolved_at > now - delay(5 min) - day
        // resolved_at < now - delay(5 min)
        aoiToRecentNewTaskCounts = gateway
            .taskStatsConsole(ModerationMode::SuperModerator)
            .setFilterByEventType(EventType::Complaint)
            .recentNewTaskCountsByAoi(TEST_MODERATION_TIME_INTERVALS);
        UNIT_ASSERT_EQUAL(aoiToRecentNewTaskCounts.size(), 1);
        UNIT_ASSERT(aoiToRecentNewTaskCounts.count(1));
        UNIT_ASSERT_EQUAL(aoiToRecentNewTaskCounts.at(1), 1);

        aoiToRecentNewTaskCounts = gateway
            .taskStatsConsole(ModerationMode::Supervisor)
            .setFilterByEventType(EventType::Edit)
            .recentNewTaskCountsByAoi(TEST_MODERATION_TIME_INTERVALS);
        UNIT_ASSERT_EQUAL(aoiToRecentNewTaskCounts.size(), 1);
        UNIT_ASSERT(aoiToRecentNewTaskCounts.count(1));
        UNIT_ASSERT_EQUAL(aoiToRecentNewTaskCounts.at(1), 1);
    }

    Y_UNIT_TEST_F(should_group_recent_new_tasks_by_aoi, DbFixture)
    {
        pqxx::work txn(conn);
        Gateway gateway(txn);

        TaskCreator(txn)
            .event(EventCreator(txn).aoiIds({1}))
            .createdAt("NOW() - '12 hours'::interval")();
        TaskCreator(txn)
            .event(EventCreator(txn).aoiIds({1, 2}))
            .createdAt("NOW() - '12 hours'::interval")
            .resolved(1, ResolveResolution::Accept, "NOW() - '1 hour'::interval")();
        TaskCreator(txn)
            .event(EventCreator(txn).aoiIds({3, 4}))
            .createdAt("NOW() - '36 hours'::interval")();
        TaskCreator(txn)
            .event(EventCreator(txn).aoiIds({3}))
            .createdAt("NOW() - '36 hours'::interval")
            .resolved(1, ResolveResolution::Accept, "NOW() - '1 hour'::interval")();

        // created_at > now - day
        auto aoiToRecentNewTaskCounts = gateway
            .taskStatsConsole(ModerationMode::Moderator)
            .recentNewTaskCountsByAoi(TEST_MODERATION_TIME_INTERVALS);
        // Check against value?
        UNIT_ASSERT_EQUAL(aoiToRecentNewTaskCounts.size(), 2);
        UNIT_ASSERT(aoiToRecentNewTaskCounts.count(1));
        UNIT_ASSERT_EQUAL(aoiToRecentNewTaskCounts.at(1), 2);
        UNIT_ASSERT(aoiToRecentNewTaskCounts.count(2));
        UNIT_ASSERT_EQUAL(aoiToRecentNewTaskCounts.at(2), 1);

        // supermoderator and supervisor
        // created_at > now - delay(1 day) - day
        // created_at < now - delay(1 day)
        // or
        // resolved_at > now - delay(5 min) - day
        // resolved_at < now - delay(5 min)
        aoiToRecentNewTaskCounts = gateway
            .taskStatsConsole(ModerationMode::SuperModerator)
            .recentNewTaskCountsByAoi(TEST_MODERATION_TIME_INTERVALS);
        UNIT_ASSERT_EQUAL(aoiToRecentNewTaskCounts.size(), 4);
        UNIT_ASSERT(aoiToRecentNewTaskCounts.count(1));
        UNIT_ASSERT_EQUAL(aoiToRecentNewTaskCounts.at(1), 1);
        UNIT_ASSERT(aoiToRecentNewTaskCounts.count(2));
        UNIT_ASSERT_EQUAL(aoiToRecentNewTaskCounts.at(2), 1);
        UNIT_ASSERT(aoiToRecentNewTaskCounts.count(3));
        UNIT_ASSERT_EQUAL(aoiToRecentNewTaskCounts.at(3), 2);
        UNIT_ASSERT(aoiToRecentNewTaskCounts.count(4));
        UNIT_ASSERT_EQUAL(aoiToRecentNewTaskCounts.at(4), 1);

        aoiToRecentNewTaskCounts = gateway
            .taskStatsConsole(ModerationMode::Supervisor)
            .recentNewTaskCountsByAoi(TEST_MODERATION_TIME_INTERVALS);
        UNIT_ASSERT_EQUAL(aoiToRecentNewTaskCounts.size(), 4);
        UNIT_ASSERT(aoiToRecentNewTaskCounts.count(1));
        UNIT_ASSERT_EQUAL(aoiToRecentNewTaskCounts.at(1), 1);
        UNIT_ASSERT(aoiToRecentNewTaskCounts.count(2));
        UNIT_ASSERT_EQUAL(aoiToRecentNewTaskCounts.at(2), 1);
        UNIT_ASSERT(aoiToRecentNewTaskCounts.count(3));
        UNIT_ASSERT_EQUAL(aoiToRecentNewTaskCounts.at(3), 2);
        UNIT_ASSERT(aoiToRecentNewTaskCounts.count(4));
        UNIT_ASSERT_EQUAL(aoiToRecentNewTaskCounts.at(4), 1);
    }

    Y_UNIT_TEST_F(should_get_recent_processed_tasks, DbFixture)
    {
        pqxx::work txn(conn);
        Gateway gateway(txn);

        // Moderator
        //             now - day           now - 5 min   now
        // ----------------+-------------------+----------+------->
        //                  ######### resolved at #########
        //
        // Supermoderator and supervisor
        //             now - day           now - 5 min   now
        // ----------------+-------------------+----------+------->
        // ## created at ## ######### resolved at #########
        // or
        //                  ########## closed at ##########
        // ############ resolved at ###########

        // Not a recent processed task
        TaskCreator(txn)
            .event(EventCreator(txn).aoiIds({1}))
            .createdAt("NOW() - '1 day 3 hours'::interval")
            .resolved(1, ResolveResolution::Accept, "NOW() - '1 day 2 hours'::interval")
            .closed(1, CloseResolution::Approve, "NOW() - '1 day 1 hour'::interval")();
        // Moderator, Supermoderator, Supervisor
        TaskCreator(txn)
            .event(EventCreator(txn).aoiIds({1}))
            .createdAt("NOW() - '1 day 3 hours'::interval")
            .resolved(1, ResolveResolution::Accept, "NOW() - '2 hour'::interval")
            .closed(1, CloseResolution::Approve, "NOW() - '1 hour'::interval")();
        // Moderator, Supermoderator, Supervisor
        TaskCreator(txn)
            .event(EventCreator(txn).aoiIds({1}))
            .createdAt("NOW() - '3 hours'::interval")
            .resolved(1, ResolveResolution::Accept, "NOW() - '2 hour'::interval")
            .closed(1, CloseResolution::Approve, "NOW()")();
        // Moderator, Supermoderator, Supervisor
        TaskCreator(txn)
            .event(EventCreator(txn).aoiIds({1}))
            .createdAt("NOW() - '1 day 3 hours'::interval")
            .resolved(1, ResolveResolution::Accept, "NOW()")
            .closed(1, CloseResolution::Approve, "NOW()")();

        // resolved_at > now - day
        auto aoiToRecentProcessedTasksCounts = gateway
            .taskStatsConsole(ModerationMode::Moderator)
            .recentProcessedTaskCountsByAoi(TEST_MODERATION_TIME_INTERVALS);
        UNIT_ASSERT_EQUAL(aoiToRecentProcessedTasksCounts.size(), 1);
        UNIT_ASSERT(aoiToRecentProcessedTasksCounts.count(1));
        UNIT_ASSERT_EQUAL(aoiToRecentProcessedTasksCounts.at(1), 3);

        // resolved_at > now - day
        // created_at  < now - delay(1 day)
        aoiToRecentProcessedTasksCounts = gateway
            .taskStatsConsole(ModerationMode::SuperModerator)
            .recentProcessedTaskCountsByAoi(TEST_MODERATION_TIME_INTERVALS);
        UNIT_ASSERT_EQUAL(aoiToRecentProcessedTasksCounts.size(), 1);
        UNIT_ASSERT(aoiToRecentProcessedTasksCounts.count(1));
        UNIT_ASSERT_EQUAL(aoiToRecentProcessedTasksCounts.at(1), 3);

        // closed_at   > now - day
        // resolved_at < now - delay(5 min)
        aoiToRecentProcessedTasksCounts = gateway
            .taskStatsConsole(ModerationMode::Supervisor)
            .recentProcessedTaskCountsByAoi(TEST_MODERATION_TIME_INTERVALS);
        UNIT_ASSERT_EQUAL(aoiToRecentProcessedTasksCounts.size(), 1);
        UNIT_ASSERT(aoiToRecentProcessedTasksCounts.count(1));
        UNIT_ASSERT_EQUAL(aoiToRecentProcessedTasksCounts.at(1), 3);
    }

    Y_UNIT_TEST_F(should_filter_recent_processed_tasks_by_categories, DbFixture)
    {
        {
            pqxx::work txn(conn);

            TaskCreator(txn)
                .event(EventCreator(txn).aoiIds({1}).primaryObjData({10, "bld", "label", "notes"}))
                .resolved(1, ResolveResolution::Accept)();
            TaskCreator(txn)
                .event(EventCreator(txn).aoiIds({1}).primaryObjData({10, "rd", "label", "notes"}))
                .resolved(1, ResolveResolution::Accept)();

            const auto aoiToRecentProcessedTasksCounts = Gateway(txn)
                .taskStatsConsole(ModerationMode::Moderator)
                .setFilterByCategories({"bld"})
                .recentProcessedTaskCountsByAoi(TEST_MODERATION_TIME_INTERVALS);
            UNIT_ASSERT_EQUAL(aoiToRecentProcessedTasksCounts.size(), 1);
            UNIT_ASSERT(aoiToRecentProcessedTasksCounts.count(1));
            UNIT_ASSERT_EQUAL(aoiToRecentProcessedTasksCounts.at(1), 1);
        }

        {
            pqxx::work txn(conn);

            TaskCreator(txn)
                .event(EventCreator(txn).aoiIds({1}).primaryObjData({10, "bld", "label", "notes"}))
                .createdAt("NOW() - '1 day 3 hours'::interval")
                .resolved(1, ResolveResolution::Accept, "NOW()")();
            TaskCreator(txn)
                .event(EventCreator(txn).aoiIds({1}).primaryObjData({10, "rd", "label", "notes"}))
                .createdAt("NOW() - '1 day 3 hours'::interval")
                .resolved(1, ResolveResolution::Accept, "NOW()")();

            const auto aoiToRecentProcessedTasksCounts = Gateway(txn)
                .taskStatsConsole(ModerationMode::SuperModerator)
                .setFilterByCategories({"rd"})
                .recentProcessedTaskCountsByAoi(TEST_MODERATION_TIME_INTERVALS);
            UNIT_ASSERT_EQUAL(aoiToRecentProcessedTasksCounts.size(), 1);
            UNIT_ASSERT(aoiToRecentProcessedTasksCounts.count(1));
            UNIT_ASSERT_EQUAL(aoiToRecentProcessedTasksCounts.at(1), 1);
        }

        {
            pqxx::work txn(conn);

            TaskCreator(txn)
                .event(EventCreator(txn).aoiIds({1}).primaryObjData({10, "bld", "label", "notes"}))
                .resolved(1, ResolveResolution::Accept, "NOW() - '2 hour'::interval")
                .closed(1, CloseResolution::Approve, "NOW()")();
            TaskCreator(txn)
                .event(EventCreator(txn).aoiIds({1}).primaryObjData({10, "rd", "label", "notes"}))
                .resolved(1, ResolveResolution::Accept, "NOW() - '2 hour'::interval")
                .closed(1, CloseResolution::Approve, "NOW()")();

            const auto aoiToRecentProcessedTasksCounts = Gateway(txn)
                .taskStatsConsole(ModerationMode::Supervisor)
                .setFilterByCategories({"bld"})
                .recentProcessedTaskCountsByAoi(TEST_MODERATION_TIME_INTERVALS);
            UNIT_ASSERT_EQUAL(aoiToRecentProcessedTasksCounts.size(), 1);
            UNIT_ASSERT(aoiToRecentProcessedTasksCounts.count(1));
            UNIT_ASSERT_EQUAL(aoiToRecentProcessedTasksCounts.at(1), 1);
        }
    }

    Y_UNIT_TEST_F(should_filter_recent_processed_tasks_by_event_type, DbFixture)
    {
        {
            pqxx::work txn(conn);

            TaskCreator(txn)
                .event(EventCreator(txn).aoiIds({1}).type(EventType::Edit))
                .resolved(1, ResolveResolution::Accept)();
            TaskCreator(txn)
                .event(EventCreator(txn).aoiIds({1}).type(EventType::Complaint))
                .resolved(1, ResolveResolution::Accept)();

            const auto aoiToRecentProcessedTasksCounts = Gateway(txn)
                .taskStatsConsole(ModerationMode::Moderator)
                .setFilterByEventType(EventType::Edit)
                .recentProcessedTaskCountsByAoi(TEST_MODERATION_TIME_INTERVALS);
            UNIT_ASSERT_EQUAL(aoiToRecentProcessedTasksCounts.size(), 1);
            UNIT_ASSERT(aoiToRecentProcessedTasksCounts.count(1));
            UNIT_ASSERT_EQUAL(aoiToRecentProcessedTasksCounts.at(1), 1);
        }

        {
            pqxx::work txn(conn);

            TaskCreator(txn)
                .event(EventCreator(txn).aoiIds({1}).type(EventType::Complaint))
                .createdAt("NOW() - '1 day 3 hours'::interval")
                .resolved(1, ResolveResolution::Accept, "NOW()")();
            TaskCreator(txn)
                .event(EventCreator(txn).aoiIds({1}).type(EventType::RequestForDeletion))
                .createdAt("NOW() - '1 day 3 hours'::interval")
                .resolved(1, ResolveResolution::Accept, "NOW()")();

            const auto aoiToRecentProcessedTasksCounts = Gateway(txn)
                .taskStatsConsole(ModerationMode::SuperModerator)
                .setFilterByEventType(EventType::Complaint)
                .recentProcessedTaskCountsByAoi(TEST_MODERATION_TIME_INTERVALS);
            UNIT_ASSERT_EQUAL(aoiToRecentProcessedTasksCounts.size(), 1);
            UNIT_ASSERT(aoiToRecentProcessedTasksCounts.count(1));
            UNIT_ASSERT_EQUAL(aoiToRecentProcessedTasksCounts.at(1), 1);
        }

        {
            pqxx::work txn(conn);

            TaskCreator(txn)
                .event(EventCreator(txn).aoiIds({1}).type(EventType::RequestForDeletion))
                .resolved(1, ResolveResolution::Accept, "NOW() - '2 hour'::interval")
                .closed(1, CloseResolution::Approve, "NOW()")();
            TaskCreator(txn)
                .event(EventCreator(txn).aoiIds({1}).type(EventType::Edit))
                .resolved(1, ResolveResolution::Accept, "NOW() - '2 hour'::interval")
                .closed(1, CloseResolution::Approve, "NOW()")();

            const auto aoiToRecentProcessedTasksCounts = Gateway(txn)
                .taskStatsConsole(ModerationMode::Supervisor)
                .setFilterByEventType(EventType::Edit)
                .recentProcessedTaskCountsByAoi(TEST_MODERATION_TIME_INTERVALS);
            UNIT_ASSERT_EQUAL(aoiToRecentProcessedTasksCounts.size(), 1);
            UNIT_ASSERT(aoiToRecentProcessedTasksCounts.count(1));
            UNIT_ASSERT_EQUAL(aoiToRecentProcessedTasksCounts.at(1), 1);
        }
    }

    Y_UNIT_TEST_F(should_group_recent_processed_tasks_by_aoi, DbFixture)
    {
        {
            pqxx::work txn(conn);

            TaskCreator(txn)
                .event(EventCreator(txn).aoiIds({1, 2}))
                .resolved(1, ResolveResolution::Accept)();
            TaskCreator(txn)
                .event(EventCreator(txn).aoiIds({1}))
                .resolved(1, ResolveResolution::Accept)();

            const auto aoiToRecentProcessedTasksCounts = Gateway(txn)
                .taskStatsConsole(ModerationMode::Moderator)
                .recentProcessedTaskCountsByAoi(TEST_MODERATION_TIME_INTERVALS);
            UNIT_ASSERT_EQUAL(aoiToRecentProcessedTasksCounts.size(), 2);
            UNIT_ASSERT(aoiToRecentProcessedTasksCounts.count(1));
            UNIT_ASSERT_EQUAL(aoiToRecentProcessedTasksCounts.at(1), 2);
            UNIT_ASSERT(aoiToRecentProcessedTasksCounts.count(2));
            UNIT_ASSERT_EQUAL(aoiToRecentProcessedTasksCounts.at(2), 1);
        }

        {
            pqxx::work txn(conn);

            TaskCreator(txn)
                .event(EventCreator(txn).aoiIds({2}))
                .createdAt("NOW() - '1 day 3 hours'::interval")
                .resolved(1, ResolveResolution::Accept, "NOW()")();
            TaskCreator(txn)
                .event(EventCreator(txn).aoiIds({1, 2}))
                .createdAt("NOW() - '1 day 3 hours'::interval")
                .resolved(1, ResolveResolution::Accept, "NOW()")();

            const auto aoiToRecentProcessedTasksCounts = Gateway(txn)
                .taskStatsConsole(ModerationMode::SuperModerator)
                .recentProcessedTaskCountsByAoi(TEST_MODERATION_TIME_INTERVALS);
            UNIT_ASSERT_EQUAL(aoiToRecentProcessedTasksCounts.size(), 2);
            UNIT_ASSERT(aoiToRecentProcessedTasksCounts.count(1));
            UNIT_ASSERT_EQUAL(aoiToRecentProcessedTasksCounts.at(1), 1);
            UNIT_ASSERT(aoiToRecentProcessedTasksCounts.count(2));
            UNIT_ASSERT_EQUAL(aoiToRecentProcessedTasksCounts.at(2), 2);
        }

        {
            pqxx::work txn(conn);

            TaskCreator(txn)
                .event(EventCreator(txn).aoiIds({1, 2}))
                .resolved(1, ResolveResolution::Accept, "NOW() - '2 hour'::interval")
                .closed(1, CloseResolution::Approve, "NOW()")();
            TaskCreator(txn)
                .event(EventCreator(txn).aoiIds({1, 2}))
                .resolved(1, ResolveResolution::Accept, "NOW() - '2 hour'::interval")
                .closed(1, CloseResolution::Approve, "NOW()")();

            const auto aoiToRecentProcessedTasksCounts = Gateway(txn)
                .taskStatsConsole(ModerationMode::Supervisor)
                .recentProcessedTaskCountsByAoi(TEST_MODERATION_TIME_INTERVALS);
            UNIT_ASSERT_EQUAL(aoiToRecentProcessedTasksCounts.size(), 2);
            UNIT_ASSERT(aoiToRecentProcessedTasksCounts.count(1));
            UNIT_ASSERT_EQUAL(aoiToRecentProcessedTasksCounts.at(1), 2);
            UNIT_ASSERT(aoiToRecentProcessedTasksCounts.count(2));
            UNIT_ASSERT_EQUAL(aoiToRecentProcessedTasksCounts.at(2), 2);
        }
    }
}

} // namespace maps::wiki::social::tests

#include <maps/wikimap/mapspro/libs/social/aoi_feed.h>

#include <maps/wikimap/mapspro/libs/social/include/yandex/maps/wiki/social/gateway.h>

#include <maps/wikimap/mapspro/libs/social/tests/helpers/event_creator.h>
#include "helpers.h"

#include <library/cpp/testing/unittest/registar.h>

#include <array>

namespace maps::wiki::social::tests {

const TId UID = 0xface;

Y_UNIT_TEST_SUITE(aoi_feed)
{
    Y_UNIT_TEST_F(should_get_aoi_ids_by_commit_id, DbFixture)
    {
        pqxx::work txn(conn);
        Gateway gw(txn);

        const std::array tasks{
            gw.createTask(EventCreator(txn),                0_hours_ago),
            gw.createTask(EventCreator(txn).aoiIds({1, 3}), 1_hours_ago),
            gw.createTask(EventCreator(txn),                2_hours_ago),
            gw.createTask(EventCreator(txn).aoiIds({2, 3}), 3_hours_ago)
        };

        UNIT_ASSERT_EQUAL(gw.getAoiIdsOfActiveEditTask(tasks[0].commitId()), TIds{});
        UNIT_ASSERT_EQUAL(gw.getAoiIdsOfActiveEditTask(tasks[1].commitId()), TIds({1, 3}));
        UNIT_ASSERT_EQUAL(gw.getAoiIdsOfActiveEditTask(tasks[2].commitId()), TIds{});
        UNIT_ASSERT_EQUAL(gw.getAoiIdsOfActiveEditTask(tasks[3].commitId()), TIds({2, 3}));
    }

    Y_UNIT_TEST_F(should_get_aoi_ids_of_active_tasks_only, DbFixture)
    {
        pqxx::work txn(conn);
        Gateway gw(txn);

        const std::array tasks{
            gw.createTask(EventCreator(txn).aoiIds({42}), 2_hours_ago),
            gw.createTask(EventCreator(txn).aoiIds({42}), 2_hours_ago),
            gw.createTask(EventCreator(txn).aoiIds({42}), 2_hours_ago),
        };

        gw.superModerationConsole(UID).closeTasksByTaskIds(
            {tasks[0].id(), tasks[2].id()},
            ResolveResolution::Accept,
            CloseResolution::Approve
        );

        UNIT_ASSERT_EQUAL(gw.getAoiIdsOfActiveEditTask(tasks[0].commitId()), TIds{});
        UNIT_ASSERT_EQUAL(gw.getAoiIdsOfActiveEditTask(tasks[1].commitId()), TIds({42}));
        UNIT_ASSERT_EQUAL(gw.getAoiIdsOfActiveEditTask(tasks[2].commitId()), TIds{});
    }

    Y_UNIT_TEST_F(should_get_aoi_ids_of_edit_tasks_only, DbFixture)
    {
        pqxx::work txn(conn);
        Gateway gw(txn);

        const std::array tasks{
            gw.createTask(EventCreator(txn).aoiIds({24}).type(EventType::Complaint),          3_hours_ago),
            gw.createTask(EventCreator(txn).aoiIds({24}).type(EventType::Edit),               3_hours_ago),
            gw.createTask(EventCreator(txn).aoiIds({24}).type(EventType::RequestForDeletion), 3_hours_ago),
        };

        UNIT_ASSERT_EQUAL(gw.getAoiIdsOfActiveEditTask(tasks[0].commitId()), TIds{});
        UNIT_ASSERT_EQUAL(gw.getAoiIdsOfActiveEditTask(tasks[1].commitId()), TIds({24}));
        UNIT_ASSERT_EQUAL(gw.getAoiIdsOfActiveEditTask(tasks[2].commitId()), TIds{});
    }

    Y_UNIT_TEST_F(should_get_aoi_ids_of_tasks_from_trunk_only, DbFixture)
    {
        pqxx::work txn(conn);
        Gateway gw(txn);

        const std::array tasks{
            gw.createTask(EventCreator(txn).aoiIds({4, 2}).branchId(123), 4_hours_ago),
            gw.createTask(EventCreator(txn).aoiIds({4, 2}),               4_hours_ago),
            gw.createTask(EventCreator(txn).aoiIds({4, 2}).branchId(321), 4_hours_ago),
        };

        UNIT_ASSERT_EQUAL(gw.getAoiIdsOfActiveEditTask(tasks[0].commitId()), TIds{});
        UNIT_ASSERT_EQUAL(gw.getAoiIdsOfActiveEditTask(tasks[1].commitId()), TIds({4, 2}));
        UNIT_ASSERT_EQUAL(gw.getAoiIdsOfActiveEditTask(tasks[2].commitId()), TIds{});
    }
}

} // namespace maps::wiki::social::tests

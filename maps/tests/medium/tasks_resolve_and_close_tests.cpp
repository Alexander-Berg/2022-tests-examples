#include <maps/wikimap/mapspro/libs/social/tasks/resolve_and_close.h>

#include <maps/wikimap/mapspro/libs/social/tasks/acquire.h>
#include <yandex/maps/wiki/social/gateway.h>

#include <maps/wikimap/mapspro/libs/social/tests/helpers/task_creator.h>
#include "helpers.h"

#include <library/cpp/testing/unittest/registar.h>

#include <array>

namespace maps::wiki::social::tests {

namespace {
const TUid LOCKED_BY  = 101;
const TUid RESOLVE_BY = 102;
const TUid CLOSE_BY   = 103;

const auto RESOLVE_RESOLUTION = ResolveResolution::Edit;
const auto CLOSE_RESOLUTION   = CloseResolution::Edit;
}


Y_UNIT_TEST_SUITE(tasks_resolve_and_close_tests) {
    Y_UNIT_TEST_F(should_resolve_and_close_tasks, DbFixture)
    {
        pqxx::work txn(conn);

        const std::array allTasks = {
            TaskCreator(txn).locked(LOCKED_BY)(),
            TaskCreator(txn)(),
            TaskCreator(txn).locked(LOCKED_BY)()
        };
        const TaskIds acquiredTasksIds = {allTasks[0].id(), allTasks[2].id()};

        const auto resultIds = tasks::resolveAndClose(
            txn, acquiredTasksIds, LOCKED_BY, RESOLVE_BY, RESOLVE_RESOLUTION, CLOSE_BY, CLOSE_RESOLUTION
        );
        UNIT_ASSERT_EQUAL(resultIds, acquiredTasksIds);

        for (const auto& task: Gateway(txn).loadTasksByTaskIds(getIds(allTasks))) {
            if (acquiredTasksIds.count(task.id())) {
                UNIT_ASSERT_EQUAL(task.resolved().uid(), RESOLVE_BY);
                UNIT_ASSERT_EQUAL(task.resolved().resolution(), RESOLVE_RESOLUTION);
                UNIT_ASSERT_EQUAL(task.closed().uid(), CLOSE_BY);
                UNIT_ASSERT_EQUAL(task.closed().resolution(), CLOSE_RESOLUTION);
            } else {
                UNIT_ASSERT(!task.isResolved());
                UNIT_ASSERT(!task.isClosed());
            }
            UNIT_ASSERT(!task.isLocked());
        }
    }

    // It is prohibited by a check in DB to lock a closed task, therefore there
    // are no tests for already closed tasks.
    Y_UNIT_TEST_F(should_resolve_and_close_already_resolved, DbFixture)
    {
        pqxx::work txn(conn);

        const std::array allTasks = {
            TaskCreator(txn).locked(LOCKED_BY).resolved(1, ResolveResolution::Accept)(),
            TaskCreator(txn).locked(LOCKED_BY)()
        };
        const TaskIds allTasksIds = getIds(allTasks);
        const TaskIds unresolvedTasksIds = {allTasks[1].id()};

        const auto resultIds = tasks::resolveAndClose(
            txn, allTasksIds, LOCKED_BY, RESOLVE_BY, RESOLVE_RESOLUTION, CLOSE_BY, CLOSE_RESOLUTION
        );
        UNIT_ASSERT_EQUAL(resultIds, allTasksIds);

        for (const auto& task: Gateway(txn).loadTasksByTaskIds(allTasksIds)) {
            if (unresolvedTasksIds.count(task.id())) {
                UNIT_ASSERT_EQUAL(task.resolved().uid(), RESOLVE_BY);
                UNIT_ASSERT_EQUAL(task.resolved().resolution(), RESOLVE_RESOLUTION);
            } else {
                UNIT_ASSERT_EQUAL(task.resolved().uid(), 1);
                UNIT_ASSERT_EQUAL(task.resolved().resolution(), ResolveResolution::Accept);
            }
            UNIT_ASSERT(task.isClosed());
            UNIT_ASSERT_EQUAL(task.closed().uid(), CLOSE_BY);
            UNIT_ASSERT_EQUAL(task.closed().resolution(), CLOSE_RESOLUTION);
            UNIT_ASSERT(!task.isLocked());
        }
    }
}

} // namespace maps::wiki::social::tests

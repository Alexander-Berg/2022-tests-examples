#include <maps/wikimap/feedback/api/src/samsara_importer/lib/importer.h>
#include <maps/wikimap/feedback/api/src/samsara_importer/lib/import_for_tracker.h>
#include <maps/wikimap/feedback/api/src/samsara_importer/tests/medium_tests/common.h>

#include <maps/libs/chrono/include/time_point.h>
#include <maps/libs/common/include/file_utils.h>
#include <maps/libs/log8/include/log8.h>
#include <maps/wikimap/feedback/api/src/libs/common/config.h>
#include <maps/wikimap/feedback/api/src/libs/dbqueries/constants.h>
#include <maps/wikimap/feedback/api/src/libs/sync_queue/action_params.h>
#include <maps/wikimap/feedback/api/src/libs/sync_queue/types.h>
#include <maps/wikimap/feedback/api/src/libs/test_helpers/common.h>
#include <maps/wikimap/feedback/api/src/libs/test_helpers/db_fixture.h>
#include <maps/wikimap/feedback/api/src/yacare/lib/dbqueries.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::feedback::api::samsara_importer::tests {

namespace th = maps::wiki::feedback::api::tests;
namespace sq = maps::wiki::feedback::api::sync_queue;

namespace {

pgpool3::Pool& dbPool()
{
    static api::tests::DbFixture db;
    return db.pool();
}

} // unnamed namespace

Y_UNIT_TEST_SUITE(test_import_for_tracker)
{

Y_UNIT_TEST(import_some_for_tracker)
{
    TestGlobals globals(dbPool());
    auto txn = dbPool().masterWriteableTransaction();

    // import Samsara-ticket
    {
        auto samsaraTicket = samsara::Ticket::fromJson(
            json::Value::fromFile(SRC_("data/samsara_ticket_for_tracker.json")),
            {});

        ImportForTracker(globals).doImport(*txn, samsaraTicket);
    }

    // check task
    TaskId taskId;
    {
        const auto taskIds = getAllTaskIds(*txn);
        UNIT_ASSERT_VALUES_EQUAL(taskIds.size(), 1);

        auto task = loadMandatoryFeedbackTask(*txn, taskIds.front());
        taskId = task.id;
        auto taskExpected = FeedbackTask::fromJson(
            json::Value::fromFile(SRC_("data/fbapi_ticket_for_tracker.json")));
        taskExpected.id = task.id;
        taskExpected.updatedAt = task.updatedAt;
        taskExpected.createdAt = task.createdAt;
        UNIT_ASSERT_VALUES_EQUAL(task, taskExpected);
    }

    // check sync queue
    {
        auto syncQueue = th::loadSyncQueue(*txn);
        // [0] - SamsaraLinkToTracker (latest)
        // [1] - TrackerCreateIssue
        // [2] - UgcContribution
        UNIT_ASSERT_VALUES_EQUAL(syncQueue.size(), 3);

        {
            sq::ActionInfo actionInfo(syncQueue[1]);

            UNIT_ASSERT_VALUES_EQUAL(
                actionInfo.action,
                sq::SyncAction::TrackerCreateIssue);

            sync_queue::CreateTrackerIssueParams params(actionInfo.syncInfo);

            UNIT_ASSERT_VALUES_EQUAL(params.queueKey, "GEOCONTENTFB");
            UNIT_ASSERT_VALUES_EQUAL(params.component, 88074);
            UNIT_ASSERT_VALUES_EQUAL(params.summary, "Redirected from Samsara");
            UNIT_ASSERT_VALUES_EQUAL(
                params.description,
                maps::common::readFileToString(SRC_("data/tracker_message.txt")));
        }
        {
            sq::ActionInfo actionInfo(syncQueue[0]);
            UNIT_ASSERT_VALUES_EQUAL(
                actionInfo.action,
                sq::SyncAction::SamsaraLinkToTracker);

            sync_queue::SamsaraLinkToTrackerParams params(actionInfo.syncInfo);

            UNIT_ASSERT_VALUES_EQUAL(params.samsaraStatus, samsara::TicketStatus::Closed);
            UNIT_ASSERT_VALUES_EQUAL(params.samsaraResolution, samsara::TicketResolution::Resolved);
            UNIT_ASSERT_VALUES_EQUAL(params.message, "Redirected to Tracker");
        }
    }

    // check history
    {
        History history = loadTaskChanges(*txn, taskId);
        UNIT_ASSERT_VALUES_EQUAL(history.size(), 1);
        const TaskChange& taskChange = history[0];
        UNIT_ASSERT_VALUES_EQUAL(taskChange.status, TaskStatus::Rejected);
        UNIT_ASSERT(!taskChange.service);
        UNIT_ASSERT(!taskChange.message);
    }
}

} // test_importer suite

} // namespace maps::wiki::feedback::api::samsara_importer:tests

#include <maps/wikimap/feedback/api/src/libs/common/config.h>
#include <maps/wikimap/feedback/api/src/libs/dbqueries/constants.h>
#include <maps/wikimap/feedback/api/src/libs/dbqueries/dbqueries.h>
#include <maps/wikimap/feedback/api/src/libs/test_helpers/common.h>
#include <maps/wikimap/feedback/api/src/libs/test_helpers/db_fixture.h>
#include <maps/wikimap/feedback/api/src/libs/sync_queue/constants.h>
#include <maps/wikimap/feedback/api/src/libs/sync_queue/dbqueries.h>
#include <maps/wikimap/feedback/api/src/yacare/lib/dbqueries.h>
#include <maps/wikimap/feedback/api/src/yacare/lib/tracker.h>
#include <maps/wikimap/mapspro/libs/query_builder/include/select_query.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <util/stream/output.h>

namespace maps::wiki::feedback::api::tests {

namespace sq = sync_queue;

namespace {

auto& dbPool()
{
    static api::tests::DbFixture db;
    return db.pool();
}

TaskId TASK_ID("dd3b2eab-a8e8-491e-bd8b-868edb754050");

void createTask(pqxx::transaction_base& txn)
{
    const OriginalTask originalTask{maps::json::Value::fromString(R"({
        "form_id": "toponym",
        "message": "IDDQD_AEZAKMI",
        "form_point": {
            "lon": 37.37,
            "lat": 55.55
        },
        "metadata": {
            "uid": 42,
            "uuid": "c5216b6a-2379-4bab-a729-515831a42e72",
            "locale": "ru_RU",
            "client_id": "mobile_maps_android",
            "version": "1.0"
        },
        "answer_id": "toponym",
        "answer_context": {
            "toponym": {
                "address": [
                    {
                        "kind": "street",
                        "name": "\u0434\u0440\u0443\u0436\u0431\u0430"
                    },
                    {
                        "kind": "house",
                        "name": "39"
                    }
                ],
                "center_point": {
                    "lat": 56.992454367158864,
                    "lon": 60.500016232619615
                },
                "name": "\u0421\u041d\u0422 \u0414\u0440\u0443\u0436\u0431\u0430, 39"
            }
        },
        "object_id": "object_id",
        "question_id": "add_object",
        "user_email": "email@test.ru"
    })")};

    const auto now = maps::chrono::TimePoint::clock::now();
    FeedbackTask task{
        .id = TASK_ID,
        .service = Service::Nmaps,
        .serviceObjectId = ServiceObjectId("serviceObjectId"),
        .serviceObjectUrl = "http://serviceObjectUrl",
        .status = TaskStatus::Published,
        .originalTask = originalTask,
        .integration = {},
        .createdAt = now,
        .updatedAt = now
    };

    task.updateIntegration();

    dbqueries::createFeedbackTask(task, HttpData{}).exec(txn);
}

FeedbackTask loadTask(pqxx::transaction_base& txn, const TaskId& taskId)
{
    auto task = dbqueries::loadFeedbackTask(txn, taskId);
    UNIT_ASSERT(task);
    return std::move(*task);
}

} // namespace

Y_UNIT_TEST_SUITE(test_tracker)
{

Y_UNIT_TEST(redirect_to_tracker)
{
    std::unique_ptr<Globals> globals = Globals::create(
        SRC_("data/feedback_api.conf"), false);
    ASSERT(globals != nullptr);

    auto txn = dbPool().masterWriteableTransaction();

    createTask(*txn);
    const auto task = loadTask(*txn, TASK_ID);

    // redirect to Tracker
    {
        RedirectToTrackerRule redirectRule(json::Value::fromString(R"--({
            "trackerQueueKey": "GEOCONTENTFB",
            "trackerComponentId": 88067
        })--"));

        UNIT_ASSERT_NO_EXCEPTION(rejectToTracker(
            *globals,
            task,
            NmapsResolution::RedirectToContentAuto,
            redirectRule,
            "redirect message"
        ).exec(*txn));
    }

    // check sync queue
    {
        auto syncQueue = loadSyncQueue(*txn);
        // [0] - SamsaraLinkToTracker (latest)
        // [1] - TrackerCreateIssue
        // [2] - UgcContribution
        // [3] - UgcContribution
        UNIT_ASSERT_VALUES_EQUAL(syncQueue.size(), 4);

        {
            sq::ActionInfo actionInfo(syncQueue[1]);

            UNIT_ASSERT_VALUES_EQUAL(
                actionInfo.action,
                sq::SyncAction::TrackerCreateIssue);
            UNIT_ASSERT_VALUES_EQUAL(
                actionInfo.task.id,
                task.id);

            sync_queue::CreateTrackerIssueParams params(actionInfo.syncInfo);

            UNIT_ASSERT_VALUES_EQUAL(params.queueKey, "GEOCONTENTFB");
            UNIT_ASSERT_VALUES_EQUAL(params.component, 88067);
            UNIT_ASSERT_VALUES_EQUAL(params.summary, "add_object.toponym:serviceObjectId");
        }
        {
            sq::ActionInfo actionInfo(syncQueue[0]);
            UNIT_ASSERT_VALUES_EQUAL(
                actionInfo.action,
                sq::SyncAction::SamsaraLinkToTracker);
            UNIT_ASSERT_VALUES_EQUAL(
                actionInfo.task.id,
                task.id);

            sync_queue::SamsaraLinkToTrackerParams params(actionInfo.syncInfo);

            UNIT_ASSERT_VALUES_EQUAL(params.samsaraStatus, samsara::TicketStatus::Closed);
            UNIT_ASSERT_VALUES_EQUAL(params.samsaraResolution, samsara::TicketResolution::Resolved);
            UNIT_ASSERT_VALUES_EQUAL(params.samsaraQueue, samsara::QUEUE_NEED_INFO_2L);
            UNIT_ASSERT_VALUES_EQUAL(params.message, "redirect message");
        }
    }

    // check history
    {
        History history = loadTaskChanges(*txn, task.id);
        UNIT_ASSERT_VALUES_EQUAL(history.size(), 2);
        TaskChange taskChange = history[1];
        UNIT_ASSERT_VALUES_EQUAL(taskChange.taskId.value(), task.id.value());
        UNIT_ASSERT_VALUES_EQUAL(taskChange.status, TaskStatus::Rejected);
        UNIT_ASSERT_VALUES_EQUAL(*taskChange.service, Service::Nmaps);
        UNIT_ASSERT_VALUES_EQUAL(
            *taskChange.message,
            "Rejected in nmaps with resolution redirect-to-content-auto");
    }

    // check task
    {
        const auto updatedTask = dbqueries::loadFeedbackTask(*txn, task.id);
        UNIT_ASSERT(updatedTask);
        UNIT_ASSERT_VALUES_EQUAL(updatedTask->service, Service::Nmaps);
        UNIT_ASSERT_VALUES_EQUAL(updatedTask->status, TaskStatus::Rejected);

        Integration expectedIntegration;
        expectedIntegration.addServiceOrThrow(
            Service::Nmaps,
            {
                ServiceObjectId{"serviceObjectId"},
                "http://serviceObjectUrl",
                "redirect-to-content-auto"
            });
        UNIT_ASSERT_VALUES_EQUAL(updatedTask->integration, expectedIntegration);
    }
}

} // test_tracker suite

} // namespace maps::wiki::feedback::api::tests

#include <maps/wikimap/feedback/api/src/libs/common/config.h>
#include <maps/wikimap/feedback/api/src/libs/sync_queue/constants.h>
#include <maps/wikimap/feedback/api/src/libs/sync_queue/dbqueries.h>
#include <maps/wikimap/feedback/api/src/yacare/lib/samsara.h>

#include <maps/wikimap/feedback/api/src/libs/test_helpers/common.h>
#include <maps/wikimap/feedback/api/src/libs/test_helpers/db_fixture.h>

#include <maps/libs/http/include/test_utils.h>

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

const FeedbackTask SAMPLE_TASK_ONE{
    TaskId("fa5dd2f3-dd1b-7fb4-4425-21aaf5dbb885"),
    Service::Support,
    ServiceObjectId("serviceObjectId"),
    "http://serviceObjectUrl",
    TaskStatus::NeedInfo,
    api::tests::EMPTY_ORIGINAL_TASK,
    {/*integration*/},
    maps::chrono::parseIsoDateTime("2020-04-01 01:00:00+00:00"),
    maps::chrono::parseIsoDateTime("2020-04-02 01:00:00+00:00")
};

const OriginalTask ORIGINAL_TASK_IMPORTED{
    maps::json::Value::fromString(R"(
        {
            "form_id": "other",
            "form_point": {
                "lon": 37.37,
                "lat": 55.55
            },
            "metadata": {
                "client_id": "samsara",
                "locale": "ru_RU",
                "version": "1.0"
            },
            "question_id": "other",
            "answer_id": "toponym"
        }
    )")
};

const FeedbackTask SAMPLE_TASK_IMPORTED{
    TaskId("fa5dd2f3-dd1b-7fb4-4425-21aaf5dbb885"),
    Service::Nmaps,
    ServiceObjectId("serviceObjectId"),
    "http://serviceObjectUrl",
    TaskStatus::InProgress,
    ORIGINAL_TASK_IMPORTED,
    {/*integration*/},
    maps::chrono::parseIsoDateTime("2020-04-01 01:00:00+00:00"),
    maps::chrono::parseIsoDateTime("2020-04-02 01:00:00+00:00")
};

} // namespace

Y_UNIT_TEST_SUITE(test_samsara)
{

Y_UNIT_TEST(forward_to_support_async)
{
    auto txn = dbPool().masterWriteableTransaction();

    UNIT_ASSERT_NO_EXCEPTION(
        forwardToSupportAsync(
            SAMPLE_TASK_ONE,
            "some article message")
        .exec(*txn));

    auto syncQueue = loadSyncQueue(*txn);
    UNIT_ASSERT_VALUES_EQUAL(syncQueue.size(), 1);

    auto actionInfo = sq::ActionInfo{syncQueue[0]};
    UNIT_ASSERT_VALUES_EQUAL(sq::SyncAction::SamsaraUpdateTicket, actionInfo.action);
    UNIT_ASSERT_VALUES_EQUAL(SAMPLE_TASK_ONE, actionInfo.task);

    auto params = sq::SamsaraUpdateTicketParams(actionInfo.syncInfo);
    UNIT_ASSERT_VALUES_EQUAL(samsara::QUEUE_MAPS_2L,      params.samsaraQueue);
    UNIT_ASSERT_VALUES_EQUAL("some article message",      params.ticketNoteMessage.value_or("empty"));
    UNIT_ASSERT_VALUES_EQUAL(false,                       params.keepIntegration);
    UNIT_ASSERT_VALUES_EQUAL(false,                       params.forceSamsaraQueue);
}

Y_UNIT_TEST(switch_ticket_queue_async)
{
    auto txn = dbPool().masterWriteableTransaction();

    UNIT_ASSERT_NO_EXCEPTION(
        switchTicketQueueAsync(
            SAMPLE_TASK_ONE,
            samsara::QUEUE_NEED_INFO_2L)
        .exec(*txn));

    auto syncQueue = loadSyncQueue(*txn);
    UNIT_ASSERT_VALUES_EQUAL(syncQueue.size(), 1);

    auto actionInfo = sq::ActionInfo{syncQueue[0]};
    UNIT_ASSERT_VALUES_EQUAL(sq::SyncAction::SamsaraUpdateTicket, actionInfo.action);
    UNIT_ASSERT_VALUES_EQUAL(SAMPLE_TASK_ONE, actionInfo.task);

    auto params = sq::SamsaraUpdateTicketParams(actionInfo.syncInfo);
    UNIT_ASSERT_VALUES_EQUAL(samsara::QUEUE_NEED_INFO_2L, params.samsaraQueue);
    UNIT_ASSERT_VALUES_EQUAL(true,                        params.forceSamsaraQueue);
    UNIT_ASSERT_VALUES_EQUAL(true,                        params.keepIntegration);
    UNIT_ASSERT(!params.ticketNoteMessage.has_value());
}

Y_UNIT_TEST(post_resolution_async)
{
    auto ticketId = ServiceObjectId{"123"};

    auto txn = dbPool().masterWriteableTransaction();

    UNIT_ASSERT_NO_EXCEPTION(
        postResolutionAsync(
            SAMPLE_TASK_ONE,
            ticketId,
            TaskStatus::Rejected,
            NmapsResolution::IncorrectData)
        .exec(*txn));

    auto syncQueue = loadSyncQueue(*txn);
    UNIT_ASSERT_VALUES_EQUAL(syncQueue.size(), 1);

    auto actionInfo = sq::ActionInfo{syncQueue[0]};
    UNIT_ASSERT_VALUES_EQUAL(sq::SyncAction::SamsaraAddNote, actionInfo.action);
    UNIT_ASSERT_VALUES_EQUAL(SAMPLE_TASK_ONE, actionInfo.task);

    auto params = sq::SamsaraAddNoteParams(actionInfo.syncInfo);
    UNIT_ASSERT_VALUES_EQUAL(ticketId, params.ticketId);
    UNIT_ASSERT_VALUES_EQUAL(
        "Правка отклонена по причине «ошибочная информация».",
        params.message);
}

Y_UNIT_TEST(close_ticket_async)
{
    auto ticketId = ServiceObjectId{"123"};

    auto txn = dbPool().masterWriteableTransaction();

    UNIT_ASSERT_NO_EXCEPTION(
        closeTicketAsync(
            SAMPLE_TASK_ONE,
            ticketId,
            samsara::TicketResolution::Rejected)
        .exec(*txn));

    auto syncQueue = loadSyncQueue(*txn);
    UNIT_ASSERT_VALUES_EQUAL(syncQueue.size(), 1);

    const auto& syncRow = syncQueue[0];
    UNIT_ASSERT_VALUES_EQUAL(
        syncRow[sq::columns::ACTION].as<std::string>(),
        std::string{toString(sq::SyncAction::SamsaraCloseTicket)});
    UNIT_ASSERT_VALUES_EQUAL(
        FeedbackTask::fromJson(maps::json::Value::fromString(
            syncRow[sq::columns::FEEDBACK_TASK].as<std::string>())).id,
        SAMPLE_TASK_ONE.id);

    auto syncInfo = maps::json::Value::fromString(
        syncRow[sq::columns::SYNC_INFO].as<std::string>());

    UNIT_ASSERT_VALUES_EQUAL(syncInfo["ticket_id"].as<std::string>(), ticketId.value());
    UNIT_ASSERT_VALUES_EQUAL(
        syncInfo["resolution"].as<std::string>(),
        std::string{toString(samsara::TicketResolution::Rejected)});
}

} // test_samsara suite

} // namespace maps::wiki::feedback::api::tests

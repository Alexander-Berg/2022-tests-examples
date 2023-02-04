#include <maps/wikimap/feedback/api/src/yacare/lib/sync_utils.h>

#include <maps/wikimap/feedback/api/src/libs/sync_queue/action_params.h>
#include <maps/wikimap/feedback/api/src/libs/sync_queue/constants.h>
#include <maps/wikimap/feedback/api/src/libs/sync_queue/types.h>

#include <maps/wikimap/feedback/api/src/libs/common/config.h>
#include <maps/wikimap/feedback/api/src/libs/common/original_task.h>

#include <maps/wikimap/feedback/api/src/libs/test_helpers/common.h>
#include <maps/wikimap/feedback/api/src/libs/test_helpers/db_fixture.h>

#include <maps/libs/http/include/test_utils.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::feedback::api::tests {

namespace sq = sync_queue;

namespace {

auto& dbPool()
{
    static api::tests::DbFixture db;
    return db.pool();
}

const FeedbackTask providerFeedbackTask()
{
    const auto now = maps::chrono::TimePoint::clock::now();
    return FeedbackTask{
        .id = TaskId{"9a8d36c4-3f44-4b39-8e55-75d6233adb03"},
        .service = Service::Support,
        .status = TaskStatus::Rejected,
        .originalTask = OriginalTask{
            maps::json::Value::fromFile(SRC_("data/original_task_provider.json"))},
        .createdAt = now,
        .updatedAt = now
    };
}

} // namespace

Y_UNIT_TEST_SUITE(test_sync_utils)
{

Y_UNIT_TEST(test_sync_feedback_task_with_support_provider_queue)
{

    const FeedbackTask task = providerFeedbackTask();
    auto query = transferFeedbackTaskToSamsaraAsync(task);

    UNIT_ASSERT(!query.empty());

    auto txn = dbPool().masterWriteableTransaction();
    query.exec(*txn);

    auto syncQueue = loadSyncQueue(*txn);
    UNIT_ASSERT_VALUES_EQUAL(syncQueue.size(), 1);

    auto actionInfo = sq::ActionInfo{syncQueue[0]};
    UNIT_ASSERT_VALUES_EQUAL(sq::SyncAction::SamsaraUpdateTicket, actionInfo.action);
    UNIT_ASSERT_VALUES_EQUAL(task, actionInfo.task);

    auto params = sq::SamsaraUpdateTicketParams{actionInfo.syncInfo};
    UNIT_ASSERT_VALUES_EQUAL(samsara::QUEUE_MAPS_2L, params.samsaraQueue);
    UNIT_ASSERT_VALUES_EQUAL(
        "test\n"
            "\tНазвание: Provider name\n"
            "\tСсылка на сайт провайдера: <не задана>\n"
            "\tОрганизация в Яндекс.Картах: Provider organization\n"
            "\tСсылка на организацию в Яндекс.Картах: https://yandex.by/maps/org/12345678901\n",
        params.ticketNoteMessage.value_or("empty"));
    UNIT_ASSERT_VALUES_EQUAL(false,         params.forceSamsaraQueue);
    UNIT_ASSERT_VALUES_EQUAL(false,         params.keepIntegration);
}

} // test_sync_utils suite

} // namespace maps::wiki::feedback::api::tests

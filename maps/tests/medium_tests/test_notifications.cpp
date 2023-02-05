#include <maps/wikimap/feedback/api/src/libs/sync_queue/action_params.h>
#include <maps/wikimap/feedback/api/src/libs/sync_queue/types.h>
#include <maps/wikimap/feedback/api/src/yacare/lib/notifications.h>

#include <maps/wikimap/feedback/api/src/libs/test_helpers/common.h>
#include <maps/wikimap/feedback/api/src/libs/test_helpers/db_fixture.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <maps/libs/json/include/value.h>

namespace maps::wiki::feedback::api::tests {

namespace sq = sync_queue;
using namespace std::literals::chrono_literals;

namespace {

auto& dbPool()
{
    static api::tests::DbFixture db;
    return db.pool();
}

const OriginalTask ORIGINAL_TASK_FROM_PUSH{
    maps::json::Value::fromFile(SRC_("data/original_task_from_push.json"))};

const OriginalTask ORIGINAL_TASK_WITHOUT_UID{
    maps::json::Value::fromFile(SRC_("data/original_task_without_uid.json"))};

const OriginalTask ORIGINAL_TASK{
    maps::json::Value::fromFile(SRC_("data/original_task_parking.json"))};

} // namespace

Y_UNIT_TEST_SUITE(test_notifications)
{

Y_UNIT_TEST(can_send_push)
{
    MobileClients mobileMapsClients{
        .android = "ru.yandex.yandexmaps.debug",
        .ios = "ru.yandex.traffic.sandbox",
    };
    MobileClients naviClients{
        .android = "ru.yandex.yandexnavi.inhouse",
        .ios = "ru.yandex.mobile.navigator.inhouse",
    };

    auto wrapper = [&] (
        std::optional<TaskStatus> newStatus,
        const OriginalTask& originalTask,
        const maps::chrono::TimePoint& now) {
        FeedbackTask task{
            .id = TaskId{"9a8d36c4-3f44-4b39-8e55-75d6233adb03"},
            .service = Service::Nmaps,
            .status = TaskStatus::Published,
            .originalTask = originalTask,
            .createdAt = now,
            .updatedAt = now
        };
        return canSendPush(task, newStatus, mobileMapsClients, naviClients);
    };

    const auto now = maps::chrono::TimePoint::clock::now();
    UNIT_ASSERT(!wrapper(std::nullopt, ORIGINAL_TASK, now));
    UNIT_ASSERT(!wrapper(TaskStatus::Published, ORIGINAL_TASK_WITHOUT_UID, now));
    UNIT_ASSERT(wrapper(TaskStatus::Published, ORIGINAL_TASK, now));

    OriginalTask originalTask(ORIGINAL_TASK);
    originalTask.setMetadataClientId("mobile_maps_android");
    UNIT_ASSERT(!wrapper(TaskStatus::Published, originalTask, now));
    originalTask.setMetadataClientId("desktop-maps");
    UNIT_ASSERT(wrapper(TaskStatus::Published, originalTask, now));

    originalTask.setQuestionId(QuestionId::ApproveObject);
    originalTask.setAnswerId(AnswerId::Fence);
    UNIT_ASSERT(!wrapper(TaskStatus::Published, originalTask, now));
    originalTask.setQuestionId(QuestionId::Closed);
    originalTask.setAnswerId(AnswerId::Moved);
    UNIT_ASSERT(!wrapper(TaskStatus::Published, originalTask, now));

    UNIT_ASSERT(!wrapper(TaskStatus::Rejected, ORIGINAL_TASK_FROM_PUSH, now));
    UNIT_ASSERT(wrapper(TaskStatus::Rejected, ORIGINAL_TASK, now));

    UNIT_ASSERT(wrapper(TaskStatus::Rejected, ORIGINAL_TASK, now - 335h));
    UNIT_ASSERT(!wrapper(TaskStatus::Rejected, ORIGINAL_TASK, now - 336h));
}

Y_UNIT_TEST(send_push_async)
{
    auto txn = dbPool().masterWriteableTransaction();

    const auto now = maps::chrono::TimePoint::clock::now();
    auto task = FeedbackTask{
        .id = TaskId{"9a8d36c4-3f44-4b39-8e55-75d6233adb03"},
        .service = Service::Support,
        .status = TaskStatus::InProgress,
        .originalTask = ORIGINAL_TASK,
        .createdAt = now,
        .updatedAt = now
    };

    MobileClients mobileMapsClients{
        .android = "ru.yandex.yandexmaps.debug",
        .ios = "ru.yandex.traffic.sandbox",
    };
    sendPushAsync(sq::PushType::ToponymPublished, task, mobileMapsClients).exec(*txn);

    auto syncQueue = loadSyncQueue(*txn);
    UNIT_ASSERT_VALUES_EQUAL(syncQueue.size(), 1);

    auto actionInfo = sq::ActionInfo{syncQueue[0]};
    UNIT_ASSERT_VALUES_EQUAL(sq::SyncAction::SupSendPush, actionInfo.action);
    UNIT_ASSERT_VALUES_EQUAL(task, actionInfo.task);

    auto params = sq::SupSendPushParams(actionInfo.syncInfo);
    UNIT_ASSERT_VALUES_EQUAL(sq::PushType::ToponymPublished, params.pushType);
    UNIT_ASSERT_VALUES_EQUAL("en_EN", params.locale);
    UNIT_ASSERT_VALUES_EQUAL("ru.yandex.yandexmaps.debug", params.clientId);
    UNIT_ASSERT_VALUES_EQUAL(42, params.receiverUid.value());
    UNIT_ASSERT_VALUES_EQUAL("fb:9a8d36c4-3f44-4b39-8e55-75d6233adb03", params.contributionId);
}

Y_UNIT_TEST(can_send_email)
{
    const auto now = maps::chrono::TimePoint::clock::now();
    FeedbackTask task{
        .id = TaskId{"9a8d36c4-3f44-4b39-8e55-75d6233adb03"},
        .service = Service::Nmaps,
        .status = TaskStatus::Published,
        .originalTask = ORIGINAL_TASK,
        .createdAt = now,
        .updatedAt = now
    };

    UNIT_ASSERT(canSendEmail(task, MailNotificationType::ToponymNew));
    task.createdAt = now - 336h;
    UNIT_ASSERT(!canSendEmail(task, MailNotificationType::ToponymNew));
    task.createdAt = now;

    task.originalTask.setQuestionId(QuestionId::ApproveObject);
    task.originalTask.setAnswerId(AnswerId::Fence);
    UNIT_ASSERT(!canSendEmail(task, MailNotificationType::ToponymNew));
    task.originalTask.setQuestionId(QuestionId::AddObject);
    task.originalTask.setAnswerId(AnswerId::Parking);

    task.originalTask = ORIGINAL_TASK_FROM_PUSH;
    UNIT_ASSERT(!canSendEmail(task, MailNotificationType::ToponymNew));

    task.originalTask = ORIGINAL_TASK_WITHOUT_UID;
    UNIT_ASSERT(!canSendEmail(task, MailNotificationType::ToponymNew));
    UNIT_ASSERT(!canSendEmail(task, MailNotificationType::RouteNew));
    UNIT_ASSERT(canSendEmail(task, MailNotificationType::RoutePublished));
}

Y_UNIT_TEST(send_email_async)
{
    auto txn = dbPool().masterWriteableTransaction();

    const auto now = maps::chrono::TimePoint::clock::now();
    FeedbackTask task{
        .id = TaskId{"9a8d36c4-3f44-4b39-8e55-75d6233adb03"},
        .service = Service::Nmaps,
        .status = TaskStatus::Published,
        .originalTask = ORIGINAL_TASK,
        .createdAt = now,
        .updatedAt = now
    };

    sendEmailAsync(task, MailNotificationType::ToponymPublished).exec(*txn);

    auto syncQueue = loadSyncQueue(*txn);
    UNIT_ASSERT_VALUES_EQUAL(syncQueue.size(), 1);

    auto actionInfo = sq::ActionInfo{syncQueue[0]};
    UNIT_ASSERT_VALUES_EQUAL(sq::SyncAction::SenderSendEmail, actionInfo.action);
    UNIT_ASSERT_VALUES_EQUAL(task, actionInfo.task);

    auto params = sq::SenderSendEmailParams(actionInfo.syncInfo);
    UNIT_ASSERT_VALUES_EQUAL(MailNotificationType::ToponymPublished, params.mailType);
    UNIT_ASSERT_VALUES_EQUAL("en_EN", params.locale);
    UNIT_ASSERT_VALUES_EQUAL("sample@yandex.ru", params.email);

    const double PRECISION = 1e-15;
    UNIT_ASSERT(std::abs(params.formPoint.x() - 27.501077764443266) < PRECISION);
    UNIT_ASSERT(std::abs(params.formPoint.y() - 52.7867289800041) < PRECISION);

    UNIT_ASSERT(!params.routeEncodedPoints);
}

Y_UNIT_TEST(send_push_async_desktop_maps_and_touch_maps)
{
    auto txn = dbPool().masterWriteableTransaction();

    const auto now = maps::chrono::TimePoint::clock::now();
    auto task = FeedbackTask{
        .id = TaskId{"9a8d36c4-3f44-4b39-8e55-75d6233adb03"},
        .service = Service::Support,
        .status = TaskStatus::InProgress,
        .originalTask = ORIGINAL_TASK,
        .createdAt = now,
        .updatedAt = now
    };
    task.originalTask.setMetadataClientId("desktop-maps");

    MobileClients mobileMapsClients{
        .android = "ru.yandex.yandexmaps.debug",
        .ios = "ru.yandex.traffic.sandbox",
    };
    sendPushAsync(sq::PushType::ToponymRejectedProhibitedByRules, task, mobileMapsClients).exec(*txn);

    auto syncQueue = loadSyncQueue(*txn);
    UNIT_ASSERT_VALUES_EQUAL(syncQueue.size(), 2);

    auto actionInfo1 = sq::ActionInfo{syncQueue[0]};
    UNIT_ASSERT_VALUES_EQUAL(sq::SyncAction::SupSendPush, actionInfo1.action);
    UNIT_ASSERT_VALUES_EQUAL(task, actionInfo1.task);

    auto params1 = sq::SupSendPushParams(actionInfo1.syncInfo);
    UNIT_ASSERT_VALUES_EQUAL(sq::PushType::ToponymRejectedProhibitedByRules, params1.pushType);
    UNIT_ASSERT_VALUES_EQUAL("en_EN", params1.locale);
    UNIT_ASSERT(params1.clientId == mobileMapsClients.android ||
                params1.clientId == mobileMapsClients.ios);
    UNIT_ASSERT_VALUES_EQUAL(42, params1.receiverUid.value());
    UNIT_ASSERT_VALUES_EQUAL("fb:9a8d36c4-3f44-4b39-8e55-75d6233adb03", params1.contributionId);

    auto actionInfo2 = sq::ActionInfo{syncQueue[1]};
    UNIT_ASSERT_VALUES_EQUAL(sq::SyncAction::SupSendPush, actionInfo2.action);
    UNIT_ASSERT_VALUES_EQUAL(task, actionInfo2.task);

    auto params2 = sq::SupSendPushParams(actionInfo2.syncInfo);
    UNIT_ASSERT_VALUES_EQUAL(sq::PushType::ToponymRejectedProhibitedByRules, params2.pushType);
    UNIT_ASSERT_VALUES_EQUAL("en_EN", params2.locale);
    UNIT_ASSERT(params2.clientId == mobileMapsClients.android ||
                params2.clientId == mobileMapsClients.ios);
    UNIT_ASSERT_VALUES_EQUAL(42, params2.receiverUid.value());
    UNIT_ASSERT_VALUES_EQUAL("fb:9a8d36c4-3f44-4b39-8e55-75d6233adb03", params2.contributionId);

    UNIT_ASSERT(params1.clientId != params2.clientId);

    task.originalTask.setMetadataClientId("touch-maps");

    sendPushAsync(sq::PushType::ToponymRejectedProhibitedByRules, task, mobileMapsClients).exec(*txn);
    UNIT_ASSERT_VALUES_EQUAL(loadSyncQueue(*txn).size(), 4);

    task.originalTask.setMetadataClientId("ru.yandex.mobile.navigator.inhouse");

    sendPushAsync(sq::PushType::ToponymRejectedIncorrectData, task, mobileMapsClients).exec(*txn);
    UNIT_ASSERT_VALUES_EQUAL(loadSyncQueue(*txn).size(), 5);
}

} // test_notifications suite

} // namespace maps::wiki::feedback::api::tests

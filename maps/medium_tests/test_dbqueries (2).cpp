#include <maps/wikimap/feedback/api/src/yacare/lib/dbqueries.h>

#include <maps/wikimap/feedback/api/src/libs/common/lang.h>
#include <maps/wikimap/feedback/api/src/libs/dbqueries/dbqueries.h>
#include <maps/wikimap/feedback/api/src/libs/sync_queue/constants.h>
#include <maps/wikimap/feedback/api/src/libs/sync_queue/dbqueries.h>
#include <maps/wikimap/feedback/api/src/libs/test_helpers/common.h>
#include <maps/wikimap/feedback/api/src/libs/test_helpers/db_fixture.h>
#include <maps/wikimap/feedback/api/src/notifications/lib/dbqueries.h>

#include <maps/infra/yacare/include/yacare.h>

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

const History SAMPLE_TASK_ONE_HISTORY{
    {
        TaskChangeId{1},
        TaskId("fa5dd2f3-dd1b-7fb4-4425-21aaf5dbb885"),
        TaskStatus::New,
        std::nullopt,
        {"message"},
        maps::chrono::parseIsoDateTime("2020-04-01 01:00:00+00:00")
    },
    {
        TaskChangeId{3},
        TaskId("fa5dd2f3-dd1b-7fb4-4425-21aaf5dbb885"),
        TaskStatus::Rejected,
        std::nullopt,
        {"message"},
        maps::chrono::parseIsoDateTime("2020-05-01 01:00:00+00:00")
    },
};

const FeedbackTask SAMPLE_TASK_TWO = FeedbackTask{
    TaskId("1c6f63e7-cc1a-4469-9f6a-389e8132f796"),
        Service::Support,
        ServiceObjectId("serviceObjectId"),
        "http://serviceObjectUrl",
        TaskStatus::NeedInfo,
        api::tests::EMPTY_ORIGINAL_TASK,
        {/*integration*/},
        maps::chrono::parseIsoDateTime("2020-06-01 01:00:00+00:00"),
        maps::chrono::parseIsoDateTime("2020-06-01 02:00:00+00:00")
};

const History SAMPLE_TASK_TWO_HISTORY{
    {
        TaskChangeId{2},
        TaskId("1c6f63e7-cc1a-4469-9f6a-389e8132f796"),
        TaskStatus::New,
        std::nullopt,
        {"message"},
        maps::chrono::parseIsoDateTime("2020-04-01 01:00:00+00:00")
    },
};

const auto SAMPLE_UPDATE_PARAMS = UpdateTaskParams{R"({
        "task" : {
            "service" : "sprav",
            "status" : "rejected",
            "resolution": "prohibited-by-rules"
        },
        "service" : "nmaps",
        "message" : "some message"
    })"};

} // namespace

Y_UNIT_TEST_SUITE(test_dbqueries)
{

Y_UNIT_TEST(load_task_changes)
{
    auto txn = dbPool().masterWriteableTransaction();

    api::tests::insertTask(*txn, SAMPLE_TASK_ONE);
    api::tests::insertTaskChange(*txn, SAMPLE_TASK_ONE_HISTORY[0]);
    api::tests::insertTaskChange(*txn, SAMPLE_TASK_ONE_HISTORY[1]);

    api::tests::insertTask(*txn, SAMPLE_TASK_TWO);
    api::tests::insertTaskChange(*txn, SAMPLE_TASK_TWO_HISTORY[0]);

    History result = loadTaskChanges(*txn, SAMPLE_TASK_ONE.id);
    UNIT_ASSERT_VALUES_EQUAL(result, SAMPLE_TASK_ONE_HISTORY);
}

Y_UNIT_TEST(load_feedback_task_exception)
{
    TaskId id{"fa5dd2f3-dd1b-7fb4-4425-21aaf5dbb885"};
    auto txn = dbPool().masterReadOnlyTransaction();
    UNIT_ASSERT(loadTaskChanges(*txn, id).empty());
}

Y_UNIT_TEST(load_multiple_task_changes)
{
    auto txn = dbPool().masterWriteableTransaction();

    api::tests::insertTask(*txn, SAMPLE_TASK_ONE);
    api::tests::insertTaskChange(*txn, SAMPLE_TASK_ONE_HISTORY[0]);
    api::tests::insertTaskChange(*txn, SAMPLE_TASK_ONE_HISTORY[1]);

    api::tests::insertTask(*txn, SAMPLE_TASK_TWO);
    api::tests::insertTaskChange(*txn, SAMPLE_TASK_TWO_HISTORY[0]);

    TaskHistories taskHistories = loadMultipleTaskChanges(
        *txn, {SAMPLE_TASK_ONE, SAMPLE_TASK_TWO});

    UNIT_ASSERT_VALUES_EQUAL(taskHistories.size(), 2);
    UNIT_ASSERT_VALUES_EQUAL(
        taskHistories[SAMPLE_TASK_ONE.id],
        SAMPLE_TASK_ONE_HISTORY);
    UNIT_ASSERT_VALUES_EQUAL(
        taskHistories[SAMPLE_TASK_TWO.id],
        SAMPLE_TASK_TWO_HISTORY);
}

Y_UNIT_TEST(load_multiple_task_changes_with_empty_tasks_set)
{
    auto txn = dbPool().slaveTransaction();
    UNIT_ASSERT_NO_EXCEPTION(loadMultipleTaskChanges(*txn, {}));
}

Y_UNIT_TEST(load_feedback_task_with_validation)
{
    auto txn = dbPool().masterWriteableTransaction();

    UNIT_ASSERT_EXCEPTION(
        loadFeedbackTaskWithValidation(*txn, SAMPLE_TASK_ONE.id),
        yacare::errors::NotFound);
    UNIT_ASSERT_EXCEPTION(
        loadFeedbackTaskWithValidation(*txn, TaskId{"fa5dd2f3dd1b-7fb4-4425-21aaf5dbb885"}),
        std::exception);

    api::tests::insertTask(*txn, SAMPLE_TASK_ONE);
    FeedbackTask task = loadFeedbackTaskWithValidation(*txn, SAMPLE_TASK_ONE.id);
    UNIT_ASSERT_VALUES_EQUAL(task.id.value(), SAMPLE_TASK_ONE.id.value());
}

Y_UNIT_TEST(insert_task_change)
{
    auto txn = dbPool().masterWriteableTransaction();

    api::tests::insertTask(*txn, SAMPLE_TASK_ONE);
    dbqueries::insertTaskChange(
        SAMPLE_TASK_ONE.id,
        *SAMPLE_UPDATE_PARAMS.newStatus,
        SAMPLE_UPDATE_PARAMS.initiatorService,
        SAMPLE_UPDATE_PARAMS.message,
        maps::chrono::TimePoint::clock::now()
    ).exec(*txn);

    History history = loadTaskChanges(*txn, SAMPLE_TASK_ONE.id);
    UNIT_ASSERT_VALUES_EQUAL(history.size(), 1);

    TaskChange taskChange = history[0];
    UNIT_ASSERT_VALUES_EQUAL(taskChange.taskId.value(), SAMPLE_TASK_ONE.id.value());
    UNIT_ASSERT_VALUES_EQUAL(taskChange.status, TaskStatus::Rejected);
    UNIT_ASSERT_VALUES_EQUAL(*taskChange.service, Service::Nmaps);
    UNIT_ASSERT_VALUES_EQUAL(*taskChange.message, "some message");
}

Y_UNIT_TEST(apply_task_patch)
{
    auto txn = dbPool().masterWriteableTransaction();

    api::tests::insertTask(*txn, SAMPLE_TASK_ONE);
    FeedbackTask targetTask(SAMPLE_TASK_ONE);

    const Integration newIntegration(maps::json::Value::fromString(R"({
        "services": {
            "nmaps": {
                "service_object_id": "serviceObjectId",
                "service_object_url": "serviceObjectUrl"
            }
        }
    })"));

    applyTaskPatch(
        targetTask,
        maps::chrono::TimePoint::clock::now(),
        {
            .newService = Service::Sprav,
            .newStatus = TaskStatus::Rejected,
            .newIntegration = newIntegration
        },
        Service::Nmaps
    ).exec(*txn);

    auto task = dbqueries::loadFeedbackTask(*txn, SAMPLE_TASK_ONE.id);
    UNIT_ASSERT(task);
    UNIT_ASSERT_VALUES_EQUAL(task->service, Service::Sprav);
    UNIT_ASSERT_VALUES_EQUAL(task->status, TaskStatus::Rejected);

    Integration expectedIntegration;
    expectedIntegration.addServiceOrThrow(
        Service::Nmaps,
        {
            ServiceObjectId{"serviceObjectId"},
            "serviceObjectUrl",
            ServiceDesc::NO_RESOLUTION
        });
    UNIT_ASSERT_VALUES_EQUAL(task->integration, expectedIntegration);

    const auto rows = loadSyncQueue(*txn);
    UNIT_ASSERT_VALUES_EQUAL(rows.size(), 1);
    const auto& row = rows[0];
    UNIT_ASSERT_VALUES_EQUAL(
        maps::enum_io::fromString<sq::SyncAction>(
            row[sq::columns::ACTION].as<std::string>()),
        sq::SyncAction::UgcContribution
    );
    UNIT_ASSERT_VALUES_EQUAL(
        row[dbqueries::columns::TASK_ID].as<std::string>(),
        task->id.value());
    UNIT_ASSERT_VALUES_EQUAL(
        FeedbackTask::fromJson(maps::json::Value::fromString(
            row[sq::columns::FEEDBACK_TASK].as<std::string>())),
        *task);
}

Y_UNIT_TEST(insert_notification)
{
    auto txn = dbPool().masterWriteableTransaction();

    OriginalTask originalTask{maps::json::Value::fromString(R"(
    {
        "form_id": "organization",
        "form_point": {
            "lon": 37.37,
            "lat": 55.55
        },
        "message": "test",
        "metadata": {
            "uid": 1,
            "uuid": "u-u-i-d-1",
            "locale": "ru_RU",
            "client_id": "mobile_maps_android",
            "version": "1.0"
        },
        "answer_id": "entrance",
        "object_id": "object_id",
        "question_id": "add_object"
    })")};

    auto now = maps::chrono::TimePoint::clock::now();
    FeedbackTask task{
        TaskId("7f19b475-df5e-4cef-b820-50ab4354a52c"),
        Service::Sprav,
        ServiceObjectId("serviceObjectId1"),
        "http://serviceObjectUrl",
        TaskStatus::Published,
        originalTask,
        {/*integraion*/},
        now,
        now
    };

    const auto updateParamsPublished = UpdateTaskParams{R"({
        "task": {
            "status" : "published"
        }
    })"};

    insertNotification(task, updateParamsPublished).exec(*txn);
    std::vector<notifications::Notification> notifications =
        notifications::loadNotifications(*txn);

    // result of NMAPS-12271
    // UNIT_ASSERT_VALUES_EQUAL(notifications.size(), 1);
    // const auto& notification = notifications[0];
    // UNIT_ASSERT_VALUES_EQUAL(notification.taskId.value(), task.id.value());

    const auto updateParamsSupportPublished = UpdateTaskParams{R"({
        "task": {
            "service" : "support",
            "status" : "published"
        }
    })"};
    insertNotification(task, updateParamsSupportPublished).exec(*txn);
}

Y_UNIT_TEST(insert_notification_prohibited_by_rules)
{
    auto txn = dbPool().masterWriteableTransaction();

    const OriginalTask originalTask{maps::json::Value::fromString(R"({
        "form_id": "organization",
        "message": "IDDQD_AEZAKMI",
        "form_point": {
            "lon": 37.37,
            "lat": 55.55
        },
        "metadata": {
            "uid": 42,
            "locale": "ru_RU",
            "client_id": "mobile_maps_android",
            "version": "1.0"
        },
        "answer_id": "toponym",
        "question_id": "add_object",
        "user_email": "email@test.ru"
    })")};

    const auto now = maps::chrono::TimePoint::clock::now();
    FeedbackTask task{
        .id = TaskId("dd3b2eab-a8e8-491e-bd8b-868edb754050"),
        .service = Service::Nmaps,
        .serviceObjectId = ServiceObjectId("nmapsId"),
        .serviceObjectUrl = "http://nmapsUrl",
        .status = TaskStatus::Rejected,
        .originalTask = originalTask,
        .integration = {},
        .createdAt = now,
        .updatedAt = now,
    };

    UpdateTaskParams updateParams;
    updateParams.resolution = std::string{toString(NmapsResolution::ProhibitedByRules)};

    insertNotification(task, updateParams).exec(*txn);
    const std::vector<notifications::Notification> notifications =
        notifications::loadNotifications(*txn);
    UNIT_ASSERT_VALUES_EQUAL(notifications.size(), 1);
    const auto notification = notifications[0];
    UNIT_ASSERT_VALUES_EQUAL(notification.taskId.value(), task.id.value());
    UNIT_ASSERT_VALUES_EQUAL(*notification.userId, "42");
    UNIT_ASSERT_VALUES_EQUAL(*notification.userEmail, "email@test.ru");
    UNIT_ASSERT_VALUES_EQUAL(
        notification.type, MailNotificationType::ToponymProhibitedByRules);
    UNIT_ASSERT_VALUES_EQUAL(notification.lang, Lang::Ru);
}

Y_UNIT_TEST(dont_insert_rejected_notification_from_push)
{
    auto txn = dbPool().masterWriteableTransaction();

    const OriginalTask originalTask{maps::json::Value::fromString(R"({
        "form_id": "organization",
        "message": "IDDQD_AEZAKMI",
        "form_point": {
            "lon": 37.37,
            "lat": 55.55
        },
        "metadata": {
            "uid": 42,
            "locale": "ru_RU",
            "client_id": "mobile_maps_android",
            "version": "1.0"
        },
        "form_context": {
            "client_context_id": "ugc.address_add.push_notifications"
        },
        "answer_id": "toponym",
        "question_id": "add_object",
        "user_email": "email@test.ru"
    })")};

    const auto now = maps::chrono::TimePoint::clock::now();
    FeedbackTask task{
        .id = TaskId("dd3b2eab-a8e8-491e-bd8b-868edb754050"),
        .service = Service::Nmaps,
        .serviceObjectId = ServiceObjectId("nmapsId"),
        .serviceObjectUrl = "http://nmapsUrl",
        .status = TaskStatus::InProgress,
        .originalTask = originalTask,
        .integration = {},
        .createdAt = now,
        .updatedAt = now,
    };

    UpdateTaskParams updateParams;
    updateParams.newStatus = TaskStatus::Rejected;
    updateParams.resolution = std::string{toString(NmapsResolution::ProhibitedByRules)};

    insertNotification(task, updateParams).exec(*txn);
    const std::vector<notifications::Notification> notifications =
        notifications::loadNotifications(*txn);
    UNIT_ASSERT(notifications.empty());
}

Y_UNIT_TEST(alter_db_after_task_modification)
{
    auto txn = dbPool().masterWriteableTransaction();

    const OriginalTask originalTask{maps::json::Value::fromString(R"(
    {
        "form_id": "organization",
        "message": "IDDQD_AEZAKMI",
        "form_point": {
            "lon": 37.37,
            "lat": 55.55
        },
        "metadata": {
            "uid": 42,
            "locale": "ru_RU",
            "client_id": "mobile_maps_android",
            "version": "1.0"
        },
        "answer_id": "toponym",
        "object_id": "object_id",
        "question_id": "add_object",
        "user_email": "email@test.ru"
    })")};

    const auto now = maps::chrono::TimePoint::clock::now();
    const FeedbackTask task{
        TaskId("fa5dd2f3-dd1b-7fb4-4425-21aaf5dbb885"),
        Service::Sprav,
        ServiceObjectId("serviceObjectId"),
        "http://serviceObjectUrl",
        TaskStatus::Accepted,
        originalTask,
        {/*integration*/},
        now,
        now
    };
    api::tests::insertTask(*txn, task);

    const auto updateParams = UpdateTaskParams{R"({
        "task" : {
            "service" : "nmaps",
            "status" : "published"
        },
        "message" : "test message",
        "service" : "sprav"
    })"};

    Integration newIntegration;
    newIntegration.addServiceOrThrow(
        Service::SupportNeedInfo,
        ServiceDesc{
            ServiceObjectId{"42"},
            "https://test-api.samsara.yandex-team.ru/v2/tickets/42",
            ServiceDesc::NO_RESOLUTION
        });

    UNIT_ASSERT_NO_EXCEPTION(
        alterDbAfterTaskModification(
            task,
            updateParams,
            newIntegration).exec(*txn));

    History history = loadTaskChanges(*txn, task.id);
    UNIT_ASSERT_VALUES_EQUAL(loadTaskChanges(*txn, task.id).size(), 1);
    TaskChange taskChange = history[0];
    UNIT_ASSERT_VALUES_EQUAL(taskChange.taskId.value(), task.id.value());
    UNIT_ASSERT_VALUES_EQUAL(taskChange.status, TaskStatus::Published);
    UNIT_ASSERT_VALUES_EQUAL(*taskChange.service, Service::Sprav);
    UNIT_ASSERT_VALUES_EQUAL(*taskChange.message, "test message");

    const auto updatedTask = dbqueries::loadFeedbackTask(*txn, task.id);
    UNIT_ASSERT(updatedTask);
    UNIT_ASSERT_VALUES_EQUAL(updatedTask->service, Service::Nmaps);
    UNIT_ASSERT_VALUES_EQUAL(updatedTask->status, TaskStatus::Published);
    UNIT_ASSERT_VALUES_EQUAL(updatedTask->integration, newIntegration);

    const auto notifications = notifications::loadNotifications(*txn);
    UNIT_ASSERT_VALUES_EQUAL(notifications.size(), 1);
    const auto notification = notifications[0];
    UNIT_ASSERT_VALUES_EQUAL(notification.taskId.value(), task.id.value());
    UNIT_ASSERT_VALUES_EQUAL(notification.lang, Lang::Ru);

    const double eps = 1e-9;
    const auto& notificationData = notification.data;
    UNIT_ASSERT(std::abs(notificationData.point->x() - 37.37) < eps);
    UNIT_ASSERT(std::abs(notificationData.point->y() - 55.55) < eps);

    const auto rows = loadSyncQueue(*txn);
    UNIT_ASSERT_VALUES_EQUAL(rows.size(), 1);
    const auto& row = rows[0];
    UNIT_ASSERT_VALUES_EQUAL(
        maps::enum_io::fromString<sq::SyncAction>(
            row[sq::columns::ACTION].as<std::string>()),
        sq::SyncAction::UgcContribution
    );
    UNIT_ASSERT_VALUES_EQUAL(
        row[dbqueries::columns::TASK_ID].as<std::string>(),
        task.id.value());
    UNIT_ASSERT_VALUES_EQUAL(
        FeedbackTask::fromJson(maps::json::Value::fromString(
            row[sq::columns::FEEDBACK_TASK].as<std::string>())),
        *updatedTask);
}

Y_UNIT_TEST(alter_db_after_adding_task)
{
    auto txn = dbPool().masterWriteableTransaction();

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
        .id = TaskId("dd3b2eab-a8e8-491e-bd8b-868edb754050"),
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

    dbqueries::createFeedbackTask(task, HttpData{}).exec(*txn);
    UNIT_ASSERT(!task.id.value().empty());
    task.integration = {{{
        Service::Nmaps,
        ServiceDesc{
            ServiceObjectId("serviceObjectId"),
            "http://serviceObjectUrl",
            std::nullopt
        }
    }}};

    const auto insertedTask = dbqueries::loadFeedbackTask(*txn, task.id);
    UNIT_ASSERT(insertedTask);
    UNIT_ASSERT_VALUES_EQUAL(task, *insertedTask);

    TaskChange expectedTaskChange{
        .taskId = task.id,
        .status = task.status,
        .createdAt = task.createdAt
    };

    History history = loadTaskChanges(*txn, task.id);
    UNIT_ASSERT_VALUES_EQUAL(history.size(), 1);
    expectedTaskChange.id = history[0].id;
    UNIT_ASSERT_VALUES_EQUAL(history[0], expectedTaskChange);

    const auto rows = loadSyncQueue(*txn);
    UNIT_ASSERT_VALUES_EQUAL(rows.size(), 1);
    const auto& row = rows[0];
    UNIT_ASSERT_VALUES_EQUAL(
        maps::enum_io::fromString<sq::SyncAction>(
            row[sq::columns::ACTION].as<std::string>()),
        sq::SyncAction::UgcContribution
    );
    UNIT_ASSERT_VALUES_EQUAL(
        row[dbqueries::columns::TASK_ID].as<std::string>(), task.id.value());
    UNIT_ASSERT_VALUES_EQUAL(
        FeedbackTask::fromJson(maps::json::Value::fromString(
            row[sq::columns::FEEDBACK_TASK].as<std::string>())),
        task);
}

Y_UNIT_TEST(update_rejected_with_resolution)
{
    const OriginalTask originalTask{maps::json::Value::fromString(R"({
        "form_id": "organization",
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
        "object_id": "object_id",
        "question_id": "add_object",
        "user_email": "email@test.ru"
    })")};

    const auto now = maps::chrono::TimePoint::clock::now();
    ServiceDesc nmapsServiceDesc{
        ServiceObjectId{"nmapsId"},
        "http://nmapsUrl",
        ServiceDesc::NO_RESOLUTION
    };
    FeedbackTask task{
        .id = TaskId("dd3b2eab-a8e8-491e-bd8b-868edb754050"),
        .service = Service::Nmaps,
        .serviceObjectId = ServiceObjectId("nmapsId"),
        .serviceObjectUrl = "http://nmapsUrl",
        .status = TaskStatus::Rejected,
        .originalTask = originalTask,
        .integration = Integration{std::map<Service, ServiceDesc>{
            {
                Service::Nmaps,
                nmapsServiceDesc
            }
        }},
        .createdAt = now,
        .updatedAt = now
    };

    auto txn = dbPool().masterWriteableTransaction();
    api::tests::insertTask(*txn, task);

    updateProhibitedByRulesTaskInDb(task).exec(*txn);

    const auto updatedTask = dbqueries::loadFeedbackTask(*txn, task.id);
    UNIT_ASSERT(updatedTask);
    UNIT_ASSERT_VALUES_EQUAL(updatedTask->service, Service::Nmaps);
    UNIT_ASSERT_VALUES_EQUAL(updatedTask->status, TaskStatus::Rejected);
    UNIT_ASSERT_VALUES_EQUAL(updatedTask->serviceObjectId.value(), "nmapsId");
    UNIT_ASSERT_VALUES_EQUAL(updatedTask->serviceObjectUrl, "http://nmapsUrl");
    Integration expectedIntegration{{
        {
            Service::Nmaps,
            {
                ServiceObjectId{"nmapsId"},
                "http://nmapsUrl",
                "prohibited-by-rules"
            }
        }
    }};
    UNIT_ASSERT_VALUES_EQUAL(updatedTask->integration, expectedIntegration);

    TaskChange expectedTaskChange{
        .taskId = task.id,
        .status = TaskStatus::Rejected,
        .service = Service::Nmaps,
        .message = "Rejected in nmaps with resolution prohibited-by-rules",
        .createdAt = updatedTask->updatedAt,
    };

    History history = loadTaskChanges(*txn, task.id);
    UNIT_ASSERT_VALUES_EQUAL(history.size(), 1);
    expectedTaskChange.id = history[0].id;
    UNIT_ASSERT_VALUES_EQUAL(history[0], expectedTaskChange);

    const auto rows = loadSyncQueue(*txn);
    UNIT_ASSERT_VALUES_EQUAL(rows.size(), 1);
    const auto& row = rows[0];
    UNIT_ASSERT_VALUES_EQUAL(
        maps::enum_io::fromString<sq::SyncAction>(
            row[sq::columns::ACTION].as<std::string>()),
        sq::SyncAction::UgcContribution
    );
    UNIT_ASSERT_VALUES_EQUAL(
        row[dbqueries::columns::TASK_ID].as<std::string>(),
        task.id.value());
    UNIT_ASSERT_VALUES_EQUAL(
        FeedbackTask::fromJson(maps::json::Value::fromString(
            row[sq::columns::FEEDBACK_TASK].as<std::string>())),
        *updatedTask);
}

Y_UNIT_TEST(update_redirected_to_support_in_db)
{
    const OriginalTask originalTask{maps::json::Value::fromString(R"({
        "form_id": "organization",
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
        "object_id": "object_id",
        "question_id": "add_object",
        "user_email": "email@test.ru"
    })")};

    const auto now = maps::chrono::TimePoint::clock::now();
    ServiceDesc nmapsServiceDesc{
        ServiceObjectId{"nmapsId"},
        "http://nmapsUrl",
        ServiceDesc::NO_RESOLUTION
    };
    ServiceDesc needInfoServiceDesc{
        ServiceObjectId{"samsaraId"},
        "http://samsaraUrl",
        ServiceDesc::NO_RESOLUTION
    };
    FeedbackTask task{
        .id = TaskId("dd3b2eab-a8e8-491e-bd8b-868edb754050"),
        .service = Service::Nmaps,
        .serviceObjectId = ServiceObjectId("nmapsId"),
        .serviceObjectUrl = "http://nmapsUrl",
        .status = TaskStatus::Rejected,
        .originalTask = originalTask,
        .integration = Integration{std::map<Service, ServiceDesc>{
            {
                Service::Nmaps,
                nmapsServiceDesc
            },
            {
                Service::SupportNeedInfo,
                needInfoServiceDesc
            }
        }},
        .createdAt = now,
        .updatedAt = now
    };


    auto txn = dbPool().masterWriteableTransaction();
    api::tests::insertTask(*txn, task);


    auto query = updateRedirectedToSupportInDb(task);
    query.exec(*txn);


    const auto updatedTask = dbqueries::loadFeedbackTask(*txn, task.id);
    UNIT_ASSERT(updatedTask);
    UNIT_ASSERT_VALUES_EQUAL(updatedTask->service, Service::Nmaps);
    UNIT_ASSERT_VALUES_EQUAL(updatedTask->status, TaskStatus::Rejected);
    UNIT_ASSERT_VALUES_EQUAL(updatedTask->serviceObjectId.value(), "nmapsId");
    UNIT_ASSERT_VALUES_EQUAL(updatedTask->serviceObjectUrl, "http://nmapsUrl");
    Integration expectedIntegration{{
        {
            Service::Nmaps,
            {
                ServiceObjectId{"nmapsId"},
                "http://nmapsUrl",
                "redirect-to-support"
            }
        },
        {
            Service::SupportNeedInfo,
            needInfoServiceDesc
        }
    }};
    UNIT_ASSERT_VALUES_EQUAL(updatedTask->integration, expectedIntegration);


    TaskChange expectedTaskChange{
        .taskId = task.id,
        .status = TaskStatus::Rejected,
        .service = Service::Nmaps,
        .message = "Rejected in nmaps with resolution redirect-to-support",
        .createdAt = updatedTask->updatedAt,
    };

    History history = loadTaskChanges(*txn, task.id);
    UNIT_ASSERT_VALUES_EQUAL(history.size(), 1);
    expectedTaskChange.id = history[0].id;
    UNIT_ASSERT_VALUES_EQUAL(history[0], expectedTaskChange);


    const auto rows = loadSyncQueue(*txn);
    UNIT_ASSERT_VALUES_EQUAL(rows.size(), 1);
    const auto& row = rows[0];
    UNIT_ASSERT_VALUES_EQUAL(
        maps::enum_io::fromString<sq::SyncAction>(
            row[sq::columns::ACTION].as<std::string>()),
        sq::SyncAction::UgcContribution
    );
    UNIT_ASSERT_VALUES_EQUAL(
        row[dbqueries::columns::TASK_ID].as<std::string>(),
        task.id.value());
    UNIT_ASSERT_VALUES_EQUAL(
        FeedbackTask::fromJson(maps::json::Value::fromString(
            row[sq::columns::FEEDBACK_TASK].as<std::string>())),
        *updatedTask);
}

} // test_dbqueries suite

} // namespace maps::wiki::feedback::api::tests

#include <maps/wikimap/feedback/api/src/libs/dbqueries/constants.h>
#include <maps/wikimap/feedback/api/src/libs/dbqueries/notifications.h>
#include <maps/wikimap/feedback/api/src/libs/test_helpers/common.h>
#include <maps/wikimap/feedback/api/src/libs/test_helpers/db_fixture.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::feedback::api::dbqueries::tests {

namespace {

namespace qbuilder = query_builder;

auto& dbPool()
{
    static api::tests::DbFixture db;
    return db.pool();
}

using namespace std::literals::chrono_literals;
const auto TIMEPOINT1 = maps::chrono::parseIsoDateTime("2020-04-01 01:00:00+00:00");
const auto TIMEPOINT2 = maps::chrono::parseIsoDateTime("2020-05-01 01:00:00+00:00");
const auto NOW = maps::chrono::TimePoint::clock::now();

} // namespace

Y_UNIT_TEST_SUITE(test_notifications)
{

Y_UNIT_TEST(build_notification_without_email_and_uid)
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
            "uuid": "u-u-i-d",
            "locale": "ru_RU",
            "client_id": "mobile_maps_android",
            "version": "1.0"
        },
        "answer_id": "entrance",
        "object_id": "object_id",
        "question_id": "add_object"
    })")};

    auto taskTime = NOW - 1000s;
    FeedbackTask task{
        TaskId("7f19b475-df5e-4cef-b820-50ab4354a52c"),
        Service::Nmaps,
        ServiceObjectId("serviceObjectId"),
        "http://serviceObjectUrl",
        TaskStatus::Published,
        originalTask,
        {/*integraion*/},
        taskTime,
        taskTime + 10h
    };

    auto insertToponymEmailQuery =
        buildToponymEmail(task, taskTime, MailNotificationType::ToponymPublished);
    UNIT_ASSERT(!insertToponymEmailQuery);
}

Y_UNIT_TEST(build_notification_with_email_and_uid)
{
    auto txn = dbPool().masterWriteableTransaction();

    OriginalTask originalTask{maps::json::Value::fromString(R"(
    {
        "form_id": "organization",
        "message": "test",
        "form_point": {
            "lon": 37.37,
            "lat": 55.55
        },
        "metadata": {
            "uid": 42,
            "uuid": "u-u-i-d",
            "locale": "xx_XX",
            "client_id": "mobile_maps_android",
            "version": "1.0"
        },
        "answer_id": "organization",
        "object_id": "object_id",
        "question_id": "add_object",
        "user_email": "email@test.ru"
    })")};
    auto taskTime = NOW - 100s;
    FeedbackTask task{
        TaskId("7f19b475-df5e-4cef-b820-50ab4354a52c"),
        Service::Sprav,
        ServiceObjectId("serviceObjectId"),
        "http://serviceObjectUrl",
        TaskStatus::Published,
        originalTask,
        {/*integraion*/},
        NOW - 100s,
        NOW + 100s
    };

    // add_object.organization is not in ALLOWED_QA
    UNIT_ASSERT(!buildToponymEmail(task, taskTime, MailNotificationType::ToponymPublished));
}

Y_UNIT_TEST(build_toponym_notification_without_uuid)
{
    auto txn = dbPool().masterWriteableTransaction();

    OriginalTask originalTask{maps::json::Value::fromString(R"(
    {
        "form_id": "organization",
        "message": "test",
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
    auto taskTime = NOW - 100s;
    FeedbackTask task{
        TaskId("7f19b475-df5e-4cef-b820-50ab4354a52c"),
        Service::Sprav,
        ServiceObjectId("serviceObjectId"),
        "http://serviceObjectUrl",
        TaskStatus::New,
        originalTask,
        {/*integraion*/},
        NOW - 100s,
        NOW + 100s
    };
    std::string createdAt = maps::chrono::formatSqlDateTime(taskTime);

    UNIT_ASSERT(!buildToponymEmail(task, taskTime, MailNotificationType::ToponymNew));
    task.originalTask.setMessage(internal::ALLOW_NOTIFICATIONS_IN_TESTING_KEY);

    const std::string data =
        "{\"point\":{\"lat\":55.55,\"lon\":37.37},\"user_comment\":\"IDDQD_AEZAKMI\"}";
    query_builder::InsertQuery expectedToponymEmailQuery(dbqueries::tables::NOTIFICATIONS);
    expectedToponymEmailQuery
        .appendQuoted("task_id",    "7f19b475-df5e-4cef-b820-50ab4354a52c")
        .appendQuoted("created_at", createdAt)
        .appendQuoted("lang",       "ru")
        .appendQuoted("data",       data)
        .appendQuoted("user_email", "email@test.ru")
        .appendQuoted("type",       "toponym_new")
        .appendQuoted("user_id",    "42");

    auto insertToponymEmailQuery =
        buildToponymEmail(task, taskTime, MailNotificationType::ToponymNew);
    UNIT_ASSERT(insertToponymEmailQuery);
    UNIT_ASSERT_NO_EXCEPTION(insertToponymEmailQuery->exec(*txn));
    UNIT_ASSERT_VALUES_EQUAL(
        insertToponymEmailQuery->asString(*txn),
        expectedToponymEmailQuery.asString(*txn));
}

} // test_notifications suite

} // namespace maps::wiki::feedback::api::dbqueries::tests

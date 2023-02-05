#include <maps/wikimap/feedback/api/src/sprav/lib/dbqueries.h>

#include <maps/wikimap/feedback/api/src/libs/dbqueries/constants.h>
#include <maps/wikimap/feedback/api/src/libs/sync_queue/constants.h>
#include <maps/wikimap/mapspro/libs/query_builder/include/insert_query.h>
#include <maps/wikimap/mapspro/libs/query_builder/include/select_query.h>
#include <maps/wikimap/feedback/api/src/libs/test_helpers/common.h>
#include <maps/wikimap/feedback/api/src/libs/test_helpers/db_fixture.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::feedback::api::sprav::tests {

namespace qbuilder = query_builder;
namespace sq = sync_queue;

using namespace internal;

namespace {

auto& dbPool()
{
    static api::tests::DbFixture db;
    return db.pool();
}

using namespace std::literals::chrono_literals;
const auto TIMEPOINT1 = maps::chrono::parseIsoDateTime("2020-04-01 01:00:00+00:00");
const auto TIMEPOINT2 = maps::chrono::parseIsoDateTime("2020-05-01 01:00:00+00:00");

FeedbackTask FEEDBACK_TASK{
    TaskId("e653fcfa-400c-4da6-a4c3-df170f5bc425"),
    Service::Sprav,
    ServiceObjectId("id1"),
    "http://serviceObjectUrl",
    TaskStatus::NeedInfo,
    api::tests::EMPTY_ORIGINAL_TASK,
    {/*integration*/},
    TIMEPOINT1,
    TIMEPOINT1 + 10h
};

} // namespace

Y_UNIT_TEST_SUITE(test_dbqueries)
{

Y_UNIT_TEST(build_update_without_object_id)
{
    auto txn = dbPool().masterWriteableTransaction();

    TaskStatusWithoutOid input{ServiceObjectId("id2"), Status::Rejected};
    auto expected =
        "UPDATE " + dbqueries::tables::TASK + " SET status = 'rejected', "
        "updated_at = '2020-05-01 01:00:00+00:00' "
        "WHERE service = 'sprav' AND service_object_id = 'id2'";
    auto query = buildUpdateTask(input, TIMEPOINT2, "id2").asString(*txn);
    UNIT_ASSERT_NO_EXCEPTION(txn->exec(query));
    UNIT_ASSERT_VALUES_EQUAL(query, expected);
}

Y_UNIT_TEST(build_update_with_object_id)
{
    auto txn = dbPool().masterWriteableTransaction();

    TaskStatusWithOid input{{ServiceObjectId("id1"), Status::Published}, 1u};
    auto expected =
        "UPDATE " + dbqueries::tables::TASK + " SET "
        "status = 'published', "
        "updated_at = '2020-05-01 01:00:00+00:00', "
        "original_task = jsonb_set(original_task, '{object_id}', to_jsonb('1'::text)) "
        "WHERE service = 'sprav' AND service_object_id = 'id1'";
    auto query = buildUpdateTask(input, TIMEPOINT2, "id1").asString(*txn);
    UNIT_ASSERT_NO_EXCEPTION(txn->exec(query));
    UNIT_ASSERT_VALUES_EQUAL(query, expected);
}

Y_UNIT_TEST(build_insert_change_task)
{
    auto txn = dbPool().masterWriteableTransaction();

    // Cannot insert task in feedback_task_changes if task with the same
    // id is not present in feedback_task table
    api::tests::insertTask(*txn, FEEDBACK_TASK);

    TaskStatusWithOid input{{ServiceObjectId("id1"), Status::Published}, 1u};
    auto expected =
        "INSERT INTO " + dbqueries::tables::TASK_CHANGES + " "
        "(created_at, service, status, task_id) VALUES "
        "('2020-05-01 01:00:00+00:00', 'sprav', 'published', 'e653fcfa-400c-4da6-a4c3-df170f5bc425')";
    auto query = buildInsertChangeTask(
        input,
        TaskId{"e653fcfa-400c-4da6-a4c3-df170f5bc425"},
        TIMEPOINT2).asString(*txn);
    UNIT_ASSERT_NO_EXCEPTION(txn->exec(query));
    UNIT_ASSERT_VALUES_EQUAL(query, expected);
}

Y_UNIT_TEST(build_notification)
{
    auto txn = dbPool().masterWriteableTransaction();

    auto query = qbuilder::InsertQuery(dbqueries::tables::NOTIFICATIONS)
        .appendQuoted("task_id",    "a5da513d-bbaf-4f7e-b7f4-30c4fb137d8d")
        .appendQuoted("created_at", "2020-05-01 12:00:00.0+03")
        .appendQuoted("user_id",    "uuid1")
        .appendQuoted("type",       "organization_update")
        .appendQuoted("lang",       "ru")
        .appendQuoted("data",       "{\"company_id\":\"object_id\"}")
        .asString(*txn);

    auto expected =
        "INSERT INTO " + dbqueries::tables::NOTIFICATIONS + " ("
        "created_at, data, lang, task_id, type, user_id"
        ") VALUES ("
            "'2020-05-01 12:00:00.0+03', "
            "'{\"company_id\":\"object_id\"}', "
            "'ru', "
            "'a5da513d-bbaf-4f7e-b7f4-30c4fb137d8d', "
            "'organization_update', "
            "'uuid1'"
        ")";
    UNIT_ASSERT_NO_EXCEPTION(txn->exec(query));
    UNIT_ASSERT_VALUES_EQUAL(query, expected);
}

Y_UNIT_TEST(load_tasks_empty)
{
    auto tasks = loadInProgressTasks(dbPool());
    UNIT_ASSERT(tasks.empty());
}

Y_UNIT_TEST(load_notifications_empty)
{
    auto txn = dbPool().slaveTransaction();

    auto query = "SELECT COUNT(*) FROM " + dbqueries::tables::NOTIFICATIONS;
    auto result = txn->exec(query);
    UNIT_ASSERT_EQUAL(result[0][0].as<size_t>(), 0);
}

Y_UNIT_TEST(update_task_status)
{
    auto txn = dbPool().masterWriteableTransaction();

    TaskStatusWithoutOid taskStatus{
        FEEDBACK_TASK.serviceObjectId,
        sprav::Status::Rejected
    };

    UNIT_ASSERT_EQUAL(api::tests::loadSyncQueue(*txn).size(), 0);

    api::tests::insertTask(*txn, FEEDBACK_TASK);
    updateTaskStatus(taskStatus, FEEDBACK_TASK, TIMEPOINT1).exec(*txn);

    auto syncQueue = api::tests::loadSyncQueue(*txn);
    UNIT_ASSERT_EQUAL(syncQueue.size(), 1);
    UNIT_ASSERT_EQUAL(
        syncQueue[0][sq::columns::ACTION].as<std::string>(),
        std::string(toString(sq::SyncAction::UgcContribution))
    );
    UNIT_ASSERT_EQUAL(
        FeedbackTask::fromJson(maps::json::Value::fromString(
            syncQueue[0][sq::columns::FEEDBACK_TASK].as<std::string>()
        )).status,
        TaskStatus::Rejected
    );
}

} // test_dbqueries suite

} // namespace maps::wiki::feedback::api::sprav::tests

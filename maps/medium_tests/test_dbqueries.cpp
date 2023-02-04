#include <maps/wikimap/feedback/api/src/gdpr/lib/dbqueries.h>

#include <maps/wikimap/feedback/api/src/libs/dbqueries/constants.h>
#include <maps/wikimap/feedback/api/src/libs/test_helpers/common.h>
#include <maps/wikimap/feedback/api/src/libs/test_helpers/db_fixture.h>
#include <maps/wikimap/feedback/api/src/libs/common/tests/helpers/printers.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <util/stream/output.h>

template <>
void Out<maps::wiki::feedback::api::gdpr::Takeout>(
    IOutputStream& os,
    const maps::wiki::feedback::api::gdpr::Takeout& takeout)
{
    std::ostringstream ostr;
    ostr << takeout;
    os << ostr.str();
}

template <>
void Out<maps::wiki::feedback::api::gdpr::HiddenData>(
    IOutputStream& os,
    const maps::wiki::feedback::api::gdpr::HiddenData& hiddenData)
{
    std::ostringstream ostr;
    ostr << hiddenData;
    os << ostr.str();
}

namespace maps::wiki::feedback::api::gdpr::tests {

namespace qbuilder = query_builder;

using namespace internal;

namespace {

auto& dbPool()
{
    static api::tests::DbFixture db;
    return db.pool();
}

using namespace std::literals::chrono_literals;
const auto TIMEPOINT = maps::chrono::parseIsoDateTime("2021-01-01 12:00:00+00:00");

Takeout NEW_REQUEST{
    TakeoutId(0),
    Uid(42),
    RequestId("id1"),
    TIMEPOINT,
    std::nullopt
};

const OriginalTask ORIGINAL_TASK_SAMPLE{maps::json::Value::fromString(R"({
    "form_id": "organization",
    "message": "IDDQD_AEZAKMI",
    "form_point": {
        "lon": 37.37,
        "lat": 55.55
    },
    "metadata": {
        "uid": 42,
        "uuid": "c5216b6a-2379-4bab-a729-515831a42e72",
        "device_id": "ABC012",
        "yandexuid": "123456",
        "ip": "192.168.0.1",
        "port": 54321,
        "locale": "ru_RU",
        "client_id": "mobile_maps_android",
        "version": "1.0",
        "bebr_session_id": "bebr_session_id1"
    },
    "answer_id": "toponym",
    "object_id": "object_id",
    "question_id": "add_object",
    "user_full_name": "Test Test",
    "user_email": "email@test.ru"
})")};

const OriginalTask ORIGINAL_TASK_SAMPLE_CLEANED{maps::json::Value::fromString(R"({
    "form_id": "organization",
    "message": "IDDQD_AEZAKMI",
    "form_point": {
        "lon": 37.37,
        "lat": 55.55
    },
    "metadata": {
        "locale": "ru_RU",
        "client_id": "mobile_maps_android",
        "version": "1.0"
    },
    "answer_id": "toponym",
    "object_id": "object_id",
    "question_id": "add_object"
})")};

const FeedbackTask FEEDBACK_TASK_SAMPLE{
    TaskId("fa5dd2f3-dd1b-7fb4-4425-21aaf5dbb885"),
    Service::Support,
    ServiceObjectId("serviceObjectId"),
    "http://serviceObjectUrl",
    TaskStatus::NeedInfo,
    ORIGINAL_TASK_SAMPLE,
    {/*integration*/},
    maps::chrono::parseIsoDateTime("2020-04-01 01:00:00+00:00"),
    maps::chrono::parseIsoDateTime("2020-04-02 01:00:00+00:00")
};

query_builder::InsertQuery insertTakeoutQuery(const Takeout& takeout)
{
    query_builder::InsertQuery insertQuery(dbqueries::tables::GDPR_TAKEOUT);
    insertQuery
        .append(dbqueries::columns::UID, std::to_string(takeout.uid.value()))
        .appendQuoted(dbqueries::columns::REQUEST_ID, takeout.requestId.value())
        .appendQuoted(dbqueries::columns::REQUESTED_AT,
            maps::chrono::formatSqlDateTime(takeout.requestedAt));
    return insertQuery;
}

TakeoutId insertTakeout(pqxx::transaction_base& txn, const Takeout& takeout)
{
    auto query = insertTakeoutQuery(takeout);
    auto queryStr = query.asString(txn) + " RETURNING " + dbqueries::columns::TAKEOUT_ID;

    auto result = txn.exec(queryStr);
    return TakeoutId(result[0][dbqueries::columns::TAKEOUT_ID].as<TakeoutId::ValueType>());
}

query_builder::SelectQuery loadTakeoutQuery(TakeoutId takeoutId)
{
    return query_builder::SelectQuery(
        dbqueries::tables::GDPR_TAKEOUT,
        qbuilder::WhereConditions()
            .append(dbqueries::columns::TAKEOUT_ID, std::to_string(takeoutId.value())));
}

void checkTakeout(const Takeout& left, const Takeout& right) {
    UNIT_ASSERT_VALUES_EQUAL(left.uid, right.uid);
    UNIT_ASSERT_VALUES_EQUAL(left.requestId, right.requestId);
    UNIT_ASSERT_VALUES_EQUAL(bool(left.completedAt), bool(right.completedAt));
}

} // namespace

Y_UNIT_TEST_SUITE(test_gdpr_dbqueries)
{

Y_UNIT_TEST(load_incomplete_takeouts)
{
    auto txn = dbPool().masterWriteableTransaction();

    insertTakeout(*txn, NEW_REQUEST);

    auto takeouts = loadIncompleteTakeouts(*txn);
    UNIT_ASSERT_VALUES_EQUAL(takeouts.size(), 1);
    checkTakeout(takeouts[0], NEW_REQUEST);
}

Y_UNIT_TEST(same_request_id)
{
    auto txn = dbPool().masterWriteableTransaction();

    insertTakeout(*txn, NEW_REQUEST);

    auto anotherRequest = NEW_REQUEST;
    anotherRequest.uid = Uid(43);
    insertTakeout(*txn, anotherRequest);

    auto takeouts = loadIncompleteTakeouts(*txn);
    UNIT_ASSERT_VALUES_EQUAL(takeouts.size(), 2);
    checkTakeout(takeouts[0], NEW_REQUEST);
    checkTakeout(takeouts[1], anotherRequest);
}

Y_UNIT_TEST(set_takeout_completed)
{
    auto txn = dbPool().masterWriteableTransaction();

    TakeoutId takeoutId = insertTakeout(*txn, NEW_REQUEST);

    UNIT_ASSERT_VALUES_EQUAL(loadIncompleteTakeouts(*txn).size(), 1);

    auto setCompletedQuery = setTakeoutCompletedQuery(takeoutId, TIMEPOINT);

    UNIT_ASSERT_NO_EXCEPTION(setCompletedQuery.exec(*txn));
    UNIT_ASSERT_VALUES_EQUAL(loadIncompleteTakeouts(*txn).size(), 0);

    Takeout completedTakeout{loadTakeoutQuery(takeoutId).exec(*txn)[0]};
    UNIT_ASSERT(completedTakeout.completedAt);
    UNIT_ASSERT_EQUAL(completedTakeout.completedAt.value(), TIMEPOINT);
}

Y_UNIT_TEST(load_tasks_to_clean_query)
{
    auto txn = dbPool().masterWriteableTransaction();
    api::tests::insertTask(*txn, FEEDBACK_TASK_SAMPLE);

    auto query = loadTasksToCleanQuery(NEW_REQUEST.uid, NEW_REQUEST.requestedAt);
    std::ostringstream expected;

    expected <<
        "SELECT "
            "id, service, service_object_id, service_object_url, status, "
            "original_task, created_at, updated_at, integration "
        "FROM " << dbqueries::tables::TASK << " "
        "WHERE "
            "original_task->'metadata'->>'uid' = '" << NEW_REQUEST.uid << "' "
            "AND created_at <= '" << maps::chrono::formatSqlDateTime(NEW_REQUEST.requestedAt) << "' "
        "LIMIT 10000";

    UNIT_ASSERT_VALUES_EQUAL(query.asString(*txn), expected.str());
    auto result = query.exec(*txn);
    UNIT_ASSERT_VALUES_EQUAL(result.size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(result[0], FEEDBACK_TASK_SAMPLE);
}

Y_UNIT_TEST(load_hidden_data)
{
    const auto& metadata = FEEDBACK_TASK_SAMPLE.originalTask.metadata();

    auto txn = dbPool().masterWriteableTransaction();
    TakeoutId takeoutId = insertTakeout(*txn, NEW_REQUEST);

    query_builder::InsertQuery insertQuery(dbqueries::tables::GDPR_HIDDEN_DATA);
    insertQuery
        .append(dbqueries::columns::UID, std::to_string(FEEDBACK_TASK_SAMPLE.originalTask.uid()->value()))
        .appendQuoted(dbqueries::columns::TASK_ID,        FEEDBACK_TASK_SAMPLE.id.value())
        .append(dbqueries::columns::TAKEOUT_ID,           std::to_string(takeoutId.value()))
        .appendQuoted(dbqueries::columns::USER_EMAIL,     *FEEDBACK_TASK_SAMPLE.originalTask.userEmail())
        .appendQuoted(dbqueries::columns::USER_FULL_NAME, *FEEDBACK_TASK_SAMPLE.originalTask.userFullName())
        .appendQuoted(dbqueries::columns::UUID,           metadata.uuid->value())
        .appendQuoted(dbqueries::columns::DEVICE_ID,      *metadata.deviceId)
        .appendQuoted(dbqueries::columns::YANDEXUID,      *metadata.yandexuid)
        .appendQuoted(dbqueries::columns::IP,             metadata.ip->value())
        .append(dbqueries::columns::PORT, std::to_string(metadata.port->value()));

    insertQuery.exec(*txn);

    auto hiddenData = loadHiddenData(*txn, FEEDBACK_TASK_SAMPLE.id);

    UNIT_ASSERT(hiddenData);
    UNIT_ASSERT_VALUES_EQUAL(hiddenData->taskId.value(), FEEDBACK_TASK_SAMPLE.id.value());
    UNIT_ASSERT_VALUES_EQUAL(hiddenData->takeoutId.value(), takeoutId.value());

    UNIT_ASSERT(hiddenData->userEmail);
    UNIT_ASSERT(hiddenData->userFullName);
    UNIT_ASSERT_VALUES_EQUAL(*hiddenData->userEmail, *FEEDBACK_TASK_SAMPLE.originalTask.userEmail());
    UNIT_ASSERT_VALUES_EQUAL(*hiddenData->userFullName, *FEEDBACK_TASK_SAMPLE.originalTask.userFullName());

    UNIT_ASSERT_VALUES_EQUAL(hiddenData->uid.value(), metadata.uid->value());

    UNIT_ASSERT(hiddenData->uuid);
    UNIT_ASSERT(hiddenData->deviceId);
    UNIT_ASSERT(hiddenData->yandexuid);
    UNIT_ASSERT(hiddenData->ip);
    UNIT_ASSERT(hiddenData->port);
    UNIT_ASSERT_VALUES_EQUAL(*hiddenData->uuid, metadata.uuid->value());
    UNIT_ASSERT_VALUES_EQUAL(*hiddenData->deviceId, *metadata.deviceId);
    UNIT_ASSERT_VALUES_EQUAL(*hiddenData->yandexuid, *metadata.yandexuid);
    UNIT_ASSERT_VALUES_EQUAL(*hiddenData->ip, *metadata.ip);
    UNIT_ASSERT_VALUES_EQUAL(*hiddenData->port, *metadata.port);
}

Y_UNIT_TEST(move_task_data_to_hidden)
{
    auto txn = dbPool().masterWriteableTransaction();

    TakeoutId takeoutId = insertTakeout(*txn, NEW_REQUEST);

    api::tests::insertTask(*txn, FEEDBACK_TASK_SAMPLE);
    UNIT_ASSERT_NO_EXCEPTION(moveTaskDataToHidden(*txn, FEEDBACK_TASK_SAMPLE.id, takeoutId));

    const auto updatedTask = dbqueries::loadFeedbackTask(*txn, FEEDBACK_TASK_SAMPLE.id);

    UNIT_ASSERT(updatedTask);
    UNIT_ASSERT_VALUES_EQUAL(updatedTask->id.value(), FEEDBACK_TASK_SAMPLE.id.value());
    UNIT_ASSERT_VALUES_EQUAL(updatedTask->originalTask, ORIGINAL_TASK_SAMPLE_CLEANED);

    const auto hiddenData = loadHiddenData(*txn, FEEDBACK_TASK_SAMPLE.id);
    UNIT_ASSERT(hiddenData);
    UNIT_ASSERT_VALUES_EQUAL(hiddenData->taskId.value(), FEEDBACK_TASK_SAMPLE.id.value());
    UNIT_ASSERT_VALUES_EQUAL(hiddenData->takeoutId.value(), takeoutId.value());

    UNIT_ASSERT(hiddenData->userEmail);
    UNIT_ASSERT(hiddenData->userFullName);
    UNIT_ASSERT_VALUES_EQUAL(*hiddenData->userEmail, *FEEDBACK_TASK_SAMPLE.originalTask.userEmail());
    UNIT_ASSERT_VALUES_EQUAL(*hiddenData->userFullName, *FEEDBACK_TASK_SAMPLE.originalTask.userFullName());

    const auto& metadata = FEEDBACK_TASK_SAMPLE.originalTask.metadata();
    UNIT_ASSERT_VALUES_EQUAL(hiddenData->uid.value(), metadata.uid->value());

    UNIT_ASSERT(hiddenData->uuid);
    UNIT_ASSERT(hiddenData->deviceId);
    UNIT_ASSERT(hiddenData->yandexuid);
    UNIT_ASSERT(hiddenData->ip);
    UNIT_ASSERT(hiddenData->port);
    UNIT_ASSERT(hiddenData->bebrSessionId);
    UNIT_ASSERT_VALUES_EQUAL(hiddenData->uuid.value(), metadata.uuid->value());
    UNIT_ASSERT_VALUES_EQUAL(*hiddenData->deviceId, *metadata.deviceId);
    UNIT_ASSERT_VALUES_EQUAL(*hiddenData->yandexuid, *metadata.yandexuid);
    UNIT_ASSERT_VALUES_EQUAL(*hiddenData->ip, *metadata.ip);
    UNIT_ASSERT_VALUES_EQUAL(*hiddenData->port, *metadata.port);
    UNIT_ASSERT_VALUES_EQUAL(*hiddenData->bebrSessionId, *metadata.bebrSessionId);
}

Y_UNIT_TEST(process_takeouts)
{
    auto txnInsert = dbPool().masterWriteableTransaction();
    api::tests::insertTask(*txnInsert, FEEDBACK_TASK_SAMPLE);
    insertTakeoutQuery(NEW_REQUEST).exec(*txnInsert);

    UNIT_ASSERT_VALUES_EQUAL(loadIncompleteTakeouts(*txnInsert).size(), 1);

    txnInsert->commit();

    UNIT_ASSERT_NO_EXCEPTION(processTakeouts(dbPool(), false));

    auto txnCheck = dbPool().slaveTransaction();
    UNIT_ASSERT_VALUES_EQUAL(loadIncompleteTakeouts(*txnCheck).size(), 1);

    const auto updatedTask = dbqueries::loadFeedbackTask(*txnCheck, FEEDBACK_TASK_SAMPLE.id);
    UNIT_ASSERT(updatedTask);
    UNIT_ASSERT_VALUES_EQUAL(updatedTask->id.value(), FEEDBACK_TASK_SAMPLE.id.value());
    UNIT_ASSERT_VALUES_EQUAL(updatedTask->originalTask, ORIGINAL_TASK_SAMPLE_CLEANED);

    UNIT_ASSERT_NO_EXCEPTION(processTakeouts(dbPool(), false));

    auto txnComplete = dbPool().slaveTransaction();

    UNIT_ASSERT_VALUES_EQUAL(loadIncompleteTakeouts(*txnComplete).size(), 0);
}

} // test_dbqueries suite

} // namespace maps::wiki::feedback::api::sprav::tests
